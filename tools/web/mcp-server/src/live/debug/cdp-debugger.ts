import type { BrowserContext, CDPSession, Page } from "playwright";
import { readFile } from "node:fs/promises";
import type {
  DebugCommand,
  DebuggerScriptParsedEvent,
  DebuggerSetBreakpointByUrlResponse,
  DebuggerSetBreakpointResponse,
  DebugScriptInfo,
  EvaluateResult,
  PausedState,
  RuntimeEvaluateResponse,
  SetBreakpointResult,
  SetWasmBreakpointResult,
  WasmMemoryReadResult,
} from "../../types/live/debug.js";
import { createLogger } from "../../utils/logger.js";
import { CdpMux } from "./cdp-mux.js";

const log = createLogger("live:debugger");

function isScriptParsedEvent(value: unknown): value is DebuggerScriptParsedEvent {
  if (value == null || typeof value !== "object") return false;
  const payload = value as Record<string, unknown>;
  return (
    typeof payload.scriptId === "string" &&
    typeof payload.startLine === "number" &&
    typeof payload.startColumn === "number" &&
    typeof payload.endLine === "number" &&
    typeof payload.endColumn === "number"
  );
}

export class LiveCdpDebugger {
  private cdp: CDPSession | null = null;
  private scriptParsedListenerAttached = false;
  private readonly debugScripts = new Map<string, DebugScriptInfo>();
  private lastPausedState: PausedState | null = null;
  private fetchEnabled = false;
  private readonly replacements = new Map<string, string>();
  private readonly requireContext: () => BrowserContext;
  private readonly requirePage: () => Page;
  private readonly mux: CdpMux;

  constructor(
    requireContext: () => BrowserContext,
    requirePage: () => Page,
    getCdpPort: () => number | null
  ) {
    this.requireContext = requireContext;
    this.requirePage = requirePage;
    this.mux = new CdpMux(getCdpPort);
  }

  async detach(): Promise<void> {
    this.mux.close();
    if (!this.cdp) return;
    log.info("detaching CDP debugger session");
    await this.cdp.detach().catch(() => undefined);
    this.cdp = null;
    this.scriptParsedListenerAttached = false;
    this.fetchEnabled = false;
    this.debugScripts.clear();
    this.replacements.clear();
  }

  async serveReplacement(urlSubstring: string, filePath: string): Promise<void> {
    const cdp = await this.ensureCdp();
    this.replacements.set(urlSubstring, filePath);
    if (this.fetchEnabled) return;

    cdp.on("Fetch.requestPaused", async (event: unknown) => {
      const e = event as { requestId: string; request: { url: string } };
      try {
        const match = [...this.replacements.entries()].find(([sub]) => e.request.url.includes(sub));
        if (match) {
          const body = await readFile(match[1]);
          await cdp.send("Fetch.fulfillRequest", {
            requestId: e.requestId,
            responseCode: 200,
            responseHeaders: [{ name: "Content-Type", value: "application/wasm" }],
            body: body.toString("base64"),
          });
          return;
        }
      } catch {

      }
      await cdp.send("Fetch.continueRequest", { requestId: e.requestId }).catch(() => undefined);
    });
    await cdp.send("Fetch.enable", { patterns: [{ urlPattern: "*wasm*", requestStage: "Request" }] });
    this.fetchEnabled = true;
  }

  async clearReplacements(): Promise<void> {
    this.replacements.clear();
    if (this.fetchEnabled && this.cdp) {
      await this.cdp.send("Fetch.disable").catch(() => undefined);
      this.fetchEnabled = false;
    }
  }

  async listScripts(filter?: string, limit: number = 200): Promise<DebugScriptInfo[]> {
    await this.mux.ensure();
    const normalizedFilter = filter ? filter.toLowerCase() : null;
    return this.mux
      .listScripts()
      .filter((script) =>
        normalizedFilter ? script.url.toLowerCase().includes(normalizedFilter) : true
      )
      .sort((a, b) => a.url.localeCompare(b.url))
      .slice(0, Math.max(1, limit));
  }

