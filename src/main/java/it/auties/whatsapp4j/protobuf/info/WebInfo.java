package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.misc.WebdPayload;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebInfo {
  @JsonProperty(value = "4", required = false)
  private WebInfoWebSubPlatform webSubPlatform;

  @JsonProperty(value = "3", required = false)
  private WebdPayload webdPayload;

  @JsonProperty(value = "2", required = false)
  private String version;

  @JsonProperty(value = "1", required = false)
  private String refToken;

  @Accessors(fluent = true)
  public enum WebInfoWebSubPlatform {
    WEB_BROWSER(0),
    APP_STORE(1),
    WIN_STORE(2),
    DARWIN(3),
    WIN32(4);

    private final @Getter int index;

    WebInfoWebSubPlatform(int index) {
      this.index = index;
    }

    @JsonCreator
    public static WebInfoWebSubPlatform forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
