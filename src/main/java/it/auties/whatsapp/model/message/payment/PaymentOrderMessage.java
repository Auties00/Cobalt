package it.auties.whatsapp.model.message.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message to pay an order.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@Accessors(fluent = true)
@ProtobufName("OrderMessage")
public final class PaymentOrderMessage extends ContextualMessage implements PaymentMessage {
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
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = PaymentOrderMessage.OrderMessageOrderStatus.class)
    private OrderMessageOrderStatus status;

    /**
     * The surface of this order
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = PaymentOrderMessage.OrderSurface.class)
    private OrderSurface surface;

    /**
     * The message of this order
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String message;

    /**
     * The title of this order
     */
    @ProtobufProperty(index = 7, type = STRING)
    private String title;

    /**
     * The jid of the seller associated with this order
     */
    @ProtobufProperty(index = 8, type = STRING, implementation = ContactJid.class)
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
     * The currency countryCode for {@link PaymentOrderMessage#amount}. Follows the ISO-4217 Standard. For a
     * list of valid currency codes click <a href="https://en.wikipedia.org/wiki/ISO_4217">here</a>
     */
    @ProtobufProperty(index = 11, type = STRING)
    private String currency;

    @Override
    public MessageType type() {
        return MessageType.PAYMENT_ORDER;
    }

    /**
     * Unsupported, doesn't make much sense
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("OrderStatus")
    public enum OrderMessageOrderStatus {
        /**
         * Inquiry
         */
        INQUIRY(1);
        
        @Getter
        private final int index;

        @JsonCreator
        public static OrderMessageOrderStatus of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }

    /**
     * Unsupported, doesn't make much sense
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum OrderSurface {
        /**
         * Catalog
         */
        CATALOG(1);
        
        @Getter
        private final int index;

        @JsonCreator
        public static OrderSurface of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}