import { WHATSAPP_WEB_URL } from "./manager-constants.js";

export function normalizePhone(value: string): string {
  return value.replace(/[^\d+]/g, "");
}

export function phoneDigitsOnly(value: string): string {
  return value.replace(/\D/g, "");
}

export function buildFullPhoneValue(phoneNumber: string, countryCode?: string): string {
  const normalizedPhone = normalizePhone(phoneNumber);
  const normalizedCountry = countryCode ? normalizePhone(countryCode) : "";
  if (normalizedPhone.startsWith("+")) {
    return normalizedPhone;
  }
  if (normalizedCountry.length > 0) {
    return `${normalizedCountry} ${phoneDigitsOnly(normalizedPhone)}`.trim();
  }
  return normalizedPhone;
}

export function normalizePairingCode(value: string): string {
  return value.replace(/[\s-]+/g, "").trim();
}

export function primaryLanguage(locale: string): string {
  const normalized = locale.trim();
  if (!normalized) return "en";
  return normalized.split("-")[0].toLowerCase();
}

export function buildWebUrlForLocale(locale: string): string {
  const lang = primaryLanguage(locale);
  const url = new URL(WHATSAPP_WEB_URL);
  url.searchParams.set("lang", lang);
  return url.toString();
}
