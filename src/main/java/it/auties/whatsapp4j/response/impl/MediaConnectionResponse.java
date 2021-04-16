package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class MediaConnectionResponse implements JsonResponseModel<MediaConnectionResponse> {
    private final int status;
    @JsonProperty("media_conn")
    private final @NotNull WhatsappMediaConnection connection;

    public MediaConnectionResponse(int status, @NotNull @JsonProperty("media_conn") WhatsappMediaConnection connection) {
        this.status = status;
        this.connection = connection;
    }

    public int status() {
        return status;
    }

    @JsonProperty("media_conn")
    public @NotNull WhatsappMediaConnection connection() {
        return connection;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MediaConnectionResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, connection);
    }

    @Override
    public String toString() {
        return "MediaConnectionResponse[" +
                "status=" + status + ", " +
                "connection=" + connection + ']';
    }


}
