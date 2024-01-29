package it.auties.whatsapp.registration;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.response.WebVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.Specification.Whatsapp;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public final class MobileMetadata {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static volatile Version webVersion;
    private static volatile Version personalIosVersion;
    private static volatile Version businessIosVersion;
    private static volatile WhatsappApk personalApk;
    private static volatile WhatsappApk businessApk;

    private static Path androidCache = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");

    public static void setAndroidCache(Path path) {
        try {
            Files.createDirectories(path);
            androidCache = path;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(platform.isBusiness(), true).thenApply(WhatsappApk::version);
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            case KAIOS ->
                CompletableFuture.completedFuture(Whatsapp.DEFAULT_MOBILE_KAIOS_VERSION);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<Version> getIosVersion(boolean business) {
        return CompletableFuture.completedFuture(Version.of("2.24.1.80"));
       /*
        if (business && businessIosVersion != null) {
            return CompletableFuture.completedFuture(businessIosVersion);
        }

        if (!business && personalIosVersion != null) {
            return CompletableFuture.completedFuture(personalIosVersion);
        }

        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .GET()
                    .header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0")
                    .uri(URI.create(business ? Whatsapp.MOBILE_BUSINESS_IOS_URL : Whatsapp.MOBILE_IOS_URL))
                    .build();
            return client.sendAsync(request, ofString())
                    .thenApplyAsync(response -> {
                        var result = Json.readValue(response.body(), IosVersionResponse.class)
                                .version()
                                .orElseThrow();
                        System.out.println("Version:" + result);
                        if(business) {
                            businessIosVersion = result;
                        }else {
                            personalIosVersion = result;
                        }
                        return result;
                    });
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
        */
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
            return client.sendAsync(request, ofString())
                    .thenApplyAsync(response -> {
                        var webVersionResponse = Json.readValue(response.body(), WebVersionResponse.class);
                        return webVersion = Version.of(webVersionResponse.currentVersion());
                    });
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, Version appVersion, boolean useJarCache) {
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS -> getAndroidToken(String.valueOf(phoneNumber), platform.isBusiness(), useJarCache);
            case IOS, IOS_BUSINESS -> getIosToken(phoneNumber, appVersion, platform.isBusiness());
            case KAIOS ->  getKaiOsToken(phoneNumber);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        var staticToken = business ? Whatsapp.MOBILE_BUSINESS_IOS_STATIC : Whatsapp.MOBILE_IOS_STATIC;
        var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return CompletableFuture.completedFuture(HexFormat.of().formatHex(MD5.calculate(token)));
    }

    private static CompletableFuture<String> getKaiOsToken(long phoneNumber) {
        var staticTokenPart = HexFormat.of().parseHex(Whatsapp.MOBILE_KAIOS_STATIC);
        var pagePart = HexFormat.of().formatHex(Sha256.calculate(BytesHelper.concat(readKaiOsResource("index.html"), readKaiOsResource("backendRoot.js"))));
        var phonePart = String.valueOf(phoneNumber).getBytes(StandardCharsets.UTF_8);
        return CompletableFuture.completedFuture(HexFormat.of().formatHex(Sha256.calculate(BytesHelper.concat(staticTokenPart, pagePart.getBytes(StandardCharsets.UTF_8), phonePart))));
    }

    private static byte[] readKaiOsResource(String name) {
        try (var stream = ClassLoader.getSystemResource("token/kaios/" + name).openStream()) {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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
        if (!business && personalApk != null) {
            return CompletableFuture.completedFuture(personalApk);
        }

        if (business && businessApk != null) {
            return CompletableFuture.completedFuture(businessApk);
        }

        return getCachedApk(business, useJarCache)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadWhatsappApk(business));
    }

    public static CompletableFuture<WhatsappApk> downloadWhatsappApk(boolean business) {
        return Medias.downloadAsync(business ? Whatsapp.MOBILE_BUSINESS_ANDROID_URL : Whatsapp.MOBILE_ANDROID_URL)
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
            if (fileTime.toInstant().until(now, ChronoUnit.DAYS) > 7) {
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
                return businessApk = result;
            }

            var result = new WhatsappApk(version, md5Hash, secretKey.getEncoded(), certificates, false);
            cacheWhatsappData(result);
            return personalApk = result;
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

    public record WhatsappApk(Version version, byte[] md5Hash, byte[] secretKey, Collection<byte[]> certificates, boolean business) {

    }

    public static String generateGpiaToken(byte[] deviceIdentifier, int desiredLength) {
        if (deviceIdentifier == null || desiredLength <= 0) {
            throw new IllegalArgumentException();
        }

        var bytesNeeded = (int) Math.ceil((desiredLength * 3) / 4.0);
        var randomBytes = BytesHelper.random(bytesNeeded - deviceIdentifier.length);
        var tokenBytes = new byte[bytesNeeded];
        System.arraycopy(deviceIdentifier, 0, tokenBytes, 0, deviceIdentifier.length);
        System.arraycopy(randomBytes, 0, tokenBytes, deviceIdentifier.length, randomBytes.length);
        var token = Base64.getEncoder().encodeToString(tokenBytes);
        return token.substring(0, desiredLength);
    }
}