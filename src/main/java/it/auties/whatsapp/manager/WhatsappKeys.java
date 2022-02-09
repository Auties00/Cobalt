package it.auties.whatsapp.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.protobuf.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.protobuf.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyName;
import it.auties.whatsapp.protobuf.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.protobuf.signal.session.Session;
import it.auties.whatsapp.protobuf.signal.session.SessionAddress;
import it.auties.whatsapp.protobuf.sync.AppStateSyncKey;
import it.auties.whatsapp.protobuf.sync.LTHashState;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Validate;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.BackingStoreException;

import static java.util.prefs.Preferences.userRoot;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
@Jacksonized
@Data
@Accessors(fluent = true, chain = true)
@Log
@SuppressWarnings({"unused", "UnusedReturnValue"}) // Chaining
public class WhatsappKeys implements JacksonProvider {
    /**
     * The path used to serialize and deserialize this object
     */
    private static final String PREFERENCES_PATH = WhatsappKeys.class.getName();

    /**
     * The client jid
     */
    @JsonProperty
    private int id;

    /**
     * The secret key pair used for buffer messages
     */
    @JsonProperty
    @NonNull
    private SignalKeyPair companionKeyPair;

    /**
     * The ephemeral key pair
     */
    @JsonProperty
    @NonNull
    private SignalKeyPair ephemeralKeyPair;

    /**
     * The signed identity key
     */
    @JsonProperty
    @NonNull
    private SignalKeyPair identityKeyPair;

    /**
     * The signed pre key
     */
    @JsonProperty
    @NonNull
    private SignalSignedKeyPair signedKeyPair;

    /**
     * The adv secret key
     */
    @JsonProperty
    private byte @NonNull [] companionKey;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @JsonProperty
    @NonNull
    private LinkedList<@NonNull SignalPreKeyPair> preKeys;

    /**
     * The user using these keys
     */
    @JsonProperty
    private ContactJid companion;

    /**
     * Sender keys for signal implementation
     */
    @JsonProperty
    private Map<SenderKeyName, SenderKeyRecord> senderKeys;

    /**
     * Receiver keys for signal implementation
     */
    @JsonProperty
    private Map<SenderKeyName, SenderKeyDistributionMessage> receiverKeys;

    /**
     * Sessions map
     */
    @JsonProperty
    private Map<SessionAddress, Session> sessions;

    /**
     * Trusted keys
     */
    @JsonProperty
    private Map<SessionAddress, byte[]> identities;

    /**
     * Hash state
     */
    @JsonProperty
    private Map<String, LTHashState> hashStates;

    /**
     * App state keys
     */
    @JsonProperty
    private LinkedList<AppStateSyncKey> appStateKeys;

    /**
     * Write counter for IV
     */
    @NonNull
    @Default
    private AtomicLong writeCounter = new AtomicLong();

    /**
     * Read counter for IV
     */
    @NonNull
    @Default
    private AtomicLong readCounter = new AtomicLong();

    /**
     * Session dependent keys to write and read cyphered messages
     */
    private BinaryArray writeKey, readKey;

    /**
     * The bytes of the encoded {@link SignedDeviceIdentityHMAC} received during the auth process
     */
    private byte[] companionIdentity;

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     */
    public static void deleteAllKeys() {
        try {
            userRoot().clear();
        }catch (BackingStoreException exception){
            throw new RuntimeException("Cannot delete keys from memory", exception);
        }
    }

    /**
     * Clears the keys associated with the provided jid
     *
     * @param id the jid of the keys
     */
    public static void deleteKeys(int id) {
        var path = WhatsappUtils.createPreferencesPath(PREFERENCES_PATH, id);
        userRoot().remove(path);
    }

    /**
     * Returns a new instance of random keys
     *
     * @param id the unsigned jid of these keys
     * @return a non-null instance of WhatsappKeys
     */
    public static WhatsappKeys newKeys(int id){
        return new WhatsappKeys(id);
    }

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @param id the jid of this session
     * @return a non-null instance of WhatsappKeys
     */
    public static WhatsappKeys fromMemory(int id){
        var path = WhatsappUtils.createPreferencesPath(PREFERENCES_PATH, id);
        var json = userRoot().get(path, null);
        return Optional.ofNullable(json)
                .map(value -> deserialize(id, value))
                .orElseGet(() -> newKeys(id));
    }

    private static WhatsappKeys deserialize(int id, String json) {
        try {
            return JACKSON.readValue(json, WhatsappKeys.class);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            log.warning("Cannot read keys for jid %s: defaulting to new keys".formatted(id));
            return null;
        }
    }

