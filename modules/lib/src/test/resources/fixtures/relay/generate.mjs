#!/usr/bin/env node
/**
 * generate-call-fixtures.mjs — drives a real outbound voice call against
 * a logged-in WhatsApp Web session and captures the fixtures used by the
 * relay-protocol parity tests in modules/lib (WaRelayPacketParityTest,
 * WaRelayMessageIntegrityParityTest, WaRelayAllocateRequestBuilderParityTest,
 * WaRelayCallInfoParityTest, WaRelayXorAddressParityTest).
 *
 * ============================================================================
 * What the fixtures are
 * ============================================================================
 *
 * The parity tests pin Cobalt's pure-Java relay-protocol implementation
 * against the byte-for-byte output of WhatsApp Web's wasm engine
 * (LXhtHOXr16K.wasm — pjsip-derived STUN/TURN code with WhatsApp
 * extensions). Two fixtures are generated:
 *
 *   1. fixtures/relay/stun-bytes-raw.json
 *      The raw bytes of every RTCDataChannel.send call captured during
 *      the session. Format mirrors the original voip-shadow capture
 *      schema:
 *
 *        {
 *          "totalChecked": 1,
 *          "capturedCount": 1,
 *          "captured": [{
 *            "targetId": "<worker-id>",
 *            "title":    "WAWebVoipWebWasmWorkerBundle",
 *            "stats":    {"total": N, "byPhase": {"rtc-dc": N}, "byName": {"send": N}},
 *            "trace":    [
 *              {
 *                "phase": "rtc-dc",
 *                "name":  "send",
 *                "args":  [
 *                  {"$typed": "Uint8Array", "$bytes": "<base64>", "$len": <N>},
 *                  {"label": "pre-negotiated", "id": 0, "state": "open"}
 *                ],
 *                "thrown": null,
 *                "t": <ms-since-hook-install>
 *              },
 *              ...
 *            ]
 *          }]
 *        }
 *
 *      The trace contains both the 344-byte Allocate Requests
 *      (msgType 0x0003, carrying WA-RELAY-TOKEN + WA-CALL-INFO +
 *      XOR-RELAYED-ADDRESS + MESSAGE-INTEGRITY) and the 20-byte
 *      WA-keepalive packets (msgType 0x0801, header-only).
 *
 *   2. fixtures/relay/relay-list-updates.json
 *      The RelayListUpdate (eventType=156) payloads emitted by the wasm
 *      engine via onCallEvent during the call. Multiple are emitted per
 *      call as the engine refreshes its relay list; each Allocate
 *      Request is keyed on the RLU whose relay_tokens[] contains the
 *      packet's WA-RELAY-TOKEN. Format:
 *
 *        {
 *          "capturedAt": "<ISO-8601>",
 *          "callId":     "<32-char hex>",
 *          "peerJid":    "<peer JID used>",
 *          "relayListUpdates": [
 *            {
 *              "auth_tokens":  [...],
 *              "relay_key":    "<base64>",
 *              "relay_tokens": [...],
 *              "relays":       [...],
 *              ...
 *            },
 *            ...
 *          ]
 *        }
 *
 *      Note: the relay_key here is a base64 string. The HMAC-SHA1 key
 *      WhatsApp uses for MESSAGE-INTEGRITY is the raw ASCII bytes of
 *      this base64 string (padding included), NOT the binary form. See
 *      WaRelayMessageIntegrity.java for the documented derivation.
 *
 * ============================================================================
 * How the script works
 * ============================================================================
 *
 *   [1] Connect to the live session's CDP endpoint (HTTP /json on the
 *       supplied port). Locate the page target whose URL is
 *       web.whatsapp.com.
 *
 *   [2] Apply the calling-related AB props that the engine gates on
 *       (enable_web_calling, enable_web_group_calling,
 *       enable_web_voip_proxy_and_sctp_workers, ...) via
 *       WAWebABProps.setABPropConfigValue, then patch
 *       WAWebVoipGatingUtils functions directly so isCallingEnabled()
 *       et al. return true regardless of cached state. This avoids
 *       requiring a page reload.
 *
 *   [3] Install the page-side onCallEvent hook on
 *       WAWebVoipHandleNativeCallEvent.handleWAWebVoipNativeCallEvent.
 *       Defines the property as a getter that always returns the
 *       wrapper, so that the WhatsApp module loader's slot-rewrite
 *       behaviour cannot silently displace the wrap. The wrapper
 *       records every onCallEvent invocation (eventType + parsed
 *       eventDataJson) on window.__callEventShadow.
 *
 *   [4] Spin up a 150 ms-tick auto-hook loop that polls the CDP
 *       /json endpoint for new WAWebVoipWebWasmWorkerBundle worker
 *       targets and injects the RTCDataChannel.send hook into each.
 *       The active-call worker spawns AFTER the call starts, so
 *       continuous hooking is required to catch the very first
 *       (Allocate Request) send.
 *
 *   [5] Place the outbound call by invoking the low-level
 *       stack.startCall(peerJid, fanoutCsv, callId, isVideo, ...)
 *       directly. This bypasses
 *       WAWebVoipStartCall.startWAWebVoipCall's permission/UI
 *       prompts that hang in headless contexts.
 *
 *   [6] Poll all hooked workers for activity. The active worker is the
 *       one whose __rtcShadow.trace contains ≥1 entry of length ≥300
 *       bytes (an Allocate Request). Wait until both that condition
 *       AND ≥1 RelayListUpdate are observed.
 *
 *   [7] Drain the page trace (onCallEvent log) and the active worker's
 *       RTC trace, then end the call via stack.endCall.
 *
 *   [8] Write the two fixtures to
 *       this directory.
 *
 * ============================================================================
 * Limitations / pre-conditions (NOT fully automated)
 * ============================================================================
 *
 *   - The MCP-managed live session must already be running, logged in,
 *     and contactable on the supplied CDP port. This script DOES NOT
 *     start the session itself — start it via the MCP server (or the
 *     web-mcp-server CLI) before invoking this script.
 *
 *   - The peer JID must be reachable enough that the engine reaches
 *     the relay-bind phase. In practice, even calls to offline peers
 *     work — WhatsApp emits Allocate Requests during the brief
 *     "calling" window before the peer answers. Calls that fail to
 *     even leave the device (e.g., blocked contact, malformed JID)
 *     produce no fixture data.
 *
 *   - The peer JID is supplied on the command line. It can be either
 *     a phone-number JID (e.g., 14155551234@c.us) or a LID-form JID
 *     (e.g., 83116928594056@lid). The script resolves it via
 *     WAWebLidMigrationUtils.toLid / toPn so either form works.
 *
 *   - On Chrome 145+ the wasm worker terminates seconds after the call
 *     ends. If the script's drain step is slow, the worker may die
 *     mid-drain. The script reads the trace BEFORE ending the call,
 *     which avoids this.
 *
 * ============================================================================
 * Usage
 * ============================================================================
 *
 *   node generate.mjs --port 53699 --peer 83116928594056@lid
 *
 *   --port <N>      CDP port of the live session (required).
 *   --peer <jid>    Peer JID to call (required).
 *   --video         Place a video call instead of voice.
 *   --max-wait <s>  Seconds to wait for a big packet + RLU (default 60).
 *   --out <dir>     Override output directory (default: this script's directory).
 *   --keep-call     Don't end the call after capture (useful for
 *                   continuing to dump wasm memory etc.)
 *
 * ============================================================================
 * Re-running
 * ============================================================================
 *
 * The script overwrites the two fixture files on each run. The Java
 * parity tests are stable against re-captures: WA-RELAY-TOKEN,
 * relay_key, transactionId, and the captured byte sequence change per
 * call but the structural properties they pin (round-trip,
 * MESSAGE-INTEGRITY verifies, axis grid, byte-exact rebuild) hold for
 * every legitimate capture.
 */

