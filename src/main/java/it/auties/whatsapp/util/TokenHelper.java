package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
public class TokenHelper {
    public String getToken(String phoneNumber, UserAgentPlatform platform){
       return switch (platform){
           case ANDROID -> getAndroidToken(phoneNumber);
           case IOS -> throw new UnsupportedOperationException("Token generation for IOS is not supported"); // TODO: add support for ios token generation
           default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
       };
    }

    private String getAndroidToken(String phoneNumber){
        try {
            var whatsappData = getWhatsappData();
            var mac = Mac.getInstance("HMACSHA1");
            mac.init(whatsappData.secretKey());
            for(var certificate : whatsappData.certificates()){
                mac.update(certificate.getEncoded());
            }
            mac.update(whatsappData.md5Hash());
            mac.update(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(mac.doFinal());
        }catch (GeneralSecurityException throwable){
            throw new RuntimeException("Cannot compute mobile token", throwable);
        }
    }

    private WhatsappApk getWhatsappData() {
        try {
            var apk = Medias.download(Whatsapp.MOBILE_DOWNLOAD_URL)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot read apk at %s".formatted(Whatsapp.MOBILE_DOWNLOAD_URL)));
            var certFactory = CertificateFactory.getInstance("X.509");
            var certificates = new ArrayList<Certificate>();
            byte[] md5Hash = null;
            SecretKey secretKey = null;
            try (var zipStream = new ZipInputStream(new ByteArrayInputStream(apk))) {
                ZipEntry zipEntry;
                while ((zipEntry = zipStream.getNextEntry()) != null) {
                    if (zipEntry.getName().endsWith(".RSA") || zipEntry.getName().endsWith(".DSA")) {
                        certificates.addAll(certFactory.generateCertificates(zipStream));
                    } else if (zipEntry.getName().equals("classes.dex")) {
                        md5Hash = MD5.calculate(zipStream.readAllBytes());
                    } else if (zipEntry.getName().contains("about_logo.png") && secretKey == null) {
                        secretKey = getSecretKey(zipStream);
                    }
                }
            }

            return new WhatsappApk(md5Hash, secretKey, certificates);
        }catch (IOException | GeneralSecurityException exception){
            throw new RuntimeException("Cannot extract certificates from APK", exception);
        }
    }

    private SecretKey getSecretKey(ZipInputStream zipStream) throws IOException, GeneralSecurityException {
        try(var out = new ByteArrayOutputStream()) {
            out.write("com.whatsapp".getBytes(StandardCharsets.UTF_8));
            out.write(zipStream.readAllBytes());
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

    private record WhatsappApk(byte[] md5Hash, SecretKey secretKey, Collection<Certificate> certificates) {

    }
}