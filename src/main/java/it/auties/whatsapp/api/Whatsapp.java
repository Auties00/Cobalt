package it.auties.whatsapp.api;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.business.*;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.call.CallBuilder;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessageBuilder;
import it.auties.whatsapp.model.message.standard.CallMessageBuilder;
import it.auties.whatsapp.model.message.standard.NewsletterAdminInviteMessageBuilder;
import it.auties.whatsapp.model.message.standard.ReactionMessageBuilder;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.mobile.AccountInfo;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.newsletter.*;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.*;
import it.auties.whatsapp.model.request.*;
import it.auties.whatsapp.model.response.*;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.PatchRequest.PatchEntry;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.socket.SocketHandler;
import it.auties.whatsapp.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.model.contact.ContactStatus.*;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Whatsapp {
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};
    private static final byte[] DEVICE_MOBILE_SIGNATURE_HEADER = {6, 2};
    private static final int COMPANION_PAIRING_TIMEOUT = 10;
    private static final int MAX_COMPANIONS = 5;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(\\S+)$");

    // The instances are added and removed when the client connects/disconnects
    // This is to make sure that the instances remain in memory only as long as it's needed
    private static final Map<UUID, Whatsapp> instances = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Jid, Boolean> usersCache = new ConcurrentHashMap<>();

    static Optional<Whatsapp> getInstanceByUuid(UUID uuid) {
        return Optional.ofNullable(instances.get(uuid));
    }

    static void removeInstanceByUuid(UUID uuid) {
        instances.remove(uuid);
    }

    /**
     * Checks if a connection exists
     *
     * @param uuid the non-null uuid
     * @return a boolean
     */
    public static boolean isConnected(UUID uuid) {
        return SocketHandler.isConnected(uuid);
    }

    /**
     * Checks if a connection exists
     *
     * @param phoneNumber the non-null phone number
     * @return a boolean
     */
    public static boolean isConnected(long phoneNumber) {
        return SocketHandler.isConnected(phoneNumber);
    }

    /**
     * Checks if a connection exists
     *
     * @param alias the non-null alias
     * @return a boolean
     */
    public static boolean isConnected(String alias) {
        return SocketHandler.isConnected(alias);
    }

    private final SocketHandler socketHandler;
    private final Set<Jid> trustedContacts;
    protected Whatsapp(Store store, Keys keys, ErrorHandler errorHandler, WebVerificationHandler webVerificationHandler) {
        this.socketHandler = new SocketHandler(this, store, keys, errorHandler, webVerificationHandler);
        this.trustedContacts = ConcurrentHashMap.newKeySet();
        handleDisconnections(store);
    }

    private void handleDisconnections(Store store) {
        addDisconnectedListener((reason) -> {
            if (reason != DisconnectReason.RECONNECTING) {
                removeInstanceByUuid(store.uuid());
            }
        });
    }

    /**
     * Creates a new web api
     * The web api is based around the WhatsappWeb client
     *
     * @return a web api builder
     */
    public static ConnectionBuilder<WebOptionsBuilder> webBuilder() {
        return new ConnectionBuilder<>(ClientType.WEB);
    }

    /**
     * Creates a new mobile api
     * The mobile api is based around the Whatsapp App available on IOS and Android
     *
     * @return a web mobile builder
     */
    public static ConnectionBuilder<MobileOptionsBuilder> mobileBuilder() {
        return new ConnectionBuilder<>(ClientType.MOBILE);
    }

    /**
     * Creates an advanced builder if you need more customization
     *
     * @return a custom builder
     */
    public static WhatsappCustomBuilder customBuilder() {
        return new WhatsappCustomBuilder();
    }

    /**
     * Connects to Whatsapp
     *
     * @return a future
     */
    public CompletableFuture<Whatsapp> connect() {
        var loggedIn = new CompletableFuture<Whatsapp>();
        addLoggedInListener(() -> loggedIn.complete(this));
        return socketHandler.connect(null)
                .thenRunAsync(() -> instances.put(store().uuid(), this))
                .thenCompose(ignored -> loggedIn);
    }

    /**
     * Waits for this session to be disconnected
     */
    public void awaitDisconnection() {
        var future = new CompletableFuture<Void>();
        addDisconnectedListener((reason) -> {
            if(reason != DisconnectReason.RECONNECTING) {
                future.complete(null);
            }
        });
        future.join();
    }

    /**
     * Returns whether the connection is active or not
     *
     * @return a boolean
     */
    public boolean isConnected() {
        return socketHandler.isConnected();
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
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public Store store() {
        return socketHandler.store();
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
     * saved credentials. The next time the API is used, the QR code will need to be scanned again.
     *
     * @return a future
     */
    public CompletableFuture<Void> logout() {
        if (jidOrThrowError() == null) {
            return socketHandler.disconnect(DisconnectReason.LOGGED_OUT);
        }

        var metadata = Map.of("jid", jidOrThrowError(), "reason", "user_initiated");
        var device = Node.of("remove-companion-device", metadata);
        return socketHandler.sendQuery("set", "md", device)
                .thenRun(() -> {});
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
    public final CompletableFuture<Void> changePrivacySetting(PrivacySettingType type, PrivacySettingValue value, JidProvider... excluded) {
        if (!type.isSupported(value)) {
            throw new IllegalArgumentException("Cannot change setting %s to %s: this toggle cannot be used because Whatsapp doesn't support it".formatted(value.name(), type.name()));
        }
        var attributes = Attributes.of()
                .put("name", type.data())
                .put("value", value.data())
                .put("dhash", "none", () -> value == PrivacySettingValue.CONTACTS_EXCEPT)
                .toMap();
        var excludedJids = Arrays.stream(excluded).map(JidProvider::toJid).toList();
        var children = value != PrivacySettingValue.CONTACTS_EXCEPT ? null : excludedJids.stream()
                .map(entry -> Node.of("user", Map.of("jid", entry, "action", "add")))
                .toList();
        return socketHandler.sendQuery("set", "privacy", Node.of("privacy", Node.of("category", attributes, children)))
                .thenRun(() -> onPrivacyFeatureChanged(type, value, excludedJids));
    }

    private void onPrivacyFeatureChanged(PrivacySettingType type, PrivacySettingValue value, List<Jid> excludedJids) {
        var newEntry = new PrivacySettingEntryBuilder()
                .type(type)
                .value(value)
                .excluded(excludedJids)
                .build();
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
    public CompletableFuture<Void> changeNewChatsEphemeralTimer(ChatEphemeralTimer timer) {
        return socketHandler.sendQuery("set", "disappearing_mode", Node.of("disappearing_mode", Map.of("duration", timer.period().toSeconds())))
                .thenRun(() -> store().setNewChatsEphemeralTimer(timer));
    }

    /**
     * Creates a new request to get a document containing all the data that was collected by Whatsapp
     * about this user. It takes three business days to receive it. To query the newsletters status, use
     * {@link Whatsapp#queryGdprAccountInfoStatus()}
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Void> createGdprAccountInfo() {
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("gdpr", Map.of("gdpr", "request")))
                .thenRun(() -> {});
    }

    /**
     * Queries the document containing all the data that was collected by Whatsapp about this user. To
     * create a request for this document, use {@link Whatsapp#createGdprAccountInfo()}
     *
     * @return the same instance wrapped in a completable future
     */
    // TODO: Implement ready and error states
    public CompletableFuture<GdprAccountReport> queryGdprAccountInfoStatus() {
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("gdpr", Map.of("gdpr", "status")))
                .thenApplyAsync(result -> GdprAccountReport.ofPending(result.attributes().getLong("timestamp")));
    }

    /**
     * Changes the name of this user
     *
     * @param newName the non-null new name
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Void> changeName(String newName) {
        if (store().clientType() == ClientType.WEB && store().device().platform().isBusiness()) {
            throw new IllegalArgumentException("The business name cannot be changed using the web api");
        }

        if (store().clientType() == ClientType.MOBILE && store().device().platform().isBusiness()) {
            var oldName = store().name();
            return socketHandler.updateBusinessCertificate(newName)
                    .thenRunAsync(() -> socketHandler.onUserChanged(newName, oldName));
        }

        var oldName = store().name();
        return socketHandler.sendNodeWithNoResponse(Node.of("presence", Map.of("name", newName, "type", "available")))
                .thenRunAsync(() -> socketHandler.onUserChanged(newName, oldName));
    }

    /**
     * Changes the about of this user
     *
     * @param newAbout the non-null new status
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Void> changeAbout(String newAbout) {
        return socketHandler.changeAbout(newAbout);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jids the contacts whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> subscribeToPresence(JidProvider... jids) {
        var futures = Arrays.stream(jids)
                .map(socketHandler::subscribeToPresence)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jids the contacts whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> subscribeToPresence(List<? extends JidProvider> jids) {
        var futures = jids.stream()
                .map(socketHandler::subscribeToPresence)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> subscribeToPresence(JidProvider jid) {
        return socketHandler.subscribeToPresence(jid);
    }

    /**
     * Remove a reaction from a message
     *
     * @param message the non-null message
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> removeReaction(MessageInfo<?> message) {
        return sendReaction(message, (String) null);
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendReaction(MessageInfo<?> message, Emoji reaction) {
        return sendReaction(message, Objects.toString(reaction));
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction. If a string that
     *                 isn't an emoji supported by Whatsapp is used, it will not get displayed
     *                 correctly. Use {@link Whatsapp#sendReaction(MessageInfo, Emoji)} if
     *                 you need a typed emoji enum.
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendReaction(MessageInfo<?> message, String reaction) {
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .chatJid(message.parentJid())
                .senderJid(message.senderJid())
                .fromMe(Objects.equals(message.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid()))
                .id(message.id())
                .build();
        var reactionMessage = new ReactionMessageBuilder()
                .key(key)
                .content(reaction)
                .timestampSeconds(Instant.now().toEpochMilli())
                .build();
        return sendChatMessage(message.parentJid(), MessageContainer.of(reactionMessage));
    }

    /**
     * Forwards a message to another chat
     *
     * @param chat the non-null chat
     * @param messageInfo the message to forward
     * @return a future
     */
    public CompletableFuture<ChatMessageInfo> forwardChatMessage(JidProvider chat, ChatMessageInfo messageInfo) {
        var message = messageInfo.message()
                .contentWithContext()
                .map(this::createForwardedMessage)
                .or(() -> createForwardedText(messageInfo))
                .orElseThrow(() -> new IllegalArgumentException("This message cannot be forwarded: " + messageInfo.message().type()));
        return sendChatMessage(chat, message);
    }

    private MessageContainer createForwardedMessage(ContextualMessage<?> messageWithContext) {
        var forwardingScore = messageWithContext.contextInfo()
                .map(ContextInfo::forwardingScore)
                .orElse(0);
        var contextInfo = new ContextInfoBuilder()
                .forwardingScore(forwardingScore + 1)
                .forwarded(true)
                .build();
        messageWithContext.setContextInfo(contextInfo);
        return MessageContainer.of(messageWithContext);
    }

    private Optional<MessageContainer> createForwardedText(ChatMessageInfo messageInfo) {
        if(!(messageInfo.message().content() instanceof TextMessage textMessage)) {
            return Optional.empty();
        }

        var contextInfo = new ContextInfoBuilder()
                .forwardingScore(1)
                .forwarded(true)
                .build();
        textMessage.setContextInfo(contextInfo);
        return Optional.ofNullable(MessageContainer.of(textMessage));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendMessage(JidProvider chat, String message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider chat, String message) {
        return sendChatMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<NewsletterMessageInfo> sendsNewsletterMessage(JidProvider chat, String message) {
        return sendNewsletterMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendMessage(JidProvider chat, String message, MessageInfo<?> quotedMessage) {
        return sendMessage(chat, TextMessage.of(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendChatMessage(JidProvider chat, String message, MessageInfo<?> quotedMessage) {
        return sendChatMessage(chat, TextMessage.of(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendNewsletterMessage(JidProvider chat, String message, MessageInfo<?> quotedMessage) {
        return sendNewsletterMessage(chat, TextMessage.of(message), quotedMessage);
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo<?> quotedMessage) {
        var contextInfo = ContextInfo.of(message.contextInfo().orElse(null), quotedMessage);
        message.setContextInfo(contextInfo);
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo<?> quotedMessage) {
        var contextInfo = ContextInfo.of(message.contextInfo().orElse(null), quotedMessage);
        message.setContextInfo(contextInfo);
        return sendChatMessage(chat, MessageContainer.of(message));
    }


    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat          the chat where the message should be sent
     * @param message       the message to send
     * @param quotedMessage the message to quote
     * @return a CompletableFuture
     */
    public CompletableFuture<NewsletterMessageInfo> sendNewsletterMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo<?> quotedMessage) {
        var contextInfo = ContextInfo.of(message.contextInfo().orElse(null), quotedMessage);
        message.setContextInfo(contextInfo);
        return sendNewsletterMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendMessage(JidProvider chat, Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo<?>> sendMessage(JidProvider recipient, MessageContainer message) {
        return recipient.toJid().server() == JidServer.newsletter() ? sendNewsletterMessage(recipient, message) : sendChatMessage(recipient, message);
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider recipient, MessageContainer message) {
        return sendChatMessage(recipient, message, true);
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @param compose   whether the chat state should be changed to composing
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider recipient, MessageContainer message, boolean compose) {
        if (recipient.toJid().hasServer(JidServer.newsletter())) {
            throw new IllegalArgumentException("Use sendNewsletterMessage to send a message in a newsletter");
        }
        var info = buildChatMessage(recipient, message);
        return sendMessage(info, compose);
    }

    private ChatMessageInfo buildChatMessage(JidProvider recipient, MessageContainer message) {
        var timestamp = Clock.nowSeconds();
        var deviceInfoMetadata = new DeviceListMetadataBuilder()
                .senderTimestamp(Clock.nowSeconds())
                .build();
        var deviceInfo = recipient.toJid().hasServer(JidServer.whatsapp()) ? new DeviceContextInfoBuilder()
                .deviceListMetadataVersion(2)
                .deviceListMetadata(deviceInfoMetadata)
                .build() : null;
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .chatJid(recipient.toJid())
                .fromMe(true)
                .senderJid(jidOrThrowError())
                .build();
        return new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(jidOrThrowError())
                .key(key)
                .message(message.withDeviceInfo(deviceInfo))
                .timestampSeconds(timestamp)
                .broadcast(recipient.toJid().hasServer(JidServer.broadcast()))
                .build();
    }

    private CompletableFuture<List<? extends JidProvider>> prepareChat(long timestamp, Set<Jid> recipients) {
        if(recipients == null || recipients.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return hasWhatsapp(recipients.toArray(Jid[]::new)).thenComposeAsync(result -> {
            var availableRecipients = result.entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .toList();
            if(store().clientType() == ClientType.WEB) {
                return CompletableFuture.completedFuture(availableRecipients);
            }

            var identityUsers = recipients.stream()
                    .map(user -> Node.of("user", Map.of("jid", user)))
                    .toList();
            return socketHandler.sendQuery("get", "encrypt", Node.of("identity", identityUsers))
                    .thenComposeAsync(response -> prepareRecipients(availableRecipients))
                    .thenComposeAsync(ignored -> queryPreparePic(availableRecipients))
                    .thenComposeAsync(ignored -> subscribeToPresence(availableRecipients))
                    .thenComposeAsync(ignored -> socketHandler.querySessions(availableRecipients))
                    .thenComposeAsync(ignored -> sendPrivacyTokens(timestamp, availableRecipients))
                    .thenApplyAsync(ignored -> {
                        trustedContacts.addAll(availableRecipients);
                        return availableRecipients;
                    });
        });
    }

    private CompletableFuture<Node> prepareRecipients(List<Jid> recipients) {
        var finalRecipients = recipients.stream()
                .filter(user -> !trustedContacts.contains(user))
                .toList();
        var users = finalRecipients.stream()
                .map(user -> Node.of(
                        "user",
                        Map.of("jid", user.toJid()),
                        Node.of(
                                "devices",
                                Map.of("device_hash", "2:" + Base64.getEncoder().encodeToString(Bytes.random(6)))
                        )
                ))
                .toList();
        var sync = Node.of(
                "usync",
                Map.of(
                        "mode", "query",
                        "context", "message",
                        "last", "true",
                        "sid", SocketHandler.randomSid(),
                        "index", "0"
                ),
                Node.of("query", Node.of("devices", Map.of("version", "2"))),
                Node.of(
                        "list",
                        users
                ),
                Node.of("side_list")
        );
        return socketHandler.sendQuery("get", "usync", sync).thenComposeAsync(response -> {
            var users2 = finalRecipients.stream()
                    .map(Jid::toPhoneNumber)
                    .flatMap(Optional::stream)
                    .map(phoneNumber -> Node.of("user", Node.of("contact", phoneNumber.getBytes())))
                    .toList();
            var sync2 = Node.of(
                    "usync",
                    Map.of(
                            "mode", "delta",
                            "allow_mutation", "true",
                            "context", "interactive",
                            "last", "true",
                            "sid", SocketHandler.randomSid(),
                            "index", "0"
                    ),
                    Node.of(
                            "query",
                            Node.of("business", Node.of("verified_name"), Node.of("profile", Map.of("v", 1396))),
                            Node.of("contact"),
                            Node.of("devices", Map.of("version", "2")),
                            Node.of("disappearing_mode"),
                            Node.of("sidelist"),
                            Node.of("status")
                    ),
                    Node.of(
                            "list",
                            users2
                    ),
                    Node.of("side_list")
            );
            return socketHandler.sendQuery("get", "usync", sync2);
        });
    }

    private CompletableFuture<?> sendPrivacyTokens(long timestamp, List<Jid> toPrepare) {
        var tokens = toPrepare.stream()
                .filter(user -> !trustedContacts.contains(user))
                .map(user -> socketHandler.sendQuery("set", "privacy", Node.of("tokens", Node.of("token", Map.of("t", timestamp, "jid", user.toJid(), "type", "trusted_contact")))))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(tokens);
    }

    private CompletableFuture<Void> queryPreparePic(List<Jid> availableMembers) {
        var futures = availableMembers.stream()
                .flatMap(entry -> Stream.of(
                        socketHandler.sendQuery("get", "w:profile:picture", Map.of("target", entry), Node.of("picture", Map.of("type", "preview"))),
                        socketHandler.sendQuery("get", "w:profile:picture", Map.of("target", entry), Node.of("picture", Map.of("type", "image")))
                ))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<List<Node>> getContactData(String phoneNumber) {
        var businessNode = Node.of("business", Node.of("verified_name"), Node.of("profile", Map.of("v", 372)));
        var contactNode = Node.of("contact");
        var lidNode = Node.of("lid");
        var userNode = Node.of("user", Node.of("contact", phoneNumber.getBytes()));
        return socketHandler.sendInteractiveQuery(List.of(businessNode, contactNode, lidNode), List.of(userNode), List.of());
    }


    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<NewsletterMessageInfo> sendNewsletterMessage(JidProvider recipient, MessageContainer message) {
        var newsletter = store()
                .findNewsletterByJid(recipient)
                .orElseThrow(() -> new IllegalArgumentException("Cannot send a message in a newsletter that you didn't join"));
        var oldServerId = newsletter.newestMessage()
                .map(NewsletterMessageInfo::serverId)
                .orElse(0);
        var info = new NewsletterMessageInfoBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .serverId(oldServerId + 1)
                .timestampSeconds(Clock.nowSeconds())
                .message(message)
                .status(MessageStatus.PENDING)
                .build();
        info.setNewsletter(newsletter);
        return sendMessage(info);
    }

    /**
     * Builds and sends an edited message
     *
     * @param oldMessage the message to edit
     * @param newMessage the new message's content
     * @return a CompletableFuture
     */
    public <T extends MessageInfo<T>> CompletableFuture<T> editMessage(T oldMessage, Message newMessage) {
        var oldMessageType = oldMessage.message().content().type();
        var newMessageType = newMessage.type();
        if (oldMessageType != newMessageType) {
            throw new IllegalArgumentException("Message type mismatch: %s != %s".formatted(oldMessageType, newMessageType));
        }
        return switch (oldMessage) {
            case NewsletterMessageInfo oldNewsletterInfo -> {
                var info = new NewsletterMessageInfoBuilder()
                        .id(oldNewsletterInfo.id())
                        .serverId(oldNewsletterInfo.serverId())
                        .timestampSeconds(Clock.nowSeconds())
                        .message(MessageContainer.ofEditedMessage(newMessage))
                        .status(MessageStatus.PENDING)
                        .build();
                info.setNewsletter(oldNewsletterInfo.newsletter());
                var request = new MessageRequest.Newsletter(info, Map.of("edit", getEditBit(info)));
                yield socketHandler.sendMessage(request)
                        .thenApply(ignored -> oldMessage);
            }
            case ChatMessageInfo oldChatInfo -> {
                var key = new ChatMessageKeyBuilder()
                        .id(oldChatInfo.id())
                        .chatJid(oldChatInfo.chatJid())
                        .fromMe(true)
                        .senderJid(jidOrThrowError())
                        .build();
                var info = new ChatMessageInfoBuilder()
                        .status(MessageStatus.PENDING)
                        .senderJid(jidOrThrowError())
                        .key(key)
                        .message(MessageContainer.ofEditedMessage(newMessage))
                        .timestampSeconds(Clock.nowSeconds())
                        .broadcast(oldChatInfo.chatJid().hasServer(JidServer.broadcast()))
                        .build();
                var request = new MessageRequest.Chat(info, null, false, false, Map.of("edit", getEditBit(info)));
                yield socketHandler.sendMessage(request)
                        .thenApply(ignored -> oldMessage);
            }
            default -> throw new IllegalStateException("Unsupported edit: " + oldMessage);
        };
    }

    public CompletableFuture<ChatMessageInfo> sendStatus(String message) {
        return sendStatus(MessageContainer.of(message));
    }

    public CompletableFuture<ChatMessageInfo> sendStatus(Message message) {
        return sendStatus(MessageContainer.of(message));
    }

    public CompletableFuture<ChatMessageInfo> sendStatus(MessageContainer message) {
        var timestamp = Clock.nowSeconds();
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .chatJid(Jid.of("status@broadcast"))
                .fromMe(true)
                .senderJid(jidOrThrowError())
                .build();
        var info = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(jidOrThrowError())
                .key(key)
                .timestampSeconds(timestamp)
                .broadcast(false)
                .build();
        return sendMessage(info, false);
    }

    /**
     * Sends a message to a chat
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendMessage(ChatMessageInfo info) {
        return sendMessage(info, true);
    }

    /**
     * Sends a message to a chat
     *
     * @param info the message to send
     * @param compose whether a compose status should be sent before sending the message
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendMessage(ChatMessageInfo info, boolean compose) {
        var recipient = info.chatJid();
        if (recipient.hasServer(JidServer.newsletter())) {
            throw new IllegalArgumentException("Use sendNewsletterMessage to send a message in a newsletter");
        }
        var timestamp = Clock.nowSeconds();
        return (recipient.hasServer(JidServer.whatsapp()) ? prepareChat(timestamp, Set.of(recipient)) : CompletableFuture.completedFuture(List.of(recipient))).thenComposeAsync(chatResult -> {
            if (chatResult.isEmpty()) {
                return CompletableFuture.completedFuture(info.setStatus(MessageStatus.ERROR));
            }

            return (compose ? changePresence(recipient, COMPOSING) : CompletableFuture.completedFuture(null))
                    .thenComposeAsync(ignored -> socketHandler.sendMessage(new MessageRequest.Chat(info)))
                    .thenComposeAsync(ignored -> compose ? pauseCompose(recipient) : CompletableFuture.completedFuture(null))
                    .thenApply(ignored -> info);
        });
    }

    private CompletableFuture<Void> pauseCompose(Jid chatJid) {
        var node = Node.of("chatstate",
                Map.of("to", chatJid),
                Node.of("paused"));
        return socketHandler.sendNodeWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updatePresence(chatJid, AVAILABLE));
    }


    /**
     * Sends a message to a newsletter
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<NewsletterMessageInfo> sendMessage(NewsletterMessageInfo info) {
        return socketHandler.sendMessage(new MessageRequest.Newsletter(info))
                .thenApply(ignored -> info);
    }

    /**
     * Marks a chat as read.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> markChatRead(JidProvider chat) {
        return mark(chat, true)
                .thenComposeAsync(ignored -> markAllAsRead(chat));
    }

    private CompletableFuture<Void> markAllAsRead(JidProvider chat) {
        var all = store()
                .findChatByJid(chat.toJid())
                .stream()
                .map(Chat::unreadMessages)
                .flatMap(Collection::stream)
                .map(this::markMessageRead)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(all);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> markChatUnread(JidProvider chat) {
        return mark(chat, false);
    }

    private CompletableFuture<Void> mark(JidProvider chat, boolean read) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat.toJid())
                    .ifPresent(entry -> entry.setMarkedAsUnread(read));
            return CompletableFuture.completedFuture(null);
        }

        var range = createRange(chat, false);
        var markAction = new MarkChatAsReadActionBuilder()
                .read(read)
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(markAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    private ActionMessageRangeSync createRange(JidProvider chat, boolean allMessages) {
        var known = store().findChatByJid(chat.toJid()).orElseGet(() -> store().addNewChat(chat.toJid()));
        return new ActionMessageRangeSync(known, allMessages);
    }

    /**
     * Marks a message as read
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> markMessageRead(ChatMessageInfo info) {
        var type = store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS)
                .value() == PrivacySettingValue.EVERYONE ? "read" : "read-self";
        socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), type);
        info.chat().ifPresent(chat -> {
            var count = chat.unreadMessagesCount();
            if (count > 0) {
                chat.setUnreadMessagesCount(count - 1);
            }
        });
        info.setStatus(MessageStatus.READ);
        return CompletableFuture.completedFuture(info);
    }

    /**
     * Awaits for a single newsletters to a message
     *
     * @param info the non-null message whose newsletters is pending
     * @return a non-null newsletters
     */
    public CompletableFuture<ChatMessageInfo> awaitMessageReply(ChatMessageInfo info) {
        return awaitMessageReply(info.id());
    }

    /**
     * Awaits for a single newsletters to a message
     *
     * @param id the non-null id of message whose newsletters is pending
     * @return a non-null newsletters
     */
    public CompletableFuture<ChatMessageInfo> awaitMessageReply(String id) {
        return store().addPendingReply(id);
    }

    /**
     * Executes a query to determine whether a user has an account on Whatsapp
     *
     * @param contact the contact to check
     * @return a CompletableFuture that wraps a non-null newsletters
     */
    public CompletableFuture<Boolean> hasWhatsapp(JidProvider contact) {
        return hasWhatsapp(new JidProvider[]{contact})
                .thenApply(result -> result.get(contact.toJid()));
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null map
     */
    public CompletableFuture<Map<Jid, Boolean>> hasWhatsapp(JidProvider... contacts) {
        var results = new HashMap<Jid, Boolean>();
        var todo = new ArrayList<Jid>();
        for (var contact : contacts) {
            var cached = usersCache.get(contact.toJid());
            if(cached != null) {
                results.put(contact.toJid(), cached);
            }else {
                todo.add(contact.toJid());
            }
        }
        if(todo.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.unmodifiableMap(results));
        }

        var jids = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(user -> !usersCache.containsKey(user))
                .toList();
        var contactNodes = jids.stream()
                .map(Jid::toPhoneNumber)
                .flatMap(Optional::stream)
                .map(phoneNumber -> Node.of("user", Node.of("contact", phoneNumber)))
                .toList();
        return socketHandler.sendInteractiveQuery(List.of(Node.of("contact")), contactNodes, List.of()).thenApplyAsync(result -> {
            var additionalResults = parseHasWhatsappResponse(jids, result);
            usersCache.putAll(additionalResults);
            results.putAll(additionalResults);
            return Collections.unmodifiableMap(results);
        });
    }

    private Map<Jid, Boolean> parseHasWhatsappResponse(List<Jid> contacts, List<Node> nodes) {
        var result = nodes.stream()
                .map(HasWhatsappResponse::ofNode)
                .collect(Collectors.toMap(HasWhatsappResponse::contact, HasWhatsappResponse::hasWhatsapp, (first, second) -> first, HashMap::new));
        contacts.stream()
                .filter(contact -> !result.containsKey(contact))
                .forEach(contact -> result.put(contact, false));
        return result;
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> queryBlockList() {
        return socketHandler.queryBlockList();
    }

    /**
     * Queries the display name of a contact
     *
     * @param contactJid the non-null contact
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<String>> queryName(JidProvider contactJid) {
        var contact = store().findContactByJid(contactJid);
        return contact.map(value -> CompletableFuture.completedFuture(value.chosenName()))
                .orElseGet(() -> queryNameFromServer(contactJid));
    }

    private CompletableFuture<Optional<String>> queryNameFromServer(JidProvider contactJid) {
        var request = UserRequests.chosenName(contactJid.toJid().user());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6556393721124826"), request))
                .thenApplyAsync(this::parseNameResponse);
    }

    @SuppressWarnings("OptionalIsPresent")
    private Optional<String> parseNameResponse(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return Optional.empty();
        }

        var content = result.get()
                .contentAsBytes();
        if(content.isEmpty()) {
            return Optional.empty();
        }

        return UserChosenNameResponse.ofJson(content.get())
                .flatMap(UserChosenNameResponse::name);
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status newsletters
     */
    public CompletableFuture<Optional<UserAboutResponse>> queryAbout(JidProvider chat) {
        return socketHandler.queryAbout(chat);
    }

    /**
     * Queries the profile picture
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public CompletableFuture<Optional<URI>> queryPicture(JidProvider chat) {
        return socketHandler.queryPicture(chat);
    }

    /**
     * Queries the metadata of a chat
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<ChatMetadata>> queryChatMetadata(JidProvider chat) {
        if(!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return socketHandler.queryGroupMetadata(chat.toJid())
                .thenApply(Optional::of)
                .exceptionally(ignored -> Optional.empty());
    }

    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMetadata> queryGroupMetadata(JidProvider chat) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        return socketHandler.queryGroupMetadata(chat.toJid()).thenApply(result -> {
            if (result.isCommunity()) {
                throw new IllegalArgumentException("Expected a group: use queryCommunityMetadata for a community or queryChatMetadata");
            }
            return result;
        });
    }

    /**
     * Queries this account's info
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<AccountInfo> queryAccountInfo() {
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("account")).thenApplyAsync(result -> {
            var accoutNode = result.findChild("account")
                    .orElseThrow(() -> new NoSuchElementException("Missing account node: " + result));
            var lastRegistration = Clock.parseSeconds(accoutNode.attributes().getLong("last_reg"))
                    .orElseThrow(() -> new NoSuchElementException("Missing account last_reg: " + accoutNode));
            var creation = Clock.parseSeconds(accoutNode.attributes().getLong("creation"))
                    .orElseThrow(() -> new NoSuchElementException("Missing account creation: " + accoutNode));
            return new AccountInfo(lastRegistration, creation);
        });
    }

    /**
     * Queries a business profile, if available
     *
     * @param contact the target contact
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<BusinessProfile>> queryBusinessProfile(JidProvider contact) {
        return socketHandler.sendQuery("get", "w:biz", Node.of("business_profile", Map.of("v", 116),
                        Node.of("profile", Map.of("jid", contact.toJid()))))
                .thenApplyAsync(this::getBusinessProfile);
    }

    private Optional<BusinessProfile> getBusinessProfile(Node result) {
        return result.findChild("business_profile")
                .flatMap(entry -> entry.findChild("profile"))
                .map(BusinessProfile::of);
    }

    /**
     * Queries all the known business categories
     *
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCategory>> queryBusinessCategories() {
        return socketHandler.queryBusinessCategories();
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<String> queryGroupInviteCode(JidProvider chat) {
        return socketHandler.sendQuery(chat.toJid(), "get", "w:g2", Node.of("invite"))
                .thenApplyAsync(this::parseInviteCode);
    }

    private String parseInviteCode(Node result) {
        return result.findChild("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite code in invite newsletters"))
                .attributes()
                .getRequiredString("code");
    }

     /**
     * Queries the invite link of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<String> queryGroupInviteLink(JidProvider chat) {
        return queryGroupInviteCode(chat)
                .thenApplyAsync("https://chat.whatsapp.com/%s"::formatted);
    }

    /**
     * Queries the lists of participants currently waiting to be accepted into the group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> queryGroupParticipantsPendingApproval(JidProvider chat) {
        return socketHandler.sendQuery(chat.toJid(), "get", "w:g2", Node.of("membership_approval_requests"))
                .thenApplyAsync(this::parseParticipantsPendingApproval);
    }

    private List<Jid> parseParticipantsPendingApproval(Node node) {
        return node.findChild("membership_approval_requests")
                .stream()
                .map(requests -> requests.listChildren("membership_approval_request"))
                .flatMap(Collection::stream)
                .map(participant -> participant.attributes().getRequiredJid("user"))
                .toList();
    }

    /**
     * Changes the approval request status of an array of participants for a group
     *
     * @param chat the target group
     * @param approve whether the participants should be accepted into the group
     * @param participants the target participants
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> changeGroupParticipantPendingApprovalStatus(JidProvider chat, boolean approve, JidProvider... participants) {
        var participantsNodes = Arrays.stream(participants)
                .map(participantJid -> Node.of("participant", Map.of("jid", participantJid)))
                .toList();
        var action = approve ? "approve" : "reject";
        return socketHandler.sendQuery(chat.toJid(), "set", "w:g2", Node.of("membership_requests_action", Node.of(action, participantsNodes)))
                .thenApplyAsync(result -> parseParticipantsPendingApprovalChange(result, action));
    }

    private List<Jid> parseParticipantsPendingApprovalChange(Node node, String action) {
        return node.findChild("membership_requests_action")
                .flatMap(response -> response.findChild(action))
                .map(requests -> requests.listChildren("participant"))
                .stream()
                .flatMap(Collection::stream)
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getRequiredJid("jid"))
                .toList();
    }

    /**
     * Revokes the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> revokeGroupInvite(JidProvider chat) {
        return socketHandler.sendQuery(chat.toJid(), "set", "w:g2", Node.of("invite"))
                .thenRun(() -> {});
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite countryCode
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<Chat>> acceptGroupInvite(String inviteCode) {
        return socketHandler.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", Node.of("invite", Map.of("code", inviteCode)))
                .thenApplyAsync(this::parseAcceptInvite);
    }

    private Optional<Chat> parseAcceptInvite(Node result) {
        return result.findChild("group")
                .flatMap(group -> group.attributes().getOptionalJid("jid"))
                .map(jid -> store().findChatByJid(jid).orElseGet(() -> store().addNewChat(jid)));
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePresence(boolean available) {
        var status = socketHandler.store().online();
        if (status == available) {
            return CompletableFuture.completedFuture(null);
        }

        var presence = available ? AVAILABLE : UNAVAILABLE;
        var node = Node.of("presence", Map.of("name", store().name(), "type", presence.toString()));
        return socketHandler.sendNodeWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updatePresence(null, presence));
    }

    private void updatePresence(JidProvider chatJid, ContactStatus presence) {
        if (chatJid == null) {
            store().setOnline(presence == AVAILABLE);
        }

        var self = store().findContactByJid(jidOrThrowError().toSimpleJid());
        if (self.isEmpty()) {
            return;
        }

        self.get().setLastKnownPresence(presence);

        if (chatJid != null) {
            store().findChatByJid(chatJid)
                    .ifPresent(chat -> chat.addPresence(self.get().jid(), presence));
        }

        self.get().setLastSeen(ZonedDateTime.now());
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chatJid  the target chat
     * @param presence the new status
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changePresence(JidProvider chatJid, ContactStatus presence) {
        if (presence == COMPOSING || presence == RECORDING) {
            var node = Node.of("chatstate",
                    Map.of("to", chatJid.toJid()),
                    Node.of(COMPOSING.toString(), presence == RECORDING ? Map.of("media", "audio") : Map.of()));
            return socketHandler.sendNodeWithNoResponse(node)
                    .thenAcceptAsync(socketHandler -> updatePresence(chatJid, presence));
        }

        var node = Node.of("presence", Map.of("type", presence.toString(), "name", store().name()));
        return socketHandler.sendNodeWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updatePresence(chatJid, presence));
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> promoteGroupParticipants(JidProvider group, JidProvider... contacts) {
        return queryGroupMetadata(group.toJid())
                .thenComposeAsync(metadata -> {
                    if (metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a group: use promoteCommunityParticipants for communities");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(participantsSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(group, false, GroupAction.PROMOTE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot promote participant in group", error);
                });
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> demoteGroupParticipants(JidProvider group, JidProvider... contacts) {
        return queryGroupMetadata(group.toJid())
                .thenComposeAsync(metadata -> {
                    if (metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a group: use demoteCommunityParticipants for communities");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(participantsSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(group, false, GroupAction.DEMOTE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot demote participant in group", error);
                });
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> addGroupParticipants(JidProvider group, JidProvider... contacts) {
        return queryGroupMetadata(group.toJid())
                .thenComposeAsync(metadata -> {
                    if (metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a group: use addCommunityParticipants for communities");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(entry -> !participantsSet.contains(entry))
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(group, false, GroupAction.ADD, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot add participant to group", error);
                });
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> removeGroupParticipants(JidProvider group, JidProvider... contacts) {
        return queryGroupMetadata(group.toJid())
                .thenComposeAsync(metadata -> {
                    if (metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a group: use removeCommunityParticipants for communities");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(participantsSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(group, false, GroupAction.REMOVE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot remove participant from group", error);
                });
    }

    private CompletableFuture<List<Jid>> executeActionOnParticipants(JidProvider group, boolean community, GroupAction action, Set<Jid> jids) {
        return prepareActionOnGroupParticipant(action, community, jids).thenComposeAsync(ignored -> {
            var participants = jids.stream()
                    .map(JidProvider::toJid)
                    .map(jid -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(jid, "Cannot execute action on yourself"))))
                    .toArray(Node[]::new);
            return socketHandler.sendQuery(group.toJid(), "set", "w:g2", Node.of(action.data(), participants))
                    .thenApplyAsync(result -> parseGroupActionResponse(result, group, action));
        });
    }

    private CompletableFuture<?> prepareActionOnGroupParticipant(GroupAction action, boolean community, Set<Jid> jids) {
        if (action == GroupAction.ADD && !community) {
            sendGroupWam(Clock.nowSeconds());
            return prepareChat(Clock.nowSeconds(), jids);
        }

        return CompletableFuture.completedFuture(null);
    }

    private Jid checkGroupParticipantJid(Jid jid, String errorMessage) {
        if (Objects.equals(jid.toSimpleJid(), jidOrThrowError().toSimpleJid())) {
            throw new IllegalArgumentException(errorMessage);
        }

        return jid;
    }

    private List<Jid> parseGroupActionResponse(Node result, JidProvider groupJid, GroupAction action) {
        return result.findChild(action.data())
                .map(body -> body.listChildren("participant"))
                .stream()
                .flatMap(Collection::stream)
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Changes the name of a group
     *
     * @param group   the target group
     * @param newName the new name for the group
     * @return a CompletableFuture
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public CompletableFuture<Void> changeGroupSubject(JidProvider group, String newName) {
        if (newName != null && newName.isBlank()) {
            throw new IllegalArgumentException("Empty subjects are not allowed");
        }
        var body = Node.of("subject", newName.getBytes(StandardCharsets.UTF_8));
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenRun(() -> {});
    }

    /**
     * Changes the description of a group
     *
     * @param group       the target group
     * @param description the new name for the group, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeGroupDescription(JidProvider group, String description) {
        return socketHandler.queryGroupMetadata(group.toJid())
                .thenApplyAsync(ChatMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeGroupDescription(group, description, descriptionId.orElse(null)))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> changeGroupDescription(JidProvider group, String description, String descriptionId) {
        var descriptionNode = Optional.ofNullable(description)
                .map(content -> Node.of("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", SocketHandler.randomSid(), () -> description != null)
                .put("delete", true, () -> description == null)
                .put("prev", descriptionId, () -> descriptionId != null)
                .toMap();
        var body = Node.of("description", attributes, descriptionNode);
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenRun(() -> {});
    }

    /**
     * Changes a group setting
     *
     * @param group   the non-null group affected by this change
     * @param setting the non-null setting
     * @param policy  the non-null policy
     * @return a future
     */
    public CompletableFuture<Void> changeGroupSetting(JidProvider group, GroupSetting setting, ChatSettingPolicy policy) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("This method only accepts groups");
        }
        var body = switch (setting) {
            case EDIT_GROUP_INFO -> Node.of(policy == ChatSettingPolicy.ADMINS ? "locked" : "unlocked");
            case SEND_MESSAGES -> Node.of(policy == ChatSettingPolicy.ADMINS ? "announcement" : "not_announcement");
            case ADD_PARTICIPANTS ->
                    Node.of("member_add_mode", policy == ChatSettingPolicy.ADMINS ? "admin_add".getBytes(StandardCharsets.UTF_8) : "all_member_add".getBytes(StandardCharsets.UTF_8));
            case APPROVE_PARTICIPANTS ->
                    Node.of("membership_approval_mode", Node.of("group_join", Map.of("state", policy == ChatSettingPolicy.ADMINS ? "on" : "off")));
        };
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenRun(() -> {});
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeProfilePicture(byte[] image) {
        var data = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), data);
        return switch (store().clientType()) {
            case WEB -> socketHandler.sendQuery("set", "w:profile:picture", body)
                    .thenRun(() -> {});
            case MOBILE -> socketHandler.sendQuery(jidOrThrowError(), "set", "w:profile:picture", body)
                    .thenRun(() -> {});
        };
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeGroupPicture(JidProvider group, URI image) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var proxy = store().proxy()
                .filter(ignored -> store().mediaProxySetting().allowsDownloads())
                .orElse(null);
        return Medias.downloadAsync(image, proxy)
                .thenComposeAsync(imageResult -> changeGroupPicture(group, imageResult));
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeGroupPicture(JidProvider group, byte[] image) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), profilePic);
        return socketHandler.sendQuery("set", "w:profile:picture", Map.of("target", group.toJid()), body)
                .thenRun(() -> {});
    }

    /**
     * Creates a new group
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<ChatMetadata>> createGroup(String subject, JidProvider... contacts) {
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
    public CompletableFuture<Optional<ChatMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider... contacts) {
        return createGroup(subject, timer, null, contacts);
    }

    /**
     * Creates a new group
     *
     * @param subject     the new group's name
     * @param parentCommunity the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<ChatMetadata>> createCommunityGroup(String subject, JidProvider parentCommunity) {
        return createGroup(subject, ChatEphemeralTimer.OFF, parentCommunity, new JidProvider[0]);
    }
    
    /**
     * Creates a new group
     *
     * @param subject     the new group's name
     * @param timer       the default ephemeral timer for messages sent in this group
     * @param parentCommunity the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<ChatMetadata>> createCommunityGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity) {
        return createGroup(subject, timer, parentCommunity, new JidProvider[0]);
    }
    
    private CompletableFuture<Optional<ChatMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity, JidProvider... contacts) {
        var timestamp = Clock.nowSeconds();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("The subject of a group cannot be blank");
        }
        if(parentCommunity == null && contacts.length < 1) {
            throw new IllegalArgumentException("Expected at least 1 member for this group");
        }
        var contactsJids = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .collect(Collectors.toUnmodifiableSet());
        return prepareChat(timestamp, contactsJids).thenComposeAsync(availableMembers -> {
            var children = new ArrayList<Node>();
            if (parentCommunity != null) {
                children.add(Node.of("linked_parent", Map.of("jid", parentCommunity.toJid())));
            }
            if (timer != ChatEphemeralTimer.OFF) {
                children.add(Node.of("ephemeral", Map.of("expiration", timer.periodSeconds())));
            }
            children.add(Node.of("member_add_mode", "all_member_add".getBytes(StandardCharsets.UTF_8)));
            children.add(Node.of("membership_approval_mode", Node.of("group_join", Map.of("state", "off"))));
            availableMembers.stream()
                    .map(JidProvider::toJid)
                    .map(Jid::toSimpleJid)
                    .distinct()
                    .map(contact -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(contact.toJid(), "Cannot create group with yourself as a participant"))))
                    .forEach(children::add);
            var body = Node.of("create", Map.of("subject", subject, "key", timestamp), children);
            var future = socketHandler.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", body);
            sendGroupWam(timestamp);
            return future.thenComposeAsync(this::parseGroupResult);
        });
    }

    private void sendGroupWam(long timestamp) {
        var wamBinary = "57414d0501020001200b800d086950686f6e652058800f0631362e372e34801109322e32342e342e373810152017502f0dd9e065206928830138790604387b060288eb0a0361746e887911904246342c316a332c55772c79492c39442c31552c45722c31432c41472c324a2c49662c35552c4f582c31462c352c41792c38772c4c442c414a2c35362c642c346f2c466d2c37512c36392c32442c332c31762c33772c337a2c31332c7a2c512c722c33752c32652c522c6f2c36662c502c692c572c372c562c4b2c382c31532c4c2c31362c31702c742c6d2c32382c5088a5134632343835312c32343336362c32313031382c32333939332c32333633302c31373832352c31373833302c32353530382c32353530302c363633372c32323634392c3233363237186b1828a71c88911e063230483234308879240431372e3018ed3318ab3888fb3c0935363936333037343129b4072601";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone X", socketHandler.store().device().model().replaceAll("_", " "))
                .replace("16.7.4", socketHandler.store().device().osVersion().toString())
                .replace("2.24.4.78", socketHandler.store().version().toString())
                .getBytes();
        var addNode = Node.of("add", Map.of("t", timestamp), wamData);
        socketHandler.sendQuery("set", "w:stats", addNode);
    }

    /**
     * Syncs any number of contacts with whatsapp
     *
     * @param contacts the contacts to sync
     * @return the contacts that were successfully synced
     */
    public CompletableFuture<List<Jid>> addContacts(JidProvider... contacts) {
        var users = Arrays.stream(contacts)
                .filter(entry -> entry.toJid().hasServer(JidServer.whatsapp()) && !store().hasContact(entry))
                .map(contact -> contact.toJid().toPhoneNumber())
                .flatMap(Optional::stream)
                .map(phoneNumber -> Node.of("user", Node.of("contact", phoneNumber.getBytes())))
                .toList();
        if(users.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        var sync = Node.of(
                "usync",
                Map.of(
                        "context", "add",
                        "index", "0",
                        "last", "true",
                        "mode", "delta",
                        "sid", SocketHandler.randomSid()
                ),
                Node.of(
                        "query",
                        Node.of("business", Node.of("verified_name"), Node.of("profile", Map.of("v", 372))),
                        Node.of("contact"),
                        Node.of("devices", Map.of("version", "2")),
                        Node.of("disappearing_mode"),
                        Node.of("sidelist"),
                        Node.of("status")
                ),
                Node.of(
                        "list",
                        users
                ),
                Node.of("side_list")
        );
        return socketHandler.sendQuery(store().jid().orElseThrow(), "get", "usync", sync)
                .thenApplyAsync(this::parseAddedContacts);
    }

    private List<Jid> parseAddedContacts(Node result) {
        return result.findChild("usync")
                .flatMap(usync -> usync.findChild("list"))
                .map(list -> list.listChildren("user"))
                .stream()
                .flatMap(Collection::stream)
                .map(this::parseAddedContact)
                .toList();
    }

    private Jid parseAddedContact(Node user) {
        var jid = user.attributes().getOptionalJid("jid");
        if(jid.isEmpty()) {
            return null;
        }

        var contactNode = user.findChild("contact");
        if(contactNode.isEmpty() || !contactNode.get().attributes().hasValue("type", "in")) {
            return null;
        }

        store().addContact(jid.get());
        return jid.get();
    }

    private String findErrorNode(Node result) {
        return Optional.ofNullable(result)
                .flatMap(node -> node.findChild("error"))
                .map(Node::toString)
                .orElseGet(() -> Objects.toString(result));
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public CompletableFuture<Void> leaveGroup(JidProvider group) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group");
        }
        var body = Node.of("leave", Node.of("group", Map.of("id", group.toJid())));
        return socketHandler.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", body)
                .thenAcceptAsync(ignored -> handleLeaveGroup(group));
    }

    private void handleLeaveGroup(JidProvider group) {
        var jid = jidOrThrowError().toSimpleJid();
        var pastParticipant = new ChatPastParticipantBuilder()
                .jid(jid)
                .reason(ChatPastParticipant.Reason.REMOVED)
                .timestampSeconds(Clock.nowSeconds())
                .build();
        socketHandler.addPastParticipant(group.toJid(), pastParticipant);
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> muteChat(JidProvider chat) {
        return muteChat(chat, ChatMute.muted());
    }

    /**
     * Mutes a chat
     *
     * @param chat the target chat
     * @param mute the type of mute
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> muteChat(JidProvider chat, ChatMute mute) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(mute));
            return CompletableFuture.completedFuture(null);
        }

        var endTimeStamp = mute.type() == ChatMute.Type.MUTED_FOR_TIMEFRAME ? mute.endTimeStamp() * 1000L : mute.endTimeStamp();
        var muteAction = new MuteActionBuilder()
                .muted(true)
                .muteEndTimestampSeconds(endTimeStamp)
                .autoMuted(false)
                .build();
        var syncAction = ActionValueSync.of(muteAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unmuteChat(JidProvider chat) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(ChatMute.notMuted()));
            return CompletableFuture.completedFuture(null);
        }

        var muteAction = new MuteActionBuilder()
                .muted(false)
                .muteEndTimestampSeconds(0)
                .autoMuted(false)
                .build();
        var syncAction = ActionValueSync.of(muteAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    /**
     * Blocks a contact
     *
     * @param contact the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> blockContact(JidProvider contact) {
        var body = Node.of("item", Map.of("action", "block", "jid", contact.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body)
                .thenRun(() -> {});
    }

    /**
     * Unblocks a contact
     *
     * @param contact the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unblockContact(JidProvider contact) {
        var body = Node.of("item", Map.of("action", "unblock", "jid", contact.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body)
                .thenRun(() -> {});
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled
     * in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeEphemeralTimer(JidProvider chat, ChatEphemeralTimer timer) {
        return switch (chat.toJid().server().type()) {
            case WHATSAPP -> {
                var message = new ProtocolMessageBuilder()
                        .protocolType(ProtocolMessage.Type.EPHEMERAL_SETTING)
                        .ephemeralExpirationSeconds(timer.period().toSeconds())
                        .build();
                yield sendMessage(chat, message)
                        .thenRun(() -> {});
            }
            case GROUP_OR_COMMUNITY -> {
                var body = timer == ChatEphemeralTimer.OFF ? Node.of("not_ephemeral") : Node.of("ephemeral", Map.of("expiration", timer.period()
                        .toSeconds()));
                yield socketHandler.sendQuery(chat.toJid(), "set", "w:g2", body)
                        .thenRun(() -> {});
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
    public CompletableFuture<ChatMessageInfo> markMessagePlayed(ChatMessageInfo info) {
        if (store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS).value() != PrivacySettingValue.EVERYONE) {
            return CompletableFuture.completedFuture(info);
        }
        socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), "played");
        info.setStatus(MessageStatus.PLAYED);
        return CompletableFuture.completedFuture(info);
    }

    /**
     * Pins a chat to the top. A maximum of three chats can be pinned to the top. This condition can
     * be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> pinChat(JidProvider chat) {
        return pinChat(chat, true);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unpinChat(JidProvider chat) {
        return pinChat(chat, false);
    }

    private CompletableFuture<Void> pinChat(JidProvider chat, boolean pin) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setPinnedTimestampSeconds(pin ? (int) Clock.nowSeconds() : 0));
            return CompletableFuture.completedFuture(null);
        }
        var pinAction = new PinActionBuilder()
                .pinned(pin)
                .build();
        var syncAction = ActionValueSync.of(pinAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    /**
     * Stars a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> starMessage(ChatMessageInfo info) {
        return starMessage(info, true);
    }

    private CompletableFuture<ChatMessageInfo> starMessage(ChatMessageInfo info, boolean star) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            info.setStarred(star);
            return CompletableFuture.completedFuture(info);
        }

        var starAction = new StarActionBuilder()
                .starred(star)
                .build();
        var syncAction = ActionValueSync.of(starAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> info);
    }

    private String fromMeToFlag(MessageInfo<?> info) {
        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        return booleanToInt(fromMe);
    }

    private String participantToFlag(MessageInfo<?> info) {
        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        return info.parentJid().hasServer(JidServer.groupOrCommunity())
                && !fromMe ? info.senderJid().toString() : "0";
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
    public CompletableFuture<ChatMessageInfo> unstarMessage(ChatMessageInfo info) {
        return starMessage(info, false);
    }

    /**
     * Archives a chat. If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> archiveChat(JidProvider chat) {
        return archiveChat(chat, true);
    }

    private CompletableFuture<Void> archiveChat(JidProvider chat, boolean archive) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setArchived(archive));
            return CompletableFuture.completedFuture(null);
        }

        var range = createRange(chat, false);
        var archiveAction = new ArchiveChatActionBuilder()
                .archived(archive)
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(archiveAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> unarchive(JidProvider chat) {
        return archiveChat(chat, false);
    }

    /**
     * Deletes a message
     *
     * @param messageInfo the non-null message to delete
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> deleteMessage(NewsletterMessageInfo messageInfo) {
        var revokeInfo = new NewsletterMessageInfoBuilder()
                .id(messageInfo.id())
                .serverId(messageInfo.serverId())
                .timestampSeconds(Clock.nowSeconds())
                .message(MessageContainer.empty())
                .status(MessageStatus.PENDING)
                .build();
        revokeInfo.setNewsletter(messageInfo.newsletter());
        var attrs = Map.of("edit", getDeleteBit(messageInfo));
        var request = new MessageRequest.Newsletter(revokeInfo, attrs);
        return socketHandler.sendMessage(request);
    }

    /**
     * Deletes a message
     *
     * @param messageInfo non-null message to delete
     * @param everyone    whether the message should be deleted for everyone or only for this client and
     *                    its companions
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> deleteMessage(ChatMessageInfo messageInfo, boolean everyone) {
        if (everyone) {
            var message = new ProtocolMessageBuilder()
                    .protocolType(ProtocolMessage.Type.REVOKE)
                    .key(messageInfo.key())
                    .build();
            var sender = messageInfo.chatJid().hasServer(JidServer.groupOrCommunity()) ? jidOrThrowError() : null;
            var key = new ChatMessageKeyBuilder()
                    .id(ChatMessageKey.randomId(store().clientType()))
                    .chatJid(messageInfo.chatJid())
                    .fromMe(true)
                    .senderJid(sender)
                    .build();
            var revokeInfo = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING)
                    .senderJid(sender)
                    .key(key)
                    .message(MessageContainer.of(message))
                    .timestampSeconds(Clock.nowSeconds())
                    .build();
            var attrs = Map.of("edit", getDeleteBit(messageInfo));
            var request = new MessageRequest.Chat(revokeInfo, null, false, false, attrs);
            return socketHandler.sendMessage(request);
        }

        return switch (store().clientType()) {
            case WEB -> {
                var range = createRange(messageInfo.chatJid(), false);
                var deleteMessageAction = new DeleteMessageForMeActionBuilder()
                        .deleteMedia(false)
                        .messageTimestampSeconds(messageInfo.timestampSeconds().orElse(0L))
                        .build();
                var syncAction = ActionValueSync.of(deleteMessageAction);
                var entry = PatchEntry.of(syncAction, Operation.SET, messageInfo.chatJid().toString(), messageInfo.id(), fromMeToFlag(messageInfo), participantToFlag(messageInfo));
                var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
                yield socketHandler.pushPatch(request);
            }
            case MOBILE -> {
                // TODO: Send notification to companions
                messageInfo.chat().ifPresent(chat -> chat.removeMessage(messageInfo));
                yield CompletableFuture.completedFuture(null);
            }
        };
    }


    private int getEditBit(MessageInfo<?> info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 3;
        }

        return 1;
    }

    private int getDeleteBit(MessageInfo<?> info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 8;
        }

        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        if (info.parentJid().hasServer(JidServer.groupOrCommunity()) && !fromMe) {
            return 8;
        }

        return 7;
    }

    /**
     * Deletes a chat for this client and its companions using a modern version of Whatsapp Important:
     * this message doesn't seem to work always as of now
     *
     * @param chat the non-null chat to delete
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> deleteChat(JidProvider chat) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().removeChat(chat.toJid());
            return CompletableFuture.completedFuture(null);
        }

        var range = createRange(chat.toJid(), false);
        var deleteChatAction = new DeleteChatActionBuilder()
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(deleteChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString(), "1");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request);
    }

    /**
     * Clears the content of a chat for this client and its companions using a modern version of
     * Whatsapp Important: this message doesn't seem to work always as of now
     *
     * @param chat                the non-null chat to clear
     * @param keepStarredMessages whether starred messages in this chat should be kept
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> clearChat(JidProvider chat, boolean keepStarredMessages) {
        if (store().clientType() == ClientType.MOBILE) {
            // TODO: Send notification to companions
            store().findChatByJid(chat.toJid())
                    .ifPresent(Chat::removeMessages);
            return CompletableFuture.completedFuture(null);
        }

        var known = store().findChatByJid(chat);
        var range = createRange(chat.toJid(), true);
        var clearChatAction = new ClearChatActionBuilder()
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(clearChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString(), booleanToInt(keepStarredMessages), "0");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request);
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
        return socketHandler.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.of(key, Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8))))
                .thenAcceptAsync(result -> checkBusinessAttributeConflict(key, value, result))
                .thenApplyAsync(ignored -> value);
    }

    private void checkBusinessAttributeConflict(String key, String value, Node result) {
        var keyNode = result.findChild("profile").flatMap(entry -> entry.findChild(key));
        if (keyNode.isEmpty()) {
            return;
        }
        var actual = keyNode.get()
                .contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing business %s newsletters, something went wrong: %s".formatted(key, findErrorNode(result))));
        if (value != null && !value.equals(actual)) {
            throw new IllegalArgumentException("Cannot change business %s: conflict(expected %s, got %s)".formatted(key, value, actual));
        }
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
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        return changeBusinessAttribute("email", email);
    }

    /**
     * Change the categories of this business profile
     *
     * @param categories the new categories, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCategory>> changeBusinessCategories(List<BusinessCategory> categories) {
        return socketHandler.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.of("categories", createCategories(categories))))
                .thenApplyAsync(ignored -> categories);
    }

    private Collection<Node> createCategories(List<BusinessCategory> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream().map(entry -> Node.of("category", Map.of("id", entry.id()))).toList();
    }

    /**
     * Change the websites of this business profile
     *
     * @param websites the new websites, can be null
     * @return a CompletableFuture
     */
    public CompletableFuture<List<URI>> changeBusinessWebsites(List<URI> websites) {
        return socketHandler.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), createWebsites(websites)))
                .thenApplyAsync(ignored -> websites);
    }

    private List<Node> createWebsites(List<URI> websites) {
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
        return queryBusinessCatalog(jidOrThrowError().toSimpleJid(), productsLimit);
    }

    /**
     * Query the catalog of a business
     *
     * @param contact       the business
     * @param productsLimit the maximum number of products to query
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog(JidProvider contact, int productsLimit) {
        return socketHandler.sendQuery("get", "w:biz:catalog", Node.of("product_catalog", Map.of("jid", contact, "allow_shop_source", "true"), Node.of("limit", String.valueOf(productsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))))
                .thenApplyAsync(this::parseCatalog);
    }

    private List<BusinessCatalogEntry> parseCatalog(Node result) {
        return Objects.requireNonNull(result, "Cannot query business catalog, missing newsletters node")
                .findChild("product_catalog")
                .map(entry -> entry.listChildren("product"))
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
    public CompletableFuture<List<BusinessCatalogEntry>> queryBusinessCatalog(JidProvider contact) {
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
        return queryBusinessCollections(jidOrThrowError().toSimpleJid(), collectionsLimit);
    }

    /**
     * Query the collections of a business
     *
     * @param contact          the business
     * @param collectionsLimit the maximum number of collections to query
     * @return a CompletableFuture
     */
    public CompletableFuture<List<BusinessCollectionEntry>> queryBusinessCollections(JidProvider contact, int collectionsLimit) {
        return socketHandler.sendQuery("get", "w:biz:catalog", Map.of("smax_id", "35"), Node.of("collections", Map.of("biz_jid", contact), Node.of("collection_limit", String.valueOf(collectionsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("item_limit", String.valueOf(collectionsLimit)
                        .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))))
                .thenApplyAsync(this::parseCollections);
    }

    private List<BusinessCollectionEntry> parseCollections(Node result) {
        return Objects.requireNonNull(result, "Cannot query business collections, missing newsletters node")
                .findChild("collections")
                .stream()
                .map(entry -> entry.listChildren("collection"))
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
    public CompletableFuture<List<BusinessCollectionEntry>> queryBusinessCollections(JidProvider contact) {
        return queryBusinessCollections(contact, 50);
    }

    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, a reupload request will be sent to Whatsapp.
     * If the latter fails as well, an empty optional will be returned.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<byte[]> downloadMedia(ChatMessageInfo info) {
        if (!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        return downloadMedia(mediaMessage)
                .exceptionallyCompose(ignored1 -> requireMediaReupload(info)
                        .thenCompose(ignored -> downloadMedia(mediaMessage)));
    }

    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, an empty optional will be returned.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<byte[]> downloadMedia(NewsletterMessageInfo info) {
        if (!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        return downloadMedia(mediaMessage);
    }

    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, an empty optional will be returned.
     *
     * @param mediaMessage the non-null media
     * @return a CompletableFuture
     */
    public CompletableFuture<byte[]> downloadMedia(MediaMessage<?> mediaMessage) {
        var decodedMedia = mediaMessage.decodedMedia();
        if (decodedMedia.isPresent()) {
            return CompletableFuture.completedFuture(decodedMedia.get());
        }

        var proxy = store().proxy()
                .filter(ignored -> store().mediaProxySetting().allowsDownloads())
                .orElse(null);
        return Medias.downloadAsync(mediaMessage, proxy).thenApply(result -> {
            mediaMessage.setDecodedMedia(result);
            return result;
        });
    }

    /**
     * Asks Whatsapp for a media reupload for a specific media
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> requireMediaReupload(ChatMessageInfo info) {
        try {
            if (!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
                throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
            }

            var mediaKey = mediaMessage.mediaKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing media key"));
            var retryKey = Hkdf.extractAndExpand(mediaKey, "WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
            var receipt = ServerErrorReceiptSpec.encode(new ServerErrorReceipt(info.id()));
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(retryKey, "AES"),
                    new GCMParameterSpec(128, Bytes.random(12))
            );
            cipher.updateAAD(info.key().id().getBytes(StandardCharsets.UTF_8));
            var ciphertext = cipher.update(receipt);
            var rmrAttributes = Attributes.of()
                    .put("jid", info.chatJid())
                    .put("from_me", String.valueOf(info.fromMe()))
                    .put("participant", info.senderJid(), () -> !Objects.equals(info.chatJid(), info.senderJid()))
                    .toMap();
            var node = Node.of("receipt", Map.of("id", info.key().id(), "to", jidOrThrowError()
                    .toSimpleJid(), "type", "server-error"), Node.of("encrypt", Node.of("enc_p", ciphertext), Node.of("enc_iv", Bytes.random(12))), Node.of("rmr", rmrAttributes));
            return socketHandler.sendNode(node, result -> result.hasDescription("notification"))
                    .thenAcceptAsync(result -> parseMediaReupload(info, mediaMessage, retryKey, info.key().id().getBytes(StandardCharsets.UTF_8), result));
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt media reupload", exception);
        }
    }

    private void parseMediaReupload(ChatMessageInfo info, MediaMessage<?> mediaMessage, byte[] retryKey, byte[] retryIdData, Node node) {
        try {
            if (node.hasNode("error")) {
                throw new IllegalArgumentException("Erroneous response from media reupload: " + node.attributes().getInt("code"));
            }
            var encryptNode = node.findChild("encrypt")
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypt node in media reupload"));
            var mediaPayload = encryptNode.findChild("enc_p")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted payload node in media reupload"));
            var mediaIv = encryptNode.findChild("enc_iv")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted iv node in media reupload"));
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(retryKey, "AES"),
                    new GCMParameterSpec(128, mediaIv)
            );
            if(retryIdData != null) {
                cipher.updateAAD(retryIdData);
            }
            var mediaRetryNotificationData = cipher.doFinal(mediaPayload);
            var mediaRetryNotification = MediaRetryNotificationSpec.decode(mediaRetryNotificationData);
            var directPath = mediaRetryNotification.directPath()
                    .orElseThrow(() -> new RuntimeException("Media reupload failed"));
            mediaMessage.setMediaUrl(Medias.createMediaUrl(directPath));
            mediaMessage.setMediaDirectPath(directPath);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt media reupload", exception);
        }
    }

    /**
     * Sends a custom node to Whatsapp
     *
     * @param node the non-null node to send
     * @return the newsletters from Whatsapp
     */
    public CompletableFuture<Node> sendNode(Node node) {
        return socketHandler.sendNode(node);
    }

    /**
     * Creates a new community
     *
     * @param subject the non-null name of the new community
     * @param body    the nullable description of the new community
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<ChatMetadata>> createCommunity(String subject, String body) {
        var descriptionId = HexFormat.of().formatHex(Bytes.random(12));
        var children = new ArrayList<Node>();
        children.add(Node.of("description", Map.of("id", descriptionId), Node.of("body", Objects.requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8))));
        children.add(Node.of("parent", Map.of("default_membership_approval_mode", "request_required")));
        children.add(Node.of("allow_non_admin_sub_group_creation"));
        children.add(Node.of("create_general_chat"));
        var entry = Node.of("create", Map.of("subject", subject), children);
        return socketHandler.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", entry)
                .thenComposeAsync(this::parseGroupResult);
    }

    private CompletableFuture<Optional<ChatMetadata>> parseGroupResult(Node node) {
        return node.findChild("group")
                .map(response -> socketHandler.handleGroupMetadata(response)
                        .thenApply(Optional::ofNullable))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * Queries the metadata of a community
     *
     * @param community the target community
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMetadata> queryCommunityMetadata(JidProvider community) {
        if (!community.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        return socketHandler.queryGroupMetadata(community.toJid()).thenApply(result -> {
            if (!result.isCommunity()) {
                throw new IllegalArgumentException("Expected a community: use queryGroupMetadata for a group or queryChatMetadata");
            }
            return result;
        });
    }

    /**
     * Deactivates a community
     *
     * @param community the target community
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> deactivateCommunity(JidProvider community) {
        if (!community.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a community");
        }
        return socketHandler.sendQuery(community.toJid(), "set","w:g2", Node.of("delete_parent"))
                .thenRunAsync(() -> {});
    }

    /**
     * Changes the picture of a community
     *
     * @param community the target community
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeCommunityPicture(JidProvider community, URI image) {
        return changeGroupPicture(community, image);
    }

    /**
     * Changes the picture of a community
     *
     * @param community the target community
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeCommunityPicture(JidProvider community, byte[] image) {
        return changeGroupPicture(community, image);
    }

    /**
     * Changes the name of a community
     *
     * @param community   the target community
     * @param newName the new name for the community
     * @return a CompletableFuture
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public CompletableFuture<Void> changeCommunitySubject(JidProvider community, String newName) {
        return changeGroupSubject(community, newName);
    }

    /**
     * Changes the description of a community
     *
     * @param community       the target community
     * @param description the new name for the community, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeCommunityDescription(JidProvider community, String description) {
        return changeGroupDescription(community, description);
    }

    /**
     * Changes a community setting
     *
     * @param community the non-null community affected by this change
     * @param setting   the non-null setting
     * @param policy    the non-null policy
     * @return a future
     */
    public CompletableFuture<Void> changeCommunitySetting(JidProvider community, CommunitySetting setting, ChatSettingPolicy policy) {
        if (!community.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("This method only accepts communities");
        }
        return switch (setting) {
            case MODIFY_GROUPS -> {
                var request = CommunityRequests.changeModifyGroupsSetting(community.toJid(), policy == ChatSettingPolicy.ANYONE);
                var body = Node.of("query", Map.of("query_id", "24745914578387890"), request.getBytes());
                yield socketHandler.sendQuery("get", "w:mex", body).thenAcceptAsync(result -> {
                    var resultJsonSource = result.findChild("result")
                            .flatMap(Node::contentAsString)
                            .orElse(null);
                    if (resultJsonSource == null) {
                        throw new IllegalArgumentException("Cannot change community setting: " + result);
                    }

                    var resultJson = JSON.parseObject(resultJsonSource);
                    if (resultJson.containsKey("errors")) {
                        throw new IllegalArgumentException("Cannot change community setting: " + resultJsonSource);
                    }
                });
            }
            case ADD_PARTICIPANTS -> {
                var body = Node.of("member_add_mode", policy == ChatSettingPolicy.ANYONE ? "all_member_add".getBytes() : "admin_add".getBytes());
                yield socketHandler.sendQuery(community.toJid(), "set", "w:g2", body).thenAcceptAsync(result -> {
                    if (result.hasNode("error")) {
                        throw new IllegalArgumentException("Cannot change community setting: " + result);
                    }
                });
            }
        };
    }

    /**
     * Links any number of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups    the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public CompletableFuture<Map<Jid, Boolean>> addCommunityGroups(JidProvider community, JidProvider... groups) {
        var body = Arrays.stream(groups)
                .map(entry -> Node.of("group", Map.of("jid", entry.toJid())))
                .toArray(Node[]::new);
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.of("links", Node.of("link", Map.of("link_type", "sub_group"), body)))
                .thenApplyAsync(result -> parseLinksResponse(result, groups));
    }

    private Map<Jid, Boolean> parseLinksResponse(Node result, JidProvider[] groups) {
        var success = result.findChild("links")
                .stream()
                .map(entry -> entry.listChildren("link"))
                .flatMap(Collection::stream)
                .filter(entry -> entry.attributes().hasValue("link_type", "sub_group"))
                .map(entry -> entry.findChild("group"))
                .flatMap(Optional::stream)
                .map(entry -> entry.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        return Arrays.stream(groups)
                .map(JidProvider::toJid)
                .collect(Collectors.toUnmodifiableMap(Function.identity(), success::contains));
    }

    /**
     * Unlinks a group from a community
     *
     * @param community the non-null parent community
     * @param group     the non-null group to unlink
     * @return a CompletableFuture that indicates whether the request was successful
     */
    public CompletableFuture<Boolean> removeCommunityGroup(JidProvider community, JidProvider group) {
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.of("unlink", Map.of("unlink_type", "sub_group"), Node.of("group", Map.of("jid", group.toJid()))))
                .thenApplyAsync(result -> parseUnlinkResponse(result, group));
    }

    private boolean parseUnlinkResponse(Node result, JidProvider group) {
        return result.findChild("unlink")
                .filter(entry -> entry.attributes().hasValue("unlink_type", "sub_group"))
                .flatMap(entry -> entry.findChild("group"))
                .map(entry -> entry.attributes().hasValue("jid", group.toJid().toString()))
                .isPresent();
    }

    /**
     * Promotes any number of contacts to admin in a community
     *
     * @param community    the target community
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> promoteCommunityParticipants(JidProvider community, JidProvider... contacts) {
        return queryCommunityMetadata(community)
                .thenComposeAsync(metadata -> {
                    if (!metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a community: use promoteGroupParticipants for groups");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(participantsSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(community, true, GroupAction.PROMOTE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot promote participant in community", error);
                });
    }

    /**
     * Demotes any number of contacts to admin in a community
     *
     * @param community    the target community
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> demoteCommunityParticipants(JidProvider community, JidProvider... contacts) {
        return queryCommunityMetadata(community)
                .thenComposeAsync(metadata -> {
                    if (!metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a community: use demoteGroupParticipants for groups");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(participantsSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(community, true, GroupAction.DEMOTE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot demote participant in community", error);
                });
    }

    /**
     * Adds any number of contacts to a community
     *
     * @param community    the target community
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> addCommunityParticipants(JidProvider community, JidProvider... contacts) {
        return queryCommunityMetadata(community)
                .thenComposeAsync(metadata -> {
                    if (!metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a community: use addGroupParticipants for groups");
                    }
                    var participantsSet = metadata.participants()
                            .stream()
                            .map(ChatParticipant::jid)
                            .collect(Collectors.toUnmodifiableSet());
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .filter(entry -> !participantsSet.contains(entry))
                            .collect(Collectors.toUnmodifiableSet());
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    var announcementsGroup = metadata.communityGroups()
                            .getLast()
                            .jid();
                    return executeActionOnParticipants(announcementsGroup, true, GroupAction.ADD, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot add participant to community", error);
                });
    }

    /**
     * Removes any number of contacts from community
     *
     * @param community    the target community
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> removeCommunityParticipants(JidProvider community, JidProvider... contacts) {
        return queryCommunityMetadata(community)
                .thenComposeAsync(metadata -> {
                    if (!metadata.isCommunity()) {
                        throw new IllegalArgumentException("Expected a community: use removeGroupParticipants for groups");
                    }
                    var targets = Arrays.stream(contacts)
                            .map(JidProvider::toJid)
                            .collect(Collectors.toUnmodifiableSet()); // No contains check because we would need to enumerate all the children, just let whatsapp do it internally
                    if(targets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return executeActionOnParticipants(community, true, GroupAction.REMOVE, targets);
                })
                .exceptionally(error -> {
                    throw new RuntimeException("Cannot remove participant from community", error);
                });
    }

    /**
     * Leaves a community
     *
     * @param community the target community
     * @throws IllegalArgumentException if the provided chat is not a community
     * @return a future
     */
    public CompletableFuture<Void> leaveCommunity(JidProvider community) {
        if (!community.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a community");
        }
        return queryCommunityMetadata(community).thenComposeAsync(metadata -> {
            var communityJid = metadata.parentCommunityJid().orElse(metadata.jid());
            var body = Node.of("leave", Node.of("linked_groups", Map.of("parent_group_jid", communityJid)));
            return socketHandler.sendQuery("set", "w:g2", body).thenAcceptAsync(ignored -> {
                handleLeaveGroup(community);
                metadata.communityGroups().forEach(linkedGroup -> handleLeaveGroup(linkedGroup.jid()));
            });
        });
    }

    /**
     * Opens a wa.me chat link
     *
     * @param link the non-null link to open
     * @return a future
     */
    public CompletableFuture<Optional<Jid>> openChatLink(URI link) {
        var host = link.getHost();
        if (!"wa.me".equals(host)) {
            throw new IllegalArgumentException("Expected wa.me link");
        }
        var path = link.getPath();
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Expected path component");
        }
        try {
            var result = Jid.of(path.substring(1));
            return prepareChat(Clock.nowSeconds(), Set.of(result)).thenApply(results -> {
                if(results.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(result);
            });
        }catch (Throwable throwable) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Gets the verified name certificate
     *
     * @return a future
     */
    public CompletableFuture<Optional<BusinessVerifiedNameCertificate>> queryBusinessCertificate(JidProvider provider) {
        return socketHandler.sendQuery("get", "w:biz", Node.of("verified_name", Map.of("jid", provider.toJid())))
                .thenApplyAsync(this::parseCertificate);
    }

    private Optional<BusinessVerifiedNameCertificate> parseCertificate(Node result) {
        return result.findChild("verified_name")
                .flatMap(Node::contentAsBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode);
    }

    /**
     * Enables two-factor authentication
     * Mobile API only
     *
     * @param code the six digits non-null numeric code
     * @return a future
     */
    public CompletableFuture<?> enable2fa(String code) {
        return set2fa(code, null);
    }

    /**
     * Enables two-factor authentication
     * Mobile API only
     *
     * @param code  the six digits non-null numeric code
     * @param email the nullable recovery email
     * @return a future
     */
    public CompletableFuture<Boolean> enable2fa(String code, String email) {
        return set2fa(code, email);
    }

    /**
     * Disables two-factor authentication
     * Mobile API only
     *
     * @return a future
     */
    public CompletableFuture<Boolean> disable2fa() {
        return set2fa(null, null);
    }

    private CompletableFuture<Boolean> set2fa(String code, String email) {
        if (store().clientType() != ClientType.MOBILE) {
            throw new IllegalArgumentException("2FA is only available for the mobile api");
        }
        if (code != null && (!code.matches("^[0-9]*$") || code.length() != 6)) {
            throw new IllegalArgumentException("Invalid 2fa code: expected a numeric six digits string");
        }

        if (email != null && !EMAIL_PATTERN.matcher(email)
                .matches()) {
            throw new IllegalArgumentException("Invalid email: %s".formatted(email));
        }

        var body = new ArrayList<Node>();
        body.add(Node.of("code", Objects.requireNonNullElse(code, "").getBytes(StandardCharsets.UTF_8)));
        if (code != null && email != null) {
            body.add(Node.of("email", email.getBytes(StandardCharsets.UTF_8)));
        }

        return socketHandler.sendQuery("set", "urn:xmpp:whatsapp:account", Node.of("2fa", body))
                .thenApplyAsync(result -> !result.hasNode("error"));
    }

    /**
     * Starts a call with a contact
     * Mobile API only
     *
     * @param contact the non-null contact
     * @param video whether it's a video call or an audio call
     * @return a future
     */
    public CompletableFuture<Call> startCall(JidProvider contact, boolean video) {
        if (store().clientType() != ClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        return addContacts(contact)
                .thenComposeAsync(ignored -> socketHandler.querySessions(List.of(contact.toJid())))
                .thenComposeAsync(ignored -> sendCallMessage(contact, video));
    }

    private CompletableFuture<Call> sendCallMessage(JidProvider provider, boolean video) {
        var callId = ChatMessageKey.randomId(store().clientType());
        var description = video ? "video" : "audio";
        var audioStream = Node.of(description, Map.of("rate", 8000, "enc", "opus"));
        var audioStreamTwo = Node.of(description, Map.of("rate", 16000, "enc", "opus"));
        var net = Node.of("net", Map.of("medium", 3));
        var encopt = Node.of("encopt", Map.of("keygen", 2));
        var enc = createCallNode(provider);
        var capability = Node.of("capability", Map.of("ver", 1), HexFormat.of().parseHex("0104ff09c4fa"));
        var callCreator = "%s:0@s.whatsapp.net".formatted(jidOrThrowError().user());
        var offer = Node.of("offer",
                Map.of("call-creator", callCreator, "call-id", callId),
                audioStream, audioStreamTwo, net, capability, encopt, enc);
        return socketHandler.sendNode(Node.of("call", Map.of("to", provider.toJid()), offer))
                .thenApply(result -> onCallSent(provider, callId, result));
    }

    private Call onCallSent(JidProvider provider, String callId, Node result) {
        var call = new CallBuilder()
                .chatJid(provider.toJid())
                .callerJid(jidOrThrowError())
                .id(callId)
                .timestampSeconds(Clock.nowSeconds())
                .video(false)
                .status(CallStatus.RINGING)
                .offline(false)
                .build();
        store().addCall(call);
        socketHandler.onCall(call);
        return call;
    }

    private Node createCallNode(JidProvider provider) {
        var call = new CallMessageBuilder()
                .key(SignalKeyPair.random().publicKey())
                .build();
        var message = MessageContainer.of(call);
        var cipher = new SessionCipher(provider.toJid().toSignalAddress(), keys());
        var encodedMessage = Bytes.messageToBytes(message);
        var cipheredMessage = cipher.encrypt(encodedMessage);
        return Node.of("enc", Map.of("v", 2, "type", cipheredMessage.type()), cipheredMessage.message());
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param callId the non-null id of the call to reject
     * @return a future
     */
    public CompletableFuture<Boolean> stopCall(String callId) {
        if (store().clientType() != ClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        return store().findCallById(callId)
                .map(this::stopCall)
                .orElseGet(() -> CompletableFuture.completedFuture(false));
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param call the non-null call to reject
     * @return a future
     */
    public CompletableFuture<Boolean> stopCall(Call call) {
        if (store().clientType() != ClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        if (Objects.equals(call.callerJid().user(), jidOrThrowError().user())) {
            var rejectNode = Node.of("terminate", Map.of("reason", "timeout", "call-id", call.id(), "call-creator", call.callerJid()));
            var body = Node.of("call", Map.of("to", call.chatJid()), rejectNode);
            return socketHandler.sendNode(body)
                    .thenApplyAsync(result -> !result.hasNode("error"));
        }

        var rejectNode = Node.of("reject", Map.of("call-id", call.id(), "call-creator", call.callerJid(), "count", 0));
        var body = Node.of("call", Map.of("from", jidOrThrowError(), "to", call.callerJid()), rejectNode);
        return socketHandler.sendNode(body)
                .thenApplyAsync(result -> !result.hasNode("error"));
    }


    /**
     * Queries a list of fifty recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @return a list of recommended newsletters, if the feature is available
     */
    public CompletableFuture<List<Newsletter>> queryRecommendedNewsletters(String countryCode) {
        return queryRecommendedNewsletters(countryCode, 50);
    }


    /**
     * Queries a list of recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @param limit       how many entries should be returned
     * @return a list of recommended newsletters, if the feature is available
     */
    public CompletableFuture<List<Newsletter>> queryRecommendedNewsletters(String countryCode, int limit) {
        var request = NewsletterRequests.recommendedNewsletters("RECOMMENDED", List.of(countryCode), limit);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6190824427689257"), request))
                .thenApplyAsync(this::parseRecommendedNewsletters);
    }

    @SuppressWarnings("OptionalIsPresent")
    private List<Newsletter> parseRecommendedNewsletters(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return List.of();
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return List.of();
        }

        return RecommendedNewslettersResponse.of(content.get())
                .map(RecommendedNewslettersResponse::newsletters)
                .orElse(List.of());
    }

    /**
     * Queries any number of messages from a newsletter
     *
     * @param newsletterJid the non-null jid of the newsletter
     * @param count         how many messages should be queried
     * @return a future
     */
    public CompletableFuture<Void> queryNewsletterMessages(JidProvider newsletterJid, int count) {
        return socketHandler.queryNewsletterMessages(newsletterJid, count);
    }

    /**
     * Subscribes to a public newsletter's event stream of reactions
     *
     * @param channel the non-null channel
     * @return the time, in minutes, during which updates will be sent
     */
    public CompletableFuture<OptionalLong> subscribeToNewsletterReactions(JidProvider channel) {
        return socketHandler.subscribeToNewsletterReactions(channel);
    }

    /**
     * Creates a newsletter
     *
     * @param name the non-null name of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name) {
        return createNewsletter(name, null, null);
    }

    /**
     * Creates newsletter channel
     *
     * @param name        the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name, String description) {
        return createNewsletter(name, description, null);
    }

    /**
     * Creates a newsletter
     *
     * @param name        the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     * @param picture     the nullable profile picture of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name, String description, byte[] picture) {
        var request = NewsletterRequests.createNewsletter(name, description, picture != null ? Base64.getEncoder().encodeToString(picture) : null);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6996806640408138"), request))
                .thenApplyAsync(this::parseNewsletterCreation)
                .thenComposeAsync(this::onNewsletterCreation);
    }

    @SuppressWarnings("OptionalIsPresent")
    private Optional<Newsletter> parseNewsletterCreation(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return Optional.empty();
        }

        var content = result.get()
                .contentAsBytes();
        if(content.isEmpty()) {
            return Optional.empty();
        }

        return NewsletterResponse.ofJson(content.get())
                .map(NewsletterResponse::newsletter);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CompletableFuture<Optional<Newsletter>> onNewsletterCreation(Optional<Newsletter> result) {
        if (result.isEmpty()) {
            return CompletableFuture.completedFuture(result);
        }

        return subscribeToNewsletterReactions(result.get().jid())
                .thenApply(ignored -> result);
    }

    /**
     * Changes the description of a newsletter
     *
     * @param newsletter  the non-null target newsletter
     * @param description the nullable new description
     * @return a future
     */
    public CompletableFuture<Void> changeNewsletterDescription(JidProvider newsletter, String description) {
        var request = NewsletterRequests.updateNewsletter(newsletter.toJid(), Objects.requireNonNullElse(description, ""));
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7150902998257522"), request))
                .thenRun(() -> {});
    }

    /**
     * Joins a newsletter
     *
     * @param newsletter a non-null newsletter
     * @return a future
     */
    public CompletableFuture<Void> joinNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.joinNewsletter(newsletter.toJid());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "9926858900719341"), request))
                .thenRun(() -> {});
    }

    /**
     * Leaves a newsletter
     *
     * @param newsletter a non-null newsletter
     * @return a future
     */
    public CompletableFuture<Void> leaveNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.leaveNewsletter(newsletter.toJid());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6392786840836363"), request))
                .thenRun(() -> {});
    }

    /**
     * Queries the number of people subscribed to a newsletter
     *
     * @param newsletter the id of the newsletter
     * @return a CompletableFuture
     */
    public CompletableFuture<OptionalLong> queryNewsletterSubscribers(JidProvider newsletter) {
        var newsletterRole = store()
                .findNewsletterByJid(newsletter)
                .flatMap(Newsletter::viewerMetadata)
                .map(NewsletterViewerMetadata::role)
                .orElse(NewsletterViewerRole.GUEST);
        var request = NewsletterRequests.newsletterSubscribers(newsletter.toJid(), "JID", newsletterRole);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7272540469429201"), request))
                .thenApply(this::parseNewsletterSubscribers);
    }

    private OptionalLong parseNewsletterSubscribers(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return OptionalLong.empty();
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return OptionalLong.empty();
        }

        var response = NewsletterSubscribersResponse.ofJson(content.get());
        if(response.isEmpty()) {
            return OptionalLong.empty();
        }

        return response.get()
                .subscribersCount();
    }

    /**
     * Sends an invitation to each jid provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param admins        the new admins
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> inviteNewsletterAdmins(JidProvider newsletterJid, JidProvider... admins) {
        return inviteNewsletterAdmins(newsletterJid, null, admins);
    }

    /**
     * Sends an invitation to each jid provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param inviteCaption the nullable caption of the invitation
     * @param admins        the new admins
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> inviteNewsletterAdmins(JidProvider newsletterJid, String inviteCaption, JidProvider... admins) {
        var messageFutures = Arrays.stream(admins)
                .map(admin -> createNewsletterAdminInvite(newsletterJid, inviteCaption, admin))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(messageFutures);
    }

    private CompletableFuture<ChatMessageInfo> createNewsletterAdminInvite(JidProvider newsletterJid, String inviteCaption, JidProvider admin) {
        var adminPhoneNumber = admin.toJid()
                .toPhoneNumber()
                .orElseThrow(() -> new IllegalArgumentException("%s cannot be parsed as a phone number".formatted(admin)));
        return getContactData(adminPhoneNumber).thenCompose(results -> {
            var recipient = results.getFirst()
                    .findChild("lid")
                    .flatMap(result -> result.attributes().getOptionalJid("val"))
                    .map(jid -> jid.withServer(JidServer.lid()).toSimpleJid())
                    .orElse(admin.toJid());
            var request = NewsletterRequests.createAdminInviteNewsletter(newsletterJid.toJid(), recipient);
            return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6826078034173770"), request))
                    .thenApplyAsync(this::parseNewsletterAdminInviteExpiration)
                    .thenComposeAsync(expirationTimestamp -> sendNewsletterInviteMessage(newsletterJid, inviteCaption, expirationTimestamp, admin));
        });
    }

    @SuppressWarnings("OptionalIsPresent")
    private long parseNewsletterAdminInviteExpiration(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return 0;
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return 0;
        }

        var response = CreateAdminInviteNewsletterResponse.ofJson(content.get());
        if(response.isEmpty()) {
            return 0;
        }

        return response.get()
                .expirationTime();
    }

    private CompletableFuture<ChatMessageInfo> sendNewsletterInviteMessage(JidProvider newsletterJid, String inviteCaption, long expirationTimestamp, JidProvider admin) {
        var newsletterName = store().findNewsletterByJid(newsletterJid.toJid())
                .flatMap(Newsletter::metadata)
                .flatMap(NewsletterMetadata::name)
                .map(NewsletterName::text)
                .orElse(null);
        var message = new NewsletterAdminInviteMessageBuilder()
                .newsletterJid(newsletterJid.toJid())
                .newsletterName(newsletterName)
                .inviteExpirationTimestampSeconds(expirationTimestamp)
                .caption(Objects.requireNonNullElse(inviteCaption, "Accept this invitation to be an admin for my WhatsApp channel"))
                .build();
        return sendChatMessage(admin, MessageContainer.of(message));
    }

    /**
     * Revokes an invitation to become an admin in a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param admin         the non-null user that received the invite previously
     * @return a CompletableFuture
     */
    public CompletableFuture<Boolean> revokeNewsletterAdminInvite(JidProvider newsletterJid, JidProvider admin) {
        var adminPhoneNumber = admin.toJid()
                .toPhoneNumber()
                .orElseThrow(() -> new IllegalArgumentException("%s cannot be parsed as a phone number".formatted(admin)));
        return getContactData(adminPhoneNumber).thenCompose(results -> {
            var recipient = results.getFirst()
                    .findChild("lid")
                    .flatMap(result -> result.attributes().getOptionalJid("val"))
                    .map(jid -> jid.withServer(JidServer.lid()).toSimpleJid())
                    .orElse(admin.toJid());
            var request = NewsletterRequests.revokeAdminInviteNewsletter(newsletterJid.toJid(), recipient);
            return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6111171595650958"), request))
                    .thenApplyAsync(this::hasRevokedNewsletterAdminInvite);
        });
    }

    @SuppressWarnings("OptionalIsPresent")
    private boolean hasRevokedNewsletterAdminInvite(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return false;
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return false;
        }

        return RevokeAdminInviteNewsletterResponse.ofJson(content.get())
                .map(RevokeAdminInviteNewsletterResponse::jid)
                .isPresent();
    }

    /**
     * Accepts an invitation to become an admin in a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @return a CompletableFuture
     */
    public CompletableFuture<Boolean> acceptNewsletterAdminInvite(JidProvider newsletterJid) {
        var request = NewsletterRequests.acceptAdminInviteNewsletter(newsletterJid.toJid());
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7292354640794756"), request))
                .thenCompose(this::parseNewsletterAdminInviteResponse);
    }

    @SuppressWarnings("OptionalIsPresent")
    private CompletableFuture<Boolean> parseNewsletterAdminInviteResponse(Node node) {
        var result = node.findChild("result");
        if(result.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var jid = AcceptAdminInviteNewsletterResponse.ofJson(content.get())
                .map(AcceptAdminInviteNewsletterResponse::jid);
        if(jid.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return queryNewsletter(jid.get(), NewsletterViewerRole.ADMIN).thenApplyAsync(newsletter -> {
            if (newsletter.isEmpty()) {
                return false;
            }else {
                store().addNewsletter(newsletter.get());
                return true;
            }
        });
    }

    /**
     * Sends a message retry for the specified message
     * Use this if a message is marked as unavailable
     * View once messages are only visible on the companion device
     *
     * @param messageInfo the message to retry
     * @return a future
     */
    public CompletableFuture<Void> sendMessageRetry(MessageInfo<?> messageInfo) {
        var timestamp = messageInfo.timestampSeconds()
                .orElse(0);
        var chatJid = messageInfo.parentJid();
        var senderJid = messageInfo.senderJid();
        return socketHandler.sendRetryReceipt(timestamp, chatJid, senderJid, messageInfo.id(), 1);
    }

    /**
     * Queries a newsletter
     *
     * @param newsletterJid the non-null jid of the newsletter
     * @param role          the non-null role of the user executing the query
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        return socketHandler.queryNewsletter(newsletterJid, role);
    }

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Whatsapp addListener(Listener listener) {
        store().addListener(listener);
        return this;
    }

    /**
     * Unregisters a listener
     *
     * @param listener the listener to unregister
     * @return the same instance
     */
    public Whatsapp removeListener(Listener listener) {
        store().removeListener(listener);
        return this;
    }

    // Generated code from it.auties.whatsapp.routine.GenerateListenersLambda

    public Whatsapp addNodeSentListener(Listener.Consumer.Binary<Whatsapp, Node> consumer) {
        addListener(new Listener() {
            @Override
            public void onNodeSent(Whatsapp whatsapp, Node outgoing) {
                consumer.accept(whatsapp, outgoing);
            }
        });
        return this;
    }

    public Whatsapp addNodeSentListener(Listener.Consumer.Unary<Node> consumer) {
        addListener(new Listener() {
            @Override
            public void onNodeSent(Node outgoing) {
                consumer.accept(outgoing);
            }
        });
        return this;
    }

    public Whatsapp addNodeReceivedListener(Listener.Consumer.Unary<Node> consumer) {
        addListener(new Listener() {
            @Override
            public void onNodeReceived(Node incoming) {
                consumer.accept(incoming);
            }
        });
        return this;
    }

    public Whatsapp addNodeReceivedListener(Listener.Consumer.Binary<Whatsapp, Node> consumer) {
        addListener(new Listener() {
            @Override
            public void onNodeReceived(Whatsapp whatsapp, Node incoming) {
                consumer.accept(whatsapp, incoming);
            }
        });
        return this;
    }

    public Whatsapp addLoggedInListener(Listener.Consumer.Empty consumer) {
        addListener(new Listener() {
            @Override
            public void onLoggedIn() {
                consumer.accept();
            }
        });
        return this;
    }

    public Whatsapp addLoggedInListener(Listener.Consumer.Unary<Whatsapp> consumer) {
        addListener(new Listener() {
            @Override
            public void onLoggedIn(Whatsapp whatsapp) {
                consumer.accept(whatsapp);
            }
        });
        return this;
    }

    public Whatsapp addMetadataListener(Listener.Consumer.Unary<Map<String, String>> consumer) {
        addListener(new Listener() {
            @Override
            public void onMetadata(Map<String, String> metadata) {
                consumer.accept(metadata);
            }
        });
        return this;
    }

    public Whatsapp addMetadataListener(Listener.Consumer.Binary<Whatsapp, Map<String, String>> consumer) {
        addListener(new Listener() {
            @Override
            public void onMetadata(Whatsapp whatsapp, Map<String, String> metadata) {
                consumer.accept(whatsapp, metadata);
            }
        });
        return this;
    }

    public Whatsapp addDisconnectedListener(Listener.Consumer.Unary<DisconnectReason> consumer) {
        addListener(new Listener() {
            @Override
            public void onDisconnected(DisconnectReason reason) {
                consumer.accept(reason);
            }
        });
        return this;
    }

    public Whatsapp addDisconnectedListener(Listener.Consumer.Binary<Whatsapp, DisconnectReason> consumer) {
        addListener(new Listener() {
            @Override
            public void onDisconnected(Whatsapp whatsapp, DisconnectReason reason) {
                consumer.accept(whatsapp, reason);
            }
        });
        return this;
    }

    public Whatsapp addActionListener(Listener.Consumer.Binary<Action, MessageIndexInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onAction(Action action, MessageIndexInfo messageIndexInfo) {
                consumer.accept(action, messageIndexInfo);
            }
        });
        return this;
    }

    public Whatsapp addActionListener(Listener.Consumer.Ternary<Whatsapp, Action, MessageIndexInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onAction(Whatsapp whatsapp, Action action, MessageIndexInfo messageIndexInfo) {
                consumer.accept(whatsapp, action, messageIndexInfo);
            }
        });
        return this;
    }

    public Whatsapp addSettingListener(Listener.Consumer.Unary<Setting> consumer) {
        addListener(new Listener() {
            @Override
            public void onSetting(Setting setting) {
                consumer.accept(setting);
            }
        });
        return this;
    }

    public Whatsapp addSettingListener(Listener.Consumer.Binary<Whatsapp, Setting> consumer) {
        addListener(new Listener() {
            @Override
            public void onSetting(Whatsapp whatsapp, Setting setting) {
                consumer.accept(whatsapp, setting);
            }
        });
        return this;
    }

    public Whatsapp addFeaturesListener(Listener.Consumer.Unary<List<String>> consumer) {
        addListener(new Listener() {
            @Override
            public void onFeatures(List<String> features) {
                Listener.super.onFeatures(features);
            }
        });
        return this;
    }

    public Whatsapp addFeaturesListener(Listener.Consumer.Binary<Whatsapp, List<String>> consumer) {
        addListener(new Listener() {
            @Override
            public void onFeatures(Whatsapp whatsapp, List<String> features) {
                consumer.accept(whatsapp, features);
            }
        });
        return this;
    }

    public Whatsapp addContactsListener(Listener.Consumer.Unary<Collection<Contact>> consumer) {
        addListener(new Listener() {
            @Override
            public void onContacts(Collection<Contact> contacts) {
                consumer.accept(contacts);
            }
        });
        return this;
    }

    public Whatsapp addContactsListener(Listener.Consumer.Binary<Whatsapp, Collection<Contact>> consumer) {
        addListener(new Listener() {
            @Override
            public void onContacts(Whatsapp whatsapp, Collection<Contact> contacts) {
                consumer.accept(whatsapp, contacts);
            }
        });
        return this;
    }

    public Whatsapp addContactPresenceListener(Listener.Consumer.Binary<Chat, JidProvider> consumer) {
        addListener(new Listener() {
            @Override
            public void onContactPresence(Chat chat, JidProvider jid) {
                consumer.accept(chat, jid);
            }
        });
        return this;
    }

    public Whatsapp addContactPresenceListener(Listener.Consumer.Ternary<Whatsapp, Chat, JidProvider> consumer) {
        addListener(new Listener() {
            @Override
            public void onContactPresence(Whatsapp whatsapp, Chat chat, JidProvider jid) {
                consumer.accept(whatsapp, chat, jid);
            }
        });
        return this;
    }

    public Whatsapp addChatsListener(Listener.Consumer.Unary<Collection<Chat>> consumer) {
        addListener(new Listener() {
            @Override
            public void onChats(Collection<Chat> chats) {
                consumer.accept(chats);
            }
        });
        return this;
    }

    public Whatsapp addChatsListener(Listener.Consumer.Binary<Whatsapp, Collection<Chat>> consumer) {
        addListener(new Listener() {
            @Override
            public void onChats(Whatsapp whatsapp, Collection<Chat> chats) {
                consumer.accept(whatsapp, chats);
            }
        });
        return this;
    }

    public Whatsapp addNewslettersListener(Listener.Consumer.Unary<Collection<Newsletter>> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewsletters(Collection<Newsletter> newsletters) {
                consumer.accept(newsletters);
            }
        });
        return this;
    }

    public Whatsapp addNewslettersListener(Listener.Consumer.Binary<Whatsapp, Collection<Newsletter>> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewsletters(Whatsapp whatsapp, Collection<Newsletter> newsletters) {
                consumer.accept(whatsapp, newsletters);
            }
        });
        return this;
    }

    public Whatsapp addChatMessagesSyncListener(Listener.Consumer.Binary<Chat, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onChatMessagesSync(Chat chat, boolean last) {
                consumer.accept(chat, last);
            }
        });
        return this;
    }

    public Whatsapp addChatMessagesSyncListener(Listener.Consumer.Ternary<Whatsapp, Chat, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onChatMessagesSync(Whatsapp whatsapp, Chat chat, boolean last) {
                consumer.accept(whatsapp, chat, last);
            }
        });
        return this;
    }

    public Whatsapp addHistorySyncProgressListener(Listener.Consumer.Ternary<Whatsapp, Integer, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onHistorySyncProgress(Whatsapp whatsapp, int percentage, boolean recent) {
                consumer.accept(whatsapp, percentage, recent);
            }
        });
        return this;
    }

    public Whatsapp addHistorySyncProgressListener(Listener.Consumer.Binary<Integer, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onHistorySyncProgress(int percentage, boolean recent) {
                consumer.accept(percentage, recent);
            }
        });
        return this;
    }

    public Whatsapp addNewMessageListener(Listener.Consumer.Unary<MessageInfo<?>> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(MessageInfo<?> info) {
                consumer.accept(info);
            }
        });
        return this;
    }

    public Whatsapp addNewChatMessageListener(Listener.Consumer.Unary<ChatMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(MessageInfo<?> info) {
                if(info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(chatMessageInfo);
                }
            }
        });
        return this;
    }

    public Whatsapp addNewNewsletterMessageListener(Listener.Consumer.Unary<NewsletterMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(MessageInfo<?> info) {
                if(info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(newsletterMessageInfo);
                }
            }
        });
        return this;
    }

    public Whatsapp addNewMessageListener(Listener.Consumer.Binary<Whatsapp, MessageInfo<?>> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo<?> info) {
                consumer.accept(whatsapp, info);
            }
        });
        return this;
    }

    public Whatsapp addNewNewsletterMessageListener(Listener.Consumer.Binary<Whatsapp, NewsletterMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo<?> info) {
                if(info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(whatsapp, newsletterMessageInfo);
                }
            }
        });
        return this;
    }

    public Whatsapp addNewChatMessageListener(Listener.Consumer.Binary<Whatsapp, ChatMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo<?> info) {
                if(info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(whatsapp, chatMessageInfo);
                }
            }
        });
        return this;
    }

    public Whatsapp addMessageDeletedListener(Listener.Consumer.Binary<MessageInfo<?>, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageDeleted(MessageInfo<?> info, boolean everyone) {
                consumer.accept(info, everyone);
            }
        });
        return this;
    }

    public Whatsapp addMessageDeletedListener(Listener.Consumer.Ternary<Whatsapp, MessageInfo<?>, Boolean> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageDeleted(Whatsapp whatsapp, MessageInfo<?> info, boolean everyone) {
                consumer.accept(whatsapp, info, everyone);
            }
        });
        return this;
    }

    public Whatsapp addMessageStatusListener(Listener.Consumer.Unary<MessageInfo<?>> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageStatus(MessageInfo<?> info) {
                consumer.accept(info);
            }
        });
        return this;
    }

    public Whatsapp addMessageStatusListener(Listener.Consumer.Binary<Whatsapp, MessageInfo<?>> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageStatus(Whatsapp whatsapp, MessageInfo<?> info) {
                consumer.accept(whatsapp, info);
            }
        });
        return this;
    }

    public Whatsapp addStatusListener(Listener.Consumer.Unary<Collection<ChatMessageInfo>> consumer) {
        addListener(new Listener() {
            @Override
            public void onStatus(Collection<ChatMessageInfo> status) {
                consumer.accept(status);
            }
        });
        return this;
    }

    public Whatsapp addStatusListener(Listener.Consumer.Binary<Whatsapp, Collection<ChatMessageInfo>> consumer) {
        addListener(new Listener() {
            @Override
            public void onStatus(Whatsapp whatsapp, Collection<ChatMessageInfo> status) {
                consumer.accept(whatsapp, status);
            }
        });
        return this;
    }

    public Whatsapp addNewStatusListener(Listener.Consumer.Unary<ChatMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewStatus(ChatMessageInfo status) {
                consumer.accept(status);
            }
        });
        return this;
    }

    public Whatsapp addNewStatusListener(Listener.Consumer.Binary<Whatsapp, ChatMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewStatus(Whatsapp whatsapp, ChatMessageInfo status) {
                consumer.accept(whatsapp, status);
            }
        });
        return this;
    }

    public Whatsapp addMessageReplyListener(Listener.Consumer.Ternary<Whatsapp, ChatMessageInfo, QuotedMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageReply(Whatsapp whatsapp, ChatMessageInfo response, QuotedMessageInfo quoted) {
                consumer.accept(whatsapp, response, quoted);
            }
        });
        return this;
    }

    public Whatsapp addMessageReplyListener(Listener.Consumer.Binary<ChatMessageInfo, QuotedMessageInfo> consumer) {
        addListener(new Listener() {
            @Override
            public void onMessageReply(ChatMessageInfo response, QuotedMessageInfo quoted) {
                consumer.accept(response, quoted);
            }
        });
        return this;
    }

    public Whatsapp addProfilePictureChangedListener(Listener.Consumer.Unary<Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onProfilePictureChanged(Contact contact) {
                consumer.accept(contact);
            }
        });
        return this;
    }

    public Whatsapp addProfilePictureChangedListener(Listener.Consumer.Binary<Whatsapp, Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onProfilePictureChanged(Whatsapp whatsapp, Contact contact) {
                consumer.accept(whatsapp, contact);
            }
        });
        return this;
    }

    public Whatsapp addGroupPictureChangedListener(Listener.Consumer.Binary<Whatsapp, Chat> consumer) {
        addListener(new Listener() {
            @Override
            public void onGroupPictureChanged(Whatsapp whatsapp, Chat group) {
                consumer.accept(whatsapp, group);
            }
        });
        return this;
    }

    public Whatsapp addGroupPictureChangedListener(Listener.Consumer.Unary<Chat> consumer) {
        addListener(new Listener() {
            @Override
            public void onGroupPictureChanged(Chat group) {
                consumer.accept(group);
            }
        });
        return this;
    }

    public Whatsapp addNameChangedListener(Listener.Consumer.Binary<String, String> consumer) {
        addListener(new Listener() {
            @Override
            public void onNameChanged(String oldName, String newName) {
                consumer.accept(oldName, newName);
            }
        });
        return this;
    }

    public Whatsapp addNameChangedListener(Listener.Consumer.Ternary<Whatsapp, String, String> consumer) {
        addListener(new Listener() {
            @Override
            public void onNameChanged(Whatsapp whatsapp, String oldName, String newName) {
                consumer.accept(whatsapp, oldName, newName);
            }
        });
        return this;
    }

    public Whatsapp addAboutChangedListener(Listener.Consumer.Ternary<Whatsapp, String, String> consumer) {
        addListener(new Listener() {
            @Override
            public void onAboutChanged(Whatsapp whatsapp, String oldAbout, String newAbout) {
                consumer.accept(whatsapp, oldAbout, newAbout);
            }
        });
        return this;
    }

    public Whatsapp addAboutChangedListener(Listener.Consumer.Binary<String, String> consumer) {
        addListener(new Listener() {
            @Override
            public void onAboutChanged(String oldAbout, String newAbout) {
                consumer.accept(oldAbout, newAbout);
            }
        });
        return this;
    }

    public Whatsapp addLocaleChangedListener(Listener.Consumer.Ternary<Whatsapp, CountryLocale, CountryLocale> consumer) {
        addListener(new Listener() {
            @Override
            public void onLocaleChanged(Whatsapp whatsapp, CountryLocale oldLocale, CountryLocale newLocale) {
                consumer.accept(whatsapp, oldLocale, newLocale);
            }
        });
        return this;
    }

    public Whatsapp addLocaleChangedListener(Listener.Consumer.Binary<CountryLocale, CountryLocale> consumer) {
        addListener(new Listener() {
            @Override
            public void onLocaleChanged(CountryLocale oldLocale, CountryLocale newLocale) {
                consumer.accept(oldLocale, newLocale);
            }
        });
        return this;
    }

    public Whatsapp addContactBlockedListener(Listener.Consumer.Binary<Whatsapp, Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onContactBlocked(Whatsapp whatsapp, Contact contact) {
                consumer.accept(whatsapp, contact);
            }
        });
        return this;
    }

    public Whatsapp addContactBlockedListener(Listener.Consumer.Unary<Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onContactBlocked(Contact contact) {
                consumer.accept(contact);
            }
        });
        return this;
    }

    public Whatsapp addNewContactListener(Listener.Consumer.Unary<Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewContact(Contact contact) {
                consumer.accept(contact);
            }
        });
        return this;
    }

    public Whatsapp addNewContactListener(Listener.Consumer.Binary<Whatsapp, Contact> consumer) {
        addListener(new Listener() {
            @Override
            public void onNewContact(Whatsapp whatsapp, Contact contact) {
                consumer.accept(whatsapp, contact);
            }
        });
        return this;
    }

    public Whatsapp addPrivacySettingChangedListener(Listener.Consumer.Binary<PrivacySettingEntry, PrivacySettingEntry> consumer) {
        addListener(new Listener() {
            @Override
            public void onPrivacySettingChanged(PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry) {
                consumer.accept(oldPrivacyEntry, newPrivacyEntry);
            }
        });
        return this;
    }

    public Whatsapp addPrivacySettingChangedListener(Listener.Consumer.Ternary<Whatsapp, PrivacySettingEntry, PrivacySettingEntry> consumer) {
        addListener(new Listener() {
            @Override
            public void onPrivacySettingChanged(Whatsapp whatsapp, PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry) {
                consumer.accept(whatsapp, oldPrivacyEntry, newPrivacyEntry);
            }
        });
        return this;
    }

    public Whatsapp addLinkedDevicesListener(Listener.Consumer.Unary<Collection<Jid>> consumer) {
        addListener(new Listener() {
            @Override
            public void onLinkedDevices(Collection<Jid> devices) {
                consumer.accept(devices);
            }
        });
        return this;
    }

    public Whatsapp addLinkedDevicesListener(Listener.Consumer.Binary<Whatsapp, Collection<Jid>> consumer) {
        addListener(new Listener() {
            @Override
            public void onLinkedDevices(Whatsapp whatsapp, Collection<Jid> devices) {
                Listener.super.onLinkedDevices(whatsapp, devices);
            }
        });
        return this;
    }

    public Whatsapp addRegistrationCodeListener(Listener.Consumer.Binary<Whatsapp, Long> consumer) {
        addListener(new Listener() {
            @Override
            public void onRegistrationCode(Whatsapp whatsapp, long code) {
                consumer.accept(whatsapp, code);
            }
        });
        return this;
    }

    public Whatsapp addRegistrationCodeListener(Listener.Consumer.Unary<Long> consumer) {
        addListener(new Listener() {
            @Override
            public void onRegistrationCode(long code) {
                consumer.accept(code);
            }
        });
        return this;
    }

    public Whatsapp addCallListener(Listener.Consumer.Unary<Call> consumer) {
        addListener(new Listener() {
            @Override
            public void onCall(Call call) {
                consumer.accept(call);
            }
        });
        return this;
    }

    public Whatsapp addCallListener(Listener.Consumer.Binary<Whatsapp, Call> consumer) {
        addListener(new Listener() {
            @Override
            public void onCall(Whatsapp whatsapp, Call call) {
                consumer.accept(whatsapp, call);
            }
        });
        return this;
    }

    public Whatsapp addMessageReplyListener(ChatMessageInfo info, Consumer<MessageInfo<?>> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public Whatsapp addMessageReplyListener(ChatMessageInfo info, BiConsumer<Whatsapp, MessageInfo<?>> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public Whatsapp addMessageReplyListener(String id, Consumer<MessageInfo<?>> consumer) {
        return addListener(new Listener() {
            @Override
            public void onNewMessage(MessageInfo<?> info) {
                if (!info.id().equals(id)) {
                    return;
                }

                consumer.accept(info);
            }
        });
    }

    public Whatsapp addMessageReplyListener(String id, BiConsumer<Whatsapp, MessageInfo<?>> consumer) {
        return addListener(new Listener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo<?> info) {
                if (!info.id().equals(id)) {
                    return;
                }

                consumer.accept(whatsapp, info);
            }
        });
    }

    private Jid jidOrThrowError() {
        return store().jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
    }
}
