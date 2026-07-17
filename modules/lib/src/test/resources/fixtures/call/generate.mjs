#!/usr/bin/env node
//
// Drives two logged-in WA Web sessions through a deterministic VoIP-call
// sequence and captures the call-package fixture corpus. Output goes to
// data/captures/<sessionId>/call/<topic>.{jsonl,expected.json} on each
// participating session and is later curated into this directory.
//
// This driver is fully unattended — every step runs without user prompts.
// Calls are placed against the consented pre-paired peer roster locked
// at the top of this file. Do NOT run against a session whose roster has
// not been pre-confirmed.
//
// Session pair (locked, validated):
//   * caller  "business"     19254863482@c.us   83116928594056@lid       :1@lid
//   * callee  "primary"      19153544650@c.us   39110693621863@lid       :17@lid
//   * third   "history-sync" 393495089819@c.us  258252122116273@lid      :82@lid
//
// Codec note: the current WA Web snapshot emits Opus-only in offers and
// preaccepts. Tier-1 flow #1 asserts this against the captured offer; if
// a future snapshot adds MLow advertisement the assertion fails loudly.
//
// Usage:
//   node generate.mjs
//
// Each capture step prints a banner. Between every flow we stop+start
// both participating sessions because WA Web sessions reliably fail to
// land a second call on the same session (see
// memory/feedback_kill_session_per_call.md).

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

// -------- Locked session/peer roster -------------------------------------

const SESSIONS = Object.freeze({
  CALLER: "business",
  CALLEE: "primary",
  THIRD:  "history-sync",
});

const PEERS = Object.freeze({
  CALLER_PN:        "19254863482@c.us",
  CALLER_LID:       "83116928594056@lid",
  CALLER_DEVICE:    "83116928594056:1@lid",
  CALLEE_PN:        "19153544650@c.us",
  CALLEE_LID:       "39110693621863@lid",
  CALLEE_DEVICE:    "39110693621863:17@lid",
  THIRD_PN:         "393495089819@c.us",
  THIRD_LID:        "258252122116273@lid",
  THIRD_DEVICE:     "258252122116273:82@lid",
});

// -------- Embedded enable-VoIP block ------------------------------------
//
// Run on every participating session before placing or answering a call.

// Fire-and-forget pattern: the MCP/CDP bridge cannot serialise the
// resolved value of a bare async IIFE (it comes back as `{}`), so the
// outer wrapper is sync and stashes the eventual state on
// `window.__voipReady` for the poller to read.
const ENABLE_VOIP = `(() => {
  window.__voipReady = 'pending';
  (async () => {
    try {
      // Opt the session into the external-web beta BEFORE setting AB
      // props locally. setOptInBetaAction stops + restarts comms and
      // re-syncs AB props from the server, which clobbers any local
      // overrides that ran first. The server-side beta channel also
      // delivers call-gating AB props that no client-side override can
      // replicate. Idempotent: skip if already opted in.
      try {
        const prefs = require('WAWebUserPrefsGeneral');
        const joined = await prefs.getWhatsAppWebExternalBetaJoinedIdb();
        if (!joined) {
          window.__voipReady = 'opting-into-beta';
          await require('WAWebExternalBetaOptInAction').setOptInBetaAction(true);
        }
      } catch (e) {
        window.__voipReady = 'beta-opt-in-err:' + String(e && (e.message || e));
        return;
      }

      function setProp(name, value) {
        try { require('WAWebABProps').setABPropConfigValue(name, value); } catch (_) {}
      }
      setProp('enable_web_calling', true);
      setProp('enable_web_group_calling', true);
      setProp('enable_web_voip_p2p', true);
      setProp('enable_web_voip_proxy_and_sctp_workers', true);
      setProp('enable_wds_calling_dropdown', true);
      setProp('enable_unified_call_buttons_in_chat', true);
      try { await require('JSResourceForInteraction')('WAWebVoipStackInterfaceImpl').load(); } catch (_) {}
      try { await require('JSResourceForInteraction')('WAWebVoipStackInterfaceWeb').load(); } catch (_) {}
      try { await require('JSResourceForInteraction')('WAWebVoipInit').load(); } catch (_) {}
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
        const web = require('WAWebVoipStackInterfaceWeb');
        impl.getVoipStackInterfaceImpl = () => web.createWAWebVoipStackInterface();
      } catch (_) {}
      try { await require('WAWebVoipInit').initWAWebVoip(); } catch (_) {}
      // In headless Chromium, checkVoipDevicePermissions(isVideo=true)
      // hangs forever trying to acquire a real camera stream. Replace
      // it with a no-op that reports permissions as granted so video
      // calls can progress past the precondition gate.
      try {
        const M = require('WAWebVoipAcquireMediaStream');
        if (M && typeof M.checkVoipDevicePermissions === 'function') {
          M.checkVoipDevicePermissions = function () { return Promise.resolve(true); };
        }
        if (M && typeof M.acquireVoipMediaStream === 'function') {
          const orig = M.acquireVoipMediaStream;
          M.acquireVoipMediaStream = async function (streamType /*, ...rest*/) {
            try { return await orig.apply(this, arguments); }
            catch (_) {
              // Fall back to an empty synthetic stream when the real
              // acquire fails — the signaling layer (offer/accept/etc.)
              // doesn't care that there are no real tracks.
              return new MediaStream();
            }
          };
        }
      } catch (_) {}
      for (let i = 0; i < 30; i++) {
        try {
          const E = require('WAWebVoipInitEventEmitter').VoipInitEventEmitter;
          if (E.getIsVoipInited()) { window.__voipReady = 'voip-ready'; return; }
        } catch (_) {}
        await new Promise(r => setTimeout(r, 200));
      }
      window.__voipReady = 'voip-not-ready';
    } catch (e) {
      window.__voipReady = 'ERR:' + String(e && (e.message || e));
    }
  })();
  return 'fired';
})()`;

