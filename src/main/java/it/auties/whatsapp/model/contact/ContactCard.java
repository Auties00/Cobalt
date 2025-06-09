package it.auties.whatsapp.model.contact;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Telephone;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A model class to represent and build the vcard of a contact
 */
public sealed interface ContactCard {
    String BUSINESS_NAME_VCARD_PROPERTY = "X-WA-BIZ-NAME";
    String PHONE_NUMBER_VCARD_PROPERTY = "WAID";
    String DEFAULT_NUMBER_VCARD_TYPE = "CELL";

    /**
     * Parses a vcard
     * If the vCard dependency wasn't included, or a parsing error occurs, a raw representation is returned
     *
     * @param vcard the non-null vcard to parse
     * @return a non-null vcard
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
     * Creates a new vcard
     *
     * @param name        the nullable name of the contact
     * @param phoneNumber the non-null phone number of the contact
     * @return a vcard
     */
    static ContactCard of(String name, Jid phoneNumber) {
        return of(name, phoneNumber, null);
    }

    /**
     * Creates a new vcard
     *
     * @param name         the nullable name of the contact
     * @param phoneNumber  the non-null phone number of the contact
     * @param businessName the nullable business name of the contact
     * @return a vcard
     */
    static ContactCard of(String name, Jid phoneNumber, String businessName) {
        return new Parsed(
                VCardVersion.V3_0.getVersion(),
                name,
                Map.of(DEFAULT_NUMBER_VCARD_TYPE, List.of(Objects.requireNonNull(phoneNumber))),
                businessName
        );
    }

    private static boolean isValidPhoneNumber(Telephone entry) {
        return getPhoneType(entry) != null && entry.getParameter(PHONE_NUMBER_VCARD_PROPERTY) != null;
    }

    private static String getPhoneType(Telephone entry) {
        return entry.getParameters().getType();
    }

    private static List<Jid> getPhoneValue(Telephone entry) {
        return List.of(Jid.of(entry.getParameter(PHONE_NUMBER_VCARD_PROPERTY)));
    }

    private static List<Jid> joinPhoneNumbers(List<Jid> first, List<Jid> second) {
        return Stream.of(first, second).flatMap(Collection::stream).toList();
    }

    @ProtobufSerializer
    String toVcard();

    /**
     * A parsed representation of the vcard
     */
    final class Parsed implements ContactCard {
        private final String version;
        private final String name;
        private final Map<String, List<Jid>> phoneNumbers;
        private final String businessName;

        private Parsed(String version, String name, Map<String, List<Jid>> phoneNumbers, String businessName) {
            this.version = version;
            this.name = name;
            this.phoneNumbers = phoneNumbers;
            this.businessName = businessName;
        }

        public String version() {
            return version;
        }

        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        public Optional<String> businessName() {
            return Optional.ofNullable(businessName);
        }

        public List<Jid> phoneNumbers(String type) {
            return phoneNumbers.getOrDefault(type, List.of());
        }

        public List<Jid> phoneNumbers() {
            return Objects.requireNonNullElseGet(phoneNumbers.get(DEFAULT_NUMBER_VCARD_TYPE), List::of);
        }

        public void addPhoneNumber(Jid contact) {
            addPhoneNumber(DEFAULT_NUMBER_VCARD_TYPE, contact);
        }

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
         * Converts this object in a valid vcard
         *
         * @return a non-null String
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

        @Override
        public boolean equals(Object o) {
            return o instanceof Parsed parsed
                    && Objects.equals(version, parsed.version)
                    && Objects.equals(name, parsed.name)
                    && Objects.equals(phoneNumbers, parsed.phoneNumbers)
                    && Objects.equals(businessName, parsed.businessName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, name, phoneNumbers, businessName);
        }

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
     * A raw representation of the vcard
     */
    final class Raw implements ContactCard {
        private final String toVcard;

        private Raw(String toVcard) {
            this.toVcard = toVcard;
        }

        @Override
        @ProtobufSerializer
        public String toVcard() {
            return toVcard;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Raw raw
                    && Objects.equals(toVcard, raw.toVcard);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(toVcard);
        }

        @Override
        public String toString() {
            return "Raw[" +
                    "toVcard=" + toVcard + ']';
        }
    }
}