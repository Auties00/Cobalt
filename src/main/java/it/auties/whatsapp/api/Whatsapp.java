package it.auties.whatsapp.api;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.listener.*;
import it.auties.whatsapp.model.action.MarkChatAsReadAction;
import it.auties.whatsapp.model.action.MuteAction;
import it.auties.whatsapp.model.action.PinAction;
import it.auties.whatsapp.model.action.StarAction;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.response.HasWhatsappResponse;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.model.sync.ActionMessageRangeSync;
import it.auties.whatsapp.model.sync.ActionValueSync;
import it.auties.whatsapp.model.sync.PatchRequest;
import it.auties.whatsapp.util.*;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static it.auties.bytes.Bytes.ofRandom;
import static it.auties.whatsapp.api.Whatsapp.Options.defaultOptions;
import static it.auties.whatsapp.binary.Sync.REGULAR_HIGH;
import static it.auties.whatsapp.binary.Sync.REGULAR_LOW;
import static it.auties.whatsapp.controller.Controller.knownIds;
import static it.auties.whatsapp.model.request.Node.*;
import static it.auties.whatsapp.model.sync.RecordSync.Operation.SET;
import static java.util.Map.of;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket.
 * It provides various functionalities, including the possibility to query, set and modify data associated with the loaded session of whatsapp.
 * It can be configured using a default configuration or a custom one.
 * Multiple instances of this class can be initialized, though it is not advisable as; is a singleton and cannot distinguish between the data associated with each session.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Whatsapp {
    /**
     * Constant for unlimited listeners size
     */
    private static final int UNLIMITED_LISTENERS = -1;

    /**
     * The socket associated with this session
     */
    private final Socket socket;

    /**
     * Listeners limit
     */
    private int listenersLimit;

    /**
     * Default serialization listener
     */
    private Listener defaultSerializer;

    private Whatsapp(Options options) {
        this(options, Store.of(options.id()), Keys.of(options.id()));
    }

    private Whatsapp(Options options, Store store, Keys keys) {
        this.socket = new Socket(this, options, store, keys);
        this.listenersLimit = UNLIMITED_LISTENERS;
        this.defaultSerializer = new BlockingDefaultSerializer();
        addListener(defaultSerializer);
        if (!options.autodetectListeners()) {
            return;
        }

        ListenerScanner.scan(this)
                .forEach(this::addListener);
    }

    /**
     * Constructs a new instance of the API.
     * If the id is not associated with any session, a new one will be created.
     *
     * @param id the id of the session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(int id) {
        return newConnection(Options.defaultOptions()
                .withId(id));
    }

    /**
     * Constructs a new instance of the API
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection() {
        return newConnection(defaultOptions());
    }

    /**
     * Constructs a new instance of the API.
     * If the id is not associated with any session, a new one will be created.
     *
     * @param options the non-null options used to create this session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(@NonNull Options options) {
        return new Whatsapp(options);
    }

    /**
     * Constructs a new instance of the API from a fresh connection using a random id
     *
     * @param options the non-null options used to create this session
     * @param store   the non-null store used to create this session
     * @param keys    the non-null keys used to create this session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(@NonNull Options options, @NonNull Store store, @NonNull Keys keys) {
        return new Whatsapp(options, store, keys);
    }

    /**
     * Constructs a new instance of the API from the first session opened.
     * If no sessions are available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp firstConnection() {
        return newConnection(requireNonNullElseGet(knownIds().peekFirst(), KeyHelper::registrationId));
    }

    /**
     * Constructs a new instance of the API from the last session opened.
     * If no sessions are available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp lastConnection() {
        return newConnection(requireNonNullElseGet(knownIds().peekLast(), KeyHelper::registrationId));
    }

    /**
     * Returns a list of all known connections
     *
     * @return a non-null List
     */
    public static List<Whatsapp> listConnections() {
        return streamConnections().toList();
    }

    /**
     * Returns a stream of all known connections
     *
     * @return a non-null Stream
     */
    public static Stream<Whatsapp> streamConnections() {
        return knownIds().stream()
                .map(Whatsapp::newConnection);
    }

    private static Optional<ContactStatusResponse> parseStatus(List<Node> response) {
        return Nodes.findFirst(response, "status")
                .map(ContactStatusResponse::new);
    }

    private static String parseInviteCode(Node result) {
        return result.findNode("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite code in invite response"))
                .attributes()
                .getRequiredString("code");
    }

    /**
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public Store store() {
        return socket.store();
    }

    /**
     * Returns the keys associated with this session
     *
     * @return a non-null WhatsappKeys
     */
    public Keys keys() {
        return socket.keys();
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Whatsapp addListener(@NonNull Listener listener) {
        Validate.isTrue(listenersLimit < 0 || store().listeners()
                        .size() + 1 <= listenersLimit, "The number of listeners is too high: expected %s, got %s",
                listenersLimit, socket.store()
                        .listeners()
                        .size());
        Validate.isTrue(socket.store()
                .listeners()
                .add(listener), "WhatsappAPI: Cannot add listener %s", listener.getClass()
                .getName());
        return this;
    }

    /**
     * Limits the number of listeners that this connection can have.
     * This limit is enforced as soon as the method is called and for future modifications to the listeners.
     *
     * @param size the maximum number of listeners
     * @return the same instance
     * @throws IllegalStateException if the number of listeners is already too high
     */
    public Whatsapp limitListeners(int size) {
        this.listenersLimit = size;
        Validate.isTrue(store().listeners()
                        .size() <= size, "The number of listeners is too high: expected %s, got %s", listenersLimit,
                store().listeners()
                        .size(), IllegalStateException.class);
        return this;
    }

    /**
     * Registers an action listener
     *
     * @param onAction the listener to register
     * @return the same instance
     */
    public Whatsapp addActionListener(OnAction onAction) {
        return addListener(onAction);
    }

    /**
     * Registers a chat recent messages listener
     *
     * @param onChatRecentMessages the listener to register
     * @return the same instance
     */
    public Whatsapp addChatRecentMessagesListener(OnChatRecentMessages onChatRecentMessages) {
        return addListener(onChatRecentMessages);
    }

    /**
     * Registers a chats listener
     *
     * @param onChats the listener to register
     * @return the same instance
     */
    public Whatsapp addChatsListener(OnChats onChats) {
        return addListener(onChats);
    }

    /**
     * Registers a contact presence listener
     *
     * @param onContactPresence the listener to register
     * @return the same instance
     */
    public Whatsapp addContactPresenceListener(OnContactPresence onContactPresence) {
        return addListener(onContactPresence);
    }

    /**
     * Registers a contacts listener
     *
     * @param onContacts the listener to register
     * @return the same instance
     */
    public Whatsapp addContactsListener(OnContacts onContacts) {
        return addListener(onContacts);
    }

    /**
     * Registers a message status listener
     *
     * @param onConversationMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageStatusListener(OnConversationMessageStatus onConversationMessageStatus) {
        return addListener(onConversationMessageStatus);
    }

    /**
     * Registers a message status listener
     *
     * @param onMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageStatusListener(OnMessageStatus onMessageStatus) {
        return addListener(onMessageStatus);
    }

    /**
     * Registers a disconnected listener
     *
     * @param onDisconnected the listener to register
     * @return the same instance
     */
    public Whatsapp addDisconnectedListener(OnDisconnected onDisconnected) {
        return addListener(onDisconnected);
    }

    /**
     * Registers a features listener
     *
     * @param onFeatures the listener to register
     * @return the same instance
     */
    public Whatsapp addFeaturesListener(OnFeatures onFeatures) {
        return addListener(onFeatures);
    }

    /**
     * Registers a logged in listener
     *
     * @param onLoggedIn the listener to register
     * @return the same instance
     */
    public Whatsapp addLoggedInListener(OnLoggedIn onLoggedIn) {
        return addListener(onLoggedIn);
    }

    /**
     * Registers a message deleted listener
     *
     * @param onMessageDeleted the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageDeletedListener(OnMessageDeleted onMessageDeleted) {
        return addListener(onMessageDeleted);
    }

    /**
     * Registers a metadata listener
     *
     * @param onMetadata the listener to register
     * @return the same instance
     */
    public Whatsapp addMetadataListener(OnMetadata onMetadata) {
        return addListener(onMetadata);
    }

    /**
     * Registers a new contact listener
     *
     * @param onNewContact the listener to register
     * @return the same instance
     */
    public Whatsapp addNewContactListener(OnNewContact onNewContact) {
        return addListener(onNewContact);
    }

    /**
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewMessageListener(OnNewMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new status listener
     *
     * @param onNewStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addNewStatusListener(OnNewStatus onNewStatus) {
        return addListener(onNewStatus);
    }

    /**
     * Registers a received node listener
     *
     * @param onNodeReceived the listener to register
     * @return the same instance
     */
    public Whatsapp addNodeReceivedListener(OnNodeReceived onNodeReceived) {
        return addListener(onNodeReceived);
    }

    /**
     * Registers a sent node listener
     *
     * @param onNodeSent the listener to register
     * @return the same instance
     */
    public Whatsapp addNodeSentListener(OnNodeSent onNodeSent) {
        return addListener(onNodeSent);
    }

    /**
     * Registers a qr code listener
     *
     * @param onQrCode the listener to register
     * @return the same instance
     */
    public Whatsapp addQrCodeListener(OnQrCode onQrCode) {
        return addListener(onQrCode);
    }

    /**
     * Registers a setting listener
     *
     * @param onSetting the listener to register
     * @return the same instance
     */
    public Whatsapp addSettingListener(OnSetting onSetting) {
        return addListener(onSetting);
    }

    /**
     * Registers a status listener
     *
     * @param onStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addStatusListener(OnStatus onStatus) {
        return addListener(onStatus);
    }

    /**
     * Registers an event listener
     *
     * @param onSocketEvent the listener to register
     * @return the same instance
     */
    public Whatsapp addSocketEventListener(OnSocketEvent onSocketEvent) {
        return addListener(onSocketEvent);
    }

    /**
     * Registers an action listener
     *
     * @param onAction the listener to register
     * @return the same instance
     */
    public Whatsapp addActionListener(OnWhatsappAction onAction) {
        return addListener(onAction);
    }

    /**
     * Registers a chat recent messages listener
     *
     * @param onChatRecentMessages the listener to register
     * @return the same instance
     */
    public Whatsapp addChatRecentMessagesListener(OnWhatsappChatRecentMessages onChatRecentMessages) {
        return addListener(onChatRecentMessages);
    }

    /**
     * Registers a chats listener
     *
     * @param onChats the listener to register
     * @return the same instance
     */
    public Whatsapp addChatsListener(OnWhatsappChats onChats) {
        return addListener(onChats);
    }

    /**
     * Registers a contact presence listener
     *
     * @param onContactPresence the listener to register
     * @return the same instance
     */
    public Whatsapp addContactPresenceListener(OnWhatsappContactPresence onContactPresence) {
        return addListener(onContactPresence);
    }

    /**
     * Registers a contacts listener
     *
     * @param onContacts the listener to register
     * @return the same instance
     */
    public Whatsapp addContactsListener(OnWhatsappContacts onContacts) {
        return addListener(onContacts);
    }

    /**
     * Registers a message status listener
     *
     * @param onConversationMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageStatusListener(OnWhatsappConversationMessageStatus onConversationMessageStatus) {
        return addListener(onConversationMessageStatus);
    }

    /**
     * Registers a message status listener
     *
     * @param onMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageStatusListener(OnWhatsappMessageStatus onMessageStatus) {
        return addListener(onMessageStatus);
    }

    /**
     * Registers a disconnected listener
     *
     * @param onDisconnected the listener to register
     * @return the same instance
     */
    public Whatsapp addDisconnectedListener(OnWhatsappDisconnected onDisconnected) {
        return addListener(onDisconnected);
    }

    /**
     * Registers a features listener
     *
     * @param onFeatures the listener to register
     * @return the same instance
     */
    public Whatsapp addFeaturesListener(OnWhatsappFeatures onFeatures) {
        return addListener(onFeatures);
    }

    /**
     * Registers a logged in listener
     *
     * @param onLoggedIn the listener to register
     * @return the same instance
     */
    public Whatsapp addLoggedInListener(OnWhatsappLoggedIn onLoggedIn) {
        return addListener(onLoggedIn);
    }

    /**
     * Registers a message deleted listener
     *
     * @param onMessageDeleted the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageDeletedListener(OnWhatsappMessageDeleted onMessageDeleted) {
        return addListener(onMessageDeleted);
    }

    /**
     * Registers a metadata listener
     *
     * @param onMetadata the listener to register
     * @return the same instance
     */
    public Whatsapp addMetadataListener(OnWhatsappMetadata onMetadata) {
        return addListener(onMetadata);
    }

    /**
     * Registers a new contact listener
     *
     * @param onNewContact the listener to register
     * @return the same instance
     */
    public Whatsapp addNewContactListener(OnWhatsappNewContact onNewContact) {
        return addListener(onNewContact);
    }

    /**
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewMessageListener(OnWhatsappNewMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new status listener
     *
     * @param onNewStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addNewStatusListener(OnWhatsappNewStatus onNewStatus) {
        return addListener(onNewStatus);
    }

    /**
     * Registers a received node listener
     *
     * @param onNodeReceived the listener to register
     * @return the same instance
     */
    public Whatsapp addNodeReceivedListener(OnWhatsappNodeReceived onNodeReceived) {
        return addListener(onNodeReceived);
    }

    /**
     * Registers a sent node listener
     *
     * @param onNodeSent the listener to register
     * @return the same instance
     */
    public Whatsapp addNodeSentListener(OnWhatsappNodeSent onNodeSent) {
        return addListener(onNodeSent);
    }

    /**
     * Registers a setting listener
     *
     * @param onSetting the listener to register
     * @return the same instance
     */
    public Whatsapp addSettingListener(OnWhatsappSetting onSetting) {
        return addListener(onSetting);
    }

    /**
     * Registers a status listener
     *
     * @param onStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addStatusListener(OnWhatsappStatus onStatus) {
        return addListener(onStatus);
    }

    /**
     * Registers an event listener
     *
     * @param onSocketEvent the listener to register
     * @return the same instance
     */
    public Whatsapp addSocketEventListener(OnWhatsappSocketEvent onSocketEvent) {
        return addListener(onSocketEvent);
    }

    /**
     * Registers an event listener
     *
     * @param onSocketEvent the listener to register
     * @return the same instance
     */
    public Whatsapp addSerialization(OnWhatsappSocketEvent onSocketEvent) {
        return addListener(onSocketEvent);
    }

    /**
     * Uses the default serialization mechanism.
     * Calling this method is not necessary unless {@link Whatsapp#withoutDefaultSerialization()} has been called previously.
     *
     * @return the same instance
     */
    public Whatsapp withDefaultSerialization() {
        if (defaultSerializer != null) {
            return this;
        }

        this.defaultSerializer = new BlockingDefaultSerializer();
        addListener(defaultSerializer);
        return this;
    }

    /**
     * Disables the default serialization mechanism.
     *
     * @return the same instance
     */
    public Whatsapp withoutDefaultSerialization() {
        if (defaultSerializer == null) {
            return this;
        }

        removeListener(defaultSerializer);
        this.defaultSerializer = null;
        return this;
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove
     * @return the same instance
     */
    public Whatsapp removeListener(@NonNull Listener listener) {
        Validate.isTrue(socket.store()
                .listeners()
                .remove(listener), "WhatsappAPI: Cannot remove listener %s", listener.getClass()
                .getName());
        return this;
    }

    /**
     * Waits for the socket to be closed on the current thread
     *
     * @return the same instance
     */
    public Whatsapp await() {
        socket.await();
        return this;
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> connect() {
        return socket.connect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> disconnect() {
        return socket.disconnect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> reconnect() {
        return socket.reconnect()
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous saved credentials.
     * The next time the API is used, the QR code will need to be scanned again.
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> logout() {
        if (keys().hasCompanion()) {
            var metadata = of("jid", keys().companion(), "reason", "user_initiated");
            var device = withAttributes("remove-companion-device", metadata);
            return socket.sendQuery("set", "md", device)
                    .thenRunAsync(socket::changeKeys)
                    .thenApplyAsync(ignored -> this);
        }

        return disconnect().thenRunAsync(socket::changeKeys)
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the endTimeStamp the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> subscribeToPresence(@NonNull ContactJidProvider jid) {
        var node = withAttributes("presence", of("to", jid.toJid(), "type", "subscribe"));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull String message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the quoted message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull String message,
                                                      @NonNull MessageInfo quotedMessage) {
        return sendMessage(chat, TextMessage.of(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat, a message and a quoted message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the quoted message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat,
                                                      @NonNull ContextualMessage message,
                                                      @NonNull MessageInfo quotedMessage) {
        Validate.isTrue(!quotedMessage.message()
                .isEmpty(), "Cannot quote an empty message");
        Validate.isTrue(!quotedMessage.message()
                .isServer(), "Cannot quote a server message");
        var context = ContextInfo.newContextInfo()
                .quotedMessageSender(quotedMessage.fromMe() ?
                        keys().companion() :
                        quotedMessage.senderJid())
                .quotedMessageId(quotedMessage.id())
                .quotedMessage(quotedMessage.message());
        if (!Objects.equals(quotedMessage.senderJid(), quotedMessage.chatJid())) {
            context.quotedMessageChat(quotedMessage.chatJid());
        }

        return sendMessage(chat, message, context.create());
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat,
                                                      @NonNull MessageContainer message) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.toJid())
                .fromMe(true)
                .senderJid(chat.toJid()
                        .isGroup() ?
                        keys().companion() :
                        null)
                .create();
        var info = MessageInfo.newMessageInfo()
                .storeId(store().id())
                .senderJid(chat.toJid()
                        .isGroup() ?
                        keys().companion() :
                        null)
                .key(key)
                .message(message)
                .create();
        return sendMessage(info);
    }

    /**
     * Builds and sends a message from a chat, a message and a context
     *
     * @param chat        the chat where the message should be sent
     * @param message     the message to send
     * @param contextInfo the context of the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat,
                                                      @NonNull ContextualMessage message,
                                                      @NonNull ContextInfo contextInfo) {
        var key = MessageKey.newMessageKey()
                .chatJid(chat.toJid())
                .fromMe(true)
                .senderJid(chat.toJid()
                        .isGroup() ?
                        keys().companion() :
                        null)
                .create();
        var info = MessageInfo.newMessageInfo()
                .storeId(store().id())
                .senderJid(chat.toJid()
                        .isGroup() ?
                        keys().companion() :
                        null)
                .key(key)
                .message(MessageContainer.of(message.contextInfo(contextInfo)))
                .timestamp(Clock.now())
                .create();
        return sendMessage(info);
    }

    /**
     * Sends a message info to a chat
     *
     * @param info the info to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull MessageInfo info) {
        info.key().chatJid(info.chatJid().toUserJid());
        if (info.message().content() instanceof MediaMessage mediaMessage) {
            mediaMessage.storeId(store().id());
        }

        parseEphemeralMessage(info);
        return socket.sendMessage(info)
                .thenApplyAsync(ignored -> info);
    }

    private void parseEphemeralMessage(MessageInfo info) {
        info.chat()
                .filter(Chat::isEphemeral)
                .ifPresent(chat -> createEphemeralMessage(info, chat));
    }

    private void createEphemeralMessage(MessageInfo info, Chat chat) {
        info.message()
                .contentWithContext()
                .map(ContextualMessage::contextInfo)
                .ifPresent(contextInfo -> createEphemeralContext(chat, contextInfo));
        info.message(info.message().toEphemeral());
    }

    private void createEphemeralContext(Chat chat, ContextInfo contextInfo) {
        var period = chat.ephemeralMessageDuration()
                .period()
                .toSeconds();
        contextInfo.ephemeralExpiration((int) period);
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param chats the users to check
     * @return a CompletableFuture that wraps a non-null list of HasWhatsappResponse
     */
    public CompletableFuture<List<HasWhatsappResponse>> hasWhatsapp(@NonNull ContactJidProvider @NonNull ... chats) {
        var contactNodes = Arrays.stream(chats)
                .map(jid -> with("contact", "+%s".formatted(jid.toJid()
                        .user())))
                .toArray(Node[]::new);
        return socket.sendInteractiveQuery(with("contact"), withChildren("user", contactNodes))
                .thenApplyAsync(nodes -> nodes.stream()
                        .map(HasWhatsappResponse::new)
                        .toList());
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture that wraps a non-null list of ContactJid
     */
    public CompletableFuture<List<ContactJid>> queryBlockList() {
        return socket.sendQuery("get", "blocklist", (Node) null)
                .thenApplyAsync(this::parseBlockList);
    }

    private List<ContactJid> parseBlockList(Node result) {
        return result.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing block list in response"))
                .findNodes("item")
                .stream()
                .map(item -> item.attributes()
                        .getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status response
     */
    public CompletableFuture<Optional<ContactStatusResponse>> queryStatus(@NonNull ContactJidProvider chat) {
        var query = with("status");
        var body = withAttributes("user", of("jid", chat.toJid()));
        return socket.sendInteractiveQuery(query, body)
                .thenApplyAsync(Whatsapp::parseStatus);
    }

    /**
     * Queries the profile picture of a chat.
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<Optional<URI>> queryPic(@NonNull ContactJidProvider chat) {
        var body = withAttributes("picture", of("query", "url"));
        return socket.sendQuery("get", "w:profile:picture", of("target", chat.toJid()), body)
                .thenApplyAsync(this::parseChatPicture);
    }

    private Optional<URI> parseChatPicture(Node result) {
        return result.findNode("picture")
                .flatMap(picture -> picture.attributes()
                        .getOptionalString("url"))
                .map(URI::create);
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> queryGroupMetadata(@NonNull ContactJidProvider chat) {
        return socket.queryGroupMetadata(chat.toJid());
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<String> queryInviteCode(@NonNull ContactJidProvider chat) {
        return socket.sendQuery(chat.toJid(), "get", "w:g2", with("invite"))
                .thenApplyAsync(Whatsapp::parseInviteCode);
    }

    /**
     * Revokes the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> revokeInviteCode(@NonNull ContactJidProvider chat) {
        return socket.sendQuery(chat.toJid(), "set", "w:g2", with("invite"))
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite code
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<Chat>> acceptInvite(@NonNull String inviteCode) {
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", withAttributes("invite", of("code", inviteCode)))
                .thenApplyAsync(this::parseAcceptInvite);
    }

    private Optional<Chat> parseAcceptInvite(Node result) {
        return result.findNode("group")
                .flatMap(group -> group.attributes()
                        .getJid("jid"))
                .map(jid -> store().findChatByJid(jid)
                        .orElseGet(() -> socket.createChat(jid)));
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePresence(boolean available) {
        var presence = available ?
                ContactStatus.AVAILABLE :
                ContactStatus.UNAVAILABLE;
        var node = withAttributes("presence", of("type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePresence(@NonNull ContactJidProvider chat, @NonNull ContactStatus presence) {
        var node = withAttributes("presence", of("to", chat.toJid(), "type", presence.data()));
        return socket.sendWithNoResponse(node);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> promote(@NonNull ContactJidProvider group,
                                                       @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, contacts);
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> demote(@NonNull ContactJidProvider group,
                                                      @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, contacts);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> add(@NonNull ContactJidProvider group,
                                                   @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, contacts);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> remove(@NonNull ContactJidProvider group,
                                                      @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.REMOVE, contacts);
    }

    private CompletableFuture<List<ContactJid>> executeActionOnGroupParticipant(ContactJidProvider group,
                                                                                GroupAction action,
                                                                                ContactJidProvider... jids) {
        var body = Arrays.stream(jids)
                .map(ContactJidProvider::toJid)
                .map(jid -> withAttributes("participant", of("jid", checkGroupParticipantJid(jid))))
                .map(innerBody -> withChildren(action.data(), innerBody))
                .toArray(Node[]::new);
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(result -> parseGroupActionResponse(result, action));
    }

    private List<ContactJid> parseGroupActionResponse(Node result, GroupAction action) {
        return result.findNode(action.data())
                .orElseThrow(() -> new NoSuchElementException("An erroneous group operation was executed"))
                .findNodes("participant")
                .stream()
                .filter(participant -> !participant.attributes()
                        .hasKey("error"))
                .map(participant -> participant.attributes()
                        .getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    private ContactJid checkGroupParticipantJid(ContactJid jid) {
        Validate.isTrue(!Objects.equals(jid.toUserJid(), keys().companion()
                .toUserJid()), "Cannot execute action on yourself");
        return jid;
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public CompletableFuture<Void> changeSubject(@NonNull ContactJidProvider group, @NonNull String newName) {
        var body = with("subject", newName.getBytes(StandardCharsets.UTF_8));
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Changes the description of a group
     *
     * @param group       the target group
     * @param description the new name for the group, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeDescription(@NonNull ContactJidProvider group, String description) {
        return socket.queryGroupMetadata(group.toJid())
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeDescription(group, description, descriptionId))
                .thenAcceptAsync(ignored -> {
                });
    }

    private CompletableFuture<Node> changeDescription(ContactJidProvider group, String description,
                                                      String descriptionId) {
        var descriptionNode = Optional.ofNullable(description)
                .map(content -> with("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var attributes = Attributes.empty()
                .put("id", MessageKey.randomId(), () -> description != null)
                .put("delete", true, () -> description == null)
                .put("prev", descriptionId, () -> descriptionId != null)
                .map();
        var body = withChildren("description", attributes, descriptionNode);
        return socket.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeWhoCanSendMessages(@NonNull ContactJidProvider group,
                                                            @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ?
                "not_announcement" :
                "announcement");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeWhoCanEditInfo(@NonNull ContactJidProvider group,
                                                        @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ?
                "locked" :
                "unlocked");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePicture(byte[] image) {
        return changePicture(keys().companion()
                .toUserJid(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePicture(@NonNull ContactJidProvider group, byte[] image) {
        var profilePic = image != null ?
                Medias.getProfilePic(image) :
                null;
        var body = with("picture", of("type", "image"), profilePic);
        return socket.sendQuery(group.toJid(), "set", "w:profile:picture", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Creates a new group with the provided name and with at least one contact
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> create(@NonNull String subject, @NonNull ContactJidProvider... contacts) {
        var participants = Arrays.stream(contacts)
                .map(contact -> withAttributes("participant", of("jid", contact.toJid())))
                .toArray(Node[]::new);
        var key = ofRandom(12).toHex();
        var body = withChildren("create", of("subject", subject, "key", key), participants);
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", body)
                .thenApplyAsync(response -> response.findNode("group")
                        .orElseThrow(() -> new NoSuchElementException("Missing group response, something went wrong")))
                .thenApplyAsync(GroupMetadata::of);
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<Void> leave(@NonNull ContactJidProvider group) {
        var body = withChildren("leave", withAttributes("group", of("id", group.toJid())));
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> mute(@NonNull ContactJidProvider chat) {
        return mute(chat, (Long) null);
    }

    /**
     * Mutes a chat until a specific date
     *
     * @param chat  the target chat
     * @param until the date the mute ends, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> mute(@NonNull ContactJidProvider chat, ZonedDateTime until) {
        return mute(chat, until != null ?
                until.toEpochSecond() :
                null);
    }

    /**
     * Mutes a chat until a specific date expressed in seconds since the epoch
     *
     * @param chat           the target chat
     * @param untilInSeconds the date the mute ends expressed in seconds since the epoch, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> mute(@NonNull ContactJidProvider chat, Long untilInSeconds) {
        var muteAction = new MuteAction(true, untilInSeconds);
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid()
                .toString());
        return socket.push(request);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unmute(@NonNull ContactJidProvider chat) {
        var muteAction = new MuteAction(false, null);
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid()
                .toString());
        return socket.push(request);
    }

    /**
     * Blocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> block(@NonNull ContactJidProvider chat) {
        var body = withAttributes("item", of("action", "block", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body)
                .thenAcceptAsync(ignored -> {
                });
    }

    /**
     * Unblocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unblock(@NonNull ContactJidProvider chat) {
        var body = withAttributes("item", of("action", "unblock", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body)
                .thenAcceptAsync(ignored -> {
                });
    }


    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeEphemeralTimer(@NonNull ContactJidProvider chat,
                                                        @NonNull ChatEphemeralTimer timer) {
        return switch (chat.toJid()
                .server()) {
            case USER, WHATSAPP -> {
                var message = ProtocolMessage.newProtocolMessage()
                        .type(ProtocolMessage.ProtocolMessageType.EPHEMERAL_SETTING)
                        .ephemeralExpiration(timer.period()
                                .toSeconds())
                        .create();
                yield sendMessage(chat, message).thenAcceptAsync(ignored -> {
                });
            }

            case GROUP -> {
                var body = timer == ChatEphemeralTimer.OFF ?
                        with("not_ephemeral") :
                        withAttributes("ephemeral", of("expiration", timer.period()
                                .toSeconds()));
                yield socket.sendQuery(chat.toJid(), "set", "w:g2", body)
                        .thenAcceptAsync(ignored -> {
                        });
            }

            default -> throw new IllegalArgumentException(
                    "Unexpected chat %s: ephemeral messages are only supported for conversations and groups".formatted(
                            chat.toJid()));
        };
    }

    /**
     * Marks a chat as read
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> markAsRead(@NonNull ContactJidProvider chat) {
        return markAs(chat, true);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> markAsUnread(@NonNull ContactJidProvider chat) {
        return markAs(chat, false);
    }

    private CompletableFuture<Void> markAs(@NonNull ContactJidProvider chat, boolean read) {
        var range = createLastMessageRange(chat);
        var muteAction = new MarkChatAsReadAction(read, range);
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid()
                .toString());
        return socket.push(request);
    }

    /**
     * Pins a chat to the top.
     * A maximum of three chats can be pinned to the top.
     * This condition can be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> pin(@NonNull ContactJidProvider chat) {
        return pin(chat, true);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unpin(@NonNull ContactJidProvider chat) {
        return pin(chat, false);
    }

    private CompletableFuture<Void> pin(ContactJidProvider chat, boolean pin) {
        var pinAction = new PinAction(pin);
        var syncAction = new ActionValueSync(pinAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 5, chat.toJid()
                .toString());
        return socket.push(request);
    }

    /**
     * Stars a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> star(@NonNull MessageInfo info) {
        return star(info, true);
    }

    /**
     * Removes star from a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unstar(@NonNull MessageInfo info) {
        return star(info, false);
    }

    private CompletableFuture<Void> star(MessageInfo info, boolean star) {
        var starAction = new StarAction(star);
        var syncAction = new ActionValueSync(starAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 3, info.chatJid()
                .toString(), info.id(), String.valueOf(info.fromMe() ?
                1 :
                0), info.chatJid()
                .server() == ContactJid.Server.GROUP && !info.fromMe() ?
                info.senderJid()
                        .toString() :
                "0");
        return socket.push(request);
    }

    /**
     * Archives a chat.
     * If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> archive(@NonNull ContactJidProvider chat) {
        return archive(chat, true);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unarchive(@NonNull ContactJidProvider chat) {
        return archive(chat, false);
    }

    private CompletableFuture<Void> archive(ContactJidProvider chat, boolean archive) {
        var range = createLastMessageRange(chat);
        var muteAction = new MarkChatAsReadAction(archive, range);
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid()
                .toString());
        return socket.push(request);
    }

    private ActionMessageRangeSync createLastMessageRange(ContactJidProvider chat) {
        return store().findChatByJid(chat.toJid())
                .flatMap(Chat::lastMessage)
                .map(ActionMessageRangeSync::new)
                .orElseGet(() -> new ActionMessageRangeSync(null, null, null));
    }

    /**
     * A configuration class used to specify the behaviour of {@link Whatsapp}
     */
    @Builder(builderMethodName = "newOptions", buildMethodName = "create")
    @With
    @Data
    @Accessors(fluent = true)
    public static class Options {
        /**
         * Last known version of Whatsapp
         */
        private static final Version WHATSAPP_VERSION = new Version(2, 2212, 7);

        /**
         * The id of the session.
         * This id needs to be unique.
         * By default, a random integer.
         */
        @Default
        private final int id = KeyHelper.registrationId();

        /**
         * Whether listeners marked with @RegisteredListener should be registered automatically.
         * By default, this option is enabled.
         */
        @Default
        private final boolean autodetectListeners = true;

        /**
         * The version of WhatsappWeb to use.
         * If the version is too outdated, the server will refuse to connect.
         */
        @Default
        private final Version version = Version.latest(WHATSAPP_VERSION);

        /**
         * The url of the socket
         */
        @Default
        @NonNull
        private final String url = "wss://web.whatsapp.com/ws/chat";

        /**
         * The description provided to Whatsapp during the authentication process.
         * This should be, for example, the name of your service.
         * By default, it's WhatsappWeb4j.
         */
        @Default
        @NonNull
        private final String description = "WhatsappWeb4j";

        /**
         * Describes how much chat history Whatsapp should send when the QR is first scanned.
         * By default, three months are chosen.
         */
        @Default
        private HistoryLength historyLength = HistoryLength.THREE_MONTHS;

        /**
         * Handles failures in the WebSocket.
         * Returns true if the current connection should be killed and a new one created.
         * Otherwise, the connection will not be killed, but more failures may be caused by the latter.
         * By default, false.
         */
        @Default
        private ErrorHandler errorHandler = ErrorHandler.defaultErrorHandler();

        /**
         * Constructs a new instance of WhatsappConfiguration with default options
         *
         * @return a non-null options configuration
         */
        public static Options defaultOptions() {
            return newOptions().create();
        }
    }

    private class BlockingDefaultSerializer implements OnSocketEvent {
        @Override
        public void onSocketEvent(SocketEvent event) {
            if (event != SocketEvent.CLOSE) {
                return;
            }

            keys().save(false);
            store().save(false);
        }
    }
}
