package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents a sync action that toggles whether the primary Android companion is
 * allowed to participate in sync mutations that it does not fully understand.
 *
 * <p>When new action types are introduced server side but an older Android build
 * does not yet know how to deserialise them, WhatsApp uses this singleton flag to
 * decide whether the Android device should forward those unknown mutations
 * verbatim or skip them. Because the flag is singleton, the mutation index is
 * simply {@link #ACTION_NAME} with no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.AndroidUnsupportedActions")
public final class AndroidUnsupportedActions implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "android_unsupported_actions";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 4;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for every {@code AndroidUnsupportedActions}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code AndroidUnsupportedActions}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag that permits Android companions to forward mutations of action types
     * they do not natively understand.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean allowed;


    /**
     * Constructs a new {@code AndroidUnsupportedActions} from raw protobuf field
     * values.
     *
     * @param allowed whether unsupported actions are allowed, possibly {@code null}
     */
    AndroidUnsupportedActions(Boolean allowed) {
        this.allowed = allowed;
    }

    /**
     * Returns whether Android companions are allowed to forward mutations of
     * action types they do not natively understand.
     *
     * @return {@code true} if forwarding is allowed, {@code false} otherwise
     *         (including when the field was unset on the wire)
     */
    public boolean allowed() {
        return allowed != null && allowed;
    }

    /**
     * Sets the flag that permits Android companions to forward unsupported
     * actions.
     *
     * @param allowed {@code true} to permit forwarding, {@code false} to block
     *                it, or {@code null} to clear
     */
    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
}
