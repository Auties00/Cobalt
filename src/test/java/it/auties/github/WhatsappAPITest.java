package it.auties.github;

import it.auties.whatsapp.api.Whatsapp;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Test;

@Log
public class WhatsappAPITest {
    @Test
    public void login() {
        var api = new Whatsapp();
        api.connect();
    }
}
