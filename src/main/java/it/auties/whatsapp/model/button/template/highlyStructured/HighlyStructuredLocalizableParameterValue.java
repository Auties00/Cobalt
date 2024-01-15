package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

/**
 * A model class that represents the value of a localizable parameter
 */
public sealed interface HighlyStructuredLocalizableParameterValue extends ProtobufMessage permits HighlyStructuredCurrency, HighlyStructuredDateTime {
    /**
     * Returns the type of parameter
     *
     * @return a non-null type
     */
    Type parameterType();

    enum Type implements ProtobufEnum {
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