package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the comet mutation that submits the caller's ads-integrity self-certification.
 *
 * <p>The mutation takes a single {@code input} GraphQL object carrying the self-certification
 * payload, whose {@code source} field names the surface the certification was made from. The relay
 * returns the certification outcome under {@code ads_integrity_self_certification_certify}, whose
 * {@code certified_user_name} and {@code create_time} scalars record who certified and when; the reply
 * is consumed through {@link BizAdCertifyFacebookGraphQlResponse}.
 *
 * @see BizAdCertifyFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCertifyMutation")
public final class BizAdCertifyFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCertifyMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25340988522238060";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCertifyMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCertifyMutation";

    /**
     * The {@code source} field of the {@code input} object naming the surface the certification was
     * made from, or {@code null} to omit it.
     */
    private final String source;

    /**
     * Constructs an ads-integrity self-certification mutation request.
     *
     * <p>The {@code source} names the surface the certification was made from. A {@code null} value
     * omits the field from the serialized {@code input} object.
     *
     * @param source the certification source surface, or {@code null} to omit the field
     */
    public BizAdCertifyFacebookGraphQlRequest(String source) {
        this.source = source;
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
     * @implNote This implementation emits {@code {"input": {"source": <source>}}}, writing
     * {@code source} only when it is non-null and emitting {@code {"input": {}}} otherwise.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (source != null) {
                writer.writeName("source");
                writer.writeColon();
                writer.writeString(source);
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
