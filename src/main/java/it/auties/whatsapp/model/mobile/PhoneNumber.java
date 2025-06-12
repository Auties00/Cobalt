package it.auties.whatsapp.model.mobile;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

// FIXME: The named constructors should return Optional<PhoneNumber>
//        They currently don't because I haven't implemented this feature yet in ModernProtobuf
public record PhoneNumber(long number, CountryCode countryCode, long numberWithoutPrefix) {
    public static Optional<PhoneNumber> of(String phoneNumber) {
        try {
            var parsed = Long.parseLong(phoneNumber, !phoneNumber.isEmpty() && phoneNumber.charAt(0) == '+' ? 1 : 0, phoneNumber.length(), 10);
            return of(parsed);
        }catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<PhoneNumber> of(Long phoneNumber) {
        return Optional.ofNullable(ofNullable(phoneNumber));
    }

    @ProtobufDeserializer
    public static PhoneNumber ofNullable(Long phoneNumber) {
        try {
            if(phoneNumber == null) {
                return null;
            }

            var parsed = PhoneNumberUtil.getInstance()
                    .parse("+" + phoneNumber, null);
            return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
                    .map(countryCode -> new PhoneNumber(phoneNumber, countryCode, parsed.getNationalNumber()))
                    .orElse(null);
        } catch (NumberParseException throwable) {
            return null;
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
