package com.github.auties00.cobalt.wire.linked.sync.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Persistent state kept for each app state sync mutation that has been
 * observed locally.
 *
 * <p>The entry stores everything required to:
 * <ul>
 *   <li>recompute the collection level integrity hash (via the index and
 *       value MAC pair)</li>
 *   <li>re-encrypt the mutation under a newer app state sync key during key
 *       rotation, without requiring a full resync from the server</li>
 *   <li>audit mutation processing and drive orphan retry through the
 *       {@link SyncActionState} category together with the target entity
 *       type and identifier</li>
 * </ul>
 *
 * <p>The key identifier of the encrypting key is preserved so that the
 * {@code REMOVE} operation that follows a {@code SET} can reuse the same
 * key, matching the protocol requirement for paired set and remove
 * mutations.
 */
@ProtobufMessage
public final class SyncActionEntry {
    /**
     * HMAC of the plaintext mutation index, used as the lookup key in the
     * sync action entry map.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] indexMac;

    /**
     * HMAC of the encrypted mutation value, used as input to the collection
     * integrity hash that protects against tampering and reordering.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] valueMac;

    /**
     * Identifier of the app state sync key that was used to encrypt the
     * mutation, preserved so that matched set and remove mutations reuse
     * the same key.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] keyId;

    /**
     * Plaintext index string of the mutation, preserved so the entry can be
     * re-encrypted under a rotated app state sync key without a full resync.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String actionIndex;

    /**
     * Decoded action value of the mutation, preserved so the entry can be
     * re-encrypted under a rotated app state sync key without a full resync.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    SyncActionValue actionValue;

    /**
     * Action schema version of the mutation, preserved so the entry can be
     * re-encrypted under a rotated app state sync key without a full resync.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT32)
    int actionVersion;

    /**
     * Processing outcome of the mutation, enabling auditing, orphan
     * management, and replay of mutations that could not be applied during
     * their initial sync round.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT32)
    SyncActionState actionState;

    /**
     * Identifier of the entity targeted by the mutation, such as a chat JID
     * for chat actions or a message id for message actions, used together
     * with {@link #modelType} to drive targeted orphan replay on entity
     * arrival.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String modelId;

    /**
     * Kind of entity targeted by the mutation, such as {@code "chat"},
     * {@code "message"}, or {@code "contact"}, used together with
     * {@link #modelId} to drive targeted orphan replay on entity arrival.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String modelType;

    /**
     * Constructs a new sync action entry with the given fields.
     *
     * @param indexMac the HMAC of the plaintext index
     * @param valueMac the HMAC of the encrypted value
     * @param keyId the identifier of the encrypting app state sync key
     * @param actionIndex the plaintext index string
     * @param actionValue the decoded action payload
     * @param actionVersion the action schema version
     * @param actionState the processing outcome for the mutation
     * @param modelId the identifier of the targeted entity
     * @param modelType the kind of the targeted entity
     */
    SyncActionEntry(byte[] indexMac, byte[] valueMac, byte[] keyId, String actionIndex, SyncActionValue actionValue, int actionVersion, SyncActionState actionState, String modelId, String modelType) {
        this.indexMac = indexMac;
        this.valueMac = valueMac;
        this.keyId = keyId;
        this.actionIndex = actionIndex;
        this.actionValue = actionValue;
        this.actionVersion = actionVersion;
        this.actionState = actionState;
        this.modelId = modelId;
        this.modelType = modelType;
    }

    /**
     * Returns the HMAC of the plaintext mutation index.
     *
     * @return the index MAC bytes
     */
    public byte[] indexMac() {
        return indexMac;
    }

    /**
     * Returns the HMAC of the encrypted mutation value.
     *
     * @return the value MAC bytes
     */
    public byte[] valueMac() {
        return valueMac;
    }

    /**
     * Returns the identifier of the encrypting app state sync key.
     *
     * @return the key identifier bytes
     */
    public byte[] keyId() {
        return keyId;
    }

    /**
     * Returns the plaintext index string preserved for key rotation
     * re-encryption.
     *
     * @return the index string, or {@code null} if not stored
     */
    public String actionIndex() {
        return actionIndex;
    }

    /**
     * Returns the decoded action value preserved for key rotation
     * re-encryption.
     *
     * @return the action value, or {@code null} if not stored
     */
    public SyncActionValue actionValue() {
        return actionValue;
    }

    /**
     * Returns the action schema version preserved for key rotation
     * re-encryption.
     *
     * @return the action version
     */
    public int actionVersion() {
        return actionVersion;
    }

    /**
     * Sets the HMAC of the plaintext mutation index.
     *
     * @param indexMac the index MAC bytes
     */
    public void setIndexMac(byte[] indexMac) {
        this.indexMac = indexMac;
    }

    /**
     * Sets the HMAC of the encrypted mutation value.
     *
     * @param valueMac the value MAC bytes
     */
    public void setValueMac(byte[] valueMac) {
        this.valueMac = valueMac;
    }

    /**
     * Sets the identifier of the encrypting app state sync key.
     *
     * @param keyId the key identifier bytes
     */
    public void setKeyId(byte[] keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the plaintext index string preserved for key rotation
     * re-encryption.
     *
     * @param actionIndex the index string
     */
    public void setActionIndex(String actionIndex) {
        this.actionIndex = actionIndex;
    }

    /**
     * Sets the decoded action value preserved for key rotation re-encryption.
     *
     * @param actionValue the action value
     */
    public void setActionValue(SyncActionValue actionValue) {
        this.actionValue = actionValue;
    }

    /**
     * Sets the action schema version preserved for key rotation re-encryption.
     *
     * @param actionVersion the action version
     */
    public void setActionVersion(int actionVersion) {
        this.actionVersion = actionVersion;
    }

    /**
     * Returns the processing outcome of the mutation.
     *
     * @return the action state, or {@code null} if not set
     */
    public SyncActionState actionState() {
        return actionState;
    }

    /**
     * Sets the processing outcome of the mutation.
     *
     * @param actionState the action state
     */
    public void setActionState(SyncActionState actionState) {
        this.actionState = actionState;
    }

    /**
     * Returns the identifier of the entity targeted by the mutation.
     *
     * @return the entity identifier, or {@code null} if not set
     */
    public String modelId() {
        return modelId;
    }

    /**
     * Sets the identifier of the entity targeted by the mutation.
     *
     * @param modelId the entity identifier
     */
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    /**
     * Returns the kind of entity targeted by the mutation.
     *
     * @return the entity kind, or {@code null} if not set
     */
    public String modelType() {
        return modelType;
    }

    /**
     * Sets the kind of entity targeted by the mutation.
     *
     * @param modelType the entity kind
     */
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}
