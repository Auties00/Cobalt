package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model class that represents a product
 */
@ProtobufMessage(name = "Message.ListMessage.Product")
public final class ProductSectionEntry {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    ProductSectionEntry(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductSectionEntry that
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProductSectionEntry[" +
                "id=" + id +
                ']';
    }
}