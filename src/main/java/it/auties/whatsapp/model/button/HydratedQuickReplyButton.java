package it.auties.whatsapp.model.button;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a hydrated quick reply button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HydratedQuickReplyButton implements ProtobufMessage {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String text;

    /**
     * The id of this button
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String id;

    /**
     * Constructs a new HydratedQuickReplyButton from a text with a random id
     *
     * @param text the non-null text
     * @return a non-null HydratedQuickReplyButton
     */
    public static HydratedQuickReplyButton of(@NonNull String text) {
        return new HydratedQuickReplyButton(text, Bytes.ofRandom(6).toHex());
    }
}