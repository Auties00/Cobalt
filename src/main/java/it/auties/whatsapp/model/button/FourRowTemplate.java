package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.button.ButtonStructureMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class FourRowTemplate implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = DocumentMessage.class)
  private DocumentMessage documentMessage;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ButtonStructureMessage.class)
  private ButtonStructureMessage highlyStructuredMessage;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
  private ImageMessage imageMessage;

  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = VideoMessage.class)
  private VideoMessage videoMessage;

  @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
  private LocationMessage locationMessage;

  @ProtobufProperty(index = 6, type = MESSAGE, concreteType = ButtonStructureMessage.class)
  private ButtonStructureMessage content;

  @ProtobufProperty(index = 7, type = MESSAGE, concreteType = ButtonStructureMessage.class)
  private ButtonStructureMessage footer;

  @ProtobufProperty(index = 8, type = MESSAGE,
          concreteType = ButtonTemplate.class, repeated = true)
  private List<ButtonTemplate> buttons;

  public TitleType titleType() {
    if (documentMessage != null) return TitleType.DOCUMENT_MESSAGE;
    if (highlyStructuredMessage != null) return TitleType.HIGHLY_STRUCTURED_MESSAGE;
    if (imageMessage != null) return TitleType.IMAGE_MESSAGE;
    if (videoMessage != null) return TitleType.VIDEO_MESSAGE;
    if (locationMessage != null) return TitleType.LOCATION_MESSAGE;
    return TitleType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum TitleType implements ProtobufMessage {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(1),
    HIGHLY_STRUCTURED_MESSAGE(2),
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

  public static class FourRowTemplateBuilder {
    public FourRowTemplateBuilder hydratedButtons(List<ButtonTemplate> buttons) {
      if (this.buttons == null) this.buttons = new ArrayList<>();
      this.buttons.addAll(buttons);
      return this;
    }
  }
}
