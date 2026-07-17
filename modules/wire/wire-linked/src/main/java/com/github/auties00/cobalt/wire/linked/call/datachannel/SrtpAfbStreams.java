package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Application-layer SRTP feedback for every stream a peer is tracking.
 *
 * <p>An {@code SrtpAfbStreams} bundle carries one {@link SrtpAfbStreamInfo}
 * per SSRC the sender tracks. It is published as part of the periodic
 * application feedback the receiver uses to keep its SRTP replay windows
 * synchronised with the sender's; missing entries identify desynchronised
 * SSRCs the receiver must request a rekey for.
 */
@ProtobufMessage(name = "SrtpAfbStreams")
public final class SrtpAfbStreams {
    /**
     * The per-stream feedback entries.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<SrtpAfbStreamInfo> srtpAfb;

    /**
     * Constructs a new {@code SrtpAfbStreams}.
     *
     * @param srtpAfb the per-stream entries, or {@code null} for none
     */
    SrtpAfbStreams(List<SrtpAfbStreamInfo> srtpAfb) {
        this.srtpAfb = srtpAfb;
    }

    /**
     * Returns the per-stream feedback entries.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<SrtpAfbStreamInfo> srtpAfb() {
        return srtpAfb == null ? List.of() : Collections.unmodifiableList(srtpAfb);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SrtpAfbStreams that
                && Objects.equals(this.srtpAfb, that.srtpAfb));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(srtpAfb);
    }

    @Override
    public String toString() {
        return "SrtpAfbStreams[srtpAfb=" + srtpAfb() + ']';
    }
}
