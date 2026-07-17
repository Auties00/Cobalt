#!/usr/bin/env node
//
// Re-captures the WAM-package KAT fixtures by running each capture
// expression against the live WA Web bundle via MCP and writing the
// resulting JSON envelopes into data/captures/<sessionId>/wam/<name>.json
// (then curated into this directory).
//
// Eight captures:
//   - wam-tags-roundtrip           (WamTagsKatTest)
//   - wam-encoder-type-matrix      (WamEventEncoderKatTest)
//   - wam-global-encoder           (WamGlobalEncoderKatTest)
//   - wam-events-multi-field       (WamGeneratedImplKatTest)
//   - wam-buffer-headers           (WamBufferHeaderKatTest)
//   - wam-beaconing                (WamBeaconingTest)
//   - wam-event-definitions        (WamEventRegistryAuditTest)
//   - ed25519-live-bundle-vectors  (Ed25519LiveBundleKatTest) [reconstructed]
//
// Bump the SNAPSHOT_REVISION / LIVE_REVISION constants below when
// re-running against a newer WA Web bundle; the values are interpolated
// into each capture's payload header so future drift is loud, not silent.
//
// Usage:
//   node generate.mjs --session=personal [--topic=<name>]

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";

const SNAPSHOT_REVISION = 1039260921;
const LIVE_REVISION     = 1039260921;
const CAPTURED_AT       = new Date().toISOString().slice(0, 10);

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
    topic: `wam/${topic}`,
    expression,
  });
}

// -----------------------------------------------------------------------------
// Helper: header interpolation
// -----------------------------------------------------------------------------

function header(fixture, description, sourceModule, extra = {}) {
  return {
    fixture,
    description,
    snapshotRevision: SNAPSHOT_REVISION,
    liveRuntimeRevision: LIVE_REVISION,
    capturedAt: CAPTURED_AT,
    capturedVia: "mcp__whatsapp__web_live_debug_eval_to_file",
    sourceModule,
    ...extra,
  };
}

// -----------------------------------------------------------------------------
// Captures
// -----------------------------------------------------------------------------

const CAPTURES = [
  {
    name: "wam-tags-roundtrip",
    expression: wamTagsRoundtrip(),
  },
  {
    name: "wam-encoder-type-matrix",
    expression: wamEncoderTypeMatrix(),
  },
  {
    name: "wam-global-encoder",
    expression: wamGlobalEncoder(),
  },
  {
    name: "wam-events-multi-field",
    expression: wamEventsMultiField(),
  },
  {
    name: "wam-buffer-headers",
    expression: wamBufferHeaders(),
  },
  {
    name: "wam-beaconing",
    expression: wamBeaconing(),
  },
  {
    name: "wam-event-definitions",
    expression: wamEventDefinitions(),
  },
  {
    name: "ed25519-live-bundle-vectors",
    expression: ed25519LiveBundleVectors(),
    // ed25519 returns a complete JSON document, not an oracle envelope.
    bareJson: true,
  },
];

