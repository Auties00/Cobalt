package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Enumerates the integer codes carried on the {@code error} attribute of an {@code <ack>} stanza,
 * the WhatsApp wire-protocol encoding of both client-emitted NACKs and server-side send rejections.
 *
 * <p>The fifteen values mirror WA Web's nack-reason set one-for-one. The wire protocol never carries
 * a literal {@code <nack>} element; failure is always an {@code <ack error="N">}, optionally
 * augmented with a {@code <meta failure_reason="..."/>} child for {@link #INVALID_PROTOBUF}.
 *
 * <p>This type is used on both sides of the ack flow. On the outbound side, a constant is passed to
 * {@link AckSender#sendNack(AckClass, Stanza, NackReason)} to ship the NACK. On the inbound-response
 * side, callers compare {@link AckResult#error()} against {@link #code()} or route through
 * {@link #fromCode(int)} for a typed switch; this is the branch the send pipeline uses to drive
 * recovery, for example {@link #STALE_GROUP_ADDRESSING_MODE} triggering the addressing-mode
 * migration.
 *
 * @see AckResult#error()
 */
@WhatsAppWebModule(moduleName = "WAWebCreateNackFromStanza")
public enum NackReason {
    /**
     * Marks that the group addressing mode (LID vs PN) drifted from the client's last-known view.
     *
     * <p>On the inbound-response side the {@code GroupMessageSender} send path recovers by refreshing
     * participant metadata and rewriting every participant JID into the server-reported mode.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    STALE_GROUP_ADDRESSING_MODE(421),

    /**
     * Marks that the recipient chat hit the monthly cap on never-messaged-before contacts.
     *
     * <p>Non-retriable. The send pipeline records the cap state on the chat record so the UI can
     * surface the monthly-contact-cap-reached affordance.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    NEW_CHAT_MESSAGES_CAPPED(475),

    /**
     * Marks that the stanza did not parse.
     *
     * <p>On the outbound side, emitted by
     * {@link com.github.auties00.cobalt.stream.message.MessageStreamHandler} when a malformed inbound
     * stanza cannot be fed to the receive-stanza parser.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    PARSING_ERROR(487),

    /**
     * Marks that the stanza tag is not one the client knows how to dispatch.
     *
     * <p>Mirrors WA Web's default branch: the client emits this code when an inbound stanza tag is
     * outside the logged-in stanza switch.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNRECOGNIZED_STANZA(488),

    /**
     * Marks that the stanza's {@code class} attribute is not one the client knows how to dispatch.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNRECOGNIZED_STANZA_CLASS(489),

    /**
     * Marks that the stanza's {@code type} attribute is not one the client knows how to dispatch.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNRECOGNIZED_STANZA_TYPE(490),

    /**
     * Marks that the stanza decoded but the embedded protobuf failed to parse.
     *
     * <p>Outbound acks carrying this reason must also include a {@code <meta failure_reason="..."/>}
     * child, populated via {@link AckBuilder#failureReason(String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    INVALID_PROTOBUF(491),

    /**
     * Marks that the stanza targets a hosted-companion route the client cannot service.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    INVALID_HOSTED_COMPANION_STANZA(493),

    /**
     * Marks that the stanza references a message whose {@code messageSecret} the client does not have
     * locally.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    MISSING_MESSAGE_SECRET(495),

    /**
     * Marks a Signal-protocol counter older than the server's last-observed value for the same
     * session.
     *
     * <p>Emitted by the offline-delivery receipt path when a Signal decryption fails terminally.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    SIGNAL_ERROR_OLD_COUNTER(496),

    /**
     * Marks that the peer has already deleted the targeted message on their device.
     *
     * <p>Returned for revokes whose target is already gone on the recipient side.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    MESSAGE_DELETED_ON_PEER(499),

    /**
     * Marks an unclassified runtime failure inside stanza handling.
     *
     * <p>The catch-all reason; used only when no more specific reason applies. WA Web emits this code
     * for unexpected exceptions inside its per-stanza dispatch.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNHANDLED_ERROR(500),

    /**
     * Marks that the stanza requests an admin-revoke action the client does not support.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNSUPPORTED_ADMIN_REVOKE(550),

    /**
     * Marks that the stanza targets a LID-addressed group the client cannot service in its current
     * configuration.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNSUPPORTED_LID_GROUP(551),

    /**
     * Marks that a local persistence operation failed while applying the stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza", exports = "NackReason",
            adaptation = WhatsAppAdaptation.DIRECT)
    DB_OPERATION_FAILED(552);

    /**
     * The on-wire integer error code carried on the {@code error} attribute.
     */
    private final int code;

    /**
     * Constructs a {@code NackReason} bound to its on-wire integer code.
     *
     * @param code the value written to the {@code error} attribute of the outbound {@code <ack>}
     */
    NackReason(int code) {
        this.code = code;
    }

    /**
     * Returns the on-wire integer code carried on the {@code error} attribute of an {@code <ack>}
     * stanza.
     *
     * <p>On the outbound side {@link AckBuilder#send()} writes this value into the {@code error}
     * attribute; on the inbound-response side callers compare it against {@link AckResult#error()} to
     * classify a server rejection.
     *
     * @return the integer code for this constant
     */
    public int code() {
        return code;
    }

    /**
     * Maps an integer error code parsed off an inbound {@code <ack>} back to its enum constant.
     *
     * <p>Returns {@code null} when {@code code} is outside the fifteen documented reasons; the server
     * is not required to limit itself to this set, so callers should keep the integer alongside the
     * typed lookup.
     *
     * @param code the integer parsed off the inbound {@code error} attribute
     * @return the matching {@code NackReason}, or {@code null} when the code is not one of the
     *         fifteen documented reasons
     */
    public static NackReason fromCode(int code) {
        for (var value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
