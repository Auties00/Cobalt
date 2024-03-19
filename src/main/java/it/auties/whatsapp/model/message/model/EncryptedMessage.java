package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.standard.EncryptedReactionMessage;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;

public sealed interface EncryptedMessage permits EncryptedReactionMessage, PollUpdateMessage {
    String secretName();
}
