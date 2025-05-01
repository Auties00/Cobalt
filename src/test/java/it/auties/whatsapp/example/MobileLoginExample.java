package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.util.AppMetadata;

import java.util.Scanner;

public class MobileLoginExample {
    public static void main(String[] args) {
        System.out.println("Enter the six parts segment: ");
        var scanner = new Scanner(System.in);
        var sixParts = scanner.nextLine().trim();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        AppMetadata.getVersion(UserAgent.PlatformType.KAIOS).join();
        Whatsapp.mobileBuilder()
                .newConnection()
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .device(CompanionDevice.android(business)) // Make sure to select the correct account type(business or personal) or you'll get error 401
                .unregistered()
                .verificationCodeSupplier(() -> "1")
                .register(114141414141414L)
                .join()
                .whatsapp()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(api -> System.out.println("Logged in"))
                .connect() // If you get error 403 o 503 the account is banned
                .join()
                .awaitDisconnection();
    }
}
