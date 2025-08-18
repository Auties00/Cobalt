package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.RecentEmojiWeight;

import java.util.List;

/**
 * A model clas that represents a change in the weight of recent emojis
 */
@ProtobufMessage(name = "SyncActionValue.RecentEmojiWeightsAction")
public record RecentEmojiWeightsAction(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<RecentEmojiWeight> weights
) implements Action {
    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public int actionVersion() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public PatchType actionType() {
        throw new UnsupportedOperationException("Cannot send action");
    }
}
