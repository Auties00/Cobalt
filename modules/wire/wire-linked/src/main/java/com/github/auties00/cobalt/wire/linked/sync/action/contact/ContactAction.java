package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that records a contact entry from the user's address book.
 *
 * <p>This action is broadcast between the user's linked devices so that every
 * device shares the same view of how contacts are named, which phone number
 * (PN) and LID identifiers they resolve to, and whether they should appear in
 * the primary address book. It is the canonical mechanism WhatsApp uses to
 * keep contact metadata in sync across all devices linked to an account.
 *
 * <p>A {@code ContactAction} is indexed by the contact's JID through
 * {@link ContactActionArgs} and is carried in the
 * {@link SyncPatchType#CRITICAL_UNBLOCK_LOW} collection, which is one of the
 * first collections replicated after a new device links to the account.
 */
@ProtobufMessage(name = "SyncActionValue.ContactAction")
public final class ContactAction implements SyncAction<ContactActionArgs> {
    /**
     * The canonical action name {@code "contact"} used to identify this action
     * inside a sync patch.
     */
    public static final String ACTION_NAME = "contact";

    /**
     * The canonical action version for this action type, used by the sync
     * engine to reject patches produced by an incompatible client.
     */
    public static final int ACTION_VERSION = 2;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.CRITICAL_UNBLOCK_LOW;

    /**
     * Returns the canonical action name {@code "contact"}.
     *
     * @return the string {@code "contact"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version declared by this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The full display name saved for the contact, typically built from the
     * first and last name entered in the device's address book.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String fullName;

    /**
     * The first name saved for the contact, used on its own when no full name
     * is available.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String firstName;

    /**
     * The LID-based JID associated with the contact, used by the privacy
     * preserving identifier scheme.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid lidJid;

    /**
     * Whether this contact should be stored in the user's primary address
     * book, as opposed to a secondary or derived address book.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean saveOnPrimaryAddressbook;

    /**
     * The phone-number based JID associated with the contact.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid pnJid;

    /**
     * The WhatsApp username associated with the contact, when the contact has
     * claimed one.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String username;


    /**
     * Constructs a new {@code ContactAction}. Intended to be invoked by the
     * generated builder and by the protobuf deserializer.
     *
     * @param fullName                 the full display name, or {@code null}
     * @param firstName                the first name, or {@code null}
     * @param lidJid                   the LID-based JID, or {@code null}
     * @param saveOnPrimaryAddressbook whether the contact should be saved on
     *                                 the primary address book, or
     *                                 {@code null} if the flag is not set
     * @param pnJid                    the phone-number based JID, or
     *                                 {@code null}
     * @param username                 the WhatsApp username, or {@code null}
     */
    ContactAction(String fullName, String firstName, Jid lidJid, Boolean saveOnPrimaryAddressbook, Jid pnJid, String username) {
        this.fullName = fullName;
        this.firstName = firstName;
        this.lidJid = lidJid;
        this.saveOnPrimaryAddressbook = saveOnPrimaryAddressbook;
        this.pnJid = pnJid;
        this.username = username;
    }

    /**
     * Returns the full display name saved for the contact.
     *
     * @return the full name, or an empty {@link Optional} if none was provided
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the first name saved for the contact.
     *
     * @return the first name, or an empty {@link Optional} if none was
     *         provided
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    /**
     * Returns the LID-based JID associated with the contact.
     *
     * @return the LID JID, or an empty {@link Optional} if none was provided
     */
    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    /**
     * Returns whether this contact should be saved in the user's primary
     * address book, coalescing an absent value to {@code false}.
     *
     * @return {@code true} if the contact is marked for the primary address
     *         book, otherwise {@code false}
     */
    public boolean saveOnPrimaryAddressbook() {
        return saveOnPrimaryAddressbook != null && saveOnPrimaryAddressbook;
    }

    /**
     * Returns the phone-number based JID associated with the contact.
     *
     * @return the PN JID, or an empty {@link Optional} if none was provided
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Returns the WhatsApp username associated with the contact.
     *
     * @return the username, or an empty {@link Optional} if the contact has
     *         not claimed a username
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Updates the full display name saved for the contact.
     *
     * @param fullName the new full name, or {@code null} to clear it
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Updates the first name saved for the contact.
     *
     * @param firstName the new first name, or {@code null} to clear it
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Updates the LID-based JID associated with the contact.
     *
     * @param lidJid the new LID JID, or {@code null} to clear it
     */
    public void setLidJid(Jid lidJid) {
        this.lidJid = lidJid;
    }

    /**
     * Updates whether this contact should be stored in the primary address
     * book.
     *
     * @param saveOnPrimaryAddressbook the new flag value, or {@code null} to
     *                                 clear the field entirely
     */
    public void setSaveOnPrimaryAddressbook(Boolean saveOnPrimaryAddressbook) {
        this.saveOnPrimaryAddressbook = saveOnPrimaryAddressbook;
    }

    /**
     * Updates the phone-number based JID associated with the contact.
     *
     * @param pnJid the new PN JID, or {@code null} to clear it
     */
    public void setPnJid(Jid pnJid) {
        this.pnJid = pnJid;
    }

    /**
     * Updates the WhatsApp username associated with the contact.
     *
     * @param username the new username, or {@code null} to clear it
     */
    public void setUsername(String username) {
        this.username = username;
    }


}
