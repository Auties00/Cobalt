package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.engine.control.event.PlayCallTone;

/**
 * Enumerates the call tones the engine can play, each carrying a priority bit in a tone bitmask.
 *
 * <p>The engine tracks every tone that wants to play as a bit in a single bitmask and plays only the
 * highest priority tone currently set, so a ringback gives way to a busy signal and a connecting tone
 * gives way to either; the chosen tone is what the engine surfaces through the {@link PlayCallTone}
 * event. Each constant binds the {@link #bit() priority bit} the engine ORs into the mask when its tone
 * becomes active and clears when it stops. The {@link #NONE} constant carries bit {@code 0} and
 * represents silence: the value the mask collapses to when no tone is active.
 *
 * <p>Priority is encoded by bit position, not by enum order: a numerically larger bit is a higher
 * priority tone, so {@link #highestPriority(int)} selects the active tone by taking the highest set bit
 * of the mask. The bits are NOT a contiguous run; they skip values (there is no tone at bit {@code 0x2},
 * {@code 0x4} is interruption, {@code 0x8} is offer sent, and the incoming and group tones jump to
 * {@code 0x100} and {@code 0x200}).
 *
 * @implNote This implementation assigns each tone a single priority bit and selects the tone to play by
 * isolating the highest set bit of the mask in {@link #highestPriority(int)}. The bit values are
 * {@code NONE=0}, {@code CONNECTING=1}, {@code RECONNECTING=2}, {@code INTERRUPTION=4},
 * {@code OFFER_SENT=8}, {@code RINGBACK=0x10}, {@code BUSY=0x20}, {@code VIDEO_UPGRADE_REQUEST=0x40},
 * {@code INCOMING_PENDING_CALL=0x100}, and {@code GROUP_CALL_EVENT_SHORT_TONE=0x200}; the slot at
 * {@code 0x80} between the video upgrade request tone and the incoming call tone is left undefined.
 */
public enum ToneType {
    /**
     * Represents silence: the value the tone bitmask collapses to when no tone is active.
     */
    NONE(0x000),

    /**
     * Represents the tone played while a call is being connected.
     */
    CONNECTING(0x001),

    /**
     * Represents the tone played while an active call is reconnecting after a network path loss.
     */
    RECONNECTING(0x002),

    /**
     * Represents the tone played when the call is interrupted by another event, such as an incoming
     * cellular call.
     */
    INTERRUPTION(0x004),

    /**
     * Represents the tone played to the caller once an outbound offer has been sent.
     */
    OFFER_SENT(0x008),

    /**
     * Represents the ringback tone played to the caller while the callee is alerting.
     */
    RINGBACK(0x010),

    /**
     * Represents the busy tone played when the callee cannot take the call.
     */
    BUSY(0x020),

    /**
     * Represents the tone played while a video upgrade request is pending.
     */
    VIDEO_UPGRADE_REQUEST(0x040),

    /**
     * Represents the tone played for an incoming pending call awaiting the local user's answer.
     */
    INCOMING_PENDING_CALL(0x100),

    /**
     * Represents the short tone played for a group call event, such as a participant joining or leaving.
     */
    GROUP_CALL_EVENT_SHORT_TONE(0x200);

    /**
     * Holds the priority bit the engine ORs into the tone bitmask for this tone.
     */
    private final int bit;

    /**
     * Constructs a tone constant bound to its priority bit.
     *
     * @param bit the priority bit the engine ORs into the tone bitmask
     */
    ToneType(int bit) {
        this.bit = bit;
    }

    /**
     * Returns the priority bit the engine ORs into the tone bitmask for this tone.
     *
     * <p>A numerically larger bit denotes a higher priority tone; {@link #NONE} carries bit {@code 0}.
     *
     * @return the priority bit for this tone
     */
    public int bit() {
        return bit;
    }

    /**
     * Returns the highest priority tone set in a tone bitmask.
     *
     * <p>The active tone is the highest set bit of the mask; the engine plays only that one. A mask with
     * no defined tone bits set, including the empty mask, resolves to {@link #NONE}. A mask whose highest
     * set bit is an undefined slot (for example a stray {@code 0x80}) is masked down to the highest
     * defined tone bit at or below it, so an undefined bit never suppresses a real tone.
     *
     * @param mask the tone bitmask, an OR of the bits of the currently active tones
     * @return the highest priority active tone, or {@link #NONE} when the mask has no defined tone set
     */
    public static ToneType highestPriority(int mask) {
        var best = NONE;
        for (var tone : values()) {
            if (tone.bit != 0 && (mask & tone.bit) == tone.bit && tone.bit > best.bit) {
                best = tone;
            }
        }
        return best;
    }
}
