package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentity;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.groups.state.SignalSenderKeyRecord;
import com.github.auties00.libsignal.key.*;
import com.github.auties00.libsignal.state.SignalSessionRecord;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed {@link LinkedWhatsAppSignalStore} holding this session's Signal-protocol cryptographic state.
 *
 * <p>This is a nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore}; it owns the
 * long-lived key material, the per-recipient sessions and per-group sender keys, the
 * trust-on-first-use remote-identity table, the ADV companion-pairing credential and the X3DH
 * base-key replay-dedup table, alongside the transient bookkeeping (sender-key rotation set,
 * identity-encryption range, sender-key distribution tracking and unconfirmed identity changes)
 * that the encryption pipeline rebuilds at runtime.
 *
 * @implNote
 * This implementation performs all cryptographic defaulting in its constructor (registration id,
 * Noise and identity key pairs, signed pre-key, device/identity/backup byte buffers) so a freshly
 * built instance is immediately a usable {@link com.github.auties00.libsignal.SignalProtocolStore};
 * on deserialization the persisted values are present and the defaulting is a no-op. The self-address
 * special case of {@code findIdentityByAddress} is handled by {@link LinkedWhatsAppStore} because it also
 * needs the account JID, which lives in a different sub-store.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ProtobufLinkedWhatsAppSignalStore implements LinkedWhatsAppSignalStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppSignalStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppSignalStore.class);

    /**
     * The Signal registration id published in every pre-key bundle so peers can tell apart
     * different installs of the same account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    private final Integer registrationId;

    /**
     * The long-lived Noise XX static key pair used for the outer transport handshake.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final SignalIdentityKeyPair noiseKeyPair;

    /**
     * The long-lived Signal identity key pair used to sign pre-key bundles and derive sessions.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final SignalIdentityKeyPair identityKeyPair;

    /**
     * The ADV certificate proving this companion was authorised by the primary device.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private ADVSignedDeviceIdentity signedDeviceIdentity;

    /**
     * The currently published signed pre-key, with its server id and ECDSA signature.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final SignalSignedKeyPair signedKeyPair;

    /**
     * The map of one-time pre-keys keyed by server-assigned id, insertion-ordered.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    private final LinkedHashMap<Integer, SignalPreKeyPair> preKeysMap;

    /**
     * The Facebook device identifier bundled into client payloads for cross-Meta correlation.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final UUID fdid;

    /**
     * The 16-byte stable device identifier announced during pairing.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    private final byte[] deviceId;

    /**
     * The advertising identifier surfaced by the host platform.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    private final UUID advertisingId;

    /**
     * The 16-byte randomly generated identity buffer seeding deterministic per-account derivations.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BYTES)
    private final byte[] identityId;

    /**
     * The 20-byte token announced to the server as the backup credential for encrypted-history recovery.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    private final byte[] backupToken;

    /**
     * The map of Signal group-session sender keys keyed by the (group id, sender device) pair.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeysMap;

    /**
     * The map of Signal sessions for each remote (user, device) pair.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessionsMap;

    /**
     * The map of trusted remote Signal identity keys, keyed by (user, device).
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.BYTES)
    private final ConcurrentMap<SignalProtocolAddress, SignalIdentityPublicKey> remoteIdentitiesMap;

    /**
     * The shared secret used to derive the ADV MAC key on this companion.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BYTES)
    private byte[] advSecretKey;

    /**
     * The X3DH base-key dedup table keyed by {@code address|originalMsgId}.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.BYTES)
    private final ConcurrentMap<String, byte[]> baseKeysMap;

    /**
     * The per-remote-device counter tracking the last Signal-encrypted message sequence number
     * observed in flight; not persisted because it is reconstructed from session state on reload.
     */
    private final ConcurrentMap<SignalProtocolAddress, Long> identityEncryptionRange;

    /**
     * The monotonic counter providing the per-process sequence component of identity-range tracking.
     */
    private final AtomicLong encryptionSequence;

    /**
     * The set of user JIDs for which the local device must rotate and reshare the Signal sender key
     * on the next outbound group message.
     */
    private final KeySetView<Jid, Boolean> usersNeedingSenderKeyRotation;

    /**
     * The set of remote JIDs whose Signal identity key changed since last accepted and which the
     * user has not yet confirmed.
     */
    private final Set<Jid> unconfirmedIdentityChanges;

    /**
     * The map of group id to the set of device ids already issued the current sender-key distribution.
     */
    private final ConcurrentMap<String, Set<String>> groupSenderKeyDistribution;

    /**
     * The account sub-store consulted for the self-address case of {@link #findIdentityByAddress};
     * wired post-construction.
     */
    private LinkedWhatsAppAccountStore account;

    /**
     * Constructs a Signal sub-store, defaulting any absent cryptographic material.
     *
     * @implNote
     * This implementation generates a random registration id, Noise key pair, identity key pair,
     * signed pre-key, FDID, device id, advertising id, identity id and backup token when the
     * corresponding argument is {@code null}; the pre-key, sender-key and session maps are required
     * non-null, while the remote-identity and base-key maps default to empty concurrent maps. The
     * five transient collections are always freshly allocated.
     *
     * @param registrationId      the Signal registration id, or {@code null} to generate one
     * @param noiseKeyPair        the Noise XX key pair, or {@code null} to generate one
     * @param identityKeyPair     the Signal identity key pair, or {@code null} to generate one
     * @param signedDeviceIdentity the ADV signed device identity, or {@code null}
     * @param signedKeyPair       the signed pre-key pair, or {@code null} to derive one
     * @param preKeysMap          the one-time pre-key map, never {@code null}
     * @param fdid                the Facebook device id, or {@code null} to generate one
     * @param deviceId            the device id bytes, or {@code null} to generate them
     * @param advertisingId       the advertising id, or {@code null} to generate one
     * @param identityId          the identity id bytes, or {@code null} to generate them
     * @param backupToken         the backup token bytes, or {@code null} to generate them
     * @param senderKeysMap       the sender-key map, never {@code null}
     * @param sessionsMap         the session map, never {@code null}
     * @param remoteIdentitiesMap the remote-identity map, or {@code null} for an empty map
     * @param advSecretKey        the ADV secret key bytes, or {@code null}
     * @param baseKeysMap         the base-key dedup map, or {@code null} for an empty map
     */
    ProtobufLinkedWhatsAppSignalStore(Integer registrationId, SignalIdentityKeyPair noiseKeyPair, SignalIdentityKeyPair identityKeyPair, ADVSignedDeviceIdentity signedDeviceIdentity, SignalSignedKeyPair signedKeyPair, LinkedHashMap<Integer, SignalPreKeyPair> preKeysMap, UUID fdid, byte[] deviceId, UUID advertisingId, byte[] identityId, byte[] backupToken, ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeysMap, ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessionsMap, ConcurrentMap<SignalProtocolAddress, SignalIdentityPublicKey> remoteIdentitiesMap, byte[] advSecretKey, ConcurrentMap<String, byte[]> baseKeysMap) {
        this.registrationId = requireNonNullElseGet(registrationId, () -> DataUtils.randomInt(16380) + 1);
        this.noiseKeyPair = requireNonNullElseGet(noiseKeyPair, SignalIdentityKeyPair::random);
        this.identityKeyPair = requireNonNullElseGet(identityKeyPair, SignalIdentityKeyPair::random);
        this.signedDeviceIdentity = signedDeviceIdentity;
        this.signedKeyPair = requireNonNullElseGet(signedKeyPair, () -> SignalSignedKeyPair.of(this.registrationId, this.identityKeyPair));
        this.preKeysMap = Objects.requireNonNull(preKeysMap, "preKeysMap cannot be null");
        this.fdid = requireNonNullElseGet(fdid, UUID::randomUUID);
        this.deviceId = requireNonNullElseGet(deviceId, () -> HexFormat.of().parseHex(UUID.randomUUID().toString().replace("-", "")));
        this.advertisingId = requireNonNullElseGet(advertisingId, UUID::randomUUID);
        this.identityId = requireNonNullElseGet(identityId, () -> DataUtils.randomByteArray(16));
        this.backupToken = requireNonNullElseGet(backupToken, () -> DataUtils.randomByteArray(20));
        this.senderKeysMap = Objects.requireNonNull(senderKeysMap, "senderKeysMap cannot be null");
        this.sessionsMap = Objects.requireNonNull(sessionsMap, "sessionsMap cannot be null");
        this.remoteIdentitiesMap = requireNonNullElseGet(remoteIdentitiesMap, ConcurrentHashMap::new);
        this.advSecretKey = advSecretKey;
        this.baseKeysMap = requireNonNullElseGet(baseKeysMap, ConcurrentHashMap::new);
        this.identityEncryptionRange = new ConcurrentHashMap<>();
        this.encryptionSequence = new AtomicLong();
        this.usersNeedingSenderKeyRotation = ConcurrentHashMap.newKeySet();
        this.unconfirmedIdentityChanges = ConcurrentHashMap.newKeySet();
        this.groupSenderKeyDistribution = new ConcurrentHashMap<>();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "signal store initialized, preKeys={0}, sessions={1}, senderKeys={2}, hasDeviceIdentity={3}",
                    this.preKeysMap.size(), this.sessionsMap.size(), this.senderKeysMap.size(), this.signedDeviceIdentity != null);
        }
    }

    /**
     * Sets the account sub-store used for the self-address case of {@link #findIdentityByAddress}
     * and for the LID canonicalisation of {@link #canonicalAddress(SignalProtocolAddress)}.
     *
     * <p>The account sub-store cannot be a constructor argument because the protobuf deserializer builds
     * this sub-store from its own wire fields alone; it is set once by the {@link ProtobufWhatsAppStore}
     * aggregate constructor, which composes both sub-stores. Setting it also runs the one-time
     * {@link #migrateSelfRecordsToLid()} consolidation so a store loaded from disk before the
     * canonicalisation existed has its split self records folded onto a single LID-keyed form.
     *
     * @param account the account sub-store, never {@code null}
     */
    void setAccount(LinkedWhatsAppAccountStore account) {
        this.account = Objects.requireNonNull(account, "account cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "signal store account sub-store set");
        migrateSelfRecordsToLid();
    }

    /**
     * Canonicalises a Signal protocol address so the local account's own devices are always keyed by
     * their LID form instead of splitting into two divergent records, one keyed by the account phone
     * number and one by the account LID.
     *
     * <p>{@link Jid#toSignalAddress()} derives the address name from the bare user number with no
     * LID-versus-PN normalisation, so the self account, which Cobalt references by both its phone
     * number and its LID, is the one peer that can land a single physical device under two addresses
     * and therefore two divergent Signal sessions: an inbound self-sent peer echo arrives
     * LID-addressed while other internal paths address the same device by phone number, so the two
     * ratchets drift and every echo fails its MAC. Rewriting the phone-number name to the LID name
     * (preserving the device id) collapses both onto one record. The rewrite is a no-op until the
     * account is LID-migrated (both {@link LinkedWhatsAppAccountStore#jid()} and {@link LinkedWhatsAppAccountStore#lid()} are
     * present) and never touches a non-self address.
     *
     * @param address the address to canonicalise, or {@code null}
     * @return the canonical LID-keyed address for a self phone-number address, otherwise the input
     */
    private SignalProtocolAddress canonicalAddress(SignalProtocolAddress address) {
        if (address == null || account == null) {
            return address;
        }
        var selfPn = account.jid().orElse(null);
        var selfLid = account.lid().orElse(null);
        if (selfPn == null || selfLid == null) {
            return address;
        }
        if (address.name().equals(selfPn.user()) && !address.name().equals(selfLid.user())) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "canonicalizing self address {0} to lid form", new LogRedactable.User(address.toString()));
            return new SignalProtocolAddress(selfLid.user(), address.id());
        }
        return address;
    }

    /**
     * Folds any self phone-number-keyed session and identity records onto their canonical LID form,
     * once, when the account is bound.
     *
     * <p>Reconciles a store written before {@link #canonicalAddress(SignalProtocolAddress)} existed:
     * a self device's record keyed by the account phone number is moved to the LID address when that
     * slot is free, and dropped as a stale duplicate when a LID-keyed record already exists, so the
     * post-condition is that no self device retains a phone-number-keyed session or identity. A
     * surviving but stale LID record is healed by the normal Signal retry-receipt flow on the next
     * inbound message. The pass is a no-op until the account is LID-migrated.
     */
    private void migrateSelfRecordsToLid() {
        if (account == null) {
            return;
        }
        var selfPn = account.jid().orElse(null);
        var selfLid = account.lid().orElse(null);
        if (selfPn == null || selfLid == null) {
            return;
        }
        var pnUser = selfPn.user();
        var lidUser = selfLid.user();
        if (pnUser.equals(lidUser)) {
            return;
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "migrating self signal records from {0} to {1}", new LogRedactable.User(pnUser), new LogRedactable.User(lidUser));
        }
        migrateSelfMap(sessionsMap, pnUser, lidUser);
        migrateSelfMap(remoteIdentitiesMap, pnUser, lidUser);
    }

    /**
     * Moves every self phone-number-keyed entry of {@code map} to its LID-keyed address, preserving
     * the device id and never overwriting an existing LID entry.
     *
     * @param map     the address-keyed map to consolidate
     * @param pnUser  the account phone-number user component to rewrite from
     * @param lidUser the account LID user component to rewrite to
     * @param <V>     the map value type
     */
    private static <V> void migrateSelfMap(ConcurrentMap<SignalProtocolAddress, V> map, String pnUser, String lidUser) {
        for (var entry : map.entrySet()) {
            var address = entry.getKey();
            if (!address.name().equals(pnUser)) {
                continue;
            }
            map.putIfAbsent(new SignalProtocolAddress(lidUser, address.id()), entry.getValue());
            map.remove(address);
        }
    }

    @Override
    public int registrationId() {
        return this.registrationId;
    }

    @Override
    public SignalIdentityKeyPair noiseKeyPair() {
        return this.noiseKeyPair;
    }

    @Override
    public SignalIdentityKeyPair identityKeyPair() {
        return this.identityKeyPair;
    }

    @Override
    public SignalSignedKeyPair signedKeyPair() {
        return signedKeyPair;
    }

    @Override
    public Optional<ADVSignedDeviceIdentity> signedDeviceIdentity() {
        return Optional.ofNullable(signedDeviceIdentity);
    }

    @Override
    public LinkedWhatsAppSignalStore setSignedDeviceIdentity(ADVSignedDeviceIdentity signedDeviceIdentity) {
        this.signedDeviceIdentity = signedDeviceIdentity;
        return this;
    }

    @Override
    public UUID fdid() {
        return fdid;
    }

    @Override
    public byte[] deviceId() {
        return deviceId;
    }

    @Override
    public UUID advertisingId() {
        return advertisingId;
    }

    @Override
    public byte[] identityId() {
        return identityId;
    }

    @Override
    public byte[] backupToken() {
        return backupToken;
    }

    @Override
    public Optional<byte[]> advSecretKey() {
        return Optional.ofNullable(advSecretKey);
    }

    @Override
    public LinkedWhatsAppSignalStore setAdvSecretKey(byte[] advSecretKey) {
        this.advSecretKey = advSecretKey;
        return this;
    }


    /**
     * Returns the live pre-key map backing this store.
     *
     * @return the live pre-key map
     */
    LinkedHashMap<Integer, SignalPreKeyPair> preKeysMap() {
        return preKeysMap;
    }

    /**
     * Returns the live sender-key map backing this store.
     *
     * @return the live sender-key map
     */
    ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeysMap() {
        return senderKeysMap;
    }

    /**
     * Returns the live session map backing this store.
     *
     * @return the live session map
     */
    ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessionsMap() {
        return sessionsMap;
    }

    /**
     * Returns the live remote-identity map backing this store.
     *
     * @return the live remote-identity map
     */
    ConcurrentMap<SignalProtocolAddress, SignalIdentityPublicKey> remoteIdentitiesMap() {
        return remoteIdentitiesMap;
    }

    /**
     * Returns the live base-key dedup map backing this store.
     *
     * @return the live base-key dedup map
     */
    ConcurrentMap<String, byte[]> baseKeysMap() {
        return baseKeysMap;
    }

    @Override
    public SequencedCollection<SignalPreKeyPair> preKeys() {
        return List.copyOf(preKeysMap.sequencedValues());
    }

    @Override
    public boolean hasPreKeys() {
        return !preKeysMap.isEmpty();
    }

    @Override
    public Optional<SignalPreKeyPair> findPreKeyById(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(preKeysMap.get(id));
    }

    @Override
    public void addPreKey(SignalPreKeyPair preKey) {
        Objects.requireNonNull(preKey, "preKey cannot be null");
        preKeysMap.put(preKey.id(), preKey);
    }

    @Override
    public boolean removePreKey(int id) {
        return preKeysMap.remove(id) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stores a single {@link #signedKeyPair} and matches solely against its
     * {@link SignalSignedKeyPair#id()}; any other id yields {@link Optional#empty()}. Rotation is
     * performed by replacing the field via the dedicated setter, not by accumulating signed pre-keys.
     */
    @Override
    public Optional<SignalSignedKeyPair> findSignedPreKeyById(Integer id) {
        return id == signedKeyPair.id() ? Optional.of(signedKeyPair) : Optional.empty();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @apiNote
     * The signed pre-key is a singleton in this store; new signed pre-keys are installed by
     * replacing {@link #signedKeyPair} via its setter, not by appending.
     *
     * @param signalSignedKeyPair ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void addSignedPreKey(SignalSignedKeyPair signalSignedKeyPair) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "rejected attempt to add a signed pre key to a singleton store");
        throw new UnsupportedOperationException("Cannot add signed pre keys to a Keys instance");
    }

    @Override
    public Optional<SignalSessionRecord> findSessionByAddress(SignalProtocolAddress address) {
        return Optional.ofNullable(sessionsMap.get(canonicalAddress(address)));
    }

    @Override
    public void addSession(SignalProtocolAddress address, SignalSessionRecord record) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "storing signal session for {0}", new LogRedactable.User(address.toString()));
        sessionsMap.put(canonicalAddress(address), record);
    }

    @Override
    public Optional<SignalSenderKeyRecord> findSenderKeyByName(SignalSenderKeyName name) {
        return Optional.ofNullable(senderKeysMap.get(name));
    }

    @Override
    public void addSenderKey(SignalSenderKeyName name, SignalSenderKeyRecord newRecord) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "storing sender key for {0}", new LogRedactable.User(name.toString()));
        senderKeysMap.put(name, newRecord);
    }

    @Override
    public boolean removeSession(SignalProtocolAddress address) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing signal session for {0}", new LogRedactable.User(address.toString()));
        return sessionsMap.remove(canonicalAddress(address)) != null;
    }

    @Override
    public void removeSenderKeys(Jid deviceJid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing sender keys for device {0}", deviceJid);
        var signalAddress = deviceJid.toSignalAddress();
        senderKeysMap.keySet().removeIf(name ->
                name.sender().equals(signalAddress)
        );
    }

    @Override
    public void removeSenderKeys(SignalSenderKeyName senderKeyName) {
        Objects.requireNonNull(senderKeyName, "senderKeyName cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing sender key {0}", new LogRedactable.User(senderKeyName.toString()));
        senderKeysMap.remove(senderKeyName);
    }

    @Override
    public void cleanupSignalSessions(Jid deviceJid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cleaning up signal sessions for device {0}", deviceJid);
        var signalAddress = deviceJid.toSignalAddress();
        removeSession(signalAddress);
        removeSenderKeys(deviceJid);
    }

    /**
     * Composes the {@link #baseKeysMap} map key from a Signal address and the upstream message id.
     *
     * @implNote
     * The pipe character is used as a separator because neither {@link SignalProtocolAddress#toString()}
     * nor a Signal message id can contain it, which keeps the encoding unambiguous and reversible.
     *
     * @param address       the Signal protocol address that owns the base key
     * @param originalMsgId the upstream message id that introduced the base key
     * @return the composite map key
     */
    private static String encodeBaseKeyKey(SignalProtocolAddress address, String originalMsgId) {
        return address.toString() + "|" + originalMsgId;
    }

    @Override
    public void saveSessionBaseKey(SignalProtocolAddress address, String originalMsgId, byte[] baseKey) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(originalMsgId, "originalMsgId cannot be null");
        Objects.requireNonNull(baseKey, "baseKey cannot be null");
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "saving session base key for {0}, msgId={1}, key={2}",
                    new LogRedactable.User(address.toString()), originalMsgId, baseKey);
        }
        baseKeysMap.put(encodeBaseKeyKey(canonicalAddress(address), originalMsgId), baseKey);
    }

    @Override
    public Optional<byte[]> findSessionBaseKey(SignalProtocolAddress address, String originalMsgId) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(originalMsgId, "originalMsgId cannot be null");
        return Optional.ofNullable(baseKeysMap.get(encodeBaseKeyKey(canonicalAddress(address), originalMsgId)));
    }

    @Override
    public boolean hasSameBaseKey(SignalProtocolAddress address, String originalMsgId, byte[] candidate) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(originalMsgId, "originalMsgId cannot be null");
        Objects.requireNonNull(candidate, "candidate cannot be null");
        var stored = baseKeysMap.get(encodeBaseKeyKey(canonicalAddress(address), originalMsgId));
        var matches = stored != null && Arrays.equals(stored, candidate);
        if (!matches && Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "base key mismatch for {0}, msgId={1}", new LogRedactable.User(address.toString()), originalMsgId);
        }
        return matches;
    }

    @Override
    public boolean removeSessionBaseKey(SignalProtocolAddress address, String originalMsgId) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(originalMsgId, "originalMsgId cannot be null");
        return baseKeysMap.remove(encodeBaseKeyKey(canonicalAddress(address), originalMsgId)) != null;
    }

    @Override
    public void markKeyRotation(Jid userJid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marking sender key rotation needed for {0}", userJid);
        usersNeedingSenderKeyRotation.add(userJid.toUserJid());
    }

    @Override
    public boolean clearKeyRotation(Jid userJid) {
        var cleared = usersNeedingSenderKeyRotation.remove(userJid.toUserJid());
        if (cleared && Log.DEBUG) LOGGER.log(Level.DEBUG, "cleared sender key rotation flag for {0}", userJid);
        return cleared;
    }

    @Override
    public boolean isKeyRotated(Collection<Jid> userJids) {
        return userJids.stream()
                .anyMatch(entry -> usersNeedingSenderKeyRotation.contains(entry.toUserJid()));
    }

    @Override
    public long updateIdentityRange(Collection<Jid> devices) {
        var seq = encryptionSequence.incrementAndGet();
        for (var device : devices) {
            var address = device.toSignalAddress();
            identityEncryptionRange.merge(address, seq, Math::min);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "updated identity range for {0} devices, seq={1}", devices.size(), seq);
        return seq;
    }

    @Override
    public boolean hasIdentityChanged(long sendSequence, Jid device) {
        var recorded = identityEncryptionRange.get(device.toSignalAddress());
        var changed = recorded == null || recorded > sendSequence;
        if (changed && Log.DEBUG) LOGGER.log(Level.DEBUG, "identity range changed for device {0}", device);
        return changed;
    }

    @Override
    public void clearIdentityRange(Jid device) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clearing identity range for device {0}", device);
        identityEncryptionRange.remove(device.toSignalAddress());
    }

    @Override
    public boolean hasSenderKeyDistributed(Jid groupJid, Jid participantJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participantJid, "participantJid cannot be null");

        var groupKey = groupJid.toString();
        var participants = groupSenderKeyDistribution.get(groupKey);
        if (participants == null) {
            return false;
        }
        return participants.contains(participantJid.toString());
    }

    @Override
    public void markSenderKeyDistributed(Jid groupJid, Jid participantJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participantJid, "participantJid cannot be null");

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marking sender key distributed for group {0} to {1}", groupJid, participantJid);
        var groupKey = groupJid.toString();
        groupSenderKeyDistribution
                .computeIfAbsent(groupKey, k -> ConcurrentHashMap.newKeySet())
                .add(participantJid.toString());
    }

    @Override
    public void clearSenderKeyDistribution(Jid groupJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clearing sender key distribution for group {0}", groupJid);
        groupSenderKeyDistribution.remove(groupJid.toString());
    }

    @Override
    public void clearSenderKeyDistributionForParticipant(Jid participantJid) {
        Objects.requireNonNull(participantJid, "participantJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clearing sender key distribution for participant {0}", participantJid);
        var participantKey = participantJid.toString();

        for (var participants : groupSenderKeyDistribution.values()) {
            participants.remove(participantKey);
        }
    }

    @Override
    public void forgetSenderKeyDistributed(Jid groupJid, Jid participantJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(participantJid, "participantJid cannot be null");

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "forgetting sender key distribution for group {0} to {1}", groupJid, participantJid);
        var groupKey = groupJid.toString();
        var participants = groupSenderKeyDistribution.get(groupKey);
        if (participants != null) {
            participants.remove(participantJid.toString());
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey, SignalKeyDirection signalKeyDirection) {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "trust-on-first-use accept for {0}, direction={1}",
                    new LogRedactable.User(signalProtocolAddress.toString()), signalKeyDirection);
        }
        return true;
    }

    @Override
    public void addTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey) {

    }

    @Override
    public void saveIdentity(SignalProtocolAddress address, SignalIdentityPublicKey identityKey) {
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(identityKey, "identityKey cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "saving remote identity for {0}", new LogRedactable.User(address.toString()));
        remoteIdentitiesMap.put(canonicalAddress(address), identityKey);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the local {@link #identityKeyPair()} public key when {@code address}
     * is the account's own Signal address (resolved through the bound {@link LinkedWhatsAppAccountStore}), and
     * otherwise reads the remote-identity table.
     */
    @Override
    public Optional<SignalIdentityPublicKey> findIdentityByAddress(SignalProtocolAddress address) {
        if (address == null) {
            return Optional.empty();
        }
        var canonical = canonicalAddress(address);
        var localJid = account == null ? null : account.jid().orElse(null);
        if (localJid != null && canonical.equals(canonicalAddress(localJid.toSignalAddress()))) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "resolved identity for {0} as local account identity", new LogRedactable.User(canonical.toString()));
            return Optional.of(identityKeyPair.publicKey());
        }
        return Optional.ofNullable(remoteIdentitiesMap.get(canonical));
    }

    @Override
    public Set<Jid> unconfirmedIdentityChanges() {
        return Set.copyOf(unconfirmedIdentityChanges);
    }

    @Override
    public void markIdentityChange(Jid deviceJid) {
        if (Log.WARNING) LOGGER.log(Level.WARNING, "unconfirmed identity change detected for {0}", deviceJid);
        unconfirmedIdentityChanges.add(deviceJid);
    }

    @Override
    public void confirmIdentityChange(Jid deviceJid) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "identity change confirmed for {0}", deviceJid);
        unconfirmedIdentityChanges.remove(deviceJid);
    }

    @Override
    public void clearUnconfirmedIdentityChanges() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "clearing {0} unconfirmed identity changes", unconfirmedIdentityChanges.size());
        unconfirmedIdentityChanges.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtobufLinkedWhatsAppSignalStore that)) {
            return false;
        }
        return Objects.equals(registrationId, that.registrationId)
               && Objects.equals(noiseKeyPair, that.noiseKeyPair)
               && Objects.equals(identityKeyPair, that.identityKeyPair)
               && Objects.equals(signedDeviceIdentity, that.signedDeviceIdentity)
               && Objects.equals(signedKeyPair, that.signedKeyPair)
               && Objects.equals(preKeysMap, that.preKeysMap)
               && Objects.equals(fdid, that.fdid)
               && Objects.deepEquals(deviceId, that.deviceId)
               && Objects.equals(advertisingId, that.advertisingId)
               && Objects.deepEquals(identityId, that.identityId)
               && Objects.deepEquals(backupToken, that.backupToken)
               && Objects.equals(senderKeysMap, that.senderKeysMap)
               && Objects.equals(sessionsMap, that.sessionsMap)
               && Objects.equals(remoteIdentitiesMap, that.remoteIdentitiesMap)
               && Objects.deepEquals(advSecretKey, that.advSecretKey)
               && Objects.equals(baseKeysMap, that.baseKeysMap)
               && Objects.equals(identityEncryptionRange, that.identityEncryptionRange)
               && encryptionSequence.get() == that.encryptionSequence.get()
               && Objects.equals(usersNeedingSenderKeyRotation, that.usersNeedingSenderKeyRotation)
               && Objects.equals(unconfirmedIdentityChanges, that.unconfirmedIdentityChanges)
               && Objects.equals(groupSenderKeyDistribution, that.groupSenderKeyDistribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationId, noiseKeyPair, identityKeyPair, signedDeviceIdentity, signedKeyPair,
                preKeysMap, fdid, Arrays.hashCode(deviceId), advertisingId, Arrays.hashCode(identityId),
                Arrays.hashCode(backupToken), senderKeysMap, sessionsMap, remoteIdentitiesMap,
                Arrays.hashCode(advSecretKey), baseKeysMap, identityEncryptionRange,
                encryptionSequence.get(), usersNeedingSenderKeyRotation, unconfirmedIdentityChanges,
                groupSenderKeyDistribution);
    }
}
