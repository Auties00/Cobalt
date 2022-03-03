package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.message.standard.DocumentMessage;
import it.auties.whatsapp.protobuf.message.standard.ImageMessage;
import it.auties.whatsapp.protobuf.message.standard.LocationMessage;
import it.auties.whatsapp.protobuf.message.standard.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class HydratedFourRowTemplate {
  @JsonProperty("1")
  @JsonPropertyDescription("DocumentMessage")
  private DocumentMessage documentMessage;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String hydratedTitleText;

  @JsonProperty("3")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage imageMessage;

  @JsonProperty("4")
  @JsonPropertyDescription("VideoMessage")
  private VideoMessage videoMessage;

  @JsonProperty("5")
  @JsonPropertyDescription("LocationMessage")
  private LocationMessage locationMessage;

  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String hydratedContentText;

  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String hydratedFooterText;

  @JsonProperty("8")
  @JsonPropertyDescription("HydratedTemplateButton")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HydratedTemplateButton> hydratedButtons;

  @JsonProperty("9")
  @JsonPropertyDescription("string")
  private String templateId;

  public TitleType titleType() {
    if (documentMessage != null) return TitleType.DOCUMENT_MESSAGE;
    if (hydratedTitleText != null) return TitleType.HYDRATED_TITLE_TEXT;
    if (imageMessage != null) return TitleType.IMAGE_MESSAGE;
    if (videoMessage != null) return TitleType.VIDEO_MESSAGE;
    if (locationMessage != null) return TitleType.LOCATION_MESSAGE;
    return TitleType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum TitleType {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(1),
    HYDRATED_TITLE_TEXT(2),
    IMAGE_MESSAGE(3),
    VIDEO_MESSAGE(4),
    LOCATION_MESSAGE(5);

    @Getter
    private final int index;

    @JsonCreator
    public static TitleType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(TitleType.UNKNOWN);
    }
  }
}
