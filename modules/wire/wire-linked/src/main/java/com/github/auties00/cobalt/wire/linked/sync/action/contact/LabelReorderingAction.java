package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * A sync action that records the overall ordering of the user's labels.
 *
 * <p>Rather than updating each label individually, a single
 * {@code LabelReorderingAction} carries the complete ordered list of label
 * identifiers after the user has rearranged them in the labels drawer. The
 * action is a singleton in the app state (it is keyed by an empty index)
 * and supersedes any previous ordering.
 *
 * <p>This action is replicated via the {@link SyncPatchType#REGULAR}
 * collection. It uses {@link SyncActionEmptyArgs} because only one such
 * entry exists per account.
 */
@ProtobufMessage(name = "SyncActionValue.LabelReorderingAction")
public final class LabelReorderingAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name {@code "label_reordering"} used to identify
     * this action inside a sync patch.
     */
    public static final String ACTION_NAME = "label_reordering";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name {@code "label_reordering"}.
     *
     * @return the string {@code "label_reordering"}
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
     * The label identifiers in the order chosen by the user.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    List<Integer> sortedLabelIds;


    /**
     * Constructs a new {@code LabelReorderingAction}. Intended to be invoked
     * by the generated builder and by the protobuf deserializer.
     *
     * @param sortedLabelIds the ordered list of label identifiers, or
     *                       {@code null} if the field is absent
     */
    LabelReorderingAction(List<Integer> sortedLabelIds) {
        this.sortedLabelIds = sortedLabelIds;
    }

    /**
     * Returns the ordered list of label identifiers as an unmodifiable view.
     *
     * @return the label identifiers in the order chosen by the user, or an
     *         empty list if the field was not provided
     */
    public List<Integer> sortedLabelIds() {
        return sortedLabelIds == null ? List.of() : Collections.unmodifiableList(sortedLabelIds);
    }

    /**
     * Updates the ordered list of label identifiers.
     *
     * @param sortedLabelIds the new ordered list, or {@code null} to clear it
     */
    public void setSortedLabelIds(List<Integer> sortedLabelIds) {
        this.sortedLabelIds = sortedLabelIds;
    }
}
