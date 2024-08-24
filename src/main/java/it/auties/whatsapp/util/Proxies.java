package it.auties.whatsapp.util;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Objects;
import java.util.OptionalInt;

public final class Proxies {
    public static Proxy toProxy(URI uri) {
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(uri.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(uri));
        var host = Objects.requireNonNull(uri.getHost(), "Invalid proxy, expected a host: %s".formatted(uri));
        var port = getDefaultPort(scheme, uri.getPort()).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(uri)));
        return switch (scheme.toLowerCase()) {
            case "http", "https" -> new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
            case "socks5", "socks5h" -> new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port));
            default -> throw new IllegalStateException("Unexpected scheme: " + scheme);
        };
    }

    private static OptionalInt getDefaultPort(String scheme, int port) {
        return port != -1 ? OptionalInt.of(port) : switch (scheme.toLowerCase()) {
            case "http" -> OptionalInt.of(80);
            case "https" -> OptionalInt.of(443);
            default -> OptionalInt.empty();
        };
    }

    public static UserInfo parseUserInfo(String userInfo) {
        if(userInfo == null || userInfo.isEmpty()) {
            return null;
        }

        var data = userInfo.split(":", 2);
        if(data.length > 2) {
            throw new IllegalArgumentException("Invalid proxy authentication: " + userInfo);
        }

        return new UserInfo(data[0], data.length == 2 ? data[1] : null);
    }

    public record UserInfo(String username, String password) {

    }
}
