package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <notify>} in call action, an out of band status notification carrying the
 * sender's battery state.
 *
 * <p>A notify action reports a low priority status update to the other participants. It carries the
 * universal call header ({@code call-id} and {@code call-creator}) and a numeric {@code batterystate}
 * code describing the sender's battery condition. The {@code notify} element is also the vehicle
 * through which several containerless actions (such as raise hand and mute) travel; this class models
 * the battery state use of the element itself.
 *
 * <p>On the wire the element is:
 * {@snippet lang="xml" :
 * <notify call-id="..." call-creator="..." batterystate="N"/>
 * }
 *
 * <p>The action projects to {@link SignalingType#NOTIFY}. Decoding requires the {@code batterystate}
 * attribute in addition to the common {@code call-id} and {@code call-creator} header attributes.
 *
 * @see SignalingType#NOTIFY
 */
public final class NotifyStanza implements InCallActionStanza {
    /**
     * The wire element tag for a notify action.
     */
    public static final String ELEMENT = "notify";

    /**
     * The wire attribute naming the sender's battery state code.
     */
    private static final String BATTERY_STATE_ATTRIBUTE = "batterystate";

    /**
     * The call identifier this notify's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this notify's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * The numeric battery state code reported by the sender.
     */
    private final int batteryState;

    /**
     * Constructs a notify action.
     *
     * @param callId       the call identifier, never {@code null}
     * @param callCreator  the call creator's device JID, never {@code null}
     * @param batteryState the numeric battery state code reported by the sender
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public NotifyStanza(String callId, Jid callCreator, int batteryState) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.batteryState = batteryState;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a notify action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a notify action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the numeric battery state code reported by the sender.
     *
     * @return the battery state code
     */
    public int batteryState() {
        return batteryState;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#NOTIFY}
     */
    @Override
    public SignalingType type() {
        return SignalingType.NOTIFY;
    }

    /**
     * Builds the {@code <notify call-id call-creator batterystate/>} action stanza.
     *
     * @return the notify action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(BATTERY_STATE_ATTRIBUTE, batteryState)
                .build();
    }

    /**
     * Decodes a {@code <notify>} action stanza into a {@link NotifyStanza}.
     *
     * @param stanza the {@code <notify>} stanza
     * @return the decoded notify action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id}, {@code call-creator}, or
     *                                {@code batterystate} attribute is absent
     */
    public static NotifyStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var batteryState = stanza.getAttributeAsInt(BATTERY_STATE_ATTRIBUTE)
                .orElseThrow(() -> new NoSuchElementException("notify requires a batterystate attribute"));
        return new NotifyStanza(callId, callCreator, batteryState);
    }

    /**
     * Returns whether {@code obj} is a {@link NotifyStanza} with the same call header and battery state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal notify action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof NotifyStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && batteryState == that.batteryState);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this notify action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, batteryState);
    }

    /**
     * Returns a debug oriented string for this notify action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "NotifyStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", batteryState=" + batteryState + ']';
    }
}
