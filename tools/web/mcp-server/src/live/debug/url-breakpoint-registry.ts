import type {
  BindTargetResult,
  BreakpointLocation,
  CdpScope,
  DebuggerSetBreakpointByUrlResponse,
  LogpointCapture,
  RuntimeEvaluateResponse,
  RuntimeGetPropertiesResponse,
  RuntimePropertyDescriptor,
  RuntimeRemoteObject,
  UrlBreakpointAddResult,
  UrlBreakpointSpec,
} from "../../types/live/debug.js";
import { createLogger } from "../../utils/logger.js";

const log = createLogger("live:breakpoints");

const MAX_LOG_CAPTURES = 4000;

const MAX_CONCURRENT_CAPTURES = 2;

const CAPTURE_MARKER = "##WLPCAP##";

export type CdpSend = (
  method: string,
  params: Record<string, unknown>,
  sessionId: string
) => Promise<unknown>;

export type SessionLister = () => Iterable<string>;

export type TargetInfoResolver = (sessionId: string) => { type: string; url: string } | undefined;

export interface LogpointFrame {
  callFrameId: string;
  scopeChain: CdpScope[];
}

export interface PausedDisposition {

  matched: boolean;

  block: boolean;
}

interface UrlBreakpoint extends UrlBreakpointSpec {
  id: string;
  block: boolean;

  nonPausing: boolean;

  locationKey: string;
}

interface BoundLocation {

  key: string;
  urlRegex: string;
  byteOffset: number;
  condition?: string;

  refIds: Set<string>;

  v8BySession: Map<string, string>;

  locations: BreakpointLocation[];

  targetTypes: ReadonlySet<string> | null;
}

export class UrlBreakpointRegistry {
  private readonly send: CdpSend;
  private readonly sessions: SessionLister;
  private readonly targetInfo?: TargetInfoResolver;
  private readonly breakpoints = new Map<string, UrlBreakpoint>();
  private readonly locations = new Map<string, BoundLocation>();

  private readonly v8ToLocation = new Map<string, string>();
  private readonly captures: LogpointCapture[] = [];

  private inFlightCaptures = 0;
  private nextId = 0;

  constructor(send: CdpSend, sessions: SessionLister, targetInfo?: TargetInfoResolver) {
    this.send = send;
    this.sessions = sessions;
    this.targetInfo = targetInfo;
  }

  async add(spec: UrlBreakpointSpec): Promise<UrlBreakpointAddResult> {
    const id = `wbp_${++this.nextId}`;

    // A logExpression installs a non-suspending capture condition (captureCondition console.logs the value
    // and returns false). V8 exposes locals ($var0...) inside a breakpoint condition but NOT the operand
    // stack $stack (verified: re/calls/runtime/test-stack-condition.mjs -> hasStack=false), so a $stack
    // expression MUST take the suspending path and be read from the paused frame.
    const nonPausing = spec.logExpression != null && !spec.logExpression.includes("$stack");
    const v8Condition = nonPausing
      ? UrlBreakpointRegistry.captureCondition(id, spec.logExpression as string)
      : spec.condition;
    // A logExpression signals logpoint intent, so do not freeze on hit by default: a non-pausing capture
    // never suspends, and a $stack capture suspends only long enough to read the operand stack and then
    // resumes. A breakpoint with no logExpression is a real breakpoint and stays paused.
    const block = spec.block ?? (spec.logExpression != null ? false : true);
    const warning =
      spec.logExpression != null && spec.logExpression.includes("$stack")
        ? "logExpression reads the operand stack ($stack), which V8 cannot expose to a non-pausing " +
          "condition, so this breakpoint suspends the target on each hit to read the stack and then resumes " +
          "(unless block is set). For a purely non-pausing logpoint, target an instruction where the value " +
          "is in a local and read $var0, $var1, ... instead; or pass block to control the pause."
        : undefined;
    const locationKey = UrlBreakpointRegistry.locationKey(spec.url, spec.byteOffset, v8Condition);
    const breakpoint: UrlBreakpoint = {
      ...spec,
      id,
      block,
      nonPausing,
      locationKey,
    };
    this.breakpoints.set(id, breakpoint);

    let location = this.locations.get(locationKey);
    if (location) {
      location.refIds.add(id);
      // Reusing a shared location: widen its type filter to the union (an unfiltered spec wins).
      if (location.targetTypes != null) {
        location.targetTypes =
          spec.targetTypes == null ? null : new Set([...location.targetTypes, ...spec.targetTypes]);
      }
      log.info(`url-breakpoint: ${id} reuses location ${locationKey} (${location.refIds.size} sharing)`);
      return { id, locations: location.locations, perTarget: this.reportLocation(location), warning };
    }

    location = {
      key: locationKey,
      urlRegex: UrlBreakpointRegistry.escapeRegex(spec.url),
      byteOffset: spec.byteOffset,
      condition: v8Condition,
      refIds: new Set([id]),
      v8BySession: new Map(),
      locations: [],
      targetTypes: spec.targetTypes ?? null,
    };
    this.locations.set(locationKey, location);
    const perTarget: BindTargetResult[] = [];
    for (const sessionId of this.sessions()) {
      perTarget.push(await this.bindLocation(location, sessionId));
    }
    const boundCount = perTarget.filter((t) => t.bound).length;
    log.info(
      `url-breakpoint: ${id} bound in ${boundCount}/${perTarget.length} targets, ${location.locations.length} resolved locations`
    );
    return { id, locations: location.locations, perTarget, warning };
  }

