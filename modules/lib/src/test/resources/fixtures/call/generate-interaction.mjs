#!/usr/bin/env node
//
// Captures the binary wire format of WA Web's call-interaction
// messages (sendReaction, raiseHand, requestPeerMute, requestKeyFrame,
// requestVideoUpgrade) by:
//
//   1. Hooking RTCDataChannel.prototype.send in every wasm worker on
//      BOTH the caller (`business`) and callee (`primary`) sessions
//      BEFORE placing a call;
//   2. Triggering each interaction on the page-side via Runtime.evaluate;
//   3. Draining the captured byte traces and writing them to
//      data/captures/<session>/interaction/<topic>.json.
//
// Prerequisites:
//   - Both sessions running, logged_in, VoIP-init complete.
//   - The MCP server is healthy and reachable at WEB_MCP_HTTP_URL.
//
// This is the Phase 6 RE harness — paired captures from caller +
// callee make it easy to identify which bytes are the application
// payload vs SCTP framing.

import { setupInstrumenter } from "../../../../../../../tools/web/js-scripts/wasm-aes-instrumenter.mjs";

const MCP_URL = process.env.WEB_MCP_HTTP_URL ?? "http://localhost:8787/mcp";
const SESSIONS = { CALLER: "business", CALLEE: "primary" };
const PEERS = {
  CALLEE_PN:  "19153544650@c.us",
  CALLEE_LID: "39110693621863@lid",
  CALLER_LID: "83116928594056@lid",
};
// AES encrypt's indirect_function_table slot. Snapshot 1039561367 / wasm
// IL8soPYFU4n.wasm has slot 4946. Override via COBALT_AES_SLOT env var.
const AES_TABLE_SLOT = Number(process.env.COBALT_AES_SLOT || 4946);

// -------- RPC plumbing (matches capture-call-corpus.mjs) ----------------
let nextRpcId = 1;
async function callTool(name, params) {
  const rpcId = nextRpcId++;
  const response = await fetch(MCP_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json, text/event-stream" },
    body: JSON.stringify({ jsonrpc: "2.0", id: rpcId, method: "tools/call",
      params: { name, arguments: params } }),
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
  throw new Error(`unexpected response: ${trimmed.slice(0, 200)}`);
}
function parseToolResult(result) {
  const part = result?.content?.[0];
  if (part?.type !== "text") return result;
  try { return JSON.parse(part.text); } catch { return part.text; }
}

const banner = msg => console.log(`\n========== ${msg} ==========`);
const sleep = ms => new Promise(r => setTimeout(r, ms));
function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(`TIMEOUT ${label} after ${ms}ms`)), ms)),
  ]);
}

async function eval_(sessionId, expression) {
  return callTool("web_live_debug_eval", { sessionId, expression });
}
async function evalStr(sessionId, expr) {
  const r = await eval_(sessionId, expr);
  return r?.value ?? "";
}

// -------- CDP plumbing (direct WebSocket) -------------------------------
class CdpClient {
  constructor(url) { this.url = url; this.id = 0; this.pending = new Map(); }
  connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);
      this.ws.addEventListener("open", () => resolve());
      this.ws.addEventListener("error", () => reject(new Error("ws connect failed: " + this.url)));
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
  const r = await fetch(`http://127.0.0.1:${port}/json`);
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

