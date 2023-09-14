package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.BytesHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HexFormat;

/**
 * A model class that represents a hydrated quick reply button
 */
public record HydratedQuickReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String text,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String id
) implements HydratedButton {
    /**
     * Constructs a new HydratedQuickReplyButton from a text with a random id
     *
     * @param text the non-null text
     * @return a non-null HydratedQuickReplyButton
     */
    public static HydratedQuickReplyButton of(@NonNull String text) {
        var id = HexFormat.of().formatHex(BytesHelper.random(6));
        return new HydratedQuickReplyButton(text, id);
    }

    @Override
    public HydratedButtonType buttonType() {
        return HydratedButtonType.QUICK_REPLY;
    }
}