package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A single per-product visibility toggle for a WhatsApp Business catalog.
 *
 * <p>The merchant can hide individual products from customers without
 * deleting them from the catalog. The toggle takes a list of these entries:
 * each names the affected product and whether it should be hidden from the
 * customer-facing surfaces. Hidden products remain visible to the merchant
 * in the catalog editor but disappear from buyer views.
 */
@ProtobufMessage(name = "BusinessCatalogProductVisibility")
public final class BusinessCatalogProductVisibility {
    /**
     * The server-issued identifier of the catalog product whose visibility
     * changes. Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String productId;

    /**
     * Whether the product is to be hidden from customers; {@code true}
     * hides it, {@code false} makes it visible.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean hidden;

    /**
     * Constructs a new {@code BusinessCatalogProductVisibility} from the
     * product identifier and the hidden flag.
     *
     * @param productId the product identifier whose visibility changes;
     *                  never {@code null}
     * @param hidden    {@code true} to hide the product, {@code false} to
     *                  make it visible
     * @throws NullPointerException if {@code productId} is {@code null}
     */
    BusinessCatalogProductVisibility(String productId, boolean hidden) {
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.hidden = hidden;
    }

    /**
     * Returns the server-issued identifier of the catalog product whose
     * visibility changes.
     *
     * @return the product identifier; never {@code null}
     */
    public String productId() {
        return productId;
    }

    /**
     * Returns whether the product is to be hidden from customers.
     *
     * @return {@code true} when the product is to be hidden, {@code false}
     *         when it is to be visible
     */
    public boolean hidden() {
        return hidden;
    }

    /**
     * Sets the server-issued identifier of the catalog product whose
     * visibility changes.
     *
     * @param productId the product identifier to set
     * @throws NullPointerException if {@code productId} is {@code null}
     */
    public void setProductId(String productId) {
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
    }

    /**
     * Sets whether the product is to be hidden from customers.
     *
     * @param hidden {@code true} to hide the product, {@code false} to make
     *               it visible
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogProductVisibility) obj;
        return Objects.equals(this.productId, that.productId)
                && this.hidden == that.hidden;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, hidden);
    }

    @Override
    public String toString() {
        return "BusinessCatalogProductVisibility[" +
                "productId=" + productId + ", " +
                "hidden=" + hidden + ']';
    }
}
