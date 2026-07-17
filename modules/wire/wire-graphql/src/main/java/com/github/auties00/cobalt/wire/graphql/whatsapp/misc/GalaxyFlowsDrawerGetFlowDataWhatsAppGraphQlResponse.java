package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlow;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowBuilder;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowEndpointPublicKey;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowEndpointPublicKeyBuilder;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowMetadata;
import com.github.auties00.cobalt.wire.linked.business.flow.BusinessFlowMetadataBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the flow-metadata query built by
 * {@link GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlRequest} into a {@link BusinessFlowMetadata}.
 *
 * <p>Reads the linked root {@code xwa_extensions_get_flow_data} and flattens its nested
 * {@code extensions_flow_data -> extensions -> metadata} chain and {@code endpoint_public_key} onto the
 * Cobalt domain model: the per-flow metadata and the signed endpoint public key used to verify the
 * flow endpoint before rendering an interactive flow with a business.
 *
 * @see GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebGalaxyFlowsDrawerGetFlowDataQuery")
public final class GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed flow metadata.
     */
    private final BusinessFlowMetadata metadata;

    /**
     * Constructs a response wrapping the parsed flow metadata.
     *
     * <p>Reserved for the static parser.
     *
     * @param metadata the parsed flow metadata, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse(BusinessFlowMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_extensions_get_flow_data} and projects it onto a
     * {@link BusinessFlowMetadata}; the returned {@link Optional} is empty when {@code data} or the
     * flow-data object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the flow-data object is missing
     */
    public static Optional<GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_extensions_get_flow_data");
        if (node == null) {
            return Optional.empty();
        }

        var metadata = new BusinessFlowMetadataBuilder()
                .flows(parseFlows(node.getJSONArray("extensions_flow_data")))
                .endpointPublicKey(parseEndpointPublicKey(node.getJSONObject("endpoint_public_key")))
                .build();
        return Optional.of(new GalaxyFlowsDrawerGetFlowDataWhatsAppGraphQlResponse(metadata));
    }

    /**
     * Projects the {@code extensions_flow_data} array onto a list of {@link BusinessFlow}, flattening
     * the nested {@code extensions -> metadata} chain of each entry.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<BusinessFlow> parseFlows(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessFlow>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            var flowId = obj.getString("flow_id");
            var extensions = obj.getJSONObject("extensions");
            var metadata = extensions == null ? null : extensions.getJSONObject("metadata");
            var builder = new BusinessFlowBuilder().flowId(flowId);
            if (metadata != null) {
                builder.name(metadata.getString("flow_name"))
                        .state(metadata.getString("state"))
                        .categories(metadata.getString("categories"))
                        .creationSource(metadata.getString("creation_source"))
                        .proxySecret(metadata.getString("www_proxy_secret"))
                        .flowTokenSignature(metadata.getString("flow_token_signature"));
            }
            result.add(builder.build());
        }
        return result;
    }

    /**
     * Projects the {@code endpoint_public_key} object onto a {@link BusinessFlowEndpointPublicKey}.
     *
     * @param obj the JSON object to project
     * @return the projected endpoint public key, or {@code null} when {@code obj} is {@code null}
     */
    private static BusinessFlowEndpointPublicKey parseEndpointPublicKey(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        return new BusinessFlowEndpointPublicKeyBuilder()
                .key(obj.getString("key"))
                .signature(obj.getString("signature"))
                .build();
    }

    /**
     * Returns the parsed flow metadata.
     *
     * @return the parsed {@link BusinessFlowMetadata}, never {@code null}
     */
    public BusinessFlowMetadata metadata() {
        return metadata;
    }
}
