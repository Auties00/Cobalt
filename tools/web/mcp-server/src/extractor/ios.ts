import { mkdtemp, rm, stat } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import type { ParsedModule } from "../types/module.js";
import {
  extractAppBundle,
  extractIpa,
  extractBinaryFromPath,
  parseMachOInfo,
} from "./ipa.js";
import { decompileBinary } from "./ghidra.js";
import { parseGhidraOutput } from "./ios-parser.js";
import { readFile } from "node:fs/promises";
import { createLogger } from "../utils/logger.js";

const log = createLogger("native-macho");

export interface NativeMachOExtractionOptions {
  ghidraPath?: string;
  analysisTimeoutSec?: number;
  maxCpu?: number;
}

export interface NativeMachOExtractionResult {
  revision: string;
  modules: ParsedModule[];
  bundleId: string | null;
  bundleVersion: string | null;
  encrypted: boolean;
  functionCount: number;
  frameworks: string[];
}

async function isDirectory(path: string): Promise<boolean> {
  try {
    const info = await stat(path);
    return info.isDirectory();
  } catch {
    return false;
  }
}

export async function extractNativeMachOModules(
  inputPath: string,
  options: NativeMachOExtractionOptions = {}
): Promise<NativeMachOExtractionResult> {
  const lower = inputPath.toLowerCase();
  const isIpa = lower.endsWith(".ipa");
  const isAppBundle = lower.endsWith(".app") || (await isDirectory(inputPath));
  const workDir = await mkdtemp(join(tmpdir(), "native-macho-extract-"));

  try {
    let binaryPath: string;
    let bundleId: string | null = null;
    let bundleVersion: string | null = null;
    let encrypted = false;
    let frameworks: string[] = [];

    if (isIpa) {
      log.info(`extracting IPA: ${inputPath}`);
      const result = await extractIpa(inputPath, workDir);
      binaryPath = result.binaryPath;
      bundleId = result.bundleId;
      bundleVersion = result.bundleVersion;
      encrypted = result.encrypted;
      frameworks = result.frameworks;
      log.info(`executable: ${result.executableName}, bundle: ${bundleId}, version: ${bundleVersion}`);
    } else if (isAppBundle) {
      log.info(`processing .app bundle: ${inputPath}`);
      const result = await extractAppBundle(inputPath);
      binaryPath = result.binaryPath;
      bundleId = result.bundleId;
      bundleVersion = result.bundleVersion;
      encrypted = result.encrypted;
      frameworks = result.frameworks;
      log.info(`executable: ${result.executableName}, bundle: ${bundleId}, version: ${bundleVersion}`);
    } else {
      log.info(`processing binary: ${inputPath}`);
      const buffer = await readFile(inputPath);
      const info = parseMachOInfo(buffer);
      encrypted = info.encrypted;

      if (info.isFat) {
        log.info("fat binary detected, extracting arm64 slice");
        const result = await extractBinaryFromPath(inputPath, workDir);
        binaryPath = result.arm64Path;
      } else {
        binaryPath = inputPath;
      }
    }

    if (encrypted) {
      throw new Error(
        "Binary is FairPlay encrypted. A decrypted binary is required for analysis.\n" +
          "Options:\n" +
          "  - iOS (jailbroken): use frida-ios-dump or flexdecrypt\n" +
          "  - iOS (Apple Silicon Mac): install the iOS app and use the decrypted binary from ~/Library/Containers/\n" +
          "  - macOS Catalyst: Mac App Store builds are not FairPlay-encrypted — pass the .app bundle or the Mach-O directly"
      );
    }

    log.info("running Ghidra decompilation (this may take a while)");
    const ghidraOutput = await decompileBinary(binaryPath, {
      ghidraPath: options.ghidraPath,
      analysisTimeoutSec: options.analysisTimeoutSec,
      maxCpu: options.maxCpu,
    });

    log.info(`Ghidra found ${ghidraOutput.functionCount} functions, parsing`);
    const modules = parseGhidraOutput(ghidraOutput);
    log.info(`parsed ${modules.length} ObjC classes/modules`);

    const revision = bundleVersion ?? `native-${ghidraOutput.functionCount}`;

    return {
      revision,
      modules,
      bundleId,
      bundleVersion,
      encrypted: false,
      functionCount: ghidraOutput.functionCount,
      frameworks,
    };
  } finally {
    await rm(workDir, { recursive: true, force: true }).catch(() => {});
  }
}
