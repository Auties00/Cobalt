package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Input model for {@code LinkedWhatsAppClient.acceptGroupAdd} — the
 * parameters carried on an {@code <add_request>} acceptance.
 *
 * <p>All four fields are required — the relay rejects the acceptance
 * if any wire attribute is missing.
 */
@ProtobufMessage
public final class GroupAddAccept {
    /**
     * JID of the group the invitee is being added to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid group;

    /**
     * Acceptance code that authorises the add request.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String acceptCode;

    /**
     * Expiration timestamp of the acceptance window, in seconds.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long acceptExpiration;

    /**
     * JID of the admin that issued the original add request.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid acceptAdmin;

    /**
     * Constructs a new {@code GroupAddAccept}.
     *
     * @param group            the group JID; required
     * @param acceptCode       the acceptance code; required
     * @param acceptExpiration the acceptance expiration in seconds
     * @param acceptAdmin      the issuing admin's JID; required
     * @throws NullPointerException if any reference argument is {@code null}
     */
    GroupAddAccept(Jid group, String acceptCode, long acceptExpiration, Jid acceptAdmin) {
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.acceptCode = Objects.requireNonNull(acceptCode, "acceptCode cannot be null");
        this.acceptExpiration = acceptExpiration;
        this.acceptAdmin = Objects.requireNonNull(acceptAdmin, "acceptAdmin cannot be null");
    }

    /**
     * Returns the group JID.
     *
     * @return the group JID, never {@code null}
     */
    public Jid group() {
        return group;
    }

    /**
     * Returns the acceptance code.
     *
     * @return the code, never {@code null}
     */
    public String acceptCode() {
        return acceptCode;
    }

    /**
     * Returns the acceptance expiration timestamp.
     *
     * @return the expiration in seconds
     */
    public long acceptExpiration() {
        return acceptExpiration;
    }

    /**
     * Returns the JID of the admin that issued the add request.
     *
     * @return the admin JID, never {@code null}
     */
    public Jid acceptAdmin() {
        return acceptAdmin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupAddAccept) obj;
        return Objects.equals(group, that.group) &&
                Objects.equals(acceptCode, that.acceptCode) &&
                acceptExpiration == that.acceptExpiration &&
                Objects.equals(acceptAdmin, that.acceptAdmin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, acceptCode, acceptExpiration, acceptAdmin);
    }

    @Override
    public String toString() {
        return "GroupAddAccept[" +
                "group=" + group + ", " +
                "acceptCode=" + acceptCode + ", " +
                "acceptExpiration=" + acceptExpiration + ", " +
                "acceptAdmin=" + acceptAdmin + ']';
    }
}
