import { existsSync, mkdirSync, readdirSync } from "node:fs";
import { join as pathJoin } from "node:path";
import { createLogger } from "../../utils/logger.js";
import {
  DEFAULT_EMULATOR_STATE_DIR,
  MAX_CONCURRENT_EMULATORS,
} from "../manager-constants.js";
import { Emulator, loadEmulatorRecord } from "./emulator.js";
import type { CreateEmulatorOptions, EmulatorRecord } from "../../types/live/emulator.js";

const log = createLogger("live:emulator:registry");

export class EmulatorNotFoundError extends Error {
  constructor(name: string, available: string[]) {
    const hint = available.length === 0
      ? "no emulators are registered"
      : `available: [${available.join(", ")}]`;
    super(`Emulator "${name}" not found; ${hint}.`);
    this.name = "EmulatorNotFoundError";
  }
}
export class EmulatorLimitReachedError extends Error {
  constructor(limit: number) {
    super(`Concurrent emulator limit (${limit}) reached. Stop a running emulator before starting another.`);
    this.name = "EmulatorLimitReachedError";
  }
}

export class EmulatorRegistry {
  private readonly emulators: Map<string, Emulator> = new Map();
  private readonly runCap: number;

  constructor(runCap: number = MAX_CONCURRENT_EMULATORS) {
    this.runCap = runCap;
    this.loadPersistedEmulators();
  }

  private loadPersistedEmulators(): void {
    if (!existsSync(DEFAULT_EMULATOR_STATE_DIR)) {
      mkdirSync(DEFAULT_EMULATOR_STATE_DIR, { recursive: true });
      return;
    }
    const entries = readdirSync(DEFAULT_EMULATOR_STATE_DIR);
    for (const entry of entries) {
      if (!entry.endsWith(".json")) continue;
      const path = pathJoin(DEFAULT_EMULATOR_STATE_DIR, entry);
      const record = loadEmulatorRecord(path);
      if (!record) continue;
      this.emulators.set(record.name, Emulator.fromRecord(record));
    }
    log.info(`loadPersistedEmulators: loaded ${this.emulators.size} record(s)`);
  }

  requireEmulator(name: string): Emulator {
    const emu = this.emulators.get(name);
    if (!emu) throw new EmulatorNotFoundError(name, [...this.emulators.keys()]);
    return emu;
  }

  hasEmulator(name: string): boolean {
    return this.emulators.has(name);
  }

  listRecords(): EmulatorRecord[] {
    return [...this.emulators.values()].map((e) => e.getRecord());
  }

  private runningCount(): number {
    return [...this.emulators.values()].filter((e) => e.getRecord().runState !== "stopped").length;
  }

  async createEmulator(options: CreateEmulatorOptions): Promise<EmulatorRecord> {
    if (this.emulators.has(options.name)) {
      throw new Error(`Emulator "${options.name}" already exists.`);
    }
    const emulator = Emulator.forNewAvd(options);
    await emulator.provision();
    this.emulators.set(options.name, emulator);
    log.info(`createEmulator: "${options.name}" provisioned`);
    return emulator.getRecord();
  }

  async startEmulator(name: string, options: { headless?: boolean; bootTimeoutMs?: number } = {}): Promise<EmulatorRecord> {
    const emulator = this.requireEmulator(name);
    const record = emulator.getRecord();
    if (record.runState !== "stopped") {
      return emulator.getRecord();
    }
    if (this.runningCount() >= this.runCap) {
      throw new EmulatorLimitReachedError(this.runCap);
    }
    return emulator.start({ name, ...options });
  }

  async stopEmulator(name: string): Promise<void> {
    const emulator = this.emulators.get(name);
    if (!emulator) return;
    await emulator.stop();
  }

  async stopAll(): Promise<void> {
    log.info(`stopAll: stopping ${this.runningCount()} running emulator(s)`);
    const records = this.listRecords();
    for (const record of records) {
      if (record.runState !== "stopped") {
        await this.stopEmulator(record.name);
      }
    }
  }

  async deleteEmulator(name: string): Promise<void> {
    const emulator = this.emulators.get(name);
    if (!emulator) return;
    await emulator.destroy();
    this.emulators.delete(name);
    log.info(`deleteEmulator: removed "${name}"`);
  }
}

let registry: EmulatorRegistry | null = null;

export function getEmulatorRegistry(): EmulatorRegistry {
  if (!registry) registry = new EmulatorRegistry();
  return registry;
}