// -------- RPC plumbing ---------------------------------------------------

let nextRpcId = 1;

async function callTool(name, params) {
  const rpcId = nextRpcId++;
  const response = await fetch(MCP_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json, text/event-stream" },
    body: JSON.stringify({
      jsonrpc: "2.0",
      id: rpcId,
      method: "tools/call",
      params: { name, arguments: params },
    }),
  });
  if (!response.ok) {
    throw new Error(`tools/call ${name} -> HTTP ${response.status}`);
  }
  const body = parseSseOrJson(await response.text());
  if (body.error) {
    throw new Error(`tools/call ${name} -> ${body.error.message ?? JSON.stringify(body.error)}`);
  }
  const result = body.result;
  if (result?.isError) {
    const msg = result.content?.[0]?.text ?? "unknown error";
    throw new Error(`tools/call ${name} -> ${msg}`);
  }
  return parseToolResult(result);
}

function parseSseOrJson(text) {
  const trimmed = text.trim();
  if (trimmed.startsWith("{")) return JSON.parse(trimmed);
  for (const line of trimmed.split(/\r?\n/)) {
    if (line.startsWith("data:")) return JSON.parse(line.slice(5).trim());
  }
  throw new Error(`unexpected response body: ${trimmed.slice(0, 200)}`);
}

function parseToolResult(result) {
  const part = result?.content?.[0];
  if (part?.type !== "text") return result;
  try { return JSON.parse(part.text); } catch { return part.text; }
}

// -------- Tiny helpers ---------------------------------------------------

const banner = (msg) => console.log(`\n========== ${msg} ==========`);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Hard ceiling per flow — once exceeded we abort that flow's action so the
// run continues. Each flow's internal awaits already have their own (smaller)
// timeouts; this is the safety net for everything else.
const FLOW_TIMEOUT_MS = 90_000;

function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(`TIMEOUT ${label} after ${ms}ms`)), ms)),
  ]);
}

async function eval_(sessionId, expression) {
  return callTool("web_live_debug_eval", { sessionId, expression });
}

async function evalString(sessionId, expression) {
  const res = await eval_(sessionId, expression);
  return res?.value ?? "";
}

async function clearStanzaCapture(sessionId) {
  await callTool("web_live_clear_capture", {
    sessionId, domain: "stanza", scope: "history",
  });
}

async function dumpStanzas(sessionId, topic, filter = {}) {
  return callTool("web_live_stanza_dump_to_file", {
    sessionId,
    topic: `call/${topic}`,
    history: true,
    limit: 2000,
    ...filter,
  });
}

async function dumpEval(sessionId, topic, expression) {
  return callTool("web_live_debug_eval_to_file", {
    sessionId,
    topic: `call/${topic}`,
    expression,
  });
}

// Polls `window.__r` on the session until it transitions out of 'pending'
// (set by the async IIFE we fire). Returns the final string value.
async function waitResult(sessionId, timeoutMs = 20000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const val = await evalString(sessionId, `String(window.__r)`);
      if (val !== "pending" && val !== "undefined") return val;
    } catch (_) { /* page may briefly close mid-eval */ }
    await sleep(250);
  }
  return "TIMEOUT";
}

// -------- Session lifecycle --------------------------------------------

async function resetSession(sessionId) {
  try { await callTool("web_live_stop_session", { sessionId }); } catch (_) {}
  await callTool("web_live_start_session", { sessionId });
  const res = await callTool("web_live_wait_for_login", { sessionId, timeoutMs: 60000 });
  if (!res?.success) throw new Error(`reset ${sessionId}: not logged_in (state=${res?.status?.authState})`);
}

async function ensureVoipEnabled(sessionId) {
  await eval_(sessionId, ENABLE_VOIP);
  const start = Date.now();
  // 75s ceiling. The beta opt-in path inside ENABLE_VOIP restarts comms
  // and re-syncs AB props (5-15s typical) before VoIP init even starts.
  while (Date.now() - start < 75000) {
    const v = await evalString(sessionId, `String(window.__voipReady || '')`);
    if (v && v !== "pending" && v !== "opting-into-beta") {
      if (v !== "voip-ready") throw new Error(`enableVoip ${sessionId}: ${v}`);
      return;
    }
    await sleep(250);
  }
  throw new Error(`enableVoip ${sessionId}: TIMEOUT waiting for voip-ready`);
}

async function resetAndEnable(sessionIds) {
  for (const id of sessionIds) await resetSession(id);
  for (const id of sessionIds) await ensureVoipEnabled(id);
  // give the call-listener bundles a moment to register on the WS layer
  await sleep(500);
}

// -------- Hooks -------------------------------------------------------

