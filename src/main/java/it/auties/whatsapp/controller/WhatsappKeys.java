package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.sync.AppStateSyncKey;
import it.auties.whatsapp.model.sync.LTHashState;
import it.auties.whatsapp.util.Preferences;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.java.Log;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElseGet;

/**
 * This controller holds the cryptographic-related data regarding a WhatsappWeb session
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder // FIXME: 22/03/2022 (access = AccessLevel.PROTECTED) TESTING ONLY!!!
@Jacksonized
@Data
@Accessors(fluent = true, chain = true)
@Log
@SuppressWarnings({"unused", "UnusedReturnValue"}) // Chaining
public final class WhatsappKeys implements WhatsappController {
    /**
     * The client id
     */
    private int id;

    /**
     * The secret key pair used for buffer messages
     */
    @Default
    @NonNull
    private SignalKeyPair noiseKeyPair = SignalKeyPair.random();

    /**
     * The ephemeral key pair
     */
    @Default
    @NonNull
    private SignalKeyPair ephemeralKeyPair = SignalKeyPair.random();

    /**
     * The signed identity key
     */
    @Default
    @NonNull
    private SignalKeyPair identityKeyPair = SignalKeyPair.random();

    /**
     * The signed pre key
     */
    private SignalSignedKeyPair signedKeyPair;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @Default
    @NonNull
    private ConcurrentLinkedDeque<SignalPreKeyPair> preKeys = new ConcurrentLinkedDeque<>();

    /**
     * The user using these keys
     */
    private ContactJid companion;

    /**
     * The companion secret key
     */
    @Default
    private byte[] companionKey = SignalKeyPair.random().publicKey();

    /**
     * The bytes of the encoded {@link SignedDeviceIdentityHMAC} received during the auth process
     */
    private SignedDeviceIdentity companionIdentity;

    /**
     * Sender keys for signal implementation
     */
    @NonNull
    @Default
    private Map<SenderKeyName, SenderKeyRecord> senderKeys = new ConcurrentHashMap<>();

    /**
     * Receiver keys for signal implementation
     */
    @NonNull
    @Default
    private Map<SenderKeyName, SenderKeyDistributionMessage> receiverKeys = new ConcurrentHashMap<>();

    /**
     * App state keys
     */
    @NonNull
    @Default
    private ConcurrentLinkedDeque<AppStateSyncKey> appStateKeys = new ConcurrentLinkedDeque<>();

    /**
     * Sessions map
     */
    @NonNull
    @Default
    private Map<SessionAddress, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Hash state
     */
    @NonNull
    @Default
    private Map<String, LTHashState> hashStates = new ConcurrentHashMap<>();

    /**
     * Write counter for IV
     */
    @NonNull
    @JsonIgnore
    @Default
    private AtomicLong writeCounter = new AtomicLong();

    /**
     * Read counter for IV
     */
    @NonNull
    @JsonIgnore
    @Default
    private AtomicLong readCounter = new AtomicLong();

    /**
     * Session dependent keys to write and read cyphered messages
     */
    @JsonIgnore
    private Bytes writeKey, readKey;

    /**
     * Deletes all the known keys from memory
     */
    public static void deleteAll() {
        var preferences = Preferences.of("keys");
        preferences.delete();
    }

    /**
     * Clears the keys associated with the provided id
     *
     * @param id the id of the keys
     */
    public static void deleteKeys(int id) {
        var preferences = Preferences.of("%s/keys.json", id);
        preferences.delete();
    }

    /**
     * Returns a new instance of random keys
     *
     * @param id the unsigned id of these keys
     * @return a non-null instance of WhatsappKeys
     */
    public static WhatsappKeys random(int id){
        var result = WhatsappKeys.builder()
                .id(id)
                .build();
        return result.signedKeyPair(SignalSignedKeyPair.of(result.id(), result.identityKeyPair()));
    }

    /**
     * Returns the keys saved in memory or constructs a new clean instance
     *
     * @param id the id of this session
     * @return a non-null instance of WhatsappKeys
     */
    public static WhatsappKeys of(int id){
        var preferences = Preferences.of("%s/keys.json", id);
        return requireNonNullElseGet(preferences.readJson(new TypeReference<>() {}), () -> random(id));
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
     * @return a non-null SenderKeyRecord
     */
    public SenderKeyRecord findSenderKeyByName(@NonNull SenderKeyName name) {
        return requireNonNullElseGet(senderKeys.get(name), () -> {
            var record = new SenderKeyRecord();
            senderKeys.put(name, record);
            return record;
        });
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
     * Queries the trusted key that matches {@code id}
     *
     * @param id the id to search
     * @return a non-null signed key pair
     * @throws IllegalArgumentException if no element can be found
     */
    public SignalSignedKeyPair findSignedKeyPairById(int id) {
        Validate.isTrue(id == signedKeyPair.id(),
                "Id mismatch: %s != %s",
                SecurityException.class,
                id, signedKeyPair.id());
        return signedKeyPair;
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the non-null id to search
     * @return a non-null pre key
     */
    public SignalPreKeyPair findPreKeyById(@NonNull Integer id) {
        return preKeys.stream()
                .filter(preKey -> preKey.id() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find a pre key with id %s"
                        .formatted(id)));
    }

    /**
     * Queries the app state key that matches {@code id}
     *
     * @param id the non-null id to search
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
        return Objects.requireNonNull(hashStates.get(name), "Missing hash state: %s".formatted(name));
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

    /**
     * Serializes this object to a json and saves it in memory
     */
    @Override
    public void save(boolean async){
        var preferences = Preferences.of("%s/keys.json", id);
        save(preferences, async);
    }

    @Override
    public void save(@NonNull Path path, boolean async) {
        save(Preferences.of(path), async);
    }

    @Override
    public void save(@NonNull Preferences preferences, boolean async) {
        if(async) {
            preferences.writeJsonAsync(this);
            return;
        }

        preferences.writeJson(this);
    }

    @JsonSetter
    private void defaultSignedKey(){
        this.signedKeyPair = SignalSignedKeyPair.of(id, identityKeyPair);
    }
}