package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an audio inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawAudioMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class AudioMessage extends MediaMessage {
  /**
   * The upload url of the encoded media that this object wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String url;

  /**
   * The mime type of the audio that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String mimetype;

  /**
   * The sha256 of the decoded media that this object wraps
   */
  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded media that this object wraps
   */
  @ProtobufProperty(index = 4, type = UINT64)
  private Long fileLength;

  /**
   * The unsigned length of the decoded audio in seconds
   */
  @ProtobufProperty(index = 5, type = UINT32)
  private Integer seconds;

  /**
   * Determines whether this object is a normal audio message, which might contain for example music, or a voice message
   */
  @ProtobufProperty(index = 6, type = BOOLEAN)
  private boolean voiceMessage;

  /**
   * The media key of the audio that this object wraps.
   */
  @ProtobufProperty(index = 7, type = BYTES)
  private byte[] key; 

  /**
   * The sha256 of the encoded media that this object wraps
   */
  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] fileEncSha256;

  /**
   * The direct path to the encoded media that this object wraps
   */
  @ProtobufProperty(index = 9, type = STRING)
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link AudioMessage#key()}
   */
  @ProtobufProperty(index = 10, type = INT64)
  private Long mediaKeyTimestamp;

  /**
   * The sidecar is an array of bytes obtained by concatenating every [n*64K, (n+1)*64K+16] chunk of the encoded media signed with the mac key and truncated to ten bytes.
   * This allows to play and seek the audio without the need to fully decode it decrypt as CBC allows to read data from a random offset (block-size aligned).
   * Source: <a href="https://github.com/sigalor/whatsapp-web-reveng#encryption">WhatsApp Web reverse engineered</a>
   */
  @ProtobufProperty(index = 18, type = BYTES)
  private byte[] streamingSidecar;

  /**
   * Constructs a new builder to create a AudioMessage.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media        the non-null image that the new message holds
   * @param mimeType     the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param contextInfo  the context info that the new message wraps
   * @param voiceMessage whether the new message should be considered as a voice message or as a normal audio, by default the latter is used
   *
   * @return a non-null new message
   */
  @Builder(builderClassName= "SimpleAudioMessageBuilder", builderMethodName = "newAudioMessage", buildMethodName = "create")
  private static AudioMessage builder(byte @NonNull [] media, ContextInfo contextInfo, String mimeType, boolean voiceMessage) {
    /*
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.AUDIO);
    return AudioMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().toByteArray())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .contextInfo(contextInfo)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.AUDIO.defaultMimeType()))
            .streamingSidecar(upload.sidecar())
            .voiceMessage(voiceMessage)
            .create();
     */
    throw new UnsupportedOperationException("Work in progress");
  }

  /**
   * Returns the media type of the audio that this object wraps
   *
   * @return {@link MediaMessageType#AUDIO}
   */
  @Override
  public MediaMessageType type() {
    return MediaMessageType.AUDIO;
  }
}