// Installs a self-contained auto-accept loop on the callee. It polls
// WAWebCallCollection.activeCall every 200ms and accepts the moment an
// inbound call (outgoing===false) appears. The fields used here were
// verified live: CC exposes `activeCall` directly as a model with
// {id, peerJid, offerTime, isVideo, isGroup, outgoing, ...}.
//
// `window.__autoState` reports lifecycle: 'armed' → 'incoming' →
//                                         'accepting' → 'accepted' | 'error:...'
async function installAutoAcceptHook(sessionId, isVideo = false) {
  await eval_(sessionId, `(() => {
    if (window.__autoTimer) { clearInterval(window.__autoTimer); window.__autoTimer = null; }
    window.__autoState = 'armed';
    window.__autoCallId = null;
    window.__autoTimer = setInterval(async () => {
      if (window.__autoState !== 'armed') return;
      try {
        const CC = require('WAWebCallCollection');
        const ac = CC.activeCall;
        if (!ac || ac.outgoing !== false || !ac.id) return;
        const cid = ac.id;
        window.__autoState = 'incoming';
        window.__autoCallId = cid;
        clearInterval(window.__autoTimer); window.__autoTimer = null;
        try {
          window.__autoState = 'accepting';
          const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
          await stack.acceptCall(true, ${isVideo ? "true" : "false"});
          window.__autoState = 'accepted';
        } catch (e) {
          window.__autoState = 'error:' + String(e && (e.message || e));
        }
      } catch (_) {
        // ignore; next tick will retry
      }
    }, 200);
    return 'auto-accept-armed';
  })()`);
}

// Mirror: poll for inbound, reject the moment it appears. Falls back to
// endCall(id, 0) if rejectCall isn't usable from this entrypoint.
async function installAutoRejectHook(sessionId) {
  await eval_(sessionId, `(() => {
    if (window.__autoTimer) { clearInterval(window.__autoTimer); window.__autoTimer = null; }
    window.__autoState = 'armed';
    window.__autoCallId = null;
    window.__autoTimer = setInterval(async () => {
      if (window.__autoState !== 'armed') return;
      try {
        const CC = require('WAWebCallCollection');
        const ac = CC.activeCall;
        if (!ac || ac.outgoing !== false || !ac.id) return;
        const cid = ac.id;
        window.__autoState = 'incoming';
        window.__autoCallId = cid;
        clearInterval(window.__autoTimer); window.__autoTimer = null;
        try {
          window.__autoState = 'rejecting';
          const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
          try { await stack.rejectCall(ac); }
          catch (_) { await stack.endCall(cid, 0); }
          window.__autoState = 'rejected';
        } catch (e) {
          window.__autoState = 'error:' + String(e && (e.message || e));
        }
      } catch (_) {}
    }, 200);
    return 'auto-reject-armed';
  })()`);
}

// Polls until the auto-hook reaches the named terminal state.
async function awaitAutoState(sessionId, target, timeoutMs = 30000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const s = await evalString(sessionId, `String(window.__autoState)`);
    if (s === target) return s;
    if (s && s.startsWith("error:")) return s;
    await sleep(250);
  }
  return "TIMEOUT";
}

// -------- Call actions ---------------------------------------------------

// Places an outbound 1:1 call via startWAWebVoipCall. The headless-mode
// camera-permission hang is fixed by the checkVoipDevicePermissions /
// acquireVoipMediaStream monkey-patches installed in ENABLE_VOIP — so
// the same path works for both audio and video.
async function placeCall(sessionId, peerJid, isVideo = false) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const peerWid = require('WAWebWidFactory').createWid(${JSON.stringify(peerJid)});
        const res = await require('WAWebVoipStartCall').startWAWebVoipCall(peerWid, ${isVideo ? "true" : "false"});
        window.__lastCall = res || null;
        window.__r = 'placed:' + ${isVideo ? "'video'" : "'audio'"};
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 30000);
}

// Places an outbound GROUP call by participant wid list. Uses
// WAWebVoipStartCall.startWAWebVoipGroupCallFromWids — verified export
// (see "l.startWAWebVoipGroupCallFromWids=ae" at the tail of the module).
//
// The signature is (participantsWids, isVideo, ...). Internally the
// function resolves LIDs, fanout, TC token, and dispatches the offer
// via stack.startGroupCall.
async function placeGroupCall(sessionId, participantJids, isVideo = false) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const W = require('WAWebWidFactory');
        const wids = ${JSON.stringify(participantJids)}.map(j => W.createWid(j));
        await require('WAWebVoipStartCall').startWAWebVoipGroupCallFromWids(wids, ${isVideo ? "true" : "false"}, 0, 0);
        window.__r = 'placed:group:' + ${isVideo ? "'video'" : "'audio'"};
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 30000);
}

async function endCall(sessionId, callId) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.endCall(${JSON.stringify(callId)}, 0);
        window.__r = 'ended';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 10000);
}

// stack.setCallMute(true) mutes the local mic; setCallVideoMute(true) hides
// the local camera. API names verified via runtime probe of the stack
// interface — keep these aligned with the actual WA Web exports.
async function setMute(sessionId, muted) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.setCallMute(${muted ? "true" : "false"});
        window.__r = 'mute:' + ${muted ? "'on'" : "'off'"};
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function setVideoMute(sessionId, muted) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.setCallVideoMute(${muted ? "true" : "false"});
        window.__r = 'video-mute:' + ${muted ? "'on'" : "'off'"};
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function requestVideoUpgrade(sessionId) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.requestVideoUpgrade();
        window.__r = 'upgrade-requested';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function acceptPeerVideo(sessionId, peerJid) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        const peerWid = require('WAWebWidFactory').createWid(${JSON.stringify(peerJid)});
        // acceptPeerVideo({jid: peerJid}) — signature verified via WAWebVoipWorkerHandler source.
        await stack.acceptPeerVideo({ jid: peerWid });
        window.__r = 'video-accepted';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function sendInteractionReaction(sessionId, emoji) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.sendReaction(${JSON.stringify(emoji)});
        window.__r = 'reaction-sent';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function sendRaiseHand(sessionId, raised) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.raiseHand(${raised ? "true" : "false"});
        window.__r = 'hand:' + ${raised ? "'up'" : "'down'"};
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function sendPeerMuteRequest(sessionId, targetJid) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        const wid = require('WAWebWidFactory').createWid(${JSON.stringify(targetJid)});
        await stack.requestPeerMute(wid);
        window.__r = 'peer-mute-sent';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

