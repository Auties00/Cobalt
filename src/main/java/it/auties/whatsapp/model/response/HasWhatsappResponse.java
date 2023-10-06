package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.jid.Jid;
import org.checkerframework.checker.nullness.qual.NonNull;

public record HasWhatsappResponse(@NonNull Jid contact, boolean hasWhatsapp) {

}
