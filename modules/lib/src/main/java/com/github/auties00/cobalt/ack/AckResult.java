package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Typed view of a server {@code <ack>} stanza.
 *
 * <p>WhatsApp stamps every {@code <ack>} with a {@code class} attribute that names the inbound
 * stanza class being acknowledged: {@code message}, {@code receipt}, {@code notification}, or
 * {@code call}. Each class shares a common set of envelope attributes ({@code id},
 * {@code t}, {@code type}, {@code from}, {@code participant}, {@code recipient}, {@code error})
 * and adds its own class-specific payload; the subtypes of this interface project both layers.
 *
 * <p>{@link AckParser#parse(Stanza)} dispatches on the {@code class}
 * attribute and returns the matching subtype:
 * <ul>
 *   <li>{@code message} → {@link MessageAck} (carries the message-fanout slots: {@code sync},
 *       {@code phash}, {@code refresh_lid}, {@code addressing_mode}, {@code count})</li>
 *   <li>{@code receipt} → {@link ReceiptAck}</li>
 *   <li>{@code notification} → {@link NotificationAck}</li>
 *   <li>{@code call} → {@link CallAck} (carries the {@code <relay>} block on offer ACKs)</li>
 * </ul>
 *
 * <p>Common consumers branch on {@link #isSuccess()} and {@link #error()}; class-specific
 * consumers pattern-match on the subtype.
 *
 * @see AckParser
 * @see NackReason
 */
@WhatsAppWebModule(moduleName = "WAWebSendMsgCommonApi")
@WhatsAppWebModule(moduleName = "WAAckParser")
public sealed interface AckResult permits MessageAck, ReceiptAck, NotificationAck, CallAck {
    /**
     * Returns the {@code id} attribute of the {@code <ack>} stanza.
     *
     * <p>Always populated on a real server ack and matched against the outbound stanza's id by
     * the send pipeline to correlate the response.
     *
     * @return the {@code id} value; never {@code null} on a real server ack
     */
    String id();

    /**
     * Returns the {@code class} attribute of the {@code <ack>} stanza.
     *
     * @return the ack class; never {@code null}
     */
    AckClass ackClass();

    /**
     * Returns the server timestamp carried on the ack.
     *
     * <p>Always populated on a real server ack; an empty result surfaces only when a synthetic
     * stanza was fed through {@link AckParser}.
     *
     * @return the parsed {@link Instant}, or {@link Optional#empty()} when the {@code t}
     *         attribute was absent
     */
    @WhatsAppWebExport(moduleName = "WAAckParser", exports = "AckParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<Instant> timestamp();

    /**
     * Returns the {@code type} attribute, the sub-classification of the stanza being acked.
     *
     * @return the {@code type} value, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAAckParser", exports = "AckParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<String> type();

    /**
     * Returns the {@code from} attribute, the originator of the acked stanza.
     *
     * @return the originator JID, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAAckParser", exports = "AckParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<Jid> from();

    /**
     * Returns the {@code participant} attribute, set on group fanout acks to identify the
     * specific device the ack refers to.
     *
     * @return the participant device JID, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAAckParser", exports = "AckParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<Jid> participant();

    /**
     * Returns the {@code recipient} attribute, set on acks to user-targeted stanzas.
     *
     * @return the recipient user JID, or {@link Optional#empty()} when absent
     */
    @WhatsAppWebExport(moduleName = "WAAckParser", exports = "AckParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    Optional<Jid> recipient();

    /**
     * Returns the server error code carried on a NACK.
     *
     * <p>The presence of a value is the canonical accept/reject signal; compare the integer
     * against {@link NackReason#code()} or fold through {@link NackReason#fromCode(int)} to
     * classify the rejection. {@link #isSuccess()} is the boolean shortcut for callers that only
     * need the accept/reject test.
     *
     * @return the {@code error} code, or {@link OptionalInt#empty()} when the send succeeded
     */
    OptionalInt error();

    /**
     * Returns whether the server accepted the outgoing stanza.
     *
     * <p>Equivalent to {@code error().isEmpty()}.
     *
     * @return {@code true} when no {@code error} attribute was present
     */
    default boolean isSuccess() {
        return error().isEmpty();
    }
}
