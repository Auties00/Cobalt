import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.WhatsAppWebClientHistory;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;

void main() {
    var phoneNumber = promptPhoneNumber();
    WhatsAppClient.builder()
            .webClient()
            .createConnection()
            .historySetting(WhatsAppWebClientHistory.standard(true))
            .unregistered(phoneNumber, WhatsAppClientVerificationHandler.Web.PairingCode.toTerminal())
            .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings()))
            .addWebAppPrimaryFeaturesListener((_, features) -> System.out.printf("Received features: %s%n", features))
            .addNewMessageListener((_, message) -> System.out.println(message))
            .addContactsListener((_, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
            .addChatsListener((_, chats) -> System.out.printf("Chats: %s%n", chats.size()))
            .addNewslettersListener((_, newsletters) -> System.out.printf("Newsletters: %s%n", newsletters.size()))
            .addNodeReceivedListener((_, incoming) -> System.out.printf("Received node %s%n", incoming))
            .addNodeSentListener((_, outgoing) -> System.out.printf("Sent node %s%n", outgoing))
            .addWebAppStateActionListener((_, action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
            .addWebAppStateSettingListener((_, setting) -> System.out.printf("New setting: %s%n", setting))
            .addMessageStatusListener((_, info) -> System.out.printf("Message status update for %s%n", info.id()))
            .addWebHistorySyncMessagesListener((_, chat, last) -> System.out.printf("%s now has %s messages: %s(oldest message: %s)%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done", chat.oldestMessage().flatMap(ChatMessageInfo::timestamp).orElse(null)))
            .addDisconnectedListener((_, reason) -> System.out.printf("Disconnected: %s%n", reason))
            .connect()
            .waitForDisconnection();
}

private long promptPhoneNumber() {
    while (true) {
        try {
            var input = IO.readln("Enter the phone number: ");
            return Long.parseUnsignedLong(input);
        } catch(NumberFormatException _) {
            IO.println("Invalid phone number!");
        }
    }
}
