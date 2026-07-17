import type { WasmMemory } from "./memory.js";

/** {@code PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC} from {@code protobuf-c.h}. */
export const MSG_MAGIC = 0x28aaeef9;
/** {@code PROTOBUF_C__ENUM_DESCRIPTOR_MAGIC} from {@code protobuf-c.h}. */
export const ENUM_MAGIC = 0x114315af;

/** Wire-level proto type names indexed by {@code ProtobufCType}. */
export const PROTO_TYPES: readonly string[] = [
    "int32", "sint32", "sfixed32",
    "int64", "sint64", "sfixed64",
    "uint32", "fixed32", "uint64", "fixed64",
    "float", "double",
    "bool",
    "enum",
    "string", "bytes",
    "message",
];

export const TYPE_ENUM = 13;
export const TYPE_MESSAGE = 16;

export const LABEL_REQUIRED = 0;
export const LABEL_OPTIONAL = 1;
export const LABEL_REPEATED = 2;

export const FLAG_PACKED = 1;
export const FLAG_ONEOF = 4;

export const SIZEOF_FIELD_DESCRIPTOR = 48;
export const SIZEOF_ENUM_VALUE = 12;

const MAX_FIELDS = 1000;
const MAX_ENUM_VALUES = 10_000;

/** A {@code ProtobufCMessageDescriptor} validated and decoded into JS-friendly fields. */
export interface RawMessageDesc {
    readonly address: number;
    readonly canonical: string;
    readonly packageName: string;
    readonly fieldsPtr: number;
    readonly fieldCount: number;
}

/** A {@code ProtobufCEnumDescriptor} validated and decoded into JS-friendly fields. */
export interface RawEnumDesc {
    readonly address: number;
    readonly canonical: string;
    readonly packageName: string;
    readonly valuesPtr: number;
    readonly valueCount: number;
}

/** Validates a name string looks like a proto identifier path. */
export function isValidName(s: string | null): s is string {
    return s !== null && s.length > 0 && s.length < 200 && /^[A-Za-z_][A-Za-z0-9_.]*$/.test(s);
}

/** Validates a name string looks like a proto field/value identifier. */
export function isValidFieldName(s: string | null): s is string {
    return s !== null && s.length > 0 && s.length < 200 && /^[A-Za-z_][A-Za-z0-9_]*$/.test(s);
}

/**
 * Converts {@code (package, fullName)} into Cobalt's {@code $}-nested canonical
 * form, stripping the package prefix.
 *
 * @example
 * toCanonical("wa.voip", "wa.voip.Foo.Bar") // returns "Foo$Bar"
 */
export function toCanonical(packageName: string, fullName: string): string {
    const prefix = packageName ? `${packageName}.` : "";
    const tail = fullName.startsWith(prefix) ? fullName.substring(prefix.length) : fullName;
    return tail.replaceAll(".", "$");
}

/** Reads and validates one {@code ProtobufCMessageDescriptor} struct, or returns {@code null}. */
export function readMessageDescriptor(mem: WasmMemory, addr: number): RawMessageDesc | null {
    if (mem.readU32(addr) !== MSG_MAGIC) return null;
    const namePtr = mem.readU32(addr + 4);
    const packagePtr = mem.readU32(addr + 16);
    const nFields = mem.readU32(addr + 24);
    const fieldsPtr = mem.readU32(addr + 28);

    if (namePtr === null || packagePtr === null || nFields === null || fieldsPtr === null) {
        return null;
    }
    if (nFields > MAX_FIELDS) return null;
    if (nFields > 0 && (fieldsPtr === 0 || mem.read(fieldsPtr, nFields * SIZEOF_FIELD_DESCRIPTOR) === null)) {
        return null;
    }

    const name = mem.readCString(namePtr);
    if (!isValidName(name)) return null;
    const packageName = packagePtr === 0 ? "" : mem.readCString(packagePtr);
    if (packagePtr !== 0 && (packageName === null || (packageName.length > 0 && !isValidName(packageName)))) {
        return null;
    }

    return {
        address: addr,
        canonical: toCanonical(packageName ?? "", name),
        packageName: packageName ?? "",
        fieldsPtr,
        fieldCount: nFields,
    };
}

/** Reads and validates one {@code ProtobufCEnumDescriptor} struct, or returns {@code null}. */
export function readEnumDescriptor(mem: WasmMemory, addr: number): RawEnumDesc | null {
    if (mem.readU32(addr) !== ENUM_MAGIC) return null;
    const namePtr = mem.readU32(addr + 4);
    const packagePtr = mem.readU32(addr + 16);
    const nValues = mem.readU32(addr + 20);
    const valuesPtr = mem.readU32(addr + 24);

    if (namePtr === null || packagePtr === null || nValues === null || valuesPtr === null) {
        return null;
    }
    if (nValues > MAX_ENUM_VALUES) return null;
    if (nValues > 0 && (valuesPtr === 0 || mem.read(valuesPtr, nValues * SIZEOF_ENUM_VALUE) === null)) {
        return null;
    }

    const name = mem.readCString(namePtr);
    if (!isValidName(name)) return null;
    const packageName = packagePtr === 0 ? "" : mem.readCString(packagePtr);
    if (packagePtr !== 0 && (packageName === null || (packageName.length > 0 && !isValidName(packageName)))) {
        return null;
    }

    return {
        address: addr,
        canonical: toCanonical(packageName ?? "", name),
        packageName: packageName ?? "",
        valuesPtr,
        valueCount: nValues,
    };
}

/** Walks every aligned 4-byte slot in every data segment and collects descriptors. */
export function scanDescriptors(mem: WasmMemory): { messages: RawMessageDesc[]; enums: RawEnumDesc[] } {
    const messages: RawMessageDesc[] = [];
    const enums: RawEnumDesc[] = [];
    for (const seg of mem.segments) {
        const data = seg.data;
        const limit = data.length - 3;
        for (let i = 0; i < limit; i += 4) {
            const word = ((data[i]! | (data[i + 1]! << 8) | (data[i + 2]! << 16) | (data[i + 3]! << 24)) >>> 0);
            if (word === MSG_MAGIC) {
                const desc = readMessageDescriptor(mem, seg.address + i);
                if (desc) messages.push(desc);
            } else if (word === ENUM_MAGIC) {
                const desc = readEnumDescriptor(mem, seg.address + i);
                if (desc) enums.push(desc);
            }
        }
    }
    return { messages, enums };
}
