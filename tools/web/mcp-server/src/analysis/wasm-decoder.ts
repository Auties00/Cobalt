

import { BinaryReader } from "./wasm-binary-reader.js";

const enum Imm {
  UNKNOWN = 0,
  NONE,
  U32,
  U32X2,
  S32,
  S64,
  F32,
  F64,
  BLOCKTYPE,
  MEMARG,
  MEMARG_LANE,
  BR_TABLE,
  SELECT_T,
  V128_CONST,
  SHUFFLE,
  LANE,
  REFTYPE,
  BYTE1,
}

const SHAPE = new Uint8Array(256).fill(Imm.UNKNOWN);

function setRange(lo: number, hi: number, shape: Imm): void {
  for (let op = lo; op <= hi; op++) SHAPE[op] = shape;
}

SHAPE[0x00] = Imm.NONE;
SHAPE[0x01] = Imm.NONE;
SHAPE[0x02] = Imm.BLOCKTYPE;
SHAPE[0x03] = Imm.BLOCKTYPE;
SHAPE[0x04] = Imm.BLOCKTYPE;
SHAPE[0x05] = Imm.NONE;
SHAPE[0x0b] = Imm.NONE;
SHAPE[0x0c] = Imm.U32;
SHAPE[0x0d] = Imm.U32;
SHAPE[0x0e] = Imm.BR_TABLE;
SHAPE[0x0f] = Imm.NONE;
SHAPE[0x10] = Imm.U32;
SHAPE[0x11] = Imm.U32X2;
SHAPE[0x12] = Imm.U32;
SHAPE[0x13] = Imm.U32X2;

SHAPE[0x1a] = Imm.NONE;
SHAPE[0x1b] = Imm.NONE;
SHAPE[0x1c] = Imm.SELECT_T;

setRange(0x20, 0x24, Imm.U32);
SHAPE[0x25] = Imm.U32;
SHAPE[0x26] = Imm.U32;

setRange(0x28, 0x3e, Imm.MEMARG);
SHAPE[0x3f] = Imm.U32;
SHAPE[0x40] = Imm.U32;

SHAPE[0x41] = Imm.S32;
SHAPE[0x42] = Imm.S64;
SHAPE[0x43] = Imm.F32;
SHAPE[0x44] = Imm.F64;

setRange(0x45, 0xc4, Imm.NONE);

SHAPE[0xd0] = Imm.REFTYPE;
SHAPE[0xd1] = Imm.NONE;
SHAPE[0xd2] = Imm.U32;

function prefixShape(prefix: number, sub: number): Imm {
  if (prefix === 0xfc) {

    if (sub <= 7) return Imm.NONE;
    switch (sub) {
      case 8: return Imm.U32X2;
      case 9: return Imm.U32;
      case 10: return Imm.U32X2;
      case 11: return Imm.U32;
      case 12: return Imm.U32X2;
      case 13: return Imm.U32;
      case 14: return Imm.U32X2;
      case 15:
      case 16:
      case 17: return Imm.U32;
      default: return Imm.UNKNOWN;
    }
  }
  if (prefix === 0xfd) {

    if (sub <= 0x0b) return Imm.MEMARG;
    if (sub === 0x0c) return Imm.V128_CONST;
    if (sub === 0x0d) return Imm.SHUFFLE;
    if (sub >= 0x15 && sub <= 0x22) return Imm.LANE;
    if (sub >= 0x54 && sub <= 0x5b) return Imm.MEMARG_LANE;
    if (sub >= 0x5c && sub <= 0x5d) return Imm.MEMARG;
    return Imm.NONE;
  }
  if (prefix === 0xfe) {

    if (sub === 0x03) return Imm.BYTE1;
    return Imm.MEMARG;
  }
  return Imm.UNKNOWN;
}

function readMemarg(r: BinaryReader): { align: number; offset: number } {
  const alignFlags = r.readU32Leb();
  if (alignFlags & 0x40) r.readU32Leb();
  const offset = r.readU32Leb();
  return { align: alignFlags & ~0x40, offset };
}

export interface InstrVisitor {
  onCall?(funcIdx: number, at: number): void;
  onCallIndirect?(typeIdx: number, tableIdx: number, at: number): void;
  onRefFunc?(funcIdx: number, at: number): void;
  onI32Const?(value: number, at: number): void;
  onMemAccess?(opcode: number, offset: number, align: number, at: number): void;

  onMemoryInit?(dataIndex: number, destAddr: number | null, at: number): void;
  onBlockStart?(opcode: number, at: number): void;
  onBlockEnd?(at: number): void;
  onElse?(at: number): void;
}

export interface WalkResult {

  partial: boolean;

  stoppedAt?: number;

  unknownOpcode?: number;
}

