package com.github.auties00.cobalt.wire.linked.business.catalog;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Input model for editing one existing product in a WhatsApp Business
 * catalog.
 *
 * <p>An edit identifies the catalog and the target product, optionally
 * resizes the rendered product thumbnails, and carries the changed
 * product fields as a typed {@link CatalogProductInfo}. The thumbnail width
 * and height are independently optional; leaving either unset lets the
 * server pick a default size.
 */
@ProtobufMessage(name = "BusinessCatalogProductEdit")
public final class BusinessCatalogProductEdit {
    /**
     * Business account that owns the catalog the product is edited in.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid businessJid;

    /**
     * Server-assigned identifier of the product being edited.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String productId;

    /**
     * Requested thumbnail width, in pixels. Unset lets the server pick a
     * default size.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final Integer thumbnailWidth;

    /**
     * Requested thumbnail height, in pixels. Unset lets the server pick a
     * default size.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final Integer thumbnailHeight;

    /**
     * The changed product fields. Unset omits the product body from the
     * request.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final CatalogProductInfo productInfo;

    /**
     * Constructs a new {@code BusinessCatalogProductEdit}.
     *
     * @param businessJid     the business account that owns the catalog; required
     * @param productId       the identifier of the product to edit; required
     * @param thumbnailWidth  the requested thumbnail width, or {@code null}
     * @param thumbnailHeight the requested thumbnail height, or {@code null}
     * @param productInfo     the changed product fields, or {@code null}
     * @throws NullPointerException if {@code businessJid} or {@code productId}
     *                              is {@code null}
     */
    public BusinessCatalogProductEdit(Jid businessJid, String productId, Integer thumbnailWidth,
                                      Integer thumbnailHeight, CatalogProductInfo productInfo) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
        this.productInfo = productInfo;
    }

    /**
     * Convenience constructor that accepts any {@link JidProvider} and
     * resolves it to a {@link Jid}.
     *
     * @param businessJid     the business account that owns the catalog; required
     * @param productId       the identifier of the product to edit; required
     * @param thumbnailWidth  the requested thumbnail width, or {@code null}
     * @param thumbnailHeight the requested thumbnail height, or {@code null}
     * @param productInfo     the changed product fields, or {@code null}
     * @throws NullPointerException if {@code businessJid} or {@code productId}
     *                              is {@code null}
     */
    public BusinessCatalogProductEdit(JidProvider businessJid, String productId,
                                      Integer thumbnailWidth, Integer thumbnailHeight,
                                      CatalogProductInfo productInfo) {
        this(Objects.requireNonNull(businessJid, "businessJid cannot be null").toJid(),
                productId, thumbnailWidth, thumbnailHeight, productInfo);
    }

    /**
     * Returns the business account that owns the catalog.
     *
     * @return the business JID, never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the identifier of the product to edit.
     *
     * @return the product id, never {@code null}
     */
    public String productId() {
        return productId;
    }

    /**
     * Returns the requested thumbnail width in pixels.
     *
     * @return an {@code OptionalInt} carrying the width, or empty when unset
     */
    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    /**
     * Returns the requested thumbnail height in pixels.
     *
     * @return an {@code OptionalInt} carrying the height, or empty when unset
     */
    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    /**
     * Returns the changed product fields.
     *
     * @return an {@link Optional} carrying the product fields, or empty when unset
     */
    public Optional<CatalogProductInfo> productInfo() {
        return Optional.ofNullable(productInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogProductEdit) obj;
        return Objects.equals(businessJid, that.businessJid)
                && Objects.equals(productId, that.productId)
                && Objects.equals(thumbnailWidth, that.thumbnailWidth)
                && Objects.equals(thumbnailHeight, that.thumbnailHeight)
                && Objects.equals(productInfo, that.productInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid, productId, thumbnailWidth, thumbnailHeight, productInfo);
    }

    @Override
    public String toString() {
        return "BusinessCatalogProductEdit[" +
                "businessJid=" + businessJid + ", " +
                "productId=" + productId + ", " +
                "thumbnailWidth=" + thumbnailWidth + ", " +
                "thumbnailHeight=" + thumbnailHeight + ", " +
                "productInfo=" + productInfo + ']';
    }
}
