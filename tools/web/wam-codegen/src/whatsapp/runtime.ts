import { chromium, type Browser, type Page } from "playwright";

const PAGE_LOAD_TIMEOUT = 60_000;
const WAIT_AFTER_LOAD = 5_000;
const LAZY_CHUNK_BATCH_SIZE = 50;
const LAZY_CHUNK_SETTLE_WAIT = 12_000;

const DEFINE_HOOK = `(() => {
  if (window.__waDefineHookInstalled) return;
  window.__waDefineHookInstalled = true;
  window.__waDefinedModules = [];
  window.__waWamEvents = [];
  const record = (name) => { try { window.__waDefinedModules.push(name); } catch (e) {} };
  const captureSpec = (spec) => {
    try { for (const k of Object.keys(spec || {})) window.__waWamEvents.push({ name: k, arr: spec[k] }); } catch (e) {}
  };
  const wrapDefineEvents = (exp) => {
    if (!exp || typeof exp.defineEvents !== "function" || exp.__waDeWrapped) return;
    const orig = exp.defineEvents;
    exp.defineEvents = function (spec) { captureSpec(spec); return orig.apply(this, arguments); };
    exp.__waDeWrapped = true;
  };
  const wrapDefine = (define) => function () {
    const args = Array.prototype.slice.call(arguments);
    record(args[0]);
    if (args[0] === "WAWebWamCodegenUtils" && typeof args[2] === "function") {
      const origFactory = args[2];
      args[2] = function () {
        const ret = origFactory.apply(this, arguments);
        for (let i = 0; i < arguments.length; i++) {
          const a = arguments[i];
          if (a && typeof a === "object") { wrapDefineEvents(a); if (a.exports) wrapDefineEvents(a.exports); }
        }
        return ret;
      };
    }
    return define.apply(this, args);
  };
  let current;
  Object.defineProperty(window, "__d", {
    configurable: true,
    enumerable: true,
    get() { return current; },
    set(fn) { current = typeof fn === "function" ? wrapDefine(fn) : fn; },
  });
})()`;

const FORCE_LOAD = `(() => {
  if (typeof require !== "function") return { ok: false, reason: "no require" };
  const BL = require("Bootloader");
  if (!BL || !BL.__debug || !BL.__debug.revMap || typeof BL.loadResources !== "function") {
    return { ok: false, reason: "no Bootloader.loadResources" };
  }
  const all = [...BL.__debug.revMap.keys()];
  const loaded = BL.__debug.DOMAppendedJSHashes;
  const pending = loaded instanceof Set ? all.filter((h) => !loaded.has(h)) : all;
  const BATCH = ${LAZY_CHUNK_BATCH_SIZE};
  let errors = 0;
  for (let i = 0; i < pending.length; i += BATCH) {
    try { BL.loadResources(pending.slice(i, i + BATCH)); } catch (e) { errors++; }
  }
  return { ok: true, total: all.length, pending: pending.length, errors };
})()`;

interface ForceLoadStats {
  readonly ok: boolean;
  readonly reason?: string;
  readonly total?: number;
  readonly pending?: number;
  readonly errors?: number;
}

export async function withForceLoadedBundle<T>(
  extract: () => T | Promise<T>,
): Promise<T> {
  console.log("[INFO] Launching browser (headed)...");
  const browser: Browser = await chromium.launch({ headless: false });
  try {
    const page: Page = await browser.newPage({
      viewport: { width: 1920, height: 1080 },
    });
    await page.addInitScript(DEFINE_HOOK);

    console.log("[INFO] Loading WhatsApp Web...");
    await page.goto("https://web.whatsapp.com/", {
      waitUntil: "networkidle",
      timeout: PAGE_LOAD_TIMEOUT,
    });
    await page.waitForTimeout(WAIT_AFTER_LOAD);

    console.log("[INFO] Force-loading all bootloader resources...");
    const stats = (await page.evaluate(FORCE_LOAD)) as ForceLoadStats;
    if (!stats.ok) {
      throw new Error(`Force-load failed: ${stats.reason}`);
    }
    console.log(
      `[INFO] Dispatched ${stats.pending}/${stats.total} pending resources (${stats.errors} batch errors); settling...`,
    );
    await page.waitForTimeout(LAZY_CHUNK_SETTLE_WAIT);

    const moduleCount = await page.evaluate(
      "(window.__waDefinedModules || []).length",
    );
    console.log(`[INFO] ${moduleCount} modules defined; extracting...`);
    if (typeof moduleCount === "number" && moduleCount === 0) {
      throw new Error(
        "Define hook captured 0 modules; the pre-document __d hook did not bind.",
      );
    }

    return (await page.evaluate(extract)) as T;
  } finally {
    await browser.close();
  }
}
