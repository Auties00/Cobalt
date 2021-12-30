package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ShopMessage {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int32")
  private int messageVersion;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("ShopMessageSurface")
  private ShopMessageSurface surface;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String id;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ShopMessageSurface {
    UNKNOWN_SURFACE(0),
    FB(1),
    IG(2),
    WA(3);

    @Getter
    private final int index;

    @JsonCreator
    public static ShopMessageSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
