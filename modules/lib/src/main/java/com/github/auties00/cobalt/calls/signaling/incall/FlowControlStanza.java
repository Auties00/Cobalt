package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <flowcontrol>} in call action asking a peer to cap its outbound video encoding.
 *
 * <p>A flow control action carries the video rate control targets the receiver wants the sender to
 * honour. It carries the common call header, a numeric {@code transaction-id} attribute correlating
 * the request, and up to three child elements holding integer text content: {@code <bitrate>} (the
 * target encoding bitrate), {@code <width>} (the target frame width), and {@code <fps>} (the target
 * frame rate). Each child is optional; an absent target is omitted from the stanza entirely, and a
 * flow control stanza with no targets carries only the common header.
 *
 * <p>On the wire the element is
 * {@snippet lang="xml" :
 * <flowcontrol call-id="..." call-creator="..." transaction-id="N">
 *   <bitrate>N</bitrate>
 *   <width>N</width>
 *   <fps>N</fps>
 * </flowcontrol>
 * }
 *
 * @see SignalingType#FLOW_CONTROL
 */
public final class FlowControlStanza implements InCallActionStanza {
    /**
     * The wire element tag for a flow control action.
     */
    public static final String ELEMENT = "flowcontrol";

    /**
     * The wire attribute naming the correlation transaction id.
     */
    private static final String TRANSACTION_ID_ATTRIBUTE = "transaction-id";

    /**
     * The wire child element naming the target encoding bitrate.
     */
    private static final String BITRATE_ELEMENT = "bitrate";

    /**
     * The wire child element naming the target frame width.
     */
    private static final String WIDTH_ELEMENT = "width";

    /**
     * The wire child element naming the target frame rate.
     */
    private static final String FPS_ELEMENT = "fps";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The correlation transaction id, or {@code -1} when absent.
     */
    private final int transactionId;

    /**
     * The target encoding bitrate, or {@code -1} when absent.
     */
    private final int bitrate;

    /**
     * The target frame width, or {@code -1} when absent.
     */
    private final int width;

    /**
     * The target frame rate, or {@code -1} when absent.
     */
    private final int fps;

    /**
     * Constructs a flow control action, validating its components.
     *
     * @param callId        the call identifier; never {@code null}
     * @param callCreator   the call creator's device JID; never {@code null}
     * @param transactionId the correlation transaction id, or {@code -1} when absent
     * @param bitrate       the target encoding bitrate, or {@code -1} when absent
     * @param width         the target frame width, or {@code -1} when absent
     * @param fps           the target frame rate, or {@code -1} when absent
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public FlowControlStanza(String callId, Jid callCreator, int transactionId, int bitrate, int width, int fps) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.transactionId = transactionId;
        this.bitrate = bitrate;
        this.width = width;
        this.fps = fps;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a flow control action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a flow control action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the correlation transaction id, or {@code -1} when absent.
     *
     * @return the raw transaction id
     */
    public int transactionId() {
        return transactionId;
    }

    /**
     * Returns the target encoding bitrate, or {@code -1} when absent.
     *
     * @return the raw bitrate
     */
    public int bitrate() {
        return bitrate;
    }

    /**
     * Returns the target frame width, or {@code -1} when absent.
     *
     * @return the raw frame width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the target frame rate, or {@code -1} when absent.
     *
     * @return the raw frame rate
     */
    public int fps() {
        return fps;
    }

    /**
     * Returns the correlation transaction id, if present.
     *
     * @return an {@link OptionalInt} holding the transaction id, or empty when absent
     */
    public OptionalInt transactionIdValue() {
        return transactionId < 0 ? OptionalInt.empty() : OptionalInt.of(transactionId);
    }

    /**
     * Returns the target encoding bitrate, if present.
     *
     * @return an {@link OptionalInt} holding the bitrate, or empty when absent
     */
    public OptionalInt bitrateValue() {
        return bitrate < 0 ? OptionalInt.empty() : OptionalInt.of(bitrate);
    }

    /**
     * Returns the target frame width, if present.
     *
     * @return an {@link OptionalInt} holding the width, or empty when absent
     */
    public OptionalInt widthValue() {
        return width < 0 ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the target frame rate, if present.
     *
     * @return an {@link OptionalInt} holding the frame rate, or empty when absent
     */
    public OptionalInt fpsValue() {
        return fps < 0 ? OptionalInt.empty() : OptionalInt.of(fps);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#FLOW_CONTROL}
     */
    @Override
    public SignalingType type() {
        return SignalingType.FLOW_CONTROL;
    }

    /**
     * Builds the {@code <flowcontrol>} action stanza carrying the {@code transaction-id} attribute and
     * the {@code <bitrate>}, {@code <width>}, and {@code <fps>} child elements.
     *
     * <p>The {@code transaction-id} attribute and each of the {@code bitrate}, {@code width}, and
     * {@code fps} child elements are omitted when their value is absent; a flow control stanza with no
     * targets carries only the common header.
     *
     * @return the flow control action stanza
     */
    @Override
    public Stanza toStanza() {
        var children = new ArrayList<Stanza>(3);
        if (bitrate >= 0) {
            children.add(new StanzaBuilder().description(BITRATE_ELEMENT).content(bitrate).build());
        }
        if (width >= 0) {
            children.add(new StanzaBuilder().description(WIDTH_ELEMENT).content(width).build());
        }
        if (fps >= 0) {
            children.add(new StanzaBuilder().description(FPS_ELEMENT).content(fps).build());
        }
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(TRANSACTION_ID_ATTRIBUTE, transactionId, transactionId >= 0);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <flowcontrol>} action stanza into a {@link FlowControlStanza}.
     *
     * <p>An absent {@code transaction-id} attribute and any absent child element decode to {@code -1}.
     *
     * @param stanza the {@code <flowcontrol>} stanza
     * @return the decoded flow control action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static FlowControlStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var transactionId = stanza.getAttributeAsInt(TRANSACTION_ID_ATTRIBUTE, -1);
        var bitrate = childInt(stanza, BITRATE_ELEMENT);
        var width = childInt(stanza, WIDTH_ELEMENT);
        var fps = childInt(stanza, FPS_ELEMENT);
        return new FlowControlStanza(callId, callCreator, transactionId, bitrate, width, fps);
    }

    /**
     * Reads the integer text content of a named child element.
     *
     * @param stanza  the parent stanza
     * @param element the child element tag to read
     * @return the child's integer content, or {@code -1} when the child is absent or not numeric
     */
    private static int childInt(Stanza stanza, String element) {
        return stanza.getChild(element)
                .flatMap(Stanza::toContentInt)
                .orElse(-1);
    }

    /**
     * Returns whether {@code obj} is a {@link FlowControlStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal flow control action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof FlowControlStanza that
                && transactionId == that.transactionId
                && bitrate == that.bitrate
                && width == that.width
                && fps == that.fps
                && Objects.equals(callId, that.callId)
                && Objects.equals(callCreator, that.callCreator));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this flow control action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, transactionId, bitrate, width, fps);
    }

    /**
     * Returns a debug string for this flow control action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "FlowControlStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", transactionId=" + transactionId
                + ", bitrate=" + bitrate
                + ", width=" + width
                + ", fps=" + fps
                + ']';
    }
}