  /** Builds a per-target binding report for an already-bound location (used on the shared-reuse path). */
  private reportLocation(location: BoundLocation): BindTargetResult[] {
    const report: BindTargetResult[] = [];
    for (const sessionId of this.sessions()) {
      const info = this.targetInfo?.(sessionId);
      report.push({
        sessionId,
        type: info?.type ?? "",
        url: info?.url ?? "",
        bound: location.v8BySession.has(sessionId),
        locations: location.locations,
      });
    }
    return report;
  }

  async remove(id: string): Promise<void> {
    const breakpoint = this.breakpoints.get(id);
    if (!breakpoint) return;
    this.breakpoints.delete(id);
    const location = this.locations.get(breakpoint.locationKey);
    if (!location) return;
    location.refIds.delete(id);
    if (location.refIds.size > 0) {
      log.info(`url-breakpoint: removed ${id}, ${location.refIds.size} still sharing its location`);
      return;
    }
    for (const [sessionId, v8Id] of location.v8BySession) {
      await this.send("Debugger.removeBreakpoint", { breakpointId: v8Id }, sessionId).catch((e) => {
        log.debug(`url-breakpoint: removeBreakpoint ${v8Id} failed: ${String(e)}`);
      });
      this.v8ToLocation.delete(v8Id);
    }
    this.locations.delete(breakpoint.locationKey);
    log.info(`url-breakpoint: removed ${id} and tore down its location`);
  }

  async bindSession(sessionId: string): Promise<void> {
    for (const location of this.locations.values()) {
      await this.bindLocation(location, sessionId);
    }
  }

  forgetSession(sessionId: string): void {
    for (const location of this.locations.values()) {
      const v8Id = location.v8BySession.get(sessionId);
      if (v8Id == null) continue;
      this.v8ToLocation.delete(v8Id);
      location.v8BySession.delete(sessionId);
    }
  }

  handlePaused(hitBreakpointIds: string[], frame: LogpointFrame, sessionId: string): PausedDisposition {
    const matched: UrlBreakpoint[] = [];
    for (const v8Id of hitBreakpointIds) {
      const locationKey = this.v8ToLocation.get(v8Id);
      if (locationKey == null) continue;
      const location = this.locations.get(locationKey);
      if (!location) continue;
      for (const refId of location.refIds) {
        const breakpoint = this.breakpoints.get(refId);

        if (breakpoint?.logExpression != null && !breakpoint.nonPausing) matched.push(breakpoint);
      }
    }
    if (matched.length === 0) return { matched: false, block: true };
    const anyBlock = matched.some((b) => b.block);

    if (this.inFlightCaptures >= MAX_CONCURRENT_CAPTURES) {
      if (!anyBlock) void this.send("Debugger.resume", {}, sessionId).catch(() => undefined);
      return { matched: true, block: anyBlock };
    }
    this.inFlightCaptures++;
    void this.runCaptures(matched, frame, sessionId, !anyBlock).finally(() => {
      this.inFlightCaptures--;
    });
    return { matched: true, block: anyBlock };
  }

  getCaptures(options: { id?: string; clear?: boolean } = {}): LogpointCapture[] {
    const { id, clear = false } = options;
    const selected = id == null ? [...this.captures] : this.captures.filter((c) => c.id === id);
    if (clear) {
      if (id == null) {
        this.captures.length = 0;
      } else {
        const kept = this.captures.filter((c) => c.id !== id);
        this.captures.length = 0;
        this.captures.push(...kept);
      }
    }
    return selected;
  }

