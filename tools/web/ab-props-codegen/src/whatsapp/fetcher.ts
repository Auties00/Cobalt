import { chromium } from "playwright";

const PAGE_LOAD_TIMEOUT = 60_000;
const WAIT_AFTER_LOAD = 5_000;
const MAX_RETRIES = 3;
const RETRY_DELAY = 5_000;

const sleep = (ms: number): Promise<void> =>
    new Promise((resolve) => setTimeout(resolve, ms));

async function fetchWithRetry(url: string): Promise<string> {
    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            const res = await fetch(url);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.text();
        } catch (err) {
            if (attempt === MAX_RETRIES) throw err;
            const message = err instanceof Error ? err.message : String(err);
            console.warn(`[WARN] Retry ${attempt}/${MAX_RETRIES} for ${url}: ${message}`);
            await sleep(RETRY_DELAY);
        }
    }
    throw new Error("unreachable");
}

/**
 * Launches a headless browser, navigates to WhatsApp Web, collects
 * every {@code .js} resource URL, downloads them all, and returns
 * the concatenated JS source.
 */
export async function fetchWhatsAppWebJs(): Promise<string> {
    console.log("[INFO] Launching browser...");

    const browser = await chromium.launch({ headless: false });
    const page = await browser.newPage({
        viewport: { width: 1920, height: 1080 }
    });

    const urls = new Set<string>();
    page.on("response", (res) => {
        if (res.url().endsWith(".js")) urls.add(res.url());
    });

    try {
        console.log("[INFO] Loading WhatsApp Web...");
        await page.goto("https://web.whatsapp.com/", {
            waitUntil: "networkidle",
            timeout: PAGE_LOAD_TIMEOUT,
        });
        await sleep(WAIT_AFTER_LOAD);
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        console.warn(`[WARN] Page load issue: ${message}`);
    } finally {
        await browser.close();
    }

    console.log(`[INFO] Found ${urls.size} JS files`);

    const chunks: string[] = [];
    const urlList = [...urls];
    for (let i = 0; i < urlList.length; i++) {
        const url = urlList[i];
        const filename = url.split("/").pop()?.split("?")[0] ?? url;
        try {
            console.log(`[INFO] [${i + 1}/${urlList.length}] Downloading ${filename}...`);
            chunks.push(await fetchWithRetry(url));
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            console.error(`[ERROR] Failed to download ${filename}: ${message}`);
        }
    }

    console.log(`[INFO] Downloaded ${chunks.length} JS files`);
    return chunks.join("\n");
}
