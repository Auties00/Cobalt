package com.github.auties00.cobalt.wire.push.fcm.mcs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models an MCS iq stanza.
 *
 * <p>Cobalt emits a single shape of this stanza: the stream-ack iq sent
 * periodically once enough frames have accumulated, carrying the current
 * {@code lastStreamIdReceived} cursor so the server can advance its retry
 * cursor. The remaining fields are filled with the fixed values the native
 * client uses for that ack.
 *
 * @implNote This implementation carries the MCS frame tag {@code 7}; the tag is
 * written as the frame's length-prefixed type byte by the connection layer, not
 * by this message.
 */
@ProtobufMessage(name = "FcmMcsIqStanza")
public final class FcmMcsIqStanza {
    /**
     * Holds the iq type.
     *
     * @implNote This implementation sends {@code 1} (set) for stream acks.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long type;

    /**
     * Holds the iq id.
     *
     * @implNote This implementation sends an empty string for stream acks,
     * matching the native client.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String id;

    /**
     * Holds the extension nested message carrying the iq-specific payload.
     *
     * <p>For a stream ack the extension's {@link Extension#id} is {@code 13} and
     * its {@link Extension#data} is a zero-length byte string.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    Extension extension;

    /**
     * Holds the cursor sent alongside the iq.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    long lastStreamIdReceived;

    /**
     * Holds the iq status.
     *
     * @implNote This implementation always sends {@code 0} for stream acks.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    long status;

    /**
     * Constructs a new iq with the given values.
     *
     * @param type                 the iq type
     * @param id                   the iq id
     * @param extension            the extension payload
     * @param lastStreamIdReceived the cursor to advertise
     * @param status               the iq status
     */
    FcmMcsIqStanza(long type, String id, Extension extension,
                   long lastStreamIdReceived, long status) {
        this.type = type;
        this.id = id;
        this.extension = extension;
        this.lastStreamIdReceived = lastStreamIdReceived;
        this.status = status;
    }

    /**
     * Models the iq extension payload carried in field 7 of the parent iq.
     *
     * <p>For a stream ack the {@link #id} is {@code 13} and the {@link #data} is
     * an empty byte array.
     */
    @ProtobufMessage(name = "FcmMcsIqStanza.Extension")
    public static final class Extension {
        /**
         * Holds the extension id.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long id;

        /**
         * Holds the opaque extension payload bytes.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] data;

        /**
         * Constructs a new extension with the given id and payload.
         *
         * @param id   the extension id
         * @param data the opaque payload bytes
         */
        Extension(long id, byte[] data) {
            this.id = id;
            this.data = data;
        }
    }
}
