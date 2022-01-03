package it.auties.whatsapp.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.SignalHelper;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.protobuf.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.protobuf.signal.session.SessionAddress;
import it.auties.whatsapp.protobuf.signal.session.Session;
import it.auties.whatsapp.util.Validate;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.Objects.requireNonNull;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@Log
@Data
@Accessors(fluent = true, chain = true)
@SuppressWarnings("UnusedReturnValue")
public class WhatsappKeys {
    /**
     * The path used to serialize and deserialize this object
     */
    public static final String PREFERENCES_PATH = WhatsappKeys.class.getName();

    /**
     * An instance of Jackson
     */
    private static final ObjectMapper JACKSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * The client id
     */
    @JsonProperty
    private int id;

    /**
     * The secret key pair used for buffer messages
     */
    @JsonProperty
    private @NonNull SignalKeyPair companionKeyPair;

    /**
     * The ephemeral key pair
     */
    @JsonProperty
    private @NonNull SignalKeyPair ephemeralKeyPair;

    /**
     * The signed identity key
     */
    @JsonProperty
    private @NonNull SignalKeyPair identityKeyPair;

    /**
     * The signed pre key
     */
    @JsonProperty
    private @NonNull SignalSignedKeyPair signedKeyPair;

    /**
     * The adv secret key
     */
    @JsonProperty
    private byte @NonNull [] companionKey;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @JsonProperty
    private @NonNull LinkedList<@NonNull SignalPreKeyPair> preKeys;

    /**
     * The user using these keys
     */
    @JsonProperty
    private ContactJid companion;

    /**
     * Session dependent keys to write and read cyphered messages
     */
    private BinaryArray writeKey, readKey;

    /**
     * Sender keys for signal implementation
     */
    private Map<SenderKeyName, SenderKeyRecord> senderKeys;

    /**
     * Sessions map
     */
    private Map<SessionAddress, Session> sessions;

    /**
     * Trusted keys
     */
    private Map<SessionAddress, byte[]> identities;

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     */
    public static void deleteAllKeys() {
        try {
            Preferences.userRoot().clear();
        }catch (BackingStoreException exception){
            throw new RuntimeException("Cannot delete keys from memory", exception);
        }
    }

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @return a non-null instance of WhatsappKeys
     */
    public static WhatsappKeys fromMemory(){
        return Optional.ofNullable(Preferences.userRoot().get(PREFERENCES_PATH, null))
                .map(WhatsappKeys::deserializeOrThrow)
                .orElse(new WhatsappKeys());
    }

    @SneakyThrows
    private static WhatsappKeys deserializeOrThrow(String json) {
        try {
            return JACKSON.readValue(json, WhatsappKeys.class);
        }catch (Throwable throwable){
            log.warning("Cannot read preferences: %s".formatted(throwable.getMessage()));
            Preferences.userRoot().clear();
            return null;
        }
    }

    /**
     * Constructs new WhatsappKeys using random keys
     */
    public WhatsappKeys() {
        this.id = SignalHelper.randomRegistrationId();
        this.companionKeyPair = SignalKeyPair.random();
        this.ephemeralKeyPair = SignalKeyPair.random();
        this.identityKeyPair = SignalKeyPair.random();
        this.signedKeyPair = SignalSignedKeyPair.with(id, identityKeyPair());
        this.companionKey = SignalHelper.randomSenderKey();
        this.senderKeys = new ConcurrentHashMap<>();
        this.preKeys = new LinkedList<>();
        this.sessions = new ConcurrentHashMap<>();
        this.identities = new ConcurrentHashMap<>();
    }

    /**
     * Serializes this object to a json and saves it in memory
     *
     * @return this
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
     * Clears the signal keys associated with this object
     *
     * @return this
     */
    public WhatsappKeys clear() {
        writeKey(null).readKey(null);
        return this;
    }

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     *
     * @return this
     */
    public WhatsappKeys delete() {
        deleteAllKeys();
        return this;
    }

    /**
     * Checks if the serverToken and clientToken are not null
     *
     * @return true if both the serverToken and clientToken are not null
     */
    public boolean hasCompanion() {
        return companion != null;
    }

    /**
     * Checks if the client sent pre keys to the server
     *
     * @return true if the client sent pre keys to the server
     */
    public boolean hasPreKeys() {
        return !preKeys.isEmpty();
    }

    /**
     * Queries the first {@link SenderKeyRecord} that matches {@code name}.
     * Otherwise, creates a new one.
     *
     * @param name the non-null name to search
     * @return a non-null SenderKeyRecord
     */
    public Optional<SenderKeyRecord> findSenderKeyByName(@NonNull SenderKeyName name) {
        return Optional.ofNullable(senderKeys.get(name));
    }

    /**
     * Queries the {@link Session} that matches {@code address}
     *
     * @param address the non-null address to search
     * @return a non-null SessionRecord
     */
    public Optional<Session> findSessionByAddress(@NonNull SessionAddress address){
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the id to search
     * @return a non-null array of bytes
     * @throws NullPointerException if no element can be found
     */
    public SignalSignedKeyPair findSignedKeyPairById(int id) {
        Validate.isTrue(id == signedKeyPair.id(), "Id mismatch: %s != %s", id, signedKeyPair.id());
        return signedKeyPair;
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the non-null id to search
     * @return a non-null array of bytes
     * @throws NullPointerException if no element can be found
     */
    public Optional<SignalPreKeyPair> findPreKeyById(int id) {
        return preKeys.stream()
                .filter(preKey -> preKey.id() == id)
                .findFirst();
    }

    /**
     * Checks whether {@code identityKey} is trusted for {@code address}
     *
     * @param address the non-null address
     * @param identityKey the nullable identity key
     * @return true if any match is found
     */
    public boolean hasTrust(@NonNull SessionAddress address, byte[] identityKey) {
        return true; // At least for now
    }

    /**
     * Adds the provided address and record to the known sessions
     *
     * @param address the non-null address
     * @param record the non-null record
     * @return this
     */
    public WhatsappKeys addSession(@NonNull SessionAddress address, @NonNull Session record){
        sessions.put(address, record);
        return this;
    }

    /**
     * Adds the provided name and key record to the known sender keys
     *
     * @param name the non-null name
     * @param record the non-null record
     * @return this
     */
    public WhatsappKeys addSenderKey(@NonNull SenderKeyName name, @NonNull SenderKeyRecord record){
        senderKeys.put(name, record);
        return this;
    }
}