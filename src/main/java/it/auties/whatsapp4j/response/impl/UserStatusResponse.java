package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * A json model that contains information about the text status of a contact
 */
public class UserStatusResponse implements JsonResponseModel {
    /**
     * The textual status of a contact or the http status code for the original request
     */
    @JsonProperty("status")
    private Object status;

    /**
     * Returns the text status of a contact
     * @return a non null optional if the contact has a text status, otherwise an empty optional
     */
    public @NotNull Optional<String> status(){
        return status instanceof Integer ? Optional.empty() : Optional.of((String) status);
    }
}
