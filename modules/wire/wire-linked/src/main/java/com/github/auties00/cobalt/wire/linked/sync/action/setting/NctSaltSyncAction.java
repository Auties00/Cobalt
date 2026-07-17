package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents the Notification Content Tokenizer (NCT) salt shared across
 * the user's linked devices.
 *
 * <p>The NCT salt is a secret byte string used to derive a
 * privacy-preserving tokenization of notification payloads. Every linked
 * device must use the same salt so that tokens computed by one device can
 * be recognised by the others. Rotating the salt invalidates previously
 * generated tokens and is performed transparently through this sync
 * action.
 *
 * <p>This action is keyed by a singleton index (see
 * {@link NctSaltSyncActionArgs}); each new publication replaces the
 * previous salt.
 */
@ProtobufMessage(name = "SyncActionValue.NctSaltSyncAction")
public final class NctSaltSyncAction implements SyncAction<NctSaltSyncActionArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "nct_salt_sync";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * App state collection that stores this setting.
     *
     * <p>Because the salt is required promptly on linked devices, it is
     * delivered through the higher-priority {@code REGULAR_HIGH} patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * The raw salt bytes shared across devices.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] salt;

    /**
     * Constructs a new action carrying the given salt.
     *
     * @param salt the salt bytes, or {@code null} if no value is set
     */
    NctSaltSyncAction(byte[] salt) {
        this.salt = salt;
    }

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the current NCT salt.
     *
     * @return an {@link Optional} containing the salt bytes, or empty when
     *         the setting has never been written
     */
    public Optional<byte[]> salt() {
        return Optional.ofNullable(salt);
    }
}
