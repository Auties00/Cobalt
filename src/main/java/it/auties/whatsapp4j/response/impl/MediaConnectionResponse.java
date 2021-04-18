package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;

/**
 * A json model that contains a {@link WhatsappMediaConnection}, used to decrypt media files
 *
 * @param status the http status code for the original request
 * @param connection an instance of the requested connection
 */
public record MediaConnectionResponse(int status, @NotNull @JsonProperty("media_conn") WhatsappMediaConnection connection) implements JsonResponseModel {

}
