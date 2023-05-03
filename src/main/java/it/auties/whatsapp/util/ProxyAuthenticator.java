package it.auties.whatsapp.util;

import lombok.NonNull;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyAuthenticator extends Authenticator {
    private final static Map<String, URI> credentials;

    static {
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        credentials = new ConcurrentHashMap<>();
    }

    public static void register(@NonNull URI uri){
        credentials.put("%s:%s".formatted(uri.getHost(), uri.getPort()), uri);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if(getRequestorType() != RequestorType.PROXY){
            return super.getPasswordAuthentication();
        }

        var info = credentials.get("%s:%s".formatted(getRequestingHost(), getRequestingPort()));
        if(info == null) {
            return super.getPasswordAuthentication();
        }

        var userInfo = info.getUserInfo().split(":", 2);
        return new PasswordAuthentication(userInfo[0], userInfo[1].toCharArray());
    }
}
