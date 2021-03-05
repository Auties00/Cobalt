package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record ChatPictureResponse(int status, @JsonProperty("eurl") String url, String tag) implements JsonResponseModel {
}
