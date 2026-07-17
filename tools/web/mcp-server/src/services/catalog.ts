import type {
  CodeSearchResult,
  DependencyTraceNode,
  LiteralCrossRef,
  ModuleMetadataResponse,
  NativeCallGraphNode,
  NativeDataMatch,
  NativeModuleMetadataResponse,
  NativeReferenceResult,
  NativeVtable,
  NativeVtableSlot,
  ReferenceSearchResult,
  ResolvedExport,
  SymbolSourceResult,
} from "../types/catalog.js";
import type {
  CrossModuleCallEdgeRecord,
  ExportBindingRecord,
  ModuleAnalysis,
  ReferenceRecord,
  SymbolRecord,
} from "../types/analysis.js";
import type {
  SnapshotIndex,
  SnapshotManifest,
  SnapshotModuleRecord,
  SnapshotNativeModuleRecord,
  SnapshotPlatform,
} from "../types/snapshot.js";
import type { WasmAnalysis, WasmCrossReference } from "../types/wasm.js";
import { ModuleSearchService } from "./search.js";
import { LruCache } from "./lru-cache.js";
import { nativePath } from "../storage/snapshot-utils.js";
import { analyzeWasm } from "../analysis/wasm-analyzer.js";
import { buildWasmIndex, type WasmIndex } from "../analysis/wasm-index.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("catalog");

const SOURCE_CACHE_MAX = 8_000;

export interface ModuleSourceResult {
  snapshotId: string;
  module: string;
  sourceHash: string;
  startByte: number;
  endByte: number;
  source: string;
}

export class SnapshotCatalog {
  private moduleMap = new Map<string, SnapshotModuleRecord>();
  private nativeModuleMap = new Map<string, SnapshotNativeModuleRecord>();
  private analysisMap = new Map<string, ModuleAnalysis>();
  private symbolsByName = new Map<string, SymbolRecord[]>();
  private referencesBySymbol = new Map<string, ReferenceRecord[]>();
  private reverseDeps = new Map<string, string[]>();
  private sourceCache = new LruCache<string, string>(SOURCE_CACHE_MAX);
  private search = new ModuleSearchService();
  private literalsByValue = new Map<string, Set<string>>();
  private crossModuleEdgesByCallee = new Map<string, CrossModuleCallEdgeRecord[]>();
  private crossModuleEdgesByCaller = new Map<string, CrossModuleCallEdgeRecord[]>();
  private nativeAnalysisCache = new Map<string, WasmAnalysis>();
  private wasmIndexCache = new LruCache<string, Promise<WasmIndex>>(3);

  constructor(
    public readonly manifest: SnapshotManifest,
    public readonly index: SnapshotIndex,
    private readonly loadSourceByPath: (relativePath: string) => Promise<string>,
    private readonly loadNativeBinaryByPath?: (relativePath: string) => Promise<Buffer>,
    private readonly loadNativeAnalysisByPath?: (relativePath: string) => Promise<WasmAnalysis>
  ) {
    for (const native of manifest.nativeModules ?? []) {
      this.nativeModuleMap.set(native.name, native);
    }
    for (const module of manifest.modules) {
      this.moduleMap.set(module.name, module);
      for (const dep of module.dependencies) {
        const dependents = this.reverseDeps.get(dep) ?? [];
        dependents.push(module.name);
        this.reverseDeps.set(dep, dependents);
      }
    }

    for (const analysis of index.analyses) {
      this.analysisMap.set(analysis.module, analysis);
      for (const symbol of analysis.symbols) {
        const list = this.symbolsByName.get(symbol.name) ?? [];
        list.push(symbol);
        this.symbolsByName.set(symbol.name, list);
      }
      for (const reference of analysis.references) {
        const list = this.referencesBySymbol.get(reference.symbol) ?? [];
        list.push(reference);
        this.referencesBySymbol.set(reference.symbol, list);
      }
      for (const literal of analysis.literals) {
        const modules = this.literalsByValue.get(literal) ?? new Set();
        modules.add(analysis.module);
        this.literalsByValue.set(literal, modules);
      }
      for (const edge of analysis.crossModuleCallEdges ?? []) {
        const calleeKey = `${edge.calleeModule}:${edge.calleeExport}`;
        const byCallee = this.crossModuleEdgesByCallee.get(calleeKey) ?? [];
        byCallee.push(edge);
        this.crossModuleEdgesByCallee.set(calleeKey, byCallee);
        const byCaller = this.crossModuleEdgesByCaller.get(edge.callerModule) ?? [];
        byCaller.push(edge);
        this.crossModuleEdgesByCaller.set(edge.callerModule, byCaller);
      }
    }

    for (const [module, dependents] of this.reverseDeps.entries()) {
      this.reverseDeps.set(module, [...new Set(dependents)].sort());
    }
  }

