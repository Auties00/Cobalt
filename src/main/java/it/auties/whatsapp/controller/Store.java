package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.TextPreviewSetting;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatBuilder;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.DeviceContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.standard.PollCreationMessage;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.poll.PollUpdate;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedOptionsSpec;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.signal.auth.UserAgent.ReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This controller holds the user-related data regarding a WhatsappWeb session
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Store extends Controller<Store> {
    /**
     * The version used by this session
     */
    @Nullable
    private URI proxy;

    /**
     * The version used by this session
     */
    @NonNull
    private FutureReference<Version> version;

    /**
     * Whether this account is online for other users
     */
    private boolean online;

    /**
     * The locale of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Nullable
    private String locale;

    /**
     * The name of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @NonNull
    private String name;

    /**
     * Whether the linked companion is a business account or not
     */
    private boolean business;

    /**
     * The address of this account, if it's a business account
     */
    @Nullable
    private String businessAddress;

    /**
     * The longitude of this account's location, if it's a business account
     */
    @Nullable
    private Double businessLongitude;

    /**
     * The latitude of this account's location, if it's a business account
     */
    @Nullable
    private Double businessLatitude;

    /**
     * The description of this account, if it's a business account
     */
    @Nullable
    private String businessDescription;

    /**
     * The website of this account, if it's a business account
     */
    @Nullable
    private String businessWebsite;

    /**
     * The email of this account, if it's a business account
     */
    @Nullable
    private String businessEmail;

    /**
     * The category of this account, if it's a business account
     */
    @Nullable
    private BusinessCategory businessCategory;

    /**
     * The hash of the companion associated with this session
     */
    @Nullable
    private String deviceHash;

    /**
     * A map of all the devices that the companion has associated using WhatsappWeb
     * The key here is the index of the device's key
     * The value is the device's companion jid
     */
    @NonNull
    private LinkedHashMap<Jid, Integer> linkedDevicesKeys;

    /**
     * The profile picture of the user linked to this account. This field will be null while the user
     * hasn't logged in yet. This field can also be null if no image was set.
     */
    @Nullable
    private URI profilePicture;

    /**
     * The status of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @Nullable
    private String about;

    /**
     * The user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Nullable
    private Jid jid;

    /**
     * The lid user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Nullable
    private Jid lid;

    /**
     * The non-null map of properties received by whatsapp
     */
    @NonNull
    private final ConcurrentHashMap<String, String> properties;

    /**
     * The non-null map of chats
     */
    @NonNull
    @JsonIgnore
    private final ConcurrentHashMap<Jid, Chat> chats;

    /**
     * The non-null map of contacts
     */
    @NonNull
    private final ConcurrentHashMap<Jid, Contact> contacts;

    /**
     * The non-null list of status messages
     */
    @NonNull
    private final ConcurrentHashMap<Jid, ConcurrentLinkedDeque<MessageInfo>> status;

    /**
     * The non-null map of privacy settings
     */
    @NonNull
    private final ConcurrentHashMap<PrivacySettingType, PrivacySettingEntry> privacySettings;

    /**
     * The non-null map of calls
     */
    @NonNull
    private final ConcurrentHashMap<String, Call> calls;

    /**
     * Whether chats should be unarchived if a new message arrives
     */
    private boolean unarchiveChats;

    /**
     * Whether the twenty-hours format is being used by the client
     */
    private boolean twentyFourHourFormat;

    /**
     * The non-null list of requests that were sent to Whatsapp. They might or might not be waiting
     * for a response
     */
    @NonNull
    @JsonIgnore
    private final ConcurrentHashMap<String, Request> requests;

    /**
     * The non-null list of replies waiting to be fulfilled
     */
    @NonNull
    @JsonIgnore
    private final ConcurrentHashMap<String, CompletableFuture<MessageInfo>> replyHandlers;

    /**
     * The non-null list of listeners
     */
    @NonNull
    @JsonIgnore
    private final KeySetView<Listener, Boolean> listeners;

    /**
     * The request tag, used to create messages
     */
    @NonNull
    @JsonIgnore
    private final String tag;

    /**
     * The timestampSeconds in seconds for the initialization of this object
     */
    private final long initializationTimeStamp;

    /**
     * The media connection associated with this store
     */
    @JsonIgnore
    @Nullable
    private MediaConnection mediaConnection;

    /**
     * The media connection latch associated with this store
     */
    @JsonIgnore
    @NonNull
    private final CountDownLatch mediaConnectionLatch;

    /**
     * The request tag, used to create messages
     */
    @NonNull
    private ChatEphemeralTimer newChatsEphemeralTimer;

    /**
     * The setting to use when generating previews for text messages that contain links
     */
    @NonNull
    private TextPreviewSetting textPreviewSetting;

    /**
     * Describes how much chat history Whatsapp should send
     */
    @NonNull
    private WebHistoryLength historyLength;

    /**
     * Whether listeners should be automatically scanned and registered or not
     */
    private boolean autodetectListeners;


    /**
     * Whether updates about the presence of the session should be sent automatically to Whatsapp
     * For example, when the bot is started, the status of the companion is changed to available if this option is enabled
     */
    private boolean automaticPresenceUpdates;

    /**
     * The release channel to use when connecting to Whatsapp
     * This should allow the use of beta features
     */
    @NonNull
    private ReleaseChannel releaseChannel;

    /**
     * Metadata about the device that is being simulated for Whatsapp
     */
    @Nullable
    private CompanionDevice device;

    /**
     * The os of the associated device, available only for the web api
     */
    @Nullable
    private PlatformType companionDeviceOs;

    /**
     * Whether the mac of every app state request should be checked
     */
    private boolean checkPatchMacs;

    /**
     * All args constructor
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    Store(@NonNull UUID uuid, PhoneNumber phoneNumber, @NonNull ControllerSerializer serializer, @NonNull ClientType clientType, @Nullable List<String> alias, @Nullable URI proxy, @NonNull FutureReference<Version> version, boolean online, @Nullable String locale, @NonNull String name, boolean business, @Nullable String businessAddress, @Nullable Double businessLongitude, @Nullable Double businessLatitude, @Nullable String businessDescription, @Nullable String businessWebsite, @Nullable String businessEmail, @Nullable BusinessCategory businessCategory, @Nullable String deviceHash, @NonNull LinkedHashMap<Jid, Integer> linkedDevicesKeys, @Nullable URI profilePicture, @Nullable String about, @Nullable Jid jid, @Nullable Jid lid, @NonNull ConcurrentHashMap<String, String> properties, @NonNull ConcurrentHashMap<Jid, Contact> contacts, @NonNull ConcurrentHashMap<Jid, ConcurrentLinkedDeque<MessageInfo>> status, @NonNull ConcurrentHashMap<PrivacySettingType, PrivacySettingEntry> privacySettings, @NonNull ConcurrentHashMap<String, Call> calls, boolean unarchiveChats, boolean twentyFourHourFormat, long initializationTimeStamp, @NonNull ChatEphemeralTimer newChatsEphemeralTimer, @NonNull TextPreviewSetting textPreviewSetting, @NonNull WebHistoryLength historyLength, boolean autodetectListeners, boolean automaticPresenceUpdates, @NonNull ReleaseChannel releaseChannel, @Nullable CompanionDevice device, @Nullable PlatformType companionDeviceOs, boolean checkPatchMacs) {
        super(uuid, phoneNumber, serializer, clientType, alias);
        if(proxy != null) {
            ProxyAuthenticator.register(proxy);
        }

        this.proxy = proxy;
        this.version = version;
        this.online = online;
        this.locale = locale;
        this.name = name;
        this.business = business;
        this.businessAddress = businessAddress;
        this.businessLongitude = businessLongitude;
        this.businessLatitude = businessLatitude;
        this.businessDescription = businessDescription;
        this.businessWebsite = businessWebsite;
        this.businessEmail = businessEmail;
        this.businessCategory = businessCategory;
        this.deviceHash = deviceHash;
        this.linkedDevicesKeys = linkedDevicesKeys;
        this.profilePicture = profilePicture;
        this.about = about;
        this.jid = jid;
        this.lid = lid;
        this.properties = properties;
        this.chats = new ConcurrentHashMap<>();
        this.contacts = contacts;
        this.status = status;
        this.privacySettings = privacySettings;
        this.calls = calls;
        this.unarchiveChats = unarchiveChats;
        this.twentyFourHourFormat = twentyFourHourFormat;
        this.requests = new ConcurrentHashMap<>();
        this.replyHandlers = new ConcurrentHashMap<>();
        this.listeners = ConcurrentHashMap.newKeySet();
        this.tag = HexFormat.of().formatHex(BytesHelper.random(1));
        this.initializationTimeStamp = initializationTimeStamp;
        this.mediaConnectionLatch = new CountDownLatch(1);
        this.newChatsEphemeralTimer = newChatsEphemeralTimer;
        this.textPreviewSetting = textPreviewSetting;
        this.historyLength = historyLength;
        this.autodetectListeners = autodetectListeners;
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        this.releaseChannel = releaseChannel;
        this.device = device;
        this.companionDeviceOs = companionDeviceOs;
        this.checkPatchMacs = checkPatchMacs;
    }

    /**
     * Creates a builder
     *
     * @return a builder
     */
    public static StoreBuilder builder() {
        return new StoreBuilder();
    }


    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByJid(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }

        if (jid instanceof Contact contact) {
            return Optional.of(contact);
        }

        return Optional.ofNullable(contacts.get(jid.toJid()));
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
    public Optional<MessageInfo> findMessageByKey(MessageKey key) {
        return key == null ? Optional.empty() : findMessageById(key.chatJid(), key.id());
    }

    /**
     * Queries the first message whose id matches the one provided in the specified chat
     *
     * @param provider the chat to search in
     * @param id       the jid to search
     * @return a non-null optional
     */
    public Optional<MessageInfo> findMessageById(JidProvider provider, String id) {
        if (provider == null || id == null) {
            return Optional.empty();
        }

        var chat = findChatByJid(provider.toJid())
                .orElse(null);
        if (chat == null) {
            return Optional.empty();
        }

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
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null optional
     */
    public Optional<Chat> findChatByName(String name) {
        return findChatsStream(name).findAny();
    }

    private Stream<Chat> findChatsStream(String name) {
        return name == null ? Stream.empty() : chats.values()
                .parallelStream()
                .filter(chat -> chat.name().equalsIgnoreCase(name));
    }

    /**
     * Queries the first chat that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Optional<Chat> findChatBy(@NonNull Function<Chat, Boolean> function) {
        return chats.values().parallelStream()
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
        return findChatsStream(name).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first chat that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Set<Chat> findChatsBy(@NonNull Function<Chat, Boolean> function) {
        return chats.values()
                .stream()
                .filter(function::apply)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first status whose id matches the one provided
     *
     * @param id the id of the status
     * @return a non-null optional
     */
    public Optional<MessageInfo> findStatusById(String id) {
        return id == null ? Optional.empty() : status().stream()
                .filter(status -> Objects.equals(status.id(), id))
                .findFirst();
    }

    /**
     * Returns all the status
     *
     * @return an immutable collection
     */
    public Collection<MessageInfo> status() {
        return status.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries all the status of a contact
     *
     * @param jid the sender of the status
     * @return a non-null immutable list
     */
    public Collection<MessageInfo> findStatusBySender(JidProvider jid) {
        return Optional.ofNullable(status.get(jid.toJid()))
                .map(Collections::unmodifiableCollection)
                .orElseGet(Set::of);
    }

    /**
     * Queries the first request whose id equals the one stored by the response and, if any is found,
     * it completes it
     *
     * @param response      the response to complete the request with
     * @param exceptionally whether the response is erroneous
     * @return a boolean
     */
    public boolean resolvePendingRequest(@NonNull Node response, boolean exceptionally) {
        return findPendingRequest(response.id()).map(request -> deleteAndComplete(request, response, exceptionally))
                .isPresent();
    }

    /**
     * Queries the first request whose id is equal to {@code id}
     *
     * @param id the id to search, can be null
     * @return a non-null optional
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public Optional<Request> findPendingRequest(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(requests.get(id));
    }

    private Request deleteAndComplete(Request request, Node response, boolean exceptionally) {
        if (request.complete(response, exceptionally)) {
            requests.remove(request.id());
        }

        return request;
    }

    /**
     * Clears all the data that this object holds and closes the pending requests
     */
    public void resolveAllPendingRequests() {
        requests.values().forEach(request -> request.complete(null, false));
    }

    /**
     * Returns an immutable collection of pending requests
     *
     * @return a non-null collection
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public Collection<Request> pendingRequests() {
        return Collections.unmodifiableCollection(requests.values());
    }

    /**
     * Queries the first reply waiting and completes it with the input message
     *
     * @param response the response to complete the reply with
     * @return a boolean
     */
    public boolean resolvePendingReply(@NonNull MessageInfo response) {
        return response.message()
                .contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(ContextInfo::quotedMessageId)
                .map(id -> {
                    var future = replyHandlers.remove(id);
                    if (future == null) {
                        return false;
                    }

                    future.complete(response);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Adds a chat in memory
     *
     * @param chatJid the chat to add
     * @return the input chat
     */
    public Chat addNewChat(@NonNull Jid chatJid) {
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
    public Optional<Chat> addChat(@NonNull Chat chat) {
        chat.messages().forEach(this::attribute);
        if (chat.hasName() && chat.jid().hasServer(JidServer.WHATSAPP)) {
            var contact = findContactByJid(chat.jid())
                    .orElseGet(() -> addContact(new Contact(chat.jid())));
            contact.setFullName(chat.name());
        }
        var oldChat = chats.get(chat.jid());
        if (oldChat != null) {
            if (oldChat.hasName() && !chat.hasName()) {
                chat.setName(oldChat.name()); // Coming from contact actions
            }
            joinMessages(chat, oldChat);
        }
        return addChatDirect(chat);
    }

    private void joinMessages(Chat chat, Chat oldChat) {
        var newChatTimestamp = chat.newestMessage()
                .map(MessageInfo::timestampSeconds)
                .orElse(0L);
        var oldChatTimestamp = oldChat.newestMessage()
                .map(MessageInfo::timestampSeconds)
                .orElse(0L);
        if (newChatTimestamp <= oldChatTimestamp) {
            chat.addMessages(oldChat.messages());
            return;
        }
        chat.addOldMessages(chat.messages());
    }

    /**
     * Adds a chat in memory without executing any check
     *
     * @param chat the chat to add
     * @return the old chat, if present
     */
    public Optional<Chat> addChatDirect(Chat chat) {
        return Optional.ofNullable(chats.put(chat.jid(), chat));
    }

    /**
     * Removes a chat from memory
     *
     * @param chatJid the chat to remove
     * @return the chat that was deleted wrapped by an optional
     */
    public Optional<Chat> removeChat(@NonNull Jid chatJid) {
        return Optional.ofNullable(chats.remove(chatJid));
    }

    /**
     * Adds a contact in memory
     *
     * @param jid the contact to add
     * @return the input contact
     */
    public Contact addContact(@NonNull Jid jid) {
        return addContact(new Contact(jid));
    }

    /**
     * Adds a contact in memory
     *
     * @param contact the contact to add
     * @return the input contact
     */
    public Contact addContact(@NonNull Contact contact) {
        contacts.put(contact.jid(), contact);
        return contact;
    }

    /**
     * Attributes a message Usually used by the socket handler
     *
     * @param historySyncMessage a non-null message
     * @return the same incoming message
     */
    public MessageInfo attribute(@NonNull HistorySyncMessage historySyncMessage) {
        return attribute(historySyncMessage.messageInfo());
    }

    /**
     * Attributes a message Usually used by the socket handler
     *
     * @param info a non-null message
     * @return the same incoming message
     */
    public MessageInfo attribute(@NonNull MessageInfo info) {
        var chat = findChatByJid(info.chatJid())
                .orElseGet(() -> addNewChat(info.chatJid()));
        info.setChat(chat);
        if (info.fromMe() && jid != null) {
            info.key().setSenderJid(jid.withoutDevice());
        }
        info.key()
                .senderJid()
                .ifPresent(senderJid -> attributeSender(info, senderJid));
        info.message()
                .contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .ifPresent(this::attributeContext);
        processMessage(info);
        return info;
    }

    private MessageKey attributeSender(MessageInfo info, Jid senderJid) {
        var contact = findContactByJid(senderJid)
                .orElseGet(() -> addContact(new Contact(senderJid)));
        info.setSender(contact);
        return info.key();
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageChatJid().ifPresent(chatJid -> attributeContextChat(contextInfo, chatJid));
    }

    private void attributeContextChat(ContextInfo contextInfo, Jid chatJid) {
        var chat = findChatByJid(chatJid)
                .orElseGet(() -> addNewChat(chatJid));
        contextInfo.setQuotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        var contact = findContactByJid(senderJid)
                .orElseGet(() -> addContact(new Contact(senderJid)));
        contextInfo.setQuotedMessageSender(contact);
    }

    private void processMessage(MessageInfo info) {
        Message content = info.message().content();
        if (Objects.requireNonNull(content) instanceof PollCreationMessage pollCreationMessage) {
            handlePollCreation(info, pollCreationMessage);
        } else if (content instanceof PollUpdateMessage pollUpdateMessage) {
            handlePollUpdate(info, pollUpdateMessage);
        } else if (content instanceof ReactionMessage reactionMessage) {
            handleReactionMessage(info, reactionMessage);
        }
    }

    private void handlePollCreation(MessageInfo info, PollCreationMessage pollCreationMessage) {
        if (pollCreationMessage.encryptionKey().isPresent()) {
            return;
        }

        info.message()
                .deviceInfo()
                .flatMap(DeviceContextInfo::messageSecret)
                .or(info::messageSecret)
                .ifPresent(pollCreationMessage::setEncryptionKey);
    }

    private void handlePollUpdate(MessageInfo info, PollUpdateMessage pollUpdateMessage) {
        var originalPollInfo = findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
        var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
        pollUpdateMessage.setPollCreationMessage(originalPollMessage);
        var originalPollSender = originalPollInfo.senderJid()
                .withoutDevice()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        var modificationSenderJid = info.senderJid().withoutDevice();
        pollUpdateMessage.setVoter(modificationSenderJid);
        var modificationSender = modificationSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var secretName = pollUpdateMessage.secretName().getBytes(StandardCharsets.UTF_8);
        var useSecretPayload = BytesHelper.concat(
                originalPollInfo.id().getBytes(StandardCharsets.UTF_8),
                originalPollSender,
                modificationSender,
                secretName
        );
        var encryptionKey = originalPollMessage.encryptionKey()
                .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
        var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload, 32);
        var additionalData = "%s\0%s".formatted(
                originalPollInfo.id(),
                modificationSenderJid
        );
        var metadata = pollUpdateMessage.encryptedMetadata()
                .orElseThrow(() -> new NoSuchElementException("Missing encrypted metadata"));
        var decrypted = AesGcm.decrypt(metadata.iv(), metadata.payload(), useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollVoteMessage = PollUpdateEncryptedOptionsSpec.decode(decrypted);
        var selectedOptions = pollVoteMessage.selectedOptions()
                .stream()
                .map(sha256 -> originalPollMessage.getSelectableOption(HexFormat.of().formatHex(sha256)))
                .flatMap(Optional::stream)
                .toList();
        originalPollMessage.addSelectedOptions(modificationSenderJid, selectedOptions);
        pollUpdateMessage.setVotes(selectedOptions);
        var update = new PollUpdate(info.key(), pollVoteMessage, Clock.nowMilliseconds());
        info.pollUpdates().add(update);
    }

    private void handleReactionMessage(MessageInfo info, ReactionMessage reactionMessage) {
        info.setIgnore(true);
        findMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
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
    public List<MessageInfo> starredMessages() {
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
    public MediaConnection mediaConnection(@NonNull Duration timeout) {
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
     * @return the same instance
     */
    public Store setMediaConnection(MediaConnection mediaConnection) {
        this.mediaConnection = mediaConnection;
        mediaConnectionLatch.countDown();
        return this;
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
     * @return the same instance
     */
    public Store addStatus(@NonNull MessageInfo info) {
        attribute(info);
        var wrapper = Objects.requireNonNullElseGet(status.get(info.senderJid()), ConcurrentLinkedDeque<MessageInfo>::new);
        wrapper.add(info);
        status.put(info.senderJid(), wrapper);
        return this;
    }

    /**
     * Adds a request to this store
     *
     * @param request the non-null request to add
     * @return the non-null completable result of the request
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public CompletableFuture<Node> addRequest(@NonNull Request request) {
        if (request.id() == null) {
            return CompletableFuture.completedFuture(null);
        }

        requests.put(request.id(), request);
        return request.future();
    }

    /**
     * Adds a replay handler to this store
     *
     * @param messageId the non-null message id to listen for
     * @return the non-null completable result of the reply handler
     */
    public CompletableFuture<MessageInfo> addPendingReply(@NonNull String messageId) {
        var result = new CompletableFuture<MessageInfo>();
        replyHandlers.put(messageId, result);
        return result;
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
    public PrivacySettingEntry findPrivacySetting(@NonNull PrivacySettingType type) {
        return privacySettings.get(type);
    }

    /**
     * Sets the privacy setting entry for a type
     *
     * @param type  a non-null type
     * @param entry the non-null entry
     * @return the old privacy setting entry
     */
    public PrivacySettingEntry addPrivacySetting(@NonNull PrivacySettingType type, @NonNull PrivacySettingEntry entry) {
        return privacySettings.put(type, entry);
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
    public Optional<Integer> addLinkedDevice(@NonNull Jid companion, int keyId) {
        return Optional.ofNullable(linkedDevicesKeys.put(companion, keyId));
    }

    /**
     * Removes a companion
     * Only use this method in the mobile api
     *
     * @param companion a non-null companion
     * @return the nullable old key
     */
    public Optional<Integer> removeLinkedCompanion(@NonNull Jid companion) {
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
    public Collection<Listener> listeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Store addListener(@NonNull Listener listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Registers a collection of listeners
     *
     * @param listeners the listeners to register
     * @return the same instance
     */
    public Store addListeners(@NonNull Collection<Listener> listeners) {
        this.listeners.addAll(listeners);
        return this;
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove
     * @return the same instance
     */
    public Store removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
        return this;
    }

    /**
     * Removes all listeners
     *
     * @return the same instance
     */
    public Store removeListener() {
        listeners.clear();
        return this;
    }

    /**
     * Sets the proxy used by this session
     *
     * @return the same instance
     */
    public Store setProxy(URI proxy) {
        if (proxy != null && proxy.getUserInfo() != null) {
            ProxyAuthenticator.register(proxy);
        } else if (proxy == null && this.proxy != null && this.proxy.getUserInfo() != null) {
            ProxyAuthenticator.unregister(this.proxy);
        }

        this.proxy = proxy;
        return this;
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
     * The os of the associated device
     * Available only for the web api
     *
     * @return a non-null optional
     */
    public Optional<PlatformType> companionDeviceOs() {
        return Optional.ofNullable(companionDeviceOs);
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
        serialize(false);
        mediaConnectionLatch.countDown();
    }

    @Override
    public void serialize(boolean async) {
        serializer.serializeStore(this, async);
    }

    /**
     * Adds a call to the store
     *
     * @param call a non-null call
     * @return the old value associated with {@link Call#id()}
     */
    public Optional<Call> addCall(@NonNull Call call) {
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

    public String tag() {
        return tag;
    }

    @JsonGetter("version")
    public Version version() {
        return version.value();
    }

    public boolean online() {
        return this.online;
    }

    public Optional<String> locale() {
        return Optional.ofNullable(this.locale);
    }

    public String name() {
        return name;
    }

    public boolean business() {
        return this.business;
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

    public TextPreviewSetting textPreviewSetting() {
        return this.textPreviewSetting;
    }

    public WebHistoryLength historyLength() {
        return this.historyLength;
    }

    public boolean autodetectListeners() {
        return this.autodetectListeners;
    }

    public boolean automaticPresenceUpdates() {
        return this.automaticPresenceUpdates;
    }

    public ReleaseChannel releaseChannel() {
        return this.releaseChannel;
    }

    public Optional<CompanionDevice> device() {
        return Optional.ofNullable(device);
    }

    public boolean checkPatchMacs() {
        return this.checkPatchMacs;
    }

    public Store setOnline(boolean online) {
        this.online = online;
        return this;
    }

    public Store setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    public Store setName(String name) {
        this.name = name;
        return this;
    }

    public Store setBusiness(boolean business) {
        this.business = business;
        return this;
    }

    public Store setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
        return this;
    }

    public Store setBusinessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
        return this;
    }

    public Store setBusinessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
        return this;
    }

    public Store setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
        return this;
    }

    public Store setBusinessWebsite(String businessWebsite) {
        this.businessWebsite = businessWebsite;
        return this;
    }

    public Store setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
        return this;
    }

    public Store setBusinessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
        return this;
    }

    public Store setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
        return this;
    }

    public Store setLinkedDevicesKeys(LinkedHashMap<Jid, Integer> linkedDevicesKeys) {
        this.linkedDevicesKeys = linkedDevicesKeys;
        return this;
    }

    public Store setProfilePicture(URI profilePicture) {
        this.profilePicture = profilePicture;
        return this;
    }

    public Store setAbout(String about) {
        this.about = about;
        return this;
    }

    public Store setJid(Jid jid) {
        this.jid = jid;
        return this;
    }

    public Store setLid(Jid lid) {
        this.lid = lid;
        return this;
    }

    public Store setUnarchiveChats(boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
        return this;
    }

    public Store setTwentyFourHourFormat(boolean twentyFourHourFormat) {
        this.twentyFourHourFormat = twentyFourHourFormat;
        return this;
    }

    public Store setNewChatsEphemeralTimer(ChatEphemeralTimer newChatsEphemeralTimer) {
        this.newChatsEphemeralTimer = newChatsEphemeralTimer;
        return this;
    }

    public Store setTextPreviewSetting(TextPreviewSetting textPreviewSetting) {
        this.textPreviewSetting = textPreviewSetting;
        return this;
    }

    public Store setHistoryLength(WebHistoryLength historyLength) {
        this.historyLength = historyLength;
        return this;
    }

    public Store setAutodetectListeners(boolean autodetectListeners) {
        this.autodetectListeners = autodetectListeners;
        return this;
    }

    public Store setAutomaticPresenceUpdates(boolean automaticPresenceUpdates) {
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        return this;
    }

    public Store setReleaseChannel(ReleaseChannel releaseChannel) {
        this.releaseChannel = releaseChannel;
        return this;
    }

    public Store setDevice(CompanionDevice device) {
        this.device = device;
        this.version = new FutureReference<>(null, () -> MetadataHelper.getVersion(device.platform(), business));
        return this;
    }

    public Store setCompanionDeviceOs(PlatformType companionDeviceOs) {
        this.companionDeviceOs = companionDeviceOs;
        return this;
    }

    public Store setCheckPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
        return this;
    }

    public Store setVersion(@NonNull Version version) {
        this.version.setValue(version);
        return this;
    }
}
