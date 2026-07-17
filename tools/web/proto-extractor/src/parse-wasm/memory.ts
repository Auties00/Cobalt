/** A single wasm data segment materialised at a fixed linear-memory address. */
export interface DataSegment {
    /** The linear-memory address this segment occupies. */
    readonly address: number;
    /** The raw bytes of this segment. */
    readonly data: Uint8Array;
}

const WASM_MAGIC = 0x6d736100; /* "\0asm" little-endian */
const SECTION_CODE = 10;
const SECTION_DATA = 11;
const OPCODE_I32_CONST = 0x41;
const OPCODE_GLOBAL_GET = 0x23;
const OPCODE_END = 0x0b;
const OPCODE_PREFIX_FC = 0xfc;
const FC_MEMORY_INIT = 0x08;

/** Largest coalesced data image to materialise, guarding against an outlier segment address. */
const MAX_DATA_SPAN = 256 * 1024 * 1024;

/**
 * Decodes one unsigned LEB128 integer.
 *
 * @returns the value and the number of bytes consumed.
 * @throws Error if the encoding exceeds 5 bytes (i.e. would overflow u32).
 */
function readVarUint(buf: Uint8Array, offset: number): [number, number] {
    let result = 0;
    let shift = 0;
    let bytes = 0;
    while (true) {
        const byte = buf[offset + bytes];
        if (byte === undefined) throw new Error("LEB128 EOF");
        bytes++;
        result += (byte & 0x7f) * Math.pow(2, shift);
        if ((byte & 0x80) === 0) break;
        shift += 7;
        if (shift > 35) throw new Error("LEB128 too long for u32");
    }
    return [result >>> 0, bytes];
}

/** Decodes one signed LEB128 integer (used by {@code i32.const} init expressions). */
function readVarInt(buf: Uint8Array, offset: number): [number, number] {
    let result = 0;
    let shift = 0;
    let bytes = 0;
    let byte = 0;
    while (true) {
        const cur = buf[offset + bytes];
        if (cur === undefined) throw new Error("LEB128 EOF");
        byte = cur;
        bytes++;
        result |= (byte & 0x7f) << shift;
        shift += 7;
        if ((byte & 0x80) === 0) break;
        if (shift > 35) throw new Error("LEB128 too long for i32");
    }
    if (shift < 32 && (byte & 0x40)) {
        result |= -(1 << shift);
    }
    return [result, bytes];
}

/** Treats a list of {@link DataSegment}s as a sparse byte map, providing pointer-style reads. */
export class WasmMemory {
    constructor(public readonly segments: readonly DataSegment[]) {}

    /**
     * Returns the byte slice of {@code length} bytes starting at virtual
     * address {@code addr}, or {@code null} if the range escapes the loaded
     * segments.
     */
    read(addr: number, length: number): Uint8Array | null {
        for (const seg of this.segments) {
            if (addr >= seg.address && addr + length <= seg.address + seg.data.length) {
                const offset = addr - seg.address;
                return seg.data.subarray(offset, offset + length);
            }
        }
        return null;
    }

    /** Reads a little-endian unsigned 32-bit word, or returns {@code null}. */
    readU32(addr: number): number | null {
        const bytes = this.read(addr, 4);
        if (!bytes) return null;
        return (
            (bytes[0]! | (bytes[1]! << 8) | (bytes[2]! << 16) | (bytes[3]! << 24)) >>> 0
        );
    }

    /** Reads a little-endian signed 32-bit word, or returns {@code null}. */
    readI32(addr: number): number | null {
        const u = this.readU32(addr);
        return u === null ? null : (u | 0);
    }

    /**
     * Reads a null-terminated UTF-8 C string starting at virtual address
     * {@code addr}.
     *
     * @returns the decoded string, or {@code null} if the address falls
     *          outside any segment or no terminator is found within
     *          {@code maxLength} bytes.
     */
    readCString(addr: number, maxLength = 1024): string | null {
        for (const seg of this.segments) {
            if (addr < seg.address || addr >= seg.address + seg.data.length) continue;
            const start = addr - seg.address;
            const limit = Math.min(start + maxLength, seg.data.length);
            let end = start;
            while (end < limit && seg.data[end] !== 0) end++;
            if (end >= limit || seg.data[end] !== 0) return null;
            return Buffer.from(seg.data.subarray(start, end)).toString("utf-8");
        }
        return null;
    }
}

