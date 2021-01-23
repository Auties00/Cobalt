package it.auties.whatsapp4j.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.utils.BytesArray;
import it.auties.whatsapp4j.utils.CypherUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
public class WhatsappKeysManager {
    @JsonProperty
    private static final @NotNull ObjectWriter JACKSON_WRITER = new ObjectMapper().writer();
    @JsonProperty
    private static final @NotNull ObjectReader JACKSON_READER = new ObjectMapper().reader();
    @JsonProperty
    private @NotNull String clientId;
    @JsonProperty
    private @Nullable String serverToken, clientToken;
    @JsonProperty
    private byte[] publicKey, privateKey;
    @JsonProperty
    private @Nullable BytesArray encKey, macKey;

    @SneakyThrows
    public static WhatsappKeysManager fromPreferences() {
        final var preferences = Preferences.userRoot().get("whatsapp", null);
        if(preferences != null){
            return JACKSON_READER.readValue(preferences, WhatsappKeysManager.class);
        }

        var keyPair = CypherUtils.calculateRandomKeyPair();
        return new WhatsappKeysManager(Base64.getEncoder().encodeToString(BytesArray.random(16).data()), null, null, keyPair.getPublicKey(), keyPair.getPrivateKey(), null, null);
    }

    public boolean mayRestore() {
        return Objects.nonNull(serverToken) && Objects.nonNull(clientToken);
    }

    @SneakyThrows
    public void initializeKeys(@NotNull String serverToken, @NotNull String clientToken, @NotNull BytesArray encKey, @NotNull BytesArray macKey){
        encKey(encKey).macKey(macKey).serverToken(serverToken).clientToken(clientToken);
        Preferences.userRoot().put("whatsapp", JACKSON_WRITER.writeValueAsString(this));
    }

    @SneakyThrows
    public void deleteKeysFromMemory(){
        Preferences.userRoot().clear();
    }
}
