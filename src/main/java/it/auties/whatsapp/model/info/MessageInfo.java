package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.Json;

public sealed interface MessageInfo extends Info permits ChatMessageInfo, NewsletterMessageInfo, MessageStatusInfo, QuotedMessageInfo {
    Jid parentJid();

    Jid senderJid();

    String id();

    MessageContainer message();

    default String toJson() {
        return Json.writeValueAsString(this, true);
    }
}