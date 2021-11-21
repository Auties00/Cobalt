package it.auties.whatsapp.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.model.key.IdentityKeyPair;
import it.auties.whatsapp.protobuf.model.key.SignedKeyPair;
import it.auties.whatsapp.utils.KeyPairDeserializer;
import it.auties.whatsapp.utils.KeyPairSerializer;
import it.auties.whatsapp.utils.*;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.util.KeyHelper;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@Data
@Accessors(fluent = true, chain = true)
public class WhatsappKeys {
    /**
     * The path used to serialize and deserialize this object
     */
    public static final String PREFERENCES_PATH = WhatsappKeys.class.getName();

    /**
     * An instance of Jackson
     */
    private static final ObjectMapper JACKSON = new ObjectMapper();

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
     * The ephemeral key pair
     */
    @JsonProperty
    private IdentityKeyPair ephemeralKeyPair;

    /**
     * The signed identity key
     */
    @JsonProperty
    private IdentityKeyPair signedIdentityKey;

    /**
     * The signed pre key
     */
    @JsonProperty
    private SignedKeyPair signedPreKey;

    /**
     * The registration id
     */
    @JsonProperty
    private int id;

    /**
     * The adv secret key
     */
    @JsonProperty
    private @NonNull String advSecretKey;

    /**
     * The user using these keys
     */
    @JsonProperty
    private ContactId me;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @JsonProperty
    private boolean preKeys;

    /**
     * The write key
     */
    private BinaryArray writeKey;

    /**
     * The read key
     */
    private BinaryArray readKey;

    /**
     * Populates this object's fields with deserialized data
     *
     * @return this object for chaining
     */
    public static Optional<WhatsappKeys> fromMemory(){
        return Optional.ofNullable(Preferences.userRoot().get(PREFERENCES_PATH, null))
                .map(WhatsappKeys::deserializeOrThrow);
    }

    @SneakyThrows
    private static WhatsappKeys deserializeOrThrow(String json) {
        try {
            return JACKSON.readValue(json, WhatsappKeys.class);
        }catch (Throwable throwable){
            Preferences.userRoot().clear();
            return null;
        }
    }

    public WhatsappKeys() {
        this.clientId = BinaryArray.random(16).toBase64();
        this.keyPair = CypherUtils.randomKeyPair();
        this.ephemeralKeyPair = MultiDeviceCypher.createKeyPair();
        this.signedIdentityKey = MultiDeviceCypher.createKeyPair();
        this.signedPreKey = MultiDeviceCypher.generateSignedPreKey(signedIdentityKey());
        this.id = KeyHelper.generateRegistrationId(false);
        this.advSecretKey = Base64.getEncoder().encodeToString(KeyHelper.generateSenderKey());
    }

    /**
     * Checks if the serverToken and clientToken are not null
     *
     * @return true if both the serverToken and clientToken are not null
     */
    public boolean mayRestore() {
        return me != null;
    }

    /**
     * Serializes this object to a json and saves it in memory
     *
     * @return this object for chaining
     */
    public WhatsappKeys save(){
        try {
            Preferences.userRoot().put(PREFERENCES_PATH, JACKSON.writeValueAsString(this));
            return this;
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Cannot save keys to memory", exception);
        }
    }


    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     *
     * @return this object for chaining
     */
    public WhatsappKeys clear() {
        try {
            Preferences.userRoot().clear();
            return this;
        }catch (BackingStoreException exception){
            throw new RuntimeException("Cannot delete keys from memory", exception);
        }
    }
}