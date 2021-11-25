package it.auties.whatsapp.protobuf.temp;

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
public class InvoiceMessage {
  @JsonProperty(value = "10")
  @JsonPropertyDescription("bytes")
  private byte[] attachmentJpegThumbnail;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String attachmentDirectPath;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bytes")
  private byte[] attachmentFileEncSha256;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bytes")
  private byte[] attachmentFileSha256;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("int64")
  private long attachmentMediaKeyTimestamp;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("bytes")
  private byte[] attachmentMediaKey;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String attachmentMimetype;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("InvoiceMessageAttachmentType")
  private InvoiceMessageAttachmentType attachmentType;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String token;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
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
