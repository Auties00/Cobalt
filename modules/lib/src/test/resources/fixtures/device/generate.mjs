#!/usr/bin/env node
//
// Drives a logged-in WA Web session through a deterministic sequence and
// captures the device-package fixture corpus. Output goes to
// data/captures/<sessionId>/device/<topic>.{jsonl,expected.json} and is
// later curated into this directory.
//
// Prerequisites:
//   - MCP server running in HTTP mode on http://localhost:8787 (default).
//   - The named session (default: "business") is registered and logged in.
//
// Usage:
//   node generate.mjs --session=business [--target=<jid>] [--topic=<name>]
//
// Captures (Java consumer in parens):
//
//   Captured by this driver (corpus block):
//     self-identity.expected                  (DeviceFixtures, DeviceServiceTest)
//     usync-self.{jsonl,expected}             (DeviceUSyncResponseParserTest)
//     usync-other.{jsonl,expected}            (DeviceUSyncResponseParserTest, when --target supplied)
//     usync-nonexistent.{jsonl,expected}      (DeviceUSyncResponseParserTest)
//     usync-bot.{jsonl,expected}              (DeviceUSyncResponseParserTest)
//     usync-personal-self.{jsonl,expected}    (DeviceUSyncResponseParserTest)
//     usync-hosted-pn.{jsonl,expected}        (DeviceUSyncResponseParserTest)
//     usync-hosted-lid.{jsonl,expected}       (DeviceUSyncResponseParserTest)
//     usync-group-small.{jsonl,expected}      (DeviceUSyncResponseParserTest)
//     usync-group-medium.{jsonl,expected}     (DeviceUSyncResponseParserTest)
//     usync-group-large.{jsonl,expected}      (DeviceUSyncResponseParserTest)
//     self-chat-routing.expected              (SelfPnLidRoutingTest)
//     phash-samples.expected                  (DevicePhashCalculatorTest)
//     group-phashes.expected                  (DevicePhashCalculatorTest)
//     adv-notification-link.jsonl             (operator-driven)
//     adv-notification-unlink.jsonl           (operator-driven)
//     adv-self-material.expected              (DeviceADVValidatorTest [reconstructed])
//     adv-decode-self-oracle.expected         (DeviceADVValidatorTest [reconstructed])
//     icdc-oracle.expected                    (IcdcComputerTest [reconstructed])
//     prekey-single.{jsonl,expected}          (DevicePreKeyHandlerTest [reconstructed])
//     prekey-batch.{jsonl,expected}           (DevicePreKeyHandlerTest [reconstructed])
//
// The five [reconstructed] topics had their original setup-eval scripts
// lost. The setup expressions below are best-effort reconstructions
// derived from the Java test assertions and the data shape preserved in
// each fixture's `result.value`. Verify the WA Web module names against
// the active snapshot before relying on them.

import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const args = parseArgs(process.argv.slice(2));
const sessionId = args.session ?? "business";
const targetJid = args.target ?? null;
const onlyTopic = args.topic ?? null;

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
  if (!response.ok) throw new Error(`tools/call ${name} -> HTTP ${response.status}`);
  const text = await response.text();
  const body = parseSseOrJson(text);
  if (body.error) throw new Error(`tools/call ${name} -> ${body.error.message ?? JSON.stringify(body.error)}`);
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

function parseArgs(argv) {
  const out = {};
  for (const arg of argv) {
    const m = arg.match(/^--([^=]+)=(.*)$/);
    if (m) out[m[1]] = m[2];
    else if (arg.startsWith("--")) out[arg.slice(2)] = "true";
  }
  return out;
}

const rl = createInterface({ input: stdin, output: stdout });
async function prompt(message) {
  await rl.question(`\n>>> ${message}\n    Press Enter to continue (or Ctrl-C to abort): `);
}

function shouldRun(topic) {
  return !onlyTopic || topic.includes(onlyTopic);
}

