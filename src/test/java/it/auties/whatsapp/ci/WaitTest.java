package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import org.junit.jupiter.api.Test;

public class WaitTest {
    @Test
    public void testForFiveMinutes() {
        var options = Whatsapp.Options.defaultOptions()
                .withErrorHandler(ErrorHandler.toTerminal());
        var whatsapp = Whatsapp.lastConnection(options)
                .connect()
                .join();
        whatsapp.await();
    }
}
