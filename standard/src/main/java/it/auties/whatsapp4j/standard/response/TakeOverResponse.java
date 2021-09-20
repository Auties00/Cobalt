package it.auties.whatsapp4j.standard.response;

import it.auties.whatsapp4j.common.response.JsonResponseModel;
import it.auties.whatsapp4j.standard.request.TakeOverRequest;

/**
 * A json model that contains information regarding a {@link TakeOverRequest}
 *
 * @param status    the http status code for the original request, 0 if challenge != null
 * @param ref       a token sent by Whatsapp to login, null if status != 200
 * @param challenge a token sent by Whatsapp to verify ownership of the credentials, null if status != 0
 * @param ttl       time to live in seconds for the ref, 0 if status != 200
 */
public final record TakeOverResponse(int status, String ref, String challenge, int ttl) implements JsonResponseModel {
}
