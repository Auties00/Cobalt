package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResult;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiMutationResultBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the file-knowledge-extraction trigger mutation built by
 * {@link BizAiKnowledgeSourceUploadFacebookGraphQlRequest} into a {@link BusinessAiMutationResult}.
 *
 * <p>Projects the linked {@code xfb_maiba_trigger_file_knowledge_extraction} field onto the shared
 * mutation-result shape. The {@code success} flag becomes {@link BusinessAiMutationResult#success()};
 * the id of the created uploaded-file knowledge source, when present, is the sole entry of
 * {@link BusinessAiMutationResult#affectedIds()}; and the extraction error string, when present,
 * becomes {@link BusinessAiMutationResult#errorMessage()}.
 *
 * @see BizAiKnowledgeSourceUploadFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourceUploadMutation")
public final class BizAiKnowledgeSourceUploadFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected mutation result, or {@code null} when the Meta graph endpoint omitted the
     * field.
     */
    private final BusinessAiMutationResult result;

    /**
     * Constructs a response wrapping the projected mutation result.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the projected mutation result, or {@code null} when the Meta graph endpoint
     *               omitted the field
     */
    private BizAiKnowledgeSourceUploadFacebookGraphQlResponse(BusinessAiMutationResult result) {
        this.result = result;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * extraction outcome onto a {@link BusinessAiMutationResult}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiKnowledgeSourceUploadFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xfb_maiba_trigger_file_knowledge_extraction");
        if (node == null) {
            return Optional.of(new BizAiKnowledgeSourceUploadFacebookGraphQlResponse(null));
        }

        var dataSourceId = node.getString("uploaded_file_data_source_id");
        var result = new BusinessAiMutationResultBuilder()
                .success(Boolean.TRUE.equals(node.getBoolean("success")))
                .affectedIds(dataSourceId != null ? List.of(dataSourceId) : List.of())
                .errorMessage(node.getString("error"))
                .build();
        return Optional.of(new BizAiKnowledgeSourceUploadFacebookGraphQlResponse(result));
    }

    /**
     * Returns the projected mutation result.
     *
     * @return the projected {@link BusinessAiMutationResult}, or empty when the Meta graph endpoint
     *         omitted the field
     */
    public Optional<BusinessAiMutationResult> result() {
        return Optional.ofNullable(result);
    }
}
