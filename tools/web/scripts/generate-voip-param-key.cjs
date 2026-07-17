// Regenerates the VoipParamKey catalogue (the sealed VoipParamKey interface, its
// VoipParamKeyCatalogue holder, and the per-namespace partition enums) under
// modules/lib/.../calls/config/param directly from the wa-voip WASM module.
//
// This script is self-contained: its only input is the WASM binary. It parses the module
// itself (code + data sections), decodes the native reg_param_entry_impl descriptor-building
// instruction patterns, reads each tunable's dotted-path name straight from the data section,
// and emits the Java. It does not depend on any pre-parsed JSON, captured string table, text
// disassembly, or the RE MCP server.
//
// Usage (run from the repository root so the output path resolves):
//   node tools/web/scripts/generate-voip-param-key.cjs [path/to/wa-voip.wasm]
//
// How it works: the engine registers each param with a 20-byte voip_param_entry descriptor
//   { field_offset@0 (i32), value_len@4 (u16), type@6 (u8 ParamType 1..5), is_bwe@8 (u8),
//     arr_elem_len@9 (u8), arr_elem_type@10 (u8), group_id@12 (i32), name_ptr@16 (i32) }.
//   The registration functions build it with i32.const/i32.store* sequences; this script
//   finds each store to field offset 16, reconstructs the struct bytes from the surrounding
//   constant stores, and resolves the dotted-path name from the data segment the name pointer
//   points into. Keys are partitioned into several enums because a single enum holding all
//   ~3040 constants overflows the JVM 64KB <clinit> method-size limit.

const fs = require('fs');

const wasmPath = process.argv[2] || '.temp/voip-param/O4cDmmXP6rI.wasm';
const outDir = 'modules/lib/src/main/java/com/github/auties00/cobalt/calls/config/param';
const CHUNK_SIZE = 700;

// ---------------------------------------------------------------------------------------------
// LEB128 readers. readU/readS return [value, nextPos]; skipLEB only advances the cursor.
// ---------------------------------------------------------------------------------------------
function readU(buf, p) {
  let result = 0;
  let shift = 0;
  let byte;
  do {
    byte = buf[p++];
    result += (byte & 0x7f) * 2 ** shift;
    shift += 7;
  } while (byte & 0x80);
  return [result, p];
}

function readS(buf, p) {
  let result = 0;
  let shift = 0;
  let byte;
  do {
    byte = buf[p++];
    result += (byte & 0x7f) * 2 ** shift;
    shift += 7;
  } while (byte & 0x80);
  if (shift < 53 && (byte & 0x40)) {
    result -= 2 ** shift;
  }
  return [result, p];
}

function skipLEB(buf, p) {
  while (buf[p] & 0x80) p++;
  return p + 1;
}

function skipMemarg(buf, p) {
  let align;
  [align, p] = readU(buf, p);
  if (align & 0x40) {
    p = skipLEB(buf, p);
  }
  return skipLEB(buf, p);
}

// ---------------------------------------------------------------------------------------------
// Module sections.
// ---------------------------------------------------------------------------------------------
function parseSections(buf) {
  if (buf.readUInt32LE(0) !== 0x6d736100) {
    throw new Error('not a WASM module (bad magic)');
  }
  let p = 8;
  const sections = [];
  while (p < buf.length) {
    const id = buf[p++];
    let size;
    [size, p] = readU(buf, p);
    sections.push({ id, start: p, end: p + size });
    p += size;
  }
  return sections;
}

// ---------------------------------------------------------------------------------------------
// Memory image with a NUL-terminated string reader.
//
// This emscripten module emits its data as passive segments materialized at runtime by
// memory.init; there are no active segments with static offsets. So the data section gives the
// raw segment bytes, and the code section's memory.init instructions give where each segment
// lands in linear memory. This reproduces both to map a linear address back to its bytes.
// ---------------------------------------------------------------------------------------------
function buildMemory(buf, sections) {
  const dataSection = sections.find((s) => s.id === 11);
  const codeSection = sections.find((s) => s.id === 10);
  const segments = [];

  // Parse the data segments. Active segments carry their own offset; passive segments are
  // retained by index for the memory.init pass below.
  const passive = [];
  let p = dataSection.start;
  let count;
  [count, p] = readU(buf, p);
  for (let i = 0; i < count; i++) {
    let flags;
    [flags, p] = readU(buf, p);
    if (flags === 1) {
      let len;
      [len, p] = readU(buf, p);
      passive.push(buf.subarray(p, p + len));
      p += len;
      continue;
    }
    if (flags === 2) {
      p = skipLEB(buf, p); // explicit memory index
    }
    let addr = null;
    if (buf[p] === 0x41) {
      [addr, p] = readS(buf, p + 1);
    } else {
      while (buf[p] !== 0x0b) p = skipLEB(buf, p);
    }
    if (buf[p] === 0x0b) p++;
    let len;
    [len, p] = readU(buf, p);
    if (addr !== null) {
      segments.push({ start: addr, bytes: buf.subarray(p, p + len) });
    }
    passive.push(null);
    p += len;
  }

  // Materialize passive segments at the linear addresses memory.init copies them to. Each
  // memory.init is preceded by three constants: dest address, source offset, and length.
  let q = codeSection.start;
  let bodyCount;
  [bodyCount, q] = readU(buf, q);
  for (let i = 0; i < bodyCount; i++) {
    let size;
    [size, q] = readU(buf, q);
    const bodyStart = q;
    const bodyEnd = q + size;
    q = bodyEnd;
    let ins;
    try {
      ins = decodeBody(buf, bodyStart, bodyEnd);
    } catch {
      continue;
    }
    for (let index = 0; index < ins.length; index++) {
      if (ins[index].k !== 'meminit') continue;
      const bytes = passive[ins[index].seg];
      if (!bytes) continue;
      const args = [];
      for (let j = index - 1; j >= 0 && args.length < 3; j--) {
        if (ins[j].k === 'const') args.push(ins[j].v);
      }
      if (args.length < 3) continue;
      const [length, offset, dest] = args;
      if (dest < 0 || offset < 0 || length < 0) continue;
      segments.push({ start: dest, bytes: bytes.subarray(offset, offset + length) });
    }
  }

  segments.sort((a, b) => a.start - b.start);
  return segments;
}

