package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;

public record DescriptionChangeResponse(@JsonProperty("desc") @NotNull String description, @JsonProperty("descId") String descriptionId) implements JsonResponseModel {
}
