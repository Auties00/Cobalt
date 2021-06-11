package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public interface Command {
    void onCommand(@NotNull WhatsappAPI api, @NotNull Chat chat, @NotNull MessageInfo message);
    String command();
    Set<String> aliases();
}
