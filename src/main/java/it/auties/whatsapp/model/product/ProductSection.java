package it.auties.whatsapp.model.product;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a section inside a list of products
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ProductSection implements ProtobufMessage {
    /**
     * The title of the section
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The products in this section
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ProductSectionEntry.class, repeated = true)
    private List<ProductSectionEntry> products;
}
