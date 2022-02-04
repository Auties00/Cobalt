package it.auties.github.dev;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.util.QrHandler;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.util.Objects;

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
        public QrHandler onQRCode(@NonNull BitMatrix qr) {
            return QrHandler.FILE;
        }

        @Override
        public void onNewMessage(@NonNull MessageInfo message) {
            if(message.chat().isEmpty()){
                System.out.println("Empty: " + message);
            }
            System.out.printf("Received a new message at %s%n", message.chat().map(Chat::name).orElse("AAA: " + message.chatJid()));
        }

        @Override
        public void onChatRecentMessages(@NonNull Chat chat) {
            System.out.printf("Received %s messages at %s%n", chat.messages().size(), chat.name());
        }
    }
}
