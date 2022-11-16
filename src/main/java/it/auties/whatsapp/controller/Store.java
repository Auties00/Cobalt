package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.bytes.Bytes;
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
import it.auties.whatsapp.model.request.NodeHandler;
import it.auties.whatsapp.model.request.ReplyHandler;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.serialization.ControllerProviderLoader;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.ConcurrentSet;
import it.auties.whatsapp.util.InitializationLock;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

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
    private static ConcurrentSet<Store> stores = new ConcurrentSet<>();

    /**
     * The session id of this store
     */
    @Getter
    private int id;

    /**
     * The locale of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userLocale;

    /**
     * The name of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userName;

    /**
     * The profile picture of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * This field can also be null if no image was set.
     */
    @Setter
    private URI userProfilePicture;

    /**
     * The status of the user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     * Assumed to be non-null otherwise.
     */
    @Getter
    @Setter
    private String userStatus;

    /**
     * The user linked to this account.
     * This field will be null while the user hasn't logged in yet.
     */
    @Getter
    @Setter
    private ContactJid userCompanionJid;

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
    private ConcurrentHashMap<ContactJid, Set<MessageInfo>> status = new ConcurrentHashMap<>();

    /**
     * The non-null map of privacy settings
     */
    @NonNull
    @Default
    @Getter
    private ConcurrentHashMap<PrivacySettingType, PrivacySettingValue> privacySettings = new ConcurrentHashMap<>();

    /**
     * Whether this store has already received the snapshot from
     * Whatsapp Web containing chats and contacts
     */
    @Getter
    @Setter
    private boolean initialSnapshot;

    /**
     * Whether the initial app sync has already been sent
     */
    @Getter
    @Setter
    private boolean initialAppSync;

    /**
     * Whether chats should be unarchived if a new message arrives
     */
    @Getter
    @Setter
    private boolean unarchiveChats;

    /**
     * The non-null list of requests that are waiting for a response from Whatsapp
     */
    @NonNull
    @JsonIgnore
    @Default
    private ConcurrentLinkedDeque<Request> pendingRequests = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of all the predicates awaiting a result
     */
    @NonNull
    @JsonIgnore
    @Default
    private ConcurrentLinkedDeque<NodeHandler> pendingHandlers = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of replies waiting to be fulfilled
     */
    @NonNull
    @JsonIgnore
    @Default
    private ConcurrentLinkedDeque<ReplyHandler> replyHandlers = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of listeners
     */
    @NonNull
    @JsonIgnore
    @Default
    @Getter
    private ConcurrentLinkedDeque<Listener> listeners = new ConcurrentLinkedDeque<>();

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
    private String tag = Bytes.ofRandom(1)
            .toHex()
            .toLowerCase(Locale.ROOT);

    /**
     * The timestamp in seconds for the initialization of this object
     */
    @Default
    @Getter
    private long initializationTimeStamp = Clock.now();

    /**
     * The non-null service used to call listeners.
     * This is needed in order to not block the socket.
     */
    @NonNull
    @JsonIgnore
    @Default
    private ScheduledExecutorService requestsService = newScheduledThreadPool(10);

    /**
     * The media connection associated with this store
     */
    @JsonIgnore
    @Default
    private InitializationLock<MediaConnection> mediaConnection = new InitializationLock<>();

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
     * Constructs a new default instance of WhatsappStore
     *
     * @param id the unsigned jid of this store
     * @param useDefaultSerializer whether the default serializer should be used
     * @return a non-null store
     */
    public static Store random(int id, boolean useDefaultSerializer) {
        var result = Store.builder()
                .id(id)
                .useDefaultSerializer(useDefaultSerializer)
                .build();
        stores.add(result);
        return result;
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param id the jid of this session
     * @param useDefaultSerializer whether the default serializer should be used
     * @return a non-null store
     */
    public static Store of(int id, boolean useDefaultSerializer) {
        var deserializer = ControllerProviderLoader.findOnlyDeserializer(useDefaultSerializer);
        var result = deserializer.deserializeStore(id)
                .map(store -> store.useDefaultSerializer(useDefaultSerializer))
                .orElseGet(() -> random(id, useDefaultSerializer));
        deserializer.attributeStore(result); // Run async
        return result;
    }

    /**
     * Queries the first store whose id is equal to {@code id}
     *
     * @param id the id to search
     * @return a non-null optional
     */
    public static Optional<Store> findStoreById(int id) {
        return Collections.synchronizedSet(stores)
                .parallelStream()
                .filter(entry -> entry.id() == id)
                .findFirst();
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Contact> findContactByJid(ContactJidProvider jid) {
        return jid == null ?
                Optional.empty() :
                contacts().parallelStream()
                        .filter(contact -> contact.jid()
                                .user()
                                .equals(jid.toJid()
                                        .user()))
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

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Contact> findContactsByName(String name) {
        return findContactsStream(name).collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Contact> findContactsStream(String name) {
        return name == null ?
                Stream.empty() :
                contacts().parallelStream()
                        .filter(contact -> Objects.equals(contact.fullName(), name) || Objects.equals(
                                contact.shortName(), name) || Objects.equals(contact.chosenName(), name));
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-null optional
     */
    public Optional<Chat> findChatByJid(ContactJidProvider jid) {
        return jid == null ?
                Optional.empty() :
                chats().parallelStream()
                        .filter(chat -> chat.jid()
                                .user()
                                .equals(jid.toJid()
                                        .user()))
                        .findAny();
    }

    /**
     * Queries the message in {@code chat} whose jid is equal to {@code jid}
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non-null optional
     */
    public Optional<MessageInfo> findMessageById(Chat chat, String id) {
        return chat == null || id == null ?
                Optional.empty() :
                chat.messages()
                        .parallelStream()
                        .filter(message -> Objects.equals(message.key()
                                .id(), id))
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

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-null immutable set
     */
    public Set<Chat> findChatsByName(String name) {
        return findChatsStream(name).collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Chat> findChatsStream(String name) {
        return name == null ?
                Stream.empty() :
                chats().parallelStream()
                        .filter(chat -> chat.name()
                                .equalsIgnoreCase(name));
    }

    /**
     * Queries the first status whose id matches the one provided
     *
     * @param id the id of the status
     * @return a non-null optional
     */
    public Optional<MessageInfo> findStatusById(String id) {
        return id == null ?
                Optional.empty() :
                status().stream()
                        .filter(status -> Objects.equals(status.id(), id))
                        .findFirst();
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
     * Queries the first request whose id is equal to {@code id}
     *
     * @param id the id to search, can be null
     * @return a non-null optional
     */
    public Optional<Request> findPendingRequest(String id) {
        return id == null ?
                Optional.empty() :
                pendingRequests.parallelStream()
                        .filter(request -> Objects.equals(request.id(), id))
                        .findAny();
    }

    /**
     * Queries the first request whose id equals the one stored by the response and, if any is found, it completes it
     *
     * @param response the response to complete the request with
     * @param exceptionally whether the response is erroneous
     * @return a boolean
     */
    public boolean resolvePendingRequest(@NonNull Node response, boolean exceptionally) {
        return findPendingRequest(response.id())
                .map(request -> deleteAndComplete(response, request, exceptionally))
                .isPresent();
    }

    /**
     * Queries the first reply waiting and completes it with the input message
     *
     * @param response the response to complete the reply with
     * @return a boolean
     */
    public boolean resolvePendingReply(@NonNull MessageInfo response) {
        var contextualMessage = response.message().contentWithContext();
        if(contextualMessage.isEmpty()){
            return false;
        }

        var contextualMessageId = contextualMessage.get()
                .contextInfo()
                .quotedMessageId()
                .orElse(null);
        if(contextualMessageId == null){
            return false;
        }

        var result = replyHandlers.stream()
                .filter(entry -> entry.id().equals(contextualMessageId))
                .findFirst();
        result.ifPresent(reply -> {
            replyHandlers.remove(reply);
            reply.future().complete(response);
        });
        return result.isPresent();
    }

    /**
     * Queries the first handler that matches a handler in memory and uses its consumer
     *
     * @param response the response to test the handler with
     * @return a boolean
     */
    public boolean resolvePendingHandler(@NonNull Node response){
        var result = pendingHandlers.stream()
                .filter(predicate -> predicate.predicate().test(response))
                .findFirst();
        result.ifPresent(nodeHandler -> {
            pendingHandlers.remove(nodeHandler);
            nodeHandler.future().complete(response);
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
        chat.messages()
                .forEach(this::attribute);
        if(chat.hasName() && chat.jid().hasServer(ContactJid.Server.WHATSAPP)){
            findContactByJid(chat.jid())
                    .orElseGet(() -> addContact(Contact.ofJid(chat.jid())))
                    .fullName(chat.name());
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
        var oldContact = contacts.get(contact.jid());
        contacts.put(contact.jid(), contact);
        callListeners(listener -> listener.onNewContact(contact));
        return contact;
    }

    public void attribute(MessageInfo info) {
        var chat = findChatByJid(info.chatJid())
                .orElseGet(() -> addChat(Chat.ofJid(info.chatJid())));
        info.key().chat(chat);
        info.key()
                .senderJid()
                .ifPresent(senderJid -> attributeSender(info, senderJid));
        info.message()
                .contentWithContext()
                .map(ContextualMessage::contextInfo)
                .ifPresent(this::attributeContext);
    }

    private MessageKey attributeSender(MessageInfo info, ContactJid senderJid) {
        var contact = findContactByJid(senderJid)
                .orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        return info.sender(contact)
                .key()
                .sender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid()
                .ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageChatJid()
                .ifPresent(chatJid -> attributeContextChat(contextInfo, chatJid));
    }

    private void attributeContextChat(ContextInfo contextInfo, ContactJid chatJid) {
        var chat = findChatByJid(chatJid)
                .orElseGet(() -> addChat(Chat.ofJid(chatJid)));
        contextInfo.quotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, ContactJid senderJid) {
        var contact = findContactByJid(senderJid)
                .orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        contextInfo.quotedMessageSender(contact);
    }

    /**
     * Returns the chats pinned to the top
     *
     * @return a non-null list of chats
     */
    public List<Chat> pinnedChats() {
        return chats().parallelStream()
                .filter(Chat::isPinned)
                .toList();
    }

    /**
     * Returns all the starred messages
     *
     * @return a non-null list of messages
     */
    public List<MessageInfo> starredMessages() {
        return chats().parallelStream()
                .map(Chat::starredMessages)
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Clears all the data that this object holds and closes the pending requests
     */
    @Override
    public void clear() {
        chats.clear();
        contacts.clear();
        status.clear();
        listeners.clear();
        pendingRequests.forEach(request -> request.complete(null, false));
        pendingRequests.clear();
    }

    /**
     * Executes an operation on every registered listener on the listener thread
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     *
     * @param consumer the operation to execute
     */
    @SneakyThrows
    public void invokeListeners(Consumer<Listener> consumer) {
        if (requestsService.isShutdown()) {
            this.requestsService = newSingleThreadScheduledExecutor();
        }

        var futures = listeners.stream()
                .map(listener -> runAsync(() -> consumer.accept(listener), requestsService))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures)
                .get();
    }

    /**
     * Executes an operation on every registered listener on the listener thread
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     *
     * @param consumer the operation to execute
     */
    public void callListeners(Consumer<Listener> consumer) {
        listeners.forEach(listener -> callListener(consumer, listener));
    }

    private void callListener(Consumer<Listener> consumer, Listener listener) {
        if (requestsService.isShutdown()) {
            this.requestsService = newSingleThreadScheduledExecutor();
        }

        requestsService.execute(() -> consumer.accept(listener));
    }

    private Request deleteAndComplete(Node response, Request request, boolean exceptionally) {
        pendingRequests.remove(request);
        request.complete(response, exceptionally);
        return request;
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
        return mediaConnection.read();
    }

    /**
     * Writes a media connection
     *
     * @param mediaConnection a media connection
     * @return the same instance
     */
    public Store mediaConnection(MediaConnection mediaConnection) {
        this.mediaConnection.write(mediaConnection);
        return this;
    }

    /**
     * Returns all the chats
     *
     * @return an immutable collection
     */
    public Collection<Chat> chats() {
        return Collections.unmodifiableCollection(chats.values());
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
     * Returns all the blocked contacts
     *
     * @return an immutable collection
     */
    public Collection<Contact> blockedContacts() {
        return contacts().stream()
                .filter(Contact::blocked)
                .toList();
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
     * Adds a status to this store
     *
     * @param info the non-null status to add
     * @return the same instance
     */
    public Store addStatus(@NonNull MessageInfo info) {
        attribute(info);
        var wrapper = Objects.requireNonNullElseGet(status.get(info.senderJid()), ConcurrentSet<MessageInfo>::new);
        wrapper.add(info);
        status.put(info.senderJid(), wrapper);
        return this;
    }

    /**
     * Adds a pending request to this store
     *
     * @param request the non-null status to add
     * @return the non-null completable future of the request
     */
    public CompletableFuture<Node> addPendingRequest(@NonNull Request request) {
        pendingRequests.add(request);
        return request.future();
    }

    /**
     * Adds a pending request to this store
     *
     * @param handler the non-null handler to add
     * @return the non-null completable future of the handler
     */
    public CompletableFuture<Node> addNodeHandler(@NonNull NodeHandler handler) {
        pendingHandlers.add(handler);
        return handler.future();
    }

    /**
     * Adds a replay handler to this store
     *
     * @param reply the non-null reply handler to add
     * @return the non-null completable future of the reply handler
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
        requestsService.shutdownNow();
        serialize(false);
    }

    @Override
    public void serialize(boolean async) {
        ControllerProviderLoader.findAllSerializers(useDefaultSerializer())
                .forEach(serializer -> serializer.serializeStore(this, async));
    }
}
