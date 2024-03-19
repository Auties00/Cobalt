package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Map;
import java.util.Set;

public sealed interface MessageSendRequest permits MessageSendRequest.Chat, MessageSendRequest.Newsletter {
    record Chat(ChatMessageInfo info, Set<Jid> recipients, boolean force, boolean peer,
                Map<String, ?> additionalAttributes) implements MessageSendRequest {
        public Chat(ChatMessageInfo info) {
            this(info, null, false, false, null);
        }

        public boolean hasRecipientOverride() {
            return recipients != null && !recipients.isEmpty();
        }
    }

    record Newsletter(NewsletterMessageInfo info, Map<String, ?> additionalAttributes) implements MessageSendRequest {
        public Newsletter(NewsletterMessageInfo info) {
            this(info, null);
        }
    }
}