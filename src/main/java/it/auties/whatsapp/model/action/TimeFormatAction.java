package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

/**
 * A model clas that represents the time format used by the companion
 */
@ProtobufMessage(name = "SyncActionValue.TimeFormatAction")
public record TimeFormatAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean twentyFourHourFormatEnabled
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "time_format";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 7;
    }

    /**
     * The type of this action
     *
     * @return a non-null string
     */
    @Override
    public PatchType actionType() {
        return null;
    }
}
