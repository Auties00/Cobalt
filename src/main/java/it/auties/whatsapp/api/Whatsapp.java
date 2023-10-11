package it.auties.whatsapp.api;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.AesGcm;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.SessionCipher;
import it.auties.whatsapp.listener.*;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.business.*;
import it.auties.whatsapp.model.call.Call;
import it.auties.whatsapp.model.call.CallStatus;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.companion.CompanionLinkResult;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MediaFile;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.model.reserved.LocalMediaMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessageBuilder;
import it.auties.whatsapp.model.message.standard.CallMessageBuilder;
import it.auties.whatsapp.model.message.standard.ReactionMessageBuilder;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.GdprAccountReport;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.*;
import it.auties.whatsapp.model.request.UpdateNewsletterRequest.UpdatePayload;
import it.auties.whatsapp.model.response.ContactStatusResponse;
import it.auties.whatsapp.model.response.HasWhatsappResponse;
import it.auties.whatsapp.model.response.NewsletterResponse;
import it.auties.whatsapp.model.response.RecommendedNewslettersResponse;
import it.auties.whatsapp.model.setting.LocaleSettings;
import it.auties.whatsapp.model.setting.PushNameSettings;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.PatchRequest.PatchEntry;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.socket.SocketHandler;
import it.auties.whatsapp.socket.SocketState;
import it.auties.whatsapp.util.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
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

    protected static Optional<Whatsapp> getInstanceByUuid(UUID uuid) {
        return Optional.ofNullable(instances.get(uuid));
    }

    protected static void removeInstanceByUuid(UUID uuid) {
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

    protected Whatsapp(Store store, Keys keys, ErrorHandler errorHandler, WebVerificationSupport webVerificationSupport, Executor socketExecutor) {
        this.socketHandler = new SocketHandler(this, store, keys, errorHandler, webVerificationSupport, socketExecutor);
        store.addListener((OnDisconnected) (reason) -> {
            if(reason != DisconnectReason.RECONNECTING) {
                removeInstanceByUuid(store.uuid());
            }
        });
        if(store.autodetectListeners()){
            return;
        }

        store.addListeners(ListenerScanner.scan(this));
    }

    /**
     * Creates a new web api
     * The web api is based around the WhatsappWeb client
     *
     * @return a web api builder
     */
    public static ConnectionBuilder<WebOptionsBuilder> webBuilder(){
        return new ConnectionBuilder<>(ClientType.WEB);
    }

    /**
     * Creates a new mobile api
     * The mobile api is based around the Whatsapp App available on IOS and Android
     *
     * @return a web mobile builder
     */
    public static ConnectionBuilder<MobileOptionsBuilder> mobileBuilder(){
        return new ConnectionBuilder<>(ClientType.MOBILE);
    }

    /**
     * Creates an advanced builder if you need more customization
     *
     * @return a custom builder
     */
    public static WhatsappCustomBuilder customBuilder(){
        return new WhatsappCustomBuilder();
    }


    /**
     * Connects to Whatsapp
     *
     * @return a future
     */
    public CompletableFuture<Whatsapp> connect(){
        return socketHandler.connect()
                .thenRunAsync(() -> instances.put(store().uuid(), this))
                .thenApply(ignored -> this);
    }

    /**
     * Returns whether the connection is active or not
     *
     * @return a boolean
     */
    public boolean isConnected(){
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
    @SafeVarargs
    public final <T extends JidProvider> CompletableFuture<Whatsapp> changePrivacySetting(PrivacySettingType type, PrivacySettingValue value, T ... excluded) {
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
                .thenRun(() -> onPrivacyFeatureChanged(type, value, excludedJids))
                .thenApply(ignored -> this);
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
    public CompletableFuture<Whatsapp> changeNewChatsEphemeralTimer(ChatEphemeralTimer timer) {
        return socketHandler.sendQuery("set", "disappearing_mode", Node.of("disappearing_mode", Map.of("duration", timer.period().toSeconds())))
                .thenRun(() -> store().setNewChatsEphemeralTimer(timer))
                .thenApply(ignored -> this);
    }

    /**
     * Creates a new request to get a document containing all the data that was collected by Whatsapp
     * about this user. It takes three business days to receive it. To query the newsletters status, use
     * {@link Whatsapp#getGdprAccountInfoStatus()}
     *
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> createGdprAccountInfo() {
        return socketHandler.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("gdpr", Map.of("gdpr", "request")))
                .thenApply(ignored -> this);
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
    public CompletableFuture<Whatsapp> changeName(String newName) {
        var oldName = store().name();
        return socketHandler.send(Node.of("presence", Map.of("name", newName)))
                .thenRun(() -> socketHandler.updateUserName(newName, oldName))
                .thenApply(ignored -> this);
    }

    /**
     * Changes the status(i.e. user description) of this user
     *
     * @param newStatus the non-null new status
     * @return the same instance wrapped in a completable future
     */
    public CompletableFuture<Whatsapp> changeStatus(String newStatus) {
        return socketHandler.sendQuery("set", "status", Node.of("status", newStatus.getBytes(StandardCharsets.UTF_8)))
                .thenRun(() -> store().setName(newStatus))
                .thenApply(ignored -> this);
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> subscribeToPresence(T jid) {
        return socketHandler.subscribeToPresence(jid)
                .thenApplyAsync(ignored -> jid);
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
        // TODO: Support newsletters
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
                .chatJid(message.parentJid())
                .senderJid(message.senderJid())
                .fromMe(Objects.equals(message.senderJid().withoutDevice(), jidOrThrowError().withoutDevice()))
                .id(message.id())
                .build();
        var reactionMessage = new ReactionMessageBuilder()
                .key(key)
                .content(reaction)
                .timestampSeconds(Instant.now().toEpochMilli())
                .build();
        return sendMessage(message.parentJid(), reactionMessage);
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
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider chat, Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient    the recipient where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public CompletableFuture<? extends MessageInfo> sendMessage(JidProvider recipient, MessageContainer message) {
        if(recipient.toJid().server() == JidServer.NEWSLETTER) {
            var newsletter = store().findNewsletterByJid(recipient);
            if(newsletter.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot send a message in a newsletter that you didn't join"));
            }

            var info = new NewsletterMessageInfo(
                    newsletter.get(),
                    ChatMessageKey.randomId(),
                    1,
                    Clock.nowSeconds(),
                    null,
                    new ConcurrentHashMap<>(),
                    message,
                    MessageStatus.PENDING
            );
            return sendMessage(info);
        }

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
                .message(message)
                .timestampSeconds(Clock.nowSeconds())
                .broadcast(recipient.toJid().hasServer(JidServer.BROADCAST))
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
    public <T extends JidProvider> CompletableFuture<T> markRead(T chat) {
        return mark(chat, true).thenComposeAsync(ignored -> markAllAsRead(chat)).thenApplyAsync(ignored -> chat);
    }

    private <T extends JidProvider> CompletableFuture<T> mark(T chat, boolean read) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat.toJid())
                    .ifPresent(entry -> entry.setMarkedAsUnread(read));
            return CompletableFuture.completedFuture(chat);
        }

        var range = createRange(chat, false);
        var markAction = new MarkChatAsReadAction(read, Optional.of(range));
        var syncAction = ActionValueSync.of(markAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 3, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    private CompletableFuture<Void> markAllAsRead(JidProvider chat) {
        var all = store()
                .findChatByJid(chat.toJid())
                .stream()
                .map(Chat::unreadMessages)
                .flatMap(Collection::stream)
                .map(this::markRead)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(all);
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
    public CompletableFuture<ChatMessageInfo> markRead(ChatMessageInfo info) {
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
    public CompletableFuture<ChatMessageInfo> awaitReply(ChatMessageInfo info) {
        return awaitReply(info.id());
    }

    /**
     * Awaits for a single newsletters to a message
     *
     * @param id the non-null id of message whose newsletters is pending
     * @return a non-null newsletters
     */
    public CompletableFuture<ChatMessageInfo> awaitReply(String id) {
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
                .toArray(Node[]::new);
        return socketHandler.sendInteractiveQuery(Node.of("contact"), contactNodes)
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
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid"));
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
        var query = new ContactStatusRequest(ChatMessageKey.randomId(), List.of(new ContactStatusRequest.Variable(contactJid.toJid().user(), List.of("STATUS"))));
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Json.writeValueAsBytes(query)))
                .thenApplyAsync(this::parseNameResponse);
    }

    private Optional<String> parseNameResponse(Node result) {
        return result.findNode("result")
                .flatMap(Node::contentAsString)
                .flatMap(ContactStatusResponse::ofJson)
                .flatMap(ContactStatusResponse::status);
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status newsletters
     */
    public CompletableFuture<Optional<ContactStatusResponse>> queryAbout(JidProvider chat) {
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
     * Queries a business profile, if any exists
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
    public <T extends JidProvider> CompletableFuture<T> revokeGroupInvite(T chat) {
        return socketHandler.sendQuery(chat.toJid(), "set", "w:g2", Node.of("invite")).thenApplyAsync(ignored -> chat);
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
                .flatMap(group -> group.attributes().getJid("jid"))
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
        if(status == available){
            return CompletableFuture.completedFuture(status);
        }

        var presence = available ? ContactStatus.AVAILABLE : ContactStatus.UNAVAILABLE;
        var node = Node.of("presence", Map.of("name", store().name(), "type", presence.toString()));
        return socketHandler.sendWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(null, presence))
                .thenApplyAsync(ignored -> available);
    }

    private void updateSelfPresence(JidProvider chatJid, ContactStatus presence) {
        if(chatJid == null){
            store().setOnline(presence == ContactStatus.AVAILABLE);
        }

        var self = store().findContactByJid(jidOrThrowError().withoutDevice());
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
    public <T extends JidProvider> CompletableFuture<T> changePresence(T chatJid, ContactStatus presence) {
        var knownPresence = store().findChatByJid(chatJid)
                .map(Chat::presences)
                .map(entry -> entry.get(jidOrThrowError().withoutDevice()))
                .orElse(null);
        if(knownPresence == COMPOSING || knownPresence == RECORDING){
            var node = Node.of("chatstate", Map.of("to", chatJid.toJid()), Node.of("paused"));
            return socketHandler.sendWithNoResponse(node)
                    .thenApplyAsync(ignored -> chatJid);
        }

        if(presence == COMPOSING || presence == RECORDING){
            var tag = presence == RECORDING ? COMPOSING : presence;
            var node = Node.of("chatstate",
                    Map.of("to", chatJid.toJid()),
                    Node.of(COMPOSING.toString(), presence == RECORDING ? Map.of("media", "audio") : Map.of()));
            return socketHandler.sendWithNoResponse(node)
                    .thenAcceptAsync(socketHandler -> updateSelfPresence(chatJid, presence))
                    .thenApplyAsync(ignored -> chatJid);
        }

        var node = Node.of("presence", Map.of("type", presence.toString(), "name", store().name()));
        return socketHandler.sendWithNoResponse(node)
                .thenAcceptAsync(socketHandler -> updateSelfPresence(chatJid, presence))
                .thenApplyAsync(ignored -> chatJid);
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> promote(JidProvider group, JidProvider ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.PROMOTE, contacts);
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param group    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> demote(JidProvider group, JidProvider ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.DEMOTE, contacts);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> addGroupParticipant(JidProvider group, JidProvider ... contacts) {
        return executeActionOnGroupParticipant(group, GroupAction.ADD, contacts);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param group    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public CompletableFuture<List<Jid>> removeGroupParticipant(JidProvider group, JidProvider ... contacts) {
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
        Validate.isTrue(!Objects.equals(jid.withoutDevice(), jidOrThrowError().withoutDevice()), errorMessage);
        return jid;
    }

    private List<Jid> parseGroupActionResponse(Node result, JidProvider groupJid, GroupAction action) {
        var results = result.findNode(action.data())
                .orElseThrow(() -> new NoSuchElementException("An erroneous group operation was executed"))
                .findNodes("participant")
                .stream()
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getJid("jid"))
                .flatMap(Optional::stream)
                .toList();
        var chat = groupJid instanceof Chat entry ? entry : store()
                .findChatByJid(groupJid)
                .orElse(null);
        if (chat != null) {
            results.forEach(entry -> handleGroupAction(action, chat, entry));
        }

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
    public <T extends JidProvider> CompletableFuture<T> changeGroupSubject(T group, String newName) {
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
    public <T extends JidProvider> CompletableFuture<T> changeGroupDescription(T group, String description) {
        return socketHandler.queryGroupMetadata(group.toJid())
                .thenApplyAsync(GroupMetadata::descriptionId)
                .thenComposeAsync(descriptionId -> changeGroupDescription(group, description, descriptionId.orElse(null)))
                .thenApplyAsync(ignored -> group);
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
    public <T extends JidProvider> CompletableFuture<T> changeGroupSetting(T group, GroupSetting setting, GroupSettingPolicy policy) {
        var body = Node.of(policy != GroupSettingPolicy.ANYONE ? setting.on() : setting.off());
        return socketHandler.sendQuery(group.toJid(), "set", "w:g2", body)
                .thenApplyAsync(ignored -> group);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public CompletableFuture<Jid> changeProfilePicture(byte[] image) {
        return changeGroupPicture(jidOrThrowError(), image);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> changeGroupPicture(T group, URI image) {
        return changeGroupPicture(group, image == null ? null : Medias.download(image)
                .orElseThrow(() -> new IllegalArgumentException("Invalid uri: %s".formatted(image))));
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> changeGroupPicture(T group, byte[] image) {
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), profilePic);
        return socketHandler.sendQuery(group.toJid().withoutDevice(), "set", "w:profile:picture", body)
                .thenApplyAsync(ignored -> group);
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
     * @param subject     the new group's name
     * @param timer       the default ephemeral timer for messages sent in this group
     * @param parentCommunity the community to whom the new group will be linked
     * @param contacts    at least one contact to add to the group, not enforced if part of a community
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<GroupMetadata>> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity, JidProvider... contacts) {
        Validate.isTrue(!subject.isBlank(), "The subject of a group cannot be blank");
        var minimumMembersCount = parentCommunity == null ? 1 : 0;
        Validate.isTrue(contacts.length >= minimumMembersCount, "Expected at least %s members for this group", minimumMembersCount);
        var children = new ArrayList<Node>();
        if (parentCommunity != null) {
            children.add(Node.of("linked_parent", Map.of("jid", parentCommunity.toJid())));
        }
        if (timer != ChatEphemeralTimer.OFF) {
            children.add(Node.of("ephemeral", Map.of("expiration", timer.periodSeconds())));
        }
        Arrays.stream(contacts)
                .map(contact -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(contact.toJid(), "Cannot create group with yourself as a participant"))))
                .forEach(children::add);
        var key = HexFormat.of().formatHex(BytesHelper.random(12));
        var body = Node.of("create", Map.of("subject", subject, "key", key), children);
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", body)
                .thenApplyAsync(this::parseGroupResponse);
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
    public <T extends JidProvider> CompletableFuture<T> leaveGroup(T group) {
        var body = Node.of("leave", Node.of("group", Map.of("id", group.toJid())));
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", body)
                .thenApplyAsync(ignored -> handleLeaveGroup(group));
    }

    private <T extends JidProvider> T handleLeaveGroup(T group) {
        var chat = group instanceof Chat entry ? entry : store()
                .findChatByJid(group)
                .orElse(null);
        if(chat != null) {
            var pastParticipant = new GroupPastParticipantBuilder()
                    .jid(jidOrThrowError().withoutDevice())
                    .reason(GroupPastParticipant.Reason.REMOVED)
                    .timestampSeconds(Clock.nowSeconds())
                    .build();
            chat.addPastParticipant(pastParticipant);
        }

        return group;
    }

    /**
     * Links any number of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public CompletableFuture<Map<Jid, Boolean>> linkGroupsToCommunity(JidProvider community, JidProvider... groups){
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
                .map(entry -> entry.attributes().getJid("jid"))
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
     * @param group the non-null group to unlink
     * @return a CompletableFuture that indicates whether the request was successful
     */
    public CompletableFuture<Boolean> unlinkGroupFromCommunity(JidProvider community, JidProvider group){
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
    public <T extends JidProvider> CompletableFuture<T> mute(T chat) {
        return mute(chat, ChatMute.muted());
    }

    /**
     * Mutes a chat
     *
     * @param chat the target chat
     * @param mute the type of mute
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> mute(T chat, ChatMute mute) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(mute));
            return CompletableFuture.completedFuture(chat);
        }

        var endTimeStamp = mute.type() == ChatMute.Type.MUTED_FOR_TIMEFRAME ? mute.endTimeStamp() * 1000L : mute.endTimeStamp();
        var muteAction = new MuteAction(true, OptionalLong.of(endTimeStamp), false);
        var syncAction = ActionValueSync.of(muteAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 2, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> unmute(T chat) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(ChatMute.notMuted()));
            return CompletableFuture.completedFuture(chat);
        }

        var muteAction = new MuteAction(false, null, false);
        var syncAction = ActionValueSync.of(muteAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 2, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Blocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> block(T chat) {
        var body = Node.of("item", Map.of("action", "block", "jid", chat.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body).thenApplyAsync(ignored -> chat);
    }

    /**
     * Unblocks a contact
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> unblock(T chat) {
        var body = Node.of("item", Map.of("action", "unblock", "jid", chat.toJid()));
        return socketHandler.sendQuery("set", "blocklist", body).thenApplyAsync(ignored -> chat);
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled
     * in said chat after a week
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> changeEphemeralTimer(T chat, ChatEphemeralTimer timer) {
        return switch (chat.toJid().server()) {
            case USER, WHATSAPP -> {
                var message = new ProtocolMessageBuilder()
                        .protocolType(ProtocolMessage.Type.EPHEMERAL_SETTING)
                        .ephemeralExpiration(timer.period().toSeconds())
                        .build();
                yield sendMessage(chat, message).thenApplyAsync(ignored -> chat);
            }
            case GROUP -> {
                var body = timer == ChatEphemeralTimer.OFF ? Node.of("not_ephemeral") : Node.of("ephemeral", Map.of("expiration", timer.period()
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
    public CompletableFuture<ChatMessageInfo> markPlayed(ChatMessageInfo info) {
        if (store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS).value() != PrivacySettingValue.EVERYONE) {
            return CompletableFuture.completedFuture(info);
        }
        socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), "played");
        info.setStatus(MessageStatus.PLAYED);
        return CompletableFuture.completedFuture(info);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> markUnread(T chat) {
        return mark(chat, false);
    }

    /**
     * Pins a chat to the top. A maximum of three chats can be pinned to the top. This condition can
     * be checked using;.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> pin(T chat) {
        return pin(chat, true);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> unpin(T chat) {
        return pin(chat, false);
    }

    private <T extends JidProvider> CompletableFuture<T> pin(T chat, boolean pin) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setPinnedTimestampSeconds(pin ? (int) Clock.nowSeconds() : 0));
            return CompletableFuture.completedFuture(chat);
        }

        var pinAction = new PinAction(pin);
        var syncAction = ActionValueSync.of(pinAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 5, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Stars a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> star(ChatMessageInfo info) {
        return star(info, true);
    }

    private CompletableFuture<ChatMessageInfo> star(ChatMessageInfo info, boolean star) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            info.setStarred(star);
            return CompletableFuture.completedFuture(info);
        }

        var starAction = new StarAction(star);
        var syncAction = ActionValueSync.of(starAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> info);
    }

    private String fromMeToFlag(ChatMessageInfo info) {
        return booleanToInt(info.fromMe());
    }

    private String participantToFlag(ChatMessageInfo info) {
        return info.chatJid().hasServer(JidServer.GROUP) && !info.fromMe() ? info.senderJid().toString() : "0";
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
    public CompletableFuture<ChatMessageInfo> unstar(ChatMessageInfo info) {
        return star(info, false);
    }

    /**
     * Archives a chat. If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> archive(T chat) {
        return archive(chat, true);
    }

    private <T extends JidProvider> CompletableFuture<T> archive(T chat, boolean archive) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setArchived(archive));
            return CompletableFuture.completedFuture(chat);
        }

        var range = createRange(chat, false);
        var archiveAction = new ArchiveChatAction(archive, Optional.of(range));
        var syncAction = ActionValueSync.of(archiveAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 3, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        return socketHandler.pushPatch(request).thenApplyAsync(ignored -> chat);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     * @return a CompletableFuture
     */
    public <T extends JidProvider> CompletableFuture<T> unarchive(T chat) {
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
    public CompletableFuture<ChatMessageInfo> delete(ChatMessageInfo info, boolean everyone) {
        if (everyone) {
            var message = new ProtocolMessageBuilder()
                    .protocolType(ProtocolMessage.Type.REVOKE)
                    .key(info.key())
                    .build();
            var sender = info.chatJid().hasServer(JidServer.GROUP) ? jidOrThrowError() : null;
            var key = new ChatMessageKeyBuilder()
                    .id(ChatMessageKey.randomId())
                    .chatJid(info.chatJid())
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
            var attrs = Map.of("edit", getEditBit(info));
            var request = new MessageSendRequest.Chat(revokeInfo, null, false, false, attrs);
            return socketHandler.sendMessage(request)
                    .thenApplyAsync(ignored -> info);
        }

        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            info.chat().ifPresent(chat -> chat.removeMessage(info));
            return CompletableFuture.completedFuture(info);
        }

        var range = createRange(info.chatJid(), false);
        var deleteMessageAction = new DeleteMessageForMeAction(false, info.timestampSeconds());
        var syncAction = ActionValueSync.of(deleteMessageAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 3, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        return socketHandler.pushPatch(request)
                .thenApplyAsync(ignored -> info);
    }

    private int getEditBit(ChatMessageInfo info) {
        if(info.chatJid().hasServer(JidServer.NEWSLETTER)) {
            return 8;
        }

        if(info.chatJid().hasServer(JidServer.GROUP) && !info.fromMe()) {
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
    public <T extends JidProvider> CompletableFuture<T> delete(T chat) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().removeChat(chat.toJid());
            return CompletableFuture.completedFuture(chat);
        }

        var range = createRange(chat.toJid(), false);
        var deleteChatAction = new DeleteChatAction(Optional.of(range));
        var syncAction = ActionValueSync.of(deleteChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 6, chat.toJid().toString(), "1");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
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
    public <T extends JidProvider> CompletableFuture<T> clear(T chat, boolean keepStarredMessages) {
        if(store().clientType() == ClientType.MOBILE){
            // TODO: Send notification to companions
            store().findChatByJid(chat.toJid())
                    .ifPresent(Chat::removeMessages);
            return CompletableFuture.completedFuture(chat);
        }

        var known = store().findChatByJid(chat);
        var range = createRange(chat.toJid(), true);
        var clearChatAction = new ClearChatAction(Optional.of(range));
        var syncAction = ActionValueSync.of(clearChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, 6, chat.toJid().toString(), booleanToInt(keepStarredMessages), "0");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
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
        return queryBusinessCatalog(jidOrThrowError().withoutDevice(), productsLimit);
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
        return queryBusinessCollections(jidOrThrowError().withoutDevice(), collectionsLimit);
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
     * Downloads a media from Whatsapp's servers. If the media is available, it will be returned
     * asynchronously. Otherwise, a retry request will be issued. If that also fails, an exception
     * will be thrown
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<byte[]> downloadMedia(ChatMessageInfo info) {
        if(!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        if(!(mediaMessage instanceof LocalMediaMessage<?> uploadedMediaMessage)) {
            throw new IllegalArgumentException("This message wasn't uploaded yet");
        }

        return uploadedMediaMessage.decodedMedia()
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> requireMediaReupload(info)
                        .thenApplyAsync(ignored -> uploadedMediaMessage.decodedMedia()
                                .orElseThrow(() -> new RuntimeException("Media reupload failed"))));
    }

    /**
     * Asks Whatsapp for a media reupload for a specific media
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public CompletableFuture<ChatMessageInfo> requireMediaReupload(ChatMessageInfo info) {
        if(!(info.message().content() instanceof MediaMessage<?> mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        var mediaKey = mediaMessage.mediaKey()
                .orElseThrow(() -> new NoSuchElementException("Missing media key"));
        var retryKey = Hkdf.extractAndExpand(mediaKey, "WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
        var retryIv = BytesHelper.random(12);
        var retryIdData = info.key().id().getBytes(StandardCharsets.UTF_8);
        var receipt = ServerErrorReceiptSpec.encode(new ServerErrorReceipt(info.id()));
        var ciphertext = AesGcm.encrypt(retryIv, receipt, retryKey, retryIdData);
        var rmrAttributes = Attributes.of()
                .put("jid", info.chatJid())
                .put("from_me", String.valueOf(info.fromMe()))
                .put("participant", info.senderJid(), () -> !Objects.equals(info.chatJid(), info.senderJid()))
                .toMap();
        var node = Node.of("receipt", Map.of("id", info.key().id(), "to", jidOrThrowError()
                .withoutDevice(), "type", "server-error"), Node.of("encrypt", Node.of("enc_p", ciphertext), Node.of("enc_iv", retryIv)), Node.of("rmr", rmrAttributes));
        return socketHandler.send(node, result -> result.hasDescription("notification"))
                .thenApplyAsync(result -> parseMediaReupload(info, mediaMessage, retryKey, retryIdData, result));
    }

    private ChatMessageInfo parseMediaReupload(ChatMessageInfo info, MediaMessage<?> mediaMessage, byte[] retryKey, byte[] retryIdData, Node node) {
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
        return info;
    }

    /**
     * Sends a custom node to Whatsapp
     *
     * @param node the non-null node to send
     * @return the newsletters from Whatsapp
     */
    public CompletableFuture<Node> sendNode(Node node) {
        return socketHandler.send(node);
    }

    /**
     * Creates a new community
     *
     * @param subject the non-null name of the new community
     * @param body    the nullable description of the new community
     * @return a CompletableFuture
     */
    public CompletableFuture<Optional<GroupMetadata>> createCommunity(String subject, String body) {
        var descriptionId = HexFormat.of().formatHex(BytesHelper.random(12));
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
     * @param setting the non-null setting
     * @param policy the non-null policy
     * @return a future
     */
    public <T extends JidProvider> CompletableFuture<T> changeCommunitySetting(T community, CommunitySetting setting, GroupSettingPolicy policy) {
        var tag = policy == GroupSettingPolicy.ANYONE ? setting.on() : setting.off();
        return socketHandler.sendQuery(JidServer.GROUP.toJid(), "set", "w:g2", Node.of(tag))
                .thenApplyAsync(ignored -> community);
    }


    /**
     * Unlinks all the companions of this device
     *
     * @return a future
     */
    public CompletableFuture<Whatsapp> unlinkDevices(){
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
    public CompletableFuture<Whatsapp> unlinkDevice(Jid companion){
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
    public CompletableFuture<CompanionLinkResult> linkDevice(byte[] qrCode){
        try {
            var inputStream = new ByteArrayInputStream(qrCode);
            var luminanceSource = new BufferedImageLuminanceSource(ImageIO.read(inputStream));
            var hybridBinarizer = new HybridBinarizer(luminanceSource);
            var binaryBitmap = new BinaryBitmap(hybridBinarizer);
            var reader = new QRCodeReader();
            var result = reader.decode(binaryBitmap);
            return linkDevice(result.getText());
        }catch (IOException | NotFoundException | ChecksumException | FormatException exception){
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
                .rawId(KeyHelper.agent())
                .keyIndex(store().linkedDevices().size() + 1)
                .timestamp(Clock.nowSeconds())
                .build();
        var deviceIdentityBytes = DeviceIdentitySpec.encode(deviceIdentity);
        var accountSignatureMessage = BytesHelper.concat(
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
        var deviceSignatureMessage = BytesHelper.concat(Specification.Whatsapp.DEVICE_MOBILE_SIGNATURE_HEADER, keyIndexListBytes);
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
        if(maxDevices == null){
            return Specification.Whatsapp.MAX_COMPANIONS;
        }

        try {
            return Integer.parseInt(maxDevices);
        }catch (NumberFormatException exception){
            return Specification.Whatsapp.MAX_COMPANIONS;
        }
    }

    private CompletableFuture<CompanionLinkResult> handleCompanionPairing(Node result, int keyId) {
        if(result.attributes().hasValue("type", "error")){
            var error = result.findNode("error")
                    .filter(entry -> entry.attributes().hasValue("text", "resource-limit"))
                    .map(entry -> CompanionLinkResult.MAX_DEVICES_ERROR)
                    .orElse(CompanionLinkResult.RETRY_ERROR);
            return CompletableFuture.completedFuture(error);
        }

        var device = result.findNode("device")
                .flatMap(entry -> entry.attributes().getJid("jid"))
                .orElse(null);
        if(device == null){
            return CompletableFuture.completedFuture(CompanionLinkResult.RETRY_ERROR);
        }

        return awaitCompanionRegistration(device)
                .thenComposeAsync(ignored -> socketHandler.sendQuery("get", "encrypt", Node.of("key", Node.of("user", Map.of("jid", device)))))
                .thenComposeAsync(encryptResult -> handleCompanionEncrypt(encryptResult, device, keyId));
    }

    private CompletableFuture<Void> awaitCompanionRegistration(Jid device) {
        var future = new CompletableFuture<Void>();
        OnLinkedDevices listener = data -> {
            if(data.contains(device)) {
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
        return socketHandler.pushPatches(companion, List.of(criticalUnblockLowRequest, criticalBlockRequest)).thenComposeAsync(ignored -> {
            var regularLowRequests = createRegularLowRequests();
            var regularRequests = createRegularRequests();
            return socketHandler.pushPatches(companion, List.of(regularLowRequests, regularRequests));
        });
    }

    private PatchRequest createRegularRequests(){
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
        return PatchEntry.of(ActionValueSync.of(pushNameSetting), Operation.SET, 1);
    }

    private PatchEntry createLocaleEntry() {
        var localeSetting = new LocaleSettings(store().locale().orElse("en-US"));
        return PatchEntry.of(ActionValueSync.of(localeSetting), Operation.SET, 3);
    }

    private PatchEntry createAndroidEntry() {
        var device = store().device();
        if(device.isEmpty()) {
            return null;
        }

        var osType = device.get().platform();
        if (osType != UserAgent.PlatformType.ANDROID && osType != UserAgent.PlatformType.SMB_ANDROID) {
            return null;
        }

        var action = new AndroidUnsupportedActions(true);
        return PatchEntry.of(ActionValueSync.of(action), Operation.SET);
    }

    private PatchEntry createNuxRequest() {
        var nuxAction = new NuxAction(true);
        var timeFormatSync = ActionValueSync.of(nuxAction);
        return PatchEntry.of(timeFormatSync, Operation.SET, 7, "keep@s.whatsapp.net");
    }

    private PatchEntry createPrimaryVersionEntry(PrimaryVersionAction primaryVersion, String to) {
        var timeFormatSync = ActionValueSync.of(primaryVersion);
        return PatchEntry.of(timeFormatSync, Operation.SET, 7, to);
    }

    private PatchEntry createTimeFormatEntry() {
        var timeFormatAction = new TimeFormatAction(store().twentyFourHourFormat());
        var timeFormatSync = ActionValueSync.of(timeFormatAction);
        return PatchEntry.of(timeFormatSync, Operation.SET);
    }

    private PatchEntry createContactRequestEntry(Contact contact) {
        var action = new ContactAction(null, contact.shortName(), contact.fullName());
        var sync = ActionValueSync.of(action);
        return PatchEntry.of(sync, Operation.SET, 2, contact.jid().toString());
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
                .keyId(new AppStateSyncKeyId(KeyHelper.appKeyId()))
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
                .rawId(KeyHelper.senderKeyId())
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
        if(code != null && email != null){
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
        return socketHandler.querySessions(contact.toJid())
                .thenComposeAsync(ignored -> sendCallMessage(contact));
    }

    private CompletableFuture<Call> sendCallMessage(JidProvider provider) {
        var callId = ChatMessageKey.randomId();
        var audioStream = Node.of("audio", Map.of("rate", 8000, "enc", "opus"));
        var audioStreamTwo = Node.of("audio", Map.of("rate", 16000, "enc", "opus"));
        var net = Node.of("net", Map.of("medium", 3));
        var encopt = Node.of("encopt", Map.of("keygen", 2));
        var enc = createCallNode(provider);
        var capability = Node.of("capability", Map.of("ver", 1), URLDecoder.decode("%01%04%f7%09%c4:", StandardCharsets.UTF_8));
        var callCreator = "%s.%s:%s@s.whatsapp.net".formatted(jidOrThrowError().user(), jidOrThrowError().device(), jidOrThrowError().device());
        var offer = Node.of("offer",
                Map.of("call-creator", callCreator, "call-id", callId, "device_class", 2016),
                audioStream, audioStreamTwo, net, enc, capability, encopt);
        return socketHandler.send(Node.of("call", Map.of("to", provider.toJid()), offer))
                .thenApply(result -> onCallSent(provider, callId, result));
    }

    private Call onCallSent(JidProvider provider, String callId, Node result) {
        var call = new Call(provider.toJid(), jidOrThrowError(), callId, ZonedDateTime.now(), false, CallStatus.RINGING, false);
        store().addCall(call);
        socketHandler.onCall(call);
        return call;
    }

    private Node createCallNode(JidProvider provider) {
        var call = new CallMessageBuilder()
                .key(SignalKeyPair.random().publicKey())
                .build();
        var message = MessageContainer.of(call);
        socketHandler.querySessions(provider.toJid());
        var cipher = new SessionCipher(provider.toJid().toSignalAddress(), keys());
        var encodedMessage = BytesHelper.messageToBytes(message);
        var cipheredMessage = cipher.encrypt(encodedMessage);
        return Node.of("enc",
                Map.of("v", 2, "type", cipheredMessage.type(), "count", 0), cipheredMessage.message());
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
        if(Objects.equals(call.caller().user(), jidOrThrowError().user())) {
            var rejectNode = Node.of("terminate", Map.of("reason", "timeout", "call-id", call.id(), "call-creator", callCreator));
            var body = Node.of("call", Map.of("to", call.chat()), rejectNode);
            return socketHandler.send(body)
                    .thenApplyAsync(result -> !result.hasNode("error"));
        }

        var rejectNode = Node.of("reject", Map.of("call-id", call.id(), "call-creator", callCreator, "count", 0));
        var body = Node.of("call", Map.of("from", socketHandler.store().jid(), "to", call.caller()), rejectNode);
        return socketHandler.send(body)
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
     * @param limit how many entries should be returned
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
     * @param count how many messages should be queried
     * @return a future
     */
    public <T extends JidProvider> CompletableFuture<T> queryNewsletterMessages(T newsletterJid, int count) {
        var newsletterHandle = store().findNewsletterByJid(newsletterJid)
                .orElseThrow(() -> new NoSuchElementException("Missing newsletter"))
                .metadata()
                .handle();
        return socketHandler.sendQuery("get", "newsletter", Node.of("messages", Map.of("count", count, "type", "invite", "key", newsletterHandle)))
                .thenApplyAsync(result -> newsletterJid);
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
     * Web/Mobile Business API only
     *
     * @param name the non-null name of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name) {
        return createNewsletter(name, null, null);
    }

    /**
     * Creates newsletter channel
     * Web/Mobile Business API only
     *
     * @param name the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name, String description) {
        return createNewsletter(name, description, null);
    }

    /**
     * Creates a newsletter
     * Web/Mobile Business API only
     *
     * @param name the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     * @param picture the nullable profile picture of the newsletter
     * @return a future
     */
    public CompletableFuture<Optional<Newsletter>> createNewsletter(String name, String description, byte[] picture) {
        Validate.isTrue(store().business(), "Only business users can create channels");
        var input = new CreateNewsletterRequest.NewsletterInput(name, description, picture != null ? Base64.getEncoder().encodeToString(picture) : null);
        var variable = new CreateNewsletterRequest.Variable(input);
        var request = new CreateNewsletterRequest(variable);
        return socketHandler.sendQuery("set", "tos", Node.of("notice", Map.of("stage", 5, "id", ChatMessageKey.randomId())))
                .thenComposeAsync(ignored -> socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6234210096708695"), Json.writeValueAsBytes(request))))
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
     * @param newsletter the non-null target newsletter
     * @param description the nullable new description
     * @return a future
     */
    public <T extends JidProvider> CompletableFuture<T> changeNewsletterDescription(T newsletter, String description) {
        var safeDescription = Objects.requireNonNullElse(description, "");
        var payload = new UpdatePayload(safeDescription);
        var body = new UpdateNewsletterRequest.Variable(newsletter.toJid(), payload);
        var request = new UpdateNewsletterRequest(body);
        return socketHandler.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "future"), Json.writeValueAsBytes(request)))
                .thenApply(ignored -> newsletter);
    }

    public CompletableFuture<Void> joinNewsletter(JidProvider newsletter) {
        return CompletableFuture.completedFuture(null);
    }


    public CompletableFuture<Void> leaveNewsletter(JidProvider newsletter) {
        return CompletableFuture.completedFuture(null);
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
    public Whatsapp addLinkedDevicesListener(OnWhatsappRegistrationCode onWhatsappRegistrationCode) {
        return addListener(onWhatsappRegistrationCode);
    }

    /**
     * Registers a call listener
     *
     * @param onCall the listener to register
     * @return the same instance
     */
    public Whatsapp addRegistrationCodeListener(OnCall onCall) {
        return addListener(onCall);
    }

    /**
     * Registers a call listener
     *
     * @param onWhatsappCall the listener to register
     * @return the same instance
     */
    public Whatsapp addLinkedDevicesListener(OnWhatsappCall onWhatsappCall) {
        return addListener(onWhatsappCall);
    }

    private Jid jidOrThrowError() {
        return store().jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
    }
}
