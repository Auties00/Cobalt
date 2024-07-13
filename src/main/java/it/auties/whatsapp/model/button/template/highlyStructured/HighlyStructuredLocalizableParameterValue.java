package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * A model class that represents the value of a localizable parameter
 */
public sealed interface HighlyStructuredLocalizableParameterValue permits HighlyStructuredCurrency, HighlyStructuredDateTime {
    /**
     * Returns the type of parameter
     *
     * @return a non-null type
     */
    Type parameterType();

    @ProtobufEnum
    enum Type {
        /**
         * No parameter
         */
        NONE(0),
        /**
         * Currency parameter
         */
        CURRENCY(2),
        /**
         * Date time parameter
         */
        DATE_TIME(3);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}