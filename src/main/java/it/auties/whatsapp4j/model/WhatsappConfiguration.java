package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.binary.BinaryArray;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.function.Function;

@Builder
@Data
@Accessors(fluent = true)
public class WhatsappConfiguration {
  @Default
  private final @NotNull String whatsappUrl = "wss://web.whatsapp.com/ws";

  @Default
  private final @NotNull String whatsappTag = "W4J";

  @Default
  private final @NotNull String requestTag = BinaryArray.random(12).toHex();

  @Default
  private final @NotNull String description = "Whatsapp4j";

  @Default
  private final @NotNull String shortDescription = "W4J";

  @Default
  private final Function<String, Boolean> reconnectWhenDisconnected = (reason) -> true;

  @Default
  private final boolean async = true;

  public static @NotNull WhatsappConfiguration defaultOptions(){
    return WhatsappConfiguration.builder().build();
  }
}
