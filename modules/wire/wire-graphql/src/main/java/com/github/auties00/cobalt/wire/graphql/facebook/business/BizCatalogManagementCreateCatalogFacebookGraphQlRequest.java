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
 * Builds the Facebook GraphQL mutation that creates a WhatsApp Business product catalog.
 *
 * <p>The single {@code input} GraphQL variable carries a nested {@code product_catalog} object naming
 * the business account that owns the new catalog and a {@code platform} string identifying the
 * surface requesting the creation. WhatsApp Web's
 * {@code WAWebBizCreateProductCatalogJob.createProductCatalogGraphQL} fills the input with
 * {@code {product_catalog: {biz_jid: <meUser>}, platform: "WEB"}}. The Meta graph endpoint returns the creation
 * outcome under {@code xfb_whatsapp_catalog_create}; the reply is consumed through
 * {@link BizCatalogManagementCreateCatalogFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementCreateCatalogFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementCreateCatalogMutation")
public final class BizCatalogManagementCreateCatalogFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementCreateCatalogMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "29232780583035464";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementCreateCatalogMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementCreateCatalogMutation";

    /**
     * The {@code product_catalog.biz_jid} field of the {@code input} object naming the business
     * account that owns the new catalog, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code platform} field of the {@code input} object identifying the requesting surface, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web sends the literal {@code "WEB"}; the full server-side value set is not
     * observable from the bundle, so this is kept as a plain {@link String}.
     */
    private final String platform;

    /**
     * Constructs a create-catalog mutation request carrying the owning business account and the
     * requesting platform.
     *
     * <p>The {@code bizJid} populates the nested {@code product_catalog.biz_jid} field and the
     * {@code platform} populates the top-level {@code input.platform} field; each value that is
     * {@code null} is omitted from the serialized object.
     *
     * @param bizJid   the business account {@link Jid} that owns the new catalog, or {@code null} to
     *                 omit the field
     * @param platform the requesting surface identifier (WhatsApp Web sends {@code "WEB"}), or
     *                 {@code null} to omit the field
     */
    public BizCatalogManagementCreateCatalogFacebookGraphQlRequest(Jid bizJid, String platform) {
        this.bizJid = bizJid;
        this.platform = platform;
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
     * @implNote This implementation emits {@code {"input": {"product_catalog": {"biz_jid": <bizJid>},
     * "platform": <platform>}}}, writing the nested {@code product_catalog} object only when
     * {@code bizJid} is non-null and the {@code platform} field only when it is non-null, and emitting
     * {@code {"input": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCreateProductCatalogJob", exports = "createProductCatalogGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("product_catalog");
                writer.writeColon();
                writer.startObject();
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
                writer.endObject();
            }

            if (platform != null) {
                writer.writeName("platform");
                writer.writeColon();
                writer.writeString(platform);
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
