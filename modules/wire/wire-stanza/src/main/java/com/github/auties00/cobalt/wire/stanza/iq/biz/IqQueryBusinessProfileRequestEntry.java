package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import java.util.Objects;
import java.util.Optional;

/**
 * Models one {@code (businessJid, tag)} pair fanned out into a {@code <profile/>} child of an {@link IqQueryBusinessProfileRequest}.
 *
 * <p>An entry names one merchant in a fan-out business-profile fetch and optionally attaches the
 * version tag the relay previously returned for that merchant. When the supplied tag matches the
 * cached version, the relay echoes a header-only acknowledgement instead of the full profile body,
 * so chat openers and the merchant directory can refresh many merchants in one round-trip.
 */
public final class IqQueryBusinessProfileRequestEntry {
    /**
     * Holds the merchant JID stamped into the {@code jid} attribute of the {@code <profile/>} child.
     */
    private final Jid businessJid;

    /**
     * Holds the optional version tag stamped into the {@code tag} attribute of the {@code <profile/>} child.
     */
    private final Integer tag;

    /**
     * Constructs an entry over the given merchant JID and optional version tag.
     *
     * <p>A non-{@code null} tag enables the relay's conditional-fetch path; a {@code null} tag
     * forces the full profile body to be returned.
     *
     * @param businessJid the merchant JID; never {@code null}
     * @param tag         the optional version tag; may be {@code null}
     * @throws NullPointerException if {@code businessJid} is {@code null}
     */
    public IqQueryBusinessProfileRequestEntry(Jid businessJid, Integer tag) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.tag = tag;
    }

    /**
     * Returns the merchant JID the fan-out names.
     *
     * <p>The relay routes it verbatim into the {@code jid} attribute of the resulting
     * {@code <profile/>} child.
     *
     * @return the merchant JID; never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the cached version tag the entry carries.
     *
     * <p>An empty {@link Optional} means the request forces the relay to return the full profile
     * body for this merchant.
     *
     * @return an {@link Optional} carrying the tag
     */
    public Optional<Integer> tag() {
        return Optional.ofNullable(tag);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryBusinessProfileRequestEntry) obj;
        return Objects.equals(this.businessJid, that.businessJid)
                && Objects.equals(this.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid, tag);
    }

    @Override
    public String toString() {
        return "IqQueryBusinessProfileRequestEntry[businessJid=" + businessJid
                + ", tag=" + tag + ']';
    }
}
