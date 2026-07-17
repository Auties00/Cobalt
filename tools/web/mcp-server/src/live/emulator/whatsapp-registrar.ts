import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { join as pathJoin, resolve as pathResolve } from "node:path";
import { parsePhoneNumberFromString } from "libphonenumber-js";
import { WebAdbController } from "../adb/web-adb-controller.js";
import { createLogger } from "../../utils/logger.js";
import { getSmsVerificationProvider } from "../verification/textverified.js";
import type { SmsVerification } from "../../types/live/verification.js";
import type { ApkVariant, RegisterWhatsAppResult } from "../../types/live/emulator.js";
import type { UiNode } from "../../types/live/adb.js";
import { parseBounds } from "../adb/ui-utils.js";
import { sleep } from "../../utils/async.js";
import { WA_PACKAGE_BUSINESS, WA_PACKAGE_PERSONAL } from "../manager-constants.js";
import {
  BAN_SCREEN_IDS,
  BUSINESS_PROFILE_CONTAINER_IDS,
  CHAT_LIST_IDS,
  CODE_INPUT_IDS,
  DEFAULT_STEP_TIMEOUT_MS,
  DEVICE_CONFIRM_SCREEN_IDS,
  DEVICE_CONFIRM_SECOND_CODE_IDS,
  DIALOG_BUTTON_POSITIVE,
  EULA_ACCEPT_IDS,
  NOTIF_ALLOW_IDS,
  NOTIF_PRIMER_CONTINUE_IDS,
  NOTIF_PRIMER_IDS,
  PERMISSION_CANCEL_IDS,
  PERMISSION_DIALOG_ID,
  PHONE_CC_INPUT_IDS,
  PHONE_ENTRY_TITLE_IDS,
  PHONE_NUMBER_INPUT_IDS,
  PHONE_SUBMIT_BUTTON_IDS,
  PHONE_SUBMIT_IDS,
  PROFILE_NAME_INPUT_IDS,
  TWO_STEP_FORGOT_IDS,
  VERIFICATION_METHOD_CHOOSER_IDS,
  VERIFICATION_METHOD_CONTINUE_ID,
  VERIFICATION_METHOD_ROW_NAME_ID,
  findNodeByShortResourceId,
  findNodesByClass,
  hasAnyShortResourceId,
  shortResourceId,
  sortedVisibleButtons,
  sortedVisibleEditTexts,
} from "./registration-constants.js";

const log = createLogger("live:emulator:registrar");

interface RegisterOptions {
  serial: string;
  variant: ApkVariant;
  phone?: string;
  countryCode?: string;
  stepTimeoutMs?: number;
}

type ScreenId =
  | "Eula"
  | "NotifPermission"
  | "PhoneEntry"
  | "ConfirmDialog"
  | "CodeEntry"
  | "DeviceConfirmMovingPhones"
  | "VerificationMethodChooser"
  | "TwoStepVerification"
  | "PermissionRequest"
  | "BusinessProfileCreation"
  | "ProfileName"
  | "ChatList"
  | "Loading"
  | "GmsPhoneNumberHint"
  | "OnboardingPrompt"
  | "InvalidNumber"
  | "Banned"
  | "RateLimit"
  | "LoginUnavailable"
  | "Unknown";

const KEYBOARD_PACKAGES = [
  "com.google.android.inputmethod.latin",
  "com.android.inputmethod.latin",
];

export class WhatsAppRegistrar {
  constructor(private readonly adb: WebAdbController = new WebAdbController()) {}

