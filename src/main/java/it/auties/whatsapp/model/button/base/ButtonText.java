package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents the text of a button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ButtonText implements ButtonBody {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String content;

    @Override
    public ButtonBodyType bodyType() {
        return ButtonBodyType.TEXT;
    }
}
