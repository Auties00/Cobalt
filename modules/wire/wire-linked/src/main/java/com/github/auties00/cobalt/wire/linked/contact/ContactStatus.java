package com.github.auties00.cobalt.wire.linked.contact;


import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Optional;

/**
 * A presence state for a {@link Contact}, describing whether the contact is
 * currently online, offline, typing a message, or recording an audio message
 * in the 1:1 conversation with the local user.
 *
 * <p>The WhatsApp server does not push presence updates for every contact by
 * default: updates arrive automatically only when a contact sends a message
 * or appears in the recent contacts list. To receive real-time presence
 * updates for a specific contact, explicitly subscribe to that contact using
 * {@code LinkedWhatsAppClient#subscribeToPresence(JidProvider)}.
 *
 * <p>This enum represents presence at the individual-contact level, reflecting
 * only the 1:1 conversation state. The presence of a participant within a
 * group chat is tracked separately, per group.
 *
 * <p>Composing and recording states reported by the server are transient and
 * are automatically cleared after a short inactivity window if no further
 * updates arrive; callers that present these states in a user interface
 * should apply a comparable timeout of their own.
 *
 * @see Contact#lastKnownPresence()
 */
@ProtobufEnum
public enum ContactStatus {
    /**
     * The contact is currently online and connected to WhatsApp. When a
     * contact transitions to this state, the server delivers a presence
     * update and the contact's last-seen timestamp is advanced to the
     * current time.
     */
    AVAILABLE(0),

    /**
     * The contact is currently offline, has disconnected from WhatsApp, or no
     * presence information has yet been received. This is the default state
     * assigned to a contact when the client has no fresher data. When the
     * contact is unavailable, the {@linkplain Contact#lastSeen() last-seen
     * timestamp} indicates when they were last online, unless hidden by the
     * contact's privacy settings.
     */
    UNAVAILABLE(1),

    /**
     * The contact is currently typing a text message in the conversation.
     * This state is reported by the server while the contact is actively
     * composing and expires automatically after a short inactivity window
     * if no further updates arrive.
     */
    COMPOSING(2),

    /**
     * The contact is currently recording an audio message in the conversation.
     * This state is reported by the server while the contact is actively
     * recording and expires automatically after a short inactivity window
     * if no further updates arrive.
     */
    RECORDING(3);

    /**
     * The protobuf wire-format index associated with this presence state.
     */
    final int index;

    /**
     * Constructs a new presence state with the given protobuf index.
     *
     * @param index the protobuf wire-format index for this state
     */
    ContactStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire-format index associated with this presence
     * state.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the {@code ContactStatus} whose {@link #name()} matches the
     * given string, ignoring case.
     *
     * <p>This helper is intended for parsing textual presence values such as
     * {@code "available"} or {@code "composing"} delivered by the server.
     *
     * @param name the presence state name to look up
     * @return an {@code Optional} containing the matching state, or empty if
     *         no constant matches
     */
    public static Optional<ContactStatus> of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Returns the lowercase form of this state's {@linkplain #name() name},
     * as used in presence stanza attributes.
     *
     * @return the state name in lowercase (for example {@code "available"}
     *         or {@code "composing"})
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
