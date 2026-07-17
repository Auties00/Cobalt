package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.AiRuleInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that updates a single WhatsApp Business AI agent rule.
 *
 * <p>The single {@code request} GraphQL variable carries the rule to apply as an {@link AiRuleInput}: a
 * rule type, an optional free-text instruction, optional emoji-usage and price-sharing configurations,
 * and the rule id being updated. WhatsApp Web's {@code WAWebBizAiRuleUpdateMutation.updateRule(request)}
 * forwards it to {@code WAWebRelayClient.commitMutation}; the Meta graph endpoint returns the update
 * outcome under {@code xfb_meta_ai_biz_agent_wa_update_rule}; the reply is consumed through
 * {@link BizAiRuleUpdateFacebookGraphQlResponse}. An update carries the input's rule id.
 *
 * @see BizAiRuleUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiRuleUpdateMutation")
public final class BizAiRuleUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26525955470399173";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiRuleUpdateMutation";

    /**
     * The {@code request} GraphQL variable describing the rule to update, or {@code null} to omit it.
     */
    private final AiRuleInput request;

    /**
     * Constructs an update-business-AI-rule mutation request.
     *
     * <p>The {@code request} describes the rule to update and carries the rule id being updated. A
     * {@code null} value omits the variable.
     *
     * @param request the rule to update, or {@code null} to omit the variable
     */
    public BizAiRuleUpdateFacebookGraphQlRequest(AiRuleInput request) {
        this.request = request;
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
     * @implNote This implementation emits {@code {"request": {...}}} with the rule fields under the
     * snake_case keys {@code custom_rule}, {@code rule_type}, {@code emojis_config}
     * ({@code {emojis_freq}}), {@code price_config} ({@code {price_sharing}}), and {@code rule_id},
     * writing the variable only when the request is non-null and emitting {@code "{}"} when it is
     * {@code null}. The camelCase-to-snake_case mapping is performed by
     * {@link BizAiInputJson#writeRuleInput(JSONWriter, AiRuleInput)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleUpdateMutation", exports = "updateRule",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (request != null) {
                writer.writeName("request");
                writer.writeColon();
                BizAiInputJson.writeRuleInput(writer, request);
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
