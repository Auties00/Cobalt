#!/usr/bin/env node
//
// Drives a logged-in WA Web session through a deterministic sequence and
// captures the message-package fixture corpus. Output goes to
// data/captures/<sessionId>/message/<topic>.{jsonl,expected.json} and is
// later curated into modules/lib/src/test/resources/fixtures/message/.
//
// This driver is fully unattended — every step runs without user prompts.
// Real messages are dispatched to the consented peer roster locked in
// plans/tender-finding-crescent.md. Do not run against a session whose peer
// roster has not been pre-confirmed.
//
// Peer roster (constants at the top of this file):
//   * 1:1 peer       19254863482@s.whatsapp.net (consented)
//   * Small group    "Test"                       120363409745354608@g.us
//   * Large group    "Ventas y Cambios Caballeros" 120363023250764418@g.us (205 participants)
//   * Newsletter     "Newsletter"                  120363402045452944@newsletter (admin)
//   * Community      "Test1" parent                120363409813756927@g.us
//   * CAG subgroup   Test1 Announcement            120363409463700665@g.us  (defaultSubgroup, announce, 2 members)
//   * Meta AI bot                                  13135550002@s.whatsapp.net
//   * Cloud API biz  "Bose"                        44650513604847@lid
//
// Usage:
//   node scripts/capture-message-corpus.mjs --session=personal
//
// Each capture step prints a banner. The driver returns non-zero on
// unrecoverable errors.

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const args = parseArgs(process.argv.slice(2));
const sessionId = args.session ?? "personal";

// -------- Peer roster (locked) -------------------------------------------
const PEERS = Object.freeze({
  SELF_LID: "258252122116273@lid",
  SELF_PN:  "393495089819@s.whatsapp.net",
  PEER_PN:  "19254863482@s.whatsapp.net",
  PEER_LID: "83116928594056@lid",
  SMALL_GROUP:      "120363409745354608@g.us",
  LARGE_GROUP:      "120363023250764418@g.us",
  COMMUNITY_PARENT: "120363409813756927@g.us",
  CAG_SUBGROUP:     "120363409463700665@g.us",
  NEWSLETTER:       "120363402045452944@newsletter",
  META_AI:          "13135550002@s.whatsapp.net",
  HOSTED_BIZ_LID:   "44650513604847@lid",
});

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

function parseArgs(argv) {
  const out = {};
  for (const arg of argv) {
    const m = arg.match(/^--([^=]+)=(.*)$/);
    if (m) out[m[1]] = m[2];
    else if (arg.startsWith("--")) out[arg.slice(2)] = "true";
  }
  return out;
}

async function clearCapture() {
  await callTool("web_live_clear_capture", { sessionId, domain: "stanza", scope: "history" });
}

async function dumpStanzas(topic, filter) {
  return callTool("web_live_stanza_dump_to_file", {
    sessionId,
    topic: `message/${topic}`,
    history: true,
    limit: 1000,
    ...filter,
  });
}

async function dumpEval(topic, expression) {
  return callTool("web_live_debug_eval_to_file", {
    sessionId,
    topic: `message/${topic}`,
    expression,
  });
}

async function eval_(expression) {
  return callTool("web_live_debug_eval", { sessionId, expression });
}

// All async eval steps store their result on window.__r so we can pull it
// synchronously through the standard eval_ result envelope.
async function waitResult(timeoutMs = 12000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const res = await eval_(`String(window.__r)`);
    const val = res?.value ?? res;
    if (val !== "pending" && val !== "undefined") return val;
    await new Promise((r) => setTimeout(r, 250));
  }
  return "TIMEOUT";
}

const banner = (msg) => console.log(`\n========== ${msg} ==========`);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// -------- Step helpers ---------------------------------------------------

async function captureSelfIdentity() {
  banner("self-identity (baseline)");
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
  await dumpEval("self-identity", expr);
  const raw = await eval_(expr);
  return JSON.parse(raw.value ?? raw);
}

