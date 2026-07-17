import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { Readable } from "node:stream";
import { createLogger } from "./logger.js";
import {
  HttpCookie,
  ResponseWrapper,
  type AppDeliveryData,
  type AppDetails,
  type SplitDeliveryData,
} from "../generated/fdfe.js";

const log = createLogger("util:playstore");

const AURORA_DISPENSER_URL = "https://auroraoss.com/api/auth";
const AURORA_USER_AGENT = "com.aurora.store-4.6.1-70";
const FDFE_BASE_URL = "https://android.clients.google.com/fdfe";
const DETAILS_URL = `${FDFE_BASE_URL}/details`;
const PURCHASE_URL = `${FDFE_BASE_URL}/purchase`;
const DELIVERY_URL = `${FDFE_BASE_URL}/delivery`;

const X_DFE_ENCODED_TARGETS =
  "CAESN/qigQYC2AMBFfUbyA7SM5Ij/CvfBoIDgxXrBPsDlQUdMfOLAfoFrwEHgAcBrQYhoA0cGt4MKK0Y2gI";

const FALLBACK_FINSKY_USER_AGENT =
  "Android-Finsky/41.2.29-23 [0] [PR] 639844241 " +
  "(api=3,versionCode=84122900,sdk=34,device=lynx," +
  "hardware=lynx,product=lynx,platformVersionRelease=14," +
  "model=Pixel%207a,buildId=UQ1A.231205.015," +
  "isWideScreen=0,supportedAbis=arm64-v8a;armeabi-v7a;armeabi)";

const LOCALE = "en_US";
const REQUEST_TIMEOUT_MS = 120_000;
const COOKIE_TAG_LENGTH_DELIMITED = 0x0a;

const DEVICE_PROFILE: Record<string, string> = JSON.parse(
  readFileSync(
    resolve(dirname(fileURLToPath(import.meta.url)), "play-store-profile.json"),
    "utf8"
  )
);

interface AuthContext {
  authToken: string;
  gsfId: string;
  dfeCookie: string;
  deviceCheckinConsistencyToken: string | null;
  deviceConfigToken: string | null;
  userAgent: string;
  mccMnc: string | null;
}

export interface AppVersion {
  code: number;
  name: string;
}

export interface SplitInfo {
  name: string;
  url: string;
}

interface Delivery {
  baseUrl: string;
  splits: SplitInfo[];
  cookies: Map<string, string>;
}

export interface DownloadedApk {
  packageName: string;
  baseApk: Readable;
  splits: Map<string, Readable>;
  close(): void;
}

export async function latestVersion(packageName: string): Promise<AppVersion> {
  requireNonBlank(packageName);
  const headers = buildFdfeHeaders(await fetchAnonymousAuth());
  return await fetchVersion(packageName, headers);
}

export async function downloadApk(
  packageName: string,
  versionCode?: number
): Promise<DownloadedApk> {
  requireNonBlank(packageName);
  const headers = buildFdfeHeaders(await fetchAnonymousAuth());
  const code = versionCode ?? (await fetchVersion(packageName, headers)).code;
  return await openDownloadedApk(packageName, code, headers);
}

async function openDownloadedApk(
  packageName: string,
  versionCode: number,
  headers: Record<string, string>
): Promise<DownloadedApk> {
  await acquire(packageName, versionCode, headers);
  const delivery = await fetchDelivery(packageName, versionCode, headers);
  const baseApk = await openDownloadStream(delivery.baseUrl, delivery.cookies);
  const splits = new Map<string, Readable>();
  try {
    for (const split of delivery.splits) {
      splits.set(split.name, await openDownloadStream(split.url, delivery.cookies));
    }
  } catch (err) {
    baseApk.destroy();
    for (const s of splits.values()) s.destroy();
    throw err;
  }
  return {
    packageName,
    baseApk,
    splits,
    close() {
      baseApk.destroy();
      for (const s of splits.values()) s.destroy();
    },
  };
}

async function fetchAnonymousAuth(): Promise<AuthContext> {
  const res = await fetch(AURORA_DISPENSER_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "User-Agent": AURORA_USER_AGENT,
    },
    body: JSON.stringify(DEVICE_PROFILE),
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
  if (res.status !== 200) {
    throw new Error(`Token dispenser returned HTTP ${res.status}`);
  }
  const json = (await res.json()) as Record<string, any> | null;
  if (!json) throw new Error("Token dispenser returned an empty body");
  const authToken = typeof json.authToken === "string" ? json.authToken.trim() : "";
  if (!authToken) throw new Error("Token dispenser returned no authToken");
  const deviceInfoProvider: Record<string, any> = json.deviceInfoProvider ?? {};
  const rawUserAgent =
    typeof deviceInfoProvider.userAgentString === "string"
      ? deviceInfoProvider.userAgentString.trim()
      : "";
  return {
    authToken,
    gsfId: json.gsfId ?? "",
    dfeCookie: json.dfeCookie ?? "",
    deviceCheckinConsistencyToken: json.deviceCheckInConsistencyToken ?? null,
    deviceConfigToken: json.deviceConfigToken ?? null,
    userAgent: rawUserAgent || FALLBACK_FINSKY_USER_AGENT,
    mccMnc: deviceInfoProvider.mccMnc ?? null,
  };
}

