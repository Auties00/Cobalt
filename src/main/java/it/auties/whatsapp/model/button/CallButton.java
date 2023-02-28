package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import lombok.AccessLevel;
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
public class CallButton implements ProtobufMessage {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage text;

    /**
     * The phone number
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage phoneNumber;
}
