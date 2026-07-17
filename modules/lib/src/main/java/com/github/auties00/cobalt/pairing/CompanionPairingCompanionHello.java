package com.github.auties00.cobalt.pairing;

import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

/**
 * Carries the intermediate state retained between sending
 * {@code companion_hello} and receiving {@code primary_hello}.
 *
 * <p>This is an internal handshake intermediate that no embedder
 * observes. The {@link #linkCodePairingSecret} is surfaced to the user
 * through the
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.PairingCode}
 * handler, while the other two fields are cached on the
 * {@link CompanionPairingService} and consulted when a
 * {@code primary_hello} notification later arrives.
 *
 * @implNote
 * This implementation collapses WA Web's {@code companionHello} return
 * shape ({@code linkCodePairingWrappedCompanionEphemeralPub},
 * {@code linkCodeKey}, {@code linkCodePairingCompanionADVEphemeralKeyPair},
 * {@code linkCodePairingSecret}) into a single record and discards
 * {@code linkCodeKey}: WA Web caches the imported PBKDF2 key for reuse
 * during {@code companionFinish}, but Cobalt re-derives it from the
 * pairing code and the primary's salt at finish time, so retaining the
 * cached key would serve no purpose. The
 * {@link #linkCodePairingWrappedCompanionEphemeralPub} layout is a
 * 32-byte PBKDF2 salt followed by a 16-byte AES-CTR counter followed
 * by the AES-CTR ciphertext of the companion's ephemeral X25519 public
 * key.
 *
 * @param linkCodePairingSecret                       the eight-character
 *                                                    Crockford base32
 *                                                    pairing code shown
 *                                                    to the user
 * @param linkCodePairingWrappedCompanionEphemeralPub the salt {@code ||}
 *                                                    counter {@code ||}
 *                                                    AES-CTR ciphertext
 *                                                    payload carried in
 *                                                    the
 *                                                    {@code companion_hello}
 *                                                    IQ
 * @param companionEphemeralKeyPair                   the companion's ADV
 *                                                    ephemeral
 *                                                    Curve25519 keypair
 *                                                    for this attempt
 */
record CompanionPairingCompanionHello(
        String linkCodePairingSecret,
        byte[] linkCodePairingWrappedCompanionEphemeralPub,
        SignalIdentityKeyPair companionEphemeralKeyPair
) {

}
