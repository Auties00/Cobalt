package com.github.auties00.cobalt.client;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.message.MessageReceiverService;
import com.github.auties00.cobalt.message.MessageSenderService;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.model.action.*;
import com.github.auties00.cobalt.model.auth.*;
import com.github.auties00.cobalt.model.business.*;
import com.github.auties00.cobalt.model.call.Call;
import com.github.auties00.cobalt.model.call.CallBuilder;
import com.github.auties00.cobalt.model.call.CallStatus;
import com.github.auties00.cobalt.model.chat.*;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.model.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidProvider;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.media.MediaProvider;
import com.github.auties00.cobalt.model.message.model.*;
import com.github.auties00.cobalt.model.message.server.ProtocolMessage;
import com.github.auties00.cobalt.model.message.server.ProtocolMessageBuilder;
import com.github.auties00.cobalt.model.message.standard.NewsletterAdminInviteMessageBuilder;
import com.github.auties00.cobalt.model.message.standard.ReactionMessageBuilder;
import com.github.auties00.cobalt.model.message.standard.TextMessage;
import com.github.auties00.cobalt.model.newsletter.*;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntry;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntryBuilder;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.model.setting.Setting;
import com.github.auties00.cobalt.model.sync.*;
import com.github.auties00.cobalt.model.sync.RecordSync.Operation;
import com.github.auties00.cobalt.node.*;
import com.github.auties00.cobalt.node.mex.json.request.CommunityRequests;
import com.github.auties00.cobalt.node.mex.json.request.NewsletterRequests;
import com.github.auties00.cobalt.node.mex.json.request.UserRequests;
import com.github.auties00.cobalt.node.mex.json.response.*;
import com.github.auties00.cobalt.socket.SocketRequest;
import com.github.auties00.cobalt.socket.SocketSession;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.store.WhatsAppStore;
import com.github.auties00.cobalt.sync.WebAppStateService;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.cobalt.util.MetaBots;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.key.SignalPreKeyPair;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.*;
import static com.github.auties00.cobalt.model.contact.ContactStatus.*;

