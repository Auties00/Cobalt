package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Preferences;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.java.Log;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * This controller holds the user-related data regarding a WhatsappWeb session
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Jacksonized
@Builder(access = AccessLevel.PROTECTED)
@Data
@Accessors(fluent = true, chain = true)
@Log
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Store implements Controller {
    /**
     * All the known stores
     */
    @JsonIgnore
    private static Set<Store> stores = ConcurrentHashMap.newKeySet();

    /**
     * The session id of this store
     */
    private int id;

    /**
     * The non-null list of chats
     */
    @NonNull
    @Default
    private ConcurrentLinkedDeque<Chat> chats = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of status messages
     */
    @NonNull
    @Default
    private ConcurrentLinkedDeque<MessageInfo> status = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of contacts
     */
    @NonNull
    @Default
    private ConcurrentLinkedDeque<Contact> contacts = new ConcurrentLinkedDeque<>();

    /**
     * Whether this store has already received the snapshot from
     * Whatsapp Web containing chats and contacts
     */
    private boolean hasSnapshot;

    /**
     * Whether chats should be unarchived if a new message arrives
     */
    private boolean unarchiveChats;

    /**
     * The non-null list of requests that are waiting for a response from Whatsapp
     */
    @NonNull
    @JsonIgnore
    @Default
    private ConcurrentLinkedDeque<Request> pendingRequests = new ConcurrentLinkedDeque<>();

    /**
     * The non-null list of listeners
     */
    @NonNull
    @JsonIgnore
    @Default
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
    @JsonIgnore
    @Default
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
    @Setter
    @JsonIgnore
    private MediaConnection mediaConnection;

    /**
     * Constructs a new default instance of WhatsappStore
     *
     * @param id the unsigned jid of this store
     * @return a non-null instance of WhatsappStore
     */
    public static Store random(int id) {
        var result = Store.builder()
                .id(id)
                .build();
        stores.add(result);
        return result;
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param id the jid of this session
     * @return a non-null instance of WhatsappStore
     */
    public static Store of(int id) {
        var preferences = Preferences.of("%s/store.json", id);
        var result = requireNonNullElseGet(preferences.readJson(new TypeReference<>() {
        }), () -> random(id));
        stores.add(result);
        return result;
    }

    /**
     * Deletes all the known keys from memory
     */
    public static void deleteAll() {
        var preferences = Preferences.of("store");
        preferences.delete();
    }


    /**
     * Queries the first store whose id is equal to {@code id}
     *
     * @param id the id to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
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
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
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
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
     */
    public Optional<Contact> findContactByName(String name) {
        return findContactsStream(name).findAny();
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
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
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
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
     * @return a non-empty Optional containing the result if it is found otherwise an empty Optional
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
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
     */
    public Optional<Chat> findChatByName(String name) {
        return findChatsStream(name).findAny();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
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
     * Queries all the status of a contact
     *
     * @param jid the sender of the status
     * @return a List containing every result
     */
    public List<MessageInfo> findStatusBySender(ContactJidProvider jid) {
        return jid == null ?
                List.of() :
                status().stream()
                        .filter(status -> Objects.equals(status.senderJid(), jid.toJid()))
                        .toList();
    }

    /**
     * Queries the first request whose jid is equal to {@code jid}
     *
     * @param id the jid to search, can be null
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional
     */
    public Optional<Request> findPendingRequest(String id) {
        return id == null ?
                Optional.empty() :
                pendingRequests().parallelStream()
                        .filter(request -> Objects.equals(request.id(), id))
                        .findAny();
    }

    /**
     * Queries the first request whose jid equals the one stored by the response and, if any is found, it completes it
     *
     * @param response the response to complete the request with
     * @return true if any request matching {@code response} is found
     */
    public boolean resolvePendingRequest(Node response, boolean exceptionally) {
        return findPendingRequest(response.id()).map(request -> deleteAndComplete(response, request, exceptionally))
                .isPresent();
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public Chat addChat(Chat chat) {
        chat.messages()
                .forEach(message -> message.storeId(id()));
        if (!chats.add(chat)) {
            log.warning("Chat already exists: %s".formatted(chat.jid()));
        }

        return chat;
    }

    /**
     * Adds a contact in memory
     *
     * @param contact the contact to add
     * @return the input contact
     */
    public Contact addContact(Contact contact) {
        if (!contacts.add(contact)) {
            log.warning("Contact already exists: %s".formatted(contact.jid()));
        }

        return contact;
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
        pendingRequests.forEach(request -> request.complete(null, false));
        pendingRequests.clear();
    }

    /**
     * Terminate the service associated with this store
     */
    public void dispose() {
        requestsService.shutdownNow();
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
     * Serializes this object to a json and saves it in memory
     */
    @Override
    public void save(boolean async) {
        var preferences = Preferences.of("%s/store.json", id);
        if (async) {
            preferences.writeJsonAsync(this);
            return;
        }

        preferences.writeJson(this);
    }

    /**
     * Deletes this store from memory
     */
    @Override
    public void delete() {
        delete(id);
    }

    /**
     * Clears the store associated with the provided id
     *
     * @param id the id of the store
     */
    public static void delete(int id) {
        var preferences = Preferences.of("%s/store.json", id);
        preferences.delete();
    }
}
