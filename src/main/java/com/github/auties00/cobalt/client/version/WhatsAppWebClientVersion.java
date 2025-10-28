package com.github.auties00.cobalt.client.version;

import com.github.auties00.cobalt.model.auth.Version;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class WhatsAppWebClientVersion implements WhatsAppClientVersion {
    private static volatile WhatsAppWebClientVersion webInfo;
    private static final Object webInfoLock = new Object();

    private static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com");
    private static final char[] WEB_UPDATE_PATTERN = "\"client_revision\":".toCharArray();

    private final Version version;

    private WhatsAppWebClientVersion(Version version) {
        this.version = version;
    }

    public static WhatsAppWebClientVersion of() {
        if (webInfo == null) {
            synchronized (webInfoLock) {
                if(webInfo == null) {
                    webInfo = queryWebInfo();
                }
            }
        }
        return webInfo;
    }

    private static WhatsAppWebClientVersion queryWebInfo() {
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(WEB_UPDATE_URL)
                    .GET()
                    .header("User-Agent", WEB_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode() != 200) {
                throw new IllegalStateException("Cannot query web version: status code " + response.statusCode());
            }
            try (var inputStream = response.body()) {
                var patternIndex = 0;
                int value;
                while ((value = inputStream.read()) != -1) {
                    if (value == WEB_UPDATE_PATTERN[patternIndex]) {
                        if (++patternIndex == WEB_UPDATE_PATTERN.length) {
                            var clientVersion = 0;
                            while ((value = inputStream.read()) != -1 && Character.isDigit(value)) {
                                clientVersion *= 10;
                                clientVersion += value - '0';
                            }
                            var version = new Version(2, 3000, clientVersion);
                            return new WhatsAppWebClientVersion(version);
                        }
                    } else {
                        patternIndex = 0;
                        if (value == WEB_UPDATE_PATTERN[0]) {
                            patternIndex = 1;
                        }
                    }
                }
                throw new IllegalStateException("Cannot find client_revision in web update response");
            }
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot query web version", exception);
        }
    }

    @Override
    public Version latest() {
        return version;
    }
}