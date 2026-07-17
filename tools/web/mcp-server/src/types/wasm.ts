export interface WasmTypeEntry {
  params: string[];
  results: string[];
}

export interface WasmImportEntry {
  module: string;
  name: string;
  kind: "function" | "table" | "memory" | "global";
  typeIndex?: number;
  type?: WasmTypeEntry;
  tableType?: { elementType: string; initial: number; maximum?: number };
  memoryType?: { initial: number; maximum?: number };
  globalType?: { valueType: string; mutable: boolean };
}

export interface WasmExportEntry {
  name: string;
  kind: "function" | "table" | "memory" | "global";
  index: number;
  type?: WasmTypeEntry;
}

export interface WasmFunctionEntry {
  index: number;
  name?: string;
  typeIndex: number;
  type: WasmTypeEntry;
  bodyOffset: number;
  bodySize: number;
  localDecls: string[];
}

export interface WasmMemoryEntry {
  initial: number;
  maximum?: number;
}

export interface WasmTableEntry {
  elementType: string;
  initial: number;
  maximum?: number;
}

export interface WasmGlobalEntry {
  index: number;
  valueType: string;
  mutable: boolean;
  name?: string;
}

export interface WasmCustomSection {
  name: string;
  offset: number;
  size: number;
}

export interface WasmElementSegment {

  mode: "active" | "passive" | "declared";

  tableIndex: number;

  offset: number | null;

  funcIndices: number[];
}

export interface WasmDataSegment {

  mode: "active" | "passive";

  memoryIndex: number;

  offset: number | null;

  byteLength: number;

  fileOffset: number;
}

export interface WasmSectionSizes {
  type: number;
  import: number;
  function: number;
  table: number;
  memory: number;
  global: number;
  export: number;
  start: number;
  element: number;
  code: number;
  data: number;
  dataCount: number;
  custom: Record<string, number>;
}

export interface WasmCrossReference {
  loaderModules: string[];
  referencingModules: string[];
}

export interface WasmAnalysis {
  name: string;
  types: WasmTypeEntry[];
  imports: WasmImportEntry[];
  exports: WasmExportEntry[];
  functions: WasmFunctionEntry[];
  memories: WasmMemoryEntry[];
  tables: WasmTableEntry[];
  globals: WasmGlobalEntry[];
  startFunction?: number;
  customSections: WasmCustomSection[];
  sectionSizes: WasmSectionSizes;
  totalSize: number;

  elements?: WasmElementSegment[];

  dataSegments?: WasmDataSegment[];
}
