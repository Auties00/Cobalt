import { execFile } from "node:child_process";
import { access, mkdir, mkdtemp, readFile, rename, rm, writeFile } from "node:fs/promises";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";
import { createLogger } from "../utils/logger.js";

const log = createLogger("ghidra");

const CURRENT_DIR = dirname(fileURLToPath(import.meta.url));
const SCRIPTS_DIR = join(CURRENT_DIR, "..", "..", "scripts");
const GHIDRA_SCRIPT_NAME = "DecompileToJson.java";
const GHIDRA_WASM_SCRIPT_NAME = "DecompileWasmFuncsToJson.java";

const DEFAULT_ANALYSIS_TIMEOUT_SEC = 1800;
const DEFAULT_MAX_CPU = 4;

export interface GhidraOptions {
  ghidraPath?: string;
  analysisTimeoutSec?: number;
  maxCpu?: number;
  processor?: string;
  compilerSpec?: string;
}

export interface GhidraDecompiledFunction {
  name: string;
  address: string;
  signature: string;
  size: number;
  isThunk: boolean;
  isExternal: boolean;
  decompiled: boolean;
  code: string;
}

export interface GhidraOutput {
  binary: string;
  architecture: string;
  compiler: string;
  functionCount: number;
  functions: GhidraDecompiledFunction[];
}

export async function findGhidraInstallation(
  explicitPath?: string
): Promise<string> {

  if (explicitPath) {
    const headless = analyzeHeadlessPath(explicitPath);
    if (await fileExists(headless)) return explicitPath;
    throw new Error(`Ghidra not found at: ${explicitPath}`);
  }

  const envPath = process.env.GHIDRA_INSTALL_DIR;
  if (envPath) {
    const headless = analyzeHeadlessPath(envPath);
    if (await fileExists(headless)) return envPath;
  }

  const candidates = [

    "/opt/homebrew/Caskroom/ghidra",

    "/opt/ghidra",
    "/usr/share/ghidra",

    join(process.env.HOME ?? "", "ghidra"),
  ];

  for (const candidate of candidates) {
    const headless = analyzeHeadlessPath(candidate);
    if (await fileExists(headless)) return candidate;

    try {
      const { readdir } = await import("node:fs/promises");
      const entries = await readdir(candidate);
      for (const entry of entries) {
        const subPath = join(candidate, entry);

        const nested = await readdir(subPath).catch(() => []);
        for (const n of nested) {
          if (n.startsWith("ghidra_")) {
            const ghidraDir = join(subPath, n);
            if (await fileExists(analyzeHeadlessPath(ghidraDir))) return ghidraDir;
          }
        }
      }
    } catch {

    }
  }

  try {
    await execFileAsync("analyzeHeadless", ["--help"]);
    return "";
  } catch {

  }

  throw new Error(
    "Ghidra installation not found. Set GHIDRA_INSTALL_DIR environment variable " +
      "or install Ghidra: brew install --cask ghidra (macOS) or download from https://ghidra-sre.org"
  );
}

