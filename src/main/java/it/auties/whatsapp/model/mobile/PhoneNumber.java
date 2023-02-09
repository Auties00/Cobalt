package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.NonNull;

public record PhoneNumber(long number, @NonNull CountryCode countryCode) {
  @JsonCreator
  public static PhoneNumber of(String phoneNumber){
    try {
      var prefixedPhoneNumber = phoneNumber.startsWith("+") ? phoneNumber : "+%s".formatted(phoneNumber);
      var parsed = PhoneNumberUtil.getInstance().parse(prefixedPhoneNumber, null);
      return CountryCode.ofPrefix(String.valueOf(parsed.getCountryCode()))
          .map(countryCode -> new PhoneNumber(parsed.getNationalNumber(), countryCode))
          .orElseThrow(() -> new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber)));
    } catch (NumberFormatException | NumberParseException exception){
      throw new IllegalArgumentException("Cannot parse phone number %s".formatted(phoneNumber), exception);
    }
  }
}
