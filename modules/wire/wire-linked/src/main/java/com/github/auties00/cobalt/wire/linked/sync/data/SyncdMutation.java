package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Single change to apply to an app-state collection.
 *
 * <p>A mutation pairs an {@link SyncdOperation} (set or remove) with the
 * {@link SyncdRecord} it targets. Mutations are the atomic units contained in
 * {@link SyncdPatch} messages: applying a patch in order produces a new
 * collection state, and the same sequence is replayed by other devices of the
 * account to stay in sync.
 */
@ProtobufMessage(name = "SyncdMutation")
public final class SyncdMutation {
    /**
     * Operation performed by this mutation ({@code SET} or {@code REMOVE}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    SyncdOperation operation;

    /**
     * Record (index, value and key identifier) that the operation targets.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncdRecord record;


    /**
     * Constructs a new mutation.
     *
     * @param operation the operation to apply
     * @param record the record the operation targets
     */
    SyncdMutation(SyncdOperation operation, SyncdRecord record) {
        this.operation = operation;
        this.record = record;
    }

    /**
     * Returns the operation performed by this mutation.
     *
     * @return the operation, or empty if absent
     */
    public Optional<SyncdOperation> operation() {
        return Optional.ofNullable(operation);
    }

    /**
     * Returns the record targeted by this mutation.
     *
     * @return the target record, or empty if absent
     */
    public Optional<SyncdRecord> record() {
        return Optional.ofNullable(record);
    }

    /**
     * Sets the operation performed by this mutation.
     *
     * @param operation the operation
     */
    public void setOperation(SyncdOperation operation) {
        this.operation = operation;
    }

    /**
     * Sets the record targeted by this mutation.
     *
     * @param record the target record
     */
    public void setRecord(SyncdRecord record) {
        this.record = record;
    }
}
