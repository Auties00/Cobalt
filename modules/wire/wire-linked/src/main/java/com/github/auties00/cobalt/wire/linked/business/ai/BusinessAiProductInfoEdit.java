package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for saving changes to a product the WhatsApp Business AI agent
 * can describe to customers.
 *
 * <p>An edit names the existing product info entry by its server-assigned
 * id and carries the changed fields. The display name, structured price
 * (as a sub-object JSON literal so the AI agent can reproduce tiered or
 * range pricing), and description are individually optional; leaving any
 * unset keeps that field's current value. Imagery comes from two parallel
 * sources: local file paths uploaded fresh through the WhatsApp Business
 * media pipeline, and references to images already uploaded that the
 * merchant is keeping or reusing.
 */
@ProtobufMessage(name = "BusinessAiProductInfoEdit")
public final class BusinessAiProductInfoEdit {
    /**
     * Server-assigned identifier of the product info entry being edited.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String productInfoId;

    /**
     * New display name. Unset leaves the existing name unchanged.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Pre-encoded JSON object literal carrying the structured product
     * price (tiered, range, or scalar). Unset leaves the existing price
     * unchanged.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String priceJson;

    /**
     * New free-form product description. Unset leaves the existing
     * description unchanged.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String description;

    /**
     * Local file paths fed to the WhatsApp Business media pipeline for
     * fresh upload. Defaults to {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final List<String> localImageFilePaths;

    /**
     * References to images already uploaded that the product is keeping or
     * reusing. Defaults to {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final List<String> imageReferences;

    /**
     * Constructs a new {@code BusinessAiProductInfoEdit}.
     *
     * @param productInfoId       the identifier of the product info entry; required
     * @param name                the new display name, or {@code null}
     * @param priceJson           the pre-encoded structured price, or {@code null}
     * @param description         the new description, or {@code null}
     * @param localImageFilePaths the local image paths to upload; never
     *                            {@code null}, defaults to {@link List#of()}
     * @param imageReferences     the references to existing images; never
     *                            {@code null}, defaults to {@link List#of()}
     * @throws NullPointerException if {@code productInfoId},
     *                              {@code localImageFilePaths}, or
     *                              {@code imageReferences} is {@code null}
     */
    public BusinessAiProductInfoEdit(String productInfoId, String name, String priceJson, String description,
                                     List<String> localImageFilePaths, List<String> imageReferences) {
        this.productInfoId = Objects.requireNonNull(productInfoId, "productInfoId cannot be null");
        this.name = name;
        this.priceJson = priceJson;
        this.description = description;
        this.localImageFilePaths = Objects.requireNonNull(localImageFilePaths,
                "localImageFilePaths cannot be null");
        this.imageReferences = Objects.requireNonNull(imageReferences,
                "imageReferences cannot be null");
    }

    /**
     * Returns the identifier of the product info entry being edited.
     *
     * @return the product info id, never {@code null}
     */
    public String productInfoId() {
        return productInfoId;
    }

    /**
     * Returns the new display name.
     *
     * @return an {@link Optional} carrying the new name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the pre-encoded structured price JSON.
     *
     * @return an {@link Optional} carrying the price JSON, or empty when unset
     */
    public Optional<String> priceJson() {
        return Optional.ofNullable(priceJson);
    }

    /**
     * Returns the new product description.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the local image file paths to upload.
     *
     * @return the image paths, never {@code null}
     */
    public List<String> localImageFilePaths() {
        return localImageFilePaths;
    }

    /**
     * Returns the references to existing images.
     *
     * @return the image references, never {@code null}
     */
    public List<String> imageReferences() {
        return imageReferences;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAiProductInfoEdit) obj;
        return Objects.equals(productInfoId, that.productInfoId)
                && Objects.equals(name, that.name)
                && Objects.equals(priceJson, that.priceJson)
                && Objects.equals(description, that.description)
                && Objects.equals(localImageFilePaths, that.localImageFilePaths)
                && Objects.equals(imageReferences, that.imageReferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productInfoId, name, priceJson, description,
                localImageFilePaths, imageReferences);
    }

    @Override
    public String toString() {
        return "BusinessAiProductInfoEdit[" +
                "productInfoId=" + productInfoId + ", " +
                "name=" + name + ", " +
                "priceJson=" + priceJson + ", " +
                "description=" + description + ", " +
                "localImageFilePaths=" + localImageFilePaths + ", " +
                "imageReferences=" + imageReferences + ']';
    }
}
