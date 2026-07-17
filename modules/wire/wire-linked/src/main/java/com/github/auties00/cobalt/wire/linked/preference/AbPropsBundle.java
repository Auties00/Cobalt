package com.github.auties00.cobalt.wire.linked.preference;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries a parsed A/B-experiment configuration bundle returned by the
 * relay's {@code experiment-config} fetch.
 *
 * <p>Exposes the relay-echoed delta-update identifiers ({@code hash},
 * {@code refresh}, {@code refreshId}, {@code abKey}) alongside a typed
 * {@code config_code -> config_value} map of synced experiment values.
 * Group-scoped fetches additionally surface the target group JID so the
 * caller can route the bundle to the right slot.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class AbPropsBundle {
    /**
     * The optional target group JID. Present only on group-scoped
     * fetches; {@code null} for the global props bundle.
     */
    private final Jid groupJid;

    /**
     * The relay-returned content hash. Echoed back to the relay on the
     * next refresh to enable the delta-update fast path.
     */
    private final String hash;

    /**
     * The relay-returned refresh-cooldown hint, in seconds. Absent when
     * the relay omits it.
     */
    private final Integer refresh;

    /**
     * The relay-returned refresh id. Echoed back to the relay on the
     * next refresh.
     */
    private final Integer refreshId;

    /**
     * The relay-returned A/B framework key, when supplied.
     */
    private final String abKey;

    /**
     * The synced experiment values, keyed by their numeric
     * {@code config_code}.
     */
    private final Map<Integer, String> experiments;

    /**
     * Constructs a new bundle.
     *
     * @param groupJid    the optional group JID; may be {@code null}
     * @param hash        the optional content hash; may be {@code null}
     * @param refresh     the optional refresh cooldown; may be
     *                    {@code null}
     * @param refreshId   the optional refresh id; may be {@code null}
     * @param abKey       the optional A/B framework key; may be
     *                    {@code null}
     * @param experiments the experiment map; never {@code null}
     * @throws NullPointerException if {@code experiments} is
     *                              {@code null}
     */
    public AbPropsBundle(Jid groupJid, String hash, Integer refresh, Integer refreshId,
                         String abKey, Map<Integer, String> experiments) {
        this.groupJid = groupJid;
        this.hash = hash;
        this.refresh = refresh;
        this.refreshId = refreshId;
        this.abKey = abKey;
        Objects.requireNonNull(experiments, "experiments cannot be null");
        this.experiments = Collections.unmodifiableMap(new LinkedHashMap<>(experiments));
    }

    /**
     * Returns the optional target group JID.
     *
     * @return an {@link Optional} carrying the group JID, or empty for
     *         the global bundle
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the content hash.
     *
     * @return an {@link Optional} carrying the hash, or empty
     */
    public Optional<String> hash() {
        return Optional.ofNullable(hash);
    }

    /**
     * Returns the refresh cooldown, in seconds.
     *
     * @return an {@link Optional} carrying the cooldown, or empty
     */
    public Optional<Integer> refresh() {
        return Optional.ofNullable(refresh);
    }

    /**
     * Returns the refresh id.
     *
     * @return an {@link Optional} carrying the refresh id, or empty
     */
    public Optional<Integer> refreshId() {
        return Optional.ofNullable(refreshId);
    }

    /**
     * Returns the A/B framework key.
     *
     * @return an {@link Optional} carrying the ab-key, or empty
     */
    public Optional<String> abKey() {
        return Optional.ofNullable(abKey);
    }

    /**
     * Returns an unmodifiable snapshot of the synced experiment values
     * keyed by their numeric {@code config_code}.
     *
     * @return the experiment map; never {@code null}
     */
    public Map<Integer, String> experiments() {
        return experiments;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (AbPropsBundle) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.hash, that.hash)
                && Objects.equals(this.refresh, that.refresh)
                && Objects.equals(this.refreshId, that.refreshId)
                && Objects.equals(this.abKey, that.abKey)
                && Objects.equals(this.experiments, that.experiments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupJid, hash, refresh, refreshId, abKey, experiments);
    }

    @Override
    public String toString() {
        return "AbPropsBundle[groupJid=" + groupJid
                + ", hash=" + hash
                + ", refresh=" + refresh
                + ", refreshId=" + refreshId
                + ", abKey=" + abKey
                + ", experiments=" + experiments + ']';
    }
}
