package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import it.auties.whatsapp.model.contact.ContactJid;
import lombok.NonNull;

public record PhoneNumber(long number, @NonNull CountryCode countryCode) {
    @JsonCreator
    public static PhoneNumber of(long phoneNumber) {
        try {
            var parsed = PhoneNumberUtil.getInstance().parse("+%s".formatted(phoneNumber), null);
            return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
                    .map(countryCode -> new PhoneNumber(parsed.getNationalNumber(), countryCode))
                    .orElseThrow(() -> new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber)));
        } catch (NumberFormatException | NumberParseException exception) {
            throw new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber), exception);
        }
    }

    public ContactJid toJid(){
        return ContactJid.of("%s%s".formatted(countryCode.prefix(), number));
    }
}
