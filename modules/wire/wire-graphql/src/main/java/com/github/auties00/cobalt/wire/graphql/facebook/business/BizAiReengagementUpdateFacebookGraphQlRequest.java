package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that updates a WhatsApp Business AI agent's re-engagement settings (the
 * automatic follow-up messages the agent sends to re-engage a contact).
 *
 * <p>The mutation takes a single {@code request} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiReengagementUpdateMutation.updateReengagement(settings)} builds it as
 * {@code {request: {enabled, amount}}}, where {@code enabled} toggles the feature and {@code amount}
 * is the configured re-engagement amount. The Meta graph endpoint echoes the persisted settings back under the
 * linked field {@code xfb_meta_ai_biz_agent_wa_update_reengagement}; the reply is consumed through
 * {@link BizAiReengagementUpdateFacebookGraphQlResponse}.
 *
 * <p>The {@code amount} field is a numeric scalar whose exact type is not confirmed from the bundle;
 * it is modelled as a {@code long}, matching the integer-count semantics of a re-engagement message
 * budget.
 *
 * @see BizAiReengagementUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiReengagementUpdateMutation")
public final class BizAiReengagementUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReengagementUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27136574649299744";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReengagementUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiReengagementUpdateMutation";

    /**
     * The {@code enabled} field of the {@code request} object toggling re-engagement, or {@code null}
     * to omit it.
     */
    private final Boolean enabled;

    /**
     * The {@code amount} field of the {@code request} object holding the re-engagement amount, or
     * {@code null} to omit it.
     */
    private final Long amount;

    /**
     * Constructs a re-engagement-update mutation request.
     *
     * <p>The {@code enabled} flag and {@code amount} populate the {@code request} object. Each value
     * that is {@code null} omits its field from the serialized object.
     *
     * @param enabled whether re-engagement is enabled, or {@code null} to omit the field
     * @param amount  the configured re-engagement amount, or {@code null} to omit the field
     */
    public BizAiReengagementUpdateFacebookGraphQlRequest(Boolean enabled, Long amount) {
        this.enabled = enabled;
        this.amount = amount;
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
     * @implNote This implementation emits {@code {"request": {"enabled": <enabled>, "amount":
     * <amount>}}}, writing each inner field only when its value is non-null and emitting
     * {@code {"request": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiReengagementUpdateMutation", exports = "updateReengagement",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (enabled != null) {
                writer.writeName("enabled");
                writer.writeColon();
                writer.writeBool(enabled);
            }

            if (amount != null) {
                writer.writeName("amount");
                writer.writeColon();
                writer.writeInt64(amount);
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
