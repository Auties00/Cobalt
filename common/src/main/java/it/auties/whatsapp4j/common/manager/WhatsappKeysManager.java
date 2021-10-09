package it.auties.whatsapp4j.common.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.serialization.KeyPairDeserializer;
import it.auties.whatsapp4j.common.serialization.KeyPairSerializer;
import it.auties.whatsapp4j.common.serialization.IWhatsappSerializer;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Accessors(fluent = true, chain = true)
public class WhatsappKeysManager {
    /**
     * The path used to serialize and deserialize this object
     */
    public static final String PREFERENCES_PATH = WhatsappKeysManager.class.getName();

    /**
     * An instance of Jackson
     */
    protected static final ObjectMapper JACKSON = new ObjectMapper();

    /**
     * The client id
     */
    @JsonProperty
    private @NonNull String clientId;

    /**
     * The secret key pair used for binary messages
     */
    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private @NonNull KeyPair keyPair;

    /**
     * The secret server token
     */
    @JsonProperty
    private String serverToken;

    /**
     * The secret client token
     */
    @JsonProperty
    private String clientToken;

    /**
     * The encryption key, obtained during the handshake
     */
    @JsonProperty
    private BinaryArray encKey;

    /**
     * The mac key, obtained during the handshake
     */
    @JsonProperty
    private BinaryArray macKey;

    public WhatsappKeysManager(){
        this(Base64.getEncoder().encodeToString(BinaryArray.random(16).data()), CypherUtils.randomKeyPair(), null, null, null, null);
    }

    /**
     * Constructs an instance of {@link WhatsappKeysManager} using a json value
     *
     * @param json the encoded json
     * @return a non-null {@link WhatsappKeysManager}
     */
    public static WhatsappKeysManager fromJson(byte @NonNull [] json) {
        try {
            return JACKSON.readValue(json, WhatsappKeysManager.class);
        }catch (IOException exception){
            throw new RuntimeException("WhatsappAPI: Cannot deserialize WhatsappKeysManager from %s".formatted(new String(json)), exception);
        }
    }

    /**
     * Constructs an instance of {@link WhatsappKeysManager} using a json value
     *
     * @param json the encoded json
     * @return a non-null {@link WhatsappKeysManager}
     */
    public static WhatsappKeysManager fromJson(@NonNull  String json) {
        return fromJson(json.getBytes(StandardCharsets.UTF_8));
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
     * Initializes the serverToken, clientToken, encryptionKey and macKey with non-null values
     *
     * @return this object for chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public WhatsappKeysManager initializeKeys(String serverToken, String clientToken, @NonNull BinaryArray encKey, @NonNull BinaryArray macKey) {
        this.encKey(encKey).macKey(macKey).serverToken(serverToken).clientToken(clientToken);
        save();
        return this;
    }

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     * 
     * @return this object for chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public WhatsappKeysManager deleteKeysFromMemory() {
        try {
            Preferences.userRoot().clear();
            return this;
        }catch (BackingStoreException exception){
            throw new RuntimeException("Cannot delete keys from memory", exception);
        }
    }

    /**
     * Populates this object's fields with deserialized data
     *
     * @return this object for chaining
     */
    public WhatsappKeysManager withPreferences(){
        try {
            var preferences = Preferences.userRoot().get(PREFERENCES_PATH, null);
            return preferences == null ? this : JACKSON.readerForUpdating(this).readValue(preferences);
        }catch (JsonProcessingException exception){
            throw new RuntimeException("Cannot deserialize WhatsappKeysManager from preferences", exception);
        }
    }

    /**
     * Serializes this object to a json and saves it in memory
     *
     * @return this object for chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public WhatsappKeysManager save(){
        try {
            Preferences.userRoot().put(PREFERENCES_PATH, JACKSON.writeValueAsString(this));
            return this;
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("WhatsappAPI: Cannot serialize MultiDeviceKeysManager to JSON", exception);
        }
    }
}