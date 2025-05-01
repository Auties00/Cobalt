package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.crypto.MD5;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;


@ProtobufMessage(name = "ClientPayload.UserAgent.AppVersion")
public record Version(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        Integer primary,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer secondary,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        Integer tertiary,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        Integer quaternary,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        Integer quinary
) {
    public Version(int primary) {
        this(primary, null, null, null, null);
    }

    public Version(int primary, int secondary, int tertiary) {
        this(primary, secondary, tertiary, null, null);
    }

    @JsonCreator
    public static Version of(String version) {
        var tokens = version.split("\\.", 5);
        if (tokens.length > 5) {
            throw new IllegalArgumentException("Invalid number of tokens for version %s: %s".formatted(version, tokens));
        }

        var primary = tokens.length > 0 ? parseInt(tokens[0]) : null;
        var secondary = tokens.length > 1 ? parseInt(tokens[1]) : null;
        var tertiary = tokens.length > 2 ? parseInt(tokens[2]) : null;
        var quaternary = tokens.length > 3 ? parseInt(tokens[3]) : null;
        var quinary = tokens.length > 4 ? parseInt(tokens[4]) : null;
        return new Version(primary, secondary, tertiary, quaternary, quinary);
    }

    public byte[] toHash() {
        return MD5.calculate(toString());
    }

    @Override
    @JsonValue
    public String toString() {
        return Stream.of(primary, secondary, tertiary, quaternary, quinary)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }
}