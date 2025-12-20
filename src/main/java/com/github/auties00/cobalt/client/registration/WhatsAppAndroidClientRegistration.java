package com.github.auties00.cobalt.client.registration;

import com.github.auties00.cobalt.client.WhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.store.WhatsAppStore;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Locale;
import java.util.UUID;

public final class WhatsAppAndroidClientRegistration extends WhatsAppMobileClientRegistration {
    public WhatsAppAndroidClientRegistration(WhatsAppStore store, WhatsAppClientVerificationHandler.Mobile verification) {
        super(store, verification);
    }

    @Override
    protected HttpRequest createRequest(String path, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create("%s%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path)))
                .POST(HttpRequest.BodyPublishers.ofString("ENC=" + body))
                .header("User-Agent", store.device().toUserAgent(store.clientVersion()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/json")
                .header("WaMsysRequest", "1")
                .header("request_token", UUID.randomUUID().toString())
                .build();
    }

    @Override
    protected String[] getRequestVerificationCodeParameters(String method) {
        return new String[]{
                "method", method,
                "sim_mcc", "000",
                "sim_mnc", "000",
                "reason", "",
                "mcc", "000",
                "mnc", "000",
                "feo2_query_status", "error_security_exception",
                "db", "1",
                "sim_type", "0",
                "recaptcha", "%7B%22stage%22%3A%22ABPROP_DISABLED%22%7D",
                "network_radio_type", "1",
                "prefer_sms_over_flash", "false",
                "simnum", "0",
                "airplane_mode_type", "0",
                "client_metrics", "%7B%22attempts%22%3A20%2C%22app_campaign_download_source%22%3A%22google-play%7Cunknown%22%7D",
                "mistyped", "7",
                "advertising_id", store.advertisingId().toString(),
                "hasinrc", "1",
                "roaming_type", "0",
                "device_ram", "3.57",
                "education_screen_displayed", "false",
                "pid", String.valueOf(ProcessHandle.current().pid()),
                "gpia", "",
                "cellular_strength", "5",
                "_gg", "",
                "_gi", "",
                "_gp", "",
                "backup_token", toUrlHex(store.backupToken()),
                "hasav", "2"
        };
    }

    @Override
    protected String generateFdid() {
        return store.fdid().toString().toLowerCase(Locale.ROOT);
    }
}