package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Version implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = UINT32)
    private Integer primary;

    @ProtobufProperty(index = 2, type = UINT32)
    private Integer secondary;

    @ProtobufProperty(index = 3, type = UINT32)
    private Integer tertiary;

    @ProtobufProperty(index = 4, type = UINT32)
    private Integer quaternary;

    @ProtobufProperty(index = 5, type = UINT32)
    private Integer quinary;

    public Version(int primary) {
        this.primary = primary;
    }

    public Version(int primary, int secondary, int tertiary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    @SneakyThrows
    public byte[] toHash(){
        var digest = MessageDigest.getInstance("MD5");
        digest.update(toString().getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    @Override
    public String toString(){
        return Stream.of(primary, secondary, tertiary, quaternary, quinary)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }
}