async function sendKeyframeRequest(sessionId) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.requestKeyFrame();
        window.__r = 'keyframe-sent';
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  return await waitResult(sessionId, 8000);
}

// Polls a session's outbound stanza buffer until an `<offer>` payload
// appears under a `<call>` envelope; returns the call-id, or null.
async function awaitOutgoingOfferCallId(sessionId, timeoutMs = 15000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const nodes = await callTool("web_live_stanza_query_nodes", {
      sessionId, tag: "call", direction: "out", history: true, limit: 50,
    }).catch(() => null);
    if (Array.isArray(nodes)) {
      for (const n of nodes) {
        const offer = n?.node?.content?.find?.(c => c.tag === "offer");
        const cid = offer?.attrs?.["call-id"];
        if (cid) return cid;
      }
    }
    await sleep(250);
  }
  return null;
}

// Polls a session's inbound stanza buffer for the first `<terminate>` under
// `<call>`, or a bare `<terminate>` carrying call-id.
async function awaitTerminate(sessionId, timeoutMs = 10000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const nodes = await callTool("web_live_stanza_query_nodes", {
      sessionId, direction: "any", history: true, limit: 100, query: "terminate",
    }).catch(() => null);
    if (Array.isArray(nodes)) {
      for (const n of nodes) {
        if (n?.tag === "terminate") return n;
        const t = n?.node?.content?.find?.(c => c.tag === "terminate");
        if (t) return n;
      }
    }
    await sleep(250);
  }
  return null;
}

// -------- Per-flow runner ---------------------------------------------

// Runs one captured flow:
//   1. resetAndEnable for every involved session (forces a clean slate)
//   2. clear stanza buffers
//   3. invoke the flow action
//   4. dump call stanzas + the eval expected snapshot for every session
//
// `sessionIds` is the array of sessions actively driven by this flow;
// the caller is always sessionIds[0].
async function runFlow(topic, sessionIds, action, expectedExpr = null, timeoutMs = FLOW_TIMEOUT_MS) {
  banner(topic);
  try {
    await withTimeout(resetAndEnable(sessionIds), 60_000, `resetAndEnable[${sessionIds.join(",")}]`);
  } catch (e) {
    console.log(`    setup ERR: ${e?.message ?? e}`);
    return `ERR:setup:${e?.message ?? e}`;
  }
  for (const id of sessionIds) await clearStanzaCapture(id).catch(() => {});
  let actionResult = null;
  try {
    actionResult = await withTimeout(action(), timeoutMs, `flow ${topic}`);
  } catch (e) {
    console.log(`    action ERR: ${e?.message ?? e}`);
    actionResult = `ERR:${e?.message ?? e}`;
  }
  // Give the wire a moment to flush trailing stanzas (relaylatency tail,
  // ack stanzas, etc.) before we snapshot the buffer.
  await sleep(1500);
  for (const id of sessionIds) {
    await withTimeout(dumpStanzas(id, topic, { tag: "call",      direction: "any" }), 10_000, `dump ${id} ${topic}`).catch(() => {});
    await withTimeout(dumpStanzas(id, `${topic}.terminate`, { tag: "terminate", direction: "any" }), 10_000, `dump ${id} terminate`).catch(() => {});
    await withTimeout(dumpStanzas(id, `${topic}.ack`,       { tag: "ack",       direction: "any" }), 10_000, `dump ${id} ack`).catch(() => {});
  }
  for (const id of sessionIds) {
    const auto  = await evalString(id, `String(window.__autoState || '')`).catch(() => "");
    const lastR = await evalString(id, `String(window.__r || '')`).catch(() => "");
    const expr = expectedExpr || `String(JSON.stringify({ result: window.__r || null, auto: window.__autoState || null }))`;
    await withTimeout(dumpEval(id, `${topic}.${id}`, expr), 10_000, `dumpEval ${id}`).catch(() => {});
    console.log(`    ${id}: __r=${shorten(lastR)}  auto=${auto || '-'}`);
  }
  return actionResult;
}

function shorten(s) { return s && s.length > 96 ? s.slice(0, 96) + "..." : s; }

// -------- Tier 1 — 1:1 audio/video lifecycle -------------------------

async function tier1AudioOfferPlace() {
  return runFlow("1to1/audio-offer-place", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    console.log(`    place: ${shorten(placed)}`);
    // Let the offer + preaccept + relay-latency chatter accumulate.
    await sleep(3000);
    return placed;
  });
}

async function tier1VideoOfferPlace() {
  return runFlow("1to1/video-offer-place", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, true);
    console.log(`    place: ${shorten(placed)}`);
    await sleep(3000);
    return placed;
  });
}