// The worker-side hook — installs four independent shadows:
//   1. __rtcShadow: every RTCDataChannel.prototype.send call (ciphertext).
//   2. __cryptoShadow: every HKDF + HMAC call routed through
//      WhatsAppVoipWasmWorkerCompatibleCallbacks. HKDF is how the wasm
//      derives all its SRTP session keys — capturing
//      (info, salt, key, length, output) per HKDF invocation lets us
//      reconstruct the full key schedule offline.
//   3. __relayShadow: every sendDataToRelay call. This is the wasm's
//      transport hand-off (after encryption, before RTCDataChannel).
//      Useful for cross-checking the ciphertext we see at RTC.
//   4. __callEventShadow: every onCallEvent payload (plaintext events).
//      Did not fire for sendReaction in earlier tests but kept as a
//      sanity check.
const WORKER_HOOK = `
(function () {
  if (globalThis.__cryptoShadow) return 'already';
  const t0 = globalThis.__rtcShadow ? globalThis.__rtcShadow.t0 : performance.now();
  function b64(buf) {
    if (buf == null) return null;
    let u;
    if (buf instanceof ArrayBuffer) u = new Uint8Array(buf);
    else if (ArrayBuffer.isView(buf)) u = new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
    else return null;
    // chunked to avoid call-stack overflow on big buffers
    let s = '';
    for (let i = 0; i < u.length; i += 0x8000) {
      s += String.fromCharCode.apply(null, u.subarray(i, i + 0x8000));
    }
    return { $bytes: btoa(s), $len: u.length };
  }
  function ser(v) {
    if (v == null) return null;
    if (v instanceof ArrayBuffer) return b64(v);
    if (ArrayBuffer.isView(v)) return { $typed: v.constructor.name, ...b64(v) };
    return null;
  }
  // 0. postMessage shadow — wasm worker posts bytes to a separate
  //    DC-worker; hooking BOTH directions on EVERY worker lets us see
  //    whether wasm posts plaintext or ciphertext, and which other
  //    worker is the consumer.
  if (!globalThis.__postMsgShadow) {
    const trace = [];
    globalThis.__postMsgShadow = { trace, flush() { const c = trace.slice(); trace.length = 0; return c; } };
    function summarize(v) {
      if (v == null) return { t: 'null' };
      if (v instanceof ArrayBuffer) return { t: 'AB', len: v.byteLength, b64: ser(v) };
      if (ArrayBuffer.isView(v)) return { t: v.constructor.name, len: v.byteLength, b64: ser(v) };
      if (typeof v === 'string') return { t: 'str', len: v.length, val: v.length < 200 ? v : v.slice(0, 200) + '...' };
      if (typeof v !== 'object') return { t: typeof v, val: v };
      // object — sample top-level keys + any byte-array values
      const out = { t: 'obj', keys: Object.keys(v).slice(0, 16) };
      try {
        for (const k of out.keys) {
          const x = v[k];
          if (x instanceof ArrayBuffer) out['$' + k] = { t: 'AB', len: x.byteLength, b64: ser(x) };
          else if (ArrayBuffer.isView(x)) out['$' + k] = { t: x.constructor.name, len: x.byteLength, b64: ser(x) };
          else if (typeof x === 'string' && x.length < 80) out['$' + k] = x;
          else if (typeof x === 'number' || typeof x === 'boolean') out['$' + k] = x;
        }
      } catch (_) {}
      return out;
    }
    // self.postMessage = bytes from this worker outward (to parent thread).
    if (typeof self !== 'undefined' && typeof self.postMessage === 'function') {
      const orig = self.postMessage.bind(self);
      self.postMessage = function (msg, ...rest) {
        try { trace.push({ dir: 'self→parent', msg: summarize(msg), t: performance.now() - t0 }); } catch (_) {}
        return orig(msg, ...rest);
      };
    }
    // MessagePort.prototype.postMessage — used by direct worker-to-worker channels.
    if (typeof MessagePort !== 'undefined' && MessagePort.prototype && !MessagePort.prototype.__cobaltHooked) {
      MessagePort.prototype.__cobaltHooked = true;
      const orig = MessagePort.prototype.postMessage;
      MessagePort.prototype.postMessage = function (msg, ...rest) {
        try { trace.push({ dir: 'port→peer', msg: summarize(msg), t: performance.now() - t0 }); } catch (_) {}
        return orig.call(this, msg, ...rest);
      };
    }
  }

  // 1. RTCDataChannel.send shadow — captures stack trace so we can
  //    identify which wasm function (Chrome's stack traces include
  //    wasm-function[N]@... frames) feeds bytes to the DataChannel.
  if (!globalThis.__rtcShadow) {
    const trace = [];
    let stacksSampled = 0;
    if (typeof RTCDataChannel !== 'undefined') {
      const dcProto = RTCDataChannel.prototype;
      if (!dcProto.__rtcShadowHooked) {
        dcProto.__rtcShadowHooked = true;
        const orig = dcProto.send;
        dcProto.send = function (data) {
          const snap = ser(data);
          // Capture stack on the first 30 calls only (avoid memory bloat).
          let stack = null;
          if (stacksSampled < 30) {
            try { stack = new Error().stack; } catch (_) {}
            stacksSampled++;
          }
          let r, t = null;
          try { r = orig.call(this, data); } catch (e) { t = e; }
          trace.push({
            phase: 'rtc-dc-send',
            args: [snap, { label: this.label, id: this.id, state: this.readyState, ordered: this.ordered, protocol: this.protocol }],
            stack: stack,
            thrown: t ? String(t.message || t) : null,
            t: performance.now() - t0
          });
          if (t) throw t;
          return r;
        };
      }
    }
    globalThis.__rtcShadow = { trace, t0, flush() { const c = trace.slice(); trace.length = 0; return c; } };
  }
  // 2-4. Callbacks-object shims. The page sets these on the worker
  // AFTER the worker boots, so poll until ready.
  const cryptoTrace = [], relayTrace = [], callEventTrace = [];
  globalThis.__cryptoShadow    = { trace: cryptoTrace,    flush() { const c = cryptoTrace.slice();    cryptoTrace.length    = 0; return c; } };
  globalThis.__relayShadow     = { trace: relayTrace,     flush() { const c = relayTrace.slice();     relayTrace.length     = 0; return c; } };
  globalThis.__callEventShadow = { trace: callEventTrace, flush() { const c = callEventTrace.slice(); callEventTrace.length = 0; return c; } };

  function tryShim() {
    const cb = self.WhatsAppVoipWasmWorkerCompatibleCallbacks;
    if (!cb || cb.__cobaltShimmed) return false;
    // HKDF
    if (typeof cb.cryptoHkdfExtractWithSaltAndExpand === 'function') {
      const orig = cb.cryptoHkdfExtractWithSaltAndExpand;
      cb.cryptoHkdfExtractWithSaltAndExpand = function (t) {
        let out;
        try {
          out = orig.call(this, t);
          // info_ comes in as a WABinary-style object; try to serialize.
          // If it's a string, just take it; if it's bytes, b64; else JSON.
          let infoSer;
          try {
            if (typeof t.info_ === 'string') infoSer = { $str: t.info_ };
            else if (t.info_ && (t.info_ instanceof ArrayBuffer || ArrayBuffer.isView(t.info_))) infoSer = ser(t.info_);
            else infoSer = { $json: JSON.stringify(t.info_, (k,v) => v instanceof ArrayBuffer ? '<ab:' + v.byteLength + '>' : v) };
          } catch (_) { infoSer = { $err: 'serialize' }; }
          cryptoTrace.push({
            phase: 'hkdf',
            info: infoSer,
            key: ser(t.key_),
            salt: t.salt_ ? ser(t.salt_) : null,
            length: t.length,
            output: ser(out),
            t: performance.now() - t0
          });
        } catch (e) {
          cryptoTrace.push({ phase: 'hkdf-err', err: String(e && (e.message || e)), t: performance.now() - t0 });
          throw e;
        }
        return out;
      };
    }
    // HMAC
    if (typeof cb.hmacSha256KeyGenerator === 'function') {
      const orig = cb.hmacSha256KeyGenerator;
      cb.hmacSha256KeyGenerator = function (t) {
        let out;
        try {
          out = orig.call(this, t);
          cryptoTrace.push({
            phase: 'hmac',
            data: ser(t.data_),
            key: ser(t.key_),
            output: ser(out),
            t: performance.now() - t0
          });
        } catch (e) {
          cryptoTrace.push({ phase: 'hmac-err', err: String(e && (e.message || e)), t: performance.now() - t0 });
          throw e;
        }
        return out;
      };
    }
    // sendDataToRelay
    if (typeof cb.sendDataToRelay === 'function') {
      const orig = cb.sendDataToRelay;
      cb.sendDataToRelay = function (t) {
        // t = { data: Uint8Array, len: int, ip: string, port: int }
        relayTrace.push({
          phase: 'send-to-relay',
          data: ser(t.data),
          len: t.len,
          ip: t.ip,
          port: t.port,
          t: performance.now() - t0
        });
        return orig.call(this, t);
      };
    }
    // onCallEvent (kept for sanity)
    if (typeof cb.onCallEvent === 'function') {
      const orig = cb.onCallEvent;
      cb.onCallEvent = function (payload) {
        try {
          callEventTrace.push({ phase: 'on-call-event', payload: payload, t: performance.now() - t0 });
        } catch (_) {}
        return orig.apply(this, arguments);
      };
    }
    cb.__cobaltShimmed = true;
    return true;
  }
  let installed = tryShim();
  if (!installed) {
    const deadline = performance.now() + 60000;
    const interval = setInterval(() => {
      if (tryShim() || performance.now() > deadline) clearInterval(interval);
    }, 100);
  }
  // 5. AES-encrypt table-slot hook. The wasm's AES encrypt function lives
  //    at indirect_function_table slot 4946 (fn 7000). Hook
  //    WebAssembly.instantiate + WebAssembly.instantiateStreaming on the
  //    worker; when the instance lands, read slot 4946 + replace with a
  //    JS wrapper that snapshots wasm memory at the two pointer args,
  //    calls original, snapshots memory again. That gives us (key context,
  //    plaintext, ciphertext) per AES block — same SRTP keys that
  //    encrypt the captured RTC bytes.
  if (!globalThis.__aesShadow) {
    const trace = [];
    globalThis.__aesShadow = { trace, flush() { const c = trace.slice(); trace.length = 0; return c; } };
    function memSnapshot(mem, ptr, len) {
      try {
        const u = new Uint8Array(mem.buffer, ptr, len);
        let s = '';
        for (let i = 0; i < u.length; i += 0x8000) s += String.fromCharCode.apply(null, u.subarray(i, i + 0x8000));
        return btoa(s);
      } catch (e) { return 'ERR:' + (e.message || e); }
    }
    function installInstance(instance) {
      try {
        const table = instance.exports.__indirect_function_table;
        const memory = instance.exports.memory;
        if (!table || !memory) return false;
        // Sanity: slot count should be ≥ 4947
        if (table.length < 4947) return false;
        const orig = table.get(4946);
        if (typeof orig !== 'function') return false;
        // Build a typed JS wasm function that we can place back into the table.
        const wrapper = new WebAssembly.Function(
          { parameters: ['i32', 'i32'], results: ['i32'] },
          function (statePtr, roundKeyPtr) {
            const ptBefore = memSnapshot(memory, statePtr, 16);
            const keyBefore = memSnapshot(memory, roundKeyPtr, 256);
            let result;
            try { result = orig(statePtr, roundKeyPtr); }
            catch (e) {
              trace.push({ phase: 'aes-error', err: String(e && e.message), t: performance.now() - t0 });
              throw e;
            }
            const ctAfter = memSnapshot(memory, statePtr, 16);
            trace.push({
              phase: 'aes-encrypt',
              statePtr: statePtr,
              roundKeyPtr: roundKeyPtr,
              pt: ptBefore,
              key: keyBefore,
              ct: ctAfter,
              t: performance.now() - t0
            });
            return result;
          }
        );
        table.set(4946, wrapper);
        return true;
      } catch (e) {
        trace.push({ phase: 'install-error', err: String(e && e.message), t: performance.now() - t0 });
        return false;
      }
    }
    // Hook WebAssembly.instantiate + instantiateStreaming
    const origInst = WebAssembly.instantiate.bind(WebAssembly);
    WebAssembly.instantiate = function (...args) {
      const ret = origInst(...args);
      Promise.resolve(ret).then((r) => {
        const inst = r && r.instance ? r.instance : r;
        if (inst && inst.exports) installInstance(inst);
      }, () => {});
      return ret;
    };
    const origStream = WebAssembly.instantiateStreaming.bind(WebAssembly);
    WebAssembly.instantiateStreaming = function (...args) {
      const ret = origStream(...args);
      Promise.resolve(ret).then((r) => {
        const inst = r && r.instance ? r.instance : r;
        if (inst && inst.exports) installInstance(inst);
      }, () => {});
      return ret;
    };
    // If an instance already exists in scope (rare), try to find it via
    // WAWebVoipWebWasmWorker — emscripten typically stores it on Module/h.
    try {
      const candidates = [globalThis.Module, globalThis.h, globalThis.A];
      for (const c of candidates) {
        if (c && c.asm && c.asm.__indirect_function_table) {
          installInstance({ exports: c.asm });
          break;
        }
      }
    } catch (_) {}
  }

  // 6. Web Crypto hooks. If wasm uses crypto.subtle for AES/HMAC,
  //    importKey captures the raw key bytes before they become an
  //    opaque CryptoKey, and encrypt/decrypt captures plaintext +
  //    ciphertext keyed by a WeakMap reference back to importKey
  //    inputs. Same shadow buffer as crypto/relay/etc.
  if (!globalThis.__subtleShadow) {
    const trace = [];
    globalThis.__subtleShadow = { trace, flush() { const c = trace.slice(); trace.length = 0; return c; } };
    const keyMap = new WeakMap();
    let counter = 0;
    if (typeof crypto !== 'undefined' && crypto.subtle && !crypto.subtle.__cobaltSubtleHooked) {
      const cs = crypto.subtle;
      const origImport = cs.importKey.bind(cs);
      cs.importKey = function (format, keyData, algorithm, extractable, usages) {
        const ret = origImport(format, keyData, algorithm, extractable, usages);
        Promise.resolve(ret).then(ck => {
          const id = ++counter;
          keyMap.set(ck, { id, format, algorithm, usages });
          trace.push({ phase: 'importKey', id, format, algorithm: JSON.stringify(algorithm), usages, keyData: ser(keyData), extractable, t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      const origEncrypt = cs.encrypt.bind(cs);
      cs.encrypt = function (algorithm, key, data) {
        const ret = origEncrypt(algorithm, key, data);
        const meta = keyMap.get(key);
        const dataSnap = ser(data);
        Promise.resolve(ret).then(out => {
          trace.push({ phase: 'encrypt', algorithm: JSON.stringify(algorithm), keyId: meta ? meta.id : null, dataIn: dataSnap, dataOut: ser(out), t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      const origDecrypt = cs.decrypt.bind(cs);
      cs.decrypt = function (algorithm, key, data) {
        const ret = origDecrypt(algorithm, key, data);
        const meta = keyMap.get(key);
        const dataSnap = ser(data);
        Promise.resolve(ret).then(out => {
          trace.push({ phase: 'decrypt', algorithm: JSON.stringify(algorithm), keyId: meta ? meta.id : null, dataIn: dataSnap, dataOut: ser(out), t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      const origDeriveBits = cs.deriveBits.bind(cs);
      cs.deriveBits = function (algorithm, baseKey, length) {
        const ret = origDeriveBits(algorithm, baseKey, length);
        const meta = keyMap.get(baseKey);
        Promise.resolve(ret).then(out => {
          trace.push({ phase: 'deriveBits', algorithm: JSON.stringify(algorithm), baseKeyId: meta ? meta.id : null, length, output: ser(out), t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      const origDeriveKey = cs.deriveKey.bind(cs);
      cs.deriveKey = function (algorithm, baseKey, derived, extractable, usages) {
        const ret = origDeriveKey(algorithm, baseKey, derived, extractable, usages);
        const meta = keyMap.get(baseKey);
        Promise.resolve(ret).then(ck => {
          const id = ++counter;
          keyMap.set(ck, { id, format: 'derived', algorithm: derived, usages });
          trace.push({ phase: 'deriveKey', id, algorithm: JSON.stringify(algorithm), baseKeyId: meta ? meta.id : null, derivedAlgo: JSON.stringify(derived), usages, t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      const origSign = cs.sign.bind(cs);
      cs.sign = function (algorithm, key, data) {
        const ret = origSign(algorithm, key, data);
        const meta = keyMap.get(key);
        const dataSnap = ser(data);
        Promise.resolve(ret).then(out => {
          trace.push({ phase: 'sign', algorithm: JSON.stringify(algorithm), keyId: meta ? meta.id : null, dataIn: dataSnap, dataOut: ser(out), t: performance.now() - t0 });
        }, () => {});
        return ret;
      };
      cs.__cobaltSubtleHooked = true;
    }
  }
  return installed ? 'installed' : 'pending-shim';
})()
`;

