package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A json model that contains a {@link WhatsappMediaConnection}, used to decrypt media files
 *
 * @param status     the http status code for the original request
 * @param connection an instance of the requested connection
 */
public final record MediaConnectionResponse(int status,
                                            @NotNull @JsonProperty("media_conn") WhatsappMediaConnection connection) implements JsonResponseModel {

}
