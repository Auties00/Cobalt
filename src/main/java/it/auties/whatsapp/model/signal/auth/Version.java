package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.response.AppVersionResponse;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Spec.Whatsapp.WEB_UPDATE_URL;
import static java.lang.Integer.parseInt;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("AppVersion")
public class Version implements ProtobufMessage {
    private static Version cachedWebVersion;
    private static Version cachedMobileVersion;

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

    public static Version latest(ClientType type) {
        return switch (type){
            case WEB_CLIENT -> getLatestWebVersion();
            case APP_CLIENT -> getLatestMobileVersion();
        };
    }

    private static Version getLatestWebVersion() {
        try {
            if(cachedWebVersion != null){
                return cachedWebVersion;
            }

            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(WEB_UPDATE_URL))
                    .build();
            var response = client.send(request, ofString());
            var model = Json.readValue(response.body(), AppVersionResponse.class);
            return cachedWebVersion = new Version(model.currentVersion());
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot fetch latest web version", throwable);
        }
    }

    private static Version getLatestMobileVersion() {
        return new Version("2.23.9.71");
    }

    public byte[] toHash() {
        return MD5.calculate(toString());
    }

    @Override
    public String toString() {
        return Stream.of(primary, secondary, tertiary, quaternary, quinary)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining("."));
    }
}