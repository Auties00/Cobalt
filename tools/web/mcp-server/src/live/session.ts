import { chromium, type Browser, type BrowserContext, type Page } from "playwright";
import { existsSync, mkdirSync } from "node:fs";
import { rm } from "node:fs/promises";
import { createServer } from "node:net";
import { join as pathJoin } from "node:path";
import { LiveCdpDebugger } from "./debug/cdp-debugger.js";
import { McpScriptInjector } from "./inject/mcp-script-injector.js";
import { NetworkCapture } from "./network/network-capture.js";
import { createLogger } from "../utils/logger.js";
import { extractIdentity } from "./identity.js";
import {
  DEFAULT_DESKTOP_CDP_PORT,
  DEFAULT_NAVIGATION_TIMEOUT_MS,
  DEFAULT_PAIRING_CODE_TIMEOUT_MS,
  DEFAULT_USER_DATA_DIR_ROOT,
  DEFAULT_WEB_LOCALE,
  PHONE_TYPE_DELAY_MS,
} from "./manager-constants.js";
import {
  buildFullPhoneValue,
  buildWebUrlForLocale,
  normalizePairingCode,
  normalizePhone,
  phoneDigitsOnly,
} from "./manager-utils.js";
import { sleep } from "../utils/async.js";
import type {
  BrowserAbProps,
  BrowserStanzaLogger,
  BrowserWamLogger,
  LiveSessionInfo,
  PhoneLoginOptions,
  PhoneLoginResult,
  SessionIdentity,
  SessionMode,
  StartSessionOptions,
  WaitForLoginResult,
  WebAuthState,
} from "../types/live/session.js";
import type {
  DebugCommand,
  DebugScriptInfo,
  EvaluateResult,
  SetBreakpointResult,
  SetWasmBreakpointResult,
} from "../types/live/debug.js";
import type {
  NetworkCaptureQuery,
  NetworkCaptureState,
  NetworkRequest,
  WebSocketFrame,
} from "../types/network.js";
import { CdpBridge } from "../bridge/cdp-bridge.js";

const log = createLogger("live:session");

function sessionIdToDirName(sessionId: string): string {
  const cleaned = sessionId.replace(/[^A-Za-z0-9._-]+/g, "_");
  if (!cleaned || cleaned.startsWith(".")) return `_${cleaned}`;
  return cleaned;
}

export class LiveWebSession {
  readonly sessionId: string;
  private mode: SessionMode;
  private readonly ephemeral: boolean;
  private readonly userDataDir: string | null;

  private browser: Browser | null = null;
  private context: BrowserContext | null = null;
  private page: Page | null = null;
  private pairingCode: string | null = null;
  private snapshotRevision: string | null = null;
  private startedAt: string | null = null;
  private updatedAt: string = new Date().toISOString();

  private lastAuthState: WebAuthState = "unknown";
  private identity: SessionIdentity | null = null;
  private linkedAt: string | null = null;

  private readonly scriptInjector: McpScriptInjector;
  private readonly debuggerBridge: LiveCdpDebugger;
  private readonly networkCapture: NetworkCapture;
  private cdpPort: number | null = null;

  constructor(
    sessionId: string,
    mode: SessionMode,
    options: { ephemeral?: boolean } = {}
  ) {
    this.sessionId = sessionId;
    this.mode = mode;
    this.ephemeral = options.ephemeral ?? false;

    this.userDataDir =
      mode === "web" && !this.ephemeral
        ? pathJoin(DEFAULT_USER_DATA_DIR_ROOT, sessionIdToDirName(sessionId), "chromium")
        : null;
    this.scriptInjector = new McpScriptInjector(
      () => this.bridgeEvaluate.bind(this)
    );
    this.debuggerBridge = new LiveCdpDebugger(
      () => this.requireContext(),
      () => this.requirePage(),
      () => this.getCdpPort()
    );
    this.networkCapture = new NetworkCapture(
      () => this.requireContext(),
      () => this.requirePage()
    );
  }

  getMode(): SessionMode {
    return this.mode;
  }

  isRunning(): boolean {
    return this.page !== null;
  }

  private updateTimestamp(): void {
    this.updatedAt = new Date().toISOString();
  }

  private requirePage(): Page {
    if (!this.page) throw new Error(`Live session "${this.sessionId}" not started.`);
    return this.page;
  }

  private requireContext(): BrowserContext {
    if (!this.context) throw new Error(`Live session "${this.sessionId}" context not started.`);
    return this.context;
  }

  private async bridgeEvaluate<R = unknown>(
    pageFunction: string | ((...args: any[]) => R | Promise<R>),
    ...args: any[]
  ): Promise<R> {
    const page = this.requirePage();
    return page.evaluate(pageFunction as any, ...args);
  }