function readCString(segments, addr) {
  for (const segment of segments) {
    if (addr < segment.start || addr >= segment.start + segment.bytes.length) continue;
    let end = addr - segment.start;
    while (end < segment.bytes.length && segment.bytes[end] !== 0) end++;
    return segment.bytes.toString('latin1', addr - segment.start, end);
  }
  return null;
}

// ---------------------------------------------------------------------------------------------
// Instruction decode for one function body. Returns a list where each entry is one
// instruction; the entries this script reasons about are tagged, the rest are opaque ('o').
// Throws on an unrecognized opcode so a mis-decode aborts that one body rather than desyncing.
// ---------------------------------------------------------------------------------------------
function decodeBody(buf, start, end) {
  let p = start;
  let localGroups;
  [localGroups, p] = readU(buf, p);
  for (let i = 0; i < localGroups; i++) {
    p = skipLEB(buf, p); // local count
    p++; // value type
  }
  const ins = [];
  while (p < end) {
    const op = buf[p++];
    if (op === 0x41) { // i32.const
      let v;
      [v, p] = readS(buf, p);
      ins.push({ k: 'const', v });
    } else if (op === 0x36) { // i32.store
      let off;
      [off, p] = readStoreOffset(buf, p);
      ins.push({ k: 'store', w: 4, off });
    } else if (op === 0x3b) { // i32.store16
      let off;
      [off, p] = readStoreOffset(buf, p);
      ins.push({ k: 'store', w: 2, off });
    } else if (op === 0x3a) { // i32.store8
      let off;
      [off, p] = readStoreOffset(buf, p);
      ins.push({ k: 'store', w: 1, off });
    } else if (op === 0x37) { // i64.store
      let off;
      [off, p] = readStoreOffset(buf, p);
      ins.push({ k: 'store', w: 8, off });
    } else if (op === 0x47) { // i32.ne
      ins.push({ k: 'ne' });
    } else if (op === 0x6a) { // i32.add
      ins.push({ k: 'add' });
    } else if (op === 0x42) { // i64.const
      let v;
      [v, p] = readS(buf, p);
      ins.push({ k: 'i64const', v });
    } else if (op === 0x43) { // f32.const
      p += 4;
      ins.push({ k: 'o', op });
    } else if (op === 0x44) { // f64.const
      p += 8;
      ins.push({ k: 'o', op });
    } else if (op >= 0x28 && op <= 0x35) { // loads: memarg, retained with their offset
      let off;
      [off, p] = readStoreOffset(buf, p);
      ins.push({ k: 'load', off });
    } else if (op >= 0x38 && op <= 0x3e) { // remaining stores (f32/f64.store, i64.store8/16/32): memarg
      p = skipMemarg(buf, p);
      ins.push({ k: 'fstore' });
    } else if (op === 0x02 || op === 0x03 || op === 0x04) { // block/loop/if: blocktype
      p = skipLEB(buf, p);
      ins.push({ k: 'ctrl' });
    } else if (op === 0x0c || op === 0x0d) { // br/br_if
      p = skipLEB(buf, p);
      ins.push({ k: 'ctrl' });
    } else if (op === 0x0e) { // br_table
      let n;
      [n, p] = readU(buf, p);
      for (let i = 0; i <= n; i++) p = skipLEB(buf, p);
      ins.push({ k: 'ctrl' });
    } else if (op === 0x10) { // call
      let target;
      [target, p] = readU(buf, p);
      ins.push({ k: 'call', t: target });
    } else if (op === 0x11) { // call_indirect
      p = skipLEB(buf, p);
      p = skipLEB(buf, p);
      ins.push({ k: 'ctrl' });
    } else if (op === 0x1c) { // select with types
      let n;
      [n, p] = readU(buf, p);
      p += n;
      ins.push({ k: 'o', op });
    } else if (op >= 0x20 && op <= 0x24) { // local.get/set/tee, global.get/set
      let x;
      [x, p] = readU(buf, p);
      ins.push({ k: 'local', op, x });
    } else if (op === 0x25 || op === 0x26) { // table.get/set
      p = skipLEB(buf, p);
      ins.push({ k: 'o', op });
    } else if (op === 0x3f || op === 0x40) { // memory.size/grow
      p += 1;
      ins.push({ k: 'o', op });
    } else if (op === 0xd0) { // ref.null
      p += 1;
      ins.push({ k: 'o', op });
    } else if (op === 0xd2) { // ref.func
      p = skipLEB(buf, p);
      ins.push({ k: 'o', op });
    } else if (op === 0xfc) { // misc prefix
      let sub;
      [sub, p] = readU(buf, p);
      if (sub === 8) { // memory.init <dataidx> <memidx>
        let seg;
        [seg, p] = readU(buf, p);
        p += 1; // memory index
        ins.push({ k: 'meminit', seg });
      } else if (sub === 10) { // memory.copy
        p += 2;
        ins.push({ k: 'mem3' });
      } else if (sub === 11) { // memory.fill
        p += 1;
        ins.push({ k: 'mem3' });
      } else {
        p = skipFC(buf, p, sub);
        ins.push({ k: 'o' });
      }
    } else if (op === 0xfd) { // SIMD prefix
      let sub;
      [sub, p] = readU(buf, p);
      p = skipFD(buf, p, sub);
      ins.push({ k: 'o' });
    } else if (op === 0xfe) { // atomics prefix
      let sub;
      [sub, p] = readU(buf, p);
      p = sub === 3 ? p + 1 : skipMemarg(buf, p);
      ins.push({ k: 'o' });
    } else if (NO_IMMEDIATE.has(op)) {
      ins.push({ k: 'o', op });
    } else {
      throw new Error(`unhandled opcode 0x${op.toString(16)}`);
    }
  }
  return ins;
}

