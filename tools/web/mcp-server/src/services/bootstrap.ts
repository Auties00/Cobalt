import { basename } from "node:path";
import { analyzeModules } from "../analysis/analyzer.js";
import { checkRevision, extractModules } from "../extractor/browser.js";
import { extractNativeMachOModules } from "../extractor/ios.js";
import {
  createSnapshot,
  findSnapshotByRevision,
  indexExists,
  listAllSnapshots,
  listSnapshots,
  loadIndex,
  loadLatestSnapshotId,
  loadManifest,
  loadModuleSource,
  loadNativeModuleAnalysis,
  loadNativeModuleBinary,
  loadParsedModulesFromDirectory,
  saveIndex,
} from "../storage/snapshots.js";
import type { SnapshotPlatform } from "../types/snapshot.js";
import {
  DEFAULT_PLATFORM,
  resolveAutoPlatforms,
  resolveDefaultMacosBinaryPath,
} from "../storage/snapshot-utils.js";
import type { ParsedModule } from "../types/module.js";
import { SnapshotCatalog } from "./catalog.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("bootstrap");

async function rebuildIndexFromManifest(platform: SnapshotPlatform, snapshotId: string): Promise<void> {
  log.info(`rebuilding index from manifest: platform=${platform} snapshot=${snapshotId}`);
  const manifest = await loadManifest(platform, snapshotId);
  log.debug(`manifest loaded: ${manifest.modules.length} modules`);
  const READ_BATCH = 200;
  const modules: ParsedModule[] = [];
  for (let i = 0; i < manifest.modules.length; i += READ_BATCH) {
    const loaded = await Promise.all(
      manifest.modules.slice(i, i + READ_BATCH).map(async (module) => ({
        name: module.name,
        dependencies: module.dependencies,
        exports: module.exports,
        body: await loadModuleSource(platform, snapshotId, module.sourcePath),
      }))
    );
    modules.push(...loaded);
  }
  log.debug(`loaded ${modules.length} module sources, running analysis`);
  const analyses = analyzeModules(modules);
  await saveIndex(platform, snapshotId, manifest.revision, analyses);
  log.info(`index rebuilt: ${analyses.length} analyses saved`);
}

export async function loadCatalogById(platform: SnapshotPlatform, snapshotId: string): Promise<SnapshotCatalog> {
  return loadCatalog(platform, snapshotId);
}

async function loadCatalog(platform: SnapshotPlatform, snapshotId: string): Promise<SnapshotCatalog> {
  log.info(`loading catalog: platform=${platform} snapshot=${snapshotId}`);
  const manifest = await loadManifest(platform, snapshotId);
  log.debug(`manifest: revision=${manifest.revision} modules=${manifest.modules.length} natives=${manifest.nativeModules?.length ?? 0}`);
  if (!(await indexExists(platform, snapshotId))) {
    log.info(`index not found, rebuilding from manifest`);
    await rebuildIndexFromManifest(platform, snapshotId);
  }
  const index = await loadIndex(platform, snapshotId);
  log.debug(`index loaded: ${index.analyses.length} module analyses`);
  const hasNatives = (manifest.nativeModules?.length ?? 0) > 0;
  const catalog = new SnapshotCatalog(
    manifest,
    index,
    (relativePath) => loadModuleSource(platform, snapshotId, relativePath),
    hasNatives
      ? (relativePath) => loadNativeModuleBinary(platform, snapshotId, relativePath)
      : undefined,
    hasNatives
      ? (relativePath) => loadNativeModuleAnalysis(platform, snapshotId, relativePath)
      : undefined
  );
  await log.time(`catalog.initialize (${platform}/${snapshotId})`, () =>
    catalog.initialize()
  );
  log.info(`catalog ready: platform=${platform} revision=${manifest.revision} modules=${manifest.modules.length}`);
  return catalog;
}

