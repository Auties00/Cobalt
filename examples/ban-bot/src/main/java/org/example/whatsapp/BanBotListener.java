package org.example.whatsapp;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.standard.TextMessage;
import it.auties.whatsapp4j.response.impl.json.ModificationForParticipant;
import lombok.NonNull;

@RegisterListener
public record BanBotListener(WhatsappAPI api) implements WhatsappListener {
    @Override
    public void onNewMessage(@NonNull Chat chat, @NonNull MessageInfo info) {
        var textMessage = info.container().textMessage();
        if(textMessage == null){
            return;
        }

        if(!textMessage.text().toLowerCase().contains("/ban")){
            return;
        }

        if(chat.isGroup()){
            api.sendMessage(chat, new TextMessage("[WhatsappBot] This command is only supported in groups"), info);
            return;
        }

        var quoted = info.quotedMessage();
        if(quoted.isEmpty()){
            api.sendMessage(chat, new TextMessage("[WhatsappBot] Please quote a message sent by the person that you want to ban"), info);
            return;
        }

        var victim = quoted.get().sender().orElse(null);
        if(victim == null){
            api.sendMessage(chat, new TextMessage("[WhatsappBot] Missing contact, cannot ban target"), info);
            return;
        }

        api.remove(chat, victim).thenAcceptAsync(result -> {
            var victimName = victim.bestName().orElse(victim.jid());
            if(result.status() != 200 && result.status() != 207){
                api.sendMessage(chat, new TextMessage("[WhatsappBot] Could not ban %s, status code: %s".formatted(victimName, result.status())), info);
                return;
            }

            var success = result.modifications().stream().map(ModificationForParticipant::status).allMatch(e -> e.code() == 200);
            if(!success) {
                api.sendMessage(chat, new TextMessage("[WhatsappBot] Could not ban %s, status code: %s".formatted(victimName, result.status())), info);
                return;
            }

            api.sendMessage(chat, new TextMessage("[WhatsappBot] Banned %s".formatted(victimName)), info);
        });
    }
}