function readStoreOffset(buf, p) {
  let align;
  [align, p] = readU(buf, p);
  if (align & 0x40) {
    p = skipLEB(buf, p);
  }
  return readU(buf, p);
}

function skipFC(buf, p, sub) {
  switch (sub) {
    case 8: return skipLEB(buf, p) + 1; // memory.init
    case 9: return skipLEB(buf, p); // data.drop
    case 10: return p + 2; // memory.copy
    case 11: return p + 1; // memory.fill
    case 12: case 14: return skipLEB(buf, skipLEB(buf, p)); // table.init / table.copy
    case 13: case 15: case 16: case 17: return skipLEB(buf, p);
    default: return p; // 0..7 saturating truncations
  }
}

function skipFD(buf, p, sub) {
  if (sub <= 11) return skipMemarg(buf, p); // v128 loads/stores
  if (sub === 12 || sub === 13) return p + 16; // v128.const / i8x16.shuffle
  if (sub >= 21 && sub <= 34) return p + 1; // extract_lane / replace_lane
  if (sub >= 84 && sub <= 91) return skipMemarg(buf, p) + 1; // v128.load/store lane
  if (sub === 92 || sub === 93) return skipMemarg(buf, p); // v128.load32/64_zero
  return p; // arithmetic
}

// Opcodes that take no immediate operand (numeric, comparison, conversion, parametric).
const NO_IMMEDIATE = new Set([
  0x00, 0x01, 0x05, 0x0b, 0x0f, 0x1a, 0x1b, 0xd1,
]);
for (let op = 0x45; op <= 0xc4; op++) NO_IMMEDIATE.add(op);

// ---------------------------------------------------------------------------------------------
// Descriptor reconstruction. Mirrors the native struct layout: a store to field offset 16 is
// the registration anchor; the preceding constant stores fill the 20-byte descriptor.
// ---------------------------------------------------------------------------------------------
function constBefore(ins, index) {
  for (let i = index - 1; i >= 0 && i >= index - 8; i--) {
    if (ins[i].k === 'const' && ins[i].v >= 0) return ins[i].v;
  }
  return null;
}

function writeBytes(bytes, offset, value, width) {
  for (let i = 0; i < width; i++) {
    const absolute = offset + i;
    if (absolute >= 6 && absolute <= 10) {
      bytes[absolute - 6] = (value >>> (i * 8)) & 0xff;
    }
  }
}

function isParamName(name) {
  return name != null && /^(p|mvp|vp|tp)->/.test(name);
}

// ---------------------------------------------------------------------------------------------
// Wire-path recovery.
//
// The engine's struct-field registry (reg_param_entry_impl) names each tunable by its engine
// struct path (p->use_mlow_codec); the wire <voip_settings> JSON instead keys fields by an
// area-sectioned name (encode.use_mlow_codec_v1: root["encode"]["use_mlow_codec_v1"]). These two
// namespaces are joined inside the param compilation unit (voip_param_internal.cc /
// voip_param_filler.cc): each descriptor registration (the store to field offset 16) is immediately
// followed by the matching JSON read, the reader helper call read_param(field_ptr, name, name_len).
//
//   - The SECTION is the descriptor's group_id field (offset 12): a pointer to the section-name
//     string ("encode", "rc", "sfu", ...), authoritative when the registration writes it as a
//     constant. Params registered inside a loop write group_id from a loop variable; for those the
//     section is recovered from the reader stream, where the reader fetches each section object by
//     name before reading its fields, so the most recent section-name string load is the section in
//     force. Where both are available they agree, so the constant form is preferred and the stream
//     form fills the loop-built remainder.
//   - The WIRE FIELD is the name argument the reader read passes. The reader call is
//     read_param(field_ptr, name_ptr, name_len); name_ptr (the second argument, NOT the first) is
//     either a data-segment pointer or a stack-local pj_str buffer filled before the call. We
//     recover it by a forward abstract interpretation of the function body that tracks a symbolic
//     value stack (constants and local+offset pointers) and a per-local byte memory written by the
//     const-store run (i64.const/i32.const stores and data-segment copies). At each reader call we
//     pop the three arguments by stack position and read name_len bytes from where name_ptr points.
//     Resolving name_ptr precisely is essential: the first argument's field-offset constant often
//     dereferences to unrelated data bytes, so a positional scan that ignores argument order
//     reconstructs a mangled name. Many wire names are prefixes of longer merged data strings, so
//     the read is strictly length-bounded.
// ---------------------------------------------------------------------------------------------

