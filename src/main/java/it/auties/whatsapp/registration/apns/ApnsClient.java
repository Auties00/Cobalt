package it.auties.whatsapp.registration.apns;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import it.auties.whatsapp.crypto.Sha1;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.ProxyAuthenticator;
import it.auties.whatsapp.util.Validate;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ApnsClient implements AutoCloseable {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Authenticator.setDefault(new ProxyAuthenticator());
    }

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("<Protocol>([^*]+)</Protocol>", Pattern.MULTILINE);
    private static final byte[] FAIRPLAY_PRIVATE_KEY = Base64.getDecoder().decode("MIICWwIBAAKBgQC3BKrLPIBabhpr+4SvuQHnbF0ssqRIQ67/1bTfArVuUF6p9sdcv70N+r8yFxesDmpTmKitLP06szKNAO1k5JVk9/P1ejz08BMe9eAb4juAhVWdfAIyaJ7sGFjeSL015mAvrxTFcOM10F/qSlARBiccxHjPXtuWVr0fLGrhM+/AMQIDAQABAoGACGW3bHHPNdb9cVzt/p4Pf03SjJ15ujMY0XY9wUm/h1s6rLO8+/10MDMEGMlEdcmHiWRkwOVijRHxzNRxEAMI87AruofhjddbNVLt6ppW2nLCK7cEDQJFahTW9GQFzpVRQXXfxr4cs1X3kutlB6uY2VGltxQFYsj5djv7D+A72A0CQQDZj1RGdxbeOo4XzxfA6n42GpZavTlM3QzGFoBJgCqqVu1JQOzooAMRT+NPfgoE8+usIVVB4Io0bCUTWLpkEytTAkEA11rzIpGIhFkPtNc/33fvBFgwUbsjTs1V5G6z5ly/XnG9ENfLblgEobLmSmz3irvBRWADiwUx5zY6FN/Dmti56wJAdiScakufcnyvzwQZ7Rwp/61+erYJGNFtb2Cmt8NO6AOehcopHMZQBCWy1ecm/7uJ/oZ3avfJdWBI3fGv/kpemwJAGMXyoDBjpu3j26bDRz6xtSs767r+VctTLSL6+O4EaaXl3PEmCrx/U+aTjU45r7Dni8Z+wdhIJFPdnJcdFkwGHwJAPQ+wVqRjc4h3Hwu8I6llk9whpK9O70FLo1FMVdaytElMyqzQ2/05fMb7F6yaWhu+Q2GGXvdlURiA3tY0CsfM0w==");
    private static final byte[] FAIRPLAY_CERT_CHAIN = Base64.getDecoder().decode("MIIC8zCCAlygAwIBAgIKAlKu1qgdFrqsmzANBgkqhkiG9w0BAQUFADBaMQswCQYDVQQGEwJVUzETMBEGA1UEChMKQXBwbGUgSW5jLjEVMBMGA1UECxMMQXBwbGUgaVBob25lMR8wHQYDVQQDExZBcHBsZSBpUGhvbmUgRGV2aWNlIENBMB4XDTIxMTAxMTE4NDczMVoXDTI0MTAxMTE4NDczMVowgYMxLTArBgNVBAMWJDE2MEQzRkExLUM3RDUtNEY4NS04NDQ4LUM1Q0EzQzgxMTE1NTELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkNBMRIwEAYDVQQHEwlDdXBlcnRpbm8xEzARBgNVBAoTCkFwcGxlIEluYy4xDzANBgNVBAsTBmlQaG9uZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAtwSqyzyAWm4aa/uEr7kB52xdLLKkSEOu/9W03wK1blBeqfbHXL+9Dfq/MhcXrA5qU5iorSz9OrMyjQDtZOSVZPfz9Xo89PATHvXgG+I7gIVVnXwCMmie7BhY3ki9NeZgL68UxXDjNdBf6kpQEQYnHMR4z17blla9Hyxq4TPvwDECAwEAAaOBlTCBkjAfBgNVHSMEGDAWgBSy/iEjRIaVannVgSaOcxDYp0yOdDAdBgNVHQ4EFgQURyh+oArXlcLvCzG4m5/QxwUFzzMwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBaAwIAYDVR0lAQH/BBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMBAGCiqGSIb3Y2QGCgIEAgUAMA0GCSqGSIb3DQEBBQUAA4GBAKwB9DGwHsinZu78lk6kx7zvwH5d0/qqV1+4Hz8EG3QMkAOkMruSRkh8QphF+tNhP7y93A2kDHeBSFWk/3Zy/7riB/dwl94W7vCox/0EJDJ+L2SXvtB2VEv8klzQ0swHYRV9+rUCBWSglGYlTNxfAsgBCIsm8O1Qr5SnIhwfutc4MIIDaTCCAlGgAwIBAgIBATANBgkqhkiG9w0BAQUFADB5MQswCQYDVQQGEwJVUzETMBEGA1UEChMKQXBwbGUgSW5jLjEmMCQGA1UECxMdQXBwbGUgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkxLTArBgNVBAMTJEFwcGxlIGlQaG9uZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw0wNzA0MTYyMjU0NDZaFw0xNDA0MTYyMjU0NDZaMFoxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBcHBsZSBJbmMuMRUwEwYDVQQLEwxBcHBsZSBpUGhvbmUxHzAdBgNVBAMTFkFwcGxlIGlQaG9uZSBEZXZpY2UgQ0EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAPGUSsnquloYYK3Lok1NTlQZaRdZB2bLl+hmmkdfRq5nerVKc1SxywT2vTa4DFU4ioSDMVJl+TPhl3ecK0wmsCU/6TKqewh0lOzBSzgdZ04IUpRai1mjXNeT9KD+VYW7TEaXXm6yd0UvZ1y8Cxi/WblshvcqdXbSGXH0KWO5JQuvAgMBAAGjgZ4wgZswDgYDVR0PAQH/BAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFLL+ISNEhpVqedWBJo5zENinTI50MB8GA1UdIwQYMBaAFOc0Ki4i3jlga7SUzneDYS8xoHw1MDgGA1UdHwQxMC8wLaAroCmGJ2h0dHA6Ly93d3cuYXBwbGUuY29tL2FwcGxlY2EvaXBob25lLmNybDANBgkqhkiG9w0BAQUFAAOCAQEAd13PZ3pMViukVHe9WUg8Hum+0I/0kHKvjhwVd/IMwGlXyU7DhUYWdja2X/zqj7W24Aq57dEKm3fqqxK5XCFVGY5HI0cRsdENyTP7lxSiiTRYj2mlPedheCn+k6T5y0U4Xr40FXwWb2nWqCF1AgIudhgvVbxlvqcxUm8Zz7yDeJ0JFovXQhyO5fLUHRLCQFssAbf8B4i8rYYsBUhYTspVJcxVpIIltkYpdIRSIARA49HNvKK4hzjzMS/OhKQpVKw+OCEZxptCVeN2pjbdt9uzi175oVo/u6B2ArKAW17u6XEHIdDMOe7cb33peVI6TD15W4MIpyQPbp8orlXe+tA8JDCCA/MwggLboAMCAQICARcwDQYJKoZIhvcNAQEFBQAwYjELMAkGA1UEBhMCVVMxEzARBgNVBAoTCkFwcGxlIEluYy4xJjAkBgNVBAsTHUFwcGxlIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MRYwFAYDVQQDEw1BcHBsZSBSb290IENBMB4XDTA3MDQxMjE3NDMyOFoXDTIyMDQxMjE3NDMyOFoweTELMAkGA1UEBhMCVVMxEzARBgNVBAoTCkFwcGxlIEluYy4xJjAkBgNVBAsTHUFwcGxlIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MS0wKwYDVQQDEyRBcHBsZSBpUGhvbmUgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCjHr7wR8C0nhBbRqS4IbhPhiFwKEVgXBzDyApkY4j7/Gnu+FT86Vu3Bk4EL8NrM69ETOpLgAm0h/ZbtP1k3bNy4BOz/RfZvOeo7cKMYcIq+ezOpV7WaetkC40Ij7igUEYJ3Bnk5bCUbbv3mZjE6JtBTtTxZeMbUnrc6APZbh3aEFWGpClYSQzqR9cVNDP2wKBESnC+LLUqMDeMLhXr0eRslzhVVrE1K1jqRKMmhe7IZkrkz4nwPWOtKd6tulqz3KWjmqcJToAWNWWkhQ1jez5jitp9SkbsozkYNLnGKGUYvBNgnH9XrBTJie2htodoUraETrjIg+z5nhmrs8ELhsefAgMBAAGjgZwwgZkwDgYDVR0PAQH/BAQDAgGGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFOc0Ki4i3jlga7SUzneDYS8xoHw1MB8GA1UdIwQYMBaAFCvQaUeUdgn+9GuNLkCm90dNfwheMDYGA1UdHwQvMC0wK6ApoCeGJWh0dHA6Ly93d3cuYXBwbGUuY29tL2FwcGxlY2Evcm9vdC5jcmwwDQYJKoZIhvcNAQEFBQADggEBAB3R1XvddE7XF/yCLQyZm15CcvJp3NVrXg0Ma0s+exQl3rOU6KD6D4CJ8hc9AAKikZG+dFfcr5qfoQp9ML4AKswhWev9SaxudRnomnoD0Yb25/awDktJ+qO3QbrX0eNWoX2Dq5eu+FFKJsGFQhMmjQNUZhBeYIQFEjEra1TAoMhBvFQe51StEwDSSse7wYqvgQiO8EYKvyemvtzPOTqAcBkjMqNrZl2eTahHSbJ7RbVRM6d0ZwlOtmxvSPcsuTMFRGtFvnRLb7KGkbQ+JSglnrPCUYb8T+WvO6q7RCwBSeJ0szT6RO8UwhHyLRkaUYnTCEpBbFhW3ps64QVX5WLP0g8wggS7MIIDo6ADAgECAgECMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBcHBsZSBJbmMuMSYwJAYDVQQLEx1BcHBsZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEWMBQGA1UEAxMNQXBwbGUgUm9vdCBDQTAeFw0wNjA0MjUyMTQwMzZaFw0zNTAyMDkyMTQwMzZaMGIxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBcHBsZSBJbmMuMSYwJAYDVQQLEx1BcHBsZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEWMBQGA1UEAxMNQXBwbGUgUm9vdCBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOSRqQkfkdseR1DrBe1eeYQt6zaiV0xV7IsZid75S2z1B6siMALoGD74UAnTf0GomPnRymacJGsR0KO75Bsqwx+VnnoMpEeLW9QWNzPLxA9NzhRp0ckZcvVdDtV/X5vyJQO6VY9NXQ3xZDUjFUsVWR2zlPf2nJ7PULrBWFBnjwi0IPfLrCwgb3C2PwEwjLdDzw+dPfMrSSgayP7OtbkO2V4c1ss9tTqt9A8OAJILsSEWLnTVPA3bYharo3GSR1NVwa8vQbP4++NwzeajTEV+H0xrUJZBicR0YgsQg0GHM4qBsTBY7FoEMoxos48d3mVz/2deZbxJ2HafMxRloXeUyS0CAwEAAaOCAXowggF2MA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBQr0GlHlHYJ/vRrjS5ApvdHTX8IXjAfBgNVHSMEGDAWgBQr0GlHlHYJ/vRrjS5ApvdHTX8IXjCCAREGA1UdIASCAQgwggEEMIIBAAYJKoZIhvdjZAUBMIHyMCoGCCsGAQUFBwIBFh5odHRwczovL3d3dy5hcHBsZS5jb20vYXBwbGVjYS8wgcMGCCsGAQUFBwICMIG2GoGzUmVsaWFuY2Ugb24gdGhpcyBjZXJ0aWZpY2F0ZSBieSBhbnkgcGFydHkgYXNzdW1lcyBhY2NlcHRhbmNlIG9mIHRoZSB0aGVuIGFwcGxpY2FibGUgc3RhbmRhcmQgdGVybXMgYW5kIGNvbmRpdGlvbnMgb2YgdXNlLCBjZXJ0aWZpY2F0ZSBwb2xpY3kgYW5kIGNlcnRpZmljYXRpb24gcHJhY3RpY2Ugc3RhdGVtZW50cy4wDQYJKoZIhvcNAQEFBQADggEBAFw2mUwteLftjJvc83eb8nbSdzBPwR+Fg4UbmT1HN/Kpm0COLNSxkBLYvvRzm+7SZA/LeU802KI++Xj/a8gH7H05g4tTINM4xLG/mk8Ka/8r/FmnBQl8F0BWER5007eLIztHo9VvJOLr0bdw3w9F4SfK8W147ee1Fxeo3H4iNcol1dkP1mvUoiQjEfehrI9zgWDGG1sJL5Ky+ERI8GA4nhX1PSZnIIozavcNgs/e66Mv+VNqW2TAYzN39zoHLFbr2g8hDtq6cxlPtdk2f8GHVdmnmbkyQvvY1XGefqFStxu9k0IkEirHDx22TZxeY8hLgBdQqorV2uT80AkHN7B1dSE=");
    private static final int PORT = 5223;

    private final Set<Listener> listeners;
    private SSLSocket socket;
    private byte[] certificate;
    private PrivateKey privateKey;
    private byte[] authToken;
    private ExecutorService readerService;
    private ScheduledExecutorService keepAliveExecutor;

    public ApnsClient() {
        this.listeners = ConcurrentHashMap.newKeySet();
    }

    public CompletableFuture<Void> login() {
        return generatePushCert()
                .thenComposeAsync(ignored -> getAPNSBag())
                .thenComposeAsync(this::authenticate);
    }

    private CompletableFuture<Void> generatePushCert() {
        var keyPair = generateRSAKeyPair();
        var csr = generateCSR(keyPair);
        var activationInfo = getActivationInfo(csr);
        var activationSignature = getActivationSignature(activationInfo);
        var activationBody = getActivationBody(activationInfo, activationSignature);
        try(var client = HttpClient.newHttpClient()) {
            var encodedInfo = URLEncoder.encode(activationBody, StandardCharsets.UTF_8);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://albert.apple.com/WebObjects/ALUnbrick.woa/wa/deviceActivation"))
                    .POST(HttpRequest.BodyPublishers.ofString("device=Windows&activation-info=" + encodedInfo))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                var protocol = PROTOCOL_PATTERN.matcher(response.body())
                        .results()
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("Missing result"))
                        .group(1)
                        .getBytes(StandardCharsets.UTF_8);
                var deviceInfo = DeviceActivationInfo.ofPlist(protocol);
                this.privateKey = keyPair.getPrivate();
                this.certificate = deviceInfo.activationRecord().deviceCertificate();
            });
        }
    }

    private String getActivationBody(byte[] activationInfo, byte[] activationSignature) {
        var activationInfoBody = new NSDictionary();
        activationInfoBody.put("ActivationInfoComplete", true);
        activationInfoBody.put("ActivationInfoXML", activationInfo);
        activationInfoBody.put("FairPlayCertChain", FAIRPLAY_CERT_CHAIN);
        activationInfoBody.put("FairPlaySignature", activationSignature);
        return activationInfoBody.toXMLPropertyList();
    }

    private byte[] getActivationSignature(byte[] activationInfoXml) {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var fairPlayPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(FAIRPLAY_PRIVATE_KEY));
            var signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(fairPlayPrivateKey);
            signature.update(activationInfoXml);
            return signature.sign();
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot generate activation signature", exception);
        }
    }

    private KeyPair generateRSAKeyPair() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        }catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("Missing algorithm: RSA", exception);
        }
    }

    private byte[] getActivationInfo(byte[] csr) {
        var activationInfo = new NSDictionary();
        activationInfo.put("ActivationRandomness", UUID.randomUUID().toString());
        activationInfo.put("ActivationState", "Unactivated");
        activationInfo.put("BuildVersion", "10.6.4");
        activationInfo.put("DeviceCertRequest", csr);
        activationInfo.put("DeviceClass", "Windows");
        activationInfo.put("ProductType", "windows1,1");
        activationInfo.put("ProductVersion", "10.6.4");
        activationInfo.put("SerialNumber", "WindowSerial");
        activationInfo.put("UniqueDeviceID", UUID.randomUUID().toString());
        return activationInfo.toXMLPropertyList()
                .getBytes(StandardCharsets.UTF_8);
    }

    private CompletableFuture<Void> authenticate(ApnsBag bag) {
        var nonce = createNonce();
        var signature = createNonceSignature(nonce);
        createSocketConnection(bag);
        var certificateBytes = getCertificateBytes();
        this.readerService = Executors.newSingleThreadExecutor();
        readerService.execute(this::receive);
        send(ApnsPayloadTag.CONNECT, Map.of(
                0x2, new byte[]{0x01},
                0x5, new byte[]{0, 0, 0, 65},
                0xc, certificateBytes,
                0xd, nonce,
                0xe, signature
        ));
        return waitForPacket(packet -> packet.tag() == ApnsPayloadTag.READY).thenAccept(packet -> {
            var statusBuffer = ByteBuffer.wrap(packet.fields().get(0x1));
            var statusCode = Byte.toUnsignedInt(statusBuffer.get());
            Validate.isTrue(statusCode == 0, "Connection failed: %s", statusCode);
            this.authToken = packet.fields().get(0x3);
            send(ApnsPayloadTag.STATE, Map.of(
                    1, new byte[]{1},
                    2, new byte[]{127, -1, -1, -1}
            ));
            this.keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
            keepAliveExecutor.scheduleAtFixedRate(() -> send(ApnsPayloadTag.KEEP_ALIVE_SEND, Map.of()), 0, 5, TimeUnit.MINUTES);
        });
    }

    private byte[] getCertificateBytes() {
        try {
            var pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(certificate)));
            var certificateHolder = (X509CertificateHolder) pemParser.readObject();
            return certificateHolder.getEncoded();
        }catch (IOException exception) {
            throw new UncheckedIOException("Cannot get certificate bytes", exception);
        }
    }

    private void createSocketConnection(ApnsBag bag) {
      try {
          var sslContext = SSLContext.getInstance("TLSv1.3");
          sslContext.init(null, new TrustManager[]{
                  new X509TrustManager() {
                      @Override
                      public X509Certificate[] getAcceptedIssuers() {
                          return null;
                      }

                      @Override
                      public void checkClientTrusted(X509Certificate[] certs, String authType) {

                      }

                      @Override
                      public void checkServerTrusted(X509Certificate[] certs, String authType) {

                      }
                  }
          }, null);
          var sslParameters = sslContext.getDefaultSSLParameters();
          sslParameters.setApplicationProtocols(new String[]{"apns-security-v3"});
          var sslSocketFactory = sslContext.getSocketFactory();
          var underlyingSocket = new Socket();
          var endpoint = ThreadLocalRandom.current().nextInt(1, bag.hostCount()) + "-" + bag.hostname();
          underlyingSocket.connect(new InetSocketAddress(endpoint, PORT));
          this.socket = (SSLSocket) sslSocketFactory.createSocket(underlyingSocket, endpoint, PORT, true);
          socket.setSSLParameters(sslParameters);
          socket.startHandshake();
      }catch (IOException exception) {
          throw new UncheckedIOException(exception);
      } catch (GeneralSecurityException exception) {
          throw new RuntimeException(exception);
      }
    }

    private byte[] createNonceSignature(byte[] nonce) {
        try {
            var signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(nonce);
            return BytesHelper.concat(new byte[]{0x01, 0x01}, signature.sign());
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot generate signature for nonce", exception);
        }
    }

    private byte[] createNonce() {
        var nonceBuffer = ByteBuffer.allocate(17);
        nonceBuffer.putLong(1, System.currentTimeMillis());
        nonceBuffer.put(9, BytesHelper.random(8));
        return nonceBuffer.array();
    }

    public CompletableFuture<ApnsPacket> waitForPacket(Function<ApnsPacket, Boolean> listener) {
        var listenerWithFuture = new Listener(listener);
        listeners.add(listenerWithFuture);
        return listenerWithFuture.future();
    }

    public CompletableFuture<String> getAppToken(String topic) {
        Validate.isTrue(authToken != null, "Missing connect() call");
        var topicHash = Sha1.calculate(topic);
        send(ApnsPayloadTag.GET_TOKEN, Map.of(
                1, authToken,
                2, topicHash,
                3, new byte[]{0x00, 0x00}
        ));
        return waitForPacket(packet -> packet.tag() == ApnsPayloadTag.TOKEN_RESPONSE && Arrays.equals(packet.fields().get(0x3), topicHash))
                .thenApply(packet -> HexFormat.of().formatHex(packet.fields().get(0x2)));
    }

    public void setFilter(String... topics) {
        Validate.isTrue(authToken != null, "Missing connect() call");
        var hashes = Arrays.stream(topics)
                .map(Sha1::calculate)
                .toArray(byte[][]::new);
        send(ApnsPayloadTag.FILTER, Map.of(
                1, authToken,
                2, hashes
        ));
    }

    private void send(ApnsPayloadTag payloadType, Map<Integer, ?> fields) {
        var payloadLength = getPayloadLength(fields);
        var byteArrayOutputStream = new ByteArrayOutputStream(payloadLength);
        try (var dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeByte(payloadType.value());
            dataOutputStream.writeInt(payloadLength);
            for (var entry : fields.entrySet()) {
                if(entry.getValue() == null) {
                    continue;
                }

                dataOutputStream.writeByte(entry.getKey().byteValue());
                switch (entry.getValue()) {
                    case byte[] bytes -> {
                        dataOutputStream.writeShort(bytes.length);
                        dataOutputStream.write(bytes);
                    }
                    case byte[][] bytes -> {
                        for (var payloadEntry : bytes) {
                            dataOutputStream.writeShort(payloadEntry.length);
                            dataOutputStream.write(payloadEntry);
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value");
                }
            }
            socket.getOutputStream().write(byteArrayOutputStream.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot send message", exception);
        }
    }

    private int getPayloadLength(Map<Integer, ?> fields) {
        return fields.values()
                .stream()
                .mapToInt(this::getPayloadLength)
                .sum();
    }

    private int getPayloadLength(Object entry) {
        return switch (entry) {
            case byte[] bytes -> bytes.length + 3;
            case byte[][] bytes -> Arrays.stream(bytes).mapToInt(value -> value.length + 2).sum() + 1;
            case null -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + entry.getClass().getName());
        };
    }

    private void receive() {
        try(var dataInputStream = new DataInputStream(socket.getInputStream())) {
            while (socket.isConnected()) {
                var id = dataInputStream.readUnsignedByte();
                System.out.println("Read id: " + id);
                var length = dataInputStream.readInt();
                System.out.println("Read message with length: " + length);
                if (length <= 0) {
                    continue;
                }

                var payload = new byte[length];
                dataInputStream.readFully(payload);
                var fields = new HashMap<Integer, byte[]>();
                try(var payloadDataInputStream = new DataInputStream(new ByteArrayInputStream(payload))) {
                   while (true) {
                       var fieldId = payloadDataInputStream.read();
                       if(fieldId < 0) {
                           break;
                       }

                       var fieldLength = payloadDataInputStream.readUnsignedShort();
                       var value = new byte[fieldLength];
                       payloadDataInputStream.readFully(value);
                       fields.put(fieldId, value);
                   }
                }

                var packetType = ApnsPayloadTag.of(id);
                System.out.println(packetType);
                if(packetType == ApnsPayloadTag.NOTIFICATION) {
                    send(ApnsPayloadTag.ACK, Map.of(
                            1, authToken,
                            4, fields.get(0x4),
                            8, new byte[]{0x00}
                    ));
                }

                var packet = new ApnsPacket(packetType, fields);
                listeners.removeIf(listener -> {
                    var remove = listener.filter().apply(packet);
                    if(remove) {
                        listener.future().complete(packet);
                    }

                    return remove;
                });
            }
        }catch (IOException exception) {
            if(socket.isClosed()) {
                return;
            }

            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void close() throws IOException {
        if(socket != null) {
            socket.close();
        }

        if(keepAliveExecutor != null) {
            keepAliveExecutor.close();
        }

        if(readerService != null) {
            readerService.close();
        }
    }

    private CompletableFuture<ApnsBag> getAPNSBag() {
        return Medias.downloadAsync(URI.create("http://init-p01st.push.apple.com/bag"))
                .thenApply(ApnsBag::ofPlist);
    }

    private record ApnsBag(
            int hostCount,
            String hostname
    ) {
        private static ApnsBag ofPlist(byte[] plist) {
            try {
                var parsed = (NSDictionary) PropertyListParser.parse(plist);
                var bagBytes = parsed.get("bag").toJavaObject(byte[].class);
                var bag = (NSDictionary) PropertyListParser.parse(bagBytes);
                var hostCount = bag.get("APNSCourierHostcount")
                        .toJavaObject(Integer.class);
                var hostname = bag.get("APNSCourierHostname")
                        .toJavaObject(String.class);
                return new ApnsBag(hostCount, hostname);
            }catch (Throwable throwable) {
                throw new RuntimeException("Cannot parse apns bag", throwable);
            }
        }
    }

    private byte[] generateCSR(KeyPair keys) {
        try {
            var subject = new X500NameBuilder(BCStyle.INSTANCE)
                    .addRDN(BCStyle.C, "US")
                    .addRDN(BCStyle.ST, "CA")
                    .addRDN(BCStyle.L, "Cupertino")
                    .addRDN(BCStyle.O, "Apple Inc.")
                    .addRDN(BCStyle.OU, "iPhone")
                    .addRDN(BCStyle.CN, java.util.UUID.randomUUID().toString())
                    .build();
            var signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keys.getPrivate());
            var certificateRequest = new JcaPKCS10CertificationRequestBuilder(subject, keys.getPublic())
                    .build(signer);
            var stringWriter = new StringWriter();
            try (var pemWriter = new PemWriter(stringWriter)) {
                pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", certificateRequest.getEncoded()));
            }
            return stringWriter.toString()
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot generate csr", throwable);
        }
    }

    private record DeviceActivationInfo(boolean ackReceived, boolean showSettings, ActivationRecord activationRecord) {
        private static DeviceActivationInfo ofPlist(byte[] plist) {
            try {
                var parsed = (NSDictionary) PropertyListParser.parse(plist);
                var deviceActivation = (NSDictionary) parsed.get("device-activation");
                var ackReceived = deviceActivation.get("ack-received")
                        .toJavaObject(boolean.class);
                var showSettings = deviceActivation.get("show-settings")
                        .toJavaObject(boolean.class);
                var activationDictionary = (NSDictionary) deviceActivation.get("activation-record");
                var deviceCertificate = activationDictionary.get("DeviceCertificate")
                        .toJavaObject(byte[].class);
                var activationRecord = new ActivationRecord(deviceCertificate);
                return new DeviceActivationInfo(ackReceived, showSettings, activationRecord);
            }catch (Throwable throwable) {
                throw new RuntimeException("Cannot parse device activation info", throwable);
            }
        }

        private record ActivationRecord(byte[] deviceCertificate) {

        }
    }

    private record Listener(Function<ApnsPacket, Boolean> filter, CompletableFuture<ApnsPacket> future) {
        private Listener(Function<ApnsPacket, Boolean> filter) {
            this(filter, new CompletableFuture<>());
        }
    }
}
