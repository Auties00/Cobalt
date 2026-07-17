import type { ParsedModule, ParsedNativeModule } from "../types/module.js";
import type { SnapshotPlatform } from "../types/snapshot.js";
import type { PlatformBridge } from "../types/bridge.js";
import { connectToPlatform } from "../bridge/connect.js";
import { isWhatsAppModule, parseModules, parseRuntimeModule } from "./parser.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("extractor:browser");

const PAGE_LOAD_TIMEOUT = 60_000;
const WAIT_AFTER_LOAD = 5_000;
const LAZY_CHUNK_BATCH_SIZE = 50;
const LAZY_CHUNK_SETTLE_WAIT = 10_000;
const WASM_SETTLE_WAIT = 15_000;
const MAX_RETRIES = 3;
const RETRY_DELAY = 5_000;
const LOGIN_POLL_INTERVAL_MS = 3_000;
const LOGIN_TIMEOUT_MS = 600_000;

const DEFINE_HOOK = `(() => {
  if (window.__waDefineHookInstalled) return;
  window.__waDefineHookInstalled = true;
  window.__waDefinedModules = [];
  window.__waModules = {};
  const record = (name) => { try { window.__waDefinedModules.push(name); } catch (e) {} };
  const recordModule = (name, deps, args) => {
    try {
      if (typeof name !== "string" || window.__waModules[name]) return;
      let factory = null;
      for (let i = 0; i < args.length; i++) {
        if (typeof args[i] === "function") { factory = args[i]; break; }
      }
      window.__waModules[name] = {
        deps: Array.isArray(deps) ? deps : [],
        source: factory ? Function.prototype.toString.call(factory) : null,
      };
    } catch (e) {}
  };
  const wrap = (fn) => function (name, deps) {
    record(name);
    recordModule(name, deps, arguments);
    return fn.apply(this, arguments);
  };
  let current;
  Object.defineProperty(window, "__d", {
    configurable: true,
    enumerable: true,
    get() { return current; },
    set(fn) { current = typeof fn === "function" ? wrap(fn) : fn; },
  });
})()`;

const WASM_CAPTURE_HOOK = `(() => {
  if (window.__waWasmCaptureInstalled) return;
  window.__waWasmCaptureInstalled = true;
  window.__waWasmUrls = [];
  const add = (u) => { try { if (u && /\\.wasm/i.test(String(u))) window.__waWasmUrls.push(String(u)); } catch (e) {} };
  const oFetch = window.fetch;
  if (typeof oFetch === "function") {
    window.fetch = function (input) {
      try { add(input && input.url ? input.url : input); } catch (e) {}
      return oFetch.apply(this, arguments);
    };
  }
  for (const fn of ["instantiateStreaming", "compileStreaming"]) {
    const orig = WebAssembly[fn];
    if (typeof orig === "function") {
      WebAssembly[fn] = function (src) {
        try { Promise.resolve(src).then((r) => { if (r && r.url) add(r.url); }).catch(() => {}); } catch (e) {}
        return orig.apply(this, arguments);
      };
    }
  }
})()`;

const AGNOSTIC_WASM_TRIGGER = `(() => {
  if (typeof require !== "function") return { fired: [], reason: "no require" };
  const req = require;
  const defs = Array.from(new Set(window.__waDefinedModules || []));
  if (defs.length === 0) {
    defs.push("WAWebVoipWebWasmVariantLoader", "WAGetKaleidoscopeWasm");
  }
  const fired = [];
  const looksLikeLoaderName = (n) =>
    /wasm/i.test(n) && !/^WASmax/.test(n) && !/Mocks/i.test(n);
  const looksLikeLoaderExport = (k) =>
    /wasm|^(get|load|init|preload|warm|ensure|create|build)/i.test(k);
  for (const name of defs) {
    if (!looksLikeLoaderName(name)) continue;
    let m;
    try { m = req(name); } catch (e) { continue; }
    if (!m) continue;
    const tryCall = (fn, label) => {
      if (typeof fn !== "function" || fn.length > 1) return;
      try { Promise.resolve(fn()).catch(() => {}); fired.push(label); } catch (e) {}
    };
    if (typeof m === "object") {
      for (const k of Object.keys(m)) {
        if (!looksLikeLoaderExport(k)) continue;
        let v;
        try { v = m[k]; } catch (e) { continue; }
        tryCall(v, name + "." + k);
      }
    } else if (typeof m === "function") {
      tryCall(m, name);
    }
  }
  try {
    const jrfi = req("JSResourceForInteraction");
    for (const name of defs) {
      if (!/wasm/i.test(name) || !/\\.worker$/i.test(name)) continue;
      try {
        const handle = jrfi(name);
        const ref = handle && handle.__setRef ? handle.__setRef("snapshot") : handle;
        if (ref && typeof ref.load === "function") {
          Promise.resolve(ref.load()).catch(() => {});
          fired.push("worker:" + name);
        }
      } catch (e) {}
    }
  } catch (e) {}
  return { fired };
})()`;

