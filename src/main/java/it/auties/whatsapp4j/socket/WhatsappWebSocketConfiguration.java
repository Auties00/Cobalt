package it.auties.whatsapp4j.socket;

import jakarta.validation.constraints.NotNull;
import jakarta.websocket.ClientEndpointConfig;

import java.util.List;
import java.util.Map;

/**
 * A class used to define a pair of headers necessary to start a session with WhatsappWeb's WebSocket.
 * Without these, WhatsappWeb's WebSocket would respond with a 401 http status error code.
 */
public class WhatsappWebSocketConfiguration extends ClientEndpointConfig.Configurator{
  @Override
  public void beforeRequest(@NotNull Map<String, List<String>> headers) {
    headers.put("Origin", List.of("https://web.whatsapp.com"));
    headers.put("User-Agent", List.of("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.51"));
    headers.put("Host", List.of("web.whatsapp.com"));
    headers.put("Pragma", List.of("no-cache"));
    headers.put("Accept-Encoding", List.of("gzip, deflate, br"));
    headers.put("Accept-Language", List.of("en-US,en;q=0.9,hi;q=0.8"));
    headers.put("Upgrade", List.of("websocket"));
    headers.put("Cache-Control", List.of("no-cache"));
    headers.put("Connection", List.of("Upgrade"));
    headers.put("Cookie", List.of("***********"));
    headers.put("Sec-WebSocket-Version", List.of("13"));
    headers.put("Sec-WebSocket-Extensions", List.of("permessage-deflate; client_max_window_bits"));
  }
}