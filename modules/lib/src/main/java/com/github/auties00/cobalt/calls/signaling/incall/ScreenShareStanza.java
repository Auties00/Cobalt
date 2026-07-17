package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <screen_share>} action sent during an active call when a participant starts,
 * stops, or fails screen sharing.
 *
 * <p>The action reports a transition in the sender's screen sharing stream. It carries the universal
 * call header, a numeric {@code screenshare_state} code, and an optional numeric {@code version}
 * naming the negotiated screen sharing protocol generation. The state codes are {@link #STATE_STARTED}
 * ({@code 1}, sharing started), {@link #STATE_STOPPED} ({@code 2}, sharing stopped), and
 * {@link #STATE_FAILED} ({@code 3}, sharing failed); the version distinguishes the V2 single stream
 * port swap path from the V3 dual stream auxiliary stream path. The dual stream lifecycle and the per
 * direction request counters are owned by the screen sharing controller, not by this wire record.
 *
 * <p>On the wire the element is
 * {@snippet lang=xml : <screen_share call-id="..." call-creator="..." screenshare_state="N" version="N"/>}
 *
 * @see SignalingType#SCREEN_SHARE
 */
public final class ScreenShareStanza implements InCallActionStanza {
    /**
     * The wire element tag for a screen sharing action.
     */
    public static final String ELEMENT = "screen_share";

    /**
     * The wire attribute naming the screen sharing state code.
     */
    private static final String STATE_ATTRIBUTE = "screenshare_state";

    /**
     * The wire attribute naming the negotiated screen sharing protocol version.
     */
    private static final String VERSION_ATTRIBUTE = "version";

    /**
     * The screen sharing state code reported when sharing starts.
     */
    public static final int STATE_STARTED = 1;

    /**
     * The screen sharing state code reported when sharing stops.
     */
    public static final int STATE_STOPPED = 2;

    /**
     * The screen sharing state code reported when sharing fails.
     */
    public static final int STATE_FAILED = 3;

    /**
     * The call identifier this action's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this action's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * The numeric screen sharing state code ({@code 1} start, {@code 2} stopped, {@code 3} failed).
     */
    private final int state;

    /**
     * The negotiated screen sharing protocol version, or {@code -1} when absent.
     */
    private final int version;

    /**
     * Constructs a screen sharing action.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param state       the numeric screen sharing state code ({@code 1} start, {@code 2} stopped,
     *                    {@code 3} failed)
     * @param version     the negotiated screen sharing protocol version, or {@code -1} when absent
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public ScreenShareStanza(String callId, Jid callCreator, int state, int version) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.state = state;
        this.version = version;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a screen sharing action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a screen sharing action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the numeric screen sharing state code.
     *
     * @return the state code ({@code 1} start, {@code 2} stopped, {@code 3} failed)
     */
    public int state() {
        return state;
    }

    /**
     * Returns the negotiated screen sharing protocol version, or {@code -1} when absent.
     *
     * @return the version, or {@code -1} when the action carries none
     */
    public int version() {
        return version;
    }

    /**
     * Returns the negotiated screen sharing protocol version, if present.
     *
     * @return an {@link OptionalInt} holding the version, or empty when the action carries none
     */
    public OptionalInt versionValue() {
        return version < 0 ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#SCREEN_SHARE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.SCREEN_SHARE;
    }

    /**
     * Builds the {@code <screen_share call-id call-creator screenshare_state version/>} action stanza.
     *
     * <p>An absent {@code version} is omitted from the stanza.
     *
     * @return the screen sharing action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(STATE_ATTRIBUTE, state)
                .attribute(VERSION_ATTRIBUTE, version, version >= 0)
                .build();
    }

    /**
     * Decodes a {@code <screen_share>} action stanza into a {@link ScreenShareStanza}.
     *
     * <p>An absent {@code screenshare_state} decodes to {@code 0}; an absent {@code version} decodes
     * to {@code -1}.
     *
     * @param stanza the {@code <screen_share>} stanza
     * @return the decoded screen sharing action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static ScreenShareStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var state = stanza.getAttributeAsInt(STATE_ATTRIBUTE, 0);
        var version = stanza.getAttributeAsInt(VERSION_ATTRIBUTE, -1);
        return new ScreenShareStanza(callId, callCreator, state, version);
    }

    /**
     * Returns whether {@code obj} is a {@link ScreenShareStanza} with the same call header, state, and
     * version.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal screen sharing action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ScreenShareStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && state == that.state
                && version == that.version);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this screen sharing action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, state, version);
    }

    /**
     * Returns a string for debugging this screen sharing action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "ScreenShareStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", state=" + state + ", version=" + version + ']';
    }
}
