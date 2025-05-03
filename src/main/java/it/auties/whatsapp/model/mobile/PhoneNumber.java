package it.auties.whatsapp.model.mobile;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public record PhoneNumber(long number, CountryCode countryCode, long numberWithoutPrefix) {
    public static Optional<PhoneNumber> of(String phoneNumber) {
        var parsed = Long.parseLong(phoneNumber, !phoneNumber.isEmpty() && phoneNumber.charAt(0) == '+' ? 1 : 0, phoneNumber.length(), 10);
        return of(parsed);
    }

    @ProtobufDeserializer
    public static Optional<PhoneNumber> of(Long phoneNumber) {
        if(phoneNumber == null) {
            return Optional.empty();
        }

        try {
            var parsed = PhoneNumberUtil.getInstance()
                    .parse("+" + phoneNumber, null);
            return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
                    .map(countryCode -> new PhoneNumber(phoneNumber, countryCode, parsed.getNationalNumber()));
        } catch (Throwable exception) {
            return Optional.empty();
        }
    }

    @ProtobufSerializer
    @Override
    public long number() {
        return number;
    }

    public Jid toJid() {
        return Jid.of(number);
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
