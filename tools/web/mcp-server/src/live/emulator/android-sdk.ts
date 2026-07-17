import type { ChildProcess } from "node:child_process";
import spawn from "cross-spawn";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join as pathJoin, resolve as pathResolve } from "node:path";
import { ANDROID_SDK_ROOT } from "../manager-constants.js";
import { createLogger } from "../../utils/logger.js";
import { sleep } from "../../utils/async.js";
import type { SystemImage } from "../../types/live/emulator.js";

const AVD_DEFAULT_RAM_MB = 4096;
const AVD_DEFAULT_VM_HEAP_MB = 576;

const log = createLogger("live:emulator:sdk");

const IS_WINDOWS = process.platform === "win32";
const EXEC_SUFFIX = IS_WINDOWS ? ".exe" : "";
const BAT_SUFFIX = IS_WINDOWS ? ".bat" : "";

interface ExecOut {
  stdout: string;
  stderr: string;
}

export interface ResolvedSdkTools {
  sdkRoot: string | null;
  adb: string | null;
  emulator: string | null;
  avdmanager: string | null;
  sdkmanager: string | null;
}

export class AndroidSdk {
  private readonly tools: ResolvedSdkTools;

  constructor(sdkRoot: string | null = ANDROID_SDK_ROOT) {
    this.tools = AndroidSdk.resolveTools(sdkRoot);
    log.info(
      `sdkRoot=${this.tools.sdkRoot ?? "(none)"} adb=${!!this.tools.adb} emulator=${!!this.tools.emulator} avdmanager=${!!this.tools.avdmanager} sdkmanager=${!!this.tools.sdkmanager}`
    );
  }

  static resolveTools(sdkRoot: string | null): ResolvedSdkTools {
    if (!sdkRoot) {
      return { sdkRoot: null, adb: null, emulator: null, avdmanager: null, sdkmanager: null };
    }
    const root = pathResolve(sdkRoot);
    const platformTools = pathJoin(root, "platform-tools");
    const emulatorDir = pathJoin(root, "emulator");
    const cmdlineTools = pathJoin(root, "cmdline-tools", "latest", "bin");
    const legacyTools = pathJoin(root, "tools", "bin");

    const adb = AndroidSdk.firstExisting([
      pathJoin(platformTools, `adb${EXEC_SUFFIX}`),
    ]);
    const emulator = AndroidSdk.firstExisting([
      pathJoin(emulatorDir, `emulator${EXEC_SUFFIX}`),
    ]);
    const avdmanager = AndroidSdk.firstExisting([
      pathJoin(cmdlineTools, `avdmanager${BAT_SUFFIX}`),
      pathJoin(legacyTools, `avdmanager${BAT_SUFFIX}`),
    ]);
    const sdkmanager = AndroidSdk.firstExisting([
      pathJoin(cmdlineTools, `sdkmanager${BAT_SUFFIX}`),
      pathJoin(legacyTools, `sdkmanager${BAT_SUFFIX}`),
    ]);

    return { sdkRoot: root, adb, emulator, avdmanager, sdkmanager };
  }

  private static firstExisting(candidates: string[]): string | null {
    for (const candidate of candidates) {
      if (existsSync(candidate)) return candidate;
    }
    return null;
  }

  private require(tool: keyof ResolvedSdkTools, hint: string): string {
    const value = this.tools[tool] as string | null;
    if (!value) {
      const rootHint = this.tools.sdkRoot
        ? ` (searched under ${this.tools.sdkRoot})`
        : " (ANDROID_HOME/ANDROID_SDK_ROOT is not set)";
      throw new Error(`${tool} not found${rootHint}. ${hint}`);
    }
    return value;
  }

  private exec(
    command: string,
    args: string[],
    opts: { timeoutMs?: number; env?: Record<string, string> } = {}
  ): Promise<ExecOut> {
    const timeoutMs = opts.timeoutMs ?? 180_000;
    const maxBuffer = 16 * 1024 * 1024;
    return new Promise((resolve, reject) => {
      const child = spawn(command, args, {
        env: { ...process.env, ...(opts.env ?? {}) },
      });
      let stdout = "";
      let stderr = "";
      let aborted = false;
      const abort = (reason: string) => {
        if (aborted) return;
        aborted = true;
        child.kill();
        reject(new Error(`${command} ${args.join(" ")} failed: ${reason}\n${stdout}\n${stderr}`.trim()));
      };
      const timer = setTimeout(() => abort(`timed out after ${timeoutMs}ms`), timeoutMs);
      child.stdout?.on("data", (chunk) => {
        if (stdout.length + chunk.length > maxBuffer) return abort("stdout exceeded maxBuffer");
        stdout += chunk.toString();
      });
      child.stderr?.on("data", (chunk) => {
        if (stderr.length + chunk.length > maxBuffer) return abort("stderr exceeded maxBuffer");
        stderr += chunk.toString();
      });
      child.on("error", (err) => {
        clearTimeout(timer);
        if (!aborted) reject(err);
      });
      child.on("close", (code) => {
        clearTimeout(timer);
        if (aborted) return;
        if (code === 0) {
          resolve({ stdout, stderr });
        } else {
          reject(new Error(`${command} ${args.join(" ")} exit ${code}\n${stdout}\n${stderr}`.trim()));
        }
      });
    });
  }

