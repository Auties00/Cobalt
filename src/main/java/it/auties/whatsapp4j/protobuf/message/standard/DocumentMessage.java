package it.auties.whatsapp4j.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessage;
import it.auties.whatsapp4j.protobuf.message.model.MediaMessageType;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a document inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
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
   * Constructs a new builder to create a DocumentMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param media         the non null document that the new message wraps
   * @param mimeType      the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param title         the title of the document that the new message wraps
   * @param pageCount     the number of pages of the document that the new message wraps
   * @param fileName      the name of the document that the new message wraps
   * @param jpegThumbnail the thumbnail of the document that the new message wraps
   * @param contextInfo   the context info that the new message wraps
   *
   * @return a non null new message
   */
  @Builder(builderClassName = "NewDocumentMessageBuilder", builderMethodName = "newDocumentMessage", buildMethodName = "create")
  private static DocumentMessage builder(byte @NotNull [] media, String mimeType, String title, int pageCount, String fileName, byte[] jpegThumbnail, ContextInfo contextInfo) {
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.DOCUMENT);
    return DocumentMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().data())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.DOCUMENT.defaultMimeType()))
            .fileName(fileName)
            .pageCount(pageCount)
            .jpegThumbnail(jpegThumbnail)
            .contextInfo(contextInfo)
            .create();
  }

  private static DocumentMessageBuilder<?, ?> builder(){
    return new DocumentMessageBuilderImpl();
  }

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
