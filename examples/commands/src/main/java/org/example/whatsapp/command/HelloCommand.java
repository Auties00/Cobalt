package org.example.whatsapp.command;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.NonNull;

import java.util.Set;

public class HelloCommand implements Command {
    @Override
    public void onCommand(@NonNull Whatsapp api, @NonNull MessageInfo message) {
        api.sendMessage(message.chatJid(), "Hello :)", message);
    }

    @Override
    public String command() {
        return "/hello";
    }

    @Override
    public Set<String> alias() {
        return Set.of("/hi", "/morning");
    }
}
