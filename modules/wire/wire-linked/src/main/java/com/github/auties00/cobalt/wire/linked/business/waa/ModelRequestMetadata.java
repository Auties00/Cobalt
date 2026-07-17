package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One entry of a native machine-learning model request naming the model and version to resolve.
 *
 * <p>A native-model manifest query resolves a batch of on-device models into the files the client must
 * download. Each requested model is named by this metadata: the model {@link #name() name} and the
 * {@link #version() version} the client wants resolved.
 */
@ProtobufMessage(name = "ModelRequestMetadata")
public final class ModelRequestMetadata {
    /**
     * Name of the model to resolve. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Version of the model to resolve. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String version;

    /**
     * Constructs a new {@code ModelRequestMetadata}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param name    the model name, or {@code null}
     * @param version the model version, or {@code null}
     */
    ModelRequestMetadata(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * Returns the name of the model to resolve.
     *
     * @return an {@link Optional} carrying the model name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the version of the model to resolve.
     *
     * @return an {@link Optional} carrying the model version, or empty when unset
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }
}
