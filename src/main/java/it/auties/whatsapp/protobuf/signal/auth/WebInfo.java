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
public class WebInfo {
  @JsonProperty("4")
  @JsonPropertyDescription("WebInfoWebSubPlatform")
  private WebInfoWebSubPlatform webSubPlatform;

  @JsonProperty("3")
  @JsonPropertyDescription("WebdPayload")
  private WebPayload webdPayload;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String version;

  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String refToken;

  public WebInfo(@NonNull WebInfoWebSubPlatform webSubPlatform){
    this.webSubPlatform = webSubPlatform;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum WebInfoWebSubPlatform {
    WEB_BROWSER(0),
    APP_STORE(1),
    WIN_STORE(2),
    DARWIN(3),
    WIN32(4);

    @Getter
    private int index;

    @JsonCreator
    public static WebInfoWebSubPlatform forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
