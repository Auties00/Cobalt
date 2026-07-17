package com.github.auties00.cobalt.wire.linked.setting;

import java.util.Objects;

/**
 * Carries the parsed acknowledgement of a successfully-submitted
 * "Contact us" support form.
 *
 * <p>The relay-side success reply surfaces three pieces of
 * information the caller's UI needs to render the post-submit
 * confirmation: the opaque ticket id used for follow-up, the routing
 * group JID assigned to the case, and the user-visible
 * acknowledgement message.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class SupportTicketAcknowledgement {
    /**
     * The opaque ticket id used for follow-up correspondence.
     */
    private final String ticketId;

    /**
     * The routing group JID assigned to the case.
     */
    private final String groupJid;

    /**
     * The user-visible acknowledgement message text.
     */
    private final String message;

    /**
     * Constructs a new acknowledgement.
     *
     * @param ticketId the ticket id; never {@code null}
     * @param groupJid the routing group JID; never {@code null}
     * @param message  the acknowledgement text; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SupportTicketAcknowledgement(String ticketId, String groupJid, String message) {
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId cannot be null");
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
    }

    /**
     * Returns the ticket id.
     *
     * @return the ticket id; never {@code null}
     */
    public String ticketId() {
        return ticketId;
    }

    /**
     * Returns the routing group JID.
     *
     * @return the JID; never {@code null}
     */
    public String groupJid() {
        return groupJid;
    }

    /**
     * Returns the acknowledgement message.
     *
     * @return the message; never {@code null}
     */
    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SupportTicketAcknowledgement) obj;
        return Objects.equals(this.ticketId, that.ticketId)
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, groupJid, message);
    }

    @Override
    public String toString() {
        return "SupportTicketAcknowledgement[ticketId=" + ticketId
                + ", groupJid=" + groupJid
                + ", message=" + message + ']';
    }
}
