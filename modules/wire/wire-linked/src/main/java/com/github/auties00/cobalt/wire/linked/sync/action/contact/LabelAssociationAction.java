package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A sync action that records the association or disassociation of a label
 * with a chat or contact.
 *
 * <p>WhatsApp users can organise their chats with coloured labels (for
 * example "Work", "Family" or a predefined filter list). When a label is
 * applied to or removed from a chat, a {@code LabelAssociationAction} is
 * appended to the sync patch so that every linked device reflects the same
 * label membership.
 *
 * <p>Each association is indexed by the {@code (labelId, chatJid)} pair
 * through {@link LabelAssociationActionArgs} and is replicated via the
 * {@link SyncPatchType#REGULAR} collection.
 */
@ProtobufMessage(name = "SyncActionValue.LabelAssociationAction")
public final class LabelAssociationAction implements SyncAction<LabelAssociationActionArgs> {
    /**
     * The canonical action name {@code "label_jid"} used to identify this
     * action inside a sync patch.
     */
    public static final String ACTION_NAME = "label_jid";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name {@code "label_jid"}.
     *
     * @return the string {@code "label_jid"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version declared by this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether the label is currently applied to the chat or contact, or has
     * been removed from it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean labeled;


    /**
     * Constructs a new {@code LabelAssociationAction}. Intended to be invoked
     * by the generated builder and by the protobuf deserializer.
     *
     * @param labeled whether the label is applied, or {@code null} to leave
     *                the flag unset
     */
    LabelAssociationAction(Boolean labeled) {
        this.labeled = labeled;
    }

    /**
     * Returns whether the label is currently applied to the chat or contact,
     * coalescing an absent value to {@code false}.
     *
     * @return {@code true} if the label is applied, otherwise {@code false}
     */
    public boolean labeled() {
        return labeled != null && labeled;
    }

    /**
     * Updates whether the label is currently applied to the chat or contact.
     *
     * @param labeled the new flag value, or {@code null} to clear the field
     */
    public void setLabeled(Boolean labeled) {
        this.labeled = labeled;
    }


}
