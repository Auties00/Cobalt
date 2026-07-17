import { parsePhoneNumberFromString } from "libphonenumber-js";
import {
  TEXTVERIFIED_BASE_URL,
  TEXTVERIFIED_SERVICE_BUSINESS,
  TEXTVERIFIED_SERVICE_PERSONAL,
} from "../manager-constants.js";
import { createLogger } from "../../utils/logger.js";
import { sleep } from "../../utils/async.js";
import type {
  CreateVerificationOptions,
  SmsVerification,
  SmsVerificationProvider,
} from "../../types/live/verification.js";

const log = createLogger("live:verification:textverified");

const POLL_INTERVAL_MS = 4_000;

interface TvBearerResponse {
  token: string;
  expiresIn: number;
  expiresAt: string;
}

interface TvLink {
  href: string;
  method: string;
}

interface TvVerificationDetails {
  id: string;
  state: string;
  number: string;
  totalCost?: number;
}

interface TvSms {
  id: string;
  from: string | null;
  to: string;
  createdAt: string;
  smsContent: string | null;
  parsedCode: string | null;
  encrypted: boolean;
}

interface TvSmsListing {
  data: TvSms[];
  hasNext: boolean;
  hasPrevious: boolean;
  count: number;
  links: unknown;
}

export interface TvService {
  serviceName: string;
  description: string | null;
  capability: string;
}

