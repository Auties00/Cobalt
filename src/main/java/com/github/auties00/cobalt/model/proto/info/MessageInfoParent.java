package com.github.auties00.cobalt.model.proto.info;

import com.github.auties00.cobalt.model.proto.chat.Chat;
import com.github.auties00.cobalt.model.proto.jid.JidProvider;
import com.github.auties00.cobalt.model.proto.newsletter.Newsletter;

import java.util.Optional;
import java.util.SequencedCollection;

public sealed interface MessageInfoParent
        extends JidProvider
        permits Chat, Newsletter {
    SequencedCollection<? extends MessageInfo> messages();
    boolean removeMessage(String messageId);
    void removeMessages();
    Optional<? extends MessageInfo> newestMessage();
    Optional<? extends MessageInfo> oldestMessage();
}
