package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
public record BlocklistResponse(int id, @JsonProperty("blocklist") List<String> blockedJids) implements JsonResponseModel {

}
