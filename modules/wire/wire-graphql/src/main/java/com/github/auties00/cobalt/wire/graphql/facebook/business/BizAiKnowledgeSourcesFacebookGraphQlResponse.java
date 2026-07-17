package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHome;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiAgentHomeBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeSource;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiKnowledgeSourceBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the list-knowledge-sources query built by
 * {@link BizAiKnowledgeSourcesFacebookGraphQlRequest} into a {@link BusinessAiAgentHome}.
 *
 * <p>Projects the linked {@code xfb_meta_ai_biz_agent_wa_ai_home} field onto the learning-source
 * facets of the home view: the configured knowledge sources (uploaded files, chat history, and
 * websites) and the chat-history export progress marker. The remaining facets are left empty because
 * this query does not request them.
 *
 * @see BizAiKnowledgeSourcesFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourcesQuery")
public final class BizAiKnowledgeSourcesFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected agent-home view, or {@code null} when the Meta graph endpoint omitted the
     * field.
     */
    private final BusinessAiAgentHome agentHome;

    /**
     * Constructs a response wrapping the projected agent-home view.
     *
     * <p>Reserved for the static parser.
     *
     * @param agentHome the projected agent-home view, or {@code null} when the Meta graph endpoint
     *                  omitted the field
     */
    private BizAiKnowledgeSourcesFacebookGraphQlResponse(BusinessAiAgentHome agentHome) {
        this.agentHome = agentHome;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects the
     * learning-source facets onto a {@link BusinessAiAgentHome}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiKnowledgeSourcesFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var home = data.getJSONObject("xfb_meta_ai_biz_agent_wa_ai_home");
        if (home == null) {
            return Optional.of(new BizAiKnowledgeSourcesFacebookGraphQlResponse(null));
        }

        var knowledgeSources = parseKnowledgeSources(home.getJSONArray("knowledge_sources"));
        var agentHome = new BusinessAiAgentHomeBuilder()
                .knowledgeSources(knowledgeSources)
                .chatHistoryExportStatus(home.getString("chat_history_export_status"))
                .build();
        return Optional.of(new BizAiKnowledgeSourcesFacebookGraphQlResponse(agentHome));
    }

    /**
     * Projects the {@code knowledge_sources} union array onto a list of {@link BusinessAiKnowledgeSource}
     * values, flattening the file-upload, chat-history, and website members onto one shape.
     *
     * @param arr the {@code knowledge_sources} array, possibly {@code null}
     * @return the projected sources, never {@code null}
     */
    private static List<BusinessAiKnowledgeSource> parseKnowledgeSources(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiKnowledgeSource>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            var updateTime = obj.getLong("update_time");
            var creationTime = obj.getLong("creation_time");
            result.add(new BusinessAiKnowledgeSourceBuilder()
                    .sourceKind(obj.getString("source_type"))
                    .id(obj.getString("id"))
                    .label(obj.getString("label"))
                    .lastUpdated(updateTime != null ? Instant.ofEpochSecond(updateTime) : null)
                    .created(creationTime != null ? Instant.ofEpochSecond(creationTime) : null)
                    .fileName(obj.getString("user_provided_file_name"))
                    .downloadUrl(obj.getString("cdn_url"))
                    .thumbnailUrl(obj.getString("thumbnail_url"))
                    .fileType(obj.getString("file_type"))
                    .mimeType(obj.getString("mime_type"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the projected agent-home view carrying the configured knowledge sources.
     *
     * @return the projected {@link BusinessAiAgentHome}, or empty when the Meta graph endpoint
     *         omitted the field
     */
    public Optional<BusinessAiAgentHome> agentHome() {
        return Optional.ofNullable(agentHome);
    }
}
