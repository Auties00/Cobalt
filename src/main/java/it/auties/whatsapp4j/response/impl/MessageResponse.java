package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;


import java.time.Instant;
import java.util.Objects;

/**
 * A json model that contains information about a WhatsappMessage sent by the client
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class MessageResponse implements JsonResponseModel {
    private final int status;
    @JsonProperty("t")
    private final Long timeStamp;

    /**
     * @param status the http status code for the original request
     * @param timeStamp the time in seconds since {@link Instant#EPOCH} when the message was received by the server, null if the request wasn't successfully
     */
    public MessageResponse(int status, @JsonProperty("t") Long timeStamp) {
        this.status = status;
        this.timeStamp = timeStamp;
    }
}
