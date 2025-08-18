package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.ImageMessage;

import java.util.Objects;

/**
 * A model class that represents a product catalog
 */
@ProtobufMessage(name = "Message.ProductMessage.CatalogSnapshot")
public final class ProductCatalog {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ImageMessage catalogImage;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String title;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String description;

    ProductCatalog(ImageMessage catalogImage, String title, String description) {
        this.catalogImage = Objects.requireNonNull(catalogImage, "catalogImage cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public ImageMessage catalogImage() {
        return catalogImage;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductCatalog that
                && Objects.equals(catalogImage, that.catalogImage)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogImage, title, description);
    }

    @Override
    public String toString() {
        return "ProductCatalog[" +
                "catalogImage=" + catalogImage +
                ", title=" + title +
                ", description=" + description +
                ']';
    }
}