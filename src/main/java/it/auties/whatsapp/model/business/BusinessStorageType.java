package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of hosting that a Whatsapp business account can use
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessStorageType implements ProtobufMessage {
    /**
     * Hosted on a private server ("On-Premise")
     */
    SELF_HOSTED(0),

    /**
     * Hosted by facebook
     */
    FACEBOOK(1);

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessStorageType forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}