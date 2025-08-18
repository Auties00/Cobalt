package it.auties.whatsapp.model.info;

import it.auties.whatsapp.model.message.model.MessageStatus;

public sealed interface MessageStatusInfo<T extends MessageStatusInfo<T>> extends Info, MessageInfo<T> permits ChatMessageInfo, NewsletterMessageInfo {
    MessageStatus status();

    T setStatus(MessageStatus status);
}
