package com.github.auties00.cobalt.wire.linked.message.contact;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that carries a single contact card.
 *
 * <p>A contact message shares one person's contact details with the
 * recipient. The contact is described by a human-readable display name
 * and a vCard payload that follows the standard vCard format (typically
 * version 3.0), so that the recipient's device can import the contact
 * directly into its address book. This type of message is typically
 * produced when a user picks an entry from the native contact picker
 * and sends it through a chat.
 *
 * <p>To share multiple contacts at once, use {@link ContactsArrayMessage}.
 *
 * <p>As a {@link ContextualMessage}, this message can also carry
 * {@link ContextInfo} describing a quoted message, a forwarding score,
 * mentions and other contextual metadata.
 */
@ProtobufMessage(name = "Message.ContactMessage")
public final class ContactMessage implements ContextualMessage {
    /**
     * The human-readable name shown to the recipient for this contact, used
     * as a short label when the recipient previews the message in the chat
     * list. Typically matches the formatted full name stored on the contact's
     * vCard.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String displayName;

    /**
     * The contact card payload encoded as a vCard string. The value follows
     * the standard vCard format (typically version 3.0) and contains every
     * shared property of the contact, such as names, phone numbers, e-mail
     * addresses and photos. The recipient's client parses this string to
     * import the contact into the device address book.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    String vcard;

    /**
     * Optional contextual metadata attached to this message, describing for
     * example a quoted message, a forwarding score, mentioned participants
     * or any other context-related information surrounding the share.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Indicates whether the shared contact represents the sender themself.
     * When set to {@code true}, the recipient's client may render the message
     * with a different label to highlight that the sender is sharing their
     * own contact card. Populated on receive-side parsing rather than at
     * authoring time.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    Boolean isSelfContact;


    /**
     * Constructs a new contact message with the given values.
     *
     * @param displayName the human-readable name for the contact, or {@code null}
     * @param vcard the vCard payload describing the contact, or {@code null}
     * @param contextInfo optional contextual metadata, or {@code null}
     * @param isSelfContact {@code true} if the contact represents the sender, or {@code null}
     */
    ContactMessage(String displayName, String vcard, ContextInfo contextInfo, Boolean isSelfContact) {
        this.displayName = displayName;
        this.vcard = vcard;
        this.contextInfo = contextInfo;
        this.isSelfContact = isSelfContact;
    }

    /**
     * Returns the human-readable name of the shared contact.
     *
     * @return an {@link Optional} containing the display name, or empty if none was provided
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the vCard payload describing the shared contact.
     *
     * @return an {@link Optional} containing the vCard string, or empty if none was provided
     */
    public Optional<String> vcard() {
        return Optional.ofNullable(vcard);
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
     * Returns whether the shared contact represents the sender. When the
     * underlying field is {@code null}, this method returns {@code false},
     * meaning the contact is treated as a regular third-party contact.
     *
     * @return {@code true} if the contact is the sender themself, {@code false} otherwise
     */
    public boolean isSelfContact() {
        return isSelfContact != null && isSelfContact;
    }

    /**
     * Sets the human-readable name of the shared contact.
     *
     * @param displayName the new display name, or {@code null} to clear the value
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the vCard payload describing the shared contact.
     *
     * @param vcard the new vCard string, or {@code null} to clear the value
     */
    public void setVcard(String vcard) {
        this.vcard = vcard;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the new {@link ContextInfo}, or {@code null} to clear the value
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets whether the shared contact represents the sender.
     *
     * @param isSelfContact {@code true} to mark the contact as the sender, {@code false} or {@code null} otherwise
     */
    public void setSelfContact(Boolean isSelfContact) {
        this.isSelfContact = isSelfContact;
    }
}
