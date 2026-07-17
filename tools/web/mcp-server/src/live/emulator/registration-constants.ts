import type { UiNode } from "../../types/live/adb.js";
import { parseBounds } from "../adb/ui-utils.js";

export const DEFAULT_STEP_TIMEOUT_MS = 45_000;
export const FAST_POLL_MS = 400;

export const EULA_ACCEPT_IDS = ["eula_accept", "agree_button", "accept_button"];

export const NOTIF_ALLOW_IDS = [
  "permission_allow_button",
  "permission_allow_foreground_only_button",
];
export const NOTIF_DENY_IDS = ["permission_deny_button"];

export const NOTIF_PRIMER_IDS = ["notification_primer_description"];
export const NOTIF_PRIMER_CONTINUE_IDS = ["primary_button"];

export const PHONE_COUNTRY_IDS = ["registration_country", "country_code_field"];
export const PHONE_CC_INPUT_IDS = ["registration_cc", "cc", "country_code"];
export const PHONE_NUMBER_INPUT_IDS = [
  "registration_phone",
  "phone_number_input",
  "phone_field",
  "phone",
];
export const PHONE_SUBMIT_IDS = ["registration_submit", "next_btn", "next_button"];
export const PHONE_SUBMIT_BUTTON_IDS = ["button_view"];
export const PHONE_ENTRY_TITLE_IDS = [
  "register_phone_toolbar_title",
  "registration_phone_title",
];

export const DIALOG_BUTTON_POSITIVE = "android:id/button1";
export const DIALOG_BUTTON_NEGATIVE = "android:id/button2";

export const PERMISSION_DIALOG_ID = "permission_request_dialog";
export const PERMISSION_CANCEL_IDS = ["cancel"];
export const PERMISSION_SUBMIT_IDS = ["submit"];

export const CODE_INPUT_IDS = [
  "verify_sms_code_input",
  "code_input",
  "sms_code",
  "verification_code",
  "registration_code",
];

export const PROFILE_NAME_INPUT_IDS = ["registration_name", "profile_name"];

export const TWO_STEP_PIN_IDS = ["code"];
export const TWO_STEP_FORGOT_IDS = ["forgot_pin_button"];

export const DEVICE_CONFIRM_SCREEN_IDS = [
  "device_confirmation_registration_screen_text_layout",
  "device_confirmation_learn_more",
  "device_confirmation_second_code",
  "verify_wa_old_content_title",
  "verify_wa_old_content_subtitle",
  "wa_old_device_icon",
];
export const DEVICE_CONFIRM_SECOND_CODE_IDS = [
  "device_confirmation_second_code",
  "fallback_methods_entry_button",
];

export const VERIFICATION_METHOD_CHOOSER_IDS = [
  "request_otp_content",
  "request_otp_code_bottom_sheet_title",
  "verification_methods_list",
];

export const BAN_SCREEN_IDS = ["ban_info_text_layout"];

export const BUSINESS_PROFILE_CONTAINER_IDS = [
  "profile_creation_fragment_container",
];
export const VERIFICATION_METHOD_ROW_NAME_ID = "reg_method_name";
export const VERIFICATION_METHOD_CONTINUE_ID = "continue_button";

export const CHAT_LIST_IDS = [
  "conversation_list_view_host",
  "conversations_coordinator_layout",
  "conversation_container",
  "bottom_nav",
  "bottom_nav_container",
  "pager_holder",
  "fabText",
  "conversations_row_contact_name",
  "empty_chat_list_home_text",
];

export function shortResourceId(resourceId: string): string {
  const idx = resourceId.lastIndexOf("/");
  return idx >= 0 ? resourceId.slice(idx + 1) : resourceId;
}

export function hasAnyShortResourceId(nodes: UiNode[], ids: string[]): boolean {
  const set = new Set(ids);
  return nodes.some((node) => set.has(shortResourceId(node.resourceId)));
}

export function findNodeByShortResourceId(
  nodes: UiNode[],
  ids: string[]
): UiNode | null {
  const set = new Set(ids);
  let fallback: UiNode | null = null;
  for (const node of nodes) {
    if (!set.has(shortResourceId(node.resourceId))) continue;
    if (node.enabled && node.clickable) return node;
    if (!fallback) fallback = node;
  }
  return fallback;
}

export function findNodesByClass(nodes: UiNode[], simpleName: string): UiNode[] {
  const suffix = `.${simpleName}`;
  return nodes.filter(
    (node) => node.className === simpleName || node.className.endsWith(suffix)
  );
}

export function sortedVisibleButtons(nodes: UiNode[]): UiNode[] {
  return findNodesByClass(nodes, "Button")
    .filter((node) => {
      const bounds = parseBounds(node.bounds);
      return (
        bounds != null &&
        bounds.right > bounds.left &&
        bounds.bottom > bounds.top &&
        node.enabled
      );
    })
    .sort((a, b) => {
      const ab = parseBounds(a.bounds)!;
      const bb = parseBounds(b.bounds)!;
      return ab.top + ab.bottom - (bb.top + bb.bottom);
    });
}

export function sortedVisibleEditTexts(nodes: UiNode[]): UiNode[] {
  return findNodesByClass(nodes, "EditText")
    .filter((node) => parseBounds(node.bounds) != null)
    .sort((a, b) => {
      const ab = parseBounds(a.bounds)!;
      const bb = parseBounds(b.bounds)!;
      const ay = ab.top + ab.bottom;
      const by = bb.top + bb.bottom;
      if (ay !== by) return ay - by;
      return ab.left - bb.left;
    });
}
