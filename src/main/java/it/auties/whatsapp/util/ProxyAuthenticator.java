package it.auties.whatsapp.util;

import java.net.*;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyAuthenticator extends Authenticator {
    private static volatile ProxyAuthenticator instance;

    static {
        allowAll();
    }

    public static void allowAll() {
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }

    public static Proxy getProxy(URI uri) {
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        var scheme = Objects.requireNonNull(uri.getScheme(), "Invalid proxy, expected a scheme: %s".formatted(uri));
        var host = Objects.requireNonNull(uri.getHost(), "Invalid proxy, expected a host: %s".formatted(uri));
        var port = getProxyPort(scheme, uri.getPort()).orElseThrow(() -> new NullPointerException("Invalid proxy, expected a port: %s".formatted(uri)));
        return switch (scheme) {
            case "http", "https" -> new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
            case "socks5" -> new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port));
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

    // We can't have a specialized authenticator as the Socket class doesn't provide a way to pass one
    // Http(s) sockets under the hood use the HTTP(S)UrlConnection class, which supports setAuthenticator
    // But Socks5 proxies invoke Authenticator#requestPasswordAuthentication which uses the default authenticator
    // For now this is good enough, even though in a shared environment it could be a problem because two proxies could have the same host and port, but different users
    public static ProxyAuthenticator globalAuthenticator() {
        if(instance == null) {
            instance = new ProxyAuthenticator();
        }

        return instance;
    }

    private final Map<String, URI> credentials;
    private ProxyAuthenticator() {
        this.credentials = new ConcurrentHashMap<>();
    }

    public void register(URI uri) {
        credentials.put("%s:%s".formatted(uri.getHost(), uri.getPort()), uri);
    }

    public void unregister(URI uri) {
        credentials.remove("%s:%s".formatted(uri.getHost(), uri.getPort()));
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
