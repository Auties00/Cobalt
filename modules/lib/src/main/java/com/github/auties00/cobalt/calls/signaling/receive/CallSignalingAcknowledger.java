package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Set;

/**
 * Emits the wire acknowledgement an inbound call signaling stanza requires, picking between the two
 * distinct mechanisms the server expects.
 *
 * <p>An inbound {@code <call>} signal is acknowledged in one of two ways, and the choice is keyed on
 * the payload tag, not the stanza class:
 * <ul>
 *   <li>The offer, accept, reject, and rekey legs are acknowledged with a {@code <receipt>} whose
 *       single child mirrors the signaling tag and carries the {@code call-id} and
 *       {@code call-creator}, with the {@code from} attribute set to the local identity that matches
 *       the peer's addressing (the local LID user for a LID peer, the local phone number user
 *       otherwise).</li>
 *   <li>Every other signal is acknowledged with an {@code <ack class="call" type="...">} whose
 *       {@code type} echoes the inbound payload tag rather than the {@code <call>} element's own
 *       {@code type}.</li>
 * </ul>
 *
 * <p>The acknowledgement is sent regardless of whether the local engine can act on the message, so the
 * server stops retransmitting even for a payload the engine drops or buffers. The {@code <ack>} path
 * reuses the shared {@link AckSender}; the {@code <receipt>} path builds and ships its stanza directly
 * through the client. Both paths do nothing when the inbound stanza lacks the attributes the
 * acknowledgement needs: an {@code id} and a sender for the ack, and both of those plus a
 * {@code call-id} and {@code call-creator} for the receipt.
 */
public final class CallSignalingAcknowledger {
    /**
     * The logger for {@link CallSignalingAcknowledger}.
     */
    private static final System.Logger LOGGER = Log.get(CallSignalingAcknowledger.class);

    /**
     * The wire attribute naming the call identifier on a {@code <call>} child element and on a receipt
     * child.
     */
    private static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the call creator's device JID on a {@code <call>} child element and on
     * a receipt child.
     */
    private static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * The wire element tag for a receipt acknowledging a call signal.
     */
    private static final String RECEIPT_ELEMENT = "receipt";

    /**
     * The wire attribute naming the sender on the inbound {@code <call>} envelope and the recipient on
     * the outbound {@code <receipt>}.
     */
    private static final String FROM_ATTRIBUTE = "from";

    /**
     * The wire attribute naming the recipient on the outbound {@code <receipt>}.
     */
    private static final String TO_ATTRIBUTE = "to";

    /**
     * The wire attribute naming the inbound stanza identifier echoed onto the outbound
     * {@code <receipt>}.
     */
    private static final String ID_ATTRIBUTE = "id";

    /**
     * The set of payload tags acknowledged with a {@code <receipt>} rather than an
     * {@code <ack class="call">}.
     *
     * <p>The offer, accept, reject, and rekey legs receive a {@code <receipt>} whose child mirrors the
     * payload tag; every other signal receives an {@code <ack class="call" type="...">}.
     */
    private static final Set<String> RECEIPT_TAGS = Set.of("offer", "accept", "reject", "enc_rekey");

    /**
     * The client used to ship receipts without awaiting a response and to read the local account
     * identity for the receipt {@code from} attribute.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * The shared sender used to emit the {@code <ack class="call">} for non receipt signals.
     */
    private final AckSender ackSender;

