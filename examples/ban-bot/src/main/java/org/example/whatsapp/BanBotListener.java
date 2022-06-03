package org.example.whatsapp;

import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;

@RegisterListener
public record BanBotListener(Whatsapp api) implements WhatsappListener {
    @Override
    public void onNewMessage(MessageInfo info) {
        if(!(info.message().content() instanceof TextMessage textMessage)){
            return;
        }

        if(!textMessage.text().toLowerCase().contains("/ban")){
            return;
        }

        if(info.chatJid().type() == ContactJid.Type.GROUP){
            api.sendMessage(info.chatJid(), "[WhatsappBot] This command is only supported in groups", info);
            return;
        }

        var quoted = info.quotedMessage();
        if(quoted.isEmpty()){
            api.sendMessage(info.chatJid(), "[WhatsappBot] Please quote a message sent by the person that you want to ban", info);
            return;
        }

        var victim = quoted.get().sender().orElse(null);
        if(victim == null){
            api.sendMessage(info.chatJid(), "[WhatsappBot] Missing contact, cannot ban target", info);
            return;
        }

        api.remove(info.chat().orElseThrow(), victim).thenAcceptAsync(result -> {
            // Not implemented yet
        });
    }
}
