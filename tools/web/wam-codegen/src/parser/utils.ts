/** Result of a balanced-block extraction. */
export interface BalancedBlock {
    /** The extracted text including the opening and closing delimiters. */
    readonly text: string;
    /** The index in the source immediately after the closing delimiter. */
    readonly end: number;
}

/**
 * Extracts a balanced block delimited by {@link open}/{@link close}
 * starting from {@link startIndex}, skipping ahead to the first
 * occurrence of {@link open} if necessary.
 *
 * The extraction is string-literal–aware: characters inside single-,
 * double-, or template-quoted strings do not affect the depth counter.
 */
export function extractBalanced(
    content: string,
    startIndex: number,
    open: string,
    close: string,
): BalancedBlock | null {
    let depth = 0;
    let inString = false;
    let stringChar = "";
    let i = startIndex;

    while (i < content.length && content[i] !== open) i++;
    if (i >= content.length) return null;

    const bodyStart = i;

    for (; i < content.length; i++) {
        const char = content[i];
        const prev = i > 0 ? content[i - 1] : "";

        if (inString) {
            if (char === stringChar && prev !== "\\") inString = false;
            continue;
        }

        if (char === '"' || char === "'" || char === "`") {
            inString = true;
            stringChar = char;
        } else if (char === open) {
            depth++;
        } else if (char === close) {
            depth--;
            if (depth === 0) {
                return {
                    text: content.slice(bodyStart, i + 1),
                    end: i + 1,
                };
            }
        }
    }

    return null;
}
