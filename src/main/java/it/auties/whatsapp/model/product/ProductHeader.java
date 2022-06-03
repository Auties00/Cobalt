package it.auties.whatsapp.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ProductHeader {
  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  @ProtobufProperty(index = 2, type = STRING)
  private String subtitle;

  @ProtobufProperty(index = 5, type = BOOLEAN)
  private boolean hasMediaAttachment;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = DocumentMessage.class)
  private DocumentMessage documentMessage;

  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ImageMessage.class)
  private ImageMessage imageMessage;

  @ProtobufProperty(index = 6, type = BYTES)
  private byte[] jpegThumbnail;

  @ProtobufProperty(index = 7, type = MESSAGE, concreteType = VideoMessage.class)
  private VideoMessage videoMessage;

  public Media mediaType() {
    if (documentMessage != null) return Media.DOCUMENT_MESSAGE;
    if (imageMessage != null) return Media.IMAGE_MESSAGE;
    if (jpegThumbnail != null) return Media.JPEG_THUMBNAIL;
    if (videoMessage != null) return Media.VIDEO_MESSAGE;
    return Media.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Media {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(3),
    IMAGE_MESSAGE(4),
    JPEG_THUMBNAIL(6),
    VIDEO_MESSAGE(7);

    @Getter
    private final int index;

    @JsonCreator
    public static Media forIndex(int index) {
      return Arrays.stream(values())
              .filter(entry -> entry.index() == index)
              .findFirst()
              .orElse(Media.UNKNOWN);
    }
  }
}
