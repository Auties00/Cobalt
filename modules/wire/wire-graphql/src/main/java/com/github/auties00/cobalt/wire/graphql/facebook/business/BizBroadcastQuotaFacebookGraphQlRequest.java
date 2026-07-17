package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessBroadcastQuotaData;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that fetches the SMB marketing-message quota for a WhatsApp Business
 * broadcast.
 *
 * <p>The query takes a single {@code data} GraphQL variable describing the quota lookup as a
 * {@link BusinessBroadcastQuotaData}: a terms-of-service acknowledgement carried under
 * {@code tos.did_accept}. The Meta graph endpoint returns the quota under {@code xwa_smb_mm_quota}; the
 * reply is consumed through {@link BizBroadcastQuotaFacebookGraphQlResponse}.
 *
 * @see BizBroadcastQuotaFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizBroadcastQuotaQuery")
public final class BizBroadcastQuotaFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastQuotaQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26388379850831833";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizBroadcastQuotaQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizBroadcastQuotaQuery";

    /**
     * The {@code data} GraphQL object describing the quota lookup, or {@code null} to omit it.
     */
    private final BusinessBroadcastQuotaData data;

    /**
     * Constructs a broadcast-quota query request.
     *
     * <p>The {@code data} describes the quota lookup. A {@code null} value omits the variable from the
     * serialized object.
     *
     * @param data the quota-lookup data, or {@code null} to omit the variable
     */
    public BizBroadcastQuotaFacebookGraphQlRequest(BusinessBroadcastQuotaData data) {
        this.data = data;
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
     * @implNote This implementation emits {@code {"data": {"tos": {"did_accept": <didAccept>}}}},
     * writing the {@code data} object only when it is non-null and emitting {@code "{}"} otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (data != null) {
                writer.writeName("data");
                writer.writeColon();
                writer.startObject();
                writer.writeName("tos");
                writer.writeColon();
                writer.startObject();
                writer.writeName("did_accept");
                writer.writeColon();
                writer.writeBool(data.didAccept());
                writer.endObject();
                writer.endObject();
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
