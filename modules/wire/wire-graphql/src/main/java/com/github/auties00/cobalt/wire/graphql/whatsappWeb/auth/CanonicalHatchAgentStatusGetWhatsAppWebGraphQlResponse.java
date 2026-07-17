package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelAgentStatus;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelAgentStatusBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the GenAI agent-channel activity-status query built by
 * {@link CanonicalHatchAgentStatusGetWhatsAppWebGraphQlRequest} into an {@link AiChannelAgentStatus}.
 *
 * <p>Reads the linked root {@code wa_genai_hatch_channel_metadata} and flattens its nested
 * {@code agent_status} child onto the Cobalt domain model: the machine-readable activity code and the
 * human-readable activity text shown next to the agent's name.
 *
 * @see CanonicalHatchAgentStatusGetWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchAgentStatusGetQuery")
public final class CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed agent-channel activity status.
     */
    private final AiChannelAgentStatus status;

    /**
     * Constructs a response wrapping the parsed activity status.
     *
     * <p>Reserved for the static parser.
     *
     * @param status the parsed activity status, or {@code null} when the relay omitted the field
     */
    private CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse(AiChannelAgentStatus status) {
        this.status = status;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code wa_genai_hatch_channel_metadata} and projects its nested
     * {@code agent_status} child onto an {@link AiChannelAgentStatus}; the returned {@link Optional}
     * is empty when {@code data}, the metadata root, or the {@code agent_status} object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the metadata root, or the
     *         {@code agent_status} object is missing
     */
    public static Optional<CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("wa_genai_hatch_channel_metadata");
        if (root == null) {
            return Optional.empty();
        }

        var node = root.getJSONObject("agent_status");
        if (node == null) {
            return Optional.empty();
        }

        var status = new AiChannelAgentStatusBuilder()
                .activityCode(node.getString("activity_code"))
                .activityText(node.getString("activity_text"))
                .build();
        return Optional.of(new CanonicalHatchAgentStatusGetWhatsAppWebGraphQlResponse(status));
    }

    /**
     * Returns the parsed agent-channel activity status.
     *
     * @return the parsed {@link AiChannelAgentStatus}, never {@code null}
     */
    public AiChannelAgentStatus status() {
        return status;
    }
}
