package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

@UtilityClass
public class RegistrationHelper {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CompletableFuture<Void> registerPhoneNumber(Store store, Keys keys, Supplier<CompletableFuture<String>> handler, VerificationCodeMethod method) {
        if (method == VerificationCodeMethod.NONE) {
            return sendVerificationCode(store, keys, handler);
        }

        return requestVerificationCode(store, keys, method)
                .thenComposeAsync(ignored -> sendVerificationCode(store, keys, handler));
    }

    public CompletableFuture<Void> requestVerificationCode(Store store, Keys keys, VerificationCodeMethod method) {
        if(method == VerificationCodeMethod.NONE){
            return CompletableFuture.completedFuture(null);
        }

        var codeOptions = getRegistrationOptions(
                store,
                keys,
                Map.entry("mcc", store.phoneNumber().countryCode().mcc()),
                Map.entry("mnc", store.phoneNumber().countryCode().mnc()),
                Map.entry("sim_mcc", "000"),
                Map.entry("sim_mnc", "000"),
                Map.entry("method", method.type()),
                Map.entry("reason", ""),
                Map.entry("hasav", "1")
        );
        return sendRegistrationRequest(store,"/code", codeOptions)
                .thenAcceptAsync(RegistrationHelper::checkResponse)
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, false));
    }

    public CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, Supplier<CompletableFuture<String>> handler) {
        return handler.get()
                .thenComposeAsync(result -> sendVerificationCode(store, keys, result))
                .thenRunAsync(() -> saveRegistrationStatus(store, keys, true));
    }

    private void saveRegistrationStatus(Store store, Keys keys, boolean registered) {
        keys.registered(registered);
        if(registered){
            store.jid(store.phoneNumber().toJid());
            store.addLinkedDevice(store.jid(), 0);
        }
        keys.serialize(true);
        store.serialize(true);
    }

    private CompletableFuture<Void> sendVerificationCode(Store store, Keys keys, String code) {
        var registerOptions = getRegistrationOptions(store, keys, Map.entry("code", code.replaceAll("-", "")));
        return sendRegistrationRequest(store, "/register", registerOptions)
                .thenAcceptAsync(RegistrationHelper::checkResponse);
    }

    private void checkResponse(HttpResponse<String> result) {
        var response = Json.readValue(result.body(), VerificationCodeResponse.class);
        Validate.isTrue(response.status().isSuccessful(), "Invalid response, status code %s: %s".formatted(result.statusCode(), result.body()));
    }

    private CompletableFuture<HttpResponse<String>> sendRegistrationRequest(Store store, String path, Map<String, Object> params) {
        System.out.println(getUserAgent(store));
        var client = createClient(store);
        var request = HttpRequest.newBuilder()
                .uri(URI.create("%s%s?%s".formatted(Whatsapp.MOBILE_REGISTRATION_ENDPOINT, path, toFormParams(params))))
                .GET()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", getUserAgent(store))
                .build();
        return client.sendAsync(request, BodyHandlers.ofString());
    }

    private String getUserAgent(Store store) {
        return "WhatsApp/%s %s/%s Device/%s-%s".formatted(
                store.version(),
                getMobileOsName(),
                Spec.Whatsapp.MOBILE_OS_VERSION,
                Spec.Whatsapp.MOBILE_DEVICE_MANUFACTURER,
                Spec.Whatsapp.MOBILE_DEVICE_MODEL
        );
    }

    private Object getMobileOsName() {
        return switch (Whatsapp.MOBILE_OS_TYPE) {
            case ANDROID -> "Android";
            case IOS -> "iOS";
            default -> throw new IllegalStateException("Unsupported mobile os: " + Whatsapp.MOBILE_OS_TYPE);
        };
    }

    private HttpClient createClient(Store store) {
        var clientBuilder = HttpClient.newBuilder();
        store.proxy().ifPresent(proxy -> {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            clientBuilder.authenticator(new ProxyAuthenticator());
        });
        return clientBuilder.build();
    }

    @SafeVarargs
    private Map<String, Object> getRegistrationOptions(Store store, Keys keys, Entry<String, Object>... attributes) {
        Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
        var token = getToken(store.phoneNumber());
        System.out.println(token);
        return Attributes.of(attributes)
                .put("cc", store.phoneNumber().countryCode().prefix())
                .put("in", store.phoneNumber().number())
                .put("lg", "en")
                .put("lc", "GB")
                .put("mistyped", "6")
                .put("authkey", Base64.getUrlEncoder().encodeToString(keys.noiseKeyPair().publicKey()))
                .put("e_regid", Base64.getUrlEncoder().encodeToString(keys.encodedRegistrationId()))
                .put("e_ident", Base64.getUrlEncoder().encodeToString(keys.identityKeyPair().publicKey()))
                .put("e_skey_id", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().encodedId()))
                .put("e_skey_val", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().publicKey()))
                .put("e_skey_sig", Base64.getUrlEncoder().encodeToString(keys.signedKeyPair().signature()))
                .put("fdid", keys.phoneId())
                .put("expid", keys.deviceId())
                .put("network_radio_type", "1")
                .put("simnum", "1")
                .put("hasinrc", "1")
                .put("pid", ProcessHandle.current().pid())
                .put("rc", Whatsapp.MOBILE_RELEASE_CHANNEL.index())
                .put("id", keys.identityId())
                .put("token", token)
                .toMap();
    }

    private String toFormParams(Map<String, Object> values) {
        return values.entrySet()
                .stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String getToken(PhoneNumber phoneNumber){
        try {
            var mac = Mac.getInstance("HMACSHA1");
            mac.init(getTokenKey());
            var apk = getWhatsappApk();
            for(var cert : getCertificates(apk)){
                mac.update(cert.getEncoded());
            }
            var tempFile = Files.createTempFile("whatsapp", ".apk");
            Files.write(tempFile, apk);
            var zipFile = new ZipFile(tempFile.toFile());
            var zipEntry = zipFile.getEntry("classes.dex");
            var zipStream = zipFile.getInputStream(zipEntry);
            var messageDigest = MessageDigest.getInstance("MD5");
            var messageResult = new byte[8192];
            while (true) {
                int read2 = zipStream.read(messageResult);
                if (read2 <= 0) {
                    break;
                }
                messageDigest.update(messageResult, 0, read2);
            }
            zipFile.close();
            mac.update(messageDigest.digest());
            mac.update(String.valueOf(phoneNumber.number()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(mac.doFinal());
        }catch (IOException | GeneralSecurityException throwable){
            throw new RuntimeException("Cannot compute mobile token", throwable);
        }
    }

    private byte[] getWhatsappApk() {
        try {
            return new URL(Whatsapp.MOBILE_DOWNLOAD_URL)
                    .openStream()
                    .readAllBytes();
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot download whatsapp apk", exception);
        }
    }

    private SecretKey getTokenKey() {
        try(var out = new ByteArrayOutputStream()) {
            out.write("com.whatsapp".getBytes(StandardCharsets.UTF_8));
            try(var resourceAsStream = ClassLoader.getSystemResource("about_logo.png").openStream()) {
                var bArr = new byte[8192];
                var read = resourceAsStream.read(bArr);
                while (true) {
                    if (read != -1) {
                        out.write(bArr, 0, read);
                        read = resourceAsStream.read(bArr);
                    }else {
                        break;
                    }
                }
            }

            var result = out.toByteArray();
            var whatsappLogoChars = new char[result.length];
            for (int i3 = 0; i3 < result.length; i3++) {
                whatsappLogoChars[i3] = (char) result[i3];
            }
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT");
            var key = new PBEKeySpec(whatsappLogoChars, Base64.getDecoder().decode(Whatsapp.MOBILE_SALT), 128, 512);
            return factory.generateSecret(key);
        }catch (IOException | GeneralSecurityException exception){
            throw new RuntimeException("Cannot load key", exception);
        }
    }

    public List<Certificate> getCertificates(byte[] jarBytes) {
        try {
            var certificates = new ArrayList<Certificate>();
            var certFactory = CertificateFactory.getInstance("X.509");
            try (var jarStream = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
                JarEntry jarEntry;
                while ((jarEntry = jarStream.getNextJarEntry()) != null) {
                    if (jarEntry.getName().endsWith(".RSA") || jarEntry.getName().endsWith(".DSA")) {
                        certificates.addAll(certFactory.generateCertificates(jarStream));
                    }
                }
            }

            return certificates;
        }catch (IOException | GeneralSecurityException exception){
            throw new RuntimeException("Cannot extract certificates from APK", exception);
        }
    }
}