  clear(): void {
    this.breakpoints.clear();
    this.locations.clear();
    this.v8ToLocation.clear();
    this.captures.length = 0;
  }

  private async bindLocation(location: BoundLocation, sessionId: string): Promise<BindTargetResult> {
    const info = this.targetInfo?.(sessionId);
    const type = info?.type ?? "";
    const url = info?.url ?? "";
    // Honour the optional target-type filter (null = bind every target).
    if (location.targetTypes && !location.targetTypes.has(type)) {
      return { sessionId, type, url, bound: false, locations: [] };
    }
    if (location.v8BySession.has(sessionId)) {
      return { sessionId, type, url, bound: true, locations: location.locations };
    }
    let response: DebuggerSetBreakpointByUrlResponse | undefined;
    let error: string | undefined;
    try {
      response = (await this.send(
        "Debugger.setBreakpointByUrl",
        {
          urlRegex: location.urlRegex,
          lineNumber: 0,
          columnNumber: location.byteOffset,
          condition: location.condition,
        },
        sessionId
      )) as DebuggerSetBreakpointByUrlResponse | undefined;
    } catch (e) {
      error = String(e);
      log.debug(`url-breakpoint: setBreakpointByUrl failed in ${sessionId}: ${error}`);
    }
    if (!response?.breakpointId) {
      return { sessionId, type, url, bound: false, locations: [], error: error ?? "no breakpointId returned" };
    }
    location.v8BySession.set(sessionId, response.breakpointId);
    this.v8ToLocation.set(response.breakpointId, location.key);
    const resolved = response.locations ?? [];
    if (resolved.length) location.locations.push(...resolved);
    return { sessionId, type, url, bound: true, locations: resolved };
  }

  /**
   * Re-attempts binding any not-yet-bound location whose URL matches a freshly parsed script in the given
   * session. Recovers locations whose initial setBreakpointByUrl failed because the target was busy or not
   * yet attached when the breakpoint was added (e.g. a pthread worker that only yields to CDP at spawn).
   */
  async onScriptParsed(sessionId: string, url: string): Promise<void> {
    if (!url) return;
    for (const location of this.locations.values()) {
      if (location.v8BySession.has(sessionId)) continue;
      let matches = false;
      try {
        matches = new RegExp(location.urlRegex).test(url);
      } catch {
        matches = false;
      }
      if (matches) await this.bindLocation(location, sessionId);
    }
  }

  private async runCaptures(
    matched: UrlBreakpoint[],
    frame: LogpointFrame,
    sessionId: string,
    resumeAfter: boolean
  ): Promise<void> {
    try {

      const needsLocals = matched.some((b) => b.logExpression?.includes("$var"));
      const needsStack = matched.some((b) => b.logExpression?.includes("$stack"));
      const { locals, stack } = await this.readFrameScopes(
        frame.scopeChain,
        sessionId,
        needsLocals,
        needsStack
      );
      for (const breakpoint of matched) {
        if (breakpoint.logExpression == null) continue;
        const capture = await this.evaluateLogpoint(
          frame.callFrameId,
          locals,
          stack,
          breakpoint.logExpression,
          sessionId
        );
        if (capture.skip) continue;
        this.buffer({ id: breakpoint.id, ts: new Date().toISOString(), value: capture.value });
      }
    } catch (e) {
      log.debug(`url-breakpoint: capture failed: ${String(e)}`);
    } finally {
      if (resumeAfter) {
        await this.send("Debugger.resume", {}, sessionId).catch(() => undefined);
      }
    }
  }

  private async readFrameScopes(
    scopeChain: CdpScope[],
    sessionId: string,
    needsLocals: boolean,
    needsStack: boolean
  ): Promise<{ locals: Record<string, number>; stack: number[] }> {
    const locals: Record<string, number> = {};
    const stack: number[] = [];
    for (const scope of scopeChain) {
      const objectId = scope.object?.objectId;
      if (objectId == null) continue;
      if (scope.type === "wasm-expression-stack") {
        if (needsStack) await this.readOperandStack(objectId, sessionId, stack);
      } else if (scope.type === "local" && needsLocals) {
        for (const property of await this.getProperties(objectId, sessionId)) {
          if (!property.name.startsWith("$var") || property.name in locals) continue;
          const value = await this.wasmLocalNumber(property.value, sessionId);
          if (value != null) locals[property.name] = value;
        }
      }
    }
    return { locals, stack };
  }