  async evaluate(
    expression: string,
    awaitPromise: boolean = true,
    target: string = "page"
  ): Promise<EvaluateResult | Array<{ sessionId: string; type: string; url: string; value?: unknown; error?: string }>> {
    // Non-"page" targets resolve over the auto-attach mux (workers/iframes/service workers by selector).
    // "all" additionally includes the page, evaluated over its own Playwright CDP session below.
    if (target !== "page") {
      await this.mux.ensure();
      const results = await this.mux.evalOnTargets(expression, target, awaitPromise);
      if (target === "all") {
        const pageRes = (await this.evaluate(expression, awaitPromise, "page")) as EvaluateResult;
        return [
          {
            sessionId: "page",
            type: "page",
            url: this.requirePage().url(),
            value: pageRes.value,
            error: pageRes.exception ?? undefined,
          },
          ...results,
        ];
      }
      return results;
    }
    const cdp = await this.ensureCdp();
    // Do NOT set replMode: with replMode V8 returns the un-awaited Promise (which serializes to {} under
    // returnByValue) instead of honoring awaitPromise, so async expressions silently yielded {}.
    const responseRaw = await cdp.send("Runtime.evaluate", {
      expression,
      awaitPromise,
      returnByValue: true,
      generatePreview: true,
    });
    const response = responseRaw as RuntimeEvaluateResponse;
    const result = response.result;
    return {
      resultType: result?.type ?? "undefined",
      value: result?.value,
      description: result?.description ?? null,
      unserializableValue: result?.unserializableValue ?? null,
      exception:
        response.exceptionDetails?.text ??
        response.exceptionDetails?.exception?.description ??
        null,
    };
  }

  async setBreakpointByUrl(
    url: string,
    lineNumberOneBased: number,
    columnNumberOneBased: number = 1,
    condition?: string
  ): Promise<SetBreakpointResult> {
    const cdp = await this.ensureCdp();
    const responseRaw = await cdp.send("Debugger.setBreakpointByUrl", {
      url,
      lineNumber: Math.max(0, lineNumberOneBased - 1),
      columnNumber: Math.max(0, columnNumberOneBased - 1),
      condition,
    });
    const response = responseRaw as DebuggerSetBreakpointByUrlResponse;
    return {
      breakpointId: response.breakpointId,
      locations: response.locations ?? [],
    };
  }

  async setBreakpointByScriptId(
    scriptId: string,
    lineNumberOneBased: number,
    columnNumberOneBased: number = 1,
    condition?: string
  ): Promise<SetBreakpointResult> {
    await this.mux.ensure();
    const sessionId = this.mux.sessionForScript(scriptId);
    const responseRaw = await this.mux.send(
      "Debugger.setBreakpoint",
      {
        location: {
          scriptId,
          lineNumber: Math.max(0, lineNumberOneBased - 1),
          columnNumber: Math.max(0, columnNumberOneBased - 1),
        },
        condition,
      },
      sessionId
    );
    const response = responseRaw as DebuggerSetBreakpointResponse;
    if (sessionId) this.mux.rememberBreakpoint(response.breakpointId, sessionId);
    const location = response.actualLocation ? [response.actualLocation] : [];
    return {
      breakpointId: response.breakpointId,
      locations: location,
    };
  }

  /**
   * Sets a wasm breakpoint/logpoint bound by module URL so it applies to every instance of the module
   * (the page plus every pthread worker, now and respawned later). {@code byteOffset} is module-absolute.
   * A {@code logExpression} makes it a buffered logpoint (retrieve via {@link getLogpointCaptures}); a
   * {@code condition} that returns a falsy value logs without ever suspending. Returns a registry id
   * ("wbp_N") standing for the breakpoint across every attached session.
   */
  async setWasmBreakpoint(
    url: string,
    byteOffset: number,
    logExpression?: string,
    target?: string,
    block?: boolean
  ): Promise<SetWasmBreakpointResult> {
    await this.mux.ensure();
    // Attach every worker currently alive (including nested pthread workers the non-recursive auto-attach
    // raced past) so the breakpoint binds to all instances now; later targets pick it up from the registry.
    await this.mux.reconcileTargets();
    // Optionally scope binding to a CDP target class (mirrors setInitScript): "workers" -> the worker
    // targets, "page" -> the page, "all"/unset -> every target.
    const targetTypes =
      !target || target === "all"
        ? null
        : target === "workers"
          ? new Set(["worker", "shared_worker", "service_worker"])
          : new Set([target]);
    // With a logExpression the registry installs a non-suspending capture (console.log + return false)
    // unless it reads $stack; block controls whether a suspending hit stays paused.
    const { id, locations, perTarget, warning } = await this.mux.addUrlBreakpoint({
      url,
      byteOffset,
      logExpression,
      block,
      targetTypes,
    });
    return { breakpointId: id, locations, perTarget, warning };
  }

  /**
   * Returns buffered logpoint captures, optionally filtered to one breakpoint id and/or clearing the
   * returned entries.
   */
  getLogpointCaptures(options: { id?: string; clear?: boolean } = {}) {
    return this.mux.getLogCaptures(options);
  }

