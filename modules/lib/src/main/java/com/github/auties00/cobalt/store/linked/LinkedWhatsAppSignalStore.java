package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentity;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalProtocolStore;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.key.SignalPreKeyPair;
import com.github.auties00.libsignal.key.SignalSignedKeyPair;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.SequencedCollection;
import java.util.UUID;

/**
 * The Signal-protocol cryptographic state of a WhatsApp client session.
 *
 * <p>This is the sub-store that backs WhatsApp's end-to-end encryption. It owns
 * the long-lived identity material (the Noise transport key pair, the Signal
 * identity key pair, the published signed pre-key and the one-time pre-keys),
 * the per-recipient Signal sessions and per-group sender keys, the
 * trust-on-first-use store of remote identity keys, the ADV (Auth Device V2)
 * companion-pairing credential, and the assorted in-flight bookkeeping the
 * encryption pipeline keeps to bound replays and drive sender-key rotation.
 *
 * <p>It extends {@link SignalProtocolStore} so it can be handed directly to the
 * libsignal session and group ciphers; the additional members declared here are
 * the WhatsApp-specific extensions to that contract.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#signalStore()} and rarely
 * call it directly; the surrounding client consumes it during connect, pairing
 * and message encryption/decryption.
 *
 * @see LinkedWhatsAppStore
 * @see SignalProtocolStore
 */
