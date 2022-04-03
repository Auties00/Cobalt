package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class Version {
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int primary;

    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int secondary;

    @JsonProperty("3")
    @JsonPropertyDescription("uint32")
    private int tertiary;

    @JsonProperty("4")
    @JsonPropertyDescription("uint32")
    private int quaternary;

    @JsonProperty("5")
    @JsonPropertyDescription("uint32")
    private int quinary;

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
                .filter(entry -> entry != 0)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }
}
