package it.auties.whatsapp.protobuf.authentication;

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
  @JsonProperty(value = "4")
  @JsonPropertyDescription("WebInfoWebSubPlatform")
  private WebInfoWebSubPlatform webSubPlatform;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("WebdPayload")
  private WebdPayload webdPayload;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String version;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String refToken;

  public WebInfo(@NonNull WebInfoWebSubPlatform webSubPlatform){
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
