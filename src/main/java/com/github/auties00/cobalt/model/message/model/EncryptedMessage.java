package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.message.standard.EncryptedReactionMessage;
import com.github.auties00.cobalt.model.message.standard.PollUpdateMessage;

public sealed interface EncryptedMessage permits EncryptedReactionMessage, PollUpdateMessage {
    String secretName();
}
