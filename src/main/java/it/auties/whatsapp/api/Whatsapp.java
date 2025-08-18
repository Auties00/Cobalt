package it.auties.whatsapp.api;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.crypto.Hkdf;
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
import it.auties.whatsapp.model.message.standard.NewsletterAdminInviteMessageBuilder;
import it.auties.whatsapp.model.message.standard.ReactionMessageBuilder;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.mobile.AccountInfo;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.newsletter.*;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.privacy.*;
import it.auties.whatsapp.model.request.CommunityRequests;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.model.request.NewsletterRequests;
import it.auties.whatsapp.model.request.UserRequests;
import it.auties.whatsapp.model.response.*;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.PatchRequest.PatchEntry;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.auties.whatsapp.model.contact.ContactStatus.*;

/**
 * A class used to interface a user to Whatsapp's WebSocket
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Whatsapp {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(\\S+)$");

    private final SocketConnection socketConnection;
    protected Whatsapp(Store store, Keys keys, WhatsappErrorHandler errorHandler, WhatsappVerificationHandler.Web webVerificationHandler) {
        this.socketConnection = new SocketConnection(this, store, keys, errorHandler, webVerificationHandler);
    }

    /**
     * Creates a new builder
     *
     * @return a builder
     */
    public static WhatsappBuilder builder() {
        return WhatsappBuilder.INSTANCE;
    }

    //<editor-fold desc="Data">
    /**
     * Returns the keys associated with this session
     *
     * @return a non-null WhatsappKeys
     */
    public Keys keys() {
        return socketConnection.keys();
    }

    /**
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public Store store() {
        return socketConnection.store();
    }
    //</editor-fold>

    //<editor-fold desc="Connection">
    /**
     * Connects to Whatsapp
     */
    public Whatsapp connect() {
        socketConnection.connect(null);
        return this;
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     */
    public void disconnect() {
        socketConnection.disconnect(WhatsappDisconnectReason.DISCONNECTED);
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     */
    public void reconnect() {
        socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous
     * saved credentials. The next time the API is used, the QR code will need to be scanned again.
     *
     */
    public void logout() {
        if (jidOrThrowError() == null) {
            socketConnection.disconnect(WhatsappDisconnectReason.LOGGED_OUT);
            return;
        }

        var metadata = Map.of("jid", jidOrThrowError(), "reason", "user_initiated");
        var device = Node.of("remove-companion-device", metadata);
        socketConnection.sendQuery("set", "md", device);
    }

    /**
     * Returns whether the connection is active or not
     *
     * @return a boolean
     */
    public boolean isConnected() {
        return socketConnection.isConnected();
    }

    /**
     * Waits for this session to be disconnected
     */
    public Whatsapp waitForDisconnection() {
        if(!isConnected()) {
            return this;
        }

        var future = new CompletableFuture<Void>();
        addDisconnectedListener((reason) -> {
            if(reason != WhatsappDisconnectReason.RECONNECTING) {
                future.complete(null);
            }
        });
        future.join();
        return this;
    }
    
    private Jid jidOrThrowError() {
        return store().jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
    }
    //</editor-fold>

    //<editor-fold desc="Account">
    /**
     * Queries this account's info
     *
     * @return a CompletableFuture
     */
    public AccountInfo queryAccountInfo() {
        var result = socketConnection.sendQuery("get", "urn:xmpp:whatsapp:account", Node.of("account"));
        var accoutNode = result.findChild("account")
                .orElseThrow(() -> new NoSuchElementException("Missing account node: " + result));
        var lastRegistration = Clock.parseSeconds(accoutNode.attributes().getLong("last_reg"))
                .orElseThrow(() -> new NoSuchElementException("Missing account last_reg: " + accoutNode));
        var creation = Clock.parseSeconds(accoutNode.attributes().getLong("creation"))
                .orElseThrow(() -> new NoSuchElementException("Missing account creation: " + accoutNode));
        return new AccountInfo(lastRegistration, creation);
    }

    /**
     * Queries a business profile, if available
     *
     * @param contact the target contact
     * @return a CompletableFuture
     */
    public Optional<BusinessProfile> queryBusinessProfile(JidProvider contact) {
        var result = socketConnection.sendQuery("get", "w:biz", Node.of("business_profile", Map.of("v", 116),
                Node.of("profile", Map.of("jid", contact.toJid()))));
        return result.findChild("business_profile")
                .flatMap(entry -> entry.findChild("profile"))
                .map(BusinessProfile::of);
    }
    
    /**
     * Executes a query to determine whether a user has an account on Whatsapp
     *
     * @param contact the contact to check
     * @return a CompletableFuture that wraps a non-null newsletters
     */
    public boolean hasWhatsapp(JidProvider contact) {
        return hasWhatsapp(new JidProvider[]{contact})
                .contains(contact.toJid());
    }

    /**
     * Executes a query to determine whether any number of users have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null map
     */
    public Set<Jid> hasWhatsapp(JidProvider... contacts) {
        if(contacts == null) {
            return Set.of();
        }
        
        var contactNodes = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(jid -> jid.hasServer(JidServer.user()))
                .map(Jid::toPhoneNumber)
                .flatMap(Optional::stream)
                .map(phoneNumber -> Node.of("user", Node.of("contact", phoneNumber)))
                .toList();
        if(contactNodes.isEmpty()) {
            return Set.of();
        }
        
        return socketConnection.sendInteractiveQuery(List.of(Node.of("contact")), contactNodes, List.of())
                .stream()
                .map(HasWhatsappResponse::ofNode)
                .filter(HasWhatsappResponse::hasWhatsapp)
                .map(HasWhatsappResponse::contact)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture
     */
    public Collection<Jid> queryBlockList() {
        return socketConnection.queryBlockList();
    }

    /**
     * Queries the display name of a contact
     *
     * @param contactJid the non-null contact
     * @return a CompletableFuture
     */
    public Optional<String> queryName(JidProvider contactJid) {
        return store().findContactByJid(contactJid)
                .flatMap(Contact::chosenName)
                .or(() -> {
                    var request = UserRequests.chosenName(contactJid.toJid().user());
                    return socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6556393721124826"), request))
                            .findChild("result")
                            .flatMap(Node::contentAsBytes)
                            .flatMap(bytes -> UserChosenNameResponse.ofJson(bytes)
                                    .flatMap(UserChosenNameResponse::name));
                });
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status newsletters
     */
    public Optional<UserAboutResponse> queryAbout(JidProvider chat) {
        return socketConnection.queryAbout(chat);
    }

    /**
     * Queries the profile picture
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public Optional<URI> queryPicture(JidProvider chat) {
        return socketConnection.queryPicture(chat);
    }

    /**
     * Changes a privacy setting in Whatsapp's settings. If the value is
     * {@link PrivacySettingValue#CONTACTS_EXCEPT}, the excluded parameter should also be filled or an
     * exception will be thrown, otherwise it will be ignored.
     *
     * @param type     the non-null setting to change
     * @param value    the non-null value to attribute to the setting
     * @param excluded the non-null excluded contacts if value is {@link PrivacySettingValue#CONTACTS_EXCEPT}
     */
    public final void changePrivacySetting(PrivacySettingType type, PrivacySettingValue value, JidProvider... excluded) {
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
        socketConnection.sendQuery("set", "privacy", Node.of("privacy", Node.of("category", attributes, children)));
        var newEntry = new PrivacySettingEntryBuilder()
                .type(type)
                .value(value)
                .excluded(excludedJids)
                .build();
        var oldEntry = store().findPrivacySetting(type);
        store().addPrivacySetting(type, newEntry);
        socketConnection.onPrivacySettingChanged(oldEntry, newEntry);
    }

    /**
     * Changes the default ephemeral timer of new chats.
     *
     * @param timer the new ephemeral timer
     */
    public void changeNewChatsEphemeralTimer(ChatEphemeralTimer timer) {
        socketConnection.sendQuery("set", "disappearing_mode", Node.of("disappearing_mode", Map.of("duration", timer.period().toSeconds())));
        store().setNewChatsEphemeralTimer(timer);
    }

    /**
     * Changes the name of this user
     *
     * @param newName the non-null new name
     */
    public void changeName(String newName) {
        if (store().device().platform().isBusiness()) {
            switch (store().clientType()) {
                case WEB -> {
                    throw new IllegalArgumentException("The business name cannot be changed using the web api. " +
                            "This is a limitation by WhatsApp. " +
                            "If this ever changes, please open an issue/PR.");
                }
                case MOBILE -> {
                    var oldName = store().name();
                    socketConnection.updateBusinessCertificate(newName);
                    socketConnection.onUserChanged(newName, oldName);
                }
            }
        }else {
            var oldName = store().name();
            socketConnection.sendNodeWithNoResponse(Node.of("presence", Map.of("name", newName, "type", "available")));
            socketConnection.onUserChanged(newName, oldName);
        }
    }

    /**
     * Changes the about of this user
     *
     * @param newAbout the non-null new status
     */
    public void changeAbout(String newAbout) {
        socketConnection.changeAbout(newAbout);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     */
    public void changeProfilePicture(InputStream image) {
        var data = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), data);
        switch (store().clientType()) {
            case WEB -> socketConnection.sendQuery("set", "w:profile:picture", body);
            case MOBILE -> socketConnection.sendQuery(jidOrThrowError(), "set", "w:profile:picture", body);
        }
    }

    /**
     * Change the description of this business profile
     *
     * @param description the new description, can be null
     * @return a CompletableFuture
     */
    public String changeBusinessDescription(String description) {
        return changeBusinessAttribute("description", description);
    }

    private String changeBusinessAttribute(String key, String value) {
        var result = socketConnection.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.of(key, Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8))));
        checkBusinessAttributeConflict(key, value, result);
        return value;
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

    private String findErrorNode(Node result) {
        return Optional.ofNullable(result)
                .flatMap(node -> node.findChild("error"))
                .map(Node::toString)
                .orElseGet(() -> Objects.toString(result));
    }

    /**
     * Change the address of this business profile
     *
     * @param address the new address, can be null
     * @return a CompletableFuture
     */
    public String changeBusinessAddress(String address) {
        return changeBusinessAttribute("address", address);
    }


    /**
     * Change the email of this business profile
     *
     * @param email the new email, can be null
     * @return a CompletableFuture
     */
    public String changeBusinessEmail(String email) {
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
    public Collection<BusinessCategory> changeBusinessCategories(Collection<BusinessCategory> categories) {
        socketConnection.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), Node.of("categories", createCategories(categories))));
        return categories;
    }

    private Collection<Node> createCategories(Collection<BusinessCategory> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .map(entry -> Node.of("category", Map.of("id", entry.id())))
                .toList();
    }

    /**
     * Change the websites of this business profile
     *
     * @param websites the new websites, can be null
     * @return a CompletableFuture
     */
    public Collection<URI> changeBusinessWebsites(Collection<URI> websites) {
        socketConnection.sendQuery("set", "w:biz", Node.of("business_profile", Map.of("v", "3", "mutation_type", "delta"), createWebsites(websites)));
        return websites;
    }

    private List<Node> createWebsites(Collection<URI> websites) {
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
    public Collection<BusinessCatalogEntry> queryBusinessCatalog() {
        return queryBusinessCatalog(10);
    }

    /**
     * Query the catalog of this business
     *
     * @param productsLimit the maximum number of products to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCatalogEntry> queryBusinessCatalog(int productsLimit) {
        return queryBusinessCatalog(jidOrThrowError().withoutData(), productsLimit);
    }

    /**
     * Query the catalog of a business
     *
     * @param contact       the business
     * @param productsLimit the maximum number of products to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCatalogEntry> queryBusinessCatalog(JidProvider contact, int productsLimit) {
        var result = socketConnection.sendQuery("get", "w:biz:catalog", Node.of("product_catalog", Map.of("jid", contact, "allow_shop_source", "true"), Node.of("limit", String.valueOf(productsLimit)
                .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))));
        return result.findChild("product_catalog")
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
    public Collection<BusinessCatalogEntry> queryBusinessCatalog(JidProvider contact) {
        return queryBusinessCatalog(contact, 10);
    }

    /**
     * Query the collections of this business
     *
     * @return a CompletableFuture
     */
    public Collection<BusinessCollectionEntry> queryBusinessCollections() {
        return queryBusinessCollections(50);
    }

    /**
     * Query the collections of this business
     *
     * @param collectionsLimit the maximum number of collections to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCollectionEntry> queryBusinessCollections(int collectionsLimit) {
        return queryBusinessCollections(jidOrThrowError().withoutData(), collectionsLimit);
    }

    /**
     * Query the collections of a business
     *
     * @param contact          the business
     * @param collectionsLimit the maximum number of collections to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCollectionEntry> queryBusinessCollections(JidProvider contact, int collectionsLimit) {
        var result = socketConnection.sendQuery("get", "w:biz:catalog", Map.of("smax_id", "35"), Node.of("collections", Map.of("biz_jid", contact), Node.of("collection_limit", String.valueOf(collectionsLimit)
                .getBytes(StandardCharsets.UTF_8)), Node.of("item_limit", String.valueOf(collectionsLimit)
                .getBytes(StandardCharsets.UTF_8)), Node.of("width", "100".getBytes(StandardCharsets.UTF_8)), Node.of("height", "100".getBytes(StandardCharsets.UTF_8))));
        return parseCollections(result);
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
    public Collection<BusinessCollectionEntry> queryBusinessCollections(JidProvider contact) {
        return queryBusinessCollections(contact, 50);
    }

    /**
     * Sends a custom node to Whatsapp
     *
     * @param node the non-null node to send
     * @return the newsletters from Whatsapp
     */
    public Node sendNode(Node node) {
        return socketConnection.sendNode(node);
    }

    /**
     * Gets the verified name certificate
     *
     */
    public Optional<BusinessVerifiedNameCertificate> queryBusinessCertificate(JidProvider provider) {
        var result = socketConnection.sendQuery("get", "w:biz", Node.of("verified_name", Map.of("jid", provider.toJid())));
        return parseCertificate(result);
    }

    private Optional<BusinessVerifiedNameCertificate> parseCertificate(Node result) {
        return result.findChild("verified_name")
                .flatMap(Node::contentAsBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode);
    }
    //</editor-fold>

    //<editor-fold desc="Presence">
    /**
     * Changes your presence for everyone on Whatsapp
     *
     * @param available whether you are online or not
     */
    public void changePresence(boolean available) {
        var status = socketConnection.store().online();
        if (status == available) {
            return;
        }

        var presence = available ? AVAILABLE : UNAVAILABLE;
        var node = Node.of("presence", Map.of("name", store().name(), "type", presence.toString()));
        socketConnection.sendNodeWithNoResponse(node);
        updatePresence(null, presence);
    }

    private void updatePresence(JidProvider chatJid, ContactStatus presence) {
        if (chatJid == null) {
            store().setOnline(presence == AVAILABLE);
        }

        var self = store().findContactByJid(jidOrThrowError().withoutData());
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
     */
    public void changePresence(JidProvider chatJid, ContactStatus presence) {
        if (presence == COMPOSING || presence == RECORDING) {
            var node = Node.of("chatstate",
                    Map.of("to", chatJid.toJid()),
                    Node.of(COMPOSING.toString(), presence == RECORDING ? Map.of("media", "audio") : Map.of()));
            socketConnection.sendNodeWithNoResponse(node);
            updatePresence(chatJid, presence);
            return;
        }

        var node = Node.of("presence", Map.of("type", presence.toString(), "name", store().name()));
        socketConnection.sendNodeWithNoResponse(node);
        updatePresence(chatJid, presence);
    }

    /**
     * Sends a request to Whatsapp to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jids the contacts whose status the api should receive updates on
     */
    public void subscribeToPresence(JidProvider... jids) {
        for (var jid : jids) {
            socketConnection.subscribeToPresence(jid);
        }
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jids the contacts whose status the api should receive updates on
     */
    public void subscribeToPresence(Collection<? extends JidProvider> jids) {
        for (var jid : jids) {
            socketConnection.subscribeToPresence(jid);
        }
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     */
    public void subscribeToPresence(JidProvider jid) {
        socketConnection.subscribeToPresence(jid);
    }
    //</editor-fold>

    //<editor-fold desc="React to a message">
    /**
     * Remove a reaction from a message
     *
     * @param message the non-null message
     * @return a CompletableFuture
     */
    public MessageInfo removeReaction(MessageInfo message) {
        return sendReaction(message, (String) null);
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction
     * @return a CompletableFuture
     */
    public MessageInfo sendReaction(MessageInfo message, ReactionEmoji reaction) {
        return sendReaction(message, Objects.toString(reaction));
    }

    /**
     * Send a reaction to a message
     *
     * @param message  the non-null message
     * @param reaction the reaction to send, null if you want to remove the reaction. If a string that
     * isn't an emoji supported by Whatsapp is used, it will not get displayed
     * correctly. Use {@link Whatsapp#sendReaction(MessageInfo, ReactionEmoji)} if
     * you need a typed emoji enum.
     * @return a CompletableFuture
     */
    public MessageInfo sendReaction(MessageInfo message, String reaction) {
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .chatJid(message.parentJid())
                .senderJid(message.senderJid())
                .fromMe(Objects.equals(message.senderJid().withoutData(), jidOrThrowError().withoutData()))
                .id(message.id())
                .build();
        var reactionMessage = new ReactionMessageBuilder()
                .key(key)
                .content(reaction)
                .timestampSeconds(Instant.now().toEpochMilli())
                .build();
        return sendChatMessage(message.parentJid(), MessageContainer.of(reactionMessage));
    }
    //</editor-fold>

    //<editor-fold desc="Send messages to chats and newsletters">
    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public MessageInfo sendMessage(JidProvider chat, String message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public ChatMessageInfo sendChatMessage(JidProvider chat, String message) {
        return sendChatMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a chat and a message
     *
     * @param chat    the chat where the message should be sent
     * @param message the message to send
     * @return a CompletableFuture
     */
    public NewsletterMessageInfo sendsNewsletterMessage(JidProvider chat, String message) {
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
    public MessageInfo sendMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public ChatMessageInfo sendChatMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public NewsletterMessageInfo sendNewsletterMessage(JidProvider chat, String message, MessageInfo quotedMessage) {
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
    public MessageInfo sendMessage(JidProvider chat, ContextualMessage message, MessageInfo quotedMessage) {
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
    public ChatMessageInfo sendChatMessage(JidProvider chat, ContextualMessage message, MessageInfo quotedMessage) {
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
    public NewsletterMessageInfo sendNewsletterMessage(JidProvider chat, ContextualMessage message, MessageInfo quotedMessage) {
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
    public MessageInfo sendMessage(JidProvider chat, Message message) {
        return sendMessage(chat, MessageContainer.of(message));
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public MessageInfo sendMessage(JidProvider recipient, MessageContainer message) {
        return recipient.toJid().hasServer(JidServer.newsletter())
                ? sendNewsletterMessage(recipient, message)
                : sendChatMessage(recipient, message);
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public ChatMessageInfo sendChatMessage(JidProvider recipient, MessageContainer message) {
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
    public ChatMessageInfo sendChatMessage(JidProvider recipient, MessageContainer message, boolean compose) {
        if (recipient.toJid().hasServer(JidServer.newsletter())) {
            throw new IllegalArgumentException("Use sendNewsletterMessage to send a message in a newsletter");
        }
        var timestamp = Clock.nowSeconds();
        var deviceInfoMetadata = new DeviceListMetadataBuilder()
                .senderTimestamp(Clock.nowSeconds())
                .build();
        var deviceInfo = recipient.toJid().hasServer(JidServer.user()) ? new DeviceContextInfoBuilder()
                .deviceListMetadataVersion(2)
                .deviceListMetadata(deviceInfoMetadata)
                .build() : null;
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
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
                .broadcast(recipient.toJid().hasServer(JidServer.broadcast()))
                .build();
        return sendChatMessage(info, compose);
    }

    /**
     * Builds and sends a message from a recipient and a message
     *
     * @param recipient the recipient where the message should be sent
     * @param message   the message to send
     * @return a CompletableFuture
     */
    public NewsletterMessageInfo sendNewsletterMessage(JidProvider recipient, MessageContainer message) {
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
     * Sends a message to a chat
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public ChatMessageInfo sendChatMessage(ChatMessageInfo info) {
        return sendChatMessage(info, true);
    }

    /**
     * Sends a message to a chat
     *
     * @param info the message to send
     * @param compose whether a compose status should be sent before sending the message
     * @return a CompletableFuture
     */
    public ChatMessageInfo sendChatMessage(ChatMessageInfo info, boolean compose) {
        var recipient = info.chatJid();
        if (recipient.hasServer(JidServer.newsletter())) {
            throw new IllegalArgumentException("Use sendNewsletterMessage to send a message in a newsletter");
        }
        var timestamp = Clock.nowSeconds();
        if (compose) {
            changePresence(recipient, COMPOSING);
        }
        socketConnection.sendMessage(new MessageRequest.Chat(info));
        if (compose) {
            var node = Node.of("chatstate",
                    Map.of("to", recipient),
                    Node.of("paused"));
            socketConnection.sendNodeWithNoResponse(node);
            updatePresence(recipient, AVAILABLE);
        }
        return info;
    }

    /**
     * Sends a message to a newsletter
     *
     * @param info the message to send
     * @return a CompletableFuture
     */
    public NewsletterMessageInfo sendMessage(NewsletterMessageInfo info) {
        socketConnection.sendMessage(new MessageRequest.Newsletter(info));
        return info;
    }
    //</editor-fold>

    //<editor-fold desc="Send status updates">
    /**
     * Sends a status update message to {@link Jid#statusBroadcastAccount()}
     *
     * @param message the non-null text message to send
     * @return the message that was sent
     */
    public ChatMessageInfo sendStatus(String message) {
        return sendStatus(MessageContainer.of(message));
    }


    /**
     * Sends a status update message to {@link Jid#statusBroadcastAccount()}
     *
     * @param message the non-null message to send
     * @return the message that was sent
     */
    public ChatMessageInfo sendStatus(Message message) {
        return sendStatus(MessageContainer.of(message));
    }


    /**
     * Sends a status update message to {@link Jid#statusBroadcastAccount()}
     *
     * @param message the non-null message to send
     * @return the message that was sent
     */
    public ChatMessageInfo sendStatus(MessageContainer message) {
        var timestamp = Clock.nowSeconds();
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store().clientType()))
                .chatJid(Jid.statusBroadcastAccount())
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
        return sendChatMessage(info, false);
    }
    //</editor-fold>

    //<editor-fold desc="Message utilities">
    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, a reupload request will be sent to Whatsapp.
     * If the latter fails as well, an empty optional will be returned.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public byte[] downloadMedia(ChatMessageInfo info) {
        if (!(info.message().content() instanceof MediaMessage mediaMessage)) {
            throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
        }

        try {
            return downloadMedia(mediaMessage);
        } catch (Exception ignored) {
            requireMediaReupload(info);
            return downloadMedia(mediaMessage);
        }
    }

    /**
     * Downloads a media from Whatsapp's servers.
     * If the media was already downloaded, the cached version will be returned.
     * If the download fails because the media is too old/invalid, an empty optional will be returned.
     *
     * @param info the non-null message info wrapping the media
     * @return a CompletableFuture
     */
    public byte[] downloadMedia(NewsletterMessageInfo info) {
        if (!(info.message().content() instanceof MediaMessage mediaMessage)) {
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
    public byte[] downloadMedia(MediaMessage mediaMessage) {
        var decodedMedia = mediaMessage.decodedMedia();
        if (decodedMedia.isPresent()) {
            return decodedMedia.get();
        }

        var result = Medias.download(mediaMessage);
        mediaMessage.setDecodedMedia(result);
        return result;
    }

    /**
     * Asks Whatsapp for a media reupload for a specific media
     *
     * @param info the non-null message info wrapping the media
     */
    public void requireMediaReupload(ChatMessageInfo info) {
        try {
            if (!(info.message().content() instanceof MediaMessage mediaMessage)) {
                throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
            }

            var mediaKey = mediaMessage.mediaKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing media key"));
            var retryKey = Hkdf.extractAndExpand(mediaKey, "WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
            var receipt = ServerErrorReceiptSpec.encode(new ServerErrorReceipt(info.id()));
            var aad = info.key().id().getBytes(StandardCharsets.UTF_8);
            var encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            encryptCipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(retryKey, "AES"),
                    new GCMParameterSpec(128, Bytes.random(12))
            );
            encryptCipher.updateAAD(aad);
            var ciphertext = encryptCipher.update(receipt);
            var rmrAttributes = Attributes.of()
                    .put("jid", info.chatJid())
                    .put("from_me", String.valueOf(info.fromMe()))
                    .put("participant", info.senderJid(), () -> !Objects.equals(info.chatJid(), info.senderJid()))
                    .toMap();
            var node = Node.of("receipt", Map.of("id", info.key().id(), "to", jidOrThrowError()
                    .withoutData(), "type", "server-error"), Node.of("encrypt", Node.of("enc_p", ciphertext), Node.of("enc_iv", Bytes.random(12))), Node.of("rmr", rmrAttributes));
            var result = socketConnection.sendNode(node, resultNode -> resultNode.hasDescription("notification"));
            if (result.hasNode("error")) {
                throw new IllegalArgumentException("Erroneous response from media reupload: " + result.attributes().getInt("code"));
            }
            var encryptNode = result.findChild("encrypt")
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypt node in media reupload"));
            var mediaPayload = encryptNode.findChild("enc_p")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted payload node in media reupload"));
            var mediaIv = encryptNode.findChild("enc_iv")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted iv node in media reupload"));
            var decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            decryptCipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(retryKey, "AES"),
                    new GCMParameterSpec(128, mediaIv)
            );
            decryptCipher.updateAAD(aad);
            var mediaRetryNotificationData = decryptCipher.doFinal(mediaPayload);
            var mediaRetryNotification = MediaRetryNotificationSpec.decode(mediaRetryNotificationData);
            var directPath = mediaRetryNotification.directPath()
                    .orElseThrow(() -> new RuntimeException("Media reupload failed"));
            mediaMessage.setMediaUrl(Medias.createMediaUrl(directPath));
            mediaMessage.setMediaDirectPath(directPath);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot reupload media", exception);
        }
    }
    
    /**
     * Awaits for a single newsletters to a message
     *
     * @param info the non-null message whose newsletters is pending
     * @return a non-null newsletters
     */
    @SuppressWarnings("unchecked")
    public <T extends MessageInfo> T waitForMessageReply(T info) {
        return (T) waitForMessageReply(info.id());
    }

    /**
     * Awaits for a single newsletters to a message
     *
     * @param id the non-null id of message whose newsletters is pending
     * @return a non-null newsletters
     */
    public MessageInfo waitForMessageReply(String id) {
        return socketConnection.waitForMessageReply(id);
    }

    /**
     * Forwards a message to another chat
     *
     * @param chat the non-null chat
     * @param messageInfo the message to forward
     */
    public ChatMessageInfo forwardChatMessage(JidProvider chat, ChatMessageInfo messageInfo) {
        var message = messageInfo.message()
                .contentWithContext()
                .map(this::createForwardedMessage)
                .or(() -> createForwardedText(messageInfo))
                .orElseThrow(() -> new IllegalArgumentException("This message cannot be forwarded: " + messageInfo.message().type()));
        return sendChatMessage(chat, message);
    }

    private MessageContainer createForwardedMessage(ContextualMessage messageWithContext) {
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
     * Builds and sends an edited message
     *
     * @param oldMessage the message to edit
     * @param newMessage the new message's content
     * @return a CompletableFuture
     */
    public <T extends MessageInfo> T editMessage(T oldMessage, Message newMessage) {
        var oldMessageType = oldMessage.message().content().type();
        var newMessageType = newMessage.type();
        if (oldMessageType != newMessageType) {
            throw new IllegalArgumentException("Message type mismatch: %s != %s".formatted(oldMessageType, newMessageType));
        }
        switch (oldMessage) {
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
                socketConnection.sendMessage(request);
                return oldMessage;
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
                socketConnection.sendMessage(request);
                return oldMessage;
            }
            default -> throw new IllegalStateException("Unsupported edit: " + oldMessage);
        }
    }

    /**
     * Deletes a message
     *
     * @param messageInfo the non-null message to delete
     */
    public void deleteMessage(NewsletterMessageInfo messageInfo) {
        var revokeInfo = new NewsletterMessageInfoBuilder()
                .id(messageInfo.id())
                .serverId(messageInfo.serverId())
                .timestampSeconds(Clock.nowSeconds())
                .message(MessageContainer.empty())
                .status(MessageStatus.PENDING)
                .build();
        revokeInfo.setNewsletter(messageInfo.newsletter());
        var attrs = Map.of("edit", getEditBit(messageInfo));
        var request = new MessageRequest.Newsletter(revokeInfo, attrs);
        socketConnection.sendMessage(request);
    }

    /**
     * Deletes a message
     *
     * @param messageInfo non-null message to delete
     * @param everyone    whether the message should be deleted for everyone or only for this client and
     * its companions
     */
    public void deleteMessage(ChatMessageInfo messageInfo, boolean everyone) {
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
            var attrs = Map.of("edit", getEditBit(messageInfo));
            var request = new MessageRequest.Chat(revokeInfo, null, false, false, attrs);
            socketConnection.sendMessage(request);
            return;
        }

        switch (store().clientType()) {
            case WEB -> {
                var range = createRange(messageInfo.chatJid(), false);
                var deleteMessageAction = new DeleteMessageForMeActionBuilder()
                        .deleteMedia(false)
                        .messageTimestampSeconds(messageInfo.timestampSeconds().orElse(0L))
                        .build();
                var syncAction = ActionValueSync.of(deleteMessageAction);
                var entry = PatchEntry.of(syncAction, Operation.SET, messageInfo.chatJid().toString(), messageInfo.id(), fromMeToFlag(messageInfo), participantToFlag(messageInfo));
                var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
                socketConnection.pushPatch(request);
            }
            case MOBILE -> messageInfo.chat().ifPresent(chat -> chat.removeMessage(messageInfo));
        }
    }


    private int getEditBit(MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 3;
        }

        return 1;
    }

    private int getDeleteBit(MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 8;
        }

        var fromMe = Objects.equals(info.senderJid().withoutData(), jidOrThrowError().withoutData());
        if (info.parentJid().hasServer(JidServer.groupOrCommunity()) && !fromMe) {
            return 8;
        }

        return 7;
    }
    //</editor-fold>

    //<editor-fold desc="Change state">  
    /**
     * Marks a chat as read.
     *
     * @param chat the target chat
     */
    public void markChatRead(JidProvider chat) {
        mark(chat, true);
        store().findChatByJid(chat.toJid())
                .stream()
                .map(Chat::unreadMessages)
                .flatMap(Collection::stream)
                .forEach(this::markMessageRead);
    }

    /**
     * Marks a chat as unread
     *
     * @param chat the target chat
     */
    public void markChatUnread(JidProvider chat) {
        mark(chat, false);
    }

    private void mark(JidProvider chat, boolean read) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat.toJid())
                    .ifPresent(entry -> entry.setMarkedAsUnread(read));
            return;
        }

        var range = createRange(chat, false);
        var markAction = new MarkChatAsReadActionBuilder()
                .read(read)
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(markAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Mutes a chat indefinitely
     *
     * @param chat the target chat
     */
    public void muteChat(JidProvider chat) {
        muteChat(chat, ChatMute.muted());
    }

    /**
     * Mutes a chat
     *
     * @param chat the target chat
     * @param mute the type of mute
     */
    public void muteChat(JidProvider chat, ChatMute mute) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(mute));
            return;
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
        socketConnection.pushPatch(request);
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     */
    public void unmuteChat(JidProvider chat) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(ChatMute.notMuted()));
            return;
        }

        var muteAction = new MuteActionBuilder()
                .muted(false)
                .muteEndTimestampSeconds(0)
                .autoMuted(false)
                .build();
        var syncAction = ActionValueSync.of(muteAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Blocks a contact
     *
     * @param contact the target chat
     */
    public void blockContact(JidProvider contact) {
        var body = Node.of("item", Map.of("action", "block", "jid", contact.toJid()));
        socketConnection.sendQuery("set", "blocklist", body);
    }

    /**
     * Unblocks a contact
     *
     * @param contact the target chat
     */
    public void unblockContact(JidProvider contact) {
        var body = Node.of("item", Map.of("action", "unblock", "jid", contact.toJid()));
        socketConnection.sendQuery("set", "blocklist", body);
    }

    /**
     * Enables ephemeral messages in a chat, this means that messages will be automatically cancelled
     * in said chat after a week
     *
     * @param chat the target chat
     */
    public void changeEphemeralTimer(JidProvider chat, ChatEphemeralTimer timer) {
        switch (chat.toJid().server().type()) {
            case USER -> {
                var message = new ProtocolMessageBuilder()
                        .protocolType(ProtocolMessage.Type.EPHEMERAL_SETTING)
                        .ephemeralExpirationSeconds(timer.period().toSeconds())
                        .build();
                sendMessage(chat, message);
            }
            case GROUP_OR_COMMUNITY -> {
                var body = timer == ChatEphemeralTimer.OFF ? Node.of("not_ephemeral") : Node.of("ephemeral", Map.of("expiration", timer.period()
                        .toSeconds()));
                socketConnection.sendQuery(chat.toJid(), "set", "w:g2", body);
            }
            default ->
                    throw new IllegalArgumentException("Unexpected chat %s: ephemeral messages are only supported for conversations and groups".formatted(chat.toJid()));
        }
    }

    /**
     * Marks a message as played
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public ChatMessageInfo markMessagePlayed(ChatMessageInfo info) {
        if (store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS).value() != PrivacySettingValue.EVERYONE) {
            return info;
        }
        socketConnection.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), "played");
        info.setStatus(MessageStatus.PLAYED);
        return info;
    }

    /**
     * Pins a chat to the top. A maximum of three chats can be pinned to the top. This condition can
     * be checked using;.
     *
     * @param chat the target chat
     */
    public void pinChat(JidProvider chat) {
        pinChat(chat, true);
    }

    /**
     * Unpins a chat from the top
     *
     * @param chat the target chat
     */
    public void unpinChat(JidProvider chat) {
        pinChat(chat, false);
    }

    private void pinChat(JidProvider chat, boolean pin) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setPinnedTimestampSeconds(pin ? (int) Clock.nowSeconds() : 0));
            return;
        }
        var pinAction = new PinActionBuilder()
                .pinned(pin)
                .build();
        var syncAction = ActionValueSync.of(pinAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Stars a message
     *
     * @param info the target message
     * @return a CompletableFuture
     */
    public ChatMessageInfo starMessage(ChatMessageInfo info) {
        return starMessage(info, true);
    }

    private ChatMessageInfo starMessage(ChatMessageInfo info, boolean star) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            info.setStarred(star);
            return info;
        }

        var starAction = new StarActionBuilder()
                .starred(star)
                .build();
        var syncAction = ActionValueSync.of(starAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        socketConnection.pushPatch(request);
        return info;
    }

    private String fromMeToFlag(MessageInfo info) {
        var fromMe = Objects.equals(info.senderJid().withoutData(), jidOrThrowError().withoutData());
        return booleanToInt(fromMe);
    }

    private String participantToFlag(MessageInfo info) {
        var fromMe = Objects.equals(info.senderJid().withoutData(), jidOrThrowError().withoutData());
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
    public ChatMessageInfo unstarMessage(ChatMessageInfo info) {
        return starMessage(info, false);
    }

    /**
     * Archives a chat. If said chat is pinned, it will be unpinned.
     *
     * @param chat the target chat
     */
    public void archiveChat(JidProvider chat) {
        archiveChat(chat, true);
    }

    private void archiveChat(JidProvider chat, boolean archive) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat)
                    .ifPresent(entry -> entry.setArchived(archive));
            return;
        }

        var range = createRange(chat, false);
        var archiveAction = new ArchiveChatActionBuilder()
                .archived(archive)
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(archiveAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString());
        var request = new PatchRequest(PatchType.REGULAR_LOW, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     */
    public void unarchive(JidProvider chat) {
        archiveChat(chat, false);
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
    public ChatMessageInfo markMessageRead(ChatMessageInfo info) {
        var type = store().findPrivacySetting(PrivacySettingType.READ_RECEIPTS)
                .value() == PrivacySettingValue.EVERYONE ? "read" : "read-self";
        socketConnection.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.id()), type);
        info.chat().ifPresent(chat -> {
            var count = chat.unreadMessagesCount();
            if (count > 0) {
                chat.setUnreadMessagesCount(count - 1);
            }
        });
        info.setStatus(MessageStatus.READ);
        return info;
    }
    //</editor-fold>  

    //<editor-fold desc="Groups and communities">
    /**
     * Queries the metadata of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public GroupOrCommunityMetadata queryGroupOrCommunityMetadata(JidProvider chat) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        return socketConnection.queryGroupOrCommunityMetadata(chat.toJid());
    }

    /**
     * Queries the invite link of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public String queryGroupOrCommunityInviteLink(JidProvider chat) {
        var inviteCode = queryGroupOrCommunityInviteCode(chat);
        return "https://chat.whatsapp.com/" + inviteCode;
    }

    /**
     * Queries the invite code of a group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public String queryGroupOrCommunityInviteCode(JidProvider chat) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var result = socketConnection.sendQuery(chat.toJid(), "get", "w:g2", Node.of("invite"));
        return result.findChild("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite code in invite newsletters"))
                .attributes()
                .getRequiredString("code");
    }

    /**
     * Queries the lists of participants currently waiting to be accepted into the group
     *
     * @param chat the target group
     * @return a CompletableFuture
     */
    public Collection<Jid> queryGroupOrCommunityParticipantsPendingApproval(JidProvider chat) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var result = socketConnection.sendQuery(chat.toJid(), "get", "w:g2", Node.of("membership_approval_requests"));
        return result.findChild("membership_approval_requests")
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
    public Collection<Jid> approveGroupOrCommunityParticipants(JidProvider chat, boolean approve, JidProvider... participants) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var participantsNodes = Arrays.stream(participants)
                .map(participantJid -> Node.of("participant", Map.of("jid", participantJid)))
                .toList();
        var action = approve ? "approve" : "reject";
        var result = socketConnection.sendQuery(chat.toJid(), "set", "w:g2", Node.of("membership_requests_action", Node.of(action, participantsNodes)));
        return result.findChild("membership_requests_action")
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
     */
    public void revokeGroupOrCommunityInvite(JidProvider chat) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        
        socketConnection.sendQuery(chat.toJid(), "set", "w:g2", Node.of("invite"));
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite countryCode
     * @return a CompletableFuture
     */
    public Optional<Chat> acceptGroupOrCommunityInvite(String inviteCode) {
        var result = socketConnection.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", Node.of("invite", Map.of("code", inviteCode)));
        return result.findChild("group")
                .flatMap(group -> group.attributes().getOptionalJid("jid"))
                .map(jid -> store().findChatByJid(jid).orElseGet(() -> store().addNewChat(jid)));
    }

    /**
     * Promotes any number of contacts to admin in a group
     *
     * @param chat    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public Collection<Jid> promoteGroupOrCommunityParticipants(JidProvider chat, JidProvider... contacts) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        
        var metadata = queryGroupOrCommunityMetadata(chat.toJid());
        var participantsSet = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toUnmodifiableSet());
        var targets = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(participantsSet::contains)
                .collect(Collectors.toUnmodifiableSet());
        return executeActionOnParticipants(chat, metadata.isCommunity(), GroupAction.PROMOTE, targets);
    }

    /**
     * Demotes any number of contacts to admin in a group
     *
     * @param chat    the target group
     * @param contacts the target contacts
     * @return a CompletableFuture
     */
    public Collection<Jid> demoteGroupOrCommunityParticipants(JidProvider chat, JidProvider... contacts) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var metadata = queryGroupOrCommunityMetadata(chat.toJid());
        var participantsSet = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toUnmodifiableSet());
        var targets = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(participantsSet::contains)
                .collect(Collectors.toUnmodifiableSet());
        return executeActionOnParticipants(chat, metadata.isCommunity(), GroupAction.DEMOTE, targets);
    }

    /**
     * Adds any number of contacts to a group
     *
     * @param chat    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public Collection<Jid> addGroupOrCommunityParticipants(JidProvider chat, JidProvider... contacts) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var metadata = queryGroupOrCommunityMetadata(chat.toJid());
        var participantsSet = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toUnmodifiableSet());
        var targets = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(entry -> !participantsSet.contains(entry))
                .collect(Collectors.toUnmodifiableSet());
        return executeActionOnParticipants(chat, metadata.isCommunity(), GroupAction.ADD, targets);
    }

    /**
     * Removes any number of contacts from group
     *
     * @param chat    the target group
     * @param contacts the target contact/s
     * @return a CompletableFuture
     */
    public Collection<Jid> removeGroupOrCommunityParticipants(JidProvider chat, JidProvider... contacts) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var metadata = queryGroupOrCommunityMetadata(chat.toJid());
        var participantsSet = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toUnmodifiableSet());
        var targets = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .filter(participantsSet::contains)
                .collect(Collectors.toUnmodifiableSet());
        return executeActionOnParticipants(chat, metadata.isCommunity(), GroupAction.REMOVE, targets);
    }

    private List<Jid> executeActionOnParticipants(JidProvider chat, boolean community, GroupAction action, Set<Jid> jids) {
        if(jids.isEmpty()) {
            return List.of();
        }

        var participants = jids.stream()
                .map(JidProvider::toJid)
                .map(jid -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(jid, "Cannot execute action on yourself"))))
                .toArray(Node[]::new);
        var result = socketConnection.sendQuery(chat.toJid(), "set", "w:g2", Node.of(action.data(), participants));
        return result.findChild(action.data())
                .map(body -> body.listChildren("participant"))
                .stream()
                .flatMap(Collection::stream)
                .filter(participant -> !participant.attributes().hasKey("error"))
                .map(participant -> participant.attributes().getOptionalJid("jid"))
                .flatMap(Optional::stream)
                .toList();
    }

    private Jid checkGroupParticipantJid(Jid jid, String errorMessage) {
        if (Objects.equals(jid.withoutData(), jidOrThrowError().withoutData())) {
            throw new IllegalArgumentException(errorMessage);
        }

        return jid;
    }

    /**
     * Changes the name of a group
     *
     * @param chat   the target group
     * @param newName the new name for the group
     * @throws IllegalArgumentException if the provided new name is empty or blank
     */
    public void changeGroupOrCommunitySubject(JidProvider chat, String newName) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Empty subjects are not allowed");
        }
        var body = Node.of("subject", newName.getBytes(StandardCharsets.UTF_8));
        socketConnection.sendQuery(chat.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes the description of a group
     *
     * @param chat       the target group
     * @param description the new name for the group, can be null if you want to remove it
     */
    public void changeGroupOrCommunityDescription(JidProvider chat, String description) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var descriptionId = socketConnection.queryGroupOrCommunityMetadata(chat.toJid())
                .descriptionId()
                .orElse(null);
        var descriptionNode = Optional.ofNullable(description)
                .map(content -> Node.of("body", content.getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
        var attributes = Attributes.of()
                .put("id", SocketConnection.randomSid(), () -> description != null)
                .put("delete", true, () -> description == null)
                .put("prev", descriptionId, () -> descriptionId != null)
                .toMap();
        var body = Node.of("description", attributes, descriptionNode);
        socketConnection.sendQuery(chat.toJid(), "set", "w:g2", body);
    }

    /**
     * Changes the picture of a group
     *
     * @param group the target group
     * @param image the new image, can be null if you want to remove it
     */
    public void changeGroupOrCommunityPicture(JidProvider group, InputStream image) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var profilePic = image != null ? Medias.getProfilePic(image) : null;
        var body = Node.of("picture", Map.of("type", "image"), profilePic);
        socketConnection.sendQuery("set", "w:profile:picture", Map.of("target", group.toJid()), body);
    }

    /**
     * Creates a new group
     *
     * @param subject  the new group's name
     * @param contacts at least one contact to add to the group
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, JidProvider... contacts) {
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
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, ChatEphemeralTimer timer, JidProvider... contacts) {
        return createGroup(subject, timer, null, contacts);
    }

    /**
     * Creates a new group
     *
     * @param subject     the new group's name
     * @param parentCommunity the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, JidProvider parentCommunity) {
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
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity) {
        return createGroup(subject, timer, parentCommunity, new JidProvider[0]);
    }

    private Optional<GroupOrCommunityMetadata> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity, JidProvider... contacts) {
        var timestamp = Clock.nowSeconds();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("The subject of a group cannot be blank");
        }
        if(parentCommunity == null && contacts.length < 1) {
            throw new IllegalArgumentException("Expected at least 1 member for this group");
        }
        var availableMembers = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .collect(Collectors.toUnmodifiableSet());
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
                .map(Jid::withoutData)
                .distinct()
                .map(contact -> Node.of("participant", Map.of("jid", checkGroupParticipantJid(contact.toJid(), "Cannot create group with yourself as a participant"))))
                .forEach(children::add);
        var body = Node.of("create", Map.of("subject", subject, "key", timestamp), children);
        var future = socketConnection.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", body);
        return parseGroupResult(future);
    }

    /**
     * Leaves a group
     *
     * @param group the target group
     * @throws IllegalArgumentException if the provided chat is not a group
     */
    public void leaveGroup(JidProvider group) {
        if (!group.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }

        var metadata = queryGroupOrCommunityMetadata(group);
        if(metadata.isCommunity()) {
            var communityJid = metadata.parentCommunityJid().orElse(metadata.jid());
            var body = Node.of("leave", Node.of("linked_groups", Map.of("parent_group_jid", communityJid)));
            socketConnection.sendQuery("set", "w:g2", body);
        }else {
            var body = Node.of("leave", Node.of("group", Map.of("id", group.toJid())));
            socketConnection.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", body);
        }
    }
    
    /**
     * Deletes a chat for this client and its companions using a modern version of Whatsapp Important:
     * this message doesn't seem to work always as of now
     *
     * @param chat the non-null chat to delete
     */
    public void deleteChat(JidProvider chat) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().removeChat(chat.toJid());
            return;
        }

        var range = createRange(chat.toJid(), false);
        var deleteChatAction = new DeleteChatActionBuilder()
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(deleteChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString(), "1");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Clears the content of a chat for this client and its companions using a modern version of
     * Whatsapp Important: this message doesn't seem to work always as of now
     *
     * @param chat                the non-null chat to clear
     * @param keepStarredMessages whether starred messages in this chat should be kept
     */
    public void clearChat(JidProvider chat, boolean keepStarredMessages) {
        if (store().clientType() == WhatsappClientType.MOBILE) {
            store().findChatByJid(chat.toJid())
                    .ifPresent(Chat::removeMessages);
            return;
        }

        var known = store().findChatByJid(chat);
        var range = createRange(chat.toJid(), true);
        var clearChatAction = new ClearChatActionBuilder()
                .messageRange(range)
                .build();
        var syncAction = ActionValueSync.of(clearChatAction);
        var entry = PatchEntry.of(syncAction, Operation.SET, chat.toJid().toString(), booleanToInt(keepStarredMessages), "0");
        var request = new PatchRequest(PatchType.REGULAR_HIGH, List.of(entry));
        socketConnection.pushPatch(request);
    }

    /**
     * Creates a new community
     *
     * @param subject the non-null name of the new community
     * @param body    the nullable description of the new community
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createCommunity(String subject, String body) {
        var descriptionId = HexFormat.of().formatHex(Bytes.random(12));
        var children = new ArrayList<Node>();
        children.add(Node.of("description", Map.of("id", descriptionId), Node.of("body", Objects.requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8))));
        children.add(Node.of("parent", Map.of("default_membership_approval_mode", "request_required")));
        children.add(Node.of("allow_non_admin_sub_group_creation"));
        children.add(Node.of("create_general_chat"));
        var entry = Node.of("create", Map.of("subject", subject), children);
        var resultNode = socketConnection.sendQuery(JidServer.groupOrCommunity().toJid(), "set", "w:g2", entry);
        return parseGroupResult(resultNode);
    }

    private Optional<GroupOrCommunityMetadata> parseGroupResult(Node node) {
        return node.findChild("group")
                .map(socketConnection::handleGroupMetadata);
    }

    /**
     * Deactivates a community
     *
     * @param community the target community
     */
    public void deactivateCommunity(JidProvider community) {
        if (!community.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a community");
        }
        socketConnection.sendQuery(community.toJid(), "set","w:g2", Node.of("delete_parent"));
    }

    /**
     * Changes a group setting
     *
     * @param chat   the non-null group affected by this change
     * @param setting the non-null setting
     * @param policy  the non-null policy
     */
    public void changeGroupOrCommunitySetting(JidProvider chat, ChatSetting setting, ChatSettingPolicy policy) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("This method only accepts groups");
        }
        var metadata = queryGroupOrCommunityMetadata(chat);
        switch (setting) {
            case GroupSetting groupSetting -> {
                if(metadata.isCommunity()) {
                    throw new  IllegalArgumentException("Cannot change community setting '" + setting + "' in a group");
                }
                var body = switch (groupSetting) {
                    case EDIT_GROUP_INFO -> Node.of(policy == ChatSettingPolicy.ADMINS ? "locked" : "unlocked");
                    case SEND_MESSAGES -> Node.of(policy == ChatSettingPolicy.ADMINS ? "announcement" : "not_announcement");
                    case ADD_PARTICIPANTS ->
                            Node.of("member_add_mode", policy == ChatSettingPolicy.ADMINS ? "admin_add".getBytes(StandardCharsets.UTF_8) : "all_member_add".getBytes(StandardCharsets.UTF_8));
                    case APPROVE_PARTICIPANTS ->
                            Node.of("membership_approval_mode", Node.of("group_join", Map.of("state", policy == ChatSettingPolicy.ADMINS ? "on" : "off")));
                };
                socketConnection.sendQuery(chat.toJid(), "set", "w:g2", body);
            }
            case CommunitySetting communitySetting -> {
                if(!metadata.isCommunity()) {
                    throw new IllegalArgumentException("Cannot change group setting '" + setting + "' in a community");
                }
                
                switch (communitySetting) {
                    case MODIFY_GROUPS -> {
                        var request = CommunityRequests.changeModifyGroupsSetting(chat.toJid(), policy == ChatSettingPolicy.ANYONE);
                        var body = Node.of("query", Map.of("query_id", "24745914578387890"), request.getBytes());
                        var result = socketConnection.sendQuery("get", "w:mex", body);
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
                    }
                    case ADD_PARTICIPANTS -> {
                        var body = Node.of("member_add_mode", policy == ChatSettingPolicy.ANYONE ? "all_member_add".getBytes() : "admin_add".getBytes());
                        var result = socketConnection.sendQuery(chat.toJid(), "set", "w:g2", body);
                        if (result.hasNode("error")) {
                            throw new IllegalArgumentException("Cannot change community setting: " + result);
                        }
                    }
                }
            }
        }
    }

    /**
     * Links any number of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups    the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public Set<Jid> linkGroupsToCommunity(JidProvider community, JidProvider... groups) {
        var body = Arrays.stream(groups)
                .map(entry -> Node.of("group", Map.of("jid", entry.toJid())))
                .toArray(Node[]::new);
        var result = socketConnection.sendQuery(community.toJid(), "set", "w:g2", Node.of("links", Node.of("link", Map.of("link_type", "sub_group"), body)));
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
                .filter(success::contains)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Unlinks a group from a community
     *
     * @param community the non-null parent community
     * @param group     the non-null group to unlink
     * @return a CompletableFuture that indicates whether the request was successful
     */
    public boolean unlinkGroupFromCommunity(JidProvider community, JidProvider group) {
        var result = socketConnection.sendQuery(community.toJid(), "set", "w:g2", Node.of("unlink", Map.of("unlink_type", "sub_group"), Node.of("group", Map.of("jid", group.toJid()))));
        return result.findChild("unlink")
                .filter(entry -> entry.attributes().hasValue("unlink_type", "sub_group"))
                .flatMap(entry -> entry.findChild("group"))
                .map(entry -> entry.attributes().hasValue("jid", group.toJid().toString()))
                .isPresent();
    }
    //</editor-fold>
    
    //<editor-fold desc="Newsletters">  
    /**
     * Queries a list of fifty recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @return a list of recommended newsletters, if the feature is available
     */
    public Collection<Newsletter> queryRecommendedNewsletters(String countryCode) {
        return queryRecommendedNewsletters(countryCode, 50);
    }


    /**
     * Queries a list of recommended newsletters by country
     *
     * @param countryCode the non-null country code
     * @param limit       how many entries should be returned
     * @return a list of recommended newsletters, if the feature is available
     */
    public Collection<Newsletter> queryRecommendedNewsletters(String countryCode, int limit) {
        var request = NewsletterRequests.recommendedNewsletters("RECOMMENDED", List.of(countryCode), limit);
        return socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6190824427689257"), request))
                .findChild("result")
                .flatMap(Node::contentAsBytes)
                .flatMap(RecommendedNewslettersResponse::of)
                .map(RecommendedNewslettersResponse::newsletters)
                .orElse(List.of());
    }

    /**
     * Queries any number of messages from a newsletter
     *
     * @param newsletterJid the non-null jid of the newsletter
     * @param count         how many messages should be queried
     */
    public void queryNewsletterMessages(JidProvider newsletterJid, int count) {
        socketConnection.queryNewsletterMessages(newsletterJid, count);
    }

    /**
     * Subscribes to a public newsletter's event stream of reactions
     *
     * @param channel the non-null channel
     * @return the time, in minutes, during which updates will be sent
     */
    public OptionalLong subscribeToNewsletterReactions(JidProvider channel) {
        return socketConnection.subscribeToNewsletterReactions(channel);
    }

    /**
     * Creates a newsletter
     *
     * @param name the non-null name of the newsletter
     */
    public Optional<Newsletter> createNewsletter(String name) {
        return createNewsletter(name, null, null);
    }

    /**
     * Creates newsletter channel
     *
     * @param name        the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     */
    public Optional<Newsletter> createNewsletter(String name, String description) {
        return createNewsletter(name, description, null);
    }

    /**
     * Creates a newsletter
     *
     * @param name        the non-null name of the newsletter
     * @param description the nullable description of the newsletter
     * @param picture     the nullable profile picture of the newsletter
     */
    public Optional<Newsletter> createNewsletter(String name, String description, byte[] picture) {
        var request = NewsletterRequests.createNewsletter(name, description, picture != null ? Base64.getEncoder().encodeToString(picture) : null);
        var result = socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6996806640408138"), request))
                .findChild("result")
                .flatMap(Node::contentAsBytes)
                .flatMap(NewsletterResponse::ofJson)
                .map(NewsletterResponse::newsletter);
        if(result.isPresent()) {
            subscribeToNewsletterReactions(result.get().jid());
        }
        return result;
    }

    /**
     * Changes the description of a newsletter
     *
     * @param newsletter  the non-null target newsletter
     * @param description the nullable new description
     */
    public void changeNewsletterDescription(JidProvider newsletter, String description) {
        var request = NewsletterRequests.updateNewsletter(newsletter.toJid(), Objects.requireNonNullElse(description, ""));
        socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7150902998257522"), request));
    }

    /**
     * Joins a newsletter
     *
     * @param newsletter a non-null newsletter
     */
    public void joinNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.joinNewsletter(newsletter.toJid());
        socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "9926858900719341"), request));
    }

    /**
     * Leaves a newsletter
     *
     * @param newsletter a non-null newsletter
     */
    public void leaveNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.leaveNewsletter(newsletter.toJid());
        socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6392786840836363"), request));
    }

    /**
     * Queries the number of people subscribed to a newsletter
     *
     * @param newsletter the id of the newsletter
     * @return a CompletableFuture
     */
    public Optional<Long> queryNewsletterSubscribers(JidProvider newsletter) {
        var newsletterRole = store()
                .findNewsletterByJid(newsletter)
                .flatMap(Newsletter::viewerMetadata)
                .map(NewsletterViewerMetadata::role)
                .orElse(NewsletterViewerRole.GUEST);
        var request = NewsletterRequests.newsletterSubscribers(newsletter.toJid(), "JID", newsletterRole);
        return socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7272540469429201"), request))
                .findChild("result")
                .flatMap(Node::contentAsBytes)
                .flatMap(NewsletterSubscribersResponse::ofJson)
                .flatMap(NewsletterSubscribersResponse::subscribersCount);
    }

    /**
     * Sends an invitation to the jid provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param admin        the new admin
     */
    public void inviteNewsletterAdmin(JidProvider newsletterJid, JidProvider admin) {
        inviteNewsletterAdmin(newsletterJid, null, admin);
    }

    /**
     * Sends an invitation to the jid provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param inviteCaption the nullable caption of the invitation
     * @param admin        the new admin
     */
    public Optional<ChatMessageInfo> inviteNewsletterAdmin(JidProvider newsletterJid, String inviteCaption, JidProvider admin) {
        var request = NewsletterRequests.createAdminInviteNewsletter(newsletterJid.toJid(), admin.toJid());
        var expirationTimestamp = socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6826078034173770"), request))
                .findChild("result")
                .flatMap(Node::contentAsBytes)
                .flatMap(CreateAdminInviteNewsletterResponse::ofJson)
                .map(CreateAdminInviteNewsletterResponse::expirationTime)
                .orElse(null);
        if(expirationTimestamp == null) {
            return Optional.empty();
        }

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
        return Optional.of(sendChatMessage(admin, MessageContainer.of(message)));
    }

    /**
     * Revokes an invitation to become an admin in a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param admin         the non-null user that received the invite previously
     * @return a CompletableFuture
     */
    public boolean revokeNewsletterAdminInvite(JidProvider newsletterJid, JidProvider admin) {
        var request = NewsletterRequests.revokeAdminInviteNewsletter(newsletterJid.toJid(), admin.toJid());
        return socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "6111171595650958"), request))
                .findChild("result")
                .flatMap(Node::contentAsBytes)
                .flatMap(RevokeAdminInviteNewsletterResponse::ofJson)
                .map(RevokeAdminInviteNewsletterResponse::jid)
                .isPresent();
    }

    /**
     * Accepts an invitation to become an admin in a newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @return a CompletableFuture
     */
    public boolean acceptNewsletterAdminInvite(JidProvider newsletterJid) {
        var request = NewsletterRequests.acceptAdminInviteNewsletter(newsletterJid.toJid());
        var resultNode = socketConnection.sendQuery("get", "w:mex", Node.of("query", Map.of("query_id", "7292354640794756"), request));
        var result = resultNode.findChild("result");
        if(result.isEmpty()) {
            return false;
        }

        var content = result.get().contentAsBytes();
        if(content.isEmpty()) {
            return false;
        }

        var jid = AcceptAdminInviteNewsletterResponse.ofJson(content.get())
                .map(AcceptAdminInviteNewsletterResponse::jid);
        if(jid.isEmpty()) {
            return false;
        }

        var newsletter = queryNewsletter(jid.get(), NewsletterViewerRole.ADMIN);
        if (newsletter.isEmpty()) {
            return false;
        }

        store().addNewsletter(newsletter.get());
        return true;
    }

    /**
     * Queries a newsletter
     *
     * @param newsletterJid the non-null jid of the newsletter
     * @param role          the non-null role of the user executing the query
     */
    public Optional<Newsletter> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        return socketConnection.queryNewsletter(newsletterJid, role);
    }
    //</editor-fold>  

    // <editor-fold desc="Mobile only methods">
    /**
     * Syncs any number of contacts with whatsapp
     *
     * @param contacts the contacts to sync
     * @return the contacts that were successfully synced
     */
    public Collection<Jid> addContacts(JidProvider... contacts) {
        // TODO: Verify if this works on web as well

        var users = Arrays.stream(contacts)
                .filter(entry -> entry.toJid().hasServer(JidServer.user()) && !store().hasContact(entry))
                .map(contact -> contact.toJid().toPhoneNumber())
                .flatMap(Optional::stream)
                .map(phoneNumber -> Node.of("user", Node.of("contact", phoneNumber.getBytes())))
                .toList();
        if(users.isEmpty()) {
            return List.of();
        }

        var sync = Node.of(
                "usync",
                Map.of(
                        "context", "add",
                        "index", "0",
                        "last", "true",
                        "mode", "delta",
                        "sid", SocketConnection.randomSid()
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
        return socketConnection.sendQuery(store().jid().orElseThrow(), "get", "usync", sync)
                .findChild("usync")
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

    /**
     * Enables two-factor authentication
     * Mobile API only
     *
     * @param code the six digits non-null numeric code
     */
    public void enable2fa(String code) {
        set2fa(code, null);
    }

    /**
     * Enables two-factor authentication
     * Mobile API only
     *
     * @param code  the six digits non-null numeric code
     * @param email the nullable recovery email
     */
    public boolean enable2fa(String code, String email) {
        return set2fa(code, email);
    }

    /**
     * Disables two-factor authentication
     * Mobile API only
     *
     */
    public boolean disable2fa() {
        return set2fa(null, null);
    }

    private boolean set2fa(String code, String email) {
        if (store().clientType() != WhatsappClientType.MOBILE) {
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

        var result = socketConnection.sendQuery("set", "urn:xmpp:whatsapp:account", Node.of("2fa", body));
        return !result.hasNode("error");
    }

    /**
     * Starts a call with a contact
     * Mobile API only
     *
     * @param contact the non-null contact
     * @param video whether it's a video call or an audio call
     */
    public Call startCall(JidProvider contact, boolean video) {
        if (store().clientType() != WhatsappClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        addContacts(contact);
        socketConnection.querySessions(List.of(contact.toJid()));
        return sendCallMessage(contact, video);
    }

    private Call sendCallMessage(JidProvider jid, boolean video) {
        var callId = ChatMessageKey.randomId(store().clientType());
        var description = video ? "video" : "audio";
        var audioStream = Node.of(description, Map.of("rate", 8000, "enc", "opus"));
        var audioStreamTwo = Node.of(description, Map.of("rate", 16000, "enc", "opus"));
        var net = Node.of("net", Map.of("medium", 3));
        var encopt = Node.of("encopt", Map.of("keygen", 2));
        var enc = socketConnection.createCall(jid);
        var capability = Node.of("capability", Map.of("ver", 1), HexFormat.of().parseHex("0104ff09c4fa"));
        var callCreator = "%s:0@s.whatsapp.net".formatted(jidOrThrowError().user());
        var offer = Node.of("offer",
                Map.of("call-creator", callCreator, "call-id", callId),
                audioStream, audioStreamTwo, net, capability, encopt, enc);
        var result = socketConnection.sendNode(Node.of("call", Map.of("to", jid.toJid()), offer));
        return onCallSent(jid, callId, result);
    }

    private Call onCallSent(JidProvider jid, String callId, Node result) {
        var call = new CallBuilder()
                .chatJid(jid.toJid())
                .callerJid(jidOrThrowError())
                .id(callId)
                .timestampSeconds(Clock.nowSeconds())
                .video(false)
                .status(CallStatus.RINGING)
                .offline(false)
                .build();
        store().addCall(call);
        socketConnection.onCall(call);
        return call;
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param callId the non-null id of the call to reject
     */
    public boolean stopCall(String callId) {
        if (store().clientType() != WhatsappClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        return store().findCallById(callId)
                .map(this::stopCall)
                .orElse(false); // Changed to return boolean directly
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param call the non-null call to reject
     */
    public boolean stopCall(Call call) {
        if (store().clientType() != WhatsappClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        if (Objects.equals(call.callerJid().user(), jidOrThrowError().user())) {
            var rejectNode = Node.of("terminate", Map.of("reason", "timeout", "call-id", call.id(), "call-creator", call.callerJid()));
            var body = Node.of("call", Map.of("to", call.chatJid()), rejectNode);
            var result = socketConnection.sendNode(body);
            return !result.hasNode("error");
        }

        var rejectNode = Node.of("reject", Map.of("call-id", call.id(), "call-creator", call.callerJid(), "count", 0));
        var body = Node.of("call", Map.of("from", jidOrThrowError(), "to", call.callerJid()), rejectNode);
        var result = socketConnection.sendNode(body);
        return !result.hasNode("error");
    }
    //</editor-fold>  

    //<editor-fold desc="Listeners">
    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public Whatsapp addListener(WhatsappListener listener) {
        store().addListener(listener);
        return this;
    }

    /**
     * Unregisters a listener
     *
     * @param listener the listener to unregister
     * @return the same instance
     */
    public Whatsapp removeListener(WhatsappListener listener) {
        store().removeListener(listener);
        return this;
    }

    // Start of generated code from it.auties.whatsapp.routine.GenerateListenersLambda
    public Whatsapp addContactsListener(WhatsappFunctionalListener.Unary<Collection<Contact>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContacts(Collection<Contact> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addContactsListener(WhatsappFunctionalListener.Binary<Whatsapp, Collection<Contact>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContacts(Whatsapp arg0, Collection<Contact> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addChatsListener(WhatsappFunctionalListener.Unary<Collection<Chat>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onChats(Collection<Chat> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addChatsListener(WhatsappFunctionalListener.Binary<Whatsapp, Collection<Chat>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onChats(Whatsapp arg0, Collection<Chat> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNodeSentListener(WhatsappFunctionalListener.Binary<Whatsapp, Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNodeSent(Whatsapp arg0, Node arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNodeSentListener(WhatsappFunctionalListener.Unary<Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNodeSent(Node arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addLoggedInListener(WhatsappFunctionalListener.Unary<Whatsapp> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLoggedIn(Whatsapp arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addLoggedInListener(WhatsappFunctionalListener.Empty consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLoggedIn() {
                consumer.accept();
            }
        });
        return this;
    }

    public Whatsapp addNewMessageListener(WhatsappFunctionalListener.Binary<Whatsapp, MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(Whatsapp arg0, MessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNewMessageListener(WhatsappFunctionalListener.Unary<MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(MessageInfo arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addLinkedDevicesListener(WhatsappFunctionalListener.Binary<Whatsapp, Collection<Jid>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLinkedDevices(Whatsapp arg0, Collection<Jid> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addLinkedDevicesListener(WhatsappFunctionalListener.Unary<Collection<Jid>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLinkedDevices(Collection<Jid> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addMessageReplyListener(WhatsappFunctionalListener.Ternary<Whatsapp, MessageInfo, QuotedMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageReply(Whatsapp arg0, MessageInfo arg1, QuotedMessageInfo arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addMessageReplyListener(WhatsappFunctionalListener.Binary<MessageInfo, QuotedMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageReply(MessageInfo arg0, QuotedMessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncMessagesListener(WhatsappFunctionalListener.Ternary<Whatsapp, Chat, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncMessages(Whatsapp arg0, Chat arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncMessagesListener(WhatsappFunctionalListener.Binary<Chat, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncMessages(Chat arg0, boolean arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addMessageStatusListener(WhatsappFunctionalListener.Binary<Whatsapp, MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageStatus(Whatsapp arg0, MessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addMessageStatusListener(WhatsappFunctionalListener.Unary<MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageStatus(MessageInfo arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addContactPresenceListener(WhatsappFunctionalListener.Ternary<Whatsapp, Chat, JidProvider> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContactPresence(Whatsapp arg0, Chat arg1, JidProvider arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addContactPresenceListener(WhatsappFunctionalListener.Binary<Chat, JidProvider> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContactPresence(Chat arg0, JidProvider arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNewslettersListener(WhatsappFunctionalListener.Unary<Collection<Newsletter>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewsletters(Collection<Newsletter> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addNewslettersListener(WhatsappFunctionalListener.Binary<Whatsapp, Collection<Newsletter>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewsletters(Whatsapp arg0, Collection<Newsletter> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addProfilePictureChangedListener(WhatsappFunctionalListener.Binary<Whatsapp, JidProvider> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onProfilePictureChanged(Whatsapp arg0, JidProvider arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addProfilePictureChangedListener(WhatsappFunctionalListener.Unary<JidProvider> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onProfilePictureChanged(JidProvider arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addLocaleChangedListener(WhatsappFunctionalListener.Binary<CountryLocale, CountryLocale> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLocaleChanged(CountryLocale arg0, CountryLocale arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addLocaleChangedListener(WhatsappFunctionalListener.Ternary<Whatsapp, CountryLocale, CountryLocale> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onLocaleChanged(Whatsapp arg0, CountryLocale arg1, CountryLocale arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addNewContactListener(WhatsappFunctionalListener.Binary<Whatsapp, Contact> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewContact(Whatsapp arg0, Contact arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNewContactListener(WhatsappFunctionalListener.Unary<Contact> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewContact(Contact arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncProgressListener(WhatsappFunctionalListener.Binary<Integer, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncProgress(int arg0, boolean arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncProgressListener(WhatsappFunctionalListener.Ternary<Whatsapp, Integer, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncProgress(Whatsapp arg0, int arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addNewStatusListener(WhatsappFunctionalListener.Binary<Whatsapp, ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewStatus(Whatsapp arg0, ChatMessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNewStatusListener(WhatsappFunctionalListener.Unary<ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNewStatus(ChatMessageInfo arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addRegistrationCodeListener(WhatsappFunctionalListener.Unary<Long> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onRegistrationCode(long arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addRegistrationCodeListener(WhatsappFunctionalListener.Binary<Whatsapp, Long> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onRegistrationCode(Whatsapp arg0, long arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNameChangedListener(WhatsappFunctionalListener.Ternary<Whatsapp, String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNameChanged(Whatsapp arg0, String arg1, String arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addNameChangedListener(WhatsappFunctionalListener.Binary<String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNameChanged(String arg0, String arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addContactBlockedListener(WhatsappFunctionalListener.Binary<Whatsapp, Contact> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContactBlocked(Whatsapp arg0, Contact arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addContactBlockedListener(WhatsappFunctionalListener.Unary<Contact> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onContactBlocked(Contact arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addAboutChangedListener(WhatsappFunctionalListener.Binary<String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onAboutChanged(String arg0, String arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addAboutChangedListener(WhatsappFunctionalListener.Ternary<Whatsapp, String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onAboutChanged(Whatsapp arg0, String arg1, String arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addPrivacySettingChangedListener(WhatsappFunctionalListener.Ternary<Whatsapp, PrivacySettingEntry, PrivacySettingEntry> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onPrivacySettingChanged(Whatsapp arg0, PrivacySettingEntry arg1, PrivacySettingEntry arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addPrivacySettingChangedListener(WhatsappFunctionalListener.Binary<PrivacySettingEntry, PrivacySettingEntry> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onPrivacySettingChanged(PrivacySettingEntry arg0, PrivacySettingEntry arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebAppPrimaryFeaturesListener(WhatsappFunctionalListener.Binary<Whatsapp, List<String>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppPrimaryFeatures(Whatsapp arg0, List<String> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebAppPrimaryFeaturesListener(WhatsappFunctionalListener.Unary<Collection<String>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppPrimaryFeatures(Collection<String> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addMessageDeletedListener(WhatsappFunctionalListener.Ternary<Whatsapp, MessageInfo, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageDeleted(Whatsapp arg0, MessageInfo arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addMessageDeletedListener(WhatsappFunctionalListener.Binary<MessageInfo, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onMessageDeleted(MessageInfo arg0, boolean arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addStatusListener(WhatsappFunctionalListener.Unary<Collection<ChatMessageInfo>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onStatus(Collection<ChatMessageInfo> arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addStatusListener(WhatsappFunctionalListener.Binary<Whatsapp, Collection<ChatMessageInfo>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onStatus(Whatsapp arg0, Collection<ChatMessageInfo> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addCallListener(WhatsappFunctionalListener.Unary<Call> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onCall(Call arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addCallListener(WhatsappFunctionalListener.Binary<Whatsapp, Call> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onCall(Whatsapp arg0, Call arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebAppStateSettingListener(WhatsappFunctionalListener.Unary<Setting> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppStateSetting(Setting arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addWebAppStateSettingListener(WhatsappFunctionalListener.Binary<Whatsapp, Setting> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppStateSetting(Whatsapp arg0, Setting arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addNodeReceivedListener(WhatsappFunctionalListener.Unary<Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNodeReceived(Node arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addNodeReceivedListener(WhatsappFunctionalListener.Binary<Whatsapp, Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onNodeReceived(Whatsapp arg0, Node arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addDisconnectedListener(WhatsappFunctionalListener.Binary<Whatsapp, WhatsappDisconnectReason> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onDisconnected(Whatsapp arg0, WhatsappDisconnectReason arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addDisconnectedListener(WhatsappFunctionalListener.Unary<WhatsappDisconnectReason> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onDisconnected(WhatsappDisconnectReason arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public Whatsapp addWebAppStateActionListener(WhatsappFunctionalListener.Binary<Action, MessageIndexInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppStateAction(Action arg0, MessageIndexInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebAppStateActionListener(WhatsappFunctionalListener.Ternary<Whatsapp, Action, MessageIndexInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebAppStateAction(Whatsapp arg0, Action arg1, MessageIndexInfo arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncPastParticipantsListener(WhatsappFunctionalListener.Binary<Jid, Collection<ChatPastParticipant>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncPastParticipants(Jid arg0, Collection<ChatPastParticipant> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public Whatsapp addWebHistorySyncPastParticipantsListener(WhatsappFunctionalListener.Ternary<Whatsapp, Jid, Collection<ChatPastParticipant>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsappListener() {
            @Override
            public void onWebHistorySyncPastParticipants(Whatsapp arg0, Jid arg1, Collection<ChatPastParticipant> arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }
    // End of generated code from it.auties.whatsapp.routine.GenerateListenersLambda

    public Whatsapp addMessageReplyListener(ChatMessageInfo info, Consumer<MessageInfo> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public Whatsapp addMessageReplyListener(ChatMessageInfo info, BiConsumer<Whatsapp, MessageInfo> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public Whatsapp addMessageReplyListener(String id, Consumer<MessageInfo> consumer) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(MessageInfo info) {
                var quotedMessageId = info.quotedMessage()
                        .map(QuotedMessageInfo::id)
                        .orElse(null);
                if (id.equals(quotedMessageId)) {
                    consumer.accept(info);
                }
            }
        });
    }

    public Whatsapp addMessageReplyListener(String id, BiConsumer<Whatsapp, MessageInfo> consumer) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
                var quotedMessageId = info.quotedMessage()
                        .map(QuotedMessageInfo::id)
                        .orElse(null);
                if (id.equals(quotedMessageId)) {
                    consumer.accept(whatsapp, info);
                }
            }
        });
    }

    public Whatsapp addNewChatMessageListener(WhatsappFunctionalListener.Unary<ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(MessageInfo info) {
                if(info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(chatMessageInfo);
                }
            }
        });
    }

    public Whatsapp addNewChatMessageListener(WhatsappFunctionalListener.Binary<Whatsapp, ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
                if(info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(whatsapp, chatMessageInfo);
                }
            }
        });
    }

    public Whatsapp addNewNewsletterMessageListener(WhatsappFunctionalListener.Unary<NewsletterMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(MessageInfo info) {
                if(info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(newsletterMessageInfo);
                }
            }
        });
    }

    public Whatsapp addNewNewsletterMessageListener(WhatsappFunctionalListener.Binary<Whatsapp, NewsletterMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsappListener() {
            @Override
            public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
                if(info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(whatsapp, newsletterMessageInfo);
                }
            }
        });
    }
    //</editor-fold>
}
