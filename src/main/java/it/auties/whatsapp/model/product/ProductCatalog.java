package it.auties.whatsapp.model.product;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ProductCatalog implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage catalogImage;

    @ProtobufProperty(index = 2, type = STRING)
    private String title;

    @ProtobufProperty(index = 3, type = STRING)
    private String description;
}
