package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class MobileRegisterExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Enter the phone number: ");
        Scanner scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        var proxyUsername = "wy961882248*4g_%s".formatted(ThreadLocalRandom.current().nextInt(0, 100_000_000));
        System.out.println(proxyUsername);
        rotateProxy(proxyUsername, "us");
        var result = Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(business))
                .proxy(URI.create("http://%s:999999@proxyus.rola.vip:1000/".formatted(proxyUsername)))
                .unregistered()
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(phoneNumber)
                .join();
        System.out.println(result.whatsapp().keys().toString());
    }

    private static void rotateProxy(String username, String country) throws IOException, InterruptedException {
        try(var httpClient = HttpClient.newHttpClient()) {
            for(var index = 0; index < 3; index++) {
                System.out.println("Rotating to " + country);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://refreshus2.rola.vip/refresh?user=%s&country=%s".formatted(username, country)))
                        .build();
                var result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Rola response: " + result.body());
                if(result.body().contains("SUCCESS")) {
                    return;
                }
            }

            throw new RuntimeException("Cannot rotate proxy");
        }
    }
}
