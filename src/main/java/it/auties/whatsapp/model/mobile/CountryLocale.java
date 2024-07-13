package it.auties.whatsapp.model.mobile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public record CountryLocale(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String languageValue,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String languageCode,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String separator
) {
    public static Optional<CountryLocale> of(String encoded) {
        return of(encoded, "-")
                .or(() -> of(encoded, "_"));
    }

    public static Optional<CountryLocale> of(String encoded, String separator) {
        Objects.requireNonNull(encoded, "Expected non-null encoded locale");
        var split = encoded.split(String.valueOf(separator));
        return split.length == 2 ? Optional.of(new CountryLocale(split[0], split[1], separator)) : Optional.empty();
    }

    @Override
    public String toString() {
        return languageValue + separator + languageCode;
    }
}
