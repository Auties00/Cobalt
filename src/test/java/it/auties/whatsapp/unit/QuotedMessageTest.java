package it.auties.whatsapp.unit;

import it.auties.whatsapp.api.Whatsapp;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static it.auties.whatsapp.api.Whatsapp.Options.defaultOptions;

public class QuotedMessageTest {
    @Test
    public void quoteMessage() throws InterruptedException, ExecutionException {
        var options = defaultOptions().withAutodetectListeners(false);
        Whatsapp.lastConnection(options)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNodeReceivedListener(System.out::println)
                .addDisconnectedListener((reconnected) -> System.out.println("Disconnected: " + reconnected))
                .addNewMessageListener(System.out::println)
                .addNewMessageListener((whatsapp, info) -> whatsapp.sendMessage(info.chatJid(), "è bello!"))
                .connect()
                .get()
                .await();
    }
}
