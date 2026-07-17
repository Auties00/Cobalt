import { parse } from "acorn";
import * as walk from "acorn-walk";
import type { JsChunk } from "../fetch/index.js";
import type {
    CrossRef,
    EnumValue,
    Identifier,
    IndentationEntry,
    MessageMember,
    ModuleInfo,
    ParsedProtos,
} from "../types.js";
import {
    type AstNode,
    extractAllExpressions,
    getNesting,
    renameIdentifier,
} from "./ast.js";
import { applyConstraints, findByAlias, parseMember } from "./members.js";

/**
 * Proto modules that are never exchanged with the server and are therefore
 * filtered out of extraction: client-local IndexedDB row blobs
 * ({@code WAWebProtobufsMdStorage*}), the libsignal on-disk record structures
 * ({@code WASignalLocalStorage*}), the local media-entry cache
 * ({@code WAMediaEntryData.pb}), and the out-of-band P2P safety-number
 * fingerprints ({@code WAFingerprint.pb}, {@code WAWebProtobufsFingerprintV3.pb}).
 * Filtering is module-scoped, never type-name-scoped, because several local type
 * names (e.g. {@code PollEncValue}, {@code PollOption}) collide with distinct wire
 * types declared in modules that are kept.
 */
const EXCLUDED_PROTO_MODULES: ReadonlySet<string> = new Set([
    "WAMediaEntryData.pb",
    "WAFingerprint.pb",
    "WAWebProtobufsFingerprintV3.pb",
]);

/** Returns whether the named proto module is client-local and should be skipped. */
function isExcludedProtoModule(moduleName: string): boolean {
    return moduleName.startsWith("WAWebProtobufsMdStorage")
        || moduleName.startsWith("WASignalLocalStorage")
        || EXCLUDED_PROTO_MODULES.has(moduleName);
}

/** Parses one JS chunk and returns every top-level {@code __d("Module", ...)} call whose body assigns to {@code <alias>.internalSpec}. */
function findProtoModules(chunk: JsChunk): AstNode[] {
    /* Some chunks contain a `Foo$Bar` enum split across two declarations.
     * The legacy extractor patched one specific symbol; we apply the same
     * defensive patch so the AST parse succeeds. */
    const patched = chunk.content.replaceAll(
        "LimitSharing$Trigger",
        "LimitSharing$TriggerType",
    );

    let body: AstNode[];
    try {
        body = parse(patched, { ecmaVersion: "latest", sourceType: "script" }).body as AstNode[];
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        console.warn(`[WARN] Skipping ${chunk.url} (parse failure): ${message}`);
        return [];
    }

    return body.filter((m) => {
        const expressions = extractAllExpressions(m);
        return expressions.some((e) => e?.left?.property?.name === "internalSpec");
    });
}

/** Walks one proto module and collects every cross-module {@code r("$Other")} alias assignment. */
function collectCrossRefs(module: AstNode, info: ModuleInfo): void {
    walk.simple(module, {
        AssignmentExpression(node: AstNode) {
            if (
                node?.right?.type === "CallExpression" &&
                Array.isArray(node.right.arguments) &&
                node.right.arguments.length === 1 &&
                node.right.arguments[0].type !== "ObjectExpression"
            ) {
                const ref: CrossRef = {
                    alias: node.left.name,
                    module: node.right.arguments[0].value,
                };
                info.crossRefs.push(ref);
            }
        },
    });
}

