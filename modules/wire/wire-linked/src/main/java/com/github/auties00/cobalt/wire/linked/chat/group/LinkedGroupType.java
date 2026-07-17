package com.github.auties00.cobalt.wire.linked.chat.group;

import java.util.Optional;

/**
 * Discriminates the linkage direction or sub-group kind carried by the
 * community linked-group operations.
 *
 * <p>The value is surfaced verbatim on the wire as the {@code type} attribute
 * of the {@code <query_linked/>} child of a get-linked-group request and the
 * {@code <join_linked_group/>} child of a join request. Resolving a linked
 * group selects the linkage direction ({@link #PARENT_GROUP} or
 * {@link #SUB_GROUP}); joining a sub-group selects the sub-group kind
 * ({@link #SUB_GROUP} for an ordinary sub-group or {@link #DEFAULT_SUB_GROUP}
 * for a linked announcement group).
 */
public enum LinkedGroupType {
    /**
     * The parent community of the queried group. Wire value
     * {@code "parent_group"}.
     */
    PARENT_GROUP("parent_group"),

    /**
     * An ordinary community sub-group. Wire value {@code "sub_group"}.
     */
    SUB_GROUP("sub_group"),

    /**
     * The community's default (general announcement) sub-group. Wire value
     * {@code "default_sub_group"}.
     */
    DEFAULT_SUB_GROUP("default_sub_group");

    /**
     * The literal wire-level string carried by the {@code type} attribute.
     */
    private final String wireValue;

    /**
     * Constructs a new {@code LinkedGroupType} with the supplied wire string.
     *
     * @param wireValue the wire-level literal; never {@code null}
     */
    LinkedGroupType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-level string carried by the {@code type} attribute.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a {@code LinkedGroupType} from the wire literal.
     *
     * @param wireValue the wire literal, possibly {@code null}
     * @return an {@link Optional} containing the matching constant, or empty
     *         when the literal is {@code null} or unrecognised
     */
    public static Optional<LinkedGroupType> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
