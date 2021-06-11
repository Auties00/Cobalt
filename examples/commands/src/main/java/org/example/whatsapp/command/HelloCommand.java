package org.example.whatsapp.command;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.MessageKey;
import it.auties.whatsapp4j.protobuf.message.TextMessage;

import java.util.Set;

public class HelloCommand implements Command{
    @Override
    public void onCommand(WhatsappAPI api, Chat chat, MessageInfo message) {
        var key = new MessageKey(chat);
        var context = new ContextInfo(message);
        var responseText = TextMessage.newTextMessage()
                .text("Hello :)")
                .contextInfo(context)
                .create();
        var responseMessage = new MessageContainer(responseText);
        var responseMessageInfo = new MessageInfo(key, responseMessage);
        api.sendMessage(responseMessageInfo);
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
