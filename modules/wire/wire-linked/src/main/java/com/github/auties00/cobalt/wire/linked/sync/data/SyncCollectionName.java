package com.github.auties00.cobalt.wire.linked.sync.data;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Logical identifier of an app-state collection used in sync messages.
 *
 * <p>App-state mutations are partitioned across a small fixed set of
 * named collections so that critical state (such as the block list) can
 * be fetched and applied before bulkier non-critical state. The
 * canonical names of those partitions are part of the wire protocol
 * (lowercase strings such as {@code "regular"} or
 * {@code "critical_block"}) and are used as keys in patch messages,
 * snapshot envelopes, and mutation requests.
 *
 * <p>This enum mirrors the five canonical collections and prepends an
 * {@link #COLLECTION_NAME_UNKNOWN} sentinel so the protobuf serializer
 * can represent a missing or unrecognised value. At runtime, sync
 * operations primarily use the companion
 * {@link com.github.auties00.cobalt.wire.linked.sync.SyncPatchType} enum,
 * which carries both the protobuf index and the lowercase wire name;
 * this enum is retained for forward compatibility with protobuf
 * messages that reference {@code CollectionName} directly.
 */
@ProtobufEnum(name = "CollectionName")
public enum SyncCollectionName {
    /**
     * Sentinel value used when the collection field is missing or
     * carries an unrecognised wire value.
     */
    COLLECTION_NAME_UNKNOWN(0),

    /**
     * Standard-priority collection holding the bulk of regular user
     * actions.
     */
    REGULAR(1),

    /**
     * Low-priority collection used for large volumes of non-essential
     * actions such as note edits.
     */
    REGULAR_LOW(2),

    /**
     * High-priority collection for actions that must converge quickly
     * across companion devices.
     */
    REGULAR_HIGH(3),

    /**
     * Critical collection containing block-list-related state.
     */
    CRITICAL_BLOCK(4),

    /**
     * Critical, low-priority collection containing unblock-related state
     * applied alongside the critical block collection during bootstrap.
     */
    CRITICAL_UNBLOCK_LOW(5);

    /**
     * The protobuf wire index assigned to this collection.
     */
    final int index;

    /**
     * Constructs a collection-name constant with the given protobuf
     * index.
     *
     * @param index the protobuf wire index
     */
    SyncCollectionName(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire index of this collection name.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return this.index;
    }
}
