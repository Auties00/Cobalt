package com.github.auties00.cobalt.wire.linked.business.flow;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Metadata describing a single WhatsApp Flow hosted by a business.
 *
 * <p>A WhatsApp Flow is an interactive form a business can present inside a
 * chat. Before rendering it the client fetches the flow's metadata: its
 * {@linkplain #flowId() identifier}, its {@linkplain #name() name}, its current
 * {@linkplain #state() state}, the {@linkplain #categories() categories} it
 * belongs to, the {@linkplain #creationSource() surface} it was created from,
 * and two secrets the client needs to drive the flow exchange, the
 * {@linkplain #proxySecret() proxy secret} and the
 * {@linkplain #flowTokenSignature() flow token signature}.
 *
 * <p>This model is one such flow's metadata as the server reports it. The
 * state is exposed as a raw marker because its value set is server-defined.
 */
@ProtobufMessage(name = "BusinessFlow")
public final class BusinessFlow {
    /**
     * Identifier of the flow. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String flowId;

    /**
     * Name of the flow. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Current state of the flow, as a server-defined marker. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String state;

    /**
     * Categories the flow belongs to, as a server-defined marker. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String categories;

    /**
     * Surface the flow was created from, as a server-defined marker.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String creationSource;

    /**
     * Proxy secret the client uses to drive the flow exchange. {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String proxySecret;

    /**
     * Signature of the flow token the client presents during the flow
     * exchange. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String flowTokenSignature;

    /**
     * Constructs a new {@code BusinessFlow}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param flowId             the flow identifier, or {@code null}
     * @param name               the flow name, or {@code null}
     * @param state              the flow state marker, or {@code null}
     * @param categories         the flow categories marker, or {@code null}
     * @param creationSource     the creation-source marker, or {@code null}
     * @param proxySecret        the proxy secret, or {@code null}
     * @param flowTokenSignature the flow token signature, or {@code null}
     */
    BusinessFlow(String flowId, String name, String state, String categories, String creationSource,
                 String proxySecret, String flowTokenSignature) {
        this.flowId = flowId;
        this.name = name;
        this.state = state;
        this.categories = categories;
        this.creationSource = creationSource;
        this.proxySecret = proxySecret;
        this.flowTokenSignature = flowTokenSignature;
    }

    /**
     * Returns the identifier of the flow.
     *
     * @return the flow identifier, or empty when the server omitted it
     */
    public Optional<String> flowId() {
        return Optional.ofNullable(flowId);
    }

    /**
     * Returns the name of the flow.
     *
     * @return the flow name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the current state of the flow.
     *
     * @return the flow state marker, or empty when the server omitted it
     */
    public Optional<String> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns the categories the flow belongs to.
     *
     * @return the flow categories marker, or empty when the server omitted it
     */
    public Optional<String> categories() {
        return Optional.ofNullable(categories);
    }

    /**
     * Returns the surface the flow was created from.
     *
     * @return the creation-source marker, or empty when the server omitted it
     */
    public Optional<String> creationSource() {
        return Optional.ofNullable(creationSource);
    }

    /**
     * Returns the proxy secret the client uses to drive the flow exchange.
     *
     * @return the proxy secret, or empty when the server omitted it
     */
    public Optional<String> proxySecret() {
        return Optional.ofNullable(proxySecret);
    }

    /**
     * Returns the signature of the flow token the client presents during the
     * flow exchange.
     *
     * @return the flow token signature, or empty when the server omitted it
     */
    public Optional<String> flowTokenSignature() {
        return Optional.ofNullable(flowTokenSignature);
    }
}
