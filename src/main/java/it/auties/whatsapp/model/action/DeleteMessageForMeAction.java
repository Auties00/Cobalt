package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents a message deleted for this client
 */
@ProtobufMessage(name = "SyncActionValue.DeleteMessageForMeAction")
public final class DeleteMessageForMeAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean deleteMedia;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long messageTimestampSeconds;

    DeleteMessageForMeAction(boolean deleteMedia, long messageTimestampSeconds) {
        this.deleteMedia = deleteMedia;
        this.messageTimestampSeconds = messageTimestampSeconds;
    }

    @Override
    public String indexName() {
        return "deleteMessageForMe";
    }

    @Override
    public int actionVersion() {
        return 3;
    }

    public boolean deleteMedia() {
        return deleteMedia;
    }

    public long messageTimestampSeconds() {
        return messageTimestampSeconds;
    }

    public Optional<ZonedDateTime> messageTimestamp() {
        return Clock.parseSeconds(messageTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeleteMessageForMeAction that
                && deleteMedia == that.deleteMedia
                && messageTimestampSeconds == that.messageTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deleteMedia, messageTimestampSeconds);
    }

    @Override
    public String toString() {
        return "DeleteMessageForMeAction[" +
                "deleteMedia=" + deleteMedia + ", " +
                "messageTimestampSeconds=" + messageTimestampSeconds + ']';
    }
}
