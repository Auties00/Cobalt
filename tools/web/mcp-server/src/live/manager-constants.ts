import { resolve as pathResolve } from "node:path";

export const WHATSAPP_WEB_URL = "https://web.whatsapp.com/";
export const DEFAULT_NAVIGATION_TIMEOUT_MS = 60_000;
export const DEFAULT_PAIRING_CODE_TIMEOUT_MS = 45_000;
export const DEFAULT_WEB_LOCALE = process.env.WEB_MCP_WEB_LOCALE ?? "en-US";
export const PHONE_TYPE_DELAY_MS = 85;
export const DEFAULT_DESKTOP_CDP_PORT = 47832;

export const MAX_CONCURRENT_SESSIONS = (() => {
  const raw = process.env.WEB_MCP_MAX_SESSIONS;
  if (!raw) return 8;
  const parsed = parseInt(raw, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 8;
})();

export const DEFAULT_USER_DATA_DIR_ROOT = pathResolve(
  process.env.WEB_MCP_SESSIONS_DIR ?? "./data/sessions"
);

export const DEFAULT_SESSION_ID = "primary";

export const MAX_CONCURRENT_EMULATORS = (() => {
  const raw = process.env.WEB_MCP_MAX_EMULATORS;
  if (!raw) return 8;
  const parsed = parseInt(raw, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 8;
})();

export const DEFAULT_EMULATOR_STATE_DIR = pathResolve(
  process.env.WEB_MCP_EMULATORS_DIR ?? "./data/emulators"
);

export const DEFAULT_APK_CACHE_DIR = pathResolve(
  process.env.WEB_MCP_APK_CACHE ?? "./data/apks"
);

export const DEFAULT_AVD_DEVICE = process.env.WEB_MCP_AVD_DEVICE ?? "pixel_7_pro";

export const DEFAULT_AVD_SYSTEM_IMAGE =
  process.env.WEB_MCP_AVD_SYSTEM_IMAGE ??
  "system-images;android-33;google_apis_playstore;x86_64";

export const WA_PACKAGE_PERSONAL = "com.whatsapp";
export const WA_PACKAGE_BUSINESS = "com.whatsapp.w4b";

export const TEXTVERIFIED_BASE_URL =
  process.env.TEXTVERIFIED_BASE_URL ?? "https://www.textverified.com/api/pub/v2";

export const TEXTVERIFIED_SERVICE_PERSONAL =
  process.env.TEXTVERIFIED_SERVICE_WHATSAPP ?? "whatsapp";
export const TEXTVERIFIED_SERVICE_BUSINESS =
  process.env.TEXTVERIFIED_SERVICE_WHATSAPP_BUSINESS ?? "whatsapp";

export const ANDROID_SDK_ROOT =
  process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT ?? null;
