import { join } from "node:path";
import { chromium, type BrowserContext, type Page } from "playwright";
import { fetchBinary, fetchText, sleep } from "./http.js";
import { extractBootstrapUrl, extractVersion } from "./service-worker.js";
import { scanForWasmUrls } from "./url-scanner.js";
import { captureLazyWasmUrls, forceLoadAllChunks, installCaptureHooks, readCapturedWasmUrls } from "./inject.js";

const BASE_URL = "https://web.whatsapp.com";
/**
 * Persistent Chromium profile directory. Reusing one profile across runs keeps
 * the WhatsApp Web session logged in and its history synced, so only the first
 * run pays the QR scan and full history sync.
 */
const PROFILE_DIR = join(process.cwd(), ".wa-profile");
const PAGE_LOAD_TIMEOUT_MS = 60_000;
const WAIT_AFTER_LOAD_MS = 5_000;
const LAZY_CHUNK_BATCH_SIZE = 50;
const LAZY_CHUNK_SETTLE_WAIT_MS = 10_000;
/** How long to wait for the operator to scan the QR / enter the pairing code. */
const LOGIN_WAIT_TIMEOUT_MS = 20 * 60_000;
/** Short post-login settle so voip gating (self identity, browser support) is ready before the loader runs. */
const POST_LOGIN_SETTLE_MS = 15_000;
/**
 * Lazy/call-gated wasm-loader modules whose engine wasm is fetched (and its URL
 * captured) by running their factory under neutered fetch/instantiate hooks. Add
 * a module here to surface another feature's wasm.
 */
const LAZY_WASM_LOADER_MODULES: readonly string[] = ["WAWebVoipWebWasmLoader"];

/** A downloaded JavaScript chunk served by WhatsApp Web. */
export interface JsChunk {
    readonly url: string;
    readonly content: string;
}

/** A downloaded {@code .wasm} module served by WhatsApp Web. */
export interface WasmBinary {
    readonly url: string;
    readonly data: Uint8Array;
}

/** Everything captured from a WhatsApp Web page load. */
export interface FetchResult {
    /** The WhatsApp Web version, e.g. {@code "2.3000.1039683107"}. */
    readonly version: string;
    /** Every {@code .js} resource downloaded during the page load. */
    readonly chunks: readonly JsChunk[];
    /** Every {@code .wasm} resource downloaded. */
    readonly wasmBinaries: readonly WasmBinary[];
}

/** The {@code .js} and {@code .wasm} URLs observed during a page session. */
interface PageCaptureState {
    readonly jsUrls: Set<string>;
    readonly wasmUrls: Set<string>;
}

/**
 * Launches Chromium, logs in to WhatsApp Web, force-loads every JS chunk for full
 * proto coverage, drives the voip loader to make the engine fetch its wasm (the
 * only way that {@code .wasm} URL surfaces), downloads every observed JS and wasm
 * resource, and returns them.
 *
 * @returns a {@link FetchResult} with the version, JS chunks, and wasm binaries.
 *
 * @remarks
 * Chromium must run headed: in headless mode WA Web's bundle refuses to progress
 * past the splash screen, where most chunks are requested. Nothing touches the
 * live application until the operator has logged in.
 */
export async function fetchWhatsAppWeb(): Promise<FetchResult> {
    console.log(`[INFO] Launching browser (profile: ${PROFILE_DIR})...`);
    const context = await chromium.launchPersistentContext(PROFILE_DIR, {
        headless: false,
        viewport: { width: 1920, height: 1080 },
    });

    const state: PageCaptureState = { jsUrls: new Set(), wasmUrls: new Set() };
    await wireNetworkRoute(context, state);
    await context.addInitScript(installCaptureHooks);

    const page = context.pages()[0] ?? await context.newPage();

    let pageHtml = "";
    try {
        console.log("[INFO] Loading WhatsApp Web...");
        await page.goto(`${BASE_URL}/`, { waitUntil: "networkidle", timeout: PAGE_LOAD_TIMEOUT_MS });
        await sleep(WAIT_AFTER_LOAD_MS);

        await waitForLogin(page);
        await sleep(POST_LOGIN_SETTLE_MS);

        // Force-load every chunk for the full JS proto set and to register the
        // resource resolver the voip wasm URL is resolved through.
        await page.evaluate(forceLoadAllChunks, LAZY_CHUNK_BATCH_SIZE);
        await sleep(LAZY_CHUNK_SETTLE_WAIT_MS);

        const lazyWasmUrls = await page.evaluate(captureLazyWasmUrls, LAZY_WASM_LOADER_MODULES);
        for (const url of lazyWasmUrls) state.wasmUrls.add(url);
        console.log(`[INFO] Captured ${lazyWasmUrls.length} lazy wasm URLs: ${lazyWasmUrls.map((u) => u.split("/").pop()).join(", ")}`);

        await drainCapturedWasmUrls(page, state);
        pageHtml = await page.content();
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        console.warn(`[WARN] Page load issue: ${message}`);
    } finally {
        await context.close();
    }

    console.log(`[INFO] Page session: ${state.jsUrls.size} JS files, ${state.wasmUrls.size} wasm files`);

    const version = await fetchVersionAndBootstrap(state);
    const chunks = await downloadChunks(state);
    sweepChunkAndHtmlForWasmUrls(pageHtml, chunks, state);
    const wasmBinaries = await downloadWasmBinaries(state);

    console.log(`[INFO] Downloaded ${chunks.length} JS chunks and ${wasmBinaries.length} wasm modules`);
    return { version, chunks, wasmBinaries };
}