interface RuntimeModule {
  name: string;
  deps: string[];
  source: string | null;
}

interface CapturedResponses {
  runtimeModules: RuntimeModule[];
  jsUrls: string[];
  wasmCaptures: Map<string, Buffer>;
}

async function extractRevision(bridge: PlatformBridge): Promise<string> {
  return bridge.evaluate(() => {
    if (typeof require !== "function") {
      throw new Error(
        "Could not extract client_revision from WhatsApp Web: require is not a function"
      );
    }

    const siteData = require("SiteData");
    if (!siteData) {
      throw new Error(
        "Could not extract client_revision from WhatsApp Web: no SiteData found"
      );
    }

    const result = siteData.client_revision?.toString();
    if (!result) {
      throw new Error(
        "Could not extract client_revision from WhatsApp Web: no client_revision found"
      );
    }

    return result;
  });
}

async function forceLoadAllChunks(bridge: PlatformBridge): Promise<void> {
  await bridge.evaluate((batchSize: number) => {
    if (typeof require !== "function") return;
    try {
      const Bootloader = require("Bootloader");
      if (!Bootloader?.__debug?.revMap || !Bootloader.loadResources) return;
      const loaded = Bootloader.__debug.DOMAppendedJSHashes;
      const all = [...Bootloader.__debug.revMap.keys()];
      const pending =
        loaded instanceof Set
          ? all.filter((h: string) => !loaded.has(h))
          : all;
      for (let i = 0; i < pending.length; i += batchSize) {
        try {
          Bootloader.loadResources(pending.slice(i, i + batchSize));
        } catch {}
      }
    } catch {}
  }, LAZY_CHUNK_BATCH_SIZE);
  await new Promise((resolve) => setTimeout(resolve, LAZY_CHUNK_SETTLE_WAIT));
}

async function collectRuntimeModules(
  bridge: PlatformBridge
): Promise<RuntimeModule[]> {
  return bridge.evaluate(() => {
    const registry =
      (window as unknown as {
        __waModules?: Record<string, { deps?: string[]; source?: string | null }>;
      }).__waModules ?? {};
    const modules: RuntimeModule[] = [];
    for (const name of Object.keys(registry)) {
      const entry = registry[name];
      modules.push({
        name,
        deps: Array.isArray(entry?.deps) ? entry.deps : [],
        source: entry?.source ?? null,
      });
    }
    return modules;
  });
}

async function discoverBxDataWasmUrls(
  bridge: PlatformBridge
): Promise<string[]> {
  return bridge.evaluate(() => {
    const urls: string[] = [];
    const pattern =
      /"uri"\s*:\s*"(https:\\\/\\\/static\.whatsapp\.net\\\/rsrc\.php\\\/[^"]+\.wasm)"/g;
    for (const script of Array.from(
      document.querySelectorAll("script:not([src])")
    )) {
      const text = script.textContent ?? "";
      let m: RegExpExecArray | null;
      while ((m = pattern.exec(text))) {
        urls.push(m[1].replace(/\\\//g, "/"));
      }
    }
    return urls;
  });
}

const AUTH_PROBE = `(() => ({
  loggedIn: document.querySelector("#pane-side") != null
    || document.querySelector('[data-testid="chat-list"]') != null
    || document.querySelector('[data-testid="conversation-panel-messages"]') != null,
}))()`;