import { argv, exit, stdout } from "node:process";
import { writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

function parseArgs() {
  const args = { port: null, peer: null, video: false, maxWait: 60, out: null, keepCall: false };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--port") args.port = Number.parseInt(argv[++i], 10);
    else if (a === "--peer") args.peer = argv[++i];
    else if (a === "--video") args.video = true;
    else if (a === "--max-wait") args.maxWait = Number.parseInt(argv[++i], 10);
    else if (a === "--out") args.out = argv[++i];
    else if (a === "--keep-call") args.keepCall = true;
    else if (a === "-h" || a === "--help") { printUsage(); exit(0); }
    else { console.error("unknown arg: " + a); printUsage(); exit(1); }
  }
  if (!args.port || !args.peer) { printUsage(); exit(1); }
  if (!args.out) args.out = dirname(fileURLToPath(import.meta.url));
  return args;
}
function printUsage() {
  console.error("usage: generate.mjs --port <cdp-port> --peer <jid> [--video] [--max-wait <s>] [--out <dir>] [--keep-call]");
}

class CdpClient {
  constructor(url) { this.url = url; this.id = 0; this.pending = new Map(); }
  connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);
      this.ws.addEventListener("open", () => resolve());
      this.ws.addEventListener("error", () => reject(new Error("ws connect failed")));
      this.ws.addEventListener("close", () => {});
      this.ws.addEventListener("message", (ev) => {
        const m = JSON.parse(typeof ev.data === "string" ? ev.data : "");
        if (m.id != null && this.pending.has(m.id)) {
          const p = this.pending.get(m.id);
          this.pending.delete(m.id);
          m.error ? p.reject(new Error(JSON.stringify(m.error))) : p.resolve(m.result);
        }
      });
    });
  }
  send(method, params = {}, ms = 30000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      try { this.ws.send(JSON.stringify({ id, method, params })); }
      catch (e) { this.pending.delete(id); return reject(e); }
      setTimeout(() => { if (this.pending.delete(id)) reject(new Error(method + ": timeout")); }, ms);
    });
  }
  close() { try { this.ws.close(); } catch (_) {} }
}