function wamTagsRoundtrip() {
  return `(async () => {
  const Protocol = require('WAWebWamLibProtocol');
  const Binary = require('WABinary').Binary;
  const PREFIX_BYTES = 32;
  const captureBytes = (fn, args) => {
    const buf = new Binary(undefined, true);
    fn(buf, ...args);
    return buf.peek(v => Array.from(v.readByteArrayView()));
  };
  const sha256Hex = async bytes => {
    const ab = new Uint8Array(bytes).buffer;
    const digest = await crypto.subtle.digest('SHA-256', ab);
    return Array.from(new Uint8Array(digest)).map(b => b.toString(16).padStart(2, '0')).join('');
  };
  const G = Protocol.writeGlobalAttribute;
  const E = (b, id, weight, hasFields) => Protocol.writeEvent(b, id, weight, hasFields);
  const F = (b, id, value, hasFollowing) => Protocol.writeField(b, id, value, hasFollowing);
  const tinyId = 5, wideId = 300;
  const rows = [
    { name: 'global_null_tiny',  fn: G, args: [tinyId, null] },
    { name: 'global_null_wide',  fn: G, args: [wideId, null] },
    { name: 'global_int0_tiny',  fn: G, args: [tinyId, 0] },
    { name: 'global_int1_tiny',  fn: G, args: [tinyId, 1] },
    { name: 'global_int8_pos',   fn: G, args: [tinyId, 5] },
    { name: 'global_int8_neg',   fn: G, args: [tinyId, -7] },
    { name: 'global_int8_min',   fn: G, args: [tinyId, -128] },
    { name: 'global_int8_max',   fn: G, args: [tinyId, 127] },
    { name: 'global_int16_pos',  fn: G, args: [tinyId, 200] },
    { name: 'global_int16_neg',  fn: G, args: [tinyId, -200] },
    { name: 'global_int16_min',  fn: G, args: [tinyId, -32768] },
    { name: 'global_int16_max',  fn: G, args: [tinyId, 32767] },
    { name: 'global_int32_pos',  fn: G, args: [tinyId, 70000] },
    { name: 'global_int32_neg',  fn: G, args: [tinyId, -70000] },
    { name: 'global_int32_min',  fn: G, args: [tinyId, -2147483648] },
    { name: 'global_int32_max',  fn: G, args: [tinyId, 2147483647] },
    { name: 'global_float_pi',   fn: G, args: [tinyId, 3.14] },
    { name: 'global_float_neg',  fn: G, args: [tinyId, -2.5] },
    { name: 'global_float_big',  fn: G, args: [tinyId, 1e15] },
    { name: 'global_float_small',fn: G, args: [tinyId, 1.5e-10] },
    { name: 'global_float_half', fn: G, args: [tinyId, 0.5] },
    { name: 'global_str_empty',  fn: G, args: [tinyId, ''] },
    { name: 'global_str_ascii',  fn: G, args: [tinyId, 'hi'] },
    { name: 'global_str_utf8',   fn: G, args: [tinyId, 'h\\u00e9llo' + String.fromCodePoint(128512)] },
    { name: 'global_str_255',    fn: G, args: [tinyId, 'a'.repeat(255)] },
    { name: 'global_str_256',    fn: G, args: [tinyId, 'a'.repeat(256)] },
    { name: 'global_str_65535',  fn: G, args: [tinyId, 'a'.repeat(65535)] },
    { name: 'global_str_65536',  fn: G, args: [tinyId, 'a'.repeat(65536)] },
    { name: 'event_no_fields_tiny',  fn: E, args: [tinyId, -100, false] },
    { name: 'event_has_fields_tiny', fn: E, args: [tinyId, -100, true] },
    { name: 'event_no_fields_wide',  fn: E, args: [wideId, -1, false] },
    { name: 'event_has_fields_wide', fn: E, args: [wideId, -1, true] },
    { name: 'field_null_last',      fn: F, args: [tinyId, null, false] },
    { name: 'field_null_continues', fn: F, args: [tinyId, null, true] },
    { name: 'field_int0_last',      fn: F, args: [tinyId, 0, false] },
    { name: 'field_int1_last',      fn: F, args: [tinyId, 1, false] },
    { name: 'field_int8_last',      fn: F, args: [tinyId, 7, false] },
    { name: 'field_int16_last',     fn: F, args: [tinyId, 200, false] },
    { name: 'field_int32_last',     fn: F, args: [tinyId, 70000, false] },
    { name: 'field_float_last',     fn: F, args: [tinyId, 3.14, false] },
    { name: 'field_str8_last',      fn: F, args: [tinyId, 'hi', false] },
    { name: 'field_str16_last',     fn: F, args: [tinyId, 'a'.repeat(300), false] },
    { name: 'field_str8_continues', fn: F, args: [tinyId, 'x', true] },
    { name: 'field_wide_int8_last', fn: F, args: [wideId, 7, false] },
    { name: 'field_wide_str8_last', fn: F, args: [wideId, 'hi', false] }
  ];
  const INLINE_LIMIT = 256;
  const vectors = [];
  for (const row of rows) {
    const bytes = captureBytes(row.fn, row.args);
    const hex = bytes.map(b => b.toString(16).padStart(2, '0')).join('');
    const summarizedArgs = row.args.map(a => typeof a === 'string' && a.length > 16
        ? { kind: 'str', length: a.length, head: a.slice(0, 8) }
        : a);
    if (bytes.length <= INLINE_LIMIT) {
      vectors.push({ name: row.name, role: row.fn.name || null, args: summarizedArgs, byteLength: bytes.length, bytes: hex });
    } else {
      vectors.push({ name: row.name, role: row.fn.name || null, args: summarizedArgs, byteLength: bytes.length, prefix: hex.slice(0, PREFIX_BYTES * 2), sha256: await sha256Hex(bytes) });
    }
  }
  return JSON.stringify({
    fixture: 'wam-tags-roundtrip',
    description: 'Byte output of WAWebWamLibProtocol.{writeGlobalAttribute,writeEvent,writeField} across role bits, LAST/WIDE_ID flags, and every value-type mask. Vectors with byteLength > 256 store prefix + sha256 instead of full bytes.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamLibProtocol',
    encoding: { inlineLimitBytes: INLINE_LIMIT, prefixBytes: PREFIX_BYTES, hashAlgorithm: 'sha-256' },
    vectors
  }, null, 2);
})()`;
}

