package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.response.impl.json.ModificationForParticipant;

@RegisterListener
public record BanBotListener(WhatsappAPI api) implements WhatsappListener {
    @Override
    public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message) {
        if(!(message instanceof WhatsappTextMessage textMessage)){
            return;
        }

        if(!textMessage.text().toLowerCase().contains("/ban")){
            return;
        }

        if(chat.isGroup()){
            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "[WhatsappBot] This command is only supported in groups", textMessage));
            return;
        }

        var quoted = textMessage.quotedMessage().orElse(null);
        if(quoted == null){
            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "[WhatsappBot] Please quote a message sent by the person that you want to ban", textMessage));
            return;
        }

        var victim = quoted.sender().orElse(null);
        if(victim == null){
            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "[WhatsappBot] Missing contact, cannot ban target", textMessage));
            return;
        }

        api.remove(chat, victim).thenAcceptAsync(result -> {
            var victimName = quoted.sender().flatMap(WhatsappContact::bestName).orElse(quoted.senderJid());
            if(result.status() != 200 && result.status() != 207){
                return;
            }

            var success = result.modifications().stream().map(ModificationForParticipant::status).allMatch(e -> e.code() == 200);
            if(!success) {
                api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "[WhatsappBot] Could not ban %s, status code: %s".formatted(victimName, result.status()), textMessage));
                return;
            }

            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "[WhatsappBot] Banned %s", textMessage));
        });
    }
}
