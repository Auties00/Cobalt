package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.List;

/**
 * A json model that contains information about a modification made to a group
 * @param jid           the nullable jid of the group
 * @param status        the http status code for the original request
 * @param modifications a list of modifications made to the participants of the group and their relative status
 */
public record GroupModificationResponse(@JsonProperty("gid") String jid, int status,
                                        @JsonProperty("participants") List<ModificationForParticipantStatus> modifications) implements JsonResponseModel {
}
