package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents the footer of a product
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("Footer")
public class InteractiveFooter implements ProtobufMessage {
    /**
     * The footer of this product
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String content;
}