    /**
     * Constructs an acknowledger bound to its client and acknowledgement sender.
     *
     * @param whatsapp  the client used to send receipts and read the local account identity
     * @param ackSender the {@link AckSender} used to emit the {@code <ack class="call">} for
     *                  non receipt signals
     * @throws NullPointerException if any argument is {@code null}
     */
    public CallSignalingAcknowledger(LinkedWhatsAppClient whatsapp, AckSender ackSender) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.ackSender = Objects.requireNonNull(ackSender, "ackSender cannot be null");
    }

    /**
     * Reports whether a payload tag is acknowledged with a {@code <receipt>} rather than an
     * {@code <ack class="call">}.
     *
     * <p>True for the offer, accept, reject, and rekey legs; false for every other signaling tag.
     *
     * @param payloadTag the signaling tag of the inbound {@code <call>} child element
     * @return {@code true} when the tag is acknowledged with a {@code <receipt>}
     */
    public static boolean usesReceipt(String payloadTag) {
        return RECEIPT_TAGS.contains(payloadTag);
    }

    /**
     * Emits the wire acknowledgement an inbound payload requires, choosing the receipt or ack
     * mechanism by the payload tag.
     *
     * <p>The offer, accept, reject, and rekey legs are acknowledged with a {@code <receipt>} when both
     * the {@code call-id} and the {@code call-creator} are present, since a receipt child requires
     * both; every other signal, and a receipt tag payload that failed header validation, is
     * acknowledged with an {@code <ack class="call" type="...">} echoing the payload tag. The
     * {@code call-id}, the envelope {@code from}, and the {@code call-creator} are supplied by the
     * caller rather than read again from the stanza, so a caller that has already parsed them (for
     * example through a classification step) passes the resolved values instead of forcing a second
     * parse.
     *
     * @param envelope    the inbound {@code <call>} stanza, used for the inbound identifier and ack
     *                    correlation
     * @param payload     the {@code <call>} child element being acknowledged
     * @param callId      the validated call identifier, or {@code null} when the payload carried none
     * @param from        the envelope {@code from} sender, or {@code null} when absent
     * @param callCreator the {@code call-creator} device JID from the payload, or {@code null} when
     *                    absent
     * @return {@code true} when an acknowledgement was dispatched, {@code false} when it was dropped
     *         for a missing required attribute
     * @throws NullPointerException if {@code envelope} or {@code payload} is {@code null}
     */
    public boolean acknowledge(Stanza envelope, Stanza payload, String callId, Jid from, Jid callCreator) {
        Objects.requireNonNull(envelope, "envelope cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        var tag = payload.description();
        if (usesReceipt(tag) && callId != null && callCreator != null && from != null) {
            return sendReceipt(envelope, from, callId, callCreator, tag);
        }
        var sent = ackSender.ack(AckClass.CALL, envelope)
                .type(tag)
                .send();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent call ack type={0} result={1}", tag, sent);
        return sent;
    }

    /**
     * Sends a {@code <receipt>} acknowledging an offer, accept, reject, or rekey signal.
     *
     * <p>The receipt wraps a child whose tag mirrors the signaling type and carries the
     * {@code call-id} and {@code call-creator}; its {@code from} attribute is the local identity that
     * matches the peer's addressing. The receipt is dropped when the local identity or the inbound
     * stanza identifier cannot be resolved.
     *
     * @param envelope    the inbound {@code <call>} stanza
     * @param to          the receipt recipient, the sender of the inbound stanza
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param childTag    the receipt child tag mirroring the signaling type
     * @return {@code true} when the receipt was dispatched, {@code false} when it was dropped for a
     *         missing local identity or inbound identifier
     */
    public boolean sendReceipt(Stanza envelope, Jid to, String callId, Jid callCreator, String childTag) {
        var self = resolveReceiptFrom(to);
        var stanzaId = envelope.getAttributeAsString(ID_ATTRIBUTE, null);
        if (self == null || stanzaId == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dropping call receipt {0} for call {1}: missing local identity or stanza id",
                        childTag, callId);
            }
            return false;
        }
        var child = new StanzaBuilder()
                .description(childTag)
                .attribute(CALL_ID_ATTRIBUTE, callId)
                .attribute(CALL_CREATOR_ATTRIBUTE, callCreator)
                .build();
        var receipt = new StanzaBuilder()
                .description(RECEIPT_ELEMENT)
                .attribute(TO_ATTRIBUTE, to)
                .attribute(FROM_ATTRIBUTE, self)
                .attribute(ID_ATTRIBUTE, stanzaId)
                .content(child)
                .build();
        whatsapp.sendNodeWithNoResponse(receipt);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent call receipt {0} for call {1}", childTag, callId);
        return true;
    }

    /**
     * Sends an {@code <ack class="call" type="...">} echoing the inbound payload tag.
     *
     * <p>The {@code type} attribute carries the inbound payload tag rather than the {@code <call>}
     * element's own {@code type}, and the shared {@link AckSender} resolves the {@code id} and
     * {@code to} from the inbound envelope. The ack is dropped when the inbound stanza lacks an
     * {@code id} or a {@code from}, matching the shared sender's precondition.
     *
     * @param envelope   the inbound {@code <call>} stanza being acknowledged
     * @param payloadTag the signaling tag echoed into the {@code type} attribute
     * @return {@code true} when the ack was dispatched, {@code false} when it was dropped for a missing
     *         {@code id} or {@code from}
     * @throws NullPointerException if {@code envelope} is {@code null}
     */
    public boolean sendAck(Stanza envelope, String payloadTag) {
        Objects.requireNonNull(envelope, "envelope cannot be null");
        var sent = ackSender.ack(AckClass.CALL, envelope)
                .type(payloadTag)
                .send();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent call ack type={0} result={1}", payloadTag, sent);
        return sent;
    }

    /**
     * Resolves the local identity to stamp on an outgoing receipt's {@code from} attribute.
     *
     * <p>Returns the local LID user when the peer uses the LID server, and the local phone number user
     * otherwise; returns {@code null} when the local identity is unavailable, which occurs before the
     * connection is ready.
     *
     * @param remote the peer the receipt is addressed to
     * @return the local {@link Jid} for the {@code from} attribute, or {@code null} when unavailable
     */
    private Jid resolveReceiptFrom(Jid remote) {
        var self = whatsapp.store().accountStore().jid().orElse(null);
        if (self == null) {
            return null;
        }
        if (remote.hasLidServer()) {
            return whatsapp.store().accountStore().lid().orElse(self.toUserJid());
        }
        return self.toUserJid();
    }
}
