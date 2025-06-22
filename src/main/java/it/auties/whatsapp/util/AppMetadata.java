package it.auties.whatsapp.util;

import com.alibaba.fastjson2.JSON;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class AppMetadata {
    private static volatile Version webVersion;
    private static volatile Version personalIosVersion;
    private static volatile Version businessIosVersion;
    private static volatile WhatsappAndroidApp personalApk;
    private static volatile WhatsappAndroidApp businessApk;

    private static final Path ANDROID_CACHE = Path.of(System.getProperty("user.home") + "/.cobalt/token/android");
    private static final String MOBILE_WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
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
            var result = IosVersionResponse.of(response);
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
            default -> throw new IllegalStateException("Cannot compute token for platform " + platform);
        };
    }

    private static CompletableFuture<String> getIosToken(long phoneNumber, Version version, boolean business) {
        try {
            var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
            var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
            var digest = MessageDigest.getInstance("MD5");
            digest.update(token.getBytes());
            var result = digest.digest();
            return CompletableFuture.completedFuture(HexFormat.of().formatHex(result));
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing md5 implementation", exception);
        }
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

            var result = WhatsappAndroidApp.of(Files.readAllBytes(localCache));
            return Optional.of(result);
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
                var digest = MessageDigest.getInstance("MD5");
                digest.update(apkFile.getFileData("classes.dex"));
                var md5Hash = digest.digest();
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
                var json = apk.toJson();
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
        try {
            var packageNameLength = Strings.utf8Length(packageName);
            var password = new byte[packageNameLength + resource.length];
            var encoder = StandardCharsets.UTF_8.newEncoder();
            var encodeResult = encoder.encode(CharBuffer.wrap(packageName), ByteBuffer.wrap(password, 0, packageNameLength), true);
            if(encodeResult.isError()) {
                throw new RuntimeException("Cannot encode package name: " + encodeResult);
            }
            System.arraycopy(resource, 0, password, packageNameLength, resource.length);
            var mac = Mac.getInstance("HMACSHA1");
            var key = new SecretKeySpec(password, "HMACSHA1");
            mac.init(key);
            var state = new byte[mac.getMacLength()];
            var keySize = 512 / 8;
            var blocks = (keySize + mac.getMacLength() - 1) / mac.getMacLength();
            var iBuf = new byte[4];
            var result = new byte[blocks * mac.getMacLength()];
            var offset = 0;
            for(var i = 1; i <= blocks; ++i) {
                var pos = 3;
                while (++iBuf[pos] == 0) {
                    --pos;
                }

                mac.update(MOBILE_ANDROID_SALT);

                mac.update(iBuf, 0, iBuf.length);
                mac.doFinal(state, 0);
                System.arraycopy(state, 0, result, offset, state.length);

                for(var count = 1; count < 128; ++count) {
                    mac.update(state, 0, state.length);
                    mac.doFinal(state, 0);

                    for(var j = 0; j != state.length; ++j) {
                        result[offset + j] ^= state[j];
                    }
                }

                offset += mac.getMacLength();
            }
            return Arrays.copyOf(result, keySize);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
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
                .signature(Curve25519.sign(keys.identityKeyPair().privateKey(), encodedDetails))
                .build();
        return Base64.getUrlEncoder().encodeToString(BusinessVerifiedNameCertificateSpec.encode(certificate));
    }

    record IosVersionResponse(
            Version version
    ) {
        static IosVersionResponse of(byte[] json) {
            if(json == null) {
                return null;
            }

            var jsonObject = JSON.parseObject(json);
            var results = jsonObject.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            var result = results.getJSONObject(0);
            var version = result.getString("version");
            if (version == null) {
                return null;
            }

            if (!version.startsWith("2.")) {
                version = "2." + version;
            }

            return new IosVersionResponse(Version.of(version));
        }
    }

    record WhatsappAndroidApp(
            Version version,
            byte[] md5Hash,
            byte[] secretKey,
            List<byte[]> certificates,
            boolean business
    ) {
        static WhatsappAndroidApp of(byte[] json) {
            if(json == null) {
                return null;
            }

            var jsonObject = JSON.parseObject(json);
            if(jsonObject == null) {
                return null;
            }

            var versionValue = jsonObject.getString("version");
            var version = Version.of(versionValue);
            var md5Hash = Base64.getDecoder().decode(jsonObject.getString("md5Hash"));
            var secretKey = Base64.getDecoder().decode(jsonObject.getString("secretKey"));
            var certificatesJsonObjects = jsonObject.getJSONArray("certificates");
            var certificates = new ArrayList<byte[]>(certificatesJsonObjects.size());
            for(var i = 0; i < certificatesJsonObjects.size(); i++) {
                certificates.set(i, Base64.getDecoder().decode(certificatesJsonObjects.getString(i)));
            }
            var business = jsonObject.getBooleanValue("business");
            return new WhatsappAndroidApp(version, md5Hash, secretKey, certificates, business);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }
}