  async register(options: RegisterOptions): Promise<RegisterWhatsAppResult> {
    const packageName =
      options.variant === "business" ? WA_PACKAGE_BUSINESS : WA_PACKAGE_PERSONAL;
    const stepTimeout = options.stepTimeoutMs ?? DEFAULT_STEP_TIMEOUT_MS;
    const details: string[] = [];

    log.info(
      `register: serial=${options.serial} variant=${options.variant} phoneProvided=${!!options.phone}`
    );

    await this.adb.launchWhatsApp(options.serial, packageName);
    details.push(`Launched ${packageName}.`);
    await sleep(2_500);

    let phoneNumber = options.phone?.trim() ?? null;
    let verification: SmsVerification | null = null;
    let releasedEarly = false;
    let verificationCode: string | null = null;
    let lastScreen: ScreenId | null = null;
    let successiveUnknowns = 0;
    let codeSubmittedAt: number | null = null;
    const deadline = Date.now() + stepTimeout * 20;

    try {
      while (Date.now() < deadline) {
        const nodes = await this.adb.dumpUiHierarchyWithRetry(options.serial, 4);
        const screen = this.classifyScreen(nodes);

        if (screen !== lastScreen) {
          details.push(`Screen: ${screen}.`);
          log.info(`screen=${screen}`);
          lastScreen = screen;
        }

        try {
        switch (screen) {
          case "Eula":
            await this.handleEula(options.serial, nodes, details);
            break;

          case "NotifPermission":
            await this.handleNotifPermission(options.serial, nodes, details);
            break;

          case "PhoneEntry": {
            if (!phoneNumber) {
              const provider = getSmsVerificationProvider();
              verification = await provider.create({
                variant: options.variant,
                country: options.countryCode,
              });
              phoneNumber = verification.phoneNumber;
              details.push(
                `Rented phone number ${phoneNumber.slice(0, 4)}*** from ${provider.name}.`
              );
            } else {
              details.push(
                `Using caller-supplied phone number ${phoneNumber.slice(0, 4)}***.`
              );
            }
            await this.handlePhoneEntry(options.serial, nodes, phoneNumber, details);
            break;
          }

          case "ConfirmDialog":
            await this.handleConfirmDialog(options.serial, nodes, details);
            break;

          case "CodeEntry": {
            if (codeSubmittedAt) {
              if (Date.now() - codeSubmittedAt > 60_000) {
                throw new Error(
                  "CodeEntry: WA did not advance 60s after code submission — code may have been rejected."
                );
              }
              await sleep(1500);
              continue;
            }
            if (!verification) {
              details.push(
                "Reached code-entry screen without an SMS rental; caller must enter the code via web_live_emulator_enter_verification_code."
              );
              return {
                success: false,
                accountPhone: phoneNumber,
                verificationCode: null,
                details,
              };
            }
            details.push("Waiting for SMS code from provider...");
            verificationCode = await verification.waitForCode();
            details.push("Received SMS verification code.");
            await this.handleCodeEntry(
              options.serial,
              nodes,
              verificationCode,
              details
            );
            codeSubmittedAt = Date.now();
            break;
          }

          case "TwoStepVerification":
            await this.handleTwoStepVerification(options.serial, nodes, details);
            break;

          case "DeviceConfirmMovingPhones":
            await this.handleDeviceConfirmMovingPhones(
              options.serial,
              nodes,
              details
            );
            codeSubmittedAt = null;
            break;

          case "VerificationMethodChooser":
            await this.handleVerificationMethodChooser(
              options.serial,
              nodes,
              details
            );
            codeSubmittedAt = null;
            break;

          case "PermissionRequest":
            await this.handlePermissionRequest(options.serial, nodes, details);
            break;

          case "BusinessProfileCreation":
            await this.handleBusinessProfileCreation(
              options.serial,
              nodes,
              details
            );
            break;

          case "ProfileName":
            await this.handleProfileName(options.serial, nodes, details);
            break;

          case "InvalidNumber":
          case "Banned":
          case "RateLimit":
          case "LoginUnavailable": {
            const message = this.extractErrorText(nodes);
            const artifacts = await this.captureFailureArtifacts(
              options.serial,
              screen.toLowerCase()
            );
            details.push(
              `WA returned error screen ${screen}: "${message}". Forensics: ${artifacts.join(", ")}`
            );
            if (verification) {
              await verification.release();
              releasedEarly = true;
            }
            return {
              success: false,
              accountPhone: phoneNumber,
              verificationCode,
              details,
            };
          }

          case "ChatList":
            details.push("Reached chat list — registration complete.");
            if (verification) {
              await verification.release();
              releasedEarly = true;
            }
            return {
              success: true,
              accountPhone: phoneNumber,
              verificationCode,
              details,
            };

          case "Loading":

            await sleep(1500);
            break;

          case "GmsPhoneNumberHint":
            await this.handleGmsPhoneNumberHint(
              options.serial,
              nodes,
              details
            );
            break;

          case "OnboardingPrompt":
            await this.handleOnboardingPrompt(
              options.serial,
              nodes,
              details
            );
            break;

          case "Unknown":
            successiveUnknowns += 1;
            if (successiveUnknowns >= 3) {
              const artifacts = await this.captureFailureArtifacts(
                options.serial,
                "unknown-screen"
              );
              details.push(
                `Unknown screen after 3 probes; forensics: ${artifacts.join(", ")}`
              );
              return {
                success: false,
                accountPhone: phoneNumber,
                verificationCode,
                details,
              };
            }
            await sleep(1500);
            continue;
        }
        } catch (err) {
          const msg = err instanceof Error ? err.message : String(err);
          log.warn(`handler failed on screen=${screen}: ${msg}`);
          const artifacts = await this.captureFailureArtifacts(
            options.serial,
            `${screen}-handler-error`
          );
          details.push(
            `Handler for ${screen} failed: ${msg}. Forensics: ${artifacts.join(", ")}`
          );
          return {
            success: false,
            accountPhone: phoneNumber,
            verificationCode,
            details,
          };
        }
        successiveUnknowns = 0;
        await sleep(1200);
      }
      details.push("Registration step loop exceeded wall-clock budget.");
      return {
        success: false,
        accountPhone: phoneNumber,
        verificationCode,
        details,
      };
    } finally {
      if (verification && !releasedEarly) {
        await verification.release().catch(() => undefined);
      }
    }
  }

