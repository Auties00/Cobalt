import type {
  ModuleAnalysis,
} from "../types/analysis.js";
import type {
  SnapshotDiff,
  SnapshotManifest,
  SnapshotModuleRecord,
} from "../types/snapshot.js";

function toModuleMap(
  modules: SnapshotModuleRecord[]
): Map<string, SnapshotModuleRecord> {
  return new Map(modules.map((module) => [module.name, module]));
}

function arrayDiff(before: string[], after: string[]): { added: string[]; removed: string[] } {
  const beforeSet = new Set(before);
  const afterSet = new Set(after);
  return {
    added: after.filter((item) => !beforeSet.has(item)),
    removed: before.filter((item) => !afterSet.has(item)),
  };
}

function symbolHashMap(analysis: ModuleAnalysis | undefined): Map<string, string> {
  if (!analysis) return new Map();
  const map = new Map<string, string>();
  for (const symbol of analysis.symbols) {
    const current = map.get(symbol.name);
    if (!current) {
      map.set(symbol.name, symbol.hash);
      continue;
    }
    if (current !== symbol.hash) {
      map.set(symbol.name, `${current}|${symbol.hash}`);
    }
  }
  return map;
}

function symbolByteRanges(analysis: ModuleAnalysis | undefined): Map<string, { startByte: number; endByte: number }> {
  if (!analysis) return new Map();
  const map = new Map<string, { startByte: number; endByte: number }>();
  for (const symbol of analysis.symbols) {
    if (!map.has(symbol.name)) {
      map.set(symbol.name, { startByte: symbol.startByte, endByte: symbol.endByte });
    }
  }
  return map;
}

export interface DiffSourceLoader {
  loadSource(snapshotId: string, sourcePath: string): Promise<string>;
}

export function diffSnapshots(
  fromManifest: SnapshotManifest,
  toManifest: SnapshotManifest,
  fromAnalyses: ModuleAnalysis[],
  toAnalyses: ModuleAnalysis[]
): SnapshotDiff {
  const fromModules = toModuleMap(fromManifest.modules);
  const toModules = toModuleMap(toManifest.modules);
  const fromNames = new Set(fromModules.keys());
  const toNames = new Set(toModules.keys());

  const fromAnalysisByModule = new Map(
    fromAnalyses.map((analysis) => [analysis.module, analysis])
  );
  const toAnalysisByModule = new Map(
    toAnalyses.map((analysis) => [analysis.module, analysis])
  );

  const addedModules = [...toNames].filter((name) => !fromNames.has(name)).sort();
  const removedModules = [...fromNames]
    .filter((name) => !toNames.has(name))
    .sort();

  const changedModules: SnapshotDiff["changedModules"] = [];
  for (const name of [...toNames].filter((moduleName) => fromNames.has(moduleName)).sort()) {
    const before = fromModules.get(name);
    const after = toModules.get(name);
    if (!before || !after) continue;

    const depChanges = arrayDiff(before.dependencies, after.dependencies);
    const expChanges = arrayDiff(before.exports, after.exports);

    const beforeSymbols = symbolHashMap(fromAnalysisByModule.get(name));
    const afterSymbols = symbolHashMap(toAnalysisByModule.get(name));
    const symbolNames = new Set([...beforeSymbols.keys(), ...afterSymbols.keys()]);
    const symbolsAdded: string[] = [];
    const symbolsRemoved: string[] = [];
    const symbolsChanged: string[] = [];
    for (const symbolName of [...symbolNames].sort()) {
      const prev = beforeSymbols.get(symbolName);
      const next = afterSymbols.get(symbolName);
      if (!prev && next) {
        symbolsAdded.push(symbolName);
      } else if (prev && !next) {
        symbolsRemoved.push(symbolName);
      } else if (prev && next && prev !== next) {
        symbolsChanged.push(symbolName);
      }
    }

    const sourceChanged = before.sourceHash !== after.sourceHash;
    const dependenciesChanged = depChanges.added.length > 0 || depChanges.removed.length > 0;
    const exportsChanged = expChanges.added.length > 0 || expChanges.removed.length > 0;
    const symbolsMutated =
      symbolsAdded.length > 0 || symbolsRemoved.length > 0 || symbolsChanged.length > 0;

    if (!sourceChanged && !dependenciesChanged && !exportsChanged && !symbolsMutated) {
      continue;
    }

    changedModules.push({
      module: name,
      sourceChanged,
      dependenciesChanged,
      exportsAdded: expChanges.added,
      exportsRemoved: expChanges.removed,
      symbolsAdded,
      symbolsRemoved,
      symbolsChanged,
    });
  }

  return {
    fromSnapshotId: fromManifest.snapshotId,
    toSnapshotId: toManifest.snapshotId,
    addedModules,
    removedModules,
    changedModules,
  };
}

const EXCERPT_MAX = 500;

export async function enrichDiffExcerpts(
  diff: SnapshotDiff,
  fromManifest: SnapshotManifest,
  toManifest: SnapshotManifest,
  fromAnalyses: ModuleAnalysis[],
  toAnalyses: ModuleAnalysis[],
  sourceLoader: DiffSourceLoader,
  maxExcerptModules: number = 20
): Promise<SnapshotDiff> {
  const fromModulesMap = toModuleMap(fromManifest.modules);
  const toModulesMap = toModuleMap(toManifest.modules);
  const fromAnalysisMap = new Map(fromAnalyses.map((a) => [a.module, a]));
  const toAnalysisMap = new Map(toAnalyses.map((a) => [a.module, a]));

  let excerptCount = 0;
  for (const changed of diff.changedModules) {
    if (excerptCount >= maxExcerptModules) break;
    if (changed.symbolsChanged.length === 0 && changed.symbolsAdded.length === 0 && changed.symbolsRemoved.length === 0) {
      continue;
    }

    const allSymbols = [...changed.symbolsChanged, ...changed.symbolsAdded, ...changed.symbolsRemoved].slice(0, 5);
    const excerpts: Record<string, { before: string | null; after: string | null }> = {};

    const beforeRanges = symbolByteRanges(fromAnalysisMap.get(changed.module));
    const afterRanges = symbolByteRanges(toAnalysisMap.get(changed.module));
    const beforeRecord = fromModulesMap.get(changed.module);
    const afterRecord = toModulesMap.get(changed.module);

    let beforeSource: string | null = null;
    let afterSource: string | null = null;

    for (const sym of allSymbols) {
      const bRange = beforeRanges.get(sym);
      const aRange = afterRanges.get(sym);

      let before: string | null = null;
      let after: string | null = null;

      if (bRange && beforeRecord) {
        try {
          if (beforeSource == null) {
            beforeSource = await sourceLoader.loadSource(diff.fromSnapshotId, beforeRecord.sourcePath);
          }
          before = beforeSource.slice(bRange.startByte, Math.min(bRange.endByte, bRange.startByte + EXCERPT_MAX));
        } catch {  }
      }

      if (aRange && afterRecord) {
        try {
          if (afterSource == null) {
            afterSource = await sourceLoader.loadSource(diff.toSnapshotId, afterRecord.sourcePath);
          }
          after = afterSource.slice(aRange.startByte, Math.min(aRange.endByte, aRange.startByte + EXCERPT_MAX));
        } catch {  }
      }

      if (before != null || after != null) {
        excerpts[sym] = { before, after };
      }
    }

    if (Object.keys(excerpts).length > 0) {
      changed.excerpts = excerpts;
      excerptCount++;
    }
  }

  return diff;
}
