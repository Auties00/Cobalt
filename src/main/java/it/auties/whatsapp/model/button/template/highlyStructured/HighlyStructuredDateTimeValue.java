package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

/**
 * A model class that represents the value of a localizable parameter
 */
public sealed interface HighlyStructuredDateTimeValue extends ProtobufMessage permits HighlyStructuredDateTimeComponent, HighlyStructuredDateTimeUnixEpoch {
    /**
     * Returns the type of date
     *
     * @return a non-null type
     */
    Type dateType();


    /**
     * The constants of this enumerated type describe the various type of date types that a date time can wrap
     */
    enum Type implements ProtobufEnum {
        /**
         * No date
         */
        NONE(0),
        /**
         * Component date
         */
        COMPONENT(1),
        /**
         * Unix epoch date
         */
        UNIX_EPOCH(2);


        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}