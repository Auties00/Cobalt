package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MobileMultiExistsExample {
    public static void main(String[] args) throws IOException {
        var dataInput = ClassLoader.getSystemResource("data.txt");
        Objects.requireNonNull(dataInput, "Missing data.txt, create it in the resources directory");
        try(var input = dataInput.openStream()) {
            var data = new String(input.readAllBytes());
            for(var entry : data.split("\n")) {
                var randomId = ThreadLocalRandom.current().nextInt(1_000_000_000);
                Whatsapp.mobileBuilder()
                        .newConnection()
                        .device(CompanionDevice.ios(true))
                        // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy and use a random id to change the ip at every request
                        .exists(Long.parseLong(entry))
                        .thenApply(result -> System.out.printf("%s: %s%n", entry, result));
            }
        }
    }
}
