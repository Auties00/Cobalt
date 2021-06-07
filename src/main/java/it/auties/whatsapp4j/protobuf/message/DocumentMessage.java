package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a document inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class DocumentMessage extends MediaMessage {
  /**
   * The thumbnail for this document encoded as jpeg in an array of bytes
   */
  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link DocumentMessage#mediaKey()}
   */
  @JsonProperty(value = "11")
  private long mediaKeyTimestamp;

  /**
   * The direct path to the encoded media that this object wraps
   */
  @JsonProperty(value = "10")
  private String directPath;

  /**
   * The sha256 of the encoded media that this object wraps
   */
  @JsonProperty(value = "9")
  private byte[] fileEncSha256;

  /**
   * The name of the document that this object wraps
   */
  @JsonProperty(value = "8")
  private String fileName;

  /**
   * The media key of the document that this object wraps.
   * This key is used to decrypt the encoded media by {@link CypherUtils#mediaDecrypt(MediaMessage)}
   */
  @JsonProperty(value = "7")
  private byte[] mediaKey;

  /**
   * The unsigned length in pages of the document that this object wraps
   */
  @JsonProperty(value = "6")
  private int pageCount;

  /**
   * The unsigned size of the decoded media that this object wraps
   */
  @JsonProperty(value = "5")
  private long fileLength;

  /**
   * The sha256 of the decoded media that this object wraps
   */
  @JsonProperty(value = "4")
  private byte[] fileSha256;

  /**
   * The title of the document that this object wraps
   */
  @JsonProperty(value = "3")
  private String title;

  /**
   * The mime type of the audio that this object wraps.
   * Most of the times this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty(value = "2")
  private String mimetype;

  /**
   * The upload url of the encoded document that this object wraps
   */
  @JsonProperty(value = "1")
  private String url;

  /**
   * Returns the media type of the document that this object wraps
   *
   * @return {@link MediaMessageType#DOCUMENT}
   */
  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.DOCUMENT;
  }
}
