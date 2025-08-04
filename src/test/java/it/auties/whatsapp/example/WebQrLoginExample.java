package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.api.WhatsappWebHistoryPolicy;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.message.standard.VideoMessageSimpleBuilder;

import java.net.URI;

public class WebQrLoginExample {
    public static void main(String[] args) {
        Whatsapp.builder()
                .webClient()
                .newConnection()
                .historySetting(WhatsappWebHistoryPolicy.standard(true))
                .unregistered(WhatsappVerificationHandler.Web.QrCode.toTerminal())
                .addLoggedInListener(() -> System.out.println("Logged in!"))
                .addFeaturesListener(features -> System.out.printf("Received features: %s%n", features))
                .addNewChatMessageListener((api, message) -> System.out.println(message))
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener((api, chats) -> {
                    System.out.printf("Chats: %s%n", chats.size());
                    try(var stream = URI.create("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                            .toURL()
                            .openStream()) {
                        var image = new VideoMessageSimpleBuilder()
                                .media(stream.readAllBytes())
                                .caption("Hello World")
                                .build();
                        var chat = api.store()
                                .findChatByName("brew install emacs")
                                .orElseThrow();
                        var result = api.sendMessage(chat, image);
                        System.out.println(result);
                    }catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .addNewslettersListener((api, newsletters) -> System.out.printf("Newsletters: %s%n", newsletters.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener ((action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addMessageStatusListener((info) -> System.out.printf("Message status update for %s%n", info.id()))
                .addChatMessagesSyncListener((api, chat, last) -> System.out.printf("%s now has %s messages: %s(oldest message: %s)%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done", chat.oldestMessage().flatMap(ChatMessageInfo::timestamp).orElse(null)))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .waitForDisconnection();
    }
}
