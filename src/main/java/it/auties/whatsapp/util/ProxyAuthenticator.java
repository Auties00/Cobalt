package it.auties.whatsapp.util;

import java.net.*;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Write a custom implementation of Socket that supports a custom proxy
public class ProxyAuthenticator extends Authenticator {
    private final static Map<String, URI> credentials;

    static {
        allowAll();
        credentials = new ConcurrentHashMap<>();
    }

    public static void allowAll() {
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }

    public static void register(URI uri) {
        credentials.put("%s:%s".formatted(uri.getHost(), uri.getPort()), uri);
    }

    public static void unregister(URI uri) {
        credentials.remove("%s:%s".formatted(uri.getHost(), uri.getPort()));
    }

    public static Proxy getProxy(URI uri) {
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(uri.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(uri));
        var host = Objects.requireNonNull(uri.getHost(), "Invalid proxy, expected a host: %s".formatted(uri));
        var port = getProxyPort(scheme, uri.getPort()).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(uri)));
        return switch (scheme) {
            case "http", "https" -> new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            case "socks4", "socks5" -> new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
            default -> throw new IllegalStateException("Unexpected scheme: " + scheme);
        };
    }

    private static OptionalInt getProxyPort(String scheme, int port) {
        return port != -1 ? OptionalInt.of(port) : switch (scheme) {
            case "http" -> OptionalInt.of(80);
            case "https" -> OptionalInt.of(443);
            default -> OptionalInt.empty();
        };
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        var host = "%s:%s".formatted(getRequestingHost(), getRequestingPort());
        var info = credentials.get(host);
        if (info == null) {
            return super.getPasswordAuthentication();
        }

        var userInfo = info.getUserInfo().split(":", 2);
        Validate.isTrue(userInfo.length == 2, "Invalid proxy credentials");
        return new PasswordAuthentication(userInfo[0], userInfo[1].toCharArray());
    }
}
