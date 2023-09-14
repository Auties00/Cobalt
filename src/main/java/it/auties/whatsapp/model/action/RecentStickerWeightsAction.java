package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.model.sync.RecentStickerWeight;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * A model clas that represents a change in the weight of recent stickers
 */
public record RecentStickerWeightsAction(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT, repeated = true)
        @NonNull
        List<RecentStickerWeight> weights
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