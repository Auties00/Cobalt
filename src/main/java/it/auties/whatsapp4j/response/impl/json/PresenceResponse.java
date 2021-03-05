package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.model.WhatsappContactStatus;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PresenceResponse(@JsonProperty("id") String jid,
                               @JsonProperty("type") WhatsappContactStatus presence,
                               @JsonProperty("t") Long offsetFromLastSeen,
                               String participant) implements JsonResponseModel {
}
