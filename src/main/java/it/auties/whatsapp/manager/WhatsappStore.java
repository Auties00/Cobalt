package it.auties.whatsapp.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.media.MediaConnection;
import it.auties.whatsapp.socket.Node;
import it.auties.whatsapp.socket.Request;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.prefs.Preferences.userRoot;

/**
 * This class holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true)
@Log
@SuppressWarnings({"unused", "UnusedReturnValue"}) // Chaining
public class WhatsappStore implements JacksonProvider {
    /**
     * The path used to serialize and deserialize this object
     */
    private static final String PREFERENCES_PATH = WhatsappStore.class.getName();

    /**
     * The unsigned jid
     */
    @JsonProperty
    private final int id;

    /**
     * The non-null list of chats
     */
    @NonNull
    @JsonProperty
    private final List<@NonNull Chat> chats;

    /**
     * The non-null list of status messages
     */
    @NonNull
    @JsonProperty
    private final List<@NonNull MessageInfo> status;

    /**
     * The non-null list of contacts
     */
    @NonNull
    @JsonProperty
    private final List<@NonNull Contact> contacts;

    /**
     * The non-null list of requests that are waiting for a response from Whatsapp
     */
    @NonNull
    private final List<@NonNull Request> pendingRequests;

    /**
     * The non-null list of listeners
     */
    @NonNull
    private final List<@NonNull WhatsappListener> listeners;

    /**
     * Request counter
     */
    private final AtomicLong counter;

    /**
     * The request tag, used to create messages
     */
    private final String tag;

    /**
     * The timestamp in seconds for the initialization of this object
     */
    private final long initializationTimeStamp;

    /**
     * The media connection associated with this store
     */
    @Getter(onMethod = @__(@NonNull))
    private MediaConnection mediaConnection;

    /**
     * The non-null service used to call listeners.
     * This is needed in order to not block the socket.
     */
    @NonNull
    private ExecutorService requestsService;

    /**
     * Constructs a new default instance of WhatsappStore
     *
     * @param id the unsigned jid of this store
     * @return a non-null instance of WhatsappStore
     */
    public static WhatsappStore newStore(int id){
        return new WhatsappStore(WhatsappUtils.saveId(id));
    }

    /**
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param id the jid of this session
     * @return a non-null instance of WhatsappStore
     */
    public static WhatsappStore fromMemory(int id){
        var path = WhatsappUtils.createPreferencesPath(PREFERENCES_PATH, id);
        var json = userRoot().get(path, null);
        return Optional.ofNullable(json)
                .map(value -> deserialize(id, value))
                .orElseGet(() -> newStore(id));
    }

    private static WhatsappStore deserialize(int id, String json) {
        try {
            return JACKSON.readValue(json, WhatsappStore.class);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace();
            log.warning("Cannot read store for jid %s: defaulting to new keys".formatted(id));
            return null;
        }
    }

    private WhatsappStore(int id){
        this(WhatsappUtils.saveId(id), new Vector<>(), new Vector<>(),
                new Vector<>(), new Vector<>(),
                new Vector<>(), new AtomicLong(),
                BinaryArray.random(12).toHex().toLowerCase(Locale.ROOT),
                Instant.now().getEpochSecond(),
                Executors.newSingleThreadExecutor());
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public Optional<Contact> findContactByJid(ContactJid jid) {
        return jid == null ? Optional.empty() : contacts.parallelStream()
                .filter(contact -> contact.jid().contentEquals(jid))
                .findAny();
    }

    /**
     * Queries the first contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public Optional<Contact> findContactByName(String name) {
        return findContactsStream(name)
                .findAny();
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public Set<Contact> findContactsByName(String name) {
        return findContactsStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Contact> findContactsStream(String name) {
        return name == null ? Stream.empty() : contacts.parallelStream()
                .filter(contact -> Objects.equals(contact.fullName(), name)
                        || Objects.equals(contact.shortName(), name)
                        || Objects.equals(contact.chosenName(), name));
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public Optional<Chat> findChatByJid(ContactJid jid) {
        return jid == null ? Optional.empty() : chats.parallelStream()
                .filter(chat -> chat.jid().contentEquals(jid))
                .findAny();
    }

    /**
     * Queries the message in {@code chat} whose jid is equal to {@code jid}
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non-empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public Optional<MessageInfo> findMessageById(Chat chat, String id) {
        return chat == null || id == null ? Optional.empty() : chat.messages()
                .parallelStream()
                .filter(message -> Objects.equals(message.key().id(), id))
                .findAny();
    }

    /**
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public Optional<Chat> findChatByName(String name) {
        return findChatsStream(name)
                .findAny();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public Set<Chat> findChatsByName(String name) {
        return findChatsStream(name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Chat> findChatsStream(String name) {
        return name == null ? Stream.empty() : chats.parallelStream()
                .filter(chat -> chat.name().equalsIgnoreCase(name));
    }

    /**
     * Queries the first request whose jid is equal to {@code jid}
     *
     * @param id the jid to search, can be null
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public Optional<Request> findPendingRequest(String id) {
        return id == null ? Optional.empty() : pendingRequests.parallelStream()
                .filter(request -> Objects.equals(request.id(), id))
                .findAny();
    }

    /**
     * Queries the first request whose jid equals the one stored by the response and, if any is found, it completes it
     *
     * @param response the response to complete the request with
     * @return true if any request matching {@code response} is found
     */
    public boolean resolvePendingRequest(@NonNull Node response, boolean exceptionally) {
        return findPendingRequest(response.id())
                .map(request -> deleteAndComplete(response, request, exceptionally))
                .isPresent();
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public Chat addChat(@NonNull Chat chat) {
        chat.messages().forEach(message -> message.store(this));
        chats.add(chat);
        return chat;
    }

    /**
     * Adds a contact in memory
     *
     * @param contact the contact to add
     * @return the input contact
     */
    public Contact addContact(@NonNull Contact contact) {
        contacts.add(contact);
        return contact;
    }

    /**
     * Returns the chats pinned to the top
     *
     * @return a non-null list of chats
     */
    public List<Chat> pinnedChats(){
        return chats.parallelStream()
                .filter(Chat::isPinned)
                .toList();
    }

    /**
     * Clears all the data that this object holds
     */
    public void clear() {
        chats.clear();
        contacts.clear();
        pendingRequests.clear();
    }

    /**
     * Terminate the service associated with this store
     */
    public void dispose(){
        requestsService.shutdownNow();
    }

    /**
     * Executes an operation on every registered listener on the listener thread
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     *
     * @param consumer the operation to execute
     */
    public void callListeners(@NonNull Consumer<WhatsappListener> consumer){
        listeners.forEach(listener -> callListener(consumer, listener));
    }

    private void callListener(Consumer<WhatsappListener> consumer, WhatsappListener listener) {
        if(requestsService.isShutdown()){
            this.requestsService = Executors.newSingleThreadExecutor();
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
     *
     * @return this
     */
    public WhatsappStore save(){
        try {
            var path = WhatsappUtils.createPreferencesPath(PREFERENCES_PATH, id);
            userRoot().put(path, JACKSON.writeValueAsString(this));
            return this;
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Cannot save keys to memory", exception);
        }
    }
}