function wamEncoderTypeMatrix() {
  return `(async () => {
  const Protocol = require('WAWebWamLibProtocol');
  const Binary = require('WABinary').Binary;
  const sha256Hex = async bytes => {
    const ab = new Uint8Array(bytes).buffer;
    const digest = await crypto.subtle.digest('SHA-256', ab);
    return Array.from(new Uint8Array(digest)).map(b => b.toString(16).padStart(2, '0')).join('');
  };
  const encodeSequence = (eventId, weight, fields) => {
    const buf = new Binary(undefined, true);
    Protocol.writeEvent(buf, eventId, weight, fields.length > 0);
    fields.forEach((f, idx) => Protocol.writeField(buf, f.id, f.value, idx < fields.length - 1));
    return buf.peek(v => Array.from(v.readByteArrayView()));
  };
  const cases = [
    { name: 'event_no_fields_negative_weight',  eventId: 100,  weight: -1,    fields: [] },
    { name: 'event_no_fields_wide_id',          eventId: 5000, weight: -100,  fields: [] },
    { name: 'event_single_int_field',           eventId: 100,  weight: -1,    fields: [{ id: 1, value: 42, type: 'int' }] },
    { name: 'event_single_str_field',           eventId: 100,  weight: -1,    fields: [{ id: 1, value: 'hello', type: 'str' }] },
    { name: 'event_single_bool_true_field',     eventId: 100,  weight: -1,    fields: [{ id: 1, value: 1, type: 'bool' }] },
    { name: 'event_single_bool_false_field',    eventId: 100,  weight: -1,    fields: [{ id: 1, value: 0, type: 'bool' }] },
    { name: 'event_single_null_field',          eventId: 100,  weight: -1,    fields: [{ id: 1, value: null, type: 'null' }] },
    { name: 'event_single_float_field',         eventId: 100,  weight: -1,    fields: [{ id: 1, value: 3.14, type: 'float' }] },
    { name: 'event_int_boundary_int0',          eventId: 100,  weight: -1,    fields: [{ id: 7, value: 0, type: 'int' }] },
    { name: 'event_int_boundary_int1',          eventId: 100,  weight: -1,    fields: [{ id: 7, value: 1, type: 'int' }] },
    { name: 'event_int_boundary_int8_max',      eventId: 100,  weight: -1,    fields: [{ id: 7, value: 127, type: 'int' }] },
    { name: 'event_int_boundary_int16_min',     eventId: 100,  weight: -1,    fields: [{ id: 7, value: -32768, type: 'int' }] },
    { name: 'event_int_boundary_int32_min',     eventId: 100,  weight: -1,    fields: [{ id: 7, value: -2147483648, type: 'int' }] },
    { name: 'event_int_boundary_int32_max',     eventId: 100,  weight: -1,    fields: [{ id: 7, value: 2147483647, type: 'int' }] },
    { name: 'event_str_empty',                  eventId: 100,  weight: -1,    fields: [{ id: 9, value: '', type: 'str' }] },
    { name: 'event_str_utf8',                   eventId: 100,  weight: -1,    fields: [{ id: 9, value: 'h\\u00e9llo' + String.fromCodePoint(128512), type: 'str' }] },
    { name: 'event_str_boundary_255',           eventId: 100,  weight: -1,    fields: [{ id: 9, value: 'a'.repeat(255), type: 'str' }] },
    { name: 'event_str_boundary_256',           eventId: 100,  weight: -1,    fields: [{ id: 9, value: 'a'.repeat(256), type: 'str' }] },
    { name: 'event_mixed_int_str_bool_null',    eventId: 100,  weight: -1,    fields: [
        { id: 1, value: 42, type: 'int' }, { id: 2, value: 'hello', type: 'str' },
        { id: 3, value: 1, type: 'bool' }, { id: 4, value: null, type: 'null' }
    ] },
    { name: 'event_dense_low_field_ids',        eventId: 100,  weight: -1,    fields: [
        { id: 1, value: 10, type: 'int' }, { id: 2, value: 20, type: 'int' },
        { id: 3, value: 30, type: 'int' }, { id: 4, value: 40, type: 'int' },
        { id: 5, value: 50, type: 'int' }
    ] },
    { name: 'event_wide_and_narrow_field_ids',  eventId: 100,  weight: -1,    fields: [
        { id: 5, value: 'narrow', type: 'str' },
        { id: 500, value: 'wide1', type: 'str' },
        { id: 50000, value: 'wide2', type: 'str' }
    ] },
    { name: 'event_many_null_fields',           eventId: 100,  weight: -1,    fields: [
        { id: 1, value: null, type: 'null' },
        { id: 2, value: null, type: 'null' },
        { id: 3, value: null, type: 'null' }
    ] },
    { name: 'event_weight_default',             eventId: 100,  weight: 0,     fields: [{ id: 1, value: 1, type: 'int' }] },
    { name: 'event_negative_weight_high_magnitude', eventId: 100, weight: -100000, fields: [{ id: 1, value: 1, type: 'int' }] }
  ];
  const INLINE_LIMIT = 256, PREFIX_BYTES = 32;
  const vectors = [];
  for (const c of cases) {
    const bytes = encodeSequence(c.eventId, c.weight, c.fields);
    const hex = bytes.map(b => b.toString(16).padStart(2, '0')).join('');
    const fieldsSummary = c.fields.map(f => typeof f.value === 'string' && f.value.length > 16
      ? { ...f, value: { kind: 'str', length: f.value.length, head: f.value.slice(0, 8) } }
      : f);
    const base = { name: c.name, eventId: c.eventId, weight: c.weight, fields: fieldsSummary, byteLength: bytes.length };
    if (bytes.length <= INLINE_LIMIT) vectors.push({ ...base, bytes: hex });
    else vectors.push({ ...base, prefix: hex.slice(0, PREFIX_BYTES * 2), sha256: await sha256Hex(bytes) });
  }
  return JSON.stringify({
    fixture: 'wam-encoder-type-matrix',
    description: 'Live JS encoding of complete event-with-fields sequences across (WamType x edge-value) combinations. Each row reproduces what Cobalt *Impl.encode emits for the same eventId, weight, and field list.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamLibProtocol',
    encoding: { inlineLimitBytes: INLINE_LIMIT, prefixBytes: PREFIX_BYTES, hashAlgorithm: 'sha-256' },
    vectors
  }, null, 2);
})()`;
}

