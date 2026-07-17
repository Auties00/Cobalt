package com.github.auties00.cobalt.wire.linked.setting.notice;

import java.util.List;
import java.util.Objects;

/**
 * Carries the parsed bundle of pending user-notice disclosures
 * returned by the relay's {@code get_disclosures} fetch.
 *
 * <p>The bundle is a flat list of {@link UserNotice} entries. Each
 * entry surfaces a single Terms-of-Service / privacy-policy /
 * compliance modal the user has not yet acknowledged.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class UserNoticeBundle {
    /**
     * The disclosure entries returned by the relay.
     */
    private final List<UserNotice> notices;

    /**
     * Constructs a new bundle.
     *
     * @param notices the disclosure entries; never {@code null}
     * @throws NullPointerException if {@code notices} is {@code null}
     */
    public UserNoticeBundle(List<UserNotice> notices) {
        Objects.requireNonNull(notices, "notices cannot be null");
        this.notices = List.copyOf(notices);
    }

    /**
     * Returns the disclosure entries.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<UserNotice> notices() {
        return notices;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UserNoticeBundle) obj;
        return Objects.equals(this.notices, that.notices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notices);
    }

    @Override
    public String toString() {
        return "UserNoticeBundle[notices=" + notices + ']';
    }
}
