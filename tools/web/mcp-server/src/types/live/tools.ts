import type { LiveWebSession } from "../../live/session.js";
import type { LiveWebSessionRegistry } from "../../live/registry.js";
import type { Emulator } from "../../live/emulator/emulator.js";
import type { EmulatorRegistry } from "../../live/emulator/registry.js";

export interface LiveToolsContext {
  registry: LiveWebSessionRegistry;
  emulatorRegistry: EmulatorRegistry;
  activeSnapshotRevision: () => string | null;
  requireReady: () => void;
  requireSession: (sessionId: string) => LiveWebSession;
  requireEmulator: (name: string) => Emulator;
}
