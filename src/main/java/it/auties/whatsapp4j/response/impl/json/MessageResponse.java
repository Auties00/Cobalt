package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record MessageResponse(int status, @JsonProperty("t") long timeStamp) implements JsonResponseModel {
}
