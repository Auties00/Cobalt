package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;


import java.time.Instant;
import java.util.Objects;

/**
 * A json model that contains information about a WhatsappMessage sent by the client
 *
 */
public final class MessageResponse implements JsonResponseModel<MessageResponse> {
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

    public int status() {
        return status;
    }

    @JsonProperty("t")
    public Long timeStamp() {
        return timeStamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MessageResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.timeStamp, that.timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, timeStamp);
    }

    @Override
    public String toString() {
        return "MessageResponse[" +
                "status=" + status + ", " +
                "timeStamp=" + timeStamp + ']';
    }

}
