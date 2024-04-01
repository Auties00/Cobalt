package it.auties.whatsapp.registration.metadata;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.PBKDF2;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.*;
import it.auties.whatsapp.util.Specification.Whatsapp;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public final class WhatsappMetadata {
    static {
        ProxyAuthenticator.allowAll();
    }

    private static volatile CompletableFuture<Version> webVersion;
    private static volatile CompletableFuture<Version> personalIosVersion;
    private static volatile CompletableFuture<Version> businessIosVersion;
    private static volatile CompletableFuture<WhatsappAndroidApp> personalApk;
    private static volatile CompletableFuture<WhatsappAndroidApp> businessApk;
    private static volatile CompletableFuture<WhatsappKaiOsApp> kaiOsApp;

    private static final Path androidCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");
    private static final Path kaiOsCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/kaios");

    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(platform.isBusiness()).thenApply(WhatsappAndroidApp::version);
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            case KAIOS ->
                    getKaiOsData().thenApply(WhatsappKaiOsApp::version);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<Version> getIosVersion(boolean business) {
        if (business && businessIosVersion != null) {
            return businessIosVersion;
        }

        if (!business && personalIosVersion != null) {
            return personalIosVersion;
        }

        var future = Medias.downloadAsync(URI.create(business ? Whatsapp.MOBILE_BUSINESS_IOS_URL : Whatsapp.MOBILE_IOS_URL))
                .thenApplyAsync(response -> parseIosVersion(business, response));
        if(business) {
            businessIosVersion = future;
        }else {
            personalIosVersion = future;
        }

        return future;
    }

    private static Version parseIosVersion(boolean business, byte[] response) {
        return Json.readValue(response, IosVersionResponse.class)
                .version()
                .filter(version -> String.valueOf(version.tertiary()).length() != 1 || String.valueOf(version.quaternary()).length() != 1)
                .orElse(business ? Whatsapp.MOBILE_DEFAULT_BUSINESS_IOS_VERSION : Whatsapp.MOBILE_DEFAULT_PERSONAL_IOS_VERSION);
    }

    private static CompletableFuture<Version> getWebVersion() {
        if (webVersion != null) {
            return webVersion;
        }

        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(Whatsapp.WEB_UPDATE_URL))
                    .build();
            return webVersion = client.sendAsync(request, ofString())
                    .thenApplyAsync(WhatsappMetadata::parseWebVersion);
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    private static Version parseWebVersion(HttpResponse<String> response) {
        var webVersionResponse = Json.readValue(response.body(), WebVersionResponse.class);
        return Version.of(webVersionResponse.currentVersion());
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, Version appVersion) {
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS -> getAndroidData(platform.isBusiness())
                    .thenApplyAsync(whatsappData -> getAndroidToken(String.valueOf(phoneNumber), whatsappData));
            case IOS, IOS_BUSINESS -> getIosToken(phoneNumber, appVersion, platform.isBusiness());
            case KAIOS -> getKaiOsData()
                    .thenApplyAsync(kaiOsApp -> getKaiOsToken(phoneNumber, kaiOsApp));
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        var staticToken = business ? Whatsapp.MOBILE_BUSINESS_IOS_STATIC : Whatsapp.MOBILE_IOS_STATIC;
        var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return CompletableFuture.completedFuture(HexFormat.of().formatHex(MD5.calculate(token)));
    }

    private static String getKaiOsToken(long phoneNumber, WhatsappKaiOsApp kaiOsApp) {
        var staticTokenPart = HexFormat.of().parseHex(Whatsapp.MOBILE_KAIOS_STATIC);
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
            whatsappData.certificates().forEach(mac::update);
            mac.update(whatsappData.md5Hash());
            mac.update(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(mac.doFinal()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException throwable) {
            throw new RuntimeException("Cannot compute mobile token", throwable);
        }
    }

    private static CompletableFuture<WhatsappAndroidApp> getAndroidData(boolean business) {
        if (!business && personalApk != null) {
            return personalApk;
        }

        if (business && businessApk != null) {
            return businessApk;
        }

        var future = getCachedAndroidApk(business)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadAndroidData(business));
        if(business) {
            businessApk = future;
        }else {
            personalApk = future;
        }

        return future;
    }

    private static Optional<WhatsappAndroidApp> getCachedAndroidApk(boolean business) {
        try {
            var localCache = getAndroidLocalCache(business);
            if (Files.notExists(localCache)) {
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.DAYS) >= 1) {
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

    private static CompletableFuture<WhatsappAndroidApp> downloadAndroidData(boolean business) {
        return Medias.downloadAsync(business ? Whatsapp.MOBILE_BUSINESS_ANDROID_URL : Whatsapp.MOBILE_ANDROID_URL, (String) null).thenApplyAsync(apk -> {
            try (var apkFile = new ByteArrayApkFile(apk)) {
                var packageName = apkFile.getApkMeta().getPackageName();
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var classes = apkFile.getFileData("classes.dex");
                var md5Hash = MD5.calculate(classes);
                var sha256Hash = Sha256.calculate(classes);
                var compactSha256Hash = Sha256.calculate(Arrays.copyOf(classes, 10));
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificates = getCertificates(apkFile);
                var result = new WhatsappAndroidApp(
                        packageName,
                        version,
                        sha256Hash,
                        compactSha256Hash,
                        md5Hash,
                        secretKey,
                        certificates,
                        classes.length,
                        business
                );
                cacheWhatsappData(result);
                return result;
            } catch (IOException | GeneralSecurityException exception) {
                throw new RuntimeException("Cannot extract certificates from APK", exception);
            }
        });
    }

    private static void cacheWhatsappData(WhatsappAndroidApp apk) {
        CompletableFuture.runAsync(() -> {
            try {
                var json = Json.writeValueAsString(apk, true);
                var file = getAndroidLocalCache(apk.business());
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
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

    private static List<byte[]> getCertificates(ByteArrayApkFile apkFile) throws IOException, CertificateException {
        return apkFile.getApkSingers()
                .stream()
                .map(ApkSigner::getCertificateMetas)
                .flatMap(Collection::stream)
                .map(CertificateMeta::getData)
                .toList();
    }

    private static byte[] getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
        var password = Bytes.concat(packageName.getBytes(StandardCharsets.UTF_8), resource);
        return PBKDF2.hmacSha1With8Bit(password, Whatsapp.MOBILE_ANDROID_SALT, 128, 512);
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
        return Medias.downloadAsync(URI.create(Whatsapp.MOBILE_KAIOS_URL), Whatsapp.MOBILE_KAIOS_USER_AGENT, Map.entry("Content-Type", "application/json")).thenComposeAsync(catalogResponse -> {
            try (var compressedStream = new GZIPInputStream(new ByteArrayInputStream(catalogResponse))) {
                var catalog = Json.readValue(compressedStream.readAllBytes(), KaiOsCatalogResponse.class);
                var app = catalog.apps()
                        .stream()
                        .filter(entry -> Objects.equals(entry.name(), "whatsapp"))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Missing whatsapp from catalog"));
                return Medias.downloadAsync(app.uri(), Whatsapp.MOBILE_KAIOS_USER_AGENT).thenApplyAsync(appArchiveResponse -> {
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

    public static CompletableFuture<String> getGpiaToken(byte[] authKey, boolean business) {
        return getAndroidData(business).thenComposeAsync(androidData -> {
            try(var client = HttpClient.newHttpClient()) {
                var authKeyBase64 = Base64.getEncoder().withoutPadding().encodeToString(authKey);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:1119/gpia?authKey=" + URLEncoder.encode(authKeyBase64, StandardCharsets.UTF_8)))
                        .GET()
                        .build();
                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(response -> {
                    var supportData = Json.readValue(response.body(), GpiaResponse.class);
                    var gpiaData = new GpiaData(
                            Specification.Whatsapp.MOBILE_ANDROID_GPIA_CERTIFICATE,
                            androidData.packageName(),
                            Base64.getEncoder().encodeToString(androidData.sha256Hash()),
                            Base64.getEncoder().encodeToString(androidData.compactSha256Hash()),
                            androidData.size(),
                            supportData.token(),
                            0
                    );
                    var gpiaPayload = AesCbc.encryptAndPrefix(Json.writeValueAsBytes(gpiaData), authKey);
                    return Base64.getUrlEncoder().encodeToString(gpiaPayload);
                });
            }
        });
    }

    private record GpiaResponse(String token) {

    }

    private record GpiaData(String cert, String packageName, String sha256, String shatr, int sizeInBytes, String token, int code) {

    }

    public static CompletableFuture<AndroidCert> getAndroidCert(byte[] authKey, byte[] enc) {
        try(var client = HttpClient.newHttpClient()) {
            var authKeyBase64 = Base64.getUrlEncoder().encodeToString(authKey);
            var encBase64 = Base64.getUrlEncoder().encodeToString(enc);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:1119/cert?authKey=%s&enc=%s".formatted(authKeyBase64, encBase64)))
                    .GET()
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> Json.readValue(response.body(), AndroidCert.class));
        }
    }
}