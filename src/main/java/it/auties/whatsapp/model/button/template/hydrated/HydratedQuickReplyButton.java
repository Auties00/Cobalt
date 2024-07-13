package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;

/**
 * A model class that represents a hydrated quick reply button
 */
@ProtobufMessage(name = "HydratedTemplateButton.HydratedQuickReplyButton")
public record HydratedQuickReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id
) implements HydratedButton {
    /**
     * Constructs a new HydratedQuickReplyButton from a text with a random id
     *
     * @param text the non-null text
     * @return a non-null HydratedQuickReplyButton
     */
    public static HydratedQuickReplyButton of(String text) {
        var id = HexFormat.of().formatHex(Bytes.random(6));
        return new HydratedQuickReplyButton(text, id);
    }

    @Override
    public Type buttonType() {
        return Type.QUICK_REPLY;
    }
}