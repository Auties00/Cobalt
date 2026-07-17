import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import type { EmojiCatalog, EmojiDef } from "../parser/types.js";

/** The Fitzpatrick skin-tone constant names, in WhatsApp's modifier order (light to dark). */
const SKIN_TONE_NAMES = ["LIGHT", "MEDIUM_LIGHT", "MEDIUM", "MEDIUM_DARK", "DARK"] as const;

/**
 * The fixed public API block appended to {@code WhatsAppEmoji} after the constants:
 * thin delegators to the hand-written package-private helpers, so regeneration never
 * touches the behaviour implementation.
 */
const WHATSAPP_EMOJI_METHODS = `
    /**
     * Returns whether the given string is a recognized WhatsApp emoji in any
     * encoding (qualified, unqualified, legacy, or skin-tone variant).
     *
     * @param value the string to test, may be {@code null}
     * @return {@code true} if the string resolves to a known emoji
     */
    public static boolean isEmoji(String value) {
        return EmojiLookup.isEmoji(value);
    }

    /**
     * Resolves a string to its base emoji constant, accepting any encoding:
     * qualified or unqualified, a legacy code point, or a skin-tone variant (which
     * resolves to its tone-less base).
     *
     * @param value the emoji string, may be {@code null}
     * @return the base emoji, or empty when the string is not a known emoji
     */
    public static Optional<WhatsAppEmoji> of(String value) {
        return EmojiLookup.of(value);
    }

    /**
     * Normalizes an emoji string to its canonical form, folding variation
     * selectors and legacy encodings while preserving any skin tone.
     *
     * @param value the emoji string, may be {@code null}
     * @return the canonical emoji string, or empty when not a known emoji
     */
    public static Optional<String> normalize(String value) {
        return EmojiLookup.normalize(value);
    }

    /**
     * Returns every base emoji in canonical order.
     *
     * @return an immutable, canonically ordered list of all base emojis
     */
    public static List<WhatsAppEmoji> all() {
        return EmojiRegistry.ALL;
    }

    /**
     * Returns the base emojis in a category, in canonical order.
     *
     * @param category the category to list
     * @return an immutable list of the category's emojis, empty when none
     */
    public static List<WhatsAppEmoji> byCategory(EmojiCategory category) {
        return EmojiLookup.byCategory(category);
    }

    /**
     * Returns the skin tone applied to an emoji string, or {@link EmojiSkinTone#NONE}
     * when it carries no skin-tone modifier.
     *
     * @param value the emoji string, may be {@code null}
     * @return the skin tone, never {@code null}
     */
    public static EmojiSkinTone skinToneOf(String value) {
        return EmojiSkinTones.toneOf(value);
    }

    /**
     * Searches for emojis whose keywords match the query, ranked by relevance,
     * using the default locale.
     *
     * @param query the search text, may be {@code null}
     * @return the matching emojis, most relevant first; empty when none match
     */
    public static List<WhatsAppEmoji> search(String query) {
        return EmojiSearch.search(query, Locale.getDefault());
    }

    /**
     * Searches for emojis whose keywords match the query in the given locale,
     * ranked by relevance.
     *
     * @param query  the search text, may be {@code null}
     * @param locale the locale whose keyword dictionary to search
     * @return the matching emojis, most relevant first; empty when none match
     */
    public static List<WhatsAppEmoji> search(String query, Locale locale) {
        return EmojiSearch.search(query, locale);
    }

    /**
     * Applies a skin tone to this emoji, returning the variant string.
     *
     * @param tone the skin tone to apply
     * @return the variant emoji string, or empty when this emoji has no skin-tone variants
     */
    public Optional<String> withSkinTone(EmojiSkinTone tone) {
        return EmojiSkinTones.apply(this, tone);
    }

    /**
     * Applies two distinct skin tones to a multi-person emoji (handshake, holding
     * hands, kiss, couple with heart), returning the variant string.
     *
     * @param first  the first person's skin tone
     * @param second the second person's skin tone
     * @return the variant emoji string, or empty when this emoji has no such variant
     */
    public Optional<String> withSkinTone(EmojiSkinTone first, EmojiSkinTone second) {
        return EmojiSkinTones.apply(this, first, second);
    }
`;

function packageToPath(pkg: string): string {
  return pkg.replace(/\./g, "/");
}

/** Renders a string as an all-{@code \\uXXXX}-escaped Java string literal body (no surrounding quotes). */
function unicodeEscape(value: string): string {
  let out = "";
  for (let i = 0; i < value.length; i++) {
    out += "\\u" + value.charCodeAt(i).toString(16).toUpperCase().padStart(4, "0");
  }
  return out;
}

