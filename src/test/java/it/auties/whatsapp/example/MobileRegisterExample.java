package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Scanner;

// Android setup:
//   1. Rooted android phone
//   2. Magisk installed with Magisk Hide and Play integrity fix module
//   3. Download any play integrity app from the play store: the phone must pass device and basic integrity (emulators and rooted devices without the Play integrity fix module can't do this)
//   4. Frida-server installed and running on the phone (leave the phone connected via usb)
//   5. Compile, install and open the app located at support/cert on the phone
//   6. Download whatsapp business on the phone and try to register any number, leave the app open
//   7. Start the go backend located at support/gpia
//   8. Now you can register
// iOS setup: none necessary
// KaiOS setup: none necessary
public class MobileRegisterExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        Scanner scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(business))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .unregistered()
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(phoneNumber)
                .join();
    }
}
