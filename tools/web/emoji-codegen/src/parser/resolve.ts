import type { EmojiCatalog, EmojiDef, RawCatalog } from "./types.js";
import type { NameLookup } from "../whatsapp/unicode-names.js";

/** Returns the space-separated upper-hex code-point sequence of a value. */
function codepointsOf(value: string): string {
  return Array.from(value)
    .map((c) => c.codePointAt(0)!.toString(16).toUpperCase().padStart(4, "0"))
    .join(" ");
}

/** Sanitizes a name into a valid, idiomatic UPPER_SNAKE_CASE Java identifier. */
function toIdentifier(name: string): string {
  let id = name
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
  if (id.length === 0) id = "EMOJI";
  if (/^[0-9]/.test(id)) id = "_" + id;
  return id;
}

/**
 * Joins Unicode names onto the raw catalog, derives unique Java constant
 * identifiers and code-point strings, and returns the resolved catalog.
 *
 * When a base emoji has no Unicode name (e.g. a WhatsApp-specific sequence that
 * predates or postdates the fetched emoji version), the constant falls back to a
 * code-point identifier ({@code EMOJI_<hex>}) and the count is reported so the
 * mismatch is visible rather than silent.
 */
export function resolveCatalog(raw: RawCatalog, names: NameLookup): EmojiCatalog {
  const used = new Set<string>();
  let unnamedCount = 0;

  const emojis: EmojiDef[] = raw.emojis.map((e) => {
    const unicodeName = names.nameFor(e.value);
    const codepoints = codepointsOf(e.value);
    let displayName: string;
    let baseId: string;
    if (unicodeName !== null) {
      displayName = unicodeName;
      baseId = toIdentifier(unicodeName);
    } else {
      unnamedCount++;
      displayName = "emoji " + codepoints.toLowerCase();
      baseId = "EMOJI_" + codepoints.replace(/ /g, "_");
    }

    let constant = baseId;
    for (let i = 2; used.has(constant); i++) constant = `${baseId}_${i}`;
    used.add(constant);

    return { ...e, constant, displayName, codepoints };
  });

  return {
    emojiType: raw.emojiType,
    categories: raw.categories,
    skinTones: raw.skinTones,
    emojis,
    legacy: raw.legacy,
    locales: raw.locales,
    popular: raw.popular,
    unnamedCount,
  };
}
