package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class InvoiceMessage {

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] attachmentJpegThumbnail;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("string")
  private String attachmentDirectPath;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] attachmentFileEncSha256;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] attachmentFileSha256;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("int64")
  private long attachmentMediaKeyTimestamp;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] attachmentMediaKey;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String attachmentMimetype;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("InvoiceMessageAttachmentType")
  private InvoiceMessageAttachmentType attachmentType;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String token;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String note;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum InvoiceMessageAttachmentType {
    IMAGE(0),
    PDF(1);

    @Getter
    private final int index;

    @JsonCreator
    public static InvoiceMessageAttachmentType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
