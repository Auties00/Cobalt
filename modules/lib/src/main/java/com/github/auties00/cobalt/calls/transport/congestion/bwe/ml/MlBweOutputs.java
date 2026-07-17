package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

/**
 * Holds the steering outputs a machine learning bandwidth estimation inference round produces, which
 * the sender estimator folds into its rate decisions.
 *
 * <p>Each component carries one head's verdict or value: a congestion verdict with its quantized
 * probability, an undershoot verdict, a predicted loss ratio, and a high definition target bitrate. The
 * {@link #DISABLED} instance is the neutral value the {@link NoopMlBweEngine} returns, so the delay based
 * and sender side path runs unchanged when machine learning is off.
 *
 * <p>The congestion probability is a {@code ushort} in {@code [0, }{@link #MAX_CONGESTION_PROBABILITY}{@code ]}:
 * the congestion head scales its raw output by {@code 1000} and quantizes it into that range, then compares
 * it against a per call threshold to yield the congestion verdict.
 *
 * @param congestionDetected     whether the congestion model reported congestion
 * @param congestionProbability  the quantized congestion probability, in
 *                               {@code [0, }{@link #MAX_CONGESTION_PROBABILITY}{@code ]}; {@code 0} when no
 *                               congestion inference ran
 * @param undershootDetected     whether the undershoot model reported that the estimate has undershot link
 *                               capacity
 * @param predictedLoss          the predicted packet loss ratio, in {@code [0, 1]}; {@code 0} when no
 *                               prediction is available
 * @param hdTargetBps            the high definition target bitrate, in bits per second; {@code 0} when no
 *                               target is available
 */
public record MlBweOutputs(
        boolean congestionDetected,
        int congestionProbability,
        boolean undershootDetected,
        // TODO: derive predictedLoss from the packet loss concealment head; its feature and output wiring
        //  is not yet implemented, so this component is always left 0
        double predictedLoss,
        // TODO: derive hdTargetBps from the high definition targeting head; the head reports a boolean
        //  positive/negative verdict and the bitrate derivation is not yet implemented, so this component
        //  is always left 0
        long hdTargetBps
) {
    /**
     * The maximum quantized congestion probability, the upper bound of the {@code ushort} range.
     *
     * @implNote This implementation uses {@code 65535}, the {@code ushort} ceiling the congestion head
     * quantizes its scaled probability into.
     */
    public static final int MAX_CONGESTION_PROBABILITY = 65535;

    /**
     * The outputs returned when machine learning is disabled: no congestion, no undershoot, no loss
     * prediction, and no high definition target.
     */
    public static final MlBweOutputs DISABLED = new MlBweOutputs(false, 0, false, 0.0, 0);

    /**
     * Returns a congestion only output carrying the verdict and its quantized probability.
     *
     * <p>This is the output the congestion head produces on its own: the verdict and the probability, with
     * no undershoot, loss prediction, or high definition target. A negative verdict with a {@code 0}
     * probability collapses to the shared {@link #DISABLED} instance.
     *
     * @param detected    whether the congestion model reported congestion
     * @param probability the quantized congestion probability, in
     *                    {@code [0, }{@link #MAX_CONGESTION_PROBABILITY}{@code ]}
     * @return the congestion only outputs, or {@link #DISABLED} when both arguments are neutral
     */
    public static MlBweOutputs ofCongestion(boolean detected, int probability) {
        if (!detected && probability == 0) {
            return DISABLED;
        }
        return new MlBweOutputs(detected, probability, false, 0.0, 0);
    }
}
