package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.util.JacksonProvider;

// Just used for testing locally
public class WaitTest implements JacksonProvider {
  public static void main(String[] args) {
    var whatsapp = Whatsapp.lastConnection()
        .addLoggedInListener(() -> System.out.println("Connected"))
        .addNewMessageListener(message -> System.out.println(message.toJson()))
        .addContactsListener(
            (api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
        .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
        .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
        .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
        .addActionListener(
            (action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
        .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
        .addContactPresenceListener(
            (chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n",
                contact.name(), chat.name(), status.name()))
        .addAnyMessageStatusListener((chat, contact, info, status) -> System.out.printf(
            "Message %s in chat %s now has status %s for %s %n", info.id(), info.chatName(), status,
            contact == null ? null : contact.name()))
        .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason));
    whatsapp.connect().join();
    System.out.println("Done");
  }
}
