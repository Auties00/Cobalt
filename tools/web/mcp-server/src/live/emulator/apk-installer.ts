import {
  createWriteStream,
  existsSync,
  mkdirSync,
  readdirSync,
  renameSync,
  rmSync,
} from "node:fs";
import { join as pathJoin } from "node:path";
import { pipeline } from "node:stream/promises";
import { execFile, type ExecFileException } from "node:child_process";
import {
  DEFAULT_APK_CACHE_DIR,
  WA_PACKAGE_BUSINESS,
  WA_PACKAGE_PERSONAL,
} from "../manager-constants.js";
import { createLogger } from "../../utils/logger.js";
import { downloadApk, latestVersion } from "../../utils/play-store.js";
import { getAndroidSdk } from "./android-sdk.js";
import type { ApkVariant } from "../../types/live/emulator.js";

const log = createLogger("live:emulator:apk");

export class ApkInstaller {
  constructor(private readonly cacheDir: string = DEFAULT_APK_CACHE_DIR) {
    if (!existsSync(this.cacheDir)) {
      mkdirSync(this.cacheDir, { recursive: true });
    }
  }

  async ensureApk(variant: ApkVariant): Promise<string[]> {
    const packageName = packageFor(variant);
    const version = await latestVersion(packageName);
    const dir = pathJoin(this.cacheDir, `${packageName}-${version.code}`);
    if (existsSync(dir)) {
      const cached = readdirSync(dir)
        .filter((f) => f.endsWith(".apk"))
        .map((f) => pathJoin(dir, f));
      if (cached.length > 0) {
        log.debug(`ensureApk: cache hit ${dir} (${cached.length} apks)`);
        return cached;
      }
    }
    log.info(`ensureApk: downloading ${packageName} v${version.name} (${version.code})`);
    const partial = `${dir}.partial`;
    rmSync(partial, { recursive: true, force: true });
    mkdirSync(partial, { recursive: true });
    const download = await downloadApk(packageName, version.code);
    try {
      const names: string[] = ["base.apk"];
      await pipeline(download.baseApk, createWriteStream(pathJoin(partial, "base.apk")));
      for (const [splitName, stream] of download.splits) {
        const fileName = `${sanitize(splitName)}.apk`;
        await pipeline(stream, createWriteStream(pathJoin(partial, fileName)));
        names.push(fileName);
      }
      rmSync(dir, { recursive: true, force: true });
      renameSync(partial, dir);
      log.info(`ensureApk: cached ${names.length} apks to ${dir}`);
      return names.map((n) => pathJoin(dir, n));
    } catch (err) {
      rmSync(partial, { recursive: true, force: true });
      throw err;
    } finally {
      download.close();
    }
  }

  async installOnEmulator(serial: string, variant: ApkVariant): Promise<void> {
    const adb = getAndroidSdk().getTools().adb;
    if (!adb) {
      throw new Error("adb not found; install platform-tools.");
    }
    const apks = await this.ensureApk(variant);
    const subcommand = apks.length === 1 ? "install" : "install-multiple";
    log.info(`installOnEmulator: serial=${serial} apks=${apks.length} cmd=${subcommand}`);
    await this.runCmd(adb, ["-s", serial, subcommand, "-r", "-t", ...apks], 240_000);
    log.info(`installOnEmulator: serial=${serial} done`);
  }

  private runCmd(command: string, args: string[], timeoutMs: number): Promise<string> {
    return new Promise((resolve, reject) => {
      execFile(
        command,
        args,
        { timeout: timeoutMs, maxBuffer: 16 * 1024 * 1024 },
        (error: ExecFileException | null, stdout: string, stderr: string) => {
          if (error) {
            reject(
              new Error(
                `${command} ${args.join(" ")} failed: ${error.message}\n${stdout}\n${stderr}`.trim()
              )
            );
            return;
          }
          resolve(stdout ?? "");
        }
      );
    });
  }
}

function packageFor(variant: ApkVariant): string {
  return variant === "business" ? WA_PACKAGE_BUSINESS : WA_PACKAGE_PERSONAL;
}

function sanitize(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, "_");
}
