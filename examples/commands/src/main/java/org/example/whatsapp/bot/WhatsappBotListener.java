package org.example.whatsapp.bot;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import org.example.whatsapp.command.CommandManager;

@RegisterListener
public record WhatsappBotListener(WhatsappAPI api, CommandManager manager) implements WhatsappListener {
    @Override
    public void onNewMessage(Chat chat, MessageInfo info) {
        var textMessage = info.container().textMessage();
        if(textMessage == null){
            return;
        }

        manager.findCommand(textMessage.text()).ifPresent(command -> command.onCommand(api, chat, info));
    }
}
