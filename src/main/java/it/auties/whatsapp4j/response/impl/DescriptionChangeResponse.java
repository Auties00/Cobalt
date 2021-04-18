package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;

/**
 * A json model that contains information about a change in a WhatsappGroup's description
 *
 * @param description the new description
 * @param descriptionId the id of the new description
 */
public record DescriptionChangeResponse(@JsonProperty("desc") @NotNull String description, @JsonProperty("descId") String descriptionId) implements JsonResponseModel {
}
