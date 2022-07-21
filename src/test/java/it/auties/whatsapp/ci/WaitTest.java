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
                    try {
                        if (message.message()
                                .content() instanceof TextMessage textMessage) {
                            if (textMessage.text()
                                    .contains("/prova")) {
                                var quotedMessage = message.quotedMessage()
                                        .orElse(null);
                                if(quotedMessage == null) return;
                                System.out.println(api.sendReaction(quotedMessage, textMessage.text().split(" ", 2)[1]).join());
                                System.out.println("Sent " + textMessage.text().split(" ", 2)[1]);
                            }
                        }
                    }catch (Throwable throwable){
                        throwable.printStackTrace();
                    }
                })
                .connect()
                .join();
        whatsapp.await();
    }
}
