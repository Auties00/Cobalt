package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that records a contact entry addressed by its LID identifier.
 *
 * <p>WhatsApp progressively migrates identifiers from phone-number-based
 * JIDs to LID (Linked Identity) JIDs, which do not expose the contact's
 * phone number. A {@code LidContactAction} stores the name and optional
 * username under which a LID contact is known, so that every linked device
 * displays the same information for contacts reached through their LID.
 *
 * <p>Each entry is indexed by the contact's LID JID through
 * {@link LidContactActionArgs} and is replicated via the
 * {@link SyncPatchType#CRITICAL_UNBLOCK_LOW} collection.
 */
@ProtobufMessage(name = "SyncActionValue.LidContactAction")
public final class LidContactAction implements SyncAction<LidContactActionArgs> {
    /**
     * The canonical action name {@code "lid_contact"} used to identify this
     * action inside a sync patch.
     */
    public static final String ACTION_NAME = "lid_contact";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.CRITICAL_UNBLOCK_LOW;

    /**
     * Returns the canonical action name {@code "lid_contact"}.
     *
     * @return the string {@code "lid_contact"}
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
     * The full display name saved for the LID contact.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String fullName;

    /**
     * The first name saved for the LID contact.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String firstName;

    /**
     * The WhatsApp username associated with the LID contact, when the
     * contact has claimed one.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String username;


    /**
     * Constructs a new {@code LidContactAction}. Intended to be invoked by
     * the generated builder and by the protobuf deserializer.
     *
     * @param fullName  the full display name, or {@code null}
     * @param firstName the first name, or {@code null}
     * @param username  the WhatsApp username, or {@code null}
     */
    LidContactAction(String fullName, String firstName, String username) {
        this.fullName = fullName;
        this.firstName = firstName;
        this.username = username;
    }

    /**
     * Returns the full display name saved for the LID contact.
     *
     * @return the full name, or an empty {@link Optional} if none was provided
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the first name saved for the LID contact.
     *
     * @return the first name, or an empty {@link Optional} if none was
     *         provided
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }

    /**
     * Returns the WhatsApp username associated with the LID contact.
     *
     * @return the username, or an empty {@link Optional} if the contact has
     *         not claimed a username
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Updates the full display name saved for the LID contact.
     *
     * @param fullName the new full name, or {@code null} to clear it
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Updates the first name saved for the LID contact.
     *
     * @param firstName the new first name, or {@code null} to clear it
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Updates the WhatsApp username associated with the LID contact.
     *
     * @param username the new username, or {@code null} to clear it
     */
    public void setUsername(String username) {
        this.username = username;
    }


}
