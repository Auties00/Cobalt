package it.auties.whatsapp.registration;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

public final class WhatsappMetadata {
    private static final Version MOBILE_BUSINESS_IOS_VERSION = Version.of("2.24.16.78");
    private static final Version MOBILE_PERSONAL_IOS_VERSION = Version.of("2.24.17.71");
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com/check-update?version=2.2245.9&platform=web");
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";

    private static volatile Version webVersion;

    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case IOS ->
                    CompletableFuture.completedFuture(MOBILE_PERSONAL_IOS_VERSION);
            case IOS_BUSINESS ->
                    CompletableFuture.completedFuture(MOBILE_BUSINESS_IOS_VERSION);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<Version> getWebVersion() {
        if (webVersion != null) {
            return CompletableFuture.completedFuture(webVersion);
        }

        try(var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(WEB_UPDATE_URL)
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApplyAsync(response -> {
                var webVersionResponse = Json.readValue(response.body(), WebVersionResponse.class);
                return webVersion = Version.of(webVersionResponse.currentVersion());
            });
        }
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, Version appVersion) {
        return switch (platform) {
            case IOS, IOS_BUSINESS -> getIosToken(phoneNumber, appVersion, platform.isBusiness());
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
        var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return CompletableFuture.completedFuture(HexFormat.of().formatHex(MD5.calculate(token)));
    }

    public static String generateBusinessCertificate(Keys keys) {
        var details = new BusinessVerifiedNameDetailsBuilder()
                .name("")
                .issuer("smb:wa")
                .serial(Math.abs(new SecureRandom().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(keys.identityKeyPair().privateKey(), encodedDetails, true))
                .build();
        return Base64.getUrlEncoder().encodeToString(BusinessVerifiedNameCertificateSpec.encode(certificate));
    }


    private record WebVersionResponse(@JsonProperty("isBroken") boolean broken,
                                      @JsonProperty("isBelowSoft") boolean outdatedSoft,
                                      @JsonProperty("isBelowHard") boolean outdatedHard,
                                      @JsonProperty("hardUpdateTime") long outdatedUpdateTime,
                                      @JsonProperty("beta") String beta,
                                      @JsonProperty("currentVersion") String currentVersion) {

    }
}