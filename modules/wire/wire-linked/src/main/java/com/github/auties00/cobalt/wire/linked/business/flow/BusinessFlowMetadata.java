package com.github.auties00.cobalt.wire.linked.business.flow;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The metadata needed to render and drive a business's WhatsApp Flows.
 *
 * <p>WhatsApp Flows are interactive forms a business can present inside a chat.
 * Before rendering one the client fetches the metadata of the flows the
 * business hosts together with the signed public key it uses to verify the
 * business's flow endpoint. This model is that fetched metadata: the list of
 * {@linkplain BusinessFlow flows} and the
 * {@linkplain BusinessFlowEndpointPublicKey endpoint public key}.
 */
@ProtobufMessage(name = "BusinessFlowMetadata")
public final class BusinessFlowMetadata {
    /**
     * Flows the business hosts, in the order the server returned them. Never
     * {@code null}, possibly empty when the server returned none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<BusinessFlow> flows;

    /**
     * Signed public key used to verify the business's flow endpoint, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final BusinessFlowEndpointPublicKey endpointPublicKey;

    /**
     * Constructs a new {@code BusinessFlowMetadata}. A {@code null}
     * {@code flows} is coerced to an empty list, and {@code endpointPublicKey}
     * may be {@code null} when the server omitted it.
     *
     * @param flows             the flows; {@code null} treated as empty
     * @param endpointPublicKey the endpoint public key, or {@code null}
     */
    BusinessFlowMetadata(List<BusinessFlow> flows, BusinessFlowEndpointPublicKey endpointPublicKey) {
        this.flows = flows == null ? List.of() : flows;
        this.endpointPublicKey = endpointPublicKey;
    }

    /**
     * Returns the flows the business hosts.
     *
     * @return an unmodifiable view of the flows; never {@code null}, possibly
     *         empty
     */
    public List<BusinessFlow> flows() {
        return Collections.unmodifiableList(flows);
    }

    /**
     * Returns the signed public key used to verify the business's flow
     * endpoint.
     *
     * @return the endpoint public key, or empty when the server omitted it
     */
    public Optional<BusinessFlowEndpointPublicKey> endpointPublicKey() {
        return Optional.ofNullable(endpointPublicKey);
    }
}
