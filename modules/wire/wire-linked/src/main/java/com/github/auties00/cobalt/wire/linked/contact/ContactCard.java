package com.github.auties00.cobalt.wire.linked.contact;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Telephone;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A contact vCard (Virtual Contact File) attached to a WhatsApp contact
 * message.
 *
 * <p>When a user shares a contact through WhatsApp, the contact information
 * is encoded as a vCard string on the wire. This sealed interface exposes
 * two representations of that data:
 * <ul>
 *   <li>{@link Parsed}, which decomposes the vCard into structured fields
 *       (version, name, phone numbers grouped by type, business name); and</li>
 *   <li>{@link Raw}, which preserves the original vCard string verbatim and
 *       is used when parsing fails, when the vCard uses unsupported
 *       constructs, or when the vCard library is unavailable at runtime.</li>
 * </ul>
 *
 * <p>The vCard payload follows the standard RFC 6350 format with two
 * WhatsApp-specific extensions that the parser understands:
 * <ul>
 *   <li>the {@code X-WA-BIZ-NAME} extended property stores the business
 *       display name for contacts belonging to a WhatsApp Business account;
 *       and</li>
 *   <li>the {@code WAID} parameter on each {@code TEL} entry stores the
 *       phone number in the WhatsApp JID form (the phone number without the
 *       leading {@code +}), allowing the consumer to address the contact
 *       directly on WhatsApp without having to re-parse the display form.</li>
 * </ul>
 *
 * <p>Instances are serialised to and deserialised from the protobuf wire
 * format as plain {@code String} values through the {@link ProtobufSerializer}
 * and {@link ProtobufDeserializer} annotations.
 *
 * @see com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage
 * @see com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage
 */
public sealed interface ContactCard {
    /**
     * The name of the vCard extended property used by WhatsApp to carry the
     * display name of a business contact.
     */
    String BUSINESS_NAME_VCARD_PROPERTY = "X-WA-BIZ-NAME";

    /**
     * The name of the vCard parameter used by WhatsApp on {@code TEL} entries
     * to carry the phone number's WhatsApp user identifier (the JID).
     */
    String PHONE_NUMBER_VCARD_PROPERTY = "WAID";

    /**
     * The default vCard telephone type assigned to phone numbers when no
     * explicit type is specified. Corresponds to the standard {@code CELL}
     * type.
     */
    String DEFAULT_NUMBER_VCARD_TYPE = "CELL";

    /**
     * Parses a vCard string into a structured {@link Parsed} representation.
     *
     * <p>The parser extracts the formatted name, the telephone entries that
     * carry both a recognised type and a {@code WAID} parameter, and the
     * optional {@code X-WA-BIZ-NAME} extended property. If parsing fails for
     * any reason (malformed vCard, missing dependency, unexpected exception),
     * a {@link Raw} representation preserving the original string is returned
     * instead so that the original payload can still be forwarded.
     *
     * @param vcard the vCard string to parse, or {@code null}
     * @return a {@code Parsed} instance if parsing succeeds, a {@code Raw}
     *         instance if parsing fails, or {@code null} when {@code vcard}
     *         is {@code null}
     */
    @ProtobufDeserializer
    static ContactCard of(String vcard) {
        try {
            if(vcard == null) {
                return null;
            }

            var parsed = Ezvcard.parse(vcard).first();
            var version = Objects.requireNonNullElse(parsed.getVersion().getVersion(), VCardVersion.V3_0.getVersion());
            var name = parsed.getFormattedName().getValue();
            var phoneNumbers = parsed.getTelephoneNumbers()
                    .stream()
                    .filter(ContactCard::isValidPhoneNumber)
                    .collect(Collectors.toUnmodifiableMap(ContactCard::getPhoneType, ContactCard::getPhoneValue, ContactCard::joinPhoneNumbers));
            var businessName = parsed.getExtendedProperty(BUSINESS_NAME_VCARD_PROPERTY);
            return new Parsed(version, name, phoneNumbers, businessName != null ? businessName.getValue() : null);
        } catch (Throwable ignored) {
            return new Raw(vcard);
        }
    }

