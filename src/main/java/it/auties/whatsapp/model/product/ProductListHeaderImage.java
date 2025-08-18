package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

/**
 * A model class that represents the header of a product list
 */
@ProtobufMessage(name = "Message.ListMessage.ProductListHeaderImage")
public final class ProductListHeaderImage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] thumbnail;

    ProductListHeaderImage(String id, byte[] thumbnail) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.thumbnail = Objects.requireNonNull(thumbnail, "thumbnail cannot be null");
    }

    public String id() {
        return id;
    }

    public byte[] thumbnail() {
        return thumbnail;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductListHeaderImage that
                && Objects.equals(id, that.id)
                && Arrays.equals(thumbnail, that.thumbnail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, Arrays.hashCode(thumbnail));
    }

    @Override
    public String toString() {
        return "ProductListHeaderImage[" +
                "id=" + id +
                ", thumbnail=" + Arrays.toString(thumbnail) +
                ']';
    }
}