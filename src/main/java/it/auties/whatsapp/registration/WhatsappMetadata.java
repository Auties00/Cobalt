package it.auties.whatsapp.registration;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.response.IosVersionResponse;
import it.auties.whatsapp.model.response.KaiOsCatalogResponse;
import it.auties.whatsapp.model.response.WebVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.*;
import it.auties.whatsapp.util.Specification.Whatsapp;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
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

    private static volatile Version webVersion;
    private static volatile Version personalIosVersion;
    private static volatile Version businessIosVersion;
    private static volatile WhatsappApk personalApk;
    private static volatile WhatsappApk businessApk;
    private static volatile KaiOsApp kaiOsApp;

    private static final Path androidCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");
    private static final Path kaiOsCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/kaios");

    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(platform.isBusiness()).thenApply(WhatsappApk::version);
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            case KAIOS ->
                    getKaiOsData().thenApply(KaiOsApp::version);
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

    private static String getKaiOsToken(long phoneNumber, KaiOsApp kaiOsApp) {
        var staticTokenPart = HexFormat.of().parseHex(Whatsapp.MOBILE_KAIOS_STATIC);
        var pagePart = HexFormat.of().formatHex(Sha256.calculate(Bytes.concat(kaiOsApp.indexHtml(), kaiOsApp.backendJs())));
        var phonePart = String.valueOf(phoneNumber).getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(Sha256.calculate(Bytes.concat(staticTokenPart, pagePart.getBytes(StandardCharsets.UTF_8), phonePart)));
    }

    private static String getAndroidToken(String phoneNumber, WhatsappApk whatsappData) {
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

    private static CompletableFuture<WhatsappApk> getAndroidData(boolean business) {
        if (!business && personalApk != null) {
            return CompletableFuture.completedFuture(personalApk);
        }

        if (business && businessApk != null) {
            return CompletableFuture.completedFuture(businessApk);
        }

        return getCachedAndroidApk(business)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadAndroidData(business));
    }

    private static Optional<WhatsappApk> getCachedAndroidApk(boolean business) {
        try {
            var localCache = getAndroidLocalCache(business);
            if (Files.notExists(localCache)) {
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.DAYS) > 7) {
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(localCache), WhatsappApk.class));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static Path getAndroidLocalCache(boolean business) {
        return androidCache.resolve(business ? "whatsapp_business.json" : "whatsapp.json");
    }

    private static CompletableFuture<WhatsappApk> downloadAndroidData(boolean business) {
        return Medias.downloadAsync(business ? Whatsapp.MOBILE_BUSINESS_ANDROID_URL : Whatsapp.MOBILE_ANDROID_URL, (String) null).thenApplyAsync(apk -> {
            try (var apkFile = new ByteArrayApkFile(apk)) {
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var md5Hash = MD5.calculate(apkFile.getFileData("classes.dex"));
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificates = getCertificates(apkFile);
                if (business) {
                    var result = new WhatsappApk(version, md5Hash, secretKey.getEncoded(), certificates, true);
                    cacheWhatsappData(result);
                    return businessApk = result;
                }

                var result = new WhatsappApk(version, md5Hash, secretKey.getEncoded(), certificates, false);
                cacheWhatsappData(result);
                return personalApk = result;
            } catch (IOException | GeneralSecurityException exception) {
                throw new RuntimeException("Cannot extract certificates from APK", exception);
            }
        });
    }

    private static void cacheWhatsappData(WhatsappApk apk) {
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

    private static SecretKey getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
        var result = Bytes.concat(packageName.getBytes(StandardCharsets.UTF_8), resource);
        var whatsappLogoChars = new char[result.length];
        for (var i = 0; i < result.length; i++) {
            whatsappLogoChars[i] = (char) result[i];
        }
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT");
        var key = new PBEKeySpec(whatsappLogoChars, Whatsapp.MOBILE_ANDROID_SALT, 128, 512);
        return factory.generateSecret(key);
    }

    private static Path getKaiOsLocalCache() {
        return kaiOsCache.resolve("whatsapp.json");
    }

    private static CompletableFuture<KaiOsApp> getKaiOsData() {
        if (kaiOsApp != null) {
            return CompletableFuture.completedFuture(kaiOsApp);
        }

        return getCachedKaiOsApp()
                .map(CompletableFuture::completedFuture)
                .orElseGet(WhatsappMetadata::downloadKaiOsData);
    }

    private static Optional<KaiOsApp> getCachedKaiOsApp() {
        try {
            var localCache = getKaiOsLocalCache();
            if (Files.notExists(localCache)) {
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.DAYS) > 7) {
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(localCache), KaiOsApp.class));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static CompletableFuture<KaiOsApp> downloadKaiOsData() {
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

                        var result = new KaiOsApp(
                                new Version(app.version().primary(), app.version().secondary(), app.version().tertiary()),
                                Objects.requireNonNull(indexHtml, "Missing index.html"),
                                Objects.requireNonNull(backendJs, "Missing backendRoot.js")
                        );
                        cacheKaiOsData(kaiOsApp = result);
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

    private static void cacheKaiOsData(KaiOsApp app) {
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

    private record WhatsappApk(Version version, byte[] md5Hash, byte[] secretKey, List<byte[]> certificates, boolean business) {

    }

    private record KaiOsApp(Version version, byte[] indexHtml, byte[] backendJs) {

    }

    public static CompletableFuture<String> generateGpiaToken(UUID advertisingId, byte[] deviceIdentifier, boolean business) {
        return getAndroidData(business).thenApplyAsync(androidData -> {
            var uuidBytes = uuidToBytes(advertisingId);
            var combinedBytes = Bytes.concat(deviceIdentifier, androidData.certificates().getFirst(), androidData.secretKey(), uuidBytes);
            var randomBytes = Bytes.random(Math.max(0, Specification.Whatsapp.GPIA_TOKEN_LENGTH - combinedBytes.length));
            combinedBytes = Bytes.concat(combinedBytes, randomBytes);
            var hashedBytes = Sha256.calculate(combinedBytes);
            var truncatedBytes = new byte[Specification.Whatsapp.GPIA_TOKEN_LENGTH];
            System.arraycopy(hashedBytes, 0, truncatedBytes, 0, Math.min(hashedBytes.length, Specification.Whatsapp.GPIA_TOKEN_LENGTH));
            var thirdHeaderRandom = Bytes.random(Specification.Whatsapp.GPIA_TOKEN_LENGTH - hashedBytes.length);
            System.arraycopy(thirdHeaderRandom, 0, truncatedBytes, hashedBytes.length, thirdHeaderRandom.length);
            return Base64.getEncoder().encodeToString(truncatedBytes);
        });
    }

    private static byte[] uuidToBytes(UUID uuid) {
        var bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}