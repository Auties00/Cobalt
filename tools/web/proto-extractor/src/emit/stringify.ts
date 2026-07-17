import type { Identifier, IndentationEntry, MessageMember } from "../types.js";

/** Standard proto-file indent (4 spaces). */
export const SPACE_INDENT = " ".repeat(4);

/** Returns the final {@code $}-delimited segment of a canonical proto name. */
function unnestName(name: string): string {
    return name.split("$").slice(-1)[0]!;
}

/** Prepends {@code prefix} to each line of {@code lines}. */
function addPrefix(lines: readonly string[], prefix: string): string[] {
    return lines.map((line) => prefix + line);
}

/** Emits the lines for one {@code enum Foo { ... }} block. */
function stringifyEnum(ident: Identifier, overrideName?: string): string[] {
    const values = ident.enumValues ?? [];
    return [
        `enum ${overrideName ?? ident.displayName ?? ident.name} {`,
        ...addPrefix(values.map((v) => `${v.name} = ${v.id};`), SPACE_INDENT),
        "}",
    ];
}

/** Emits one field line (or, for {@code __oneof__} synthetic members, a nested oneof block). */
function stringifyMessageSpecMember(
    info: MessageMember,
    completeFlags: boolean,
    parentName: string | undefined,
    indentationMap: Record<string, IndentationEntry>,
): string[] {
    if (info.type === "__oneof__") {
        const inner = (info.members ?? []).flatMap((m) =>
            stringifyMessageSpecMember(m, false, parentName, indentationMap),
        );
        return [
            `oneof ${info.name} {`,
            ...addPrefix(inner, SPACE_INDENT),
            "}",
        ];
    }

    if (info.flags.includes("packed")) {
        info.flags.splice(info.flags.indexOf("packed"));
        info.packed = " [packed=true]";
    }
    if (
        completeFlags &&
        info.flags.length === 0 &&
        !info.type.includes("map")
    ) {
        info.flags.push("optional");
    }

    const ownerPath = indentationMap[info.type]?.indentation;
    let typeName = unnestName(info.type);
    if (ownerPath !== parentName && ownerPath) {
        typeName = `${ownerPath.replaceAll("$", ".")}.${typeName}`;
    }

    const flagsStr = info.flags.length === 0 ? "" : `${info.flags.join(" ")} `;
    return [`${flagsStr}${typeName} ${info.name} = ${info.id}${info.packed ?? ""};`];
}

/** Emits the lines for one {@code message Foo { ... }} block (including any nested types). */
function stringifyMessageSpec(
    ident: Identifier,
    modIdentifiers: Record<string, Identifier>,
    indentationMap: Record<string, IndentationEntry>,
): string[] {
    const result: string[] = [];
    result.push(
        `message ${ident.displayName ?? ident.name} {`,
        ...addPrefix(
            (ident.members ?? []).flatMap((m) =>
                stringifyMessageSpecMember(m, true, ident.name, indentationMap),
            ),
            SPACE_INDENT,
        ),
    );

    const members = indentationMap[ident.name]?.members;
    if (members?.size) {
        for (const memberName of [...members].sort()) {
            const entity = modIdentifiers[memberName];
            if (!entity) {
                console.log("missing nested entity ", memberName);
                continue;
            }
            const displayName = entity.name.slice(ident.name.length + 1);
            const renamed: Identifier = { ...entity, displayName };
            result.push(
                ...addPrefix(getEntityLines(renamed, modIdentifiers, indentationMap), SPACE_INDENT),
            );
        }
    }

    result.push("}");
    result.push("");
    return result;
}

/** Dispatches to {@link stringifyMessageSpec} or {@link stringifyEnum} based on what fields the identifier has. */
export function getEntityLines(
    ident: Identifier,
    modIdentifiers: Record<string, Identifier>,
    indentationMap: Record<string, IndentationEntry>,
): string[] {
    if (ident.members) {
        return stringifyMessageSpec(ident, modIdentifiers, indentationMap);
    }
    if (ident.enumValues?.length) {
        return stringifyEnum(ident);
    }
    return [`// Unknown entity ${ident.name}`];
}