/** Resolves a possibly-relative resource URL against {@link BASE_URL}, falling back to the input when it is malformed. */
function toAbsoluteUrl(url: string): string {
    try {
        return new URL(url, BASE_URL).toString();
    } catch {
        return url;
    }
}

/** Records the {@code .js} and {@code .wasm} URL of every request the context makes (including worker requests). */
async function wireNetworkRoute(context: BrowserContext, state: PageCaptureState): Promise<void> {
    await context.route("**/*", async (route, request) => {
        const url = request.url();
        if (url.endsWith(".js")) state.jsUrls.add(url);
        else if (url.endsWith(".wasm")) state.wasmUrls.add(url);
        await route.continue();
    });
}

/** Adds the wasm URLs the page-realm capture hook saw (service-worker cache hits the network route misses). */
async function drainCapturedWasmUrls(page: Page, state: PageCaptureState): Promise<void> {
    const captured = await page.evaluate(readCapturedWasmUrls);
    for (const u of captured) state.wasmUrls.add(toAbsoluteUrl(u));
    console.log(`[INFO] WebAssembly hook surfaced ${new Set(captured).size} unique wasm URLs (SW cache hits)`);
}

/**
 * Blocks until the operator has logged WhatsApp Web in by scanning the QR code or
 * entering a pairing code. Resolves once the chat-list pane ({@code #pane-side})
 * is present, which only renders after a successful login handshake.
 *
 * @throws Error if no login is detected within {@link LOGIN_WAIT_TIMEOUT_MS}.
 */
async function waitForLogin(page: Page): Promise<void> {
    if (await page.$("#pane-side")) {
        console.log("[INFO] Persistent profile already logged in.");
        return;
    }
    console.log("");
    console.log("============================================================");
    console.log("[ACTION] Log in to WhatsApp Web in the opened browser window:");
    console.log("[ACTION]   scan the QR code (or use the pairing code) with a");
    console.log("[ACTION]   BETA-enabled account.");
    console.log("============================================================");
    console.log("");
    await page.waitForSelector("#pane-side", { timeout: LOGIN_WAIT_TIMEOUT_MS });
    console.log("[INFO] Login detected.");
}

async function fetchVersionAndBootstrap(state: PageCaptureState): Promise<string> {
    console.log("[INFO] Fetching service worker for version detection...");
    const swSource = await fetchText(`${BASE_URL}/sw.js`);
    const version = extractVersion(swSource);
    console.log(`[INFO] WhatsApp Web version: ${version}`);

    const bootstrapUrl = extractBootstrapUrl(swSource);
    if (bootstrapUrl) {
        state.jsUrls.add(bootstrapUrl);
    } else {
        console.warn("[WARN] No importScripts URL found in sw.js");
    }
    return version;
}

async function downloadChunks(state: PageCaptureState): Promise<JsChunk[]> {
    const chunks: JsChunk[] = [];
    const list = [...state.jsUrls];
    for (let i = 0; i < list.length; i++) {
        const url = list[i]!;
        const filename = url.split("/").pop()?.split("?")[0] ?? url;
        try {
            console.log(`[INFO] [${i + 1}/${list.length}] Downloading ${filename}...`);
            chunks.push({ url, content: await fetchText(url) });
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            console.error(`[ERROR] Failed to download ${filename}: ${message}`);
        }
    }
    return chunks;
}

function sweepChunkAndHtmlForWasmUrls(pageHtml: string, chunks: readonly JsChunk[], state: PageCaptureState): void {
    if (pageHtml) {
        for (const url of scanForWasmUrls(pageHtml)) state.wasmUrls.add(url);
    }
    for (const chunk of chunks) {
        for (const url of scanForWasmUrls(chunk.content)) state.wasmUrls.add(url);
    }
    console.log(`[INFO] Total wasm URLs after manifest+JS scan: ${state.wasmUrls.size}`);
}

async function downloadWasmBinaries(state: PageCaptureState): Promise<WasmBinary[]> {
    const binaries: WasmBinary[] = [];
    const list = [...state.wasmUrls];
    for (let i = 0; i < list.length; i++) {
        const url = list[i]!;
        const filename = url.split("/").pop()?.split("?")[0] ?? url;
        try {
            console.log(`[INFO] [${i + 1}/${list.length}] Downloading ${filename}...`);
            binaries.push({ url, data: await fetchBinary(url) });
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            console.error(`[ERROR] Failed to download ${filename}: ${message}`);
        }
    }
    return binaries;
}
