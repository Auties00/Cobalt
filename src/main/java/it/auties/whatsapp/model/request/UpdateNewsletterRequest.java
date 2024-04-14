package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;

public record UpdateNewsletterRequest(Variable variables) {
    public record Variable(@JsonProperty("newsletter_id") Jid jid, UpdatePayload updates) {

    }

    public record UpdatePayload(String description) {

    }
}
