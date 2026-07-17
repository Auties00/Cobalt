/**
 * Page-side scripts injected into the WhatsApp Web context via
 * {@link import("playwright").Page.evaluate} and
 * {@link import("playwright").BrowserContext.addInitScript}.
 *
 * @remarks
 * Every function in this file executes inside the browser, not in Node. They
 * receive only the arguments Playwright explicitly forwards; they cannot close
 * over Node-side variables. Each function is self-contained.
 *
 * Window-level state, written by these scripts and consumed by the orchestrator
 * in {@link import("./browser.ts").fetchWhatsAppWeb}:
 * - {@code window.__WA_CAPTURED_WASM_URLS__}: every wasm URL passed to
 *   {@link WebAssembly.instantiateStreaming}, {@link WebAssembly.compileStreaming},
 *   or {@link fetch} (including service-worker cache hits).
 */

/**
 * Installs hooks BEFORE WA's bundle runs, wrapping
 * {@link WebAssembly.instantiateStreaming}/{@link WebAssembly.compileStreaming}
 * and {@link fetch} to capture every wasm URL the page requests into
 * {@code window.__WA_CAPTURED_WASM_URLS__}.
 *
 * @remarks
 * Run via {@link import("playwright").BrowserContext.addInitScript} so this
 * code executes before any of WA's own scripts.
 */
export function installCaptureHooks(): void {
    const w = window as Window & {
        __WA_CAPTURED_WASM_URLS__?: string[];
    };
    if (!w.__WA_CAPTURED_WASM_URLS__) w.__WA_CAPTURED_WASM_URLS__ = [];
    const captured = w.__WA_CAPTURED_WASM_URLS__;

    const trackUrl = (src: unknown): void => {
        try {
            let url: string | null = null;
            if (typeof src === "string") url = src;
            else if (src && typeof src === "object" && "url" in (src as { url?: unknown })) {
                const u = (src as { url?: unknown }).url;
                if (typeof u === "string") url = u;
            }
            if (url && url.includes(".wasm")) captured.push(url);
        } catch { /* ignore */ }
    };

    const originalIS = WebAssembly.instantiateStreaming;
    if (originalIS) {
        WebAssembly.instantiateStreaming = function (source, ...rest) {
            Promise.resolve(source).then(trackUrl);
            return originalIS.call(this, source, ...rest);
        };
    }
    const originalCS = WebAssembly.compileStreaming;
    if (originalCS) {
        WebAssembly.compileStreaming = function (source, ...rest) {
            Promise.resolve(source).then(trackUrl);
            return originalCS.call(this, source, ...rest);
        };
    }
    const originalFetch = window.fetch;
    if (originalFetch) {
        window.fetch = function (this: typeof globalThis, input, init) {
            try {
                const url = typeof input === "string" ? input :
                    input instanceof URL ? input.toString() :
                    (input as { url?: string }).url;
                if (url && /\.wasm($|\?)/.test(url)) captured.push(url);
            } catch { /* ignore */ }
            return originalFetch.call(this, input as RequestInfo, init);
        } as typeof fetch;
    }
}

/**
 * Force-loads every JS chunk the BDLDR knows about, in batches.
 *
 * @param batchSize - number of resource hashes per call to {@code Bootloader.loadResources}.
 *
 * @remarks
 * WA's bootloader exposes its full chunk registry as
 * {@code require("Bootloader").__debug.revMap}. Iterating those hashes and
 * calling {@code loadResources(batch)} triggers each chunk's network fetch, so
 * the orchestrator's network route observes every chunk URL for download.
 */
