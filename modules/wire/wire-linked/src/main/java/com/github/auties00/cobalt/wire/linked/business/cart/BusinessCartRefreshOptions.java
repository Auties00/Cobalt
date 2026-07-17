package com.github.auties00.cobalt.wire.linked.business.cart;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Input model for re-fetching a WhatsApp Business shopping cart through the
 * relay path.
 *
 * <p>A refresh names the business account whose cart is being re-quoted
 * and the cart's product ids, and optionally tunes how the server renders
 * the product imagery and which variant attributes it projects. The
 * direct-connection encrypted info is an opaque server-provided blob
 * threaded through the merchant direct-connection retry path; leaving it
 * unset uses the standard relay path. The variant-info field selector is a
 * comma-separated list of variant attribute field names the server should
 * include in each product entry; leaving it unset omits variant attributes
 * from the response.
 */
@ProtobufMessage(name = "BusinessCartRefreshOptions")
public final class BusinessCartRefreshOptions {
    /**
     * Business account whose cart is refreshed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid businessJid;

    /**
     * Retailer product ids in the cart. Defaults to {@link List#of()} when
     * unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> productIds;

    /**
     * Requested image width, in pixels. Unset lets the server pick a
     * default size.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final Integer imageWidth;

    /**
     * Requested image height, in pixels. Unset lets the server pick a
     * default size.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final Integer imageHeight;

    /**
     * Opaque direct-connection token threading the refresh through the
     * merchant direct-connection retry path. Unset uses the standard
     * relay path.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String directConnectionEncryptedInfo;

    /**
     * Comma-separated selector naming which variant attribute fields to
     * include in each product entry. Unset omits variant attributes from
     * the response.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String variantInfoFields;

    /**
     * Constructs a new {@code BusinessCartRefreshOptions}.
     *
     * @param businessJid                   the business account; required
     * @param productIds                    the product ids; never {@code null}, defaults to {@link List#of()}
     * @param imageWidth                    the image width, or {@code null}
     * @param imageHeight                   the image height, or {@code null}
     * @param directConnectionEncryptedInfo the direct-connection token, or {@code null}
     * @param variantInfoFields             the variant-info selector, or {@code null}
     * @throws NullPointerException if {@code businessJid} or {@code productIds}
     *                              is {@code null}
     */
    public BusinessCartRefreshOptions(Jid businessJid, List<String> productIds, Integer imageWidth,
                                      Integer imageHeight, String directConnectionEncryptedInfo,
                                      String variantInfoFields) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.productIds = Objects.requireNonNull(productIds, "productIds cannot be null");
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
        this.variantInfoFields = variantInfoFields;
    }

    /**
     * Convenience constructor that accepts any {@link JidProvider} and
     * resolves it to a {@link Jid}.
     *
     * @param businessJid                   the business account; required
     * @param productIds                    the product ids; never {@code null}
     * @param imageWidth                    the image width, or {@code null}
     * @param imageHeight                   the image height, or {@code null}
     * @param directConnectionEncryptedInfo the direct-connection token, or {@code null}
     * @param variantInfoFields             the variant-info selector, or {@code null}
     * @throws NullPointerException if {@code businessJid} or {@code productIds}
     *                              is {@code null}
     */
    public BusinessCartRefreshOptions(JidProvider businessJid, List<String> productIds,
                                      Integer imageWidth, Integer imageHeight,
                                      String directConnectionEncryptedInfo,
                                      String variantInfoFields) {
        this(Objects.requireNonNull(businessJid, "businessJid cannot be null").toJid(),
                productIds, imageWidth, imageHeight, directConnectionEncryptedInfo,
                variantInfoFields);
    }

    /**
     * Returns the business account whose cart is refreshed.
     *
     * @return the business JID, never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the retailer product ids.
     *
     * @return the product ids, never {@code null}
     */
    public List<String> productIds() {
        return productIds;
    }

    /**
     * Returns the requested image width in pixels.
     *
     * @return an {@code OptionalInt} carrying the width, or empty when unset
     */
    public OptionalInt imageWidth() {
        return imageWidth == null ? OptionalInt.empty() : OptionalInt.of(imageWidth);
    }

    /**
     * Returns the requested image height in pixels.
     *
     * @return an {@code OptionalInt} carrying the height, or empty when unset
     */
    public OptionalInt imageHeight() {
        return imageHeight == null ? OptionalInt.empty() : OptionalInt.of(imageHeight);
    }

    /**
     * Returns the opaque direct-connection token.
     *
     * @return an {@link Optional} carrying the token, or empty when unset
     */
    public Optional<String> directConnectionEncryptedInfo() {
        return Optional.ofNullable(directConnectionEncryptedInfo);
    }

    /**
     * Returns the variant-info field selector.
     *
     * @return an {@link Optional} carrying the selector, or empty when unset
     */
    public Optional<String> variantInfoFields() {
        return Optional.ofNullable(variantInfoFields);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCartRefreshOptions) obj;
        return Objects.equals(businessJid, that.businessJid)
                && Objects.equals(productIds, that.productIds)
                && Objects.equals(imageWidth, that.imageWidth)
                && Objects.equals(imageHeight, that.imageHeight)
                && Objects.equals(directConnectionEncryptedInfo, that.directConnectionEncryptedInfo)
                && Objects.equals(variantInfoFields, that.variantInfoFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid, productIds, imageWidth, imageHeight,
                directConnectionEncryptedInfo, variantInfoFields);
    }

    @Override
    public String toString() {
        return "BusinessCartRefreshOptions[" +
                "businessJid=" + businessJid + ", " +
                "productIds=" + productIds + ", " +
                "imageWidth=" + imageWidth + ", " +
                "imageHeight=" + imageHeight + ", " +
                "directConnectionEncryptedInfo=" + directConnectionEncryptedInfo + ", " +
                "variantInfoFields=" + variantInfoFields + ']';
    }
}