async function captureRecipientContext(topic, recipient) {
  banner(`oracle: recipient context → ${recipient}`);
  await eval_(`window.__r = 'pending'`);
  const expr = `(() => {
    const W = require('WAWebWidFactory');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const NewsletterCollection = require('WAWebNewsletterCollection');
    try {
      const wid = W.createWid(${JSON.stringify(recipient)});
      const chat = ChatCollection.get(wid)
        || (NewsletterCollection.get ? NewsletterCollection.get(wid) : null);
      const gm = chat?.groupMetadata;
      window.__r = JSON.stringify({
        recipient: ${JSON.stringify(recipient)},
        widServer: wid?.server,
        widIsLid: typeof wid?.isLid === 'function' ? wid.isLid() : null,
        widIsUser: typeof wid?.isUser === 'function' ? wid.isUser() : null,
        widIsGroup: typeof wid?.isGroup === 'function' ? wid.isGroup() : null,
        chatFound: !!chat,
        accountLid: chat?.accountLid ? String(chat.accountLid) : null,
        lidOriginType: chat?.lidOriginType ?? null,
        groupMetadata: gm ? {
          addressingMode: gm.addressingMode,
          announce: gm.announce,
          groupType: gm.groupType,
          isParentGroup: gm.isParentGroup,
          defaultSubgroup: gm.defaultSubgroup,
          parentGroup: gm.parentGroup?._serialized,
          size: gm.size,
          participantCount: gm.participants?.getModelsArray ? gm.participants.getModelsArray().length : null,
        } : null,
      });
    } catch (e) { window.__r = 'ERROR: ' + String(e); }
    return 'started';
  })()`;
  await eval_(expr);
  const result = await waitResult();
  await dumpEval(topic, `String(window.__r)`);
  return result;
}

