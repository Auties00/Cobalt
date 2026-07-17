package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.catalog.CatalogProductInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that edits an existing product in a WhatsApp Business catalog.
 *
 * <p>The mutation takes one {@code input} GraphQL variable mapped onto the
 * {@code xfb_whatsapp_catalog_edit_product} field's {@code request} argument. WhatsApp Web's
 * {@code WAWebBizCatalogManagementEditProduct.editProduct(input)} forwards the object built by
 * {@code WAWebBizCatalogEditProductJob} as {@code {product: {biz_jid, product_id, width, height,
 * product_info}}}: the catalog-owner {@link Jid}, the id of the product being edited, the requested
 * image-render {@code width} and {@code height}, and the {@code product_info} {@link CatalogProductInfo}
 * carrying the fields to change. The Meta graph endpoint returns the edited product under
 * {@code xfb_whatsapp_catalog_edit_product}; the reply is consumed through
 * {@link BizCatalogManagementEditProductFacebookGraphQlResponse}.
 *
 * @implNote This implementation maps {@code product_info} from its typed model to snake_case JSON by
 * {@link BizCatalogInputJson}. The catalog-owner {@code biz_jid}, the {@code product_id}, and the
 * render dimensions remain typed scalar fields.
 *
 * @see BizCatalogManagementEditProductFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementEditProductMutation")
public final class BizCatalogManagementEditProductFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementEditProductMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9889773371084956";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementEditProductMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementEditProductMutation";

    /**
     * The {@code product.biz_jid} field naming the business account that owns the catalog, or
     * {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code product.product_id} field identifying the catalog product being edited, or
     * {@code null} to omit it.
     */
    private final String productId;

    /**
     * The {@code product.width} field requesting the rendered image width, or {@code null} to omit it.
     */
    private final Integer width;

    /**
     * The {@code product.height} field requesting the rendered image height, or {@code null} to omit
     * it.
     */
    private final Integer height;

    /**
     * The {@code product.product_info} write-model carrying the product fields to change, or
     * {@code null} to omit it.
     */
    private final CatalogProductInfo productInfo;

    /**
     * Constructs an edit-product mutation request.
     *
     * <p>All values populate the nested {@code product} GraphQL object; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid      the catalog-owner {@link Jid}, or {@code null} to omit the field
     * @param productId   the id of the product being edited, or {@code null} to omit the field
     * @param width       the requested rendered image width, or {@code null} to omit the field
     * @param height      the requested rendered image height, or {@code null} to omit the field
     * @param productInfo the {@code product_info} write-model, or {@code null} to omit the field
     */
    public BizCatalogManagementEditProductFacebookGraphQlRequest(Jid bizJid, String productId, Integer width, Integer height, CatalogProductInfo productInfo) {
        this.bizJid = bizJid;
        this.productId = productId;
        this.width = width;
        this.height = height;
        this.productInfo = productInfo;
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
     * @implNote This implementation emits {@code {"input": {"product": {"biz_jid": <bizJid>,
     * "product_id": <productId>, "width": <width>, "height": <height>, "product_info": {...}}}}},
     * writing each sub-field only when its value is non-null. The {@code biz_jid} is rendered as its
     * canonical {@link Jid} string and the {@code product_info} object is mapped by
     * {@link BizCatalogInputJson#writeCatalogProductInfo(JSONWriter, CatalogProductInfo)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementEditProduct", exports = "editProduct",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("product");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (productId != null) {
                writer.writeName("product_id");
                writer.writeColon();
                writer.writeString(productId);
            }

            if (width != null) {
                writer.writeName("width");
                writer.writeColon();
                writer.writeString(String.valueOf(width));
            }

            if (height != null) {
                writer.writeName("height");
                writer.writeColon();
                writer.writeString(String.valueOf(height));
            }

            if (productInfo != null) {
                writer.writeName("product_info");
                writer.writeColon();
                BizCatalogInputJson.writeCatalogProductInfo(writer, productInfo);
            }
            writer.endObject();
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