  /**
   * Sets a JavaScript init script run in matching targets before their own code executes. Pass {@code null} to
   * clear. {@code target} selects where it lands: "workers" (worker + shared_worker + service_worker, injected
   * via {@code Runtime.evaluate} while the target is paused at {@code waitForDebugger}), "all" (every
   * auto-attached child target), or an exact {@code TargetInfo.type}; "page"/"all" additionally registers a
   * Playwright init script that runs on the next page load. Use this to install an instrumentation hook (e.g.
   * wrapping the {@code WebTransport} constructor in the pthread worker that performs network I/O) before any
   * target code runs.
   */
  async setInitScript(script: string | null, target: string = "workers"): Promise<void> {
    await this.mux.ensure();
    const types =
      target === "all"
        ? null
        : target === "workers"
          ? ["worker", "shared_worker", "service_worker"]
          : [target];
    this.mux.setInitScript(script, types);
    if (script != null && (target === "page" || target === "all")) {
      try {
        await this.requirePage().addInitScript({ content: script });
      } catch {
        // page init script is best-effort; the child-target injection above is the primary path
      }
    }
  }

  async readWasmMemory(callFrameId: string, addr: number, len: number): Promise<WasmMemoryReadResult> {
    await this.mux.ensure();
    const sessionId = this.mux.getLastPaused()?.sessionId;
    const expression = `(() => {
      const mem = (typeof memories !== "undefined" && memories[0]) || (typeof memory !== "undefined" && memory);
      if (!mem) return { __err: "no memory in frame scope" };
      const view = new Uint8Array(mem.buffer, ${Math.max(0, addr)}, ${Math.max(0, len)});
      let bin = "";
      for (let i = 0; i < view.length; i += 0x8000) {
        bin += String.fromCharCode.apply(null, view.subarray(i, i + 0x8000));
      }
      return { __b64: btoa(bin) };
    })()`;
    try {
      const responseRaw = await this.mux.send(
        "Debugger.evaluateOnCallFrame",
        { callFrameId, expression, returnByValue: true },
        sessionId
      );
      const response = responseRaw as RuntimeEvaluateResponse;
      const value = response.result?.value as { __b64?: string; __err?: string } | undefined;
      if (response.exceptionDetails) {
        return { addr, len, base64: null, error: response.exceptionDetails.text ?? "evaluation failed" };
      }
      if (value?.__err) return { addr, len, base64: null, error: value.__err };
      return { addr, len, base64: value?.__b64 ?? null };
    } catch (err) {
      return { addr, len, base64: null, error: err instanceof Error ? err.message : String(err) };
    }
  }

  async evaluateOnCallFrame(callFrameId: string, expression: string): Promise<EvaluateResult> {
    await this.mux.ensure();
    const sessionId = this.mux.getLastPaused()?.sessionId;
    const responseRaw = await this.mux.send(
      "Debugger.evaluateOnCallFrame",
      { callFrameId, expression, returnByValue: true, generatePreview: true },
      sessionId
    );
    const response = responseRaw as RuntimeEvaluateResponse;
    const result = response.result;
    return {
      resultType: result?.type ?? "undefined",
      value: result?.value,
      description: result?.description ?? null,
      unserializableValue: result?.unserializableValue ?? null,
      exception:
        response.exceptionDetails?.text ??
        response.exceptionDetails?.exception?.description ??
        null,
    };
  }

  async removeBreakpoint(breakpointId: string): Promise<void> {
    await this.mux.ensure();
    // setWasmBreakpoint returns a registry id ("wbp_N") standing for the breakpoint across every attached
    // session; route it to the registry, which tears down the shared V8 binding when its last ref is gone.
    if (breakpointId.startsWith("wbp_")) {
      await this.mux.removeUrlBreakpoint(breakpointId);
      return;
    }
    const sessionId = this.mux.sessionForBreakpoint(breakpointId);
    await this.mux.send("Debugger.removeBreakpoint", { breakpointId }, sessionId);
  }

  async command(command: DebugCommand): Promise<void> {
    await this.mux.ensure();

    const sessionId = this.mux.getLastPaused()?.sessionId;
    if (command === "pause") {
      await this.mux.send("Debugger.pause", {}, sessionId);
      return;
    }
    if (command === "resume") {
      this.mux.clearPaused();
      await this.mux.send("Debugger.resume", {}, sessionId);
      return;
    }
    if (command === "step_over") {
      await this.mux.send("Debugger.stepOver", {}, sessionId);
      return;
    }
    if (command === "step_into") {
      await this.mux.send("Debugger.stepInto", {}, sessionId);
      return;
    }
    await this.mux.send("Debugger.stepOut", {}, sessionId);
  }

