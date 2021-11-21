package it.auties.whatsapp.socket;

import jakarta.websocket.ClientEndpointConfig;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * A class used to define a pair of headers necessary to start a session with WhatsappWeb's WebSocket.
 * Without these, WhatsappWeb's WebSocket would respond with a 401 http status error code.
 */
public class WhatsappSocketConfiguration extends ClientEndpointConfig.Configurator{
    private static final Map<String, List<String>> HEADERS = Map.of(
            "Origin",  List.of("https://web.whatsapp.com"),
            "Host", List.of("web.whatsapp.com"),
            "Accept-Encoding", List.of("gzip, deflate, br"),
            "Accept-Language", List.of("en-US,en;q=0.9"),
            "Cache-Control", List.of("no-cache"),
            "Pragma", List.of("no-cache"),
            "Sec-WebSocket-Extensions", List.of("permessage-deflate", "client_max_window_bits"),
            "Sec-WebSocket-Version", List.of("13"),
            "Upgrade", List.of("websocket"),
            "User-Agent", List.of("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36")
    );

    @Override
    public void beforeRequest(@NonNull Map<String, List<String>> headers) {
        headers.putAll(HEADERS);
    }
}