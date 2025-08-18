package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public record PhoneNumber(CountryCode countryCode, long numberWithoutPrefix) {
    public static Optional<PhoneNumber> ofNullable(Long phoneNumber) {
        if (phoneNumber == null) {
            return Optional.empty();
        }

        return Optional.of(PhoneNumber.of(phoneNumber));
    }

    public static PhoneNumber of(String phoneNumber) {
        return of(Long.parseLong(phoneNumber));
    }

    @ProtobufDeserializer
    @JsonCreator
    public static PhoneNumber of(long phoneNumber) {
        try {
            var parsed = PhoneNumberUtil.getInstance().parse("+%s".formatted(phoneNumber), null);
            return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
                    .map(countryCode -> new PhoneNumber(countryCode, parsed.getNationalNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber)));
        } catch (Throwable exception) {
            throw new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber), exception);
        }
    }

    @ProtobufSerializer
    public long number() {
        return Long.parseLong(countryCode.prefix() + numberWithoutPrefix);
    }

    public String prefix() {
        return countryCode.prefix();
    }

    public Jid toJid() {
        return Jid.of(toString());
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(number());
    }
}