async function listTargets(port) {
  const r = await fetch("http://127.0.0.1:" + port + "/json");
  return await r.json();
}

async function quickEval(target, expression, ms = 4000) {
  const c = new CdpClient(target.webSocketDebuggerUrl);
  try {
    await c.connect();
    const r = await c.send("Runtime.evaluate", { expression, returnByValue: true }, ms);
    c.close();
    if (r.exceptionDetails) return undefined;
    return r?.result?.value;
  } catch (_) { c.close(); return undefined; }
}

/**
 * Enables calls + force-loads VoIP modules on the page.
 * Patches WAWebVoipGatingUtils directly so calls-disabled gating
 * cannot suppress the call.
 */
const ENABLE_VOIP_SCRIPT = `
(async () => {
  function setProp(name, value) {
    try { require('WAWebABProps').setABPropConfigValue(name, value); } catch (_) {}
  }
  setProp('enable_web_calling', true);
  setProp('enable_web_group_calling', true);
  setProp('enable_web_voip_p2p', true);
  setProp('enable_web_voip_proxy_and_sctp_workers', true);
  setProp('enable_wds_calling_dropdown', true);
  setProp('enable_unified_call_buttons_in_chat', true);
  try {
    await require('JSResourceForInteraction')('WAWebVoipStackInterfaceImpl').load();
    await require('JSResourceForInteraction')('WAWebVoipStackInterfaceWeb').load();
    await require('JSResourceForInteraction')('WAWebVoipInit').load();
  } catch (_) {}
  try {
    const g = require('WAWebVoipGatingUtils');
    g.isCallingEnabled = () => true;
    g.isGroupCallingEnabled = () => true;
    g.isVoipDownloadEnabled = () => true;
    g.callLinksEnabled = () => true;
    g.isUnsupportedBrowserForWebCalling = () => false;
    g.getUnsupportedBrowserReason = () => null;
  } catch (_) {}
  try {
    const impl = require('WAWebVoipStackInterfaceImpl');
    const webStack = require('WAWebVoipStackInterfaceWeb');
    impl.getVoipStackInterfaceImpl = () => webStack.createWAWebVoipStackInterface();
  } catch (_) {}
  try { await require('WAWebVoipInit').initWAWebVoip(); } catch (_) {}
  return 'ok';
})()
`;

