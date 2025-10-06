package com.github.auties00.cobalt.util;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.auth.UserAgent.PlatformType;
import com.github.auties00.cobalt.model.auth.Version;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

public final class AppMetadata {
    private static volatile Version webVersion;
    private static final Object webVersionLock = new Object();
    private static volatile Version personalIosVersion;
    private static final Object personalIosVersionLock = new Object();
    private static volatile Version businessIosVersion;
    private static final Object businessIosVersionLock = new Object();
    private static volatile AndroidData personalAndroidData;
    private static final Object personalAndroidDataLock = new Object();
    private static volatile AndroidData businessAndroidData;
    private static final Object businessAndroidDataLock = new Object();

    private static final String MOBILE_WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    private static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com");
    private static final char[] WEB_UPDATE_PATTERN = "\"client_revision\":".toCharArray();
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";
    private static final URI MOBILE_PERSONAL_ANDROID_URL = URI.create("https://www.whatsapp.com/android/current/WhatsApp.apk");
    private static final URI MOBILE_BUSINESS_ANDROID_URL = URI.create("https://d.cdnpure.com/b/APK/com.whatsapp.w4b?version=latest");
    private static final URI MOBILE_PERSONAL_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsApp");
    private static final URI MOBILE_BUSINESS_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsAppSMB");
    private static final String MOBILE_IOS_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1";
   private static final String MOBILE_ANDROID_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