  async initialize(): Promise<void> {
    log.debug(`initializing catalog: snapshot=${this.snapshotId} modules=${this.manifest.modules.length}`);
    await this.search.build(this.manifest, this.analysisMap);
    log.debug(`search index built for snapshot=${this.snapshotId}`);
  }

  get snapshotId(): string {
    return this.manifest.snapshotId;
  }

  get revision(): string {
    return this.manifest.revision;
  }

  getAllModules(): string[] {
    return this.manifest.modules.map((module) => module.name);
  }

  listModules(): Array<{
    name: string;
    platform: SnapshotPlatform;
    sourceBytes: number;
    exports: string[];
    dependencies: string[];
    sourcePath: string;
  }> {
    const platform = this.manifest.platform;
    return this.manifest.modules.map((module) => ({
      name: module.name,
      platform,
      sourceBytes: module.sourceBytes,
      exports: module.exports,
      dependencies: module.dependencies,
      sourcePath: module.sourcePath,
    }));
  }

  async searchModules(query: string, limit: number, tolerance: number) {
    return this.search.search(query, limit, tolerance);
  }

  getModuleRecord(moduleName: string): SnapshotModuleRecord {
    const module = this.moduleMap.get(moduleName);
    if (!module) throw new Error(`Module "${moduleName}" not found`);
    return module;
  }

  getModuleAnalysis(moduleName: string): ModuleAnalysis {
    return (
      this.analysisMap.get(moduleName) ?? {
        module: moduleName,
        symbols: [],
        exportBindings: [],
        imports: [],
        references: [],
        callEdges: [],
        literals: [],
        parseError: "analysis missing",
      }
    );
  }

  getModuleMetadata(moduleName: string): ModuleMetadataResponse {
    const module = this.getModuleRecord(moduleName);
    return {
      snapshotId: this.snapshotId,
      name: module.name,
      dependencies: module.dependencies,
      exports: module.exports,
      sourceHash: module.sourceHash,
      sourceBytes: module.sourceBytes
    };
  }

  getExports(moduleName: string): ExportBindingRecord[] {
    const module = this.getModuleRecord(moduleName);
    const bindings = this.getModuleAnalysis(moduleName).exportBindings;
    if (!bindings.length) {
      return module.exports.map((exportName) => ({
        module: moduleName,
        exportName,
        objectName: null,
        bindingKind: "unresolved",
        localName: null,
        expression: null,
        startByte: null,
        endByte: null,
      }));
    }
    return bindings;
  }

  async resolveExport(
    moduleName: string,
    exportName: string
  ): Promise<ResolvedExport> {
    const module = this.getModuleRecord(moduleName);
    if (!module.exports.includes(exportName)) {
      throw new Error(`Export "${exportName}" not found in module "${moduleName}"`);
    }

    const binding =
      this.getExports(moduleName).find((item) => item.exportName === exportName) ??
      null;
    const symbol =
      binding?.localName != null
        ? this.getModuleAnalysis(moduleName).symbols.find(
            (candidate) => candidate.name === binding.localName
          ) ?? null
        : null;

    return {
      snapshotId: this.snapshotId,
      module: moduleName,
      exportName,
      bindingKind: binding?.bindingKind ?? "unresolved",
      localName: binding?.localName ?? null,
      expression: binding?.expression ?? null,
      symbol,
      sourceHash: module.sourceHash,
    };
  }

