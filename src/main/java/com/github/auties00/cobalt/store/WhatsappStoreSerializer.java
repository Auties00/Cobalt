
package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.client.WhatsAppClientType;

import java.nio.file.*;
import java.util.*;

/**
 * A serialization interface for managing persistent storage of WhatsApp session data.
 * <p>
 * This interface provides a standardized mechanism for serializing and deserializing ({@link WhatsAppStore}).
 * <p>
 * The interface supports multiple client types ({@link WhatsAppClientType#WEB} and {@link WhatsAppClientType#MOBILE})
 * and can be implemented to provide different storage backends. Two built-in implementations are provided:
 * <ul>
 *     <li>{@link WhatsappStoreSerializer#toProtobuf()} - Persists sessions as Protocol Buffer files on disk</li>
 *     <li>{@link WhatsappStoreSerializer#discarding()} - Discards all session data (useful for temporary/ephemeral sessions)</li>
 * </ul>
 * <p>
 * Sessions are identified by either a UUID or a phone number.
 * <p>
 * <b>Thread Safety:</b> Implementations should be thread-safe, as serialization operations may be
 * called concurrently from different parts of the WhatsApp API.
 *
 * @see WhatsAppStore
 */
@SuppressWarnings("unused")
public interface WhatsappStoreSerializer {
    /**
     * Creates a serializer that discards all session data without persisting it.
     * <p>
     * This implementation is useful for temporary sessions where persistence is not required,
     * such as testing environments or scenarios where sessions should not survive application restarts.
     *
     * @return a serializer that performs no actual persistence
     */
    static WhatsappStoreSerializer discarding() {
        return DiscardingStoreSerializer.INSTANCE;
    }

    /**
     * Creates a protobuf-based serializer that persists sessions to the default directory.
     * <p>
     * Sessions are stored as Protocol Buffer files in the default location, typically
     * {@code $HOME/.whatsapp/} on Unix-like systems or {@code %USERPROFILE%\.whatsapp\}
     * on Windows.
     *
     * @return a new protobuf-based serializer using the default storage directory
     */
    static WhatsappStoreSerializer toProtobuf() {
        return new ProtobufStoreSerializer();
    }

    /**
     * Creates a protobuf-based serializer that persists sessions to a specified directory.
     * <p>
     * This allows customization of where session files are stored, which can be useful for
     * applications that need to manage multiple isolated session stores or have specific
     * file system requirements.
     *
     * @param baseDirectory the directory where session files should be stored; must be writable
     * @return a new protobuf-based serializer using the specified storage directory
     */
    static WhatsappStoreSerializer toProtobuf(Path baseDirectory) {
        return new ProtobufStoreSerializer(baseDirectory);
    }

    /**
     * Retrieves an existing session from persistent storage using one or more identifiers.
     * <p>
     * This method attempts to locate and deserialize a session using the provided identifiers
     * in the following order:
     * <ol>
     *     <li>If {@code uuid} is provided, attempts to load the session by UUID</li>
     *     <li>If UUID lookup fails or is null, and {@code phoneNumber} is provided, attempts to load by phone number</li>
     * </ol>
     *
     * @param clientType  the type of WhatsApp client (web or mobile); must not be null
     * @param id          the unique identifier of the session to retrieve; may be null
     * @param phoneNumber the phone number associated with the session; may be null
     * @return an {@link Optional} containing the session if found, or empty if no matching session exists
     */
    default Optional<WhatsAppStore> startDeserialize(WhatsAppClientType clientType, UUID id, Long phoneNumber) {
        if (id != null) {
            var store = startDeserialize(clientType, id);
            if(store.isPresent()) {
                return store;
            }
        }

        if (phoneNumber != null) {
            var store = startDeserialize(clientType, phoneNumber);
            if(store.isPresent()) {
                return store;
            }
        }

        return Optional.empty();
    }

