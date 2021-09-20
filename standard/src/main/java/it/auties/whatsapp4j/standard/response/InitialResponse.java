package it.auties.whatsapp4j.standard.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.response.JsonResponseModel;
import it.auties.whatsapp4j.standard.request.TakeOverRequest;

/**
 * A json model that contains information regarding a {@link TakeOverRequest}
 *
 * @param status   the http status code for the original request, 0 if challenge != null
 * @param ref      a token sent by Whatsapp to login, might be null
 * @param ttl      time to live in seconds for the ref
 * @param outdated determines whether the version of the client is outdated
 */
public final record InitialResponse(int status, String ref, int ttl, String latestVersion,
                                    boolean outdated, boolean unsupported) implements JsonResponseModel {

    /**
     * Constructs an initial response for a drastically outdated client version code
     *
     * @param type used by Whatsapp's WebSocket
     */
    @JsonCreator
    public static InitialResponse fromJson(@JsonProperty("status") int status, @JsonProperty("ref") String ref,
                                          @JsonProperty("ttl") int ttl, @JsonProperty("curr") String curr,
                                          @JsonProperty("update") boolean update, @JsonProperty("type") String type) {
        if (type == null) {
            return new InitialResponse(status, ref, ttl, curr, update, false);
        }

        return new InitialResponse(426, null, -1, type, false, true);
    }
}
