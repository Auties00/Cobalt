import { mkdir, writeFile } from "node:fs/promises";
import { dirname } from "node:path";
import type { ParsedProtos } from "../types.js";
import { getEntityLines } from "./stringify.js";

/** Module-name prefix used by {@code parseProtobufCFromWasm} to label its synthetic module. */
const WASM_MODULE_PREFIX = "wasm:";

const JS_BANNER = [
    "// ============================================================",
    "// Extracted from WhatsApp Web JavaScript modules",
    "// (internalSpec declarations on protobuf-runtime classes)",
    "// ============================================================",
].join("\n");

const WASM_BANNER = [
    "// ============================================================",
    "// Extracted from WhatsApp Web WASM modules",
    "// (protobuf-c reflection tables in native code)",
    "// ============================================================",
].join("\n");

/** A top-level entity's serialised proto source together with its source module. */
interface DecodedEntity {
    /** The source module name this entity was declared in (last-wins on cross-module name collision). */
    readonly module: string;
    /** Whether {@link module} is a wasm module (its name carries the {@link WASM_MODULE_PREFIX}). */
    readonly isWasm: boolean;
    /** The serialised proto2 source of the entity, newline-joined. */
    readonly text: string;
}

/**
 * Builds a sub-section banner naming the source proto module the following
 * entities were extracted from.
 *
 * @param displayName - the module name to show in the banner.
 * @returns the banner comment block.
 */
function moduleBanner(displayName: string): string {
    return [
        "// ------------------------------------------------------------",
        `// ${displayName}`,
        "// ------------------------------------------------------------",
    ].join("\n");
}

/**
 * Renders one top-level section as a run of per-module sub-sections, each
 * introduced by a {@link moduleBanner}. Modules are ordered alphabetically by
 * name and each module's entities are sorted alphabetically within it.
 *
 * @param modules - entities grouped by source module name, each module holding
 *                  its entities keyed by canonical name.
 * @param displayNameOf - maps a source module name to the label shown in its
 *                        sub-section banner.
 * @returns the rendered sub-sections, or the empty string when no module carries
 *          any entity.
 */
function renderModuleSections(
    modules: Record<string, Record<string, string>>,
    displayNameOf: (moduleName: string) => string,
): string {
    return Object.keys(modules)
        .sort()
        .map((moduleName) => {
            const entities = modules[moduleName]!;
            const body = Object.keys(entities).sort().map((n) => entities[n]).join("\n");
            return `${moduleBanner(displayNameOf(moduleName))}\n\n${body}`;
        })
        .join("\n\n");
}

/**
 * Serialises every top-level identifier in {@code parsed} into proto2 source.
 *
 * @param parsed - the parsed proto schema to emit.
 * @param version - the WhatsApp Web version string to embed in the header.
 * @param pkg - the proto package declaration to emit.
 * @returns the proto2 source.
 *
 * @remarks
 * The output is split into two top-level sections: first the JS-derived protos
 * (extracted from {@code internalSpec} declarations), then the wasm-derived
 * protos (extracted from {@code protobuf-c} reflection tables inside native
 * modules). A banner comment introduces each section. Within a section the
 * entities are further grouped into per-module sub-sections, one per source
 * proto module (e.g. {@code WAWebProtobufsE2E.pb}), each introduced by its own
 * sub-banner; modules are ordered alphabetically and each module's entities are
 * sorted alphabetically within it. When the same entity name is declared by more
 * than one module, the last module in discovery order wins, matching the merge
 * dedup, and the entity is filed under that winning module.
 */
export function generateProtoSource(
    parsed: ParsedProtos,
    version: string,
    pkg: string,
): string {
    const decoded: Record<string, DecodedEntity> = {};

    for (const moduleName of parsed.moduleOrder) {
        const info = parsed.modulesInfo[moduleName]!;
        const isWasm = moduleName.startsWith(WASM_MODULE_PREFIX);

        for (const ident of Object.values(info.identifiers)) {
            const path = parsed.indentation[ident.name]?.indentation;
            if (path?.length) continue;

            const lines = getEntityLines(ident, info.identifiers, parsed.indentation);
            decoded[ident.name] = { module: moduleName, isWasm, text: lines.join("\n") };
        }
    }

    const jsModules: Record<string, Record<string, string>> = {};
    const wasmModules: Record<string, Record<string, string>> = {};
    for (const [name, entity] of Object.entries(decoded)) {
        const bucket = entity.isWasm ? wasmModules : jsModules;
        (bucket[entity.module] ??= {})[name] = entity.text;
    }

    const jsBody = renderModuleSections(jsModules, (m) => m);
    const wasmBody = renderModuleSections(wasmModules, (m) => m.slice(WASM_MODULE_PREFIX.length));

    const sections: string[] = [];
    if (jsBody.length) sections.push(`${JS_BANNER}\n\n${jsBody}`);
    if (wasmBody.length) sections.push(`${WASM_BANNER}\n\n${wasmBody}`);

    return `syntax = "proto2";\npackage ${pkg};\n\n/// WhatsApp Version: ${version}\n\n${sections.join("\n")}`;
}

/**
 * Writes the generated proto source to {@code outputPath}, creating parent
 * directories as needed.
 *
 * @param parsed - the parsed proto schema to emit.
 * @param version - the WhatsApp Web version string to embed in the header.
 * @param pkg - the proto package declaration to emit.
 * @param outputPath - the file path to write to.
 */
export async function writeProtoFile(
    parsed: ParsedProtos,
    version: string,
    pkg: string,
    outputPath: string,
): Promise<void> {
    await mkdir(dirname(outputPath), { recursive: true });
    await writeFile(outputPath, generateProtoSource(parsed, version, pkg));
}
