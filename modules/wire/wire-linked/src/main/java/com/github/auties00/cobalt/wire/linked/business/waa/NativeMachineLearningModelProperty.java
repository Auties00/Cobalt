package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One free-form key-value property attached to a
 * {@link NativeMachineLearningModel}.
 *
 * <p>The server attaches free-form properties to a model so the client can
 * configure model-specific behaviour without a schema change. Both the name and
 * the value are exposed as raw strings.
 */
@ProtobufMessage(name = "NativeMachineLearningModelProperty")
public final class NativeMachineLearningModelProperty {
    /**
     * Property name. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Property value. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String value;

    /**
     * Constructs a new {@code NativeMachineLearningModelProperty}.
     *
     * @param name  the property name, or {@code null}
     * @param value the property value, or {@code null}
     */
    NativeMachineLearningModelProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the property name.
     *
     * @return the property name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the property value.
     *
     * @return the property value, or empty when the server omitted it
     */
    public Optional<String> value() {
        return Optional.ofNullable(value);
    }
}
