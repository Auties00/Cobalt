package com.github.auties00.cobalt.calls.config.param;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A voip-param key recovered from WhatsApp Web's {@code <voip_settings>} reads, identified by
 * its area-sectioned wire path.
 *
 * <p>The modelled keys are generated directly from the wa-voip WASM module as the public
 * constants of this record, one per distinct wire path. Each key carries its {@code section.key}
 * {@linkplain #wirePath() wire path} (the name the field carries in the JSON document,
 * {@code encode.use_mlow_codec_v1}) and the native descriptor {@linkplain #type() value type}
 * copied from the engine's descriptor write, not inferred from names or JSON values. The wire
 * path is recovered from the native param filler, where each descriptor registration is followed
 * by the JSON read that populates it; the section comes from the descriptor group pointer and the
 * field name from that read.
 *
 * <p>One engine struct field name read under several sections is several distinct wire fields
 * (the field behind {@code p-&gt;enable} is a different field under the {@code traffic_shaper},
 * {@code agc}, and {@code ns} sections); each is a separate constant under its own wire path, so
 * {@link #ofWirePath(String)} resolves every modelled leaf to one key. A leaf whose wire path is
 * not modelled is carried by an {@linkplain #unknown(String) unknown} key of type
 * {@link VoipParamType#UNKNOWN}, so the two together resolve every document leaf.
 *
 * <p>Being a record, a key has value equality over {@code (type, wirePath)}; the modelled
 * constants are singletons, and two unknown keys for the same path are equal, so a key is a sound
 * map key. The constants are flat public static fields read in a single class initializer, within
 * the JVM 64KB method-size limit for this key count.
 *
 * @param type     the native descriptor value type, or {@link VoipParamType#UNKNOWN} for an
 *                 unknown key
 * @param wirePath the area-sectioned {@code section.key} wire path that identifies this key
 */
public record VoipParamKey(VoipParamType type, String wirePath) {
    /**
     * The {@code aec.agc} voip-param.
     */
    public static final VoipParamKey AEC_AGC = new VoipParamKey(VoipParamType.INTEGER, "aec.agc");
    /**
     * The {@code aec.beryl_aec_latest_enable} voip-param.
     */
    public static final VoipParamKey AEC_BERYL_AEC_LATEST_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "aec.beryl_aec_latest_enable");
    /**
     * The {@code aec.beryl_aec_latest_enable_speaker} voip-param.
     */
    public static final VoipParamKey AEC_BERYL_AEC_LATEST_ENABLE_SPEAKER = new VoipParamKey(VoipParamType.INTEGER, "aec.beryl_aec_latest_enable_speaker");
    /**
     * The {@code aec.beryl_aec_prealloc_buffers} voip-param.
     */
    public static final VoipParamKey AEC_BERYL_AEC_PREALLOC_BUFFERS = new VoipParamKey(VoipParamType.INTEGER, "aec.beryl_aec_prealloc_buffers");
    /**
     * The {@code aec.beryl_opt_type} voip-param.
     */
    public static final VoipParamKey AEC_BERYL_OPT_TYPE = new VoipParamKey(VoipParamType.INTEGER, "aec.beryl_opt_type");
    /**
     * The {@code aec.disable_agc} voip-param.
     */
    public static final VoipParamKey AEC_DISABLE_AGC = new VoipParamKey(VoipParamType.INTEGER, "aec.disable_agc");
    /**
     * The {@code aec.ec_strength_threshold} voip-param.
     */
    public static final VoipParamKey AEC_EC_STRENGTH_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "aec.ec_strength_threshold");
    /**
     * The {@code aec.ec_threshold} voip-param.
     */
    public static final VoipParamKey AEC_EC_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "aec.ec_threshold");
    /**
     * The {@code aec.ec_threshold_sw_only} voip-param.
     */
    public static final VoipParamKey AEC_EC_THRESHOLD_SW_ONLY = new VoipParamKey(VoipParamType.INTEGER, "aec.ec_threshold_sw_only");
    /**
     * The {@code aec.echo_confidence_hist_enabled} voip-param.
     */
    public static final VoipParamKey AEC_ECHO_CONFIDENCE_HIST_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "aec.echo_confidence_hist_enabled");
    /**
     * The {@code aec.echo_sup_opt_fix} voip-param.
     */
    public static final VoipParamKey AEC_ECHO_SUP_OPT_FIX = new VoipParamKey(VoipParamType.INTEGER, "aec.echo_sup_opt_fix");
    /**
     * The {@code aec.enable_diff_ec_metrics} voip-param.
     */
    public static final VoipParamKey AEC_ENABLE_DIFF_EC_METRICS = new VoipParamKey(VoipParamType.INTEGER, "aec.enable_diff_ec_metrics");
    /**
     * The {@code aec.length} voip-param.
     */
    public static final VoipParamKey AEC_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "aec.length");
    /**
     * The {@code aec.metrics_compute_after_process} voip-param.
     */
    public static final VoipParamKey AEC_METRICS_COMPUTE_AFTER_PROCESS = new VoipParamKey(VoipParamType.INTEGER, "aec.metrics_compute_after_process");
    /**
     * The {@code aec.mode} voip-param.
     */
    public static final VoipParamKey AEC_MODE = new VoipParamKey(VoipParamType.INTEGER, "aec.mode");
    /**
     * The {@code aec.offset} voip-param.
     */
    public static final VoipParamKey AEC_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "aec.offset");
    /**
     * The {@code aec.run_aec_followed_by_ns} voip-param.
     */
    public static final VoipParamKey AEC_RUN_AEC_FOLLOWED_BY_NS = new VoipParamKey(VoipParamType.INTEGER, "aec.run_aec_followed_by_ns");
    /**
     * The {@code aec.skip_when_no_echo} voip-param.
     */
    public static final VoipParamKey AEC_SKIP_WHEN_NO_ECHO = new VoipParamKey(VoipParamType.INTEGER, "aec.skip_when_no_echo");
    /**
     * The {@code aec.speaker_mode} voip-param.
     */
    public static final VoipParamKey AEC_SPEAKER_MODE = new VoipParamKey(VoipParamType.INTEGER, "aec.speaker_mode");
    /**
     * The {@code aec.use_batch_mode} voip-param.
     */
    public static final VoipParamKey AEC_USE_BATCH_MODE = new VoipParamKey(VoipParamType.INTEGER, "aec.use_batch_mode");
    /**
     * The {@code aec.use_clean_capture} voip-param.
     */
    public static final VoipParamKey AEC_USE_CLEAN_CAPTURE = new VoipParamKey(VoipParamType.INTEGER, "aec.use_clean_capture");
    /**
     * The {@code aec.use_full_mode_beryl} voip-param.
     */
    public static final VoipParamKey AEC_USE_FULL_MODE_BERYL = new VoipParamKey(VoipParamType.INTEGER, "aec.use_full_mode_beryl");
    /**
     * The {@code aec.use_full_mode_beryl_speaker} voip-param.
     */
    public static final VoipParamKey AEC_USE_FULL_MODE_BERYL_SPEAKER = new VoipParamKey(VoipParamType.INTEGER, "aec.use_full_mode_beryl_speaker");
    /**
     * The {@code aec.use_route_based_beryl_options} voip-param.
     */
    public static final VoipParamKey AEC_USE_ROUTE_BASED_BERYL_OPTIONS = new VoipParamKey(VoipParamType.INTEGER, "aec.use_route_based_beryl_options");
    /**
     * The {@code aec.xra_echo_metrics_enabled} voip-param.
     */
    public static final VoipParamKey AEC_XRA_ECHO_METRICS_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "aec.xra_echo_metrics_enabled");
    /**
     * The {@code aec.xra_pre_echo_metrics_enabled} voip-param.
     */
    public static final VoipParamKey AEC_XRA_PRE_ECHO_METRICS_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "aec.xra_pre_echo_metrics_enabled");
    /**
     * The {@code agc.adaptive_leveler_ec_thres} voip-param.
     */
    public static final VoipParamKey AGC_ADAPTIVE_LEVELER_EC_THRES = new VoipParamKey(VoipParamType.INTEGER, "agc.adaptive_leveler_ec_thres");
    /**
     * The {@code agc.adaptive_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_ADAPTIVE_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.adaptive_leveler_mode");
    /**
     * The {@code agc.adaptive_leveler_mode_hi_low} voip-param.
     */
    public static final VoipParamKey AGC_ADAPTIVE_LEVELER_MODE_HI_LOW = new VoipParamKey(VoipParamType.INTEGER, "agc.adaptive_leveler_mode_hi_low");
    /**
     * The {@code agc.adaptive_leveler_mode_min_intensity} voip-param.
     */
    public static final VoipParamKey AGC_ADAPTIVE_LEVELER_MODE_MIN_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.adaptive_leveler_mode_min_intensity");
    /**
     * The {@code agc.adaptive_leveler_mode_rev} voip-param.
     */
    public static final VoipParamKey AGC_ADAPTIVE_LEVELER_MODE_REV = new VoipParamKey(VoipParamType.INTEGER, "agc.adaptive_leveler_mode_rev");
    /**
     * The {@code agc.agc_serialize_farend_on_capture} voip-param.
     */
    public static final VoipParamKey AGC_AGC_SERIALIZE_FAREND_ON_CAPTURE = new VoipParamKey(VoipParamType.INTEGER, "agc.agc_serialize_farend_on_capture");
    /**
     * The {@code agc.agc_use_webrtc_m89} voip-param.
     */
    public static final VoipParamKey AGC_AGC_USE_WEBRTC_M89 = new VoipParamKey(VoipParamType.INTEGER, "agc.agc_use_webrtc_m89");
    /**
     * The {@code agc.agc2_adaptive_digital_mode} voip-param.
     */
    public static final VoipParamKey AGC_AGC2_ADAPTIVE_DIGITAL_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.agc2_adaptive_digital_mode");
    /**
     * The {@code agc.bipolar_compression_en} voip-param.
     */
    public static final VoipParamKey AGC_BIPOLAR_COMPRESSION_EN = new VoipParamKey(VoipParamType.INTEGER, "agc.bipolar_compression_en");
    /**
     * The {@code agc.bt_adaptive_leveler_ec_thres} voip-param.
     */
    public static final VoipParamKey AGC_BT_ADAPTIVE_LEVELER_EC_THRES = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_adaptive_leveler_ec_thres");
    /**
     * The {@code agc.bt_adaptive_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_BT_ADAPTIVE_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_adaptive_leveler_mode");
    /**
     * The {@code agc.bt_adaptive_leveler_mode_hi_low} voip-param.
     */
    public static final VoipParamKey AGC_BT_ADAPTIVE_LEVELER_MODE_HI_LOW = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_adaptive_leveler_mode_hi_low");
    /**
     * The {@code agc.bt_adaptive_leveler_mode_min_intensity} voip-param.
     */
    public static final VoipParamKey AGC_BT_ADAPTIVE_LEVELER_MODE_MIN_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.bt_adaptive_leveler_mode_min_intensity");
    /**
     * The {@code agc.bt_adaptive_leveler_mode_rev} voip-param.
     */
    public static final VoipParamKey AGC_BT_ADAPTIVE_LEVELER_MODE_REV = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_adaptive_leveler_mode_rev");
    /**
     * The {@code agc.bt_bipolar_compression_en} voip-param.
     */
    public static final VoipParamKey AGC_BT_BIPOLAR_COMPRESSION_EN = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_bipolar_compression_en");
    /**
     * The {@code agc.bt_leveler_intensity} voip-param.
     */
    public static final VoipParamKey AGC_BT_LEVELER_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.bt_leveler_intensity");
    /**
     * The {@code agc.bt_min_wait_frames_transitions} voip-param.
     */
    public static final VoipParamKey AGC_BT_MIN_WAIT_FRAMES_TRANSITIONS = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_min_wait_frames_transitions");
    /**
     * The {@code agc.bt_smooth_leveler_mode_factor} voip-param.
     */
    public static final VoipParamKey AGC_BT_SMOOTH_LEVELER_MODE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_smooth_leveler_mode_factor");
    /**
     * The {@code agc.bt_use_smooth_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_BT_USE_SMOOTH_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.bt_use_smooth_leveler_mode");
    /**
     * The {@code agc.compressiongain} voip-param.
     */
    public static final VoipParamKey AGC_COMPRESSIONGAIN = new VoipParamKey(VoipParamType.INTEGER, "agc.compressiongain");
    /**
     * The {@code agc.enable} voip-param.
     */
    public static final VoipParamKey AGC_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "agc.enable");
    /**
     * The {@code agc.enable_rx_lufs_metric} voip-param.
     */
    public static final VoipParamKey AGC_ENABLE_RX_LUFS_METRIC = new VoipParamKey(VoipParamType.INTEGER, "agc.enable_rx_lufs_metric");
    /**
     * The {@code agc.enable_tx_lufs_metric} voip-param.
     */
    public static final VoipParamKey AGC_ENABLE_TX_LUFS_METRIC = new VoipParamKey(VoipParamType.INTEGER, "agc.enable_tx_lufs_metric");
    /**
     * The {@code agc.enable_uwp_agc2} voip-param.
     */
    public static final VoipParamKey AGC_ENABLE_UWP_AGC2 = new VoipParamKey(VoipParamType.INTEGER, "agc.enable_uwp_agc2");
    /**
     * The {@code agc.hs_adaptive_leveler_ec_thres} voip-param.
     */
    public static final VoipParamKey AGC_HS_ADAPTIVE_LEVELER_EC_THRES = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_adaptive_leveler_ec_thres");
    /**
     * The {@code agc.hs_adaptive_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_HS_ADAPTIVE_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_adaptive_leveler_mode");
    /**
     * The {@code agc.hs_adaptive_leveler_mode_hi_low} voip-param.
     */
    public static final VoipParamKey AGC_HS_ADAPTIVE_LEVELER_MODE_HI_LOW = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_adaptive_leveler_mode_hi_low");
    /**
     * The {@code agc.hs_adaptive_leveler_mode_min_intensity} voip-param.
     */
    public static final VoipParamKey AGC_HS_ADAPTIVE_LEVELER_MODE_MIN_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.hs_adaptive_leveler_mode_min_intensity");
    /**
     * The {@code agc.hs_adaptive_leveler_mode_rev} voip-param.
     */
    public static final VoipParamKey AGC_HS_ADAPTIVE_LEVELER_MODE_REV = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_adaptive_leveler_mode_rev");
    /**
     * The {@code agc.hs_bipolar_compression_en} voip-param.
     */
    public static final VoipParamKey AGC_HS_BIPOLAR_COMPRESSION_EN = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_bipolar_compression_en");
    /**
     * The {@code agc.hs_leveler_intensity} voip-param.
     */
    public static final VoipParamKey AGC_HS_LEVELER_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.hs_leveler_intensity");
    /**
     * The {@code agc.hs_min_wait_frames_transitions} voip-param.
     */
    public static final VoipParamKey AGC_HS_MIN_WAIT_FRAMES_TRANSITIONS = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_min_wait_frames_transitions");
    /**
     * The {@code agc.hs_smooth_leveler_mode_factor} voip-param.
     */
    public static final VoipParamKey AGC_HS_SMOOTH_LEVELER_MODE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_smooth_leveler_mode_factor");
    /**
     * The {@code agc.hs_use_smooth_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_HS_USE_SMOOTH_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.hs_use_smooth_leveler_mode");
    /**
     * The {@code agc.leveler_decimation_step_size} voip-param.
     */
    public static final VoipParamKey AGC_LEVELER_DECIMATION_STEP_SIZE = new VoipParamKey(VoipParamType.INTEGER, "agc.leveler_decimation_step_size");
    /**
     * The {@code agc.leveler_intensity} voip-param.
     */
    public static final VoipParamKey AGC_LEVELER_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.leveler_intensity");
    /**
     * The {@code agc.leveler_intensity_echo} voip-param.
     */
    public static final VoipParamKey AGC_LEVELER_INTENSITY_ECHO = new VoipParamKey(VoipParamType.FLOAT, "agc.leveler_intensity_echo");
    /**
     * The {@code agc.leveler_process_20ms_samples} voip-param.
     */
    public static final VoipParamKey AGC_LEVELER_PROCESS_20MS_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "agc.leveler_process_20ms_samples");
    /**
     * The {@code agc.leveler_smooth_factor} voip-param.
     */
    public static final VoipParamKey AGC_LEVELER_SMOOTH_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "agc.leveler_smooth_factor");
    /**
     * The {@code agc.limiterenable} voip-param.
     */
    public static final VoipParamKey AGC_LIMITERENABLE = new VoipParamKey(VoipParamType.INTEGER, "agc.limiterenable");
    /**
     * The {@code agc.metrics_compute_after_agc} voip-param.
     */
    public static final VoipParamKey AGC_METRICS_COMPUTE_AFTER_AGC = new VoipParamKey(VoipParamType.INTEGER, "agc.metrics_compute_after_agc");
    /**
     * The {@code agc.min_wait_frames_transitions} voip-param.
     */
    public static final VoipParamKey AGC_MIN_WAIT_FRAMES_TRANSITIONS = new VoipParamKey(VoipParamType.INTEGER, "agc.min_wait_frames_transitions");
    /**
     * The {@code agc.mode} voip-param.
     */
    public static final VoipParamKey AGC_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.mode");
    /**
     * The {@code agc.run_agc_first} voip-param.
     */
    public static final VoipParamKey AGC_RUN_AGC_FIRST = new VoipParamKey(VoipParamType.INTEGER, "agc.run_agc_first");
    /**
     * The {@code agc.run_rx_agc} voip-param.
     */
    public static final VoipParamKey AGC_RUN_RX_AGC = new VoipParamKey(VoipParamType.INTEGER, "agc.run_rx_agc");
    /**
     * The {@code agc.smooth_leveler_mode_factor} voip-param.
     */
    public static final VoipParamKey AGC_SMOOTH_LEVELER_MODE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "agc.smooth_leveler_mode_factor");
    /**
     * The {@code agc.spkr_adaptive_leveler_ec_thres} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_ADAPTIVE_LEVELER_EC_THRES = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_adaptive_leveler_ec_thres");
    /**
     * The {@code agc.spkr_adaptive_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_ADAPTIVE_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_adaptive_leveler_mode");
    /**
     * The {@code agc.spkr_adaptive_leveler_mode_hi_low} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_ADAPTIVE_LEVELER_MODE_HI_LOW = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_adaptive_leveler_mode_hi_low");
    /**
     * The {@code agc.spkr_adaptive_leveler_mode_min_intensity} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_ADAPTIVE_LEVELER_MODE_MIN_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.spkr_adaptive_leveler_mode_min_intensity");
    /**
     * The {@code agc.spkr_adaptive_leveler_mode_rev} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_ADAPTIVE_LEVELER_MODE_REV = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_adaptive_leveler_mode_rev");
    /**
     * The {@code agc.spkr_bipolar_compression_en} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_BIPOLAR_COMPRESSION_EN = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_bipolar_compression_en");
    /**
     * The {@code agc.spkr_leveler_intensity} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_LEVELER_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "agc.spkr_leveler_intensity");
    /**
     * The {@code agc.spkr_min_wait_frames_transitions} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_MIN_WAIT_FRAMES_TRANSITIONS = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_min_wait_frames_transitions");
    /**
     * The {@code agc.spkr_smooth_leveler_mode_factor} voip-param.
     */
    public static final VoipParamKey AGC_SPKR_SMOOTH_LEVELER_MODE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "agc.spkr_smooth_leveler_mode_factor");
    /**
     * The {@code agc.strength_threshold} voip-param.
     */
    public static final VoipParamKey AGC_STRENGTH_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "agc.strength_threshold");
    /**
     * The {@code agc.targetlevel} voip-param.
     */
    public static final VoipParamKey AGC_TARGETLEVEL = new VoipParamKey(VoipParamType.INTEGER, "agc.targetlevel");
    /**
     * The {@code agc.use_avg_echo_likelihood_for_agc} voip-param.
     */
    public static final VoipParamKey AGC_USE_AVG_ECHO_LIKELIHOOD_FOR_AGC = new VoipParamKey(VoipParamType.INTEGER, "agc.use_avg_echo_likelihood_for_agc");
    /**
     * The {@code agc.use_bt_mode_leveler_config} voip-param.
     */
    public static final VoipParamKey AGC_USE_BT_MODE_LEVELER_CONFIG = new VoipParamKey(VoipParamType.INTEGER, "agc.use_bt_mode_leveler_config");
    /**
     * The {@code agc.use_hs_mode_leveler_config} voip-param.
     */
    public static final VoipParamKey AGC_USE_HS_MODE_LEVELER_CONFIG = new VoipParamKey(VoipParamType.INTEGER, "agc.use_hs_mode_leveler_config");
    /**
     * The {@code agc.use_legacy_agc_preprocess} voip-param.
     */
    public static final VoipParamKey AGC_USE_LEGACY_AGC_PREPROCESS = new VoipParamKey(VoipParamType.INTEGER, "agc.use_legacy_agc_preprocess");
    /**
     * The {@code agc.use_leveler} voip-param.
     */
    public static final VoipParamKey AGC_USE_LEVELER = new VoipParamKey(VoipParamType.INTEGER, "agc.use_leveler");
    /**
     * The {@code agc.use_leveler_bg_noise} voip-param.
     */
    public static final VoipParamKey AGC_USE_LEVELER_BG_NOISE = new VoipParamKey(VoipParamType.INTEGER, "agc.use_leveler_bg_noise");
    /**
     * The {@code agc.use_leveler_init_opt} voip-param.
     */
    public static final VoipParamKey AGC_USE_LEVELER_INIT_OPT = new VoipParamKey(VoipParamType.INTEGER, "agc.use_leveler_init_opt");
    /**
     * The {@code agc.use_low_int_leveler_on_echo} voip-param.
     */
    public static final VoipParamKey AGC_USE_LOW_INT_LEVELER_ON_ECHO = new VoipParamKey(VoipParamType.INTEGER, "agc.use_low_int_leveler_on_echo");
    /**
     * The {@code agc.use_max_echo_likelihood_for_agc} voip-param.
     */
    public static final VoipParamKey AGC_USE_MAX_ECHO_LIKELIHOOD_FOR_AGC = new VoipParamKey(VoipParamType.INTEGER, "agc.use_max_echo_likelihood_for_agc");
    /**
     * The {@code agc.use_smooth_leveler_mode} voip-param.
     */
    public static final VoipParamKey AGC_USE_SMOOTH_LEVELER_MODE = new VoipParamKey(VoipParamType.INTEGER, "agc.use_smooth_leveler_mode");
    /**
     * The {@code agc.use_spkr_mode_leveler_config} voip-param.
     */
    public static final VoipParamKey AGC_USE_SPKR_MODE_LEVELER_CONFIG = new VoipParamKey(VoipParamType.INTEGER, "agc.use_spkr_mode_leveler_config");
    /**
     * The {@code agc.use_tf_leveler} voip-param.
     */
    public static final VoipParamKey AGC_USE_TF_LEVELER = new VoipParamKey(VoipParamType.INTEGER, "agc.use_tf_leveler");
    /**
     * The {@code bwa_rc.audio_bitrate_reserve} voip-param.
     */
    public static final VoipParamKey BWA_RC_AUDIO_BITRATE_RESERVE = new VoipParamKey(VoipParamType.INTEGER, "bwa_rc.audio_bitrate_reserve");
    /**
     * The {@code bwa_rc.cond_is_speaker} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_IS_SPEAKER = new VoipParamKey(VoipParamType.INTEGER, "bwa_rc.cond_is_speaker");
    /**
     * The {@code bwa_rc.cond_range_dl_bwe} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_DL_BWE = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_dl_bwe");
    /**
     * The {@code bwa_rc.cond_range_ema_downlink_plr} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_EMA_DOWNLINK_PLR = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_ema_downlink_plr");
    /**
     * The {@code bwa_rc.cond_range_ema_uplink_plr} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_EMA_UPLINK_PLR = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_ema_uplink_plr");
    /**
     * The {@code bwa_rc.cond_range_gcall_size} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_GCALL_SIZE = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_gcall_size");
    /**
     * The {@code bwa_rc.cond_range_sec_since_sfu_simulcast_capable} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_SEC_SINCE_SFU_SIMULCAST_CAPABLE = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_sec_since_sfu_simulcast_capable");
    /**
     * The {@code bwa_rc.cond_range_ul_bwe} voip-param.
     */
    public static final VoipParamKey BWA_RC_COND_RANGE_UL_BWE = new VoipParamKey(VoipParamType.ARRAY, "bwa_rc.cond_range_ul_bwe");
    /**
     * The {@code bwe.action_on_rtp_marker} voip-param.
     */
    public static final VoipParamKey BWE_ACTION_ON_RTP_MARKER = new VoipParamKey(VoipParamType.INTEGER, "bwe.action_on_rtp_marker");
    /**
     * The {@code bwe.bwe_sampling_rate} voip-param.
     */
    public static final VoipParamKey BWE_BWE_SAMPLING_RATE = new VoipParamKey(VoipParamType.INTEGER, "bwe.bwe_sampling_rate");
    /**
     * The {@code bwe.delay_based_bwe_aimd_adpt_thresh_exp} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_ADPT_THRESH_EXP = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_adpt_thresh_exp");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_enable} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_enable");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_low_bitrate_pct} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_LOW_BITRATE_PCT = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_low_bitrate_pct");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_max_delay_ms} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_MAX_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_max_delay_ms");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_min_bitrate_kbps} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_MIN_BITRATE_KBPS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_min_bitrate_kbps");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_rtt_threshold_ms} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_RTT_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_rtt_threshold_ms");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_bw_cap_throughput_pct} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_BW_CAP_THROUGHPUT_PCT = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_bw_cap_throughput_pct");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_max_estimate_pct} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_MAX_ESTIMATE_PCT = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_max_estimate_pct");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_min_bitrate_for_pp_mode_kbps} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_MIN_BITRATE_FOR_PP_MODE_KBPS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_min_bitrate_for_pp_mode_kbps");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_multi_slow_increase_pct} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_MULTI_SLOW_INCREASE_PCT = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_multi_slow_increase_pct");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_use_pp_aimd_rate_controller_bwe} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_USE_PP_AIMD_RATE_CONTROLLER_BWE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_use_pp_aimd_rate_controller_bwe");
    /**
     * The {@code bwe.delay_based_bwe_aimd_rp_version_of_pp_bwe} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AIMD_RP_VERSION_OF_PP_BWE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_aimd_rp_version_of_pp_bwe");
    /**
     * The {@code bwe.delay_based_bwe_audio_only_force_additive_increase} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AUDIO_ONLY_FORCE_ADDITIVE_INCREASE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_audio_only_force_additive_increase");
    /**
     * The {@code bwe.delay_based_bwe_audio_remb_clamp_bps} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_AUDIO_REMB_CLAMP_BPS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_audio_remb_clamp_bps");
    /**
     * The {@code bwe.delay_based_bwe_bitrate_estimator_enabled} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_BITRATE_ESTIMATOR_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_bitrate_estimator_enabled");
    /**
     * The {@code bwe.delay_based_bwe_br_est_scale_small} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_BR_EST_SCALE_SMALL = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_br_est_scale_small");
    /**
     * The {@code bwe.delay_based_bwe_br_est_small_thresh} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_BR_EST_SMALL_THRESH = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_br_est_small_thresh");
    /**
     * The {@code bwe.delay_based_bwe_br_est_window_ms} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_BR_EST_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_br_est_window_ms");
    /**
     * The {@code bwe.delay_based_bwe_clamp_estimate_by_max_bitrate} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_CLAMP_ESTIMATE_BY_MAX_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_clamp_estimate_by_max_bitrate");
    /**
     * The {@code bwe.delay_based_bwe_enable_aimd_rate_control_rp} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_ENABLE_AIMD_RATE_CONTROL_RP = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_enable_aimd_rate_control_rp");
    /**
     * The {@code bwe.delay_based_bwe_enable_initial_overuse_detection} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_ENABLE_INITIAL_OVERUSE_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_enable_initial_overuse_detection");
    /**
     * The {@code bwe.delay_based_bwe_enable_separate_audio} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_ENABLE_SEPARATE_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_enable_separate_audio");
    /**
     * The {@code bwe.delay_based_bwe_inter_arrival_use_burst_duration} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_INTER_ARRIVAL_USE_BURST_DURATION = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_inter_arrival_use_burst_duration");
    /**
     * The {@code bwe.delay_based_bwe_pp_feed_type} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_PP_FEED_TYPE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_pp_feed_type");
    /**
     * The {@code bwe.delay_based_bwe_trendline_est_k_down} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_EST_K_DOWN = new VoipParamKey(VoipParamType.FLOAT, "bwe.delay_based_bwe_trendline_est_k_down");
    /**
     * The {@code bwe.delay_based_bwe_trendline_est_k_up} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_EST_K_UP = new VoipParamKey(VoipParamType.FLOAT, "bwe.delay_based_bwe_trendline_est_k_up");
    /**
     * The {@code bwe.delay_based_bwe_trendline_est_threshold} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_EST_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "bwe.delay_based_bwe_trendline_est_threshold");
    /**
     * The {@code bwe.delay_based_bwe_trendline_filter_enabled} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_FILTER_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_trendline_filter_enabled");
    /**
     * The {@code bwe.delay_based_bwe_trendline_overusing_time_threshold} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_OVERUSING_TIME_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_trendline_overusing_time_threshold");
    /**
     * The {@code bwe.delay_based_bwe_trendline_smoothing_coef} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_SMOOTHING_COEF = new VoipParamKey(VoipParamType.FLOAT, "bwe.delay_based_bwe_trendline_smoothing_coef");
    /**
     * The {@code bwe.delay_based_bwe_trendline_window_size} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_TRENDLINE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_trendline_window_size");
    /**
     * The {@code bwe.delay_based_bwe_use_pp} voip-param.
     */
    public static final VoipParamKey BWE_DELAY_BASED_BWE_USE_PP = new VoipParamKey(VoipParamType.INTEGER, "bwe.delay_based_bwe_use_pp");
    /**
     * The {@code bwe.enable_audio_sender_bwe} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_AUDIO_SENDER_BWE = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_audio_sender_bwe");
    /**
     * The {@code bwe.enable_deduct_from_vid_stream} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_DEDUCT_FROM_VID_STREAM = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_deduct_from_vid_stream");
    /**
     * The {@code bwe.enable_max_bwe_fix} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_MAX_BWE_FIX = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_max_bwe_fix");
    /**
     * The {@code bwe.enable_oob_deduction} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_OOB_DEDUCTION = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_oob_deduction");
    /**
     * The {@code bwe.enable_short_offset_build} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_SHORT_OFFSET_BUILD = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_short_offset_build");
    /**
     * The {@code bwe.enable_transport_seq_num_ext} voip-param.
     */
    public static final VoipParamKey BWE_ENABLE_TRANSPORT_SEQ_NUM_EXT = new VoipParamKey(VoipParamType.INTEGER, "bwe.enable_transport_seq_num_ext");
    /**
     * The {@code bwe.fast_remb_delayed_start} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_DELAYED_START = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_delayed_start");
    /**
     * The {@code bwe.fast_remb_in_dwgrd_1x1} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_IN_DWGRD_1X1 = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_in_dwgrd_1x1");
    /**
     * The {@code bwe.fast_remb_in_video_rtp} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_IN_VIDEO_RTP = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_in_video_rtp");
    /**
     * The {@code bwe.fast_remb_send_interval_ms} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_SEND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_send_interval_ms");
    /**
     * The {@code bwe.fast_remb_start_delay_ms} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_START_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_start_delay_ms");
    /**
     * The {@code bwe.fast_remb_use_fast_rr_parser} voip-param.
     */
    public static final VoipParamKey BWE_FAST_REMB_USE_FAST_RR_PARSER = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_remb_use_fast_rr_parser");
    /**
     * The {@code bwe.fast_rr_compute_loss_using_sr} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_COMPUTE_LOSS_USING_SR = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_compute_loss_using_sr");
    /**
     * The {@code bwe.fast_rr_min_send_interval_ms} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_MIN_SEND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_min_send_interval_ms");
    /**
     * The {@code bwe.fast_rr_plr_src} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_PLR_SRC = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_plr_src");
    /**
     * The {@code bwe.fast_rr_rtt_src} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_RTT_SRC = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_rtt_src");
    /**
     * The {@code bwe.fast_rr_send_on_nack} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_SEND_ON_NACK = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_send_on_nack");
    /**
     * The {@code bwe.fast_rr_start_delay_ms} voip-param.
     */
    public static final VoipParamKey BWE_FAST_RR_START_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "bwe.fast_rr_start_delay_ms");
    /**
     * The {@code bwe.force_sbwe_follow_encoder} voip-param.
     */
    public static final VoipParamKey BWE_FORCE_SBWE_FOLLOW_ENCODER = new VoipParamKey(VoipParamType.INTEGER, "bwe.force_sbwe_follow_encoder");
    /**
     * The {@code bwe.max_unknown_on_rate_increase} voip-param.
     */
    public static final VoipParamKey BWE_MAX_UNKNOWN_ON_RATE_INCREASE = new VoipParamKey(VoipParamType.INTEGER, "bwe.max_unknown_on_rate_increase");
    /**
     * The {@code bwe.override_fec_ssrc_with_rtp_ssrc} voip-param.
     */
    public static final VoipParamKey BWE_OVERRIDE_FEC_SSRC_WITH_RTP_SSRC = new VoipParamKey(VoipParamType.INTEGER, "bwe.override_fec_ssrc_with_rtp_ssrc");
    /**
     * The {@code bwe.parallel_remb} voip-param.
     */
    public static final VoipParamKey BWE_PARALLEL_REMB = new VoipParamKey(VoipParamType.INTEGER, "bwe.parallel_remb");
    /**
     * The {@code bwe.reset_oud_timestamp_on_bwe_reset} voip-param.
     */
    public static final VoipParamKey BWE_RESET_OUD_TIMESTAMP_ON_BWE_RESET = new VoipParamKey(VoipParamType.INTEGER, "bwe.reset_oud_timestamp_on_bwe_reset");
    /**
     * The {@code bwe.reset_rcc_on_bwe_reset} voip-param.
     */
    public static final VoipParamKey BWE_RESET_RCC_ON_BWE_RESET = new VoipParamKey(VoipParamType.INTEGER, "bwe.reset_rcc_on_bwe_reset");
    /**
     * The {@code bwe.run_bwe_on_sender_side} voip-param.
     */
    public static final VoipParamKey BWE_RUN_BWE_ON_SENDER_SIDE = new VoipParamKey(VoipParamType.INTEGER, "bwe.run_bwe_on_sender_side");
    /**
     * The {@code bwe.sbwe_abs_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_ABS_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_abs_rtt_congestion_threshold");
    /**
     * The {@code bwe.sbwe_combine_policy} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_COMBINE_POLICY = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_combine_policy");
    /**
     * The {@code bwe.sbwe_loss_high} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_LOSS_HIGH = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_loss_high");
    /**
     * The {@code bwe.sbwe_loss_low} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_LOSS_LOW = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_loss_low");
    /**
     * The {@code bwe.sbwe_max_target_bitrate} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_MAX_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_max_target_bitrate");
    /**
     * The {@code bwe.sbwe_min_target_bitrate} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_MIN_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_min_target_bitrate");
    /**
     * The {@code bwe.sbwe_nonzero_rtt_count_thr} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_NONZERO_RTT_COUNT_THR = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_nonzero_rtt_count_thr");
    /**
     * The {@code bwe.sbwe_rd_target_lower_multiplier} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_RD_TARGET_LOWER_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "bwe.sbwe_rd_target_lower_multiplier");
    /**
     * The {@code bwe.sbwe_receive_side_drop_pct} voip-param.
     */
    public static final VoipParamKey BWE_SBWE_RECEIVE_SIDE_DROP_PCT = new VoipParamKey(VoipParamType.INTEGER, "bwe.sbwe_receive_side_drop_pct");
    /**
     * The {@code bwe.short_offset_precise} voip-param.
     */
    public static final VoipParamKey BWE_SHORT_OFFSET_PRECISE = new VoipParamKey(VoipParamType.INTEGER, "bwe.short_offset_precise");
    /**
     * The {@code bwe.start_remb_with_init_bwe} voip-param.
     */
    public static final VoipParamKey BWE_START_REMB_WITH_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "bwe.start_remb_with_init_bwe");
    /**
     * The {@code bwe.start_remb_with_init_bwe_multiplier} voip-param.
     */
    public static final VoipParamKey BWE_START_REMB_WITH_INIT_BWE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "bwe.start_remb_with_init_bwe_multiplier");
    /**
     * The {@code decode.allow_dynamic_frame_size} voip-param.
     */
    public static final VoipParamKey DECODE_ALLOW_DYNAMIC_FRAME_SIZE = new VoipParamKey(VoipParamType.INTEGER, "decode.allow_dynamic_frame_size");
    /**
     * The {@code decode.fec} voip-param.
     */
    public static final VoipParamKey DECODE_FEC = new VoipParamKey(VoipParamType.INTEGER, "decode.fec");
    /**
     * The {@code decode.gain} voip-param.
     */
    public static final VoipParamKey DECODE_GAIN = new VoipParamKey(VoipParamType.INTEGER, "decode.gain");
    /**
     * The {@code decode.mlow_dec_cutoff_hz} voip-param.
     */
    public static final VoipParamKey DECODE_MLOW_DEC_CUTOFF_HZ = new VoipParamKey(VoipParamType.INTEGER, "decode.mlow_dec_cutoff_hz");
    /**
     * The {@code decode.mlow_post_filter} voip-param.
     */
    public static final VoipParamKey DECODE_MLOW_POST_FILTER = new VoipParamKey(VoipParamType.INTEGER, "decode.mlow_post_filter");
    /**
     * The {@code decode.neteq_codec_settings_fix} voip-param.
     */
    public static final VoipParamKey DECODE_NETEQ_CODEC_SETTINGS_FIX = new VoipParamKey(VoipParamType.INTEGER, "decode.neteq_codec_settings_fix");
    /**
     * The {@code decode.plc} voip-param.
     */
    public static final VoipParamKey DECODE_PLC = new VoipParamKey(VoipParamType.INTEGER, "decode.plc");
    /**
     * The {@code encode.bitrate} voip-param.
     */
    public static final VoipParamKey ENCODE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "encode.bitrate");
    /**
     * The {@code encode.cbr} voip-param.
     */
    public static final VoipParamKey ENCODE_CBR = new VoipParamKey(VoipParamType.INTEGER, "encode.cbr");
    /**
     * The {@code encode.complexity} voip-param.
     */
    public static final VoipParamKey ENCODE_COMPLEXITY = new VoipParamKey(VoipParamType.INTEGER, "encode.complexity");
    /**
     * The {@code encode.enable_encode_with_secondary} voip-param.
     */
    public static final VoipParamKey ENCODE_ENABLE_ENCODE_WITH_SECONDARY = new VoipParamKey(VoipParamType.INTEGER, "encode.enable_encode_with_secondary");
    /**
     * The {@code encode.fec} voip-param.
     */
    public static final VoipParamKey ENCODE_FEC = new VoipParamKey(VoipParamType.INTEGER, "encode.fec");
    /**
     * The {@code encode.max_cpu} voip-param.
     */
    public static final VoipParamKey ENCODE_MAX_CPU = new VoipParamKey(VoipParamType.INTEGER, "encode.max_cpu");
    /**
     * The {@code encode.max_frames_per_packet} voip-param.
     */
    public static final VoipParamKey ENCODE_MAX_FRAMES_PER_PACKET = new VoipParamKey(VoipParamType.INTEGER, "encode.max_frames_per_packet");
    /**
     * The {@code encode.min_bitrate} voip-param.
     */
    public static final VoipParamKey ENCODE_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "encode.min_bitrate");
    /**
     * The {@code encode.mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey ENCODE_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "encode.mlow_dtx_hangover_ms");
    /**
     * The {@code encode.mlow_reuse_hb_data} voip-param.
     */
    public static final VoipParamKey ENCODE_MLOW_REUSE_HB_DATA = new VoipParamKey(VoipParamType.INTEGER, "encode.mlow_reuse_hb_data");
    /**
     * The {@code encode.mlow_vad_hp_sharpness} voip-param.
     */
    public static final VoipParamKey ENCODE_MLOW_VAD_HP_SHARPNESS = new VoipParamKey(VoipParamType.INTEGER, "encode.mlow_vad_hp_sharpness");
    /**
     * The {@code encode.mlow_vad_non_binary} voip-param.
     */
    public static final VoipParamKey ENCODE_MLOW_VAD_NON_BINARY = new VoipParamKey(VoipParamType.INTEGER, "encode.mlow_vad_non_binary");
    /**
     * The {@code encode.mtu_aware} voip-param.
     */
    public static final VoipParamKey ENCODE_MTU_AWARE = new VoipParamKey(VoipParamType.INTEGER, "encode.mtu_aware");
    /**
     * The {@code encode.nack} voip-param.
     */
    public static final VoipParamKey ENCODE_NACK = new VoipParamKey(VoipParamType.INTEGER, "encode.nack");
    /**
     * The {@code encode.opus_version} voip-param.
     */
    public static final VoipParamKey ENCODE_OPUS_VERSION = new VoipParamKey(VoipParamType.INTEGER, "encode.opus_version");
    /**
     * The {@code encode.sampling_rate} voip-param.
     */
    public static final VoipParamKey ENCODE_SAMPLING_RATE = new VoipParamKey(VoipParamType.INTEGER, "encode.sampling_rate");
    /**
     * The {@code encode.use_mlow_codec_v1} voip-param.
     */
    public static final VoipParamKey ENCODE_USE_MLOW_CODEC_V1 = new VoipParamKey(VoipParamType.INTEGER, "encode.use_mlow_codec_v1");
    /**
     * The {@code fs.allowed_column_ids_csv} voip-param.
     */
    public static final VoipParamKey FS_ALLOWED_COLUMN_IDS_CSV = new VoipParamKey(VoipParamType.STRING, "fs.allowed_column_ids_csv");
    /**
     * The {@code fs.enable_peer_abtest_direct_ptr} voip-param.
     */
    public static final VoipParamKey FS_ENABLE_PEER_ABTEST_DIRECT_PTR = new VoipParamKey(VoipParamType.INTEGER, "fs.enable_peer_abtest_direct_ptr");
    /**
     * The {@code fs.enable_video_enabled_in_peer_row} voip-param.
     */
    public static final VoipParamKey FS_ENABLE_VIDEO_ENABLED_IN_PEER_ROW = new VoipParamKey(VoipParamType.INTEGER, "fs.enable_video_enabled_in_peer_row");
    /**
     * The {@code fs.exclude_lobby_from_link_callee_setup_t} voip-param.
     */
    public static final VoipParamKey FS_EXCLUDE_LOBBY_FROM_LINK_CALLEE_SETUP_T = new VoipParamKey(VoipParamType.INTEGER, "fs.exclude_lobby_from_link_callee_setup_t");
    /**
     * The {@code fs.exclude_offer_processing_from_setup_time_when_not_critical} voip-param.
     */
    public static final VoipParamKey FS_EXCLUDE_OFFER_PROCESSING_FROM_SETUP_TIME_WHEN_NOT_CRITICAL = new VoipParamKey(VoipParamType.INTEGER, "fs.exclude_offer_processing_from_setup_time_when_not_critical");
    /**
     * The {@code fs.fs_fix_reset_accumulated_stats} voip-param.
     */
    public static final VoipParamKey FS_FS_FIX_RESET_ACCUMULATED_STATS = new VoipParamKey(VoipParamType.INTEGER, "fs.fs_fix_reset_accumulated_stats");
    /**
     * The {@code fs.log_max_entries} voip-param.
     */
    public static final VoipParamKey FS_LOG_MAX_ENTRIES = new VoipParamKey(VoipParamType.INTEGER, "fs.log_max_entries");
    /**
     * The {@code fs.min_connected_participants} voip-param.
     */
    public static final VoipParamKey FS_MIN_CONNECTED_PARTICIPANTS = new VoipParamKey(VoipParamType.INTEGER, "fs.min_connected_participants");
    /**
     * The {@code fs.peer_row_sampling_rate} voip-param.
     */
    public static final VoipParamKey FS_PEER_ROW_SAMPLING_RATE = new VoipParamKey(VoipParamType.INTEGER, "fs.peer_row_sampling_rate");
    /**
     * The {@code fs.self_row_sampling_rate} voip-param.
     */
    public static final VoipParamKey FS_SELF_ROW_SAMPLING_RATE = new VoipParamKey(VoipParamType.INTEGER, "fs.self_row_sampling_rate");
    /**
     * The {@code fs.sub_fail_start_stream_fix_enabled} voip-param.
     */
    public static final VoipParamKey FS_SUB_FAIL_START_STREAM_FIX_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "fs.sub_fail_start_stream_fix_enabled");
    /**
     * The {@code history_based_bwe.enable_bwe_set_history_bitrate_fix} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLE_BWE_SET_HISTORY_BITRATE_FIX = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enable_bwe_set_history_bitrate_fix");
    /**
     * The {@code history_based_bwe.enable_history_based_info} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLE_HISTORY_BASED_INFO = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enable_history_based_info");
    /**
     * The {@code history_based_bwe.enable_history_based_info_fix} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLE_HISTORY_BASED_INFO_FIX = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enable_history_based_info_fix");
    /**
     * The {@code history_based_bwe.enable_history_based_min_rtt} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLE_HISTORY_BASED_MIN_RTT = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enable_history_based_min_rtt");
    /**
     * The {@code history_based_bwe.enable_history_video_record_info} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLE_HISTORY_VIDEO_RECORD_INFO = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enable_history_video_record_info");
    /**
     * The {@code history_based_bwe.enabled} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enabled");
    /**
     * The {@code history_based_bwe.enabled_match_self_only} voip-param.
     */
    public static final VoipParamKey HISTORY_BASED_BWE_ENABLED_MATCH_SELF_ONLY = new VoipParamKey(VoipParamType.INTEGER, "history_based_bwe.enabled_match_self_only");
    /**
     * The {@code history_storage.audio_record_clock_cb_threshold} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_AUDIO_RECORD_CLOCK_CB_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.audio_record_clock_cb_threshold");
    /**
     * The {@code history_storage.detailed_call_transport_record_audit} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_DETAILED_CALL_TRANSPORT_RECORD_AUDIT = new VoipParamKey(VoipParamType.INTEGER, "history_storage.detailed_call_transport_record_audit");
    /**
     * The {@code history_storage.detailed_call_transport_record_enable} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_DETAILED_CALL_TRANSPORT_RECORD_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.detailed_call_transport_record_enable");
    /**
     * The {@code history_storage.detailed_call_transport_record_max_num_of_call_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_DETAILED_CALL_TRANSPORT_RECORD_MAX_NUM_OF_CALL_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.detailed_call_transport_record_max_num_of_call_record");
    /**
     * The {@code history_storage.detailed_call_transport_record_min_duration_to_save} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_DETAILED_CALL_TRANSPORT_RECORD_MIN_DURATION_TO_SAVE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.detailed_call_transport_record_min_duration_to_save");
    /**
     * The {@code history_storage.enable_audio_device_restart_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_AUDIO_DEVICE_RESTART_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_audio_device_restart_record");
    /**
     * The {@code history_storage.enable_call_participant_record_saving} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_CALL_PARTICIPANT_RECORD_SAVING = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_call_participant_record_saving");
    /**
     * The {@code history_storage.enable_call_record_query_test} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_CALL_RECORD_QUERY_TEST = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_call_record_query_test");
    /**
     * The {@code history_storage.enable_call_state_v1_record_saving} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_CALL_STATE_V1_RECORD_SAVING = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_call_state_v1_record_saving");
    /**
     * The {@code history_storage.enable_calling_audio_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_CALLING_AUDIO_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_calling_audio_record");
    /**
     * The {@code history_storage.enable_group_call_history_based_rtt} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_GROUP_CALL_HISTORY_BASED_RTT = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_group_call_history_based_rtt");
    /**
     * The {@code history_storage.enable_redial_after_cer_metric} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_REDIAL_AFTER_CER_METRIC = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_redial_after_cer_metric");
    /**
     * The {@code history_storage.enable_redial_metric} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_REDIAL_METRIC = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_redial_metric");
    /**
     * The {@code history_storage.enable_redial_metric_for_bwe} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_REDIAL_METRIC_FOR_BWE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_redial_metric_for_bwe");
    /**
     * The {@code history_storage.enable_uaqc_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLE_UAQC_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enable_uaqc_record");
    /**
     * The {@code history_storage.enabled} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "history_storage.enabled");
    /**
     * The {@code history_storage.group_call_record_enable} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_GROUP_CALL_RECORD_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.group_call_record_enable");
    /**
     * The {@code history_storage.max_num_of_call_participant_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_MAX_NUM_OF_CALL_PARTICIPANT_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.max_num_of_call_participant_record");
    /**
     * The {@code history_storage.max_num_of_call_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_MAX_NUM_OF_CALL_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.max_num_of_call_record");
    /**
     * The {@code history_storage.max_num_of_call_transport_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_MAX_NUM_OF_CALL_TRANSPORT_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.max_num_of_call_transport_record");
    /**
     * The {@code history_storage.max_redial_interval} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_MAX_REDIAL_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "history_storage.max_redial_interval");
    /**
     * The {@code history_storage.min_duration_to_save_call_transport_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_MIN_DURATION_TO_SAVE_CALL_TRANSPORT_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.min_duration_to_save_call_transport_record");
    /**
     * The {@code history_storage.uaqc_record_max_num} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_UAQC_RECORD_MAX_NUM = new VoipParamKey(VoipParamType.INTEGER, "history_storage.uaqc_record_max_num");
    /**
     * The {@code history_storage.uaqc_record_min_duration_to_save} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_UAQC_RECORD_MIN_DURATION_TO_SAVE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.uaqc_record_min_duration_to_save");
    /**
     * The {@code history_storage.use_pj_file_api} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_USE_PJ_FILE_API = new VoipParamKey(VoipParamType.INTEGER, "history_storage.use_pj_file_api");
    /**
     * The {@code history_storage.video_record_enable} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_VIDEO_RECORD_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.video_record_enable");
    /**
     * The {@code history_storage.video_record_max_num_of_call_record} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_VIDEO_RECORD_MAX_NUM_OF_CALL_RECORD = new VoipParamKey(VoipParamType.INTEGER, "history_storage.video_record_max_num_of_call_record");
    /**
     * The {@code history_storage.video_record_min_duration_to_save} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_VIDEO_RECORD_MIN_DURATION_TO_SAVE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.video_record_min_duration_to_save");
    /**
     * The {@code history_storage.web_detailed_call_transport_record_enable} voip-param.
     */
    public static final VoipParamKey HISTORY_STORAGE_WEB_DETAILED_CALL_TRANSPORT_RECORD_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "history_storage.web_detailed_call_transport_record_enable");
    /**
     * The {@code init_bwe.adaptive_ramp_up_speed_bw_thresh_kbps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ADAPTIVE_RAMP_UP_SPEED_BW_THRESH_KBPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.adaptive_ramp_up_speed_bw_thresh_kbps");
    /**
     * The {@code init_bwe.adaptive_ramp_up_speed_decr_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ADAPTIVE_RAMP_UP_SPEED_DECR_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.adaptive_ramp_up_speed_decr_ratio");
    /**
     * The {@code init_bwe.adaptive_ramp_up_speed_min_incr_factor} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ADAPTIVE_RAMP_UP_SPEED_MIN_INCR_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.adaptive_ramp_up_speed_min_incr_factor");
    /**
     * The {@code init_bwe.additional_iter_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ADDITIONAL_ITER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.additional_iter_threshold");
    /**
     * The {@code init_bwe.always_upd_last_udst_br} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ALWAYS_UPD_LAST_UDST_BR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.always_upd_last_udst_br");
    /**
     * The {@code init_bwe.ap_bitrate_threshold_for_switch} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_BITRATE_THRESHOLD_FOR_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_bitrate_threshold_for_switch");
    /**
     * The {@code init_bwe.ap_disable_probe_down} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_DISABLE_PROBE_DOWN = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_disable_probe_down");
    /**
     * The {@code init_bwe.ap_down_learning_rate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_DOWN_LEARNING_RATE = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_down_learning_rate");
    /**
     * The {@code init_bwe.ap_ema_alpha} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_ema_alpha");
    /**
     * The {@code init_bwe.ap_enable_dyn_switch2} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_ENABLE_DYN_SWITCH2 = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_enable_dyn_switch2");
    /**
     * The {@code init_bwe.ap_enable_dynamic_switch} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_ENABLE_DYNAMIC_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_enable_dynamic_switch");
    /**
     * The {@code init_bwe.ap_enable_ema_estimation} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_ENABLE_EMA_ESTIMATION = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_enable_ema_estimation");
    /**
     * The {@code init_bwe.ap_enable_train_probing} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_ENABLE_TRAIN_PROBING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_enable_train_probing");
    /**
     * The {@code init_bwe.ap_margin_for_hold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MARGIN_FOR_HOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_margin_for_hold");
    /**
     * The {@code init_bwe.ap_margin_for_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MARGIN_FOR_UP = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_margin_for_up");
    /**
     * The {@code init_bwe.ap_max_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MAX_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_max_bps");
    /**
     * The {@code init_bwe.ap_max_packet_pairs} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MAX_PACKET_PAIRS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_max_packet_pairs");
    /**
     * The {@code init_bwe.ap_max_train_probing_pkts} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MAX_TRAIN_PROBING_PKTS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_max_train_probing_pkts");
    /**
     * The {@code init_bwe.ap_max_train_probing_rounds} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MAX_TRAIN_PROBING_ROUNDS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_max_train_probing_rounds");
    /**
     * The {@code init_bwe.ap_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_min_bps");
    /**
     * The {@code init_bwe.ap_pct_down} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PCT_DOWN = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_pct_down");
    /**
     * The {@code init_bwe.ap_pct_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PCT_UP = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_pct_up");
    /**
     * The {@code init_bwe.ap_pdt_down} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PDT_DOWN = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_pdt_down");
    /**
     * The {@code init_bwe.ap_pdt_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PDT_UP = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_pdt_up");
    /**
     * The {@code init_bwe.ap_probe_next} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PROBE_NEXT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_probe_next");
    /**
     * The {@code init_bwe.ap_probing_pkt_size} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PROBING_PKT_SIZE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_probing_pkt_size");
    /**
     * The {@code init_bwe.ap_probing_reset_timeout_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_PROBING_RESET_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_probing_reset_timeout_ms");
    /**
     * The {@code init_bwe.ap_rtt_threshold_for_switch} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_RTT_THRESHOLD_FOR_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_rtt_threshold_for_switch");
    /**
     * The {@code init_bwe.ap_scheme} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_scheme");
    /**
     * The {@code init_bwe.ap_start_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_START_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_start_bps");
    /**
     * The {@code init_bwe.ap_target_relaxation_factor} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_TARGET_RELAXATION_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_target_relaxation_factor");
    /**
     * The {@code init_bwe.ap_up_learning_rate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_UP_LEARNING_RATE = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ap_up_learning_rate");
    /**
     * The {@code init_bwe.ap_use_dl_recv_bytes} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_USE_DL_RECV_BYTES = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_use_dl_recv_bytes");
    /**
     * The {@code init_bwe.ap_use_ul_server_ts} voip-param.
     */
    public static final VoipParamKey INIT_BWE_AP_USE_UL_SERVER_TS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ap_use_ul_server_ts");
    /**
     * The {@code init_bwe.apply_2p_info_init_sfu_ul_bwe_multiplier_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_APPLY_2P_INFO_INIT_SFU_UL_BWE_MULTIPLIER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.apply_2p_info_init_sfu_ul_bwe_multiplier_threshold");
    /**
     * The {@code init_bwe.apply_hd_max_tgt_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_APPLY_HD_MAX_TGT_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.apply_hd_max_tgt_bitrate");
    /**
     * The {@code init_bwe.bitrate_multiplier_per_iter} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BITRATE_MULTIPLIER_PER_ITER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.bitrate_multiplier_per_iter");
    /**
     * The {@code init_bwe.bwa_min_vid_stream_reserve_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWA_MIN_VID_STREAM_RESERVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwa_min_vid_stream_reserve_bps");
    /**
     * The {@code init_bwe.bwe_additive_hold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_ADDITIVE_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_additive_hold_ms");
    /**
     * The {@code init_bwe.bwe_clamp_scheme_after_call_start} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_CLAMP_SCHEME_AFTER_CALL_START = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_clamp_scheme_after_call_start");
    /**
     * The {@code init_bwe.bwe_congestion_hold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_CONGESTION_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_congestion_hold_ms");
    /**
     * The {@code init_bwe.bwe_init_hold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_INIT_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_init_hold_ms");
    /**
     * The {@code init_bwe.bwe_mcp_hold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_MCP_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_mcp_hold_ms");
    /**
     * The {@code init_bwe.bwe_probing_mode} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_PROBING_MODE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_probing_mode");
    /**
     * The {@code init_bwe.bwe_recv_drop_hold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BWE_RECV_DROP_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.bwe_recv_drop_hold_ms");
    /**
     * The {@code init_bwe.byte_multiplier_per_iter} voip-param.
     */
    public static final VoipParamKey INIT_BWE_BYTE_MULTIPLIER_PER_ITER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.byte_multiplier_per_iter");
    /**
     * The {@code init_bwe.cap_estimated_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CAP_ESTIMATED_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.cap_estimated_bitrate");
    /**
     * The {@code init_bwe.cap_hist_init_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CAP_HIST_INIT_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.cap_hist_init_bitrate");
    /**
     * The {@code init_bwe.check_sbwe_bottleneck_for_low_rbwe_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CHECK_SBWE_BOTTLENECK_FOR_LOW_RBWE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.check_sbwe_bottleneck_for_low_rbwe_threshold");
    /**
     * The {@code init_bwe.collect_hbwe_stats} voip-param.
     */
    public static final VoipParamKey INIT_BWE_COLLECT_HBWE_STATS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.collect_hbwe_stats");
    /**
     * The {@code init_bwe.conservative_mode_bw_thresh_kbps_lower_bound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CONSERVATIVE_MODE_BW_THRESH_KBPS_LOWER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.conservative_mode_bw_thresh_kbps_lower_bound");
    /**
     * The {@code init_bwe.conservative_mode_init_bwe_lower_bound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CONSERVATIVE_MODE_INIT_BWE_LOWER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.conservative_mode_init_bwe_lower_bound");
    /**
     * The {@code init_bwe.conservative_mode_init_bwe_upper_bound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CONSERVATIVE_MODE_INIT_BWE_UPPER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.conservative_mode_init_bwe_upper_bound");
    /**
     * The {@code init_bwe.consider_remote_bwe_min_valid} voip-param.
     */
    public static final VoipParamKey INIT_BWE_CONSIDER_REMOTE_BWE_MIN_VALID = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.consider_remote_bwe_min_valid");
    /**
     * The {@code init_bwe.disable_duplicate_clamping} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DISABLE_DUPLICATE_CLAMPING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.disable_duplicate_clamping");
    /**
     * The {@code init_bwe.disable_remb_rules} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DISABLE_REMB_RULES = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.disable_remb_rules");
    /**
     * The {@code init_bwe.disable_rtcp_remb} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DISABLE_RTCP_REMB = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.disable_rtcp_remb");
    /**
     * The {@code init_bwe.disable_stop_mcp_on_get_stats} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DISABLE_STOP_MCP_ON_GET_STATS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.disable_stop_mcp_on_get_stats");
    /**
     * The {@code init_bwe.downlink_sender_side_rate_increase_factor_fr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DOWNLINK_SENDER_SIDE_RATE_INCREASE_FACTOR_FR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.downlink_sender_side_rate_increase_factor_fr");
    /**
     * The {@code init_bwe.dynamic_init_bwe_check_time_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_BWE_CHECK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_bwe_check_time_ms");
    /**
     * The {@code init_bwe.dynamic_init_bwe_fallback_plr_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_BWE_FALLBACK_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.dynamic_init_bwe_fallback_plr_threshold");
    /**
     * The {@code init_bwe.dynamic_init_bwe_fallback_rtt_threshold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_BWE_FALLBACK_RTT_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_bwe_fallback_rtt_threshold_ms");
    /**
     * The {@code init_bwe.dynamic_init_bwe_fallback_value} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_BWE_FALLBACK_VALUE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_bwe_fallback_value");
    /**
     * The {@code init_bwe.dynamic_init_dl_bwe_check_time_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_DL_BWE_CHECK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_dl_bwe_check_time_ms");
    /**
     * The {@code init_bwe.dynamic_init_dl_bwe_fallback_plr_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_DL_BWE_FALLBACK_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.dynamic_init_dl_bwe_fallback_plr_threshold");
    /**
     * The {@code init_bwe.dynamic_init_dl_bwe_fallback_rtt_threshold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_DL_BWE_FALLBACK_RTT_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_dl_bwe_fallback_rtt_threshold_ms");
    /**
     * The {@code init_bwe.dynamic_init_dl_bwe_fallback_value} voip-param.
     */
    public static final VoipParamKey INIT_BWE_DYNAMIC_INIT_DL_BWE_FALLBACK_VALUE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.dynamic_init_dl_bwe_fallback_value");
    /**
     * The {@code init_bwe.ecn_ce_per_sec_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ECN_CE_PER_SEC_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ecn_ce_per_sec_threshold");
    /**
     * The {@code init_bwe.ecn_pct_ce_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ECN_PCT_CE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ecn_pct_ce_threshold");
    /**
     * The {@code init_bwe.ecn_slide_window_size} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ECN_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ecn_slide_window_size");
    /**
     * The {@code init_bwe.enable_adaptive_probing} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_ADAPTIVE_PROBING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_adaptive_probing");
    /**
     * The {@code init_bwe.enable_av1_to_h264_fallback} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_AV1_TO_H264_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_av1_to_h264_fallback");
    /**
     * The {@code init_bwe.enable_bwe_hold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_BWE_HOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_bwe_hold");
    /**
     * The {@code init_bwe.enable_bwe_init_ts_check} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_BWE_INIT_TS_CHECK = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_bwe_init_ts_check");
    /**
     * The {@code init_bwe.enable_downlink_relay_latency_only} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_DOWNLINK_RELAY_LATENCY_ONLY = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_downlink_relay_latency_only");
    /**
     * The {@code init_bwe.enable_dynamic_init_bwe_fallback} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_DYNAMIC_INIT_BWE_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_dynamic_init_bwe_fallback");
    /**
     * The {@code init_bwe.enable_dynamic_init_dl_bwe_fallback} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_DYNAMIC_INIT_DL_BWE_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_dynamic_init_dl_bwe_fallback");
    /**
     * The {@code init_bwe.enable_ecn_bwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_ECN_BWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_ecn_bwe");
    /**
     * The {@code init_bwe.enable_fast_remb} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_FAST_REMB = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_fast_remb");
    /**
     * The {@code init_bwe.enable_fast_rr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_FAST_RR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_fast_rr");
    /**
     * The {@code init_bwe.enable_fr_on_non_default_init_value} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_FR_ON_NON_DEFAULT_INIT_VALUE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_fr_on_non_default_init_value");
    /**
     * The {@code init_bwe.enable_goodput_in_sbwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_GOODPUT_IN_SBWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_goodput_in_sbwe");
    /**
     * The {@code init_bwe.enable_hbh_fec_splitter_tx} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_HBH_FEC_SPLITTER_TX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_hbh_fec_splitter_tx");
    /**
     * The {@code init_bwe.enable_hbh_fec_srtp_tx} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_HBH_FEC_SRTP_TX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_hbh_fec_srtp_tx");
    /**
     * The {@code init_bwe.enable_hist_based} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_HIST_BASED = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_hist_based");
    /**
     * The {@code init_bwe.enable_init_fallback_based_on_dl_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_INIT_FALLBACK_BASED_ON_DL_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_init_fallback_based_on_dl_pp");
    /**
     * The {@code init_bwe.enable_init_fallback_based_on_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_INIT_FALLBACK_BASED_ON_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_init_fallback_based_on_pp");
    /**
     * The {@code init_bwe.enable_init_info} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_INIT_INFO = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_init_info");
    /**
     * The {@code init_bwe.enable_init_pp_probing} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_INIT_PP_PROBING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_init_pp_probing");
    /**
     * The {@code init_bwe.enable_nadl_input_log} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_NADL_INPUT_LOG = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_nadl_input_log");
    /**
     * The {@code init_bwe.enable_nadl_output_log} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_NADL_OUTPUT_LOG = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_nadl_output_log");
    /**
     * The {@code init_bwe.enable_one_side_mode} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_ONE_SIDE_MODE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_one_side_mode");
    /**
     * The {@code init_bwe.enable_peer_bwe_based_clamping} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_PEER_BWE_BASED_CLAMPING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_peer_bwe_based_clamping");
    /**
     * The {@code init_bwe.enable_peer_clamping_on_init_update} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_PEER_CLAMPING_ON_INIT_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_peer_clamping_on_init_update");
    /**
     * The {@code init_bwe.enable_peer_clamping_on_init_update_v2} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_PEER_CLAMPING_ON_INIT_UPDATE_V2 = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_peer_clamping_on_init_update_v2");
    /**
     * The {@code init_bwe.enable_preset_algo_fix} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_PRESET_ALGO_FIX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_preset_algo_fix");
    /**
     * The {@code init_bwe.enable_quickhd} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd");
    /**
     * The {@code init_bwe.enable_quickhd_instant_ramp_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_INSTANT_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_instant_ramp_up");
    /**
     * The {@code init_bwe.enable_quickhd_ml} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_ML = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_ml");
    /**
     * The {@code init_bwe.enable_quickhd_ml_bwe_override} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_ML_BWE_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_ml_bwe_override");
    /**
     * The {@code init_bwe.enable_quickhd_slc} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_SLC = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_slc");
    /**
     * The {@code init_bwe.enable_quickhd_slr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_SLR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_slr");
    /**
     * The {@code init_bwe.enable_quickhd_slr_bwe_override} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_QUICKHD_SLR_BWE_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_quickhd_slr_bwe_override");
    /**
     * The {@code init_bwe.enable_rbe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_RBE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_rbe");
    /**
     * The {@code init_bwe.enable_reuse_2p_bwe_for_sfu} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_REUSE_2P_BWE_FOR_SFU = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_reuse_2p_bwe_for_sfu");
    /**
     * The {@code init_bwe.enable_rl_bwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_RL_BWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_rl_bwe");
    /**
     * The {@code init_bwe.enable_time_info_in_sbwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_TIME_INFO_IN_SBWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_time_info_in_sbwe");
    /**
     * The {@code init_bwe.enable_video_hist_based_instant_ramp_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLE_VIDEO_HIST_BASED_INSTANT_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enable_video_hist_based_instant_ramp_up");
    /**
     * The {@code init_bwe.enabled_for_video_upgrade} voip-param.
     */
    public static final VoipParamKey INIT_BWE_ENABLED_FOR_VIDEO_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.enabled_for_video_upgrade");
    /**
     * The {@code init_bwe.fast_rr_handling_policy} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FAST_RR_HANDLING_POLICY = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fast_rr_handling_policy");
    /**
     * The {@code init_bwe.fast_rr_pt} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FAST_RR_PT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fast_rr_pt");
    /**
     * The {@code init_bwe.fast_rr_send_interval_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FAST_RR_SEND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fast_rr_send_interval_ms");
    /**
     * The {@code init_bwe.fec_exploration_max_duration_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORATION_MAX_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_exploration_max_duration_ms");
    /**
     * The {@code init_bwe.fec_exploration_max_ratio_delta} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORATION_MAX_RATIO_DELTA = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.fec_exploration_max_ratio_delta");
    /**
     * The {@code init_bwe.fec_exploration_min_duration_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORATION_MIN_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_exploration_min_duration_ms");
    /**
     * The {@code init_bwe.fec_exploration_min_wait_time_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORATION_MIN_WAIT_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_exploration_min_wait_time_ms");
    /**
     * The {@code init_bwe.fec_exploration_prob_perc} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORATION_PROB_PERC = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_exploration_prob_perc");
    /**
     * The {@code init_bwe.fec_explore_absolute_target} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORE_ABSOLUTE_TARGET = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_explore_absolute_target");
    /**
     * The {@code init_bwe.fec_explore_max_target} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORE_MAX_TARGET = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.fec_explore_max_target");
    /**
     * The {@code init_bwe.fec_explore_min_feasible_bwe_kbps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FEC_EXPLORE_MIN_FEASIBLE_BWE_KBPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.fec_explore_min_feasible_bwe_kbps");
    /**
     * The {@code init_bwe.finish_probing_always} voip-param.
     */
    public static final VoipParamKey INIT_BWE_FINISH_PROBING_ALWAYS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.finish_probing_always");
    /**
     * The {@code init_bwe.h264_keyframe_vqs} voip-param.
     */
    public static final VoipParamKey INIT_BWE_H264_KEYFRAME_VQS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.h264_keyframe_vqs");
    /**
     * The {@code init_bwe.hbh_fec_tx_algorithm} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HBH_FEC_TX_ALGORITHM = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.hbh_fec_tx_algorithm");
    /**
     * The {@code init_bwe.hd_dyn_max_target_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HD_DYN_MAX_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.hd_dyn_max_target_bitrate");
    /**
     * The {@code init_bwe.hd_dyn_only_increase_cap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HD_DYN_ONLY_INCREASE_CAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.hd_dyn_only_increase_cap");
    /**
     * The {@code init_bwe.hd_user_setting_bitmap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HD_USER_SETTING_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.hd_user_setting_bitmap");
    /**
     * The {@code init_bwe.his_always_use_if_avail} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_ALWAYS_USE_IF_AVAIL = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.his_always_use_if_avail");
    /**
     * The {@code init_bwe.his_bitrate_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_BITRATE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.his_bitrate_multiplier");
    /**
     * The {@code init_bwe.his_enable} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.his_enable");
    /**
     * The {@code init_bwe.his_recent_call_threshold_sec_cell} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_RECENT_CALL_THRESHOLD_SEC_CELL = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.his_recent_call_threshold_sec_cell");
    /**
     * The {@code init_bwe.his_recent_call_threshold_sec_wifi} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_RECENT_CALL_THRESHOLD_SEC_WIFI = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.his_recent_call_threshold_sec_wifi");
    /**
     * The {@code init_bwe.his_use_recent_probing} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HIS_USE_RECENT_PROBING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.his_use_recent_probing");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_instant_ramp_up");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up_match_peer} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP_MATCH_PEER = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_instant_ramp_up_match_peer");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up_match_self} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP_MATCH_SELF = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_instant_ramp_up_match_self");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up_option} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP_OPTION = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_instant_ramp_up_option");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.history_based_bwe_instant_ramp_up_ratio");
    /**
     * The {@code init_bwe.history_based_bwe_instant_ramp_up_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_INSTANT_RAMP_UP_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_instant_ramp_up_threshold");
    /**
     * The {@code init_bwe.history_based_bwe_update_ceiling} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_UPDATE_CEILING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_update_ceiling");
    /**
     * The {@code init_bwe.history_based_bwe_update_ceiling_audio_reserve} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_UPDATE_CEILING_AUDIO_RESERVE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_update_ceiling_audio_reserve");
    /**
     * The {@code init_bwe.history_based_bwe_update_ceiling_forced} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_BWE_UPDATE_CEILING_FORCED = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_bwe_update_ceiling_forced");
    /**
     * The {@code init_bwe.history_based_sfu_downlink_init_bwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_SFU_DOWNLINK_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_sfu_downlink_init_bwe");
    /**
     * The {@code init_bwe.history_based_sfu_downlink_init_bwe_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_SFU_DOWNLINK_INIT_BWE_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.history_based_sfu_downlink_init_bwe_ratio");
    /**
     * The {@code init_bwe.history_based_sfu_uplink_init_bwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_SFU_UPLINK_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.history_based_sfu_uplink_init_bwe");
    /**
     * The {@code init_bwe.history_based_sfu_uplink_init_bwe_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HISTORY_BASED_SFU_UPLINK_INIT_BWE_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.history_based_sfu_uplink_init_bwe_ratio");
    /**
     * The {@code init_bwe.hold_source_bitmap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_HOLD_SOURCE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.hold_source_bitmap");
    /**
     * The {@code init_bwe.indefinite_hold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INDEFINITE_HOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.indefinite_hold");
    /**
     * The {@code init_bwe.init_allow_remote_dec} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_ALLOW_REMOTE_DEC = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_allow_remote_dec");
    /**
     * The {@code init_bwe.init_bwe_fr_check_time_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_BWE_FR_CHECK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_bwe_fr_check_time_ms");
    /**
     * The {@code init_bwe.init_dl_bwe_fr_check_time_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_DL_BWE_FR_CHECK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_dl_bwe_fr_check_time_ms");
    /**
     * The {@code init_bwe.init_enable_stop_clamp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_ENABLE_STOP_CLAMP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_enable_stop_clamp");
    /**
     * The {@code init_bwe.init_fallback_dl_pp_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_FALLBACK_DL_PP_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.init_fallback_dl_pp_weight");
    /**
     * The {@code init_bwe.init_fallback_pp_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_FALLBACK_PP_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.init_fallback_pp_weight");
    /**
     * The {@code init_bwe.init_high_end_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_HIGH_END_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_high_end_bitrate");
    /**
     * The {@code init_bwe.init_momentum_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_MOMENTUM_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.init_momentum_multiplier");
    /**
     * The {@code init_bwe.init_pp_bitrate_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_BITRATE_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.init_pp_bitrate_ratio");
    /**
     * The {@code init_bwe.init_pp_hdr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_HDR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_hdr");
    /**
     * The {@code init_bwe.init_pp_min_bps_to_use_momentum} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_MIN_BPS_TO_USE_MOMENTUM = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_min_bps_to_use_momentum");
    /**
     * The {@code init_bwe.init_pp_probing_reset_inflection_point} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_PROBING_RESET_INFLECTION_POINT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_probing_reset_inflection_point");
    /**
     * The {@code init_bwe.init_pp_probing_stop_fix} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_PROBING_STOP_FIX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_probing_stop_fix");
    /**
     * The {@code init_bwe.init_pp_ramp_up_expire_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_EXPIRE_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_expire_ms");
    /**
     * The {@code init_bwe.init_pp_ramp_up_stop_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_STOP_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_stop_min_bps");
    /**
     * The {@code init_bwe.init_pp_ramp_up_target_hd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_TARGET_HD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_target_hd_bps");
    /**
     * The {@code init_bwe.init_pp_ramp_up_target_hd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_TARGET_HD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_target_hd_min_bps");
    /**
     * The {@code init_bwe.init_pp_ramp_up_target_sd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_TARGET_SD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_target_sd_bps");
    /**
     * The {@code init_bwe.init_pp_ramp_up_target_sd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_TARGET_SD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_ramp_up_target_sd_min_bps");
    /**
     * The {@code init_bwe.init_pp_ramp_up_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_RAMP_UP_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.init_pp_ramp_up_weight");
    /**
     * The {@code init_bwe.init_pp_stop_probing_on_receive_drop} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_PP_STOP_PROBING_ON_RECEIVE_DROP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_pp_stop_probing_on_receive_drop");
    /**
     * The {@code init_bwe.init_ramp_up_target_using_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_RAMP_UP_TARGET_USING_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_ramp_up_target_using_pp");
    /**
     * The {@code init_bwe.init_ramp_up_using_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_RAMP_UP_USING_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_ramp_up_using_pp");
    /**
     * The {@code init_bwe.init_secondary_video_stream_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_SECONDARY_VIDEO_STREAM_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_secondary_video_stream_bitrate");
    /**
     * The {@code init_bwe.init_skip_first_n_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_SKIP_FIRST_N_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_skip_first_n_pp");
    /**
     * The {@code init_bwe.init_stop_momentum_on_low_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_STOP_MOMENTUM_ON_LOW_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_stop_momentum_on_low_pp");
    /**
     * The {@code init_bwe.init_time_threshold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_TIME_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_time_threshold_ms");
    /**
     * The {@code init_bwe.init_update_bitrate_using_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_UPDATE_BITRATE_USING_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_update_bitrate_using_pp");
    /**
     * The {@code init_bwe.init_use_momentum} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_USE_MOMENTUM = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_use_momentum");
    /**
     * The {@code init_bwe.init_use_remote_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INIT_USE_REMOTE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.init_use_remote_bitrate");
    /**
     * The {@code init_bwe.instant_ramp_up_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INSTANT_RAMP_UP_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.instant_ramp_up_min_bps");
    /**
     * The {@code init_bwe.instant_ramp_up_target_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_INSTANT_RAMP_UP_TARGET_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.instant_ramp_up_target_bps");
    /**
     * The {@code init_bwe.low_rbwe_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_LOW_RBWE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.low_rbwe_threshold");
    /**
     * The {@code init_bwe.max_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_bitrate");
    /**
     * The {@code init_bwe.max_bytes} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_BYTES = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_bytes");
    /**
     * The {@code init_bwe.max_hist_init_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_HIST_INIT_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_hist_init_bitrate");
    /**
     * The {@code init_bwe.max_init_bwe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_init_bwe");
    /**
     * The {@code init_bwe.max_iterations} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_ITERATIONS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_iterations");
    /**
     * The {@code init_bwe.max_tx_rott_based_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MAX_TX_ROTT_BASED_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.max_tx_rott_based_bitrate");
    /**
     * The {@code init_bwe.min_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.min_bitrate");
    /**
     * The {@code init_bwe.min_partition_ratio_to_promote} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MIN_PARTITION_RATIO_TO_PROMOTE = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.min_partition_ratio_to_promote");
    /**
     * The {@code init_bwe.min_partition_ratio_to_stay} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MIN_PARTITION_RATIO_TO_STAY = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.min_partition_ratio_to_stay");
    /**
     * The {@code init_bwe.min_segment_duration_to_use_2p_info_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_MIN_SEGMENT_DURATION_TO_USE_2P_INFO_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.min_segment_duration_to_use_2p_info_ms");
    /**
     * The {@code init_bwe.nack_rtx_pkt_seq_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_NACK_RTX_PKT_SEQ_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.nack_rtx_pkt_seq_threshold");
    /**
     * The {@code init_bwe.nack_rtx_pkt_ts_threshold_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_NACK_RTX_PKT_TS_THRESHOLD_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.nack_rtx_pkt_ts_threshold_multiplier");
    /**
     * The {@code init_bwe.oibwe_slow_polling} voip-param.
     */
    public static final VoipParamKey INIT_BWE_OIBWE_SLOW_POLLING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.oibwe_slow_polling");
    /**
     * The {@code init_bwe.polling_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_POLLING_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.polling_ms");
    /**
     * The {@code init_bwe.probing_pkt_size} voip-param.
     */
    public static final VoipParamKey INIT_BWE_PROBING_PKT_SIZE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.probing_pkt_size");
    /**
     * The {@code init_bwe.probing_req_timeout_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_PROBING_REQ_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.probing_req_timeout_ms");
    /**
     * The {@code init_bwe.probing_res_timeout_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_PROBING_RES_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.probing_res_timeout_ms");
    /**
     * The {@code init_bwe.quickhd_ml_model_name} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "init_bwe.quickhd_ml_model_name");
    /**
     * The {@code init_bwe.quickhd_ml_num_features} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_NUM_FEATURES = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_num_features");
    /**
     * The {@code init_bwe.quickhd_ml_ramp_up_option} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_RAMP_UP_OPTION = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_ramp_up_option");
    /**
     * The {@code init_bwe.quickhd_ml_ramp_up_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_RAMP_UP_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_ml_ramp_up_ratio");
    /**
     * The {@code init_bwe.quickhd_ml_target_hd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_TARGET_HD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_target_hd_bps");
    /**
     * The {@code init_bwe.quickhd_ml_target_hd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_TARGET_HD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_target_hd_min_bps");
    /**
     * The {@code init_bwe.quickhd_ml_target_sd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_TARGET_SD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_target_sd_bps");
    /**
     * The {@code init_bwe.quickhd_ml_target_sd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_ML_TARGET_SD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_ml_target_sd_min_bps");
    /**
     * The {@code init_bwe.quickhd_slc_hd_prob_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLC_HD_PROB_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slc_hd_prob_threshold");
    /**
     * The {@code init_bwe.quickhd_slc_sd_prob_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLC_SD_PROB_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slc_sd_prob_threshold");
    /**
     * The {@code init_bwe.quickhd_slc_target_hd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLC_TARGET_HD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slc_target_hd_bps");
    /**
     * The {@code init_bwe.quickhd_slc_target_sd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLC_TARGET_SD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slc_target_sd_bps");
    /**
     * The {@code init_bwe.quickhd_slr_goodput_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_GOODPUT_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slr_goodput_coeff");
    /**
     * The {@code init_bwe.quickhd_slr_hist_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_HIST_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slr_hist_coeff");
    /**
     * The {@code init_bwe.quickhd_slr_hist_fix} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_HIST_FIX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_hist_fix");
    /**
     * The {@code init_bwe.quickhd_slr_hist_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_HIST_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_hist_min_bps");
    /**
     * The {@code init_bwe.quickhd_slr_init_bwe_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_INIT_BWE_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slr_init_bwe_coeff");
    /**
     * The {@code init_bwe.quickhd_slr_offset} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_offset");
    /**
     * The {@code init_bwe.quickhd_slr_ramp_up_option} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_RAMP_UP_OPTION = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_ramp_up_option");
    /**
     * The {@code init_bwe.quickhd_slr_ramp_up_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_RAMP_UP_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.quickhd_slr_ramp_up_ratio");
    /**
     * The {@code init_bwe.quickhd_slr_target_hd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_TARGET_HD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_target_hd_bps");
    /**
     * The {@code init_bwe.quickhd_slr_target_hd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_TARGET_HD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_target_hd_min_bps");
    /**
     * The {@code init_bwe.quickhd_slr_target_sd_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_TARGET_SD_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_target_sd_bps");
    /**
     * The {@code init_bwe.quickhd_slr_target_sd_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_QUICKHD_SLR_TARGET_SD_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.quickhd_slr_target_sd_min_bps");
    /**
     * The {@code init_bwe.rbe_init_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INIT_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_init_bps");
    /**
     * The {@code init_bwe.rbe_init_bps_use_vector} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INIT_BPS_USE_VECTOR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_init_bps_use_vector");
    /**
     * The {@code init_bwe.rbe_init_bps_vector} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INIT_BPS_VECTOR = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_init_bps_vector");
    /**
     * The {@code init_bwe.rbe_instant_ramp_min_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INSTANT_RAMP_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_instant_ramp_min_bps");
    /**
     * The {@code init_bwe.rbe_instant_ramp_min_bps_vector} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INSTANT_RAMP_MIN_BPS_VECTOR = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_instant_ramp_min_bps_vector");
    /**
     * The {@code init_bwe.rbe_instant_ramp_target_bps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INSTANT_RAMP_TARGET_BPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_instant_ramp_target_bps");
    /**
     * The {@code init_bwe.rbe_instant_ramp_target_bps_vector} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INSTANT_RAMP_TARGET_BPS_VECTOR = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_instant_ramp_target_bps_vector");
    /**
     * The {@code init_bwe.rbe_instant_ramp_use_vector} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_INSTANT_RAMP_USE_VECTOR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_instant_ramp_use_vector");
    /**
     * The {@code init_bwe.rbe_network_fallback} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_NETWORK_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rbe_network_fallback");
    /**
     * The {@code init_bwe.rbe_targeting_vector_history_cap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_HISTORY_CAP = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_history_cap");
    /**
     * The {@code init_bwe.rbe_targeting_vector_history_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_HISTORY_THRESHOLD = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_history_threshold");
    /**
     * The {@code init_bwe.rbe_targeting_vector_main} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_MAIN = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_main");
    /**
     * The {@code init_bwe.rbe_targeting_vector_ml_cap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_ML_CAP = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_ml_cap");
    /**
     * The {@code init_bwe.rbe_targeting_vector_pp_flip_count_upper_bound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_PP_FLIP_COUNT_UPPER_BOUND = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_pp_flip_count_upper_bound");
    /**
     * The {@code init_bwe.rbe_targeting_vector_pp_high_cap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_PP_HIGH_CAP = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_pp_high_cap");
    /**
     * The {@code init_bwe.rbe_targeting_vector_pp_high_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_PP_HIGH_THRESHOLD = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_pp_high_threshold");
    /**
     * The {@code init_bwe.rbe_targeting_vector_pp_low_cap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_PP_LOW_CAP = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_pp_low_cap");
    /**
     * The {@code init_bwe.rbe_targeting_vector_pp_low_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RBE_TARGETING_VECTOR_PP_LOW_THRESHOLD = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rbe_targeting_vector_pp_low_threshold");
    /**
     * The {@code init_bwe.receive_side_congestion_drop_pct} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RECEIVE_SIDE_CONGESTION_DROP_PCT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.receive_side_congestion_drop_pct");
    /**
     * The {@code init_bwe.reset_bwe_features_bitmap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_features_bitmap");
    /**
     * The {@code init_bwe.reset_bwe_min_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_min_bitrate");
    /**
     * The {@code init_bwe.reset_bwe_on_network_change_vid} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_ON_NETWORK_CHANGE_VID = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_on_network_change_vid");
    /**
     * The {@code init_bwe.reset_bwe_opt_bitmap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_OPT_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_opt_bitmap");
    /**
     * The {@code init_bwe.reset_bwe_ramp_up_using_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_RAMP_UP_USING_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_ramp_up_using_pp");
    /**
     * The {@code init_bwe.reset_bwe_skip_first_n_pp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_BWE_SKIP_FIRST_N_PP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_bwe_skip_first_n_pp");
    /**
     * The {@code init_bwe.reset_dl_bwe_min_bitrate} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_DL_BWE_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_dl_bwe_min_bitrate");
    /**
     * The {@code init_bwe.reset_sfu_bwe_bitmap} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RESET_SFU_BWE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.reset_sfu_bwe_bitmap");
    /**
     * The {@code init_bwe.reuse_2p_info_init_sfu_dl_bwe_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_REUSE_2P_INFO_INIT_SFU_DL_BWE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.reuse_2p_info_init_sfu_dl_bwe_multiplier");
    /**
     * The {@code init_bwe.reuse_2p_info_init_sfu_ul_bwe_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_REUSE_2P_INFO_INIT_SFU_UL_BWE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.reuse_2p_info_init_sfu_ul_bwe_multiplier");
    /**
     * The {@code init_bwe.rl_br_change_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_BR_CHANGE_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_br_change_weight");
    /**
     * The {@code init_bwe.rl_br_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_BR_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_br_weight");
    /**
     * The {@code init_bwe.rl_clamp_to_floor} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_CLAMP_TO_FLOOR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rl_clamp_to_floor");
    /**
     * The {@code init_bwe.rl_delay_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_DELAY_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_delay_weight");
    /**
     * The {@code init_bwe.rl_epoch_duration_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_EPOCH_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rl_epoch_duration_ms");
    /**
     * The {@code init_bwe.rl_epsilon} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_EPSILON = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_epsilon");
    /**
     * The {@code init_bwe.rl_epsilon_decay} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_EPSILON_DECAY = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_epsilon_decay");
    /**
     * The {@code init_bwe.rl_exploration_factor} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_EXPLORATION_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_exploration_factor");
    /**
     * The {@code init_bwe.rl_hold_prob} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_HOLD_PROB = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_hold_prob");
    /**
     * The {@code init_bwe.rl_jitter_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_JITTER_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_jitter_weight");
    /**
     * The {@code init_bwe.rl_learn_only} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_LEARN_ONLY = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rl_learn_only");
    /**
     * The {@code init_bwe.rl_loss_weight} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_LOSS_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_loss_weight");
    /**
     * The {@code init_bwe.rl_reward_alpha} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_REWARD_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_reward_alpha");
    /**
     * The {@code init_bwe.rl_reward_gamma} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_REWARD_GAMMA = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.rl_reward_gamma");
    /**
     * The {@code init_bwe.rl_use_simple_rampup} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_USE_SIMPLE_RAMPUP = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rl_use_simple_rampup");
    /**
     * The {@code init_bwe.rl_video_bitrate_actions} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RL_VIDEO_BITRATE_ACTIONS = new VoipParamKey(VoipParamType.ARRAY, "init_bwe.rl_video_bitrate_actions");
    /**
     * The {@code init_bwe.run_sbwe_on_simulcast_pause_type} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RUN_SBWE_ON_SIMULCAST_PAUSE_TYPE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.run_sbwe_on_simulcast_pause_type");
    /**
     * The {@code init_bwe.rx_throttled_kbps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_RX_THROTTLED_KBPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.rx_throttled_kbps");
    /**
     * The {@code init_bwe.sbwe_bursty_plr_rtt_lowerbound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_BURSTY_PLR_RTT_LOWERBOUND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_bursty_plr_rtt_lowerbound");
    /**
     * The {@code init_bwe.sbwe_bw_delay_product} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_BW_DELAY_PRODUCT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_bw_delay_product");
    /**
     * The {@code init_bwe.sbwe_cong_decr_factor_fast_rr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_CONG_DECR_FACTOR_FAST_RR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_cong_decr_factor_fast_rr");
    /**
     * The {@code init_bwe.sbwe_ignore_rnd_plr_cong} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_IGNORE_RND_PLR_CONG = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_ignore_rnd_plr_cong");
    /**
     * The {@code init_bwe.sbwe_min_fast_rr_plr_interval_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_MIN_FAST_RR_PLR_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_min_fast_rr_plr_interval_ms");
    /**
     * The {@code init_bwe.sbwe_min_fast_rr_rtt_interval_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_MIN_FAST_RR_RTT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_min_fast_rr_rtt_interval_ms");
    /**
     * The {@code init_bwe.sbwe_plr_cong_decr_factor} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_PLR_CONG_DECR_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_plr_cong_decr_factor");
    /**
     * The {@code init_bwe.sbwe_plr_cong_unknown_as_rnd} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_PLR_CONG_UNKNOWN_AS_RND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_plr_cong_unknown_as_rnd");
    /**
     * The {@code init_bwe.sbwe_random_plr_rtt_upperbound} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_RANDOM_PLR_RTT_UPPERBOUND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_random_plr_rtt_upperbound");
    /**
     * The {@code init_bwe.sbwe_reset_inflection_point} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_RESET_INFLECTION_POINT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_reset_inflection_point");
    /**
     * The {@code init_bwe.sbwe_skip_rtt_slope_for_rtcp_rr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_SKIP_RTT_SLOPE_FOR_RTCP_RR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_skip_rtt_slope_for_rtcp_rr");
    /**
     * The {@code init_bwe.sbwe_unknown_plr_enforced_by_rtt} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_UNKNOWN_PLR_ENFORCED_BY_RTT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_unknown_plr_enforced_by_rtt");
    /**
     * The {@code init_bwe.sbwe_use_combined_rtt_slide_window} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_USE_COMBINED_RTT_SLIDE_WINDOW = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_use_combined_rtt_slide_window");
    /**
     * The {@code init_bwe.sbwe_use_decr_factor_for_plr_cong} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_USE_DECR_FACTOR_FOR_PLR_CONG = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_use_decr_factor_for_plr_cong");
    /**
     * The {@code init_bwe.sbwe_use_plr_classifier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_USE_PLR_CLASSIFIER = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_use_plr_classifier");
    /**
     * The {@code init_bwe.sbwe_use_remote_est} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SBWE_USE_REMOTE_EST = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sbwe_use_remote_est");
    /**
     * The {@code init_bwe.sender_side_init_rate_increase_factor_fr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SENDER_SIDE_INIT_RATE_INCREASE_FACTOR_FR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sender_side_init_rate_increase_factor_fr");
    /**
     * The {@code init_bwe.sfu_dl_freeze_as_cong_signal_interval_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SFU_DL_FREEZE_AS_CONG_SIGNAL_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sfu_dl_freeze_as_cong_signal_interval_ms");
    /**
     * The {@code init_bwe.sfu_dl_freeze_as_cong_signal_threshold_ms} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SFU_DL_FREEZE_AS_CONG_SIGNAL_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.sfu_dl_freeze_as_cong_signal_threshold_ms");
    /**
     * The {@code init_bwe.skip_forced_signaling} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SKIP_FORCED_SIGNALING = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.skip_forced_signaling");
    /**
     * The {@code init_bwe.slc_goodput_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SLC_GOODPUT_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.slc_goodput_coeff");
    /**
     * The {@code init_bwe.slc_hist_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SLC_HIST_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.slc_hist_coeff");
    /**
     * The {@code init_bwe.slc_init_bwe_coeff} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SLC_INIT_BWE_COEFF = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.slc_init_bwe_coeff");
    /**
     * The {@code init_bwe.slc_offset} voip-param.
     */
    public static final VoipParamKey INIT_BWE_SLC_OFFSET = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.slc_offset");
    /**
     * The {@code init_bwe.stop_probing_after_accept_received} voip-param.
     */
    public static final VoipParamKey INIT_BWE_STOP_PROBING_AFTER_ACCEPT_RECEIVED = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.stop_probing_after_accept_received");
    /**
     * The {@code init_bwe.stop_probing_after_call_start} voip-param.
     */
    public static final VoipParamKey INIT_BWE_STOP_PROBING_AFTER_CALL_START = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.stop_probing_after_call_start");
    /**
     * The {@code init_bwe.stop_probing_before_accept_send} voip-param.
     */
    public static final VoipParamKey INIT_BWE_STOP_PROBING_BEFORE_ACCEPT_SEND = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.stop_probing_before_accept_send");
    /**
     * The {@code init_bwe.stop_ramp_up_when_media_undershoots} voip-param.
     */
    public static final VoipParamKey INIT_BWE_STOP_RAMP_UP_WHEN_MEDIA_UNDERSHOOTS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.stop_ramp_up_when_media_undershoots");
    /**
     * The {@code init_bwe.stop_ramp_up_when_media_undershoots_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_STOP_RAMP_UP_WHEN_MEDIA_UNDERSHOOTS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.stop_ramp_up_when_media_undershoots_threshold");
    /**
     * The {@code init_bwe.trigger_fec_with_uplink_plr} voip-param.
     */
    public static final VoipParamKey INIT_BWE_TRIGGER_FEC_WITH_UPLINK_PLR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.trigger_fec_with_uplink_plr");
    /**
     * The {@code init_bwe.ul_fec_min_remaining_bw_kbps} voip-param.
     */
    public static final VoipParamKey INIT_BWE_UL_FEC_MIN_REMAINING_BW_KBPS = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.ul_fec_min_remaining_bw_kbps");
    /**
     * The {@code init_bwe.ul_fec_worst_dl_plr_thresh} voip-param.
     */
    public static final VoipParamKey INIT_BWE_UL_FEC_WORST_DL_PLR_THRESH = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.ul_fec_worst_dl_plr_thresh");
    /**
     * The {@code init_bwe.update_dyn_video_settings_for_dl} voip-param.
     */
    public static final VoipParamKey INIT_BWE_UPDATE_DYN_VIDEO_SETTINGS_FOR_DL = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.update_dyn_video_settings_for_dl");
    /**
     * The {@code init_bwe.use_fec_rate_fix} voip-param.
     */
    public static final VoipParamKey INIT_BWE_USE_FEC_RATE_FIX = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.use_fec_rate_fix");
    /**
     * The {@code init_bwe.use_last_udst_br} voip-param.
     */
    public static final VoipParamKey INIT_BWE_USE_LAST_UDST_BR = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.use_last_udst_br");
    /**
     * The {@code init_bwe.use_rbe} voip-param.
     */
    public static final VoipParamKey INIT_BWE_USE_RBE = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.use_rbe");
    /**
     * The {@code init_bwe.video_hist_based_480p_time_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_VIDEO_HIST_BASED_480P_TIME_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.video_hist_based_480p_time_threshold");
    /**
     * The {@code init_bwe.video_hist_based_720p_time_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_VIDEO_HIST_BASED_720P_TIME_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.video_hist_based_720p_time_threshold");
    /**
     * The {@code init_bwe.video_hist_based_freeze_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_VIDEO_HIST_BASED_FREEZE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.video_hist_based_freeze_threshold");
    /**
     * The {@code init_bwe.video_hist_based_tx_pkt_loss_threshold} voip-param.
     */
    public static final VoipParamKey INIT_BWE_VIDEO_HIST_BASED_TX_PKT_LOSS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.video_hist_based_tx_pkt_loss_threshold");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_kd} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_KD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_kd");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_ki} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_KI = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_ki");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_kp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_KP = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_kp");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_max_integral} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_MAX_INTEGRAL = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_max_integral");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_min_decrease_ratio} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_MIN_DECREASE_RATIO = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_min_decrease_ratio");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_min_integral} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_MIN_INTEGRAL = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_min_integral");
    /**
     * The {@code init_bwe.wa_pid_controller_ml_cong_signal_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_ML_CONG_SIGNAL_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_ml_cong_signal_multiplier");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_inc_factor_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_INC_FACTOR_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_inc_factor_multiplier");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_init_inc_factor_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_INIT_INC_FACTOR_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_init_inc_factor_multiplier");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_kd} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_KD = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_kd");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_ki} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_KI = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_ki");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_kp} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_KP = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_kp");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_max_integral} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_MAX_INTEGRAL = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_max_integral");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_min_integral} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_MIN_INTEGRAL = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_min_integral");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_setpoint_alpha} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_SETPOINT_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_setpoint_alpha");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_setpoint_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_SETPOINT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_setpoint_multiplier");
    /**
     * The {@code init_bwe.wa_pid_controller_rtt_ramp_up_signal_multiplier} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_CONTROLLER_RTT_RAMP_UP_SIGNAL_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_controller_rtt_ramp_up_signal_multiplier");
    /**
     * The {@code init_bwe.wa_pid_inc_ratio_diff_to_reset} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_INC_RATIO_DIFF_TO_RESET = new VoipParamKey(VoipParamType.FLOAT, "init_bwe.wa_pid_inc_ratio_diff_to_reset");
    /**
     * The {@code init_bwe.wa_pid_inc_ratio_reset_by_init} voip-param.
     */
    public static final VoipParamKey INIT_BWE_WA_PID_INC_RATIO_RESET_BY_INIT = new VoipParamKey(VoipParamType.INTEGER, "init_bwe.wa_pid_inc_ratio_reset_by_init");
    /**
     * The {@code ns.builtin} voip-param.
     */
    public static final VoipParamKey NS_BUILTIN = new VoipParamKey(VoipParamType.INTEGER, "ns.builtin");
    /**
     * The {@code ns.compute_crest_factor} voip-param.
     */
    public static final VoipParamKey NS_COMPUTE_CREST_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "ns.compute_crest_factor");
    /**
     * The {@code ns.compute_sii_snr_metric} voip-param.
     */
    public static final VoipParamKey NS_COMPUTE_SII_SNR_METRIC = new VoipParamKey(VoipParamType.INTEGER, "ns.compute_sii_snr_metric");
    /**
     * The {@code ns.compute_spectral_metrics} voip-param.
     */
    public static final VoipParamKey NS_COMPUTE_SPECTRAL_METRICS = new VoipParamKey(VoipParamType.INTEGER, "ns.compute_spectral_metrics");
    /**
     * The {@code ns.denoiser_intensity} voip-param.
     */
    public static final VoipParamKey NS_DENOISER_INTENSITY = new VoipParamKey(VoipParamType.FLOAT, "ns.denoiser_intensity");
    /**
     * The {@code ns.denoiser_intensity_with_ml_ns} voip-param.
     */
    public static final VoipParamKey NS_DENOISER_INTENSITY_WITH_ML_NS = new VoipParamKey(VoipParamType.FLOAT, "ns.denoiser_intensity_with_ml_ns");
    /**
     * The {@code ns.disable_ml_ns_high_cpu_threshold} voip-param.
     */
    public static final VoipParamKey NS_DISABLE_ML_NS_HIGH_CPU_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "ns.disable_ml_ns_high_cpu_threshold");
    /**
     * The {@code ns.disable_sw_ns_when_builtin_available} voip-param.
     */
    public static final VoipParamKey NS_DISABLE_SW_NS_WHEN_BUILTIN_AVAILABLE = new VoipParamKey(VoipParamType.INTEGER, "ns.disable_sw_ns_when_builtin_available");
    /**
     * The {@code ns.dynamically_update_denoiser_intensity} voip-param.
     */
    public static final VoipParamKey NS_DYNAMICALLY_UPDATE_DENOISER_INTENSITY = new VoipParamKey(VoipParamType.INTEGER, "ns.dynamically_update_denoiser_intensity");
    /**
     * The {@code ns.enable} voip-param.
     */
    public static final VoipParamKey NS_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "ns.enable");
    /**
     * The {@code ns.enable_uwp_ns_metrics} voip-param.
     */
    public static final VoipParamKey NS_ENABLE_UWP_NS_METRICS = new VoipParamKey(VoipParamType.INTEGER, "ns.enable_uwp_ns_metrics");
    /**
     * The {@code ns.log_mic_mode} voip-param.
     */
    public static final VoipParamKey NS_LOG_MIC_MODE = new VoipParamKey(VoipParamType.INTEGER, "ns.log_mic_mode");
    /**
     * The {@code ns.loudness_limit} voip-param.
     */
    public static final VoipParamKey NS_LOUDNESS_LIMIT = new VoipParamKey(VoipParamType.INTEGER, "ns.loudness_limit");
    /**
     * The {@code ns.mc_ferraris_suppression_level} voip-param.
     */
    public static final VoipParamKey NS_MC_FERRARIS_SUPPRESSION_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "ns.mc_ferraris_suppression_level");
    /**
     * The {@code ns.ml_ns_debug_asp_load_only} voip-param.
     */
    public static final VoipParamKey NS_ML_NS_DEBUG_ASP_LOAD_ONLY = new VoipParamKey(VoipParamType.INTEGER, "ns.ml_ns_debug_asp_load_only");
    /**
     * The {@code ns.ml_ns_debug_init_only} voip-param.
     */
    public static final VoipParamKey NS_ML_NS_DEBUG_INIT_ONLY = new VoipParamKey(VoipParamType.INTEGER, "ns.ml_ns_debug_init_only");
    /**
     * The {@code ns.ml_ns_log_sys_available_mem} voip-param.
     */
    public static final VoipParamKey NS_ML_NS_LOG_SYS_AVAILABLE_MEM = new VoipParamKey(VoipParamType.INTEGER, "ns.ml_ns_log_sys_available_mem");
    /**
     * The {@code ns.mode} voip-param.
     */
    public static final VoipParamKey NS_MODE = new VoipParamKey(VoipParamType.INTEGER, "ns.mode");
    /**
     * The {@code ns.model_name} voip-param.
     */
    public static final VoipParamKey NS_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "ns.model_name");
    /**
     * The {@code ns.noise_est_gvad_threshold} voip-param.
     */
    public static final VoipParamKey NS_NOISE_EST_GVAD_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "ns.noise_est_gvad_threshold");
    /**
     * The {@code ns.noise_est_vad_threshold} voip-param.
     */
    public static final VoipParamKey NS_NOISE_EST_VAD_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "ns.noise_est_vad_threshold");
    /**
     * The {@code ns.ns_use_webrtc_m89} voip-param.
     */
    public static final VoipParamKey NS_NS_USE_WEBRTC_M89 = new VoipParamKey(VoipParamType.INTEGER, "ns.ns_use_webrtc_m89");
    /**
     * The {@code ns.output_noise_loudness_hist_enabled} voip-param.
     */
    public static final VoipParamKey NS_OUTPUT_NOISE_LOUDNESS_HIST_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "ns.output_noise_loudness_hist_enabled");
    /**
     * The {@code ns.recreate_audio_proc_route_change} voip-param.
     */
    public static final VoipParamKey NS_RECREATE_AUDIO_PROC_ROUTE_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "ns.recreate_audio_proc_route_change");
    /**
     * The {@code ns.run_metrics_with_ml_ns} voip-param.
     */
    public static final VoipParamKey NS_RUN_METRICS_WITH_ML_NS = new VoipParamKey(VoipParamType.INTEGER, "ns.run_metrics_with_ml_ns");
    /**
     * The {@code ns.run_ml_ns_first} voip-param.
     */
    public static final VoipParamKey NS_RUN_ML_NS_FIRST = new VoipParamKey(VoipParamType.INTEGER, "ns.run_ml_ns_first");
    /**
     * The {@code ns.skip_ml_ns_for_voice_isolation} voip-param.
     */
    public static final VoipParamKey NS_SKIP_ML_NS_FOR_VOICE_ISOLATION = new VoipParamKey(VoipParamType.INTEGER, "ns.skip_ml_ns_for_voice_isolation");
    /**
     * The {@code ns.speaker_mode_policy} voip-param.
     */
    public static final VoipParamKey NS_SPEAKER_MODE_POLICY = new VoipParamKey(VoipParamType.INTEGER, "ns.speaker_mode_policy");
    /**
     * The {@code ns.start_ml_ns_process_at_frame} voip-param.
     */
    public static final VoipParamKey NS_START_ML_NS_PROCESS_AT_FRAME = new VoipParamKey(VoipParamType.INTEGER, "ns.start_ml_ns_process_at_frame");
    /**
     * The {@code ns.sw_only_policy} voip-param.
     */
    public static final VoipParamKey NS_SW_ONLY_POLICY = new VoipParamKey(VoipParamType.INTEGER, "ns.sw_only_policy");
    /**
     * The {@code ns.use_denoiser} voip-param.
     */
    public static final VoipParamKey NS_USE_DENOISER = new VoipParamKey(VoipParamType.INTEGER, "ns.use_denoiser");
    /**
     * The {@code ns.use_denoiser_with_smpl_aec} voip-param.
     */
    public static final VoipParamKey NS_USE_DENOISER_WITH_SMPL_AEC = new VoipParamKey(VoipParamType.INTEGER, "ns.use_denoiser_with_smpl_aec");
    /**
     * The {@code ns.use_dynamic_samp_rate} voip-param.
     */
    public static final VoipParamKey NS_USE_DYNAMIC_SAMP_RATE = new VoipParamKey(VoipParamType.INTEGER, "ns.use_dynamic_samp_rate");
    /**
     * The {@code ns.use_executorch} voip-param.
     */
    public static final VoipParamKey NS_USE_EXECUTORCH = new VoipParamKey(VoipParamType.INTEGER, "ns.use_executorch");
    /**
     * The {@code ns.use_gaussian_vad_noise_metric} voip-param.
     */
    public static final VoipParamKey NS_USE_GAUSSIAN_VAD_NOISE_METRIC = new VoipParamKey(VoipParamType.INTEGER, "ns.use_gaussian_vad_noise_metric");
    /**
     * The {@code ns.use_lower_denoiser_threshold_with_ml_ns} voip-param.
     */
    public static final VoipParamKey NS_USE_LOWER_DENOISER_THRESHOLD_WITH_ML_NS = new VoipParamKey(VoipParamType.INTEGER, "ns.use_lower_denoiser_threshold_with_ml_ns");
    /**
     * The {@code ns.use_ml_ns} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns");
    /**
     * The {@code ns.use_ml_ns_asp_load_safe_mode} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_ASP_LOAD_SAFE_MODE = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_asp_load_safe_mode");
    /**
     * The {@code ns.use_ml_ns_asp_work_thread} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_ASP_WORK_THREAD = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_asp_work_thread");
    /**
     * The {@code ns.use_ml_ns_init_opt} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_INIT_OPT = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_init_opt");
    /**
     * The {@code ns.use_ml_ns_init_safe_mode} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_INIT_SAFE_MODE = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_init_safe_mode");
    /**
     * The {@code ns.use_ml_ns_non_earpiece} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_NON_EARPIECE = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_non_earpiece");
    /**
     * The {@code ns.use_ml_ns_pytorch_api_threads_guard} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_PYTORCH_API_THREADS_GUARD = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_pytorch_api_threads_guard");
    /**
     * The {@code ns.use_ml_ns_use_pytorch_no_pthread_pool_guard} voip-param.
     */
    public static final VoipParamKey NS_USE_ML_NS_USE_PYTORCH_NO_PTHREAD_POOL_GUARD = new VoipParamKey(VoipParamType.INTEGER, "ns.use_ml_ns_use_pytorch_no_pthread_pool_guard");
    /**
     * The {@code ns.use_user_option_for_ml_ns} voip-param.
     */
    public static final VoipParamKey NS_USE_USER_OPTION_FOR_ML_NS = new VoipParamKey(VoipParamType.INTEGER, "ns.use_user_option_for_ml_ns");
    /**
     * The {@code ns.use_user_option_for_ml_ns_bluetooth} voip-param.
     */
    public static final VoipParamKey NS_USE_USER_OPTION_FOR_ML_NS_BLUETOOTH = new VoipParamKey(VoipParamType.INTEGER, "ns.use_user_option_for_ml_ns_bluetooth");
    /**
     * The {@code ns.use_user_option_for_ml_ns_headset} voip-param.
     */
    public static final VoipParamKey NS_USE_USER_OPTION_FOR_ML_NS_HEADSET = new VoipParamKey(VoipParamType.INTEGER, "ns.use_user_option_for_ml_ns_headset");
    /**
     * The {@code ns.use_user_option_for_ml_ns_speaker} voip-param.
     */
    public static final VoipParamKey NS_USE_USER_OPTION_FOR_ML_NS_SPEAKER = new VoipParamKey(VoipParamType.INTEGER, "ns.use_user_option_for_ml_ns_speaker");
    /**
     * The {@code ns.use_wa_ml_ns_intensity_impl} voip-param.
     */
    public static final VoipParamKey NS_USE_WA_ML_NS_INTENSITY_IMPL = new VoipParamKey(VoipParamType.INTEGER, "ns.use_wa_ml_ns_intensity_impl");
    /**
     * The {@code options.add_hbh_fec_ssrc_to_stream_descriptor} voip-param.
     */
    public static final VoipParamKey OPTIONS_ADD_HBH_FEC_SSRC_TO_STREAM_DESCRIPTOR = new VoipParamKey(VoipParamType.INTEGER, "options.add_hbh_fec_ssrc_to_stream_descriptor");
    /**
     * The {@code options.af_opt_af_check_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_AF_CHECK_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_af_check_period_ms");
    /**
     * The {@code options.af_opt_max_af_switch_cnt} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_MAX_AF_SWITCH_CNT = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_max_af_switch_cnt");
    /**
     * The {@code options.af_opt_min_mos_diff} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_MIN_MOS_DIFF = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_min_mos_diff");
    /**
     * The {@code options.af_opt_prefer_v4} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_PREFER_V4 = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_prefer_v4");
    /**
     * The {@code options.af_opt_reset_relay_when_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_RESET_RELAY_WHEN_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_reset_relay_when_switch");
    /**
     * The {@code options.af_opt_supported_net_medium_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_SUPPORTED_NET_MEDIUM_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_supported_net_medium_bitmap");
    /**
     * The {@code options.af_opt_switch_af_only_when_using_relay} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPT_SWITCH_AF_ONLY_WHEN_USING_RELAY = new VoipParamKey(VoipParamType.INTEGER, "options.af_opt_switch_af_only_when_using_relay");
    /**
     * The {@code options.af_optimizer_disable_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPTIMIZER_DISABLE_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.af_optimizer_disable_switch");
    /**
     * The {@code options.af_optimizer_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_AF_OPTIMIZER_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.af_optimizer_enable");
    /**
     * The {@code options.alloc_err_unauth_src_addr_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOC_ERR_UNAUTH_SRC_ADDR_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.alloc_err_unauth_src_addr_timeout");
    /**
     * The {@code options.alloc_error_enable_relay_latency_cnt_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOC_ERROR_ENABLE_RELAY_LATENCY_CNT_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.alloc_error_enable_relay_latency_cnt_fix");
    /**
     * The {@code options.alloc_error_handled_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOC_ERROR_HANDLED_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.alloc_error_handled_bitmap");
    /**
     * The {@code options.allow_alt_net_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOW_ALT_NET_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.allow_alt_net_strategy");
    /**
     * The {@code options.allow_hosted_jid_match_regular_device_jid} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOW_HOSTED_JID_MATCH_REGULAR_DEVICE_JID = new VoipParamKey(VoipParamType.INTEGER, "options.allow_hosted_jid_match_regular_device_jid");
    /**
     * The {@code options.allow_tcp_concurrent_asock_ioqueue_key} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALLOW_TCP_CONCURRENT_ASOCK_IOQUEUE_KEY = new VoipParamKey(VoipParamType.INTEGER, "options.allow_tcp_concurrent_asock_ioqueue_key");
    /**
     * The {@code options.alt_af_check_max_default_af_relay_bind_time_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_AF_CHECK_MAX_DEFAULT_AF_RELAY_BIND_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "options.alt_af_check_max_default_af_relay_bind_time_ms");
    /**
     * The {@code options.alt_af_ip_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_AF_IP_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.alt_af_ip_enable");
    /**
     * The {@code options.alt_af_ip_max_probe_alloc_retries} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_AF_IP_MAX_PROBE_ALLOC_RETRIES = new VoipParamKey(VoipParamType.INTEGER, "options.alt_af_ip_max_probe_alloc_retries");
    /**
     * The {@code options.alt_af_ip_probe_alloc_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_AF_IP_PROBE_ALLOC_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.alt_af_ip_probe_alloc_interval_ms");
    /**
     * The {@code options.alt_net_bandwidth_min_sample} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_BANDWIDTH_MIN_SAMPLE = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_bandwidth_min_sample");
    /**
     * The {@code options.alt_net_bandwidth_threshold_kbps} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_BANDWIDTH_THRESHOLD_KBPS = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_bandwidth_threshold_kbps");
    /**
     * The {@code options.alt_net_ping_burst} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_PING_BURST = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_ping_burst");
    /**
     * The {@code options.alt_net_support_ipv6_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_SUPPORT_IPV6_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_support_ipv6_bitmap");
    /**
     * The {@code options.alt_net_switch_opt_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_SWITCH_OPT_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_switch_opt_bitmap");
    /**
     * The {@code options.alt_net_switch_opt_min_sample} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALT_NET_SWITCH_OPT_MIN_SAMPLE = new VoipParamKey(VoipParamType.INTEGER, "options.alt_net_switch_opt_min_sample");
    /**
     * The {@code options.always_update_audio_stream} voip-param.
     */
    public static final VoipParamKey OPTIONS_ALWAYS_UPDATE_AUDIO_STREAM = new VoipParamKey(VoipParamType.INTEGER, "options.always_update_audio_stream");
    /**
     * The {@code options.app_data_add_ssrc_to_stream_descriptor} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_ADD_SSRC_TO_STREAM_DESCRIPTOR = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_add_ssrc_to_stream_descriptor");
    /**
     * The {@code options.app_data_allocate_circ_buffer_for_rx_stream_only} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_ALLOCATE_CIRC_BUFFER_FOR_RX_STREAM_ONLY = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_allocate_circ_buffer_for_rx_stream_only");
    /**
     * The {@code options.app_data_allow_dup_rtp_packets} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_ALLOW_DUP_RTP_PACKETS = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_allow_dup_rtp_packets");
    /**
     * The {@code options.app_data_allow_out_of_order_rtp_packets} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_ALLOW_OUT_OF_ORDER_RTP_PACKETS = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_allow_out_of_order_rtp_packets");
    /**
     * The {@code options.app_data_stream_resend_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_STREAM_RESEND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_stream_resend_interval_ms");
    /**
     * The {@code options.app_data_stream_version} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_STREAM_VERSION = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_stream_version");
    /**
     * The {@code options.app_data_use_slab_allocator} voip-param.
     */
    public static final VoipParamKey OPTIONS_APP_DATA_USE_SLAB_ALLOCATOR = new VoipParamKey(VoipParamType.INTEGER, "options.app_data_use_slab_allocator");
    /**
     * The {@code options.asock_max_packets_per_loop} voip-param.
     */
    public static final VoipParamKey OPTIONS_ASOCK_MAX_PACKETS_PER_LOOP = new VoipParamKey(VoipParamType.INTEGER, "options.asock_max_packets_per_loop");
    /**
     * The {@code options.assert_host_addr_zero_port} voip-param.
     */
    public static final VoipParamKey OPTIONS_ASSERT_HOST_ADDR_ZERO_PORT = new VoipParamKey(VoipParamType.INTEGER, "options.assert_host_addr_zero_port");
    /**
     * The {@code options.assert_on_mutex_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_ASSERT_ON_MUTEX_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.assert_on_mutex_timeout");
    /**
     * The {@code options.aud_lqm_stats_calc_min_kbps} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_LQM_STATS_CALC_MIN_KBPS = new VoipParamKey(VoipParamType.INTEGER, "options.aud_lqm_stats_calc_min_kbps");
    /**
     * The {@code options.aud_nack_type} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_NACK_TYPE = new VoipParamKey(VoipParamType.INTEGER, "options.aud_nack_type");
    /**
     * The {@code options.aud_reserve_on_mute_state_change_in_bps} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_RESERVE_ON_MUTE_STATE_CHANGE_IN_BPS = new VoipParamKey(VoipParamType.INTEGER, "options.aud_reserve_on_mute_state_change_in_bps");
    /**
     * The {@code options.aud_restart_silence_det_freq_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_RESTART_SILENCE_DET_FREQ_MS = new VoipParamKey(VoipParamType.INTEGER, "options.aud_restart_silence_det_freq_ms");
    /**
     * The {@code options.aud_restart_silence_det_init_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_RESTART_SILENCE_DET_INIT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.aud_restart_silence_det_init_ms");
    /**
     * The {@code options.aud_share_mixer_release_time_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_SHARE_MIXER_RELEASE_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "options.aud_share_mixer_release_time_ms");
    /**
     * The {@code options.aud_share_mixer_samp_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_SHARE_MIXER_SAMP_RATE = new VoipParamKey(VoipParamType.INTEGER, "options.aud_share_mixer_samp_rate");
    /**
     * The {@code options.aud_share_mixer_target_gain} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_SHARE_MIXER_TARGET_GAIN = new VoipParamKey(VoipParamType.FLOAT, "options.aud_share_mixer_target_gain");
    /**
     * The {@code options.aud_stream_update_last_decode_ts_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUD_STREAM_UPDATE_LAST_DECODE_TS_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.aud_stream_update_last_decode_ts_fix");
    /**
     * The {@code options.audio_capping_pause_sn_window_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_CAPPING_PAUSE_SN_WINDOW_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.audio_capping_pause_sn_window_msec");
    /**
     * The {@code options.audio_cb_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_CB_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.audio_cb_threshold");
    /**
     * The {@code options.audio_decoder_do_not_pad_zeros} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_DECODER_DO_NOT_PAD_ZEROS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_decoder_do_not_pad_zeros");
    /**
     * The {@code options.audio_encode_offload} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_ENCODE_OFFLOAD = new VoipParamKey(VoipParamType.INTEGER, "options.audio_encode_offload");
    /**
     * The {@code options.audio_fps_max_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_FPS_MAX_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.audio_fps_max_threshold");
    /**
     * The {@code options.audio_fps_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_FPS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.audio_fps_threshold");
    /**
     * The {@code options.audio_health_report_interval_in_sec} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_HEALTH_REPORT_INTERVAL_IN_SEC = new VoipParamKey(VoipParamType.INTEGER, "options.audio_health_report_interval_in_sec");
    /**
     * The {@code options.audio_jitbuf_buffer_limits_window_size_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_JITBUF_BUFFER_LIMITS_WINDOW_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_jitbuf_buffer_limits_window_size_ms");
    /**
     * The {@code options.audio_jitbuf_buffer_lower_limit_scale_percent} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_JITBUF_BUFFER_LOWER_LIMIT_SCALE_PERCENT = new VoipParamKey(VoipParamType.INTEGER, "options.audio_jitbuf_buffer_lower_limit_scale_percent");
    /**
     * The {@code options.audio_level_capacity} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_LEVEL_CAPACITY = new VoipParamKey(VoipParamType.INTEGER, "options.audio_level_capacity");
    /**
     * The {@code options.audio_level_history_duration_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_LEVEL_HISTORY_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_level_history_duration_ms");
    /**
     * The {@code options.audio_level_num_lsb_to_zero} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_LEVEL_NUM_LSB_TO_ZERO = new VoipParamKey(VoipParamType.INTEGER, "options.audio_level_num_lsb_to_zero");
    /**
     * The {@code options.audio_metrics_stft_source} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_METRICS_STFT_SOURCE = new VoipParamKey(VoipParamType.INTEGER, "options.audio_metrics_stft_source");
    /**
     * The {@code options.audio_nack_enable_renack} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_ENABLE_RENACK = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_enable_renack");
    /**
     * The {@code options.audio_nack_jitter_multiplier} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_JITTER_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "options.audio_nack_jitter_multiplier");
    /**
     * The {@code options.audio_nack_max_seq_req} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_MAX_SEQ_REQ = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_max_seq_req");
    /**
     * The {@code options.audio_nack_pri_fec} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_PRI_FEC = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_pri_fec");
    /**
     * The {@code options.audio_nack_recheck_on_recv} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_RECHECK_ON_RECV = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_recheck_on_recv");
    /**
     * The {@code options.audio_nack_renack_min_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_RENACK_MIN_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_renack_min_interval_ms");
    /**
     * The {@code options.audio_nack_renack_rtt_multiplier} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_RENACK_RTT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "options.audio_nack_renack_rtt_multiplier");
    /**
     * The {@code options.audio_nack_rtt_discount_factor} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_RTT_DISCOUNT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "options.audio_nack_rtt_discount_factor");
    /**
     * The {@code options.audio_nack_seq_min_delay} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_NACK_SEQ_MIN_DELAY = new VoipParamKey(VoipParamType.INTEGER, "options.audio_nack_seq_min_delay");
    /**
     * The {@code options.audio_piggyback_enable_cache} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_PIGGYBACK_ENABLE_CACHE = new VoipParamKey(VoipParamType.INTEGER, "options.audio_piggyback_enable_cache");
    /**
     * The {@code options.audio_restart_before_fallback_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_RESTART_BEFORE_FALLBACK_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.audio_restart_before_fallback_count");
    /**
     * The {@code options.audio_stream_ts_logger_log_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_STREAM_TS_LOGGER_LOG_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_stream_ts_logger_log_period_ms");
    /**
     * The {@code options.audio_ts_jitter_use_frame_ts} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_TS_JITTER_USE_FRAME_TS = new VoipParamKey(VoipParamType.INTEGER, "options.audio_ts_jitter_use_frame_ts");
    /**
     * The {@code options.audio_video_clock_src_sync} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUDIO_VIDEO_CLOCK_SRC_SYNC = new VoipParamKey(VoipParamType.INTEGER, "options.audio_video_clock_src_sync");
    /**
     * The {@code options.automos_rx_init_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUTOMOS_RX_INIT_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.automos_rx_init_delay_ms");
    /**
     * The {@code options.automos_tx_init_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AUTOMOS_TX_INIT_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.automos_tx_init_delay_ms");
    /**
     * The {@code options.av_drift_at_render} voip-param.
     */
    public static final VoipParamKey OPTIONS_AV_DRIFT_AT_RENDER = new VoipParamKey(VoipParamType.INTEGER, "options.av_drift_at_render");
    /**
     * The {@code options.av_sync_dtx_offset_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AV_SYNC_DTX_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "options.av_sync_dtx_offset_ms");
    /**
     * The {@code options.av_sync_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AV_SYNC_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.av_sync_threshold_ms");
    /**
     * The {@code options.avsync_feedback_to_audio_fraction} voip-param.
     */
    public static final VoipParamKey OPTIONS_AVSYNC_FEEDBACK_TO_AUDIO_FRACTION = new VoipParamKey(VoipParamType.FLOAT, "options.avsync_feedback_to_audio_fraction");
    /**
     * The {@code options.avsync_feedback_to_audio_max_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AVSYNC_FEEDBACK_TO_AUDIO_MAX_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.avsync_feedback_to_audio_max_threshold_ms");
    /**
     * The {@code options.avsync_feedback_to_audio_min_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AVSYNC_FEEDBACK_TO_AUDIO_MIN_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.avsync_feedback_to_audio_min_threshold_ms");
    /**
     * The {@code options.avsync_feedback_to_audio_update_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_AVSYNC_FEEDBACK_TO_AUDIO_UPDATE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.avsync_feedback_to_audio_update_interval_ms");
    /**
     * The {@code options.avsync_feedback_to_audio_weight} voip-param.
     */
    public static final VoipParamKey OPTIONS_AVSYNC_FEEDBACK_TO_AUDIO_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "options.avsync_feedback_to_audio_weight");
    /**
     * The {@code options.backup_signaling_over_rtcp_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_BACKUP_SIGNALING_OVER_RTCP_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.backup_signaling_over_rtcp_bitmap");
    /**
     * The {@code options.batch_rl_after_media_start} voip-param.
     */
    public static final VoipParamKey OPTIONS_BATCH_RL_AFTER_MEDIA_START = new VoipParamKey(VoipParamType.INTEGER, "options.batch_rl_after_media_start");
    /**
     * The {@code options.batched_relay_latencies_send_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_BATCHED_RELAY_LATENCIES_SEND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.batched_relay_latencies_send_interval_ms");
    /**
     * The {@code options.batched_relay_latencies_send_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_BATCHED_RELAY_LATENCIES_SEND_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.batched_relay_latencies_send_size");
    /**
     * The {@code options.battery_low_threshold_pct} voip-param.
     */
    public static final VoipParamKey OPTIONS_BATTERY_LOW_THRESHOLD_PCT = new VoipParamKey(VoipParamType.FLOAT, "options.battery_low_threshold_pct");
    /**
     * The {@code options.bot_call_delay_caller_relay_lat} voip-param.
     */
    public static final VoipParamKey OPTIONS_BOT_CALL_DELAY_CALLER_RELAY_LAT = new VoipParamKey(VoipParamType.INTEGER, "options.bot_call_delay_caller_relay_lat");
    /**
     * The {@code options.bot_call_send_relay_lat_trigger_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_BOT_CALL_SEND_RELAY_LAT_TRIGGER_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.bot_call_send_relay_lat_trigger_bitmap");
    /**
     * The {@code options.broadcast_interruption_state_on_stream_failure} voip-param.
     */
    public static final VoipParamKey OPTIONS_BROADCAST_INTERRUPTION_STATE_ON_STREAM_FAILURE = new VoipParamKey(VoipParamType.INTEGER, "options.broadcast_interruption_state_on_stream_failure");
    /**
     * The {@code options.buffer_flush_max_length_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_BUFFER_FLUSH_MAX_LENGTH_MS = new VoipParamKey(VoipParamType.INTEGER, "options.buffer_flush_max_length_ms");
    /**
     * The {@code options.bwe_update_before_applying_rc_dyn} voip-param.
     */
    public static final VoipParamKey OPTIONS_BWE_UPDATE_BEFORE_APPLYING_RC_DYN = new VoipParamKey(VoipParamType.INTEGER, "options.bwe_update_before_applying_rc_dyn");
    /**
     * The {@code options.call_connect_stat_type_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_CONNECT_STAT_TYPE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.call_connect_stat_type_bitmap");
    /**
     * The {@code options.call_reaction_clear_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REACTION_CLEAR_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.call_reaction_clear_interval_ms");
    /**
     * The {@code options.call_reaction_clear_timer_frequency_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REACTION_CLEAR_TIMER_FREQUENCY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.call_reaction_clear_timer_frequency_ms");
    /**
     * The {@code options.call_reactions_retransmission_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REACTIONS_RETRANSMISSION_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.call_reactions_retransmission_timeout_ms");
    /**
     * The {@code options.call_replayer_max_num_sources} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REPLAYER_MAX_NUM_SOURCES = new VoipParamKey(VoipParamType.INTEGER, "options.call_replayer_max_num_sources");
    /**
     * The {@code options.call_replayer_sampling_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REPLAYER_SAMPLING_RATE = new VoipParamKey(VoipParamType.FLOAT, "options.call_replayer_sampling_rate");
    /**
     * The {@code options.call_replayer_tag} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REPLAYER_TAG = new VoipParamKey(VoipParamType.STRING, "options.call_replayer_tag");
    /**
     * The {@code options.call_replayer_use_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALL_REPLAYER_USE_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.call_replayer_use_v2");
    /**
     * The {@code options.callee_precall_e2e_bind_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALLEE_PRECALL_E2E_BIND_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.callee_precall_e2e_bind_interval_ms");
    /**
     * The {@code options.caller_lonely_state_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALLER_LONELY_STATE_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.caller_lonely_state_timeout");
    /**
     * The {@code options.caller_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_CALLER_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.caller_timeout");
    /**
     * The {@code options.capi_enable_sfu_on_av_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_CAPI_ENABLE_SFU_ON_AV_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.capi_enable_sfu_on_av_switch");
    /**
     * The {@code options.capture_dev_skip_dupe_frames} voip-param.
     */
    public static final VoipParamKey OPTIONS_CAPTURE_DEV_SKIP_DUPE_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "options.capture_dev_skip_dupe_frames");
    /**
     * The {@code options.check_active_net_interface} voip-param.
     */
    public static final VoipParamKey OPTIONS_CHECK_ACTIVE_NET_INTERFACE = new VoipParamKey(VoipParamType.INTEGER, "options.check_active_net_interface");
    /**
     * The {@code options.check_active_net_interval_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_CHECK_ACTIVE_NET_INTERVAL_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.check_active_net_interval_in_msec");
    /**
     * The {@code options.check_alt_net_state_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_CHECK_ALT_NET_STATE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.check_alt_net_state_bitmap");
    /**
     * The {@code options.circ_buf_len_mutex_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_CIRC_BUF_LEN_MUTEX_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.circ_buf_len_mutex_enabled");
    /**
     * The {@code options.client_relay_election_latency_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CLIENT_RELAY_ELECTION_LATENCY_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.client_relay_election_latency_threshold_ms");
    /**
     * The {@code options.client_relay_election_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_CLIENT_RELAY_ELECTION_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.client_relay_election_strategy");
    /**
     * The {@code options.clock_callback_process_step} voip-param.
     */
    public static final VoipParamKey OPTIONS_CLOCK_CALLBACK_PROCESS_STEP = new VoipParamKey(VoipParamType.INTEGER, "options.clock_callback_process_step");
    /**
     * The {@code options.clock_thread_audio_prio_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_CLOCK_THREAD_AUDIO_PRIO_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.clock_thread_audio_prio_bitmap");
    /**
     * The {@code options.cng} voip-param.
     */
    public static final VoipParamKey OPTIONS_CNG = new VoipParamKey(VoipParamType.INTEGER, "options.cng");
    /**
     * The {@code options.codec_avatar_duplex_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_CODEC_AVATAR_DUPLEX_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.codec_avatar_duplex_mode");
    /**
     * The {@code options.codec_avatar_processing_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CODEC_AVATAR_PROCESSING_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.codec_avatar_processing_delay_ms");
    /**
     * The {@code options.collect_pinning_view_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_COLLECT_PINNING_VIEW_STATS = new VoipParamKey(VoipParamType.INTEGER, "options.collect_pinning_view_stats");
    /**
     * The {@code options.compute_rx_audio_level} voip-param.
     */
    public static final VoipParamKey OPTIONS_COMPUTE_RX_AUDIO_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "options.compute_rx_audio_level");
    /**
     * The {@code options.conf_bridge_sampling_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONF_BRIDGE_SAMPLING_RATE = new VoipParamKey(VoipParamType.INTEGER, "options.conf_bridge_sampling_rate");
    /**
     * The {@code options.conf_mix_cnt_limt} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONF_MIX_CNT_LIMT = new VoipParamKey(VoipParamType.INTEGER, "options.conf_mix_cnt_limt");
    /**
     * The {@code options.connect_bind_success_rate_fail_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECT_BIND_SUCCESS_RATE_FAIL_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.connect_bind_success_rate_fail_threshold");
    /**
     * The {@code options.connected_fix_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_FIX_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.connected_fix_bitmap");
    /**
     * The {@code options.connected_lonely_state_timer_intervals_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_LONELY_STATE_TIMER_INTERVALS_MS = new VoipParamKey(VoipParamType.ARRAY, "options.connected_lonely_state_timer_intervals_ms");
    /**
     * The {@code options.connected_video_gc_bad_jb_empty_pct_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_VIDEO_GC_BAD_JB_EMPTY_PCT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.connected_video_gc_bad_jb_empty_pct_threshold");
    /**
     * The {@code options.connected_video_gc_bad_jb_lost_pct_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_VIDEO_GC_BAD_JB_LOST_PCT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.connected_video_gc_bad_jb_lost_pct_threshold");
    /**
     * The {@code options.connected_video_gc_bad_jb_total_plc_pct_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_VIDEO_GC_BAD_JB_TOTAL_PLC_PCT_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.connected_video_gc_bad_jb_total_plc_pct_threshold");
    /**
     * The {@code options.connected_video_gc_bad_mte_delay_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_VIDEO_GC_BAD_MTE_DELAY_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.connected_video_gc_bad_mte_delay_threshold_ms");
    /**
     * The {@code options.connected_video_gc_bad_mte_net_eq_delay_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTED_VIDEO_GC_BAD_MTE_NET_EQ_DELAY_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.connected_video_gc_bad_mte_net_eq_delay_threshold_ms");
    /**
     * The {@code options.connecting_tone_desc} voip-param.
     */
    public static final VoipParamKey OPTIONS_CONNECTING_TONE_DESC = new VoipParamKey(VoipParamType.STRING, "options.connecting_tone_desc");
    /**
     * The {@code options.cpu_over_utilization_threshold_in_pct} voip-param.
     */
    public static final VoipParamKey OPTIONS_CPU_OVER_UTILIZATION_THRESHOLD_IN_PCT = new VoipParamKey(VoipParamType.INTEGER, "options.cpu_over_utilization_threshold_in_pct");
    /**
     * The {@code options.cpu_sampling_duration_in_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_CPU_SAMPLING_DURATION_IN_MS = new VoipParamKey(VoipParamType.INTEGER, "options.cpu_sampling_duration_in_ms");
    /**
     * The {@code options.cpu_util_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_CPU_UTIL_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.cpu_util_enable");
    /**
     * The {@code options.cr_audio_driver_dsp_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_AUDIO_DRIVER_DSP_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_audio_driver_dsp_bitmask");
    /**
     * The {@code options.cr_audio_driver_dsp_bitmask2} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_AUDIO_DRIVER_DSP_BITMASK2 = new VoipParamKey(VoipParamType.INTEGER, "options.cr_audio_driver_dsp_bitmask2");
    /**
     * The {@code options.cr_audio_rate_control_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_AUDIO_RATE_CONTROL_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_audio_rate_control_bitmask");
    /**
     * The {@code options.cr_audio_stream_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_AUDIO_STREAM_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_audio_stream_bitmask");
    /**
     * The {@code options.cr_history_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_HISTORY_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_history_bitmask");
    /**
     * The {@code options.cr_history_bitmask2} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_HISTORY_BITMASK2 = new VoipParamKey(VoipParamType.INTEGER, "options.cr_history_bitmask2");
    /**
     * The {@code options.cr_mcs_sfu_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_MCS_SFU_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_mcs_sfu_bitmask");
    /**
     * The {@code options.cr_mcs_sfu_bitmask2} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_MCS_SFU_BITMASK2 = new VoipParamKey(VoipParamType.INTEGER, "options.cr_mcs_sfu_bitmask2");
    /**
     * The {@code options.cr_misc_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_MISC_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_misc_bitmask");
    /**
     * The {@code options.cr_ml_feature_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_ML_FEATURE_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_ml_feature_bitmask");
    /**
     * The {@code options.cr_ml_feature_bitmask2} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_ML_FEATURE_BITMASK2 = new VoipParamKey(VoipParamType.INTEGER, "options.cr_ml_feature_bitmask2");
    /**
     * The {@code options.cr_network_conditioner_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_NETWORK_CONDITIONER_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_network_conditioner_bitmask");
    /**
     * The {@code options.cr_platform_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_PLATFORM_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_platform_bitmask");
    /**
     * The {@code options.cr_relay_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_RELAY_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_relay_bitmask");
    /**
     * The {@code options.cr_sender_bwe_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_SENDER_BWE_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_sender_bwe_bitmask");
    /**
     * The {@code options.cr_signaling_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_SIGNALING_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_signaling_bitmask");
    /**
     * The {@code options.cr_transport_network_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_TRANSPORT_NETWORK_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_transport_network_bitmask");
    /**
     * The {@code options.cr_transport_network_bitmask_1} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_TRANSPORT_NETWORK_BITMASK_1 = new VoipParamKey(VoipParamType.INTEGER, "options.cr_transport_network_bitmask_1");
    /**
     * The {@code options.cr_uaqc_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_UAQC_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_uaqc_bitmask");
    /**
     * The {@code options.cr_video_decoding_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_VIDEO_DECODING_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_video_decoding_bitmask");
    /**
     * The {@code options.cr_video_encoding_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_VIDEO_ENCODING_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_video_encoding_bitmask");
    /**
     * The {@code options.cr_video_rate_control_bitmask} voip-param.
     */
    public static final VoipParamKey OPTIONS_CR_VIDEO_RATE_CONTROL_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "options.cr_video_rate_control_bitmask");
    /**
     * The {@code options.data_channel_connection_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_DATA_CHANNEL_CONNECTION_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.data_channel_connection_timeout_ms");
    /**
     * The {@code options.debug_metric_1} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_1 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_1");
    /**
     * The {@code options.debug_metric_2} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_2 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_2");
    /**
     * The {@code options.debug_metric_3} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_3 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_3");
    /**
     * The {@code options.debug_metric_4} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_4 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_4");
    /**
     * The {@code options.debug_metric_5} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_5 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_5");
    /**
     * The {@code options.debug_metric_method_1} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_METHOD_1 = new VoipParamKey(VoipParamType.INTEGER, "options.debug_metric_method_1");
    /**
     * The {@code options.debug_metric_method_2} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_METHOD_2 = new VoipParamKey(VoipParamType.INTEGER, "options.debug_metric_method_2");
    /**
     * The {@code options.debug_metric_method_3} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_METHOD_3 = new VoipParamKey(VoipParamType.INTEGER, "options.debug_metric_method_3");
    /**
     * The {@code options.debug_metric_method_4} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_METHOD_4 = new VoipParamKey(VoipParamType.INTEGER, "options.debug_metric_method_4");
    /**
     * The {@code options.debug_metric_method_5} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_METHOD_5 = new VoipParamKey(VoipParamType.INTEGER, "options.debug_metric_method_5");
    /**
     * The {@code options.debug_metric_pcent_1} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_PCENT_1 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_pcent_1");
    /**
     * The {@code options.debug_metric_pcent_2} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_PCENT_2 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_pcent_2");
    /**
     * The {@code options.debug_metric_pcent_3} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_PCENT_3 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_pcent_3");
    /**
     * The {@code options.debug_metric_pcent_4} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_PCENT_4 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_pcent_4");
    /**
     * The {@code options.debug_metric_pcent_5} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEBUG_METRIC_PCENT_5 = new VoipParamKey(VoipParamType.FLOAT, "options.debug_metric_pcent_5");
    /**
     * The {@code options.deprecate_conn_local_cand} voip-param.
     */
    public static final VoipParamKey OPTIONS_DEPRECATE_CONN_LOCAL_CAND = new VoipParamKey(VoipParamType.INTEGER, "options.deprecate_conn_local_cand");
    /**
     * The {@code options.destroy_warp_mcs_rbwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_DESTROY_WARP_MCS_RBWE = new VoipParamKey(VoipParamType.INTEGER, "options.destroy_warp_mcs_rbwe");
    /**
     * The {@code options.disable_all_ltrp_event_verbose} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_ALL_LTRP_EVENT_VERBOSE = new VoipParamKey(VoipParamType.INTEGER, "options.disable_all_ltrp_event_verbose");
    /**
     * The {@code options.disable_audio_restart_record_silence} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_AUDIO_RESTART_RECORD_SILENCE = new VoipParamKey(VoipParamType.INTEGER, "options.disable_audio_restart_record_silence");
    /**
     * The {@code options.disable_av_sync_for_capi} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_AV_SYNC_FOR_CAPI = new VoipParamKey(VoipParamType.INTEGER, "options.disable_av_sync_for_capi");
    /**
     * The {@code options.disable_cre_bias_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_CRE_BIAS_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.disable_cre_bias_strategy");
    /**
     * The {@code options.disable_freeze_metrics_on_background} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_FREEZE_METRICS_ON_BACKGROUND = new VoipParamKey(VoipParamType.INTEGER, "options.disable_freeze_metrics_on_background");
    /**
     * The {@code options.disable_hbh_nack_p2p_to_relay_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_HBH_NACK_P2P_TO_RELAY_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.disable_hbh_nack_p2p_to_relay_v2");
    /**
     * The {@code options.disable_late_rtcp_bye_on_teardown} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_LATE_RTCP_BYE_ON_TEARDOWN = new VoipParamKey(VoipParamType.INTEGER, "options.disable_late_rtcp_bye_on_teardown");
    /**
     * The {@code options.disable_p2p} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_P2P = new VoipParamKey(VoipParamType.INTEGER, "options.disable_p2p");
    /**
     * The {@code options.disable_p2p_local} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_P2P_LOCAL = new VoipParamKey(VoipParamType.INTEGER, "options.disable_p2p_local");
    /**
     * The {@code options.disable_p2p_only_on_same_subnet} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_P2P_ONLY_ON_SAME_SUBNET = new VoipParamKey(VoipParamType.INTEGER, "options.disable_p2p_only_on_same_subnet");
    /**
     * The {@code options.disable_reconnect_tone} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_RECONNECT_TONE = new VoipParamKey(VoipParamType.INTEGER, "options.disable_reconnect_tone");
    /**
     * The {@code options.disable_report_fatal_record_silence} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_REPORT_FATAL_RECORD_SILENCE = new VoipParamKey(VoipParamType.INTEGER, "options.disable_report_fatal_record_silence");
    /**
     * The {@code options.disable_ssrc_subscription} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISABLE_SSRC_SUBSCRIPTION = new VoipParamKey(VoipParamType.INTEGER, "options.disable_ssrc_subscription");
    /**
     * The {@code options.discard_hbh_rtcp_pkts_from_old_relay} voip-param.
     */
    public static final VoipParamKey OPTIONS_DISCARD_HBH_RTCP_PKTS_FROM_OLD_RELAY = new VoipParamKey(VoipParamType.INTEGER, "options.discard_hbh_rtcp_pkts_from_old_relay");
    /**
     * The {@code options.dl_intest_model_after_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_DL_INTEST_MODEL_AFTER_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.dl_intest_model_after_call");
    /**
     * The {@code options.driver_sampling_rate_max} voip-param.
     */
    public static final VoipParamKey OPTIONS_DRIVER_SAMPLING_RATE_MAX = new VoipParamKey(VoipParamType.INTEGER, "options.driver_sampling_rate_max");
    /**
     * The {@code options.dscp_overwrite} voip-param.
     */
    public static final VoipParamKey OPTIONS_DSCP_OVERWRITE = new VoipParamKey(VoipParamType.INTEGER, "options.dscp_overwrite");
    /**
     * The {@code options.dtls_sctp_extra_header_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTLS_SCTP_EXTRA_HEADER_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.dtls_sctp_extra_header_size");
    /**
     * The {@code options.dtmf_clock_rate_khz} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTMF_CLOCK_RATE_KHZ = new VoipParamKey(VoipParamType.INTEGER, "options.dtmf_clock_rate_khz");
    /**
     * The {@code options.dtmf_event_default_duration_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTMF_EVENT_DEFAULT_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "options.dtmf_event_default_duration_ms");
    /**
     * The {@code options.dtmf_payload_type} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTMF_PAYLOAD_TYPE = new VoipParamKey(VoipParamType.INTEGER, "options.dtmf_payload_type");
    /**
     * The {@code options.dtx_disable_aggressive_report} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTX_DISABLE_AGGRESSIVE_REPORT = new VoipParamKey(VoipParamType.INTEGER, "options.dtx_disable_aggressive_report");
    /**
     * The {@code options.dtx_enable_nack_during_delay_reset} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTX_ENABLE_NACK_DURING_DELAY_RESET = new VoipParamKey(VoipParamType.INTEGER, "options.dtx_enable_nack_during_delay_reset");
    /**
     * The {@code options.dtx_jb_avg_target_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTX_JB_AVG_TARGET_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.dtx_jb_avg_target_size");
    /**
     * The {@code options.dtx_play_saved_samples} voip-param.
     */
    public static final VoipParamKey OPTIONS_DTX_PLAY_SAVED_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "options.dtx_play_saved_samples");
    /**
     * The {@code options.dual_call_trigger} voip-param.
     */
    public static final VoipParamKey OPTIONS_DUAL_CALL_TRIGGER = new VoipParamKey(VoipParamType.INTEGER, "options.dual_call_trigger");
    /**
     * The {@code options.eager_video_preview_on_outgoing_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_EAGER_VIDEO_PREVIEW_ON_OUTGOING_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.eager_video_preview_on_outgoing_call");
    /**
     * The {@code options.ecn_overwrite} voip-param.
     */
    public static final VoipParamKey OPTIONS_ECN_OVERWRITE = new VoipParamKey(VoipParamType.INTEGER, "options.ecn_overwrite");
    /**
     * The {@code options.elapsed_msec_since_last_pong_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_ELAPSED_MSEC_SINCE_LAST_PONG_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.elapsed_msec_since_last_pong_threshold");
    /**
     * The {@code options.elapsed_msec_since_last_rtp_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_ELAPSED_MSEC_SINCE_LAST_RTP_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.elapsed_msec_since_last_rtp_threshold");
    /**
     * The {@code options.empty_frm_buf_on_decoder_disconnect} voip-param.
     */
    public static final VoipParamKey OPTIONS_EMPTY_FRM_BUF_ON_DECODER_DISCONNECT = new VoipParamKey(VoipParamType.INTEGER, "options.empty_frm_buf_on_decoder_disconnect");
    /**
     * The {@code options.enable_3p_group_call_openh264_320x240_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_3P_GROUP_CALL_OPENH264_320X240_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_3p_group_call_openh264_320x240_fix");
    /**
     * The {@code options.enable_48khz_rtp_clock} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_48KHZ_RTP_CLOCK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_48khz_rtp_clock");
    /**
     * The {@code options.enable_additional_dtx_frames_at_call_start_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ADDITIONAL_DTX_FRAMES_AT_CALL_START_MS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_additional_dtx_frames_at_call_start_ms");
    /**
     * The {@code options.enable_adj_enc_res_by_peer} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ADJ_ENC_RES_BY_PEER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_adj_enc_res_by_peer");
    /**
     * The {@code options.enable_advanced_group_call_key_exchange} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ADVANCED_GROUP_CALL_KEY_EXCHANGE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_advanced_group_call_key_exchange");
    /**
     * The {@code options.enable_alloc_err_mi_chk} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ALLOC_ERR_MI_CHK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_alloc_err_mi_chk");
    /**
     * The {@code options.enable_alloc_error_handling} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ALLOC_ERROR_HANDLING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_alloc_error_handling");
    /**
     * The {@code options.enable_alt_af_other_addr_valid_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ALT_AF_OTHER_ADDR_VALID_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_alt_af_other_addr_valid_check");
    /**
     * The {@code options.enable_always_handle_p2p_probe_only} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ALWAYS_HANDLE_P2P_PROBE_ONLY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_always_handle_p2p_probe_only");
    /**
     * The {@code options.enable_always_handle_p2p_stun} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ALWAYS_HANDLE_P2P_STUN = new VoipParamKey(VoipParamType.INTEGER, "options.enable_always_handle_p2p_stun");
    /**
     * The {@code options.enable_and_group_call_aspect_ratio_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AND_GROUP_CALL_ASPECT_RATIO_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_and_group_call_aspect_ratio_fix");
    /**
     * The {@code options.enable_android_high_res_capture} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ANDROID_HIGH_RES_CAPTURE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_android_high_res_capture");
    /**
     * The {@code options.enable_app_data_controller} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_APP_DATA_CONTROLLER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_app_data_controller");
    /**
     * The {@code options.enable_app_data_stream} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_APP_DATA_STREAM = new VoipParamKey(VoipParamType.INTEGER, "options.enable_app_data_stream");
    /**
     * The {@code options.enable_app_data_stream_performance_test} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_APP_DATA_STREAM_PERFORMANCE_TEST = new VoipParamKey(VoipParamType.INTEGER, "options.enable_app_data_stream_performance_test");
    /**
     * The {@code options.enable_apply_network_info_to_secondary_vid_stream} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_APPLY_NETWORK_INFO_TO_SECONDARY_VID_STREAM = new VoipParamKey(VoipParamType.INTEGER, "options.enable_apply_network_info_to_secondary_vid_stream");
    /**
     * The {@code options.enable_aud_over_p2p_in_vid_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUD_OVER_P2P_IN_VID_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_aud_over_p2p_in_vid_call");
    /**
     * The {@code options.enable_aud_rec_thread_high_pri} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUD_REC_THREAD_HIGH_PRI = new VoipParamKey(VoipParamType.INTEGER, "options.enable_aud_rec_thread_high_pri");
    /**
     * The {@code options.enable_aud_share_mixer} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUD_SHARE_MIXER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_aud_share_mixer");
    /**
     * The {@code options.enable_aud_share_resampling} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUD_SHARE_RESAMPLING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_aud_share_resampling");
    /**
     * The {@code options.enable_aud_tx_traffic_started_event} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUD_TX_TRAFFIC_STARTED_EVENT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_aud_tx_traffic_started_event");
    /**
     * The {@code options.enable_audio_capping_on_edgeray} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUDIO_CAPPING_ON_EDGERAY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_audio_capping_on_edgeray");
    /**
     * The {@code options.enable_audio_driver_early_init_for_callee} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUDIO_DRIVER_EARLY_INIT_FOR_CALLEE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_audio_driver_early_init_for_callee");
    /**
     * The {@code options.enable_audio_record_cb_drain} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUDIO_RECORD_CB_DRAIN = new VoipParamKey(VoipParamType.INTEGER, "options.enable_audio_record_cb_drain");
    /**
     * The {@code options.enable_audio_target_include_secondary} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUDIO_TARGET_INCLUDE_SECONDARY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_audio_target_include_secondary");
    /**
     * The {@code options.enable_audiodrop_on_peer_interrupted} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AUDIODROP_ON_PEER_INTERRUPTED = new VoipParamKey(VoipParamType.INTEGER, "options.enable_audiodrop_on_peer_interrupted");
    /**
     * The {@code options.enable_av_sync_at_dtx} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AV_SYNC_AT_DTX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_av_sync_at_dtx");
    /**
     * The {@code options.enable_av_sync_dtx_interpolation} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AV_SYNC_DTX_INTERPOLATION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_av_sync_dtx_interpolation");
    /**
     * The {@code options.enable_avsync_feedback_ingestion_neteq} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AVSYNC_FEEDBACK_INGESTION_NETEQ = new VoipParamKey(VoipParamType.INTEGER, "options.enable_avsync_feedback_ingestion_neteq");
    /**
     * The {@code options.enable_avsync_feedback_to_audio} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_AVSYNC_FEEDBACK_TO_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_avsync_feedback_to_audio");
    /**
     * The {@code options.enable_batch_drain_network_events} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_BATCH_DRAIN_NETWORK_EVENTS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_batch_drain_network_events");
    /**
     * The {@code options.enable_bitrate_stat_for_field_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_BITRATE_STAT_FOR_FIELD_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_bitrate_stat_for_field_stat");
    /**
     * The {@code options.enable_biz_calling_afb} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_BIZ_CALLING_AFB = new VoipParamKey(VoipParamType.INTEGER, "options.enable_biz_calling_afb");
    /**
     * The {@code options.enable_bwe_send_ts_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_BWE_SEND_TS_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_bwe_send_ts_fix");
    /**
     * The {@code options.enable_call_context_ts_logging} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CALL_CONTEXT_TS_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_call_context_ts_logging");
    /**
     * The {@code options.enable_capabilities_ownership_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CAPABILITIES_OWNERSHIP_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_capabilities_ownership_fix");
    /**
     * The {@code options.enable_capi_av_sync_ts_reconciliation} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CAPI_AV_SYNC_TS_RECONCILIATION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_capi_av_sync_ts_reconciliation");
    /**
     * The {@code options.enable_capture_port_recreate_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CAPTURE_PORT_RECREATE_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_capture_port_recreate_fix");
    /**
     * The {@code options.enable_cell_signal_optimizations} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CELL_SIGNAL_OPTIMIZATIONS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_cell_signal_optimizations");
    /**
     * The {@code options.enable_client_signaling_ts_logger} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CLIENT_SIGNALING_TS_LOGGER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_client_signaling_ts_logger");
    /**
     * The {@code options.enable_client_ts_logger} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CLIENT_TS_LOGGER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_client_ts_logger");
    /**
     * The {@code options.enable_clock_thread_pause_resume} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CLOCK_THREAD_PAUSE_RESUME = new VoipParamKey(VoipParamType.INTEGER, "options.enable_clock_thread_pause_resume");
    /**
     * The {@code options.enable_conf_bridge_ml_ns_override} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CONF_BRIDGE_ML_NS_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_conf_bridge_ml_ns_override");
    /**
     * The {@code options.enable_crash_fix_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_CRASH_FIX_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_crash_fix_bitmap");
    /**
     * The {@code options.enable_data_channel} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DATA_CHANNEL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_data_channel");
    /**
     * The {@code options.enable_dec_mutex_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DEC_MUTEX_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_dec_mutex_fix");
    /**
     * The {@code options.enable_default_h264decoder} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DEFAULT_H264DECODER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_default_h264decoder");
    /**
     * The {@code options.enable_destroy_init_bwe_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DESTROY_INIT_BWE_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_destroy_init_bwe_fix");
    /**
     * The {@code options.enable_device_clock_rate_update} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DEVICE_CLOCK_RATE_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_device_clock_rate_update");
    /**
     * The {@code options.enable_device_timestamps} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DEVICE_TIMESTAMPS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_device_timestamps");
    /**
     * The {@code options.enable_dtmf_rfc4733_support} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DTMF_RFC4733_SUPPORT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_dtmf_rfc4733_support");
    /**
     * The {@code options.enable_dtx_follow_opus_standard} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DTX_FOLLOW_OPUS_STANDARD = new VoipParamKey(VoipParamType.INTEGER, "options.enable_dtx_follow_opus_standard");
    /**
     * The {@code options.enable_dual_stream_screen_share} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_DUAL_STREAM_SCREEN_SHARE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_dual_stream_screen_share");
    /**
     * The {@code options.enable_e2e_bind_probe_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_E2E_BIND_PROBE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_e2e_bind_probe_bitmap");
    /**
     * The {@code options.enable_early_call_state_transition_on_end} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_EARLY_CALL_STATE_TRANSITION_ON_END = new VoipParamKey(VoipParamType.INTEGER, "options.enable_early_call_state_transition_on_end");
    /**
     * The {@code options.enable_early_impl_accept_optimization} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_EARLY_IMPL_ACCEPT_OPTIMIZATION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_early_impl_accept_optimization");
    /**
     * The {@code options.enable_early_return_on_start_stream_error} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_EARLY_RETURN_ON_START_STREAM_ERROR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_early_return_on_start_stream_error");
    /**
     * The {@code options.enable_edgeray_dtls_active_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_EDGERAY_DTLS_ACTIVE_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_edgeray_dtls_active_mode");
    /**
     * The {@code options.enable_encode_preset_latency_report} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_ENCODE_PRESET_LATENCY_REPORT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_encode_preset_latency_report");
    /**
     * The {@code options.enable_ev_thread_race_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_EV_THREAD_RACE_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ev_thread_race_fix");
    /**
     * The {@code options.enable_face_detection} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FACE_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_face_detection");
    /**
     * The {@code options.enable_fast_call_setup_callee_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FAST_CALL_SETUP_CALLEE_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.enable_fast_call_setup_callee_v2");
    /**
     * The {@code options.enable_fine_grained_peer_camera_pause_fs} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FINE_GRAINED_PEER_CAMERA_PAUSE_FS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_fine_grained_peer_camera_pause_fs");
    /**
     * The {@code options.enable_frame_info_based_fmt_update} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FRAME_INFO_BASED_FMT_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_frame_info_based_fmt_update");
    /**
     * The {@code options.enable_frame_merging_for_bot_calls} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FRAME_MERGING_FOR_BOT_CALLS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_frame_merging_for_bot_calls");
    /**
     * The {@code options.enable_freeze_disable_in_call_screen_bg_hook} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_FREEZE_DISABLE_IN_CALL_SCREEN_BG_HOOK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_freeze_disable_in_call_screen_bg_hook");
    /**
     * The {@code options.enable_gate_video_before_destroy} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_GATE_VIDEO_BEFORE_DESTROY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_gate_video_before_destroy");
    /**
     * The {@code options.enable_get_host_cand_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_GET_HOST_CAND_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.enable_get_host_cand_v2");
    /**
     * The {@code options.enable_get_index_from_old_ctx} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_GET_INDEX_FROM_OLD_CTX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_get_index_from_old_ctx");
    /**
     * The {@code options.enable_group_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_GROUP_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_group_call");
    /**
     * The {@code options.enable_group_call_self_preview_size_for_ratio} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_GROUP_CALL_SELF_PREVIEW_SIZE_FOR_RATIO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_group_call_self_preview_size_for_ratio");
    /**
     * The {@code options.enable_handle_relays_for_av_upgrade} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HANDLE_RELAYS_FOR_AV_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_handle_relays_for_av_upgrade");
    /**
     * The {@code options.enable_hbh_compound_rtcp_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_COMPOUND_RTCP_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_compound_rtcp_fix");
    /**
     * The {@code options.enable_hbh_data_channel} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_DATA_CHANNEL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_data_channel");
    /**
     * The {@code options.enable_hbh_nack_audio} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_NACK_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_nack_audio");
    /**
     * The {@code options.enable_hbh_nack_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_NACK_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_nack_video");
    /**
     * The {@code options.enable_hbh_peer_pli_throttle_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_PEER_PLI_THROTTLE_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_peer_pli_throttle_fix");
    /**
     * The {@code options.enable_hbh_pli_cer_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_PLI_CER_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_pli_cer_fix");
    /**
     * The {@code options.enable_hbh_pli_is_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_PLI_IS_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_pli_is_video");
    /**
     * The {@code options.enable_hbh_pli_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_PLI_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_pli_video");
    /**
     * The {@code options.enable_hbh_rtcp_is_video_override} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_RTCP_IS_VIDEO_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_rtcp_is_video_override");
    /**
     * The {@code options.enable_hbh_server_pli_throttle_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SERVER_PLI_THROTTLE_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_server_pli_throttle_fix");
    /**
     * The {@code options.enable_hbh_srtcp_reset_on_hbh_key_change} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTCP_RESET_ON_HBH_KEY_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtcp_reset_on_hbh_key_change");
    /**
     * The {@code options.enable_hbh_srtcp_smlv} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTCP_SMLV = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtcp_smlv");
    /**
     * The {@code options.enable_hbh_srtp_afb} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTP_AFB = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtp_afb");
    /**
     * The {@code options.enable_hbh_srtp_afb_batch} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTP_AFB_BATCH = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtp_afb_batch");
    /**
     * The {@code options.enable_hbh_srtp_reset_on_hbh_key_change} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTP_RESET_ON_HBH_KEY_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtp_reset_on_hbh_key_change");
    /**
     * The {@code options.enable_hbh_srtp_rtp_index_signaling_req_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_SRTP_RTP_INDEX_SIGNALING_REQ_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_srtp_rtp_index_signaling_req_bitmap");
    /**
     * The {@code options.enable_hbh_warp_mi_req_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HBH_WARP_MI_REQ_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hbh_warp_mi_req_bitmap");
    /**
     * The {@code options.enable_history_based_audio_device_preference} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HISTORY_BASED_AUDIO_DEVICE_PREFERENCE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_history_based_audio_device_preference");
    /**
     * The {@code options.enable_hosted_jid_ssrc_calc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_HOSTED_JID_SSRC_CALC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_hosted_jid_ssrc_calc");
    /**
     * The {@code options.enable_immediate_subs_bind_inc_retry} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IMMEDIATE_SUBS_BIND_INC_RETRY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_immediate_subs_bind_inc_retry");
    /**
     * The {@code options.enable_implicit_relay_election_on_rx_dtls} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IMPLICIT_RELAY_ELECTION_ON_RX_DTLS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_implicit_relay_election_on_rx_dtls");
    /**
     * The {@code options.enable_implicit_relay_election_on_rx_pkts} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IMPLICIT_RELAY_ELECTION_ON_RX_PKTS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_implicit_relay_election_on_rx_pkts");
    /**
     * The {@code options.enable_imu_data_stream} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IMU_DATA_STREAM = new VoipParamKey(VoipParamType.INTEGER, "options.enable_imu_data_stream");
    /**
     * The {@code options.enable_imu_data_stream_nack} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IMU_DATA_STREAM_NACK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_imu_data_stream_nack");
    /**
     * The {@code options.enable_init_codecs_on_upgrade} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_INIT_CODECS_ON_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_init_codecs_on_upgrade");
    /**
     * The {@code options.enable_init_quality_fs} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_INIT_QUALITY_FS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_init_quality_fs");
    /**
     * The {@code options.enable_ioqueue_reset_at_end_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IOQUEUE_RESET_AT_END_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ioqueue_reset_at_end_call");
    /**
     * The {@code options.enable_ipv6_loopback_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_IPV6_LOOPBACK_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ipv6_loopback_fix");
    /**
     * The {@code options.enable_jitter_stat_for_field_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_JITTER_STAT_FOR_FIELD_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_jitter_stat_for_field_stat");
    /**
     * The {@code options.enable_l4s_scalable_cc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_L4S_SCALABLE_CC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_l4s_scalable_cc");
    /**
     * The {@code options.enable_list_open_fd} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_LIST_OPEN_FD = new VoipParamKey(VoipParamType.INTEGER, "options.enable_list_open_fd");
    /**
     * The {@code options.enable_loss_info_ext} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_LOSS_INFO_EXT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_loss_info_ext");
    /**
     * The {@code options.enable_media_endpt_thread_high_pri} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MEDIA_ENDPT_THREAD_HIGH_PRI = new VoipParamKey(VoipParamType.INTEGER, "options.enable_media_endpt_thread_high_pri");
    /**
     * The {@code options.enable_media_endpt_thread_high_pri_android} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MEDIA_ENDPT_THREAD_HIGH_PRI_ANDROID = new VoipParamKey(VoipParamType.INTEGER, "options.enable_media_endpt_thread_high_pri_android");
    /**
     * The {@code options.enable_media_hbh_srtp} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MEDIA_HBH_SRTP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_media_hbh_srtp");
    /**
     * The {@code options.enable_media_platform_event_refactor} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MEDIA_PLATFORM_EVENT_REFACTOR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_media_platform_event_refactor");
    /**
     * The {@code options.enable_media_timeout_terminate_reason} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MEDIA_TIMEOUT_TERMINATE_REASON = new VoipParamKey(VoipParamType.INTEGER, "options.enable_media_timeout_terminate_reason");
    /**
     * The {@code options.enable_migrated_unified_api} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MIGRATED_UNIFIED_API = new VoipParamKey(VoipParamType.INTEGER, "options.enable_migrated_unified_api");
    /**
     * The {@code options.enable_mlow_red} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MLOW_RED = new VoipParamKey(VoipParamType.INTEGER, "options.enable_mlow_red");
    /**
     * The {@code options.enable_mock_imu_data_sender} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_MOCK_IMU_DATA_SENDER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_mock_imu_data_sender");
    /**
     * The {@code options.enable_network_health_monitor} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NETWORK_HEALTH_MONITOR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_network_health_monitor");
    /**
     * The {@code options.enable_network_health_status_bc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NETWORK_HEALTH_STATUS_BC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_network_health_status_bc");
    /**
     * The {@code options.enable_network_medium_attr} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NETWORK_MEDIUM_ATTR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_network_medium_attr");
    /**
     * The {@code options.enable_network_medium_bc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NETWORK_MEDIUM_BC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_network_medium_bc");
    /**
     * The {@code options.enable_new_p2p_priority} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NEW_P2P_PRIORITY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_new_p2p_priority");
    /**
     * The {@code options.enable_no_audio_metrics} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NO_AUDIO_METRICS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_no_audio_metrics");
    /**
     * The {@code options.enable_no_reconnecting_indicator_in_self_no_good_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NO_RECONNECTING_INDICATOR_IN_SELF_NO_GOOD_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_no_reconnecting_indicator_in_self_no_good_status");
    /**
     * The {@code options.enable_non_dyn_codec_param_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_NON_DYN_CODEC_PARAM_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_non_dyn_codec_param_fix");
    /**
     * The {@code options.enable_oh264_kf_frame_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_OH264_KF_FRAME_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_oh264_kf_frame_mode");
    /**
     * The {@code options.enable_parallel_af_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PARALLEL_AF_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_parallel_af_check");
    /**
     * The {@code options.enable_peer_dec_active_time_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PEER_DEC_ACTIVE_TIME_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_peer_dec_active_time_fix");
    /**
     * The {@code options.enable_peer_local_ip_prefix_fs_L1410938PRV} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PEER_LOCAL_IP_PREFIX_FS_L1410938PRV = new VoipParamKey(VoipParamType.INTEGER, "options.enable_peer_local_ip_prefix_fs_L1410938PRV");
    /**
     * The {@code options.enable_pending_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PENDING_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_pending_call");
    /**
     * The {@code options.enable_periodical_aud_rr_processing} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PERIODICAL_AUD_RR_PROCESSING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_periodical_aud_rr_processing");
    /**
     * The {@code options.enable_pid_resubscription_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PID_RESUBSCRIPTION_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_pid_resubscription_fix");
    /**
     * The {@code options.enable_pip_failure_video_resume} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PIP_FAILURE_VIDEO_RESUME = new VoipParamKey(VoipParamType.INTEGER, "options.enable_pip_failure_video_resume");
    /**
     * The {@code options.enable_post_net_health_status_to_cb_queue} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_POST_NET_HEALTH_STATUS_TO_CB_QUEUE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_post_net_health_status_to_cb_queue");
    /**
     * The {@code options.enable_post_net_health_status_to_cb_queue_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_POST_NET_HEALTH_STATUS_TO_CB_QUEUE_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.enable_post_net_health_status_to_cb_queue_v2");
    /**
     * The {@code options.enable_pp_flip_tracker} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PP_FLIP_TRACKER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_pp_flip_tracker");
    /**
     * The {@code options.enable_proactive_nack} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PROACTIVE_NACK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_proactive_nack");
    /**
     * The {@code options.enable_process_server_prefer_ipv6} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_PROCESS_SERVER_PREFER_IPV6 = new VoipParamKey(VoipParamType.INTEGER, "options.enable_process_server_prefer_ipv6");
    /**
     * The {@code options.enable_reason_code_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_REASON_CODE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_reason_code_bitmap");
    /**
     * The {@code options.enable_rebind_all_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_REBIND_ALL_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rebind_all_bitmap");
    /**
     * The {@code options.enable_receiver_side_automos} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RECEIVER_SIDE_AUTOMOS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_receiver_side_automos");
    /**
     * The {@code options.enable_reconnecting_all_grey_tile} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RECONNECTING_ALL_GREY_TILE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_reconnecting_all_grey_tile");
    /**
     * The {@code options.enable_red_dtx_as_redundant} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_DTX_AS_REDUNDANT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_dtx_as_redundant");
    /**
     * The {@code options.enable_red_dtx_carry_redundant} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_DTX_CARRY_REDUNDANT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_dtx_carry_redundant");
    /**
     * The {@code options.enable_red_multi_level_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_MULTI_LEVEL_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_multi_level_threshold");
    /**
     * The {@code options.enable_red_pt_support_rx} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_PT_SUPPORT_RX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_pt_support_rx");
    /**
     * The {@code options.enable_red_pt_support_tx} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_PT_SUPPORT_TX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_pt_support_tx");
    /**
     * The {@code options.enable_red_ts_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RED_TS_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_red_ts_fix");
    /**
     * The {@code options.enable_refactored_integrity_for_ping} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_REFACTORED_INTEGRITY_FOR_PING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_refactored_integrity_for_ping");
    /**
     * The {@code options.enable_refl_addr_signaling} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_REFL_ADDR_SIGNALING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_refl_addr_signaling");
    /**
     * The {@code options.enable_relay_election_with_xpop_rtt} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RELAY_ELECTION_WITH_XPOP_RTT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_relay_election_with_xpop_rtt");
    /**
     * The {@code options.enable_relay_idx_from_name} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RELAY_IDX_FROM_NAME = new VoipParamKey(VoipParamType.INTEGER, "options.enable_relay_idx_from_name");
    /**
     * The {@code options.enable_render_queue} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RENDER_QUEUE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_render_queue");
    /**
     * The {@code options.enable_reuse_bwe_last_video_segment} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_REUSE_BWE_LAST_VIDEO_SEGMENT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_reuse_bwe_last_video_segment");
    /**
     * The {@code options.enable_rp_psnr_calc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RP_PSNR_CALC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rp_psnr_calc");
    /**
     * The {@code options.enable_rtt_stat_for_field_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RTT_STAT_FOR_FIELD_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rtt_stat_for_field_stat");
    /**
     * The {@code options.enable_rtx_indication} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RTX_INDICATION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rtx_indication");
    /**
     * The {@code options.enable_rx_subscription} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RX_SUBSCRIPTION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rx_subscription");
    /**
     * The {@code options.enable_rx_subscription_vid_quality_field} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_RX_SUBSCRIPTION_VID_QUALITY_FIELD = new VoipParamKey(VoipParamType.INTEGER, "options.enable_rx_subscription_vid_quality_field");
    /**
     * The {@code options.enable_sampling_rate_overrides} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SAMPLING_RATE_OVERRIDES = new VoipParamKey(VoipParamType.INTEGER, "options.enable_sampling_rate_overrides");
    /**
     * The {@code options.enable_save_remote_cand_private_addr} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SAVE_REMOTE_CAND_PRIVATE_ADDR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_save_remote_cand_private_addr");
    /**
     * The {@code options.enable_schedule_timer_in_app_data_controller_create} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SCHEDULE_TIMER_IN_APP_DATA_CONTROLLER_CREATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_schedule_timer_in_app_data_controller_create");
    /**
     * The {@code options.enable_self_preview_size_for_ratio} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SELF_PREVIEW_SIZE_FOR_RATIO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_self_preview_size_for_ratio");
    /**
     * The {@code options.enable_send_call_connect_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SEND_CALL_CONNECT_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_send_call_connect_stat");
    /**
     * The {@code options.enable_send_rtp_p2p_crash_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SEND_RTP_P2P_CRASH_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_send_rtp_p2p_crash_fix");
    /**
     * The {@code options.enable_sender_side_automos} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SENDER_SIDE_AUTOMOS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_sender_side_automos");
    /**
     * The {@code options.enable_separate_keys_hbh_srtcp} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SEPARATE_KEYS_HBH_SRTCP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_separate_keys_hbh_srtcp");
    /**
     * The {@code options.enable_share_mixer_pipeline_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SHARE_MIXER_PIPELINE_RATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_share_mixer_pipeline_rate");
    /**
     * The {@code options.enable_signaling_network_probe} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SIGNALING_NETWORK_PROBE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_signaling_network_probe");
    /**
     * The {@code options.enable_signaling_probe_response} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SIGNALING_PROBE_RESPONSE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_signaling_probe_response");
    /**
     * The {@code options.enable_single_ip_relay_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SINGLE_IP_RELAY_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_single_ip_relay_fix");
    /**
     * The {@code options.enable_sl_optimizations} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SL_OPTIMIZATIONS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_sl_optimizations");
    /**
     * The {@code options.enable_software_mute_during_call_hold} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SOFTWARE_MUTE_DURING_CALL_HOLD = new VoipParamKey(VoipParamType.INTEGER, "options.enable_software_mute_during_call_hold");
    /**
     * The {@code options.enable_speaker_ranking} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SPEAKER_RANKING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_speaker_ranking");
    /**
     * The {@code options.enable_speaker_status_changed_events} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SPEAKER_STATUS_CHANGED_EVENTS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_speaker_status_changed_events");
    /**
     * The {@code options.enable_speaker_status_rx} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SPEAKER_STATUS_RX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_speaker_status_rx");
    /**
     * The {@code options.enable_stale_acceptsent_supersede} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STALE_ACCEPTSENT_SUPERSEDE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_stale_acceptsent_supersede");
    /**
     * The {@code options.enable_standalone_warp_pr_to_reset_cer_ts} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STANDALONE_WARP_PR_TO_RESET_CER_TS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_standalone_warp_pr_to_reset_cer_ts");
    /**
     * The {@code options.enable_standalone_warp_pr_to_reset_rx_media_ts} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STANDALONE_WARP_PR_TO_RESET_RX_MEDIA_TS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_standalone_warp_pr_to_reset_rx_media_ts");
    /**
     * The {@code options.enable_start_transport_media_on_p2p_connection} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_START_TRANSPORT_MEDIA_ON_P2P_CONNECTION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_start_transport_media_on_p2p_connection");
    /**
     * The {@code options.enable_stream_descriptor} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STREAM_DESCRIPTOR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_stream_descriptor");
    /**
     * The {@code options.enable_stream_mutex_fix_for_aud_ev} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STREAM_MUTEX_FIX_FOR_AUD_EV = new VoipParamKey(VoipParamType.INTEGER, "options.enable_stream_mutex_fix_for_aud_ev");
    /**
     * The {@code options.enable_stream_mutex_fix_for_vid_ev} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STREAM_MUTEX_FIX_FOR_VID_EV = new VoipParamKey(VoipParamType.INTEGER, "options.enable_stream_mutex_fix_for_vid_ev");
    /**
     * The {@code options.enable_strict_stun_tid_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STRICT_STUN_TID_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_strict_stun_tid_check");
    /**
     * The {@code options.enable_stun_mapped_addr_af_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_STUN_MAPPED_ADDR_AF_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_stun_mapped_addr_af_fix");
    /**
     * The {@code options.enable_sym_nat_bug_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SYM_NAT_BUG_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_sym_nat_bug_fix");
    /**
     * The {@code options.enable_sym_nat_detection} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_SYM_NAT_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_sym_nat_detection");
    /**
     * The {@code options.enable_teardown_reorder_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TEARDOWN_REORDER_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_teardown_reorder_fix");
    /**
     * The {@code options.enable_tee_mv1_mv2_data_channel_compatibility_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TEE_MV1_MV2_DATA_CHANNEL_COMPATIBILITY_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_tee_mv1_mv2_data_channel_compatibility_mode");
    /**
     * The {@code options.enable_thread_safe_sockaddr} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_THREAD_SAFE_SOCKADDR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_thread_safe_sockaddr");
    /**
     * The {@code options.enable_transport_bwe_monitor} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TRANSPORT_BWE_MONITOR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_transport_bwe_monitor");
    /**
     * The {@code options.enable_transport_feedback} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TRANSPORT_FEEDBACK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_transport_feedback");
    /**
     * The {@code options.enable_transport_fs_L1320265PRV} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TRANSPORT_FS_L1320265PRV = new VoipParamKey(VoipParamType.INTEGER, "options.enable_transport_fs_L1320265PRV");
    /**
     * The {@code options.enable_ts_logger_mutex} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TS_LOGGER_MUTEX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ts_logger_mutex");
    /**
     * The {@code options.enable_ts_logger_pkt_loss_pattern} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TS_LOGGER_PKT_LOSS_PATTERN = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ts_logger_pkt_loss_pattern");
    /**
     * The {@code options.enable_ts_logger_render_events} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TS_LOGGER_RENDER_EVENTS = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ts_logger_render_events");
    /**
     * The {@code options.enable_tx_packet_cache_result_cache} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TX_PACKET_CACHE_RESULT_CACHE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_tx_packet_cache_result_cache");
    /**
     * The {@code options.enable_tx_packet_cache_ssrc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TX_PACKET_CACHE_SSRC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_tx_packet_cache_ssrc");
    /**
     * The {@code options.enable_tx_traffic_enc_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TX_TRAFFIC_ENC_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_tx_traffic_enc_check");
    /**
     * The {@code options.enable_type_only_signaling_probe} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_TYPE_ONLY_SIGNALING_PROBE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_type_only_signaling_probe");
    /**
     * The {@code options.enable_uic_coex_crash_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UIC_COEX_CRASH_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_uic_coex_crash_fix");
    /**
     * The {@code options.enable_ul_audio_lqm} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UL_AUDIO_LQM = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ul_audio_lqm");
    /**
     * The {@code options.enable_ul_vid_pause_standalone} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UL_VID_PAUSE_STANDALONE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_ul_vid_pause_standalone");
    /**
     * The {@code options.enable_unified_connected_lonely_state_timer} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UNIFIED_CONNECTED_LONELY_STATE_TIMER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_unified_connected_lonely_state_timer");
    /**
     * The {@code options.enable_update_min_latency_relay_idx_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UPDATE_MIN_LATENCY_RELAY_IDX_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_update_min_latency_relay_idx_fix");
    /**
     * The {@code options.enable_uplink_prefetch_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_UPLINK_PREFETCH_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_uplink_prefetch_video");
    /**
     * The {@code options.enable_valid_pause_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VALID_PAUSE_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_valid_pause_check");
    /**
     * The {@code options.enable_vid_dev_set_preferred_driver} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_DEV_SET_PREFERRED_DRIVER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_dev_set_preferred_driver");
    /**
     * The {@code options.enable_vid_jb_dd_calc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_JB_DD_CALC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_jb_dd_calc");
    /**
     * The {@code options.enable_vid_jb_dd_err_recovery} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_JB_DD_ERR_RECOVERY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_jb_dd_err_recovery");
    /**
     * The {@code options.enable_vid_jb_dd_use_result} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_JB_DD_USE_RESULT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_jb_dd_use_result");
    /**
     * The {@code options.enable_vid_one_way_codec_dyn_rule} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_ONE_WAY_CODEC_DYN_RULE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_one_way_codec_dyn_rule");
    /**
     * The {@code options.enable_vid_port_restart_stats_accumulate} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_PORT_RESTART_STATS_ACCUMULATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_port_restart_stats_accumulate");
    /**
     * The {@code options.enable_vid_rec_thread_high_pri} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VID_REC_THREAD_HIGH_PRI = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vid_rec_thread_high_pri");
    /**
     * The {@code options.enable_video_nack_throttling} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VIDEO_NACK_THROTTLING = new VoipParamKey(VoipParamType.INTEGER, "options.enable_video_nack_throttling");
    /**
     * The {@code options.enable_video_resume_after_hold_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VIDEO_RESUME_AFTER_HOLD_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.enable_video_resume_after_hold_fix");
    /**
     * The {@code options.enable_video_rtp_hdr_ext_stream_subscription} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VIDEO_RTP_HDR_EXT_STREAM_SUBSCRIPTION = new VoipParamKey(VoipParamType.INTEGER, "options.enable_video_rtp_hdr_ext_stream_subscription");
    /**
     * The {@code options.enable_video_simulcast} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VIDEO_SIMULCAST = new VoipParamKey(VoipParamType.INTEGER, "options.enable_video_simulcast");
    /**
     * The {@code options.enable_voip_err_detector} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VOIP_ERR_DETECTOR = new VoipParamKey(VoipParamType.INTEGER, "options.enable_voip_err_detector");
    /**
     * The {@code options.enable_voip_err_detector_assert_debug_info} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VOIP_ERR_DETECTOR_ASSERT_DEBUG_INFO = new VoipParamKey(VoipParamType.INTEGER, "options.enable_voip_err_detector_assert_debug_info");
    /**
     * The {@code options.enable_voip_err_detector_for_all} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VOIP_ERR_DETECTOR_FOR_ALL = new VoipParamKey(VoipParamType.INTEGER, "options.enable_voip_err_detector_for_all");
    /**
     * The {@code options.enable_voip_err_detector_report_loc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VOIP_ERR_DETECTOR_REPORT_LOC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_voip_err_detector_report_loc");
    /**
     * The {@code options.enable_vpn_interface_used_field_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VPN_INTERFACE_USED_FIELD_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vpn_interface_used_field_stat");
    /**
     * The {@code options.enable_vqm} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_VQM = new VoipParamKey(VoipParamType.INTEGER, "options.enable_vqm");
    /**
     * The {@code options.enable_wa_asock_cfg_create_external_sock} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WA_ASOCK_CFG_CREATE_EXTERNAL_SOCK = new VoipParamKey(VoipParamType.INTEGER, "options.enable_wa_asock_cfg_create_external_sock");
    /**
     * The {@code options.enable_warp_hbh_fec_ssrc} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WARP_HBH_FEC_SSRC = new VoipParamKey(VoipParamType.INTEGER, "options.enable_warp_hbh_fec_ssrc");
    /**
     * The {@code options.enable_weak_wifi_in_tcp} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WEAK_WIFI_IN_TCP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_weak_wifi_in_tcp");
    /**
     * The {@code options.enable_web_compatible_p2p} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WEB_COMPATIBLE_P2P = new VoipParamKey(VoipParamType.INTEGER, "options.enable_web_compatible_p2p");
    /**
     * The {@code options.enable_webrtc_compatibility} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WEBRTC_COMPATIBILITY = new VoipParamKey(VoipParamType.INTEGER, "options.enable_webrtc_compatibility");
    /**
     * The {@code options.enable_webrtc_nack_requester} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WEBRTC_NACK_REQUESTER = new VoipParamKey(VoipParamType.INTEGER, "options.enable_webrtc_nack_requester");
    /**
     * The {@code options.enable_webrtc_video_jb} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_WEBRTC_VIDEO_JB = new VoipParamKey(VoipParamType.INTEGER, "options.enable_webrtc_video_jb");
    /**
     * The {@code options.enable_xpop_for_group} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_XPOP_FOR_GROUP = new VoipParamKey(VoipParamType.INTEGER, "options.enable_xpop_for_group");
    /**
     * The {@code options.enable_xr2d_codec_avatar_video_state} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENABLE_XR2D_CODEC_AVATAR_VIDEO_STATE = new VoipParamKey(VoipParamType.INTEGER, "options.enable_xr2d_codec_avatar_video_state");
    /**
     * The {@code options.enc_fps_over_capture_fps_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENC_FPS_OVER_CAPTURE_FPS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.enc_fps_over_capture_fps_threshold");
    /**
     * The {@code options.enc_res_align_base} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENC_RES_ALIGN_BASE = new VoipParamKey(VoipParamType.INTEGER, "options.enc_res_align_base");
    /**
     * The {@code options.endpt_polling_timeout_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENDPT_POLLING_TIMEOUT_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.endpt_polling_timeout_msec");
    /**
     * The {@code options.enforce_audio_reserve_bitrate} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENFORCE_AUDIO_RESERVE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "options.enforce_audio_reserve_bitrate");
    /**
     * The {@code options.enforce_pools_for_media_platform_events_with_data} voip-param.
     */
    public static final VoipParamKey OPTIONS_ENFORCE_POOLS_FOR_MEDIA_PLATFORM_EVENTS_WITH_DATA = new VoipParamKey(VoipParamType.INTEGER, "options.enforce_pools_for_media_platform_events_with_data");
    /**
     * The {@code options.exclude_peer_setup_error_from_connected} voip-param.
     */
    public static final VoipParamKey OPTIONS_EXCLUDE_PEER_SETUP_ERROR_FROM_CONNECTED = new VoipParamKey(VoipParamType.INTEGER, "options.exclude_peer_setup_error_from_connected");
    /**
     * The {@code options.fail_v3_screen_share_and_show_app_update_dialog} voip-param.
     */
    public static final VoipParamKey OPTIONS_FAIL_V3_SCREEN_SHARE_AND_SHOW_APP_UPDATE_DIALOG = new VoipParamKey(VoipParamType.INTEGER, "options.fail_v3_screen_share_and_show_app_update_dialog");
    /**
     * The {@code options.fast_callee_setup_max_bind_retries} voip-param.
     */
    public static final VoipParamKey OPTIONS_FAST_CALLEE_SETUP_MAX_BIND_RETRIES = new VoipParamKey(VoipParamType.INTEGER, "options.fast_callee_setup_max_bind_retries");
    /**
     * The {@code options.fast_callee_setup_max_e2e_rtt_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_FAST_CALLEE_SETUP_MAX_E2E_RTT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.fast_callee_setup_max_e2e_rtt_ms");
    /**
     * The {@code options.fast_callee_setup_skip_after_elected} voip-param.
     */
    public static final VoipParamKey OPTIONS_FAST_CALLEE_SETUP_SKIP_AFTER_ELECTED = new VoipParamKey(VoipParamType.INTEGER, "options.fast_callee_setup_skip_after_elected");
    /**
     * The {@code options.fatal_send_error_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_FATAL_SEND_ERROR_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.fatal_send_error_bitmap");
    /**
     * The {@code options.fec_bw_downgrade_min_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_FEC_BW_DOWNGRADE_MIN_PLR = new VoipParamKey(VoipParamType.INTEGER, "options.fec_bw_downgrade_min_plr");
    /**
     * The {@code options.fix_audio_low_data_mode_receiver_device} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_AUDIO_LOW_DATA_MODE_RECEIVER_DEVICE = new VoipParamKey(VoipParamType.INTEGER, "options.fix_audio_low_data_mode_receiver_device");
    /**
     * The {@code options.fix_capture_fmt_id_after_format_copy} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_CAPTURE_FMT_ID_AFTER_FORMAT_COPY = new VoipParamKey(VoipParamType.INTEGER, "options.fix_capture_fmt_id_after_format_copy");
    /**
     * The {@code options.fix_first_frame_converter_resize} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_FIRST_FRAME_CONVERTER_RESIZE = new VoipParamKey(VoipParamType.INTEGER, "options.fix_first_frame_converter_resize");
    /**
     * The {@code options.fix_reconnecting_state_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_RECONNECTING_STATE_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.fix_reconnecting_state_count");
    /**
     * The {@code options.fix_render_pause_duration_across_segments} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_RENDER_PAUSE_DURATION_ACROSS_SEGMENTS = new VoipParamKey(VoipParamType.INTEGER, "options.fix_render_pause_duration_across_segments");
    /**
     * The {@code options.fix_rx_remb_rst} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_RX_REMB_RST = new VoipParamKey(VoipParamType.INTEGER, "options.fix_rx_remb_rst");
    /**
     * The {@code options.fix_speaker_ranking_corrupted_stream_crash} voip-param.
     */
    public static final VoipParamKey OPTIONS_FIX_SPEAKER_RANKING_CORRUPTED_STREAM_CRASH = new VoipParamKey(VoipParamType.INTEGER, "options.fix_speaker_ranking_corrupted_stream_crash");
    /**
     * The {@code options.force_3_2_aspect_ratio} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_3_2_ASPECT_RATIO = new VoipParamKey(VoipParamType.INTEGER, "options.force_3_2_aspect_ratio");
    /**
     * The {@code options.force_call_failure} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_CALL_FAILURE = new VoipParamKey(VoipParamType.INTEGER, "options.force_call_failure");
    /**
     * The {@code options.force_passive_capture_dev_stream_role} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_PASSIVE_CAPTURE_DEV_STREAM_ROLE = new VoipParamKey(VoipParamType.INTEGER, "options.force_passive_capture_dev_stream_role");
    /**
     * The {@code options.force_rebind_on_relay_election} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_REBIND_ON_RELAY_ELECTION = new VoipParamKey(VoipParamType.INTEGER, "options.force_rebind_on_relay_election");
    /**
     * The {@code options.force_refresh_capture_port_camera} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_REFRESH_CAPTURE_PORT_CAMERA = new VoipParamKey(VoipParamType.INTEGER, "options.force_refresh_capture_port_camera");
    /**
     * The {@code options.force_rtp_ext_prof} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_RTP_EXT_PROF = new VoipParamKey(VoipParamType.INTEGER, "options.force_rtp_ext_prof");
    /**
     * The {@code options.force_snd_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_SND_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.force_snd_size");
    /**
     * The {@code options.force_swb} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_SWB = new VoipParamKey(VoipParamType.INTEGER, "options.force_swb");
    /**
     * The {@code options.force_width} voip-param.
     */
    public static final VoipParamKey OPTIONS_FORCE_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "options.force_width");
    /**
     * The {@code options.frm_buf_convert_outside_lock} voip-param.
     */
    public static final VoipParamKey OPTIONS_FRM_BUF_CONVERT_OUTSIDE_LOCK = new VoipParamKey(VoipParamType.INTEGER, "options.frm_buf_convert_outside_lock");
    /**
     * The {@code options.get_detailed_afl_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_GET_DETAILED_AFL_STATS = new VoipParamKey(VoipParamType.INTEGER, "options.get_detailed_afl_stats");
    /**
     * The {@code options.get_detailed_v2v_afl_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_GET_DETAILED_V2V_AFL_STATS = new VoipParamKey(VoipParamType.INTEGER, "options.get_detailed_v2v_afl_stats");
    /**
     * The {@code options.get_pip_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_GET_PIP_STATS = new VoipParamKey(VoipParamType.INTEGER, "options.get_pip_stats");
    /**
     * The {@code options.get_refl_ip_from_alloc_err} voip-param.
     */
    public static final VoipParamKey OPTIONS_GET_REFL_IP_FROM_ALLOC_ERR = new VoipParamKey(VoipParamType.INTEGER, "options.get_refl_ip_from_alloc_err");
    /**
     * The {@code options.gethostip_disable_local_resolution} voip-param.
     */
    public static final VoipParamKey OPTIONS_GETHOSTIP_DISABLE_LOCAL_RESOLUTION = new VoipParamKey(VoipParamType.INTEGER, "options.gethostip_disable_local_resolution");
    /**
     * The {@code options.goodput_downlink_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_GOODPUT_DOWNLINK_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.goodput_downlink_enabled");
    /**
     * The {@code options.harmonic_fps_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_HARMONIC_FPS_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.harmonic_fps_interval");
    /**
     * The {@code options.hbh_nack_control_p2p} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_NACK_CONTROL_P2P = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_nack_control_p2p");
    /**
     * The {@code options.hbh_srtp_peroidic_sync_roc_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_SRTP_PEROIDIC_SYNC_ROC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_srtp_peroidic_sync_roc_timeout_ms");
    /**
     * The {@code options.hbh_srtp_states_sync_freq_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_SRTP_STATES_SYNC_FREQ_MS = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_srtp_states_sync_freq_ms");
    /**
     * The {@code options.hbh_srtp_states_sync_num_pkt_resend} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_SRTP_STATES_SYNC_NUM_PKT_RESEND = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_srtp_states_sync_num_pkt_resend");
    /**
     * The {@code options.hbh_srtp_states_sync_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_SRTP_STATES_SYNC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_srtp_states_sync_timeout_ms");
    /**
     * The {@code options.hbh_warp_roc_sync_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_HBH_WARP_ROC_SYNC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.hbh_warp_roc_sync_timeout_ms");
    /**
     * The {@code options.high_plr_pct_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_HIGH_PLR_PCT_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.high_plr_pct_thresh");
    /**
     * The {@code options.high_plr_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_HIGH_PLR_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.high_plr_thresh");
    /**
     * The {@code options.high_sample_rate_year_class_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_HIGH_SAMPLE_RATE_YEAR_CLASS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.high_sample_rate_year_class_threshold");
    /**
     * The {@code options.history_based_audio_device_change_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_HISTORY_BASED_AUDIO_DEVICE_CHANGE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.history_based_audio_device_change_threshold");
    /**
     * The {@code options.hybrid_lazy_encoding} voip-param.
     */
    public static final VoipParamKey OPTIONS_HYBRID_LAZY_ENCODING = new VoipParamKey(VoipParamType.INTEGER, "options.hybrid_lazy_encoding");
    /**
     * The {@code options.ignore_first_n_silence_frame} voip-param.
     */
    public static final VoipParamKey OPTIONS_IGNORE_FIRST_N_SILENCE_FRAME = new VoipParamKey(VoipParamType.INTEGER, "options.ignore_first_n_silence_frame");
    /**
     * The {@code options.ignore_hostile_network} voip-param.
     */
    public static final VoipParamKey OPTIONS_IGNORE_HOSTILE_NETWORK = new VoipParamKey(VoipParamType.INTEGER, "options.ignore_hostile_network");
    /**
     * The {@code options.imu_data_circular_buffer_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_IMU_DATA_CIRCULAR_BUFFER_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.imu_data_circular_buffer_size");
    /**
     * The {@code options.imu_data_fpp} voip-param.
     */
    public static final VoipParamKey OPTIONS_IMU_DATA_FPP = new VoipParamKey(VoipParamType.INTEGER, "options.imu_data_fpp");
    /**
     * The {@code options.imu_data_stream_enc_clock_rate_hz} voip-param.
     */
    public static final VoipParamKey OPTIONS_IMU_DATA_STREAM_ENC_CLOCK_RATE_HZ = new VoipParamKey(VoipParamType.INTEGER, "options.imu_data_stream_enc_clock_rate_hz");
    /**
     * The {@code options.init_quality_window_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_INIT_QUALITY_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "options.init_quality_window_ms");
    /**
     * The {@code options.init_rtp_ts_on_first_audio_frame} voip-param.
     */
    public static final VoipParamKey OPTIONS_INIT_RTP_TS_ON_FIRST_AUDIO_FRAME = new VoipParamKey(VoipParamType.INTEGER, "options.init_rtp_ts_on_first_audio_frame");
    /**
     * The {@code options.initial_connecting_sound_delay} voip-param.
     */
    public static final VoipParamKey OPTIONS_INITIAL_CONNECTING_SOUND_DELAY = new VoipParamKey(VoipParamType.INTEGER, "options.initial_connecting_sound_delay");
    /**
     * The {@code options.initial_fpp} voip-param.
     */
    public static final VoipParamKey OPTIONS_INITIAL_FPP = new VoipParamKey(VoipParamType.INTEGER, "options.initial_fpp");
    /**
     * The {@code options.io_buffer_duration_in_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_IO_BUFFER_DURATION_IN_MS = new VoipParamKey(VoipParamType.INTEGER, "options.io_buffer_duration_in_ms");
    /**
     * The {@code options.ip_changed_medium_update_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CHANGED_MEDIUM_UPDATE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ip_changed_medium_update_interval_ms");
    /**
     * The {@code options.ip_changed_socket_event_reset_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CHANGED_SOCKET_EVENT_RESET_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ip_changed_socket_event_reset_interval_ms");
    /**
     * The {@code options.ip_config} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CONFIG = new VoipParamKey(VoipParamType.INTEGER, "options.ip_config");
    /**
     * The {@code options.ip_corr_allow_disable_prefer_relay} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CORR_ALLOW_DISABLE_PREFER_RELAY = new VoipParamKey(VoipParamType.INTEGER, "options.ip_corr_allow_disable_prefer_relay");
    /**
     * The {@code options.ip_corr_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CORR_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.ip_corr_enable");
    /**
     * The {@code options.ip_corr_max_num_relays_per_update} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CORR_MAX_NUM_RELAYS_PER_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "options.ip_corr_max_num_relays_per_update");
    /**
     * The {@code options.ip_corr_max_relay_info_update} voip-param.
     */
    public static final VoipParamKey OPTIONS_IP_CORR_MAX_RELAY_INFO_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "options.ip_corr_max_relay_info_update");
    /**
     * The {@code options.jb_ignore_start_of_call_empties} voip-param.
     */
    public static final VoipParamKey OPTIONS_JB_IGNORE_START_OF_CALL_EMPTIES = new VoipParamKey(VoipParamType.INTEGER, "options.jb_ignore_start_of_call_empties");
    /**
     * The {@code options.jb_impl} voip-param.
     */
    public static final VoipParamKey OPTIONS_JB_IMPL = new VoipParamKey(VoipParamType.INTEGER, "options.jb_impl");
    /**
     * The {@code options.joining_sound_gap_in_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_JOINING_SOUND_GAP_IN_MS = new VoipParamKey(VoipParamType.INTEGER, "options.joining_sound_gap_in_ms");
    /**
     * The {@code options.keep_conf_bridge_to_48} voip-param.
     */
    public static final VoipParamKey OPTIONS_KEEP_CONF_BRIDGE_TO_48 = new VoipParamKey(VoipParamType.INTEGER, "options.keep_conf_bridge_to_48");
    /**
     * The {@code options.keep_conf_bridge_to_wb} voip-param.
     */
    public static final VoipParamKey OPTIONS_KEEP_CONF_BRIDGE_TO_WB = new VoipParamKey(VoipParamType.INTEGER, "options.keep_conf_bridge_to_wb");
    /**
     * The {@code options.keep_driver_at_native} voip-param.
     */
    public static final VoipParamKey OPTIONS_KEEP_DRIVER_AT_NATIVE = new VoipParamKey(VoipParamType.INTEGER, "options.keep_driver_at_native");
    /**
     * The {@code options.lanczos_filter_setting} voip-param.
     */
    public static final VoipParamKey OPTIONS_LANCZOS_FILTER_SETTING = new VoipParamKey(VoipParamType.INTEGER, "options.lanczos_filter_setting");
    /**
     * The {@code options.lock_video_orientation} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOCK_VIDEO_ORIENTATION = new VoipParamKey(VoipParamType.INTEGER, "options.lock_video_orientation");
    /**
     * The {@code options.loss_info_ext_int_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOSS_INFO_EXT_INT_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.loss_info_ext_int_msec");
    /**
     * The {@code options.loss_period_consecutive_trigger_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOSS_PERIOD_CONSECUTIVE_TRIGGER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.loss_period_consecutive_trigger_threshold");
    /**
     * The {@code options.low_battery_notify_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOW_BATTERY_NOTIFY_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.low_battery_notify_threshold");
    /**
     * The {@code options.low_connection_bind_success_rate_grace_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOW_CONNECTION_BIND_SUCCESS_RATE_GRACE_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.low_connection_bind_success_rate_grace_period_ms");
    /**
     * The {@code options.low_fd_setsize_factor} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOW_FD_SETSIZE_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "options.low_fd_setsize_factor");
    /**
     * The {@code options.low_plr_pct_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOW_PLR_PCT_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.low_plr_pct_thresh");
    /**
     * The {@code options.low_plr_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_LOW_PLR_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.low_plr_thresh");
    /**
     * The {@code options.majority_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAJORITY_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.majority_thresh");
    /**
     * The {@code options.max_aud_restarts_record_silence} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_AUD_RESTARTS_RECORD_SILENCE = new VoipParamKey(VoipParamType.INTEGER, "options.max_aud_restarts_record_silence");
    /**
     * The {@code options.max_audio_driver_restarts} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_AUDIO_DRIVER_RESTARTS = new VoipParamKey(VoipParamType.INTEGER, "options.max_audio_driver_restarts");
    /**
     * The {@code options.max_audio_ts_jitter_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_AUDIO_TS_JITTER_MS = new VoipParamKey(VoipParamType.INTEGER, "options.max_audio_ts_jitter_ms");
    /**
     * The {@code options.max_av_resync_duration_in_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_AV_RESYNC_DURATION_IN_MS = new VoipParamKey(VoipParamType.INTEGER, "options.max_av_resync_duration_in_ms");
    /**
     * The {@code options.max_capture_fps} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_CAPTURE_FPS = new VoipParamKey(VoipParamType.INTEGER, "options.max_capture_fps");
    /**
     * The {@code options.max_matching_bound_socket} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_MATCHING_BOUND_SOCKET = new VoipParamKey(VoipParamType.INTEGER, "options.max_matching_bound_socket");
    /**
     * The {@code options.max_relay_bind_retry_count_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_RELAY_BIND_RETRY_COUNT_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.max_relay_bind_retry_count_strategy");
    /**
     * The {@code options.max_rtp_audio_packet_resends} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_RTP_AUDIO_PACKET_RESENDS = new VoipParamKey(VoipParamType.INTEGER, "options.max_rtp_audio_packet_resends");
    /**
     * The {@code options.max_rtp_video_packet_resends} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_RTP_VIDEO_PACKET_RESENDS = new VoipParamKey(VoipParamType.INTEGER, "options.max_rtp_video_packet_resends");
    /**
     * The {@code options.max_rtx_bitrate_pct} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_RTX_BITRATE_PCT = new VoipParamKey(VoipParamType.INTEGER, "options.max_rtx_bitrate_pct");
    /**
     * The {@code options.max_rtx_window_size_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_RTX_WINDOW_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.max_rtx_window_size_ms");
    /**
     * The {@code options.max_simultaneous_sends} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_SIMULTANEOUS_SENDS = new VoipParamKey(VoipParamType.INTEGER, "options.max_simultaneous_sends");
    /**
     * The {@code options.max_streams_to_mix_grp_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_MAX_STREAMS_TO_MIX_GRP_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.max_streams_to_mix_grp_call");
    /**
     * The {@code options.media_rx_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MEDIA_RX_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.media_rx_timeout_ms");
    /**
     * The {@code options.merge_vid_strm_port_op} voip-param.
     */
    public static final VoipParamKey OPTIONS_MERGE_VID_STRM_PORT_OP = new VoipParamKey(VoipParamType.INTEGER, "options.merge_vid_strm_port_op");
    /**
     * The {@code options.min_audio_level_for_speech_tx} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_AUDIO_LEVEL_FOR_SPEECH_TX = new VoipParamKey(VoipParamType.INTEGER, "options.min_audio_level_for_speech_tx");
    /**
     * The {@code options.min_audio_restarts_for_sampling_rate_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_AUDIO_RESTARTS_FOR_SAMPLING_RATE_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.min_audio_restarts_for_sampling_rate_check");
    /**
     * The {@code options.min_batt_drop_to_update_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_BATT_DROP_TO_UPDATE_STATS = new VoipParamKey(VoipParamType.FLOAT, "options.min_batt_drop_to_update_stats");
    /**
     * The {@code options.min_capture_fps} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_CAPTURE_FPS = new VoipParamKey(VoipParamType.INTEGER, "options.min_capture_fps");
    /**
     * The {@code options.min_ecn_feedback_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_ECN_FEEDBACK_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.min_ecn_feedback_interval_ms");
    /**
     * The {@code options.min_num_participants_to_enable_rx_sub} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_NUM_PARTICIPANTS_TO_ENABLE_RX_SUB = new VoipParamKey(VoipParamType.INTEGER, "options.min_num_participants_to_enable_rx_sub");
    /**
     * The {@code options.min_rx_buf_size_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_RX_BUF_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.min_rx_buf_size_ms");
    /**
     * The {@code options.min_time_first_audio_restart_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIN_TIME_FIRST_AUDIO_RESTART_MS = new VoipParamKey(VoipParamType.INTEGER, "options.min_time_first_audio_restart_ms");
    /**
     * The {@code options.mix_stream_with_speech_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_MIX_STREAM_WITH_SPEECH_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.mix_stream_with_speech_status");
    /**
     * The {@code options.mlow_red_proactive_rtt_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MLOW_RED_PROACTIVE_RTT_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.mlow_red_proactive_rtt_threshold_ms");
    /**
     * The {@code options.mlow_red_proactive_update_limit} voip-param.
     */
    public static final VoipParamKey OPTIONS_MLOW_RED_PROACTIVE_UPDATE_LIMIT = new VoipParamKey(VoipParamType.INTEGER, "options.mlow_red_proactive_update_limit");
    /**
     * The {@code options.multipop_check_duration_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_MULTIPOP_CHECK_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "options.multipop_check_duration_ms");
    /**
     * The {@code options.mute_sender_on_hold} voip-param.
     */
    public static final VoipParamKey OPTIONS_MUTE_SENDER_ON_HOLD = new VoipParamKey(VoipParamType.INTEGER, "options.mute_sender_on_hold");
    /**
     * The {@code options.n_packets_for_capi_av_sync_ts_reconciliation} voip-param.
     */
    public static final VoipParamKey OPTIONS_N_PACKETS_FOR_CAPI_AV_SYNC_TS_RECONCILIATION = new VoipParamKey(VoipParamType.INTEGER, "options.n_packets_for_capi_av_sync_ts_reconciliation");
    /**
     * The {@code options.net_health_check_destroying_before_rtcp_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_CHECK_DESTROYING_BEFORE_RTCP_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_check_destroying_before_rtcp_stat");
    /**
     * The {@code options.net_health_enable_banner_sub_message} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_ENABLE_BANNER_SUB_MESSAGE = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_enable_banner_sub_message");
    /**
     * The {@code options.net_health_enable_p2p_connection_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_ENABLE_P2P_CONNECTION_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_enable_p2p_connection_check");
    /**
     * The {@code options.net_health_enable_send_peer_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_ENABLE_SEND_PEER_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_enable_send_peer_status");
    /**
     * The {@code options.net_health_enable_set_peer_no_network_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_ENABLE_SET_PEER_NO_NETWORK_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_enable_set_peer_no_network_status");
    /**
     * The {@code options.net_health_max_p2p_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_P2P_PLR = new VoipParamKey(VoipParamType.FLOAT, "options.net_health_max_p2p_plr");
    /**
     * The {@code options.net_health_max_p2p_rtt} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_P2P_RTT = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_p2p_rtt");
    /**
     * The {@code options.net_health_max_peer_count_to_send_peer_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_PEER_COUNT_TO_SEND_PEER_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_peer_count_to_send_peer_status");
    /**
     * The {@code options.net_health_max_sent_banner} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_SENT_BANNER = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_sent_banner");
    /**
     * The {@code options.net_health_max_sent_poor_peer_status_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_SENT_POOR_PEER_STATUS_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_sent_poor_peer_status_count");
    /**
     * The {@code options.net_health_max_sound_alert_audio} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_SOUND_ALERT_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_sound_alert_audio");
    /**
     * The {@code options.net_health_max_sound_alert_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_MAX_SOUND_ALERT_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_max_sound_alert_video");
    /**
     * The {@code options.net_health_no_network_rx_traffic_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_NO_NETWORK_RX_TRAFFIC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_no_network_rx_traffic_timeout_ms");
    /**
     * The {@code options.net_health_peer_status_normal_net_rx_traffic_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_PEER_STATUS_NORMAL_NET_RX_TRAFFIC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_peer_status_normal_net_rx_traffic_timeout_ms");
    /**
     * The {@code options.net_health_peer_status_poor_net_rx_traffic_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_PEER_STATUS_POOR_NET_RX_TRAFFIC_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_peer_status_poor_net_rx_traffic_timeout_ms");
    /**
     * The {@code options.net_health_ping_loss_sample_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_PING_LOSS_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_ping_loss_sample_size");
    /**
     * The {@code options.net_health_poor_peer_status_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_POOR_PEER_STATUS_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_poor_peer_status_interval_ms");
    /**
     * The {@code options.net_health_sent_banner_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_SENT_BANNER_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_sent_banner_interval_ms");
    /**
     * The {@code options.net_health_sound_alert_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_HEALTH_SOUND_ALERT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.net_health_sound_alert_interval_ms");
    /**
     * The {@code options.net_medium_bc_async} voip-param.
     */
    public static final VoipParamKey OPTIONS_NET_MEDIUM_BC_ASYNC = new VoipParamKey(VoipParamType.INTEGER, "options.net_medium_bc_async");
    /**
     * The {@code options.neteq_allow_red_jitter} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ALLOW_RED_JITTER = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_allow_red_jitter");
    /**
     * The {@code options.neteq_allow_time_stretch_acceleration} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ALLOW_TIME_STRETCH_ACCELERATION = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_allow_time_stretch_acceleration");
    /**
     * The {@code options.neteq_allow_time_stretch_for_high_latency} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ALLOW_TIME_STRETCH_FOR_HIGH_LATENCY = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_allow_time_stretch_for_high_latency");
    /**
     * The {@code options.neteq_allow_time_stretch_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ALLOW_TIME_STRETCH_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_allow_time_stretch_threshold_ms");
    /**
     * The {@code options.neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_delay_offset_ms");
    /**
     * The {@code options.neteq_dl_history_size_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_DL_HISTORY_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_dl_history_size_ms");
    /**
     * The {@code options.neteq_dm_history_size_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_DM_HISTORY_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_dm_history_size_ms");
    /**
     * The {@code options.neteq_effective_peak_period_fraction_perc} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_EFFECTIVE_PEAK_PERIOD_FRACTION_PERC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_effective_peak_period_fraction_perc");
    /**
     * The {@code options.neteq_enable_codec_plc} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_CODEC_PLC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_codec_plc");
    /**
     * The {@code options.neteq_enable_custom_required_samples_for_acc} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_CUSTOM_REQUIRED_SAMPLES_FOR_ACC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_custom_required_samples_for_acc");
    /**
     * The {@code options.neteq_enable_depack_multiframe} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_DEPACK_MULTIFRAME = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_depack_multiframe");
    /**
     * The {@code options.neteq_enable_ff} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_FF = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_ff");
    /**
     * The {@code options.neteq_enable_peak_detector} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_PEAK_DETECTOR = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_peak_detector");
    /**
     * The {@code options.neteq_enable_silence_deletion} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_SILENCE_DELETION = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_silence_deletion");
    /**
     * The {@code options.neteq_enable_speaker_status} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_ENABLE_SPEAKER_STATUS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_enable_speaker_status");
    /**
     * The {@code options.neteq_ft_lookup_override} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_FT_LOOKUP_OVERRIDE = new VoipParamKey(VoipParamType.STRING, "options.neteq_ft_lookup_override");
    /**
     * The {@code options.neteq_ft_string_override} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_FT_STRING_OVERRIDE = new VoipParamKey(VoipParamType.STRING, "options.neteq_ft_string_override");
    /**
     * The {@code options.neteq_init_min_e2e_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_INIT_MIN_E2E_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_init_min_e2e_delay_ms");
    /**
     * The {@code options.neteq_lad_enabled_for_fec} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_LAD_ENABLED_FOR_FEC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_lad_enabled_for_fec");
    /**
     * The {@code options.neteq_lad_enabled_for_nack} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_LAD_ENABLED_FOR_NACK = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_lad_enabled_for_nack");
    /**
     * The {@code options.neteq_lad_max_lost_packet_list_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_LAD_MAX_LOST_PACKET_LIST_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_lad_max_lost_packet_list_size");
    /**
     * The {@code options.neteq_lad_nack_extra_insert_time_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_LAD_NACK_EXTRA_INSERT_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_lad_nack_extra_insert_time_ms");
    /**
     * The {@code options.neteq_lad_nack_extra_receive_time_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_LAD_NACK_EXTRA_RECEIVE_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_lad_nack_extra_receive_time_ms");
    /**
     * The {@code options.neteq_max_delay} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_MAX_DELAY = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_max_delay");
    /**
     * The {@code options.neteq_max_packets_in_buf} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_MAX_PACKETS_IN_BUF = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_max_packets_in_buf");
    /**
     * The {@code options.neteq_max_peak_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_MAX_PEAK_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_max_peak_period_ms");
    /**
     * The {@code options.neteq_min_delay} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_MIN_DELAY = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_min_delay");
    /**
     * The {@code options.neteq_min_peaks_to_trigger} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_MIN_PEAKS_TO_TRIGGER = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_min_peaks_to_trigger");
    /**
     * The {@code options.neteq_nack_rtt_limit_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_NACK_RTT_LIMIT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_nack_rtt_limit_ms");
    /**
     * The {@code options.neteq_num_initial_packets} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_NUM_INITIAL_PACKETS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_num_initial_packets");
    /**
     * The {@code options.neteq_peak_detection_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_PEAK_DETECTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_peak_detection_threshold");
    /**
     * The {@code options.neteq_preexpand_with_filtered_level_perc} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_PREEXPAND_WITH_FILTERED_LEVEL_PERC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_preexpand_with_filtered_level_perc");
    /**
     * The {@code options.neteq_proactive_nack_iat_percentile} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_PROACTIVE_NACK_IAT_PERCENTILE = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_proactive_nack_iat_percentile");
    /**
     * The {@code options.neteq_proactive_nack_margin_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_PROACTIVE_NACK_MARGIN_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_proactive_nack_margin_ms");
    /**
     * The {@code options.neteq_proactive_nack_max_num_missing_packets_predicted} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_PROACTIVE_NACK_MAX_NUM_MISSING_PACKETS_PREDICTED = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_proactive_nack_max_num_missing_packets_predicted");
    /**
     * The {@code options.neteq_red_optimizer_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_RED_OPTIMIZER_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_red_optimizer_enabled");
    /**
     * The {@code options.neteq_skip_nack_with_fec} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_SKIP_NACK_WITH_FEC = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_skip_nack_with_fec");
    /**
     * The {@code options.neteq_smart_buffer_flush_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_SMART_BUFFER_FLUSH_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_smart_buffer_flush_enabled");
    /**
     * The {@code options.neteq_smart_buffer_flush_multiplier} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_SMART_BUFFER_FLUSH_MULTIPLIER = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_smart_buffer_flush_multiplier");
    /**
     * The {@code options.neteq_smart_buffer_flush_target_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_SMART_BUFFER_FLUSH_TARGET_MS = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_smart_buffer_flush_target_ms");
    /**
     * The {@code options.neteq_underrun_forget_factor} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_UNDERRUN_FORGET_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "options.neteq_underrun_forget_factor");
    /**
     * The {@code options.neteq_underrun_quantile} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_UNDERRUN_QUANTILE = new VoipParamKey(VoipParamType.FLOAT, "options.neteq_underrun_quantile");
    /**
     * The {@code options.neteq_use_20ms_get_period} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_USE_20MS_GET_PERIOD = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_use_20ms_get_period");
    /**
     * The {@code options.neteq_use_mute_in_audio_dropping} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_USE_MUTE_IN_AUDIO_DROPPING = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_use_mute_in_audio_dropping");
    /**
     * The {@code options.neteq_use_muted_state_after_remote_hangup} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_USE_MUTED_STATE_AFTER_REMOTE_HANGUP = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_use_muted_state_after_remote_hangup");
    /**
     * The {@code options.neteq_use_span_samples_for_cng} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETEQ_USE_SPAN_SAMPLES_FOR_CNG = new VoipParamKey(VoipParamType.INTEGER, "options.neteq_use_span_samples_for_cng");
    /**
     * The {@code options.network_optimizer_bwe_check_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_BWE_CHECK_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_bwe_check_period_ms");
    /**
     * The {@code options.network_optimizer_bwe_ema_min_sample_cnt} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_BWE_EMA_MIN_SAMPLE_CNT = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_bwe_ema_min_sample_cnt");
    /**
     * The {@code options.network_optimizer_check_remote_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_CHECK_REMOTE_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_check_remote_bwe");
    /**
     * The {@code options.network_optimizer_clear_history_on_running} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_CLEAR_HISTORY_ON_RUNNING = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_clear_history_on_running");
    /**
     * The {@code options.network_optimizer_disable_prefer_relay_when_sampled} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_DISABLE_PREFER_RELAY_WHEN_SAMPLED = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_disable_prefer_relay_when_sampled");
    /**
     * The {@code options.network_optimizer_disable_sts_switch_when_sampled} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_DISABLE_STS_SWITCH_WHEN_SAMPLED = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_disable_sts_switch_when_sampled");
    /**
     * The {@code options.network_optimizer_enable_light_weight_p2p_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_ENABLE_LIGHT_WEIGHT_P2P_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_enable_light_weight_p2p_switch");
    /**
     * The {@code options.network_optimizer_enable_logging_avg_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_ENABLE_LOGGING_AVG_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_enable_logging_avg_stat");
    /**
     * The {@code options.network_optimizer_enable_prefer_relay_for_snadl} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_ENABLE_PREFER_RELAY_FOR_SNADL = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_enable_prefer_relay_for_snadl");
    /**
     * The {@code options.network_optimizer_enable_transport_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_ENABLE_TRANSPORT_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_enable_transport_switch");
    /**
     * The {@code options.network_optimizer_max_transport_switch_cnt} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MAX_TRANSPORT_SWITCH_CNT = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_max_transport_switch_cnt");
    /**
     * The {@code options.network_optimizer_min_bwe_audio} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MIN_BWE_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_min_bwe_audio");
    /**
     * The {@code options.network_optimizer_min_bwe_video} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MIN_BWE_VIDEO = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_min_bwe_video");
    /**
     * The {@code options.network_optimizer_min_mos_diff} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MIN_MOS_DIFF = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_min_mos_diff");
    /**
     * The {@code options.network_optimizer_min_plr_diff} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MIN_PLR_DIFF = new VoipParamKey(VoipParamType.FLOAT, "options.network_optimizer_min_plr_diff");
    /**
     * The {@code options.network_optimizer_min_rtt_diff_for_nadl} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_MIN_RTT_DIFF_FOR_NADL = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_min_rtt_diff_for_nadl");
    /**
     * The {@code options.network_optimizer_random_probe_sample_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_RANDOM_PROBE_SAMPLE_RATE = new VoipParamKey(VoipParamType.FLOAT, "options.network_optimizer_random_probe_sample_rate");
    /**
     * The {@code options.network_optimizer_relay_plr_discount_factor_for_nadl} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_RELAY_PLR_DISCOUNT_FACTOR_FOR_NADL = new VoipParamKey(VoipParamType.FLOAT, "options.network_optimizer_relay_plr_discount_factor_for_nadl");
    /**
     * The {@code options.network_optimizer_transport_switch_sample_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_TRANSPORT_SWITCH_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_transport_switch_sample_size");
    /**
     * The {@code options.network_optimizer_transport_switch_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_OPTIMIZER_TRANSPORT_SWITCH_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.network_optimizer_transport_switch_threshold");
    /**
     * The {@code options.network_quality_active_transport_rx_sample_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_ACTIVE_TRANSPORT_RX_SAMPLE_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_active_transport_rx_sample_period_ms");
    /**
     * The {@code options.network_quality_bind_loss_sample_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_BIND_LOSS_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_bind_loss_sample_size");
    /**
     * The {@code options.network_quality_bind_loss_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_BIND_LOSS_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_bind_loss_timeout_ms");
    /**
     * The {@code options.network_quality_check_bind_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_CHECK_BIND_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_check_bind_period_ms");
    /**
     * The {@code options.network_quality_check_bind_period_ms_sampled} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_CHECK_BIND_PERIOD_MS_SAMPLED = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_check_bind_period_ms_sampled");
    /**
     * The {@code options.network_quality_check_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_CHECK_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_check_mode");
    /**
     * The {@code options.network_quality_metric_log_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_METRIC_LOG_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_metric_log_period_ms");
    /**
     * The {@code options.network_quality_min_sampling_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_MIN_SAMPLING_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_min_sampling_period_ms");
    /**
     * The {@code options.network_quality_plr_stat_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_PLR_STAT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.network_quality_plr_stat_ema_alpha");
    /**
     * The {@code options.network_quality_stat_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_STAT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.network_quality_stat_ema_alpha");
    /**
     * The {@code options.network_quality_supported_protocol_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_NETWORK_QUALITY_SUPPORTED_PROTOCOL_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.network_quality_supported_protocol_bitmap");
    /**
     * The {@code options.nhi_battery_low_en} voip-param.
     */
    public static final VoipParamKey OPTIONS_NHI_BATTERY_LOW_EN = new VoipParamKey(VoipParamType.INTEGER, "options.nhi_battery_low_en");
    /**
     * The {@code options.no_usable_socket_event_reset_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NO_USABLE_SOCKET_EVENT_RESET_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.no_usable_socket_event_reset_interval_ms");
    /**
     * The {@code options.non_def_cell_data_limit_in_kbytes} voip-param.
     */
    public static final VoipParamKey OPTIONS_NON_DEF_CELL_DATA_LIMIT_IN_KBYTES = new VoipParamKey(VoipParamType.INTEGER, "options.non_def_cell_data_limit_in_kbytes");
    /**
     * The {@code options.notify_aud_process_on_mute_state_change} voip-param.
     */
    public static final VoipParamKey OPTIONS_NOTIFY_AUD_PROCESS_ON_MUTE_STATE_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "options.notify_aud_process_on_mute_state_change");
    /**
     * The {@code options.ns_status_prst_afb_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_NS_STATUS_PRST_AFB_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ns_status_prst_afb_interval_ms");
    /**
     * The {@code options.null_call_info_pool_refs_before_release} voip-param.
     */
    public static final VoipParamKey OPTIONS_NULL_CALL_INFO_POOL_REFS_BEFORE_RELEASE = new VoipParamKey(VoipParamType.INTEGER, "options.null_call_info_pool_refs_before_release");
    /**
     * The {@code options.num_file_limit_leeway} voip-param.
     */
    public static final VoipParamKey OPTIONS_NUM_FILE_LIMIT_LEEWAY = new VoipParamKey(VoipParamType.INTEGER, "options.num_file_limit_leeway");
    /**
     * The {@code options.num_max_tcp_connection_retries} voip-param.
     */
    public static final VoipParamKey OPTIONS_NUM_MAX_TCP_CONNECTION_RETRIES = new VoipParamKey(VoipParamType.INTEGER, "options.num_max_tcp_connection_retries");
    /**
     * The {@code options.num_tcp_tunnel_buffer_slots} voip-param.
     */
    public static final VoipParamKey OPTIONS_NUM_TCP_TUNNEL_BUFFER_SLOTS = new VoipParamKey(VoipParamType.INTEGER, "options.num_tcp_tunnel_buffer_slots");
    /**
     * The {@code options.num_vp_afb_after_pause} voip-param.
     */
    public static final VoipParamKey OPTIONS_NUM_VP_AFB_AFTER_PAUSE = new VoipParamKey(VoipParamType.INTEGER, "options.num_vp_afb_after_pause");
    /**
     * The {@code options.oboe_native_frames_per_buffer_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_OBOE_NATIVE_FRAMES_PER_BUFFER_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.oboe_native_frames_per_buffer_enabled");
    /**
     * The {@code options.ohai_request_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_OHAI_REQUEST_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ohai_request_timeout_ms");
    /**
     * The {@code options.oob_bwe_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.oob_bwe_alpha");
    /**
     * The {@code options.oob_bwe_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_enable");
    /**
     * The {@code options.oob_bwe_max_report_bitrate_bps} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_MAX_REPORT_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_max_report_bitrate_bps");
    /**
     * The {@code options.oob_bwe_report_factor} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_REPORT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "options.oob_bwe_report_factor");
    /**
     * The {@code options.oob_bwe_rounding_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_ROUNDING_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_rounding_strategy");
    /**
     * The {@code options.oob_bwe_tx_fixed_bw_bytes} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_TX_FIXED_BW_BYTES = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_tx_fixed_bw_bytes");
    /**
     * The {@code options.oob_bwe_tx_type_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_TX_TYPE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_tx_type_bitmap");
    /**
     * The {@code options.oob_bwe_update_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_OOB_BWE_UPDATE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.oob_bwe_update_interval_ms");
    /**
     * The {@code options.optimize_user_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_OPTIMIZE_USER_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.optimize_user_enable");
    /**
     * The {@code options.optimize_vid_port_frm_buf_allocation} voip-param.
     */
    public static final VoipParamKey OPTIONS_OPTIMIZE_VID_PORT_FRM_BUF_ALLOCATION = new VoipParamKey(VoipParamType.INTEGER, "options.optimize_vid_port_frm_buf_allocation");
    /**
     * The {@code options.optimizer_call_side} voip-param.
     */
    public static final VoipParamKey OPTIONS_OPTIMIZER_CALL_SIDE = new VoipParamKey(VoipParamType.INTEGER, "options.optimizer_call_side");
    /**
     * The {@code options.optimizer_probing_call_side} voip-param.
     */
    public static final VoipParamKey OPTIONS_OPTIMIZER_PROBING_CALL_SIDE = new VoipParamKey(VoipParamType.INTEGER, "options.optimizer_probing_call_side");
    /**
     * The {@code options.opus_fec_cache_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_OPUS_FEC_CACHE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.opus_fec_cache_size");
    /**
     * The {@code options.p2p_enable_delay_after_net_switch_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_ENABLE_DELAY_AFTER_NET_SWITCH_MS = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_enable_delay_after_net_switch_ms");
    /**
     * The {@code options.p2p_first_negotiation_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_FIRST_NEGOTIATION_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_first_negotiation_delay_ms");
    /**
     * The {@code options.p2p_optimization_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_OPTIMIZATION_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_optimization_strategy");
    /**
     * The {@code options.p2p_optimization_strategy_2} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_OPTIMIZATION_STRATEGY_2 = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_optimization_strategy_2");
    /**
     * The {@code options.p2p_preferred_af} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_PREFERRED_AF = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_preferred_af");
    /**
     * The {@code options.p2p_to_relay_fallback_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_TO_RELAY_FALLBACK_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_to_relay_fallback_timeout");
    /**
     * The {@code options.p2p_to_relay_on_rx_relay_frame_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_TO_RELAY_ON_RX_RELAY_FRAME_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_to_relay_on_rx_relay_frame_timeout");
    /**
     * The {@code options.p2p_tx_pct} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_TX_PCT = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_tx_pct");
    /**
     * The {@code options.p2p_worker_thread_init_sleep_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_P2P_WORKER_THREAD_INIT_SLEEP_MS = new VoipParamKey(VoipParamType.INTEGER, "options.p2p_worker_thread_init_sleep_ms");
    /**
     * The {@code options.peer_dec_active_time_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PEER_DEC_ACTIVE_TIME_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.peer_dec_active_time_threshold_ms");
    /**
     * The {@code options.peer_high_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_PEER_HIGH_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.peer_high_bwe");
    /**
     * The {@code options.peer_low_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_PEER_LOW_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.peer_low_bwe");
    /**
     * The {@code options.ping_alt_check_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_CHECK_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_check_interval_ms");
    /**
     * The {@code options.ping_alt_check_start_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_CHECK_START_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_check_start_ms");
    /**
     * The {@code options.ping_alt_payload_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_PAYLOAD_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_payload_size");
    /**
     * The {@code options.ping_alt_round_total} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_ROUND_TOTAL = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_round_total");
    /**
     * The {@code options.ping_alt_threshold_10x_mos} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_THRESHOLD_10X_MOS = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_threshold_10x_mos");
    /**
     * The {@code options.ping_alt_threshold_loss} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_THRESHOLD_LOSS = new VoipParamKey(VoipParamType.FLOAT, "options.ping_alt_threshold_loss");
    /**
     * The {@code options.ping_alt_time_between_rounds_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_TIME_BETWEEN_ROUNDS_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_time_between_rounds_in_msec");
    /**
     * The {@code options.ping_alt_timeout_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_ALT_TIMEOUT_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.ping_alt_timeout_in_msec");
    /**
     * The {@code options.ping_def_payload_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_DEF_PAYLOAD_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.ping_def_payload_size");
    /**
     * The {@code options.ping_def_threshold_10x_mos} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_DEF_THRESHOLD_10X_MOS = new VoipParamKey(VoipParamType.INTEGER, "options.ping_def_threshold_10x_mos");
    /**
     * The {@code options.ping_def_threshold_loss} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_DEF_THRESHOLD_LOSS = new VoipParamKey(VoipParamType.FLOAT, "options.ping_def_threshold_loss");
    /**
     * The {@code options.ping_loss_diff_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_LOSS_DIFF_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.ping_loss_diff_threshold");
    /**
     * The {@code options.ping_summary_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PING_SUMMARY_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.ping_summary_interval_ms");
    /**
     * The {@code options.play_cb_skip_no_frame} voip-param.
     */
    public static final VoipParamKey OPTIONS_PLAY_CB_SKIP_NO_FRAME = new VoipParamKey(VoipParamType.INTEGER, "options.play_cb_skip_no_frame");
    /**
     * The {@code options.precise_rx_timestamps_mask} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRECISE_RX_TIMESTAMPS_MASK = new VoipParamKey(VoipParamType.INTEGER, "options.precise_rx_timestamps_mask");
    /**
     * The {@code options.prefer_host_addr_for_active_addr} voip-param.
     */
    public static final VoipParamKey OPTIONS_PREFER_HOST_ADDR_FOR_ACTIVE_ADDR = new VoipParamKey(VoipParamType.INTEGER, "options.prefer_host_addr_for_active_addr");
    /**
     * The {@code options.prefer_relay_enable_p2p_after_relay_fail} voip-param.
     */
    public static final VoipParamKey OPTIONS_PREFER_RELAY_ENABLE_P2P_AFTER_RELAY_FAIL = new VoipParamKey(VoipParamType.INTEGER, "options.prefer_relay_enable_p2p_after_relay_fail");
    /**
     * The {@code options.prefer_relay_enable_p2p_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PREFER_RELAY_ENABLE_P2P_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.prefer_relay_enable_p2p_timeout_ms");
    /**
     * The {@code options.prefer_relay_fallback_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_PREFER_RELAY_FALLBACK_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.prefer_relay_fallback_threshold");
    /**
     * The {@code options.prefer_relay_with_active_p2p} voip-param.
     */
    public static final VoipParamKey OPTIONS_PREFER_RELAY_WITH_ACTIVE_P2P = new VoipParamKey(VoipParamType.INTEGER, "options.prefer_relay_with_active_p2p");
    /**
     * The {@code options.preserve_all_events_in_event_queue} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRESERVE_ALL_EVENTS_IN_EVENT_QUEUE = new VoipParamKey(VoipParamType.INTEGER, "options.preserve_all_events_in_event_queue");
    /**
     * The {@code options.prioritize_hbh_pli_over_nack} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITIZE_HBH_PLI_OVER_NACK = new VoipParamKey(VoipParamType.INTEGER, "options.prioritize_hbh_pli_over_nack");
    /**
     * The {@code options.priority_tx_queue_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_bitmap");
    /**
     * The {@code options.priority_tx_queue_capacity} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_CAPACITY = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_capacity");
    /**
     * The {@code options.priority_tx_queue_critical_burst_limit} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_CRITICAL_BURST_LIMIT = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_critical_burst_limit");
    /**
     * The {@code options.priority_tx_queue_drain_byte_limit} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_DRAIN_BYTE_LIMIT = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_drain_byte_limit");
    /**
     * The {@code options.priority_tx_queue_max_pkt_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_MAX_PKT_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_max_pkt_size");
    /**
     * The {@code options.priority_tx_queue_stale_thin_every} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_STALE_THIN_EVERY = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_stale_thin_every");
    /**
     * The {@code options.priority_tx_queue_stale_thin_send} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_STALE_THIN_SEND = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_stale_thin_send");
    /**
     * The {@code options.priority_tx_queue_ttl_critical_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_TTL_CRITICAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_ttl_critical_ms");
    /**
     * The {@code options.priority_tx_queue_ttl_normal_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PRIORITY_TX_QUEUE_TTL_NORMAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.priority_tx_queue_ttl_normal_ms");
    /**
     * The {@code options.probe_sample_p2p_retry_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROBE_SAMPLE_P2P_RETRY_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.probe_sample_p2p_retry_mode");
    /**
     * The {@code options.probe_sample_p2p_retry_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROBE_SAMPLE_P2P_RETRY_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.probe_sample_p2p_retry_timeout");
    /**
     * The {@code options.process_hbh_pli} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROCESS_HBH_PLI = new VoipParamKey(VoipParamType.INTEGER, "options.process_hbh_pli");
    /**
     * The {@code options.process_packet_on_jbuf_reset} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROCESS_PACKET_ON_JBUF_RESET = new VoipParamKey(VoipParamType.INTEGER, "options.process_packet_on_jbuf_reset");
    /**
     * The {@code options.propagate_audio_dup_on_remb} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROPAGATE_AUDIO_DUP_ON_REMB = new VoipParamKey(VoipParamType.INTEGER, "options.propagate_audio_dup_on_remb");
    /**
     * The {@code options.propagate_updated_settings_audio_jb} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROPAGATE_UPDATED_SETTINGS_AUDIO_JB = new VoipParamKey(VoipParamType.INTEGER, "options.propagate_updated_settings_audio_jb");
    /**
     * The {@code options.propagate_updated_settings_stream_info} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROPAGATE_UPDATED_SETTINGS_STREAM_INFO = new VoipParamKey(VoipParamType.INTEGER, "options.propagate_updated_settings_stream_info");
    /**
     * The {@code options.proxy_enable_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_PROXY_ENABLE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.proxy_enable_bitmap");
    /**
     * The {@code options.psnr_calc_hw_scaler_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_PSNR_CALC_HW_SCALER_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.psnr_calc_hw_scaler_interval");
    /**
     * The {@code options.pub_ip_change_confirm_period_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_PUB_IP_CHANGE_CONFIRM_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.pub_ip_change_confirm_period_ms");
    /**
     * The {@code options.pub_ip_change_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_PUB_IP_CHANGE_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.pub_ip_change_enable");
    /**
     * The {@code options.pub_ip_change_ping_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_PUB_IP_CHANGE_PING_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.pub_ip_change_ping_interval");
    /**
     * The {@code options.pub_ip_change_send_ping} voip-param.
     */
    public static final VoipParamKey OPTIONS_PUB_IP_CHANGE_SEND_PING = new VoipParamKey(VoipParamType.INTEGER, "options.pub_ip_change_send_ping");
    /**
     * The {@code options.pub_ip_change_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_PUB_IP_CHANGE_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.pub_ip_change_strategy");
    /**
     * The {@code options.read_port_skip_no_frame} voip-param.
     */
    public static final VoipParamKey OPTIONS_READ_PORT_SKIP_NO_FRAME = new VoipParamKey(VoipParamType.INTEGER, "options.read_port_skip_no_frame");
    /**
     * The {@code options.receiver_side_automos_model_name} voip-param.
     */
    public static final VoipParamKey OPTIONS_RECEIVER_SIDE_AUTOMOS_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "options.receiver_side_automos_model_name");
    /**
     * The {@code options.reconnecting_option} voip-param.
     */
    public static final VoipParamKey OPTIONS_RECONNECTING_OPTION = new VoipParamKey(VoipParamType.INTEGER, "options.reconnecting_option");
    /**
     * The {@code options.recreate_socket_on_active_addr} voip-param.
     */
    public static final VoipParamKey OPTIONS_RECREATE_SOCKET_ON_ACTIVE_ADDR = new VoipParamKey(VoipParamType.INTEGER, "options.recreate_socket_on_active_addr");
    /**
     * The {@code options.reenable_p2p_on_peer_network_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_REENABLE_P2P_ON_PEER_NETWORK_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.reenable_p2p_on_peer_network_switch");
    /**
     * The {@code options.refactor_sockaddr_available} voip-param.
     */
    public static final VoipParamKey OPTIONS_REFACTOR_SOCKADDR_AVAILABLE = new VoipParamKey(VoipParamType.INTEGER, "options.refactor_sockaddr_available");
    /**
     * The {@code options.rekey_fanout_format} voip-param.
     */
    public static final VoipParamKey OPTIONS_REKEY_FANOUT_FORMAT = new VoipParamKey(VoipParamType.INTEGER, "options.rekey_fanout_format");
    /**
     * The {@code options.relative_speech_activity_thresholds} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELATIVE_SPEECH_ACTIVITY_THRESHOLDS = new VoipParamKey(VoipParamType.ARRAY, "options.relative_speech_activity_thresholds");
    /**
     * The {@code options.relay_data_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_DATA_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_data_timeout_ms");
    /**
     * The {@code options.relay_e2e_probe_rsp_switch_relay_max_delta_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_E2E_PROBE_RSP_SWITCH_RELAY_MAX_DELTA_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_e2e_probe_rsp_switch_relay_max_delta_ms");
    /**
     * The {@code options.relay_election_latency_update_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_ELECTION_LATENCY_UPDATE_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_election_latency_update_threshold_ms");
    /**
     * The {@code options.relay_ping_before_peer_accept_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_BEFORE_PEER_ACCEPT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_before_peer_accept_interval_ms");
    /**
     * The {@code options.relay_ping_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_interval");
    /**
     * The {@code options.relay_ping_sample_rate} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_SAMPLE_RATE = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_sample_rate");
    /**
     * The {@code options.relay_ping_v2_behavior_tuning_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_BEHAVIOR_TUNING_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_behavior_tuning_bitmap");
    /**
     * The {@code options.relay_ping_v2_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_enable");
    /**
     * The {@code options.relay_ping_v2_enable_nat_hole_stale_check} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_ENABLE_NAT_HOLE_STALE_CHECK = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_enable_nat_hole_stale_check");
    /**
     * The {@code options.relay_ping_v2_extended_ping_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_EXTENDED_PING_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_extended_ping_interval_ms");
    /**
     * The {@code options.relay_ping_v2_keep_alive_all_relays} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_KEEP_ALIVE_ALL_RELAYS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_keep_alive_all_relays");
    /**
     * The {@code options.relay_ping_v2_nat_hole_stale_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_NAT_HOLE_STALE_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_nat_hole_stale_threshold_ms");
    /**
     * The {@code options.relay_ping_v2_ping_peer_used_relays} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_PING_V2_PING_PEER_USED_RELAYS = new VoipParamKey(VoipParamType.INTEGER, "options.relay_ping_v2_ping_peer_used_relays");
    /**
     * The {@code options.relay_unresponsive_reset_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_UNRESPONSIVE_RESET_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.relay_unresponsive_reset_timeout");
    /**
     * The {@code options.relay_unresponsive_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_RELAY_UNRESPONSIVE_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.relay_unresponsive_timeout");
    /**
     * The {@code options.remove_empty_ssrclayer} voip-param.
     */
    public static final VoipParamKey OPTIONS_REMOVE_EMPTY_SSRCLAYER = new VoipParamKey(VoipParamType.INTEGER, "options.remove_empty_ssrclayer");
    /**
     * The {@code options.render_last_frm_copy_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_RENDER_LAST_FRM_COPY_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.render_last_frm_copy_interval");
    /**
     * The {@code options.report_aud_lqm_stats} voip-param.
     */
    public static final VoipParamKey OPTIONS_REPORT_AUD_LQM_STATS = new VoipParamKey(VoipParamType.INTEGER, "options.report_aud_lqm_stats");
    /**
     * The {@code options.report_header_overhead_in_audio_rr} voip-param.
     */
    public static final VoipParamKey OPTIONS_REPORT_HEADER_OVERHEAD_IN_AUDIO_RR = new VoipParamKey(VoipParamType.INTEGER, "options.report_header_overhead_in_audio_rr");
    /**
     * The {@code options.request_net_opt_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_REQUEST_NET_OPT_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.request_net_opt_bitmap");
    /**
     * The {@code options.require_rtp_for_stale_rebind} voip-param.
     */
    public static final VoipParamKey OPTIONS_REQUIRE_RTP_FOR_STALE_REBIND = new VoipParamKey(VoipParamType.INTEGER, "options.require_rtp_for_stale_rebind");
    /**
     * The {@code options.reset_conf_mix_buf_on_downlink_only} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESET_CONF_MIX_BUF_ON_DOWNLINK_ONLY = new VoipParamKey(VoipParamType.INTEGER, "options.reset_conf_mix_buf_on_downlink_only");
    /**
     * The {@code options.reset_hbh_nack_check_init} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESET_HBH_NACK_CHECK_INIT = new VoipParamKey(VoipParamType.INTEGER, "options.reset_hbh_nack_check_init");
    /**
     * The {@code options.reset_protocol_on_switch_connection} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESET_PROTOCOL_ON_SWITCH_CONNECTION = new VoipParamKey(VoipParamType.INTEGER, "options.reset_protocol_on_switch_connection");
    /**
     * The {@code options.reset_vid_dis_part_left} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESET_VID_DIS_PART_LEFT = new VoipParamKey(VoipParamType.INTEGER, "options.reset_vid_dis_part_left");
    /**
     * The {@code options.respect_initial_bitrate_estimate} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESPECT_INITIAL_BITRATE_ESTIMATE = new VoipParamKey(VoipParamType.INTEGER, "options.respect_initial_bitrate_estimate");
    /**
     * The {@code options.restart_audio_on_white_noise} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESTART_AUDIO_ON_WHITE_NOISE = new VoipParamKey(VoipParamType.INTEGER, "options.restart_audio_on_white_noise");
    /**
     * The {@code options.restart_on_net_med_update_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESTART_ON_NET_MED_UPDATE_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.restart_on_net_med_update_delay_ms");
    /**
     * The {@code options.restart_opt_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESTART_OPT_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.restart_opt_bitmap");
    /**
     * The {@code options.restart_p2p_negotiation_upon_group_call_downgrade} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESTART_P2P_NEGOTIATION_UPON_GROUP_CALL_DOWNGRADE = new VoipParamKey(VoipParamType.INTEGER, "options.restart_p2p_negotiation_upon_group_call_downgrade");
    /**
     * The {@code options.resume_device_after_pstn_call} voip-param.
     */
    public static final VoipParamKey OPTIONS_RESUME_DEVICE_AFTER_PSTN_CALL = new VoipParamKey(VoipParamType.INTEGER, "options.resume_device_after_pstn_call");
    /**
     * The {@code options.retry_p2p_imm_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RETRY_P2P_IMM_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.retry_p2p_imm_timeout_ms");
    /**
     * The {@code options.ringback_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_RINGBACK_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.ringback_mode");
    /**
     * The {@code options.rtcp_afb_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_AFB_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_afb_interval");
    /**
     * The {@code options.rtcp_cur_rx_bitrate_calc_win_sz} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_CUR_RX_BITRATE_CALC_WIN_SZ = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_cur_rx_bitrate_calc_win_sz");
    /**
     * The {@code options.rtcp_ignore_time_ms_after_res} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_IGNORE_TIME_MS_AFTER_RES = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_ignore_time_ms_after_res");
    /**
     * The {@code options.rtcp_init_stats_window_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_INIT_STATS_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_init_stats_window_ms");
    /**
     * The {@code options.rtcp_loss_period_slide_window_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_LOSS_PERIOD_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_loss_period_slide_window_size");
    /**
     * The {@code options.rtcp_plr_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_PLR_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.rtcp_plr_ema_alpha");
    /**
     * The {@code options.rtcp_plr_max_disorder_dist} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_PLR_MAX_DISORDER_DIST = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_plr_max_disorder_dist");
    /**
     * The {@code options.rtcp_plr_min_disorder_dist} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_PLR_MIN_DISORDER_DIST = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_plr_min_disorder_dist");
    /**
     * The {@code options.rtcp_report_raw_rtt} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_REPORT_RAW_RTT = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_report_raw_rtt");
    /**
     * The {@code options.rtcp_rtt_min_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_RTT_MIN_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.rtcp_rtt_min_ema_alpha");
    /**
     * The {@code options.rtcp_rx_bitrate_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_RX_BITRATE_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.rtcp_rx_bitrate_ema_alpha");
    /**
     * The {@code options.rtcp_use_new_cur_bitrate} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_USE_NEW_CUR_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_use_new_cur_bitrate");
    /**
     * The {@code options.rtcp_use_tp_fb_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTCP_USE_TP_FB_PLR = new VoipParamKey(VoipParamType.INTEGER, "options.rtcp_use_tp_fb_plr");
    /**
     * The {@code options.rtp_incoming_secure_buf} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTP_INCOMING_SECURE_BUF = new VoipParamKey(VoipParamType.INTEGER, "options.rtp_incoming_secure_buf");
    /**
     * The {@code options.rtp_zero_ext_len_fix_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTP_ZERO_EXT_LEN_FIX_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.rtp_zero_ext_len_fix_enabled");
    /**
     * The {@code options.rtt_ema_num_samples} voip-param.
     */
    public static final VoipParamKey OPTIONS_RTT_EMA_NUM_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "options.rtt_ema_num_samples");
    /**
     * The {@code options.run_rate_control_with_transport_feedback} voip-param.
     */
    public static final VoipParamKey OPTIONS_RUN_RATE_CONTROL_WITH_TRANSPORT_FEEDBACK = new VoipParamKey(VoipParamType.INTEGER, "options.run_rate_control_with_transport_feedback");
    /**
     * The {@code options.rx_gap_detection_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_DETECTION_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_detection_threshold_ms");
    /**
     * The {@code options.rx_gap_monitor_ema_n_dtx} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_EMA_N_DTX = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_monitor_ema_n_dtx");
    /**
     * The {@code options.rx_gap_monitor_ema_n_voice} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_EMA_N_VOICE = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_monitor_ema_n_voice");
    /**
     * The {@code options.rx_gap_monitor_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_monitor_enable");
    /**
     * The {@code options.rx_gap_monitor_gap_event_k_stddev} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_GAP_EVENT_K_STDDEV = new VoipParamKey(VoipParamType.FLOAT, "options.rx_gap_monitor_gap_event_k_stddev");
    /**
     * The {@code options.rx_gap_monitor_gap_event_min_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_GAP_EVENT_MIN_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_monitor_gap_event_min_interval_ms");
    /**
     * The {@code options.rx_gap_monitor_gap_logging_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_GAP_MONITOR_GAP_LOGGING_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_gap_monitor_gap_logging_interval_ms");
    /**
     * The {@code options.rx_interval_to_reset_inferences_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_INTERVAL_TO_RESET_INFERENCES_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_interval_to_reset_inferences_ms");
    /**
     * The {@code options.rx_max_inferences_per_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_MAX_INFERENCES_PER_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.rx_max_inferences_per_interval");
    /**
     * The {@code options.rx_max_queue_size_for_async_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_MAX_QUEUE_SIZE_FOR_ASYNC_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.rx_max_queue_size_for_async_mode");
    /**
     * The {@code options.rx_process_single_chunk_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_PROCESS_SINGLE_CHUNK_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_process_single_chunk_ms");
    /**
     * The {@code options.rx_query_samples_to_skip} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_QUERY_SAMPLES_TO_SKIP = new VoipParamKey(VoipParamType.INTEGER, "options.rx_query_samples_to_skip");
    /**
     * The {@code options.rx_should_delay_inference} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_SHOULD_DELAY_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "options.rx_should_delay_inference");
    /**
     * The {@code options.rx_should_limit_inferences_per_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_SHOULD_LIMIT_INFERENCES_PER_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.rx_should_limit_inferences_per_interval");
    /**
     * The {@code options.rx_sub_min_dur_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_SUB_MIN_DUR_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_sub_min_dur_ms");
    /**
     * The {@code options.rx_sub_vid_stream_resume_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_SUB_VID_STREAM_RESUME_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.rx_sub_vid_stream_resume_fix");
    /**
     * The {@code options.rx_timeout_alloc_resp_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_TIMEOUT_ALLOC_RESP_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_timeout_alloc_resp_ms");
    /**
     * The {@code options.rx_timeout_for_no_media_signal_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_TIMEOUT_FOR_NO_MEDIA_SIGNAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_timeout_for_no_media_signal_ms");
    /**
     * The {@code options.rx_traffic_event_rtp_only_v2} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_TRAFFIC_EVENT_RTP_ONLY_V2 = new VoipParamKey(VoipParamType.INTEGER, "options.rx_traffic_event_rtp_only_v2");
    /**
     * The {@code options.rx_traffic_inactive_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_TRAFFIC_INACTIVE_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_traffic_inactive_threshold_ms");
    /**
     * The {@code options.rx_use_executorch} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_USE_EXECUTORCH = new VoipParamKey(VoipParamType.INTEGER, "options.rx_use_executorch");
    /**
     * The {@code options.rx_use_mutex_for_inference} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_USE_MUTEX_FOR_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "options.rx_use_mutex_for_inference");
    /**
     * The {@code options.rx_wait_time_for_speech_frames_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_RX_WAIT_TIME_FOR_SPEECH_FRAMES_MS = new VoipParamKey(VoipParamType.INTEGER, "options.rx_wait_time_for_speech_frames_ms");
    /**
     * The {@code options.send_accept_before_stream_start} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_ACCEPT_BEFORE_STREAM_START = new VoipParamKey(VoipParamType.INTEGER, "options.send_accept_before_stream_start");
    /**
     * The {@code options.send_batched_relay_latencies} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_BATCHED_RELAY_LATENCIES = new VoipParamKey(VoipParamType.INTEGER, "options.send_batched_relay_latencies");
    /**
     * The {@code options.send_batched_relay_latencies_mutex_fix} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_BATCHED_RELAY_LATENCIES_MUTEX_FIX = new VoipParamKey(VoipParamType.INTEGER, "options.send_batched_relay_latencies_mutex_fix");
    /**
     * The {@code options.send_connect_stat_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_CONNECT_STAT_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.send_connect_stat_timeout_ms");
    /**
     * The {@code options.send_destination_in_gc_relay_latency} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_DESTINATION_IN_GC_RELAY_LATENCY = new VoipParamKey(VoipParamType.INTEGER, "options.send_destination_in_gc_relay_latency");
    /**
     * The {@code options.send_dup_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_DUP_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.send_dup_bitmap");
    /**
     * The {@code options.send_is_xpop_in_connect_stat} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_IS_XPOP_IN_CONNECT_STAT = new VoipParamKey(VoipParamType.INTEGER, "options.send_is_xpop_in_connect_stat");
    /**
     * The {@code options.send_net_health_timeout_in_connect_stat_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_NET_HEALTH_TIMEOUT_IN_CONNECT_STAT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.send_net_health_timeout_in_connect_stat_ms");
    /**
     * The {@code options.send_rtcp_bye_time_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_RTCP_BYE_TIME_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.send_rtcp_bye_time_strategy");
    /**
     * The {@code options.send_rtp_hdr_ext_stream_subscription_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_RTP_HDR_EXT_STREAM_SUBSCRIPTION_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.send_rtp_hdr_ext_stream_subscription_timeout_ms");
    /**
     * The {@code options.send_secondary_bind} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_SECONDARY_BIND = new VoipParamKey(VoipParamType.INTEGER, "options.send_secondary_bind");
    /**
     * The {@code options.send_transport_feedback_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SEND_TRANSPORT_FEEDBACK_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.send_transport_feedback_interval_ms");
    /**
     * The {@code options.sender_side_automos_model_name} voip-param.
     */
    public static final VoipParamKey OPTIONS_SENDER_SIDE_AUTOMOS_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "options.sender_side_automos_model_name");
    /**
     * The {@code options.server_fec_high_bw_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_SERVER_FEC_HIGH_BW_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.server_fec_high_bw_thresh");
    /**
     * The {@code options.server_fec_low_bw_thresh} voip-param.
     */
    public static final VoipParamKey OPTIONS_SERVER_FEC_LOW_BW_THRESH = new VoipParamKey(VoipParamType.INTEGER, "options.server_fec_low_bw_thresh");
    /**
     * The {@code options.set_port_in_host_addr} voip-param.
     */
    public static final VoipParamKey OPTIONS_SET_PORT_IN_HOST_ADDR = new VoipParamKey(VoipParamType.INTEGER, "options.set_port_in_host_addr");
    /**
     * The {@code options.set_rotation_in_capture_cb} voip-param.
     */
    public static final VoipParamKey OPTIONS_SET_ROTATION_IN_CAPTURE_CB = new VoipParamKey(VoipParamType.INTEGER, "options.set_rotation_in_capture_cb");
    /**
     * The {@code options.setup_video_stream_before_accept} voip-param.
     */
    public static final VoipParamKey OPTIONS_SETUP_VIDEO_STREAM_BEFORE_ACCEPT = new VoipParamKey(VoipParamType.INTEGER, "options.setup_video_stream_before_accept");
    /**
     * The {@code options.short_call_t_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SHORT_CALL_T_MS = new VoipParamKey(VoipParamType.INTEGER, "options.short_call_t_ms");
    /**
     * The {@code options.signaling_handler_strategy_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIGNALING_HANDLER_STRATEGY_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.signaling_handler_strategy_interval_ms");
    /**
     * The {@code options.signaling_network_probe_end_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIGNALING_NETWORK_PROBE_END_MS = new VoipParamKey(VoipParamType.INTEGER, "options.signaling_network_probe_end_ms");
    /**
     * The {@code options.signaling_network_probe_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIGNALING_NETWORK_PROBE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.signaling_network_probe_interval_ms");
    /**
     * The {@code options.signaling_network_probe_start_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIGNALING_NETWORK_PROBE_START_MS = new VoipParamKey(VoipParamType.INTEGER, "options.signaling_network_probe_start_ms");
    /**
     * The {@code options.signaling_probe_response_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIGNALING_PROBE_RESPONSE_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.signaling_probe_response_timeout_ms");
    /**
     * The {@code options.silent_detection} voip-param.
     */
    public static final VoipParamKey OPTIONS_SILENT_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "options.silent_detection");
    /**
     * The {@code options.simulate_vpause} voip-param.
     */
    public static final VoipParamKey OPTIONS_SIMULATE_VPAUSE = new VoipParamKey(VoipParamType.INTEGER, "options.simulate_vpause");
    /**
     * The {@code options.skip_alt_net_last_pong_internal_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_ALT_NET_LAST_PONG_INTERNAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.skip_alt_net_last_pong_internal_ms");
    /**
     * The {@code options.skip_choose_ip_version_on_same_config} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_CHOOSE_IP_VERSION_ON_SAME_CONFIG = new VoipParamKey(VoipParamType.INTEGER, "options.skip_choose_ip_version_on_same_config");
    /**
     * The {@code options.skip_delay_update_for_rtx} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_DELAY_UPDATE_FOR_RTX = new VoipParamKey(VoipParamType.INTEGER, "options.skip_delay_update_for_rtx");
    /**
     * The {@code options.skip_local_ip} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_LOCAL_IP = new VoipParamKey(VoipParamType.INTEGER, "options.skip_local_ip");
    /**
     * The {@code options.skip_pjmedia_vid_jb_for_webrtc} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_PJMEDIA_VID_JB_FOR_WEBRTC = new VoipParamKey(VoipParamType.INTEGER, "options.skip_pjmedia_vid_jb_for_webrtc");
    /**
     * The {@code options.skip_restart_when_restart_pending} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_RESTART_WHEN_RESTART_PENDING = new VoipParamKey(VoipParamType.INTEGER, "options.skip_restart_when_restart_pending");
    /**
     * The {@code options.skip_stay_on_alt_net_traffic_gap_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_STAY_ON_ALT_NET_TRAFFIC_GAP_MS = new VoipParamKey(VoipParamType.INTEGER, "options.skip_stay_on_alt_net_traffic_gap_ms");
    /**
     * The {@code options.skip_vid_jb_on_call_ending} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_VID_JB_ON_CALL_ENDING = new VoipParamKey(VoipParamType.INTEGER, "options.skip_vid_jb_on_call_ending");
    /**
     * The {@code options.skip_xor_relayed_addr_in_alloc} voip-param.
     */
    public static final VoipParamKey OPTIONS_SKIP_XOR_RELAYED_ADDR_IN_ALLOC = new VoipParamKey(VoipParamType.INTEGER, "options.skip_xor_relayed_addr_in_alloc");
    /**
     * The {@code options.smart_transport_signal_client_switch} voip-param.
     */
    public static final VoipParamKey OPTIONS_SMART_TRANSPORT_SIGNAL_CLIENT_SWITCH = new VoipParamKey(VoipParamType.INTEGER, "options.smart_transport_signal_client_switch");
    /**
     * The {@code options.sobuf_rcv_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_SOBUF_RCV_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.sobuf_rcv_size");
    /**
     * The {@code options.sobuf_snd_size} voip-param.
     */
    public static final VoipParamKey OPTIONS_SOBUF_SND_SIZE = new VoipParamKey(VoipParamType.INTEGER, "options.sobuf_snd_size");
    /**
     * The {@code options.srtp_decryption_failure_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_SRTP_DECRYPTION_FAILURE_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.srtp_decryption_failure_timeout_ms");
    /**
     * The {@code options.ss_recv_init_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_RECV_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.ss_recv_init_bwe");
    /**
     * The {@code options.ss_recv_init_bwe_cond_min_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_RECV_INIT_BWE_COND_MIN_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.ss_recv_init_bwe_cond_min_bwe");
    /**
     * The {@code options.ss_recv_init_bwe_max_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_RECV_INIT_BWE_MAX_PLR = new VoipParamKey(VoipParamType.FLOAT, "options.ss_recv_init_bwe_max_plr");
    /**
     * The {@code options.ss_sharer_init_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_SHARER_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.ss_sharer_init_bwe");
    /**
     * The {@code options.ss_sharer_init_bwe_cond_min_bwe} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_SHARER_INIT_BWE_COND_MIN_BWE = new VoipParamKey(VoipParamType.INTEGER, "options.ss_sharer_init_bwe_cond_min_bwe");
    /**
     * The {@code options.ss_sharer_init_bwe_max_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_SHARER_INIT_BWE_MAX_PLR = new VoipParamKey(VoipParamType.FLOAT, "options.ss_sharer_init_bwe_max_plr");
    /**
     * The {@code options.ss_sharer_update_vid_scale_type} voip-param.
     */
    public static final VoipParamKey OPTIONS_SS_SHARER_UPDATE_VID_SCALE_TYPE = new VoipParamKey(VoipParamType.INTEGER, "options.ss_sharer_update_vid_scale_type");
    /**
     * The {@code options.st_conn_id_first_byte} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_CONN_ID_FIRST_BYTE = new VoipParamKey(VoipParamType.INTEGER, "options.st_conn_id_first_byte");
    /**
     * The {@code options.st_debug_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_DEBUG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.st_debug_bitmap");
    /**
     * The {@code options.st_enable_conn_id_reset} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_ENABLE_CONN_ID_RESET = new VoipParamKey(VoipParamType.INTEGER, "options.st_enable_conn_id_reset");
    /**
     * The {@code options.st_enable_share_conn_id} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_ENABLE_SHARE_CONN_ID = new VoipParamKey(VoipParamType.INTEGER, "options.st_enable_share_conn_id");
    /**
     * The {@code options.st_enforce_conn_id_presence} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_ENFORCE_CONN_ID_PRESENCE = new VoipParamKey(VoipParamType.INTEGER, "options.st_enforce_conn_id_presence");
    /**
     * The {@code options.st_min_audio_bandwidth_bps} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_MIN_AUDIO_BANDWIDTH_BPS = new VoipParamKey(VoipParamType.INTEGER, "options.st_min_audio_bandwidth_bps");
    /**
     * The {@code options.st_min_video_bandwidth_bps} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_MIN_VIDEO_BANDWIDTH_BPS = new VoipParamKey(VoipParamType.INTEGER, "options.st_min_video_bandwidth_bps");
    /**
     * The {@code options.st_routing_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_ROUTING_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.st_routing_enable");
    /**
     * The {@code options.st_tcp_support} voip-param.
     */
    public static final VoipParamKey OPTIONS_ST_TCP_SUPPORT = new VoipParamKey(VoipParamType.INTEGER, "options.st_tcp_support");
    /**
     * The {@code options.stay_on_alt_net_fix_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_STAY_ON_ALT_NET_FIX_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.stay_on_alt_net_fix_bitmap");
    /**
     * The {@code options.stay_on_alt_net_interface} voip-param.
     */
    public static final VoipParamKey OPTIONS_STAY_ON_ALT_NET_INTERFACE = new VoipParamKey(VoipParamType.INTEGER, "options.stay_on_alt_net_interface");
    /**
     * The {@code options.stft_metrics_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_STFT_METRICS_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.stft_metrics_interval_ms");
    /**
     * The {@code options.stft_metrics_window_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_STFT_METRICS_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "options.stft_metrics_window_ms");
    /**
     * The {@code options.stop_probing_gp_av_upgrade} voip-param.
     */
    public static final VoipParamKey OPTIONS_STOP_PROBING_GP_AV_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "options.stop_probing_gp_av_upgrade");
    /**
     * The {@code options.swap_dominant_speaker_freeze_v1_v2_reporting} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWAP_DOMINANT_SPEAKER_FREEZE_V1_V2_REPORTING = new VoipParamKey(VoipParamType.INTEGER, "options.swap_dominant_speaker_freeze_v1_v2_reporting");
    /**
     * The {@code options.swap_last_min_video_freeze_v1_v2_reporting} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWAP_LAST_MIN_VIDEO_FREEZE_V1_V2_REPORTING = new VoipParamKey(VoipParamType.INTEGER, "options.swap_last_min_video_freeze_v1_v2_reporting");
    /**
     * The {@code options.swap_video_init_freeze_v1_v2_reporting} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWAP_VIDEO_INIT_FREEZE_V1_V2_REPORTING = new VoipParamKey(VoipParamType.INTEGER, "options.swap_video_init_freeze_v1_v2_reporting");
    /**
     * The {@code options.swap_video_render_freeze_v1_v2_reporting} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWAP_VIDEO_RENDER_FREEZE_V1_V2_REPORTING = new VoipParamKey(VoipParamType.INTEGER, "options.swap_video_render_freeze_v1_v2_reporting");
    /**
     * The {@code options.switch_network_jitter_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWITCH_NETWORK_JITTER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.switch_network_jitter_threshold");
    /**
     * The {@code options.switch_network_plr_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWITCH_NETWORK_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.switch_network_plr_threshold");
    /**
     * The {@code options.switch_relay_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_SWITCH_RELAY_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.switch_relay_strategy");
    /**
     * The {@code options.sym_nat_handling_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_SYM_NAT_HANDLING_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.sym_nat_handling_strategy");
    /**
     * The {@code options.sym_nat_max_p2p_retry} voip-param.
     */
    public static final VoipParamKey OPTIONS_SYM_NAT_MAX_P2P_RETRY = new VoipParamKey(VoipParamType.INTEGER, "options.sym_nat_max_p2p_retry");
    /**
     * The {@code options.sym_nat_min_distinct_reflex_ip_cnt} voip-param.
     */
    public static final VoipParamKey OPTIONS_SYM_NAT_MIN_DISTINCT_REFLEX_IP_CNT = new VoipParamKey(VoipParamType.INTEGER, "options.sym_nat_min_distinct_reflex_ip_cnt");
    /**
     * The {@code options.sys_aud_pre_process_gain} voip-param.
     */
    public static final VoipParamKey OPTIONS_SYS_AUD_PRE_PROCESS_GAIN = new VoipParamKey(VoipParamType.FLOAT, "options.sys_aud_pre_process_gain");
    /**
     * The {@code options.tcp_alt_af_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_TCP_ALT_AF_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.tcp_alt_af_bitmap");
    /**
     * The {@code options.tcp_alt_af_timeout_hostile_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TCP_ALT_AF_TIMEOUT_HOSTILE_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tcp_alt_af_timeout_hostile_ms");
    /**
     * The {@code options.tcp_alt_af_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TCP_ALT_AF_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tcp_alt_af_timeout_ms");
    /**
     * The {@code options.tcp_reconnect_interval_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_TCP_RECONNECT_INTERVAL_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.tcp_reconnect_interval_in_msec");
    /**
     * The {@code options.tcp_reconnect_reset_window_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_TCP_RECONNECT_RESET_WINDOW_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.tcp_reconnect_reset_window_in_msec");
    /**
     * The {@code options.test_alt_net_interval_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_ALT_NET_INTERVAL_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.test_alt_net_interval_in_msec");
    /**
     * The {@code options.test_default_net_block_strategy} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_BLOCK_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_block_strategy");
    /**
     * The {@code options.test_default_net_block_switch_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_BLOCK_SWITCH_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_block_switch_count");
    /**
     * The {@code options.test_default_net_block_time_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_BLOCK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_block_time_ms");
    /**
     * The {@code options.test_default_net_check_gap_after_first_media} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_CHECK_GAP_AFTER_FIRST_MEDIA = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_check_gap_after_first_media");
    /**
     * The {@code options.test_default_net_immed_traffic_gap_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_IMMED_TRAFFIC_GAP_MS = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_immed_traffic_gap_ms");
    /**
     * The {@code options.test_default_net_interval_in_msec} voip-param.
     */
    public static final VoipParamKey OPTIONS_TEST_DEFAULT_NET_INTERVAL_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "options.test_default_net_interval_in_msec");
    /**
     * The {@code options.thread_watchdog_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_THREAD_WATCHDOG_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.thread_watchdog_interval_ms");
    /**
     * The {@code options.thread_watchdog_timeout_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_THREAD_WATCHDOG_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "options.thread_watchdog_timeout_ms");
    /**
     * The {@code options.tos_byte_per_net_meidum} voip-param.
     */
    public static final VoipParamKey OPTIONS_TOS_BYTE_PER_NET_MEIDUM = new VoipParamKey(VoipParamType.INTEGER, "options.tos_byte_per_net_meidum");
    /**
     * The {@code options.tp_bwe_monitor_min_sample_cnt} voip-param.
     */
    public static final VoipParamKey OPTIONS_TP_BWE_MONITOR_MIN_SAMPLE_CNT = new VoipParamKey(VoipParamType.INTEGER, "options.tp_bwe_monitor_min_sample_cnt");
    /**
     * The {@code options.tp_bwe_monitor_update_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TP_BWE_MONITOR_UPDATE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tp_bwe_monitor_update_interval_ms");
    /**
     * The {@code options.transition_to_silence_thres_tx_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSITION_TO_SILENCE_THRES_TX_MS = new VoipParamKey(VoipParamType.INTEGER, "options.transition_to_silence_thres_tx_ms");
    /**
     * The {@code options.transition_to_speech_thres_rx_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSITION_TO_SPEECH_THRES_RX_MS = new VoipParamKey(VoipParamType.INTEGER, "options.transition_to_speech_thres_rx_ms");
    /**
     * The {@code options.transition_to_speech_thres_tx_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSITION_TO_SPEECH_THRES_TX_MS = new VoipParamKey(VoipParamType.INTEGER, "options.transition_to_speech_thres_tx_ms");
    /**
     * The {@code options.transport_assert_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_ASSERT_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.transport_assert_bitmap");
    /**
     * The {@code options.transport_comparison_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_COMPARISON_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.transport_comparison_mode");
    /**
     * The {@code options.transport_debug_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_DEBUG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.transport_debug_bitmap");
    /**
     * The {@code options.transport_debug_log_bind_payload} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_DEBUG_LOG_BIND_PAYLOAD = new VoipParamKey(VoipParamType.INTEGER, "options.transport_debug_log_bind_payload");
    /**
     * The {@code options.transport_protocol_policy} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_PROTOCOL_POLICY = new VoipParamKey(VoipParamType.INTEGER, "options.transport_protocol_policy");
    /**
     * The {@code options.transport_restart_on_bound_socket_error_max_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_RESTART_ON_BOUND_SOCKET_ERROR_MAX_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.transport_restart_on_bound_socket_error_max_count");
    /**
     * The {@code options.transport_restart_on_bound_socket_error_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_RESTART_ON_BOUND_SOCKET_ERROR_MS = new VoipParamKey(VoipParamType.INTEGER, "options.transport_restart_on_bound_socket_error_ms");
    /**
     * The {@code options.transport_restart_on_ip_changed_check_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_RESTART_ON_IP_CHANGED_CHECK_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.transport_restart_on_ip_changed_check_interval_ms");
    /**
     * The {@code options.transport_restart_on_ip_changed_max_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_RESTART_ON_IP_CHANGED_MAX_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.transport_restart_on_ip_changed_max_count");
    /**
     * The {@code options.transport_stats_p2p_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRANSPORT_STATS_P2P_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.transport_stats_p2p_threshold");
    /**
     * The {@code options.trigger_weak_wifi_on_tcp_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TRIGGER_WEAK_WIFI_ON_TCP_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.trigger_weak_wifi_on_tcp_delay_ms");
    /**
     * The {@code options.tune_watchdog_bitmap} voip-param.
     */
    public static final VoipParamKey OPTIONS_TUNE_WATCHDOG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "options.tune_watchdog_bitmap");
    /**
     * The {@code options.tx_bitrate_logging_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_BITRATE_LOGGING_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tx_bitrate_logging_interval_ms");
    /**
     * The {@code options.tx_bitrate_tracking} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_BITRATE_TRACKING = new VoipParamKey(VoipParamType.INTEGER, "options.tx_bitrate_tracking");
    /**
     * The {@code options.tx_cache_size_pkts} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_CACHE_SIZE_PKTS = new VoipParamKey(VoipParamType.INTEGER, "options.tx_cache_size_pkts");
    /**
     * The {@code options.tx_interval_to_reset_inferences_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_INTERVAL_TO_RESET_INFERENCES_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tx_interval_to_reset_inferences_ms");
    /**
     * The {@code options.tx_max_inferences_per_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_MAX_INFERENCES_PER_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.tx_max_inferences_per_interval");
    /**
     * The {@code options.tx_max_queue_size_for_async_mode} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_MAX_QUEUE_SIZE_FOR_ASYNC_MODE = new VoipParamKey(VoipParamType.INTEGER, "options.tx_max_queue_size_for_async_mode");
    /**
     * The {@code options.tx_pl_perc_attack_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_PL_PERC_ATTACK_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.tx_pl_perc_attack_ema_alpha");
    /**
     * The {@code options.tx_pl_perc_release_ema_alpha} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_PL_PERC_RELEASE_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "options.tx_pl_perc_release_ema_alpha");
    /**
     * The {@code options.tx_process_single_chunk_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_PROCESS_SINGLE_CHUNK_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tx_process_single_chunk_ms");
    /**
     * The {@code options.tx_query_samples_to_skip} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_QUERY_SAMPLES_TO_SKIP = new VoipParamKey(VoipParamType.INTEGER, "options.tx_query_samples_to_skip");
    /**
     * The {@code options.tx_should_delay_inference} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_SHOULD_DELAY_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "options.tx_should_delay_inference");
    /**
     * The {@code options.tx_should_limit_inferences_per_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_SHOULD_LIMIT_INFERENCES_PER_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.tx_should_limit_inferences_per_interval");
    /**
     * The {@code options.tx_use_executorch} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_USE_EXECUTORCH = new VoipParamKey(VoipParamType.INTEGER, "options.tx_use_executorch");
    /**
     * The {@code options.tx_use_mutex_for_inference} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_USE_MUTEX_FOR_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "options.tx_use_mutex_for_inference");
    /**
     * The {@code options.tx_wait_time_for_speech_frames_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_TX_WAIT_TIME_FOR_SPEECH_FRAMES_MS = new VoipParamKey(VoipParamType.INTEGER, "options.tx_wait_time_for_speech_frames_ms");
    /**
     * The {@code options.unusable_relay_check_e2e_connectivity} voip-param.
     */
    public static final VoipParamKey OPTIONS_UNUSABLE_RELAY_CHECK_E2E_CONNECTIVITY = new VoipParamKey(VoipParamType.INTEGER, "options.unusable_relay_check_e2e_connectivity");
    /**
     * The {@code options.upd_codec_param} voip-param.
     */
    public static final VoipParamKey OPTIONS_UPD_CODEC_PARAM = new VoipParamKey(VoipParamType.INTEGER, "options.upd_codec_param");
    /**
     * The {@code options.upd_strm_param} voip-param.
     */
    public static final VoipParamKey OPTIONS_UPD_STRM_PARAM = new VoipParamKey(VoipParamType.INTEGER, "options.upd_strm_param");
    /**
     * The {@code options.update_initial_minimum_delay_in_neteq} voip-param.
     */
    public static final VoipParamKey OPTIONS_UPDATE_INITIAL_MINIMUM_DELAY_IN_NETEQ = new VoipParamKey(VoipParamType.INTEGER, "options.update_initial_minimum_delay_in_neteq");
    /**
     * The {@code options.update_status_from_send_self_video_state_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_UPDATE_STATUS_FROM_SEND_SELF_VIDEO_STATE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.update_status_from_send_self_video_state_enabled");
    /**
     * The {@code options.use_alt_net_interface} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_ALT_NET_INTERFACE = new VoipParamKey(VoipParamType.INTEGER, "options.use_alt_net_interface");
    /**
     * The {@code options.use_atomic_audio_stream_pause} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_ATOMIC_AUDIO_STREAM_PAUSE = new VoipParamKey(VoipParamType.INTEGER, "options.use_atomic_audio_stream_pause");
    /**
     * The {@code options.use_aud_jitter_stat_default_peer_jid} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_AUD_JITTER_STAT_DEFAULT_PEER_JID = new VoipParamKey(VoipParamType.INTEGER, "options.use_aud_jitter_stat_default_peer_jid");
    /**
     * The {@code options.use_current_dec_active_time_for_res_switch_time} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_CURRENT_DEC_ACTIVE_TIME_FOR_RES_SWITCH_TIME = new VoipParamKey(VoipParamType.INTEGER, "options.use_current_dec_active_time_for_res_switch_time");
    /**
     * The {@code options.use_device_jid_for_relay_latency_peer_lookup} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_DEVICE_JID_FOR_RELAY_LATENCY_PEER_LOOKUP = new VoipParamKey(VoipParamType.INTEGER, "options.use_device_jid_for_relay_latency_peer_lookup");
    /**
     * The {@code options.use_ema_plr} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_EMA_PLR = new VoipParamKey(VoipParamType.INTEGER, "options.use_ema_plr");
    /**
     * The {@code options.use_ema_plr_for_rc_dyn} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_EMA_PLR_FOR_RC_DYN = new VoipParamKey(VoipParamType.INTEGER, "options.use_ema_plr_for_rc_dyn");
    /**
     * The {@code options.use_freeze_disable_reasons} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_FREEZE_DISABLE_REASONS = new VoipParamKey(VoipParamType.INTEGER, "options.use_freeze_disable_reasons");
    /**
     * The {@code options.use_hbh_rtt_for_audio_nacks} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_HBH_RTT_FOR_AUDIO_NACKS = new VoipParamKey(VoipParamType.INTEGER, "options.use_hbh_rtt_for_audio_nacks");
    /**
     * The {@code options.use_hbh_rtt_for_video_nacks} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_HBH_RTT_FOR_VIDEO_NACKS = new VoipParamKey(VoipParamType.INTEGER, "options.use_hbh_rtt_for_video_nacks");
    /**
     * The {@code options.use_maps_audio_processing} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_MAPS_AUDIO_PROCESSING = new VoipParamKey(VoipParamType.INTEGER, "options.use_maps_audio_processing");
    /**
     * The {@code options.use_new_batt_drop_calcs} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_NEW_BATT_DROP_CALCS = new VoipParamKey(VoipParamType.INTEGER, "options.use_new_batt_drop_calcs");
    /**
     * The {@code options.use_ping_loss_diff} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_PING_LOSS_DIFF = new VoipParamKey(VoipParamType.INTEGER, "options.use_ping_loss_diff");
    /**
     * The {@code options.use_relay_rtt_as_init_rtt} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_RELAY_RTT_AS_INIT_RTT = new VoipParamKey(VoipParamType.INTEGER, "options.use_relay_rtt_as_init_rtt");
    /**
     * The {@code options.use_send_rx_sub_from_cache_api} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_SEND_RX_SUB_FROM_CACHE_API = new VoipParamKey(VoipParamType.INTEGER, "options.use_send_rx_sub_from_cache_api");
    /**
     * The {@code options.use_server_prefer_relay} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_SERVER_PREFER_RELAY = new VoipParamKey(VoipParamType.INTEGER, "options.use_server_prefer_relay");
    /**
     * The {@code options.use_total_dec_active_time_for_res_switch_time} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_TOTAL_DEC_ACTIVE_TIME_FOR_RES_SWITCH_TIME = new VoipParamKey(VoipParamType.INTEGER, "options.use_total_dec_active_time_for_res_switch_time");
    /**
     * The {@code options.use_transport_feedback_for_stats_only} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_TRANSPORT_FEEDBACK_FOR_STATS_ONLY = new VoipParamKey(VoipParamType.INTEGER, "options.use_transport_feedback_for_stats_only");
    /**
     * The {@code options.use_transport_rx_ts_for_reconnecting_ui_and_timeout} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_TRANSPORT_RX_TS_FOR_RECONNECTING_UI_AND_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "options.use_transport_rx_ts_for_reconnecting_ui_and_timeout");
    /**
     * The {@code options.use_vid_low_q} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_VID_LOW_Q = new VoipParamKey(VoipParamType.INTEGER, "options.use_vid_low_q");
    /**
     * The {@code options.use_webtc_neon_funcs} voip-param.
     */
    public static final VoipParamKey OPTIONS_USE_WEBTC_NEON_FUNCS = new VoipParamKey(VoipParamType.INTEGER, "options.use_webtc_neon_funcs");
    /**
     * The {@code options.vid_dec_nalus} voip-param.
     */
    public static final VoipParamKey OPTIONS_VID_DEC_NALUS = new VoipParamKey(VoipParamType.INTEGER, "options.vid_dec_nalus");
    /**
     * The {@code options.vid_pkt_reorder_pct} voip-param.
     */
    public static final VoipParamKey OPTIONS_VID_PKT_REORDER_PCT = new VoipParamKey(VoipParamType.FLOAT, "options.vid_pkt_reorder_pct");
    /**
     * The {@code options.vid_port_renderer_buffer_count} voip-param.
     */
    public static final VoipParamKey OPTIONS_VID_PORT_RENDERER_BUFFER_COUNT = new VoipParamKey(VoipParamType.INTEGER, "options.vid_port_renderer_buffer_count");
    /**
     * The {@code options.vid_stream_drop_pkts_when_paused} voip-param.
     */
    public static final VoipParamKey OPTIONS_VID_STREAM_DROP_PKTS_WHEN_PAUSED = new VoipParamKey(VoipParamType.INTEGER, "options.vid_stream_drop_pkts_when_paused");
    /**
     * The {@code options.vid_stream_pause_resume_jb_reset_threshold_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_VID_STREAM_PAUSE_RESUME_JB_RESET_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "options.vid_stream_pause_resume_jb_reset_threshold_ms");
    /**
     * The {@code options.video_brightness_enhancement_bright_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_ENHANCEMENT_BRIGHT_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.video_brightness_enhancement_bright_threshold");
    /**
     * The {@code options.video_brightness_enhancement_calculate_decframe_luminance} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_ENHANCEMENT_CALCULATE_DECFRAME_LUMINANCE = new VoipParamKey(VoipParamType.INTEGER, "options.video_brightness_enhancement_calculate_decframe_luminance");
    /**
     * The {@code options.video_brightness_enhancement_calculate_enhanced_luminance} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_ENHANCEMENT_CALCULATE_ENHANCED_LUMINANCE = new VoipParamKey(VoipParamType.INTEGER, "options.video_brightness_enhancement_calculate_enhanced_luminance");
    /**
     * The {@code options.video_brightness_enhancement_consecutive_frame_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_ENHANCEMENT_CONSECUTIVE_FRAME_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.video_brightness_enhancement_consecutive_frame_threshold");
    /**
     * The {@code options.video_brightness_enhancement_dark_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_ENHANCEMENT_DARK_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "options.video_brightness_enhancement_dark_threshold");
    /**
     * The {@code options.video_brightness_setting} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_BRIGHTNESS_SETTING = new VoipParamKey(VoipParamType.FLOAT, "options.video_brightness_setting");
    /**
     * The {@code options.video_codec_priority} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_CODEC_PRIORITY = new VoipParamKey(VoipParamType.INTEGER, "options.video_codec_priority");
    /**
     * The {@code options.video_composite_brightness_interval} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_COMPOSITE_BRIGHTNESS_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "options.video_composite_brightness_interval");
    /**
     * The {@code options.video_composite_brightness_overexposure_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_COMPOSITE_BRIGHTNESS_OVEREXPOSURE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.video_composite_brightness_overexposure_threshold");
    /**
     * The {@code options.video_composite_brightness_pixel_step} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_COMPOSITE_BRIGHTNESS_PIXEL_STEP = new VoipParamKey(VoipParamType.INTEGER, "options.video_composite_brightness_pixel_step");
    /**
     * The {@code options.video_contrast_setting} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_CONTRAST_SETTING = new VoipParamKey(VoipParamType.FLOAT, "options.video_contrast_setting");
    /**
     * The {@code options.video_edge_sharpening_high_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_EDGE_SHARPENING_HIGH_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.video_edge_sharpening_high_threshold");
    /**
     * The {@code options.video_edge_sharpening_low_threshold} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_EDGE_SHARPENING_LOW_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "options.video_edge_sharpening_low_threshold");
    /**
     * The {@code options.video_edge_sharpening_pixel_step} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_EDGE_SHARPENING_PIXEL_STEP = new VoipParamKey(VoipParamType.INTEGER, "options.video_edge_sharpening_pixel_step");
    /**
     * The {@code options.video_saturation_setting} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_SATURATION_SETTING = new VoipParamKey(VoipParamType.FLOAT, "options.video_saturation_setting");
    /**
     * The {@code options.video_sharpening_setting} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_SHARPENING_SETTING = new VoipParamKey(VoipParamType.FLOAT, "options.video_sharpening_setting");
    /**
     * The {@code options.video_state_txn_id_recv_enforce} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_STATE_TXN_ID_RECV_ENFORCE = new VoipParamKey(VoipParamType.INTEGER, "options.video_state_txn_id_recv_enforce");
    /**
     * The {@code options.video_state_txn_id_send_enabled} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_STATE_TXN_ID_SEND_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "options.video_state_txn_id_send_enabled");
    /**
     * The {@code options.video_upgrade_requestee_tone_desc} voip-param.
     */
    public static final VoipParamKey OPTIONS_VIDEO_UPGRADE_REQUESTEE_TONE_DESC = new VoipParamKey(VoipParamType.STRING, "options.video_upgrade_requestee_tone_desc");
    /**
     * The {@code options.wa_log_time_series} voip-param.
     */
    public static final VoipParamKey OPTIONS_WA_LOG_TIME_SERIES = new VoipParamKey(VoipParamType.INTEGER, "options.wa_log_time_series");
    /**
     * The {@code options.wa_plr_ema_impl_types} voip-param.
     */
    public static final VoipParamKey OPTIONS_WA_PLR_EMA_IMPL_TYPES = new VoipParamKey(VoipParamType.INTEGER, "options.wa_plr_ema_impl_types");
    /**
     * The {@code options.wa_zero_rate_sig} voip-param.
     */
    public static final VoipParamKey OPTIONS_WA_ZERO_RATE_SIG = new VoipParamKey(VoipParamType.INTEGER, "options.wa_zero_rate_sig");
    /**
     * The {@code options.webrtc_jb_av_sync_interval_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_WEBRTC_JB_AV_SYNC_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "options.webrtc_jb_av_sync_interval_ms");
    /**
     * The {@code options.webrtc_jb_enable_av_sync} voip-param.
     */
    public static final VoipParamKey OPTIONS_WEBRTC_JB_ENABLE_AV_SYNC = new VoipParamKey(VoipParamType.INTEGER, "options.webrtc_jb_enable_av_sync");
    /**
     * The {@code options.webrtc_jb_max_audio_sync_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_WEBRTC_JB_MAX_AUDIO_SYNC_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.webrtc_jb_max_audio_sync_delay_ms");
    /**
     * The {@code options.webrtc_jb_max_relative_delay_ms} voip-param.
     */
    public static final VoipParamKey OPTIONS_WEBRTC_JB_MAX_RELATIVE_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "options.webrtc_jb_max_relative_delay_ms");
    /**
     * The {@code options.xnet_med_p2p_policy_enable} voip-param.
     */
    public static final VoipParamKey OPTIONS_XNET_MED_P2P_POLICY_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "options.xnet_med_p2p_policy_enable");
    /**
     * The {@code options.xnet_med_p2p_policy_max_ms_since_last_rx} voip-param.
     */
    public static final VoipParamKey OPTIONS_XNET_MED_P2P_POLICY_MAX_MS_SINCE_LAST_RX = new VoipParamKey(VoipParamType.INTEGER, "options.xnet_med_p2p_policy_max_ms_since_last_rx");
    /**
     * The {@code options.xpop_relay_election_scheme} voip-param.
     */
    public static final VoipParamKey OPTIONS_XPOP_RELAY_ELECTION_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "options.xpop_relay_election_scheme");
    /**
     * The {@code options.xpop_relay_latency_discount_factor} voip-param.
     */
    public static final VoipParamKey OPTIONS_XPOP_RELAY_LATENCY_DISCOUNT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "options.xpop_relay_latency_discount_factor");
    /**
     * The {@code plr_predictor.coeff_plr_above1_count} voip-param.
     */
    public static final VoipParamKey PLR_PREDICTOR_COEFF_PLR_ABOVE1_COUNT = new VoipParamKey(VoipParamType.FLOAT, "plr_predictor.coeff_plr_above1_count");
    /**
     * The {@code plr_predictor.coeff_plr_ema} voip-param.
     */
    public static final VoipParamKey PLR_PREDICTOR_COEFF_PLR_EMA = new VoipParamKey(VoipParamType.FLOAT, "plr_predictor.coeff_plr_ema");
    /**
     * The {@code plr_predictor.coeff_plr_ema_trend} voip-param.
     */
    public static final VoipParamKey PLR_PREDICTOR_COEFF_PLR_EMA_TREND = new VoipParamKey(VoipParamType.FLOAT, "plr_predictor.coeff_plr_ema_trend");
    /**
     * The {@code plr_predictor.coeff_plr_over_ema} voip-param.
     */
    public static final VoipParamKey PLR_PREDICTOR_COEFF_PLR_OVER_EMA = new VoipParamKey(VoipParamType.FLOAT, "plr_predictor.coeff_plr_over_ema");
    /**
     * The {@code plr_predictor.coeff_plr_stddev} voip-param.
     */
    public static final VoipParamKey PLR_PREDICTOR_COEFF_PLR_STDDEV = new VoipParamKey(VoipParamType.FLOAT, "plr_predictor.coeff_plr_stddev");
    /**
     * The {@code rc.abs_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey RC_ABS_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.abs_rtt_congestion_threshold");
    /**
     * The {@code rc.abs_rtt_on_hold_threshold} voip-param.
     */
    public static final VoipParamKey RC_ABS_RTT_ON_HOLD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.abs_rtt_on_hold_threshold");
    /**
     * The {@code rc.allow_frame_timestamp_to_regress} voip-param.
     */
    public static final VoipParamKey RC_ALLOW_FRAME_TIMESTAMP_TO_REGRESS = new VoipParamKey(VoipParamType.INTEGER, "rc.allow_frame_timestamp_to_regress");
    /**
     * The {@code rc.allow_no_delay_duplication} voip-param.
     */
    public static final VoipParamKey RC_ALLOW_NO_DELAY_DUPLICATION = new VoipParamKey(VoipParamType.INTEGER, "rc.allow_no_delay_duplication");
    /**
     * The {@code rc.allow_pli_under_all_ltrp} voip-param.
     */
    public static final VoipParamKey RC_ALLOW_PLI_UNDER_ALL_LTRP = new VoipParamKey(VoipParamType.INTEGER, "rc.allow_pli_under_all_ltrp");
    /**
     * The {@code rc.android_decoder_deq_out_time_ms} voip-param.
     */
    public static final VoipParamKey RC_ANDROID_DECODER_DEQ_OUT_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.android_decoder_deq_out_time_ms");
    /**
     * The {@code rc.android_media_codec_init_height} voip-param.
     */
    public static final VoipParamKey RC_ANDROID_MEDIA_CODEC_INIT_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "rc.android_media_codec_init_height");
    /**
     * The {@code rc.android_media_codec_init_width} voip-param.
     */
    public static final VoipParamKey RC_ANDROID_MEDIA_CODEC_INIT_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "rc.android_media_codec_init_width");
    /**
     * The {@code rc.audio_bitrate_cap} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_BITRATE_CAP = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_bitrate_cap");
    /**
     * The {@code rc.audio_duplication_exploration_max_duration_ms} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_DUPLICATION_EXPLORATION_MAX_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_duplication_exploration_max_duration_ms");
    /**
     * The {@code rc.audio_duplication_exploration_min_duration_ms} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_DUPLICATION_EXPLORATION_MIN_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_duplication_exploration_min_duration_ms");
    /**
     * The {@code rc.audio_duplication_exploration_min_wait_time_ms} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_DUPLICATION_EXPLORATION_MIN_WAIT_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_duplication_exploration_min_wait_time_ms");
    /**
     * The {@code rc.audio_duplication_off_exploration_prob_perc} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_DUPLICATION_OFF_EXPLORATION_PROB_PERC = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_duplication_off_exploration_prob_perc");
    /**
     * The {@code rc.audio_duplication_on_exploration_prob_perc} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_DUPLICATION_ON_EXPLORATION_PROB_PERC = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_duplication_on_exploration_prob_perc");
    /**
     * The {@code rc.audio_fec_disable_encoding} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_FEC_DISABLE_ENCODING = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_fec_disable_encoding");
    /**
     * The {@code rc.audio_nack_algo_mask} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_NACK_ALGO_MASK = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_nack_algo_mask");
    /**
     * The {@code rc.audio_nack_disable_rtp_retransmit} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_NACK_DISABLE_RTP_RETRANSMIT = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_nack_disable_rtp_retransmit");
    /**
     * The {@code rc.audio_nack_discarded_frames} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_NACK_DISCARDED_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_nack_discarded_frames");
    /**
     * The {@code rc.audio_nack_max_jb_delay} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_NACK_MAX_JB_DELAY = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_nack_max_jb_delay");
    /**
     * The {@code rc.audio_oob_fec_max_pkts} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_OOB_FEC_MAX_PKTS = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_oob_fec_max_pkts");
    /**
     * The {@code rc.audio_oob_fec_min_pkts} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_OOB_FEC_MIN_PKTS = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_oob_fec_min_pkts");
    /**
     * The {@code rc.audio_oob_fec_ratio} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_OOB_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "rc.audio_oob_fec_ratio");
    /**
     * The {@code rc.audio_piggyback_timeout_msec} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_PIGGYBACK_TIMEOUT_MSEC = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_piggyback_timeout_msec");
    /**
     * The {@code rc.audio_resend_interval_for_exploration_msec} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_RESEND_INTERVAL_FOR_EXPLORATION_MSEC = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_resend_interval_for_exploration_msec");
    /**
     * The {@code rc.audio_resend_interval_msec} voip-param.
     */
    public static final VoipParamKey RC_AUDIO_RESEND_INTERVAL_MSEC = new VoipParamKey(VoipParamType.INTEGER, "rc.audio_resend_interval_msec");
    /**
     * The {@code rc.block_step_for_motion_analysis} voip-param.
     */
    public static final VoipParamKey RC_BLOCK_STEP_FOR_MOTION_ANALYSIS = new VoipParamKey(VoipParamType.INTEGER, "rc.block_step_for_motion_analysis");
    /**
     * The {@code rc.blocking_fmt_change_event} voip-param.
     */
    public static final VoipParamKey RC_BLOCKING_FMT_CHANGE_EVENT = new VoipParamKey(VoipParamType.INTEGER, "rc.blocking_fmt_change_event");
    /**
     * The {@code rc.cc_bwe_enable_ramp_up_for_random_packet_loss} voip-param.
     */
    public static final VoipParamKey RC_CC_BWE_ENABLE_RAMP_UP_FOR_RANDOM_PACKET_LOSS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_bwe_enable_ramp_up_for_random_packet_loss");
    /**
     * The {@code rc.cc_cap_rl_bwe_to_max_target} voip-param.
     */
    public static final VoipParamKey RC_CC_CAP_RL_BWE_TO_MAX_TARGET = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_cap_rl_bwe_to_max_target");
    /**
     * The {@code rc.cc_enable_ml_plc_inference} voip-param.
     */
    public static final VoipParamKey RC_CC_ENABLE_ML_PLC_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_enable_ml_plc_inference");
    /**
     * The {@code rc.cc_enable_offline_rl_bwe_inference} voip-param.
     */
    public static final VoipParamKey RC_CC_ENABLE_OFFLINE_RL_BWE_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_enable_offline_rl_bwe_inference");
    /**
     * The {@code rc.cc_hd_targeting_model_input_feature_agg_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_HD_TARGETING_MODEL_INPUT_FEATURE_AGG_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_hd_targeting_model_input_feature_agg_mode");
    /**
     * The {@code rc.cc_ml_cong_should_fill_zero_pp} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_CONG_SHOULD_FILL_ZERO_PP = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_cong_should_fill_zero_pp");
    /**
     * The {@code rc.cc_ml_inference_num_threads} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_INFERENCE_NUM_THREADS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_inference_num_threads");
    /**
     * The {@code rc.cc_ml_model_agg_mode_for_training} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_MODEL_AGG_MODE_FOR_TRAINING = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_model_agg_mode_for_training");
    /**
     * The {@code rc.cc_ml_model_load_max_retry} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_MODEL_LOAD_MAX_RETRY = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_model_load_max_retry");
    /**
     * The {@code rc.cc_ml_model_load_retry_interval} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_MODEL_LOAD_RETRY_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_model_load_retry_interval");
    /**
     * The {@code rc.cc_ml_model_load_trigger_point} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_MODEL_LOAD_TRIGGER_POINT = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_model_load_trigger_point");
    /**
     * The {@code rc.cc_ml_model_only_load_max_retry} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_MODEL_ONLY_LOAD_MAX_RETRY = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_model_only_load_max_retry");
    /**
     * The {@code rc.cc_ml_plc_n_feature} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_PLC_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_plc_n_feature");
    /**
     * The {@code rc.cc_ml_pytorch_load_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_PYTORCH_LOAD_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_pytorch_load_mode");
    /**
     * The {@code rc.cc_ml_random_plc_probability_threshold} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_RANDOM_PLC_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_random_plc_probability_threshold");
    /**
     * The {@code rc.cc_ml_should_fill_zero_pp} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_SHOULD_FILL_ZERO_PP = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_should_fill_zero_pp");
    /**
     * The {@code rc.cc_ml_undershoot_n_feature} voip-param.
     */
    public static final VoipParamKey RC_CC_ML_UNDERSHOOT_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_ml_undershoot_n_feature");
    /**
     * The {@code rc.cc_offline_rl_bwe_model_input_feature_agg_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_OFFLINE_RL_BWE_MODEL_INPUT_FEATURE_AGG_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_offline_rl_bwe_model_input_feature_agg_mode");
    /**
     * The {@code rc.cc_plc_model_input_feature_agg_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_PLC_MODEL_INPUT_FEATURE_AGG_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_plc_model_input_feature_agg_mode");
    /**
     * The {@code rc.cc_rl_bwe_blend_alpha} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_BLEND_ALPHA = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_blend_alpha");
    /**
     * The {@code rc.cc_rl_bwe_check_range} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_CHECK_RANGE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_check_range");
    /**
     * The {@code rc.cc_rl_bwe_check_rate_of_change} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_CHECK_RATE_OF_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_check_rate_of_change");
    /**
     * The {@code rc.cc_rl_bwe_check_tfrc_divergence} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_CHECK_TFRC_DIVERGENCE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_check_tfrc_divergence");
    /**
     * The {@code rc.cc_rl_bwe_check_variance} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_CHECK_VARIANCE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_check_variance");
    /**
     * The {@code rc.cc_rl_bwe_cooldown_duration_ms} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_COOLDOWN_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_cooldown_duration_ms");
    /**
     * The {@code rc.cc_rl_bwe_cooldown_enabled} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_COOLDOWN_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_cooldown_enabled");
    /**
     * The {@code rc.cc_rl_bwe_cooldown_trigger_count} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_COOLDOWN_TRIGGER_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_cooldown_trigger_count");
    /**
     * The {@code rc.cc_rl_bwe_feedback_to_slide_window} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_FEEDBACK_TO_SLIDE_WINDOW = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_feedback_to_slide_window");
    /**
     * The {@code rc.cc_rl_bwe_max_change_ratio} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MAX_CHANGE_RATIO = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_max_change_ratio");
    /**
     * The {@code rc.cc_rl_bwe_max_reasonable_bps} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MAX_REASONABLE_BPS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_max_reasonable_bps");
    /**
     * The {@code rc.cc_rl_bwe_max_tfrc_ratio} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MAX_TFRC_RATIO = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_max_tfrc_ratio");
    /**
     * The {@code rc.cc_rl_bwe_min_reasonable_bps} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MIN_REASONABLE_BPS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_min_reasonable_bps");
    /**
     * The {@code rc.cc_rl_bwe_min_tfrc_ratio} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MIN_TFRC_RATIO = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_min_tfrc_ratio");
    /**
     * The {@code rc.cc_rl_bwe_min_variance_bps} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_MIN_VARIANCE_BPS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_min_variance_bps");
    /**
     * The {@code rc.cc_rl_bwe_on_fail} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_ON_FAIL = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_on_fail");
    /**
     * The {@code rc.cc_rl_bwe_swap_nm_max_target} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_SWAP_NM_MAX_TARGET = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_swap_nm_max_target");
    /**
     * The {@code rc.cc_rl_bwe_tfrc_div_window_ms} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_TFRC_DIV_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_tfrc_div_window_ms");
    /**
     * The {@code rc.cc_rl_bwe_variance_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_CC_RL_BWE_VARIANCE_EMA_ALPHA = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_rl_bwe_variance_ema_alpha");
    /**
     * The {@code rc.cc_rtt_approaching_congestion_multiplier} voip-param.
     */
    public static final VoipParamKey RC_CC_RTT_APPROACHING_CONGESTION_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.cc_rtt_approaching_congestion_multiplier");
    /**
     * The {@code rc.cc_rtt_heavily_congestion_multiplier} voip-param.
     */
    public static final VoipParamKey RC_CC_RTT_HEAVILY_CONGESTION_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.cc_rtt_heavily_congestion_multiplier");
    /**
     * The {@code rc.cc_skip_pytorch_impl_for_executorch} voip-param.
     */
    public static final VoipParamKey RC_CC_SKIP_PYTORCH_IMPL_FOR_EXECUTORCH = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_skip_pytorch_impl_for_executorch");
    /**
     * The {@code rc.cc_tr_model_input_feature_agg_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_TR_MODEL_INPUT_FEATURE_AGG_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_tr_model_input_feature_agg_mode");
    /**
     * The {@code rc.cc_tx_and_peer_rx_slide_window_size} voip-param.
     */
    public static final VoipParamKey RC_CC_TX_AND_PEER_RX_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_tx_and_peer_rx_slide_window_size");
    /**
     * The {@code rc.cc_undershoot_model_input_feature_agg_mode} voip-param.
     */
    public static final VoipParamKey RC_CC_UNDERSHOOT_MODEL_INPUT_FEATURE_AGG_MODE = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_undershoot_model_input_feature_agg_mode");
    /**
     * The {@code rc.cc_use_mlp_model} voip-param.
     */
    public static final VoipParamKey RC_CC_USE_MLP_MODEL = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_use_mlp_model");
    /**
     * The {@code rc.cc_use_offline_rl_bwe_as_target} voip-param.
     */
    public static final VoipParamKey RC_CC_USE_OFFLINE_RL_BWE_AS_TARGET = new VoipParamKey(VoipParamType.INTEGER, "rc.cc_use_offline_rl_bwe_as_target");
    /**
     * The {@code rc.check_reset_frame_all_pli} voip-param.
     */
    public static final VoipParamKey RC_CHECK_RESET_FRAME_ALL_PLI = new VoipParamKey(VoipParamType.INTEGER, "rc.check_reset_frame_all_pli");
    /**
     * The {@code rc.check_reset_frame_jb_covered} voip-param.
     */
    public static final VoipParamKey RC_CHECK_RESET_FRAME_JB_COVERED = new VoipParamKey(VoipParamType.INTEGER, "rc.check_reset_frame_jb_covered");
    /**
     * The {@code rc.codec_impl} voip-param.
     */
    public static final VoipParamKey RC_CODEC_IMPL = new VoipParamKey(VoipParamType.INTEGER, "rc.codec_impl");
    /**
     * The {@code rc.codec_type} voip-param.
     */
    public static final VoipParamKey RC_CODEC_TYPE = new VoipParamKey(VoipParamType.INTEGER, "rc.codec_type");
    /**
     * The {@code rc.comb_psnr_ema_sample_size} voip-param.
     */
    public static final VoipParamKey RC_COMB_PSNR_EMA_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.comb_psnr_ema_sample_size");
    /**
     * The {@code rc.compound_npsi} voip-param.
     */
    public static final VoipParamKey RC_COMPOUND_NPSI = new VoipParamKey(VoipParamType.INTEGER, "rc.compound_npsi");
    /**
     * The {@code rc.cond_congestion_abs_rtt_thr} voip-param.
     */
    public static final VoipParamKey RC_COND_CONGESTION_ABS_RTT_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_congestion_abs_rtt_thr");
    /**
     * The {@code rc.cond_congestion_no_data_thr} voip-param.
     */
    public static final VoipParamKey RC_COND_CONGESTION_NO_DATA_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_congestion_no_data_thr");
    /**
     * The {@code rc.cond_congestion_no_init_rtt_thr} voip-param.
     */
    public static final VoipParamKey RC_COND_CONGESTION_NO_INIT_RTT_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_congestion_no_init_rtt_thr");
    /**
     * The {@code rc.cond_congestion_no_rtcp_thr} voip-param.
     */
    public static final VoipParamKey RC_COND_CONGESTION_NO_RTCP_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_congestion_no_rtcp_thr");
    /**
     * The {@code rc.cond_congestion_signal_mask} voip-param.
     */
    public static final VoipParamKey RC_COND_CONGESTION_SIGNAL_MASK = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_congestion_signal_mask");
    /**
     * The {@code rc.cond_jb_delay_hysteresis} voip-param.
     */
    public static final VoipParamKey RC_COND_JB_DELAY_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_jb_delay_hysteresis");
    /**
     * The {@code rc.cond_jb_last_delay_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_COND_JB_LAST_DELAY_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.cond_jb_last_delay_ema_alpha");
    /**
     * The {@code rc.cond_net_medium} voip-param.
     */
    public static final VoipParamKey RC_COND_NET_MEDIUM = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_net_medium");
    /**
     * The {@code rc.cond_packet_loss_pct_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_COND_PACKET_LOSS_PCT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.cond_packet_loss_pct_ema_alpha");
    /**
     * The {@code rc.cond_packet_rx_loss_pct_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_COND_PACKET_RX_LOSS_PCT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.cond_packet_rx_loss_pct_ema_alpha");
    /**
     * The {@code rc.cond_peer_net_medium} voip-param.
     */
    public static final VoipParamKey RC_COND_PEER_NET_MEDIUM = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_peer_net_medium");
    /**
     * The {@code rc.cond_peer_platform_mask} voip-param.
     */
    public static final VoipParamKey RC_COND_PEER_PLATFORM_MASK = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_peer_platform_mask");
    /**
     * The {@code rc.cond_peer_uaqc_state} voip-param.
     */
    public static final VoipParamKey RC_COND_PEER_UAQC_STATE = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_peer_uaqc_state");
    /**
     * The {@code rc.cond_permanent_if_matched} voip-param.
     */
    public static final VoipParamKey RC_COND_PERMANENT_IF_MATCHED = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_permanent_if_matched");
    /**
     * The {@code rc.cond_pip_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_COND_PIP_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_pip_threshold_ms");
    /**
     * The {@code rc.cond_pkt_loss_hysteresis} voip-param.
     */
    public static final VoipParamKey RC_COND_PKT_LOSS_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_pkt_loss_hysteresis");
    /**
     * The {@code rc.cond_platform_mask} voip-param.
     */
    public static final VoipParamKey RC_COND_PLATFORM_MASK = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_platform_mask");
    /**
     * The {@code rc.cond_plr_predictor_state} voip-param.
     */
    public static final VoipParamKey RC_COND_PLR_PREDICTOR_STATE = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_plr_predictor_state");
    /**
     * The {@code rc.cond_range_avg_loss_count} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_AVG_LOSS_COUNT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_avg_loss_count");
    /**
     * The {@code rc.cond_range_bwe} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_BWE = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_bwe");
    /**
     * The {@code rc.cond_range_ema_jb_last_delay} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_EMA_JB_LAST_DELAY = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ema_jb_last_delay");
    /**
     * The {@code rc.cond_range_ema_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_EMA_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ema_packet_loss_pct");
    /**
     * The {@code rc.cond_range_ema_rtt} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_EMA_RTT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ema_rtt");
    /**
     * The {@code rc.cond_range_ema_rx_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_EMA_RX_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ema_rx_packet_loss_pct");
    /**
     * The {@code rc.cond_range_ema_uplink_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_EMA_UPLINK_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ema_uplink_packet_loss_pct");
    /**
     * The {@code rc.cond_range_gcall_size} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_GCALL_SIZE = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_gcall_size");
    /**
     * The {@code rc.cond_range_goodput_peer_downlink} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_GOODPUT_PEER_DOWNLINK = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_goodput_peer_downlink");
    /**
     * The {@code rc.cond_range_jb_avg_delay} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_JB_AVG_DELAY = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_jb_avg_delay");
    /**
     * The {@code rc.cond_range_jb_last_delay} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_JB_LAST_DELAY = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_jb_last_delay");
    /**
     * The {@code rc.cond_range_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_packet_loss_pct");
    /**
     * The {@code rc.cond_range_rtt} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_RTT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_rtt");
    /**
     * The {@code rc.cond_range_rx_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_RX_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_rx_packet_loss_pct");
    /**
     * The {@code rc.cond_range_sec_since_start} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_SEC_SINCE_START = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_sec_since_start");
    /**
     * The {@code rc.cond_range_target_total_bitrate} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_TARGET_TOTAL_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_target_total_bitrate");
    /**
     * The {@code rc.cond_range_tx_bwe} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_TX_BWE = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_tx_bwe");
    /**
     * The {@code rc.cond_range_ul_bwe} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_UL_BWE = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_ul_bwe");
    /**
     * The {@code rc.cond_range_unused_uplink_bw} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_UNUSED_UPLINK_BW = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_unused_uplink_bw");
    /**
     * The {@code rc.cond_range_uplink_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_UPLINK_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_uplink_packet_loss_pct");
    /**
     * The {@code rc.cond_range_uplink_rtt} voip-param.
     */
    public static final VoipParamKey RC_COND_RANGE_UPLINK_RTT = new VoipParamKey(VoipParamType.ARRAY, "rc.cond_range_uplink_rtt");
    /**
     * The {@code rc.cond_rtt_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_COND_RTT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.cond_rtt_ema_alpha");
    /**
     * The {@code rc.cond_rtt_hysteresis} voip-param.
     */
    public static final VoipParamKey RC_COND_RTT_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_rtt_hysteresis");
    /**
     * The {@code rc.cond_total_bitrate_hysteresis} voip-param.
     */
    public static final VoipParamKey RC_COND_TOTAL_BITRATE_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_total_bitrate_hysteresis");
    /**
     * The {@code rc.cond_tx_bwe_hysteresis} voip-param.
     */
    public static final VoipParamKey RC_COND_TX_BWE_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "rc.cond_tx_bwe_hysteresis");
    /**
     * The {@code rc.conservative_mode_apply_cong_ceiling} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_APPLY_CONG_CEILING = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_apply_cong_ceiling");
    /**
     * The {@code rc.conservative_mode_bw_thresh_kbps} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_BW_THRESH_KBPS = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_bw_thresh_kbps");
    /**
     * The {@code rc.conservative_mode_ceiling_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_CEILING_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.conservative_mode_ceiling_ema_alpha");
    /**
     * The {@code rc.conservative_mode_ceiling_num_samples} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_CEILING_NUM_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_ceiling_num_samples");
    /**
     * The {@code rc.conservative_mode_ceiling_pct} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_CEILING_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_ceiling_pct");
    /**
     * The {@code rc.conservative_mode_disable_by_init_bwe} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_DISABLE_BY_INIT_BWE = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_disable_by_init_bwe");
    /**
     * The {@code rc.conservative_mode_enabled} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_enabled");
    /**
     * The {@code rc.conservative_mode_explore_backoff_thresh_pct} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_EXPLORE_BACKOFF_THRESH_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_explore_backoff_thresh_pct");
    /**
     * The {@code rc.conservative_mode_explore_max_wait_t} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_EXPLORE_MAX_WAIT_T = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_explore_max_wait_t");
    /**
     * The {@code rc.conservative_mode_explore_min_wait_t} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_EXPLORE_MIN_WAIT_T = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_explore_min_wait_t");
    /**
     * The {@code rc.conservative_mode_explore_stateful} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_EXPLORE_STATEFUL = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_explore_stateful");
    /**
     * The {@code rc.conservative_mode_explore_thresh_pct} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_EXPLORE_THRESH_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_explore_thresh_pct");
    /**
     * The {@code rc.conservative_mode_override_rd_target_lower_bound} voip-param.
     */
    public static final VoipParamKey RC_CONSERVATIVE_MODE_OVERRIDE_RD_TARGET_LOWER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "rc.conservative_mode_override_rd_target_lower_bound");
    /**
     * The {@code rc.consume_rtp_ext_plr} voip-param.
     */
    public static final VoipParamKey RC_CONSUME_RTP_EXT_PLR = new VoipParamKey(VoipParamType.INTEGER, "rc.consume_rtp_ext_plr");
    /**
     * The {@code rc.content_detector_inter_blocks_thresh_perc} voip-param.
     */
    public static final VoipParamKey RC_CONTENT_DETECTOR_INTER_BLOCKS_THRESH_PERC = new VoipParamKey(VoipParamType.INTEGER, "rc.content_detector_inter_blocks_thresh_perc");
    /**
     * The {@code rc.content_detector_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_CONTENT_DETECTOR_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.content_detector_interval_ms");
    /**
     * The {@code rc.content_detector_luma_variance_thresh_perc} voip-param.
     */
    public static final VoipParamKey RC_CONTENT_DETECTOR_LUMA_VARIANCE_THRESH_PERC = new VoipParamKey(VoipParamType.INTEGER, "rc.content_detector_luma_variance_thresh_perc");
    /**
     * The {@code rc.content_detector_skip_blocks_thresh_perc} voip-param.
     */
    public static final VoipParamKey RC_CONTENT_DETECTOR_SKIP_BLOCKS_THRESH_PERC = new VoipParamKey(VoipParamType.INTEGER, "rc.content_detector_skip_blocks_thresh_perc");
    /**
     * The {@code rc.dav1d_dec_thread_count} voip-param.
     */
    public static final VoipParamKey RC_DAV1D_DEC_THREAD_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.dav1d_dec_thread_count");
    /**
     * The {@code rc.disable_conservative_mode_in_group_segment} voip-param.
     */
    public static final VoipParamKey RC_DISABLE_CONSERVATIVE_MODE_IN_GROUP_SEGMENT = new VoipParamKey(VoipParamType.INTEGER, "rc.disable_conservative_mode_in_group_segment");
    /**
     * The {@code rc.disable_rtcp_remb} voip-param.
     */
    public static final VoipParamKey RC_DISABLE_RTCP_REMB = new VoipParamKey(VoipParamType.INTEGER, "rc.disable_rtcp_remb");
    /**
     * The {@code rc.disable_rtt_congestion_detection_with_ice_rtt} voip-param.
     */
    public static final VoipParamKey RC_DISABLE_RTT_CONGESTION_DETECTION_WITH_ICE_RTT = new VoipParamKey(VoipParamType.INTEGER, "rc.disable_rtt_congestion_detection_with_ice_rtt");
    /**
     * The {@code rc.dtx} voip-param.
     */
    public static final VoipParamKey RC_DTX = new VoipParamKey(VoipParamType.INTEGER, "rc.dtx");
    /**
     * The {@code rc.early_rtt_computation} voip-param.
     */
    public static final VoipParamKey RC_EARLY_RTT_COMPUTATION = new VoipParamKey(VoipParamType.INTEGER, "rc.early_rtt_computation");
    /**
     * The {@code rc.empty_reset_ema_smoothing} voip-param.
     */
    public static final VoipParamKey RC_EMPTY_RESET_EMA_SMOOTHING = new VoipParamKey(VoipParamType.INTEGER, "rc.empty_reset_ema_smoothing");
    /**
     * The {@code rc.empty_reset_pkts_per_frame_multiplier} voip-param.
     */
    public static final VoipParamKey RC_EMPTY_RESET_PKTS_PER_FRAME_MULTIPLIER = new VoipParamKey(VoipParamType.INTEGER, "rc.empty_reset_pkts_per_frame_multiplier");
    /**
     * The {@code rc.enable_add_to_jb_lite} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ADD_TO_JB_LITE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_add_to_jb_lite");
    /**
     * The {@code rc.enable_all_ltrp} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ALL_LTRP = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_all_ltrp");
    /**
     * The {@code rc.enable_android_decoder_fallback} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ANDROID_DECODER_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_android_decoder_fallback");
    /**
     * The {@code rc.enable_android_decoder_long_drain} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ANDROID_DECODER_LONG_DRAIN = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_android_decoder_long_drain");
    /**
     * The {@code rc.enable_async_fmt_update} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ASYNC_FMT_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_async_fmt_update");
    /**
     * The {@code rc.enable_aud_rc_param_ts_logging} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUD_RC_PARAM_TS_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_aud_rc_param_ts_logging");
    /**
     * The {@code rc.enable_audio_duplication_exploration} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUDIO_DUPLICATION_EXPLORATION = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_audio_duplication_exploration");
    /**
     * The {@code rc.enable_audio_oob_fec_feature} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUDIO_OOB_FEC_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_audio_oob_fec_feature");
    /**
     * The {@code rc.enable_audio_oob_fec_for_sender} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUDIO_OOB_FEC_FOR_SENDER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_audio_oob_fec_for_sender");
    /**
     * The {@code rc.enable_audio_piggyback_feature} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUDIO_PIGGYBACK_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_audio_piggyback_feature");
    /**
     * The {@code rc.enable_audio_pkt_piggyback_for_sender} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_AUDIO_PKT_PIGGYBACK_FOR_SENDER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_audio_pkt_piggyback_for_sender");
    /**
     * The {@code rc.enable_both_et_pt_lib_loading} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_BOTH_ET_PT_LIB_LOADING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_both_et_pt_lib_loading");
    /**
     * The {@code rc.enable_cc_bwe_slow_ramp_up_sfu_ul} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_CC_BWE_SLOW_RAMP_UP_SFU_UL = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_cc_bwe_slow_ramp_up_sfu_ul");
    /**
     * The {@code rc.enable_decoder_state_reset} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_DECODER_STATE_RESET = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_decoder_state_reset");
    /**
     * The {@code rc.enable_dl_model_load_at_bwe_create} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_DL_MODEL_LOAD_AT_BWE_CREATE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_dl_model_load_at_bwe_create");
    /**
     * The {@code rc.enable_droppable_no_fec} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_DROPPABLE_NO_FEC = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_droppable_no_fec");
    /**
     * The {@code rc.enable_droppable_no_packet_cache} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_DROPPABLE_NO_PACKET_CACHE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_droppable_no_packet_cache");
    /**
     * The {@code rc.enable_droppable_no_piggyback} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_DROPPABLE_NO_PIGGYBACK = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_droppable_no_piggyback");
    /**
     * The {@code rc.enable_executorch_lib_loading} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_EXECUTORCH_LIB_LOADING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_executorch_lib_loading");
    /**
     * The {@code rc.enable_fast_ramp_feature} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FAST_RAMP_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fast_ramp_feature");
    /**
     * The {@code rc.enable_fast_remb} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FAST_REMB = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fast_remb");
    /**
     * The {@code rc.enable_feature_length_bitmap_check} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FEATURE_LENGTH_BITMAP_CHECK = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_feature_length_bitmap_check");
    /**
     * The {@code rc.enable_fec_for_key_frames} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FEC_FOR_KEY_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fec_for_key_frames");
    /**
     * The {@code rc.enable_fec_no_piggyback} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FEC_NO_PIGGYBACK = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fec_no_piggyback");
    /**
     * The {@code rc.enable_fec_recovered_pt_from_rtp_header} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FEC_RECOVERED_PT_FROM_RTP_HEADER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fec_recovered_pt_from_rtp_header");
    /**
     * The {@code rc.enable_fr_basic} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FR_BASIC = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fr_basic");
    /**
     * The {@code rc.enable_fr_build_ext} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FR_BUILD_EXT = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fr_build_ext");
    /**
     * The {@code rc.enable_fr_bwe_clamp} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FR_BWE_CLAMP = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_fr_bwe_clamp");
    /**
     * The {@code rc.enable_frame_dropper} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FRAME_DROPPER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_frame_dropper");
    /**
     * The {@code rc.enable_frame_dropper_delta_frame_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FRAME_DROPPER_DELTA_FRAME_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_frame_dropper_delta_frame_fix");
    /**
     * The {@code rc.enable_frame_num_continuation} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_FRAME_NUM_CONTINUATION = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_frame_num_continuation");
    /**
     * The {@code rc.enable_h26x_nal_start_code_4bytes} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_H26X_NAL_START_CODE_4BYTES = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_h26x_nal_start_code_4bytes");
    /**
     * The {@code rc.enable_ios_dec_err_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_IOS_DEC_ERR_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ios_dec_err_fix");
    /**
     * The {@code rc.enable_ios_h264_validate_frame} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_IOS_H264_VALIDATE_FRAME = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ios_h264_validate_frame");
    /**
     * The {@code rc.enable_ios_sei_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_IOS_SEI_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ios_sei_fix");
    /**
     * The {@code rc.enable_jb_kf_offset_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_JB_KF_OFFSET_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_jb_kf_offset_fix");
    /**
     * The {@code rc.enable_jb_level_logging} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_JB_LEVEL_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_jb_level_logging");
    /**
     * The {@code rc.enable_kf_switch_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_KF_SWITCH_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_kf_switch_fix");
    /**
     * The {@code rc.enable_ltrp_call_replayer} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_LTRP_CALL_REPLAYER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ltrp_call_replayer");
    /**
     * The {@code rc.enable_ltrp_logs} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_LTRP_LOGS = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ltrp_logs");
    /**
     * The {@code rc.enable_ml_plc_for_audio} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_ML_PLC_FOR_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ml_plc_for_audio");
    /**
     * The {@code rc.enable_new_ltr_hdr_protocol} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_NEW_LTR_HDR_PROTOCOL = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_new_ltr_hdr_protocol");
    /**
     * The {@code rc.enable_new_vid_jb_framelist} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_NEW_VID_JB_FRAMELIST = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_new_vid_jb_framelist");
    /**
     * The {@code rc.enable_npsi_logs} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_NPSI_LOGS = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_npsi_logs");
    /**
     * The {@code rc.enable_passthrough_frame_dropper} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_PASSTHROUGH_FRAME_DROPPER = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_passthrough_frame_dropper");
    /**
     * The {@code rc.enable_pli_for_crc_mismatch} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_PLI_FOR_CRC_MISMATCH = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_pli_for_crc_mismatch");
    /**
     * The {@code rc.enable_pli_for_dec_err} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_PLI_FOR_DEC_ERR = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_pli_for_dec_err");
    /**
     * The {@code rc.enable_ptedge_lib_loading} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_PTEDGE_LIB_LOADING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_ptedge_lib_loading");
    /**
     * The {@code rc.enable_sctp_buffer_congestion_detection} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_SCTP_BUFFER_CONGESTION_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_sctp_buffer_congestion_detection");
    /**
     * The {@code rc.enable_send_frame_num_rtp_ext} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_SEND_FRAME_NUM_RTP_EXT = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_send_frame_num_rtp_ext");
    /**
     * The {@code rc.enable_send_psnr} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_SEND_PSNR = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_send_psnr");
    /**
     * The {@code rc.enable_send_vmos2_rtp_ext} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_SEND_VMOS2_RTP_EXT = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_send_vmos2_rtp_ext");
    /**
     * The {@code rc.enable_tensor_mem_reallocation} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_TENSOR_MEM_REALLOCATION = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_tensor_mem_reallocation");
    /**
     * The {@code rc.enable_udst_dyn_check} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_UDST_DYN_CHECK = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_udst_dyn_check");
    /**
     * The {@code rc.enable_verbose_aud_pkt_logs} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VERBOSE_AUD_PKT_LOGS = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_verbose_aud_pkt_logs");
    /**
     * The {@code rc.enable_verbose_pkt_logs} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VERBOSE_PKT_LOGS = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_verbose_pkt_logs");
    /**
     * The {@code rc.enable_vid_delay_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VID_DELAY_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_vid_delay_fix");
    /**
     * The {@code rc.enable_vid_frame_logging} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VID_FRAME_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_vid_frame_logging");
    /**
     * The {@code rc.enable_vid_rtx_indication} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VID_RTX_INDICATION = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_vid_rtx_indication");
    /**
     * The {@code rc.enable_video_corruption_fix} voip-param.
     */
    public static final VoipParamKey RC_ENABLE_VIDEO_CORRUPTION_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.enable_video_corruption_fix");
    /**
     * The {@code rc.enc_latency_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_ENC_LATENCY_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.enc_latency_ema_alpha");
    /**
     * The {@code rc.enc_p_adj_cpu_step} voip-param.
     */
    public static final VoipParamKey RC_ENC_P_ADJ_CPU_STEP = new VoipParamKey(VoipParamType.FLOAT, "rc.enc_p_adj_cpu_step");
    /**
     * The {@code rc.enc_p_adj_max_enc_latency} voip-param.
     */
    public static final VoipParamKey RC_ENC_P_ADJ_MAX_ENC_LATENCY = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_p_adj_max_enc_latency");
    /**
     * The {@code rc.enc_p_adj_min_frames_for_valid_stats} voip-param.
     */
    public static final VoipParamKey RC_ENC_P_ADJ_MIN_FRAMES_FOR_VALID_STATS = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_p_adj_min_frames_for_valid_stats");
    /**
     * The {@code rc.enc_p_adj_min_multiples_of_width_base} voip-param.
     */
    public static final VoipParamKey RC_ENC_P_ADJ_MIN_MULTIPLES_OF_WIDTH_BASE = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_p_adj_min_multiples_of_width_base");
    /**
     * The {@code rc.enc_p_adj_min_vpx_cpu} voip-param.
     */
    public static final VoipParamKey RC_ENC_P_ADJ_MIN_VPX_CPU = new VoipParamKey(VoipParamType.FLOAT, "rc.enc_p_adj_min_vpx_cpu");
    /**
     * The {@code rc.enc_thread_latency_ema_sample_size} voip-param.
     */
    public static final VoipParamKey RC_ENC_THREAD_LATENCY_EMA_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_thread_latency_ema_sample_size");
    /**
     * The {@code rc.enc_thread_latency_sample_interval} voip-param.
     */
    public static final VoipParamKey RC_ENC_THREAD_LATENCY_SAMPLE_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_thread_latency_sample_interval");
    /**
     * The {@code rc.enc_thread_max_latency} voip-param.
     */
    public static final VoipParamKey RC_ENC_THREAD_MAX_LATENCY = new VoipParamKey(VoipParamType.INTEGER, "rc.enc_thread_max_latency");
    /**
     * The {@code rc.encode_mutex_early_unlock} voip-param.
     */
    public static final VoipParamKey RC_ENCODE_MUTEX_EARLY_UNLOCK = new VoipParamKey(VoipParamType.INTEGER, "rc.encode_mutex_early_unlock");
    /**
     * The {@code rc.fec_frames} voip-param.
     */
    public static final VoipParamKey RC_FEC_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "rc.fec_frames");
    /**
     * The {@code rc.fec_timeout} voip-param.
     */
    public static final VoipParamKey RC_FEC_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "rc.fec_timeout");
    /**
     * The {@code rc.fpp} voip-param.
     */
    public static final VoipParamKey RC_FPP = new VoipParamKey(VoipParamType.INTEGER, "rc.fpp");
    /**
     * The {@code rc.fr_bitrate_win_msec} voip-param.
     */
    public static final VoipParamKey RC_FR_BITRATE_WIN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_bitrate_win_msec");
    /**
     * The {@code rc.fr_bits_thr_for_peer_ramp_up} voip-param.
     */
    public static final VoipParamKey RC_FR_BITS_THR_FOR_PEER_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_bits_thr_for_peer_ramp_up");
    /**
     * The {@code rc.fr_bwe_clamp_rx_bitrate_multiplier} voip-param.
     */
    public static final VoipParamKey RC_FR_BWE_CLAMP_RX_BITRATE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_bwe_clamp_rx_bitrate_multiplier");
    /**
     * The {@code rc.fr_disable_sync_event_handling} voip-param.
     */
    public static final VoipParamKey RC_FR_DISABLE_SYNC_EVENT_HANDLING = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_disable_sync_event_handling");
    /**
     * The {@code rc.fr_hbwe_init_bitrate_bps} voip-param.
     */
    public static final VoipParamKey RC_FR_HBWE_INIT_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_hbwe_init_bitrate_bps");
    /**
     * The {@code rc.fr_hbwe_target_bitrate_multiplier} voip-param.
     */
    public static final VoipParamKey RC_FR_HBWE_TARGET_BITRATE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_hbwe_target_bitrate_multiplier");
    /**
     * The {@code rc.fr_max_action_notification_count} voip-param.
     */
    public static final VoipParamKey RC_FR_MAX_ACTION_NOTIFICATION_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_max_action_notification_count");
    /**
     * The {@code rc.fr_max_target_bitrate_multiplier} voip-param.
     */
    public static final VoipParamKey RC_FR_MAX_TARGET_BITRATE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_max_target_bitrate_multiplier");
    /**
     * The {@code rc.fr_min_init_bitrate_bps} voip-param.
     */
    public static final VoipParamKey RC_FR_MIN_INIT_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_min_init_bitrate_bps");
    /**
     * The {@code rc.fr_msec_thr_coeff_of_rtt_for_peer_ramp_up} voip-param.
     */
    public static final VoipParamKey RC_FR_MSEC_THR_COEFF_OF_RTT_FOR_PEER_RAMP_UP = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_msec_thr_coeff_of_rtt_for_peer_ramp_up");
    /**
     * The {@code rc.fr_msec_thr_for_peer_ramp_up} voip-param.
     */
    public static final VoipParamKey RC_FR_MSEC_THR_FOR_PEER_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_msec_thr_for_peer_ramp_up");
    /**
     * The {@code rc.fr_no_fr_header_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_FR_NO_FR_HEADER_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_no_fr_header_threshold_ms");
    /**
     * The {@code rc.fr_pl_max_out_of_order_distance} voip-param.
     */
    public static final VoipParamKey RC_FR_PL_MAX_OUT_OF_ORDER_DISTANCE = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_pl_max_out_of_order_distance");
    /**
     * The {@code rc.fr_pl_ratio_window_size_ms} voip-param.
     */
    public static final VoipParamKey RC_FR_PL_RATIO_WINDOW_SIZE_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_pl_ratio_window_size_ms");
    /**
     * The {@code rc.fr_pl_total_thr} voip-param.
     */
    public static final VoipParamKey RC_FR_PL_TOTAL_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_pl_total_thr");
    /**
     * The {@code rc.fr_plr_perc_thr} voip-param.
     */
    public static final VoipParamKey RC_FR_PLR_PERC_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_plr_perc_thr");
    /**
     * The {@code rc.fr_rtt_ema_alpha} voip-param.
     */
    public static final VoipParamKey RC_FR_RTT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_rtt_ema_alpha");
    /**
     * The {@code rc.fr_rtt_msec_above_min_thr} voip-param.
     */
    public static final VoipParamKey RC_FR_RTT_MSEC_ABOVE_MIN_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_rtt_msec_above_min_thr");
    /**
     * The {@code rc.fr_rtt_slide_window_size} voip-param.
     */
    public static final VoipParamKey RC_FR_RTT_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_rtt_slide_window_size");
    /**
     * The {@code rc.fr_rtt_slope_pts} voip-param.
     */
    public static final VoipParamKey RC_FR_RTT_SLOPE_PTS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_rtt_slope_pts");
    /**
     * The {@code rc.fr_rtt_slope_threshold} voip-param.
     */
    public static final VoipParamKey RC_FR_RTT_SLOPE_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "rc.fr_rtt_slope_threshold");
    /**
     * The {@code rc.fr_sbwe_mode_after_stop} voip-param.
     */
    public static final VoipParamKey RC_FR_SBWE_MODE_AFTER_STOP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_sbwe_mode_after_stop");
    /**
     * The {@code rc.fr_sbwe_mode_reach_hbwe_target_bitrate} voip-param.
     */
    public static final VoipParamKey RC_FR_SBWE_MODE_REACH_HBWE_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_sbwe_mode_reach_hbwe_target_bitrate");
    /**
     * The {@code rc.fr_skip_clamp_on_out_stop} voip-param.
     */
    public static final VoipParamKey RC_FR_SKIP_CLAMP_ON_OUT_STOP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_skip_clamp_on_out_stop");
    /**
     * The {@code rc.fr_skip_ramp_up_pause} voip-param.
     */
    public static final VoipParamKey RC_FR_SKIP_RAMP_UP_PAUSE = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_skip_ramp_up_pause");
    /**
     * The {@code rc.fr_use_hbwe_target_bitrate} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_HBWE_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_hbwe_target_bitrate");
    /**
     * The {@code rc.fr_use_in_cong_signal_for_out_ramp} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_IN_CONG_SIGNAL_FOR_OUT_RAMP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_in_cong_signal_for_out_ramp");
    /**
     * The {@code rc.fr_use_in_pl_signal_for_out_ramp} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_IN_PL_SIGNAL_FOR_OUT_RAMP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_in_pl_signal_for_out_ramp");
    /**
     * The {@code rc.fr_use_init_bitrate_for_in_ramp} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_INIT_BITRATE_FOR_IN_RAMP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_init_bitrate_for_in_ramp");
    /**
     * The {@code rc.fr_use_init_bitrate_for_out_ramp} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_INIT_BITRATE_FOR_OUT_RAMP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_init_bitrate_for_out_ramp");
    /**
     * The {@code rc.fr_use_packet_loss} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_PACKET_LOSS = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_packet_loss");
    /**
     * The {@code rc.fr_use_rtt_slope_stop} voip-param.
     */
    public static final VoipParamKey RC_FR_USE_RTT_SLOPE_STOP = new VoipParamKey(VoipParamType.INTEGER, "rc.fr_use_rtt_slope_stop");
    /**
     * The {@code rc.freeze_and_cong_signal_max_diff_ms} voip-param.
     */
    public static final VoipParamKey RC_FREEZE_AND_CONG_SIGNAL_MAX_DIFF_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.freeze_and_cong_signal_max_diff_ms");
    /**
     * The {@code rc.freeze_state_avg_num_freezes_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_FREEZE_STATE_AVG_NUM_FREEZES_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.freeze_state_avg_num_freezes_interval_ms");
    /**
     * The {@code rc.freeze_state_min_msg_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_FREEZE_STATE_MIN_MSG_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.freeze_state_min_msg_interval_ms");
    /**
     * The {@code rc.freeze_state_num_freezes_per_sec_threshold} voip-param.
     */
    public static final VoipParamKey RC_FREEZE_STATE_NUM_FREEZES_PER_SEC_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "rc.freeze_state_num_freezes_per_sec_threshold");
    /**
     * The {@code rc.freeze_state_time_since_last_freeze_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_FREEZE_STATE_TIME_SINCE_LAST_FREEZE_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.freeze_state_time_since_last_freeze_threshold_ms");
    /**
     * The {@code rc.gcall_use_pli_timer} voip-param.
     */
    public static final VoipParamKey RC_GCALL_USE_PLI_TIMER = new VoipParamKey(VoipParamType.INTEGER, "rc.gcall_use_pli_timer");
    /**
     * The {@code rc.group_video_encode_height} voip-param.
     */
    public static final VoipParamKey RC_GROUP_VIDEO_ENCODE_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "rc.group_video_encode_height");
    /**
     * The {@code rc.group_video_encode_width} voip-param.
     */
    public static final VoipParamKey RC_GROUP_VIDEO_ENCODE_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "rc.group_video_encode_width");
    /**
     * The {@code rc.h264_bit_list_fix} voip-param.
     */
    public static final VoipParamKey RC_H264_BIT_LIST_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.h264_bit_list_fix");
    /**
     * The {@code rc.h264_emu_prevent_fix} voip-param.
     */
    public static final VoipParamKey RC_H264_EMU_PREVENT_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.h264_emu_prevent_fix");
    /**
     * The {@code rc.h264_ignore_ds_ref_lost} voip-param.
     */
    public static final VoipParamKey RC_H264_IGNORE_DS_REF_LOST = new VoipParamKey(VoipParamType.INTEGER, "rc.h264_ignore_ds_ref_lost");
    /**
     * The {@code rc.h264_sps_fail_on_error} voip-param.
     */
    public static final VoipParamKey RC_H264_SPS_FAIL_ON_ERROR = new VoipParamKey(VoipParamType.INTEGER, "rc.h264_sps_fail_on_error");
    /**
     * The {@code rc.hmrtt_congestion_threshold_margin_constant} voip-param.
     */
    public static final VoipParamKey RC_HMRTT_CONGESTION_THRESHOLD_MARGIN_CONSTANT = new VoipParamKey(VoipParamType.INTEGER, "rc.hmrtt_congestion_threshold_margin_constant");
    /**
     * The {@code rc.hmrtt_congestion_threshold_margin_ratio} voip-param.
     */
    public static final VoipParamKey RC_HMRTT_CONGESTION_THRESHOLD_MARGIN_RATIO = new VoipParamKey(VoipParamType.INTEGER, "rc.hmrtt_congestion_threshold_margin_ratio");
    /**
     * The {@code rc.ice_rtt_low_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_ICE_RTT_LOW_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.ice_rtt_low_threshold_ms");
    /**
     * The {@code rc.ignore_batt_rules} voip-param.
     */
    public static final VoipParamKey RC_IGNORE_BATT_RULES = new VoipParamKey(VoipParamType.INTEGER, "rc.ignore_batt_rules");
    /**
     * The {@code rc.imm_pli_after_kf_err} voip-param.
     */
    public static final VoipParamKey RC_IMM_PLI_AFTER_KF_ERR = new VoipParamKey(VoipParamType.INTEGER, "rc.imm_pli_after_kf_err");
    /**
     * The {@code rc.indeterminate_means_missing} voip-param.
     */
    public static final VoipParamKey RC_INDETERMINATE_MEANS_MISSING = new VoipParamKey(VoipParamType.INTEGER, "rc.indeterminate_means_missing");
    /**
     * The {@code rc.init_bitrate} voip-param.
     */
    public static final VoipParamKey RC_INIT_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.init_bitrate");
    /**
     * The {@code rc.initial_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey RC_INITIAL_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.initial_rtt_congestion_threshold");
    /**
     * The {@code rc.ios_enable_hw_frame_converter} voip-param.
     */
    public static final VoipParamKey RC_IOS_ENABLE_HW_FRAME_CONVERTER = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_enable_hw_frame_converter");
    /**
     * The {@code rc.ios_enable_hw_frame_converter_h265} voip-param.
     */
    public static final VoipParamKey RC_IOS_ENABLE_HW_FRAME_CONVERTER_H265 = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_enable_hw_frame_converter_h265");
    /**
     * The {@code rc.ios_enable_low_latency_rc} voip-param.
     */
    public static final VoipParamKey RC_IOS_ENABLE_LOW_LATENCY_RC = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_enable_low_latency_rc");
    /**
     * The {@code rc.ios_enable_ltrp} voip-param.
     */
    public static final VoipParamKey RC_IOS_ENABLE_LTRP = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_enable_ltrp");
    /**
     * The {@code rc.ios_enable_ts} voip-param.
     */
    public static final VoipParamKey RC_IOS_ENABLE_TS = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_enable_ts");
    /**
     * The {@code rc.ios_h264_profile_level} voip-param.
     */
    public static final VoipParamKey RC_IOS_H264_PROFILE_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_h264_profile_level");
    /**
     * The {@code rc.ios_h265_profile_level} voip-param.
     */
    public static final VoipParamKey RC_IOS_H265_PROFILE_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_h265_profile_level");
    /**
     * The {@code rc.ios_is_compression_session_realtime} voip-param.
     */
    public static final VoipParamKey RC_IOS_IS_COMPRESSION_SESSION_REALTIME = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_is_compression_session_realtime");
    /**
     * The {@code rc.ios_is_decoding_synchronous} voip-param.
     */
    public static final VoipParamKey RC_IOS_IS_DECODING_SYNCHRONOUS = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_is_decoding_synchronous");
    /**
     * The {@code rc.ios_is_encoding_synchronous} voip-param.
     */
    public static final VoipParamKey RC_IOS_IS_ENCODING_SYNCHRONOUS = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_is_encoding_synchronous");
    /**
     * The {@code rc.ios_max_allowed_frame_qp_h265} voip-param.
     */
    public static final VoipParamKey RC_IOS_MAX_ALLOWED_FRAME_QP_H265 = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_max_allowed_frame_qp_h265");
    /**
     * The {@code rc.ios_min_allowed_frame_qp_h265} voip-param.
     */
    public static final VoipParamKey RC_IOS_MIN_ALLOWED_FRAME_QP_H265 = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_min_allowed_frame_qp_h265");
    /**
     * The {@code rc.ios_on_demand_ack} voip-param.
     */
    public static final VoipParamKey RC_IOS_ON_DEMAND_ACK = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_on_demand_ack");
    /**
     * The {@code rc.ios_prioritize_encoding_quality_h265} voip-param.
     */
    public static final VoipParamKey RC_IOS_PRIORITIZE_ENCODING_QUALITY_H265 = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_prioritize_encoding_quality_h265");
    /**
     * The {@code rc.ios_render_i420} voip-param.
     */
    public static final VoipParamKey RC_IOS_RENDER_I420 = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_render_i420");
    /**
     * The {@code rc.ios_vt_codec_rpsi_to_pli} voip-param.
     */
    public static final VoipParamKey RC_IOS_VT_CODEC_RPSI_TO_PLI = new VoipParamKey(VoipParamType.INTEGER, "rc.ios_vt_codec_rpsi_to_pli");
    /**
     * The {@code rc.jb_max_covered_length_floor_ms} voip-param.
     */
    public static final VoipParamKey RC_JB_MAX_COVERED_LENGTH_FLOOR_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.jb_max_covered_length_floor_ms");
    /**
     * The {@code rc.jb_max_covered_length_ms} voip-param.
     */
    public static final VoipParamKey RC_JB_MAX_COVERED_LENGTH_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.jb_max_covered_length_ms");
    /**
     * The {@code rc.jb_max_covered_rtt_based} voip-param.
     */
    public static final VoipParamKey RC_JB_MAX_COVERED_RTT_BASED = new VoipParamKey(VoipParamType.INTEGER, "rc.jb_max_covered_rtt_based");
    /**
     * The {@code rc.jb_max_covered_rtt_multiplier} voip-param.
     */
    public static final VoipParamKey RC_JB_MAX_COVERED_RTT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.jb_max_covered_rtt_multiplier");
    /**
     * The {@code rc.last_udst_rd_factor} voip-param.
     */
    public static final VoipParamKey RC_LAST_UDST_RD_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "rc.last_udst_rd_factor");
    /**
     * The {@code rc.low_data_usage_bitrate} voip-param.
     */
    public static final VoipParamKey RC_LOW_DATA_USAGE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.low_data_usage_bitrate");
    /**
     * The {@code rc.ltr_pool_ring_buf_max_frames} voip-param.
     */
    public static final VoipParamKey RC_LTR_POOL_RING_BUF_MAX_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "rc.ltr_pool_ring_buf_max_frames");
    /**
     * The {@code rc.ltrp_kf_correction_factor_wt} voip-param.
     */
    public static final VoipParamKey RC_LTRP_KF_CORRECTION_FACTOR_WT = new VoipParamKey(VoipParamType.FLOAT, "rc.ltrp_kf_correction_factor_wt");
    /**
     * The {@code rc.ltrp_kf_frame_size_wt} voip-param.
     */
    public static final VoipParamKey RC_LTRP_KF_FRAME_SIZE_WT = new VoipParamKey(VoipParamType.FLOAT, "rc.ltrp_kf_frame_size_wt");
    /**
     * The {@code rc.ltrp_nack_pkt_cnt_wt} voip-param.
     */
    public static final VoipParamKey RC_LTRP_NACK_PKT_CNT_WT = new VoipParamKey(VoipParamType.FLOAT, "rc.ltrp_nack_pkt_cnt_wt");
    /**
     * The {@code rc.ltrp_qp_offset} voip-param.
     */
    public static final VoipParamKey RC_LTRP_QP_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "rc.ltrp_qp_offset");
    /**
     * The {@code rc.max_audio_frame_disorder_distance_rc} voip-param.
     */
    public static final VoipParamKey RC_MAX_AUDIO_FRAME_DISORDER_DISTANCE_RC = new VoipParamKey(VoipParamType.INTEGER, "rc.max_audio_frame_disorder_distance_rc");
    /**
     * The {@code rc.max_decrease_factor_on_congestion} voip-param.
     */
    public static final VoipParamKey RC_MAX_DECREASE_FACTOR_ON_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "rc.max_decrease_factor_on_congestion");
    /**
     * The {@code rc.max_nacks_throttled} voip-param.
     */
    public static final VoipParamKey RC_MAX_NACKS_THROTTLED = new VoipParamKey(VoipParamType.INTEGER, "rc.max_nacks_throttled");
    /**
     * The {@code rc.max_npsi_timer_ms} voip-param.
     */
    public static final VoipParamKey RC_MAX_NPSI_TIMER_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.max_npsi_timer_ms");
    /**
     * The {@code rc.max_npsi_timer_ms_newpkt} voip-param.
     */
    public static final VoipParamKey RC_MAX_NPSI_TIMER_MS_NEWPKT = new VoipParamKey(VoipParamType.INTEGER, "rc.max_npsi_timer_ms_newpkt");
    /**
     * The {@code rc.max_plr_to_opus} voip-param.
     */
    public static final VoipParamKey RC_MAX_PLR_TO_OPUS = new VoipParamKey(VoipParamType.INTEGER, "rc.max_plr_to_opus");
    /**
     * The {@code rc.maxbwe} voip-param.
     */
    public static final VoipParamKey RC_MAXBWE = new VoipParamKey(VoipParamType.INTEGER, "rc.maxbwe");
    /**
     * The {@code rc.maxfpp} voip-param.
     */
    public static final VoipParamKey RC_MAXFPP = new VoipParamKey(VoipParamType.INTEGER, "rc.maxfpp");
    /**
     * The {@code rc.maxfpp_duration} voip-param.
     */
    public static final VoipParamKey RC_MAXFPP_DURATION = new VoipParamKey(VoipParamType.INTEGER, "rc.maxfpp_duration");
    /**
     * The {@code rc.maxrtt} voip-param.
     */
    public static final VoipParamKey RC_MAXRTT = new VoipParamKey(VoipParamType.INTEGER, "rc.maxrtt");
    /**
     * The {@code rc.min_decrease_factor_on_congestion} voip-param.
     */
    public static final VoipParamKey RC_MIN_DECREASE_FACTOR_ON_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "rc.min_decrease_factor_on_congestion");
    /**
     * The {@code rc.min_elastic_disorder_buf_size_in_frames_rc} voip-param.
     */
    public static final VoipParamKey RC_MIN_ELASTIC_DISORDER_BUF_SIZE_IN_FRAMES_RC = new VoipParamKey(VoipParamType.INTEGER, "rc.min_elastic_disorder_buf_size_in_frames_rc");
    /**
     * The {@code rc.min_elastic_disorder_buf_size_ratio_rc} voip-param.
     */
    public static final VoipParamKey RC_MIN_ELASTIC_DISORDER_BUF_SIZE_RATIO_RC = new VoipParamKey(VoipParamType.FLOAT, "rc.min_elastic_disorder_buf_size_ratio_rc");
    /**
     * The {@code rc.min_interval_rr} voip-param.
     */
    public static final VoipParamKey RC_MIN_INTERVAL_RR = new VoipParamKey(VoipParamType.INTEGER, "rc.min_interval_rr");
    /**
     * The {@code rc.min_interval_sr} voip-param.
     */
    public static final VoipParamKey RC_MIN_INTERVAL_SR = new VoipParamKey(VoipParamType.INTEGER, "rc.min_interval_sr");
    /**
     * The {@code rc.min_nack_resend_period_e2e_ms} voip-param.
     */
    public static final VoipParamKey RC_MIN_NACK_RESEND_PERIOD_E2E_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.min_nack_resend_period_e2e_ms");
    /**
     * The {@code rc.min_nack_resend_period_ms} voip-param.
     */
    public static final VoipParamKey RC_MIN_NACK_RESEND_PERIOD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.min_nack_resend_period_ms");
    /**
     * The {@code rc.min_npsi_timer_ms} voip-param.
     */
    public static final VoipParamKey RC_MIN_NPSI_TIMER_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.min_npsi_timer_ms");
    /**
     * The {@code rc.min_sender_estimate_on_drop} voip-param.
     */
    public static final VoipParamKey RC_MIN_SENDER_ESTIMATE_ON_DROP = new VoipParamKey(VoipParamType.INTEGER, "rc.min_sender_estimate_on_drop");
    /**
     * The {@code rc.minfpp} voip-param.
     */
    public static final VoipParamKey RC_MINFPP = new VoipParamKey(VoipParamType.INTEGER, "rc.minfpp");
    /**
     * The {@code rc.missing_start_frame_nack_estimate} voip-param.
     */
    public static final VoipParamKey RC_MISSING_START_FRAME_NACK_ESTIMATE = new VoipParamKey(VoipParamType.INTEGER, "rc.missing_start_frame_nack_estimate");
    /**
     * The {@code rc.ml_udst_max_pp_kbps} voip-param.
     */
    public static final VoipParamKey RC_ML_UDST_MAX_PP_KBPS = new VoipParamKey(VoipParamType.INTEGER, "rc.ml_udst_max_pp_kbps");
    /**
     * The {@code rc.ml_udst2_max_pp_kbps} voip-param.
     */
    public static final VoipParamKey RC_ML_UDST2_MAX_PP_KBPS = new VoipParamKey(VoipParamType.INTEGER, "rc.ml_udst2_max_pp_kbps");
    /**
     * The {@code rc.mlow_inband_fec_fixed_bitrate} voip-param.
     */
    public static final VoipParamKey RC_MLOW_INBAND_FEC_FIXED_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_inband_fec_fixed_bitrate");
    /**
     * The {@code rc.mlow_red_redundancy_level} voip-param.
     */
    public static final VoipParamKey RC_MLOW_RED_REDUNDANCY_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_red_redundancy_level");
    /**
     * The {@code rc.mlow_red_secondary_bitrate} voip-param.
     */
    public static final VoipParamKey RC_MLOW_RED_SECONDARY_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_red_secondary_bitrate");
    /**
     * The {@code rc.mlow_red_secondary_complexity} voip-param.
     */
    public static final VoipParamKey RC_MLOW_RED_SECONDARY_COMPLEXITY = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_red_secondary_complexity");
    /**
     * The {@code rc.mlow_sf_imp_factor} voip-param.
     */
    public static final VoipParamKey RC_MLOW_SF_IMP_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_sf_imp_factor");
    /**
     * The {@code rc.mlow_use_sp_act_flat} voip-param.
     */
    public static final VoipParamKey RC_MLOW_USE_SP_ACT_FLAT = new VoipParamKey(VoipParamType.INTEGER, "rc.mlow_use_sp_act_flat");
    /**
     * The {@code rc.monochrome_mode_ratio} voip-param.
     */
    public static final VoipParamKey RC_MONOCHROME_MODE_RATIO = new VoipParamKey(VoipParamType.INTEGER, "rc.monochrome_mode_ratio");
    /**
     * The {@code rc.motion_analysis_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_MOTION_ANALYSIS_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.motion_analysis_interval_ms");
    /**
     * The {@code rc.nack_default_resend_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_NACK_DEFAULT_RESEND_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_default_resend_threshold_ms");
    /**
     * The {@code rc.nack_enabled} voip-param.
     */
    public static final VoipParamKey RC_NACK_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_enabled");
    /**
     * The {@code rc.nack_if_pli_throttled} voip-param.
     */
    public static final VoipParamKey RC_NACK_IF_PLI_THROTTLED = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_if_pli_throttled");
    /**
     * The {@code rc.nack_if_rpsi_throttled} voip-param.
     */
    public static final VoipParamKey RC_NACK_IF_RPSI_THROTTLED = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_if_rpsi_throttled");
    /**
     * The {@code rc.nack_pli_threshold_min} voip-param.
     */
    public static final VoipParamKey RC_NACK_PLI_THRESHOLD_MIN = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_pli_threshold_min");
    /**
     * The {@code rc.nack_rtt_interactive_threshold} voip-param.
     */
    public static final VoipParamKey RC_NACK_RTT_INTERACTIVE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_rtt_interactive_threshold");
    /**
     * The {@code rc.nack_rtt_modifier_high} voip-param.
     */
    public static final VoipParamKey RC_NACK_RTT_MODIFIER_HIGH = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_rtt_modifier_high");
    /**
     * The {@code rc.nack_rtt_modifier_low} voip-param.
     */
    public static final VoipParamKey RC_NACK_RTT_MODIFIER_LOW = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_rtt_modifier_low");
    /**
     * The {@code rc.nack_rtx_pkt_seq_threshold} voip-param.
     */
    public static final VoipParamKey RC_NACK_RTX_PKT_SEQ_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.nack_rtx_pkt_seq_threshold");
    /**
     * The {@code rc.nack_rtx_pkt_ts_threshold_multiplier} voip-param.
     */
    public static final VoipParamKey RC_NACK_RTX_PKT_TS_THRESHOLD_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.nack_rtx_pkt_ts_threshold_multiplier");
    /**
     * The {@code rc.neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey RC_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.neteq_delay_offset_ms");
    /**
     * The {@code rc.no_decodable_handling} voip-param.
     */
    public static final VoipParamKey RC_NO_DECODABLE_HANDLING = new VoipParamKey(VoipParamType.INTEGER, "rc.no_decodable_handling");
    /**
     * The {@code rc.no_initial_rtt_threshold} voip-param.
     */
    public static final VoipParamKey RC_NO_INITIAL_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.no_initial_rtt_threshold");
    /**
     * The {@code rc.no_sr_rr_piggyback} voip-param.
     */
    public static final VoipParamKey RC_NO_SR_RR_PIGGYBACK = new VoipParamKey(VoipParamType.INTEGER, "rc.no_sr_rr_piggyback");
    /**
     * The {@code rc.npsi_enabled} voip-param.
     */
    public static final VoipParamKey RC_NPSI_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.npsi_enabled");
    /**
     * The {@code rc.npsi_old_timer_ms} voip-param.
     */
    public static final VoipParamKey RC_NPSI_OLD_TIMER_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.npsi_old_timer_ms");
    /**
     * The {@code rc.npsi_rstltrpsize} voip-param.
     */
    public static final VoipParamKey RC_NPSI_RSTLTRPSIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.npsi_rstltrpsize");
    /**
     * The {@code rc.npsi_rtt_mult} voip-param.
     */
    public static final VoipParamKey RC_NPSI_RTT_MULT = new VoipParamKey(VoipParamType.FLOAT, "rc.npsi_rtt_mult");
    /**
     * The {@code rc.npsi_rtt_mult_sender} voip-param.
     */
    public static final VoipParamKey RC_NPSI_RTT_MULT_SENDER = new VoipParamKey(VoipParamType.FLOAT, "rc.npsi_rtt_mult_sender");
    /**
     * The {@code rc.npsi_use_ema_rtt} voip-param.
     */
    public static final VoipParamKey RC_NPSI_USE_EMA_RTT = new VoipParamKey(VoipParamType.INTEGER, "rc.npsi_use_ema_rtt");
    /**
     * The {@code rc.opus_max_bandwidth} voip-param.
     */
    public static final VoipParamKey RC_OPUS_MAX_BANDWIDTH = new VoipParamKey(VoipParamType.INTEGER, "rc.opus_max_bandwidth");
    /**
     * The {@code rc.opus_non_speech_bitrate} voip-param.
     */
    public static final VoipParamKey RC_OPUS_NON_SPEECH_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.opus_non_speech_bitrate");
    /**
     * The {@code rc.opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey RC_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.opus_vad_threshold");
    /**
     * The {@code rc.overshoot_rate_downgrade_threshold_pct} voip-param.
     */
    public static final VoipParamKey RC_OVERSHOOT_RATE_DOWNGRADE_THRESHOLD_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.overshoot_rate_downgrade_threshold_pct");
    /**
     * The {@code rc.overshoot_rate_ema_sample_size} voip-param.
     */
    public static final VoipParamKey RC_OVERSHOOT_RATE_EMA_SAMPLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.overshoot_rate_ema_sample_size");
    /**
     * The {@code rc.overshoot_rate_sample_interval} voip-param.
     */
    public static final VoipParamKey RC_OVERSHOOT_RATE_SAMPLE_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "rc.overshoot_rate_sample_interval");
    /**
     * The {@code rc.ping_ice_rtt_diff_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_PING_ICE_RTT_DIFF_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.ping_ice_rtt_diff_threshold_ms");
    /**
     * The {@code rc.pixel_step_for_edge_analysis} voip-param.
     */
    public static final VoipParamKey RC_PIXEL_STEP_FOR_EDGE_ANALYSIS = new VoipParamKey(VoipParamType.INTEGER, "rc.pixel_step_for_edge_analysis");
    /**
     * The {@code rc.pkt_size_thresh_bitrate} voip-param.
     */
    public static final VoipParamKey RC_PKT_SIZE_THRESH_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.pkt_size_thresh_bitrate");
    /**
     * The {@code rc.pli_enabled} voip-param.
     */
    public static final VoipParamKey RC_PLI_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_enabled");
    /**
     * The {@code rc.pli_freeze_timeout} voip-param.
     */
    public static final VoipParamKey RC_PLI_FREEZE_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_freeze_timeout");
    /**
     * The {@code rc.pli_key_frame_pct} voip-param.
     */
    public static final VoipParamKey RC_PLI_KEY_FRAME_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_key_frame_pct");
    /**
     * The {@code rc.pli_max_threshold} voip-param.
     */
    public static final VoipParamKey RC_PLI_MAX_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_max_threshold");
    /**
     * The {@code rc.pli_resp_throttle_time} voip-param.
     */
    public static final VoipParamKey RC_PLI_RESP_THROTTLE_TIME = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_resp_throttle_time");
    /**
     * The {@code rc.pli_rtt_multiplier} voip-param.
     */
    public static final VoipParamKey RC_PLI_RTT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.pli_rtt_multiplier");
    /**
     * The {@code rc.pli_rx_bwe_check_fix} voip-param.
     */
    public static final VoipParamKey RC_PLI_RX_BWE_CHECK_FIX = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_rx_bwe_check_fix");
    /**
     * The {@code rc.pli_throttle_time_ms} voip-param.
     */
    public static final VoipParamKey RC_PLI_THROTTLE_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.pli_throttle_time_ms");
    /**
     * The {@code rc.publish_dyn_params_to_dec} voip-param.
     */
    public static final VoipParamKey RC_PUBLISH_DYN_PARAMS_TO_DEC = new VoipParamKey(VoipParamType.INTEGER, "rc.publish_dyn_params_to_dec");
    /**
     * The {@code rc.report_client_nadl_fs} voip-param.
     */
    public static final VoipParamKey RC_REPORT_CLIENT_NADL_FS = new VoipParamKey(VoipParamType.INTEGER, "rc.report_client_nadl_fs");
    /**
     * The {@code rc.report_rc_fs} voip-param.
     */
    public static final VoipParamKey RC_REPORT_RC_FS = new VoipParamKey(VoipParamType.INTEGER, "rc.report_rc_fs");
    /**
     * The {@code rc.report_vrc_fs} voip-param.
     */
    public static final VoipParamKey RC_REPORT_VRC_FS = new VoipParamKey(VoipParamType.INTEGER, "rc.report_vrc_fs");
    /**
     * The {@code rc.restrict_fmt_change_event} voip-param.
     */
    public static final VoipParamKey RC_RESTRICT_FMT_CHANGE_EVENT = new VoipParamKey(VoipParamType.INTEGER, "rc.restrict_fmt_change_event");
    /**
     * The {@code rc.rtcp_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_RTCP_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.rtcp_interval_ms");
    /**
     * The {@code rc.rtcp_retx_time_ms} voip-param.
     */
    public static final VoipParamKey RC_RTCP_RETX_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.rtcp_retx_time_ms");
    /**
     * The {@code rc.rtcp_retxr_enabled_bitmask} voip-param.
     */
    public static final VoipParamKey RC_RTCP_RETXR_ENABLED_BITMASK = new VoipParamKey(VoipParamType.INTEGER, "rc.rtcp_retxr_enabled_bitmask");
    /**
     * The {@code rc.rtcp_retxr_num_retx} voip-param.
     */
    public static final VoipParamKey RC_RTCP_RETXR_NUM_RETX = new VoipParamKey(VoipParamType.INTEGER, "rc.rtcp_retxr_num_retx");
    /**
     * The {@code rc.rtcp_rtt_high_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_RTCP_RTT_HIGH_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.rtcp_rtt_high_threshold_ms");
    /**
     * The {@code rc.rtcp_to_ice_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey RC_RTCP_TO_ICE_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "rc.rtcp_to_ice_rtt_ratio_threshold");
    /**
     * The {@code rc.rtt_based_pli_timer} voip-param.
     */
    public static final VoipParamKey RC_RTT_BASED_PLI_TIMER = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_based_pli_timer");
    /**
     * The {@code rc.rtt_congestion_consecutive_increase_count} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_CONSECUTIVE_INCREASE_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_congestion_consecutive_increase_count");
    /**
     * The {@code rc.rtt_congestion_slope_threshold} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_SLOPE_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "rc.rtt_congestion_slope_threshold");
    /**
     * The {@code rc.rtt_congestion_slope_window_size} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_SLOPE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_congestion_slope_window_size");
    /**
     * The {@code rc.rtt_congestion_step} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_STEP = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_congestion_step");
    /**
     * The {@code rc.rtt_congestion_step_ema} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_STEP_EMA = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_congestion_step_ema");
    /**
     * The {@code rc.rtt_congestion_step_previous} voip-param.
     */
    public static final VoipParamKey RC_RTT_CONGESTION_STEP_PREVIOUS = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_congestion_step_previous");
    /**
     * The {@code rc.rtt_slide_window_size} voip-param.
     */
    public static final VoipParamKey RC_RTT_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "rc.rtt_slide_window_size");
    /**
     * The {@code rc.rx_throttled_kbps} voip-param.
     */
    public static final VoipParamKey RC_RX_THROTTLED_KBPS = new VoipParamKey(VoipParamType.INTEGER, "rc.rx_throttled_kbps");
    /**
     * The {@code rc.sbwe_nonzero_rtt_count_thr} voip-param.
     */
    public static final VoipParamKey RC_SBWE_NONZERO_RTT_COUNT_THR = new VoipParamKey(VoipParamType.INTEGER, "rc.sbwe_nonzero_rtt_count_thr");
    /**
     * The {@code rc.sbwe_ramp_down_target_lower_multiplier} voip-param.
     */
    public static final VoipParamKey RC_SBWE_RAMP_DOWN_TARGET_LOWER_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "rc.sbwe_ramp_down_target_lower_multiplier");
    /**
     * The {@code rc.sbwe_ramp_down_target_lower_multiplier_pl} voip-param.
     */
    public static final VoipParamKey RC_SBWE_RAMP_DOWN_TARGET_LOWER_MULTIPLIER_PL = new VoipParamKey(VoipParamType.FLOAT, "rc.sbwe_ramp_down_target_lower_multiplier_pl");
    /**
     * The {@code rc.sbwe_ramp_down_target_lower_recent_thr_msec} voip-param.
     */
    public static final VoipParamKey RC_SBWE_RAMP_DOWN_TARGET_LOWER_RECENT_THR_MSEC = new VoipParamKey(VoipParamType.INTEGER, "rc.sbwe_ramp_down_target_lower_recent_thr_msec");
    /**
     * The {@code rc.sctp_buffer_clear_rtcp_fresh_threshold_ms} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_CLEAR_RTCP_FRESH_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_clear_rtcp_fresh_threshold_ms");
    /**
     * The {@code rc.sctp_buffer_congestion_decrease_factor} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_CONGESTION_DECREASE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_congestion_decrease_factor");
    /**
     * The {@code rc.sctp_buffer_congestion_persistence_count} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_CONGESTION_PERSISTENCE_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_congestion_persistence_count");
    /**
     * The {@code rc.sctp_buffer_high_threshold_bytes} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_HIGH_THRESHOLD_BYTES = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_high_threshold_bytes");
    /**
     * The {@code rc.sctp_buffer_low_threshold_bytes} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_LOW_THRESHOLD_BYTES = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_low_threshold_bytes");
    /**
     * The {@code rc.sctp_buffer_threshold_factor_ms} voip-param.
     */
    public static final VoipParamKey RC_SCTP_BUFFER_THRESHOLD_FACTOR_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.sctp_buffer_threshold_factor_ms");
    /**
     * The {@code rc.send_audio_level} voip-param.
     */
    public static final VoipParamKey RC_SEND_AUDIO_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "rc.send_audio_level");
    /**
     * The {@code rc.send_ltrp_fs} voip-param.
     */
    public static final VoipParamKey RC_SEND_LTRP_FS = new VoipParamKey(VoipParamType.INTEGER, "rc.send_ltrp_fs");
    /**
     * The {@code rc.send_piggyback_ext_when_enabled} voip-param.
     */
    public static final VoipParamKey RC_SEND_PIGGYBACK_EXT_WHEN_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.send_piggyback_ext_when_enabled");
    /**
     * The {@code rc.sender_side_rc_policy} voip-param.
     */
    public static final VoipParamKey RC_SENDER_SIDE_RC_POLICY = new VoipParamKey(VoipParamType.INTEGER, "rc.sender_side_rc_policy");
    /**
     * The {@code rc.set_ltrp_target_bitrate} voip-param.
     */
    public static final VoipParamKey RC_SET_LTRP_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.set_ltrp_target_bitrate");
    /**
     * The {@code rc.skip_nack_if_ltrp_sent} voip-param.
     */
    public static final VoipParamKey RC_SKIP_NACK_IF_LTRP_SENT = new VoipParamKey(VoipParamType.INTEGER, "rc.skip_nack_if_ltrp_sent");
    /**
     * The {@code rc.skip_rtt_min_cmp} voip-param.
     */
    public static final VoipParamKey RC_SKIP_RTT_MIN_CMP = new VoipParamKey(VoipParamType.INTEGER, "rc.skip_rtt_min_cmp");
    /**
     * The {@code rc.spatial_analysis_interval_ms} voip-param.
     */
    public static final VoipParamKey RC_SPATIAL_ANALYSIS_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.spatial_analysis_interval_ms");
    /**
     * The {@code rc.start_all_ltrp} voip-param.
     */
    public static final VoipParamKey RC_START_ALL_LTRP = new VoipParamKey(VoipParamType.INTEGER, "rc.start_all_ltrp");
    /**
     * The {@code rc.target_bitrate} voip-param.
     */
    public static final VoipParamKey RC_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "rc.target_bitrate");
    /**
     * The {@code rc.udst_br_valid_time_ms} voip-param.
     */
    public static final VoipParamKey RC_UDST_BR_VALID_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.udst_br_valid_time_ms");
    /**
     * The {@code rc.use_default_frame_context_for_ltrp} voip-param.
     */
    public static final VoipParamKey RC_USE_DEFAULT_FRAME_CONTEXT_FOR_LTRP = new VoipParamKey(VoipParamType.INTEGER, "rc.use_default_frame_context_for_ltrp");
    /**
     * The {@code rc.use_nack_rtt_for_pli_threshold} voip-param.
     */
    public static final VoipParamKey RC_USE_NACK_RTT_FOR_PLI_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.use_nack_rtt_for_pli_threshold");
    /**
     * The {@code rc.use_plr_ema} voip-param.
     */
    public static final VoipParamKey RC_USE_PLR_EMA = new VoipParamKey(VoipParamType.INTEGER, "rc.use_plr_ema");
    /**
     * The {@code rc.use_sbwe_ramp_down_target_lower} voip-param.
     */
    public static final VoipParamKey RC_USE_SBWE_RAMP_DOWN_TARGET_LOWER = new VoipParamKey(VoipParamType.INTEGER, "rc.use_sbwe_ramp_down_target_lower");
    /**
     * The {@code rc.use_udst_based_md_in_rd} voip-param.
     */
    public static final VoipParamKey RC_USE_UDST_BASED_MD_IN_RD = new VoipParamKey(VoipParamType.INTEGER, "rc.use_udst_based_md_in_rd");
    /**
     * The {@code rc.verify_got_frame} voip-param.
     */
    public static final VoipParamKey RC_VERIFY_GOT_FRAME = new VoipParamKey(VoipParamType.INTEGER, "rc.verify_got_frame");
    /**
     * The {@code rc.verify_sbwe_ramp_down_target_lower} voip-param.
     */
    public static final VoipParamKey RC_VERIFY_SBWE_RAMP_DOWN_TARGET_LOWER = new VoipParamKey(VoipParamType.INTEGER, "rc.verify_sbwe_ramp_down_target_lower");
    /**
     * The {@code rc.vid_jb_grace_hold_ms} voip-param.
     */
    public static final VoipParamKey RC_VID_JB_GRACE_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "rc.vid_jb_grace_hold_ms");
    /**
     * The {@code rc.vid_jb_max_frames} voip-param.
     */
    public static final VoipParamKey RC_VID_JB_MAX_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "rc.vid_jb_max_frames");
    /**
     * The {@code rc.vid_max_br_pct} voip-param.
     */
    public static final VoipParamKey RC_VID_MAX_BR_PCT = new VoipParamKey(VoipParamType.INTEGER, "rc.vid_max_br_pct");
    /**
     * The {@code rc.vid_strm_max_packet_count} voip-param.
     */
    public static final VoipParamKey RC_VID_STRM_MAX_PACKET_COUNT = new VoipParamKey(VoipParamType.INTEGER, "rc.vid_strm_max_packet_count");
    /**
     * The {@code rc.vid_strm_ts_nondroppable_pkt_cnt_threshold} voip-param.
     */
    public static final VoipParamKey RC_VID_STRM_TS_NONDROPPABLE_PKT_CNT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "rc.vid_strm_ts_nondroppable_pkt_cnt_threshold");
    /**
     * The {@code rc.video_frame_crc_sample_interval} voip-param.
     */
    public static final VoipParamKey RC_VIDEO_FRAME_CRC_SAMPLE_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "rc.video_frame_crc_sample_interval");
    /**
     * The {@code rc.vmos2_rtp_psnr_send_interval} voip-param.
     */
    public static final VoipParamKey RC_VMOS2_RTP_PSNR_SEND_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "rc.vmos2_rtp_psnr_send_interval");
    /**
     * The {@code rc.webrtc_nack_check_pli_enabled} voip-param.
     */
    public static final VoipParamKey RC_WEBRTC_NACK_CHECK_PLI_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "rc.webrtc_nack_check_pli_enabled");
    /**
     * The {@code re.additive_forced_probing_factor} voip-param.
     */
    public static final VoipParamKey RE_ADDITIVE_FORCED_PROBING_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "re.additive_forced_probing_factor");
    /**
     * The {@code re.additive_sender_bwe_inc_ema_weight} voip-param.
     */
    public static final VoipParamKey RE_ADDITIVE_SENDER_BWE_INC_EMA_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "re.additive_sender_bwe_inc_ema_weight");
    /**
     * The {@code re.additive_sender_bwe_inc_near_max} voip-param.
     */
    public static final VoipParamKey RE_ADDITIVE_SENDER_BWE_INC_NEAR_MAX = new VoipParamKey(VoipParamType.INTEGER, "re.additive_sender_bwe_inc_near_max");
    /**
     * The {@code re.additive_sender_bwe_inc_skip_first_n} voip-param.
     */
    public static final VoipParamKey RE_ADDITIVE_SENDER_BWE_INC_SKIP_FIRST_N = new VoipParamKey(VoipParamType.INTEGER, "re.additive_sender_bwe_inc_skip_first_n");
    /**
     * The {@code re.additive_sender_bwe_inc_use_ceiling_from_cc} voip-param.
     */
    public static final VoipParamKey RE_ADDITIVE_SENDER_BWE_INC_USE_CEILING_FROM_CC = new VoipParamKey(VoipParamType.INTEGER, "re.additive_sender_bwe_inc_use_ceiling_from_cc");
    /**
     * The {@code re.allow_exit_forced_probing_early} voip-param.
     */
    public static final VoipParamKey RE_ALLOW_EXIT_FORCED_PROBING_EARLY = new VoipParamKey(VoipParamType.INTEGER, "re.allow_exit_forced_probing_early");
    /**
     * The {@code re.aud_issue_hold_timer} voip-param.
     */
    public static final VoipParamKey RE_AUD_ISSUE_HOLD_TIMER = new VoipParamKey(VoipParamType.INTEGER, "re.aud_issue_hold_timer");
    /**
     * The {@code re.aud_issue_mask} voip-param.
     */
    public static final VoipParamKey RE_AUD_ISSUE_MASK = new VoipParamKey(VoipParamType.INTEGER, "re.aud_issue_mask");
    /**
     * The {@code re.aud_issue_plr_ema_size} voip-param.
     */
    public static final VoipParamKey RE_AUD_ISSUE_PLR_EMA_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.aud_issue_plr_ema_size");
    /**
     * The {@code re.aud_issue_plr_thresh} voip-param.
     */
    public static final VoipParamKey RE_AUD_ISSUE_PLR_THRESH = new VoipParamKey(VoipParamType.INTEGER, "re.aud_issue_plr_thresh");
    /**
     * The {@code re.aud_ul_bps_ave_time_ms} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_BPS_AVE_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_bps_ave_time_ms");
    /**
     * The {@code re.aud_ul_bps_ema_size} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_BPS_EMA_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_bps_ema_size");
    /**
     * The {@code re.aud_ul_bps_ignore_time_after_restart_ms} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_BPS_IGNORE_TIME_AFTER_RESTART_MS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_bps_ignore_time_after_restart_ms");
    /**
     * The {@code re.aud_ul_enable_br_drop_fix} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_ENABLE_BR_DROP_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_enable_br_drop_fix");
    /**
     * The {@code re.aud_ul_enable_lqm_calc_logs} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_ENABLE_LQM_CALC_LOGS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_enable_lqm_calc_logs");
    /**
     * The {@code re.aud_ul_good_lqm_max_plr_ema} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_GOOD_LQM_MAX_PLR_EMA = new VoipParamKey(VoipParamType.FLOAT, "re.aud_ul_good_lqm_max_plr_ema");
    /**
     * The {@code re.aud_ul_good_lqm_max_rtt_ema_ms} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_GOOD_LQM_MAX_RTT_EMA_MS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_good_lqm_max_rtt_ema_ms");
    /**
     * The {@code re.aud_ul_good_lqm_min_ave_bps} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_GOOD_LQM_MIN_AVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_good_lqm_min_ave_bps");
    /**
     * The {@code re.aud_ul_good_lqm_min_bps} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_GOOD_LQM_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_good_lqm_min_bps");
    /**
     * The {@code re.aud_ul_lqm_sample_history_size} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_LQM_SAMPLE_HISTORY_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_lqm_sample_history_size");
    /**
     * The {@code re.aud_ul_min_lqm_samples} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_MIN_LQM_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_min_lqm_samples");
    /**
     * The {@code re.aud_ul_poor_lqm_max_plr_ema} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_POOR_LQM_MAX_PLR_EMA = new VoipParamKey(VoipParamType.FLOAT, "re.aud_ul_poor_lqm_max_plr_ema");
    /**
     * The {@code re.aud_ul_poor_lqm_max_rtt_ema_ms} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_POOR_LQM_MAX_RTT_EMA_MS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_poor_lqm_max_rtt_ema_ms");
    /**
     * The {@code re.aud_ul_poor_lqm_min_ave_bps} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_POOR_LQM_MIN_AVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_poor_lqm_min_ave_bps");
    /**
     * The {@code re.aud_ul_poor_lqm_min_bps} voip-param.
     */
    public static final VoipParamKey RE_AUD_UL_POOR_LQM_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.aud_ul_poor_lqm_min_bps");
    /**
     * The {@code re.bitrate_probing_is_paused} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_IS_PAUSED = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_is_paused");
    /**
     * The {@code re.bitrate_probing_max_probed_bitrate} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MAX_PROBED_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_max_probed_bitrate");
    /**
     * The {@code re.bitrate_probing_max_wait_ms_for_initial_probing} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MAX_WAIT_MS_FOR_INITIAL_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_max_wait_ms_for_initial_probing");
    /**
     * The {@code re.bitrate_probing_min_bitrate_for_initial_probing} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MIN_BITRATE_FOR_INITIAL_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_min_bitrate_for_initial_probing");
    /**
     * The {@code re.bitrate_probing_min_pkts_for_cluster} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MIN_PKTS_FOR_CLUSTER = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_min_pkts_for_cluster");
    /**
     * The {@code re.bitrate_probing_min_quiet_period} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MIN_QUIET_PERIOD = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_min_quiet_period");
    /**
     * The {@code re.bitrate_probing_min_wait_ms_for_initial_probing} voip-param.
     */
    public static final VoipParamKey RE_BITRATE_PROBING_MIN_WAIT_MS_FOR_INITIAL_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.bitrate_probing_min_wait_ms_for_initial_probing");
    /**
     * The {@code re.br_adj_factor} voip-param.
     */
    public static final VoipParamKey RE_BR_ADJ_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "re.br_adj_factor");
    /**
     * The {@code re.cc_abs_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_ABS_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_abs_rtt_congestion_threshold");
    /**
     * The {@code re.cc_bwe_slow_ramp_up_fallback_to_previous_n_sbwe} voip-param.
     */
    public static final VoipParamKey RE_CC_BWE_SLOW_RAMP_UP_FALLBACK_TO_PREVIOUS_N_SBWE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_bwe_slow_ramp_up_fallback_to_previous_n_sbwe");
    /**
     * The {@code re.cc_bwe_slow_ramp_up_freeze_duration_after_fallback} voip-param.
     */
    public static final VoipParamKey RE_CC_BWE_SLOW_RAMP_UP_FREEZE_DURATION_AFTER_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "re.cc_bwe_slow_ramp_up_freeze_duration_after_fallback");
    /**
     * The {@code re.cc_bwe_slow_ramp_up_hold_on_period_in_sec} voip-param.
     */
    public static final VoipParamKey RE_CC_BWE_SLOW_RAMP_UP_HOLD_ON_PERIOD_IN_SEC = new VoipParamKey(VoipParamType.INTEGER, "re.cc_bwe_slow_ramp_up_hold_on_period_in_sec");
    /**
     * The {@code re.cc_bwe_slow_ramp_up_only_near_ceiling} voip-param.
     */
    public static final VoipParamKey RE_CC_BWE_SLOW_RAMP_UP_ONLY_NEAR_CEILING = new VoipParamKey(VoipParamType.INTEGER, "re.cc_bwe_slow_ramp_up_only_near_ceiling");
    /**
     * The {@code re.cc_bwe_slow_ramp_up_use_cur_tx_bitrate} voip-param.
     */
    public static final VoipParamKey RE_CC_BWE_SLOW_RAMP_UP_USE_CUR_TX_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_bwe_slow_ramp_up_use_cur_tx_bitrate");
    /**
     * The {@code re.cc_ml_cong_features_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_CONG_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_cong_features_bitmap");
    /**
     * The {@code re.cc_ml_cong_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_CONG_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_cong_n_feature");
    /**
     * The {@code re.cc_ml_cong_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_CONG_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_cong_ts_length");
    /**
     * The {@code re.cc_ml_hd_targeting_check_time_ms} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING_CHECK_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting_check_time_ms");
    /**
     * The {@code re.cc_ml_hd_targeting_features_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting_features_bitmap");
    /**
     * The {@code re.cc_ml_hd_targeting_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting_n_feature");
    /**
     * The {@code re.cc_ml_hd_targeting_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting_ts_length");
    /**
     * The {@code re.cc_ml_hd_targeting2_features_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING2_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting2_features_bitmap");
    /**
     * The {@code re.cc_ml_hd_targeting2_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING2_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting2_n_feature");
    /**
     * The {@code re.cc_ml_hd_targeting2_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_HD_TARGETING2_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_hd_targeting2_ts_length");
    /**
     * The {@code re.cc_ml_multi_class_congestion_probability_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_MULTI_CLASS_CONGESTION_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_multi_class_congestion_probability_threshold");
    /**
     * The {@code re.cc_ml_multi_class_stop_mcp_on_congestion} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_MULTI_CLASS_STOP_MCP_ON_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_multi_class_stop_mcp_on_congestion");
    /**
     * The {@code re.cc_ml_multi_class_undershoot_probability_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_MULTI_CLASS_UNDERSHOOT_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_multi_class_undershoot_probability_threshold");
    /**
     * The {@code re.cc_ml_offline_rl_bwe_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_OFFLINE_RL_BWE_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_offline_rl_bwe_bitmap");
    /**
     * The {@code re.cc_ml_offline_rl_bwe_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_OFFLINE_RL_BWE_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_offline_rl_bwe_n_feature");
    /**
     * The {@code re.cc_ml_offline_rl_bwe_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_OFFLINE_RL_BWE_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_offline_rl_bwe_ts_length");
    /**
     * The {@code re.cc_ml_plc_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_PLC_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_plc_ts_length");
    /**
     * The {@code re.cc_ml_tr_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_TR_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_tr_n_feature");
    /**
     * The {@code re.cc_ml_tr_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_TR_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_tr_ts_length");
    /**
     * The {@code re.cc_ml_undershoot_features_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot_features_bitmap");
    /**
     * The {@code re.cc_ml_undershoot_num_classes} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT_NUM_CLASSES = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot_num_classes");
    /**
     * The {@code re.cc_ml_undershoot_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot_ts_length");
    /**
     * The {@code re.cc_ml_undershoot2_features_bitmap} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT2_FEATURES_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot2_features_bitmap");
    /**
     * The {@code re.cc_ml_undershoot2_n_feature} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT2_N_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot2_n_feature");
    /**
     * The {@code re.cc_ml_undershoot2_ts_length} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_UNDERSHOOT2_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_undershoot2_ts_length");
    /**
     * The {@code re.cc_ml_use_namespace_v2_automos} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_AUTOMOS = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_automos");
    /**
     * The {@code re.cc_ml_use_namespace_v2_cong} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_CONG = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_cong");
    /**
     * The {@code re.cc_ml_use_namespace_v2_gc_hd_target} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_GC_HD_TARGET = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_gc_hd_target");
    /**
     * The {@code re.cc_ml_use_namespace_v2_gc_undershoot} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_GC_UNDERSHOOT = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_gc_undershoot");
    /**
     * The {@code re.cc_ml_use_namespace_v2_hd_target} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_HD_TARGET = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_hd_target");
    /**
     * The {@code re.cc_ml_use_namespace_v2_nadl} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_NADL = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_nadl");
    /**
     * The {@code re.cc_ml_use_namespace_v2_ns} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_NS = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_ns");
    /**
     * The {@code re.cc_ml_use_namespace_v2_plc} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_PLC = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_plc");
    /**
     * The {@code re.cc_ml_use_namespace_v2_quickhd} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_QUICKHD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_quickhd");
    /**
     * The {@code re.cc_ml_use_namespace_v2_rl_bwe} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_RL_BWE = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_rl_bwe");
    /**
     * The {@code re.cc_ml_use_namespace_v2_tr} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_TR = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_tr");
    /**
     * The {@code re.cc_ml_use_namespace_v2_undershoot} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_UNDERSHOOT = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_undershoot");
    /**
     * The {@code re.cc_ml_use_namespace_v2_vmos} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_VMOS = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_vmos");
    /**
     * The {@code re.cc_ml_use_namespace_v2_vsr} voip-param.
     */
    public static final VoipParamKey RE_CC_ML_USE_NAMESPACE_V2_VSR = new VoipParamKey(VoipParamType.INTEGER, "re.cc_ml_use_namespace_v2_vsr");
    /**
     * The {@code re.cc_no_data_received_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_NO_DATA_RECEIVED_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_no_data_received_threshold");
    /**
     * The {@code re.cc_no_initial_rtt_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_NO_INITIAL_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_no_initial_rtt_threshold");
    /**
     * The {@code re.cc_no_rtcp_received_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_NO_RTCP_RECEIVED_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_no_rtcp_received_threshold");
    /**
     * The {@code re.cc_packet_loss_percentage_approaching_multiplier} voip-param.
     */
    public static final VoipParamKey RE_CC_PACKET_LOSS_PERCENTAGE_APPROACHING_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "re.cc_packet_loss_percentage_approaching_multiplier");
    /**
     * The {@code re.cc_packet_loss_percentage_heavy_multiplier} voip-param.
     */
    public static final VoipParamKey RE_CC_PACKET_LOSS_PERCENTAGE_HEAVY_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "re.cc_packet_loss_percentage_heavy_multiplier");
    /**
     * The {@code re.cc_packet_loss_percentage_threshold} voip-param.
     */
    public static final VoipParamKey RE_CC_PACKET_LOSS_PERCENTAGE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.cc_packet_loss_percentage_threshold");
    /**
     * The {@code re.cc_signal_mask_to_pause_sender_bwe_ramp_up} voip-param.
     */
    public static final VoipParamKey RE_CC_SIGNAL_MASK_TO_PAUSE_SENDER_BWE_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "re.cc_signal_mask_to_pause_sender_bwe_ramp_up");
    /**
     * The {@code re.cc_tx_and_peer_rx_slide_window_entries_to_eval} voip-param.
     */
    public static final VoipParamKey RE_CC_TX_AND_PEER_RX_SLIDE_WINDOW_ENTRIES_TO_EVAL = new VoipParamKey(VoipParamType.INTEGER, "re.cc_tx_and_peer_rx_slide_window_entries_to_eval");
    /**
     * The {@code re.ceiling_calculation} voip-param.
     */
    public static final VoipParamKey RE_CEILING_CALCULATION = new VoipParamKey(VoipParamType.INTEGER, "re.ceiling_calculation");
    /**
     * The {@code re.ceiling_calculation_dl} voip-param.
     */
    public static final VoipParamKey RE_CEILING_CALCULATION_DL = new VoipParamKey(VoipParamType.INTEGER, "re.ceiling_calculation_dl");
    /**
     * The {@code re.comb_psnr_sample_interval} voip-param.
     */
    public static final VoipParamKey RE_COMB_PSNR_SAMPLE_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "re.comb_psnr_sample_interval");
    /**
     * The {@code re.cong_model_name} voip-param.
     */
    public static final VoipParamKey RE_CONG_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.cong_model_name");
    /**
     * The {@code re.congestion_hold_ms} voip-param.
     */
    public static final VoipParamKey RE_CONGESTION_HOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "re.congestion_hold_ms");
    /**
     * The {@code re.congestion_rtt_ratio} voip-param.
     */
    public static final VoipParamKey RE_CONGESTION_RTT_RATIO = new VoipParamKey(VoipParamType.FLOAT, "re.congestion_rtt_ratio");
    /**
     * The {@code re.content_detector_static_blocks_thresh_perc} voip-param.
     */
    public static final VoipParamKey RE_CONTENT_DETECTOR_STATIC_BLOCKS_THRESH_PERC = new VoipParamKey(VoipParamType.INTEGER, "re.content_detector_static_blocks_thresh_perc");
    /**
     * The {@code re.content_detector_version} voip-param.
     */
    public static final VoipParamKey RE_CONTENT_DETECTOR_VERSION = new VoipParamKey(VoipParamType.INTEGER, "re.content_detector_version");
    /**
     * The {@code re.disable_h264_dec_impl_mask} voip-param.
     */
    public static final VoipParamKey RE_DISABLE_H264_DEC_IMPL_MASK = new VoipParamKey(VoipParamType.INTEGER, "re.disable_h264_dec_impl_mask");
    /**
     * The {@code re.disable_inference_at_high_latency} voip-param.
     */
    public static final VoipParamKey RE_DISABLE_INFERENCE_AT_HIGH_LATENCY = new VoipParamKey(VoipParamType.INTEGER, "re.disable_inference_at_high_latency");
    /**
     * The {@code re.dl_forced_probing_ts_length} voip-param.
     */
    public static final VoipParamKey RE_DL_FORCED_PROBING_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.dl_forced_probing_ts_length");
    /**
     * The {@code re.dl_max_target_bitrate} voip-param.
     */
    public static final VoipParamKey RE_DL_MAX_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "re.dl_max_target_bitrate");
    /**
     * The {@code re.dl_min_target_bitrate} voip-param.
     */
    public static final VoipParamKey RE_DL_MIN_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "re.dl_min_target_bitrate");
    /**
     * The {@code re.dl_undershoot_model_name} voip-param.
     */
    public static final VoipParamKey RE_DL_UNDERSHOOT_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.dl_undershoot_model_name");
    /**
     * The {@code re.double_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey RE_DOUBLE_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.double_rtt_congestion_threshold");
    /**
     * The {@code re.double_rtt_multiplier} voip-param.
     */
    public static final VoipParamKey RE_DOUBLE_RTT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "re.double_rtt_multiplier");
    /**
     * The {@code re.double_tx_delay_congestion_threshold} voip-param.
     */
    public static final VoipParamKey RE_DOUBLE_TX_DELAY_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.double_tx_delay_congestion_threshold");
    /**
     * The {@code re.dyn_alloc_c2r_rtt_factor} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_C2R_RTT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "re.dyn_alloc_c2r_rtt_factor");
    /**
     * The {@code re.dyn_alloc_default_rtt_estimate} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_DEFAULT_RTT_ESTIMATE = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_default_rtt_estimate");
    /**
     * The {@code re.dyn_alloc_get_rtt_from_alloc_err} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_GET_RTT_FROM_ALLOC_ERR = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_get_rtt_from_alloc_err");
    /**
     * The {@code re.dyn_alloc_osb_alloc_strategy} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_OSB_ALLOC_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_osb_alloc_strategy");
    /**
     * The {@code re.dyn_alloc_rebind_rtt_factor} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_REBIND_RTT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "re.dyn_alloc_rebind_rtt_factor");
    /**
     * The {@code re.dyn_alloc_timeout_allow_tcp} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_ALLOW_TCP = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_allow_tcp");
    /**
     * The {@code re.dyn_alloc_timeout_backoff_base} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_BACKOFF_BASE = new VoipParamKey(VoipParamType.FLOAT, "re.dyn_alloc_timeout_backoff_base");
    /**
     * The {@code re.dyn_alloc_timeout_burst} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_BURST = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_burst");
    /**
     * The {@code re.dyn_alloc_timeout_c2r_rtt_strategy} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_C2R_RTT_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_c2r_rtt_strategy");
    /**
     * The {@code re.dyn_alloc_timeout_debug_bitmap} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_DEBUG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_debug_bitmap");
    /**
     * The {@code re.dyn_alloc_timeout_enable} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_enable");
    /**
     * The {@code re.dyn_alloc_timeout_enable_L1330464PRV} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_ENABLE_L1330464PRV = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_enable_L1330464PRV");
    /**
     * The {@code re.dyn_alloc_timeout_include_ping} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_INCLUDE_PING = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_include_ping");
    /**
     * The {@code re.dyn_alloc_timeout_max_bind_timeout} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_MAX_BIND_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_max_bind_timeout");
    /**
     * The {@code re.dyn_alloc_timeout_max_relay_election_timeout} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_MAX_RELAY_ELECTION_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_max_relay_election_timeout");
    /**
     * The {@code re.dyn_alloc_timeout_min_bind_timeout} voip-param.
     */
    public static final VoipParamKey RE_DYN_ALLOC_TIMEOUT_MIN_BIND_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "re.dyn_alloc_timeout_min_bind_timeout");
    /**
     * The {@code re.enable_av1_ltr} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_AV1_LTR = new VoipParamKey(VoipParamType.INTEGER, "re.enable_av1_ltr");
    /**
     * The {@code re.enable_bitrate_probing} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_BITRATE_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_bitrate_probing");
    /**
     * The {@code re.enable_bwe_ceiling_calc_by_turning_pt} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_BWE_CEILING_CALC_BY_TURNING_PT = new VoipParamKey(VoipParamType.INTEGER, "re.enable_bwe_ceiling_calc_by_turning_pt");
    /**
     * The {@code re.enable_capture_fps_median_filter} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_CAPTURE_FPS_MEDIAN_FILTER = new VoipParamKey(VoipParamType.INTEGER, "re.enable_capture_fps_median_filter");
    /**
     * The {@code re.enable_conditions_on_downlink} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_CONDITIONS_ON_DOWNLINK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_conditions_on_downlink");
    /**
     * The {@code re.enable_dl_forced_probing} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_DL_FORCED_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_dl_forced_probing");
    /**
     * The {@code re.enable_empty_rtt_check} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_EMPTY_RTT_CHECK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_empty_rtt_check");
    /**
     * The {@code re.enable_enh_scaling} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_ENH_SCALING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_enh_scaling");
    /**
     * The {@code re.enable_forced_probing} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_FORCED_PROBING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_forced_probing");
    /**
     * The {@code re.enable_fragmentation_fix} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_FRAGMENTATION_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.enable_fragmentation_fix");
    /**
     * The {@code re.enable_h265_dec} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_H265_DEC = new VoipParamKey(VoipParamType.INTEGER, "re.enable_h265_dec");
    /**
     * The {@code re.enable_h265_enc} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_H265_ENC = new VoipParamKey(VoipParamType.INTEGER, "re.enable_h265_enc");
    /**
     * The {@code re.enable_hostile_network_timeouts} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_HOSTILE_NETWORK_TIMEOUTS = new VoipParamKey(VoipParamType.INTEGER, "re.enable_hostile_network_timeouts");
    /**
     * The {@code re.enable_hw_dec_to_sw_fallback} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_HW_DEC_TO_SW_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_hw_dec_to_sw_fallback");
    /**
     * The {@code re.enable_init_info_recalculation_fix} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_INIT_INFO_RECALCULATION_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.enable_init_info_recalculation_fix");
    /**
     * The {@code re.enable_late_arriving_history_fix} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LATE_ARRIVING_HISTORY_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.enable_late_arriving_history_fix");
    /**
     * The {@code re.enable_lazy_realloc} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LAZY_REALLOC = new VoipParamKey(VoipParamType.INTEGER, "re.enable_lazy_realloc");
    /**
     * The {@code re.enable_libyuv_extra_asserts} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LIBYUV_EXTRA_ASSERTS = new VoipParamKey(VoipParamType.INTEGER, "re.enable_libyuv_extra_asserts");
    /**
     * The {@code re.enable_limit_bw_with_ceiling} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LIMIT_BW_WITH_CEILING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_limit_bw_with_ceiling");
    /**
     * The {@code re.enable_lqm_check} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LQM_CHECK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_lqm_check");
    /**
     * The {@code re.enable_ltr_ack} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LTR_ACK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_ltr_ack");
    /**
     * The {@code re.enable_ltr_nack} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LTR_NACK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_ltr_nack");
    /**
     * The {@code re.enable_ltr_pool} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_LTR_POOL = new VoipParamKey(VoipParamType.INTEGER, "re.enable_ltr_pool");
    /**
     * The {@code re.enable_oh264_ltr} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_OH264_LTR = new VoipParamKey(VoipParamType.INTEGER, "re.enable_oh264_ltr");
    /**
     * The {@code re.enable_packetizer_logging} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_PACKETIZER_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_packetizer_logging");
    /**
     * The {@code re.enable_peer_platform_fix} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_PEER_PLATFORM_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.enable_peer_platform_fix");
    /**
     * The {@code re.enable_psnr_resolution_ctrl} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_PSNR_RESOLUTION_CTRL = new VoipParamKey(VoipParamType.INTEGER, "re.enable_psnr_resolution_ctrl");
    /**
     * The {@code re.enable_refresh_frame} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_REFRESH_FRAME = new VoipParamKey(VoipParamType.INTEGER, "re.enable_refresh_frame");
    /**
     * The {@code re.enable_rpsi_recovery} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_RPSI_RECOVERY = new VoipParamKey(VoipParamType.INTEGER, "re.enable_rpsi_recovery");
    /**
     * The {@code re.enable_rtt_min_ema} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_RTT_MIN_EMA = new VoipParamKey(VoipParamType.INTEGER, "re.enable_rtt_min_ema");
    /**
     * The {@code re.enable_sampling_dist} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_SAMPLING_DIST = new VoipParamKey(VoipParamType.INTEGER, "re.enable_sampling_dist");
    /**
     * The {@code re.enable_screen_content_detection} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_SCREEN_CONTENT_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "re.enable_screen_content_detection");
    /**
     * The {@code re.enable_slide_window_min_rtt} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_SLIDE_WINDOW_MIN_RTT = new VoipParamKey(VoipParamType.INTEGER, "re.enable_slide_window_min_rtt");
    /**
     * The {@code re.enable_tcp_ping_bitmap} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_TCP_PING_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "re.enable_tcp_ping_bitmap");
    /**
     * The {@code re.enable_ul_vid_disable} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_UL_VID_DISABLE = new VoipParamKey(VoipParamType.INTEGER, "re.enable_ul_vid_disable");
    /**
     * The {@code re.enable_uvq_detailed_tracking} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_UVQ_DETAILED_TRACKING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_uvq_detailed_tracking");
    /**
     * The {@code re.enable_uvq_inf} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_UVQ_INF = new VoipParamKey(VoipParamType.INTEGER, "re.enable_uvq_inf");
    /**
     * The {@code re.enable_uvq_load} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_UVQ_LOAD = new VoipParamKey(VoipParamType.INTEGER, "re.enable_uvq_load");
    /**
     * The {@code re.enable_uvq_ts_log} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_UVQ_TS_LOG = new VoipParamKey(VoipParamType.INTEGER, "re.enable_uvq_ts_log");
    /**
     * The {@code re.enable_verbose_ml_logging} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VERBOSE_ML_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "re.enable_verbose_ml_logging");
    /**
     * The {@code re.enable_vmos} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VMOS = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vmos");
    /**
     * The {@code re.enable_vp9} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VP9 = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vp9");
    /**
     * The {@code re.enable_vsr_inf} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VSR_INF = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vsr_inf");
    /**
     * The {@code re.enable_vsr_inf_update_lock} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VSR_INF_UPDATE_LOCK = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vsr_inf_update_lock");
    /**
     * The {@code re.enable_vsr_load} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VSR_LOAD = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vsr_load");
    /**
     * The {@code re.enable_vt_decode_err_handle} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_VT_DECODE_ERR_HANDLE = new VoipParamKey(VoipParamType.INTEGER, "re.enable_vt_decode_err_handle");
    /**
     * The {@code re.enable_zero_rtt_check_slide_window} voip-param.
     */
    public static final VoipParamKey RE_ENABLE_ZERO_RTT_CHECK_SLIDE_WINDOW = new VoipParamKey(VoipParamType.INTEGER, "re.enable_zero_rtt_check_slide_window");
    /**
     * The {@code re.enc_psnr_downgrade_threshold} voip-param.
     */
    public static final VoipParamKey RE_ENC_PSNR_DOWNGRADE_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "re.enc_psnr_downgrade_threshold");
    /**
     * The {@code re.equalize_packet_sizes} voip-param.
     */
    public static final VoipParamKey RE_EQUALIZE_PACKET_SIZES = new VoipParamKey(VoipParamType.INTEGER, "re.equalize_packet_sizes");
    /**
     * The {@code re.exit_forced_probing_after_rbwe_update} voip-param.
     */
    public static final VoipParamKey RE_EXIT_FORCED_PROBING_AFTER_RBWE_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "re.exit_forced_probing_after_rbwe_update");
    /**
     * The {@code re.exit_forced_probing_after_sbwe_update} voip-param.
     */
    public static final VoipParamKey RE_EXIT_FORCED_PROBING_AFTER_SBWE_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "re.exit_forced_probing_after_sbwe_update");
    /**
     * The {@code re.fall_back_to_rtt_congestion_upon_delay_error} voip-param.
     */
    public static final VoipParamKey RE_FALL_BACK_TO_RTT_CONGESTION_UPON_DELAY_ERROR = new VoipParamKey(VoipParamType.INTEGER, "re.fall_back_to_rtt_congestion_upon_delay_error");
    /**
     * The {@code re.fallback_model} voip-param.
     */
    public static final VoipParamKey RE_FALLBACK_MODEL = new VoipParamKey(VoipParamType.STRING, "re.fallback_model");
    /**
     * The {@code re.fallback_model_type} voip-param.
     */
    public static final VoipParamKey RE_FALLBACK_MODEL_TYPE = new VoipParamKey(VoipParamType.INTEGER, "re.fallback_model_type");
    /**
     * The {@code re.force_additive_sender_bwe_inc} voip-param.
     */
    public static final VoipParamKey RE_FORCE_ADDITIVE_SENDER_BWE_INC = new VoipParamKey(VoipParamType.INTEGER, "re.force_additive_sender_bwe_inc");
    /**
     * The {@code re.force_fallback_to_openh264_enc} voip-param.
     */
    public static final VoipParamKey RE_FORCE_FALLBACK_TO_OPENH264_ENC = new VoipParamKey(VoipParamType.INTEGER, "re.force_fallback_to_openh264_enc");
    /**
     * The {@code re.forced_probing_after_segment_start_ms} voip-param.
     */
    public static final VoipParamKey RE_FORCED_PROBING_AFTER_SEGMENT_START_MS = new VoipParamKey(VoipParamType.INTEGER, "re.forced_probing_after_segment_start_ms");
    /**
     * The {@code re.forced_probing_interval_ms} voip-param.
     */
    public static final VoipParamKey RE_FORCED_PROBING_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "re.forced_probing_interval_ms");
    /**
     * The {@code re.forced_probing_scheme} voip-param.
     */
    public static final VoipParamKey RE_FORCED_PROBING_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "re.forced_probing_scheme");
    /**
     * The {@code re.forced_probing_ts_length} voip-param.
     */
    public static final VoipParamKey RE_FORCED_PROBING_TS_LENGTH = new VoipParamKey(VoipParamType.INTEGER, "re.forced_probing_ts_length");
    /**
     * The {@code re.hd_targeting_model_name} voip-param.
     */
    public static final VoipParamKey RE_HD_TARGETING_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.hd_targeting_model_name");
    /**
     * The {@code re.hd_targeting2_model_name} voip-param.
     */
    public static final VoipParamKey RE_HD_TARGETING2_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.hd_targeting2_model_name");
    /**
     * The {@code re.ignore_transient_delay_error} voip-param.
     */
    public static final VoipParamKey RE_IGNORE_TRANSIENT_DELAY_ERROR = new VoipParamKey(VoipParamType.INTEGER, "re.ignore_transient_delay_error");
    /**
     * The {@code re.inf_latency_win_size} voip-param.
     */
    public static final VoipParamKey RE_INF_LATENCY_WIN_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.inf_latency_win_size");
    /**
     * The {@code re.ios_zero_copy} voip-param.
     */
    public static final VoipParamKey RE_IOS_ZERO_COPY = new VoipParamKey(VoipParamType.INTEGER, "re.ios_zero_copy");
    /**
     * The {@code re.limit_bw_with_ceiling_duration_sec} voip-param.
     */
    public static final VoipParamKey RE_LIMIT_BW_WITH_CEILING_DURATION_SEC = new VoipParamKey(VoipParamType.INTEGER, "re.limit_bw_with_ceiling_duration_sec");
    /**
     * The {@code re.limit_bw_with_ceiling_multiplier} voip-param.
     */
    public static final VoipParamKey RE_LIMIT_BW_WITH_CEILING_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "re.limit_bw_with_ceiling_multiplier");
    /**
     * The {@code re.limit_bw_with_ceiling_skip_times} voip-param.
     */
    public static final VoipParamKey RE_LIMIT_BW_WITH_CEILING_SKIP_TIMES = new VoipParamKey(VoipParamType.INTEGER, "re.limit_bw_with_ceiling_skip_times");
    /**
     * The {@code re.lterm_rtt_ema_size} voip-param.
     */
    public static final VoipParamKey RE_LTERM_RTT_EMA_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.lterm_rtt_ema_size");
    /**
     * The {@code re.max_inf_latency_ms} voip-param.
     */
    public static final VoipParamKey RE_MAX_INF_LATENCY_MS = new VoipParamKey(VoipParamType.INTEGER, "re.max_inf_latency_ms");
    /**
     * The {@code re.max_udp_fallbacks} voip-param.
     */
    public static final VoipParamKey RE_MAX_UDP_FALLBACKS = new VoipParamKey(VoipParamType.INTEGER, "re.max_udp_fallbacks");
    /**
     * The {@code re.max_udp_pongs_for_network_restart} voip-param.
     */
    public static final VoipParamKey RE_MAX_UDP_PONGS_FOR_NETWORK_RESTART = new VoipParamKey(VoipParamType.INTEGER, "re.max_udp_pongs_for_network_restart");
    /**
     * The {@code re.max_udp_retries_before_tcp_attempt} voip-param.
     */
    public static final VoipParamKey RE_MAX_UDP_RETRIES_BEFORE_TCP_ATTEMPT = new VoipParamKey(VoipParamType.INTEGER, "re.max_udp_retries_before_tcp_attempt");
    /**
     * The {@code re.mcp_stop_fix} voip-param.
     */
    public static final VoipParamKey RE_MCP_STOP_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.mcp_stop_fix");
    /**
     * The {@code re.min_fragmentation_size} voip-param.
     */
    public static final VoipParamKey RE_MIN_FRAGMENTATION_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.min_fragmentation_size");
    /**
     * The {@code re.min_packets_per_frame} voip-param.
     */
    public static final VoipParamKey RE_MIN_PACKETS_PER_FRAME = new VoipParamKey(VoipParamType.INTEGER, "re.min_packets_per_frame");
    /**
     * The {@code re.min_slrtt_upd_ms} voip-param.
     */
    public static final VoipParamKey RE_MIN_SLRTT_UPD_MS = new VoipParamKey(VoipParamType.INTEGER, "re.min_slrtt_upd_ms");
    /**
     * The {@code re.nack_continue_on_cache_miss} voip-param.
     */
    public static final VoipParamKey RE_NACK_CONTINUE_ON_CACHE_MISS = new VoipParamKey(VoipParamType.INTEGER, "re.nack_continue_on_cache_miss");
    /**
     * The {@code re.nack_skip_recovery_on_max_retries} voip-param.
     */
    public static final VoipParamKey RE_NACK_SKIP_RECOVERY_ON_MAX_RETRIES = new VoipParamKey(VoipParamType.INTEGER, "re.nack_skip_recovery_on_max_retries");
    /**
     * The {@code re.nack_skip_recovery_on_rate_limit} voip-param.
     */
    public static final VoipParamKey RE_NACK_SKIP_RECOVERY_ON_RATE_LIMIT = new VoipParamKey(VoipParamType.INTEGER, "re.nack_skip_recovery_on_rate_limit");
    /**
     * The {@code re.no_data_received_threshold} voip-param.
     */
    public static final VoipParamKey RE_NO_DATA_RECEIVED_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.no_data_received_threshold");
    /**
     * The {@code re.no_rtcp_received_threshold} voip-param.
     */
    public static final VoipParamKey RE_NO_RTCP_RECEIVED_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.no_rtcp_received_threshold");
    /**
     * The {@code re.nondroppable_count_to_stop_resend_droppable} voip-param.
     */
    public static final VoipParamKey RE_NONDROPPABLE_COUNT_TO_STOP_RESEND_DROPPABLE = new VoipParamKey(VoipParamType.INTEGER, "re.nondroppable_count_to_stop_resend_droppable");
    /**
     * The {@code re.nonkeyframe_maxresend_change_enabled} voip-param.
     */
    public static final VoipParamKey RE_NONKEYFRAME_MAXRESEND_CHANGE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "re.nonkeyframe_maxresend_change_enabled");
    /**
     * The {@code re.nonkeyframe_maxresend_decrease_speed} voip-param.
     */
    public static final VoipParamKey RE_NONKEYFRAME_MAXRESEND_DECREASE_SPEED = new VoipParamKey(VoipParamType.INTEGER, "re.nonkeyframe_maxresend_decrease_speed");
    /**
     * The {@code re.offline_rl_bwe_model_name} voip-param.
     */
    public static final VoipParamKey RE_OFFLINE_RL_BWE_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.offline_rl_bwe_model_name");
    /**
     * The {@code re.p_interval} voip-param.
     */
    public static final VoipParamKey RE_P_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "re.p_interval");
    /**
     * The {@code re.p2p_add_rte_reflexive_addr} voip-param.
     */
    public static final VoipParamKey RE_P2P_ADD_RTE_REFLEXIVE_ADDR = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_add_rte_reflexive_addr");
    /**
     * The {@code re.p2p_keep_alive_force_fixed_cadence} voip-param.
     */
    public static final VoipParamKey RE_P2P_KEEP_ALIVE_FORCE_FIXED_CADENCE = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_keep_alive_force_fixed_cadence");
    /**
     * The {@code re.p2p_keep_alive_timeout_ms} voip-param.
     */
    public static final VoipParamKey RE_P2P_KEEP_ALIVE_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_keep_alive_timeout_ms");
    /**
     * The {@code re.p2p_nego_max_retry_cnt} voip-param.
     */
    public static final VoipParamKey RE_P2P_NEGO_MAX_RETRY_CNT = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_nego_max_retry_cnt");
    /**
     * The {@code re.p2p_request_timeout} voip-param.
     */
    public static final VoipParamKey RE_P2P_REQUEST_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_request_timeout");
    /**
     * The {@code re.p2p_retry_mode} voip-param.
     */
    public static final VoipParamKey RE_P2P_RETRY_MODE = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_retry_mode");
    /**
     * The {@code re.p2p_retry_timeout} voip-param.
     */
    public static final VoipParamKey RE_P2P_RETRY_TIMEOUT = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_retry_timeout");
    /**
     * The {@code re.p2p_retry_timeout_short} voip-param.
     */
    public static final VoipParamKey RE_P2P_RETRY_TIMEOUT_SHORT = new VoipParamKey(VoipParamType.INTEGER, "re.p2p_retry_timeout_short");
    /**
     * The {@code re.periodic_ltrp_interval_in_frames} voip-param.
     */
    public static final VoipParamKey RE_PERIODIC_LTRP_INTERVAL_IN_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "re.periodic_ltrp_interval_in_frames");
    /**
     * The {@code re.plc_model_name} voip-param.
     */
    public static final VoipParamKey RE_PLC_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.plc_model_name");
    /**
     * The {@code re.pld_max_nacks} voip-param.
     */
    public static final VoipParamKey RE_PLD_MAX_NACKS = new VoipParamKey(VoipParamType.INTEGER, "re.pld_max_nacks");
    /**
     * The {@code re.pld_rtt_ratio} voip-param.
     */
    public static final VoipParamKey RE_PLD_RTT_RATIO = new VoipParamKey(VoipParamType.FLOAT, "re.pld_rtt_ratio");
    /**
     * The {@code re.pp_ceiling_stat} voip-param.
     */
    public static final VoipParamKey RE_PP_CEILING_STAT = new VoipParamKey(VoipParamType.INTEGER, "re.pp_ceiling_stat");
    /**
     * The {@code re.pp_est_max_age} voip-param.
     */
    public static final VoipParamKey RE_PP_EST_MAX_AGE = new VoipParamKey(VoipParamType.INTEGER, "re.pp_est_max_age");
    /**
     * The {@code re.pp_flip_count_enabled} voip-param.
     */
    public static final VoipParamKey RE_PP_FLIP_COUNT_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "re.pp_flip_count_enabled");
    /**
     * The {@code re.pp_flip_count_for_hd_hi_threshold} voip-param.
     */
    public static final VoipParamKey RE_PP_FLIP_COUNT_FOR_HD_HI_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.pp_flip_count_for_hd_hi_threshold");
    /**
     * The {@code re.pp_flip_count_for_hd_lo_threshold} voip-param.
     */
    public static final VoipParamKey RE_PP_FLIP_COUNT_FOR_HD_LO_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "re.pp_flip_count_for_hd_lo_threshold");
    /**
     * The {@code re.pp_flip_count_skip_sanity_check} voip-param.
     */
    public static final VoipParamKey RE_PP_FLIP_COUNT_SKIP_SANITY_CHECK = new VoipParamKey(VoipParamType.INTEGER, "re.pp_flip_count_skip_sanity_check");
    /**
     * The {@code re.pp_slide_window_size} voip-param.
     */
    public static final VoipParamKey RE_PP_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.pp_slide_window_size");
    /**
     * The {@code re.prefer_pp_udst_source} voip-param.
     */
    public static final VoipParamKey RE_PREFER_PP_UDST_SOURCE = new VoipParamKey(VoipParamType.INTEGER, "re.prefer_pp_udst_source");
    /**
     * The {@code re.psnr_calc_end_bps} voip-param.
     */
    public static final VoipParamKey RE_PSNR_CALC_END_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.psnr_calc_end_bps");
    /**
     * The {@code re.psnr_calc_start_bps} voip-param.
     */
    public static final VoipParamKey RE_PSNR_CALC_START_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.psnr_calc_start_bps");
    /**
     * The {@code re.refresh_vp9_key_frame_during_res_change} voip-param.
     */
    public static final VoipParamKey RE_REFRESH_VP9_KEY_FRAME_DURING_RES_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "re.refresh_vp9_key_frame_during_res_change");
    /**
     * The {@code re.rplr_algo} voip-param.
     */
    public static final VoipParamKey RE_RPLR_ALGO = new VoipParamKey(VoipParamType.INTEGER, "re.rplr_algo");
    /**
     * The {@code re.rplr_cz_cluster_sz} voip-param.
     */
    public static final VoipParamKey RE_RPLR_CZ_CLUSTER_SZ = new VoipParamKey(VoipParamType.FLOAT, "re.rplr_cz_cluster_sz");
    /**
     * The {@code re.rplr_cz_dist_adj_factor} voip-param.
     */
    public static final VoipParamKey RE_RPLR_CZ_DIST_ADJ_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "re.rplr_cz_dist_adj_factor");
    /**
     * The {@code re.rplr_init_eval_size} voip-param.
     */
    public static final VoipParamKey RE_RPLR_INIT_EVAL_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.rplr_init_eval_size");
    /**
     * The {@code re.rplr_init_win_size} voip-param.
     */
    public static final VoipParamKey RE_RPLR_INIT_WIN_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.rplr_init_win_size");
    /**
     * The {@code re.rplr_max_delta_mean} voip-param.
     */
    public static final VoipParamKey RE_RPLR_MAX_DELTA_MEAN = new VoipParamKey(VoipParamType.FLOAT, "re.rplr_max_delta_mean");
    /**
     * The {@code re.rplr_max_delta_var} voip-param.
     */
    public static final VoipParamKey RE_RPLR_MAX_DELTA_VAR = new VoipParamKey(VoipParamType.FLOAT, "re.rplr_max_delta_var");
    /**
     * The {@code re.rplr_min_plr} voip-param.
     */
    public static final VoipParamKey RE_RPLR_MIN_PLR = new VoipParamKey(VoipParamType.FLOAT, "re.rplr_min_plr");
    /**
     * The {@code re.rplr_wait_ms} voip-param.
     */
    public static final VoipParamKey RE_RPLR_WAIT_MS = new VoipParamKey(VoipParamType.INTEGER, "re.rplr_wait_ms");
    /**
     * The {@code re.rplr_win_size} voip-param.
     */
    public static final VoipParamKey RE_RPLR_WIN_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.rplr_win_size");
    /**
     * The {@code re.sender_side_bwe_slide_window_size} voip-param.
     */
    public static final VoipParamKey RE_SENDER_SIDE_BWE_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.sender_side_bwe_slide_window_size");
    /**
     * The {@code re.short_hd_duration_sec} voip-param.
     */
    public static final VoipParamKey RE_SHORT_HD_DURATION_SEC = new VoipParamKey(VoipParamType.INTEGER, "re.short_hd_duration_sec");
    /**
     * The {@code re.skip_forced_probing_during_mcp} voip-param.
     */
    public static final VoipParamKey RE_SKIP_FORCED_PROBING_DURING_MCP = new VoipParamKey(VoipParamType.INTEGER, "re.skip_forced_probing_during_mcp");
    /**
     * The {@code re.skip_forced_probing_during_pp} voip-param.
     */
    public static final VoipParamKey RE_SKIP_FORCED_PROBING_DURING_PP = new VoipParamKey(VoipParamType.INTEGER, "re.skip_forced_probing_during_pp");
    /**
     * The {@code re.skip_forced_probing_during_udst} voip-param.
     */
    public static final VoipParamKey RE_SKIP_FORCED_PROBING_DURING_UDST = new VoipParamKey(VoipParamType.INTEGER, "re.skip_forced_probing_during_udst");
    /**
     * The {@code re.staggered_binds_enable} voip-param.
     */
    public static final VoipParamKey RE_STAGGERED_BINDS_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "re.staggered_binds_enable");
    /**
     * The {@code re.staggered_binds_rank_by_c2r_rtt} voip-param.
     */
    public static final VoipParamKey RE_STAGGERED_BINDS_RANK_BY_C2R_RTT = new VoipParamKey(VoipParamType.INTEGER, "re.staggered_binds_rank_by_c2r_rtt");
    /**
     * The {@code re.staggered_binds_wave_delay_ms} voip-param.
     */
    public static final VoipParamKey RE_STAGGERED_BINDS_WAVE_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "re.staggered_binds_wave_delay_ms");
    /**
     * The {@code re.staggered_binds_wave_size} voip-param.
     */
    public static final VoipParamKey RE_STAGGERED_BINDS_WAVE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.staggered_binds_wave_size");
    /**
     * The {@code re.sterm_rtt_ema_size} voip-param.
     */
    public static final VoipParamKey RE_STERM_RTT_EMA_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.sterm_rtt_ema_size");
    /**
     * The {@code re.support_av1_in_gc} voip-param.
     */
    public static final VoipParamKey RE_SUPPORT_AV1_IN_GC = new VoipParamKey(VoipParamType.INTEGER, "re.support_av1_in_gc");
    /**
     * The {@code re.tcp_ping_interval} voip-param.
     */
    public static final VoipParamKey RE_TCP_PING_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "re.tcp_ping_interval");
    /**
     * The {@code re.tfrc_sender_report_bwe_action} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_BWE_ACTION = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_bwe_action");
    /**
     * The {@code re.tfrc_sender_report_bwe_detailed_action} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_BWE_DETAILED_ACTION = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_bwe_detailed_action");
    /**
     * The {@code re.tfrc_sender_report_bwe_detailed_action_sfu_dl} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_BWE_DETAILED_ACTION_SFU_DL = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_bwe_detailed_action_sfu_dl");
    /**
     * The {@code re.tfrc_sender_report_congestion_prob} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_CONGESTION_PROB = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_congestion_prob");
    /**
     * The {@code re.tfrc_sender_report_hd_targeting_prob} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_HD_TARGETING_PROB = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_hd_targeting_prob");
    /**
     * The {@code re.tfrc_sender_report_math_plr_type} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_MATH_PLR_TYPE = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_math_plr_type");
    /**
     * The {@code re.tfrc_sender_report_ml_metrics} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_ML_METRICS = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_ml_metrics");
    /**
     * The {@code re.tfrc_sender_report_ml_metrics_sfu_dl} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_ML_METRICS_SFU_DL = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_ml_metrics_sfu_dl");
    /**
     * The {@code re.tfrc_sender_report_ml_plc_inference_result} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_ML_PLC_INFERENCE_RESULT = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_ml_plc_inference_result");
    /**
     * The {@code re.tfrc_sender_report_tr_result} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_TR_RESULT = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_tr_result");
    /**
     * The {@code re.tfrc_sender_report_undershoot_type} voip-param.
     */
    public static final VoipParamKey RE_TFRC_SENDER_REPORT_UNDERSHOOT_TYPE = new VoipParamKey(VoipParamType.INTEGER, "re.tfrc_sender_report_undershoot_type");
    /**
     * The {@code re.timeout_after_accept_for_tcp_alloc_msec} voip-param.
     */
    public static final VoipParamKey RE_TIMEOUT_AFTER_ACCEPT_FOR_TCP_ALLOC_MSEC = new VoipParamKey(VoipParamType.INTEGER, "re.timeout_after_accept_for_tcp_alloc_msec");
    /**
     * The {@code re.timeout_for_tcp_alloc_msec} voip-param.
     */
    public static final VoipParamKey RE_TIMEOUT_FOR_TCP_ALLOC_MSEC = new VoipParamKey(VoipParamType.INTEGER, "re.timeout_for_tcp_alloc_msec");
    /**
     * The {@code re.tr_model_name} voip-param.
     */
    public static final VoipParamKey RE_TR_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.tr_model_name");
    /**
     * The {@code re.ts_diff_calculation_scheme} voip-param.
     */
    public static final VoipParamKey RE_TS_DIFF_CALCULATION_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "re.ts_diff_calculation_scheme");
    /**
     * The {@code re.udp_bind_timeout_for_tcp_hostile_in_msec} voip-param.
     */
    public static final VoipParamKey RE_UDP_BIND_TIMEOUT_FOR_TCP_HOSTILE_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "re.udp_bind_timeout_for_tcp_hostile_in_msec");
    /**
     * The {@code re.udp_bind_timeout_for_tcp_in_msec} voip-param.
     */
    public static final VoipParamKey RE_UDP_BIND_TIMEOUT_FOR_TCP_IN_MSEC = new VoipParamKey(VoipParamType.INTEGER, "re.udp_bind_timeout_for_tcp_in_msec");
    /**
     * The {@code re.udp_ping_on_tcp_interval} voip-param.
     */
    public static final VoipParamKey RE_UDP_PING_ON_TCP_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "re.udp_ping_on_tcp_interval");
    /**
     * The {@code re.ul_aud_issue_thresh} voip-param.
     */
    public static final VoipParamKey RE_UL_AUD_ISSUE_THRESH = new VoipParamKey(VoipParamType.INTEGER, "re.ul_aud_issue_thresh");
    /**
     * The {@code re.ul_lqm_calc_min_kbps} voip-param.
     */
    public static final VoipParamKey RE_UL_LQM_CALC_MIN_KBPS = new VoipParamKey(VoipParamType.INTEGER, "re.ul_lqm_calc_min_kbps");
    /**
     * The {@code re.ul_vid_disable_min_kbps} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_DISABLE_MIN_KBPS = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_disable_min_kbps");
    /**
     * The {@code re.ul_vid_disable_start_min_time_s} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_DISABLE_START_MIN_TIME_S = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_disable_start_min_time_s");
    /**
     * The {@code re.ul_vid_disable_thresh} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_DISABLE_THRESH = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_disable_thresh");
    /**
     * The {@code re.ul_vid_max_pause_time_s} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_MAX_PAUSE_TIME_S = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_max_pause_time_s");
    /**
     * The {@code re.ul_vid_min_kbps_offset} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_MIN_KBPS_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_min_kbps_offset");
    /**
     * The {@code re.ul_vid_reenable_timer_s} voip-param.
     */
    public static final VoipParamKey RE_UL_VID_REENABLE_TIMER_S = new VoipParamKey(VoipParamType.INTEGER, "re.ul_vid_reenable_timer_s");
    /**
     * The {@code re.undershoot_model_name} voip-param.
     */
    public static final VoipParamKey RE_UNDERSHOOT_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.undershoot_model_name");
    /**
     * The {@code re.undershoot_model2_name} voip-param.
     */
    public static final VoipParamKey RE_UNDERSHOOT_MODEL2_NAME = new VoipParamKey(VoipParamType.STRING, "re.undershoot_model2_name");
    /**
     * The {@code re.update_ul_sbwe_rc} voip-param.
     */
    public static final VoipParamKey RE_UPDATE_UL_SBWE_RC = new VoipParamKey(VoipParamType.INTEGER, "re.update_ul_sbwe_rc");
    /**
     * The {@code re.use_alternate_relay} voip-param.
     */
    public static final VoipParamKey RE_USE_ALTERNATE_RELAY = new VoipParamKey(VoipParamType.INTEGER, "re.use_alternate_relay");
    /**
     * The {@code re.use_delay_for_rtt_congestion} voip-param.
     */
    public static final VoipParamKey RE_USE_DELAY_FOR_RTT_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "re.use_delay_for_rtt_congestion");
    /**
     * The {@code re.uvq_frame_interval_ms} voip-param.
     */
    public static final VoipParamKey RE_UVQ_FRAME_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "re.uvq_frame_interval_ms");
    /**
     * The {@code re.uvq_input_size} voip-param.
     */
    public static final VoipParamKey RE_UVQ_INPUT_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.uvq_input_size");
    /**
     * The {@code re.uvq_model_name} voip-param.
     */
    public static final VoipParamKey RE_UVQ_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.uvq_model_name");
    /**
     * The {@code re.uvq_use_luma} voip-param.
     */
    public static final VoipParamKey RE_UVQ_USE_LUMA = new VoipParamKey(VoipParamType.INTEGER, "re.uvq_use_luma");
    /**
     * The {@code re.vid_ul_bps_ave_time_ms} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_BPS_AVE_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_bps_ave_time_ms");
    /**
     * The {@code re.vid_ul_bps_ema_size} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_BPS_EMA_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_bps_ema_size");
    /**
     * The {@code re.vid_ul_bps_ignore_time_after_restart_ms} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_BPS_IGNORE_TIME_AFTER_RESTART_MS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_bps_ignore_time_after_restart_ms");
    /**
     * The {@code re.vid_ul_enable_br_drop_fix} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_ENABLE_BR_DROP_FIX = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_enable_br_drop_fix");
    /**
     * The {@code re.vid_ul_enable_lqm_calc_logs} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_ENABLE_LQM_CALC_LOGS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_enable_lqm_calc_logs");
    /**
     * The {@code re.vid_ul_good_lqm_max_plr_ema} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_GOOD_LQM_MAX_PLR_EMA = new VoipParamKey(VoipParamType.FLOAT, "re.vid_ul_good_lqm_max_plr_ema");
    /**
     * The {@code re.vid_ul_good_lqm_max_rtt_ema_ms} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_GOOD_LQM_MAX_RTT_EMA_MS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_good_lqm_max_rtt_ema_ms");
    /**
     * The {@code re.vid_ul_good_lqm_min_ave_bps} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_GOOD_LQM_MIN_AVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_good_lqm_min_ave_bps");
    /**
     * The {@code re.vid_ul_good_lqm_min_bps} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_GOOD_LQM_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_good_lqm_min_bps");
    /**
     * The {@code re.vid_ul_lqm_sample_history_size} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_LQM_SAMPLE_HISTORY_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_lqm_sample_history_size");
    /**
     * The {@code re.vid_ul_poor_lqm_max_plr_ema} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_POOR_LQM_MAX_PLR_EMA = new VoipParamKey(VoipParamType.FLOAT, "re.vid_ul_poor_lqm_max_plr_ema");
    /**
     * The {@code re.vid_ul_poor_lqm_max_rtt_ema_ms} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_POOR_LQM_MAX_RTT_EMA_MS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_poor_lqm_max_rtt_ema_ms");
    /**
     * The {@code re.vid_ul_poor_lqm_min_ave_bps} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_POOR_LQM_MIN_AVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_poor_lqm_min_ave_bps");
    /**
     * The {@code re.vid_ul_poor_lqm_min_bps} voip-param.
     */
    public static final VoipParamKey RE_VID_UL_POOR_LQM_MIN_BPS = new VoipParamKey(VoipParamType.INTEGER, "re.vid_ul_poor_lqm_min_bps");
    /**
     * The {@code re.vid_uld_min_lqm_samples} voip-param.
     */
    public static final VoipParamKey RE_VID_ULD_MIN_LQM_SAMPLES = new VoipParamKey(VoipParamType.INTEGER, "re.vid_uld_min_lqm_samples");
    /**
     * The {@code re.vmos_model_name} voip-param.
     */
    public static final VoipParamKey RE_VMOS_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.vmos_model_name");
    /**
     * The {@code re.vsr_disable_battery_level} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_BATTERY_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_battery_level");
    /**
     * The {@code re.vsr_disable_hidden_state_updates} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_HIDDEN_STATE_UPDATES = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_hidden_state_updates");
    /**
     * The {@code re.vsr_disable_max_inference_error_count} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_MAX_INFERENCE_ERROR_COUNT = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_max_inference_error_count");
    /**
     * The {@code re.vsr_disable_max_inference_latency_ms} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_MAX_INFERENCE_LATENCY_MS = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_max_inference_latency_ms");
    /**
     * The {@code re.vsr_disable_min_psnr} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_MIN_PSNR = new VoipParamKey(VoipParamType.FLOAT, "re.vsr_disable_min_psnr");
    /**
     * The {@code re.vsr_disable_thermal_frame_count} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_THERMAL_FRAME_COUNT = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_thermal_frame_count");
    /**
     * The {@code re.vsr_disable_thermal_level} voip-param.
     */
    public static final VoipParamKey RE_VSR_DISABLE_THERMAL_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_disable_thermal_level");
    /**
     * The {@code re.vsr_dummy_inf} voip-param.
     */
    public static final VoipParamKey RE_VSR_DUMMY_INF = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_dummy_inf");
    /**
     * The {@code re.vsr_enable_clipping} voip-param.
     */
    public static final VoipParamKey RE_VSR_ENABLE_CLIPPING = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_enable_clipping");
    /**
     * The {@code re.vsr_enable_hidden_reset_resolution_changes} voip-param.
     */
    public static final VoipParamKey RE_VSR_ENABLE_HIDDEN_RESET_RESOLUTION_CHANGES = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_enable_hidden_reset_resolution_changes");
    /**
     * The {@code re.vsr_enable_psnr_calculation} voip-param.
     */
    public static final VoipParamKey RE_VSR_ENABLE_PSNR_CALCULATION = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_enable_psnr_calculation");
    /**
     * The {@code re.vsr_has_hidden_state} voip-param.
     */
    public static final VoipParamKey RE_VSR_HAS_HIDDEN_STATE = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_has_hidden_state");
    /**
     * The {@code re.vsr_height} voip-param.
     */
    public static final VoipParamKey RE_VSR_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_height");
    /**
     * The {@code re.vsr_hidden_dim_batch_size} voip-param.
     */
    public static final VoipParamKey RE_VSR_HIDDEN_DIM_BATCH_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_hidden_dim_batch_size");
    /**
     * The {@code re.vsr_hidden_dim_height} voip-param.
     */
    public static final VoipParamKey RE_VSR_HIDDEN_DIM_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_hidden_dim_height");
    /**
     * The {@code re.vsr_hidden_dim_num_channels} voip-param.
     */
    public static final VoipParamKey RE_VSR_HIDDEN_DIM_NUM_CHANNELS = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_hidden_dim_num_channels");
    /**
     * The {@code re.vsr_hidden_dim_width} voip-param.
     */
    public static final VoipParamKey RE_VSR_HIDDEN_DIM_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_hidden_dim_width");
    /**
     * The {@code re.vsr_inference_latency_window_size} voip-param.
     */
    public static final VoipParamKey RE_VSR_INFERENCE_LATENCY_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_inference_latency_window_size");
    /**
     * The {@code re.vsr_is_float32} voip-param.
     */
    public static final VoipParamKey RE_VSR_IS_FLOAT32 = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_is_float32");
    /**
     * The {@code re.vsr_load_outside_lock} voip-param.
     */
    public static final VoipParamKey RE_VSR_LOAD_OUTSIDE_LOCK = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_load_outside_lock");
    /**
     * The {@code re.vsr_model_load_max_retry} voip-param.
     */
    public static final VoipParamKey RE_VSR_MODEL_LOAD_MAX_RETRY = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_model_load_max_retry");
    /**
     * The {@code re.vsr_model_load_retry_interval_s} voip-param.
     */
    public static final VoipParamKey RE_VSR_MODEL_LOAD_RETRY_INTERVAL_S = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_model_load_retry_interval_s");
    /**
     * The {@code re.vsr_model_name} voip-param.
     */
    public static final VoipParamKey RE_VSR_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "re.vsr_model_name");
    /**
     * The {@code re.vsr_psnr_sample_interval} voip-param.
     */
    public static final VoipParamKey RE_VSR_PSNR_SAMPLE_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_psnr_sample_interval");
    /**
     * The {@code re.vsr_upsampling_ratio} voip-param.
     */
    public static final VoipParamKey RE_VSR_UPSAMPLING_RATIO = new VoipParamKey(VoipParamType.FLOAT, "re.vsr_upsampling_ratio");
    /**
     * The {@code re.vsr_use_optimized_frame_processing} voip-param.
     */
    public static final VoipParamKey RE_VSR_USE_OPTIMIZED_FRAME_PROCESSING = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_use_optimized_frame_processing");
    /**
     * The {@code re.vsr_width} voip-param.
     */
    public static final VoipParamKey RE_VSR_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "re.vsr_width");
    /**
     * The {@code re.wait_frames_after_last_keyframe_to_stop_resend} voip-param.
     */
    public static final VoipParamKey RE_WAIT_FRAMES_AFTER_LAST_KEYFRAME_TO_STOP_RESEND = new VoipParamKey(VoipParamType.INTEGER, "re.wait_frames_after_last_keyframe_to_stop_resend");
    /**
     * The {@code sframe.enable_audio_dtx_encryption} voip-param.
     */
    public static final VoipParamKey SFRAME_ENABLE_AUDIO_DTX_ENCRYPTION = new VoipParamKey(VoipParamType.INTEGER, "sframe.enable_audio_dtx_encryption");
    /**
     * The {@code sframe.enable_media_codec_types} voip-param.
     */
    public static final VoipParamKey SFRAME_ENABLE_MEDIA_CODEC_TYPES = new VoipParamKey(VoipParamType.INTEGER, "sframe.enable_media_codec_types");
    /**
     * The {@code sframe.enable_sframe} voip-param.
     */
    public static final VoipParamKey SFRAME_ENABLE_SFRAME = new VoipParamKey(VoipParamType.INTEGER, "sframe.enable_sframe");
    /**
     * The {@code sframe.enable_sframe_rx} voip-param.
     */
    public static final VoipParamKey SFRAME_ENABLE_SFRAME_RX = new VoipParamKey(VoipParamType.INTEGER, "sframe.enable_sframe_rx");
    /**
     * The {@code sframe.enable_sframe_tx} voip-param.
     */
    public static final VoipParamKey SFRAME_ENABLE_SFRAME_TX = new VoipParamKey(VoipParamType.INTEGER, "sframe.enable_sframe_tx");
    /**
     * The {@code sframe.sframe_cipher_suite} voip-param.
     */
    public static final VoipParamKey SFRAME_SFRAME_CIPHER_SUITE = new VoipParamKey(VoipParamType.INTEGER, "sframe.sframe_cipher_suite");
    /**
     * The {@code sfu.adjust_br_on_num_participant_diff} voip-param.
     */
    public static final VoipParamKey SFU_ADJUST_BR_ON_NUM_PARTICIPANT_DIFF = new VoipParamKey(VoipParamType.INTEGER, "sfu.adjust_br_on_num_participant_diff");
    /**
     * The {@code sfu.allow_bwe_configuration} voip-param.
     */
    public static final VoipParamKey SFU_ALLOW_BWE_CONFIGURATION = new VoipParamKey(VoipParamType.INTEGER, "sfu.allow_bwe_configuration");
    /**
     * The {@code sfu.audio_bitrate_reserve} voip-param.
     */
    public static final VoipParamKey SFU_AUDIO_BITRATE_RESERVE = new VoipParamKey(VoipParamType.INTEGER, "sfu.audio_bitrate_reserve");
    /**
     * The {@code sfu.audio_fec_plr_coeff} voip-param.
     */
    public static final VoipParamKey SFU_AUDIO_FEC_PLR_COEFF = new VoipParamKey(VoipParamType.FLOAT, "sfu.audio_fec_plr_coeff");
    /**
     * The {@code sfu.audio_reserve_for_muted_participant} voip-param.
     */
    public static final VoipParamKey SFU_AUDIO_RESERVE_FOR_MUTED_PARTICIPANT = new VoipParamKey(VoipParamType.INTEGER, "sfu.audio_reserve_for_muted_participant");
    /**
     * The {@code sfu.bwa_rc_enabled} voip-param.
     */
    public static final VoipParamKey SFU_BWA_RC_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "sfu.bwa_rc_enabled");
    /**
     * The {@code sfu.cc_bwe_slow_ramp_up_ceiling_mode_dl} voip-param.
     */
    public static final VoipParamKey SFU_CC_BWE_SLOW_RAMP_UP_CEILING_MODE_DL = new VoipParamKey(VoipParamType.INTEGER, "sfu.cc_bwe_slow_ramp_up_ceiling_mode_dl");
    /**
     * The {@code sfu.centile_plr_update_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_CENTILE_PLR_UPDATE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.centile_plr_update_interval_ms");
    /**
     * The {@code sfu.change_vid_rc_after_simulcast_ms} voip-param.
     */
    public static final VoipParamKey SFU_CHANGE_VID_RC_AFTER_SIMULCAST_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.change_vid_rc_after_simulcast_ms");
    /**
     * The {@code sfu.disable_sml_nadl_override_when_simulcast} voip-param.
     */
    public static final VoipParamKey SFU_DISABLE_SML_NADL_OVERRIDE_WHEN_SIMULCAST = new VoipParamKey(VoipParamType.INTEGER, "sfu.disable_sml_nadl_override_when_simulcast");
    /**
     * The {@code sfu.disable_zero_warp} voip-param.
     */
    public static final VoipParamKey SFU_DISABLE_ZERO_WARP = new VoipParamKey(VoipParamType.INTEGER, "sfu.disable_zero_warp");
    /**
     * The {@code sfu.do_s_nadl_client_bwa} voip-param.
     */
    public static final VoipParamKey SFU_DO_S_NADL_CLIENT_BWA = new VoipParamKey(VoipParamType.INTEGER, "sfu.do_s_nadl_client_bwa");
    /**
     * The {@code sfu.dont_report_ds_fs_if_never_speak} voip-param.
     */
    public static final VoipParamKey SFU_DONT_REPORT_DS_FS_IF_NEVER_SPEAK = new VoipParamKey(VoipParamType.INTEGER, "sfu.dont_report_ds_fs_if_never_speak");
    /**
     * The {@code sfu.downlink_init_rbwe_bitrate} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_INIT_RBWE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_init_rbwe_bitrate");
    /**
     * The {@code sfu.downlink_init_target_bitrate} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_INIT_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_init_target_bitrate");
    /**
     * The {@code sfu.downlink_min_remote_bwe_lower_bound} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_MIN_REMOTE_BWE_LOWER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_min_remote_bwe_lower_bound");
    /**
     * The {@code sfu.downlink_min_remote_bwe_upper_bound} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_MIN_REMOTE_BWE_UPPER_BOUND = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_min_remote_bwe_upper_bound");
    /**
     * The {@code sfu.downlink_per_peer_min_remote_bwe} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_PER_PEER_MIN_REMOTE_BWE = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_per_peer_min_remote_bwe");
    /**
     * The {@code sfu.downlink_remote_bwe_scheme} voip-param.
     */
    public static final VoipParamKey SFU_DOWNLINK_REMOTE_BWE_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "sfu.downlink_remote_bwe_scheme");
    /**
     * The {@code sfu.dynamic_twcc_max_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_DYNAMIC_TWCC_MAX_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.dynamic_twcc_max_interval_ms");
    /**
     * The {@code sfu.dynamic_twcc_min_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_DYNAMIC_TWCC_MIN_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.dynamic_twcc_min_interval_ms");
    /**
     * The {@code sfu.e2e_plr_congestion_threshold} voip-param.
     */
    public static final VoipParamKey SFU_E2E_PLR_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "sfu.e2e_plr_congestion_threshold");
    /**
     * The {@code sfu.e2e_rtt_congestion_threshold} voip-param.
     */
    public static final VoipParamKey SFU_E2E_RTT_CONGESTION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "sfu.e2e_rtt_congestion_threshold");
    /**
     * The {@code sfu.enable_bwe_reset} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_BWE_RESET = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_bwe_reset");
    /**
     * The {@code sfu.enable_check_for_warp_header} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_CHECK_FOR_WARP_HEADER = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_check_for_warp_header");
    /**
     * The {@code sfu.enable_dynamic_twcc_interval} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_DYNAMIC_TWCC_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_dynamic_twcc_interval");
    /**
     * The {@code sfu.enable_fix_vid_stream_resume_after_direct_bwa} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_FIX_VID_STREAM_RESUME_AFTER_DIRECT_BWA = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_fix_vid_stream_resume_after_direct_bwa");
    /**
     * The {@code sfu.enable_hbh_srtp} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_HBH_SRTP = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_hbh_srtp");
    /**
     * The {@code sfu.enable_historical_relay_verbose_logging} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_HISTORICAL_RELAY_VERBOSE_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_historical_relay_verbose_logging");
    /**
     * The {@code sfu.enable_mcs_fs_peer_device_jid_fix} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_MCS_FS_PEER_DEVICE_JID_FIX = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_mcs_fs_peer_device_jid_fix");
    /**
     * The {@code sfu.enable_nadl_model} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_NADL_MODEL = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_nadl_model");
    /**
     * The {@code sfu.enable_pause_rampup_dl_sbwe} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_PAUSE_RAMPUP_DL_SBWE = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_pause_rampup_dl_sbwe");
    /**
     * The {@code sfu.enable_pause_rampup_ul_sbwe} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_PAUSE_RAMPUP_UL_SBWE = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_pause_rampup_ul_sbwe");
    /**
     * The {@code sfu.enable_periodic_warp_roc_for_hbh} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_PERIODIC_WARP_ROC_FOR_HBH = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_periodic_warp_roc_for_hbh");
    /**
     * The {@code sfu.enable_pr_on_combined_dl_drop} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_PR_ON_COMBINED_DL_DROP = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_pr_on_combined_dl_drop");
    /**
     * The {@code sfu.enable_probation} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_PROBATION = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_probation");
    /**
     * The {@code sfu.enable_simulcast_subscription_throttle_kf} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_SIMULCAST_SUBSCRIPTION_THROTTLE_KF = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_simulcast_subscription_throttle_kf");
    /**
     * The {@code sfu.enable_sml_nadl_override} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_SML_NADL_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_sml_nadl_override");
    /**
     * The {@code sfu.enable_standalone_warp_pr} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_STANDALONE_WARP_PR = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_standalone_warp_pr");
    /**
     * The {@code sfu.enable_standalone_warp_pr_during_multipop} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_STANDALONE_WARP_PR_DURING_MULTIPOP = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_standalone_warp_pr_during_multipop");
    /**
     * The {@code sfu.enable_verbose_ceiling_stats} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_VERBOSE_CEILING_STATS = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_verbose_ceiling_stats");
    /**
     * The {@code sfu.enable_warp_approx_peer_ul_plr} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_APPROX_PEER_UL_PLR = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_approx_peer_ul_plr");
    /**
     * The {@code sfu.enable_warp_mi_remaining_len_check} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_MI_REMAINING_LEN_CHECK = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_mi_remaining_len_check");
    /**
     * The {@code sfu.enable_warp_pr_bg_flag} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_PR_BG_FLAG = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_pr_bg_flag");
    /**
     * The {@code sfu.enable_warp_pr_cam_flag} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_PR_CAM_FLAG = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_pr_cam_flag");
    /**
     * The {@code sfu.enable_warp_pr_dl_bwe_pp_report} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_PR_DL_BWE_PP_REPORT = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_pr_dl_bwe_pp_report");
    /**
     * The {@code sfu.enable_warp_pr_speaker_view_flag} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_PR_SPEAKER_VIEW_FLAG = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_pr_speaker_view_flag");
    /**
     * The {@code sfu.enable_warp_pr_ss_flag} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_PR_SS_FLAG = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_pr_ss_flag");
    /**
     * The {@code sfu.enable_warp_roc_for_hbh} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_ROC_FOR_HBH = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_roc_for_hbh");
    /**
     * The {@code sfu.enable_warp_rtx_indication} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_RTX_INDICATION = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_rtx_indication");
    /**
     * The {@code sfu.enable_warp_rtx_indicator_fix} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_RTX_INDICATOR_FIX = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_rtx_indicator_fix");
    /**
     * The {@code sfu.enable_warp_sfu} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_SFU = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_sfu");
    /**
     * The {@code sfu.enable_warp_sfu_mock_brc} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_SFU_MOCK_BRC = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_sfu_mock_brc");
    /**
     * The {@code sfu.enable_warp_sfu_simulcast} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_SFU_SIMULCAST = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_sfu_simulcast");
    /**
     * The {@code sfu.enable_warp_twcc_bw_deduction} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_TWCC_BW_DEDUCTION = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_twcc_bw_deduction");
    /**
     * The {@code sfu.enable_warp_twsn} voip-param.
     */
    public static final VoipParamKey SFU_ENABLE_WARP_TWSN = new VoipParamKey(VoipParamType.INTEGER, "sfu.enable_warp_twsn");
    /**
     * The {@code sfu.fast_participant_report_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_FAST_PARTICIPANT_REPORT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.fast_participant_report_interval_ms");
    /**
     * The {@code sfu.fast_pr_peer_downlink_plr_threshold} voip-param.
     */
    public static final VoipParamKey SFU_FAST_PR_PEER_DOWNLINK_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "sfu.fast_pr_peer_downlink_plr_threshold");
    /**
     * The {@code sfu.fast_pr_self_uplink_plr_threshold} voip-param.
     */
    public static final VoipParamKey SFU_FAST_PR_SELF_UPLINK_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "sfu.fast_pr_self_uplink_plr_threshold");
    /**
     * The {@code sfu.force_sending_server_bwe_update} voip-param.
     */
    public static final VoipParamKey SFU_FORCE_SENDING_SERVER_BWE_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.force_sending_server_bwe_update");
    /**
     * The {@code sfu.force_worst_e2e_plr_fec_in_multi_pop} voip-param.
     */
    public static final VoipParamKey SFU_FORCE_WORST_E2E_PLR_FEC_IN_MULTI_POP = new VoipParamKey(VoipParamType.INTEGER, "sfu.force_worst_e2e_plr_fec_in_multi_pop");
    /**
     * The {@code sfu.fs_enable_sfu_e2e_tx_pkt_loss} voip-param.
     */
    public static final VoipParamKey SFU_FS_ENABLE_SFU_E2E_TX_PKT_LOSS = new VoipParamKey(VoipParamType.INTEGER, "sfu.fs_enable_sfu_e2e_tx_pkt_loss");
    /**
     * The {@code sfu.hbh_fec_loss_overhead} voip-param.
     */
    public static final VoipParamKey SFU_HBH_FEC_LOSS_OVERHEAD = new VoipParamKey(VoipParamType.INTEGER, "sfu.hbh_fec_loss_overhead");
    /**
     * The {@code sfu.hbh_fec_loss_threshold_window_ms} voip-param.
     */
    public static final VoipParamKey SFU_HBH_FEC_LOSS_THRESHOLD_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.hbh_fec_loss_threshold_window_ms");
    /**
     * The {@code sfu.historical_relay_hash_table_size} voip-param.
     */
    public static final VoipParamKey SFU_HISTORICAL_RELAY_HASH_TABLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "sfu.historical_relay_hash_table_size");
    /**
     * The {@code sfu.historical_relay_latency_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_HISTORICAL_RELAY_LATENCY_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.historical_relay_latency_threshold_ms");
    /**
     * The {@code sfu.hybrid_sbwa_send_at_least_one_stream} voip-param.
     */
    public static final VoipParamKey SFU_HYBRID_SBWA_SEND_AT_LEAST_ONE_STREAM = new VoipParamKey(VoipParamType.INTEGER, "sfu.hybrid_sbwa_send_at_least_one_stream");
    /**
     * The {@code sfu.hybrid_update_freq_msec} voip-param.
     */
    public static final VoipParamKey SFU_HYBRID_UPDATE_FREQ_MSEC = new VoipParamKey(VoipParamType.INTEGER, "sfu.hybrid_update_freq_msec");
    /**
     * The {@code sfu.ignore_sbwa_on_participant_mismatch_since_segment_start} voip-param.
     */
    public static final VoipParamKey SFU_IGNORE_SBWA_ON_PARTICIPANT_MISMATCH_SINCE_SEGMENT_START = new VoipParamKey(VoipParamType.INTEGER, "sfu.ignore_sbwa_on_participant_mismatch_since_segment_start");
    /**
     * The {@code sfu.ignore_warp_mi_failures} voip-param.
     */
    public static final VoipParamKey SFU_IGNORE_WARP_MI_FAILURES = new VoipParamKey(VoipParamType.INTEGER, "sfu.ignore_warp_mi_failures");
    /**
     * The {@code sfu.imbalanced_plr_rtt_threshold} voip-param.
     */
    public static final VoipParamKey SFU_IMBALANCED_PLR_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "sfu.imbalanced_plr_rtt_threshold");
    /**
     * The {@code sfu.imm_send_historical_relay_latencies} voip-param.
     */
    public static final VoipParamKey SFU_IMM_SEND_HISTORICAL_RELAY_LATENCIES = new VoipParamKey(VoipParamType.INTEGER, "sfu.imm_send_historical_relay_latencies");
    /**
     * The {@code sfu.loss_threshold_for_hbh_fec} voip-param.
     */
    public static final VoipParamKey SFU_LOSS_THRESHOLD_FOR_HBH_FEC = new VoipParamKey(VoipParamType.INTEGER, "sfu.loss_threshold_for_hbh_fec");
    /**
     * The {@code sfu.min_passive_rx_pkt_to_flip} voip-param.
     */
    public static final VoipParamKey SFU_MIN_PASSIVE_RX_PKT_TO_FLIP = new VoipParamKey(VoipParamType.INTEGER, "sfu.min_passive_rx_pkt_to_flip");
    /**
     * The {@code sfu.msec_to_wait_before_client_bwa_fallback} voip-param.
     */
    public static final VoipParamKey SFU_MSEC_TO_WAIT_BEFORE_CLIENT_BWA_FALLBACK = new VoipParamKey(VoipParamType.INTEGER, "sfu.msec_to_wait_before_client_bwa_fallback");
    /**
     * The {@code sfu.nadl_audio_dup_q_thresh} voip-param.
     */
    public static final VoipParamKey SFU_NADL_AUDIO_DUP_Q_THRESH = new VoipParamKey(VoipParamType.FLOAT, "sfu.nadl_audio_dup_q_thresh");
    /**
     * The {@code sfu.nadl_inference_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_NADL_INFERENCE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.nadl_inference_interval_ms");
    /**
     * The {@code sfu.nadl_model_load_max_retry} voip-param.
     */
    public static final VoipParamKey SFU_NADL_MODEL_LOAD_MAX_RETRY = new VoipParamKey(VoipParamType.INTEGER, "sfu.nadl_model_load_max_retry");
    /**
     * The {@code sfu.nadl_model_load_retry_interval} voip-param.
     */
    public static final VoipParamKey SFU_NADL_MODEL_LOAD_RETRY_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "sfu.nadl_model_load_retry_interval");
    /**
     * The {@code sfu.nadl_model_load_trigger_point} voip-param.
     */
    public static final VoipParamKey SFU_NADL_MODEL_LOAD_TRIGGER_POINT = new VoipParamKey(VoipParamType.INTEGER, "sfu.nadl_model_load_trigger_point");
    /**
     * The {@code sfu.nadl_model_name} voip-param.
     */
    public static final VoipParamKey SFU_NADL_MODEL_NAME = new VoipParamKey(VoipParamType.STRING, "sfu.nadl_model_name");
    /**
     * The {@code sfu.nadl_model_only_load_max_retry} voip-param.
     */
    public static final VoipParamKey SFU_NADL_MODEL_ONLY_LOAD_MAX_RETRY = new VoipParamKey(VoipParamType.INTEGER, "sfu.nadl_model_only_load_max_retry");
    /**
     * The {@code sfu.no_conn_paused_vid} voip-param.
     */
    public static final VoipParamKey SFU_NO_CONN_PAUSED_VID = new VoipParamKey(VoipParamType.INTEGER, "sfu.no_conn_paused_vid");
    /**
     * The {@code sfu.on_relay_change_detect} voip-param.
     */
    public static final VoipParamKey SFU_ON_RELAY_CHANGE_DETECT = new VoipParamKey(VoipParamType.INTEGER, "sfu.on_relay_change_detect");
    /**
     * The {@code sfu.participant_report_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_PARTICIPANT_REPORT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.participant_report_interval_ms");
    /**
     * The {@code sfu.peer_dl_plr_centile} voip-param.
     */
    public static final VoipParamKey SFU_PEER_DL_PLR_CENTILE = new VoipParamKey(VoipParamType.INTEGER, "sfu.peer_dl_plr_centile");
    /**
     * The {@code sfu.plr_aggregation_scheme} voip-param.
     */
    public static final VoipParamKey SFU_PLR_AGGREGATION_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "sfu.plr_aggregation_scheme");
    /**
     * The {@code sfu.plr_reset_fix} voip-param.
     */
    public static final VoipParamKey SFU_PLR_RESET_FIX = new VoipParamKey(VoipParamType.INTEGER, "sfu.plr_reset_fix");
    /**
     * The {@code sfu.plr_wt} voip-param.
     */
    public static final VoipParamKey SFU_PLR_WT = new VoipParamKey(VoipParamType.FLOAT, "sfu.plr_wt");
    /**
     * The {@code sfu.probation_min_sequential} voip-param.
     */
    public static final VoipParamKey SFU_PROBATION_MIN_SEQUENTIAL = new VoipParamKey(VoipParamType.INTEGER, "sfu.probation_min_sequential");
    /**
     * The {@code sfu.rc_data_request_list_derive_from_speaker_info} voip-param.
     */
    public static final VoipParamKey SFU_RC_DATA_REQUEST_LIST_DERIVE_FROM_SPEAKER_INFO = new VoipParamKey(VoipParamType.INTEGER, "sfu.rc_data_request_list_derive_from_speaker_info");
    /**
     * The {@code sfu.rc_data_request_list_use_rx_subscription_info} voip-param.
     */
    public static final VoipParamKey SFU_RC_DATA_REQUEST_LIST_USE_RX_SUBSCRIPTION_INFO = new VoipParamKey(VoipParamType.INTEGER, "sfu.rc_data_request_list_use_rx_subscription_info");
    /**
     * The {@code sfu.reconfig_rbwe_min_max_bitrate_for_av_upgrade} voip-param.
     */
    public static final VoipParamKey SFU_RECONFIG_RBWE_MIN_MAX_BITRATE_FOR_AV_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "sfu.reconfig_rbwe_min_max_bitrate_for_av_upgrade");
    /**
     * The {@code sfu.report_sml_nadl_fs} voip-param.
     */
    public static final VoipParamKey SFU_REPORT_SML_NADL_FS = new VoipParamKey(VoipParamType.INTEGER, "sfu.report_sml_nadl_fs");
    /**
     * The {@code sfu.reserve_warp_in_vid_enc_mtu} voip-param.
     */
    public static final VoipParamKey SFU_RESERVE_WARP_IN_VID_ENC_MTU = new VoipParamKey(VoipParamType.INTEGER, "sfu.reserve_warp_in_vid_enc_mtu");
    /**
     * The {@code sfu.reset_enable_warp_sfu_simulcast} voip-param.
     */
    public static final VoipParamKey SFU_RESET_ENABLE_WARP_SFU_SIMULCAST = new VoipParamKey(VoipParamType.INTEGER, "sfu.reset_enable_warp_sfu_simulcast");
    /**
     * The {@code sfu.reset_hbh_tx_srtp} voip-param.
     */
    public static final VoipParamKey SFU_RESET_HBH_TX_SRTP = new VoipParamKey(VoipParamType.INTEGER, "sfu.reset_hbh_tx_srtp");
    /**
     * The {@code sfu.reset_rbwe_for_av_upgrade} voip-param.
     */
    public static final VoipParamKey SFU_RESET_RBWE_FOR_AV_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "sfu.reset_rbwe_for_av_upgrade");
    /**
     * The {@code sfu.rtt_min_ema_alpha} voip-param.
     */
    public static final VoipParamKey SFU_RTT_MIN_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "sfu.rtt_min_ema_alpha");
    /**
     * The {@code sfu.rtt_normalize_factor} voip-param.
     */
    public static final VoipParamKey SFU_RTT_NORMALIZE_FACTOR = new VoipParamKey(VoipParamType.INTEGER, "sfu.rtt_normalize_factor");
    /**
     * The {@code sfu.rx_pkt_cache_capacity} voip-param.
     */
    public static final VoipParamKey SFU_RX_PKT_CACHE_CAPACITY = new VoipParamKey(VoipParamType.INTEGER, "sfu.rx_pkt_cache_capacity");
    /**
     * The {@code sfu.rx_pkt_cache_stale_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_RX_PKT_CACHE_STALE_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.rx_pkt_cache_stale_threshold_ms");
    /**
     * The {@code sfu.s_nadl_bwa_high_bwe_kbps_threshold} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_BWA_HIGH_BWE_KBPS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_bwa_high_bwe_kbps_threshold");
    /**
     * The {@code sfu.s_nadl_bwa_low_bwe_kbps_threshold} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_BWA_LOW_BWE_KBPS_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_bwa_low_bwe_kbps_threshold");
    /**
     * The {@code sfu.s_nadl_max_reduction_percentage} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_MAX_REDUCTION_PERCENTAGE = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_max_reduction_percentage");
    /**
     * The {@code sfu.s_nadl_min_bandwidth_floor_kbps} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_MIN_BANDWIDTH_FLOOR_KBPS = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_min_bandwidth_floor_kbps");
    /**
     * The {@code sfu.s_nadl_nr_fec_plr_threshold} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_NR_FEC_PLR_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "sfu.s_nadl_nr_fec_plr_threshold");
    /**
     * The {@code sfu.s_nadl_nr_fec_reserve_rate} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_NR_FEC_RESERVE_RATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_nr_fec_reserve_rate");
    /**
     * The {@code sfu.s_nadl_plr_window_count} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_PLR_WINDOW_COUNT = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_plr_window_count");
    /**
     * The {@code sfu.s_nadl_plr_window_length_ms} voip-param.
     */
    public static final VoipParamKey SFU_S_NADL_PLR_WINDOW_LENGTH_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.s_nadl_plr_window_length_ms");
    /**
     * The {@code sfu.sbwa_result_logging_freq_msec} voip-param.
     */
    public static final VoipParamKey SFU_SBWA_RESULT_LOGGING_FREQ_MSEC = new VoipParamKey(VoipParamType.INTEGER, "sfu.sbwa_result_logging_freq_msec");
    /**
     * The {@code sfu.send_bwe_configuration_duration_ms} voip-param.
     */
    public static final VoipParamKey SFU_SEND_BWE_CONFIGURATION_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.send_bwe_configuration_duration_ms");
    /**
     * The {@code sfu.send_bwe_configuration_interval_ms} voip-param.
     */
    public static final VoipParamKey SFU_SEND_BWE_CONFIGURATION_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.send_bwe_configuration_interval_ms");
    /**
     * The {@code sfu.set_muted_participant_audio_reserve} voip-param.
     */
    public static final VoipParamKey SFU_SET_MUTED_PARTICIPANT_AUDIO_RESERVE = new VoipParamKey(VoipParamType.INTEGER, "sfu.set_muted_participant_audio_reserve");
    /**
     * The {@code sfu.sfu_delay_since_last_pr_discard_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_SFU_DELAY_SINCE_LAST_PR_DISCARD_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.sfu_delay_since_last_pr_discard_threshold_ms");
    /**
     * The {@code sfu.sfu_downlink_init_bwe_on_high_end_android} voip-param.
     */
    public static final VoipParamKey SFU_SFU_DOWNLINK_INIT_BWE_ON_HIGH_END_ANDROID = new VoipParamKey(VoipParamType.INTEGER, "sfu.sfu_downlink_init_bwe_on_high_end_android");
    /**
     * The {@code sfu.sfu_rtt_adjust_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_SFU_RTT_ADJUST_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.sfu_rtt_adjust_threshold_ms");
    /**
     * The {@code sfu.sfu_rtt_discard_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_SFU_RTT_DISCARD_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.sfu_rtt_discard_threshold_ms");
    /**
     * The {@code sfu.sfu_vid_rc_result_event_publish_flag} voip-param.
     */
    public static final VoipParamKey SFU_SFU_VID_RC_RESULT_EVENT_PUBLISH_FLAG = new VoipParamKey(VoipParamType.INTEGER, "sfu.sfu_vid_rc_result_event_publish_flag");
    /**
     * The {@code sfu.simulcast_nack_race_condition_check} voip-param.
     */
    public static final VoipParamKey SFU_SIMULCAST_NACK_RACE_CONDITION_CHECK = new VoipParamKey(VoipParamType.INTEGER, "sfu.simulcast_nack_race_condition_check");
    /**
     * The {@code sfu.sml_nadl_stale_behavior} voip-param.
     */
    public static final VoipParamKey SFU_SML_NADL_STALE_BEHAVIOR = new VoipParamKey(VoipParamType.INTEGER, "sfu.sml_nadl_stale_behavior");
    /**
     * The {@code sfu.sml_nadl_staleness_threshold_ms} voip-param.
     */
    public static final VoipParamKey SFU_SML_NADL_STALENESS_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.sml_nadl_staleness_threshold_ms");
    /**
     * The {@code sfu.strip_warp_header_for_packets_on_p2p} voip-param.
     */
    public static final VoipParamKey SFU_STRIP_WARP_HEADER_FOR_PACKETS_ON_P2P = new VoipParamKey(VoipParamType.INTEGER, "sfu.strip_warp_header_for_packets_on_p2p");
    /**
     * The {@code sfu.sync_dl_plr_calc_to_pr_send} voip-param.
     */
    public static final VoipParamKey SFU_SYNC_DL_PLR_CALC_TO_PR_SEND = new VoipParamKey(VoipParamType.INTEGER, "sfu.sync_dl_plr_calc_to_pr_send");
    /**
     * The {@code sfu.tx_participant_report_on_audio} voip-param.
     */
    public static final VoipParamKey SFU_TX_PARTICIPANT_REPORT_ON_AUDIO = new VoipParamKey(VoipParamType.INTEGER, "sfu.tx_participant_report_on_audio");
    /**
     * The {@code sfu.update_rbwe_options_for_av_upgrade} voip-param.
     */
    public static final VoipParamKey SFU_UPDATE_RBWE_OPTIONS_FOR_AV_UPGRADE = new VoipParamKey(VoipParamType.INTEGER, "sfu.update_rbwe_options_for_av_upgrade");
    /**
     * The {@code sfu.uplink_init_rbwe_bitrate} voip-param.
     */
    public static final VoipParamKey SFU_UPLINK_INIT_RBWE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.uplink_init_rbwe_bitrate");
    /**
     * The {@code sfu.uplink_init_rbwe_duration_ms} voip-param.
     */
    public static final VoipParamKey SFU_UPLINK_INIT_RBWE_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.uplink_init_rbwe_duration_ms");
    /**
     * The {@code sfu.uplink_init_target_bitrate} voip-param.
     */
    public static final VoipParamKey SFU_UPLINK_INIT_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "sfu.uplink_init_target_bitrate");
    /**
     * The {@code sfu.uplink_min_rbwe} voip-param.
     */
    public static final VoipParamKey SFU_UPLINK_MIN_RBWE = new VoipParamKey(VoipParamType.INTEGER, "sfu.uplink_min_rbwe");
    /**
     * The {@code sfu.use_bwa_info_log} voip-param.
     */
    public static final VoipParamKey SFU_USE_BWA_INFO_LOG = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_bwa_info_log");
    /**
     * The {@code sfu.use_hybrid_server_bwa} voip-param.
     */
    public static final VoipParamKey SFU_USE_HYBRID_SERVER_BWA = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_hybrid_server_bwa");
    /**
     * The {@code sfu.use_real_e2e_peer_stats_fix} voip-param.
     */
    public static final VoipParamKey SFU_USE_REAL_E2E_PEER_STATS_FIX = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_real_e2e_peer_stats_fix");
    /**
     * The {@code sfu.use_secondary_rbwe_without_null_check} voip-param.
     */
    public static final VoipParamKey SFU_USE_SECONDARY_RBWE_WITHOUT_NULL_CHECK = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_secondary_rbwe_without_null_check");
    /**
     * The {@code sfu.use_server_bwa} voip-param.
     */
    public static final VoipParamKey SFU_USE_SERVER_BWA = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_server_bwa");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_for_aud_rc} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_FOR_AUD_RC = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_for_aud_rc");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_for_aud_stream} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_FOR_AUD_STREAM = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_for_aud_stream");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_for_rtt_congestion} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_FOR_RTT_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_for_rtt_congestion");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_for_vid_rc} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_FOR_VID_RC = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_for_vid_rc");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_for_vid_stream} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_FOR_VID_STREAM = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_for_vid_stream");
    /**
     * The {@code sfu.use_xpop_e2e_rtt_stats} voip-param.
     */
    public static final VoipParamKey SFU_USE_XPOP_E2E_RTT_STATS = new VoipParamKey(VoipParamType.INTEGER, "sfu.use_xpop_e2e_rtt_stats");
    /**
     * The {@code sfu.verbose_logging_freq_msec} voip-param.
     */
    public static final VoipParamKey SFU_VERBOSE_LOGGING_FREQ_MSEC = new VoipParamKey(VoipParamType.INTEGER, "sfu.verbose_logging_freq_msec");
    /**
     * The {@code sfu.warp_dl_bwe_to_video_stream_adjustment_factor} voip-param.
     */
    public static final VoipParamKey SFU_WARP_DL_BWE_TO_VIDEO_STREAM_ADJUSTMENT_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "sfu.warp_dl_bwe_to_video_stream_adjustment_factor");
    /**
     * The {@code sfu.warp_early_pr_threshold} voip-param.
     */
    public static final VoipParamKey SFU_WARP_EARLY_PR_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_early_pr_threshold");
    /**
     * The {@code sfu.warp_mcs_stale_state_fix_enable} voip-param.
     */
    public static final VoipParamKey SFU_WARP_MCS_STALE_STATE_FIX_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_mcs_stale_state_fix_enable");
    /**
     * The {@code sfu.warp_seq_num_check_delay_ms} voip-param.
     */
    public static final VoipParamKey SFU_WARP_SEQ_NUM_CHECK_DELAY_MS = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_seq_num_check_delay_ms");
    /**
     * The {@code sfu.warp_seq_num_scheme} voip-param.
     */
    public static final VoipParamKey SFU_WARP_SEQ_NUM_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_seq_num_scheme");
    /**
     * The {@code sfu.warp_sn_max_disorder} voip-param.
     */
    public static final VoipParamKey SFU_WARP_SN_MAX_DISORDER = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_sn_max_disorder");
    /**
     * The {@code sfu.warp_sn_max_dropout} voip-param.
     */
    public static final VoipParamKey SFU_WARP_SN_MAX_DROPOUT = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_sn_max_dropout");
    /**
     * The {@code sfu.warp_transport_feedback_clear_when_disabled} voip-param.
     */
    public static final VoipParamKey SFU_WARP_TRANSPORT_FEEDBACK_CLEAR_WHEN_DISABLED = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_transport_feedback_clear_when_disabled");
    /**
     * The {@code sfu.warp_transport_feedback_send_period_msec} voip-param.
     */
    public static final VoipParamKey SFU_WARP_TRANSPORT_FEEDBACK_SEND_PERIOD_MSEC = new VoipParamKey(VoipParamType.INTEGER, "sfu.warp_transport_feedback_send_period_msec");
    /**
     * The {@code test.bucket_id_list} voip-param.
     */
    public static final VoipParamKey TEST_BUCKET_ID_LIST = new VoipParamKey(VoipParamType.STRING, "test.bucket_id_list");
    /**
     * The {@code test.eligible_bucket_id_list} voip-param.
     */
    public static final VoipParamKey TEST_ELIGIBLE_BUCKET_ID_LIST = new VoipParamKey(VoipParamType.STRING, "test.eligible_bucket_id_list");
    /**
     * The {@code test.name} voip-param.
     */
    public static final VoipParamKey TEST_NAME = new VoipParamKey(VoipParamType.STRING, "test.name");
    /**
     * The {@code traffic_shaper.audio_priority} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_AUDIO_PRIORITY = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.audio_priority");
    /**
     * The {@code traffic_shaper.drain_on_pause} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_DRAIN_ON_PAUSE = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.drain_on_pause");
    /**
     * The {@code traffic_shaper.drain_on_resume} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_DRAIN_ON_RESUME = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.drain_on_resume");
    /**
     * The {@code traffic_shaper.enable} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.enable");
    /**
     * The {@code traffic_shaper.enable_for_screen_sharer_only} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_ENABLE_FOR_SCREEN_SHARER_ONLY = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.enable_for_screen_sharer_only");
    /**
     * The {@code traffic_shaper.enable_verbose_paced_pkt_logs} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_ENABLE_VERBOSE_PACED_PKT_LOGS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.enable_verbose_paced_pkt_logs");
    /**
     * The {@code traffic_shaper.max_packets} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_MAX_PACKETS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.max_packets");
    /**
     * The {@code traffic_shaper.pacer_min_bwe_kbps_for_packet_pairs} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_PACER_MIN_BWE_KBPS_FOR_PACKET_PAIRS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.pacer_min_bwe_kbps_for_packet_pairs");
    /**
     * The {@code traffic_shaper.pacer_percent_packet_pairs} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_PACER_PERCENT_PACKET_PAIRS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.pacer_percent_packet_pairs");
    /**
     * The {@code traffic_shaper.pacing_factor} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_PACING_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "traffic_shaper.pacing_factor");
    /**
     * The {@code traffic_shaper.send_video_fec_immediately} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_SEND_VIDEO_FEC_IMMEDIATELY = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.send_video_fec_immediately");
    /**
     * The {@code traffic_shaper.shaper_stats_log_interval_ms} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_SHAPER_STATS_LOG_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.shaper_stats_log_interval_ms");
    /**
     * The {@code traffic_shaper.shaper_time_until_next_process_ms} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_SHAPER_TIME_UNTIL_NEXT_PROCESS_MS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.shaper_time_until_next_process_ms");
    /**
     * The {@code traffic_shaper.video_fec_on_pacer_egress} voip-param.
     */
    public static final VoipParamKey TRAFFIC_SHAPER_VIDEO_FEC_ON_PACER_EGRESS = new VoipParamKey(VoipParamType.INTEGER, "traffic_shaper.video_fec_on_pacer_egress");
    /**
     * The {@code transport_rtx.audio_max_cache_size} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_AUDIO_MAX_CACHE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.audio_max_cache_size");
    /**
     * The {@code transport_rtx.audio_max_rtx_resends} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_AUDIO_MAX_RTX_RESENDS = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.audio_max_rtx_resends");
    /**
     * The {@code transport_rtx.enable_seq_wraparound_fix} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_ENABLE_SEQ_WRAPAROUND_FIX = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.enable_seq_wraparound_fix");
    /**
     * The {@code transport_rtx.enable_transport_rtx_enc} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_ENABLE_TRANSPORT_RTX_ENC = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.enable_transport_rtx_enc");
    /**
     * The {@code transport_rtx.enable_verbose_logging} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_ENABLE_VERBOSE_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.enable_verbose_logging");
    /**
     * The {@code transport_rtx.hash_table_size} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_HASH_TABLE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.hash_table_size");
    /**
     * The {@code transport_rtx.video_max_cache_size} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_VIDEO_MAX_CACHE_SIZE = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.video_max_cache_size");
    /**
     * The {@code transport_rtx.video_max_rtx_resends} voip-param.
     */
    public static final VoipParamKey TRANSPORT_RTX_VIDEO_MAX_RTX_RESENDS = new VoipParamKey(VoipParamType.INTEGER, "transport_rtx.video_max_rtx_resends");
    /**
     * The {@code transport_splitter.enable_hbh_fec_rx} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SPLITTER_ENABLE_HBH_FEC_RX = new VoipParamKey(VoipParamType.INTEGER, "transport_splitter.enable_hbh_fec_rx");
    /**
     * The {@code transport_splitter.enable_transport_attach_fix} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SPLITTER_ENABLE_TRANSPORT_ATTACH_FIX = new VoipParamKey(VoipParamType.INTEGER, "transport_splitter.enable_transport_attach_fix");
    /**
     * The {@code transport_splitter.get_transport_stats} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SPLITTER_GET_TRANSPORT_STATS = new VoipParamKey(VoipParamType.INTEGER, "transport_splitter.get_transport_stats");
    /**
     * The {@code transport_splitter.sample_interval_ms} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SPLITTER_SAMPLE_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "transport_splitter.sample_interval_ms");
    /**
     * The {@code transport_srtp.cgu_metrics_timeout_ms} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_CGU_METRICS_TIMEOUT_MS = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.cgu_metrics_timeout_ms");
    /**
     * The {@code transport_srtp.enable_crypto_scope} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_CRYPTO_SCOPE = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_crypto_scope");
    /**
     * The {@code transport_srtp.enable_fix_for_tx_buf_release} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_FIX_FOR_TX_BUF_RELEASE = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_fix_for_tx_buf_release");
    /**
     * The {@code transport_srtp.enable_hbh_fec_destroy_race_fix} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_HBH_FEC_DESTROY_RACE_FIX = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_hbh_fec_destroy_race_fix");
    /**
     * The {@code transport_srtp.enable_hbh_srtp_tx_fix} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_HBH_SRTP_TX_FIX = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_hbh_srtp_tx_fix");
    /**
     * The {@code transport_srtp.enable_srtp_hbh_fec_rx} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_SRTP_HBH_FEC_RX = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_srtp_hbh_fec_rx");
    /**
     * The {@code transport_srtp.enable_srtp_hbh_srtp} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_SRTP_HBH_SRTP = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_srtp_hbh_srtp");
    /**
     * The {@code transport_srtp.enable_srtp_hbh_srtp_set_index_bitmasks} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_SRTP_HBH_SRTP_SET_INDEX_BITMASKS = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_srtp_hbh_srtp_set_index_bitmasks");
    /**
     * The {@code transport_srtp.enable_verbose_logging} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_ENABLE_VERBOSE_LOGGING = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.enable_verbose_logging");
    /**
     * The {@code transport_srtp.hbh_fec_rx_algorithm} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_HBH_FEC_RX_ALGORITHM = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.hbh_fec_rx_algorithm");
    /**
     * The {@code transport_srtp.init_stats_window_ms} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_INIT_STATS_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.init_stats_window_ms");
    /**
     * The {@code transport_srtp.old_key_valid_threshold} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_OLD_KEY_VALID_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.old_key_valid_threshold");
    /**
     * The {@code transport_srtp.treat_recovered_packet_as_normal_packet} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_TREAT_RECOVERED_PACKET_AS_NORMAL_PACKET = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.treat_recovered_packet_as_normal_packet");
    /**
     * The {@code transport_srtp.update_srtp_tx_delay} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_UPDATE_SRTP_TX_DELAY = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.update_srtp_tx_delay");
    /**
     * The {@code transport_srtp.update_srtp_tx_delay_rekey_master} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_UPDATE_SRTP_TX_DELAY_REKEY_MASTER = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.update_srtp_tx_delay_rekey_master");
    /**
     * The {@code transport_srtp.use_per_ssrc_tx_key} voip-param.
     */
    public static final VoipParamKey TRANSPORT_SRTP_USE_PER_SSRC_TX_KEY = new VoipParamKey(VoipParamType.INTEGER, "transport_srtp.use_per_ssrc_tx_key");
    /**
     * The {@code uaqc.enable_gc_uaqc} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_GC_UAQC = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_gc_uaqc");
    /**
     * The {@code uaqc.enable_gc_uaqc_reset_on_gc_transition} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_GC_UAQC_RESET_ON_GC_TRANSITION = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_gc_uaqc_reset_on_gc_transition");
    /**
     * The {@code uaqc.enable_receive_peer_uaqc_state} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_RECEIVE_PEER_UAQC_STATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_receive_peer_uaqc_state");
    /**
     * The {@code uaqc.enable_self_ul_rbwe} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_SELF_UL_RBWE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_self_ul_rbwe");
    /**
     * The {@code uaqc.enable_send_uaqc_state_to_peer} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_SEND_UAQC_STATE_TO_PEER = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_send_uaqc_state_to_peer");
    /**
     * The {@code uaqc.enable_sending_bwe_rst_when_fpp_changes} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_SENDING_BWE_RST_WHEN_FPP_CHANGES = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_sending_bwe_rst_when_fpp_changes");
    /**
     * The {@code uaqc.enable_uaqc} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc");
    /**
     * The {@code uaqc.enable_uaqc_in_vid} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_IN_VID = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_in_vid");
    /**
     * The {@code uaqc.enable_uaqc_level_n_strategy} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_LEVEL_N_STRATEGY = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_level_n_strategy");
    /**
     * The {@code uaqc.enable_uaqc_lossy_state} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_LOSSY_STATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_lossy_state");
    /**
     * The {@code uaqc.enable_uaqc_no_rtcp_check} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_NO_RTCP_CHECK = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_no_rtcp_check");
    /**
     * The {@code uaqc.enable_uaqc_peer_neteq} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_PEER_NETEQ = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_peer_neteq");
    /**
     * The {@code uaqc.enable_uaqc_plr_ema} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_PLR_EMA = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_plr_ema");
    /**
     * The {@code uaqc.enable_uaqc_ts_logger} voip-param.
     */
    public static final VoipParamKey UAQC_ENABLE_UAQC_TS_LOGGER = new VoipParamKey(VoipParamType.INTEGER, "uaqc.enable_uaqc_ts_logger");
    /**
     * The {@code uaqc.uaqc_audio_flag_bitmap} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_AUDIO_FLAG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_audio_flag_bitmap");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_fpp");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_mlow_dtx_hangover_ms");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_oob_factor");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_opus_vad_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_plr_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_PLR_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_plr_lower_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_plr_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_PLR_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_plr_upper_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_remb_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_REMB_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_remb_lower_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_remb_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_REMB_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_remb_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_remb_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_REMB_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_remb_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_remb_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_REMB_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_remb_upper_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_rtt_ratio_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_rtt_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_rtt_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_rtt_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_RTT_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_rtt_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_rtt_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_RTT_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_bandwidth_managed_rtt_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_target_bitrate");
    /**
     * The {@code uaqc.uaqc_bandwidth_managed_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BANDWIDTH_MANAGED_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bandwidth_managed_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_bw_managed_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_BW_MANAGED_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_bw_managed_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_codec_flag_bitmap} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_CODEC_FLAG_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_codec_flag_bitmap");
    /**
     * The {@code uaqc.uaqc_drain_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_fpp");
    /**
     * The {@code uaqc.uaqc_drain_high_rtt_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_HIGH_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_high_rtt_threshold");
    /**
     * The {@code uaqc.uaqc_drain_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_drain_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_oob_factor");
    /**
     * The {@code uaqc.uaqc_drain_plr_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_PLR_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_plr_lower_threshold");
    /**
     * The {@code uaqc.uaqc_drain_plr_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_PLR_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_plr_upper_threshold");
    /**
     * The {@code uaqc.uaqc_drain_remb_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_REMB_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_remb_lower_threshold");
    /**
     * The {@code uaqc.uaqc_drain_remb_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_REMB_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_remb_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_drain_remb_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_REMB_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_remb_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_drain_remb_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_REMB_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_remb_upper_threshold");
    /**
     * The {@code uaqc.uaqc_drain_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_rtt_ratio_threshold");
    /**
     * The {@code uaqc.uaqc_drain_rtt_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_RTT_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_rtt_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_drain_rtt_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_RTT_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_drain_rtt_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_drain_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_target_bitrate");
    /**
     * The {@code uaqc.uaqc_drain_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_drain_ultra_low_opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_DRAIN_ULTRA_LOW_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_drain_ultra_low_opus_vad_threshold");
    /**
     * The {@code uaqc.uaqc_enable_probing_setting_fix} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ENABLE_PROBING_SETTING_FIX = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_enable_probing_setting_fix");
    /**
     * The {@code uaqc.uaqc_enable_redundancy_at_drain} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ENABLE_REDUNDANCY_AT_DRAIN = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_enable_redundancy_at_drain");
    /**
     * The {@code uaqc.uaqc_enable_redundancy_at_probing} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ENABLE_REDUNDANCY_AT_PROBING = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_enable_redundancy_at_probing");
    /**
     * The {@code uaqc.uaqc_enable_redundancy_at_ultra_low} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ENABLE_REDUNDANCY_AT_ULTRA_LOW = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_enable_redundancy_at_ultra_low");
    /**
     * The {@code uaqc.uaqc_enable_rtt_kalman_filter} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ENABLE_RTT_KALMAN_FILTER = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_enable_rtt_kalman_filter");
    /**
     * The {@code uaqc.uaqc_high_quality_compensation_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_COMPENSATION_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_compensation_pct");
    /**
     * The {@code uaqc.uaqc_high_quality_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_fpp");
    /**
     * The {@code uaqc.uaqc_high_quality_mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_mlow_dtx_hangover_ms");
    /**
     * The {@code uaqc.uaqc_high_quality_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_oob_factor");
    /**
     * The {@code uaqc.uaqc_high_quality_opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_opus_vad_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_plr_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_PLR_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_plr_lower_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_plr_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_PLR_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_plr_upper_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_remb_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_REMB_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_remb_lower_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_remb_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_REMB_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_remb_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_remb_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_REMB_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_remb_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_remb_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_REMB_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_remb_upper_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_rtt_ratio_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_rtt_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_rtt_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_rtt_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_RTT_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_rtt_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_rtt_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_RTT_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_high_quality_rtt_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_high_quality_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_target_bitrate");
    /**
     * The {@code uaqc.uaqc_high_quality_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HIGH_QUALITY_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_high_quality_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_hq_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_HQ_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_hq_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_lossy_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_fpp");
    /**
     * The {@code uaqc.uaqc_lossy_mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_mlow_dtx_hangover_ms");
    /**
     * The {@code uaqc.uaqc_lossy_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_lossy_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_oob_factor");
    /**
     * The {@code uaqc.uaqc_lossy_opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_opus_vad_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_plr_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_PLR_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_plr_lower_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_plr_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_PLR_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_plr_upper_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_remb_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_REMB_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_remb_lower_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_remb_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_REMB_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_remb_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_remb_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_REMB_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_remb_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_remb_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_REMB_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_remb_upper_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_rtt_ratio_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_rtt_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_rtt_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_rtt_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_RTT_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_rtt_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_rtt_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_RTT_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_lossy_rtt_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_lossy_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_target_bitrate");
    /**
     * The {@code uaqc.uaqc_lossy_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOSSY_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_lossy_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_low_quality_compensation_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_LOW_QUALITY_COMPENSATION_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_low_quality_compensation_pct");
    /**
     * The {@code uaqc.uaqc_main_stream_min_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_MAIN_STREAM_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_main_stream_min_bitrate");
    /**
     * The {@code uaqc.uaqc_no_rtcp_threshold_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_NO_RTCP_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_no_rtcp_threshold_ms");
    /**
     * The {@code uaqc.uaqc_oob_min_target_bitrate_bps} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_OOB_MIN_TARGET_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_oob_min_target_bitrate_bps");
    /**
     * The {@code uaqc.uaqc_plr_ema_attack_alpha} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PLR_EMA_ATTACK_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_plr_ema_attack_alpha");
    /**
     * The {@code uaqc.uaqc_plr_ema_impl} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PLR_EMA_IMPL = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_plr_ema_impl");
    /**
     * The {@code uaqc.uaqc_plr_ema_release_alpha} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PLR_EMA_RELEASE_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_plr_ema_release_alpha");
    /**
     * The {@code uaqc.uaqc_plr_slope_points} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PLR_SLOPE_POINTS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_plr_slope_points");
    /**
     * The {@code uaqc.uaqc_probing_compensation_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_COMPENSATION_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_compensation_pct");
    /**
     * The {@code uaqc.uaqc_probing_enable_history_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_ENABLE_HISTORY_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_enable_history_bitrate");
    /**
     * The {@code uaqc.uaqc_probing_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_fpp");
    /**
     * The {@code uaqc.uaqc_probing_history_match_filter} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_HISTORY_MATCH_FILTER = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_history_match_filter");
    /**
     * The {@code uaqc.uaqc_probing_history_max_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_HISTORY_MAX_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_history_max_bitrate");
    /**
     * The {@code uaqc.uaqc_probing_history_min_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_HISTORY_MIN_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_history_min_bitrate");
    /**
     * The {@code uaqc.uaqc_probing_history_mode} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_HISTORY_MODE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_history_mode");
    /**
     * The {@code uaqc.uaqc_probing_history_scale_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_HISTORY_SCALE_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_history_scale_pct");
    /**
     * The {@code uaqc.uaqc_probing_max_red_level} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_MAX_RED_LEVEL = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_max_red_level");
    /**
     * The {@code uaqc.uaqc_probing_mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_mlow_dtx_hangover_ms");
    /**
     * The {@code uaqc.uaqc_probing_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_probing_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_probing_oob_factor");
    /**
     * The {@code uaqc.uaqc_probing_opus_vad_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_OPUS_VAD_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_opus_vad_threshold");
    /**
     * The {@code uaqc.uaqc_probing_red_plr_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_RED_PLR_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_red_plr_threshold");
    /**
     * The {@code uaqc.uaqc_probing_remb_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_REMB_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_remb_threshold");
    /**
     * The {@code uaqc.uaqc_probing_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_target_bitrate");
    /**
     * The {@code uaqc.uaqc_probing_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_probing_use_start_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_PROBING_USE_START_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_probing_use_start_bitrate");
    /**
     * The {@code uaqc.uaqc_red_level_n_plr_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RED_LEVEL_N_PLR_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_red_level_n_plr_threshold");
    /**
     * The {@code uaqc.uaqc_remb_check_flag} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_REMB_CHECK_FLAG = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_remb_check_flag");
    /**
     * The {@code uaqc.uaqc_remb_slope_points} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_REMB_SLOPE_POINTS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_remb_slope_points");
    /**
     * The {@code uaqc.uaqc_rtt_based_start_bitrate_enabled} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_BASED_START_BITRATE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_based_start_bitrate_enabled");
    /**
     * The {@code uaqc.uaqc_rtt_congestion_kf_with_ratio} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_CONGESTION_KF_WITH_RATIO = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_congestion_kf_with_ratio");
    /**
     * The {@code uaqc.uaqc_rtt_congestion_step_kf_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_CONGESTION_STEP_KF_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_congestion_step_kf_ms");
    /**
     * The {@code uaqc.uaqc_rtt_ema_alpha} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_rtt_ema_alpha");
    /**
     * The {@code uaqc.uaqc_rtt_good_start_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_GOOD_START_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_good_start_bitrate");
    /**
     * The {@code uaqc.uaqc_rtt_good_threshold_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_GOOD_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_good_threshold_ms");
    /**
     * The {@code uaqc.uaqc_rtt_kf_down_gain_scale_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_KF_DOWN_GAIN_SCALE_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_kf_down_gain_scale_pct");
    /**
     * The {@code uaqc.uaqc_rtt_kf_up_gain_scale_pct} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_KF_UP_GAIN_SCALE_PCT = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_kf_up_gain_scale_pct");
    /**
     * The {@code uaqc.uaqc_rtt_poor_start_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_POOR_START_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_poor_start_bitrate");
    /**
     * The {@code uaqc.uaqc_rtt_poor_threshold_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_POOR_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_poor_threshold_ms");
    /**
     * The {@code uaqc.uaqc_rtt_slope_points} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_RTT_SLOPE_POINTS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_rtt_slope_points");
    /**
     * The {@code uaqc.uaqc_slide_window_size} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_SLIDE_WINDOW_SIZE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_slide_window_size");
    /**
     * The {@code uaqc.uaqc_state_transition_hesitation_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_STATE_TRANSITION_HESITATION_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_state_transition_hesitation_ms");
    /**
     * The {@code uaqc.uaqc_target_bitrate_net_bitmap} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_TARGET_BITRATE_NET_BITMAP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_target_bitrate_net_bitmap");
    /**
     * The {@code uaqc.uaqc_ulb_neteq_delay_offset_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULB_NETEQ_DELAY_OFFSET_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ulb_neteq_delay_offset_ms");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_fpp} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_FPP = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_fpp");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_oob_factor} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_OOB_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_oob_factor");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_plr_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_PLR_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_plr_lower_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_plr_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_PLR_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_plr_upper_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_remb_lower_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_REMB_LOWER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_remb_lower_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_remb_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_REMB_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_remb_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_remb_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_REMB_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_remb_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_remb_upper_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_REMB_UPPER_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_remb_upper_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_rtt_ratio_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_RTT_RATIO_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_rtt_ratio_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_rtt_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_RTT_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_rtt_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_rtt_trend_down_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_RTT_TREND_DOWN_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_rtt_trend_down_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_rtt_trend_up_threshold} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_RTT_TREND_UP_THRESHOLD = new VoipParamKey(VoipParamType.FLOAT, "uaqc.uaqc_ultra_low_bandwidth_rtt_trend_up_threshold");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_target_bitrate} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_TARGET_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_target_bitrate");
    /**
     * The {@code uaqc.uaqc_ultra_low_bandwidth_target_bitrate_net_offset} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_BANDWIDTH_TARGET_BITRATE_NET_OFFSET = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_bandwidth_target_bitrate_net_offset");
    /**
     * The {@code uaqc.uaqc_ultra_low_drain_mlow_dtx_hangover_ms} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_ULTRA_LOW_DRAIN_MLOW_DTX_HANGOVER_MS = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_ultra_low_drain_mlow_dtx_hangover_ms");
    /**
     * The {@code uaqc.uaqc_use_rtt_ema} voip-param.
     */
    public static final VoipParamKey UAQC_UAQC_USE_RTT_EMA = new VoipParamKey(VoipParamType.INTEGER, "uaqc.uaqc_use_rtt_ema");
    /**
     * The {@code vbwa_alg_rc.cond_is_speaker} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_IS_SPEAKER = new VoipParamKey(VoipParamType.INTEGER, "vbwa_alg_rc.cond_is_speaker");
    /**
     * The {@code vbwa_alg_rc.cond_range_bwa_vid_target_bitrate} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_BWA_VID_TARGET_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_bwa_vid_target_bitrate");
    /**
     * The {@code vbwa_alg_rc.cond_range_dl_bwe} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_DL_BWE = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_dl_bwe");
    /**
     * The {@code vbwa_alg_rc.cond_range_ema_downlink_plr} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_EMA_DOWNLINK_PLR = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_ema_downlink_plr");
    /**
     * The {@code vbwa_alg_rc.cond_range_ema_uplink_plr} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_EMA_UPLINK_PLR = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_ema_uplink_plr");
    /**
     * The {@code vbwa_alg_rc.cond_range_gcall_size} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_GCALL_SIZE = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_gcall_size");
    /**
     * The {@code vbwa_alg_rc.cond_range_sec_since_sfu_simulcast_capable} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_SEC_SINCE_SFU_SIMULCAST_CAPABLE = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_sec_since_sfu_simulcast_capable");
    /**
     * The {@code vbwa_alg_rc.cond_range_ul_bwe} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_COND_RANGE_UL_BWE = new VoipParamKey(VoipParamType.ARRAY, "vbwa_alg_rc.cond_range_ul_bwe");
    /**
     * The {@code vbwa_alg_rc.min_vid_stream_reserve_bps_receiver} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_MIN_VID_STREAM_RESERVE_BPS_RECEIVER = new VoipParamKey(VoipParamType.INTEGER, "vbwa_alg_rc.min_vid_stream_reserve_bps_receiver");
    /**
     * The {@code vbwa_alg_rc.min_vid_stream_reserve_bps_sender} voip-param.
     */
    public static final VoipParamKey VBWA_ALG_RC_MIN_VID_STREAM_RESERVE_BPS_SENDER = new VoipParamKey(VoipParamType.INTEGER, "vbwa_alg_rc.min_vid_stream_reserve_bps_sender");
    /**
     * The {@code vid_driver.camera_height} voip-param.
     */
    public static final VoipParamKey VID_DRIVER_CAMERA_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "vid_driver.camera_height");
    /**
     * The {@code vid_driver.camera_width} voip-param.
     */
    public static final VoipParamKey VID_DRIVER_CAMERA_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "vid_driver.camera_width");
    /**
     * The {@code vid_driver.max_capture_fps} voip-param.
     */
    public static final VoipParamKey VID_DRIVER_MAX_CAPTURE_FPS = new VoipParamKey(VoipParamType.INTEGER, "vid_driver.max_capture_fps");
    /**
     * The {@code vid_rc.a2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_A2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.a2a_ml_feature_val");
    /**
     * The {@code vid_rc.a2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_A2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.a2i_ml_feature_val");
    /**
     * The {@code vid_rc.additive_sender_bwe_inc_ceiling_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_ADDITIVE_SENDER_BWE_INC_CEILING_MULTIPLIER = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.additive_sender_bwe_inc_ceiling_multiplier");
    /**
     * The {@code vid_rc.adjust_vid_bitrate_using_e2e_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_ADJUST_VID_BITRATE_USING_E2E_FEC_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.adjust_vid_bitrate_using_e2e_fec_ratio");
    /**
     * The {@code vid_rc.apply_ramp_down_enc_params} voip-param.
     */
    public static final VoipParamKey VID_RC_APPLY_RAMP_DOWN_ENC_PARAMS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.apply_ramp_down_enc_params");
    /**
     * The {@code vid_rc.audio_reserve_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_AUDIO_RESERVE_BPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.audio_reserve_bps");
    /**
     * The {@code vid_rc.av1_decoder_high_priority} voip-param.
     */
    public static final VoipParamKey VID_RC_AV1_DECODER_HIGH_PRIORITY = new VoipParamKey(VoipParamType.STRING, "vid_rc.av1_decoder_high_priority");
    /**
     * The {@code vid_rc.bad_action} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_ACTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_action");
    /**
     * The {@code vid_rc.bad_clamp_bitrate_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_CLAMP_BITRATE_BPS = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.bad_clamp_bitrate_bps");
    /**
     * The {@code vid_rc.bad_mode_duration_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_MODE_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_mode_duration_ms");
    /**
     * The {@code vid_rc.bad_pp_clamp_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_PP_CLAMP_MULTIPLIER = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_pp_clamp_multiplier");
    /**
     * The {@code vid_rc.bad_ramp_down_percentage} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_RAMP_DOWN_PERCENTAGE = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.bad_ramp_down_percentage");
    /**
     * The {@code vid_rc.bad_tr_bitrate_hi_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_BITRATE_HI_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_bitrate_hi_threshold");
    /**
     * The {@code vid_rc.bad_tr_bitrate_lo_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_BITRATE_LO_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_bitrate_lo_threshold");
    /**
     * The {@code vid_rc.bad_tr_probability_hi_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_PROBABILITY_HI_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_probability_hi_threshold");
    /**
     * The {@code vid_rc.bad_tr_probability_hi_threshold_transport} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_PROBABILITY_HI_THRESHOLD_TRANSPORT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_probability_hi_threshold_transport");
    /**
     * The {@code vid_rc.bad_tr_probability_lo_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_PROBABILITY_LO_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_probability_lo_threshold");
    /**
     * The {@code vid_rc.bad_tr_probability_lo_threshold_transport} voip-param.
     */
    public static final VoipParamKey VID_RC_BAD_TR_PROBABILITY_LO_THRESHOLD_TRANSPORT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.bad_tr_probability_lo_threshold_transport");
    /**
     * The {@code vid_rc.cc_bwe_slow_ramp_up_ceiling_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_BWE_SLOW_RAMP_UP_CEILING_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_bwe_slow_ramp_up_ceiling_mode");
    /**
     * The {@code vid_rc.cc_bwe_slow_ramp_up_ceiling_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_BWE_SLOW_RAMP_UP_CEILING_MULTIPLIER = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_bwe_slow_ramp_up_ceiling_multiplier");
    /**
     * The {@code vid_rc.cc_bwe_slow_ramp_up_peer_rx_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_BWE_SLOW_RAMP_UP_PEER_RX_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_bwe_slow_ramp_up_peer_rx_bitrate");
    /**
     * The {@code vid_rc.cc_enable_per_model_platform_pair} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ENABLE_PER_MODEL_PLATFORM_PAIR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_enable_per_model_platform_pair");
    /**
     * The {@code vid_rc.cc_enable_pip_mode_improvement} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ENABLE_PIP_MODE_IMPROVEMENT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_enable_pip_mode_improvement");
    /**
     * The {@code vid_rc.cc_ml_cong_condition_type} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_CONG_CONDITION_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_cong_condition_type");
    /**
     * The {@code vid_rc.cc_ml_cong_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_CONG_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_cong_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_hd_targeting_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_HD_TARGETING_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_hd_targeting_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_hd_targeting2_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_HD_TARGETING2_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_hd_targeting2_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_model_load_start_time_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_MODEL_LOAD_START_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_model_load_start_time_ms");
    /**
     * The {@code vid_rc.cc_ml_tr_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_TR_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_tr_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_undershoot_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_UNDERSHOOT_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_undershoot_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_undershoot2_probability_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_UNDERSHOOT2_PROBABILITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_undershoot2_probability_threshold");
    /**
     * The {@code vid_rc.cc_ml_use_hd_targeting2_for_inference} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_USE_HD_TARGETING2_FOR_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_use_hd_targeting2_for_inference");
    /**
     * The {@code vid_rc.cc_ml_use_undershoot2_for_inference} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_ML_USE_UNDERSHOOT2_FOR_INFERENCE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_ml_use_undershoot2_for_inference");
    /**
     * The {@code vid_rc.cc_use_nm_pair_feature} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_USE_NM_PAIR_FEATURE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_use_nm_pair_feature");
    /**
     * The {@code vid_rc.cc_use_peer_rx_aud_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_CC_USE_PEER_RX_AUD_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cc_use_peer_rx_aud_bitrate");
    /**
     * The {@code vid_rc.cellular_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CELLULAR_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cellular_ml_feature_val");
    /**
     * The {@code vid_rc.clamp_sfu_ul_bwe_in_all_key_frame_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_CLAMP_SFU_UL_BWE_IN_ALL_KEY_FRAME_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.clamp_sfu_ul_bwe_in_all_key_frame_mode");
    /**
     * The {@code vid_rc.codec_rc_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_CODEC_RC_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.codec_rc_mode");
    /**
     * The {@code vid_rc.cond_after_simulcast_schedule} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_AFTER_SIMULCAST_SCHEDULE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_after_simulcast_schedule");
    /**
     * The {@code vid_rc.cond_aud_in_trouble} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_AUD_IN_TROUBLE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_aud_in_trouble");
    /**
     * The {@code vid_rc.cond_battery_drop_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_BATTERY_DROP_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_battery_drop_threshold");
    /**
     * The {@code vid_rc.cond_battery_low_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_BATTERY_LOW_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_battery_low_threshold");
    /**
     * The {@code vid_rc.cond_bitrate_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_BITRATE_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_bitrate_hysteresis");
    /**
     * The {@code vid_rc.cond_codec_scheme} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CODEC_SCHEME = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_codec_scheme");
    /**
     * The {@code vid_rc.cond_codec_type} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CODEC_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_codec_type");
    /**
     * The {@code vid_rc.cond_codecs_contain_any} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CODECS_CONTAIN_ANY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_codecs_contain_any");
    /**
     * The {@code vid_rc.cond_codecs_contain_only} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CODECS_CONTAIN_ONLY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_codecs_contain_only");
    /**
     * The {@code vid_rc.cond_congestion_abs_rtt_thr} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CONGESTION_ABS_RTT_THR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_congestion_abs_rtt_thr");
    /**
     * The {@code vid_rc.cond_congestion_no_data_thr} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CONGESTION_NO_DATA_THR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_congestion_no_data_thr");
    /**
     * The {@code vid_rc.cond_congestion_no_init_rtt_thr} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CONGESTION_NO_INIT_RTT_THR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_congestion_no_init_rtt_thr");
    /**
     * The {@code vid_rc.cond_congestion_no_rtcp_thr} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CONGESTION_NO_RTCP_THR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_congestion_no_rtcp_thr");
    /**
     * The {@code vid_rc.cond_congestion_signal_mask} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_CONGESTION_SIGNAL_MASK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_congestion_signal_mask");
    /**
     * The {@code vid_rc.cond_device_class_mask} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_DEVICE_CLASS_MASK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_device_class_mask");
    /**
     * The {@code vid_rc.cond_encoding_doc_screen_sharing} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_ENCODING_DOC_SCREEN_SHARING = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_encoding_doc_screen_sharing");
    /**
     * The {@code vid_rc.cond_encoding_video_screen_sharing} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_ENCODING_VIDEO_SCREEN_SHARING = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_encoding_video_screen_sharing");
    /**
     * The {@code vid_rc.cond_freq_rtt_cycle} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_FREQ_RTT_CYCLE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_freq_rtt_cycle");
    /**
     * The {@code vid_rc.cond_high_init_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_HIGH_INIT_RTT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_high_init_rtt");
    /**
     * The {@code vid_rc.cond_high_init_rtt_stddev} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_HIGH_INIT_RTT_STDDEV = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_high_init_rtt_stddev");
    /**
     * The {@code vid_rc.cond_hist_rtt_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_HIST_RTT_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_hist_rtt_ratio");
    /**
     * The {@code vid_rc.cond_in_congestion} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_IN_CONGESTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_in_congestion");
    /**
     * The {@code vid_rc.cond_is_sfu_downlink} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_IS_SFU_DOWNLINK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_is_sfu_downlink");
    /**
     * The {@code vid_rc.cond_is_sfu_uplink} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_IS_SFU_UPLINK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_is_sfu_uplink");
    /**
     * The {@code vid_rc.cond_low_quality_vid_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_LOW_QUALITY_VID_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_low_quality_vid_mode");
    /**
     * The {@code vid_rc.cond_ml_hd_targeting_type} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_ML_HD_TARGETING_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_ml_hd_targeting_type");
    /**
     * The {@code vid_rc.cond_mte_combine_bad} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_MTE_COMBINE_BAD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_mte_combine_bad");
    /**
     * The {@code vid_rc.cond_net_medium} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_NET_MEDIUM = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_net_medium");
    /**
     * The {@code vid_rc.cond_net_medium_pair} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_NET_MEDIUM_PAIR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_net_medium_pair");
    /**
     * The {@code vid_rc.cond_new_codec_type} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_NEW_CODEC_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_new_codec_type");
    /**
     * The {@code vid_rc.cond_ongoing_all_ltrp} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_ONGOING_ALL_LTRP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_ongoing_all_ltrp");
    /**
     * The {@code vid_rc.cond_packet_loss_pct_ema_alpha} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PACKET_LOSS_PCT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.cond_packet_loss_pct_ema_alpha");
    /**
     * The {@code vid_rc.cond_peer_android_percentage} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_ANDROID_PERCENTAGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_android_percentage");
    /**
     * The {@code vid_rc.cond_peer_cellular_percentage} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_CELLULAR_PERCENTAGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_cellular_percentage");
    /**
     * The {@code vid_rc.cond_peer_in_speaker_view} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_IN_SPEAKER_VIEW = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_in_speaker_view");
    /**
     * The {@code vid_rc.cond_peer_iphone_percentage} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_IPHONE_PERCENTAGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_iphone_percentage");
    /**
     * The {@code vid_rc.cond_peer_net_medium} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_NET_MEDIUM = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_net_medium");
    /**
     * The {@code vid_rc.cond_peer_platform_mask} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_PLATFORM_MASK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_platform_mask");
    /**
     * The {@code vid_rc.cond_peer_wifi_percentage} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PEER_WIFI_PERCENTAGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_peer_wifi_percentage");
    /**
     * The {@code vid_rc.cond_permanent_if_matched} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PERMANENT_IF_MATCHED = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_permanent_if_matched");
    /**
     * The {@code vid_rc.cond_pip_threshold_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PIP_THRESHOLD_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pip_threshold_ms");
    /**
     * The {@code vid_rc.cond_pkt_loss_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PKT_LOSS_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pkt_loss_hysteresis");
    /**
     * The {@code vid_rc.cond_platform_mask} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PLATFORM_MASK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_platform_mask");
    /**
     * The {@code vid_rc.cond_plr_predictor_state} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PLR_PREDICTOR_STATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_plr_predictor_state");
    /**
     * The {@code vid_rc.cond_pp_bitrate_avg_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PP_BITRATE_AVG_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pp_bitrate_avg_hysteresis");
    /**
     * The {@code vid_rc.cond_pp_bitrate_cov_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PP_BITRATE_COV_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pp_bitrate_cov_hysteresis");
    /**
     * The {@code vid_rc.cond_pp_bitrate_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PP_BITRATE_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pp_bitrate_hysteresis");
    /**
     * The {@code vid_rc.cond_pp_bitrate_last_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PP_BITRATE_LAST_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pp_bitrate_last_hysteresis");
    /**
     * The {@code vid_rc.cond_pp_bitrate_max_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PP_BITRATE_MAX_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pp_bitrate_max_hysteresis");
    /**
     * The {@code vid_rc.cond_pure_1x1_call} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_PURE_1X1_CALL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_pure_1x1_call");
    /**
     * The {@code vid_rc.cond_range_android_device_class} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_ANDROID_DEVICE_CLASS = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_android_device_class");
    /**
     * The {@code vid_rc.cond_range_audplr} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_AUDPLR = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_audplr");
    /**
     * The {@code vid_rc.cond_range_avg_target_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_AVG_TARGET_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_avg_target_bitrate");
    /**
     * The {@code vid_rc.cond_range_bwe} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_BWE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_bwe");
    /**
     * The {@code vid_rc.cond_range_cell_rsrq_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CELL_RSRQ_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_cell_rsrq_bitrate");
    /**
     * The {@code vid_rc.cond_range_cell_signal_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CELL_SIGNAL_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_cell_signal_bitrate");
    /**
     * The {@code vid_rc.cond_range_cell_sinr_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CELL_SINR_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_cell_sinr_bitrate");
    /**
     * The {@code vid_rc.cond_range_cell_sl_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CELL_SL_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_cell_sl_bitrate");
    /**
     * The {@code vid_rc.cond_range_congestion_ceiling_seen} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CONGESTION_CEILING_SEEN = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_congestion_ceiling_seen");
    /**
     * The {@code vid_rc.cond_range_cur_vid_rx_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_CUR_VID_RX_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_cur_vid_rx_bitrate");
    /**
     * The {@code vid_rc.cond_range_dec_width} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_DEC_WIDTH = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_dec_width");
    /**
     * The {@code vid_rc.cond_range_dl_bwe} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_DL_BWE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_dl_bwe");
    /**
     * The {@code vid_rc.cond_range_dl_bwe_div_by_ceiling} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_DL_BWE_DIV_BY_CEILING = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_dl_bwe_div_by_ceiling");
    /**
     * The {@code vid_rc.cond_range_ema_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_EMA_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_ema_packet_loss_pct");
    /**
     * The {@code vid_rc.cond_range_ema_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_EMA_RTT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_ema_rtt");
    /**
     * The {@code vid_rc.cond_range_ema_uplink_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_EMA_UPLINK_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_ema_uplink_packet_loss_pct");
    /**
     * The {@code vid_rc.cond_range_gcall_size} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_GCALL_SIZE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_gcall_size");
    /**
     * The {@code vid_rc.cond_range_goodput_peer_downlink} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_GOODPUT_PEER_DOWNLINK = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_goodput_peer_downlink");
    /**
     * The {@code vid_rc.cond_range_history_based_480p_encoding} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_BASED_480P_ENCODING = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_based_480p_encoding");
    /**
     * The {@code vid_rc.cond_range_history_based_720p_encoding} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_BASED_720P_ENCODING = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_based_720p_encoding");
    /**
     * The {@code vid_rc.cond_range_history_based_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_BASED_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_based_bitrate");
    /**
     * The {@code vid_rc.cond_range_history_based_bitrate_match_self_only} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_BASED_BITRATE_MATCH_SELF_ONLY = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_based_bitrate_match_self_only");
    /**
     * The {@code vid_rc.cond_range_history_based_tx_pkt_loss_perc} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_BASED_TX_PKT_LOSS_PERC = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_based_tx_pkt_loss_perc");
    /**
     * The {@code vid_rc.cond_range_history_v2_based_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_HISTORY_V2_BASED_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_history_v2_based_bitrate");
    /**
     * The {@code vid_rc.cond_range_lterm_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_LTERM_RTT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_lterm_rtt");
    /**
     * The {@code vid_rc.cond_range_network_type_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_NETWORK_TYPE_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_network_type_bitrate");
    /**
     * The {@code vid_rc.cond_range_num_res_rampdowns} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_NUM_RES_RAMPDOWNS = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_num_res_rampdowns");
    /**
     * The {@code vid_rc.cond_range_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_packet_loss_pct");
    /**
     * The {@code vid_rc.cond_range_peer_device_class} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PEER_DEVICE_CLASS = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_peer_device_class");
    /**
     * The {@code vid_rc.cond_range_peer_screen_w} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PEER_SCREEN_W = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_peer_screen_w");
    /**
     * The {@code vid_rc.cond_range_pp_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_bitrate");
    /**
     * The {@code vid_rc.cond_range_pp_bitrate_avg} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_BITRATE_AVG = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_bitrate_avg");
    /**
     * The {@code vid_rc.cond_range_pp_bitrate_cov} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_BITRATE_COV = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_bitrate_cov");
    /**
     * The {@code vid_rc.cond_range_pp_bitrate_last} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_BITRATE_LAST = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_bitrate_last");
    /**
     * The {@code vid_rc.cond_range_pp_bitrate_max} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_BITRATE_MAX = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_bitrate_max");
    /**
     * The {@code vid_rc.cond_range_pp_flip_count_for_hd} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_FLIP_COUNT_FOR_HD = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_flip_count_for_hd");
    /**
     * The {@code vid_rc.cond_range_pp_flip_freq_for_hd} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_PP_FLIP_FREQ_FOR_HD = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_pp_flip_freq_for_hd");
    /**
     * The {@code vid_rc.cond_range_remb_div_by_peer_rx_br} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_REMB_DIV_BY_PEER_RX_BR = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_remb_div_by_peer_rx_br");
    /**
     * The {@code vid_rc.cond_range_remb_minus_peer_rx_br} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_REMB_MINUS_PEER_RX_BR = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_remb_minus_peer_rx_br");
    /**
     * The {@code vid_rc.cond_range_res_switch_freq} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_RES_SWITCH_FREQ = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_res_switch_freq");
    /**
     * The {@code vid_rc.cond_range_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_RTT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_rtt");
    /**
     * The {@code vid_rc.cond_range_sec_since_start} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_SEC_SINCE_START = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_sec_since_start");
    /**
     * The {@code vid_rc.cond_range_slr_output_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_SLR_OUTPUT_BPS = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_slr_output_bps");
    /**
     * The {@code vid_rc.cond_range_sterm_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_STERM_RTT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_sterm_rtt");
    /**
     * The {@code vid_rc.cond_range_target_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_TARGET_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_target_bitrate");
    /**
     * The {@code vid_rc.cond_range_target_total_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_TARGET_TOTAL_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_target_total_bitrate");
    /**
     * The {@code vid_rc.cond_range_tgt_br_div_by_ceiling} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_TGT_BR_DIV_BY_CEILING = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_tgt_br_div_by_ceiling");
    /**
     * The {@code vid_rc.cond_range_total_vid_target_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_TOTAL_VID_TARGET_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_total_vid_target_bitrate");
    /**
     * The {@code vid_rc.cond_range_tr_seq_len} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_TR_SEQ_LEN = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_tr_seq_len");
    /**
     * The {@code vid_rc.cond_range_ul_bwe} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_UL_BWE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_ul_bwe");
    /**
     * The {@code vid_rc.cond_range_ul_bwe_div_by_ceiling} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_UL_BWE_DIV_BY_CEILING = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_ul_bwe_div_by_ceiling");
    /**
     * The {@code vid_rc.cond_range_unused_link_bw} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_UNUSED_LINK_BW = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_unused_link_bw");
    /**
     * The {@code vid_rc.cond_range_uplink_packet_loss_pct} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_UPLINK_PACKET_LOSS_PCT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_uplink_packet_loss_pct");
    /**
     * The {@code vid_rc.cond_range_uplink_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_UPLINK_RTT = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_uplink_rtt");
    /**
     * The {@code vid_rc.cond_range_wifi_sl_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RANGE_WIFI_SL_BITRATE = new VoipParamKey(VoipParamType.ARRAY, "vid_rc.cond_range_wifi_sl_bitrate");
    /**
     * The {@code vid_rc.cond_redial_status} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_REDIAL_STATUS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_redial_status");
    /**
     * The {@code vid_rc.cond_rtt_ema_alpha} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RTT_EMA_ALPHA = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.cond_rtt_ema_alpha");
    /**
     * The {@code vid_rc.cond_rtt_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_RTT_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_rtt_hysteresis");
    /**
     * The {@code vid_rc.cond_sbwe_in_bad} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_SBWE_IN_BAD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_sbwe_in_bad");
    /**
     * The {@code vid_rc.cond_sbwe_in_mcp} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_SBWE_IN_MCP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_sbwe_in_mcp");
    /**
     * The {@code vid_rc.cond_screen_share_receiver} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_SCREEN_SHARE_RECEIVER = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_screen_share_receiver");
    /**
     * The {@code vid_rc.cond_self_in_speaker_view} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_SELF_IN_SPEAKER_VIEW = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_self_in_speaker_view");
    /**
     * The {@code vid_rc.cond_total_bitrate_hysteresis} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_TOTAL_BITRATE_HYSTERESIS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_total_bitrate_hysteresis");
    /**
     * The {@code vid_rc.cond_tr_type} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_TR_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_tr_type");
    /**
     * The {@code vid_rc.cond_udst_source_mask} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_UDST_SOURCE_MASK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_udst_source_mask");
    /**
     * The {@code vid_rc.cond_udst_type} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_UDST_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_udst_type");
    /**
     * The {@code vid_rc.cond_vid_byte_throttling} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_VID_BYTE_THROTTLING = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_vid_byte_throttling");
    /**
     * The {@code vid_rc.cond_video_quality_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_VIDEO_QUALITY_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_video_quality_mode");
    /**
     * The {@code vid_rc.cond_vr_platform} voip-param.
     */
    public static final VoipParamKey VID_RC_COND_VR_PLATFORM = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cond_vr_platform");
    /**
     * The {@code vid_rc.cong_a2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_A2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_a2a_ml_feature_val");
    /**
     * The {@code vid_rc.cong_a2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_A2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_a2i_ml_feature_val");
    /**
     * The {@code vid_rc.cong_default_platform_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_DEFAULT_PLATFORM_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_default_platform_feature_val");
    /**
     * The {@code vid_rc.cong_i2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_I2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_i2a_ml_feature_val");
    /**
     * The {@code vid_rc.cong_i2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_I2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_i2i_ml_feature_val");
    /**
     * The {@code vid_rc.cong_web_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_CONG_WEB_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.cong_web_ml_feature_val");
    /**
     * The {@code vid_rc.decoder_high_priority} voip-param.
     */
    public static final VoipParamKey VID_RC_DECODER_HIGH_PRIORITY = new VoipParamKey(VoipParamType.STRING, "vid_rc.decoder_high_priority");
    /**
     * The {@code vid_rc.delay_fec_receiver_creation} voip-param.
     */
    public static final VoipParamKey VID_RC_DELAY_FEC_RECEIVER_CREATION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.delay_fec_receiver_creation");
    /**
     * The {@code vid_rc.delay_fec_sender_creation} voip-param.
     */
    public static final VoipParamKey VID_RC_DELAY_FEC_SENDER_CREATION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.delay_fec_sender_creation");
    /**
     * The {@code vid_rc.disable_e2e_fec_fix} voip-param.
     */
    public static final VoipParamKey VID_RC_DISABLE_E2E_FEC_FIX = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.disable_e2e_fec_fix");
    /**
     * The {@code vid_rc.disable_simulcast_under_bad_network} voip-param.
     */
    public static final VoipParamKey VID_RC_DISABLE_SIMULCAST_UNDER_BAD_NETWORK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.disable_simulcast_under_bad_network");
    /**
     * The {@code vid_rc.dl_bwe_combine_policy} voip-param.
     */
    public static final VoipParamKey VID_RC_DL_BWE_COMBINE_POLICY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.dl_bwe_combine_policy");
    /**
     * The {@code vid_rc.dl_sender_loss_high} voip-param.
     */
    public static final VoipParamKey VID_RC_DL_SENDER_LOSS_HIGH = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.dl_sender_loss_high");
    /**
     * The {@code vid_rc.dl_sender_loss_low} voip-param.
     */
    public static final VoipParamKey VID_RC_DL_SENDER_LOSS_LOW = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.dl_sender_loss_low");
    /**
     * The {@code vid_rc.downlink_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_DOWNLINK_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.downlink_ml_feature_val");
    /**
     * The {@code vid_rc.dyn_enc_fmt_change_wait_fix} voip-param.
     */
    public static final VoipParamKey VID_RC_DYN_ENC_FMT_CHANGE_WAIT_FIX = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.dyn_enc_fmt_change_wait_fix");
    /**
     * The {@code vid_rc.early_recv_consistent_low_count} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_CONSISTENT_LOW_COUNT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_consistent_low_count");
    /**
     * The {@code vid_rc.early_recv_floor_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_FLOOR_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_floor_bitrate");
    /**
     * The {@code vid_rc.early_recv_loss_override} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_LOSS_OVERRIDE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_loss_override");
    /**
     * The {@code vid_rc.early_recv_only_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_ONLY_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_only_mode");
    /**
     * The {@code vid_rc.early_recv_sender_bwe_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_SENDER_BWE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_sender_bwe_threshold");
    /**
     * The {@code vid_rc.early_recv_slope_convergence} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_SLOPE_CONVERGENCE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_slope_convergence");
    /**
     * The {@code vid_rc.early_recv_slope_stable_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_SLOPE_STABLE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_slope_stable_threshold");
    /**
     * The {@code vid_rc.early_recv_time_fallback_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_TIME_FALLBACK_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_time_fallback_ms");
    /**
     * The {@code vid_rc.early_recv_use_floor} voip-param.
     */
    public static final VoipParamKey VID_RC_EARLY_RECV_USE_FLOOR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.early_recv_use_floor");
    /**
     * The {@code vid_rc.enabl_ml_udst_ratio_calc} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABL_ML_UDST_RATIO_CALC = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enabl_ml_udst_ratio_calc");
    /**
     * The {@code vid_rc.enable_bad_call_prevention} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_BAD_CALL_PREVENTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_bad_call_prevention");
    /**
     * The {@code vid_rc.enable_bwe_ceiling_calc_by_link_capacity} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_BWE_CEILING_CALC_BY_LINK_CAPACITY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_bwe_ceiling_calc_by_link_capacity");
    /**
     * The {@code vid_rc.enable_bwe_dyn_param} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_BWE_DYN_PARAM = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_bwe_dyn_param");
    /**
     * The {@code vid_rc.enable_bwe_dyn_param_in_congestion_detection} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_BWE_DYN_PARAM_IN_CONGESTION_DETECTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_bwe_dyn_param_in_congestion_detection");
    /**
     * The {@code vid_rc.enable_cc_bwe_slow_ramp_up} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_CC_BWE_SLOW_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_cc_bwe_slow_ramp_up");
    /**
     * The {@code vid_rc.enable_ml_udst} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_ML_UDST = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_ml_udst");
    /**
     * The {@code vid_rc.enable_random_forced_probing_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_RANDOM_FORCED_PROBING_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_random_forced_probing_mode");
    /**
     * The {@code vid_rc.enable_random_forced_probing_mode_dl} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_RANDOM_FORCED_PROBING_MODE_DL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_random_forced_probing_mode_dl");
    /**
     * The {@code vid_rc.enable_roi_encoding} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_ROI_ENCODING = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_roi_encoding");
    /**
     * The {@code vid_rc.enable_rs_fec_receiver_reset} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_RS_FEC_RECEIVER_RESET = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_rs_fec_receiver_reset");
    /**
     * The {@code vid_rc.enable_separate_congestion_thresholds} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_SEPARATE_CONGESTION_THRESHOLDS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_separate_congestion_thresholds");
    /**
     * The {@code vid_rc.enable_ss_enc_downscale_fix} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_SS_ENC_DOWNSCALE_FIX = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_ss_enc_downscale_fix");
    /**
     * The {@code vid_rc.enable_stop_mcp_without_fr} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_STOP_MCP_WITHOUT_FR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_stop_mcp_without_fr");
    /**
     * The {@code vid_rc.enable_vid_red_aware_ares} voip-param.
     */
    public static final VoipParamKey VID_RC_ENABLE_VID_RED_AWARE_ARES = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enable_vid_red_aware_ares");
    /**
     * The {@code vid_rc.enc_latency_handling_fixes} voip-param.
     */
    public static final VoipParamKey VID_RC_ENC_LATENCY_HANDLING_FIXES = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enc_latency_handling_fixes");
    /**
     * The {@code vid_rc.enc_p_adj_complexity_step} voip-param.
     */
    public static final VoipParamKey VID_RC_ENC_P_ADJ_COMPLEXITY_STEP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enc_p_adj_complexity_step");
    /**
     * The {@code vid_rc.enc_p_adj_min_complexity} voip-param.
     */
    public static final VoipParamKey VID_RC_ENC_P_ADJ_MIN_COMPLEXITY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.enc_p_adj_min_complexity");
    /**
     * The {@code vid_rc.encode_fmt_change_wait} voip-param.
     */
    public static final VoipParamKey VID_RC_ENCODE_FMT_CHANGE_WAIT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.encode_fmt_change_wait");
    /**
     * The {@code vid_rc.fec_algorithm} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_ALGORITHM = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_algorithm");
    /**
     * The {@code vid_rc.fec_burst_on_relay_change_duration_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_BURST_ON_RELAY_CHANGE_DURATION_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_burst_on_relay_change_duration_ms");
    /**
     * The {@code vid_rc.fec_burst_on_relay_change_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_BURST_ON_RELAY_CHANGE_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_burst_on_relay_change_ratio");
    /**
     * The {@code vid_rc.fec_cover_range} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_COVER_RANGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_cover_range");
    /**
     * The {@code vid_rc.fec_high_rtt_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_HIGH_RTT_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_high_rtt_ms");
    /**
     * The {@code vid_rc.fec_hyst_time_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_HYST_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_hyst_time_ms");
    /**
     * The {@code vid_rc.fec_low_rtt_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_LOW_RTT_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_low_rtt_ms");
    /**
     * The {@code vid_rc.fec_max_media_frames} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_MAX_MEDIA_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_max_media_frames");
    /**
     * The {@code vid_rc.fec_max_rtt_decrease_factor} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_MAX_RTT_DECREASE_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_max_rtt_decrease_factor");
    /**
     * The {@code vid_rc.fec_max_rtt_increase_factor} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_MAX_RTT_INCREASE_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_max_rtt_increase_factor");
    /**
     * The {@code vid_rc.fec_min_num_media_frame} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_MIN_NUM_MEDIA_FRAME = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_min_num_media_frame");
    /**
     * The {@code vid_rc.fec_min_num_media_packet} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_MIN_NUM_MEDIA_PACKET = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_min_num_media_packet");
    /**
     * The {@code vid_rc.fec_quant_step} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_QUANT_STEP = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_quant_step");
    /**
     * The {@code vid_rc.fec_rs_num_parity_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_RS_NUM_PARITY_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.fec_rs_num_parity_threshold");
    /**
     * The {@code vid_rc.fec_rtt_based_scaling_weight} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_RTT_BASED_SCALING_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_rtt_based_scaling_weight");
    /**
     * The {@code vid_rc.fec_to_packet_loss} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_TO_PACKET_LOSS = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_to_packet_loss");
    /**
     * The {@code vid_rc.fec_vid_bitrate_adj_factor} voip-param.
     */
    public static final VoipParamKey VID_RC_FEC_VID_BITRATE_ADJ_FACTOR = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.fec_vid_bitrate_adj_factor");
    /**
     * The {@code vid_rc.h265_bitrate_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_H265_BITRATE_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.h265_bitrate_multiplier");
    /**
     * The {@code vid_rc.hd_targeting_a2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_A2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_a2a_ml_feature_val");
    /**
     * The {@code vid_rc.hd_targeting_a2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_A2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_a2i_ml_feature_val");
    /**
     * The {@code vid_rc.hd_targeting_default_platform_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_DEFAULT_PLATFORM_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_default_platform_feature_val");
    /**
     * The {@code vid_rc.hd_targeting_i2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_I2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_i2a_ml_feature_val");
    /**
     * The {@code vid_rc.hd_targeting_i2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_I2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_i2i_ml_feature_val");
    /**
     * The {@code vid_rc.hd_targeting_web_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_HD_TARGETING_WEB_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.hd_targeting_web_ml_feature_val");
    /**
     * The {@code vid_rc.high_cellular_bitrate_usage_detection_bitrate_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_HIGH_CELLULAR_BITRATE_USAGE_DETECTION_BITRATE_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.high_cellular_bitrate_usage_detection_bitrate_threshold");
    /**
     * The {@code vid_rc.high_cellular_bitrate_usage_detection_duration_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_HIGH_CELLULAR_BITRATE_USAGE_DETECTION_DURATION_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.high_cellular_bitrate_usage_detection_duration_threshold");
    /**
     * The {@code vid_rc.high_cellular_bitrate_usage_detection_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_HIGH_CELLULAR_BITRATE_USAGE_DETECTION_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.high_cellular_bitrate_usage_detection_mode");
    /**
     * The {@code vid_rc.history_v2_cond_range_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_HISTORY_V2_COND_RANGE_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.history_v2_cond_range_mode");
    /**
     * The {@code vid_rc.i2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_I2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.i2a_ml_feature_val");
    /**
     * The {@code vid_rc.i2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_I2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.i2i_ml_feature_val");
    /**
     * The {@code vid_rc.ios_data_rate_limit_sec} voip-param.
     */
    public static final VoipParamKey VID_RC_IOS_DATA_RATE_LIMIT_SEC = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ios_data_rate_limit_sec");
    /**
     * The {@code vid_rc.ios_data_rate_perc} voip-param.
     */
    public static final VoipParamKey VID_RC_IOS_DATA_RATE_PERC = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ios_data_rate_perc");
    /**
     * The {@code vid_rc.key_fec_ratio_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_KEY_FEC_RATIO_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.key_fec_ratio_multiplier");
    /**
     * The {@code vid_rc.key_frame_interval} voip-param.
     */
    public static final VoipParamKey VID_RC_KEY_FRAME_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.key_frame_interval");
    /**
     * The {@code vid_rc.low_data_usage_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_LOW_DATA_USAGE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.low_data_usage_bitrate");
    /**
     * The {@code vid_rc.max_capture_width} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_CAPTURE_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.max_capture_width");
    /**
     * The {@code vid_rc.max_encode_height} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_ENCODE_HEIGHT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.max_encode_height");
    /**
     * The {@code vid_rc.max_encode_width} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_ENCODE_WIDTH = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.max_encode_width");
    /**
     * The {@code vid_rc.max_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.max_fec_ratio");
    /**
     * The {@code vid_rc.max_fps} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_FPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.max_fps");
    /**
     * The {@code vid_rc.max_hbh_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_HBH_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.max_hbh_fec_ratio");
    /**
     * The {@code vid_rc.max_key_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_KEY_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.max_key_fec_ratio");
    /**
     * The {@code vid_rc.max_key_frame_mode_bitrate} voip-param.
     */
    public static final VoipParamKey VID_RC_MAX_KEY_FRAME_MODE_BITRATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.max_key_frame_mode_bitrate");
    /**
     * The {@code vid_rc.maxbwe} voip-param.
     */
    public static final VoipParamKey VID_RC_MAXBWE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.maxbwe");
    /**
     * The {@code vid_rc.mcp_set_inflection_point} voip-param.
     */
    public static final VoipParamKey VID_RC_MCP_SET_INFLECTION_POINT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.mcp_set_inflection_point");
    /**
     * The {@code vid_rc.mcp_skip_ml_inference} voip-param.
     */
    public static final VoipParamKey VID_RC_MCP_SKIP_ML_INFERENCE = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.mcp_skip_ml_inference");
    /**
     * The {@code vid_rc.mcp_stop_bitrate_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_MCP_STOP_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.mcp_stop_bitrate_bps");
    /**
     * The {@code vid_rc.mcp_stop_bitrate_inc_pcnt} voip-param.
     */
    public static final VoipParamKey VID_RC_MCP_STOP_BITRATE_INC_PCNT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.mcp_stop_bitrate_inc_pcnt");
    /**
     * The {@code vid_rc.mcp_stop_sbwe_to_pp_pcnt} voip-param.
     */
    public static final VoipParamKey VID_RC_MCP_STOP_SBWE_TO_PP_PCNT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.mcp_stop_sbwe_to_pp_pcnt");
    /**
     * The {@code vid_rc.min_ares_upd_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_MIN_ARES_UPD_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.min_ares_upd_ms");
    /**
     * The {@code vid_rc.min_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MIN_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.min_fec_ratio");
    /**
     * The {@code vid_rc.min_hbh_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MIN_HBH_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.min_hbh_fec_ratio");
    /**
     * The {@code vid_rc.min_key_fec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_MIN_KEY_FEC_RATIO = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.min_key_fec_ratio");
    /**
     * The {@code vid_rc.min_time_ms_bw_audio_br_calls} voip-param.
     */
    public static final VoipParamKey VID_RC_MIN_TIME_MS_BW_AUDIO_BR_CALLS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.min_time_ms_bw_audio_br_calls");
    /**
     * The {@code vid_rc.minbwe} voip-param.
     */
    public static final VoipParamKey VID_RC_MINBWE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.minbwe");
    /**
     * The {@code vid_rc.ml_cong_decrease_pcnt} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_CONG_DECREASE_PCNT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_cong_decrease_pcnt");
    /**
     * The {@code vid_rc.ml_cong_max_decrease_cnt_per_cycle} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_CONG_MAX_DECREASE_CNT_PER_CYCLE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_cong_max_decrease_cnt_per_cycle");
    /**
     * The {@code vid_rc.ml_cong_time_since_last_ramp_down_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_CONG_TIME_SINCE_LAST_RAMP_DOWN_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_cong_time_since_last_ramp_down_ms");
    /**
     * The {@code vid_rc.ml_udst_cap_fraction} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_CAP_FRACTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_cap_fraction");
    /**
     * The {@code vid_rc.ml_udst_cap_margin} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_CAP_MARGIN = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.ml_udst_cap_margin");
    /**
     * The {@code vid_rc.ml_udst_check_pp} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_CHECK_PP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_check_pp");
    /**
     * The {@code vid_rc.ml_udst_encode_margin} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_ENCODE_MARGIN = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.ml_udst_encode_margin");
    /**
     * The {@code vid_rc.ml_udst_min_bitrate_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_MIN_BITRATE_BPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_min_bitrate_bps");
    /**
     * The {@code vid_rc.ml_udst_min_pp_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_MIN_PP_BPS = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.ml_udst_min_pp_bps");
    /**
     * The {@code vid_rc.ml_udst_model_type} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_MODEL_TYPE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_model_type");
    /**
     * The {@code vid_rc.ml_udst_pp_noise_to_mean_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_PP_NOISE_TO_MEAN_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_pp_noise_to_mean_ratio");
    /**
     * The {@code vid_rc.ml_udst_ratio_calc_min_cnt} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_RATIO_CALC_MIN_CNT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_ratio_calc_min_cnt");
    /**
     * The {@code vid_rc.ml_udst_ratio_calc_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_RATIO_CALC_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_ratio_calc_threshold");
    /**
     * The {@code vid_rc.ml_udst_ratio_calc_window_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_RATIO_CALC_WINDOW_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_ratio_calc_window_ms");
    /**
     * The {@code vid_rc.ml_udst_skip_if_ml_plc_in_effect} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_SKIP_IF_ML_PLC_IN_EFFECT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_skip_if_ml_plc_in_effect");
    /**
     * The {@code vid_rc.ml_udst_tgt_br_fraction} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_TGT_BR_FRACTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_tgt_br_fraction");
    /**
     * The {@code vid_rc.ml_udst_time_since_last_ramp_down_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_TIME_SINCE_LAST_RAMP_DOWN_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_time_since_last_ramp_down_ms");
    /**
     * The {@code vid_rc.ml_udst_time_since_last_ramp_up_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_ML_UDST_TIME_SINCE_LAST_RAMP_UP_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ml_udst_time_since_last_ramp_up_ms");
    /**
     * The {@code vid_rc.mludst_start_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_MLUDST_START_BPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.mludst_start_bps");
    /**
     * The {@code vid_rc.nm_pair_c2c_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_NM_PAIR_C2C_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.nm_pair_c2c_ml_feature_val");
    /**
     * The {@code vid_rc.nm_pair_c2w_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_NM_PAIR_C2W_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.nm_pair_c2w_ml_feature_val");
    /**
     * The {@code vid_rc.nm_pair_w2c_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_NM_PAIR_W2C_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.nm_pair_w2c_ml_feature_val");
    /**
     * The {@code vid_rc.nm_pair_w2w_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_NM_PAIR_W2W_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.nm_pair_w2w_ml_feature_val");
    /**
     * The {@code vid_rc.no_process_ignored_rd} voip-param.
     */
    public static final VoipParamKey VID_RC_NO_PROCESS_IGNORED_RD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.no_process_ignored_rd");
    /**
     * The {@code vid_rc.openh264_allow_key_frame_drop} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_ALLOW_KEY_FRAME_DROP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_allow_key_frame_drop");
    /**
     * The {@code vid_rc.openh264_complexity} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_COMPLEXITY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_complexity");
    /**
     * The {@code vid_rc.openh264_enable_cross_me_for_screenshare} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_ENABLE_CROSS_ME_FOR_SCREENSHARE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_enable_cross_me_for_screenshare");
    /**
     * The {@code vid_rc.openh264_enable_frame_skip} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_ENABLE_FRAME_SKIP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_enable_frame_skip");
    /**
     * The {@code vid_rc.openh264_enable_non_5multiple_fps} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_ENABLE_NON_5MULTIPLE_FPS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_enable_non_5multiple_fps");
    /**
     * The {@code vid_rc.openh264_enable_simd_psnr} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_ENABLE_SIMD_PSNR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_enable_simd_psnr");
    /**
     * The {@code vid_rc.openh264_fix_gom_calculation} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_FIX_GOM_CALCULATION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_fix_gom_calculation");
    /**
     * The {@code vid_rc.openh264_fix_preframe_skip} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_FIX_PREFRAME_SKIP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_fix_preframe_skip");
    /**
     * The {@code vid_rc.openh264_idr_bitrate_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_IDR_BITRATE_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_idr_bitrate_ratio");
    /**
     * The {@code vid_rc.openh264_interfrm_thrd_for_idr_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_INTERFRM_THRD_FOR_IDR_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_interfrm_thrd_for_idr_ratio");
    /**
     * The {@code vid_rc.openh264_ltr_marking_period_in_frames} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_LTR_MARKING_PERIOD_IN_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_ltr_marking_period_in_frames");
    /**
     * The {@code vid_rc.openh264_ltr_ref_list_size} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_LTR_REF_LIST_SIZE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_ltr_ref_list_size");
    /**
     * The {@code vid_rc.openh264_max_qp} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_MAX_QP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_max_qp");
    /**
     * The {@code vid_rc.openh264_min_qp} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_MIN_QP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_min_qp");
    /**
     * The {@code vid_rc.openh264_no_periodic_key_frame} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_NO_PERIODIC_KEY_FRAME = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_no_periodic_key_frame");
    /**
     * The {@code vid_rc.openh264_num_ltr_frames} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_NUM_LTR_FRAMES = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_num_ltr_frames");
    /**
     * The {@code vid_rc.openh264_num_temporal_layers} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_NUM_TEMPORAL_LAYERS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_num_temporal_layers");
    /**
     * The {@code vid_rc.openh264_screen_share_enable_qp_reconfig} voip-param.
     */
    public static final VoipParamKey VID_RC_OPENH264_SCREEN_SHARE_ENABLE_QP_RECONFIG = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.openh264_screen_share_enable_qp_reconfig");
    /**
     * The {@code vid_rc.oscillating_width_fix} voip-param.
     */
    public static final VoipParamKey VID_RC_OSCILLATING_WIDTH_FIX = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.oscillating_width_fix");
    /**
     * The {@code vid_rc.packet_loss_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_PACKET_LOSS_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.packet_loss_mode");
    /**
     * The {@code vid_rc.pip_mode_bitrate_estimation_action} voip-param.
     */
    public static final VoipParamKey VID_RC_PIP_MODE_BITRATE_ESTIMATION_ACTION = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pip_mode_bitrate_estimation_action");
    /**
     * The {@code vid_rc.pip_sbwe_reset_max_cap} voip-param.
     */
    public static final VoipParamKey VID_RC_PIP_SBWE_RESET_MAX_CAP = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.pip_sbwe_reset_max_cap");
    /**
     * The {@code vid_rc.pip_update_fix} voip-param.
     */
    public static final VoipParamKey VID_RC_PIP_UPDATE_FIX = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pip_update_fix");
    /**
     * The {@code vid_rc.pkt_loss_threshold_in_milliseconds} voip-param.
     */
    public static final VoipParamKey VID_RC_PKT_LOSS_THRESHOLD_IN_MILLISECONDS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pkt_loss_threshold_in_milliseconds");
    /**
     * The {@code vid_rc.pkt_loss_threshold_in_packets} voip-param.
     */
    public static final VoipParamKey VID_RC_PKT_LOSS_THRESHOLD_IN_PACKETS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pkt_loss_threshold_in_packets");
    /**
     * The {@code vid_rc.pli_quick_send} voip-param.
     */
    public static final VoipParamKey VID_RC_PLI_QUICK_SEND = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pli_quick_send");
    /**
     * The {@code vid_rc.pp_noise_to_mean_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_PP_NOISE_TO_MEAN_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pp_noise_to_mean_ratio");
    /**
     * The {@code vid_rc.pp_udst_min_link_cap_bps} voip-param.
     */
    public static final VoipParamKey VID_RC_PP_UDST_MIN_LINK_CAP_BPS = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.pp_udst_min_link_cap_bps");
    /**
     * The {@code vid_rc.pp_udst_peer_vid_rx_to_sbwe_pcnt} voip-param.
     */
    public static final VoipParamKey VID_RC_PP_UDST_PEER_VID_RX_TO_SBWE_PCNT = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.pp_udst_peer_vid_rx_to_sbwe_pcnt");
    /**
     * The {@code vid_rc.pp_udst_sbwe_to_link_cap_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_PP_UDST_SBWE_TO_LINK_CAP_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.pp_udst_sbwe_to_link_cap_ratio");
    /**
     * The {@code vid_rc.ramp_down_av1_idr} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_AV1_IDR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_av1_idr");
    /**
     * The {@code vid_rc.ramp_down_av1_maxqp} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_AV1_MAXQP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_av1_maxqp");
    /**
     * The {@code vid_rc.ramp_down_av1_overshoot} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_AV1_OVERSHOOT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_av1_overshoot");
    /**
     * The {@code vid_rc.ramp_down_av1_undershoot} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_AV1_UNDERSHOOT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_av1_undershoot");
    /**
     * The {@code vid_rc.ramp_down_openh264_idr} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_OPENH264_IDR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_openh264_idr");
    /**
     * The {@code vid_rc.ramp_down_openh264_maxqp} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_OPENH264_MAXQP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_openh264_maxqp");
    /**
     * The {@code vid_rc.ramp_down_switches_threshold} voip-param.
     */
    public static final VoipParamKey VID_RC_RAMP_DOWN_SWITCHES_THRESHOLD = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ramp_down_switches_threshold");
    /**
     * The {@code vid_rc.random_forced_probing_inc_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_RANDOM_FORCED_PROBING_INC_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.random_forced_probing_inc_ratio");
    /**
     * The {@code vid_rc.random_forced_probing_max_time_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_RANDOM_FORCED_PROBING_MAX_TIME_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.random_forced_probing_max_time_ms");
    /**
     * The {@code vid_rc.random_forced_probing_min_interval_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_RANDOM_FORCED_PROBING_MIN_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.random_forced_probing_min_interval_ms");
    /**
     * The {@code vid_rc.random_forced_probing_prob_perc} voip-param.
     */
    public static final VoipParamKey VID_RC_RANDOM_FORCED_PROBING_PROB_PERC = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.random_forced_probing_prob_perc");
    /**
     * The {@code vid_rc.random_forced_probing_probation_after_stop_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_RANDOM_FORCED_PROBING_PROBATION_AFTER_STOP_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.random_forced_probing_probation_after_stop_ms");
    /**
     * The {@code vid_rc.rc_policy} voip-param.
     */
    public static final VoipParamKey VID_RC_RC_POLICY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.rc_policy");
    /**
     * The {@code vid_rc.rd_int_only_plr_rtt} voip-param.
     */
    public static final VoipParamKey VID_RC_RD_INT_ONLY_PLR_RTT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.rd_int_only_plr_rtt");
    /**
     * The {@code vid_rc.real_audio_bps_quant} voip-param.
     */
    public static final VoipParamKey VID_RC_REAL_AUDIO_BPS_QUANT = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.real_audio_bps_quant");
    /**
     * The {@code vid_rc.remote_est_weight} voip-param.
     */
    public static final VoipParamKey VID_RC_REMOTE_EST_WEIGHT = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.remote_est_weight");
    /**
     * The {@code vid_rc.rexmt_threshold_rtt_multiplier} voip-param.
     */
    public static final VoipParamKey VID_RC_REXMT_THRESHOLD_RTT_MULTIPLIER = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.rexmt_threshold_rtt_multiplier");
    /**
     * The {@code vid_rc.sbwe_mcp_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_SBWE_MCP_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sbwe_mcp_mode");
    /**
     * The {@code vid_rc.sbwe_mcp_skip_additive_ramp_up} voip-param.
     */
    public static final VoipParamKey VID_RC_SBWE_MCP_SKIP_ADDITIVE_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sbwe_mcp_skip_additive_ramp_up");
    /**
     * The {@code vid_rc.sbwe_mcp_skip_ramp_up_pause} voip-param.
     */
    public static final VoipParamKey VID_RC_SBWE_MCP_SKIP_RAMP_UP_PAUSE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sbwe_mcp_skip_ramp_up_pause");
    /**
     * The {@code vid_rc.sbwe_mcp_skip_remote_bwe} voip-param.
     */
    public static final VoipParamKey VID_RC_SBWE_MCP_SKIP_REMOTE_BWE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sbwe_mcp_skip_remote_bwe");
    /**
     * The {@code vid_rc.second_kf_after_reset} voip-param.
     */
    public static final VoipParamKey VID_RC_SECOND_KF_AFTER_RESET = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.second_kf_after_reset");
    /**
     * The {@code vid_rc.second_kf_interval_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_SECOND_KF_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.second_kf_interval_ms");
    /**
     * The {@code vid_rc.sender_dec_ratio} voip-param.
     */
    public static final VoipParamKey VID_RC_SENDER_DEC_RATIO = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sender_dec_ratio");
    /**
     * The {@code vid_rc.sender_side_rc_min_adjustment_interval_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_SENDER_SIDE_RC_MIN_ADJUSTMENT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sender_side_rc_min_adjustment_interval_ms");
    /**
     * The {@code vid_rc.sender_side_rc_min_rd_adjustment_interval_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_SENDER_SIDE_RC_MIN_RD_ADJUSTMENT_INTERVAL_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sender_side_rc_min_rd_adjustment_interval_ms");
    /**
     * The {@code vid_rc.sfu_reorder_tolerance_ms} voip-param.
     */
    public static final VoipParamKey VID_RC_SFU_REORDER_TOLERANCE_MS = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sfu_reorder_tolerance_ms");
    /**
     * The {@code vid_rc.single_ml_driven_ramp_up} voip-param.
     */
    public static final VoipParamKey VID_RC_SINGLE_ML_DRIVEN_RAMP_UP = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.single_ml_driven_ramp_up");
    /**
     * The {@code vid_rc.skip_bwe_dyn_param_in_main_update} voip-param.
     */
    public static final VoipParamKey VID_RC_SKIP_BWE_DYN_PARAM_IN_MAIN_UPDATE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.skip_bwe_dyn_param_in_main_update");
    /**
     * The {@code vid_rc.sru_fall_back_ceiling_mode} voip-param.
     */
    public static final VoipParamKey VID_RC_SRU_FALL_BACK_CEILING_MODE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.sru_fall_back_ceiling_mode");
    /**
     * The {@code vid_rc.ss_downscale_with_buffer_check} voip-param.
     */
    public static final VoipParamKey VID_RC_SS_DOWNSCALE_WITH_BUFFER_CHECK = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ss_downscale_with_buffer_check");
    /**
     * The {@code vid_rc.ss_encoder_max_res_tolerance} voip-param.
     */
    public static final VoipParamKey VID_RC_SS_ENCODER_MAX_RES_TOLERANCE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ss_encoder_max_res_tolerance");
    /**
     * The {@code vid_rc.threshold_fps_pct_for_preset_change} voip-param.
     */
    public static final VoipParamKey VID_RC_THRESHOLD_FPS_PCT_FOR_PRESET_CHANGE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.threshold_fps_pct_for_preset_change");
    /**
     * The {@code vid_rc.ul_sender_loss_high} voip-param.
     */
    public static final VoipParamKey VID_RC_UL_SENDER_LOSS_HIGH = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ul_sender_loss_high");
    /**
     * The {@code vid_rc.ul_sender_loss_low} voip-param.
     */
    public static final VoipParamKey VID_RC_UL_SENDER_LOSS_LOW = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.ul_sender_loss_low");
    /**
     * The {@code vid_rc.undershoot_a2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_A2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_a2a_ml_feature_val");
    /**
     * The {@code vid_rc.undershoot_a2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_A2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_a2i_ml_feature_val");
    /**
     * The {@code vid_rc.undershoot_default_platform_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_DEFAULT_PLATFORM_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_default_platform_feature_val");
    /**
     * The {@code vid_rc.undershoot_i2a_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_I2A_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_i2a_ml_feature_val");
    /**
     * The {@code vid_rc.undershoot_i2i_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_I2I_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_i2i_ml_feature_val");
    /**
     * The {@code vid_rc.undershoot_web_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UNDERSHOOT_WEB_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.undershoot_web_ml_feature_val");
    /**
     * The {@code vid_rc.unify_enc_cpu_complexity} voip-param.
     */
    public static final VoipParamKey VID_RC_UNIFY_ENC_CPU_COMPLEXITY = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.unify_enc_cpu_complexity");
    /**
     * The {@code vid_rc.update_fps_enc_preset_interval} voip-param.
     */
    public static final VoipParamKey VID_RC_UPDATE_FPS_ENC_PRESET_INTERVAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.update_fps_enc_preset_interval");
    /**
     * The {@code vid_rc.uplink_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_UPLINK_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.uplink_ml_feature_val");
    /**
     * The {@code vid_rc.use_audio_module_br} voip-param.
     */
    public static final VoipParamKey VID_RC_USE_AUDIO_MODULE_BR = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.use_audio_module_br");
    /**
     * The {@code vid_rc.use_channel_buf_for_fec_encoding} voip-param.
     */
    public static final VoipParamKey VID_RC_USE_CHANNEL_BUF_FOR_FEC_ENCODING = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.use_channel_buf_for_fec_encoding");
    /**
     * The {@code vid_rc.vid_nack_renack_probe_enabled} voip-param.
     */
    public static final VoipParamKey VID_RC_VID_NACK_RENACK_PROBE_ENABLED = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.vid_nack_renack_probe_enabled");
    /**
     * The {@code vid_rc.vpx_bg_delta_qp} voip-param.
     */
    public static final VoipParamKey VID_RC_VPX_BG_DELTA_QP = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.vpx_bg_delta_qp");
    /**
     * The {@code vid_rc.vpx_cpu} voip-param.
     */
    public static final VoipParamKey VID_RC_VPX_CPU = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.vpx_cpu");
    /**
     * The {@code vid_rc.vpx_roi_delta_qp} voip-param.
     */
    public static final VoipParamKey VID_RC_VPX_ROI_DELTA_QP = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.vpx_roi_delta_qp");
    /**
     * The {@code vid_rc.wa_pid_controller_rtt_ramp_up_enable} voip-param.
     */
    public static final VoipParamKey VID_RC_WA_PID_CONTROLLER_RTT_RAMP_UP_ENABLE = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.wa_pid_controller_rtt_ramp_up_enable");
    /**
     * The {@code vid_rc.wa_pid_controller_rtt_ramp_up_inc_adjust_enable} voip-param.
     */
    public static final VoipParamKey VID_RC_WA_PID_CONTROLLER_RTT_RAMP_UP_INC_ADJUST_ENABLE = new VoipParamKey(VoipParamType.FLOAT, "vid_rc.wa_pid_controller_rtt_ramp_up_inc_adjust_enable");
    /**
     * The {@code vid_rc.web_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_WEB_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.web_ml_feature_val");
    /**
     * The {@code vid_rc.wifi_ml_feature_val} voip-param.
     */
    public static final VoipParamKey VID_RC_WIFI_ML_FEATURE_VAL = new VoipParamKey(VoipParamType.INTEGER, "vid_rc.wifi_ml_feature_val");
    /**
     * The {@code voip_settings_version.release_type} voip-param.
     */
    public static final VoipParamKey VOIP_SETTINGS_VERSION_RELEASE_TYPE = new VoipParamKey(VoipParamType.INTEGER, "voip_settings_version.release_type");
    /**
     * The {@code voip_settings_version.version_number} voip-param.
     */
    public static final VoipParamKey VOIP_SETTINGS_VERSION_VERSION_NUMBER = new VoipParamKey(VoipParamType.INTEGER, "voip_settings_version.version_number");

    /**
     * The wire-path lookup over every generated constant.
     *
     * <p>Maps each {@code section.key} wire path to its constant. The catalogue holds one
     * constant per distinct wire path, so the map is collision-free on keys.
     */
    private static final Map<String, VoipParamKey> BY_WIRE_PATH = buildByWirePath();

    /**
     * Builds the wire-path lookup by reflecting over this record's generated constants.
     *
     * <p>Reflection is used deliberately: listing the constants explicitly here would re-add
     * thousands of field references to the class initializer and overflow the 64KB method-size
     * limit. Each {@code public static final VoipParamKey} field is read once and indexed by its
     * wire path.
     *
     * @return the unmodifiable wire-path lookup
     */
    private static Map<String, VoipParamKey> buildByWirePath() {
        var map = new HashMap<String, VoipParamKey>();
        for (var field : VoipParamKey.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != VoipParamKey.class) {
                continue;
            }
            try {
                var key = (VoipParamKey) field.get(null);
                map.put(key.wirePath(), key);
            } catch (IllegalAccessException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
        return Map.copyOf(map);
    }

    /**
     * Returns every modelled key.
     *
     * <p>The returned collection is an unmodifiable view over the wire-path lookup's values; it
     * is not a precomputed copy.
     *
     * @return an unmodifiable view of all modelled keys
     */
    public static Collection<VoipParamKey> values() {
        return BY_WIRE_PATH.values();
    }

    /**
     * Returns the modelled key whose {@linkplain #wirePath() wire path} equals the given value.
     *
     * @param wirePath the area-sectioned {@code section.key} wire path to resolve
     * @return the matching key, or {@link Optional#empty()} if the path is not modelled
     */
    public static Optional<VoipParamKey> ofWirePath(String wirePath) {
        return Optional.ofNullable(BY_WIRE_PATH.get(wirePath));
    }

    /**
     * Returns an unknown key for a wire path that no modelled constant covers.
     *
     * <p>The deserializer keys a {@code <voip_settings>} leaf whose wire path is not modelled
     * under such a key (a field added after this module revision, or one whose wire path could
     * not be reconstructed), so its parsed value is never dropped. The key carries the leaf's
     * wire path and {@link VoipParamType#UNKNOWN}; its value is read back through the coercing
     * {@link VoipParams} accessors like any other key.
     *
     * @param wirePath the area-sectioned {@code section.key} wire path the leaf was carried under
     * @return an unknown key for the given wire path
     */
    public static VoipParamKey unknown(String wirePath) {
        return new VoipParamKey(VoipParamType.UNKNOWN, wirePath);
    }
}
