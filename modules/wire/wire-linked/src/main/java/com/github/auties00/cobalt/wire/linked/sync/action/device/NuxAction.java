package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents a sync action that records acknowledgement of a New User Experience
 * (NUX) onboarding prompt.
 *
 * <p>WhatsApp shows one-time educational prompts, tooltips, and feature
 * announcements identified by a NUX key. Once the user dismisses or interacts
 * with a prompt, this action is replicated to every linked device so the same
 * prompt is not shown twice. Each mutation targets one NUX prompt identified by
 * the key carried in {@link NuxActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.NuxAction")
public final class NuxAction implements SyncAction<NuxActionArgs> {
    /**
     * Canonical action name used as the first component of the mutation index
     * for every {@code NuxAction} replicated through the app state sync
     * protocol.
     */
    public static final String ACTION_NAME = "nux";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for every {@code NuxAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code NuxAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag that records whether the user has acknowledged the NUX prompt
     * identified by the associated index key.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean acknowledged;


    /**
     * Constructs a new {@code NuxAction} from raw protobuf field values.
     *
     * @param acknowledged whether the prompt has been acknowledged, possibly
     *                     {@code null}
     */
    NuxAction(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    /**
     * Returns whether the user has acknowledged the NUX prompt.
     *
     * @return {@code true} if the prompt has been acknowledged, {@code false}
     *         otherwise (including when the field was unset on the wire)
     */
    public boolean acknowledged() {
        return acknowledged != null && acknowledged;
    }

    /**
     * Sets the acknowledgement flag for the NUX prompt.
     *
     * @param acknowledged {@code true} to mark as acknowledged, {@code false} to
     *                     reset, or {@code null} to clear
     */
    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }


}
