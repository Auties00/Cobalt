package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Companion {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String os;

  @JsonProperty("2")
  @JsonPropertyDescription("AppVersion")
  private Version version;

  @JsonProperty("3")
  @JsonPropertyDescription("CompanionPropsPlatformType")
  private CompanionPropsPlatformType platformType;

  @JsonProperty("4")
  @JsonPropertyDescription("bool")
  private boolean requireFullSync;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum CompanionPropsPlatformType {
    UNKNOWN(0),
    CHROME(1),
    FIREFOX(2),
    IE(3),
    OPERA(4),
    SAFARI(5),
    EDGE(6),
    DESKTOP(7),
    IPAD(8),
    ANDROID_TABLET(9),
    OHANA(10),
    ALOHA(11),
    CATALINA(12);

    @Getter
    private final int index;

    @JsonCreator
    public static CompanionPropsPlatformType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
