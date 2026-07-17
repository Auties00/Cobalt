package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * A sync action recording the engagement metrics of an individual broadcast send produced from
 * a marketing message template.
 *
 * <p>When a business sends a {@link MarketingMessageAction} template to a recipient, the
 * server aggregates reply statistics for that specific send and emits this action so that
 * every linked device sees the same counter. Currently the only tracked metric is the number
 * of replies the broadcast received.
 *
 * <p>This action is transported in the {@code REGULAR} sync collection and keyed by the pair
 * of marketing message identifier and broadcast message identifier through
 * {@link MarketingMessageBroadcastActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.MarketingMessageBroadcastAction")
public final class MarketingMessageBroadcastAction implements SyncAction<MarketingMessageBroadcastActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "marketingMessageBroadcast";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 7;

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
     * The number of replies received by this broadcast send.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer repliedCount;


    /**
     * Creates a broadcast engagement action with the supplied reply count.
     *
     * @param repliedCount the number of replies received, or {@code null} if not set
     */
    MarketingMessageBroadcastAction(Integer repliedCount) {
        this.repliedCount = repliedCount;
    }

    /**
     * Returns the number of replies received by this broadcast send.
     *
     * @return the reply count, or {@link OptionalInt#empty()} if not set
     */
    public OptionalInt repliedCount() {
        return repliedCount == null ? OptionalInt.empty() : OptionalInt.of(repliedCount);
    }

    /**
     * Updates the reply count for this broadcast send.
     *
     * @param repliedCount the new reply count, or {@code null} to clear it
     */
    public void setRepliedCount(Integer repliedCount) {
        this.repliedCount = repliedCount;
    }


}
