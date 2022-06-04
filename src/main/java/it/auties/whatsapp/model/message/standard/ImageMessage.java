package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.InteractiveAnnotation;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;
import static it.auties.whatsapp.model.message.model.MediaMessageType.IMAGE;
import static it.auties.whatsapp.util.Medias.Format.JPG;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds an image inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawImageMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class ImageMessage extends MediaMessage {
  /**
   * The upload url of the encoded image that this object wraps
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String url;

  /**
   * The mime type of the image that this object wraps.
   * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String mimetype;

  /**
   * The caption of this message
   */
  @ProtobufProperty(index = 3, type = STRING)
  private String caption;

  /**
   * The sha256 of the decoded image that this object wraps
   */
  @ProtobufProperty(index = 4, type = BYTES)
  private byte[] fileSha256;

  /**
   * The unsigned size of the decoded image that this object wraps
   */
  @ProtobufProperty(index = 5, type = UINT64)
  private Long fileLength;

  /**
   * The unsigned height of the decoded image that this object wraps
   */
  @ProtobufProperty(index = 6, type = UINT32)
  private Integer height;

  /**
   * The unsigned width of the decoded image that this object wraps
   */
  @ProtobufProperty(index = 7, type = UINT32)
  private Integer width;

  /**
   * The media key of the image that this object wraps
   */
  @ProtobufProperty(index = 8, type = BYTES)
  private byte[] key; 

  /**
   * The sha256 of the encoded image that this object wraps
   */
  @ProtobufProperty(index = 9, type = BYTES)
  private byte[] fileEncSha256;

  /**
   * Interactive annotations
   */
  @ProtobufProperty(index = 10, type = MESSAGE,
          concreteType = InteractiveAnnotation.class, repeated = true)
  private List<InteractiveAnnotation> interactiveAnnotations;
  
  /**
   * The direct path to the encoded image that this object wraps
   */
  @ProtobufProperty(index = 11, type = STRING)
  private String directPath;

  /**
   * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link ImageMessage#key()}
   */
  @ProtobufProperty(index = 12, type = UINT64)
  private Long mediaKeyTimestamp;
  
  /**
   * The thumbnail for this image message encoded as jpeg in an array of bytes
   */
  @ProtobufProperty(index = 16, type = BYTES)
  private byte[] thumbnail;

  /**
   * The sidecar for the first sidecar
   */
  @ProtobufProperty(index = 18, type = BYTES)
  private byte[] firstScanSidecar;

  /**
   * The length of the first scan
   */
  @ProtobufProperty(index = 19, type = UINT32)
  private Integer firstScanLength;

  /**
   * Experiment Group Id
   */
  @ProtobufProperty(index = 20, type = UINT32)
  private Integer experimentGroupId;
  
  /**
   * The sidecar for the scans of the decoded image
   */
  @ProtobufProperty(index = 21, type = BYTES)
  private byte[] scansSidecar;

  /**
   * The length of each scan of the decoded image
   */
  @ProtobufProperty(index = 22, type = UINT32, repeated = true)
  private List<Integer> scanLengths;

  /**
   * The sha256 of the decoded image in medium quality
   */
  @ProtobufProperty(index = 23, type = BYTES)
  private byte[] midQualityFileSha256;

  /**
   * The sha256 of the encoded image in medium quality
   */
  @ProtobufProperty(index = 24, type = BYTES)
  private byte[] midQualityFileEncSha256;

  /**
   * Constructs a new builder to create a ImageMessage.
   * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param storeId     the id of the store where this message will be stored
   * @param media       the non-null image that the new message wraps
   * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
   * @param caption     the caption of the new message
   * @param thumbnail   the thumbnail of the document that the new message wraps
   * @param contextInfo the context info that the new message wraps
   *
   * @return a non-null new message
   */
  @Builder(builderClassName = "SimpleImageBuilder", builderMethodName = "newImageMessage", buildMethodName = "create")
  private static ImageMessage simpleBuilder(int storeId, byte @NonNull [] media, String mimeType, String caption, byte[] thumbnail, ContextInfo contextInfo) {
    var store = WhatsappStore.findStoreById(storeId)
            .orElseThrow(() -> new NoSuchElementException("Cannot create image message, invalid store id: %s".formatted(storeId)));
    var dimensions = Medias.getDimensions(media, false);
    var upload = Medias.upload(media, IMAGE, store);
    return ImageMessage.newRawImageMessage()
            .storeId(storeId)
            .fileSha256(upload.fileSha256())
            .fileEncSha256(upload.fileEncSha256())
            .key(upload.mediaKey())
            .mediaKeyTimestamp(Clock.now())
            .url(upload.url())
            .directPath(upload.directPath())
            .fileLength(upload.fileLength())
            .mimetype(requireNonNullElse(mimeType, IMAGE.defaultMimeType()))
            .caption(caption)
            .width(dimensions.width())
            .height(dimensions.height())
            .thumbnail(thumbnail != null ? thumbnail : Medias.getThumbnail(media, JPG))
            .contextInfo(contextInfo)
            .create();
  }

  /**
   * Returns the media type of the image that this object wraps
   *
   * @return {@link MediaMessageType#IMAGE}
   */
  @Override
  public MediaMessageType type() {
    return MediaMessageType.IMAGE;
  }

  public static abstract class ImageMessageBuilder<C extends ImageMessage, B extends ImageMessageBuilder<C, B>> extends MediaMessageBuilder<C, B> {
    public B interactiveAnnotations(List<InteractiveAnnotation> interactiveAnnotations) {
      if(this.interactiveAnnotations == null) this.interactiveAnnotations = new ArrayList<>();
      this.interactiveAnnotations.addAll(interactiveAnnotations);
      return self();
    }

    public B scanLengths(List<Integer> scanLengths){
      if(this.scanLengths == null) this.scanLengths = new ArrayList<>();
      this.scanLengths.addAll(scanLengths);
      return self();
    }
  }
}
