package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.bytes.Bytes;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.crypto.Hkdf;
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
import it.auties.whatsapp.model.message.standard.PollCreationMessage;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.poll.PollUpdate;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedOptions;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.ReplyHandler;
import it.auties.whatsapp.model.request.Request;
import it.auties.whatsapp.util.Clock;
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
import java.util.concurrent.atomic.AtomicLong;
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
    private String userCompanionName;

    /**
     * The hash of the companion associated with this session
     */
    @Getter
    @Setter
    private String userCompanionDeviceHash;

    /**
     * A map of all the devices that the companion has associated using WhatsappWeb
     * The key here is the index of the device's key
     * The value is the device's companion jid
     */
    @Getter
    @Setter
    @Default
    private LinkedHashMap<ContactJid, Integer> userCompanionDeviceKeyIndexes = new LinkedHashMap<>();

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
     * The non-null map of properties received by whatsapp
     */
    @NonNull
    @Default
    @Getter
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
     * Returns the store saved in memory or constructs a new clean instance
     *
     * @param options the non-null options
     * @return a non-null store
     */
    public static Store of(@NonNull WhatsappOptions options) {
        var deserializer = options.deserializer();
        var result = deserializer.deserializeStore(options.clientType(), options.uuid())
                .map(store -> store.serializer(options.serializer()))
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
        return Store.builder()
                .serializer(options.serializer())
                .clientType(options.clientType())
                .uuid(options.uuid())
                .build();
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
        return chat.messages()
                .parallelStream()
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
     * Queries the first chat that matches the provided function
     *
     * @param function the non-null filter
     * @return a non-null optional
     */
    public Optional<Chat> findChatBy(@NonNull Function<Chat, Boolean> function) {
        return chats.values().stream().filter(function::apply).findFirst();
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
        return chats.values().stream().filter(function::apply).collect(Collectors.toUnmodifiableSet());
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
        var chat = findChatByJid(chatJid).orElseGet(() -> addChat(Chat.ofJid(chatJid)));
        contextInfo.quotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, ContactJid senderJid) {
        var contact = findContactByJid(senderJid).orElseGet(() -> addContact(Contact.ofJid(senderJid)));
        contextInfo.quotedMessageSender(contact);
    }

    private void processMessage(MessageInfo info) {
        switch (info.message().content()) {
            case PollCreationMessage pollCreationMessage -> handlePollCreation(info, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> handlePollUpdate(info, pollUpdateMessage);
            case ReactionMessage reactionMessage -> handleReactionMessage(info, reactionMessage);
            default -> {}
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
        var useSecretPayload = Bytes.of(originalPollInfo.id())
                .append(originalPollSender)
                .append(modificationSender)
                .append(secretName)
                .toByteArray();
        var encryptionKey = originalPollInfo.message()
                .deviceInfo()
                .messageSecret()
                .orElseThrow(() -> new NoSuchElementException("Missing encryption key for poll"));
        var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload, 32);
        var additionalData = "%s\0%s".formatted(
                originalPollInfo.id(),
                modificationSenderJid
        );
        var metadata = pollUpdateMessage.encryptedMetadata();
        var decrypted = AesGmc.decrypt(metadata.iv(), metadata.payload(), useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollVoteMessage = Protobuf.readMessage(decrypted, PollUpdateEncryptedOptions.class);
        var selectedOptions = pollVoteMessage.selectedOptions()
                .stream()
                .map(sha256 -> originalPollMessage.selectableOptionsHashesMap().get(Bytes.of(sha256).toHex()))
                .filter(Objects::nonNull)
                .toList();
        originalPollMessage.selectedOptionsMap().put(modificationSenderJid, selectedOptions);
        pollUpdateMessage.votes(selectedOptions);
        var update = new PollUpdate(info.key(), pollVoteMessage, Clock.nowInMilliseconds());
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
    public Collection<Chat> chats() {
        return chats.values().stream().sorted(Comparator.comparingLong((Chat chat) -> chat.timestampSeconds()).reversed()).toList();
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
     */
    public void addPrivacySetting(@NonNull PrivacySettingType type, @NonNull PrivacySettingEntry entry){
        privacySettings.put(type, entry);
    }

    /**
     * Returns an unmodifiable list that contains the devices associated using Whatsapp web to this session's companion
     *
     * @return an unmodifiable list
     */
    public Collection<ContactJid> userCompanionDevices(){
        return userCompanionDeviceKeyIndexes.keySet();
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
}
