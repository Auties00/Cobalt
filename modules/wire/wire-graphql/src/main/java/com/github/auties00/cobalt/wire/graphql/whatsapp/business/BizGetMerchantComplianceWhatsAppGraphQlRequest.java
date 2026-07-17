package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches a WhatsApp Business merchant's legal-compliance information.
 *
 * <p>The single {@code request} GraphQL variable is the
 * {@code XFBWhatsAppBizMerchantGetComplianceInfoRequest} input object. WhatsApp Web's
 * {@code WAWebMerchantComplianceJob} fills it with a single {@code biz_jid} field naming the business
 * account whose compliance record is being read. The graph.whatsapp.com endpoint returns the record under
 * {@code xfb_whatsapp_biz_merchant_compliance_info}; the reply is consumed through
 * {@link BizGetMerchantComplianceWhatsAppGraphQlResponse}.
 *
 * @see BizGetMerchantComplianceWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetMerchantComplianceQuery")
public final class BizGetMerchantComplianceWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetMerchantComplianceQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25960403573553316";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizGetMerchantComplianceQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizGetMerchantComplianceQuery";

    /**
     * The {@code biz_jid} field of the {@code request} object naming the business account whose
     * compliance record is being read, or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * Constructs a merchant-compliance query request targeting the given business account.
     *
     * <p>The {@code bizJid} populates the {@code biz_jid} field of the {@code request} GraphQL object;
     * a {@code null} value omits the field.
     *
     * @param bizJid the business account {@link Jid} whose compliance record is being read, or
     *               {@code null} to omit the field
     */
    public BizGetMerchantComplianceWhatsAppGraphQlRequest(Jid bizJid) {
        this.bizJid = bizJid;
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
     * @implNote This implementation emits {@code {"request": {"biz_jid": <bizJid>}}}, writing the
     * {@code biz_jid} field only when {@link #bizJid} is non-null and emitting {@code {"request": {}}}
     * otherwise.
     */
    @WhatsAppWebExport(moduleName = "WAWebMerchantComplianceJob", exports = "getMerchantCompliance",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
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
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
