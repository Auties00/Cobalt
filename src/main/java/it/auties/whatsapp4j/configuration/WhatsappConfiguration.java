package it.auties.whatsapp4j.configuration;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Builder
@Data
@Accessors(fluent = true)
public class WhatsappConfiguration {
  @Default
  private final @NotNull String whatsappUrl = "wss://web.whatsapp.com/ws";

  @Default
  private final @NotNull String tag = "W4J";

  @Default
  private final @NotNull String description = "Whatsapp4j";

  @Default
  private final @NotNull String shortDescription = "W4J";

  @Default
  private final Function<String, Boolean> reconnectWhenDisconnected = (reason) -> true;

  public static @NotNull WhatsappConfiguration defaultOptions(){
    return WhatsappConfiguration.builder().build();
  }
}