async function ingestFromDirectory(
  platform: SnapshotPlatform,
  sourceDir: string,
  revisionOverride: string | undefined
): Promise<string> {
  log.info(`ingesting from directory: ${sourceDir} platform=${platform}`);
  const parsedModules = await loadParsedModulesFromDirectory(sourceDir);
  if (parsedModules.length === 0) {
    throw new Error(`No valid modules found in directory: ${sourceDir}`);
  }
  log.info(`parsed ${parsedModules.length} modules from directory`);
  const revision =
    revisionOverride ??
    `dir-${basename(sourceDir)}-${new Date().toISOString().slice(0, 10)}`;
  const manifest = await createSnapshot(platform, revision, parsedModules);
  log.debug(`snapshot created: id=${manifest.snapshotId} revision=${revision}`);
  if (!(await indexExists(platform, manifest.snapshotId))) {
    log.debug(`building index for snapshot ${manifest.snapshotId}`);
    const analyses = analyzeModules(parsedModules);
    await saveIndex(platform, manifest.snapshotId, revision, analyses);
    log.info(`index saved: ${analyses.length} analyses`);
  } else {
    log.debug(`index already exists for snapshot ${manifest.snapshotId}`);
  }
  return manifest.snapshotId;
}

async function ingestFromNativeBinary(
  platform: SnapshotPlatform,
  binaryPath: string
): Promise<string> {
  log.info(`ingesting native Mach-O: platform=${platform} path=${binaryPath}`);
  const ghidraPath = process.env.GHIDRA_INSTALL_DIR;
  const timeoutStr = process.env.WEB_MCP_NATIVE_ANALYSIS_TIMEOUT ?? process.env.WEB_MCP_IOS_ANALYSIS_TIMEOUT;
  const maxCpuStr = process.env.WEB_MCP_NATIVE_MAX_CPU ?? process.env.WEB_MCP_IOS_MAX_CPU;

  log.debug(`ghidra config: path=${ghidraPath ?? "auto"} timeout=${timeoutStr ?? "default"} maxCpu=${maxCpuStr ?? "default"}`);
  const result = await log.time(`native extraction + Ghidra decompilation (${platform})`, () =>
    extractNativeMachOModules(binaryPath, {
      ghidraPath: ghidraPath || undefined,
      analysisTimeoutSec: timeoutStr ? parseInt(timeoutStr, 10) : undefined,
      maxCpu: maxCpuStr ? parseInt(maxCpuStr, 10) : undefined,
    })
  );

  log.info(`native extraction result: platform=${platform} revision=${result.revision} modules=${result.modules.length} functions=${result.functionCount}`);
  const manifest = await createSnapshot(platform, result.revision, result.modules);
  if (!(await indexExists(platform, manifest.snapshotId))) {
    log.debug(`building index for ${platform} snapshot ${manifest.snapshotId}`);
    const analyses = analyzeModules(result.modules);
    await saveIndex(platform, manifest.snapshotId, result.revision, analyses);
    log.info(`${platform} index saved: ${analyses.length} analyses`);
  } else {
    log.debug(`${platform} index already exists for snapshot ${manifest.snapshotId}`);
  }
  return manifest.snapshotId;
}

async function ingestFromWeb(platform: SnapshotPlatform): Promise<string> {
  log.info(`ingesting from web: platform=${platform}`);
  let revision: string;
  try {
    revision = await checkRevision(platform);
    log.info(`live revision: ${revision}`);
  } catch(revisionError: any) {
    log.warn(`unable to check live revision: ${revisionError.message}, trying local fallback`);
    const fallback = await loadLatestSnapshotId(platform);
    if (!fallback) {
      throw new Error(`Unable to check live revision(${revisionError.message}) and no local ${platform} snapshots found.`);
    }
    log.info(`using cached snapshot: ${fallback}`);
    return fallback;
  }

  const existing = await findSnapshotByRevision(platform, revision);
  if (existing) {
    log.info(`snapshot already exists for revision ${revision}: ${existing}`);
    return existing;
  }

  log.info(`no existing snapshot for revision ${revision}, extracting modules`);
  const [liveRevision, parsedModules, nativeModules] = await log.time(
    `extract modules (${platform})`,
    () => extractModules(platform)
  );
  log.info(`extracted: ${parsedModules.length} JS modules, ${nativeModules.length} native modules`);
  const manifest = await createSnapshot(platform, liveRevision, parsedModules, nativeModules);
  const analyses = analyzeModules(parsedModules);
  await saveIndex(platform, manifest.snapshotId, liveRevision, analyses);
  log.info(`snapshot saved: id=${manifest.snapshotId} revision=${liveRevision}`);
  return manifest.snapshotId;
}

