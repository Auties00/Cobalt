package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappContactStatus;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;


/**
 * A json model that contains information about an update regarding the presence of a contact in a chat
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class PresenceResponse implements JsonResponseModel {
    @JsonProperty("id")
    private final @NotNull String jid;
    @JsonProperty("type")
    private final @NotNull WhatsappContactStatus presence;
    @JsonProperty("t")
    private final Long offsetFromLastSeen;
    private final String participant;

    /**
     * @param jid the jid of the contact
     * @param presence the new presence for the chat
     * @param offsetFromLastSeen a nullable unsigned int that represents the offset in seconds since the last time contact was seen
     * @param participant if the chat is a group, the participant this update regards
     */
    public PresenceResponse(@NotNull @JsonProperty("id") String jid,
                            @NotNull @JsonProperty("type") WhatsappContactStatus presence,
                            @JsonProperty("t") Long offsetFromLastSeen,
                            String participant) {
        this.jid = jid;
        this.presence = presence;
        this.offsetFromLastSeen = offsetFromLastSeen;
        this.participant = participant;
    }
}
