package it.auties.whatsapp.controller;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactBuilder;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.MessageStatusInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterMetadata;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.signal.auth.UserAgent.ReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;
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
 * This controller holds the user-related data regarding a WhatsappWeb session
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
public final class Store extends Controller {
    /**
     * The version used by this session
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    URI proxy;

    /**
     * The version used by this session
     */
    Version version;

    /**
     * Whether this account is online for other users
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean online;

    /**
     * The locale of the user linked to this account
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    CountryLocale locale;

    /**
     * The name of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String name;

    /**
     * The name of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.STRING)
    String verifiedName;

    /**
     * The address of this account, if it's a business account
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String businessAddress;

    /**
     * The longitude of this account's location, if it's a business account
     */
    @ProtobufProperty(index = 11, type = ProtobufType.DOUBLE)
    Double businessLongitude;

    /**
     * The latitude of this account's location, if it's a business account
     */
    @ProtobufProperty(index = 12, type = ProtobufType.DOUBLE)
    Double businessLatitude;

    /**
     * The description of this account, if it's a business account
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String businessDescription;

    /**
     * The website of this account, if it's a business account
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String businessWebsite;

    /**
     * The email of this account, if it's a business account
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    String businessEmail;

    /**
     * The category of this account, if it's a business account
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    BusinessCategory businessCategory;

    /**
     * The hash of the companion associated with this session
     */
    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    String deviceHash;

    /**
     * A map of all the devices that the companion has associated using WhatsappWeb
     * The key here is the index of the device's key
     * The value is the device's companion jid
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.INT32)
    LinkedHashMap<Jid, Integer> linkedDevicesKeys;

    /**
     * The profile picture of the user linked to this account. This field will be null while the user
     * hasn't logged in yet. This field can also be null if no image was set.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    URI profilePicture;

    /**
     * The status of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    String about;

    /**
     * The user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The lid user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.STRING)
    Jid lid;

    /**
     * The non-null map of properties received by whatsapp
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    final ConcurrentHashMap<String, String> properties;

    /**
     * The non-null map of chats
     */
    final ConcurrentHashMap<Jid, Chat> chats;

    /**
     * The non-null map of contacts
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<Jid, Contact> contacts;

    /**
     * The non-null list of status messages
     */
    @ProtobufProperty(index = 25, type = ProtobufType.MESSAGE)
    final KeySetView<ChatMessageInfo, Boolean> status;

    /**
     * The non-null map of newsletters
     */
    final ConcurrentHashMap<Jid, Newsletter> newsletters;

    /**
     * The non-null map of privacy settings
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, PrivacySettingEntry> privacySettings;

    /**
     * The non-null map of calls
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<String, Call> calls;

    /**
     * Whether chats should be unarchived if a new message arrives
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    boolean unarchiveChats;

    /**
     * Whether the twenty-hours format is being used by the client
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean twentyFourHourFormat;

    /**
     * The non-null list of listeners
     */
    final KeySetView<WhatsappListener, Boolean> listeners;

    /**
     * The timestampSeconds in seconds for the initialization of this object
     */
    @ProtobufProperty(index = 30, type = ProtobufType.UINT64)
    final Long initializationTimeStamp;

    /**
     * The media connection associated with this store
     */
    MediaConnection mediaConnection;

    /**
     * The media connection latch associated with this store
     */
    final CountDownLatch mediaConnectionLatch;

    /**
     * The request tag, used to create messages
     */
    @ProtobufProperty(index = 31, type = ProtobufType.ENUM)
    ChatEphemeralTimer newChatsEphemeralTimer;

    /**
     * The setting to use when generating previews for text messages that contain links
     */
    @ProtobufProperty(index = 32, type = ProtobufType.ENUM)
    WhatsappTextPreviewPolicy whatsappTextPreviewPolicy;

    /**
     * Describes how much chat history Whatsapp should send
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    WhatsappWebHistoryPolicy historyLength;

    /**
     * Whether updates about the presence of the session should be sent automatically to Whatsapp
     * For example, when the bot is started, the status of the companion is changed to available if this option is enabled
     * If this option is enabled, the companion will not receive notifications because the bot will instantly read them
     */
    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    boolean automaticPresenceUpdates;

    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean automaticMessageReceipts;