  private async ensureMcpScriptsInjected(): Promise<void> {
    await this.scriptInjector.ensureInjected();
  }

  private async readRevision(): Promise<string | null> {
    if (!this.page) return null;
    try {
      return await this.bridgeEvaluate(() => {
        const globalObject = globalThis as unknown as Record<string, unknown>;
        const siteData = globalObject.SiteData as Record<string, unknown> | undefined;
        const rawRevision = siteData?.client_revision;
        if (typeof rawRevision === "string" || typeof rawRevision === "number") {
          return String(rawRevision);
        }
        const scripts = document.querySelectorAll("script");
        for (let i = 0; i < scripts.length; i += 1) {
          const script = scripts.item(i);
          if (!script) continue;
          const text = script.textContent ?? "";
          const match = text.match(/client_revision['":\s]+(\d+)/);
          if (match) return match[1];
        }
        return null;
      });
    } catch {
      return null;
    }
  }

  private async detectAuthAndCode(): Promise<{
    authState: WebAuthState;
    pairingCode: string | null;
  }> {
    if (!this.page) {
      return { authState: "unknown", pairingCode: null };
    }

    return this.page.evaluate(() => {

      const hasLoggedUi =
        document.querySelector("#pane-side") != null ||
        document.querySelector('[data-testid="chat-list-search"]') != null ||
        document.querySelector('[data-testid="conversation-panel-messages"]') != null;

      const hasQrElement =
        document.querySelector('[data-testid="qrcode"]') != null ||
        document.querySelector("div[data-ref]") != null ||
        document.querySelector("canvas[aria-label]") != null;

      const hasPhoneInput =
        document.querySelector('input[type="tel"]') != null;

      let code: string | null = null;

      const codeContainer = document.querySelector(
        '[aria-label*="code on your phone" i],[aria-label*="enter code" i]'
      );
      if (codeContainer?.textContent) {
        const compact = codeContainer.textContent.replace(/[^A-Za-z0-9]/g, "");
        if (compact.length >= 8) {
          code = compact.slice(0, 8).toUpperCase();
        }
      }

      if (!code) {
        const text = document.body?.innerText ?? "";
        const spacedCodeMatch = text.match(/(?:[A-Z0-9]\s*){8,10}/g);
        if (spacedCodeMatch?.length) {
          const normalized = spacedCodeMatch
            .map((segment) => segment.replace(/[^A-Za-z0-9]/g, ""))
            .find((segment) => segment.length >= 8);
          if (normalized) {
            code = normalized.slice(0, 8).toUpperCase();
          }
        }
      }

      if (!code) {
        const text = document.body?.innerText ?? "";
        const regexes = [
          /\b[A-Z0-9]{4}\s?[A-Z0-9]{4}\b/g,
          /\b\d{8}\b/g,
          /\b[A-Z0-9]{3}\s?[A-Z0-9]{3}\s?[A-Z0-9]{3}\b/g,
        ];
        for (const regex of regexes) {
          const match = regex.exec(text);
          regex.lastIndex = 0;
          if (match?.[0]) {
            code = match[0].replace(/[^A-Za-z0-9]/g, "").toUpperCase();
            break;
          }
        }
      }

      const bodyText = (document.body?.innerText ?? "").toLowerCase();

      const isRateLimited =
        bodyText.includes("too many attempts") ||
        bodyText.includes("try again later");

      const isPhoneLinkScreen = hasPhoneInput ||
        bodyText.includes("log in with phone number") ||
        bodyText.includes("link with phone number");

      const isQrScreen = hasQrElement ||
        bodyText.includes("scan this qr code") ||
        bodyText.includes("use whatsapp on your phone to link");

      if (hasLoggedUi) {
        return { authState: "logged_in" as const, pairingCode: null };
      }
      if (isRateLimited) {
        return { authState: "rate_limited" as const, pairingCode: null };
      }
      if (code) {
        return { authState: "pairing_code_ready" as const, pairingCode: code };
      }
      if (isPhoneLinkScreen) {
        return { authState: "phone_link_required" as const, pairingCode: null };
      }
      if (isQrScreen) {
        return { authState: "qr_required" as const, pairingCode: null };
      }
      return { authState: "unknown" as const, pairingCode: null };
    });
  }

  private async clickByText(candidates: string[]): Promise<boolean> {
    const page = this.requirePage();
    const normalized = candidates.map((item) => item.toLowerCase());
    return page.evaluate((patterns) => {
      const elements = Array.from(
        document.querySelectorAll<HTMLElement>(
          "button,[role='button'],div[role='button'],a,span"
        )
      );
      for (const element of elements) {
        const text = (
          `${element.innerText ?? ""} ${element.getAttribute("aria-label") ?? ""}`
        ).toLowerCase();
        if (!text.trim()) continue;
        if (!patterns.some((pattern) => text.includes(pattern))) continue;
        const style = window.getComputedStyle(element);
        const rect = element.getBoundingClientRect();
        const visible =
          style.display !== "none" &&
          style.visibility !== "hidden" &&
          rect.width > 0 &&
          rect.height > 0;
        if (!visible) continue;
        const clickable = element.closest<HTMLElement>(
          "button,[role='button'],a,div[role='button']"
        );
        const target = clickable ?? element;
        const targetRect = target.getBoundingClientRect();
        if (targetRect.width <= 0 || targetRect.height <= 0) continue;
        target.scrollIntoView({ block: "center", inline: "center" });
        if (target instanceof HTMLElement) {
          target.focus();
        } else {
          continue;
        }
        target.click();
        return true;
      }
      return false;
    }, normalized);
  }

  private async fillPhoneFields(
    phoneNumber: string,
    countryCode?: string
  ): Promise<string | null> {
    const page = this.requirePage();
    const normalizedPhone = phoneDigitsOnly(normalizePhone(phoneNumber));
    const normalizedCountry = countryCode ? normalizePhone(countryCode) : "";
    const fullPhoneValue = buildFullPhoneValue(phoneNumber, countryCode);

    const filledValue = await page.evaluate(
      (payload) => {
        const candidates = Array.from(
          document.querySelectorAll<HTMLElement>("input, [role='textbox'], [contenteditable='true']")
        );
        const visibleCandidates = candidates.filter((candidate) => {
          const style = window.getComputedStyle(candidate);
          return (
            style.display !== "none" &&
            style.visibility !== "hidden" &&
            candidate.getBoundingClientRect().width > 0 &&
            candidate.getBoundingClientRect().height > 0
          );
        });
        if (visibleCandidates.length === 0) return false;

        const phoneCandidates = visibleCandidates.filter((candidate) => {
          const hints = `${candidate.getAttribute("type") ?? ""} ${
            candidate.getAttribute("inputmode") ?? ""
          } ${candidate.getAttribute("aria-label") ?? ""} ${
            candidate.getAttribute("placeholder") ?? ""
          }`.toLowerCase();
          return (
            hints.includes("phone") ||
            candidate.getAttribute("type") === "tel" ||
            candidate.getAttribute("inputmode") === "numeric"
          );
        });

        const assignValue = (candidate: HTMLElement, value: string) => {
          candidate.focus();
          if (candidate instanceof HTMLInputElement || candidate instanceof HTMLTextAreaElement) {
            candidate.value = "";
            candidate.dispatchEvent(new Event("input", { bubbles: true }));
            candidate.value = value;
            candidate.dispatchEvent(new Event("input", { bubbles: true }));
            candidate.dispatchEvent(new Event("change", { bubbles: true }));
            return;
          }
          if (candidate.isContentEditable) {
            candidate.textContent = value;
            candidate.dispatchEvent(new Event("input", { bubbles: true }));
            candidate.dispatchEvent(new Event("change", { bubbles: true }));
          }
        };

        const readValue = (candidate: HTMLElement): string => {
          if (candidate instanceof HTMLInputElement || candidate instanceof HTMLTextAreaElement) {
            return candidate.value ?? "";
          }
          return candidate.textContent ?? "";
        };

        if (phoneCandidates.length > 0) {
          const target = phoneCandidates[phoneCandidates.length - 1];
          if (phoneCandidates.length > 1) {
            if (payload.country) {
              assignValue(phoneCandidates[0], payload.country);
            }
            assignValue(target, payload.phone);
          } else {
            assignValue(target, payload.full);
          }

          if (payload.country && phoneCandidates.length === 1 && visibleCandidates.length > 1) {
            assignValue(visibleCandidates[0], payload.country);
          }
          return readValue(target);
        }

        assignValue(
          visibleCandidates[visibleCandidates.length - 1],
          payload.full
        );
        return readValue(visibleCandidates[visibleCandidates.length - 1]);
      },
      { phone: normalizedPhone, country: normalizedCountry, full: fullPhoneValue }
    );

    if (typeof filledValue === "string" && filledValue.trim().length > 0) {
      return filledValue.trim();
    }

    const textboxes = page.getByRole("textbox");
    const count = await textboxes.count();
    if (count > 0) {
      const target = textboxes.nth(count - 1);
      await target.click({ timeout: 2_000 });
      await page.keyboard.press("Control+A");
      await page.keyboard.type(fullPhoneValue);
      return fullPhoneValue;
    }

    return null;
  }

  private async verifyPhoneFieldValue(
    phoneNumber: string
  ): Promise<{ ok: boolean; observed: string[] }> {
    const page = this.requirePage();
    const expectedDigits = phoneDigitsOnly(phoneNumber);
    return page.evaluate((digits) => {
      const inputs = Array.from(document.querySelectorAll<HTMLInputElement>("input"));
      const visibleInputs = inputs.filter((input) => {
        const style = window.getComputedStyle(input);
        return (
          style.display !== "none" &&
          style.visibility !== "hidden" &&
          input.getBoundingClientRect().width > 0 &&
          input.getBoundingClientRect().height > 0
        );
      });

      const observed = visibleInputs.map((input) => input.value ?? "");
      const matches = observed.some((value) => {
        const valueDigits = value.replace(/\D/g, "");
        return valueDigits.endsWith(digits);
      });
      return { ok: matches, observed };
    }, expectedDigits);
  }

  private async forceTypePhoneField(
    phoneNumber: string,
    countryCode?: string
  ): Promise<string | null> {
    const page = this.requirePage();
    const fullPhoneValue = buildFullPhoneValue(phoneNumber, countryCode);

    const candidateLocator = page.locator(
      "input[type='tel'], input[inputmode='numeric'], input[aria-label*='numero' i], input[aria-label*='phone' i], input"
    );
    const count = await candidateLocator.count();
    for (let i = 0; i < count; i += 1) {
      const candidate = candidateLocator.nth(i);
      if (!(await candidate.isVisible().catch(() => false))) {
        continue;
      }
      try {
        await candidate.click({ timeout: 2_000, force: true });
        await page.keyboard.press("Control+A").catch(() => undefined);
        await page.keyboard.press("Meta+A").catch(() => undefined);
        await page.keyboard.press("Backspace").catch(() => undefined);
        await page.keyboard.type(fullPhoneValue, { delay: PHONE_TYPE_DELAY_MS });
        return fullPhoneValue;
      } catch {

      }
    }

    const textboxes = page.getByRole("textbox");
    const textboxCount = await textboxes.count();
    for (let i = 0; i < textboxCount; i += 1) {
      const target = textboxes.nth(i);
      if (!(await target.isVisible().catch(() => false))) continue;
      try {
        await target.click({ timeout: 2_000, force: true });
        await page.keyboard.press("Control+A").catch(() => undefined);
        await page.keyboard.press("Meta+A").catch(() => undefined);
        await page.keyboard.press("Backspace").catch(() => undefined);
        await page.keyboard.type(fullPhoneValue, { delay: PHONE_TYPE_DELAY_MS });
        return fullPhoneValue;
      } catch {

      }
    }

    return null;
  }

  async start(options: StartSessionOptions = {}): Promise<LiveSessionInfo> {
    log.info(`[${this.sessionId}] start: mode=${this.mode} locale=${options.locale ?? "default"}`);
    this.snapshotRevision = options.snapshotRevision ?? this.snapshotRevision;

    if (this.mode === "desktop") {
      return this.startDesktopSession(options);
    }
    if (this.mode === "desktop_macos") {
      throw new Error("desktop_macos mode is not yet supported on this MCP server.");
    }
    return this.startWebSession(options);
  }

  private async startWebSession(options: StartSessionOptions): Promise<LiveSessionInfo> {
    const slowMoMs = options.slowMoMs ?? 0;
    const navigationTimeoutMs =
      options.navigationTimeoutMs ?? DEFAULT_NAVIGATION_TIMEOUT_MS;
    const locale = options.locale?.trim() || DEFAULT_WEB_LOCALE;
    const targetUrl = buildWebUrlForLocale(locale);

    log.info(`[${this.sessionId}] startWebSession: locale=${locale} slowMo=${slowMoMs} timeout=${navigationTimeoutMs} persistent=${this.userDataDir ? "yes" : "no"}`);

    if (!this.context) {

      this.cdpPort = await LiveWebSession.findFreeTcpPort();

      const cdpArgs = [
        `--remote-debugging-port=${this.cdpPort}`,
        "--use-fake-device-for-media-stream",
        "--use-fake-ui-for-media-stream",
      ];
      const fakeVideoFile = process.env.WA_FAKE_VIDEO_FILE?.trim();
      if (fakeVideoFile) {
        cdpArgs.push(`--use-file-for-fake-video-capture=${fakeVideoFile}`);
      }
      const fakeAudioFile = process.env.WA_FAKE_AUDIO_FILE?.trim();
      if (fakeAudioFile) {
        cdpArgs.push(`--use-file-for-fake-audio-capture=${fakeAudioFile}`);
      }
      if (this.userDataDir) {
        if (!existsSync(this.userDataDir)) {
          mkdirSync(this.userDataDir, { recursive: true });
        }

        this.context = await chromium.launchPersistentContext(this.userDataDir, {
          headless: false,
          slowMo: slowMoMs,
          viewport: { width: 1920, height: 1080 },
          locale,
          extraHTTPHeaders: {
            "Accept-Language": `${locale},en;q=0.9`,
          },
          args: cdpArgs,
        });
        this.browser = this.context.browser();
        const pages = this.context.pages();
        this.page = pages[0] ?? (await this.context.newPage());
      } else {
        this.browser = await chromium.launch({
          headless: false,
          slowMo: slowMoMs,
          args: cdpArgs,
        });
        this.context = await this.browser.newContext({
          viewport: { width: 1920, height: 1080 },
          locale,
          extraHTTPHeaders: {
            "Accept-Language": `${locale},en;q=0.9`,
          },
        });
        this.page = await this.context.newPage();
      }
      this.startedAt = new Date().toISOString();
      log.info(`[${this.sessionId}] browser ready, page created, cdp=${this.cdpPort}`);
    }

    const page = this.requirePage();
    if (!page.url().startsWith("https://web.whatsapp.com")) {
      await page.goto(targetUrl, {
        waitUntil: "networkidle",
        timeout: navigationTimeoutMs,
      });
      await page.waitForTimeout(2_000);
    } else {
      await page.goto(targetUrl, {
        waitUntil: "networkidle",
        timeout: navigationTimeoutMs,
      });
      await page.bringToFront();
    }

    await this.ensureMcpScriptsInjected();

    this.updateTimestamp();
    return this.info();
  }

  private async startDesktopSession(options: StartSessionOptions): Promise<LiveSessionInfo> {
    const cdpPort = options.desktopCdpPort ?? DEFAULT_DESKTOP_CDP_PORT;
    const cdpUrl = `http://localhost:${cdpPort}`;

    this.cdpPort = cdpPort;
    log.info(`[${this.sessionId}] startDesktopSession: cdpPort=${cdpPort}`);

    if (this.browser) {
      await this.ensureMcpScriptsInjected();
      this.updateTimestamp();
      return this.info();
    }

    const bridge = await CdpBridge.connect("desktop_windows", {
      cdpUrl,
      autoLaunchDesktop: true,
    });

    if (bridge.cdpUrl) {
      const resolvedPort = Number(new URL(bridge.cdpUrl).port);
      if (Number.isFinite(resolvedPort) && resolvedPort > 0) {
        this.cdpPort = resolvedPort;
      }
    }

    this.browser = bridge.browser;
    this.context = bridge.context;
    this.page = bridge.page;
    this.startedAt = new Date().toISOString();

    await this.ensureMcpScriptsInjected();
    this.updateTimestamp();
    return this.info();
  }

  async stop(): Promise<void> {
    log.info(`[${this.sessionId}] stop: mode=${this.mode}`);
    await this.networkCapture.stop();
    await this.debuggerBridge.detach();
    if (this.mode === "desktop") {
      if (this.browser) {
        await this.browser.close().catch(() => undefined);
      }
    } else {
      if (this.context) {
        await this.context.close().catch(() => undefined);
      }
      if (this.browser) {
        await this.browser.close().catch(() => undefined);
      }
    }
    this.browser = null;
    this.context = null;
    this.page = null;
    this.pairingCode = null;
    this.startedAt = null;
    this.lastAuthState = "unknown";
    this.identity = null;
    this.linkedAt = null;
    this.updateTimestamp();
    log.info(`[${this.sessionId}] stopped, resources released`);
  }

  async wipeUserDataDir(): Promise<void> {
    if (!this.userDataDir) return;
    try {
      await rm(this.userDataDir, { recursive: true, force: true });
      log.info(`[${this.sessionId}] wiped user-data-dir ${this.userDataDir}`);
    } catch (error) {
      log.warn(
        `[${this.sessionId}] failed to wipe user-data-dir: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  async info(currentSnapshotRevision?: string | null): Promise<LiveSessionInfo> {
    const effectiveSnapshotRevision = currentSnapshotRevision !== undefined
      ? currentSnapshotRevision
      : this.snapshotRevision;
    const isRunning = this.page !== null;
    if (!isRunning) {
      return {
        sessionId: this.sessionId,
        running: false,
        mode: this.mode,
        currentUrl: null,
        revision: null,
        snapshotRevision: effectiveSnapshotRevision,
        snapshotMatches: null,
        authState: "unknown",
        pairingCode: null,
        startedAt: this.startedAt,
        updatedAt: this.updatedAt,
        identity: null,
        ephemeral: this.ephemeral,
        userDataDir: this.userDataDir,
      };
    }

    const revision = await this.readRevision();
    const currentUrl = this.page?.url() ?? null;

    const auth = await this.detectAuthAndCode();
    if (auth.pairingCode) {
      this.pairingCode = normalizePairingCode(auth.pairingCode);
    }

    if (auth.authState === "logged_in") {
      if (this.lastAuthState !== "logged_in" || this.identity === null) {
        this.linkedAt = this.linkedAt ?? new Date().toISOString();
        this.identity = await extractIdentity(
          this.requirePage(),
          this.mode,
          this.linkedAt
        );
      }
    } else {
      this.identity = null;
      this.linkedAt = null;
    }
    this.lastAuthState = auth.authState;

    const snapshotMatches =
      revision && effectiveSnapshotRevision
        ? revision === effectiveSnapshotRevision
        : null;

    this.updateTimestamp();
    return {
      sessionId: this.sessionId,
      running: true,
      mode: this.mode,
      currentUrl,
      revision,
      snapshotRevision: effectiveSnapshotRevision,
      snapshotMatches,
      authState: auth.authState,
      pairingCode: this.pairingCode,
      startedAt: this.startedAt,
      updatedAt: this.updatedAt,
      identity: this.identity,
      ephemeral: this.ephemeral,
      userDataDir: this.userDataDir,
    };
  }

  async ensureSnapshotMatches(snapshotRevision: string): Promise<{
    matches: boolean;
    status: LiveSessionInfo;
  }> {
    const status = await this.info(snapshotRevision);
    return {
      matches: status.snapshotMatches === true,
      status,
    };
  }

  async beginPhoneNumberLogin(
    phoneNumber: string,
    options: PhoneLoginOptions = {}
  ): Promise<PhoneLoginResult> {
    if (!this.isRunning()) {
      await this.start();
    }
    const details: string[] = [];
    const currentStatus = await this.info();
    if (currentStatus.authState === "logged_in") {
      details.push("Already logged in.");
      return {
        status: currentStatus,
        pairingCode: null,
        blockedReason: null,
        details,
      };
    }
    if (currentStatus.authState === "rate_limited") {
      details.push("WhatsApp Web is rate-limited (too many attempts).");
      return {
        status: currentStatus,
        pairingCode: null,
        blockedReason: "too_many_attempts",
        details,
      };
    }

    let clickedPhoneMode = await this.clickByText([
      "log in with phone number",
      "link with phone number",
      "phone number",
    ]);
    if (!clickedPhoneMode) {
      const page = this.requirePage();
      const phoneButton = page.getByRole("button", {
        name: /phone number/i,
      });
      const count = await phoneButton.count();
      if (count > 0) {
        await phoneButton.first().click({ force: true, timeout: 3_000 });
        clickedPhoneMode = true;
      }
    }
    if (clickedPhoneMode) {
      details.push("Phone-number login mode selected.");
      await sleep(1_000);
    } else {
      details.push("Phone-number mode button not found (continuing).");
    }

    const filledPhoneValue = await this.fillPhoneFields(phoneNumber, options.countryCode);
    if (!filledPhoneValue) {
      throw new Error("Could not find a phone number input on the login screen.");
    }
    await sleep(500);
    let verification = await this.verifyPhoneFieldValue(phoneNumber);
    if (!verification.ok) {
      const forcedValue = await this.forceTypePhoneField(phoneNumber, options.countryCode);
      if (forcedValue) {
        await sleep(500);
        verification = await this.verifyPhoneFieldValue(phoneNumber);
        if (verification.ok) {
          details.push(`Phone number retyped with keyboard (${forcedValue}).`);
        }
      }
    }
    if (!verification.ok) {
      throw new Error(
        `Phone number verification failed before submit. Observed inputs: ${verification.observed.join(" | ")}`
      );
    }
    await sleep(700);
    const postSettleVerification = await this.verifyPhoneFieldValue(phoneNumber);
    if (!postSettleVerification.ok) {
      throw new Error(
        `Phone number changed before submit. Observed inputs: ${postSettleVerification.observed.join(" | ")}`
      );
    }
    details.push(`Phone number filled (${filledPhoneValue}).`);

    const submitted = await this.clickByText([
      "next",
      "continue",
      "confirm",
    ]);
    if (submitted) {
      details.push("Submitted phone number.");
    } else {
      details.push("Submit button not found. Manual submit may be required.");
    }

    const pairingCode = await this.waitForPairingCode(
      options.waitForPairingCodeTimeoutMs ?? DEFAULT_PAIRING_CODE_TIMEOUT_MS
    );
    if (pairingCode) {
      details.push("Pairing code retrieved from web app.");
    } else {
      details.push("Pairing code not found yet.");
    }

    const status = await this.info();
    const blockedReason =
      status.authState === "rate_limited" ? "too_many_attempts" : null;
    if (blockedReason) {
      details.push("WhatsApp Web reports too many attempts. Wait before retrying.");
    }
    return {
      status,
      pairingCode,
      blockedReason,
      details,
    };
  }

  async waitForPairingCode(timeoutMs: number = DEFAULT_PAIRING_CODE_TIMEOUT_MS): Promise<string | null> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const status = await this.info();
      if (status.pairingCode) return status.pairingCode;
      if (status.authState === "rate_limited") return null;
      await sleep(1_000);
    }
    return null;
  }

  async waitForLogin(timeoutMs: number = 120_000): Promise<WaitForLoginResult> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const status = await this.info();
      if (status.authState === "logged_in") {
        return { success: true, status };
      }
      await sleep(1_000);
    }
    return { success: false, status: await this.info() };
  }

  async listDebuggerScripts(filter?: string, limit: number = 200): Promise<DebugScriptInfo[]> {
    return this.debuggerBridge.listScripts(filter, limit);
  }

  async evaluate(expression: string, awaitPromise: boolean = true, target: string = "page") {
    return this.debuggerBridge.evaluate(expression, awaitPromise, target);
  }

  getCdpPort(): number | null {
    return this.cdpPort;
  }

  private static async findFreeTcpPort(): Promise<number> {
    return new Promise<number>((resolve, reject) => {
      const server = createServer();
      server.unref();
      server.on("error", reject);
      server.listen(0, "127.0.0.1", () => {
        const address = server.address();
        if (!address || typeof address === "string") {
          server.close();
          reject(new Error("Could not allocate a free TCP port"));
          return;
        }
        const port = address.port;
        server.close(() => resolve(port));
      });
    });
  }

  async setBreakpointByUrl(
    url: string,
    lineNumberOneBased: number,
    columnNumberOneBased: number = 1,
    condition?: string
  ): Promise<SetBreakpointResult> {
    return this.debuggerBridge.setBreakpointByUrl(
      url,
      lineNumberOneBased,
      columnNumberOneBased,
      condition
    );
  }

  async setBreakpointByScriptId(
    scriptId: string,
    lineNumberOneBased: number,
    columnNumberOneBased: number = 1,
    condition?: string
  ): Promise<SetBreakpointResult> {
    return this.debuggerBridge.setBreakpointByScriptId(
      scriptId,
      lineNumberOneBased,
      columnNumberOneBased,
      condition
    );
  }

  async removeBreakpoint(breakpointId: string): Promise<void> {
    await this.debuggerBridge.removeBreakpoint(breakpointId);
  }

  async setWasmBreakpoint(
    url: string,
    byteOffset: number,
    logExpression?: string,
    target?: string,
    block?: boolean
  ): Promise<SetWasmBreakpointResult> {
    return this.debuggerBridge.setWasmBreakpoint(url, byteOffset, logExpression, target, block);
  }

  getLogpointCaptures(options: { id?: string; clear?: boolean } = {}) {
    return this.debuggerBridge.getLogpointCaptures(options);
  }

  async setInitScript(script: string | null, target: string = "workers"): Promise<void> {
    return this.debuggerBridge.setInitScript(script, target);
  }

  async readWasmMemory(callFrameId: string, addr: number, len: number) {
    return this.debuggerBridge.readWasmMemory(callFrameId, addr, len);
  }

  async evaluateOnCallFrame(callFrameId: string, expression: string) {
    return this.debuggerBridge.evaluateOnCallFrame(callFrameId, expression);
  }

  async serveWasmReplacement(urlSubstring: string, filePath: string): Promise<void> {
    await this.debuggerBridge.serveReplacement(urlSubstring, filePath);
  }

  async clearWasmReplacements(): Promise<void> {
    await this.debuggerBridge.clearReplacements();
  }

  async debuggerCommand(command: DebugCommand): Promise<void> {
    await this.debuggerBridge.command(command);
  }

  async queryStanzaNodes(query: Record<string, unknown> = {}): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.stanzaLogger as BrowserStanzaLogger | undefined;
      if (!logger) throw new Error("stanzaLogger is not available in the page.");
      const queryFn = typeof logger.query === "function" ? logger.query : logger.getEvents;
      if (typeof queryFn !== "function") {
        throw new Error("stanzaLogger query method is not available.");
      }
      return queryFn(payload);
    }, query);
  }

  async clearStanzaNodes(): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.stanzaLogger as BrowserStanzaLogger | undefined;
      if (!logger || typeof logger.clearEvents !== "function") {
        throw new Error("stanzaLogger clearEvents method is not available.");
      }
      return logger.clearEvents();
    });
  }

  async clearStanzaHistory(): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.stanzaLogger as BrowserStanzaLogger | undefined;
      if (!logger) throw new Error("stanzaLogger is not available in the page.");
      if (typeof logger.clearHistory === "function") {
        return logger.clearHistory();
      }
      if (typeof logger.clearEvents === "function") {
        return logger.clearEvents();
      }
      throw new Error("stanzaLogger clearHistory method is not available.");
    });
  }

  async sendStanzaNode(node: Record<string, unknown>): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.stanzaLogger as BrowserStanzaLogger | undefined;
      if (!logger || typeof logger.sendNode !== "function") {
        throw new Error("stanzaLogger sendNode method is not available.");
      }
      return logger.sendNode(payload);
    }, node);
  }

  async getWamEvents(query: Record<string, unknown> = {}): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.wamLogger as BrowserWamLogger | undefined;
      if (!logger) throw new Error("wamLogger is not available in the page.");
      const queryFn = typeof logger.query === "function" ? logger.query : logger.getEvents;
      if (typeof queryFn !== "function") {
        throw new Error("wamLogger query method is not available.");
      }
      return queryFn(payload);
    }, query);
  }

  async clearWamEvents(): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.wamLogger as BrowserWamLogger | undefined;
      if (!logger || typeof logger.clearEvents !== "function") {
        throw new Error("wamLogger clearEvents method is not available.");
      }
      return logger.clearEvents();
    });
  }

  async clearWamHistory(): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.wamLogger as BrowserWamLogger | undefined;
      if (!logger) throw new Error("wamLogger is not available in the page.");
      if (typeof logger.clearHistory === "function") {
        return logger.clearHistory();
      }
      if (typeof logger.clearEvents === "function") {
        return logger.clearEvents();
      }
      throw new Error("wamLogger clearHistory method is not available.");
    });
  }

  async sendCustomWamEvent(
    name: string,
    props: Record<string, unknown> = {},
    flush: boolean = false
  ): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(async (payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.wamLogger as BrowserWamLogger | undefined;
      if (!logger || typeof logger.sendCustomEvent !== "function") {
        throw new Error("wamLogger sendCustomEvent method is not available.");
      }
      return logger.sendCustomEvent(payload);
    }, { name, props, flush });
  }

  async getWamEventDefinitions(query: Record<string, unknown> = {}): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const logger = root.wamLogger as BrowserWamLogger | undefined;
      if (!logger) throw new Error("wamLogger is not available in the page.");
      const queryFn =
        typeof logger.queryDefinitions === "function"
          ? logger.queryDefinitions
          : logger.getEventDefinitions;
      if (typeof queryFn !== "function") {
        throw new Error("wamLogger definitions method is not available.");
      }
      return queryFn(payload);
    }, query);
  }

  async queryAbProps(query: Record<string, unknown> = {}): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api) throw new Error("abProps is not available in the page.");
      if (typeof api.query === "function") {
        return api.query(payload);
      }
      const filter =
        payload && typeof payload.filter === "string"
          ? payload.filter
          : undefined;
      const diffOnly = payload && payload.diffOnly === true;
      if (diffOnly) {
        if (typeof api.diff !== "function") {
          throw new Error("abProps diff method is not available.");
        }
        return api.diff(filter);
      }
      if (typeof api.list !== "function") {
        throw new Error("abProps list method is not available.");
      }
      return api.list(filter);
    }, query);
  }

  async getAbPropDefinitions(filter?: string): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((filterValue) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api || typeof api.definitions !== "function") {
        throw new Error("abProps definitions method is not available.");
      }
      return api.definitions(filterValue);
    }, filter);
  }

  async getAbProp(name: string): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((propName) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api || typeof api.get !== "function") {
        throw new Error("abProps get method is not available.");
      }
      return api.get(propName);
    }, name);
  }

  async setAbProp(name: string, value: unknown): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((payload) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api || typeof api.set !== "function") {
        throw new Error("abProps set method is not available.");
      }
      return api.set(payload.name, payload.value);
    }, { name, value });
  }

  async resetAbProp(name: string): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate((propName) => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api || typeof api.reset !== "function") {
        throw new Error("abProps reset method is not available.");
      }
      return api.reset(propName);
    }, name);
  }

  async resetAllAbProps(): Promise<unknown> {
    await this.ensureMcpScriptsInjected();
    return this.bridgeEvaluate(() => {
      const root = globalThis as unknown as Record<string, unknown>;
      const api = root.abProps as BrowserAbProps | undefined;
      if (!api || typeof api.resetAll !== "function") {
        throw new Error("abProps resetAll method is not available.");
      }
      return api.resetAll();
    });
  }

  async startNetworkCapture(): Promise<NetworkCaptureState> {
    return this.networkCapture.start();
  }

  async stopNetworkCapture(): Promise<void> {
    await this.networkCapture.stop();
  }

  getNetworkCaptureState(): NetworkCaptureState {
    return this.networkCapture.getState();
  }

  queryNetwork(query: NetworkCaptureQuery = {}): {
    wsFrames: WebSocketFrame[];
    httpRequests: NetworkRequest[];
  } {
    return this.networkCapture.query(query);
  }

  clearNetworkBuffers(): { wsCleared: number; httpCleared: number } {
    return this.networkCapture.clearBuffers();
  }

  clearNetworkHistory(): { wsCleared: number; httpCleared: number } {
    return this.networkCapture.clearHistory();
  }

  async getPausedState(): Promise<unknown> {
    return this.debuggerBridge.getPausedState();
  }
}
