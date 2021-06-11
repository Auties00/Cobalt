package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.TextMessage;
import it.auties.whatsapp4j.protobuf.message.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.MessageKey;
import it.auties.whatsapp4j.response.impl.json.ModificationForParticipant;

@RegisterListener
public record BanBotListener(WhatsappAPI api) implements WhatsappListener {
    @Override
    public void onNewMessage(Chat chat, MessageInfo info) {
        var textMessage = info.container().textMessage();
        if(textMessage == null){
            return;
        }

        if(!textMessage.text().toLowerCase().contains("/ban")){
            return;
        }

        if(chat.isGroup()){
            sendResponseText(chat, info, "[WhatsappBot] This command is only supported in groups");
            return;
        }

        var quoted = textMessage.contextInfo().quotedMessage();
        if(quoted.isEmpty()){
            sendResponseText(chat, info, "[WhatsappBot] Please quote a message sent by the person that you want to ban");
            return;
        }

        var victim = quoted.get().key().sender().orElse(null);
        if(victim == null){
            sendResponseText(chat, info, "[WhatsappBot] Missing contact, cannot ban target");
            return;
        }

        api.remove(chat, victim).thenAcceptAsync(result -> {
            var victimName = victim.bestName().orElse(victim.jid());
            if(result.status() != 200 && result.status() != 207){
                sendResponseText(chat, info, "[WhatsappBot] Could not ban %s, status code: %s".formatted(victimName, result.status()));
                return;
            }

            var success = result.modifications().stream().map(ModificationForParticipant::status).allMatch(e -> e.code() == 200);
            if(!success) {
                sendResponseText(chat, info, "[WhatsappBot] Could not ban %s, status code: %s".formatted(victimName, result.status()));
                return;
            }

            sendResponseText(chat, info, "[WhatsappBot] Banned %s".formatted(victimName));
        });
    }

    private void sendResponseText(Chat chat, MessageInfo info, String text) {
        var key = new MessageKey(chat);
        var responseText = TextMessage.newTextMessage()
                .text(text)
                .contextInfo(new ContextInfo(info))
                .create();
        var responseMessage = new MessageContainer(responseText);
        var responseMessageInfo = new MessageInfo(key, responseMessage);
        api.sendMessage(responseMessageInfo);
    }
}