async function ensureLoggedIn(bridge: PlatformBridge): Promise<void> {
  const deadline = Date.now() + LOGIN_TIMEOUT_MS;
  let announced = false;
  for (;;) {
    let loggedIn = false;
    try {
      const result = (await bridge.evaluate(AUTH_PROBE)) as { loggedIn: boolean };
      loggedIn = Boolean(result?.loggedIn);
    } catch {
      void 0;
    }
    if (loggedIn) {
      if (announced) log.info("login detected; continuing extraction");
      return;
    }
    if (!announced) {
      announced = true;
      const where =
        bridge.platform === "web"
          ? "the browser window that just opened"
          : "the WhatsApp Desktop window";
      log.warn("================ ACTION REQUIRED ================");
      log.warn("Log in to WhatsApp to capture the full WASM set.");
      log.warn(`Scan the QR code or link with your phone number in ${where}.`);
      log.warn(`Waiting up to ${Math.round(LOGIN_TIMEOUT_MS / 60_000)} minutes for login...`);
      log.warn("================================================");
    }
    if (Date.now() >= deadline) {
      throw new Error(
        `Timed out after ${Math.round(LOGIN_TIMEOUT_MS / 60_000)} minutes waiting for WhatsApp Web login.`
      );
    }
    await new Promise((resolve) => setTimeout(resolve, LOGIN_POLL_INTERVAL_MS));
  }
}

async function triggerWasmLoadersAgnostic(bridge: PlatformBridge): Promise<void> {
  try {
    const result = (await bridge.evaluate(AGNOSTIC_WASM_TRIGGER)) as {
      fired?: string[];
      reason?: string;
    };
    const fired = result?.fired ?? [];
    log.info(
      `WASM trigger pass fired ${fired.length} loader(s)${result?.reason ? ` (${result.reason})` : ""}`
    );
  } catch (error) {
    log.warn(
      `WASM trigger pass failed: ${error instanceof Error ? error.message : String(error)}`
    );
  }
  await new Promise((resolve) => setTimeout(resolve, WASM_SETTLE_WAIT));
}

async function collectResponses(
  bridge: PlatformBridge
): Promise<CapturedResponses> {
  const jsUrls = new Set<string>();
  const wasmCaptures = new Map<string, Buffer>();
  const pendingCaptures: Promise<void>[] = [];

  try {
    await bridge.addInitScript(DEFINE_HOOK);
    await bridge.addInitScript(WASM_CAPTURE_HOOK);
  } catch (error) {
    log.warn(
      `addInitScript failed: ${error instanceof Error ? error.message : String(error)}`
    );
  }

  const alreadyLoaded = await bridge.getLoadedResourceUrls();
  for (const url of alreadyLoaded.jsUrls) jsUrls.add(url);
  for (const url of alreadyLoaded.wasmUrls) {
    pendingCaptures.push(
      bridge
        .fetchBinary(url)
        .then((buf) => {
          wasmCaptures.set(url, buf);
        })
        .catch(() => {})
    );
  }

  bridge.onResponse((response) => {
    if (response.url.endsWith(".js")) {
      jsUrls.add(response.url);
    }
    const isWasm =
      response.url.endsWith(".wasm") ||
      response.contentType.includes("application/wasm");
    if (isWasm) {
      pendingCaptures.push(
        response
          .getBody()
          .then((body) => {
            wasmCaptures.set(response.url, body);
          })
          .catch(() => {})
      );
    }
  });

  if (bridge.platform === "web") {
    await bridge.navigate("https://web.whatsapp.com/", {
      timeout: PAGE_LOAD_TIMEOUT,
    });
    await new Promise((resolve) => setTimeout(resolve, WAIT_AFTER_LOAD));
  } else {
    await bridge.reload({ timeout: PAGE_LOAD_TIMEOUT });
    await new Promise((resolve) => setTimeout(resolve, WAIT_AFTER_LOAD));
  }

  await ensureLoggedIn(bridge);

  await forceLoadAllChunks(bridge);

  const bxWasmUrls = await discoverBxDataWasmUrls(bridge);
  for (const url of bxWasmUrls) {
    if (wasmCaptures.has(url)) continue;
    pendingCaptures.push(
      bridge
        .fetchBinary(url)
        .then((buf) => {
          wasmCaptures.set(url, buf);
        })
        .catch(() => {})
    );
  }

  await triggerWasmLoadersAgnostic(bridge);

  const runtimeModules = await collectRuntimeModules(bridge);
  log.info(`captured ${runtimeModules.length} module definitions from the __d registry`);

  await Promise.all(pendingCaptures);
  bridge.removeResponseListeners();

  return { runtimeModules, jsUrls: [...jsUrls], wasmCaptures };
}

