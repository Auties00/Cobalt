package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Map;

/**
 * A json model that contains information about a modification made to a participant of a group
 *
 * @param jid    the jid of the participant
 * @param status the status of the modification
 */
public record ModificationForParticipant(@NotNull String jid, @NotNull ModificationForParticipantStatus status) {
    @JsonCreator
    public ModificationForParticipant(@NotNull Map<String, ModificationForParticipantStatus> json) {
        this(new ArrayList<>(json.keySet()).get(0), json.get(new ArrayList<>(json.keySet()).get(0)));
    }
}