/**
 * Hooks WAWebVoipHandleNativeCallEvent.handleWAWebVoipNativeCallEvent.
 * Defined as a property getter so the WA module loader cannot silently
 * displace the wrap by reassigning the slot.
 */
const PAGE_HOOK = `
(() => {
  const trace = (window.__callEventShadow && window.__callEventShadow.trace) || [];
  const t0 = window.__callEventShadow ? window.__callEventShadow.t0 : performance.now();
  const m = require('WAWebVoipHandleNativeCallEvent');
  if (!m) return 'handler-missing';
  let orig = m.handleWAWebVoipNativeCallEvent;
  if (orig && orig.__shadowed) orig = orig.__origRef;
  if (!orig) return 'handler-missing';
  const wrapped = function (eventType, eventDataJson) {
    try {
      let parsed = null; try { parsed = eventDataJson ? JSON.parse(eventDataJson) : null; } catch (_) {}
      trace.push({ name: 'onCallEvent', t: performance.now() - t0, eventType, eventData: parsed });
    } catch (_) {}
    return orig.call(this, eventType, eventDataJson);
  };
  wrapped.__shadowed = true; wrapped.__origRef = orig;
  try {
    Object.defineProperty(m, 'handleWAWebVoipNativeCallEvent', {
      configurable: true, enumerable: true,
      get() { return wrapped; },
      set(v) { orig = v && v.__shadowed ? v.__origRef : v; },
    });
  } catch (_) { m.handleWAWebVoipNativeCallEvent = wrapped; }
  window.__callEventShadow = { trace, t0, flush() { const c = trace.slice(); trace.length = 0; return c; } };
  return 'installed';
})()
`;

/**
 * Hooks RTCDataChannel.prototype.send in the worker scope. Records
 * every send as {phase, name, args:[serialised, dcInfo], thrown, t}
 * onto globalThis.__rtcShadow.trace.
 */
const WORKER_HOOK = `
(function () {
  if (globalThis.__rtcShadow) return 'already';
  const t0 = performance.now();
  const trace = [];
  function ser(v) {
    if (v == null) return null;
    if (v instanceof ArrayBuffer) return { $bytes: btoa(String.fromCharCode.apply(null, new Uint8Array(v))), $len: v.byteLength };
    if (ArrayBuffer.isView(v)) return { $typed: v.constructor.name, $bytes: btoa(String.fromCharCode.apply(null, new Uint8Array(v.buffer, v.byteOffset, v.byteLength))), $len: v.byteLength };
    return null;
  }
  if (typeof RTCDataChannel !== 'undefined') {
    const dcProto = RTCDataChannel.prototype;
    if (!dcProto.__rtcShadowHooked) {
      dcProto.__rtcShadowHooked = true;
      const orig = dcProto.send;
      dcProto.send = function (data) {
        const snap = ser(data);
        let r, t = null;
        try { r = orig.call(this, data); } catch (e) { t = e; }
        trace.push({ phase: 'rtc-dc', name: 'send', args: [snap, { label: this.label, id: this.id, state: this.readyState }], thrown: t ? String(t.message || t) : null, t: performance.now() - t0 });
        if (t) throw t;
        return r;
      };
    }
  }
  globalThis.__rtcShadow = {
    trace,
    big() { return trace.filter(e => e.args && e.args[0] && e.args[0].$len >= 300).length; },
    flush() { const c = trace.slice(); trace.length = 0; return c; },
    stats() { return { total: trace.length, big: this.big() }; }
  };
  return 'installed';
})()
`;

