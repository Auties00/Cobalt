package it.auties.whatsapp.local;

import it.auties.whatsapp.api.PairingCodeHandler;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Just used for testing locally
public class WebTest {
    @Test
    public void run() {
        var whatsapp = Whatsapp.webBuilder()
                .lastConnection()
                .historyLength(WebHistoryLength.ZERO)
                .unregistered(QrHandler.toTerminal())
                .addLoggedInListener(api -> {
                    System.out.printf("Connected: %s%n", api.store().privacySettings());
                    api.createCommunity("Test", "Something").join();
                })
                .addNewMessageListener((api, message, offline) -> System.out.println(message.toJson()))
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener ((action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addContactPresenceListener((chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n", contact, chat.name(), status.name()))
                .addAnyMessageStatusListener((chat, contact, info, status) -> System.out.printf("Message %s in chat %s now has status %s for %s %n", info.id(), info.chatName(), status, contact == null ? null : contact.name()))
                .addChatMessagesSyncListener((chat, last) -> System.out.printf("%s now has %s messages: %s%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done"))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
        System.out.println("Connected");
        whatsapp.awaitDisconnection();
        System.out.println("Disconnected");
    }
}
