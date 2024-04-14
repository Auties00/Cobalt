package it.auties.whatsapp.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Specification.Whatsapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public final class WhatsappMetadata {
    static {
        ProxyAuthenticator.allowAll();
    }

    private static volatile Version webVersion;
    private static volatile Version personalIosVersion;
    private static volatile Version businessIosVersion;


    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<Version> getIosVersion(boolean business) {
        if (business && businessIosVersion != null) {
            return CompletableFuture.completedFuture(businessIosVersion);
        }

        if (!business && personalIosVersion != null) {
            return CompletableFuture.completedFuture(personalIosVersion);
        }

        return Medias.downloadAsync(URI.create(business ? Whatsapp.MOBILE_BUSINESS_IOS_URL : Whatsapp.MOBILE_IOS_URL), Whatsapp.MOBILE_IOS_USER_AGENT).thenApplyAsync(response -> {
            var result = Json.readValue(response, IosVersionResponse.class)
                    .version()
                    .filter(version -> String.valueOf(version.tertiary()).length() != 1 || String.valueOf(version.quaternary()).length() != 1)
                    .orElse(business ? Whatsapp.MOBILE_DEFAULT_BUSINESS_IOS_VERSION : Whatsapp.MOBILE_DEFAULT_PERSONAL_IOS_VERSION);
            if(business) {
                businessIosVersion = result;
            }else {
                personalIosVersion = result;
            }
            return result;
        });
    }

    private static CompletableFuture<Version> getWebVersion() {
        if (webVersion != null) {
            return CompletableFuture.completedFuture(webVersion);
        }

        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(Whatsapp.WEB_UPDATE_URL))
                    .build();
            return client.sendAsync(request, ofString()).thenApplyAsync(response -> {
                var webVersionResponse = Json.readValue(response.body(), WebVersionResponse.class);
                return webVersion = Version.of(webVersionResponse.currentVersion());
            });
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, Version appVersion) {
        return switch (platform) {
            case IOS, IOS_BUSINESS -> getIosToken(phoneNumber, appVersion, platform.isBusiness());
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        var staticToken = business ? Whatsapp.MOBILE_BUSINESS_IOS_STATIC : Whatsapp.MOBILE_IOS_STATIC;
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

    private static final class IosVersionResponse {
        private static final IosVersionResponse EMPTY = new IosVersionResponse(null);
        private final Version version;
        IosVersionResponse(Version version) {
            this.version = version;
        }

        @SuppressWarnings("unchecked")
        @JsonCreator
        public static IosVersionResponse of(Map<String, Object> json) {
            var results = (List<Map<String, Object>>) json.get("results");
            if (results.isEmpty()) {
                return EMPTY;
            }

            var result = (String) results.getFirst().get("version");
            if(result == null) {
                return EMPTY;
            }

            return new IosVersionResponse(Version.of("2." + result));
        }

        public Optional<Version> version() {
            return Optional.of(version);
        }
    }
}