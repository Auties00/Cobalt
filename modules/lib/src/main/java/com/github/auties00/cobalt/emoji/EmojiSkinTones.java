package com.github.auties00.cobalt.emoji;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Backs the skin-tone behaviour of {@link WhatsAppEmoji}: applying tones to a base
 * emoji, detecting the tone of a string, and resolving the canonical variant form
 * for normalization.
 *
 * <p>The variant table ({@code base value -> tone key -> variant value}) is loaded
 * lazily from {@code emoji_skintones.bin} on first use and shared by every caller,
 * so skin-tone variants are never duplicated in memory. The tone key mirrors
 * WhatsApp's convention: a single modifier for a one-person or uniformly-toned
 * emoji, or two modifiers concatenated for a multi-person emoji with distinct tones.
 */
final class EmojiSkinTones {
    /**
     * The lowest Fitzpatrick skin-tone modifier code point (U+1F3FB, light).
     */
    private static final int TONE_MIN = 0x1F3FB;

    /**
     * The highest Fitzpatrick skin-tone modifier code point (U+1F3FF, dark).
     */
    private static final int TONE_MAX = 0x1F3FF;

    /**
     * Prevents instantiation.
     */
    private EmojiSkinTones() {
        throw new AssertionError();
    }

    /**
     * Holder for the lazily-loaded variant table, initialized on first access.
     */
    private static final class Table {
        /**
         * Maps a base emoji value to its tone-key-to-variant map.
         */
        private static final Map<String, Map<String, String>> BY_BASE = load();

        /**
         * Prevents instantiation.
         */
        private Table() {
            throw new AssertionError();
        }

        /**
         * Reads the variant table from {@code emoji_skintones.bin}.
         *
         * @return the base-to-tone-to-variant table
         * @throws UncheckedIOException if the resource cannot be read
         */
        private static Map<String, Map<String, String>> load() {
            try (var in = EmojiResources.open("emoji_skintones.bin")) {
                var bases = in.readInt();
                Map<String, Map<String, String>> table = HashMap.newHashMap(bases);
                for (var i = 0; i < bases; i++) {
                    var base = EmojiResources.readString(in);
                    var variants = in.readInt();
                    Map<String, String> tones = HashMap.newHashMap(variants);
                    for (var j = 0; j < variants; j++) {
                        var key = EmojiResources.readString(in);
                        tones.put(key, EmojiResources.readString(in));
                    }
                    table.put(base, tones);
                }
                return table;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Applies a single skin tone to a base emoji.
     *
     * @param base the base emoji
     * @param tone the tone to apply
     * @return the variant string, the base value itself for {@link EmojiSkinTone#NONE},
     *         or empty when the base has no skin-tone variants
     */
    static Optional<String> apply(WhatsAppEmoji base, EmojiSkinTone tone) {
        if (!base.skinToneable()) {
            return Optional.empty();
        }
        if (tone == EmojiSkinTone.NONE) {
            return Optional.of(base.value());
        }
        return lookup(base.value(), tone.modifier());
    }

    /**
     * Applies two skin tones to a multi-person base emoji.
     *
     * @param base   the base emoji
     * @param first  the first person's tone
     * @param second the second person's tone
     * @return the variant string, the base value itself when both tones are
     *         {@link EmojiSkinTone#NONE}, or empty when no such variant exists
     */
    static Optional<String> apply(WhatsAppEmoji base, EmojiSkinTone first, EmojiSkinTone second) {
        if (!base.skinToneable()) {
            return Optional.empty();
        }
        var a = first.modifier();
        var b = second.modifier();
        if (a.isEmpty() && b.isEmpty()) {
            return Optional.of(base.value());
        }
        return lookup(base.value(), a.equals(b) ? a : a + b);
    }

    /**
     * Returns the skin tone carried by an emoji string.
     *
     * @param value the emoji string, may be {@code null}
     * @return the tone of the first skin-tone modifier present, or
     *         {@link EmojiSkinTone#NONE} when there is none
     */
    static EmojiSkinTone toneOf(String value) {
        if (value == null) {
            return EmojiSkinTone.NONE;
        }
        for (var i = 0; i < value.length(); ) {
            var codePoint = value.codePointAt(i);
            if (isToneModifier(codePoint)) {
                return forModifier(new String(Character.toChars(codePoint)));
            }
            i += Character.charCount(codePoint);
        }
        return EmojiSkinTone.NONE;
    }

    /**
     * Resolves the canonical variant of a base emoji for a toned input string,
     * using the same tone-key convention as the variant table.
     *
     * @param base       the tone-less base emoji
     * @param tonedInput the original string carrying one or two skin-tone modifiers
     * @return the canonical variant string, the base value when no modifier is
     *         present, or empty when the base has no matching variant
     */
    static Optional<String> variantFor(WhatsAppEmoji base, String tonedInput) {
        List<String> modifiers = new ArrayList<>(2);
        for (var i = 0; i < tonedInput.length(); ) {
            var codePoint = tonedInput.codePointAt(i);
            if (isToneModifier(codePoint)) {
                modifiers.add(new String(Character.toChars(codePoint)));
            }
            i += Character.charCount(codePoint);
        }
        if (modifiers.isEmpty()) {
            return Optional.of(base.value());
        }
        var first = modifiers.get(0);
        var uniform = modifiers.stream().allMatch(first::equals);
        return lookup(base.value(), uniform ? first : String.join("", modifiers));
    }

    /**
     * Removes every Fitzpatrick skin-tone modifier from a string.
     *
     * @param value the string to strip
     * @return the string with all skin-tone modifiers removed, or the same instance
     *         when it carries none
     */
    static String stripModifiers(String value) {
        var toned = false;
        for (var i = 0; i < value.length(); ) {
            var codePoint = value.codePointAt(i);
            if (isToneModifier(codePoint)) {
                toned = true;
                break;
            }
            i += Character.charCount(codePoint);
        }
        if (!toned) {
            return value;
        }
        var stripped = new StringBuilder(value.length());
        for (var i = 0; i < value.length(); ) {
            var codePoint = value.codePointAt(i);
            if (!isToneModifier(codePoint)) {
                stripped.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return stripped.toString();
    }

    /**
     * Looks up a variant by base value and tone key.
     *
     * @param baseValue the base emoji value
     * @param toneKey   the tone key (one modifier, or two concatenated)
     * @return the variant string, or empty when there is no such base or key
     */
    private static Optional<String> lookup(String baseValue, String toneKey) {
        var tones = Table.BY_BASE.get(baseValue);
        if (tones == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tones.get(toneKey));
    }

    /**
     * Returns the tone whose modifier equals the given modifier string.
     *
     * @param modifier the single modifier code point as a string
     * @return the matching tone, or {@link EmojiSkinTone#NONE} when none matches
     */
    private static EmojiSkinTone forModifier(String modifier) {
        for (var tone : EmojiSkinTone.values()) {
            if (tone.modifier().equals(modifier)) {
                return tone;
            }
        }
        return EmojiSkinTone.NONE;
    }

    /**
     * Returns whether a code point is a Fitzpatrick skin-tone modifier.
     *
     * @param codePoint the code point to test
     * @return {@code true} when the code point is in U+1F3FB..U+1F3FF
     */
    private static boolean isToneModifier(int codePoint) {
        return codePoint >= TONE_MIN && codePoint <= TONE_MAX;
    }
}
