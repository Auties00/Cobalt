package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

/**
 * A json model that ignores any data provided by a response
 */
public record DiscardResponse() implements JsonResponseModel {

}
