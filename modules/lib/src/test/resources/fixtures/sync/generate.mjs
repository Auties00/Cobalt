#!/usr/bin/env node
//
// Drives a logged-in WA Web session through a deterministic sequence and
// captures the sync-package fixture corpus. Output goes to
// data/captures/<sessionId>/sync/<topic>.{jsonl,expected.json,synckey.bin}
// and is later curated into modules/lib/src/test/resources/fixtures/sync/.
//
// Prerequisites:
//   - MCP server running in HTTP mode on http://localhost:8787 (default).
//   - The named session (default: "business") is registered and logged in.
//
// Usage:
//   node scripts/capture-sync-corpus.mjs --session=business [--phase=<n>] [--topic=<name>]
//
// Phases (mirror the test plan):
//   2  crypto KAT vectors           — eval-only, no user interaction needed
//   3  exchange (sync IQ pairs)     — requires user to trigger actions in the UI
//   4  key management               — requires user to trigger key rotation
//   5  orchestration                — incidental, captured via phases 3 + 6
//   6  history sync                 — requires fresh companion link
//   7  per-handler                  — requires user to exercise each action
//
// Each phase can be skipped with --skip=<phase>. Use --phase=<n> to run only one.
//
// This script never edits the fixture directory directly. After it runs, you
// curate the outputs from data/captures/<sessionId>/sync/ into
// modules/lib/src/test/resources/fixtures/sync/, committing only the topics
// you want covered.

import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const args = parseArgs(process.argv.slice(2));
const sessionId = args.session ?? "business";
const onlyPhase = args.phase ? Number(args.phase) : null;
const onlyTopic = args.topic ?? null;
const skipPhases = new Set((args.skip ?? "").split(",").filter(Boolean).map(Number));

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
  const text = await response.text();
  const body = parseSseOrJson(text);
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
  try {
    return JSON.parse(part.text);
  } catch {
    return part.text;
  }
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

async function clearCapture() {
  await callTool("web_live_clear_capture", { sessionId, domain: "stanza", scope: "history" });
}

async function dumpStanzas(topic, filter) {
  return callTool("web_live_stanza_dump_to_file", {
    sessionId,
    topic: `sync/${topic}`,
    history: true,
    limit: 1000,
    ...filter,
  });
}

async function dumpEval(topic, expression) {
  return callTool("web_live_debug_eval_to_file", {
    sessionId,
    topic: `sync/${topic}`,
    expression,
  });
}

async function eval_(expression) {
  const r = await callTool("web_live_debug_eval", { sessionId, expression });
  return r?.value ?? r;
}

function shouldRun(phase, topic) {
  if (skipPhases.has(phase)) return false;
  if (onlyPhase !== null && onlyPhase !== phase) return false;
  if (onlyTopic !== null && !topic.includes(onlyTopic)) return false;
  return true;
}

const captured = [];
const skipped = [];

async function topic(phase, name, fn) {
  if (!shouldRun(phase, name)) {
    skipped.push({ phase, name });
    return;
  }
  console.log(`\n[phase ${phase}] capturing ${name}`);
  try {
    await fn();
    captured.push({ phase, name });
  } catch (e) {
    console.error(`    FAILED: ${e.message}`);
    skipped.push({ phase, name, error: e.message });
  }
}

// -----------------------------------------------------------------------------
// Phase 2 — Crypto KAT
//
// Eval-only oracles that don't need any UI exercise. We feed deterministic
// inputs (sync keys, plaintexts, IVs) into WAWebSyncd* and capture the expected
// outputs so the Java side can KAT them.
// -----------------------------------------------------------------------------

async function phase2_mutationKeys() {
  // Drive WAWebSyncdCryptoHelper.generateEncryptionKeysUnmemoized over a small
  // family of sync keys (all-zero, all-0xFF, a deterministic pattern). The
  // result is the five 32-byte derived keys; Cobalt's MutationKeys.ofSyncKey
  // must produce byte-identical slices.
  const expr = `JSON.stringify((() => {
    const helper = require('WAWebSyncdCryptoHelper');
    const samples = {
      'zero':    new Uint8Array(32),
      'ones':    new Uint8Array(32).fill(0xFF),
      'pattern': new Uint8Array(Array.from({length: 32}, (_, i) => i * 7 & 0xFF)),
    };
    const out = {};
    for (const [tag, k] of Object.entries(samples)) {
      const keys = helper.generateEncryptionKeysUnmemoized(k);
      out[tag] = {
        syncKey:            btoa(String.fromCharCode(...k)),
        indexKey:           btoa(String.fromCharCode(...new Uint8Array(keys.indexKey))),
        valueEncryptionKey: btoa(String.fromCharCode(...new Uint8Array(keys.valueEncryptionKey))),
        valueMacKey:        btoa(String.fromCharCode(...new Uint8Array(keys.valueMacKey))),
        snapshotMacKey:     btoa(String.fromCharCode(...new Uint8Array(keys.snapshotMacKey))),
        patchMacKey:        btoa(String.fromCharCode(...new Uint8Array(keys.patchMacKey))),
      };
    }
    return out;
  })())`;
  await dumpEval("crypto/mutation-keys.expected", expr);
}

