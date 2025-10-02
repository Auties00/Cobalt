
package it.auties.whatsapp.controller;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.api.WhatsappWebHistoryPolicy;
import it.auties.whatsapp.model.auth.UserAgent.ReleaseChannel;
import it.auties.whatsapp.model.auth.Version;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactBuilder;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidDevice;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterBuilder;
import it.auties.whatsapp.model.newsletter.NewsletterMetadata;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.AppMetadata;
import it.auties.whatsapp.util.Clock;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Store controller manages session-scoped user data and state for WhatsApp Web and Mobile connections.
 * <p>
 * This class serves as the central repository for all user-related information during an active WhatsApp session,
 * including account details, contacts, chats, newsletters, privacy settings, and connection configuration.
 * It extends {@link Controller} to inherit serialization and lifecycle management capabilities.
 * <p>
 * The Store maintains both volatile runtime state (like media connections and listeners) and persistent data
 * (like chat history and contacts) that can be serialized between sessions. It supports both WhatsApp Web
 * and Mobile client types, adapting its behavior accordingly.
 * <p>
 * <b>Thread Safety:</b> This class uses concurrent collections ({@link ConcurrentHashMap}) for thread-safe
 * access to shared data structures. However, individual field modifications are not synchronized, so external
 * synchronization may be required when updating multiple related fields atomically.
 * <p>
 * <b>Data Categories:</b>
 * <ul>
 *     <li><b>Account Information:</b> User profile, business details, locale, and device configuration</li>
 *     <li><b>Communication Data:</b> Chats, contacts, newsletters, status updates, and messages</li>
 *     <li><b>Privacy & Security:</b> Linked devices, privacy settings, and encryption keys</li>
 *     <li><b>Session Configuration:</b> Client type, version, proxy settings, and feature flags</li>
 *     <li><b>Runtime State:</b> Media connections, event listeners, and synchronization status</li>
 * </ul>
 * <p>
 * <b>Lifecycle:</b> Store instances are typically created through {@link #of(UUID, Long, WhatsappClientType)}
 * and managed by the WhatsApp API. Call {@link #dispose()} when the session ends to properly clean up resources
 * and serialize the current state.
 *
 * @see Controller
 * @see Keys
 * @see Whatsapp
 * @see WhatsappClientType
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
public final class WhatsappStore extends Controller {
    //region Connection Configuration Fields

    /**
     * The HTTP proxy URI used for network connections, if configured.
     * <p>
     * When set, all network traffic will be routed through this proxy server.
     * The URI should follow the format: {@code http://host:port} or {@code https://host:port}
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    URI proxy;

    /**
     * The WhatsApp protocol version used by this session.
     * <p>
     * This version is automatically determined based on the {@link #device} platform
     * and is used for protocol negotiation with WhatsApp servers. The version
     * affects available features and message formats.
     *
     * @see AppMetadata#getVersion(it.auties.whatsapp.model.auth.UserAgent.PlatformType)
     */
    Version version;

    /**
     * The device information used to identify this client to WhatsApp servers.
     * <p>
     * Contains platform type, OS version, device model, and other metadata required
     * for authentication and feature availability. Different device types (web, mobile)
     * have different capabilities and protocol requirements.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.MESSAGE)
    JidDevice device;

    /**
     * The release channel for this connection (e.g., RELEASE, BETA).
     * <p>
     * Determines which version of the WhatsApp protocol and features are available.
     * Beta channels may provide early access to new features but might be less stable.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.ENUM)
    ReleaseChannel releaseChannel;

    /**
     * The unique hash identifier for the companion device (for multi-device sessions).
     * <p>
     * Used in WhatsApp's multi-device architecture to identify and validate specific
     * device pairings. This hash is generated during the initial device linking process.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    String deviceHash;

    //endregion

    //region Account & Profile Fields

    /**
     * Indicates whether this account appears online to other users.
     * <p>
     * When {@code true}, the user's online status is visible to contacts according to
     * their privacy settings. This affects presence notifications and the "last seen" indicator.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean online;

    /**
     * The locale/language code for this account (e.g., "en_US", "pt_BR").
     * <p>
     * Determines the language used for system messages and localized content.
     * The format follows ISO 639-1 (language) and ISO 3166-1 alpha-2 (country).
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String locale;

    /**
     * The display name of the user associated with this account.
     * <p>
     * This is the name shown to other WhatsApp users. It will be {@code null}
     * until the user successfully logs in. For unregistered accounts, the device
     * platform name is used as a default.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String name;

    /**
     * The verified business name for this account, if it is a verified business account.
     * <p>
     * Only populated for business accounts that have completed WhatsApp's business
     * verification process. Regular accounts will have this field as {@code null}.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.STRING)
    String verifiedName;

    /**
     * The profile picture URI for this account.
     * <p>
     * Points to the URL where the user's profile picture can be downloaded.
     * This field is {@code null} when no profile picture has been set or when
     * the user hasn't logged in yet.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    URI profilePicture;

    /**
     * The status message (about text) of the user linked to this account.
     * <p>
     * This is the personal status text displayed on the user's profile.
     * Will be {@code null} until the user logs in. Common examples include
     * "Hey there! I am using WhatsApp" or custom status messages.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    String about;

    /**
     * The WhatsApp JID of the user linked to this account.
     * <p>
     * This unique identifier represents the user's phone number in WhatsApp's
     * internal format (e.g., "1234567890@s.whatsapp.net"). It is {@code null}
     * until authentication completes successfully.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The LID for this account, used in some WhatsApp operations.
     * <p>
     * The LID is an alternative identifier used when the real phone number is not advertised.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.STRING)
    Jid lid;

    //endregion

    //region Business Account Fields

    /**
     * The physical address of the business, if this is a business account.
     * <p>
     * Contains the full street address where the business is located.
     * Only applicable to WhatsApp Business accounts.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String businessAddress;

    /**
     * The geographical longitude coordinate of the business location.
     * <p>
     * Used together with {@link #businessLatitude} to provide precise location
     * information for business accounts. Value ranges from -180.0 to +180.0 degrees.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.DOUBLE)
    Double businessLongitude;

    /**
     * The geographical latitude coordinate of the business location.
     * <p>
     * Used together with {@link #businessLongitude} to provide precise location
     * information for business accounts. Value ranges from -90.0 to +90.0 degrees.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.DOUBLE)
    Double businessLatitude;

    /**
     * A description of the business and its services.
     * <p>
     * Free-form text that business accounts use to describe what they offer.
     * This is displayed in the business profile and helps customers understand
     * the nature of the business.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String businessDescription;

    /**
     * The website URL for the business account.
     * <p>
     * Provides a link to the business's online presence. Should be a valid
     * HTTP/HTTPS URL that customers can visit for more information.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String businessWebsite;

    /**
     * The contact email address for the business account.
     * <p>
     * Used for customer inquiries and business communication outside of WhatsApp.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    String businessEmail;

    /**
     * The business category classification for this account.
     * <p>
     * Defines the type of business (e.g., Restaurant, Retail, Services).
     * Used for business discovery and filtering in WhatsApp Business features.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    BusinessCategory businessCategory;

    //endregion

    //region Communication Data Collections

    /**
     * Thread-safe map of all chats indexed by their JID.
     * <p>
     * Contains all individual and group conversations, including metadata and
     * message history. Messages within chats are stored as {@link HistorySyncMessage}
     * instances. This collection is populated during login and updated in real-time
     * as new messages arrive.
     * <p>
     * Not serialized directly via protobuf - managed through custom serialization.
     */
    final ConcurrentHashMap<Jid, Chat> chats;

    /**
     * Thread-safe map of all contacts indexed by their JID.
     * <p>
     * Includes both contacts saved in the phone's address book and WhatsApp users
     * the account has interacted with. Contact information includes names, profile
     * pictures, and status messages.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<Jid, Contact> contacts;

    /**
     * Thread-safe map of all newsletters (broadcast channels) indexed by their JID.
     * <p>
     * Newsletters are one-way communication channels where administrators can send
     * messages to subscribers. This collection stores metadata and message history
     * for each newsletter the account is subscribed to.
     * <p>
     * Not serialized directly via protobuf - managed through custom serialization.
     */
    final ConcurrentHashMap<Jid, Newsletter> newsletters;

    /**
     * Thread-safe set of all status updates (stories) visible to this account.
     * <p>
     * Contains status messages posted by contacts, which are visible for 24 hours.
     * Statuses are ephemeral and stored separately from regular chat messages.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.MESSAGE)
    final KeySetView<ChatMessageInfo, Boolean> status;

    /**
     * Thread-safe map of all active and recent calls indexed by call ID.
     * <p>
     * Stores information about voice and video calls, including call state,
     * participants, and timestamps. Call history is maintained for reference
     * and notification purposes.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, Call> calls;

    //endregion

    //region Privacy & Security Fields

    /**
     * Map of privacy settings indexed by setting type name.
     * <p>
     * Controls visibility of profile information, online status, and other
     * privacy-sensitive features. Each entry defines who can see specific
     * information (everyone, contacts only, nobody, etc.).
     *
     * @see PrivacySettingType
     * @see PrivacySettingEntry
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, PrivacySettingEntry> privacySettings;

    /**
     * Map of linked companion devices indexed by their JID, with associated key indices.
     * <p>
     * In WhatsApp's multi-device architecture, this tracks all devices that have been
     * linked to the primary account. The integer value represents the key index used
     * for encrypting messages to that device.
     * <p>
     * Only relevant for mobile API sessions acting as the primary device.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.INT32)
    LinkedHashMap<Jid, Integer> linkedDevicesKeys;

    //endregion

    //region Session Configuration & Behavior

    /**
     * Configuration properties received from WhatsApp servers.
     * <p>
     * Contains key-value pairs that control various aspects of the session behavior,
     * feature flags, and protocol settings. These properties are updated by the server
     * and may change between sessions.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    final ConcurrentHashMap<String, String> properties;

    /**
     * Whether chats should automatically unarchive when new messages arrive.
     * <p>
     * When {@code true}, receiving a message in an archived chat will automatically
     * move it back to the main chat list. When {@code false}, archived chats remain
     * archived regardless of new activity.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    boolean unarchiveChats;

    /**
     * Whether the client uses 24-hour time format.
     * <p>
     * When {@code true}, times are displayed in 24-hour format (e.g., 13:00).
     * When {@code false}, 12-hour format with AM/PM is used (e.g., 1:00 PM).
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean twentyFourHourFormat;

    /**
     * Default ephemeral timer setting for new chats.
     * <p>
     * Determines the default disappearing message timer when creating new chats.
     * When set to a non-OFF value, new chats will automatically have disappearing
     * messages enabled with the specified duration.
     *
     * @see ChatEphemeralTimer
     */
    @ProtobufProperty(index = 31, type = ProtobufType.ENUM)
    ChatEphemeralTimer newChatsEphemeralTimer;

    /**
     * Configuration for how much chat history to synchronize from WhatsApp servers.
     * <p>
     * Determines the amount of message history downloaded during initial connection
     * and history sync operations. Options range from recent messages only to full
     * history depending on storage and bandwidth preferences.
     * <p>
     * Only applicable to Web client type.
     *
     * @see WhatsappWebHistoryPolicy
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    WhatsappWebHistoryPolicy historyLength;

    /**
     * Whether presence updates should be sent automatically to WhatsApp.
     * <p>
     * When {@code true}, the client automatically updates its online/offline status
     * and sends read receipts. When {@code false}, these updates must be sent manually.
     * Disabling this prevents the "last seen" timestamp from updating automatically.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    boolean automaticPresenceUpdates;

    /**
     * Whether message read receipts should be sent automatically.
     * <p>
     * When {@code true}, read receipts (blue ticks) are automatically sent when messages
     * are processed. When {@code false}, read receipts must be sent manually. This is
     * typically {@code true} for mobile clients and configurable for web clients.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean automaticMessageReceipts;

    /**
     * Whether to verify MAC (Message Authentication Code) in app state patches.
     * <p>
     * When {@code true}, the integrity of app state synchronization patches is verified
     * using cryptographic MACs. This ensures data hasn't been tampered with but may
     * cause synchronization issues if the check fails.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    boolean checkPatchMacs;

    //endregion

    //region Synchronization State Flags

    /**
     * Indicates whether chats have been synchronized from the server.
     * <p>
     * For Web clients, this is {@code true} after history sync completes.
     * For Mobile clients, this is {@code true} after initial bootstrap.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.BOOL)
    boolean syncedChats;

    /**
     * Indicates whether contacts have been synchronized from the server.
     * <p>
     * For Web clients, this is {@code true} after history sync completes.
     * For Mobile clients, this is {@code true} after initial bootstrap.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.BOOL)
    boolean syncedContacts;

    /**
     * Indicates whether newsletters have been synchronized from the server.
     * <p>
     * On both platforms, a w:mex query is used to fetch newsletter information.
     * This flag is {@code true} after the initial newsletter sync completes.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.BOOL)
    boolean syncedNewsletters;

    /**
     * Indicates whether status updates have been synchronized from the server.
     * <p>
     * For Web clients, this is {@code true} after history sync completes.
     * For Mobile clients, this is {@code true} after initial bootstrap.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.BOOL)
    boolean syncedStatus;

    /**
     * Indicates whether web app state has been synchronized.
     * <p>
     * Web app state includes settings, starred messages, and other non-message data.
     * This flag is {@code true} after the initial app state sync completes.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.BOOL)
    boolean syncedWebAppState;

    //endregion

    //region Runtime State (Non-Serialized)

    /**
     * Set of registered event listeners for this session.
     * <p>
     * Event listeners receive callbacks for various WhatsApp events like new messages,
     * status changes, and connection events. This collection is not serialized and
     * must be repopulated after session restoration.
     *
     * @see WhatsappListener
     */
    final KeySetView<WhatsappListener, Boolean> listeners;

    /**
     * The Unix timestamp (in seconds) when this Store instance was created.
     * <p>
     * Used for tracking session age and managing time-sensitive operations.
     * Automatically set to the current time during Store creation.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.UINT64)
    final Long initializationTimeStamp;

    /**
     * The active media connection for uploading and downloading media files.
     * <p>
     * Media files (images, videos, documents) require a separate connection with
     * authentication tokens. This field is populated during session initialization
     * and may be refreshed periodically. Not serialized between sessions.
     */
    MediaConnection mediaConnection;

    /**
     * Synchronization latch for waiting on media connection availability.
     * <p>
     * Used to block operations that require a media connection until one is established.
     * The latch is counted down when {@link #mediaConnection} is set.
     */
    final CountDownLatch mediaConnectionLatch;

    //endregion

    //region Constructor

    WhatsappStore(UUID uuid, Long phoneNumber, WhatsappClientType clientType, URI proxy, boolean online, String locale, String name, String verifiedName, String businessAddress, Double businessLongitude, Double businessLatitude, String businessDescription, String businessWebsite, String businessEmail, BusinessCategory businessCategory, String deviceHash, LinkedHashMap<Jid, Integer> linkedDevicesKeys, URI profilePicture, String about, Jid jid, Jid lid, ConcurrentHashMap<String, String> properties, ConcurrentHashMap<Jid, Contact> contacts, KeySetView<ChatMessageInfo, Boolean> status, ConcurrentHashMap<String, PrivacySettingEntry> privacySettings, ConcurrentHashMap<String, Call> calls, boolean unarchiveChats, boolean twentyFourHourFormat, Long initializationTimeStamp, ChatEphemeralTimer newChatsEphemeralTimer, WhatsappWebHistoryPolicy historyLength, boolean automaticPresenceUpdates, boolean automaticMessageReceipts, ReleaseChannel releaseChannel, JidDevice device, boolean checkPatchMacs, boolean syncedChats, boolean syncedContacts, boolean syncedNewsletters, boolean syncedStatus, boolean syncedWebAppState) {
        super(uuid, phoneNumber, null, clientType);
        this.proxy = proxy;
        this.online = online;
        this.locale = locale;
        this.name = name;
        this.verifiedName = verifiedName;
        this.businessAddress = businessAddress;
        this.businessLongitude = businessLongitude;
        this.businessLatitude = businessLatitude;
        this.businessDescription = businessDescription;
        this.businessWebsite = businessWebsite;
        this.businessEmail = businessEmail;
        this.businessCategory = businessCategory;
        this.deviceHash = deviceHash;
        this.linkedDevicesKeys = Objects.requireNonNullElseGet(linkedDevicesKeys, LinkedHashMap::new);
        this.profilePicture = profilePicture;
        this.about = about;
        this.jid = jid;
        this.lid = lid;
        this.properties = Objects.requireNonNullElseGet(properties, ConcurrentHashMap::new);
        this.chats = new ConcurrentHashMap<>();
        this.contacts = Objects.requireNonNullElseGet(contacts, ConcurrentHashMap::new);
        this.status = Objects.requireNonNullElseGet(status, ConcurrentHashMap::newKeySet);
        this.newsletters = new ConcurrentHashMap<>();
        this.privacySettings = Objects.requireNonNullElseGet(privacySettings, ConcurrentHashMap::new);
        this.calls = Objects.requireNonNullElseGet(calls, ConcurrentHashMap::new);
        this.unarchiveChats = unarchiveChats;
        this.twentyFourHourFormat = twentyFourHourFormat;
        this.listeners = ConcurrentHashMap.newKeySet();
        this.initializationTimeStamp = Objects.requireNonNullElseGet(initializationTimeStamp, Clock::nowSeconds);
        this.mediaConnectionLatch = new CountDownLatch(1);
        this.newChatsEphemeralTimer = Objects.requireNonNullElse(newChatsEphemeralTimer, ChatEphemeralTimer.OFF);
        this.historyLength = Objects.requireNonNullElseGet(historyLength, () -> WhatsappWebHistoryPolicy.standard(true));
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        this.automaticMessageReceipts = automaticMessageReceipts;
        this.releaseChannel = Objects.requireNonNullElse(releaseChannel, ReleaseChannel.RELEASE);
        this.device = device;
        this.checkPatchMacs = checkPatchMacs;
        this.syncedChats = syncedChats;
        this.syncedContacts = syncedContacts;
        this.syncedNewsletters = syncedNewsletters;
        this.syncedStatus = syncedStatus;
        this.syncedWebAppState = syncedWebAppState;
    }

    /**
     * Creates a new Store instance with default configuration for the specified client type.
     * <p>
     * This factory method initializes a Store with sensible defaults appropriate for either
     * Web or Mobile client types. The store is configured but not yet connected to WhatsApp.
     *
     * @param uuid        the unique identifier for this session
     * @param phoneNumber the phone number for this account (can be null for initial pairing)
     * @param clientType  the type of WhatsApp client (WEB or MOBILE)
     * @return a new Store instance ready for connection
     */
    public static WhatsappStore of(UUID uuid, Long phoneNumber, WhatsappClientType clientType) {
        return new StoreBuilder()
                .uuid(uuid)
                .initializationTimeStamp(Clock.nowSeconds())
                .phoneNumber(phoneNumber)
                .device(clientType == WhatsappClientType.MOBILE ? JidDevice.ios(false) : JidDevice.web())
                .clientType(clientType)
                .jid(phoneNumber != null ? Jid.of(phoneNumber) : null)
                .automaticPresenceUpdates(true)
                .automaticMessageReceipts(clientType == WhatsappClientType.MOBILE)
                .build();
    }

    //endregion

    //region Contact Query Methods

    /**
     * Finds a contact by their JID.
     * <p>
     * This method supports multiple input types through the {@link JidProvider} interface,
     * including {@link Contact}, {@link Jid}, and other JID-providing types.
     *
     * @param jid the JID to search for (can be null)
     * @return an {@link Optional} containing the contact if found, empty otherwise
     */
    public Optional<Contact> findContactByJid(JidProvider jid) {
        return switch (jid) {
            case Contact contact -> Optional.of(contact);
            case null -> Optional.empty();
            default -> Optional.ofNullable(contacts.get(jid.toJid().withoutData()));
        };
    }

    /**
     * Finds the first contact whose name exactly matches the given name.
     * <p>
     * This method searches through full names, chosen names, and short names for a match.
     * The comparison is case-sensitive and must be an exact match.
     *
     * @param name the name to search for (case-sensitive)
     * @return an {@link Optional} containing the first matching contact, empty if none found
     */
    public Optional<Contact> findContactByName(String name) {
        return findContactsStream(name).findAny();
    }

    /**
     * Finds all contacts whose name exactly matches the given name.
     * <p>
     * This method searches through full names, chosen names, and short names for matches.
     * The comparison is case-sensitive and must be an exact match.
     *
     * @param name the name to search for (case-sensitive)
     * @return an immutable set of all matching contacts (empty if none found)
     */
    public Set<Contact> findContactsByName(String name) {
        return findContactsStream(name).collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Contact> findContactsStream(String name) {
        return name == null ? Stream.empty() : contacts().parallelStream()
                .filter(contact -> contact.fullName().filter(name::equals).isPresent() || contact.chosenName().filter(name::equals).isPresent() || contact.shortName().filter(name::equals).isPresent());
    }

    /**
     * Returns all contacts stored in this session.
     *
     * @return an immutable collection of all contacts
     */
    public Collection<Contact> contacts() {
        return Collections.unmodifiableCollection(contacts.values());
    }

    /**
     * Checks if a contact with the given JID exists in memory.
     *
     * @param jidProvider the JID to check (can be null)
     * @return {@code true} if the contact exists, {@code false} otherwise
     */
    public boolean hasContact(JidProvider jidProvider) {
        return jidProvider != null && contacts.get(jidProvider.toJid()) != null;
    }

    /**
     * Returns all contacts that are currently blocked.
     *
     * @return an immutable collection of blocked contacts
     */
    public Collection<Contact> blockedContacts() {
        return contacts().stream().filter(Contact::blocked).toList();
    }

    /**
     * Adds a new contact to the store using just a JID.
     * <p>
     * Creates a minimal contact with only the JID populated. Additional information
     * can be added later through contact updates.
     *
     * @param jid the JID of the contact to add
     * @return the newly created contact
     */
    public Contact addContact(Jid jid) {
        var newContact = new ContactBuilder()
                .jid(jid)
                .build();
        return addContact(newContact);
    }

    /**
     * Adds or updates a contact in the store.
     * <p>
     * If a contact with the same JID already exists, it will be replaced with the new one.
     *
     * @param contact the contact to add or update
     * @return the contact that was added (same as the parameter)
     */
    public Contact addContact(Contact contact) {
        contacts.put(contact.jid(), contact);
        return contact;
    }

    /**
     * Removes a contact from the store.
     *
     * @param contactJid the JID of the contact to remove
     * @return an {@link Optional} containing the removed contact if it existed, empty otherwise
     */
    public Optional<Contact> removeContact(JidProvider contactJid) {
        return Optional.ofNullable(contacts.remove(contactJid.toJid()));
    }

    //endregion

    //region Chat Query Methods

    /**
     * Finds a chat by its JID.
     * <p>
     * This method supports multiple input types through the {@link JidProvider} interface,
     * including {@link Chat}, {@link Jid}, and other JID-providing types.
     *
     * @param jid the JID to search for (can be null)
     * @return an {@link Optional} containing the chat if found, empty otherwise
     */
    public Optional<Chat> findChatByJid(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }

        if (jid instanceof Chat chat) {
            return Optional.of(chat);
        }

        return Optional.ofNullable(chats.get(jid.toJid()));
    }

    /**
     * Finds the first chat whose name exactly matches the given name (case-insensitive).
     *
     * @param name the chat name to search for
     * @return an {@link Optional} containing the first matching chat, empty if none found
     */
    public Optional<Chat> findChatByName(String name) {
        return findChatsByNameStream(name).findAny();
    }

    /**
     * Finds all chats whose name exactly matches the given name (case-insensitive).
     *
     * @param name the chat name to search for
     * @return an immutable set of all matching chats (empty if none found)
     */
    public Set<Chat> findChatsByName(String name) {
        return findChatsByNameStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the first chat that matches the provided predicate function.
     *
     * @param function a predicate that returns {@code true} for matching chats
     * @return an {@link Optional} containing the first matching chat, empty if none found
     */
    public Optional<Chat> findChatBy(Function<Chat, Boolean> function) {
        return chats.values().parallelStream()
                .filter(function::apply)
                .findFirst();
    }

    /**
     * Finds all chats that match the provided predicate function.
     *
     * @param function a predicate that returns {@code true} for matching chats
     * @return an immutable set of all matching chats (empty if none found)
     */
    public Set<Chat> findChatsBy(Function<Chat, Boolean> function) {
        return chats.values()
                .stream()
                .filter(function::apply)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Chat> findChatsByNameStream(String name) {
        return name == null ? Stream.empty() : chats.values()
                .parallelStream()
                .filter(chat -> chat.name().equalsIgnoreCase(name));
    }

    /**
     * Returns all chats sorted from newest to oldest by timestamp.
     *
     * @return an immutable list of all chats in descending timestamp order
     */
    public List<Chat> chats() {
        return chats.values()
                .stream()
                .sorted(Comparator.comparingLong(Chat::timestampSeconds).reversed())
                .toList();
    }

    /**
     * Returns all chats that are pinned to the top, sorted newest to oldest.
     *
     * @return an immutable list of pinned chats in descending pin timestamp order
     */
    public List<Chat> pinnedChats() {
        return chats.values()
                .parallelStream()
                .filter(Chat::isPinned)
                .sorted(Comparator.comparingLong(Chat::pinnedTimestampSeconds).reversed())
                .toList();
    }

    /**
     * Adds a new chat to the store with only a JID.
     * <p>
     * Creates a minimal chat with only the JID populated. Additional information
     * will be updated as messages are received.
     *
     * @param chatJid the JID of the chat to add
     * @return the newly created chat
     */
    public Chat addNewChat(Jid chatJid) {
        var chat = new ChatBuilder()
                .jid(chatJid)
                .build();
        addChat(chat);
        return chat;
    }

    /**
     * Adds or updates a chat in the store.
     * <p>
     * If a chat with the same JID already exists, this method merges the message history
     * and preserves the chat name if the new chat doesn't have one. For user chats with names,
     * the name is also synchronized to the corresponding contact.
     *
     * @param chat the chat to add or update
     * @return an {@link Optional} containing the old chat if it was replaced, empty otherwise
     */
    public Optional<Chat> addChat(Chat chat) {
        if (chat.hasName() && chat.jid().hasServer(JidServer.user())) {
            var contact = findContactByJid(chat.jid())
                    .orElseGet(() -> addContact(chat.jid()));
            contact.setFullName(chat.name());
        }
        var oldChat = chats.get(chat.jid());
        if (oldChat != null) {
            if (oldChat.hasName() && !chat.hasName()) {
                chat.setName(oldChat.name()); // Coming from contact actions
            }
            joinMessages(chat, oldChat);
        }
        return Optional.ofNullable(chats.put(chat.jid(), chat));
    }

    private void joinMessages(Chat chat, Chat oldChat) {
        var newChatTimestamp = chat.newestMessage()
                .map(message -> message.timestampSeconds().orElse(0L))
                .orElse(0L);
        var oldChatTimestamp = oldChat.newestMessage()
                .map(message -> message.timestampSeconds().orElse(0L))
                .orElse(0L);
        if (newChatTimestamp <= oldChatTimestamp) {
            chat.addMessages(oldChat.messages());
            return;
        }
        chat.addOldMessages(chat.messages());
    }

    /**
     * Removes a chat from the store.
     *
     * @param chatJid the JID of the chat to remove
     * @return an {@link Optional} containing the removed chat if it existed, empty otherwise
     */
    public Optional<Chat> removeChat(JidProvider chatJid) {
        return Optional.ofNullable(chats.remove(chatJid.toJid()));
    }

    //endregion

    //region Newsletter Query Methods

    /**
     * Finds a newsletter by its JID.
     * <p>
     * This method supports multiple input types through the {@link JidProvider} interface,
     * including {@link Newsletter}, {@link Jid}, and other JID-providing types.
     *
     * @param jid the JID to search for (can be null)
     * @return an {@link Optional} containing the newsletter if found, empty otherwise
     */
    public Optional<Newsletter> findNewsletterByJid(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }

        if (jid instanceof Newsletter newsletter) {
            return Optional.of(newsletter);
        }

        return Optional.ofNullable(newsletters.get(jid.toJid()));
    }

    /**
     * Finds the first newsletter whose name exactly matches the given name.
     *
     * @param name the newsletter name to search for
     * @return an {@link Optional} containing the first matching newsletter, empty if none found
     */
    public Optional<Newsletter> findNewsletterByName(String name) {
        return findNewslettersByNameStream(name).findAny();
    }

    /**
     * Finds all newsletters whose name exactly matches the given name.
     *
     * @param name the newsletter name to search for
     * @return an immutable set of all matching newsletters (empty if none found)
     */
    public Set<Newsletter> findNewslettersByName(String name) {
        return findNewslettersByNameStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the first newsletter that matches the provided predicate function.
     *
     * @param function a predicate that returns {@code true} for matching newsletters
     * @return an {@link Optional} containing the first matching newsletter, empty if none found
     */
    public Optional<Newsletter> findNewsletterBy(Function<Newsletter, Boolean> function) {
        return newsletters.values()
                .parallelStream()
                .filter(function::apply)
                .findFirst();
    }

    private Stream<Newsletter> findNewslettersByNameStream(String name) {
        return name == null ? Stream.empty() : newsletters.values()
                .parallelStream()
                .filter(newsletter -> hasNewsletterName(newsletter, name));
    }

    private static boolean hasNewsletterName(Newsletter newsletter, String name) {
        return newsletter.metadata()
                .flatMap(NewsletterMetadata::name)
                .filter(entry -> Objects.equals(entry.text(), name))
                .isPresent();
    }

    /**
     * Returns all newsletters subscribed by this account.
     *
     * @return an immutable collection of all newsletters
     */
    public Collection<Newsletter> newsletters() {
        return Collections.unmodifiableCollection(newsletters.values());
    }

    /**
     * Adds a new newsletter to the store with only a JID.
     * <p>
     * Creates a minimal newsletter with only the JID populated. Additional information
     * will be updated as metadata is received.
     *
     * @param newseltterJid the JID of the newsletter to add
     * @return the newly created newsletter
     */
    public Newsletter addNewNewsletter(Jid newseltterJid) {
        var newsletter = new NewsletterBuilder()
                .jid(newseltterJid)
                .build();
        addNewsletter(newsletter);
        return newsletter;
    }

    /**
     * Adds or updates a newsletter in the store.
     * <p>
     * If a newsletter with the same JID already exists, it will be replaced with the new one.
     *
     * @param newsletter the newsletter to add or update
     * @return an {@link Optional} containing the old newsletter if it was replaced, empty otherwise
     */
    public Optional<Newsletter> addNewsletter(Newsletter newsletter) {
        return Optional.ofNullable(newsletters.put(newsletter.jid(), newsletter));
    }

    /**
     * Removes a newsletter from the store.
     *
     * @param newsletterJid the JID of the newsletter to remove
     * @return an {@link Optional} containing the removed newsletter if it existed, empty otherwise
     */
    public Optional<Newsletter> removeNewsletter(JidProvider newsletterJid) {
        return Optional.ofNullable(newsletters.remove(newsletterJid.toJid()));
    }

    //endregion

    //region Message Query Methods

    /**
     * Finds a message by its key in the appropriate chat.
     * <p>
     * The message key contains both the chat JID and message ID, allowing for efficient lookup.
     *
     * @param key the message key to search for (can be null)
     * @return an {@link Optional} containing the message if found, empty otherwise
     */
    public Optional<ChatMessageInfo> findMessageByKey(ChatMessageKey key) {
        return Optional.ofNullable(key)
                .map(ChatMessageKey::chatJid)
                .flatMap(this::findChatByJid)
                .flatMap(chat -> findMessageById(chat, key.id()));
    }

    /**
     * Finds a message by ID in a specific chat, newsletter, or status.
     * <p>
     * This method automatically determines the message location based on the provider type
     * and searches the appropriate collection.
     *
     * @param provider the chat, newsletter, contact, or JID where the message should be searched
     * @param id       the message ID to search for (can be null)
     * @return an {@link Optional} containing the message if found, empty otherwise
     */
    public Optional<? extends MessageInfo> findMessageById(JidProvider provider, String id) {
        if (provider == null || id == null) {
            return Optional.empty();
        }

        return switch (provider) {
            case Chat chat -> findMessageById(chat, id);
            case Newsletter newsletter -> findMessageById(newsletter, id);
            case Contact contact -> findChatByJid(contact.jid())
                    .flatMap(chat -> findMessageById(chat, id));
            case Jid contactJid -> {
                if (contactJid.server().type() == JidServer.Type.NEWSLETTER) {
                    yield findNewsletterByJid(contactJid)
                            .flatMap(newsletter -> findMessageById(newsletter, id));
                } else if (Jid.statusBroadcastAccount().equals(contactJid)) {
                    yield status.stream()
                            .filter(entry -> Objects.equals(entry.chatJid(), provider.toJid()) && Objects.equals(entry.id(), id))
                            .findFirst();
                } else {
                    yield findChatByJid(contactJid)
                            .flatMap(chat -> findMessageById(chat, id));
                }
            }
            case JidServer jidServer -> findChatByJid(jidServer.toJid())
                    .flatMap(chat -> findMessageById(chat, id));
        };
    }

    /**
     * Finds a message by ID in a specific chat.
     * <p>
     * Searches through all messages in the chat's history for a matching ID.
     *
     * @param chat the chat to search in
     * @param id   the message ID to search for (can be null)
     * @return an {@link Optional} containing the message if found, empty otherwise
     */
    public Optional<ChatMessageInfo> findMessageById(Chat chat, String id) {
        return chat.messages()
                .parallelStream()
                .map(HistorySyncMessage::messageInfo)
                .filter(message -> Objects.equals(message.key().id(), id))
                .findAny();
    }

    /**
     * Finds a message by ID in a specific newsletter.
     * <p>
     * Searches through all messages in the newsletter for a matching ID or server ID.
     *
     * @param newsletter the newsletter to search in
     * @param id         the message ID or server ID to search for (can be null)
     * @return an {@link Optional} containing the message if found, empty otherwise
     */
    public Optional<NewsletterMessageInfo> findMessageById(Newsletter newsletter, String id) {
        return newsletter.messages()
                .parallelStream()
                .filter(entry -> Objects.equals(id, entry.id()) || Objects.equals(id, String.valueOf(entry.serverId())))
                .findFirst();
    }

    /**
     * Returns all starred messages across all chats.
     * <p>
     * Collects messages that have been marked as starred (important) by the user.
     *
     * @return an immutable list of all starred messages
     */
    public List<ChatMessageInfo> starredMessages() {
        return chats().parallelStream().map(Chat::starredMessages).flatMap(Collection::stream).toList();
    }

    //endregion

    //region Status Query Methods

    /**
     * Returns all status updates visible to this account.
     * <p>
     * Status updates (stories) are ephemeral messages visible for 24 hours.
     *
     * @return an immutable collection of all status updates
     */
    public Collection<ChatMessageInfo> status() {
        return Collections.unmodifiableCollection(status);
    }

    /**
     * Finds all status updates posted by a specific contact.
     *
     * @param jid the JID of the status sender
     * @return an immutable list of all status updates from that sender
     */
    public Collection<ChatMessageInfo> findStatusBySender(JidProvider jid) {
        return status.stream()
                .filter(entry -> Objects.equals(entry.chatJid(), jid))
                .toList();
    }

    /**
     * Adds a status update to the store.
     *
     * @param info the status message to add
     */
    public void addStatus(ChatMessageInfo info) {
        status.add(info);
    }

    //endregion

    //region Call Management Methods

    /**
     * Finds a call by its unique ID.
     *
     * @param callId the call ID to search for (can be null)
     * @return an {@link Optional} containing the call if found, empty otherwise
     */
    public Optional<Call> findCallById(String callId) {
        return callId == null ? Optional.empty() : Optional.ofNullable(calls.get(callId));
    }

    /**
     * Returns all calls (active and historical).
     *
     * @return an immutable collection of all calls
     */
    public Collection<Call> calls() {
        return Collections.unmodifiableCollection(calls.values());
    }

    /**
     * Adds a call to the store.
     *
     * @param call the call to add
     * @return an {@link Optional} containing the old call if one with the same ID was replaced
     */
    public Optional<Call> addCall(Call call) {
        return Optional.ofNullable(calls.put(call.id(), call));
    }

    //endregion

    //region Privacy Settings Methods

    /**
     * Returns all configured privacy settings.
     *
     * @return an immutable collection of all privacy setting patches
     */
    public Collection<PrivacySettingEntry> privacySettings() {
        return privacySettings.values();
    }

    /**
     * Finds the privacy setting entry for a specific type.
     *
     * @param type the privacy setting type to query
     * @return the privacy setting entry for that type
     */
    public PrivacySettingEntry findPrivacySetting(PrivacySettingType type) {
        return privacySettings.get(type.name());
    }

    /**
     * Sets or updates a privacy setting.
     *
     * @param type  the privacy setting type
     * @param entry the new privacy setting entry
     * @return the previous privacy setting entry for this type, or null if none existed
     */
    public PrivacySettingEntry addPrivacySetting(PrivacySettingType type, PrivacySettingEntry entry) {
        return privacySettings.put(type.name(), entry);
    }

    //endregion

    //region Linked Devices Methods

    /**
     * Returns all linked companion devices with their key indices.
     * <p>
     * This map contains all devices linked to the primary account using WhatsApp's
     * multi-device feature. Only relevant for mobile API sessions.
     *
     * @return an immutable map of device JIDs to key indices
     */
    public Map<Jid, Integer> linkedDevicesKeys() {
        return Collections.unmodifiableMap(linkedDevicesKeys);
    }

    /**
     * Returns the JIDs of all linked companion devices.
     *
     * @return an immutable collection of linked device JIDs
     */
    public Collection<Jid> linkedDevices() {
        return Collections.unmodifiableCollection(linkedDevicesKeys.keySet());
    }

    /**
     * Registers a new companion device.
     * <p>
     * <b>Note:</b> This method should only be used with the mobile API.
     *
     * @param companion the JID of the companion device
     * @param keyId     the encryption key ID for this device
     * @return an {@link Optional} containing the old key ID if this device was already registered
     */
    public Optional<Integer> addLinkedDevice(Jid companion, int keyId) {
        return Optional.ofNullable(linkedDevicesKeys.put(companion, keyId));
    }

    /**
     * Removes a linked companion device.
     * <p>
     * <b>Note:</b> This method should only be used with the mobile API.
     *
     * @param companion the JID of the companion device to remove
     * @return an {@link Optional} containing the removed key ID if the device existed
     */
    public Optional<Integer> removeLinkedCompanion(Jid companion) {
        return Optional.ofNullable(linkedDevicesKeys.remove(companion));
    }

    /**
     * Removes all linked companion devices.
     */
    public void removeLinkedCompanions() {
        linkedDevicesKeys.clear();
    }

    //endregion

    //region Properties Management

    /**
     * Returns all configuration properties received from WhatsApp servers.
     *
     * @return an immutable map of property keys to values
     */
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Adds or updates multiple configuration properties.
     *
     * @param properties the properties to add or update
     */
    public void addProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    //endregion

    //region Listener Management

    /**
     * Returns all registered event listeners.
     *
     * @return an immutable collection of listeners
     */
    public Collection<WhatsappListener> listeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Registers a new event listener.
     *
     * @param listener the listener to register
     */
    public void addListener(WhatsappListener listener) {
        listeners.add(listener);
    }

    /**
     * Registers multiple event listeners.
     *
     * @param listeners the listeners to register
     */
    public void addListeners(Collection<WhatsappListener> listeners) {
        this.listeners.addAll(listeners);
    }

    /**
     * Removes an event listener.
     *
     * @param listener the listener to remove
     * @return {@code true} if the listener was registered and removed, {@code false} otherwise
     */
    public boolean removeListener(WhatsappListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Removes all registered event listeners.
     */
    public void removeListeners() {
        listeners.clear();
    }

    //endregion

    //region Media Connection Methods

    /**
     * Gets the media connection, waiting up to 1 minute if necessary.
     * <p>
     * This method blocks until a media connection is available or the default timeout expires.
     *
     * @return the media connection
     * @throws RuntimeException if the timeout expires before a connection is available
     */
    public MediaConnection mediaConnection() {
        return mediaConnection(Duration.ofMinutes(1));
    }

    /**
     * Gets the media connection, waiting up to the specified timeout if necessary.
     * <p>
     * This method blocks until a media connection is available or the timeout expires.
     *
     * @param timeout the maximum time to wait for a connection
     * @return the media connection
     * @throws RuntimeException if the timeout expires or the thread is interrupted
     */
    public MediaConnection mediaConnection(Duration timeout) {
        try {
            var result = mediaConnectionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!result) {
                throw new RuntimeException("Cannot get media connection");
            }
            return mediaConnection;
        } catch (InterruptedException exception) {
            throw new RuntimeException("Cannot lock on media connection", exception);
        }
    }

    /**
     * Checks if a media connection has been established.
     *
     * @return {@code true} if a media connection exists, {@code false} otherwise
     */
    public boolean hasMediaConnection() {
        return mediaConnection != null;
    }

    /**
     * Sets the media connection and notifies all waiting threads.
     *
     * @param mediaConnection the media connection to set
     */
    public void setMediaConnection(MediaConnection mediaConnection) {
        this.mediaConnection = mediaConnection;
        mediaConnectionLatch.countDown();
    }

    //endregion

    //region Lifecycle & Serialization

    /**
     * Disposes of this Store, serializing its state and releasing resources.
     * <p>
     * Call this method when the session is ending to ensure data is persisted
     * and resources are properly cleaned up.
     */
    public void dispose() {
        serialize();
        mediaConnectionLatch.countDown();
    }

    @Override
    public void serialize() {
        if(serializable) {
            serializer.serializeStore(this);
        }
    }

    //endregion

    //region Accessor Methods (Getters)

    /**
     * Returns the WhatsApp protocol version for this session.
     * <p>
     * If not explicitly set, the version is determined from the device platform.
     *
     * @return the protocol version
     */
    public Version version() {
        return Objects.requireNonNullElseGet(version, () -> version = AppMetadata.getVersion(device.platform()));
    }

    /**
     * Returns whether this account is currently showing as online.
     *
     * @return {@code true} if online, {@code false} otherwise
     */
    public boolean online() {
        return this.online;
    }

    /**
     * Returns the locale setting for this account.
     *
     * @return an {@link Optional} containing the locale if set, empty otherwise
     */
    public Optional<String> locale() {
        return Optional.ofNullable(this.locale);
    }

    /**
     * Returns the display name for this account.
     * <p>
     * If no name is set, returns the device platform name as a fallback.
     *
     * @return the display name
     */
    public String name() {
        if(name == null) {
            return device.platform().platformName();
        }else {
            return name;
        }
    }

    /**
     * Returns the verified business name for this account.
     *
     * @return an {@link Optional} containing the verified name if available, empty otherwise
     */
    public Optional<String> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }

    /**
     * Returns the device hash for this session.
     *
     * @return an {@link Optional} containing the device hash if set, empty otherwise
     */
    public Optional<String> deviceHash() {
        return Optional.ofNullable(this.deviceHash);
    }

    /**
     * Returns the profile picture URI for this account.
     *
     * @return an {@link Optional} containing the profile picture URI if set, empty otherwise
     */
    public Optional<URI> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    /**
     * Returns the about/status text for this account.
     *
     * @return an {@link Optional} containing the about text if set, empty otherwise
     */
    public Optional<String> about() {
        return Optional.ofNullable(this.about);
    }

    /**
     * Returns the user JID for this account.
     *
     * @return an {@link Optional} containing the JID if logged in, empty otherwise
     */
    public Optional<Jid> jid() {
        return Optional.ofNullable(this.jid);
    }

    /**
     * Returns the LID for this account.
     *
     * @return an {@link Optional} containing the LID if available, empty otherwise
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(this.lid);
    }

    /**
     * Returns the business address for this account.
     *
     * @return an {@link Optional} containing the address if this is a business account, empty otherwise
     */
    public Optional<String> businessAddress() {
        return Optional.ofNullable(businessAddress);
    }

    /**
     * Returns the business location longitude.
     *
     * @return an {@link Optional} containing the longitude if this is a business account, empty otherwise
     */
    public Optional<Double> businessLongitude() {
        return Optional.ofNullable(businessLongitude);
    }

    /**
     * Returns the business location latitude.
     *
     * @return an {@link Optional} containing the latitude if this is a business account, empty otherwise
     */
    public Optional<Double> businessLatitude() {
        return Optional.ofNullable(businessLatitude);
    }

    /**
     * Returns the business description.
     *
     * @return an {@link Optional} containing the description if this is a business account, empty otherwise
     */
    public Optional<String> businessDescription() {
        return Optional.ofNullable(businessDescription);
    }

    /**
     * Returns the business website.
     *
     * @return an {@link Optional} containing the website if this is a business account, empty otherwise
     */
    public Optional<String> businessWebsite() {
        return Optional.ofNullable(businessWebsite);
    }

    /**
     * Returns the business contact email.
     *
     * @return an {@link Optional} containing the email if this is a business account, empty otherwise
     */
    public Optional<String> businessEmail() {
        return Optional.ofNullable(businessEmail);
    }

    /**
     * Returns the business category.
     *
     * @return an {@link Optional} containing the category if this is a business account, empty otherwise
     */
    public Optional<BusinessCategory> businessCategory() {
        return Optional.ofNullable(businessCategory);
    }

    /**
     * Returns the proxy URI used for connections.
     *
     * @return an {@link Optional} containing the proxy URI if configured, empty otherwise
     */
    public Optional<URI> proxy() {
        return Optional.ofNullable(proxy);
    }

    /**
     * Returns whether archived chats should automatically unarchive when new messages arrive.
     *
     * @return {@code true} if unarchiving is enabled, {@code false} otherwise
     */
    public boolean unarchiveChats() {
        return this.unarchiveChats;
    }

    /**
     * Returns whether the 24-hour time format is enabled.
     *
     * @return {@code true} if using 24-hour format, {@code false} for 12-hour format
     */
    public boolean twentyFourHourFormat() {
        return this.twentyFourHourFormat;
    }

    /**
     * Returns the timestamp when this Store was initialized.
     *
     * @return the initialization timestamp in seconds since epoch
     */
    public long initializationTimeStamp() {
        return this.initializationTimeStamp;
    }

    /**
     * Returns the default ephemeral timer for new chats.
     *
     * @return the default ephemeral timer setting
     */
    public ChatEphemeralTimer newChatsEphemeralTimer() {
        return this.newChatsEphemeralTimer;
    }

    /**
     * Returns the web history synchronization policy.
     *
     * @return the history synchronization policy
     */
    public WhatsappWebHistoryPolicy webHistorySetting() {
        return this.historyLength;
    }

    /**
     * Returns whether automatic presence updates are enabled.
     *
     * @return {@code true} if automatic presence updates are enabled, {@code false} otherwise
     */
    public boolean automaticPresenceUpdates() {
        return this.automaticPresenceUpdates;
    }

    /**
     * Returns whether automatic message read receipts are enabled.
     *
     * @return {@code true} if automatic read receipts are enabled, {@code false} otherwise
     */
    public boolean automaticMessageReceipts() {
        return automaticPresenceUpdates;
    }

    /**
     * Returns the release channel for this connection.
     *
     * @return the release channel (RELEASE, BETA, etc.)
     */
    public ReleaseChannel releaseChannel() {
        return this.releaseChannel;
    }

    /**
     * Returns the device information for this session.
     *
     * @return the device information
     */
    public JidDevice device() {
        return device;
    }

    /**
     * Returns whether MAC verification is enabled for app state patches.
     *
     * @return {@code true} if MAC checking is enabled, {@code false} otherwise
     */
    public boolean checkPatchMacs() {
        return this.checkPatchMacs;
    }

    /**
     * Returns whether chats have been synchronized.
     *
     * @return {@code true} if chats are synced, {@code false} otherwise
     */
    public boolean syncedChats() {
        return syncedChats;
    }

    /**
     * Returns whether contacts have been synchronized.
     *
     * @return {@code true} if contacts are synced, {@code false} otherwise
     */
    public boolean syncedContacts() {
        return syncedContacts;
    }

    /**
     * Returns whether newsletters have been synchronized.
     *
     * @return {@code true} if newsletters are synced, {@code false} otherwise
     */
    public boolean syncedNewsletters() {
        return syncedNewsletters;
    }

    /**
     * Returns whether status updates have been synchronized.
     *
     * @return {@code true} if status is synced, {@code false} otherwise
     */
    public boolean syncedStatus() {
        return syncedStatus;
    }

    /**
     * Returns whether web app state has been synchronized.
     *
     * @return {@code true} if web app state is synced, {@code false} otherwise
     */
    public boolean syncedWebAppState() {
        return syncedWebAppState;
    }

    //endregion

    //region Mutator Methods (Setters)

    /**
     * Sets the HTTP proxy URI for network connections.
     *
     * @param proxy the proxy URI to use, or null to disable proxy
     */
    public void setProxy(URI proxy) {
        this.proxy = proxy;
    }

    /**
     * Sets whether this account appears online to other users.
     *
     * @param online {@code true} to appear online, {@code false} to appear offline
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Sets the locale for this account.
     *
     * @param locale the locale code (e.g., "en_US", "pt_BR")
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Sets the display name for this account.
     *
     * @param name the new display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the verified business name for this account.
     *
     * @param verifiedName the verified business name
     */
    public void setVerifiedName(String verifiedName) {
        this.verifiedName = verifiedName;
    }

    /**
     * Sets the business physical address.
     *
     * @param businessAddress the business address
     */
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    /**
     * Sets the business location longitude.
     *
     * @param businessLongitude the longitude coordinate (-180.0 to +180.0)
     */
    public void setBusinessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
    }

    /**
     * Sets the business location latitude.
     *
     * @param businessLatitude the latitude coordinate (-90.0 to +90.0)
     */
    public void setBusinessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
    }

    /**
     * Sets the business description text.
     *
     * @param businessDescription the business description
     */
    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    /**
     * Sets the business website URL.
     *
     * @param businessWebsite the business website URL
     */
    public void setBusinessWebsite(String businessWebsite) {
        this.businessWebsite = businessWebsite;
    }

    /**
     * Sets the business contact email address.
     *
     * @param businessEmail the business email address
     */
    public void setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
    }

    /**
     * Sets the business category classification.
     *
     * @param businessCategory the business category
     */
    public void setBusinessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
    }

    /**
     * Sets the device hash for this session.
     *
     * @param deviceHash the companion device hash identifier
     */
    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    /**
     * Sets the map of linked companion devices with their key indices.
     *
     * @param linkedDevicesKeys the map of device JIDs to key indices
     */
    public void setLinkedDevicesKeys(LinkedHashMap<Jid, Integer> linkedDevicesKeys) {
        this.linkedDevicesKeys = linkedDevicesKeys;
    }

    /**
     * Sets the profile picture URI for this account.
     *
     * @param profilePicture the profile picture URI
     */
    public void setProfilePicture(URI profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Sets the about/status text for this account.
     *
     * @param about the new about text
     */
    public void setAbout(String about) {
        this.about = about;
    }

    /**
     * Sets the user JID for this account.
     *
     * @param jid the user's WhatsApp JID
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * Sets the LID for this account.
     *
     * @param lid the LID identifier
     */
    public void setLid(Jid lid) {
        this.lid = lid;
    }

    /**
     * Sets whether archived chats should automatically unarchive when receiving new messages.
     *
     * @param unarchiveChats {@code true} to enable automatic unarchiving, {@code false} to keep archived
     */
    public void setUnarchiveChats(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
    }

    /**
     * Sets whether to use 24-hour time format.
     *
     * @param twentyFourHourFormat {@code true} for 24-hour format, {@code false} for 12-hour format
     */
    public void setTwentyFourHourFormat(boolean twentyFourHourFormat) {
        this.twentyFourHourFormat = twentyFourHourFormat;
    }

    /**
     * Sets the default ephemeral timer for new chats.
     *
     * @param newChatsEphemeralTimer the default ephemeral timer setting
     */
    public void setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer) {
        this.newChatsEphemeralTimer = newChatsEphemeralTimer;
    }

    /**
     * Sets the web history synchronization policy.
     *
     * @param whatsappWebHistoryPolicy the history synchronization policy
     */
    public void setWebHistorySetting(WhatsappWebHistoryPolicy whatsappWebHistoryPolicy) {
        this.historyLength = whatsappWebHistoryPolicy;
    }

    /**
     * Sets whether automatic presence updates are enabled.
     *
     * @param automaticPresenceUpdates {@code true} to enable automatic presence updates, {@code false} to disable
     */
    public void setAutomaticPresenceUpdates(boolean automaticPresenceUpdates) {
        this.automaticPresenceUpdates = automaticPresenceUpdates;
    }

    /**
     * Sets whether automatic message read receipts are enabled.
     *
     * @param automaticMessageReceipts {@code true} to enable automatic read receipts, {@code false} to disable
     */
    public void setAutomaticMessageReceipts(boolean automaticMessageReceipts) {
        this.automaticMessageReceipts = automaticMessageReceipts;
    }

    /**
     * Sets the release channel for this connection.
     *
     * @param releaseChannel the release channel (RELEASE, BETA, etc.)
     */
    public void setReleaseChannel(ReleaseChannel releaseChannel) {
        this.releaseChannel = releaseChannel;
    }

    /**
     * Sets the device information for this session.
     * <p>
     * Changing the device type will reset the cached protocol version.
     *
     * @param device the new device information (must not be null)
     * @throws NullPointerException if device is null
     */
    public void setDevice(JidDevice device) {
        if(!Objects.equals(device(), device)) {
            this.device = Objects.requireNonNull(device, "The device cannot be null");
            this.version = null;
        }
    }

    /**
     * Sets whether MAC verification is enabled for app state patches.
     *
     * @param checkPatchMacs {@code true} to enable MAC checking, {@code false} to disable
     */
    public void setCheckPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
    }

    /**
     * Sets whether chats have been synchronized.
     *
     * @param syncedChats {@code true} if chats are synced, {@code false} otherwise
     */
    public void setSyncedChats(boolean syncedChats) {
        this.syncedChats = syncedChats;
    }

    /**
     * Sets whether contacts have been synchronized.
     *
     * @param syncedContacts {@code true} if contacts are synced, {@code false} otherwise
     */
    public void setSyncedContacts(boolean syncedContacts) {
        this.syncedContacts = syncedContacts;
    }

    /**
     * Sets whether newsletters have been synchronized.
     *
     * @param syncedNewsletters {@code true} if newsletters are synced, {@code false} otherwise
     */
    public void setSyncedNewsletters(boolean syncedNewsletters) {
        this.syncedNewsletters = syncedNewsletters;
    }

    /**
     * Sets whether status updates have been synchronized.
     *
     * @param syncedStatus {@code true} if status is synced, {@code false} otherwise
     */
    public void setSyncedStatus(boolean syncedStatus) {
        this.syncedStatus = syncedStatus;
    }

    /**
     * Sets whether web app state has been synchronized.
     *
     * @param syncedWebAppState {@code true} if web app state is synced, {@code false} otherwise
     */
    public void setSyncedWebAppState(boolean syncedWebAppState) {
        this.syncedWebAppState = syncedWebAppState;
    }

    //endregion

    //region Object Overrides

    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappStore store &&
                online == store.online &&
                unarchiveChats == store.unarchiveChats &&
                twentyFourHourFormat == store.twentyFourHourFormat &&
                automaticPresenceUpdates == store.automaticPresenceUpdates &&
                automaticMessageReceipts == store.automaticMessageReceipts &&
                checkPatchMacs == store.checkPatchMacs &&
                Objects.equals(proxy, store.proxy) &&
                Objects.equals(version, store.version) &&
                Objects.equals(locale, store.locale) &&
                Objects.equals(name, store.name) &&
                Objects.equals(verifiedName, store.verifiedName) &&
                Objects.equals(businessAddress, store.businessAddress) &&
                Objects.equals(businessLongitude, store.businessLongitude) &&
                Objects.equals(businessLatitude, store.businessLatitude) &&
                Objects.equals(businessDescription, store.businessDescription) &&
                Objects.equals(businessWebsite, store.businessWebsite) &&
                Objects.equals(businessEmail, store.businessEmail) &&
                Objects.equals(businessCategory, store.businessCategory) &&
                Objects.equals(deviceHash, store.deviceHash) &&
                Objects.equals(linkedDevicesKeys, store.linkedDevicesKeys) &&
                Objects.equals(profilePicture, store.profilePicture) &&
                Objects.equals(about, store.about) &&
                Objects.equals(jid, store.jid) &&
                Objects.equals(lid, store.lid) &&
                Objects.equals(properties, store.properties) &&
                Objects.equals(contacts, store.contacts) &&
                Objects.equals(status, store.status) &&
                Objects.equals(privacySettings, store.privacySettings) &&
                Objects.equals(calls, store.calls) &&
                Objects.equals(initializationTimeStamp, store.initializationTimeStamp) &&
                newChatsEphemeralTimer == store.newChatsEphemeralTimer &&
                Objects.equals(historyLength, store.historyLength) &&
                releaseChannel == store.releaseChannel &&
                Objects.equals(device, store.device) &&
                syncedChats == store.syncedChats &&
                syncedContacts == store.syncedContacts &&
                syncedNewsletters == store.syncedNewsletters &&
                syncedStatus == store.syncedStatus &&
                syncedWebAppState == store.syncedWebAppState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(proxy, version, online, locale, name, verifiedName, businessAddress, businessLongitude, businessLatitude, businessDescription, businessWebsite, businessEmail, businessCategory, deviceHash, linkedDevicesKeys, profilePicture, about, jid, lid, properties, contacts, status, privacySettings, calls, unarchiveChats, twentyFourHourFormat, initializationTimeStamp, newChatsEphemeralTimer, historyLength, automaticPresenceUpdates, automaticMessageReceipts, releaseChannel, device, checkPatchMacs, syncedChats, syncedContacts, syncedNewsletters, syncedStatus, syncedWebAppState);
    }

    //endregion
}