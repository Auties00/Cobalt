package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Client decode-capability metadata sent alongside a native machine-learning model request.
 *
 * <p>A native-model manifest query tells the server which model bytecode versions the client can
 * execute so the server returns compatible assets. This model carries that capability set as the list
 * of supported {@link #bytecodeVersion() bytecode versions}.
 */
@ProtobufMessage(name = "ClientCapabilityMetadata")
public final class ClientCapabilityMetadata {
    /**
     * Bytecode versions the client can execute, in the order they are sent. Never {@code null},
     * possibly empty.
     */
    // TODO: element type unconfirmed - WAWebBweMLModelManager always sends an empty array; confirm
    //  against the server AIMModelManifest input schema
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final List<Integer> bytecodeVersion;

    /**
     * Constructs a new {@code ClientCapabilityMetadata}. A {@code null} {@code bytecodeVersion} is
     * coerced to an empty list.
     *
     * @param bytecodeVersion the supported bytecode versions; {@code null} treated as empty
     */
    ClientCapabilityMetadata(List<Integer> bytecodeVersion) {
        this.bytecodeVersion = bytecodeVersion == null ? List.of() : List.copyOf(bytecodeVersion);
    }

    /**
     * Returns the bytecode versions the client can execute.
     *
     * @return an unmodifiable view of the bytecode versions; never {@code null}, possibly empty
     */
    public List<Integer> bytecodeVersion() {
        return bytecodeVersion;
    }
}
