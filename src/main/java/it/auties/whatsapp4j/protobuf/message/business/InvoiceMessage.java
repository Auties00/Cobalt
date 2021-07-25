package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class InvoiceMessage {
  @JsonProperty(value = "10")
  private byte[] attachmentJpegThumbnail;

  @JsonProperty(value = "9")
  private String attachmentDirectPath;

  @JsonProperty(value = "8")
  private byte[] attachmentFileEncSha256;

  @JsonProperty(value = "7")
  private byte[] attachmentFileSha256;

  @JsonProperty(value = "6")
  private long attachmentMediaKeyTimestamp;

  @JsonProperty(value = "5")
  private byte[] attachmentMediaKey;

  @JsonProperty(value = "4")
  private String attachmentMimetype;

  @JsonProperty(value = "3")
  private InvoiceMessageAttachmentType attachmentType;

  @JsonProperty(value = "2")
  private String token;

  @JsonProperty(value = "1")
  private String note;

  @Accessors(fluent = true)
  public enum InvoiceMessageAttachmentType {
    IMAGE(0),
    PDF(1);

    private final @Getter int index;

    InvoiceMessageAttachmentType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static InvoiceMessageAttachmentType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
