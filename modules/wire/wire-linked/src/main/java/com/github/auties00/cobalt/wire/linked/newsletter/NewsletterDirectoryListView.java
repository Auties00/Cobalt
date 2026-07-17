package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the filter pills available on the WhatsApp Channels explore
 * tab.
 *
 * <p>The explore tab displays a horizontal row of pills ("Recommended",
 * "New", "Popular", "Featured", "Trending") that re-rank the directory
 * listing according to different criteria. Each pill maps to a fixed
 * upper-case wire token that the client sends as a GraphQL variable when
 * fetching the directory page; the relay then returns the channels
 * ordered by the requested view.
 *
 * <p>Each constant carries both a stable protobuf wire index and the
 * upper-case wire token; the {@link #UNKNOWN} sentinel covers values the
 * client does not yet recognise without disturbing the existing indices.
 */
@ProtobufEnum
public enum NewsletterDirectoryListView {
    /**
     * Defensive sentinel for an unrecognised or absent view value.
     */
    UNKNOWN(0, ""),

    /**
     * The "Recommended" directory pill, ranking channels by personalised
     * relevance for the current user.
     */
    RECOMMENDED(1, "RECOMMENDED"),

    /**
     * The "New" directory pill, ranking channels by recency of creation.
     */
    NEW(2, "NEW"),

    /**
     * The "Popular" directory pill, ranking channels by overall follower
     * volume.
     */
    POPULAR(3, "POPULAR"),

    /**
     * The "Featured" directory pill, displaying an editorially curated
     * channel selection.
     */
    FEATURED(4, "FEATURED"),

    /**
     * The "Trending" directory pill, ranking channels by short-term
     * engagement velocity.
     */
    TRENDING(5, "TRENDING");

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * The upper-case wire string the relay expects on the GraphQL
     * variables payload.
     */
    final String value;

    /**
     * Constructs a constant with the supplied protobuf index and wire
     * value.
     *
     * @param index the protobuf wire index
     * @param value the upper-case wire string
     */
    NewsletterDirectoryListView(@ProtobufEnumIndex int index, String value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Returns the upper-case wire string for this view.
     *
     * @return the wire value; never {@code null}; empty for
     *         {@link #UNKNOWN}
     */
    public String value() {
        return value;
    }
}
