package it.auties.whatsapp;

import it.auties.whatsapp.util.Json;

import java.net.InetSocketAddress;

public class MiscTest {
    public static void main(String[] args) {
        System.out.println(Json.writeValueAsString(InetSocketAddress.createUnresolved("localhost", 8080)));
    }
}
