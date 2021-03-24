package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Builder;

/**
 * A json model that contains information only about the http status code for the original request
 *
 * @param status the http status code for the original request
 */
public record SimpleStatusResponse(int status) implements JsonResponseModel {
}
