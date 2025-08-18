package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * An enumeration of possible Availabilities.
 */
@ProtobufEnum
public enum BusinessItemAvailability {
    /**
     * Indicates an unknown availability.
     */
    UNKNOWN,
    /**
     * Indicates that the item is in stock.
     */
    IN_STOCK,
    /**
     * Indicates that the item is out of stock.
     */
    OUT_OF_STOCK
}
