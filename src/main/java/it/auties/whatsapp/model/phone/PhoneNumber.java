package it.auties.whatsapp.model.phone;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.util.Optional;
import lombok.NonNull;

public record PhoneNumber(long number, @NonNull CountryCode countryCode) {
  public static Optional<PhoneNumber> of(@NonNull String phoneNumber){
    try {
      var prefixedPhoneNumber = phoneNumber.startsWith("+") ? phoneNumber : "+%s".formatted(phoneNumber);
      var parsed = PhoneNumberUtil.getInstance().parse(prefixedPhoneNumber, null);
      var prefix = CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()));
      return prefix.map(countryCode -> new PhoneNumber(parsed.getNationalNumber(), countryCode));
    } catch (Throwable throwable) {
      return Optional.empty();
    }
  }
}
