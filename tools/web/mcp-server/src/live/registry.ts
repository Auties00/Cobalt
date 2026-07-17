import { randomUUID } from "node:crypto";
import { createLogger } from "../utils/logger.js";
import { MAX_CONCURRENT_SESSIONS } from "./manager-constants.js";
import { LiveWebSession } from "./session.js";
import type {
  LiveSessionInfo,
  SessionMode,
  StartSessionOptions,
} from "../types/live/session.js";

const log = createLogger("live:registry");

export class SessionNotFoundError extends Error {
  constructor(sessionId: string, available: string[]) {
    const hint = available.length === 0
      ? "no sessions are running"
      : `available: [${available.join(", ")}]`;
    super(`Live session "${sessionId}" not found; ${hint}.`);
    this.name = "SessionNotFoundError";
  }
}

export class SessionLimitReachedError extends Error {
  constructor(limit: number) {
    super(`Concurrent session limit (${limit}) reached. Stop an existing session before starting a new one.`);
    this.name = "SessionLimitReachedError";
  }
}

export class LiveWebSessionRegistry {
  private readonly sessions: Map<string, LiveWebSession> = new Map();
  private readonly cap: number;

  constructor(cap: number = MAX_CONCURRENT_SESSIONS) {
    this.cap = cap;
  }

  requireSession(sessionId: string): LiveWebSession {
    const session = this.sessions.get(sessionId);
    if (!session) {
      throw new SessionNotFoundError(sessionId, [...this.sessions.keys()]);
    }
    return session;
  }

  getSession(sessionId: string): LiveWebSession | undefined {
    return this.sessions.get(sessionId);
  }

  hasSession(sessionId: string): boolean {
    return this.sessions.has(sessionId);
  }

  listSessionIds(): string[] {
    return [...this.sessions.keys()];
  }

  async listSessions(snapshotRevision?: string | null): Promise<LiveSessionInfo[]> {
    const out: LiveSessionInfo[] = [];
    for (const session of this.sessions.values()) {
      out.push(await session.info(snapshotRevision));
    }
    return out;
  }

  async startSession(options: StartSessionOptions = {}): Promise<LiveSessionInfo> {
    const requestedMode: SessionMode = options.mode ?? "web";
    const sessionId = options.sessionId ?? randomUUID();
    const existing = this.sessions.get(sessionId);
    if (existing) {
      if (existing.getMode() !== requestedMode) {
        throw new Error(
          `Session "${sessionId}" already exists with mode "${existing.getMode()}"; cannot refresh with mode "${requestedMode}". Stop it first.`
        );
      }
      log.info(`startSession: refreshing existing session "${sessionId}" (mode=${requestedMode})`);
      return existing.start(options);
    }

    if (this.sessions.size >= this.cap) {
      throw new SessionLimitReachedError(this.cap);
    }

    const session = new LiveWebSession(sessionId, requestedMode, {
      ephemeral: options.ephemeral,
    });
    this.sessions.set(sessionId, session);
    log.info(`startSession: created "${sessionId}" (mode=${requestedMode} ephemeral=${!!options.ephemeral}) — ${this.sessions.size}/${this.cap} sessions`);

    try {
      return await session.start(options);
    } catch (error) {

      this.sessions.delete(sessionId);
      throw error;
    }
  }

  async stopSession(sessionId: string): Promise<void> {
    const session = this.sessions.get(sessionId);
    if (!session) return;
    await session.stop();
    this.sessions.delete(sessionId);
    log.info(`stopSession: removed "${sessionId}" — ${this.sessions.size}/${this.cap} sessions`);
  }

  async stopAllSessions(): Promise<void> {
    log.info(`stopAllSessions: stopping ${this.sessions.size} session(s)`);
    const ids = [...this.sessions.keys()];
    await Promise.all(ids.map((id) => this.stopSession(id)));
  }

  async deleteSession(sessionId: string): Promise<void> {
    const session = this.sessions.get(sessionId);
    if (!session) return;
    await session.stop();
    await session.wipeUserDataDir();
    this.sessions.delete(sessionId);
    log.info(`deleteSession: wiped "${sessionId}"`);
  }
}

let registry: LiveWebSessionRegistry | null = null;

export function getLiveWebSessionRegistry(): LiveWebSessionRegistry {
  if (!registry) {
    registry = new LiveWebSessionRegistry();
  }
  return registry;
}
