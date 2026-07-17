import type { ChildProcess } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join as pathJoin } from "node:path";
import {
  DEFAULT_AVD_DEVICE,
  DEFAULT_AVD_SYSTEM_IMAGE,
  DEFAULT_EMULATOR_STATE_DIR,
  WA_PACKAGE_BUSINESS,
  WA_PACKAGE_PERSONAL,
} from "../manager-constants.js";
import { createLogger } from "../../utils/logger.js";
import { ApkInstaller } from "./apk-installer.js";
import { getAndroidSdk } from "./android-sdk.js";
import { WhatsAppRegistrar } from "./whatsapp-registrar.js";
import { WebAdbController } from "../adb/web-adb-controller.js";
import type {
  ApkVariant,
  CreateEmulatorOptions,
  EmulatorRecord,
  EmulatorRegistrationState,
  EmulatorRunState,
  PairingCodeOptions,
  RegisterWhatsAppOptions,
  RegisterWhatsAppResult,
  StartEmulatorOptions,
} from "../../types/live/emulator.js";

const log = createLogger("live:emulator");

function nowIso(): string {
  return new Date().toISOString();
}

function sanitizeEmulatorName(name: string): string {
  if (!/^[A-Za-z0-9_-]{1,40}$/.test(name)) {
    throw new Error(
      `Invalid emulator name "${name}". Use 1-40 chars of [A-Za-z0-9_-].`
    );
  }
  return name;
}

export class Emulator {
  readonly name: string;
  private record: EmulatorRecord;
  private process: ChildProcess | null = null;
  private readonly adb: WebAdbController;
  private readonly installer: ApkInstaller;
  private readonly registrar: WhatsAppRegistrar;
  private readonly stateFile: string;

  constructor(record: EmulatorRecord) {
    this.name = record.name;
    this.record = record;
    this.adb = new WebAdbController();
    this.installer = new ApkInstaller();
    this.registrar = new WhatsAppRegistrar(this.adb);
    this.stateFile = pathJoin(DEFAULT_EMULATOR_STATE_DIR, `${record.name}.json`);
  }

  static forNewAvd(options: CreateEmulatorOptions): Emulator {
    const name = sanitizeEmulatorName(options.name);
    const avdName = `cobalt_${name}`;
    const record: EmulatorRecord = {
      name,
      avdName,
      deviceProfile: options.deviceProfile ?? DEFAULT_AVD_DEVICE,
      systemImage: options.systemImage ?? DEFAULT_AVD_SYSTEM_IMAGE,
      apkVariant: options.apkVariant ?? "personal",
      runState: "stopped",
      adbSerial: null,
      accountPhone: null,
      accountType: options.apkVariant === "business" ? "business" : "personal",
      registrationState: "unregistered",
      createdAt: nowIso(),
      updatedAt: nowIso(),
      lastBootedAt: null,
    };
    return new Emulator(record);
  }

  static fromRecord(record: EmulatorRecord): Emulator {
    return new Emulator(record);
  }

  getRecord(): EmulatorRecord {
    return { ...this.record };
  }

  private updateState(partial: Partial<EmulatorRecord>): void {
    this.record = { ...this.record, ...partial, updatedAt: nowIso() };
  }

  persist(): void {
    if (!existsSync(DEFAULT_EMULATOR_STATE_DIR)) {
      mkdirSync(DEFAULT_EMULATOR_STATE_DIR, { recursive: true });
    }
    writeFileSync(this.stateFile, JSON.stringify(this.record, null, 2), "utf-8");
  }

  async provision(): Promise<void> {
    const sdk = getAndroidSdk();
    if (!sdk.isReady()) {
      throw new Error(
        "Android SDK is not set up. Set ANDROID_HOME and install platform-tools, emulator, cmdline-tools, and a system-images package."
      );
    }
    const existing = await sdk.listAvds();
    if (existing.includes(this.record.avdName)) {
      log.info(`provision: AVD ${this.record.avdName} already exists; skipping create.`);
      this.persist();
      return;
    }
    await sdk.createAvd({
      name: this.record.avdName,
      deviceProfile: this.record.deviceProfile,
      systemImage: this.record.systemImage,
    });
    this.persist();
  }

