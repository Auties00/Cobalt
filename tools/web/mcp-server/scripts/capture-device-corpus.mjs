#!/usr/bin/env node
//
// Drives a logged-in WA Web session through a deterministic sequence and
// captures the device-package fixture corpus. Output goes to
// data/captures/<sessionId>/device/<topic>.{jsonl,expected.json} and is later
// curated into modules/lib/src/test/resources/fixtures/device/.
//
// Prerequisites:
//   - MCP server running in HTTP mode on http://localhost:8787 (default).
//   - The named session (default: "business") is registered and logged in.
//
// Usage:
//   node scripts/capture-device-corpus.mjs --session=business [--target=<jid>]
//

import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const args = parseArgs(process.argv.slice(2));
const sessionId = args.session ?? "business";
const targetJid = args.target ?? null;

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
  // StreamableHTTP can come back as text/event-stream; pull the first JSON data: payload.
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

// -----------------------------------------------------------------------------
// Procedure
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
  // WAWebApiDeviceList.getDevices is the canonical entry; force a fresh fetch.
  const expr = `(() => {
    window.__r = 'pending';
    const dl = require('WAWebApiDeviceList');
    dl.getDevices([${JSON.stringify(target)}], 'message', null, true).then(r => {
      window.__r = JSON.stringify(r, (k, v) => v && v.toString && k.indexOf('id') === 0 ? String(v) : v);
    }, e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  // Give the session a beat to issue & receive the IQ
  await new Promise((r) => setTimeout(r, 1500));
  await dumpStanzas(`usync-${label}`, { direction: "any", tag: "iq", query: "usync" });
  await dumpEval(`usync-${label}.expected`, `String(window.__r)`);
}

async function captureSelfChatRouting(self) {
  // The PN→LID rewrite that broke self-send originated here.
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
  // For a small set of synthetic device lists, ask WA Web to compute the phash.
  // The Cobalt phashV2 test asserts byte-equality against these.
  const samples = [
    { tag: "single-primary", devices: ["19254863482@s.whatsapp.net"] },
    { tag: "two-companions", devices: ["19254863482:1@s.whatsapp.net", "19254863482:2@s.whatsapp.net"] },
    {
      tag: "mixed-pn-lid",
      devices: ["19254863482@s.whatsapp.net", "83116928594056:1@lid"],
    },
  ];
  for (const sample of samples) {
    const expr = `(() => {
      const ph = require('WAWebPhashUtils');
      const WidFactory = require('WAWebWidFactory');
      const devices = ${JSON.stringify(sample.devices)}.map(s => WidFactory.createWid(s));
      return JSON.stringify({
        devices: ${JSON.stringify(sample.devices)},
        phashV1: ph.phashV1(devices),
        phashV2: ph.phashV2(devices),
      });
    })()`;
    await dumpEval(`phash-${sample.tag}.expected`, expr);
  }
}

async function main() {
  console.log(`==> capture-device-corpus: session=${sessionId}`);
  const tools = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!tools) {
    throw new Error(`session "${sessionId}" not reachable via MCP at ${MCP_URL}`);
  }

  // 1. Self identity baseline (every other capture references it).
  const self = await captureSelfIdentity();
  console.log(`    self.mePn=${self.mePn} self.meLid=${self.meLid}`);

  // 2. USync corpora.
  await captureUSync("self", self.mePn);
  if (targetJid) await captureUSync("other-contact", targetJid);
  await captureUSync("nonexistent", "10000000000@s.whatsapp.net");

  // 3. Self-chat PN→LID routing oracle.
  await captureSelfChatRouting(self);

  // 4. Phash oracle samples.
  await capturePhashSamples();

  // 5. Operator-driven stanzas (ADV notifications, pre-key depletion).
  await prompt(
    "Link a companion device via QR on the live session, wait for the device-list notification, then continue."
  );
  await dumpStanzas("adv-notification-link", {
    direction: "in",
    tag: "notification",
    attrs: { type: "devices" },
  });

  await prompt("Unlink that companion now, wait for the removal notification, then continue.");
  await dumpStanzas("adv-notification-unlink", {
    direction: "in",
    tag: "notification",
    attrs: { type: "devices" },
  });

  console.log("==> done. Curate the outputs under data/captures/ into src/test/resources/fixtures/device/.");
  rl.close();
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  rl.close();
  process.exit(1);
});
