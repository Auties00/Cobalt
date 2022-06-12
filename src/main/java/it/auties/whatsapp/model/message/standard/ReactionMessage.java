package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a message holding an emoji reaction inside
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ReactionMessage implements Message {
    /**
     * The key of the quoted message
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageKey.class)
    private MessageKey key;

    /**
     * The operation as text
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String text;

    /**
     * The grouping key
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String groupingKey;

    /**
     * The timestamp of this message in milliseconds
     */
    @ProtobufProperty(index = 4, type = INT64)
    private long timestamp;

    /**
     * Whether this message is marked as read or not
     */
    @ProtobufProperty(index = 5, type = BOOLEAN)
    private boolean unread;
}
