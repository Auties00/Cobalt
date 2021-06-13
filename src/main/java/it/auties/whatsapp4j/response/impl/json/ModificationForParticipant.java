package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Map;

/**
 * A json model that contains information about a modification made to a participant of a group
 *
 * @param jid    the jid of the participant
 * @param status the status of the modification
 */
public record ModificationForParticipant(@NonNull String jid, @NonNull ModificationForParticipantStatus status) {
    @JsonCreator
    public ModificationForParticipant(@NonNull Map<String, ModificationForParticipantStatus> json) {
        this(new ArrayList<>(json.keySet()).get(0), json.get(new ArrayList<>(json.keySet()).get(0)));
    }
}
