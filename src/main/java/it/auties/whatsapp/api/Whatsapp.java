package it.auties.whatsapp.api;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.listener.*;
import it.auties.whatsapp.listener.processor.RegisterListenerProcessor;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.business.*;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.companion.CompanionLinkResult;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MediaFile;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.model.reserved.ExtendedMediaMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessageBuilder;
import it.auties.whatsapp.model.message.standard.CallMessageBuilder;
import it.auties.whatsapp.model.message.standard.NewsletterAdminInviteMessageBuilder;
import it.auties.whatsapp.model.message.standard.ReactionMessageBuilder;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.newsletter.*;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.GdprAccountReport;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.product.LeaveNewsletterRequest;
import it.auties.whatsapp.model.request.*;
import it.auties.whatsapp.model.request.UpdateNewsletterRequest.UpdatePayload;
import it.auties.whatsapp.model.response.*;
import it.auties.whatsapp.model.setting.LocaleSettings;
import it.auties.whatsapp.model.setting.PushNameSettings;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.PatchRequest.PatchEntry;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.registration.WhatsappRegistration;
import it.auties.whatsapp.socket.SocketHandler;
import it.auties.whatsapp.socket.SocketState;
import it.auties.whatsapp.util.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.model.contact.ContactStatus.COMPOSING;
import static it.auties.whatsapp.model.contact.ContactStatus.RECORDING;

