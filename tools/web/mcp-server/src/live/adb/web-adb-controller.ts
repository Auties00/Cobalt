import {execFile, type ExecFileException} from "node:child_process";
import type {
  AdbDeviceInfo,
  AdbLinkOptions,
  AdbLinkResult,
  ExecResult,
  LinkedDeviceEntry,
  ParsedBounds,
  UiNode,
} from "../../types/live/adb.js";
import {
  BIOMETRIC_PACKAGE_MARKERS,
  BIOMETRIC_RESOURCE_ID_PREFIXES,
  CODE_INPUT_IDS,
  CONFIRM_BUTTON_IDS,
  DEFAULT_POLL_INTERVAL_MS,
  DEFAULT_STEP_TIMEOUT_MS,
  DEVICE_DETAIL_CLOSE_IDS,
  DEVICE_DETAIL_LOGOUT_IDS,
  DEVICE_NAME_IDS,
  DEVICE_STATUS_IDS,
  FAST_POLL_INTERVAL_MS,
  LINK_DEVICE_BUTTON_IDS,
  LINKED_DEVICES_MENU_ENTRY_IDS,
  LINKED_DEVICES_SCREEN_IDS,
  MAX_DIALOG_OK_BUTTON_IDS,
  MAX_LINKED_DEVICES_DIALOG_IDS,
  MAX_ONBOARDING_DISMISSALS,
  ONBOARDING_DISMISS_TEXT_PATTERNS,
  OVERFLOW_LINKED_DEVICES_INDEX,
  OVERFLOW_MENU_BUTTON_IDS,
  OVERFLOW_MENU_TITLE_IDS,
  PHONE_LINK_BUTTON_IDS,
  QR_SCANNER_VIEW_IDS,
} from "./ui-constants.js";
import {
  centerY,
  decodeXmlAttribute,
  hasAllResourceIds,
  hasAnyResourceId,
  isInteractive,
  normalizeAdbTextInput,
  parseBool,
  parseBounds,
} from "./ui-utils.js";
import { sleep } from "../../utils/async.js";

export class WebAdbController {
  private readonly adbPath: string;

  constructor(adbPath: string = process.env.ADB_PATH ?? "adb") {
    this.adbPath = adbPath;
  }

  private runAdb(args: string[], timeoutMs: number = 20_000): Promise<ExecResult> {
    return new Promise((resolve, reject) => {
      execFile(
        this.adbPath,
        args,
        { timeout: timeoutMs, maxBuffer: 10 * 1024 * 1024 },
        (
          error: ExecFileException | null,
          stdout: string,
          stderr: string
        ) => {
          if (error) {
            reject(
              new Error(
                `adb ${args.join(" ")} failed: ${error.message}\n${stdout}\n${stderr}`.trim()
              )
            );
            return;
          }
          resolve({
            stdout: (stdout ?? "").toString(),
            stderr: (stderr ?? "").toString(),
          });
        }
      );
    });
  }

  async emuTap(serial: string, x: number, y: number, dwellMs: number = 100): Promise<void> {
    await this.runAdb([
      "-s",
      serial,
      "emu",
      "event",
      "mouse",
      `${Math.round(x)}`,
      `${Math.round(y)}`,
      "0",
      "1",
    ]);
    await sleep(dwellMs);
    await this.runAdb([
      "-s",
      serial,
      "emu",
      "event",
      "mouse",
      `${Math.round(x)}`,
      `${Math.round(y)}`,
      "0",
      "0",
    ]);
  }

  async emuTapNode(serial: string, stanza: UiNode): Promise<boolean> {
    const bounds = parseBounds(stanza.bounds);
    if (!bounds) return false;
    const x = (bounds.left + bounds.right) / 2;
    const y = (bounds.top + bounds.bottom) / 2;
    await this.emuTap(serial, x, y);
    return true;
  }

  async screenshot(serial: string, localPath: string): Promise<void> {
    const remote = `/sdcard/cobalt-shot-${Date.now()}.png`;
    await this.runAdb(this.withSerial(serial, ["shell", "screencap", "-p", remote]));
    await this.runAdb(["-s", serial, "pull", remote, localPath]);
    await this.runAdb(this.withSerial(serial, ["shell", "rm", "-f", remote])).catch(
      () => undefined
    );
  }

  private withSerial(serial: string | undefined, args: string[]): string[] {
    if (!serial) return args;
    return ["-s", serial, ...args];
  }

  async listDevices(): Promise<AdbDeviceInfo[]> {
    const { stdout } = await this.runAdb(["devices", "-l"]);
    const lines = stdout
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0 && !line.startsWith("List of devices"));