async function tier1AudioAccept() {
  return runFlow("1to1/audio-accept", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    console.log(`    place: ${shorten(placed)}`);
    const auto = await awaitAutoState(SESSIONS.CALLEE, "accepted", 25000);
    console.log(`    callee auto-state: ${auto}`);
    // Hold the call briefly so the relay-handshake completes and the
    // accept-receipt/accept-ack sequence drains.
    await sleep(4000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier1AudioReject() {
  return runFlow("1to1/audio-reject", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoRejectHook(SESSIONS.CALLEE);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    console.log(`    place: ${shorten(placed)}`);
    const auto = await awaitAutoState(SESSIONS.CALLEE, "rejected", 15000);
    console.log(`    callee auto-state: ${auto}`);
    await sleep(2000);
    return placed;
  });
}

async function tier1CallerHangupPreAccept() {
  return runFlow("1to1/caller-hangup-pre-accept", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    console.log(`    place: ${shorten(placed)}`);
    // Hang up before any callee action.
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 5000);
    if (cid) {
      console.log(`    caller endCall(${cid})`);
      await endCall(SESSIONS.CALLER, cid);
    }
    await sleep(3000);
    return placed;
  });
}

async function tier1CalleeTerminatePostAccept() {
  return runFlow("1to1/callee-terminate-post-accept", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    console.log(`    place: ${shorten(placed)}`);
    const auto = await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    console.log(`    callee auto-state: ${auto}`);
    await sleep(2500);
    const cid = await evalString(SESSIONS.CALLEE, `String(window.__autoCallId || '')`);
    if (cid) {
      console.log(`    callee endCall(${cid})`);
      await endCall(SESSIONS.CALLEE, cid);
    }
    await sleep(2500);
    return placed;
  });
}

