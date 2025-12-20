package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.info.MessageInfo;

import java.util.Map;

public final class MessageSenderService {
    private final WhatsAppClient whatsapp;

    public MessageSenderService(WhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    public void sendMessage(MessageInfo info, Map<String, ?> attributes) {

    }
}