  getSymbols(moduleName: string): SymbolRecord[] {
    this.getModuleRecord(moduleName);
    return this.getModuleAnalysis(moduleName).symbols;
  }

  private async loadModuleSourceRaw(moduleName: string): Promise<string> {
    const cached = this.sourceCache.get(moduleName);
    if (cached != null) {
      log.debug(`source cache hit: module="${moduleName}"`);
      return cached;
    }
    log.debug(`source cache miss: module="${moduleName}", loading from disk`);
    const module = this.getModuleRecord(moduleName);
    const source = await this.loadSourceByPath(module.sourcePath);
    this.sourceCache.set(moduleName, source);
    return source;
  }

  async getModuleSource(
    moduleName: string,
    options: {
      startByte?: number;
      endByte?: number;
      startLine?: number;
      endLine?: number;
    } = {}
  ): Promise<ModuleSourceResult> {
    const module = this.getModuleRecord(moduleName);
    const source = await this.loadModuleSourceRaw(moduleName);

    let start: number;
    let end: number;

    if (options.startLine != null || options.endLine != null) {
      const lines = source.split("\n");
      const sl = Math.max(0, (options.startLine ?? 1) - 1);
      const el = Math.min(lines.length, options.endLine ?? lines.length);
      const selectedLines = lines.slice(sl, el);
      const slice = selectedLines.join("\n");
      let byteStart = 0;
      for (let i = 0; i < sl; i++) byteStart += lines[i].length + 1;
      return {
        snapshotId: this.snapshotId,
        module: moduleName,
        sourceHash: module.sourceHash,
        startByte: byteStart,
        endByte: byteStart + slice.length,
        source: slice,
      };
    }

    start = Math.max(0, options.startByte ?? 0);
    end = Math.min(source.length, options.endByte ?? source.length);
    if (start > end) {
      throw new Error("Invalid range: startByte cannot be greater than endByte");
    }

    return {
      snapshotId: this.snapshotId,
      module: moduleName,
      sourceHash: module.sourceHash,
      startByte: start,
      endByte: end,
      source: source.slice(start, end),
    };
  }

  async getSymbolSource(
    moduleName: string,
    symbolName: string
  ): Promise<SymbolSourceResult> {
    const module = this.getModuleRecord(moduleName);
    const symbol =
      this.getModuleAnalysis(moduleName).symbols.find(
        (candidate) => candidate.name === symbolName
      ) ?? null;
    if (!symbol) {
      throw new Error(`Symbol "${symbolName}" not found in module "${moduleName}"`);
    }

    const source = await this.loadModuleSourceRaw(moduleName);
    return {
      snapshotId: this.snapshotId,
      module: moduleName,
      symbol,
      source: source.slice(symbol.startByte, symbol.endByte),
      sourceHash: module.sourceHash,
    };
  }

  async findReferences(
    symbol: string,
    scope: string | undefined,
    maxResults: number
  ): Promise<ReferenceSearchResult[]> {
    const normalizedMax = Math.max(1, maxResults);
    const references = this.referencesBySymbol.get(symbol) ?? [];
    const scoped = scope
      ? references.filter((reference) => reference.module === scope)
      : references;

    return scoped.slice(0, normalizedMax).map((reference) => ({
      module: reference.module,
      symbol: reference.symbol,
      type: reference.type,
      startByte: reference.startByte,
      endByte: reference.endByte,
      context: reference.context,
    }));
  }

