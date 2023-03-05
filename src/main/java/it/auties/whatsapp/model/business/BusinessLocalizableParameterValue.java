package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;

/**
 * A model class that represents the value of a localizable parameter
 */
public sealed interface BusinessLocalizableParameterValue extends ProtobufMessage permits BusinessCurrency, BusinessDateTime {
    /**
     * Returns the type of parameter
     *
     * @return a non-null type
     */
    BusinessLocalizableParameterType parameterType();
}