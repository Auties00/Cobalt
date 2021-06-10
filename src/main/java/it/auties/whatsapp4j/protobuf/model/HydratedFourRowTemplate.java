package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.message.DocumentMessage;
import it.auties.whatsapp4j.protobuf.message.ImageMessage;
import it.auties.whatsapp4j.protobuf.message.LocationMessage;
import it.auties.whatsapp4j.protobuf.message.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HydratedFourRowTemplate {
  @JsonProperty(value = "5")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  private VideoMessage videoMessage;

  @JsonProperty(value = "3")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  private String hydratedTitleText;

  @JsonProperty(value = "1")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "9")
  private String templateId;

  @JsonProperty(value = "8")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HydratedTemplateButton> hydratedButtons;

  @JsonProperty(value = "7")
  private String hydratedFooterText;

  @JsonProperty(value = "6")
  private String hydratedContentText;

  public Title titleCase() {
    if (documentMessage != null) return Title.DOCUMENT_MESSAGE;
    if (hydratedTitleText != null) return Title.HYDRATED_TITLE_TEXT;
    if (imageMessage != null) return Title.IMAGE_MESSAGE;
    if (videoMessage != null) return Title.VIDEO_MESSAGE;
    if (locationMessage != null) return Title.LOCATION_MESSAGE;
    return Title.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Title {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(1),
    HYDRATED_TITLE_TEXT(2),
    IMAGE_MESSAGE(3),
    VIDEO_MESSAGE(4),
    LOCATION_MESSAGE(5);

    private final @Getter int index;

    Title(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Title forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Title.UNKNOWN);
    }
  }
}
