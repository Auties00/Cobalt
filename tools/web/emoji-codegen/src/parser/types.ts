/**
 * A single skin-tone variant of a base emoji.
 *
 * The {@link toneKey} mirrors WhatsApp Web's tone-key convention (WAWebEmoji's
 * {@code W()}): for a single-person emoji it is the one Fitzpatrick modifier;
 * for a multi-person emoji whose people share a tone it is that single modifier;
 * for a multi-person emoji with two distinct tones it is the two modifiers
 * concatenated in order.
 */
export interface SkinToneVariant {
    /** The Fitzpatrick modifier key (one modifier, or two concatenated for mixed tones). */
    readonly toneKey: string;
    /** The fully-formed variant emoji string. */
    readonly value: string;
}

/**
 * A base emoji as WhatsApp Web orders and categorizes it, before names are
 * joined from the Unicode data. Skin-tone variants are recorded against the
 * base rather than promoted to separate entries.
 */
export interface RawEmoji {
    /** The canonical emoji string, as it appears in WAWebEmojiJsonWaEmojiUnicode. */
    readonly value: string;
    /** The category id (one of WAWebEmojiConst.ORDERED_CATEGORY_IDS). */
    readonly category: string;
    /** The global canonical order index across all categories. */
    readonly order: number;
    /** Whether the emoji has at least one skin-tone variant. */
    readonly skinToneable: boolean;
    /** The skin-tone variants, empty when {@link skinToneable} is {@code false}. */
    readonly variants: readonly SkinToneVariant[];
}

/** A legacy/alternate encoding mapped to its modern canonical emoji string. */
export interface LegacyMapping {
    /** The legacy or alternate-qualification form (e.g. a SoftBank PUA code point). */
    readonly form: string;
    /** The modern canonical emoji the form resolves to. */
    readonly canonical: string;
}

/** One locale's search dictionary: keyword to the emoji(s) it suggests. */
export interface LocaleKeywords {
    /** The normalized locale tag (e.g. {@code "en"}, {@code "zh_TW"}). */
    readonly tag: string;
    /** The WhatsApp Web module the dictionary was read from. */
    readonly module: string;
    /** The keyword-to-emojis entries. */
    readonly entries: readonly { readonly keyword: string; readonly values: readonly string[] }[];
}

/** The raw catalog extracted in-page, before Unicode names are joined. */
export interface RawCatalog {
    /** The configured emoji type (e.g. {@code "WHATSAPP"}). */
    readonly emojiType: string;
    /** The ordered category ids. */
    readonly categories: readonly string[];
    /** The five Fitzpatrick skin-tone modifier strings. */
    readonly skinTones: readonly string[];
    /** The base emojis in canonical order. */
    readonly emojis: readonly RawEmoji[];
    /** The legacy/alternate encoding mappings. */
    readonly legacy: readonly LegacyMapping[];
    /** The per-locale search dictionaries. */
    readonly locales: readonly LocaleKeywords[];
    /** WhatsApp's curated popularity list (the search-ranking {@code u} array), most popular first. */
    readonly popular: readonly string[];
}

/** A base emoji with its joined Unicode name and derived Java constant identifier. */
export interface EmojiDef extends RawEmoji {
    /** The UPPER_SNAKE_CASE Java constant identifier (unique within the catalog). */
    readonly constant: string;
    /** The human-readable Unicode name (e.g. {@code "grinning face"}), used as the display name. */
    readonly displayName: string;
    /** The space-separated upper-hex code points (e.g. {@code "1F600"}), for the javadoc snippet. */
    readonly codepoints: string;
}

/** The fully resolved catalog the generators consume. */
export interface EmojiCatalog {
    readonly emojiType: string;
    readonly categories: readonly string[];
    readonly skinTones: readonly string[];
    readonly emojis: readonly EmojiDef[];
    readonly legacy: readonly LegacyMapping[];
    readonly locales: readonly LocaleKeywords[];
    /** WhatsApp's curated popularity list (the search-ranking {@code u} array), most popular first. */
    readonly popular: readonly string[];
    /** The number of base emojis whose Unicode name could not be resolved (fell back to a code-point name). */
    readonly unnamedCount: number;
}
