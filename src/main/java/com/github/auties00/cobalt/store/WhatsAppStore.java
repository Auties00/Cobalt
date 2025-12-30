
package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientListener;
import com.github.auties00.cobalt.client.WhatsAppClientType;
import com.github.auties00.cobalt.client.WhatsAppWebClientHistory;
import com.github.auties00.cobalt.client.info.WhatsAppClientInfo;
import com.github.auties00.cobalt.media.MediaConnection;
import com.github.auties00.cobalt.model.auth.SignedDeviceIdentity;
import com.github.auties00.cobalt.model.auth.UserAgent.ReleaseChannel;
import com.github.auties00.cobalt.model.auth.Version;
import com.github.auties00.cobalt.model.business.BusinessCategory;
import com.github.auties00.cobalt.model.call.Call;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatBuilder;
import com.github.auties00.cobalt.model.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.model.chat.GroupOrCommunityMetadata;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.contact.ContactBuilder;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidCompanion;
import com.github.auties00.cobalt.model.jid.JidProvider;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.newsletter.Newsletter;
import com.github.auties00.cobalt.model.newsletter.NewsletterBuilder;
import com.github.auties00.cobalt.model.preferences.Label;
import com.github.auties00.cobalt.model.preferences.QuickReply;
import com.github.auties00.cobalt.model.preferences.Sticker;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntry;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.model.sync.*;
import com.github.auties00.cobalt.sync.crypto.MutationLTHash;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalProtocolStore;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.groups.state.SignalSenderKeyRecord;
import com.github.auties00.libsignal.key.*;
import com.github.auties00.libsignal.state.SignalSessionRecord;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ConcurrentMap;