// Continuous auto-hooker — polls CDP /json every 150ms and injects
// the worker hook into any newly-spawned WAWebVoipWebWasmWorkerBundle
// target.
async function startAutoHook(port) {
  const seen = new Set();
  let stop = false;
  const loop = (async () => {
    while (!stop) {
      try {
        const tgs = await listTargets(port);
        for (const t of tgs.filter(x => (x.title || "").includes("WAWebVoipWebWasmWorkerBundle"))) {
          if (seen.has(t.id)) continue;
          seen.add(t.id);
          try { await quickEval(t, WORKER_HOOK, 2000); } catch (_) {}
        }
      } catch (_) {}
      await sleep(150);
    }
  })();
  return { stop: () => { stop = true; }, seen, done: loop };
}

async function drainHookedWorkers(port) {
  const tgs = await listTargets(port);
  const workers = tgs.filter(x => (x.title || "").includes("WAWebVoipWebWasmWorkerBundle"));
  // Drain in parallel — worker count is ~26+, sequential x 2s-each blows
  // any reasonable per-trigger budget.
  const results = await Promise.all(workers.map(async (w) => {
    const combined = await quickEval(w, `JSON.stringify({
      rtc:    globalThis.__rtcShadow       ? globalThis.__rtcShadow.flush()       : [],
      crypto: globalThis.__cryptoShadow    ? globalThis.__cryptoShadow.flush()    : [],
      relay:  globalThis.__relayShadow     ? globalThis.__relayShadow.flush()     : [],
      events: globalThis.__callEventShadow ? globalThis.__callEventShadow.flush() : [],
      subtle: globalThis.__subtleShadow    ? globalThis.__subtleShadow.flush()    : [],
      aes:    globalThis.__aesShadow       ? globalThis.__aesShadow.flush()       : [],
      postmsg:globalThis.__postMsgShadow   ? globalThis.__postMsgShadow.flush()   : []
    })`, 2000);
    if (typeof combined !== "string") return null;
    try {
      const parsed = JSON.parse(combined);
      return {
        workerId: w.id,
        trace: parsed.rtc,
        crypto: parsed.crypto,
        relay: parsed.relay,
        events: parsed.events,
        subtle: parsed.subtle,
        aes: parsed.aes,
        postmsg: parsed.postmsg
      };
    } catch (_) { return null; }
  }));
  return results.filter(Boolean);
}

