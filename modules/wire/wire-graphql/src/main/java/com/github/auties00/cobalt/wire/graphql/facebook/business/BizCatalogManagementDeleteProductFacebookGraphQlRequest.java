package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that deletes one or more products from a WhatsApp Business catalog.
 *
 * <p>The mutation takes one {@code input} GraphQL variable mapped onto the
 * {@code xfb_whatsapp_catalog_delete_product} field's {@code request} argument. WhatsApp Web's
 * {@code WAWebBizCatalogManagementDeleteProduct.deleteProduct(input)} forwards the object built by
 * {@code WAWebBizCatalogDeleteProductsJob} as {@code {biz_jid, product_ids}}: the business account
 * {@link Jid} owning the catalog and the list of retailer-assigned product ids to remove. The Meta graph endpoint
 * returns the delete outcome under {@code xfb_whatsapp_catalog_delete_product}; the reply is consumed
 * through {@link BizCatalogManagementDeleteProductFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementDeleteProductFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementDeleteProductMutation")
public final class BizCatalogManagementDeleteProductFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteProductMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9376108569185474";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteProductMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementDeleteProductMutation";

    /**
     * The {@code biz_jid} field of the {@code input} object naming the business account whose catalog
     * the products belong to, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code product_ids} field of the {@code input} object listing the retailer-assigned product
     * ids to delete, or {@code null} to omit it.
     */
    private final List<String> productIds;

    /**
     * Constructs a delete-product mutation request carrying the catalog owner and the product ids to
     * remove.
     *
     * <p>Both values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object, and an empty {@code productIds} list serializes as an empty
     * array.
     *
     * @param bizJid     the business account {@link Jid} owning the catalog, or {@code null} to omit
     *                   the field
     * @param productIds the retailer-assigned product ids to delete, or {@code null} to omit the field
     */
    public BizCatalogManagementDeleteProductFacebookGraphQlRequest(Jid bizJid, List<String> productIds) {
        this.bizJid = bizJid;
        this.productIds = productIds;
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
     * @implNote This implementation emits {@code {"input": {"biz_jid": <bizJid>, "product_ids":
     * [...]}}}, writing each field only when its value is non-null and emitting {@code {"input": {}}}
     * when both are {@code null}. The {@code biz_jid} is rendered as its canonical {@link Jid} string.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementDeleteProduct", exports = "deleteProduct",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (productIds != null) {
                writer.writeName("product_ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < productIds.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(productIds.get(i));
                }
                writer.endArray();
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
