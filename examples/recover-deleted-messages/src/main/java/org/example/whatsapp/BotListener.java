package org.example.whatsapp;

import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.response.impl.json.UserInformationResponse;

// A listener that will be automatically detected, used to listen to the deletion of messages
@RegisterListener
public class BotListener implements WhatsappListener {
    // Called when we successfully log into org.example.whatsapp
    @Override
    public void onLoggedIn(UserInformationResponse info) {
        System.out.println("Hello :)");
    }

    // Called when anyone deleted a message
    @Override
    public void onMessageDeleted(Chat chat, MessageInfo info, boolean everyone) {
        // Get the name of the chat where the message was cancelled
        var chatName = chat.displayName();

        // Check if the message was deleted for everyone or only for yourself
        if(everyone) {
            // If the message was deleted for everyone it's not guaranteed that you have sent said message, so we get the name of the original sender
            var sender = info.sender().flatMap(Contact::bestName).orElse(info.senderJid());

            // Print a message to confirm that the event was caught
            System.out.printf("%s deleted a message from %s for everyone%n", sender, chatName);
            return;
        }

        // If the message was deleted only for you, it means that you sent it
        // Print a message to confirm that the event was caught
        System.out.printf("You deleted a message from %s%n", chatName);
    }
}