async function bootstrapSinglePlatform(
  platform: SnapshotPlatform,
  options: {
    requestedSnapshot?: string;
    sourceDir?: string;
    revisionOverride?: string;
    mode: string;
    iosBinaryPath?: string;
    macosBinaryPath?: string;
  }
): Promise<[SnapshotPlatform, SnapshotCatalog]> {
  const {
    requestedSnapshot,
    sourceDir,
    revisionOverride,
    mode,
    iosBinaryPath,
    macosBinaryPath,
  } = options;

  if (requestedSnapshot) {
    return [platform, await loadCatalog(platform, requestedSnapshot)];
  }

  if (sourceDir) {
    const snapshotId = await ingestFromDirectory(platform, sourceDir, revisionOverride);
    return [platform, await loadCatalog(platform, snapshotId)];
  }

  if (mode === "cached") {
    const latest = await loadLatestSnapshotId(platform);
    if (!latest) throw new Error(`No cached ${platform} snapshots available.`);
    return [platform, await loadCatalog(platform, latest)];
  }

  if (platform === "ios" || platform === "desktop_macos") {
    const binaryPath = platform === "ios" ? iosBinaryPath : macosBinaryPath;
    if (binaryPath) {
      const snapshotId = await ingestFromNativeBinary(platform, binaryPath);
      return [platform, await loadCatalog(platform, snapshotId)];
    }
    const latest = await loadLatestSnapshotId(platform);
    if (!latest) {
      if (platform === "ios") {
        throw new Error(
          "No iOS binary provided and no cached iOS snapshots found.\n" +
            "Set WEB_MCP_IOS_BINARY to a decrypted IPA or Mach-O path, " +
            "or set WEB_MCP_MODE=cached with an existing iOS snapshot."
        );
      }
      throw new Error(
        "No macOS binary found and no cached desktop_macos snapshots available.\n" +
          "Set WEB_MCP_MACOS_BINARY to the .app bundle (e.g. /Applications/WhatsApp.app) " +
          "or its Mach-O executable, or set WEB_MCP_MODE=cached with an existing snapshot."
      );
    }
    return [platform, await loadCatalog(platform, latest)];
  }

  const snapshotId = await ingestFromWeb(platform);
  return [platform, await loadCatalog(platform, snapshotId)];
}

export async function bootstrapCatalogFromEnv(): Promise<Map<SnapshotPlatform, SnapshotCatalog>> {
  const requestedSnapshot = process.env.WEB_MCP_SNAPSHOT_ID;
  const sourceDir = process.env.WEB_MCP_SOURCE_DIR;
  const revisionOverride = process.env.WEB_MCP_REVISION;
  const mode = (process.env.WEB_MCP_MODE ?? "").trim().toLowerCase();
  const rawPlatform = (process.env.WEB_MCP_PLATFORM ?? "").trim().toLowerCase();
  const iosBinaryPath = process.env.WEB_MCP_IOS_BINARY;
  const macosBinaryPath =
    process.env.WEB_MCP_MACOS_BINARY || resolveDefaultMacosBinaryPath();

  const catalogs = new Map<SnapshotPlatform, SnapshotCatalog>();

  const isAuto = rawPlatform === "auto" || rawPlatform === "";
  const platforms: SnapshotPlatform[] = isAuto
    ? resolveAutoPlatforms()
    : [rawPlatform as SnapshotPlatform];

  log.info(`bootstrap env: platform=${rawPlatform || "auto"} resolved=[${platforms.join(", ")}] mode=${mode || "live"} snapshotId=${requestedSnapshot ?? "auto"} sourceDir=${sourceDir ?? "none"} iosBinary=${iosBinaryPath ?? "none"} macosBinary=${macosBinaryPath ?? "none"}`);

  const opts = { requestedSnapshot, sourceDir, revisionOverride, mode, iosBinaryPath, macosBinaryPath };

  for (const platform of platforms) {
    try {
      const [p, catalog] = await bootstrapSinglePlatform(platform, opts);
      catalogs.set(p, catalog);
    } catch (error) {
      if (isAuto && platforms.length > 1) {
        log.warn(`auto-bootstrap: ${platform} failed (${error instanceof Error ? error.message : String(error)}), skipping`);
      } else {
        throw error;
      }
    }
  }

  if (catalogs.size === 0) {
    throw new Error(`No catalogs could be bootstrapped for platforms: [${platforms.join(", ")}]`);
  }

  return catalogs;
}

export async function getSnapshotList(platform?: SnapshotPlatform): Promise<string[]> {
  if (platform) return listSnapshots(platform);
  const all = await listAllSnapshots();
  return all.map((e) => `${e.platform}/${e.snapshotId}`);
}
