package com.github.auties00.cobalt.wire.linked.business.flow;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The signed public key used to verify a WhatsApp Flow endpoint.
 *
 * <p>A WhatsApp Flow is an interactive form a business can present inside a
 * chat; the form's data is exchanged with a business-hosted endpoint over an
 * encrypted channel. Before opening that channel the client verifies the
 * endpoint's authenticity using a public {@linkplain #key() key} the server
 * vouches for with a {@linkplain #signature() signature}.
 *
 * <p>This model is that signed endpoint public key as the server reports it.
 */
@ProtobufMessage(name = "BusinessFlowEndpointPublicKey")
public final class BusinessFlowEndpointPublicKey {
    /**
     * Public key of the flow endpoint. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String key;

    /**
     * Signature vouching for the endpoint public key. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String signature;

    /**
     * Constructs a new {@code BusinessFlowEndpointPublicKey}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param key       the endpoint public key, or {@code null}
     * @param signature the key signature, or {@code null}
     */
    BusinessFlowEndpointPublicKey(String key, String signature) {
        this.key = key;
        this.signature = signature;
    }

    /**
     * Returns the public key of the flow endpoint.
     *
     * @return the endpoint public key, or empty when the server omitted it
     */
    public Optional<String> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the signature vouching for the endpoint public key.
     *
     * @return the key signature, or empty when the server omitted it
     */
    public Optional<String> signature() {
        return Optional.ofNullable(signature);
    }
}
