package com.github.auties00.cobalt.model.info;

import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.newsletter.Newsletter;

public sealed interface MessageInfoParent permits Chat, Newsletter {
    Jid jid();
}
