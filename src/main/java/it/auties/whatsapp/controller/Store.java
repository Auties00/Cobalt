package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.ReplyHandler;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.serialization.ControllerProviderLoader;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This controller holds the user-related data regarding a WhatsappWeb session
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Jacksonized
@Builder(access = AccessLevel.PROTECTED)
@Accessors(fluent = true, chain = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Store implements Controller<Store> {
    /**
     * All the known stores
     */
    @JsonIgnore
    private static ConcurrentHashMap<Integer, Store> stores = new ConcurrentHashMap<>();

    /**
     * The session id of this store
     */
    @Getter
    private int id;

    /**
     * The locale of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userLocale;

    /**
     * The name of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userName;

    /**
     * The profile picture of the user linked to this account. This field will be null while the user
     * hasn't logged in yet. This field can also be null if no image was set.
     */
    @Setter
    private URI userProfilePicture;

    /**
     * The status of the user linked to this account. This field will be null while the user hasn't
     * logged in yet. Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userStatus;

    /**
     * The user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Getter
    @Setter
    private ContactJid userCompanionJid;

    /**
     * The lid user linked to this account. This field will be null while the user hasn't logged in yet.
     */
    @Getter
    @Setter
    private ContactJid userCompanionLid;

    /**
     * The non-null toMap of chats
     */
    @NonNull
    @Default
    @JsonIgnore
    private ConcurrentHashMap<ContactJid, Chat> chats = new ConcurrentHashMap<>();

    /**
     * The non-null toMap of contacts
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
     * The non-null toMap of privacy settings
     */
    @NonNull
    @Default
    @Getter
    private ConcurrentHashMap<PrivacySettingType, PrivacySettingValue> privacySettings = new ConcurrentHashMap<>();

    /**
     * Whether this store has already received the snapshot from Whatsapp Web containing chats and
     * contacts
     */
    @Getter
    @Setter
    private boolean initialSync;

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
    @Getter
    private KeySetView<Listener, Boolean> listeners = ConcurrentHashMap.newKeySet();

    /**
     * Request counter
     */
    @NonNull
    @JsonIgnore
    @Default
    private AtomicLong counter = new AtomicLong();

    /**
     * The request tag, used to create messages
     */
    @NonNull
    @JsonIgnore
    @Default
    private String tag = Bytes.ofRandom(1).toHex().toLowerCase(Locale.ROOT);

    /**
     * The timestamp in seconds for the initialization of this object
     */
    @Default
    @Getter
    private long initializationTimeStamp = Clock.nowInSeconds();

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

    @JsonIgnore
    @Getter
    @Setter
    private boolean useDefaultSerializer;

    /**
     * The request tag, used to create messages
     */
    @NonNull
    @Getter
    @Setter
    @Default
    private ChatEphemeralTimer newChatsEphemeralTimer = ChatEphemeralTimer.OFF;

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param options the non-null options
     * @return a non-null store
     */
    public static Store of(@NonNull WhatsappOptions options) {
        var deserializer = ControllerProviderLoader.findOnlyDeserializer(options.defaultSerialization());
        var result = deserializer.deserializeStore(options.id())
                .map(store -> store.useDefaultSerializer(options.defaultSerialization()))
                .orElseGet(() -> random(options));
        deserializer.attributeStore(result); // Run async
        return result;
    }

    /**
     * Constructs a new default instance of WhatsappStore
     *
     * @param options the non-null options
     * @return a non-null store
     */
    public static Store random(@NonNull WhatsappOptions options) {
        var result = Store.builder().id(options.id()).useDefaultSerializer(options.defaultSerialization()).build();
        stores.put(result.id(), result);
        return result;
    }

    /**
     * Queries the first store whose id is equal to {@code id}
     *
     * @param id the id to search
     * @return a non-null optional
     */
    public static Optional<Store> findStoreById(int id) {
        return Optional.ofNullable(stores.get(id));
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByJid(ContactJidProvider jid) {
        return jid == null ? Optional.empty() : contacts().parallelStream()
                .filter(contact -> contact.jid().user().equals(jid.toJid().user()))
                .findAny();
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
        var chat = provider instanceof Chat value ? value : findChatByJid(provider.toJid()).orElse(null);
        if (chat == null) {
            return Optional.empty();
        }
        return chat.messages().parallelStream().filter(message -> Objects.equals(message.key().id(), id)).findAny();
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Chat> findChatByJid(ContactJidProvider jid) {
        return jid == null ? Optional.empty() : chats.values()
                .parallelStream()
                .filter(chat -> chat.jid().user().equals(jid.toJid().user()))
                .findAny();
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
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Chat> findChatsByName(String name) {
        return findChatsStream(name).collect(Collectors.toUnmodifiableSet());
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
        return status.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries all the status of a contact
     *
     * @param jid the sender of the status
     * @return a non-null immutable list
     */
    public Collection<MessageInfo> findStatusBySender(ContactJidProvider jid) {
        return Optional.ofNullable(status.get(jid.toJid())).map(Collections::unmodifiableCollection).orElseGet(Set::of);
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
    public Chat addChat(@NonNull ContactJid chatJid) {
        return addChat(Chat.ofJid(chatJid));
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public Chat addChat(@NonNull Chat chat) {
        chat.messages().forEach(this::attribute);
        if (chat.hasName() && chat.jid().hasServer(ContactJid.Server.WHATSAPP)) {
            findContactByJid(chat.jid()).orElseGet(() -> addContact(Contact.ofJid(chat.jid()))).fullName(chat.name());
        }

        return addChatDirect(chat);
    }

    /**
     * Adds a chat in memory without executing any check
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public Chat addChatDirect(Chat chat) {
        chats.put(chat.jid(), chat);
        return chat;
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
     * @param info a non-null message
     * @return the same incoming message
     */
    public MessageInfo attribute(@NonNull MessageInfo info) {
        var chat = findChatByJid(info.chatJid()).orElseGet(() -> addChat(Chat.ofJid(info.chatJid())));
        info.key().chat(chat);
        info.key().senderJid().ifPresent(senderJid -> attributeSender(info, senderJid));
        info.message().contentWithContext().map(ContextualMessage::contextInfo).ifPresent(this::attributeContext);
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
        var chat = findChatByJid(chatJid).orElseGet(() -> addChat(Chat.ofJid(chatJid)));
        contextInfo.quotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, ContactJid senderJid) {
        var contact = findContactByJid(senderJid).orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        contextInfo.quotedMessageSender(contact);
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
                .sorted(Comparator.comparingLong((Chat chat) -> chat.pinnedTimestampInSeconds()).reversed())
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
    public Collection<Chat> chats() {
        return chats.values().stream().sorted(Comparator.comparingLong(Chat::timestampInSeconds).reversed()).toList();
    }

    /**
     * Returns a request tag
     *
     * @return a non-null String
     */
    public String nextTag() {
        return "%s-%s".formatted(tag, counter.getAndIncrement());
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
    public Optional<URI> userProfilePicture() {
        return Optional.ofNullable(userProfilePicture);
    }

    public void dispose() {
        serialize(false);
        mediaConnectionLatch.countDown();
        mediaConnectionLatch = new CountDownLatch(1);
    }

    @Override
    public void serialize(boolean async) {
        ControllerProviderLoader.findAllSerializers(useDefaultSerializer())
                .forEach(serializer -> serializer.serializeStore(this, async));
    }
}
