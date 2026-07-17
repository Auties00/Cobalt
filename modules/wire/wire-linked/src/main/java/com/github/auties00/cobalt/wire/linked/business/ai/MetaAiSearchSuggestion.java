package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One suggested-query tile shown in the empty state of Meta AI search.
 *
 * <p>Before the user types anything into the AI search box, WhatsApp shows a
 * set of suggested queries the user can tap to get started. Each tile carries
 * the {@linkplain #suggestion() copy} the user sees, the
 * {@linkplain #query() query} that is actually run when the tile is tapped,
 * and the {@linkplain #sessionId() session} the suggestion was generated for.
 *
 * <p>This model is one such suggestion tile as the server reports it.
 */
@ProtobufMessage(name = "MetaAiSearchSuggestion")
public final class MetaAiSearchSuggestion {
    /**
     * Displayed copy of the suggestion tile. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String suggestion;

    /**
     * Query run when the tile is tapped. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String query;

    /**
     * Identifier of the search session the suggestion was generated for.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String sessionId;

    /**
     * Constructs a new {@code MetaAiSearchSuggestion}. Any reference argument
     * may be {@code null} when the server omitted the corresponding field.
     *
     * @param suggestion the displayed suggestion copy, or {@code null}
     * @param query      the query run when tapped, or {@code null}
     * @param sessionId  the originating session id, or {@code null}
     */
    MetaAiSearchSuggestion(String suggestion, String query, String sessionId) {
        this.suggestion = suggestion;
        this.query = query;
        this.sessionId = sessionId;
    }

    /**
     * Returns the displayed copy of the suggestion tile.
     *
     * @return the suggestion copy, or empty when the server omitted it
     */
    public Optional<String> suggestion() {
        return Optional.ofNullable(suggestion);
    }

    /**
     * Returns the query run when the tile is tapped.
     *
     * @return the query, or empty when the server omitted it
     */
    public Optional<String> query() {
        return Optional.ofNullable(query);
    }

    /**
     * Returns the identifier of the search session the suggestion was generated
     * for.
     *
     * @return the session id, or empty when the server omitted it
     */
    public Optional<String> sessionId() {
        return Optional.ofNullable(sessionId);
    }
}
