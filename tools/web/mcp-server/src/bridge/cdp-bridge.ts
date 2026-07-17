import { execSync } from "node:child_process";
import { createServer } from "node:net";
import { chromium, type Browser, type BrowserContext, type Page, type Response } from "playwright";
import type { SnapshotPlatform } from "../types/snapshot.js";
import type { PlatformBridge, CapturedResponse, ResponseListener, LoadedResources } from "../types/bridge.js";
import { sleep } from "../utils/async.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("cdp-bridge");

const DESKTOP_AUMID = "5319275A.WhatsAppDesktop_cv1g1gvanyjgm!App";
const CDP_ENV_VAR = "WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS";
const CDP_POLL_INTERVAL_MS = 500;
const CDP_MAX_WAIT_MS = 30_000;

export interface CdpBridgeOptions {
  launch?: {
    headless?: boolean;
    slowMo?: number;
    locale?: string;
    viewport?: { width: number; height: number };
  };
  cdpUrl?: string;
  autoLaunchDesktop?: boolean;
  navigationTimeoutMs?: number;
}

export class CdpBridge implements PlatformBridge {
  readonly platform: SnapshotPlatform;
  readonly page: Page;
  readonly context: BrowserContext;
  readonly browser: Browser;
  readonly cdpUrl: string | null;

  private readonly ownsBrowser: boolean;
  private responseListeners: ResponseListener[] = [];

  private constructor(
    platform: SnapshotPlatform,
    browser: Browser,
    context: BrowserContext,
    page: Page,
    ownsBrowser: boolean,
    cdpUrl: string | null = null
  ) {
    this.platform = platform;
    this.browser = browser;
    this.context = context;
    this.page = page;
    this.ownsBrowser = ownsBrowser;
    this.cdpUrl = cdpUrl;

    this.page.on("response", (res: Response) => {
      if (this.responseListeners.length === 0) return;
      const captured: CapturedResponse = {
        url: res.url(),
        contentType: res.headers()["content-type"] ?? "",
        getBody: () => res.body(),
      };
      for (const listener of this.responseListeners) {
        listener(captured);
      }
    });
  }

  static async connect(
    platform: SnapshotPlatform,
    options: CdpBridgeOptions = {}
  ): Promise<CdpBridge> {
    if (options.cdpUrl) {
      let cdpUrl = options.cdpUrl;
      if (options.autoLaunchDesktop) {
        cdpUrl = await CdpBridge.ensureDesktopRunning(cdpUrl);
      }
      const browser = await chromium.connectOverCDP(cdpUrl);
      const contexts = browser.contexts();
      if (contexts.length === 0) {
        throw new Error("No browser contexts found via CDP.");
      }
      for (const context of contexts) {
        const page = context.pages().find(
          (p) => p.url().includes("web.whatsapp.com")
        );
        if (page) {
          return new CdpBridge(platform, browser, context, page, false, cdpUrl);
        }
      }
      const urls = contexts.flatMap((c) => c.pages().map((p) => p.url()));
      throw new Error(
        `No WhatsApp page found on CDP endpoint ${cdpUrl}. ` +
          `The port may be held by another WebView2 process (e.g., Windows Widgets). ` +
          `Available pages: ${urls.join(", ")}`
      );
    }

    const launchOpts = options.launch ?? {};
    const browser = await chromium.launch({
      headless: launchOpts.headless ?? false,
      slowMo: launchOpts.slowMo,
    });
    const locale = launchOpts.locale;
    const context = await browser.newContext({
      viewport: launchOpts.viewport ?? { width: 1920, height: 1080 },
      locale,
      ...(locale
        ? { extraHTTPHeaders: { "Accept-Language": `${locale},en;q=0.9` } }
        : {}),
    });
    const page = await context.newPage();
    return new CdpBridge(platform, browser, context, page, true);
  }

  private static async ensureDesktopRunning(preferredCdpUrl: string): Promise<string> {
    if (await CdpBridge.hasWhatsAppPage(preferredCdpUrl)) return preferredCdpUrl;

    const preferredPort = Number(new URL(preferredCdpUrl).port || "9222");
    const port = (await CdpBridge.isPortFree(preferredPort))
      ? preferredPort
      : await CdpBridge.findFreePort();
    const cdpUrl = `http://localhost:${port}`;
    if (port !== preferredPort) {
      log.warn(
        `CDP port ${preferredPort} is held by another process; launching WhatsApp Desktop on free port ${port} instead`
      );
    }

    CdpBridge.ensureCdpEnvVar(cdpUrl);
    CdpBridge.killDesktopProcesses();
    await sleep(1_000);
    CdpBridge.launchDesktopApp();

    const ready = await CdpBridge.waitForWhatsAppPage(cdpUrl);
    if (!ready) {
      throw new Error(
        `WhatsApp Desktop launched but no web.whatsapp.com page appeared on CDP at ${cdpUrl} within ${CDP_MAX_WAIT_MS / 1000}s.`
      );
    }
    return cdpUrl;
  }