    /**
     * Lists all session UUIDs stored for a specific client type.
     * <p>
     * This method scans the persistent storage and returns the UUIDs of all sessions
     * associated with the specified client type. The returned collection maintains
     * the insertion order (typically the order in which sessions were created).
     *
     * @param type the WhatsApp client type to query; must not be null
     * @return a sequenced collection of UUIDs, never null but may be empty if no sessions exist
     */
    SequencedCollection<UUID> listIds(WhatsAppClientType type);

    /**
     * Lists all session phone numbers stored for a specific client type.
     * <p>
     * This method scans the persistent storage and returns the phone numbers of all sessions
     * associated with the specified client type. The returned collection maintains
     * the insertion order (typically the order in which sessions were created).
     * <p>
     * Note: Not all sessions have associated phone numbers (particularly web sessions
     * that haven't completed pairing), so this list may be smaller than the list
     * returned by {@link #listIds(WhatsAppClientType)}.
     *
     * @param type the WhatsApp client type to query; must not be null
     * @return a sequenced collection of phone numbers, never null but may be empty
     */
    SequencedCollection<Long> listPhoneNumbers(WhatsAppClientType type);

    /**
     * Persists session state to storage.
     * <p>
     * This method serializes the provided {@link WhatsAppStore} instance to persistent storage,
     * overwriting any existing store for the same session. Implementations should ensure
     * that the serialization is atomic to prevent data corruption in case of interruption.
     *
     * @param store the store to serialize; must not be null
     */
    void serialize(WhatsAppStore store);

    /**
     * Retrieves session state from storage by UUID.
     * <p>
     * This method deserializes a {@link WhatsAppStore} instance from persistent storage using
     * the session's UUID as the identifier.
     * <p>
     * <b>Important:</b> Implementations should only block while deserializing data that is
     * strictly necessary to bootstrap a WhatsApp session (e.g., JID, encryption keys references,
     * contact list). Large collections like chat history and newsletters should be deserialized
     * asynchronously to avoid blocking the caller for extended periods.
     *
     * @param type the WhatsApp client type; must not be null
     * @param id   the UUID of the session whose store should be retrieved; must not be null
     * @return an {@link Optional} containing the store if found, or empty if no store exists for this UUID
     * @see #finishDeserialize(WhatsAppStore)
     */
    Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, UUID id);

    /**
     * Retrieves session state from storage by phone number.
     * <p>
     * This method deserializes a {@link WhatsAppStore} instance from persistent storage using
     * the session's phone number as the identifier.
     * <p>
     * <b>Important:</b> Implementations should only block while deserializing data that is
     * strictly necessary to bootstrap a WhatsApp session (e.g., JID, encryption keys references,
     * contact list). Large collections like chat history and newsletters should be deserialized
     * asynchronously to avoid blocking the caller for extended periods.
     *
     * @param type        the WhatsApp client type; must not be null
     * @param phoneNumber the phone number of the session whose store should be retrieved; must not be null
     * @return an {@link Optional} containing the store if found, or empty if no store exists for this phone number
     * @see #finishDeserialize(WhatsAppStore)
     */
    Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, Long phoneNumber);

    /**
     * Blocks until all asynchronous deserialization operations for a store are complete.
     * <p>
     * This method should be called after {@link #startDeserialize(WhatsAppClientType, UUID)} or
     * {@link #startDeserialize(WhatsAppClientType, Long)} when the caller needs to ensure that
     * all session data (including large collections like chats and newsletters) has been fully
     * loaded into memory.
     * <p>
     * Implementations that perform all deserialization synchronously may provide an empty
     * implementation of this method.
     *
     * @param store the store whose deserialization should be completed; must not be null
     */
    void finishDeserialize(WhatsAppStore store);

    /**
     * Permanently removes a session from storage.
     * <p>
     * This method deletes all persistent data associated with the specified session.
     * After this operation completes, the session cannot be recovered and a new session must be created to use WhatsApp again.
     * <p>
     * This operation should be atomic and should not leave partial session data behind
     * in case of errors.
     *
     * @param type the WhatsApp client type; must not be null
     * @param uuid the UUID of the session to delete; must not be null
     */
    void deleteSession(WhatsAppClientType type, UUID uuid);
}