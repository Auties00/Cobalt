package com.github.auties00.cobalt.wire.linked.message.media;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Scope under which a media message's decryption key was derived.
 *
 * <p>WhatsApp derives per-message media keys in different cryptographic domains
 * depending on where the media is used. The domain is serialized with the message
 * so that decryption can pick the correct derivation path.
 */
@ProtobufEnum(name = "Message.MediaKeyDomain")
public enum MediaMessageKeyDomain {
    /**
     * No explicit domain is set; legacy default.
     */
    UNSET(0),
    /**
     * Key derived for end-to-end encrypted one-to-one or group chat media.
     */
    E2EE_CHAT(1),
    /**
     * Key derived for status updates.
     */
    STATUS(2),
    /**
     * Key derived for the WhatsApp Cloud API surface.
     */
    CAPI(3),
    /**
     * Key derived for bot interactions.
     */
    BOT(4);

    /**
     * Constructs a new enum constant.
     *
     * @param index the protobuf wire index used to serialize this constant
     */
    MediaMessageKeyDomain(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Protobuf wire index of this constant.
     */
    final int index;

    /**
     * Returns the protobuf wire index of this constant.
     *
     * @return the index
     */
    public int index() {
        return this.index;
    }
}