  private classifyScreen(nodes: UiNode[]): ScreenId {
    if (hasAnyShortResourceId(nodes, CHAT_LIST_IDS)) return "ChatList";

    const hasProgress = nodes.some(
      (n) =>
        n.resourceId === "android:id/progress" &&
        n.className.endsWith(".ProgressBar")
    );
    const hasDialogMessage = nodes.some(
      (n) => n.resourceId === "android:id/message"
    );
    if (hasProgress && hasDialogMessage) {
      return "Loading";
    }

    if (
      nodes.some(
        (n) =>
          shortResourceId(n.resourceId) === "onboarding_decline_button"
      )
    ) {
      return "OnboardingPrompt";
    }

    if (
      nodes.some(
        (n) => n.resourceId === "com.google.android.gms:id/phone_number_list"
      )
    ) {
      return "GmsPhoneNumberHint";
    }

    if (hasAnyShortResourceId(nodes, BAN_SCREEN_IDS)) {
      return "Banned";
    }

    if (hasAnyShortResourceId(nodes, TWO_STEP_FORGOT_IDS)) {
      return "TwoStepVerification";
    }

    if (hasAnyShortResourceId(nodes, VERIFICATION_METHOD_CHOOSER_IDS)) {
      return "VerificationMethodChooser";
    }

    if (hasAnyShortResourceId(nodes, DEVICE_CONFIRM_SCREEN_IDS)) {
      return "DeviceConfirmMovingPhones";
    }

    if (hasAnyShortResourceId(nodes, [PERMISSION_DIALOG_ID])) {
      return "PermissionRequest";
    }

    if (hasAnyShortResourceId(nodes, BUSINESS_PROFILE_CONTAINER_IDS)) {
      return "BusinessProfileCreation";
    }

    if (hasAnyShortResourceId(nodes, PROFILE_NAME_INPUT_IDS)) return "ProfileName";

    const errorScreenEarly = this.classifyErrorDialog(nodes);
    if (errorScreenEarly) return errorScreenEarly;

    if (hasAnyShortResourceId(nodes, CODE_INPUT_IDS)) return "CodeEntry";

    if (
      hasAnyShortResourceId(nodes, NOTIF_ALLOW_IDS) ||
      hasAnyShortResourceId(nodes, NOTIF_PRIMER_IDS)
    ) {
      return "NotifPermission";
    }

    if (hasAnyShortResourceId(nodes, EULA_ACCEPT_IDS)) return "Eula";

    if (this.looksLikeConfirmDialog(nodes)) return "ConfirmDialog";

    if (
      hasAnyShortResourceId(nodes, PHONE_ENTRY_TITLE_IDS) ||
      hasAnyShortResourceId(nodes, PHONE_NUMBER_INPUT_IDS) ||
      hasAnyShortResourceId(nodes, PHONE_SUBMIT_IDS)
    ) {
      return "PhoneEntry";
    }

    if (this.looksLikePhoneEntryByShape(nodes)) return "PhoneEntry";

    return "Unknown";
  }

