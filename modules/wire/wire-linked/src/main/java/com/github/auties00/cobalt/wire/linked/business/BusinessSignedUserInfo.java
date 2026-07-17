package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Cryptographic identity envelope returned by WhatsApp's "signed user info"
 * business catalog query.
 *
 * <p>When a third-party app or device wants to confirm that a phone number
 * advertised by a business is genuinely operated by the holder of a
 * particular cryptographic key, it issues a signed-user-info IQ against the
 * business catalog. The server replies with this envelope, which pairs the
 * advertised phone number with a signature over that number, the
 * registered business domain, and a server-supplied expiration timestamp
 * after which the signature must be re-fetched.
 *
 * <p>Every field is delivered as an opaque string and any of them may be
 * absent on the wire, so callers that depend on the signature must check for
 * the presence of {@link #phoneNumber()}, {@link #phoneNumberSignature()},
 * and {@link #phoneNumberSignatureExpiration()} before trusting the result.
 * The {@link #businessDomain()} field, when present, carries the registered
 * fully-qualified domain name associated with the business account.
 */
@ProtobufMessage
public final class BusinessSignedUserInfo {
    /**
     * Phone number advertised by the business account, in the same opaque
     * string form returned by the server. Populated when the wire response
     * included the {@code <phone_number>} child of {@code <signed_user_info>}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String phoneNumber;

    /**
     * Server-supplied expiration of the signature, conveyed as an opaque
     * TTL string in the wire format. Callers must re-fetch the envelope
     * after this moment to obtain a fresh signature; the value is left as a
     * raw string because WhatsApp does not commit to a numeric encoding.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String phoneNumberSignatureExpiration;

    /**
     * Cryptographic signature over the advertised phone number, used by
     * verifiers to assert that the business holding the corresponding key
     * controls the number. Populated when the wire response included the
     * {@code <phone_number_signature>} child.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String phoneNumberSignature;

    /**
     * Fully-qualified domain name registered to the business account. When
     * present, this domain can be cross-checked against the website
     * advertised in the business profile to provide a second link between
     * the WhatsApp account and the public business identity.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String businessDomain;

    /**
     * Constructs a new {@code BusinessSignedUserInfo} with the given
     * signature components. Any argument may be {@code null} when the
     * corresponding wire field was omitted by the server.
     *
     * @param phoneNumber                    the advertised phone number, or {@code null}
     * @param phoneNumberSignatureExpiration the signature expiration TTL, or {@code null}
     * @param phoneNumberSignature           the signature over the phone number, or {@code null}
     * @param businessDomain                 the registered business domain, or {@code null}
     */
    BusinessSignedUserInfo(String phoneNumber,
                           String phoneNumberSignatureExpiration,
                           String phoneNumberSignature,
                           String businessDomain) {
        this.phoneNumber = phoneNumber;
        this.phoneNumberSignatureExpiration = phoneNumberSignatureExpiration;
        this.phoneNumberSignature = phoneNumberSignature;
        this.businessDomain = businessDomain;
    }

    /**
     * Returns the phone number advertised by the business account.
     *
     * @return an {@code Optional} containing the phone number string, or
     *         empty when the wire omitted the field
     */
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the server-supplied expiration of the signature.
     *
     * @return an {@code Optional} containing the expiration TTL string, or
     *         empty when the wire omitted the field
     */
    public Optional<String> phoneNumberSignatureExpiration() {
        return Optional.ofNullable(phoneNumberSignatureExpiration);
    }

    /**
     * Returns the cryptographic signature over the advertised phone
     * number.
     *
     * @return an {@code Optional} containing the signature string, or empty
     *         when the wire omitted the field
     */
    public Optional<String> phoneNumberSignature() {
        return Optional.ofNullable(phoneNumberSignature);
    }

    /**
     * Returns the fully-qualified domain name registered to the business
     * account.
     *
     * @return an {@code Optional} containing the domain string, or empty
     *         when the wire omitted the field
     */
    public Optional<String> businessDomain() {
        return Optional.ofNullable(businessDomain);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessSignedUserInfo) obj;
        return Objects.equals(this.phoneNumber, that.phoneNumber) &&
               Objects.equals(this.phoneNumberSignatureExpiration, that.phoneNumberSignatureExpiration) &&
               Objects.equals(this.phoneNumberSignature, that.phoneNumberSignature) &&
               Objects.equals(this.businessDomain, that.businessDomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber, phoneNumberSignatureExpiration, phoneNumberSignature, businessDomain);
    }

    @Override
    public String toString() {
        return "BusinessSignedUserInfo[" +
               "phoneNumber=" + phoneNumber + ", " +
               "phoneNumberSignatureExpiration=" + phoneNumberSignatureExpiration + ", " +
               "phoneNumberSignature=" + phoneNumberSignature + ", " +
               "businessDomain=" + businessDomain + ']';
    }
}
