package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.model.response.AppVersionResponse;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentReleaseChannel;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Spec.Whatsapp.UPDATE_URL;
import static java.lang.Integer.parseInt;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@ProtobufName("AppVersion")
public class Version implements ProtobufMessage {
    private static volatile CompletableFuture<AppVersionResponse> appVersionResponse;

    static {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(UPDATE_URL))
                .build();
        appVersionResponse = client.sendAsync(request, ofString())
                .thenApplyAsync(response -> Json.readValue(response.body(), AppVersionResponse.class));
    }

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

    @JsonCreator
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

    public static Version latest(ClientType type, UserAgentPlatform platform, UserAgentReleaseChannel channel, boolean business) {
        var response = appVersionResponse.join();
        return switch (type){
            case WEB_CLIENT -> channel != UserAgentReleaseChannel.RELEASE ? response.web().beta() : response.web().latest();
            case APP_CLIENT -> switch (platform){
                case IOS -> business ? response.ios().messenger() : response.ios().business();
                case ANDROID -> business ? response.android().messenger() : response.android().business();
                default -> throw new IllegalStateException("Unsupported mobile os: " + platform);
            };
        };
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