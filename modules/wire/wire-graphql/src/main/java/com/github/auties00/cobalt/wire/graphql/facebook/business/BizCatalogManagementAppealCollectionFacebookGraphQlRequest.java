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
 * Builds the Facebook GraphQL mutation that appeals a rejected WhatsApp Business catalog collection.
 *
 * <p>The single {@code input} GraphQL variable carries the appeal request. WhatsApp Web's
 * {@code WAWebProductCollectionsJob.appealCollection} fills it with the {@code product_set_id} naming
 * the rejected collection, the {@code jid} of the business owner, and the {@code reason} the merchant
 * supplied. The Meta graph endpoint returns the appeal outcome under {@code xfb_whatsapp_catalog_appeal_collection};
 * the reply is consumed through {@link BizCatalogManagementAppealCollectionFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementAppealCollectionFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementAppealCollectionMutation")
public final class BizCatalogManagementAppealCollectionFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAppealCollectionMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9971242039605207";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementAppealCollectionMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementAppealCollectionMutation";

    /**
     * The {@code product_set_id} field of the {@code input} object naming the rejected collection, or
     * {@code null} to omit it.
     *
     * <p>Kept as a {@link String}: it is a catalog product-set identifier, not a WhatsApp address.
     */
    private final String productSetId;

    /**
     * The {@code jid} field of the {@code input} object naming the business owner, or {@code null} to
     * omit it.
     */
    private final Jid jid;

    /**
     * The {@code reason} field of the {@code input} object holding the merchant's appeal reason, or
     * {@code null} to omit it.
     */
    private final String reason;

    /**
     * Constructs an appeal-collection mutation request.
     *
     * <p>The {@code productSetId} names the rejected collection, the {@code jid} names the business
     * owner, and {@code reason} carries the merchant's appeal reason. Each value that is {@code null}
     * omits its field from the serialized object.
     *
     * @param productSetId the rejected collection's product-set id, or {@code null} to omit the field
     * @param jid          the business owner {@link Jid}, or {@code null} to omit the field
     * @param reason       the appeal reason, or {@code null} to omit the field
     */
    public BizCatalogManagementAppealCollectionFacebookGraphQlRequest(String productSetId, Jid jid, String reason) {
        this.productSetId = productSetId;
        this.jid = jid;
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
     * @implNote This implementation emits {@code {"input": {"product_set_id": <productSetId>, "jid":
     * <jid>, "reason": <reason>}}}, writing each field only when its value is non-null and emitting
     * {@code {"input": {}}} when all are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebProductCollectionsJob", exports = "appealCollection",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (productSetId != null) {
                writer.writeName("product_set_id");
                writer.writeColon();
                writer.writeString(productSetId);
            }

            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
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