function wamGlobalEncoder() {
  return `(() => {
  const Protocol = require('WAWebWamLibProtocol');
  const Binary = require('WABinary').Binary;
  const enc = (fieldId, value) => {
    const buf = new Binary(undefined, true);
    Protocol.writeGlobalAttribute(buf, fieldId, value);
    const arr = buf.peek(v => Array.from(v.readByteArrayView()));
    return arr.map(b => b.toString(16).padStart(2, '0')).join('');
  };
  const rows = [
    ['mnc', 3, 'int', 310], ['mcc', 5, 'int', 1], ['platform', 11, 'int', 8],
    ['deviceName', 13, 'str', 'Chrome'], ['osVersion', 15, 'str', 'macOS 14.3'],
    ['appVersion', 17, 'str', '2.3001.1039248090'], ['appIsBetaRelease', 21, 'bool', false],
    ['networkIsWifi', 23, 'bool', true], ['commitTime', 47, 'int', 1747000000],
    ['browserVersion', 295, 'str', '120.0.6099.109'], ['webcEnv', 633, 'int', 0],
    ['memClass', 655, 'int', 8192], ['yearClass', 689, 'int', 2023],
    ['webcPhonePlatform', 707, 'int', 2], ['browser', 779, 'str', 'Chrome'],
    ['webcPhoneCharging', 783, 'bool', true],
    ['webcPhoneDeviceManufacturer', 829, 'str', 'Apple'],
    ['webcPhoneDeviceModel', 831, 'str', 'iPhone 14'],
    ['webcPhoneOsBuildNumber', 833, 'str', '21D50'],
    ['webcPhoneOsVersion', 835, 'str', '17.3.1'],
    ['webcBucket', 875, 'str', 'bucket42'], ['webcWebPlatform', 899, 'int', 1],
    ['webcPhoneAppVersion', 1005, 'str', '2.24.1.78'],
    ['webcNativeBetaUpdates', 1007, 'bool', false],
    ['webcNativeAutolaunch', 1009, 'bool', true],
    ['appBuild', 1657, 'int', 4], ['yearClass2016', 2617, 'int', 2016],
    ['datacenter', 2795, 'str', 'ash'], ['beaconSessionId', 3433, 'int', 42],
    ['streamId', 3543, 'int', 1],
    ['webcTabId', 3727, 'str', '8c1d2f3e-1234-4567-89ab-cdef01234567'],
    ['abKey2', 4473, 'str', 'experiment_key_42'],
    ['deviceVersion', 4505, 'str', 'macOS 14.3'],
    ['expoKey', 5029, 'str', 'expo_test_alpha'],
    ['psId', 6005, 'str', '4f3e2d1c0b0a09080706050403020100'],
    ['ocVersion', 6251, 'int', 1],
    ['webcWebDeviceManufacturer', 6599, 'str', 'Apple'],
    ['webcWebDeviceModel', 6601, 'str', 'MacBookPro18,2'],
    ['webcWebOsReleaseNumber', 6603, 'str', '23D40'],
    ['webcWebArch', 6605, 'str', 'arm64'],
    ['psCountryCode', 6833, 'str', 'us'], ['numCpu', 10317, 'int', 12],
    ['serviceImprovementOptOut', 13293, 'bool', false],
    ['deviceClassification', 14507, 'int', 4],
    ['wametaLoggerTestFilter', 15881, 'str', 'unit_test'],
    ['webcRevision', 18491, 'int', 1039248090],
    ['isInCohort', 19129, 'bool', true],
    ['null_tinyId', 47, 'null', null], ['null_wideId', 18491, 'null', null],
    ['int_zero', 11, 'int', 0], ['int_one', 11, 'int', 1], ['int_neg', 655, 'int', -1],
    ['bool_false', 21, 'bool', false], ['bool_true', 21, 'bool', true],
    ['str_empty', 13, 'str', ''], ['str_utf8', 13, 'str', 'Ol\\u00e1 ' + String.fromCodePoint(128512)]
  ];
  const valueForCall = (type, v) => type === 'bool' ? (v ? 1 : 0) : v;
  const vectors = rows.map(([name, fieldId, type, sampleValue]) => ({
    name, fieldId, type, sampleValue,
    bytes: enc(fieldId, valueForCall(type, sampleValue))
  }));
  return JSON.stringify({
    fixture: 'wam-global-encoder',
    description: 'writeGlobalAttribute output for every named global field id in WamGlobalEncoder.java, plus boundary cases (null, zero, neg, empty, UTF-8). Cobalt WamGlobalEncoder.writeXxx must produce identical bytes for the same fieldId+value pair.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamLibProtocol',
    encoding: { booleansEmittedAsInt: true, hashAlgorithm: 'none' },
    vectors
  }, null, 2);
})()`;
}

