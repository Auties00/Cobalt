package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class DescriptionChangeResponse implements JsonResponseModel {
    @JsonProperty("desc")
    private final @NotNull String description;
    @JsonProperty("descId")
    private final String descriptionId;

    public DescriptionChangeResponse(@JsonProperty("desc") @NotNull String description, @JsonProperty("descId") String descriptionId) {
        this.description = description;
        this.descriptionId = descriptionId;
    }

    @JsonProperty("desc")
    public @NotNull String description() {
        return description;
    }

    @JsonProperty("descId")
    public String descriptionId() {
        return descriptionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DescriptionChangeResponse) obj;
        return Objects.equals(this.description, that.description) &&
                Objects.equals(this.descriptionId, that.descriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, descriptionId);
    }

    @Override
    public String toString() {
        return "DescriptionChangeResponse[" +
                "description=" + description + ", " +
                "descriptionId=" + descriptionId + ']';
    }

}
