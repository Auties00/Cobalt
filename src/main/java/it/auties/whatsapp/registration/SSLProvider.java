package it.auties.whatsapp.registration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SSLProvider {
    public static SSLContext getRandomSslContext()  {
        return getSslContext(null, false);
    }

    public static SSLContext getSslContext(Integer tlsVersion, boolean ignoreCerts)  {
      try {
          var random = ThreadLocalRandom.current();
          var sslContext = SSLContext.getInstance("TLSv1." + Objects.requireNonNullElseGet(tlsVersion, SSLProvider::randomTlsVersion));
          sslContext.init(null, ignoreCerts ? getTrustAnyIssuer() : null, null);
          return sslContext;
      }catch (GeneralSecurityException exception) {
          throw new IllegalArgumentException("Cannot get ssl context for tls version " + tlsVersion, null);
      }
    }

    private static String randomTlsVersion() {
        return ThreadLocalRandom.current().nextBoolean() ? "3" : "2";
    }

    public static SSLParameters getParameters(SSLContext sslContext) {
        var supportedCiphers = Arrays.stream(sslContext.getDefaultSSLParameters().getCipherSuites())
                .filter(entry -> ThreadLocalRandom.current().nextBoolean())
                .sorted()
                .collect(Collectors.collectingAndThen(Collectors.toList(), result -> { Collections.shuffle(result); return result; }))
                .toArray(String[]::new);
        var sslParameters = sslContext.getDefaultSSLParameters();
        sslParameters.setCipherSuites(supportedCiphers);
        return sslParameters;
    }

    private static TrustManager[] getTrustAnyIssuer() {
        return new TrustManager[]{
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
        };
    }
}
