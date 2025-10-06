package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;

import java.util.Map;
import java.util.Set;

// TODO: Remove me
public sealed interface MessageRequest permits MessageRequest.Chat, MessageRequest.Newsletter {
    record Chat(ChatMessageInfo info, Set<Jid> recipients, boolean force, boolean peer,
                Map<String, ?> additionalAttributes) implements MessageRequest {
        public Chat(ChatMessageInfo info) {
            this(info, null, false, false, null);
        }

        public boolean hasRecipientOverride() {
            return recipients != null && !recipients.isEmpty();
        }
    }

    record Newsletter(NewsletterMessageInfo info, Map<String, ?> additionalAttributes) implements MessageRequest {
        public Newsletter(NewsletterMessageInfo info) {
            this(info, null);
        }
    }
}