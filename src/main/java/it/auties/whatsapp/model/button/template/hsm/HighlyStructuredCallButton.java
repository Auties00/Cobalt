package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model class that represents a button that can start a phone call
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class HighlyStructuredCallButton implements HighlyStructuredButton {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = MESSAGE)
    private HighlyStructuredMessage text;

    /**
     * The phone number
     */
    @ProtobufProperty(index = 2, type = MESSAGE)
    private HighlyStructuredMessage phoneNumber;

    @Override
    public HighlyStructuredButtonType buttonType() {
        return HighlyStructuredButtonType.CALL;
    }
}