   private AppMetadata() {
       throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

    public static Version getVersion(PlatformType platform) {
        return switch (platform) {
            case WINDOWS, MACOS ->
                    getWebVersion();
            case ANDROID ->
                    getPersonalAndroidData().version();
            case ANDROID_BUSINESS ->
                    getBusinessAndroidData().version();
            case IOS ->
                    getPersonalIosVersion();
            case IOS_BUSINESS ->
                    getBusinessIosVersion();
        };
    }

    private static Version getWebVersion() {
        if (webVersion == null) {
            synchronized (webVersionLock) {
                if(webVersion == null) {
                    webVersion = queryWebVersion();
                }
            }
        }
        return webVersion;
    }

    // Optimized for speed, it's pretty much looking for the "client_revision":(\\w+) regex
    // But this doesn't need to parse the response as a value
    private static Version queryWebVersion() {
        try {
            try(var httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build()) {
                var request = HttpRequest.newBuilder()
                        .uri(WEB_UPDATE_URL)
                        .GET()
                        .header("User-Agent", MOBILE_WEB_USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if(response.statusCode() != 200) {
                    throw new IllegalStateException("Cannot query web version: status code " + response.statusCode());
                }
                try (var inputStream = response.body()) {
                    var patternIndex = 0;
                    int value;
                    while ((value = inputStream.read()) != -1) {
                        if (value == WEB_UPDATE_PATTERN[patternIndex]) {
                            if (++patternIndex == WEB_UPDATE_PATTERN.length) {
                                var clientVersion = 0;
                                while ((value = inputStream.read()) != -1 && Character.isDigit(value)) {
                                    clientVersion *= 10;
                                    clientVersion += value - '0';
                                }
                                return new Version(2, 3000, clientVersion);
                            }
                        } else {
                            patternIndex = 0;
                            if (value == WEB_UPDATE_PATTERN[0]) {
                                patternIndex = 1;
                            }
                        }
                    }
                    throw new IllegalStateException("Cannot find client_revision in web update response");
                }
            }
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot query web version", exception);
        }
    }

    private static AndroidData getPersonalAndroidData() {
        if (personalAndroidData == null) {
            synchronized (personalAndroidDataLock) {
                if(personalAndroidData == null) {
                    personalAndroidData = downloadAndroidData(false);
                }
            }
        }
        return personalAndroidData;
    }

    private static AndroidData getBusinessAndroidData() {
        if (businessAndroidData == null) {
            synchronized (businessAndroidDataLock) {
                if(businessAndroidData == null) {
                    businessAndroidData = downloadAndroidData(true);
                }
            }
        }
        return businessAndroidData;
    }

    private static AndroidData downloadAndroidData(boolean business) {
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(business ? MOBILE_BUSINESS_ANDROID_URL : MOBILE_PERSONAL_ANDROID_URL)
                    .GET()
                    .header("User-Agent", MOBILE_ANDROID_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP request failed with status code: " + response.statusCode());
            }

            try (var apkFile = new ByteArrayApkFile(response.body())) {
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var digest = MessageDigest.getInstance("MD5");
                digest.update(apkFile.getFileData("classes.dex"));
                var md5Hash = digest.digest();
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificates = getCertificates(apkFile);
                return new AndroidData(version, md5Hash, secretKey, certificates, business);
            }
        } catch (IOException | GeneralSecurityException | InterruptedException exception) {
            throw new RuntimeException("Cannot extract data from APK", exception);
        }
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
        var packageBytes = packageName.getBytes(StandardCharsets.UTF_8);
        var password = new byte[packageBytes.length + resource.length];
        System.arraycopy(packageBytes, 0, password, 0, packageBytes.length);
        System.arraycopy(resource, 0, password, packageBytes.length, resource.length);

        var mac = Mac.getInstance("HmacSHA1");
        var keySpec = new SecretKeySpec(password, mac.getAlgorithm());
        mac.init(keySpec);

        var keySize = 64;
        var macLen = mac.getMacLength();
        var iterations = 128;
        var blocks = (keySize + macLen - 1) / macLen;

        var out = new byte[keySize];
        var state = new byte[macLen];
        var iBuf = new byte[4];

        var offset = 0;
        for (var block = 1; block <= blocks; ++block) {
            mac.update(MOBILE_ANDROID_SALT);

            iBuf[0] = (byte) (block >>> 24);
            iBuf[1] = (byte) (block >>> 16);
            iBuf[2] = (byte) (block >>> 8);
            iBuf[3] = (byte) (block);
            mac.update(iBuf, 0, iBuf.length);

            mac.doFinal(state, 0);

            var toCopy = Math.min(macLen, keySize - offset);
            System.arraycopy(state, 0, out, offset, toCopy);

            for (var cnt = 1; cnt < iterations; ++cnt) {
                mac.update(state, 0, macLen);

                mac.doFinal(state, 0);

                for (var j = 0; j < toCopy; ++j) {
                    out[offset + j] ^= state[j];
                }
            }

            offset += toCopy;
        }

        return out;
    }

    private static Version getPersonalIosVersion() {
        if (personalIosVersion == null) {
            synchronized (personalIosVersionLock) {
                if(personalIosVersion == null) {
                    personalIosVersion = queryIosVersion(false);
                }
            }
        }
        return personalIosVersion;
    }

    private static Version getBusinessIosVersion() {
        if (businessIosVersion == null) {
            synchronized (businessIosVersionLock) {
                if(businessIosVersion == null) {
                    businessIosVersion = queryIosVersion(true);
                }
            }
        }
        return businessIosVersion;
    }

    private static Version queryIosVersion(boolean business) {
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(business ? MOBILE_BUSINESS_IOS_URL : MOBILE_PERSONAL_IOS_URL)
                    .header("User-Agent", MOBILE_IOS_USER_AGENT)
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP request failed with status code: " + response.statusCode());
            }

            var jsonObject = JSON.parseObject(response.body());
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

            return Version.of(version);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Cannot query iOS version", e);
        }
    }

    public static String getToken(long phoneNumber, PlatformType platform, Version appVersion) {
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS ->
                    getAndroidToken(String.valueOf(phoneNumber), platform.isBusiness());
            case IOS, IOS_BUSINESS ->
                    getIosToken(phoneNumber, appVersion, platform.isBusiness());
            default -> throw new IllegalStateException("Cannot compute token for platform " + platform);
        };
    }

    private static String getIosToken(long phoneNumber, Version version, boolean business) {
        try {
            var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
            var token = staticToken + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
            var digest = MessageDigest.getInstance("MD5");
            digest.update(token.getBytes());
            var result = digest.digest();
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing md5 implementation", exception);
        }
    }

    private static String getAndroidToken(String phoneNumber, boolean business) {
        try {
            var whatsappData = business ? getBusinessAndroidData() : getPersonalAndroidData();
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

    private record AndroidData(
            Version version,
            byte[] md5Hash,
            byte[] secretKey,
            List<byte[]> certificates,
            boolean business
    ) {

    }
}