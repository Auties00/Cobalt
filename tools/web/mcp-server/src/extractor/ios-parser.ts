import type { ParsedModule } from "../types/module.js";
import type { GhidraDecompiledFunction, GhidraOutput } from "./ghidra.js";

interface ObjCClass {
  name: string;
  superClass: string | null;
  methods: GhidraDecompiledFunction[];
}

const OBJC_METHOD_RE = /^([+-])\[(\S+)\s+(.+)]$/;

export function parseGhidraOutput(output: GhidraOutput): ParsedModule[] {
  const classMap = new Map<string, ObjCClass>();
  const standaloneFunctions: GhidraDecompiledFunction[] = [];

  for (const func of output.functions) {
    if (func.isThunk || func.isExternal) continue;
    if (!func.decompiled || !func.code) continue;

    const match = OBJC_METHOD_RE.exec(func.name);
    if (match) {
      const className = match[2];
      let cls = classMap.get(className);
      if (!cls) {
        cls = { name: className, superClass: null, methods: [] };
        classMap.set(className, cls);
      }
      cls.methods.push(func);
    } else {
      standaloneFunctions.push(func);
    }
  }

  for (const cls of classMap.values()) {
    cls.superClass = inferSuperClass(cls);
  }

  const modules: ParsedModule[] = [];

  for (const cls of classMap.values()) {
    const exports = cls.methods.map((m) => {
      const match = OBJC_METHOD_RE.exec(m.name);
      return match ? `${match[1]}${match[3]}` : m.name;
    });

    const dependencies = inferDependencies(cls, classMap);
    const body = generateClassSource(cls);

    modules.push({
      name: cls.name,
      dependencies,
      exports,
      body,
    });
  }

  if (standaloneFunctions.length > 0) {
    const grouped = groupStandaloneFunctions(standaloneFunctions);
    for (const [groupName, funcs] of grouped) {
      const exports = funcs.map((f) => f.name);
      const body = funcs
        .map((f) => `// ${f.signature}\n// Address: ${f.address}\n${f.code}`)
        .join("\n\n");

      modules.push({
        name: groupName,
        dependencies: [],
        exports,
        body,
      });
    }
  }

  return modules;
}

function inferSuperClass(cls: ObjCClass): string | null {

  for (const method of cls.methods) {

    const superMatch = method.code.match(
      /\b(\w+)_super\b|objc_msgSendSuper.*?&OBJC_CLASS_\$_(\w+)/
    );
    if (superMatch) {
      return superMatch[1] ?? superMatch[2] ?? null;
    }
  }
  return null;
}

function inferDependencies(
  cls: ObjCClass,
  allClasses: Map<string, ObjCClass>
): string[] {
  const deps = new Set<string>();

  for (const method of cls.methods) {

    const classRefPattern = /OBJC_CLASS_\$_(\w+)/g;
    let match: RegExpExecArray | null;
    while ((match = classRefPattern.exec(method.code))) {
      const refClass = match[1];
      if (refClass !== cls.name && allClasses.has(refClass)) {
        deps.add(refClass);
      }
    }

    const msgSendPattern = /\[(\w+)\s+\w+/g;
    while ((match = msgSendPattern.exec(method.code))) {
      const refClass = match[1];
      if (
        refClass !== cls.name &&
        refClass !== "self" &&
        refClass !== "super" &&
        allClasses.has(refClass)
      ) {
        deps.add(refClass);
      }
    }

    const typeRefPattern = /(\w+)\s*\*/g;
    while ((match = typeRefPattern.exec(method.signature))) {
      const refClass = match[1];
      if (
        refClass !== cls.name &&
        refClass !== "void" &&
        refClass !== "id" &&
        refClass !== "SEL" &&
        refClass !== "char" &&
        refClass !== "int" &&
        refClass !== "unsigned" &&
        allClasses.has(refClass)
      ) {
        deps.add(refClass);
      }
    }
  }

  return [...deps].sort();
}

function generateClassSource(cls: ObjCClass): string {
  const lines: string[] = [];

  const superPart = cls.superClass ? ` : ${cls.superClass}` : "";
  lines.push(`@interface ${cls.name}${superPart}`);
  lines.push("");

  const instanceMethods = cls.methods.filter((m) => m.name.startsWith("-["));
  const classMethods = cls.methods.filter((m) => m.name.startsWith("+["));

  if (classMethods.length > 0) {
    lines.push("// Class methods");
    for (const m of classMethods) {
      const sel = OBJC_METHOD_RE.exec(m.name);
      lines.push(`// + (${extractReturnType(m.signature)})${sel ? sel[3] : m.name};`);
    }
    lines.push("");
  }

  if (instanceMethods.length > 0) {
    lines.push("// Instance methods");
    for (const m of instanceMethods) {
      const sel = OBJC_METHOD_RE.exec(m.name);
      lines.push(`// - (${extractReturnType(m.signature)})${sel ? sel[3] : m.name};`);
    }
    lines.push("");
  }

  lines.push("@end");
  lines.push("");
  lines.push("// === Decompiled Implementation ===");
  lines.push("");

  for (const m of [...classMethods, ...instanceMethods]) {
    lines.push(`// ${m.name}`);
    lines.push(`// Address: ${m.address} | Size: ${m.size} bytes`);
    lines.push(m.code);
    lines.push("");
  }

  return lines.join("\n");
}

function extractReturnType(signature: string): string {

  const match = signature.match(/^(\S+)\s/);
  if (match) {
    const type = match[1];
    if (type === "undefined" || type === "undefined8") return "id";
    if (type === "undefined4") return "int";
    if (type === "undefined1") return "BOOL";
    return type;
  }
  return "id";
}

function groupStandaloneFunctions(
  functions: GhidraDecompiledFunction[]
): Map<string, GhidraDecompiledFunction[]> {
  const groups = new Map<string, GhidraDecompiledFunction[]>();

  for (const func of functions) {

    let groupName = "_CFunctions";

    const prefixMatch = func.name.match(/^_?([A-Z][a-zA-Z]+?)_/);
    if (prefixMatch) {
      groupName = `_C_${prefixMatch[1]}`;
    }

    let group = groups.get(groupName);
    if (!group) {
      group = [];
      groups.set(groupName, group);
    }
    group.push(func);
  }

  return groups;
}
