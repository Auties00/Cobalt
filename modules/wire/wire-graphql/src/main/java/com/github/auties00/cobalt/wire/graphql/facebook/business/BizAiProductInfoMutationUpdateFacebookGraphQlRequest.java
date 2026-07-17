package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that updates a WhatsApp Business AI agent product-info knowledge entry.
 *
 * <p>The mutation takes three GraphQL variables: an {@code id} naming the product-info knowledge
 * entry to update, a {@code product_params} object carrying the new product fields, and a
 * {@code media_options} object carrying the thumbnail dimensions. WhatsApp Web's
 * {@code WAWebBizAiProductInfoMutation.updateProductInfo(id, product)} builds {@code product_params}
 * with {@code {name, complex_price, description}} plus the optional {@code manifold_image_file_paths}
 * and {@code images} lists, and a constant {@code media_options} of
 * {@code {thumbnail_height: "76", thumbnail_width: "76"}}. The Meta graph endpoint returns the update outcome under
 * the linked field {@code xfb_maiba_update_product_info_knowledge}; the reply is consumed through
 * {@link BizAiProductInfoMutationUpdateFacebookGraphQlResponse}.
 *
 * <p>The {@code id} is a product-info knowledge entry identifier rather than a WhatsApp address, so
 * it is modelled as a {@link String}. The {@code complex_price} field is the product price; WhatsApp
 * Web forwards the caller's {@code price} value through unchanged and its structure is not confirmed
 * from the bundle, so it is modelled as a {@link String} (mirroring the price field on
 * {@link com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiProductInfo}). The {@code images} field carries the
 * caller's existing product image references; their element shape is not confirmed from the bundle,
 * so they are modelled as a {@link String} list.
 *
 * @see BizAiProductInfoMutationUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiProductInfoMutationUpdateMutation")
public final class BizAiProductInfoMutationUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutationUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26619965944279430";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutationUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiProductInfoMutationUpdateMutation";

    /**
     * The default thumbnail edge size WhatsApp Web sets on both {@code media_options} dimensions.
     */
    private static final String DEFAULT_THUMBNAIL_SIZE = "76";

    /**
     * The {@code id} GraphQL variable naming the product-info knowledge entry to update, or
     * {@code null} to omit it.
     */
    private final String id;

    /**
     * The {@code name} field of the {@code product_params} object, or {@code null} to omit it.
     */
    private final String name;

    /**
     * The {@code complex_price} field of the {@code product_params} object holding the product price,
     * or {@code null} to omit it.
     */
    private final String complexPrice;

    /**
     * The {@code description} field of the {@code product_params} object, or {@code null} to omit it.
     */
    private final String description;

    /**
     * The {@code manifold_image_file_paths} field of the {@code product_params} object listing the
     * newly-uploaded image file paths, or {@code null} to omit it.
     */
    private final List<String> manifoldImageFilePaths;

    /**
     * The {@code images} field of the {@code product_params} object listing the caller's existing
     * product image references, or {@code null} to omit it.
     */
    private final List<String> images;

    /**
     * Constructs an update-product-info mutation request.
     *
     * <p>The {@code id} names the product-info knowledge entry to update. The {@code name},
     * {@code complexPrice}, and {@code description} populate the {@code product_params} object's
     * scalar fields. The {@code manifoldImageFilePaths} and {@code images} populate its optional list
     * fields. Each value that is {@code null} omits its field from the serialized object; the
     * {@code media_options} object is always emitted with the default thumbnail dimensions.
     *
     * @param id                     the product-info knowledge entry id to update, or {@code null} to
     *                               omit the variable
     * @param name                   the product name, or {@code null} to omit the field
     * @param complexPrice           the product price, or {@code null} to omit the field
     * @param description            the product description, or {@code null} to omit the field
     * @param manifoldImageFilePaths the newly-uploaded image file paths, or {@code null} to omit the
     *                               field
     * @param images                 the existing product image references, or {@code null} to omit
     *                               the field
     */
    public BizAiProductInfoMutationUpdateFacebookGraphQlRequest(String id, String name, String complexPrice, String description, List<String> manifoldImageFilePaths, List<String> images) {
        this.id = id;
        this.name = name;
        this.complexPrice = complexPrice;
        this.description = description;
        this.manifoldImageFilePaths = manifoldImageFilePaths;
        this.images = images;
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
     * @implNote This implementation emits
     * {@code {"id": <id>, "product_params": {...}, "media_options": {"thumbnail_height": "76",
     * "thumbnail_width": "76"}}}. The {@code id} variable and each {@code product_params} field are
     * written only when non-null, with the {@code manifold_image_file_paths} and {@code images} lists
     * serialized as JSON arrays; the {@code media_options} object is always present and constant,
     * matching the WhatsApp Web {@code thumbnail_height}/{@code thumbnail_width} default of
     * {@code "76"}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutation", exports = "updateProductInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
            }

            writer.writeName("product_params");
            writer.writeColon();
            writer.startObject();
            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            if (complexPrice != null) {
                writer.writeName("complex_price");
                writer.writeColon();
                writer.writeString(complexPrice);
            }

            if (description != null) {
                writer.writeName("description");
                writer.writeColon();
                writer.writeString(description);
            }

            if (manifoldImageFilePaths != null) {
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

            if (images != null) {
                writer.writeName("images");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < images.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(images.get(i));
                }
                writer.endArray();
            }
            writer.endObject();

            writer.writeName("media_options");
            writer.writeColon();
            writer.startObject();
            writer.writeName("thumbnail_height");
            writer.writeColon();
            writer.writeString(DEFAULT_THUMBNAIL_SIZE);
            writer.writeName("thumbnail_width");
            writer.writeColon();
            writer.writeString(DEFAULT_THUMBNAIL_SIZE);
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