    private WhatsappKeys(int id) {
        this.id = WhatsappUtils.saveId(id);
        this.companionKeyPair = SignalKeyPair.random();
        this.ephemeralKeyPair = SignalKeyPair.random();
        this.identityKeyPair = SignalKeyPair.random();
        this.signedKeyPair = SignalSignedKeyPair.of(id, identityKeyPair());
        this.companionKey = SignalKeyPair.random().publicKey();
        this.senderKeys = new ConcurrentHashMap<>();
        this.receiverKeys = new ConcurrentHashMap<>();
        this.preKeys = new LinkedList<>();
        this.sessions = new ConcurrentHashMap<>();
        this.identities = new ConcurrentHashMap<>();
        this.hashStates = new ConcurrentHashMap<>();
        this.appStateKeys = new LinkedList<>();
        this.readCounter = new AtomicLong();
        this.writeCounter = new AtomicLong();
    }

    /**
     * Serializes this object to a json and saves it in memory
     *
     * @return this
     */
    public WhatsappKeys save(){
        try {
            var path = WhatsappUtils.createPreferencesPath(PREFERENCES_PATH, id);
            userRoot().put(path, JACKSON.writeValueAsString(this));
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
        this.readKey = null;
        this.writeKey = null;
        this.writeCounter.set(0);
        this.readCounter.set(0);
        return this;
    }

    /**
     * Clears all the keys from this machine's memory.
     * This method doesn't clear this object's values.
     *
     * @return this
     */
    public WhatsappKeys delete() {
        deleteKeys(id);
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
     * Queries the first {@link SenderKeyRecord} that matches {@code name}
     *
     * @param name the non-null name to search
     * @return a non-null Optional SenderKeyRecord
     */
    public Optional<SenderKeyRecord> findSenderKeyByName(@NonNull SenderKeyName name) {
        return Optional.ofNullable(senderKeys.get(name));
    }

    /**
     * Queries the {@link Session} that matches {@code address}
     *
     * @param address the non-null address to search
     * @return a non-null Optional SessionRecord
     */
    public Optional<Session> findSessionByAddress(@NonNull SessionAddress address){
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Queries the trusted key that matches {@code jid}
     *
     * @param id the jid to search
     * @return a non-null signed key pair
     * @throws IllegalArgumentException if no element can be found
     */
    public SignalSignedKeyPair findSignedKeyPairById(int id) {
        Validate.isTrue(id == signedKeyPair.id(), "Id mismatch: %s != %s", id, signedKeyPair.id());
        return signedKeyPair;
    }

    /**
     * Queries the trusted key that matches {@code jid}
     *
     * @param id the non-null jid to search
     * @return a non-null Optional signal pre key
     */
    public Optional<SignalPreKeyPair> findPreKeyById(int id) {
        return preKeys.stream()
                .filter(preKey -> preKey.id() == id)
                .findFirst();
    }

    /**
     * Queries the app state key that matches {@code jid}
     *
     * @param id the non-null jid to search
     * @return a non-null Optional app state dataSync key
     */
    public Optional<AppStateSyncKey> findAppKeyById(byte[] id) {
        return appStateKeys.stream()
                .filter(preKey -> preKey.keyId() != null && Arrays.equals(preKey.keyId().keyId(), id))
                .findFirst();
    }

    /**
     * Queries the hash state that matches {@code name}.
     * Otherwise, creates a new one.
     *
     * @param name the non-null name to search
     * @return a non-null hash state
     */
    public LTHashState findHashStateByName(@NonNull String name) {
        return Objects.requireNonNull(hashStates.get(name), "Missing hash state");
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
     * Checks whether the receiver key has been sent already
     *
     * @param group the group to check
     * @param participant the participant to check
     * @return true if the key was already sent
     */
    public boolean hasReceiverKey(@NonNull ContactJid group, @NonNull ContactJid participant){
        var senderKey = new SenderKeyName(group.toString(), participant.toSignalAddress());
        return receiverKeys.containsKey(senderKey);
    }

    /**
     * Checks whether a session already exists for the given address
     *
     * @param address the address to check
     * @return true if a session for that address already exists
     */
    public boolean hasSession(@NonNull SessionAddress address){
        return sessions.containsKey(address);
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

    /**
     * Adds the provided keys to the app state keys
     *
     * @param keys the keys to add
     * @return this
     */
    public WhatsappKeys addAppKeys(@NonNull Collection<AppStateSyncKey> keys){
        appStateKeys.addAll(keys);
        return this;
    }

    /**
     * Returns write counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long writeCounter(boolean increment){
        return increment ? writeCounter.getAndIncrement()
                : writeCounter.get();
    }

    /**
     * Returns read counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long readCounter(boolean increment){
        return increment ? readCounter.getAndIncrement()
                : readCounter.get();
    }
}