    /**
     * Creates a new {@link Parsed} contact card with the given name and phone
     * number, using the default vCard version ({@code 3.0}) and the default
     * phone type ({@code CELL}).
     *
     * @param name        the display name of the contact, or {@code null}
     * @param phoneNumber the non-{@code null} phone number JID of the contact
     * @return a new {@code Parsed} contact card
     */
    static ContactCard of(String name, Jid phoneNumber) {
        return of(name, phoneNumber, null);
    }

    /**
     * Creates a new {@link Parsed} contact card with the given name, phone
     * number and optional business name, using the default vCard version
     * ({@code 3.0}) and the default phone type ({@code CELL}).
     *
     * @param name         the display name of the contact, or {@code null}
     * @param phoneNumber  the non-{@code null} phone number JID of the
     *                     contact
     * @param businessName the business display name to attach as
     *                     {@code X-WA-BIZ-NAME}, or {@code null} for regular
     *                     contacts
     * @return a new {@code Parsed} contact card
     */
    static ContactCard of(String name, Jid phoneNumber, String businessName) {
        return new Parsed(
                VCardVersion.V3_0.getVersion(),
                name,
                Map.of(DEFAULT_NUMBER_VCARD_TYPE, List.of(Objects.requireNonNull(phoneNumber))),
                businessName
        );
    }

    /**
     * Returns whether the given telephone entry has both a recognised type
     * and a {@code WAID} parameter identifying the WhatsApp user.
     *
     * @param entry the telephone property to validate
     * @return {@code true} if the entry has a type and a {@code WAID}
     *         parameter
     */
    private static boolean isValidPhoneNumber(Telephone entry) {
        return getPhoneType(entry) != null && entry.getParameter(PHONE_NUMBER_VCARD_PROPERTY) != null;
    }

    /**
     * Extracts the telephone type from a vCard telephone entry.
     *
     * @param entry the telephone property
     * @return the type string (for example {@code "CELL"} or {@code "HOME"}),
     *         or {@code null} if no type is set
     */
    private static String getPhoneType(Telephone entry) {
        return entry.getParameters().getType();
    }

    /**
     * Extracts the WhatsApp JID from a vCard telephone entry's {@code WAID}
     * parameter.
     *
     * @param entry the telephone property
     * @return a singleton list containing the JID derived from the
     *         {@code WAID} value
     */
    private static List<Jid> getPhoneValue(Telephone entry) {
        return List.of(Jid.of(entry.getParameter(PHONE_NUMBER_VCARD_PROPERTY)));
    }

    /**
     * Merges two phone number lists into a single unmodifiable list, used as
     * a merge function when multiple telephone entries share the same type.
     *
     * @param first  the first list of JIDs
     * @param second the second list of JIDs
     * @return a combined unmodifiable list containing all JIDs from both
     *         input lists in order
     */
    private static List<Jid> joinPhoneNumbers(List<Jid> first, List<Jid> second) {
        return Stream.of(first, second).flatMap(Collection::stream).toList();
    }

    /**
     * Serialises this contact card back into its vCard string representation.
     *
     * @return the vCard string
     */
    @ProtobufSerializer
    String toVcard();

    /**
     * A structured representation of a parsed vCard, providing typed access
     * to the contact's name, phone numbers, version and optional business
     * name.
     *
     * <p>Phone numbers are organised by their vCard type (for example
     * {@code "CELL"}, {@code "HOME"} or {@code "WORK"}) and mapped to their
     * corresponding WhatsApp {@link Jid} values extracted from the
     * {@code WAID} parameter of each {@code TEL} entry. The class can be
     * serialised back to a valid vCard string via {@link #toVcard()}.
     */
    final class Parsed implements ContactCard {
        /**
         * The vCard version string (for example {@code "3.0"} or
         * {@code "4.0"}).
         */
        String version;

