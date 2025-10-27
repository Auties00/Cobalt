package com.github.auties00.cobalt.client.version;

import com.github.auties00.cobalt.model.proto.auth.Version;
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
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Collection;
import java.util.NoSuchElementException;

final class WhatsAppAndroidClientVersion implements WhatsAppMobileClientVersion {
    private static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");
    private static final URI MOBILE_PERSONAL_ANDROID_URL = URI.create("https://www.whatsapp.com/android/current/WhatsApp.apk");
    private static final URI MOBILE_BUSINESS_ANDROID_URL = URI.create("https://d.cdnpure.com/b/APK/com.whatsapp.w4b?version=latest");
    private static final String MOBILE_ANDROID_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

    private static volatile WhatsAppAndroidClientVersion personalApkInfo;
    private static final Object personalApkInfoLock = new Object();
    private static volatile WhatsAppAndroidClientVersion businessApkInfo;
    private static final Object businessApkInfoLock = new Object();

    private final Version version;
    private final byte[] md5Hash;
    private final SecretKeySpec secretKey;
    private final byte[][] certificates;
    private final boolean business;

    private WhatsAppAndroidClientVersion(Version version, byte[] md5Hash, SecretKeySpec secretKey, byte[][] certificates, boolean business) {
        this.version = version;
        this.md5Hash = md5Hash;
        this.secretKey = secretKey;
        this.certificates = certificates;
        this.business = business;
    }

    public static WhatsAppAndroidClientVersion ofPersonal() {
        if (personalApkInfo == null) {
            synchronized (personalApkInfoLock) {
                if(personalApkInfo == null) {
                    personalApkInfo = queryApkInfo(false);
                }
            }
        }
        return personalApkInfo;
    }

    public static WhatsAppAndroidClientVersion ofBusiness() {
        if (businessApkInfo == null) {
            synchronized (businessApkInfoLock) {
                if(businessApkInfo == null) {
                    businessApkInfo = queryApkInfo(true);
                }
            }
        }
        return businessApkInfo;
    }

    private static WhatsAppAndroidClientVersion queryApkInfo(boolean business) {
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
                return new WhatsAppAndroidClientVersion(version, md5Hash, secretKey, certificates, business);
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

    private static byte[][] getCertificates(ByteArrayApkFile apkFile) throws IOException, CertificateException {
        return apkFile.getApkSingers()
                .stream()
                .map(ApkSigner::getCertificateMetas)
                .flatMap(Collection::stream)
                .map(CertificateMeta::getData)
                .toArray(byte[][]::new);
    }

    private static SecretKeySpec getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
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

        return new SecretKeySpec(out, 0, out.length, "PBKDF2");
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public boolean business() {
        return business;
    }

    @Override
    public String computeRegistrationToken(long nationalPhoneNumber) {
        try {
            var mac = Mac.getInstance("HMACSHA1");
            mac.init(secretKey);
            for (var certificate : certificates) {
                mac.update(certificate);
            }
            mac.update(md5Hash);
            mac.update(String.valueOf(nationalPhoneNumber).getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(mac.doFinal()), StandardCharsets.UTF_8);
        }catch (GeneralSecurityException exception) {
            throw new InternalError("Cannot compute registration token", exception);
        }
    }
}
