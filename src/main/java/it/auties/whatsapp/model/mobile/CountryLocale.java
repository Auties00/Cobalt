package it.auties.whatsapp.model.mobile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class CountryLocale {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String languageValue;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String languageCode;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String separator;

    CountryLocale(String languageValue, String languageCode, String separator) {
        this.languageValue = Objects.requireNonNull(languageValue, "languageValue cannot be null");
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode cannot be null");
        this.separator = Objects.requireNonNull(separator, "separator cannot be null");
    }

    public String languageValue() {
        return languageValue;
    }

    public String languageCode() {
        return languageCode;
    }

    public String separator() {
        return separator;
    }

    public static Optional<CountryLocale> of(String encoded) {
        return of(encoded, "-")
                .or(() -> of(encoded, "_"));
    }

    public static Optional<CountryLocale> of(String encoded, String separator) {
        Objects.requireNonNull(encoded, "Expected non-null encodedPoint locale");
        var split = encoded.split(String.valueOf(separator));
        return split.length == 2 ? Optional.of(new CountryLocale(split[0], split[1], separator)) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountryLocale that
                && Objects.equals(languageValue, that.languageValue)
                && Objects.equals(languageCode, that.languageCode)
                && Objects.equals(separator, that.separator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageValue, languageCode, separator);
    }

    @Override
    public String toString() {
        return "CountryLocale[" +
                "languageValue=" + languageValue + ", " +
                "languageCode=" + languageCode + ", " +
                "separator=" + separator + ']';
    }
}