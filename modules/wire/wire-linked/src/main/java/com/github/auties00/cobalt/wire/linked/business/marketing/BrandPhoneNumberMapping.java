package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Resolution of one marketing brand identifier to the WhatsApp phone numbers
 * and LIDs that back it.
 *
 * <p>A merchant's marketing-messages opt-out machinery references the
 * recipients it must respect by brand identifier rather than by phone number.
 * Before honouring an opt-out the client asks the server to resolve each brand
 * identifier into the concrete WhatsApp addresses registered under it. The
 * server answers per brand: it echoes the requested
 * {@linkplain #brandId() brand identifier}, the
 * {@linkplain #phoneNumbers() phone numbers} and {@linkplain #lids() LIDs} it
 * resolved, and an {@linkplain #error() error} marker on the brands it could
 * not resolve.
 *
 * <p>This model is one such per-brand answer. The phone numbers and LIDs are
 * the raw user identifiers the server returned, not fully formed WhatsApp
 * addresses, so they are exposed as plain strings.
 */
@ProtobufMessage(name = "BrandPhoneNumberMapping")
public final class BrandPhoneNumberMapping {
    /**
     * The brand identifier this mapping was requested for, echoed back by the
     * server. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String brandId;

    /**
     * Reason the brand could not be resolved, present only on brands the
     * server failed to resolve. {@code null} when the brand resolved
     * successfully.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String error;

    /**
     * Phone-number user identifiers resolved for the brand, as raw server
     * tokens. Never {@code null}, possibly empty when none resolved.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> phoneNumbers;

    /**
     * LID user identifiers resolved for the brand, as raw server tokens. Never
     * {@code null}, possibly empty when none resolved.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> lids;

    /**
     * Constructs a new {@code BrandPhoneNumberMapping}. A {@code null}
     * {@code phoneNumbers} or {@code lids} is coerced to an empty list, and
     * {@code error} may be {@code null} when the brand resolved successfully.
     *
     * @param brandId      the echoed brand identifier, or {@code null}
     * @param error        the resolution error, or {@code null} when resolved
     * @param phoneNumbers the resolved phone-number tokens; {@code null} treated as empty
     * @param lids         the resolved LID tokens; {@code null} treated as empty
     */
    BrandPhoneNumberMapping(String brandId, String error, List<String> phoneNumbers, List<String> lids) {
        this.brandId = brandId;
        this.error = error;
        this.phoneNumbers = phoneNumbers == null ? List.of() : phoneNumbers;
        this.lids = lids == null ? List.of() : lids;
    }

    /**
     * Returns the brand identifier this mapping was requested for.
     *
     * @return the brand identifier, or empty when the server omitted it
     */
    public Optional<String> brandId() {
        return Optional.ofNullable(brandId);
    }

    /**
     * Returns the reason the brand could not be resolved.
     *
     * @return the resolution error, or empty when the brand resolved
     *         successfully
     */
    public Optional<String> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Returns the phone-number user identifiers resolved for the brand.
     *
     * @return an unmodifiable view of the resolved phone-number tokens; never
     *         {@code null}, possibly empty
     */
    public List<String> phoneNumbers() {
        return Collections.unmodifiableList(phoneNumbers);
    }

    /**
     * Returns the LID user identifiers resolved for the brand.
     *
     * @return an unmodifiable view of the resolved LID tokens; never
     *         {@code null}, possibly empty
     */
    public List<String> lids() {
        return Collections.unmodifiableList(lids);
    }
}
