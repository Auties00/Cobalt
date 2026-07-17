import { mkdir, writeFile } from "node:fs/promises";
import { dirname, isAbsolute, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { LiveToolsContext } from "../../../types/live/tools.js";
import { createLogger } from "../../../utils/logger.js";

const log = createLogger("tools:live:observability");

const sessionIdSchema = z.string().min(1).describe("Target live session id.");

const CAPTURES_DIR = join(
  dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
  "..",
  "..",
  "data",
  "captures"
);

const TOPIC_SLUG = /^[A-Za-z0-9._/-]+$/;

const FIXTURE_SCHEMA_VERSION = "v1";

function resolveOutputPath(
  sessionId: string,
  topic: string,
  outputPath: string | undefined,
  extension: string
): string {
  if (outputPath) {
    return isAbsolute(outputPath) ? outputPath : resolve(process.cwd(), outputPath);
  }
  if (!TOPIC_SLUG.test(topic) || topic.split("/").some((seg) => seg === "" || seg === ".." || seg === ".")) {
    throw new Error(
      `topic must match ${TOPIC_SLUG.toString()} with no '.', '..', or empty segments when outputPath is not supplied (got "${topic}")`
    );
  }
  return join(CAPTURES_DIR, sessionId, `${topic}.${extension}`);
}

export function registerLiveObservabilityTools(
  server: McpServer,
  context: LiveToolsContext
): void {
  const { requireReady, requireSession } = context;

  server.tool(
    "web_live_stanza_query_nodes",
    "Read-only. Queries captured inbound/outbound stanza traffic for a session.",
    {
      sessionId: sessionIdSchema,
      direction: z.enum(["in", "out", "any"]).optional().default("any"),
      tag: z.string().optional(),
      id: z.string().optional(),
      from: z.string().optional(),
      to: z.string().optional(),
      query: z.string().optional(),
      attrs: z.record(z.string()).optional(),
      history: z.boolean().optional().default(true),
      limit: z.number().optional().default(100),
    },
    async ({
      sessionId,
      direction,
      tag,
      id,
      from,
      to,
      query,
      attrs,
      history,
      limit,
    }: {
      sessionId: string;
      direction: "in" | "out" | "any";
      tag?: string;
      id?: string;
      from?: string;
      to?: string;
      query?: string;
      attrs?: Record<string, string>;
      history: boolean;
      limit: number;
    }) => {
      requireReady();
      log.debug(`web_live_stanza_query_nodes: id=${sessionId} dir=${direction} tag=${tag ?? "any"} limit=${limit}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.queryStanzaNodes({
          direction,
          tag,
          id,
          from,
          to,
          query,
          attrs,
          history,
          limit,
        });
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
    "web_live_stanza_send_node",
    "Sends a custom stanza stanza into a session's live runtime.",
    {
      sessionId: sessionIdSchema,
      node: z.object({
        tag: z.string().describe("Stanza tag (required)."),
        attrs: z.record(z.union([z.string(), z.number(), z.boolean()])).optional(),
        content: z.unknown().optional(),
      }),
    },
    async ({
      sessionId,
      node,
    }: {
      sessionId: string;
      node: {
        tag: string;
        attrs?: Record<string, string | number | boolean>;
        content?: unknown;
      };
    }) => {
      requireReady();
      log.info(`web_live_stanza_send_node: id=${sessionId} tag="${node.tag}"`);
      try {
        const session = requireSession(sessionId);
        const result = await session.sendStanzaNode(node as unknown as Record<string, unknown>);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_stanza_send_node: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_wam_get_events",
    "Read-only. Queries captured WAM telemetry events for a session.",
    {
      sessionId: sessionIdSchema,
      name: z.string().optional(),
      channel: z.string().optional(),
      query: z.string().optional(),
      history: z.boolean().optional().default(true),
      limit: z.number().optional().default(100),
    },
    async ({
      sessionId,
      name,
      channel,
      query,
      history,
      limit,
    }: {
      sessionId: string;
      name?: string;
      channel?: string;
      query?: string;
      history: boolean;
      limit: number;
    }) => {
      requireReady();
      log.debug(`web_live_wam_get_events: id=${sessionId} name=${name ?? "any"} limit=${limit}`);
      try {
        const session = requireSession(sessionId);
        const result = await session.getWamEvents({ name, channel, query, history, limit });
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
    "web_live_wam_send_event",
    "Sends a custom WAM event into a session's live runtime.",
    {
      sessionId: sessionIdSchema,
      name: z.string().describe("WAM event constructor name."),
      props: z.record(z.unknown()).optional(),
      flush: z.boolean().optional().default(false),
    },
    async ({
      sessionId,
      name,
      props,
      flush,
    }: {
      sessionId: string;
      name: string;
      props?: Record<string, unknown>;
      flush: boolean;
    }) => {
      requireReady();
      log.info(`web_live_wam_send_event: id=${sessionId} name="${name}"`);
      try {
        const session = requireSession(sessionId);
        const result = await session.sendCustomWamEvent(name, props ?? {}, flush);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_wam_send_event: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_wam_get_event_definitions",
    "Read-only. Returns WAM event constructor definitions for a session.",
    {
      sessionId: sessionIdSchema,
      filter: z.string().optional(),
      limit: z.number().optional().default(300),
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
      try {
        const session = requireSession(sessionId);
        const result = await session.getWamEventDefinitions({ filter, limit });
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
    "web_live_ab_props_query",
    "Read-only. Lists / diffs AB props for a session.",
    {
      sessionId: sessionIdSchema,
      filter: z.string().optional(),
      diffOnly: z.boolean().optional().default(false),
      limit: z.number().optional(),
    },
    async ({
      sessionId,
      filter,
      diffOnly,
      limit,
    }: {
      sessionId: string;
      filter?: string;
      diffOnly: boolean;
      limit?: number;
    }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.queryAbProps({ filter, diffOnly, limit });
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
    "web_live_ab_props_get",
    "Read-only AB prop lookup by exact name for a session.",
    {
      sessionId: sessionIdSchema,
      name: z.string(),
    },
    async ({ sessionId, name }: { sessionId: string; name: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.getAbProp(name);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ name, value: result }, null, 2) }],
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
    "web_live_ab_props_set",
    "Sets an AB prop for a session's runtime.",
    {
      sessionId: sessionIdSchema,
      name: z.string(),
      value: z.unknown(),
    },
    async ({ sessionId, name, value }: { sessionId: string; name: string; value: unknown }) => {
      requireReady();
      log.info(`web_live_ab_props_set: id=${sessionId} name="${name}"`);
      try {
        const session = requireSession(sessionId);
        const result = await session.setAbProp(name, value);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ name, value: result }, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_ab_props_set: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_ab_props_reset",
    "Resets one AB prop to default for a session.",
    {
      sessionId: sessionIdSchema,
      name: z.string(),
    },
    async ({ sessionId, name }: { sessionId: string; name: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.resetAbProp(name);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ name, value: result }, null, 2) }],
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
    "web_live_ab_props_reset_all",
    "Resets every AB prop to default for a session.",
    { sessionId: sessionIdSchema },
    async ({ sessionId }: { sessionId: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.resetAllAbProps();
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
    "web_live_ab_props_definitions",
    "Read-only. AB props schema discovery for a session.",
    {
      sessionId: sessionIdSchema,
      filter: z.string().optional(),
    },
    async ({ sessionId, filter }: { sessionId: string; filter?: string }) => {
      requireReady();
      try {
        const session = requireSession(sessionId);
        const result = await session.getAbPropDefinitions(filter);
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
    "web_live_stanza_dump_to_file",
    "Dumps captured stanzas matching a filter to a JSONL file on disk. Binary content is emitted as base64 (full fidelity, not previewed). One event per line. Schema version 'v1'.",
    {
      sessionId: sessionIdSchema,
      topic: z
        .string()
        .min(1)
        .describe(
          "Short slug used to name the default output file (e.g. 'usync-message'). Required when outputPath is omitted."
        ),
      outputPath: z
        .string()
        .optional()
        .describe(
          "Explicit destination path. Absolute or relative to the MCP server process cwd. When omitted, writes to <data>/captures/<sessionId>/<topic>.jsonl."
        ),
      direction: z.enum(["in", "out", "any"]).optional().default("any"),
      tag: z.string().optional(),
      id: z.string().optional(),
      from: z.string().optional(),
      to: z.string().optional(),
      query: z.string().optional(),
      attrs: z.record(z.string()).optional(),
      history: z.boolean().optional().default(true),
      limit: z.number().optional().default(1000),
    },
    async ({
      sessionId,
      topic,
      outputPath,
      direction,
      tag,
      id,
      from,
      to,
      query,
      attrs,
      history,
      limit,
    }: {
      sessionId: string;
      topic: string;
      outputPath?: string;
      direction: "in" | "out" | "any";
      tag?: string;
      id?: string;
      from?: string;
      to?: string;
      query?: string;
      attrs?: Record<string, string>;
      history: boolean;
      limit: number;
    }) => {
      requireReady();
      try {
        const target = resolveOutputPath(sessionId, topic, outputPath, "jsonl");
        log.info(
          `web_live_stanza_dump_to_file: id=${sessionId} topic="${topic}" dir=${direction} tag=${tag ?? "any"} limit=${limit} -> ${target}`
        );
        const session = requireSession(sessionId);
        const result = await session.queryStanzaNodes({
          direction,
          tag,
          id,
          from,
          to,
          query,
          attrs,
          history,
          limit,
        });
        const events = Array.isArray(result) ? result : [];
        const lines = new Array<string>(events.length);
        for (let i = 0; i < events.length; i++) {
          lines[i] = JSON.stringify({
            schema: FIXTURE_SCHEMA_VERSION,
            sessionId,
            topic,
            event: events[i],
          });
        }
        const payload = lines.length > 0 ? lines.join("\n") + "\n" : "";
        await mkdir(dirname(target), { recursive: true });
        await writeFile(target, payload, "utf8");
        return {
          content: [
            {
              type: "text" as const,
              text: JSON.stringify(
                { path: target, count: events.length, byteLength: Buffer.byteLength(payload, "utf8") },
                null,
                2
              ),
            },
          ],
        };
      } catch (error) {
        log.error(
          `web_live_stanza_dump_to_file: ${error instanceof Error ? error.message : String(error)}`
        );
        return {
          content: [
            {
              type: "text" as const,
              text: error instanceof Error ? error.message : String(error),
            },
          ],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_debug_eval_to_file",
    "Evaluates JavaScript in a session's live runtime and writes the JSON result to a file. Use to capture WA Web oracle outputs (e.g. expected fanout, phash, validated key index list) as .expected.json sibling files.",
    {
      sessionId: sessionIdSchema,
      expression: z.string(),
      topic: z
        .string()
        .min(1)
        .describe("Short slug used to name the default output file. Required when outputPath is omitted."),
      outputPath: z
        .string()
        .optional()
        .describe(
          "Explicit destination path. When omitted, writes to <data>/captures/<sessionId>/<topic>.expected.json."
        ),
      awaitPromise: z.boolean().optional().default(true),
    },
    async ({
      sessionId,
      expression,
      topic,
      outputPath,
      awaitPromise,
    }: {
      sessionId: string;
      expression: string;
      topic: string;
      outputPath?: string;
      awaitPromise: boolean;
    }) => {
      requireReady();
      try {
        const target = resolveOutputPath(sessionId, topic, outputPath, "expected.json");
        log.info(
          `web_live_debug_eval_to_file: id=${sessionId} topic="${topic}" expr="${expression.slice(0, 80)}${expression.length > 80 ? "..." : ""}" -> ${target}`
        );
        const session = requireSession(sessionId);
        const result = await session.evaluate(expression, awaitPromise);
        const payload =
          JSON.stringify(
            { schema: FIXTURE_SCHEMA_VERSION, sessionId, topic, expression, result },
            null,
            2
          ) + "\n";
        await mkdir(dirname(target), { recursive: true });
        await writeFile(target, payload, "utf8");
        return {
          content: [
            {
              type: "text" as const,
              text: JSON.stringify(
                { path: target, byteLength: Buffer.byteLength(payload, "utf8") },
                null,
                2
              ),
            },
          ],
        };
      } catch (error) {
        log.error(
          `web_live_debug_eval_to_file: ${error instanceof Error ? error.message : String(error)}`
        );
        return {
          content: [
            {
              type: "text" as const,
              text: error instanceof Error ? error.message : String(error),
            },
          ],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_clear_capture",
    "Clears captured data buffers for a session. Domain targets stanza/wam/network/all; scope is buffer (active only) or history (everything).",
    {
      sessionId: sessionIdSchema,
      domain: z.enum(["stanza", "wam", "network", "all"]),
      scope: z.enum(["buffer", "history"]).optional().default("buffer"),
    },
    async ({
      sessionId,
      domain,
      scope,
    }: {
      sessionId: string;
      domain: "stanza" | "wam" | "network" | "all";
      scope: "buffer" | "history";
    }) => {
      requireReady();
      try {
        log.info(`web_live_clear_capture: id=${sessionId} domain=${domain} scope=${scope}`);
        const session = requireSession(sessionId);
        const results: Record<string, unknown> = {};
        const domains = domain === "all" ? (["stanza", "wam", "network"] as const) : ([domain] as const);

        for (const d of domains) {
          if (d === "stanza") {
            results.stanza = scope === "history"
              ? await session.clearStanzaHistory()
              : await session.clearStanzaNodes();
          } else if (d === "wam") {
            results.wam = scope === "history"
              ? await session.clearWamHistory()
              : await session.clearWamEvents();
          } else if (d === "network") {
            results.network = scope === "history"
              ? session.clearNetworkHistory()
              : session.clearNetworkBuffers();
          }
        }

        return {
          content: [{ type: "text" as const, text: JSON.stringify({ sessionId, domain, scope, ...results }, null, 2) }],
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
