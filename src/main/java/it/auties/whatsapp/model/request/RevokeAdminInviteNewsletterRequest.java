package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;

public record RevokeAdminInviteNewsletterRequest(Variable variables) {
    public record Variable(@JsonProperty("newsletter_id") Jid jid, @JsonProperty("user_id") Jid admin) {

    }
}
