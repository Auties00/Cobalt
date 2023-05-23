package it.auties.whatsapp.socket;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.model.action.ContactAction;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificate;
import it.auties.whatsapp.model.business.BusinessVerifiedNameDetails;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.chat.Chat.EndOfHistoryTransferType;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJid.Server;
import it.auties.whatsapp.model.contact.ContactJid.Type;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.server.DeviceSentMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.setting.EphemeralSetting;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.sync.HistorySync;
import it.auties.whatsapp.model.sync.PushName;
import it.auties.whatsapp.util.*;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.MESSAGE;
import static it.auties.whatsapp.api.ErrorHandler.Location.UNKNOWN;
import static it.auties.whatsapp.model.sync.HistorySync.Type.RECENT;
import static it.auties.whatsapp.util.Spec.Signal.*;

class MessageHandler {
    private static final int MAX_ATTEMPTS = 3;
    private static final int WEEKS_GROUP_METADATA_SYNC = 2;
    private static final int HISTORY_SYNC_TIMEOUT = 10;

    private final SocketHandler socketHandler;
    private final OrderedAsyncTaskRunner runner;
    private final Map<String, Integer> retries;
    private final Cache<ContactJid, GroupMetadata> groupsCache;
    private final Cache<String, List<ContactJid>> devicesCache;
    private final Map<ContactJid, List<PastParticipant>> pastParticipantsQueue;
    private final Set<Chat> historyCache;
    private final Logger logger;
    private final DeferredTaskRunner deferredTaskRunner;
    private final Set<ContactJid> attributedGroups;
    private CompletableFuture<?> historySyncTask;

