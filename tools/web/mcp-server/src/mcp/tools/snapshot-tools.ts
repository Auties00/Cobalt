import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { loadIndex, loadManifest, loadModuleSource } from "../../storage/snapshots.js";
import { getSnapshotList } from "../../services/bootstrap.js";
import { diffSnapshots, enrichDiffExcerpts } from "../../services/diff.js";
import type { SnapshotCatalog } from "../../services/catalog.js";
import type { SnapshotPlatform } from "../../types/snapshot.js";
import { createLogger } from "../../utils/logger.js";

const log = createLogger("tools:snapshot");

const platformSchema = z.enum(["web", "desktop_windows", "desktop_macos", "ios"]).optional()
  .describe("Platform to query. Defaults to 'web'.");

interface SnapshotToolsContext {
  requireCatalog: (platform?: SnapshotPlatform) => SnapshotCatalog;
  requireReady: () => void;
}

export function registerSnapshotTools(
  server: McpServer,
  context: SnapshotToolsContext
): void {
  const { requireCatalog, requireReady } = context;

server.tool(
  "list_snapshots",
  "Lists available snapshot identifiers (most recent first). Without platform, lists all snapshots across platforms as 'platform/snapshotId'.",
  {
    platform: platformSchema,
  },
  async ({ platform }: { platform?: SnapshotPlatform }) => {
    requireReady();
    log.debug(`list_snapshots: platform=${platform ?? "all"}`);
    const snapshots = await getSnapshotList(platform);
    log.info(`list_snapshots: returned ${snapshots.length} snapshots`);
    return {
      content: [{ type: "text" as const, text: JSON.stringify(snapshots, null, 2) }],
    };
  }
);

server.tool(
  "get_revision_diff",
  "Compares two snapshots and returns module/export/symbol deltas with optional source excerpts for changed symbols",
  {
    platform: platformSchema,
    fromSnapshotId: z.string().optional(),
    toSnapshotId: z.string().optional(),
    includeExcerpts: z.boolean().optional().default(false).describe("Include before/after source excerpts for changed symbols"),
    maxExcerptModules: z.number().optional().default(20).describe("Max modules to include excerpts for"),
  },
  async ({
    platform,
    fromSnapshotId,
    toSnapshotId,
    includeExcerpts,
    maxExcerptModules,
  }: {
    platform?: SnapshotPlatform;
    fromSnapshotId?: string;
    toSnapshotId?: string;
    includeExcerpts: boolean;
    maxExcerptModules: number;
  }) => {
    log.info(`get_revision_diff: platform=${platform ?? "web"} from=${fromSnapshotId ?? "auto"} to=${toSnapshotId ?? "active"} excerpts=${includeExcerpts}`);
    try {
      const p = platform ?? "web";
      const snapshots = await getSnapshotList(p);
      if (snapshots.length < 2 && (!fromSnapshotId || !toSnapshotId)) {
        throw new Error("At least two snapshots are required to compute diff.");
      }

      const active = requireCatalog(p);
      const toId = toSnapshotId ?? active.snapshotId;
      const fromId =
        fromSnapshotId ??
        snapshots.find((snapshot) => snapshot !== toId) ??
        snapshots[1];

      if (!fromId || !toId) {
        throw new Error("Unable to resolve snapshot IDs for diff.");
      }

      const [fromManifest, toManifest, fromIndex, toIndex] = await Promise.all([
        loadManifest(p, fromId),
        loadManifest(p, toId),
        loadIndex(p, fromId),
        loadIndex(p, toId),
      ]);

      let result = diffSnapshots(
        fromManifest,
        toManifest,
        fromIndex.analyses,
        toIndex.analyses
      );

      if (includeExcerpts) {
        result = await enrichDiffExcerpts(
          result,
          fromManifest,
          toManifest,
          fromIndex.analyses,
          toIndex.analyses,
          {
            loadSource: (snapshotId, sourcePath) =>
              loadModuleSource(p, snapshotId, sourcePath),
          },
          maxExcerptModules
        );
      }

      log.info(`get_revision_diff: from=${fromId} to=${toId} completed`);
      return {
        content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
      };
    } catch (error) {
      log.error(`get_revision_diff: error: ${error instanceof Error ? error.message : String(error)}`);
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
  "get_active_snapshot",
  "Returns current active snapshot metadata",
  {
    platform: platformSchema,
  },
  async ({ platform }: { platform?: SnapshotPlatform }) => {
    log.debug(`get_active_snapshot: platform=${platform ?? "default"}`);
    const active = requireCatalog(platform);
    log.info(`get_active_snapshot: snapshot=${active.snapshotId} revision=${active.revision}`);
    return {
      content: [
        {
          type: "text" as const,
          text: JSON.stringify(
            {
              snapshotId: active.snapshotId,
              revision: active.revision,
              modules: active.getAllModules().length,
            },
            null,
            2
          ),
        },
      ],
    };
  }
);
}
