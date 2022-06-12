package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessLocalizableParameter implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String defaultValue;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = BusinessCurrency.class)
    private BusinessCurrency currency;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = BusinessDateTime.class)
    private BusinessDateTime dateTime;

    public ParamType paramType() {
        if (currency != null)
            return ParamType.CURRENCY;
        if (dateTime != null)
            return ParamType.DATE_TIME;
        return ParamType.UNKNOWN;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ParamType implements ProtobufMessage {
        UNKNOWN(0),
        CURRENCY(2),
        DATE_TIME(3);

        @Getter
        private final int index;

        @JsonCreator
        public static ParamType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(ParamType.UNKNOWN);
        }
    }
}
