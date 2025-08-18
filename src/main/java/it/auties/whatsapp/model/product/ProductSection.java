package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * A model class that represents a section inside a list of products
 */
@ProtobufMessage(name = "Message.ListMessage.ProductSection")
public final class ProductSection {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<ProductSectionEntry> products;

    ProductSection(String title, List<ProductSectionEntry> products) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.products = Objects.requireNonNullElse(products, List.of());
    }

    public String title() {
        return title;
    }

    public List<ProductSectionEntry> products() {
        return products;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductSection that
                && Objects.equals(title, that.title)
                && Objects.equals(products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, products);
    }

    @Override
    public String toString() {
        return "ProductSection[" +
                "title=" + title +
                ", products=" + products +
                ']';
    }
}