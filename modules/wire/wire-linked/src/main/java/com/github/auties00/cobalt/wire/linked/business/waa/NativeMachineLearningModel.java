package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One on-device machine-learning model resolved by a
 * {@link NativeMachineLearningModelManifest}.
 *
 * <p>A model is identified by its name and version and carries the list of
 * downloadable assets the client must fetch to run it, together with a list of
 * free-form key-value properties the server attaches.
 */
@ProtobufMessage(name = "NativeMachineLearningModel")
public final class NativeMachineLearningModel {
    /**
     * Model name. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Model version marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String version;

    /**
     * Downloadable assets for the model, in the order the server returned them.
     * Never {@code null}, possibly empty when the server returned none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<NativeMachineLearningModelAsset> assets;

    /**
     * Free-form key-value properties attached to the model, in the order the
     * server returned them. Never {@code null}, possibly empty when the server
     * returned none.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<NativeMachineLearningModelProperty> properties;

    /**
     * Constructs a new {@code NativeMachineLearningModel}. A {@code null}
     * {@code assets} or {@code properties} is coerced to an empty list, and
     * {@code name} or {@code version} may be {@code null} when the server
     * omitted them.
     *
     * @param name       the model name, or {@code null}
     * @param version    the model version marker, or {@code null}
     * @param assets     the downloadable assets; {@code null} treated as empty
     * @param properties the free-form properties; {@code null} treated as empty
     */
    NativeMachineLearningModel(String name,
                               String version,
                               List<NativeMachineLearningModelAsset> assets,
                               List<NativeMachineLearningModelProperty> properties) {
        this.name = name;
        this.version = version;
        this.assets = assets == null ? List.of() : assets;
        this.properties = properties == null ? List.of() : properties;
    }

    /**
     * Returns the model name.
     *
     * @return the model name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the model version marker.
     *
     * @return the version marker, or empty when the server omitted it
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the downloadable assets for the model.
     *
     * @return an unmodifiable view of the assets; never {@code null}, possibly
     *         empty
     */
    public List<NativeMachineLearningModelAsset> assets() {
        return Collections.unmodifiableList(assets);
    }

    /**
     * Returns the free-form properties attached to the model.
     *
     * @return an unmodifiable view of the properties; never {@code null},
     *         possibly empty
     */
    public List<NativeMachineLearningModelProperty> properties() {
        return Collections.unmodifiableList(properties);
    }
}
