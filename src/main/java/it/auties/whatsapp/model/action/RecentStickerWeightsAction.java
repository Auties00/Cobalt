package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.sync.RecentStickerWeight;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model clas that represents a change in the weight of recent stickers
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Accessors(fluent = true)
public final class RecentStickerWeightsAction implements Action {
    /**
     * The weight of the stickers
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = RecentStickerWeight.class, repeated = true)
    private List<RecentStickerWeight> weights;

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action: no index name");
    }

    public static class RecentStickerWeightsActionBuilder {
        public RecentStickerWeightsActionBuilder weights(List<RecentStickerWeight> weights) {
            if (this.weights == null)
                this.weights = new ArrayList<>();
            this.weights.addAll(weights);
            return this;
        }
    }
}