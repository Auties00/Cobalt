package com.github.auties00.cobalt.pairing;

import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;

/**
 * Carries the output of the {@code companionFinish} algorithm.
 *
 * <p>This is an internal handshake intermediate that no embedder
 * observes. It is produced inside
 * {@link CompanionPairingService#handlePrimaryHello} after the
 * {@code primary_hello} notification arrives and is consumed
 * immediately by the same method to populate the
 * {@code companion_finish} IQ and to persist the derived ADV secret.
 *
 * @implNote
 * This implementation flattens what WA Web returns as a structured
 * {@code {linkCodePairingWrappedKeyBundle, companionIdentityPublic,
 * advSecret}} object into a record with three byte-array fields. The
 * wrapped key bundle layout is the HKDF salt (32 bytes), the AES-GCM
 * IV (12 bytes), then the AES-GCM ciphertext of
 * {@code companionIdentityPublic || primaryIdentityPublic ||
 * advSecretMaterialSalt}; the {@code advSecret} is the HKDF-SHA256
 * output produced with {@code info="adv_secret"} and is persisted via
 * {@link LinkedWhatsAppSignalStore#setAdvSecretKey}.
 *
 * @param linkCodePairingWrappedKeyBundle the salt {@code ||} IV
 *                                        {@code ||} GCM ciphertext
 *                                        payload carried in the
 *                                        {@code companion_finish} IQ
 * @param advSecret                       the 32-byte HKDF-derived ADV
 *                                        master secret to persist
 * @param companionIdentityPublic         the companion's long-term
 *                                        identity public key echoed
 *                                        into the IQ
 */
record CompanionPairingCompanionFinish(
        byte[] linkCodePairingWrappedKeyBundle,
        byte[] advSecret,
        byte[] companionIdentityPublic
) {

}
