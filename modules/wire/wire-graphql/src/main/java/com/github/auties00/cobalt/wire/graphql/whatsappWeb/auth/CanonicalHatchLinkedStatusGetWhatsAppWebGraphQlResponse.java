package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelLinkedStatus;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelLinkedStatusBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the GenAI agent-channel linked-status query built by
 * {@link CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlRequest} into an {@link AiChannelLinkedStatus}.
 *
 * <p>Reads the linked root {@code wa_genai_hatch_channel_metadata} and flattens its nested
 * {@code linked_status} child onto the Cobalt domain model: the channel-presence flag, the lifecycle
 * status token, the pairing flag, and the Facebook-side channel identifier. Absent boolean scalars
 * are treated as {@code false}.
 *
 * @see CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchLinkedStatusGetQuery")
public final class CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed agent-channel linked status.
     */
    private final AiChannelLinkedStatus status;

    /**
     * Constructs a response wrapping the parsed linked status.
     *
     * <p>Reserved for the static parser.
     *
     * @param status the parsed linked status, or {@code null} when the relay omitted the field
     */
    private CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse(AiChannelLinkedStatus status) {
        this.status = status;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code wa_genai_hatch_channel_metadata} and projects its nested
     * {@code linked_status} child onto an {@link AiChannelLinkedStatus}; the returned {@link Optional}
     * is empty when {@code data}, the metadata root, or the {@code linked_status} object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data}, the metadata root, or the
     *         {@code linked_status} object is missing
     */
    public static Optional<CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("wa_genai_hatch_channel_metadata");
        if (root == null) {
            return Optional.empty();
        }

        var node = root.getJSONObject("linked_status");
        if (node == null) {
            return Optional.empty();
        }

        var hasChannel = node.getBoolean("has_channel");
        var paired = node.getBoolean("is_paired");
        var status = new AiChannelLinkedStatusBuilder()
                .hasChannel(hasChannel != null && hasChannel)
                .status(node.getString("status"))
                .paired(paired != null && paired)
                .facebookChannelId(node.getString("channel_fbid"))
                .build();
        return Optional.of(new CanonicalHatchLinkedStatusGetWhatsAppWebGraphQlResponse(status));
    }

    /**
     * Returns the parsed agent-channel linked status.
     *
     * @return the parsed {@link AiChannelLinkedStatus}, never {@code null}
     */
    public AiChannelLinkedStatus status() {
        return status;
    }
}
