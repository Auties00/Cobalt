package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public interface Command {
    void onCommand(@NotNull WhatsappAPI api, @NotNull Chat chat, @NotNull WhatsappTextMessage message);
    String command();
    Set<String> aliases();
}
