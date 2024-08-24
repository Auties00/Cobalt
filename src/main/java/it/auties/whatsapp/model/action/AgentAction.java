package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.Optional;

/**
 * A model clas that represents an agent
 */
@ProtobufMessage(name = "SyncActionValue.AgentAction")
public record AgentAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> name,
        @ProtobufProperty(index = 2, type = ProtobufType.INT32)
        int deviceId,
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        boolean deleted
) implements Action {

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "deviceAgent";
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
