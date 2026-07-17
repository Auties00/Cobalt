package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * A sync action that propagates the user's recent-emoji usage weights across
 * linked devices.
 *
 * <p>The action carries a list of {@link RecentEmojiWeight} entries, each
 * mapping an emoji glyph to a usage weight. These weights are used to rank
 * the emojis surfaced in the recent-emoji picker so that every linked device
 * shows the same ordering.
 */
@ProtobufMessage(name = "SyncActionValue.RecentEmojiWeightsAction")
public final class RecentEmojiWeightsAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "recent_emoji_weights_action";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 11;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the action name used to route this action through the app-state
     * sync pipeline.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version used to route this action through the
     * app-state sync pipeline.
     *
     * @return the canonical action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The list of per-emoji usage weights propagated by this action.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<RecentEmojiWeight> weights;


    /**
     * Constructs a new {@code RecentEmojiWeightsAction} carrying the supplied
     * per-emoji usage weights.
     *
     * @param weights the per-emoji weights, or {@code null} if unset
     */
    RecentEmojiWeightsAction(List<RecentEmojiWeight> weights) {
        this.weights = weights;
    }

    /**
     * Returns the per-emoji usage weights carried by this action.
     *
     * @return an unmodifiable view of the weights, never {@code null}
     */
    public List<RecentEmojiWeight> weights() {
        return weights == null ? List.of() : Collections.unmodifiableList(weights);
    }

    /**
     * Sets the per-emoji usage weights carried by this action.
     *
     * @param weights the new weights list, or {@code null} to clear it
     */
    public void setWeights(List<RecentEmojiWeight> weights) {
        this.weights = weights;
    }
}
