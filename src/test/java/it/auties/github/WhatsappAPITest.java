package it.auties.github;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

@Log
public class WhatsappAPITest {
    @Test
    public void login() {
        new WhatsappKeys().delete();
        var api = new Whatsapp();
        api.connect();
    }

    @AllArgsConstructor
    @RegisterListener
    public static class TestListener implements WhatsappListener {
        public Whatsapp whatsapp;

        @Override
        public void onLoggedIn() {

            System.out.println("Logged in!");
            whatsapp.changePresence(ContactStatus.UNAVAILABLE)
                    .whenCompleteAsync((result, exception) -> {
                        System.out.println("RESULT: " + result);
                        if(exception != null) exception.printStackTrace();
                    });
        }
    }
}
