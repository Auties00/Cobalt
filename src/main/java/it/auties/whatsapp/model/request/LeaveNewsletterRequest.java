package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;

// {"data":{"xwa2_newsletter_delete_v2":{"id":"120363182769055130@newsletter","newsletter_state":{"type":"DELETED"}}}}]]]
public record LeaveNewsletterRequest(Variable variables) {
    public record Variable(@JsonProperty("newsletter_id") Jid jid, UpdatePayload updates) {

    }

    public record UpdatePayload(String description) {

    }
}
