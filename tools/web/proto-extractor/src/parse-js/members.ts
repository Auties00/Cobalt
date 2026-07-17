import type { Identifier, MessageMember, ModuleInfo } from "../types.js";
import { type AstNode, renameIdentifier, unwrapBinaryOr } from "./ast.js";

/** Looks up an identifier inside a {@link ModuleInfo} by its one-letter local alias. */
export function findByAlias(idents: Record<string, Identifier>, alias: string): Identifier | undefined {
    return Object.values(idents).find((it) => it.alias === alias);
}

/**
 * Resolves the {@code message}/{@code enum} type tag carried by an
 * {@code internalSpec} field down to the concrete identifier name (possibly
 * cross-module).
 */
function resolveReferenceType(
    elements: AstNode[],
    info: ModuleInfo,
    allModules: Record<string, ModuleInfo>,
    fieldName: string,
    targetName: string,
    initialType: string,
): string {
    const ref = elements[2];
    const currLoc = ` from member '${fieldName}' of message '${targetName}'`;

    if (ref?.type === "Identifier") {
        const found = findByAlias(info.identifiers, ref.name);
        if (!found) {
            console.warn(`unable to find reference of alias '${ref.name}'${currLoc}`);
            return initialType;
        }
        return found.name;
    }

    if (ref?.type === "MemberExpression") {
        const aliasName =
            ref.object?.name ??
            ref.object?.left?.name ??
            ref.object?.callee?.name;
        const crossRef = info.crossRefs.find((r) => r.alias === aliasName);

        if (elements[1]?.property?.name === "ENUM" && ref.property?.name?.includes("Type")) {
            return renameIdentifier(ref.property.name);
        }
        if (ref.property?.name?.includes("Spec")) {
            return renameIdentifier(ref.property.name);
        }
        if (
            crossRef &&
            crossRef.module !== "$InternalEnum" &&
            allModules[crossRef.module]?.identifiers[renameIdentifier(ref.property.name)]
        ) {
            return renameIdentifier(ref.property.name);
        }

        console.warn(
            `unable to find reference of alias to other module '${ref.object?.name}' or to message ${ref.property?.name} of this module${currLoc}`,
        );
        return initialType;
    }

    return initialType;
}

/** Synthesises the {@code map<K, V>} type string from the field's value-tuple. */
function buildMapType(elements: AstNode[], info: ModuleInfo): string {
    if (elements[2]?.type !== "ArrayExpression") return "map";

    const subElements: AstNode[] = elements[2].elements;
    let out = "map<";
    subElements.forEach((element, index) => {
        if (element?.property?.name) {
            out += element.property.name.toLowerCase();
        } else {
            const ref = findByAlias(info.identifiers, element.name);
            out += ref?.name ?? element.name;
        }
        if (index < subElements.length - 1) out += ", ";
    });
    out += ">";
    return out;
}

/** Parses one field of an {@code internalSpec} ObjectExpression into a {@link MessageMember}. */
export function parseMember(
    keyName: string,
    elements: AstNode[],
    info: ModuleInfo,
    allModules: Record<string, ModuleInfo>,
    targetName: string,
): MessageMember {
    let type = "";
    const flags: string[] = [];

    unwrapBinaryOr(elements[1]).forEach((m: AstNode) => {
        if (m?.type === "MemberExpression" && m.object?.type === "MemberExpression") {
            if (m.object.property?.name === "TYPES") {
                type = m.property.name.toLowerCase();
                if (type === "map") {
                    type = buildMapType(elements, info);
                }
            } else if (m.object.property?.name === "FLAGS") {
                flags.push(m.property.name.toLowerCase());
            }
        }
    });

    if (type === "message" || type === "enum") {
        type = resolveReferenceType(elements, info, allModules, keyName, targetName, type);
    }

    return {
        name: keyName,
        id: elements[0]?.value,
        type,
        flags,
    };
}

/** Folds {@code __oneofs__} constraint groups into synthetic {@code __oneof__} members. */
export function applyConstraints(constraints: AstNode[], members: MessageMember[]): MessageMember[] {
    constraints.forEach((c: AstNode) => {
        if (c.key.name === "__oneofs__" && c.value?.type === "ObjectExpression") {
            const newOneOfs: MessageMember[] = c.value.properties.map((p: AstNode) => ({
                name: p.key.name,
                type: "__oneof__",
                flags: [],
                members: p.value.elements.map((e: AstNode) => {
                    const idx = members.findIndex((m) => m.name === e.value);
                    return members.splice(idx, 1)[0]!;
                }),
            }));
            members.push(...newOneOfs);
        }
    });
    return members;
}
