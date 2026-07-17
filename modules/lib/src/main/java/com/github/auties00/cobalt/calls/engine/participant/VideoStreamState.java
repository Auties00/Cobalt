package com.github.auties00.cobalt.calls.engine.participant;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the video stream states the wa-voip engine tracks for each participant.
 *
 * <p>This is the {@code kVideoState*} machine the engine drives for every participant's
 * video stream. It covers the simple on or off lifecycle ({@link #DISABLED},
 * {@link #ENABLED}, {@link #PAUSED}, {@link #STOPPED}), the full video upgrade
 * negotiation (request, accept, reject, cancel, their timeout variants, and the v2
 * request newer peers use), the avatar codec enablement signalled on XR2D devices, and
 * the terminal error sentinel. The engine projects a participant's video state to one of
 * these values when reading it back through the participant view; a participant with no
 * resolved detail block reads as {@link #UNKNOWN_PEER}.
 *
 * <p>Each constant carries the exact {@link #wireOrdinal() wire ordinal} the engine
 * stores and transmits. The ordinals are not contiguous: they occupy {@code 0..12}
 * densely and then jump to {@code 20} for {@link #ERROR}, leaving the {@code 13..19}
 * slots unused as padding in the engine lookup table. {@link #ENABLED} is the video on
 * state and {@link #DISABLED} the video off state; {@link #PAUSED} and {@link #STOPPED}
 * are both treated as not actively sending by the engine, which tests them together via
 * the bit pattern {@code (value & ~4) == 2}.
 *
 * @implNote This implementation stores the engine wire ordinal separately from the Java
 * {@link #ordinal()} because the two diverge: the wire ordinals run {@code 0..12} for
 * {@link #DISABLED} through {@link #XR2D_CODEC_AVATAR_ENABLED} and then skip to
 * {@code 20} for {@link #ERROR}, so {@link #ofWireOrdinal(int)} decodes by scanning the
 * stored wire values rather than indexing {@link #values()}. The {@code 13..19} wire
 * ordinals carry no state and resolve to {@link #UNKNOWN_PEER}.
 */
public enum VideoStreamState {
    /**
     * Indicates the participant's camera is off and no video stream is active.
     *
     * <p>This is the default state for a participant that has not enabled video. It is
     * also the value the engine name table assigns to every unused padding slot, so it
     * is the only state with multiple table entries pointing at its name.
     */
    DISABLED(0),

    /**
     * Indicates the participant's camera is on and a video stream is active.
     *
     * <p>This is also the state read back for a call extension such as a bot or avatar
     * stream, which the engine always treats as video enabled.
     */
    ENABLED(1),

    /**
     * Indicates the participant's video stream is temporarily paused.
     *
     * <p>The engine groups this with {@link #STOPPED} as a state that is not actively
     * sending through the predicate {@code (value & ~4) == 2}; a paused stream can resume
     * without a fresh upgrade negotiation.
     */
    PAUSED(2),

    /**
     * Indicates a request to upgrade an audio only call to video has been issued.
     *
     * <p>This is the initiating state of the video upgrade handshake; the peer answers
     * with {@link #UPGRADE_ACCEPT} or {@link #UPGRADE_REJECT}, and the request expires
     * via {@link #UPGRADE_REJECT_BY_TIMEOUT} if it goes unanswered.
     */
    UPGRADE_REQUEST(3),

    /**
     * Indicates a peer accepted a pending video upgrade request.
     *
     * <p>Acceptance transitions both sides toward an active video stream.
     */
    UPGRADE_ACCEPT(4),

    /**
     * Indicates a peer declined a pending video upgrade request.
     */
    UPGRADE_REJECT(5),

    /**
     * Indicates the participant's video stream has been stopped.
     *
     * <p>The engine groups this with {@link #PAUSED} as a state that is not actively
     * sending through the predicate {@code (value & ~4) == 2}; it is the value set when a
     * peer's video is observed to have stopped.
     */
    STOPPED(6),

    /**
     * Indicates a pending video upgrade request was rejected because it timed out.
     *
     * <p>This is the timeout counterpart of {@link #UPGRADE_REJECT}: the engine reaches
     * it when the requester receives no answer within the upgrade window.
     */
    UPGRADE_REJECT_BY_TIMEOUT(7),

    /**
     * Indicates the requester cancelled a pending video upgrade request.
     *
     * <p>This is the requester side counterpart of {@link #UPGRADE_REJECT}: the side that
     * issued {@link #UPGRADE_REQUEST} withdraws it before the peer answers.
     */
    UPGRADE_CANCEL(8),

    /**
     * Indicates a pending video upgrade request was cancelled because it timed out.
     *
     * <p>This is the timeout counterpart of {@link #UPGRADE_CANCEL}.
     */
    UPGRADE_CANCEL_BY_TIMEOUT(9),

    /**
     * Indicates the video state belongs to a peer the engine cannot resolve.
     *
     * <p>This is the value read back for a participant whose detail block is absent; the
     * participant view video state getter returns this ordinal as its default.
     */
    UNKNOWN_PEER(10),

    /**
     * Indicates a video upgrade request issued through the newer v2 upgrade flow.
     *
     * <p>Peers that advertise the v2 capability use this in place of
     * {@link #UPGRADE_REQUEST}; the rest of the handshake is unchanged.
     */
    UPGRADE_REQUEST_V2(11),

    /**
     * Indicates the participant's video stream is an XR2D codec avatar stream.
     *
     * <p>This is the enabled state for a participant rendering through the XR2D avatar
     * codec path rather than a standard camera capture.
     */
    XR2D_CODEC_AVATAR_ENABLED(12),

    /**
     * Indicates the participant's video stream is in a terminal error state.
     *
     * <p>This is the only state whose ordinal lies outside the dense {@code 0..12} band;
     * the engine name table places it at ordinal {@code 20}.
     */
    ERROR(20);

    /**
     * Caches the constant array so the {@link #ofWireOrdinal(int)} decode scan does not
     * pay the defensive clone cost of {@link #values()} on every video state lookup.
     */
    private static final VideoStreamState[] VALUES = values();

    /**
     * Resolves an engine wire ordinal to its state, backing {@link #ofWireOrdinal(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #wireOrdinal}, so a wire ordinal
     * resolves to its state in constant time rather than by scanning {@link #VALUES}. A wire ordinal with
     * no entry, including the unused {@code 13..19} padding slots, falls back to {@link #UNKNOWN_PEER} in
     * {@link #ofWireOrdinal(int)}.
     */
    private static final Map<Integer, VideoStreamState> BY_WIRE_ORDINAL;

    static {
        var byWireOrdinal = new HashMap<Integer, VideoStreamState>();
        for (var state : VALUES) {
            if (byWireOrdinal.put(state.wireOrdinal, state) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_WIRE_ORDINAL = Map.copyOf(byWireOrdinal);
    }

    /**
     * The integer value the wa-voip engine stores and transmits for this state.
     */
    private final int wireOrdinal;

    /**
     * Constructs a state constant bound to its engine wire ordinal.
     *
     * @param wireOrdinal the integer value the engine uses for this state
     */
    VideoStreamState(int wireOrdinal) {
        this.wireOrdinal = wireOrdinal;
    }

    /**
     * Returns the integer value the wa-voip engine stores and transmits for this state.
     *
     * <p>The returned value is the engine ordinal, not the Java {@link #ordinal()}; the
     * two diverge for {@link #ERROR}, whose wire ordinal is {@code 20} while its Java
     * ordinal is {@code 13}.
     *
     * @return the engine wire ordinal for this state
     */
    public int wireOrdinal() {
        return wireOrdinal;
    }

    /**
     * Returns the state whose {@linkplain #wireOrdinal() wire ordinal} equals the given
     * value.
     *
     * <p>Any value that does not correspond to a defined state, including the unused
     * {@code 13..19} padding slots, resolves to {@link #UNKNOWN_PEER}, mirroring the
     * engine's treatment of an unresolved or out of range video state.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_WIRE_ORDINAL} map rather than
     * scanning {@link #VALUES}.
     * @param wireOrdinal the engine wire ordinal to resolve
     * @return the matching state, or {@link #UNKNOWN_PEER} if no state matches
     */
    public static VideoStreamState ofWireOrdinal(int wireOrdinal) {
        var state = BY_WIRE_ORDINAL.get(wireOrdinal);
        return state != null ? state : UNKNOWN_PEER;
    }

    /**
     * Returns whether this state represents a stream that is paused or stopped.
     *
     * <p>The result is {@code true} for exactly {@link #PAUSED} and {@link #STOPPED},
     * matching the engine predicate {@code (wireOrdinal & ~4) == 2} that treats both as
     * video states that are not actively sending.
     *
     * @return {@code true} if this state is {@link #PAUSED} or {@link #STOPPED},
     *         {@code false} otherwise
     */
    public boolean isPausedOrStopped() {
        return (wireOrdinal & ~4) == 2;
    }
}