  private static async hasWhatsAppPage(cdpUrl: string): Promise<boolean> {
    try {
      const res = await fetch(`${cdpUrl}/json/list`);
      if (!res.ok) return false;
      const targets = (await res.json()) as Array<{ url?: string }>;
      return targets.some((t) => t.url?.includes("web.whatsapp.com"));
    } catch {
      return false;
    }
  }

  private static async waitForWhatsAppPage(cdpUrl: string): Promise<boolean> {
    const deadline = Date.now() + CDP_MAX_WAIT_MS;
    while (Date.now() <= deadline) {
      if (await CdpBridge.hasWhatsAppPage(cdpUrl)) return true;
      await sleep(CDP_POLL_INTERVAL_MS);
    }
    return false;
  }

  private static isPortFree(port: number): Promise<boolean> {
    return new Promise((resolvePort) => {
      const server = createServer();
      server.unref();
      server.once("error", () => resolvePort(false));
      server.listen(port, "127.0.0.1", () => {
        server.close(() => resolvePort(true));
      });
    });
  }

  private static findFreePort(): Promise<number> {
    return new Promise((resolvePort, rejectPort) => {
      const server = createServer();
      server.unref();
      server.once("error", rejectPort);
      server.listen(0, "127.0.0.1", () => {
        const address = server.address();
        if (!address || typeof address === "string") {
          server.close();
          rejectPort(new Error("Could not allocate a free TCP port"));
          return;
        }
        const port = address.port;
        server.close(() => resolvePort(port));
      });
    });
  }

  private static ensureCdpEnvVar(cdpUrl: string): void {
    const port = new URL(cdpUrl).port || "9222";
    const expectedValue = `--remote-debugging-port=${port}`;
    try {
      const current = execSync(
        `powershell -NoProfile -Command "[System.Environment]::GetEnvironmentVariable('${CDP_ENV_VAR}', 'User')"`,
        { encoding: "utf-8" }
      ).trim();
      if (current === expectedValue) return;
      execSync(
        `powershell -NoProfile -Command "[System.Environment]::SetEnvironmentVariable('${CDP_ENV_VAR}', '${expectedValue}', 'User')"`,
        { encoding: "utf-8" }
      );
    } catch {

    }
  }

  private static killDesktopProcesses(): void {
    try {
      execSync(
        `powershell -NoProfile -Command "Get-Process -Name 'WhatsApp.Root' -ErrorAction SilentlyContinue | Stop-Process -Force"`,
        { encoding: "utf-8" }
      );
    } catch {

    }
  }

  private static launchDesktopApp(): void {
    try {
      execSync(
        `cmd /c start "" "shell:AppsFolder\\${DESKTOP_AUMID}"`,
        { encoding: "utf-8" }
      );
    } catch (error) {
      throw new Error(
        `Failed to launch WhatsApp Desktop: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  evaluate<R = unknown>(
    pageFunction: string | ((...args: any[]) => R | Promise<R>),
    ...args: any[]
  ): Promise<R> {
    return this.page.evaluate(pageFunction as any, ...args);
  }

  async addInitScript(source: string): Promise<void> {
    await this.page.addInitScript(source);
  }

  url(): string {
    return this.page.url();
  }

  async navigate(url: string, options?: { timeout?: number }): Promise<void> {
    await this.page.goto(url, {
      waitUntil: "networkidle",
      timeout: options?.timeout ?? 60_000,
    });
  }

  async reload(options?: { timeout?: number }): Promise<void> {
    await this.page.reload({
      waitUntil: "networkidle",
      timeout: options?.timeout ?? 60_000,
    });
  }

  async getLoadedResourceUrls(): Promise<LoadedResources> {
    return this.page.evaluate(() => {
      const entries = performance.getEntriesByType(
        "resource"
      ) as PerformanceResourceTiming[];
      const jsUrls: string[] = [];
      const wasmUrls: string[] = [];
      for (const entry of entries) {
        const url = entry.name;
        if (url.endsWith(".js")) {
          jsUrls.push(url);
        } else if (url.endsWith(".wasm")) {
          wasmUrls.push(url);
        }
      }
      return { jsUrls, wasmUrls };
    });
  }

  onResponse(listener: ResponseListener): void {
    this.responseListeners.push(listener);
  }

  removeResponseListeners(): void {
    this.responseListeners = [];
  }

  async fetchText(url: string): Promise<string> {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return res.text();
  }

  async fetchBinary(url: string): Promise<Buffer> {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return Buffer.from(await res.arrayBuffer());
  }

  async disconnect(): Promise<void> {
    this.removeResponseListeners();
    if (this.ownsBrowser) {
      await this.browser.close();
    } else {
      this.browser.removeAllListeners();
    }
  }
}
