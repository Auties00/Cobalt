package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.AiProductImage;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that creates product-info knowledge for a WhatsApp Business AI agent.
 *
 * <p>The operation takes two GraphQL variables. The {@code input} object describes the product to
 * register and the {@code media_options} object carries the thumbnail-rendering hints. WhatsApp Web's
 * {@code WAWebBizAiProductInfoMutation.createProductInfo(product)} builds the {@code input} object as
 * {@code {name, complex_price, description, manifold_image_file_paths?, images?}} (writing the freshly
 * uploaded image file paths only when non-empty and the {@link AiProductImage existing-image
 * references} only when present) and pins {@code media_options} to {@code {thumbnail_height: "76",
 * thumbnail_width: "76"}}. The Meta graph endpoint returns the creation outcome under
 * {@code xfb_maiba_create_product_info_knowledge}; the reply is consumed through
 * {@link BizAiProductInfoFacebookGraphQlResponse}.
 *
 * @implNote This implementation models the {@code input} scalars ({@code name}, {@code description},
 * {@code complex_price}) as typed fields and the {@code manifold_image_file_paths} and {@code images}
 * members as typed lists. To match the dispatcher, {@code complex_price} and {@code description} are
 * always emitted (as JSON {@code null} when no value is supplied) while {@code images} and
 * {@code manifold_image_file_paths} are written only when non-empty.
 *
 * @see BizAiProductInfoFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiProductInfoMutation")
public final class BizAiProductInfoFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26665182546499350";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiProductInfoMutation";

    /**
     * The default thumbnail dimension WhatsApp Web pins on both axes of the {@code media_options}
     * object.
     */
    private static final String DEFAULT_THUMBNAIL_DIMENSION = "76";

    /**
     * The {@code name} field of the {@code input} object naming the product, or {@code null} to omit
     * it.
     */
    private final String name;

    /**
     * The {@code complex_price} member of the {@code input} object carrying the product price as a
     * plain string (the trimmed price text the merchant entered), or {@code null} to emit a JSON
     * {@code null}. {@code WAWebBizAiProductInfoMutation} maps this input straight to
     * {@code complex_price: input.price}, a scalar, so no structured price object applies.
     */
    private final String complexPrice;

    /**
     * The {@code description} field of the {@code input} object describing the product, or
     * {@code null} to emit a JSON {@code null}.
     */
    private final String description;

    /**
     * The {@code manifold_image_file_paths} field of the {@code input} object listing the freshly
     * uploaded image file paths, written only when non-empty.
     */
    private final List<String> manifoldImageFilePaths;

    /**
     * The {@code images} member of the {@code input} object referencing the already-uploaded product
     * images, written only when non-empty. Never {@code null} after construction.
     */
    private final List<AiProductImage> existingImages;

    /**
     * The {@code thumbnail_height} field of the {@code media_options} object, or {@code null} to omit
     * it.
     */
    private final String thumbnailHeight;

    /**
     * The {@code thumbnail_width} field of the {@code media_options} object, or {@code null} to omit
     * it.
     */
    private final String thumbnailWidth;

    /**
     * Constructs a create-product-info mutation request with the thumbnail dimensions pinned to
     * WhatsApp Web's default.
     *
     * <p>Equivalent to {@link #BizAiProductInfoFacebookGraphQlRequest(String, String, String, List, List,
     * String, String)} with both thumbnail dimensions set to the {@value #DEFAULT_THUMBNAIL_DIMENSION}
     * default WhatsApp Web sends.
     *
     * @param name                   the product name, or {@code null} to omit the field
     * @param complexPrice           the {@code complex_price} value, or {@code null} to emit a JSON
     *                               {@code null}
     * @param description            the product description, or {@code null} to emit a JSON
     *                               {@code null}
     * @param manifoldImageFilePaths the freshly uploaded image file paths, written only when non-empty
     * @param existingImages         the already-uploaded image references, written only when non-empty
     */
    public BizAiProductInfoFacebookGraphQlRequest(String name, String complexPrice, String description, List<String> manifoldImageFilePaths, List<AiProductImage> existingImages) {
        this(name, complexPrice, description, manifoldImageFilePaths, existingImages, DEFAULT_THUMBNAIL_DIMENSION, DEFAULT_THUMBNAIL_DIMENSION);
    }

    /**
     * Constructs a create-product-info mutation request.
     *
     * <p>The first five arguments populate the {@code input} object and the last two populate the
     * {@code media_options} object. Each value that is {@code null} or, for
     * {@code manifoldImageFilePaths}, empty is omitted as documented on the corresponding field; the
     * {@code complexPriceJson} and {@code description} members are emitted as JSON {@code null} rather
     * than omitted, matching the dispatcher.
     *
     * @param name                   the product name, or {@code null} to omit the field
     * @param complexPrice           the {@code complex_price} value, or {@code null} to emit a JSON
     *                               {@code null}
     * @param description            the product description, or {@code null} to emit a JSON
     *                               {@code null}
     * @param manifoldImageFilePaths the freshly uploaded image file paths, written only when non-empty
     * @param existingImages         the already-uploaded image references, written only when non-empty
     * @param thumbnailHeight        the {@code thumbnail_height} hint, or {@code null} to omit the
     *                               field
     * @param thumbnailWidth         the {@code thumbnail_width} hint, or {@code null} to omit the
     *                               field
     */
    public BizAiProductInfoFacebookGraphQlRequest(String name, String complexPrice, String description, List<String> manifoldImageFilePaths, List<AiProductImage> existingImages, String thumbnailHeight, String thumbnailWidth) {
        this.name = name;
        this.complexPrice = complexPrice;
        this.description = description;
        this.manifoldImageFilePaths = manifoldImageFilePaths == null ? List.of() : List.copyOf(manifoldImageFilePaths);
        this.existingImages = existingImages == null ? List.of() : List.copyOf(existingImages);
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {...}, "media_options": {...}}}. Inside
     * {@code input} it writes {@code name} when non-null, always writes {@code complex_price} and
     * {@code description} (as JSON {@code null} when not supplied), writes
     * {@code manifold_image_file_paths} as a string array only when the list is non-empty, and writes
     * {@code images} as an array of {@code {image_id, image_url}} objects (via
     * {@link BizAiInputJson#writeProductImage(JSONWriter, AiProductImage)}) only when the list is
     * non-empty. Inside {@code media_options} it writes each thumbnail dimension when non-null.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutation", exports = "createProductInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();

            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            writer.writeName("complex_price");
            writer.writeColon();
            if (complexPrice != null) {
                writer.writeString(complexPrice);
            } else {
                writer.writeNull();
            }

            writer.writeName("description");
            writer.writeColon();
            if (description != null) {
                writer.writeString(description);
            } else {
                writer.writeNull();
            }

            if (!manifoldImageFilePaths.isEmpty()) {
                writer.writeName("manifold_image_file_paths");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < manifoldImageFilePaths.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(manifoldImageFilePaths.get(i));
                }
                writer.endArray();
            }

            if (!existingImages.isEmpty()) {
                writer.writeName("images");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < existingImages.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    BizAiInputJson.writeProductImage(writer, existingImages.get(i));
                }
                writer.endArray();
            }
            writer.endObject();

            writer.writeName("media_options");
            writer.writeColon();
            writer.startObject();
            if (thumbnailHeight != null) {
                writer.writeName("thumbnail_height");
                writer.writeColon();
                writer.writeString(thumbnailHeight);
            }

            if (thumbnailWidth != null) {
                writer.writeName("thumbnail_width");
                writer.writeColon();
                writer.writeString(thumbnailWidth);
            }
            writer.endObject();

            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
