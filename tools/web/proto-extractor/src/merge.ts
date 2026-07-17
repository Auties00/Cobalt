import type { IndentationEntry, ParsedProtos } from "./types.js";

/**
 * Merges multiple {@link ParsedProtos} fragments into a single result.
 *
 * @remarks
 * - {@code modulesInfo} entries are concatenated; if two fragments register the
 *   same module name, the later one wins and a warning is emitted.
 * - {@code moduleOrder} entries are concatenated while preserving first-seen order.
 * - {@code indentation} entries are merged by unioning their {@code members} sets;
 *   the {@code indentation} string takes the value from the first fragment that
 *   provided it.
 *
 * @param parts - parsed-proto fragments to merge, in priority order (later
 *                fragments override earlier ones on module-name conflict).
 * @returns the merged result.
 */
export function mergeProtos(parts: readonly ParsedProtos[]): ParsedProtos {
    const modulesInfo: ParsedProtos["modulesInfo"] = {};
    const moduleOrder: string[] = [];
    const indentation: Record<string, IndentationEntry> = {};

    for (const part of parts) {
        for (const [name, info] of Object.entries(part.modulesInfo)) {
            if (name in modulesInfo) {
                console.warn(`[WARN] Duplicate module label '${name}', last wins`);
            }
            modulesInfo[name] = info;
        }
        for (const name of part.moduleOrder) {
            if (!moduleOrder.includes(name)) moduleOrder.push(name);
        }
        for (const [key, entry] of Object.entries(part.indentation)) {
            const existing = indentation[key];
            const mergedMembers = new Set<string>([
                ...(existing?.members ?? []),
                ...(entry.members ?? []),
            ]);
            indentation[key] = {
                indentation: existing?.indentation ?? entry.indentation,
                members: mergedMembers.size > 0 ? mergedMembers : undefined,
            };
        }
    }

    return { modulesInfo, indentation, moduleOrder };
}

/**
 * Counts identifiers that are top-level (i.e. have no {@code $}-prefixed parent path).
 *
 * @param parsed - the parsed-proto fragment to inspect.
 * @returns the number of top-level identifiers across all modules.
 */
export function countTopLevel(parsed: ParsedProtos): number {
    let count = 0;
    for (const info of Object.values(parsed.modulesInfo)) {
        for (const ident of Object.values(info.identifiers)) {
            if (!parsed.indentation[ident.name]?.indentation?.length) count++;
        }
    }
    return count;
}
