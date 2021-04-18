package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;


import java.util.List;
import java.util.Objects;

/**
 * A json model that contains information about a modification made to a group
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
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
}
