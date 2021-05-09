package org.example.whatsapp.bot;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappTextMessage;
import org.example.whatsapp.command.CommandManager;

@RegisterListener
public record WhatsappBotListener(WhatsappAPI api, CommandManager manager) implements WhatsappListener {
    @Override
    public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message) {
        if(!(message instanceof WhatsappTextMessage textMessage)){
            return;
        }

        manager.findCommand(textMessage.text()).ifPresent(command -> command.onCommand(api, chat, textMessage));
    }
}
