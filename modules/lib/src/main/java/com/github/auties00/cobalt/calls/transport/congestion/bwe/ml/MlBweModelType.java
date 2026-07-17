package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

/**
 * Enumerates the machine learning bandwidth estimation model types the {@link MlBweEngine} can load,
 * each a distinct inference task in the sender side rate control pipeline.
 *
 * <p>Each model is a separate inference task loaded on demand and gated by its own
 * {@code cc_enable_ml_*_inference} voip param, so a build runs only the subset its configuration
 * enables. The constants cover the congestion classifier, two undershoot detectors, two high
 * definition targeting regressors, the trendline regressor, the packet loss concealment predictor,
 * the QuickHD ramp model, and the offline reinforcement learning estimator. Audio quality models the
 * same runtime also hosts (NADL, AutoMOS, video mean opinion score, video super resolution, noise
 * suppression) are not sender side bandwidth estimation models and are not enumerated here.
 */
public enum MlBweModelType {
    /**
     * Congestion classification: predicts a congestion probability the rate controller acts on, gated by
     * {@code cc_enable_ml_cong_inference}.
     *
     * <p>The model emits two float32 outputs; the second is scaled to a probability and compared against a
     * per call threshold to yield a binary congestion verdict.
     */
    CONG,

    /**
     * Undershoot detection: classifies whether the estimate has undershot link capacity, gated by
     * {@code cc_enable_ml_undershoot_inference}.
     */
    UNDERSHOOT,

    /**
     * Generic congestion undershoot detection (the {@code udst2} model), gated by
     * {@code cc_enable_ml_undershoot2_inference}.
     */
    GC_UNDERSHOOT,

    /**
     * High definition targeting: predicts the bitrate the link can sustain for high definition video,
     * gated by {@code cc_enable_ml_hd_targeting_inference}.
     */
    HD_TARGET,

    /**
     * Generic congestion high definition targeting (the {@code hd_targeting2} model), gated by
     * {@code cc_enable_ml_hd_targeting2_inference}.
     */
    GC_HD_TARGET,

    /**
     * Trendline regression: a learned alternative to the delay based trendline slope, gated by
     * {@code cc_enable_ml_tr_inference}.
     */
    TR,

    /**
     * Packet loss concealment: predicts the loss ratio for concealment decisions, gated by
     * {@code cc_enable_ml_plc_inference}.
     *
     * <p>This model is loaded and run by the audio jitter buffer path rather than the sender bandwidth
     * estimation loop, so its feature vector and output differ from the rate control models.
     */
    PLC,

    /**
     * QuickHD: drives an instant high definition ramp up when the link can carry it, gated by the QuickHD
     * model namespace ({@code wavoip_ml_bwe_quickhd_model_download_versions}).
     */
    QUICKHD,

    /**
     * Offline reinforcement learning bandwidth estimation: predicts a target bitrate consumed when
     * {@code cc_use_offline_rl_bwe_as_target} is set, gated by {@code cc_enable_offline_rl_bwe_inference}.
     */
    OFFLINE_RL
}
