package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

/**
 * A json model that contains information only about the http status code for the original request
 *
 * @param status the http status code for the original request
 */
public final record SimpleStatusResponse(int status) implements JsonResponseModel {
}
