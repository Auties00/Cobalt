package com.github.auties00.cobalt.client.registration;

import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.store.WhatsAppStore;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Locale;

public final class WhatsAppIosClientRegistration extends WhatsAppMobileClientRegistration {
    public WhatsAppIosClientRegistration(WhatsAppStore store, WhatsAppClientVerificationHandler.Mobile verification) {
        super(store, verification);
    }

    @Override
    protected HttpRequest createRequest(String path, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create("%s%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path)))
                .POST(HttpRequest.BodyPublishers.ofString("ENC=" + body))
                .header("User-Agent", store.device().toUserAgent(store.clientVersion()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
    }

    @Override
    protected String[] getRequestVerificationCodeParameters(String method) {
        return new String[]{
                "method", method,
                "sim_mcc", "000",
                "sim_mnc", "000",
                "reason", "",
                "cellular_strength", "1"
        };
    }

    @Override
    protected String generateFdid() {
        return store.fdid().toString().toUpperCase(Locale.ROOT);
    }
}