async function clearCapture() {
  await callTool("web_live_clear_capture", { sessionId, domain: "stanza", scope: "history" });
}

async function dumpStanzas(topic, filter) {
  return callTool("web_live_stanza_dump_to_file", {
    sessionId,
    topic: `device/${topic}`,
    history: true,
    limit: 1000,
    ...filter,
  });
}

async function dumpEval(topic, expression) {
  return callTool("web_live_debug_eval_to_file", {
    sessionId,
    topic: `device/${topic}`,
    expression,
  });
}

async function eval_(expression) {
  return callTool("web_live_debug_eval", { sessionId, expression });
}

async function setupWindow(expression) {
  // Helper: install a `window.__<var> = JSON.stringify(...)` setup so the
  // matching capture can read it back with `String(window.__<var>)`.
  await eval_(expression);
}

// -----------------------------------------------------------------------------
// Captures
// -----------------------------------------------------------------------------

async function captureSelfIdentity() {
  const expr = `JSON.stringify((() => {
    const me = require('WAWebUserPrefsMeUser');
    return {
      mePn: String(me.getMaybeMePnUser()),
      meLid: String(me.getMaybeMeLidUser()),
      meDevicePn: String(me.getMaybeMeDevicePn()),
      meDeviceLid: String(me.getMaybeMeDeviceLid()),
      meDeviceId: me.getMaybeMeDeviceId(),
    };
  })())`;
  await dumpEval("self-identity.expected", expr);
  const raw = await eval_(expr);
  return JSON.parse(raw.value ?? raw);
}

