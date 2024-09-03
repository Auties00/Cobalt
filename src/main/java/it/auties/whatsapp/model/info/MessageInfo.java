package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.Json;

import java.util.OptionalLong;

public sealed interface MessageInfo<T extends MessageInfo<T>> extends Info permits ChatMessageInfo, NewsletterMessageInfo, MessageStatusInfo, QuotedMessageInfo {
    Jid parentJid();

    Jid senderJid();

    String id();

    MessageContainer message();

    T setMessage(MessageContainer message);

    OptionalLong timestampSeconds();

    default String toJson() {
        return Json.writeValueAsString(this, true);
    }
}