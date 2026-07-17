package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.message.receipt.MessageReceiptHandler;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Enumerates the aggregated outcomes of the end-to-end decryption pipeline for an
 * incoming message stanza.
 *
 * <p>The value is returned by {@link MessageDecryptionHandler#getResult()} after
 * iteration over every {@code <enc>} child finishes; it drives the follow-up branching
 * in {@link MessageReceiptHandler} (delivery receipt, retry, NACK, plain ack) and
 * decides whether the decoded {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo}
 * is persisted.
 *
 * @implNote
 * This implementation mirrors WhatsApp Web's {@code E2EProcessResult} enum from
 * {@code WAWebHandleMsgTypes.flow}; the WhatsApp Web enum also defines a
 * {@code DEFERRED} variant used for orphan bot messages, which Cobalt does not yet
 * implement.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgTypes.flow")
public enum MessageDecryptionResult {
    /**
     * Indicates at least one encrypted payload was decrypted and its protobuf content
     * passed post-decryption validation.
     *
     * <p>Triggers a delivery receipt and persists the decoded
     * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo}.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    SUCCESS,

    /**
     * Indicates decryption failed with a retryable Signal-protocol error and the sender
     * must re-encrypt the payload.
     *
     * <p>Triggers a retry receipt; WhatsApp Web attaches a fresh prekey bundle from the
     * second attempt onward so the sender can re-establish the Signal session.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    RETRY,

    /**
     * Indicates the stanza marked the message as highly-structured but the decoded
     * protobuf did not carry an
     * {@link com.github.auties00.cobalt.wire.linked.message.text.HighlyStructuredMessage}, or
     * vice versa.
     *
     * <p>Triggers a silent drop: no receipt is sent because the mismatch is a
     * protocol-level inconsistency that retrying cannot resolve.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    HSM_MISMATCH,

    /**
     * Indicates the stanza is an unavailable fanout placeholder produced when a
     * companion device needs history backfill but has not yet received the content.
     *
     * <p>Triggers a plain {@code <ack>} and the placeholder is recorded so the on-demand
     * history sync can replay the original message later.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "PlaceholderType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    BACKFILL,

    /**
     * Indicates the decrypted bytes could not be parsed as a
     * {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer} protobuf or
     * carried an unrecognised content tag.
     *
     * <p>Triggers a NACK with the WhatsApp Web {@code NackReason.ParsingError} code.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    PARSE_ERROR,

    /**
     * Indicates the protobuf parsed but failed post-decode structural validation, for
     * example an invalid {@code DeviceSentMessage} envelope or a stanza/protobuf type
     * mismatch.
     *
     * <p>Triggers a NACK with the {@code NackReason.InvalidProtobuf} code (491); the
     * NACK may additionally carry a {@code <meta failure_reason=...>} child surfaced
     * from the underlying validation error for server-side telemetry.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    PARSE_VALIDATION_ERROR,

    /**
     * Indicates the Signal protocol reported a duplicate or already-seen message counter
     * for one of the encrypted payloads.
     *
     * <p>If the message qualifies for dedup the cached delivery outcome is reused via the
     * duplicate-message receipt path; otherwise the message proceeds as a normal
     * delivery.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "E2EProcessResult",
            adaptation = WhatsAppAdaptation.DIRECT)
    SIGNAL_OLD_COUNTER_ERROR
}
