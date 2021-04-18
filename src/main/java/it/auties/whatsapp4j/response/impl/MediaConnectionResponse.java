package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class MediaConnectionResponse implements JsonResponseModel {
    private final int status;
    @JsonProperty("media_conn")
    private final @NotNull WhatsappMediaConnection connection;

    public MediaConnectionResponse(int status, @NotNull @JsonProperty("media_conn") WhatsappMediaConnection connection) {
        this.status = status;
        this.connection = connection;
    }
}