    const devices: AdbDeviceInfo[] = [];
    for (const line of lines) {
      const parts = line.split(/\s+/);
      if (parts.length < 2) continue;
      const serial = parts[0];
      const state = parts[1];
      let model: string | null = null;
      if (state === "device") {
        try {
          const response = await this.runAdb(
            this.withSerial(serial, ["shell", "getprop", "ro.product.model"])
          );
          const parsedModel = response.stdout.trim();
          model = parsedModel.length > 0 ? parsedModel : null;
        } catch {
          model = null;
        }
      }
      devices.push({ serial, state, model });
    }
    return devices;
  }

  async getPrimaryDevice(preferredSerial?: string): Promise<AdbDeviceInfo | null> {
    const devices = await this.listDevices();
    const connected = devices.filter((device) => device.state === "device");
    if (preferredSerial) {
      return connected.find((device) => device.serial === preferredSerial) ?? null;
    }
    return connected[0] ?? null;
  }

  async hasConnectedDevice(preferredSerial?: string): Promise<boolean> {
    const device = await this.getPrimaryDevice(preferredSerial);
    return device != null;
  }

  private parseUiNodes(xml: string): UiNode[] {
    const nodes: UiNode[] = [];
    const nodeMatches = xml.match(/<stanza\b[^>]*?\/?>/g) ?? [];
    for (const nodeMatch of nodeMatches) {
      const attributes = new Map<string, string>();
      const attrRegex = /([A-Za-z_:][A-Za-z0-9_:.-]*)="([^"]*)"/g;
      let attrMatch: RegExpExecArray | null;
      while ((attrMatch = attrRegex.exec(nodeMatch)) != null) {
        attributes.set(attrMatch[1], decodeXmlAttribute(attrMatch[2]));
      }

      const index = Number(attributes.get("index") ?? "0");
      nodes.push({
        index: Number.isFinite(index) ? index : 0,
        text: attributes.get("text") ?? "",
        contentDesc: attributes.get("content-desc") ?? "",
        resourceId: attributes.get("resource-id") ?? "",
        className: attributes.get("class") ?? "",
        packageName: attributes.get("package") ?? "",
        bounds: attributes.get("bounds") ?? "",
        clickable: parseBool(attributes.get("clickable")),
        enabled: parseBool(attributes.get("enabled")),
        focusable: parseBool(attributes.get("focusable")),
        password: parseBool(attributes.get("password")),
      });
    }
    return nodes;
  }

  private async dumpUiHierarchy(serial: string): Promise<UiNode[]> {
    return this.dumpUiHierarchyWithRetry(serial, 3);
  }

  async dumpUiHierarchyWithRetry(
    serial: string,
    attempts: number,
    includeWindows: boolean = false
  ): Promise<UiNode[]> {
    let lastError: unknown;
    for (let i = 0; i < attempts; i++) {
      try {
        const remote = `/sdcard/cobalt-dump-${Date.now()}-${i}.xml`;
        const args = includeWindows
          ? ["shell", "uiautomator", "dump", "--windows", remote]
          : ["shell", "uiautomator", "dump", remote];
        await this.runAdb(this.withSerial(serial, args), 30_000);
        const { stdout } = await this.runAdb(
          this.withSerial(serial, ["shell", "cat", remote]),
          30_000
        );
        await this.runAdb(
          this.withSerial(serial, ["shell", "rm", "-f", remote])
        ).catch(() => undefined);
        if (stdout.trim().length === 0) {
          throw new Error("empty dump");
        }
        return this.parseUiNodes(stdout);
      } catch (error) {
        lastError = error;
        if (i < attempts - 1) {
          await sleep(1000 * (i + 1));
        }
      }
    }
    throw new Error(
      `uiautomator dump failed after ${attempts} attempts: ${
        lastError instanceof Error ? lastError.message : String(lastError)
      }`
    );
  }

  private findNodeByResourceId(nodes: UiNode[], resourceIds: string[]): UiNode | null {
    for (const resourceId of resourceIds) {
      const candidates = nodes.filter((stanza) => stanza.resourceId === resourceId);
      if (!candidates.length) continue;
      const interactive = candidates.find((stanza) => isInteractive(stanza));
      if (interactive) return interactive;
      return candidates[0];
    }
    return null;
  }

  private isOverflowMenuVisible(nodes: UiNode[]): boolean {
    const titleCount = nodes.filter((stanza) =>
      OVERFLOW_MENU_TITLE_IDS.includes(stanza.resourceId)
    ).length;
    return titleCount >= 4;
  }

  private pickOverflowLinkedDevicesNode(nodes: UiNode[]): UiNode | null {
    const titles = nodes
      .filter((stanza) => OVERFLOW_MENU_TITLE_IDS.includes(stanza.resourceId))
      .map((stanza) => ({
        stanza,
        bounds: parseBounds(stanza.bounds),
      }))
      .filter((entry): entry is { stanza: UiNode; bounds: ParsedBounds } => entry.bounds != null)
      .sort((a, b) => a.bounds.top - b.bounds.top)
      .map((entry) => entry.stanza);

    const byText = titles.find((stanza) =>
      /linked\s*device/i.test(stanza.text ?? "")
    );
    if (byText) return byText;

    // Newer WhatsApp builds do not tag the overflow menu rows with a known title
    // resource-id, so the id/index lookups above find nothing. Fall back to matching
    // any tappable node by its visible "Linked devices" label; its bounds land on the row.
    const byAnyText = this.findLinkedDevicesNodeByText(nodes);
    if (byAnyText) return byAnyText;

    if (titles.length <= OVERFLOW_LINKED_DEVICES_INDEX) return null;
    return titles[OVERFLOW_LINKED_DEVICES_INDEX];
  }

  private findLinkedDevicesNodeByText(nodes: UiNode[]): UiNode | null {
    return this.findNodeByText(nodes, /^linked\s*devices?$/i, 24);
  }

  private findNodeByText(
    nodes: UiNode[],
    pattern: RegExp,
    maxLen = 64
  ): UiNode | null {
    const matches = nodes.filter((stanza) => {
      const text = (stanza.text ?? "").trim();
      return (
        text.length > 0 &&
        text.length <= maxLen &&
        pattern.test(text) &&
        parseBounds(stanza.bounds) != null
      );
    });
    if (!matches.length) return null;
    const interactive = matches.find((stanza) => isInteractive(stanza));
    return interactive ?? matches[0];
  }

  async tapByText(
    serial: string,
    pattern: RegExp,
    timeoutMs: number,
    maxLen = 64
  ): Promise<boolean> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const nodes = await this.dumpUiHierarchy(serial);
      const node = this.findNodeByText(nodes, pattern, maxLen);
      if (node && (await this.tapNode(serial, node))) {
        return true;
      }
      await sleep(DEFAULT_POLL_INTERVAL_MS);
    }
    return false;
  }

  private async tapBounds(serial: string, bounds: ParsedBounds): Promise<void> {
    const x = Math.floor((bounds.left + bounds.right) / 2);
    const y = Math.floor((bounds.top + bounds.bottom) / 2);
    await this.runAdb(this.withSerial(serial, ["shell", "input", "tap", `${x}`, `${y}`]));
  }

  private async tapNode(serial: string, stanza: UiNode): Promise<boolean> {
    const bounds = parseBounds(stanza.bounds);
    if (!bounds) return false;
    await this.tapBounds(serial, bounds);
    return true;
  }

  private async waitForUiCondition(
    serial: string,
    timeoutMs: number,
    predicate: (nodes: UiNode[]) => boolean,
    pollIntervalMs: number = DEFAULT_POLL_INTERVAL_MS
  ): Promise<UiNode[] | null> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const nodes = await this.dumpUiHierarchy(serial);
      if (predicate(nodes)) {
        return nodes;
      }
      await sleep(pollIntervalMs);
    }
    return null;
  }

  async tapByResourceIds(
    serial: string,
    resourceIds: string[],
    timeoutMs: number = DEFAULT_STEP_TIMEOUT_MS
  ): Promise<boolean> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const nodes = await this.dumpUiHierarchy(serial);
      const stanza = this.findNodeByResourceId(nodes, resourceIds);
      if (stanza && (await this.tapNode(serial, stanza))) {
        return true;
      }
      await sleep(DEFAULT_POLL_INTERVAL_MS);
    }
    return false;
  }

  private async tapLinkedDevicesMenuEntryById(
    serial: string,
    timeoutMs: number
  ): Promise<boolean> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const nodes = await this.dumpUiHierarchy(serial);
      const byKnownIds = this.findNodeByResourceId(nodes, LINKED_DEVICES_MENU_ENTRY_IDS);
      if (byKnownIds && (await this.tapNode(serial, byKnownIds))) {
        return true;
      }
      const byOverflowIndex = this.pickOverflowLinkedDevicesNode(nodes);
      if (byOverflowIndex && (await this.tapNode(serial, byOverflowIndex))) {
        return true;
      }
      await sleep(DEFAULT_POLL_INTERVAL_MS);
    }
    return false;
  }

  async typeText(serial: string, value: string): Promise<void> {
    const normalized = normalizeAdbTextInput(value);
    if (!normalized) return;
    await this.runAdb(this.withSerial(serial, ["shell", "input", "text", normalized]));
  }

  async pressKeyCode(serial: string, keyCode: number): Promise<void> {
    await this.runAdb(this.withSerial(serial, ["shell", "input", "keyevent", `${keyCode}`]));
  }

  async launchWhatsApp(serial: string, packageName: string): Promise<void> {
    await this.runAdb(
      this.withSerial(serial, [
        "shell",
        "monkey",
        "-p",
        packageName,
        "-c",
        "android.intent.category.LAUNCHER",
        "1",
      ]),
      20_000
    );
    await sleep(1_000);
  }

  private async ensureLinkedDevicesScreen(
    serial: string,
    timeoutMs: number
  ): Promise<{
    ready: boolean;
    details: string[];
    blockedReason: "biometric_required" | "pin_required" | null;
  }> {
    const details: string[] = [];
    let currentNodes = await this.dumpUiHierarchy(serial);
    const initialSecurityPrompt = this.detectSecurityPrompt(currentNodes);
    if (initialSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${initialSecurityPrompt}.`);
      return { ready: false, details, blockedReason: initialSecurityPrompt };
    }
    if (this.isLinkedDevicesScreen(currentNodes)) {
      details.push("Linked Devices screen detected.");
      return { ready: true, details, blockedReason: null };
    }

    currentNodes = await this.dismissOnboardingInterstitials(
      serial,
      details,
      Math.min(8_000, timeoutMs)
    );
    const promptAfterDismiss = this.detectSecurityPrompt(currentNodes);
    if (promptAfterDismiss) {
      details.push(`Blocking security prompt detected: ${promptAfterDismiss}.`);
      return { ready: false, details, blockedReason: promptAfterDismiss };
    }
    if (this.isLinkedDevicesScreen(currentNodes)) {
      details.push("Linked Devices screen detected.");
      return { ready: true, details, blockedReason: null };
    }

    if (this.isOverflowMenuVisible(currentNodes)) {
      details.push("Overflow menu already open.");
    } else {
      const menuOpened = await this.tapByResourceIds(
        serial,
        OVERFLOW_MENU_BUTTON_IDS,
        Math.min(4_000, timeoutMs)
      );
      if (!menuOpened) {
        await this.pressKeyCode(serial, 82);
        await sleep(250);
        details.push("Opened overflow menu using hardware MENU key.");
      } else {
        details.push("Opened overflow menu by resource-id.");
      }
    }

    const linkedEntryTapped = await this.tapLinkedDevicesMenuEntryById(
      serial,
      Math.min(4_000, timeoutMs)
    );

    details.push(
      linkedEntryTapped
        ? "Tapped Linked Devices menu entry."
        : "Could not tap Linked Devices menu entry by resource-id."
    );

    const settledNodes = await this.waitForUiCondition(
      serial,
      timeoutMs,
      (nodes) =>
        this.detectSecurityPrompt(nodes) != null || this.isLinkedDevicesScreen(nodes),
      FAST_POLL_INTERVAL_MS
    );
    if (settledNodes) {
      const securityPrompt = this.detectSecurityPrompt(settledNodes);
      if (securityPrompt) {
        details.push(`Blocking security prompt detected: ${securityPrompt}.`);
        return { ready: false, details, blockedReason: securityPrompt };
      }
      if (this.isLinkedDevicesScreen(settledNodes)) {
        details.push("Linked Devices screen reached.");
        return { ready: true, details, blockedReason: null };
      }
    }

    details.push("Linked Devices screen not detected.");
    return { ready: false, details, blockedReason: null };
  }

  private async dismissOnboardingInterstitials(
    serial: string,
    details: string[],
    timeoutMs: number
  ): Promise<UiNode[]> {
    const deadline = Date.now() + timeoutMs;
    let nodes = await this.dumpUiHierarchy(serial);
    let dismissed = 0;
    while (dismissed < MAX_ONBOARDING_DISMISSALS && Date.now() <= deadline) {
      if (
        this.detectSecurityPrompt(nodes) != null ||
        this.isLinkedDevicesScreen(nodes) ||
        this.isOverflowMenuVisible(nodes)
      ) {
        break;
      }
      const target = this.findDismissableInterstitialNode(nodes);
      if (!target) break;
      const label = (target.text ?? "").trim();
      if (!(await this.tapNode(serial, target))) break;
      dismissed += 1;
      details.push(`Dismissed onboarding interstitial via "${label}".`);
      await sleep(750);
      nodes = await this.dumpUiHierarchy(serial);
    }
    return nodes;
  }

  private findDismissableInterstitialNode(nodes: UiNode[]): UiNode | null {
    let fallback: UiNode | null = null;
    for (const stanza of nodes) {
      const text = (stanza.text ?? "").trim();
      if (!text) continue;
      if (!ONBOARDING_DISMISS_TEXT_PATTERNS.some((pattern) => pattern.test(text))) {
        continue;
      }
      if (parseBounds(stanza.bounds) == null) continue;
      if (isInteractive(stanza)) return stanza;
      if (fallback == null) fallback = stanza;
    }
    return fallback;
  }

  private async isMaxLinkedDevicesDialogVisible(serial: string): Promise<boolean> {
    const nodes = await this.dumpUiHierarchy(serial);
    return hasAllResourceIds(nodes, MAX_LINKED_DEVICES_DIALOG_IDS);
  }

  private async dismissMaxDevicesDialog(serial: string): Promise<void> {
    await this.tapByResourceIds(serial, MAX_DIALOG_OK_BUTTON_IDS, 4_000).catch(
      () => undefined
    );
  }

  private detectSecurityPrompt(nodes: UiNode[]): "biometric_required" | "pin_required" | null {
    const biometricVisible = nodes.some((stanza) => {
      const resourceId = stanza.resourceId.toLowerCase();
      const packageName = stanza.packageName.toLowerCase();
      return (
        BIOMETRIC_RESOURCE_ID_PREFIXES.some((prefix) =>
          resourceId.startsWith(prefix.toLowerCase())
        ) ||
        BIOMETRIC_PACKAGE_MARKERS.some((marker) => packageName.includes(marker))
      );
    });
    if (biometricVisible) return "biometric_required";

    const pinVisible = nodes.some((stanza) => {
      if (
        stanza.packageName !== "com.whatsapp" &&
        stanza.packageName !== "com.whatsapp.w4b"
      )
        return false;
      if (stanza.password) return true;

      const resourceId = stanza.resourceId.toLowerCase();
      return resourceId.includes("pin");
    });
    if (pinVisible) return "pin_required";

    return null;
  }

  private isLinkedDevicesScreen(nodes: UiNode[]): boolean {
    return hasAnyResourceId(nodes, LINKED_DEVICES_SCREEN_IDS);
  }

  private isQrScannerScreen(nodes: UiNode[]): boolean {
    return (
      hasAnyResourceId(nodes, QR_SCANNER_VIEW_IDS) &&
      hasAnyResourceId(nodes, PHONE_LINK_BUTTON_IDS)
    );
  }

  private extractLinkedDeviceEntries(nodes: UiNode[]): LinkedDeviceEntry[] {
    const statusNodes = nodes.filter((stanza) => DEVICE_STATUS_IDS.includes(stanza.resourceId));
    const nameNodes = nodes.filter((stanza) => DEVICE_NAME_IDS.includes(stanza.resourceId));
    const entries: LinkedDeviceEntry[] = [];

    for (const nameNode of nameNodes) {
      if (!nameNode.text.trim()) continue;
      const nameBounds = parseBounds(nameNode.bounds);
      if (!nameBounds) continue;
      const y = centerY(nameBounds);

      const statusNode =
        statusNodes
          .filter((candidate) => {
            const candidateBounds = parseBounds(candidate.bounds);
            if (!candidateBounds) return false;
            const candidateY = centerY(candidateBounds);
            return candidateY >= y - 20 && candidateY <= y + 220;
          })
          .sort((a, b) => {
            const aBounds = parseBounds(a.bounds);
            const bBounds = parseBounds(b.bounds);
            if (!aBounds || !bBounds) return 0;
            return Math.abs(centerY(aBounds) - y) - Math.abs(centerY(bBounds) - y);
          })[0] ?? null;

      entries.push({
        name: nameNode.text.trim(),
        status: statusNode?.text?.trim() ?? "",
        anchorNode: nameNode,
        yCenter: y,
      });
    }

    return entries
      .filter((entry) => !/disconnessione in attesa|pending/i.test(entry.status))
      .sort((a, b) => a.yCenter - b.yCenter);
  }

  private pickOldestLinkedDevice(nodes: UiNode[]): LinkedDeviceEntry | null {
    const entries = this.extractLinkedDeviceEntries(nodes);
    if (!entries.length) return null;
    return entries[entries.length - 1];
  }

  private async waitForLinkedDevicesScreen(
    serial: string,
    timeoutMs: number
  ): Promise<boolean> {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() <= deadline) {
      const nodes = await this.dumpUiHierarchy(serial);
      if (this.isLinkedDevicesScreen(nodes)) return true;
      await sleep(DEFAULT_POLL_INTERVAL_MS);
    }
    return false;
  }

  private async disconnectOldestLinkedDevice(
    serial: string,
    timeoutMs: number
  ): Promise<{ success: boolean; details: string[] }> {
    const details: string[] = [];
    const nodes = await this.dumpUiHierarchy(serial);
    const securityPrompt = this.detectSecurityPrompt(nodes);
    if (securityPrompt) {
      details.push(`Blocking security prompt detected: ${securityPrompt}.`);
      return { success: false, details };
    }
    if (!this.isLinkedDevicesScreen(nodes)) {
      details.push("Linked Devices screen not active for disconnect step.");
      return { success: false, details };
    }

    const oldest = this.pickOldestLinkedDevice(nodes);
    if (!oldest) {
      details.push("No linked device row available to disconnect.");
      return { success: false, details };
    }

    details.push(`Selected bottom list device "${oldest.name}" for disconnect.`);
    const tappedRow = await this.tapNode(serial, oldest.anchorNode);
    if (!tappedRow) {
      details.push("Could not tap selected linked device row.");
      return { success: false, details };
    }

    const detailNodes = await this.waitForUiCondition(
      serial,
      Math.min(6_000, timeoutMs),
      (nodes) =>
        this.detectSecurityPrompt(nodes) != null ||
        hasAnyResourceId(nodes, DEVICE_DETAIL_LOGOUT_IDS),
      FAST_POLL_INTERVAL_MS
    );
    if (!detailNodes) {
      details.push("Device detail sheet did not open.");
      return { success: false, details };
    }
    const detailSecurityPrompt = this.detectSecurityPrompt(detailNodes);
    if (detailSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${detailSecurityPrompt}.`);
      return { success: false, details };
    }

    const tappedLogout = await this.tapByResourceIds(
      serial,
      DEVICE_DETAIL_LOGOUT_IDS,
      Math.min(8_000, timeoutMs)
    );
    if (!tappedLogout) {
      details.push("Disconnect action button not found.");
      await this.tapByResourceIds(serial, DEVICE_DETAIL_CLOSE_IDS, 2_000).catch(
        () => undefined
      );
      return { success: false, details };
    }
    details.push("Disconnect action tapped.");

    const maybeConfirm =
      (await this.waitForUiCondition(
        serial,
        3_500,
        (nodes) =>
          this.detectSecurityPrompt(nodes) != null ||
          hasAnyResourceId(nodes, CONFIRM_BUTTON_IDS) ||
          this.isLinkedDevicesScreen(nodes),
        FAST_POLL_INTERVAL_MS
      )) ?? (await this.dumpUiHierarchy(serial));
    const confirmSecurityPrompt = this.detectSecurityPrompt(maybeConfirm);
    if (confirmSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${confirmSecurityPrompt}.`);
      return { success: false, details };
    }
    if (
      hasAnyResourceId(maybeConfirm, CONFIRM_BUTTON_IDS) &&
      !this.isLinkedDevicesScreen(maybeConfirm)
    ) {
      await this.tapByResourceIds(serial, CONFIRM_BUTTON_IDS, 3_000).catch(
        () => undefined
      );
      details.push("Confirmed disconnect dialog.");
    }

    const returned = await this.waitForLinkedDevicesScreen(
      serial,
      Math.min(15_000, timeoutMs + 5_000)
    );
    if (!returned) {
      await this.pressKeyCode(serial, 4).catch(() => undefined);
      const fallbackReturned = await this.waitForLinkedDevicesScreen(serial, 6_000);
      if (!fallbackReturned) {
        details.push("Did not return to linked devices screen after disconnect.");
        return { success: false, details };
      }
    }

    details.push("Returned to linked devices screen after disconnect.");
    return { success: true, details };
  }

  private async tapLinkDeviceWithRecovery(
    serial: string,
    timeoutMs: number,
    details: string[]
  ): Promise<{ success: boolean; maxDevicesReached: boolean; blockedReason: string | null }> {
    const tappedLink = await this.tapByResourceIds(
      serial,
      LINK_DEVICE_BUTTON_IDS,
      Math.min(10_000, timeoutMs)
    );
    details.push(
      tappedLink
        ? "Tapped Link a device button by resource-id."
        : "Could not tap Link a device button by resource-id."
    );
    if (!tappedLink) {
      return {
        success: false,
        maxDevicesReached: false,
        blockedReason: "link_device_button_not_found",
      };
    }

    const postTapNodes =
      (await this.waitForUiCondition(
        serial,
        Math.min(6_000, timeoutMs),
        (nodes) =>
          this.detectSecurityPrompt(nodes) != null ||
          hasAllResourceIds(nodes, MAX_LINKED_DEVICES_DIALOG_IDS) ||
          hasAnyResourceId(nodes, PHONE_LINK_BUTTON_IDS) ||
          hasAnyResourceId(nodes, CODE_INPUT_IDS),
        FAST_POLL_INTERVAL_MS
      )) ?? (await this.dumpUiHierarchy(serial));
    const postTapSecurityPrompt = this.detectSecurityPrompt(postTapNodes);
    if (postTapSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${postTapSecurityPrompt}.`);
      return {
        success: false,
        maxDevicesReached: false,
        blockedReason: postTapSecurityPrompt,
      };
    }
    if (!hasAllResourceIds(postTapNodes, MAX_LINKED_DEVICES_DIALOG_IDS)) {
      if (hasAnyResourceId(postTapNodes, PHONE_LINK_BUTTON_IDS)) {
        details.push("Phone-number linking options detected after tapping Link a device.");
      }
      return { success: true, maxDevicesReached: false, blockedReason: null };
    }

    details.push("Max linked devices dialog detected.");
    await this.dismissMaxDevicesDialog(serial);

    const disconnected = await this.disconnectOldestLinkedDevice(serial, timeoutMs);
    details.push(...disconnected.details);
    if (!disconnected.success) {
      return {
        success: false,
        maxDevicesReached: true,
        blockedReason: "disconnect_oldest_failed",
      };
    }

    const retriedTap = await this.tapByResourceIds(
      serial,
      LINK_DEVICE_BUTTON_IDS,
      Math.min(10_000, timeoutMs)
    );
    details.push(
      retriedTap
        ? "Retried Link a device after disconnect."
        : "Retry tap on Link a device failed."
    );
    if (!retriedTap) {
      return {
        success: false,
        maxDevicesReached: false,
        blockedReason: "link_device_button_not_found",
      };
    }

    const postRetryNodes =
      (await this.waitForUiCondition(
        serial,
        Math.min(6_000, timeoutMs),
        (nodes) =>
          this.detectSecurityPrompt(nodes) != null ||
          hasAllResourceIds(nodes, MAX_LINKED_DEVICES_DIALOG_IDS) ||
          hasAnyResourceId(nodes, PHONE_LINK_BUTTON_IDS) ||
          hasAnyResourceId(nodes, CODE_INPUT_IDS),
        FAST_POLL_INTERVAL_MS
      )) ?? (await this.dumpUiHierarchy(serial));
    const postRetrySecurityPrompt = this.detectSecurityPrompt(postRetryNodes);
    if (postRetrySecurityPrompt) {
      details.push(`Blocking security prompt detected: ${postRetrySecurityPrompt}.`);
      return {
        success: false,
        maxDevicesReached: false,
        blockedReason: postRetrySecurityPrompt,
      };
    }
    if (hasAllResourceIds(postRetryNodes, MAX_LINKED_DEVICES_DIALOG_IDS)) {
      await this.dismissMaxDevicesDialog(serial);
      details.push("Max linked devices still reached after disconnect+retry.");
      return {
        success: false,
        maxDevicesReached: true,
        blockedReason: "max_linked_devices_reached",
      };
    }

    return { success: true, maxDevicesReached: false, blockedReason: null };
  }

  async linkByPhoneCode(code: string, options: AdbLinkOptions = {}): Promise<AdbLinkResult> {
    const stepTimeoutMs = options.stepTimeoutMs ?? DEFAULT_STEP_TIMEOUT_MS;
    const packageName = options.packageName ?? "com.whatsapp";
    const details: string[] = [];

    const device = await this.getPrimaryDevice(options.serial);
    if (!device) {
      return {
        success: false,
        code,
        device: null,
        maxDevicesReached: false,
        blockedReason: "no_device",
        details: ["No connected Android device found via adb."],
      };
    }

    const serial = device.serial;
    details.push(`Using device ${serial}${device.model ? ` (${device.model})` : ""}`);

    await this.launchWhatsApp(serial, packageName);
    details.push("WhatsApp app launched.");

    const afterLaunchNodes = await this.dumpUiHierarchy(serial);
    const afterLaunchSecurityPrompt = this.detectSecurityPrompt(afterLaunchNodes);
    if (afterLaunchSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${afterLaunchSecurityPrompt}.`);
      return {
        success: false,
        code: code.replace(/[\s-]+/g, ""),
        device,
        maxDevicesReached: false,
        blockedReason: afterLaunchSecurityPrompt,
        details,
      };
    }

    if (hasAnyResourceId(afterLaunchNodes, CODE_INPUT_IDS)) {
      details.push("Code input screen already active.");
    } else if (this.isQrScannerScreen(afterLaunchNodes)) {
      details.push("QR scanner pairing screen already active.");
    } else {
      const linkedScreen = await this.ensureLinkedDevicesScreen(serial, stepTimeoutMs);
      details.push(...linkedScreen.details);
      if (!linkedScreen.ready) {
        if (linkedScreen.blockedReason) {
          return {
            success: false,
            code: code.replace(/[\s-]+/g, ""),
            device,
            maxDevicesReached: false,
            blockedReason: linkedScreen.blockedReason,
            details,
          };
        }
        if (await this.isMaxLinkedDevicesDialogVisible(serial)) {
          await this.dismissMaxDevicesDialog(serial);
          details.push("Max linked devices dialog detected.");
          return {
            success: false,
            code: code.replace(/[\s-]+/g, ""),
            device,
            maxDevicesReached: true,
            blockedReason: "max_linked_devices_reached",
            details,
          };
        }
        return {
          success: false,
          code,
          device,
          maxDevicesReached: false,
          blockedReason: "linked_devices_not_found",
          details,
        };
      }

      const linkRecovery = await this.tapLinkDeviceWithRecovery(
        serial,
        stepTimeoutMs,
        details
      );
      if (!linkRecovery.success) {
        return {
          success: false,
          code,
          device,
          maxDevicesReached: linkRecovery.maxDevicesReached,
          blockedReason: linkRecovery.blockedReason,
          details,
        };
      }
    }

    const nodesAfterLink = await this.dumpUiHierarchy(serial);
    const codeInputAlreadyVisible = hasAnyResourceId(nodesAfterLink, CODE_INPUT_IDS);
    let selectedPhoneNumberLinking = codeInputAlreadyVisible;
    if (!codeInputAlreadyVisible) {
      selectedPhoneNumberLinking = await this.tapByResourceIds(
          serial,
          PHONE_LINK_BUTTON_IDS,
          Math.min(6_000, stepTimeoutMs)
      );
      if (!selectedPhoneNumberLinking) {
        // The "Link with phone number instead" banner is not reliably tagged with a
        // known resource-id on newer builds, so fall back to matching it by text.
        selectedPhoneNumberLinking = await this.tapByText(
          serial,
          /link\s*with\s*phone\s*number/i,
          Math.min(4_000, stepTimeoutMs),
          40
        );
      }
      details.push(
        selectedPhoneNumberLinking
          ? "Selected phone-number linking."
          : "Could not select phone-number linking."
      );
    } else {
      details.push("Pairing code input already visible.");
    }

    const codeInputNodes =
      (await this.waitForUiCondition(
        serial,
        Math.min(6_000, stepTimeoutMs),
        (nodes) =>
          this.detectSecurityPrompt(nodes) != null || hasAnyResourceId(nodes, CODE_INPUT_IDS),
        FAST_POLL_INTERVAL_MS
      )) ?? (await this.dumpUiHierarchy(serial));
    const codeInputSecurityPrompt = this.detectSecurityPrompt(codeInputNodes);
    if (codeInputSecurityPrompt) {
      details.push(`Blocking security prompt detected: ${codeInputSecurityPrompt}.`);
      return {
        success: false,
        code: code.replace(/[\s-]+/g, ""),
        device,
        maxDevicesReached: false,
        blockedReason: codeInputSecurityPrompt,
        details,
      };
    }
    if (!hasAnyResourceId(codeInputNodes, CODE_INPUT_IDS)) {
      details.push("Pairing code input not detected.");
      return {
        success: false,
        code: code.replace(/[\s-]+/g, ""),
        device,
        maxDevicesReached: false,
        blockedReason: "code_input_not_found",
        details,
      };
    }
    if (selectedPhoneNumberLinking) {

      const containerNode =
        codeInputNodes.find(
          (n) => n.resourceId === "com.whatsapp:id/enter_code_boxes"
        ) ?? null;
      const leftmostField = this.findLeftmostEditTextWithin(
        codeInputNodes,
        containerNode
      );
      if (leftmostField) {
        await this.tapNode(serial, leftmostField);
      } else {
        await this.tapByResourceIds(serial, CODE_INPUT_IDS, 2_500).catch(
          () => undefined
        );
      }
    }

    const normalizedCode = code.replace(/[\s-]+/g, "");
    await this.typeText(serial, normalizedCode);
    await this.pressKeyCode(serial, 66);
    details.push("Code typed into Android app.");

    const SCAM_LINK_BUTTON_IDS = [
      "com.whatsapp:id/primary_button",
      "com.whatsapp.w4b:id/primary_button",
    ];
    const confirmNodes = await this.waitForUiCondition(
      serial,
      8_000,
      (nodes) =>
        nodes.some((n) => SCAM_LINK_BUTTON_IDS.includes(n.resourceId)),
      FAST_POLL_INTERVAL_MS
    );
    if (confirmNodes) {
      const linkButton = confirmNodes.find((n) =>
        SCAM_LINK_BUTTON_IDS.includes(n.resourceId)
      );
      if (linkButton && (await this.tapNode(serial, linkButton))) {
        details.push('Confirmed "This may be a scam" — tapped LINK DEVICE.');
      }
    }

    return {
      success: true,
      code: normalizedCode,
      device,
      maxDevicesReached: false,
      blockedReason: null,
      details,
    };
  }

  private findLeftmostEditTextWithin(
    nodes: UiNode[],
    container: UiNode | null
  ): UiNode | null {
    const containerBounds = container ? parseBounds(container.bounds) : null;
    const editTexts = nodes.filter((n) => {
      if (!n.className.endsWith(".EditText")) return false;
      const bounds = parseBounds(n.bounds);
      if (!bounds) return false;
      if (!containerBounds) return true;
      return (
        bounds.left >= containerBounds.left &&
        bounds.right <= containerBounds.right &&
        bounds.top >= containerBounds.top &&
        bounds.bottom <= containerBounds.bottom
      );
    });
    if (editTexts.length === 0) return null;
    editTexts.sort(
      (a, b) => parseBounds(a.bounds)!.left - parseBounds(b.bounds)!.left
    );
    return editTexts[0];
  }
}
