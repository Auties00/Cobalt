package it.auties.whatsapp.registration.apns;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

class AppleTrustManager implements X509TrustManager {
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