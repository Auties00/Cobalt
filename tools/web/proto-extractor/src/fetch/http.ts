/** Per-attempt timeout in milliseconds between retries. */
const RETRY_DELAY_MS = 5_000;
/** Total number of attempts (initial + retries). */
const MAX_ATTEMPTS = 3;

/**
 * Sleeps for the given number of milliseconds.
 *
 * @param ms - duration in milliseconds.
 */
export function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

async function withRetry<T>(label: string, op: () => Promise<T>): Promise<T> {
    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        try {
            return await op();
        } catch (err) {
            if (attempt === MAX_ATTEMPTS) throw err;
            const message = err instanceof Error ? err.message : String(err);
            console.warn(`[WARN] Retry ${attempt}/${MAX_ATTEMPTS} for ${label}: ${message}`);
            await sleep(RETRY_DELAY_MS);
        }
    }
    throw new Error("unreachable");
}

/**
 * Fetches a URL as text, retrying transient failures.
 *
 * @param url - the URL to fetch.
 * @returns the response body decoded as UTF-8.
 * @throws Error if the final attempt fails or the response is not 2xx.
 */
export function fetchText(url: string): Promise<string> {
    return withRetry(url, async () => {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.text();
    });
}

/**
 * Fetches a URL as a binary blob, retrying transient failures.
 *
 * @param url - the URL to fetch.
 * @returns the response body as a {@link Uint8Array}.
 * @throws Error if the final attempt fails or the response is not 2xx.
 */
export function fetchBinary(url: string): Promise<Uint8Array> {
    return withRetry(url, async () => {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const ab = await res.arrayBuffer();
        return new Uint8Array(ab);
    });
}
