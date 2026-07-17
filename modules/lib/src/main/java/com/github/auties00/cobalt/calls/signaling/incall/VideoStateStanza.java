package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.calls.engine.participant.VideoStreamState;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <video>} in call action broadcasting a change in the sender's video stream state.
 *
 * <p>The action reports the sender's current {@link VideoStreamState}: the plain camera on and off
 * lifecycle (enabled, disabled, paused, stopped) together with every step of the video upgrade
 * negotiation (request, accept, reject, cancel, and their timeout variants). It carries the common call
 * header ({@code call-id}, {@code call-creator}) and a numeric {@code state} attribute equal to the
 * {@linkplain VideoStreamState#wireOrdinal() wire ordinal} of the broadcast state.
 *
 * <p>On the wire the element takes the shape {@snippet lang="xml" :
 * <video call-id="..." call-creator="..." state="N" device_orientation="0" dec="H264"/>
 * }
 * where {@code device_orientation} is always present and {@code dec="H264"} is added only while the
 * camera is on. Although the type is named {@code video_state} internally, the serialized {@code <call>}
 * child element is {@code <video>}.
 *
 * @see SignalingType#VIDEO_STATE
 * @see VideoStreamState
 */
public final class VideoStateStanza implements InCallActionStanza {
    /**
     * The wire element tag for a video state action; the type's internal name is {@code video_state} but
     * the serialized {@code <call>} child is {@code <video>}.
     */
    public static final String ELEMENT = "video";

    /**
     * The wire attribute naming the video stream state ordinal.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The wire attribute naming the camera orientation.
     */
    private static final String DEVICE_ORIENTATION_ATTRIBUTE = "device_orientation";

    /**
     * The wire attribute naming the active video decode codec, written only while the camera is on.
     */
    private static final String DEC_ATTRIBUTE = "dec";

    /**
     * The camera orientation advertised in every announcement; a Web client has no orientation sensor, so
     * the value is always {@code 0}.
     */
    private static final int DEFAULT_DEVICE_ORIENTATION = 0;

    /**
     * The decode codec token advertised while the camera is on, matching the offered video format's
     * {@code dec} token.
     */
    private static final String CAMERA_ON_DECODE_CODEC = "H264";

    /**
     * The call identifier carried by this action's {@code call-id} header.
     */
    private final String callId;

    /**
     * The call creator device JID carried by this action's {@code call-creator} header.
     */
    private final Jid callCreator;

    /**
     * The broadcast video stream state.
     */
    private final VideoStreamState state;

    /**
     * Constructs a video state action.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param state       the broadcast video stream state; never {@code null}
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code state} is
     *                              {@code null}
     */
    public VideoStateStanza(String callId, Jid callCreator, VideoStreamState state) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.state = state;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a video state action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a video state action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the broadcast video stream state.
     *
     * @return the video stream state; never {@code null}
     */
    public VideoStreamState state() {
        return state;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#VIDEO_STATE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.VIDEO_STATE;
    }

    /**
     * Builds the {@code <video call-id call-creator state device_orientation [dec]/>} action stanza.
     *
     * <p>The {@code state} attribute is the {@linkplain VideoStreamState#wireOrdinal() wire ordinal} of
     * {@link #state()}, not its Java {@link Enum#ordinal()}. The {@code device_orientation} attribute is
     * always written; the {@code dec} attribute carries the active decode codec and is written only while
     * the camera is on (the {@link VideoStreamState#ENABLED} state).
     *
     * @return the video state action stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(STATE_ATTRIBUTE, state.wireOrdinal())
                .attribute(DEVICE_ORIENTATION_ATTRIBUTE, DEFAULT_DEVICE_ORIENTATION);
        if (state == VideoStreamState.ENABLED) {
            builder.attribute(DEC_ATTRIBUTE, CAMERA_ON_DECODE_CODEC);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <video>} action stanza into a {@link VideoStateStanza}.
     *
     * <p>The {@code state} attribute is resolved through {@link VideoStreamState#ofWireOrdinal(int)};
     * an absent attribute decodes to ordinal {@code 0} ({@link VideoStreamState#DISABLED}).
     *
     * @param stanza the {@code <video>} stanza
     * @return the decoded video state action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static VideoStateStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var state = VideoStreamState.ofWireOrdinal(stanza.getAttributeAsInt(STATE_ATTRIBUTE, 0));
        return new VideoStateStanza(callId, callCreator, state);
    }

    /**
     * Returns whether {@code obj} is a {@link VideoStateStanza} with the same call header and video state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal video state action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof VideoStateStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && state.equals(that.state));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this video state action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, state);
    }

    /**
     * Returns a debug oriented string for this video state action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "VideoStateStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", state=" + state + ']';
    }
}
