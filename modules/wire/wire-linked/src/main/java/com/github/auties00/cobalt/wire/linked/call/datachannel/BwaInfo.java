package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Bandwidth-allocation control identifier for one simulcast round.
 *
 * <p>WhatsApp's bandwidth-allocation (BWA) algorithm tags each round of
 * per-stream bitrate decisions with a monotonically-increasing
 * {@linkplain #id() id} and the current {@linkplain #numParticipants()
 * participant count}, which together let the receiver correlate
 * {@link BwaStream} entries with the BWA round that produced them.
 */
@ProtobufMessage(name = "BwaInfo")
public final class BwaInfo {
    /**
     * The monotonic round identifier minted by the BWA algorithm.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer id;

    /**
     * The number of participants the round was computed against.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer numParticipants;

    /**
     * Constructs a new {@code BwaInfo}.
     *
     * @param id              the round id, or {@code null} when absent
     * @param numParticipants the participant count, or {@code null}
     */
    BwaInfo(Integer id, Integer numParticipants) {
        this.id = id;
        this.numParticipants = numParticipants;
    }

    /**
     * Returns the monotonic round identifier.
     *
     * @return an {@link OptionalInt} carrying the id, or empty
     */
    public OptionalInt id() {
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    /**
     * Returns the participant count this round was computed against.
     *
     * @return an {@link OptionalInt} carrying the count, or empty
     */
    public OptionalInt numParticipants() {
        return numParticipants == null ? OptionalInt.empty() : OptionalInt.of(numParticipants);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof BwaInfo that
                && Objects.equals(this.id, that.id)
                && Objects.equals(this.numParticipants, that.numParticipants));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numParticipants);
    }

    @Override
    public String toString() {
        return "BwaInfo[id=" + id + ", numParticipants=" + numParticipants + ']';
    }
}
