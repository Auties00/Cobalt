export type LogLevel = "debug" | "info" | "warn" | "error" | "silent";

const LEVEL_ORDER: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
  silent: 4,
};

const LEVEL_LABELS: Record<Exclude<LogLevel, "silent">, string> = {
  debug: "DEBUG",
  info: "INFO",
  warn: "WARN",
  error: "ERROR",
};

function parseLogLevel(raw: string | undefined): LogLevel {
  const normalized = (raw ?? "").trim().toLowerCase();
  if (normalized in LEVEL_ORDER) return normalized as LogLevel;
  return "info";
}

function formatTimestamp(): string {
  const now = new Date();
  const h = String(now.getHours()).padStart(2, "0");
  const m = String(now.getMinutes()).padStart(2, "0");
  const s = String(now.getSeconds()).padStart(2, "0");
  const ms = String(now.getMilliseconds()).padStart(3, "0");
  return `${h}:${m}:${s}.${ms}`;
}

function formatArgs(args: unknown[]): string {
  return args
    .map((arg) => {
      if (typeof arg === "string") return arg;
      if (arg instanceof Error) return `${arg.message}\n${arg.stack ?? ""}`;
      try {
        return JSON.stringify(arg, null, 2);
      } catch {
        return String(arg);
      }
    })
    .join(" ");
}

class Logger {
  private readonly component: string;
  private readonly minLevel: number;

  constructor(component: string, level: LogLevel) {
    this.component = component;
    this.minLevel = LEVEL_ORDER[level];
  }

  child(subComponent: string): Logger {
    const fullComponent = this.component
      ? `${this.component}:${subComponent}`
      : subComponent;
    return new Logger(fullComponent, this.currentLevel());
  }

  private currentLevel(): LogLevel {
    for (const [level, order] of Object.entries(LEVEL_ORDER)) {
      if (order === this.minLevel) return level as LogLevel;
    }
    return "info";
  }

  private emit(level: Exclude<LogLevel, "silent">, args: unknown[]): void {
    if (LEVEL_ORDER[level] < this.minLevel) return;
    const ts = formatTimestamp();
    const label = LEVEL_LABELS[level];
    const prefix = this.component ? `[${this.component}]` : "";
    const message = formatArgs(args);

    process.stderr.write(`${ts} ${label} ${prefix} ${message}\n`);
  }

  debug(...args: unknown[]): void {
    this.emit("debug", args);
  }

  info(...args: unknown[]): void {
    this.emit("info", args);
  }

  warn(...args: unknown[]): void {
    this.emit("warn", args);
  }

  error(...args: unknown[]): void {
    this.emit("error", args);
  }

  isDebug(): boolean {
    return this.minLevel <= LEVEL_ORDER.debug;
  }

  async time<T>(label: string, fn: () => Promise<T>): Promise<T> {
    const start = performance.now();
    this.debug(`${label} started`);
    try {
      const result = await fn();
      const elapsed = (performance.now() - start).toFixed(1);
      this.info(`${label} completed in ${elapsed}ms`);
      return result;
    } catch (error) {
      const elapsed = (performance.now() - start).toFixed(1);
      this.error(`${label} failed after ${elapsed}ms`, error);
      throw error;
    }
  }

  timeSync<T>(label: string, fn: () => T): T {
    const start = performance.now();
    this.debug(`${label} started`);
    try {
      const result = fn();
      const elapsed = (performance.now() - start).toFixed(1);
      this.info(`${label} completed in ${elapsed}ms`);
      return result;
    } catch (error) {
      const elapsed = (performance.now() - start).toFixed(1);
      this.error(`${label} failed after ${elapsed}ms`, error);
      throw error;
    }
  }
}

const ROOT_LEVEL = parseLogLevel(process.env.WEB_MCP_LOG_LEVEL);

export function createLogger(component: string): Logger {
  return new Logger(component, ROOT_LEVEL);
}

export const log = createLogger("mcp");
