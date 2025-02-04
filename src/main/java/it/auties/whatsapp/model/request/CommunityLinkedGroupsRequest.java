package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;

public record CommunityLinkedGroupsRequest(Variable variables) {
    public record Variable(Input input) {

    }

    public record Input(@JsonProperty("group_jid") Jid groupJid, @JsonProperty("query_context") String context) {

    }
}
