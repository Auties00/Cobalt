import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { LiveToolsContext } from "../../../types/live/tools.js";
import { createLogger } from "../../../utils/logger.js";

const log = createLogger("tools:live:debug");

const sessionIdSchema = z.string().min(1).describe("Target live session id.");

export function registerLiveDebugTools(server: McpServer, context: LiveToolsContext): void {
  const { requireReady, requireSession } = context;

  server.tool(
    "web_live_debug_list_scripts",
    "Read-only. Lists runtime scripts known to a session's debugger, including WASM scripts (scriptLanguage 'WebAssembly' with a codeOffset). Use the filter to find a WASM module by URL.",
    {
      sessionId: sessionIdSchema,
      filter: z.string().optional(),
      limit: z.number().optional().default(200),
    },
    async ({
      sessionId,
      filter,
      limit,
    }: {
      sessionId: string;
      filter?: string;
      limit: number;
    }) => {
      requireReady();
      log.debug(`web_live_debug_list_scripts: id=${sessionId} filter=${filter ?? "none"} limit=${limit}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.listDebuggerScripts(filter, limit);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_eval",
    "Evaluates JavaScript in a session's live runtime and returns the result. Can mutate app state. `target` selects where it runs: 'page' (default; the main page) returns a single result; 'workers' (every worker/shared_worker/service_worker target), 'all' (page + every auto-attached child target), an exact TargetInfo.type (e.g. 'service_worker', 'iframe'), or a specific sessionId return an array of per-target { sessionId, type, url, value | error }. Worker-only globals (e.g. a buffer accumulated by a web_live_debug_set_init_script hook) are only visible with a worker/all target, not 'page'.",
    {
      sessionId: sessionIdSchema,
      expression: z.string(),
      awaitPromise: z.boolean().optional().default(true),
      target: z
        .string()
        .optional()
        .default("page")
        .describe("Where to evaluate: 'page' (default), 'workers', 'all', a TargetInfo.type, or a sessionId."),
    },
    async ({
      sessionId,
      expression,
      awaitPromise,
      target,
    }: {
      sessionId: string;
      expression: string;
      awaitPromise: boolean;
      target: string;
    }) => {
      requireReady();
      log.info(`web_live_debug_eval: id=${sessionId} target=${target} expr="${expression.slice(0, 100)}${expression.length > 100 ? "..." : ""}"`);
      try {
        const session = requireSession(sessionId);
        const result = await session.evaluate(expression, awaitPromise, target);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_debug_eval: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_set_breakpoint_url",
    "Sets a breakpoint by script URL (1-based line/col) on a session.",
    {
      sessionId: sessionIdSchema,
      url: z.string(),
      lineNumber: z.number(),
      columnNumber: z.number().optional().default(1),
      condition: z.string().optional(),
    },
    async ({
      sessionId,
      url,
      lineNumber,
      columnNumber,
      condition,
    }: {
      sessionId: string;
      url: string;
      lineNumber: number;
      columnNumber: number;
      condition?: string;
    }) => {
      requireReady();
      log.info(`web_live_debug_set_breakpoint_url: id=${sessionId} url="${url}" line=${lineNumber}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.setBreakpointByUrl(url, lineNumber, columnNumber, condition);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_debug_set_breakpoint_url: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_set_breakpoint_script",
    "Sets a breakpoint by scriptId (1-based line/col) on a session.",
    {
      sessionId: sessionIdSchema,
      scriptId: z.string(),
      lineNumber: z.number(),
      columnNumber: z.number().optional().default(1),
      condition: z.string().optional(),
    },
    async ({
      sessionId,
      scriptId,
      lineNumber,
      columnNumber,
      condition,
    }: {
      sessionId: string;
      scriptId: string;
      lineNumber: number;
      columnNumber: number;
      condition?: string;
    }) => {
      requireReady();
      log.info(`web_live_debug_set_breakpoint_script: id=${sessionId} scriptId="${scriptId}" line=${lineNumber}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.setBreakpointByScriptId(scriptId, lineNumber, columnNumber, condition);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_debug_set_breakpoint_script: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_set_wasm_breakpoints",
    "Sets a breakpoint at EACH byteOffset in `byteOffsets` (pass a single-element array for one breakpoint), all bound by module URL across the page + the whole pthread worker pool (now and respawned later). byteOffset is MODULE-ABSOLUTE: the function's bodyOffset from get_native_module_metadata used DIRECTLY (do NOT add codeOffset) plus any intra-function instruction offset. With a `logExpression` the breakpoint becomes a non-suspending LOGPOINT: the registry wraps the expression in a condition that console.logs its value (tagged by breakpointId) and returns false, so execution never stalls. A logExpression that reads the operand stack (`$stack`) CANNOT be non-suspending (V8 cannot expose $stack to a condition): it suspends on each hit to read the stack and then resumes, returning a `warning` - prefer targeting an instruction where the value is in a local (`$var0`, `$var1`, ...). Retrieve buffered values via web_live_debug_get_logpoint_captures (each tagged by breakpointId; map back to a site via the returned {byteOffset, breakpointId}). Without a logExpression each breakpoint suspends on hit. `target` scopes binding ('workers' | 'all' | 'page' | a CDP target type); `block` forces/keeps a hit paused. The result reports `boundByType`/`resolvedLocations` so you can see which targets (page vs each worker) resolved the breakpoint, plus any `bindErrors`. A binding that fails because a target was busy or not yet attached is retried when the module next parses there. Remove via web_live_debug_remove_breakpoint per breakpointId.",
    {
      sessionId: sessionIdSchema,
      url: z.string().describe("The WASM module URL (the 'url' field from web_live_debug_list_scripts)."),
      byteOffsets: z.array(z.number()).describe("Module-absolute byte offsets; one breakpoint is set per offset."),
      logExpression: z
        .string()
        .optional()
        .describe("If set, evaluated on each hit and its value buffered. Reads of locals ($var0, ...) are non-suspending; a read of $stack forces a suspend-to-read (returns a warning)."),
      target: z
        .string()
        .optional()
        .describe("Scope binding: 'workers' (worker/shared_worker/service_worker), 'all'/unset (every target), 'page', or an exact CDP target type."),
      block: z
        .boolean()
        .optional()
        .describe("Force a hit to stay paused (true) or resume after capture (false). Default: non-logpoints pause, logpoints resume."),
    },
    async ({
      sessionId,
      url,
      byteOffsets,
      logExpression,
      target,
      block,
    }: {
      sessionId: string;
      url: string;
      byteOffsets: number[];
      logExpression?: string;
      target?: string;
      block?: boolean;
    }) => {
      requireReady();
      log.info(
        `web_live_debug_set_wasm_breakpoints: id=${sessionId} url="${url}" count=${byteOffsets.length} target=${target ?? "all"}`
      );
      try {
        const session = requireSession(sessionId);
        const breakpoints: Array<Record<string, unknown>> = [];
        let ok = 0;
        for (const byteOffset of byteOffsets) {
          try {
            const result = await session.setWasmBreakpoint(url, byteOffset, logExpression, target, block);
            const bound = result.perTarget.filter((t) => t.bound);
            const boundByType: Record<string, number> = {};
            for (const t of bound) boundByType[t.type || "?"] = (boundByType[t.type || "?"] ?? 0) + 1;
            const bindErrors = result.perTarget
              .filter((t) => t.error)
              .slice(0, 8)
              .map((t) => ({ type: t.type, error: t.error }));
            breakpoints.push({
              byteOffset,
              breakpointId: result.breakpointId,
              boundTargets: bound.length,
              boundByType,
              resolvedLocations: result.locations.length,
              ...(bindErrors.length ? { bindErrors } : {}),
              ...(result.warning ? { warning: result.warning } : {}),
            });
            ok++;
          } catch (e) {
            breakpoints.push({ byteOffset, error: e instanceof Error ? e.message : String(e) });
          }
        }
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ count: breakpoints.length, ok, breakpoints }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_get_logpoint_captures",
    "Returns the buffered values captured by non-suspending logpoints (set via web_live_debug_set_wasm_breakpoints with a logExpression). Each entry is { id, ts, value } where id is the breakpointId that produced it. Pass id to filter to one breakpoint, and clear:true to drain the returned entries.",
    {
      sessionId: sessionIdSchema,
      id: z.string().optional().describe("Restrict to captures from this breakpointId."),
      clear: z.boolean().optional().default(false),
    },
    async ({
      sessionId,
      id,
      clear,
    }: {
      sessionId: string;
      id?: string;
      clear: boolean;
    }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = session.getLogpointCaptures({ id, clear });
        return { content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }] };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_read_wasm_memory",
    "Reads a slice of a paused WASM frame's linear memory and returns it as base64. Requires the session to be paused in a WASM frame; pass the callFrameId from web_live_debug_paused_state.",
    {
      sessionId: sessionIdSchema,
      callFrameId: z.string().describe("callFrameId of a paused WASM frame (from web_live_debug_paused_state)."),
      addr: z.number().describe("Linear-memory start address."),
      length: z.number().describe("Number of bytes to read."),
    },
    async ({
      sessionId,
      callFrameId,
      addr,
      length,
    }: {
      sessionId: string;
      callFrameId: string;
      addr: number;
      length: number;
    }) => {
      requireReady();
      log.info(`web_live_debug_read_wasm_memory: id=${sessionId} addr=${addr} len=${length}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.readWasmMemory(callFrameId, addr, length);
        return { content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }] };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_eval_on_frame",
    "Evaluates a JavaScript expression in the context of a specific paused call frame (returns by value). Pass a callFrameId from web_live_debug_paused_state; the expression sees that frame's locals and `this`. Works on JS frames (e.g. read a worker onmessage handler's `e.data`). Return JSON-serializable data (e.g. Array.from(new Uint8Array(buf)) or btoa(...)) since ArrayBuffer/typed-array values do not serialize by value.",
    {
      sessionId: sessionIdSchema,
      callFrameId: z.string().describe("callFrameId of a paused frame (from web_live_debug_paused_state)."),
      expression: z.string().describe("JavaScript expression evaluated in the frame's scope."),
    },
    async ({
      sessionId,
      callFrameId,
      expression,
    }: {
      sessionId: string;
      callFrameId: string;
      expression: string;
    }) => {
      requireReady();
      log.info(`web_live_debug_eval_on_frame: id=${sessionId} expr="${expression.slice(0, 100)}${expression.length > 100 ? "..." : ""}"`);
      try {
        const session = requireSession(sessionId);
        const result = await session.evaluateOnCallFrame(callFrameId, expression);
        return { content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }] };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_set_init_script",
    "Sets a JavaScript init script that runs in matching targets BEFORE their own code executes (omit script to clear). `target` selects where: 'workers' (default; worker + shared_worker + service_worker, injected via Runtime.evaluate while the target is paused at waitForDebugger), 'all' (every auto-attached child target), an exact TargetInfo.type, or 'page'/'all' (additionally registers a Playwright page init script applied on the next page load). Use this to install an instrumentation hook (e.g. wrapping the WebTransport constructor to capture datagrams into globalThis.__wtcap) in the worker that performs network I/O, which a media-side wasm breakpoint cannot reach. Targets spawned AFTER this call receive it at creation; read accumulated state back with web_live_debug_eval(target:'workers'|'all').",
    {
      sessionId: sessionIdSchema,
      script: z.string().optional().describe("JS source to run in each matching target before its own code; omit to clear."),
      target: z.string().optional().default("workers").describe("'workers' (default), 'all', a TargetInfo.type, or 'page'."),
    },
    async ({ sessionId, script, target }: { sessionId: string; script?: string; target: string }) => {
      requireReady();
      log.info(`web_live_debug_set_init_script: id=${sessionId} target=${target} len=${script?.length ?? 0}`);
      try {
        const session = requireSession(sessionId);
        await session.setInitScript(script ?? null, target);
        return { content: [{ type: "text" as const, text: JSON.stringify({ set: script != null, target, length: script?.length ?? 0 }, null, 2) }] };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_remove_breakpoint",
    "Removes a breakpoint by id from a session.",
    {
      sessionId: sessionIdSchema,
      breakpointId: z.string(),
    },
    async ({ sessionId, breakpointId }: { sessionId: string; breakpointId: string }) => {
      requireReady();
      log.info(`web_live_debug_remove_breakpoint: id=${sessionId} bp="${breakpointId}"`);
      try {
        const session = requireSession(sessionId);
        await session.removeBreakpoint(breakpointId);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ removed: breakpointId }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_command",
    "Stepping control for a session's debugger (pause/resume/step).",
    {
      sessionId: sessionIdSchema,
      command: z.enum(["pause", "resume", "step_over", "step_into", "step_out"]),
    },
    async ({
      sessionId,
      command,
    }: {
      sessionId: string;
      command: "pause" | "resume" | "step_over" | "step_into" | "step_out";
    }) => {
      requireReady();
      log.info(`web_live_debug_command: id=${sessionId} command="${command}"`);
      try {
        const session = requireSession(sessionId);
        await session.debuggerCommand(command);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ sessionId, command, ok: true }, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_debug_command: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_paused_state",
    "Read-only. Returns a session's paused debugger state (call stack, scopes, reason).",
    { sessionId: sessionIdSchema },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.getPausedState();
        if (!result) {
          return {
            content: [{ type: "text" as const, text: JSON.stringify({ paused: false }, null, 2) }],
          };
        }
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ paused: true, ...(result as object) }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_serve_wasm",
    "Serves a patched WASM binary (a file produced by patch_native_module) in place of any request whose URL contains 'urlSubstring', via CDP Fetch interception. The replacement applies to subsequent loads of the module; reload the page to pick it up. Set clear=true to remove all replacements and disable interception.",
    {
      sessionId: sessionIdSchema,
      urlSubstring: z.string().describe("Substring of the WASM URL to replace (e.g. the module's hashed filename)."),
      path: z.string().optional().describe("Path to the patched .wasm file (from patch_native_module). Omit with clear=true."),
      clear: z.boolean().optional().default(false).describe("Remove all serve replacements and disable interception."),
    },
    async ({
      sessionId,
      urlSubstring,
      path,
      clear,
    }: {
      sessionId: string;
      urlSubstring: string;
      path?: string;
      clear: boolean;
    }) => {
      requireReady();
      log.info(`web_live_serve_wasm: id=${sessionId} url~="${urlSubstring}" clear=${clear}`);
      try {
        const session = requireSession(sessionId);
        if (clear) {
          await session.clearWasmReplacements();
          return { content: [{ type: "text" as const, text: JSON.stringify({ cleared: true }, null, 2) }] };
        }
        if (!path) throw new Error("path is required unless clear=true");
        await session.serveWasmReplacement(urlSubstring, path);
        return { content: [{ type: "text" as const, text: JSON.stringify({ serving: path, forUrlSubstring: urlSubstring }, null, 2) }] };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_cdp_endpoint",
    "Returns the raw Chrome DevTools Protocol port that Chromium is listening on for this session. External clients (LLMs, custom scripts, debuggers) connect to http://localhost:<port>/json/version for browser metadata and the browser-level WebSocket URL, or to http://localhost:<port>/json for per-target WebSocket URLs (page, worker, service worker — each independently attachable for raw CDP). Useful when the page-level Playwright session cannot reach a capability — worker target multiplexing, Fetch.requestPaused with arbitrary handlers, child-target-bound Runtime.evaluate, etc.",
    {
      sessionId: sessionIdSchema,
    },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const port = session.getCdpPort();
        if (port == null) {
          return {
            content: [{ type: "text" as const, text: JSON.stringify({ port: null, message: "browser not running" }, null, 2) }],
          };
        }
        return {
          content: [
            {
              type: "text" as const,
              text: JSON.stringify(
                {
                  port,
                  http: `http://127.0.0.1:${port}`,
                  versionUrl: `http://127.0.0.1:${port}/json/version`,
                  targetsUrl: `http://127.0.0.1:${port}/json`,
                },
                null,
                2
              ),
            },
          ],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );
}
