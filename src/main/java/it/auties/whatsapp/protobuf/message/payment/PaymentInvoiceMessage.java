package it.auties.whatsapp.protobuf.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.message.model.MediaMessage;
import it.auties.whatsapp.protobuf.message.model.MediaMessageType;
import it.auties.whatsapp.protobuf.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage to notify the invoice about a successful payment.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class PaymentInvoiceMessage extends MediaMessage implements PaymentMessage {
  /**
   * The note of this invoice
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String note;

  /**
   * The token of this invoice
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String token;

  /**
   * The type of attachment that this invoice provides
   */
  @JsonProperty("3")
  @JsonPropertyDescription("type")
  private AttachmentType type;

  /**
   * The mime type of the attachment that this invoice provides
   */
  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String mimeType;

  /**
   * The media key of the attachment that this invoice provides
   */
  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  /**
   * The media key timestamp of the attachment that this invoice provides
   */
  @JsonProperty("6")
  @JsonPropertyDescription("uint64")
  private long mediaKeyTimestamp;

  /**
   * The sha256 of the attachment that this invoice provides
   */
  @JsonProperty("7")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  /**
   * The sha256 of the encrypted attachment that this invoice provides
   */
  @JsonProperty("8")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  /**
   * The direct path to the attachment that this invoice provides
   */
  @JsonProperty("9")
  @JsonPropertyDescription("string")
  private String directPath;

  /**
   * The thumbnail of the attachment that this invoice provides
   */
  @JsonProperty("10")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  /**
   * This method is not supported
   *
   * @return an exception
   * @throws UnsupportedOperationException always
   */
  @Override
  public @NonNull String url() {
    throw new UnsupportedOperationException("Invoices don't provide an upload url");
  }

  /**
   * This method is not supported
   *
   * @return an exception
   * @throws UnsupportedOperationException always
   */
  @Override
  public long fileLength() {
    throw new UnsupportedOperationException("Invoices don't provide a file size");
  }

  /**
   * Returns the media type of the media that this object wraps
   *
   * @return a non-null {@link MediaMessageType}
   */
  @Override
  public @NonNull MediaMessageType type() {
    return type == AttachmentType.IMAGE ? MediaMessageType.IMAGE
            : MediaMessageType.DOCUMENT;
  }

  /**
   * The constants of this enumerated type describe the various types of attachment that an invoice can wrap
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum AttachmentType {
    /**
     * Image
     */
    IMAGE(0),

    /**
     * PDF
     */
    PDF(1);

    @Getter
    private final int index;

    @JsonCreator
    public static AttachmentType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
