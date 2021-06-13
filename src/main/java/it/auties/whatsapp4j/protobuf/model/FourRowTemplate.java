package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.message.business.HighlyStructuredMessage;
import it.auties.whatsapp4j.protobuf.message.standard.DocumentMessage;
import it.auties.whatsapp4j.protobuf.message.standard.ImageMessage;
import it.auties.whatsapp4j.protobuf.message.standard.LocationMessage;
import it.auties.whatsapp4j.protobuf.message.standard.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class FourRowTemplate {
  @JsonProperty(value = "5")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  private VideoMessage videoMessage;

  @JsonProperty(value = "3")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  private HighlyStructuredMessage highlyStructuredMessage;

  @JsonProperty(value = "1")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "8")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<TemplateButton> buttons;

  @JsonProperty(value = "7")
  private HighlyStructuredMessage footer;

  @JsonProperty(value = "6")
  private HighlyStructuredMessage content;

  public Title titleCase() {
    if (documentMessage != null) return Title.DOCUMENT_MESSAGE;
    if (highlyStructuredMessage != null) return Title.HIGHLY_STRUCTURED_MESSAGE;
    if (imageMessage != null) return Title.IMAGE_MESSAGE;
    if (videoMessage != null) return Title.VIDEO_MESSAGE;
    if (locationMessage != null) return Title.LOCATION_MESSAGE;
    return Title.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Title {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(1),
    HIGHLY_STRUCTURED_MESSAGE(2),
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
