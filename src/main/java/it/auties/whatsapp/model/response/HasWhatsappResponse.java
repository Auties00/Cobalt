package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.contact.ContactJid;
import org.checkerframework.checker.nullness.qual.NonNull;

public record HasWhatsappResponse(@NonNull ContactJid contact, boolean hasWhatsapp) implements ResponseWrapper {

}