async function captureUSync(label, target) {
  await clearCapture();
  const expr = `(() => {
    window.__r = 'pending';
    const dl = require('WAWebApiDeviceList');
    dl.getDevices([${JSON.stringify(target)}], 'message', null, true).then(r => {
      window.__r = JSON.stringify(r, (k, v) => v && v.toString && k.indexOf('id') === 0 ? String(v) : v);
    }, e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  await new Promise((r) => setTimeout(r, 1500));
  await dumpStanzas(`usync-${label}`, { direction: "any", tag: "iq", query: "usync" });
  await dumpEval(`usync-${label}.expected`, `String(window.__r)`);
}

async function captureSelfChatRouting(self) {
  const expr = `(() => {
    window.__r = 'pending';
    const fo = require('WAWebFindChat');
    fo.findOrCreateLatestChat(${JSON.stringify(self.mePn)}).then(res => {
      window.__r = JSON.stringify({
        pnInput: ${JSON.stringify(self.mePn)},
        chatId: String(res.chat.id),
        chatIsLid: typeof res.chat.id.isLid === 'function' ? res.chat.id.isLid() : null,
        chatIsUser: typeof res.chat.id.isUser === 'function' ? res.chat.id.isUser() : null,
        accountLid: res.chat.accountLid ? String(res.chat.accountLid) : null,
      });
    }, e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  await new Promise((r) => setTimeout(r, 1500));
  await dumpEval(`self-chat-routing.expected`, `String(window.__r)`);
}

async function capturePhashSamples() {
  // Consolidated single-file fixture: {tag: {devices, phashV1, phashV2}}.
  const samples = [
    { tag: "single-primary", devices: ["19254863482@s.whatsapp.net"] },
    { tag: "two-companions", devices: ["19254863482:1@s.whatsapp.net", "19254863482:2@s.whatsapp.net"] },
    { tag: "mixed-pn-lid",   devices: ["19254863482@s.whatsapp.net", "83116928594056:1@lid"] },
    { tag: "peer-and-self",  devices: ["393495089819:1@s.whatsapp.net", "19254863482:1@s.whatsapp.net"] },
  ];
  const expr = `JSON.stringify((() => {
    const ph = require('WAWebPhashUtils');
    const WidFactory = require('WAWebWidFactory');
    const samples = ${JSON.stringify(samples)};
    const out = {};
    for (const s of samples) {
      const devices = s.devices.map(d => WidFactory.createWid(d));
      out[s.tag] = { devices: s.devices, phashV1: ph.phashV1(devices), phashV2: ph.phashV2(devices) };
    }
    return out;
  })())`;
  await dumpEval("phash-samples.expected", expr);
}

async function captureGroupPhashes() {
  // Iterates every cached group metadata, captures (groupId, participantCount,
  // addressingMode, devices, phashV2). Cobalt's phashV2 must match for each
  // captured group.
  const expr = `JSON.stringify((() => {
    const ph = require('WAWebPhashUtils');
    const GMD = require('WAWebGroupMetadataCollection');
    const groups = GMD.getGroupMetadataCollection().getModelsArray
      ? GMD.getGroupMetadataCollection().getModelsArray()
      : Array.from(GMD.getGroupMetadataCollection().models || []);
    const out = [];
    for (const g of groups) {
      try {
        if (!g || !g.id || !g.participants) continue;
        const devices = g.participants.getModelsArray
          ? g.participants.getModelsArray().map(p => String(p.id))
          : [];
        if (devices.length === 0) continue;
        const wids = devices.map(d => g.participants.get(d).id);
        out.push({
          groupId: String(g.id),
          participantCount: devices.length,
          addressingMode: g.addressingMode || null,
          devices: devices,
          phashV2: ph.phashV2(wids),
        });
      } catch (e) { /* skip unrenderable groups */ }
    }
    return out;
  })())`;
  await dumpEval("group-phashes.expected", expr);
}

async function captureAdvSelfMaterial() {
  // Reads the locally stored ADV encoded identity + own JIDs. Consumed by
  // DeviceADVValidatorTest to plant the same material in a temporary store.
  // VERIFY: getAdvEncodedIdentity module name against active snapshot — the
  // ADV-side storage moved between snapshots. Search for `encodedIdentity`
  // in WAWebAdv*.js to confirm.
  const setup = `(async () => {
    const me = require('WAWebUserPrefsMeUser');
    let encodedIdentity = null;
    try {
      const Adv = require('WAWebUserPrefsAdv');
      encodedIdentity = Adv.getAdvEncodedIdentity ? Adv.getAdvEncodedIdentity() : null;
    } catch (_) { /* module may be renamed */ }
    if (!encodedIdentity) {
      try {
        const Adv = require('WAWebAdvHandlerApi');
        encodedIdentity = Adv.getEncodedIdentity ? await Adv.getEncodedIdentity() : null;
      } catch (_) { /* fall through */ }
    }
    const bytes = encodedIdentity ? new Uint8Array(encodedIdentity) : new Uint8Array(0);
    const base64 = btoa(String.fromCharCode(...bytes));
    window.__advMaterial = JSON.stringify({
      mePn: String(me.getMaybeMePnUser()),
      meLid: String(me.getMaybeMeLidUser()),
      meDev: String(me.getMaybeMeDevicePn()),
      advEncodedIdentity: { byteLength: bytes.byteLength, base64 },
    });
  })()`;
  await setupWindow(setup);
  await new Promise((r) => setTimeout(r, 500));
  await dumpEval("adv-self-material.expected", `String(window.__advMaterial)`);
}

async function captureAdvDecodeSelfOracle() {
  // Re-decodes the self signed-key-index bytes via WA Web's
  // decodeSignedKeyIndexBytes to pin the rawId / timestamp / validIndexes /
  // currentIndex / accountType against Cobalt's DeviceADVValidator.
  // VERIFY: the primary-identity-key and signed-key-index storage modules
  // against the active snapshot.
  const setup = `(async () => {
    const me = require('WAWebUserPrefsMeUser');
    const utils = require('WAWebHandleAdvDeviceNotificationUtils');
    let primaryKey = null, signedBytes = null, encodedIdentity = null;
    try {
      const Adv = require('WAWebUserPrefsAdv');
      primaryKey = Adv.getPrimaryIdentityKey ? Adv.getPrimaryIdentityKey() : null;
      signedBytes = Adv.getSignedKeyIndexBytes ? Adv.getSignedKeyIndexBytes() : null;
      encodedIdentity = Adv.getAdvEncodedIdentity ? Adv.getAdvEncodedIdentity() : null;
    } catch (_) { /* module may be renamed */ }
    const toB64 = u8 => u8 ? btoa(String.fromCharCode(...new Uint8Array(u8))) : null;
    let decoded = null;
    try {
      const mePn = me.getMaybeMePnUser();
      const r = await utils.decodeSignedKeyIndexBytes(mePn, signedBytes);
      if (r && r.value) {
        decoded = {
          rawId: r.value.rawId, timestamp: r.value.timestamp,
          validIndexes: r.value.validIndexes,
          currentIndex: r.value.currentIndex, accountType: r.value.accountType,
        };
      }
    } catch (e) { /* leave decoded null */ }
    window.__advFull = JSON.stringify({
      advEncodedIdentityBase64: toB64(encodedIdentity),
      primaryIdentityKeyBase64: toB64(primaryKey),
      primaryIdentityKeyLength: primaryKey ? new Uint8Array(primaryKey).byteLength : 0,
      signedKeyIndexBytesBase64: toB64(signedBytes),
      decoded,
    });
  })()`;
  await setupWindow(setup);
  await new Promise((r) => setTimeout(r, 1000));
  await dumpEval("adv-decode-self-oracle.expected", `String(window.__advFull)`);
}

async function captureIcdcOracle() {
  // For each well-known identity (self-PN, self-LID, hosted-PN, hosted-LID),
  // reads the ICDC metadata cached in the WA Web store. Consumed by
  // IcdcComputerTest.
  // VERIFY: ICDCMetaApi / Identity ICDC API names against active snapshot.
  const setup = `(async () => {
    const me = require('WAWebUserPrefsMeUser');
    const icdc = require('WAWebICDCMetaApi');
    const WidFactory = require('WAWebWidFactory');
    const targets = [
      { tag: 'self',       wid: me.getMaybeMePnUser() },
      { tag: 'self-lid',   wid: me.getMaybeMeLidUser() },
      { tag: 'hosted-pn',  wid: WidFactory.createWid('15086146312@c.us') },
      { tag: 'hosted-lid', wid: WidFactory.createWid('44650513604847@lid') },
    ];
    const out = [];
    for (const t of targets) {
      let meta = null;
      try {
        const m = await icdc.getIcdcMetadata(t.wid);
        if (m) {
          meta = {
            keyHashBase64: btoa(String.fromCharCode(...new Uint8Array(m.keyHash))),
            timestamp: m.timestamp,
          };
        }
      } catch (_) { /* leave meta null */ }
      out.push({ tag: t.tag, jid: String(t.wid), meta });
    }
    window.__icdcOracle = JSON.stringify(out);
  })()`;
  await setupWindow(setup);
  await new Promise((r) => setTimeout(r, 1000));
  await dumpEval("icdc-oracle.expected", `String(window.__icdcOracle)`);
}

async function capturePrekeyDepletion(label, requested) {
  // Pre-key depletion fixtures: capture the {missedPrekeyCount,
  // depletedPrekeyCount, requested?} counters that WA Web's
  // SmaxFetchMissingPreKeys path emits. Consumed by DevicePreKeyHandlerTest.
  // VERIFY: SmaxFetchMissingPreKeys vs PreKeyStore module names — the
  // depletion counter lives in the request/response handler.
  const setup = `(async () => {
    const prekey = require('WAWebApiPreKey');
    const stats = { missedPrekeyCount: 0, depletedPrekeyCount: 0, requested: ${requested} };
    // Subscribe to the depletion event for ${requested} prekey(s), then trigger.
    try {
      await prekey.requestMissingPreKeys(${requested}, stats);
    } catch (_) { /* counters are read below regardless */ }
    window.${label === "single" ? "__preKeySingle" : "__preKeyBatch"} = JSON.stringify(stats);
  })()`;
  await setupWindow(setup);
  await clearCapture();
  await new Promise((r) => setTimeout(r, 1000));
  await dumpStanzas(`prekey-${label}`, { direction: "out", tag: "iq", query: "key" });
  const readback = label === "single" ? "__preKeySingle" : "__preKeyBatch";
  await dumpEval(`prekey-${label}.expected`, `String(window.${readback})`);
}

// -----------------------------------------------------------------------------
// Main
// -----------------------------------------------------------------------------

async function main() {
  console.log(`==> capture-device-corpus: session=${sessionId}`);
  const tools = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!tools) throw new Error(`session "${sessionId}" not reachable via MCP at ${MCP_URL}`);

  // 1. Self identity baseline (every other capture references it).
  let self;
  if (shouldRun("self-identity")) {
    self = await captureSelfIdentity();
    console.log(`    self.mePn=${self.mePn} self.meLid=${self.meLid}`);
  }

  // 2. USync corpora — one capture per well-known target shape.
  if (self) {
    const usyncTargets = [
      { label: "self",          target: self.mePn },
      { label: "personal-self", target: "393495089819@s.whatsapp.net" },
      { label: "nonexistent",   target: "10000000000@s.whatsapp.net" },
      { label: "bot",           target: "13135550002@s.whatsapp.net" },
      { label: "hosted-pn",     target: "15086146312@s.whatsapp.net" },
      { label: "hosted-lid",    target: "44650513604847@lid" },
      { label: "group-small",   target: "120363409745354608@g.us" },
      { label: "group-medium",  target: "120363141955893345@g.us" },
      { label: "group-large",   target: "120363023250764418@g.us" },
    ];
    for (const t of usyncTargets) {
      if (!shouldRun(`usync-${t.label}`)) continue;
      console.log(`    capturing usync-${t.label} (target=${t.target})`);
      await captureUSync(t.label, t.target);
    }
    if (targetJid && shouldRun("usync-other")) {
      console.log(`    capturing usync-other (target=${targetJid})`);
      await captureUSync("other", targetJid);
    }
  }

  // 3. Self-chat PN -> LID routing oracle.
  if (self && shouldRun("self-chat-routing")) await captureSelfChatRouting(self);

  // 4. Phash oracles.
  if (shouldRun("phash-samples")) await capturePhashSamples();
  if (shouldRun("group-phashes")) await captureGroupPhashes();

  // 5. ADV / ICDC / pre-key reconstructed captures.
  if (shouldRun("adv-self-material")) await captureAdvSelfMaterial();
  if (shouldRun("adv-decode-self-oracle")) await captureAdvDecodeSelfOracle();
  if (shouldRun("icdc-oracle")) await captureIcdcOracle();
  if (shouldRun("prekey-single")) await capturePrekeyDepletion("single", 1);
  if (shouldRun("prekey-batch")) await capturePrekeyDepletion("batch", 8);

  // 6. Operator-driven stanzas (ADV notifications).
  if (shouldRun("adv-notification-link") && !args["skip-operator"]) {
    await prompt("Link a companion device via QR on the live session, wait for the device-list notification, then continue.");
    await dumpStanzas("adv-notification-link", { direction: "in", tag: "notification", attrs: { type: "devices" } });
  }
  if (shouldRun("adv-notification-unlink") && !args["skip-operator"]) {
    await prompt("Unlink that companion now, wait for the removal notification, then continue.");
    await dumpStanzas("adv-notification-unlink", { direction: "in", tag: "notification", attrs: { type: "devices" } });
  }

  console.log("==> done. Curate the outputs under data/captures/ into this directory.");
  rl.close();
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  rl.close();
  process.exit(1);
});
