package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.response.WebVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.CertificateMeta;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

@UtilityClass
public class MetadataHelper {
    // TODO: This is temporary
    private final String IOS_TOKEN = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM4174c0243f5277a5d7720ce842cc4ae6";

    private volatile Version webVersion;
    private volatile WhatsappApk cachedApk;

    public CompletableFuture<Version> getWebVersion() {
        try{
            if (webVersion != null) {
                return CompletableFuture.completedFuture(webVersion);
            }

            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(Whatsapp.WEB_UPDATE_URL))
                    .build();
            return client.sendAsync(request, ofString())
                    .thenApplyAsync(response -> Json.readValue(response.body(), WebVersionResponse.class))
                    .thenApplyAsync(version -> webVersion = new Version(version.currentVersion()));
        } catch(Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    public CompletableFuture<Version> getMobileVersion(UserAgentPlatform platform) {
        return CompletableFuture.supplyAsync(() -> switch (platform) {
            case ANDROID -> getWhatsappData().version();
            case IOS -> new Version("2.22.24.81"); // TODO: Add support for dynamic version fetching
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        });
    }

    public CompletableFuture<String> getToken(long phoneNumber, UserAgentPlatform platform) {
        return CompletableFuture.supplyAsync(() -> switch (platform) {
            case ANDROID -> getAndroidToken(String.valueOf(phoneNumber));
            case IOS -> getIosToken(String.valueOf(phoneNumber));
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        });
    }

    private String getIosToken(String phoneNumber) {
        var token = IOS_TOKEN + phoneNumber;
        return HexFormat.of().formatHex(MD5.calculate(token.getBytes(StandardCharsets.UTF_8)));
    }

    private String getAndroidToken(String phoneNumber) {
        try {
            var whatsappData = getWhatsappData();
            var mac = Mac.getInstance("HMACSHA1");
            mac.init(whatsappData.secretKey());
            whatsappData.certificates().forEach(mac::update);
            mac.update(whatsappData.md5Hash());
            mac.update(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(mac.doFinal());
        } catch (GeneralSecurityException throwable) {
            throw new RuntimeException("Cannot compute mobile token", throwable);
        }
    }

    private synchronized WhatsappApk getWhatsappData() {
        try {
            if(cachedApk != null){
                return cachedApk;
            }

            var apk = Medias.download(Whatsapp.MOBILE_DOWNLOAD_URL)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot read apk at %s".formatted(Whatsapp.MOBILE_DOWNLOAD_URL)));
            try (var apkFile = new ByteArrayApkFile(apk)) {
                var certificates = apkFile.getApkSingers()
                        .stream()
                        .flatMap(signer -> signer.getCertificateMetas().stream())
                        .map(CertificateMeta::getData)
                        .toList();
                var md5Hash = MD5.calculate(apkFile.getFileData("classes.dex"));
                var secretKey = getSecretKey(apkFile.getFileData("res/drawable-hdpi-v4/about_logo.png"));
                var version = new Version(apkFile.getApkMeta().getVersionName());
                return cachedApk = new WhatsappApk(version, md5Hash, secretKey, certificates);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new RuntimeException("Cannot extract certificates from APK", exception);
        }
    }

    private SecretKey getSecretKey(byte[] resource) throws IOException, GeneralSecurityException {
        try (var out = new ByteArrayOutputStream()) {
            out.write("com.whatsapp".getBytes(StandardCharsets.UTF_8));
            out.write(resource);
            var result = out.toByteArray();
            var whatsappLogoChars = new char[result.length];
            for (var i = 0; i < result.length; i++) {
                whatsappLogoChars[i] = (char) result[i];
            }
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT");
            var key = new PBEKeySpec(whatsappLogoChars, Base64.getDecoder().decode(Whatsapp.MOBILE_SALT), 128, 512);
            return factory.generateSecret(key);
        }
    }

    private record WhatsappApk(Version version, byte[] md5Hash, SecretKey secretKey, Collection<byte[]> certificates) {

    }
}