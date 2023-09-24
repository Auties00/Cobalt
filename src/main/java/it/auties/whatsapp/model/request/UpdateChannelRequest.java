package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.contact.ContactJid;

public record UpdateChannelRequest(Variable variables) {
    public record Variable(@JsonProperty("newsletter_id") ContactJid jid, UpdatePayload updates) {

    }

    public record UpdatePayload(String description) {

    }
}
