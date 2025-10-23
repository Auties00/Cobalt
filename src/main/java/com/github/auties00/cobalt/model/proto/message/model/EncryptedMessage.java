package com.github.auties00.cobalt.model.proto.message.model;

import com.github.auties00.cobalt.model.proto.message.standard.EncryptedReactionMessage;
import com.github.auties00.cobalt.model.proto.message.standard.PollUpdateMessage;

public sealed interface EncryptedMessage permits EncryptedReactionMessage, PollUpdateMessage {
    String secretName();
}
