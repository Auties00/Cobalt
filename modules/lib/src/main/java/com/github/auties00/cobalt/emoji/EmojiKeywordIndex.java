package com.github.auties00.cobalt.emoji;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * A single locale's keyword search index: a sorted keyword array with a parallel
 * array of the emojis each keyword suggests, supporting prefix queries by binary
 * search.
 *
 * <p>This deliberately uses two flat, sorted arrays rather than a node-per-character
 * trie: it holds no per-node objects, so it is far cheaper in memory, and a prefix
 * query is a binary search plus a short linear scan over the matching range, which
 * gives the same results WhatsApp's trie does for the short queries an emoji search
 * issues. Keywords are lower-cased with {@link Locale#ROOT} at build time so a query
 * lower-cased the same way matches consistently.
 */
final class EmojiKeywordIndex {
    /**
     * The keywords, sorted ascending.
     */
    private final String[] keywords;

    /**
     * The emojis suggested by each keyword, parallel to {@link #keywords}.
     */
    private final WhatsAppEmoji[][] emojis;

    /**
     * Constructs an index over the given parallel arrays.
     *
     * @param keywords the sorted keywords
     * @param emojis   the emojis per keyword, parallel to {@code keywords}
     */
    private EmojiKeywordIndex(String[] keywords, WhatsAppEmoji[][] emojis) {
        this.keywords = keywords;
        this.emojis = emojis;
    }

    /**
     * Loads and builds the index for a locale tag from its keyword resource.
     *
     * <p>Each keyword's emoji values are resolved to their base constants through
     * {@link WhatsAppEmoji#of(String)} and de-duplicated; unresolvable values are
     * skipped.
     *
     * @param tag the locale tag, e.g. {@code "en"} or {@code "zh_TW"}
     * @return the built index
     * @throws UncheckedIOException if the keyword resource cannot be read
     */
    static EmojiKeywordIndex load(String tag) {
        record Entry(String keyword, WhatsAppEmoji[] emojis) {
        }
        try (var in = EmojiResources.open("emoji_keywords_" + tag + ".bin")) {
            var count = in.readInt();
            List<Entry> entries = new ArrayList<>(count);
            for (var i = 0; i < count; i++) {
                var keyword = EmojiResources.readString(in).toLowerCase(Locale.ROOT);
                var values = in.readInt();
                Set<WhatsAppEmoji> resolved = new LinkedHashSet<>();
                for (var j = 0; j < values; j++) {
                    WhatsAppEmoji.of(EmojiResources.readString(in)).ifPresent(resolved::add);
                }
                if (!resolved.isEmpty()) {
                    entries.add(new Entry(keyword, resolved.toArray(new WhatsAppEmoji[0])));
                }
            }
            entries.sort(Comparator.comparing(Entry::keyword));
            var keywords = new String[entries.size()];
            var emojis = new WhatsAppEmoji[entries.size()][];
            for (var i = 0; i < entries.size(); i++) {
                keywords[i] = entries.get(i).keyword();
                emojis[i] = entries.get(i).emojis();
            }
            return new EmojiKeywordIndex(keywords, emojis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Adds every emoji of every keyword that starts with the given prefix to the
     * collection.
     *
     * @param prefix the lower-cased query word
     * @param out    the collection receiving the matched emojis
     */
    void collectPrefix(String prefix, Collection<WhatsAppEmoji> out) {
        if (prefix.isEmpty()) {
            return;
        }
        for (var i = lowerBound(prefix); i < keywords.length && keywords[i].startsWith(prefix); i++) {
            Collections.addAll(out, emojis[i]);
        }
    }

    /**
     * Returns the index of the first keyword that is not less than the prefix.
     *
     * @param prefix the prefix to locate
     * @return the lower-bound index into {@link #keywords}
     */
    private int lowerBound(String prefix) {
        var low = 0;
        var high = keywords.length;
        while (low < high) {
            var mid = (low + high) >>> 1;
            if (keywords[mid].compareTo(prefix) < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