  async getPausedState(): Promise<PausedState | null> {
    await this.mux.ensure();
    const paused = this.mux.getLastPaused();
    if (!paused) return null;
    const sessionId = paused.sessionId;
    const enriched: PausedState = {
      reason: paused.reason,
      ts: paused.ts,
      callFrames: paused.callFrames.map((f) => ({ ...f, scopeChain: f.scopeChain, variables: [] })),
    };

    for (const frame of enriched.callFrames) {
      if (!frame.scopeChain) continue;

      const cap = frame.scriptLanguage === "WebAssembly" ? 200 : 20;
      const scopeVars: Array<{ name: string; value: string; type: string }> = [];
      for (const scope of frame.scopeChain) {
        if (scope.type === "global") continue;
        if (!scope.object?.objectId) continue;
        try {
          const propsResult = await this.mux.send(
            "Runtime.getProperties",
            {
              objectId: scope.object.objectId,
              ownProperties: true,
              generatePreview: true,
            },
            sessionId
          );
          const props = propsResult as {
            result: Array<{
              name: string;
              value?: { type: string; subtype?: string; value?: unknown; description?: string; objectId?: string };
            }>;
          };
          for (const prop of (props.result ?? []).slice(0, cap)) {
            const pv = prop.value;
            let valueStr = pv?.description ?? String(pv?.value ?? "undefined");

            const isWasmValue =
              pv?.type === "object" &&
              pv.objectId != null &&
              (pv.subtype === "wasmvalue" || /^(i32|i64|f32|f64|v128)$/.test(pv.description ?? ""));
            if (isWasmValue && pv?.objectId) {
              try {
                const inner = (await this.mux.send(
                  "Runtime.getProperties",
                  { objectId: pv.objectId, ownProperties: true, generatePreview: false },
                  sessionId
                )) as { result: Array<{ name: string; value?: { value?: unknown; description?: string } }> };
                const valProp = (inner.result ?? []).find((p) => p.name === "value");
                if (valProp) {
                  const n = valProp.value?.value ?? valProp.value?.description ?? "";
                  valueStr = `${pv.description}:${n}`;
                }
              } catch {

              }
            }
            scopeVars.push({
              name: prop.name,
              value: valueStr,
              type: pv?.description ?? pv?.type ?? "undefined",
            });
          }
        } catch {  }
      }
      frame.variables = scopeVars;
    }

    return enriched;
  }

  private async ensureCdp(): Promise<CDPSession> {
    if (this.cdp) return this.cdp;
    const context = this.requireContext();
    const page = this.requirePage();
    const cdp = await context.newCDPSession(page);
    if (!this.scriptParsedListenerAttached) {
      cdp.on("Debugger.scriptParsed", (event: unknown) => {
        if (!isScriptParsedEvent(event)) return;
        this.debugScripts.set(event.scriptId, {
          scriptId: event.scriptId,
          url: event.url ?? "",
          startLine: event.startLine,
          startColumn: event.startColumn,
          endLine: event.endLine,
          endColumn: event.endColumn,
          hash: event.hash ?? null,
          executionContextId: event.executionContextId ?? null,
          length: event.length ?? null,
          scriptLanguage: event.scriptLanguage ?? null,
          codeOffset: event.codeOffset ?? null,
        });
      });
      cdp.on("Debugger.paused", (event: unknown) => {
        const payload = event as Record<string, unknown>;
        const callFrames = payload.callFrames as Array<Record<string, unknown>> | undefined;
        const reason = (payload.reason as string) ?? "other";

        this.lastPausedState = {
          reason,
          callFrames: (callFrames ?? []).slice(0, 20).map((frame) => {
            const location = frame.location as Record<string, unknown> | undefined;
            const scopeChain = frame.scopeChain as Array<Record<string, unknown>> | undefined;
            const scriptId = (location?.scriptId as string) ?? "";
            const scriptLanguage = this.debugScripts.get(scriptId)?.scriptLanguage ?? undefined;
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
      });
      cdp.on("Debugger.resumed", () => {
        this.lastPausedState = null;
      });
      this.scriptParsedListenerAttached = true;
    }
    await cdp.send("Runtime.enable");
    await cdp.send("Debugger.enable");
    this.cdp = cdp;
    return cdp;
  }
}
