package com.github.auties00.cobalt.emoji;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Backs the lookup, normalization, and enumeration methods of {@link WhatsAppEmoji}.
 *
 * <p>The lookup index ({@code unqualified form -> base emoji}) covers every base
 * emoji and every legacy code point; it is built lazily on first use from
 * {@link EmojiRegistry#ALL} and {@code emoji_legacy.bin}. Skin-tone variants are
 * deliberately not stored here: a toned input is resolved by stripping its
 * modifiers to find the base, and its canonical toned form is reconstructed from
 * the shared {@link EmojiSkinTones} table, so variant strings live in exactly one
 * place.
 */
final class EmojiLookup {
    /**
     * The Unicode variation selector (U+FE0F) folded away during normalization.
     */
    private static final char VARIATION_SELECTOR = '\uFE0F';

    /**
     * Prevents instantiation.
     */
    private EmojiLookup() {
        throw new AssertionError();
    }

    /**
     * Holder for the lazily-built indices, initialized on first access.
     */
    private static final class Index {
        /**
         * Maps an unqualified base or legacy form to its base emoji.
         */
        private static final Map<String, WhatsAppEmoji> BY_FORM = buildForms();

        /**
         * Groups the base emojis by category, each list in canonical order.
         */
        private static final Map<EmojiCategory, List<WhatsAppEmoji>> BY_CATEGORY = buildCategories();

        /**
         * Prevents instantiation.
         */
        private Index() {
            throw new AssertionError();
        }

        /**
         * Builds the unqualified-form index from the constants plus the legacy map.
         *
         * @return the form-to-base index
         * @throws UncheckedIOException if the legacy resource cannot be read
         */
        private static Map<String, WhatsAppEmoji> buildForms() {
            var all = EmojiRegistry.ALL;
            Map<String, WhatsAppEmoji> forms = HashMap.newHashMap(all.size() * 2);
            for (var emoji : all) {
                forms.put(unqualify(emoji.value()), emoji);
            }
            try (var in = EmojiResources.open("emoji_legacy.bin")) {
                var count = in.readInt();
                for (var i = 0; i < count; i++) {
                    var form = EmojiResources.readString(in);
                    var canonical = EmojiResources.readString(in);
                    var base = forms.get(unqualify(canonical));
                    if (base == null) {
                        base = forms.get(EmojiSkinTones.stripModifiers(unqualify(canonical)));
                    }
                    if (base != null) {
                        forms.putIfAbsent(unqualify(form), base);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return forms;
        }

        /**
         * Groups every base emoji by its category, preserving canonical order.
         *
         * @return an immutable-list-valued category map
         */
        private static Map<EmojiCategory, List<WhatsAppEmoji>> buildCategories() {
            Map<EmojiCategory, List<WhatsAppEmoji>> groups = new EnumMap<>(EmojiCategory.class);
            for (var emoji : EmojiRegistry.ALL) {
                groups.computeIfAbsent(emoji.category(), key -> new ArrayList<>()).add(emoji);
            }
            groups.replaceAll((key, list) -> Collections.unmodifiableList(list));
            return groups;
        }
    }

    /**
     * Returns whether a string resolves to a known emoji.
     *
     * @param value the string to test, may be {@code null}
     * @return {@code true} when the string is a known emoji in any encoding
     */
    static boolean isEmoji(String value) {
        return of(value).isPresent();
    }

    /**
     * Resolves a string to its base emoji, stripping any skin tone.
     *
     * @param value the emoji string, may be {@code null}
     * @return the base emoji, or empty when the string is not a known emoji
     */
    static Optional<WhatsAppEmoji> of(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        var unqualified = unqualify(value);
        var direct = Index.BY_FORM.get(unqualified);
        if (direct != null) {
            return Optional.of(direct);
        }
        var stripped = EmojiSkinTones.stripModifiers(unqualified);
        if (stripped.equals(unqualified)) {
            return Optional.empty();
        }
        return Optional.ofNullable(Index.BY_FORM.get(stripped));
    }

    /**
     * Normalizes an emoji string to its canonical form, folding variation
     * selectors and legacy encodings while preserving any skin tone.
     *
     * @param value the emoji string, may be {@code null}
     * @return the canonical emoji string, or empty when not a known emoji
     */
    static Optional<String> normalize(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        var unqualified = unqualify(value);
        var direct = Index.BY_FORM.get(unqualified);
        if (direct != null) {
            return Optional.of(direct.value());
        }
        var stripped = EmojiSkinTones.stripModifiers(unqualified);
        if (stripped.equals(unqualified)) {
            return Optional.empty();
        }
        var base = Index.BY_FORM.get(stripped);
        if (base == null) {
            return Optional.empty();
        }
        return EmojiSkinTones.variantFor(base, value);
    }

    /**
     * Returns the base emojis in a category, in canonical order.
     *
     * @param category the category to list
     * @return an immutable list of the category's emojis, empty when none
     */
    static List<WhatsAppEmoji> byCategory(EmojiCategory category) {
        return Index.BY_CATEGORY.getOrDefault(category, List.of());
    }

    /**
     * Removes the Unicode variation selector (U+FE0F) from a string, matching
     * WhatsApp's emoji unqualification.
     *
     * @param value the string to unqualify
     * @return the string without U+FE0F, or the same instance when it has none
     */
    static String unqualify(String value) {
        return value.indexOf(VARIATION_SELECTOR) < 0 ? value : value.replace("\uFE0F", "");
    }
}
