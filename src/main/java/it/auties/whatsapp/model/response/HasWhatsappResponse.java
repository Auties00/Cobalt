package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.node.Node;

import java.util.NoSuchElementException;

public final class HasWhatsappResponse {
    private final Jid contact;
    private final boolean hasWhatsapp;

    private HasWhatsappResponse(Jid contact, boolean hasWhatsapp) {
        this.contact = contact;
        this.hasWhatsapp = hasWhatsapp;
    }

    public static HasWhatsappResponse ofNode(Node node) {
        var jid = node.attributes()
                .getRequiredJid("jid");
        var in = node.findChild("contact")
                .orElseThrow(() -> new NoSuchElementException("Missing contact in HasWhatsappResponse"))
                .attributes()
                .getRequiredString("type")
                .equals("in");
        return new HasWhatsappResponse(jid, in);
    }

    public Jid contact() {
        return contact;
    }

    public boolean hasWhatsapp() {
        return hasWhatsapp;
    }
}
