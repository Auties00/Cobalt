package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

public record SubjectChangeResponse(@NotNull String subject, @JsonProperty("s_t") long timestamp, @JsonProperty("s_o") @NotNull String authorJid) implements JsonResponseModel {
}