export async function decompileBinary(
  binaryPath: string,
  options: GhidraOptions = {}
): Promise<GhidraOutput> {
  const ghidraDir = await findGhidraInstallation(options.ghidraPath);
  const headless = ghidraDir
    ? analyzeHeadlessPath(ghidraDir)
    : "analyzeHeadless";

  const projectDir = await mkdtemp(join(tmpdir(), "ghidra-"));
  const outputPath = join(projectDir, "decompiled.json");

  const args = [
    projectDir,
    "TempProject",
    "-import",
    binaryPath,
    "-processor",
    options.processor ?? "AARCH64:LE:64:v8A",
    "-cspec",
    options.compilerSpec ?? "default",
    "-scriptPath",
    SCRIPTS_DIR,
    "-postScript",
    GHIDRA_SCRIPT_NAME,
    outputPath,
    "-analysisTimeoutPerFile",
    String(options.analysisTimeoutSec ?? DEFAULT_ANALYSIS_TIMEOUT_SEC),
    "-max-cpu",
    String(options.maxCpu ?? DEFAULT_MAX_CPU),
    "-deleteProject",
  ];

  log.info(`running headless analysis on ${binaryPath}`);
  log.debug(`command: ${headless} ${args.join(" ")}`);

  try {
    const { stdout, stderr } = await execFileAsync(headless, args, {
      timeout: (options.analysisTimeoutSec ?? DEFAULT_ANALYSIS_TIMEOUT_SEC) * 1000 * 2,
      maxBuffer: 50 * 1024 * 1024,
    });

    if (stderr) {

      const lines = stderr.split("\n");
      for (const line of lines) {
        if (
          line.includes("Decompiled ") ||
          line.includes("Done.") ||
          line.includes("ERROR") ||
          line.includes("WARN")
        ) {
          log.debug(line.trim());
        }
      }
    }
  } catch (error) {
    await rm(projectDir, { recursive: true, force: true }).catch(() => {});
    const message =
      error instanceof Error ? error.message : String(error);
    throw new Error(`Ghidra analysis failed: ${message}`);
  }

  if (!(await fileExists(outputPath))) {
    await rm(projectDir, { recursive: true, force: true }).catch(() => {});
    throw new Error(
      "Ghidra analysis completed but no output file was produced. " +
        "The binary may be encrypted or unsupported."
    );
  }

  const raw = await readFile(outputPath, "utf8");
  await rm(projectDir, { recursive: true, force: true }).catch(() => {});

  return JSON.parse(raw) as GhidraOutput;
}

export interface WasmDecompileResult {
  funcIndex: number;
  name: string;
  address: string;
  signature: string;
  decompiled: boolean;
  code: string;
  error?: string;
  hint?: string;
}

export interface WasmDecompileOptions extends GhidraOptions {
  cacheDir?: string;
}

export async function decompileWasmFunctions(
  wasmPath: string,
  funcIndices: number[],
  options: WasmDecompileOptions = {}
): Promise<WasmDecompileResult[]> {
  if (funcIndices.length === 0) return [];

  const { cacheDir } = options;
  const resultByIdx = new Map<number, WasmDecompileResult>();
  const misses: number[] = [];
  for (const idx of new Set(funcIndices)) {
    const cached = cacheDir ? await readDecompileCache(cacheDir, idx) : null;
    if (cached) resultByIdx.set(idx, cached);
    else misses.push(idx);
  }

  if (misses.length > 0) {
    const fresh = await runWasmDecompile(wasmPath, misses, options);
    for (const result of fresh) {
      resultByIdx.set(result.funcIndex, result);
      if (cacheDir) {
        await writeDecompileCache(cacheDir, result.funcIndex, result).catch(() => {});
      }
    }
  }

  const out: WasmDecompileResult[] = [];
  for (const idx of funcIndices) {
    const result = resultByIdx.get(idx);
    if (result) out.push(withDecompileHint(result));
  }
  return out;
}

