package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model class that represents an url button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class URLButton implements ProtobufMessage {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage text;

    /**
     * The url of this button
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage url;
}
