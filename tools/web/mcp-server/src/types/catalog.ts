import type {
  ExportBindingKind,
  ReferenceType,
  SymbolRecord,
} from "./analysis.js";

export interface ModuleMetadataResponse {
  snapshotId: string;
  name: string;
  dependencies: string[];
  exports: string[];
  sourceHash: string;
  sourceBytes: number;
}

export interface ModuleSearchResult {
  snapshotId: string;
  name: string;
  score: number;
  exports: string[];
  dependencies: string[];
}

export interface LiteralCrossRef {
  literal: string;
  modules: Array<{ module: string; count: number }>;
}

export interface EnrichedChangedModule {
  module: string;
  sourceChanged: boolean;
  dependenciesChanged: boolean;
  exportsAdded: string[];
  exportsRemoved: string[];
  symbolsAdded: string[];
  symbolsRemoved: string[];
  symbolsChanged: string[];
  excerpts: Record<string, { before: string | null; after: string | null }>;
}

export interface ResolvedExport {
  snapshotId: string;
  module: string;
  exportName: string;
  bindingKind: ExportBindingKind;
  localName: string | null;
  expression: string | null;
  symbol: SymbolRecord | null;
  sourceHash: string;
}

export interface SymbolSourceResult {
  snapshotId: string;
  module: string;
  symbol: SymbolRecord;
  source: string;
  sourceHash: string;
}

export interface DependencyTraceNode {
  module: string;
  depth: number;
  direction: "forward" | "reverse";
  dependencies: string[];
  dependents: string[];
}

export interface CodeSearchResult {
  module: string;
  startByte: number;
  endByte: number;
  context: string;
}

export interface ReferenceSearchResult {
  module: string;
  symbol: string;
  type: ReferenceType;
  startByte: number;
  endByte: number;
  context: string;
}

export interface NativeModuleMetadataResponse {
  snapshotId: string;
  name: string;
  url: string;
  contentHash: string;
  sizeBytes: number;
}

export interface NativeReferenceResult {
  funcIndex: number;
  name?: string;

  strings?: string[];
}

export interface NativeDataMatch {

  address: number;
  value: string;

  referencedBy: number[];
}

export interface NativeIndirectEdge {
  typeIndex: number;
  tableIndex: number;

  candidates: number[];
}

export interface NativeCallGraphNode {
  funcIndex: number;
  name?: string;
  depth: number;
  direction: "forward" | "reverse";
  calls: number[];
  callers: number[];
  indirect: NativeIndirectEdge[];
}

export interface NativeVtableSlot {
  slot: number;

  funcIndex: number;
  name?: string;
}

export interface NativeVtable {

  typeName: string;

  demangled: string;
  ztsAddr: number;
  ztiAddr: number | null;
  vtableAddr: number | null;
  addressPoint: number | null;
  slots: NativeVtableSlot[];

  notes: string[];
}

