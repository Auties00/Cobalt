package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.Pbkdf2;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetailsSpec;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.Version;
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
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AppMetadata {
    private static volatile Version webVersion;
    private static volatile Version personalIosVersion;
    private static volatile Version businessIosVersion;
    private static volatile WhatsappAndroidApp personalApk;
    private static volatile WhatsappAndroidApp businessApk;
    private static volatile WhatsappKaiOsApp kaiOsApp;

    private static final Path ANDROID_CACHE = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");
    private static final Path KAI_OS_CACHE = Path.of(System.getProperty("user.home") + "/.cobalt/token/kaios");
    private static final String MOBILE_WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    private static final URI MOBILE_KAIOS_URL = URI.create("https://api.kai.jiophone.net/v2.0/apps?cu=F90M-FBJIINA");
    private static final String MOBILE_KAIOS_USER_AGENT = "Mozilla/5.0 (Mobile; LYF/F90M/LYF-F90M-000-03-31-121219; Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5";
    private static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");
    private static final Version WEB_VERSION = Version.of("2.3000.1022032575");
    private static final Version MOBILE_BUSINESS_IOS_VERSION = Version.of("2.25.10.72");
    private static final Version MOBILE_PERSONAL_IOS_VERSION = Version.of("2.25.10.72");
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com");
    private static final Pattern WEB_UPDATE_PATTERN = Pattern.compile("\"client_revision\":(\\w+)", Pattern.MULTILINE);
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";
    private static final URI MOBILE_ANDROID_URL = URI.create("https://www.whatsapp.com/android/current/WhatsApp.apk");
    private static final URI MOBILE_BUSINESS_ANDROID_URL = URI.create("https://d.cdnpure.com/b/APK/com.whatsapp.w4b?version=latest");
    private static final String MOBILE_KAIOS_STATIC = "aa8243c465a743c488beb4645dda63edc2ca9a58";
    private static final URI MOBILE_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsApp");
    private static final URI MOBILE_BUSINESS_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsAppSMB");
    private static final String MOBILE_IOS_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1";
   private static final String MOBILE_ANDROID_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

    public static CompletableFuture<Version> getVersion(PlatformType platform) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(platform.isBusiness())
                            .thenApply(WhatsappAndroidApp::version);
            case IOS ->
                    getIosVersion(false);
            case IOS_BUSINESS ->
                    getIosVersion(true);
            case KAIOS ->
                    getKaiOsData()
                            .thenApply(WhatsappKaiOsApp::version);
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
                    .header("User-Agent", MOBILE_WEB_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(response -> {
                if(response.statusCode() != 200) {
                    return WEB_VERSION;
                }

                return WEB_UPDATE_PATTERN.matcher(response.body())
                        .results()
                        .findFirst()
                        .map(entry -> {
                            try {
                                var clientVersion = Integer.parseUnsignedInt(entry.group(1));
                                return webVersion = new Version(2, 3000, clientVersion);
                            }catch (Throwable throwable) {
                                return WEB_VERSION;
                            }
                        })
                        .orElse(WEB_VERSION);
            });
        }
    }

    private static CompletableFuture<Version> getIosVersion(boolean business) {
        if (business && businessIosVersion != null) {
            return CompletableFuture.completedFuture(businessIosVersion);
        }

        if (!business && personalIosVersion != null) {
            return CompletableFuture.completedFuture(personalIosVersion);
        }

        return Medias.downloadAsync(business ? MOBILE_BUSINESS_IOS_URL : MOBILE_IOS_URL, null, MOBILE_IOS_USER_AGENT).thenApplyAsync(response -> {
            var result = Json.readValue(response, IosVersionResponse.class);
            if(result == null) {
                return business ? MOBILE_BUSINESS_IOS_VERSION : MOBILE_PERSONAL_IOS_VERSION;
            }

            if(business) {
                businessIosVersion = result.version;
            }else {
                personalIosVersion = result.version;
            }
            return result.version;
        });
    }

    public static CompletableFuture<String> getToken(long phoneNumber, PlatformType platform, Version appVersion) {
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidData(platform.isBusiness())
                            .thenApplyAsync(whatsappData -> getAndroidToken(String.valueOf(phoneNumber), whatsappData));
            case IOS, IOS_BUSINESS ->
                    getIosToken(phoneNumber, appVersion, platform.isBusiness());
            case KAIOS ->
                    getKaiOsData()
                            .thenApplyAsync(kaiOsApp -> getKaiOsToken(phoneNumber, kaiOsApp));
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
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
            return CompletableFuture.completedFuture(personalApk);
        }

        if (business && businessApk != null) {
            return CompletableFuture.completedFuture(businessApk);
        }

        return getCachedAndroidApk(business)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadAndroidData(business));
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
        return ANDROID_CACHE.resolve(business ? "whatsapp_business.json" : "whatsapp.json");
    }

    private static CompletableFuture<WhatsappAndroidApp> downloadAndroidData(boolean business) {
        return Medias.downloadAsync(business ? MOBILE_BUSINESS_ANDROID_URL : MOBILE_ANDROID_URL, null, MOBILE_ANDROID_USER_AGENT).thenApplyAsync(apk -> {
            try (var apkFile = new ByteArrayApkFile(apk)) {
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var md5Hash = MD5.calculate(apkFile.getFileData("classes.dex"));
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificates = getCertificates(apkFile);
                if (business) {
                    var result = new WhatsappAndroidApp(version, md5Hash, secretKey, certificates, true);
                    cacheWhatsappData(result);
                    return businessApk = result;
                }else {
                    var result = new WhatsappAndroidApp(version, md5Hash, secretKey, certificates, false);
                    cacheWhatsappData(result);
                    return personalApk = result;
                }
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
        return Pbkdf2.hmacSha1With8Bit(password, MOBILE_ANDROID_SALT, 128, 512);
    }

    private static Path getKaiOsLocalCache() {
        return KAI_OS_CACHE.resolve("whatsapp.json");
    }

    // https://faq.whatsapp.com/420008397294796
    // Leaving it here just for research
    private static CompletableFuture<WhatsappKaiOsApp> getKaiOsData() {
        if (kaiOsApp != null) {
            return CompletableFuture.completedFuture(kaiOsApp);
        }

        return getCachedKaiOsApp()
                .map(CompletableFuture::completedFuture)
                .orElseGet(AppMetadata::downloadKaiOsData);
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
        return Medias.downloadAsync(MOBILE_KAIOS_URL, null, MOBILE_KAIOS_USER_AGENT, Map.entry("Content-Type", "application/json")).thenComposeAsync(catalogResponse -> {
            try (var compressedStream = new GZIPInputStream(new ByteArrayInputStream(catalogResponse))) {
                var catalog = Json.readValue(compressedStream.readAllBytes(), KaiOsCatalogResponse.class);
                var app = catalog.apps()
                        .stream()
                        .filter(entry -> Objects.equals(entry.name(), "whatsapp"))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Missing whatsapp from catalog"));
                return Medias.downloadAsync(app.uri(), null, MOBILE_KAIOS_USER_AGENT).thenApplyAsync(appArchiveResponse -> {
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

    private record IosVersionResponse(
            Version version
    ) {
        @SuppressWarnings("unchecked")
        @JsonCreator
        public static IosVersionResponse of(Map<String, Object> json) {
            var results = (List<Map<String, Object>>) json.get("results");
            if (results.isEmpty()) {
                return null;
            }

            var result = (String) results.getFirst().get("version");
            if (result == null) {
                return null;
            }

            if (!result.startsWith("2.")) {
                result = "2." + result;
            }

            return new IosVersionResponse(Version.of(result));
        }
    }

    private record WhatsappAndroidApp(
            Version version,
            byte[] md5Hash,
            byte[] secretKey,
            List<byte[]> certificates,
            boolean business
    ) {

    }

    private record WhatsappKaiOsApp(
            Version version,
            byte[] indexHtml,
            byte[] backendJs
    ) {

    }

    private record KaiOsCatalogResponse(
            List<App> apps
    ) {
        record App(
                String name,
                Version version,
                @JsonProperty("package_path")
                URI uri
        ) {

        }
    }
}