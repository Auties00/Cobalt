package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

/**
 * A json model that ignores any data provided by a response
 */
public record DiscardResponse() implements JsonResponseModel {
}