/** Walks one proto module and seeds an empty {@link Identifier} for every member assignment. */
function collectIdentifiers(
    module: AstNode,
    info: ModuleInfo,
    indentation: Record<string, IndentationEntry>,
): void {
    const assignments: AstNode[] = [];
    walk.simple(module, {
        AssignmentExpression(node: AstNode) {
            const left = node?.left;
            const propName = left?.property?.name;
            if (
                propName &&
                propName !== "internalSpec" &&
                propName !== "internalDefaults" &&
                propName !== "name"
            ) {
                assignments.push(left);
            }
        },
    });

    const seeded: [string, Identifier][] = assignments.map((a) => {
        const key = renameIdentifier(a.property.name);
        const parentPath = getNesting(key);

        indentation[key] = indentation[key] ?? {};
        indentation[key]!.indentation = parentPath;

        if (parentPath.length) {
            indentation[parentPath] = indentation[parentPath] ?? {};
            indentation[parentPath]!.members = indentation[parentPath]!.members ?? new Set<string>();
            indentation[parentPath]!.members!.add(key);
        }

        return [key, { name: key }];
    });

    /* The legacy code reversed before building the lookup table so that when
     * the same identifier is assigned twice in source order, the FIRST one
     * wins after Object.fromEntries dedupes by key. */
    info.identifiers = Object.fromEntries(seeded.reverse());
}

/**
 * Extracts enum {@code (name, id)} pairs from a bare object literal whose values
 * are all numeric literals (e.g. {@code {EVERYONE:1, SILENT:2}}). Returns
 * {@code null} when the node is not such an all-numeric object, which excludes
 * empty message-spec initializers ({@code {}}) and field-spec arrays.
 *
 * @param node - the candidate {@code ObjectExpression} AST node.
 * @returns the enum value list, or {@code null} when the node is not an enum body.
 */
function bareEnumValues(node: AstNode): EnumValue[] | null {
    if (node?.type !== "ObjectExpression" || !node.properties?.length) {
        return null;
    }
    const values: EnumValue[] = [];
    for (const p of node.properties) {
        if (p?.type !== "Property") return null;
        const negative = p.value?.type === "UnaryExpression" && p.value.operator === "-";
        const literal = negative ? p.value.argument : p.value;
        if (literal?.type !== "Literal" || typeof literal.value !== "number") {
            return null;
        }
        const name = p.key?.name ?? p.key?.value;
        if (name == null) return null;
        values.push({ name, id: negative ? -literal.value : literal.value });
    }
    return values;
}

/**
 * Walks one proto module and collects the enum bodies keyed by their local alias.
 *
 * @remarks
 * WA Web emits enums in two shapes:
 * - {@code A.internalSpec={NAME:0, ...}} - direct ObjectExpression assignment to
 *   {@code internalSpec}.
 * - {@code A=t({NAME:0, ...})} - wrapper-call form where the values live inside
 *   the first call argument.
 */
function collectEnumAliases(module: AstNode): Record<string, EnumValue[]> {
    const enumAliases: Record<string, EnumValue[]> = {};
    walk.ancestor(module, {
        Property(node: AstNode, _state: unknown, ancestors: AstNode[]) {
            const father = ancestors[ancestors.length - 3];
            const grandfather = ancestors[ancestors.length - 4];

            if (
                father?.type === "AssignmentExpression" &&
                father?.left?.property?.name === "internalSpec" &&
                father?.right?.properties?.length
            ) {
                const values: EnumValue[] = father.right.properties.map((p: AstNode) => ({
                    name: p.key.name,
                    id: p.value.value,
                }));
                enumAliases[father.left.name] = values;
            } else if (
                node?.key?.name &&
                father?.arguments?.length > 0
            ) {
                const values: EnumValue[] = father.arguments[0]?.properties?.map((p: AstNode) => ({
                    name: p.key.name,
                    id: p.value.value,
                })) ?? [];
                const nameAlias = grandfather?.left?.name ?? grandfather?.id?.name;
                if (nameAlias) {
                    enumAliases[nameAlias] = values;
                }
            }
        },
    } as walk.AncestorVisitors<unknown>);

    /* Enums declared as bare object-literal variables (e.g. `s={EVERYONE:1,...}`)
     * exported under an ALL_CAPS name: neither an `internalSpec` assignment nor a
     * wrapper call, so the passes above miss them. Keyed by the local variable
     * name, matching how a field's enum reference is resolved by alias. */
    walk.simple(module, {
        VariableDeclarator(node: AstNode) {
            const values = bareEnumValues(node.init);
            if (values && node.id?.name) {
                enumAliases[node.id.name] = values;
            }
        },
        AssignmentExpression(node: AstNode) {
            if (node?.left?.type === "Identifier") {
                const values = bareEnumValues(node.right);
                if (values) {
                    enumAliases[node.left.name] = values;
                }
            }
        },
    });

    return enumAliases;
}

