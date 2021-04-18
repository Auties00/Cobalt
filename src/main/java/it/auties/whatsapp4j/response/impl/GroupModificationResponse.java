package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;


import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about a modification made to a group
 *
 */
public final class GroupModificationResponse implements JsonResponseModel {
    @JsonProperty("gid")
    private final String jid;
    private final int status;
    @JsonProperty("participants")
    private final List<ModificationForParticipantStatus> modifications;

    /**
     * @param jid the nullable jid of the group
     * @param status the http status code for the original request
     * @param modifications a list of modifications made to the participants of the group and their relative status
     */
    public GroupModificationResponse(@JsonProperty("gid") String jid, int status, @JsonProperty("participants") List<ModificationForParticipantStatus> modifications) {
        this.jid = jid;
        this.status = status;
        this.modifications = modifications;
    }

    @JsonProperty("gid")
    public String jid() {
        return jid;
    }

    public int status() {
        return status;
    }

    @JsonProperty("participants")
    public List<ModificationForParticipantStatus> modifications() {
        return modifications;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupModificationResponse) obj;
        return Objects.equals(this.jid, that.jid) &&
                this.status == that.status &&
                Objects.equals(this.modifications, that.modifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, status, modifications);
    }

    @Override
    public String toString() {
        return "GroupModificationResponse[" +
                "jid=" + jid + ", " +
                "status=" + status + ", " +
                "modifications=" + modifications + ']';
    }


}
