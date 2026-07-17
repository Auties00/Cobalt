package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Defines the end to end call key operations the call lifecycle drives across a call.
 *
 * <p>This is the contract over the call key plane: minting the thirty two byte raw key, wrapping it as the
 * Signal encryptable plaintext, fanning the wrapped key out per peer device inside a one to one offer or as
 * a group's per participant {@code <enc_rekey>}, exposing the local signed device identity a {@code pkmsg}
 * envelope must carry, and recovering an inbound key from a single offer fanout slot. Callers depend on
 * this interface rather than on a concrete crypto facade. The production implementation is
 * {@link LiveCallKeyExchange}, which routes every encrypt and decrypt through the reused Signal pipeline;
 * the lower level stanza level and explicit variant decrypt overloads live on {@link LiveCallKeyExchange}
 * itself, since no caller drives them through this interface.
 *
 * @see LiveCallKeyExchange
 * @see CallKeyDistribution
 * @see CallRekeyEnvelope
 */
public interface CallKeyExchange {
    /**
     * Mints a fresh thirty two byte raw end to end call key.
     *
     * <p>The key is the only secret the call plane distributes over the wire; the caller stores it on the
     * call runtime so accept and rekey reuse it and feed it to the local key derivation chain that derives
     * the SRTP, SFrame, and data channel keys.
     *
     * @return a new {@link LiveCallKeyExchange#CALL_KEY_LENGTH} byte call key
     */
    byte[] mintCallKey();

    /**
     * Wraps a raw call key as the plaintext that travels Signal encrypted inside an {@code <enc>}.
     *
     * <p>The plaintext is the protobuf encoding of the message container that carries the key, the
     * identical shape used for both the offer key and the group rekey key; the caller hands the returned
     * bytes to a per device Signal encrypt.
     *
     * @param callKey the raw call key to wrap
     * @return the protobuf encoded message container plaintext carrying the key
     * @throws NullPointerException     if {@code callKey} is {@code null}
     * @throws IllegalArgumentException if {@code callKey} is not {@link LiveCallKeyExchange#CALL_KEY_LENGTH}
     *                                  bytes long
     */
    byte[] wrapCallKey(byte[] callKey);

    /**
     * Encrypts a wrapped call key to every peer device as a one to one offer's per device fanout.
     *
     * <p>Each peer device becomes one {@link CallKeyDistribution} slot encrypted through the reused Signal
     * session cipher. The fanout is all or nothing: if encryption fails for any device, every encrypted
     * slot is discarded and each device is addressed with a {@linkplain CallKeyDistribution#bare(Jid) bare
     * destination} carrying no key, so the call still rings.
     *
     * @param deviceJids the peer device JIDs to fan the key out to; never empty in practice
     * @param plaintext  the wrapped message container plaintext from {@link #wrapCallKey(byte[])}
     * @return one fanout slot per device, all encrypted on success or all bare on any encryption failure
     * @throws NullPointerException if {@code deviceJids} or {@code plaintext} is {@code null}, or if
     *                              {@code deviceJids} contains a {@code null} element
     */
    List<CallKeyDistribution> encryptOfferFanout(Collection<Jid> deviceJids, byte[] plaintext);

    /**
     * Encrypts a wrapped call key to every connected participant device as a group rekey fanout.
     *
     * <p>This is the group only key rotation a group placement and every membership change drive: the local
     * participant mints one fresh key and reshares it to every other connected participant device. Unlike
     * the {@link #encryptOfferFanout(Collection, byte[]) offer fanout}, a rekey is NOT one stanza carrying a
     * per device {@code <destination>} block; each recipient device becomes one {@link CallRekeyEnvelope}
     * addressed unicast, which the caller wraps in its own
     * {@code <call to="<recipientDeviceLid>"><enc_rekey>...} stanza with a per round transaction id. The
     * single thirty two byte key is the same plaintext shape as the offer key; the three per domain
     * audio/video/appdata keys are derived locally after decrypt, not transmitted.
     *
     * <p>Unlike the offer fanout there is no all or nothing fallback: a rekey that fails for one recipient
     * simply omits that recipient's envelope while the others still rotate.
     *
     * @param recipientDevices the connected participant devices to reshare the key to
     * @param plaintext        the wrapped message container plaintext from {@link #wrapCallKey(byte[])} for
     *                         the freshly minted rekey key
     * @return one rekey envelope per recipient whose encryption succeeded, in input order
     * @throws NullPointerException if {@code recipientDevices} or {@code plaintext} is {@code null}, or if
     *                              {@code recipientDevices} contains a {@code null} element
     */
    List<CallRekeyEnvelope> encryptRekeyFanout(Collection<Jid> recipientDevices, byte[] plaintext);

    /**
     * Recovers the call key from a single offer fanout slot.
     *
     * <p>A {@linkplain CallKeyDistribution#isEncrypted() bare} slot, a slot whose {@code type} is absent or
     * unparseable, or a failed decrypt yields an empty result. The slot's device JID is the recipient
     * address, so the decryption sender is supplied separately by the caller from the outer call stanza's
     * {@code from}.
     *
     * @param slot      the offer fanout slot to decrypt
     * @param senderJid the device JID that authored the envelope, used as the Signal decryption sender
     * @return the recovered call key, or an empty result when it could not be recovered
     * @throws NullPointerException if {@code slot} or {@code senderJid} is {@code null}
     */
    Optional<byte[]> decryptCallKey(CallKeyDistribution slot, Jid senderJid);

    /**
     * Returns the local ADV signed device identity bytes, or {@code null} when none is stored.
     *
     * <p>A {@code pkmsg} call key envelope bootstraps a new Signal session, so the recipient must learn the
     * sender's identity; the caller attaches the returned block to the offer stanza whenever any device's
     * {@code <enc>} is {@code pkmsg}.
     *
     * @return the encoded ADV signed device identity, or {@code null} when none is stored
     */
    byte[] signedDeviceIdentity();
}
