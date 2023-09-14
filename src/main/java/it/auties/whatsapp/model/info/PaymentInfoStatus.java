package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the status of a payment described by a
 * {@link PaymentInfo}
 */
public enum PaymentInfoStatus implements ProtobufEnum {
    /**
     * Unknown status
     */
    UNKNOWN_STATUS(0),
    /**
     * Processing
     */
    PROCESSING(1),
    /**
     * Sent
     */
    SENT(2),
    /**
     * Need to accept
     */
    NEED_TO_ACCEPT(3),
    /**
     * Complete
     */
    COMPLETE(4),
    /**
     * Could not complete
     */
    COULD_NOT_COMPLETE(5),
    /**
     * Refunded
     */
    REFUNDED(6),
    /**
     * Expired
     */
    EXPIRED(7),
    /**
     * Rejected
     */
    REJECTED(8),
    /**
     * Cancelled
     */
    CANCELLED(9),
    /**
     * Waiting for payer
     */
    WAITING_FOR_PAYER(10),
    /**
     * Waiting
     */
    WAITING(11);

    final int index;

    PaymentInfoStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