// Top-level <voip_settings> section names; the reader fetches each by name before reading its fields.
const SECTION_NAMES = new Set([
  'aec', 'agc', 'bwe', 'encode', 'decode', 'options', 'rc', 'rc_dyn', 'sfu', 'uaqc', 'vid_rc',
  'vid_rc_dyn', 'init_bwe', 'ns', 'fs', 're', 'test', 'traffic_shaper', 'transport_splitter',
  'transport_srtp', 'transport_rtx', 'history_based_bwe', 'history_storage', 'sframe',
  'plr_predictor', 'bwa_rc', 'vbwa_alg_rc', 'vid_driver', 'voip_settings_version',
]);

// The reader helper read_param(field_ptr, name_pj_str, name_len). Recovered as the single call
// target invoked once per descriptor registration across the param-filler functions.
const READER_HELPER = 703;

function readBytes(segments, addr, len) {
  for (const segment of segments) {
    if (addr < segment.start || addr + len > segment.start + segment.bytes.length) continue;
    return segment.bytes.toString('latin1', addr - segment.start, addr - segment.start + len);
  }
  return null;
}

function i64Bytes(value) {
  const out = Buffer.alloc(8);
  try {
    out.writeBigInt64LE(BigInt(value));
  } catch {
    out.writeBigUInt64LE(BigInt.asUintN(64, BigInt(value)));
  }
  return out;
}

function isWireName(value) {
  return value != null && /^[A-Za-z0-9_.]+$/.test(value);
}

// Approximate operand arity (pops, pushes) of a generic numeric/parametric opcode, used to keep the
// abstract value stack aligned around reader calls. Comparisons and binary arithmetic pop two and
// push one; unary operations and conversions pop one and push one; drop pops one; select pops three
// and pushes one.
function numericArity(op) {
  if (op === 0x1a) return [1, 0]; // drop
  if (op === 0x1b) return [3, 1]; // select
  if (op === 0x45 || op === 0x50) return [1, 1]; // i32.eqz / i64.eqz
  if (op >= 0x67 && op <= 0x69) return [1, 1]; // i32 clz/ctz/popcnt
  if (op >= 0x79 && op <= 0x7b) return [1, 1]; // i64 clz/ctz/popcnt
  if (op >= 0xa7 && op <= 0xc4) return [1, 1]; // conversions / sign extensions
  if ((op >= 0x46 && op <= 0x66) || (op >= 0x6b && op <= 0x78) || (op >= 0x7c && op <= 0x8a)) return [2, 1]; // comparisons + binary arithmetic
  return [1, 1];
}

// Forward-simulates a function body and returns a map from each reader-call instruction index to the
// wire field name that call reads, or null where it cannot be recovered. The value stack holds
// symbolic nodes: a known constant, a local-plus-offset pointer, an i64 immediate, a data load, or an
// opaque value. Stores into a local-plus-offset pointer accumulate into that local's byte memory.
// Control flow and unknown calls reset the stack (the argument build for a reader call is always a
// straight-line sequence), while the local byte memory persists.
function simulateWireNames(ins, segments) {
  const names = {};
  let stack = [];
  const localMemory = new Map(); // local index -> Map(byteOffset -> byte)
  const memoryOf = (local) => {
    let memory = localMemory.get(local);
    if (!memory) { memory = new Map(); localMemory.set(local, memory); }
    return memory;
  };
  const writeMemory = (local, offset, bytes) => {
    const memory = memoryOf(local);
    for (let i = 0; i < bytes.length; i++) memory.set(offset + i, bytes[i]);
  };
  const pop = () => stack.pop() ?? { t: 'other' };
  for (let index = 0; index < ins.length; index++) {
    const entry = ins[index];
    switch (entry.k) {
      case 'const': stack.push({ t: 'const', v: entry.v }); break;
      case 'i64const': stack.push({ t: 'i64', v: entry.v }); break;
      case 'load': { const a = pop(); stack.push(a.t === 'const' ? { t: 'dataload', addr: a.v } : { t: 'other' }); break; }
      case 'add': {
        const b = pop(); const a = pop();
        if (a.t === 'ptr' && b.t === 'const') stack.push({ t: 'ptr', base: a.base, off: a.off + b.v });
        else if (b.t === 'ptr' && a.t === 'const') stack.push({ t: 'ptr', base: b.base, off: b.off + a.v });
        else if (a.t === 'const' && b.t === 'const') stack.push({ t: 'const', v: a.v + b.v });
        else stack.push({ t: 'other' });
        break;
      }
      case 'ne': pop(); pop(); stack.push({ t: 'other' }); break;
      case 'store': {
        const value = pop(); const addr = pop();
        if (addr.t === 'ptr') {
          const offset = addr.off + entry.off;
          if (entry.w === 8 && value.t === 'i64') writeMemory(addr.base, offset, i64Bytes(value.v));
          else if (entry.w === 4 && value.t === 'const') { const word = Buffer.alloc(4); word.writeUInt32LE(value.v >>> 0); writeMemory(addr.base, offset, word); }
          else if (entry.w === 2 && value.t === 'const') { const half = Buffer.alloc(2); half.writeUInt16LE(value.v & 0xffff); writeMemory(addr.base, offset, half); }
          else if (entry.w === 1 && value.t === 'const') writeMemory(addr.base, offset, Buffer.from([value.v & 0xff]));
          else if (value.t === 'dataload') { const width = entry.w === 8 ? 8 : entry.w === 4 ? 4 : entry.w === 2 ? 2 : 1; const copied = readBytes(segments, value.addr, width); if (copied) writeMemory(addr.base, offset, Buffer.from(copied, 'latin1')); }
        }
        break;
      }
      case 'fstore': pop(); pop(); break;
      case 'mem3': pop(); pop(); pop(); break;
      case 'meminit': pop(); pop(); pop(); break;
      case 'local': {
        if (entry.op === 0x20) stack.push({ t: 'ptr', base: entry.x, off: 0 }); // local.get
        else if (entry.op === 0x23) stack.push({ t: 'other' }); // global.get
        else if (entry.op === 0x21 || entry.op === 0x24) pop(); // local.set / global.set
        // local.tee (0x22) leaves the value in place
        break;
      }
      case 'call': {
        if (entry.t === READER_HELPER) {
          const length = pop(); const namePtr = pop(); pop(); // pop name_len, name_ptr, field_ptr
          const nameLen = length.t === 'const' ? length.v : null;
          let name = null;
          if (nameLen != null && nameLen >= 1 && nameLen <= 120) {
            if (namePtr.t === 'ptr') {
              const memory = localMemory.get(namePtr.base);
              if (memory) {
                const out = Buffer.alloc(nameLen);
                let complete = true;
                for (let i = 0; i < nameLen; i++) { const byte = memory.get(namePtr.off + i); if (byte == null) { complete = false; break; } out[i] = byte; }
                if (complete) { const candidate = out.toString('latin1'); if (isWireName(candidate)) name = candidate; }
              }
            } else if (namePtr.t === 'const') {
              const slice = readBytes(segments, namePtr.v, nameLen);
              if (isWireName(slice)) name = slice;
            }
          }
          names[index] = name;
          stack.push({ t: 'other' }); // the helper returns a status value
        } else {
          stack = []; // unknown call: arity unknown, reset to avoid desync
        }
        break;
      }
      case 'ctrl': stack = []; break; // block/loop/if/br/return/call_indirect boundary
      case 'o': {
        if (entry.op === 0x43 || entry.op === 0x44) { stack.push({ t: 'other' }); break; } // f32/f64.const
        if (entry.op === 0x3f) { stack.push({ t: 'other' }); break; } // memory.size
        if (entry.op === 0x40) { pop(); stack.push({ t: 'other' }); break; } // memory.grow
        if (entry.op === 0xd0) { stack.push({ t: 'other' }); break; } // ref.null
        if (entry.op === 0xd2) { stack.push({ t: 'other' }); break; } // ref.func
        if (entry.op == null) { stack = []; break; } // opaque (SIMD/atomics/select-with-types): reset
        const [pops, pushes] = numericArity(entry.op);
        for (let i = 0; i < pops; i++) pop();
        for (let i = 0; i < pushes; i++) stack.push({ t: 'other' });
        break;
      }
      default: stack = []; break;
    }
  }
  return names;
}

