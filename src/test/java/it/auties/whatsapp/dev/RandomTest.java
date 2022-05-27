package it.auties.whatsapp.dev;

import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Keys;

import java.io.IOException;

public class RandomTest implements JacksonProvider {
    public static void main(String[] args) {
        var keyId = Keys.senderKeyId();
        var senderKey = Keys.senderKey();
        System.out.println("Done");
    }
}
