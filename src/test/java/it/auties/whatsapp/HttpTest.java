package it.auties.whatsapp;

import it.auties.whatsapp.net.HttpClient;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;

public class HttpTest {
    public static void main(String[] args) {
        var test = new HttpClient(URI.create("socks5://wy961882248_dc_%s-country-us:999999@proxyus.rola.vip:2000/".formatted(ThreadLocalRandom.current().nextInt(0, 999_999))));
        System.out.println(test.getString(URI.create("http://api.ipify.org")).join());
        test.close();
    }
}
