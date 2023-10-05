package it.auties.whatsapp.socket;

import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.model.action.ContactAction;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.info.MessageInfoBuilder;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.jid.JidType;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.model.reserved.LocalMediaMessage;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.server.DeviceSentMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.setting.EphemeralSettings;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentitySpec;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.sync.HistorySync;
import it.auties.whatsapp.model.sync.HistorySync.Type;
import it.auties.whatsapp.model.sync.HistorySyncNotification;
import it.auties.whatsapp.model.sync.HistorySyncSpec;
import it.auties.whatsapp.model.sync.PushName;
import it.auties.whatsapp.util.*;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static it.auties.whatsapp.util.Specification.Signal.*;

class MessageHandler {
    private static final int HISTORY_SYNC_TIMEOUT = 25;

    private final SocketHandler socketHandler;
    private final Map<Jid, List<GroupPastParticipant>> pastParticipantsQueue;
    private final Set<Jid> historyCache;
    private final Logger logger;
    private final EnumSet<Type> historySyncTypes;
    private final ReentrantLock lock;
    private ExecutorService executor;
    private CompletableFuture<?> historySyncTask;

    protected MessageHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.pastParticipantsQueue = new ConcurrentHashMap<>();
        this.historyCache = ConcurrentHashMap.newKeySet();
        this.logger = System.getLogger("MessageHandler");
        this.historySyncTypes = EnumSet.noneOf(Type.class);
        this.lock = new ReentrantLock(true);
    }

    protected CompletableFuture<Void> encode(MessageSendRequest request) {
        return encodeMessage(request)
                .thenRunAsync(() -> attributeOutgoingMessage(request))
                .exceptionallyAsync(throwable -> onEncodeError(request, throwable));
    }

    private CompletableFuture<?> encodeMessage(MessageSendRequest request) {
        return request.info().chatJid().hasServer(JidServer.CHANNEL) ? encodePlainMessage(request) : encodeE2EMessage(request);
    }

    private CompletableFuture<Void> encodePlainMessage(MessageSendRequest request) {
        var message = request.info().message();
        var messageNode = getPlainMessageNode(message, request.additionalAttributes());
        var type = message.content().type() == MessageType.TEXT ? "text" : "media";
        var attributes = Attributes.of()
                .put("id", request.info().id())
                .put("to", request.info().chatJid())
                .put("type", type)
                .put("media_id", getPlainMessageHandle(request), Objects::nonNull)
                .toMap();
        return socketHandler.send(Node.of("message", attributes, messageNode))
                .thenRunAsync(() -> attributeOutgoingMessage(request));
    }

    private String getPlainMessageHandle(MessageSendRequest request) {
        var message = request.info().message().content();
        if (!(message instanceof LocalMediaMessage<?> localMediaMessage)) {
            return null;
        }

        return localMediaMessage.handle().orElse(null);
    }

    private Node getPlainMessageNode(MessageContainer message, Map<String, ?> additionalAttributes) {
        var messageAttributes = Attributes.ofNullable(additionalAttributes)
                .put("mediatype", getMediaType(message), Objects::nonNull)
                .toMap();
        return switch (message.content()) {
            case TextMessage textMessage -> {
                var byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(10);
                byteArrayOutputStream.writeBytes(BytesHelper.intToVarInt(textMessage.text().length()));
                byteArrayOutputStream.writeBytes(textMessage.text().getBytes(StandardCharsets.UTF_8));
                yield Node.of("plaintext", byteArrayOutputStream.toByteArray());
            }
            case ReactionMessage reactionMessage -> Node.of("reaction", Map.of("code", reactionMessage.content()));
            default -> Node.of("plaintext", messageAttributes, MessageContainerSpec.encode(message));
        };
    }

    private CompletableFuture<Node> encodeE2EMessage(MessageSendRequest request) {
        try {
            lock.lock();
            return request.peer() || isConversation(request.info()) ? encodeConversation(request) : encodeGroup(request);
        }finally {
            lock.unlock();
        }
    }

    private Void onEncodeError(MessageSendRequest request, Throwable throwable) {
        request.info().setStatus(MessageStatus.ERROR);
        return socketHandler.handleFailure(MESSAGE, throwable);
    }

    private void attributeOutgoingMessage(MessageSendRequest request) {
        if(request.peer()){
            return;
        }

        saveMessage(request.info(), false);
        attributeMessageReceipt(request.info());
    }

    private CompletableFuture<Node> encodeGroup(MessageSendRequest request) {
        var encodedMessage = BytesHelper.messageToBytes(request.info().message());
        var sender = socketHandler.store()
                .jid()
                .orElse(null);
        if(sender == null){
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var senderName = new SenderKeyName(request.info().chatJid().toString(), sender.toSignalAddress());
        var groupBuilder = new GroupBuilder(socketHandler.keys());
        var signalMessage = groupBuilder.createOutgoing(senderName);
        var groupCipher = new GroupCipher(senderName, socketHandler.keys());
        var groupMessage = groupCipher.encrypt(encodedMessage);
        var messageNode = createMessageNode(request, groupMessage);
        if (request.hasRecipientOverride()) {
            return getDevices(request.recipients(), false)
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, request.force()))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::send);
        }

        if (request.force()) {
            return socketHandler.queryGroupMetadata(request.info().chatJid())
                    .thenComposeAsync(this::getGroupDevices)
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, true))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::send);
        }

        return socketHandler.queryGroupMetadata(request.info().chatJid())
                .thenComposeAsync(this::getGroupDevices)
                .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, false))
                .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                .thenComposeAsync(socketHandler::send);
    }

    private CompletableFuture<Node> encodeConversation(MessageSendRequest request) {
        var sender = socketHandler.store()
                .jid()
                .orElse(null);
        if(sender == null){
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var encodedMessage = BytesHelper.messageToBytes(request.info().message());
        if(request.peer()){
            var chatJid = request.info().chatJid();
            var peerNode = createMessageNode(request, chatJid, encodedMessage, true);
            var encodedMessageNode = createEncodedMessageNode(request, List.of(peerNode), null);
            return socketHandler.send(encodedMessageNode);
        }

        var knownDevices = getRecipients(request, sender);
        var deviceMessage = new DeviceSentMessage(request.info().chatJid(), request.info().message(), Optional.empty());
        var encodedDeviceMessage = BytesHelper.messageToBytes(deviceMessage);
        return getDevices(knownDevices, true)
                .thenComposeAsync(allDevices -> createConversationNodes(request, allDevices, encodedMessage, encodedDeviceMessage))
                .thenApplyAsync(sessions -> createEncodedMessageNode(request, sessions, null))
                .thenComposeAsync(socketHandler::send);
    }

    private List<Jid> getRecipients(MessageSendRequest request, Jid sender) {
        if(request.peer()){
            return List.of(request.info().chatJid());
        }

        if (request.hasRecipientOverride()) {
            return request.recipients();
        }

        return List.of(sender.withoutDevice(), request.info().chatJid());
    }

    private boolean isConversation(MessageInfo info) {
        return info.chatJid().hasServer(JidServer.WHATSAPP)
                || info.chatJid().hasServer(JidServer.USER);
    }

    private Node createEncodedMessageNode(MessageSendRequest request, List<Node> preKeys, Node descriptor) {
        var body = new ArrayList<Node>();
        if (!preKeys.isEmpty()) {
            if (request.peer()) {
                body.addAll(preKeys);
            } else {
                body.add(Node.of("participants", preKeys));
            }
        }

        if (descriptor != null) {
            body.add(descriptor);
        }

        if (!request.peer() && hasPreKeyMessage(preKeys)) {
            socketHandler.keys().companionIdentity()
                    .ifPresent(companionIdentity -> body.add(Node.of("device-identity", SignedDeviceIdentitySpec.encode(companionIdentity))));
        }

        var attributes = Attributes.ofNullable(request.additionalAttributes())
                .put("id", request.info().id())
                .put("to", request.info().chatJid())
                .put("t", request.info().timestampSeconds(), !request.peer())
                .put("type", "text")
                .put("category", "peer", request::peer)
                .put("duration", "900", request.info().message().type() == MessageType.LIVE_LOCATION)
                .put("device_fanout", false, request.info().message().type() == MessageType.BUTTONS)
                .put("push_priority", "high", isAppStateKeyShare(request))
                .toMap();
        return Node.of("message", attributes, body);
    }

    private boolean isAppStateKeyShare(MessageSendRequest request) {
        return request.peer()
                && request.info().message().content() instanceof ProtocolMessage protocolMessage
                && protocolMessage.protocolType() == ProtocolMessage.Type.APP_STATE_SYNC_KEY_SHARE;
    }

    private boolean hasPreKeyMessage(List<Node> participants) {
        return participants.stream()
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(node -> node.attributes().getOptionalString("type"))
                .flatMap(Optional::stream)
                .anyMatch(PKMSG::equals);
    }

    private CompletableFuture<List<Node>> createConversationNodes(MessageSendRequest request, List<Jid> contacts, byte[] message, byte[] deviceMessage) {
        var jid = socketHandler.store()
                .jid()
                .orElse(null);
        if(jid == null){
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var partitioned = contacts.stream()
                .collect(Collectors.partitioningBy(contact -> Objects.equals(contact.user(), jid.user())));
        var companions = querySessions(partitioned.get(true), request.force())
                .thenApplyAsync(ignored -> createMessageNodes(request, partitioned.get(true), deviceMessage));
        var others = querySessions(partitioned.get(false), request.force())
                .thenApplyAsync(ignored -> createMessageNodes(request, partitioned.get(false), message));
        return companions.thenCombineAsync(others, (first, second) -> toSingleList(first, second));
    }

    private CompletableFuture<List<Node>> createGroupNodes(MessageSendRequest request, byte[] distributionMessage, List<Jid> participants, boolean force) {
        var missingParticipants = participants.stream()
                .filter(participant -> force || !socketHandler.keys().hasGroupKeys(request.info().chatJid(), participant))
                .toList();
        if (missingParticipants.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        var whatsappMessage = new SenderKeyDistributionMessage(request.info().chatJid().toString(), distributionMessage);
        var paddedMessage = BytesHelper.messageToBytes(whatsappMessage);
        return querySessions(missingParticipants, force)
                .thenApplyAsync(ignored -> createMessageNodes(request, missingParticipants, paddedMessage))
                .thenApplyAsync(results -> {
                    socketHandler.keys().addRecipientsWithPreKeys(request.info().chatJid(), missingParticipants);
                    return results;
                });
    }

    protected CompletableFuture<Void> querySessions(List<Jid> contacts, boolean force) {
        var missingSessions = contacts.stream()
                .filter(contact -> force || !socketHandler.keys().hasSession(contact.toSignalAddress()))
                .map(contact -> Node.of("user", Map.of("jid", contact)))
                .toList();
        return missingSessions.isEmpty() ? CompletableFuture.completedFuture(null) : querySession(missingSessions);
    }

    private CompletableFuture<Void> querySession(List<Node> children){
        return socketHandler.sendQuery("get", "encrypt", Node.of("key", children))
                .thenAcceptAsync(this::parseSessions);
    }

    private List<Node> createMessageNodes(MessageSendRequest request, List<Jid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(request, contact, message, false))
                .toList();
    }

    private Node createMessageNode(MessageSendRequest request, Jid contact, byte[] message, boolean peer) {
        var cipher = new SessionCipher(contact.toSignalAddress(), socketHandler.keys());
        var encrypted = cipher.encrypt(message);
        var messageNode = createMessageNode(request, encrypted);
        return peer ? messageNode : Node.of("to", Map.of("jid", contact), messageNode);
    }

    private CompletableFuture<List<Jid>> getGroupDevices(GroupMetadata metadata) {
        var jids = metadata.participants()
                .stream()
                .map(GroupParticipant::jid)
                .toList();
        return getDevices(jids, false);
    }

    protected CompletableFuture<List<Jid>> getDevices(List<Jid> contacts, boolean excludeSelf) {
        return queryDevices(contacts, excludeSelf)
                .thenApplyAsync(missingDevices -> excludeSelf ? toSingleList(contacts, missingDevices) : missingDevices);
    }

    private CompletableFuture<List<Jid>> queryDevices(List<Jid> contacts, boolean excludeSelf) {
        var contactNodes = contacts.stream()
                .map(contact -> Node.of("user", Map.of("jid", contact)))
                .toList();
        var body = Node.of("usync",
                Map.of("sid", MessageKey.randomId(), "mode", "query", "last", "true", "index", "0", "context", "message"),
                Node.of("query", Node.of("devices", Map.of("version", "2"))),
                Node.of("list", contactNodes));
        return socketHandler.sendQuery("get", "usync", body)
                .thenApplyAsync(result -> parseDevices(result, excludeSelf));
    }

    private List<Jid> parseDevices(Node node, boolean excludeSelf) {
        return node.children()
                .stream()
                .map(child -> child.findNode("list"))
                .flatMap(Optional::stream)
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(entry -> parseDevice(entry, excludeSelf))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Jid> parseDevice(Node wrapper, boolean excludeSelf) {
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
                .map(id -> Jid.ofDevice(jid.user(), id))
                .toList();
    }

    private Optional<Integer> parseDeviceId(Node child, Jid jid, boolean excludeSelf) {
        var self = socketHandler.store()
                .jid()
                .orElse(null);
        if(self == null){
            return Optional.empty();
        }

        var deviceId = child.attributes().getInt("id");
        return child.description().equals("device")
                && (!excludeSelf || deviceId != 0)
                && (!jid.user().equals(self.user()) || self.device() != deviceId)
                && (deviceId == 0 || child.attributes().hasKey("key-index")) ? Optional.of(deviceId) : Optional.empty();
    }

    protected void parseSessions(Node node) {
        node.findNode("list")
                .orElseThrow(() -> new IllegalArgumentException("Cannot parse sessions: " + node))
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

    public CompletableFuture<Void> decode(Node node) {
        try {
            var businessName = getBusinessName(node);
            var encrypted = node.findNodes("enc");
            if (node.hasNode("unavailable") && !node.hasNode("enc")) {
                return decodeMessage(node, null, businessName);
            }

            var futures = encrypted.stream()
                    .map(message -> decodeMessage(node, message, businessName))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
            return CompletableFuture.failedFuture(throwable);
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
                .map(BusinessVerifiedNameCertificateSpec::decode)
                .map(certificate -> certificate.details().name());
    }

    private Node createMessageNode(MessageSendRequest request, CipheredMessageResult groupMessage) {
        var mediaType = getMediaType(request.info().message());
        var attributes = Attributes.of()
                .put("v", "2")
                .put("type", groupMessage.type())
                .put("mediatype", mediaType, Objects::nonNull);
        return Node.of("enc", attributes, groupMessage.message());
    }

    private String getMediaType(MessageContainer container) {
        var content = container.content();
        return switch (content) {
            case ImageMessage imageMessage -> "image";
            case VideoOrGifMessage videoMessage -> videoMessage.gifPlayback() ? "gif" : "video";
            case AudioMessage audioMessage -> audioMessage.voiceMessage() ? "ptt" : "audio";
            case ContactMessage contactMessage -> "vcard";
            case DocumentMessage documentMessage -> "document";
            case ContactsMessage contactsMessage -> "contact_array";
            case LiveLocationMessage liveLocationMessage -> "livelocation";
            case StickerMessage stickerMessage -> "sticker";
            case ListMessage listMessage -> "list";
            case ListResponseMessage listResponseMessage -> "list_response";
            case ButtonsResponseMessage buttonsResponseMessage -> "buttons_response";
            case PaymentOrderMessage paymentOrderMessage -> "order";
            case ProductMessage productMessage -> "product";
            case NativeFlowResponseMessage nativeFlowResponseMessage -> "native_flow_response";
            case ButtonsMessage buttonsMessage ->
                    buttonsMessage.headerType().hasMedia() ? buttonsMessage.headerType().name().toLowerCase() : null;
            case null, default -> null;
        };
    }

    private CompletableFuture<Void> decodeMessage(Node infoNode, Node messageNode, String businessName) {
        try {
            lock.lock();
            var offline = infoNode.attributes().hasKey("offline");
            var pushName = infoNode.attributes().getNullableString("notify");
            var timestamp = infoNode.attributes().getLong("t");
            var id = infoNode.attributes().getRequiredString("id");
            var from = infoNode.attributes()
                    .getJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var recipient = infoNode.attributes().getJid("recipient").orElse(from);
            var participant = infoNode.attributes().getJid("participant").orElse(null);
            var messageBuilder = new MessageInfoBuilder()
                    .status(MessageStatus.PENDING);
            var keyBuilder = new MessageKeyBuilder()
                    .id(MessageKey.randomId());
            var receiver = socketHandler.store()
                    .jid()
                    .map(Jid::withoutDevice)
                    .orElse(null);
            if(receiver == null){
                return CompletableFuture.completedFuture(null); // This means that the session got disconnected while processing
            }

            if (from.hasServer(JidServer.WHATSAPP) || from.hasServer(JidServer.USER)) {
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from.withoutDevice(), receiver));
                messageBuilder.senderJid(from);
            } else {
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.withoutDevice(), receiver));
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }
            var key = keyBuilder.id(id).build();
            if(Objects.equals(key.senderJid().orElse(null), socketHandler.store().jid().orElse(null))) {
                return sendReceipt(infoNode, id, key.chatJid(), key.senderJid().orElse(null), key.fromMe());
            }

            if (messageNode == null) {
                logger.log(Level.WARNING, "Cannot decode message(id: %s, from: %s)".formatted(id, from));
                return sendReceipt(infoNode, id, key.chatJid(), key.senderJid().orElse(null), key.fromMe());
            }

            var type = messageNode.attributes().getRequiredString("type");
            var encodedMessage = messageNode.contentAsBytes().orElse(null);
            var decodedMessage = decodeMessageBytes(type, encodedMessage, from, participant);
            if (decodedMessage.hasError()) {
                logger.log(Level.WARNING, "Cannot decode message(id: %s, from: %s): %s".formatted(id, from, decodedMessage.error().getMessage()));
                return sendReceipt(infoNode, id, key.chatJid(), key.senderJid().orElse(null), key.fromMe());
            }

            var messageContainer = BytesHelper.bytesToMessage(decodedMessage.message()).unbox();
            var info = messageBuilder.key(key)
                    .broadcast(key.chatJid().hasServer(JidServer.BROADCAST))
                    .pushName(pushName)
                    .status(MessageStatus.DELIVERED)
                    .businessVerifiedName(businessName)
                    .timestampSeconds(timestamp)
                    .message(messageContainer)
                    .build();
            attributeMessageReceipt(info);
            socketHandler.store().attribute(info);
            saveMessage(info, offline);
            socketHandler.onReply(info);
            return sendReceipt(infoNode, id, key.chatJid(), key.senderJid().orElse(null), key.fromMe());
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
            return CompletableFuture.failedFuture(throwable);
        }finally {
            lock.unlock();
        }
    }

    private CompletableFuture<Void> sendReceipt(Node infoNode, String id, Jid chatJid, Jid senderJid, boolean fromMe) {
        var participant = fromMe && senderJid == null ? chatJid : senderJid;
        var category = infoNode.attributes().getString("category");
        var receiptType = getReceiptType(category, fromMe);
        return socketHandler.sendMessageAck(infoNode)
                .thenComposeAsync(ignored -> socketHandler.sendReceipt(chatJid, participant, List.of(id), receiptType));
    }

    private String getReceiptType(String category, boolean fromMe) {
        if(Objects.equals(category, "peer")){
            return "peer_msg";
        }

        if(fromMe){
            return "sender";
        }

        if(!socketHandler.store().online()){
            return "inactive";
        }

        return null;
    }

    private MessageDecodeResult decodeMessageBytes(String type, byte[] encodedMessage, Jid from, Jid participant) {
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
                    var user = from.hasServer(JidServer.WHATSAPP) ? from : participant;
                    Objects.requireNonNull(user, "Cannot decipher pkmsg without user");
                    var session = new SessionCipher(user.toSignalAddress(), socketHandler.keys());
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(preKey);
                }
                case MSG -> {
                    var user = from.hasServer(JidServer.WHATSAPP) ? from : participant;
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
        var self = socketHandler.store()
                .jid()
                .map(Jid::withoutDevice)
                .orElse(null);
        if (!info.fromMe() || (self != null && !info.chatJid().equals(self))) {
            return;
        }
        info.receipt().readTimestampSeconds(info.timestampSeconds());
        info.receipt().deliveredJids().add(self);
        info.receipt().readJids().add(self);
        info.setStatus(MessageStatus.READ);
    }

    private void saveMessage(MessageInfo info, boolean offline) {
        if(info.message().content() instanceof SenderKeyDistributionMessage distributionMessage) {
            handleDistributionMessage(distributionMessage, info.senderJid());
        }
        if (info.chatJid().type() == JidType.STATUS) {
            socketHandler.store().addStatus(info);
            socketHandler.onNewStatus(info);
            return;
        }
        if (info.message().hasCategory(MessageCategory.SERVER)) {
            if (info.message().content() instanceof ProtocolMessage protocolMessage) {
                handleProtocolMessage(info, protocolMessage);
            }
            return;
        }

        var chat = info.chat().orElse(null);
        if(chat == null) {
            return;
        }

        var result = chat.addNewMessage(info);
        if (!result || info.timestampSeconds() <= socketHandler.store().initializationTimeStamp()) {
            return;
        }
        if (chat.archived() && socketHandler.store().unarchiveChats()) {
            chat.setArchived(false);
        }
        info.sender()
                .filter(this::isTyping)
                .ifPresent(sender -> socketHandler.onUpdateChatPresence(ContactStatus.AVAILABLE, sender.jid(), chat));
        if (!info.ignore() && !info.fromMe()) {
            chat.setUnreadMessagesCount(chat.unreadMessagesCount() + 1);
        }
        socketHandler.onNewMessage(info, offline);
    }

    private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, Jid from) {
        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var builder = new GroupBuilder(socketHandler.keys());
        var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
        builder.createIncoming(groupName, message);
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
        var chat = info.chat().orElse(null);
        if(chat != null) {
            chat.setEphemeralMessagesToggleTimeSeconds(info.timestampSeconds());
            chat.setEphemeralMessageDuration(ChatEphemeralTimer.of((int) protocolMessage.ephemeralExpiration()));
        }
        var setting = new EphemeralSettings((int) protocolMessage.ephemeralExpiration(), info.timestampSeconds());
        socketHandler.onSetting(setting);
    }

    private void onMessageRevoked(MessageInfo info, ProtocolMessage protocolMessage) {
        socketHandler.store()
                .findMessageById(info.chatJid(), protocolMessage.key().orElseThrow().id())
                .ifPresent(message -> onMessageDeleted(info, message));
    }

    private void onAppStateSyncKeyShare(ProtocolMessage protocolMessage) {
        var data = protocolMessage.appStateSyncKeyShare()
                .orElseThrow(() -> new NoSuchElementException("Missing app state keys"));
        var self = socketHandler.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        socketHandler.keys()
                .addAppKeys(self, data.keys());
        socketHandler.pullInitialPatches()
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(UNKNOWN, throwable));
    }

    private void onHistorySyncNotification(MessageInfo info, ProtocolMessage protocolMessage) {
        if(isZeroHistorySyncComplete()){
            return;
        }

        downloadHistorySync(protocolMessage)
                .thenAcceptAsync(history -> onHistoryNotification(info, history))
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(HISTORY_SYNC, throwable))
                .thenRunAsync(() -> socketHandler.sendReceipt(info.chatJid(), null, List.of(info.id()), "hist_sync"));
    }

    private boolean isZeroHistorySyncComplete() {
        return socketHandler.store().historyLength().isZero()
                && historySyncTypes.contains(Type.INITIAL_STATUS_V3)
                && historySyncTypes.contains(Type.PUSH_NAME)
                && historySyncTypes.contains(Type.INITIAL_BOOTSTRAP)
                && historySyncTypes.contains(Type.NON_BLOCKING_DATA);
    }

    private boolean isTyping(Contact sender) {
        return sender.lastKnownPresence() == ContactStatus.COMPOSING
                || sender.lastKnownPresence() == ContactStatus.RECORDING;
    }

    private CompletableFuture<HistorySync> downloadHistorySync(ProtocolMessage protocolMessage) {
        return protocolMessage.historySyncNotification()
                .map(this::downloadHistorySyncNotification)
                .orElseGet(() -> CompletableFuture.completedFuture(null));

    }

    private CompletableFuture<HistorySync> downloadHistorySyncNotification(HistorySyncNotification notification) {
        return notification.initialHistBootstrapInlinePayload()
                .map(result -> CompletableFuture.completedFuture(HistorySyncSpec.decode(BytesHelper.decompress(result))))
                .orElseGet(() -> Medias.download(notification)
                        .thenApplyAsync(entry -> entry.orElseThrow(() -> new NoSuchElementException("Cannot download history sync")))
                        .thenApplyAsync(result -> HistorySyncSpec.decode(BytesHelper.decompress(result))));
    }

    private void onHistoryNotification(MessageInfo info, HistorySync history) {
        handleHistorySync(history);
        if (history.progress() == null) {
            return;
        }

        socketHandler.onHistorySyncProgress(history.progress(), history.syncType() == Type.RECENT);
    }

    private void onMessageDeleted(MessageInfo info, MessageInfo message) {
        info.chat().ifPresent(chat -> chat.removeMessage(message));
        message.setRevokeTimestampSeconds(Clock.nowSeconds());
        socketHandler.onMessageDeleted(message, true);
    }

    private void handleHistorySync(HistorySync history) {
        try {
            switch (history.syncType()) {
                case INITIAL_STATUS_V3 -> handleInitialStatus(history);
                case PUSH_NAME -> handlePushNames(history);
                case INITIAL_BOOTSTRAP -> handleInitialBootstrap(history);
                case RECENT, FULL -> handleChatsSync(history);
                case NON_BLOCKING_DATA -> handleNonBlockingData(history);
            }
        }finally {
            historySyncTypes.add(history.syncType());
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
        var jid = Jid.of(pushName.id());
        var contact = socketHandler.store()
                .findContactByJid(jid)
                .orElseGet(() -> createNewContact(jid));
        pushName.name().ifPresent(contact::setChosenName);
        var action = new ContactAction(pushName.name(), Optional.empty(), Optional.empty());
        socketHandler.onAction(action, MessageIndexInfo.of("contact", jid, null, true));
    }

    private Contact createNewContact(Jid jid) {
        var contact = socketHandler.store().addContact(jid);
        socketHandler.onNewContact(contact);
        return contact;
    }

    private void handleInitialBootstrap(HistorySync history) {
        if(!socketHandler.store().historyLength().isZero()){
            var jids = history.conversations()
                    .stream()
                    .map(Chat::jid)
                    .toList();
            historyCache.addAll(jids);
        }

        handleConversations(history);
        socketHandler.onChats();
    }

    private void handleChatsSync(HistorySync history) {
        if(socketHandler.store().historyLength().isZero()){
            return;
        }

        handleConversations(history);
        handleConversationsNotifications(history);
        scheduleHistorySyncTimeout();
    }

    private void handleConversationsNotifications(HistorySync history) {
        var toRemove = new HashSet<Jid>();
        for (var cachedJid : historyCache) {
            var chat = socketHandler.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if(chat == null) {
                continue;
            }

            var done = !history.conversations().contains(chat);
            if(done){
                chat.setEndOfHistoryTransfer(true);
                chat.setEndOfHistoryTransferType(Chat.EndOfHistoryTransferType.COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY);
                toRemove.add(cachedJid);
            }

            socketHandler.onChatRecentMessages(chat, done);
        }

        historyCache.removeAll(toRemove);
    }

    private void scheduleHistorySyncTimeout() {
        var executor = CompletableFuture.delayedExecutor(HISTORY_SYNC_TIMEOUT, TimeUnit.SECONDS);
        if(historySyncTask != null){
            historySyncTask.cancel(true);
        }

        this.historySyncTask = CompletableFuture.runAsync(this::onForcedHistorySyncCompletion, executor);
    }

    private void onForcedHistorySyncCompletion() {
        for (var cachedJid : historyCache) {
            var chat = socketHandler.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if(chat == null) {
                continue;
            }

            socketHandler.onChatRecentMessages(chat, true);
        }

        historyCache.clear();
    }


    private void handleConversations(HistorySync history) {
        var store = socketHandler.store();
        for (var chat : history.conversations()) {
            var pastParticipants = pastParticipantsQueue.remove(chat.jid());
            if (pastParticipants != null) {
                chat.addPastParticipants(pastParticipants);
            }

            store.addChat(chat);
        }
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            handlePastParticipants(pastParticipants);
        }
    }

    private void handlePastParticipants(GroupPastParticipants pastParticipants) {
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
        historyCache.clear();
        if(executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        historySyncTask = null;
        historySyncTypes.clear();
    }

    private record MessageDecodeResult(byte[] message, Throwable error) {
        public boolean hasError() {
            return error != null;
        }
    }
}