async function tier1MuteToggle() {
  return runFlow("1to1/mute-toggle", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    // Caller mutes then unmutes; callee mutes; pause between each so the
    // stanza ordering on the wire is unambiguous.
    await setMute(SESSIONS.CALLER, true);  await sleep(1000);
    await setMute(SESSIONS.CALLER, false); await sleep(1000);
    await setMute(SESSIONS.CALLEE, true);  await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier1VideoStateToggle() {
  return runFlow("1to1/video-state-toggle", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, true);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, true);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    await setVideoMute(SESSIONS.CALLER, true);  await sleep(1500);
    await setVideoMute(SESSIONS.CALLER, false); await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier1VideoUpgradeAccept() {
  return runFlow("1to1/video-upgrade-accept", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    await requestVideoUpgrade(SESSIONS.CALLER); await sleep(1500);
    // Callee accepts the peer-video request.
    await acceptPeerVideo(SESSIONS.CALLEE, PEERS.CALLER_LID); await sleep(2000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier1VideoUpgradeReject() {
  return runFlow("1to1/video-upgrade-reject", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    await requestVideoUpgrade(SESSIONS.CALLER); await sleep(2500);
    // Callee never enables its camera — the upgrade hangs / rejects.
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

// -------- Tier 2 — in-call interactions --------------------------------

async function tier2InteractionReaction() {
  return runFlow("1to1/interaction-reaction", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    const sent = await sendInteractionReaction(SESSIONS.CALLER, "🎉");
    console.log(`    reaction: ${shorten(sent)}`);
    await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier2InteractionRaiseHand() {
  return runFlow("1to1/interaction-raise-hand", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    const r = await sendRaiseHand(SESSIONS.CALLER, true);
    console.log(`    raise-hand: ${shorten(r)}`);
    await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier2InteractionLowerHand() {
  return runFlow("1to1/interaction-lower-hand", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    await sendRaiseHand(SESSIONS.CALLER, true);  await sleep(800);
    const r = await sendRaiseHand(SESSIONS.CALLER, false);
    console.log(`    lower-hand: ${shorten(r)}`);
    await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier2InteractionPeerMute() {
  return runFlow("1to1/interaction-peer-mute", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    const r = await sendPeerMuteRequest(SESSIONS.CALLER, PEERS.CALLEE_LID);
    console.log(`    peer-mute: ${shorten(r)}`);
    await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier2InteractionKeyframe() {
  return runFlow("1to1/interaction-keyframe", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, true);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, true);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2500);
    const r = await sendKeyframeRequest(SESSIONS.CALLER);
    console.log(`    keyframe: ${shorten(r)}`);
    await sleep(1500);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

// -------- Tier 3 — server-emitted / passive ---------------------------

async function tier3ServerAcksReceipts() {
  // A full accepted+terminated 1:1 call drains the entire ack/receipt
  // chain (offer_receipt, offer_ack, preaccept, accept_receipt, accept_ack,
  // and the matching client-side acks for each).
  return runFlow("1to1/server-acks-and-receipts", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(5000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    await sleep(2000);
    return placed;
  });
}

async function tier3ServerRelaySignals() {
  // Holding an accepted call for ~10 seconds drains the relaylatency
  // round-robin probe AND the relayelection result the server sends.
  return runFlow("1to1/server-relay-signals", [SESSIONS.CALLER, SESSIONS.CALLEE], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(10000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier3OfferNoticeMissed() {
  // Callee offline at offer time → WA queues an offer_notice and delivers
  // it as <call><offer_notice ...> on next connect. The window between
  // caller's offer and callee's reconnect must be short enough that the
  // server still has the notice but long enough that we caught the offer
  // hitting the wire and the caller terminating.
  banner("1to1/offer-notice-missed");
  await resetAndEnable([SESSIONS.CALLER]);
  // Stop the callee BEFORE the offer goes out so it's offline on the wire.
  try { await callTool("web_live_stop_session", { sessionId: SESSIONS.CALLEE }); } catch (_) {}
  await clearStanzaCapture(SESSIONS.CALLER);
  const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
  console.log(`    place (callee offline): ${shorten(placed)}`);
  // Wait briefly for the offer to dispatch (and for the server to detect
  // the offline callee and queue the notice).
  await sleep(2500);
  const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 5000);
  if (cid) {
    console.log(`    caller endCall(${cid}) — produces missed-call status on the queued notice`);
    await endCall(SESSIONS.CALLER, cid);
  }
  // Start callee back up — the offline-queued offer_notice should fly
  // shortly after the WS handshake completes.
  await resetSession(SESSIONS.CALLEE);
  await ensureVoipEnabled(SESSIONS.CALLEE);
  // Poll callee's inbound buffer for an offer_notice payload; bail early
  // once we see it. Offline-delivery on linked web sessions is server-
  // controlled — we wait up to 90 seconds total since some snapshots
  // hold the notice for the full server resend cycle.
  const deadline = Date.now() + 90_000;
  let noticeSeen = false;
  while (Date.now() < deadline) {
    const nodes = await callTool("web_live_stanza_query_nodes", {
      sessionId: SESSIONS.CALLEE, tag: "call", direction: "in", history: true, limit: 100,
    }).catch(() => null);
    if (Array.isArray(nodes)) {
      for (const n of nodes) {
        const xml = n?.xml || "";
        if (xml.includes("<offer_notice ")) { noticeSeen = true; break; }
      }
    }
    if (noticeSeen) break;
    await sleep(500);
  }
  console.log(`    offer_notice delivery: ${noticeSeen ? "captured" : "NOT captured within 25s"}`);
  await dumpStanzas(SESSIONS.CALLER, "1to1/offer-notice-missed",       { tag: "call", direction: "any" });
  await dumpStanzas(SESSIONS.CALLEE, "1to1/offer-notice-missed",       { tag: "call", direction: "any" });
  await dumpStanzas(SESSIONS.CALLER, "1to1/offer-notice-missed.ack",   { tag: "ack",  direction: "any" });
  await dumpStanzas(SESSIONS.CALLEE, "1to1/offer-notice-missed.ack",   { tag: "ack",  direction: "any" });
  await dumpEval(SESSIONS.CALLER, "1to1/offer-notice-missed.caller",
    `String(JSON.stringify({ result: window.__r || null, noticeSeen: ${noticeSeen} }))`);
  await dumpEval(SESSIONS.CALLEE, "1to1/offer-notice-missed.callee",
    `String(JSON.stringify({ noticeSeen: ${noticeSeen} }))`);
  return noticeSeen ? `placed; offer_notice captured` : `placed; offer_notice NOT captured`;
}

// -------- Tier 4 — group calls (third = history-sync) -----------------

// All three sessions need a shared group to call into. We create one
// from the caller side and add the callee + the third's PN/LID. If the
// group already exists from a prior run, reuse it.
//
// createGroup's participant array is NOT plain Wids — each entry must
// be a contact-shaped object with `.phoneNumber` (a userWid) and
// optionally `.lid` (a userLid wid). See WAWebGroupCreateJob source.
async function ensureGroupExists() {
  await eval_(SESSIONS.CALLER, `(() => { window.__r = 'pending'; })()`);
  await eval_(SESSIONS.CALLER, `(() => {
    (async () => {
      try {
        const W = require('WAWebWidFactory');
        const LMU = require('WAWebLidMigrationUtils');
        const CC = require('WAWebChatCollection').ChatCollection;
        // Look for any existing group whose member list matches our pair.
        const all = CC.getModelsArray ? CC.getModelsArray() : [];
        for (const c of all) {
          try {
            if (c && c.isGroup && c.groupMetadata && c.groupMetadata.participants) {
              const jids = c.groupMetadata.participants.getModelsArray().map(p => String(p.id));
              if (jids.some(j => j.includes('19153544650')) && jids.some(j => j.includes('393495089819'))) {
                window.__r = 'reuse:' + String(c.id);
                return;
              }
            }
          } catch (_) {}
        }
        // Build participant records — {phoneNumber: userWid, lid: userLid?}.
        function asParticipant(jidStr) {
          const pnWid = W.createWid(jidStr);
          const lidWid = LMU.toLid ? LMU.toLid(pnWid) : null;
          const entry = { phoneNumber: pnWid };
          if (lidWid && lidWid.isLid && lidWid.isLid()) entry.lid = lidWid;
          return entry;
        }
        const participants = [
          asParticipant(${JSON.stringify(PEERS.CALLEE_PN)}),
          asParticipant(${JSON.stringify(PEERS.THIRD_PN)}),
        ];
        const options = {
          title: 'Cobalt call test',
          memberAddMode: true,
          announce: false,
        };
        const res = await require('WAWebGroupCreateJob').createGroup(options, participants);
        // createGroup returns {wid, participants, invitedOutContacts}.
        // wid is the group WAP wid; serialise it to the <n>@g.us form.
        const wid = res && res.wid;
        const widStr = wid ? (wid._serialized || (wid.toString ? wid.toString() : String(wid))) : 'unknown';
        window.__lastGroupJid = widStr;
        window.__r = 'created:' + widStr;
      } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
    })();
    return 'fired';
  })()`);
  const r = await waitResult(SESSIONS.CALLER, 30000);
  console.log(`    ensureGroup: ${shorten(r)}`);
  // Prefer the jid stashed at createGroup time. Fall back to a participant
  // scan against both PN and LID forms (groupMetadata participants are
  // typically LID-form on this snapshot).
  let jid = await evalString(SESSIONS.CALLER, `String(window.__lastGroupJid || '')`);
  if (!jid || jid === 'unknown') {
    jid = await evalString(SESSIONS.CALLER, `(() => {
      const all = require('WAWebChatCollection').ChatCollection.getModelsArray();
      const NEEDLES = ['19153544650','39110693621863','393495089819','258252122116273'];
      for (const c of all) {
        try {
          if (c && c.isGroup && c.groupMetadata && c.groupMetadata.participants) {
            const jids = c.groupMetadata.participants.getModelsArray().map(p => String(p.id));
            const hits = NEEDLES.filter(n => jids.some(j => j.includes(n))).length;
            if (hits >= 2) return String(c.id);
          }
        } catch (_) {}
      }
      return '';
    })()`);
  }
  if (!jid) throw new Error(`could not locate created group: ${r}`);
  console.log(`    group jid: ${jid}`);
  return jid;
}

async function tier4GroupAudioOffer() {
  return runFlow("group/audio-offer", [SESSIONS.CALLER, SESSIONS.CALLEE, SESSIONS.THIRD], async () => {
    const placed = await placeGroupCall(SESSIONS.CALLER, [PEERS.CALLEE_PN, PEERS.THIRD_PN], false);
    console.log(`    place group call: ${shorten(placed)}`);
    await sleep(4000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 5000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier4GroupUpdateAdd() {
  // Start a 1:1 with callee then add the third mid-call via the
  // high-level WAWebVoipStartCall.inviteToCall(wid) entrypoint. That
  // wraps stack.inviteToCall and emits the <group_update action="add">
  // stanza.
  return runFlow("group/update-add", [SESSIONS.CALLER, SESSIONS.CALLEE, SESSIONS.THIRD], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeCall(SESSIONS.CALLER, PEERS.CALLEE_PN, false);
    await awaitAutoState(SESSIONS.CALLEE, "accepted", 20000);
    await sleep(2000);
    await eval_(SESSIONS.CALLER, `(() => { window.__r = 'pending'; })()`);
    await eval_(SESSIONS.CALLER, `(() => {
      (async () => {
        try {
          const W = require('WAWebWidFactory');
          const third = W.createWid(${JSON.stringify(PEERS.THIRD_PN)});
          await require('WAWebVoipStartCall').inviteToCall(third);
          window.__r = 'add-sent';
        } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
      })();
      return 'fired';
    })()`);
    const addRes = await waitResult(SESSIONS.CALLER, 15000);
    console.log(`    add: ${shorten(addRes)}`);
    await sleep(3000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

async function tier4GroupUpdateRemove() {
  // Start a 3-party group call, then drop the third — emits
  // <group_update action="remove">.
  return runFlow("group/update-remove", [SESSIONS.CALLER, SESSIONS.CALLEE, SESSIONS.THIRD], async () => {
    await installAutoAcceptHook(SESSIONS.CALLEE, false);
    const placed = await placeGroupCall(SESSIONS.CALLER, [PEERS.CALLEE_PN, PEERS.THIRD_PN], false);
    await sleep(5000);
    await eval_(SESSIONS.CALLER, `(() => { window.__r = 'pending'; })()`);
    await eval_(SESSIONS.CALLER, `(() => {
      (async () => {
        try {
          const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
          const W = require('WAWebWidFactory');
          const third = W.createWid(${JSON.stringify(PEERS.THIRD_PN)});
          await stack.removeCallParticipant(third);
          window.__r = 'remove-sent';
        } catch (e) { window.__r = 'ERR:' + String(e && (e.message || e)); }
      })();
      return 'fired';
    })()`);
    const rmRes = await waitResult(SESSIONS.CALLER, 10000);
    console.log(`    remove: ${shorten(rmRes)}`);
    await sleep(3000);
    const cid = await awaitOutgoingOfferCallId(SESSIONS.CALLER, 1000);
    if (cid) await endCall(SESSIONS.CALLER, cid);
    return placed;
  });
}

// -------- Main procedure ----------------------------------------------

async function ensureSessionLoggedIn(id) {
  let s = await callTool("web_live_status", { sessionId: id }).catch(() => null);
  if (!s || !s.running) {
    console.log(`    starting session "${id}"`);
    await callTool("web_live_start_session", { sessionId: id });
    const r = await callTool("web_live_wait_for_login", { sessionId: id, timeoutMs: 60_000 });
    if (!r?.success) throw new Error(`session "${id}": did not reach logged_in (state=${r?.status?.authState})`);
    s = r.status;
  }
  if (s.authState !== "logged_in") {
    throw new Error(`session "${id}" is not logged in (authState=${s.authState}) — re-pair manually before running this script`);
  }
}

async function preflightOnePair() {
  for (const id of [SESSIONS.CALLER, SESSIONS.CALLEE]) await ensureSessionLoggedIn(id);
}

async function preflightThird() {
  await ensureSessionLoggedIn(SESSIONS.THIRD);
}

async function main() {
  console.log(`==> capture-call-corpus: caller=${SESSIONS.CALLER} callee=${SESSIONS.CALLEE} third=${SESSIONS.THIRD}`);
  await preflightOnePair();
  await preflightThird();

  const summary = [];
  async function record(name, fn) {
    const started = Date.now();
    try {
      const r = await withTimeout(fn(), FLOW_TIMEOUT_MS + 70_000, `record ${name}`);
      summary.push({
        name,
        status: typeof r === "string" && r.startsWith("ERR") ? "ERR" : "ok",
        note: shorten(String(r ?? "")),
        ms: Date.now() - started,
      });
    } catch (e) {
      summary.push({ name, status: "ERR", note: shorten(String(e?.message ?? e)), ms: Date.now() - started });
    }
  }

  // ---- Tier 1 -----------------------------------------------------------
  await record("1to1/audio-offer-place",               tier1AudioOfferPlace);
  await record("1to1/video-offer-place",               tier1VideoOfferPlace);
  await record("1to1/audio-accept",                    tier1AudioAccept);
  await record("1to1/audio-reject",                    tier1AudioReject);
  await record("1to1/caller-hangup-pre-accept",        tier1CallerHangupPreAccept);
  await record("1to1/callee-terminate-post-accept",    tier1CalleeTerminatePostAccept);
  await record("1to1/mute-toggle",                     tier1MuteToggle);
  await record("1to1/video-state-toggle",              tier1VideoStateToggle);
  await record("1to1/video-upgrade-accept",            tier1VideoUpgradeAccept);
  await record("1to1/video-upgrade-reject",            tier1VideoUpgradeReject);

  // ---- Tier 2 -----------------------------------------------------------
  await record("1to1/interaction-reaction",            tier2InteractionReaction);
  await record("1to1/interaction-raise-hand",          tier2InteractionRaiseHand);
  await record("1to1/interaction-lower-hand",          tier2InteractionLowerHand);
  await record("1to1/interaction-peer-mute",           tier2InteractionPeerMute);
  await record("1to1/interaction-keyframe",            tier2InteractionKeyframe);

  // ---- Tier 3 -----------------------------------------------------------
  await record("1to1/server-acks-and-receipts",        tier3ServerAcksReceipts);
  await record("1to1/server-relay-signals",            tier3ServerRelaySignals);
  await record("1to1/offer-notice-missed",             tier3OfferNoticeMissed);

  // ---- Tier 4 -----------------------------------------------------------
  // No pre-flight group needed — startWAWebVoipGroupCallFromWids takes
  // a participant array directly and the wasm engine derives the call's
  // group context internally.
  await record("group/audio-offer",     tier4GroupAudioOffer);
  await record("group/update-add",      tier4GroupUpdateAdd);
  await record("group/update-remove",   tier4GroupUpdateRemove);

  // ---- Codec assertion: scan every captured .jsonl on disk -------------
  // Each <call><offer> stanza must advertise only `enc="opus"` in its
  // <audio> children. If a future WA Web snapshot ships a different codec
  // (MLow etc.), we want to fail loudly during the capture run rather
  // than ship fixtures Cobalt can't replay end-to-end.
  console.log("\n========== Codec assertion ==========");
  await record("codec-assertion", async () => {
    const fs = await import("node:fs/promises");
    const path = await import("node:path");
    const root = "tools/web/mcp-server/data/captures";
    let offersChecked = 0;
    let nonOpus = [];
    async function walk(dir) {
      let entries;
      try { entries = await fs.readdir(dir, { withFileTypes: true }); }
      catch (_) { return; }
      for (const e of entries) {
        const p = path.join(dir, e.name);
        if (e.isDirectory()) { await walk(p); continue; }
        if (!e.isFile() || !e.name.endsWith(".jsonl")) continue;
        const txt = await fs.readFile(p, "utf8");
        for (const line of txt.split(/\r?\n/)) {
          if (!line.trim()) continue;
          let obj; try { obj = JSON.parse(line); } catch { continue; }
          const ev = obj.event || obj;
          const xml = ev && ev.xml;
          if (!xml || !xml.includes("<offer ")) continue;
          // Find the offer block, then collect every <audio enc="..."> attr.
          const offerStart = xml.indexOf("<offer ");
          const offerEnd = xml.indexOf("</offer>", offerStart);
          if (offerEnd < 0) continue;
          const offerXml = xml.slice(offerStart, offerEnd);
          const audios = [...offerXml.matchAll(/<audio\b[^/]*\benc="([^"]*)"/g)].map(m => m[1]);
          if (audios.length === 0) continue;
          offersChecked++;
          for (const enc of audios) {
            if (enc !== "opus") nonOpus.push({ file: p, enc });
          }
        }
      }
    }
    await walk(root);
    const summary = `checked=${offersChecked} non-opus=${nonOpus.length}`;
    console.log(`    ${summary}`);
    if (nonOpus.length > 0) {
      const sample = nonOpus.slice(0, 3).map(x => `${x.file}: enc=${x.enc}`).join("; ");
      throw new Error(`offers carry non-opus codec advertisements — WA Web snapshot changed; investigate before regenerating fixtures. samples: ${sample}`);
    }
    if (offersChecked === 0) {
      throw new Error("no captured offers found anywhere under data/captures — every Tier-1 flow's capture is empty");
    }
    return summary;
  });

  // ---- Summary -----------------------------------------------------------
  console.log("\n========== Summary ==========");
  for (const { name, status, note, ms } of summary) {
    const sec = ms != null ? `${(ms / 1000).toFixed(1)}s` : "";
    console.log(`  ${status === "ok" ? "✓" : "✗"} ${name.padEnd(40)} [${sec.padStart(6)}] ${note}`);
  }
  console.log("\n==> done. Per-session captures in data/captures/<sessionId>/call/");
  console.log("        Curate into modules/lib/src/test/resources/fixtures/call/");
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  process.exit(1);
});
