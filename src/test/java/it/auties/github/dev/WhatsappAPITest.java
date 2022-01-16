package it.auties.github.dev;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.file.Files;
import java.util.UUID;

@Log
public class WhatsappAPITest {
    @Test
    public void login() {
        var api = new Whatsapp();
        api.connect();
    }

    @AllArgsConstructor
    @RegisterListener
    public static class TestListener implements WhatsappListener {
        public Whatsapp whatsapp;

        @Override
        @SneakyThrows
        public void onQRCode(@NonNull BitMatrix qr) {
            var path = Files.createTempFile(UUID.randomUUID().toString(), ".jpg");
            MatrixToImageWriter.writeToPath(qr, "jpg", path);
            Desktop.getDesktop().open(path.toFile());
        }

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
