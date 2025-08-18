package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;

import java.util.Scanner;

public class MobileHttpExistsExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        Scanner scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        // URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
        var result = Whatsapp.checkNumber(phoneNumber)
                .join();
        System.out.println("Has whatsapp? " + result);
    }
}
