package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.SixPartsKeys;

import java.util.Scanner;

public class SendMessageExample {
    public static void main(String[] args) {
        System.out.println("Enter the six parts segment: ");
        var scanner = new Scanner(System.in);
        var sixParts = scanner.nextLine().trim();
        System.out.println("Enter the phone number of the recipient: ");
        var recipient = scanner.nextLong();
        Whatsapp.mobileBuilder()
                .newConnection(SixPartsKeys.of(sixParts))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .device(CompanionDevice.ios(false)) // Make sure to select the correct account type(business or personal) or you'll get error 401
                .registered()
                .orElseThrow()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(api -> {
                    System.out.println("Sending message...");
                    api.sendMessage(Jid.of(recipient), "Hello World").join();
                    System.out.println("Sent message!");
                })
                .connect() // If you get error 403 o 503 the account is banned
                .join();
    }
}
