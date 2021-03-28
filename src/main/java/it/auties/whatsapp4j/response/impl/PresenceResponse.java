package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappContactStatus;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A json model that contains information about an update regarding the presence of a contact in a chat
 *
 * @param jid the jid of the contact
 * @param presence the new presence for the chat
 * @param offsetFromLastSeen a nullable unsigned int that represents the offset in seconds since the last time contact was seen
 * @param participant if the chat is a group, the participant this update regards
 */
public record PresenceResponse(@NotNull @JsonProperty("id") String jid,
                               @NotNull @JsonProperty("type") WhatsappContactStatus presence,
                               @Nullable @JsonProperty("t") Long offsetFromLastSeen,
                               @Nullable String participant) implements JsonResponseModel {
}
