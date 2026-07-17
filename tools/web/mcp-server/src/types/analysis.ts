export type SymbolKind = "function" | "variable" | "class";

export interface SymbolRecord {
  id: string;
  module: string;
  name: string;
  kind: SymbolKind;
  startByte: number;
  endByte: number;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  hash: string;
}

export type ExportBindingKind =
  | "identifier"
  | "member"
  | "expression"
  | "unresolved";

export interface ExportBindingRecord {
  module: string;
  exportName: string;
  objectName: string | null;
  bindingKind: ExportBindingKind;
  localName: string | null;
  expression: string | null;
  startByte: number | null;
  endByte: number | null;
}

export interface ImportReferenceRecord {
  module: string;
  dependency: string;
  loader: "o" | "r";
  startByte: number;
  endByte: number;
}

export type ReferenceType = "identifier" | "dependency";

export interface ReferenceRecord {
  module: string;
  symbol: string;
  type: ReferenceType;
  startByte: number;
  endByte: number;
  context: string;
}

export interface CallEdgeRecord {
  module: string;
  caller: string;
  callee: string;
  startByte: number;
  endByte: number;
}

export interface CrossModuleCallEdgeRecord {
  callerModule: string;
  callerSymbol: string;
  calleeModule: string;
  calleeExport: string;
  startByte: number;
  endByte: number;
}

export interface DestructuredImportRecord {
  module: string;
  dependency: string;
  bindings: string[];
  startByte: number;
  endByte: number;
}

export interface SwitchDispatchRecord {
  module: string;
  symbol: string;
  discriminant: string;
  cases: Array<{ value: string | number | null; startByte: number; endByte: number }>;
  startByte: number;
  endByte: number;
}

export interface ModuleAnalysis {
  module: string;
  symbols: SymbolRecord[];
  exportBindings: ExportBindingRecord[];
  imports: ImportReferenceRecord[];
  references: ReferenceRecord[];
  callEdges: CallEdgeRecord[];
  crossModuleCallEdges?: CrossModuleCallEdgeRecord[];
  destructuredImports?: DestructuredImportRecord[];
  switchDispatches?: SwitchDispatchRecord[];
  literals: string[];
  parseError: string | null;
}
