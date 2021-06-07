package it.auties.whatsapp4j.protobuf.message;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import jakarta.validation.constraints.NotNull;

/**
 * A model interface that represents a WhatsappMessage sent by a contact that provides a context.
 * Classes that implement this interface provide an accessor named contextInfo to access said property.
 *
 * This interface is sealed to prepare for <a href="https://openjdk.java.net/jeps/406">pattern matching for instanceof in switch statements</a>, set to be released in Java 17.
 */
public sealed interface ContextualMessage extends Message permits ContactMessage,
        ContactsArrayMessage, ExtendedTextMessage, GroupInviteMessage,
        LiveLocationMessage, LocationMessage, MediaMessage {

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @NotNull ContextInfo contextInfo();
}
