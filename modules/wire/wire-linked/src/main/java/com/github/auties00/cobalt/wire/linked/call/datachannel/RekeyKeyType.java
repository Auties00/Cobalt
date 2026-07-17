package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Discriminator for the three SRTP key domains a call rekey may target.
 *
 * <p>A call carries three independent encryption contexts: one for audio
 * RTP, one for video RTP, and one for the AppData DataChannel. When a
 * rekey event fires (for example on participant join or leave), each
 * {@link RekeyKeyEntry} in the {@link E2eRekeyPayload} is tagged with one
 * of these constants so the receiver can route the new key bytes to the
 * matching SRTP context.
 */
@ProtobufEnum(name = "RekeyKeyType")
public enum RekeyKeyType {
    /**
     * The rekey applies to the audio RTP stream's SRTP context.
     */
    AUDIO(0),

    /**
     * The rekey applies to the video RTP stream's SRTP context.
     */
    VIDEO(1),

    /**
     * The rekey applies to the AppData DataChannel's SRTP context.
     */
    APPDATA(2);

    /**
     * The protobuf wire index of this domain.
     */
    final int index;

    /**
     * Constructs a new {@code RekeyKeyType}.
     *
     * @param index the protobuf wire index
     */
    RekeyKeyType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire index of this domain.
     *
     * @return the index
     */
    public int index() {
        return index;
    }
}