/**
 * WhatsappStore manages all session-scoped data and state for WhatsApp client connections.
 * <p>
 * This class serves as the central repository for user information, communication data, and session
 * configuration during an active WhatsApp session. It maintains both persistent data (serialized between
 * sessions) and transient runtime state (recreated on each connection).
 * <p>
 * <b>Core Responsibilities:</b>
 * <ul>
 *     <li>Account management - profile, business info, locale, and device configuration</li>
 *     <li>Communication data - chats, contacts, newsletters, and status updates</li>
 *     <li>Security infrastructure - cryptographic keys, sessions, and device linking</li>
 *     <li>Session configuration - client type, version, proxy, and feature flags</li>
 *     <li>Synchronization state - tracking data sync completion across different categories</li>
 * </ul>
 * <p>
 * <b>Client Type Support:</b>
 * The store adapts its behavior based on {@link WhatsAppClientType}:
 * <ul>
 *     <li>{@link WhatsAppClientType#WEB} - Web/Desktop client using QR/pairing code authentication</li>
 *     <li>{@link WhatsAppClientType#MOBILE} - Mobile client using phone number authentication</li>
 * </ul>
 * <p>
 * <b>Serialization:</b>
 * The store uses Protocol Buffers for efficient serialization. Configure serialization through
 * {@link #setSerializer(WhatsappStoreSerializer)} to control persistence location and behavior.
 * Some fields (like runtime connections and listeners) are not serialized.
 *
 * @see WhatsAppClient
 * @see WhatsappStoreSerializer
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
public final class WhatsAppStore implements SignalProtocolStore {
    private static final WhatsappStoreSerializer DEFAULT_DESERIALIZER = WhatsappStoreSerializer.discarding();
    private static final String DEFAULT_NAME = "User";

    // =====================================================
    // SECTION: Core Identity & Configuration
    // =====================================================

    /**
     * Unique identifier for this store instance.
     * <p>
     * Used to distinguish between multiple concurrent sessions and during serialization/deserialization.
     * Generated once during store creation and remains constant for the lifetime of the session.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final UUID uuid;

    /**
     * Phone number associated with this WhatsApp account.
     * <p>
     * Stored in international format without '+' prefix (e.g., 1234567890 for +1-234-567-890).
     * May be null during initial Web client setup before QR code authentication completes.
     * For Mobile clients, typically set during registration.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long phoneNumber;

    /**
     * Client type determining protocol behavior and authentication method.
     * <p>
     * Affects:
     * <ul>
     *     <li>Authentication flow (QR/pairing code vs phone number)</li>
     *     <li>Available API features and operations</li>
     *     <li>Data serialization format</li>
     *     <li>Key management and encryption behavior</li>
     * </ul>
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final WhatsAppClientType clientType;

    /**
     * Unix timestamp (seconds) when this store instance was created.
     * <p>
     * Used for session age tracking and time-sensitive operations. Automatically
     * set to current time during store creation via {@link Clock#nowSeconds()}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final long initializationTimeStamp;

    // =====================================================
    // SECTION: Network & Connection Configuration
    // =====================================================

    /**
     * HTTP proxy URI for routing network traffic.
     * <p>
     * When configured, all network connections are routed through this proxy server.
     * Format: {@code http://host:port} or {@code https://host:port}
     * May include authentication: {@code http://username:password@host:port}
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    URI proxy;

    /**
     * Device information identifying this client to WhatsApp servers.
     * <p>
     * Contains:
     * <ul>
     *     <li>Platform type (iOS, Android, Web, Desktop)</li>
     *     <li>OS version and device model</li>
     *     <li>App version metadata</li>
     *     <li>Device-specific identifiers</li>
     * </ul>
     * Different device types have varying capabilities and protocol requirements.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    JidCompanion device;

    /**
     * Release channel for this connection.
     * <p>
     * Determines available protocol version and features:
     * <ul>
     *     <li>RELEASE - Stable public release</li>
     *     <li>BETA - Early access to new features</li>
     * </ul>
     * Beta channels may provide newer functionality but with reduced stability.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    ReleaseChannel releaseChannel;

    // =====================================================
    // SECTION: User Account & Profile
    // =====================================================

    /**
     * Indicates whether this account appears online to other users.
     * <p>
     * Controls visibility of online status and "last seen" indicator according to
     * privacy settings. When true, presence updates are sent to WhatsApp servers.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    boolean online;

    /**
     * Locale/language code for this account.
     * <p>
     * Format: ISO 639-1 language + ISO 3166-1 alpha-2 country (e.g., "en_US", "pt_BR").
     * Determines language for system messages and localized content.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String locale;

    /**
     * Display name shown to other WhatsApp users.
     * <p>
     * Null until successful login. For unregistered accounts, defaults to device
     * platform name. Updated via {@link WhatsAppClient#changeName(String)}.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String name;

    /**
     * Verified business name for verified business accounts.
     * <p>
     * Only populated for business accounts that completed WhatsApp's verification process.
     * Displays prominently in business profiles. Null for regular accounts.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String verifiedName;

    /**
     * URL of this account's profile picture.
     * <p>
     * Points to WhatsApp-hosted image resource. Null when no profile picture is set
     * or before initial login. Updated via {@link WhatsAppClient#changeProfilePicture(java.io.InputStream)}.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    URI profilePicture;

    /**
     * Personal status message (about text) displayed on profile.
     * <p>
     * Examples: "Hey there! I am using WhatsApp", custom status messages.
     * Null before login. Updated via {@link WhatsAppClient#changeAbout(String)}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String about;

    /**
     * WhatsApp JID uniquely identifying this user.
     * <p>
     * Format: phone_number@s.whatsapp.net (e.g., "1234567890@s.whatsapp.net")
     * Null until authentication completes. Used as primary identifier in all
     * WhatsApp protocol operations.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    Jid jid;

    /**
     * LID used when real phone number is not advertised.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    Jid lid;

    // =====================================================
    // SECTION: Business Account Information
    // =====================================================

    /**
     * Physical address of the business (Business accounts only).
     * <p>
     * Full street address where business is located. Displayed in business profile
     * and used for location-based discovery. Null for non-business accounts.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    String businessAddress;

    /**
     * Geographic longitude coordinate of business location (Business accounts only).
     * <p>
     * Range: -180.0 to +180.0 degrees. Used with {@link #businessLatitude} for
     * precise location mapping and discovery. Null for non-business accounts.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.DOUBLE)
    Double businessLongitude;

    /**
     * Geographic latitude coordinate of business location (Business accounts only).
     * <p>
     * Range: -90.0 to +90.0 degrees. Used with {@link #businessLongitude} for
     * precise location mapping and discovery. Null for non-business accounts.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.DOUBLE)
    Double businessLatitude;

    /**
     * Description of business services and offerings (Business accounts only).
     * <p>
     * Free-form text displayed in business profile. Helps customers understand
     * the nature and scope of business services. Null for non-business accounts.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    String businessDescription;

    /**
     * Website URL for the business (Business accounts only).
     * <p>
     * Valid HTTP/HTTPS URL linking to business's online presence. Displayed in
     * business profile for customer reference. Null for non-business accounts.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    String businessWebsite;

    /**
     * Contact email address for business inquiries (Business accounts only).
     * <p>
     * Used for customer communication outside WhatsApp. Displayed in business
     * profile. Null for non-business accounts.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.STRING)
    String businessEmail;

    /**
     * Business category classification (Business accounts only).
     * <p>
     * Defines business type (e.g., Restaurant, Retail, Services). Used for
     * business discovery, filtering, and categorization in WhatsApp Business
     * features. Null for non-business accounts.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    BusinessCategory businessCategory;

    // =====================================================
    // SECTION: Communication Data Collections
    // =====================================================

    /**
     * All chats (individual and group conversations) indexed by JID.
     *
     * @see Chat
     * @see #findChatByJid(JidProvider)
     */
    final ConcurrentHashMap<Jid, Chat> chats;

    /**
     * All newsletters (broadcast channels) indexed by JID.
     *
     * @see Newsletter
     */
    final ConcurrentHashMap<Jid, Newsletter> newsletters;

    /**
     * All status updates (stories) visible to this account.
     * <p>
     * Thread-safe set of ephemeral status messages posted by contacts. Statuses
     * are visible for 24 hours and stored separately from regular chat messages.
     *
     * @see ChatMessageInfo
     */
    final ConcurrentHashMap<String, ChatMessageInfo> status;


    /**
     * All contacts (address book and interaction history) indexed by JID.
     * <p>
     * Thread-safe map including saved contacts and WhatsApp users with whom
     * the account has interacted. Contains names, profile pictures, and status messages.
     *
     * @see Contact
     * @see #findContactByJid(JidProvider)
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<Jid, Contact> contacts;

    /**
     * All active and recent calls indexed by call ID.
     * <p>
     * Thread-safe map storing voice and video call information including state,
     * participants, and timestamps. Maintains call history for reference and notifications.
     *
     * @see Call
     */
    @ProtobufProperty(index = 25, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, Call> calls;

    // =====================================================
    // SECTION: Privacy & Security Settings
    // =====================================================

    /**
     * Privacy settings controlling visibility of profile information and activity.
     * <p>
     * Thread-safe map indexed by setting type name. Each entry defines visibility
     * rules (everyone, contacts only, nobody, etc.) for specific information types
     * like last seen, profile picture, about, and online status.
     *
     * @see PrivacySettingEntry
     * @see PrivacySettingType
     * @see PrivacySettingValue
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<PrivacySettingType, PrivacySettingEntry> privacySettings;

    // =====================================================
    // SECTION: Session Configuration & Behavior
    // =====================================================

    /**
     * Server-provided configuration properties controlling session behavior.
     * <p>
     * Thread-safe map of key-value pairs including feature flags, protocol settings,
     * and behavioral configurations. Updated by WhatsApp servers and may change
     * between sessions. Affects available features and protocol behavior.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    final ConcurrentHashMap<String, String> properties;

    /**
     * Whether archived chats automatically unarchive when receiving new messages.
     * <p>
     * When true: new message in archived chat moves it back to main chat list.
     * When false: archived chats remain archived regardless of new activity.
     * Default: true
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    boolean unarchiveChats;

    /**
     * Whether to use 24-hour time format for timestamp display.
     * <p>
     * When true: times displayed as 13:00, 23:45, etc.
     * When false: times displayed as 1:00 PM, 11:45 PM, etc.
     * Default: false
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean twentyFourHourFormat;

    /**
     * Default ephemeral timer for new chats.
     * <p>
     * When set to non-OFF value, new chats automatically enable disappearing messages
     * with specified duration. Messages in such chats are automatically deleted after
     * the configured time period.
     *
     * @see ChatEphemeralTimer
     */
    @ProtobufProperty(index = 30, type = ProtobufType.ENUM)
    ChatEphemeralTimer newChatsEphemeralTimer;

    /**
     * Amount of chat history to synchronize from WhatsApp servers (Web clients only).
     * <p>
     * Determines message history downloaded during initial connection and history sync.
     * Options range from recent messages only to full history, balancing storage and
     * bandwidth requirements.
     *
     * @see WhatsAppWebClientHistory
     */
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    WhatsAppWebClientHistory webHistoryPolicy;

    /**
     * Whether presence updates are automatically sent to WhatsApp servers.
     * <p>
     * When true: client automatically updates online/offline status and sends read receipts.
     * When false: presence updates must be sent manually. Disabling prevents automatic
     * "last seen" timestamp updates.
     * Default: true
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BOOL)
    boolean automaticPresenceUpdates;

    /**
     * Whether message read receipts are automatically sent.
     * <p>
     * When true: read receipts (blue ticks) automatically sent when messages are processed.
     * When false: read receipts must be sent manually via {@link WhatsAppClient#markMessageRead(MessageInfo)}.
     * Typically true for Mobile clients, configurable for Web clients.
     * Default: true
     */
    @ProtobufProperty(index = 33, type = ProtobufType.BOOL)
    boolean automaticMessageReceipts;

    /**
     * Whether to verify MAC (Message Authentication Code) in app state patches (Web clients only).
     * <p>
     * When true: cryptographically verifies integrity of app state synchronization patches.
     * Ensures data hasn't been tampered with but may cause sync issues if verification fails.
     * When false: skips MAC verification for faster synchronization.
     * Default: true
     */
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    boolean checkPatchMacs;

    // =====================================================
    // SECTION: Synchronization State Tracking
    // =====================================================

    /**
     * Indicates whether chat data has been synchronized from server.
     * <p>
     * Web clients: true after history sync completes.
     * Mobile clients: true after initial bootstrap.
     * Used to prevent duplicate synchronization and track initialization progress.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    boolean syncedChats;

    /**
     * Indicates whether contact data has been synchronized from server.
     * <p>
     * Web clients: true after history sync completes.
     * Mobile clients: true after initial bootstrap.
     * Used to prevent duplicate synchronization and track initialization progress.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    boolean syncedContacts;

    /**
     * Indicates whether newsletter data has been synchronized from server.
     * <p>
     * Both platforms: uses w:mex query to fetch newsletter information.
     * True after initial newsletter sync completes. Prevents redundant queries.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.BOOL)
    boolean syncedNewsletters;

    /**
     * Indicates whether status updates have been synchronized from server.
     * <p>
     * Web clients: true after history sync completes.
     * Mobile clients: true after initial bootstrap.
     * Used to prevent duplicate synchronization and track initialization progress.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.BOOL)
    boolean syncedStatus;

    /**
     * Indicates whether web app state has been synchronized (Web clients only).
     * <p>
     * Web app state includes settings, starred messages, and other non-message data.
     * True after initial app state sync completes. Prevents redundant synchronization.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    boolean syncedWebAppState;

    /**
     * Flag indicating whether the business certificate has been sent (Mobile API only).
     * <p>
     * For WhatsApp Business accounts, a business certificate must be sent during the
     * initial registration process. This flag tracks whether that certificate has
     * already been transmitted to avoid duplicate submissions.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.BOOL)
    boolean syncedBusinessCertificate;

    // =====================================================
    // SECTION: Signal Protocol - Identity & Registration
    // =====================================================

    /**
     * Signal protocol registration ID distinguishing this client installation.
     * <p>
     * Randomly generated integer (1-16380) used during Signal protocol handshake
     * and pre-key exchange. Generated once during initial setup if not provided.
     * Remains constant for lifetime of the installation.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.INT32)
    final Integer registrationId;

    /**
     * Noise protocol key pair for secure channel establishment.
     * <p>
     * Used during initial Noise XX handshake with WhatsApp servers to establish
     * encrypted communication channel. Generated once during account creation and
     * remains constant for account lifetime. Provides forward secrecy and ensures
     * initial connection security before sensitive data exchange.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    final SignalIdentityKeyPair noiseKeyPair;

    /**
     * Signal protocol identity key pair for end-to-end encryption.
     * <p>
     * Primary key pair used for Signal protocol implementation providing end-to-end
     * encryption for all messages. Generated once during account creation and remains
     * constant. Used in X3DH key agreement and message encryption/decryption.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    final SignalIdentityKeyPair identityKeyPair;

    /**
     * Companion device key pair identifying linked Web/Desktop clients (Web clients only).
     * <p>
     * For Web clients, identifies the linked device and establishes trust with primary
     * mobile device. Exchanged during QR code/pairing code pairing process. Null for
     * Mobile clients or before pairing completes.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    SignalIdentityKeyPair companionKeyPair;

    /**
     * Companion device signed identity information (Web clients only).
     * <p>
     * Contains signed identity received from primary mobile device during pairing.
     * Null until QR code/pairing code pairing completes and identity information
     * is synchronized from mobile device. Used to verify device authenticity.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    SignedDeviceIdentity companionIdentity;

    // =====================================================
    // SECTION: Signal Protocol - Pre-Keys & Key Management
    // =====================================================

    /**
     * Currently active signed pre-key pair.
     * <p>
     * Used in Signal protocol's X3DH (Extended Triple Diffie-Hellman) key agreement.
     * Generated during initialization and rotated according to WhatsApp's security
     * policies. Provides forward secrecy and deniability properties.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.MESSAGE)
    final SignalSignedKeyPair signedKeyPair;

    /**
     * Collection of one-time pre-keys for new session establishment.
     * <p>
     * Used in Signal protocol's asynchronous key agreement. Each key is used exactly
     * once when a new session is initiated. When supply runs low, new batches are
     * generated and uploaded to ensure users can always initiate encrypted sessions.
     * <p>
     * LinkedHashMap preserves insertion order important for key rotation and management.
     *
     * @see SignalPreKeyPair
     */
    @ProtobufProperty(index = 48, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    final LinkedHashMap<Integer, SignalPreKeyPair> preKeys;

    // =====================================================
    // SECTION: Device Identifiers (Mobile)
    // =====================================================

    /**
     * FDID used during mobile registration.
     * <p>
     * Unique identifier for WhatsApp's mobile clients during registration process.
     * Generated for Web clients but may not be actively used. Format: UUID string.
     */
    @ProtobufProperty(index = 49, type = ProtobufType.STRING)
    final UUID fdid;

    /**
     * Unique device identifier for mobile clients.
     * <p>
     * Device-specific identifier used by WhatsApp to track individual installations.
     * Generated as UUID without hyphens, then hex-decoded to bytes. Remains constant
     * for lifetime of the installation.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.BYTES)
    final byte[] deviceId;

    /**
     * Advertising identifier for mobile analytics and tracking (Mobile clients only).
     * <p>
     * UUID used for analytics and advertising purposes on mobile platforms. May be
     * reset by user through device settings. Null for Web clients.
     */
    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    final UUID advertisingId;

    /**
     * Unique identity identifier for this WhatsApp account installation.
     * <p>
     * Cryptographically random byte array uniquely identifying this specific account
     * installation. Different from JID (WhatsApp user ID). Provides additional layer
     * of identity verification. Generated once during account creation and remains constant.
     * Used in various cryptographic protocols and authentication flows.
     */
    @ProtobufProperty(index = 52, type = ProtobufType.BYTES)
    final byte[] identityId;

    /**
     * Backup/recovery token for mobile clients (Mobile clients only).
     * <p>
     * 20-byte random value authenticating backup and restore operations. Allows users
     * to recover chat history when reinstalling app or switching devices. Cryptographically
     * tied to account - encrypted backups cannot be accessed without it. Must be kept secure.
     */
    @ProtobufProperty(index = 53, type = ProtobufType.BYTES)
    final byte[] backupToken;

    // =====================================================
    // SECTION: Signal Protocol - Sessions & Group Keys
    // =====================================================

    /**
     * Signal protocol sender keys for group messaging.
     * <p>
     * Thread-safe map storing sender key records for efficient group message encryption.
     * Each group has a sender key allowing all members to encrypt messages that any
     * member can decrypt. Provides efficient multi-party encryption.
     *
     * @see SignalSenderKeyRecord
     * @see SignalSenderKeyName
     */
    @ProtobufProperty(index = 54, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeys;

    /**
     * App state synchronization keys (Web clients only).
     * <p>
     * Ordered map of keys used for syncing application state between devices. Each key
     * is versioned and used to encrypt/decrypt different types of app state patches.
     * Enables consistent state across all linked devices.
     *
     * @see AppStateSyncKey
     */
    @ProtobufProperty(index = 55, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final LinkedHashMap<String, AppStateSyncKey> appStateKeys;

    /**
     * Active Signal protocol sessions for end-to-end encryption.
     * <p>
     * Thread-safe map storing session records for each conversation. Sessions are
     * established during initial key exchange and persist across messages. Each
     * session maintains its own encryption state and ratcheting keys.
     *
     * @see SignalSessionRecord
     * @see SignalProtocolAddress
     */
    @ProtobufProperty(index = 56, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessions;

    /**
     * App state sync hash states tracking synchronization status (Web clients only).
     * <p>
     * Thread-safe map tracking sync status for different app state types. Each
     * {@link PatchType} has associated {@link AppStateSyncHash} that:
     * <ul>
     *     <li>Identifies current version of that state type</li>
     *     <li>Enables detection of changes from mobile device</li>
     *     <li>Facilitates efficient differential synchronization</li>
     * </ul>
     * Examples: contacts, chat settings, starred messages, etc.
     *
     * @see PatchType
     * @see AppStateSyncHash
     */
    @ProtobufProperty(index = 57, type = ProtobufType.MESSAGE, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<PatchType, AppStateSyncHash> hashStates;

    // =====================================================
    // SECTION: Mobile status
    // =====================================================

    /**
     * Flag indicating whether this client has completed registration with WhatsApp.
     * <p>
     * For Web clients, registration is complete after successful QR code/Pairing code pairing.
     * For Mobile clients, registration is complete after phone number verification.
     * <p>
     * This flag affects which API calls are available and how the client behaves during
     * connection establishment.
     */
    @ProtobufProperty(index = 58, type = ProtobufType.BOOL)
    boolean registered;

    /**
     * Whether to show security notifications when chatting with a contact.
     */
    @ProtobufProperty(index = 59, type = ProtobufType.BOOL)
    boolean showSecurityNotifications;

    /**
     * Recent stickers
     */
    @ProtobufProperty(index = 60, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<String, Sticker> recentStickers;

    /**
     * Favorite stickers
     */
    @ProtobufProperty(index = 61, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<String, Sticker> favouriteStickers;

    /**
     * Quick replies
     */
    @ProtobufProperty(index = 62, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<String, QuickReply> quickReplies;

    /**
     * Labels
     */
    @ProtobufProperty(index = 63, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentMap<Integer, Label> labels;

    @ProtobufProperty(index = 64, type = ProtobufType.MESSAGE)
    volatile Version clientVersion;

    @ProtobufProperty(index = 65, type = ProtobufType.MESSAGE)
    Version companionVersion;

    // =====================================================
    // SECTION: Runtime State (Non-Serialized)
    // =====================================================

    /**
     * Serializer responsible for persisting this store to storage.
     * <p>
     * Determines how and where session data is saved. Can be changed at runtime via
     * {@link #setSerializer(WhatsappStoreSerializer)} to migrate between storage backends.
     *
     * @see WhatsappStoreSerializer
     * @see #serialize()
     */
    private WhatsappStoreSerializer serializer;

    /**
     * Controls whether this store is persisted to storage.
     * <p>
     * When false, {@link #serialize()} performs no operations. Useful for temporary
     * sessions or testing scenarios where persistence is not desired.
     * Default: true
     */
    private boolean serializable;

    /**
     * Registered event listeners for this session.
     * <p>
     * Thread-safe set receiving callbacks for WhatsApp events (new messages, status
     * changes, connection events). Not serialized - must be repopulated after session
     * restoration. Add/remove via {@link WhatsAppClient#addListener(WhatsAppClientListener)} and
     * {@link WhatsAppClient#removeListener(WhatsAppClientListener)}.
     *
     * @see WhatsAppClientListener
     */
    private final KeySetView<WhatsAppClientListener, Boolean> listeners;

    /**
     * LID to phone number JID mappings for the LID migration system.
     * <p>
     * Thread-safe map that enables reverse lookups from LID to phone number.
     * Not serialized - rebuilt from contacts on session restoration.
     * Used for contact resolution when receiving messages with LID addressing.
     */
    private final ConcurrentHashMap<Jid, Jid> lidToPhoneMappings;

    /**
     * Phone number JID to LID mappings for the LID migration system.
     * <p>
     * Thread-safe map that enables lookups from phone number to LID.
     * Not serialized - rebuilt from contacts on session restoration.
     */
    private final ConcurrentHashMap<Jid, Jid> phoneToLidMappings;

    /**
     * Active media connection for uploading/downloading media files.
     * <p>
     * Separate connection with authentication tokens for media operations (images,
     * videos, documents). Populated during session initialization and may be refreshed
     * periodically. Not serialized - recreated on each connection.
     * <p>
     *
     * @see MediaConnection
     */
    private volatile MediaConnection mediaConnection;

    /**
     * Lock object for the media connection
     */
    private final Object mediaConnectionLock = new Object();

    /**
     * Pending mutations awaiting synchronization to the server.
     */
    private final ConcurrentMap<PatchType, SequencedCollection<PendingMutation>> webAppStatePendingMutations;

    /**
     * The web app state
     */
    private final ConcurrentMap<PatchType, CollectionMetadata> webAppStateCollections;

    /**
     * Lock to update the client version
     */
    private final Object clientVersionLock;

    /**
     * Cache for group/community metadata
     */
    private final ConcurrentMap<Jid, GroupOrCommunityMetadata> groupOrCommunityMetadata;

    /**
     * Cache for WhatsApp devices
     */
    private final ConcurrentMap<Jid, SequencedCollection<Jid>> deviceLists;

    // =====================================================
    // SECTION: Constructor & Factory Methods
    // =====================================================


    /**
     * Private constructor used by builder and deserialization
     */
    WhatsAppStore(
            UUID uuid,
            Long phoneNumber,
            WhatsAppClientType clientType,
            long initializationTimeStamp,
            URI proxy,
            JidCompanion device,
            ReleaseChannel releaseChannel,
            boolean online,
            String locale,
            String name,
            String verifiedName,
            URI profilePicture,
            String about,
            Jid jid,
            Jid lid,
            String businessAddress,
            Double businessLongitude,
            Double businessLatitude,
            String businessDescription,
            String businessWebsite,
            String businessEmail,
            BusinessCategory businessCategory,
            ConcurrentHashMap<Jid, Contact> contacts,
            ConcurrentHashMap<String, Call> calls,
            ConcurrentHashMap<PrivacySettingType, PrivacySettingEntry> privacySettings,
            ConcurrentHashMap<String, String> properties,
            boolean unarchiveChats,
            boolean twentyFourHourFormat,
            ChatEphemeralTimer newChatsEphemeralTimer,
            WhatsAppWebClientHistory webHistoryPolicy,
            boolean automaticPresenceUpdates,
            boolean automaticMessageReceipts,
            boolean checkPatchMacs,
            boolean syncedChats,
            boolean syncedContacts,
            boolean syncedNewsletters,
            boolean syncedStatus,
            boolean syncedWebAppState,
            boolean syncedBusinessCertificate,
            Integer registrationId,
            SignalIdentityKeyPair noiseKeyPair,
            SignalIdentityKeyPair identityKeyPair,
            SignalIdentityKeyPair companionKeyPair,
            SignedDeviceIdentity companionIdentity,
            SignalSignedKeyPair signedKeyPair,
            LinkedHashMap<Integer, SignalPreKeyPair> preKeys,
            UUID fdid,
            byte[] deviceId,
            UUID advertisingId,
            byte[] identityId,
            byte[] backupToken,
            ConcurrentMap<SignalSenderKeyName, SignalSenderKeyRecord> senderKeys,
            LinkedHashMap<String, AppStateSyncKey> appStateKeys,
            ConcurrentMap<SignalProtocolAddress, SignalSessionRecord> sessions,
            ConcurrentMap<PatchType, AppStateSyncHash> hashStates,
            boolean registered,
            boolean showSecurityNotifications,
            ConcurrentMap<String, Sticker> recentStickers,
            ConcurrentMap<String, Sticker> favouriteStickers,
            ConcurrentMap<String, QuickReply> quickReplies,
            ConcurrentMap<Integer, Label> labels,
            Version clientVersion,
            Version companionVersion
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.phoneNumber = phoneNumber; 
        this.clientType = Objects.requireNonNull(clientType, "clientType cannot be null");
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
        this.profilePicture = profilePicture; 
        this.about = about; 
        this.jid = jid;  
        this.lid = lid;  
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");  
        this.contacts = Objects.requireNonNull(contacts, "contacts cannot be null");
        this.privacySettings = Objects.requireNonNull(privacySettings, "privacySettings cannot be null");
        this.calls = Objects.requireNonNull(calls, "calls cannot be null");
        this.unarchiveChats = unarchiveChats;
        this.twentyFourHourFormat = twentyFourHourFormat;
        this.initializationTimeStamp = initializationTimeStamp;
        this.newChatsEphemeralTimer = Objects.requireNonNullElse(newChatsEphemeralTimer, ChatEphemeralTimer.OFF);
        this.webHistoryPolicy = webHistoryPolicy;
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        this.automaticMessageReceipts = automaticMessageReceipts;
        this.releaseChannel = Objects.requireNonNullElse(releaseChannel, ReleaseChannel.RELEASE);
        this.device = Objects.requireNonNull(device, "device cannot be null");
        this.checkPatchMacs = checkPatchMacs;
        this.syncedChats = syncedChats;
        this.syncedContacts = syncedContacts;
        this.syncedNewsletters = syncedNewsletters;
        this.syncedStatus = syncedStatus;
        this.syncedWebAppState = syncedWebAppState;
        this.syncedBusinessCertificate = syncedBusinessCertificate;
        this.favouriteStickers = favouriteStickers;
        this.quickReplies = quickReplies;
        this.labels = labels;
        this.chats = new ConcurrentHashMap<>();
        this.newsletters = new ConcurrentHashMap<>();
        this.status = new ConcurrentHashMap<>();
        this.listeners = ConcurrentHashMap.newKeySet();
        this.lidToPhoneMappings = new ConcurrentHashMap<>();
        this.phoneToLidMappings = new ConcurrentHashMap<>();
        for (var contact : contacts.values()) {
            contact.lid()
                    .ifPresent(entry -> registerLidMapping(contact.jid(), entry));
        }
        this.registrationId = Objects.requireNonNullElseGet(registrationId, () -> SecureBytes.nextInt(16380) + 1);
        this.noiseKeyPair = Objects.requireNonNullElseGet(noiseKeyPair, SignalIdentityKeyPair::random);
        this.identityKeyPair = Objects.requireNonNullElseGet(identityKeyPair, SignalIdentityKeyPair::random);
        this.companionKeyPair = companionKeyPair;
        this.signedKeyPair = Objects.requireNonNullElseGet(signedKeyPair, () -> SignalSignedKeyPair.of(this.registrationId, this.identityKeyPair));
        this.preKeys = Objects.requireNonNull(preKeys, "preKeys cannot be null");
        this.fdid = Objects.requireNonNullElseGet(fdid, UUID::randomUUID);
        this.deviceId = Objects.requireNonNullElseGet(deviceId, () -> HexFormat.of().parseHex(UUID.randomUUID().toString().replace("-", "")));
        this.advertisingId = Objects.requireNonNullElseGet(advertisingId, UUID::randomUUID);
        this.identityId = Objects.requireNonNullElseGet(identityId, () -> SecureBytes.random(16));
        this.backupToken = Objects.requireNonNullElseGet(backupToken, () -> SecureBytes.random(20));
        this.companionIdentity = companionIdentity;
        this.senderKeys = Objects.requireNonNull(senderKeys, "senderKeys cannot be null");
        this.appStateKeys = Objects.requireNonNull(appStateKeys, "appStateKeys cannot be null");
        this.sessions = Objects.requireNonNull(sessions, "sessions cannot be null");
        this.hashStates = Objects.requireNonNull(hashStates, "hashStates cannot be null");
        this.registered = registered;
        this.showSecurityNotifications = showSecurityNotifications;
        this.recentStickers = recentStickers;
        this.clientVersion = clientVersion;
        this.clientVersionLock = new Object();
        this.companionVersion = companionVersion;
        this.webAppStatePendingMutations = new ConcurrentHashMap<>();
        this.webAppStateCollections = new ConcurrentHashMap<>();
        this.serializable = true;
        this.groupOrCommunityMetadata = new ConcurrentHashMap<>();
        this.deviceLists = new ConcurrentHashMap<>();
    }

    // =====================================================
    // SECTION: Serialization & Persistence
    // =====================================================

    /**
     * Returns whether this store is configured for serialization.
     *
     * @return true if this store will be persisted via {@link #serialize()}
     */
    public boolean serializable() {
        return serializable;
    }

    /**
     * Configures whether this store should be persisted to storage.
     * <p>
     * When set to false, {@link #serialize()} performs no operations. Useful for
     * temporary sessions or testing where persistence is not desired.
     *
     * @param serializable whether to enable serialization
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSerializable(boolean serializable) {
        this.serializable = serializable;
        return this;
    }

    /**
     * Returns the serializer responsible for persisting this store.
     *
     * @return the current serializer, may be null if not configured
     */
    public WhatsappStoreSerializer serializer() {
        return Objects.requireNonNullElse(serializer, DEFAULT_DESERIALIZER);
    }

    /**
     * Sets the serializer responsible for persisting this store.
     * <p>
     * Can be changed at runtime to migrate between storage backend.
     *
     * @param serializer the new serializer, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSerializer(WhatsappStoreSerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * Persists this store to storage using the configured serializer.
     * <p>
     * Only performs serialization if {@link #serializable} is true and a serializer
     * is configured. Called automatically during normal operation and before disconnection.
     *
     * @return this store instance for method chaining
     */
    public WhatsAppStore serialize() {
        if (serializable && serializer != null) {
            serializer.serialize(this);
        }
        return this;
    }

    // =====================================================
    // SECTION: Contact Management
    // =====================================================

    /**
     * Finds a contact by either phone number JID or LID.
     * <p>
     * First attempts direct lookup by the provided JID, then tries
     * the alternate addressing mode if the first lookup fails.
     *
     * @param jid the JID to search for (phone or LID)
     * @return Optional containing the contact if found
     */
    public Optional<Contact> findContactByJid(JidProvider jid) {
        return switch (jid) {
            case Contact contact -> Optional.of(contact);
            case null -> Optional.empty();
            case Chat _, Newsletter _, Jid _, JidServer _-> {
                var targetJid = jid.toJid();
                if(targetJid.hasUserServer()) {
                    var jidContact = contacts.get(targetJid);
                    if(jidContact != null) {
                        yield Optional.of(jidContact);
                    } else {
                        yield findLidByPhone(targetJid)
                                .map(contacts::get);
                    }
                } else if(targetJid.hasLidServer()) {
                    var lidContact = contacts.get(targetJid);
                    if(lidContact != null) {
                        yield Optional.of(lidContact);
                    } else {
                        yield findPhoneByLid(targetJid)
                                .map(contacts::get);
                    }
                } else {
                    var contact = contacts.get(targetJid);
                    yield Optional.ofNullable(contact);
                }
            }
        };
    }

    /**
     * Returns all contacts stored in this session.
     *
     * @return immutable collection of all contacts
     */
    public Collection<Contact> contacts() {
        return Collections.unmodifiableCollection(contacts.values());
    }

    /**
     * Adds a new contact to the store using just a JID.
     * <p>
     * Creates a minimal contact with only the JID populated. Additional information
     * can be added later through contact updates from WhatsApp servers.
     *
     * @param jid the JID of the contact to add, must not be null
     * @return the newly created contact
     * @throws NullPointerException if jid is null
     */
    public Contact addNewContact(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        var newContact = new ContactBuilder()
                .jid(jid)
                .lid(phoneToLidMappings.get(jid))
                .build();
        return addContact(newContact);
    }

    /**
     * Adds or updates a contact in the store.
     * <p>
     * If a contact with the same JID already exists, it will be replaced with the new one.
     *
     * @param contact the contact to add or update, must not be null
     * @return the contact that was added (same as parameter)
     * @throws NullPointerException if contact is null
     */
    public Contact addContact(Contact contact) {
        Objects.requireNonNull(contact, "contact cannot be null");
        contacts.put(contact.jid(), contact);
        return contact;
    }

    /**
     * Removes a contact from the store.
     *
     * @param contactJid the JID of the contact to remove, may be null
     * @return Optional containing the removed contact if it existed, empty otherwise
     */
    public Optional<Contact> removeContact(JidProvider contactJid) {
        if(contactJid == null) {
            return Optional.empty();
        } else {
            var targetJid = jid.toJid();
            if(targetJid.hasUserServer()) {
                var jidContact = contacts.remove(targetJid);
                if(jidContact != null) {
                    return Optional.of(jidContact);
                } else {
                    return findLidByPhone(targetJid)
                            .map(contacts::remove);
                }
            } else if(targetJid.hasLidServer()) {
                var lidContact = contacts.remove(targetJid);
                if(lidContact != null) {
                    return Optional.of(lidContact);
                } else {
                    return findPhoneByLid(targetJid)
                            .map(contacts::remove);
                }
            } else {
                var chat = contacts.remove(targetJid);
                return Optional.ofNullable(chat);
            }
        }
    }

    // =====================================================
    // SECTION: LID Migration Support
    // =====================================================

    /**
     * Registers a LID mapping for a contact.
     * <p>
     * This creates bidirectional mappings between phone number JID and LID
     * to enable lookups in both directions during LID migration.
     *
     * @param phoneJid the phone number JID
     * @param lidJid the LID JID
     */
    public void registerLidMapping(Jid phoneJid, Jid lidJid) {
        if (phoneJid == null || lidJid == null) {
            return;
        }
        var normalizedPhone = phoneJid.withoutData();
        var normalizedLid = lidJid.withoutData();
        lidToPhoneMappings.put(normalizedLid, normalizedPhone);
        phoneToLidMappings.put(normalizedPhone, normalizedLid);
    }

    /**
     * Finds the phone number JID for a given LID.
     *
     * @param lidJid the LID to look up
     * @return Optional containing the phone number JID if found
     */
    public Optional<Jid> findPhoneByLid(Jid lidJid) {
        return lidJid == null ? Optional.empty() : Optional.ofNullable(lidToPhoneMappings.get(lidJid.withoutData()));
    }

    /**
     * Finds the LID for a given phone number JID.
     *
     * @param phoneJid the phone number JID to look up
     * @return Optional containing the LID if found
     */
    public Optional<Jid> findLidByPhone(Jid phoneJid) {
        if (phoneJid == null) {
            return Optional.empty();
        } else {
            var localJid = jid;
            if(localJid != null && Objects.equals(phoneJid.user(), localJid.user())) {
                return Optional.ofNullable(lid)
                        .map(lid -> lid.withDevice(phoneJid.device()));
            } else {
                var result = phoneToLidMappings.get(phoneJid.withoutData());
                return Optional.ofNullable(result);
            }
        }
    }

    // =====================================================
    // SECTION: Chat & Message Management
    // =====================================================

    /**
     * Finds a chat by its JID.
     *
     * @param jid the JID to search for, may be null
     * @return Optional containing the chat if found, empty otherwise
     */
    public Optional<Chat> findChatByJid(JidProvider jid) {
        return switch (jid) {
            case null -> Optional.empty();
            case Chat chat -> Optional.of(chat);
            case Contact _, Newsletter _, Jid _, JidServer _-> {
                var targetJid = jid.toJid();
                if(targetJid.hasUserServer()) {
                    var jidChat = chats.get(targetJid);
                    if(jidChat != null) {
                        yield Optional.of(jidChat);
                    } else {
                        yield findLidByPhone(targetJid)
                                .map(chats::get);
                    }
                } else if(targetJid.hasLidServer()) {
                    var lidChat = chats.get(targetJid);
                    if(lidChat != null) {
                        yield Optional.of(lidChat);
                    } else {
                        yield findPhoneByLid(targetJid)
                                .map(chats::get);
                    }
                } else {
                    var chat = chats.get(targetJid);
                    yield Optional.ofNullable(chat);
                }
            }
        };
    }

    /**
     * Queries the first message whose id matches the one provided in the specified chat or newsletter
     *
     * @param provider the chat to search in
     * @param id       the jid to search
     * @return a non-null optional
     */
    public Optional<? extends MessageInfo> findMessageById(JidProvider provider, String id) {
        return provider == null || id == null ? Optional.empty() : switch (provider) {
            case Chat chat -> findMessageById(chat, id);
            case Newsletter newsletter -> findMessageById(newsletter, id);
            case Contact contact -> findChatByJid(contact.jid())
                    .flatMap(chat -> findMessageById(chat, id));
            case Jid contactJid -> {
                if (contactJid.server().type() == JidServer.Type.NEWSLETTER) {
                    yield findNewsletterByJid(contactJid)
                            .flatMap(newsletter -> findMessageById(newsletter, id));
                } else if (Jid.statusBroadcastAccount().equals(contactJid)) {
                    yield Optional.ofNullable(status.get(id));
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
     * Queries the first message whose id matches the one provided in the specified newsletter
     *
     * @param newsletter newsletter chat to search in
     * @param id         the jid to search
     * @return a non-null optional
     */
    public Optional<NewsletterMessageInfo> findMessageById(Newsletter newsletter, String id) {
        return newsletter == null || id == null ? Optional.empty() : newsletter.messages()
                .parallelStream()
                .filter(entry -> Objects.equals(id, entry.id()) || Objects.equals(id, String.valueOf(entry.serverId())))
                .findFirst();
    }


    /**
     * Queries the first message whose id matches the one provided in the specified chat
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non-null optional
     */
    public Optional<ChatMessageInfo> findMessageById(Chat chat, String id) {
        return chat == null || id == null ? Optional.empty() : chat.messages()
                .parallelStream()
                .filter(message -> Objects.equals(message.key().id(), id))
                .findAny();
    }

    /**
     * Returns all chats stored in this session.
     *
     * @return immutable collection of all chats
     */
    public Collection<Chat> chats() {
        return Collections.unmodifiableCollection(chats.values());
    }

    /**
     * Adds or updates a chat in the store.
     * <p>
     * If a chat with the same JID already exists, it will be replaced.
     *
     * @param chat the chat to add or update, must not be null
     * @return the chat that was added (same as parameter)
     * @throws NullPointerException if chat is null
     */
    public Chat addChat(Chat chat) {
        Objects.requireNonNull(chat, "chat cannot be null");
        chats.put(chat.jid(), chat);
        return chat;
    }

    /**
     * Adds a chat in memory
     *
     * @param chatJid the chat to add
     * @return the input chat
     * @throws NullPointerException if chatJid is null
     */
    public Chat addNewChat(Jid chatJid) {
        Objects.requireNonNull(chatJid, "chatJid cannot be null");
        var chat = new ChatBuilder()
                .jid(chatJid)
                .build();
        addChat(chat);
        return chat;
    }

    /**
     * Removes a chat from the store.
     *
     * @param chatJid the JID of the chat to remove, may be null
     * @return Optional containing the removed chat if it existed, empty otherwise
     */
    public Optional<Chat> removeChat(JidProvider chatJid) {
        if(chatJid == null) {
            return Optional.empty();
        } else {
            var targetJid = jid.toJid();
            if(targetJid.hasUserServer()) {
                var jidChat = chats.remove(targetJid);
                if(jidChat != null) {
                    return Optional.of(jidChat);
                } else {
                    return findLidByPhone(targetJid)
                            .map(chats::remove);
                }
            } else if(targetJid.hasLidServer()) {
                var lidChat = chats.remove(targetJid);
                if(lidChat != null) {
                    return Optional.of(lidChat);
                } else {
                    return findPhoneByLid(targetJid)
                            .map(chats::remove);
                }
            } else {
                var chat = chats.remove(targetJid);
                return Optional.ofNullable(chat);
            }
        }
    }

    public ChatMessageInfo addStatus(ChatMessageInfo messageInfo) {
        Objects.requireNonNull(messageInfo, "messageInfo cannot be null");
        status.put(messageInfo.key().id(), messageInfo);
        return messageInfo;
    }

    public Optional<ChatMessageInfo> removeStatus(String id) {
        return Optional.ofNullable(status.remove(id));
    }

    // =====================================================
    // SECTION: Call Management
    // =====================================================

    public Optional<Call> findCallById(String callId) {
        return callId == null
                ? Optional.empty()
                : Optional.ofNullable(calls.get(callId));
    }

    /**
     * Adds a new call to the store.
     * <p>
     * If a call with the same id already exists, it will be replaced.
     *
     * @param call the call to add or update, must not be null
     * @return the call that was added (same as parameter)
     * @throws NullPointerException if call is null
     */
    public Call addCall(Call call) {
        Objects.requireNonNull(call, "call cannot be null");
        calls.put(call.id(), call);
        return call;
    }

    /**
     * Removes a call from the store.
     *
     * @param id the id of the call to remove, may be null
     * @return an {@code Optional} containing the removed call if it existed, otherwise an empty {@code Optional}
     */
    public Optional<Call> removeCall(String id) {
        return id == null
                ? Optional.empty()
                : Optional.ofNullable(calls.remove(id));
    }

    // =====================================================
    // SECTION: Newsletter Management
    // =====================================================

    /**
     * Finds a newsletter by its JID.
     *
     * @param jid the JID to search for, may be null
     * @return Optional containing the newsletter if found, empty otherwise
     */
    public Optional<Newsletter> findNewsletterByJid(JidProvider jid) {
        return jid == null
                ? Optional.empty()
                : Optional.ofNullable(newsletters.get(jid.toJid()));
    }

    /**
     * Returns all newsletters stored in this session.
     *
     * @return immutable collection of all newsletters
     */
    public Collection<Newsletter> newsletters() {
        return Collections.unmodifiableCollection(newsletters.values());
    }

    /**
     * Adds or updates a newsletter in the store.
     *
     * @param newsletter the newsletter to add or update, must not be null
     * @return the newsletter that was added (same as parameter)
     * @throws NullPointerException if newsletter is null
     */
    public Newsletter addNewsletter(Newsletter newsletter) {
        newsletters.put(newsletter.jid(), newsletter);
        return newsletter;
    }

    /**
     * Adds a newsletter in memory
     *
     * @param newsletterJid the newsletter to add
     * @return the input newsletter
     * @throws NullPointerException if newsletterJid is null
     */
    public Newsletter addNewNewsletter(Jid newsletterJid) {
        Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        var newsletter = new NewsletterBuilder()
                .jid(newsletterJid)
                .build();
        addNewsletter(newsletter);
        return newsletter;
    }

    /**
     * Removes a newsletter from the store.
     *
     * @param newsletterJid the JID of the newsletter to remove, may be null
     * @return Optional containing the removed newsletter if it existed, empty otherwise
     */
    public Optional<Newsletter> removeNewsletter(JidProvider newsletterJid) {
        return newsletterJid == null
                ? Optional.empty()
                : Optional.ofNullable(newsletters.remove(newsletterJid.toJid()));
    }

    // =====================================================
    // SECTION: Accessors & Mutators
    // =====================================================

    /**
     * Returns the unique identifier for this store instance.
     *
     * @return the UUID, never null
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Returns the phone number associated with this account.
     *
     * @return Optional containing the phone number in international format, empty if not set
     */
    public OptionalLong phoneNumber() {
        return phoneNumber == null ? OptionalLong.empty() : OptionalLong.of(phoneNumber);
    }

    /**
     * Sets the phone number for this account.
     *
     * @param phoneNumber the phone number in international format, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    /**
     * Returns the client type for this session.
     *
     * @return the client type, never null
     */
    public WhatsAppClientType clientType() {
        return clientType;
    }

    /**
     * Returns the proxy URI if configured.
     *
     * @return Optional containing the proxy URI, empty if not configured
     */
    public Optional<URI> proxy() {
        return Optional.ofNullable(proxy);
    }

    /**
     * Sets the proxy URI for network connections.
     *
     * @param proxy the proxy URI, may be null to disable proxy
     * @return this store instance for method chaining
     */
    public WhatsAppStore setProxy(URI proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Returns the device information.
     *
     * @return the device info,cannot be null
     */
    public JidCompanion device() {
        return device;
    }

    /**
     * Sets the device information.
     *
     * @param device the device info, cannot be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setDevice(JidCompanion device) {
        this.device = Objects.requireNonNull(device, "device cannot be null");
        return this;
    }

    /**
     * Returns the release channel.
     *
     * @return the release channel, cannot be null
     */
    public ReleaseChannel releaseChannel() {
        return releaseChannel;
    }

    /**
     * Sets the release channel.
     *
     * @param releaseChannel the release channel, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setReleaseChannel(ReleaseChannel releaseChannel) {
        this.releaseChannel = Objects.requireNonNull(releaseChannel, "releaseChannel cannot be null");
        return this;
    }

    /**
     * Returns whether this account appears online.
     *
     * @return true if online, false otherwise
     */
    public boolean online() {
        return online;
    }

    /**
     * Sets whether this account appears online.
     *
     * @param online true to appear online, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setOnline(boolean online) {
        this.online = online;
        return this;
    }

    /**
     * Returns the locale code.
     *
     * @return Optional containing the locale, empty if not set
     */
    public Optional<String> locale() {
        return Optional.ofNullable(locale);
    }

    /**
     * Sets the locale code.
     *
     * @param locale the locale in format "language_COUNTRY", may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    /**
     * Returns the display name.
     *
     * @return Optional containing the name, empty if not yet set or before login
     */
    public String name() {
        return Objects.requireNonNullElse(name, DEFAULT_NAME);
    }

    /**
     * Sets the display name.
     *
     * @param name the display name, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the verified business name.
     *
     * @return Optional containing the verified name, empty for non-business accounts
     */
    public Optional<String> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }

    /**
     * Sets the verified business name.
     *
     * @param verifiedName the verified name, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setVerifiedName(String verifiedName) {
        this.verifiedName = verifiedName;
        return this;
    }

    /**
     * Returns the profile picture URI.
     *
     * @return Optional containing the profile picture URI, empty if not set
     */
    public Optional<URI> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    /**
     * Sets the profile picture URI.
     *
     * @param profilePicture the profile picture URI, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setProfilePicture(URI profilePicture) {
        this.profilePicture = profilePicture;
        return this;
    }

    /**
     * Returns the about text.
     *
     * @return Optional containing the about text, empty if not set
     */
    public Optional<String> about() {
        return Optional.ofNullable(about);
    }

    /**
     * Sets the about text.
     *
     * @param about the about text, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setAbout(String about) {
        this.about = about;
        return this;
    }

    /**
     * Returns the WhatsApp JID.
     *
     * @return Optional containing the JID, empty before authentication completes
     */
    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }

    /**
     * Sets the WhatsApp JID.
     *
     * @param jid the JID, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setJid(Jid jid) {
        this.jid = jid;
        return this;
    }

    /**
     * Returns the LID.
     *
     * @return Optional containing the LID, empty if not set
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Sets the LID.
     *
     * @param lid the LID, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setLid(Jid lid) {
        this.lid = lid;
        return this;
    }

    /**
     * Returns the business address.
     *
     * @return Optional containing the business address, empty for non-business accounts
     */
    public Optional<String> businessAddress() {
        return Optional.ofNullable(businessAddress);
    }

    /**
     * Sets the business address.
     *
     * @param businessAddress the business address, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
        return this;
    }

    /**
     * Returns the business longitude.
     *
     * @return Optional containing the longitude, empty for non-business accounts
     */
    public OptionalDouble businessLongitude() {
        return businessLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(businessLongitude);
    }

    /**
     * Sets the business longitude.
     *
     * @param businessLongitude the longitude (-180.0 to 180.0), may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
        return this;
    }

    /**
     * Returns the business latitude.
     *
     * @return Optional containing the latitude, empty for non-business accounts
     */
    public OptionalDouble businessLatitude() {
        return businessLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(businessLatitude);
    }

    /**
     * Sets the business latitude.
     *
     * @param businessLatitude the latitude (-90.0 to 90.0), may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
        return this;
    }

    /**
     * Returns the business description
     *
     * @return Optional containing the description, empty for non-business accounts
     */
    public Optional<String> businessDescription() {
        return Optional.ofNullable(businessDescription);
    }

    /**
     * Sets the business description.
     *
     * @param businessDescription the description, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
        return this;
    }

    /**
     * Returns the business website.
     *
     * @return Optional containing the website URL, empty for non-business accounts
     */
    public Optional<String> businessWebsite() {
        return Optional.ofNullable(businessWebsite);
    }

    /**
     * Sets the business website.
     *
     * @param businessWebsite the website URL, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessWebsite(String businessWebsite) {
        this.businessWebsite = businessWebsite;
        return this;
    }

    /**
     * Returns the business email.
     *
     * @return Optional containing the email, empty for non-business accounts
     */
    public Optional<String> businessEmail() {
        return Optional.ofNullable(businessEmail);
    }

    /**
     * Sets the business email.
     *
     * @param businessEmail the email, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
        return this;
    }

    /**
     * Returns the business category.
     *
     * @return Optional containing the category, empty for non-business accounts
     */
    public Optional<BusinessCategory> businessCategory() {
        return Optional.ofNullable(businessCategory);
    }

    /**
     * Sets the business category.
     *
     * @param businessCategory the category, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setBusinessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
        return this;
    }

    /**
     * Returns the session properties.
     *
     * @return immutable map of properties, never null
     */
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns all status updates.
     *
     * @return immutable collection of status updates, never null
     */
    public Collection<ChatMessageInfo> status() {
        return Collections.unmodifiableCollection(status.values());
    }

    /**
     * Returns all calls.
     *
     * @return immutable collection of calls, never null
     */
    public Collection<Call> calls() {
        return Collections.unmodifiableCollection(calls.values());
    }

    /**
     * Returns the privacy settings.
     *
     * @return immutable privacy settings, never null
     */
    public Collection<PrivacySettingEntry> privacySettings() {
        return Collections.unmodifiableCollection(privacySettings.values());
    }

    /**
     * Returns whether chats unarchive automatically.
     *
     * @return true if chats unarchive on new messages, false otherwise
     */
    public boolean unarchiveChats() {
        return unarchiveChats;
    }

    /**
     * Sets whether chats unarchive automatically.
     *
     * @param unarchiveChats true to enable automatic unarchiving, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setUnarchiveChats(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
        return this;
    }

    /**
     * Returns whether 24-hour time format is used.
     *
     * @return true if using 24-hour format, false for 12-hour format
     */
    public boolean twentyFourHourFormat() {
        return twentyFourHourFormat;
    }

    /**
     * Sets whether to use 24-hour time format.
     *
     * @param twentyFourHourFormat true for 24-hour format, false for 12-hour format
     * @return this store instance for method chaining
     */
    public WhatsAppStore setTwentyFourHourFormat(boolean twentyFourHourFormat) {
        this.twentyFourHourFormat = twentyFourHourFormat;
        return this;
    }

    /**
     * Returns the initialization timestamp.
     *
     * @return the timestamp in seconds since Unix epoch
     */
    public long initializationTimeStamp() {
        return initializationTimeStamp;
    }

    /**
     * Returns the default ephemeral timer for new chats.
     *
     * @return the ephemeral timer, never null
     */
    public ChatEphemeralTimer newChatsEphemeralTimer() {
        return newChatsEphemeralTimer;
    }

    /**
     * Sets the default ephemeral timer for new chats.
     *
     * @param newChatsEphemeralTimer the ephemeral timer, never null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer) {
        this.newChatsEphemeralTimer = Objects.requireNonNull(newChatsEphemeralTimer, "newChatsEphemeralTimer cannot be null");
        return this;
    }

    /**
     * Returns the history sync policy.
     *
     * @return Optional containing the history policy, empty if not configured
     */
    public Optional<WhatsAppWebClientHistory> webHistoryPolicy() {
        return Optional.ofNullable(webHistoryPolicy);
    }

    /**
     * Sets the history sync policy.
     *
     * @param webHistoryPolicy the history policy, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setWebHistoryPolicy(WhatsAppWebClientHistory webHistoryPolicy) {
        this.webHistoryPolicy = webHistoryPolicy;
        return this;
    }

    /**
     * Returns whether automatic presence updates are enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean automaticPresenceUpdates() {
        return automaticPresenceUpdates;
    }

    /**
     * Sets whether to send automatic presence updates.
     *
     * @param automaticPresenceUpdates true to enable, false to disable
     * @return this store instance for method chaining
     */
    public WhatsAppStore setAutomaticPresenceUpdates(boolean automaticPresenceUpdates) {
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        return this;
    }

    /**
     * Returns whether automatic message receipts are enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean automaticMessageReceipts() {
        return automaticMessageReceipts;
    }

    /**
     * Sets whether to send automatic message receipts.
     *
     * @param automaticMessageReceipts true to enable, false to disable
     * @return this store instance for method chaining
     */
    public WhatsAppStore setAutomaticMessageReceipts(boolean automaticMessageReceipts) {
        this.automaticMessageReceipts = automaticMessageReceipts;
        return this;
    }

    /**
     * Returns whether patch MAC verification is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean checkPatchMacs() {
        return checkPatchMacs;
    }

    /**
     * Sets whether to verify patch MACs.
     *
     * @param checkPatchMacs true to enable verification, false to disable
     * @return this store instance for method chaining
     */
    public WhatsAppStore setCheckPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
        return this;
    }

    /**
     * Returns whether chats have been synchronized.
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedChats() {
        return syncedChats;
    }

    /**
     * Sets whether chats have been synchronized.
     *
     * @param syncedChats true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedChats(boolean syncedChats) {
        this.syncedChats = syncedChats;
        return this;
    }

    /**
     * Returns whether contacts have been synchronized.
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedContacts() {
        return syncedContacts;
    }

    /**
     * Sets whether contacts have been synchronized.
     *
     * @param syncedContacts true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedContacts(boolean syncedContacts) {
        this.syncedContacts = syncedContacts;
        return this;
    }

    /**
     * Returns whether newsletters have been synchronized.
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedNewsletters() {
        return syncedNewsletters;
    }

    /**
     * Sets whether newsletters have been synchronized.
     *
     * @param syncedNewsletters true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedNewsletters(boolean syncedNewsletters) {
        this.syncedNewsletters = syncedNewsletters;
        return this;
    }

    /**
     * Returns whether status updates have been synchronized.
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedStatus() {
        return syncedStatus;
    }

    /**
     * Sets whether status updates have been synchronized.
     *
     * @param syncedStatus true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedStatus(boolean syncedStatus) {
        this.syncedStatus = syncedStatus;
        return this;
    }

    /**
     * Returns whether web app state has been synchronized.
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedWebAppState() {
        return syncedWebAppState;
    }

    /**
     * Sets whether web app state has been synchronized.
     *
     * @param syncedWebAppState true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedWebAppState(boolean syncedWebAppState) {
        this.syncedWebAppState = syncedWebAppState;
        return this;
    }

    /**
     * Returns whether business certificate has been synchronized. (Mobile API only)
     *
     * @return true if synchronized, false otherwise
     */
    public boolean syncedBusinessCertificate() {
        return this.syncedBusinessCertificate;
    }
    /**
     * Sets whether business certificate has been synchronized. (Mobile API only)
     *
     * @param syncedBusinessCertificate true if synchronized, false otherwise
     * @return this store instance for method chaining
     */
    public WhatsAppStore setSyncedBusinessCertificate(boolean syncedBusinessCertificate) {
        this.syncedBusinessCertificate = syncedBusinessCertificate;
        return this;
    }

    public WhatsAppClientListener addListener(WhatsAppClientListener listener) {
        listeners.add(listener);
        return listener;
    }

    public boolean removeListener(WhatsAppClientListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Returns the event listeners.
     *
     * @return immutable collection of event listeners, never null
     */
    public Collection<WhatsAppClientListener> listeners() {
        return Collections.unmodifiableCollection(listeners);
    }


    public MediaConnection waitForMediaConnection() throws InterruptedException {
        if(mediaConnection == null) {
            synchronized (mediaConnectionLock) {
                if(mediaConnection == null) {
                    mediaConnectionLock.wait();
                }
            }
        }
        return mediaConnection;
    }

    /**
     * Sets the media connection.
     *
     * @param mediaConnection the media connection, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setMediaConnection(MediaConnection mediaConnection) {
        this.mediaConnection = mediaConnection;
        synchronized (mediaConnectionLock) {
            this.mediaConnectionLock.notifyAll();
        }
        return this;
    }

    /**
     * Returns the companion identity.
     *
     * @return Optional containing the companion identity, empty if not set
     */
    public Optional<SignedDeviceIdentity> companionIdentity() {
        return Optional.ofNullable(companionIdentity);
    }

    /**
     * Sets the companion identity.
     *
     * @param companionIdentity the companion identity, may be null
     * @return this store instance for method chaining
     */
    public WhatsAppStore setCompanionIdentity(SignedDeviceIdentity companionIdentity) {
        this.companionIdentity = companionIdentity;
        return this;
    }

    /**
     * Returns the signed key pair.
     *
     * @return the signed key pair, never null
     */
    public SignalSignedKeyPair signedKeyPair() {
        return signedKeyPair;
    }

    /**
     * Returns the FDID.
     *
     * @return the FDID, never null
     */
    public UUID fdid() {
        return fdid;
    }

    /**
     * Returns the device ID.
     *
     * @return the device ID, never null
     */
    public byte[] deviceId() {
        return deviceId;
    }

    /**
     * Returns the advertising ID.
     *
     * @return the advertising ID, never null
     */
    public UUID advertisingId() {
        return advertisingId;
    }

    /**
     * Returns the identity ID.
     *
     * @return the identity ID, never null
     */
    public byte[] identityId() {
        return identityId;
    }

    /**
     * Returns the backup token.
     *
     * @return the backup token, never null
     */
    public byte[] backupToken() {
        return backupToken;
    }

    /**
     * Returns the Signal protocol registration ID.
     *
     * @return the registration ID, a value between 1 and 16380
     */
    public int registrationId() {
        return this.registrationId;
    }

    /**
     * Returns the Noise protocol key pair.
     *
     * @return the non-null noise key pair used for secure channel establishment
     */
    public SignalIdentityKeyPair noiseKeyPair() {
        return this.noiseKeyPair;
    }

    /**
     * Returns the Signal protocol identity key pair.
     *
     * @return the non-null identity key pair used for end-to-end encryption
     */
    public SignalIdentityKeyPair identityKeyPair() {
        return this.identityKeyPair;
    }
    
    /**
     * Returns the companion device key pair.
     *
     * @return Optional containing the companion device key pair, empty if not set
     */
    public Optional<SignalIdentityKeyPair> companionKeyPair() {
        return Optional.ofNullable(companionKeyPair);
    }

    /**
     * Sets the companion device key pair.
     * <p>
     * This is typically called during the pairing process when establishing a
     * Web/Desktop client connection to a mobile device.
     *
     * @param companionKeyPair the new companion key pair, must not be null
     */
    public void setCompanionKeyPair(SignalIdentityKeyPair companionKeyPair) {
        this.companionKeyPair = companionKeyPair;
    }

    /**
     * Returns all registered pre-keys in the order they were added.
     *
     * @return a non-null sequenced collection of pre-key pairs
     */
    public SequencedCollection<SignalPreKeyPair> preKeys() {
        return preKeys.sequencedValues();
    }

    /**
     * Checks whether any pre-keys are currently available.
     * <p>
     * If this returns false, new pre-keys should be generated and uploaded to
     * WhatsApp servers to allow new sessions to be established.
     *
     * @return true if pre-keys are available, false otherwise
     */
    public boolean hasPreKeys() {
        return !preKeys.isEmpty();
    }

    /**
     * Finds a pre-key by its ID.
     *
     * @param id the pre-key ID to search for, may be null
     * @return an Optional containing the pre-key if found, empty otherwise
     */
    public Optional<SignalPreKeyPair> findPreKeyById(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(preKeys.get(id));
    }

    /**
     * Adds a pre-key to the collection.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface implementation.
     *
     * @param preKey the pre-key to add, must not be null
     * @throws NullPointerException if preKey is null
     */
    @Override
    public void addPreKey(SignalPreKeyPair preKey) {
        Objects.requireNonNull(preKey, "preKey cannot be null");
        preKeys.put(preKey.id(), preKey);
    }

    /**
     * Removes a pre-key from the collection.
     * <p>
     * Pre-keys are typically removed after being used to establish a new session,
     * ensuring they are only used once.
     *
     * @param id the ID of the pre-key to remove
     * @return true if a pre-key was removed, false if no pre-key with that ID existed
     */
    @Override
    public boolean removePreKey(int id) {
        return preKeys.remove(id) != null;
    }

    /**
     * Finds the signed pre-key by its ID.
     * <p>
     * Currently, only one signed pre-key is maintained at a time, so this method
     * only returns the current signed key pair if the ID matches.
     *
     * @param id the signed pre-key ID to search for
     * @return an Optional containing the signed pre-key if the ID matches, empty otherwise
     */
    @Override
    public Optional<SignalSignedKeyPair> findSignedPreKeyById(Integer id) {
        return id == signedKeyPair.id() ? Optional.of(signedKeyPair) : Optional.empty();
    }

    /**
     * Adds a signed pre-key.
     * <p>
     * This operation is not supported.
     *
     * @param signalSignedKeyPair the signed pre-key to add
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public void addSignedPreKey(SignalSignedKeyPair signalSignedKeyPair) {
        throw new UnsupportedOperationException("Cannot add signed pre keys to a Keys instance");
    }

    /**
     * Finds a Signal protocol session by address.
     *
     * @param address the address to search for, must not be null
     * @return an Optional containing the session if found, empty otherwise
     */
    @Override
    public Optional<SignalSessionRecord> findSessionByAddress(SignalProtocolAddress address) {
        return Optional.ofNullable(sessions.get(address));
    }

    /**
     * Adds or updates a Signal protocol session.
     *
     * @param address the address for this session, must not be null
     * @param record  the session record, must not be null
     */
    public void addSession(SignalProtocolAddress address, SignalSessionRecord record) {
        sessions.put(address, record);
    }

    /**
     * Finds a sender key by name for group messaging.
     *
     * @param name the sender key name (group + sender + device), must not be null
     * @return an Optional containing the sender key record if found, empty otherwise
     */
    @Override
    public Optional<SignalSenderKeyRecord> findSenderKeyByName(SignalSenderKeyName name) {
        return Optional.ofNullable(senderKeys.get(name));
    }

    /**
     * Adds or updates a sender key for group messaging.
     *
     * @param name      the sender key name, must not be null
     * @param newRecord the sender key record, must not be null
     */
    @Override
    public void addSenderKey(SignalSenderKeyName name, SignalSenderKeyRecord newRecord) {
        senderKeys.put(name, newRecord);
    }

    /**
     * Retrieves a sequenced collection of web app state keys.
     *
     * @return an unmodifiable sequenced collection
     */
    public SequencedCollection<AppStateSyncKey> appStateKeys() {
        return Collections.unmodifiableSequencedCollection(appStateKeys.sequencedValues());
    }

    /**
     * Finds an app state sync key by its key ID.
     *
     * @param id the key ID to search for, must not be null
     * @return an Optional containing the app state sync key if found, empty otherwise
     */
    public Optional<AppStateSyncKey> findWebAppStateKeyById(byte[] id) {
        return Optional.ofNullable(appStateKeys.get(HexFormat.of().formatHex(id)));
    }

    /**
     * Adds multiple app state sync keys to the collection.
     * <p>
     * Keys without a valid key ID are silently skipped.
     *
     * @param keys the collection of keys to add, must not be null
     */
    public void addWebAppStateKeys(Collection<AppStateSyncKey> keys) {
        for (var key : keys) {
            var keyId = key.keyId();
            if(keyId == null) {
                continue;
            }

            var keyIdValue = keyId.value();
            if(keyIdValue == null) {
                continue;
            }

            appStateKeys.put(HexFormat.of().formatHex(keyIdValue), key);
        }
    }

    /**
     * Finds a hash state by patch type.
     *
     * @param patchType the type of app state to query, must not be null
     * @return an Optional containing the hash state if found, empty otherwise
     */
    public Optional<AppStateSyncHash> findWebAppHashStateByName(PatchType patchType) {
        return Optional.ofNullable(hashStates.get(patchType));
    }

    /**
     * Adds or updates a hash state for app state synchronization.
     *
     * @param state the hash state to add, must not be null
     */
    public void addWebAppHashState(AppStateSyncHash state) {
        hashStates.put(state.type(), state);
    }

    /**
     * Adds a pending mutation to the queue for the specified collection.
     *
     * @param collectionName the collection name
     * @param patch the patch to queue
     */
    public void addPendingMutations(PatchType collectionName, Collection<? extends PendingMutation> patch) {
        webAppStatePendingMutations
            .computeIfAbsent(collectionName, k -> new ArrayList<>())
            .addAll(patch);
    }

    /**
     * Gets all pending mutations for the specified collection.
     *
     * @param collectionName the collection name
     * @return list of pending patches
     */
    public SequencedCollection<PendingMutation> findPendingMutations(PatchType collectionName) {
        var collectionPending = webAppStatePendingMutations.get(collectionName);
        return collectionPending == null ? List.of() : Collections.unmodifiableSequencedCollection(collectionPending);
    }

    /**
     * Removes pending mutations by their indices.
     *
     * @param collectionName the collection name
     */
    public void removePendingMutations(PatchType collectionName) {
        webAppStatePendingMutations.remove(collectionName);
    }

    /**
     * Clears all pending mutations for a collection.
     *
     * @param collectionName the collection name
     */
    public void clearPendingMutations(PatchType collectionName) {
        webAppStatePendingMutations.remove(collectionName);
    }

    /**
     * Returns whether the client has completed registration.
     *
     * @return true if registered, false otherwise
     */
    public boolean registered() {
        return this.registered;
    }

    /**
     * Sets the registration status.
     *
     * @param registered true if the client is now registered, false otherwise
     */
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /**
     * Checks if the given identity is trusted for the specified address.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface. Currently returns
     * false as trust verification is handled elsewhere.
     *
     * @param signalProtocolAddress       the address to check
     * @param signalIdentityPublicKey     the identity key to verify
     * @param signalKeyDirection          the direction of the key (sending or receiving)
     * @return false (trust verification not implemented here)
     */
    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey, SignalKeyDirection signalKeyDirection) {
        return true;
    }

    /**
     * Adds a trusted identity for the specified address.
     * <p>
     * This is part of the {@link SignalProtocolStore} interface. Currently a no-op
     * as trust management is handled elsewhere.
     *
     * @param signalProtocolAddress       the address
     * @param signalIdentityPublicKey     the identity key to trust
     */
    @Override
    public void addTrustedIdentity(SignalProtocolAddress signalProtocolAddress, SignalIdentityPublicKey signalIdentityPublicKey) {

    }

    /**
     * Marks a collection as dirty.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateDirty(PatchType collectionName) {
        webAppStateCollections.compute(collectionName, (_, current) -> {
            if (current == null || current.state() == CollectionState.UP_TO_DATE) {
                return new CollectionMetadata(
                        collectionName,
                        current != null ? current.version() : 0,
                        current != null ? MutationLTHash.copy(current.ltHash()) : MutationLTHash.copy(MutationLTHash.EMPTY_HASH),
                        System.currentTimeMillis(),
                        CollectionState.DIRTY,
                        0,  // Reset retry count
                        0   // Reset error timestamp
                );
            }
            return current;
        });
    }

    /**
     * Marks a collection as in-flight.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateInFlight(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        current.lastSyncTimestamp(),
                        CollectionState.IN_FLIGHT,
                        current.retryCount(),
                        current.lastErrorTimestamp()
                )
        );
    }

    /**
     * Marks a collection as up-to-date.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateUpToDate(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        System.currentTimeMillis(),
                        CollectionState.UP_TO_DATE,
                        0,  // Reset retry count on success
                        0   // Reset error timestamp
                )
        );
    }

    /**
     * Marks a collection as pending.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStatePending(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        current.lastSyncTimestamp(),
                        CollectionState.PENDING,
                        current.retryCount(),
                        current.lastErrorTimestamp()
                )
        );
    }

    /**
     * Marks a collection as blocked.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateBlocked(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        current.lastSyncTimestamp(),
                        CollectionState.BLOCKED,
                        current.retryCount(),
                        System.currentTimeMillis()
                )
        );
    }

    /**
     * Marks a collection in error retry state.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateErrorRetry(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        current.lastSyncTimestamp(),
                        CollectionState.ERROR_RETRY,
                        current.retryCount() + 1,
                        System.currentTimeMillis()
                )
        );
    }

    /**
     * Marks a collection in fatal error state.
     *
     * @param collectionName the collection name
     */
    public void markWebAppStateErrorFatal(PatchType collectionName) {
        webAppStateCollections.computeIfPresent(collectionName, (_, current) ->
                new CollectionMetadata(
                        current.name(),
                        current.version(),
                        current.ltHash(),
                        current.lastSyncTimestamp(),
                        CollectionState.ERROR_FATAL,
                        current.retryCount(),
                        System.currentTimeMillis()
                )
        );
    }

    /**
     * Gets metadata for a collection.
     *
     * @param collectionName the collection name
     * @return the collection metadata
     */
    public CollectionMetadata findWebAppState(PatchType collectionName) {
        return webAppStateCollections.computeIfAbsent(collectionName, key ->
                new CollectionMetadata(
                        key,
                        0,
                        MutationLTHash.copy(MutationLTHash.EMPTY_HASH),
                        0,
                        CollectionState.UP_TO_DATE,
                        0,
                        0
                )
        );
    }

    /**
     * Updates a collection's version and LT-Hash.
     *
     * @param collectionName the collection name
     * @param newVersion the new version
     * @param newLtHash the new LT-Hash
     */
    public void updateWebAppStateVersion(PatchType collectionName, long newVersion, byte[] newLtHash) {
        webAppStateCollections.compute(collectionName, (_, current) ->
                new CollectionMetadata(
                        collectionName,
                        newVersion,
                        MutationLTHash.copy(newLtHash),
                        System.currentTimeMillis(),
                        current != null ? current.state() : CollectionState.UP_TO_DATE,
                        0,  // Reset retry count on successful update
                        0   // Reset error timestamp
                )
        );
    }

    // =====================================================
    // SECTION: Object Methods
    // =====================================================


    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof WhatsAppStore that
               && serializable == that.serializable
               && online == that.online
               && unarchiveChats == that.unarchiveChats
               && twentyFourHourFormat == that.twentyFourHourFormat
               && automaticPresenceUpdates == that.automaticPresenceUpdates
               && automaticMessageReceipts == that.automaticMessageReceipts
               && checkPatchMacs == that.checkPatchMacs
               && syncedChats == that.syncedChats
               && syncedContacts == that.syncedContacts
               && syncedNewsletters == that.syncedNewsletters
               && syncedStatus == that.syncedStatus
               && syncedWebAppState == that.syncedWebAppState
               && syncedBusinessCertificate == that.syncedBusinessCertificate
               && registered == that.registered
               && Objects.equals(uuid, that.uuid)
               && Objects.equals(phoneNumber, that.phoneNumber)
               && clientType == that.clientType
               && Objects.equals(serializer, that.serializer)
               && Objects.equals(initializationTimeStamp, that.initializationTimeStamp)
               && Objects.equals(proxy, that.proxy)
               && Objects.equals(device, that.device)
               && releaseChannel == that.releaseChannel
               && Objects.equals(locale, that.locale)
               && Objects.equals(name, that.name)
               && Objects.equals(verifiedName, that.verifiedName)
               && Objects.equals(profilePicture, that.profilePicture)
               && Objects.equals(about, that.about)
               && Objects.equals(jid, that.jid)
               && Objects.equals(lid, that.lid)
               && Objects.equals(businessAddress, that.businessAddress)
               && Objects.equals(businessLongitude, that.businessLongitude)
               && Objects.equals(businessLatitude, that.businessLatitude)
               && Objects.equals(businessDescription, that.businessDescription)
               && Objects.equals(businessWebsite, that.businessWebsite)
               && Objects.equals(businessEmail, that.businessEmail)
               && Objects.equals(businessCategory, that.businessCategory)
               && Objects.equals(chats, that.chats)
               && Objects.equals(newsletters, that.newsletters)
               && Objects.equals(status, that.status)
               && Objects.equals(contacts, that.contacts)
               && Objects.equals(calls, that.calls)
               && Objects.equals(privacySettings, that.privacySettings)
               && Objects.equals(properties, that.properties)
               && newChatsEphemeralTimer == that.newChatsEphemeralTimer
               && Objects.equals(webHistoryPolicy, that.webHistoryPolicy)
               && Objects.equals(registrationId, that.registrationId)
               && Objects.equals(noiseKeyPair, that.noiseKeyPair)
               && Objects.equals(identityKeyPair, that.identityKeyPair)
               && Objects.equals(companionKeyPair, that.companionKeyPair)
               && Objects.equals(companionIdentity, that.companionIdentity)
               && Objects.equals(signedKeyPair, that.signedKeyPair)
               && Objects.equals(preKeys, that.preKeys)
               && Objects.equals(fdid, that.fdid)
               && Objects.deepEquals(deviceId, that.deviceId)
               && Objects.equals(advertisingId, that.advertisingId)
               && Objects.deepEquals(identityId, that.identityId)
               && Objects.deepEquals(backupToken, that.backupToken)
               && Objects.equals(senderKeys, that.senderKeys)
               && Objects.equals(appStateKeys, that.appStateKeys)
               && Objects.equals(sessions, that.sessions)
               && Objects.equals(hashStates, that.hashStates)
               && Objects.equals(listeners, that.listeners)
               && Objects.equals(mediaConnection, that.mediaConnection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, phoneNumber, clientType, serializer, serializable,
                initializationTimeStamp, proxy, device, releaseChannel,
                online, locale, name, verifiedName, profilePicture,
                about, jid, lid, businessAddress, businessLongitude, businessLatitude,
                businessDescription, businessWebsite, businessEmail, businessCategory,
                chats, newsletters, status, contacts, calls, privacySettings, properties,
                unarchiveChats, twentyFourHourFormat, newChatsEphemeralTimer, webHistoryPolicy,
                automaticPresenceUpdates, automaticMessageReceipts, checkPatchMacs, syncedChats, 
                syncedContacts, syncedNewsletters, syncedStatus, syncedWebAppState, syncedBusinessCertificate,
                registrationId, noiseKeyPair, identityKeyPair, companionKeyPair, companionIdentity,
                signedKeyPair, preKeys, fdid, Arrays.hashCode(deviceId), advertisingId, Arrays.hashCode(identityId), 
                Arrays.hashCode(backupToken), senderKeys, appStateKeys, sessions, hashStates, registered, listeners, mediaConnection);
    }

    @Override
    public String toString() {
        return "WhatsappStore[" +
               "uuid=" + uuid +
               ", phoneNumber=" + phoneNumber +
               ", clientType=" + clientType +
               ", jid=" + jid +
               ']';
    }

    public Optional<PrivacySettingEntry> findPrivacySetting(PrivacySettingType type) {
        return type == null
                ? Optional.empty()
                : Optional.ofNullable(privacySettings.get(type));
    }

    public void addPrivacySetting(PrivacySettingEntry entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        privacySettings.put(entry.type(), entry);
    }

    public Optional<ChatMessageInfo> findChatMessageByKey(ChatMessageKey key) {
        var chat = chats.get(key.chatJid());
        if(chat == null) {
            return Optional.empty();
        }

        return chat.getMessageById(key.id());
    }

    public boolean showSecurityNotifications() {
        return showSecurityNotifications;
    }

    public void setShowSecurityNotifications(boolean showSecurityNotifications) {
        this.showSecurityNotifications = showSecurityNotifications;
    }

    public Optional<Sticker> findRecentSticker(String stickerHash) {
        return Optional.ofNullable(recentStickers.get(stickerHash));
    }

    public void addRecentSticker(String stickerHash, Sticker sticker) {
        Objects.requireNonNull(stickerHash, "stickerHash cannot be null");
        Objects.requireNonNull(sticker, "sticker cannot be null");
        recentStickers.put(stickerHash, sticker);
    }

    public Optional<Sticker> removeRecentSticker(String stickerHash) {
        return Optional.ofNullable(recentStickers.remove(stickerHash));
    }

    public Optional<Sticker> findFavouriteSticker(String stickerHash) {
        return Optional.ofNullable(favouriteStickers.get(stickerHash));
    }

    public void addFavouriteSticker(String stickerHash, Sticker sticker) {
        Objects.requireNonNull(stickerHash, "stickerHash cannot be null");
        Objects.requireNonNull(sticker, "sticker cannot be null");
        favouriteStickers.put(stickerHash, sticker);
    }

    public Optional<Sticker> removeFavouriteSticker(String stickerHash) {
        return Optional.ofNullable(favouriteStickers.remove(stickerHash));
    }

    public Optional<QuickReply> findQuickReply(String shortcut) {
        return Optional.ofNullable(quickReplies.get(shortcut));
    }

    public void addQuickReply(QuickReply action) {
        Objects.requireNonNull(action, "action cannot be null");
        quickReplies.put(action.shortcut(), action);
    }

    public Optional<Label> findLabel(int labelId) {
        return Optional.ofNullable(labels.get(labelId));
    }

    public Optional<Label> removeLabel(int labelId) {
        return Optional.ofNullable(labels.remove(labelId));
    }

    public void addLabel(Label label) {
        Objects.requireNonNull(label, "label cannot be null");
        labels.put(label.id(), label);
    }

    public Optional<QuickReply> removeQuickReply(String shortcut) {
        return shortcut == null
                ? Optional.empty()
                : Optional.ofNullable(quickReplies.remove(shortcut));
    }

    public Version clientVersion() {
        if(clientVersion == null) {
            synchronized (clientVersionLock) {
                if(clientVersion == null) {
                    clientVersion = WhatsAppClientInfo.of(device.platform())
                            .version();
                }
            }
        }
        return clientVersion;
    }

    public void setClientVersion(Version clientVersion) {
        this.clientVersion = clientVersion;
    }

    public Optional<Version> companionVersion() {
        return Optional.ofNullable(companionVersion);
    }

    public void setCompanionVersion(Version companionVersion) {
        this.companionVersion = companionVersion;
    }

    /**
     * Gets group metadata.
     *
     * @param groupJid the group JID
     * @return the group metadata, or empty if not found
     */
    public Optional<GroupOrCommunityMetadata> findGroupOrCommunityMetadata(Jid groupJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        return Optional.ofNullable(groupOrCommunityMetadata.get(groupJid));
    }

    /**
     * Stores group metadata.
     *
     * @param groupData the group metadata
     */
    public void addGroupOrCommunityMetadata(GroupOrCommunityMetadata groupData) {
        Objects.requireNonNull(groupData, "groupData cannot be null");
        groupOrCommunityMetadata.put(groupData.jid(), groupData);
    }

    /**
     * Clears group metadata.
     *
     * @param groupJid the group JID
     */
    public void removeGroupOrCommunityMetadata(Jid groupJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        groupOrCommunityMetadata.remove(groupJid);
    }

    /**
     * Gets the device list for a user.
     *
     * @param userJid the user JID
     * @return the device list, or empty if not cached
     */
    public SequencedCollection<Jid> findDeviceList(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        var deviceList = deviceLists.get(userJid);
        return deviceList == null ? List.of() : Collections.unmodifiableSequencedCollection(deviceList);
    }

    /**
     * Stores a device list for a user.
     *
     * @param userJid the user JID
     * @param deviceList the device list
     */
    public void addDeviceList(Jid userJid, SequencedCollection<Jid> deviceList) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(deviceList, "deviceList cannot be null");
        deviceLists.put(userJid, deviceList);
    }

    /**
     * Adds a device to a user's device list.
     *
     * @param userJid the user JID
     * @param deviceJid the device JID to add
     */
    public void addDevice(Jid userJid, Jid deviceJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");

        deviceLists.compute(userJid, (key, existing) -> {
            if (existing == null) {
                existing = new ArrayList<>();
            }
            existing.add(deviceJid);
            return existing;
        });
    }

    /**
     * Removes a device from a user's device list.
     *
     * @param userJid the user JID
     * @param deviceJid the device JID to remove
     */
    public void removeDevice(Jid userJid, Jid deviceJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        deviceLists.computeIfPresent(userJid, (key, existing) -> {
            existing.remove(deviceJid);
            return existing;
        });
    }

    /**
     * Clears the device list cache for a user.
     *
     * @param userJid the user JID
     */
    public void removeDevices(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        deviceLists.remove(userJid);
    }

    public boolean hasJid(JidProvider entry) {
        if(entry == null) {
            return false;
        } else {
            var localJid = jid;
            var localLid = lid;
            var remoteJid = entry.toJid();
            return remoteJid.equals(localJid) || remoteJid.equals(localLid);
        }
    }

    public boolean hasUserJid(JidProvider entry) {
        if(entry == null) {
            return false;
        } else {
            var localJid = jid;
            var localLid = lid;
            var remoteJid = entry.toJid();
            return (localJid != null && remoteJid.hasUser(localJid.user()))
                    || (localLid != null && remoteJid.hasUser(localLid.user()));
        }
    }
}