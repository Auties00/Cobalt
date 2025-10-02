package it.auties.whatsapp.controller;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.WhatsappClientType;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Abstract base class for WhatsApp session controllers that manage cryptographic keys and session state.
 * <p>
 * This sealed class serves as the foundation for two core controller types in the WhatsApp client:
 * <ul>
 *     <li>{@link WhatsappStore} - Manages user data, contacts, chats, and session-related information</li>
 *     <li>{@link Keys} - Manages cryptographic keys and Signal protocol state</li>
 * </ul>
 * <p>
 * Controllers provide a unified interface for managing session identifiers, phone numbers, and
 * serialization behavior across both Web and Mobile client types. Each controller instance is
 * uniquely identified by a UUID and optionally associated with a phone number.
 * <p>
 * Controllers can be serialized to persistent storage using a {@link WhatsappStoreSerializer}
 * implementation. This allows sessions to be restored across application restarts. Serialization
 * can be disabled by setting {@link #setSerializable(boolean)} to false.
 * <p>
 * Thread Safety: Implementations of this class should document their thread-safety guarantees.
 *
 * @see WhatsappStore
 * @see Keys
 * @see WhatsappStoreSerializer
 * @see WhatsappClientType
 */
@SuppressWarnings("unused")
@ProtobufMessage
public abstract sealed class Controller permits WhatsappStore, Keys {
    /**
     * The unique identifier for this controller instance.
     * <p>
     * This UUID is used to identify and manage the controller throughout its lifecycle,
     * including during serialization and deserialization operations.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    protected final UUID uuid;

    /**
     * The phone number associated with this controller's WhatsApp account.
     * <p>
     * This field may be null during initial session setup, particularly for Web clients
     * before QR code authentication is completed. For Mobile clients, this is typically
     * set during the registration process.
     * <p>
     * The phone number is stored as a Long representation of the international format
     * without the '+' prefix (e.g., 1234567890 for +1-234-567-890).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    protected Long phoneNumber;

    /**
     * The serializer responsible for persisting this controller to storage.
     * <p>
     * This serializer determines how and where the controller's data is saved. Common
     * implementations include file-based serialization and in-memory-only operation.
     * <p>
     * The serializer can be changed at runtime using {@link #setSerializer(WhatsappStoreSerializer)},
     * which is useful for migrating between different storage backends.
     *
     * @see WhatsappStoreSerializer
     * @see #serialize()
     */
    protected WhatsappStoreSerializer serializer;

    /**
     * The type of WhatsApp client this controller is managing.
     * <p>
     * This determines the protocol behavior, authentication method, and available features:
     * <ul>
     *     <li>{@link WhatsappClientType#WEB} - Web/Desktop client using QR code/Pairing code authentication</li>
     *     <li>{@link WhatsappClientType#MOBILE} - Mobile client using phone number authentication</li>
     * </ul>
     * <p>
     * The client type affects how the controller serializes data, what authentication
     * credentials are required, and which API features are accessible.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    protected final WhatsappClientType clientType;

    /**
     * Flag indicating whether this controller should be persisted to storage.
     * <p>
     * When set to false, the {@link #serialize()} method will not perform any operations.
     * Default value is true.
     *
     * @see #serialize()
     * @see #setSerializable(boolean)
     */
    protected boolean serializable;

    /**
     * Constructs a new Controller with the specified parameters.
     * <p>
     * This constructor is called by subclasses ({@link WhatsappStore} and {@link Keys}) during
     * initialization. It validates the UUID and initializes the controller with the
     * provided configuration.
     *
     * @param uuid        the unique identifier for this controller, must not be null
     * @param phoneNumber the phone number associated with this controller, may be null
     * @param serializer  the serializer to use for persistence operations, may be null
     * @param clientType  the type of WhatsApp client this controller manages, must not be null
     * @throws NullPointerException if uuid is null
     */
    public Controller(UUID uuid, Long phoneNumber, WhatsappStoreSerializer serializer, WhatsappClientType clientType) {
        this.uuid = Objects.requireNonNull(uuid, "Missing uuid");
        this.phoneNumber = phoneNumber;
        this.serializer = serializer;
        this.clientType = clientType;
        this.serializable = true;
    }

    /**
     * Serializes this controller's state to persistent storage.
     * <p>
     * This method is called automatically during certain lifecycle events and can be
     * invoked manually to ensure data is persisted. The actual serialization behavior
     * depends on:
     * <ul>
     *     <li>The {@link #serializable} flag - if false, no operation is performed</li>
     *     <li>The configured {@link #serializer} - determines the storage format and location</li>
     *     <li>The controller type - different data is persisted for Store vs Keys</li>
     * </ul>
     * <p>
     * Implementations should ensure this method is safe to call multiple times and
     * handles I/O errors gracefully.
     *
     * @see #serializable()
     * @see #serializer()
     */
    public abstract void serialize();

    /**
     * Disposes of this controller and releases any associated resources.
     * <p>
     * This method is called when the controller is no longer needed, typically when:
     * <ul>
     *     <li>The WhatsApp session is being closed</li>
     *     <li>The application is shutting down</li>
     *     <li>The session is being deleted or logged out</li>
     * </ul>
     * <p>
     * Implementations should:
     * <ul>
     *     <li>Perform a final serialization of the current state</li>
     *     <li>Release any held resources (file handles, network connections, etc.)</li>
     *     <li>Clear sensitive data from memory when appropriate</li>
     * </ul>
     * <p>
     * After disposal, the controller should not be used for any further operations.
     */
    public abstract void dispose();

    /**
     * Returns the unique identifier for this controller.
     *
     * @return the non-null UUID that uniquely identifies this controller instance
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Returns the type of WhatsApp client this controller is managing.
     *
     * @return the non-null client type (WEB or MOBILE)
     * @see WhatsappClientType
     */
    public WhatsappClientType clientType() {
        return this.clientType;
    }

    /**
     * Returns the phone number associated with this controller, if available.
     * <p>
     * The phone number may not be available during initial setup phases:
     * <ul>
     *     <li>For Web clients: before QR code authentication is completed</li>
     *     <li>For Mobile clients: during the initial registration flow</li>
     * </ul>
     * <p>
     * Once authenticated, this value should be present and represent the phone number
     * in international format without the '+' prefix.
     *
     * @return an OptionalLong containing the phone number if set, empty otherwise
     */
    public OptionalLong phoneNumber() {
        return phoneNumber == null ? OptionalLong.empty() : OptionalLong.of(phoneNumber);
    }

    /**
     * Sets the phone number for this controller.
     * <p>
     * This method is typically called during the authentication process once the
     * phone number is confirmed by the WhatsApp servers. The phone number should
     * be provided in international format without the '+' prefix.
     *
     * @param phoneNumber the phone number to associate with this controller, may be null to clear
     */
    public void setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the serializer configured for this controller.
     * <p>
     * The serializer determines how and where this controller's state is persisted.
     * A null serializer indicates that no persistence has been configured.
     *
     * @return the configured serializer, or null if none is set
     * @see WhatsappStoreSerializer
     */
    public WhatsappStoreSerializer serializer() {
        return serializer;
    }

    /**
     * Sets the serializer for this controller.
     * <p>
     * Changing the serializer allows migrating between different persistence strategies
     * or storage locations. This can be done at any time during the controller's lifecycle,
     * but care should be taken to ensure data consistency when switching serializers.
     * <p>
     * Setting this to null will effectively disable persistence operations until a new
     * serializer is configured.
     *
     * @param serializer the serializer to use, may be null to disable persistence
     * @see WhatsappStoreSerializer
     */
    public void setSerializer(WhatsappStoreSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Checks whether this controller will be persisted to storage.
     * <p>
     * When this returns false, calls to {@link #serialize()} will have no effect.
     * This allows for temporary or in-memory-only sessions.
     *
     * @return true if this controller can be serialized, false otherwise
     * @see #setSerializable(boolean)
     */
    public boolean serializable() {
        return serializable;
    }

    /**
     * Sets whether this controller should be persisted to storage.
     * <p>
     * Disabling serialization (setting to false) prevents the controller from being
     * saved to persistent storage, which is useful for:
     * <ul>
     *     <li>Testing scenarios where persistence is not desired</li>
     *     <li>Temporary sessions that should not be restored</li>
     *     <li>Privacy-focused applications that avoid storing session data</li>
     * </ul>
     * <p>
     * Note: Disabling serialization does not delete existing persisted data; it only
     * prevents future serialization operations.
     *
     * @param serializable true to enable serialization, false to disable it
     */
    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }
}