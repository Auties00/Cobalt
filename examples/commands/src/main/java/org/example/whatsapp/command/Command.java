package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappTextMessage;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public interface Command {
    void onCommand(@NotNull WhatsappAPI api, @NotNull WhatsappChat chat, @NotNull WhatsappTextMessage message);
    String command();
    Set<String> aliases();
}
