package org.example.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.util.concurrent.ExecutionException;

// This is the main class of our bot
public class BanBot {
    public static void main(String... args) throws ExecutionException, InterruptedException {
        // Create a new instance of WhatsappAPI
        Whatsapp.lastConnection()
                .addLoggedInListener(() -> System.out.println("Connected!"))
                .addNewMessageListener(BanBot::onMessage)
                .connect()
                .get();
    }

    private static void onMessage(Whatsapp api, MessageInfo info) {
        if (!(info.message()
                .content() instanceof TextMessage textMessage)) {
            return;
        }

        if (!textMessage.text()
                .toLowerCase()
                .contains("/ban")) {
            return;
        }

        if (info.chatJid().hasServer(ContactJid.Server.GROUP)) {
            api.sendMessage(info.chatJid(), "[WhatsappBot] This command is only supported in groups", info);
            return;
        }

        var quoted = info.quotedMessage();
        if (quoted.isEmpty()) {
            api.sendMessage(info.chatJid(),
                    "[WhatsappBot] Please quote a message sent by the person that you want to ban", info);
            return;
        }

        var victim = quoted.get()
                .sender()
                .orElse(null);
        if (victim == null) {
            api.sendMessage(info.chatJid(), "[WhatsappBot] Missing contact, cannot ban target", info);
            return;
        }

        api.removeGroupParticipant(info.chat(), victim)
                .thenRunAsync(() -> api.sendMessage(info.chatJid(), "[WhatsappBot] The contact was successfully banned", info));
    }
}
