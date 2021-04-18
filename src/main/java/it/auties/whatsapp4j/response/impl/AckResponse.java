package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;


/**
 * A json model that contains information regarding an update about the read status of a message
 * @param cmd         a nullable identifier for the request
 * @param ids         a non null array of message ids that this update regards
 * @param ack         an unsigned int representing {@link WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus}
 * @param from        the sender of the messages that this update regards
 * @param to          chat of the messages that this update regards
 * @param timestamp   the time in seconds since {@link Instant#EPOCH} when the update was dispatched by the server
 * @param participant if {@code to} is a group, the participant that this update regards
 */
public record AckResponse(String cmd,
                          @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) @JsonProperty("id") String[] ids,
                          int ack, @NotNull String from, @NotNull String to,
                          @JsonProperty("t") int timestamp, String participant) implements JsonResponseModel {
}