function wamEventsMultiField() {
  return `(() => {
  const Protocol = require('WAWebWamLibProtocol');
  const Binary = require('WABinary').Binary;
  const encodeEvent = (eventId, weight, fields) => {
    const buf = new Binary(undefined, true);
    Protocol.writeEvent(buf, eventId, weight, fields.length > 0);
    fields.forEach((f, idx) => Protocol.writeField(buf, f.index, f.value, idx < fields.length - 1));
    const arr = buf.peek(v => Array.from(v.readByteArrayView()));
    return arr.map(b => b.toString(16).padStart(2, '0')).join('');
  };
  const scenarios = [
    { name: 'PsIdUpdateEvent_created', cobaltClass: 'PsIdUpdateEvent', eventId: 2862,
      channel: 'REGULAR', weight: -1,
      fields: [{ index: 1, value: 42, type: 'int' }, { index: 2, value: 1, type: 'enum' }, { index: 3, value: 7, type: 'int' }] },
    { name: 'PsIdUpdateEvent_rotated', cobaltClass: 'PsIdUpdateEvent', eventId: 2862,
      channel: 'REGULAR', weight: -1,
      fields: [{ index: 1, value: 999, type: 'int' }, { index: 2, value: 2, type: 'enum' }, { index: 3, value: 30, type: 'int' }] },
    { name: 'WamClientErrorsEvent_drop_only', cobaltClass: 'WamClientErrorsEvent', eventId: 1144,
      channel: 'REGULAR', weight: -1,
      fields: [{ index: 28, value: 1, type: 'int' }] },
    { name: 'WamClientErrorsEvent_many_counters', cobaltClass: 'WamClientErrorsEvent', eventId: 1144,
      channel: 'REGULAR', weight: -1,
      fields: [
        { index: 2, value: 5, type: 'int' }, { index: 3, value: 200, type: 'int' },
        { index: 22, value: 1, type: 'int' }, { index: 23, value: 100, type: 'int' },
        { index: 28, value: 1, type: 'int' }, { index: 1, value: 1, type: 'bool' }
      ] },
    { name: 'WamClientErrorsEvent_int32_range_field', cobaltClass: 'WamClientErrorsEvent', eventId: 1144,
      channel: 'REGULAR', weight: -1,
      fields: [{ index: 3, value: 1500000, type: 'int' }] },
    { name: 'WamClientErrorsEvent_all_bool_flags', cobaltClass: 'WamClientErrorsEvent', eventId: 1144,
      channel: 'REGULAR', weight: -1,
      fields: [
        { index: 8,  value: 1, type: 'bool' }, { index: 11, value: 0, type: 'bool' },
        { index: 15, value: 1, type: 'bool' }, { index: 16, value: 1, type: 'bool' },
        { index: 17, value: 0, type: 'bool' }, { index: 18, value: 1, type: 'bool' },
        { index: 19, value: 0, type: 'bool' }, { index: 27, value: 1, type: 'bool' }
      ] },
    { name: 'SyntheticEvent_wide_field_ids', cobaltClass: '<synthetic>', eventId: 100,
      channel: 'REGULAR', weight: -1,
      fields: [
        { index: 5, value: 'narrow', type: 'str' }, { index: 256, value: 1, type: 'bool' },
        { index: 257, value: 'wide1', type: 'str' }, { index: 65535, value: 42, type: 'int' }
      ] },
    { name: 'SyntheticEvent_sampled_weight', cobaltClass: '<synthetic>', eventId: 100,
      channel: 'REGULAR', weight: -42,
      fields: [{ index: 1, value: 'a', type: 'str' }, { index: 2, value: 7, type: 'int' }] }
  ];
  const vectors = scenarios.map(s => ({
    name: s.name, cobaltClass: s.cobaltClass, eventId: s.eventId, channel: s.channel, weight: s.weight,
    fields: s.fields,
    bytes: encodeEvent(s.eventId, s.weight, s.fields.map(f => ({ index: f.index, value: f.value })))
  }));
  return JSON.stringify({
    fixture: 'wam-events-multi-field',
    description: 'Live JS encoding of representative multi-field events per channel. Cobalt *EventBuilder + *Impl.encode for the matching event id and field set must produce identical bytes.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamLibProtocol',
    vectors
  }, null, 2);
})()`;
}

function wamBufferHeaders() {
  return `(() => {
  const Binary = require('WABinary').Binary;
  const constants = require('WAWebWamConstants');
  const buildHeader = (seqNum, channelByte) => {
    const buf = new Binary(undefined, true);
    buf.writeString('WAM');
    buf.writeUint8(constants.WAM_PROTOCOL_VERSION);
    buf.writeUint8(1);
    buf.writeUint16(seqNum);
    buf.writeUint8(channelByte);
    const arr = buf.peek(v => Array.from(v.readByteArrayView()));
    return arr.map(b => b.toString(16).padStart(2, '0')).join('');
  };
  const channelByteName = b => ({ 0: 'REGULAR', 1: 'REALTIME', 2: 'PRIVATE' })[b];
  const rows = [];
  for (const ch of [0, 1, 2]) {
    for (const seq of [1, 2, 256, 0xFF, 0x100, 0xFFFE, 0xFFFF]) {
      rows.push({ channelByte: ch, channelName: channelByteName(ch), seqNum: seq, bytes: buildHeader(seq, ch) });
    }
  }
  return JSON.stringify({
    fixture: 'wam-buffer-headers',
    description: 'The 8-byte WAM buffer header for every (channel, seqNum) combination Cobalt cares about. WAM_PROTOCOL_VERSION pinned at capture time.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamLibContext + WAWebWamConstants',
    protocolVersion: constants.WAM_PROTOCOL_VERSION,
    vectors: rows
  }, null, 2);
})()`;
}

