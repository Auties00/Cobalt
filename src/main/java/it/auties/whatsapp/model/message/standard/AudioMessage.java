package it.auties.whatsapp.model.message.standard;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.FIXED32;
import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;
import static it.auties.whatsapp.model.message.model.MediaMessageType.AUDIO;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message holding an audio inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
public final class AudioMessage extends MediaMessage {
  /**
   * The upload url of the encoded media that this object wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String mediaUrl;

  /**
   * The mime type of the audio that this object wraps. Most of the seconds this is
   * {@link MediaMessageType#defaultMimeType()}
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String mimetype;

  /**
   * The sha256 of the decoded media that this object wraps
   */
  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] mediaSha256;

  /**
   * The unsigned size of the decoded media that this object wraps
   */
  @ProtobufProperty(index = 4, type = UINT64)
  private long mediaSize;

  /**
   * The unsigned codeLength of the decoded audio in endTimeStamp
   */
  @ProtobufProperty(index = 5, type = UINT32)
  private int duration;

  /**
   * Determines whether this object is a normal audio message, which might contain for example
   * music, or a voice message
   */
  @ProtobufProperty(index = 6, type = BOOL)
  private boolean voiceMessage;

  /**
   * The media key of the audio that this object wraps.
   */
  @ProtobufProperty(index = 7, type = BYTES)
  private byte[] mediaKey;

  /**
   * The sha256 of the encoded media that this object wraps
   */
  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] mediaEncryptedSha256;

  /**
   * The direct path to the encoded media that this object wraps
   */
  @ProtobufProperty(index = 9, type = STRING)
  private String mediaDirectPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for
   * {@link AudioMessage#mediaKey()}
   */
  @ProtobufProperty(index = 10, type = INT64)
  private long mediaKeyTimestamp;

  /**
   * The sidecar is an array of bytes obtained by concatenating every [n*64K, (n+1)*64K+16] chunk of
   * the encoded media signed with the mac key and truncated to ten bytes. This allows to play and
   * seek the audio without the need to fully decode it decrypt as CBC allows to read data from a
   * random offset (block-size aligned). Source: <a
   * href="https://github.com/sigalor/whatsapp-web-reveng#encryption">WhatsApp Web reverse
   * engineered</a>
   */
  @ProtobufProperty(index = 18, type = BYTES)
  private byte[] streamingSidecar;

  @ProtobufProperty(index = 19, name = "waveform", type = BYTES)
  private byte[] waveform;

  @ProtobufProperty(index = 20, name = "backgroundArgb", type = FIXED32)
  private Integer backgroundArgb;

  /**
   * Constructs a new builder to create a AudioMessage. The result can be later sent using
   * {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param media        the non-null image that the new message holds
   * @param mimeType     the mime type of the new message, by default
   *                     {@link MediaMessageType#defaultMimeType()}
   * @param contextInfo  the context info that the new message wraps
   * @param voiceMessage whether the new message should be considered as a voice message or as a
   *                     normal audio, by default the latter is used
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleAudioMessageBuilder", builderMethodName = "simpleBuilder")
  private static AudioMessage customBuilder(byte[] media, ContextInfo contextInfo,
      String mimeType, boolean voiceMessage) {
    var duration = Medias.getDuration(media, true);
    return AudioMessage.builder().decodedMedia(media).mediaKeyTimestamp(Clock.nowInSeconds())
        .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::new))
        .duration(duration).mimetype(mimeType).voiceMessage(voiceMessage).build();
  }

  /**
   * Returns the media type of the audio that this object wraps
   *
   * @return {@link MediaMessageType#AUDIO}
   */
  @Override
  public MediaMessageType mediaType() {
    return AUDIO;
  }
}