  traceDependencies(
    moduleName: string,
    depth: number,
    direction: "forward" | "reverse"
  ): DependencyTraceNode[] {
    this.getModuleRecord(moduleName);
    const maxDepth = Math.max(0, depth);
    const visited = new Set<string>();
    const queue: Array<{ module: string; depth: number }> = [
      { module: moduleName, depth: 0 },
    ];
    const output: DependencyTraceNode[] = [];

    while (queue.length > 0) {
      const next = queue.shift();
      if (!next) break;
      if (visited.has(next.module)) continue;
      if (next.depth > maxDepth) continue;
      visited.add(next.module);

      const module = this.getModuleRecord(next.module);
      const dependents = this.reverseDeps.get(next.module) ?? [];
      output.push({
        module: next.module,
        depth: next.depth,
        direction,
        dependencies: module.dependencies,
        dependents,
      });

      const neighbors =
        direction === "forward" ? module.dependencies : dependents;
      if (next.depth < maxDepth) {
        for (const neighbor of neighbors) {
          if (!visited.has(neighbor) && this.moduleMap.has(neighbor)) {
            queue.push({ module: neighbor, depth: next.depth + 1 });
          }
        }
      }
    }

    return output;
  }

  private async preloadSources(moduleNames: string[]): Promise<void> {
    const BATCH_SIZE = 100;
    for (let i = 0; i < moduleNames.length; i += BATCH_SIZE) {
      const batch = moduleNames.slice(i, i + BATCH_SIZE);
      await Promise.all(batch.map((name) => this.loadModuleSourceRaw(name)));
    }
  }

  async searchCode(
    pattern: string,
    mode: "regex" | "literal",
    scope: string | undefined,
    maxResults: number
  ): Promise<CodeSearchResult[]> {
    const modules = scope ? [scope] : this.getAllModules();
    if (scope) this.getModuleRecord(scope);
    log.debug(`searchCode: pattern="${pattern}" mode=${mode} scope=${scope ?? `all (${modules.length} modules)`} maxResults=${maxResults}`);

    const compiled =
      mode === "regex"
        ? new RegExp(pattern, "gim")
        : new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "gim");

    const results: CodeSearchResult[] = [];
    for (const moduleName of modules) {
      if (results.length >= maxResults) break;
      const source = await this.loadModuleSourceRaw(moduleName);

      let match: RegExpExecArray | null;
      while ((match = compiled.exec(source)) != null) {
        const start = match.index;
        const end = start + match[0].length;
        const contextStart = Math.max(0, start - 120);
        const contextEnd = Math.min(source.length, end + 120);
        results.push({
          module: moduleName,
          startByte: start,
          endByte: end,
          context: source.slice(contextStart, contextEnd),
        });
        if (results.length >= maxResults) break;
      }
      compiled.lastIndex = 0;
    }

