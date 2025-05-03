package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.product.ProductListHeaderImage;
import it.auties.whatsapp.model.product.ProductSection;

import java.util.List;
import java.util.Objects;

/**
 * A model class that holds the information related to a list of products.
 */
@ProtobufMessage(name = "Message.ListMessage.ProductListInfo")
public final class ProductListInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<ProductSection> productSections;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ProductListHeaderImage headerImage;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid sellerJid;

    ProductListInfo(List<ProductSection> productSections, ProductListHeaderImage headerImage, Jid sellerJid) {
        this.productSections = Objects.requireNonNullElse(productSections, List.of());
        this.headerImage = headerImage;
        this.sellerJid = Objects.requireNonNull(sellerJid, "sellerJid cannot be null");
    }

    public List<ProductSection> productSections() {
        return productSections;
    }

    public ProductListHeaderImage headerImage() {
        return headerImage;
    }

    public Jid sellerJid() {
        return sellerJid;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductListInfo that
                && Objects.equals(productSections, that.productSections)
                && Objects.equals(headerImage, that.headerImage)
                && Objects.equals(sellerJid, that.sellerJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productSections, headerImage, sellerJid);
    }

    @Override
    public String toString() {
        return "ProductListInfo[" +
                "productSections=" + productSections +
                ", headerImage=" + headerImage +
                ", sellerJid=" + sellerJid +
                ']';
    }
}
