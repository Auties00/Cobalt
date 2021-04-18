package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

public record MediaConnectionResponse(int status, @NotNull @JsonProperty("media_conn") WhatsappMediaConnection connection) implements JsonResponseModel {

}
