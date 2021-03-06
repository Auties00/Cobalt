package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
public final record BlocklistResponse(int id,
                                      @JsonProperty("blocklist") List<String> blockedJids) implements JsonResponseModel {
}
