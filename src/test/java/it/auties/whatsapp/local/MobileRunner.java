package it.auties.whatsapp.local;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MobileRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        try(var client = HttpClient.newHttpClient()) {
            var phoneNumberRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://daisysms.com/stubs/handler_api.php?api_key=dQc1wqtHwm0M6mFs6wmRBw8VK7nElD&action=getNumber&service=ds&max_price=0.50"))
                    .GET()
                    .build();
            var phoneNumberResponse = client.send(phoneNumberRequest, HttpResponse.BodyHandlers.ofString());
            var phoneNumberResponsePayload = phoneNumberResponse.body().split(":", 3);
            if(!Objects.equals(phoneNumberResponsePayload[0], "ACCESS_NUMBER")) {
                System.err.println(phoneNumberResponse.body());
                return;
            }

            var id = phoneNumberResponsePayload[1];
            var phoneNumber = Long.parseLong(phoneNumberResponsePayload[2]);
            System.out.println("Got phone number: " + phoneNumber);
            var whatsapp = Whatsapp.mobileBuilder()
                    .newConnection()
                    .proxy(URI.create("http://wy961882248_1:999999@gate8.rola.vip:1066/"))
                    .device(CompanionDevice.ios(false))
                    .unregistered()
                    .verificationCodeSupplier(() -> getOtp(client, id))
                    .verificationCodeMethod(VerificationCodeMethod.SMS)
                    .register(phoneNumber)
                    .join();
            System.out.println(whatsapp.keys().toString());
            whatsapp.addLoggedInListener(() -> System.out.println("Connected"))
                    .connect()
                    .join();
            CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                    .execute(() -> whatsapp.disconnect().join());
        }
    }

    private static CompletableFuture<String> getOtp(HttpClient client, String id) {
        var otpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://daisysms.com/stubs/handler_api.php?api_key=dQc1wqtHwm0M6mFs6wmRBw8VK7nElD&action=getStatus&id=" + id))
                .GET()
                .build();
        return client.sendAsync(otpRequest, HttpResponse.BodyHandlers.ofString()).thenCompose(result -> {
            var otpResponsePayload = result.body().split(":", 2);
            if(!Objects.equals(otpResponsePayload[0], "ACCESS_NUMBER")) {
                System.out.println("Waiting otp: " + result.body());
                waitOtp();
                return getOtp(client, id);
            }

            System.out.println("Got otp: " + otpResponsePayload[1]);
            return CompletableFuture.completedFuture(otpResponsePayload[1]);
        });
    }

    private static void waitOtp() {
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