export async function forceLoadAllChunks(batchSize: number): Promise<void> {
    const req = (globalThis as { require?: Function }).require;
    if (typeof req !== "function") return;
    try {
        const Bootloader = req("Bootloader") as {
            __debug?: { revMap?: Map<string, unknown>; DOMAppendedJSHashes?: Set<string> };
            loadResources?: (hashes: string[]) => void;
        };
        if (!Bootloader?.__debug?.revMap || !Bootloader.loadResources) return;
        const loaded = Bootloader.__debug.DOMAppendedJSHashes;
        const all = [...Bootloader.__debug.revMap.keys()];
        const pending = loaded instanceof Set ? all.filter((h) => !loaded.has(h)) : all;
        for (let i = 0; i < pending.length; i += batchSize) {
            try {
                Bootloader.loadResources(pending.slice(i, i + batchSize));
            } catch { /* ignore */ }
        }
    } catch { /* ignore */ }
}

/** Returns the list of wasm URLs surfaced via {@link installCaptureHooks}. */
export function readCapturedWasmUrls(): string[] {
    const w = window as Window & { __WA_CAPTURED_WASM_URLS__?: string[] };
    return w.__WA_CAPTURED_WASM_URLS__ ?? [];
}

/**
 * Captures the {@code .wasm} URL of each lazy/call-gated wasm-loader module
 * without a call and without instantiating anything. Loading a voip-style
 * Emscripten loader and running its module factory makes it {@code fetch} its
 * wasm as the first step of instantiation; this temporarily wraps {@code fetch}
 * to record that URL and reject it, and neuters {@code WebAssembly.instantiate*}
 * and the {@code Worker} constructor, so the fetch fires (URL captured) but the
 * compile, the 2 GB shared memory, and the pthread pool never materialise. That
 * keeps the renderer safe while surfacing URLs that never appear on the network
 * from an idle session.
 *
 * @param loaderModules - the lazy wasm-loader modules to drive.
 * @returns the distinct captured {@code .wasm} URLs.
 */
export async function captureLazyWasmUrls(loaderModules: readonly string[]): Promise<string[]> {
    const g = globalThis as typeof globalThis & { require?: (name: string) => unknown; fetch: typeof fetch; Worker: typeof Worker };
    const req = g.require;
    if (typeof req !== "function") return [];

    const captured = new Set<string>();
    const urlOf = (input: unknown): string | null => {
        try {
            if (typeof input === "string") return input;
            if (input instanceof URL) return input.toString();
            if (input && typeof (input as { url?: unknown }).url === "string") return (input as { url: string }).url;
        } catch { /* ignore */ }
        return null;
    };

    const originalFetch = g.fetch;
    const originalInstantiate = WebAssembly.instantiate;
    const originalInstantiateStreaming = WebAssembly.instantiateStreaming;

    g.fetch = function (this: unknown, input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
        const url = urlOf(input);
        if (url && /\.wasm(\?|$)/.test(url)) {
            captured.add(url);
            return Promise.reject(new Error("captured"));
        }
        return originalFetch.call(this, input as RequestInfo, init);
    } as typeof fetch;
    WebAssembly.instantiate = (() => Promise.reject(new Error("blocked"))) as unknown as typeof WebAssembly.instantiate;
    WebAssembly.instantiateStreaming = (() => Promise.reject(new Error("blocked"))) as typeof WebAssembly.instantiateStreaming;

    try {
        const jsResource = req("JSResourceForInteraction") as ((name: string) => { load(): Promise<unknown> }) | undefined;
        for (const module of loaderModules) {
            let factory: unknown;
            try {
                const loaded = typeof jsResource === "function" ? await jsResource(module).load() : undefined;
                factory = typeof loaded === "function" ? loaded : req(module);
            } catch {
                continue;
            }
            if (typeof factory !== "function") continue;
            try {
                const instance = (factory as (config: unknown) => unknown)({});
                if (instance && typeof (instance as { then?: unknown }).then === "function") {
                    (instance as Promise<unknown>).then(() => undefined, () => undefined);
                }
            } catch { /* the wasm fetch fires before any instantiation failure */ }
            await new Promise((resolve) => setTimeout(resolve, 500));
        }
    } finally {
        g.fetch = originalFetch;
        WebAssembly.instantiate = originalInstantiate;
        WebAssembly.instantiateStreaming = originalInstantiateStreaming;
    }
    return [...captured];
}
