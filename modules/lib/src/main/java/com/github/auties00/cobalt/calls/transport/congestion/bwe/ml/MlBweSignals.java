package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

/**
 * Carries one round's network signals into {@link MlBweEngine#infer(MlBweSignals)}, in the raw units the
 * feature writer consumes before its per feature scaling.
 *
 * <p>Each inbound feedback round the rate control loop builds one of these and the engine pushes the
 * values into its per model slide window ring. The fields are the feature slots the voip feature writer
 * threads, in their units before scaling: the engine applies the scaling itself (loss times one hundred,
 * nanoseconds to microseconds by dividing by one thousand, bits per second to kilobits per second by
 * dividing by one thousand). The slots the rate control loop already measures are carried as concrete
 * values; the slots not yet threaded (the audio round trip, the video and audio jitter, the packet pair
 * estimate, and the configured categorical voip param knobs) are carried as the {@link #UNAVAILABLE}
 * sentinel, and the engine runs a model only when that model's full feature selection can be filled from
 * concrete values, never fabricating a missing signal.
 *
 * @param packetLossFraction  the packet loss ratio over the round, in {@code [0, 1]} (feature slot 0)
 * @param rttNs               the round trip time, in nanoseconds (feature slot 1)
 * @param remoteBweBps        the remote receiver estimate, in bits per second (feature slot 2)
 * @param senderBweBps        the sender side estimate, in bits per second (feature slot 3)
 * @param audioRttNs          the audio round trip time, in nanoseconds, or {@link #UNAVAILABLE} (slot 4)
 * @param videoJitterNs       the video jitter, in nanoseconds, or {@link #UNAVAILABLE} (slot 5)
 * @param audioJitterNs       the audio jitter, in nanoseconds, or {@link #UNAVAILABLE} (slot 6)
 * @param packetPairBps       the packet pair link capacity estimate, in bits per second, or
 *                            {@link #UNAVAILABLE} (slot 7)
 * @param maxTargetBitrateBps the configured maximum target bitrate, in bits per second (feature slot 11)
 * @implNote This implementation carries the eight measured feature windows (packet loss, round trip,
 * remote estimate, sender estimate, audio round trip, video jitter, audio jitter, packet pair) plus the
 * configured maximum bitrate window, and omits the four categorical voip param windows and the unused
 * placeholder window, none of which are measured signals. An unthreaded measured slot holds
 * {@link #UNAVAILABLE} rather than a guessed value because feeding a fabricated value would silently
 * change the model input.
 */
public record MlBweSignals(
        double packetLossFraction,
        long rttNs,
        long remoteBweBps,
        long senderBweBps,
        long audioRttNs,
        long videoJitterNs,
        long audioJitterNs,
        long packetPairBps,
        long maxTargetBitrateBps
) {
    /**
     * The sentinel marking a measured signal slot the rate control loop does not yet thread.
     *
     * <p>A field set to this value is not a measurement; the engine treats any feature whose source slot
     * holds this sentinel as unfillable and skips the model rather than feeding a fabricated value.
     */
    public static final long UNAVAILABLE = Long.MIN_VALUE;

    /**
     * Returns whether the given slot value is a concrete measurement rather than the {@link #UNAVAILABLE}
     * sentinel.
     *
     * @param slotValue the raw slot value to test
     * @return {@code true} when {@code slotValue} is a real measurement
     */
    public static boolean isAvailable(long slotValue) {
        return slotValue != UNAVAILABLE;
    }

    /**
     * Builds a signal set from the slots the rate control loop measures, marking the not yet threaded
     * measured slots as {@link #UNAVAILABLE}.
     *
     * <p>This is the factory the rate control loop uses each round: it supplies the packet loss, the round
     * trip time, the remote and sender estimates, and the configured maximum target bitrate, and the audio
     * round trip, the jitter pair, and the packet pair estimate are left unavailable until those
     * measurements are threaded through.
     *
     * @param packetLossFraction  the packet loss ratio over the round, in {@code [0, 1]}
     * @param rttNs               the round trip time, in nanoseconds
     * @param remoteBweBps        the remote receiver estimate, in bits per second
     * @param senderBweBps        the sender side estimate, in bits per second
     * @param maxTargetBitrateBps the configured maximum target bitrate, in bits per second
     * @return the signal set with the unthreaded measured slots marked unavailable
     */
    public static MlBweSignals ofThreaded(double packetLossFraction, long rttNs, long remoteBweBps,
                                          long senderBweBps, long maxTargetBitrateBps) {
        return new MlBweSignals(
                packetLossFraction,
                rttNs,
                remoteBweBps,
                senderBweBps,
                UNAVAILABLE,
                UNAVAILABLE,
                UNAVAILABLE,
                UNAVAILABLE,
                maxTargetBitrateBps);
    }
}
