package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.PaymentMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;


/**
 * A model class that represents a message to pay an order.
 */
@ProtobufMessageName("Message.PaymentOrderMessage")
public record PaymentOrderMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int itemCount,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        Status status,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        @NonNull
        PaymentOrderSurface surface,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> message,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> title,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        @NonNull
        Jid sellerId,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        @NonNull
        String token,
        @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
        long amount,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        @NonNull
        String currency,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, PaymentMessage {
        @Override
        public MessageType type() {
                return MessageType.PAYMENT_ORDER;
        }

        @ProtobufMessageName("Message.OrderMessage.OrderStatus")
        public enum Status implements ProtobufEnum {
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

        @ProtobufMessageName("Message.OrderMessage.OrderSurface")
        public enum PaymentOrderSurface implements ProtobufEnum {
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