package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
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
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Clock;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElseGet;

/**
 * This controller holds the cryptographic-related data regarding a WhatsappWeb session
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Keys extends Controller<Keys> {
    /**
     * The client id
     */
    private final int registrationId;

    /**
     * The secret key pair used for buffer messages
     */
    private final SignalKeyPair noiseKeyPair;

    /**
     * The ephemeral key pair
     */
    private final SignalKeyPair ephemeralKeyPair;

    /**
     * The signed identity key
     */
    private final SignalKeyPair identityKeyPair;

    /**
     * The companion secret key
     */
    private SignalKeyPair companionKeyPair;

    /**
     * The signed pre key
     */
    private final SignalSignedKeyPair signedKeyPair;

    /**
     * The signed key of the companion's device
     * This value will be null until it gets synced by whatsapp
     */
    private byte[] signedKeyIndex;

    /**
     * The timestampSeconds of the signed key companion's device
     */
    private Long signedKeyIndexTimestamp;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    private final List<SignalPreKeyPair> preKeys;

    /**
     * The phone id for the mobile api
     */
    private final String phoneId;

    /**
     * The device id for the mobile api
     */
    private final String deviceId;

    /**
     * The identity id for the mobile api
     */
    private final String recoveryToken;

    /**
     * The bytes of the encoded {@link SignedDeviceIdentityHMAC} received during the auth process
     */
    private SignedDeviceIdentity companionIdentity;

    /**
     * Sender keys for signal implementation
     */
    private final Map<SenderKeyName, SenderKeyRecord> senderKeys;

    /**
     * App state keys
     */
    private final Map<Jid, LinkedList<AppStateSyncKey>> appStateKeys;

    /**
     * Sessions map
     */
    private final Map<SessionAddress, Session> sessions;

    /**
     * Hash state
     */
    private final Map<Jid, Map<PatchType, CompanionHashState>> hashStates;

    private final Map<Jid, Collection<Jid>> groupsPreKeys;

    /**
     * Whether the client was registered
     */
    private boolean registered;

    /**
     * Whether the client has already sent its business certificate (mobile api only)
     */
    private boolean businessCertificate;

    /**
     * Whether the client received the initial app sync (web api only)
     */
    private boolean initialAppSync;

    /**
     * Write counter for IV
     */
    @JsonIgnore
    private final AtomicLong writeCounter;

    /**
     * Read counter for IV
     */
    @JsonIgnore
    private final AtomicLong readCounter;

    /**
     * Session dependent keys to write and read cyphered messages
     */
    @JsonIgnore
    private byte[] writeKey, readKey;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    Keys(UUID uuid, PhoneNumber phoneNumber, ControllerSerializer serializer, ClientType clientType, List<String> alias, int registrationId, SignalKeyPair noiseKeyPair, SignalKeyPair ephemeralKeyPair, SignalKeyPair identityKeyPair, SignalKeyPair companionKeyPair, SignalSignedKeyPair signedKeyPair, byte[] signedKeyIndex, Long signedKeyIndexTimestamp, List<SignalPreKeyPair> preKeys, String phoneId, String deviceId, String recoveryToken, SignedDeviceIdentity companionIdentity, Map<SenderKeyName, SenderKeyRecord> senderKeys, Map<Jid, LinkedList<AppStateSyncKey>> appStateKeys, Map<SessionAddress, Session> sessions, Map<Jid, Map<PatchType, CompanionHashState>> hashStates, Map<Jid, Collection<Jid>> groupsPreKeys, boolean registered, boolean businessCertificate, boolean initialAppSync) {
        super(uuid, phoneNumber, serializer, clientType, alias);
        this.registrationId = registrationId;
        this.noiseKeyPair = noiseKeyPair;
        this.ephemeralKeyPair = ephemeralKeyPair;
        this.identityKeyPair = identityKeyPair;
        this.companionKeyPair = companionKeyPair;
        this.signedKeyPair = signedKeyPair;
        this.signedKeyIndex = signedKeyIndex;
        this.signedKeyIndexTimestamp = signedKeyIndexTimestamp;
        this.preKeys = preKeys;
        this.phoneId = phoneId;
        this.deviceId = deviceId;
        this.recoveryToken = recoveryToken;
        this.companionIdentity = companionIdentity;
        this.senderKeys = senderKeys;
        this.appStateKeys = appStateKeys;
        this.sessions = sessions;
        this.hashStates = hashStates;
        this.groupsPreKeys = groupsPreKeys;
        this.registered = registered;
        this.businessCertificate = businessCertificate;
        this.initialAppSync = initialAppSync;
        this.writeCounter = new AtomicLong();
        this.readCounter = new AtomicLong();
    }

    /**
     * Creates a builder
     *
     * @return a builder
     */
    public static KeysBuilder builder() {
        return new KeysBuilder();
    }

    /**
     * Returns the encoded id
     *
     * @return a non-null byte array
     */
    public byte[] encodedRegistrationId() {
        return BytesHelper.intToBytes(registrationId(), 4);
    }

    /**
     * Clears the signal keys associated with this object
     */
    public void clearReadWriteKey() {
        this.writeKey = null;
        this.writeCounter.set(0);
        this.readCounter.set(0);
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
    public SenderKeyRecord findSenderKeyByName(SenderKeyName name) {
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
    public Optional<Session> findSessionByAddress(SessionAddress address) {
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the id to search
     * @return a non-null signed key pair
     * @throws IllegalArgumentException if no element can be found
     */
    public Optional<SignalSignedKeyPair> findSignedKeyPairById(int id) {
        return id == signedKeyPair.id() ? Optional.of(signedKeyPair) : Optional.empty();
    }

    /**
     * Queries the trusted key that matches {@code id}
     *
     * @param id the non-null id to search
     * @return a non-null pre key
     */
    public Optional<SignalPreKeyPair> findPreKeyById(Integer id) {
        return id == null ? Optional.empty() : preKeys.stream().filter(preKey -> preKey.id() == id).findFirst();
    }

    /**
     * Queries the app state key that matches {@code id}
     *
     * @param jid the non-null jid of the app key
     * @param id  the non-null id to search
     * @return a non-null Optional app state dataSync key
     */
    public Optional<AppStateSyncKey> findAppKeyById(Jid jid, byte[] id) {
        return Objects.requireNonNull(appStateKeys.get(jid), "Missing keys")
                .stream()
                .filter(preKey -> preKey.keyId() != null && Arrays.equals(preKey.keyId().keyId(), id))
                .findFirst();
    }

    /**
     * Queries the hash state that matches {@code name}. Otherwise, creates a new one.
     *
     * @param device    the non-null device
     * @param patchType the non-null name to search
     * @return a non-null hash state
     */
    public Optional<CompanionHashState> findHashStateByName(Jid device, PatchType patchType) {
        return Optional.ofNullable(hashStates.get(device))
                .map(entry -> entry.get(patchType));
    }

    /**
     * Checks whether {@code identityKey} is trusted for {@code address}
     *
     * @param address     the non-null address
     * @param identityKey the nullable identity key
     * @return true if any match is found
     */
    public boolean hasTrust(SessionAddress address, byte[] identityKey) {
        return true; // At least for now
    }

    /**
     * Checks whether a session already exists for the given address
     *
     * @param address the address to check
     * @return true if a session for that address already exists
     */
    public boolean hasSession(SessionAddress address) {
        return sessions.containsKey(address);
    }

    /**
     * Adds the provided address and record to the known sessions
     *
     * @param address the non-null address
     * @param record  the non-null record
     * @return this
     */
    public Keys putSession(SessionAddress address, Session record) {
        sessions.put(address, record);
        return this;
    }

    /**
     * Adds the provided hash state to the known ones
     *
     * @param device the non-null device
     * @param state  the non-null hash state
     * @return this
     */
    public Keys putState(Jid device, CompanionHashState state) {
        var oldData = Objects.requireNonNullElseGet(hashStates.get(device), HashMap<PatchType, CompanionHashState>::new);
        oldData.put(state.name(), state);
        hashStates.put(device, oldData);
        return this;
    }

    /**
     * Adds the provided keys to the app state keys
     *
     * @param jid  the non-null jid of the app key
     * @param keys the keys to add
     * @return this
     */
    public Keys addAppKeys(Jid jid, Collection<AppStateSyncKey> keys) {
        appStateKeys.put(jid, new LinkedList<>(keys));
        return this;
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public AppStateSyncKey getLatestAppKey(Jid jid) {
        var keys = Objects.requireNonNull(appStateKeys.get(jid), "Missing keys");
        return keys.getLast();
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public LinkedList<AppStateSyncKey> getAppKeys(Jid jid) {
        return Objects.requireNonNullElseGet(appStateKeys.get(jid), LinkedList::new);
    }

    /**
     * Adds the provided pre key to the pre keys
     *
     * @param preKey the key to add
     * @return this
     */
    public Keys addPreKey(SignalPreKeyPair preKey) {
        preKeys.add(preKey);
        return this;
    }

    /**
     * Returns write counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long writeCounter(boolean increment) {
        return increment ? writeCounter.getAndIncrement() : writeCounter.get();
    }

    /**
     * Returns read counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public long readCounter(boolean increment) {
        return increment ? readCounter.getAndIncrement() : readCounter.get();
    }

    /**
     * Returns the id of the last available pre key
     *
     * @return an integer
     */
    public int lastPreKeyId() {
        return preKeys.isEmpty() ? 0 : preKeys.get(preKeys.size() - 1).id();
    }

    /**
     * This function sets the companionIdentity field to the value of the companionIdentity parameter,
     * serializes the object, and returns the object.
     *
     * @param companionIdentity The identity of the companion device.
     * @return The object itself.
     */
    public Keys companionIdentity(SignedDeviceIdentity companionIdentity) {
        this.companionIdentity = companionIdentity;
        return this;
    }

    /**
     * Returns the companion identity of this session
     * Only available for web sessions
     *
     * @return an optional
     */
    public Optional<SignedDeviceIdentity> companionIdentity() {
        return Optional.ofNullable(companionIdentity);
    }

    /**
     * Returns all the registered pre keys
     *
     * @return a non-null collection
     */
    public Collection<SignalPreKeyPair> preKeys() {
        return Collections.unmodifiableList(preKeys);
    }

    public void addRecipientWithPreKeys(Jid group, Jid recipient) {
        var preKeys = groupsPreKeys.get(group);
        if (preKeys != null) {
            preKeys.add(recipient);
            return;
        }

        var newPreKeys = new ArrayList<Jid>();
        newPreKeys.add(recipient);
        groupsPreKeys.put(group, newPreKeys);
    }

    public void addRecipientsWithPreKeys(Jid group, Collection<Jid> recipients) {
        var preKeys = groupsPreKeys.get(group);
        if (preKeys != null) {
            preKeys.addAll(recipients);
            return;
        }

        var newPreKeys = new ArrayList<>(recipients);
        groupsPreKeys.put(group, newPreKeys);
    }

    public boolean hasGroupKeys(Jid group, Jid recipient) {
        var preKeys = groupsPreKeys.get(group);
        return preKeys != null && preKeys.contains(recipient);
    }

    @Override
    public void dispose() {
        serialize(false);
    }

    @Override
    public void serialize(boolean async) {
        serializer.serializeKeys(this, async);
    }

    public int registrationId() {
        return this.registrationId;
    }

    public SignalKeyPair noiseKeyPair() {
        return this.noiseKeyPair;
    }

    public SignalKeyPair ephemeralKeyPair() {
        return this.ephemeralKeyPair;
    }

    public SignalKeyPair identityKeyPair() {
        return this.identityKeyPair;
    }

    public SignalSignedKeyPair signedKeyPair() {
        return this.signedKeyPair;
    }

    public Optional<byte[]> signedKeyIndex() {
        return Optional.ofNullable(signedKeyIndex);
    }

    public OptionalLong signedKeyIndexTimestamp() {
        return Clock.parseTimestamp(signedKeyIndexTimestamp);
    }

    public SignalKeyPair companionKeyPair() {
        return this.companionKeyPair;
    }

    public String phoneId() {
        return this.phoneId;
    }

    public String deviceId() {
        return this.deviceId;
    }

    public String recoveryToken() {
        return this.recoveryToken;
    }

    public Map<SenderKeyName, SenderKeyRecord> senderKeys() {
        return this.senderKeys;
    }

    public Map<Jid, LinkedList<AppStateSyncKey>> appStateKeys() {
        return this.appStateKeys;
    }

    public Map<SessionAddress, Session> sessions() {
        return this.sessions;
    }

    public Map<Jid, Map<PatchType, CompanionHashState>> hashStates() {
        return this.hashStates;
    }

    public Map<Jid, Collection<Jid>> groupsPreKeys() {
        return this.groupsPreKeys;
    }

    public boolean registered() {
        return this.registered;
    }

    public boolean businessCertificate() {
        return this.businessCertificate;
    }

    public boolean initialAppSync() {
        return this.initialAppSync;
    }

    public AtomicLong writeCounter() {
        return this.writeCounter;
    }

    public AtomicLong readCounter() {
        return this.readCounter;
    }

    public Optional<byte[]> writeKey() {
        return Optional.ofNullable(this.writeKey);
    }

    public Optional<byte[]> readKey() {
        return Optional.ofNullable(this.readKey);
    }

    public Keys setCompanionKeyPair(SignalKeyPair companionKeyPair) {
        this.companionKeyPair = companionKeyPair;
        return this;
    }

    public Keys setSignedKeyIndex(byte[] signedKeyIndex) {
        this.signedKeyIndex = signedKeyIndex;
        return this;
    }

    public Keys setSignedKeyIndexTimestamp(Long signedKeyIndexTimestamp) {
        this.signedKeyIndexTimestamp = signedKeyIndexTimestamp;
        return this;
    }

    public Keys setCompanionIdentity(SignedDeviceIdentity companionIdentity) {
        this.companionIdentity = companionIdentity;
        return this;
    }

    public Keys setRegistered(boolean registered) {
        this.registered = registered;
        return this;
    }

    public Keys setBusinessCertificate(boolean businessCertificate) {
        this.businessCertificate = businessCertificate;
        return this;
    }

    public Keys setInitialAppSync(boolean initialAppSync) {
        this.initialAppSync = initialAppSync;
        return this;
    }

    public Keys setWriteKey(byte[] writeKey) {
        this.writeKey = writeKey;
        return this;
    }

    public Keys setReadKey(byte[] readKey) {
        this.readKey = readKey;
        return this;
    }
}