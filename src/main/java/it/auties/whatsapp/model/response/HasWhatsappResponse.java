package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.jid.Jid;

public record HasWhatsappResponse(Jid contact, boolean hasWhatsapp) {

}
