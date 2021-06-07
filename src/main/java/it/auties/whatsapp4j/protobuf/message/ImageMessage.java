package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.model.InteractiveAnnotation;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public final class ImageMessage extends MediaMessage {
  @JsonProperty(value = "24")
  private byte[] midQualityFileEncSha256;

  @JsonProperty(value = "23")
  private byte[] midQualityFileSha256;

  @JsonProperty(value = "22")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> scanLengths;

  @JsonProperty(value = "21")
  private byte[] scansSidecar;

  @JsonProperty(value = "20")
  private int experimentGroupId;

  @JsonProperty(value = "19")
  private int firstScanLength;

  @JsonProperty(value = "18")
  private byte[] firstScanSidecar;

  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "12")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "11")
  private String directPath;

  @JsonProperty(value = "10")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "9")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8")
  private byte[] mediaKey;

  @JsonProperty(value = "7")
  private int width;

  @JsonProperty(value = "6")
  private int height;

  @JsonProperty(value = "5")
  private long fileLength;

  @JsonProperty(value = "4")
  private byte[] fileSha256;

  @JsonProperty(value = "3")
  private String caption;

  @JsonProperty(value = "2")
  private String mimetype;

  @JsonProperty(value = "1")
  private String url;

  @Override
  public MediaMessageType type() {
    return MediaMessageType.IMAGE;
  }
}
