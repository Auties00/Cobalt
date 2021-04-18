package it.auties.whatsapp4j.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.utils.CypherUtils;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.Base64;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(fluent = true, chain = true)
public class WhatsappKeysManager {
    private static final String PREFERENCES_PATH = WhatsappKeysManager.class.getName();
    private static final ObjectWriter JACKSON_WRITER = new ObjectMapper().writer();
    private static final ObjectReader JACKSON_READER = new ObjectMapper().reader();
    @JsonProperty
    private @NotNull String clientId;
    @JsonProperty
    private byte @NotNull [] publicKey, privateKey;
    @JsonProperty
    private String serverToken, clientToken;
    @JsonProperty
    private BinaryArray encKey, macKey;

    /**
     * Constructs a new WhatsappKeysManager from the saved preferences on this machine
     *
     * @return a new WhatsappKeysManager with the above characteristics
     */
    @SneakyThrows
    public static WhatsappKeysManager fromPreferences() {
        final var preferences = Preferences.userRoot().get(PREFERENCES_PATH, null);
        if(preferences != null){
            return JACKSON_READER.readValue(preferences, WhatsappKeysManager.class);
        }

        var keyPair = CypherUtils.calculateRandomKeyPair();
        return new WhatsappKeysManager(Base64.getEncoder().encodeToString(BinaryArray.random(16).data()), keyPair.getPublicKey(), keyPair.getPrivateKey(), null, null, null, null);
    }

    /**
     * Checks if the serverToken and clientToken are not null
     *
     * @return true if both the serverToken and clientToken are not null
     */
    public boolean mayRestore() {
        return Objects.nonNull(serverToken) && Objects.nonNull(clientToken);
    }

    /**
     * Initializes the serverToken, clientToken, encryptionKey and macKey with non null values
     */
    @SneakyThrows
    public void initializeKeys(@NotNull String serverToken, @NotNull String clientToken, @NotNull BinaryArray encKey, @NotNull BinaryArray macKey){
        Preferences.userRoot().put(PREFERENCES_PATH, JACKSON_WRITER.writeValueAsString(encKey(encKey).macKey(macKey).serverToken(serverToken).clientToken(clientToken)));
    }

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     */
    @SneakyThrows
    public void deleteKeysFromMemory(){
        Preferences.userRoot().clear();
    }
}
