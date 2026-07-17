package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the Meta AI search type-ahead suggestion query.
 *
 * <p>While the user types into the Meta AI search box the WhatsApp client
 * fetches ranked suggestion tiles for the partial prefix. This input
 * carries every parameter the suggestion ranker consumes.
 *
 * <p>{@link #query()} is the partial search prefix as typed.
 * {@link #locale()} is the requesting locale the ranker considers when
 * picking suggestion copy. {@link #experimentConfig()} lists the
 * server-side experiment identifiers the WhatsApp client opts into; the
 * ranker may serve different suggestion sets depending on which
 * experiments are active. {@link #capabilities()} enumerates the
 * client-feature capability tokens the server should consider when
 * ranking, so that suggestion tiles whose rendering the client cannot
 * support are skipped.
 */
@ProtobufMessage(name = "MetaAiSearchTypeAheadQuery")
public final class MetaAiSearchTypeAheadQuery {
    /**
     * Partial search prefix as typed. Unset omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String query;

    /**
     * Requesting locale the ranker considers when picking suggestion
     * copy. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String locale;

    /**
     * Server-side experiment identifiers the WhatsApp client opts into.
     * Defaults to {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final List<Integer> experimentConfig;

    /**
     * Client-feature capability tokens the server should consider when
     * ranking, so that suggestion tiles whose rendering the client cannot
     * support are skipped. Defaults to {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> capabilities;

    /**
     * Constructs a new {@code MetaAiSearchTypeAheadQuery}. The list
     * arguments may be {@code null} to default to {@link List#of()}; the
     * scalar arguments may be {@code null} to omit the corresponding
     * variable from the request.
     *
     * @param query            the partial search prefix, or {@code null}
     * @param locale           the requesting locale, or {@code null}
     * @param experimentConfig the server-side experiment identifiers, or
     *                         {@code null} to default to empty
     * @param capabilities     the client-feature capability tokens, or
     *                         {@code null} to default to empty
     */
    public MetaAiSearchTypeAheadQuery(String query, String locale, List<Integer> experimentConfig,
                                      List<String> capabilities) {
        this.query = query;
        this.locale = locale;
        this.experimentConfig = experimentConfig == null ? List.of() : List.copyOf(experimentConfig);
        this.capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }

    /**
     * Returns the partial search prefix.
     *
     * @return an {@link Optional} carrying the prefix, or empty when unset
     */
    public Optional<String> query() {
        return Optional.ofNullable(query);
    }

    /**
     * Returns the requesting locale.
     *
     * @return an {@link Optional} carrying the locale, or empty when unset
     */
    public Optional<String> locale() {
        return Optional.ofNullable(locale);
    }

    /**
     * Returns the server-side experiment identifiers.
     *
     * @return an unmodifiable view of the identifiers; never {@code null},
     *         possibly empty
     */
    public List<Integer> experimentConfig() {
        return experimentConfig;
    }

    /**
     * Returns the client-feature capability tokens.
     *
     * @return an unmodifiable view of the tokens; never {@code null},
     *         possibly empty
     */
    public List<String> capabilities() {
        return capabilities;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MetaAiSearchTypeAheadQuery) obj;
        return Objects.equals(query, that.query)
                && Objects.equals(locale, that.locale)
                && Objects.equals(experimentConfig, that.experimentConfig)
                && Objects.equals(capabilities, that.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, locale, experimentConfig, capabilities);
    }

    @Override
    public String toString() {
        return "MetaAiSearchTypeAheadQuery[" +
                "query=" + query + ", " +
                "locale=" + locale + ", " +
                "experimentConfig=" + experimentConfig + ", " +
                "capabilities=" + capabilities + ']';
    }
}
