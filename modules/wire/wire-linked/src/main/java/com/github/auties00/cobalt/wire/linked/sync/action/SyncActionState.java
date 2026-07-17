package com.github.auties00.cobalt.wire.linked.sync.action;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Outcome category assigned to every app state sync mutation after it has
 * been processed against local state.
 *
 * <p>Every mutation processed by the sync pipeline is tagged with one of
 * these states. The category drives bookkeeping downstream of processing:
 * successful mutations are acknowledged and their entries are retained for
 * integrity hashing; orphan mutations are persisted and retried when the
 * referenced entity arrives; unsupported mutations are kept for forward
 * compatibility; and malformed, skipped, or failed mutations are logged or
 * surfaced to the error handler.
 */
@ProtobufEnum
public enum SyncActionState {
    /**
     * The mutation was successfully applied to local state.
     */
    SUCCESS(0),

    /**
     * The mutation references an entity not yet present locally and has
     * been persisted for replay once the entity arrives.
     */
    ORPHAN(1),

    /**
     * The mutation's action type has no registered handler and has been
     * retained so that a future handler can replay it.
     */
    UNSUPPORTED(2),

    /**
     * The mutation payload failed to decode or passed a structural check
     * that rejected it.
     */
    MALFORMED(3),

    /**
     * The mutation was intentionally skipped by its handler, for example
     * because it was version gated or duplicated.
     */
    SKIPPED(4),

    /**
     * The handler encountered an error while attempting to apply the
     * mutation.
     */
    FAILED(5);

    /**
     * Protobuf wire index for this enum constant.
     */
    final int index;

    /**
     * Constructs a new enum constant with the given protobuf wire index.
     *
     * @param index the protobuf wire index
     */
    SyncActionState(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire index for this enum constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return index;
    }
}
