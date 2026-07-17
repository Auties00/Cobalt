package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * Per-category disallow-list of contacts that cannot see a given piece of
 * the local user's profile.
 *
 * <p>WhatsApp lets the user override the global "everyone / my contacts /
 * nobody" setting for a privacy category (last-seen, profile photo,
 * about/status, online presence, read receipts) on a per-contact basis. The
 * server stores one disallow-list per category; this DTO carries one such
 * list along with the {@code dhash} the server uses to invalidate the
 * client's local cache.
 *
 * <p>Two response shapes share this type. When the client's cached
 * {@code dhash} matches the server view, the response is a "match" with no
 * payload and {@link #isMatch()} returns {@code true}; otherwise the server
 * returns the full user list plus a fresh {@code dhash}, which the client
 * uses to bulk-replace its local copy.
 */
@ProtobufMessage
public final class PrivacyDisallowedList {
    /**
     * Match discriminator. {@code true} when the local cached {@code dhash}
     * matched the server's view (the server omitted the {@code <list>}
     * child); {@code false} when the caches disagreed and the server
     * returned a fresh user list.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean match;

    /**
     * The disallow-list users carried by the "mismatch" response shape;
     * empty for the "match" response shape.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<Jid> users;

    /**
     * The server-side {@code dhash} used to invalidate the client's local
     * cache. Carried only by the "mismatch" response shape; {@code null}
     * for the "match" response shape.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String dhash;

    /**
     * Constructs a new {@code PrivacyDisallowedList} with the supplied
     * discriminator, user list and dhash.
     *
     * @param match the match discriminator
     * @param users the disallow-list users; {@code null} is treated as an
     *              empty list
     * @param dhash the server-side dhash, or {@code null} for the "match"
     *              response shape
     */
    PrivacyDisallowedList(boolean match, List<Jid> users, String dhash) {
        this.match = match;
        this.users = users == null ? List.of() : users;
        this.dhash = dhash;
    }

    /**
     * Returns whether the local cache matched the server's view.
     *
     * @return {@code true} for the "match" response shape, {@code false}
     *         for the "mismatch" response shape
     */
    public boolean isMatch() {
        return match;
    }

    /**
     * Returns the disallow-list users carried by the "mismatch" response
     * shape.
     *
     * @return an unmodifiable list of users; never {@code null}, possibly
     *         empty
     */
    public List<Jid> users() {
        return users;
    }

    /**
     * Returns the server-side {@code dhash} used to invalidate the local
     * cache.
     *
     * @return an {@code Optional} containing the dhash, or empty for the
     *         "match" response shape
     */
    public Optional<String> dhash() {
        return Optional.ofNullable(dhash);
    }

    /**
     * Sets the match discriminator.
     *
     * @param match {@code true} for the "match" response shape, {@code false}
     *              for the "mismatch" response shape
     */
    public void setMatch(boolean match) {
        this.match = match;
    }

    /**
     * Sets the disallow-list users.
     *
     * @param users the users to set; {@code null} is treated as an empty
     *              list
     */
    public void setUsers(List<Jid> users) {
        this.users = users == null ? List.of() : users;
    }

    /**
     * Sets the server-side {@code dhash}.
     *
     * @param dhash the dhash to set, or {@code null} to clear
     */
    public void setDhash(String dhash) {
        this.dhash = dhash;
    }
}
