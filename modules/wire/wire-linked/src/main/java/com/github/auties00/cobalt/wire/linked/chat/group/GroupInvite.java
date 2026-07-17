package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;

/**
 * Input model for {@code LinkedWhatsAppClient.queryGroupInfoByInvite} — a v4
 * in-band invite payload addressed by the
 * {@link com.github.auties00.cobalt.wire.linked.message.group.GroupInviteMessage GroupInviteMessage}.
 *
 * <p>All four fields are required by the
 * {@code WAWebGroupInviteV4Job.queryGroupInviteV4} request.
 */
@ProtobufMessage
public final class GroupInvite {
    /**
     * JID of the user that received the invite (the local user).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid invitee;

    /**
     * JID of the group administrator that issued the invite.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid sender;

    /**
     * Expiration time of the invite.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant expiration;

    /**
     * Invite code from the {@code GroupInviteMessage}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String inviteCode;

    /**
     * Constructs a new {@code GroupInvite}.
     *
     * @param invitee    the invitee JID; required
     * @param sender     the issuing admin JID; required
     * @param expiration the invite expiration; required
     * @param inviteCode the invite code; required
     * @throws NullPointerException if any argument is {@code null}
     */
    GroupInvite(Jid invitee, Jid sender, Instant expiration, String inviteCode) {
        this.invitee = Objects.requireNonNull(invitee, "invitee cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.expiration = Objects.requireNonNull(expiration, "expiration cannot be null");
        this.inviteCode = Objects.requireNonNull(inviteCode, "inviteCode cannot be null");
    }

    /**
     * Returns the invitee JID.
     *
     * @return the invitee JID, never {@code null}
     */
    public Jid invitee() {
        return invitee;
    }

    /**
     * Returns the issuing admin's JID.
     *
     * @return the sender JID, never {@code null}
     */
    public Jid sender() {
        return sender;
    }

    /**
     * Returns the invite expiration.
     *
     * @return the expiration, never {@code null}
     */
    public Instant expiration() {
        return expiration;
    }

    /**
     * Returns the invite code.
     *
     * @return the invite code, never {@code null}
     */
    public String inviteCode() {
        return inviteCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupInvite) obj;
        return Objects.equals(invitee, that.invitee) &&
                Objects.equals(sender, that.sender) &&
                Objects.equals(expiration, that.expiration) &&
                Objects.equals(inviteCode, that.inviteCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invitee, sender, expiration, inviteCode);
    }

    @Override
    public String toString() {
        return "GroupInvite[" +
                "invitee=" + invitee + ", " +
                "sender=" + sender + ", " +
                "expiration=" + expiration + ", " +
                "inviteCode=" + inviteCode + ']';
    }
}
