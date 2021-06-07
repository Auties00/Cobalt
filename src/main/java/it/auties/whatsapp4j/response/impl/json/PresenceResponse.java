package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

/**
 * A json model that contains information about an update regarding the presence of a contact in a chat
 *
 * @param jid                the jid of the contact
 * @param presence           the new presence for the chat
 * @param offsetFromLastSeen a nullable unsigned int that represents the offset in seconds since the last time contact was seen
 * @param participant        if the chat is a group, the participant this update regards
 */
public record PresenceResponse(@JsonProperty("id") @NotNull String jid,
                                     @JsonProperty("type") @NotNull ContactStatus presence,
                                     @JsonProperty("t") Long offsetFromLastSeen,
                                     String participant) implements JsonResponseModel {
}
