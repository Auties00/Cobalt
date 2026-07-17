package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Bandwidth-allocation verdict for one simulcast round.
 *
 * <p>Carries the {@linkplain #info() round identifier and participant
 * count} together with up to two per-layer {@link BwaStream} allocations
 * ({@linkplain #stream1() stream1}, {@linkplain #stream2() stream2}).
 * The producer fills in only the layers it allocates bandwidth to; a
 * receiver that sees only {@code stream1} populated treats the round as
 * single-layer.
 */
@ProtobufMessage(name = "SimulcastBwaResults")
public final class SimulcastBwaResults {
    /**
     * The round identifier and participant count this allocation belongs to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final BwaInfo info;

    /**
     * The allocation for the first simulcast layer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final BwaStream stream1;

    /**
     * The allocation for the second simulcast layer.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final BwaStream stream2;

    /**
     * Constructs a new {@code SimulcastBwaResults}.
     *
     * @param info    the round identifier
     * @param stream1 the first-layer allocation
     * @param stream2 the second-layer allocation
     */
    SimulcastBwaResults(BwaInfo info, BwaStream stream1, BwaStream stream2) {
        this.info = info;
        this.stream1 = stream1;
        this.stream2 = stream2;
    }

    /**
     * Returns the round identifier and participant count.
     *
     * @return an {@link Optional} with the info, or empty
     */
    public Optional<BwaInfo> info() {
        return Optional.ofNullable(info);
    }

    /**
     * Returns the allocation for the first simulcast layer.
     *
     * @return an {@link Optional} with the layer-1 stream, or empty
     */
    public Optional<BwaStream> stream1() {
        return Optional.ofNullable(stream1);
    }

    /**
     * Returns the allocation for the second simulcast layer.
     *
     * @return an {@link Optional} with the layer-2 stream, or empty
     */
    public Optional<BwaStream> stream2() {
        return Optional.ofNullable(stream2);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SimulcastBwaResults that
                && Objects.equals(this.info, that.info)
                && Objects.equals(this.stream1, that.stream1)
                && Objects.equals(this.stream2, that.stream2));
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, stream1, stream2);
    }

    @Override
    public String toString() {
        return "SimulcastBwaResults[info=" + info
                + ", stream1=" + stream1 + ", stream2=" + stream2 + ']';
    }
}
