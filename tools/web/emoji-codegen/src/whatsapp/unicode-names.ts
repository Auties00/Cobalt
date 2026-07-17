/**
 * Resolves human-readable emoji names from the Unicode emoji-test.txt data file.
 *
 * WhatsApp Web supplies the emoji SET, ORDER, categories, and keywords, but no
 * per-emoji canonical name (its keyword dictionaries are inverted keyword maps).
 * The canonical name used to derive a stable Java constant identifier therefore
 * comes from Unicode's authoritative emoji-test.txt, joined to the WhatsApp set
 * by code-point sequence.
 */

const EMOJI_TEST_URL = (version: string): string =>
  `https://www.unicode.org/Public/emoji/${version}/emoji-test.txt`;

/** Removes U+FE0F variation selectors, matching WAWebEmoji's unqualify (F). */
function unqualify(value: string): string {
  return value.replace(/️/g, "");
}

/** A name lookup keyed by both the exact and the unqualified code-point sequence. */
export interface NameLookup {
  /** Returns the canonical Unicode name for a value, or {@code null} when unknown. */
  nameFor(value: string): string | null;
  /** The Unicode emoji version the names were sourced from. */
  readonly version: string;
}

/**
 * Fetches and parses emoji-test.txt for the given Unicode emoji version and
 * returns a name lookup. Every status (fully-qualified, minimally-qualified,
 * unqualified, component) is indexed so a WhatsApp value joins regardless of how
 * it is qualified.
 */
export async function fetchUnicodeNames(version: string): Promise<NameLookup> {
  const url = EMOJI_TEST_URL(version);
  console.log(`[INFO] Fetching Unicode names from ${url}`);
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch ${url}: HTTP ${res.status}`);
  const text = await res.text();
  // When version is "latest" the path is stable but the data is the newest release; report the real number.
  const resolvedVersion = text.match(/Version:\s*(\S+)/i)?.[1] ?? version;

  // Lines look like: "1F600 ; fully-qualified # <emoji> E1.0 grinning face"
  const line = /^([0-9A-Fa-f][0-9A-Fa-f ]*?)\s*;\s*\S+\s*#\s*\S+\s+E\d+(?:\.\d+)?\s+(.+?)\s*$/;
  const byExact = new Map<string, string>();
  const byUnqualified = new Map<string, string>();

  for (const raw of text.split("\n")) {
    if (raw.length === 0 || raw[0] === "#") continue;
    const m = line.exec(raw);
    if (m === null) continue;
    const codepoints = m[1].trim().split(/\s+/).map((h) => parseInt(h, 16));
    const value = String.fromCodePoint(...codepoints);
    const name = m[2];
    if (!byExact.has(value)) byExact.set(value, name);
    const u = unqualify(value);
    if (!byUnqualified.has(u)) byUnqualified.set(u, name);
  }

  console.log(`[INFO] Parsed ${byExact.size} Unicode emoji names (emoji ${resolvedVersion})`);

  return {
    version: resolvedVersion,
    nameFor(value: string): string | null {
      return byExact.get(value) ?? byUnqualified.get(unqualify(value)) ?? null;
    },
  };
}
