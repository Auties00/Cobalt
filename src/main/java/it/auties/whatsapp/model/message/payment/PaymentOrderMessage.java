package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;

import java.util.Arrays;
import java.util.Optional;


/**
 * A model class that represents a message to pay an order.
 */
@ProtobufMessage(name = "Message.PaymentOrderMessage")
public final class PaymentOrderMessage implements ContextualMessage<PaymentOrderMessage>, PaymentMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String id;
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    private final int itemCount;
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    private final Status status;
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    private final PaymentOrderSurface surface;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String message;
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String title;
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private final Jid sellerId;
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    private final String token;
    @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
    private final long amount;
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    private final String currency;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public PaymentOrderMessage(String id, byte[] thumbnail, int itemCount, Status status, PaymentOrderSurface surface, String message, String title, Jid sellerId, String token, long amount, String currency, ContextInfo contextInfo) {
        this.id = id;
        this.thumbnail = thumbnail;
        this.itemCount = itemCount;
        this.status = status;
        this.surface = surface;
        this.message = message;
        this.title = title;
        this.sellerId = sellerId;
        this.token = token;
        this.amount = amount;
        this.currency = currency;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.PAYMENT_ORDER;
    }

    public String id() {
        return id;
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public int itemCount() {
        return itemCount;
    }

    public Status status() {
        return status;
    }

    public PaymentOrderSurface surface() {
        return surface;
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Jid sellerId() {
        return sellerId;
    }

    public String token() {
        return token;
    }

    public long amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public PaymentOrderMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "PaymentOrderMessage[" +
                "id=" + id + ", " +
                "thumbnail=" + Arrays.toString(thumbnail) + ", " +
                "itemCount=" + itemCount + ", " +
                "status=" + status + ", " +
                "surface=" + surface + ", " +
                "message=" + message + ", " +
                "title=" + title + ", " +
                "sellerId=" + sellerId + ", " +
                "token=" + token + ", " +
                "amount=" + amount + ", " +
                "currency=" + currency + ", " +
                "contextInfo=" + contextInfo + ']';
    }


    @ProtobufEnum(name = "Message.OrderMessage.OrderStatus")
    public enum Status {
        /**
         * Inquiry
         */
        INQUIRY(1);

        final int index;

        Status(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }

    @ProtobufEnum(name = "Message.OrderMessage.OrderSurface")
    public enum PaymentOrderSurface {
        /**
         * Catalog
         */
        CATALOG(1);

        final int index;

        PaymentOrderSurface(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}