async function captureTextSend(topic, recipient, body) {
  banner(`send text → ${recipient}    (topic=${topic})`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  // findOrCreateLatestChat returns a worker-thread snapshot of the chat —
  // a plain object that has the chat's id and a few denormalised fields,
  // but no prototype methods (isCAGAdmin, contact, etc.). The live model
  // lives on ChatCollection. Look it up by the snapshot.id and send via
  // the live model so sendTextMsgToChat can read chat.contact.businessProfile
  // and friends.
  const expr = `(() => {
    const W = require('WAWebWidFactory');
    const FindChat = require('WAWebFindChat');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const SendText = require('WAWebSendTextMsgChatAction');
    const wid = W.createWid(${JSON.stringify(recipient)});
    FindChat.findOrCreateLatestChat(wid, 'cobalt-test').then(async ({ chat: snapshot }) => {
      const liveChat = ChatCollection.get(snapshot.id);
      if (!liveChat) throw new Error('live chat not found in ChatCollection after findOrCreate');
      const msg = await SendText.sendTextMsgToChat(liveChat, ${JSON.stringify(body)});
      window.__r = JSON.stringify({
        recipient: ${JSON.stringify(recipient)},
        chatId: String(liveChat.id),
        body: ${JSON.stringify(body)},
        msgId: msg?.id?._serialized || String(msg?.id || ''),
        ack: msg?.ack,
      });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  const result = await waitResult();
  await dumpEval(topic, `String(window.__r)`);
  await dumpStanzas(topic, { direction: "any", tag: "message" });
  await dumpStanzas(`${topic}.ack`, { direction: "in", tag: "ack" });
  return result;
}

async function captureNewsletterTextSend(topic, recipient, body) {
  banner(`send newsletter text → ${recipient}    (topic=${topic})`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  // Newsletter chats live in WAWebNewsletterCollection (a singleton with
  // direct .get(wid)), NOT in WAWebChatCollection. sendNewsletterTextMsg
  // takes (newsletterModel, body, options). Pass {} as options so
  // createTextMsgData doesn't NPE on options.linkPreview.
  const expr = `(() => {
    const W = require('WAWebWidFactory');
    const NC = require('WAWebNewsletterCollection');
    const SendNewsletter = require('WAWebNewsletterSendMsgAction');
    const wid = W.createWid(${JSON.stringify(recipient)});
    const newsletter = NC.get(wid);
    if (!newsletter) { window.__r = 'ERROR: newsletter not in NewsletterCollection: ' + String(wid); return 'started'; }
    SendNewsletter.sendNewsletterTextMsg(newsletter, ${JSON.stringify(body)}, {}).then(res => {
      window.__r = JSON.stringify({
        recipient: ${JSON.stringify(recipient)},
        newsletterId: String(newsletter.id || ''),
        body: ${JSON.stringify(body)},
        result: res?.messageSendResult,
      });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  const result = await waitResult();
  await dumpEval(topic, `String(window.__r)`);
  await dumpStanzas(topic, { direction: "any", tag: "message" });
  await dumpStanzas(`${topic}.ack`, { direction: "in", tag: "ack" });
  return result;
}

async function captureReactionOnLastSent(topic, recipient, emoji = "👍") {
  banner(`send reaction "${emoji}" → ${recipient}    (topic=${topic})`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  const expr = `(() => {
    const W = require('WAWebWidFactory');
    const FindChat = require('WAWebFindChat');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const SendReaction = require('WAWebSendReactionMsgAction');
    const wid = W.createWid(${JSON.stringify(recipient)});
    FindChat.findOrCreateLatestChat(wid, 'cobalt-test').then(async ({ chat: snapshot }) => {
      const liveChat = ChatCollection.get(snapshot.id);
      if (!liveChat) throw new Error('live chat not found');
      const msgs = liveChat.msgs.getModelsArray();
      // The Msg model exposes from-me as the absence of an explicit from JID;
      // worker snapshots use id.fromMe in the WAMsgKey wrapper.
      const target = msgs.filter(m => (m.id?.fromMe ?? m.from?.isMe) === true).slice(-1)[0] || msgs.slice(-1)[0];
      if (!target) throw new Error('no msg to react to');
      const r = await SendReaction.sendReactionToMsg(target, ${JSON.stringify(emoji)});
      window.__r = JSON.stringify({ targetMsgId: String(target.id), emoji: ${JSON.stringify(emoji)}, ack: r?.ack ?? null });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  const result = await waitResult();
  await dumpEval(topic, `String(window.__r)`);
  await dumpStanzas(topic, { direction: "any", tag: "message" });
  return result;
}

async function captureEditAndRevoke(topic, recipient, body) {
  banner(`send + edit + revoke → ${recipient}    (topic=${topic})`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  // Correct edit/revoke exports (verified via static analysis of the WA
  // Web bundle):
  //   - Edit:   WAWebSendMessageEditAction.sendMessageEdit(msg, newText, opts)
  //             where opts = { groupMentions, linkPreview, mentionedJidList }.
  //   - Revoke: WAWebRevokeMsgAction.sendRevoke({type:'message', data: msg},
  //             WAWebCmd.Revoke.Sender, clearMedia=false).
  const expr = `(() => {
    const W = require('WAWebWidFactory');
    const FindChat = require('WAWebFindChat');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const SendText = require('WAWebSendTextMsgChatAction');
    const Edit = require('WAWebSendMessageEditAction');
    const Revoke = require('WAWebRevokeMsgAction');
    const Cmd = require('WAWebCmd');
    const wid = W.createWid(${JSON.stringify(recipient)});
    FindChat.findOrCreateLatestChat(wid, 'cobalt-test').then(async ({ chat: snapshot }) => {
      const liveChat = ChatCollection.get(snapshot.id);
      if (!liveChat) throw new Error('live chat not found');
      const sent = await SendText.sendTextMsgToChat(liveChat, ${JSON.stringify(body)});
      // Wait briefly so the sent msg has its server-side id resolved and is
      // findable in the local chat's msgs collection before we edit/revoke.
      await new Promise(r => setTimeout(r, 1200));
      const msgs = liveChat.msgs.getModelsArray();
      const target = msgs.filter(m => (m.id?.fromMe ?? m.from?.isMe) === true).slice(-1)[0];
      if (!target) throw new Error('no outbound msg to edit/revoke');
      // Edit. sendMessageEdit returns a promise that resolves when the edit
      // protocol-message has been wire-dispatched.
      await Edit.sendMessageEdit(target, ${JSON.stringify(body + " (edited)")}, {});
      await new Promise(r => setTimeout(r, 1500));
      // Revoke. WAWebCmd.Revoke.Sender is the enum value for a self-issued revoke.
      await Revoke.sendRevoke({ type: 'message', data: target }, Cmd.Revoke.Sender, false);
      await new Promise(r => setTimeout(r, 1500));
      window.__r = JSON.stringify({
        sentMsgId: String(target?.id || ''),
        flow: 'send+edit+revoke',
      });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`;
  await eval_(expr);
  const result = await waitResult(30000); // edit + revoke can take longer
  await dumpEval(topic, `String(window.__r)`);
  await dumpStanzas(topic, { direction: "any", tag: "message" });
  await dumpStanzas(`${topic}.ack`, { direction: "in", tag: "ack" });
  return result;
}

// -------- Status: narrow → post → revert -------------------------------

async function captureStatusPost(topic, body) {
  // Status broadcasts go through WAWebSendStatusMsgAction.sendStatusTextMsgAction
  // — NOT the regular WAWebSendTextMsgChatAction.sendTextMsgToChat path.
  // The latter doesn't recognise the status@broadcast chat as a status
  // target and produces no fanout.
  //
  // CAPTURE GAP: the resulting <message to="status@broadcast"> stanza is
  // dispatched via WAWebEncryptAndSendStatusMsg which routes through a
  // Worker thread that our main-thread CDP capture doesn't observe. The
  // .expected.json oracle is captured (msgId, result=OK) for the structural
  // test; the wire-byte test for StatusMessageSender is exercised via the
  // synthetic inputs in StatusMessageSenderTest.
  banner(`status post (text)    (topic=${topic})`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  await eval_(`(() => {
    const SendStatus = require('WAWebSendStatusMsgAction');
    SendStatus.sendStatusTextMsgAction({ text: ${JSON.stringify(body)} }).then(res => {
      window.__r = JSON.stringify({
        result: res?.messageSendResult,
        msgId: res?.msg?.id?._serialized || String(res?.msg?.id || ''),
        ack: res?.msg?.ack,
      });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`);
  const result = await waitResult(15000);
  await dumpEval(topic, `String(window.__r)`);
  await dumpStanzas(topic, { direction: "any", tag: "message" });
  return result;
}

// -------- Generic "fire and capture" eval step --------------------------

async function captureGenericEval(topic, expression, dumpTag = "message") {
  banner(`step: ${topic}`);
  await clearCapture();
  await eval_(`window.__r = 'pending'`);
  await eval_(expression);
  const result = await waitResult(15000);
  await dumpEval(topic, `String(window.__r)`);
  if (dumpTag) await dumpStanzas(topic, { direction: "out", tag: dumpTag });
  return result;
}

// -------- Main procedure -------------------------------------------------

async function main() {
  console.log(`==> capture-message-corpus: session=${sessionId}`);
  const status = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!status || !status.running) {
    throw new Error(`session "${sessionId}" not running on MCP at ${MCP_URL}`);
  }
  if (status.authState !== "logged_in") {
    throw new Error(`session "${sessionId}" is not logged in (authState=${status.authState})`);
  }

  // 1. Self-identity baseline (every other capture references it).
  const self = await captureSelfIdentity();
  console.log(`    self.mePn=${self.mePn} self.meLid=${self.meLid}`);

  // 2. Recipient-context oracles (deterministic, no sends).
  await captureRecipientContext("send/stanza/recipient-context-self-lid",     PEERS.SELF_LID);
  await captureRecipientContext("send/stanza/recipient-context-self-pn",      PEERS.SELF_PN);
  await captureRecipientContext("send/stanza/recipient-context-peer-pn",      PEERS.PEER_PN);
  await captureRecipientContext("send/stanza/recipient-context-peer-lid",     PEERS.PEER_LID);
  await captureRecipientContext("send/stanza/recipient-context-small-group",  PEERS.SMALL_GROUP);
  await captureRecipientContext("send/stanza/recipient-context-large-group",  PEERS.LARGE_GROUP);
  await captureRecipientContext("send/stanza/recipient-context-cag-subgroup", PEERS.CAG_SUBGROUP);
  await captureRecipientContext("send/stanza/recipient-context-newsletter",   PEERS.NEWSLETTER);
  await captureRecipientContext("send/stanza/recipient-context-meta-ai",      PEERS.META_AI);
  await captureRecipientContext("send/stanza/recipient-context-hosted-biz",   PEERS.HOSTED_BIZ_LID);

  // 3. 1:1 peer text — also the 479 regression oracle (LID-mode peer).
  await captureTextSend("send/peer-text", PEERS.PEER_PN, "Cobalt test 1 — text");

  // 4. 1:1 peer reaction.
  await captureReactionOnLastSent("send/peer-reaction", PEERS.PEER_PN, "👍");

  // 5. 1:1 peer edit + revoke (ProtocolMessage EDIT, REVOKE).
  await captureEditAndRevoke("send/peer-edit-revoke", PEERS.PEER_PN, "Cobalt test 2 — to be edited then revoked");

  // 6. Small group SKMSG send.
  await captureTextSend("send/group-small-text", PEERS.SMALL_GROUP, "Cobalt test — small group");

  // 7. Large group SKMSG send (205 participants — pre-approved).
  await captureTextSend("send/group-large-text", PEERS.LARGE_GROUP, "Cobalt test — large group");

  // 8. CAG announcement subgroup text + reaction (triggers
  //    EncReactionMessage path because the subgroup has defaultSubgroup=true).
  await captureTextSend("send/cag-text", PEERS.CAG_SUBGROUP, "Cobalt CAG test — text");
  await captureReactionOnLastSent("send/cag-reaction-encrypted", PEERS.CAG_SUBGROUP, "🎯");

  // 9. Newsletter plaintext SMAX send (you admin).
  await captureNewsletterTextSend("send/newsletter-text", PEERS.NEWSLETTER, "Cobalt test — newsletter");

  // 10. Meta AI bot (MSMSG outer Signal + inner AES-GCM bot secret).
  await captureTextSend("send/bot-text", PEERS.META_AI, "What is 1+1?");

  // 11. Cloud API hosted business — send "hi", passively listen for any
  //     templated/buttons reply for 10 seconds.
  await captureTextSend("send/hosted-biz-text", PEERS.HOSTED_BIZ_LID, "hi");
  console.log("    capturing inbound for ~10s in case the business sends a template/buttons reply");
  await sleep(10000);
  await dumpStanzas("receive/template-or-buttons-from-biz", { direction: "in", tag: "message" });

  // 12. Status broadcast post (uses session's current status privacy).
  await captureStatusPost("send/status-text", "Cobalt status test");

  // 13. Group invite link send to peer.
  //     WAWebGroupInviteAction.queryGroupInviteCode takes a *GroupMetadata
  //     model* (from WAWebGroupMetadataCollection), not a Wid — it reads
  //     groupMetadata.participants.iAmAdmin() synchronously and throws if
  //     either field is undefined. The returned promise resolves to nothing;
  //     the resolved invite code is written back to groupMetadata.inviteCode.
  await captureGenericEval("send/group-invite-link", `(() => {
    const W = require('WAWebWidFactory');
    const ji = require('WAWebGroupInviteAction');
    const GMD = require('WAWebGroupMetadataCollection');
    const FindChat = require('WAWebFindChat');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const Send = require('WAWebSendTextMsgChatAction');
    const groupWid = W.createWid(${JSON.stringify(PEERS.SMALL_GROUP)});
    const coll = GMD.GroupMetadataCollection || GMD.default || GMD;
    const gm = (typeof coll.get === 'function' ? coll.get(groupWid) : null);
    if (!gm) { window.__r = 'ERROR: group metadata not in collection: ' + String(groupWid); return 'started'; }
    Promise.resolve(ji.queryGroupInviteCode(gm)).then(async () => {
      const code = gm.inviteCode;
      const { chat: snapshot } = await FindChat.findOrCreateLatestChat(W.createWid(${JSON.stringify(PEERS.PEER_PN)}), 'cobalt-test');
      const liveChat = ChatCollection.get(snapshot.id);
      if (!liveChat) throw new Error('live chat not found');
      const url = 'https://chat.whatsapp.com/' + code;
      const msg = await Send.sendTextMsgToChat(liveChat, url);
      window.__r = JSON.stringify({ inviteCode: code, inviteUrl: url, msgId: msg?.id?._serialized || String(msg?.id || '') });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`);

  // 14. Static location share to peer (LocationMessage wire stanza).
  //     LiveLocationMessage is a separate UI-driven flow whose action module
  //     is lazy-loaded and not statically exported; the structural test for
  //     LiveLocationMessage covers it. Here we capture only the static
  //     LocationMessage, which is byte-equal-comparable.
  await captureGenericEval("send/location", `(() => {
    const W = require('WAWebWidFactory');
    const FindChat = require('WAWebFindChat');
    const ChatCollection = require('WAWebChatCollection').ChatCollection;
    const SendMsg = require('WAWebSendMsgChatAction');
    const MsgKey = require('WAWebMsgKey');
    const MsgType = require('WAWebMsgType');
    const Me = require('WAWebUserPrefsMeUser');
    FindChat.findOrCreateLatestChat(W.createWid(${JSON.stringify(PEERS.PEER_PN)}), 'cobalt-test').then(async ({ chat: snapshot }) => {
      const liveChat = ChatCollection.get(snapshot.id);
      if (!liveChat) throw new Error('live chat not found');
      const mePn = Me.getMaybeMePnUser();
      const id = new MsgKey.MsgKey({ from: mePn, to: liveChat.id, fromMe: true, id: MsgKey.newId(), participant: undefined });
      const msgData = {
        id,
        from: mePn,
        to: liveChat.id,
        ack: 0,
        type: MsgType.MSG_TYPE.LOCATION,
        lat: 45.0,
        lng: 7.0,
        loc: 'Cobalt test location',
        t: Math.floor(Date.now() / 1000),
        self: 'out',
        local: true,
      };
      const r = await SendMsg.addAndSendMsgToChat(liveChat, msgData);
      const sent = await (Array.isArray(r) ? r[0] : r);
      const msg = sent?.msg || sent;
      window.__r = JSON.stringify({ msgId: msg?.id?._serialized || String(msg?.id || ''), ack: msg?.ack });
    }).catch(e => { window.__r = 'ERROR: ' + String(e); });
    return 'started';
  })()`);

  // 15. WhatsApp Pay availability probe (no send; auto-defers if absent).
  await captureGenericEval("send/payment-probe", `(() => {
    try {
      const pay = require('WAWebPaymentsApi');
      const enabled = pay.isPaymentsEnabled ? pay.isPaymentsEnabled() : null;
      window.__r = JSON.stringify({ enabled, deferred: !enabled });
    } catch (e) {
      window.__r = JSON.stringify({ deferred: true, reason: String(e) });
    }
    return 'started';
  })()`, null);

  // 16. Call messages: deferred — placing a real call rings the peer's phone
  //     and there is no clean programmatic call-end API on the live runtime.
  //     The Cobalt call-message types are covered by synthetic structural
  //     tests; the receive-side CallLogMessage corpus can be captured via a
  //     separate operator-driven script if needed.
  banner("calls: deferred (no clean programmatic call-end; structural tests cover the message types)");

  // 17. Sender-key-distribution pull-out from the small-group send above.
  await dumpStanzas("send/senderkey-distribution", { direction: "out", tag: "message", query: 'type="skmsg"' });

  // 18. Ack-success corpus: all the <ack> stanzas the server returned during
  //     this run, useful for AckParser oracle tests.
  await dumpStanzas("send/ack-success-corpus", { direction: "in", tag: "ack" });

  console.log("\n==> done. Curate the outputs from data/captures/" + sessionId + "/message/");
  console.log("        into modules/lib/src/test/resources/fixtures/message/");
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  process.exit(1);
});
