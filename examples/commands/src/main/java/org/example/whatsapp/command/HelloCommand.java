package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.model.WhatsappTextMessage;
import it.auties.whatsapp4j.response.impl.json.ModificationForParticipant;

import java.util.Set;

public class HelloCommand implements Command{
    @Override
    public void onCommand(WhatsappAPI api, WhatsappChat chat, WhatsappTextMessage message) {
        api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "Hello :)", message));
    }

    @Override
    public String command() {
        return "/hello";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("/hi", "/morning");
    }
}