    protected MessageHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.groupsCache = createCache(Duration.ofMinutes(5));
        this.devicesCache = createCache(Duration.ofMinutes(5));
        this.pastParticipantsQueue = new ConcurrentHashMap<>();
        this.retries = new ConcurrentHashMap<>();
        this.historyCache = ConcurrentHashMap.newKeySet();
        this.attributedGroups = ConcurrentHashMap.newKeySet();
        this.runner = new OrderedAsyncTaskRunner();
        this.logger = System.getLogger("MessageHandler");
        this.deferredTaskRunner = new DeferredTaskRunner();
    }

    private <K, V> Cache<K, V> createCache(Duration duration) {
        return Caffeine.newBuilder().expireAfterWrite(duration).build();
    }

    protected synchronized CompletableFuture<Void> encode(MessageSendRequest request) {
        return runner.runAsync(() -> encodeMessageNode(request)
                .thenRunAsync(() -> attributeOutgoingMessage(request))
                .exceptionallyAsync(throwable -> onEncodeError(request, throwable)));
    }

    private CompletableFuture<Node> encodeMessageNode(MessageSendRequest request) {
        return request.peer() || isConversation(request.info()) ? encodeConversation(request) : encodeGroup(request);
    }

    private Void onEncodeError(MessageSendRequest request, Throwable throwable) {
        request.info().status(MessageStatus.ERROR);
        return socketHandler.handleFailure(MESSAGE, throwable);
    }

    private void attributeOutgoingMessage(MessageSendRequest request) {
        if(request.peer()){
            return;
        }

        saveMessage(request.info(), "unknown", false);
        attributeMessageReceipt(request.info());
    }

    private CompletableFuture<Node> encodeGroup(MessageSendRequest request) {
        var encodedMessage = BytesHelper.messageToBytes(request.info().message());
        var senderName = new SenderKeyName(request.info().chatJid().toString(), socketHandler.store().jid().toSignalAddress());
        var groupBuilder = new GroupBuilder(socketHandler.keys());
        var signalMessage = groupBuilder.createOutgoing(senderName);
        var groupCipher = new GroupCipher(senderName, socketHandler.keys());
        var groupMessage = groupCipher.encrypt(encodedMessage);
        var messageNode = createMessageNode(request, groupMessage);
        if (request.hasSenderOverride()) {
            return getGroupRetryDevices(request.overrideSender(), request.force()).thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, request.force()))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::send);
        }

        if (request.force()) {
            return Optional.ofNullable(groupsCache.getIfPresent(request.info().chatJid()))
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> socketHandler.queryGroupMetadata(request.info().chatJid()))
                    .thenComposeAsync(devices -> getGroupDevices(devices, true))
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, true))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::send);
        }

        return socketHandler.queryGroupMetadata(request.info().chatJid())
                .thenComposeAsync(devices -> getGroupDevices(devices, false))
                .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, false))
                .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                .thenComposeAsync(socketHandler::send);
    }

    private CompletableFuture<Node> encodeConversation(MessageSendRequest request) {
        var sender = socketHandler.store().jid();
        if(sender == null){
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var encodedMessage = BytesHelper.messageToBytes(request.info().message());
        var knownDevices = getRecipients(request, sender);
        var chatJid = request.info().chatJid();
        if(request.peer()){
            var peerNode = createMessageNode(request, chatJid, encodedMessage, true);
            var encodedMessageNode = createEncodedMessageNode(request, List.of(peerNode), null);
            return socketHandler.send(encodedMessageNode);
        }

        var deviceMessage = DeviceSentMessage.of(request.info().chatJid().toString(), request.info().message(), null);
        var encodedDeviceMessage = BytesHelper.messageToBytes(deviceMessage);
        return getDevices(knownDevices, true, request.force())
                .thenComposeAsync(allDevices -> createConversationNodes(request, allDevices, encodedMessage, encodedDeviceMessage))
                .thenApplyAsync(sessions -> createEncodedMessageNode(request, sessions, null))
                .thenComposeAsync(socketHandler::send);
    }

    private List<ContactJid> getRecipients(MessageSendRequest request, ContactJid sender) {
        if(request.peer()){
            return List.of(request.info().chatJid());
        }

        if (request.hasSenderOverride()) {
            return List.of(request.overrideSender());
        }

        return List.of(sender.toWhatsappJid(), request.info().chatJid());
    }

    private boolean isConversation(MessageInfo info) {
        return info.chatJid().hasServer(Server.WHATSAPP) || info.chatJid().hasServer(Server.USER);
    }

    private Node createEncodedMessageNode(MessageSendRequest request, List<Node> preKeys, Node descriptor) {
        var body = new ArrayList<Node>();
        if (!preKeys.isEmpty()) {
            if (request.peer()) {
                body.addAll(preKeys);
            } else {
                body.add(Node.ofChildren("participants", preKeys));
            }
        }

        if (descriptor != null) {
            body.add(descriptor);
        }

        if (!request.peer() && hasPreKeyMessage(preKeys)) {
            var identity = Protobuf.writeMessage(socketHandler.keys().companionIdentity());
            body.add(Node.of("device-identity", identity));
        }

        var attributes = Attributes.ofNullable(request.additionalAttributes())
                .put("id", request.info().id())
                .put("to", request.info().chatJid())
                .put("t", request.info().timestampSeconds())
                .put("type", "text")
                .put("category", "peer", request::peer)
                .put("duration", "900", () -> request.info().message().type() == MessageType.LIVE_LOCATION)
                .put("device_fanout", false, () -> request.info().message().type() == MessageType.BUTTONS)
                .toMap();
        return Node.ofChildren("message", attributes, body);
    }

    private boolean hasPreKeyMessage(List<Node> participants) {
        return participants.stream()
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(node -> node.attributes().getOptionalString("type"))
                .flatMap(Optional::stream)
                .anyMatch("pkmsg"::equals);
    }

    private CompletableFuture<List<Node>> createConversationNodes(MessageSendRequest request, List<ContactJid> contacts, byte[] message, byte[] deviceMessage) {
        var partitioned = contacts.stream()
                .collect(Collectors.partitioningBy(contact -> Objects.equals(contact.user(), socketHandler.store().jid().user())));
        var companions = querySessions(partitioned.get(true), request.force())
                .thenApplyAsync(ignored -> createMessageNodes(request, partitioned.get(true), deviceMessage));
        var others = querySessions(partitioned.get(false), request.force())
                .thenApplyAsync(ignored -> createMessageNodes(request, partitioned.get(false), message));
        return companions.thenCombineAsync(others, (first, second) -> toSingleList(first, second));
    }

    private CompletableFuture<List<Node>> createGroupNodes(MessageSendRequest request, byte[] distributionMessage, List<ContactJid> participants, boolean force) {
        Validate.isTrue(request.info().chat().isGroup(), "Cannot send group message to non-group");
        var missingParticipants = participants.stream()
                .filter(participant -> !request.info().chat().participantsPreKeys().contains(participant))
                .toList();
        if (missingParticipants.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        var whatsappMessage = new SenderKeyDistributionMessage(request.info().chatJid().toString(), distributionMessage);
        var paddedMessage = BytesHelper.messageToBytes(whatsappMessage);
        return querySessions(missingParticipants, force).thenApplyAsync(ignored -> createMessageNodes(request, missingParticipants, paddedMessage))
                .thenApplyAsync(results -> savePreKeys(request.info().chat(), missingParticipants, results));
    }

    private List<Node> savePreKeys(Chat group, List<ContactJid> missingParticipants, List<Node> results) {
        group.participantsPreKeys().addAll(missingParticipants);
        return results;
    }

    protected CompletableFuture<Void> querySessions(List<ContactJid> contacts, boolean force) {
        var missingSessions = contacts.stream()
                .filter(contact -> force || !socketHandler.keys().hasSession(contact.toSignalAddress()))
                .map(contact -> Node.ofAttributes("user", Map.of("jid", contact)))
                .toList();
        return missingSessions.isEmpty() ? CompletableFuture.completedFuture(null) : querySession(missingSessions);
    }

    private CompletableFuture<Void> querySession(List<Node> children){
        return socketHandler.sendQuery("get", "encrypt", Node.ofChildren("key", children))
                .thenAcceptAsync(this::parseSessions);
    }

    private List<Node> createMessageNodes(MessageSendRequest request, List<ContactJid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(request, contact, message, false))
                .toList();
    }

    private Node createMessageNode(MessageSendRequest request, ContactJid contact, byte[] message, boolean peer) {
        var cipher = new SessionCipher(contact.toSignalAddress(), socketHandler.keys());
        var encrypted = cipher.encrypt(message);
        var messageNode = createMessageNode(request, encrypted);
        return peer ? messageNode : Node.ofChildren("to", Map.of("jid", contact), messageNode);
    }

    private CompletableFuture<List<ContactJid>> getGroupRetryDevices(ContactJid contactJid, boolean force) {
        return getDevices(List.of(contactJid), false, force);
    }

    private CompletableFuture<List<ContactJid>> getGroupDevices(GroupMetadata metadata, boolean force) {
        groupsCache.put(metadata.jid(), metadata);
        return getDevices(metadata.participantsJids(), false, force);
    }

    protected CompletableFuture<List<ContactJid>> getDevices(List<ContactJid> contacts, boolean excludeSelf, boolean force) {
        if (force) {
            return queryDevices(contacts, excludeSelf)
                    .thenApplyAsync(missingDevices -> excludeSelf ? toSingleList(contacts, missingDevices) : missingDevices);
        }
        var partitioned = contacts.stream()
                .collect(Collectors.partitioningBy(contact -> devicesCache.asMap().containsKey(contact.user()), Collectors.toUnmodifiableList()));
        var cached = partitioned.get(true)
                .stream()
                .map(ContactJid::user)
                .map(devicesCache::getIfPresent)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
        var missing = partitioned.get(false);
        if (missing.isEmpty()) {
            return CompletableFuture.completedFuture(excludeSelf ? toSingleList(contacts, cached) : cached);
        }
        return queryDevices(missing, excludeSelf)
                .thenApplyAsync(missingDevices -> excludeSelf ? toSingleList(contacts, cached, missingDevices) : toSingleList(cached, missingDevices));
    }

    private CompletableFuture<List<ContactJid>> queryDevices(List<ContactJid> contacts, boolean excludeSelf) {
        var contactNodes = contacts.stream()
                .map(contact -> Node.ofAttributes("user", Map.of("jid", contact)))
                .toList();
        var body = Node.ofChildren("usync",
                Map.of("sid", socketHandler.store().nextTag(), "mode", "query", "last", "true", "index", "0", "context", "message"),
                Node.ofChildren("query", Node.ofAttributes("devices", Map.of("version", "2"))),
                Node.ofChildren("list", contactNodes));
        return socketHandler.sendQuery("get", "usync", body)
                .thenApplyAsync(result -> parseDevices(result, excludeSelf));
    }

    private List<ContactJid> parseDevices(Node node, boolean excludeSelf) {
        var results = node.children()
                .stream()
                .map(child -> child.findNode("list"))
                .flatMap(Optional::stream)
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(entry -> parseDevice(entry, excludeSelf))
                .flatMap(Collection::stream)
                .toList();
        devicesCache.putAll(results.stream().collect(Collectors.groupingBy(ContactJid::user)));
        return results;
    }

    private List<ContactJid> parseDevice(Node wrapper, boolean excludeSelf) {
        var jid = wrapper.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid for sync device"));
        return wrapper.findNode("devices")
                .orElseThrow(() -> new NoSuchElementException("Missing devices"))
                .findNode("device-list")
                .orElseThrow(() -> new NoSuchElementException("Missing device list"))
                .children()
                .stream()
                .map(child -> parseDeviceId(child, jid, excludeSelf))
                .flatMap(Optional::stream)
                .map(id -> ContactJid.ofDevice(jid.user(), id))
                .toList();
    }

    private Optional<Integer> parseDeviceId(Node child, ContactJid jid, boolean excludeSelf) {
        var deviceId = child.attributes().getInt("id");
        return child.description().equals("device") && (!excludeSelf || deviceId != 0) && (!jid.user()
                .equals(socketHandler.store().jid().user()) || socketHandler.store()
                .jid()
                .device() != deviceId) && (deviceId == 0 || child.attributes()
                .hasKey("key-index")) ? Optional.of(deviceId) : Optional.empty();
    }

    protected void parseSessions(Node node) {
        node.findNode("list")
                .orElseThrow(() -> new NoSuchElementException("Missing list: %s".formatted(node)))
                .findNodes("user")
                .forEach(this::parseSession);
    }

    private void parseSession(Node node) {
        Validate.isTrue(!node.hasNode("error"), "Erroneous session node", SecurityException.class);
        var jid = node.attributes()
                .getJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing jid for session"));
        var registrationId = node.findNode("registration")
                .map(id -> BytesHelper.bytesToInt(id.contentAsBytes().orElseThrow(), 4))
                .orElseThrow(() -> new NoSuchElementException("Missing id"));
        var identity = node.findNode("identity")
                .flatMap(Node::contentAsBytes)
                .map(KeyHelper::withHeader)
                .orElseThrow(() -> new NoSuchElementException("Missing identity"));
        var signedKey = node.findNode("skey")
                .flatMap(SignalSignedKeyPair::of)
                .orElseThrow(() -> new NoSuchElementException("Missing signed key"));
        var key = node.findNode("key")
                .flatMap(SignalSignedKeyPair::of)
                .orElse(null);
        var builder = new SessionBuilder(jid.toSignalAddress(), socketHandler.keys());
        builder.createOutgoing(registrationId, identity, signedKey, key);
    }

    public synchronized void decode(Node node) {
        try {
            var businessName = getBusinessName(node);
            var encrypted = node.findNodes("enc");
            if (node.hasNode("unavailable") && !node.hasNode("enc")) {
                decodeMessage(node, null, businessName);
                return;
            }
            encrypted.forEach(message -> decodeMessage(node, message, businessName));
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
        }
    }

    private String getBusinessName(Node node) {
        return node.attributes()
                .getOptionalString("verified_name")
                .or(() -> getBusinessNameFromNode(node))
                .orElse(null);
    }

    private static Optional<String> getBusinessNameFromNode(Node node) {
        return node.findNode("verified_name")
                .flatMap(Node::contentAsBytes)
                .map(bytes -> Protobuf.readMessage(bytes, BusinessVerifiedNameCertificate.class))
                .map(certificate -> Protobuf.readMessage(certificate.details(), BusinessVerifiedNameDetails.class))
                .map(BusinessVerifiedNameDetails::name);
    }

    private Node createMessageNode(MessageSendRequest request, CipheredMessageResult groupMessage) {
        var mediaType = getMediaType(request.info().message());
        var attributes = Attributes.of()
                .put("v", "2")
                .put("type", groupMessage.type())
                .put("mediatype", mediaType, Objects::nonNull)
                .toMap();
        return Node.of("enc", attributes, groupMessage.message());
    }

    private String getMediaType(MessageContainer container) {
        var content = container.content();
        if (content instanceof ImageMessage) {
            return "image";
        } else if (content instanceof VideoMessage videoMessage) {
            return videoMessage.gifPlayback() ? "gif" : "video";
        } else if (content instanceof AudioMessage audioMessage) {
            return audioMessage.voiceMessage() ? "ptt" : "audio";
        } else if (content instanceof ContactMessage) {
            return "vcard";
        } else if (content instanceof DocumentMessage) {
            return "document";
        } else if (content instanceof ContactsArrayMessage) {
            return "contact_array";
        } else if (content instanceof LiveLocationMessage) {
            return "livelocation";
        } else if (content instanceof StickerMessage) {
            return "sticker";
        } else if (content instanceof ListMessage) {
            return "list";
        } else if (content instanceof ListResponseMessage) {
            return "list_response";
        } else if (content instanceof ButtonsResponseMessage) {
            return "buttons_response";
        } else if (content instanceof PaymentOrderMessage) {
            return "order";
        } else if (content instanceof ProductMessage) {
            return "product";
        } else if (content instanceof NativeFlowResponseMessage) {
            return "native_flow_response";
        } else if (content instanceof ButtonsMessage buttonsMessage) {
            return buttonsMessage.headerType().hasMedia() ? buttonsMessage.headerType().name().toLowerCase() : null;
        } else {
            return null;
        }
    }

    private void decodeMessage(Node infoNode, Node messageNode, String businessName) {
        try {
            var offline = infoNode.attributes().hasKey("offline");
            var pushName = infoNode.attributes().getNullableString("notify");
            var timestamp = infoNode.attributes().getLong("t");
            var id = infoNode.attributes().getRequiredString("id");
            var from = infoNode.attributes()
                    .getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var recipient = infoNode.attributes().getJid("recipient").orElse(from);
            var participant = infoNode.attributes().getJid("participant").orElse(null);
            var messageBuilder = MessageInfo.builder();
            var keyBuilder = MessageKey.builder();
            var userCompanionJid = socketHandler.store().jid();
            if(userCompanionJid == null){
                return; // This means that the session got disconnected while processing
            }
            var receiver = userCompanionJid.toWhatsappJid();
            if (from.hasServer(ContactJid.Server.WHATSAPP) || from.hasServer(ContactJid.Server.USER)) {
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from, receiver));
                messageBuilder.senderJid(from);
            } else {
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.toWhatsappJid(), receiver));
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }
            var key = keyBuilder.id(id).build();
            if(isSelfMessage(key)){
                socketHandler.sendReceipt(key.chatJid(), key.senderJid().orElse(key.chatJid()), List.of(key.id()), null);
                return;
            }

            if (messageNode == null) {
                if(sendRetryReceipt(timestamp, id, from, recipient, participant, null, null, null)){
                    return;
                }

                socketHandler.sendReceipt(key.chatJid(), key.senderJid().orElse(key.chatJid()), List.of(key.id()), null);
                return;
            }

            var type = messageNode.attributes().getRequiredString("type");
            var encodedMessage = messageNode.contentAsBytes().orElse(null);
            var decodedMessage = decodeMessageBytes(type, encodedMessage, from, participant);
            if (decodedMessage.hasError()) {
                if(sendRetryReceipt(timestamp, id, from, recipient, participant, type, encodedMessage, decodedMessage)){
                    return;
                }

                socketHandler.sendReceipt(key.chatJid(), key.senderJid().orElse(key.chatJid()), List.of(key.id()), null);
                return;
            }

            var messageContainer = BytesHelper.bytesToMessage(decodedMessage.message()).unbox();
            var info = messageBuilder.key(key)
                    .broadcast(key.chatJid().hasServer(Server.BROADCAST))
                    .pushName(pushName)
                    .status(MessageStatus.DELIVERED)
                    .businessVerifiedName(businessName)
                    .timestampSeconds(timestamp)
                    .message(messageContainer)
                    .build();
            attributeMessageReceipt(info);
            socketHandler.store().attribute(info);
            var category = infoNode.attributes().getString("category");
            saveMessage(info, category, offline);
            socketHandler.sendReceipt(info.chatJid(), info.senderJid(), List.of(info.key().id()), null);
            socketHandler.onReply(info);
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
        }
    }

    private boolean isSelfMessage(MessageKey key) {
        return socketHandler.store().clientType() == ClientType.APP_CLIENT
                && key.fromMe()
                && key.senderJid().isPresent()
                && !key.senderJid().get().hasAgent();
    }

    private boolean sendRetryReceipt(long timestamp, String id, ContactJid from, ContactJid recipient, ContactJid participant, String type, byte[] encodedMessage, MessageDecodeResult decodedMessage) {
        if(encodedMessage != null) {
            logger.log(Level.WARNING, "Cannot decode message(id: %s, from: %s): %s".formatted(id, from, decodedMessage == null ? "unknown error" : decodedMessage.error().getMessage()));
        }

        if(socketHandler.store().clientType() == ClientType.APP_CLIENT){
            return false;
        }

        var attempts = retries.getOrDefault(id, 0);
        if (attempts >= MAX_ATTEMPTS) {
            var cause = decodedMessage != null ? decodedMessage.error() : new RuntimeException("This message is not available");
            socketHandler.handleFailure(MESSAGE, new RuntimeException("Cannot decrypt message with type %s inside %s from %s".formatted(Objects.requireNonNullElse(type, "unknown"), from, Objects.requireNonNullElse(participant, from)), cause));
            return false;
        }

        var retryAttributes = Attributes.of()
                .put("id", id)
                .put("type", "retry")
                .put("to", from)
                .put("recipient", recipient, () -> !Objects.equals(recipient, from))
                .put("participant", participant, Objects::nonNull)
                .toMap();
        var retryNode = Node.ofChildren("receipt", retryAttributes,
                Node.ofAttributes("retry", Map.of("count", attempts, "id", id, "t", timestamp, "v", "1")),
                Node.of("registration", socketHandler.keys().encodedRegistrationId()),
                attempts > 1 || encodedMessage == null ? createPreKeyNode() : null);
        socketHandler.sendWithNoResponse(retryNode);
        retries.put(id, attempts + 1);
        return true;
    }

    private MessageDecodeResult decodeMessageBytes(String type, byte[] encodedMessage, ContactJid from, ContactJid participant) {
        try {
            if (encodedMessage == null) {
                return new MessageDecodeResult(null, new IllegalArgumentException("Missing encoded message"));
            }
            var result = switch (type) {
                case SKMSG -> {
                    Objects.requireNonNull(participant, "Cannot decipher skmsg without participant");
                    var senderName = new SenderKeyName(from.toString(), participant.toSignalAddress());
                    var signalGroup = new GroupCipher(senderName, socketHandler.keys());
                    yield signalGroup.decrypt(encodedMessage);
                }
                case PKMSG -> {
                    var user = from.hasServer(ContactJid.Server.WHATSAPP) ? from : participant;
                    Objects.requireNonNull(user, "Cannot decipher pkmsg without user");
                    var session = new SessionCipher(user.toSignalAddress(), socketHandler.keys());
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(preKey);
                }
                case MSG -> {
                    var user = from.hasServer(ContactJid.Server.WHATSAPP) ? from : participant;
                    Objects.requireNonNull(user, "Cannot decipher msg without user");
                    var session = new SessionCipher(user.toSignalAddress(), socketHandler.keys());
                    var signalMessage = SignalMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(signalMessage);
                }
                default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
            };
            return new MessageDecodeResult(result, null);
        } catch (Throwable throwable) {
            return new MessageDecodeResult(null, throwable);
        }
    }

    private void attributeMessageReceipt(MessageInfo info) {
        var self = socketHandler.store().jid().toWhatsappJid();
        if (!info.fromMe() || !info.chatJid().equals(self)) {
            return;
        }
        info.receipt().readTimestampSeconds(info.timestampSeconds());
        info.receipt().deliveredJids().add(self);
        info.receipt().readJids().add(self);
        info.status(MessageStatus.READ);
    }

    private void saveMessage(MessageInfo info, String category, boolean offline) {
        if(info.message().content() instanceof SenderKeyDistributionMessage distributionMessage) {
            handleDistributionMessage(distributionMessage, info.senderJid());
        }
        if (info.chatJid().type() == Type.STATUS) {
            socketHandler.store().addStatus(info);
            socketHandler.onNewStatus(info);
            return;
        }
        if (info.message().hasCategory(MessageCategory.SERVER)) {
            if (info.message().content() instanceof ProtocolMessage protocolMessage) {
                onProtocolMessage(info, protocolMessage, Objects.equals(category, "peer"));
            }
            return;
        }
        var result = info.chat().addNewMessage(info);
        if (!result || info.timestampSeconds() <= socketHandler.store().initializationTimeStamp()) {
            return;
        }
        if (info.chat().archived() && socketHandler.store().unarchiveChats()) {
            info.chat().archived(false);
        }
        info.sender()
                .filter(this::isTyping)
                .ifPresent(sender -> socketHandler.onUpdateChatPresence(ContactStatus.AVAILABLE, sender, info.chat()));
        if (!info.ignore() && !info.fromMe()) {
            info.chat().unreadMessagesCount(info.chat().unreadMessagesCount() + 1);
        }
        socketHandler.onNewMessage(info, offline);
    }

    private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, ContactJid from) {
        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var builder = new GroupBuilder(socketHandler.keys());
        var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
        builder.createIncoming(groupName, message);
    }

    private Node createPreKeyNode() {
        var preKey = SignalPreKeyPair.random(socketHandler.keys().lastPreKeyId() + 1);
        var identity = Protobuf.writeMessage(socketHandler.keys().companionIdentity());
        return Node.ofChildren("keys",
                Node.of("type", Spec.Signal.KEY_BUNDLE_TYPE),
                Node.of("identity", socketHandler.keys().identityKeyPair().publicKey()),
                preKey.toNode(),
                socketHandler.keys().signedKeyPair().toNode(),
                Node.of("device-identity", identity));
    }

    private void onProtocolMessage(MessageInfo info, ProtocolMessage protocolMessage, boolean peer) {
        handleProtocolMessage(info, protocolMessage);
        if (!peer) {
            return;
        }
        socketHandler.sendSyncReceipt(info, "peer_msg");
    }

    private void handleProtocolMessage(MessageInfo info, ProtocolMessage protocolMessage) {
        switch (protocolMessage.protocolType()) {
            case HISTORY_SYNC_NOTIFICATION -> onHistorySyncNotification(info, protocolMessage);
            case APP_STATE_SYNC_KEY_SHARE -> onAppStateSyncKeyShare(protocolMessage);
            case REVOKE -> onMessageRevoked(info, protocolMessage);
            case EPHEMERAL_SETTING -> onEphemeralSettings(info, protocolMessage);
        }
    }

    private void onEphemeralSettings(MessageInfo info, ProtocolMessage protocolMessage) {
        info.chat()
                .ephemeralMessagesToggleTime(info.timestampSeconds())
                .ephemeralMessageDuration(ChatEphemeralTimer.of(protocolMessage.ephemeralExpiration()));
        var setting = new EphemeralSetting((int) protocolMessage.ephemeralExpiration(), info.timestampSeconds());
        socketHandler.onSetting(setting);
    }

    private void onMessageRevoked(MessageInfo info, ProtocolMessage protocolMessage) {
        socketHandler.store()
                .findMessageById(info.chat(), protocolMessage.key().id())
                .ifPresent(message -> onMessageDeleted(info, message));
    }

    private void onAppStateSyncKeyShare(ProtocolMessage protocolMessage) {
        socketHandler.keys().addAppKeys(protocolMessage.appStateSyncKeyShare().keys());
        if (socketHandler.store().initialSync()) {
            return;
        }

        socketHandler.pullInitialPatches()
                .exceptionallyAsync(throwable -> socketHandler
                        .handleFailure(UNKNOWN, throwable));
    }

    private void onHistorySyncNotification(MessageInfo info, ProtocolMessage protocolMessage) {
        downloadHistorySync(protocolMessage)
                .thenAcceptAsync(history -> onHistoryNotification(info, history))
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(MESSAGE, throwable));
    }

    private boolean isTyping(Contact sender) {
        return sender.lastKnownPresence() == ContactStatus.COMPOSING || sender.lastKnownPresence() == ContactStatus.RECORDING;
    }

    private CompletableFuture<HistorySync> downloadHistorySync(ProtocolMessage protocolMessage) {
        return Medias.download(protocolMessage.historySyncNotification())
                .thenApplyAsync(entry -> entry.orElseThrow(() -> new NoSuchElementException("Cannot download history sync")))
                .thenApplyAsync(result -> Protobuf.readMessage(BytesHelper.decompress(result), HistorySync.class));
    }

    private void onHistoryNotification(MessageInfo info, HistorySync history) {
        handleHistorySync(history);
        if (history.progress() != null) {
            scheduleTimeoutSync(history);
            socketHandler.onHistorySyncProgress(history.progress(), history.syncType() == RECENT);
        }

        socketHandler.sendSyncReceipt(info, "hist_sync");
    }

    private void scheduleTimeoutSync(HistorySync history) {
        var executor = CompletableFuture.delayedExecutor(HISTORY_SYNC_TIMEOUT, TimeUnit.SECONDS);
        if(historySyncTask != null){
            historySyncTask.cancel(true);
        }
        this.historySyncTask = CompletableFuture.runAsync(() -> handleChatsSync(history, true), executor);
    }

    private void onMessageDeleted(MessageInfo info, MessageInfo message) {
        info.chat().removeMessage(message);
        message.revokeTimestampSeconds(Clock.nowSeconds());
        socketHandler.onMessageDeleted(message, true);
    }

    private void handleHistorySync(HistorySync history) {
        switch (history.syncType()) {
            case INITIAL_STATUS_V3 -> handleInitialStatus(history);
            case PUSH_NAME -> handlePushNames(history);
            case INITIAL_BOOTSTRAP -> handleInitialBootstrap(history);
            case RECENT, FULL -> {
                deferredTaskRunner.execute();
                handleChatsSync(history, false);
            }
            case NON_BLOCKING_DATA -> handleNonBlockingData(history);
        }
    }

    private void handleInitialStatus(HistorySync history) {
        var store = socketHandler.store();
        for (var messageInfo : history.statusV3Messages()) {
            store.addStatus(messageInfo);
        }
        socketHandler.onStatus();
    }

    private void handlePushNames(HistorySync history) {
        for (var pushName : history.pushNames()) {
            handNewPushName(pushName);
        }
        socketHandler.onContacts();
    }

    private void handNewPushName(PushName pushName) {
        var jid = ContactJid.of(pushName.id());
        var contact = socketHandler.store()
                .findContactByJid(jid)
                .orElseGet(() -> createNewContact(jid));
        contact.chosenName(pushName.name());
        var action = ContactAction.of(pushName.name(), null, null);
        socketHandler.onAction(action, MessageIndexInfo.of("contact", jid, null, true));
    }

    private Contact createNewContact(ContactJid jid) {
        var contact = socketHandler.store().addContact(jid);
        socketHandler.onNewContact(contact);
        return contact;
    }

    private void handleInitialBootstrap(HistorySync history) {
        if(socketHandler.store().historyLength() != WebHistoryLength.ZERO){
            historyCache.addAll(history.conversations());
        }

        handleConversations(history);
        socketHandler.onChats();
    }

    private void handleChatsSync(HistorySync history, boolean forceDone) {
        if(socketHandler.store().historyLength() == WebHistoryLength.ZERO){
            return;
        }

        handleConversations(history);
        for (var cached : historyCache) {
            var chat = socketHandler.store()
                    .findChatByJid(cached.jid())
                    .orElse(cached);
            var done = forceDone || !history.conversations().contains(cached);
            if(done){
                chat.endOfHistoryTransferType(EndOfHistoryTransferType.COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY);
            }
            socketHandler.onChatRecentMessages(chat, done);
        }
        historyCache.removeIf(entry -> !history.conversations().contains(entry));
    }

    private void handleConversations(HistorySync history) {
        var store = socketHandler.store();
        for (var chat : history.conversations()) {
            var pastParticipants = pastParticipantsQueue.remove(chat.jid());
            if (pastParticipants != null) {
                chat.addPastParticipants(pastParticipants);
            }
            if(shouldSyncGroupMetadata(chat)){
                attributedGroups.add(chat.jid());
                deferredTaskRunner.schedule(() -> socketHandler.queryGroupMetadata(chat));
            }

            store.addChat(chat);
        }
    }

    private boolean shouldSyncGroupMetadata(Chat chat) {
        return chat.isGroup()
                && !attributedGroups.contains(chat.jid())
                && chat.timestamp().until(ZonedDateTime.now(), ChronoUnit.WEEKS) < WEEKS_GROUP_METADATA_SYNC;
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            handlePastParticipants(pastParticipants);
        }
    }

    private void handlePastParticipants(PastParticipants pastParticipants) {
        socketHandler.store()
                .findChatByJid(pastParticipants.groupJid())
                .ifPresentOrElse(chat -> chat.addPastParticipants(pastParticipants.pastParticipants()),
                        () ->  pastParticipantsQueue.put(pastParticipants.groupJid(), pastParticipants.pastParticipants()));
    }

    @SafeVarargs
    private <T> List<T> toSingleList(List<T>... all) {
        return Stream.of(all)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    protected void dispose() {
        retries.clear();
        groupsCache.invalidateAll();
        devicesCache.invalidateAll();
        historyCache.clear();
        runner.cancel();
        historySyncTask = null;
    }

    private record MessageDecodeResult(byte[] message, Throwable error) {
        public boolean hasError() {
            return error != null;
        }
    }
}
