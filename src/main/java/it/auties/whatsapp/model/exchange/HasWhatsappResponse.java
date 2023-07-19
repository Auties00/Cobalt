package it.auties.whatsapp.model.exchange;

import it.auties.whatsapp.model.contact.ContactJid;
import lombok.NonNull;

import java.util.NoSuchElementException;

public record HasWhatsappResponse(@NonNull ContactJid contact, boolean hasWhatsapp) implements ResponseWrapper {
    public HasWhatsappResponse(@NonNull Node source) {
        this(source.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid in HasWhatsappResponse")), source.findNode("contact")
                .orElseThrow(() -> new NoSuchElementException("Missing contact in HasWhatsappResponse"))
                .attributes()
                .getRequiredString("type")
                .equals("in"));
    }
}
