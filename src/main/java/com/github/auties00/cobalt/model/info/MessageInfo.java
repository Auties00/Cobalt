package com.github.auties00.cobalt.model.info;

import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.message.model.ContextualMessage;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.model.MessageStatus;

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

    Optional<MessageInfoStubType> stubType();

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