/** Resolves each identifier's local alias and, for enums, copies the matching value list onto it. */
function linkIdentifierAliases(
    module: AstNode,
    info: ModuleInfo,
    enumAliases: Record<string, EnumValue[]>,
): void {
    walk.simple(module, {
        AssignmentExpression(node: AstNode) {
            if (
                node?.left?.type === "MemberExpression" &&
                info.identifiers[renameIdentifier(node.left.property.name)]
            ) {
                const ident = info.identifiers[renameIdentifier(node.left.property.name)]!;
                ident.alias = node.right.name;
                ident.enumValues = enumAliases[ident.alias!];
            }
        },
    });
}

/** Walks one module and attaches the field list to every identifier whose {@code internalSpec} was declared inline. */
function collectMessageMembers(
    module: AstNode,
    info: ModuleInfo,
    allModules: Record<string, ModuleInfo>,
): void {
    walk.simple(module, {
        AssignmentExpression(node: AstNode) {
            if (
                node?.left?.type !== "MemberExpression" ||
                node.left.property?.name !== "internalSpec" ||
                node.right?.type !== "ObjectExpression"
            ) {
                return;
            }

            const targetIdent = findByAlias(info.identifiers, node.left.object.name);
            if (!targetIdent) {
                console.warn(`found message specification for unknown identifier alias: ${node.left.object.name}`);
                return;
            }

            const constraints: AstNode[] = [];
            const rawMembers: AstNode[] = [];
            for (const p of node.right.properties) {
                p.key.name = p.key.type === "Identifier" ? p.key.name : p.key.value;
                (p.key.name.substring(0, 2) === "__" ? constraints : rawMembers).push(p);
            }

            const parsed = rawMembers.map((p) =>
                parseMember(p.key.name, p.value.elements, info, allModules, targetIdent.name),
            );

            targetIdent.members = applyConstraints(constraints, parsed);
        },
    });
}

/** Converts a {@code SCREAMING_SNAKE} token to {@code PascalCase} (e.g. {@code SPECIAL_TEXT_SIZE} -> {@code SpecialTextSize}). */
function screamingToPascal(name: string): string {
    return name
        .split("_")
        .filter((word) => word.length > 0)
        .map((word) => word[0]!.toUpperCase() + word.slice(1).toLowerCase())
        .join("");
}

/** Converts a {@code $}-nested PascalCase path to its {@code SCREAMING_SNAKE} export prefix (e.g. {@code ConsumerApplication$Metadata} -> {@code CONSUMER_APPLICATION_METADATA}). */
function pascalPathToScreaming(name: string): string {
    return name
        .split("$")
        .map((segment) => segment.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toUpperCase())
        .join("_");
}

/** Returns whether any member (recursing into oneof groups) uses {@code typeName} as its field type. */
function memberTreeReferences(members: MessageMember[], typeName: string): boolean {
    return members.some(
        (m) => m.type === typeName || (m.members != null && memberTreeReferences(m.members, typeName)),
    );
}

/** Rewrites every member field type (recursing into oneof groups) named in {@code renames} to its new name. */
function renameMemberTypes(members: MessageMember[], renames: Map<string, string>): void {
    for (const m of members) {
        const renamed = renames.get(m.type);
        if (renamed) m.type = renamed;
        if (m.members) renameMemberTypes(m.members, renames);
    }
}

/**
 * Re-homes enums declared as bare {@code SCREAMING_SNAKE} export constants onto
 * their real proto location.
 *
 * @param modulesInfo - per-module parsed identifiers, mutated in place.
 * @param indentation - the nesting map, updated so re-homed enums emit nested.
 *
 * @remarks
 * WhatsApp exports a nested enum under a flattened uppercase key (e.g.
 * {@code Command.CommandType} as {@code COMMAND_COMMAND_TYPE}) with no
 * {@code .name}, so the field passes leave it top-level under that screaming key.
 * proto2 scopes enum values to the enclosing scope (C++ scoping), so two such
 * top-level enums sharing a value name (e.g. {@code NONE}) collide and fail to
 * compile. This pass nests each one under the single same-module message that
 * references it, reconstructing the PascalCase leaf by stripping the parent's
 * screaming-snake prefix; an enum referenced cross-module or by no message stays
 * top-level but is still de-screamed. Every affected field reference is rewritten
 * to the new canonical name.
 */
