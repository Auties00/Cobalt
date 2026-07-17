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
 * Builds the Facebook GraphQL mutation that creates a business AI assistant rule.
 *
 * <p>The mutation takes a single {@code request} GraphQL variable carried as an {@link AiRuleInput}: a
 * rule type, an optional free-text instruction, and optional emoji-usage and price-sharing
 * configurations. WhatsApp Web's {@code WAWebBizAiRuleCreateMutation.createRule(request)} forwards it to
 * the Meta graph endpoint under the shape {@code {request: <request>}}, which returns the created rule
 * under {@code xfb_meta_ai_biz_agent_wa_create_rule}; the reply is consumed through
 * {@link BizAiRuleCreateFacebookGraphQlResponse}. A creation leaves the input's rule id unset.
 *
 * @see BizAiRuleCreateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiRuleCreateMutation")
public final class BizAiRuleCreateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleCreateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "35226836873596446";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleCreateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiRuleCreateMutation";

    /**
     * The {@code request} GraphQL variable describing the rule to create, or {@code null} to omit it.
     */
    private final AiRuleInput request;

    /**
     * Constructs a create-business-AI-rule mutation request.
     *
     * <p>The {@code request} describes the rule to create. A {@code null} value omits the variable from
     * the serialized object.
     *
     * @param request the rule to create, or {@code null} to omit the variable
     */
    public BizAiRuleCreateFacebookGraphQlRequest(AiRuleInput request) {
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
    @WhatsAppWebExport(moduleName = "WAWebBizAiRuleCreateMutation", exports = "createRule",
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