// -------- Common eval helpers ------------------------------------------
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
      } catch (e) { window.__voipReady = 'beta-opt-in-err:' + String(e && e.message); return; }

      function setProp(n, v) { try { require('WAWebABProps').setABPropConfigValue(n, v); } catch (_) {} }
      setProp('enable_web_calling', true); setProp('enable_web_group_calling', true);
      setProp('enable_web_voip_p2p', true); setProp('enable_web_voip_proxy_and_sctp_workers', true);
      try { await require('JSResourceForInteraction')('WAWebVoipStackInterfaceImpl').load(); } catch (_) {}
      try { await require('JSResourceForInteraction')('WAWebVoipStackInterfaceWeb').load(); } catch (_) {}
      try { await require('JSResourceForInteraction')('WAWebVoipInit').load(); } catch (_) {}
      try {
        const g = require('WAWebVoipGatingUtils');
        g.isCallingEnabled = () => true; g.isGroupCallingEnabled = () => true;
        g.isUnsupportedBrowserForWebCalling = () => false; g.getUnsupportedBrowserReason = () => null;
      } catch (_) {}
      try {
        const impl = require('WAWebVoipStackInterfaceImpl');
        const web = require('WAWebVoipStackInterfaceWeb');
        impl.getVoipStackInterfaceImpl = () => web.createWAWebVoipStackInterface();
      } catch (_) {}
      try { await require('WAWebVoipInit').initWAWebVoip(); } catch (_) {}
      try {
        const M = require('WAWebVoipAcquireMediaStream');
        M.checkVoipDevicePermissions = () => Promise.resolve(true);
      } catch (_) {}
      for (let i = 0; i < 30; i++) {
        try { if (require('WAWebVoipInitEventEmitter').VoipInitEventEmitter.getIsVoipInited()) { window.__voipReady = 'voip-ready'; return; } } catch (_) {}
        await new Promise(r => setTimeout(r, 200));
      }
      window.__voipReady = 'voip-not-ready';
    } catch (e) { window.__voipReady = 'ERR:' + String(e && e.message); }
  })();
  return 'fired';
})()`;

const AUTO_ACCEPT_HOOK = `(() => {
  if (window.__autoTimer) { clearInterval(window.__autoTimer); window.__autoTimer = null; }
  window.__autoState = 'armed';
  window.__autoCallId = null;
  window.__autoTimer = setInterval(async () => {
    if (window.__autoState !== 'armed') return;
    try {
      const ac = require('WAWebCallCollection').activeCall;
      if (!ac || ac.outgoing !== false || !ac.id) return;
      window.__autoState = 'incoming';
      window.__autoCallId = ac.id;
      clearInterval(window.__autoTimer); window.__autoTimer = null;
      try {
        window.__autoState = 'accepting';
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        await stack.acceptCall(true, false);
        window.__autoState = 'accepted';
      } catch (e) { window.__autoState = 'error:' + String(e && e.message); }
    } catch (_) {}
  }, 200);
  return 'auto-accept-armed';
})()`;

async function ensureVoipReady(sessionId) {
  await eval_(sessionId, ENABLE_VOIP);
  // 240 * 250ms = 60s. The beta opt-in flow restarts comms +
  // re-syncs AB props, which alone takes 5-15s; VoIP init adds
  // another few seconds on top.
  for (let i = 0; i < 240; i++) {
    const v = await evalStr(sessionId, "String(window.__voipReady || '')");
    if (v === "voip-ready") return;
    if (v.startsWith("ERR") || v.startsWith("beta-opt-in-err:")) {
      throw new Error(`${sessionId}: ${v}`);
    }
    await sleep(250);
  }
  throw new Error(`${sessionId}: voip-init timed out`);
}

async function awaitAuto(sessionId, target, ms) {
  const deadline = Date.now() + ms;
  while (Date.now() < deadline) {
    const s = await evalStr(sessionId, "String(window.__autoState || '')");
    if (s === target) return s;
    if (s.startsWith("error:")) return s;
    await sleep(250);
  }
  return "TIMEOUT";
}

async function placeAudioCall(sessionId, peerJid) {
  await eval_(sessionId, `(() => { window.__r = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const w = require('WAWebWidFactory').createWid(${JSON.stringify(peerJid)});
        await require('WAWebVoipStartCall').startWAWebVoipCall(w, false);
        window.__r = 'placed';
      } catch (e) { window.__r = 'ERR:' + String(e && e.message); }
    })();
    return 'fired';
  })()`);
  for (let i = 0; i < 100; i++) {
    const r = await evalStr(sessionId, "String(window.__r || '')");
    if (r !== "pending" && r !== "undefined") return r;
    await sleep(150);
  }
  return "TIMEOUT";
}

async function triggerInteraction(sessionId, label, payload) {
  await eval_(sessionId, `(() => { window.__lastInteraction = 'pending'; })()`);
  await eval_(sessionId, `(() => {
    (async () => {
      try {
        const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
        ${payload}
        window.__lastInteraction = ${JSON.stringify("sent:" + label)};
      } catch (e) { window.__lastInteraction = 'ERR:' + String(e && e.message); }
    })();
    return 'fired';
  })()`);
  for (let i = 0; i < 40; i++) {
    const r = await evalStr(sessionId, "String(window.__lastInteraction || '')");
    if (r !== "pending" && r !== "undefined") return r;
    await sleep(100);
  }
  return "TIMEOUT";
}

// -------- Main flow ----------------------------------------------------
async function main() {
  banner("Phase 6 — interaction wire-format capture");

  // 1. Get CDP endpoints (fast, before any session resets).
  const callerCdp = (await callTool("web_live_cdp_endpoint", { sessionId: SESSIONS.CALLER })).port;
  const calleeCdp = (await callTool("web_live_cdp_endpoint", { sessionId: SESSIONS.CALLEE })).port;
  console.log(`    caller CDP port: ${callerCdp}`);
  console.log(`    callee CDP port: ${calleeCdp}`);

  // 1a. Bring up the CDP pre-instantiation AES instrumenter on both
  //     sessions BEFORE any page reload. Target.setAutoAttach with
  //     waitForDebuggerOnStart catches every new worker target paused
  //     at startup; we inject the WebAssembly.instantiate override before
  //     any worker code runs.
  const callerAesEvents = [];
  const calleeAesEvents = [];
  const callerInstaller = await setupInstrumenter({ cdpPort: callerCdp, label: "caller", aesTableSlot: AES_TABLE_SLOT });
  const calleeInstaller = await setupInstrumenter({ cdpPort: calleeCdp, label: "callee", aesTableSlot: AES_TABLE_SLOT });
  const callerInstallEvents = [];
  const calleeInstallEvents = [];
  const callerAesInitEvents = [];
  const calleeAesInitEvents = [];
  callerInstaller.on("aes", (e) => callerAesEvents.push(e));
  callerInstaller.on("aes-init", (e) => callerAesInitEvents.push(e));
  callerInstaller.on("install", (e) => { callerInstallEvents.push(e); });
  calleeInstaller.on("aes", (e) => calleeAesEvents.push(e));
  calleeInstaller.on("aes-init", (e) => calleeAesInitEvents.push(e));
  calleeInstaller.on("install", (e) => { calleeInstallEvents.push(e); });
  console.log("    AES instrumenters attached on both sessions");

  // 1b. RELOAD both pages so VoIP workers respawn under our autoAttach.
  //     Existing workers already loaded their wasm before the hook was
  //     installed; we need fresh worker spawns.
  banner("reloading pages to refresh wasm workers under autoAttach");
  await eval_(SESSIONS.CALLER, `location.reload()`);
  await eval_(SESSIONS.CALLEE, `location.reload()`);
  // Give Chromium a moment to actually navigate before we issue MCP RPCs.
  await sleep(3000);
  await callTool("web_live_wait_for_login", { sessionId: SESSIONS.CALLER, timeoutMs: 60000 });
  await callTool("web_live_wait_for_login", { sessionId: SESSIONS.CALLEE, timeoutMs: 60000 });
  console.log("    both sessions logged_in post-reload");

  // 2. Enable VoIP on both sessions.
  await Promise.all([
    ensureVoipReady(SESSIONS.CALLER),
    ensureVoipReady(SESSIONS.CALLEE),
  ]);
  console.log("    VoIP ready on both sessions");

  // 3. Start the auto-hookers BEFORE placing the call — workers spawn
  //    on demand, so we need to keep hooking new ones until the call
  //    settles.
  const callerHooker = await startAutoHook(callerCdp);
  const calleeHooker = await startAutoHook(calleeCdp);
  console.log("    worker auto-hookers running on both sides");

  // 4. Arm auto-accept on callee.
  await eval_(SESSIONS.CALLEE, AUTO_ACCEPT_HOOK);
  console.log("    callee auto-accept armed");

  // 5. Place the call.
  banner("placing audio call business → primary");
  const placed = await placeAudioCall(SESSIONS.CALLER, PEERS.CALLEE_PN);
  console.log(`    place result: ${placed}`);

  // 6. Wait for the auto-accept to fire (DataChannel takes more time
  //    on top — but auto=accepted means the call is past the offer
  //    stage at minimum).
  const auto = await awaitAuto(SESSIONS.CALLEE, "accepted", 20000);
  console.log(`    callee auto-state: ${auto}`);

  // 7. Hold for the relay handshake + DTLS + SCTP + DCEP to settle.
  //    Empirically generate-call-fixtures.mjs needs 60s of wait to see
  //    the first big-packet event; we wait similarly here.
  console.log("    waiting 30s for the data channel to open …");
  await sleep(30_000);

  // 8. Drain whatever the workers have captured so far. The crypto
  //    trace at this point should already contain all HKDF outputs
  //    from the DTLS-SRTP handshake — keys are derived BEFORE the
  //    DataChannel opens. The relay trace mirrors the RTC trace
  //    one layer up (pre-DataChannel-wrap).
  function totals(workers, key) {
    return workers.reduce((a, w) => a + (w[key] ? w[key].length : 0), 0);
  }
  const baselineCaller = await drainHookedWorkers(callerCdp);
  const baselineCallee = await drainHookedWorkers(calleeCdp);
  const baselineAesCdpCaller = callerAesEvents.splice(0);
  const baselineAesCdpCallee = calleeAesEvents.splice(0);
  const baselineAesInitCaller = callerAesInitEvents.splice(0);
  const baselineAesInitCallee = calleeAesInitEvents.splice(0);
  console.log(`    baseline drain (caller): rtc=${totals(baselineCaller, 'trace')}, postmsg=${totals(baselineCaller, 'postmsg')}, aes-cdp=${baselineAesCdpCaller.length}`);
  console.log(`    baseline drain (callee): rtc=${totals(baselineCallee, 'trace')}, postmsg=${totals(baselineCallee, 'postmsg')}, aes-cdp=${baselineAesCdpCallee.length}`);

  // 9. Trigger each interaction in sequence, draining bytes between.
  //    For each trigger we drain BOTH sides: caller-side RTC sends
  //    give the SRTP ciphertext; crypto/relay/events on both sides
  //    fill in further detail.
  const interactions = [
    ["sendReaction.thumbsup", `await stack.sendReaction("👍");`],
    ["sendReaction.heart",     `await stack.sendReaction("❤️");`],
    ["raiseHand.up",           `await stack.raiseHand(true);`],
    ["raiseHand.down",         `await stack.raiseHand(false);`],
    ["requestKeyFrame",        `await stack.requestKeyFrame(require('WAWebWidFactory').createWid(${JSON.stringify(PEERS.CALLEE_LID)}));`],
    ["requestPeerMute",        `await stack.requestPeerMute(require('WAWebWidFactory').createWid(${JSON.stringify(PEERS.CALLEE_LID)}));`],
    ["requestVideoUpgrade",    `await stack.requestVideoUpgrade();`],
  ];

  const captures = [];
  for (const [label, payload] of interactions) {
    banner(`trigger: ${label}`);
    // Per-iteration safety net: if any await in this block hangs (call
    // dropped, browser jammed), abort that iteration and continue.
    try {
      await withTimeout((async () => {
        await drainHookedWorkers(callerCdp);
        await drainHookedWorkers(calleeCdp);
        const status = await triggerInteraction(SESSIONS.CALLER, label, payload);
        console.log(`    status: ${status}`);
        await sleep(2_000);
        const afterCaller = await drainHookedWorkers(callerCdp);
        const afterCallee = await drainHookedWorkers(calleeCdp);
        const senderRtc = afterCaller.flatMap(w => (w.trace || []).map(t => ({ workerId: w.workerId, ...t })));
        const senderPostMsg = afterCaller.flatMap(w => (w.postmsg || []).map(t => ({ workerId: w.workerId, ...t })));
        const senderCrypto = afterCaller.flatMap(w => (w.crypto || []).map(t => ({ workerId: w.workerId, ...t })));
        const senderRelay = afterCaller.flatMap(w => (w.relay || []).map(t => ({ workerId: w.workerId, ...t })));
        const senderEvents = afterCaller.flatMap(w => (w.events || []).map(e => ({ workerId: w.workerId, ...e })));
        const senderSubtle = afterCaller.flatMap(w => (w.subtle || []).map(e => ({ workerId: w.workerId, ...e })));
        const senderAes = afterCaller.flatMap(w => (w.aes || []).map(e => ({ workerId: w.workerId, ...e })));
        const receiverCrypto = afterCallee.flatMap(w => (w.crypto || []).map(t => ({ workerId: w.workerId, ...t })));
        const receiverEvents = afterCallee.flatMap(w => (w.events || []).map(e => ({ workerId: w.workerId, ...e })));
        const receiverSubtle = afterCallee.flatMap(w => (w.subtle || []).map(e => ({ workerId: w.workerId, ...e })));
        const receiverAes = afterCallee.flatMap(w => (w.aes || []).map(e => ({ workerId: w.workerId, ...e })));
        // Snapshot + drain the CDP-instrumenter AES events too.
        const senderAesCdp = callerAesEvents.splice(0);
        const receiverAesCdp = calleeAesEvents.splice(0);
        console.log(`    captured: caller rtc=${senderRtc.length} postmsg=${senderPostMsg.length} aes-cdp=${senderAesCdp.length}; callee aes-cdp=${receiverAesCdp.length}`);
        captures.push({ label, status, senderRtc, senderAes, senderAesCdp, senderPostMsg, senderCrypto, senderRelay, senderEvents, senderSubtle, receiverAes, receiverAesCdp, receiverCrypto, receiverEvents, receiverSubtle });
      })(), 45_000, `trigger:${label}`);
    } catch (e) {
      console.log(`    !! ABORTED ${label}: ${e.message || e}`);
      captures.push({ label, status: 'ABORTED:' + String(e.message || e) });
    }
  }

  // 10. Tear down.
  callerHooker.stop();
  calleeHooker.stop();
  try { callerInstaller.close(); } catch (_) {}
  try { calleeInstaller.close(); } catch (_) {}

  // 11. Write output.
  const fs = await import("node:fs/promises");
  const path = await import("node:path");
  const outDir = path.join("data/captures", SESSIONS.CALLER, "interaction");
  await fs.mkdir(outDir, { recursive: true });
  const outPath = path.join(outDir, "interaction-corpus.json");
  await fs.writeFile(outPath, JSON.stringify({
    capturedAt: new Date().toISOString(),
    interactions: captures,
    baselinePreInteraction: {
      caller: baselineCaller,
      callee: baselineCallee,
      callerAesCdp: baselineAesCdpCaller,
      calleeAesCdp: baselineAesCdpCallee,
      callerAesInit: baselineAesInitCaller,
      calleeAesInit: baselineAesInitCallee,
    },
    installEvents: {
      caller: callerInstallEvents,
      callee: calleeInstallEvents,
    },
    postCaptureAesInit: {
      caller: callerAesInitEvents.splice(0),
      callee: calleeAesInitEvents.splice(0),
    },
    peerAddresses: PEERS,
    aesTableSlot: AES_TABLE_SLOT,
  }, null, 2));
  console.log("aes-init (caller post-interaction):", callerAesInitEvents.length);
  console.log("aes-init (callee post-interaction):", calleeAesInitEvents.length);
  // Tally install-event reasons for quick eyeballing.
  function tallyReasons(events) {
    const m = new Map();
    for (const e of events) {
      const k = `${e.phase}/${e.reason || '-'}`;
      m.set(k, (m.get(k) || 0) + 1);
    }
    return [...m.entries()].sort((a, b) => b[1] - a[1]);
  }
  console.log("install reasons (caller):", tallyReasons(callerInstallEvents));
  console.log("install reasons (callee):", tallyReasons(calleeInstallEvents));
  console.log(`\n==> wrote ${outPath}`);

  // 12. End the call so the primary's phone stops ringing.
  try {
    await eval_(SESSIONS.CALLER, `(() => {
      (async () => {
        try {
          const stack = await require('WAWebVoipStackInterface').getVoipStackInterface();
          const cid = (require('WAWebCallCollection').activeCall || {}).id;
          if (cid) await stack.endCall(cid, 0);
        } catch (_) {}
      })();
      return 'fired';
    })()`);
    console.log("    sent endCall");
  } catch (_) {}

  console.log("\n==> done");
}

main().catch(e => { console.error("FATAL:", e?.stack ?? e); process.exit(1); });
