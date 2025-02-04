package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a time
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime")
public record HighlyStructuredDateTime(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredDateTimeComponent> dateComponent,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredDateTimeUnixEpoch> dateUnixEpoch
) implements HighlyStructuredLocalizableParameterValue {
    /**
     * Constructs a new date time using a component
     *
     * @param dateComponent the non-null component
     * @return a non-null date time
     */
    public static HighlyStructuredDateTime of(HighlyStructuredDateTimeValue dateComponent) {
        if (dateComponent instanceof HighlyStructuredDateTimeComponent highlyStructuredDateTimeComponent) {
            return new HighlyStructuredDateTime(Optional.of(highlyStructuredDateTimeComponent), Optional.empty());
        } else if (dateComponent instanceof HighlyStructuredDateTimeUnixEpoch highlyStructuredDateTimeUnixEpoch) {
            return new HighlyStructuredDateTime(Optional.empty(), Optional.of(highlyStructuredDateTimeUnixEpoch));
        } else {
            return new HighlyStructuredDateTime(Optional.empty(), Optional.empty());
        }
    }

    /**
     * Returns the type of date of this component
     *
     * @return a non-null date type
     */
    public HighlyStructuredDateTimeValue.Type dateType() {
        return date().map(HighlyStructuredDateTimeValue::dateType)
                .orElse(HighlyStructuredDateTimeValue.Type.NONE);
    }

    /**
     * Returns the date of this component
     *
     * @return a non-null date type
     */
    public Optional<? extends HighlyStructuredDateTimeValue> date() {
        return dateComponent.isPresent() ? dateComponent : dateUnixEpoch;
    }

    @Override
    public Type parameterType() {
        return Type.DATE_TIME;
    }
}