function extractVerificationIdFromHref(href: string): string {
  const match = href.match(/\/verifications\/([^/?#]+)/);
  if (!match) {
    throw new Error(`textverified: unable to extract verification id from href "${href}".`);
  }
  return match[1];
}

function normalizeToE164(raw: string, defaultCountry: string = "US"): string {
  const trimmed = raw.trim();
  const parsed =
    parsePhoneNumberFromString(trimmed) ??
    parsePhoneNumberFromString(trimmed, defaultCountry as never);
  if (parsed?.isPossible()) {
    return parsed.number;
  }

  return trimmed.startsWith("+") ? trimmed : `+${trimmed.replace(/\D/g, "")}`;
}

export class TextVerifiedProvider implements SmsVerificationProvider {
  readonly name = "textverified";

  private bearer: string | null = null;
  private bearerExpiresAt: number = 0;

  private async authenticate(): Promise<string> {
    const apiKey = (process.env.TEXTVERIFIED_API_KEY ?? "").trim();
    const apiUsername = (process.env.TEXTVERIFIED_API_USERNAME ?? "").trim();
    if (!apiKey) {
      throw new Error(
        "TEXTVERIFIED_API_KEY is not set. Add it to .env or the environment."
      );
    }
    if (!apiUsername) {
      throw new Error(
        "TEXTVERIFIED_API_USERNAME is not set. Add it to .env or the environment."
      );
    }
    if (this.bearer && Date.now() < this.bearerExpiresAt - 60_000) {
      return this.bearer;
    }
    const res = await fetch(`${TEXTVERIFIED_BASE_URL}/auth`, {
      method: "POST",
      headers: {
        "X-API-KEY": apiKey,
        "X-API-USERNAME": apiUsername,
        "Content-Length": "0",
      },
    });
    if (!res.ok) {
      const body = await res.text().catch(() => "");
      throw new Error(`textverified auth failed: HTTP ${res.status} ${body}`);
    }
    const json = (await res.json()) as TvBearerResponse;
    if (!json.token) {
      throw new Error(`textverified auth returned no "token" field: ${JSON.stringify(json)}`);
    }
    this.bearer = json.token;
    this.bearerExpiresAt = new Date(json.expiresAt).getTime();
    log.debug(`authenticated; token expires ${json.expiresAt}`);
    return this.bearer;
  }

  private async callRaw(
    path: string,
    init: RequestInit = {}
  ): Promise<Response> {
    const bearer = await this.authenticate();
    const baseHeaders: Record<string, string> = {
      Authorization: `Bearer ${bearer}`,
    };
    if (init.body !== undefined && init.body !== null) {
      baseHeaders["Content-Type"] = "application/json";
    } else if ((init.method ?? "GET").toUpperCase() === "POST") {
      baseHeaders["Content-Length"] = "0";
    }
    const res = await fetch(`${TEXTVERIFIED_BASE_URL}${path}`, {
      ...init,
      headers: {
        ...baseHeaders,
        ...(init.headers ?? {}),
      },
    });
    if (!res.ok) {
      const body = await res.text().catch(() => "");
      throw new Error(`textverified ${init.method ?? "GET"} ${path} failed: HTTP ${res.status} ${body}`);
    }
    return res;
  }

  private async call<T>(path: string, init: RequestInit = {}): Promise<T> {
    const res = await this.callRaw(path, init);
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  }

  async listServices(): Promise<TvService[]> {
    return this.call<TvService[]>("/services");
  }

  async create(options: CreateVerificationOptions): Promise<SmsVerification> {
    const serviceName =
      options.variant === "business"
        ? TEXTVERIFIED_SERVICE_BUSINESS
        : TEXTVERIFIED_SERVICE_PERSONAL;
    log.info(`creating verification: service="${serviceName}" capability=sms`);

    const res = await this.callRaw("/verifications", {
      method: "POST",
      body: JSON.stringify({ serviceName, capability: "sms" }),
    });
    const locationHeader = res.headers.get("location");
    let verificationId: string;
    if (locationHeader) {
      verificationId = extractVerificationIdFromHref(locationHeader);
    } else {
      const link = (await res.json().catch(() => null)) as TvLink | null;
      if (!link?.href) {
        throw new Error(
          "textverified create: no Location header and no href in body."
        );
      }
      verificationId = extractVerificationIdFromHref(link.href);
    }

    const details = await this.call<TvVerificationDetails>(
      `/verifications/${verificationId}`
    );
    const rawNumber = details.number;
    if (!rawNumber) {
      throw new Error(
        `textverified verification ${verificationId} has no number yet (state=${details.state}).`
      );
    }
    const phoneNumber = normalizeToE164(rawNumber);
    log.info(`verification ${verificationId} allocated number ${phoneNumber.slice(0, 4)}***`);

    return new TextVerifiedHandle(this, verificationId, phoneNumber);
  }

  async waitForCode(verificationId: string): Promise<string> {
    log.debug(`waitForCode: polling ${verificationId}`);
    while (true) {
      const listing = await this.call<TvSmsListing>(
        `/sms?reservationId=${encodeURIComponent(verificationId)}`
      );
      const withCode = listing.data.find((entry) => entry.parsedCode);
      if (withCode?.parsedCode) {
        log.info(`waitForCode: received code for ${verificationId}`);
        return withCode.parsedCode;
      }
      await sleep(POLL_INTERVAL_MS);
    }
  }

  async releaseVerification(verificationId: string): Promise<void> {
    try {
      await this.call(`/verifications/${verificationId}/cancel`, {
        method: "POST",
      });
      log.info(`released verification ${verificationId}`);
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      if (msg.includes("HTTP 400") && msg.includes("Invalid operation")) {
        log.debug(`verification ${verificationId} already in terminal state; cancel no-op.`);
        return;
      }
      log.warn(`release verification ${verificationId} failed: ${msg}`);
    }
  }
}

class TextVerifiedHandle implements SmsVerification {
  constructor(
    private readonly provider: TextVerifiedProvider,
    readonly id: string,
    readonly phoneNumber: string
  ) {}

  waitForCode(): Promise<string> {
    return this.provider.waitForCode(this.id);
  }

  async release(): Promise<void> {
    await this.provider.releaseVerification(this.id);
  }
}

let providerCache: SmsVerificationProvider | null = null;

export function getSmsVerificationProvider(): SmsVerificationProvider {
  if (providerCache) return providerCache;
  const name = (process.env.WEB_MCP_SMS_PROVIDER ?? "textverified").toLowerCase();
  if (name === "textverified") {
    providerCache = new TextVerifiedProvider();
    return providerCache;
  }
  throw new Error(`Unknown SMS provider "${name}"; supported: [textverified].`);
}
