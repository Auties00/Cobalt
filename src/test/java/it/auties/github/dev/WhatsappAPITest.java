package it.auties.github.dev;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.util.QrHandler;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

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
    }
}
