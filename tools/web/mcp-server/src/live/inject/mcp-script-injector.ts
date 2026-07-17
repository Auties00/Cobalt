import { loadAutoInjectScripts } from "./script-sources.js";
import { createLogger } from "../../utils/logger.js";
import { sleep } from "../../utils/async.js";

const log = createLogger("live:injector");

export type EvaluateFn = <R = unknown>(
  pageFunction: string | ((...args: any[]) => R | Promise<R>),
  ...args: any[]
) => Promise<R>;

const SCRIPT_NAMES = ["abProps", "stanzaLogger", "wamLogger"] as const;

export class McpScriptInjector {
  private readonly getEvaluate: () => EvaluateFn;

  constructor(getEvaluate: () => EvaluateFn) {
    this.getEvaluate = getEvaluate;
  }

  async ensureInjected(): Promise<void> {
    if (await this.areAllReady()) {
      log.debug("all MCP scripts already injected");
      return;
    }

    log.info("injecting MCP scripts (abProps, stanzaLogger, wamLogger)");
    const scripts = loadAutoInjectScripts();
    for (let attempt = 0; attempt < 3; attempt += 1) {
      const readiness = await this.getScriptReadiness();
      const notReady = SCRIPT_NAMES.filter((_, i) => !readiness[i]);
      log.debug(`injection attempt ${attempt + 1}/3: pending=[${notReady.join(", ")}]`);
      for (let i = 0; i < scripts.length; i += 1) {
        if (!readiness[i]) {
          log.debug(`injecting ${SCRIPT_NAMES[i]}`);
          await this.injectScriptSource(scripts[i]);
        }
      }
      for (let poll = 0; poll < 8; poll += 1) {
        if (await this.areAllReady()) {
          log.info("all MCP scripts injected successfully");
          return;
        }
        await sleep(250);
      }
    }

    const finalReadiness = await this.getScriptReadiness();
    const failed = SCRIPT_NAMES.filter((_, i) => !finalReadiness[i]);
    log.error(`script injection failed: ${failed.join(", ")} did not initialize`);
    throw new Error(
      `Failed to inject MCP scripts: ${failed.join(", ")} did not initialize after 3 attempts.`
    );
  }

  private async getScriptReadiness(): Promise<boolean[]> {
    const evaluate = this.getEvaluate();
    return evaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      return [
        Boolean(root.abProps),
        Boolean(root.stanzaLogger),
        Boolean(root.wamLogger),
      ];
    });
  }

  private async areAllReady(): Promise<boolean> {
    const readiness = await this.getScriptReadiness();
    return readiness.every(Boolean);
  }

  private async injectScriptSource(source: string): Promise<void> {
    const evaluate = this.getEvaluate();
    await evaluate((scriptSource: string) => {
      const globalEval = globalThis.eval as (code: string) => unknown;
      globalEval(scriptSource);
    }, source);
  }

}
