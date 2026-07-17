package com.github.auties00.cobalt.calls.config.param;

/**
 * Models one predicate in the dynamic rate control rule engine's condition vocabulary.
 *
 * <p>The voip param system carries a dynamic rule table whose entries each gate a set of
 * parameter overrides behind a conjunction of named conditions. The engine registers 133
 * distinct {@code cond_*} predicates that it evaluates per
 * rate control round against the call's live runtime metrics (uplink bandwidth estimate,
 * round trip time, packet loss, group call size, speaker role, congestion state, and the
 * rest); a rule applies only when all its conditions hold. This sealed interface is the
 * typed representation of one such predicate, so the rule matcher can branch over the
 * condition families exhaustively.
 *
 * <p>The conditions group into a small number of families that share an evaluation shape:
 * {@link Range} tests whether a runtime metric falls within an inclusive lower and upper
 * bound; {@link Flag} tests a boolean runtime state; {@link Mask} tests whether a runtime
 * bitfield intersects (or is contained in) a constant mask; and {@link Codec} tests the
 * negotiated codec set. Each instance carries the specific
 * {@link VoipParamConditionKind} it represents, which names the exact {@code cond_*}
 * predicate, alongside the constant operands that parameterise it. The runtime metric
 * values themselves are not held here; they are supplied by the rate control reader at
 * evaluation time.
 *
 * <p>This type is the Cobalt owner of the condition seam: it declares the full
 * {@code permits} clause for the four families. The complete catalogue of which conditions
 * exist, the wire layout of the rule table that references them, and the per rule operand
 * encoding are owned by the bandwidth estimation and rate control reader; this interface
 * models the predicate vocabulary the recovered {@code voip_settings} document exposes.
 *
 * @implNote This implementation models each predicate as a registered condition entry keyed
 * by name and preserves the condition vocabulary as a sealed family of four evaluation shapes
 * rather than a per condition record explosion.
 */
