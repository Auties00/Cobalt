package it.auties.whatsapp.model.contact;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Telephone;
import it.auties.protobuf.base.ProtobufConverter;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.serializer.exception.ProtobufSerializationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A model class to represent and build the vcard of a contact
 */
@AllArgsConstructor(staticName = "of")
@Value
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ContactCard
        implements ProtobufMessage {
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
    @NonNull Map<String, ContactJid> phoneNumbers;

    /**
     * The business name
     */
    String businessName;

    /**
     * Parses a vcard
     *
     * @param vcard the non-null vcard to parse
     * @return a non-null vcard
     */
    public static ContactCard of(@NonNull String vcard) {
        var parsed = Ezvcard.parse(vcard)
                .first();
        var version = parsed.getVersion()
                .getVersion();
        var name = parsed.getFormattedName()
                .getValue();
        var phoneNumbers = parsed.getTelephoneNumbers()
                .stream()
                .filter(ContactCard::isValidPhoneNumber)
                .collect(Collectors.toUnmodifiableMap(entry -> entry.getParameters()
                        .getType(), entry -> ContactJid.of(entry.getParameter(PHONE_NUMBER_PROPERTY))));
        var businessName = parsed.getExtendedProperty(BUSINESS_NAME_PROPERTY);
        return ContactCard.of(version, name, phoneNumbers, businessName != null ?
                businessName.getValue() :
                null);
    }

    private static boolean isValidPhoneNumber(Telephone entry) {
        return entry.getParameters()
                .getType() != null && entry.getParameter(PHONE_NUMBER_PROPERTY) != null;
    }

    @ProtobufConverter
    @SuppressWarnings("unused")
    public static ContactCard convert(Object input) {
        if (input == null) {
            return null;
        }

        if (input instanceof String string) {
            return ContactCard.of(string);
        }

        throw new ProtobufSerializationException(input.toString());
    }

    /**
     * Returns the version of this card if defined
     *
     * @return an optional
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
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
    public String toVcard() {
        var vcard = new VCard();
        vcard.setVersion(version().map(VCardVersion::valueOfByStr)
                                 .orElse(VCardVersion.V3_0));
        vcard.setFormattedName(name);
        phoneNumbers().forEach((type, contact) -> addPhoneNumber(vcard, type, contact));

        if (businessName != null) {
            vcard.addExtendedProperty(BUSINESS_NAME_PROPERTY, businessName);
        }

        return Ezvcard.write(vcard)
                .go();
    }

    private void addPhoneNumber(VCard vcard, String type, ContactJid contact) {
        var telephone = new Telephone("+%s".formatted(contact.user()));
        telephone.getParameters()
                .setType(type);
        telephone.getParameters()
                .put(PHONE_NUMBER_PROPERTY, contact.user());
        vcard.addTelephoneNumber(telephone);
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
    @Override
    public Object toValue() {
        return toString();
    }

    @Override
    public boolean isValueBased() {
        return true;
    }

    public static class ContactCardBuilder {
        @SuppressWarnings("ConstantConditions")
        public ContactCardBuilder phoneNumber(@NonNull ContactJid contact) {
            if (phoneNumbers == null) {
                this.phoneNumbers = new HashMap<>();
            }

            phoneNumbers.put(DEFAULT_NUMBER_TYPE, contact);
            return this;
        }
    }
}
