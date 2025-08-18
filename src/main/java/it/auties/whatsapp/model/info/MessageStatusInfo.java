package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.message.model.MessageStatus;

public sealed interface MessageStatusInfo extends Info, MessageInfo permits ChatMessageInfo, NewsletterMessageInfo {
    MessageStatus status();

    void setStatus(MessageStatus status);
}
