package it.auties.whatsapp.model.contact;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Telephone;
import it.auties.protobuf.base.ProtobufConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A model class to represent and build the vcard of a contact
 */
@AllArgsConstructor(staticName = "of")
@Value
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ContactCard {
    private static final String BUSINESS_NAME_PROPERTY = "X-WA-BIZ-NAME";
    private static final String PHONE_NUMBER_PROPERTY = "WAID";
    private static final String DEFAULT_NUMBER_TYPE = "CELL";

    /**
     * The version of the vcard
     */
    String version;

    /**
     * The name of the contact
     */
    String name;

    /**
     * The phone numbers, ordered by type
     */
    @NonNull Map<String, List<ContactJid>> phoneNumbers;

    /**
     * The business name
     */
    String businessName;

    /**
     * Do not use this method, reserved for protobuf
     */
    @ProtobufConverter
    public static ContactCard ofProtobuf(String vcard) {
        return vcard == null ? null : of(vcard);
    }

    /**
     * Parses a vcard
     *
     * @param vcard the non-null vcard to parse
     * @return a non-null vcard
     */
    public static ContactCard of(@NonNull String vcard) {
        var parsed = Ezvcard.parse(vcard).first();
        var version = parsed.getVersion().getVersion();
        var name = parsed.getFormattedName().getValue();
        var phoneNumbers = parsed.getTelephoneNumbers()
                .stream()
                .filter(ContactCard::isValidPhoneNumber)
                .collect(Collectors.toUnmodifiableMap(ContactCard::getPhoneType, ContactCard::getPhoneValue, ContactCard::joinPhoneNumbers));
        var businessName = parsed.getExtendedProperty(BUSINESS_NAME_PROPERTY);
        return ContactCard.of(version, name, phoneNumbers, businessName != null ? businessName.getValue() : null);
    }

    private static boolean isValidPhoneNumber(Telephone entry) {
        return getPhoneType(entry) != null && entry.getParameter(PHONE_NUMBER_PROPERTY) != null;
    }

    private static String getPhoneType(Telephone entry) {
        return entry.getParameters().getType();
    }

    private static List<ContactJid> getPhoneValue(Telephone entry) {
        return List.of(ContactJid.of(entry.getParameter(PHONE_NUMBER_PROPERTY)));
    }

    private static List<ContactJid> joinPhoneNumbers(List<ContactJid> first, List<ContactJid> second) {
        return Stream.of(first, second).flatMap(Collection::stream).toList();
    }

    /**
     * Returns the name of this card if defined
     *
     * @return an optional
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the business name of this card if defined
     *
     * @return an optional
     */
    public Optional<String> businessName() {
        return Optional.ofNullable(businessName);
    }

    /**
     * Converts this object in a valid vcard
     *
     * @return a non-null String
     */
    @ProtobufConverter
    public String toValue() {
        return toString();
    }

    /**
     * Converts this object in a valid vcard
     *
     * @return a non-null String
     */
    @Override
    public String toString() {
        return toVcard();
    }

    /**
     * Converts this object in a valid vcard
     *
     * @return a non-null String
     */
    public String toVcard() {
        var vcard = new VCard();
        vcard.setVersion(version().map(VCardVersion::valueOfByStr).orElse(VCardVersion.V3_0));
        vcard.setFormattedName(name);
        phoneNumbers().forEach((type, contacts) -> contacts.forEach(contact -> addPhoneNumber(vcard, type, contact)));
        if (businessName != null) {
            vcard.addExtendedProperty(BUSINESS_NAME_PROPERTY, businessName);
        }
        return Ezvcard.write(vcard).go();
    }

    private void addPhoneNumber(VCard vcard, String type, ContactJid contact) {
        var telephone = new Telephone(contact.toPhoneNumber());
        telephone.getParameters().setType(type);
        telephone.getParameters().put(PHONE_NUMBER_PROPERTY, contact.user());
        vcard.addTelephoneNumber(telephone);
    }

    /**
     * Returns the version of this card if defined
     *
     * @return an optional
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public static class ContactCardBuilder {
        public ContactCardBuilder phoneNumber(@NonNull ContactJid contact) {
            return phoneNumber(DEFAULT_NUMBER_TYPE, contact);
        }

        @SuppressWarnings("ConstantConditions")
        public ContactCardBuilder phoneNumber(@NonNull String category, @NonNull ContactJid contact) {
            if (phoneNumbers == null) {
                this.phoneNumbers = new HashMap<>();
            }
            var oldValue = phoneNumbers.get(category);
            if(oldValue == null){
                phoneNumbers.put(category, List.of(contact));
                return this;
            }
            var values = new ArrayList<>(oldValue);
            values.add(contact);
            phoneNumbers.put(category, Collections.unmodifiableList(values));
            return this;
        }
    }
}
