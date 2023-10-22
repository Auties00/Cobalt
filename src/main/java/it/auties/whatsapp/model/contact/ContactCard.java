package it.auties.whatsapp.model.contact;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.SimpleProperty;
import ezvcard.property.Telephone;
import it.auties.protobuf.annotation.ProtobufConverter;
import it.auties.whatsapp.model.jid.Jid;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.util.Specification.Whatsapp.*;


/**
 * A model class to represent and build the vcard of a contact
 */
public sealed interface ContactCard {
    @ProtobufConverter
    static ContactCard ofProtobuf(String vcard) {
        return vcard == null ? null : of(vcard);
    }

    /**
     * Parses a vcard
     * If the vCard dependency wasn't included, or a parsing error occurs, a raw representation is returned
     *
     * @param vcard the non-null vcard to parse
     * @return a non-null vcard
     */
    static ContactCard of(String vcard) {
        try {
            var parsed = Ezvcard.parse(vcard).first();
            var version = Optional.ofNullable(parsed.getVersion().getVersion());
            var name = Optional.ofNullable(parsed.getFormattedName().getValue());
            var phoneNumbers = parsed.getTelephoneNumbers()
                    .stream()
                    .filter(ContactCard::isValidPhoneNumber)
                    .collect(Collectors.toUnmodifiableMap(ContactCard::getPhoneType, ContactCard::getPhoneValue, ContactCard::joinPhoneNumbers));
            var businessName = Optional.ofNullable(parsed.getExtendedProperty(BUSINESS_NAME_VCARD_PROPERTY))
                    .map(SimpleProperty::getValue);
            return new Parsed(version, name, phoneNumbers, businessName);
        } catch (Throwable ignored) {
            return new Raw(vcard);
        }
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

    String toVcard();

    /**
     * A parsed representation of the vcard
     */
    record Parsed(
            Optional<String> version,
            Optional<String> name,
            Map<String, List<Jid>> phoneNumbers,
            Optional<String> businessName
    ) implements ContactCard {
        public List<Jid> getPhoneNumber(Jid contact) {
            return Objects.requireNonNullElseGet(phoneNumbers.get(DEFAULT_NUMBER_VCARD_TYPE), List::of);
        }

        private void addPhoneNumber(VCard vcard, String type, Jid contact) {
            var telephone = new Telephone(contact.toPhoneNumber());
            telephone.getParameters().setType(type);
            telephone.getParameters().put(PHONE_NUMBER_VCARD_PROPERTY, contact.user());
            vcard.addTelephoneNumber(telephone);
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
        @ProtobufConverter
        public String toVcard() {
            var vcard = new VCard();
            vcard.setVersion(version().map(VCardVersion::valueOfByStr).orElse(VCardVersion.V3_0));
            vcard.setFormattedName(name.orElse(null));
            phoneNumbers().forEach((type, contacts) -> contacts.forEach(contact -> addPhoneNumber(vcard, type, contact)));
            businessName.ifPresent(value -> vcard.addExtendedProperty(BUSINESS_NAME_VCARD_PROPERTY, value));
            return Ezvcard.write(vcard).go();
        }
    }

    /**
     * A raw representation of the vcard
     */
    record Raw(String toVcard) implements ContactCard {
        @Override
        @ProtobufConverter
        public String toVcard() {
            return toVcard;
        }
    }
}