package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The combined per-stream subscription map a WhatsApp Web client publishes to the selective-forwarding
 * unit over the SCTP data channel.
 *
 * <p>Carries one flat {@link Entry} per logical stream the client either sends or wishes to receive,
 * keyed by the publishing {@linkplain Entry#participant() participant} and the
 * {@linkplain Entry#stream() stream} index, each mapped to the {@linkplain Entry#ssrc() SSRC} that
 * carries it. An entry with no participant identifies one of the client's own outbound streams (the
 * sender side), while an entry naming a participant identifies a remote stream the client subscribes to
 * receive (the receiver side); an entry with no stream index identifies the audio stream. This single
 * message therefore fuses the send layout and the receive wishes the older two-message
 * {@link SenderSubscriptions} / {@link RxSubscriptions} pair split apart.
 *
 * <p>This is the body of the proprietary {@code 0x4024} STUN attribute the subscription envelope embeds;
 * it is serialized verbatim as that attribute's value.
 */
@ProtobufMessage(name = "StreamSubscriptions")
public final class StreamSubscriptions {
    /**
     * The per-stream subscription entries.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<Entry> entries;

    /**
     * Constructs a new {@code StreamSubscriptions}.
     *
     * @param entries the per-stream entries
     */
    StreamSubscriptions(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Returns the per-stream subscription entries.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<Entry> entries() {
        return entries == null ? List.of() : Collections.unmodifiableList(entries);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof StreamSubscriptions that
                && Objects.equals(this.entries, that.entries));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entries);
    }

    @Override
    public String toString() {
        return "StreamSubscriptions[entries=" + entries() + ']';
    }

    /**
     * One stream subscription: a publishing participant and stream index mapped to the SSRC that carries
     * it.
     *
     * <p>The {@linkplain #participant() participant} is absent for one of the client's own outbound
     * streams and present for a remote participant's stream the client subscribes to. The
     * {@linkplain #stream() stream} index is absent for the audio stream and present (one-based) for a
     * video or auxiliary stream. The {@linkplain #ssrc() SSRC} is always present and identifies the
     * synchronization source the stream rides.
     */
    @ProtobufMessage(name = "StreamSubscriptions.Entry")
    public static final class Entry {
        /**
         * The publishing participant id, or {@code null} for one of the client's own outbound streams.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        final Integer participant;

        /**
         * The one-based stream index, or {@code null} for the audio stream.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        final Integer stream;

        /**
         * The synchronization-source identifier carrying the stream.
         *
         * <p>Held as a {@code long} in the canonical unsigned range {@code 0..0xFFFFFFFF} and wire-encoded
         * as a {@code UINT64} varint so a high-bit-set SSRC encodes as a five-byte unsigned varint rather
         * than the ten-byte sign-extended form a {@code UINT32}-backed {@code Integer} would produce.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        final Long ssrc;

        /**
         * Constructs a new {@code Entry}.
         *
         * @param participant the publishing participant id, or {@code null} for a self stream
         * @param stream      the one-based stream index, or {@code null} for the audio stream
         * @param ssrc        the SSRC carrying the stream
         */
        Entry(Integer participant, Integer stream, Long ssrc) {
            this.participant = participant;
            this.stream = stream;
            this.ssrc = ssrc;
        }

        /**
         * Returns the publishing participant id.
         *
         * @return an {@link OptionalInt} with the participant id, or empty for a self stream
         */
        public OptionalInt participant() {
            return participant == null ? OptionalInt.empty() : OptionalInt.of(participant);
        }

        /**
         * Returns the one-based stream index.
         *
         * @return an {@link OptionalInt} with the stream index, or empty for the audio stream
         */
        public OptionalInt stream() {
            return stream == null ? OptionalInt.empty() : OptionalInt.of(stream);
        }

        /**
         * Returns the SSRC carrying the stream.
         *
         * @return an {@link OptionalLong} with the SSRC, or empty when absent
         */
        public OptionalLong ssrc() {
            return ssrc == null ? OptionalLong.empty() : OptionalLong.of(ssrc);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Entry that
                    && Objects.equals(this.participant, that.participant)
                    && Objects.equals(this.stream, that.stream)
                    && Objects.equals(this.ssrc, that.ssrc));
        }

        @Override
        public int hashCode() {
            return Objects.hash(participant, stream, ssrc);
        }

        @Override
        public String toString() {
            return "Entry[participant=" + participant + ", stream=" + stream + ", ssrc=" + ssrc + ']';
        }
    }
}
