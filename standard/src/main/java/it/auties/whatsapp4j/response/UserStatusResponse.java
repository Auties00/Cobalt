package it.auties.whatsapp4j.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.response.JsonResponseModel;
import lombok.NonNull;

import java.util.Optional;

/**
 * A json model that contains information about the text status of a contact
 *
 * @param content if the contact has a status a non-null String, otherwise an unsigned int representing the http status code for the original request
 */
public final record UserStatusResponse(@JsonProperty("status") Object content) implements JsonResponseModel {
    /**
     * Returns the text status of a contact
     *
     * @return a non-null optional if the contact has a text status, otherwise an empty optional
     */
    public @NonNull Optional<String> status() {
        return switch (content){
            case Integer ignored, null -> Optional.empty();
            case String string -> Optional.of(string);
            default -> throw new IllegalArgumentException("Cannot determine status, invalid type: %s".formatted(content.getClass().getName()));
        };
    }
}
