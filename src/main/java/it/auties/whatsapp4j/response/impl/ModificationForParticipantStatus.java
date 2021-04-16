package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * A json model that contains information about a modification made to a participant of a group
 *
 */
public final class ModificationForParticipantStatus {
    private final @NotNull String jid;
    private final int status;

    /**
     * @param jid the jid of the participant
     * @param status the http status code for the original request
     */
    public ModificationForParticipantStatus(@NotNull String jid, int status) {
        this.jid = jid;
        this.status = status;
    }

    @JsonCreator
    public ModificationForParticipantStatus(@NotNull Map<String, Map<String, Integer>> json) {
        this(new ArrayList<>(json.keySet()).get(0), json.get(new ArrayList<>(json.keySet()).get(0)).get("code"));
    }

    public @NotNull String jid() {
        return jid;
    }

    public int status() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModificationForParticipantStatus) obj;
        return Objects.equals(this.jid, that.jid) &&
                this.status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, status);
    }

    @Override
    public String toString() {
        return "ModificationForParticipantStatus[" +
                "jid=" + jid + ", " +
                "status=" + status + ']';
    }

}
