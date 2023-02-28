package it.auties.whatsapp.model.action;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.util.Clock;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.INT64;

/**
 * A model clas that represents a message deleted for this client
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class DeleteMessageForMeAction implements Action {
    /**
     * Whether the media should be removed from the memory of the client
     */
    @ProtobufProperty(index = 1, type = BOOL)
    private boolean deleteMedia;

    /**
     * The timestamp of the message
     */
    @ProtobufProperty(index = 2, type = INT64)
    private long messageTimestampInSeconds;

    /**
     * Returns when the deleted message was sent
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> messageTimestamp() {
        return Clock.parseSeconds(messageTimestampInSeconds);
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
}
