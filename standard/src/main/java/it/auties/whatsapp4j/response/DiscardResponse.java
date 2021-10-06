package it.auties.whatsapp4j.response;

import it.auties.whatsapp4j.common.response.JsonResponseModel;

/**
 * A json model that ignores any data provided by a response
 */
public final record DiscardResponse() implements JsonResponseModel {
}
