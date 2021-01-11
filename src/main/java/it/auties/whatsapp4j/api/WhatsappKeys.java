package it.auties.whatsapp4j.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.utils.BytesArray;
import it.auties.whatsapp4j.utils.CypherUtils;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Objects;
import java.util.prefs.Preferences;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true, chain = true)
public class WhatsappKeys {
    @JsonProperty
    private static final @NotNull ObjectWriter JACKSON_WRITER = new ObjectMapper().writer();
    @JsonProperty
    private static final @NotNull ObjectReader JACKSON_READER = new ObjectMapper().reader();
    @JsonProperty
    private @NotNull String clientId;
    @JsonProperty
    private @Nullable String serverToken, clientToken;
    @JsonProperty
    private @NotNull byte[] publicKey, privateKey;
    @JsonProperty
    private @Nullable BytesArray encKey, macKey;

    @SneakyThrows
    public static WhatsappKeys buildInstance() {
        final var preferences = Preferences.userRoot().get("whatsapp", null);
        if(preferences != null){
            return JACKSON_READER.readValue(preferences, WhatsappKeys.class);
        }

        var keyPair = CypherUtils.calculateRandomKeyPair();
        return new WhatsappKeys(Base64.getEncoder().encodeToString(BytesArray.random(16).data()), null, null, keyPair.getPublicKey(), keyPair.getPrivateKey(), null, null);
    }

    public boolean mayRestore() {
        return Objects.nonNull(serverToken) && Objects.nonNull(clientToken);
    }

    @SneakyThrows
    public void initializeKeys(@NotNull String serverToken, @NotNull String clientToken, @NotNull BytesArray encKey, @NotNull BytesArray macKey){
        encKey(encKey).macKey(macKey).serverToken(serverToken).clientToken(clientToken);
        Preferences.userRoot().put("whatsapp", JACKSON_WRITER.writeValueAsString(this));
    }
}
