package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model that represents the receipt for a message
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("UserReceipt")
public class MessageReceipt implements ProtobufMessage {
    /**
     * When the message was delivered(two ticks)
     */
    @ProtobufProperty(index = 2, type = INT64)
    private Long deliveredTimestampSeconds;

    /**
     * When the message was read(two blue ticks)
     */
    @ProtobufProperty(index = 3, type = INT64)
    private Long readTimestampSeconds;

    /**
     * When the message was played(two blue ticks)
     */
    @ProtobufProperty(index = 4, type = INT64)
    private Long playedTimestampSeconds;

    /**
     * A list of contacts who received the message(two ticks)
     */
    @ProtobufProperty(index = 5, type = STRING, repeated = true, implementation = ContactJid.class)
    @Default
    private List<ContactJid> deliveredJids = new ArrayList<>();

    /**
     * A list of contacts who read the message(two blue ticks)
     */
    @ProtobufProperty(index = 6, type = STRING, repeated = true, implementation = ContactJid.class)
    @Default
    private List<ContactJid> readJids = new ArrayList<>();

    /**
     * Returns a default message receipt
     *
     * @return a non-null instance
     */
    public static MessageReceipt of() {
        return MessageReceipt.builder().build();
    }

    /**
     * Returns the date when the message was delivered
     *
     * @return a non-null optional
     */
    public ZonedDateTime deliveredTimestamp() {
        return Clock.parseSeconds(deliveredTimestampSeconds);
    }

    /**
     * Returns the date when the message was delivered
     *
     * @return a non-null optional
     */
    public ZonedDateTime readTimestamp() {
        return Clock.parseSeconds(readTimestampSeconds);
    }

    /**
     * Returns the date when the message was played
     *
     * @return a non-null optional
     */
    public ZonedDateTime playedTimestamp() {
        return Clock.parseSeconds(playedTimestampSeconds);
    }

    /**
     * Sets the read timestamp
     *
     * @param readTimestampSeconds the timestamp
     * @return the same instance
     */
    public MessageReceipt readTimestampSeconds(long readTimestampSeconds) {
        if (deliveredTimestampSeconds == null) {
            this.deliveredTimestampSeconds = readTimestampSeconds;
        }
        this.readTimestampSeconds = readTimestampSeconds;
        return this;
    }

    /**
     * Sets the played timestamp
     *
     * @param playedTimestampSeconds the timestamp
     * @return the same instance
     */
    public MessageReceipt playedTimestampSeconds(long playedTimestampSeconds) {
        if (deliveredTimestampSeconds == null) {
            this.deliveredTimestampSeconds = playedTimestampSeconds;
        }
        if (readTimestampSeconds == null) {
            this.readTimestampSeconds = playedTimestampSeconds;
        }
        this.playedTimestampSeconds = playedTimestampSeconds;
        return this;
    }
}