package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A sync action recording the association between a business broadcast list and a recipient JID.
 *
 * <p>Business accounts maintain broadcast lists that group recipients together. When a
 * recipient is added or removed from such a list, an association action is emitted so that
 * every linked device reflects the same membership. The optional {@code deleted} flag marks
 * the association as removed rather than created.
 *
 * <p>This action is transported in the {@code REGULAR} sync collection and keyed by the
 * pair of broadcast list identifier and recipient JID.
 */
@ProtobufMessage(name = "SyncActionValue.BusinessBroadcastAssociationAction")
public final class BusinessBroadcastAssociationAction implements SyncAction<BusinessBroadcastAssociationActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "broadcast_jid";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Flag indicating whether this association has been removed. A {@code null} or {@code false}
     * value represents an active (added) association, while {@code true} represents a deletion.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean deleted;


    /**
     * Creates an association action with the supplied deletion flag.
     *
     * @param deleted {@code true} if the association is being removed, {@code false} or
     *                {@code null} otherwise
     */
    BusinessBroadcastAssociationAction(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Returns whether this association represents a removal of the recipient from the list.
     *
     * @return {@code true} if the recipient has been removed, {@code false} otherwise
     */
    public boolean deleted() {
        return deleted != null && deleted;
    }

    /**
     * Updates the deletion flag of this association.
     *
     * @param deleted {@code true} to mark the association as removed, {@code false} or
     *                {@code null} to mark it as active
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }


}
