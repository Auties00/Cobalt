import { execFile } from "node:child_process";
import { createReadStream } from "node:fs";
import { access, mkdtemp, readFile, rename, rm, stat, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { createInterface } from "node:readline";
import { promisify } from "node:util";
import type {
    WasmAnalysis,
    WasmCustomSection,
    WasmDataSegment,
    WasmElementSegment,
    WasmExportEntry,
    WasmFunctionEntry,
    WasmGlobalEntry,
    WasmImportEntry,
    WasmMemoryEntry,
    WasmSectionSizes,
    WasmTableEntry,
    WasmTypeEntry,
} from "../types/wasm.js";
import { BinaryReader } from "./wasm-binary-reader.js";
import { decodeConstExpr } from "./wasm-decoder.js";

const execFileAsync = promisify(execFile);
const requireFromHere = createRequire(import.meta.url);

let wasm2watPath: string | null = null;

function getWasm2watPath(): string {
  if (wasm2watPath == null) {
    wasm2watPath = join(dirname(requireFromHere.resolve("wabt/package.json")), "bin", "wasm2wat");
  }
  return wasm2watPath;
}

const WASM2WAT_ARGS = ["--enable-all", "--generate-names"] as const;

const MAX_WHOLE_WAT_BYTES = 64 * 1024 * 1024;

async function fileExists(path: string): Promise<boolean> {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function runWasm2wat(sourceWasmPath: string, watPath: string): Promise<void> {
  const tmp = `${watPath}.tmp-${process.pid}`;
  try {
    await execFileAsync(
      process.execPath,
      [getWasm2watPath(), ...WASM2WAT_ARGS, sourceWasmPath, "-o", tmp],
      { maxBuffer: 16 * 1024 * 1024 }
    );
    await rename(tmp, watPath);
  } catch (error) {
    await rm(tmp, { force: true }).catch(() => {});
    throw error;
  }
}

export async function watFileForModule(sourceWasmPath: string, cacheWatPath: string): Promise<string> {
  if (!(await fileExists(cacheWatPath))) {
    await runWasm2wat(sourceWasmPath, cacheWatPath);
  }
  return cacheWatPath;
}

export async function extractFunctionFromWatFile(
  watPath: string,
  functionIndex: number
): Promise<string | null> {
  const importFuncRe = /^\s*\(import\b.*\(func\b/;
  const rl = createInterface({
    input: createReadStream(watPath, { encoding: "utf8" }),
    crlfDelay: Infinity,
  });

  let globalIdx = -1;
  let capturing = false;
  let depth = 0;
  const captured: string[] = [];

  try {
    for await (const line of rl) {
      if (!capturing) {
        if (importFuncRe.test(line)) {
          globalIdx++;
          continue;
        }
        const trimmed = line.trimStart();
        if (!trimmed.startsWith("(func ")) continue;
        globalIdx++;
        if (globalIdx !== functionIndex) continue;
        capturing = true;
        captured.push(line);
        for (const ch of trimmed) {
          if (ch === "(") depth++;
          else if (ch === ")") depth--;
        }
        if (depth <= 0) break;
        continue;
      }

      captured.push(line);
      for (const ch of line) {
        if (ch === "(") depth++;
        else if (ch === ")") depth--;
      }
      if (depth <= 0) break;
    }
  } finally {
    rl.close();
  }

  return captured.length ? captured.join("\n") : null;
}

const WASM_MAGIC = 0x6d736100;
const WASM_VERSION = 1;

const SECTION = {
  CUSTOM: 0,
  TYPE: 1,
  IMPORT: 2,
  FUNCTION: 3,
  TABLE: 4,
  MEMORY: 5,
  GLOBAL: 6,
  EXPORT: 7,
  START: 8,
  ELEMENT: 9,
  CODE: 10,
  DATA: 11,
  DATA_COUNT: 12,
} as const;

const EXTERNAL_KIND: Record<number, WasmExportEntry["kind"]> = {
  0: "function",
  1: "table",
  2: "memory",
  3: "global",
};

interface RawSection {
  id: number;
  offset: number;
  size: number;
  contentOffset: number;
}

function readSections(r: BinaryReader): RawSection[] {
  const magic = r.readU32Fixed();
  if (magic !== WASM_MAGIC) throw new Error("Invalid WASM magic number");
  const version = r.readU32Fixed();
  if (version !== WASM_VERSION) throw new Error(`Unsupported WASM version: ${version}`);

  const sections: RawSection[] = [];
  while (r.pos < r.length) {
    const id = r.readByte();
    const offset = r.pos - 1;
    const size = r.readU32Leb();
    const contentOffset = r.pos;
    sections.push({ id, offset, size, contentOffset });
    r.pos = contentOffset + size;
  }
  return sections;
}

function readFuncType(r: BinaryReader): WasmTypeEntry {
  const tag = r.readByte();
  if (tag !== 0x60) throw new Error(`Expected functype tag 0x60, got 0x${tag.toString(16)}`);
  const params = r.readVec(() => r.readValType());
  const results = r.readVec(() => r.readValType());
  return { params, results };
}

function readLimits(r: BinaryReader): { initial: number; maximum?: number } {
  const flags = r.readByte();
  const initial = r.readU32Leb();
  const maximum = flags & 0x01 ? r.readU32Leb() : undefined;
  return { initial, maximum };
}

function readGlobalType(r: BinaryReader): { valueType: string; mutable: boolean } {
  const valueType = r.readValType();
  const mutable = r.readByte() === 0x01;
  return { valueType, mutable };
}

function skipInitExpr(r: BinaryReader): void {
  decodeConstExpr(r);
}

function parseElementSection(r: BinaryReader, section: RawSection): WasmElementSegment[] {
  r.pos = section.contentOffset;
  return r.readVec(() => {
    const flags = r.readU32Leb();
    const active = (flags & 0x01) === 0;
    const explicitTable = (flags & 0x02) !== 0;
    const usesExpr = (flags & 0x04) !== 0;
    const declared = !active && explicitTable;

    let tableIndex = 0;
    let offset: number | null = null;

    if (active) {
      if (explicitTable) tableIndex = r.readU32Leb();
      const off = decodeConstExpr(r);
      offset = off.kind === "i32" ? off.value : null;
    }

    if ((flags & 0x03) !== 0) r.readByte();

    const funcIndices: number[] = [];
    const count = r.readU32Leb();
    for (let i = 0; i < count; i++) {
      if (usesExpr) {
        const item = decodeConstExpr(r);
        funcIndices.push(item.kind === "ref.func" ? item.index : -1);
      } else {
        funcIndices.push(r.readU32Leb());
      }
    }

    const mode: WasmElementSegment["mode"] = active ? "active" : declared ? "declared" : "passive";
    return { mode, tableIndex, offset, funcIndices };
  });
}

function parseDataSection(r: BinaryReader, section: RawSection): WasmDataSegment[] {
  r.pos = section.contentOffset;
  return r.readVec(() => {
    const flags = r.readU32Leb();
    const passive = flags === 1;
    let memoryIndex = 0;
    let offset: number | null = null;

    if (!passive) {
      if (flags === 2) memoryIndex = r.readU32Leb();
      const off = decodeConstExpr(r);
      offset = off.kind === "i32" ? off.value : null;
    }

    const byteLength = r.readU32Leb();
    const fileOffset = r.pos;
    r.skip(byteLength);

    return { mode: passive ? "passive" : "active", memoryIndex, offset, byteLength, fileOffset };
  });
}

function parseTypeSection(r: BinaryReader, section: RawSection): WasmTypeEntry[] {
  r.pos = section.contentOffset;
  return r.readVec(() => readFuncType(r));
}

function parseImportSection(
  r: BinaryReader,
  section: RawSection,
  types: WasmTypeEntry[]
): WasmImportEntry[] {
  r.pos = section.contentOffset;
  return r.readVec(() => {
    const module = r.readName();
    const name = r.readName();
    const kind = r.readByte();

    const entry: WasmImportEntry = {
      module,
      name,
      kind: EXTERNAL_KIND[kind] ?? "function",
    };

    switch (kind) {
      case 0: {
        const typeIndex = r.readU32Leb();
        entry.typeIndex = typeIndex;
        entry.type = types[typeIndex];
        break;
      }
      case 1: {
        const elementType = r.readValType();
        const limits = readLimits(r);
        entry.tableType = { elementType, ...limits };
        break;
      }
      case 2: {
          entry.memoryType = readLimits(r);
        break;
      }
      case 3: {
          entry.globalType = readGlobalType(r);
        break;
      }
    }

    return entry;
  });
}

function parseFunctionSection(r: BinaryReader, section: RawSection): number[] {
  r.pos = section.contentOffset;
  return r.readVec(() => r.readU32Leb());
}

function parseTableSection(r: BinaryReader, section: RawSection): WasmTableEntry[] {
  r.pos = section.contentOffset;
  return r.readVec(() => {
    const elementType = r.readValType();
    const limits = readLimits(r);
    return { elementType, ...limits };
  });
}

function parseMemorySection(r: BinaryReader, section: RawSection): WasmMemoryEntry[] {
  r.pos = section.contentOffset;
  return r.readVec(() => readLimits(r));
}

function parseGlobalSection(r: BinaryReader, section: RawSection): WasmGlobalEntry[] {
  r.pos = section.contentOffset;
  const count = r.readU32Leb();
  const globals: WasmGlobalEntry[] = [];
  for (let i = 0; i < count; i++) {
    const { valueType, mutable } = readGlobalType(r);
    skipInitExpr(r);
    globals.push({ index: i, valueType, mutable });
  }
  return globals;
}

function parseExportSection(r: BinaryReader, section: RawSection): WasmExportEntry[] {
  r.pos = section.contentOffset;
  return r.readVec(() => {
    const name = r.readName();
    const kindByte = r.readByte();
    const index = r.readU32Leb();
    return {
      name,
      kind: EXTERNAL_KIND[kindByte] ?? "function",
      index,
    };
  });
}

function parseStartSection(r: BinaryReader, section: RawSection): number {
  r.pos = section.contentOffset;
  return r.readU32Leb();
}

function parseCodeSection(
  r: BinaryReader,
  section: RawSection,
  typeIndices: number[],
  types: WasmTypeEntry[],
  importedFuncCount: number,
  functionNames: Map<number, string>
): WasmFunctionEntry[] {
  r.pos = section.contentOffset;
  const count = r.readU32Leb();
  const functions: WasmFunctionEntry[] = [];

  for (let i = 0; i < count; i++) {
    const bodySize = r.readU32Leb();
    const bodyOffset = r.pos;
    const funcIndex = importedFuncCount + i;
    const typeIndex = typeIndices[i] ?? -1;
    const funcType = types[typeIndex] ?? { params: [], results: [] };

    const localDecls: string[] = [];
    const localDeclCount = r.readU32Leb();
    for (let j = 0; j < localDeclCount; j++) {
      const localCount = r.readU32Leb();
      const localType = r.readValType();
      for (let k = 0; k < localCount; k++) localDecls.push(localType);
    }

    functions.push({
      index: funcIndex,
      name: functionNames.get(funcIndex),
      typeIndex,
      type: funcType,
      bodyOffset,
      bodySize,
      localDecls,
    });

    r.pos = bodyOffset + bodySize;
  }

  return functions;
}

function parseNameSection(
  r: BinaryReader,
  section: RawSection
): Map<number, string> {
  const names = new Map<number, string>();
  r.pos = section.contentOffset;
  const customName = r.readName();
  if (customName !== "name") return names;

  const end = section.contentOffset + section.size;
  while (r.pos < end) {
    const subsectionId = r.readByte();
    const subsectionSize = r.readU32Leb();
    const subsectionEnd = r.pos + subsectionSize;

    if (subsectionId === 1) {
      const count = r.readU32Leb();
      for (let i = 0; i < count; i++) {
        const index = r.readU32Leb();
        const name = r.readName();
        names.set(index, name);
      }
    }

    r.pos = subsectionEnd;
  }

  return names;
}

function resolveExportTypes(
  exports: WasmExportEntry[],
  functions: WasmFunctionEntry[],
  importedFuncCount: number,
  types: WasmTypeEntry[],
  imports: WasmImportEntry[]
): void {
  for (const exp of exports) {
    if (exp.kind !== "function") continue;
    if (exp.index < importedFuncCount) {
      exp.type = imports[exp.index]?.type;
    } else {
      const localIdx = exp.index - importedFuncCount;
      exp.type = functions[localIdx]?.type;
    }
  }
}

export function analyzeWasm(name: string, binary: Buffer | Uint8Array): WasmAnalysis {
  const r = new BinaryReader(binary);
  const rawSections = readSections(r);

  let types: WasmTypeEntry[] = [];
  let imports: WasmImportEntry[] = [];
  let typeIndices: number[] = [];
  let tables: WasmTableEntry[] = [];
  let memories: WasmMemoryEntry[] = [];
  let globals: WasmGlobalEntry[] = [];
  let exports: WasmExportEntry[] = [];
  let functions: WasmFunctionEntry[] = [];
  let elements: WasmElementSegment[] = [];
  let dataSegments: WasmDataSegment[] = [];
  let startFunction: number | undefined;
  const customSections: WasmCustomSection[] = [];
  let functionNames = new Map<number, string>();

  const sectionSizes: WasmSectionSizes = {
    type: 0, import: 0, function: 0, table: 0, memory: 0,
    global: 0, export: 0, start: 0, element: 0, code: 0,
    data: 0, dataCount: 0, custom: {},
  };

  const SECTION_SIZE_KEY: Record<number, keyof Omit<WasmSectionSizes, "custom">> = {
    [SECTION.TYPE]: "type",
    [SECTION.IMPORT]: "import",
    [SECTION.FUNCTION]: "function",
    [SECTION.TABLE]: "table",
    [SECTION.MEMORY]: "memory",
    [SECTION.GLOBAL]: "global",
    [SECTION.EXPORT]: "export",
    [SECTION.START]: "start",
    [SECTION.ELEMENT]: "element",
    [SECTION.CODE]: "code",
    [SECTION.DATA]: "data",
    [SECTION.DATA_COUNT]: "dataCount",
  };

  for (const section of rawSections) {
    const key = SECTION_SIZE_KEY[section.id];
    if (key) {
      sectionSizes[key] = section.size;
    } else if (section.id === SECTION.CUSTOM) {
      const savedPos = r.pos;
      r.pos = section.contentOffset;
      const customName = r.readName();
      r.pos = savedPos;
      sectionSizes.custom[customName] = (sectionSizes.custom[customName] ?? 0) + section.size;
      customSections.push({ name: customName, offset: section.offset, size: section.size });
    }
  }

  for (const section of rawSections) {
    if (section.id === SECTION.CUSTOM) {
      const savedPos = r.pos;
      r.pos = section.contentOffset;
      const peekName = r.readName();
      r.pos = savedPos;
      if (peekName === "name") {
        functionNames = parseNameSection(r, section);
      }
    }
  }

  for (const section of rawSections) {
    switch (section.id) {
      case SECTION.TYPE:
        types = parseTypeSection(r, section);
        break;
      case SECTION.IMPORT:
        imports = parseImportSection(r, section, types);
        break;
      case SECTION.FUNCTION:
        typeIndices = parseFunctionSection(r, section);
        break;
      case SECTION.TABLE:
        tables = parseTableSection(r, section);
        break;
      case SECTION.MEMORY:
        memories = parseMemorySection(r, section);
        break;
      case SECTION.GLOBAL:
        globals = parseGlobalSection(r, section);
        break;
      case SECTION.EXPORT:
        exports = parseExportSection(r, section);
        break;
      case SECTION.START:
        startFunction = parseStartSection(r, section);
        break;
      case SECTION.ELEMENT:
        elements = parseElementSection(r, section);
        break;
      case SECTION.DATA:
        dataSegments = parseDataSection(r, section);
        break;
    }
  }

  const importedFuncCount = imports.filter((i) => i.kind === "function").length;

  for (const section of rawSections) {
    if (section.id === SECTION.CODE) {
      functions = parseCodeSection(r, section, typeIndices, types, importedFuncCount, functionNames);
      break;
    }
  }

  resolveExportTypes(exports, functions, importedFuncCount, types, imports);

  for (const imp of imports) {
    if (imp.kind === "memory" && imp.memoryType) {
      memories.push(imp.memoryType);
    }
    if (imp.kind === "table" && imp.tableType) {
      tables.push(imp.tableType);
    }
  }

  return {
    name,
    types,
    imports,
    exports,
    functions,
    memories,
    tables,
    globals,
    startFunction,
    customSections,
    sectionSizes,
    totalSize: binary.length,
    elements,
    dataSegments,
  };
}

export async function disassembleWasm(binary: Buffer | Uint8Array): Promise<string> {
  const dir = await mkdtemp(join(tmpdir(), "wat-"));
  const wasmPath = join(dir, "module.wasm");
  const watPath = join(dir, "module.wat");
  try {
    await writeFile(wasmPath, binary);
    await runWasm2wat(wasmPath, watPath);
    const { size } = await stat(watPath);
    if (size > MAX_WHOLE_WAT_BYTES) {
      throw new Error(
        `Whole-module WAT is ${(size / 1048576).toFixed(0)} MB (exceeds the ` +
          `${MAX_WHOLE_WAT_BYTES / 1048576} MB cap); pass functionIndex to ` +
          `disassemble a single function.`
      );
    }
    return await readFile(watPath, "utf8");
  } finally {
    await rm(dir, { recursive: true, force: true }).catch(() => {});
  }
}

export async function disassembleWasmFunction(
  binary: Buffer | Uint8Array,
  functionIndex: number
): Promise<string | null> {
  const dir = await mkdtemp(join(tmpdir(), "wat-"));
  const wasmPath = join(dir, "module.wasm");
  const watPath = join(dir, "module.wat");
  try {
    await writeFile(wasmPath, binary);
    await runWasm2wat(wasmPath, watPath);
    return await extractFunctionFromWatFile(watPath, functionIndex);
  } finally {
    await rm(dir, { recursive: true, force: true }).catch(() => {});
  }
}
