package com.github.auties00.cobalt.wire.linked.message.contact;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that carries a collection of contact cards.
 *
 * <p>A contacts-array message bundles multiple {@link ContactMessage}
 * entries so that a user can share several contacts in a single chat
 * message. Each nested contact keeps its own display name and vCard
 * payload, while the array-level display name is used as a label for
 * the whole bundle, for example when previewing the message in a chat
 * list.
 *
 * <p>To share a single contact, use {@link ContactMessage} directly.
 *
 * <p>As a {@link ContextualMessage}, this message can also carry
 * {@link ContextInfo} describing a quoted message, a forwarding score,
 * mentions and other contextual metadata.
 */
@ProtobufMessage(name = "Message.ContactsArrayMessage")
public final class ContactsArrayMessage implements ContextualMessage {
    /**
     * The human-readable label shown for the bundle of contacts. Typically
     * used when previewing the message before opening it, to summarise the
     * set of shared contacts with a single name.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String displayName;

    /**
     * The list of contacts shared by this message. Each entry is a fully
     * formed {@link ContactMessage} with its own display name and vCard
     * payload, so that the recipient's client can import each contact
     * independently.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<ContactMessage> contacts;

    /**
     * Optional contextual metadata attached to this message, describing for
     * example a quoted message, a forwarding score, mentioned participants
     * or any other context-related information surrounding the share.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new contacts-array message with the given values.
     *
     * @param displayName the human-readable label for the bundle, or {@code null}
     * @param contacts the list of shared contacts, or {@code null}
     * @param contextInfo optional contextual metadata, or {@code null}
     */
    ContactsArrayMessage(String displayName, List<ContactMessage> contacts, ContextInfo contextInfo) {
        this.displayName = displayName;
        this.contacts = contacts;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the human-readable label for the bundle of contacts.
     *
     * @return an {@link Optional} containing the display name, or empty if none was provided
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the list of contacts shared by this message. The returned list
     * is unmodifiable; if no contacts were set, this method returns an empty
     * list rather than {@code null}.
     *
     * @return an unmodifiable {@link List} of {@link ContactMessage} entries, never {@code null}
     */
    public List<ContactMessage> contacts() {
        return contacts == null ? List.of() : Collections.unmodifiableList(contacts);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the {@link ContextInfo}, or empty if none was provided
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the human-readable label for the bundle of contacts.
     *
     * @param displayName the new display name, or {@code null} to clear the value
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the list of contacts shared by this message.
     *
     * @param contacts the new list of {@link ContactMessage} entries, or {@code null} to clear the value
     */
    public void setContacts(List<ContactMessage> contacts) {
        this.contacts = contacts;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the new {@link ContextInfo}, or {@code null} to clear the value
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
