package com.github.auties00.cobalt.wire.linked.privacy;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Categorises a privacy "trust token" issued by a WhatsApp client to confirm
 * that a contact knows the local user.
 *
 * <p>WhatsApp uses these tokens to lock a contact relationship to a specific
 * identity-key pair. When the local user marks another account as a trusted
 * contact, the server stores a token whose validity carries forward across
 * re-pairings of either side, so the contact does not have to re-verify when
 * the local user re-registers on a new device.
 *
 * <p>Today only one category exists, but the enum is kept open to make room
 * for additional trust-token kinds without a breaking wire change.
 */
@ProtobufEnum
public enum PrivacyTokenType {
    /**
     * Marks the contact as a "trusted contact". The local user has confirmed
     * they know the remote party and the server will preserve that trust
     * across identity-key rotations on either side. Wire form is
     * {@code "trusted_contact"}.
     */
    TRUSTED_CONTACT("trusted_contact");

    /**
     * Wire string emitted as the {@code type} attribute on each
     * {@code <token>} child of the {@code <iq xmlns="privacy">} stanza.
     */
    private final String wireValue;

    /**
     * Constructs a new constant carrying the supplied wire value.
     *
     * @param wireValue the token-type value sent on the wire
     */
    PrivacyTokenType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-side value emitted on the {@code type} attribute of
     * a privacy token stanza.
     *
     * @return the wire value, never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }
}
