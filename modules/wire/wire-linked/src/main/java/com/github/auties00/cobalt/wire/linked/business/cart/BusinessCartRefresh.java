package com.github.auties00.cobalt.wire.linked.business.cart;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.refreshBusinessCart}. Carries
 * the merchant JID, the product ids to re-quote, the requested image
 * dimensions, and an optional direct-connection encryption blob for
 * merchants that route via the direct-connection path.
 *
 * <p>{@link #bizJid} and {@link #productIds} are required;
 * {@link #productIds} must be non-empty.
 * {@link #directConnectionEncryptedInfo} is optional.
 */
@ProtobufMessage
public final class BusinessCartRefresh {
    /**
     * Merchant JID owning the products being re-quoted.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid bizJid;

    /**
     * Catalog product ids to refresh; must be non-empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> productIds;

    /**
     * Requested product-image width.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int width;

    /**
     * Requested product-image height.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int height;

    /**
     * Optional direct-connection encryption blob; non-{@code null} for
     * merchants that route through the direct-connection path.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String directConnectionEncryptedInfo;

    /**
     * Constructs a new {@code BusinessCartRefresh}.
     *
     * @param bizJid                         the merchant JID; required
     * @param productIds                     the product ids; required, non-empty
     * @param width                          the requested image width
     * @param height                         the requested image height
     * @param directConnectionEncryptedInfo  the optional direct-connection
     *                                       blob, or {@code null}
     * @throws NullPointerException if {@code bizJid} or {@code productIds}
     *                              is {@code null}
     */
    BusinessCartRefresh(Jid bizJid, List<String> productIds, int width, int height,
                        String directConnectionEncryptedInfo) {
        this.bizJid = Objects.requireNonNull(bizJid, "bizJid cannot be null");
        this.productIds = Objects.requireNonNull(productIds, "productIds cannot be null");
        this.width = width;
        this.height = height;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
    }

    /**
     * Returns the merchant JID.
     *
     * @return the merchant JID, never {@code null}
     */
    public Jid bizJid() {
        return bizJid;
    }

    /**
     * Returns the product ids.
     *
     * @return the product ids, never {@code null}
     */
    public List<String> productIds() {
        return productIds;
    }

    /**
     * Returns the requested image width.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the requested image height.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the optional direct-connection encryption blob.
     *
     * @return an {@link Optional} carrying the blob, or empty when unset
     */
    public Optional<String> directConnectionEncryptedInfo() {
        return Optional.ofNullable(directConnectionEncryptedInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartRefresh) obj;
        return Objects.equals(bizJid, that.bizJid) &&
                Objects.equals(productIds, that.productIds) &&
                width == that.width &&
                height == that.height &&
                Objects.equals(directConnectionEncryptedInfo, that.directConnectionEncryptedInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bizJid, productIds, width, height, directConnectionEncryptedInfo);
    }

    @Override
    public String toString() {
        return "BusinessCartRefresh[" +
                "bizJid=" + bizJid + ", " +
                "productIds=" + productIds + ", " +
                "width=" + width + ", " +
                "height=" + height + ", " +
                "directConnectionEncryptedInfo=" + directConnectionEncryptedInfo + ']';
    }
}
