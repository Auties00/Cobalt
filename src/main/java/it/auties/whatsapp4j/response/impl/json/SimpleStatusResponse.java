package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record SimpleStatusResponse(int status) implements JsonResponseModel {
}
