package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.ImageMessage;

import java.util.Objects;

/**
 * A model class that represents a product
 */
@ProtobufMessage(name = "Message.ListMessage.Product")
public final class Product {

    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ImageMessage image;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String id;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String title;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String description;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String currencyCode;

    @ProtobufProperty(index = 6, type = ProtobufType.INT64)
    long priceAmount1000;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String retailerId;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String url;

    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    int productImageCount;

    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String firstImageId;

    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    long salePriceAmount1000;

    Product(ImageMessage image, String id, String title, String description, String currencyCode, long priceAmount1000, String retailerId, String url, int productImageCount, String firstImageId, long salePriceAmount1000) {
        this.image = Objects.requireNonNull(image, "image cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.currencyCode = Objects.requireNonNull(currencyCode, "currencyCode cannot be null");
        this.priceAmount1000 = priceAmount1000;
        this.retailerId = Objects.requireNonNull(retailerId, "retailerId cannot be null");
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.productImageCount = productImageCount;
        this.firstImageId = Objects.requireNonNull(firstImageId, "firstImageId cannot be null");
        this.salePriceAmount1000 = salePriceAmount1000;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product that
                && Objects.equals(image, that.image)
                && Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(currencyCode, that.currencyCode)
                && priceAmount1000 == that.priceAmount1000
                && Objects.equals(retailerId, that.retailerId)
                && Objects.equals(url, that.url)
                && productImageCount == that.productImageCount
                && Objects.equals(firstImageId, that.firstImageId)
                && salePriceAmount1000 == that.salePriceAmount1000;
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, id, title, description, currencyCode, priceAmount1000, retailerId, url, productImageCount, firstImageId, salePriceAmount1000);
    }

    @Override
    public String toString() {
        return "Product[" +
                "image=" + image +
                ", id=" + id +
                ", title=" + title +
                ", description=" + description +
                ", currencyCode=" + currencyCode +
                ", priceAmount1000=" + priceAmount1000 +
                ", retailerId=" + retailerId +
                ", url=" + url +
                ", productImageCount=" + productImageCount +
                ", firstImageId=" + firstImageId +
                ", salePriceAmount1000=" + salePriceAmount1000 +
                ']';
    }
}