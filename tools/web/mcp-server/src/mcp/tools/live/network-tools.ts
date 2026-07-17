import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { LiveToolsContext } from "../../../types/live/tools.js";
import { createLogger } from "../../../utils/logger.js";

const log = createLogger("tools:live:network");

const sessionIdSchema = z.string().min(1).describe("Target live session id.");

export function registerLiveNetworkTools(
  server: McpServer,
  context: LiveToolsContext
): void {
  const { requireReady, requireSession } = context;

  server.tool(
    "web_live_network_start",
    "Starts capturing WebSocket frames and HTTP requests for a session via CDP.",
    { sessionId: sessionIdSchema },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      log.info(`web_live_network_start: id=${sessionId}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.startNetworkCapture();
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_network_start: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_network_stop",
    "Stops network capture for a session.",
    { sessionId: sessionIdSchema },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      log.info(`web_live_network_stop: id=${sessionId}`);
      try {
        const session = requireSession(sessionId);
        await session.stopNetworkCapture();
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ stopped: sessionId }, null, 2) }],
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
    "web_live_network_query",
    "Read-only. Queries captured WebSocket frames and/or HTTP requests for a session.",
    {
      sessionId: sessionIdSchema,
      type: z.enum(["websocket", "http", "all"]).optional().default("all").describe("Traffic type filter."),
      direction: z.enum(["sent", "received", "any"]).optional().default("any").describe("WebSocket frame direction filter."),
      urlFilter: z.string().optional().describe("Substring filter over request URLs (HTTP only)."),
      query: z.string().optional().describe("Free-text search over frame/request data."),
      limit: z.number().optional().default(100),
      history: z.boolean().optional().default(true),
    },
    async ({
      sessionId,
      type,
      direction,
      urlFilter,
      query,
      limit,
      history,
    }: {
      sessionId: string;
      type: "websocket" | "http" | "all";
      direction: "sent" | "received" | "any";
      urlFilter?: string;
      query?: string;
      limit: number;
      history: boolean;
    }) => {
      requireReady();
      log.debug(`web_live_network_query: id=${sessionId} type=${type} dir=${direction} limit=${limit}`);
      try {
        const session = requireSession(sessionId);
        const result = session.queryNetwork({ type, direction, urlFilter, query, limit, history });
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
    "web_live_network_status",
    "Read-only. Returns a session's current network capture state.",
    { sessionId: sessionIdSchema },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = session.getNetworkCaptureState();
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
}
