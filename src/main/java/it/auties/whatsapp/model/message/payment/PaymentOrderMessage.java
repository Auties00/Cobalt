package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a WhatsappMessage to pay an order.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder(builderMethodName = "newPaymentOrderMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class PaymentOrderMessage extends ContextInfo implements PaymentMessage {
  /**
   * The jid of this order
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String id;

  /**
   * The thumbnail of this order
   */
  @ProtobufProperty(index = 2, type = BYTES)
  private byte[] thumbnail;

  /**
   * The total number of items that was ordered
   */
  @ProtobufProperty(index = 3, type = UINT32)
  private int itemCount;

  /**
   * The status of this order
   */
  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = OrderMessageOrderStatus.class)
  private OrderMessageOrderStatus status;

  /**
   * The surface of this order
   */
  @ProtobufProperty(index = 5, type = MESSAGE, concreteType = OrderMessageOrderSurface.class)
  private OrderMessageOrderSurface surface;

  /**
   * The message of this order
   */
  @ProtobufProperty(index = 4, type = STRING)
  private String message;

  /**
   * The title of this order
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String title;

  /**
   * The jid of the seller associated with this order
   */
  @ProtobufProperty(index = 8, type = STRING,
          concreteType = ContactJid.class, requiresConversion = true)
  private ContactJid sellerId;

  /**
   * The token of this order
   */
  @ProtobufProperty(index = 9, type = STRING)
  private String token;

  /**
   * The amount of money being paid for this order
   */
  @ProtobufProperty(index = 10, type = UINT64)
  private long amount;

  /**
   * The currency code for {@link PaymentOrderMessage#amount}.
   * Follows the ISO-4217 Standard.
   * For a list of valid currency codes click <a href="https://en.wikipedia.org/wiki/ISO_4217">here</a>
   */
  @ProtobufProperty(index = 11, type = STRING)
  private String currency;

  /**
   * Unsupported, doesn't make much sense
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum OrderMessageOrderStatus {
    /**
     * Inquiry
     */
    INQUIRY(1);

    @Getter
    private final int index;

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
  public enum OrderMessageOrderSurface {
    /**
     * Catalog
     */
    CATALOG(1);

    @Getter
    private final int index;

    public static OrderMessageOrderSurface forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