async function phase2_associatedData() {
  // Drive WAWebSyncdMutationsCryptoUtils.generateAssociatedData over SET and
  // REMOVE with a known key id.
  const expr = `JSON.stringify((() => {
    const u = require('WAWebSyncdMutationsCryptoUtils');
    const keyId = new Uint8Array([0xAA, 0xBB, 0xCC, 0xDD]);
    return {
      set:    btoa(String.fromCharCode(...new Uint8Array(u.generateAssociatedData('set', keyId)))),
      remove: btoa(String.fromCharCode(...new Uint8Array(u.generateAssociatedData('remove', keyId)))),
      keyId:  btoa(String.fromCharCode(...keyId)),
    };
  })())`;
  await dumpEval("crypto/associated-data.expected", expr);
}

async function phase2_cipherText() {
  // Drive WAWebSyncdMutationsCryptoUtils.generateCipherText with a fixed key,
  // IV, and plaintext. Cobalt's MutationKeys.generateCipherText(iv, plaintext)
  // must produce byte-identical [iv || ciphertext] output.
  const expr = `JSON.stringify((async () => {
    const u = require('WAWebSyncdMutationsCryptoUtils');
    const helper = require('WAWebSyncdCryptoHelper');
    const k = new Uint8Array(32).fill(0x42);
    const keys = helper.generateEncryptionKeysUnmemoized(k);
    const iv  = new Uint8Array(16).fill(0x11);
    const pt  = new Uint8Array(64).fill(0x33);
    const ct  = await u.generateCipherText(keys.valueEncryptionKey, iv, pt);
    return {
      syncKey:    btoa(String.fromCharCode(...k)),
      iv:         btoa(String.fromCharCode(...iv)),
      plaintext:  btoa(String.fromCharCode(...pt)),
      ciphertext: btoa(String.fromCharCode(...new Uint8Array(ct))),
    };
  })())`;
  // NB: generateCipherText returns a Promise; some MCP servers wrap async eval.
  await dumpEval("crypto/cipher-text.expected", expr);
}

async function phase2_mac() {
  const expr = `JSON.stringify((async () => {
    const u = require('WAWebSyncdMutationsCryptoUtils');
    const helper = require('WAWebSyncdCryptoHelper');
    const k = new Uint8Array(32).fill(0x42);
    const keys = helper.generateEncryptionKeysUnmemoized(k);
    const ad = new Uint8Array([0x01, 0xAA, 0xBB, 0xCC, 0xDD]);
    const ct = new Uint8Array(80).fill(0x55);
    const mac = await u.generateMac(keys.valueMacKey, ad, ct);
    return {
      syncKey:        btoa(String.fromCharCode(...k)),
      associatedData: btoa(String.fromCharCode(...ad)),
      ciphertext:     btoa(String.fromCharCode(...ct)),
      mac:            btoa(String.fromCharCode(...new Uint8Array(mac))),
    };
  })())`;
  await dumpEval("crypto/mac.expected", expr);
}

async function phase2_ltHash() {
  // LT-Hash is set-homomorphic; capture a few canonical adds and an inverse
  // (add then remove) so the Java side can KAT both directions.
  const expr = `JSON.stringify((async () => {
    const lt = require('WAWebSyncdMacUtils');
    const sample = (bytes) => new Uint8Array(bytes);
    const a = sample([1,2,3,4]);
    const b = sample([5,6,7,8]);
    const c = sample([9,10,11,12]);
    const empty = new Uint8Array(128);
    const afterAB  = await lt.subtractThenAdd(empty, [a, b], []);
    const afterABC = await lt.subtractThenAdd(afterAB, [c], []);
    const afterABminusA = await lt.subtractThenAdd(afterAB, [], [a]);
    return {
      empty:          btoa(String.fromCharCode(...empty)),
      a:              btoa(String.fromCharCode(...a)),
      b:              btoa(String.fromCharCode(...b)),
      c:              btoa(String.fromCharCode(...c)),
      afterAB:        btoa(String.fromCharCode(...new Uint8Array(afterAB))),
      afterABC:       btoa(String.fromCharCode(...new Uint8Array(afterABC))),
      afterABminusA:  btoa(String.fromCharCode(...new Uint8Array(afterABminusA))),
    };
  })())`;
  await dumpEval("crypto/lt-hash.expected", expr);
}

