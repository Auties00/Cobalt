package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.client.linked.info.LinkedWhatsAppClientInfo;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategory;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.UsernameState;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPayload.ClientReleaseChannel;
import com.github.auties00.cobalt.wire.linked.device.pairing.LinkedPrimaryPlatform;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WaffleAccountLinkStateAction;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed {@link LinkedWhatsAppAccountStore} holding this session's identity and profile state.
 *
 * <p>This is a nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore}; it owns the session
 * UUID, phone number, client type, the phone-number JID and LID, the device descriptor and release
 * channel, the local and companion app versions, the display/verified names, profile picture and
 * "About" text, the WhatsApp Business profile fields, and the registration, presence, ADV-check and
 * linked-Meta-account state.
 *
 * @implNote
 * This implementation defaults the initialization timestamp ({@code Instant.now()}) and release
 * channel ({@link ClientReleaseChannel#RELEASE}) when absent and lazily derives
 * {@link #clientVersion()} from the device platform under {@link #clientVersionLock}. The linked-Meta
 * fields and the lock are transient (not persisted).
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ProtobufLinkedWhatsAppAccountStore implements LinkedWhatsAppAccountStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppAccountStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppAccountStore.class);

    /**
     * The stable per-account identifier scoping on-disk state, log tags and the pairing handshake.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final UUID uuid;

    /**
     * The E.164 phone number of the logged-in account, or {@code null} until the server confirms registration.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    private Long phoneNumber;

    /**
     * The flavour of WhatsApp client this store was initialised for.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    private final LinkedWhatsAppClientType clientType;

    /**
     * The wall-clock timestamp at which this store was first created.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    private final Instant initializationTimeStamp;

    /**
     * The device descriptor advertised during pairing and bundled into every client payload.
     *
     * @implNote
     * The sealed {@link LinkedWhatsAppClientDevice} hierarchy cannot itself be a protobuf
     * {@code MESSAGE} field, so it is persisted through the custom
     * {@link LinkedWhatsAppClientDevice#serialize()}/{@link LinkedWhatsAppClientDevice#deserialize(byte[])}
     * converter as a discriminated {@code BYTES} payload.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    private LinkedWhatsAppClientDevice device;

    /**
     * The release channel advertised to the server during connection.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    private ClientReleaseChannel releaseChannel;

    /**
     * Whether the local account is currently advertising itself as online via presence stanzas.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    private boolean online;

    /**
     * The IETF language tag advertised to the server for server-rendered notifications.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private String locale;

    /**
     * The pushname (display name) the account broadcasts to its peers.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    private String name;

    /**
     * The server-attested business display name on Business Verified accounts.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    private String verifiedName;

    /**
     * The CDN URI for the most recently fetched copy of the local account profile picture.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    private URI profilePicture;

    /**
     * The local account published "About" text status.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    private ContactTextStatus selfTextStatus;

    /**
     * The local account phone-number JID.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    private Jid jid;

    /**
     * The local account LID, the opaque identifier that hides the phone number in groups.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    private Jid lid;

    /**
     * The free-text street address shown on the business profile.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    private String businessAddress;

    /**
     * The longitude of the business profile map pin.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.DOUBLE)
    private Double businessLongitude;

    /**
     * The latitude of the business profile map pin.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.DOUBLE)
    private Double businessLatitude;

    /**
     * The free-text "About this business" description rendered on the business profile.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.STRING)
    private String businessDescription;

    /**
     * The ordered list of website URIs published on the business profile.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    private List<URI> businessWebsites;

    /**
     * The contact email shown on the business profile.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    private String businessEmail;

    /**
     * The Meta-defined business categories assigned to the business profile.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
    private List<BusinessCategory> businessCategories;

    /**
     * Whether this device has completed the pairing handshake and is registered with the server.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    private boolean registered;

    /**
     * The advertised application version this client identifies itself with on the server.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    private volatile ClientAppVersion clientVersion;

    /**
     * The currently observed application version of the paired primary device.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    private ClientAppVersion companionVersion;

    /**
     * The timestamp of the most recent successful ADV revalidation against the primary device.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant lastAdvCheckTime;

    /**
     * The platform of the linked primary device, captured at pair-success.
     *
     * <p>Exposed through {@link LinkedWhatsAppAccountStore#primaryPlatform()}.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.ENUM)
    private LinkedPrimaryPlatform primaryPlatform;

    /**
     * The companion-side authentication nonce for the MMS history-sync blob release.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.STRING)
    private String companionMmsAuthNonce;

    /**
     * The per-account key protecting the opaque chat identifier in shareable chat links.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BYTES)
    private byte[] shareableChatLinkKey;

    /**
     * The account's own assigned username, or {@code null} until one is reserved or set.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.STRING)
    private String username;

    /**
     * The registration state of the account's username, decoded from the GetUsername reply.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.ENUM)
    private UsernameState usernameState;

    /**
     * Whether a username recovery PIN is set on the account, or {@code null} when unknown.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.BOOL)
    private Boolean usernameHasRecoveryPin;

    /**
     * Whether the inactive-group LID migration sweep has completed for this account.
     *
     * @implNote
     * This implementation is {@code volatile} because it is written on the migration sweep's scheduled
     * thread and read on the connection thread that arms the sweep. It backs WhatsApp Web's
     * {@code UserPrefs.InactiveGroupLidMigrationComplete} entry, so persisting it lets a re-connecting
     * client skip the sweep once it has run to completion.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BOOL)
    private volatile boolean inactiveGroupLidMigrationComplete;

    /**
     * The monitor guarding the lazy initialisation of {@link #clientVersion}; not persisted.
     */
    private final Object clientVersionLock;

    /**
     * The link state between this WhatsApp account and a Meta account; not persisted.
     */
    private volatile WaffleAccountLinkStateAction.AccountLinkState linkedMetaAccountState;

    /**
     * The timestamp of the most recent {@link #linkedMetaAccountState} transition; not persisted.
     */
    private volatile Instant linkedMetaAccountStateTimestamp;

    /**
     * The companion device pairing expiration deadline; not persisted.
     */
    private Instant clientExpiration;

    /**
     * The list of linked companion devices; not persisted.
     */
    private volatile List<Jid> linkedDevices;

    /**
     * The timestamp of when this device paired with the primary; not persisted.
     */
    private volatile Instant pairingTimestamp;

    /**
     * Whether the account has a profile avatar; not persisted.
     */
    private Boolean hasAvatar;

    /**
     * The salt for notification-content-token hashing; not persisted.
     */
    private byte[] notificationContentTokenSalt;

    /**
     * Constructs an account sub-store, defaulting the initialization timestamp, release channel and
     * business collections when absent.
     *
     * @param uuid                    the session UUID, never {@code null}
     * @param phoneNumber             the phone number, or {@code null}
     * @param clientType              the client type, never {@code null}
     * @param initializationTimeStamp the creation timestamp, or {@code null} for now
     * @param device                  the device descriptor, never {@code null}
     * @param releaseChannel          the release channel, or {@code null} for {@link ClientReleaseChannel#RELEASE}
     * @param online                  whether the account is advertised online
     * @param locale                  the locale, or {@code null}
     * @param name                    the display name, or {@code null}
     * @param verifiedName            the verified business name, or {@code null}
     * @param profilePicture          the profile picture URI, or {@code null}
     * @param selfTextStatus          the self text status, or {@code null}
     * @param jid                     the account JID, or {@code null}
     * @param lid                     the account LID, or {@code null}
     * @param businessAddress         the business address, or {@code null}
     * @param businessLongitude       the business longitude, or {@code null}
     * @param businessLatitude        the business latitude, or {@code null}
     * @param businessDescription     the business description, or {@code null}
     * @param businessWebsites        the business websites, or {@code null} for an empty list
     * @param businessEmail           the business email, or {@code null}
     * @param businessCategories      the business categories, or {@code null} for an empty list
     * @param registered              whether the device is registered
     * @param clientVersion           the advertised client version, or {@code null} for the derived default
     * @param companionVersion        the observed companion version, or {@code null}
     * @param lastAdvCheckTime        the last ADV check time, or {@code null}
     * @param primaryPlatform         the linked-primary platform, or {@code null}
     * @param companionMmsAuthNonce   the MMS auth nonce, or {@code null}
     * @param shareableChatLinkKey    the shareable-chat-link key, or {@code null}
     * @param username                the assigned username, or {@code null}
     * @param usernameState           the username registration state, or {@code null}
     * @param usernameHasRecoveryPin  whether a username recovery PIN is set, or {@code null} when unknown
     * @param inactiveGroupLidMigrationComplete whether the inactive-group LID migration sweep has completed
     */
    ProtobufLinkedWhatsAppAccountStore(UUID uuid, Long phoneNumber, LinkedWhatsAppClientType clientType, Instant initializationTimeStamp, LinkedWhatsAppClientDevice device, ClientReleaseChannel releaseChannel, boolean online, String locale, String name, String verifiedName, URI profilePicture, ContactTextStatus selfTextStatus, Jid jid, Jid lid, String businessAddress, Double businessLongitude, Double businessLatitude, String businessDescription, List<URI> businessWebsites, String businessEmail, List<BusinessCategory> businessCategories, boolean registered, ClientAppVersion clientVersion, ClientAppVersion companionVersion, Instant lastAdvCheckTime, LinkedPrimaryPlatform primaryPlatform, String companionMmsAuthNonce, byte[] shareableChatLinkKey, String username, UsernameState usernameState, Boolean usernameHasRecoveryPin, boolean inactiveGroupLidMigrationComplete) {
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.phoneNumber = phoneNumber;
        this.clientType = Objects.requireNonNull(clientType, "clientType cannot be null");
        this.initializationTimeStamp = requireNonNullElseGet(initializationTimeStamp, Instant::now);
        this.device = Objects.requireNonNull(device, "device cannot be null");
        this.releaseChannel = requireNonNullElse(releaseChannel, ClientReleaseChannel.RELEASE);
        this.online = online;
        this.locale = locale;
        this.name = name;
        this.verifiedName = verifiedName;
        this.profilePicture = profilePicture;
        this.selfTextStatus = selfTextStatus;
        this.jid = jid;
        this.lid = lid;
        this.businessAddress = businessAddress;
        this.businessLongitude = businessLongitude;
        this.businessLatitude = businessLatitude;
        this.businessDescription = businessDescription;
        this.businessWebsites = requireNonNullElseGet(businessWebsites, ArrayList::new);
        this.businessEmail = businessEmail;
        this.businessCategories = requireNonNullElseGet(businessCategories, ArrayList::new);
        this.registered = registered;
        this.clientVersion = clientVersion;
        this.companionVersion = companionVersion;
        this.lastAdvCheckTime = lastAdvCheckTime;
        this.primaryPlatform = primaryPlatform;
        this.companionMmsAuthNonce = companionMmsAuthNonce;
        this.shareableChatLinkKey = shareableChatLinkKey;
        this.username = username;
        this.usernameState = usernameState;
        this.usernameHasRecoveryPin = usernameHasRecoveryPin;
        this.inactiveGroupLidMigrationComplete = inactiveGroupLidMigrationComplete;
        this.clientVersionLock = new Object();
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public OptionalLong phoneNumber() {
        return phoneNumber == null ? OptionalLong.empty() : OptionalLong.of(phoneNumber);
    }

    @Override
    public LinkedWhatsAppAccountStore setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    @Override
    public LinkedWhatsAppClientType clientType() {
        return clientType;
    }

    @Override
    public Instant initializationTimeStamp() {
        return initializationTimeStamp;
    }

    @Override
    public LinkedWhatsAppClientDevice device() {
        return device;
    }

    @Override
    public LinkedWhatsAppAccountStore setDevice(LinkedWhatsAppClientDevice device) {
        this.device = Objects.requireNonNull(device, "device cannot be null");
        return this;
    }

    @Override
    public ClientReleaseChannel releaseChannel() {
        return releaseChannel;
    }

    @Override
    public LinkedWhatsAppAccountStore setReleaseChannel(ClientReleaseChannel releaseChannel) {
        this.releaseChannel = Objects.requireNonNull(releaseChannel, "releaseChannel cannot be null");
        return this;
    }

    @Override
    public boolean online() {
        return online;
    }

    @Override
    public LinkedWhatsAppAccountStore setOnline(boolean online) {
        this.online = online;
        return this;
    }

    @Override
    public Optional<String> locale() {
        return Optional.ofNullable(locale);
    }

    @Override
    public LinkedWhatsAppAccountStore setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public LinkedWhatsAppAccountStore setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Optional<String> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }

    @Override
    public LinkedWhatsAppAccountStore setVerifiedName(String verifiedName) {
        this.verifiedName = verifiedName;
        return this;
    }

    @Override
    public Optional<LinkedPrimaryPlatform> primaryPlatform() {
        return Optional.ofNullable(primaryPlatform);
    }

    @Override
    public LinkedWhatsAppAccountStore setPrimaryPlatform(LinkedPrimaryPlatform primaryPlatform) {
        this.primaryPlatform = primaryPlatform;
        return this;
    }

    @Override
    public Optional<URI> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    @Override
    public LinkedWhatsAppAccountStore setProfilePicture(URI profilePicture) {
        this.profilePicture = profilePicture;
        return this;
    }

    @Override
    public Optional<ContactTextStatus> selfTextStatus() {
        return Optional.ofNullable(selfTextStatus);
    }

    @Override
    public LinkedWhatsAppAccountStore setSelfTextStatus(ContactTextStatus selfTextStatus) {
        this.selfTextStatus = selfTextStatus;
        return this;
    }

    @Override
    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }

    @Override
    public LinkedWhatsAppAccountStore setJid(Jid jid) {
        this.jid = jid;
        return this;
    }

    @Override
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    @Override
    public LinkedWhatsAppAccountStore setLid(Jid lid) {
        this.lid = lid;
        return this;
    }

    @Override
    public Optional<String> businessAddress() {
        return Optional.ofNullable(businessAddress);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
        return this;
    }

    @Override
    public OptionalDouble businessLongitude() {
        return businessLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(businessLongitude);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
        return this;
    }

    @Override
    public OptionalDouble businessLatitude() {
        return businessLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(businessLatitude);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
        return this;
    }

    @Override
    public Optional<String> businessDescription() {
        return Optional.ofNullable(businessDescription);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
        return this;
    }

    @Override
    public List<URI> businessWebsites() {
        return businessWebsites == null ? List.of() : List.copyOf(businessWebsites);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessWebsites(List<URI> businessWebsites) {
        this.businessWebsites = businessWebsites;
        return this;
    }

    @Override
    public Optional<String> businessEmail() {
        return Optional.ofNullable(businessEmail);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
        return this;
    }

    @Override
    public List<BusinessCategory> businessCategories() {
        return businessCategories == null ? List.of() : List.copyOf(businessCategories);
    }

    @Override
    public LinkedWhatsAppAccountStore setBusinessCategories(List<BusinessCategory> businessCategories) {
        this.businessCategories = businessCategories;
        return this;
    }

    @Override
    public boolean registered() {
        return registered;
    }

    @Override
    public LinkedWhatsAppAccountStore setRegistered(boolean registered) {
        this.registered = registered;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation lazily derives the version from the {@link #device()} platform on first
     * access under {@link #clientVersionLock} using double-checked locking, so the probe runs at
     * most once per store instance.
     */
    @Override
    public ClientAppVersion clientVersion() {
        if (clientVersion == null) {
            synchronized (clientVersionLock) {
                if (clientVersion == null) {
                    clientVersion = LinkedWhatsAppClientInfo.of(device.platform()).version();
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "derived client version {0} for platform {1}", clientVersion, device.platform());
                    }
                }
            }
        }
        return clientVersion;
    }

    @Override
    public LinkedWhatsAppAccountStore setClientVersion(ClientAppVersion version) {
        this.clientVersion = version;
        return this;
    }

    @Override
    public Optional<ClientAppVersion> companionVersion() {
        return Optional.ofNullable(companionVersion);
    }

    @Override
    public LinkedWhatsAppAccountStore setCompanionVersion(ClientAppVersion version) {
        this.companionVersion = version;
        return this;
    }

    @Override
    public Optional<Instant> lastAdvCheckTime() {
        return Optional.ofNullable(lastAdvCheckTime);
    }

    @Override
    public void updateAdvCheckTime() {
        this.lastAdvCheckTime = Instant.now();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "adv revalidation succeeded");
    }

    @Override
    public Optional<WaffleAccountLinkStateAction.AccountLinkState> linkedMetaAccountState() {
        return Optional.ofNullable(linkedMetaAccountState);
    }

    @Override
    public LinkedWhatsAppAccountStore setLinkedMetaAccountState(WaffleAccountLinkStateAction.AccountLinkState state) {
        this.linkedMetaAccountState = state;
        return this;
    }

    @Override
    public Optional<Instant> linkedMetaAccountStateTimestamp() {
        return Optional.ofNullable(linkedMetaAccountStateTimestamp);
    }

    @Override
    public LinkedWhatsAppAccountStore setLinkedMetaAccountStateTimestamp(Instant timestamp) {
        this.linkedMetaAccountStateTimestamp = timestamp;
        return this;
    }

    @Override
    public Optional<Instant> clientExpiration() {
        return Optional.ofNullable(clientExpiration);
    }

    @Override
    public LinkedWhatsAppAccountStore setClientExpiration(Instant clientExpiration) {
        this.clientExpiration = clientExpiration;
        return this;
    }

    @Override
    public List<Jid> linkedDevices() {
        var current = linkedDevices;
        return current == null ? List.of() : List.copyOf(current);
    }

    @Override
    public LinkedWhatsAppAccountStore setLinkedDevices(Collection<Jid> linkedDevices) {
        this.linkedDevices = linkedDevices == null ? null : List.copyOf(linkedDevices);
        return this;
    }

    @Override
    public Optional<Instant> pairingTimestamp() {
        return Optional.ofNullable(pairingTimestamp);
    }

    @Override
    public LinkedWhatsAppAccountStore setPairingTimestamp(Instant pairingTimestamp) {
        this.pairingTimestamp = pairingTimestamp;
        return this;
    }

    @Override
    public Optional<Boolean> hasAvatar() {
        return Optional.ofNullable(hasAvatar);
    }

    @Override
    public LinkedWhatsAppAccountStore setHasAvatar(Boolean hasAvatar) {
        this.hasAvatar = hasAvatar;
        return this;
    }

    @Override
    public Optional<byte[]> notificationContentTokenSalt() {
        return Optional.ofNullable(notificationContentTokenSalt);
    }

    @Override
    public LinkedWhatsAppAccountStore setNotificationContentTokenSalt(byte[] salt) {
        this.notificationContentTokenSalt = salt;
        return this;
    }

    @Override
    public Optional<String> companionMmsAuthNonce() {
        return Optional.ofNullable(companionMmsAuthNonce);
    }

    @Override
    public LinkedWhatsAppAccountStore setCompanionMmsAuthNonce(String nonce) {
        this.companionMmsAuthNonce = nonce;
        return this;
    }

    @Override
    public Optional<byte[]> shareableChatLinkKey() {
        return Optional.ofNullable(shareableChatLinkKey);
    }

    @Override
    public LinkedWhatsAppAccountStore setShareableChatLinkKey(byte[] key) {
        this.shareableChatLinkKey = key;
        return this;
    }

    @Override
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    @Override
    public LinkedWhatsAppAccountStore setUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public Optional<UsernameState> usernameState() {
        return Optional.ofNullable(usernameState);
    }

    @Override
    public LinkedWhatsAppAccountStore setUsernameState(UsernameState usernameState) {
        this.usernameState = usernameState;
        return this;
    }

    @Override
    public Optional<Boolean> usernameHasRecoveryPin() {
        return Optional.ofNullable(usernameHasRecoveryPin);
    }

    @Override
    public LinkedWhatsAppAccountStore setUsernameHasRecoveryPin(Boolean usernameHasRecoveryPin) {
        this.usernameHasRecoveryPin = usernameHasRecoveryPin;
        return this;
    }

    @Override
    public boolean inactiveGroupLidMigrationComplete() {
        return inactiveGroupLidMigrationComplete;
    }

    @Override
    public LinkedWhatsAppAccountStore setInactiveGroupLidMigrationComplete(boolean inactiveGroupLidMigrationComplete) {
        this.inactiveGroupLidMigrationComplete = inactiveGroupLidMigrationComplete;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtobufLinkedWhatsAppAccountStore that)) {
            return false;
        }
        return online == that.online
               && registered == that.registered
               && inactiveGroupLidMigrationComplete == that.inactiveGroupLidMigrationComplete
               && Objects.equals(uuid, that.uuid)
               && Objects.equals(phoneNumber, that.phoneNumber)
               && clientType == that.clientType
               && Objects.equals(initializationTimeStamp, that.initializationTimeStamp)
               && Objects.equals(device, that.device)
               && releaseChannel == that.releaseChannel
               && Objects.equals(locale, that.locale)
               && Objects.equals(name, that.name)
               && Objects.equals(verifiedName, that.verifiedName)
               && Objects.equals(profilePicture, that.profilePicture)
               && Objects.equals(selfTextStatus, that.selfTextStatus)
               && Objects.equals(jid, that.jid)
               && Objects.equals(lid, that.lid)
               && Objects.equals(businessAddress, that.businessAddress)
               && Objects.equals(businessLongitude, that.businessLongitude)
               && Objects.equals(businessLatitude, that.businessLatitude)
               && Objects.equals(businessDescription, that.businessDescription)
               && Objects.equals(businessWebsites, that.businessWebsites)
               && Objects.equals(businessEmail, that.businessEmail)
               && Objects.equals(businessCategories, that.businessCategories)
               && Objects.equals(clientVersion, that.clientVersion)
               && Objects.equals(companionVersion, that.companionVersion)
               && Objects.equals(lastAdvCheckTime, that.lastAdvCheckTime)
               && Objects.equals(primaryPlatform, that.primaryPlatform)
               && Objects.equals(companionMmsAuthNonce, that.companionMmsAuthNonce)
               && Arrays.equals(shareableChatLinkKey, that.shareableChatLinkKey)
               && Objects.equals(username, that.username)
               && Objects.equals(usernameState, that.usernameState)
               && Objects.equals(usernameHasRecoveryPin, that.usernameHasRecoveryPin)
               && Objects.equals(linkedMetaAccountState, that.linkedMetaAccountState)
               && Objects.equals(linkedMetaAccountStateTimestamp, that.linkedMetaAccountStateTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, phoneNumber, clientType, initializationTimeStamp, device, releaseChannel,
                online, locale, name, verifiedName, profilePicture, selfTextStatus, jid, lid,
                businessAddress, businessLongitude, businessLatitude, businessDescription, businessWebsites,
                businessEmail, businessCategories, registered, clientVersion, companionVersion, lastAdvCheckTime,
                primaryPlatform, companionMmsAuthNonce, Arrays.hashCode(shareableChatLinkKey),
                username, usernameState, usernameHasRecoveryPin, inactiveGroupLidMigrationComplete,
                linkedMetaAccountState, linkedMetaAccountStateTimestamp);
    }
}
