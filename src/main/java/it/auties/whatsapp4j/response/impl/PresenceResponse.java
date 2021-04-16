package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappContactStatus;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;


/**
 * A json model that contains information about an update regarding the presence of a contact in a chat
 *
 */
public final class PresenceResponse implements JsonResponseModel<PresenceResponse> {
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

    @JsonProperty("id")
    public @NotNull String jid() {
        return jid;
    }

    @JsonProperty("type")
    public @NotNull WhatsappContactStatus presence() {
        return presence;
    }

    @JsonProperty("t")
    public Long offsetFromLastSeen() {
        return offsetFromLastSeen;
    }

    public String participant() {
        return participant;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PresenceResponse) obj;
        return Objects.equals(this.jid, that.jid) &&
                Objects.equals(this.presence, that.presence) &&
                Objects.equals(this.offsetFromLastSeen, that.offsetFromLastSeen) &&
                Objects.equals(this.participant, that.participant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, presence, offsetFromLastSeen, participant);
    }

    @Override
    public String toString() {
        return "PresenceResponse[" +
                "jid=" + jid + ", " +
                "presence=" + presence + ", " +
                "offsetFromLastSeen=" + offsetFromLastSeen + ", " +
                "participant=" + participant + ']';
    }

}
