export const DEFAULT_STEP_TIMEOUT_MS = 25_000;
export const DEFAULT_POLL_INTERVAL_MS = 500;
export const FAST_POLL_INTERVAL_MS = 250;

function bothPackages(ids: string[]): string[] {
  const out: string[] = [];
  for (const id of ids) {
    out.push(id);
    if (id.startsWith("com.whatsapp:")) {
      out.push(id.replace("com.whatsapp:", "com.whatsapp.w4b:"));
    }
  }
  return out;
}

export const LINKED_DEVICES_SCREEN_IDS = bothPackages([
  "com.whatsapp:id/linked_device_recycler_view",
  "com.whatsapp:id/link_device_button",
]);

export const LINK_DEVICE_BUTTON_IDS = bothPackages([
  "com.whatsapp:id/link_device_button",
]);

export const DEVICE_NAME_IDS = bothPackages(["com.whatsapp:id/name"]);
export const DEVICE_STATUS_IDS = bothPackages(["com.whatsapp:id/status"]);
export const DEVICE_DETAIL_LOGOUT_IDS = bothPackages([
  "com.whatsapp:id/logout_text",
]);
export const DEVICE_DETAIL_CLOSE_IDS = bothPackages([
  "com.whatsapp:id/close_text",
]);
export const CONFIRM_BUTTON_IDS = bothPackages([
  "android:id/button1",
  "com.whatsapp:id/button1",
]);

export const MAX_LINKED_DEVICES_DIALOG_IDS = bothPackages([
  "com.whatsapp:id/alertTitle",
  "android:id/message",
  "android:id/button1",
]);

export const MAX_DIALOG_OK_BUTTON_IDS = bothPackages([
  "android:id/button1",
  "com.whatsapp:id/button1",
]);

export const ONBOARDING_DISMISS_TEXT_PATTERNS: RegExp[] = [
  /^not now$/i,
  /^skip$/i,
  /^skip for now$/i,
  /^maybe later$/i,
  /^no thanks$/i,
  /^not_now$/i,
];

export const MAX_ONBOARDING_DISMISSALS = 5;

export const OVERFLOW_MENU_BUTTON_IDS = bothPackages([
  "com.whatsapp:id/menuitem_overflow",
  "com.whatsapp:id/menuitem_settings",
  "com.whatsapp:id/menuitem_settings_icon",
]);
export const OVERFLOW_MENU_TITLE_IDS = bothPackages(["com.whatsapp:id/title"]);
export const OVERFLOW_LINKED_DEVICES_INDEX = 3;

export const LINKED_DEVICES_MENU_ENTRY_IDS = bothPackages([
  "com.whatsapp:id/linked_devices",
  "com.whatsapp:id/menuitem_linked_devices",
  "com.whatsapp:id/settings_linked_devices_row",
  "com.whatsapp:id/linked_devices_row",
]);

export const PHONE_LINK_BUTTON_IDS = bothPackages([
  "com.whatsapp:id/link_with_phone_number_button",
  "com.whatsapp:id/link_with_phone_number",
  "com.whatsapp:id/use_phone_number_button",
  "com.whatsapp:id/phone_number_link_button",
  "com.whatsapp:id/pair_with_phone_number",
  "com.whatsapp:id/pair_by_phone_number",
  "com.whatsapp:id/link_by_phone_number_button",
  "com.whatsapp:id/code_linking_button",
  "com.whatsapp:id/bottom_banner",
]);

export const CODE_INPUT_IDS = bothPackages([
  "com.whatsapp:id/code_input",
  "com.whatsapp:id/pairing_code_input",
  "com.whatsapp:id/pairing_code_text_input",
  "com.whatsapp:id/code",
  "com.whatsapp:id/input",
  "com.whatsapp:id/edit_text",
  "com.whatsapp:id/enter_code_boxes",
]);
export const QR_SCANNER_VIEW_IDS = ["com.whatsapp:id/qr_scanner_view"];

export const BIOMETRIC_RESOURCE_ID_PREFIXES = [
  "com.samsung.android.biometrics.app.setting:id/",
  "com.android.systemui:id/biometric_",
];

export const BIOMETRIC_PACKAGE_MARKERS = ["biometric", "biometrics"];
