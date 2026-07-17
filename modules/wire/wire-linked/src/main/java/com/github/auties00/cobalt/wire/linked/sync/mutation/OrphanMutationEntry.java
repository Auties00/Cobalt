package com.github.auties00.cobalt.wire.linked.sync.mutation;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;

/**
 * A persisted app state mutation that could not be applied at decoding time
 * because it referenced an entity not yet known to the local store.
 *
 * <p>Orphan mutations typically arrive when the server pushes a sync action
 * for a chat, contact, or message whose entity has not been materialised
 * locally (for example, because history sync has not yet delivered it). The
 * mutation is retained here together with the original plaintext index,
 * decoded action value, operation type, timestamp, and action version, so
 * that it can be replayed later when the referenced entity becomes available.
 *
 * <p>The {@link #modelType()} and {@link #modelId()} fields are derived from
 * the mutation index and allow targeted lookup of orphans by entity, so that
 * the replay pass only scans entries relevant to the newly arrived target.
 */
@ProtobufMessage
public final class OrphanMutationEntry {
    /**
     * The plaintext index string of the mutation, exactly as produced by the
     * action during encoding.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String index;

    /**
     * The decoded action value the mutation carries, used as input for the
     * handler during replay.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncActionValue value;

    /**
     * The operation kind of the mutation, either set or remove.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    SyncdOperation operation;

    /**
     * The wall clock time associated with the mutation, preserved so that
     * replays use the original occurrence time rather than the replay time.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    Instant timestamp;

    /**
     * The action version declared by the producing action, used to gate
     * handlers that depend on a minimum schema version.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    int actionVersion;

    /**
     * The action name extracted from position zero of the parsed index, used
     * to group orphans by action category for targeted lookup (for example
     * {@code "star"}, {@code "contact"}, or {@code "deleteMessageForMe"}).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String modelType;

    /**
     * The primary entity identifier extracted from position one of the parsed
     * index, typically a JID string naming the chat or contact the mutation
     * applies to, used for targeted lookup of orphans by entity.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String modelId;

    /**
     * Constructs a new orphan mutation entry with the given fields.
     *
     * @param index the plaintext index string of the mutation
     * @param value the decoded action value
     * @param operation the operation kind
     * @param timestamp the wall clock time of the mutation
     * @param actionVersion the action version number
     * @param modelType the action name extracted from the index
     * @param modelId the primary entity identifier extracted from the index
     */
    OrphanMutationEntry(String index, SyncActionValue value, SyncdOperation operation, Instant timestamp, int actionVersion, String modelType, String modelId) {
        this.index = index;
        this.value = value;
        this.operation = operation;
        this.timestamp = timestamp;
        this.actionVersion = actionVersion;
        this.modelType = modelType;
        this.modelId = modelId;
    }

    /**
     * Returns the plaintext index string of the mutation.
     *
     * @return the index string
     */
    public String index() {
        return index;
    }

    /**
     * Returns the decoded action value carried by the mutation.
     *
     * @return the action value
     */
    public SyncActionValue value() {
        return value;
    }

    /**
     * Returns the operation kind of the mutation.
     *
     * @return the operation kind, either set or remove
     */
    public SyncdOperation operation() {
        return operation;
    }

    /**
     * Returns the wall clock time associated with the mutation.
     *
     * @return the original mutation timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the action version declared by the producing action.
     *
     * @return the action version number
     */
    public int actionVersion() {
        return actionVersion;
    }

    /**
     * Returns the action name extracted from the mutation index.
     *
     * @return the action name, or {@code null} if not set
     */
    public String modelType() {
        return modelType;
    }

    /**
     * Returns the primary entity identifier extracted from the mutation
     * index, typically a JID naming the chat or contact the mutation targets.
     *
     * @return the entity identifier, or {@code null} if not set
     */
    public String modelId() {
        return modelId;
    }
}
