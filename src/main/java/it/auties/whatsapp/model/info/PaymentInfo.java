package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.payment.PaymentMoney;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that holds the information related to a payment.
 */
public record PaymentInfo(
        @Deprecated
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        PaymentInfoCurrency currencyDeprecated,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long amount1000,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @NonNull
        ContactJid receiverJid,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        PaymentInfoStatus status,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
        long transactionTimestampSeconds,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        @NonNull
        MessageKey requestMessageKey,
        @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
        long expiryTimestampSeconds,
        @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
        boolean futureProofed,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        @NonNull
        String currency,
        @ProtobufProperty(index = 10, type = ProtobufType.OBJECT)
        @NonNull
        PaymentInfoTxnStatus transactionStatus,
        @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
        boolean useNoviFormat,
        @ProtobufProperty(index = 12, type = ProtobufType.OBJECT)
        @NonNull
        PaymentMoney primaryAmount,
        @ProtobufProperty(index = 13, type = ProtobufType.OBJECT)
        @NonNull
        PaymentMoney exchangeAmount
) implements Info, ProtobufMessage {
    /**
     * Returns when the transaction happened
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> transactionTimestamp() {
        return Clock.parseSeconds(transactionTimestampSeconds);
    }

    /**
     * Returns when the transaction expires
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> expiryTimestamp() {
        return Clock.parseSeconds(expiryTimestampSeconds);
    }
}