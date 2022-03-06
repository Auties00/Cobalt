package org.example.whatsapp.bot;

import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;
import org.example.whatsapp.command.CommandManager;

@RegisterListener
public record WhatsappBotListener(Whatsapp whatsapp) implements WhatsappListener {
    @Override
    public void onNewMessage(MessageInfo info) {
        if(!(info.message().content() instanceof TextMessage textMessage)){
            return;
        }

        CommandManager.instance()
                .findCommand(textMessage.text())
                .ifPresent(command -> command.onCommand(whatsapp, info));
    }
}
