import type { EnumValue, MessageMember } from "../types.js";
import type { WasmMemory } from "./memory.js";
import {
    FLAG_ONEOF,
    FLAG_PACKED,
    LABEL_OPTIONAL,
    LABEL_REPEATED,
    LABEL_REQUIRED,
    PROTO_TYPES,
    SIZEOF_ENUM_VALUE,
    SIZEOF_FIELD_DESCRIPTOR,
    TYPE_ENUM,
    TYPE_MESSAGE,
    isValidFieldName,
} from "./descriptors.js";

/** One field's parsed metadata plus its oneof grouping hint. */
export interface FieldParse {
    readonly field: MessageMember;
    readonly oneofKey: number | null;
}

/** Resolves a {@code message}/{@code enum} field by chasing its descriptor pointer. */
function resolveReference(
    typeCode: number,
    descPtr: number,
    descriptors: ReadonlyMap<number, { kind: "message" | "enum"; canonical: string }>,
): string {
    if (descPtr === 0) return PROTO_TYPES[typeCode]!;
    const target = descriptors.get(descPtr);
    if (!target) return PROTO_TYPES[typeCode]!;
    if (typeCode === TYPE_ENUM && target.kind !== "enum") return PROTO_TYPES[typeCode]!;
    if (typeCode === TYPE_MESSAGE && target.kind !== "message") return PROTO_TYPES[typeCode]!;
    return target.canonical;
}

/** Parses the {@code n_fields * 48}-byte {@code ProtobufCFieldDescriptor} array. */
export function parseFields(
    mem: WasmMemory,
    addr: number,
    count: number,
    descriptors: ReadonlyMap<number, { kind: "message" | "enum"; canonical: string }>,
): FieldParse[] {
    const parsed: FieldParse[] = [];
    for (let i = 0; i < count; i++) {
        const off = addr + i * SIZEOF_FIELD_DESCRIPTOR;
        const namePtr = mem.readU32(off);
        const id = mem.readU32(off + 4);
        const label = mem.readU32(off + 8);
        const type = mem.readU32(off + 12);
        const quantifierOffset = mem.readU32(off + 16);
        const descPtr = mem.readU32(off + 24);
        const flags = mem.readU32(off + 32);

        if (
            namePtr === null || id === null || label === null ||
            type === null || quantifierOffset === null ||
            descPtr === null || flags === null
        ) continue;

        const name = mem.readCString(namePtr);
        if (!isValidFieldName(name)) continue;
        if (type < 0 || type >= PROTO_TYPES.length) continue;

        const flagList: string[] = [];
        if (label === LABEL_REQUIRED) flagList.push("required");
        else if (label === LABEL_OPTIONAL) flagList.push("optional");
        else if (label === LABEL_REPEATED) flagList.push("repeated");

        if (flags & FLAG_PACKED) flagList.push("packed");

        const resolvedType =
            type === TYPE_ENUM || type === TYPE_MESSAGE
                ? resolveReference(type, descPtr, descriptors)
                : PROTO_TYPES[type]!;

        parsed.push({
            field: { name, id, type: resolvedType, flags: flagList },
            oneofKey: flags & FLAG_ONEOF ? quantifierOffset : null,
        });
    }
    return parsed;
}

/** Parses the {@code n_values * 12}-byte {@code ProtobufCEnumValue} array. */
export function parseEnumValues(mem: WasmMemory, addr: number, count: number): EnumValue[] {
    const values: EnumValue[] = [];
    for (let i = 0; i < count; i++) {
        const off = addr + i * SIZEOF_ENUM_VALUE;
        const namePtr = mem.readU32(off);
        const value = mem.readI32(off + 8);
        if (namePtr === null || value === null) continue;
        const name = mem.readCString(namePtr);
        if (!isValidFieldName(name)) continue;
        values.push({ name, id: value });
    }
    return values;
}

/**
 * Re-groups parsed fields back into {@code __oneof__} synthetic members.
 *
 * @remarks
 * protobuf-c flattens oneof fields into the field array with the
 * {@code FLAG_ONEOF} bit set and a shared {@code quantifier_offset} per group.
 * The oneof group name is not preserved, so this function emits synthetic
 * {@code _oneof_N} names indexed in the order groups are encountered.
 */
export function groupOneofs(fields: readonly FieldParse[]): MessageMember[] {
    const buckets = new Map<number, MessageMember[]>();
    const standalone: MessageMember[] = [];
    for (const { field, oneofKey } of fields) {
        if (oneofKey === null) {
            standalone.push(field);
            continue;
        }
        field.flags = field.flags.filter((f) => f !== "optional" && f !== "required");
        const existing = buckets.get(oneofKey);
        if (existing) existing.push(field);
        else buckets.set(oneofKey, [field]);
    }

    const result = [...standalone];
    let index = 0;
    for (const [, members] of buckets) {
        result.push({
            name: `_oneof_${index++}`,
            type: "__oneof__",
            flags: [],
            members,
        });
    }
    return result;
}
