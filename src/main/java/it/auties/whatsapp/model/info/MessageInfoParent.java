package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.Newsletter;

public sealed interface MessageInfoParent permits Chat, Newsletter {
    Jid jid();
}
