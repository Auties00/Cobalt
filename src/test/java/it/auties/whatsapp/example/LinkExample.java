package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.SixPartsKeys;

import java.util.Scanner;

public class LinkExample {
    public static void main(String[] args) {
        Whatsapp.mobileBuilder()
                .newConnection(SixPartsKeys.of("13372926517,Rn6zkm8y1HFsN9Ju/MablwH+An2Jz1u4BKnCG3Rtjxg=,EMHANMXhfVmSUPE8PWLCeUs/BK5EywDgwu39rZxi+Gg=,pASvgvbZK/qtp4gfeTB1waQ0sham1jnopQR0MX4sUh8=,8HN3u2MDb/cM2ws3qT1gVEuA3dMtbwdNSPV29TItjmo=,hqSf9BfkYkwGgcndEzRFYbQPz7g="))
                .device(CompanionDevice.ios(false))
                .registered()
                .orElseThrow()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(api -> {
                    System.out.println("Enter the link qr: ");
                    api.unlinkDevices().join();
                    api.linkDevice(new Scanner(System.in).nextLine().trim()).join();
                })
                .connect()
                .join();
    }
}
