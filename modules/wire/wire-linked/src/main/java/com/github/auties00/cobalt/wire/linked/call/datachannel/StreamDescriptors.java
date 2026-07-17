package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container that lists every stream a call is using.
 *
 * <p>Published once during call setup and again whenever the multiplex
 * changes (for example when video starts or a screen share is added);
 * carries one {@link StreamDescriptor} per active logical stream.
 */
@ProtobufMessage(name = "StreamDescriptors")
public final class StreamDescriptors {
    /**
     * The per-stream descriptors.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<StreamDescriptor> streamDescriptors;

    /**
     * Constructs a new {@code StreamDescriptors}.
     *
     * @param streamDescriptors the per-stream descriptors
     */
    StreamDescriptors(List<StreamDescriptor> streamDescriptors) {
        this.streamDescriptors = streamDescriptors;
    }

    /**
     * Returns the per-stream descriptors.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<StreamDescriptor> streamDescriptors() {
        return streamDescriptors == null ? List.of() : Collections.unmodifiableList(streamDescriptors);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof StreamDescriptors that
                && Objects.equals(this.streamDescriptors, that.streamDescriptors));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(streamDescriptors);
    }

    @Override
    public String toString() {
        return "StreamDescriptors[streamDescriptors=" + streamDescriptors() + ']';
    }
}
