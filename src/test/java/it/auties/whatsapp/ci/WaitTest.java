package it.auties.whatsapp.ci;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.util.JacksonProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class WaitTest {
    @Test
    public void testForFiveMinutes() {
        var options = Whatsapp.Options.defaultOptions()
                .withErrorHandler(ErrorHandler.toTerminal());
        var whatsapp = Whatsapp.lastConnection(options)
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addChatsListener((api) -> System.out.println("Received " +  api.store().chats().size() + " chats"))
                .addContactsListener((api) -> System.out.println("Received " +  api.store().contacts().size() + " contacts"))
                .addChatMessagesListener((chat, complete) -> {
                        System.out.println(chat.name() + " > " +  chat.messages().size());
                })
                .connect()
                .join();

        whatsapp.await();
    }
}
