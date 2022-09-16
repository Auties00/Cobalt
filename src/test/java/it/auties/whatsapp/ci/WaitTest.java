package it.auties.whatsapp.ci;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.util.JacksonProvider;
import org.junit.jupiter.api.Test;

public class WaitTest {
    @Test
    public void testForFiveMinutes() {
        var options = Whatsapp.Options.defaultOptions()
                .withErrorHandler(ErrorHandler.toTerminal());
        var whatsapp = Whatsapp.lastConnection(options)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNewMessageListener(message -> {
                    try {
                        System.out.println(JacksonProvider.JSON.writeValueAsString(message));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .connect()
                .join();

        whatsapp.await();
    }
}
