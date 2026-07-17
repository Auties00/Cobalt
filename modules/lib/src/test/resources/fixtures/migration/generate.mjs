#!/usr/bin/env node
//
// Re-captures the migration-package live-oracle fixtures consumed by
// LidMigrationServiceLiveOracleTest. Each capture is an eval against the
// live WA Web JS bundle whose JSON-stringified result is written to
// MCP's data/captures/<sessionId>/migration/<topic>.expected.json and
// then curated into this directory.
//
// Prerequisites:
//   - MCP server running in HTTP mode on http://localhost:8787.
//   - A named, logged-in WA Web session is reachable.
//   - The capturing session's identity matches what the matching Java test
//     asserts against (see the SELF_PN / SELF_LID constants below).
//
// Usage:
//   node generate.mjs --session=<id>
//
// Notes:
//   - migration-status-enum.expected.json is intentionally NOT captured
//     here: WA Web exports the enum as a TS-only `$InternalEnum({})` whose
//     runtime object is empty, so the fixture is hand-curated from the
//     module source. Re-edit that file by hand if the enum drifts.
//
// All eight regenerable expressions below are extracted verbatim from the
// captured `expression` field preserved in each fixture's outer envelope.
// Keep them in sync if the matching Java test's assertion shape changes.

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const args = parseArgs(process.argv.slice(2));
const sessionId = args.session ?? "personal";
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
  const body = parseSseOrJson(await response.text());
  if (body.error) throw new Error(`tools/call ${name} -> ${body.error.message ?? JSON.stringify(body.error)}`);
  const result = body.result;
  if (result?.isError) {
    throw new Error(`tools/call ${name} -> ${result.content?.[0]?.text ?? "unknown error"}`);
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

async function dumpEval(topic, expression) {
  return callTool("web_live_debug_eval_to_file", {
    sessionId,
    topic: `migration/${topic}`,
    expression,
  });
}

// -----------------------------------------------------------------------------
// Captures
// -----------------------------------------------------------------------------

const CAPTURES = [
  {
    name: "is-regular-user",
    expression: `JSON.stringify((() => {
  const WidFactory = require('WAWebWidFactory');
  const labels = ['regular_pn', 'regular_lid', 'bot', 'announcements', 'group', 'newsletter', 'broadcast'];
  const inputs = ['393495089819@c.us', '258252122116273@lid', '867051314767696@bot', '0@c.us', '120363012345678901@g.us', '120363023456789012@newsletter', '19254863482@broadcast'];
  const out = [];
  for (let i = 0; i < inputs.length; i++) {
    try {
      const w = WidFactory.createWid(inputs[i]);
      out.push({label: labels[i], jid: String(w), isRegularUser: w.isRegularUser()});
    } catch (e) {
      out.push({label: labels[i], input: inputs[i], error: String(e && e.message || e)});
    }
  }
  return out;
})())`,
  },
  {
    name: "should-have-account-lid",
    expression: `JSON.stringify((() => {
  const Utils = require('WAWebLidMigrationUtils');
  const Gating = require('WAWebLid1X1MigrationGating');
  const WidFactory = require('WAWebWidFactory');
  const isMigrated = Gating.Lid1X1MigrationUtils.isLidMigrated();
  const labels = ['regular_pn', 'regular_lid', 'bot', 'announcements', 'group', 'newsletter', 'broadcast'];
  const inputs = ['393495089819@c.us', '258252122116273@lid', '867051314767696@bot', '0@c.us', '120363012345678901@g.us', '120363023456789012@newsletter', '19254863482@broadcast'];
  const out = [];
  for (let i = 0; i < inputs.length; i++) {
    try {
      const w = WidFactory.createWid(inputs[i]);
      out.push({label: labels[i], jid: String(w), shouldHaveAccountLid: Utils.shouldHaveAccountLid(w)});
    } catch (e) {
      out.push({label: labels[i], input: inputs[i], error: String(e && e.message || e)});
    }
  }
  return {isLidMigrated: isMigrated, cases: out};
})())`,
  },
  {
    name: "chat-is-lid",
    expression: `JSON.stringify((() => {
  const Utils = require('WAWebLidMigrationUtils');
  const WidFactory = require('WAWebWidFactory');
  const w = s => WidFactory.createWid(s);
  // Synthesise the shape that WAWebLidMigrationUtils.chatIsLid accepts: {id, groupMetadata?}.
  const cases = [
    { label: 'chat_on_lid_server', chat: { id: w('258252122116273@lid') } },
    { label: 'chat_on_pn_1on1', chat: { id: w('12025550100@c.us') } },
    { label: 'group_no_metadata', chat: { id: w('120363012345678901@g.us') } },
    { label: 'group_lid_mode', chat: { id: w('120363012345678901@g.us'), groupMetadata: { isLidAddressingMode: true } } },
    { label: 'group_pn_mode', chat: { id: w('120363012345678901@g.us'), groupMetadata: { isLidAddressingMode: false } } }
  ];
  return cases.map(c => ({
    label: c.label,
    jid: String(c.chat.id),
    chatIsLid: Utils.chatIsLid(c.chat)
  }));
})())`,
  },
  {
    name: "to-common-addressing-mode",
    expression: `JSON.stringify((() => {
  const Utils = require('WAWebLidMigrationUtils');
  const WidFactory = require('WAWebWidFactory');
  const me = require('WAWebUserPrefsMeUser');
  const w = s => WidFactory.createWid(s);
  const myPn = String(me.getMaybeMePnUser());
  const myLid = String(me.getMaybeMeLidUser());
  const pairs = [
    { label: 'pn_pn_same_server',  a: '12025550100@c.us',     b: '12025550101@c.us' },
    { label: 'lid_lid_same_server', a: '258252122116273@lid', b: '999999999999999@lid' },
    { label: 'me_pn_with_peer_lid', a: myPn,                  b: '258252122116273@lid' },
    { label: 'me_lid_with_peer_pn', a: myLid,                 b: '12025550100@c.us' }
  ];
  return {
    myPn: myPn,
    myLid: myLid,
    pairs: pairs.map(p => {
      try {
        const result = Utils.toCommonAddressingMode(w(p.a), w(p.b));
        return { label: p.label, inA: p.a, inB: p.b, outA: String(result[0]), outB: String(result[1]) };
      } catch (e) {
        return { label: p.label, error: String(e && e.message || e) };
      }
    })
  };
})())`,
  },
  {
    name: "get-alternate-msg-key-1on1",
    expression: `JSON.stringify((() => {
  const Utils = require('WAWebLidMigrationUtils');
  const MsgKey = require('WAWebMsgKey');
  const WidFactory = require('WAWebWidFactory');
  const me = require('WAWebUserPrefsMeUser');
  const w = s => WidFactory.createWid(s);
  const myPn = String(me.getMaybeMePnUser());
  const myLid = String(me.getMaybeMeLidUser());
  const cases = [
    {
      label: 'me_pn_remote_to_lid',
      input: { fromMe: true, remote: w(myPn), id: 'ABC', participant: null }
    },
    {
      label: 'me_lid_remote_to_pn',
      input: { fromMe: true, remote: w(myLid), id: 'XYZ', participant: null }
    },
    {
      label: 'unmapped_peer_pn_remote',
      input: { fromMe: false, remote: w('12025550100@c.us'), id: 'P1', participant: null }
    }
  ];
  return cases.map(c => {
    try {
      const k = new MsgKey(c.input);
      const alt = Utils.getAlternateMsgKey(k);
      return {
        label: c.label,
        inRemote: String(c.input.remote),
        outRemote: alt ? String(alt.remote) : null,
        outId: alt ? alt.id : null,
        outFromMe: alt ? alt.fromMe : null
      };
    } catch (e) {
      return { label: c.label, error: String(e && e.message || e) };
    }
  });
})())`,
  },
  {
    name: "get-alternate-msg-key-group",
    expression: `JSON.stringify((() => {
  const Utils = require('WAWebLidMigrationUtils');
  const MsgKey = require('WAWebMsgKey');
  const WidFactory = require('WAWebWidFactory');
  const me = require('WAWebUserPrefsMeUser');
  const w = s => WidFactory.createWid(s);
  const myPn = String(me.getMaybeMePnUser());
  const myLid = String(me.getMaybeMeLidUser());
  const cases = [
    {
      label: 'group_with_me_pn_participant',
      input: { fromMe: true, remote: w('120363012345678901@g.us'), id: 'G1', participant: w(myPn) }
    },
    {
      label: 'group_with_me_lid_participant',
      input: { fromMe: true, remote: w('120363012345678901@g.us'), id: 'G2', participant: w(myLid) }
    },
    {
      label: 'group_with_unmapped_participant',
      input: { fromMe: false, remote: w('120363012345678901@g.us'), id: 'G3', participant: w('12025550100@c.us') }
    },
    {
      label: 'group_with_null_participant',
      input: { fromMe: true, remote: w('120363012345678901@g.us'), id: 'G4', participant: null }
    }
  ];
  return cases.map(c => {
    try {
      const k = new MsgKey(c.input);
      const alt = Utils.getAlternateMsgKey(k);
      return {
        label: c.label,
        inParticipant: c.input.participant ? String(c.input.participant) : null,
        outRemote: alt ? String(alt.remote) : null,
        outParticipant: alt && alt.participant ? String(alt.participant) : null,
        outId: alt ? alt.id : null,
        outFromMe: alt ? alt.fromMe : null
      };
    } catch (e) {
      return { label: c.label, error: String(e && e.message || e) };
    }
  });
})())`,
  },
  {
    name: "mapping-sync-empty",
    expression: `JSON.stringify((() => {
  // Capture the parser's transformation semantics by invoking the same mapping function over a
  // synthesised payload — bypassing the gzip + protobuf byte path which is environment-dependent.
  // Replicates the body of WAWebLid1x1MigrationMsgParser.parseLidMigrationMappingSyncMsg's
  // post-decode block.
  const WidFactory = require('WAWebWidFactory');

  function transform(payload) {
    if (payload == null || payload.pnToLidMappings.length === 0) {
      return { mappings: [], primaryMigrationTsSec: null };
    }
    const i = payload.pnToLidMappings.map(e => {
      const pnUser = WidFactory.asUserWidOrThrow(WidFactory.createUserWidOrThrow(e.pn.toString()));
      const assignedLid = WidFactory.asUserWidOrThrow(WidFactory.createWid(e.assignedLid.toString() + '@lid'));
      const latestLid = (e?.latestLid) != null
        ? WidFactory.asUserWidOrThrow(WidFactory.createWid(e.latestLid.toString() + '@lid'))
        : null;
      return { pnUser: String(pnUser), assignedLid: String(assignedLid), latestLid: latestLid ? String(latestLid) : null };
    });
    return { mappings: i, primaryMigrationTsSec: payload.chatDbMigrationTimestamp };
  }

  // Case: empty payload object.
  return transform({ pnToLidMappings: [], chatDbMigrationTimestamp: null });
})())`,
  },
  {
    name: "mapping-sync-typical",
    expression: `JSON.stringify((() => {
  const WidFactory = require('WAWebWidFactory');

  function transform(payload) {
    if (payload == null || payload.pnToLidMappings.length === 0) {
      return { mappings: [], primaryMigrationTsSec: null };
    }
    const i = payload.pnToLidMappings.map(e => {
      const pnUser = WidFactory.asUserWidOrThrow(WidFactory.createUserWidOrThrow(e.pn.toString()));
      const assignedLid = WidFactory.asUserWidOrThrow(WidFactory.createWid(e.assignedLid.toString() + '@lid'));
      const latestLid = (e?.latestLid) != null
        ? WidFactory.asUserWidOrThrow(WidFactory.createWid(e.latestLid.toString() + '@lid'))
        : null;
      return { pnUser: String(pnUser), assignedLid: String(assignedLid), latestLid: latestLid ? String(latestLid) : null };
    });
    return { mappings: i, primaryMigrationTsSec: payload.chatDbMigrationTimestamp };
  }

  // Case: typical payload with two mappings — one with latestLid, one without — plus a chatDbMigrationTimestamp.
  return transform({
    pnToLidMappings: [
      { pn: 393495089819, assignedLid: 258252122116273, latestLid: 999999999999999 },
      { pn: 12025550100,  assignedLid: 555555555555555, latestLid: null }
    ],
    chatDbMigrationTimestamp: 1735680000
  });
})())`,
  },
];

async function main() {
  console.log(`==> capture-migration: session=${sessionId}`);
  const ok = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!ok) throw new Error(`session "${sessionId}" not reachable via MCP at ${MCP_URL}`);

  for (const c of CAPTURES) {
    if (onlyTopic && !c.name.includes(onlyTopic)) continue;
    console.log(`    capturing ${c.name}`);
    await dumpEval(`${c.name}.expected`, c.expression);
  }
  console.log("==> done. Curate data/captures/<sessionId>/migration/ into this directory.");
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  process.exit(1);
});
