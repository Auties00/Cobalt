package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Scanner;

public class MobileExistsExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        Scanner scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        var result = Whatsapp.mobileBuilder()
                .serializer(ControllerSerializer.discarding())
                .newConnection()
                .device(CompanionDevice.ios(true))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .exists(phoneNumber)
                .join();
        System.out.println("Has whatsapp? " + result);
    }
}