    return results;
  }

  getReferencesForModule(moduleName: string): ReferenceRecord[] {
    this.getModuleRecord(moduleName);
    return this.getModuleAnalysis(moduleName).references;
  }

  getDependents(moduleName: string): string[] {
    this.getModuleRecord(moduleName);
    return this.reverseDeps.get(moduleName) ?? [];
  }

  getCrossModuleCallers(
    moduleName: string,
    exportName: string
  ): CrossModuleCallEdgeRecord[] {
    const key = `${moduleName}:${exportName}`;
    return this.crossModuleEdgesByCallee.get(key) ?? [];
  }

  getCrossModuleCallees(moduleName: string): CrossModuleCallEdgeRecord[] {
    return this.crossModuleEdgesByCaller.get(moduleName) ?? [];
  }

  searchLiterals(
    pattern: string,
    mode: "regex" | "literal",
    maxResults: number = 50
  ): LiteralCrossRef[] {
    const compiled =
      mode === "regex"
        ? new RegExp(pattern, "i")
        : new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i");

    const results: LiteralCrossRef[] = [];
    for (const [literal, modules] of this.literalsByValue) {
      if (results.length >= maxResults) break;
      if (!compiled.test(literal)) continue;

      const moduleCounts: Array<{ module: string; count: number }> = [];
      for (const mod of modules) {
        const analysis = this.analysisMap.get(mod);
        const count = analysis
          ? analysis.literals.filter((l) => l === literal).length
          : 1;
        moduleCounts.push({ module: mod, count });
      }
      moduleCounts.sort((a, b) => b.count - a.count);
      results.push({ literal, modules: moduleCounts });
    }

    return results;
  }

  listNativeModules(): NativeModuleMetadataResponse[] {
    return [...this.nativeModuleMap.values()].map((native) => ({
      snapshotId: this.snapshotId,
      name: native.name,
      url: native.url,
      contentHash: native.contentHash,
      sizeBytes: native.sizeBytes,
    }));
  }

  getNativeModuleRecord(name: string): SnapshotNativeModuleRecord {
    const record = this.nativeModuleMap.get(name);
    if (!record) throw new Error(`Native module "${name}" not found`);
    return record;
  }

  getNativeModulePath(name: string): string {
    const record = this.getNativeModuleRecord(name);
    return nativePath(this.manifest.platform, this.snapshotId, record.filePath);
  }

  async getNativeModuleAnalysis(name: string): Promise<WasmAnalysis> {
    const cached = this.nativeAnalysisCache.get(name);
    if (cached) return cached;

    const record = this.getNativeModuleRecord(name);
    if (!this.loadNativeAnalysisByPath) {
      throw new Error("Native module analysis loading not configured");
    }
    const analysis = await this.loadNativeAnalysisByPath(record.analysisPath);

    if ((!analysis.elements || !analysis.dataSegments) && this.loadNativeBinaryByPath) {
      try {
        const binary = await this.loadNativeBinaryByPath(record.filePath);
        const fresh = analyzeWasm(name, binary);
        analysis.elements = fresh.elements;
        analysis.dataSegments = fresh.dataSegments;
      } catch (err) {
        log.warn(`failed to backfill element/data segments for native module "${name}": ${String(err)}`);
      }
    }

    this.nativeAnalysisCache.set(name, analysis);
    return analysis;
  }

  async getWasmIndex(name: string): Promise<WasmIndex> {
    const record = this.getNativeModuleRecord(name);
    if (!this.loadNativeBinaryByPath) {
      throw new Error("Native module binary loading not configured");
    }
    const cached = this.wasmIndexCache.get(record.contentHash);
    if (cached) return cached;

    const build = (async () => {
      const [binary, analysis] = await Promise.all([
        this.loadNativeBinaryByPath!(record.filePath),
        this.getNativeModuleAnalysis(name),
      ]);
      return buildWasmIndex(binary, analysis);
    })();
    this.wasmIndexCache.set(record.contentHash, build);
    return build;
  }

  async getNativeModuleBinary(
    name: string,
    startByte?: number,
    endByte?: number
  ): Promise<{ binary: Buffer; startByte: number; endByte: number }> {
    const record = this.getNativeModuleRecord(name);
    if (!this.loadNativeBinaryByPath) {
      throw new Error("Native module binary loading not configured");
    }
    const full = await this.loadNativeBinaryByPath(record.filePath);
    const start = Math.max(0, startByte ?? 0);
    const end = Math.min(full.length, endByte ?? full.length);
    return { binary: full.subarray(start, end), startByte: start, endByte: end };
  }

  async getNativeModuleWat(name: string, functionIndex?: number): Promise<string> {
    const record = this.getNativeModuleRecord(name);

    const { watFileForModule, extractFunctionFromWatFile, disassembleWasm } =
      await import("../analysis/wasm-analyzer.js");

    if (functionIndex != null) {
      const wasmPath = this.getNativeModulePath(name);
      const watFile = await watFileForModule(wasmPath, `${wasmPath}.wat`);
      const result = await extractFunctionFromWatFile(watFile, functionIndex);
      if (result == null) {
        throw new Error(
          `Function index ${functionIndex} not found in native module "${name}"`
        );
      }
      return result;
    }

    if (!this.loadNativeBinaryByPath) {
      throw new Error("Native module binary loading not configured");
    }
    const binary = await this.loadNativeBinaryByPath(record.filePath);
    return await disassembleWasm(binary);
  }

  async getNativeModuleCrossReferences(name: string): Promise<WasmCrossReference> {
    const analysis = await this.getNativeModuleAnalysis(name);

    const loaderModules: string[] = [];
    const referencingModules: string[] = [];

    const wasmRelatedPatterns = [
      name,
      "WasmLoader",
      "WasmVariantLoader",
      "WasmWorker",
    ];

    for (const [moduleName, moduleRecord] of this.moduleMap) {
      for (const dep of moduleRecord.dependencies) {
        for (const pattern of wasmRelatedPatterns) {
          if (dep.toLowerCase().includes(pattern.toLowerCase())) {
            referencingModules.push(moduleName);
            break;
          }
        }
      }
    }

    for (const imp of analysis.imports) {
      if (imp.module === "env" || imp.module === "wasi_snapshot_preview1") continue;
      const matches = this.moduleMap.has(imp.module) ? [imp.module] : [];
      loaderModules.push(...matches);
    }

    return {
      loaderModules: [...new Set(loaderModules)],
      referencingModules: [...new Set(referencingModules)],
    };
  }

  async findNativeReferences(
    name: string,
    target: string,
    maxResults: number
  ): Promise<NativeReferenceResult[]> {
    const idx = await this.getWasmIndex(name);
    const out: NativeReferenceResult[] = [];
    const addr = parseAddress(target);
    if (addr != null) {
      const str = idx.stringContaining(addr);
      if (str) {
        for (const fn of idx.functionsReferencing(str)) {
          if (out.length >= maxResults) break;
          out.push({ funcIndex: fn, name: idx.nameOf(fn), strings: [str] });
        }
      }
      return out;
    }
    for (const fn of idx.functionsReferencingSubstring(target)) {
      if (out.length >= maxResults) break;
      const matched = (idx.stringRefs().byFunction.get(fn) ?? []).filter((s) => s.includes(target));
      out.push({ funcIndex: fn, name: idx.nameOf(fn), strings: matched });
    }
    return out;
  }

  async searchNativeData(
    name: string,
    pattern: string,
    mode: "regex" | "literal",
    maxResults: number
  ): Promise<NativeDataMatch[]> {
    const idx = await this.getWasmIndex(name);
    const re =
      mode === "regex"
        ? new RegExp(pattern, "i")
        : new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i");
    return idx.matchStrings((s) => re.test(s), maxResults).map((m) => ({
      address: m.address,
      value: m.value,
      referencedBy: idx.functionsReferencing(m.value),
    }));
  }

  async findNativeConst(name: string, value: number, maxResults: number): Promise<NativeReferenceResult[]> {
    const idx = await this.getWasmIndex(name);
    return idx
      .functionsWithI32Const(value)
      .slice(0, maxResults)
      .map((fn) => ({ funcIndex: fn, name: idx.nameOf(fn) }));
  }

  async traceNativeCallGraph(
    name: string,
    funcIndex: number,
    depth: number,
    direction: "forward" | "reverse"
  ): Promise<NativeCallGraphNode[]> {
    const idx = await this.getWasmIndex(name);
    const graph = idx.callGraph();
    const maxDepth = Math.max(0, depth);
    const visited = new Set<number>();
    const queue: Array<{ fn: number; depth: number }> = [{ fn: funcIndex, depth: 0 }];
    const out: NativeCallGraphNode[] = [];

    while (queue.length > 0) {
      const next = queue.shift()!;
      if (visited.has(next.fn) || next.depth > maxDepth) continue;
      visited.add(next.fn);

      const calls = [...(graph.calls.get(next.fn) ?? [])].sort((a, b) => a - b);
      const callers = [...(graph.callers.get(next.fn) ?? [])].sort((a, b) => a - b);
      const indirect = (graph.indirect.get(next.fn) ?? []).map((s) => ({
        typeIndex: s.typeIndex,
        tableIndex: s.tableIndex,
        candidates: idx.resolveIndirect(s.typeIndex, s.tableIndex),
      }));
      out.push({ funcIndex: next.fn, name: idx.nameOf(next.fn), depth: next.depth, direction, calls, callers, indirect });

      const neighbors = direction === "forward" ? calls : callers;
      if (next.depth < maxDepth) {
        for (const n of neighbors) if (!visited.has(n)) queue.push({ fn: n, depth: next.depth + 1 });
      }
    }
    return out;
  }

  async recoverNativeVtables(name: string, typeName: string, maxResults: number): Promise<NativeVtable[]> {
    const idx = await this.getWasmIndex(name);
    const out: NativeVtable[] = [];

    let candidates = idx.matchStrings((s) => s === typeName, 4);
    if (candidates.length === 0) candidates = idx.matchStrings((s) => s.includes(typeName), 4);
    if (candidates.length === 0) {
      return [
        {
          typeName,
          demangled: demangleItanium(typeName),
          ztsAddr: -1,
          ztiAddr: null,
          vtableAddr: null,
          addressPoint: null,
          slots: [],
          notes: ["typeinfo name string not found in active data segments"],
        },
      ];
    }

    for (const cand of candidates) {
      if (out.length >= maxResults) break;
      const notes: string[] = [];
      const ztsAddr = cand.address;
      let ztiAddr: number | null = null;
      let vtableAddr: number | null = null;
      let addressPoint: number | null = null;
      const slots: NativeVtableSlot[] = [];

      for (const namePtr of idx.findWordsEqualTo(ztsAddr)) {
        const ztiCandidate = namePtr - 4;
        const typeinfoSlots = idx.findWordsEqualTo(ztiCandidate);
        if (typeinfoSlots.length === 0) continue;
        ztiAddr = ztiCandidate;
        for (const tiSlot of typeinfoSlots) {
          const ap = tiSlot + 4;
          const fns = readVtableSlots(idx, ap);
          if (fns.length === 0) continue;
          vtableAddr = tiSlot - 4;
          addressPoint = ap;
          for (let k = 0; k < fns.length; k++) {
            slots.push({ slot: k, funcIndex: fns[k], name: idx.nameOf(fns[k]) });
          }
          break;
        }
        if (addressPoint != null) break;
      }

      if (addressPoint == null) {
        notes.push(
          "could not anchor a vtable for this typeinfo (RTTI may be absent, initialized from a passive segment, or use a multi-inheritance layout)"
        );
      }
      out.push({ typeName: cand.value, demangled: demangleItanium(cand.value), ztsAddr, ztiAddr, vtableAddr, addressPoint, slots, notes });
    }
    return out;
  }
}