function renestBareEnums(
    modulesInfo: Record<string, ModuleInfo>,
    indentation: Record<string, IndentationEntry>,
): void {
    const renames = new Map<string, string>();

    for (const info of Object.values(modulesInfo)) {
        const messages = Object.values(info.identifiers).filter((it) => it.members);
        for (const enumIdent of Object.values(info.identifiers)) {
            if (
                !enumIdent.enumValues?.length ||
                enumIdent.members ||
                !/^[A-Z][A-Z0-9_]*$/.test(enumIdent.name)
            ) {
                continue;
            }

            const oldName = enumIdent.name;
            const referrers = messages.filter((m) => memberTreeReferences(m.members ?? [], oldName));

            let newName: string;
            if (referrers.length === 1) {
                const parent = referrers[0]!;
                const prefix = `${pascalPathToScreaming(parent.name)}_`;
                const suffix = oldName.startsWith(prefix) ? oldName.slice(prefix.length) : oldName;
                newName = `${parent.name}$${screamingToPascal(suffix)}`;
                indentation[newName] = { indentation: parent.name };
                const slot = (indentation[parent.name] ??= {});
                (slot.members ??= new Set<string>()).add(newName);
            } else {
                newName = screamingToPascal(oldName);
                if (newName === oldName) continue;
                indentation[newName] = { indentation: "" };
            }

            delete info.identifiers[oldName];
            delete indentation[oldName];
            renames.set(oldName, newName);
            info.identifiers[newName] = { ...enumIdent, name: newName };
        }
    }

    for (const info of Object.values(modulesInfo)) {
        for (const ident of Object.values(info.identifiers)) {
            if (ident.members) renameMemberTypes(ident.members, renames);
        }
    }
}

/**
 * Extracts every protobuf identifier (messages and enums) declared across the
 * given JS chunks served by WhatsApp Web.
 *
 * @param chunks - JS chunks downloaded from WhatsApp Web.
 * @returns the extracted schema in {@link ParsedProtos} shape.
 *
 * @remarks
 * Runs three passes per module so cross-references resolve cleanly:
 * 1. Seed identifiers, collect cross-module aliases, attach enum bodies.
 * 2. Link the local one-letter alias of each identifier.
 * 3. Resolve every {@code internalSpec} field, including cross-module
 *    references.
 */
export function parseProtoModules(chunks: readonly JsChunk[]): ParsedProtos {
    const modulesInfo: Record<string, ModuleInfo> = {};
    const indentation: Record<string, IndentationEntry> = {};
    const moduleOrder: string[] = [];
    const allModuleNodes: { name: string; module: AstNode }[] = [];

    for (const chunk of chunks) {
        const modules = findProtoModules(chunk);
        for (const module of modules) {
            const moduleName: string | undefined = module?.expression?.arguments?.[0]?.value;
            if (!moduleName || modulesInfo[moduleName] || isExcludedProtoModule(moduleName)) continue;

            modulesInfo[moduleName] = { crossRefs: [], identifiers: {} };
            moduleOrder.push(moduleName);
            allModuleNodes.push({ name: moduleName, module });

            collectCrossRefs(module, modulesInfo[moduleName]!);
        }
    }

    for (const { name, module } of allModuleNodes) {
        const info = modulesInfo[name]!;
        collectIdentifiers(module, info, indentation);
        const enumAliases = collectEnumAliases(module);
        linkIdentifierAliases(module, info, enumAliases);
    }

    for (const { name, module } of allModuleNodes) {
        collectMessageMembers(module, modulesInfo[name]!, modulesInfo);
    }

    renestBareEnums(modulesInfo, indentation);

    return { modulesInfo, indentation, moduleOrder };
}
