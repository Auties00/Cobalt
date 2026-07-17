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

/**
 * Builds the Facebook GraphQL mutation that appeals the rejection of a WhatsApp Business catalog product.
 *
 * <p>The single {@code input} GraphQL variable is the appeal request object. WhatsApp Web's
 * {@code WAWebBizCatalogManagementAppealProduct.appealProduct} fills it with the business account
 * {@code jid}, the rejected {@code product_id}, and the operator-supplied {@code reason} for the
 * appeal. The Meta graph endpoint returns the appeal outcome under {@code xfb_whatsapp_catalog_appeal_product}; the
 * reply is consumed through {@link BizCatalogManagementAppealProductFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementAppealProductFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementAppealProductMutation")
public final class BizCatalogManagementAppealProductFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAppealProductMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29276343172013990";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAppealProductMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementAppealProductMutation";

    /**
     * The {@code jid} field of the {@code input} object naming the business account that owns the
     * appealed product, or {@code null} to omit it.
     */
    private final Jid jid;

    /**
     * The {@code product_id} field of the {@code input} object identifying the rejected product being
     * appealed, or {@code null} to omit it.
     *
     * <p>This is an opaque catalog product identifier rather than a WhatsApp address, so it is kept as
     * a plain {@link String}.
     */
    private final String productId;

    /**
     * The {@code reason} field of the {@code input} object carrying the operator-supplied appeal
     * justification, or {@code null} to omit it.
     */
    private final String reason;

    /**
     * Constructs an appeal-product mutation request carrying the business account, the rejected
     * product id, and the appeal reason.
     *
     * <p>All three values populate the {@code input} GraphQL object; each value that is {@code null}
     * is omitted from the serialized object.
     *
     * @param jid       the business account {@link Jid} that owns the product, or {@code null} to omit
     *                  the field
     * @param productId the rejected product identifier, or {@code null} to omit the field
     * @param reason    the operator-supplied appeal reason, or {@code null} to omit the field
     */
    public BizCatalogManagementAppealProductFacebookGraphQlRequest(Jid jid, String productId, String reason) {
        this.jid = jid;
        this.productId = productId;
        this.reason = reason;
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
     * @implNote This implementation emits {@code {"input": {"jid": <jid>, "product_id": <productId>,
     * "reason": <reason>}}}, writing each field only when its value is non-null and emitting
     * {@code {"input": {}}} when all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAppealProduct", exports = "appealProduct",
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

            if (productId != null) {
                writer.writeName("product_id");
                writer.writeColon();
                writer.writeString(productId);
            }

            if (reason != null) {
                writer.writeName("reason");
                writer.writeColon();
                writer.writeString(reason);
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
