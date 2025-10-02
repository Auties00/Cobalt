
package it.auties.whatsapp.controller;

import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalProtocolStore;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.groups.state.SignalSenderKeyRecord;
import com.github.auties00.libsignal.key.*;
import com.github.auties00.libsignal.state.SignalSessionRecord;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.model.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.sync.AppStateSyncHash;
import it.auties.whatsapp.model.sync.AppStateSyncKey;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Scalar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Controller that manages all cryptographic keys and Signal protocol state for a WhatsApp session.
 * <p>
 * This class serves as the cryptographic foundation for WhatsApp communication, implementing the
 * {@link SignalProtocolStore} interface to provide secure end-to-end encryption. It manages several
 * categories of cryptographic material:
 * <ul>
 *     <li><b>Identity Keys:</b> Core key pairs for establishing secure connections and message encryption</li>
 *     <li><b>Signal Protocol Keys:</b> Pre-keys, signed keys, and session keys for the Signal protocol</li>
 *     <li><b>Device Identifiers:</b> Unique identifiers for mobile device authentication</li>
 *     <li><b>App State Sync Keys:</b> Keys for synchronizing application state across devices (Web clients)</li>
 *     <li><b>Session State:</b> Active Signal protocol sessions and sender keys for group communication</li>
 * </ul>
 * <p>
 * The Keys controller works in tandem with {@link WhatsappStore} to provide complete session management.
 * While Store manages user data, chats, and contacts, Keys handles all security and encryption aspects.
 * <p>
 * <b>Thread Safety:</b> This class uses concurrent collections for session and key management, making it
 * safe for concurrent access in multi-threaded environments. However, methods that modify cryptographic
 * state should be carefully coordinated to avoid race conditions.
 * <p>
 * <b>Persistence:</b> All cryptographic keys are serialized to secure storage when {@link #serialize()}
 * is called. This allows sessions to be restored across application restarts without requiring
 * re-authentication.
 * <p>
 * <b>Client Types:</b> The behavior differs slightly between Web and Mobile clients:
 * <ul>
 *     <li><b>Web:</b> Uses QR code authentication, maintains companion device identity</li>
 *     <li><b>Mobile:</b> Uses phone number authentication, includes additional mobile-specific identifiers</li>
 * </ul>
 *
 * @see WhatsappStore
 * @see Controller
 * @see SignalProtocolStore
 * @see WhatsappClientType
 */
@ProtobufMessage
public final class Keys extends Controller implements SignalProtocolStore {
    //region Core Identity Fields

    /**
     * The Signal protocol registration ID for this client.
     * <p>
     * This is a randomly generated identifier used to distinguish different installations
     * of the client. It's used during the Signal protocol handshake and pre-key exchange.
     * <p>
     * The registration ID is a random integer between 1 and 16380, generated during
     * initial client setup if not provided.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    final Integer registrationId;

    /**
     * The Noise protocol key pair used for secure channel establishment.
     * <p>
     * This key pair is used during the initial Noise XX handshake with WhatsApp servers
     * to establish an encrypted communication channel. It's generated once during account
     * creation and remains constant for the lifetime of the account.
     * <p>
     * The Noise protocol provides forward secrecy and ensures that the initial connection
     * to WhatsApp servers is secure before any sensitive data is exchanged.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final SignalIdentityKeyPair noiseKeyPair;

    /**
     * The ephemeral key pair used during authentication handshake.
     * <p>
     * This key pair is temporary and used only during the connection establishment phase.
     * It provides additional security by ensuring that even if long-term keys are compromised,
     * individual sessions remain secure.
     * <p>
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final SignalIdentityKeyPair ephemeralKeyPair;

    /**
     * The Signal protocol identity key pair for end-to-end encryption.
     * <p>
     * This is the primary key pair used for the Signal protocol implementation that provides
     * end-to-end encryption for all messages.
     * <p>
     * The identity key pair is generated once during account creation and should remain constant.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final SignalIdentityKeyPair identityKeyPair;

    /**
     * The companion device key pair (Web clients only).
     * <p>
     * For Web clients, this key pair identifies the linked device and is used to
     * establish trust with the primary mobile device. It's exchanged during the QR code/Pairing code
     * pairing process.
     * <p>
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    SignalIdentityKeyPair companionKeyPair;

    /**
     * The companion device identity information (Web clients only).
     * <p>
     * Contains the signed identity information received from the primary mobile deviceduring the pairing process.
     * <p>
     * This value is null until the QR code/Pairing code pairing is completed and identity information
     * is synchronized from the mobile device.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    SignedDeviceIdentity companionIdentity;

    //endregion

    //region Signal Protocol Key Fields

    /**
     * The currently active signed pre-key pair.
     * <p>
     * Signed pre-keys are used in the Signal protocol's X3DH (Extended Triple Diffie-Hellman) key agreement protocol.
     * This key pair is generated during initialization and should be rotated according to WhatsApp's security policies.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    SignalSignedKeyPair signedKeyPair;

    /**
     * The index/identifier of the signed key from the companion device.
     * <p>
     * This byte array uniquely identifies which signed pre-key the companion (mobile) device
     * is currently using. It's synchronized from the mobile device and used to coordinate
     * key rotation across linked devices.
     * <p>
     * This value is null until synchronized by WhatsApp after initial pairing.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    byte[] signedKeyIndex;

    /**
     * The timestamp when the companion device's signed key was generated or last rotated.
     * <p>
     * This timestamp is synchronized from the mobile device and helps coordinate key
     * rotation policies across all linked devices. It's measured in milliseconds since
     * the Unix epoch.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    Long signedKeyIndexTimestamp;

    /**
     * The collection of one-time pre-keys available for new session establishment.
     * <p>
     * Pre-keys are used in the Signal protocol's asynchronous key agreement.
     * <p>
     * When the supply of pre-keys runs low, new batches should be generated and uploaded
     * to ensure users can always initiate new encrypted sessions.
     * <p>
     * The keys are stored in a LinkedHashMap to preserve insertion order, which is important
     * for key rotation and management.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    final LinkedHashMap<Integer, SignalPreKeyPair> preKeys;

    //endregion

    //region Mobile Device Identifier Fields

    /**
     * This is a unique identifier used by WhatsApp's mobile clients during registration.
     * For Web clients, this field is still generated but may not be actively used.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    final String fdid;

    /**
     * The unique device identifier for mobile clients.
     * <p>
     * This byte array contains a device-specific identifier used by WhatsApp to track
     * individual installations. It's generated as a UUID without hyphens, then converted
     * to bytes via hex decoding.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BYTES)
    final byte[] deviceId;

    /**
     * The advertising identifier for mobile clients.
     * <p>
     * This UUID is used for analytics and advertising purposes on mobile platforms.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    final UUID advertisingId;

    /**
     * The unique identity identifier for this WhatsApp account.
     * <p>
     * This is a cryptographically random byte array that uniquely identifies this specific
     * account installation. It's different from the JID (WhatsApp user ID) and provides an
     * additional layer of identity verification.
     * <p>
     * The identity ID is generated once during account creation and remains constant. It's
     * used in various cryptographic protocols and authentication flows.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    final byte[] identityId;

    /**
     * The backup/recovery token for mobile clients.
     * <p>
     * This token is used to authenticate backup and restore operations, allowing users to
     * recover their chat history when reinstalling the app or switching devices. It's a
     * 20-byte random value that should be kept secure.
     * <p>
     * The backup token is cryptographically tied to the account and encrypted backups
     * cannot be accessed without it.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    final byte[] backupToken;

    //endregion

    //region Session & Sync State Fields

    /**
     * The collection of Signal protocol sender keys for group messaging.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeys;

    /**
     * The collection of app state synchronization keys (Web clients only).
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    final LinkedHashMap<Integer, AppStateSyncKey> appStateKeys;

    /**
     * The collection of active Signal protocol sessions.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessions;

    /**
     * The collection of app state sync hash states (Web clients only).
     * <p>
     * Hash states track the synchronization status of different types of application state.
     * Each {@link PatchType} has an associated {@link AppStateSyncHash} that:
     * <ul>
     *     <li>Identifies the current version of that state type</li>
     *     <li>Allows detection of changes from the mobile device</li>
     *     <li>Enables efficient differential synchronization</li>
     * </ul>
     * <p>
     * Examples of patch types include: contacts, chat settings, starred messages, etc.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE, mapKeyType = ProtobufType.ENUM, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<PatchType, AppStateSyncHash> hashStates;

    //endregion

    //region Registration State Fields

    /**
     * Flag indicating whether this client has completed registration with WhatsApp.
     * <p>
     * For Web clients, registration is complete after successful QR code/Pairing code pairing.
     * For Mobile clients, registration is complete after phone number verification.
     * <p>
     * This flag affects which API calls are available and how the client behaves during
     * connection establishment.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    boolean registered;

    /**
     * Flag indicating whether the business certificate has been sent (Mobile API only).
     * <p>
     * For WhatsApp Business accounts, a business certificate must be sent during the
     * initial registration process. This flag tracks whether that certificate has
     * already been transmitted to avoid duplicate submissions.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
    boolean businessCertificate;

    //endregion

    //region Constructor

    Keys(UUID uuid, long phoneNumber, WhatsappClientType clientType, Integer registrationId, SignalIdentityKeyPair noiseKeyPair, SignalIdentityKeyPair ephemeralKeyPair, SignalIdentityKeyPair identityKeyPair, SignalIdentityKeyPair companionKeyPair, SignalSignedKeyPair signedKeyPair, byte[] signedKeyIndex, Long signedKeyIndexTimestamp, LinkedHashMap<Integer, SignalPreKeyPair> preKeys, String fdid, byte[] deviceId, UUID advertisingId, byte[] identityId, byte[] backupToken, SignedDeviceIdentity companionIdentity, ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeys, LinkedHashMap<Integer, AppStateSyncKey> appStateKeys, ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessions, ConcurrentMap<PatchType, AppStateSyncHash> hashStates, boolean registered, boolean businessCertificate) {
        super(uuid, phoneNumber, null, clientType);
        this.registrationId = Objects.requireNonNullElseGet(registrationId, () -> ThreadLocalRandom.current().nextInt(16380) + 1);
        this.noiseKeyPair = Objects.requireNonNull(noiseKeyPair, "Missing noise keypair");
        this.ephemeralKeyPair = Objects.requireNonNullElseGet(ephemeralKeyPair, SignalIdentityKeyPair::random);
        this.identityKeyPair = Objects.requireNonNull(identityKeyPair, "Missing identity keypair");
        this.companionKeyPair = Objects.requireNonNullElseGet(companionKeyPair, SignalIdentityKeyPair::random);
        this.signedKeyPair = Objects.requireNonNullElseGet(signedKeyPair, () -> SignalSignedKeyPair.of(this.registrationId, identityKeyPair));
        this.signedKeyIndex = signedKeyIndex;
        this.signedKeyIndexTimestamp = signedKeyIndexTimestamp;
        this.preKeys = Objects.requireNonNullElseGet(preKeys, LinkedHashMap::new);
        this.fdid = Objects.requireNonNullElseGet(fdid, UUID.randomUUID()::toString);
        this.deviceId = Objects.requireNonNullElseGet(deviceId, () -> HexFormat.of().parseHex(UUID.randomUUID().toString().replaceAll("-", "")));
        this.advertisingId = Objects.requireNonNullElseGet(advertisingId, UUID::randomUUID);
        this.identityId = Objects.requireNonNull(identityId, "Missing identity id");
        this.backupToken = Objects.requireNonNullElseGet(backupToken, () -> Bytes.random(20));
        this.companionIdentity = companionIdentity;
        this.senderKeys = Objects.requireNonNullElseGet(senderKeys, ConcurrentHashMap::new);
        this.appStateKeys = Objects.requireNonNullElseGet(appStateKeys, LinkedHashMap::new);
        this.sessions = Objects.requireNonNullElseGet(sessions, ConcurrentHashMap::new);
        this.hashStates = Objects.requireNonNullElseGet(hashStates, ConcurrentHashMap::new);
        this.registered = registered;
        this.businessCertificate = businessCertificate;
    }

    /**
     * Creates a new Keys instance with default cryptographic material.
     * <p>
     * This factory method generates all necessary cryptographic keys and identifiers
     * for a new WhatsApp session:
     * <ul>
     *     <li>Random registration ID</li>
     *     <li>Fresh noise and identity key pairs</li>
     *     <li>Random identity ID</li>
     *     <li>Device identifiers for mobile clients</li>
     * </ul>
     * <p>
     * This is typically the first step in creating a new WhatsApp session before
     * authentication is performed.
     *
     * @param uuid        the unique identifier for this session, must not be null
     * @param phoneNumber the phone number for this account, may be null for web clients
     * @param clientType  the type of client (WEB or MOBILE), must not be null
     * @return a new Keys instance with freshly generated cryptographic material
     */
    public static Keys of(UUID uuid, Long phoneNumber, WhatsappClientType clientType) {
        return new KeysBuilder()
                .uuid(uuid)
                .phoneNumber(phoneNumber)
                .clientType(clientType)
                .noiseKeyPair(SignalIdentityKeyPair.random())
                .identityKeyPair(SignalIdentityKeyPair.random())
                .identityId(Bytes.random(16))
                .build();
    }

    //endregion

    //region Core Identity Accessors

    /**
     * Returns the Signal protocol registration ID.
     *
     * @return the registration ID, a value between 1 and 16380
     */
    public int registrationId() {
        return this.registrationId;
    }

    /**
     * Returns the registration ID encoded as a 4-byte array.
     * <p>
     * This encoding is used in various protocol messages where the registration ID
     * must be transmitted as binary data.
     *
     * @return a non-null 4-byte array containing the registration ID in big-endian format
     */
    public byte[] encodedRegistrationId() {
        return Scalar.intToBytes(registrationId(), 4);
    }

    /**
     * Returns the Noise protocol key pair.
     *
     * @return the non-null noise key pair used for secure channel establishment
     */
    public SignalIdentityKeyPair noiseKeyPair() {
        return this.noiseKeyPair;
    }

    /**
     * Returns the ephemeral key pair.
     *
     * @return the non-null ephemeral key pair used during handshake
     */
    public SignalIdentityKeyPair ephemeralKeyPair() {
        return this.ephemeralKeyPair;
    }

    /**
     * Returns the Signal protocol identity key pair.
     *
     * @return the non-null identity key pair used for end-to-end encryption
     */
    public SignalIdentityKeyPair identityKeyPair() {
        return this.identityKeyPair;
    }

    /**
     * Returns the companion device key pair.
     *
     * @return the non-null companion key pair
     */
    public SignalIdentityKeyPair companionKeyPair() {
        return this.companionKeyPair;
    }

    /**
     * Sets the companion device key pair.
     * <p>
     * This is typically called during the pairing process when establishing a
     * Web/Desktop client connection to a mobile device.
     *
     * @param companionKeyPair the new companion key pair, must not be null
     */
    public void setCompanionKeyPair(SignalIdentityKeyPair companionKeyPair) {
        this.companionKeyPair = companionKeyPair;
    }

    /**
     * Returns the companion device identity information.
     * <p>
     * Only available for Web clients after successful pairing with a mobile device.
     *
     * @return an Optional containing the companion identity if available, empty otherwise
     */
    public Optional<SignedDeviceIdentity> setCompanionIdentity() {
        return Optional.ofNullable(companionIdentity);
    }

    /**
     * Sets the companion device identity information.
     * <p>
     * Called during the pairing process when identity information is received
     * from the primary mobile device.
     *
     * @param companionIdentity the signed device identity from the mobile device
     */
    public void setCompanionIdentity(SignedDeviceIdentity companionIdentity) {
        this.companionIdentity = companionIdentity;
    }

    /**
     * Returns the unique identity identifier for this account.
     *
     * @return the non-null identity ID as a byte array
     */
    public byte[] identityId() {
        return this.identityId;
    }

    /**
     * Returns the backup/recovery token.
     *
     * @return the non-null backup token as a byte array
     */
    public byte[] backupToken() {
        return backupToken;
    }

    //endregion

    //region Signal Protocol Key Accessors

    /**
     * Returns the currently active signed pre-key pair.
     *
     * @return the non-null signed key pair
     */
    public SignalSignedKeyPair signedKeyPair() {
        return this.signedKeyPair;
    }

    /**
     * Sets the signed pre-key pair.
     * <p>
     * This is typically called when rotating the signed pre-key according to
     * WhatsApp's security policies.
     *
     * @param signedKeyPair the new signed key pair, must not be null
     */
    public void setSignedKeyPair(SignalSignedKeyPair signedKeyPair) {
        this.signedKeyPair = signedKeyPair;
    }

    /**
     * Returns the signed key index from the companion device.
     *
     * @return an Optional containing the signed key index if available, empty otherwise
     */
    public Optional<byte[]> signedKeyIndex() {
        return Optional.ofNullable(signedKeyIndex);
    }

    /**
     * Sets the signed key index.
     *
     * @param signedKeyIndex the signed key index from the companion device
     */
    public void setSignedKeyIndex(byte[] signedKeyIndex) {
        this.signedKeyIndex = signedKeyIndex;
    }

    /**
     * Returns the timestamp when the companion's signed key was generated.
     *
     * @return an OptionalLong containing the timestamp in milliseconds if available, empty otherwise
     */
    public OptionalLong signedKeyIndexTimestamp() {
        return Clock.parseTimestamp(signedKeyIndexTimestamp);
    }

    /**
     * Sets the signed key index timestamp.
     *
     * @param signedKeyIndexTimestamp the timestamp in milliseconds
     */
    public void setSignedKeyIndexTimestamp(Long signedKeyIndexTimestamp) {
        this.signedKeyIndexTimestamp = signedKeyIndexTimestamp;
    }

    /**
     * Returns all registered pre-keys in the order they were added.
     *
     * @return a non-null sequenced collection of pre-key pairs
     */
    public SequencedCollection<SignalPreKeyPair> preKeys() {
        return preKeys.sequencedValues();
    }

    /**
     * Checks whether any pre-keys are currently available.
     * <p>
     * If this returns false, new pre-keys should be generated and uploaded to
     * WhatsApp servers to allow new sessions to be established.
     *
     * @return true if pre-keys are available, false otherwise
     */
    public boolean hasPreKeys() {
        return !preKeys.isEmpty();
    }

    /**
     * Finds a pre-key by its ID.
     *
     * @param id the pre-key ID to search for, may be null
     * @return an Optional containing the pre-key if found, empty otherwise
     */
    public Optional<SignalPreKeyPair> findPreKeyById(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(preKeys.get(id));
    }

    /**
     * Adds a pre-key to the collection.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface implementation.
     *
     * @param preKey the pre-key to add, must not be null
     * @throws NullPointerException if preKey is null
     */
    @Override
    public void addPreKey(SignalPreKeyPair preKey) {
        Objects.requireNonNull(preKey, "preKey cannot be null");
        preKeys.put(preKey.id(), preKey);
    }

    /**
     * Removes a pre-key from the collection.
     * <p>
     * Pre-keys are typically removed after being used to establish a new session,
     * ensuring they are only used once.
     *
     * @param id the ID of the pre-key to remove
     * @return true if a pre-key was removed, false if no pre-key with that ID existed
     */
    @Override
    public boolean removePreKey(int id) {
        return preKeys.remove(id) != null;
    }

    /**
     * Finds the signed pre-key by its ID.
     * <p>
     * Currently, only one signed pre-key is maintained at a time, so this method
     * only returns the current signed key pair if the ID matches.
     *
     * @param id the signed pre-key ID to search for
     * @return an Optional containing the signed pre-key if the ID matches, empty otherwise
     */
    @Override
    public Optional<SignalSignedKeyPair> findSignedPreKeyById(Integer id) {
        return id == signedKeyPair.id() ? Optional.of(signedKeyPair) : Optional.empty();
    }

    /**
     * Adds a signed pre-key.
     * <p>
     * This operation is not supported as signed pre-keys are managed through
     * {@link #setSignedKeyPair(SignalSignedKeyPair)} instead.
     *
     * @param signalSignedKeyPair the signed pre-key to add
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public void addSignedPreKey(SignalSignedKeyPair signalSignedKeyPair) {
        throw new UnsupportedOperationException("Cannot add signed pre keys to a Keys instance");
    }

    //endregion

    //region Mobile Device Identifier Accessors

    /**
     * Returns the Firebase Device ID (FDID).
     *
     * @return the non-null FDID as a UUID string
     */
    public String fdid() {
        return this.fdid;
    }

    /**
     * Returns the device identifier.
     *
     * @return the non-null device ID as a byte array
     */
    public byte[] deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the advertising identifier.
     *
     * @return the non-null advertising ID as a UUID
     */
    public UUID advertisingId() {
        return this.advertisingId;
    }

    //endregion

    //region Session Management

    /**
     * Finds a Signal protocol session by address.
     *
     * @param address the address to search for, must not be null
     * @return an Optional containing the session if found, empty otherwise
     */
    @Override
    public Optional<SignalSessionRecord> findSessionByAddress(SignalProtocolAddress address) {
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Checks whether a session exists for the given address.
     *
     * @param address the address to check, must not be null
     * @return true if a session exists, false otherwise
     */
    public boolean hasSession(SignalProtocolAddress address) {
        return sessions.containsKey(address);
    }

    /**
     * Adds or updates a Signal protocol session.
     *
     * @param address the address for this session, must not be null
     * @param record  the session record, must not be null
     */
    public void addSession(SignalProtocolAddress address, SignalSessionRecord record) {
        sessions.put(address, record);
    }

    //endregion

    //region Sender Key Management (Group Messaging)

    /**
     * Finds a sender key by name for group messaging.
     *
     * @param name the sender key name (group + sender + device), must not be null
     * @return an Optional containing the sender key record if found, empty otherwise
     */
    @Override
    public Optional<SignalSenderKeyRecord> findSenderKeyByName(SignalSenderKeyName name) {
        return Optional.ofNullable(senderKeys.get(name));
    }

    /**
     * Adds or updates a sender key for group messaging.
     *
     * @param name      the sender key name, must not be null
     * @param newRecord the sender key record, must not be null
     */
    @Override
    public void addSenderKey(SignalSenderKeyName name, SignalSenderKeyRecord newRecord) {
        senderKeys.put(name, newRecord);
    }

    //endregion

    //region App State Sync (Web Clients)

    /**
     * Retrieves a sequenced collection of web app state keys.
     *
     * @return an unmodifiable sequenced collection
     */
    public SequencedCollection<AppStateSyncKey> appStateKeys() {
        return Collections.unmodifiableSequencedCollection(appStateKeys.sequencedValues());
    }

    /**
     * Finds an app state sync key by its key ID.
     *
     * @param id the key ID to search for, must not be null
     * @return an Optional containing the app state sync key if found, empty otherwise
     */
    public Optional<AppStateSyncKey> findWebAppStateKeyById(byte[] id) {
        return Optional.ofNullable(appStateKeys.get(Arrays.hashCode(id)));
    }

    /**
     * Adds multiple app state sync keys to the collection.
     * <p>
     * Keys without a valid key ID are silently skipped.
     *
     * @param keys the collection of keys to add, must not be null
     */
    public void addWebAppStateKeys(Collection<AppStateSyncKey> keys) {
        for (var key : keys) {
            var keyId = key.keyId();
            if(keyId == null) {
                continue;
            }

            var keyIdValue = keyId.value();
            if(keyIdValue == null) {
                continue;
            }

            appStateKeys.put(Arrays.hashCode(keyIdValue), key);
        }
    }

    /**
     * Finds a hash state by patch type.
     *
     * @param patchType the type of app state to query, must not be null
     * @return an Optional containing the hash state if found, empty otherwise
     */
    public Optional<AppStateSyncHash> findWebAppHashStateByName(PatchType patchType) {
        return Optional.ofNullable(hashStates.get(patchType));
    }

    /**
     * Adds or updates a hash state for app state synchronization.
     *
     * @param state the hash state to add, must not be null
     */
    public void addWebAppHashState(AppStateSyncHash state) {
        hashStates.put(state.type(), state);
    }

    //endregion

    //region Registration State

    /**
     * Returns whether the client has completed registration.
     *
     * @return true if registered, false otherwise
     */
    public boolean registered() {
        return this.registered;
    }

    /**
     * Sets the registration status.
     *
     * @param registered true if the client is now registered, false otherwise
     */
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /**
     * Returns whether the business certificate has been sent (Mobile API only).
     *
     * @return true if the certificate has been sent, false otherwise
     */
    public boolean businessCertificate() {
        return this.businessCertificate;
    }

    /**
     * Sets whether the business certificate has been sent.
     *
     * @param businessCertificate true if the certificate has been sent, false otherwise
     */
    public void setBusinessCertificate(boolean businessCertificate) {
        this.businessCertificate = businessCertificate;
    }

    //endregion

    //region Signal Protocol Store Implementation

    /**
     * Checks if the given identity is trusted for the specified address.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface. Currently returns
     * false as trust verification is handled elsewhere.
     *
     * @param signalProtocolAddress       the address to check
     * @param signalIdentityPublicKey     the identity key to verify
     * @param signalKeyDirection          the direction of the key (sending or receiving)
     * @return false (trust verification not implemented here)
     */
    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey, SignalKeyDirection signalKeyDirection) {
        return false;
    }

    /**
     * Adds a trusted identity for the specified address.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface. Currently a no-op
     * as trust management is handled elsewhere.
     *
     * @param signalProtocolAddress       the address
     * @param signalIdentityPublicKey     the identity key to trust
     */
    @Override
    public void addTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey) {

    }

    //endregion

    //region Persistence & Lifecycle

    /**
     * Disposes of this Keys controller and triggers serialization.
     * <p>
     * This method is called when the session is being closed. It ensures that
     * all cryptographic keys and state are persisted before the controller is
     * discarded.
     */
    @Override
    public void dispose() {
        serialize();
    }

    /**
     * Serializes this Keys controller to persistent storage.
     * <p>
     * This method saves all cryptographic keys, sessions, and state to storage
     * using the configured {@link WhatsappStoreSerializer}. Only performs serialization
     * if {@link #serializable} is true.
     */
    @Override
    public void serialize() {
        if(serializable) {
            serializer.serializeKeys(this);
        }
    }

    //endregion

    //region Standard Object Methods

    /**
     * Returns a six-part string representation of the keys.
     * <p>
     * This format is compatible with {@link it.auties.whatsapp.api.WhatsappSixPartsKeys}
     * and contains:
     * <ol>
     *     <li>Phone number</li>
     *     <li>Noise public key (Base64)</li>
     *     <li>Noise private key (Base64)</li>
     *     <li>Identity public key (Base64)</li>
     *     <li>Identity private key (Base64)</li>
     *     <li>Identity ID (Base64)</li>
     * </ol>
     *
     * @return a comma-separated string representation of the keys
     */
    @Override
    public String toString() {
        return phoneNumber +
                "," +
                Base64.getEncoder().encodeToString(noiseKeyPair.publicKey().toEncodedPoint()) +
                "," +
                Base64.getEncoder().encodeToString(noiseKeyPair.privateKey().toEncodedPoint()) +
                "," +
                Base64.getEncoder().encodeToString(identityKeyPair.publicKey().toEncodedPoint()) +
                "," +
                Base64.getEncoder().encodeToString(identityKeyPair.privateKey().toEncodedPoint()) +
                "," +
                Base64.getEncoder().encodeToString(identityId());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Keys keys &&
                registered == keys.registered &&
                businessCertificate == keys.businessCertificate &&
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
                Objects.equals(hashStates, keys.hashStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationId, noiseKeyPair, ephemeralKeyPair, identityKeyPair, companionKeyPair, signedKeyPair, Arrays.hashCode(signedKeyIndex), signedKeyIndexTimestamp, preKeys, fdid, Arrays.hashCode(deviceId), advertisingId, Arrays.hashCode(identityId), Arrays.hashCode(backupToken), companionIdentity, senderKeys, appStateKeys, sessions, hashStates, registered, businessCertificate);
    }

    //endregion
}