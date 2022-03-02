package it.auties.whatsapp.protobuf.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.MediaMessage;
import it.auties.whatsapp.protobuf.message.model.MediaMessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a document inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class DocumentMessage extends MediaMessage {
  /**
   * The upload url of the encoded document that this object wraps
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String url;

  /**
   * The mime type of the audio that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String mimetype;

  /**
   * The title of the document that this object wraps
   */
  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String title;

  /**
   * The sha256 of the decoded media that this object wraps
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded media that this object wraps
   */
  @JsonProperty("5")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  /**
   * The unsigned length in pages of the document that this object wraps
   */
  @JsonProperty("6")
  @JsonPropertyDescription("uint32")
  private int pageCount;

  /**
   * The media key of the document that this object wraps.
   */
  @JsonProperty("7")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  /**
   * The name of the document that this object wraps
   */
  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String fileName;

  /**
   * The sha256 of the encoded media that this object wraps
   */
  @JsonProperty("9")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  /**
   * The direct path to the encoded media that this object wraps
   */
  @JsonProperty("10")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link DocumentMessage#key()}
   */
  @JsonProperty("11")
  @JsonPropertyDescription("uint64")
  private long mediaKeyTimestamp;
  
  /**
   * The thumbnail for this document encoded as jpeg in an array of bytes
   */
  @JsonProperty("16")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * Constructs a new builder to create a DocumentMessage.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media         the non-null document that the new message wraps
   * @param mimeType      the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param title         the title of the document that the new message wraps
   * @param pageCount     the number of pages of the document that the new message wraps
   * @param fileName      the name of the document that the new message wraps
   * @param jpegThumbnail the thumbnail of the document that the new message wraps
   * @param contextInfo   the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "NewDocumentMessageBuilder", builderMethodName = "newDocumentMessage", buildMethodName = "create")
  private static DocumentMessage builder(byte @NonNull [] media, String mimeType, String title, int pageCount, String fileName, byte[] jpegThumbnail, ContextInfo contextInfo) {
    /*
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.DOCUMENT);
    return DocumentMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().toByteArray())
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
     */
    throw new UnsupportedOperationException("Work in progress");
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
  public @NonNull MediaMessageType type() {
    return MediaMessageType.DOCUMENT;
  }
}
