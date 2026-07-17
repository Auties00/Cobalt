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
 * Builds the Facebook GraphQL mutation that deletes one or more WhatsApp Business AI agent product-info
 * knowledge entries.
 *
 * <p>The mutation takes a single {@code product_ids} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiProductInfoMutation.deleteProductInfo(ids)} normalises the caller's argument with
 * {@code [].concat(ids)} and forwards it to the Meta graph endpoint as {@code {product_ids: [...]}}; the ids are
 * product-catalog identifiers rather than WhatsApp addresses, so they are modelled as a
 * {@link String} list. The Meta graph endpoint returns one deletion outcome per requested id under the plural
 * linked field {@code xfb_maiba_multi_delete_product_info_knowledge}; the reply is consumed through
 * {@link BizAiProductInfoMutationDeleteFacebookGraphQlResponse}.
 *
 * @see BizAiProductInfoMutationDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiProductInfoMutationDeleteMutation")
public final class BizAiProductInfoMutationDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutationDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35049091691406061";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutationDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiProductInfoMutationDeleteMutation";

    /**
     * The {@code product_ids} GraphQL variable listing the product-catalog ids to delete, or
     * {@code null} to omit it.
     */
    private final List<String> productIds;

    /**
     * Constructs a delete-product-info mutation request carrying the product ids to delete.
     *
     * <p>The ids are product-catalog identifiers, not WhatsApp addresses. A {@code null} value omits
     * the {@code product_ids} variable from the serialized object; an empty list serializes as an
     * empty array.
     *
     * @param productIds the product-catalog ids to delete, or {@code null} to omit the variable
     */
    public BizAiProductInfoMutationDeleteFacebookGraphQlRequest(List<String> productIds) {
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
     * @implNote This implementation emits {@code {"product_ids": [...]}}, writing the
     * {@code product_ids} array only when the list is non-null and emitting {@code "{}"} when it is
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiProductInfoMutation", exports = "deleteProductInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
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
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
