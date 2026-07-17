import type {
  BreakpointLocation,
  BrowserTargetInfo,
  BrowserVersionInfo,
  CdpMessage,
  CdpScope,
  DebugScriptInfo,
  LogpointCapture,
  PausedState,
  SessionTarget,
  TargetInfo,
  UrlBreakpointAddResult,
  UrlBreakpointSpec,
} from "../../types/live/debug.js";
import { createLogger } from "../../utils/logger.js";
import { UrlBreakpointRegistry } from "./url-breakpoint-registry.js";

const log = createLogger("live:cdp-mux");

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
}

interface TrackedScript extends DebugScriptInfo {
  sessionId: string;
}

interface TrackedPause extends PausedState {
  sessionId: string;
}

export class CdpMux {
  private ws: WebSocket | null = null;
  private connecting: Promise<void> | null = null;
  private nextId = 1;
  private readonly pending = new Map<number, PendingRequest>();
  private readonly sessions = new Set<string>();
  private readonly sessionTargets = new Map<string, SessionTarget>();
  private readonly scripts = new Map<string, TrackedScript>();

  private readonly breakpointSessions = new Map<string, string>();

  private readonly breakpoints: UrlBreakpointRegistry;
  private lastPaused: TrackedPause | null = null;
  private initScript: string | null = null;
  private initScriptTypes: Set<string> | null = null;
  private readonly getPort: () => number | null;

  constructor(getPort: () => number | null) {
    this.getPort = getPort;
    this.breakpoints = new UrlBreakpointRegistry(
      (method, params, sessionId) => this.rawSend(method, params, sessionId),
      () => this.sessions,
      (sessionId) => {
        const t = this.sessionTargets.get(sessionId);
        return t ? { type: t.type, url: t.url } : undefined;
      }
    );
  }

  isConnected(): boolean {
    return this.ws != null && this.ws.readyState === WebSocket.OPEN;
  }

  async ensure(): Promise<void> {
    if (this.isConnected()) return;
    if (this.connecting) return this.connecting;
    this.connecting = this.connect().finally(() => {
      this.connecting = null;
    });
    return this.connecting;
  }

  private async connect(): Promise<void> {
    const port = this.getPort();
    if (!port) throw new Error("CDP mux: session has no remote-debugging port");
    const versionRes = await fetch(`http://127.0.0.1:${port}/json/version`);
    if (!versionRes.ok) {
      throw new Error(`CDP mux: /json/version returned ${versionRes.status}`);
    }
    const version = (await versionRes.json()) as BrowserVersionInfo;
    const wsUrl = version.webSocketDebuggerUrl;
    if (!wsUrl) throw new Error("CDP mux: no webSocketDebuggerUrl from browser");

    const ws = new WebSocket(wsUrl);
    await new Promise<void>((resolve, reject) => {
      ws.addEventListener("open", () => resolve(), { once: true });
      ws.addEventListener("error", () => reject(new Error("CDP mux: socket error")), { once: true });
    });
    ws.addEventListener("message", (ev: MessageEvent) => {
      try {
        this.onMessage(JSON.parse(String(ev.data)) as CdpMessage);
      } catch (err) {
        log.warn(`CDP mux: bad message: ${err instanceof Error ? err.message : String(err)}`);
      }
    });
    ws.addEventListener("close", () => {
      this.ws = null;
      this.sessions.clear();
      this.sessionTargets.clear();
      this.scripts.clear();

      this.breakpoints.clear();
      this.lastPaused = null;
      for (const req of this.pending.values()) req.reject(new Error("CDP mux: socket closed"));
      this.pending.clear();
    });
    this.ws = ws;

    await this.rawSend("Target.setDiscoverTargets", { discover: true });

    await this.rawSend("Target.setAutoAttach", {
      autoAttach: true,
      waitForDebuggerOnStart: true,
      flatten: true,
    });
    log.info("CDP mux: connected and auto-attaching to page + worker targets");
  }

