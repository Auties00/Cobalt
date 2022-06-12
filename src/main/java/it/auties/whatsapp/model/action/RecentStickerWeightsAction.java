package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.sync.RecentStickerWeight;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class RecentStickerWeightsAction implements Action {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = RecentStickerWeight.class, repeated = true)
    private List<RecentStickerWeight> weights;

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