package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageContainer;

import java.util.Optional;
import java.util.OptionalLong;

public sealed interface MessageInfo extends Info permits ChatMessageInfo, NewsletterMessageInfo, MessageStatusInfo, QuotedMessageInfo {
    Jid parentJid();

    Jid senderJid();

    String id();

    MessageContainer message();

    default Optional<QuotedMessageInfo> quotedMessage() {
        var message = message();
        if(message == null) {
            return Optional.empty();
        }

        return message.contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(QuotedMessageInfo::of);
    }

    void setMessage(MessageContainer message);

    OptionalLong timestampSeconds();
}