        /**
         * The formatted display name from the vCard's {@code FN} property, or
         * {@code null} if the vCard does not carry a formatted name.
         */
        String name;

        /**
         * The phone numbers associated with this contact, grouped by their
         * vCard telephone type. Each type maps to an unmodifiable list of
         * {@link Jid} values derived from the {@code WAID} parameter on the
         * corresponding {@code TEL} entries.
         */
        Map<String, List<Jid>> phoneNumbers;

        /**
         * The business display name from the {@code X-WA-BIZ-NAME} extended
         * property, or {@code null} if the contact is not a business account.
         */
        String businessName;

        /**
         * Constructs a new parsed contact card with the given values.
         *
         * @param version      the vCard version string
         * @param name         the formatted display name, or {@code null}
         * @param phoneNumbers the phone numbers grouped by vCard type
         * @param businessName the business display name, or {@code null}
         */
        Parsed(String version, String name, Map<String, List<Jid>> phoneNumbers, String businessName) {
            this.version = version;
            this.name = name;
            this.phoneNumbers = phoneNumbers;
            this.businessName = businessName;
        }

        /**
         * Returns the vCard version string.
         *
         * @return the version (for example {@code "3.0"})
         */
        public String version() {
            return version;
        }

        /**
         * Sets the vCard version string.
         *
         * @param version the version to set
         */
        public void setVersion(String version) {
            this.version = version;
    }

        /**
         * Returns the formatted display name from the vCard.
         *
         * @return an {@code Optional} containing the name, or empty if no
         *         formatted name is present
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Sets the formatted display name for this contact card.
         *
         * @param name the name to set, or {@code null} to clear
         */
        public void setName(String name) {
            this.name = name;
    }

        /**
         * Returns the business display name from the {@code X-WA-BIZ-NAME}
         * property.
         *
         * @return an {@code Optional} containing the business name, or empty
         *         if the contact is not a business account
         */
        public Optional<String> businessName() {
            return Optional.ofNullable(businessName);
        }

        /**
         * Sets the business display name for this contact card.
         *
         * @param businessName the business name to set, or {@code null} to
         *                     clear
         */
        public void setBusinessName(String businessName) {
            this.businessName = businessName;
    }

        /**
         * Returns the phone numbers associated with the given vCard telephone
         * type.
         *
         * @param type the telephone type (for example {@code "CELL"} or
         *             {@code "HOME"})
         * @return an unmodifiable list of JIDs for the given type, or an
         *         empty list if no numbers are registered under it
         */
        public List<Jid> phoneNumbers(String type) {
            return phoneNumbers.getOrDefault(type, List.of());
        }

        /**
         * Returns the phone numbers associated with the default telephone
         * type ({@code CELL}).
         *
         * @return an unmodifiable list of JIDs, or an empty list if no
         *         {@code CELL} numbers are present
         */
        public List<Jid> phoneNumbers() {
            return Objects.requireNonNullElseGet(phoneNumbers.get(DEFAULT_NUMBER_VCARD_TYPE), List::of);
        }

        /**
         * Adds a phone number under the default telephone type
         * ({@code CELL}).
         *
         * @param contact the non-{@code null} JID to add
         */
        public void addPhoneNumber(Jid contact) {
            addPhoneNumber(DEFAULT_NUMBER_VCARD_TYPE, contact);
        }

        /**
         * Adds a phone number under the specified telephone type.
         *
         * @param category the telephone type (for example {@code "CELL"} or
         *                 {@code "HOME"})
         * @param contact  the non-{@code null} JID to add
         */
        public void addPhoneNumber(String category, Jid contact) {
            var oldValue = phoneNumbers.get(category);
            if (oldValue == null) {
                phoneNumbers.put(category, List.of(contact));
                return;
            }

            var values = new ArrayList<>(oldValue);
            values.add(contact);
            phoneNumbers.put(category, Collections.unmodifiableList(values));
        }

