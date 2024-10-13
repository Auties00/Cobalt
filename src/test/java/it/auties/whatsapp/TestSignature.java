package it.auties.whatsapp;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.net.HttpClient;
import it.auties.whatsapp.util.Json;

import java.net.URI;
import java.util.Map;

public class TestSignature {
    public static void main(String[] args) {
        System.out.println("\"TLS_GREASE_IS_THE_WORD_5A\",\"TLS_AES_128_GCM_SHA256\",\"TLS_AES_256_GCM_SHA384\",\"TLS_CHACHA20_POLY1305_SHA256\",\"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384\",\"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256\",\"TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256\",\"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384\",\"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256\",\"TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256\",\"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA\",\"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA\",\"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA\",\"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA\",\"TLS_RSA_WITH_AES_256_GCM_SHA384\",\"TLS_RSA_WITH_AES_128_GCM_SHA256\",\"TLS_RSA_WITH_AES_256_CBC_SHA\",\"TLS_RSA_WITH_AES_128_CBC_SHA\",\"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA\",\"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA\",\"TLS_RSA_WITH_3DES_EDE_CBC_SHA\"]".replaceAll(",", ", "));
        try(var client = new HttpClient(HttpClient.Platform.IOS)) {
            System.out.println(Json.writeValueAsString(Json.readValue(client.getString(URI.create("https://v.whatsapp.net/v2/code")).join(), new TypeReference<Map<String, Object>>() {
            }), true));
        }
    }
}