  private async readOperandStack(scopeObjectId: string, sessionId: string, stack: number[]): Promise<void> {
    const stackArrayId = (await this.getProperties(scopeObjectId, sessionId)).find((p) => p.name === "stack")
      ?.value?.objectId;
    if (stackArrayId == null) return;
    for (const slot of await this.getProperties(stackArrayId, sessionId)) {
      if (!/^\d+$/.test(slot.name)) continue;
      const value = await this.wasmLocalNumber(slot.value, sessionId);
      if (value != null) stack[Number(slot.name)] = value;
    }
  }

  private async getProperties(objectId: string, sessionId: string): Promise<RuntimePropertyDescriptor[]> {
    const response = (await this.send(
      "Runtime.getProperties",
      { objectId, ownProperties: true, generatePreview: true },
      sessionId
    ).catch(() => undefined)) as RuntimeGetPropertiesResponse | undefined;
    return response?.result ?? [];
  }

  private async evaluateLogpoint(
    callFrameId: string,
    locals: Record<string, number>,
    stack: number[],
    expression: string,
    sessionId: string
  ): Promise<{ skip: boolean; value?: unknown }> {
    const names = Object.keys(locals);
    const args = names.map((name) => String(locals[name]));
    names.push("$stack");
    args.push("[" + Array.from(stack, (v) => (v == null ? 0 : v)).join(",") + "]");

    const wrapped = `((${names.join(",")})=>{return (${expression});})(${args.join(",")})`;
    const response = (await this.send(
      "Debugger.evaluateOnCallFrame",
      { callFrameId, expression: wrapped, returnByValue: true },
      sessionId
    )) as RuntimeEvaluateResponse;
    if (response?.exceptionDetails) {
      const description =
        response.exceptionDetails.exception?.description ?? response.exceptionDetails.text ?? "exception";
      return { skip: false, value: { __err: description } };
    }

    if (response?.result?.type === "undefined") return { skip: true };
    return { skip: false, value: response?.result?.value ?? response?.result?.description ?? null };
  }

  private async wasmLocalNumber(
    value: RuntimeRemoteObject | undefined,
    sessionId: string
  ): Promise<number | null> {
    if (!value) return null;
    if (typeof value.value === "number") return value.value;
    const parsedDescription = UrlBreakpointRegistry.parseIntegerDescription(value.description);
    if (parsedDescription != null) return parsedDescription;
    if (value.objectId == null) return null;
    const valueProperty = (await this.getProperties(value.objectId, sessionId)).find((p) => p.name === "value");
    const innerValue = valueProperty?.value?.value ?? valueProperty?.value?.description;
    if (typeof innerValue === "number") return innerValue;
    if (typeof innerValue === "string") return UrlBreakpointRegistry.parseIntegerDescription(innerValue);
    return null;
  }

  onConsole(params: Record<string, unknown>): void {
    const args = params.args as Array<{ value?: unknown }> | undefined;
    const first = args && args[0] ? args[0].value : undefined;
    if (typeof first !== "string" || !first.startsWith(CAPTURE_MARKER)) return;
    const rest = first.slice(CAPTURE_MARKER.length);
    const sep = rest.indexOf("|");
    if (sep < 0) return;
    const id = rest.slice(0, sep);
    const raw = rest.slice(sep + 1);
    let value: unknown;
    try {
      value = JSON.parse(raw);
    } catch {
      value = raw;
    }
    this.buffer({ id, ts: new Date().toISOString(), value });
  }

  private static captureCondition(id: string, logExpression: string): string {

    const tag = JSON.stringify(`${CAPTURE_MARKER}${id}|`);
    return `(()=>{try{var __r=(${logExpression});if(__r!==undefined)console.log(${tag}+(typeof __r==="string"?__r:JSON.stringify(__r)));}catch(__e){}return false;})()`;
  }

  private buffer(capture: LogpointCapture): void {
    this.captures.push(capture);
    if (this.captures.length > MAX_LOG_CAPTURES) this.captures.shift();
  }

  private static parseIntegerDescription(description: string | undefined): number | null {
    if (!description) return null;
    const tail = description.includes(":") ? description.split(":")[1] : description;
    const cleaned = tail.replace(/n$/, "");
    return /^-?\d+$/.test(cleaned) ? Number(cleaned) : null;
  }

  private static escapeRegex(url: string): string {
    return url.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  private static locationKey(url: string, byteOffset: number, condition: string | undefined): string {
    return `${url} ${byteOffset} ${condition ?? ""}`;
  }
}