// -----------------------------------------------------------------------------
// Phase 3 — Exchange (sync IQ pairs)
//
// Each topic captures one round-trip: an outgoing <iq xmlns="w:sync:app:state">
// upload + the server's response. The user triggers a real action in the
// browser; the MCP stanza logger records the pair.
// -----------------------------------------------------------------------------

async function phase3_uploadArchive() {
  await clearCapture();
  await prompt("In the WA Web UI, archive a chat. The session will send an outgoing sync IQ.");
  await dumpStanzas("exchange/regular-low/upload-archive", { tag: "iq", attrs: { xmlns: "w:sync:app:state" } });
}

async function phase3_uploadPin() {
  await clearCapture();
  await prompt("Pin a chat in the WA Web UI.");
  await dumpStanzas("exchange/regular-low/upload-pin", { tag: "iq", attrs: { xmlns: "w:sync:app:state" } });
}

async function phase3_uploadMute() {
  await clearCapture();
  await prompt("Mute a chat for 8 hours in the WA Web UI.");
  await dumpStanzas("exchange/regular-high/upload-mute", { tag: "iq", attrs: { xmlns: "w:sync:app:state" } });
}

async function phase3_downloadOnLink() {
  await clearCapture();
  await prompt("Re-link a fresh companion device (or run the next full sync). The server will reply with snapshots for all collections.");
  await dumpStanzas("exchange/initial-bootstrap", { tag: "iq", attrs: { xmlns: "w:sync:app:state" } });
}

// -----------------------------------------------------------------------------
// Phase 4 — Key management
// -----------------------------------------------------------------------------

async function phase4_keyShare() {
  await clearCapture();
  await prompt(
    "Trigger an app-state-sync key share. The simplest path: link a fresh companion device, then continue."
  );
  await dumpStanzas("key/share-broadcast", { tag: "message", attrs: { type: "text" } });
}

async function phase4_missingKeyRequest() {
  await clearCapture();
  await prompt(
    "Force a missing-key request: log in on a companion with a wiped app-state DB, or unlink+relink, then continue."
  );
  await dumpStanzas("key/missing-request", { tag: "message", attrs: { type: "text" } });
}

async function phase4_timeoutCurve() {
  const expr = `JSON.stringify((() => {
    const m = require('WAWebSyncdMissingKeys');
    // Capture the backoff curve constants. Exact symbols depend on the bundle
    // revision; record whatever scheduleTimeoutCheck reveals via toString().
    return { source: String(m.scheduleTimeoutCheck) };
  })())`;
  await dumpEval("key/missing-timeout-curve.expected", expr);
}

// -----------------------------------------------------------------------------
// Phase 6 — History sync
// -----------------------------------------------------------------------------

async function phase6_historyChunk(chunkType) {
  await clearCapture();
  await prompt(
    `Trigger a ${chunkType} history-sync chunk. Initial bootstrap and recent fire automatically after re-link; PUSH_NAME fires on contact name changes; FULL fires via "Request account info" in WA settings.`
  );
  await dumpStanzas(`history/${chunkType.toLowerCase().replace(/_/g, "-")}`, {
    tag: "notification",
    attrs: { type: "server_sync" },
  });
}

// -----------------------------------------------------------------------------
// Phase 7 — Per-handler (subset; expand as tests come online)
//
// Each handler test asserts WA Web byte-parity for the action's protobuf
// encoding. We capture an oracle that emits the encoded SyncActionValue for a
// known instance via WAWebSyncActionMutations.encode.
// -----------------------------------------------------------------------------

