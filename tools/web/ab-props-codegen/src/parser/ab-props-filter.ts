import type { ABPropDef } from "./types.js";

/**
 * Extracts the set of AB prop names that are actually referenced in the
 * WhatsApp Web JS source code (outside their definition in
 * {@code WAWebABPropsConfigs}).
 *
 * <p>The primary accessor pattern is:
 * {@code getABPropConfigValue("prop_name")}, but prop names may also
 * appear as quoted string literals in arrays, variables, or other contexts.
 * This function searches for every occurrence of each prop name as a quoted
 * string outside the {@code ABPropConfigs} definition block.
 */
function findReferencedPropNames(
    jsContent: string,
    configBlockStart: number,
    configBlockEnd: number,
): Set<string> {
    const referenced = new Set<string>();

    // Find all getABPropConfigValue("...") / getABPropConfigValue('...')
    const pattern = /getABPropConfigValue\(["']([^"']+)["']\)/g;
    let match: RegExpExecArray | null;

    while ((match = pattern.exec(jsContent)) !== null) {
        // Skip matches inside the ABPropConfigs definition block
        if (match.index >= configBlockStart && match.index < configBlockEnd) {
            continue;
        }
        referenced.add(match[1]);
    }

    return referenced;
}

/**
 * Locates the boundaries of the {@code ABPropConfigs} object literal in the
 * JS source. Returns {@code [start, end]} indices, or {@code null} if not
 * found.
 */
function findConfigBlock(jsContent: string): [number, number] | null {
    const sentinel = ".ABPropConfigs=";
    const sentinelIdx = jsContent.indexOf(sentinel);
    if (sentinelIdx === -1) return null;

    let objectStart = -1;
    for (let i = sentinelIdx; i >= 0; i--) {
        if (
            jsContent[i] === "{" &&
            i >= 6 &&
            jsContent.substring(i - 6, i).match(/var \w=/)
        ) {
            objectStart = i;
            break;
        }
    }

    if (objectStart === -1) return null;

    let depth = 0;
    let inString = false;
    let stringChar = "";
    for (let i = objectStart; i < jsContent.length; i++) {
        const ch = jsContent[i];

        if (inString) {
            if (ch === "\\") {
                i++;
            } else if (ch === stringChar) {
                inString = false;
            }
            continue;
        }

        if (ch === '"' || ch === "'") {
            inString = true;
            stringChar = ch;
        } else if (ch === "{") {
            depth++;
        } else if (ch === "}") {
            depth--;
            if (depth === 0) {
                return [objectStart, i + 1];
            }
        }
    }

    return null;
}

/**
 * Filters the given AB prop definitions to only include those whose names
 * are referenced elsewhere in the WhatsApp Web JS source code.
 *
 * <p>This prevents generating constants for AB props that are defined but
 * never actually used by any module.
 *
 * @param props     the full list of parsed AB prop definitions
 * @param jsContent the concatenated WhatsApp Web JS source
 * @returns an object with the filtered props and the count of removed props
 */
export function filterReferencedProps(
    props: readonly ABPropDef[],
    jsContent: string,
): { filtered: ABPropDef[]; removedCount: number } {
    const block = findConfigBlock(jsContent);
    const [blockStart, blockEnd] = block ?? [-1, -1];

    const referenced = findReferencedPropNames(jsContent, blockStart, blockEnd);

    const filtered = props.filter((prop) => referenced.has(prop.name));
    return {
        filtered,
        removedCount: props.length - filtered.length,
    };
}
