package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a hydrated button that can start a phone call
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public non-sealed class HydratedCallButton implements HydratedButton {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String text;

    /**
     * The phone numberWithoutPrefix of this button
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String phoneNumber;

    @Override
    public HydratedButtonType buttonType() {
        return HydratedButtonType.CALL;
    }
}
