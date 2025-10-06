package com.github.auties00.cobalt.example;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappVerificationHandler;
import com.github.auties00.cobalt.api.WhatsappWebHistoryPolicy;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;

public class WebQrLoginExample {
    public static void main(String[] args) {
        Whatsapp.builder()
                .webClient()
                .lastConnection()
                .historySetting(WhatsappWebHistoryPolicy.standard(true))
                .unregistered(WhatsappVerificationHandler.Web.QrCode.toTerminal())
                .addLoggedInListener(api -> {
                    System.out.println("Logged in!");
                })
                .addWebAppPrimaryFeaturesListener(features -> System.out.printf("Received features: %s%n", features))
                .addNewMessageListener((api, message) -> System.out.println(message))
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener((api, chats) -> System.out.printf("Chats: %s%n", chats.size()))
                .addNewslettersListener((api, newsletters) -> System.out.printf("Newsletters: %s%n", newsletters.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addNewMessageListener((action) -> System.out.printf("New action: %s%n", action))
                .addWebAppStateSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addMessageStatusListener((info) -> System.out.printf("Message status update for %s%n", info.id()))
                .addWebHistorySyncMessagesListener((api, chat, last) -> System.out.printf("%s now has %s messages: %s(oldest message: %s)%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done", chat.oldestMessage().flatMap(ChatMessageInfo::timestamp).orElse(null)))
                .addWebHistorySyncProgressListener((progress, last) -> System.out.printf("Sync progress: %s%%, last: %s%n", progress, last))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .waitForDisconnection();
    }
}
