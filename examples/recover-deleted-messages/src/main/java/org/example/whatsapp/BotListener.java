package org.example.whatsapp;

import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.model.info.MessageInfo;

// A listener that will be automatically detected, used to listen to the deletion of messages
@RegisterListener
public class BotListener implements WhatsappListener {
    // Called when we successfully log into org.example.whatsapp
    @Override
    public void onLoggedIn() {
        System.out.println("Hello :)");
    }

    @Override
    public void onMessageDeleted(MessageInfo message, boolean everyone) {
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
