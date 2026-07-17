package com.github.auties00.cobalt.wire.linked.business.profile;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of resolving a WhatsApp Business short link to the account that
 * owns it.
 *
 * <p>Every WhatsApp Business account can publish a memorable short link (for
 * example {@code wa.me/yourshop}) that points back to the account. When an app
 * is handed only the trailing slug of such a link, it asks WhatsApp to look up
 * which account the slug belongs to. This model carries the result of that
 * lookup: whether the slug resolved at all, and, when it did, the resolved
 * account's identity.
 *
 * <p>WhatsApp exposes the owning account in two forms depending on which lookup
 * was used. {@link #identifier()} carries the account's primary contact
 * identifier (its phone-number-based id), while {@link #linkedIdentifier()}
 * carries the account's privacy-preserving alternate id; a given lookup
 * populates one of the two and leaves the other empty. When resolution fails,
 * both are empty and {@link #resolved()} is {@code false}; the optional
 * {@link #errorCode()} and {@link #errorText()} then describe why, with the
 * error text suitable for surfacing to the user.
 */
@ProtobufMessage
public final class BusinessCustomUrlIdentity {
    /**
     * Whether the short-link slug resolved to an owning account. When
     * {@code false}, no identity is present and the error fields describe the
     * failure.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean resolved;

    /**
     * Primary contact identifier of the owning account, populated by the
     * lookup that returns the account's phone-number-based id. Empty when the
     * lookup returned the alternate id instead or when resolution failed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid identifier;

    /**
     * Privacy-preserving alternate identifier of the owning account, populated
     * by the lookup that returns the account's hidden id. Empty when the lookup
     * returned the primary id instead or when resolution failed.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid linkedIdentifier;

    /**
     * Machine-readable error code describing why resolution failed. Empty when
     * resolution succeeded or when the server omitted the code.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String errorCode;

    /**
     * Human-readable error message describing why resolution failed, suitable
     * for surfacing to the user. Empty when resolution succeeded or when the
     * server omitted the message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String errorText;

    /**
     * Constructs a new {@code BusinessCustomUrlIdentity}. At most one of the
     * two identifiers is populated on a successful lookup; on failure both are
     * {@code null} and the error fields describe the cause.
     *
     * @param resolved         whether the slug resolved to an owning account
     * @param identifier       the primary contact identifier, or {@code null}
     * @param linkedIdentifier the privacy-preserving alternate identifier, or {@code null}
     * @param errorCode        the machine-readable failure code, or {@code null}
     * @param errorText        the human-readable failure message, or {@code null}
     */
    BusinessCustomUrlIdentity(boolean resolved,
                              Jid identifier,
                              Jid linkedIdentifier,
                              String errorCode,
                              String errorText) {
        this.resolved = resolved;
        this.identifier = identifier;
        this.linkedIdentifier = linkedIdentifier;
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    /**
     * Returns whether the short-link slug resolved to an owning account.
     *
     * @return {@code true} when the slug resolved, {@code false} otherwise
     */
    public boolean resolved() {
        return resolved;
    }

    /**
     * Returns the primary contact identifier of the owning account.
     *
     * @return an {@code Optional} containing the primary identifier, or empty
     *         when the alternate id was returned instead or resolution failed
     */
    public Optional<Jid> identifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Returns the privacy-preserving alternate identifier of the owning
     * account.
     *
     * @return an {@code Optional} containing the alternate identifier, or empty
     *         when the primary id was returned instead or resolution failed
     */
    public Optional<Jid> linkedIdentifier() {
        return Optional.ofNullable(linkedIdentifier);
    }

    /**
     * Returns the machine-readable error code describing why resolution failed.
     *
     * @return an {@code Optional} containing the error code, or empty when
     *         resolution succeeded or no code was supplied
     */
    public Optional<String> errorCode() {
        return Optional.ofNullable(errorCode);
    }

    /**
     * Returns the human-readable error message describing why resolution
     * failed.
     *
     * @return an {@code Optional} containing the error message, or empty when
     *         resolution succeeded or no message was supplied
     */
    public Optional<String> errorText() {
        return Optional.ofNullable(errorText);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCustomUrlIdentity) obj;
        return this.resolved == that.resolved &&
               Objects.equals(this.identifier, that.identifier) &&
               Objects.equals(this.linkedIdentifier, that.linkedIdentifier) &&
               Objects.equals(this.errorCode, that.errorCode) &&
               Objects.equals(this.errorText, that.errorText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resolved, identifier, linkedIdentifier, errorCode, errorText);
    }

    @Override
    public String toString() {
        return "BusinessCustomUrlIdentity[" +
               "resolved=" + resolved + ", " +
               "identifier=" + identifier + ", " +
               "linkedIdentifier=" + linkedIdentifier + ", " +
               "errorCode=" + errorCode + ", " +
               "errorText=" + errorText + ']';
    }
}
