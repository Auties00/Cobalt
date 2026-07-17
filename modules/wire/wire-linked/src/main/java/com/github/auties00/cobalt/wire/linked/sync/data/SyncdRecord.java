package com.github.auties00.cobalt.wire.linked.sync.data;

import com.github.auties00.cobalt.wire.linked.signal.KeyId;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Entry in an app-state collection.
 *
 * <p>A record ties three pieces together: a {@link SyncdIndex} acting as the
 * stable key inside the collection, a {@link SyncdValue} carrying the
 * encrypted action bytes, and a {@link KeyId} pointing at the sync key used
 * to produce the index MAC and encrypt the value. The same record appears in
 * {@link SyncdMutation} messages (as the target of a {@code SET} or
 * {@code REMOVE}) and in {@link SyncdSnapshot} payloads (as an element of the
 * collection state).
 */
@ProtobufMessage(name = "SyncdRecord")
public final class SyncdRecord {
    /**
     * Stable key of this record inside its collection.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncdIndex index;

    /**
     * Encrypted value associated with the index.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncdValue value;

    /**
     * Identifier of the sync key used to encrypt the value and derive the
     * index MAC.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    KeyId keyId;


    /**
     * Constructs a new record.
     *
     * @param index the index inside the collection
     * @param value the encrypted value
     * @param keyId the sync key identifier
     */
    SyncdRecord(SyncdIndex index, SyncdValue value, KeyId keyId) {
        this.index = index;
        this.value = value;
        this.keyId = keyId;
    }

    /**
     * Returns the index of this record.
     *
     * @return the index, or empty if absent
     */
    public Optional<SyncdIndex> index() {
        return Optional.ofNullable(index);
    }

    /**
     * Returns the encrypted value of this record.
     *
     * @return the value, or empty if absent
     */
    public Optional<SyncdValue> value() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the identifier of the sync key that encrypted the value.
     *
     * @return the key id, or empty if absent
     */
    public Optional<KeyId> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Sets the index of this record.
     *
     * @param index the index inside the collection
     */
    public void setIndex(SyncdIndex index) {
        this.index = index;
    }

    /**
     * Sets the encrypted value of this record.
     *
     * @param value the value
     */
    public void setValue(SyncdValue value) {
        this.value = value;
    }

    /**
     * Sets the identifier of the sync key used to encrypt the value.
     *
     * @param keyId the key id
     */
    public void setKeyId(KeyId keyId) {
        this.keyId = keyId;
    }
}
