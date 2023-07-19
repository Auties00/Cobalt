package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.whatsapp.api.*;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.standard.PollCreationMessage;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.poll.PollUpdate;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedOptions;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.exchange.Node;
import it.auties.whatsapp.model.exchange.ReplyHandler;
import it.auties.whatsapp.socket.Request;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.*;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

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
@SuperBuilder
@Jacksonized
@Accessors(fluent = true, chain = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Store extends Controller<Store> {
    /**
     * The version used by this session
     */
    private URI proxy;

    /**
     * The version used by this session
     */
    private Version version;

    /**
     * Whether this account is online for other users
     */
    @Getter
    @Setter
    @Default
    private boolean online = false;

    /**
     * The locale of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String locale;

    /**
     * The name of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    @Default
    private String name = "Whatsapp4j";

    /**
     * Whether the linked companion is a business account or not
     */
    @Getter
    @Setter
    private boolean business;

    /**
     * The address of this account, if it's a business account
     */
    @Setter
    private String businessAddress;

    /**
     * The longitude of this account's location, if it's a business account
     */
    @Setter
    private Double businessLongitude;

    /**
     * The latitude of this account's location, if it's a business account
     */
    @Setter
    private Double businessLatitude;

    /**
     * The description of this account, if it's a business account
     */
    @Setter
    private String businessDescription;

    /**
     * The website of this account, if it's a business account
     */
    @Setter
    private String businessWebsite;

    /**
     * The email of this account, if it's a business account
     */
    @Setter
    private String businessEmail;

    /**
     * The category of this account, if it's a business account
     */
    @Setter
    private BusinessCategory businessCategory;

    /**
     * The hash of the companion associated with this session
     */
    @Getter
    @Setter
    private String deviceHash;

    /**
     * A map of all the devices that the companion has associated using WhatsappWeb
     * The key here is the index of the device's key
     * The value is the device's companion jid
     */
    @Setter
    @Default
    private LinkedHashMap<ContactJid, Integer> linkedDevicesKeys = new LinkedHashMap<>();

    /**
     * The profile picture of the user linked to this account. This field will be null while the user
     * hasn't logged in yet. This field can also be null if no image was set.
     */
    @Setter
    private URI profilePicture;

    /**
     * The status of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String about;

    /**
     * The user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Getter
    @Setter
    private ContactJid jid;

    /**
     * The lid user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Getter
    @Setter
    private ContactJid lid;

    /**
     * The non-null map of properties received by whatsapp
     */
    @NonNull
    @Default
    @Setter
    private ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

    /**
     * The non-null map of chats
     */
    @NonNull
    @Default
    @JsonIgnore
    private ConcurrentHashMap<ContactJid, Chat> chats = new ConcurrentHashMap<>();

    /**
     * The non-null map of contacts
     */
    @NonNull
    @Default
    private ConcurrentHashMap<ContactJid, Contact> contacts = new ConcurrentHashMap<>();

    /**
     * The non-null list of status messages
     */
    @NonNull
    @Default
    private ConcurrentHashMap<ContactJid, ConcurrentLinkedDeque<MessageInfo>> status = new ConcurrentHashMap<>();

    /**
     * The non-null map of privacy settings
     */
    @NonNull
    @Default
    private ConcurrentHashMap<PrivacySettingType, PrivacySettingEntry> privacySettings = new ConcurrentHashMap<>();

    /**
     * The non-null map of calls
     */
    @NonNull
    @Default
    private ConcurrentHashMap<String, Call> calls = new ConcurrentHashMap<>();

    /**
     * Whether chats should be unarchived if a new message arrives
     */
    @Getter
    @Setter
    private boolean unarchiveChats;

    /**
     * Whether the twenty-hours format is being used by the client
     */
    @Getter
    @Setter
    private boolean twentyFourHourFormat;

    /**
     * The non-null list of requests that were sent to Whatsapp. They might or might not be waiting
     * for a response
     */
    @NonNull
    @JsonIgnore
    @Default
    private ConcurrentHashMap<String, Request> requests = new ConcurrentHashMap<>();

    /**
     * The non-null list of replies waiting to be fulfilled
     */
    @NonNull
    @JsonIgnore
    @Default
    private KeySetView<ReplyHandler, Boolean> replyHandlers = ConcurrentHashMap.newKeySet();

    /**
     * The non-null list of listeners
     */
    @NonNull
    @JsonIgnore
    @Default
    private final KeySetView<Listener, Boolean> listeners = ConcurrentHashMap.newKeySet();

    /**
     * The request tag, used to create messages
     */
    @NonNull
    @JsonIgnore
    @Default
    private String tag = HexFormat.of().formatHex(BytesHelper.random(1));

    /**
     * The timestamp in seconds for the initialization of this object
     */
    @Default
    @Getter
    private long initializationTimeStamp = Clock.nowSeconds();

    /**
     * The media connection associated with this store
     */
    @JsonIgnore
    private MediaConnection mediaConnection;

    /**
     * The media connection latch associated with this store
     */
    @JsonIgnore
    @Default
    private CountDownLatch mediaConnectionLatch = new CountDownLatch(1);

    /**
     * The request tag, used to create messages
     */
    @NonNull
    @Getter
    @Setter
    @Default
    private ChatEphemeralTimer newChatsEphemeralTimer = ChatEphemeralTimer.OFF;

    /**
     * The setting to use when generating previews for text messages that contain links
     */
    @Getter
    @Setter
    @Default
    private TextPreviewSetting textPreviewSetting = TextPreviewSetting.ENABLED_WITH_INFERENCE;

    /**
     * Describes how much chat history Whatsapp should send
     */
    @Getter
    @Setter
    @Default
    @NonNull
    private WebHistoryLength historyLength = WebHistoryLength.STANDARD;

    /**
     * Whether listeners should be automatically scanned and registered or not
     */
    @Getter
    @Setter
    @Default
    private boolean autodetectListeners = true;


    /**
     * Whether updates about the presence of the session should be sent automatically to Whatsapp
     * For example, when the bot is started, the status of the companion is changed to available if this option is enabled
     */
    @Getter
    @Setter
    @Default
    private boolean automaticPresenceUpdates = true;

    /**
     * The release channel to use when connecting to Whatsapp
     * This should allow the use of beta features
     */
    @Getter
    @Setter
    @NonNull
    @Default
    private UserAgentReleaseChannel releaseChannel = UserAgentReleaseChannel.RELEASE;

    /**
     * Metadata about the device that is being simulated for Whatsapp
     */
    @Getter
    @Setter
    @NonNull
    private CompanionDevice device;

    /**
     * The os of the associated device, available only for the web api
     */
    @Setter
    private UserAgentPlatform companionDeviceOs;

    /**
     * Whether the mac of every app state request should be checked
     */
    @Getter
    @Setter
    @Default
    private boolean checkPatchMacs = false;

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Store of(UUID uuid, @NonNull ClientType clientType) {
        return of(uuid, clientType, DefaultControllerSerializer.instance());
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer              
     * @return a non-null store
     */
    public static Store of(UUID uuid, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        return ofNullable(uuid, clientType, serializer)
                .orElseGet(() -> random(uuid, null, clientType, serializer));
    }

    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(UUID uuid, @NonNull ClientType clientType) {
        return ofNullable(uuid, clientType, DefaultControllerSerializer.instance());
    }

    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(UUID uuid, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        if(uuid == null){
            return Optional.empty();
        }

        var store = serializer.deserializeStore(clientType, uuid);
        store.ifPresent(serializer::attributeStore);
        return store;
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param phoneNumber the phone number of the session to load
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Store of(UUID uuid, long phoneNumber, @NonNull ClientType clientType) {
        return of(uuid, phoneNumber, clientType, DefaultControllerSerializer.instance());
    }
    
    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param uuid        the uuid of the session to load, can be null
     * @param phoneNumber the phone number of the session to load
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer              
     * @return a non-null store
     */
    public static Store of(UUID uuid, long phoneNumber, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        return ofNullable(phoneNumber, clientType, serializer)
                .orElseGet(() -> random(uuid, phoneNumber, clientType, serializer));
    }

    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param phoneNumber the phone number of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(Long phoneNumber, @NonNull ClientType clientType) {
        return ofNullable(phoneNumber, clientType, DefaultControllerSerializer.instance());
    }
    
    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param phoneNumber the phone number of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(Long phoneNumber, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        if(phoneNumber == null){
            return Optional.empty();
        }
        
        var store = serializer.deserializeStore(clientType, phoneNumber);
        store.ifPresent(serializer::attributeStore);
        return store;
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param alias the alias of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Store of(UUID uuid, String alias, @NonNull ClientType clientType) {
        return of(uuid, alias, clientType, DefaultControllerSerializer.instance());
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param uuid       the uuid of the session to load, can be null
     * @param alias      the alias of the session to load, can be null
     * @param clientType the non-null type of the client
     * @param serializer the non-null serializer
     * @return a non-null store
     */
    public static Store of(UUID uuid, String alias, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        return ofNullable(alias, clientType, serializer)
                .orElseGet(() -> random(uuid, null, clientType, serializer, alias));
    }

    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param alias the alias of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(String alias, @NonNull ClientType clientType) {
        return ofNullable(alias, clientType, DefaultControllerSerializer.instance());
    }

    /**
     * Returns the store saved in memory or returns an empty optional
     *
     * @param alias the alias of the session to load, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer
     * @return a non-null store
     */
    public static Optional<Store> ofNullable(String alias, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer) {
        if(alias == null){
            return Optional.empty();
        }

        var store = serializer.deserializeStore(clientType, alias);
        store.ifPresent(serializer::attributeStore);
        return store;
    }

    /**
     * Constructs a new default instance of WhatsappStore
     *
     * @param uuid        the uuid of the session to create, can be null
     * @param phoneNumber the phone number of the session to create, can be null
     * @param clientType  the non-null type of the client
     * @param alias       the alias of the controller
     * @return a non-null store
     */
    public static Store random(UUID uuid, Long phoneNumber, @NonNull ClientType clientType, String... alias) {
        return random(uuid, phoneNumber, clientType, DefaultControllerSerializer.instance(), alias);
    }

    /**
     * Constructs a new default instance of WhatsappStore
     *
     * @param uuid        the uuid of the session to create, can be null
     * @param phoneNumber the phone number of the session to create, can be null
     * @param clientType  the non-null type of the client
     * @param serializer  the non-null serializer
     * @param alias       the alias of the controller
     * @return a non-null store
     */
    public static Store random(UUID uuid, Long phoneNumber, @NonNull ClientType clientType, @NonNull ControllerSerializer serializer, String... alias) {
        var phone = PhoneNumber.ofNullable(phoneNumber).orElse(null);
        var result = Store.builder()
                .alias(Objects.requireNonNullElseGet(Arrays.asList(alias), ArrayList::new))
                .serializer(serializer)
                .clientType(clientType)
                .jid(phone == null ? null : phone.toJid())
                .phoneNumber(phone)
                .device(getDefaultDevice(clientType))
                .uuid(Objects.requireNonNullElseGet(uuid, UUID::randomUUID))
                .build();
        serializer.linkMetadata(result);
        return result;
    }

    private static CompanionDevice getDefaultDevice(ClientType clientType) {
        return switch (clientType) {
            case WEB -> CompanionDevice.windows();
            case MOBILE -> CompanionDevice.android();
        };
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByJid(ContactJidProvider jid) {
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
                .filter(contact -> Objects.equals(contact.fullName(), name) || Objects.equals(contact.shortName(), name) || Objects.equals(contact.chosenName(), name));
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
    public Optional<MessageInfo> findMessageById(ContactJidProvider provider, String id) {
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
    public Optional<Chat> findChatByJid(ContactJidProvider jid) {
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
    public Collection<MessageInfo> findStatusBySender(ContactJidProvider jid) {
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
        var contextualMessage = response.message().contentWithContext();
        if (contextualMessage.isEmpty()) {
            return false;
        }
        var contextualMessageId = contextualMessage.get().contextInfo().quotedMessageId().orElse(null);
        if (contextualMessageId == null) {
            return false;
        }
        var result = replyHandlers.stream().filter(entry -> entry.id().equals(contextualMessageId)).findFirst();
        result.ifPresent(reply -> {
            replyHandlers.remove(reply);
            reply.future().complete(response);
        });
        return result.isPresent();
    }

    /**
     * Adds a chat in memory
     *
     * @param chatJid the chat to add
     * @return the input chat
     */
    public Chat addNewChat(@NonNull ContactJid chatJid) {
        var chat = Chat.ofJid(chatJid);
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
        if (chat.hasName() && chat.jid().hasServer(ContactJid.Server.WHATSAPP)) {
            var contact = findContactByJid(chat.jid())
                    .orElseGet(() -> addContact(Contact.ofJid(chat.jid())));
            contact.fullName(chat.name());
        }
        var oldChat = chats.get(chat.jid());
        if(oldChat != null) {
            if(oldChat.hasName() && !chat.hasName()){
                chat.name(oldChat.name()); // Coming from contact actions
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
    public Optional<Chat> removeChat(@NonNull ContactJid chatJid) {
        return Optional.ofNullable(chats.remove(chatJid));
    }

    /**
     * Adds a contact in memory
     *
     * @param contactJid the contact to add
     * @return the input contact
     */
    public Contact addContact(@NonNull ContactJid contactJid) {
        return addContact(Contact.ofJid(contactJid));
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
        info.key().chat(chat);
        if(info.fromMe() && jid != null && !Objects.equals(info.senderJid().user(), jid.user())){
            info.key().senderJid(jid.toWhatsappJid());
        }
        info.key()
                .senderJid()
                .ifPresent(senderJid -> attributeSender(info, senderJid));
        info.message()
                .contentWithContext()
                .map(ContextualMessage::contextInfo)
                .ifPresent(this::attributeContext);
        processMessage(info);
        return info;
    }

    private MessageKey attributeSender(MessageInfo info, ContactJid senderJid) {
        var contact = findContactByJid(senderJid).orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        return info.sender(contact).key().sender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageChatJid().ifPresent(chatJid -> attributeContextChat(contextInfo, chatJid));
    }

    private void attributeContextChat(ContextInfo contextInfo, ContactJid chatJid) {
        var chat = findChatByJid(chatJid).orElseGet(() -> addNewChat(chatJid));
        contextInfo.quotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, ContactJid senderJid) {
        var contact = findContactByJid(senderJid).orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        contextInfo.quotedMessageSender(contact);
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
        if(pollCreationMessage.encryptionKey() != null){
            return;
        }

        info.message()
                .deviceInfo()
                .messageSecret()
                .or(info::messageSecret)
                .ifPresent(pollCreationMessage::encryptionKey);
    }

    private void handlePollUpdate(MessageInfo info, PollUpdateMessage pollUpdateMessage) {
        var originalPollInfo = findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
        var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
        pollUpdateMessage.pollCreationMessage(originalPollMessage);
        var originalPollSender = originalPollInfo.senderJid()
                .toWhatsappJid()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        var modificationSenderJid = info.senderJid().toWhatsappJid();
        pollUpdateMessage.voter(modificationSenderJid);
        var modificationSender = modificationSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var secretName = pollUpdateMessage.secretName().getBytes(StandardCharsets.UTF_8);
        var useSecretPayload = BytesHelper.concat(
                originalPollInfo.id().getBytes(StandardCharsets.UTF_8),
                originalPollSender,
                modificationSender,
                secretName
        );
        var useCaseSecret = Hkdf.extractAndExpand(originalPollMessage.encryptionKey(), useSecretPayload, 32);
        var additionalData = "%s\0%s".formatted(
                originalPollInfo.id(),
                modificationSenderJid
        );
        var metadata = pollUpdateMessage.encryptedMetadata();
        var decrypted = AesGcm.decrypt(metadata.iv(), metadata.payload(), useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollVoteMessage = Protobuf.readMessage(decrypted, PollUpdateEncryptedOptions.class);
        var selectedOptions = pollVoteMessage.selectedOptions()
                .stream()
                .map(sha256 -> originalPollMessage.selectableOptionsHashesMap().get(HexFormat.of().formatHex(sha256)))
                .filter(Objects::nonNull)
                .toList();
        originalPollMessage.selectedOptionsMap().put(modificationSenderJid, selectedOptions);
        pollUpdateMessage.votes(selectedOptions);
        var update = new PollUpdate(info.key(), pollVoteMessage, Clock.nowMilliseconds());
        info.pollUpdates().add(update);
    }

    private void handleReactionMessage(MessageInfo info, ReactionMessage reactionMessage) {
        info.ignore(true);
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
                .sorted(Comparator.comparingLong((Chat chat) -> chat.pinnedTimestampSeconds()).reversed())
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
                .sorted(Comparator.comparingLong((Chat chat) -> chat.timestampSeconds()).reversed())
                .toList();
    }

    /**
     * Returns the non-null map of properties received by whatsapp
     *
     * @return an unmodifiable map
     */
    public Map<String, String> properties(){
        return Collections.unmodifiableMap(properties);
    }

    /**
     * The media connection associated with this store
     *
     * @return the media connection
     */
    public MediaConnection mediaConnection() {
        return mediaConnection(Duration.ofMinutes(2));
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
    public Store mediaConnection(MediaConnection mediaConnection) {
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
     * @param reply the non-null reply handler to add
     * @return the non-null completable result of the reply handler
     */
    public CompletableFuture<MessageInfo> addPendingReply(@NonNull ReplyHandler reply) {
        replyHandlers.add(reply);
        return reply.future();
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
    public Collection<PrivacySettingEntry> privacySettings(){
        return privacySettings.values();
    }

    /**
     * Queries the privacy setting entry for the type
     *
     * @param type a non-null type
     * @return a non-null entry
     */
    public PrivacySettingEntry findPrivacySetting(@NonNull PrivacySettingType type){
        return privacySettings.get(type);
    }

    /**
     * Sets the privacy setting entry for a type
     *
     * @param type a non-null type
     * @param entry the non-null entry
     * @return the old privacy setting entry
     */
    public PrivacySettingEntry addPrivacySetting(@NonNull PrivacySettingType type, @NonNull PrivacySettingEntry entry){
        return privacySettings.put(type, entry);
    }

    /**
     * Returns an unmodifiable map that contains every companion associated using Whatsapp web mapped to its key index
     *
     * @return an unmodifiable map
     */
    public Map<ContactJid, Integer> linkedDevicesKeys(){
        return Collections.unmodifiableMap(linkedDevicesKeys);
    }


    /**
     * Returns an unmodifiable list that contains the devices associated using Whatsapp web to this session's companion
     *
     * @return an unmodifiable list
     */
    public Collection<ContactJid> linkedDevices(){
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
    public Optional<Integer> addLinkedDevice(@NonNull ContactJid companion, int keyId){
        return Optional.ofNullable(linkedDevicesKeys.put(companion, keyId));
    }

    /**
     * Removes a companion
     * Only use this method in the mobile api
     *
     * @param companion a non-null companion
     * @return the nullable old key
     */
    public Optional<Integer> removeLinkedCompanion(@NonNull ContactJid companion){
        return Optional.ofNullable(linkedDevicesKeys.remove(companion));
    }

    /**
     * Removes all linked companion
     */
    public void removeLinkedCompanions(){
        linkedDevicesKeys.clear();
    }

    /**
     * Returns an immutable collection of listeners
     *
     * @return a non-null collection
     */
    public Collection<Listener> listeners(){
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
     * Sets the version of this session
     *
     * @param version the non-null version
     * @return a non-null version
     */
    public Store version(@NonNull Version version){
        this.version = version;
        return this;
    }

    /**
     * Returns the version of this object
     *
     * @return a non-null version
     */
    public Version version(){
        if(version == null){
            this.version = MetadataHelper.getVersion(device.osType(), business).join();
        }

        return version;
    }

    /**
     * Sets the proxy used by this session
     *
     * @return the same instance
     */
    public Store proxy(URI proxy) {
        if(proxy != null && proxy.getUserInfo() != null){
            ProxyAuthenticator.register(proxy);
        }else if(proxy == null && this.proxy != null && this.proxy.getUserInfo() != null){
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
    public Optional<UserAgentPlatform> companionDeviceOs() {
        return Optional.ofNullable(companionDeviceOs);
    }

    /**
     * The address of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessAddress(){
        return Optional.ofNullable(businessAddress);
    }

    /**
     * The longitude of this account's location, if it's a business account
     *
     * @return an optional
     */
    public Optional<Double> businessLongitude(){
        return Optional.ofNullable(businessLongitude);
    }

    /**
     * The latitude of this account's location, if it's a business account
     *
     * @return an optional
     */
    public Optional<Double> businessLatitude(){
        return Optional.ofNullable(businessLatitude);
    }

    /**
     * The description of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessDescription(){
        return Optional.ofNullable(businessDescription);
    }

    /**
     * The website of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessWebsite(){
        return Optional.ofNullable(businessWebsite);
    }

    /**
     * The email of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<String> businessEmail(){
        return Optional.ofNullable(businessEmail);
    }

    /**
     * The category of this account, if it's a business account
     *
     * @return an optional
     */
    public Optional<BusinessCategory> businessCategory(){
        return Optional.ofNullable(businessCategory);
    }

    public void dispose() {
        serialize(false);
        mediaConnectionLatch.countDown();
        mediaConnectionLatch = new CountDownLatch(1);
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

    public static abstract class StoreBuilder<C extends Store, B extends StoreBuilder<C, B>> extends ControllerBuilder<Store, C, B> {
        public StoreBuilder<C, B> proxy(URI proxy) {
            if(proxy != null && proxy.getUserInfo() != null){
                ProxyAuthenticator.register(proxy);
            }

            this.proxy = proxy;
            return this;
        }
    }
}