    /**
     * The release channel to use when connecting to Whatsapp
     * This should allow the use of beta features
     */
    @ProtobufProperty(index = 37, type = ProtobufType.ENUM)
    ReleaseChannel releaseChannel;

    /**
     * Metadata about the device that is being simulated for Whatsapp
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    CompanionDevice device;

    /**
     * Whether the mac of every app state request should be checked
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    boolean checkPatchMacs;

    Store(UUID uuid, PhoneNumber phoneNumber, WhatsappClientType clientType, Collection<String> alias, URI proxy, boolean online, CountryLocale locale, String name, String verifiedName, String businessAddress, Double businessLongitude, Double businessLatitude, String businessDescription, String businessWebsite, String businessEmail, BusinessCategory businessCategory, String deviceHash, LinkedHashMap<Jid, Integer> linkedDevicesKeys, URI profilePicture, String about, Jid jid, Jid lid, ConcurrentHashMap<String, String> properties, ConcurrentHashMap<Jid, Contact> contacts, KeySetView<ChatMessageInfo, Boolean> status, ConcurrentHashMap<String, PrivacySettingEntry> privacySettings, ConcurrentHashMap<String, Call> calls, boolean unarchiveChats, boolean twentyFourHourFormat, Long initializationTimeStamp, ChatEphemeralTimer newChatsEphemeralTimer, WhatsappTextPreviewPolicy whatsappTextPreviewPolicy, WhatsappWebHistoryPolicy historyLength, boolean automaticPresenceUpdates, boolean automaticMessageReceipts, ReleaseChannel releaseChannel, CompanionDevice device, boolean checkPatchMacs) {
        super(uuid, phoneNumber, null, clientType, alias);
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
        this.whatsappTextPreviewPolicy = Objects.requireNonNullElse(whatsappTextPreviewPolicy, WhatsappTextPreviewPolicy.ENABLED_WITH_INFERENCE);
        this.historyLength = Objects.requireNonNullElseGet(historyLength, () -> WhatsappWebHistoryPolicy.standard(true));
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        this.automaticMessageReceipts = automaticMessageReceipts;
        this.releaseChannel = Objects.requireNonNullElse(releaseChannel, ReleaseChannel.RELEASE);
        this.device = device;
        this.checkPatchMacs = checkPatchMacs;
    }

    public static Store of(UUID uuid, PhoneNumber phoneNumber, Collection<String> alias, WhatsappClientType clientType) {
        return new StoreBuilder()
                .uuid(uuid)
                .initializationTimeStamp(Clock.nowSeconds())
                .phoneNumber(phoneNumber)
                .device(clientType == WhatsappClientType.MOBILE ? CompanionDevice.ios(false) : CompanionDevice.web())
                .clientType(clientType)
                .alias(alias)
                .jid(phoneNumber != null ? phoneNumber.toJid() : null)
                .automaticPresenceUpdates(true)
                .automaticMessageReceipts(clientType == WhatsappClientType.MOBILE)
                .build();
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByJid(JidProvider jid) {
        return switch (jid) {
            case Contact contact -> Optional.of(contact);
            case null -> Optional.empty();
            default -> Optional.ofNullable(contacts.get(jid.toJid().withoutData()));
        };
    }

    /**
     * Queries the first contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByName(String name) {
        return findContactsStream(name).findAny();
    }

    private Stream<Contact> findContactsStream(String name) {
        return name == null ? Stream.empty() : contacts().parallelStream()
                .filter(contact -> contact.fullName().filter(name::equals).isPresent() || contact.chosenName().filter(name::equals).isPresent() || contact.shortName().filter(name::equals).isPresent());
    }

    /**
     * Returns all the contacts
     *
     * @return an immutable collection
     */
    public Collection<Contact> contacts() {
        return Collections.unmodifiableCollection(contacts.values());
    }