export function walkFunctionBody(
  r: BinaryReader,
  bodyOffset: number,
  bodySize: number,
  visitor: InstrVisitor
): WalkResult {
  const bodyEnd = bodyOffset + bodySize;
  r.pos = bodyOffset;

  const localGroups = r.readU32Leb();
  for (let i = 0; i < localGroups; i++) {
    r.readU32Leb();
    r.readByte();
  }

  let depth = 0;

  const constWindow: number[] = [];
  while (r.pos < bodyEnd) {
    const at = r.pos;
    const opcode = r.readByte();

    if (opcode === 0x0b) {

      if (depth === 0) {
        r.pos = bodyEnd;
        return { partial: false };
      }
      depth--;
      constWindow.length = 0;
      visitor.onBlockEnd?.(at);
      continue;
    }
    if (opcode === 0x05) {
      constWindow.length = 0;
      visitor.onElse?.(at);
      continue;
    }

    let sub = -1;
    let shape: Imm;
    if (opcode === 0xfc || opcode === 0xfd || opcode === 0xfe) {
      sub = r.readU32Leb();
      shape = prefixShape(opcode, sub);
    } else {
      shape = SHAPE[opcode] as Imm;
    }

    if (shape === Imm.UNKNOWN) {
      r.pos = bodyEnd;
      return { partial: true, stoppedAt: at, unknownOpcode: opcode };
    }

    let pushedConst: number | null = null;
    switch (shape) {
      case Imm.NONE:
        break;
      case Imm.U32: {
        const v = r.readU32Leb();
        if (opcode === 0x10) visitor.onCall?.(v, at);
        else if (opcode === 0x12) visitor.onCall?.(v, at);
        else if (opcode === 0xd2) visitor.onRefFunc?.(v, at);
        break;
      }
      case Imm.U32X2: {
        const a = r.readU32Leb();
        const b = r.readU32Leb();
        if (opcode === 0x11 || opcode === 0x13) visitor.onCallIndirect?.(a, b, at);
        else if (opcode === 0xfc && sub === 8) {
          const dest = constWindow.length >= 3 ? constWindow[constWindow.length - 3] : null;
          visitor.onMemoryInit?.(a, dest, at);
        }
        break;
      }
      case Imm.S32: {
        const v = r.readI32Leb();
        if (opcode === 0x41) {
          pushedConst = v;
          visitor.onI32Const?.(v, at);
        }
        break;
      }
      case Imm.S64:
        r.readI64Leb();
        break;
      case Imm.F32:
        r.skip(4);
        break;
      case Imm.F64:
        r.skip(8);
        break;
      case Imm.BLOCKTYPE:
        r.readS33();
        depth++;
        visitor.onBlockStart?.(opcode, at);
        break;
      case Imm.MEMARG: {
        const { align, offset } = readMemarg(r);
        visitor.onMemAccess?.(opcode, offset, align, at);
        break;
      }
      case Imm.MEMARG_LANE:
        readMemarg(r);
        r.skip(1);
        break;
      case Imm.BR_TABLE: {
        const n = r.readU32Leb();
        for (let i = 0; i < n; i++) r.readU32Leb();
        r.readU32Leb();
        break;
      }
      case Imm.SELECT_T: {
        const n = r.readU32Leb();
        r.skip(n);
        break;
      }
      case Imm.V128_CONST:
      case Imm.SHUFFLE:
        r.skip(16);
        break;
      case Imm.LANE:
      case Imm.REFTYPE:
      case Imm.BYTE1:
        r.skip(1);
        break;
    }

    if (pushedConst !== null) {
      constWindow.push(pushedConst);
      if (constWindow.length > 3) constWindow.shift();
    } else {
      constWindow.length = 0;
    }
  }

  return { partial: r.pos !== bodyEnd, stoppedAt: r.pos };
}

export type ConstExpr =
  | { kind: "i32"; value: number }
  | { kind: "i64"; value: bigint }
  | { kind: "f32" }
  | { kind: "f64" }
  | { kind: "global"; index: number }
  | { kind: "ref.func"; index: number }
  | { kind: "ref.null" }
  | { kind: "unknown" };

export function decodeConstExpr(r: BinaryReader): ConstExpr {
  let result: ConstExpr = { kind: "unknown" };
  while (r.pos < r.length) {
    const op = r.readByte();
    if (op === 0x0b) break;
    switch (op) {
      case 0x41:
        result = { kind: "i32", value: r.readI32Leb() };
        break;
      case 0x42:
        result = { kind: "i64", value: r.readI64Leb() };
        break;
      case 0x43:
        r.skip(4);
        result = { kind: "f32" };
        break;
      case 0x44:
        r.skip(8);
        result = { kind: "f64" };
        break;
      case 0x23:
        result = { kind: "global", index: r.readU32Leb() };
        break;
      case 0xd2:
        result = { kind: "ref.func", index: r.readU32Leb() };
        break;
      case 0xd0:
        r.skip(1);
        result = { kind: "ref.null" };
        break;
      default: {

        const shape = (op === 0xfc || op === 0xfd || op === 0xfe
          ? prefixShape(op, r.readU32Leb())
          : (SHAPE[op] as Imm));
        consumeImmediates(r, shape);
        break;
      }
    }
  }
  return result;
}

function consumeImmediates(r: BinaryReader, shape: Imm): void {
  switch (shape) {
    case Imm.U32:
      r.readU32Leb();
      break;
    case Imm.U32X2:
      r.readU32Leb();
      r.readU32Leb();
      break;
    case Imm.S32:
      r.readI32Leb();
      break;
    case Imm.S64:
      r.readI64Leb();
      break;
    case Imm.F32:
      r.skip(4);
      break;
    case Imm.F64:
      r.skip(8);
      break;
    case Imm.BLOCKTYPE:
      r.readS33();
      break;
    case Imm.MEMARG:
      readMemarg(r);
      break;
    case Imm.MEMARG_LANE:
      readMemarg(r);
      r.skip(1);
      break;
    case Imm.BR_TABLE: {
      const n = r.readU32Leb();
      for (let i = 0; i < n; i++) r.readU32Leb();
      r.readU32Leb();
      break;
    }
    case Imm.SELECT_T: {
      const n = r.readU32Leb();
      r.skip(n);
      break;
    }
    case Imm.V128_CONST:
    case Imm.SHUFFLE:
      r.skip(16);
      break;
    case Imm.LANE:
    case Imm.REFTYPE:
    case Imm.BYTE1:
      r.skip(1);
      break;
    case Imm.NONE:
    case Imm.UNKNOWN:
    default:
      break;
  }
}
