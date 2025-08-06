package it.auties.whatsapp.util;

import java.net.URI;
import java.net.http.HttpClient;

public final class HttpClientFactory {
    public static HttpClient create(URI proxy) {
        var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (proxy != null) {
            builder.proxy(Proxies.toProxySelector(proxy));
            builder.authenticator(Proxies.toAuthenticator(proxy));
        }
        return builder.build();
    }
}
