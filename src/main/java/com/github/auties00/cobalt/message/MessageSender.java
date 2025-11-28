package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.info.MessageInfo;

import java.util.Map;

public final class MessageSender {
    private final WhatsAppClient whatsapp;

    public MessageSender(WhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void sendMessage(MessageInfo info, Map<String, ?> attributes) {

    }
}
