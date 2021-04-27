package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * A json model that contains information about the text status of a contact
 *
 * @param content if the contact has a status a non null String, otherwise an unsigned int representing the http status code for the original request
 */
public record UserStatusResponse(@JsonProperty("status") Object content) implements JsonResponseModel {
    /**
     * Returns the text status of a contact
     * @return a non null optional if the contact has a text status, otherwise an empty optional
     */
    public @NotNull Optional<String> status(){
        return content instanceof Integer ? Optional.empty() : Optional.of((String) content);
    }
}
