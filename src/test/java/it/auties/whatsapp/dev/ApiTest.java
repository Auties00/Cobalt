package it.auties.whatsapp.dev;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import it.auties.whatsapp.utils.MediaUtils;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

@Log
public class ApiTest {
    @SneakyThrows
    public static void main(String[] args) {
        var api = Whatsapp.lastConnection()
                .connect()
                .get();
        waitForInput(api);
    }

    private static void waitForInput(Whatsapp whatsapp){
        try {
            var scanner = new Scanner(System.in);
            var contact = scanner.nextLine();
            if(Objects.equals("contact", "stop")){
                return;
            }

            var chat = ContactJid.of(contact);
            var video = VideoMessage.newVideoMessage()
                    .storeId(whatsapp.store().id())
                    .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                    .caption("Video")
                    .create();
            System.out.println("Sending message to " + contact);
            whatsapp.sendMessage(chat, video)
                    .thenRunAsync(() -> System.out.println("Sent message to " + contact));
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }finally {
            waitForInput(whatsapp);
        }
    }

    @RegisterListener
    public record TestListener(Whatsapp whatsapp) implements WhatsappListener {
        @Override
        public void onLoggedIn() {
            System.out.println("Connected");
        }

        @Override
        public void onAction(Action action) {
            System.out.printf("New action: %s%n" , action);
        }

        @Override
        public void onNewMessage(MessageInfo message) {
            if(message.chat().isEmpty()){
                System.out.println("Empty: " + message);
            }

            System.out.printf("Received a new message at %s%n", message.chat().map(Chat::name).orElse(message.chatJid().toString()));
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
            System.out.printf("Message with id %s sent by %s in chat %s has now status %s for %s%n", message.id(), message.senderName(), chat.name(), status.name(), contact.name());
        }
    }
}
