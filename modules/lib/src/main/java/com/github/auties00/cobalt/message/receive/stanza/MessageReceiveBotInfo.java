package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * The bot-side metadata extracted from the {@code <bot>} child of an incoming
 * {@code <message>} stanza.
 *
 * <p>Populated only for messages that originate from a Meta AI bot or a
 * WhatsApp Business 1P/3P bot. The fields drive the streaming chunk pipeline:
 * {@link #senderTimestampMs()} preserves the bot's own monotonic ordering
 * across out-of-order arrival, {@link #editTargetId()} and {@link #editType()}
 * thread each chunk back onto the same conversational turn
 * ({@code first} / {@code inner} / {@code last}), and {@link #bizBotType()}
 * disambiguates 1P from 3P business bots. Instances are produced by
 * {@link MessageReceiveStanzaParser} and consumed via
 * {@link MessageReceiveStanza#botInfo()}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceiveBotInfo {
    /**
     * The {@code sender_timestamp_ms} attribute carried on the {@code <bot>}
     * child, preserving the bot's monotonic ordering as a string.
     */
    private final String senderTimestampMs;

    /**
     * The {@code edit_target_id} attribute, which on {@code inner} and
     * {@code last} chunks references the id of the {@code first} chunk that
     * started the bot reply.
     */
    private final String editTargetId;

    /**
     * The {@code edit} attribute carried by the {@code <bot>} child;
     * {@code first} starts the stream, {@code inner} is a mid-stream token
     * update, {@code last} marks completion.
     */
    private final String editType;

    /**
     * The {@code type} attribute on the {@code <bot>} child, identifying the
     * shape of the inner bot payload.
     */
    private final String bodyType;

    /**
     * The {@code biz_bot} attribute identifying the business bot tier
     * ({@code "1"} = 1P, {@code "3"} = 3P).
     */
    private final String bizBotType;

    /**
     * Constructs a populated record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * @param senderTimestampMs the bot-side monotonic timestamp, or {@code null}
     * @param editTargetId      the id of the first chunk in the stream, or {@code null}
     * @param editType          the streaming marker, or {@code null}
     * @param bodyType          the inner payload body type, or {@code null}
     * @param bizBotType        the business bot tier, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceiveBotInfo(
            String senderTimestampMs,
            String editTargetId,
            String editType,
            String bodyType,
            String bizBotType
    ) {
        this.senderTimestampMs = senderTimestampMs;
        this.editTargetId = editTargetId;
        this.editType = editType;
        this.bodyType = bodyType;
        this.bizBotType = bizBotType;
    }

    /**
     * Returns the {@code sender_timestamp_ms} attribute, when present.
     *
     * <p>The bot's own millisecond-precision ordering id, used by the
     * streaming pipeline to reassemble chunks that may arrive out of order;
     * kept as a string because the upstream attribute is sometimes outside
     * {@code long} range.
     *
     * @return an {@link Optional} wrapping the timestamp string
     */
    public Optional<String> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Returns the {@code edit_target_id} attribute, when present.
     *
     * <p>On {@code inner} and {@code last} chunks this references the id of the
     * {@code first} chunk in the same bot reply.
     *
     * @return an {@link Optional} wrapping the target id
     */
    public Optional<String> editTargetId() {
        return Optional.ofNullable(editTargetId);
    }

    /**
     * Returns the streaming-edit marker, when present.
     *
     * <p>{@code first} starts a bot reply, {@code inner} is a mid-stream token
     * replacement, and {@code last} marks completion; the rich-response
     * stitcher uses it to know when the bot has finished.
     *
     * @return an {@link Optional} wrapping the edit type
     */
    public Optional<String> editType() {
        return Optional.ofNullable(editType);
    }

    /**
     * Returns the {@code type} attribute on the {@code <bot>} child, when
     * present.
     *
     * <p>Tells the protobuf parser which inner shape to decode (text, image,
     * native flow, and similar).
     *
     * @return an {@link Optional} wrapping the body type
     */
    public Optional<String> bodyType() {
        return Optional.ofNullable(bodyType);
    }

    /**
     * Returns the {@code biz_bot} attribute, when present.
     *
     * <p>{@code "1"} identifies a WhatsApp Business 1P bot (Meta-hosted),
     * {@code "3"} identifies a 3P bot; absence means a non-business bot such as
     * Meta AI.
     *
     * @return an {@link Optional} wrapping the biz bot tier
     */
    public Optional<String> bizBotType() {
        return Optional.ofNullable(bizBotType);
    }
}