/** Renders an ASCII string as a Java string literal, escaping backslashes and quotes. */
function javaString(value: string): string {
  return '"' + value.replace(/\\/g, "\\\\").replace(/"/g, '\\"') + '"';
}

/** Formats a space-separated hex code-point string as {@code U+XXXX U+YYYY} for javadoc. */
function codepointsDoc(codepoints: string): string {
  return codepoints
    .split(" ")
    .map((h) => "U+" + h)
    .join(" ");
}

/** Title-cases a category id ({@code SMILEYS_PEOPLE} to {@code Smileys People}). */
function titleCase(id: string): string {
  return id
    .split("_")
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(" ");
}

function constantLine(e: EmojiDef): string[] {
  const lines: string[] = [];
  lines.push("");
  lines.push("    /**");
  lines.push(`     * The ${e.displayName} emoji.`);
  lines.push("     *");
  const skin = e.skinToneable ? " It has skin-tone variants." : "";
  lines.push(`     * <p>Code points: {@code ${codepointsDoc(e.codepoints)}}; category {@link EmojiCategory#${e.category}}.${skin}`);
  lines.push("     */");
  lines.push(
    `    public static final WhatsAppEmoji ${e.constant} = ` +
      `new WhatsAppEmoji("${unicodeEscape(e.value)}", ${javaString(e.displayName)}, ` +
      `EmojiCategory.${e.category}, ${e.order}, ${e.skinToneable});`,
  );
  return lines;
}

/** Generates {@code WhatsAppEmoji.java}: the record plus one constant per base emoji. */
function generateWhatsAppEmoji(catalog: EmojiCatalog, pkg: string): string {
  const ordered = [...catalog.emojis].sort((a, b) => a.order - b.order);
  const lines: string[] = [];

  lines.push(`package ${pkg};`);
  lines.push("");
  lines.push("import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;");
  lines.push("");
  lines.push("import java.util.List;");
  lines.push("import java.util.Locale;");
  lines.push("import java.util.Optional;");
  lines.push("");
  lines.push("/**");
  lines.push(" * A single WhatsApp emoji: its canonical Unicode {@link #value() value}, display");
  lines.push(" * {@link #name() name}, {@link #category() category}, canonical {@link #order() order},");
  lines.push(" * and whether it {@link #skinToneable() has skin-tone variants}.");
  lines.push(" *");
  lines.push(" * <p>The constants on this record are the WhatsApp emoji set in its canonical");
  lines.push(" * order. Skin-tone variants are not separate constants; they are derived from a base");
  lines.push(" * emoji via {@link #withSkinTone(EmojiSkinTone)}. Lookup ({@link #of(String)}),");
  lines.push(" * normalization ({@link #normalize(String)}), and locale-aware {@link #search(String)}");
  lines.push(" * are static methods on this type, backed by package-private helpers.");
  lines.push(" *");
  lines.push(" * <p>This record and its constants are generated by {@code tools/web/emoji-codegen}");
  lines.push(" * from the WhatsApp Web emoji modules; the constant names come from the Unicode");
  lines.push(" * emoji names. Do not edit them manually.");
  lines.push(" *");
  lines.push(" * @param value         the canonical Unicode emoji string");
  lines.push(" * @param name          the human-readable Unicode name (e.g. {@code grinning face})");
  lines.push(" * @param category      the category the emoji belongs to");
  lines.push(" * @param order         the canonical sort index across the whole catalog");
  lines.push(" * @param skinToneable  whether the emoji has at least one skin-tone variant");
  lines.push(" */");
  lines.push('@WhatsAppWebModule(moduleName = "WAWebEmoji")');
  lines.push("public record WhatsAppEmoji(String value, String name, EmojiCategory category, int order, boolean skinToneable) {");

  for (const e of ordered) lines.push(...constantLine(e));

  lines.push(WHATSAPP_EMOJI_METHODS);
  lines.push("}");
  lines.push("");

  return lines.join("\n");
}

/** Generates {@code EmojiCategory.java} from the ordered category ids. */
function generateEmojiCategory(catalog: EmojiCatalog, pkg: string): string {
  const lines: string[] = [];
  lines.push(`package ${pkg};`);
  lines.push("");
  lines.push("import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;");
  lines.push("");
  lines.push("/**");
  lines.push(" * The categories a {@link WhatsAppEmoji} can belong to, in the canonical order");
  lines.push(" * WhatsApp presents them.");
  lines.push(" *");
  lines.push(" * <p>Generated by {@code tools/web/emoji-codegen}; do not edit manually.");
  lines.push(" */");
  lines.push('@WhatsAppWebModule(moduleName = "WAWebEmojiConst")');
  lines.push("public enum EmojiCategory {");
  catalog.categories.forEach((id, index) => {
    if (index > 0) lines.push("");
    lines.push("    /**");
    lines.push(`     * The ${titleCase(id)} category.`);
    lines.push("     */");
    const last = index === catalog.categories.length - 1;
    lines.push(`    ${id}${last ? ";" : ","}`);
  });
  lines.push("}");
  lines.push("");
  return lines.join("\n");
}

/** Generates {@code EmojiSkinTone.java} from the Fitzpatrick modifier list. */
function generateSkinTone(catalog: EmojiCatalog, pkg: string): string {
  const lines: string[] = [];
  lines.push(`package ${pkg};`);
  lines.push("");
  lines.push("import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;");
  lines.push("");
  lines.push("/**");
  lines.push(" * A Fitzpatrick skin-tone modifier applied to a skin-toneable {@link WhatsAppEmoji}.");
  lines.push(" *");
  lines.push(" * <p>{@link #NONE} is the default, unmodified tone. Each other constant carries the");
  lines.push(" * Unicode modifier {@link #modifier() code point} appended to a base emoji to form its");
  lines.push(" * variant.");
  lines.push(" *");
  lines.push(" * <p>Generated by {@code tools/web/emoji-codegen}; do not edit manually.");
  lines.push(" */");
  lines.push('@WhatsAppWebModule(moduleName = "WAWebEmoji")');
  lines.push("public enum EmojiSkinTone {");
  lines.push("    /**");
  lines.push("     * No skin-tone modifier; the base emoji is used unchanged.");
  lines.push("     */");
  lines.push('    NONE(""),');
  catalog.skinTones.forEach((modifier, index) => {
    const name = SKIN_TONE_NAMES[index] ?? `TONE_${index + 1}`;
    const last = index === catalog.skinTones.length - 1;
    lines.push("");
    lines.push("    /**");
    lines.push(`     * The ${titleCase(name)} skin tone.`);
    lines.push("     */");
    lines.push(`    ${name}("${unicodeEscape(modifier)}")${last ? ";" : ","}`);
  });
  lines.push("");
  lines.push("    /** The Unicode skin-tone modifier, or the empty string for {@link #NONE}. */");
  lines.push("    private final String modifier;");
  lines.push("");
  lines.push("    /**");
  lines.push("     * Constructs a skin tone with its Unicode modifier.");
  lines.push("     *");
  lines.push("     * @param modifier the modifier code point, or the empty string for no modifier");
  lines.push("     */");
  lines.push("    EmojiSkinTone(String modifier) {");
  lines.push("        this.modifier = modifier;");
  lines.push("    }");
  lines.push("");
  lines.push("    /**");
  lines.push("     * Returns the Unicode skin-tone modifier this tone appends to a base emoji.");
  lines.push("     *");
  lines.push("     * @return the modifier code point, or the empty string for {@link #NONE}");
  lines.push("     */");
  lines.push("    public String modifier() {");
  lines.push("        return modifier;");
  lines.push("    }");
  lines.push("}");
  lines.push("");
  return lines.join("\n");
}

/** Generates the package-private {@code EmojiRegistry.java}: the ordered list of every base emoji constant. */
function generateEmojiRegistry(catalog: EmojiCatalog, pkg: string): string {
  const ordered = [...catalog.emojis].sort((a, b) => a.order - b.order);
  const lines: string[] = [];
  lines.push(`package ${pkg};`);
  lines.push("");
  lines.push("import java.util.List;");
  lines.push("");
  lines.push("/**");
  lines.push(" * The canonically ordered, immutable list of every base {@link WhatsAppEmoji}");
  lines.push(" * constant, used internally for enumeration, lookup indexing, and search without");
  lines.push(" * reflecting over the constant fields.");
  lines.push(" *");
  lines.push(" * <p>Generated by {@code tools/web/emoji-codegen}; do not edit manually.");
  lines.push(" */");
  lines.push("final class EmojiRegistry {");
  lines.push("    /**");
  lines.push("     * Every base emoji constant, in canonical order.");
  lines.push("     */");
  lines.push("    static final List<WhatsAppEmoji> ALL = List.of(");
  lines.push(ordered.map((e) => "            WhatsAppEmoji." + e.constant).join(",\n"));
  lines.push("    );");
  lines.push("");
  lines.push("    /**");
  lines.push("     * Prevents instantiation.");
  lines.push("     */");
  lines.push("    private EmojiRegistry() {");
  lines.push("        throw new AssertionError();");
  lines.push("    }");
  lines.push("}");
  lines.push("");
  return lines.join("\n");
}

/**
 * Writes {@code WhatsAppEmoji.java}, {@code EmojiCategory.java},
 * {@code EmojiSkinTone.java}, and the package-private {@code EmojiRegistry.java}
 * into the package directory under {@code outputDir}.
 *
 * @returns the number of emoji constants written
 */
export async function writeJavaSources(catalog: EmojiCatalog, outputDir: string, pkg: string): Promise<number> {
  const dir = join(outputDir, packageToPath(pkg));
  await mkdir(dir, { recursive: true });

  await writeFile(join(dir, "EmojiCategory.java"), generateEmojiCategory(catalog, pkg));
  await writeFile(join(dir, "EmojiSkinTone.java"), generateSkinTone(catalog, pkg));
  await writeFile(join(dir, "WhatsAppEmoji.java"), generateWhatsAppEmoji(catalog, pkg));
  await writeFile(join(dir, "EmojiRegistry.java"), generateEmojiRegistry(catalog, pkg));

  return catalog.emojis.length;
}
