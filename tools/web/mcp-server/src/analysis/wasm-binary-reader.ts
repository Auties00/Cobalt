

export const VAL_TYPE_NAMES: Record<number, string> = {
  0x7f: "i32",
  0x7e: "i64",
  0x7d: "f32",
  0x7c: "f64",
  0x7b: "v128",
  0x70: "funcref",
  0x6f: "externref",
};

export class BinaryReader {
  public pos = 0;
  private view: DataView;
  private bytes: Uint8Array;

  constructor(buffer: Buffer | Uint8Array) {
    this.bytes =
      buffer instanceof Buffer ? new Uint8Array(buffer.buffer, buffer.byteOffset, buffer.byteLength) : buffer;
    this.view = new DataView(this.bytes.buffer, this.bytes.byteOffset, this.bytes.byteLength);
  }

  get length(): number {
    return this.bytes.length;
  }

  readByte(): number {
    return this.bytes[this.pos++];
  }

  peekByte(): number {
    return this.bytes[this.pos];
  }

  readU32Leb(): number {
    let result = 0;
    let shift = 0;
    let byte: number;
    do {
      byte = this.bytes[this.pos++];
      result |= (byte & 0x7f) << shift;
      shift += 7;
    } while (byte & 0x80);
    return result >>> 0;
  }

  readI32Leb(): number {
    let result = 0;
    let shift = 0;
    let byte: number;
    do {
      byte = this.bytes[this.pos++];
      result |= (byte & 0x7f) << shift;
      shift += 7;
    } while (byte & 0x80);
    if (shift < 32 && byte & 0x40) result |= -(1 << shift);
    return result;
  }

  readI64Leb(): bigint {
    let result = 0n;
    let shift = 0n;
    let byte: number;
    do {
      byte = this.bytes[this.pos++];
      result |= BigInt(byte & 0x7f) << shift;
      shift += 7n;
    } while (byte & 0x80);
    if (shift < 64n && byte & 0x40) result |= -(1n << shift);
    return result;
  }

  readS33(): number {
    let result = 0n;
    let shift = 0n;
    let byte: number;
    do {
      byte = this.bytes[this.pos++];
      result |= BigInt(byte & 0x7f) << shift;
      shift += 7n;
    } while (byte & 0x80);
    if (byte & 0x40) result |= -(1n << shift);
    return Number(result);
  }

  readF32(): number {
    const val = this.view.getFloat32(this.pos, true);
    this.pos += 4;
    return val;
  }

  readF64(): number {
    const val = this.view.getFloat64(this.pos, true);
    this.pos += 8;
    return val;
  }

  readU32Fixed(): number {
    const val = this.view.getUint32(this.pos, true);
    this.pos += 4;
    return val;
  }

  readBytes(n: number): Uint8Array {
    const out = this.bytes.slice(this.pos, this.pos + n);
    this.pos += n;
    return out;
  }

  readName(): string {
    const len = this.readU32Leb();
    const bytes = this.bytes.slice(this.pos, this.pos + len);
    this.pos += len;
    return new TextDecoder().decode(bytes);
  }

  readValType(): string {
    const byte = this.readByte();
    return VAL_TYPE_NAMES[byte] ?? `unknown(0x${byte.toString(16)})`;
  }

  readVec<T>(reader: () => T): T[] {
    const count = this.readU32Leb();
    const result: T[] = [];
    for (let i = 0; i < count; i++) result.push(reader());
    return result;
  }

  skip(n: number): void {
    this.pos += n;
  }
}