/** One DATA-section segment before passive destinations are resolved from the code. */
interface RawSegment {
    /** {@code true} for a passive (mode 1) segment, whose address is set later from {@code memory.init}. */
    readonly passive: boolean;
    /** The active segment's address, or {@code null} for passive segments and unresolvable offset expressions. */
    readonly address: number | null;
    /** The segment's raw bytes. */
    readonly data: Uint8Array;
}

/**
 * Parses every data segment out of a wasm binary and returns them in a
 * {@link WasmMemory}, resolving passive segments to the addresses their
 * {@code memory.init} instructions write them to.
 *
 * <p>Shared-memory / pthread Emscripten builds (e.g. the WhatsApp voip module)
 * emit their static data as passive segments materialised at runtime by a
 * synthetic {@code __wasm_init_memory} function rather than as active segments
 * with inline offsets. For a non-relocatable module that function writes each
 * segment with a constant destination, so the destination is recovered
 * statically by scanning the code section for the {@code i32.const dest;
 * i32.const src; i32.const len; memory.init seg} idiom. The resolved segments are
 * coalesced into a single contiguous image so reads that span adjacent segments
 * (a descriptor's field array, a string table) succeed.
 *
 * @param binary - the wasm module bytes.
 * @returns a {@link WasmMemory} of the materialised data segments.
 * @throws Error if {@code binary} is not a well-formed wasm module header.
 */
export function parseWasmDataSegments(binary: Uint8Array): WasmMemory {
    const view = new DataView(binary.buffer, binary.byteOffset, binary.byteLength);
    if (view.getUint32(0, true) !== WASM_MAGIC) {
        throw new Error("Not a wasm module");
    }

    let codeStart = -1;
    let codeEnd = -1;
    let rawSegments: RawSegment[] = [];

    let off = 8; /* skip 4-byte magic + 4-byte version */
    while (off < binary.length) {
        const sectionId = binary[off++]!;
        const [sectionSize, sectionSizeBytes] = readVarUint(binary, off);
        off += sectionSizeBytes;
        const sectionEnd = off + sectionSize;

        if (sectionId === SECTION_CODE) {
            codeStart = off;
            codeEnd = sectionEnd;
        } else if (sectionId === SECTION_DATA) {
            rawSegments = parseDataSection(binary, off, sectionEnd);
        }
        off = sectionEnd;
    }

    const passiveDestinations = codeStart >= 0
        ? scanPassiveDestinations(binary, codeStart, codeEnd)
        : new Map<number, number>();

    const placed: DataSegment[] = [];
    for (let index = 0; index < rawSegments.length; index++) {
        const seg = rawSegments[index]!;
        const address = seg.passive ? passiveDestinations.get(index) : seg.address;
        if (address != null) placed.push({ address, data: seg.data });
    }

    return new WasmMemory(coalesce(placed));
}

/**
 * Parses the DATA section's segments, preserving their order so each segment's
 * index matches the index a {@code memory.init} instruction refers to.
 */
function parseDataSection(binary: Uint8Array, start: number, end: number): RawSegment[] {
    const segments: RawSegment[] = [];
    let off = start;
    const [count, countBytes] = readVarUint(binary, off);
    off += countBytes;

    for (let i = 0; i < count && off < end; i++) {
        const [mode, modeBytes] = readVarUint(binary, off);
        off += modeBytes;

        if (mode === 1) {
            const [dataLen, dataLenBytes] = readVarUint(binary, off);
            off += dataLenBytes;
            segments.push({ passive: true, address: null, data: binary.subarray(off, off + dataLen) });
            off += dataLen;
            continue;
        }

        if (mode === 2) {
            const [, memidxBytes] = readVarUint(binary, off);
            off += memidxBytes;
        }
        const offsetExpr = parseInitExprAddress(binary, off);
        off = offsetExpr.end;
        const [dataLen, dataLenBytes] = readVarUint(binary, off);
        off += dataLenBytes;
        segments.push({ passive: false, address: offsetExpr.address, data: binary.subarray(off, off + dataLen) });
        off += dataLen;
    }
    return segments;
}

