import type { Page } from "playwright";
import type { SessionIdentity, SessionMode } from "../types/live/session.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("live:identity");

const IDENTITY_SCRIPT = `(() => {
  var out = {
    jid: null,
    deviceJid: null,
    phoneNumber: null,
    pushName: null,
    accountType: "unknown"
  };
  try {
    var w = (typeof window !== "undefined") ? window : undefined;
    if (!w) return out;
    var req = w.require || (w.__d && function (name) {
      try { return w.__d(name); } catch (e) { return null; }
    });
    if (typeof req !== "function") return out;

    try {
      var me = req("WAWebUserPrefsMeUser");
      if (me) {
        try {
          var device = me.getMeDevicePnOrThrow_DO_NOT_USE && me.getMeDevicePnOrThrow_DO_NOT_USE();
          if (device) {
            out.deviceJid = device.toString ? device.toString() : null;
            out.phoneNumber = device.user || null;
          }
        } catch (e) {}
        try {
          var user = (me.getMeUser && me.getMeUser())
            || (me.getMeUserPnAddr && me.getMeUserPnAddr())
            || (me.getMaybeMeUser && me.getMaybeMeUser());
          if (user) {
            out.jid = user.toString ? user.toString() : null;
          }
        } catch (e) {}
      }
    } catch (e) {}

    try {
      var pn = req("WAWebUserPrefsPushName");
      if (pn && typeof pn.getMyPushName === "function") {
        out.pushName = pn.getMyPushName() || null;
      }
    } catch (e) {}

    try {
      var bizCandidates = [
        "WAWebBizAccountSettingsAccountInfo",
        "WAWebBusinessGating",
        "WAWebBizAccountSettings"
      ];
      for (var i = 0; i < bizCandidates.length; i++) {
        try {
          var mod = req(bizCandidates[i]);
          if (!mod) continue;
          var fn = mod.isBusinessAccount || mod.isBizAccount || mod.isBusiness;
          if (typeof fn === "function") {
            out.accountType = fn() ? "business" : "personal";
            break;
          }
        } catch (e) {}
      }
    } catch (e) {}

    return out;
  } catch (e) {
    return out;
  }
})()`;

export async function extractIdentity(
  page: Page,
  platform: SessionMode,
  linkedAt: string
): Promise<SessionIdentity | null> {
  try {
    const partial = (await page.evaluate(IDENTITY_SCRIPT)) as {
      jid: string | null;
      deviceJid: string | null;
      phoneNumber: string | null;
      pushName: string | null;
      accountType: "personal" | "business" | "unknown";
    } | null;
    if (!partial) return null;
    const allNull =
      partial.jid === null &&
      partial.deviceJid === null &&
      partial.phoneNumber === null &&
      partial.pushName === null &&
      partial.accountType === "unknown";
    if (allNull) return null;
    return { ...partial, platform, linkedAt };
  } catch (error) {
    log.warn(
      `extractIdentity failed: ${error instanceof Error ? error.message : String(error)}`
    );
    return null;
  }
}
