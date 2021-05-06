package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * A json model that contains information about the invite code of a group
 *
 * @param status the http status code for the original request
 * @param code if code == 200, the requested invite code
 */
public final record GroupInviteCodeResponse(int status, String code) implements JsonResponseModel {

}
