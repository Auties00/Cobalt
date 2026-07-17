import type { ABPropDef, ABPropType } from "./types.js";

/**
 * Resolves a minified JS value to its string representation.
 *
 * <p>Handles the following patterns:
 * <ul>
 * <li>{@code !0} → {@code "true"}
 * <li>{@code !1} → {@code "false"}
 * <li>Quoted strings (single or double) → the unquoted content
 * <li>Numeric literals → their string form
 * </ul>
 */
function resolveValue(raw: string): string {
    if (raw === "!0") return "true";
    if (raw === "!1") return "false";

    // Quoted string: strip the surrounding quotes
    if ((raw.startsWith('"') && raw.endsWith('"')) ||
        (raw.startsWith("'") && raw.endsWith("'"))) {
        return raw.slice(1, -1);
    }

    // Numeric or other literal — return as-is
    return parseFloat(raw)?.toString() ?? raw;
}

/**
 * Validates that the given string is a recognized AB prop type.
 */
function isABPropType(value: string): value is ABPropType {
    return value === "bool" || value === "int" || value === "float" || value === "string";
}

/**
 * Parses all AB prop definitions from the concatenated WhatsApp Web JS.
 *
 * <p>The {@code WAWebABPropsConfigs} module declares a single object literal
 * where each key is a prop name and the value is a tuple:
 * {@code [code, "type", defaultValue, serverDefault]}.
 *
 * <p>Example from the minified source:
 * <pre>{@code
 * var e={add_member_system_message:[4579,"bool",!0,!0], ...}; i.ABPropConfigs=e
 * }</pre>
 */
export function parseABProps(jsContent: string): ABPropDef[] {
    // Locate the WAWebABPropsConfigs module body.
    // The module pattern is: __d("WAWebABPropsConfigs", ..., function(...) { ... })
    // Inside, the props are in: var e={...}; i.ABPropConfigs=e
    //
    // We search for the sentinel assignment and walk backwards to find the
    // opening brace of the object literal.
    const sentinel = ".ABPropConfigs=";
    const sentinelIdx = jsContent.indexOf(sentinel);
    if (sentinelIdx === -1) {
        throw new Error("Could not find ABPropConfigs assignment in JS source");
    }

    // The variable name assigned to (e.g. "e" in "i.ABPropConfigs=e")
    // Find the opening '{' of the object by scanning backwards from the sentinel
    // We need to find the matching 'var e={' or similar
    // Strategy: find the '{' that starts the object literal by searching backwards
    // for the pattern 'var <char>={' before the sentinel
    let objectStart = -1;
    for (let i = sentinelIdx; i >= 0; i--) {
        if (jsContent[i] === '{' &&
            i >= 6 &&
            jsContent.substring(i - 6, i).match(/var \w=/)) {
            objectStart = i;
            break;
        }
    }

    if (objectStart === -1) {
        throw new Error("Could not find ABPropConfigs object literal start");
    }

    // Find the matching closing brace
    let depth = 0;
    let objectEnd = -1;
    let inString = false;
    let stringChar = '';
    for (let i = objectStart; i < jsContent.length; i++) {
        const ch = jsContent[i];

        if (inString) {
            if (ch === '\\') {
                i++; // skip escaped char
            } else if (ch === stringChar) {
                inString = false;
            }
            continue;
        }

        if (ch === '"' || ch === "'") {
            inString = true;
            stringChar = ch;
        } else if (ch === '{') {
            depth++;
        } else if (ch === '}') {
            depth--;
            if (depth === 0) {
                objectEnd = i + 1;
                break;
            }
        }
    }

    if (objectEnd === -1) {
        throw new Error("Could not find matching '}' for ABPropConfigs object");
    }

    const objectBody = jsContent.substring(objectStart + 1, objectEnd - 1);

    // Parse each property: name:[code,"type",defaultValue,serverDefault]
    // We use a regex to match each entry.
    // Property names can contain letters, digits, and underscores.
    // Values inside the array can be: !0, !1, numbers, or quoted strings.
    const entryPattern =
        /(\w+):\[(\w+),"(\w+)",("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^,\]]+),("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^,\]]+)]/g;

    const props: ABPropDef[] = [];
    let match: RegExpExecArray | null;

    while ((match = entryPattern.exec(objectBody)) !== null) {
        const [, name, codeStr, type, defaultRaw, debugDefaultRaw] = match;

        if (!isABPropType(type)) {
            throw new Error(`Unknown AB prop type "${type}" for "${name}"`);
        }

        props.push({
            name,
            code: Math.floor(parseFloat(codeStr)),
            type,
            defaultValue: resolveValue(defaultRaw),
            debugDefaultValue: resolveValue(debugDefaultRaw),
            sourceDefinition: `${name}:[${codeStr},"${type}",${defaultRaw},${debugDefaultRaw}]`
        });
    }

    if (props.length === 0) {
        throw new Error("Parsed zero AB props — regex may need updating");
    }

    return props;
}
