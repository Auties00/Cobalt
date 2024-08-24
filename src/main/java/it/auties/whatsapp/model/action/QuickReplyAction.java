package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;

import java.util.List;

/**
 * A model clas that represents the addition or deletion of a quick reply
 */
@ProtobufMessage(name = "SyncActionValue.QuickReplyAction")
public record QuickReplyAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String shortcut,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String message,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        List<String> keywords,
        @ProtobufProperty(index = 4, type = ProtobufType.INT32)
        int count,
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        boolean deleted
) implements Action {
    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "quick_reply";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 2;
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
