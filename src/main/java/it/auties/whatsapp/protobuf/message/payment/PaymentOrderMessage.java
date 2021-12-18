package it.auties.whatsapp.protobuf.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.PaymentMessage;
import it.auties.whatsapp.util.Unsupported;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage to pay an order.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public final class PaymentOrderMessage extends ContextInfo implements PaymentMessage {
  /**
   * The id of this order
   */
  @JsonProperty(value = "1")
  private String id;

  /**
   * The thumbnail of this order
   */
  @JsonProperty(value = "2")
  private byte[] thumbnail;

  /**
   * The total number of items that was ordered
   */
  @JsonProperty(value = "3")
  private int itemCount;

  /**
   * The status of this order
   */
  @JsonProperty(value = "4")
  private OrderMessageOrderStatus status;

  /**
   * The surface of this order
   */
  @JsonProperty(value = "5")
  private OrderMessageOrderSurface surface;

  /**
   * The message of this order
   */
  @JsonProperty(value = "6")
  private String message;

  /**
   * The title of this order
   */
  @JsonProperty(value = "7")
  private String title;

  /**
   * The id of the seller associated with this order
   */
  @JsonProperty(value = "8")
  private ContactJid sellerId;

  /**
   * The token of this order
   */
  @JsonProperty(value = "9")
  private String token;

  /**
   * The amount of money being paid for this order
   */
  @JsonProperty(value = "10")
  private long amount;

  /**
   * The currency code for {@link PaymentOrderMessage#amount}.
   * Follows the ISO-4217 Standard.
   * For a list of valid currency codes click <a href="https://en.wikipedia.org/wiki/ISO_4217">here</a>
   */
  @JsonProperty(value = "11")
  private String currency;

  /**
   * Unsupported, doesn't make much sense
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  @Unsupported
  public enum OrderMessageOrderStatus {
    /**
     * Inquiry
     */
    INQUIRY(1);

    private final @Getter int index;

    @JsonCreator
    public static OrderMessageOrderStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  /**
   * Unsupported, doesn't make much sense
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  @Unsupported
  public enum OrderMessageOrderSurface {
    /**
     * Catalog
     */
    CATALOG(1);

    private final @Getter int index;

    @JsonCreator
    public static OrderMessageOrderSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
