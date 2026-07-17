package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * The payment metadata extracted from the {@code <pay>} and
 * {@code <transaction>} children of an incoming {@code <message>} stanza.
 *
 * <p>The two children sit as direct siblings of the {@code <message>} stanza, not
 * nested; this record flattens both into one shape so the UI does not need to
 * know which stanza carried which attribute. When both are present the
 * {@code <transaction>} fields win, because {@code <transaction>} is the newer
 * Novi/WhatsApp Pay envelope. The {@link #futureproofed()} flag is set when the
 * {@code <pay>} or {@code <transaction>} stanza is a Novi-style envelope that this
 * client does not yet understand; downstream code skips rendering payment
 * details in that case. Instances are produced by
 * {@link MessageReceiveStanzaParser} and consumed via
 * {@link MessageReceiveStanza#paymentInfo()}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceivePaymentInfo {
    /**
     * {@code true} when the payment stanza is tagged as a future Novi envelope.
     */
    private final boolean futureproofed;

    /**
     * The string form of the receiver's JID, taken from the {@code receiver}
     * attribute or, for legacy {@code pay} {@code type="send"} stanzas, the
     * outer {@code recipient} attribute.
     */
    private final String receiverJid;

    /**
     * The ISO currency code of the payment.
     */
    private final String currency;

    /**
     * The payment amount in 1/1000 units of the smallest currency unit
     * (millicents for USD-style currencies).
     *
     * @implNote
     * This implementation keeps the wire-level scaling factor of 1000 instead
     * of converting to a decimal type so the value round-trips losslessly
     * through any downstream WhatsApp Pay surfaces.
     */
    private final Long amount1000;

    /**
     * The Unix-second transaction timestamp.
     */
    private final Long transactionTimestamp;

    /**
     * The WhatsApp Pay transaction status (for example {@code INIT},
     * {@code PENDING}, {@code COMPLETED}), set only when the local user is a
     * party to the transaction.
     */
    private final String txnStatus;

    /**
     * Constructs a populated record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * @param futureproofed        whether this is a Novi-style futureproofed envelope
     * @param receiverJid          the receiver JID string, or {@code null}
     * @param currency             the ISO currency code, or {@code null}
     * @param amount1000           the amount in 1/1000 units, or {@code null}
     * @param txnStatus            the transaction status, or {@code null}
     * @param transactionTimestamp the Unix-second timestamp, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceivePaymentInfo(
            boolean futureproofed,
            String receiverJid,
            String currency,
            Long amount1000,
            String txnStatus,
            Long transactionTimestamp
    ) {
        this.futureproofed = futureproofed;
        this.receiverJid = receiverJid;
        this.currency = currency;
        this.amount1000 = amount1000;
        this.txnStatus = txnStatus;
        this.transactionTimestamp = transactionTimestamp;
    }

    /**
     * Returns whether this payment stanza was tagged as a Novi-style
     * futureproofed envelope.
     *
     * <p>When {@code true} every other field on this record is empty; the
     * receiver should fall back to a generic "unsupported payment" placeholder
     * rather than attempting to render the transaction.
     *
     * @return {@code true} if futureproofed
     */
    public boolean futureproofed() {
        return futureproofed;
    }

    /**
     * Returns the string form of the receiver's JID, when present.
     *
     * <p>Identifies which side of the transaction is the payee; compared
     * against the local user JID to decide whether to display a status badge.
     *
     * @return an {@link Optional} wrapping the receiver JID string
     */
    public Optional<String> receiverJid() {
        return Optional.ofNullable(receiverJid);
    }

    /**
     * Returns the ISO currency code, when present.
     *
     * <p>Combined with {@link #amount1000()} to format the displayed monetary
     * value.
     *
     * @return an {@link Optional} wrapping the currency code
     */
    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    /**
     * Returns the payment amount in 1/1000 units of the smallest currency unit,
     * when present.
     *
     * <p>Divide by 1000 to obtain the value in the smallest currency unit and
     * by 100000 to obtain the value in the major unit (for USD-style
     * currencies).
     *
     * @return an {@link Optional} wrapping the amount
     */
    public Optional<Long> amount1000() {
        return Optional.ofNullable(amount1000);
    }

    /**
     * Returns the WhatsApp Pay transaction status, when present.
     *
     * <p>Populated only when the receiver is a party to the transaction;
     * absence here typically means the message is a third-party transaction
     * visible inside a group chat.
     *
     * @return an {@link Optional} wrapping the status
     */
    public Optional<String> txnStatus() {
        return Optional.ofNullable(txnStatus);
    }

    /**
     * Returns the Unix-second transaction timestamp, when present.
     *
     * <p>Defaults to the message's own {@code t} attribute for legacy
     * {@code <pay type="send">} stanzas that have no transaction-level
     * timestamp.
     *
     * @return an {@link Optional} wrapping the timestamp
     */
    public Optional<Long> transactionTimestamp() {
        return Optional.ofNullable(transactionTimestamp);
    }
}
