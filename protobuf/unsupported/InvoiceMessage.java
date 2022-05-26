package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class InvoiceMessage {

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum InvoiceMessageAttachmentType {
    IMAGE(0),
    PDF(1);

    @Getter private final int index;

    public static InvoiceMessageAttachmentType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @ProtobufProperty(index = 1, type = STRING)
  private String note;

  @ProtobufProperty(index = 2, type = STRING)
  private String token;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = InvoiceMessageAttachmentType.class)
  private InvoiceMessageAttachmentType attachmentType;

  @ProtobufProperty(index = 4, type = STRING)
  private String attachmentMimetype;

  @ProtobufProperty(index = 5, type = BYTES)
  private byte[] attachmentMediaKey;

  @ProtobufProperty(index = 6, type = INT64)
  private Long attachmentMediaKeyTimestamp;

  @ProtobufProperty(index = 7, type = BYTES)
  private byte[] attachmentFileSha256;

  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] attachmentFileEncSha256;

  @ProtobufProperty(index = 9, type = STRING)
  private String attachmentDirectPath;

  @ProtobufProperty(index = 10, type = BYTES)
  private byte[] attachmentJpegThumbnail;
}
