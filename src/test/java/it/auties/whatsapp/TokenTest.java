package it.auties.whatsapp;

import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.MetadataHelper;
import it.auties.whatsapp.util.Spec;
import net.dongliu.apk.parser.ByteArrayApkFile;
import net.dongliu.apk.parser.bean.ApkSigner;
import net.dongliu.apk.parser.bean.CertificateMeta;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipFile;

public class TokenTest {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final byte[] A00 = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");

    @Test
    public void main() {
        int i;
        byte[] bytes;
        ByteArrayOutputStream A0Q = new ByteArrayOutputStream();
        String packageName = "com.whatsapp";
        try {
            Charset str2 = StandardCharsets.UTF_8;
            var url = Spec.Whatsapp.MOBILE_DOWNLOAD_URL;
            var apk = Medias.download(url)
                    .orElseThrow(() -> new IllegalArgumentException("Cannot read apk at %s".formatted(url)));
            var apkFile = new ByteArrayApkFile(apk);
            A0Q.write(packageName.getBytes(str2));
            var res = apkFile.getFileData("res/drawable-hdpi/about_logo.png");
            if (res == null) {
                res = apkFile.getFileData("res/drawable-hdpi-v4/about_logo.png");
            }
            if (res == null) {
                res = apkFile.getFileData("res/drawable-xxhdpi-v4/about_logo.png");
            }
            var resourceAsStream = new ByteArrayInputStream(res);
            byte[] bArr = new byte[8192];
            try {
                try {
                    int read = resourceAsStream.read(bArr);
                    while (true) {
                        if (read != -1) {
                            A0Q.write(bArr, 0, read);
                            read = resourceAsStream.read(bArr);
                        } else {
                            break;
                        }
                    }
                    var A09 = A09(A0Q.toByteArray(), A00, 128, 512);
                    A09.getEncoded();
                    try {
                        Mac mac = Mac.getInstance("HMACSHA1");
                        mac.init(A09);
                        List<byte[]> A03 = apkFile.getApkSingers()
                                .stream()
                                .map(ApkSigner::getCertificateMetas)
                                .flatMap(Collection::stream)
                                .map(CertificateMeta::getData)
                                .toList();
                        for (var signature : A03) {
                            mac.update(signature);
                        }
                        try {
                            var file = Files.createTempFile("wa", ".apk");
                            Files.write(file, apk);
                            var zipFile = new ZipFile(file.toFile());
                            var inputStream = zipFile.getInputStream(zipFile.getEntry("classes.dex"));
                            try {
                                var messageDigest = MessageDigest.getInstance("MD5");
                                byte[] bArr2 = new byte[8192];
                                while (true) {
                                    int read2 = inputStream.read(bArr2);
                                    if (read2 <= 0) {
                                        break;
                                    }
                                    messageDigest.update(bArr2, 0, read2);
                                }
                                bytes = messageDigest.digest();
                                inputStream.close();
                                zipFile.close();
                            } catch (Throwable th) {
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (Throwable th2) {
                                        th.addSuppressed(th2);
                                    }
                                }
                                throw th;
                            }
                        } catch (Exception e) {
                            bytes = "null".getBytes(str2);
                        }
                        mac.update(bytes);
                        mac.update("3495089819".getBytes(str2));
                        System.out.println(Base64.getUrlEncoder().encodeToString(mac.doFinal()));
                        System.out.println(MetadataHelper.getToken(3495089819L, UserAgent.UserAgentPlatform.ANDROID, false).join());
                    } catch (Throwable throwable){
                        throwable.printStackTrace();
                    }
                } finally {
                    try {
                        resourceAsStream.close();
                    } catch (IOException unused2) {
                    }
                }
            } catch (IOException unused3) {
                throw new AssertionError();
            }
        } catch (IOException e4) {
            throw new Error(e4);
        }
    }

    public static SecretKey A09(byte[] bArr, byte[] bArr2, int i, int i2) {
        int length = bArr.length;
        char[] cArr = new char[length];
        for (int i3 = 0; i3 < length; i3++) {
            cArr[i3] = (char) bArr[i3];
        }
        return A08("PBKDF2WithHmacSHA1And8BIT", bArr2, cArr, i, i2);
    }

    public static SecretKey A08(String str, byte[] bArr, char[] cArr, int i, int i2) {
        try {
            return SecretKeyFactory.getInstance(str).generateSecret(new PBEKeySpec(cArr, bArr, i, i2));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
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
