package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class WaitTest implements JacksonProvider {
    @Test
    public void testForFiveMinutes() {
        Whatsapp.lastConnection()
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNewMessageListener(WaitTest::logMessage)
                .addContactsListener((whatsapp) -> System.out.println(whatsapp.store().contacts().size()))
                .addChatsListener((whatsapp) -> System.out.println(whatsapp.store().chats().size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .connect()
                .join()
                .await();
    }

    @SneakyThrows
    private static void logMessage(MessageInfo message) {
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(message));
    }
}
