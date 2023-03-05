package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;

/**
 * A model class that represents the value of a localizable parameter
 */
public sealed interface BusinessDateTimeValue extends ProtobufMessage permits BusinessDateTimeComponent, BusinessDateTimeUnixEpoch {
    /**
     * Returns the type of date
     *
     * @return a non-null type
     */
    BusinessDateTimeType dateType();
}