// Reads the descriptor's group_id (offset-12) constant as a section-name string pointer, or null
// when group_id is not written as a constant (loop-built registrations).
function sectionFromGroupPointer(ins, storeIndex, segments) {
  for (let i = storeIndex; i >= Math.max(0, storeIndex - 180); i--) {
    if (ins[i].k !== 'store' || ins[i].w !== 4 || ins[i].off !== 12) continue;
    const value = constBefore(ins, i);
    if (value == null) return null;
    const name = readCString(segments, value);
    return SECTION_NAMES.has(name) ? name : null;
  }
  return null;
}

function reconstruct(ins, storeIndex, segments) {
  const windowStart = Math.max(0, storeIndex - 180);
  const nameWindowStart = Math.max(0, storeIndex - 90);

  let name = null;
  for (let i = storeIndex; i >= nameWindowStart && name == null; i--) {
    if (ins[i].k !== 'ne') continue;
    for (let j = i - 1; j >= nameWindowStart && j >= i - 6; j--) {
      if (ins[j].k !== 'const') continue;
      const candidate = readCString(segments, ins[j].v);
      name = isParamName(candidate) ? candidate : null;
      break;
    }
  }
  if (name == null) return null;

  const bytes = [0, 0, 0, 0, 0];
  let fieldOffset = null;
  let valueLen = null;
  let groupId = null;
  for (let i = windowStart; i <= storeIndex; i++) {
    const entry = ins[i];
    if (entry.k !== 'store') continue;
    const value = constBefore(ins, i);
    if (value == null) continue;
    if (entry.w === 4) {
      if (entry.off === 0) fieldOffset = value >>> 0;
      if (entry.off === 12) groupId = value >>> 0;
      writeBytes(bytes, entry.off, value, 4);
    } else if (entry.w === 2) {
      if (entry.off === 4) valueLen = value & 0xffff;
      writeBytes(bytes, entry.off, value, 2);
    } else {
      writeBytes(bytes, entry.off, value, 1);
    }
  }

  const type = bytes[0];
  const bwe = bytes[2];
  if (fieldOffset == null || valueLen == null || groupId == null) return null;
  if (type < 1 || type > 5) return null;
  if (bwe !== 0 && bwe !== 1) return null;

  return {
    name,
    valueLen,
    type,
    bweParam: bwe === 1,
    arrElemLen: bytes[3],
    arrElemType: bytes[4],
  };
}

