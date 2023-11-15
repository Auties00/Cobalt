package it.auties.whatsapp.model.mobile;

import java.util.Objects;
import java.util.Optional;

public record CountryLocale(String languageValue, String languageCode, char separator) {
    public static CountryLocale of(String encoded) {
        return of(encoded, '-')
                .or(() -> of(encoded, '_'))
                .orElseThrow(() -> new IllegalArgumentException("Cannot decode locale: " + encoded));
    }

    public static Optional<CountryLocale> of(String encoded, char separator) {
        Objects.requireNonNull(encoded, "Expected non-null encoded locale");
        var split = encoded.split(String.valueOf(separator));
        return split.length == 2 ? Optional.of(new CountryLocale(split[0], split[1], separator)) : Optional.empty();
    }

    @Override
    public String toString() {
        return languageValue + separator + languageCode;
    }
}
