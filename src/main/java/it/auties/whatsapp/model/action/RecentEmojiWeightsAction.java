package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.RecentEmojiWeight;

import java.util.List;
import java.util.Objects;

/**
 * A model clas that represents a change in the weight of recent emojis
 */
@ProtobufMessage(name = "SyncActionValue.RecentEmojiWeightsAction")
public final class RecentEmojiWeightsAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<RecentEmojiWeight> weights;

    RecentEmojiWeightsAction(List<RecentEmojiWeight> weights) {
        this.weights = weights;
    }

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    @Override
    public int actionVersion() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    public List<RecentEmojiWeight> weights() {
        return weights;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RecentEmojiWeightsAction that
                && Objects.equals(weights, that.weights);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(weights);
    }

    @Override
    public String toString() {
        return "RecentEmojiWeightsAction[" +
                "weights=" + weights + ']';
    }
}
