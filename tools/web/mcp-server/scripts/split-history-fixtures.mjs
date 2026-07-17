#!/usr/bin/env node
// Splits modules/lib/src/test/resources/fixtures/sync/history/_raw/bootstrap-dump.json
// (one JSON document with notifications + chunks captured from a live WA Web
// session via the __hs_capture IDB hook) into per-syncType fixture files:
//
//   fixtures/sync/history/<typeSlug>/notification.json   (HistorySyncNotification proto json)
//   fixtures/sync/history/<typeSlug>/chunk.b64           (inflated HistorySync proto bytes, base64)
//   fixtures/sync/history/<typeSlug>/expected.json       (decoded HistorySync proto value)
//
// Notification + chunk events are paired by their adjacent timestamps.

import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, '..', '..', '..', '..');
const DUMP = resolve(REPO_ROOT, 'modules/lib/src/test/resources/fixtures/sync/history/_raw/bootstrap-dump.json');
const OUT_ROOT = resolve(REPO_ROOT, 'modules/lib/src/test/resources/fixtures/sync/history');

const TYPE_SLUG = {
  0: 'initial-bootstrap',
  1: 'initial-status-v3',
  2: 'full',
  3: 'recent',
  4: 'push-name',
  5: 'non-blocking-data',
  6: 'on-demand',
};

function main() {
  const outer = JSON.parse(readFileSync(DUMP, 'utf8'));
  // web_live_debug_eval_to_file wraps the eval result in { schema, sessionId, topic, expression, result: { resultType, value } }
  // where `value` is the raw eval return (here, a JSON-stringified payload). Unwrap both layers.
  const innerStr = outer.result && outer.result.value;
  if (typeof innerStr !== 'string') throw new Error('expected outer.result.value to be a string; got ' + typeof innerStr);
  const raw = JSON.parse(innerStr);
  const events = raw.events || [];
  const notifs = events.filter(e => e.kind === 'notification').sort((a, b) => a.ts - b.ts);
  const chunks = events.filter(e => e.kind === 'chunk').sort((a, b) => a.ts - b.ts);
  console.log(`dump: ${notifs.length} notifications, ${chunks.length} chunks`);

  if (notifs.length !== chunks.length) {
    console.warn(`warning: notification/chunk count mismatch — pairing by nearest timestamp`);
  }

  // Pair each notification with the nearest chunk after it (chunks are emitted
  // from decodeProtobuf inside handleHistorySyncChunk, so chunk.ts >= notif.ts).
  const used = new Set();
  const pairs = [];
  for (const n of notifs) {
    let best = -1;
    let bestDelta = Infinity;
    for (let i = 0; i < chunks.length; i++) {
      if (used.has(i)) continue;
      const delta = chunks[i].ts - n.ts;
      if (delta >= 0 && delta < bestDelta) {
        bestDelta = delta;
        best = i;
      }
    }
    if (best >= 0) {
      used.add(best);
      pairs.push({ notif: n, chunk: chunks[best], delta: bestDelta });
    } else {
      pairs.push({ notif: n, chunk: null });
    }
  }

  const summary = [];
  for (const { notif, chunk } of pairs) {
    const syncType = notif.payload.syncType;
    const chunkOrder = notif.payload.chunkOrder;
    const slug = TYPE_SLUG[syncType] || `unknown-${syncType}`;
    const dir = join(OUT_ROOT, slug);
    mkdirSync(dir, { recursive: true });

    // notification.json — parsed HistorySyncNotification protobuf
    const notifPayload = JSON.parse(notif.payload.notificationJson);
    writeFileSync(join(dir, 'notification.json'), JSON.stringify({
      syncType,
      chunkOrder,
      msgKey: notif.payload.msgKey,
      progress: notif.payload.progress,
      notification: notifPayload,
    }, null, 2));

    if (chunk) {
      // chunk.b64 — raw inflated HistorySync protobuf bytes (base64).
      const b64 = chunk.payload.inflatedBytesB64 || '';
      writeFileSync(join(dir, 'chunk.b64'), b64);

      // expected.json — decoded HistorySync object (parsed protobuf value).
      const decoded = JSON.parse(chunk.payload.decodedJson);
      writeFileSync(join(dir, 'expected.json'), JSON.stringify(decoded, null, 2));
    }

    const bytesLen = chunk ? Math.floor((chunk.payload.inflatedBytesB64 || '').length * 3 / 4) : 0;
    summary.push({ slug, syncType, chunkOrder, hasChunk: !!chunk, chunkBytes: bytesLen });
  }

  console.log('\nfixtures written:');
  for (const s of summary) console.log('  ' + JSON.stringify(s));
  console.log(`\nmissing chunk types: ${Object.entries(TYPE_SLUG).filter(([t]) => !summary.find(s => s.syncType === Number(t))).map(([,slug]) => slug).join(', ') || '(none)'}`);
}

main();
