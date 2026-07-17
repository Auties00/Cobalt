package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Builds the Facebook GraphQL mutation that creates or onboards a WhatsApp Ads (WAA) ad account for a
 * click-to-WhatsApp advertising flow.
 *
 * <p>The single {@code input} GraphQL variable is the {@code WaAdAccountOnboardingInput} object.
 * WhatsApp Web's {@code WAWebWAAOnboarding} fills it with a {@code flow_id} naming the advertising
 * flow being onboarded and a {@code request_id} de-duplication timestamp (serialized as
 * milliseconds-since-epoch). The Meta graph endpoint returns the newly created or onboarded ad account under
 * {@code create_or_onboard_wa_ad_account}; the reply is consumed through
 * {@link WaaOnboardingFacebookGraphQlResponse}.
 *
 * @see WaaOnboardingFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebWAAOnboardingMutation")
public final class WaaOnboardingFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebWAAOnboardingMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25173295938976172";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebWAAOnboardingMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebWAAOnboardingMutation";

    /**
     * The {@code flow_id} field of the {@code input} object naming the advertising flow being
     * onboarded, or {@code null} to omit it.
     */
    private final String flowId;

    /**
     * The {@code request_id} field of the {@code input} object de-duplicating the onboarding request,
     * or {@code null} to omit it.
     *
     * <p>WhatsApp Web sets this to the current instant; it is serialized as milliseconds-since-epoch.
     */
    private final Instant requestId;

    /**
     * Constructs a WAA-onboarding request carrying the advertising flow id and the de-duplication
     * token.
     *
     * <p>Both values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param flowId    the advertising flow being onboarded, or {@code null} to omit the field
     * @param requestId the de-duplication timestamp for this request, or {@code null} to omit the
     *                  field
     */
    public WaaOnboardingFacebookGraphQlRequest(String flowId, Instant requestId) {
        this.flowId = flowId;
        this.requestId = requestId;
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
     * @implNote This implementation emits {@code {"input": {"flow_id": <flowId>, "request_id":
     * <requestId>}}}, writing each field only when its value is non-null, rendering {@code requestId}
     * as its milliseconds-since-epoch string, and emitting {@code {"input": {}}} when both are
     * {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebWAAOnboarding", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (flowId != null) {
                writer.writeName("flow_id");
                writer.writeColon();
                writer.writeString(flowId);
            }

            if (requestId != null) {
                writer.writeName("request_id");
                writer.writeColon();
                writer.writeString(String.valueOf(requestId.toEpochMilli()));
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
