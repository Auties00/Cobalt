import { createHash } from "node:crypto";
import { existsSync } from "node:fs";
import { mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { ParsedModule, ParsedNativeModule } from "../types/module.js";
import type {
  SnapshotModuleRecord,
  SnapshotNativeModuleRecord,
  SnapshotPlatform,
} from "../types/snapshot.js";

export const SCHEMA_VERSION = 1;
const CURRENT_DIR = dirname(fileURLToPath(import.meta.url));
const DATA_DIR = join(CURRENT_DIR, "..", "..", "data");
export const SNAPSHOTS_DIR = join(DATA_DIR, "snapshots");

export const ALL_PLATFORMS: SnapshotPlatform[] = ["web", "desktop_windows", "desktop_macos", "ios"];

export function resolveAutoPlatforms(): SnapshotPlatform[] {
  const os = process.platform;
  if (os === "win32") return ["web", "desktop_windows"];
  if (os === "darwin") return ["web", "desktop_macos"];
  return ["web"];
}

export const DEFAULT_MACOS_APP_BINARY_PATH =
  "/Applications/WhatsApp.app/Contents/MacOS/WhatsApp";
export const DEFAULT_MACOS_APP_BUNDLE_PATH = "/Applications/WhatsApp.app";

export function resolveDefaultMacosBinaryPath(): string | undefined {
  if (existsSync(DEFAULT_MACOS_APP_BINARY_PATH)) return DEFAULT_MACOS_APP_BINARY_PATH;
  if (existsSync(DEFAULT_MACOS_APP_BUNDLE_PATH)) return DEFAULT_MACOS_APP_BUNDLE_PATH;
  return undefined;
}

export const DEFAULT_PLATFORM: SnapshotPlatform = "web";

export function platformDir(platform: SnapshotPlatform): string {
  return join(SNAPSHOTS_DIR, platform);
}

export interface SourceFileEntry {
  moduleName: string;
  sourcePath: string;
}

function sha256(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map((v) => stableStringify(v)).join(",")}]`;
  }
  if (value && typeof value === "object") {
    const entries = Object.entries(value as Record<string, unknown>).sort(
      ([a], [b]) => a.localeCompare(b)
    );
    return `{${entries
      .map(([k, v]) => `${JSON.stringify(k)}:${stableStringify(v)}`)
      .join(",")}}`;
  }
  return JSON.stringify(value);
}

function sanitizeModuleName(name: string): string {
  return name
    .replace(/[<>:"/\\|?*\x00-\x1f]/g, "_")
    .replace(/\s+/g, "_")
    .slice(0, 180);
}

export function parseExports(raw: unknown): string[] {
  if (!Array.isArray(raw)) return [];
  const result: string[] = [];
  for (const item of raw) {
    if (typeof item === "string") {
      result.push(item);
      continue;
    }
    if (
      item &&
      typeof item === "object" &&
      typeof (item as Record<string, unknown>).name === "string"
    ) {
      result.push((item as Record<string, unknown>).name as string);
    }
  }
  return [...new Set(result)];
}

export function parseDependencies(raw: unknown): string[] {
  if (!Array.isArray(raw)) return [];
  return raw.filter((x): x is string => typeof x === "string");
}

function snapshotDir(platform: SnapshotPlatform, snapshotId: string): string {
  return join(platformDir(platform), snapshotId);
}

export function manifestPath(platform: SnapshotPlatform, snapshotId: string): string {
  return join(snapshotDir(platform, snapshotId), "manifest.json");
}

export function indexPath(platform: SnapshotPlatform, snapshotId: string): string {
  return join(snapshotDir(platform, snapshotId), "index.json");
}

export function sourcePath(platform: SnapshotPlatform, snapshotId: string, relativePath: string): string {
  return join(snapshotDir(platform, snapshotId), relativePath);
}

export function snapshotIdForRevision(revision: string, globalHash: string): string {
  const clean = revision.replace(/[^a-zA-Z0-9._-]/g, "_");
  return `${clean}-${globalHash.slice(0, 12)}`;
}

export async function ensureDirectories(platform?: SnapshotPlatform): Promise<void> {
  if (platform) {
    await mkdir(platformDir(platform), { recursive: true });
  } else {
    await mkdir(SNAPSHOTS_DIR, { recursive: true });
  }
}

export function moduleToRecord(
  module: ParsedModule,
  fileEntry: SourceFileEntry
): SnapshotModuleRecord {
  const dependencies = [...module.dependencies];
  const exports = [...module.exports];
  const sourceHash = sha256(module.body);
  const metadataHash = sha256(
    stableStringify({
      name: module.name,
      dependencies,
      exports,
    })
  );
  return {
    name: module.name,
    dependencies,
    exports,
    sourcePath: fileEntry.sourcePath,
    sourceHash,
    metadataHash,
    sourceBytes: new TextEncoder().encode(module.body).length,
  };
}

export function computeGlobalHash(records: SnapshotModuleRecord[]): string {
  const normalized = records
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((record) => ({
      name: record.name,
      sourceHash: record.sourceHash,
      metadataHash: record.metadataHash,
    }));
  return sha256(stableStringify(normalized));
}

export function sourceDirPath(platform: SnapshotPlatform, snapshotId: string): string {
  return join(snapshotDir(platform, snapshotId), "sources");
}

export function sanitizeModuleFilename(name: string): string {
  return sanitizeModuleName(name);
}

export function nativesDirPath(platform: SnapshotPlatform, snapshotId: string): string {
  return join(snapshotDir(platform, snapshotId), "natives");
}

export function nativePath(platform: SnapshotPlatform, snapshotId: string, relativePath: string): string {
  return join(snapshotDir(platform, snapshotId), relativePath);
}

export function nativeModuleToRecord(
  native: ParsedNativeModule,
  fileName: string,
  contentHash: string
): SnapshotNativeModuleRecord {
  return {
    name: native.name,
    url: native.url,
    filePath: join("natives", `${fileName}.wasm`),
    analysisPath: join("natives", `${fileName}.analysis.json`),
    contentHash,
    sizeBytes: native.binary.length,
  };
}