  private classifyErrorDialog(nodes: UiNode[]): ScreenId | null {
    const lower = nodes
      .map((node) => `${node.text} ${node.contentDesc}`.toLowerCase())
      .join(" ");

    if (/login not available|can'?t log you in right now|for security reasons, we can/.test(lower)) {
      return "LoginUnavailable";
    }
    if (
      /banned|blocked from using whatsapp|not allowed to use|can'?t use whatsapp|account can'?t use/.test(
        lower
      )
    ) {
      return "Banned";
    }
    if (
      /try again in \d+|wait \d+ (second|minute|hour)|too many attempts|request a new code in|couldn'?t send an sms|please wait before trying again|wait before requesting/.test(
        lower
      )
    ) {
      return "RateLimit";
    }

    if (
      /(invalid|incorrect) (phone )?number|not a valid (phone )?number|please enter a valid (phone )?number|check (your |the )?(number|country)|country code.*missing/.test(
        lower
      )
    ) {
      return "InvalidNumber";
    }
    return null;
  }

  private extractErrorText(nodes: UiNode[]): string {
    for (const node of nodes) {
      if (node.resourceId === DIALOG_BUTTON_POSITIVE) continue;
      const text = node.text?.trim();
      if (!text) continue;
      if (text.length < 20) continue;
      return text;
    }
    return "(no error text extracted)";
  }

  private looksLikeConfirmDialog(nodes: UiNode[]): boolean {
    return nodes.some((node) => node.resourceId === DIALOG_BUTTON_POSITIVE);
  }

  private looksLikePhoneEntryByShape(nodes: UiNode[]): boolean {
    const edits = sortedVisibleEditTexts(nodes);
    if (edits.length < 2) return false;
    const a = parseBounds(edits[0].bounds);
    const b = parseBounds(edits[1].bounds);
    if (!a || !b) return false;
    const sameRow = Math.abs(a.top - b.top) < 60 && Math.abs(a.bottom - b.bottom) < 60;
    if (!sameRow) return false;
    const buttons = sortedVisibleButtons(nodes);
    return buttons.some((btn) => {
      const bounds = parseBounds(btn.bounds);
      return bounds != null && bounds.top > 2000;
    });
  }

  private async handleEula(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const accept =
      findNodeByShortResourceId(nodes, EULA_ACCEPT_IDS) ??
      this.pickBottomPrimaryButton(nodes);
    if (!accept) {
      throw new Error("EULA: no accept button located by id or shape.");
    }
    const tapped = await this.adb.emuTapNode(serial, accept);
    if (!tapped) throw new Error("EULA: could not compute accept button center.");
    details.push("Tapped EULA accept.");
  }

  private async handleNotifPermission(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const allow = findNodeByShortResourceId(nodes, NOTIF_ALLOW_IDS);
    if (allow) {
      const tapped = await this.adb.emuTapNode(serial, allow);
      if (!tapped) throw new Error("NotifPermission: could not tap Allow.");
      details.push("Allowed notifications permission.");
      return;
    }
    const cont =
      findNodeByShortResourceId(nodes, NOTIF_PRIMER_CONTINUE_IDS) ??
      this.pickBottomPrimaryButton(nodes);
    if (!cont) {
      throw new Error(
        "NotifPermission: neither the system Allow button nor the primer CONTINUE button was found."
      );
    }
    const tapped = await this.adb.emuTapNode(serial, cont);
    if (!tapped) throw new Error("NotifPermission: could not tap primer CONTINUE.");
    details.push("Tapped CONTINUE on the WhatsApp notification primer.");
  }

  private async handlePhoneEntry(
    serial: string,
    nodes: UiNode[],
    phone: string,
    details: string[]
  ): Promise<void> {
    const { countryDigits, subscriberDigits } = splitE164(phone);

    const ccField = findNodeByShortResourceId(nodes, PHONE_CC_INPUT_IDS);
    if (ccField) {
      const focused = ccField.focusable && ccField.enabled;
      const desc = (ccField.contentDesc ?? "").toLowerCase();
      const looksEmpty = !desc.includes("plus") && !desc.includes(countryDigits);
      if (focused && looksEmpty) {
        await this.focusField(serial, ccField);
        await this.typeDigitsViaKeypad(serial, countryDigits);
        details.push(`Typed country code ${countryDigits}.`);
      } else {
        details.push(`Country code assumed to be +${countryDigits} (default).`);
      }
    }

    const phoneField = findNodeByShortResourceId(nodes, PHONE_NUMBER_INPUT_IDS);
    if (!phoneField) {
      throw new Error("PhoneEntry: phone-number input not found.");
    }
    await this.focusField(serial, phoneField);
    await this.clearFocusedFieldViaBackspace(serial, 30);
    await this.typeAndVerifyDigits(serial, subscriberDigits, 2);
    details.push(`Typed ${subscriberDigits.length} subscriber digits.`);

    await sleep(500);

    const enabledNext = await this.waitForEnabledNext(serial, 8_000);
    if (!enabledNext) {
      throw new Error("PhoneEntry: NEXT button did not become enabled.");
    }
    const nextTapped = await this.adb.emuTapNode(serial, enabledNext);
    if (!nextTapped) throw new Error("PhoneEntry: could not tap NEXT.");
    details.push("Tapped NEXT.");
  }

  private async focusField(serial: string, node: UiNode): Promise<void> {
    const tapped = await this.adb.emuTapNode(serial, node);
    if (!tapped) throw new Error(`Could not compute center of ${node.resourceId}.`);
    await sleep(1_000);
  }

  private async typeDigitsViaKeypad(serial: string, digits: string): Promise<void> {
    const keys = await this.discoverDigitKeys(serial);
    for (const ch of digits) {
      const key = keys.get(ch);
      if (!key) {
        throw new Error(
          `Soft keyboard has no discoverable key for "${ch}"; known: ${Array.from(
            keys.keys()
          ).sort().join(",")}.`
        );
      }
      await this.adb.emuTap(serial, key.x, key.y);
      await sleep(150);
    }
  }

  private async typeAndVerifyDigits(
    serial: string,
    digits: string,
    maxAttempts: number
  ): Promise<void> {
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      await this.typeDigitsViaKeypad(serial, digits);
      await sleep(400);
      const typed = await this.readPhoneFieldDigits(serial);
      if (typed === digits) return;
      if (attempt === maxAttempts - 1) {
        throw new Error(
          `Failed to type "${digits}" into phone field after ${maxAttempts} attempts. Last read: "${typed}".`
        );
      }
      log.warn(
        `Retry ${attempt + 1}: expected "${digits}" got "${typed}"; clearing and retyping.`
      );
      await this.clearFocusedFieldViaBackspace(serial, digits.length + 5);
      await sleep(400);
    }
  }

  private async readPhoneFieldDigits(serial: string): Promise<string> {
    const nodes = await this.adb
      .dumpUiHierarchyWithRetry(serial, 3, false)
      .catch(() => [] as UiNode[]);
    const field = findNodeByShortResourceId(nodes, PHONE_NUMBER_INPUT_IDS);
    if (!field) return "";
    return (field.text ?? "").replace(/\D/g, "");
  }

  private async clearFocusedFieldViaBackspace(
    serial: string,
    maxChars: number
  ): Promise<void> {
    const nodes = await this.adb
      .dumpUiHierarchyWithRetry(serial, 3, true)
      .catch(() => [] as UiNode[]);
    const del = nodes.find(
      (node) =>
        KEYBOARD_PACKAGES.includes(node.packageName) &&
        node.contentDesc.trim().toLowerCase() === "delete"
    );
    if (!del) return;
    const bounds = parseBounds(del.bounds);
    if (!bounds) return;
    const x = (bounds.left + bounds.right) / 2;
    const y = (bounds.top + bounds.bottom) / 2;
    for (let i = 0; i < maxChars; i++) {
      await this.adb.emuTap(serial, x, y);
      await sleep(50);
    }
  }

  private async discoverDigitKeys(
    serial: string,
    timeoutMs: number = 10_000
  ): Promise<Map<string, { x: number; y: number }>> {
    const deadline = Date.now() + timeoutMs;
    let lastCount = 0;
    let lastKeys: Map<string, { x: number; y: number }> = new Map();
    while (Date.now() < deadline) {
      const nodes = await this.adb.dumpUiHierarchyWithRetry(serial, 3, true);
      const keys = new Map<string, { x: number; y: number }>();
      for (const node of nodes) {
        if (!KEYBOARD_PACKAGES.includes(node.packageName)) continue;
        const desc = node.contentDesc.trim();
        if (!/^[0-9]$/.test(desc)) continue;
        const bounds = parseBounds(node.bounds);
        if (!bounds) continue;
        const x = (bounds.left + bounds.right) / 2;
        const y = (bounds.top + bounds.bottom) / 2;
        if (!keys.has(desc)) keys.set(desc, { x, y });
      }
      if (keys.size >= 10) return keys;
      lastCount = keys.size;
      lastKeys = keys;
      await sleep(600);
    }
    throw new Error(
      `Soft keyboard not in numeric layout; discovered ${lastCount}/10 digit keys after ${timeoutMs}ms: ${
        Array.from(lastKeys.keys()).sort().join(",") || "(none)"
      }. Ensure the phone EditText is focused before typing.`
    );
  }

  private async waitForEnabledNext(
    serial: string,
    timeoutMs: number
  ): Promise<UiNode | null> {
    const end = Date.now() + timeoutMs;
    while (Date.now() < end) {
      const nodes = await this.adb.dumpUiHierarchyWithRetry(serial, 3).catch(() => null);
      if (nodes) {
        const node = this.findPhoneSubmitButton(nodes);
        if (node && node.enabled) return node;
      }
      await sleep(400);
    }
    return null;
  }

  private findPhoneSubmitButton(nodes: UiNode[]): UiNode | null {
    const inner = findNodeByShortResourceId(nodes, PHONE_SUBMIT_BUTTON_IDS);
    if (inner) return inner;
    return findNodeByShortResourceId(nodes, PHONE_SUBMIT_IDS);
  }

  private async handleConfirmDialog(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const ok = nodes.find((node) => node.resourceId === DIALOG_BUTTON_POSITIVE);
    if (!ok) throw new Error("ConfirmDialog: OK button not found.");
    const tapped = await this.adb.emuTapNode(serial, ok);
    if (!tapped) throw new Error("ConfirmDialog: could not tap OK.");
    details.push("Confirmed phone-number dialog.");
  }

  private async handleCodeEntry(
    serial: string,
    nodes: UiNode[],
    code: string,
    details: string[]
  ): Promise<void> {
    const input = findNodeByShortResourceId(nodes, CODE_INPUT_IDS);
    if (!input) {
      throw new Error("CodeEntry: code input not found.");
    }
    await this.focusField(serial, input);
    await this.typeDigitsViaKeypad(serial, code.replace(/\D/g, ""));
    details.push("Typed verification code.");
    await sleep(2_000);
  }

  private async handleDeviceConfirmMovingPhones(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const button = findNodeByShortResourceId(nodes, DEVICE_CONFIRM_SECOND_CODE_IDS);
    if (!button) {
      throw new Error(
        'DeviceConfirmMovingPhones: "Confirm with another code" link not found.'
      );
    }
    const tapped = await this.adb.emuTapNode(serial, button);
    if (!tapped) {
      throw new Error(
        'DeviceConfirmMovingPhones: could not tap "Confirm with another code".'
      );
    }
    details.push('Tapped "Confirm with another code" to bypass device confirmation.');
  }

  private async handleVerificationMethodChooser(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const nameNodes = nodes.filter(
      (n) => shortResourceId(n.resourceId) === VERIFICATION_METHOD_ROW_NAME_ID
    );

    const smsRow = nameNodes.find((n) => /\bsms\b/i.test(n.text ?? ""));
    if (!smsRow) {
      throw new Error(
        'VerificationMethodChooser: "Receive SMS" option not found among reg_method_name rows.'
      );
    }
    const smsTapped = await this.adb.emuTapNode(serial, smsRow);
    if (!smsTapped) {
      throw new Error('VerificationMethodChooser: could not tap "Receive SMS" row.');
    }
    details.push('Tapped "Receive SMS" verification method.');
    await sleep(500);
    const continueBtn = findNodeByShortResourceId(nodes, [
      VERIFICATION_METHOD_CONTINUE_ID,
    ]);
    if (!continueBtn) {
      throw new Error('VerificationMethodChooser: CONTINUE button not found.');
    }
    const continueTapped = await this.adb.emuTapNode(serial, continueBtn);
    if (!continueTapped) {
      throw new Error('VerificationMethodChooser: could not tap CONTINUE.');
    }
    details.push('Tapped CONTINUE to request an SMS code.');
  }

  private async handleTwoStepVerification(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {

    const confirm = nodes.find((n) => n.resourceId === DIALOG_BUTTON_POSITIVE);
    if (confirm) {
      const tapped = await this.adb.emuTapNode(serial, confirm);
      if (!tapped) throw new Error("TwoStepVerification: could not tap reset confirm.");
      details.push(`Confirmed account reset ("${confirm.text}").`);
      return;
    }
    const forgot = findNodeByShortResourceId(nodes, TWO_STEP_FORGOT_IDS);
    if (!forgot) {
      throw new Error("TwoStepVerification: FORGOT PIN? button not found.");
    }
    const tapped = await this.adb.emuTapNode(serial, forgot);
    if (!tapped) throw new Error("TwoStepVerification: could not tap FORGOT PIN?.");
    details.push("Tapped FORGOT PIN? to invoke account reset.");
  }

  private async handlePermissionRequest(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const cancel = findNodeByShortResourceId(nodes, PERMISSION_CANCEL_IDS);
    if (!cancel) {
      throw new Error("PermissionRequest: cancel button not found.");
    }
    const tapped = await this.adb.emuTapNode(serial, cancel);
    if (!tapped) throw new Error("PermissionRequest: could not tap cancel.");
    const title = nodes.find((n) => shortResourceId(n.resourceId) === "permission_title")?.text;
    details.push(`Declined WA permission prompt${title ? ` "${title}"` : ""}.`);
  }

  private async handleGmsPhoneNumberHint(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const cancel = nodes.find(
      (n) => n.resourceId === "com.google.android.gms:id/cancel"
    );
    if (!cancel) {
      throw new Error(
        "GmsPhoneNumberHint: cancel button (com.google.android.gms:id/cancel) not found."
      );
    }
    const tapped = await this.adb.emuTapNode(serial, cancel);
    if (!tapped) {
      throw new Error("GmsPhoneNumberHint: could not tap cancel.");
    }
    details.push("Dismissed Google Play Services phone-number hint sheet.");
  }

  private async handleOnboardingPrompt(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const decline = findNodeByShortResourceId(nodes, [
      "onboarding_decline_button",
    ]);
    if (!decline) {
      throw new Error(
        "OnboardingPrompt: decline button (onboarding_decline_button) not found."
      );
    }
    const tapped = await this.adb.emuTapNode(serial, decline);
    if (!tapped) {
      throw new Error("OnboardingPrompt: could not tap decline button.");
    }
    const title = nodes.find(
      (n) => shortResourceId(n.resourceId) === "top_container_title"
    )?.text;
    details.push(
      `Declined onboarding prompt${title ? ` "${title}"` : ""}.`
    );
  }

  private async handleBusinessProfileCreation(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {

    const skipNode = nodes.find(
      (n) => n.clickable && n.enabled && /^\s*skip\s*$/i.test(n.text ?? "")
    );
    if (skipNode) {
      const tappedSkip = await this.adb.emuTapNode(serial, skipNode);
      if (!tappedSkip) {
        throw new Error("BusinessProfileCreation/skip: could not tap Skip.");
      }
      details.push("Tapped Skip on optional business-profile sub-step.");
      return;
    }

    const edits = findNodesByClass(nodes, "EditText").filter(
      (n) => parseBounds(n.bounds) != null
    );
    const checkboxes = findNodesByClass(nodes, "CheckBox").filter(
      (n) => parseBounds(n.bounds) != null
    );
    const radios = findNodesByClass(nodes, "RadioButton").filter(
      (n) => parseBounds(n.bounds) != null && n.enabled
    );
    const next = this.pickBottomPrimaryButton(nodes);

    if (edits.length > 0) {
      const input = edits[0];
      await this.focusField(serial, input);
      await this.adb.typeText(serial, "Cobalt Test");
      await sleep(500);
      if (!next) {
        throw new Error(
          "BusinessProfileCreation/name: Next button not found."
        );
      }
      const tapped = await this.adb.emuTapNode(serial, next);
      if (!tapped) {
        throw new Error("BusinessProfileCreation/name: could not tap Next.");
      }
      details.push("Set default business name; tapped Next.");
      return;
    }

    if (radios.length > 0) {

      const alwaysOpenRow = nodes.find((n) =>
        /always\s*open/i.test(n.contentDesc ?? "")
      );
      const target = alwaysOpenRow ?? radios[0];
      const tappedRadio = await this.adb.emuTapNode(serial, target);
      if (!tappedRadio) {
        throw new Error(
          "BusinessProfileCreation/radio: could not tap radio option."
        );
      }

      await sleep(800);
      if (!next) {
        throw new Error(
          "BusinessProfileCreation/radio: Next button not found."
        );
      }
      const tappedNext = await this.adb.emuTapNode(serial, next);
      if (!tappedNext) {
        throw new Error("BusinessProfileCreation/radio: could not tap Next.");
      }
      details.push(
        alwaysOpenRow
          ? 'Selected "Always open" business hours; tapped Next.'
          : "Selected first radio option; tapped Next."
      );
      return;
    }

    if (checkboxes.length > 0) {

      const firstCategory = checkboxes[0];
      const tappedCategory = await this.adb.emuTapNode(serial, firstCategory);
      if (!tappedCategory) {
        throw new Error(
          "BusinessProfileCreation/category: could not tap a category checkbox."
        );
      }
      await sleep(500);
      if (!next) {
        throw new Error(
          "BusinessProfileCreation/category: Next button not found."
        );
      }
      const tappedNext = await this.adb.emuTapNode(serial, next);
      if (!tappedNext) {
        throw new Error(
          "BusinessProfileCreation/category: could not tap Next."
        );
      }
      details.push("Selected first business category; tapped Next.");
      return;
    }

    if (next && next.enabled) {
      const tappedNext = await this.adb.emuTapNode(serial, next);
      if (!tappedNext) {
        throw new Error(
          "BusinessProfileCreation/review: could not tap Next."
        );
      }
      details.push("Accepted default business-profile sub-step; tapped Next.");
      return;
    }

    throw new Error(
      "BusinessProfileCreation: no known sub-step shape (no EditText, no CheckBox, no RadioButton, no enabled Next)."
    );
  }

  private async handleProfileName(
    serial: string,
    nodes: UiNode[],
    details: string[]
  ): Promise<void> {
    const input = findNodeByShortResourceId(nodes, PROFILE_NAME_INPUT_IDS);
    if (!input) {
      throw new Error("ProfileName: name input not found.");
    }
    await this.focusField(serial, input);

    await this.adb.typeText(serial, "Cobalt Test");
    await sleep(500);
    const buttons = sortedVisibleButtons(nodes);
    const next = buttons[buttons.length - 1];
    if (next) {
      await this.adb.emuTapNode(serial, next);
      details.push("Set default profile name; tapped next.");
    } else {
      details.push("Set default profile name; no next button found.");
    }
  }

  private pickBottomPrimaryButton(nodes: UiNode[]): UiNode | null {
    const buttons = sortedVisibleButtons(nodes);
    for (let i = buttons.length - 1; i >= 0; i--) {
      const bounds = parseBounds(buttons[i].bounds);
      if (bounds && bounds.top > 2000) return buttons[i];
    }
    return buttons[buttons.length - 1] ?? null;
  }

  private async captureFailureArtifacts(
    serial: string,
    tag: string
  ): Promise<string[]> {
    const root = pathResolve(
      process.env.WEB_MCP_REGISTRAR_FAILURES_DIR ?? "./data/registrar-failures"
    );
    if (!existsSync(root)) mkdirSync(root, { recursive: true });
    const stamp = new Date().toISOString().replace(/[:.]/g, "-");
    const paths: string[] = [];
    try {
      const nodes = await this.adb.dumpUiHierarchyWithRetry(serial, 2);
      const xml = JSON.stringify(
        nodes.map((n) => ({
          class: n.className,
          rid: n.resourceId,
          text: n.text,
          bounds: n.bounds,
        })),
        null,
        2
      );
      const p = pathJoin(root, `${stamp}-${tag}-nodes.json`);
      writeFileSync(p, xml, "utf-8");
      paths.push(p);
    } catch {

    }
    try {
      const p = pathJoin(root, `${stamp}-${tag}.png`);
      await this.adb.screenshot(serial, p);
      paths.push(p);
    } catch {

    }
    return paths;
  }
}

function splitE164(phone: string): { countryDigits: string; subscriberDigits: string } {
  const parsed = parsePhoneNumberFromString(phone);
  if (!parsed || !parsed.isPossible()) {
    throw new Error(`Invalid phone number "${phone}" — could not parse as E.164.`);
  }
  return {
    countryDigits: parsed.countryCallingCode,
    subscriberDigits: parsed.nationalNumber,
  };
}
