package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class GroupActionResponse implements JsonResponseModel {
    private final @NotNull List<String> participants;

    public GroupActionResponse(@NotNull List<String> participants) {
        this.participants = participants;
    }

}