// ---------------------------------------------------------------------------------------------
// Drive the extraction over every code body.
// ---------------------------------------------------------------------------------------------
function extractDescriptors(buf) {
  const sections = parseSections(buf);
  const codeSection = sections.find((s) => s.id === 10);
  const dataSection = sections.find((s) => s.id === 11);
  if (!codeSection || !dataSection) {
    throw new Error('module is missing a code or data section');
  }
  const segments = buildMemory(buf, sections);

  let p = codeSection.start;
  let bodyCount;
  [bodyCount, p] = readU(buf, p);
  const byName = new Map();
  // Wire-path join state, accumulated across all bodies: which wire paths each struct name was read
  // under, and which names each wire path was attributed to. Used to keep only bijective pairs.
  const nameToWirePaths = new Map();
  const wirePathToNames = new Map();
  const wirePathToDescriptor = new Map();
  for (let i = 0; i < bodyCount; i++) {
    let size;
    [size, p] = readU(buf, p);
    const bodyStart = p;
    const bodyEnd = p + size;
    p = bodyEnd;
    let ins;
    try {
      ins = decodeBody(buf, bodyStart, bodyEnd);
    } catch {
      continue; // a body we cannot fully decode cannot be a registration body
    }
    // Forward-simulate the body once to recover the precise wire name read at each reader call, then
    // single-pass in instruction order, tracking the reader's current section (last section-name
    // string load), collecting registration anchors and reader calls, then pairing each registration
    // with the reader call that immediately follows it (before the next registration).
    const wireNames = simulateWireNames(ins, segments);
    const events = [];
    let currentSection = null;
    for (let index = 0; index < ins.length; index++) {
      const entry = ins[index];
      if (entry.k === 'const') {
        const text = readCString(segments, entry.v);
        if (SECTION_NAMES.has(text)) currentSection = text;
      } else if (entry.k === 'store' && entry.w === 4 && entry.off === 16) {
        const descriptor = reconstruct(ins, index, segments);
        if (descriptor) {
          const groupSection = sectionFromGroupPointer(ins, index, segments);
          events.push({ kind: 'reg', descriptor, section: groupSection || currentSection });
        }
      } else if (entry.k === 'call' && entry.t === READER_HELPER) {
        events.push({ kind: 'call', wireField: wireNames[index] ?? null });
      }
    }
    for (let e = 0; e < events.length; e++) {
      if (events[e].kind !== 'reg') continue;
      let wireField = null;
      for (let f = e + 1; f < events.length; f++) {
        if (events[f].kind === 'reg') break;
        if (events[f].kind === 'call') { wireField = events[f].wireField; break; }
      }
      const { descriptor, section } = events[e];
      // The catalogue dedups by struct name, but the engine struct name is RELATIVE to its
      // sub-struct (p->enable is a field within aec, agc, ns, ...), so distinct fields collide on
      // one name. Record every (name, wirePath) read observed; the bijection filter below assigns a
      // wirePath only when the join is unambiguous, so a collision name (whose reads target different
      // struct fields) is never given a guessed wire path nor allowed to alias two wire paths onto
      // one key.
      if (!byName.has(descriptor.name)) byName.set(descriptor.name, { ...descriptor });
      if (section && wireField) {
        const wirePath = `${section}.${wireField}`;
        (nameToWirePaths.get(descriptor.name) ?? setIn(nameToWirePaths, descriptor.name)).add(wirePath);
        (wirePathToNames.get(wirePath) ?? setIn(wirePathToNames, wirePath)).add(descriptor.name);
        // Retain the descriptor backing each wire path so a collision wire path can be emitted as its
        // own typed constant (one per distinct field). The descriptor type/byteWidth/rate-control flag
        // are reliable; the struct name becomes non-unique metadata.
        if (!wirePathToDescriptor.has(wirePath)) {
          wirePathToDescriptor.set(wirePath, { ...descriptor, wirePath });
        }
      }
    }
  }

  // Assign each name-keyed descriptor its wire path only when the name and the wire path are in
  // bijection: the name was read under exactly one wire path and that wire path was read for exactly
  // one name. This is the clean, non-colliding majority; a colliding or ambiguous name keeps no wire
  // path here and is instead split below.
  for (const record of byName.values()) {
    const paths = nameToWirePaths.get(record.name);
    if (!paths || paths.size !== 1) continue;
    const wirePath = [...paths][0];
    if (wirePathToNames.get(wirePath).size !== 1) continue;
    record.wirePath = wirePath;
  }

  // Collision split: a relative struct name that backs more than one distinct wire path (p->enable
  // read under traffic_shaper, agc, and ns is three different engine fields) cannot be carried by a
  // single name-keyed entry. Emit one extra constant per distinct wire path of such a name, each
  // carrying the descriptor type/byteWidth/rate-control flag observed at that read and the struct
  // dotted path as non-unique metadata, so every collision field becomes individually wire
  // addressable. Each distinct wire path is one engine field read (the read target field pointer
  // recovered from the call is a shared per-section staging slot, not a per-field address, so it
  // cannot group fields; the wire path itself is the reliable field identity). Ambiguous wire paths
  // (attributed to more than one struct name) are not split and stay unmodelled.
  const collisionEntries = [];
  for (const [wirePath, names] of wirePathToNames) {
    if (names.size !== 1) continue; // ambiguous read; leave unmodelled
    const name = [...names][0];
    const ownerPaths = nameToWirePaths.get(name);
    if (!ownerPaths || ownerPaths.size <= 1) continue; // not a collision name; already bijective
    const descriptor = wirePathToDescriptor.get(wirePath);
    if (descriptor) collisionEntries.push(descriptor);
  }
  collisionEntries.sort((a, b) => a.wirePath.localeCompare(b.wirePath));
  return {
    entries: [...byName.values()].sort((a, b) => a.name.localeCompare(b.name)),
    collisionEntries,
  };
}

// Initializes and returns a Set value for a key in a Map.
function setIn(map, key) {
  const value = new Set();
  map.set(key, value);
  return value;
}

