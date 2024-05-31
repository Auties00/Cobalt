package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.net.HttpClient;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Medias;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class WhatsappMetadata {
    private static final Version MOBILE_BUSINESS_IOS_VERSION = Version.of("2.24.10.79");
    private static final Version MOBILE_PERSONAL_IOS_VERSION = Version.of("2.24.10.79");
    private static final String MOBILE_KAIOS_USER_AGENT = "Mozilla/5.0 (Mobile; LYF/F90M/LYF-F90M-000-03-31-121219; Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5";
    private static final URI MOBILE_KAIOS_URL = URI.create("https://api.kai.jiophone.net/v2.0/apps?cu=F90M-FBJIINA");
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com/check-update?version=2.2245.9&platform=web");
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";
    private static final String MOBILE_KAIOS_STATIC = "aa8243c465a743c488beb4645dda63edc2ca9a58";

    private static volatile HttpClient httpClient;
    private static final Object httpClientLock = new Object();

    private static volatile CompletableFuture<Version> webVersion;
    private static volatile CompletableFuture<WhatsappAndroidApp> androidPersonalData;
    private static volatile CompletableFuture<WhatsappAndroidApp> androidBusinessData;
    private static volatile CompletableFuture<WhatsappKaiOsApp> kaiOsApp;

    private static final Path kaiOsCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/kaios");

    public static CompletableFuture<Version> getVersion(CompanionDevice companion) {
        return switch (companion.platform()) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(companion)
                            .thenApply(WhatsappAndroidApp::version);
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            case KAIOS ->
                    getKaiOsData().thenApply(WhatsappKaiOsApp::version);
            default -> throw new IllegalStateException("Unsupported mobile os: " + companion.platform());
        };
    }

    private static CompletableFuture<Version> getIosVersion(boolean business) {
        return CompletableFuture.completedFuture(business ? MOBILE_BUSINESS_IOS_VERSION : MOBILE_PERSONAL_IOS_VERSION);
    }

    private static CompletableFuture<Version> getWebVersion() {
        if (webVersion != null) {
            return webVersion;
        }

        return webVersion = getOrCreateClient().getString(WEB_UPDATE_URL)
                .thenApplyAsync(WhatsappMetadata::parseWebVersion);
    }

    private static Version parseWebVersion(String response) {
        var webVersionResponse = Json.readValue(response, WebVersionResponse.class);
        return Version.of(webVersionResponse.currentVersion());
    }

    public static CompletableFuture<String> getToken(CompanionDevice companion, Version appVersion, long phoneNumber) {
        return switch (companion.platform()) {
            case ANDROID, ANDROID_BUSINESS -> getAndroidData(companion)
                    .thenApplyAsync(whatsappData -> getAndroidToken(String.valueOf(phoneNumber), whatsappData));
            case IOS, IOS_BUSINESS -> getIosToken(phoneNumber, appVersion, companion.platform().isBusiness());
            case KAIOS -> getKaiOsData()
                    .thenApplyAsync(kaiOsApp -> getKaiOsToken(phoneNumber, kaiOsApp));
            default -> throw new IllegalStateException("Unsupported mobile os: " + companion.platform());
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
        var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return CompletableFuture.completedFuture(HexFormat.of().formatHex(MD5.calculate(token)));
    }

    private static String getKaiOsToken(long phoneNumber, WhatsappKaiOsApp kaiOsApp) {
        var staticTokenPart = HexFormat.of().parseHex(MOBILE_KAIOS_STATIC);
        var pagePart = HexFormat.of().formatHex(Sha256.calculate(Bytes.concat(kaiOsApp.indexHtml(), kaiOsApp.backendJs())));
        var phonePart = String.valueOf(phoneNumber).getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(Sha256.calculate(Bytes.concat(staticTokenPart, pagePart.getBytes(StandardCharsets.UTF_8), phonePart)));
    }

    private static String getAndroidToken(String phoneNumber, WhatsappAndroidApp whatsappData) {
        try {
            var mac = Mac.getInstance("HMACSHA1");
            var secretKeyBytes = whatsappData.secretKey();
            var secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "PBKDF2");
            mac.init(secretKey);
            mac.update(whatsappData.signature());
            mac.update(whatsappData.classesMd5());
            mac.update(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(mac.doFinal()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException throwable) {
            throw new RuntimeException("Cannot compute mobile token", throwable);
        }
    }

    private static CompletableFuture<WhatsappAndroidApp> getAndroidData(CompanionDevice companion) {
        var business = companion.platform().isBusiness();
        if (!business && androidPersonalData != null) {
            return androidPersonalData;
        }

        if (business && androidBusinessData != null) {
            return androidBusinessData;
        }

        var future = downloadAndroidData(companion);
        if(business) {
            androidBusinessData = future;
        }else {
            androidPersonalData = future;
        }

        return future;
    }

    private static CompletableFuture<WhatsappAndroidApp> downloadAndroidData(CompanionDevice companion) {
        var backendAddress = getMiddlewareEndpoint(companion)
                .orElseThrow(() -> new IllegalArgumentException("Cannot access android middleware"));
        return getOrCreateClient().getRaw(URI.create("%s/info".formatted(backendAddress))).thenApplyAsync(response -> {
            var app = Json.readValue(response, WhatsappAndroidApp.class);
            if (app.error() != null) {
                throw new RuntimeException(app.error());
            }

            return app;
        }).exceptionallyAsync(throwable -> {
            throw new RuntimeException("Cannot connect to android middleware at " + backendAddress, throwable);
        });
    }

    private static Path getKaiOsLocalCache() {
        return kaiOsCache.resolve("whatsapp.json");
    }

    private static CompletableFuture<WhatsappKaiOsApp> getKaiOsData() {
        if (kaiOsApp != null) {
            return kaiOsApp;
        }

        return kaiOsApp = getCachedKaiOsApp()
                .map(CompletableFuture::completedFuture)
                .orElseGet(WhatsappMetadata::downloadKaiOsData);
    }

    private static Optional<WhatsappKaiOsApp> getCachedKaiOsApp() {
        try {
            var localCache = getKaiOsLocalCache();
            if (Files.notExists(localCache)) {
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.DAYS) >= 1) {
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(localCache), WhatsappKaiOsApp.class));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static CompletableFuture<WhatsappKaiOsApp> downloadKaiOsData() {
        return Medias.downloadAsync(MOBILE_KAIOS_URL, MOBILE_KAIOS_USER_AGENT, Map.entry("Content-Type", "application/json")).thenComposeAsync(catalogResponse -> {
            try (var compressedStream = new GZIPInputStream(new ByteArrayInputStream(catalogResponse))) {
                var catalog = Json.readValue(compressedStream.readAllBytes(), KaiOsCatalogResponse.class);
                var app = catalog.apps()
                        .stream()
                        .filter(entry -> Objects.equals(entry.name(), "whatsapp"))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Missing whatsapp from catalog"));
                return Medias.downloadAsync(app.uri(), MOBILE_KAIOS_USER_AGENT).thenApplyAsync(appArchiveResponse -> {
                    try (var zipArchive = new ZipInputStream(new ByteArrayInputStream(appArchiveResponse))) {
                        byte[] indexHtml = null;
                        byte[] backendJs = null;
                        ZipEntry entry;

                        while ((entry = zipArchive.getNextEntry()) != null) {
                            switch (entry.getName()) {
                                case "index.html" -> indexHtml = zipArchive.readAllBytes();
                                case "backendRoot.js" -> backendJs = zipArchive.readAllBytes();
                            }
                        }

                        var result = new WhatsappKaiOsApp(
                                new Version(app.version().primary(), app.version().secondary(), app.version().tertiary()),
                                Objects.requireNonNull(indexHtml, "Missing index.html"),
                                Objects.requireNonNull(backendJs, "Missing backendRoot.js")
                        );
                        cacheKaiOsData(result);
                        return result;
                    } catch (IOException exception) {
                        throw new RuntimeException("Cannot extract kaios metadata", exception);
                    }
                });
            } catch (IOException exception) {
                throw new RuntimeException("Cannot download kaios app", exception);
            }
        });
    }

    private static void cacheKaiOsData(WhatsappKaiOsApp app) {
        CompletableFuture.runAsync(() -> {
            try {
                var json = Json.writeValueAsString(app, true);
                var file = getKaiOsLocalCache();
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
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

    public static CompletableFuture<WhatsappAndroidTokens> getAndroidTokens(CompanionDevice companionDevice, byte[] authKey) {
        return getAndroidData(companionDevice).thenComposeAsync(androidData -> {
            var middleware = getMiddlewareEndpoint(companionDevice)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot access android middleware"));
            var endpoint = URI.create("%s/integrity?authKey=%s".formatted(middleware, Base64.getUrlEncoder().encodeToString(authKey)));
            return getOrCreateClient().getRaw(endpoint).thenApplyAsync(response -> {
                var supportData = Json.readValue(response, GpiaResponse.class);
                if(supportData.error() != null) {
                    throw new RuntimeException(supportData.error());
                }
                var gpiaData = new GpiaData(
                        Base64.getEncoder().encodeToString(androidData.signatureSha1()),
                        androidData.packageName(),
                        Base64.getEncoder().encodeToString(androidData.apkSha256()),
                        Base64.getEncoder().encodeToString(androidData.apkShatr()),
                        supportData.token(),
                        String.valueOf(androidData.apkSize()),
                        androidData.apkPath(),
                        0
                );
                var gpia = encryptAndroidToken(gpiaData, authKey);
                var ggData = new GgData(
                        0,
                        supportData.token()
                );
                var gg = encryptAndroidToken(ggData, authKey);
                var giData = new GiData(
                        Base64.getEncoder().encodeToString(androidData.signatureSha1()),
                        androidData.apkPath(),
                        Base64.getEncoder().encodeToString(androidData.apkSha256()),
                        Base64.getEncoder().encodeToString(androidData.apkShatr()),
                        androidData.packageName(),
                        String.valueOf(androidData.apkSize())
                );
                var gi = encryptAndroidToken(giData, authKey);
                return new WhatsappAndroidTokens(gpia, gg, gi);
            }).exceptionallyAsync(throwable -> {
                throw new RuntimeException("Cannot connect to android middleware: " + throwable.getMessage(), throwable);
            });
        });
    }

    private static String encryptAndroidToken(Object data, byte[] authKey) {
        var jsonData = Json.writeValueAsString(data)
                .replaceAll(" ", "")
                .replaceAll("/", "\\\\/");
        var payload = AesCbc.encryptAndPrefix(
                jsonData.getBytes(),
                Sha256.calculate(Base64.getEncoder().encodeToString(authKey))
        );
        return URLEncoder.encode(Base64.getEncoder().encodeToString(payload), StandardCharsets.UTF_8);
    }

    private static Optional<String> getMiddlewareEndpoint(CompanionDevice device) {
        if(device.address().isEmpty() && device.platform().isAndroid()) {
            throw new IllegalArgumentException("Please specify the address of the physical device to use as explained in android/README.md");
        }

        return device.address();
    }

    private record GpiaResponse(
            String token,
            String error
    ) {

    }

    private record GpiaData(
            @JsonProperty("cert")
            String apkSha1,
            @JsonProperty("packageName")
            String packageName,
            @JsonProperty("sha256")
            String sha256,
            @JsonProperty("shatr")
            String shatr,
            @JsonProperty("token")
            String token,
            @JsonProperty("sizeInBytes")
            String sizeInBytes,
            @JsonProperty("p")
            String apkPath,
            @JsonProperty("code")
            int code
    ) {

    }

    private record GiData(
            @JsonProperty("_icr")
            String apkSha1,
            @JsonProperty("_p")
            String apkPath,
            @JsonProperty("_is")
            String sha256,
            @JsonProperty("_ist")
            String shatr,
            @JsonProperty("_ip")
            String packageName,
            @JsonProperty("_isb")
            String sizeInBytes
    ) {

    }

    private record GgData(
            @JsonProperty("_ic")
            int code,
            @JsonProperty("_it")
            String token
    ) {

    }

    public static CompletableFuture<WhatsappAndroidCert> getAndroidCert(CompanionDevice device, byte[] authKey, byte[] enc) {
        var authKeyBase64 = URLEncoder.encode(Base64.getEncoder().encodeToString(authKey), StandardCharsets.UTF_8);
        var encBase64 = URLEncoder.encode(Base64.getEncoder().encodeToString(enc), StandardCharsets.UTF_8);
        var middleware = getMiddlewareEndpoint(device)
                .orElseThrow(() -> new IllegalArgumentException("Cannot access android middleware"));
        var endpoint = URI.create("%s/cert?authKey=%s&enc=%s".formatted(middleware, authKeyBase64, encBase64));
        return getOrCreateClient().getRaw(endpoint).thenApplyAsync(response -> {
            var cert = Json.readValue(response, WhatsappAndroidCert.class);
            if(cert.error() != null) {
                throw new RuntimeException(cert.error());
            }
            return cert;
        }).exceptionallyAsync(throwable -> {
            throw new RuntimeException("Cannot connect to android middleware: " + throwable.getMessage());
        });
    }

    private static HttpClient getOrCreateClient() {
        var value = httpClient;
        if (value == null) {
            synchronized (httpClientLock) {
                value = httpClient;
                if (value == null) {
                    value = new HttpClient();
                    httpClient = value;
                }
            }
        }

        return value;
    }

    public static CompletableFuture<WhatsappIosTokens> getIosTokens(CompanionDevice device, byte[] authKey) {
        var middleware = getMiddlewareEndpoint(device);
        if(middleware.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var endpoint = URI.create("%s/integrity?authKey=%s".formatted(middleware.get(), URLEncoder.encode(Base64.getEncoder().encodeToString(authKey), StandardCharsets.UTF_8)));
        return getOrCreateClient().getRaw(endpoint).thenApplyAsync(response -> {
            var supportData = Json.readValue(response, IntegrityResponse.class);
            if (supportData.error() != null) {
                throw new RuntimeException(supportData.error());
            }

            var assertion = Base64.getUrlEncoder().encodeToString(Json.writeValueAsBytes(new IntegrityAssertion(supportData.assertion())));
            return new WhatsappIosTokens(supportData.attestation(), assertion);
        }).exceptionallyAsync(throwable -> {
            throw new RuntimeException("Cannot connect to ios middleware: " + throwable.getMessage());
        });
    }

    private record IntegrityResponse(
            String attestation,
            String assertion,
            String error
    ) {

    }

    private record IntegrityAssertion(String assertion) {

    }
}