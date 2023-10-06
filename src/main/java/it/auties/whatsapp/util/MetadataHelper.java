package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.response.WebVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.Specification.Whatsapp;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public final class MetadataHelper {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static volatile Version webVersion;
    private static volatile Version iosVersion;
    private static volatile WhatsappApk cachedApk;
    private static volatile WhatsappApk cachedBusinessApk;

    private static Path androidCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");

    public static void setAndroidCache(@NonNull Path path) {
        try {
            Files.createDirectories(path);
            androidCache = path;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static CompletableFuture<Version> getVersion(UserAgent.PlatformType platform) {
        return getVersion(platform, true);
    }

    public static CompletableFuture<Version> getVersion(UserAgent.PlatformType platform, boolean useJarCache) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS -> getWebVersion();
            case ANDROID -> getAndroidData(platform.isBusiness(), useJarCache)
                    .thenApply(WhatsappApk::version);
            case IOS -> CompletableFuture.completedFuture(Whatsapp.DEFAULT_MOBILE_IOS_VERSION); // Fetching the latest ios version is harder than one might hope
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static Version getDefaultIosVersion() {
        return Whatsapp.DEFAULT_MOBILE_IOS_VERSION;
    }

    private static CompletableFuture<Version> getWebVersion() {
        try (var client = HttpClient.newHttpClient()) {
            if (webVersion != null) {
                return CompletableFuture.completedFuture(webVersion);
            }

            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(Whatsapp.WEB_UPDATE_URL))
                    .build();
            return client.sendAsync(request, ofString())
                    .thenApplyAsync(response -> Json.readValue(response.body(), WebVersionResponse.class))
                    .thenApplyAsync(version -> webVersion = Version.of(version.currentVersion()));
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, boolean useJarCache) {
        return switch (platform) {
            case ANDROID -> getAndroidToken(String.valueOf(phoneNumber), platform.isBusiness(), useJarCache);
            case IOS -> getIosToken(phoneNumber, platform, useJarCache);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, UserAgent.PlatformType platform, boolean useJarCache) {
        return getVersion(platform, useJarCache)
                .thenApply(version -> getIosToken(phoneNumber, version));
    }

    private static String getIosToken(long phoneNumber, Version version) {
        var token = Whatsapp.MOBILE_IOS_STATIC + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return HexFormat.of().formatHex(MD5.calculate(token));
    }

    private static CompletableFuture<String> getAndroidToken(String phoneNumber, boolean business, boolean useJarCache) {
        return getAndroidData(business, useJarCache)
                .thenApplyAsync(whatsappData -> getAndroidToken(phoneNumber, whatsappData));
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

    private static CompletableFuture<WhatsappApk> getAndroidData(boolean business, boolean useJarCache) {
        if (!business && cachedApk != null) {
            return CompletableFuture.completedFuture(cachedApk);
        }

        if (business && cachedBusinessApk != null) {
            return CompletableFuture.completedFuture(cachedBusinessApk);
        }

        return getCachedApk(business, useJarCache)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadWhatsappApk(business));
    }

    public static CompletableFuture<WhatsappApk> downloadWhatsappApk(boolean business) {
        return Medias.downloadAsync(business ? Whatsapp.MOBILE_BUSINESS_DOWNLOAD_URL : Whatsapp.MOBILE_DOWNLOAD_URL)
                .thenApplyAsync(result -> getAndroidData(result, business));
    }

    private static Optional<WhatsappApk> getCachedApk(boolean business, boolean useJarCache) {
        try {
            var localCache = getAndroidLocalCache(business);
            if (Files.notExists(localCache)) {
                if (useJarCache) {
                    var jarCache = getAndroidJarCache(business);
                    return Optional.of(Json.readValue(Files.readString(jarCache), WhatsappApk.class));
                }

                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(localCache);
            if (fileTime.toInstant().until(now, ChronoUnit.WEEKS) > 1) {
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(localCache), WhatsappApk.class));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static Path getAndroidJarCache(boolean business) throws URISyntaxException {
        var url = business
                ? ClassLoader.getSystemResource("token/android/whatsapp_business.json")
                : ClassLoader.getSystemResource("token/android/whatsapp.json");
        return Path.of(url.toURI());
    }

    private static Path getAndroidLocalCache(boolean business) {
        return androidCache.resolve(business ? "whatsapp_business.json" : "whatsapp.json");
    }

    private static WhatsappApk getAndroidData(byte[] apk, boolean business) {
        try (var apkFile = new ByteArrayApkFile(apk)) {
            var version = Version.of(apkFile.getApkMeta().getVersionName());
            var md5Hash = MD5.calculate(apkFile.getFileData("classes.dex"));
            var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
            var certificates = getCertificates(apkFile);
            if (business) {
                var result = new WhatsappApk(version, md5Hash, secretKey.getEncoded(), certificates, true);
                cacheWhatsappData(result);
                return cachedBusinessApk = result;
            }

            var result = new WhatsappApk(version, md5Hash, secretKey.getEncoded(), certificates, false);
            cacheWhatsappData(result);
            return cachedApk = result;
        } catch (IOException | GeneralSecurityException exception) {
            throw new RuntimeException("Cannot extract certificates from APK", exception);
        }
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
        var result = BytesHelper.concat(packageName.getBytes(StandardCharsets.UTF_8), resource);
        var whatsappLogoChars = new char[result.length];
        for (var i = 0; i < result.length; i++) {
            whatsappLogoChars[i] = (char) result[i];
        }
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT");
        var key = new PBEKeySpec(whatsappLogoChars, Whatsapp.MOBILE_ANDROID_SALT, 128, 512);
        return factory.generateSecret(key);
    }

    public record WhatsappApk(Version version, byte[] md5Hash, byte[] secretKey, Collection<byte[]> certificates, boolean business) {

    }
}