function wamBeaconing() {
  return `(() => {
  const Beaconing = require('WAWebWamBeaconing');
  const TimeUtils = require('WATimeUtils');
  const UserPrefs = require('WAWebUserPrefsGeneral');
  const origUnixTime = TimeUtils.unixTime;
  const origMathRandom = Math.random;
  const origGetSettings = UserPrefs.getWamBeaconingSettings;
  const origSetSettings = UserPrefs.setWamBeaconingSettings;
  try {
    let state = [];
    UserPrefs.getWamBeaconingSettings = () => state;
    UserPrefs.setWamBeaconingSettings = next => { state = next.slice(); };
    const DAY = TimeUtils.DAY_SECONDS;
    const T0 = 100 * DAY;
    let currentTime = T0;
    TimeUtils.unixTime = () => currentTime;
    const randomQueue = [];
    Math.random = () => randomQueue.length > 0 ? randomQueue.shift() : 0.5;
    const scenarios = [];
    const runScenario = (name, description, steps) => {
      state = [];
      currentTime = T0;
      randomQueue.length = 0;
      const results = [];
      for (const step of steps) {
        if (step.advanceDays) currentTime = T0 + step.advanceDays * DAY;
        if (step.advanceSeconds) currentTime += step.advanceSeconds;
        if (step.pushRandom !== undefined) randomQueue.push(step.pushRandom);
        const r = Beaconing.maybeGetEventSequenceNumber(step.bufferKey);
        results.push({
          step: step.label, bufferKey: step.bufferKey, unixTime: currentTime,
          dayIndex: Math.floor(currentTime / DAY),
          pushedRandom: step.pushRandom === undefined ? null : step.pushRandom,
          result: r
        });
      }
      scenarios.push({ name, description, results });
    };
    runScenario('activate_then_increment_regular',
      'Day0 roll under 0.01 -> activates "regular" key; subsequent calls within same day return monotonically increasing sequence',
      [
        { label: 'first_call_activates', bufferKey: 'regular', pushRandom: 0.005 },
        { label: 'same_day_increments_1', bufferKey: 'regular' },
        { label: 'same_day_increments_2', bufferKey: 'regular' },
        { label: 'same_day_increments_3', bufferKey: 'regular' }
      ]);
    runScenario('reject_then_remain_inactive',
      'Day0 roll above 0.01 -> "regular" key stored as null; further same-day calls keep returning null',
      [
        { label: 'first_call_rejects', bufferKey: 'regular', pushRandom: 0.5 },
        { label: 'same_day_still_null_1', bufferKey: 'regular' },
        { label: 'same_day_still_null_2', bufferKey: 'regular' }
      ]);
    runScenario('rerolls_at_day_boundary',
      'After a UTC day passes, the state for that key re-rolls. Day0 activates, Day1 deactivates.',
      [
        { label: 'day0_activates',  bufferKey: 'regular', pushRandom: 0.005 },
        { label: 'day0_increment',  bufferKey: 'regular' },
        { label: 'day1_deactivate', bufferKey: 'regular', advanceDays: 1, pushRandom: 0.5 },
        { label: 'day1_still_null', bufferKey: 'regular' },
        { label: 'day2_reactivate', bufferKey: 'regular', advanceDays: 2, pushRandom: 0.001 },
        { label: 'day2_increment',  bufferKey: 'regular' }
      ]);
    runScenario('independent_keys',
      'Different buffer keys roll independently; activation of "regular" does not influence "realtime".',
      [
        { label: 'regular_activates',   bufferKey: 'regular',  pushRandom: 0.005 },
        { label: 'realtime_rejects',    bufferKey: 'realtime', pushRandom: 0.5 },
        { label: 'regular_increments',  bufferKey: 'regular' },
        { label: 'realtime_still_null', bufferKey: 'realtime' }
      ]);
    runScenario('threshold_boundary',
      'The activation cutoff is 0.01 inclusive: random==0.01 activates, random==0.01000001 rejects',
      [
        { label: 'random_eq_threshold', bufferKey: 'regular', pushRandom: 0.01 },
        { label: 'reset_day_for_next',  bufferKey: 'realtime', pushRandom: 0.01000001, advanceDays: 1 }
      ]);
    return JSON.stringify({
      fixture: 'wam-beaconing',
      description: 'Captured results of WAWebWamBeaconing.maybeGetEventSequenceNumber under controlled (Math.random, WATimeUtils.unixTime, UserPrefs) stubs.',
      snapshotRevision: ${SNAPSHOT_REVISION},
      liveRuntimeRevision: ${LIVE_REVISION},
      capturedAt: '${CAPTURED_AT}',
      capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
      sourceModule: 'WAWebWamBeaconing',
      activationThreshold: 0.01,
      daySeconds: TimeUtils.DAY_SECONDS,
      scenarios
    }, null, 2);
  } finally {
    TimeUtils.unixTime = origUnixTime;
    Math.random = origMathRandom;
    UserPrefs.getWamBeaconingSettings = origGetSettings;
    UserPrefs.setWamBeaconingSettings = origSetSettings;
  }
})()`;
}

