package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a time
 */
@ProtobufMessage(name = "Message.HighlyStructuredMessage.HSMLocalizableParameter.HSMDateTime")
public final class HighlyStructuredDateTime implements HighlyStructuredLocalizableParameterValue {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final HighlyStructuredDateTimeComponent dateComponent;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final HighlyStructuredDateTimeUnixEpoch dateUnixEpoch;

    HighlyStructuredDateTime(HighlyStructuredDateTimeComponent dateComponent, HighlyStructuredDateTimeUnixEpoch dateUnixEpoch) {
        this.dateComponent = dateComponent;
        this.dateUnixEpoch = dateUnixEpoch;
    }

    /**
     * Constructs a new date time using a component
     *
     * @param dateComponent the non-null component
     * @return a non-null date time
     */
    public static HighlyStructuredDateTime of(HighlyStructuredDateTimeValue dateComponent) {
        return switch (dateComponent) {
            case HighlyStructuredDateTimeComponent highlyStructuredDateTimeComponent ->
                    new HighlyStructuredDateTime(highlyStructuredDateTimeComponent, null);
            case HighlyStructuredDateTimeUnixEpoch highlyStructuredDateTimeUnixEpoch ->
                    new HighlyStructuredDateTime(null, highlyStructuredDateTimeUnixEpoch);
            case null -> new HighlyStructuredDateTime(null, null);
        };
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
        return Optional.ofNullable(dateComponent != null ? dateComponent : dateUnixEpoch);
    }

    public Optional<HighlyStructuredDateTimeComponent> dateComponent() {
        return Optional.ofNullable(dateComponent);
    }

    public Optional<HighlyStructuredDateTimeUnixEpoch> dateUnixEpoch() {
        return Optional.ofNullable(dateUnixEpoch);
    }

    @Override
    public Type parameterType() {
        return Type.DATE_TIME;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredDateTime that
                && Objects.equals(dateComponent, that.dateComponent)
                && Objects.equals(dateUnixEpoch, that.dateUnixEpoch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dateComponent, dateUnixEpoch);
    }

    @Override
    public String toString() {
        return "HighlyStructuredDateTime[" +
                "dateComponent=" + dateComponent + ", " +
                "dateUnixEpoch=" + dateUnixEpoch + ']';
    }
}