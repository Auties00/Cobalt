package it.auties.whatsapp.manager;

import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.exchange.Request;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.model.MediaConnection;
import it.auties.whatsapp.protobuf.model.Node;
import it.auties.whatsapp.utils.WhatsappUtils;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Accessors(fluent = true)
public class WhatsappStore {
    private static final Set<WhatsappStore> instances = new HashSet<>();
    private final @NonNull UUID uuid;
    private final @NonNull ExecutorService requestsService;
    private final @NonNull List<Chat> chats;
    private final @NonNull List<Contact> contacts;
    private final @NonNull List<Request> pendingRequests;
    private final @NonNull List<WhatsappListener> listeners;
    private final AtomicLong readCounter;
    private final AtomicLong writeCounter;
    private final long initializationTimeStamp;
    private @Getter(onMethod = @__(@NonNull)) MediaConnection mediaConnection;
    private @Getter(onMethod = @__(@NonNull)) String phoneNumberJid;

    /**
     * Constructs a new default instance of WhatsappDataManager
     */
    public WhatsappStore(){
        this(UUID.randomUUID(), Executors.newSingleThreadExecutor(),
                new Vector<>(), new Vector<>(),
                new Vector<>(), new Vector<>(),
                new AtomicLong(), new AtomicLong(),
                System.currentTimeMillis());
        instances.add(this);
    }

    public static WhatsappStore findStoreById(UUID uuid){
        return instances.parallelStream()
                .filter(entry -> Objects.equals(entry.uuid(), uuid))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing session: %s".formatted(uuid)));
    }

    /**
     * Queries the first contact whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Contact> findContactByJid(@NonNull String jid) {
        return contacts.parallelStream()
                .filter(contact -> Objects.equals(contact.id().toString(), ContactId.parseId(jid)))
                .findAny();
    }

    /**
     * Queries the first contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Contact> findContactByName(@NonNull String name) {
        return contacts.parallelStream()
                .filter(contact -> Objects.equals(contact.bestName(null), name))
                .findAny();
    }

    /**
     * Queries every contact whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NonNull Set<Contact> findContactsByName(@NonNull String name) {
        return contacts.parallelStream()
                .filter(contact -> Objects.equals(contact.bestName(null), name))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first chat whose jid is equal to {@code jid}
     *
     * @param jid the jid to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByJid(@NonNull String jid) {
        return chats.parallelStream()
                .filter(chat -> Objects.equals(chat.id().toString(), ContactId.parseId(jid)))
                .findAny();
    }

    /**
     * Queries the message in {@code chat} whose jid is equal to {@code jid}
     *
     * @param chat the chat to search in
     * @param id   the jid to search
     * @return a non-empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NonNull Optional<MessageInfo> findMessageById(@NonNull Chat chat, @NonNull String id) {
        return chat.messages()
                .parallelStream()
                .filter(message -> Objects.equals(message.key().id(), id))
                .findAny();
    }

    /**
     * Queries the chat associated with {@code message}
     *
     * @param message the message to use as context
     * @return a non-empty Optional containing the result if it is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByMessage(@NonNull MessageInfo message) {
        return findChatByJid(message.key().chatJid());
    }

    /**
     * Queries the first chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Chat> findChatByName(@NonNull String name) {
        return chats.parallelStream()
                .filter(chat -> Objects.equals(chat.displayName(), name))
                .findAny();
    }

    /**
     * Queries every chat whose name is equal to {@code name}
     *
     * @param name the name to search
     * @return a Set containing every result
     */
    public @NonNull Set<Chat> findChatsByName(@NonNull String name) {
        return chats.parallelStream()
                .filter(chat -> Objects.equals(chat.displayName(), name))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the first request whose id is equal to {@code id}
     *
     * @param id the id to search, can be null
     * @return a non-empty Optional containing the first result if any is found otherwise an empty Optional empty
     */
    public @NonNull Optional<Request> findPendingRequest(String id) {
        return id == null ? Optional.empty() : pendingRequests.parallelStream()
                .filter(request -> Objects.equals(request.id(), id))
                .findAny();
    }

    /**
     * Queries the first request whose id equals the one stored by the response and, if any is found, it completes it
     *
     * @param response the response to complete the request with
     * @return true if any request matching {@code response} is found
     */
    public boolean resolvePendingRequest(@NonNull Node response, boolean exceptionally) {
        return findPendingRequest(WhatsappUtils.readNullableId(response))
                .map(request -> deleteAndComplete(response, request, exceptionally))
                .isPresent();
    }

    /**
     * Adds a chat in memory
     *
     * @param chat the chat to add
     * @return the input chat
     */
    public @NonNull Chat addChat(@NonNull Chat chat) {
        chats.add(chat);
        return chat;
    }

    /**
     * Returns the chats pinned to the top
     *
     * @return a non null list of chats
     */
    public List<Chat> pinnedChats(){
        return chats.parallelStream()
                .filter(Chat::isPinned)
                .toList();
    }

    /**
     * Resets to zero both the read and write counter
     */
    public void clearCounters(){
        this.readCounter.set(0);
        this.writeCounter.set(0);
    }

    /**
     * Clears all the data that this object holds
     */
    public void clear() {
        chats.clear();
        contacts.clear();
        pendingRequests.clear();
        clearCounters();
    }

    /**
     * Executes an operation on every registered listener on the listener thread
     * This should be used to be sure that when a listener should be called it's called on a thread that is not the WebSocket's.
     * If this condition isn't met, if the thread is put on hold to wait for a response for a pending request, the WebSocket will freeze.
     *
     * @param consumer the operation to execute
     */
    public void callListeners(@NonNull Consumer<WhatsappListener> consumer){
        listeners.forEach(listener ->
                requestsService.execute(() -> consumer.accept(listener)));
    }

    private Request deleteAndComplete(Node response, Request request, boolean exceptionally) {
        pendingRequests.remove(request);
        request.complete(response, exceptionally);
        return request;
    }
}
