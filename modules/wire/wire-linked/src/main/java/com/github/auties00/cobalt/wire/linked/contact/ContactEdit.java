package com.github.auties00.cobalt.wire.linked.contact;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.editContact}. Describes a
 * single contact-card edit that is replicated across the user's linked
 * devices. The target {@link #contact} JID is required; every other
 * field is optional and only takes effect when carried with a value.
 *
 * <p>This packet drives the contact-sync flow: when an entry is added
 * or modified on one device, the same payload is replayed against the
 * primary device's native addressbook so the contact stays consistent
 * across surfaces. Removing a contact does not use this model and goes
 * through {@code LinkedWhatsAppClient.deleteContact} instead.
 *
 * <p>{@link #syncToAddressbook} is declared as a nullable
 * {@link Boolean} so that a present {@code false} is distinguishable
 * from an absent value: an explicit {@code false} stops the primary
 * device from mirroring the contact into its native addressbook, while
 * {@code null} leaves the existing preference untouched.
 */
@ProtobufMessage
public final class ContactEdit {
    /**
     * JID of the contact whose card is being edited. Required.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid contact;

    /**
     * New first name for the contact, or {@code null} to leave it
     * unset on the wire.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String firstName;

    /**
     * New full display name for the contact, or {@code null} to leave
     * it unset on the wire.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String fullName;

    /**
     * Optional alternative privacy-preserving identifier (LID form
     * {@link Jid}) for the same contact, or {@code null} when only
     * the primary JID is known.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid lidJid;

    /**
     * Whether the primary device must mirror this contact into its
     * native addressbook. A present {@code true} requests the sync,
     * a present {@code false} suppresses it, and {@code null} leaves
     * the existing preference untouched.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final Boolean syncToAddressbook;

    /**
     * New WhatsApp username for the contact (without leading
     * {@code @}), or {@code null} to leave it unset on the wire.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String username;

    /**
     * Constructs a new {@code ContactEdit}.
     *
     * @param contact           the target contact JID; required
     * @param firstName         the new first name, or {@code null}
     * @param fullName          the new full display name, or {@code null}
     * @param lidJid            the LID-form JID for the same contact, or {@code null}
     * @param syncToAddressbook whether to mirror the contact into the
     *                          native addressbook, or {@code null} to
     *                          leave the preference unchanged
     * @param username          the new WhatsApp username (without leading
     *                          {@code @}), or {@code null}
     * @throws NullPointerException if {@code contact} is {@code null}
     */
    ContactEdit(Jid contact, String firstName, String fullName, Jid lidJid,
                Boolean syncToAddressbook, String username) {
        this.contact = Objects.requireNonNull(contact, "contact cannot be null");
        this.firstName = firstName;
        this.fullName = fullName;
        this.lidJid = lidJid;
        this.syncToAddressbook = syncToAddressbook;
        this.username = username;
    }

    /**
     * Returns the contact JID this edit applies to.
     *
     * @return the JID, never {@code null}
     */
    public Jid contact() {
        return contact;
    }

    /**
     * Returns the optional new first name.
     *
     * @return an {@link Optional} carrying the first name, or empty
     *         when the first name should be left unset on the wire
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    /**
     * Returns the optional new full display name.
     *
     * @return an {@link Optional} carrying the full name, or empty
     *         when the full name should be left unset on the wire
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the optional LID-form JID for the same contact.
     *
     * @return an {@link Optional} carrying the LID JID, or empty when
     *         only the primary JID is known
     */
    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    /**
     * Returns the optional addressbook-sync preference.
     *
     * @return an {@link Optional} carrying {@code true} to mirror the
     *         contact, {@code false} to suppress mirroring, or empty
     *         to leave the existing preference untouched
     */
    public Optional<Boolean> syncToAddressbook() {
        return Optional.ofNullable(syncToAddressbook);
    }

    /**
     * Returns the optional new WhatsApp username.
     *
     * @return an {@link Optional} carrying the username (without
     *         leading {@code @}), or empty when the username should
     *         be left unset on the wire
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ContactEdit) obj;
        return Objects.equals(contact, that.contact) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(fullName, that.fullName) &&
                Objects.equals(lidJid, that.lidJid) &&
                Objects.equals(syncToAddressbook, that.syncToAddressbook) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contact, firstName, fullName, lidJid, syncToAddressbook, username);
    }

    @Override
    public String toString() {
        return "ContactEdit[" +
                "contact=" + contact + ", " +
                "firstName=" + firstName + ", " +
                "fullName=" + fullName + ", " +
                "lidJid=" + lidJid + ", " +
                "syncToAddressbook=" + syncToAddressbook + ", " +
                "username=" + username + ']';
    }
}
