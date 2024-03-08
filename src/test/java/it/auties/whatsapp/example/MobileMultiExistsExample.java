package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class MobileMultiExistsExample {
    private static final int TOTAL = 100_000;

    public static void main(String[] args) throws IOException {
        var dataInput = ClassLoader.getSystemResource("data.txt");
        Objects.requireNonNull(dataInput, "Missing data.txt, create it in the resources directory");
        try(var input = dataInput.openStream()) {
            var data = new String(input.readAllBytes());
            var futures = new ArrayList<CompletableFuture<?>>();
            var start = System.currentTimeMillis();
            System.out.println("Starting check");
            var success = new AtomicInteger();
            for(var entry : Arrays.copyOf(data.split("\n"), TOTAL)) {
                var future =  Whatsapp.mobileBuilder()
                        .newConnection()
                        .device(CompanionDevice.ios(true))
                        // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy and use a random id to change the ip at every request
                        .exists(Long.parseLong(entry))
                        .thenRun(() -> System.out.println("Progress: " + (success.incrementAndGet() * 100 / TOTAL) + "% in " + (System.currentTimeMillis() - start) + "ms"))
                        .exceptionally(error -> null);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            System.out.printf("Took %sms%n", System.currentTimeMillis() - start);
        }
    }
}
