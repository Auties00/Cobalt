package com.github.auties00.cobalt.message.receipt;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Enumerates the receipt {@code type} values serialised into the {@code <receipt>} stanza
 * sent in response to an incoming message or to broadcast an outbound state change.
 * <p>
 * Each constant carries its on-the-wire string, retrieved via {@link #protocolValue()}.
 * {@link #DELIVERY} carries a {@code null} value because a successful active delivery
 * omits the {@code type} attribute, which is how the server recognises the default
 * delivery semantics.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's {@code RECEIPT_TYPE} frozen map and the
 * literal {@code "retry"} string used by {@code WAWebSendRetryReceiptJob} into a single
 * Java enum so callers cannot pass an unknown value.
 */
@WhatsAppWebModule(moduleName = "WAWebSendReceiptJobCommon")
public enum MessageReceiptType {
    /**
     * Confirms successful decryption and storage of an incoming message.
     * <p>
     * Used when the message was decrypted, the protobuf was decoded, and there is no
     * active inactive-chat flag. The wire stanza omits the {@code type} attribute
     * entirely.
     *
     * @implNote
     * This implementation uses a {@code null} protocol value;
     * {@link StanzaBuilder} drops any attribute whose value
     * is {@code null} so the {@code type} attribute is never serialised for this constant.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    DELIVERY(null),

    /**
     * Acknowledges that one of the logged-in user's companion devices has received a
     * self-sent message.
     * <p>
     * Sent when the {@code from} or {@code participant} JID matches the local PN or LID
     * account; the server uses this to fan the delivery confirmation back to the primary
     * device.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    SENDER("sender"),

    /**
     * Acknowledges a peer-protocol message such as an app-state sync stanza echoed between
     * the user's own devices.
     * <p>
     * Selected when {@link com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza#isPeer()}
     * returns {@code true} during delivery-receipt construction.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    PEER("peer_msg"),

    /**
     * Flags the recipient chat as inactive so the server can suppress push notifications.
     * <p>
     * Selected when the decrypted message produced an inactive-message hint and the sender
     * is not the local account.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    INACTIVE("inactive"),

    /**
     * Requests the sender to re-encrypt and re-send a message which could not be decrypted.
     * <p>
     * Sent by {@link MessageReceiptHandler#sendRetryReceipt} after a Signal-protocol
     * decryption failure; from the second attempt onward the receipt also carries a fresh
     * prekey bundle so the sender can re-establish the Signal session.
     *
     * @implNote
     * This implementation owns the wire literal {@code "retry"} that WhatsApp Web hardcodes
     * inside {@code WAWebSendRetryReceiptJob.sendRetryReceipt} rather than pulling from
     * {@code WAWebSendReceiptJobCommon.RECEIPT_TYPE}, which does not expose a retry entry.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendRetryReceiptJob", exports = "sendRetryReceipt",
            adaptation = WhatsAppAdaptation.DIRECT)
    RETRY("retry"),

    /**
     * Indicates that the recipient has opened the chat and viewed the message.
     * <p>
     * Sent only when the recipient's privacy settings permit read receipts; the server
     * echoes the receipt back to the sender so the message renders with the blue
     * double-check.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    READ("read"),

    /**
     * Mirrors a read receipt across the logged-in user's companion devices so the unread
     * badge clears everywhere.
     * <p>
     * Sent in addition to a normal {@link #READ} receipt when the user has more than one
     * device; the companion devices use this to drop the chat from their unread list
     * without echoing back to the original sender.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    READ_SELF("read-self"),

    /**
     * Indicates that voice-note or view-once media has been played by the recipient.
     * <p>
     * Sent after the recipient has fully played a voice note or revealed a view-once media
     * message; mirrors the {@link #READ} receipt semantics for non-text payloads.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    PLAYED("played"),

    /**
     * Mirrors a played receipt across the logged-in user's companion devices.
     * <p>
     * Emitted alongside the main {@link #PLAYED} receipt so other devices stop highlighting
     * the voice note or view-once media.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    PLAYED_SELF("played-self"),

    /**
     * Announces that the history-sync backfill for a chat has finished processing on the
     * client.
     * <p>
     * Sent at the end of the on-demand history-sync flow so the server stops pushing
     * additional notifications for the request.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    HISTORY_SYNC_COMPLETION("hist_sync"),

    /**
     * Acknowledges a server-reported delivery error for the message.
     * <p>
     * Used to acknowledge a server-side failure path so the server stops re-attempting
     * delivery for the same id.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendReceiptJobCommon", exports = "RECEIPT_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    SERVER_ERROR("server-error");

    /**
     * Holds the protocol literal placed in the {@code type} attribute of the receipt
     * stanza, or {@code null} when the attribute must be omitted (the default delivery
     * case).
     */
    private final String protocolValue;

    /**
     * Constructs a receipt-type constant bound to its on-the-wire literal.
     *
     * @param protocolValue the literal carried by the {@code type} attribute, or
     *                      {@code null} for {@link #DELIVERY} so the attribute is dropped
     *                      from the serialised stanza
     */
    MessageReceiptType(String protocolValue) {
        this.protocolValue = protocolValue;
    }

    /**
     * Returns the protocol literal carried by the {@code type} attribute of the receipt
     * stanza, or {@code null} for the default delivery receipt.
     * <p>
     * The returned value feeds the {@code "type"} attribute slot of a
     * {@link StanzaBuilder}; a {@code null} causes the builder
     * to omit the attribute, which is the correct serialisation for {@link #DELIVERY}.
     *
     * @return the on-the-wire {@code type} literal, or {@code null} for {@link #DELIVERY}
     */
    public String protocolValue() {
        return protocolValue;
    }
}
