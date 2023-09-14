package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.whatsapp.model.message.model.MediaMessageType;

/**
 * The constants of this enumerated type describe the various types of attachment that an invoice
 * can wrap
 */
public enum PaymentAttachmentType implements ProtobufEnum {
    /**
     * Image
     */
    IMAGE(0),
    /**
     * PDF
     */
    PDF(1);

    final int index;

    PaymentAttachmentType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public MediaMessageType toMediaType() {
        return switch (this) {
            case IMAGE -> MediaMessageType.IMAGE;
            case PDF -> MediaMessageType.DOCUMENT;
        };
    }
}
