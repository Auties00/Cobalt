package it.auties.whatsapp4j.configuration;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

@Builder
@Data
@Accessors(fluent = true)
public class WhatsappConfiguration {
  @NotNull
  @Default
  private final String whatsappUrl = "wss://web.whatsapp.com/ws";

  @NotNull
  @Default
  private final String clientId = Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphabetic(16).getBytes());

  @NotNull
  @Default
  private final String tag = "W4J";
  @NotNull
  @Default
  private final String description = "Whatsapp4j";

  @NotNull
  @Default
  private final String shortDescription = "W4J";

  public static WhatsappConfiguration defaultOptions(){
    return WhatsappConfiguration.builder().build();
  }
}
