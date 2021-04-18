package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.request.impl.TakeOverRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;


/**
 * A json model that contains information regarding a {@link TakeOverRequest}
 * @param status the http status code for the original request, 0 if challenge != null
 * @param ref    a token sent by Whatsapp to login, might be null
 * @param ttl    time to live in seconds for the ref
 */
public record InitialResponse(int status, String ref, int ttl) implements JsonResponseModel {
}
