/**
 * Extracts every {@code .wasm} URL embedded in arbitrary text -- HTML, JSON,
 * or JS source.
 *
 * @param text - arbitrary text to scan.
 * @returns the set of URLs found, with JSON-encoded forward slashes
 *          ({@code \/}) decoded.
 *
 * @remarks
 * WhatsApp's resource manifest ({@code HasteSupportData}) embeds wasm URLs
 * as JSON-escaped strings inside an inline {@code <script>}, so the pattern
 * must accept both {@code /} and {@code \/}.
 */
export function scanForWasmUrls(text: string): string[] {
    const pattern = /https?:(?:\\?\/)+[^\s"'`<>(){}\\]+\.wasm/g;
    const matches = text.match(pattern);
    if (!matches) return [];
    return matches.map((u) => u.replaceAll("\\/", "/"));
}
