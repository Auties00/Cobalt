package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import lombok.NonNull;

import java.util.Set;

public interface Command {
    void onCommand(@NonNull WhatsappAPI api, @NonNull Chat chat, @NonNull MessageInfo message);
    String command();
    Set<String> aliases();
}
