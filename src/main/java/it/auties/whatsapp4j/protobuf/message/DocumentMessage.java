package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a document inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public class DocumentMessage extends MediaMessage {
  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "11")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "10")
  private String directPath;

  @JsonProperty(value = "9")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8")
  private String fileName;

  @JsonProperty(value = "7")
  private byte[] mediaKey;

  @JsonProperty(value = "6")
  private int pageCount;

  @JsonProperty(value = "5")
  private long fileLength;

  @JsonProperty(value = "4")
  private byte[] fileSha256;

  @JsonProperty(value = "3")
  private String title;

  @JsonProperty(value = "2")
  private String mimetype;

  @JsonProperty(value = "1")
  private String url;

  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.DOCUMENT;
  }
}
