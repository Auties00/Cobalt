package com.github.auties00.cobalt.calls.media.audio.neteq;

/**
 * Enumerates the decisions the {@link DecisionLogic} returns and {@link LiveNetEq} dispatches on each
 * playback pull, one per rendered audio frame.
 *
 * <p>On every playback pull period the decision logic compares the current buffer level against the
 * {@link DelayManager} target and the availability of the next packet, then returns exactly one of these
 * operations. {@link #NORMAL} decodes the scheduled packet untouched; {@link #ACCELERATE} and
 * {@link #FAST_ACCELERATE} time compress decoded audio to drain a buffer holding too much audio;
 * {@link #PREEMPTIVE_EXPAND} time stretches decoded audio to build up a buffer holding too little;
 * {@link #EXPAND} synthesizes a concealment frame when the next packet is missing; {@link #MERGE}
 * crossfades a freshly arrived packet into the tail of a preceding expansion; {@link #RFC3389_CNG} and
 * {@link #CODEC_INTERNAL_CNG} generate comfort noise during a discontinuous transmission gap;
 * {@link #CODEC_PLC} delegates concealment to the codec's own loss concealment; {@link #DTMF} renders a
 * telephone event tone; and {@link #UNDEFINED} is the sentinel that is never expected and that the native
 * engine asserts on.
 *
 * <p>This is the internal render vocabulary {@link LiveNetEq} dispatches on directly: each operation
 * maps to a decode, an in band forward error correction reconstruction, a codec or built in concealment, a
 * time stretch, or a comfort noise or silence frame, rendered over the buffer's decoded PCM history before
 * one playback period frame is served.
 *
 * @implNote This implementation carries only the {@code kUndefined} error string from the upstream engine,
 * so the constant order follows upstream WebRTC NetEq with WhatsApp's identical naming. {@link #FAST_ACCELERATE}
 * is the aggressive accelerate variant the engine selects when the buffer is grossly too full, and
 * {@link #CODEC_PLC} is the WhatsApp codec concealment path (captured enabled) that prefers the codec's own
 * concealment over the built in {@link #EXPAND} synthesizer.
 */
public enum NetEqOperation {
    /**
     * Decodes the scheduled packet without time stretching, the steady state operation.
     *
     * <p>Chosen when the buffer level sits near the target and the next packet in sequence is available.
     */
    NORMAL,

    /**
     * Crossfades a freshly decoded packet into the tail of a preceding {@link #EXPAND} output.
     *
     * <p>Chosen on the first packet that arrives after one or more concealment frames, to avoid a
     * discontinuity between the synthesized concealment and the real audio.
     */
    MERGE,

    /**
     * Synthesizes a packet loss concealment frame from past signal when the next packet is missing.
     *
     * <p>Chosen when the scheduled packet has not arrived at pull time; successive expansions attenuate
     * toward an estimated background noise level.
     */
    EXPAND,

    /**
     * Time compresses decoded audio by roughly one pitch period to drain a buffer holding too much audio.
     *
     * <p>Chosen when the buffer level exceeds the target and time stretching is permitted; removes latency
     * without dropping a whole packet.
     */
    ACCELERATE,

    /**
     * Time compresses decoded audio aggressively to drain a buffer holding grossly too much audio.
     *
     * <p>Chosen when the buffer level greatly exceeds the target; the engine variant of
     * {@link #ACCELERATE} that may compress more than one period per frame.
     */
    FAST_ACCELERATE,

    /**
     * Time stretches decoded audio by roughly one pitch period to build up a buffer holding too little audio.
     *
     * <p>Chosen when the buffer level is below the target but the next packet is available, inserting
     * latency to guard against an imminent underrun.
     */
    PREEMPTIVE_EXPAND,

    /**
     * Generates RFC 3389 comfort noise during a discontinuous transmission silence gap.
     *
     * <p>Chosen when a comfort noise (SID) payload governs the gap; renders shaped noise rather than
     * silence so the line does not sound dead.
     */
    RFC3389_CNG,

    /**
     * Generates comfort noise through the codec's own internal generator during a silence gap.
     *
     * <p>Chosen when the active codec carries its own discontinuous transmission comfort noise instead of
     * the separate RFC 3389 path.
     */
    CODEC_INTERNAL_CNG,

    /**
     * Conceals a lost packet through the codec's own packet loss concealment.
     *
     * <p>Chosen, in preference to {@link #EXPAND}, when {@link NetEqConfig#enableCodecPlc()} is set and the
     * codec exposes a concealment entry point; the captured WhatsApp configuration enables this.
     */
    CODEC_PLC,

    /**
     * Renders a DTMF telephone event tone from the buffered event.
     *
     * <p>Chosen when an RFC 4733 telephone event payload is pending; the tone is synthesized rather than
     * decoded as audio.
     */
    DTMF,

    /**
     * The sentinel returned when the decision logic cannot classify the state, never expected in practice.
     *
     * <p>The native engine treats this as a programming error; {@link LiveNetEq} maps it to a silence frame
     * defensively rather than throwing.
     */
    UNDEFINED
}
