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
 * A model class that represents a WhatsappMessage sent by a contact and that holds an audio inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(buildMethodName = "create")
@Accessors(fluent = true)
public final class AudioMessage extends MediaMessage {
  /**
   * The sidecar is an array of bytes obtained by concatenating every [n*64K, (n+1)*64K+16] chunk of the encoded media signed with the mac key and truncated to ten bytes.
   * This allows to play and seek the audio without the need to fully decode it decrypt as CBC allows to read data from a random offset (block-size aligned).
   * Source: <a href="https://github.com/sigalor/whatsapp-web-reveng#encryption">WhatsApp Web reverse engineered</a>
   */
  @JsonProperty(value = "18")
  private byte[] streamingSidecar;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link AudioMessage#mediaKey()}
   */
  @JsonProperty(value = "10")
  private long mediaKeyTimestamp;

  /**
   * The direct path to the encoded media that this object wraps
   */
  @JsonProperty(value = "9")
  private String directPath;

  /**
   * The sha256 of the encoded media that this object wraps
   */
  @JsonProperty(value = "8")
  private byte[] fileEncSha256;

  /**
   * The media key of the audio that this object wraps.
   * This key is used to decrypt the encoded media by {@link CypherUtils#mediaDecrypt(MediaMessage)}
   */
  @JsonProperty(value = "7")
  private byte[] mediaKey;

  /**
   * Determines whether this object is a normal audio message, which might contain for example music, or a voice message
   */
  @JsonProperty(value = "6")
  private boolean voiceMessage;

  /**
   * The unsigned length of the decoded audio in seconds
   */
  @JsonProperty(value = "5")
  private int seconds;

  /**
   * The unsigned size of the decoded media that this object wraps
   */
  @JsonProperty(value = "4")
  private long fileLength;

  /**
   * The sha256 of the decoded media that this object wraps
   */
  @JsonProperty(value = "3")
  private byte[] fileSha256;

  /**
   * The mime type of the audio that this object wraps.
   * Most of the times this is {@link MediaMessageType#defaultMimeType()}
   */
  @JsonProperty(value = "2")
  private String mimetype;

  /**
   * The upload url of the encoded media that this object wraps
   */
  @JsonProperty(value = "1")
  private String url;

  /**
   * Constructs a new builder to create a AudioMessage.
   * The result can be later sent using {@link WhatsappAPI#sendMessage(it.auties.whatsapp4j.protobuf.info.MessageInfo)}
   *
   * @param media         the non null image that the new message holds
   * @param mimeType      the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param contextInfo   the context info that the new message wraps
   * @param voiceMessage  whether the new message should be considered as a voice message or as a normal audio, by default the latter is used
   *
   * @return a non null new message
   */
  @Builder(builderClassName= "NewAudioMessageBuilder", builderMethodName = "newAudioMessage", buildMethodName = "create")
  private static AudioMessage builder(byte @NotNull [] media, ContextInfo contextInfo, String mimeType, boolean voiceMessage) {
    var upload = CypherUtils.mediaEncrypt(media, MediaMessageType.AUDIO);
    return AudioMessage.builder()
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .mediaKey(upload.mediaKey().data())
            .mediaKeyTimestamp(ZonedDateTime.now().toEpochSecond())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(media.length)
            .contextInfo(contextInfo)
            .mimetype(Optional.ofNullable(mimeType).orElse(MediaMessageType.AUDIO.defaultMimeType()))
            .streamingSidecar(upload.sidecar())
            .voiceMessage(voiceMessage)
            .create();
  }

  private static AudioMessageBuilder<?, ?> builder() {
    return new AudioMessageBuilderImpl();
  }

  /**
   * Returns the media type of the audio that this object wraps
   *
   * @return {@link MediaMessageType#AUDIO}
   */
  @Override
  public @NotNull MediaMessageType type() {
    return MediaMessageType.AUDIO;
  }
}
