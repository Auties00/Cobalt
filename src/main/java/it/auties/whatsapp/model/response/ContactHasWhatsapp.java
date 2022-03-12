package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;

import java.util.NoSuchElementException;

public record ContactHasWhatsapp(@NonNull Node response, @NonNull ContactJid contact, boolean hasWhatsapp) implements ResponseWrapper {
    public ContactHasWhatsapp(@NonNull Node source) {
        this(
                source,
                source.attributes().getJid("jid").orElseThrow(() -> new NoSuchElementException("Missing jid in response")),
                source.findNode("contact").attributes().getString("type").equals("in"));
    }
}