function wamEventDefinitions() {
  return `(() => {
  const codegen = require('WAWebWamCodegenUtils');
  const ev = codegen.events;
  const m = codegen.metrics;
  const allFieldKeys = Object.keys(m.$1);
  const fieldsByEvent = {};
  for (const k of allFieldKeys) {
    const idx = k.indexOf('::');
    if (idx < 0) continue;
    const eventName = k.slice(0, idx);
    const fieldName = k.slice(idx + 2);
    const def = m.$1[k];
    (fieldsByEvent[eventName] = fieldsByEvent[eventName] || []).push({
      name: fieldName, id: def.id,
      type: typeof def.type === 'string' ? def.type : 'enum',
      enumValues: typeof def.type === 'object' ? def.type : null
    });
  }
  const events = [];
  for (const name of Object.keys(ev)) {
    let id = null, channel = null, falcoName = null;
    try {
      const inst = new ev[name]({});
      id = inst.id; channel = inst.wamChannel; falcoName = inst.$falcoEventName || null;
    } catch (e) {}
    const fields = (fieldsByEvent[name] || []).sort((a, b) => a.id - b.id);
    events.push({ name, id, channel, falcoEventName: falcoName, fields });
  }
  events.sort((a, b) => (a.id || 0) - (b.id || 0));
  const wellKnownGlobals = [
    'mnc','mcc','platform','deviceName','osVersion','appVersion','appIsBetaRelease','networkIsWifi',
    'commitTime','browserVersion','webcEnv','memClass','yearClass','webcPhonePlatform','browser',
    'webcPhoneCharging','webcPhoneDeviceManufacturer','webcPhoneDeviceModel','webcPhoneOsBuildNumber',
    'webcPhoneOsVersion','webcBucket','webcWebPlatform','webcPhoneAppVersion','webcNativeBetaUpdates',
    'webcNativeAutolaunch','appBuild','yearClass2016','datacenter','beaconSessionId','streamId',
    'webcTabId','abKey2','deviceVersion','expoKey','psId','ocVersion','webcWebDeviceManufacturer',
    'webcWebDeviceModel','webcWebOsReleaseNumber','webcWebArch','psCountryCode','numCpu',
    'serviceImprovementOptOut','deviceClassification','wametaLoggerTestFilter','webcRevision','isInCohort'
  ];
  const globals = [];
  for (const name of wellKnownGlobals) {
    try {
      const g = m.getGlobal(name);
      globals.push({ name, id: g.id, type: typeof g.type === 'string' ? g.type : 'enum',
        channels: g.channels || null, enumValues: typeof g.type === 'object' ? g.type : null });
    } catch (e) { globals.push({ name, error: String(e) }); }
  }
  return JSON.stringify({
    fixture: 'wam-event-definitions',
    description: 'Full enumeration of every registered WAM event and its fields from WAWebWamCodegenUtils.metrics.',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    sourceModule: 'WAWebWamCodegenUtils',
    eventCount: events.length,
    events, globals
  }, null, 2);
})()`;
}

// -----------------------------------------------------------------------------
// ed25519-live-bundle-vectors
//
// RECONSTRUCTED. The original ad-hoc capture script is lost. This driver
// re-uses the deterministic input seeds (msg/scalar/sk) baked into the
// existing fixture and recomputes the outputs (hashToPoint, blinded, pk,
// signed, unblinded) by calling WACryptoEd25519 and WAWamPrivateStatsToken
// live. Result is a complete top-level JSON document — not the
// debug_eval envelope — written verbatim to
// fixtures/wam/ed25519-live-bundle-vectors.json.
//
// If the existing fixture is also lost, regenerate the input seeds first
// (32 random bytes per field per vector) and adjust the INPUTS table
// below.
// -----------------------------------------------------------------------------