const HANDLER_ORACLES = [
  // bucket/handler-name → expression that builds and encodes the action
  {
    name: "handler/archive-chat/encode",
    expr: `JSON.stringify((() => {
      const m = require('WAWebSyncActionMutations');
      const enc = m.SyncActionValue.encode({
        timestamp: 1700000000000,
        archiveChatAction: { archived: true, messageRange: { lastMessageTimestamp: 1700000000000, messages: [] } }
      }).finish();
      return { base64: btoa(String.fromCharCode(...enc)) };
    })())`,
  },
  {
    name: "handler/pin-chat/encode",
    expr: `JSON.stringify((() => {
      const m = require('WAWebSyncActionMutations');
      const enc = m.SyncActionValue.encode({
        timestamp: 1700000000000,
        pinAction: { pinned: true }
      }).finish();
      return { base64: btoa(String.fromCharCode(...enc)) };
    })())`,
  },
  {
    name: "handler/mute-chat/encode",
    expr: `JSON.stringify((() => {
      const m = require('WAWebSyncActionMutations');
      const enc = m.SyncActionValue.encode({
        timestamp: 1700000000000,
        muteAction: { muted: true, muteEndTimestamp: 1700028800000, autoMuted: false }
      }).finish();
      return { base64: btoa(String.fromCharCode(...enc)) };
    })())`,
  },
  // TODO: extend to all 76 handlers as their tests come online. Pattern is
  // identical: pick a deterministic field set, encode via SyncActionValue,
  // emit { base64 }. Cobalt's handler test then constructs the same instance
  // via the *Action builder and asserts byte equality.
];

async function phase7_handlerEncodings() {
  for (const { name, expr } of HANDLER_ORACLES) {
    if (!shouldRun(7, name)) {
      skipped.push({ phase: 7, name });
      continue;
    }
    console.log(`\n[phase 7] capturing ${name}`);
    try {
      await dumpEval(`${name}.expected`, expr);
      captured.push({ phase: 7, name });
    } catch (e) {
      console.error(`    FAILED: ${e.message}`);
      skipped.push({ phase: 7, name, error: e.message });
    }
  }
}

// -----------------------------------------------------------------------------
// Main
// -----------------------------------------------------------------------------

async function main() {
  console.log(`==> capture-sync-corpus: session=${sessionId}`);
  const status = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!status) {
    throw new Error(`session "${sessionId}" not reachable via MCP at ${MCP_URL}`);
  }

  // Phase 2 — crypto oracles (eval-only)
  await topic(2, "crypto/mutation-keys",   phase2_mutationKeys);
  await topic(2, "crypto/associated-data", phase2_associatedData);
  await topic(2, "crypto/cipher-text",     phase2_cipherText);
  await topic(2, "crypto/mac",             phase2_mac);
  await topic(2, "crypto/lt-hash",         phase2_ltHash);

  // Phase 3 — exchange (interactive)
  await topic(3, "exchange/regular-low/upload-archive",  phase3_uploadArchive);
  await topic(3, "exchange/regular-low/upload-pin",      phase3_uploadPin);
  await topic(3, "exchange/regular-high/upload-mute",    phase3_uploadMute);
  await topic(3, "exchange/initial-bootstrap",           phase3_downloadOnLink);

  // Phase 4 — key management (interactive)
  await topic(4, "key/share-broadcast",        phase4_keyShare);
  await topic(4, "key/missing-request",        phase4_missingKeyRequest);
  await topic(4, "key/missing-timeout-curve",  phase4_timeoutCurve);

  // Phase 6 — history sync (interactive)
  await topic(6, "history/initial-bootstrap", () => phase6_historyChunk("INITIAL_BOOTSTRAP"));
  await topic(6, "history/recent",            () => phase6_historyChunk("RECENT"));
  await topic(6, "history/push-name",         () => phase6_historyChunk("PUSH_NAME"));
  await topic(6, "history/full",              () => phase6_historyChunk("FULL"));
  await topic(6, "history/on-demand",         () => phase6_historyChunk("ON_DEMAND"));

  // Phase 7 — per-handler encodings (eval-only; subset)
  await phase7_handlerEncodings();

  // Summary
  console.log(`\n==> done.`);
  console.log(`    captured (${captured.length}):`);
  for (const c of captured) console.log(`      [phase ${c.phase}] ${c.name}`);
  console.log(`    skipped (${skipped.length}):`);
  for (const s of skipped) console.log(`      [phase ${s.phase}] ${s.name}${s.error ? "  (" + s.error + ")" : ""}`);
  console.log(`\n    Curate the outputs under data/captures/${sessionId}/sync/ into modules/lib/src/test/resources/fixtures/sync/.`);
  rl.close();
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  rl.close();
  process.exit(1);
});
