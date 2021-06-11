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
    headers.put("Host", List.of("web.whatsapp.com"));
    headers.put("Pragma", List.of("no-cache"));
  }
}