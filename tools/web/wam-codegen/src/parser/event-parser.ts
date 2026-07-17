import { extractBalanced } from "./utils.js";
import type {
    WamChannel,
    WamEventDef,
    WamFieldDef,
    WamFieldType,
    WamWeights,
} from "./types.js";

// Alias map: local variable → module name (or codegen-utils sentinel)

const CODEGEN_UTILS = "__CODEGEN_UTILS__" as const;

type AliasMap = ReadonlyMap<string, string>;

/**
 * Builds a map of local single-letter variable aliases to the WAM enum
 * module names they reference. Also records the codegen-utils alias.
 */
function buildAliasMap(body: string): Map<string, string> {
    const map = new Map<string, string>();

    // (v=o("WAWebWamEnumFoo"))
    for (const m of body.matchAll(/\((\w)=o\("(WAWebWamEnum[^"]+)"\)\)/g)) {
        map.set(m[1], m[2]);
    }

    // v=o("WAWebWamEnumFoo").EXPORT  (without wrapping parens)
    for (const m of body.matchAll(/(\w)=o\("(WAWebWamEnum[^"]+)"\)\.\w+/g)) {
        if (!map.has(m[1])) map.set(m[1], m[2]);
    }

    // (v=o("WAWebWamCodegenUtils"))
    for (const m of body.matchAll(/\((\w)=o\("WAWebWamCodegenUtils"\)\)/g)) {
        map.set(m[1], CODEGEN_UTILS);
    }

    return map;
}

// Type resolution

interface ResolvedType {
    readonly wamType: WamFieldType;
    readonly enumJavaName: string | null;
}

function resolveType(
    typeRef: string,
    aliasMap: AliasMap,
    exportMap: ReadonlyMap<string, string>,
): ResolvedType {
    const trimmed = typeRef.trim();

    // .TYPES.STRING / .TYPES.INTEGER / ...
    const typesMatch = trimmed.match(/\.TYPES\.(STRING|INTEGER|BOOLEAN|NUMBER|TIMER)$/);
    if (typesMatch) {
        const jsType = typesMatch[1] as keyof typeof TYPES_MAP;
        return { wamType: TYPES_MAP[jsType], enumJavaName: null };
    }

    // o("WAWebWamEnumFoo").EXPORT_NAME  or  (v=o("WAWebWamEnumFoo")).EXPORT_NAME
    const directMatch = trimmed.match(/o\("(WAWebWamEnum[^"]+)"\)\)?\.(\w+)/);
    if (directMatch) {
        return resolveEnumRef(directMatch[1], directMatch[2], exportMap);
    }

    // localAlias.EXPORT_NAME
    const dotMatch = trimmed.match(/^(\w+)\.(\w+)$/);
    if (dotMatch) {
        const moduleName = aliasMap.get(dotMatch[1]);
        if (moduleName && moduleName !== CODEGEN_UTILS) {
            return resolveEnumRef(moduleName, dotMatch[2], exportMap);
        }
    }

    // Bare EXPORT_NAME
    const bareJava = exportMap.get(trimmed);
    if (bareJava) {
        return { wamType: "ENUM", enumJavaName: bareJava };
    }

    throw new Error(`Could not resolve type reference: "${trimmed}"`);
}

const TYPES_MAP = {
    STRING: "STRING",
    INTEGER: "INTEGER",
    BOOLEAN: "BOOLEAN",
    NUMBER: "FLOAT",
    TIMER: "TIMER",
} as const satisfies Record<string, WamFieldType>;

function resolveEnumRef(
    moduleName: string,
    exportName: string,
    exportMap: ReadonlyMap<string, string>,
): ResolvedType {
    const qualifiedKey = `${moduleName}.${exportName}`;
    const javaName =
        exportMap.get(qualifiedKey)
        ?? exportMap.get(exportName)
        ?? toFallbackEnumName(moduleName);
    return { wamType: "ENUM", enumJavaName: javaName };
}

function toFallbackEnumName(moduleName: string): string {
    const name = moduleName.replace(/^WAWebWamEnum/, "");
    return name.endsWith("Event") ? name + "Type" : name;
}

// Field parsing

function parseFields(
    fieldsStr: string,
    aliasMap: AliasMap,
    exportMap: ReadonlyMap<string, string>,
): WamFieldDef[] {
    const fields: WamFieldDef[] = [];
    let i = 0;

    while (i < fieldsStr.length) {
        // field name
        const nameMatch = fieldsStr.slice(i).match(/^(\w+)\s*:\s*\[/);
        if (!nameMatch) { i++; continue; }

        const fieldName = nameMatch[1];
        i += nameMatch[0].length;

        // field id
        const idMatch = fieldsStr.slice(i).match(/^\s*(\d+)\s*,\s*/);
        if (!idMatch) { i++; continue; }

        const fieldId = parseInt(idMatch[1], 10);
        i += idMatch[0].length;

        // type reference — consume until the unbalanced closing ']'
        let depth = 0;
        const typeStart = i;
        while (i < fieldsStr.length) {
            const ch = fieldsStr[i];
            if (ch === "(" || ch === "[") depth++;
            else if (ch === ")") depth--;
            else if (ch === "]") {
                if (depth === 0) break;
                depth--;
            }
            i++;
        }

        const typeRef = fieldsStr.slice(typeStart, i).trim();
        i++; // skip ']'

        const resolved = resolveType(typeRef, aliasMap, exportMap);
        fields.push({
            name: fieldName,
            id: fieldId,
            wamType: resolved.wamType,
            enumJavaName: resolved.enumJavaName,
        });

        // skip comma / whitespace
        while (i < fieldsStr.length && (fieldsStr[i] === "," || fieldsStr[i] === " ")) i++;
    }

    return fields;
}

// Weight / channel parsing

function parseWeights(raw: string): WamWeights {
    const parts = raw.split(",").map((s) => Number(s.trim()));
    return {
        alpha: parts[0] ?? 1,
        beta: parts[1] ?? 1,
        release: parts[2] ?? 1,
    };
}

function parseChannel(raw: string | undefined): WamChannel {
    if (raw === "realtime" || raw === "private") return raw;
    return "regular";
}

// Public API

/**
 * Scans raw WhatsApp Web JS content for all WAM event module definitions
 * and returns the parsed event list.
 *
 * @param jsContent  Concatenated JS source
 * @param exportMap  Enum export map produced by {@link parseEnums}
 */
export function parseEvents(
    jsContent: string,
    exportMap: ReadonlyMap<string, string>,
): WamEventDef[] {
    const events: WamEventDef[] = [];
    const modulePattern = /__d\("(WAWeb[^"]*WamEvent)"/g;
    let moduleMatch: RegExpExecArray | null;

    while ((moduleMatch = modulePattern.exec(jsContent)) !== null) {
        const moduleName = moduleMatch[1];

        // Extract module function body
        const bodyBlock = extractBalanced(
            jsContent,
            moduleMatch.index + moduleMatch[0].length,
            "{",
            "}",
        );
        if (!bodyBlock) continue;
        const body = bodyBlock.text;

        const aliasMap = buildAliasMap(body);

        // Locate defineEvents({
        const defIdx = body.indexOf("defineEvents(");
        if (defIdx === -1) continue;

        const defObjBlock = extractBalanced(body, defIdx + "defineEvents(".length, "{", "}");
        if (!defObjBlock) continue;
        const defObj = defObjBlock.text;

        // Event name — the single key of the definition object
        const eventNameMatch = defObj.match(/^\{(\w+)\s*:/);
        if (!eventNameMatch) continue;
        const eventName = eventNameMatch[1];

        // Extract the array value after "EventName:"
        const colonIdx = defObj.indexOf(":", eventNameMatch[0].indexOf(eventName));
        if (colonIdx === -1) continue;

        const arrayBlock = extractBalanced(defObj, colonIdx + 1, "[", "]");
        if (!arrayBlock) continue;
        const arrayContent = arrayBlock.text.slice(1, -1); // strip outer []

        // Event ID
        const eventIdMatch = arrayContent.match(/^\s*([\d.e+]+)\s*,/);
        if (!eventIdMatch) continue;
        const eventId = Math.round(Number(eventIdMatch[1]));

        // Fields object
        const fieldsBlock = extractBalanced(arrayContent, arrayContent.indexOf("{"), "{", "}");
        if (!fieldsBlock) continue;
        const fields = parseFields(fieldsBlock.text.slice(1, -1), aliasMap, exportMap);

        // Remainder after the fields object: ,[weights],"channel",psId
        const afterFields = arrayContent.slice(fieldsBlock.end);

        const weightsBlock = extractBalanced(afterFields, 0, "[", "]");
        const weights = weightsBlock
            ? parseWeights(weightsBlock.text.slice(1, -1))
            : { alpha: 1, beta: 1, release: 1 };

        const channelMatch = afterFields.match(/"(regular|realtime|private)"/);
        const channel = parseChannel(channelMatch?.[1]);

        let privateStatsId = -1;
        if (channelMatch) {
            const tail = afterFields.slice(
                afterFields.indexOf(channelMatch[0]) + channelMatch[0].length,
            );
            const psIdMatch = tail.match(/,\s*(-?\d+)/);
            if (psIdMatch) privateStatsId = parseInt(psIdMatch[1], 10);
        }

        events.push({
            moduleName,
            eventName,
            javaClassName: `${eventName}Event`,
            eventId,
            fields,
            weights,
            channel,
            privateStatsId,
        });
    }

    return events;
}