async function runWasmDecompile(
  wasmPath: string,
  funcIndices: number[],
  options: GhidraOptions
): Promise<WasmDecompileResult[]> {
  const ghidraDir = await findGhidraInstallation(options.ghidraPath);
  const headless = ghidraDir ? analyzeHeadlessPath(ghidraDir) : "analyzeHeadless";

  const projectDir = await mkdtemp(join(tmpdir(), "ghidra-wasm-"));
  const outputPath = join(projectDir, "funcs.json");
  const indicesPath = join(projectDir, "indices.txt");
  await writeFile(indicesPath, funcIndices.join("\n"));
  const timeoutSec = options.analysisTimeoutSec ?? DEFAULT_ANALYSIS_TIMEOUT_SEC;

  const args = [
    projectDir,
    "WasmProj",
    "-import",
    wasmPath,
    "-noanalysis",
    "-scriptPath",
    SCRIPTS_DIR,
    "-postScript",
    GHIDRA_WASM_SCRIPT_NAME,
    indicesPath,
    outputPath,
    "-deleteProject",
  ];

  if (options.maxCpu != null) {
    args.push("-max-cpu", String(options.maxCpu));
  }

  log.info(`running headless wasm decompile on ${wasmPath} funcIndices=[${funcIndices.join(",")}]`);
  try {
    await execFileAsync(headless, args, {
      timeout: timeoutSec * 1000 * 2,
      maxBuffer: 50 * 1024 * 1024,
      env: { ...process.env, MAXMEM: process.env.MAXMEM ?? "6G" },
    });
  } catch (error) {
    await rm(projectDir, { recursive: true, force: true }).catch(() => {});
    const message = error instanceof Error ? error.message : String(error);
    throw new Error(
      `Ghidra wasm decompile failed (is the ghidra-wasm-plugin installed in the Ghidra Extensions dir?): ${message}`
    );
  }

  if (!(await fileExists(outputPath))) {
    await rm(projectDir, { recursive: true, force: true }).catch(() => {});
    throw new Error("Ghidra wasm decompile produced no output (function index out of range or plugin missing).");
  }

  const raw = await readFile(outputPath, "utf8");
  await rm(projectDir, { recursive: true, force: true }).catch(() => {});
  return JSON.parse(raw) as WasmDecompileResult[];
}

function withDecompileHint(result: WasmDecompileResult): WasmDecompileResult {
  if (result.decompiled) return result;
  return {
    ...result,
    hint:
      result.hint ??
      "Ghidra's wasm plugin could not decode this function (commonly the SIMD/atomics " +
        "opcodes it mis-decodes). Request the same index with format='wat' for a readable disassembly.",
  };
}

async function readDecompileCache(
  cacheDir: string,
  funcIndex: number
): Promise<WasmDecompileResult | null> {
  try {
    const raw = await readFile(join(cacheDir, `${funcIndex}.json`), "utf8");
    return JSON.parse(raw) as WasmDecompileResult;
  } catch {
    return null;
  }
}

async function writeDecompileCache(
  cacheDir: string,
  funcIndex: number,
  result: WasmDecompileResult
): Promise<void> {
  await mkdir(cacheDir, { recursive: true });
  const finalPath = join(cacheDir, `${funcIndex}.json`);
  const tmpPath = `${finalPath}.tmp-${process.pid}`;
  await writeFile(tmpPath, JSON.stringify(result));
  await rename(tmpPath, finalPath);
}

function analyzeHeadlessPath(ghidraDir: string): string {
  if (process.platform === "win32") {
    return join(ghidraDir, "support", "analyzeHeadless.bat");
  }
  return join(ghidraDir, "support", "analyzeHeadless");
}

async function fileExists(path: string): Promise<boolean> {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

function execFileAsync(
  command: string,
  args: string[],
  options?: { timeout?: number; maxBuffer?: number; env?: NodeJS.ProcessEnv }
): Promise<{ stdout: string; stderr: string }> {

  const isWindowsBatch = process.platform === "win32" && /\.(bat|cmd)$/i.test(command);
  const spawnCommand = isWindowsBatch ? `"${command}"` : command;
  const spawnArgs = isWindowsBatch ? args.map((arg) => `"${arg}"`) : args;
  return new Promise((resolve, reject) => {
    execFile(
      spawnCommand,
      spawnArgs,
      {
        timeout: options?.timeout ?? 0,
        maxBuffer: options?.maxBuffer ?? 10 * 1024 * 1024,
        encoding: "utf8",
        shell: isWindowsBatch,
        windowsHide: true,
        ...(options?.env ? { env: options.env } : {}),
      },
      (error, stdout, stderr) => {
        if (error) {
          reject(error);
        } else {
          resolve({ stdout: stdout as string, stderr: stderr as string });
        }
      }
    );
  });
}
