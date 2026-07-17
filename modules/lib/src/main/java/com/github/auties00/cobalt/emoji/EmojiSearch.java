package com.github.auties00.cobalt.emoji;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs {@link WhatsAppEmoji#search(String, Locale)}: tokenizes a query, prefix-
 * matches each word against a locale keyword index, and ranks the matched emojis.
 *
 * <p>Indices are built lazily per locale and cached, so only the locales actually
 * searched are loaded; the default locale's index is reached through the same
 * cache. Ranking is stateless and mirrors WhatsApp's order, minus user recency:
 * by the number of query words an emoji matched (descending), then membership in
 * WhatsApp's curated popularity list, then canonical {@link WhatsAppEmoji#order()}.
 * The popularity list is loaded from {@code emoji_popular.bin} when present; absent
 * it, that tier is simply inert.
 */
final class EmojiSearch {
    /**
     * Caches the keyword index per resolved locale tag.
     */
    private static final Map<String, EmojiKeywordIndex> INDICES = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation.
     */
    private EmojiSearch() {
        throw new AssertionError();
    }

    /**
     * Holder for the available locale tags, loaded once from the index resource.
     */
    private static final class Tags {
        /**
         * The locale tags that have a keyword resource, in the order listed.
         */
        private static final List<String> AVAILABLE = load();

        /**
         * Prevents instantiation.
         */
        private Tags() {
            throw new AssertionError();
        }

        /**
         * Reads the available locale tags from {@code emoji_keywords.index}.
         *
         * @return the available tags, or an empty list when the resource is absent
         * @throws UncheckedIOException if the resource cannot be read
         */
        private static List<String> load() {
            try (var in = EmojiResources.class.getResourceAsStream("emoji_keywords.index")) {
                if (in == null) {
                    return List.of();
                }
                var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                List<String> tags = new ArrayList<>();
                for (var line : text.split("\n")) {
                    var tag = line.strip();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                }
                return Collections.unmodifiableList(tags);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Holder for the popularity ranks, loaded once from the optional resource.
     */
    private static final class Popular {
        /**
         * Maps a popular emoji to its rank, lower being more popular.
         */
        private static final Map<WhatsAppEmoji, Integer> RANK = load();

        /**
         * Prevents instantiation.
         */
        private Popular() {
            throw new AssertionError();
        }

        /**
         * Reads the curated popularity list from {@code emoji_popular.bin}.
         *
         * @return the emoji-to-rank map, empty when the resource is absent
         * @throws UncheckedIOException if the resource cannot be read
         */
        private static Map<WhatsAppEmoji, Integer> load() {
            try (var in = EmojiResources.openOptional("emoji_popular.bin")) {
                if (in == null) {
                    return Map.of();
                }
                var count = in.readInt();
                Map<WhatsAppEmoji, Integer> ranks = HashMap.newHashMap(count);
                for (var i = 0; i < count; i++) {
                    WhatsAppEmoji.of(EmojiResources.readString(in))
                            .ifPresent(emoji -> ranks.putIfAbsent(emoji, ranks.size()));
                }
                return ranks;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Searches a locale's keyword dictionary and ranks the matches.
     *
     * @param query  the search text, may be {@code null}
     * @param locale the locale whose keyword dictionary to search
     * @return the matching emojis, most relevant first; empty when none match
     */
    static List<WhatsAppEmoji> search(String query, Locale locale) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        var index = indexFor(locale);
        if (index == null) {
            return List.of();
        }

        Map<WhatsAppEmoji, Integer> counts = new HashMap<>();
        Set<WhatsAppEmoji> wordMatches = new HashSet<>();
        for (var word : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            wordMatches.clear();
            index.collectPrefix(word, wordMatches);
            for (var emoji : wordMatches) {
                counts.merge(emoji, 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return List.of();
        }

        List<WhatsAppEmoji> ranked = new ArrayList<>(counts.keySet());
        ranked.sort(Comparator
                .comparingInt((WhatsAppEmoji emoji) -> counts.get(emoji)).reversed()
                .thenComparingInt(EmojiSearch::popularRank)
                .thenComparingInt(WhatsAppEmoji::order));
        return Collections.unmodifiableList(ranked);
    }

    /**
     * Returns the keyword index for a locale, loading and caching it on first use.
     *
     * @param locale the requested locale
     * @return the keyword index, or {@code null} when no dictionary is available
     */
    private static EmojiKeywordIndex indexFor(Locale locale) {
        var tag = resolveTag(locale);
        return tag == null ? null : INDICES.computeIfAbsent(tag, EmojiKeywordIndex::load);
    }

    /**
     * Resolves a locale to an available keyword tag, preferring the exact
     * language-country tag, then the language, then any tag for the language, then
     * English.
     *
     * @param locale the requested locale
     * @return the resolved tag, or {@code null} when no dictionary is available
     */
    private static String resolveTag(Locale locale) {
        var available = Tags.AVAILABLE;
        if (available.isEmpty()) {
            return null;
        }
        var language = locale.getLanguage();
        var country = locale.getCountry();
        if (!country.isEmpty()) {
            var exact = language + "_" + country;
            if (available.contains(exact)) {
                return exact;
            }
        }
        if (available.contains(language)) {
            return language;
        }
        var prefix = language + "_";
        for (var tag : available) {
            if (tag.startsWith(prefix)) {
                return tag;
            }
        }
        return available.contains("en") ? "en" : available.get(0);
    }

    /**
     * Returns the popularity rank of an emoji, or {@link Integer#MAX_VALUE} when it
     * is not in the curated list.
     *
     * @param emoji the emoji to rank
     * @return the popularity rank, lower being more popular
     */
    private static int popularRank(WhatsAppEmoji emoji) {
        return Popular.RANK.getOrDefault(emoji, Integer.MAX_VALUE);
    }
}
