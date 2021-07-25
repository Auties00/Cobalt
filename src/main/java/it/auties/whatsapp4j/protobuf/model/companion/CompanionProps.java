package it.auties.whatsapp4j.protobuf.model.companion;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.app.AppVersion;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class CompanionProps {
  @JsonProperty(value = "4")
  private boolean requireFullSync;

  @JsonProperty(value = "3")
  private CompanionPropsPlatformType platformType;

  @JsonProperty(value = "2")
  private AppVersion version;

  @JsonProperty(value = "1")
  private String os;

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

    private final @Getter int index;

    CompanionPropsPlatformType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static CompanionPropsPlatformType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
