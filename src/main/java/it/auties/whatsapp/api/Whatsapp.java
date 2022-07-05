package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.listener.*;
import it.auties.whatsapp.model.action.*;
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
import it.auties.whatsapp.socket.Socket;
import it.auties.whatsapp.util.*;
import it.auties.linkpreview.LinkPreview;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import static java.util.Objects.requireNonNullElse;
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
     * The socket associated with this session
     */
    private final Socket socket;

    private Whatsapp(Options options) {
        this(options, Store.of(options.id()), Keys.of(options.id()));
    }

    private Whatsapp(Options options, Store store, Keys keys) {
        this.socket = new Socket(this, options, store, keys);
        if (options.defaultSerialization()) {
            addListener(new BlockingDefaultSerializer());
        }

        if (options.autodetectListeners()) {
            ListenerScanner.scan(this)
                    .forEach(this::addListener);
        }
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
     * Constructs a new instance of the API from the first session opened.
     * If no sessions are available, a new one will be created.
     *
     * @param options the non-null options
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp firstConnection(@NonNull Options options) {
        return newConnection(options.withId(requireNonNullElseGet(knownIds().peekFirst(), KeyHelper::registrationId)));
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
     * Constructs a new instance of the API from the last session opened.
     * If no sessions are available, a new one will be created.
     *
     * @param options the non-null options
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp lastConnection(@NonNull Options options) {
        return newConnection(options.withId(requireNonNullElseGet(knownIds().peekLast(), KeyHelper::registrationId)));
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

    /**
     * Deletes all the known connections from memory
     */
    public static void deleteConnections() {
        streamConnections().forEach(Whatsapp::delete);
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
        Validate.isTrue(socket.options()
                .listenersLimit() < 0 || store().listeners()
                .size() + 1 <= socket.options()
                .listenersLimit(), "The number of listeners is too high: expected %s, got %s", socket.options()
                .listenersLimit(), socket.store()
                .listeners()
                .size());
        Validate.isTrue(socket.store()
                .listeners()
                .add(listener), "WhatsappAPI: Cannot add listener %s", listener.getClass()
                .getName());
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
    public Whatsapp addChatMessagesListener(OnChatMessages onChatRecentMessages) {
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
    public Whatsapp addChatMessagesListener(OnWhatsappChatMessages onChatRecentMessages) {
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
     * Deletes the data associated with this session and disconnects from it
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> delete() {
        keys().delete();
        store().delete();
        return disconnect();
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
    public <T extends ContactJidProvider> CompletableFuture<T> subscribeToPresence(@NonNull T jid) {
        var node = withAttributes("presence", of("to", jid.toJid(), "type", "subscribe"));
        return socket.sendWithNoResponse(node)
                .thenApplyAsync(ignored -> jid);
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

        return sendMessage(chat, message, context.build());
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
        message.contextInfo(contextInfo);
        return sendMessage(chat, message);
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
                .build();
        var info = MessageInfo.newMessageInfo()
                .storeId(store().id())
                .senderJid(chat.toJid()
                        .isGroup() ?
                        keys().companion() :
                        null)
                .key(key)
                .message(message)
                .timestamp(Clock.now())
                .build();
        return sendMessage(info);
    }


    /**
     * Sends a message info to a chat
     *
     * @param info the info to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull MessageInfo info) {
        info.key()
                .chatJid(info.chatJid()
                        .toUserJid());
        if (info.message()
                .content() instanceof MediaMessage mediaMessage) {
            mediaMessage.storeId(store().id());
        }

        createTextPreview(info);
        parseEphemeralMessage(info);
        return socket.sendMessage(info)
                .thenApplyAsync(ignored -> info);
    }

    private void createTextPreview(MessageInfo info) {
        if (!(info.message()
                .content() instanceof TextMessage textMessage)) {
            return;
        }

        if (!socket.options()
                .automaticTextPreview()) {
            return;
        }

        var preview = LinkPreview.createPreview(textMessage.text())
                .orElse(null);
        if (preview == null) {
            return;
        }

        var imageUri = preview.images()
                .stream()
                .findFirst()
                .orElse(null);
        var videoUri = preview.videos()
                .stream()
                .findFirst()
                .orElse(null);
        textMessage.canonicalUrl(requireNonNullElse(videoUri, preview.uri()).toString());
        textMessage.matchedText(preview.uri()
                .toString());
        textMessage.thumbnail(Medias.getPreview(imageUri, videoUri)
                .orElse(null));
        textMessage.description(preview.siteDescription());
        textMessage.title(preview.title());
        textMessage.previewType(TextMessage.TextMessagePreviewType.VIDEO);
    }

    private void parseEphemeralMessage(MessageInfo info) {
        if (info.message()
                .isServer()) {
            return;
        }

        info.chat()
                .filter(Chat::isEphemeral)
                .ifPresent(chat -> createEphemeralMessage(info, chat));
    }

    private void createEphemeralMessage(MessageInfo info, Chat chat) {
        info.message()
                .contentWithContext()
                .map(ContextualMessage::contextInfo)
                .ifPresent(contextInfo -> createEphemeralContext(chat, contextInfo));
        info.message(info.message()
                .toEphemeral());
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
    public <T extends ContactJidProvider> CompletableFuture<T> revokeInviteCode(@NonNull T chat) {
        return socket.sendQuery(chat.toJid(), "set", "w:g2", with("invite"))
                .thenApplyAsync(ignored -> chat);
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
    public CompletableFuture<Boolean> changePresence(boolean available) {
        var presence = available ?
                ContactStatus.AVAILABLE :
                ContactStatus.UNAVAILABLE;
        var node = withAttributes("presence", of("type", presence.data()));
        return socket.sendWithNoResponse(node)
                .thenApplyAsync(ignored -> available);
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changePresence(@NonNull T chat,
                                                                              @NonNull ContactStatus presence) {
        var node = withAttributes("presence", of("to", chat.toJid(), "type", presence.data()));
        return socket.sendWithNoResponse(node)
                .thenApplyAsync(ignored -> chat);
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
    public <T extends ContactJidProvider> CompletableFuture<T> changeSubject(@NonNull T group,
                                                                             @NonNull String newName) {
        var body = with("subject", newName.getBytes(StandardCharsets.UTF_8));
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Changes the description of a group
     *
     * @param group       the target group
     * @param description the new name for the group, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeDescription(@NonNull T group, String description) {
        return socket.queryGroupMetadata(group.toJid())
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeDescription(group, description, descriptionId))
                .thenApplyAsync(ignored -> group);
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
    public <T extends ContactJidProvider> CompletableFuture<T> changeWhoCanSendMessages(@NonNull T group,
                                                                                        @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ?
                "not_announcement" :
                "announcement");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeWhoCanEditInfo(@NonNull T group,
                                                                                    @NonNull GroupPolicy policy) {
        var body = with(policy != GroupPolicy.ANYONE ?
                "locked" :
                "unlocked");
        return socket.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<ContactJid> changePicture(byte[] image) {
        return changePicture(keys().companion(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changePicture(@NonNull T group, byte[] image) {
        var profilePic = image != null ?
                Medias.getProfilePic(image) :
                null;
        var body = with("picture", of("type", "image"), profilePic);
        return socket.sendQuery(group.toJid()
                        .toUserJid(), "set", "w:profile:picture", body)
                .thenApplyAsync(ignored -> group);
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
                        .orElseThrow(() -> new NoSuchElementException(
                                "Missing group response, something went wrong: %s".formatted(response.findNode("error")
                                        .map(Node::toString)
                                        .orElse("unknown")))))
                .thenApplyAsync(GroupMetadata::of);
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public <T extends ContactJidProvider> CompletableFuture<T> leave(@NonNull T group) {
        var body = withChildren("leave", withAttributes("group", of("id", group.toJid())));
        return socket.sendQuery(ContactJid.GROUP, "set", "w:g2", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> mute(@NonNull T chat) {
        return mute(chat, ChatMute.muted());
    }

    /**
     * Mutes a chat
     *
     * @param chat the target chat
     * @param mute the type of mute
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> mute(@NonNull T chat, @NonNull ChatMute mute) {
        var muteAction = new MuteAction(true, mute.type() == ChatMute.Type.MUTED_FOR_TIMEFRAME ?
                mute.endTimeStamp() * 1000L :
                mute.endTimeStamp());
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid()
                .toString());
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unmute(@NonNull T chat) {
        var muteAction = new MuteAction(false, null);
        var syncAction = new ActionValueSync(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid()
                .toString());
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Blocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> block(@NonNull T chat) {
        var body = withAttributes("item", of("action", "block", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Unblocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unblock(@NonNull T chat) {
        var body = withAttributes("item", of("action", "unblock", "jid", chat.toJid()));
        return socket.sendQuery("set", "blocklist", body)
                .thenApplyAsync(ignored -> chat);
    }


    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeEphemeralTimer(@NonNull T chat,
                                                                                    @NonNull ChatEphemeralTimer timer) {
        return switch (chat.toJid()
                .server()) {
            case USER, WHATSAPP -> {
                var message = ProtocolMessage.newProtocolMessage()
                        .type(ProtocolMessage.ProtocolMessageType.EPHEMERAL_SETTING)
                        .ephemeralExpiration(timer.period()
                                .toSeconds())
                        .build();
                yield sendMessage(chat, message).thenApplyAsync(ignored -> chat);
            }

            case GROUP -> {
                var body = timer == ChatEphemeralTimer.OFF ?
                        with("not_ephemeral") :
                        withAttributes("ephemeral", of("expiration", timer.period()
                                .toSeconds()));
                yield socket.sendQuery(chat.toJid(), "set", "w:g2", body)
                        .thenApplyAsync(ignored -> chat);
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
    public <T extends ContactJidProvider> CompletableFuture<T> markRead(@NonNull T chat) {
        return mark(chat, true);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> markUnread(@NonNull T chat) {
        return mark(chat, false);
    }

    private <T extends ContactJidProvider> CompletableFuture<T> mark(@NonNull T chat, boolean read) {
        var range = createRange(chat, false);
        var markAction = new MarkChatAsReadAction(read, range);
        var syncAction = new ActionValueSync(markAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid()
                .toString());
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Pins a chat to the top.
     * A maximum of three chats can be pinned to the top.
     * This condition can be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> pin(@NonNull T chat) {
        return pin(chat, true);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unpin(@NonNull T chat) {
        return pin(chat, false);
    }

    private <T extends ContactJidProvider> CompletableFuture<T> pin(T chat, boolean pin) {
        var pinAction = new PinAction(pin);
        var syncAction = new ActionValueSync(pinAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 5, chat.toJid()
                .toString());
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Stars a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> star(@NonNull MessageInfo info) {
        return star(info, true);
    }

    /**
     * Removes star from a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> unstar(@NonNull MessageInfo info) {
        return star(info, false);
    }

    private CompletableFuture<MessageInfo> star(MessageInfo info, boolean star) {
        var starAction = new StarAction(star);
        var syncAction = new ActionValueSync(starAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> info);
    }

    /**
     * Archives a chat.
     * If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> archive(@NonNull T chat) {
        return archive(chat, true);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unarchive(@NonNull T chat) {
        return archive(chat, false);
    }

    private <T extends ContactJidProvider> CompletableFuture<T> archive(T chat, boolean archive) {
        var range = createRange(chat, false);
        var archiveAction = new ArchiveChatAction(archive, range);
        var syncAction = new ActionValueSync(archiveAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid()
                .toString());
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Deletes a message
     *
     * @param info     the non-null message to delete
     * @param everyone whether the message should be deleted for everyone or only for this client and its companions
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> delete(@NonNull MessageInfo info, boolean everyone) {
        if (everyone) {
            var message = ProtocolMessage.newProtocolMessage()
                    .type(ProtocolMessage.ProtocolMessageType.REVOKE)
                    .key(info.key())
                    .build();
            return sendMessage(info.chatJid(), message);
        }

        var range = createRange(info.chatJid(), false);
        var deleteMessageAction = new DeleteMessageForMeAction(false, info.timestamp());
        var syncAction = new ActionValueSync(deleteMessageAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> info);
    }

    /**
     * Deletes a chat for this client and its companions using a modern version of Whatsapp
     *
     * @param chat                the non-null chat to delete
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> delete(@NonNull T chat) {
        var range = createRange(chat.toJid(), true);
        var deleteChatAction = new DeleteChatAction(range);
        var syncAction = new ActionValueSync(deleteChatAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 6, chat.toJid()
                .toString(), "1");
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Clears the content of a chat for this client and its companions using a modern version of Whatsapp
     *
     * @param chat                the non-null chat to clear
     * @param keepStarredMessages whether starred messages in this chat should be kept
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> clear(@NonNull T chat, boolean keepStarredMessages) {
        var known = store().findChatByJid(chat);
        var range = createRange(chat.toJid(), true);
        var clearChatAction = new ClearChatAction(range);
        var syncAction = new ActionValueSync(clearChatAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 6, chat.toJid()
                .toString(), booleanToInt(keepStarredMessages), "0");
        return socket.pushPatch(request)
                .thenApplyAsync(ignored -> chat);
    }

    private ActionMessageRangeSync createRange(ContactJidProvider chat, boolean allMessages) {
        return store().findChatByJid(chat.toJid())
                .map(result -> new ActionMessageRangeSync(result, allMessages))
                .orElseThrow(() -> new NoSuchElementException("Missing chat: %s".formatted(chat.toJid())));
    }

    private String participantToFlag(MessageInfo info) {
        return info.chatJid()
                .isGroup() && !info.fromMe() ?
                info.senderJid()
                        .toString() :
                "0";
    }

    private String fromMeToFlag(MessageInfo info) {
        return booleanToInt(info.fromMe());
    }

    private String booleanToInt(boolean keepStarredMessages) {
        return keepStarredMessages ?
                "1" :
                "0";
    }

    /**
     * A configuration class used to specify the behaviour of {@link Whatsapp}
     */
    @Builder(builderMethodName = "newOptions")
    @With
    @Data
    @Accessors(fluent = true)
    public static class Options {
        /**
         * Last known version of Whatsapp
         */
        private static final Version WHATSAPP_VERSION = new Version(2, 2212, 7);

        /**
         * Constant for unlimited listeners size
         */
        private static final int UNLIMITED_LISTENERS = -1;

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
         * Whether the default serialization mechanism should be used or not.
         * Set this to false if you want to implement a custom serializer.
         */
        @Default
        private final boolean defaultSerialization = true;

        /**
         * Whether a preview should be automatically generated and attached to text messages that contain links.
         * By default, true
         */
        @Default
        private final boolean automaticTextPreview = true;

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
         * Handles the qr code when a connection is first established with Whatsapp.
         * By default, the qr code is printed on the terminal.
         */
        @Default
        private QrHandler qrHandler = QrHandler.toTerminal();

        /**
         * Handles failures in the WebSocket.
         * Returns true if the current connection should be killed and a new one created.
         * Otherwise, the connection will not be killed, but more failures may be caused by the latter.
         * By default, false.
         */
        @Default
        private ErrorHandler errorHandler = ErrorHandler.toFile();

        /**
         * The number of maximum listeners that the linked Whatsapp instance supports.
         * By default, unlimited.
         */
        @Default
        private int listenersLimit = UNLIMITED_LISTENERS;

        /**
         * Constructs a new instance of WhatsappConfiguration with default options
         *
         * @return a non-null options configuration
         */
        public static Options defaultOptions() {
            return newOptions().build();
        }
    }

    private class BlockingDefaultSerializer implements OnSocketEvent {
        @Override
        public void onSocketEvent(SocketEvent event) {
            if (event != SocketEvent.CLOSE) {
                return;
            }

            store().save(false);
            keys().save(false);
        }
    }
}
