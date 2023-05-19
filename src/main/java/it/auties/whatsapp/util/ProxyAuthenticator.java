package it.auties.whatsapp.util;

import lombok.NonNull;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Write a custom implementation of Socket that supports a custom proxy
// It could take a while
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

    public static void unregister(@NonNull URI uri){
        credentials.remove("%s:%s".formatted(uri.getHost(), uri.getPort()));
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        var host = "%s:%s".formatted(getRequestingHost(), getRequestingPort());
        var info = credentials.get(host);
        if(info == null) {
            return super.getPasswordAuthentication();
        }

        var userInfo = info.getUserInfo().split(":", 2);
        return new PasswordAuthentication(userInfo[0], userInfo[1].toCharArray());
    }
}
