package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single pending membership-approval request that a group
 * administrator must accept or reject before the requesting user becomes a
 * full participant.
 *
 * <p>WhatsApp groups configured with the
 * {@code membership_approval_mode} flag enabled enqueue inbound add /
 * invite-link / community sub-group join attempts as approval requests
 * rather than admitting the user directly. Each request captures the
 * requesting user's identity, the optional resolved phone-number JID and
 * username (when the relay performed LID-to-PN resolution), the parent
 * community JID for community-link requests, the request timestamp, and
 * the discriminator that records how the request was filed.
 *
 * <p>The {@link Method} discriminator distinguishes the three pathways
 * tracked by the relay:
 * <ul>
 *   <li>{@link Method#INVITE_LINK} — the user followed an invite link;</li>
 *   <li>{@link Method#LINKED_GROUP_JOIN} — the user joined via the
 *   parent-community linked-group flow;</li>
 *   <li>{@link Method#NON_ADMIN_ADD} — a non-admin member attempted to add
 *   the user directly.</li>
 * </ul>
 *
 * <p>Equality and hashing reflect the requesting user's primary JID since
 * a group can only carry one outstanding request per requester at a time.
 */
@ProtobufMessage(name = "GroupMembershipApprovalRequest")
public final class GroupMembershipApprovalRequest {
    /**
     * The requesting user's primary JID. Required, never {@code null} after
     * construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid requestingJid;

    /**
     * The optional resolved requestor JID, surfaced when the request
     * originates from a different identity than the primary requester
     * (typical for community sub-group join requests).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid requestor;

    /**
     * The optional resolved phone-number JID surfaced when the relay
     * performed LID-to-PN mapping for the requestor.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid requestorPhoneNumber;

    /**
     * The optional resolved username for the requestor when the relay
     * surfaced one.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String requestorUsername;

    /**
     * The optional parent-community JID surfaced for community-link join
     * requests.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid parentGroupJid;

    /**
     * The instant at which the request was filed. Required, never
     * {@code null} after construction.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant requestTimestamp;

    /**
     * The optional pathway through which the request was filed.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    Method method;

    /**
     * Constructs a new approval-request entry.
     *
     * @param requestingJid        the requesting user's JID; must not be
     *                             {@code null}
     * @param requestor            optional resolved requestor JID; may be
     *                             {@code null}
     * @param requestorPhoneNumber optional resolved phone-number JID; may
     *                             be {@code null}
     * @param requestorUsername    optional resolved username; may be
     *                             {@code null}
     * @param parentGroupJid       optional parent-community JID; may be
     *                             {@code null}
     * @param requestTimestamp     the timestamp the request was filed at;
     *                             must not be {@code null}
     * @param method               the optional request-pathway
     *                             discriminator; may be {@code null} when
     *                             the relay omitted it
     * @throws NullPointerException if {@code requestingJid} or
     *                              {@code requestTimestamp} is
     *                              {@code null}
     */
    GroupMembershipApprovalRequest(Jid requestingJid, Jid requestor,
                                   Jid requestorPhoneNumber, String requestorUsername,
                                   Jid parentGroupJid, Instant requestTimestamp,
                                   Method method) {
        this.requestingJid = Objects.requireNonNull(requestingJid, "requestingJid cannot be null");
        this.requestor = requestor;
        this.requestorPhoneNumber = requestorPhoneNumber;
        this.requestorUsername = requestorUsername;
        this.parentGroupJid = parentGroupJid;
        this.requestTimestamp = Objects.requireNonNull(requestTimestamp, "requestTimestamp cannot be null");
        this.method = method;
    }

    /**
     * Returns the requesting user's primary JID.
     *
     * @return the non-{@code null} JID
     */
    public Jid requestingJid() {
        return requestingJid;
    }

    /**
     * Returns the resolved requestor JID when the relay surfaced one.
     *
     * @return an {@link Optional} carrying the requestor JID
     */
    public Optional<Jid> requestor() {
        return Optional.ofNullable(requestor);
    }

    /**
     * Returns the resolved requestor phone-number JID when the relay
     * surfaced one.
     *
     * @return an {@link Optional} carrying the phone-number JID
     */
    public Optional<Jid> requestorPhoneNumber() {
        return Optional.ofNullable(requestorPhoneNumber);
    }

    /**
     * Returns the resolved requestor username when the relay surfaced one.
     *
     * @return an {@link Optional} carrying the username
     */
    public Optional<String> requestorUsername() {
        return Optional.ofNullable(requestorUsername);
    }

    /**
     * Returns the parent-community JID surfaced for community-link join
     * requests.
     *
     * @return an {@link Optional} carrying the parent-community JID
     */
    public Optional<Jid> parentGroupJid() {
        return Optional.ofNullable(parentGroupJid);
    }

    /**
     * Returns the instant at which the request was filed.
     *
     * @return the non-{@code null} timestamp
     */
    public Instant requestTimestamp() {
        return requestTimestamp;
    }

    /**
     * Returns the optional request-pathway discriminator.
     *
     * @return an {@link Optional} carrying the {@link Method}
     */
    public Optional<Method> method() {
        return Optional.ofNullable(method);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupMembershipApprovalRequest that
                && Objects.equals(requestingJid, that.requestingJid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(requestingJid);
    }

    /**
     * Identifies the pathway through which a membership-approval request
     * reached the relay.
     */
    @ProtobufEnum
    public enum Method {
        /**
         * The user followed an invite link.
         */
        INVITE_LINK(0),

        /**
         * The user joined via the parent-community linked-group flow.
         */
        LINKED_GROUP_JOIN(1),

        /**
         * A non-admin member attempted to add the user directly.
         */
        NON_ADMIN_ADD(2);

        /**
         * The protobuf wire-format index associated with this method.
         */
        final int index;

        /**
         * Constructs a new {@code Method} with the supplied protobuf
         * index.
         *
         * @param index the protobuf wire-format index
         */
        Method(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire-format index associated with this
         * value.
         *
         * @return the protobuf wire-format index
         */
        public int index() {
            return index;
        }
    }
}