  private rawSend(method: string, params: Record<string, unknown> = {}, sessionId?: string): Promise<unknown> {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return Promise.reject(new Error("CDP mux: not connected"));
    }
    const id = this.nextId++;
    const message: Record<string, unknown> = { id, method, params };
    if (sessionId) message.sessionId = sessionId;
    return new Promise<unknown>((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.ws!.send(JSON.stringify(message));
    });
  }

  async send(method: string, params: Record<string, unknown> = {}, sessionId?: string): Promise<unknown> {
    await this.ensure();
    return this.rawSend(method, params, sessionId);
  }

  private async onMessage(msg: CdpMessage): Promise<void> {
    if (msg.id != null && this.pending.has(msg.id)) {
      const req = this.pending.get(msg.id)!;
      this.pending.delete(msg.id);
      if (msg.error) req.reject(new Error(msg.error.message ?? "CDP error"));
      else req.resolve(msg.result);
      return;
    }
    if (!msg.method) return;
    const sessionId = msg.sessionId;
    const params = msg.params ?? {};
    switch (msg.method) {
      case "Target.attachedToTarget":
        await this.onAttached(params);
        break;
      case "Target.detachedFromTarget": {
        const sid = params.sessionId as string | undefined;
        if (sid) this.forgetSession(sid);
        break;
      }
      case "Debugger.scriptParsed":
        if (sessionId) this.onScriptParsed(params, sessionId);
        break;
      case "Debugger.paused":
        if (sessionId) this.onPaused(params, sessionId);
        break;
      case "Debugger.resumed":
        if (this.lastPaused && this.lastPaused.sessionId === sessionId) this.lastPaused = null;
        break;
      case "Runtime.consoleAPICalled":
        this.breakpoints.onConsole(params);
        break;
      default:
        break;
    }
  }

  private async onAttached(params: Record<string, unknown>): Promise<void> {
    const childSession = params.sessionId as string | undefined;
    if (!childSession) return;
    const ti = params.targetInfo as TargetInfo | undefined;
    await this.setupSession(childSession, ti?.type ?? "unknown", ti?.url ?? "", ti?.targetId ?? "");
  }

  private forgetSession(sessionId: string): void {
    log.info(
      `CDP mux: forget session=${sessionId} tid=${this.sessionTargets.get(sessionId)?.targetId ?? "?"} url=${(this.sessionTargets.get(sessionId)?.url ?? "").slice(0, 60)}`
    );
    this.sessions.delete(sessionId);
    this.sessionTargets.delete(sessionId);
    this.breakpoints.forgetSession(sessionId);
    for (const [bpId, sid] of [...this.breakpointSessions]) {
      if (sid === sessionId) this.breakpointSessions.delete(bpId);
    }
  }

  private async setupSession(sessionId: string, type: string, url: string, targetId: string): Promise<void> {
    if (this.sessions.has(sessionId)) return;
    this.sessions.add(sessionId);
    this.sessionTargets.set(sessionId, { type, url, targetId });
    log.info(`CDP mux: attached target type=${type} session=${sessionId} url=${url.slice(0, 120)}`);
    await this.rawSend("Runtime.enable", {}, sessionId).catch(() => undefined);
    await this.rawSend("Debugger.enable", {}, sessionId).catch(() => undefined);
    await this.rawSend(
      "Target.setAutoAttach",
      { autoAttach: true, waitForDebuggerOnStart: true, flatten: true },
      sessionId
    ).catch(() => undefined);
    await this.breakpoints.bindSession(sessionId);
    // Inject the configured init script while the target is still paused at waitForDebugger, so the hook
    // runs in this target's realm before any of its own code (e.g. before a WebTransport is constructed).
    // Honour the type filter so it only lands in the requested target types (e.g. workers only).
    if (this.initScript && (this.initScriptTypes === null || this.initScriptTypes.has(type))) {
      await this.rawSend(
        "Runtime.evaluate",
        { expression: this.initScript, includeCommandLineAPI: false, returnByValue: true },
        sessionId
      ).catch(() => undefined);
    }
    await this.rawSend("Runtime.runIfWaitingForDebugger", {}, sessionId).catch(() => undefined);
  }

  async reconcileTargets(): Promise<void> {
    if (!this.isConnected()) return;
    const port = this.getPort();
    if (!port) return;
    let targets: BrowserTargetInfo[] = [];
    try {
      const res = await fetch(`http://127.0.0.1:${port}/json`);
      if (!res.ok) return;
      targets = (await res.json()) as BrowserTargetInfo[];
    } catch {
      return;
    }
    log.info(`CDP mux: reconcile /json top-level-targets=${targets.length} tracked-sessions=${this.sessions.size}`);
    const known = new Set([...this.sessionTargets.values()].map((t) => t.targetId).filter(Boolean));
    for (const t of targets) {
      const id = t.id;
      if (!id || known.has(id)) continue;
      if (t.type !== "worker" && t.type !== "shared_worker") continue;

      let r = (await this.rawSend("Target.attachToTarget", { targetId: id, flatten: true })
        .catch(() => undefined)) as { sessionId?: string } | undefined;
      if (!r?.sessionId) {
        await this.rawSend("Target.detachFromTarget", { targetId: id }).catch(() => undefined);
        r = (await this.rawSend("Target.attachToTarget", { targetId: id, flatten: true })
          .catch(() => undefined)) as { sessionId?: string } | undefined;
      }
      if (r?.sessionId) await this.setupSession(r.sessionId, t.type ?? "worker", t.url ?? "", id);
    }
  }

  async addUrlBreakpoint(spec: UrlBreakpointSpec): Promise<UrlBreakpointAddResult> {
    return this.breakpoints.add(spec);
  }

  async removeUrlBreakpoint(id: string): Promise<void> {
    await this.breakpoints.remove(id);
  }

  listTargets(): Array<{ sessionId: string; type: string; url: string }> {
    return [...this.sessionTargets.entries()].map(([sessionId, t]) => ({
      sessionId,
      type: t.type,
      url: t.url,
    }));
  }

  setInitScript(script: string | null, types: string[] | null = null): void {
    this.initScript = script;
    this.initScriptTypes = types && types.length ? new Set(types) : null;
  }

  /**
   * Evaluates an expression in each attached child-target session matching {@code selector} and returns one
   * result per target. {@code selector} is "all" (every non-page/non-browser target), "workers" (worker +
   * shared_worker + service_worker), an exact {@code TargetInfo.type} (e.g. "service_worker", "iframe"), or a
   * specific sessionId. The page target is never matched here; the caller evaluates the page over its own
   * Playwright CDP session.
   */
  async evalOnTargets(
    expression: string,
    selector: string,
    awaitPromise: boolean = true
  ): Promise<Array<{ sessionId: string; type: string; url: string; value?: unknown; error?: string }>> {
    await this.ensure();
    await this.reconcileTargets();
    const matches = (type: string, sessionId: string): boolean => {
      if (this.sessionTargets.has(selector)) return sessionId === selector;
      if (selector === "all") return type !== "page" && type !== "browser";
      if (selector === "workers") return type === "worker" || type === "shared_worker" || type === "service_worker";
      return type === selector;
    };
    // Evaluate every matching target in parallel, each behind its own timeout, so a single unresponsive or
    // terminating worker yields an error entry instead of blocking the whole call (and dropping the transport).
    const targets = [...this.sessionTargets.entries()].filter(([sid, t]) => matches(t.type, sid));
    const evalOne = async (sessionId: string, t: { type: string; url: string }) => {
      try {
        const timeout = new Promise<never>((_, reject) =>
          setTimeout(() => reject(new Error("eval timeout (target unresponsive)")), 4000)
        );
        const r = (await Promise.race([
          this.rawSend("Runtime.evaluate", { expression, returnByValue: true, awaitPromise }, sessionId),
          timeout,
        ])) as { result?: { value?: unknown }; exceptionDetails?: { text?: string } };
        if (r.exceptionDetails) return { sessionId, type: t.type, url: t.url, error: r.exceptionDetails.text ?? "eval error" };
        return { sessionId, type: t.type, url: t.url, value: r.result?.value };
      } catch (err) {
        return { sessionId, type: t.type, url: t.url, error: err instanceof Error ? err.message : String(err) };
      }
    };
    return Promise.all(targets.map(([sid, t]) => evalOne(sid, t)));
  }

  private onScriptParsed(e: Record<string, unknown>, sessionId: string): void {
    const scriptId = e.scriptId as string | undefined;
    if (!scriptId) return;
    const url = (e.url as string) ?? "";
    // Late-bind any not-yet-bound url breakpoint now that a matching script (re)appeared in this target.
    void this.breakpoints.onScriptParsed(sessionId, url).catch(() => undefined);
    this.scripts.set(scriptId, {
      scriptId,
      url,
      startLine: (e.startLine as number) ?? 0,
      startColumn: (e.startColumn as number) ?? 0,
      endLine: (e.endLine as number) ?? 0,
      endColumn: (e.endColumn as number) ?? 0,
      hash: (e.hash as string) ?? null,
      executionContextId: (e.executionContextId as number) ?? null,
      length: (e.length as number) ?? null,
      scriptLanguage: (e.scriptLanguage as string) ?? null,
      codeOffset: (e.codeOffset as number) ?? null,
      sessionId,
    });
  }

  private onPaused(params: Record<string, unknown>, sessionId: string): void {
    const callFrames = (params.callFrames as Array<Record<string, unknown>>) ?? [];
    const reason = (params.reason as string) ?? "other";
    const top = callFrames[0] as Record<string, unknown> | undefined;

    const hit = (params.hitBreakpoints as string[]) ?? [];
    if (top) {
      const cfid = (top.callFrameId as string) ?? "";
      const scopeChain = (top.scopeChain as CdpScope[]) ?? [];
      const disposition = this.breakpoints.handlePaused(hit, { callFrameId: cfid, scopeChain }, sessionId);
      if (disposition.matched && !disposition.block) return;
    }

    const topLoc = top?.location as Record<string, unknown> | undefined;
    const tgt = this.sessionTargets.get(sessionId);
    log.info(
      `CDP mux: PAUSED session=${sessionId} target=${tgt?.type ?? "?"} reason=${reason} ` +
        `topFn=${String(top?.functionName ?? "")} scriptId=${String(topLoc?.scriptId ?? "")} ` +
        `col=${String(topLoc?.columnNumber ?? "")}`
    );
    this.lastPaused = {
      reason,
      sessionId,
      callFrames: callFrames.slice(0, 20).map((frame) => {
        const location = frame.location as Record<string, unknown> | undefined;
        const scopeChain = frame.scopeChain as Array<Record<string, unknown>> | undefined;
        const scriptId = (location?.scriptId as string) ?? "";
        const scriptLanguage = this.scripts.get(scriptId)?.scriptLanguage ?? undefined;
        return {
          callFrameId: (frame.callFrameId as string) ?? "",
          functionName: (frame.functionName as string) ?? "",
          scriptId,
          url: (frame.url as string) ?? "",
          lineNumber: ((location?.lineNumber as number) ?? 0) + 1,
          columnNumber: ((location?.columnNumber as number) ?? 0) + 1,
          scriptLanguage: scriptLanguage ?? undefined,
          scopeChain: (scopeChain ?? []).map((scope) => ({
            type: (scope.type as string) ?? "unknown",
            object: scope.object as { objectId?: string } | undefined,
          })),
          variables: [],
        };
      }),
      ts: new Date().toISOString(),
    };
  }

  listScripts(): TrackedScript[] {
    return [...this.scripts.values()];
  }

  sessionForScript(scriptId: string): string | undefined {
    return this.scripts.get(scriptId)?.sessionId;
  }

  getLastPaused(): TrackedPause | null {
    return this.lastPaused;
  }

  getLogCaptures(options: { id?: string; clear?: boolean } = {}): LogpointCapture[] {
    return this.breakpoints.getCaptures(options);
  }

  rememberBreakpoint(breakpointId: string, sessionId: string): void {
    this.breakpointSessions.set(breakpointId, sessionId);
  }

  sessionForBreakpoint(breakpointId: string): string | undefined {
    return this.breakpointSessions.get(breakpointId);
  }

  clearPaused(): void {
    this.lastPaused = null;
  }

  close(): void {
    if (this.ws) {
      try {
        this.ws.close();
      } catch {

      }
    }
    this.ws = null;
    this.sessions.clear();
    this.sessionTargets.clear();
    this.scripts.clear();
    this.breakpointSessions.clear();
    this.breakpoints.clear();
    this.lastPaused = null;
  }
}
