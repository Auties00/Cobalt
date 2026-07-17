package com.github.auties00.cobalt.wire.linked.contact;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model representing an outgoing contact entry used by the WhatsApp
 * "invite by contact" feature.
 *
 * <p>An outgoing contact is a locally stored reference to a phone-number
 * identified person that the current user has targeted for an invitation to
 * WhatsApp. Each record stores only the contact's {@link Jid}, full name and
 * first name: it does not carry presence, picture or encryption metadata
 * because the remote party is not yet a WhatsApp user.
 *
 * <p>Outgoing contacts are managed independently from regular {@link Contact}
 * records and are never merged into the address-book contact collection. This
 * separation mirrors the WhatsApp multi-device model, where invite targets
 * live in a dedicated store that is synchronised across devices through the
 * app-state sync mechanism.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state. To create or remove outgoing contacts at the protocol level, use the
 * dedicated sync actions exposed by {@code LinkedWhatsAppClient}.
 *
 * @see Contact
 */
@ProtobufMessage
public final class OutContact {
    /**
     * The non-{@code null} phone-number-based JID that uniquely identifies
     * this outgoing contact. The JID encodes the phone number and server of
     * the target user, even though they are not yet a WhatsApp user.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    /**
     * The full name associated with this outgoing contact, as supplied by the
     * sync action that created the entry. This value is {@code null} when no
     * full name was provided.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String fullName;

    /**
     * The first name associated with this outgoing contact, either explicitly
     * supplied by the sync action or derived from the first whitespace
     * delimited token of the {@linkplain #fullName() full name}. This value is
     * {@code null} when neither value was available.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String firstName;

    /**
     * Constructs a new outgoing contact with the given field values.
     *
     * @param jid       the non-{@code null} JID uniquely identifying this
     *                  outgoing contact
     * @param fullName  the full name to associate with this outgoing contact,
     *                  or {@code null}
     * @param firstName the first name to associate with this outgoing contact,
     *                  or {@code null}
     */
    OutContact(Jid jid, String fullName, String firstName) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.fullName = fullName;
        this.firstName = firstName;
    }

    /**
     * Returns the non-{@code null} JID that uniquely identifies this outgoing
     * contact.
     *
     * @return the outgoing contact's phone-number-based JID
     */
    public Jid jid() {
        return this.jid;
    }

    /**
     * Returns the full name associated with this outgoing contact.
     *
     * @return an {@code Optional} containing the full name, or empty if not
     *         available
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(this.fullName);
    }

    /**
     * Sets the full name associated with this outgoing contact.
     *
     * @param fullName the full name to set, or {@code null} to clear
     * @return this outgoing contact instance for method chaining
     */
    public OutContact setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    /**
     * Returns the first name associated with this outgoing contact.
     *
     * @return an {@code Optional} containing the first name, or empty if not
     *         available
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(this.firstName);
    }

    /**
     * Sets the first name associated with this outgoing contact.
     *
     * @param firstName the first name to set, or {@code null} to clear
     * @return this outgoing contact instance for method chaining
     */
    public OutContact setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * Returns a hash code derived from this outgoing contact's
     * {@linkplain #jid() JID}.
     *
     * @return the hash code of the JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.jid);
    }

    /**
     * Returns whether this outgoing contact is equal to the given object.
     *
     * <p>Two outgoing contacts are considered equal when they share the same
     * {@linkplain #jid() JID}, regardless of their name fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is an {@code OutContact} with
     *         the same JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof OutContact that && Objects.equals(this.jid, that.jid);
    }
}
