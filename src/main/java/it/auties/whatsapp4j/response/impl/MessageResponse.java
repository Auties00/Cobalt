package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;


import java.time.Instant;

/**
 * A json model that contains information about a WhatsappMessage sent by the client
 *
 * @param status the http status code for the original request
 * @param timeStamp the time in seconds since {@link Instant#EPOCH} when the message was received by the server, null if the request wasn't successfully
 */
public record MessageResponse(int status,  @JsonProperty("t") Long timeStamp) implements JsonResponseModel {
}
