package it.auties.whatsapp4j.common.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.model.misc.WebdPayload;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebInfo {
  @JsonProperty(value = "4")
  private WebInfoWebSubPlatform webSubPlatform;

  @JsonProperty(value = "3")
  private WebdPayload webdPayload;

  @JsonProperty(value = "2")
  private String version;

  @JsonProperty(value = "1")
  private String refToken;

  public WebInfo(WebInfoWebSubPlatform webSubPlatform){
    this.webSubPlatform = webSubPlatform;
  }

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
