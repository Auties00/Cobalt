package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * Wrapper for plain-text or markdown content carried by an
 * {@link AIRichResponseSubMessage} fragment within a WhatsApp AI bot
 * rich response.
 *
 * <p>This type exists so that text content can participate in the
 * {@link AIRichResponseSubMessageContent} sealed hierarchy alongside
 * the metadata message types. On the protobuf wire it is encoded as a
 * plain {@code STRING} (field index 3 of
 * {@code AIRichResponseSubMessage}); the {@link #of(String)} deserializer
 * and {@link #value()} serializer handle the conversion transparently.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#TEXT TEXT} variant
 * within an {@code AIRichResponseSubMessage}.
 *
 * <p>Example:
 * <pre>{@code
 *     var text = AIRichResponseText.of("Here is the information you requested:");
 *     var subMessage = new AIRichResponseSubMessageBuilder()
 *         .content(text)
 *         .build();
 * }</pre>
 *
 * @see AIRichResponseSubMessage#content()
 */
public final class AIRichResponseText implements AIRichResponseSubMessageContent {
    /**
     * The text content of this fragment, which may contain plain text
     * or markdown formatting.
     */
    private final String value;

    /**
     * Constructs a new {@code AIRichResponseText} with the specified
     * text value.
     *
     * @param value the text content
     */
    private AIRichResponseText(String value) {
        this.value = value;
    }

    /**
     * Deserializes a plain {@code STRING} from the protobuf wire
     * format into an {@code AIRichResponseText} instance.
     *
     * @param value the raw string value, or {@code null}
     * @return a new {@code AIRichResponseText} wrapping the value, or
     *         {@code null} if the input is {@code null}
     */
    @ProtobufDeserializer
    public static AIRichResponseText of(String value) {
        return value == null ? null : new AIRichResponseText(value);
    }

    /**
     * Returns the text content of this fragment.
     *
     * @return the text content, never {@code null}
     */
    @ProtobufSerializer
    public String value() {
        return value;
    }
}