@WhatsAppWebModule(moduleName = "WAWebSignalStorage")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppSignalStore extends SignalProtocolStore {
    /**
     * Returns the Signal registration id of this device.
     *
     * @apiNote
     * This is the {@code registrationId} field in every Signal pre-key
     * bundle this client publishes; servers and peers use it to tell
     * apart different installs of the same WhatsApp account.
     *
     * @return the registration id, a value between 1 and 16380
     */
    @Override
    int registrationId();

    /**
     * Returns the long-term Noise key pair used to negotiate the encrypted transport with the WhatsApp servers.
     *
     * @apiNote
     * The Noise XX handshake at the start of every WebSocket connection
     * authenticates this client by proving possession of the
     * corresponding private key.
     *
     * @return the noise key pair, never {@code null}
     */
    SignalIdentityKeyPair noiseKeyPair();

    /**
     * Returns the long-term Signal identity key pair used for end-to-end encryption with other WhatsApp users.
     *
     * @apiNote
     * This key pair underlies the "Verify security code" QR/number pair
     * that two users can compare in the contact info screen; rotating
     * it invalidates every active Signal session.
     *
     * @return the identity key pair, never {@code null}
     */
    @Override
    SignalIdentityKeyPair identityKeyPair();

    /**
     * Returns the currently active signed pre-key.
     *
     * @apiNote
     * WhatsApp rotates this pair periodically; it is the pair every peer
     * uses to bootstrap a new Signal session with this device when no
     * one-time pre-key is available.
     *
     * @return the signed key pair, never {@code null}
     */
    SignalSignedKeyPair signedKeyPair();

    /**
     * Returns the FDID (Facebook Device Id) used by mobile WhatsApp during registration.
     *
     * @apiNote
     * The FDID is the cross-Meta device fingerprint that the
     * WhatsApp/Facebook/Instagram apps share to identify the same
     * physical device across the family of apps; the WhatsApp
     * registration server checks it for anti-abuse signals.
     *
     * @return the FDID, never {@code null}
     */
    UUID fdid();

    /**
     * Returns the random per-installation device id sent to the WhatsApp registration server.
     *
     * @apiNote
     * Mobile WhatsApp generates this once at first launch and persists
     * it; it disambiguates re-registrations of the same phone number.
     *
     * @return the device id bytes, never {@code null}
     */
    byte[] deviceId();

    /**
     * Returns the OS-level advertising identifier reported to WhatsApp's analytics.
     *
     * @apiNote
     * On mobile this is the Android AAID or iOS IDFA, sent for opt-in
     * conversion attribution (notably click-to-WhatsApp ads).
     *
     * @return the advertising id, never {@code null}
     */
    UUID advertisingId();

    /**
     * Returns the per-installation identity id sent during the registration handshake.
     *
     * @apiNote
     * Used together with the {@link #noiseKeyPair()} and
     * {@link #identityKeyPair()} to bind this client install to its
     * phone-number registration record.
     *
     * @return the identity id bytes, never {@code null}
     */
    byte[] identityId();

    /**
     * Returns the backup/recovery token for mobile WhatsApp.
     *
     * @apiNote
     * This token powers the "transfer your account" and chat backup
     * restore flows by binding the encrypted backup blob to the new
     * install.
     *
     * @return the backup token bytes, never {@code null}
     */
    byte[] backupToken();

    /**
     * Returns the ADV (Auth Device V2) signed device identity that endorses this companion.
     *
     * @apiNote
     * ADV is the protocol behind WhatsApp's multi-device feature: each
     * companion (Web, Desktop, second phone) is endorsed by a signature
     * from the primary phone's identity key, and that signature is what
     * makes the companion a recognised participant on the account.
     * Losing this record forces a re-pairing through the QR-code or
     * phone-number flow.
     *
     * @return an {@link Optional} containing the
     *         {@link ADVSignedDeviceIdentity}, or empty if not set
     */
    Optional<ADVSignedDeviceIdentity> signedDeviceIdentity();

    /**
     * Sets the ADV (Auth Device V2) signed device identity.
     *
     * @param identity the {@link ADVSignedDeviceIdentity}, or {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSignalStore setSignedDeviceIdentity(ADVSignedDeviceIdentity identity);

    /**
     * Returns the 32-byte ADV (Auth Device V2) secret used to HMAC the pairing handshake with the primary phone.
     *
     * @apiNote
     * Together with {@link #signedDeviceIdentity()}, this binds a
     * companion's key material to the primary's signature.
     *
     * @return an {@link Optional} containing the 32-byte key, or empty
     *         if not set
     */
    Optional<byte[]> advSecretKey();

    /**
     * Sets the ADV pairing-handshake secret key.
     *
     * @param key the ADV secret key (should be 32 bytes), or
     *            {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppSignalStore setAdvSecretKey(byte[] key);

    /**
     * Returns all registered Signal pre-keys in insertion order.
     *
     * @apiNote
     * Used by the pre-key publishing pipeline to choose which pairs are
     * uploaded next to the WhatsApp directory; rotation logic preserves
     * insertion order so the oldest unused pair is selected first.
     *
     * @return a non-null sequenced collection of {@link SignalPreKeyPair}
     */
    SequencedCollection<SignalPreKeyPair> preKeys();

    /**
     * Returns whether any pre-keys are currently available.
     *
     * @apiNote
     * When this returns {@code false} the client must replenish the
     * directory before peers can start new Signal sessions against
     * this account.
     *
     * @return {@code true} if pre-keys are available
     */
    boolean hasPreKeys();

    /**
     * Removes a Signal protocol session by address.
     *
     * @apiNote
     * Used after authentication failures (bad MAC, stale session) or
     * when a peer's identity key changes, to force the next outgoing
     * message to rebuild a fresh Signal session.
     *
     * @param address the {@link SignalProtocolAddress} of the session
     *                to remove
     * @return {@code true} if a session was removed
     */
    boolean removeSession(SignalProtocolAddress address);

    /**
     * Removes all sender-key records where the given device JID is the sender.
     *
     * @apiNote
     * Invoked when a participant leaves a group or rotates their device
     * so cached group-message keys for the departing device are
     * discarded.
     *
     * @param deviceJid the device {@link Jid} whose sender keys should
     *                  be removed
     */
    void removeSenderKeys(Jid deviceJid);

    /**
     * Removes the sender keys for a {@link SignalSenderKeyName}.
     *
     * @apiNote
     * Targets a single (group, sender) pair rather than all records for
     * the sender; used when only one group needs its sender-key state
     * reset.
     *
     * @param senderKeyName the sender key name
     */
    void removeSenderKeys(SignalSenderKeyName senderKeyName);

    /**
     * Cleans up all Signal sessions and sender keys for a device.
     *
     * @apiNote
     * Run when the device list for a peer drops a device or when
     * the peer logs out and is no longer reachable.
     *
     * @param deviceJid the device {@link Jid} to clean up
     */
    void cleanupSignalSessions(Jid deviceJid);

    /**
     * Persists Alice's X3DH base key for a pre-key message so the receive path can dedupe replays of the same {@code originalMsgId}.
     *
     * @apiNote
     * Invoked from the Signal receive path on every incoming pre-key
     * message; subsequent receives with the same {@code originalMsgId}
     * compare against {@link #hasSameBaseKey} to drop replays.
     *
     * @param address       the peer {@link SignalProtocolAddress} that
     *                      initiated the session
     * @param originalMsgId the {@code originalMsgId} carried by the
     *                      pre-key stanza
     * @param baseKey       the 32-byte X3DH ephemeral public key
     *                      extracted from the pre-key message
     * @throws NullPointerException if any argument is {@code null}
     */
    void saveSessionBaseKey(SignalProtocolAddress address, String originalMsgId, byte[] baseKey);

    /**
     * Returns the previously-saved Alice base key for a {@code (address, originalMsgId)} pair.
     *
     * @param address       the peer {@link SignalProtocolAddress}
     * @param originalMsgId the {@code originalMsgId} of the pre-key
     *                      stanza
     * @return an {@link Optional} containing the 32-byte base key, or
     *         {@link Optional#empty()} if no entry exists
     * @throws NullPointerException if any argument is {@code null}
     */
    Optional<byte[]> findSessionBaseKey(SignalProtocolAddress address, String originalMsgId);

    /**
     * Returns whether a stored base key matches the candidate one for the given {@code (address, originalMsgId)} pair.
     *
     * @apiNote
     * This is the replay-detection primitive: a {@code true} return
     * means the incoming pre-key message duplicates a previously seen
     * one and the Signal session should not be rebuilt.
     *
     * @param address       the peer {@link SignalProtocolAddress}
     * @param originalMsgId the {@code originalMsgId} of the pre-key
     *                      stanza
     * @param candidate     the candidate base key extracted from the
     *                      newly-received pre-key message
     * @return {@code true} if a base key was previously stored for this
     *         pair and equals {@code candidate}, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    boolean hasSameBaseKey(SignalProtocolAddress address, String originalMsgId, byte[] candidate);

    /**
     * Removes the persisted base key for a {@code (address, originalMsgId)} pair.
     *
     * @apiNote
     * Called once the corresponding Signal session has been
     * successfully bootstrapped so the dedup table does not grow
     * unboundedly.
     *
     * @param address       the peer {@link SignalProtocolAddress}
     * @param originalMsgId the {@code originalMsgId} of the pre-key
     *                      stanza
     * @return {@code true} if an entry was removed
     * @throws NullPointerException if any argument is {@code null}
     */
    boolean removeSessionBaseKey(SignalProtocolAddress address, String originalMsgId);

    /**
     * Marks a participant as having received the sender key for a group.
     *
     * @param groupJid       the group JID
     * @param participantJid the participant device JID
     */
    void markSenderKeyDistributed(Jid groupJid, Jid participantJid);

    /**
     * Checks if a participant has received the sender key for a group.
     *
     * @param groupJid       the group JID
     * @param participantJid the participant device JID
     * @return {@code true} if the participant has received the sender key
     */
    boolean hasSenderKeyDistributed(Jid groupJid, Jid participantJid);

    /**
     * Clears the sender key distribution status for all participants in a group.
     *
     * @param groupJid the group JID
     */
    void clearSenderKeyDistribution(Jid groupJid);

    /**
     * Clears the sender key distribution status for a participant across all groups.
     *
     * @param participantJid the participant JID
     */
    void clearSenderKeyDistributionForParticipant(Jid participantJid);

    /**
     * Marks the sender key as forgotten for a specific participant in a group.
     *
     * @param groupJid       the group JID
     * @param participantJid the participant JID
     */
    void forgetSenderKeyDistributed(Jid groupJid, Jid participantJid);

    /**
     * Marks a user as needing sender key rotation.
     *
     * @param userJid the user JID whose device list changed
     */
    void markKeyRotation(Jid userJid);

    /**
     * Checks if a user needs sender key rotation and clears the flag.
     *
     * @param userJid the user JID to check
     * @return {@code true} if the user needed rotation (flag is cleared)
     */
    boolean clearKeyRotation(Jid userJid);

    /**
     * Checks if any of the provided users need sender key rotation.
     *
     * @param userJids the user JIDs to check
     * @return {@code true} if any user needs rotation
     */
    boolean isKeyRotated(Collection<Jid> userJids);

    /**
     * Allocates a new send sequence number and records it against
     * each device's identity.
     *
     * @param devices the device JIDs that were encrypted to
     * @return the allocated sequence number
     */
    long updateIdentityRange(Collection<Jid> devices);

    /**
     * Checks whether a device's identity key may have changed since
     * the given send sequence.
     *
     * @param sendSequence the sequence returned by {@link #updateIdentityRange}
     * @param device       the device JID to check
     * @return {@code true} if the identity may have changed
     */
    boolean hasIdentityChanged(long sendSequence, Jid device);

    /**
     * Clears the identity range entry for a device.
     *
     * @param device the device JID whose range should be cleared
     */
    void clearIdentityRange(Jid device);

    /**
     * Saves an identity key for a remote user.
     *
     * @param address     the signal address for the user
     * @param identityKey the identity key to save
     */
    void saveIdentity(SignalProtocolAddress address, SignalIdentityPublicKey identityKey);

    /**
     * Finds a stored identity key for a user.
     *
     * @param address the signal address for the user
     * @return an {@link Optional} containing the identity key if found
     */
    Optional<SignalIdentityPublicKey> findIdentityByAddress(SignalProtocolAddress address);

    /**
     * Returns all devices with unconfirmed identity changes.
     *
     * @return an unmodifiable set of device JIDs
     */
    Set<Jid> unconfirmedIdentityChanges();

    /**
     * Marks a device as having an unconfirmed identity change.
     *
     * @param deviceJid the device JID
     */
    void markIdentityChange(Jid deviceJid);

    /**
     * Confirms an identity change for a device.
     *
     * @param deviceJid the device JID
     */
    void confirmIdentityChange(Jid deviceJid);

    /**
     * Clears all unconfirmed identity changes.
     */
    void clearUnconfirmedIdentityChanges();
}
