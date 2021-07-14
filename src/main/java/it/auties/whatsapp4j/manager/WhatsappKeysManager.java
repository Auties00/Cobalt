package it.auties.whatsapp4j.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.serialization.KeyPairDeserializer;
import it.auties.whatsapp4j.serialization.KeyPairSerializer;
import it.auties.whatsapp4j.utils.internal.CypherUtils;
import lombok.*;
import lombok.experimental.Accessors;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true, chain = true)
public class WhatsappKeysManager {
    /**
     * The path used to serialize and deserialize this object
     */
    private static final String PREFERENCES_PATH = WhatsappKeysManager.class.getName();

    /**
     * An instance of Jackson's writer to serialize this object
     */
    private static final ObjectWriter JACKSON_WRITER = new ObjectMapper().writer();

    /**
     * An instance of Jackson's reader to serialize this object
     */
    private static final ObjectReader JACKSON_READER = new ObjectMapper().reader();

    /**
     * Returns a singleton instance of this object
     */
    private static final @Getter WhatsappKeysManager singletonInstance = buildInstance();

    @JsonProperty
    private @NonNull String clientId;
    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private @NonNull KeyPair keyPair;
    @JsonProperty
    private String serverToken, clientToken;
    @JsonProperty
    private BinaryArray encKey, macKey;

    /**
     * Constructs a singleton instance of {@link WhatsappKeysManager} using:
     * <ul>
     *     <li>The serialized object, saved in the Preferences linked to this machine at {@link WhatsappKeysManager#PREFERENCES_PATH}</li>
     *     <li>A new instance constructed using random keys if the previous could not be deserialized</li>
     * </ul>
     *
     * @return a non null {@link WhatsappKeysManager}
     */
    @SneakyThrows
    private static WhatsappKeysManager buildInstance() {
        var preferences = Preferences.userRoot().get(PREFERENCES_PATH, null);
        if (preferences != null) {
            return JACKSON_READER.readValue(preferences, WhatsappKeysManager.class);
        }

        return new WhatsappKeysManager(Base64.getEncoder().encodeToString(BinaryArray.random(16).data()), CypherUtils.calculateRandomKeyPair(), null, null, null, null);
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
    public void initializeKeys(@NonNull String serverToken, @NonNull String clientToken, @NonNull BinaryArray encKey, @NonNull BinaryArray macKey) {
        Preferences.userRoot().put(PREFERENCES_PATH, JACKSON_WRITER.writeValueAsString(encKey(encKey).macKey(macKey).serverToken(serverToken).clientToken(clientToken)));
    }

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     */
    @SneakyThrows
    public void deleteKeysFromMemory() {
        Preferences.userRoot().clear();
    }
}