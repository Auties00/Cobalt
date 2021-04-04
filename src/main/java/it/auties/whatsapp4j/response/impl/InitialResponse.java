package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A json model that contains information regarding a {@link it.auties.whatsapp4j.request.impl.TakeOverRequest}
 *
 * @param status the http status code for the original request, 0 if challenge != null
 * @param ref a token sent by Whatsapp to login, might be null
 * @param ttl time to live in seconds for the ref
 */
public record InitialResponse(int status, @Nullable String ref, int ttl) implements JsonResponseModel{
}
