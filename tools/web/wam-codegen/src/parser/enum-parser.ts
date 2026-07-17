import { extractBalanced } from "./utils.js";
import type { WamEnumConstant, WamEnumDef } from "./types.js";

/** Result returned by {@link parseEnums}. */
export interface EnumParseResult {
    readonly enums: readonly WamEnumDef[];
    /** Maps JS export names (bare or qualified) to Java enum class names. */
    readonly exportMap: Map<string, string>;
}

/**
 * Derives a Java enum class name from a WAM enum module name by
 * stripping the {@code WAWebWamEnum} prefix.
 */
function toJavaEnumName(moduleName: string): string {
    const name = moduleName.replace(/^WAWebWamEnum/, "");
    return name.endsWith("Event") ? name + "Type" : name;
}

/**
 * Parses {@code Object.freeze({...})} constant bodies into an array
 * of name–value pairs.
 */
function parseConstants(body: string): WamEnumConstant[] {
    const constants: WamEnumConstant[] = [];
    const pattern = /(\w+)\s*:\s*(-?\d+)/g;
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(body)) !== null) {
        constants.push({ name: match[1], value: parseInt(match[2], 10) });
    }
    return constants;
}

/**
 * Finds all UPPER_CASE export names in a module body by looking for
 * assignment patterns like {@code i.EXPORT_NAME = e}.
 */
function findExportNames(body: string): string[] {
    const names: string[] = [];
    const pattern = /[a-zA-Z_$]\w*\.([A-Z][A-Z0-9_]*)\s*=\s*[a-zA-Z_$]\w*/g;
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(body)) !== null) {
        names.push(match[1]);
    }
    return names;
}

/**
 * Scans raw WhatsApp Web JS content for all WAM enum module definitions
 * ({@code __d("WAWebWamEnum...")} blocks) and returns the parsed enums
 * together with an export-name lookup map.
 */
export function parseEnums(jsContent: string): EnumParseResult {
    const enums: WamEnumDef[] = [];
    const exportMap = new Map<string, string>();

    const modulePattern = /__d\("(WAWebWamEnum[^"]+)"/g;
    let moduleMatch: RegExpExecArray | null;

    while ((moduleMatch = modulePattern.exec(jsContent)) !== null) {
        const moduleName = moduleMatch[1];
        const javaName = toJavaEnumName(moduleName);

        const body = extractBalanced(
            jsContent,
            moduleMatch.index + moduleMatch[0].length,
            "{",
            "}",
        );
        if (!body) continue;

        const freezeMatch = body.text.match(/Object\.freeze\(\{([^}]+)\}\)/);
        if (!freezeMatch) continue;

        const constants = parseConstants(freezeMatch[1]);
        if (constants.length === 0) continue;

        const exportNames = findExportNames(body.text);
        for (const name of exportNames) {
            exportMap.set(name, javaName);
            exportMap.set(`${moduleName}.${name}`, javaName);
        }

        enums.push({ moduleName, javaName, constants });
    }

    return { enums, exportMap };
}
