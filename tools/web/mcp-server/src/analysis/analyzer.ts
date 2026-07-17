import {createHash} from "node:crypto";
import {parse} from "@babel/parser";
import traverseModule, {type NodePath} from "@babel/traverse";
import * as t from "@babel/types";
import type {
    CallEdgeRecord,
    CrossModuleCallEdgeRecord,
    DestructuredImportRecord,
    ExportBindingKind,
    ExportBindingRecord,
    ImportReferenceRecord,
    ModuleAnalysis,
    ReferenceRecord,
    SwitchDispatchRecord,
    SymbolKind,
    SymbolRecord,
} from "../types/analysis.js";
import type {ParsedModule} from "../types/module.js";
import {createLogger} from "../utils/logger.js";

const log = createLogger("analyzer");

const MAX_REFERENCES_PER_MODULE = 6_000;
const MAX_LITERALS_PER_MODULE = 500;
const MAX_CROSS_MODULE_EDGES = 2_000;
const MAX_SWITCH_DISPATCHES = 500;
const CONTEXT_RADIUS = 80;

function sha256(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function safeSlice(source: string, start: number, end: number): string {
  const from = Math.max(0, start);
  const to = Math.min(source.length, end);
  if (from >= to) return "";
  return source.slice(from, to);
}

function contextSlice(source: string, start: number, end: number): string {
  return safeSlice(source, start - CONTEXT_RADIUS, end + CONTEXT_RADIUS);
}

function toLoc(
  node: t.Node,
  fallbackStartByte: number,
  fallbackEndByte: number
): {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
} {
  if (node.loc) {
    return {
      startLine: node.loc.start.line,
      startColumn: node.loc.start.column,
      endLine: node.loc.end.line,
      endColumn: node.loc.end.column,
    };
  }
  return {
    startLine: 1,
    startColumn: fallbackStartByte,
    endLine: 1,
    endColumn: fallbackEndByte,
  };
}

function getMemberPropertyName(node: t.MemberExpression): string | null {
  if (t.isIdentifier(node.property) && !node.computed) {
    return node.property.name;
  }
  if (t.isStringLiteral(node.property)) {
    return node.property.value;
  }
  return null;
}

function resolveEnclosingSymbol(path: NodePath): string {
  const fn = path.getFunctionParent();
  if (!fn) return "__module__";
  if (fn.isFunctionDeclaration() && fn.node.id?.name) return fn.node.id.name;
  if (fn.isFunctionExpression() || fn.isArrowFunctionExpression()) {
    const parent = fn.parentPath;
    if (parent.isVariableDeclarator() && t.isIdentifier(parent.node.id)) {
      return parent.node.id.name;
    }
  }
  return "__module__";
}

function resolveCaller(path: NodePath<t.CallExpression>): string {
  return resolveEnclosingSymbol(path);
}

function createSymbolRecord(
  moduleName: string,
  name: string,
  kind: SymbolKind,
  node: t.Node,
  source: string
): SymbolRecord | null {
  if (node.start == null || node.end == null) return null;
  const snippet = safeSlice(source, node.start, node.end);
  const loc = toLoc(node, node.start, node.end);
  return {
    id: `${moduleName}:${name}:${node.start}`,
    module: moduleName,
    name,
    kind,
    startByte: node.start,
    endByte: node.end,
    startLine: loc.startLine,
    startColumn: loc.startColumn,
    endLine: loc.endLine,
    endColumn: loc.endColumn,
    hash: sha256(snippet),
  };
}

function classifyRightHandSide(
  right: t.Expression | t.PrivateName,
  source: string
): {
  bindingKind: ExportBindingKind;
  localName: string | null;
  expression: string | null;
} {
  if (t.isIdentifier(right)) {
    return {
      bindingKind: "identifier",
      localName: right.name,
      expression: null,
    };
  }

  if (t.isMemberExpression(right)) {
    const start = right.start ?? 0;
    const end = right.end ?? start;
    return {
      bindingKind: "member",
      localName: t.isIdentifier(right.object) ? right.object.name : null,
      expression: safeSlice(source, start, end),
    };
  }

  const start = (right as t.Node).start ?? 0;
  const end = (right as t.Node).end ?? start;
  return {
    bindingKind: "expression",
    localName: null,
    expression: safeSlice(source, start, end),
  };
}

function emptyAnalysisWithError(
  module: ParsedModule,
  error: unknown
): ModuleAnalysis {
  return {
    module: module.name,
    symbols: [],
    exportBindings: module.exports.map((exportName) => ({
      module: module.name,
      exportName,
      objectName: null,
      bindingKind: "unresolved",
      localName: null,
      expression: null,
      startByte: null,
      endByte: null,
    })),
    imports: [],
    references: [],
    callEdges: [],
    crossModuleCallEdges: [],
    destructuredImports: [],
    switchDispatches: [],
    literals: [],
    parseError: error instanceof Error ? error.message : String(error),
  };
}

export function analyzeModule(module: ParsedModule): ModuleAnalysis {
  const source = module.body;
  const symbols: SymbolRecord[] = [];
  const imports: ImportReferenceRecord[] = [];
  const references: ReferenceRecord[] = [];
  const callEdges: CallEdgeRecord[] = [];
  const crossModuleCallEdges: CrossModuleCallEdgeRecord[] = [];
  const destructuredImports: DestructuredImportRecord[] = [];
  const switchDispatches: SwitchDispatchRecord[] = [];
  const exportCandidates = new Map<string, ExportBindingRecord[]>();
  const literals = new Set<string>();
  const symbolNameSet = new Set<string>();
  const importBindings = new Map<string, string>();
  const importedExportNames = new Map<string, string>();

  let ast;
  try {
    ast = parse(source, {
      sourceType: "script",
      allowReturnOutsideFunction: true,
      errorRecovery: true,
      ranges: true,
      plugins: ["jsx"],
    });
  } catch (error) {
    return emptyAnalysisWithError(module, error);
  }

  traverseModule.default(ast, {
    FunctionDeclaration(path: NodePath<t.FunctionDeclaration>) {
      const { id } = path.node;
      if (!id?.name) return;
      const record = createSymbolRecord(
        module.name,
        id.name,
        "function",
        path.node,
        source
      );
      if (!record) return;
      symbols.push(record);
      symbolNameSet.add(id.name);
    },
    ClassDeclaration(path: NodePath<t.ClassDeclaration>) {
      const { id } = path.node;
      if (!id?.name) return;
      const record = createSymbolRecord(
        module.name,
        id.name,
        "class",
        path.node,
        source
      );
      if (!record) return;
      symbols.push(record);
      symbolNameSet.add(id.name);
    },
    VariableDeclarator(path: NodePath<t.VariableDeclarator>) {
      if (t.isIdentifier(path.node.id)) {
        const name = path.node.id.name;
        const record = createSymbolRecord(
          module.name,
          name,
          "variable",
          path.node,
          source
        );
        if (record) {
          symbols.push(record);
          symbolNameSet.add(name);
        }
      }

      const init = path.node.init;
      if (!init || !t.isCallExpression(init)) return;
      const initCallee = init.callee;
      if (!t.isIdentifier(initCallee)) return;
      if (initCallee.name !== "o" && initCallee.name !== "r") return;
      if (init.arguments.length === 0 || !t.isStringLiteral(init.arguments[0]))
        return;

      const dep = init.arguments[0].value;

      if (t.isIdentifier(path.node.id)) {
        importBindings.set(path.node.id.name, dep);
      } else if (t.isObjectPattern(path.node.id)) {
        const bindings: string[] = [];
        for (const prop of path.node.id.properties) {
          if (t.isObjectProperty(prop)) {
            let exportName: string | null = null;
            if (t.isIdentifier(prop.key) && !prop.computed) {
              exportName = prop.key.name;
            } else if (t.isStringLiteral(prop.key)) {
              exportName = prop.key.value;
            }
            if (exportName && t.isIdentifier(prop.value)) {
              const localName = prop.value.name;
              importBindings.set(localName, dep);
              importedExportNames.set(localName, exportName);
              bindings.push(exportName);
            }
          } else if (
            t.isRestElement(prop) &&
            t.isIdentifier(prop.argument)
          ) {
            importBindings.set(prop.argument.name, dep);
            bindings.push(prop.argument.name);
          }
        }
        if (
          bindings.length > 0 &&
          path.node.start != null &&
          path.node.end != null
        ) {
          destructuredImports.push({
            module: module.name,
            dependency: dep,
            bindings,
            startByte: path.node.start,
            endByte: path.node.end,
          });
        }
      }
    },
    AssignmentExpression(path: NodePath<t.AssignmentExpression>) {
      const left = path.node.left;
      if (!t.isMemberExpression(left)) return;
      const exportName = getMemberPropertyName(left);
      if (!exportName || !module.exports.includes(exportName)) return;
      const objectName = t.isIdentifier(left.object) ? left.object.name : null;
      const rhs = classifyRightHandSide(path.node.right, source);
      const startByte = path.node.right.start ?? null;
      const endByte = path.node.right.end ?? null;
      const record: ExportBindingRecord = {
        module: module.name,
        exportName,
        objectName,
        bindingKind: rhs.bindingKind,
        localName: rhs.localName,
        expression: rhs.expression,
        startByte,
        endByte,
      };
      const list = exportCandidates.get(exportName) ?? [];
      list.push(record);
      exportCandidates.set(exportName, list);
    },
    CallExpression(path: NodePath<t.CallExpression>) {
      const { callee, arguments: args } = path.node;

      if (
        t.isIdentifier(callee) &&
        (callee.name === "o" || callee.name === "r") &&
        args.length > 0 &&
        t.isStringLiteral(args[0]) &&
        path.node.start != null &&
        path.node.end != null
      ) {
        imports.push({
          module: module.name,
          dependency: args[0].value,
          loader: callee.name,
          startByte: path.node.start,
          endByte: path.node.end,
        });

        if (references.length < MAX_REFERENCES_PER_MODULE) {
          references.push({
            module: module.name,
            symbol: args[0].value,
            type: "dependency",
            startByte: path.node.start,
            endByte: path.node.end,
            context: contextSlice(source, path.node.start, path.node.end),
          });
        }
      }

      if (
        t.isMemberExpression(callee) &&
        !callee.computed &&
        t.isIdentifier(callee.object) &&
        t.isIdentifier(callee.property) &&
        crossModuleCallEdges.length < MAX_CROSS_MODULE_EDGES
      ) {
        const dep = importBindings.get(callee.object.name);
        if (dep && path.node.start != null && path.node.end != null) {
          crossModuleCallEdges.push({
            callerModule: module.name,
            callerSymbol: resolveCaller(path),
            calleeModule: dep,
            calleeExport: callee.property.name,
            startByte: path.node.start,
            endByte: path.node.end,
          });
        }
      }

      if (
        t.isIdentifier(callee) &&
        path.node.start != null &&
        path.node.end != null
      ) {
        const dep = importBindings.get(callee.name);
        if (dep && crossModuleCallEdges.length < MAX_CROSS_MODULE_EDGES) {
          crossModuleCallEdges.push({
            callerModule: module.name,
            callerSymbol: resolveCaller(path),
            calleeModule: dep,
            calleeExport:
              importedExportNames.get(callee.name) ?? callee.name,
            startByte: path.node.start,
            endByte: path.node.end,
          });
        } else if (!dep && symbolNameSet.has(callee.name)) {
          callEdges.push({
            module: module.name,
            caller: resolveCaller(path),
            callee: callee.name,
            startByte: path.node.start,
            endByte: path.node.end,
          });
        }
      }
    },
    Identifier(path: NodePath<t.Identifier>) {
      if (references.length >= MAX_REFERENCES_PER_MODULE) return;
      if (!path.isReferencedIdentifier()) return;
      if (path.node.start == null || path.node.end == null) return;
      if (
        path.parentPath.isMemberExpression() &&
        path.parentKey === "property" &&
        !path.parentPath.node.computed
      ) {
        return;
      }
      references.push({
        module: module.name,
        symbol: path.node.name,
        type: "identifier",
        startByte: path.node.start,
        endByte: path.node.end,
        context: contextSlice(source, path.node.start, path.node.end),
      });
    },
    StringLiteral(path: NodePath<t.StringLiteral>) {
      if (literals.size >= MAX_LITERALS_PER_MODULE) return;
      const value = path.node.value;
      if (value.length < 2 || value.length > 120) return;
      literals.add(value);
    },
    SwitchStatement(path: NodePath<t.SwitchStatement>) {
      if (switchDispatches.length >= MAX_SWITCH_DISPATCHES) return;
      const disc = path.node.discriminant;
      if (disc.start == null || disc.end == null) return;
      if (path.node.start == null || path.node.end == null) return;

      const discStr = safeSlice(source, disc.start, disc.end);
      const cases: Array<{
        value: string | number | null;
        startByte: number;
        endByte: number;
      }> = [];

      for (const sc of path.node.cases) {
        if (sc.start == null || sc.end == null) continue;
        let value: string | number | null = null;
        if (sc.test) {
          if (t.isStringLiteral(sc.test)) value = sc.test.value;
          else if (t.isNumericLiteral(sc.test)) value = sc.test.value;
        }
        cases.push({ value, startByte: sc.start, endByte: sc.end });
      }

      if (cases.length >= 2) {
        switchDispatches.push({
          module: module.name,
          symbol: resolveEnclosingSymbol(path),
          discriminant: discStr,
          cases,
          startByte: path.node.start,
          endByte: path.node.end,
        });
      }
    },
  });

  const objectUsageCounts = new Map<string, number>();
  for (const records of exportCandidates.values()) {
    for (const record of records) {
      if (!record.objectName) continue;
      objectUsageCounts.set(
        record.objectName,
        (objectUsageCounts.get(record.objectName) ?? 0) + 1
      );
    }
  }
  const preferredObject =
    [...objectUsageCounts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ??
    null;

  const resolvedExportBindings: ExportBindingRecord[] = module.exports.map(
    (exportName) => {
      const candidates = exportCandidates.get(exportName) ?? [];
      const preferredCandidates =
        preferredObject == null
          ? candidates
          : candidates.filter((record) => record.objectName === preferredObject);
      const selected = (preferredCandidates.length
        ? preferredCandidates
        : candidates
      )
        .slice()
        .sort((a, b) => (a.startByte ?? -1) - (b.startByte ?? -1))
        .at(-1);

      if (!selected) {
        return {
          module: module.name,
          exportName,
          objectName: preferredObject,
          bindingKind: "unresolved",
          localName: null,
          expression: null,
          startByte: null,
          endByte: null,
        };
      }
      return selected;
    }
  );

  return {
      module: module.name,
      symbols,
      exportBindings: resolvedExportBindings,
      imports,
      references,
      callEdges,
      crossModuleCallEdges,
      destructuredImports,
      switchDispatches,
      literals: [...literals],
      parseError: null,
  };
}

export function analyzeModules(modules: ParsedModule[]): ModuleAnalysis[] {
  log.info(`analyzing ${modules.length} modules`);
  const start = performance.now();
  const results = modules.map((module, index) => {
    if ((index + 1) % 500 === 0) {
      log.debug(`analyzed ${index + 1}/${modules.length} modules`);
    }
    return analyzeModule(module);
  });
  log.info(`analysis complete: ${results.length} modules in ${(performance.now() - start).toFixed(1)}ms`);
  return results;
}
