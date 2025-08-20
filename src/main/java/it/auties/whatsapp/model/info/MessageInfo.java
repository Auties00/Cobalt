package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageStatus;

import java.util.Optional;
import java.util.OptionalLong;

public sealed interface MessageInfo
        extends Info
        permits ChatMessageInfo, NewsletterMessageInfo, QuotedMessageInfo {
    MessageStatus status();
    void setStatus(MessageStatus status);

    OptionalLong timestampSeconds();

    String id();

    Jid parentJid();
    Optional<MessageInfoParent> parent();
    void setParent(MessageInfoParent parent);

    Jid senderJid();
    Optional<Contact> sender();
    void setSender(Contact sender);

    MessageContainer message();
    void setMessage(MessageContainer message);

    default Optional<QuotedMessageInfo> quotedMessage() {
        var message = message();
        if(message == null) {
            return Optional.empty();
        }

        return message.contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(QuotedMessageInfo::of);
    }
}