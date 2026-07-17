package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Kind of change represented by a {@link SyncdMutation}.
 *
 * <p>Two operations exist: {@link #SET} associates a value with an index, and
 * {@link #REMOVE} deletes the entry for an index. Each constant also exposes a
 * one-byte tag that is used during MAC computation so that {@code SET} and
 * {@code REMOVE} mutations targeting the same index produce distinct MACs.
 */
@ProtobufEnum(name = "SyncdMutation.SyncdOperation")
public enum SyncdOperation {
    /**
     * Inserts or overwrites the value for the record's index.
     */
    SET(0, ((byte) (0x1))),
    /**
     * Deletes the record identified by the index.
     */
    REMOVE(1, ((byte) (0x2)));

    /**
     * The protobuf wire index assigned to this operation.
     */
    final int index;
    /**
     * One-byte discriminator mixed into MAC derivation to separate {@code SET}
     * from {@code REMOVE} for the same index.
     */
    private final byte content;

    /**
     * Constructs an operation with the given protobuf index and MAC tag byte.
     *
     * @param index the protobuf wire index
     * @param content the discriminator byte used during MAC computation
     */
    SyncdOperation(@ProtobufEnumIndex int index, byte content) {
        this.index = index;
        this.content = content;
    }

    /**
     * Returns the protobuf wire index of this operation.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the discriminator byte used during MAC derivation to
     * distinguish this operation from the other.
     *
     * @return the operation tag byte
     */
    public byte content() {
        return content;
    }
}
