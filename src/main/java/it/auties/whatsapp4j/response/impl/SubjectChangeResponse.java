package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class SubjectChangeResponse implements JsonResponseModel {
    private final @NotNull String subject;
    @JsonProperty("s_t")
    private final long timestamp;
    @JsonProperty("s_o")
    private final @NotNull String authorJid;

}
