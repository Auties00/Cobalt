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
 * Builds the Facebook GraphQL mutation that toggles the visibility of WhatsApp Business catalog products.
 *
 * <p>The single {@code input} GraphQL variable carries the owning {@code jid} and a {@code products}
 * array of per-product visibility toggles. WhatsApp Web's
 * {@code WAWebBizCatalogProductVisibilitySetJob.updateProductVisibilityGraphQL} fills it with
 * {@code {jid, products: [{product_id, is_hidden}]}}, one {@link Product} entry per product whose
 * visibility changes. The Meta graph endpoint returns the update outcome under
 * {@code xfb_whatsapp_catalog_product_visibility_update}; the reply is consumed through
 * {@link BizCatalogManagementUpdateProductVisibilityFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementUpdateProductVisibilityFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateProductVisibilityMutation")
public final class BizCatalogManagementUpdateProductVisibilityFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateProductVisibilityMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9665162096898581";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateProductVisibilityMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementUpdateProductVisibilityMutation";

    /**
     * The {@code input.jid} field naming the business account that owns the products, or {@code null}
     * to omit it.
     */
    private final Jid jid;

    /**
     * The {@code input.products} per-product visibility toggles, or {@code null} to omit the
     * {@code products} array.
     */
    private final List<Product> products;

    /**
     * Constructs an update-product-visibility mutation request carrying the owning business account
     * and the list of per-product visibility toggles.
     *
     * <p>Both values populate the {@code input} GraphQL object; a {@code null} {@code jid} omits the
     * {@code jid} field, a {@code null} {@code products} omits the {@code products} array, and an empty
     * {@code products} serializes as an empty array.
     *
     * @param jid      the business account {@link Jid} that owns the products, or {@code null} to omit
     *                 the field
     * @param products the per-product visibility toggles, or {@code null} to omit the {@code products}
     *                 array
     */
    public BizCatalogManagementUpdateProductVisibilityFacebookGraphQlRequest(Jid jid, List<Product> products) {
        this.jid = jid;
        this.products = products;
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
     * @implNote This implementation emits {@code {"input": {"jid": <jid>, "products": [{"product_id":
     * ..., "is_hidden": ...}, ...]}}}, writing the {@code jid} field only when it is non-null and the
     * {@code products} array only when {@code products} is non-null; an empty {@code input} object is
     * emitted when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogProductVisibilitySetJob", exports = "updateProductVisibilityGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            if (products != null) {
                writer.writeName("products");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < products.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    products.get(i).write(writer);
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

    /**
     * Describes a single per-product visibility toggle within the {@code input.products} array.
     *
     * <p>Each entry names the {@code product_id} being toggled and the {@code is_hidden} flag stating
     * whether the product is to be hidden from the catalog.
     */
    public static final class Product {
        /**
         * The {@code product_id} of the product whose visibility changes.
         *
         * <p>This is an opaque catalog product identifier rather than a WhatsApp address, so it is kept
         * as a plain {@link String}.
         */
        private final String productId;

        /**
         * The {@code is_hidden} flag stating whether the product is to be hidden from the catalog.
         */
        private final boolean hidden;

        /**
         * Constructs a product toggle from the product identifier and the hidden flag.
         *
         * @param productId the product identifier whose visibility changes
         * @param hidden    whether the product is to be hidden from the catalog
         */
        public Product(String productId, boolean hidden) {
            this.productId = productId;
            this.hidden = hidden;
        }

        /**
         * Returns the product identifier whose visibility changes.
         *
         * @return the product identifier, may be {@code null}
         */
        public String productId() {
            return productId;
        }

        /**
         * Returns the flag stating whether the product is to be hidden from the catalog.
         *
         * @return {@code true} when the product is to be hidden, {@code false} otherwise
         */
        public boolean hidden() {
            return hidden;
        }

        /**
         * Writes this toggle as a {@code {"product_id": ..., "is_hidden": ...}} object onto the given
         * JSON writer.
         *
         * <p>The {@code product_id} field is written only when it is non-null; the {@code is_hidden}
         * flag is always written.
         *
         * @param writer the JSON writer to append to
         */
        private void write(JSONWriter writer) {
            writer.startObject();
            if (productId != null) {
                writer.writeName("product_id");
                writer.writeColon();
                writer.writeString(productId);
            }

            writer.writeName("is_hidden");
            writer.writeColon();
            writer.writeBool(hidden);
            writer.endObject();
        }
    }
}