function parseAddress(s: string): number | null {
  const t = s.trim();
  if (/^0x[0-9a-fA-F]+$/.test(t)) return Number.parseInt(t, 16);
  if (/^\d+$/.test(t)) return Number.parseInt(t, 10);
  return null;
}

function readVtableSlots(idx: WasmIndex, addressPoint: number): number[] {
  const out: number[] = [];
  const MAX_SLOTS = 4096;
  for (let k = 0; k < MAX_SLOTS; k++) {
    const word = idx.readU32(addressPoint + 4 * k);
    if (word == null) break;
    const fn = idx.funcAtSlot(word, 0);
    if (fn < 0) break;
    out.push(fn);
  }
  return out;
}

function demangleItanium(name: string): string {
  const m = /^_Z(?:TS|TI|TV)(.*)$/.exec(name);
  const body = m ? m[1] : name;
  let i = 0;
  const nested = body[i] === "N";
  if (nested) i++;
  const parts: string[] = [];
  while (i < body.length) {
    if (nested && body[i] === "E") break;
    const lenMatch = /^\d+/.exec(body.slice(i));
    if (!lenMatch) break;
    const len = Number.parseInt(lenMatch[0], 10);
    i += lenMatch[0].length;
    parts.push(body.slice(i, i + len));
    i += len;
  }
  return parts.length > 0 ? parts.join("::") : name;
}