        /**
         * Serialises this parsed contact card to a valid vCard string.
         *
         * <p>The output includes the version, formatted name, the telephone
         * entries with their {@code WAID} parameters, and the
         * {@code X-WA-BIZ-NAME} property when a business name is present.
         *
         * @return a non-{@code null} vCard string
         */
        @Override
        @ProtobufSerializer
        public String toVcard() {
            var vcard = new VCard();
            vcard.setVersion(VCardVersion.valueOfByStr(version()));
            vcard.setFormattedName(name);
            phoneNumbers.forEach((type, contacts) -> {
                for(var contact : contacts) {
                    contact.toPhoneNumber().ifPresent(phoneNumber -> {
                        var telephone = new Telephone(phoneNumber);
                        telephone.getParameters().setType(type);
                        telephone.getParameters().put(PHONE_NUMBER_VCARD_PROPERTY, contact.user());
                        vcard.addTelephoneNumber(telephone);
                    });
                }
            });
            if(businessName != null) {
                vcard.addExtendedProperty(BUSINESS_NAME_VCARD_PROPERTY, businessName);
            }
            return Ezvcard.write(vcard)
                    .go();
        }

        /**
         * Returns whether this parsed contact card is equal to the given
         * object.
         *
         * <p>Two parsed contact cards are considered equal when their
         * version, name, phone numbers and business name are all equal.
         *
         * @param o the object to compare with
         * @return {@code true} if the other object is a {@code Parsed}
         *         instance with identical field values
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Parsed parsed
                    && Objects.equals(version, parsed.version)
                    && Objects.equals(name, parsed.name)
                    && Objects.equals(phoneNumbers, parsed.phoneNumbers)
                    && Objects.equals(businessName, parsed.businessName);
        }

        /**
         * Returns a hash code derived from the version, name, phone numbers
         * and business name fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(version, name, phoneNumbers, businessName);
        }

        /**
         * Returns a string representation of this parsed contact card
         * including all field values.
         *
         * @return a descriptive string
         */
        @Override
        public String toString() {
            return "ContactCard[" +
                    "version=" + version + ", " +
                    "name=" + name + ", " +
                    "phoneNumbers=" + phoneNumbers + ", " +
                    "businessName=" + businessName + ']';
        }
    }

    /**
     * A raw, unparsed representation of a vCard string.
     *
     * <p>This variant is used as a fallback when the vCard string cannot be
     * decomposed into a {@link Parsed} instance, either because the content
     * is malformed or because the parsing library is unavailable. The
     * original vCard string is preserved verbatim so that it can still be
     * forwarded unchanged when the card is re-serialised.
     */
    final class Raw implements ContactCard {
        /**
         * The original, unparsed vCard string.
         */
        String toVcard;

        /**
         * Constructs a new raw contact card wrapping the given vCard string.
         *
         * @param toVcard the raw vCard string
         */
        Raw(String toVcard) {
            this.toVcard = toVcard;
        }

        /**
         * Returns the original, unparsed vCard string.
         *
         * @return the raw vCard string
         */
        @Override
        @ProtobufSerializer
        public String toVcard() {
            return toVcard;
        }

        /**
         * Returns whether this raw contact card is equal to the given object.
         *
         * <p>Two raw contact cards are considered equal when they wrap the
         * same vCard string.
         *
         * @param o the object to compare with
         * @return {@code true} if the other object is a {@code Raw} instance
         *         with the same vCard string
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Raw raw
                    && Objects.equals(toVcard, raw.toVcard);
        }

        /**
         * Returns a hash code derived from the raw vCard string.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(toVcard);
        }

        /**
         * Returns a string representation of this raw contact card including
         * the wrapped vCard content.
         *
         * @return a descriptive string
         */
        @Override
        public String toString() {
            return "Raw[" +
                    "toVcard=" + toVcard + ']';
        }
    }
}