function deriveNativeName(url: string): string {
  try {
    const pathname = new URL(url).pathname;
    const segments = pathname.split("/");
    const filename = segments[segments.length - 1] ?? "";
    const name = filename.replace(/\.wasm$/, "");
    return name || `wasm-${Date.now()}`;
  } catch {
    return `wasm-${Date.now()}`;
  }
}

async function fetchWithRetry(url: string): Promise<string> {
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt += 1) {
    try {
      const res = await fetch(url);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      return res.text();
    } catch (error) {
      if (attempt === MAX_RETRIES) throw error;
      await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY));
    }
  }
  throw new Error("Unreachable retry state");
}

function buildModulesFromRuntime(
  runtimeModules: RuntimeModule[]
): ParsedModule[] {
  const modules: ParsedModule[] = [];
  for (const runtimeModule of runtimeModules) {
    if (!isWhatsAppModule(runtimeModule.name)) continue;
    if (!runtimeModule.source) continue;
    const parsed = parseRuntimeModule(
      runtimeModule.name,
      runtimeModule.deps,
      runtimeModule.source
    );
    if (parsed) modules.push(parsed);
  }
  return modules;
}

async function parseFromNetworkUrls(jsUrls: string[]): Promise<ParsedModule[]> {
  const results = await Promise.all(
    jsUrls.map(async (url) => {
      try {
        return parseModules(await fetchWithRetry(url));
      } catch {
        return [] as ParsedModule[];
      }
    })
  );
  return results.flat();
}

async function parseCollectedResponses(
  runtimeModules: RuntimeModule[],
  jsUrls: string[],
  wasmCaptures: Map<string, Buffer>
): Promise<[ParsedModule[], ParsedNativeModule[]]> {
  let allModules = buildModulesFromRuntime(runtimeModules);
  if (allModules.length === 0) {
    log.warn(
      "runtime __d registry yielded no modules; falling back to network-fetched bundle parsing"
    );
    allModules = await parseFromNetworkUrls(jsUrls);
  }

  const nativeModules: ParsedNativeModule[] = [];
  for (const [url, binary] of wasmCaptures) {
    nativeModules.push({
      name: deriveNativeName(url),
      url,
      binary,
    });
  }

  return [allModules, nativeModules];
}

function assertJsPlatform(platform: SnapshotPlatform): void {
  if (platform !== "web" && platform !== "desktop_windows") {
    throw new Error(
      `JS-bundle extractor does not support platform="${platform}". ` +
        `Use the native Mach-O pipeline (extractNativeMachOModules) instead.`
    );
  }
}

export async function checkRevision(
  platform: SnapshotPlatform = "web"
): Promise<string> {
  assertJsPlatform(platform);
  log.info(`checking revision for platform=${platform}`);
  const bridge = await connectToPlatform(platform);
  try {
    if (bridge.platform === "web") {
      log.debug("navigating to web.whatsapp.com");
      await bridge.navigate("https://web.whatsapp.com/", {
        timeout: PAGE_LOAD_TIMEOUT,
      });
      await new Promise((resolve) => setTimeout(resolve, WAIT_AFTER_LOAD));
    }
    const revision = await extractRevision(bridge);
    log.info(`revision check result: ${revision}`);
    return revision;
  } finally {
    await bridge.disconnect();
  }
}

export async function extractModules(
  platform: SnapshotPlatform = "web"
): Promise<[string, ParsedModule[], ParsedNativeModule[]]> {
  assertJsPlatform(platform);
  log.info(`extracting modules for platform=${platform}`);
  const bridge = await connectToPlatform(platform);
  try {
    log.debug("collecting responses (JS + WASM)");
    const { runtimeModules, jsUrls, wasmCaptures } = await collectResponses(bridge);
    log.info(
      `collected ${runtimeModules.length} runtime modules, ${jsUrls.length} JS URLs, ${wasmCaptures.size} WASM binaries`
    );
    const revision = await extractRevision(bridge);
    log.info(`extracted revision: ${revision}`);
    await bridge.disconnect();

    log.debug("parsing collected responses");
    const [allModules, nativeModules] = await parseCollectedResponses(
      runtimeModules,
      jsUrls,
      wasmCaptures
    );
    log.info(`parsed ${allModules.length} modules, ${nativeModules.length} native modules`);
    return [revision, allModules, nativeModules];
  } catch (error) {
    log.error("extraction failed", error);
    await bridge.disconnect();
    throw error;
  }
}