async function main() {
  const args = parseArgs();
  const port = args.port;
  const log = (s) => stdout.write("[" + new Date().toISOString().slice(11, 19) + "] " + s + "\n");

  log("[1/8] connecting to CDP on port " + port);
  const targets = await listTargets(port);
  const page = targets.find(t => t.type === "page" && (t.url || "").includes("web.whatsapp.com"));
  if (!page) throw new Error("no WhatsApp page target found at port " + port);
  log("    page target: " + page.id.slice(0, 8));

  log("[2/8] enabling VoIP and patching gating");
  const pageClient = new CdpClient(page.webSocketDebuggerUrl);
  await pageClient.connect();
  await pageClient.send("Runtime.evaluate", { expression: ENABLE_VOIP_SCRIPT, returnByValue: true, awaitPromise: true }, 30000);

  log("[3/8] installing page-side onCallEvent hook");
  const r1 = await pageClient.send("Runtime.evaluate", { expression: PAGE_HOOK, returnByValue: true }, 10000);
  log("    " + r1.result.value);

  log("[4/8] hooking RTC.send in pre-existing wasm-capable workers");
  const preWorkers = (await listTargets(port)).filter(t => (t.title || "").includes("WAWebVoipWebWasmWorkerBundle"));
  await Promise.all(preWorkers.map(t => quickEval(t, WORKER_HOOK, 3000)));
  log("    " + preWorkers.length + " worker targets pre-hooked");

  // Auto-hook loop runs in background; covers workers spawned after
  // the call starts.
  const seen = new Set(preWorkers.map(t => t.id));
  let autoHookStop = false;
  const autoHookPromise = (async () => {
    while (!autoHookStop) {
      try {
        const tgs = await listTargets(port);
        for (const t of tgs.filter(x => (x.title || "").includes("WAWebVoipWebWasmWorkerBundle"))) {
          if (seen.has(t.id)) continue;
          seen.add(t.id);
          try { await quickEval(t, WORKER_HOOK, 2000); } catch (_) {}
        }
      } catch (_) {}
      await new Promise(r => setTimeout(r, 150));
    }
  })();

  log("[5/8] placing outbound " + (args.video ? "video" : "voice") + " call to " + args.peer);
  const callExpr = `(async () => {
    try {
      const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
      const peer = require('WAWebWidFactory').createWid(${JSON.stringify(args.peer)});
      const lid = require('WAWebLidMigrationUtils').toLid(peer);
      const pn = require('WAWebLidMigrationUtils').toPn(peer);
      const cid = '00' + require('WARandomHex').randomHex(16).substr(2);
      const fanout = await require('WAWebSendMsgDatabaseJob').getFanOutListJob([lid || pn]);
      const csv = fanout.filter(j => !j.isCompanion()).map(j => j.toString({legacy:true,formatIncludeDevice:true}));
      const tcRes = await require('WAWebBackendApi').frontendSendAndReceive('getTcToken', {wid: peer});
      await require('WAWebSendTcTokenChatAction').sendTcToken(lid || pn);
      await stack.startCall((lid || pn).toString({legacy:true}), csv, cid, ${args.video}, (pn || lid || peer).toString({legacy:true}), false, tcRes.tcToken, 0, 0, null);
      window.__lastCallId = cid;
      return 'ok:' + cid;
    } catch (e) { return 'err:' + (e && e.message); }
  })()`;
  const cr = await pageClient.send("Runtime.evaluate", { expression: callExpr, returnByValue: true, awaitPromise: true }, 30000);
  const callRes = cr.result.value;
  log("    " + callRes);
  if (typeof callRes !== "string" || !callRes.startsWith("ok:")) {
    autoHookStop = true; await autoHookPromise;
    pageClient.close();
    throw new Error("startCall failed: " + callRes);
  }
  const callId = callRes.slice(3);

  log("[6/8] waiting up to " + args.maxWait + "s for big packet + RelayListUpdate");
  let activeWorker = null;
  let foundRlu = false;
  const deadline = Date.now() + args.maxWait * 1000;
  while (Date.now() < deadline && (!activeWorker || !foundRlu)) {
    await new Promise(r => setTimeout(r, 1500));
    const tgs = await listTargets(port);
    const ws = tgs.filter(t => (t.title || "").includes("WAWebVoipWebWasmWorkerBundle"));
    let totalBig = 0;
    for (const w of ws) {
      try {
        const big = await quickEval(w, "globalThis.__rtcShadow ? globalThis.__rtcShadow.big() : -1", 1500);
        if (typeof big === "number" && big > 0 && !activeWorker) activeWorker = w;
        if (typeof big === "number" && big > 0) totalBig += big;
      } catch (_) {}
    }
    const rluCount = await pageClient.send("Runtime.evaluate", {
      expression: "window.__callEventShadow ? window.__callEventShadow.trace.filter(e => e.eventType === 156).length : 0",
      returnByValue: true,
    }, 3000).then(r => r.result.value).catch(() => 0);
    if (rluCount >= 1) foundRlu = true;
    log("    big-packets=" + totalBig + " RLU=" + rluCount);
  }
  autoHookStop = true; await autoHookPromise;

  if (!activeWorker) {
    pageClient.close();
    throw new Error("no worker captured a big packet within " + args.maxWait + "s — peer may be unreachable / call rejected");
  }
  if (!foundRlu) {
    pageClient.close();
    throw new Error("no RelayListUpdate captured within " + args.maxWait + "s — call may have ended too early");
  }

  log("[7/8] draining traces");
  const pageTraceJson = await pageClient.send("Runtime.evaluate", {
    expression: "JSON.stringify(window.__callEventShadow.flush())",
    returnByValue: true,
  }, 10000).then(r => r.result.value);
  const workerClient = new CdpClient(activeWorker.webSocketDebuggerUrl);
  await workerClient.connect();
  const rtcTraceJson = await workerClient.send("Runtime.evaluate", {
    expression: "JSON.stringify(globalThis.__rtcShadow.flush())",
    returnByValue: true,
  }, 10000).then(r => r.result.value);
  workerClient.close();

  if (!args.keepCall) {
    log("    ending call " + callId);
    try {
      await pageClient.send("Runtime.evaluate", {
        expression: "(async () => { try { const s = await require('WAWebVoipStackInterface').getVoipStackInterface(); await s.endCall(window.__lastCallId, 0); return 'ok'; } catch (e) { return 'err'; } })()",
        returnByValue: true, awaitPromise: true,
      }, 10000);
    } catch (_) {}
  }
  pageClient.close();

  log("[8/8] writing fixtures to " + args.out);
  const pageTrace = JSON.parse(pageTraceJson);
  const rtcTrace = JSON.parse(rtcTraceJson);

  const stunBytes = {
    totalChecked: 1,
    capturedCount: 1,
    captured: [{
      targetId: activeWorker.id,
      title: "WAWebVoipWebWasmWorkerBundle",
      stats: {
        total: rtcTrace.length,
        byPhase: { "rtc-dc": rtcTrace.length },
        byName: { send: rtcTrace.length },
      },
      trace: rtcTrace,
    }],
  };
  const rlus = pageTrace.filter(e => e.eventType === 156);
  const relayListUpdates = {
    capturedAt: new Date().toISOString(),
    callId,
    peerJid: args.peer,
    relayListUpdates: rlus.map(e => e.eventData),
  };

  mkdirSync(args.out, { recursive: true });
  const stunPath = join(args.out, "stun-bytes-raw.json");
  const rluPath = join(args.out, "relay-list-updates.json");
  writeFileSync(stunPath, JSON.stringify(stunBytes, null, 2));
  writeFileSync(rluPath, JSON.stringify(relayListUpdates, null, 2));

  // Summary
  const lens = {};
  for (const e of rtcTrace) {
    const a = e.args && e.args[0]; if (a && a.$len) lens[a.$len] = (lens[a.$len] || 0) + 1;
  }
  log("written:");
  log("  " + stunPath + "  (" + rtcTrace.length + " packets, lens=" + JSON.stringify(lens) + ")");
  log("  " + rluPath + "  (" + rlus.length + " RelayListUpdates)");
  if ((lens[344] || 0) === 0) {
    log("WARNING: no 344-byte Allocate Request captured — fixtures will not pin Allocate-Request shape");
  }
}

main().catch(e => { console.error("FATAL:", e.stack || e.message); exit(1); });
