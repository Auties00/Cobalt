package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.message.standard.TextMessage;
import org.junit.jupiter.api.Test;

public class WaitTest {
    @Test
    public void testForFiveMinutes() {
        var options = Whatsapp.Options.defaultOptions()
                .withErrorHandler(ErrorHandler.toTerminal());
        var whatsapp = Whatsapp.lastConnection(options)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNewMessageListener((api, message) -> {
                    api.sendMessage(message.chatJid(), "Hello");
                    if (message.message()
                            .content() instanceof TextMessage textMessage) {
                        if (textMessage.text()
                                .contains("/prova")) {
                            message.quotedMessage()
                                    .ifPresent(quotedMessage -> {
                                        api.sendReaction(quotedMessage, ":)")
                                                .join();
                                        System.out.println("Sent");
                                    });
                        }
                    }
                })
                .connect()
                .join();
        whatsapp.await();
    }
}
