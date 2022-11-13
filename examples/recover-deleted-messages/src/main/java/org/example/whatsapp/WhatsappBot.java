package org.example.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;

import java.util.concurrent.ExecutionException;

// This is the main class of our bot
public class WhatsappBot {
    public static void main(String... args) throws ExecutionException, InterruptedException {
        // Create a new instance of WhatsappAPI
        Whatsapp.lastConnection()
                .addLoggedInListener(() -> System.out.println("Connected!"))
                .addMessageDeletedListener(WhatsappBot::onMessageDeleted)
                .connect()
                .get();
    }

    private static void onMessageDeleted(MessageInfo message, boolean everyone) {
        // Check if the message was deleted for everyone or only for yourself
        if (everyone) {
            // Print a message to confirm that the event was caught
            System.out.printf("%s deleted a message from %s for everyone%n", message.senderName(), message.chatName());
            return;
        }

        // If the message was deleted only for you, it means that you sent it
        // Print a message to confirm that the event was caught
        System.out.printf("You deleted a message from %s%n", message.chatName());
    }
}
