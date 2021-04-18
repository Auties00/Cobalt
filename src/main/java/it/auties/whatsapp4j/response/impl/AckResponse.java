package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Objects;


/**
 * A json model that contains information regarding an update about the read status of a message
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class AckResponse implements JsonResponseModel {
    private final String cmd;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("id")
    private final String @NotNull [] ids;
    private final int ack;
    private final @NotNull String from;
    private final @NotNull String to;
    @JsonProperty("t")
    private final int timestamp;
    private final String participant;

    /**
     * @param cmd a nullable identifier for the request
     * @param ids a non null array of message ids that this update regards
     * @param ack an unsigned int representing {@link WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus}
     * @param from the sender of the messages that this update regards
     * @param to chat of the messages that this update regards
     * @param timestamp the time in seconds since {@link Instant#EPOCH} when the update was dispatched by the server
     * @param participant if {@code to} is a group, the participant that this update regards
     */
    public AckResponse(String cmd,
                       @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) @JsonProperty("id") String[] ids,
                       int ack, @NotNull String from, @NotNull String to, @JsonProperty("t") int timestamp,
                       String participant) {
        this.cmd = cmd;
        this.ids = ids;
        this.ack = ack;
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
        this.participant = participant;
    }

}
