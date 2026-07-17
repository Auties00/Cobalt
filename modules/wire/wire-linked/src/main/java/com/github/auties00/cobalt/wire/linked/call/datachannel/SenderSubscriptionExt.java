package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Sender-side subscription extension describing the SSRC -> PID layout
 * for one participant's outbound simulcast.
 *
 * <p>Maps every SSRC the sender emits to the participant PID and the
 * SVC temporal layer ({@link TemporalLayer#BASE} or
 * {@link TemporalLayer#ENHANCEMENT}) the PID belongs to. Receivers use
 * this to know which SSRC to subscribe to per requested quality level.
 */
@ProtobufMessage(name = "SenderSubscriptionExt")
public final class SenderSubscriptionExt {
    /**
     * The SSRC layer assignments for this sender.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SSrcsToPidAssignments ssrcLayers;

    /**
     * Constructs a new {@code SenderSubscriptionExt}.
     *
     * @param ssrcLayers the SSRC layer assignments
     */
    SenderSubscriptionExt(SSrcsToPidAssignments ssrcLayers) {
        this.ssrcLayers = ssrcLayers;
    }

    /**
     * Returns the SSRC layer assignments.
     *
     * @return an {@link Optional} with the assignments, or empty
     */
    public Optional<SSrcsToPidAssignments> ssrcLayers() {
        return Optional.ofNullable(ssrcLayers);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof SenderSubscriptionExt that
                && Objects.equals(this.ssrcLayers, that.ssrcLayers));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ssrcLayers);
    }

    @Override
    public String toString() {
        return "SenderSubscriptionExt[ssrcLayers=" + ssrcLayers + ']';
    }

    /**
     * Binding of one participant PID to the SVC temporal layer it occupies.
     */
    @ProtobufMessage(name = "SenderSubscriptionExt.PidTemporalLayer")
    public static final class PidTemporalLayer {
        /**
         * The participant PID.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        final Integer pid;

        /**
         * The SVC temporal layer this PID occupies.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        final TemporalLayer layerId;

        /**
         * Constructs a new {@code PidTemporalLayer}.
         *
         * @param pid     the participant PID
         * @param layerId the temporal layer
         */
        PidTemporalLayer(Integer pid, TemporalLayer layerId) {
            this.pid = pid;
            this.layerId = layerId;
        }

        /**
         * Returns the participant PID.
         *
         * @return an {@link OptionalInt} with the PID, or empty
         */
        public OptionalInt pid() {
            return pid == null ? OptionalInt.empty() : OptionalInt.of(pid);
        }

        /**
         * Returns the temporal layer.
         *
         * @return an {@link Optional} with the layer, or empty
         */
        public Optional<TemporalLayer> layerId() {
            return Optional.ofNullable(layerId);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof PidTemporalLayer that
                    && Objects.equals(this.pid, that.pid)
                    && this.layerId == that.layerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pid, layerId);
        }

        @Override
        public String toString() {
            return "PidTemporalLayer[pid=" + pid + ", layerId=" + layerId + ']';
        }
    }

    /**
     * SSRC-to-PID layout for one sender.
     *
     * <p>The {@linkplain #ssrcs() ssrcs} list is the ordered set of SSRCs
     * the sender uses; the {@linkplain #pids() pids} list, in matching
     * order, identifies which PID and SVC layer each SSRC carries.
     */
    @ProtobufMessage(name = "SenderSubscriptionExt.SSrcsToPidAssignments")
    public static final class SSrcsToPidAssignments {
        /**
         * The ordered SSRCs used by the sender.
         *
         * <p>Held as {@code long} values in the canonical unsigned range {@code 0..0xFFFFFFFF} and
         * wire-encoded as packed {@code UINT64} varints so a high-bit-set SSRC encodes as a five-byte
         * unsigned varint rather than the ten-byte sign-extended form a {@code UINT32}-backed
         * {@code Integer} would produce, the same rationale {@link StreamSubscriptions.Entry#ssrc()}
         * documents for its single-SSRC field.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64, packed = true)
        final List<Long> ssrcs;

        /**
         * The PID and SVC layer for each SSRC, in matching order.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        final List<PidTemporalLayer> pids;

        /**
         * Constructs a new {@code SSrcsToPidAssignments}.
         *
         * @param ssrcs the SSRC list, each in the unsigned {@code 0..0xFFFFFFFF} range
         * @param pids  the PID-layer list
         */
        SSrcsToPidAssignments(List<Long> ssrcs, List<PidTemporalLayer> pids) {
            this.ssrcs = ssrcs;
            this.pids = pids;
        }

        /**
         * Returns the ordered SSRCs, each in the unsigned {@code 0..0xFFFFFFFF} range.
         *
         * @return an unmodifiable list, never {@code null}
         */
        public List<Long> ssrcs() {
            return ssrcs == null ? List.of() : Collections.unmodifiableList(ssrcs);
        }

        /**
         * Returns the PID-layer assignments.
         *
         * @return an unmodifiable list, never {@code null}
         */
        public List<PidTemporalLayer> pids() {
            return pids == null ? List.of() : Collections.unmodifiableList(pids);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof SSrcsToPidAssignments that
                    && Objects.equals(this.ssrcs, that.ssrcs)
                    && Objects.equals(this.pids, that.pids));
        }

        @Override
        public int hashCode() {
            return Objects.hash(ssrcs, pids);
        }

        @Override
        public String toString() {
            return "SSrcsToPidAssignments[ssrcs=" + ssrcs() + ", pids=" + pids() + ']';
        }
    }

    /**
     * SVC temporal-layer discriminator used in PID-to-layer assignments.
     */
    @ProtobufEnum(name = "SenderSubscriptionExt.TemporalLayer")
    public enum TemporalLayer {
        /**
         * The base temporal layer (lowest framerate).
         */
        BASE(0),

        /**
         * The enhancement temporal layer (additive frames on top of BASE).
         */
        ENHANCEMENT(1);

        /**
         * The protobuf wire index of this layer.
         */
        final int index;

        TemporalLayer(@ProtobufEnumIndex int index) {
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
