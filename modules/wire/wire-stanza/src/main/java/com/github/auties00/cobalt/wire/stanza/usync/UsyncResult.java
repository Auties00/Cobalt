package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolError;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncUserResult;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregated result of a {@link UsyncQuery}.
 *
 * <p>Returned by {@link UsyncQuery#parseResponse(Stanza)}.
 * The relay structures its response in three layers: an optional top-level IQ
 * error (exposed via {@link #topLevelError()} and {@link #failed()}),
 * per-protocol metadata that applies to every user in the batch (exposed via
 * {@link #getProtocolError(UsyncProtocol)} and
 * {@link #getProtocolRefresh(UsyncProtocol)}), and a per-user, per-protocol
 * result list (exposed via {@link #users()}).
 *
 * @implNote
 * This implementation funnels the per-protocol maps through accessors so the
 * surface stays immutable and clients always go through a typed lookup.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public final class UsyncResult {
    /**
     * Per-user, per-protocol parse results in the order the relay returned
     * them.
     */
    private final List<UsyncUserResult> users;

    /**
     * Maps protocol wire name to the per-protocol error that applied to every
     * user in the batch.
     */
    private final Map<String, UsyncProtocolError> protocolErrors;

    /**
     * Maps protocol wire name to the {@code refresh} hint the relay attached to
     * the per-protocol result envelope.
     */
    private final Map<String, Duration> protocolRefreshes;

    /**
     * Top-level error populated when the IQ itself failed; {@code null}
     * otherwise.
     */
    private final UsyncTopLevelError topLevelError;

    /**
     * Builds a new aggregated result from the parsed sections.
     *
     * <p>Constructed by {@link UsyncQuery#parseResponse(Stanza)}.
     *
     * @implNote
     * This implementation defensively copies every collection so callers see an
     * immutable snapshot.
     *
     * @param users             the per-user results
     * @param protocolErrors    map from protocol wire name to error
     * @param protocolRefreshes map from protocol wire name to refresh window
     * @param topLevelError     top-level error, or {@code null} on success
     */
    public UsyncResult(
            List<UsyncUserResult> users,
            Map<String, UsyncProtocolError> protocolErrors,
            Map<String, Duration> protocolRefreshes,
            UsyncTopLevelError topLevelError) {
        this.users = users == null ? List.of() : List.copyOf(users);
        this.protocolErrors = protocolErrors == null ? Map.of() : Map.copyOf(protocolErrors);
        this.protocolRefreshes = protocolRefreshes == null ? Map.of() : Map.copyOf(protocolRefreshes);
        this.topLevelError = topLevelError;
    }

    /**
     * Returns the per-user results in relay order.
     *
     * <p>The returned list is unmodifiable; iterate or pattern-match the
     * per-protocol payloads inside each {@link UsyncUserResult}.
     *
     * @return the per-user results, never {@code null}
     */
    public List<UsyncUserResult> users() {
        return Collections.unmodifiableList(users);
    }

    /**
     * Returns the per-protocol error that applied to every user, when present.
     *
     * <p>Detects that the relay refused this protocol for the whole batch
     * before iterating {@link #users()}. The error may carry an
     * {@link UsyncProtocolError#errorBackoff()} hint that drives
     * {@link UsyncBackoff#setProtocolBackoffMs(String, long)}.
     *
     * @param protocol the protocol descriptor
     * @return the per-protocol error, or empty
     */
    public Optional<UsyncProtocolError> getProtocolError(UsyncProtocol protocol) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        return getProtocolError(protocol.name());
    }

    /**
     * Returns the per-protocol error for the named protocol, when present.
     *
     * <p>Takes a raw protocol wire name; the {@link #getProtocolError(UsyncProtocol)}
     * form is preferred when the descriptor is already in scope.
     *
     * @param protocolName the protocol wire name
     * @return the per-protocol error, or empty
     */
    public Optional<UsyncProtocolError> getProtocolError(String protocolName) {
        return Optional.ofNullable(protocolErrors.get(protocolName));
    }

    /**
     * Returns the {@code refresh} window the relay attached to a protocol, when
     * present.
     *
     * <p>The relay uses {@code refresh} to ask clients to re-query the protocol
     * after the supplied duration even though the current response succeeded.
     *
     * @param protocol the protocol descriptor
     * @return the refresh window, or empty
     */
    public Optional<Duration> getProtocolRefresh(UsyncProtocol protocol) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        return getProtocolRefresh(protocol.name());
    }

    /**
     * Returns the {@code refresh} window for the named protocol, when present.
     *
     * <p>Takes a raw protocol wire name; the {@link #getProtocolRefresh(UsyncProtocol)}
     * form is preferred when the descriptor is already in scope.
     *
     * @param protocolName the protocol wire name
     * @return the refresh window, or empty
     */
    public Optional<Duration> getProtocolRefresh(String protocolName) {
        return Optional.ofNullable(protocolRefreshes.get(protocolName));
    }

    /**
     * Returns the top-level IQ error envelope when the request failed
     * wholesale.
     *
     * <p>Populated when the IQ {@code type} attribute is not {@code "result"}.
     *
     * @return the top-level error, or empty
     */
    public Optional<UsyncTopLevelError> topLevelError() {
        return Optional.ofNullable(topLevelError);
    }

    /**
     * Returns whether the IQ failed wholesale.
     *
     * <p>Shortcut for {@link #topLevelError()} being present.
     *
     * @return {@code true} when the IQ returned an error envelope
     */
    public boolean failed() {
        return topLevelError != null;
    }
}
