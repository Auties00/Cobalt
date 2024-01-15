package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.jid.Jid;

public record NewsletterSubscribersRequest(Variable variables) {
    public record Variable(Input input) {

    }

    public record Input(Jid key, String type, String role) {

    }
}
