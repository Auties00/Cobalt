package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of parameters that can be
 * wrapped
 */
@AllArgsConstructor
@Accessors(fluent = true)
@ProtobufName("ParamOneofType")
public enum BusinessLocalizableParameterType implements ProtobufMessage {
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

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessLocalizableParameterType of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(BusinessLocalizableParameterType.NONE);
    }
}
