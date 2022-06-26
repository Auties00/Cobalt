package it.auties.whatsapp.unit;

import it.auties.whatsapp.api.Whatsapp;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static it.auties.whatsapp.api.Whatsapp.Options.defaultOptions;

public class QuotedMessageTest {
    @Test
    public void quoteMessage() throws InterruptedException, ExecutionException {
        var options = defaultOptions().withAutodetectListeners(false);
        Whatsapp.newConnection(options)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNewMessageListener((whatsapp, info) -> System.out.println(info))
                .connect()
                .get()
                .await();
    }
}