/**
 * A class used to interface a user to Whatsapp
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class WhatsAppClient {
    private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEB_VERSION = new byte[]{6, NodeTokens.DICTIONARY_VERSION};
    private static final byte[] WEB_PROLOGUE = SecureBytes.concat(WHATSAPP_VERSION_HEADER, WEB_VERSION);
    private static final byte[] MOBILE_VERSION = new byte[]{5, NodeTokens.DICTIONARY_VERSION};
    private static final byte[] MOBILE_PROLOGUE = SecureBytes.concat(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);

    private static final byte[] SIGNAL_KEY_TYPE = {SignalIdentityPublicKey.type()};

    private static final int PROFILE_PIC_SIZE = 64;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(\\S+)$");

    private static final long MIN_PRE_KEYS_COUNT = 5;

    private final WhatsAppStore store;
    private final WhatsAppClientErrorHandler errorHandler;
    private final WhatsAppClientMessagePreviewHandler messagePreviewHandler;

    private final WebAppStateService webAppStateService;
    private final DeviceService deviceService;
    private final LidMigrationService lidMigrationService;
    private final MessageSenderService messageSenderService;
    private final MessageReceiverService messageReceiverService;

    private SocketSession socketSession;
    private final SocketStream socketStream;
    private final ConcurrentMap<String, SocketRequest> pendingSocketRequests;
    private Thread shutdownHook;

    WhatsAppClient(WhatsAppStore store, WhatsAppClientVerificationHandler.Web webVerificationHandler, WhatsAppClientMessagePreviewHandler messagePreviewHandler, WhatsAppClientErrorHandler errorHandler) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler cannot be null");
        if ((store.clientType() == WhatsAppClientType.WEB) == (webVerificationHandler == null)) {
            throw new IllegalArgumentException("webVerificationHandler cannot be null when client type is WEB");
        }
        var sessionCipher = new SignalSessionCipher(store);
        var groupCipher = new SignalGroupCipher(store);
        this.webAppStateService = new WebAppStateService(this);
        this.deviceService = new DeviceService(this, sessionCipher, groupCipher);
        this.lidMigrationService = new LidMigrationService(this);
        this.messageSenderService = new MessageSenderService(this, deviceService, sessionCipher, groupCipher);
        this.messageReceiverService = new MessageReceiverService(this, deviceService, sessionCipher, groupCipher);
        this.pendingSocketRequests = new ConcurrentHashMap<>();
        this.socketStream = new SocketStream(this, deviceService, messageReceiverService, lidMigrationService, webVerificationHandler);
        this.messagePreviewHandler = messagePreviewHandler;
    }

    /**
     * Creates a new builder
     *
     * @return a builder
     */
    public static WhatsAppClientBuilder builder() {
        return WhatsAppClientBuilder.INSTANCE;
    }

    //<editor-fold desc="Data">

    /**
     * Returns the store associated with this session
     *
     * @return a non-null WhatsappStore
     */
    public WhatsAppStore store() {
        return store;
    }

    public WhatsAppClientMessagePreviewHandler messagePreviewHandler() {
        return messagePreviewHandler;
    }

    //</editor-fold>

    //<editor-fold desc="Connection">

    /**
     * Connects to Whatsapp
     */
    public WhatsAppClient connect() {
        connect(null);
        return this;
    }

    private void connect(WhatsAppClientDisconnectReason reason) {
        if (isConnected()) {
            return;
        }

        try {
            var noiseKeyPair = store.noiseKeyPair();
            var handshakePrologue = switch (store.clientType()) {
                case WEB -> WEB_PROLOGUE;
                case MOBILE -> MOBILE_PROLOGUE;
            };
            var handshakePayload = createUserClientPayload();
            var proxy = store.proxy().orElse(null);
            this.socketSession = SocketSession.of(noiseKeyPair, handshakePrologue, handshakePayload, proxy);
            socketSession.connect(this::onMessage);
        } catch (Throwable throwable) {
            if (reason == WhatsAppClientDisconnectReason.RECONNECTING) {
                handleFailure(RECONNECT, throwable);
            } else {
                handleFailure(AUTH, throwable);
            }
            return;
        }

        if (shutdownHook == null) {
            this.shutdownHook = Thread.ofPlatform()
                    .name("CobaltShutdownHandler")
                    .unstarted(() -> disconnect(WhatsAppClientDisconnectReason.DISCONNECTED, false));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    private ClientPayload createUserClientPayload() {
        var agent = createUserAgent();
        return switch (store.clientType()) {
            case MOBILE -> {
                var phoneNumber = store
                        .phoneNumber()
                        .orElseThrow(() -> new InternalError("Phone number was not set"));
                yield new ClientPayloadBuilder()
                        .username(phoneNumber)
                        .passive(false)
                        .pushName(store.registered() ? store.name() : null)
                        .userAgent(agent)
                        .shortConnect(true)
                        .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                        .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                        .connectAttemptCount(0)
                        .device(0)
                        .oc(false)
                        .build();
            }
            case WEB -> {
                var jid = store.jid();
                if (jid.isPresent()) {
                    yield new ClientPayloadBuilder()
                            .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                            .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                            .userAgent(agent)
                            .username(Long.parseLong(jid.get().user()))
                            .passive(true)
                            .pull(true)
                            .device(jid.get().device())
                            .build();
                } else {
                    yield new ClientPayloadBuilder()
                            .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                            .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                            .userAgent(agent)
                            .regData(createRegisterData())
                            .passive(false)
                            .pull(false)
                            .build();
                }
            }
        };
    }

    private UserAgent createUserAgent() {
        var mobile = store.clientType() == WhatsAppClientType.MOBILE;
        return new UserAgentBuilder()
                .platform(store.device().platform())
                .appVersion(store.clientVersion())
                .mcc("000")
                .mnc("000")
                .osVersion(mobile ? store.device().osVersion().toString() : null)
                .manufacturer(mobile ? store.device().manufacturer() : null)
                .device(mobile ? store.device().model().replaceAll("_", " ") : null)
                .osBuildNumber(mobile ? store.device().osBuildNumber() : null)
                .phoneId(mobile ? store.fdid().toString().toUpperCase() : null)
                .releaseChannel(store.releaseChannel())
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("US")
                .deviceType(UserAgent.DeviceType.PHONE)
                .deviceModelType(store.device().modelId())
                .build();
    }

    private CompanionRegistrationData createRegisterData() {
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(store.clientVersion().toHash())
                .eRegid(SecureBytes.intToBytes(store.registrationId(), 4))
                .eKeytype(SecureBytes.intToBytes(SignalIdentityPublicKey.type(), 1))
                .eIdent(store.identityKeyPair().publicKey().toEncodedPoint())
                .eSkeyId(SecureBytes.intToBytes(store.signedKeyPair().id(), 3))
                .eSkeyVal(store.signedKeyPair().publicKey().toEncodedPoint())
                .eSkeySig(store.signedKeyPair().signature());
        if (store.clientType() == WhatsAppClientType.WEB) {
            var props = createCompanionProps();
            var encodedProps = props == null ? null : CompanionPropertiesSpec.encode(props);
            companion.companionProps(encodedProps);
        }

        return companion.build();
    }

    private CompanionProperties createCompanionProps() {
        return switch (store.clientType()) {
            case WEB -> {
                var historyLength = store.webHistoryPolicy()
                        .orElse(WhatsAppWebClientHistory.standard(true));
                var config = new HistorySyncConfigBuilder()
                        .inlineInitialPayloadInE2EeMsg(true)
                        .supportBotUserAgentChatHistory(true)
                        .supportCallLogHistory(true)
                        .storageQuotaMb(historyLength.size())
                        .fullSyncSizeMbLimit(historyLength.size())
                        .build();
                var platformType = switch (store.device().platform()) {
                    case IOS, IOS_BUSINESS -> CompanionProperties.PlatformType.IOS_PHONE;
                    case ANDROID, ANDROID_BUSINESS -> CompanionProperties.PlatformType.ANDROID_PHONE;
                    case WINDOWS -> CompanionProperties.PlatformType.UWP;
                    case MACOS -> CompanionProperties.PlatformType.IOS_CATALYST;
                };
                yield new CompanionPropertiesBuilder()
                        .os(store.name())
                        .platformType(platformType)
                        .requireFullSync(historyLength.isExtended())
                        .historySyncConfig(config)
                        .version(store.clientVersion())
                        .build();
            }
            case MOBILE -> null;
        };
    }


    public void disconnect(WhatsAppClientDisconnectReason reason) {
        disconnect(reason, true);
    }

    private void disconnect(WhatsAppClientDisconnectReason reason, boolean canRemoveShutdownHook) {
        if (!isConnected()) {
            return;
        }

        if (socketSession != null) {
            socketSession.disconnect();
        }

        pendingSocketRequests.forEach((ignored, request) -> request.complete(null));
        pendingSocketRequests.clear();

        if (reason == WhatsAppClientDisconnectReason.LOGGED_OUT || reason == WhatsAppClientDisconnectReason.BANNED) {
            store.setSerializable(false);
            var serializer = store.serializer();
            serializer.deleteSession(store.clientType(), store.uuid());
        } else {
            store.serialize();
        }

        lidMigrationService.reset();
        socketStream.reset();
        webAppStateService.reset();

        if (reason != WhatsAppClientDisconnectReason.RECONNECTING && shutdownHook != null && canRemoveShutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }

        for (var listener : store.listeners()) {
            listener.onDisconnected(this, reason);
        }

        if (reason == WhatsAppClientDisconnectReason.RECONNECTING) {
            connect(reason);
        }
    }

    private void onMessage(ByteBuffer message) {
        try (var decoder = new NodeDecoder(message)) {
            while (decoder.hasData()) {
                var node = decoder.decode();
                for (var listener : store.listeners()) {
                    Thread.startVirtualThread(() -> listener.onNodeReceived(this, node));
                }
                resolvePendingRequest(node);
                socketStream.digest(node);
            }
        } catch (Throwable throwable) {
            handleFailure(STREAM, throwable);
        }
    }

    public void resolvePendingRequest(Node node) {
        var id = node.getAttribute("id")
                .map(NodeAttribute::toString)
                .orElse(null);
        if (id == null) {
            return;
        }

        var request = pendingSocketRequests.remove(id);
        if (request != null) {
            request.complete(node);
        }
    }

    public void sendNodeWithNoResponse(Node node) {
        socketSession.sendNode(node);
        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> listener.onNodeSent(this, node));
        }
    }

    public Node sendNode(NodeBuilder node) {
        return sendNode(node, null);
    }

    public Node sendNode(NodeBuilder node, Function<Node, Boolean> filter) {
        if (!node.hasAttribute("id")) {
            node.attribute("id", SecureBytes.randomHex(10));
        }

        var outgoing = node.build();
        var outgoingId = outgoing.getRequiredAttribute("id")
                .toString();
        socketSession.sendNode(outgoing);

        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> listener.onNodeSent(this, outgoing));
        }

        var request = new SocketRequest(outgoing, filter);
        pendingSocketRequests.put(outgoingId, request);
        return request.waitForResponse();
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket if a previous connection exists
     *
     */
    public void disconnect() {
        disconnect(WhatsAppClientDisconnectReason.DISCONNECTED);
    }

    /**
     * Disconnects and reconnects to Whatsapp Web's WebSocket if a previous connection exists
     *
     */
    public void reconnect() {
        disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
    }

    /**
     * Disconnects from Whatsapp Web's WebSocket and logs out of WhatsappWeb invalidating the previous
     * saved credentials. The next time the API is used, the QR code will need to be scanned again.
     */
    public void logout() {
        var localJid = store.jid();
        if (localJid.isEmpty()) {
            disconnect(WhatsAppClientDisconnectReason.LOGGED_OUT);
        } else {
            var device = new NodeBuilder()
                    .description("remove-companion-device")
                    .attribute("value", localJid.get())
                    .attribute("reason", "user_initiated")
                    .build();
            var iqNode = new NodeBuilder()
                    .description("iq")
                    .attribute("xmlns", "md")
                    .attribute("to", JidServer.user())
                    .attribute("type", "set")
                    .content(device);
            sendNode(iqNode);
        }
    }

    /**
     * Returns whether the connection is active or not
     *
     * @return a boolean
     */
    public boolean isConnected() {
        return socketSession != null && socketSession.isConnected();
    }

    /**
     * Waits for this session to be disconnected
     */
    public WhatsAppClient waitForDisconnection() {
        if (!isConnected()) {
            return this;
        }

        var future = new CompletableFuture<Void>();
        addDisconnectedListener((_, reason) -> {
            if (reason != WhatsAppClientDisconnectReason.RECONNECTING) {
                future.complete(null);
            }
        });
        future.join();
        return this;
    }

    //</editor-fold>

    //<editor-fold desc="Error handling">

    public void handleFailure(WhatsAppClientErrorHandler.Location location, Throwable throwable) {
        var result = errorHandler.handleError(this, location, throwable);
        switch (result) {
            case LOG_OUT -> disconnect(WhatsAppClientDisconnectReason.LOGGED_OUT);
            case DISCONNECT -> disconnect(WhatsAppClientDisconnectReason.DISCONNECTED);
            case RECONNECT -> disconnect(WhatsAppClientDisconnectReason.RECONNECTING);
        }
    }

    //</editor-fold>

    //<editor-fold desc="Account">

    /**
     * Queries a business profile, if available
     *
     * @param contact the target contact
     * @return a CompletableFuture
     */
    public Optional<BusinessProfile> queryBusinessProfile(JidProvider contact) {
        var profileNode = new NodeBuilder()
                .description("profile")
                .attribute("value", contact)
                .build();
        var businessProfileNode = new NodeBuilder()
                .description("business_profile")
                .attribute("v", 116)
                .content(profileNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(businessProfileNode);
        var result = sendNode(iqNode);
        return result.getChild("business_profile")
                .flatMap(entry -> entry.getChild("profile"))
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
     * Executes a query to determine whether any value of users have an account on Whatsapp
     *
     * @param contacts the contacts to check
     * @return a CompletableFuture that wraps a non-null map
     */
    public Set<Jid> hasWhatsapp(JidProvider... contacts) {
        if (contacts == null) {
            return Set.of();
        }

        var contactNodes = Arrays.stream(contacts)
                .map(this::createUserNode)
                .flatMap(Optional::stream)
                .toList();
        if (contactNodes.isEmpty()) {
            return Set.of();
        }

        var queryContact = new NodeBuilder()
                .description("contact")
                .build();

        var queryNode = new NodeBuilder()
                .description("query")
                .content(queryContact)
                .build();
        var listNode = new NodeBuilder()
                .description("list")
                .content(contactNodes)
                .build();
        var sideListNode = new NodeBuilder()
                .description("side_list")
                .build();
        var syncNode = new NodeBuilder()
                .description("usync")
                .attribute("sid", SecureBytes.randomSid())
                .attribute("mode", "query")
                .attribute("last", "true")
                .attribute("index", "0")
                .attribute("context", "interactive")
                .content(queryNode, listNode, sideListNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "usync")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(syncNode);
        var result = sendNode(iqNode);

        return result.streamChildren("usync")
                .flatMap(node -> node.streamChild("list"))
                .flatMap(node -> node.streamChildren("user"))
                .filter(this::hasWhatsapp)
                .map(node -> node.getRequiredAttributeAsJid("jid"))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Optional<Node> createUserNode(JidProvider provider) {
        if (provider == null) {
            return Optional.empty();
        }

        var user = provider.toJid();
        if (!user.hasServer(JidServer.user())) {
            return Optional.empty();
        }

        var phoneNumber = user.toPhoneNumber();
        if (phoneNumber.isEmpty()) {
            return Optional.empty();
        }

        var contactNode = new NodeBuilder()
                .description("contact")
                .content(phoneNumber.get())
                .build();
        var userNode = new NodeBuilder()
                .description("user")
                .content(contactNode)
                .build();
        return Optional.of(userNode);
    }

    private boolean hasWhatsapp(Node node) {
        return node.getChild("contact")
                .orElseThrow(() -> new NoSuchElementException("Missing contact"))
                .getRequiredAttribute("type")
                .toString()
                .equals("in");
    }

    /**
     * Queries the block list
     *
     * @return a CompletableFuture
     */
    public Collection<Jid> queryBlockList() {
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "blocklist")
                .attribute("to", JidServer.user())
                .attribute("type", "get");
        var result = sendNode(iqNode);
        return result.streamChild("list")
                .flatMap(node -> node.streamChildren("item"))
                .flatMap(item -> item.streamAttributeAsJid("jid"))
                .toList();
    }

    /**
     * Queries the display name of a contact
     *
     * @param contactJid the non-null contact
     * @return a CompletableFuture
     */
    public Optional<String> queryName(JidProvider contactJid) {
        return store.findContactByJid(contactJid)
                .flatMap(Contact::chosenName)
                .or(() -> {
                    var request = UserRequests.chosenName(contactJid.toJid().user());
                    var queryNode = new NodeBuilder()
                            .description("query")
                            .attribute("query_id", "6556393721124826")
                            .content(request)
                            .build();
                    var iqNode = new NodeBuilder()
                            .description("iq")
                            .attribute("xmlns", "w:mex")
                            .attribute("to", JidServer.user())
                            .attribute("type", "get")
                            .content(queryNode);
                    return sendNode(iqNode)
                            .getChild("result")
                            .flatMap(Node::toContentBytes)
                            .flatMap(UserChosenNameResponse::ofJson)
                            .flatMap(UserChosenNameResponse::name);
                });
    }

    /**
     * Queries the written whatsapp status of a Contact
     *
     * @param chat the target contact
     * @return a CompletableFuture that wraps an optional contact status newsletters
     */
    public Optional<String> queryAbout(JidProvider chat) {
        var statusNode = new NodeBuilder()
                .description("status")
                .build();
        var bodyNode = new NodeBuilder()
                .description("user")
                .attribute("jid", chat)
                .build();

        var queryNode = new NodeBuilder()
                .description("query")
                .content(statusNode)
                .build();
        var listNode = new NodeBuilder()
                .description("list")
                .content(bodyNode)
                .build();
        var sideListNode = new NodeBuilder()
                .description("side_list")
                .build();
        var syncNode = new NodeBuilder()
                .description("usync")
                .attribute("sid", SecureBytes.randomSid())
                .attribute("mode", "query")
                .attribute("last", "true")
                .attribute("index", "0")
                .attribute("context", "interactive")
                .content(queryNode, listNode, sideListNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "usync")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(syncNode);
        var result = sendNode(iqNode);

        return result.streamChildren("usync")
                .flatMap(node -> node.streamChild("list"))
                .flatMap(node -> node.streamChildren("user"))
                .flatMap(entry -> entry.streamChild("status"))
                .findFirst()
                .flatMap(Node::toContentString);
    }

    /**
     * Queries the profile picture
     *
     * @param chat the chat of the chat to query
     * @return a CompletableFuture that wraps nullable jpg url hosted on Whatsapp's servers
     */
    public Optional<URI> queryPicture(JidProvider chat) {
        var pictureNode = new NodeBuilder()
                .description("picture")
                .attribute("query", "url")
                .attribute("type", "image")
                .build();
        var community = chat.toJid().hasServer(JidServer.groupOrCommunity())
                        && queryGroupOrCommunityMetadata(chat.toJid()).isCommunity();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:profile:picture")
                .attribute(community ? "parent_group_jid" : "target", chat)
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(pictureNode);
        return sendNode(iqNode)
                .getChild("picture")
                .flatMap(picture -> picture.getAttribute("url"))
                .map(attribute -> URI.create(attribute.toString()));
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
    public void changePrivacySetting(PrivacySettingType type, PrivacySettingValue value, JidProvider... excluded) {
        if (!type.isSupported(value)) {
            throw new IllegalArgumentException("Cannot change setting %s to %s: this toggle cannot be used because Whatsapp doesn't support it".formatted(value.name(), type.name()));
        }
        var excludedJids = Arrays.stream(excluded)
                .map(JidProvider::toJid)
                .toList();
        var children = value != PrivacySettingValue.CONTACTS_EXCEPT ? null : excludedJids.stream()
                .map(entry -> new NodeBuilder()
                        .description("user")
                        .attribute("value", entry)
                        .attribute("action", "add")
                        .build())
                .toList();
        var categoryBuilder = new NodeBuilder()
                .description("category")
                .attribute("name", type.data())
                .attribute("value", value.data());
        if (value == PrivacySettingValue.CONTACTS_EXCEPT) {
            categoryBuilder.attribute("dhash", "none");
        }
        if (children != null) {
            categoryBuilder.content(children);
        }
        var privacyNode = new NodeBuilder()
                .description("privacy")
                .content(categoryBuilder.build())
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(privacyNode);
        sendNode(iqNode);
        var newEntry = new PrivacySettingEntryBuilder()
                .type(type)
                .value(value)
                .excluded(excludedJids)
                .build();
        var oldEntry = store.findPrivacySetting(type)
                .orElse(null);
        store.addPrivacySetting(newEntry);
        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> listener.onPrivacySettingChanged(this, newEntry));
        }
    }

    /**
     * Changes the default ephemeral timer of new chats.
     *
     * @param timer the new ephemeral timer
     */
    public void changeNewChatsEphemeralTimer(ChatEphemeralTimer timer) {
        var node = new NodeBuilder()
                .description("disappearing_mode")
                .attribute("duration", timer.period().toSeconds())
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "disappearing_mode")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(node);
        sendNode(iqNode);
        store.setNewChatsEphemeralTimer(timer);
    }

    /**
     * Changes the name of this user
     *
     * @param newName the non-null new name
     */
    public void changeName(String newName) {
        if (store.device().platform().isBusiness()) {
            switch (store.clientType()) {
                case WEB ->
                        throw new IllegalArgumentException("The business name cannot be changed using the web api. " +
                                                           "This is a limitation by WhatsApp. " +
                                                           "If this ever changes, please open an issue/PR.");
                case MOBILE -> {
                    var oldName = store.name();
                    updateBusinessCertificate(newName);
                    store.setName(newName);
                    for (var listener : store.listeners()) {
                        Thread.startVirtualThread(() -> listener.onNameChanged(this, oldName, newName));
                    }
                }
            }
        } else {
            var oldName = store.name();
            var presenceNode = new NodeBuilder()
                    .description("presence")
                    .attribute("name", newName)
                    .attribute("type", "available")
                    .build();
            sendNodeWithNoResponse(presenceNode);
            store.setName(newName);
            for (var listener : store.listeners()) {
                Thread.startVirtualThread(() -> listener.onNameChanged(this, oldName, newName));
            }
        }
    }

    /**
     * Changes the about of this user
     *
     * @param newAbout the non-null new status
     */
    public void changeAbout(String newAbout) {
        var statusNode = new NodeBuilder()
                .description("status")
                .content(newAbout.getBytes(StandardCharsets.UTF_8))
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "status")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(statusNode);
        sendNode(iqNode);
        store.setAbout(newAbout);
    }

    /**
     * Changes the profile picture of yourself
     *
     * @param image the new image, can be null if you want to remove it
     */
    public void changeProfilePicture(InputStream image) {
        var body = new NodeBuilder()
                .description("picture")
                .attribute("type", "image")
                .content(getProfilePic(image))
                .build();
        switch (store.clientType()) {
            case WEB -> {
                var iqNode = new NodeBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:profile:picture")
                        .attribute("to", JidServer.user())
                        .attribute("type", "set")
                        .content(body);
                sendNode(iqNode);
            }
            case MOBILE -> {
                var localJid = store.jid()
                        .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
                var iqNode = new NodeBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:profile:picture")
                        .attribute("to", localJid)
                        .attribute("type", "set")
                        .content(body);
                sendNode(iqNode);
            }
        }
    }

    private static byte[] getProfilePic(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try (inputStream) {
            var inputImage = ImageIO.read(inputStream);
            var scaledImage = inputImage.getScaledInstance(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, Image.SCALE_SMOOTH);
            var outputImage = new BufferedImage(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, BufferedImage.TYPE_INT_RGB);
            var graphics2D = outputImage.createGraphics();
            graphics2D.drawImage(scaledImage, 0, 0, null);
            graphics2D.dispose();
            var outputStream = new ByteArrayOutputStream() {
                @Override
                public byte[] toByteArray() {
                    return buf;
                }
            };
            ImageIO.write(outputImage, "jpg", outputStream);
            return outputStream.toByteArray();
        } catch (Throwable exception) {
            throw new InternalError("Cannot get profile pic", exception);
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
        var keyNode = new NodeBuilder()
                .description(key)
                .content(Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8))
                .build();
        var businessProfileNode = new NodeBuilder()
                .description("business_profile")
                .attribute("v", "3")
                .attribute("mutation_type", "delta")
                .content(keyNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(businessProfileNode);
        var result = sendNode(iqNode);
        checkBusinessAttributeConflict(key, value, result);
        return value;
    }

    private void checkBusinessAttributeConflict(String key, String value, Node result) {
        var keyNode = result.getChild("profile").flatMap(entry -> entry.getChild(key));
        if (keyNode.isEmpty()) {
            return;
        }
        var actual = keyNode.get()
                .toContentString()
                .orElseThrow(() -> new NoSuchElementException("Missing business %s newsletters, something went wrong: %s".formatted(key, findErrorNode(result))));
        if (value != null && !value.equals(actual)) {
            throw new IllegalArgumentException("Cannot change business %s: conflict(expected %s, got %s)".formatted(key, value, actual));
        }
    }

    private String findErrorNode(Node result) {
        return Optional.ofNullable(result)
                .flatMap(node -> node.getChild("error"))
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
        var categoriesNode = new NodeBuilder()
                .description("categories")
                .content(createCategories(categories))
                .build();
        var businessProfileNode = new NodeBuilder()
                .description("business_profile")
                .attribute("v", "3")
                .attribute("mutation_type", "delta")
                .content(categoriesNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(businessProfileNode);
        sendNode(iqNode);
        return categories;
    }

    private SequencedCollection<Node> createCategories(Collection<BusinessCategory> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .map(this::createCategory)
                .toList();
    }

    private Node createCategory(BusinessCategory entry) {
        return new NodeBuilder()
                .description("category")
                .attribute("id", entry.id())
                .build();
    }

    /**
     * Change the websites of this business profile
     *
     * @param websites the new websites, can be null
     * @return a CompletableFuture
     */
    public Collection<URI> changeBusinessWebsites(Collection<URI> websites) {
        var businessProfileNode = new NodeBuilder()
                .description("business_profile")
                .attribute("v", "3")
                .attribute("mutation_type", "delta")
                .content(createWebsites(websites))
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(businessProfileNode);
        sendNode(iqNode);
        return websites;
    }

    private List<Node> createWebsites(Collection<URI> websites) {
        if (websites == null) {
            return List.of();
        }
        return websites.stream()
                .map(entry -> new NodeBuilder()
                        .description("website")
                        .content(entry.toString().getBytes(StandardCharsets.UTF_8))
                        .build())
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
     * @param productsLimit the maximum value of products to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCatalogEntry> queryBusinessCatalog(int productsLimit) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        return queryBusinessCatalog(localJid.withoutData(), productsLimit);
    }

    /**
     * Query the catalog of a business
     *
     * @param contact       the business
     * @param productsLimit the maximum value of products to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCatalogEntry> queryBusinessCatalog(JidProvider contact, int productsLimit) {
        var limitNode = new NodeBuilder()
                .description("limit")
                .content(String.valueOf(productsLimit).getBytes(StandardCharsets.UTF_8))
                .build();
        var widthNode = new NodeBuilder()
                .description("width")
                .content("100".getBytes(StandardCharsets.UTF_8))
                .build();
        var heightNode = new NodeBuilder()
                .description("height")
                .content("100".getBytes(StandardCharsets.UTF_8))
                .build();
        var productCatalogNode = new NodeBuilder()
                .description("product_catalog")
                .attribute("value", contact)
                .attribute("allow_shop_source", "true")
                .content(limitNode, widthNode, heightNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:catalog")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(productCatalogNode);
        var result = sendNode(iqNode);
        return result.streamChild("product_catalog")
                .flatMap(entry -> entry.streamChildren("product"))
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
     * @param collectionsLimit the maximum value of collections to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCollectionEntry> queryBusinessCollections(int collectionsLimit) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        return queryBusinessCollections(localJid.withoutData(), collectionsLimit);
    }

    /**
     * Query the collections of a business
     *
     * @param contact          the business
     * @param collectionsLimit the maximum value of collections to query
     * @return a CompletableFuture
     */
    public Collection<BusinessCollectionEntry> queryBusinessCollections(JidProvider contact, int collectionsLimit) {
        var collectionLimitNode = new NodeBuilder()
                .description("collection_limit")
                .content(String.valueOf(collectionsLimit).getBytes(StandardCharsets.UTF_8))
                .build();
        var itemLimitNode = new NodeBuilder()
                .description("item_limit")
                .content(String.valueOf(collectionsLimit).getBytes(StandardCharsets.UTF_8))
                .build();
        var widthNode = new NodeBuilder()
                .description("width")
                .content("100".getBytes(StandardCharsets.UTF_8))
                .build();
        var heightNode = new NodeBuilder()
                .description("height")
                .content("100".getBytes(StandardCharsets.UTF_8))
                .build();
        var collectionsNode = new NodeBuilder()
                .description("collections")
                .attribute("biz_jid", contact)
                .content(collectionLimitNode, itemLimitNode, widthNode, heightNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:catalog")
                .attribute("smax_id", "35")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(collectionsNode);
        var result = sendNode(iqNode);
        return parseCollections(result);
    }

    private List<BusinessCollectionEntry> parseCollections(Node result) {
        return Objects.requireNonNull(result, "Cannot query business collections, missing newsletters node")
                .streamChild("collections")
                .flatMap(entry -> entry.streamChildren("collection"))
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
     * Gets the verified name certificate
     *
     */
    public Optional<BusinessVerifiedNameCertificate> queryBusinessCertificate(JidProvider provider) {
        var verifiedNameNode = new NodeBuilder()
                .description("verified_name")
                .attribute("value", provider)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(verifiedNameNode);
        var result = sendNode(iqNode);
        return parseCertificate(result);
    }

    private Optional<BusinessVerifiedNameCertificate> parseCertificate(Node result) {
        return result.getChild("verified_name")
                .flatMap(Node::toContentBytes)
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
        var status = store.online();
        if (status == available) {
            return;
        }

        var presence = available ? AVAILABLE : UNAVAILABLE;
        var node = new NodeBuilder()
                .description("presence")
                .attribute("name", store.name())
                .attribute("type", presence.toString())
                .build();
        sendNodeWithNoResponse(node);
        updatePresence(null, presence);
    }

    private void updatePresence(JidProvider chatJid, ContactStatus presence) {
        if (chatJid == null) {
            store.setOnline(presence == AVAILABLE);
        }

        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"))
                .withoutData();

        var localContact = store.findContactByJid(localJid);
        if (localContact.isEmpty()) {
            return;
        }
        localContact.get().setLastKnownPresence(presence);

        if (chatJid != null) {
            store.findChatByJid(chatJid)
                    .ifPresent(chat -> chat.addPresence(localContact.get().jid(), presence));
        }

        localContact.get().setLastSeen(ZonedDateTime.now());
    }

    /**
     * Changes your presence for a specific chat
     *
     * @param chatJid  the target chat
     * @param presence the new status
     */
    public void changePresence(JidProvider chatJid, ContactStatus presence) {
        if (presence == COMPOSING || presence == RECORDING) {
            var composingBuilder = new NodeBuilder()
                    .description(COMPOSING.toString());
            if (presence == RECORDING) {
                composingBuilder.attribute("media", "audio");
            }
            var node = new NodeBuilder()
                    .description("chatstate")
                    .attribute("to", chatJid)
                    .content(composingBuilder.build())
                    .build();
            sendNodeWithNoResponse(node);
            updatePresence(chatJid, presence);
            return;
        }

        var node = new NodeBuilder()
                .description("presence")
                .attribute("type", presence.toString())
                .attribute("name", store.name())
                .build();
        sendNodeWithNoResponse(node);
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
            subscribeToPresence(jid);
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
            subscribeToPresence(jid);
        }
    }

    /**
     * Sends a request to Whatsapp in order to receive updates when the status of a contact changes.
     * These changes include the last known presence and the seconds the contact was last seen.
     *
     * @param jid the contact whose status the api should receive updates on
     */
    public void subscribeToPresence(JidProvider jid) {
        var node = new NodeBuilder()
                .description("presence")
                .attribute("to", jid)
                .attribute("type", "subscribe")
                .build();
        sendNodeWithNoResponse(node);
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
     * @param reaction the reaction to send, null if you want to remove the reaction. If a value that
     *                 isn't an emoji supported by Whatsapp is used, it will not get displayed
     *                 correctly. Use {@link WhatsAppClient#sendReaction(MessageInfo, ReactionEmoji)} if
     *                 you need a typed emoji enum.
     * @return a CompletableFuture
     */
    public MessageInfo sendReaction(MessageInfo message, String reaction) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store.clientType()))
                .chatJid(message.parentJid())
                .senderJid(message.senderJid())
                .fromMe(Objects.equals(message.senderJid().withoutData(), localJid.withoutData()))
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
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var timestamp = Clock.nowSeconds();
        var deviceInfoMetadata = new DeviceListMetadataBuilder()
                .senderTimestamp(Clock.nowSeconds())
                .build();
        var deviceInfo = recipient.toJid().hasServer(JidServer.user()) ? new DeviceContextInfoBuilder()
                .deviceListMetadataVersion(2)
                .deviceListMetadata(deviceInfoMetadata)
                .build() : null;
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store.clientType()))
                .chatJid(recipient.toJid())
                .fromMe(true)
                .senderJid(localJid)
                .build();
        var info = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(localJid)
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
                .id(ChatMessageKey.randomId(store.clientType()))
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
     * @param info    the message to send
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
        messageSenderService.sendMessage(info, Map.of());
        if (compose) {
            var pausedNode = new NodeBuilder()
                    .description("paused")
                    .build();
            var node = new NodeBuilder()
                    .description("chatstate")
                    .attribute("to", recipient)
                    .content(pausedNode)
                    .build();
            sendNodeWithNoResponse(node);
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
        messageSenderService.sendMessage(info, Map.of());
        return info;
    }


    /**
     * Resends a message
     *
     * @param messsage the message to resend
     * @param deviceJid the device to resend the message to
     */
    public void resendMessage(ChatMessageInfo messsage, Jid deviceJid) {
        messageSenderService.resendToDevice(messsage, deviceJid);
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
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(store.clientType()))
                .chatJid(Jid.statusBroadcastAccount())
                .fromMe(true)
                .senderJid(localJid)
                .build();
        var info = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(localJid)
                .key(key)
                .timestampSeconds(timestamp)
                .broadcast(false)
                .build();
        return sendChatMessage(info, false);
    }
    //</editor-fold>

    //<editor-fold desc="Message utilities">

    /**
     * Asks Whatsapp for a media reupload for a specific media
     *
     * @param info the non-null message info wrapping the media
     */
    public void requireReupload(MessageInfo info) {
        try {
            if (!(info.message().content() instanceof MediaProvider mediaProvider)) {
                throw new IllegalArgumentException("Expected media message, got: " + info.message().category());
            }

            var localJid = store.jid()
                    .orElseThrow(() -> new IllegalStateException("Local jid is not available"));

            var mediaKey = mediaProvider.mediaKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing media key"));
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(mediaKey, "AES"))
                    .thenExpand("WhatsApp Media Retry Notification".getBytes(StandardCharsets.UTF_8), 32);
            var retryKey = hkdf.deriveKey("AES", params);
            var receipt = ServerErrorReceiptSpec.encode(new ServerErrorReceipt(info.id()));
            var aad = info.id().getBytes(StandardCharsets.UTF_8);
            var encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            encryptCipher.init(
                    Cipher.ENCRYPT_MODE,
                    retryKey,
                    new GCMParameterSpec(128, SecureBytes.random(12))
            );
            encryptCipher.updateAAD(aad);
            var ciphertext = encryptCipher.update(receipt);
            var encPNode = new NodeBuilder()
                    .description("enc_p")
                    .content(ciphertext)
                    .build();
            var encIvNode = new NodeBuilder()
                    .description("enc_iv")
                    .content(SecureBytes.random(12))
                    .build();
            var encryptNode = new NodeBuilder()
                    .description("encrypt")
                    .content(encPNode, encIvNode)
                    .build();
            var rmrBuilder = new NodeBuilder()
                    .description("rmr")
                    .attribute("value", info.parentJid())
                    .attribute("from_me", Objects.equals(localJid.withoutData(), info.senderJid().withoutData()));
            if (!Objects.equals(info.parentJid(), info.senderJid())) {
                rmrBuilder.attribute("participant", info.senderJid());
            }
            var node = new NodeBuilder()
                    .description("receipt")
                    .attribute("id", info.id())
                    .attribute("to", localJid.withoutData())
                    .attribute("type", "server-error")
                    .content(encryptNode, rmrBuilder.build());
            var result = sendNode(node, resultNode -> resultNode.hasDescription("notification"));
            if (result.getChild("error").isPresent()) {
                var code = result.getAttribute("code")
                        .map(NodeAttribute::toString)
                        .orElse("unknown");
                throw new IllegalArgumentException("Erroneous response from media reupload: " + code);
            }
            var resultEncryptNode = result.getChild("encrypt")
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypt node in media reupload"));
            var resultMediaNode = resultEncryptNode.getChild("enc_p")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted payload node in media reupload"));
            var resultMediaIv = resultEncryptNode.getChild("enc_iv")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted iv node in media reupload"));
            var decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            decryptCipher.init(
                    Cipher.DECRYPT_MODE,
                    retryKey,
                    new GCMParameterSpec(128, resultMediaIv)
            );
            decryptCipher.updateAAD(aad);
            var mediaRetryNotificationData = decryptCipher.doFinal(resultMediaNode);
            var mediaRetryNotification = MediaRetryNotificationSpec.decode(mediaRetryNotificationData);
            var directPath = mediaRetryNotification.directPath()
                    .orElseThrow(() -> new RuntimeException("Media reupload failed"));
            mediaProvider.setMediaUrl(null);
            mediaProvider.setMediaDirectPath(directPath);
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
        var future = new CompletableFuture<MessageInfo>();
        addMessageReplyListener(id, future::complete);
        return future.join();
    }

    /**
     * Forwards a message to another chat
     *
     * @param chat        the non-null chat
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
        if (!(messageInfo.message().content() instanceof TextMessage textMessage)) {
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
                messageSenderService.sendMessage(info, Map.of("edit", getEditBit(info)));
                return oldMessage;
            }
            case ChatMessageInfo oldChatInfo -> {
                var localJid = store.jid()
                        .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
                var key = new ChatMessageKeyBuilder()
                        .id(oldChatInfo.id())
                        .chatJid(oldChatInfo.chatJid())
                        .fromMe(true)
                        .senderJid(localJid)
                        .build();
                var info = new ChatMessageInfoBuilder()
                        .status(MessageStatus.PENDING)
                        .senderJid(localJid)
                        .key(key)
                        .message(MessageContainer.ofEditedMessage(newMessage))
                        .timestampSeconds(Clock.nowSeconds())
                        .broadcast(oldChatInfo.chatJid().hasServer(JidServer.broadcast()))
                        .build();
                messageSenderService.sendMessage(info, Map.of("edit", getEditBit(info)));
                return oldMessage;
            }
            default -> throw new IllegalStateException("Unsupported edit: " + oldMessage);
        }
    }

    /**
     * Deletes a message
     *
     * @param info the non-null message to delete
     */
    public void deleteMessage(NewsletterMessageInfo info) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var revokeInfo = new NewsletterMessageInfoBuilder()
                .id(info.id())
                .serverId(info.serverId())
                .timestampSeconds(Clock.nowSeconds())
                .message(MessageContainer.empty())
                .status(MessageStatus.PENDING)
                .build();
        revokeInfo.setNewsletter(info.newsletter());
        messageSenderService.sendMessage(info, Map.of("edit", getDeleteBit(localJid, info)));
    }

    /**
     * Deletes a message
     *
     * @param info     non-null message to delete
     * @param everyone whether the message should be deleted for everyone or only for this client and
     *                 its companions
     */
    public void deleteMessage(ChatMessageInfo info, boolean everyone) {
        if (everyone) {
            var localJid = store.jid()
                    .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
            var message = new ProtocolMessageBuilder()
                    .protocolType(ProtocolMessage.Type.REVOKE)
                    .key(info.key())
                    .build();
            var sender = info.chatJid().hasServer(JidServer.groupOrCommunity()) ? localJid : null;
            var key = new ChatMessageKeyBuilder()
                    .id(ChatMessageKey.randomId(store.clientType()))
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
            messageSenderService.sendMessage(info, Map.of("edit", getDeleteBit(localJid, info)));
        } else {
            switch (store.clientType()) {
                case WEB -> {
                    var deleteMessageAction = new DeleteMessageForMeActionBuilder()
                            .deleteMedia(false)
                            .messageTimestampSeconds(info.timestampSeconds().orElse(0L))
                            .build();
                    var syncAction = ActionValueSync.of(deleteMessageAction);
                    var entry = new PendingMutation(syncAction, Operation.SET, info.chatJid().toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
                    pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
                }
                case MOBILE -> info.chat().ifPresent(chat -> chat.removeMessage(info.id()));
            }
        }
    }


    private int getEditBit(MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 3;
        }

        return 1;
    }

    private int getDeleteBit(Jid localJid, MessageInfo info) {
        if (info.parentJid().hasServer(JidServer.newsletter())) {
            return 8;
        }

        var fromMe = Objects.equals(info.senderJid().withoutData(), localJid.withoutData());
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
        store.findChatByJid(chat.toJid())
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
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat.toJid())
                    .ifPresent(entry -> entry.setMarkedAsUnread(read));
            return;
        }

        var markAction = new MarkChatAsReadActionBuilder()
                .read(read)
                .build();
        var syncAction = ActionValueSync.of(markAction);
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString());
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
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
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat)
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
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString());
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
    }

    /**
     * Unmutes a chat
     *
     * @param chat the target chat
     */
    public void unmuteChat(JidProvider chat) {
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat)
                    .ifPresent(entry -> entry.setMute(ChatMute.notMuted()));
            return;
        }

        var muteAction = new MuteActionBuilder()
                .muted(false)
                .muteEndTimestampSeconds(0)
                .autoMuted(false)
                .build();
        var syncAction = ActionValueSync.of(muteAction);
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString());
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
    }

    /**
     * Blocks a contact
     *
     * @param contact the target chat
     */
    public void blockContact(JidProvider contact) {
        var body = new NodeBuilder()
                .description("item")
                .attribute("action", "block")
                .attribute("value", contact)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "blocklist")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(body);
        sendNode(iqNode);
    }

    /**
     * Unblocks a contact
     *
     * @param contact the target chat
     */
    public void unblockContact(JidProvider contact) {
        var body = new NodeBuilder()
                .description("item")
                .attribute("action", "unblock")
                .attribute("value", contact)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "blocklist")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(body);
        sendNode(iqNode);
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
                var body = timer == ChatEphemeralTimer.OFF
                        ? new NodeBuilder().description("not_ephemeral").build()
                        : new NodeBuilder()
                        .description("ephemeral")
                        .attribute("expiration", timer.period().toSeconds())
                        .build();
                var iqNode = new NodeBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:g2")
                        .attribute("to", chat)
                        .attribute("type", "set")
                        .content(body);
                sendNode(iqNode);
            }
            default ->
                    throw new IllegalArgumentException("Unexpected chat %s: ephemeral messages are only supported for conversations and groups".formatted(chat.toJid()));
        }
    }

    /**
     * Marks a message as played
     *
     * @param info the target message
     */
    public void markMessagePlayed(ChatMessageInfo info) {
        if (info.senderJid().hasServer(JidServer.newsletter())) {
            return;
        }

        var policy = store()
                .findPrivacySetting(PrivacySettingType.READ_RECEIPTS);
        if (policy.isPresent() && policy.get().value() == PrivacySettingValue.EVERYONE) {
            sendMessageReceipt(info, "played");
            info.setStatus(MessageStatus.PLAYED);
        }
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
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat)
                    .ifPresent(entry -> entry.setPinnedTimestampSeconds(pin ? (int) Clock.nowSeconds() : 0));
            return;
        }
        var pinAction = new PinActionBuilder()
                .pinned(pin)
                .build();
        var syncAction = ActionValueSync.of(pinAction);

        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString());
        pushWebAppState(PatchType.REGULAR_LOW, List.of(entry));
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
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            info.setStarred(star);
            return info;
        }

        var starAction = new StarActionBuilder()
                .starred(star)
                .build();
        var syncAction = ActionValueSync.of(starAction);
        var entry = new PendingMutation(syncAction, Operation.SET, info.chatJid()
                .toString(), info.id(), fromMeToFlag(info), participantToFlag(info));
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
        return info;
    }

    private String fromMeToFlag(MessageInfo info) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var fromMe = Objects.equals(info.senderJid().withoutData(), localJid.withoutData());
        return booleanToInt(fromMe);
    }

    private String participantToFlag(MessageInfo info) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var fromMe = Objects.equals(info.senderJid().withoutData(), localJid.withoutData());
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
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat)
                    .ifPresent(entry -> entry.setArchived(archive));
            return;
        }

        var archiveAction = new ArchiveChatActionBuilder()
                .archived(archive)
                .build();
        var syncAction = ActionValueSync.of(archiveAction);
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString());
        pushWebAppState(PatchType.REGULAR_LOW, List.of(entry));
    }

    /**
     * Unarchives a chat
     *
     * @param chat the target chat
     */
    public void unarchive(JidProvider chat) {
        archiveChat(chat, false);
    }

    /**
     * Marks a message as read
     *
     * @param info the target message
     */
    public void markMessageRead(MessageInfo info) {
        var policy = store()
                .findPrivacySetting(PrivacySettingType.READ_RECEIPTS);
        var type = info.senderJid().hasServer(JidServer.newsletter()) || (policy.isPresent() && policy.get().value() == PrivacySettingValue.NOBODY)
                ? "read-self"
                : "read";
        sendMessageReceipt(info, type);
        info.setStatus(MessageStatus.READ);
    }

    private void sendMessageReceipt(MessageInfo info, String type) {
        var id = info.id();
        var parentJid = info.parentJid();
        var timestamp = Clock.nowSeconds();
        var senderJid = info.senderJid();

        var builder = new NodeBuilder()
                .description("receipt")
                .attribute("id", id)
                .attribute("type", type)
                .attribute("to", parentJid)
                .attribute("t", timestamp);

        if (!Objects.equals(parentJid.user(), senderJid.user()) && !senderJid.hasServer(JidServer.lid())) {
            builder.attribute("participant", senderJid.withoutData());
        }

        sendNodeWithNoResponse(builder.build());
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
        var jid = chat.toJid();
        var body = new NodeBuilder()
                .description("query")
                .attribute("request", "interactive")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", jid)
                .attribute("type", "get")
                .content(body);
        var response = sendNode(iqNode);
        return handleGroupMetadata(response);
    }

    private GroupOrCommunityMetadata handleGroupMetadata(Node response) {
        var metadataNode = Optional.of(response)
                .filter(entry -> entry.hasDescription("group"))
                .or(() -> response.getChild("group"))
                .orElseThrow(() -> new NoSuchElementException("Erroneous response: %s".formatted(response)));
        var metadata = parseGroupMetadata(metadataNode);
        var chat = store.findChatByJid(metadata.jid())
                .orElseGet(() -> store().addNewChat(metadata.jid()));
        chat.setName(metadata.subject());
        return metadata;
    }

    private GroupOrCommunityMetadata parseGroupMetadata(Node node) {
        var groupIdUser = node.getRequiredAttributeAsString("id");
        var groupId = Jid.of(groupIdUser, JidServer.groupOrCommunity());
        var subject = node.getAttributeAsString("subject", "");
        var subjectAuthor = node.getAttributeAsJid("s_o", null);
        var subjectTimestampSeconds = node.getAttributeAsLong("s_t", 0);
        var foundationTimestampSeconds = node.getAttributeAsLong("creation", 0);
        var founder = node.getAttributeAsJid("creator", null);
        var description = node.getChild("description")
                .flatMap(parent -> parent.getChild("body"))
                .flatMap(Node::toContentString)
                .orElse(null);
        var descriptionId = node.getChild("description")
                .flatMap(descriptionNode -> descriptionNode.getAttributeAsString("id"))
                .orElse(null);
        long ephemeral = node.getChild("ephemeral")
                .map(ephemeralNode -> ephemeralNode.getAttributeAsLong("expiration", 0))
                .orElse(0L);
        var communityNode = node.getChild("parent")
                .orElse(null);
        var policies = new HashMap<Integer, ChatSettingPolicy>();
        var lidAddressingMode = node.hasAttribute("addressing_mode", "lid");
        var linkedParent = node.getChild("linked_parent")
                .flatMap(parent -> parent.getAttributeAsJid("jid"))
                .orElse(null);
        var isIncognito = node.hasChild("incognito");
        if (communityNode == null) {
            policies.put(GroupSetting.EDIT_GROUP_INFO.index(), ChatSettingPolicy.of(node.hasChild("announce")));
            policies.put(GroupSetting.SEND_MESSAGES.index(), ChatSettingPolicy.of(node.hasChild("restrict")));
            var addParticipantsMode = node.getChild("member_add_mode")
                    .flatMap(Node::toContentString)
                    .orElse(null);
            policies.put(GroupSetting.ADD_PARTICIPANTS.index(), ChatSettingPolicy.of(Objects.equals(addParticipantsMode, "admin_add")));
            var groupJoin = node.getChild("membership_approval_mode")
                    .flatMap(entry -> entry.getChild("group_join"))
                    .map(entry -> entry.hasAttribute("state", "on"))
                    .orElse(false);
            policies.put(GroupSetting.APPROVE_PARTICIPANTS.index(), ChatSettingPolicy.of(groupJoin));
            var participants = node.streamChildren("participant")
                    .filter(entry -> !entry.hasAttribute("error"))
                    .map(entry -> {
                        var id = entry.getRequiredAttributeAsJid("jid");
                        var role = entry.getAttributeAsString("type")
                                .map(ChatRole::of)
                                .orElse(ChatRole.USER);
                        return ChatParticipant.ofGroup(id, role);
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return new GroupOrCommunityMetadataBuilder()
                    .jid(groupId)
                    .subject(subject)
                    .subjectAuthorJid(subjectAuthor)
                    .subjectTimestampSeconds(subjectTimestampSeconds)
                    .foundationTimestampSeconds(foundationTimestampSeconds)
                    .founderJid(founder)
                    .description(description)
                    .descriptionId(descriptionId)
                    .settings(policies)
                    .participants(participants)
                    .ephemeralExpirationSeconds(ephemeral)
                    .isLidAddressingMode(lidAddressingMode)
                    .isCommunity(false)
                    .isIncognito(isIncognito)
                    .parentCommunityJid(linkedParent)
                    .build();
        } else {
            policies.put(CommunitySetting.MODIFY_GROUPS.index(), ChatSettingPolicy.of(communityNode.hasChild("allow_non_admin_sub_group_creation")));
            var addParticipantsMode = node.getChild("member_add_mode")
                    .flatMap(Node::toContentString)
                    .orElse(null);
            policies.put(CommunitySetting.ADD_PARTICIPANTS.index(), ChatSettingPolicy.of(Objects.equals(addParticipantsMode, "admin_add")));
            var linkedGroupsQueryBody = new NodeBuilder()
                    .description("linked_groups_participants")
                    .build();
            var linkedGroupsQueryRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("xmlns", "w:g2")
                    .attribute("to", groupId)
                    .attribute("type", "get")
                    .content(linkedGroupsQueryBody);
            var linkedGroupsResponse = sendNode(linkedGroupsQueryRequest);
            var participants = linkedGroupsResponse
                    .streamChild("linked_groups_participants")
                    .flatMap(participantsNodeBody -> participantsNodeBody.streamChildren("participant"))
                    .flatMap(participantNode -> participantNode.streamAttributeAsJid("jid"))
                    .map(ChatParticipant::ofCommunity)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            var communityGroupsQueryBody = CommunityRequests.linkedGroups(groupId, "INTERACTIVE");
            var communityGroupsQuery = new NodeBuilder()
                    .description("query")
                    .attribute("query_id", "7353258338095347")
                    .content(communityGroupsQueryBody)
                    .build();
            var communityGroupsRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("xmlns", "w:mex")
                    .attribute("to", Jid.userServer())
                    .attribute("type", "get")
                    .content(communityGroupsQuery);
            var communityGroupsResponse = sendNode(communityGroupsRequest);
            var communityLinkedGroups = communityGroupsResponse.getChild("result")
                    .flatMap(Node::toContentBytes)
                    .flatMap(CommunityLinkedGroupsResponse::ofJson)
                    .map(CommunityLinkedGroupsResponse::linkedGroups)
                    .orElseGet(LinkedHashSet::new);
            return new GroupOrCommunityMetadataBuilder()
                    .jid(groupId)
                    .subject(subject)
                    .subjectAuthorJid(subjectAuthor)
                    .subjectTimestampSeconds(subjectTimestampSeconds)
                    .foundationTimestampSeconds(foundationTimestampSeconds)
                    .founderJid(founder)
                    .description(description)
                    .descriptionId(descriptionId)
                    .settings(policies)
                    .participants(participants)
                    .ephemeralExpirationSeconds(ephemeral)
                    .isCommunity(true)
                    .communityGroups(communityLinkedGroups)
                    .isLidAddressingMode(lidAddressingMode)
                    .build();
        }
    }

    private Stream<ChatParticipant> parseGroupParticipant(Node node) {
        if (node.hasAttribute("error")) {
            return Stream.empty();
        }

        var id = node.getRequiredAttributeAsJid("jid");
        var role = ChatRole.of(node.getRequiredAttributeAsString("type"));
        var result = ChatParticipant.ofGroup(id, role);
        return Stream.of(result);
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
        var inviteNode = new NodeBuilder()
                .description("invite")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "get")
                .content(inviteNode);
        var result = sendNode(iqNode);
        return result.getChild("invite")
                .orElseThrow(() -> new NoSuchElementException("Missing invite code in invite newsletters"))
                .getRequiredAttribute("code")
                .toString();
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
        var requestsNode = new NodeBuilder()
                .description("membership_approval_requests")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "get")
                .content(requestsNode);
        var result = sendNode(iqNode);
        return result.streamChild("membership_approval_requests")
                .flatMap(requests -> requests.streamChildren("membership_approval_request"))
                .flatMap(participant -> participant.streamAttributeAsJid("user"))
                .toList();
    }

    /**
     * Changes the approval request status of an array of participants for a group
     *
     * @param chat         the target group
     * @param approve      whether the participants should be accepted into the group
     * @param participants the target participants
     * @return a CompletableFuture
     */
    public Collection<Jid> approveGroupOrCommunityParticipants(JidProvider chat, boolean approve, JidProvider... participants) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var participantsNodes = Arrays.stream(participants)
                .map(participantJid -> new NodeBuilder()
                        .description("participant")
                        .attribute("value", participantJid)
                        .build())
                .toList();
        var action = approve ? "approve" : "reject";
        var actionNode = new NodeBuilder()
                .description(action)
                .content(participantsNodes)
                .build();
        var membershipRequestsActionNode = new NodeBuilder()
                .description("membership_requests_action")
                .content(actionNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "set")
                .content(membershipRequestsActionNode);
        var result = sendNode(iqNode);
        return result.streamChild("membership_requests_action")
                .flatMap(response -> response.streamChild(action))
                .flatMap(requests -> requests.streamChildren("participant"))
                .filter(participant -> participant.getAttribute("error").isEmpty())
                .flatMap(participant -> participant.streamAttributeAsJid("value"))
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

        var inviteNode = new NodeBuilder()
                .description("invite")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "set")
                .content(inviteNode);
        sendNode(iqNode);
    }

    /**
     * Accepts the invite for a group
     *
     * @param inviteCode the invite countryCode
     * @return a CompletableFuture
     */
    public Optional<Chat> acceptGroupOrCommunityInvite(String inviteCode) {
        var inviteNode = new NodeBuilder()
                .description("invite")
                .attribute("code", inviteCode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(inviteNode);
        var result = sendNode(iqNode);
        return result.getChild("group")
                .flatMap(group -> group.getAttributeAsJid("value"))
                .map(attribute -> store.findChatByJid(attribute).orElseGet(() -> store.addNewChat(attribute)));
    }

    /**
     * Promotes any value of contacts to admin in a group
     *
     * @param chat     the target group
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
     * Demotes any value of contacts to admin in a group
     *
     * @param chat     the target group
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
     * Adds any value of contacts to a group
     *
     * @param chat     the target group
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
     * Removes any value of contacts from group
     *
     * @param chat     the target group
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
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        if (jids.isEmpty()) {
            return List.of();
        }

        var participants = jids.stream()
                .map(JidProvider::toJid)
                .map(jid -> new NodeBuilder()
                        .description("participant")
                        .attribute("value", checkGroupParticipantJid(localJid, jid, "Cannot execute action on yourself"))
                        .build())
                .toList();
        var actionNode = new NodeBuilder()
                .description(action.data())
                .content(participants)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "set")
                .content(actionNode);
        var result = sendNode(iqNode);
        return result.streamChild(action.data())
                .flatMap(body -> body.streamChildren("participant"))
                .filter(participant -> participant.getAttribute("error").isEmpty())
                .flatMap(participant -> participant.streamAttributeAsJid("value"))
                .toList();
    }

    private Jid checkGroupParticipantJid(Jid localJid, Jid groupParticipantJid, String errorMessage) {
        if (Objects.equals(localJid.withoutData(), groupParticipantJid.withoutData())) {
            throw new IllegalArgumentException(errorMessage);
        } else {
            return groupParticipantJid;
        }
    }

    /**
     * Changes the name of a group
     *
     * @param chat    the target group
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
        var body = new NodeBuilder()
                .description("subject")
                .content(newName.getBytes(StandardCharsets.UTF_8))
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "set")
                .content(body);
        sendNode(iqNode);
    }

    /**
     * Changes the description of a group
     *
     * @param chat        the target group
     * @param description the new name for the group, can be null if you want to remove it
     */
    public void changeGroupOrCommunityDescription(JidProvider chat, String description) {
        if (!chat.toJid().hasServer(JidServer.groupOrCommunity())) {
            throw new IllegalArgumentException("Expected a group/community");
        }
        var descriptionId = queryGroupOrCommunityMetadata(chat.toJid())
                .descriptionId()
                .orElse(null);
        var bodyBuilder = new NodeBuilder()
                .description("description");
        if (description != null) {
            bodyBuilder.attribute("id", SecureBytes.randomSid());
            var bodyNode = new NodeBuilder()
                    .description("body")
                    .content(description.getBytes(StandardCharsets.UTF_8))
                    .build();
            bodyBuilder.content(bodyNode);
        } else {
            bodyBuilder.attribute("delete", true);
        }
        if (descriptionId != null) {
            bodyBuilder.attribute("prev", descriptionId);
        }
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", chat)
                .attribute("type", "set")
                .content(bodyBuilder.build());
        sendNode(iqNode);
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
        var body = new NodeBuilder()
                .description("picture")
                .attribute("type", "image")
                .content(getProfilePic(image))
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:profile:picture")
                .attribute("target", group)
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(body);
        sendNode(iqNode);
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
     * @param subject         the new group's name
     * @param parentCommunity the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, JidProvider parentCommunity) {
        return createGroup(subject, ChatEphemeralTimer.OFF, parentCommunity, new JidProvider[0]);
    }

    /**
     * Creates a new group
     *
     * @param subject         the new group's name
     * @param timer           the default ephemeral timer for messages sent in this group
     * @param parentCommunity the community to whom the new group will be linked
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity) {
        return createGroup(subject, timer, parentCommunity, new JidProvider[0]);
    }

    private Optional<GroupOrCommunityMetadata> createGroup(String subject, ChatEphemeralTimer timer, JidProvider parentCommunity, JidProvider... contacts) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var timestamp = Clock.nowSeconds();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("The subject of a group cannot be blank");
        }
        if (parentCommunity == null && contacts.length < 1) {
            throw new IllegalArgumentException("Expected at least 1 member for this group");
        }
        var availableMembers = Arrays.stream(contacts)
                .map(JidProvider::toJid)
                .collect(Collectors.toUnmodifiableSet());
        var children = new ArrayList<Node>();
        if (parentCommunity != null) {
            children.add(new NodeBuilder()
                    .description("linked_parent")
                    .attribute("value", parentCommunity)
                    .build());
        }
        if (timer != ChatEphemeralTimer.OFF) {
            children.add(new NodeBuilder()
                    .description("ephemeral")
                    .attribute("expiration", timer.periodSeconds())
                    .build());
        }
        children.add(new NodeBuilder()
                .description("member_add_mode")
                .content("all_member_add".getBytes(StandardCharsets.UTF_8))
                .build());
        var groupJoinNode = new NodeBuilder()
                .description("group_join")
                .attribute("state", "off")
                .build();
        children.add(new NodeBuilder()
                .description("membership_approval_mode")
                .content(groupJoinNode)
                .build());
        availableMembers.stream()
                .map(JidProvider::toJid)
                .map(Jid::withoutData)
                .distinct()
                .map(contact -> new NodeBuilder()
                        .description("participant")
                        .attribute("value", checkGroupParticipantJid(localJid, contact.toJid(), "Cannot create group with yourself as a participant"))
                        .build())
                .forEach(children::add);
        var body = new NodeBuilder()
                .description("create")
                .attribute("subject", subject)
                .attribute("key", timestamp)
                .content(children)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(body);
        var future = sendNode(iqNode);
        return future.getChild("group")
                .map(this::handleGroupMetadata);
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
        if (metadata.isCommunity()) {
            var communityJid = metadata.parentCommunityJid().orElse(metadata.jid());
            var linkedGroupsNode = new NodeBuilder()
                    .description("linked_groups")
                    .attribute("parent_group_jid", communityJid)
                    .build();
            var body = new NodeBuilder()
                    .description("leave")
                    .content(linkedGroupsNode)
                    .build();
            var iqNode = new NodeBuilder()
                    .description("iq")
                    .attribute("xmlns", "w:g2")
                    .attribute("to", JidServer.user())
                    .attribute("type", "set")
                    .content(body);
            sendNode(iqNode);
        } else {
            var groupNode = new NodeBuilder()
                    .description("group")
                    .attribute("id", group)
                    .build();
            var body = new NodeBuilder()
                    .description("leave")
                    .content(groupNode)
                    .build();
            var iqNode = new NodeBuilder()
                    .description("iq")
                    .attribute("xmlns", "w:g2")
                    .attribute("to", JidServer.groupOrCommunity())
                    .attribute("type", "set")
                    .content(body);
            sendNode(iqNode);
        }
    }

    /**
     * Deletes a chat for this client and its companions using a modern version of Whatsapp Important:
     * this message doesn't seem to work always as of now
     *
     * @param chat the non-null chat to delete
     */
    public void deleteChat(JidProvider chat) {
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.removeChat(chat.toJid());
            return;
        }

        var deleteChatAction = new DeleteChatActionBuilder()
                .build();
        var syncAction = ActionValueSync.of(deleteChatAction);
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString(), "1");
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
    }

    /**
     * Clears the content of a chat for this client and its companions using a modern version of
     * Whatsapp Important: this message doesn't seem to work always as of now
     *
     * @param chat                the non-null chat to clear
     * @param keepStarredMessages whether starred messages in this chat should be kept
     */
    public void clearChat(JidProvider chat, boolean keepStarredMessages) {
        if (store.clientType() == WhatsAppClientType.MOBILE) {
            store.findChatByJid(chat.toJid())
                    .ifPresent(Chat::removeMessages);
            return;
        }

        var known = store.findChatByJid(chat);
        var clearChatAction = new ClearChatActionBuilder()
                .build();
        var syncAction = ActionValueSync.of(clearChatAction);
        var entry = new PendingMutation(syncAction, Operation.SET, chat.toJid().toString(), booleanToInt(keepStarredMessages), "0");
        pushWebAppState(PatchType.REGULAR_HIGH, List.of(entry));
    }

    /**
     * Creates a new community
     *
     * @param subject the non-null name of the new community
     * @param body    the nullable description of the new community
     * @return a CompletableFuture
     */
    public Optional<GroupOrCommunityMetadata> createCommunity(String subject, String body) {
        var descriptionId = HexFormat.of().formatHex(SecureBytes.random(12));
        var children = new ArrayList<Node>();
        var bodyNode = new NodeBuilder()
                .description("body")
                .content(Objects.requireNonNullElse(body, "").getBytes(StandardCharsets.UTF_8))
                .build();
        children.add(new NodeBuilder()
                .description("description")
                .attribute("id", descriptionId)
                .content(bodyNode)
                .build());
        children.add(new NodeBuilder()
                .description("parent")
                .attribute("default_membership_approval_mode", "request_required")
                .build());
        children.add(new NodeBuilder()
                .description("allow_non_admin_sub_group_creation")
                .build());
        children.add(new NodeBuilder()
                .description("create_general_chat")
                .build());
        var entry = new NodeBuilder()
                .description("create")
                .attribute("subject", subject)
                .content(children)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(entry);
        var resultNode = sendNode(iqNode);
        return parseGroupResult(resultNode);
    }

    private Optional<GroupOrCommunityMetadata> parseGroupResult(Node node) {
        return node.getChild("group")
                .map(this::handleGroupMetadata);
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
        var deleteParentNode = new NodeBuilder()
                .description("delete_parent")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", community)
                .attribute("type", "set")
                .content(deleteParentNode);
        sendNode(iqNode);
    }

    /**
     * Changes a group setting
     *
     * @param chat    the non-null group affected by this change
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
                if (metadata.isCommunity()) {
                    throw new IllegalArgumentException("Cannot change community setting '" + setting + "' in a group");
                }
                var body = switch (groupSetting) {
                    case EDIT_GROUP_INFO -> new NodeBuilder()
                            .description(policy == ChatSettingPolicy.ADMINS ? "locked" : "unlocked")
                            .build();
                    case SEND_MESSAGES -> new NodeBuilder()
                            .description(policy == ChatSettingPolicy.ADMINS ? "announcement" : "not_announcement")
                            .build();
                    case ADD_PARTICIPANTS -> new NodeBuilder()
                            .description("member_add_mode")
                            .content(policy == ChatSettingPolicy.ADMINS ? "admin_add".getBytes(StandardCharsets.UTF_8) : "all_member_add".getBytes(StandardCharsets.UTF_8))
                            .build();
                    case APPROVE_PARTICIPANTS -> {
                        var groupJoinNode = new NodeBuilder()
                                .description("group_join")
                                .attribute("state", policy == ChatSettingPolicy.ADMINS ? "on" : "off")
                                .build();
                        yield new NodeBuilder()
                                .description("membership_approval_mode")
                                .content(groupJoinNode)
                                .build();
                    }
                };
                var iqNode = new NodeBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:g2")
                        .attribute("to", chat)
                        .attribute("type", "set")
                        .content(body);
                sendNode(iqNode);
            }
            case CommunitySetting communitySetting -> {
                if (!metadata.isCommunity()) {
                    throw new IllegalArgumentException("Cannot change group setting '" + setting + "' in a community");
                }

                switch (communitySetting) {
                    case MODIFY_GROUPS -> {
                        var request = CommunityRequests.changeModifyGroupsSetting(chat.toJid(), policy == ChatSettingPolicy.ANYONE);
                        var body = new NodeBuilder()
                                .description("query")
                                .attribute("query_id", "24745914578387890")
                                .content(request.getBytes())
                                .build();
                        var iqNode = new NodeBuilder()
                                .description("iq")
                                .attribute("xmlns", "w:mex")
                                .attribute("to", JidServer.user())
                                .attribute("type", "get")
                                .content(body);
                        var result = sendNode(iqNode);
                        var resultJsonSource = result.getChild("result")
                                .flatMap(Node::toContentString)
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
                        var body = new NodeBuilder()
                                .description("member_add_mode")
                                .content(policy == ChatSettingPolicy.ANYONE ? "all_member_add".getBytes() : "admin_add".getBytes())
                                .build();
                        var iqNode = new NodeBuilder()
                                .description("iq")
                                .attribute("xmlns", "w:g2")
                                .attribute("to", chat)
                                .attribute("type", "set")
                                .content(body);
                        var result = sendNode(iqNode);
                        if (result.getChild("error").isPresent()) {
                            throw new IllegalArgumentException("Cannot change community setting: " + result);
                        }
                    }
                }
            }
        }
    }

    /**
     * Links any value of groups to a community
     *
     * @param community the non-null community where the groups will be added
     * @param groups    the non-null groups to add
     * @return a CompletableFuture that wraps a map guaranteed to contain every group that was provided as input paired to whether the request was successful
     */
    public Set<Jid> linkGroupsToCommunity(JidProvider community, JidProvider... groups) {
        var body = Arrays.stream(groups)
                .map(entry -> new NodeBuilder()
                        .description("group")
                        .attribute("value", entry)
                        .build())
                .toList();
        var linkNode = new NodeBuilder()
                .description("link")
                .attribute("link_type", "sub_group")
                .content(body)
                .build();
        var linksNode = new NodeBuilder()
                .description("links")
                .content(linkNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", community)
                .attribute("type", "set")
                .content(linksNode);
        var result = sendNode(iqNode);
        var success = result.streamChild("links")
                .flatMap(entry -> entry.streamChildren("link"))
                .filter(entry -> entry.hasAttribute("link_type", "sub_group"))
                .flatMap(entry -> entry.streamChild("group"))
                .flatMap(entry -> entry.streamAttributeAsJid("value"))
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
        var groupNode = new NodeBuilder()
                .description("group")
                .attribute("value", group)
                .build();
        var unlinkNode = new NodeBuilder()
                .description("unlink")
                .attribute("unlink_type", "sub_group")
                .content(groupNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", community)
                .attribute("type", "set")
                .content(unlinkNode);
        var result = sendNode(iqNode);
        return result.getChild("unlink")
                .filter(entry -> entry.hasAttribute("unlink_type", "sub_group"))
                .flatMap(entry -> entry.getChild("group"))
                .map(entry -> entry.hasAttribute("value", group.toJid()))
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
     * @param limit       how many patches should be returned
     * @return a list of recommended newsletters, if the feature is available
     */
    public Collection<Newsletter> queryRecommendedNewsletters(String countryCode, int limit) {
        var request = NewsletterRequests.recommendedNewsletters("RECOMMENDED", List.of(countryCode), limit);
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6190824427689257")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        return sendNode(iqNode)
                .getChild("result")
                .flatMap(Node::toContentBytes)
                .flatMap(RecommendedNewslettersResponse::of)
                .map(RecommendedNewslettersResponse::newsletters)
                .orElse(List.of());
    }

    /**
     * Queries any value of messages from a newsletter
     *
     * @param newsletterJid the non-null value of the newsletter
     * @param count         how many messages should be queried
     */
    public void queryNewsletterMessages(JidProvider newsletterJid, int count) {
        var messagesNode = new NodeBuilder()
                .description("messages")
                .attribute("count", count)
                .attribute("type", "jid")
                .attribute("jid", newsletterJid)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", JidServer.newsletter())
                .attribute("type", "get")
                .content(messagesNode);
        sendNode(iqNode);
    }

    /**
     * Subscribes to a public newsletter's event stream of reactions
     *
     * @param channel the non-null channel
     * @return the time, in minutes, during which updates will be sent
     */
    public OptionalLong subscribeToNewsletterReactions(JidProvider channel) {
        var liveUpdatesNode = new NodeBuilder()
                .description("live_updates")
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "newsletter")
                .attribute("to", channel)
                .attribute("type", "set")
                .content(liveUpdatesNode);
        var result = sendNode(iqNode);
        return result.streamChild("live_updates")
                .flatMapToLong(node -> node.streamAttributeAsLong("duration"))
                .findFirst();
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
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6996806640408138")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        var result = sendNode(iqNode)
                .getChild("result")
                .flatMap(Node::toContentBytes)
                .flatMap(NewsletterResponse::ofJson)
                .map(NewsletterResponse::newsletter);
        result.ifPresent(newsletter -> subscribeToNewsletterReactions(newsletter.jid()));
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
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "7150902998257522")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        sendNode(iqNode);
    }

    /**
     * Joins a newsletter
     *
     * @param newsletter a non-null newsletter
     */
    public void joinNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.joinNewsletter(newsletter.toJid());
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "9926858900719341")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        sendNode(iqNode);
    }

    /**
     * Leaves a newsletter
     *
     * @param newsletter a non-null newsletter
     */
    public void leaveNewsletter(JidProvider newsletter) {
        var request = NewsletterRequests.leaveNewsletter(newsletter.toJid());
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6392786840836363")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        sendNode(iqNode);
    }

    /**
     * Queries the value of people subscribed to a newsletter
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
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "7272540469429201")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        return sendNode(iqNode)
                .getChild("result")
                .flatMap(Node::toContentBytes)
                .flatMap(NewsletterSubscribersResponse::ofJson)
                .flatMap(NewsletterSubscribersResponse::subscribersCount);
    }

    /**
     * Sends an invitation to the value provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param admin         the new admin
     */
    public void inviteNewsletterAdmin(JidProvider newsletterJid, JidProvider admin) {
        inviteNewsletterAdmin(newsletterJid, null, admin);
    }

    /**
     * Sends an invitation to the value provided to become an admin in the newsletter
     *
     * @param newsletterJid the id of the newsletter
     * @param inviteCaption the nullable caption of the invitation
     * @param admin         the new admin
     */
    public Optional<ChatMessageInfo> inviteNewsletterAdmin(JidProvider newsletterJid, String inviteCaption, JidProvider admin) {
        var request = NewsletterRequests.createAdminInviteNewsletter(newsletterJid.toJid(), admin.toJid());
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6826078034173770")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        var expirationTimestamp = sendNode(iqNode)
                .getChild("result")
                .flatMap(Node::toContentBytes)
                .flatMap(CreateAdminInviteNewsletterResponse::ofJson)
                .map(CreateAdminInviteNewsletterResponse::expirationTime)
                .orElse(null);
        if (expirationTimestamp == null) {
            return Optional.empty();
        }

        var newsletterName = store.findNewsletterByJid(newsletterJid.toJid())
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
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6111171595650958")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        return sendNode(iqNode)
                .getChild("result")
                .flatMap(Node::toContentBytes)
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
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "7292354640794756")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        var resultNode = sendNode(iqNode);
        var result = resultNode.getChild("result");
        if (result.isEmpty()) {
            return false;
        }

        var content = result.get().toContentBytes();
        if (content.isEmpty()) {
            return false;
        }

        var jid = AcceptAdminInviteNewsletterResponse.ofJson(content.get())
                .map(AcceptAdminInviteNewsletterResponse::jid);
        if (jid.isEmpty()) {
            return false;
        }

        var newsletter = queryNewsletter(jid.get(), NewsletterViewerRole.ADMIN);
        if (newsletter.isEmpty()) {
            return false;
        }

        store.addNewsletter(newsletter.get());
        return true;
    }

    /**
     * Queries a newsletter
     *
     * @param newsletterJid the non-null value of the newsletter
     * @param role          the non-null role of the user executing the query
     */
    public Optional<Newsletter> queryNewsletter(Jid newsletterJid, NewsletterViewerRole role) {
        var request = NewsletterRequests.queryNewsletter(newsletterJid, "JID", role, true, false, true);
        var queryNode = new NodeBuilder()
                .description("query")
                .attribute("query_id", "6620195908089573")
                .content(request)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "w:mex")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(queryNode);
        var response = sendNode(iqNode);
        return response.getChild("result")
                .flatMap(Node::toContentBytes)
                .flatMap(NewsletterResponse::ofJson)
                .map(NewsletterResponse::newsletter);
    }
    //</editor-fold>  

    // <editor-fold desc="Mobile only methods">

    /**
     * Syncs any value of contacts with whatsapp
     *
     * @param contacts the contacts to sync
     * @return the contacts that were successfully synced
     */
    // TODO: Verify if this works on web as well
    public Collection<Jid> addContacts(JidProvider... contacts) {
        var users = Arrays.stream(contacts)
                .filter(entry -> entry.toJid().hasServer(JidServer.user()) && store.findContactByJid(entry).isEmpty())
                .map(contact -> contact.toJid().toPhoneNumber())
                .flatMap(Optional::stream)
                .map(phoneNumber -> {
                    var contactNode = new NodeBuilder()
                            .description("contact")
                            .content(phoneNumber.getBytes())
                            .build();
                    return new NodeBuilder()
                            .description("user")
                            .content(contactNode)
                            .build();
                })
                .toList();
        if (users.isEmpty()) {
            return List.of();
        }

        var verifiedNameNode = new NodeBuilder()
                .description("verified_name")
                .build();
        var businessNode = new NodeBuilder()
                .description("business")
                .content(verifiedNameNode)
                .build();
        var profileNode = new NodeBuilder()
                .description("profile")
                .attribute("v", 372)
                .build();
        var contactQueryNode = new NodeBuilder()
                .description("contact")
                .build();
        var devicesNode = new NodeBuilder()
                .description("devices")
                .attribute("version", "2")
                .build();
        var disappearingModeNode = new NodeBuilder()
                .description("disappearing_mode")
                .build();
        var sidelistNode = new NodeBuilder()
                .description("sidelist")
                .build();
        var statusNode = new NodeBuilder()
                .description("status")
                .build();
        var queryNode = new NodeBuilder()
                .description("query")
                .content(businessNode, profileNode, contactQueryNode, devicesNode, disappearingModeNode, sidelistNode, statusNode)
                .build();
        var listNode = new NodeBuilder()
                .description("list")
                .content(users)
                .build();
        var sideListNode = new NodeBuilder()
                .description("side_list")
                .build();
        var sync = new NodeBuilder()
                .description("usync")
                .attribute("context", "add")
                .attribute("index", "0")
                .attribute("last", "true")
                .attribute("mode", "delta")
                .attribute("sid", SecureBytes.randomSid())
                .content(queryNode, listNode, sideListNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "usync")
                .attribute("to", store.jid().orElseThrow())
                .attribute("type", "get")
                .content(sync);
        return sendNode(iqNode)
                .streamChild("usync")
                .flatMap(usync -> usync.streamChild("list"))
                .flatMap(list -> list.streamChildren("user"))
                .map(this::parseAddedContact)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Jid> parseAddedContact(Node user) {
        var contactNode = user.getChild("contact");
        if (contactNode.isPresent() && contactNode.get().hasAttribute("type", "in")) {
            return Optional.empty();
        }

        var jid = user.getAttributeAsJid("value");
        if (jid.isEmpty()) {
            return Optional.empty();
        }

        store.addNewContact(jid.get());
        return jid;
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
        if (store.clientType() != WhatsAppClientType.MOBILE) {
            throw new IllegalArgumentException("2FA is only available for the mobile api");
        }
        if (code != null && (!code.matches("^[0-9]*$") || code.length() != 6)) {
            throw new IllegalArgumentException("Invalid 2fa code: expected a numeric six digits value");
        }

        if (email != null && !EMAIL_PATTERN.matcher(email)
                .matches()) {
            throw new IllegalArgumentException("Invalid email: %s".formatted(email));
        }

        var body = new ArrayList<Node>();
        body.add(new NodeBuilder()
                .description("code")
                .content(Objects.requireNonNullElse(code, "").getBytes(StandardCharsets.UTF_8))
                .build());
        if (code != null && email != null) {
            body.add(new NodeBuilder()
                    .description("email")
                    .content(email.getBytes(StandardCharsets.UTF_8))
                    .build());
        }

        var twoFaNode = new NodeBuilder()
                .description("2fa")
                .content(body)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "urn:xmpp:whatsapp:account")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(twoFaNode);
        var result = sendNode(iqNode);
        return result.getChild("error").isEmpty();
    }

    /**
     * Starts a call with a contact
     * Mobile API only
     *
     * @param contact the non-null contact
     * @param video   whether it's a video call or an audio call
     */
    public Call startCall(JidProvider contact, boolean video) {
        if (store.clientType() != WhatsAppClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }
        addContacts(contact);
        deviceService.queryDevices(List.of(contact.toJid()));
        return sendCallMessage(contact, video);
    }

    private Call sendCallMessage(JidProvider jid, boolean video) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var callId = ChatMessageKey.randomId(store.clientType());
        var description = video ? "video" : "audio";
        var audioStream = new NodeBuilder()
                .description(description)
                .attribute("rate", 8000)
                .attribute("enc", "opus")
                .build();
        var audioStreamTwo = new NodeBuilder()
                .description(description)
                .attribute("rate", 16000)
                .attribute("enc", "opus")
                .build();
        var net = new NodeBuilder()
                .description("net")
                .attribute("medium", 3)
                .build();
        var encopt = new NodeBuilder()
                .description("encopt")
                .attribute("keygen", 2)
                .build();
        var enc = createCall(jid);
        var capability = new NodeBuilder()
                .description("capability")
                .attribute("ver", 1)
                .content(HexFormat.of().parseHex("0104ff09c4fa"))
                .build();
        var callCreator = "%s:0@s.whatsapp.net".formatted(localJid.user());
        var offer = new NodeBuilder()
                .description("offer")
                .attribute("call-creator", callCreator)
                .attribute("call-id", callId)
                .content(audioStream, audioStreamTwo, net, capability, encopt, enc)
                .build();
        var callNode = new NodeBuilder()
                .description("call")
                .attribute("to", jid)
                .content(offer);
        var result = sendNode(callNode);
        return onCallSent(localJid, jid.toJid(), callId, result);
    }

    private Call onCallSent(Jid callerJid, Jid chatJid, String callId, Node result) {
        var call = new CallBuilder()
                .chatJid(chatJid.toJid())
                .callerJid(callerJid)
                .id(callId)
                .timestampSeconds(Clock.nowSeconds())
                .video(false)
                .status(CallStatus.RINGING)
                .offline(false)
                .build();
        store.addCall(call);
        for (var listener : store.listeners()) {
            Thread.startVirtualThread(() -> listener.onCall(this, call));
        }
        return call;
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param callId the non-null id of the call to reject
     */
    public boolean stopCall(String callId) {
        if (store.clientType() != WhatsAppClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }

        return store.findCallById(callId)
                .map(this::stopCall)
                .orElse(false);
    }

    /**
     * Rejects an incoming call or stops an active call
     * Mobile API only
     *
     * @param call the non-null call to reject
     */
    public boolean stopCall(Call call) {
        var localJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        if (store.clientType() != WhatsAppClientType.MOBILE) {
            throw new IllegalArgumentException("Calling is only available for the mobile api");
        }

        if (Objects.equals(call.callerJid().withoutData(), localJid.withoutData())) {
            var rejectNode = new NodeBuilder()
                    .description("terminate")
                    .attribute("reason", "timeout")
                    .attribute("call-id", call.id())
                    .attribute("call-creator", call.callerJid())
                    .build();
            var body = new NodeBuilder()
                    .description("call")
                    .attribute("to", call.chatJid())
                    .content(rejectNode);
            return !sendNode(body)
                    .hasChild("error");
        } else {
            var rejectNode = new NodeBuilder()
                    .description("reject")
                    .attribute("call-id", call.id())
                    .attribute("call-creator", call.callerJid())
                    .attribute("count", 0)
                    .build();
            var body = new NodeBuilder()
                    .description("call")
                    .attribute("from", localJid)
                    .attribute("to", call.callerJid())
                    .content(rejectNode);
            return !sendNode(body)
                    .hasChild("error");
        }
    }
    //</editor-fold>

    //<editor-fold desc="Listeners">

    /**
     * Registers a listener
     *
     * @param listener the listener to register
     * @return the same instance
     */
    public WhatsAppClient addListener(WhatsAppClientListener listener) {
        store.addListener(listener);
        return this;
    }

    /**
     * Unregisters a listener
     *
     * @param listener the listener to unregister
     * @return the same instance
     */
    public WhatsAppClient removeListener(WhatsAppClientListener listener) {
        store.removeListener(listener);
        return this;
    }

    // Start of generated code from it.auties.whatsapp.routine.GenerateListenersLambda
    public WhatsAppClient addContactsListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Collection<Contact>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onContacts(WhatsAppClient arg0, Collection<Contact> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addChatsListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Collection<Chat>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onChats(WhatsAppClient arg0, Collection<Chat> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addNodeSentListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNodeSent(WhatsAppClient arg0, Node arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addLoggedInListener(WhatsappClientListenerConsumer.Unary<WhatsAppClient> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onLoggedIn(WhatsAppClient arg0) {
                consumer.accept(arg0);
            }
        });
        return this;
    }

    public WhatsAppClient addNewMessageListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient arg0, MessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addMessageReplyListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, MessageInfo, QuotedMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onMessageReply(WhatsAppClient arg0, MessageInfo arg1, QuotedMessageInfo arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addWebHistorySyncMessagesListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, Chat, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebHistorySyncMessages(WhatsAppClient arg0, Chat arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addMessageStatusListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, MessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onMessageStatus(WhatsAppClient arg0, MessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addContactPresenceListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, Jid, Jid> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onContactPresence(WhatsAppClient arg0, Jid arg1, Jid arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addNewslettersListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Collection<Newsletter>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNewsletters(WhatsAppClient arg0, Collection<Newsletter> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addProfilePictureChangedListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Jid> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onProfilePictureChanged(WhatsAppClient arg0, Jid arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addLocaleChangedListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onLocaleChanged(WhatsAppClient arg0, String arg1, String arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addNewContactListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Contact> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNewContact(WhatsAppClient arg0, Contact arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addWebHistorySyncProgressListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, Integer, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebHistorySyncProgress(WhatsAppClient arg0, int arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addNewStatusListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNewStatus(WhatsAppClient arg0, ChatMessageInfo arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addRegistrationCodeListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Long> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onRegistrationCode(WhatsAppClient arg0, long arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addNameChangedListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNameChanged(WhatsAppClient arg0, String arg1, String arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addContactBlockedListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Jid> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onContactBlocked(WhatsAppClient arg0, Jid arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addAboutChangedListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, String, String> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onAboutChanged(WhatsAppClient arg0, String arg1, String arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addPrivacySettingChangedListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, PrivacySettingEntry> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onPrivacySettingChanged(WhatsAppClient arg0, PrivacySettingEntry arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addWebAppPrimaryFeaturesListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, List<String>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebAppPrimaryFeatures(WhatsAppClient arg0, List<String> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addMessageDeletedListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, MessageInfo, Boolean> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onMessageDeleted(WhatsAppClient arg0, MessageInfo arg1, boolean arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addStatusListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Collection<ChatMessageInfo>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onStatus(WhatsAppClient arg0, Collection<ChatMessageInfo> arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addCallListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Call> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onCall(WhatsAppClient arg0, Call arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addWebAppStateSettingListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Setting> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebAppStateSetting(WhatsAppClient arg0, Setting arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addNodeReceivedListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, Node> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onNodeReceived(WhatsAppClient arg0, Node arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addDisconnectedListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, WhatsAppClientDisconnectReason> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onDisconnected(WhatsAppClient arg0, WhatsAppClientDisconnectReason arg1) {
                consumer.accept(arg0, arg1);
            }
        });
        return this;
    }

    public WhatsAppClient addWebAppStateActionListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, Action, MessageIndexInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebAppStateAction(WhatsAppClient arg0, Action arg1, MessageIndexInfo arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }

    public WhatsAppClient addWebHistorySyncPastParticipantsListener(WhatsappClientListenerConsumer.Ternary<WhatsAppClient, Jid, Collection<ChatPastParticipant>> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        addListener(new WhatsAppClientListener() {
            @Override
            public void onWebHistorySyncPastParticipants(WhatsAppClient arg0, Jid arg1, Collection<ChatPastParticipant> arg2) {
                consumer.accept(arg0, arg1, arg2);
            }
        });
        return this;
    }
    // End of generated code from it.auties.whatsapp.routine.GenerateListenersLambda

    public WhatsAppClient addMessageReplyListener(ChatMessageInfo info, Consumer<MessageInfo> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public WhatsAppClient addMessageReplyListener(ChatMessageInfo info, BiConsumer<WhatsAppClient, MessageInfo> onMessageReply) {
        return addMessageReplyListener(info.id(), onMessageReply);
    }

    public WhatsAppClient addMessageReplyListener(String id, Consumer<MessageInfo> consumer) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                var quotedMessageId = info.quotedMessage()
                        .map(QuotedMessageInfo::id)
                        .orElse(null);
                if (id.equals(quotedMessageId)) {
                    consumer.accept(info);
                }
            }
        });
    }

    public WhatsAppClient addMessageReplyListener(String id, BiConsumer<WhatsAppClient, MessageInfo> consumer) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                var quotedMessageId = info.quotedMessage()
                        .map(QuotedMessageInfo::id)
                        .orElse(null);
                if (id.equals(quotedMessageId)) {
                    consumer.accept(whatsapp, info);
                }
            }
        });
    }

    public WhatsAppClient addNewChatMessageListener(WhatsappClientListenerConsumer.Unary<ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                if (info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(chatMessageInfo);
                }
            }
        });
    }

    public WhatsAppClient addNewChatMessageListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, ChatMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                if (info instanceof ChatMessageInfo chatMessageInfo) {
                    consumer.accept(whatsapp, chatMessageInfo);
                }
            }
        });
    }

    public WhatsAppClient addNewNewsletterMessageListener(WhatsappClientListenerConsumer.Unary<NewsletterMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                if (info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(newsletterMessageInfo);
                }
            }
        });
    }

    public WhatsAppClient addNewNewsletterMessageListener(WhatsappClientListenerConsumer.Binary<WhatsAppClient, NewsletterMessageInfo> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        return addListener(new WhatsAppClientListener() {
            @Override
            public void onNewMessage(WhatsAppClient whatsapp, MessageInfo info) {
                if (info instanceof NewsletterMessageInfo newsletterMessageInfo) {
                    consumer.accept(whatsapp, newsletterMessageInfo);
                }
            }
        });
    }
    //</editor-fold>

    public void pushWebAppState(PatchType type, List<PendingMutation> patches) {
        webAppStateService.pushPatches(type, patches);
    }

    public void pullWebAppState(PatchType... patches) {
        webAppStateService.pullPatches(patches);
    }

    private void updateBusinessCertificate(String newName) {
        var details = new BusinessVerifiedNameDetailsBuilder()
                .name(Objects.requireNonNullElse(newName, store.name()))
                .issuer("smb:wa")
                .serial(Math.abs(ThreadLocalRandom.current().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameDetailsSpec.encode(details);
        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .encodedDetails(encodedDetails)
                .signature(Curve25519.sign(store.identityKeyPair().privateKey().toEncodedPoint(), encodedDetails))
                .build();
        var verifiedNameRequest = new NodeBuilder()
                .description("verified_name")
                .attribute("v", 2)
                .content(BusinessVerifiedNameCertificateSpec.encode(certificate))
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "w:biz")
                .content(verifiedNameRequest);
        var verifiedName = sendNode(queryRequest)
                .getChild("verified_name")
                .flatMap(node -> node.getAttributeAsString("id"))
                .orElse("");
        store.setVerifiedName(verifiedName);
    }

    public void sendAck(Node node) {
        var id = node.getRequiredAttributeAsString("id");
        sendAck(id, node);
    }

    public void sendAck(String id, Node node) {
        var ackBuilder = new NodeBuilder()
                .description("ack")
                .attribute("id", id);

        var ackClass = node.description();
        var isMessage = ackClass.equals("message");
        ackBuilder.attribute("class", ackClass);

        var ackTo = node.getRequiredAttributeAsJid("from");
        ackBuilder.attribute("to", ackTo);

        node.getAttributeAsJid("participant")
                .map(jid -> jid.hasServer(JidServer.bot()) && isMessage ? MetaBots.translate(jid) : jid)
                .ifPresent(receiptParticipant -> ackBuilder.attribute("recipient", receiptParticipant));

        if (!isMessage) {
            node.getAttributeAsString("type")
                    .ifPresent(type -> ackBuilder.attribute("type", type));
        }

        sendNodeWithNoResponse(ackBuilder.build());
    }

    public void sendPreKeys(long keysCount) {
        keysCount = Math.max(keysCount, MIN_PRE_KEYS_COUNT);
        var startId = store.hasPreKeys() ? store.preKeys().getLast().id() + 1 : 1;
        var listBody = new ArrayList<Node>();
        var preKeys = new ArrayList<SignalPreKeyPair>();
        while (keysCount-- > 0) {
            var preKeyPair = SignalPreKeyPair.random(startId++);
            preKeys.add(preKeyPair);
            var id = new NodeBuilder()
                    .description("id")
                    .content(SecureBytes.intToBytes(preKeyPair.id(), 3))
                    .build();
            var value = new NodeBuilder()
                    .description("value")
                    .content(preKeyPair.publicKey().toEncodedPoint())
                    .build();
            var preKayNode = new NodeBuilder()
                    .description("key")
                    .content(id, value)
                    .build();
            listBody.add(preKayNode);
        }
        var registration = new NodeBuilder()
                .description("registration")
                .content(SecureBytes.intToBytes(store.registrationId(), 4))
                .build();
        var type = new NodeBuilder()
                .description("type")
                .content(SIGNAL_KEY_TYPE)
                .build();
        var identity = new NodeBuilder()
                .description("identity")
                .content(store.identityKeyPair().publicKey().toEncodedPoint())
                .build();
        var list = new NodeBuilder()
                .description("list")
                .content(listBody)
                .build();
        var skeyId = new NodeBuilder()
                .description("id")
                .content(SecureBytes.intToBytes(store.signedKeyPair().id(), 3))
                .build();
        var skeyValue = new NodeBuilder()
                .description("value")
                .content(store.signedKeyPair().publicKey().toEncodedPoint())
                .build();
        var skeySignature = new NodeBuilder()
                .description("signature")
                .content(store.signedKeyPair().signature())
                .build();
        var skey = new NodeBuilder()
                .description("skey")
                .content(skeyId, skeyValue, skeySignature)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .attribute("xmlns", "encrypt")
                .content(registration, type, identity, list, skey);
        sendNode(queryRequest);
        for (var preKey : preKeys) {
            store.addPreKey(preKey);
        }
    }

    public void sendReceipt(String id, Jid parentJid, Jid senderJid, boolean peer) {
        var me = store.jid()
                .orElseThrow(() -> new IllegalStateException("No jid"));
        var fromMe = Objects.equals(me, senderJid);

        var receiptBuilder = new NodeBuilder()
                .description("receipt")
                .attribute("id", id);

        if(peer) {
            receiptBuilder.attribute("type", "peer_msg");
        } else if (fromMe) {
            receiptBuilder.attribute("type", "sender");
        } else if (!store.automaticMessageReceipts()) {
            receiptBuilder.attribute("type", "inactive");
        }

        if (parentJid.hasServer(JidServer.groupOrCommunity())) {
            receiptBuilder.attribute("to", parentJid);
            receiptBuilder.attribute("participant", senderJid);
        } else if (fromMe) {
            receiptBuilder.attribute("to", parentJid);
            receiptBuilder.attribute("recipient", senderJid);
        } else {
            receiptBuilder.attribute("to", senderJid);
        }

        sendNodeWithNoResponse(receiptBuilder.build());
    }

    public void sendReceipt(String id, Jid from, String type) {
        var me = store.jid()
                .orElse(null);
        if (me == null) {
            return;
        }

        var receipt = new NodeBuilder()
                .description("receipt")
                .attribute("id", id)
                .attribute("type", type)
                .attribute("to", from)
                .build();
        sendNodeWithNoResponse(receipt);
    }

    // TODO: Stuff to fix

    private Node createCall(JidProvider jid) {
        return null;
    }

    public SequencedCollection<Newsletter> queryNewsletters() {
        return List.of();
    }

    public SequencedCollection<Chat> queryGroups() {
        return List.of();
    }
}
