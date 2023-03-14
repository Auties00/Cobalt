package it.auties.whatsapp.api;

import it.auties.bytes.Bytes;
import it.auties.linkpreview.LinkPreview;
import it.auties.linkpreview.LinkPreviewMedia;
import it.auties.linkpreview.LinkPreviewResult;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGmc;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.listener.*;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.business.BusinessCatalogEntry;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.business.BusinessCollectionEntry;
import it.auties.whatsapp.model.business.BusinessProfile;
import it.auties.whatsapp.model.button.FourRowTemplate;
import it.auties.whatsapp.model.button.HydratedFourRowTemplate;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJid.Server;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.poll.PollAdditionalMetadata;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedMetadata;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedOptions;
import it.auties.whatsapp.model.privacy.GdprAccountReport;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.request.ReplyHandler;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.response.HasWhatsappResponse;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.socket.SocketHandler;
import it.auties.whatsapp.util.*;
import lombok.NonNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.bytes.Bytes.ofRandom;
import static it.auties.whatsapp.api.WhatsappOptions.WebOptions.defaultOptions;
import static it.auties.whatsapp.binary.PatchType.REGULAR_HIGH;
import static it.auties.whatsapp.binary.PatchType.REGULAR_LOW;
import static it.auties.whatsapp.model.contact.ContactJid.Server.BROADCAST;
import static it.auties.whatsapp.model.contact.ContactJid.Server.GROUP;
import static it.auties.whatsapp.model.message.standard.TextMessage.TextMessagePreviewType.NONE;
import static it.auties.whatsapp.model.message.standard.TextMessage.TextMessagePreviewType.VIDEO;
import static it.auties.whatsapp.model.sync.RecordSync.Operation.SET;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Whatsapp {
    /**
     * The socket associated with this session
     */
    private final SocketHandler socketHandler;

    private Whatsapp(@NonNull WhatsappOptions options) {
        this(options, Store.of(options), Keys.of(options));
    }

    private Whatsapp(WhatsappOptions options, Store store, Keys keys) {
        this.socketHandler = new SocketHandler(this, options, store, keys);
        if (!options.autodetectListeners()) {
            return;
        }

        ListenerScanner.scan(this).forEach(this::addListener);
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Whatsapp addListener(@NonNull Listener listener) {
        Validate.isTrue(hasRegisterSlotAvailable(), "The number of listeners is too high: expected %s, got %s", socketHandler.options()
                .listenersLimit(), socketHandler.store().listeners().size());
        socketHandler.store().listeners().add(listener);
        return this;
    }

    private boolean hasRegisterSlotAvailable() {
        return socketHandler.options().listenersLimit() < 0 || store().listeners().size() + 1 <= socketHandler.options()
                .listenersLimit();
    }

    /**
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public Store store() {
        return socketHandler.store();
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
     * Constructs a new instance of the API. If the id is not associated with any session, a new one
     * will be created.
     *
     * @param options the non-null options used to create this session
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp newConnection(@NonNull WhatsappOptions options) {
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
    public static Whatsapp newConnection(@NonNull WhatsappOptions options, @NonNull Store store, @NonNull Keys keys) {
        return new Whatsapp(options, store, keys);
    }

    /**
     * Constructs a new instance of the API from the first session opened. If no sessions are
     * available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp firstConnection() {
        return firstConnection(defaultOptions());
    }

    /**
     * Constructs a new instance of the API from the first session opened. If no sessions are
     * available, a new one will be created.
     *
     * @param options the non-null options
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp firstConnection(@NonNull WhatsappOptions options) {
        var lastIds = options.deserializer().findIds();
        if (!lastIds.isEmpty()) {
            options.id(lastIds.peekFirst());
        }
        return newConnection(options);
    }

    /**
     * Constructs a new instance of the API from the last session opened. If no sessions are
     * available, a new one will be created.
     *
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp lastConnection() {
        return lastConnection(defaultOptions());
    }

    /**
     * Constructs a new instance of the API from the last session opened. If no sessions are
     * available, a new one will be created.
     *
     * @param options the non-null options
     * @return a non-null Whatsapp instance
     */
    public static Whatsapp lastConnection(@NonNull WhatsappOptions options) {
        var lastIds = options.deserializer().findIds();
        if (!lastIds.isEmpty()) {
            options.id(lastIds.peekLast());
        }
        return newConnection(options);
    }

    /**
     * Returns a list of all known connections
     *
     * @return a non-null List
     */
    public static List<Whatsapp> listConnections() {
        return listConnections(defaultOptions());
    }

    /**
     * Returns a list of all known connections
     *
     * @param options the non-null options
     * @return a non-null List
     */
    public static List<Whatsapp> listConnections(@NonNull WhatsappOptions options) {
        return streamConnections(options).toList();
    }

    /**
     * Returns a stream of all known connections
     *
     * @param options the non-null options
     * @return a non-null Stream
     */
    public static Stream<Whatsapp> streamConnections(@NonNull WhatsappOptions options) {
        return options.deserializer()
                .findIds()
                .stream()
                .map(id -> Whatsapp.newConnection(options.id(id)));
    }

    /**
     * Returns a stream of all known connections
     *
     * @return a non-null Stream
     */
    public static Stream<Whatsapp> streamConnections() {
        return streamConnections(defaultOptions());
    }

    /**
     * Returns the keys associated with this session
     *
     * @return a non-null WhatsappKeys
     */
    public Keys keys() {
        return socketHandler.keys();
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
    public Whatsapp addChatMessagesSyncListener(OnChatMessagesSync onChatRecentMessages) {
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
    public Whatsapp addConversationMessageStatusListener(OnConversationMessageStatus onConversationMessageStatus) {
        return addListener(onConversationMessageStatus);
    }

    /**
     * Registers a message status listener
     *
     * @param onAnyMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addAnyMessageStatusListener(OnAnyMessageStatus onAnyMessageStatus) {
        return addListener(onAnyMessageStatus);
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
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewMessageListener(OnNewMarkedMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new status listener
     *
     * @param onNewMediaStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addNewStatusListener(OnNewMediaStatus onNewMediaStatus) {
        return addListener(onNewMediaStatus);
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
     * @param onMediaStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMediaStatusListener(OnMediaStatus onMediaStatus) {
        return addListener(onMediaStatus);
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
     * Registers a sync progress listener
     *
     * @param onSyncProgress the listener to register
     * @return the same instance
     */
    public Whatsapp addHistorySyncProgressListener(OnHistorySyncProgress onSyncProgress) {
        return addListener(onSyncProgress);
    }

    /**
     * Registers a chat recent messages listener
     *
     * @param onChatRecentMessages the listener to register
     * @return the same instance
     */
    public Whatsapp addChatMessagesSyncListener(OnWhatsappChatMessagesSync onChatRecentMessages) {
        return addListener(onChatRecentMessages);
    }

    /**
     * Registers a chats listener
     *
     * @param onChats the listener to register
     * @return the same instance
     */
    public Whatsapp addChatsListener(OnChatMessagesSync onChats) {
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
     * @param onWhatsappConversationMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addConversationMessageStatusListener(OnWhatsappConversationMessageStatus onWhatsappConversationMessageStatus) {
        return addListener(onWhatsappConversationMessageStatus);
    }

    /**
     * Registers a message status listener
     *
     * @param onMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addAnyMessageStatusListener(OnWhatsappAnyMessageStatus onMessageStatus) {
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
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewMessageListener(OnWhatsappNewMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewMessageListener(OnWhatsappNewMarkedMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new status listener
     *
     * @param onNewStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addNewStatusListener(OnWhatsappNewMediaStatus onNewStatus) {
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
    public Whatsapp addMediaStatusListener(OnWhatsappMediaStatus onStatus) {
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
     * Registers a sync progress listener
     *
     * @param onSyncProgress the listener to register
     * @return the same instance
     */
    public Whatsapp addHistorySyncProgressListener(OnWhatsappHistorySyncProgress onSyncProgress) {
        return addListener(onSyncProgress);
    }

    /**
     * Registers a message reply listener
     *
     * @param onMessageReply the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageReplyListener(OnWhatsappMessageReply onMessageReply) {
        return addListener(onMessageReply);
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param info           the non-null target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(@NonNull MessageInfo info, @NonNull OnMessageReply onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param id             the non-null id of the target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(@NonNull String id, @NonNull OnMessageReply onMessageReply) {
        return addMessageReplyListener((info, quoted) -> {
            if (!info.id().equals(id)) {
                return;
            }
            onMessageReply.onMessageReply(info, quoted);
        });
    }

    /**
     * Registers a message reply listener
     *
     * @param onMessageReply the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageReplyListener(OnMessageReply onMessageReply) {
        return addListener(onMessageReply);
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param info           the non-null target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(@NonNull MessageInfo info, @NonNull OnWhatsappMessageReply onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param id             the non-null id of the target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(@NonNull String id, @NonNull OnWhatsappMessageReply onMessageReply) {
        return addMessageReplyListener((info, quoted) -> {
            if (!info.id().equals(id)) {
                return;
            }
            onMessageReply.onMessageReply(info, quoted);
        });
    }

    /**
     * Registers a name change listener
     *
     * @param onUserNameChange the non-null listener
     */
    public Whatsapp addUserNameChangeListener(@NonNull OnUserNameChange onUserNameChange) {
        return addListener(onUserNameChange);
    }

    /**
     * Registers a name change listener
     *
     * @param onNameChange the non-null listener
     */
    public Whatsapp addUserNameChangeListener(@NonNull OnWhatsappUserNameChange onNameChange) {
        return addListener(onNameChange);
    }

    /**
     * Registers a status change listener
     *
     * @param onUserStatusChange the non-null listener
     */
    public Whatsapp addUserStatusChangeListener(@NonNull OnUserStatusChange onUserStatusChange) {
        return addListener(onUserStatusChange);
    }

    /**
     * Registers a status change listener
     *
     * @param onUserStatusChange the non-null listener
     */
    public Whatsapp addUserStatusChangeListener(@NonNull OnWhatsappUserStatusChange onUserStatusChange) {
        return addListener(onUserStatusChange);
    }

    /**
     * Registers a picture change listener
     *
     * @param onUserPictureChange the non-null listener
     */
    public Whatsapp addUserPictureChangeListener(@NonNull OnUserPictureChange onUserPictureChange) {
        return addListener(onUserPictureChange);
    }

    /**
     * Registers a picture change listener
     *
     * @param onUserPictureChange the non-null listener
     */
    public Whatsapp addUserPictureChangeListener(@NonNull OnWhatsappUserPictureChange onUserPictureChange) {
        return addListener(onUserPictureChange);
    }

    /**
     * Registers a profile picture listener
     *
     * @param onContactPictureChange the non-null listener
     */
    public Whatsapp addContactPictureChangeListener(@NonNull OnContactPictureChange onContactPictureChange) {
        return addListener(onContactPictureChange);
    }

    /**
     * Registers a profile picture listener
     *
     * @param onProfilePictureChange the non-null listener
     */
    public Whatsapp addContactPictureChangeListener(@NonNull OnWhatsappContactPictureChange onProfilePictureChange) {
        return addListener(onProfilePictureChange);
    }

    /**
     * Registers a group picture listener
     *
     * @param onGroupPictureChange the non-null listener
     */
    public Whatsapp addGroupPictureChangeListener(@NonNull OnGroupPictureChange onGroupPictureChange) {
        return addListener(onGroupPictureChange);
    }

    /**
     * Registers a group picture listener
     *
     * @param onGroupPictureChange the non-null listener
     */
    public Whatsapp addGroupPictureChangeListener(@NonNull OnWhatsappContactPictureChange onGroupPictureChange) {
        return addListener(onGroupPictureChange);
    }

    /**
     * Registers a contact blocked listener
     *
     * @param onContactBlocked the non-null listener
     */
    public Whatsapp addContactBlockedListener(@NonNull OnContactBlocked onContactBlocked) {
        return addListener(onContactBlocked);
    }

    /**
     * Registers a contact blocked listener
     *
     * @param onContactBlocked the non-null listener
     */
    public Whatsapp addContactBlockedListener(@NonNull OnWhatsappContactBlocked onContactBlocked) {
        return addListener(onContactBlocked);
    }

    /**
     * Registers a privacy setting changed listener
     *
     * @param onPrivacySettingChanged the listener to register
     * @return the same instance
     */
    public Whatsapp addPrivacySettingChangedListener(OnPrivacySettingChanged onPrivacySettingChanged) {
        return addListener(onPrivacySettingChanged);
    }


    /**
     * Registers a privacy setting changed listener
     *
     * @param onWhatsappPrivacySettingChanged the listener to register
     * @return the same instance
     */
    public Whatsapp addPrivacySettingChangedListener(OnWhatsappPrivacySettingChanged onWhatsappPrivacySettingChanged) {
        return addListener(onWhatsappPrivacySettingChanged);
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove
     * @return the same instance
     */
    public Whatsapp removeListener(@NonNull Listener listener) {
        socketHandler.store().listeners().remove(listener);
        return this;
    }

    /**
     * Opens a connection with Whatsapp Web's WebSocket if a previous connection doesn't exist
     *
     * @return a future that will only be completed when the connection is closed
     */
    public CompletableFuture<Void> connect() {
        return socketHandler.connect();
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return a future
     */
    public CompletableFuture<Void> disconnect() {
        return socketHandler.disconnect(DisconnectReason.DISCONNECTED);
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     * @return a future
     */
    public CompletableFuture<Void> reconnect() {
        return socketHandler.disconnect(DisconnectReason.RECONNECTING);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous
     * saved credentials. The next time the API is used, the QR countryCode will need to be scanned again.
     *
     * @return a future
     */
    public CompletableFuture<Void> logout() {
        if (store().userCompanionJid() == null) {
            return socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
        }
        var metadata = Map.of("jid", store().userCompanionJid(), "reason", "user_initiated");
        var device = Node.ofAttributes("remove-companion-device", metadata);
        return socketHandler.sendQuery("set", "md", device).thenRunAsync(() -> {
        });
    }

    /**
     * Changes a privacy setting in Whatsapp's settings. If the value is
     * {@link PrivacySettingValue#CONTACTS_EXCEPT}, the excluded parameter should also be filled or an
     * exception will be thrown, otherwise it will be ignored.
     *
     * @param type     the non-null setting to change
     * @param value    the non-null value to attribute to the setting
     * @param excluded the non-null excluded contacts if value is {@link PrivacySettingValue#CONTACTS_EXCEPT}
     * @return the same instance wrapped in a completable future
     */
    @SafeVarargs
    public final <T extends ContactJidProvider> CompletableFuture<Whatsapp> changePrivacySetting(@NonNull PrivacySettingType type, @NonNull PrivacySettingValue value, @NonNull T @NonNull ... excluded) {
        checkLoggedIn();
        Validate.isTrue(type.isSupported(value),
                "Cannot change setting %s to %s: this toggle cannot be used because Whatsapp doesn't support it", value.name(), type.name());
        var attributes = Attributes.of()
                .put("name", type.data())
                .put("value", value.data())
                .put("dhash", "none", () -> value == PrivacySettingValue.CONTACTS_EXCEPT)
                .toMap();
        var excludedJids = Arrays.stream(excluded).map(ContactJidProvider::toJid).toList();
        var children = value != PrivacySettingValue.CONTACTS_EXCEPT ? null : excludedJids.stream()
                .map(entry -> Node.ofAttributes("user", Map.of("jid", entry, "action", "add")))
                .toList();
        return socketHandler.sendQuery("set", "privacy", Node.ofChildren("privacy", Node.ofChildren("category", attributes, children)))
                .thenRunAsync(() -> onPrivacyFeatureChanged(type, value, excludedJids))
                .thenApplyAsync(ignored -> this);
    }

    private void onPrivacyFeatureChanged(PrivacySettingType type, PrivacySettingValue value, List<ContactJid> excludedJids) {
        var newEntry = new PrivacySettingEntry(type, value, excludedJids);
        var oldEntry = store().findPrivacySetting(type);
        store().addPrivacySetting(type, newEntry);
        socketHandler.onPrivacySettingChanged(oldEntry, newEntry);
    }

    /**
     * Changes the default ephemeral timer of new chats.
     *
     * @param timer the new ephemeral timer
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> changeNewChatsEphemeralTimer(@NonNull ChatEphemeralTimer timer) {
        checkLoggedIn();
        return socketHandler.sendQuery("set", "disappearing_mode", Node.ofAttributes("disappearing_mode", Map.of("duration", timer.period()
                        .toSeconds())))
                .thenRunAsync(() -> store().newChatsEphemeralTimer(timer))
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Creates a new request to get a document containing all the data that was collected by Whatsapp
     * about this user. It takes three business days to receive it. To query the result status, use
     * {@link Whatsapp#getGdprAccountInfoStatus()}
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> createGdprAccountInfo() {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.ofAttributes("gdpr", Map.of("gdpr", "request")))
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Queries the document containing all the data that was collected by Whatsapp about this user. To
     * create a request for this document, use {@link Whatsapp#createGdprAccountInfo()}
     *
     * @return the same instance wrapped in a completable future
     */
    // TODO: Implement ready and error states
    public CompletableFuture<GdprAccountReport> getGdprAccountInfoStatus() {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.ofAttributes("gdpr", Map.of("gdpr", "status")))
                .thenApplyAsync(result -> GdprAccountReport.ofPending(result.attributes().getLong("timestamp")));
    }

    /**
     * Changes the name of this user
     *
     * @param newName the non-null new name
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> changeName(@NonNull String newName) {
        checkLoggedIn();
        var oldName = socketHandler.store().userName();
        return socketHandler.send(Node.ofChildren("presence", Map.of("name", newName)))
                .thenRunAsync(() -> socketHandler.updateUserName(newName, oldName))
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Changes the status(i.e. user description) of this user
     *
     * @param newStatus the non-null new status
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> changeStatus(@NonNull String newStatus) {
        checkLoggedIn();
        return socketHandler.sendQuery("set", "status", Node.of("status", newStatus.getBytes(StandardCharsets.UTF_8)))
                .thenRunAsync(() -> store().userName(newStatus))
                .thenApplyAsync(ignored -> this);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> subscribeToPresence(@NonNull T jid) {
        checkLoggedIn();
        return socketHandler.subscribeToPresence(jid).thenApplyAsync(ignored -> jid);
    }

    /**
     * Remove a reaction from a message
     *
     * @param message the non-null message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> removeReaction(@NonNull MessageMetadataProvider message) {
        return sendReaction(message, (String) null);
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction. If a string that
     *                 isn't an emoji supported by Whatsapp is used, it will not get displayed
     *                 correctly. Use {@link Whatsapp#sendReaction(MessageMetadataProvider, Emojy)} if
     *                 you need a typed emojy enum.
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendReaction(@NonNull MessageMetadataProvider message, String reaction) {
        checkLoggedIn();
        var key = MessageKey.builder()
                .chatJid(message.chat().jid())
                .senderJid(message.senderJid())
                .fromMe(Objects.equals(message.senderJid().toUserJid(), store().userCompanionJid().toUserJid()))
                .id(message.id())
                .build();
        var reactionMessage = ReactionMessage.builder()
                .key(key)
                .content(reaction)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        return sendMessage(message.chat(), reactionMessage);
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
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull MessageContainer message) {
        var key = MessageKey.builder().chatJid(chat.toJid()).fromMe(true).senderJid(store().userCompanionJid()).build();
        var info = MessageInfo.builder()
                .senderJid(store().userCompanionJid())
                .key(key)
                .message(message)
                .timestampSeconds(Clock.nowSeconds())
                .broadcast(chat.toJid().hasServer(BROADCAST))
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
        checkLoggedIn();
        store().attribute(info);
        attributeMessageMetadata(info);
        var future = socketHandler.sendMessage(MessageSendRequest.of(info));
        return future.thenApplyAsync(ignored -> info);
    }

    private void attributeMessageMetadata(MessageInfo info) {
        info.ignore(true);
        info.key().chatJid(info.chatJid().toUserJid());
        info.key().senderJid(info.senderJid() == null ? null : info.senderJid().toUserJid());
        fixEphemeralMessage(info);
        switch (info.message().content()) {
            case TextMessage textMessage -> attributeTextMessage(textMessage);
            case MediaMessage mediaMessage -> attributeMediaMessage(mediaMessage);
            case PollCreationMessage pollCreationMessage -> attributePollCreationMessage(info, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> attributePollUpdateMessage(info, pollUpdateMessage);
            case GroupInviteMessage groupInviteMessage -> attributeGroupInviteMessage(info, groupInviteMessage);
            case ButtonMessage buttonMessage -> attributeButtonMessage(info, buttonMessage);
            default -> {
            }
        }
    }

    /**
     * Marks a chat as read.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> markRead(@NonNull T chat) {
        return mark(chat, true).thenComposeAsync(ignored -> markAllAsRead(chat)).thenApplyAsync(ignored -> chat);
    }

    private void fixEphemeralMessage(MessageInfo info) {
        if (info.message().hasCategory(MessageCategory.SERVER)) {
            return;
        }
        if (info.chat().isEphemeral()) {
            info.message()
                    .contentWithContext()
                    .map(ContextualMessage::contextInfo)
                    .ifPresent(contextInfo -> createEphemeralContext(info.chat(), contextInfo));
            info.message(info.message().toEphemeral());
            return;
        }

        if (info.message().type() != MessageType.EPHEMERAL) {
            return;
        }

        info.message(info.message().unbox());
    }

    private void attributeTextMessage(TextMessage textMessage) {
        if (socketHandler.options().textPreviewSetting() == TextPreviewSetting.DISABLED) {
            return;
        }
        var match = LinkPreview.createPreview(textMessage.text()).orElse(null);
        if (match == null) {
            return;
        }
        var uri = match.result().uri().toString();
        if (socketHandler.options().textPreviewSetting() == TextPreviewSetting.ENABLED_WITH_INFERENCE && !match.text()
                .equals(uri)) {
            textMessage.text(textMessage.text().replace(match.text(), uri));
        }
        var imageUri = match.result()
                .images()
                .stream()
                .reduce(this::compareDimensions)
                .map(LinkPreviewMedia::uri)
                .orElse(null);
        var videoUri = match.result()
                .videos()
                .stream()
                .reduce(this::compareDimensions)
                .map(LinkPreviewMedia::uri)
                .orElse(null);
        textMessage.matchedText(uri);
        textMessage.canonicalUrl(requireNonNullElse(videoUri, match.result().uri()).toString());
        textMessage.thumbnail(Medias.getPreview(imageUri).orElse(null));
        textMessage.description(match.result().siteDescription());
        textMessage.title(match.result().title());
        textMessage.previewType(videoUri != null ? VIDEO : NONE);
    }

    private void attributeMediaMessage(MediaMessage mediaMessage) {
        Validate.isTrue(mediaMessage.decodedMedia()
                .isPresent(), "Cannot upload a message whose content isn't available");
        var upload = Medias.upload(mediaMessage.decodedMedia()
                .get(), mediaMessage.mediaType(), store().mediaConnection());
        mediaMessage.mediaSha256(upload.fileSha256())
                .mediaEncryptedSha256(upload.fileEncSha256())
                .mediaKey(upload.mediaKey())
                .mediaUrl(upload.url())
                .mediaDirectPath(upload.directPath())
                .mediaSize(upload.fileLength());
    }

    private void attributePollCreationMessage(MessageInfo info, PollCreationMessage pollCreationMessage) {
        var pollEncryptionKey = requireNonNullElseGet(pollCreationMessage.encryptionKey(), KeyHelper::senderKey);
        pollCreationMessage.encryptionKey(pollEncryptionKey);
        info.messageSecret(pollEncryptionKey);
        info.message().deviceInfo().messageSecret(pollEncryptionKey);
        var metadata = new PollAdditionalMetadata(false);
        info.pollAdditionalMetadata(metadata);
    }

    private void attributePollUpdateMessage(MessageInfo info, PollUpdateMessage pollUpdateMessage) {
        if (pollUpdateMessage.encryptedMetadata() != null) {
            return;
        }
        var iv = ofRandom(12).toByteArray();
        var additionalData = "%s\0%s".formatted(pollUpdateMessage.pollCreationMessageKey()
                .id(), store().userCompanionJid().toUserJid());
        var encryptedOptions = pollUpdateMessage.votes().stream().map(entry -> Sha256.calculate(entry.name())).toList();
        var pollUpdateEncryptedOptions = Protobuf.writeMessage(PollUpdateEncryptedOptions.of(encryptedOptions));
        var originalPollInfo = socketHandler.store()
                .findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
        var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
        var originalPollSender = originalPollInfo.senderJid().toUserJid().toString().getBytes(StandardCharsets.UTF_8);
        var modificationSenderJid = info.senderJid().toUserJid();
        pollUpdateMessage.voter(modificationSenderJid);
        var modificationSender = modificationSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var secretName = pollUpdateMessage.secretName().getBytes(StandardCharsets.UTF_8);
        var useSecretPayload = Bytes.of(pollUpdateMessage.pollCreationMessageKey().id())
                .append(originalPollSender)
                .append(modificationSender)
                .append(secretName)
                .toByteArray();
        var useCaseSecret = Hkdf.extractAndExpand(originalPollMessage.encryptionKey(), useSecretPayload, 32);
        var pollUpdateEncryptedPayload = AesGmc.encrypt(iv, pollUpdateEncryptedOptions, useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollUpdateEncryptedMetadata = new PollUpdateEncryptedMetadata(pollUpdateEncryptedPayload, iv);
        pollUpdateMessage.encryptedMetadata(pollUpdateEncryptedMetadata);
    }

    private void attributeButtonMessage(MessageInfo info, ButtonMessage buttonMessage) {
        switch (buttonMessage) {
            case ButtonsMessage buttonsMessage && buttonsMessage.header().isPresent() && buttonsMessage.header()
                    .get() instanceof MediaMessage mediaMessage -> attributeMediaMessage(mediaMessage);
            case ButtonsMessage buttonsMessage && buttonsMessage.header().isPresent() && buttonsMessage.header()
                    .get() instanceof MediaMessage mediaMessage -> attributeMediaMessage(mediaMessage);
            case TemplateMessage templateMessage && templateMessage.format().isPresent() -> {
                switch (templateMessage.format().get()) {
                    case FourRowTemplate fourRowTemplate && fourRowTemplate.title()
                            .isPresent() && fourRowTemplate.title().get() instanceof MediaMessage mediaMessage ->
                            attributeMediaMessage(mediaMessage);
                    case HydratedFourRowTemplate hydratedFourRowTemplate && hydratedFourRowTemplate.title()
                            .isPresent() && hydratedFourRowTemplate.title()
                            .get() instanceof MediaMessage mediaMessage -> attributeMediaMessage(mediaMessage);
                    default -> {
                    }
                }
            }
            case InteractiveMessage interactiveMessage && interactiveMessage.header()
                    .isPresent() && interactiveMessage.header()
                    .get()
                    .attachment()
                    .isPresent() && interactiveMessage.header()
                    .get()
                    .attachment()
                    .get() instanceof MediaMessage mediaMessage -> attributeMediaMessage(mediaMessage);
            default -> {
            }
        }

        // Credit to Baileys: https://github.com/adiwajshing/Baileys/blob/f0bdb12e56cea8b0bfbb0dff37c01690274e3e31/src/Utils/messages.ts#L781
        info.message(info.message().toViewOnce());
    }

    // This is not needed probably, but Whatsapp uses a text message by default, so maybe it makes sense
    private void attributeGroupInviteMessage(MessageInfo info, GroupInviteMessage groupInviteMessage) {
        Validate.isTrue(groupInviteMessage.code() != null, "Invalid message code");
        var url = "https://chat.whatsapp.com/%s".formatted(groupInviteMessage.code());
        var preview = LinkPreview.createPreview(URI.create(url))
                .stream()
                .map(LinkPreviewResult::images)
                .map(Collection::stream)
                .map(Stream::findFirst)
                .flatMap(Optional::stream)
                .findFirst()
                .map(LinkPreviewMedia::uri)
                .orElse(null);
        var replacement = TextMessage.builder()
                .text(groupInviteMessage.caption() != null ? "%s: %s".formatted(groupInviteMessage.caption(), url) : url)
                .description("WhatsApp Group Invite")
                .title(groupInviteMessage.groupName())
                .previewType(NONE)
                .thumbnail(readURI(preview))
                .matchedText(url)
                .canonicalUrl(url)
                .build();
        info.message(MessageContainer.of(replacement));
    }

    private <T extends ContactJidProvider> CompletableFuture<T> mark(@NonNull T chat, boolean read) {
        checkLoggedIn();
        var range = createRange(chat, false);
        var markAction = MarkChatAsReadAction.of(read, range);
        var syncAction = ActionValueSync.of(markAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid().toString());
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    private CompletableFuture<Void> markAllAsRead(ContactJidProvider chat) {
        var all = socketHandler.store()
                .findChatByJid(chat.toJid())
                .stream()
                .map(Chat::unreadMessages)
                .flatMap(Collection::stream)
                .map(this::markRead)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(all);
    }

    private LinkPreviewMedia compareDimensions(LinkPreviewMedia first, LinkPreviewMedia second) {
        return first.width() * first.height() > second.width() * second.height() ? first : second;
    }

    private byte[] readURI(URI preview) {
        try {
            if (preview == null) {
                return null;
            }
            return preview.toURL().openConnection().getInputStream().readAllBytes();
        } catch (Throwable throwable) {
            return null;
        }
    }

    private ActionMessageRangeSync createRange(ContactJidProvider chat, boolean allMessages) {
        var known = store().findChatByJid(chat.toJid()).orElseGet(() -> socketHandler.store().addChat(chat.toJid()));
        return new ActionMessageRangeSync(known, allMessages);
    }

    /**
     * Marks a message as read
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> markRead(@NonNull MessageInfo info) {
        checkLoggedIn();
        var type = store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS)
                .value() == PrivacySettingValue.EVERYONE ? "read" : "read-self";
        socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), type);
        var count = info.chat().unreadMessagesCount();
        if (count > 0) {
            info.chat().unreadMessagesCount(count - 1);
        }
        return CompletableFuture.completedFuture(info.status(MessageStatus.READ));
    }

    private void createEphemeralContext(Chat chat, ContextInfo contextInfo) {
        var period = chat.ephemeralMessageDuration().period().toSeconds();
        contextInfo.ephemeralExpiration((int) period);
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendReaction(@NonNull MessageMetadataProvider message, Emojy reaction) {
        return sendReaction(message, Objects.toString(reaction));
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
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull String message, @NonNull MessageMetadataProvider quotedMessage) {
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
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull ContextualMessage message, @NonNull MessageMetadataProvider quotedMessage) {
        Validate.isTrue(!quotedMessage.message().isEmpty(), "Cannot quote an empty message");
        Validate.isTrue(!quotedMessage.message().hasCategory(MessageCategory.SERVER), "Cannot quote a server message");
        return sendMessage(chat, message, ContextInfo.of(quotedMessage));
    }

    /**
     * Builds and sends a message from a chat, a message and a context
     *
     * @param chat        the chat where the message should be sent
     * @param message     the message to send
     * @param contextInfo the context of the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> sendMessage(@NonNull ContactJidProvider chat, @NonNull ContextualMessage message, @NonNull ContextInfo contextInfo) {
        message.contextInfo(contextInfo);
        return sendMessage(chat, message);
    }

    /**
     * Awaits for a single response to a message
     *
     * @param info the non-null message whose response is pending
     * @return a non-null result
     */
    public CompletableFuture<MessageInfo> awaitReply(@NonNull MessageInfo info) {
        return awaitReply(info.id());
    }

    /**
     * Awaits for a single response to a message
     *
     * @param id the non-null id of message whose response is pending
     * @return a non-null result
     */
    public CompletableFuture<MessageInfo> awaitReply(@NonNull String id) {
        return store().addPendingReply(ReplyHandler.of(id));
    }

    /**
     * Executes a query to determine whether a user has an account on Whatsapp
     *
     * @param contact the contact to check
     * @return a CompletableFuture that wraps a non-null response
     */
    public CompletableFuture<HasWhatsappResponse> hasWhatsapp(@NonNull ContactJidProvider contact) {
        return hasWhatsapp(new ContactJidProvider[]{contact}).thenApply(result -> result.get(contact.toJid()));
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null map
     */
    public CompletableFuture<Map<ContactJid, HasWhatsappResponse>> hasWhatsapp(@NonNull ContactJidProvider... contacts) {
        checkLoggedIn();
        var contactNodes = Arrays.stream(contacts)
                .map(jid -> Node.ofChildren("user", Node.of("contact", jid.toJid().toPhoneNumber())))
                .toArray(Node[]::new);
        return socketHandler.sendInteractiveQuery(Node.of("contact"), contactNodes)
                .thenApplyAsync(this::parseHasWhatsappResponse);
    }

    private Map<ContactJid, HasWhatsappResponse> parseHasWhatsappResponse(List<Node> nodes) {
        return nodes.stream()
                .map(HasWhatsappResponse::new)
                .collect(Collectors.toMap(HasWhatsappResponse::contact, Function.identity()));
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture that wraps a non-null list of ContactJid
     */
    public CompletableFuture<List<ContactJid>> queryBlockList() {
        checkLoggedIn();
        return socketHandler.queryBlockList();
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status response
     */
    public CompletableFuture<Optional<ContactStatusResponse>> queryStatus(@NonNull ContactJidProvider chat) {
        checkLoggedIn();
        return socketHandler.queryStatus(chat);
    }

    /**
     * Queries the profile picture
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<Optional<URI>> queryPicture(@NonNull ContactJidProvider chat) {
        checkLoggedIn();
        return socketHandler.queryPicture(chat);
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> queryGroupMetadata(@NonNull ContactJidProvider chat) {
        checkLoggedIn();
        return socketHandler.queryGroupMetadata(chat.toJid());
    }

    /**
     * Queries a business profile, if any exists
     *
     * @param contact the target contact
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<BusinessProfile>> queryBusinessProfile(@NonNull ContactJidProvider contact) {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "w:biz", Node.ofChildren("business_profile", Node.of("v", "116"), Node.ofAttributes("profile", Map.of("jid", contact.toJid()))))
                .thenApplyAsync(this::getBusinessProfile);
    }

    private Optional<BusinessProfile> getBusinessProfile(Node result) {
        return result.findNode("business_profile").flatMap(entry -> entry.findNode("profile")).map(BusinessProfile::of);
    }

    /**
     * Queries all the known business categories
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCategory>> queryBusinessCategories() {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "fb:thrift_iq", Node.of("request", Map.of("op", "profile_typeahead", "type", "catkit", "v", "1"), Node.ofChildren("query", List.of())))
                .thenApplyAsync(Whatsapp::parseBusinessCategories);
    }

    private static List<BusinessCategory> parseBusinessCategories(Node result) {
        return result.findNode("response")
                .flatMap(entry -> entry.findNode("categories"))
                .stream()
                .map(entry -> entry.findNodes("category"))
                .flatMap(Collection::stream)
                .map(BusinessCategory::of)
                .toList();
    }

    /**
     * Queries the invite countryCode of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<String> queryGroupInviteCode(@NonNull ContactJidProvider chat) {
        checkLoggedIn();
        return socketHandler.sendQuery(chat.toJid(), "get", "w:g2", Node.of("invite"))
                .thenApplyAsync(Whatsapp::parseInviteCode);
    }

    private static String parseInviteCode(Node result) {
        return result.findNode("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite countryCode in invite response"))
                .attributes()
                .getRequiredString("code");
    }

    /**
     * Revokes the invite countryCode of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> revokeGroupInvite(@NonNull T chat) {
        checkLoggedIn();
        return socketHandler.sendQuery(chat.toJid(), "set", "w:g2", Node.of("invite")).thenApplyAsync(ignored -> chat);
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite countryCode
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<Chat>> acceptGroupInvite(@NonNull String inviteCode) {
        checkLoggedIn();
        return socketHandler.sendQuery(Server.GROUP.toJid(), "set", "w:g2", Node.ofAttributes("invite", Map.of("code", inviteCode)))
                .thenApplyAsync(this::parseAcceptInvite);
    }

    private Optional<Chat> parseAcceptInvite(Node result) {
        return result.findNode("group")
                .flatMap(group -> group.attributes().getJid("jid"))
                .map(jid -> store().findChatByJid(jid).orElseGet(() -> socketHandler.store().addChat(jid)));
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture
     */
    public CompletableFuture<Boolean> changePresence(boolean available) {
        checkLoggedIn();
        var presence = available ? ContactStatus.AVAILABLE : ContactStatus.UNAVAILABLE;
        var node = Node.ofAttributes("presence", Map.of("type", presence.data()));
        return socketHandler.sendWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(null, presence))
                .thenApplyAsync(ignored -> available);
    }

    private void updateSelfPresence(ContactJidProvider chatJid, ContactStatus presence) {
        var self = store().findContactByJid(store().userCompanionJid().toUserJid());
        if (self.isEmpty()) {
            return;
        }
        if (presence == ContactStatus.AVAILABLE || presence == ContactStatus.UNAVAILABLE) {
            self.get().lastKnownPresence(presence);
        }
        if (chatJid != null) {
            store().findChatByJid(chatJid).ifPresent(chat -> chat.presences().put(self.get().jid(), presence));
        }
        self.get().lastSeen(ZonedDateTime.now());
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chat     the target chat
     * @param presence the new status
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changePresence(@NonNull T chat, @NonNull ContactStatus presence) {
        checkLoggedIn();
        var node = Node.ofAttributes("presence", Map.of("to", chat.toJid(), "type", presence.data()));
        return socketHandler.sendWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(chat, presence))
                .thenApplyAsync(ignored -> chat);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> promote(@NonNull ContactJidProvider group, @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, contacts);
    }

    private CompletableFuture<List<ContactJid>> executeActionOnGroupParticipant(ContactJidProvider group, GroupAction action, ContactJidProvider... jids) {
        checkLoggedIn();
        var body = Arrays.stream(jids)
                .map(ContactJidProvider::toJid)
                .map(jid -> Node.ofAttributes("participant", Map.of("jid", checkGroupParticipantJid(jid, "Cannot execute action on yourself"))))
                .map(innerBody -> Node.ofChildren(action.data(), innerBody))
                .toArray(Node[]::new);
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(result -> parseGroupActionResponse(result, action));
    }

    private ContactJid checkGroupParticipantJid(ContactJid jid, String errorMessage) {
        if (Objects.equals(jid.toUserJid(), store().userCompanionJid().toUserJid())) {
            throw new IllegalArgumentException(errorMessage);
        }

        return jid;
    }

    private List<ContactJid> parseGroupActionResponse(Node result, GroupAction action) {
        return result.findNode(action.data())
                .orElseThrow(() -> new NoSuchElementException("An erroneous group operation was executed"))
                .findNodes("participant")
                .stream()
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> demote(@NonNull ContactJidProvider group, @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, contacts);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> addGroupParticipant(@NonNull ContactJidProvider group, @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, contacts);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<ContactJid>> removeGroupParticipant(@NonNull ContactJidProvider group, @NonNull ContactJidProvider @NonNull ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.REMOVE, contacts);
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeGroupSubject(@NonNull T group, @NonNull String newName) {
        checkLoggedIn();
        var body = Node.of("subject", newName.getBytes(StandardCharsets.UTF_8));
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body).thenApplyAsync(ignored -> group);
    }

    /**
     * Changes the description of a group
     *
     * @param group       the target group
     * @param description the new name for the group, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeGroupDescription(@NonNull T group, String description) {
        checkLoggedIn();
        return socketHandler.queryGroupMetadata(group.toJid())
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeGroupDescription(group, description, descriptionId))
                .thenApplyAsync(ignored -> group);
    }

    private CompletableFuture<Node> changeGroupDescription(ContactJidProvider group, String description, String descriptionId) {
        var descriptionNode = Optional.ofNullable(description)
                .map(content -> Node.of("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", MessageKey.randomId(), () -> description != null)
                .put("delete", true, () -> description == null)
                .put("prev", descriptionId, () -> descriptionId != null)
                .toMap();
        var body = Node.ofChildren("description", attributes, descriptionNode);
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes which category of users can send messages in a group
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeWhoCanSendMessages(@NonNull T group, @NonNull GroupPolicy policy) {
        checkLoggedIn();
        var body = Node.of(policy != GroupPolicy.ANYONE ? "not_announcement" : "announcement");
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body).thenApplyAsync(ignored -> group);
    }

    /**
     * Changes which category of users can edit the group's settings
     *
     * @param group  the target group
     * @param policy the new policy to enforce
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeWhoCanEditInfo(@NonNull T group, @NonNull GroupPolicy policy) {
        checkLoggedIn();
        var body = Node.of(policy != GroupPolicy.ANYONE ? "locked" : "unlocked");
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body).thenApplyAsync(ignored -> group);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<ContactJid> changeProfilePicture(byte[] image) {
        return changeGroupPicture(store().userCompanionJid(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeGroupPicture(@NonNull T group, URI image) {
        return changeGroupPicture(group, readURI(image));
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeGroupPicture(@NonNull T group, byte[] image) {
        checkLoggedIn();
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), profilePic);
        return socketHandler.sendQuery(group.toJid().toUserJid(), "set", "w:profile:picture", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Creates a new group
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull ContactJidProvider... contacts) {
        return createGroup(subject, ChatEphemeralTimer.OFF, contacts);
    }

    /**
     * Creates a new group
     *
     * @param subject  the new group's name
     * @param timer    the default ephemeral timer for messages sent in this group
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull ChatEphemeralTimer timer, @NonNull ContactJidProvider... contacts) {
        return createGroup(subject, timer, null, contacts);
    }

    /**
     * Creates a new group
     *
     * @param subject     the new group's name
     * @param timer       the default ephemeral timer for messages sent in this group
     * @param parentGroup the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull ChatEphemeralTimer timer, ContactJidProvider parentGroup) {
        return createGroup(subject, timer, parentGroup, new ContactJidProvider[0]);
    }

    /**
     * Creates a new group
     *
     * @param subject     the new group's name
     * @param timer       the default ephemeral timer for messages sent in this group
     * @param parentGroup the community to whom the new group will be linked
     * @param contacts    at least one contact to add to the group, not enforced if part of a community
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createGroup(@NonNull String subject, @NonNull ChatEphemeralTimer timer, ContactJidProvider parentGroup, @NonNull ContactJidProvider... contacts) {
        checkLoggedIn();
        Validate.isTrue(!subject.isBlank(), "The subject of a group cannot be blank");
        var minimumMembersCount = parentGroup == null ? 1 : 0;
        Validate.isTrue(contacts.length >= minimumMembersCount, "Expected at least %s members for this group", minimumMembersCount);
        var children = new ArrayList<Node>();
        if (parentGroup != null) {
            children.add(Node.ofAttributes("linked_parent", Map.of("jid", parentGroup.toJid())));
        }
        if (timer != ChatEphemeralTimer.OFF) {
            children.add(Node.ofAttributes("ephemeral", Map.of("expiration", timer.periodSeconds())));
        }
        Arrays.stream(contacts)
                .map(contact -> Node.ofAttributes("participant", Map.of("jid", checkGroupParticipantJid(contact.toJid(), "Cannot create group with yourself as a participant"))))
                .forEach(children::add);
        var body = Node.ofChildren("create", Map.of("subject", subject, "key", ofRandom(12).toHex()), children);
        return socketHandler.sendQuery(Server.GROUP.toJid(), "set", "w:g2", body)
                .thenApplyAsync(response -> Optional.ofNullable(response)
                        .flatMap(node -> node.findNode("group"))
                        .orElseThrow(() -> new NoSuchElementException("Missing group response, something went wrong: %s".formatted(findErrorNode(response)))))
                .thenApplyAsync(GroupMetadata::of);
    }

    private String findErrorNode(Node result) {
        return Optional.ofNullable(result)
                .flatMap(node -> node.findNode("error"))
                .map(Node::toString)
                .orElse("unknown");
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public <T extends ContactJidProvider> CompletableFuture<T> leaveGroup(@NonNull T group) {
        checkLoggedIn();
        var body = Node.ofChildren("leave", Node.ofAttributes("group", Map.of("id", group.toJid())));
        return socketHandler.sendQuery(Server.GROUP.toJid(), "set", "w:g2", body).thenApplyAsync(ignored -> group);
    }

    /**
     * Links any number of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public CompletableFuture<Map<ContactJid, Boolean>> linkGroupsToCommunity(@NonNull ContactJidProvider community, @NonNull ContactJidProvider... groups){
        checkLoggedIn();
        var body = Arrays.stream(groups)
                .map(entry -> Node.ofAttributes("group", Map.of("jid", entry.toJid())))
                .toArray(Node[]::new);
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.ofChildren("links", Node.ofChildren("link", Map.of("link_type", "sub_group"), body)))
                .thenApplyAsync(result -> parseLinksResponse(result, groups));
    }

    private Map<ContactJid, Boolean> parseLinksResponse(Node result, @NonNull ContactJidProvider[] groups) {
        var success = result.findNode("links")
                .stream()
                .map(entry -> entry.findNodes("link"))
                .flatMap(Collection::stream)
                .filter(entry -> entry.attributes().hasKey("link_type", "sub_group"))
                .map(entry -> entry.findNode("group"))
                .flatMap(Optional::stream)
                .map(entry -> entry.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        return Arrays.stream(groups)
                .map(ContactJidProvider::toJid)
                .collect(Collectors.toUnmodifiableMap(Function.identity(), success::contains));
    }

    /**
     * Unlinks a group from a community
     *
     * @param community the non-null parent community
     * @param group the non-null group to unlink
     * @return a CompletableFuture that indicates whether the request was successful
     */
    public CompletableFuture<Boolean> unlinkGroupFromCommunity(@NonNull ContactJidProvider community, @NonNull ContactJidProvider group){
        checkLoggedIn();
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.ofChildren("unlink", Map.of("unlink_type", "sub_group"), Node.ofAttributes("group", Map.of("jid", group.toJid()))))
                .thenApplyAsync(result -> parseUnlinkResponse(result, group));
    }

    private boolean parseUnlinkResponse(Node result, @NonNull ContactJidProvider group) {
        return result.findNode("unlink")
                .filter(entry -> entry.attributes().hasKey("unlink_type", "sub_group"))
                .flatMap(entry -> entry.findNode("group"))
                .map(entry -> entry.attributes().hasKey("jid", group.toJid().toString()))
                .isPresent();
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
        checkLoggedIn();
        var muteAction = MuteAction.of(true, mute.type() == ChatMute.Type.MUTED_FOR_TIMEFRAME ? mute.endTimeStamp() * 1000L : mute.endTimeStamp(), false);
        var syncAction = ActionValueSync.of(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid().toString());
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unmute(@NonNull T chat) {
        checkLoggedIn();
        var muteAction = MuteAction.of(false, null, false);
        var syncAction = ActionValueSync.of(muteAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 2, chat.toJid().toString());
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Blocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> block(@NonNull T chat) {
        checkLoggedIn();
        var body = Node.ofAttributes("item", Map.of("action", "block", "jid", chat.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body).thenApplyAsync(ignored -> chat);
    }

    /**
     * Unblocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> unblock(@NonNull T chat) {
        checkLoggedIn();
        var body = Node.ofAttributes("item", Map.of("action", "unblock", "jid", chat.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body).thenApplyAsync(ignored -> chat);
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled
     * in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> changeEphemeralTimer(@NonNull T chat, @NonNull ChatEphemeralTimer timer) {
        checkLoggedIn();
        return switch (chat.toJid().server()) {
            case USER, WHATSAPP -> {
                var message = ProtocolMessage.builder()
                        .protocolType(ProtocolMessage.ProtocolMessageType.EPHEMERAL_SETTING)
                        .ephemeralExpiration(timer.period().toSeconds())
                        .build();
                yield sendMessage(chat, message).thenApplyAsync(ignored -> chat);
            }
            case GROUP -> {
                var body = timer == ChatEphemeralTimer.OFF ? Node.of("not_ephemeral") : Node.ofAttributes("ephemeral", Map.of("expiration", timer.period()
                        .toSeconds()));
                yield socketHandler.sendQuery(chat.toJid(), "set", "w:g2", body).thenApplyAsync(ignored -> chat);
            }
            default ->
                    throw new IllegalArgumentException("Unexpected chat %s: ephemeral messages are only supported for conversations and groups".formatted(chat.toJid()));
        };
    }

    /**
     * Marks a message as played
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> markPlayed(@NonNull MessageInfo info) {
        checkLoggedIn();
        if (store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS).value() != PrivacySettingValue.EVERYONE) {
            return CompletableFuture.completedFuture(info);
        }
        socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), "played");
        return CompletableFuture.completedFuture(info.status(MessageStatus.PLAYED));
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

    /**
     * Pins a chat to the top. A maximum of three chats can be pinned to the top. This condition can
     * be checked using;.
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
        checkLoggedIn();
        var pinAction = PinAction.of(pin);
        var syncAction = ActionValueSync.of(pinAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 5, chat.toJid().toString());
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
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

    private CompletableFuture<MessageInfo> star(MessageInfo info, boolean star) {
        checkLoggedIn();
        var starAction = StarAction.of(star);
        var syncAction = ActionValueSync.of(starAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> info);
    }

    private String fromMeToFlag(MessageInfo info) {
        return booleanToInt(info.fromMe());
    }

    private String participantToFlag(MessageInfo info) {
        return info.chatJid().hasServer(GROUP) && !info.fromMe() ? info.senderJid().toString() : "0";
    }

    private String booleanToInt(boolean keepStarredMessages) {
        return keepStarredMessages ? "1" : "0";
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

    /**
     * Archives a chat. If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> archive(@NonNull T chat) {
        return archive(chat, true);
    }

    private <T extends ContactJidProvider> CompletableFuture<T> archive(T chat, boolean archive) {
        checkLoggedIn();
        var range = createRange(chat, false);
        var archiveAction = ArchiveChatAction.of(archive, range);
        var syncAction = ActionValueSync.of(archiveAction);
        var request = PatchRequest.of(REGULAR_LOW, syncAction, SET, 3, chat.toJid().toString());
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
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

    /**
     * Deletes a message
     *
     * @param info     the non-null message to delete
     * @param everyone whether the message should be deleted for everyone or only for this client and
     *                 its companions
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> delete(@NonNull MessageInfo info, boolean everyone) {
        checkLoggedIn();
        if (everyone) {
            var message = ProtocolMessage.builder()
                    .protocolType(ProtocolMessage.ProtocolMessageType.REVOKE)
                    .key(info.key())
                    .build();
            var sender = info.chat().toJid().hasServer(GROUP) ? store().userCompanionJid() : null;
            var key = MessageKey.builder().chatJid(info.chatJid()).fromMe(true).senderJid(sender).build();
            var revokeInfo = MessageInfo.builder()
                    .senderJid(sender)
                    .key(key)
                    .message(MessageContainer.of(message))
                    .timestampSeconds(Clock.nowSeconds())
                    .build();
            var request = MessageSendRequest.builder()
                    .info(revokeInfo)
                    .additionalAttributes(Map.of("edit", info.chat().isGroup() && !info.fromMe() ? "8" : "7"))
                    .build();
            return socketHandler.sendMessage(request).thenApplyAsync(ignored -> info);
        }
        var range = createRange(info.chatJid(), false);
        var deleteMessageAction = DeleteMessageForMeAction.of(false, info.timestampSeconds());
        var syncAction = ActionValueSync.of(deleteMessageAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> info);
    }

    /**
     * Deletes a chat for this client and its companions using a modern version of Whatsapp Important:
     * this message doesn't seem to work always as of now
     *
     * @param chat the non-null chat to delete
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> delete(@NonNull T chat) {
        checkLoggedIn();
        var range = createRange(chat.toJid(), false);
        var deleteChatAction = DeleteChatAction.of(range);
        var syncAction = ActionValueSync.of(deleteChatAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 6, chat.toJid().toString(), "1");
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Clears the content of a chat for this client and its companions using a modern version of
     * Whatsapp Important: this message doesn't seem to work always as of now
     *
     * @param chat                the non-null chat to clear
     * @param keepStarredMessages whether starred messages in this chat should be kept
     * @return a CompletableFuture
     */
    public <T extends ContactJidProvider> CompletableFuture<T> clear(@NonNull T chat, boolean keepStarredMessages) {
        checkLoggedIn();
        var known = store().findChatByJid(chat);
        var range = createRange(chat.toJid(), true);
        var clearChatAction = ClearChatAction.of(range);
        var syncAction = ActionValueSync.of(clearChatAction);
        var request = PatchRequest.of(REGULAR_HIGH, syncAction, SET, 6, chat.toJid()
                .toString(), booleanToInt(keepStarredMessages), "0");
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Change the description of this business profile
     *
     * @param description the new description, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<String> changeBusinessDescription(String description) {
        return changeBusinessAttribute("description", description);
    }

    private CompletableFuture<String> changeBusinessAttribute(String key, String value) {
        checkLoggedIn();
        return socketHandler.sendQuery("set", "w:biz", Node.ofChildren("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.of(key, requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8))))
                .thenAcceptAsync(result -> checkBusinessAttributeConflict(key, value, result))
                .thenApplyAsync(ignored -> value);
    }

    private void checkBusinessAttributeConflict(String key, String value, Node result) {
        var keyNode = result.findNode("profile").flatMap(entry -> entry.findNode(key));
        if (keyNode.isEmpty()) {
            return;
        }
        var actual = keyNode.get()
                .contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing business %s response, something went wrong: %s".formatted(key, findErrorNode(result))));
        Validate.isTrue(value == null || value.equals(actual), "Cannot change business %s: conflict(expected %s, got %s)", key, value, actual);
    }

    /**
     * Change the address of this business profile
     *
     * @param address the new address, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<String> changeBusinessAddress(String address) {
        return changeBusinessAttribute("address", address);
    }

    /**
     * Change the email of this business profile
     *
     * @param email the new email, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<String> changeBusinessEmail(String email) {
        Validate.isTrue(email == null || Pattern.compile("^(.+)@(\\\\S+)$")
                .matcher(email)
                .matches(), "Invalid email: %s", email);
        return changeBusinessAttribute("email", email);
    }

    /**
     * Change the categories of this business profile
     *
     * @param categories the new categories, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCategory>> changeBusinessCategories(List<BusinessCategory> categories) {
        checkLoggedIn();
        return socketHandler.sendQuery("set", "w:biz", Node.ofChildren("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.ofChildren("categories", createCategories(categories))))
                .thenApplyAsync(ignored -> categories);
    }

    private Collection<Node> createCategories(List<BusinessCategory> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream().map(entry -> Node.ofAttributes("category", Map.of("id", entry.id()))).toList();
    }

    /**
     * Change the websites of this business profile
     *
     * @param websites the new websites, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<List<URI>> changeBusinessWebsites(List<URI> websites) {
        checkLoggedIn();
        return socketHandler.sendQuery("set", "w:biz", Node.ofChildren("business_profile", Map.of("v", "3", "mutation_type", "delta"), createWebsites(websites)))
                .thenApplyAsync(ignored -> websites);
    }

    private static List<Node> createWebsites(List<URI> websites) {
        if (websites == null) {
            return List.of();
        }
        return websites.stream()
                .map(entry -> Node.of("website", entry.toString().getBytes(StandardCharsets.UTF_8)))
                .toList();
    }

    /**
     * Query the catalog of this business
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog() {
        return queryBusinessCatalog(10);
    }

    /**
     * Query the catalog of this business
     *
     * @param productsLimit the maximum number of products to query
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog(int productsLimit) {
        return queryBusinessCatalog(store().userCompanionJid().toUserJid(), productsLimit);
    }

    /**
     * Query the catalog of a business
     *
     * @param contact       the business
     * @param productsLimit the maximum number of products to query
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog(@NonNull ContactJidProvider contact, int productsLimit) {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "w:biz:catalog", Node.ofChildren("product_catalog", Map.of("jid", contact, "allow_shop_source", "true"), Node.of("limit", String.valueOf(productsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))))
                .thenApplyAsync(this::parseCatalog);
    }

    private List<BusinessCatalogEntry> parseCatalog(Node result) {
        return Objects.requireNonNull(result, "Cannot query business catalog, missing response node")
                .findNode("product_catalog")
                .map(entry -> entry.findNodes("product"))
                .stream()
                .flatMap(Collection::stream)
                .map(BusinessCatalogEntry::of)
                .toList();
    }

    /**
     * Query the catalog of a business
     *
     * @param contact the business
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog(@NonNull ContactJidProvider contact) {
        return queryBusinessCatalog(contact, 10);
    }

    /**
     * Query the collections of this business
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<?> queryBusinessCollections() {
        return queryBusinessCollections(50);
    }

    /**
     * Query the collections of this business
     *
     * @param collectionsLimit the maximum number of collections to query
     * @return a CompletableFuture
     */
    public CompletableFuture<?> queryBusinessCollections(int collectionsLimit) {
        return queryBusinessCollections(store().userCompanionJid().toUserJid(), collectionsLimit);
    }

    /**
     * Query the collections of a business
     *
     * @param contact          the business
     * @param collectionsLimit the maximum number of collections to query
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCollectionEntry>> queryBusinessCollections(@NonNull ContactJidProvider contact, int collectionsLimit) {
        checkLoggedIn();
        return socketHandler.sendQuery("get", "w:biz:catalog", Map.of("smax_id", "35"), Node.ofChildren("collections", Map.of("biz_jid", contact), Node.of("collection_limit", String.valueOf(collectionsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("item_limit", String.valueOf(collectionsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))))
                .thenApplyAsync(this::parseCollections);
    }

    private List<BusinessCollectionEntry> parseCollections(Node result) {
        return Objects.requireNonNull(result, "Cannot query business collections, missing response node")
                .findNode("collections")
                .stream()
                .map(entry -> entry.findNodes("collection"))
                .flatMap(Collection::stream)
                .map(BusinessCollectionEntry::of)
                .toList();
    }

    /**
     * Query the collections of a business
     *
     * @param contact the business
     * @return a CompletableFuture
     */
    public CompletableFuture<?> queryBusinessCollections(@NonNull ContactJidProvider contact) {
        return queryBusinessCollections(contact, 50);
    }

    /**
     * Downloads a media from Whatsapp's servers. If the media is available, it will be returned
     * asynchronously. Otherwise, a retry request will be issued. If that also fails, an exception
     * will be thrown. The difference between this method and {@link MediaMessage#decodedMedia()} is
     * that this automatically attempts a retry request.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<byte[]> downloadMedia(@NonNull MessageInfo info) {
        checkLoggedIn();
        Validate.isTrue(info.message()
                .category() == MessageCategory.MEDIA, "Expected media message, got: %s(%s)", info.message()
                .category(), info.message().type());
        return downloadMedia(info, false);
    }

    private CompletableFuture<byte[]> downloadMedia(MessageInfo info, boolean retried) {
        var mediaMessage = (MediaMessage) info.message().content();
        var result = mediaMessage.decodedMedia();
        if (result.isEmpty()) {
            Validate.isTrue(!retried, "Media reupload failed");
            return requireMediaReupload(info).thenComposeAsync(entry -> downloadMedia(entry, true));
        }
        return CompletableFuture.completedFuture(result.get());
    }

    /**
     * Asks Whatsapp for a media reupload for a specific media
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<MessageInfo> requireMediaReupload(@NonNull MessageInfo info) {
        checkLoggedIn();
        Validate.isTrue(info.message()
                .category() == MessageCategory.MEDIA, "Expected media message, got: %s(%s)", info.message()
                .category(), info.message().type());
        var mediaMessage = (MediaMessage) info.message().content();
        var retryKey = Hkdf.extractAndExpand(mediaMessage.mediaKey(), "WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
        var retryIv = Bytes.ofRandom(12).toByteArray();
        var retryIdData = info.key().id().getBytes(StandardCharsets.UTF_8);
        var receipt = Protobuf.writeMessage(ServerErrorReceipt.of(info.id()));
        var ciphertext = AesGmc.encrypt(retryIv, receipt, retryKey, retryIdData);
        var rmrAttributes = Attributes.of()
                .put("jid", info.chatJid())
                .put("from_me", String.valueOf(info.fromMe()))
                .put("participant", info.senderJid(), () -> !Objects.equals(info.chatJid(), info.senderJid()))
                .toMap();
        var node = Node.ofChildren("receipt", Map.of("id", info.key().id(), "to", socketHandler.store()
                .userCompanionJid()
                .toUserJid(), "type", "server-error"), Node.ofChildren("encrypt", Node.of("enc_p", ciphertext), Node.of("enc_iv", retryIv)), Node.ofAttributes("rmr", rmrAttributes));
        return socketHandler.send(node, result -> result.hasDescription("notification"))
                .thenApplyAsync(result -> parseMediaReupload(info, mediaMessage, retryKey, retryIdData, result));
    }

    private MessageInfo parseMediaReupload(MessageInfo info, MediaMessage mediaMessage, byte[] retryKey, byte[] retryIdData, Node node) {
        Validate.isTrue(!node.hasNode("error"), "Erroneous response from media reupload: %s", node.attributes()
                .getInt("code"));
        var encryptNode = node.findNode("encrypt")
                .orElseThrow(() -> new NoSuchElementException("Missing encrypt node in media reupload"));
        var mediaPayload = encryptNode.findNode("enc_p")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new NoSuchElementException("Missing encrypted payload node in media reupload"));
        var mediaIv = encryptNode.findNode("enc_iv")
                .flatMap(Node::contentAsBytes)
                .orElseThrow(() -> new NoSuchElementException("Missing encrypted iv node in media reupload"));
        var mediaRetryNotificationData = AesGmc.decrypt(mediaIv, mediaPayload, retryKey, retryIdData);
        var mediaRetryNotification = Protobuf.readMessage(mediaRetryNotificationData, MediaRetryNotification.class);
        Validate.isTrue(mediaRetryNotification.directPath() != null, "Media retry upload failed: %s", mediaRetryNotification);
        mediaMessage.mediaUrl(Medias.createMediaUrl(mediaRetryNotification.directPath()));
        mediaMessage.mediaDirectPath(mediaRetryNotification.directPath());
        return info;
    }

    /**
     * Sends a custom node to Whatsapp
     *
     * @param node the non-null node to send
     * @return the response from Whatsapp
     */
    public CompletableFuture<Node> sendNode(@NonNull Node node) {
        return socketHandler.send(node);
    }

    /**
     * Creates a new community
     *
     * @param subject the non-null name of the new community
     * @param body    the nullable description of the new community
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> createCommunity(@NonNull String subject, String body) {
        checkLoggedIn();
        var entry = Node.ofChildren("create", Map.of("subject", subject), Node.ofChildren("description", Map.of("id", socketHandler.store()
                .nextTag()), Node.of("body", requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8))), Node.ofAttributes("parent", Map.of("default_membership_approval_mode", "request_required")));
        return socketHandler.sendQuery(GROUP.toJid(), "set", "w:g2", entry)
                .thenApplyAsync(response -> Optional.ofNullable(response)
                        .flatMap(node -> node.findNode("group"))
                        .orElseThrow(() -> new NoSuchElementException("Missing community response, something went wrong: %s".formatted(findErrorNode(response)))))
                .thenApplyAsync(GroupMetadata::of);
    }

    private void checkLoggedIn() {
        Validate.isTrue(store().userCompanionJid() != null,
                "You need to be logged in to execute this action");
    }
}