// ---------------------------------------------------------------------------------------------
// Java code generation.
// ---------------------------------------------------------------------------------------------
const typeNames = new Map([
  [1, 'INTEGER'],
  [2, 'FLOAT'],
  [3, 'STRING'],
  [4, 'ARRAY'],
  [5, 'ARRAY_COUNT'],
]);

const javaKeywords = new Set([
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue',
  'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if',
  'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private',
  'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
  'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null', 'var', 'yield',
  'record', 'sealed', 'permits', 'non-sealed',
]);

const seenConstants = new Set();

// Builds the enum constant name for a key from its wire path. The wire path ({@code section.key}) is
// the unique identity, so its uppercased, underscore-sanitized form names the constant directly
// (encode.use_mlow_codec_v1 -> ENCODE_USE_MLOW_CODEC_V1; rc.maxbwe -> RC_MAXBWE). The dot and any
// other non-alphanumeric byte become an underscore; a leading digit or a Java keyword is prefixed.
// Wire paths are unique, so a name clash can only come from sanitization (two paths differing only in
// punctuation) and is resolved with a deterministic numeric suffix.
function wirePathConstantName(wirePath) {
  let base = wirePath
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_')
    .toUpperCase();
  if (!base) {
    base = 'PARAM';
  }
  if (/^[0-9]/.test(base) || javaKeywords.has(base.toLowerCase())) {
    base = `PARAM_${base}`;
  }
  let candidate = base;
  let suffix = 2;
  while (seenConstants.has(candidate)) {
    candidate = `${base}_${suffix++}`;
  }
  seenConstants.add(candidate);
  return candidate;
}

function javaString(value) {
  return JSON.stringify(value)
    .replace(/\\u003e/g, '>')
    .replace(/\\u003c/g, '<')
    .replace(/\\u0026/g, '&')
    .replace(/\\u0027/g, "'");
}

function escapeJavadoc(value) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function writeFile(name, lines) {
  fs.writeFileSync(`${outDir}/${name}.java`, lines.join('\n') + '\n', 'utf8');
}

