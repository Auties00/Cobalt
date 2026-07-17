package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregates the per-user result for a single {@code <user>} entry inside a USync response.
 *
 * <p>One instance is produced for every entry the relay returns in the {@code <list>} child of a
 * USync IQ. The peer's identifiers are exposed verbatim: {@link #id()} carries the {@code jid}
 * attribute and {@link #phoneJid()} carries the {@code pn_jid} attribute. Per-protocol payloads
 * are looked up by name with {@link #getProtocolResult(UsyncProtocol)} or
 * {@link #getProtocolResult(String)}, and presence is probed with
 * {@link #hasProtocolResult(UsyncProtocol)}. The backing map is never exposed directly: each value
 * is typed as {@link UsyncProtocolResult}, which forces the caller to pattern-match the
 * {@link UsyncProtocolResponse} success branch against the {@link UsyncProtocolError} branch.
 *
 * @implNote
 * This implementation defensively copies the protocol map into an unmodifiable
 * {@link Map#copyOf(Map)} snapshot so callers cannot mutate shared state, preserving the
 * per-entry immutability of the fresh object the WA Web counterpart returns.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public final class UsyncUserResult {
    /**
     * Holds the peer's canonical JID echoed in the {@code jid} attribute.
     *
     * <p>Is {@code null} when the relay omitted the attribute.
     */
    private final Jid id;

    /**
     * Holds the peer's phone-number JID echoed in the {@code pn_jid} attribute.
     *
     * <p>Is {@code null} when the relay omitted the attribute.
     */
    private final Jid phoneJid;

    /**
     * Holds the per-protocol results keyed by protocol wire name.
     *
     * <p>Keys are the literal tag names the relay stamps on each child element (for example
     * {@code "contact"} or {@code "devices"}); values are the success or error variant the matching
     * parser produced. The map is an unmodifiable snapshot.
     */
    private final Map<String, UsyncProtocolResult> protocolResults;

    /**
     * Constructs a per-user result from the identifiers and protocol map of one {@code <user>}
     * child.
     *
     * <p>A {@code null} {@code protocolResults} is normalised to an empty map; a non-null map is
     * copied into an unmodifiable snapshot.
     *
     * @param id              the peer's canonical {@link Jid}, or {@code null} when the {@code jid}
     *                        attribute is absent
     * @param phoneJid        the peer's phone-number {@link Jid}, or {@code null} when the
     *                        {@code pn_jid} attribute is absent
     * @param protocolResults the per-protocol map keyed by wire name, or {@code null} for an empty
     *                        result set
     */
    public UsyncUserResult(Jid id, Jid phoneJid, Map<String, UsyncProtocolResult> protocolResults) {
        this.id = id;
        this.phoneJid = phoneJid;
        this.protocolResults = protocolResults == null ? Map.of() : Map.copyOf(protocolResults);
    }

    /**
     * Returns the peer's canonical {@link Jid}, when present.
     *
     * <p>This is the raw value the relay echoed in the {@code jid} attribute of the {@code <user>}
     * child, not normalised in any way, so callers see the exact identifier the relay returned.
     *
     * @return the canonical {@link Jid}, or empty when absent
     */
    public Optional<Jid> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the peer's phone-number {@link Jid}, when present.
     *
     * <p>This is the {@code pn_jid} attribute of the {@code <user>} child, present when the request
     * specified a phone-number addressing mode and the relay echoes the resolved phone-number JID
     * alongside the canonical id.
     *
     * @return the phone-number {@link Jid}, or empty when absent
     */
    public Optional<Jid> phoneJid() {
        return Optional.ofNullable(phoneJid);
    }

    /**
     * Returns the parse result for the specified protocol, when present.
     *
     * <p>Delegates to {@link #getProtocolResult(String)} with {@link UsyncProtocol#name()};
     * preferred at call sites that already hold a typed protocol descriptor.
     *
     * @param protocol the protocol descriptor
     * @return the parse result, or empty when this user entry has no payload for the requested
     *         protocol
     * @throws NullPointerException if {@code protocol} is {@code null}
     */
    public Optional<UsyncProtocolResult> getProtocolResult(UsyncProtocol protocol) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        return getProtocolResult(protocol.name());
    }

    /**
     * Returns the parse result for the named protocol, when present.
     *
     * <p>The lookup key is the wire name the relay tags each child element with (for example
     * {@code "contact"}, {@code "devices"}, or {@code "picture"}). The returned value is either a
     * success {@link UsyncProtocolResponse} variant or an {@link UsyncProtocolError}; callers
     * pattern-match on {@link UsyncProtocolResult} to discriminate the two.
     *
     * @param protocolName the protocol's wire name
     * @return the parse result, or empty when this user entry has no payload for the named protocol
     */
    public Optional<UsyncProtocolResult> getProtocolResult(String protocolName) {
        return Optional.ofNullable(protocolResults.get(protocolName));
    }

    /**
     * Returns whether the relay returned a parse result for the specified protocol on this user
     * entry.
     *
     * <p>Cheaper than {@link #getProtocolResult(UsyncProtocol)} when only presence matters; lets
     * callers skip protocols the relay omitted, typically because the peer's privacy settings hid
     * the value.
     *
     * @param protocol the protocol descriptor
     * @return {@code true} when a per-protocol entry exists
     * @throws NullPointerException if {@code protocol} is {@code null}
     */
    public boolean hasProtocolResult(UsyncProtocol protocol) {
        Objects.requireNonNull(protocol, "protocol cannot be null");
        return protocolResults.containsKey(protocol.name());
    }
}