/**
 * A class used to interface a user to WhatsappWeb's WebSocket
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Whatsapp {
    // The instances are added and removed when the client connects/disconnects
    // This is to make sure that the instances remain in memory only as long as it's needed
    private static final Map<UUID, Whatsapp> instances = new ConcurrentHashMap<>();

    static Optional<Whatsapp> getInstanceByUuid(UUID uuid) {
        return Optional.ofNullable(instances.get(uuid));
    }

    static void removeInstanceByUuid(UUID uuid) {
        instances.remove(uuid);
    }

    private final SocketHandler socketHandler;

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

    /**
     * Checks if a number is already registered on Whatsapp
     *
     * @param phoneNumber a phone number(include the prefix)
     * @return a future
     */
    public static CompletableFuture<CheckNumberResponse> checkNumber(long phoneNumber) {
        return checkNumber(null, phoneNumber);
    }

    /**
     * Checks if a number is already registered on Whatsapp
     *
     * @param proxy the proxy to use, can be null
     * @param phoneNumber a phone number(include the prefix)
     * @return a future
     */
    public static CompletableFuture<CheckNumberResponse> checkNumber(URI proxy, long phoneNumber) {
        var randomedUUID = UUID.randomUUID();
        var store = Store.newStore(randomedUUID, phoneNumber, null, ClientType.MOBILE);
        store.setSerializer(ControllerSerializer.discarding());
        if(proxy != null) {
            store.setProxy(proxy);
        }
        var keys = Keys.newKeys(randomedUUID, phoneNumber, null, ClientType.MOBILE);
        keys.setSerializer(ControllerSerializer.discarding());
        store.setPhoneNumber(PhoneNumber.of(phoneNumber));
        var service = new WhatsappRegistration(store, keys, null, VerificationCodeMethod.NONE, false, false);
        return service.exists();
    }

    protected Whatsapp(Store store, Keys keys, ErrorHandler errorHandler, WebVerificationHandler webVerificationHandler) {
        this.socketHandler = new SocketHandler(this, store, keys, errorHandler, webVerificationHandler);
        addDisconnectionHandler(store);
        registerListenersAutomatically(store);
    }

    private static void addDisconnectionHandler(Store store) {
        store.addListener((OnDisconnected) (reason) -> {
            if (reason != DisconnectReason.RECONNECTING) {
                removeInstanceByUuid(store.uuid());
            }
        });
    }

    private void registerListenersAutomatically(Store store) {
        if (!store.autodetectListeners()) {
            return;
        }

        try {
            var clazz = Class.forName(RegisterListenerProcessor.qualifiedClassName());
            var method = clazz.getMethod(RegisterListenerProcessor.methodName(), Whatsapp.class);
            method.invoke(null, this);
        }catch (ClassNotFoundException exception) {
            // Ignored, this can happen if the compilation environment didn't register the processor
        }catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Cannot register listeners automatically", exception);
        }
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
        return socketHandler.connect()
                .thenRunAsync(() -> instances.put(store().uuid(), this))
                .thenApply(ignored -> this);
    }

    /**
     * Waits for this session to be disconnected
     */
    public void awaitDisconnection() {
        var future = new CompletableFuture<Void>();
        addDisconnectedListener((reason) -> {
            if(reason != DisconnectReason.RECONNECTING && reason != DisconnectReason.RESTORE) {
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
        return socketHandler.state() == SocketState.CONNECTED;
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
        Validate.isTrue(type.isSupported(value),
                "Cannot change setting %s to %s: this toggle cannot be used because Whatsapp doesn't support it", value.name(), type.name());
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
    public CompletableFuture<Void> changeNewChatsEphemeralTimer(ChatEphemeralTimer timer) {
        return socketHandler.sendQuery("set", "disappearing_mode", Node.of("disappearing_mode", Map.of("duration", timer.period().toSeconds())))
                .thenRun(() -> store().setNewChatsEphemeralTimer(timer));
    }

    /**
     * Creates a new request to get a document containing all the data that was collected by Whatsapp
     * about this user. It takes three business days to receive it. To query the newsletters status, use
     * {@link Whatsapp#getGdprAccountInfoStatus()}
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
    public CompletableFuture<GdprAccountReport> getGdprAccountInfoStatus() {
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
        var oldName = store().name();
        return socketHandler.sendNode(Node.of("presence", Map.of("name", newName)))
                .thenRun(() -> socketHandler.updateUserName(newName, oldName));
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
    public CompletableFuture<? extends MessageInfo> removeReaction(MessageInfo message) {
        return sendReaction(message, (String) null);
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo> sendReaction(MessageInfo message, Emoji reaction) {
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
    public CompletableFuture<? extends MessageInfo> sendReaction(MessageInfo message, String reaction) {
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
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
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider chat, String message) {
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
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public CompletableFuture<? extends MessageInfo> sendChatMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public CompletableFuture<? extends MessageInfo> sendNewsletterMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo quotedMessage) {
        var contextInfo = ContextInfo.of(quotedMessage);
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
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo quotedMessage) {
        var contextInfo = ContextInfo.of(quotedMessage);
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
    public CompletableFuture<NewsletterMessageInfo> sendNewsletterMessage(JidProvider chat, ContextualMessage<?> message, MessageInfo quotedMessage) {
        var contextInfo = ContextInfo.of(quotedMessage);
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
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider chat, Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider recipient, MessageContainer message) {
        return recipient.toJid().server() == JidServer.NEWSLETTER ? sendNewsletterMessage(recipient, message) : sendChatMessage(recipient, message);
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendChatMessage(JidProvider recipient, MessageContainer message) {
        Validate.isTrue(!recipient.toJid().hasServer(JidServer.NEWSLETTER), "Use sendNewsletterMessage to send a message in a newsletter");
        var timestamp = Clock.nowSeconds();
        return prepareChat(recipient, timestamp).thenComposeAsync(chatResult -> {
            var deviceInfo = new DeviceContextInfoBuilder()
                    .deviceListMetadataVersion(2)
                    .build();
            var key = new ChatMessageKeyBuilder()
                    .id(ChatMessageKey.randomId())
                    .chatJid(recipient.toJid())
                    .fromMe(true)
                    .senderJid(jidOrThrowError())
                    .build();
            var info = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING)
                    .senderJid(jidOrThrowError())
                    .key(key)
                    .message(message.withDeviceInfo(deviceInfo))
                    .timestampSeconds(timestamp)
                    .broadcast(recipient.toJid().hasServer(JidServer.BROADCAST))
                    .build();
            if (!chatResult) {
                return CompletableFuture.completedFuture(info.setStatus(MessageStatus.ERROR));
            }

            return addTrustedContact(recipient, timestamp)
                    .thenComposeAsync(trustResult -> sendDeltaChatRequest(recipient))
                    .thenComposeAsync(deltaResult -> deltaResult ? sendMessage(info) : CompletableFuture.completedFuture(info.setStatus(MessageStatus.ERROR)));
        });
    }

    private CompletableFuture<Boolean> sendDeltaChatRequest(JidProvider recipient) {
        if(!recipient.toJid().hasServer(JidServer.WHATSAPP)) {
            return CompletableFuture.completedFuture(true);
        }

        var sync = Node.of(
                "usync",
                Map.of(
                        "context", "interactive",
                        "index", "0",
                        "last", "true",
                        "mode", "delta",
                        "sid", ChatMessageKey.randomId()
                ),
                Node.of(
                        "query",
                        Node.of("business", Node.of("verified_name"), Node.of("profile", Map.of("v", 372))),
                        Node.of("contact"),
                        Node.of("disappearing_mode"),
                        Node.of("sidelist"),
                        Node.of("status")
                ),
                Node.of("list"),
                Node.of(
                        "side_list",
                        Node.of("user", Map.of("jid", recipient.toJid()))
                )
        );
        return socketHandler.sendQuery("get", "usync", sync)
                .thenApply(result -> !isNotOnWhatsapp(result));
    }

    private boolean isNotOnWhatsapp(Node result) {
        return result.findNode("usync")
                .flatMap(entry -> entry.findNode("result"))
                .flatMap(entry -> entry.findNode("sidelist"))
                .map(entry -> entry.attributes().hasValue("type", "out"))
                .orElse(false);
    }

    private CompletableFuture<Boolean> prepareChat(JidProvider recipient, long timestamp) {
        if(!recipient.toJid().hasServer(JidServer.WHATSAPP)) {
            return CompletableFuture.completedFuture(true);
        }

        if (store().findContactByJid(recipient.toJid()).isPresent()) {
            return CompletableFuture.completedFuture(true);
        }

        return getContactData(recipient).thenCompose(results -> {
            var out = results.stream().anyMatch(entry -> entry.hasDescription("out"));
            if (out) {
                return CompletableFuture.completedFuture(false);
            }

            var contactOut = results.stream()
                    .map(entry -> entry.findNode("contact"))
                    .flatMap(Optional::stream)
                    .anyMatch(entry -> entry.attributes().hasValue("type", "out"));
            if (contactOut) {
                return CompletableFuture.completedFuture(false);
            }

            var secondQuery = List.of(Node.of("disappearing_mode"));
            var secondList = List.of(Node.of("user", Map.of("jid", recipient.toJid())));
            return socketHandler.sendInteractiveQuery(secondQuery, secondList, List.of())
                    .thenCompose(secondResult -> socketHandler.sendQuery("get", "w:profile:picture", Map.of("target", recipient.toJid()), Node.of("picture", Map.of("type", "preview"))))
                    .thenCompose(thirdResult -> subscribeToPresence(recipient.toJid()))
                    .thenCompose(fourthResult -> socketHandler.querySessions(List.of(recipient.toJid())))
                    .thenApply(ignored -> true);
        });
    }

    private CompletableFuture<List<Node>> getContactData(JidProvider recipient) {
        var businessNode = Node.of("business", Node.of("verified_name"), Node.of("profile", Map.of("v", 372)));
        var contactNode = Node.of("contact");
        var lidNode = Node.of("lid");
        var userNode = Node.of("user", Node.of("contact", recipient.toJid().toPhoneNumber().getBytes(StandardCharsets.UTF_8)));
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
        var newsletter = store().findNewsletterByJid(recipient);
        Validate.isTrue(newsletter.isPresent(), "Cannot send a message in a newsletter that you didn't join");
        var oldServerId = newsletter.get()
                .newestMessage()
                .map(NewsletterMessageInfo::serverId)
                .orElse(0);
        var info = new NewsletterMessageInfo(
                ChatMessageKey.randomId(),
                oldServerId + 1,
                Clock.nowSeconds(),
                null,
                new ConcurrentHashMap<>(),
                message,
                MessageStatus.PENDING
        );
        info.setNewsletter(newsletter.get());
        return sendMessage(info);
    }

    /**
     * Builds and sends an edited message
     *
     * @param oldMessage the message to edit
     * @param newMessage the new message's content
     * @return a CompletableFuture
     */
    public <T extends MessageInfo> CompletableFuture<T> editMessage(T oldMessage, Message newMessage) {
        var oldMessageType = oldMessage.message().content().type();
        var newMessageType = newMessage.type();
        Validate.isTrue(oldMessageType == newMessageType,
                "Message type mismatch: %s != %s",
                oldMessageType, newMessageType);
        return switch (oldMessage) {
            case NewsletterMessageInfo oldNewsletterInfo -> {
                var info = new NewsletterMessageInfo(
                        oldNewsletterInfo.id(),
                        oldNewsletterInfo.serverId(),
                        Clock.nowSeconds(),
                        null,
                        new ConcurrentHashMap<>(),
                        MessageContainer.ofEditedMessage(newMessage),
                        MessageStatus.PENDING
                );
                info.setNewsletter(oldNewsletterInfo.newsletter());
                var request = new MessageSendRequest.Newsletter(info, Map.of("edit", getEditBit(info)));
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
                        .broadcast(oldChatInfo.chatJid().hasServer(JidServer.BROADCAST))
                        .build();
                var request = new MessageSendRequest.Chat(info, null, false, false, Map.of("edit", getEditBit(info)));
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
        var deviceInfo = new DeviceContextInfoBuilder()
                .deviceListMetadataVersion(2)
                .build();
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
                .chatJid(Jid.of("status@broadcast"))
                .fromMe(true)
                .senderJid(jidOrThrowError())
                .build();
        var info = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(jidOrThrowError())
                .key(key)
                .message(message.withDeviceInfo(deviceInfo))
                .timestampSeconds(timestamp)
                .broadcast(false)
                .build();
        return sendMessage(info);
    }

    /**
     * Sends a message to a chat
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> sendMessage(ChatMessageInfo info) {
        return socketHandler.sendMessage(new MessageSendRequest.Chat(info))
                .thenApply(ignored -> info);
    }

    /**
     * Sends a message to a newsletter
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<NewsletterMessageInfo> sendMessage(NewsletterMessageInfo info) {
        return socketHandler.sendMessage(new MessageSendRequest.Newsletter(info))
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
        var markAction = new MarkChatAsReadAction(read, Optional.of(range));
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
    public CompletableFuture<HasWhatsappResponse> hasWhatsapp(JidProvider contact) {
        return hasWhatsapp(new JidProvider[]{contact}).thenApply(result -> result.get(contact.toJid()));
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null map
     */
    public CompletableFuture<Map<Jid, HasWhatsappResponse>> hasWhatsapp(JidProvider... contacts) {
        var jids = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .toList();
        var contactNodes = jids.stream()
                .map(jid -> Node.of("user", Node.of("contact", jid.toPhoneNumber())))
                .toList();
        return socketHandler.sendInteractiveQuery(List.of(Node.of("contact")), contactNodes, List.of())
                .thenApplyAsync(result -> parseHasWhatsappResponse(jids, result));
    }

    private Map<Jid, HasWhatsappResponse> parseHasWhatsappResponse(List<Jid> contacts, List<Node> nodes) {
        var result = nodes.stream()
                .map(this::parseHasWhatsappResponse)
                .collect(Collectors.toMap(HasWhatsappResponse::contact, Function.identity(), (first, second) -> first, HashMap::new));
        contacts.stream()
                .filter(contact -> !result.containsKey(contact))
                .forEach(contact -> result.put(contact, new HasWhatsappResponse(contact, false)));
        return Collections.unmodifiableMap(result);
    }

    private HasWhatsappResponse parseHasWhatsappResponse(Node node) {
        var jid = node.attributes()
                .getRequiredJid("jid");
        var in = node.findNode("contact")
                .orElseThrow(() -> new NoSuchElementException("Missing contact in HasWhatsappResponse"))
                .attributes()
                .getRequiredString("type")
                .equals("in");
        return new HasWhatsappResponse(jid, in);
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
        var query = new UserChosenNameRequest(List.of(new UserChosenNameRequest.Variable(contactJid.toJid().user())));
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6556393721124826"), Json.writeValueAsBytes(query)))
                .thenApplyAsync(this::parseNameResponse);
    }

    private Optional<String> parseNameResponse(Node result) {
        return result.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(UserChosenNameResponse::ofJson)
                .flatMap(UserChosenNameResponse::name);
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status newsletters
     */
    public CompletableFuture<Optional<ContactAboutResponse>> queryAbout(JidProvider chat) {
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
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public CompletableFuture<GroupMetadata> queryGroupMetadata(JidProvider chat) {
        return socketHandler.queryGroupMetadata(chat.toJid());
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
        return result.findNode("business_profile")
                .flatMap(entry -> entry.findNode("profile"))
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
        return result.findNode("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite code in invite newsletters"))
                .attributes()
                .getRequiredString("code");
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
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", Node.of("invite", Map.of("code", inviteCode)))
                .thenApplyAsync(this::parseAcceptInvite);
    }

    private Optional<Chat> parseAcceptInvite(Node result) {
        return result.findNode("group")
                .flatMap(group -> group.attributes().getOptionalJid("jid"))
                .map(jid -> store().findChatByJid(jid).orElseGet(() -> store().addNewChat(jid)));
    }

    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     * @return a CompletableFuture
     */
    public CompletableFuture<Boolean> changePresence(boolean available) {
        var status = socketHandler.store().online();
        if (status == available) {
            return CompletableFuture.completedFuture(status);
        }

        var presence = available ? ContactStatus.AVAILABLE : ContactStatus.UNAVAILABLE;
        var node = Node.of("presence", Map.of("name", store().name(), "type", presence.toString()));
        return socketHandler.sendNodeWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(null, presence))
                .thenApplyAsync(ignored -> available);
    }

    private void updateSelfPresence(JidProvider chatJid, ContactStatus presence) {
        if (chatJid == null) {
            store().setOnline(presence == ContactStatus.AVAILABLE);
        }

        var self = store().findContactByJid(jidOrThrowError().toSimpleJid());
        if (self.isEmpty()) {
            return;
        }

        if (presence == ContactStatus.AVAILABLE || presence == ContactStatus.UNAVAILABLE) {
            self.get().setLastKnownPresence(presence);
        }
        if (chatJid != null) {
            store().findChatByJid(chatJid).ifPresent(chat -> chat.presences().put(self.get().jid(), presence));
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
        var knownPresence = store().findChatByJid(chatJid)
                .map(Chat::presences)
                .map(entry -> entry.get(jidOrThrowError().toSimpleJid()))
                .orElse(null);
        if (knownPresence == COMPOSING || knownPresence == RECORDING) {
            var node = Node.of("chatstate", Map.of("to", chatJid.toJid()), Node.of("paused"));
            return socketHandler.sendNodeWithNoResponse(node);
        }

        if (presence == COMPOSING || presence == RECORDING) {
            var tag = presence == RECORDING ? COMPOSING : presence;
            var node = Node.of("chatstate",
                    Map.of("to", chatJid.toJid()),
                    Node.of(COMPOSING.toString(), presence == RECORDING ? Map.of("media", "audio") : Map.of()));
            return socketHandler.sendNodeWithNoResponse(node)
                    .thenAcceptAsync(socketHandler -> updateSelfPresence(chatJid, presence));
        }

        var node = Node.of("presence", Map.of("type", presence.toString(), "name", store().name()));
        return socketHandler.sendNodeWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(chatJid, presence));
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> promoteGroupParticipant(JidProvider group, JidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, contacts);
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> demoteGroupParticipant(JidProvider group, JidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, contacts);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> addGroupParticipant(JidProvider group, JidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, contacts);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> removeGroupParticipant(JidProvider group, JidProvider... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.REMOVE, contacts);
    }

    private CompletableFuture<List<Jid>> executeActionOnGroupParticipant(JidProvider group, GroupAction action, JidProvider... jids) {
        var body = Arrays.stream(jids)
                .map(JidProvider::toJid)
                .map(jid -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(jid, "Cannot execute action on yourself"))))
                .map(innerBody -> Node.of(action.data(), innerBody))
                .toArray(Node[]::new);
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(result -> parseGroupActionResponse(result, group, action));
    }

    private Jid checkGroupParticipantJid(Jid jid, String errorMessage) {
        Validate.isTrue(!Objects.equals(jid.toSimpleJid(), jidOrThrowError().toSimpleJid()), errorMessage);
        return jid;
    }

    private List<Jid> parseGroupActionResponse(Node result, JidProvider groupJid, GroupAction action) {
        var results = result.findNode(action.data())
                .map(body -> body.findNodes("participant"))
                .stream()
                .flatMap(Collection::stream)
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
        store().findChatByJid(groupJid)
                .ifPresent(chat -> results.forEach(entry -> handleGroupAction(action, chat, entry)));
        return results;
    }

    private void handleGroupAction(GroupAction action, Chat chat, Jid entry) {
        switch (action) {
            case ADD -> chat.addParticipant(entry, GroupRole.USER);
            case REMOVE -> {
                chat.removeParticipant(entry);
                chat.addPastParticipant(new GroupPastParticipant(entry, GroupPastParticipant.Reason.REMOVED, Clock.nowSeconds()));
            }
            case PROMOTE -> chat.findParticipant(entry)
                    .ifPresent(participant -> participant.setRole(GroupRole.ADMIN));
            case DEMOTE -> chat.findParticipant(entry)
                    .ifPresent(participant -> participant.setRole(GroupRole.USER));
        }
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
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeGroupDescription(group, description, descriptionId.orElse(null)))
                .thenRun(() -> {});
    }

    private CompletableFuture<Void> changeGroupDescription(JidProvider group, String description, String descriptionId) {
        var descriptionNode = Optional.ofNullable(description)
                .map(content -> Node.of("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", ChatMessageKey.randomId(), () -> description != null)
                .put("delete", true, () -> description == null)
                .put("prev", descriptionId, () -> descriptionId != null)
                .toMap();
        var body = Node.of("description", attributes, descriptionNode);
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenRun(() -> onDescriptionSet(group, description));
    }

    private void onDescriptionSet(JidProvider groupJid, String description) {
        if (groupJid instanceof Chat chat) {
            chat.setDescription(description);
            return;
        }

        var group = store().findChatByJid(groupJid);
        group.ifPresent(chat -> chat.setDescription(description));
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
        Validate.isTrue(group.toJid().hasServer(JidServer.GROUP), "This method only accepts groups");
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
        return changeGroupPicture(jidOrThrowError(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeGroupPicture(JidProvider group, URI image) {
        var imageFuture = image == null ? CompletableFuture.completedFuture((byte[]) null) : Medias.downloadAsync(image);
        return imageFuture.thenComposeAsync(imageResult -> changeGroupPicture(group, imageResult));
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Void> changeGroupPicture(JidProvider group, byte[] image) {
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), profilePic);
        return socketHandler.sendQuery(group.toJid().toSimpleJid(), "set", "w:profile:picture", body)
                .thenRun(() -> {});
    }

    /**
     * Creates a new group
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<GroupMetadata>> createGroup(String subject, JidProvider... contacts) {
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
    public CompletableFuture<Optional<GroupMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider... contacts) {
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
    public CompletableFuture<Optional<GroupMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentGroup) {
        return createGroup(subject, timer, parentGroup, new JidProvider[0]);
    }

    /**
     * Creates a new group
     *
     * @param subject         the new group's name
     * @param timer           the default ephemeral timer for messages sent in this group
     * @param parentCommunity the community to whom the new group will be linked
     * @param contacts        at least one contact to add to the group, not enforced if part of a community
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<GroupMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity, JidProvider... contacts) {
        var timestamp = Clock.nowSeconds();
        Validate.isTrue(!subject.isBlank(), "The subject of a group cannot be blank");
        Validate.isTrue( parentCommunity != null || contacts.length >= 1, "Expected at least 1 member for this group");
        return addContacts(contacts)
                .thenComposeAsync(this::prepareGroupParticipants)
                .thenComposeAsync(availableMembers -> {
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
                            .map(Jid::toSimpleJid)
                            .distinct()
                            .map(contact -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(contact.toJid(), "Cannot create group with yourself as a participant"))))
                            .forEach(children::add);
                    var body = Node.of("create", Map.of("subject", subject, "key", timestamp), children);
                    var future = socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", body);
                    sendGroupWam(timestamp);
                    return future.thenApplyAsync(this::parseGroupResponse);
                }, CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));
    }

    private CompletableFuture<List<Jid>> prepareGroupParticipants(List<Jid> availableMembers) {
        return socketHandler.querySessions(availableMembers)
                .thenComposeAsync(ignored -> queryPreviewPics(availableMembers))
                .thenApplyAsync(ignored -> availableMembers);
    }

    private CompletableFuture<Void> queryPreviewPics(List<Jid> availableMembers) {
        var futures = availableMembers.stream()
                .map(entry -> socketHandler.sendQuery("get", "w:profile:picture", Map.of("target", entry), Node.of("picture", Map.of("type", "preview"))))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private void sendGroupWam(long timestamp) {
        var wamBinary = "57414d0501020001200b800d086950686f6e652058800f0631362e372e34801109322e32342e342e373810152017502f0dd9e065206928830138790604387b060288eb0a0361746e887911904246342c316a332c55772c79492c39442c31552c45722c31432c41472c324a2c49662c35552c4f582c31462c352c41792c38772c4c442c414a2c35362c642c346f2c466d2c37512c36392c32442c332c31762c33772c337a2c31332c7a2c512c722c33752c32652c522c6f2c36662c502c692c572c372c562c4b2c382c31532c4c2c31362c31702c742c6d2c32382c5088a5134632343835312c32343336362c32313031382c32333939332c32333633302c31373832352c31373833302c32353530382c32353530302c363633372c32323634392c3233363237186b1828a71c88911e063230483234308879240431372e3018ed3318ab3888fb3c0935363936333037343129b4072601";
        var wamData = new String(HexFormat.of().parseHex(wamBinary))
                .replace("iPhone X", socketHandler.store().device().model().replaceAll("_", " "))
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
                .map(contact -> Node.of("user", Node.of("contact", contact.toJid().toPhoneNumber().getBytes())))
                .toList();
        var sync = Node.of(
                "usync",
                Map.of(
                        "context", "add",
                        "index", "0",
                        "last", "true",
                        "mode", "delta",
                        "sid", ChatMessageKey.randomId()
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
        return result.findNode("usync")
                .flatMap(usync -> usync.findNode("list"))
                .map(list -> list.findNodes("user"))
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

        var contactNode = user.findNode("contact");
        if(contactNode.isEmpty() || !contactNode.get().attributes().hasValue("type", "in")) {
            return null;
        }

        return jid.get();
    }

    private Optional<GroupMetadata> parseGroupResponse(Node response) {
        return Optional.ofNullable(response)
                .flatMap(node -> node.findNode("group"))
                .map(socketHandler::parseGroupMetadata)
                .map(this::addNewGroup);
    }

    private GroupMetadata addNewGroup(GroupMetadata result) {
        var chatBuilder = new ChatBuilder()
                .jid(result.jid())
                .description(result.description().orElse(null))
                .participants(new ArrayList<>(result.participants()))
                .founder(result.founder().orElse(null));
        result.foundationTimestamp()
                .map(ChronoZonedDateTime::toEpochSecond)
                .ifPresent(chatBuilder::foundationTimestampSeconds);
        store().addChat(chatBuilder.build());
        return result;
    }

    private String findErrorNode(Node result) {
        return Optional.ofNullable(result)
                .flatMap(node -> node.findNode("error"))
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
        var body = Node.of("leave", Node.of("group", Map.of("id", group.toJid())));
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", body)
                .thenAcceptAsync(ignored -> handleLeaveGroup(group));
    }

    private void handleLeaveGroup(JidProvider group) {
        var chat = group instanceof Chat entry ? entry : store()
                .findChatByJid(group)
                .orElse(null);
        if (chat != null) {
            var pastParticipant = new GroupPastParticipantBuilder()
                    .jid(jidOrThrowError().toSimpleJid())
                    .reason(GroupPastParticipant.Reason.REMOVED)
                    .timestampSeconds(Clock.nowSeconds())
                    .build();
            chat.addPastParticipant(pastParticipant);
        }
    }

    /**
     * Links any number of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups    the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public CompletableFuture<Map<Jid, Boolean>> linkGroupsToCommunity(JidProvider community, JidProvider... groups) {
        var body = Arrays.stream(groups)
                .map(entry -> Node.of("group", Map.of("jid", entry.toJid())))
                .toArray(Node[]::new);
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.of("links", Node.of("link", Map.of("link_type", "sub_group"), body)))
                .thenApplyAsync(result -> parseLinksResponse(result, groups));
    }

    private Map<Jid, Boolean> parseLinksResponse(Node result, JidProvider[] groups) {
        var success = result.findNode("links")
                .stream()
                .map(entry -> entry.findNodes("link"))
                .flatMap(Collection::stream)
                .filter(entry -> entry.attributes().hasValue("link_type", "sub_group"))
                .map(entry -> entry.findNode("group"))
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
    public CompletableFuture<Boolean> unlinkGroupFromCommunity(JidProvider community, JidProvider group) {
        return socketHandler.sendQuery(community.toJid(), "set", "w:g2", Node.of("unlink", Map.of("unlink_type", "sub_group"), Node.of("group", Map.of("jid", group.toJid()))))
                .thenApplyAsync(result -> parseUnlinkResponse(result, group));
    }

    private boolean parseUnlinkResponse(Node result, JidProvider group) {
        return result.findNode("unlink")
                .filter(entry -> entry.attributes().hasValue("unlink_type", "sub_group"))
                .flatMap(entry -> entry.findNode("group"))
                .map(entry -> entry.attributes().hasValue("jid", group.toJid().toString()))
                .isPresent();
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
        var muteAction = new MuteAction(true, OptionalLong.of(endTimeStamp), false);
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

        var muteAction = new MuteAction(false, null, false);
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
        return switch (chat.toJid().server()) {
            case USER, WHATSAPP -> {
                var message = new ProtocolMessageBuilder()
                        .protocolType(ProtocolMessage.Type.EPHEMERAL_SETTING)
                        .ephemeralExpiration(timer.period().toSeconds())
                        .build();
                yield sendMessage(chat, message)
                        .thenRun(() -> {
                        });
            }
            case GROUP -> {
                var body = timer == ChatEphemeralTimer.OFF ? Node.of("not_ephemeral") : Node.of("ephemeral", Map.of("expiration", timer.period()
                        .toSeconds()));
                yield socketHandler.sendQuery(chat.toJid(), "set", "w:g2", body)
                        .thenRun(() -> {
                        });
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

        var pinAction = new PinAction(pin);
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

        var starAction = new StarAction(star);
        var syncAction = ActionValueSync.of(starAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> info);
    }

    private String fromMeToFlag(MessageInfo info) {
        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        return booleanToInt(fromMe);
    }

    private String participantToFlag(MessageInfo info) {
        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        return info.parentJid().hasServer(JidServer.GROUP)
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
        var archiveAction = new ArchiveChatAction(archive, Optional.of(range));
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
        var revokeInfo = new NewsletterMessageInfo(
                messageInfo.id(),
                messageInfo.serverId(),
                Clock.nowSeconds(),
                null,
                new ConcurrentHashMap<>(),
                MessageContainer.empty(),
                MessageStatus.PENDING
        );
        revokeInfo.setNewsletter(messageInfo.newsletter());
        var attrs = Map.of("edit", getDeleteBit(messageInfo));
        var request = new MessageSendRequest.Newsletter(revokeInfo, attrs);
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
            var sender = messageInfo.chatJid().hasServer(JidServer.GROUP) ? jidOrThrowError() : null;
            var key = new ChatMessageKeyBuilder()
                    .id(ChatMessageKey.randomId())
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
            var request = new MessageSendRequest.Chat(revokeInfo, null, false, false, attrs);
            return socketHandler.sendMessage(request);
        }

        return switch (store().clientType()) {
            case WEB -> {
                var range = createRange(messageInfo.chatJid(), false);
                var deleteMessageAction = new DeleteMessageForMeAction(false, messageInfo.timestampSeconds().orElse(0L));
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


    private int getEditBit(MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.NEWSLETTER)) {
            return 3;
        }

        return 1;
    }

    private int getDeleteBit(MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.NEWSLETTER)) {
            return 8;
        }

        var fromMe = Objects.equals(info.senderJid().toSimpleJid(), jidOrThrowError().toSimpleJid());
        if (info.parentJid().hasServer(JidServer.GROUP) && !fromMe) {
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
        var deleteChatAction = new DeleteChatAction(Optional.of(range));
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
        var clearChatAction = new ClearChatAction(Optional.of(range));
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
        var keyNode = result.findNode("profile").flatMap(entry -> entry.findNode(key));
        if (keyNode.isEmpty()) {
            return;
        }
        var actual = keyNode.get()
                .contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing business %s newsletters, something went wrong: %s".formatted(key, findErrorNode(result))));
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
        Validate.isTrue(email == null || isValidEmail(email), "Invalid email: %s", email);
        return changeBusinessAttribute("email", email);
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile("^(.+)@(\\S+)$")
                .matcher(email)
                .matches();
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
    public CompletableFuture<?> queryBusinessCollections(JidProvider contact) {
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
    public CompletableFuture<Optional<byte[]>> downloadMedia(ChatMessageInfo info) {
        if (!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        return downloadMedia(mediaMessage).thenCompose(result -> {
            if (result.isPresent()) {
                return CompletableFuture.completedFuture(result);
            }

            return requireMediaReupload(info)
                    .thenCompose(ignored -> downloadMedia(mediaMessage));
        });
    }

    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, an empty optional will be returned.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<byte[]>> downloadMedia(NewsletterMessageInfo info) {
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
    public CompletableFuture<Optional<byte[]>> downloadMedia(MediaMessage<?> mediaMessage) {
        if (!(mediaMessage instanceof ExtendedMediaMessage<?> extendedMediaMessage)) {
            return Medias.downloadAsync(mediaMessage);
        }

        var decodedMedia = extendedMediaMessage.decodedMedia();
        if (decodedMedia.isPresent()) {
            return CompletableFuture.completedFuture(decodedMedia);
        }


        return Medias.downloadAsync(mediaMessage).thenApply(result -> {
            result.ifPresent(extendedMediaMessage::setDecodedMedia);
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
        if (!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        var mediaKey = mediaMessage.mediaKey()
                .orElseThrow(() -> new NoSuchElementException("Missing media key"));
        var retryKey = Hkdf.extractAndExpand(mediaKey, "WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
        var retryIv = Bytes.random(12);
        var retryIdData = info.key().id().getBytes(StandardCharsets.UTF_8);
        var receipt = ServerErrorReceiptSpec.encode(new ServerErrorReceipt(info.id()));
        var ciphertext = AesGcm.encrypt(retryIv, receipt, retryKey, retryIdData);
        var rmrAttributes = Attributes.of()
                .put("jid", info.chatJid())
                .put("from_me", String.valueOf(info.fromMe()))
                .put("participant", info.senderJid(), () -> !Objects.equals(info.chatJid(), info.senderJid()))
                .toMap();
        var node = Node.of("receipt", Map.of("id", info.key().id(), "to", jidOrThrowError()
                .toSimpleJid(), "type", "server-error"), Node.of("encrypt", Node.of("enc_p", ciphertext), Node.of("enc_iv", retryIv)), Node.of("rmr", rmrAttributes));
        return socketHandler.sendNode(node, result -> result.hasDescription("notification"))
                .thenAcceptAsync(result -> parseMediaReupload(info, mediaMessage, retryKey, retryIdData, result));
    }

    private void parseMediaReupload(ChatMessageInfo info, MediaMessage<?> mediaMessage, byte[] retryKey, byte[] retryIdData, Node node) {
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
        var mediaRetryNotificationData = AesGcm.decrypt(mediaIv, mediaPayload, retryKey, retryIdData);
        var mediaRetryNotification = MediaRetryNotificationSpec.decode(mediaRetryNotificationData);
        var directPath = mediaRetryNotification.directPath()
                .orElseThrow(() -> new RuntimeException("Media reupload failed"));
        mediaMessage.setMediaUrl(Medias.createMediaUrl(directPath));
        mediaMessage.setMediaDirectPath(directPath);
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
    public CompletableFuture<Optional<GroupMetadata>> createCommunity(String subject, String body) {
        var descriptionId = HexFormat.of().formatHex(Bytes.random(12));
        var entry = Node.of("create", Map.of("subject", subject),
                Node.of("description", Map.of("id", descriptionId),
                        Node.of("body", Objects.requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8))),
                Node.of("parent", Map.of("default_membership_approval_mode", "request_required")),
                Node.of("allow_non_admin_sub_group_creation"));
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", entry)
                .thenApplyAsync(node -> node.findNode("group").map(socketHandler::parseGroupMetadata));
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
        Validate.isTrue(community.toJid().hasServer(JidServer.GROUP), "This method only accepts communities");
        var body = switch (setting) {
            case MODIFY_GROUPS ->
                    Node.of(policy == ChatSettingPolicy.ANYONE ? "allow_non_admin_sub_group_creation" : "not_allow_non_admin_sub_group_creation");
        };
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", body)
                .thenRun(() -> {});
    }


    /**
     * Unlinks all the companions of this device
     *
     * @return a future
     */
    public CompletableFuture<Whatsapp> unlinkDevices() {
        return socketHandler.sendQuery("set", "md", Node.of("remove-companion-device", Map.of("all", true, "reason", "user_initiated")))
                .thenRun(() -> store().removeLinkedCompanions())
                .thenApply(ignored -> this);
    }

    /**
     * Unlinks a specific companion
     *
     * @param companion the non-null companion to unlink
     * @return a future
     */
    public CompletableFuture<Whatsapp> unlinkDevice(Jid companion) {
        Validate.isTrue(companion.hasAgent(), "Expected companion, got jid without agent: %s", companion);
        return socketHandler.sendQuery("set", "md", Node.of("remove-companion-device", Map.of("jid", companion, "reason", "user_initiated")))
                .thenRun(() -> store().removeLinkedCompanion(companion))
                .thenApply(ignored -> this);
    }

    /**
     * Links a companion to this device
     *
     * @param qrCode the non-null qr code as an image
     * @return a future
     */
    public CompletableFuture<CompanionLinkResult> linkDevice(byte[] qrCode) {
        try {
            var inputStream = new ByteArrayInputStream(qrCode);
            var luminanceSource = new BufferedImageLuminanceSource(ImageIO.read(inputStream));
            var hybridBinarizer = new HybridBinarizer(luminanceSource);
            var binaryBitmap = new BinaryBitmap(hybridBinarizer);
            var reader = new QRCodeReader();
            var result = reader.decode(binaryBitmap);
            return linkDevice(result.getText());
        } catch (IOException | NotFoundException | ChecksumException | FormatException exception) {
            throw new IllegalArgumentException("Cannot read qr code", exception);
        }
    }

    /**
     * Links a companion to this device
     * Mobile api only
     *
     * @param qrCodeData the non-null qr code as a String
     * @return a future
     */
    public CompletableFuture<CompanionLinkResult> linkDevice(String qrCodeData) {
        Validate.isTrue(store().clientType() == ClientType.MOBILE, "Device linking is only available for the mobile api");
        var maxDevices = getMaxLinkedDevices();
        if (store().linkedDevices().size() > maxDevices) {
            return CompletableFuture.completedFuture(CompanionLinkResult.MAX_DEVICES_ERROR);
        }

        var qrCodeParts = qrCodeData.split(",");
        Validate.isTrue(qrCodeParts.length >= 4, "Expected qr code to be made up of at least four parts");
        var ref = qrCodeParts[0];
        var publicKey = Base64.getDecoder().decode(qrCodeParts[1]);
        var advIdentity = Base64.getDecoder().decode(qrCodeParts[2]);
        var identityKey = Base64.getDecoder().decode(qrCodeParts[3]);
        return socketHandler.sendQuery("set", "w:sync:app:state", Node.of("delete_all_data"))
                .thenComposeAsync(ignored -> linkDevice(advIdentity, identityKey, ref, publicKey));
    }

    private CompletableFuture<CompanionLinkResult> linkDevice(byte[] advIdentity, byte[] identityKey, String ref, byte[] publicKey) {
        var deviceIdentity = new DeviceIdentityBuilder()
                .rawId(ThreadLocalRandom.current().nextInt(800_000_000, 900_000_000))
                .keyIndex(store().linkedDevices().size() + 1)
                .timestamp(Clock.nowSeconds())
                .build();
        var deviceIdentityBytes = DeviceIdentitySpec.encode(deviceIdentity);
        var accountSignatureMessage = Bytes.concat(
                Specification.Whatsapp.ACCOUNT_SIGNATURE_HEADER,
                deviceIdentityBytes,
                advIdentity
        );
        var accountSignature = Curve25519.sign(keys().identityKeyPair().privateKey(), accountSignatureMessage, true);
        var signedDeviceIdentity = new SignedDeviceIdentityBuilder()
                .accountSignature(accountSignature)
                .accountSignatureKey(keys().identityKeyPair().publicKey())
                .details(deviceIdentityBytes)
                .build();
        var signedDeviceIdentityBytes = SignedDeviceIdentitySpec.encode(signedDeviceIdentity);
        var deviceIdentityHmac = new SignedDeviceIdentityHMACBuilder()
                .hmac(Hmac.calculateSha256(signedDeviceIdentityBytes, identityKey))
                .details(signedDeviceIdentityBytes)
                .build();
        var knownDevices = store().linkedDevices()
                .stream()
                .map(Jid::device)
                .toList();
        var keyIndexList = new KeyIndexListBuilder()
                .rawId(deviceIdentity.rawId())
                .timestamp(deviceIdentity.timestamp())
                .validIndexes(knownDevices)
                .build();
        var keyIndexListBytes = KeyIndexListSpec.encode(keyIndexList);
        var deviceSignatureMessage = Bytes.concat(Specification.Whatsapp.DEVICE_MOBILE_SIGNATURE_HEADER, keyIndexListBytes);
        var keyAccountSignature = Curve25519.sign(keys().identityKeyPair().privateKey(), deviceSignatureMessage, true);
        var signedKeyIndexList = new SignedKeyIndexListBuilder()
                .accountSignature(keyAccountSignature)
                .details(keyIndexListBytes)
                .build();
        return socketHandler.sendQuery("set", "md", Node.of("pair-device",
                        Node.of("ref", ref),
                        Node.of("pub-key", publicKey),
                        Node.of("device-identity", SignedDeviceIdentityHMACSpec.encode(deviceIdentityHmac)),
                        Node.of("key-index-list", Map.of("ts", deviceIdentity.timestamp()), SignedKeyIndexListSpec.encode(signedKeyIndexList))))
                .thenComposeAsync(result -> handleCompanionPairing(result, deviceIdentity.keyIndex()));
    }

    private int getMaxLinkedDevices() {
        var maxDevices = socketHandler.store().properties().get("linked_device_max_count");
        if (maxDevices == null) {
            return Specification.Whatsapp.MAX_COMPANIONS;
        }

        try {
            return Integer.parseInt(maxDevices);
        } catch (NumberFormatException exception) {
            return Specification.Whatsapp.MAX_COMPANIONS;
        }
    }

    private CompletableFuture<CompanionLinkResult> handleCompanionPairing(Node result, int keyId) {
        if (result.attributes().hasValue("type", "error")) {
            var error = result.findNode("error")
                    .filter(entry -> entry.attributes().hasValue("text", "resource-limit"))
                    .map(entry -> CompanionLinkResult.MAX_DEVICES_ERROR)
                    .orElse(CompanionLinkResult.RETRY_ERROR);
            return CompletableFuture.completedFuture(error);
        }

        var device = result.findNode("device")
                .flatMap(entry -> entry.attributes().getOptionalJid("jid"))
                .orElse(null);
        if (device == null) {
            return CompletableFuture.completedFuture(CompanionLinkResult.RETRY_ERROR);
        }

        return awaitCompanionRegistration(device)
                .thenComposeAsync(ignored -> socketHandler.sendQuery("get", "encrypt", Node.of("key", Node.of("user", Map.of("jid", device)))))
                .thenComposeAsync(encryptResult -> handleCompanionEncrypt(encryptResult, device, keyId));
    }

    private CompletableFuture<Void> awaitCompanionRegistration(Jid device) {
        var future = new CompletableFuture<Void>();
        OnLinkedDevices listener = data -> {
            if (data.contains(device)) {
                future.complete(null);
            }
        };
        addLinkedDevicesListener(listener);
        return future.orTimeout(Specification.Whatsapp.COMPANION_PAIRING_TIMEOUT, TimeUnit.SECONDS)
                .exceptionally(ignored -> null)
                .thenRun(() -> removeListener(listener));
    }

    private CompletableFuture<CompanionLinkResult> handleCompanionEncrypt(Node result, Jid companion, int keyId) {
        store().addLinkedDevice(companion, keyId);
        socketHandler.parseSessions(result);
        return sendInitialSecurityMessage(companion)
                .thenComposeAsync(ignore -> sendAppStateKeysMessage(companion))
                .thenComposeAsync(ignore -> sendInitialNullMessage(companion))
                .thenComposeAsync(ignore -> sendInitialStatusMessage(companion))
                .thenComposeAsync(ignore -> sendPushNamesMessage(companion))
                .thenComposeAsync(ignore -> sendInitialBootstrapMessage(companion))
                .thenComposeAsync(ignore -> sendRecentMessage(companion))
                .thenComposeAsync(ignored -> syncCompanionState(companion))
                .thenApplyAsync(ignored -> CompanionLinkResult.SUCCESS);
    }

    private CompletableFuture<Void> syncCompanionState(Jid companion) {
        var criticalUnblockLowRequest = createCriticalUnblockLowRequest();
        var criticalBlockRequest = createCriticalBlockRequest();
        return socketHandler.pushPatches(companion, List.of(criticalBlockRequest, criticalUnblockLowRequest)).thenComposeAsync(ignored -> {
            var regularLowRequests = createRegularLowRequests();
            var regularRequests = createRegularRequests();
            return socketHandler.pushPatches(companion, List.of(regularLowRequests, regularRequests));
        });
    }

    private PatchRequest createRegularRequests() {
        return new PatchRequest(PatchType.REGULAR, List.of());
    }

    private PatchRequest createRegularLowRequests() {
        var timeFormatEntry = createTimeFormatEntry();
        var primaryVersion = new PrimaryVersionAction(store().version().toString());
        var sessionVersionEntry = createPrimaryVersionEntry(primaryVersion, "session@s.whatsapp.net");
        var keepVersionEntry = createPrimaryVersionEntry(primaryVersion, "current@s.whatsapp.net");
        var nuxEntry = createNuxRequest();
        var androidEntry = createAndroidEntry();
        var entries = Stream.of(timeFormatEntry, sessionVersionEntry, keepVersionEntry, nuxEntry, androidEntry)
                .filter(Objects::nonNull)
                .toList();
        // TODO: Archive chat actions, StickerAction
        return new PatchRequest(PatchType.REGULAR_LOW, entries);
    }

    // FIXME: Settings can't be serialized
    private PatchRequest createCriticalBlockRequest() {
        var localeEntry = createLocaleEntry();
        var pushNameEntry = createPushNameEntry();
        return new PatchRequest(PatchType.CRITICAL_BLOCK, List.of(localeEntry, pushNameEntry));
    }

    private PatchRequest createCriticalUnblockLowRequest() {
        var criticalUnblockLow = createContactEntries();
        return new PatchRequest(PatchType.CRITICAL_UNBLOCK_LOW, criticalUnblockLow);
    }

    private List<PatchEntry> createContactEntries() {
        return store().contacts()
                .stream()
                .filter(entry -> entry.shortName().isPresent() || entry.fullName().isPresent())
                .map(this::createContactRequestEntry)
                .collect(Collectors.toList());
    }

    private PatchEntry createPushNameEntry() {
        var pushNameSetting = new PushNameSettings(store().name());
        return PatchEntry.of(ActionValueSync.of(pushNameSetting), Operation.SET);
    }

    private PatchEntry createLocaleEntry() {
        var localeSetting = new LocaleSettings(store().locale().toString());
        return PatchEntry.of(ActionValueSync.of(localeSetting), Operation.SET);
    }

    private PatchEntry createAndroidEntry() {
        if (!store().device().platform().isAndroid()) {
            return null;
        }

        var action = new AndroidUnsupportedActions(true);
        return PatchEntry.of(ActionValueSync.of(action), Operation.SET);
    }

    private PatchEntry createNuxRequest() {
        var nuxAction = new NuxAction(true);
        var timeFormatSync = ActionValueSync.of(nuxAction);
        return PatchEntry.of(timeFormatSync, Operation.SET, "keep@s.whatsapp.net");
    }

    private PatchEntry createPrimaryVersionEntry(PrimaryVersionAction primaryVersion, String to) {
        var timeFormatSync = ActionValueSync.of(primaryVersion);
        return PatchEntry.of(timeFormatSync, Operation.SET, to);
    }

    private PatchEntry createTimeFormatEntry() {
        var timeFormatAction = new TimeFormatAction(store().twentyFourHourFormat());
        var timeFormatSync = ActionValueSync.of(timeFormatAction);
        return PatchEntry.of(timeFormatSync, Operation.SET);
    }

    private PatchEntry createContactRequestEntry(Contact contact) {
        var action = new ContactAction(null, contact.shortName(), contact.fullName());
        var sync = ActionValueSync.of(action);
        return PatchEntry.of(sync, Operation.SET, contact.jid().toString());
    }

    private CompletableFuture<Void> sendRecentMessage(Jid jid) {
        var pushNames = new HistorySyncBuilder()
                .conversations(List.of())
                .syncType(HistorySync.Type.RECENT)
                .build();
        return sendHistoryProtocolMessage(jid, pushNames, HistorySync.Type.PUSH_NAME);
    }

    private CompletableFuture<Void> sendPushNamesMessage(Jid jid) {
        var pushNamesData = store()
                .contacts()
                .stream()
                .filter(entry -> entry.chosenName().isPresent())
                .map(entry -> new PushName(entry.jid().toString(), entry.chosenName()))
                .toList();
        var pushNames = new HistorySyncBuilder()
                .pushNames(pushNamesData)
                .syncType(HistorySync.Type.PUSH_NAME)
                .build();
        return sendHistoryProtocolMessage(jid, pushNames, HistorySync.Type.PUSH_NAME);
    }

    private CompletableFuture<Void> sendInitialStatusMessage(Jid jid) {
        var initialStatus = new HistorySyncBuilder()
                .statusV3Messages(new ArrayList<>(store().status()))
                .syncType(HistorySync.Type.INITIAL_STATUS_V3)
                .build();
        return sendHistoryProtocolMessage(jid, initialStatus, HistorySync.Type.INITIAL_STATUS_V3);
    }

    private CompletableFuture<Void> sendInitialBootstrapMessage(Jid jid) {
        var chats = store().chats()
                .stream()
                .toList();
        var initialBootstrap = new HistorySyncBuilder()
                .conversations(chats)
                .syncType(HistorySync.Type.INITIAL_BOOTSTRAP)
                .build();
        return sendHistoryProtocolMessage(jid, initialBootstrap, HistorySync.Type.INITIAL_BOOTSTRAP);
    }

    private CompletableFuture<Void> sendInitialNullMessage(Jid jid) {
        var pastParticipants = store().chats()
                .stream()
                .map(this::getPastParticipants)
                .filter(Objects::nonNull)
                .toList();
        var initialBootstrap = new HistorySyncBuilder()
                .syncType(HistorySync.Type.NON_BLOCKING_DATA)
                .pastParticipants(pastParticipants)
                .build();
        return sendHistoryProtocolMessage(jid, initialBootstrap, null);
    }

    private GroupPastParticipants getPastParticipants(Chat chat) {
        if (chat.pastParticipants().isEmpty()) {
            return null;
        }

        return new GroupPastParticipantsBuilder()
                .groupJid(chat.jid())
                .pastParticipants(new ArrayList<>(chat.pastParticipants()))
                .build();
    }

    private CompletableFuture<Void> sendAppStateKeysMessage(Jid companion) {
        var preKeys = IntStream.range(0, 10)
                .mapToObj(index -> createAppKey(companion, index))
                .toList();
        keys().addAppKeys(companion, preKeys);
        var appStateSyncKeyShare = new AppStateSyncKeyShareBuilder()
                .keys(preKeys)
                .build();
        var result = new ProtocolMessageBuilder()
                .protocolType(ProtocolMessage.Type.APP_STATE_SYNC_KEY_SHARE)
                .appStateSyncKeyShare(appStateSyncKeyShare)
                .build();
        return socketHandler.sendPeerMessage(companion, result);
    }

    private AppStateSyncKey createAppKey(Jid jid, int index) {
        return new AppStateSyncKeyBuilder()
                .keyId(new AppStateSyncKeyId(Bytes.intToBytes(ThreadLocalRandom.current().nextInt(19000, 20000), 6)))
                .keyData(createAppKeyData(jid, index))
                .build();
    }

    private AppStateSyncKeyData createAppKeyData(Jid jid, int index) {
        return new AppStateSyncKeyDataBuilder()
                .keyData(SignalKeyPair.random().publicKey())
                .fingerprint(createAppKeyFingerprint(jid, index))
                .timestamp(Clock.nowMilliseconds())
                .build();
    }

    private AppStateSyncKeyFingerprint createAppKeyFingerprint(Jid jid, int index) {
        return new AppStateSyncKeyFingerprintBuilder()
                .rawId(ThreadLocalRandom.current().nextInt())
                .currentIndex(index)
                .deviceIndexes(new ArrayList<>(store().linkedDevicesKeys().values()))
                .build();
    }

    private CompletableFuture<Void> sendInitialSecurityMessage(Jid jid) {
        var protocolMessage = new ProtocolMessageBuilder()
                .protocolType(ProtocolMessage.Type.INITIAL_SECURITY_NOTIFICATION_SETTING_SYNC)
                .initialSecurityNotificationSettingSync(new InitialSecurityNotificationSettingSync(true))
                .build();
        return socketHandler.sendPeerMessage(jid, protocolMessage);
    }

    private CompletableFuture<Void> sendHistoryProtocolMessage(Jid jid, HistorySync historySync, HistorySync.Type type) {
        var syncBytes = HistorySyncSpec.encode(historySync);
        return Medias.upload(syncBytes, AttachmentType.HISTORY_SYNC, store().mediaConnection())
                .thenApplyAsync(upload -> createHistoryProtocolMessage(upload, type))
                .thenComposeAsync(result -> socketHandler.sendPeerMessage(jid, result));
    }

    private ProtocolMessage createHistoryProtocolMessage(MediaFile upload, HistorySync.Type type) {
        var notification = new HistorySyncNotificationBuilder()
                .mediaSha256(upload.fileSha256())
                .mediaEncryptedSha256(upload.fileEncSha256())
                .mediaKey(upload.mediaKey())
                .mediaDirectPath(upload.directPath())
                .mediaSize(upload.fileLength())
                .syncType(type)
                .build();
        return new ProtocolMessageBuilder()
                .protocolType(ProtocolMessage.Type.HISTORY_SYNC_NOTIFICATION)
                .historySyncNotification(notification)
                .build();
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
        return result.findNode("verified_name")
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
        Validate.isTrue(store().clientType() == ClientType.MOBILE, "2FA is only available for the mobile api");
        Validate.isTrue(code == null || (code.matches("^[0-9]*$") && code.length() == 6),
                "Invalid 2fa code: expected a numeric six digits string");
        Validate.isTrue(email == null || isValidEmail(email),
                "Invalid email: %s", email);
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
     * @return a future
     */
    public CompletableFuture<Call> startCall(JidProvider contact) {
        Validate.isTrue(store().clientType() == ClientType.MOBILE, "Calling is only available for the mobile api");
        return addTrustedContact(contact, Clock.nowSeconds())
                .thenComposeAsync(ignored -> socketHandler.querySessions(List.of(contact.toJid())))
                .thenComposeAsync(ignored -> sendCallMessage(contact));
    }

    private CompletableFuture<?> addTrustedContact(JidProvider contact, long timestamp) {
        return socketHandler.sendQuery("set", "privacy", Node.of("tokens", Node.of("token", Map.of("jid", contact.toJid(), "t", timestamp, "type", "trusted_contact"))));
    }

    private CompletableFuture<Call> sendCallMessage(JidProvider provider) {
        var callId = ChatMessageKey.randomId();
        var audioStream = Node.of("audio", Map.of("rate", 8000, "enc", "opus"));
        var audioStreamTwo = Node.of("audio", Map.of("rate", 16000, "enc", "opus"));
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
        var call = new Call(provider.toJid(), jidOrThrowError(), callId, Clock.nowSeconds(), false, CallStatus.RINGING, false);
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
        Validate.isTrue(store().clientType() == ClientType.MOBILE, "Calling is only available for the mobile api");
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
        Validate.isTrue(store().clientType() == ClientType.MOBILE, "Calling is only available for the mobile api");
        var callCreator = "%s.%s:%s@s.whatsapp.net".formatted(call.caller().user(), call.caller().device(), call.caller().device());
        if (Objects.equals(call.caller().user(), jidOrThrowError().user())) {
            var rejectNode = Node.of("terminate", Map.of("reason", "timeout", "call-id", call.id(), "call-creator", callCreator));
            var body = Node.of("call", Map.of("to", call.chat()), rejectNode);
            return socketHandler.sendNode(body)
                    .thenApplyAsync(result -> !result.hasNode("error"));
        }

        var rejectNode = Node.of("reject", Map.of("call-id", call.id(), "call-creator", callCreator, "count", 0));
        var body = Node.of("call", Map.of("from", socketHandler.store().jid(), "to", call.caller()), rejectNode);
        return socketHandler.sendNode(body)
                .thenApplyAsync(result -> !result.hasNode("error"));
    }


    /**
     * Queries a list of fifty recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @return a list of recommended newsletters, if the feature is available
     */
    public CompletableFuture<Optional<RecommendedNewslettersResponse>> queryRecommendedNewsletters(String countryCode) {
        return queryRecommendedNewsletters(countryCode, 50);
    }


    /**
     * Queries a list of recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @param limit       how many entries should be returned
     * @return a list of recommended newsletters, if the feature is available
     */
    public CompletableFuture<Optional<RecommendedNewslettersResponse>> queryRecommendedNewsletters(String countryCode, int limit) {
        var filters = new RecommendedNewslettersRequest.Filters(List.of(countryCode));
        var input = new RecommendedNewslettersRequest.Input("RECOMMENDED", filters, limit);
        var variable = new RecommendedNewslettersRequest.Variable(input);
        var query = new RecommendedNewslettersRequest(variable);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6190824427689257"), Json.writeValueAsBytes(query)))
                .thenApplyAsync(this::parseRecommendedNewsletters);
    }

    private Optional<RecommendedNewslettersResponse> parseRecommendedNewsletters(Node response) {
        return response.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(RecommendedNewslettersResponse::ofJson);
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
        var input = new CreateNewsletterRequest.NewsletterInput(name, description, picture != null ? Base64.getEncoder().encodeToString(picture) : null);
        var variable = new CreateNewsletterRequest.Variable(input);
        var request = new CreateNewsletterRequest(variable);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6996806640408138"), Json.writeValueAsBytes(request)))
                .thenApplyAsync(this::parseNewsletterCreation)
                .thenComposeAsync(this::onNewsletterCreation);
    }

    private Optional<Newsletter> parseNewsletterCreation(Node response) {
        return response.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(NewsletterResponse::ofJson)
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
        var safeDescription = Objects.requireNonNullElse(description, "");
        var payload = new UpdatePayload(safeDescription);
        var body = new UpdateNewsletterRequest.Variable(newsletter.toJid(), payload);
        var request = new UpdateNewsletterRequest(body);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7150902998257522"), Json.writeValueAsBytes(request)))
                .thenRun(() -> {});
    }

    /**
     * Joins a newsletter
     *
     * @param newsletter a non-null newsletter
     * @return a future
     */
    public CompletableFuture<Void> joinNewsletter(JidProvider newsletter) {
        var body = new JoinNewsletterRequest.Variable(newsletter.toJid());
        var request = new JoinNewsletterRequest(body);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "9926858900719341"), Json.writeValueAsBytes(request)))
                .thenRun(() -> {});
    }

    /**
     * Leaves a newsletter
     *
     * @param newsletter a non-null newsletter
     * @return a future
     */
    public CompletableFuture<Void> leaveNewsletter(JidProvider newsletter) {
        var body = new LeaveNewsletterRequest.Variable(newsletter.toJid());
        var request = new LeaveNewsletterRequest(body);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6392786840836363"), Json.writeValueAsBytes(request)))
                .thenRun(() -> {});
    }

    /**
     * Queries the number of people subscribed to a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @return a CompletableFuture
     */
    public CompletableFuture<Long> queryNewsletterSubscribers(JidProvider newsletterJid) {
        var newsletterRole = store().findNewsletterByJid(newsletterJid)
                .flatMap(Newsletter::viewerMetadata)
                .map(NewsletterViewerMetadata::role)
                .orElse(NewsletterViewerRole.GUEST);
        var input = new NewsletterSubscribersRequest.Input(newsletterJid.toJid(), "JID", newsletterRole.name());
        var body = new NewsletterSubscribersRequest.Variable(input);
        var request = new NewsletterSubscribersRequest(body);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7272540469429201"), Json.writeValueAsBytes(request)))
                .thenApply(this::parseNewsletterSubscribers);
    }

    private long parseNewsletterSubscribers(Node response) {
        return response.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(NewsletterSubscribersResponse::ofJson)
                .map(NewsletterSubscribersResponse::subscribersCount)
                .orElse(0L);
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
        return getContactData(admin).thenCompose(results -> {
            var recipient = results.getFirst()
                    .findNode("lid")
                    .flatMap(result -> result.attributes().getOptionalJid("val"))
                    .map(jid -> jid.withServer(JidServer.LID).toSimpleJid())
                    .orElse(admin.toJid());
            var request = new CreateAdminInviteNewsletterRequest(new CreateAdminInviteNewsletterRequest.Variable(newsletterJid.toJid(), recipient));
            return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6826078034173770"), Json.writeValueAsBytes(request)))
                    .thenApplyAsync(this::parseNewsletterAdminInviteExpiration)
                    .thenComposeAsync(expirationTimestamp -> sendNewsletterInviteMessage(newsletterJid, inviteCaption, expirationTimestamp, admin));
        });
    }

    private long parseNewsletterAdminInviteExpiration(Node result) {
        var payload = result.findNode("result")
                .flatMap(Node::contentAsString);
        return payload.flatMap(CreateAdminInviteNewsletterResponse::ofJson)
                .map(CreateAdminInviteNewsletterResponse::mute)
                .orElseThrow(() -> new IllegalArgumentException("Cannot create invite: " + payload.orElse("unknown")));
    }

    private CompletableFuture<ChatMessageInfo> sendNewsletterInviteMessage(JidProvider newsletterJid, String inviteCaption, long expirationTimestamp, JidProvider admin) {
        var newsletterName = store().findNewsletterByJid(newsletterJid.toJid())
                .map(Newsletter::metadata)
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
        return getContactData(admin).thenCompose(results -> {
            var recipient = results.getFirst()
                    .findNode("lid")
                    .flatMap(result -> result.attributes().getOptionalJid("val"))
                    .map(jid -> jid.withServer(JidServer.LID).toSimpleJid())
                    .orElse(admin.toJid());
            var request = new RevokeAdminInviteNewsletterRequest(new RevokeAdminInviteNewsletterRequest.Variable(newsletterJid.toJid(), recipient));
            return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6111171595650958"), Json.writeValueAsBytes(request)))
                    .thenApplyAsync(this::hasRevokedNewsletterAdminInvite);
        });
    }

    private boolean hasRevokedNewsletterAdminInvite(Node result) {
        return result.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(RevokeAdminInviteNewsletterResponse::ofJson)
                .isPresent();
    }

    /**
     * Accepts an invitation to become an admin in a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @return a CompletableFuture
     */
    public CompletableFuture<Boolean> acceptNewsletterAdminInvite(JidProvider newsletterJid) {
        var request = new AcceptAdminInviteNewsletterRequest(new AcceptAdminInviteNewsletterRequest.Variable(newsletterJid.toJid()));
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7292354640794756"), Json.writeValueAsBytes(request)))
                .thenApplyAsync(this::hasAcceptedNewsletterAdminInvite)
                .thenComposeAsync(result -> {
                    if(result.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return queryNewsletter(result.get(), NewsletterViewerRole.ADMIN).thenApplyAsync(newsletter -> {
                        if(newsletter.isEmpty()) {
                            return false;
                        }

                        store().addNewsletter(newsletter.get());
                        return true;
                    });
                });
    }

    private Optional<Jid> hasAcceptedNewsletterAdminInvite(Node result) {
        return result.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(AcceptAdminInviteNewsletterResponse::ofJson)
                .map(AcceptAdminInviteNewsletterResponse::jid);
    }

    /**
     * Queries a newsletter
     *
     * @param newsletterJid the non-null jid of the newsletter
     * @param role          the non-null role of the user executing the query
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        var key = new QueryNewsletterRequest.Input(newsletterJid, "JID", role);
        var request = new QueryNewsletterRequest(new QueryNewsletterRequest.Variable(key, true, true, true));
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6620195908089573"), Json.writeValueAsBytes(request)))
                .thenApplyAsync(this::parseNewsletterQuery);
    }

    private Optional<Newsletter> parseNewsletterQuery(Node response) {
        return response.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(NewsletterResponse::ofJson)
                .map(NewsletterResponse::newsletter);
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
     * Registers a chats listener
     *
     * @param onChats the listener to register
     * @return the same instance
     */
    public Whatsapp addChatsListener(OnWhatsappChats onChats) {
        return addListener(onChats);
    }

    /**
     * Registers a newsletters listener
     *
     * @param onNewsletters the listener to register
     * @return the same instance
     */
    public Whatsapp addNewslettersListener(OnNewsletters onNewsletters) {
        return addListener(onNewsletters);
    }

    /**
     * Registers a newsletters listener
     *
     * @param onNewsletters the listener to register
     * @return the same instance
     */
    public Whatsapp addNewslettersListener(OnWhatsappNewsletters onNewsletters) {
        return addListener(onNewsletters);
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
     * @param onAnyMessageStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addMessageStatusListener(OnMessageStatus onAnyMessageStatus) {
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
    public Whatsapp addNewChatMessageListener(OnNewMessage onNewMessage) {
        return addListener(onNewMessage);
    }

    /**
     * Registers a new status listener
     *
     * @param onNewMediaStatus the listener to register
     * @return the same instance
     */
    public Whatsapp addNewStatusListener(OnNewStatus onNewMediaStatus) {
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
    public Whatsapp addMediaStatusListener(OnStatus onMediaStatus) {
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
     * Registers a new message listener
     *
     * @param onNewMessage the listener to register
     * @return the same instance
     */
    public Whatsapp addNewChatMessageListener(OnWhatsappNewMessage onNewMessage) {
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
    public Whatsapp addMessageReplyListener(ChatMessageInfo info, OnMessageReply onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
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
    public Whatsapp addMessageReplyListener(ChatMessageInfo info, OnWhatsappMessageReply onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param id             the non-null id of the target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(String id, OnMessageReply onMessageReply) {
        return addMessageReplyListener((info, quoted) -> {
            if (!info.id().equals(id)) {
                return;
            }

            onMessageReply.onMessageReply(info, quoted);
        });
    }

    /**
     * Registers a message reply listener for a specific message
     *
     * @param id             the non-null id of the target message
     * @param onMessageReply the non-null listener
     */
    public Whatsapp addMessageReplyListener(String id, OnWhatsappMessageReply onMessageReply) {
        return addMessageReplyListener(((whatsapp, info, quoted) -> {
            if (!info.id().equals(id)) {
                return;
            }

            onMessageReply.onMessageReply(whatsapp, info, quoted);
        }));
    }

    /**
     * Registers a name change listener
     *
     * @param onUserNameChanged the non-null listener
     */
    public Whatsapp addNameChangedListener(OnUserNameChanged onUserNameChanged) {
        return addListener(onUserNameChanged);
    }

    /**
     * Registers a name change listener
     *
     * @param onNameChange the non-null listener
     */
    public Whatsapp addNameChangedListener(OnWhatsappNameChanged onNameChange) {
        return addListener(onNameChange);
    }

    /**
     * Registers a status change listener
     *
     * @param onUserAboutChanged the non-null listener
     */
    public Whatsapp addAboutChangedListener(OnUserAboutChanged onUserAboutChanged) {
        return addListener(onUserAboutChanged);
    }

    /**
     * Registers a status change listener
     *
     * @param onUserStatusChange the non-null listener
     */
    public Whatsapp addAboutChangedListener(OnWhatsappAboutChanged onUserStatusChange) {
        return addListener(onUserStatusChange);
    }

    /**
     * Registers a picture change listener
     *
     * @param onProfilePictureChanged the non-null listener
     */
    public Whatsapp addUserPictureChangedListener(OnProfilePictureChanged onProfilePictureChanged) {
        return addListener(onProfilePictureChanged);
    }

    /**
     * Registers a picture change listener
     *
     * @param onUserPictureChange the non-null listener
     */
    public Whatsapp addUserPictureChangedListener(OnWhatsappProfilePictureChanged onUserPictureChange) {
        return addListener(onUserPictureChange);
    }

    /**
     * Registers a profile picture listener
     *
     * @param onContactPictureChanged the non-null listener
     */
    public Whatsapp addContactPictureChangedListener(OnContactPictureChanged onContactPictureChanged) {
        return addListener(onContactPictureChanged);
    }

    /**
     * Registers a profile picture listener
     *
     * @param onProfilePictureChange the non-null listener
     */
    public Whatsapp addContactPictureChangedListener(OnWhatsappContactPictureChanged onProfilePictureChange) {
        return addListener(onProfilePictureChange);
    }

    /**
     * Registers a group picture listener
     *
     * @param onGroupPictureChange the non-null listener
     */
    public Whatsapp addGroupPictureChangedListener(OnGroupPictureChange onGroupPictureChange) {
        return addListener(onGroupPictureChange);
    }

    /**
     * Registers a group picture listener
     *
     * @param onGroupPictureChange the non-null listener
     */
    public Whatsapp addGroupPictureChangedListener(OnWhatsappGroupPictureChange onGroupPictureChange) {
        return addListener(onGroupPictureChange);
    }

    /**
     * Registers a contact blocked listener
     *
     * @param onContactBlocked the non-null listener
     */
    public Whatsapp addContactBlockedListener(OnContactBlocked onContactBlocked) {
        return addListener(onContactBlocked);
    }

    /**
     * Registers a contact blocked listener
     *
     * @param onContactBlocked the non-null listener
     */
    public Whatsapp addContactBlockedListener(OnWhatsappContactBlocked onContactBlocked) {
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
     * Registers a companion devices changed listener
     *
     * @param onLinkedDevices the listener to register
     * @return the same instance
     */
    public Whatsapp addLinkedDevicesListener(OnLinkedDevices onLinkedDevices) {
        return addListener(onLinkedDevices);
    }

    /**
     * Registers a companion devices changed listener
     *
     * @param onWhatsappLinkedDevices the listener to register
     * @return the same instance
     */
    public Whatsapp addLinkedDevicesListener(OnWhatsappLinkedDevices onWhatsappLinkedDevices) {
        return addListener(onWhatsappLinkedDevices);
    }

    /**
     * Registers a registration code listener for the mobile api
     *
     * @param onRegistrationCode the listener to register
     * @return the same instance
     */
    public Whatsapp addRegistrationCodeListener(OnRegistrationCode onRegistrationCode) {
        return addListener(onRegistrationCode);
    }

    /**
     * Registers a registration code listener for the mobile api
     *
     * @param onWhatsappRegistrationCode the listener to register
     * @return the same instance
     */
    public Whatsapp addRegistrationCodeListener(OnWhatsappRegistrationCode onWhatsappRegistrationCode) {
        return addListener(onWhatsappRegistrationCode);
    }

    /**
     * Registers a call listener
     *
     * @param onCall the listener to register
     * @return the same instance
     */
    public Whatsapp addCallListener(OnCall onCall) {
        return addListener(onCall);
    }

    /**
     * Registers a call listener
     *
     * @param onWhatsappCall the listener to register
     * @return the same instance
     */
    public Whatsapp addCallListener(OnWhatsappCall onWhatsappCall) {
        return addListener(onWhatsappCall);
    }

    private Jid jidOrThrowError() {
        return store().jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
    }
}
