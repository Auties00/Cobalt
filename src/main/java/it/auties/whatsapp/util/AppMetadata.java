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
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.*;

public final class AppMetadata {
    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

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
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
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
    // But this doesn't need to parse the response as a string
    private static Version queryWebVersion() {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) WEB_UPDATE_URL.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", MOBILE_WEB_USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Sec-Fetch-Dest", "document");
            connection.setRequestProperty("Sec-Fetch-Mode", "navigate");
            connection.setRequestProperty("Sec-Fetch-Site", "none");
            connection.setRequestProperty("Sec-Fetch-User", "?1");
            try (var inputStream = connection.getInputStream()) {
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
        } catch (IOException exception) {
            throw new RuntimeException("Cannot query web version", exception);
        }finally {
            if(connection != null) {
                connection.disconnect();
            }
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
        HttpsURLConnection connection = null;
        try {
            var uri = business ? MOBILE_BUSINESS_ANDROID_URL : MOBILE_PERSONAL_ANDROID_URL;
            connection = (HttpsURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", MOBILE_ANDROID_USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Sec-Fetch-Dest", "document");
            connection.setRequestProperty("Sec-Fetch-Mode", "navigate");
            connection.setRequestProperty("Sec-Fetch-Site", "none");
            connection.setRequestProperty("Sec-Fetch-User", "?1");
            // TODO: Can this become an input stream?
            //       Something like ApkInputStream
            try (var apkFile = new ByteArrayApkFile(connection.getInputStream().readAllBytes())) {
                var version = Version.of(apkFile.getApkMeta().getVersionName());
                var digest = MessageDigest.getInstance("MD5");
                digest.update(apkFile.getFileData("classes.dex"));
                var md5Hash = digest.digest();
                var secretKey = getSecretKey(apkFile.getApkMeta().getPackageName(), getAboutLogo(apkFile));
                var certificates = getCertificates(apkFile);
                return new AndroidData(version, md5Hash, secretKey, certificates, business);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new RuntimeException("Cannot extract data from APK", exception);
        }finally {
            if(connection != null) {
                connection.disconnect();
            }
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

    // TODO: Can this be optimized / do we even care about optimizing this?
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
        HttpsURLConnection connection = null;
        try {
            var uri = business ? MOBILE_BUSINESS_IOS_URL : MOBILE_PERSONAL_IOS_URL;
            connection = (HttpsURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", MOBILE_IOS_USER_AGENT);
            try (var inputStream = connection.getInputStream()) {
                var jsonObject = JSON.parseObject(inputStream);
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
            }
        } catch (IOException exception) {
            throw new RuntimeException("Cannot query ios version", exception);
        }finally {
            if(connection != null) {
                connection.disconnect();
            }
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

    private record AndroidData(
            Version version,
            byte[] md5Hash,
            byte[] secretKey,
            List<byte[]> certificates,
            boolean business
    ) {

    }
}