  isReady(): boolean {
    return !!(this.tools.sdkRoot && this.tools.adb && this.tools.emulator && this.tools.avdmanager);
  }

  getTools(): ResolvedSdkTools {
    return { ...this.tools };
  }

  async listAvds(): Promise<string[]> {
    const avdmanager = this.require(
      "avdmanager",
      "Install cmdline-tools via Android Studio or `sdkmanager --install \"cmdline-tools;latest\"`."
    );
    const { stdout } = await this.exec(avdmanager, ["list", "avd", "-c"]);
    return stdout
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0);
  }

  async createAvd(options: {
    name: string;
    deviceProfile: string;
    systemImage: string;
  }): Promise<void> {
    const avdmanager = this.require(
      "avdmanager",
      "Install cmdline-tools via Android Studio or `sdkmanager --install \"cmdline-tools;latest\"`."
    );
    log.info(`createAvd: ${options.name} device=${options.deviceProfile} image=${options.systemImage}`);
    await this.execWithInput(
      avdmanager,
      [
        "create",
        "avd",
        "--name",
        options.name,
        "--package",
        options.systemImage,
        "--device",
        options.deviceProfile,
        "--force",
      ],
      "no\n"
    );
    this.bumpAvdMemory(options.name);
  }

  private bumpAvdMemory(name: string): void {
    const configPath = pathJoin(homedir(), ".android", "avd", `${name}.avd`, "config.ini");
    if (!existsSync(configPath)) {
      log.warn(`bumpAvdMemory: config.ini not found at ${configPath}`);
      return;
    }
    const original = readFileSync(configPath, "utf-8");
    const lines = original.split(/\r?\n/);
    let ramSet = false;
    let heapSet = false;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("hw.ramSize")) {
        lines[i] = `hw.ramSize = ${AVD_DEFAULT_RAM_MB}M`;
        ramSet = true;
      } else if (lines[i].startsWith("vm.heapSize")) {
        lines[i] = `vm.heapSize = ${AVD_DEFAULT_VM_HEAP_MB}M`;
        heapSet = true;
      }
    }
    if (!ramSet) lines.push(`hw.ramSize = ${AVD_DEFAULT_RAM_MB}M`);
    if (!heapSet) lines.push(`vm.heapSize = ${AVD_DEFAULT_VM_HEAP_MB}M`);
    writeFileSync(configPath, lines.join("\n"), "utf-8");
    log.info(`bumpAvdMemory: ${name} → ${AVD_DEFAULT_RAM_MB}M RAM, ${AVD_DEFAULT_VM_HEAP_MB}M heap`);
  }

  async deleteAvd(name: string): Promise<void> {
    const avdmanager = this.require(
      "avdmanager",
      "Install cmdline-tools via Android Studio or `sdkmanager --install \"cmdline-tools;latest\"`."
    );
    log.info(`deleteAvd: ${name}`);
    await this.exec(avdmanager, ["delete", "avd", "--name", name]);
  }

  async listSystemImages(): Promise<SystemImage[]> {
    const sdkmanager = this.require(
      "sdkmanager",
      "Install cmdline-tools via Android Studio or `sdkmanager --install \"cmdline-tools;latest\"`."
    );
    const { stdout } = await this.exec(sdkmanager, ["--list"]);
    const lines = stdout.split(/\r?\n/);
    const out: SystemImage[] = [];
    let inInstalled = false;
    const installedIds = new Set<string>();
    for (const raw of lines) {
      const line = raw.trim();
      if (line.startsWith("Installed packages")) {
        inInstalled = true;
        continue;
      }
      if (line.startsWith("Available Packages")) {
        inInstalled = false;
        continue;
      }
      if (!line.startsWith("system-images;")) continue;
      const id = line.split(/\s+/)[0];
      if (inInstalled) {
        installedIds.add(id);
      }
    }

    const seen = new Set<string>();
    for (const raw of lines) {
      const line = raw.trim();
      if (!line.startsWith("system-images;")) continue;
      const id = line.split(/\s+/)[0];
      if (seen.has(id)) continue;
      seen.add(id);
      const parts = id.split(";");

      const apiLevelRaw = parts[1]?.replace(/^android-/, "");
      const apiLevel = apiLevelRaw ? parseInt(apiLevelRaw, 10) : null;
      out.push({
        id,
        installed: installedIds.has(id),
        apiLevel: Number.isFinite(apiLevel) ? apiLevel : null,
        tag: parts[2] ?? null,
        abi: parts[3] ?? null,
      });
    }
    return out;
  }

  spawnEmulator(options: {
    avdName: string;
    port: number;
    headless: boolean;
  }): ChildProcess {
    const emulator = this.require(
      "emulator",
      "Install the emulator package via `sdkmanager --install emulator` or Android Studio."
    );
    const args = [
      "-avd",
      options.avdName,
      "-port",
      String(options.port),
      "-no-snapshot-save",
      "-no-boot-anim",
    ];
    if (options.headless) args.push("-no-window", "-no-audio");
    log.info(`spawnEmulator: ${options.avdName} port=${options.port} headless=${options.headless}`);
    const child = spawn(emulator, args, {
      detached: false,
      stdio: "ignore",
      env: { ...process.env, ANDROID_SDK_ROOT: this.tools.sdkRoot ?? "" },
    });
    return child;
  }

  async waitForBoot(serial: string): Promise<void> {
    const adb = this.require(
      "adb",
      "Install platform-tools via Android Studio or `sdkmanager --install platform-tools`."
    );
    log.info(`waitForBoot: ${serial}`);
    while (true) {
      try {
        const { stdout } = await this.exec(adb, [
          "-s",
          serial,
          "shell",
          "getprop",
          "sys.boot_completed",
        ]);
        if (stdout.trim() === "1") {
          log.info(`waitForBoot: ${serial} ready`);
          return;
        }
      } catch {

      }
      await sleep(2_000);
    }
  }

  async waitForAdbSerial(port: number): Promise<string> {
    const adb = this.require(
      "adb",
      "Install platform-tools via Android Studio or `sdkmanager --install platform-tools`."
    );
    const expected = `emulator-${port}`;
    while (true) {
      try {
        const { stdout } = await this.exec(adb, ["devices"]);
        if (stdout.includes(expected)) {
          return expected;
        }
      } catch {

      }
      await sleep(1_000);
    }
  }

  async killEmulator(serial: string): Promise<void> {
    const adb = this.require(
      "adb",
      "Install platform-tools via Android Studio or `sdkmanager --install platform-tools`."
    );
    log.info(`killEmulator: ${serial}`);
    try {
      await this.exec(adb, ["-s", serial, "emu", "kill"], { timeoutMs: 15_000 });
    } catch (error) {
      log.warn(
        `killEmulator ${serial} failed: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  async findFreeEmulatorPort(): Promise<number> {
    const adb = this.require(
      "adb",
      "Install platform-tools via Android Studio or `sdkmanager --install platform-tools`."
    );
    const { stdout } = await this.exec(adb, ["devices"]);
    const used = new Set<number>();
    for (const line of stdout.split(/\r?\n/)) {
      const match = line.match(/^emulator-(\d+)\b/);
      if (match) used.add(parseInt(match[1], 10));
    }
    for (let port = 5554; port <= 5682; port += 2) {
      if (!used.has(port)) return port;
    }
    throw new Error("No free emulator port between 5554-5682.");
  }

  private execWithInput(
    command: string,
    args: string[],
    stdinInput: string
  ): Promise<ExecOut> {
    return new Promise((resolve, reject) => {
      const child = spawn(command, args, {
        stdio: ["pipe", "pipe", "pipe"],
        env: { ...process.env },
      });
      let stdout = "";
      let stderr = "";
      child.stdout?.on("data", (chunk) => {
        stdout += chunk.toString();
      });
      child.stderr?.on("data", (chunk) => {
        stderr += chunk.toString();
      });
      child.on("error", reject);
      child.on("close", (code) => {
        if (code === 0) {
          resolve({ stdout, stderr });
        } else {
          reject(
            new Error(
              `${command} ${args.join(" ")} exit ${code}\n${stdout}\n${stderr}`.trim()
            )
          );
        }
      });
      child.stdin?.write(stdinInput);
      child.stdin?.end();
    });
  }
}

let sdkCache: AndroidSdk | null = null;

export function getAndroidSdk(): AndroidSdk {
  if (!sdkCache) sdkCache = new AndroidSdk();
  return sdkCache;
}