function buildFdfeHeaders(auth: AuthContext): Record<string, string> {
  const h: Record<string, string> = {
    Authorization: `Bearer ${auth.authToken}`,
    "User-Agent": auth.userAgent,
    "X-DFE-Device-Id": auth.gsfId,
    "Accept-Language": "en-US",
    "X-DFE-Encoded-Targets": X_DFE_ENCODED_TARGETS,
    "X-DFE-Client-Id": "am-android-google",
    "X-DFE-Network-Type": "4",
    "X-DFE-Content-Filters": "",
    "X-Limit-Ad-Tracking-Enabled": "false",
    "X-Ad-Id": "",
    "X-DFE-UserLanguages": LOCALE,
    "X-DFE-Request-Params": "timeoutMs=4000",
    "X-DFE-Cookie": auth.dfeCookie,
    "X-DFE-No-Prefetch": "true",
  };
  if (auth.deviceCheckinConsistencyToken?.trim()) {
    h["X-DFE-Device-Checkin-Consistency-Token"] = auth.deviceCheckinConsistencyToken;
  }
  if (auth.deviceConfigToken?.trim()) {
    h["X-DFE-Device-Config-Token"] = auth.deviceConfigToken;
  }
  if (auth.mccMnc?.trim()) {
    h["X-DFE-MCCMNC"] = auth.mccMnc;
  }
  return h;
}

async function fetchVersion(
  packageName: string,
  headers: Record<string, string>
): Promise<AppVersion> {
  const body = await sendProtoGet(
    `${DETAILS_URL}?doc=${encodeURIComponent(packageName)}`,
    headers
  );
  const wrapper = decodeWrapper(body, "details", packageName);
  const details: AppDetails | undefined =
    wrapper.payload?.detailsResponse?.docV2?.docDetails?.appDetails;
  if (!details) {
    throw new Error(`Details response missing AppDetails for '${packageName}'`);
  }
  if (!details.versionCode) {
    throw new Error(`Details response missing versionCode for '${packageName}'`);
  }
  return { code: details.versionCode, name: details.versionName ?? "" };
}

async function acquire(
  packageName: string,
  versionCode: number,
  headers: Record<string, string>
): Promise<void> {
  const body = `doc=${encodeURIComponent(packageName)}&ot=1&vc=${versionCode}`;
  try {
    await fetch(PURCHASE_URL, {
      method: "POST",
      headers: { ...headers, "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch (err) {
    log.debug(`acquire: swallowed error for ${packageName}: ${(err as Error).message}`);
  }
}

async function fetchDelivery(
  packageName: string,
  versionCode: number,
  headers: Record<string, string>
): Promise<Delivery> {
  const url = `${DELIVERY_URL}?doc=${encodeURIComponent(packageName)}&ot=1&vc=${versionCode}`;
  const body = await sendProtoGet(url, headers);
  const wrapper = decodeWrapper(body, "delivery", packageName);
  const data: AppDeliveryData | undefined =
    wrapper.payload?.deliveryResponse?.appDeliveryData;
  if (!data) {
    throw new Error(`Delivery response missing AppDeliveryData for '${packageName}'`);
  }
  if (!data.downloadUrl) {
    throw new Error(
      `No download URL for '${packageName}'; app may require purchase or be unavailable`
    );
  }
  const splits: SplitInfo[] = [];
  for (const split of data.splits ?? ([] as SplitDeliveryData[])) {
    if (!split.downloadUrl) continue;
    const name = split.name?.trim() ? split.name : `split-${splits.length}`;
    splits.push({ name, url: split.downloadUrl });
  }
  const cookies = new Map<string, string>();
  for (const entry of data.field4Entries ?? []) {
    if (entry.length === 0 || entry[0] !== COOKIE_TAG_LENGTH_DELIMITED) continue;
    const cookie = decodeCookie(entry);
    if (cookie && cookie.name && !cookies.has(cookie.name)) {
      cookies.set(cookie.name, cookie.value);
    }
  }
  return { baseUrl: data.downloadUrl, splits, cookies };
}

function decodeCookie(bytes: Uint8Array): HttpCookie | null {
  try {
    return HttpCookie.decode(bytes);
  } catch {
    return null;
  }
}

function decodeWrapper(
  buf: Uint8Array,
  endpoint: string,
  packageName: string
): ResponseWrapper {
  try {
    return ResponseWrapper.decode(buf);
  } catch (err) {
    throw new Error(
      `Failed to decode ${endpoint} response for '${packageName}': ${(err as Error).message}`
    );
  }
}

async function openDownloadStream(
  downloadUrl: string,
  cookies: Map<string, string>
): Promise<Readable> {
  const headers: Record<string, string> = {};
  if (cookies.size > 0) {
    headers.Cookie = Array.from(cookies, ([k, v]) => `${k}=${v}`).join("; ");
  }
  const res = await fetch(downloadUrl, {
    method: "GET",
    headers,
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    redirect: "follow",
  });
  if (!res.ok || !res.body) {
    throw new Error(`APK download failed with HTTP ${res.status}`);
  }
  return Readable.fromWeb(res.body as any);
}

async function sendProtoGet(
  url: string,
  headers: Record<string, string>
): Promise<Uint8Array> {
  const res = await fetch(url, {
    method: "GET",
    headers: {
      ...headers,
      "Content-Type": "application/x-protobuf",
      Accept: "application/x-protobuf",
    },
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    redirect: "follow",
  });
  if (res.status !== 200) {
    throw new Error(`GET ${url} returned HTTP ${res.status}`);
  }
  return new Uint8Array(await res.arrayBuffer());
}

function requireNonBlank(value: string | null | undefined): asserts value is string {
  if (!value || !value.trim()) {
    throw new Error("packageName must not be blank");
  }
}
