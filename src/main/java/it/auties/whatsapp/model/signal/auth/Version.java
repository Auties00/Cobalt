package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.response.AppVersionResponse;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;
import static java.lang.Integer.parseInt;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Version implements ProtobufMessage, JacksonProvider {
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

    public Version(@NonNull String version) {
        var tokens = version.split("\\.", 5);
        Validate.isTrue(tokens.length <= 5, "Invalid number of tokens for version %s: %s", version, tokens);
        if (tokens.length > 0) {
            this.primary = parseInt(tokens[0]);
        }

        if (tokens.length > 1) {
            this.secondary = parseInt(tokens[1]);
        }

        if (tokens.length > 2) {
            this.tertiary = parseInt(tokens[2]);
        }

        if (tokens.length > 3) {
            this.quaternary = parseInt(tokens[3]);
        }

        if (tokens.length > 4) {
            this.quinary = parseInt(tokens[4]);
        }
    }

    public Version(int primary) {
        this.primary = primary;
    }

    public Version(int primary, int secondary, int tertiary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    public static Version ofLatest(@NonNull Version defaultValue) {
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(
                            "https://web.whatsapp.com/check-update?version=%s&platform=web".formatted(defaultValue)))
                    .build();
            var response = client.send(request, ofString());
            var model = JSON.readValue(response.body(), AppVersionResponse.class);
            if (model.currentVersion() == null) {
                return defaultValue;
            }

            return new Version(model.currentVersion());
        } catch (Throwable throwable) {
            return defaultValue;
        }
    }

    @SneakyThrows
    public byte[] toHash() {
        var digest = MessageDigest.getInstance("MD5");
        digest.update(toString().getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    @Override
    public String toString() {
        return Stream.of(primary, secondary, tertiary, quaternary, quinary)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }
}