function generate(entries, collisionEntries) {
  // Build one unified set keyed by wire path: the bijective name-keyed entries that recovered a wire
  // path, plus the former collision-split entries (a struct field name read under several sections is
  // several distinct wire fields). Constants with no recovered wire path are dropped: they are never
  // wire addressable, so the deserializer can never reach them. Each distinct wire path becomes one
  // constant; the wire path is the unique identity, so deduping by wire path is the only dedup.
  const byWirePath = new Map();
  for (const entry of [...entries, ...collisionEntries]) {
    if (entry.wirePath == null) continue;
    if (!byWirePath.has(entry.wirePath)) {
      byWirePath.set(entry.wirePath, { type: entry.type, wirePath: entry.wirePath });
    }
  }
  const unified = [...byWirePath.values()].sort((a, b) => a.wirePath.localeCompare(b.wirePath));

  // Assign each entry its public-static-final constant name, derived from its (unique) wire path, in
  // emission order so any sanitization-induced clash is resolved by a deterministic numeric suffix.
  for (const entry of unified) {
    entry.constantName = wirePathConstantName(entry.wirePath);
  }

  // The reserved field/method names that must not clash with a generated constant name.
  for (const reserved of ['BY_WIRE_PATH']) seenConstants.add(reserved);

  const out = [];
  out.push('package com.github.auties00.cobalt.calls.config.param;');
  out.push('');
  out.push('import java.lang.reflect.Modifier;');
  out.push('import java.util.Collection;');
  out.push('import java.util.HashMap;');
  out.push('import java.util.Map;');
  out.push('import java.util.Optional;');
  out.push('');
  out.push('/**');
  out.push(' * A voip-param key recovered from WhatsApp Web\'s {@code <voip_settings>} reads, identified by');
  out.push(' * its area-sectioned wire path.');
  out.push(' *');
  out.push(' * <p>The modelled keys are generated directly from the wa-voip WASM module as the public');
  out.push(' * constants of this record, one per distinct wire path. Each key carries its {@code section.key}');
  out.push(' * {@linkplain #wirePath() wire path} (the name the field carries in the JSON document,');
  out.push(' * {@code encode.use_mlow_codec_v1}) and the native descriptor {@linkplain #type() value type}');
  out.push(' * copied from the engine\'s descriptor write, not inferred from names or JSON values. The wire');
  out.push(' * path is recovered from the native param filler, where each descriptor registration is followed');
  out.push(' * by the JSON read that populates it; the section comes from the descriptor group pointer and the');
  out.push(' * field name from that read.');
  out.push(' *');
  out.push(' * <p>One engine struct field name read under several sections is several distinct wire fields');
  out.push(' * (the field behind {@code p-&gt;enable} is a different field under the {@code traffic_shaper},');
  out.push(' * {@code agc}, and {@code ns} sections); each is a separate constant under its own wire path, so');
  out.push(' * {@link #ofWirePath(String)} resolves every modelled leaf to one key. A leaf whose wire path is');
  out.push(' * not modelled is carried by an {@linkplain #unknown(String) unknown} key of type');
  out.push(' * {@link VoipParamType#UNKNOWN}, so the two together resolve every document leaf.');
  out.push(' *');
  out.push(' * <p>Being a record, a key has value equality over {@code (type, wirePath)}; the modelled');
  out.push(' * constants are singletons, and two unknown keys for the same path are equal, so a key is a sound');
  out.push(' * map key. The constants are flat public static fields read in a single class initializer, within');
  out.push(' * the JVM 64KB method-size limit for this key count.');
  out.push(' *');
  out.push(' * @param type     the native descriptor value type, or {@link VoipParamType#UNKNOWN} for an');
  out.push(' *                 unknown key');
  out.push(' * @param wirePath the area-sectioned {@code section.key} wire path that identifies this key');
  out.push(' */');
  out.push('public record VoipParamKey(VoipParamType type, String wirePath) {');
  unified.forEach((entry) => {
    const typeName = typeNames.get(entry.type);
    out.push('    /**');
    out.push(`     * The {@code ${escapeJavadoc(entry.wirePath)}} voip-param.`);
    out.push('     */');
    out.push(`    public static final VoipParamKey ${entry.constantName} = new VoipParamKey(VoipParamType.${typeName}, ${javaString(entry.wirePath)});`);
  });
  out.push('');
  out.push('    /**');
  out.push('     * The wire-path lookup over every generated constant.');
  out.push('     *');
  out.push('     * <p>Maps each {@code section.key} wire path to its constant. The catalogue holds one');
  out.push('     * constant per distinct wire path, so the map is collision-free on keys.');
  out.push('     */');
  out.push('    private static final Map<String, VoipParamKey> BY_WIRE_PATH = buildByWirePath();');
  out.push('');
  out.push('    /**');
  out.push('     * Builds the wire-path lookup by reflecting over this record\'s generated constants.');
  out.push('     *');
  out.push('     * <p>Reflection is used deliberately: listing the constants explicitly here would re-add');
  out.push('     * thousands of field references to the class initializer and overflow the 64KB method-size');
  out.push('     * limit. Each {@code public static final VoipParamKey} field is read once and indexed by its');
  out.push('     * wire path.');
  out.push('     *');
  out.push('     * @return the unmodifiable wire-path lookup');
  out.push('     */');
  out.push('    private static Map<String, VoipParamKey> buildByWirePath() {');
  out.push('        var map = new HashMap<String, VoipParamKey>();');
  out.push('        for (var field : VoipParamKey.class.getDeclaredFields()) {');
  out.push('            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != VoipParamKey.class) {');
  out.push('                continue;');
  out.push('            }');
  out.push('            try {');
  out.push('                var key = (VoipParamKey) field.get(null);');
  out.push('                map.put(key.wirePath(), key);');
  out.push('            } catch (IllegalAccessException exception) {');
  out.push('                throw new ExceptionInInitializerError(exception);');
  out.push('            }');
  out.push('        }');
  out.push('        return Map.copyOf(map);');
  out.push('    }');
  out.push('');
  out.push('    /**');
  out.push('     * Returns every modelled key.');
  out.push('     *');
  out.push('     * <p>The returned collection is an unmodifiable view over the wire-path lookup\'s values; it');
  out.push('     * is not a precomputed copy.');
  out.push('     *');
  out.push('     * @return an unmodifiable view of all modelled keys');
  out.push('     */');
  out.push('    public static Collection<VoipParamKey> values() {');
  out.push('        return BY_WIRE_PATH.values();');
  out.push('    }');
  out.push('');
  out.push('    /**');
  out.push('     * Returns the modelled key whose {@linkplain #wirePath() wire path} equals the given value.');
  out.push('     *');
  out.push('     * @param wirePath the area-sectioned {@code section.key} wire path to resolve');
  out.push('     * @return the matching key, or {@link Optional#empty()} if the path is not modelled');
  out.push('     */');
  out.push('    public static Optional<VoipParamKey> ofWirePath(String wirePath) {');
  out.push('        return Optional.ofNullable(BY_WIRE_PATH.get(wirePath));');
  out.push('    }');
  out.push('');
  out.push('    /**');
  out.push('     * Returns an unknown key for a wire path that no modelled constant covers.');
  out.push('     *');
  out.push('     * <p>The deserializer keys a {@code <voip_settings>} leaf whose wire path is not modelled');
  out.push('     * under such a key (a field added after this module revision, or one whose wire path could');
  out.push('     * not be reconstructed), so its parsed value is never dropped. The key carries the leaf\'s');
  out.push('     * wire path and {@link VoipParamType#UNKNOWN}; its value is read back through the coercing');
  out.push('     * {@link VoipParams} accessors like any other key.');
  out.push('     *');
  out.push('     * @param wirePath the area-sectioned {@code section.key} wire path the leaf was carried under');
  out.push('     * @return an unknown key for the given wire path');
  out.push('     */');
  out.push('    public static VoipParamKey unknown(String wirePath) {');
  out.push('        return new VoipParamKey(VoipParamType.UNKNOWN, wirePath);');
  out.push('    }');
  out.push('}');
  writeFile('VoipParamKey', out);

  return unified;
}

// ---------------------------------------------------------------------------------------------
// Entry point.
// ---------------------------------------------------------------------------------------------
const wasm = fs.readFileSync(wasmPath);
const { entries: descriptors, collisionEntries } = extractDescriptors(wasm);
if (descriptors.length < 1000) {
  throw new Error(`only ${descriptors.length} descriptors recovered from ${wasmPath}; extraction likely failed`);
}
const unified = generate(descriptors, collisionEntries);
const bijectiveWirePaths = descriptors.filter((d) => d.wirePath != null).length;
console.log(`Extracted ${descriptors.length} descriptors from ${wasmPath}; emitted ${unified.length} wire-path keys as VoipParamKey constants in a single record.`);
console.log(`(${bijectiveWirePaths} bijective + ${collisionEntries.length} former-collision, merged and deduplicated by wire path.)`);
