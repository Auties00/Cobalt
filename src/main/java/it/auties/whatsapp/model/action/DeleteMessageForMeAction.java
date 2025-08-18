package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model clas that represents a message deleted for this client
 */
@ProtobufMessage(name = "SyncActionValue.DeleteMessageForMeAction")
public record DeleteMessageForMeAction(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean deleteMedia,
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long messageTimestampSeconds
) implements Action {
    /**
     * Returns when the deleted message was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> messageTimestamp() {
        return Clock.parseSeconds(messageTimestampSeconds);
    }

    /**
     * The name of this action
     *
     * @return a non-null string
     */
    @Override
    public String indexName() {
        return "deleteMessageForMe";
    }

    /**
     * The version of this action
     *
     * @return a non-null string
     */
    @Override
    public int actionVersion() {
        return 3;
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
