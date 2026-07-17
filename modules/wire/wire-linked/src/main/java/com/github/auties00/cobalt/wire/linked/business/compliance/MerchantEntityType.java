package com.github.auties00.cobalt.wire.linked.business.compliance;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Represents the legal-entity category a WhatsApp Business merchant declares in
 * its compliance disclosure.
 *
 * <p>When a merchant publishes the compliance information that backs the
 * "Edit business info" form, the entity type names the legal structure of the
 * registered business. WhatsApp restricts the value to a fixed set, mirrored by
 * the constants on this enum and emitted on the wire as the matching
 * upper-snake-case identifier (for example {@code "SOLE_PROPRIETORSHIP"}). The
 * {@link #OTHER} bucket is paired with a free-form description carried
 * separately on {@code MerchantComplianceEdit}.
 *
 * <p>The {@link #data()} method returns the protocol-level string identifier
 * written into the {@code entity_type} field of the set-compliance mutation;
 * {@link #of(String)} performs the inverse lookup.
 */
@ProtobufEnum
public enum MerchantEntityType {
    /**
     * A business owned and run by one individual, with no legal distinction
     * between the owner and the business.
     */
    SOLE_PROPRIETORSHIP(0, "SOLE_PROPRIETORSHIP"),

    /**
     * A business owned by two or more partners who share its profits and
     * liabilities.
     */
    PARTNERSHIP(1, "PARTNERSHIP"),

    /**
     * A privately held limited company whose shares are not offered to the
     * general public.
     */
    PRIVATE_COMPANY(2, "PRIVATE_COMPANY"),

    /**
     * A public limited company whose shares may be offered to the general
     * public.
     */
    PUBLIC_COMPANY(3, "PUBLIC_COMPANY"),

    /**
     * A limited liability partnership, combining partnership flexibility with
     * limited liability for its partners.
     */
    LIMITED_LIABILITY_PARTNERSHIP(4, "LIMITED_LIABILITY_PARTNERSHIP"),

    /**
     * Any legal-entity category not covered by the other constants. Callers
     * describe the actual structure through the free-form custom entity-type
     * field on the compliance edit.
     */
    OTHER(5, "OTHER");

    /**
     * The protobuf-assigned numeric index for this entity type.
     */
    final int index;

    /**
     * The protocol-level string identifier emitted in the {@code entity_type}
     * field on the wire.
     */
    private final String data;

    /**
     * Constructs a {@code MerchantEntityType} with the specified protobuf index
     * and protocol identifier.
     *
     * @param index the protobuf enum index
     * @param data  the protocol-level string identifier
     */
    MerchantEntityType(@ProtobufEnumIndex int index, String data) {
        this.index = index;
        this.data = data;
    }

    /**
     * Returns the {@code MerchantEntityType} matching the given protocol-level
     * string identifier.
     *
     * @param input the wire-level identifier such as {@code "SOLE_PROPRIETORSHIP"}
     * @return the matching entity-type constant
     * @throws NoSuchElementException if no constant matches the input
     */
    public static MerchantEntityType of(String input) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), input))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find MerchantEntityType for %s".formatted(input)));
    }

    /**
     * Returns the protobuf-assigned numeric index for this entity type.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the protocol-level string identifier for this entity type.
     *
     * @return the wire-level identifier
     */
    public String data() {
        return data;
    }
}
