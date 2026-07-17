import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { LiveToolsContext } from "../../../types/live/tools.js";
import { createLogger } from "../../../utils/logger.js";

const log = createLogger("tools:live:session");

const sessionIdSchema = z
  .string()
  .min(1)
  .describe("Identifier of the target live session. Use web_live_list_sessions to see registered ids.");

const optionalSessionIdSchema = z
  .string()
  .min(1)
  .optional()
  .describe("Identifier of the target live session. When omitted, an id is auto-generated (UUID).");

export function registerLiveSessionTools(server: McpServer, context: LiveToolsContext): void {
  const { registry, activeSnapshotRevision, requireReady, requireSession } = context;

  server.tool(
    "web_live_list_sessions",
    "Read-only. Lists every registered live session with its status, auth state, and (if logged in) account identity. Call first to discover session ids.",
    {},
    async () => {
      requireReady();
      log.debug("web_live_list_sessions");
      try {
        const sessions = await registry.listSessions(activeSnapshotRevision());
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ count: sessions.length, sessions }, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_list_sessions: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_start_session",
    "Starts or refreshes a live WhatsApp session. Use 'mode' to pick browser ('web'), Windows Desktop via CDP ('desktop'), or macOS Desktop via Frida ('desktop_macos'). " +
    "Supply 'sessionId' (recommended: a semantic name like 'primary' or 'counterparty') to reuse an existing session's persistent profile across server restarts; omit it to auto-generate a UUID. " +
    "By default sessions use a persistent Chromium user-data-dir so re-pairing is not required; set 'ephemeral: true' to opt out.",
    {
      sessionId: optionalSessionIdSchema,
      mode: z
        .enum(["web", "desktop", "desktop_macos"])
        .optional()
        .describe("Session mode. Defaults to 'web'."),
      ephemeral: z
        .boolean()
        .optional()
        .describe("When true, do not persist Chromium user-data-dir. Default false (web mode only)."),
      slowMoMs: z
        .number()
        .optional()
        .describe("Optional Playwright slow-mo delay (web mode only)."),
      navigationTimeoutMs: z
        .number()
        .optional()
        .describe("Navigation timeout for loading web.whatsapp.com (web mode only)."),
      locale: z
        .string()
        .optional()
        .describe("Preferred locale (e.g. en-US, it-IT). Web mode only."),
      desktopCdpPort: z
        .number()
        .optional()
        .describe("CDP port for WhatsApp Desktop (default 47832). Desktop mode only."),
    },
    async ({
      sessionId,
      mode,
      ephemeral,
      slowMoMs,
      navigationTimeoutMs,
      locale,
      desktopCdpPort,
    }: {
      sessionId?: string;
      mode?: "web" | "desktop" | "desktop_macos";
      ephemeral?: boolean;
      slowMoMs?: number;
      navigationTimeoutMs?: number;
      locale?: string;
      desktopCdpPort?: number;
    }) => {
      requireReady();
      log.info(`web_live_start_session: id=${sessionId ?? "auto"} mode=${mode ?? "web"} ephemeral=${!!ephemeral}`);
      try {
        const status = await registry.startSession({
          sessionId,
          mode,
          ephemeral,
          slowMoMs,
          navigationTimeoutMs,
          locale,
          desktopCdpPort,
          snapshotRevision: activeSnapshotRevision(),
        });
        log.info(`web_live_start_session: id=${status.sessionId} authState=${status.authState}`);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(status, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_start_session: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_status",
    "Read-only health check for a single session. Returns the session's info including identity when logged in.",
    {
      sessionId: sessionIdSchema,
    },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      log.debug(`web_live_status: id=${sessionId}`);
      try {
        const session = requireSession(sessionId);
        const status = await session.info(activeSnapshotRevision());
        return {
          content: [{ type: "text" as const, text: JSON.stringify(status, null, 2) }],
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
    "web_live_validate_snapshot_revision",
    "Confirms that a session's live runtime matches the active static snapshot revision.",
    {
      sessionId: sessionIdSchema,
    },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      try {
        const revision = activeSnapshotRevision();
        if (!revision) throw new Error("No active snapshot revision available.");
        const session = requireSession(sessionId);
        const result = await session.ensureSnapshotMatches(revision);
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
    "web_live_login_with_phone_number",
    "Drives the pairing-code login flow for a session. Use when QR flow is not preferred; may mutate session state.",
    {
      sessionId: sessionIdSchema,
      phoneNumber: z
        .string()
        .optional()
        .describe("E.164-like phone number; falls back to WEB_MCP_PHONE_NUMBER."),
      countryCode: z
        .string()
        .optional()
        .describe("Optional country code override; falls back to WEB_MCP_PHONE_COUNTRY_CODE."),
      waitForPairingCodeTimeoutMs: z
        .number()
        .optional()
        .describe("How long to wait for pairing code extraction after submit."),
    },
    async ({
      sessionId,
      phoneNumber,
      countryCode,
      waitForPairingCodeTimeoutMs,
    }: {
      sessionId: string;
      phoneNumber?: string;
      countryCode?: string;
      waitForPairingCodeTimeoutMs?: number;
    }) => {
      requireReady();
      try {
        const resolvedPhone =
          (phoneNumber ?? process.env.WEB_MCP_PHONE_NUMBER ?? "").trim();
        const resolvedCountryCode =
          (countryCode ?? process.env.WEB_MCP_PHONE_COUNTRY_CODE ?? "").trim() || undefined;
        if (!resolvedPhone) {
          throw new Error("Phone number is required. Provide phoneNumber or set WEB_MCP_PHONE_NUMBER.");
        }
        log.info(`web_live_login_with_phone_number: id=${sessionId} phone=${resolvedPhone.slice(0, 4)}***`);
        const session = requireSession(sessionId);
        const result = await session.beginPhoneNumberLogin(resolvedPhone, {
          countryCode: resolvedCountryCode,
          waitForPairingCodeTimeoutMs,
        });
        log.info(`web_live_login_with_phone_number: id=${sessionId} authState=${result.status.authState}`);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_login_with_phone_number: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_wait_for_login",
    "Blocks until the session reaches logged_in state or the timeout elapses. Returns the final session info.",
    {
      sessionId: sessionIdSchema,
      timeoutMs: z
        .number()
        .optional()
        .default(120000)
        .describe("Maximum wait time in ms."),
    },
    async ({ sessionId, timeoutMs }: { sessionId: string; timeoutMs: number }) => {
      requireReady();
      log.info(`web_live_wait_for_login: id=${sessionId} timeoutMs=${timeoutMs}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.waitForLogin(timeoutMs);
        log.info(`web_live_wait_for_login: id=${sessionId} success=${result.success} authState=${result.status.authState}`);
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
    "web_live_stop_session",
    "Stops a session and removes it from the registry. Use sessionId='all' to stop every registered session. The persistent user-data-dir is preserved; use web_live_delete_session to wipe it.",
    {
      sessionId: z.string().min(1).describe("Session id to stop, or 'all' to stop every session."),
    },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      log.info(`web_live_stop_session: id=${sessionId}`);
      try {
        if (sessionId === "all") {
          await registry.stopAllSessions();
          return {
            content: [{ type: "text" as const, text: JSON.stringify({ stopped: "all" }, null, 2) }],
          };
        }
        await registry.stopSession(sessionId);
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
    "web_live_delete_session",
    "Stops a session AND wipes its persistent Chromium user-data-dir on disk, forcing a fresh pair next time the id is used.",
    {
      sessionId: sessionIdSchema,
    },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      log.info(`web_live_delete_session: id=${sessionId}`);
      try {
        await registry.deleteSession(sessionId);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ deleted: sessionId }, null, 2) }],
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
