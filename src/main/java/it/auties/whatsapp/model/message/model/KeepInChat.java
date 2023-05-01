package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents an ephemeral message that was saved manually by the user in a chat
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("KeepInChat")
public class KeepInChat implements ProtobufMessage {
    /**
     * The type of this action
     */
    @ProtobufProperty(index = 1, name = "keepType", type = MESSAGE)
    private KeepInChatType keepType;

    /**
     * The timestamp of this action
     */
    @ProtobufProperty(index = 2, name = "serverTimestamp", type = INT64)
    private long serverTimestampSeconds;

    /**
     * The key of the message that was saved
     */
    @ProtobufProperty(index = 3, name = "key", type = MESSAGE)
    private MessageKey key;

    /**
     * The jid of the device that saved this message
     */
    @ProtobufProperty(index = 4, name = "deviceJid", type = STRING)
    private ContactJid deviceJid;

    /**
     * The timestamp for the client in milliseconds
     */
    @ProtobufProperty(index = 5, name = "clientTimestampMs", type = INT64)
    private long clientTimestampInMilliseconds;

    /**
     * The timestamp for the server in milliseconds
     */
    @ProtobufProperty(index = 6, name = "serverTimestampMs", type = INT64)
    private long serverTimestampMilliseconds;

    /**
     * The server timestamp if present
     *
     * @return a non-null optional
     */
    public ZonedDateTime serverTimestamp(){
        return Clock.parseSeconds(serverTimestampSeconds);
    }

    /**
     * The client timestamp if present
     *
     * @return a non-null optional
     */
    public ZonedDateTime clientTimestamp(){
        return Clock.parseMilliseconds(clientTimestampInMilliseconds);
    }
}