  async start(options: StartEmulatorOptions): Promise<EmulatorRecord> {
    if (this.record.runState === "running" && this.record.adbSerial) {
      log.info(`start: ${this.name} already running at ${this.record.adbSerial}`);
      return this.getRecord();
    }
    const sdk = getAndroidSdk();
    const port = await sdk.findFreeEmulatorPort();
    const child = sdk.spawnEmulator({
      avdName: this.record.avdName,
      port,
      headless: options.headless ?? true,
    });
    this.process = child;
    this.updateState({ runState: "starting" });
    this.persist();

    child.on("exit", (code) => {
      log.info(`emulator process ${this.record.avdName} exited code=${code}`);
      this.process = null;
      this.updateState({ runState: "stopped", adbSerial: null });
      this.persist();
    });

    const serial = await sdk.waitForAdbSerial(port);
    await sdk.waitForBoot(serial);

    this.updateState({
      runState: "running",
      adbSerial: serial,
      lastBootedAt: nowIso(),
    });
    this.persist();
    log.info(`start: ${this.name} ready at ${serial}`);
    return this.getRecord();
  }

  async stop(): Promise<void> {
    const sdk = getAndroidSdk();
    const serial = this.record.adbSerial;
    if (serial) {
      this.updateState({ runState: "stopping" });
      this.persist();
      await sdk.killEmulator(serial);
    }
    if (this.process && !this.process.killed) {
      try {
        this.process.kill();
      } catch {

      }
    }
    this.process = null;
    this.updateState({ runState: "stopped", adbSerial: null });
    this.persist();
  }

  async destroy(): Promise<void> {
    if (this.record.runState !== "stopped") {
      await this.stop();
    }
    const sdk = getAndroidSdk();
    try {
      await sdk.deleteAvd(this.record.avdName);
    } catch (error) {
      log.warn(
        `destroy: deleteAvd failed for ${this.record.avdName}: ${error instanceof Error ? error.message : String(error)}`
      );
    }
    try {
      const { rmSync } = await import("node:fs");
      if (existsSync(this.stateFile)) rmSync(this.stateFile);
    } catch {

    }
  }

  async installApk(variant?: ApkVariant): Promise<void> {
    const serial = this.requireSerial();
    const target = variant ?? this.record.apkVariant;
    await this.installer.installOnEmulator(serial, target);
    this.updateState({
      apkVariant: target,
      accountType: target === "business" ? "business" : "personal",

      registrationState:
        target !== this.record.apkVariant ? "unregistered" : this.record.registrationState,
      accountPhone: target !== this.record.apkVariant ? null : this.record.accountPhone,
    });
    this.persist();
  }

  async registerWhatsApp(options: RegisterWhatsAppOptions): Promise<RegisterWhatsAppResult> {
    const serial = this.requireSerial();
    if (this.record.registrationState === "registered" && !options.force) {
      throw new Error(
        `Emulator "${this.name}" is already registered; pass force:true to re-register.`
      );
    }
    this.updateState({ registrationState: "registering" });
    this.persist();

    try {
      const result = await this.registrar.register({
        serial,
        variant: this.record.apkVariant,
        phone: options.phone,
        countryCode: options.countryCode,
      });
      if (result.success) {

        this.updateState({
          registrationState: "registered" as EmulatorRegistrationState,
          accountPhone: result.accountPhone ?? this.record.accountPhone,
        });
      } else {

        this.updateState({
          registrationState: "invalidated",
          accountPhone: this.record.accountPhone,
        });
      }
      this.persist();
      return result;
    } catch (error) {
      this.updateState({ registrationState: "invalidated" });
      this.persist();
      throw error;
    }
  }

  async enterPairingCode(options: PairingCodeOptions): Promise<unknown> {
    const code = (options.code ?? "").replace(/[\s-]+/g, "");
    if (!code) {
      throw new Error("Pairing code must be a non-empty string.");
    }
    const serial = this.requireSerial();
    return this.adb.linkByPhoneCode(code, {
      serial,
      packageName:
        options.packageName ??
        (this.record.apkVariant === "business" ? WA_PACKAGE_BUSINESS : WA_PACKAGE_PERSONAL),
      stepTimeoutMs: options.stepTimeoutMs,
    });
  }

  private requireSerial(): string {
    if (!this.record.adbSerial) {
      throw new Error(
        `Emulator "${this.name}" is not running. Call web_live_emulator_start first.`
      );
    }
    return this.record.adbSerial;
  }
}

export function loadEmulatorRecord(path: string): EmulatorRecord | null {
  try {
    const raw = readFileSync(path, "utf-8");
    const parsed = JSON.parse(raw) as EmulatorRecord;
    return {
      ...parsed,
      runState: "stopped",
      adbSerial: null,
    };
  } catch {
    return null;
  }
}
