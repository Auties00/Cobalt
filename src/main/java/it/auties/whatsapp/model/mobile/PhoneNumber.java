package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import it.auties.whatsapp.model.contact.ContactJid;
import lombok.NonNull;

import java.util.Optional;

public record PhoneNumber(@NonNull CountryCode countryCode, long numberWithoutPrefix) {
    public static Optional<PhoneNumber> ofNullable(Long phoneNumber) {
        if(phoneNumber == null){
            return Optional.empty();
        }

        return Optional.of(PhoneNumber.of(phoneNumber));
    }

    public static PhoneNumber of(String phoneNumber) {
        return of(Long.parseLong(phoneNumber));
    }

    @JsonCreator
    public static PhoneNumber of(long phoneNumber) {
        try {
            var parsed = PhoneNumberUtil.getInstance().parse("+%s".formatted(phoneNumber), null);
            return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
                    .map(countryCode -> new PhoneNumber(countryCode, parsed.getNationalNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber)));
        } catch (NumberFormatException | NumberParseException exception) {
            throw new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber), exception);
        }
    }

    public long number() {
        return Long.parseLong(countryCode.prefix() + numberWithoutPrefix);
    }

    public String prefix() {
        return countryCode.prefix();
    }

    public ContactJid toJid(){
        return ContactJid.of(toString());
    }

    @Override
    public String toString() {
        return "%s%s".formatted(prefix(), numberWithoutPrefix);
    }
}