/**
 * Parses an active segment's constant offset expression. Recognises the
 * {@code i32.const N end} form; any other form (e.g. a relocatable
 * {@code global.get __memory_base} base) yields a {@code null} address.
 */
function parseInitExprAddress(binary: Uint8Array, off: number): { address: number | null; end: number } {
    const opcode = binary[off];
    if (opcode === OPCODE_I32_CONST) {
        const [addr, addrBytes] = readVarInt(binary, off + 1);
        return { address: addr >>> 0, end: skipToExprEnd(binary, off + 1 + addrBytes) };
    }
    if (opcode === OPCODE_GLOBAL_GET) {
        const [, idxBytes] = readVarUint(binary, off + 1);
        return { address: null, end: skipToExprEnd(binary, off + 1 + idxBytes) };
    }
    return { address: null, end: skipToExprEnd(binary, off + 1) };
}

/** Advances past the trailing {@code end} opcode of a constant expression. */
function skipToExprEnd(binary: Uint8Array, off: number): number {
    let p = off;
    while (p < binary.length && binary[p] !== OPCODE_END) p++;
    return p + 1;
}

/**
 * Scans the code section for {@code memory.init} instructions and returns a map
 * from data-segment index to the constant destination address each is copied to.
 *
 * <p>Matches the linker-emitted idiom {@code i32.const dest; i32.const src;
 * i32.const len; memory.init seg}. The scan is byte-pattern based, so a stray
 * {@code 0x41} byte inside an unrelated instruction may begin a candidate match;
 * those fail to complete and are skipped. The first destination seen for a
 * segment wins.
 */
function scanPassiveDestinations(binary: Uint8Array, start: number, end: number): Map<number, number> {
    const destinations = new Map<number, number>();
    for (let i = start; i < end; i++) {
        if (binary[i] !== OPCODE_I32_CONST) continue;
        let match: { segment: number; destination: number } | null;
        try {
            match = matchMemoryInit(binary, i, end);
        } catch {
            continue;
        }
        if (match && !destinations.has(match.segment)) {
            destinations.set(match.segment, match.destination);
        }
    }
    return destinations;
}

/**
 * Attempts to match the {@code i32.const dest; i32.const src; i32.const len;
 * memory.init seg} sequence starting at {@code pos}.
 *
 * @returns the segment index and its destination address, or {@code null} if the
 *          bytes at {@code pos} are not that sequence.
 * @throws Error if a LEB128 value is malformed (a false-positive start byte).
 */
function matchMemoryInit(binary: Uint8Array, pos: number, end: number): { segment: number; destination: number } | null {
    let off = pos;
    if (binary[off] !== OPCODE_I32_CONST) return null;
    const [destination, destBytes] = readVarInt(binary, off + 1);
    off += 1 + destBytes;

    for (let operand = 0; operand < 2; operand++) {
        if (binary[off] !== OPCODE_I32_CONST) return null;
        const [, operandBytes] = readVarInt(binary, off + 1);
        off += 1 + operandBytes;
    }

    if (off + 1 >= end || binary[off] !== OPCODE_PREFIX_FC || binary[off + 1] !== FC_MEMORY_INIT) return null;
    off += 2;
    const [segment, segBytes] = readVarUint(binary, off);
    off += segBytes;
    if (off > end) return null;
    return { segment, destination: destination >>> 0 };
}

/**
 * Merges placed segments into a single contiguous image spanning their address
 * range, zero-filling gaps so cross-segment reads succeed. Falls back to the
 * sparse segments when the span is empty or implausibly large.
 */
function coalesce(segments: DataSegment[]): DataSegment[] {
    if (segments.length === 0) return [];

    let min = Infinity;
    let max = -Infinity;
    for (const seg of segments) {
        min = Math.min(min, seg.address);
        max = Math.max(max, seg.address + seg.data.length);
    }

    const span = max - min;
    if (span <= 0 || span > MAX_DATA_SPAN) return segments;

    const data = new Uint8Array(span);
    for (const seg of segments) data.set(seg.data, seg.address - min);
    return [{ address: min, data }];
}
