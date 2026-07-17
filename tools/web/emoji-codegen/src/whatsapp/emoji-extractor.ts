import { withForceLoadedBundle } from "./runtime.js";
import type { RawCatalog } from "../parser/types.js";

/**
 * Runs inside the WhatsApp Web page. Configures EmojiUtil with the WhatsApp
 * emoji set and reads the catalog directly from the static modules:
 *
 * <ul>
 *   <li>WAWebEmojiConst - category order, emoji type, skin-tone modifiers;</li>
 *   <li>WAWebEmojiJsonWaEmoji{Unicode,Legacy,Category} - the raw data;</li>
 *   <li>WAWebEmoji.EmojiUtil - per-category base lists and skin-tone variants;</li>
 *   <li>WAWebLocalesCldrEmojiSuggestion* - the per-locale keyword dictionaries.</li>
 * </ul>
 *
 * Names are NOT resolved here; they are joined from the Unicode data in Node.
 */
function interceptInPage(): RawCatalog {
  const w = window as any;
  const req = w.require as (name: string) => any;
  // window.require returns these JSON modules unwrapped, so .default is absent; tolerate both shapes.
  const def = (name: string): any => {
    const m = req(name);
    return m != null && m.default !== undefined ? m.default : m;
  };

  const Const = req("WAWebEmojiConst");
  const EmojiMod = req("WAWebEmoji");
  const EmojiUtil = EmojiMod.EmojiUtil;
  const orderedEmojis = def("WAWebEmojiJsonWaEmojiUnicode");
  const legacyToEmoji = def("WAWebEmojiJsonWaEmojiLegacy");
  const categorizedEmojis = def("WAWebEmojiJsonWaEmojiCategory");
  const emojiType: string = Const.EMOJI_TYPE.WHATSAPP;

  EmojiUtil.configure({ emojiType, orderedEmojis, legacyToEmoji, categorizedEmojis });

  const skinTones: string[] = (EmojiUtil.skinToneVariations as string[]).slice();
  const categories: string[] = (Const.ORDERED_CATEGORY_IDS as string[]).slice();
  const isMultiSkinTone = EmojiMod.isBaseMultiSkinToneEmoji as (e: string) => boolean;

  // Mirrors WAWebEmoji's W(): equal tones collapse to one modifier, mixed tones concatenate.
  const toneKey = (tones: string[]): string => (tones.every((t) => t === tones[0]) ? tones[0] : tones.join(""));

  const emojis: {
    value: string;
    category: string;
    order: number;
    skinToneable: boolean;
    variants: { toneKey: string; value: string }[];
  }[] = [];
  let order = 0;
  for (const category of categories) {
    const list: string[] = EmojiUtil.getEmojisInCategory(category) || [];
    for (const value of list) {
      const byKey: Record<string, string> = {};
      if (isMultiSkinTone(value)) {
        for (const t1 of skinTones) {
          for (const t2 of skinTones) {
            const v = EmojiUtil.getSkinToneVariant(value, [t1, t2]);
            if (typeof v === "string" && v.length > 0) byKey[toneKey([t1, t2])] = v;
          }
        }
      } else {
        for (const t of skinTones) {
          const v = EmojiUtil.getSkinToneVariant(value, [t]);
          if (typeof v === "string" && v.length > 0) byKey[t] = v;
        }
      }
      const variants = Object.keys(byKey).map((k) => ({ toneKey: k, value: byKey[k] }));
      emojis.push({ value, category, order: order++, skinToneable: variants.length > 0, variants });
    }
  }

  // Resolve every legacy/alternate form to the modern canonical emoji it shares an index with.
  const canonicalAt = (index: number): string | null => {
    const e = orderedEmojis[index];
    if (e == null || e === "") return null;
    return Array.isArray(e) ? e[0] : e;
  };
  const legacy: { form: string; canonical: string }[] = [];
  for (const form of Object.keys(legacyToEmoji)) {
    const canonical = canonicalAt(legacyToEmoji[form]);
    if (canonical == null || form === canonical) continue;
    legacy.push({ form, canonical });
  }

  // Derives a locale tag from a module suffix: "En" -> "en", "ZhTw" -> "zh_TW".
  const tagOf = (moduleName: string): string => {
    const suffix = moduleName.replace(/^WAWebLocalesCldrEmojiSuggestion/, "");
    if (suffix.length === 2) return suffix.toLowerCase();
    return suffix.slice(0, 2).toLowerCase() + "_" + suffix.slice(2).toUpperCase();
  };
  const localeModules: string[] = (w.__waDefinedModules || []).filter((n: string) =>
    /^WAWebLocalesCldrEmojiSuggestion[A-Z]/.test(n),
  );
  const locales: { tag: string; module: string; entries: { keyword: string; values: string[] }[] }[] = [];
  for (const moduleName of localeModules) {
    let dict: any;
    try {
      dict = def(moduleName);
    } catch (e) {
      continue;
    }
    if (!dict || typeof dict !== "object") continue;
    const entries: { keyword: string; values: string[] }[] = [];
    for (const keyword of Object.keys(dict)) {
      const values = dict[keyword];
      if (Array.isArray(values) && values.length > 0) {
        entries.push({ keyword, values: values.filter((v: unknown) => typeof v === "string") });
      }
    }
    if (entries.length > 0) locales.push({ tag: tagOf(moduleName), module: moduleName, entries });
  }

  // WhatsApp's curated popularity list (the search `u` array) sits just before the category-id array.
  const popular: string[] = (() => {
    const src = w.__waEmojiSearchSource;
    if (typeof src !== "string") return [];
    const match = src.match(/\[((?:"(?:\\.|[^"\\])*"\s*,?\s*)+)\]\s*,\s*[A-Za-z_$][\w$]*\s*=\s*\["SMILEYS_PEOPLE"/);
    if (!match) return [];
    try {
      const parsed = JSON.parse("[" + match[1] + "]");
      return Array.isArray(parsed) ? parsed.filter((v: unknown) => typeof v === "string") : [];
    } catch (e) {
      return [];
    }
  })();

  return { emojiType, categories, skinTones, emojis, legacy, locales, popular };
}

/** Extracts the raw WhatsApp Web emoji catalog (names not yet joined). */
export async function extractRawCatalog(): Promise<RawCatalog> {
  return withForceLoadedBundle(interceptInPage);
}
