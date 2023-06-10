package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.response.WebVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import lombok.extern.jackson.Jacksonized;
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
import java.lang.System.Logger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

@UtilityClass
public class MetadataHelper {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final Path ANDROID_CACHE = Path.of(System.getProperty("user.home") + "/.whatsapp4j/token/android");
    private static final Logger LOGGER =  System.getLogger("Metadata");
    private static final Pattern IOS_VERSION_PATTERN = Pattern.compile("(?<=Minimum Requirements \\(Version )\\d+\\.\\d+\\.\\d+");

    private volatile Version webVersion;
    private volatile Version iosVersion;
    private volatile WhatsappApk cachedApk;
    private volatile WhatsappApk cachedBusinessApk;

    public CompletableFuture<Version> getVersion(UserAgentPlatform platform, boolean business) {
        return switch (platform) {
            case WEB, WINDOWS, MACOS -> getWebVersion();
            case ANDROID -> getAndroidData(business).thenApply(WhatsappApk::version);
            case IOS -> getIosVersion();
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private CompletableFuture<Version> getIosVersion() {
        if(iosVersion != null){
            return CompletableFuture.completedFuture(iosVersion);
        }

        // var client = HttpClient.newHttpClient();
        //        var request = HttpRequest.newBuilder()
        //                .GET()
        //                .uri(URI.create(Whatsapp.IOS_UPDATE_URL))
        //                .build();
        //        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        //                .thenApplyAsync(MetadataHelper::parseIosVersion);
        return CompletableFuture.completedFuture(Whatsapp.DEFAULT_MOBILE_IOS_VERSION);
    }

    private Version parseIosVersion(HttpResponse<String> result) {
        return iosVersion = IOS_VERSION_PATTERN.matcher(result.body())
                .results()
                .map(MatchResult::group)
                .reduce((first, second) -> second)
                .map(version -> new Version("2." + version))
                .orElseGet(MetadataHelper::getDefaultIosVersion);
    }

    private Version getDefaultIosVersion() {
        LOGGER.log(Logger.Level.WARNING, "Cannot fetch latest IOS version, falling back to %s".formatted(Whatsapp.DEFAULT_MOBILE_IOS_VERSION));
        return Whatsapp.DEFAULT_MOBILE_IOS_VERSION;
    }

    private CompletableFuture<Version> getWebVersion() {
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

    public CompletableFuture<String> getToken(long phoneNumber, UserAgentPlatform platform, boolean business) {
        return switch (platform) {
            case ANDROID -> getAndroidToken(String.valueOf(phoneNumber), business);
            case IOS -> getIosToken(phoneNumber, platform, business);
            default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
        };
    }

    private CompletableFuture<String> getIosToken(long phoneNumber, UserAgentPlatform platform, boolean business) {
        return getVersion(platform, business)
                .thenApply(version -> getIosToken(phoneNumber, version));
    }

    private String getIosToken(long phoneNumber, Version version) {
        var token = Whatsapp.MOBILE_IOS_STATIC + HexFormat.of().formatHex(version.toHash()) + phoneNumber;
        return HexFormat.of().formatHex(MD5.calculate(token));
    }

    private CompletableFuture<String> getAndroidToken(String phoneNumber, boolean business) {
        return getAndroidData(business)
                .thenApplyAsync(whatsappData -> getAndroidToken(phoneNumber, whatsappData));
    }

    private String getAndroidToken(String phoneNumber, WhatsappApk whatsappData) {
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

    private synchronized CompletableFuture<WhatsappApk> getAndroidData(boolean business) {
        if(!business && cachedApk != null){
            return CompletableFuture.completedFuture(cachedApk);
        }

        if(business && cachedBusinessApk != null){
            return CompletableFuture.completedFuture(cachedBusinessApk);
        }

        return getCachedApk(business)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadWhatsappApk(business));
    }

    private CompletableFuture<WhatsappApk> downloadWhatsappApk(boolean business) {
        return Medias.downloadAsync(business ? Whatsapp.MOBILE_BUSINESS_DOWNLOAD_URL : Whatsapp.MOBILE_DOWNLOAD_URL)
                .thenApplyAsync(result -> getAndroidData(result, business));
    }

    private Optional<WhatsappApk> getCachedApk(boolean business){
        try {
            var file = getAndroidCache(business);
            if(Files.notExists(file)){
                return Optional.empty();
            }

            var now = Instant.now();
            var fileTime = Files.getLastModifiedTime(file).toInstant();
            if(fileTime.until(now, ChronoUnit.WEEKS) > 1){
                return Optional.empty();
            }

            return Optional.of(Json.readValue(Files.readString(file), WhatsappApk.class));
        }catch (Throwable throwable){
            return Optional.empty();
        }
    }

    private static Path getAndroidCache(boolean business) {
        return ANDROID_CACHE.resolve(business ? "whatsapp_business.json" : "whatsapp.json");
    }

    private static WhatsappApk getAndroidData(byte[] apk, boolean business) {
        try (var apkFile = new ByteArrayApkFile(apk)) {
            var version = new Version(apkFile.getApkMeta().getVersionName());
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

    private void cacheWhatsappData(WhatsappApk apk) {
        CompletableFuture.runAsync(() -> {
            try {
                var json = Json.writeValueAsString(apk, true);
                var file = getAndroidCache(apk.business());
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            }catch (Throwable throwable){
                LOGGER.log(Logger.Level.WARNING, "Cannot update local cache", throwable);
            }
        });
    }

    private byte[] getAboutLogo(ByteArrayApkFile apkFile) throws IOException {
        var resource = apkFile.getFileData("res/drawable-hdpi/about_logo.png");
        if(resource != null){
            return resource;
        }

        var resourceV4 = apkFile.getFileData("res/drawable-hdpi-v4/about_logo.png");
        if(resourceV4 != null){
            return resourceV4;
        }

        var xxResourceV4 = apkFile.getFileData("res/drawable-xxhdpi-v4/about_logo.png");
        if(xxResourceV4 != null){
            return xxResourceV4;
        }

        throw new NoSuchElementException("Missing about_logo.png from apk");
    }

    private List<byte[]> getCertificates(ByteArrayApkFile apkFile) throws IOException, CertificateException {
        return apkFile.getApkSingers()
                .stream()
                .map(ApkSigner::getCertificateMetas)
                .flatMap(Collection::stream)
                .map(CertificateMeta::getData)
                .toList();
    }

    private SecretKey getSecretKey(String packageName, byte[] resource) throws IOException, GeneralSecurityException {
        var result = BytesHelper.concat(packageName.getBytes(StandardCharsets.UTF_8), resource);
        var whatsappLogoChars = new char[result.length];
        for (var i = 0; i < result.length; i++) {
            whatsappLogoChars[i] = (char) result[i];
        }
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT");
        var key = new PBEKeySpec(whatsappLogoChars, Whatsapp.MOBILE_ANDROID_SALT, 128, 512);
        return factory.generateSecret(key);
    }

    @Builder
    @Jacksonized
    public record WhatsappApk(Version version, byte[] md5Hash, byte[] secretKey, Collection<byte[]> certificates, boolean business) {

    }
}