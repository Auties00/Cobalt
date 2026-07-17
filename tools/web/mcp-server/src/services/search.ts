import { create, insert, search } from "@orama/orama";
import type {
  ModuleAnalysis,
} from "../types/analysis.js";
import type {
  ModuleSearchResult,
} from "../types/catalog.js";
import type {
  SnapshotManifest,
} from "../types/snapshot.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("search");

const schema = {
  name: "string" as const,
  nameTokens: "string" as const,
  exports: "string" as const,
  dependencies: "string" as const,
  symbols: "string" as const,
  literals: "string" as const
};

type SearchDb = Awaited<ReturnType<typeof create<typeof schema>>>;

interface SearchDocument {
  name: string;
  nameTokens: string;
  exports: string;
  dependencies: string;
  symbols: string;
  literals: string;
}

export function splitIdentifier(name: string): string {
  return name
    .replace(/[._]/g, " ")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2");
}

function buildDocument(
  moduleName: string,
  exports: string[],
  dependencies: string[],
  analysis: ModuleAnalysis | undefined
): SearchDocument {
  const symbolNames = analysis?.symbols.map((symbol) => symbol.name) ?? [];
  const literals = analysis?.literals ?? [];
  return {
    name: moduleName,
    nameTokens: splitIdentifier(moduleName),
    exports: exports.join(" "),
    dependencies: dependencies.join(" "),
    symbols: symbolNames.join(" "),
    literals: literals.join(" "),
  };
}

export class ModuleSearchService {
  private db: SearchDb | null = null;
  private exportsByModule = new Map<string, string[]>();
  private dependenciesByModule = new Map<string, string[]>();
  private snapshotId: string = "";

  async build(
    manifest: SnapshotManifest,
    analysesByModule: Map<string, ModuleAnalysis>,
  ): Promise<void> {
    log.info(`building search index: snapshot=${manifest.snapshotId} modules=${manifest.modules.length}`);
    const start = performance.now();
    this.snapshotId = manifest.snapshotId;
    this.db = create({
      schema,
      components: {
        tokenizer: {
          stemming: true,
          language: "english",
        },
      },
    });

    this.exportsByModule.clear();
    this.dependenciesByModule.clear();

    for (const module of manifest.modules) {
      this.exportsByModule.set(module.name, module.exports);
      this.dependenciesByModule.set(module.name, module.dependencies);
      await insert(
        this.db,
        buildDocument(
          module.name,
          module.exports,
          module.dependencies,
          analysesByModule.get(module.name)
        )
      );
    }
    log.info(`search index built in ${(performance.now() - start).toFixed(1)}ms: ${manifest.modules.length} documents indexed`);
  }

  async search(
    query: string,
    limit: number = 20,
    tolerance: number = 1
  ): Promise<ModuleSearchResult[]> {
    if (!this.db) {
      throw new Error("Search service not initialized.");
    }

    log.debug(`searching: query="${query}" limit=${limit} tolerance=${tolerance}`);
    const result = await search(this.db, {
      term: query,
      limit,
      tolerance,
      boost: {
        nameTokens: 6,
        name: 4,
        exports: 3,
        symbols: 2,
        dependencies: 1.5,
        literals: 1,
      },
    });

    return result.hits.map(
      (hit: { document: Record<string, unknown>; score: number }) => {
      const name = hit.document.name as string;
      return {
        snapshotId: this.snapshotId,
        name,
        score: hit.score,
        exports: this.exportsByModule.get(name) ?? [],
        dependencies: this.dependenciesByModule.get(name) ?? []
      };
      }
    );
  }
}
