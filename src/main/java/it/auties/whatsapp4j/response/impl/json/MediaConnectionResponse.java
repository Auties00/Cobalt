package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.media.MediaConnection;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.NonNull;

/**
 * A json model that contains a {@link MediaConnection}, used to decrypt media files
 *
 * @param status     the http status code for the original request
 * @param connection an instance of the requested connection
 */
public final record MediaConnectionResponse(int status,
                                            @NonNull @JsonProperty("media_conn") MediaConnection connection) implements JsonResponseModel {
}
