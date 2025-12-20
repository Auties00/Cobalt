package com.github.auties00.cobalt.node.mex.json.response;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the type of LID change that occurred.
 */
public enum LidChangeType {
    /**
     * Initial phone-to-LID migration
     */
    MIGRATION("migration"),

    /**
     * Account recovery with new LID
     */
    RECOVERY("recovery"),

    /**
     * Periodic LID rotation
     */
    ROTATION("rotation"),

    /**
     * Old LID invalidated
     */
    INVALIDATION("invalidation");

    private final String value;

    LidChangeType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * Parses a LidChangeType from its string value.
     *
     * @param value the string value
     * @return Optional containing the change type if found
     */
    public static Optional<LidChangeType> of(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
