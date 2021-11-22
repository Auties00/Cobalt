package it.auties.github;

import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappConfiguration;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.socket.WhatsappSocket;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Log
public class WhatsappAPITest {
    @Test
    public void login() throws InterruptedException {
        var whatsapp = new WhatsappSocket(WhatsappConfiguration.defaultOptions(), new WhatsappStore(), new WhatsappKeys());
        whatsapp.store().listeners().add(new Listener());
        whatsapp.connect();

    }

    private static class Listener implements WhatsappListener { }
}
