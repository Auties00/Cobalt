package it.auties.github.dev;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MessageStatus;
import it.auties.whatsapp.util.QrHandler;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log
public class WhatsappAPITest {
    @Test
    public void login() {
        Whatsapp.newConnection()
                .connect();
    }

    @AllArgsConstructor
    @RegisterListener
    public static class TestListener implements WhatsappListener {
        public Whatsapp whatsapp;

        @Override
        public void onLoggedIn() {
            System.out.println("Connected");
        }

        @Override
        public void onChats() {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                whatsapp.sendMessage(whatsapp.store().findChatByName("Carlo").orElseThrow(), "Test da md");
            });

        }

        @Override
        public QrHandler onQRCode(BitMatrix qr) {
            return QrHandler.FILE;
        }

        @Override
        public void onNewMessage(MessageInfo message) {
            if(message.chat().isEmpty()){
                System.out.println("Empty: " + message);
            }
            System.out.printf("Received a new message at %s%n", message.chat().map(Chat::name).orElse("AAA: " + message.chatJid()));
        }

        @Override
        public void onChatRecentMessages(Chat chat) {
            System.out.printf("Received %s messages at %s%n", chat.messages().size(), chat.name());
        }

        @Override
        public void onContactPresence(Chat chat, Contact contact, ContactStatus contactStatus) {
            System.out.printf("%s is now %s in %s%n", contact.name(), contactStatus.name(), chat.name());
        }

        @Override
        public void onMessageStatus(Chat chat, Contact contact, MessageInfo message, MessageStatus status) {
            System.out.printf("Message with jid %s in chat %s%s has now status %s%n", message.id(), chat.name(), contact == null ? "" : "sent by %s".formatted(contact.name()), status.name());
        }
    }
}
