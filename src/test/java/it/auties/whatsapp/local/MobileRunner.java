package it.auties.whatsapp.local;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;

import java.net.URI;
import java.util.Scanner;

public class MobileRunner {
    public static void main(String[] args) {
        var keys = Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(false))
                .proxy(URI.create("http://wy961882248_static_1:999999@gate8.rola.vip:1066/"))
                .unregistered()
                .verificationCodeMethod(VerificationCodeMethod.WHATSAPP)
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(new Scanner(System.in).nextLong())
                .join()
                .keys()
                .toString();
        System.out.println(keys);
    }
}
