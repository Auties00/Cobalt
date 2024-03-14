package it.auties.whatsapp;

import it.auties.whatsapp.registration.gcm.GcmService;

public class GcmClientTest {
    public static void main(String[] args) {
        var service = new GcmService(293955441834L);
        service.await();
    }
}
