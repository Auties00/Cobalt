package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record SimpleStatusResponse(int status) implements JsonResponseModel {
}
