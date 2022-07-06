package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.ErrorHandler;
import it.auties.whatsapp.api.Whatsapp;
import org.junit.jupiter.api.Test;

public class ErrorsTest {
    @Test
    public void testForFiveMinutes() {
        var options = Whatsapp.Options.defaultOptions()
                .withErrorHandler(ErrorHandler.toTerminal());
        Whatsapp.lastConnection(options)
                .connect()
                .join()
                .await();
    }
}