    /**
     * Checks if a contact is in memory
     *
     * @param jidProvider the non-null jid
     * @return a boolean
     */
    public boolean hasContact(JidProvider jidProvider) {
        return jidProvider != null && contacts.get(jidProvider.toJid()) != null;
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Contact> findContactsByName(String name) {
        return findContactsStream(name).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first message whose id matches the one provided in the specified chat
     *
     * @param key the key to search
     * @return a non-null optional
     */
    public Optional<ChatMessageInfo> findMessageByKey(ChatMessageKey key) {
        return Optional.ofNullable(key)
                .map(ChatMessageKey::chatJid)
                .flatMap(this::findChatByJid)
                .flatMap(chat -> findMessageById(chat, key.id()));
    }

    /**
     * Queries the first message whose id matches the one provided in the specified chat or newsletter
     *
     * @param provider the chat to search in
     * @param id       the jid to search
     * @return a non-null optional
     */
    public Optional<? extends MessageStatusInfo> findMessageById(JidProvider provider, String id) {
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
     * Queries the first message whose id matches the one provided in the specified newsletter
     *
     * @param newsletter newsletter chat to search in
     * @param id         the jid to search
     * @return a non-null optional
     */
    public Optional<NewsletterMessageInfo> findMessageById(Newsletter newsletter, String id) {
        return newsletter.messages()
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
        return chat.messages()
                .parallelStream()
                .map(HistorySyncMessage::messageInfo)
                .filter(message -> Objects.equals(message.key().id(), id))
                .findAny();
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
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
     * Queries the first newsletter whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
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
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null optional
     */
    public Optional<Chat> findChatByName(String name) {
        return findChatsByNameStream(name).findAny();
    }

    /**
     * Queries the first newsletter whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null optional
     */
    public Optional<Newsletter> findNewsletterByName(String name) {
        return findNewslettersByNameStream(name).findAny();
    }


    private Stream<Chat> findChatsByNameStream(String name) {
        return name == null ? Stream.empty() : chats.values()
                .parallelStream()
                .filter(chat -> chat.name().equalsIgnoreCase(name));
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
     * Queries the first chat that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Optional<Chat> findChatBy(Function<Chat, Boolean> function) {
        return chats.values().parallelStream()
                .filter(function::apply)
                .findFirst();
    }

    /**
     * Queries the first newsletter that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Optional<Newsletter> findNewsletterBy(Function<Newsletter, Boolean> function) {
        return newsletters.values()
                .parallelStream()
                .filter(function::apply)
                .findFirst();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Chat> findChatsByName(String name) {
        return findChatsByNameStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries every newsletter whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Newsletter> findNewslettersByName(String name) {
        return findNewslettersByNameStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first chat that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Set<Chat> findChatsBy(Function<Chat, Boolean> function) {
        return chats.values()
                .stream()
                .filter(function::apply)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns all the status
     *
     * @return an immutable collection
     */
    public Collection<ChatMessageInfo> status() {
        return Collections.unmodifiableCollection(status);
    }

    /**
     * Returns all the newsletters
     *
     * @return an immutable collection
     */
    public Collection<Newsletter> newsletters() {
        return Collections.unmodifiableCollection(newsletters.values());
    }

    /**
     * Queries all the status of a contact
     *
     * @param jid the sender of the status
     * @return a non-null immutable list
     */
    public Collection<ChatMessageInfo> findStatusBySender(JidProvider jid) {
        return status.stream()
                .filter(entry -> Objects.equals(entry.chatJid(), jid))
                .toList();
    }

    /**
     * Adds a chat in memory
     *
     * @param chatJid the chat to add
     * @return the input chat
     */
    public Chat addNewChat(Jid chatJid) {
        var chat = new ChatBuilder()
                .jid(chatJid)
                .build();
        addChat(chat);
        return chat;
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the old chat, if present
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
     * Adds a contact in memory
     *
     * @param jid the contact to add
     * @return the input contact
     */
    public Contact addContact(Jid jid) {
        var newContact = new ContactBuilder()
                .jid(jid)
                .build();
        return addContact(newContact);
    }

    /**
     * Adds a contact in memory
     *
     * @param contact the contact to add
     * @return the input contact
     */
    public Contact addContact(Contact contact) {
        contacts.put(contact.jid(), contact);
        return contact;
    }

    /**
     * Adds a newsletter in memory
     *
     * @param newsletter the newsletter to add
     * @return the old newsletter, if present
     */
    public Optional<Newsletter> addNewsletter(Newsletter newsletter) {
        return Optional.ofNullable(newsletters.put(newsletter.jid(), newsletter));
    }

    /**
     * Removes a chat from memory
     *
     * @param chatJid the chat to remove
     * @return the chat that was deleted wrapped by an optional
     */
    public Optional<Chat> removeChat(JidProvider chatJid) {
        return Optional.ofNullable(chats.remove(chatJid.toJid()));
    }

    /**
     * Removes a newsletter from memory
     *
     * @param newsletterJid the newsletter to remove
     * @return the newsletter that was deleted wrapped by an optional
     */
    public Optional<Newsletter> removeNewsletter(JidProvider newsletterJid) {
        return Optional.ofNullable(newsletters.remove(newsletterJid.toJid()));
    }

    /**
     * Removes a contact from memory
     *
     * @param contactJid the contact to remove
     * @return the contact that was deleted wrapped by an optional
     */
    public Optional<Contact> removeContact(JidProvider contactJid) {
        return Optional.ofNullable(contacts.remove(contactJid.toJid()));
    }

    /**
     * Returns the chats pinned to the top sorted new to old
     *
     * @return a non-null list of chats
     */
    public List<Chat> pinnedChats() {
        return chats.values()
                .parallelStream()
                .filter(Chat::isPinned)
                .sorted(Comparator.comparingLong(Chat::pinnedTimestampSeconds).reversed())
                .toList();
    }

    /**
     * Returns all the starred messages
     *
     * @return a non-null list of messages
     */
    public List<ChatMessageInfo> starredMessages() {
        return chats().parallelStream().map(Chat::starredMessages).flatMap(Collection::stream).toList();
    }

    /**
     * Returns all the chats sorted from newest to oldest
     *
     * @return an immutable collection
     */
    public List<Chat> chats() {
        return chats.values()
                .stream()
                .sorted(Comparator.comparingLong(Chat::timestampSeconds).reversed())
                .toList();
    }

    /**
     * Returns the non-null map of properties received by whatsapp
     *
     * @return an unmodifiable map
     */
    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    public void addProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    /**
     * The media connection associated with this store
     *
     * @return the media connection
     */
    public MediaConnection mediaConnection() {
        return mediaConnection(Duration.ofMinutes(1));
    }

    /**
     * The media connection associated with this store
     *
     * @param timeout the non-null timeout for the connection to be filled
     * @return the media connection
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
     * Writes a media connection
     *
     * @param mediaConnection a media connection
     */
    public void setMediaConnection(MediaConnection mediaConnection) {
        this.mediaConnection = mediaConnection;
        mediaConnectionLatch.countDown();
    }

    public boolean hasMediaConnection() {
        return mediaConnection != null;
    }

    /**
     * Returns all the blocked contacts
     *
     * @return an immutable collection
     */
    public Collection<Contact> blockedContacts() {
        return contacts().stream().filter(Contact::blocked).toList();
    }

    /**
     * Adds a status to this store
     *
     * @param info the non-null status to add
     */
    public void addStatus(ChatMessageInfo info) {
        status.add(info);
    }

    /**
     * Returns the profile picture of this user if present
     *
     * @return an optional uri
     */
    public Optional<URI> profilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    /**
     * Queries all the privacy settings
     *
     * @return a non-null list
     */
    public Collection<PrivacySettingEntry> privacySettings() {
        return privacySettings.values();
    }

    /**
     * Queries the privacy setting entry for the type
     *
     * @param type a non-null type
     * @return a non-null entry
     */
    public PrivacySettingEntry findPrivacySetting(PrivacySettingType type) {
        return privacySettings.get(type.name());
    }

    /**
     * Sets the privacy setting entry for a type
     *
     * @param type  a non-null type
     * @param entry the non-null entry
     * @return the old privacy setting entry
     */
    public PrivacySettingEntry addPrivacySetting(PrivacySettingType type, PrivacySettingEntry entry) {
        return privacySettings.put(type.name(), entry);
    }

    /**
     * Returns an unmodifiable map that contains every companion associated using Whatsapp web mapped to its key index
     *
     * @return an unmodifiable map
     */
    public Map<Jid, Integer> linkedDevicesKeys() {
        return Collections.unmodifiableMap(linkedDevicesKeys);
    }


    /**
     * Returns an unmodifiable list that contains the devices associated using Whatsapp web to this session's companion
     *
     * @return an unmodifiable list
     */
    public Collection<Jid> linkedDevices() {
        return Collections.unmodifiableCollection(linkedDevicesKeys.keySet());
    }

    /**
     * Registers a new companion
     * Only use this method in the mobile api
     *
     * @param companion a non-null companion
     * @param keyId     the id of its key
     * @return the nullable old key
     */
    public Optional<Integer> addLinkedDevice(Jid companion, int keyId) {
        return Optional.ofNullable(linkedDevicesKeys.put(companion, keyId));
    }

    /**
     * Removes a companion
     * Only use this method in the mobile api
     *
     * @param companion a non-null companion
     * @return the nullable old key
     */
    public Optional<Integer> removeLinkedCompanion(Jid companion) {
        return Optional.ofNullable(linkedDevicesKeys.remove(companion));
    }

    /**
     * Removes all linked companion
     */
    public void removeLinkedCompanions() {
        linkedDevicesKeys.clear();
    }

    /**
     * Returns an immutable collection of listeners
     *
     * @return a non-null collection
     */
    public Collection<WhatsappListener> listeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     */
    public void addListener(WhatsappListener listener) {
        listeners.add(listener);
    }

    /**
     * Registers a collection of listeners
     *
     * @param listeners the listeners to register
     */
    public void addListeners(Collection<WhatsappListener> listeners) {
        this.listeners.addAll(listeners);
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove
     * @return whether the listener was removed
     */
    public boolean removeListener(WhatsappListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Removes all listeners
     */
    public void removeListeners() {
        listeners.clear();
    }

    /**
     * Sets the proxy used by this session
     */
    public void setProxy(URI proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns the proxy used by this session
     *
     * @return a non-null optional
     */
    public Optional<URI> proxy() {
        return Optional.ofNullable(proxy);
    }

    /**
     * The address of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessAddress() {
        return Optional.ofNullable(businessAddress);
    }

    /**
     * The longitude of this account's location, if it's a business account
     *
     * @return an optional
     */
    public Optional<Double> businessLongitude() {
        return Optional.ofNullable(businessLongitude);
    }

    /**
     * The latitude of this account's location, if it's a business account
     *
     * @return an optional
     */
    public Optional<Double> businessLatitude() {
        return Optional.ofNullable(businessLatitude);
    }

    /**
     * The description of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessDescription() {
        return Optional.ofNullable(businessDescription);
    }

    /**
     * The website of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessWebsite() {
        return Optional.ofNullable(businessWebsite);
    }

    /**
     * The email of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessEmail() {
        return Optional.ofNullable(businessEmail);
    }

    /**
     * The category of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<BusinessCategory> businessCategory() {
        return Optional.ofNullable(businessCategory);
    }

    public void dispose() {
        serialize();
        mediaConnectionLatch.countDown();
    }

    @Override
    public void serialize() {
        serializer.serializeStore(this);
    }

    /**
     * Adds a call to the store
     *
     * @param call a non-null call
     * @return the old value associated with {@link Call#id()}
     */
    public Optional<Call> addCall(Call call) {
        return Optional.ofNullable(calls.put(call.id(), call));
    }

    /**
     * Finds a call by id
     *
     * @param callId the id of the call, can be null
     * @return an optional
     */
    public Optional<Call> findCallById(String callId) {
        return callId == null ? Optional.empty() : Optional.ofNullable(calls.get(callId));
    }

    /**
     * Returns all the calls registered
     *
     * @return an unmodifiable collection
     */
    public Collection<Call> calls() {
        return Collections.unmodifiableCollection(calls.values());
    }

    public Version version() {
        return Objects.requireNonNullElseGet(version, () -> version = AppMetadata.getVersion(device.platform(), proxy));
    }

    public boolean online() {
        return this.online;
    }

    public Optional<CountryLocale> locale() {
        return Optional.ofNullable(this.locale);
    }

    public String name() {
        if(name == null) {
            return device.platform().platformName();
        }else {
            return name;
        }
    }

    public Optional<String> deviceHash() {
        return Optional.ofNullable(this.deviceHash);
    }

    public Optional<String> about() {
        return Optional.ofNullable(this.about);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(this.jid);
    }

    public Optional<Jid> lid() {
        return Optional.ofNullable(this.lid);
    }

    public boolean unarchiveChats() {
        return this.unarchiveChats;
    }

    public boolean twentyFourHourFormat() {
        return this.twentyFourHourFormat;
    }

    public long initializationTimeStamp() {
        return this.initializationTimeStamp;
    }

    public ChatEphemeralTimer newChatsEphemeralTimer() {
        return this.newChatsEphemeralTimer;
    }

    public WhatsappTextPreviewPolicy textPreviewSetting() {
        return this.whatsappTextPreviewPolicy;
    }

    public WhatsappWebHistoryPolicy webHistorySetting() {
        return this.historyLength;
    }

    public boolean automaticPresenceUpdates() {
        return this.automaticPresenceUpdates;
    }

    public ReleaseChannel releaseChannel() {
        return this.releaseChannel;
    }

    public CompanionDevice device() {
        return device;
    }

    public boolean checkPatchMacs() {
        return this.checkPatchMacs;
    }

    public boolean automaticMessageReceipts() {
        return automaticPresenceUpdates;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setLocale(CountryLocale locale) {
        this.locale = locale;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public void setBusinessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
    }

    public void setBusinessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    public void setBusinessWebsite(String businessWebsite) {
        this.businessWebsite = businessWebsite;
    }

    public void setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
    }

    public void setBusinessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
    }

    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    public void setLinkedDevicesKeys(LinkedHashMap<Jid, Integer> linkedDevicesKeys) {
        this.linkedDevicesKeys = linkedDevicesKeys;
    }

    public void setProfilePicture(URI profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setJid(Jid jid) {
        this.jid = jid;
    }

    public void setLid(Jid lid) {
        this.lid = lid;
    }

    public void setUnarchiveChats(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
    }

    public void setTwentyFourHourFormat(boolean twentyFourHourFormat) {
        this.twentyFourHourFormat = twentyFourHourFormat;
    }

    public void setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer) {
        this.newChatsEphemeralTimer = newChatsEphemeralTimer;
    }

    public void setTextPreviewSetting(WhatsappTextPreviewPolicy whatsappTextPreviewPolicy) {
        this.whatsappTextPreviewPolicy = whatsappTextPreviewPolicy;
    }

    public void setWebHistorySetting(WhatsappWebHistoryPolicy whatsappWebHistoryPolicy) {
        this.historyLength = whatsappWebHistoryPolicy;
    }

    public void setAutomaticPresenceUpdates(boolean automaticPresenceUpdates) {
        this.automaticPresenceUpdates = automaticPresenceUpdates;
    }

    public void setReleaseChannel(ReleaseChannel releaseChannel) {
        this.releaseChannel = releaseChannel;
    }

    public void setDevice(CompanionDevice device) {
        if(!Objects.equals(device(), device)) {
            this.device = Objects.requireNonNull(device, "The device cannot be null");
            this.version = null;
        }
    }

    public void setCheckPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
    }

    public Optional<String> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }

    public void setVerifiedName(String verifiedName) {
        this.verifiedName = verifiedName;
    }

    public void setAutomaticMessageReceipts(boolean automaticMessageReceipts) {
        this.automaticMessageReceipts = automaticMessageReceipts;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Store store &&
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
                whatsappTextPreviewPolicy == store.whatsappTextPreviewPolicy &&
                Objects.equals(historyLength, store.historyLength) &&
                releaseChannel == store.releaseChannel &&
                Objects.equals(device, store.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proxy, version, online, locale, name, verifiedName, businessAddress, businessLongitude, businessLatitude, businessDescription, businessWebsite, businessEmail, businessCategory, deviceHash, linkedDevicesKeys, profilePicture, about, jid, lid, properties, contacts, status, privacySettings, calls, unarchiveChats, twentyFourHourFormat, initializationTimeStamp, newChatsEphemeralTimer, whatsappTextPreviewPolicy, historyLength, automaticPresenceUpdates, automaticMessageReceipts, releaseChannel, device, checkPatchMacs);
    }
}
