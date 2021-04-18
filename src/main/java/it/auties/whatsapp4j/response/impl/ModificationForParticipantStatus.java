package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * A json model that contains information about a modification made to a participant of a group
 *
 * @param jid    the jid of the participant
 * @param status the http status code for the original request
 */
public record ModificationForParticipantStatus(@NotNull String jid, int status) {
    @JsonCreator
    public ModificationForParticipantStatus(@NotNull Map<String, Map<String, Integer>> json) {
        this(new ArrayList<>(json.keySet()).get(0), json.get(new ArrayList<>(json.keySet()).get(0)).get("code"));
    }
}
