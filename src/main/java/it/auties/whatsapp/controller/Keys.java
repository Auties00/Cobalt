package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.companion.CompanionSyncKey;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentityHMAC;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.model.signal.sender.SenderPreKeys;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.sync.AppStateSyncKey;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNullElseGet;

/**
 * This controller holds the cryptographic-related data regarding a WhatsappWeb session
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
public final class Keys extends Controller<Keys> {
    /**
     * The client id
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    final Integer registrationId;

    /**
     * The secret key pair used for buffer messages
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final SignalKeyPair noiseKeyPair;

    /**
     * The ephemeral key pair
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final SignalKeyPair ephemeralKeyPair;

    /**
     * The signed identity key
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final SignalKeyPair identityKeyPair;

    /**
     * The companion secret key
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    SignalKeyPair companionKeyPair;

    /**
     * The signed pre key
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    SignalSignedKeyPair signedKeyPair;

    /**
     * The signed key of the companion's device
     * This value will be null until it gets synced by whatsapp
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    byte[] signedKeyIndex;

    /**
     * The timestampSeconds of the signed key companion's device
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    Long signedKeyIndexTimestamp;

    /**
     * Whether these keys have generated pre keys assigned to them
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    final List<SignalPreKeyPair> preKeys;

    /**
     * The phone id for the mobile api
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    final String fdid;

    /**
     * The device id for the mobile api
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BYTES)
    final byte[] deviceId;

    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    final UUID advertisingId;

    /**
     * The recovery token for the mobile api
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    final byte[] identityId;

    /**
     * The recovery token for the mobile api
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    final byte[] backupToken;

    /**
     * The bytes of the encoded {@link SignedDeviceIdentityHMAC} received during the auth process
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    SignedDeviceIdentity companionIdentity;

    /**
     * Sender keys for signal implementation
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final Map<SenderKeyName, SenderKeyRecord> senderKeys;

    /**
     * App state keys
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    final List<CompanionSyncKey> appStateKeys;

    /**
     * Sessions map
     */
    @ProtobufProperty(index = 20, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<SessionAddress, Session> sessions;

    /**
     * Hash state
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<String, CompanionHashState> hashStates;


    @ProtobufProperty(index = 22, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<Jid, SenderPreKeys> groupsPreKeys;

    /**
     * Whether the client was registered
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    boolean registered;

    /**
     * Whether the client has already sent its business certificate (mobile api only)
     */
    @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
    boolean businessCertificate;

    /**
     * Whether the client received the initial app sync (web api only)
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    boolean initialAppSync;

    /**
     * Write counter for IV
     */
    @JsonIgnore
    final AtomicLong writeCounter;

    /**
     * Read counter for IV
     */
    @JsonIgnore
    final AtomicLong readCounter;

    /**
     * Session dependent keys to write and read cyphered messages
     */
    @JsonIgnore
    byte[] writeKey, readKey;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Keys(UUID uuid, PhoneNumber phoneNumber, ClientType clientType, Collection<String> alias, Integer registrationId, SignalKeyPair noiseKeyPair, SignalKeyPair ephemeralKeyPair, SignalKeyPair identityKeyPair, SignalKeyPair companionKeyPair, SignalSignedKeyPair signedKeyPair, byte[] signedKeyIndex, Long signedKeyIndexTimestamp, List<SignalPreKeyPair> preKeys, String fdid, byte[] deviceId, UUID advertisingId, byte[] identityId, byte[] backupToken, SignedDeviceIdentity companionIdentity, Map<SenderKeyName, SenderKeyRecord> senderKeys, List<CompanionSyncKey> appStateKeys, ConcurrentMap<SessionAddress, Session> sessions, ConcurrentMap<String, CompanionHashState> hashStates, ConcurrentMap<Jid, SenderPreKeys> groupsPreKeys, boolean registered, boolean businessCertificate, boolean initialAppSync) {
        super(uuid, phoneNumber, null, clientType, alias);
        this.registrationId = Objects.requireNonNullElseGet(registrationId, () -> ThreadLocalRandom.current().nextInt(16380) + 1);
        this.noiseKeyPair = Objects.requireNonNull(noiseKeyPair, "Missing noise keypair");
        this.ephemeralKeyPair = Objects.requireNonNullElseGet(ephemeralKeyPair, SignalKeyPair::random);
        this.identityKeyPair = Objects.requireNonNull(identityKeyPair, "Missing identity keypair");
        this.companionKeyPair = Objects.requireNonNullElseGet(companionKeyPair, SignalKeyPair::random);
        this.signedKeyPair = Objects.requireNonNullElseGet(signedKeyPair, () -> SignalSignedKeyPair.of(this.registrationId, identityKeyPair));
        this.signedKeyIndex = signedKeyIndex;
        this.signedKeyIndexTimestamp = signedKeyIndexTimestamp;
        this.preKeys = Objects.requireNonNullElseGet(preKeys, ArrayList::new);
        this.fdid = Objects.requireNonNullElseGet(fdid, UUID.randomUUID()::toString);
        this.deviceId = Objects.requireNonNullElseGet(deviceId, () -> HexFormat.of().parseHex(UUID.randomUUID().toString().replaceAll("-", "")));
        this.advertisingId = Objects.requireNonNullElseGet(advertisingId, UUID::randomUUID);
        this.identityId = Objects.requireNonNull(identityId, "Missing identity id");
        this.backupToken = Objects.requireNonNullElseGet(backupToken, () -> Bytes.random(20));
        this.companionIdentity = companionIdentity;
        this.senderKeys = Objects.requireNonNullElseGet(senderKeys, ConcurrentHashMap::new);
        this.appStateKeys = Objects.requireNonNullElseGet(appStateKeys, ArrayList::new);
        this.sessions = Objects.requireNonNullElseGet(sessions, ConcurrentHashMap::new);
        this.hashStates = Objects.requireNonNullElseGet(hashStates, ConcurrentHashMap::new);
        this.groupsPreKeys = Objects.requireNonNullElseGet(groupsPreKeys, ConcurrentHashMap::new);
        this.registered = registered;
        this.businessCertificate = businessCertificate;
        this.initialAppSync = initialAppSync;
        this.writeCounter = new AtomicLong();
        this.readCounter = new AtomicLong();
    }

    public static Keys newKeys(UUID uuid, Long phoneNumber, Collection<String> alias, ClientType clientType) {
        return new KeysBuilder()
                .uuid(uuid)
                .phoneNumber(PhoneNumber.ofNullable(phoneNumber).orElse(null))
                .alias(alias)
                .clientType(clientType)
                .noiseKeyPair(SignalKeyPair.random())
                .identityKeyPair(SignalKeyPair.random())
                .identityId(Bytes.random(16))
                .build();
    }

    /**
     * Returns the encoded id
     *
     * @return a non-null byte array
     */
    public byte[] encodedRegistrationId() {
        return Bytes.intToBytes(registrationId(), 4);
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
        return appStateKeys.stream()
                .filter(preKey -> Objects.equals(preKey.companion(), jid))
                .map(CompanionSyncKey::keys)
                .flatMap(Collection::stream)
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
        return Optional.ofNullable(hashStates.get("%s_%s".formatted(device, patchType)));
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
        hashStates.put("%s_%s".formatted(device, state.type()), state);
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
        appStateKeys.stream()
                .filter(preKey -> Objects.equals(preKey.companion(), jid))
                .findFirst()
                .ifPresentOrElse(key -> key.keys().addAll(keys), () -> {
                    var syncKey = new CompanionSyncKey(jid, new LinkedList<>(keys));
                    appStateKeys.add(syncKey);
                });
        return this;
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public AppStateSyncKey getLatestAppKey(Jid jid) {
        return getAppKeys(jid).getLast();
    }

    /**
     * Get any available app key
     *
     * @return a non-null app key
     */
    public LinkedList<AppStateSyncKey> getAppKeys(Jid jid) {
        return appStateKeys.stream()
                .filter(preKey -> Objects.equals(preKey.companion(), jid))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing keys"))
                .keys();
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
    public synchronized long nextWriteCounter(boolean increment) {
        return increment ? writeCounter.getAndIncrement() : writeCounter.get();
    }

    /**
     * Returns read counter
     *
     * @param increment whether the counter should be incremented after the call
     * @return an unsigned long
     */
    public synchronized long nextReadCounter(boolean increment) {
        return increment ? readCounter.getAndIncrement() : readCounter.get();
    }

    /**
     * Returns the id of the last available pre key
     *
     * @return an integer
     */
    public int lastPreKeyId() {
        return preKeys.isEmpty() ? 0 : preKeys.getLast().id();
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
            preKeys.addPreKey(recipient);
            return;
        }

        var newPreKeys = new SenderPreKeys();
        newPreKeys.addPreKey(recipient);
        groupsPreKeys.put(group, newPreKeys);
    }

    public void addRecipientsWithPreKeys(Jid group, Collection<Jid> recipients) {
        var preKeys = groupsPreKeys.get(group);
        if (preKeys != null) {
            preKeys.addPreKeys(recipients);
            return;
        }

        var newPreKeys = new SenderPreKeys();
        newPreKeys.addPreKeys(recipients);
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

    public String fdid() {
        return this.fdid;
    }

    public byte[] deviceId() {
        return this.deviceId;
    }

    public UUID advertisingId() {
        return this.advertisingId;
    }

    public byte[] identityId() {
        return this.identityId;
    }

    public byte[] backupToken() {
        return backupToken;
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

    public void setSignedKeyPair(SignalSignedKeyPair signedKeyPair) {
        this.signedKeyPair = signedKeyPair;
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

    /**
     * Six part keys representation
     *
     * @return a string
     */
    @Override
    public String toString() {
        var cryptographicKeys = Stream.of(noiseKeyPair.publicKey(), noiseKeyPair.privateKey(), identityKeyPair.publicKey(), identityKeyPair.privateKey(), identityId())
                .map(Base64.getEncoder()::encodeToString)
                .collect(Collectors.joining(","));
        return phoneNumber()
                .map(phoneNumber -> phoneNumber + ","  + cryptographicKeys)
                .orElse(cryptographicKeys);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Keys keys &&
                registered == keys.registered &&
                businessCertificate == keys.businessCertificate &&
                initialAppSync == keys.initialAppSync &&
                Objects.equals(registrationId, keys.registrationId) &&
                Objects.equals(noiseKeyPair, keys.noiseKeyPair) &&
                Objects.equals(ephemeralKeyPair, keys.ephemeralKeyPair) &&
                Objects.equals(identityKeyPair, keys.identityKeyPair) &&
                Objects.equals(companionKeyPair, keys.companionKeyPair) &&
                Objects.equals(signedKeyPair, keys.signedKeyPair) &&
                Objects.deepEquals(signedKeyIndex, keys.signedKeyIndex) &&
                Objects.equals(signedKeyIndexTimestamp, keys.signedKeyIndexTimestamp) &&
                Objects.equals(preKeys, keys.preKeys) &&
                Objects.equals(fdid, keys.fdid) &&
                Objects.deepEquals(deviceId, keys.deviceId) &&
                Objects.equals(advertisingId, keys.advertisingId) &&
                Objects.deepEquals(identityId, keys.identityId) &&
                Objects.deepEquals(backupToken, keys.backupToken) &&
                Objects.equals(companionIdentity, keys.companionIdentity) &&
                Objects.equals(senderKeys, keys.senderKeys) &&
                Objects.equals(appStateKeys, keys.appStateKeys) &&
                Objects.equals(sessions, keys.sessions) &&
                Objects.equals(hashStates, keys.hashStates) &&
                Objects.equals(groupsPreKeys, keys.groupsPreKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationId, noiseKeyPair, ephemeralKeyPair, identityKeyPair, companionKeyPair, signedKeyPair, Arrays.hashCode(signedKeyIndex), signedKeyIndexTimestamp, preKeys, fdid, Arrays.hashCode(deviceId), advertisingId, Arrays.hashCode(identityId), Arrays.hashCode(backupToken), companionIdentity, senderKeys, appStateKeys, sessions, hashStates, groupsPreKeys, registered, businessCertificate, initialAppSync);
    }
}