package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.key.SignalPreKeyPair;
import com.github.auties00.libsignal.state.SignalPreKeyBundleBuilder;

/**
 * Test helper that builds a libsignal session between two {@link LinkedWhatsAppStore} instances so
 * the {@link com.github.auties00.cobalt.message.send.crypto.MessageEncryption} pipeline can
 * encrypt for a recipient the sender has never talked to. It mimics the production wire flow
 * in two steps: the recipient's store advertises a prekey bundle (identity key, signed prekey,
 * one-time prekey), equivalent to what a prekey-fetch IQ returns; the sender's
 * {@link SignalSessionCipher} then processes the bundle, installing a session record keyed by
 * the recipient's {@link com.github.auties00.libsignal.SignalProtocolAddress}. The network
 * prekey fetch is bypassed so tests can synthesise both stores in process.
 */
public final class TestSignalSession {

    private TestSignalSession() {
        throw new AssertionError("TestSignalSession is not instantiable");
    }

    /**
     * Seeds {@code store} with at least one one-time prekey so it can publish a complete
     * {@link com.github.auties00.libsignal.state.SignalPreKeyBundle}. Idempotent: when the
     * store already carries a prekey the existing prekey is returned and no new one is
     * generated.
     *
     * @param store the store to seed
     * @return the existing or newly added prekey
     */
    public static SignalPreKeyPair seedOneTimePreKey(LinkedWhatsAppStore store) {
        var existing = store.signalStore().preKeys();
        if (!existing.isEmpty()) {
            return existing.iterator().next();
        }
        var preKey = SignalPreKeyPair.random(1);
        store.signalStore().addPreKey(preKey);
        return preKey;
    }

    /**
     * Installs a Signal session on {@code senderStore} so it can encrypt to {@code recipientJid}.
     * The recipient's identity material is drawn from {@code recipientStore}, packaged into a
     * {@link com.github.auties00.libsignal.state.SignalPreKeyBundle}, and processed by a
     * {@link SignalSessionCipher} backed by {@code senderStore}. A repeated call simply reseats
     * the session under the same recipient address.
     *
     * @param senderStore    the sender's protocol store
     * @param recipientJid   the recipient device JID
     * @param recipientStore the recipient's protocol store
     */
    public static void establishSession(LinkedWhatsAppStore senderStore, Jid recipientJid, LinkedWhatsAppStore recipientStore) {
        var preKey = seedOneTimePreKey(recipientStore);
        var signedKey = recipientStore.signalStore().signedKeyPair();
        var bundle = new SignalPreKeyBundleBuilder()
                .registrationId(recipientStore.signalStore().registrationId())
                .deviceId(recipientJid.device())
                .preKeyId(preKey.id())
                .preKeyPublic(preKey.publicKey())
                .signedPreKeyId(signedKey.id())
                .signedPreKeyPublic(signedKey.publicKey())
                .signedPreKeySignature(signedKey.signature())
                .identityKey(recipientStore.signalStore().identityKeyPair().publicKey())
                .build();
        new SignalSessionCipher(senderStore.signalStore()).process(recipientJid.toSignalAddress(), bundle);
    }
}