const ED25519_INPUTS = [
  { index: 0,
    msg:    "00112233445566778899aabbccddeeff102132435465768798a9bacbdcedfe0f",
    scalar: "65768798a9bacbdcedfe0f2031425364758697a8b9cadbecfd0e1f3041526374",
    sk:     "cadbecfd0e1f30415263748596a7b8c9daebfc0d1e2f405162738495a6b7c8d9" },
  { index: 1,
    msg:    "25364758697a8b9cadbecfe0f102132435465768798a9bacbdcedff001122334",
    scalar: "8a9bacbdcedff00112233445566778899aabbccddeef00112233445566778899",
    sk:     "ef00112233445566778899aabbccddeeff102132435465768798a9bacbdcedfe" },
  { index: 2,
    msg:    "4a5b6c7d8e9fb0c1d2e3f405162738495a6b7c8d9eafc0d1e2f3041526374859",
    scalar: "afc0d1e2f30415263748596a7b8c9daebfd0e1f2031425364758697a8b9cadbe",
    sk:     "1425364758697a8b9cadbecfe0f102132435465768798a9bacbdcedff0011223" },
  { index: 3,
    msg:    "6f8091a2b3c4d5e6f708192a3b4c5d6e7f90a1b2c3d4e5f60718293a4b5c6d7e",
    scalar: "d4e5f60718293a4b5c6d7e8fa0b1c2d3e4f5061728394a5b6c7d8e9fb0c1d2e3",
    sk:     "394a5b6c7d8e9fb0c1d2e3f405162738495a6b7c8d9eafc0d1e2f30415263748" },
  { index: 4,
    msg:    "94a5b6c7d8e9fa0b1c2d3e4f60718293a4b5c6d7e8f90a1b2c3d4e5f708192a3",
    scalar: "f90a1b2c3d4e5f708192a3b4c5d6e7f8091a2b3c4d5e6f8091a2b3c4d5e6f708",
    sk:     "5e6f8091a2b3c4d5e6f708192a3b4c5d6e7f90a1b2c3d4e5f60718293a4b5c6d" },
  { index: 5,
    msg:    "b9cadbecfd0e1f30415263748596a7b8c9daebfc0d1e2f405162738495a6b7c8",
    scalar: "1e2f405162738495a6b7c8d9eafb0c1d2e3f5061728394a5b6c7d8e9fa0b1c2d",
    sk:     "8394a5b6c7d8e9fa0b1c2d3e4f60718293a4b5c6d7e8f90a1b2c3d4e5f708192" },
  { index: 6,
    msg:    "deef00112233445566778899aabbccddeeff102132435465768798a9bacbdced",
    scalar: "435465768798a9bacbdcedfe0f2031425364758697a8b9cadbecfd0e1f304152",
    sk:     "a8b9cadbecfd0e1f30415263748596a7b8c9daebfc0d1e2f405162738495a6b7" },
  { index: 7,
    msg:    "031425364758697a8b9cadbecfe0f102132435465768798a9bacbdcedff00112",
    scalar: "68798a9bacbdcedff00112233445566778899aabbccddeef0011223344556677",
    sk:     "cddeef00112233445566778899aabbccddeeff102132435465768798a9bacbdc" },
];

function ed25519LiveBundleVectors() {
  return `(() => {
  const Ed = require('WACryptoEd25519');
  const Token = require('WAWamPrivateStatsToken');
  const fromHex = h => {
    const out = new Uint8Array(h.length / 2);
    for (let i = 0; i < out.length; i++) out[i] = parseInt(h.substr(2 * i, 2), 16);
    return out;
  };
  const toHex = b => Array.from(b).map(x => x.toString(16).padStart(2, '0')).join('');
  const inputs = ${JSON.stringify(ED25519_INPUTS)};
  const vectors = inputs.map(v => {
    const msg = fromHex(v.msg);
    const scalar = fromHex(v.scalar);
    const sk = fromHex(v.sk);
    const hashToPoint = Ed.hashToPoint(msg);
    const blinded = Token.blindToken(msg, scalar);
    // Derive the public key from the secret key (Ed25519 scalar -> point).
    const pk = Ed.derivePublicKey ? Ed.derivePublicKey(sk) : Ed.publicKeyFromSecret(sk);
    // Server-side simulated blind-signing: sign(blinded) under sk.
    const signed = Ed.signBlinded ? Ed.signBlinded(blinded, sk) : Token.signBlindedForOracle(blinded, sk);
    const unblinded = Token.unblindToken(signed, scalar, pk);
    const pack = p => {
      if (p instanceof Uint8Array) return toHex(p);
      const out = new Uint8Array(32); Ed.pack(out, p); return toHex(out);
    };
    return {
      index: v.index, msg: v.msg, scalar: v.scalar, sk: v.sk,
      hashToPoint: pack(hashToPoint),
      blinded: toHex(blinded), pk: toHex(pk), signed: toHex(signed), unblinded: toHex(unblinded),
    };
  });
  return JSON.stringify({
    schema: 'v1',
    source: 'WACryptoEd25519 + WAWamPrivateStatsToken',
    snapshotRevision: ${SNAPSHOT_REVISION},
    liveRuntimeRevision: ${LIVE_REVISION},
    capturedAt: '${CAPTURED_AT}',
    capturedVia: 'mcp__whatsapp__web_live_debug_eval_to_file',
    byteLength: 32,
    vectors
  }, null, 2);
})()`;
}

async function main() {
  console.log(`==> capture-wam: session=${sessionId}`);
  const ok = await callTool("web_live_status", { sessionId }).catch(() => null);
  if (!ok) throw new Error(`session "${sessionId}" not reachable via MCP at ${MCP_URL}`);

  for (const c of CAPTURES) {
    if (onlyTopic && !c.name.includes(onlyTopic)) continue;
    console.log(`    capturing ${c.name}`);
    await dumpEval(c.name, c.expression);
  }
  console.log("==> done. Curate data/captures/<sessionId>/wam/ into this directory.");
}

main().catch((err) => {
  console.error(err instanceof Error ? err.stack : String(err));
  process.exit(1);
});
