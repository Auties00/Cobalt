package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Classifies the encryption guarantees under which a message was
 * exchanged, as rendered by the privacy system banner in the chat UI.
 *
 * <p>Regular conversations on WhatsApp are protected by Signal-based
 * end-to-end encryption, but a handful of interactions, such as
 * messages to business accounts that store their content on third
 * party infrastructure, relax those guarantees. The privacy banner
 * surfaces this distinction to the user so that they know up front
 * whether the conversation is fully end-to-end encrypted or not.
 */
@ProtobufEnum(name = "PrivacySystemMessage")
public enum PrivacySystemMessage {
    /**
     * The conversation is end-to-end encrypted for every participant.
     */
    E2EE_MSG(1),

    /**
     * The conversation is not end-to-end encrypted and the logged-in
     * user is the one whose content is being stored outside the E2EE
     * boundary.
     */
    NE2EE_SELF(2),

    /**
     * The conversation is not end-to-end encrypted and the remote
     * party is the one whose content is being stored outside the E2EE
     * boundary.
     */
    NE2EE_OTHER(3);

    /**
     * Constructs a new enum constant with the given protobuf wire
     * index.
     *
     * @param index the protobuf wire index for this constant
     */
    PrivacySystemMessage(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index identifying this constant on the wire.
     */
    final int index;

    /**
     * Returns the protobuf wire index of this constant.
     *
     * @return the non-negative wire index
     */
    public int index() {
        return this.index;
    }
}
