package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Describes one logical stream multiplexed inside a WhatsApp call session.
 *
 * <p>Every call carries a fixed set of logical streams (audio, video
 * simulcast layers, FEC, AppData, live transcription, screen share,
 * IMU data). A {@code StreamDescriptor} binds one {@linkplain #streamLayer()
 * logical layer} to one {@linkplain #payloadType() payload type} and one
 * {@linkplain #ssrc() SSRC} on the wire, and indicates whether
 * {@linkplain #isUplinkPrefetchEnabled() uplink prefetch} is engaged for
 * that layer.
 */
@ProtobufMessage(name = "StreamDescriptor")
public final class StreamDescriptor {
    /**
     * The logical layer this descriptor describes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final StreamLayer streamLayer;

    /**
     * The payload type this descriptor describes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final PayloadType payloadType;

    /**
     * The synchronization-source identifier of the stream.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final Integer ssrc;

    /**
     * Whether uplink prefetch is enabled on this stream.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final Boolean isUplinkPrefetchEnabled;

    /**
     * Constructs a new {@code StreamDescriptor}.
     *
     * @param streamLayer             the logical layer
     * @param payloadType             the payload type
     * @param ssrc                    the SSRC
     * @param isUplinkPrefetchEnabled whether uplink prefetch is enabled
     */
    StreamDescriptor(StreamLayer streamLayer,
                     PayloadType payloadType,
                     Integer ssrc,
                     Boolean isUplinkPrefetchEnabled) {
        this.streamLayer = streamLayer;
        this.payloadType = payloadType;
        this.ssrc = ssrc;
        this.isUplinkPrefetchEnabled = isUplinkPrefetchEnabled;
    }

    /**
     * Returns the logical layer of this descriptor.
     *
     * @return an {@link Optional} with the layer, or empty
     */
    public Optional<StreamLayer> streamLayer() {
        return Optional.ofNullable(streamLayer);
    }

    /**
     * Returns the payload type of this descriptor.
     *
     * @return an {@link Optional} with the payload type, or empty
     */
    public Optional<PayloadType> payloadType() {
        return Optional.ofNullable(payloadType);
    }

    /**
     * Returns the SSRC of this stream.
     *
     * @return an {@link OptionalInt} with the SSRC, or empty
     */
    public OptionalInt ssrc() {
        return ssrc == null ? OptionalInt.empty() : OptionalInt.of(ssrc);
    }

    /**
     * Returns whether uplink prefetch is enabled.
     *
     * @return {@code true} when the flag was set
     */
    public boolean isUplinkPrefetchEnabled() {
        return isUplinkPrefetchEnabled != null && isUplinkPrefetchEnabled;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof StreamDescriptor that
                && this.streamLayer == that.streamLayer
                && this.payloadType == that.payloadType
                && Objects.equals(this.ssrc, that.ssrc)
                && Objects.equals(this.isUplinkPrefetchEnabled, that.isUplinkPrefetchEnabled));
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamLayer, payloadType, ssrc, isUplinkPrefetchEnabled);
    }

    @Override
    public String toString() {
        return "StreamDescriptor[streamLayer=" + streamLayer
                + ", payloadType=" + payloadType
                + ", ssrc=" + ssrc
                + ", isUplinkPrefetchEnabled=" + isUplinkPrefetchEnabled + ']';
    }

    /**
     * Payload-type discriminator for streams carried over the call session.
     *
     * <p>Splits each stream into either the underlying media payload, the
     * paired forward-error-correction stream, a negative-acknowledgement
     * channel, a hop-by-hop FEC channel, or an application-data channel.
     */
    @ProtobufEnum(name = "StreamDescriptor.PayloadType")
    public enum PayloadType {
        /**
         * The stream carries media payload bytes.
         */
        MEDIA(0),

        /**
         * The stream carries forward-error-correction (FEC) packets.
         */
        FEC(1),

        /**
         * The stream carries negative acknowledgements (NACK).
         */
        NACK(2),

        /**
         * The stream carries hop-by-hop FEC packets.
         */
        HBH_FEC(3),

        /**
         * The stream carries application-data payloads.
         */
        APP_DATA(4);

        /**
         * The protobuf wire index of this payload type.
         */
        final int index;

        PayloadType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index of this payload type.
         *
         * @return the index
         */
        public int index() {
            return index;
        }
    }

    /**
     * Logical-stream-layer discriminator for streams carried over a call.
     *
     * <p>Selects one of the ten logical layers WhatsApp's call session
     * multiplexes: audio, two video simulcast layers, two FEC channels
     * (client-to-server and server-to-client), an AppData channel, a
     * live-transcription channel, an IMU-data channel, and two
     * screen-share simulcast layers.
     */
    @ProtobufEnum(name = "StreamDescriptor.StreamLayer")
    public enum StreamLayer {
        /**
         * The audio stream layer.
         */
        AUDIO(0),

        /**
         * The lower-resolution video simulcast layer.
         */
        VIDEO_STREAM0(1),

        /**
         * The higher-resolution video simulcast layer.
         */
        VIDEO_STREAM1(2),

        /**
         * The client-to-server hop-by-hop FEC channel.
         */
        HBH_FEC_CLIENT_TO_SERVER(3),

        /**
         * The server-to-client hop-by-hop FEC channel.
         */
        HBH_FEC_SERVER_TO_CLIENT(4),

        /**
         * The application-data channel (reactions, transcription, etc.).
         */
        APP_DATA_STREAM0(5),

        /**
         * The live-transcription channel.
         */
        LIVE_TRANSCRIPTION_STREAM0(6),

        /**
         * The inertial-measurement-unit data channel.
         */
        IMU_DATA_STREAM0(7),

        /**
         * The lower-resolution screen-share simulcast layer.
         */
        SCREEN_SHARE_VIDEO_STREAM0(8),

        /**
         * The higher-resolution screen-share simulcast layer.
         */
        SCREEN_SHARE_VIDEO_STREAM1(9);

        /**
         * The protobuf wire index of this layer.
         */
        final int index;

        StreamLayer(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index of this layer.
         *
         * @return the index
         */
        public int index() {
            return index;
        }
    }
}
