package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.*;
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
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;

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
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class WhatsappMetadata {
    private static final Version MOBILE_BUSINESS_IOS_VERSION = Version.of("2.24.16.78");
    private static final Version MOBILE_PERSONAL_IOS_VERSION = Version.of("2.24.17.71");
    private static final String MOBILE_KAIOS_USER_AGENT = "Mozilla/5.0 (Mobile; LYF/F90M/LYF-F90M-000-03-31-121219; Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5";
    private static final URI MOBILE_KAIOS_URL = URI.create("https://api.kai.jiophone.net/v2.0/apps?cu=F90M-FBJIINA");
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com/check-update?version=2.2245.9&platform=web");
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";
    private static final String MOBILE_KAIOS_STATIC = "aa8243c465a743c488beb4645dda63edc2ca9a58";
    private static final URI MOBILE_ANDROID_URL = URI.create("https://www.whatsapp.com/android/current/WhatsApp.apk");
    private static final URI MOBILE_BUSINESS_ANDROID_URL = URI.create("https://d.cdnpure.com/b/APK/com.whatsapp.w4b?version=latest");
    private static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");

    private static volatile HttpClient httpClient;
    private static final Object httpClientLock = new Object();

    private static volatile CompletableFuture<Version> webVersion;
    private static volatile CompletableFuture<WhatsappAndroidApp> androidPersonalData;
    private static volatile CompletableFuture<WhatsappAndroidApp> androidBusinessData;
    private static volatile CompletableFuture<WhatsappKaiOsApp> kaiOsApp;

    private static final Path androidCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");
    private static final Path kaiOsCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/kaios");

    public static CompletableFuture<Version> getVersion(CompanionDevice companion) {
        return switch (companion.clientType()) {
            case WEB -> getWebVersion();
            case MOBILE -> switch (companion.platform()) {
                case WINDOWS, MACOS ->
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

        if(companion.address().isPresent()) {
            var future = getAndroidDataFromMiddleware(companion.address().get());
            if(business) {
                androidBusinessData = future;
            }else {
                androidPersonalData = future;
            }

            return future;
        }

        return getCachedApk(business)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadAndCalculateAndroidData(business));
    }

    private static CompletableFuture<WhatsappAndroidApp> downloadAndCalculateAndroidData(boolean business) {
        return Medias.downloadAsync(business ? MOBILE_BUSINESS_ANDROID_URL : MOBILE_ANDROID_URL, (String) null).thenApplyAsync(apk -> {
            try (var apkFile = new ByteArrayApkFile(apk)) {
                var packageName = apkFile.getApkMeta().getPackageName();
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var classes = apkFile.getFileData("classes.dex");
                var md5Hash = MD5.calculate(classes);
                var sha256Hash = Sha256.calculate(classes);
                var compactSha256Hash = Sha256.calculate(Arrays.copyOf(classes, 10));
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificate = getAndroidCertificate(apkFile);
                var certificatesSha1 = Sha1.calculate(certificate);
                var randomDeviceIdentifier = Base64.getEncoder().encodeToString(Bytes.random(14));
                var randomAppIdentifier = Base64.getEncoder().encodeToString(Bytes.random(16));
                var result = new WhatsappAndroidApp(
                        packageName,
                        version,
                        "/data/app/~~S--%s=/%s-%s/base.apk".formatted(randomDeviceIdentifier, packageName, randomAppIdentifier),
                        sha256Hash,
                        compactSha256Hash,
                        classes.length,
                        md5Hash,
                        secretKey,
                        certificate,
                        certificatesSha1,
                        null
                );
                cacheAndroidData(result, business);
                return result;
            } catch (IOException | GeneralSecurityException exception) {
                throw new RuntimeException("Cannot extract certificates from APK", exception);
            }
        });
    }

    private static void cacheAndroidData(WhatsappAndroidApp apk, boolean business) {
        CompletableFuture.runAsync(() -> {
            try {
                var json = Json.writeValueAsString(apk, true);
                var file = getAndroidLocalCache(business);
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
    }

    private static byte[] getAndroidCertificate(ByteArrayApkFile apkFile) throws IOException, CertificateException {
        return apkFile.getApkSingers()
                .stream()
                .map(ApkSigner::getCertificateMetas)
                .flatMap(Collection::stream)
                .map(CertificateMeta::getData)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing android certificate"));
    }

    private static byte[] getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
        var password = Bytes.concat(packageName.getBytes(StandardCharsets.UTF_8), resource);
        return PBKDF2.hmacSha1With8Bit(password, MOBILE_ANDROID_SALT, 128, 512);
    }

    private static byte[] getAboutLogo(ByteArrayApkFile apkFile) throws IOException {
        var resource = apkFile.getFileData("res/drawable-hdpi/about_logo.png");
        if (resource != null) {
            return resource;
        }

        var resourceV4 = apkFile.getFileData("res/drawable-hdpi-v4/about_logo.png");
        if (resourceV4 != null) {
            return resourceV4;
        }

        var xxResourceV4 = apkFile.getFileData("res/drawable-xxhdpi-v4/about_logo.png");
        if (xxResourceV4 != null) {
            return xxResourceV4;
        }

        throw new NoSuchElementException("Missing about_logo.png from apk");
    }

    private static Optional<WhatsappAndroidApp> getCachedApk(boolean business) {
        try {
            var localCache = getAndroidLocalCache(business);
            if (Files.notExists(localCache)) {
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.HOURS) > 12) {
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(localCache), WhatsappAndroidApp.class));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static Path getAndroidLocalCache(boolean business) {
        return androidCache.resolve(business ? "whatsapp_business.json" : "whatsapp.json");
    }

    private static CompletableFuture<WhatsappAndroidApp> getAndroidDataFromMiddleware(String backendAddress) {
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
            var catalog = Json.readValue(catalogResponse, KaiOsCatalogResponse.class);
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
        return getAndroidData(companionDevice)
                .thenComposeAsync(androidData -> getAndroidTokens(companionDevice, authKey, androidData));
    }

    private static CompletableFuture<WhatsappAndroidTokens> getAndroidTokens(CompanionDevice companionDevice, byte[] authKey, WhatsappAndroidApp androidData) {
        return companionDevice.address()
                .map(middleware -> URI.create("%s/integrity?authKey=%s".formatted(middleware, Base64.getUrlEncoder().encodeToString(authKey))))
                .map(endpoint -> getOrCreateClient().getRaw(endpoint).exceptionallyAsync(throwable -> {
                    throw new RuntimeException("Cannot connect to android middleware: " + throwable.getMessage(), throwable);
                }))
                .orElse(CompletableFuture.completedFuture(null))
                .thenApplyAsync(response -> {
                    var token = getGpiaToken(response);
                    var gpiaData = new GpiaData(
                            Base64.getEncoder().encodeToString(androidData.signatureSha1()),
                            androidData.packageName(),
                            Base64.getEncoder().encodeToString(androidData.apkSha256()),
                            Base64.getEncoder().encodeToString(androidData.apkShatr()),
                            token,
                            String.valueOf(androidData.apkSize()),
                            androidData.apkPath(),
                            token == null ? -2 : 0
                    );
                    var gpia = encryptAndroidToken(gpiaData, authKey);
                    var ggData = new GgData(
                            token == null ? -2 : 0,
                            token
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
                    var gpData = new GpData(
                            token == null ? -2 : 0,
                            token
                    );
                    var gp = encryptAndroidToken(gpData, authKey);
                    return new WhatsappAndroidTokens(gpia, gg, gi, gp);
                });
    }

    private static String getGpiaToken(byte[] response) {
        if(response == null) {
            return null;
        }

        var supportData = Json.readValue(response, GpiaResponse.class);
        if(supportData.error() != null) {
            throw new RuntimeException(supportData.error());
        }

        return supportData.token();
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

    private record GpData(
            @JsonProperty("_ic")
            int code,
            @JsonProperty("_it")
            String token
    ) {

    }

    public static CompletableFuture<WhatsappAndroidCert> getAndroidCert(CompanionDevice device, byte[] authKey, byte[] enc) {
        if(device.address().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var authKeyBase64 = URLEncoder.encode(Base64.getEncoder().encodeToString(authKey), StandardCharsets.UTF_8);
        var encBase64 = URLEncoder.encode(Base64.getEncoder().encodeToString(enc), StandardCharsets.UTF_8);
        var middleware = device.address().get();
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
                    value = new HttpClient(HttpClient.Platform.IOS, false);
                    httpClient = value;
                }
            }
        }

        return value;
    }

    public static CompletableFuture<WhatsappIosTokens> getIosTokens(CompanionDevice device, byte[] authKey) {
        if(device.address().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var endpoint = URI.create("%s/integrity?authKey=%s".formatted(device.address().get(), URLEncoder.encode(Base64.getEncoder().encodeToString(authKey), StandardCharsets.UTF_8)));
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
