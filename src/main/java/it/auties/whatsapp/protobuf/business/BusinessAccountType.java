package it.auties.whatsapp.protobuf.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of business accounts
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessAccountType {
    /**
     * Enterprise
     */
    ENTERPRISE(0),

    /**
     * Page
     */
    PAGE(1);

    private final @Getter int index;

    @JsonCreator
    public static BusinessAccountType forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}