public sealed interface VoipParamCondition
        permits VoipParamCondition.Range, VoipParamCondition.Flag, VoipParamCondition.Mask, VoipParamCondition.Codec {
    /**
     * Returns the specific predicate this condition represents.
     *
     * @return the condition kind
     */
    VoipParamConditionKind kind();

    /**
     * Enumerates the recovered {@code cond_*} predicates and the family each belongs to.
     *
     * <p>Each constant binds the engine's predicate name to the
     * {@linkplain VoipParamConditionFamily family} that determines its evaluation shape.
     * The set is the subset whose family is known; the engine registers 133 distinct
     * {@code cond_*} predicates in total, so predicates whose family cannot yet be classified
     * are not present.
     */
    // TODO: extend this vocabulary to the full 133 conjunction predicates. Two facts block a mechanical
    //  extension. First, the raw name set is not directly usable: it mixes the genuine predicates with
    //  threading artifacts (condition variable mutex/signal/wait/init names) and with rule local tuning
    //  scalars that are NOT conjunction predicates (per metric EMA alphas, congestion thresholds, the
    //  hysteresis pairs). Second, the evaluation family of each genuine predicate cannot be read from the
    //  registry descriptor: every condition is registered with an identical type and width, so a flag, a
    //  mask, and a range scalar are indistinguishable there. Family is decided only by the rate control
    //  matcher, which addresses each condition by computed offset rather than by name. The operand
    //  encoding is partially observable in a captured rule table (a range carries a "lo,hi" string with *
    //  or empty meaning unbounded, e.g. "*,75000" / "1500,*" / "250,750"; a mask or scalar carries a bare
    //  integer, e.g. "1" or "34816"), but only for the conditions present in that capture, not for the
    //  family of every absent predicate. Blocked on recovering the registered condition ordering plus the
    //  per condition family for the full set.
    enum VoipParamConditionKind {
        /**
         * Tests the uplink bandwidth estimate against a range ({@code cond_range_ul_bwe}).
         */
        RANGE_UL_BWE("cond_range_ul_bwe", VoipParamConditionFamily.RANGE),

        /**
         * Tests the downlink bandwidth estimate against a range ({@code cond_range_dl_bwe}).
         */
        RANGE_DL_BWE("cond_range_dl_bwe", VoipParamConditionFamily.RANGE),

        /**
         * Tests the combined bandwidth estimate against a range ({@code cond_range_bwe}).
         */
        RANGE_BWE("cond_range_bwe", VoipParamConditionFamily.RANGE),

        /**
         * Tests the transmit side bandwidth estimate against a range
         * ({@code cond_range_tx_bwe}).
         */
        RANGE_TX_BWE("cond_range_tx_bwe", VoipParamConditionFamily.RANGE),

        /**
         * Tests the round trip time against a range ({@code cond_range_rtt}).
         */
        RANGE_RTT("cond_range_rtt", VoipParamConditionFamily.RANGE),

        /**
         * Tests the short term round trip time against a range
         * ({@code cond_range_short_term_rtt}).
         */
        RANGE_SHORT_TERM_RTT("cond_range_short_term_rtt", VoipParamConditionFamily.RANGE),

        /**
         * Tests the long term round trip time against a range
         * ({@code cond_range_long_term_rtt}).
         */
        RANGE_LONG_TERM_RTT("cond_range_long_term_rtt", VoipParamConditionFamily.RANGE),

        /**
         * Tests the uplink round trip time against a range
         * ({@code cond_range_uplink_rtt}).
         */
        RANGE_UPLINK_RTT("cond_range_uplink_rtt", VoipParamConditionFamily.RANGE),

        /**
         * Tests the exponential moving average round trip time against a range
         * ({@code cond_range_ema_rtt}).
         */
        RANGE_EMA_RTT("cond_range_ema_rtt", VoipParamConditionFamily.RANGE),

        /**
         * Tests the packet loss percentage against a range
         * ({@code cond_range_packet_loss_pct}).
         */
        RANGE_PACKET_LOSS_PCT("cond_range_packet_loss_pct", VoipParamConditionFamily.RANGE),

        /**
         * Tests the receive packet loss percentage against a range
         * ({@code cond_range_rx_packet_loss_pct}).
         */
        RANGE_RX_PACKET_LOSS_PCT("cond_range_rx_packet_loss_pct", VoipParamConditionFamily.RANGE),

        /**
         * Tests the uplink packet loss percentage against a range
         * ({@code cond_range_uplink_packet_loss_pct}).
         */
        RANGE_UPLINK_PACKET_LOSS_PCT("cond_range_uplink_packet_loss_pct", VoipParamConditionFamily.RANGE),

        /**
         * Tests the exponential moving average packet loss percentage against a range
         * ({@code cond_range_ema_packet_loss_pct}).
         */
        RANGE_EMA_PACKET_LOSS_PCT("cond_range_ema_packet_loss_pct", VoipParamConditionFamily.RANGE),

        /**
         * Tests the exponential moving average uplink packet loss rate against a range
         * ({@code cond_range_ema_uplink_plr}).
         */
        RANGE_EMA_UPLINK_PLR("cond_range_ema_uplink_plr", VoipParamConditionFamily.RANGE),

        /**
         * Tests the exponential moving average downlink packet loss rate against a range
         * ({@code cond_range_ema_downlink_plr}).
         */
        RANGE_EMA_DOWNLINK_PLR("cond_range_ema_downlink_plr", VoipParamConditionFamily.RANGE),

        /**
         * Tests the group call size against a range ({@code cond_range_gcall_size}).
         */
        RANGE_GCALL_SIZE("cond_range_gcall_size", VoipParamConditionFamily.RANGE),

        /**
         * Tests the seconds elapsed since call start against a range
         * ({@code cond_range_sec_since_start}).
         */
        RANGE_SEC_SINCE_START("cond_range_sec_since_start", VoipParamConditionFamily.RANGE),

        /**
         * Tests the seconds since the SFU became simulcast capable against a range
         * ({@code cond_range_sec_since_sfu_simulcast_capable}).
         */
        RANGE_SEC_SINCE_SFU_SIMULCAST_CAPABLE("cond_range_sec_since_sfu_simulcast_capable", VoipParamConditionFamily.RANGE),

        /**
         * Tests the target bitrate against a range ({@code cond_range_target_bitrate}).
         */
        RANGE_TARGET_BITRATE("cond_range_target_bitrate", VoipParamConditionFamily.RANGE),

        /**
         * Tests the total video target bitrate against a range
         * ({@code cond_range_total_vid_target_bitrate}).
         */
        RANGE_TOTAL_VID_TARGET_BITRATE("cond_range_total_vid_target_bitrate", VoipParamConditionFamily.RANGE),

        /**
         * Tests the bandwidth allocation video target bitrate against a range
         * ({@code cond_range_bwa_vid_target_bitrate}).
         */
        RANGE_BWA_VID_TARGET_BITRATE("cond_range_bwa_vid_target_bitrate", VoipParamConditionFamily.RANGE),

        /**
         * Tests the current video receive bitrate against a range
         * ({@code cond_range_cur_vid_rx_bitrate}).
         */
        RANGE_CUR_VID_RX_BITRATE("cond_range_cur_vid_rx_bitrate", VoipParamConditionFamily.RANGE),

        /**
         * Tests the peer device class against a range
         * ({@code cond_range_peer_device_class}).
         */
        RANGE_PEER_DEVICE_CLASS("cond_range_peer_device_class", VoipParamConditionFamily.RANGE),

        /**
         * Tests the Android device class against a range
         * ({@code cond_range_android_device_class}).
         */
        RANGE_ANDROID_DEVICE_CLASS("cond_range_android_device_class", VoipParamConditionFamily.RANGE),

        /**
         * Tests the minimum bandwidth estimate ({@code cond_min_bwe}).
         */
        MIN_BWE("cond_min_bwe", VoipParamConditionFamily.RANGE),

        /**
         * Tests whether the network medium matches a constant ({@code cond_net_medium}).
         */
        NET_MEDIUM("cond_net_medium", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the self and peer network media match a constant pair
         * ({@code cond_net_medium_pair}).
         */
        NET_MEDIUM_PAIR("cond_net_medium_pair", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the peer network medium matches a constant
         * ({@code cond_peer_net_medium}).
         */
        PEER_NET_MEDIUM("cond_peer_net_medium", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the self platform intersects a platform mask
         * ({@code cond_platform_mask}).
         */
        PLATFORM_MASK("cond_platform_mask", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the peer platform intersects a platform mask
         * ({@code cond_peer_platform_mask}).
         */
        PEER_PLATFORM_MASK("cond_peer_platform_mask", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the device class intersects a device class mask
         * ({@code cond_device_class_mask}).
         */
        DEVICE_CLASS_MASK("cond_device_class_mask", VoipParamConditionFamily.MASK),

        /**
         * Tests whether the congestion signal bitfield intersects a mask
         * ({@code cond_congestion_signal_mask}).
         */
        CONGESTION_SIGNAL_MASK("cond_congestion_signal_mask", VoipParamConditionFamily.MASK),

        /**
         * Tests the video quality mode against a constant ({@code cond_video_quality_mode}).
         */
        VIDEO_QUALITY_MODE("cond_video_quality_mode", VoipParamConditionFamily.MASK),

        /**
         * Tests the negotiated codec scheme against a constant ({@code cond_codec_scheme}).
         */
        CODEC_SCHEME("cond_codec_scheme", VoipParamConditionFamily.CODEC),

        /**
         * Tests the negotiated codec type against a constant ({@code cond_codec_type}).
         */
        CODEC_TYPE("cond_codec_type", VoipParamConditionFamily.CODEC),

        /**
         * Tests whether the negotiated codec set contains any of a constant set
         * ({@code cond_codecs_contain_any}).
         */
        CODECS_CONTAIN_ANY("cond_codecs_contain_any", VoipParamConditionFamily.CODEC),

        /**
         * Tests whether the negotiated codec set contains only a constant set
         * ({@code cond_codecs_contain_only}).
         */
        CODECS_CONTAIN_ONLY("cond_codecs_contain_only", VoipParamConditionFamily.CODEC),

        /**
         * Tests whether the local participant is the dominant speaker
         * ({@code cond_is_speaker}).
         */
        IS_SPEAKER("cond_is_speaker", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the call is currently in congestion ({@code cond_in_congestion}).
         */
        IN_CONGESTION("cond_in_congestion", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the transport is an SFU downlink ({@code cond_is_sfu_downlink}).
         */
        IS_SFU_DOWNLINK("cond_is_sfu_downlink", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the transport is an SFU uplink ({@code cond_is_sfu_uplink}).
         */
        IS_SFU_UPLINK("cond_is_sfu_uplink", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the call is a pure one to one call ({@code cond_pure_1x1_call}).
         */
        PURE_1X1_CALL("cond_pure_1x1_call", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the peer is in speaker view ({@code cond_peer_in_speaker_view}).
         */
        PEER_IN_SPEAKER_VIEW("cond_peer_in_speaker_view", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the local participant is in speaker view
         * ({@code cond_self_in_speaker_view}).
         */
        SELF_IN_SPEAKER_VIEW("cond_self_in_speaker_view", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the local participant is a screen share receiver
         * ({@code cond_screen_share_receiver}).
         */
        SCREEN_SHARE_RECEIVER("cond_screen_share_receiver", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the encoder is encoding screen sharing video
         * ({@code cond_encoding_video_screen_sharing}).
         */
        ENCODING_VIDEO_SCREEN_SHARING("cond_encoding_video_screen_sharing", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the initial round trip time is high ({@code cond_high_init_rtt}).
         */
        HIGH_INIT_RTT("cond_high_init_rtt", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the initial round trip time standard deviation is high
         * ({@code cond_high_init_rtt_stddev}).
         */
        HIGH_INIT_RTT_STDDEV("cond_high_init_rtt_stddev", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the call is in low quality video mode
         * ({@code cond_low_quality_vid_mode}).
         */
        LOW_QUALITY_VID_MODE("cond_low_quality_vid_mode", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the sender bandwidth estimator is in its bad state
         * ({@code cond_sbwe_in_bad}).
         */
        SBWE_IN_BAD("cond_sbwe_in_bad", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the sender bandwidth estimator is in its maximum congestion probe
         * state ({@code cond_sbwe_in_mcp}).
         */
        SBWE_IN_MCP("cond_sbwe_in_mcp", VoipParamConditionFamily.FLAG),

        /**
         * Tests whether the rule's parameters are present in the document
         * ({@code cond_params_in_json}).
         */
        PARAMS_IN_JSON("cond_params_in_json", VoipParamConditionFamily.FLAG);

        /**
         * The engine's predicate name for this condition.
         */
        private final String conditionName;

        /**
         * The evaluation family this condition belongs to.
         */
        private final VoipParamConditionFamily family;

        /**
         * Constructs a condition kind constant bound to its engine name and family.
         *
         * @param conditionName the engine predicate name
         * @param family        the evaluation family
         */
        VoipParamConditionKind(String conditionName, VoipParamConditionFamily family) {
            this.conditionName = conditionName;
            this.family = family;
        }

        /**
         * Returns the engine's predicate name for this condition.
         *
         * @return the predicate name, such as {@code "cond_range_ul_bwe"}
         */
        public String conditionName() {
            return conditionName;
        }

        /**
         * Returns the evaluation family this condition belongs to.
         *
         * @return the evaluation family
         */
        public VoipParamConditionFamily family() {
            return family;
        }
    }

    /**
     * Enumerates the evaluation families the {@code cond_*} predicates fall into.
     *
     * <p>The family determines the operand shape a condition carries and which
     * {@link VoipParamCondition} record represents it: a {@link #RANGE} condition carries an
     * inclusive lower and upper bound, a {@link #FLAG} condition carries an expected boolean,
     * a {@link #MASK} condition carries a constant mask, and a {@link #CODEC} condition
     * carries a constant codec selector.
     */
    enum VoipParamConditionFamily {
        /**
         * Conditions that test a runtime metric against an inclusive numeric range.
         */
        RANGE,

        /**
         * Conditions that test a boolean runtime state.
         */
        FLAG,

        /**
         * Conditions that test a runtime bitfield against a constant mask.
         */
        MASK,

        /**
         * Conditions that test the negotiated codec set.
         */
        CODEC
    }

    /**
     * Tests whether a runtime metric falls within an inclusive numeric range.
     *
     * <p>The condition holds when the metric named by the {@link #kind()} is at least
     * {@link #lowerInclusive()} and at most {@link #upperInclusive()}. The bounds are stored
     * as doubles so a single record covers both the integer valued metrics (bandwidth, size,
     * device class) and the fractional ones (packet loss percentage, round trip time).
     *
     * @param kind           the range predicate this condition represents
     * @param lowerInclusive the inclusive lower bound the metric must meet
     * @param upperInclusive the inclusive upper bound the metric must not exceed
     */
    record Range(VoipParamConditionKind kind, double lowerInclusive, double upperInclusive) implements VoipParamCondition {
        /**
         * Constructs a range condition, validating that its kind is in the range family and
         * that the bounds are ordered.
         *
         * @param kind           the range predicate this condition represents
         * @param lowerInclusive the inclusive lower bound the metric must meet
         * @param upperInclusive the inclusive upper bound the metric must not exceed
         * @throws IllegalArgumentException if {@code kind} is not a range predicate or the
         *                                  lower bound exceeds the upper bound
         */
        public Range {
            if (kind.family() != VoipParamConditionFamily.RANGE) {
                throw new IllegalArgumentException("not a range condition: " + kind);
            }
            if (lowerInclusive > upperInclusive) {
                throw new IllegalArgumentException("lower bound exceeds upper bound");
            }
        }

        /**
         * Returns whether the given metric value satisfies this range.
         *
         * @param value the runtime metric value to test
         * @return {@code true} if the value is within the inclusive bounds, {@code false}
         *         otherwise
         */
        public boolean test(double value) {
            return value >= lowerInclusive && value <= upperInclusive;
        }
    }

    /**
     * Tests a boolean runtime state against an expected value.
     *
     * <p>The condition holds when the runtime flag named by the {@link #kind()} equals
     * {@link #expected()}.
     *
     * @param kind     the flag predicate this condition represents
     * @param expected the boolean value the runtime flag must equal
     */
    record Flag(VoipParamConditionKind kind, boolean expected) implements VoipParamCondition {
        /**
         * Constructs a flag condition, validating that its kind is in the flag family.
         *
         * @param kind     the flag predicate this condition represents
         * @param expected the boolean value the runtime flag must equal
         * @throws IllegalArgumentException if {@code kind} is not a flag predicate
         */
        public Flag {
            if (kind.family() != VoipParamConditionFamily.FLAG) {
                throw new IllegalArgumentException("not a flag condition: " + kind);
            }
        }

        /**
         * Returns whether the given runtime flag satisfies this condition.
         *
         * @param value the runtime flag value to test
         * @return {@code true} if the value equals the expected value, {@code false}
         *         otherwise
         */
        public boolean test(boolean value) {
            return value == expected;
        }
    }

    /**
     * Tests a runtime bitfield against a constant mask.
     *
     * <p>The condition holds when the runtime bitfield named by the {@link #kind()} shares at
     * least one set bit with {@link #mask()}. Scalar equality predicates such as the
     * network medium and video quality mode tests are expressed as a single value mask whose
     * only set bit corresponds to the expected value.
     *
     * @param kind the mask predicate this condition represents
     * @param mask the constant mask the runtime bitfield is intersected with
     */
    record Mask(VoipParamConditionKind kind, long mask) implements VoipParamCondition {
        /**
         * Constructs a mask condition, validating that its kind is in the mask family.
         *
         * @param kind the mask predicate this condition represents
         * @param mask the constant mask the runtime bitfield is intersected with
         * @throws IllegalArgumentException if {@code kind} is not a mask predicate
         */
        public Mask {
            if (kind.family() != VoipParamConditionFamily.MASK) {
                throw new IllegalArgumentException("not a mask condition: " + kind);
            }
        }

        /**
         * Returns whether the given runtime bitfield intersects this mask.
         *
         * @param bitfield the runtime bitfield value to test
         * @return {@code true} if the bitfield shares a set bit with the mask, {@code false}
         *         otherwise
         */
        public boolean test(long bitfield) {
            return (bitfield & mask) != 0;
        }
    }

    /**
     * Tests the negotiated codec set against a constant codec selector.
     *
     * <p>The condition holds when the runtime codec set satisfies the codec predicate named
     * by the {@link #kind()} with respect to {@link #codecSelector()}; the selector is a
     * codec bitmask whose interpretation depends on the kind (an exact scheme or type match,
     * a contains any test, or a contains only test).
     *
     * @param kind          the codec predicate this condition represents
     * @param codecSelector the constant codec bitmask the runtime codec set is tested against
     */
    record Codec(VoipParamConditionKind kind, long codecSelector) implements VoipParamCondition {
        /**
         * Constructs a codec condition, validating that its kind is in the codec family.
         *
         * @param kind          the codec predicate this condition represents
         * @param codecSelector the constant codec bitmask the runtime codec set is tested against
         * @throws IllegalArgumentException if {@code kind} is not a codec predicate
         */
        public Codec {
            if (kind.family() != VoipParamConditionFamily.CODEC) {
                throw new IllegalArgumentException("not a codec condition: " + kind);
            }
        }
    }
}
