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
 * Builds the Facebook GraphQL mutation that deletes a business AI assistant rule.
 *
 * <p>The mutation takes a single {@code request} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiRuleDeleteMutation.deleteRule(ruleId)} wraps the caller's rule identifier into the
 * shape {@code {request: {rule_id: ruleId}}} before handing it to the Meta graph endpoint, so the only modelled
 * field is the {@code rule_id} naming the rule to delete. The Meta graph endpoint returns the deletion outcome
 * under {@code xfb_meta_ai_biz_agent_wa_delete_rule}; the reply is consumed through
 * {@link BizAiRuleDeleteFacebookGraphQlResponse}.
 *
 * @see BizAiRuleDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiRuleDeleteMutation")
public final class BizAiRuleDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35650188977905810";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiRuleDeleteMutation";

    /**
     * The {@code rule_id} field of the {@code request} object naming the rule to delete, or
     * {@code null} to omit it.
     *
     * <p>The value is a server-assigned rule identifier rather than a WhatsApp address, so it is kept
     * as a {@link String}.
     */
    private final String ruleId;

    /**
     * Constructs a delete-business-AI-rule mutation request.
     *
     * <p>The {@code ruleId} populates the {@code rule_id} field of the {@code request} GraphQL object;
     * a {@code null} value omits the field from the serialized object.
     *
     * @param ruleId the server-assigned identifier of the rule to delete, or {@code null} to omit the
     *               field
     */
    public BizAiRuleDeleteFacebookGraphQlRequest(String ruleId) {
        this.ruleId = ruleId;
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
     * @implNote This implementation emits {@code {"request": {"rule_id": <ruleId>}}}, writing the
     * {@code rule_id} field only when its value is non-null and emitting {@code {"request": {}}} when
     * it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleDeleteMutation", exports = "deleteRule",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (ruleId != null) {
                writer.writeName("rule_id");
                writer.writeColon();
                writer.writeString(ruleId);
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
