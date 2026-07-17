package com.github.auties00.cobalt.wire.linked.integrity;

import java.util.Objects;
import java.util.Optional;

/**
 * A security challenge the WhatsApp server pushes to a connected linked session, requiring the
 * client to re-prove control of the account before it may continue.
 *
 * <p>The server delivers the challenge as an in-band notification on an already-authenticated
 * socket. The client must satisfy it or be logged out; there is no way to dismiss it. Two challenge
 * shapes exist, distinguished by {@link #type()}: a {@link Type#PASSKEY} challenge carries the
 * WebAuthn challenge bytes the client signs with a passkey registered for {@code whatsapp.com}, and
 * a {@link Type#CAPTCHA} challenge carries the site key and URL of a human-verification widget. Only
 * the fields belonging to the active {@link Type} are present.
 *
 * <p>This is a read-only event payload handed to observers; the client never constructs one to send.
 */
public final class IntegrityChallenge {
    /**
     * The kind of proof the server is demanding.
     */
    public enum Type {
        /**
         * A WebAuthn assertion with a passkey registered for the relying party {@code whatsapp.com}.
         */
        PASSKEY,
        /**
         * A human-verification (CAPTCHA) widget challenge.
         */
        CAPTCHA
    }

    /**
     * The challenge kind selecting which of the remaining fields are populated.
     */
    private final Type type;

    /**
     * The raw WebAuthn challenge bytes for a {@link Type#PASSKEY} challenge, or {@code null}
     * otherwise.
     */
    private final byte[] passkeyChallenge;

    /**
     * The CAPTCHA widget site key for a {@link Type#CAPTCHA} challenge, or {@code null} otherwise.
     */
    private final String captchaSiteKey;

    /**
     * The CAPTCHA widget challenge URL for a {@link Type#CAPTCHA} challenge, or {@code null}
     * otherwise.
     */
    private final String captchaChallengeUrl;

    /**
     * Constructs a challenge of the given kind.
     *
     * <p>Callers should populate only the fields that belong to {@code type}; the others are
     * expected to be {@code null}.
     *
     * @param type                the challenge kind
     * @param passkeyChallenge    the WebAuthn challenge bytes, or {@code null} for a non-passkey
     *                            challenge
     * @param captchaSiteKey      the CAPTCHA site key, or {@code null} for a non-captcha challenge
     * @param captchaChallengeUrl the CAPTCHA challenge URL, or {@code null} for a non-captcha
     *                            challenge
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public IntegrityChallenge(Type type, byte[] passkeyChallenge, String captchaSiteKey,
                              String captchaChallengeUrl) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.passkeyChallenge = passkeyChallenge;
        this.captchaSiteKey = captchaSiteKey;
        this.captchaChallengeUrl = captchaChallengeUrl;
    }

    /**
     * Returns the challenge kind.
     *
     * @return the challenge kind
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the raw WebAuthn challenge bytes.
     *
     * @return an {@link Optional} carrying the challenge bytes, or empty for a non-passkey challenge
     */
    public Optional<byte[]> passkeyChallenge() {
        return Optional.ofNullable(passkeyChallenge);
    }

    /**
     * Returns the CAPTCHA widget site key.
     *
     * @return an {@link Optional} carrying the site key, or empty for a non-captcha challenge
     */
    public Optional<String> captchaSiteKey() {
        return Optional.ofNullable(captchaSiteKey);
    }

    /**
     * Returns the CAPTCHA widget challenge URL.
     *
     * @return an {@link Optional} carrying the challenge URL, or empty for a non-captcha challenge
     */
    public Optional<String> captchaChallengeUrl() {
        return Optional.ofNullable(captchaChallengeUrl);
    }
}
