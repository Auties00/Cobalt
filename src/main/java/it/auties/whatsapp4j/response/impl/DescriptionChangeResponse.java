package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class DescriptionChangeResponse implements JsonResponseModel {
    @JsonProperty("desc")
    private final @NotNull String description;
    @JsonProperty("descId")
    private final String descriptionId;

    public DescriptionChangeResponse(@JsonProperty("desc") @NotNull String description, @JsonProperty("descId") String descriptionId) {
        this.description = description;
        this.descriptionId = descriptionId;
    }
}
