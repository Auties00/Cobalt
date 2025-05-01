package it.auties.whatsapp.socket;

import it.auties.linkpreview.LinkPreview;
import it.auties.linkpreview.LinkPreviewMatch;
import it.auties.linkpreview.LinkPreviewMedia;
import it.auties.whatsapp.api.TextPreviewSetting;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.model.action.ContactAction;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.ChatMetadata;
import it.auties.whatsapp.model.chat.ChatParticipant;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.jid.JidType;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MediaFile;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.server.DeviceSentMessage;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessage;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.poll.*;
import it.auties.whatsapp.model.request.MessageSendRequest;
import it.auties.whatsapp.model.setting.EphemeralSettings;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentitySpec;
import it.auties.whatsapp.model.signal.keypair.ISignalKeyPair;
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
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.HISTORY_SYNC;
import static it.auties.whatsapp.api.ErrorHandler.Location.MESSAGE;
import static it.auties.whatsapp.util.SignalConstants.*;

class MessageHandler {
    private static final int HISTORY_SYNC_MAX_TIMEOUT = 25;
    private static final int MAX_MESSAGE_RETRIES = 5;

    private final SocketHandler socketHandler;
    private final Map<Jid, CopyOnWriteArrayList<Jid>> devicesCache;
    private final Set<Jid> historyCache;
    private final ReentrantLock lock;
    private final HistorySyncProgressTracker recentHistorySyncTracker;
    private final HistorySyncProgressTracker fullHistorySyncTracker;
    private ScheduledFuture<?> historySyncTask;

    protected MessageHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.devicesCache = new ConcurrentHashMap<>();
        this.historyCache = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantLock(true);
        this.recentHistorySyncTracker = new HistorySyncProgressTracker();
        this.fullHistorySyncTracker = new HistorySyncProgressTracker();
    }

    protected CompletableFuture<Void> encode(MessageSendRequest request) {
        return switch (request) {
            case MessageSendRequest.Chat chatRequest -> encodeChatMessage(chatRequest);
            case MessageSendRequest.Newsletter newsletterRequest -> encodeNewsletterMessage(newsletterRequest);
        };
    }

    private CompletableFuture<Void> encodeChatMessage(MessageSendRequest.Chat request) {
        return prepareOutgoingChatMessage(request.info())
                .thenComposeAsync(ignored -> {
                    try {
                        lock.lock();
                        if (request.peer() || isConversation(request.info())) {
                            return encodeConversation(request);
                        }else {
                            return encodeGroup(request);
                        }
                    } finally {
                        lock.unlock();
                    }
                })
                .thenRunAsync(() -> {
                    if (request.peer()) {
                        return;
                    }

                    saveMessage(request.info(), false);
                    attributeMessageReceipt(request.info());
                })
                .exceptionallyAsync(throwable -> {
                    request.info().setStatus(MessageStatus.ERROR);
                    saveMessage(request.info(), false);
                    return socketHandler.handleFailure(MESSAGE, throwable);
                });
    }

    private CompletableFuture<Void> prepareOutgoingChatMessage(MessageInfo<?> messageInfo) {
        var result = switch (messageInfo.message().content()) {
            case MediaMessage<?> mediaMessage -> attributeMediaMessage(messageInfo.parentJid(), mediaMessage);
            case ButtonMessage buttonMessage -> attributeButtonMessage(messageInfo.parentJid(), buttonMessage);
            case TextMessage textMessage -> attributeTextMessage(textMessage);
            case PollCreationMessage pollCreationMessage when messageInfo instanceof ChatMessageInfo pollCreationInfo -> // I guess they will be supported some day in newsletters
                    attributePollCreationMessage(pollCreationInfo, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage when messageInfo instanceof ChatMessageInfo pollUpdateInfo // I guess they will be supported some day in newsletters
                    -> attributePollUpdateMessage(pollUpdateInfo, pollUpdateMessage);
            default -> CompletableFuture.<Void>completedFuture(null);
        };
        if(messageInfo instanceof ChatMessageInfo chatMessageInfo) {
            attributeChatMessage(chatMessageInfo);
            fixEphemeralMessage(chatMessageInfo);
        }
        return result;
    }

    private void fixEphemeralMessage(ChatMessageInfo info) {
        if (info.message().hasCategory(MessageCategory.SERVER)) {
            return;
        }

        var chat = info.chat().orElse(null);
        if (chat != null && chat.isEphemeral()) {
            info.message()
                    .contentWithContext()
                    .flatMap(ContextualMessage::contextInfo)
                    .ifPresent(contextInfo -> createEphemeralContext(chat, contextInfo));
            info.setMessage(info.message().toEphemeral());
            return;
        }

        if (info.message().type() != MessageType.EPHEMERAL) {
            return;
        }

        info.setMessage(info.message().unbox());
    }

    private void createEphemeralContext(Chat chat, ContextInfo contextInfo) {
        var period = chat.ephemeralMessageDuration()
                .period()
                .toSeconds();
        contextInfo.setEphemeralExpiration((int) period);
    }

    private CompletableFuture<Void> attributeTextMessage(TextMessage textMessage) {
        if (socketHandler.store().textPreviewSetting() == TextPreviewSetting.DISABLED) {
            return CompletableFuture.completedFuture(null);
        }

        return LinkPreview.createPreviewAsync(textMessage.text())
                .thenComposeAsync(result -> attributeTextMessage(textMessage, result.orElse(null)))
                .exceptionallyAsync(error -> null);
    }

    private CompletableFuture<Void> attributeTextMessage(TextMessage textMessage, LinkPreviewMatch match) {
        if (match == null) {
            return CompletableFuture.completedFuture(null);
        }

        var uri = match.result().uri().toString();
        if (socketHandler.store().textPreviewSetting() == TextPreviewSetting.ENABLED_WITH_INFERENCE
                && !Objects.equals(match.text(), uri)) {
            textMessage.setText(textMessage.text().replace(match.text(), uri));
        }

        var imageThumbnail = match.result()
                .images()
                .stream()
                .reduce(this::compareDimensions);
        var videoUri = match.result()
                .videos()
                .stream()
                .reduce(this::compareDimensions);
        textMessage.setMatchedText(uri);
        textMessage.setCanonicalUrl(videoUri.map(LinkPreviewMedia::uri).orElse(match.result().uri()).toString());
        textMessage.setThumbnailWidth(imageThumbnail.map(LinkPreviewMedia::width).orElse(null));
        textMessage.setThumbnailHeight(imageThumbnail.map(LinkPreviewMedia::height).orElse(null));
        textMessage.setDescription(match.result().siteDescription());
        textMessage.setTitle(match.result().title());
        textMessage.setPreviewType(videoUri.isPresent() ? TextMessage.PreviewType.VIDEO : TextMessage.PreviewType.NONE);
        var proxy = switch (socketHandler.store().mediaProxySetting()) {
            case NONE, UPLOADS -> null;
            case DOWNLOADS, ALL -> socketHandler.store().proxy().orElse(null);
        };
        return imageThumbnail.map(data -> Medias.downloadAsync(data.uri(), proxy)
                        .thenAccept(textMessage::setThumbnail))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    private LinkPreviewMedia compareDimensions(LinkPreviewMedia first, LinkPreviewMedia second) {
        return first.width() * first.height() > second.width() * second.height() ? first : second;
    }

    private CompletableFuture<Void> attributeMediaMessage(Jid chatJid, MediaMessage<?> mediaMessage) {
        var media = mediaMessage.decodedMedia()
                .orElseThrow(() -> new IllegalArgumentException("Missing media to upload"));
        var attachmentType = getAttachmentType(chatJid, mediaMessage);
        var mediaConnection = socketHandler.store().mediaConnection();
        var userAgent = socketHandler.store()
                .device()
                .toUserAgent(socketHandler.store().version())
                .orElse(null);
        var proxy = socketHandler.store()
                .proxy()
                .filter(ignored -> socketHandler.store().mediaProxySetting().allowsUploads())
                .orElse(null);
        return Medias.upload(media, attachmentType, mediaConnection, proxy, userAgent)
                .thenAccept(upload -> attributeMediaMessage(mediaMessage, upload));
    }

    private AttachmentType getAttachmentType(Jid chatJid, MediaMessage<?> mediaMessage) {
        if (!chatJid.hasServer(JidServer.newsletter())) {
            return mediaMessage.attachmentType();
        }

        return switch (mediaMessage.mediaType()) {
            case IMAGE -> AttachmentType.NEWSLETTER_IMAGE;
            case DOCUMENT -> AttachmentType.NEWSLETTER_DOCUMENT;
            case AUDIO -> AttachmentType.NEWSLETTER_AUDIO;
            case VIDEO -> AttachmentType.NEWSLETTER_VIDEO;
            case STICKER -> AttachmentType.NEWSLETTER_STICKER;
            case NONE -> throw new IllegalArgumentException("Unexpected empty message");
        };
    }


    private void attributeMediaMessage(MutableAttachmentProvider<?> attachmentProvider, MediaFile upload) {
        if (attachmentProvider instanceof MediaMessage<?> mediaMessage) {
            mediaMessage.setHandle(upload.handle());
        }

        attachmentProvider
                .setMediaSha256(upload.fileSha256())
                .setMediaEncryptedSha256(upload.fileEncSha256())
                .setMediaKey(upload.mediaKey())
                .setMediaUrl(upload.url())
                .setMediaKeyTimestamp(upload.timestamp())
                .setMediaDirectPath(upload.directPath())
                .setMediaSize(upload.fileLength());
    }

    private CompletableFuture<Void> attributePollCreationMessage(ChatMessageInfo info, PollCreationMessage pollCreationMessage) {
        var pollEncryptionKey = pollCreationMessage.encryptionKey()
                .orElseGet(() -> Bytes.random(32));
        pollCreationMessage.setEncryptionKey(pollEncryptionKey);
        info.setMessageSecret(pollEncryptionKey);
        var metadata = new PollAdditionalMetadata(false);
        info.setPollAdditionalMetadata(metadata);
        info.message().deviceInfo().ifPresentOrElse(deviceInfo -> deviceInfo.setMessageSecret(pollEncryptionKey), () -> {
            var deviceInfo = new DeviceContextInfoBuilder()
                    .messageSecret(pollEncryptionKey)
                    .build();
            var message = info.message().withDeviceInfo(deviceInfo);
            info.setMessage(message);
        });
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> attributePollUpdateMessage(ChatMessageInfo info, PollUpdateMessage pollUpdateMessage) {
        if (pollUpdateMessage.encryptedMetadata().isPresent()) {
            return CompletableFuture.completedFuture(null);
        }

        var me = socketHandler.store().jid();
        if (me.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var additionalData = "%s\0%s".formatted(pollUpdateMessage.pollCreationMessageKey().id(), me.get().toSimpleJid());
        var encryptedOptions = pollUpdateMessage.votes().stream().map(entry -> Sha256.calculate(entry.name())).toList();
        var pollUpdateEncryptedOptions = PollUpdateEncryptedOptionsSpec.encode(new PollUpdateEncryptedOptions(encryptedOptions));
        var originalPollInfo = socketHandler.store()
                .findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
        var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
        var originalPollSender = originalPollInfo.senderJid().toSimpleJid().toString().getBytes(StandardCharsets.UTF_8);
        var modificationSenderJid = info.senderJid().toSimpleJid();
        pollUpdateMessage.setVoter(modificationSenderJid);
        var modificationSender = modificationSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var secretName = pollUpdateMessage.secretName().getBytes(StandardCharsets.UTF_8);
        var useSecretPayload = Bytes.concat(
                pollUpdateMessage.pollCreationMessageKey().id().getBytes(StandardCharsets.UTF_8),
                originalPollSender,
                modificationSender,
                secretName
        );
        var encryptionKey = originalPollMessage.encryptionKey()
                .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
        var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload, 32);
        var iv = Bytes.random(12);
        var pollUpdateEncryptedPayload = AesGcm.encrypt(iv, pollUpdateEncryptedOptions, useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollUpdateEncryptedMetadata = new PollUpdateEncryptedMetadata(pollUpdateEncryptedPayload, iv);
        pollUpdateMessage.setEncryptedMetadata(pollUpdateEncryptedMetadata);
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> attributeButtonMessage(Jid chatJid, ButtonMessage buttonMessage) {
        return switch (buttonMessage) {
            case ButtonsMessage buttonsMessage when buttonsMessage.header().isPresent()
                    && buttonsMessage.header().get() instanceof MediaMessage<?> mediaMessage ->
                    attributeMediaMessage(chatJid, mediaMessage);
            case TemplateMessage templateMessage when templateMessage.format().isPresent() -> {
                var templateFormatter = templateMessage.format().get();
                yield switch (templateFormatter) {
                    case HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplate
                            when highlyStructuredFourRowTemplate.title().isPresent() && highlyStructuredFourRowTemplate.title().get() instanceof MediaMessage<?> fourRowMedia ->
                            attributeMediaMessage(chatJid, fourRowMedia);
                    case HydratedFourRowTemplate hydratedFourRowTemplate when hydratedFourRowTemplate.title().isPresent() && hydratedFourRowTemplate.title().get() instanceof MediaMessage<?> hydratedFourRowMedia ->
                            attributeMediaMessage(chatJid, hydratedFourRowMedia);
                    default -> CompletableFuture.completedFuture(null);
                };
            }
            case InteractiveMessage interactiveMessage
                    when interactiveMessage.header().isPresent()
                    && interactiveMessage.header().get().attachment().isPresent()
                    && interactiveMessage.header().get().attachment().get() instanceof MediaMessage<?> interactiveMedia ->
                    attributeMediaMessage(chatJid, interactiveMedia);
            default -> CompletableFuture.completedFuture(null);
        };
    }

    private CompletableFuture<Void> encodeNewsletterMessage(MessageSendRequest.Newsletter request) {
        return prepareOutgoingChatMessage(request.info()).thenComposeAsync(ignored -> {
            var message = request.info().message();
            var messageNode = getPlainMessageNode(message);
            var type = request.info().message().content() instanceof MediaMessage<?> ? "media" : "text";
            var attributes = Attributes.ofNullable(request.additionalAttributes())
                    .put("id", request.info().id())
                    .put("to", request.info().parentJid())
                    .put("type", type)
                    .put("media_id", getPlainMessageHandle(request), Objects::nonNull)
                    .toMap();
            return socketHandler.sendNode(Node.of("message", attributes, messageNode))
                    .thenRunAsync(() -> {
                        var newsletter = request.info().newsletter();
                        newsletter.addMessage(request.info());
                    })
                    .exceptionallyAsync(throwable -> {
                        request.info().setStatus(MessageStatus.ERROR);
                        return socketHandler.handleFailure(MESSAGE, throwable);
                    });
        });
    }

    private String getPlainMessageHandle(MessageSendRequest.Newsletter request) {
        var message = request.info()
                .message()
                .content();
        if (!(message instanceof MediaMessage<?> extendedMediaMessage)) {
            return null;
        }

        return extendedMediaMessage.handle()
                .orElse(null);
    }

    private Node getPlainMessageNode(MessageContainer message) {
        if(message.content() instanceof ReactionMessage reactionMessage) {
            return Node.of("reaction", Map.of("code", reactionMessage.content()));
        }

        if(message.content() instanceof TextMessage textMessage && textMessage.thumbnail().isEmpty()) {
            var byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(10);
            var encoded = textMessage.text().getBytes(StandardCharsets.UTF_8);
            byteArrayOutputStream.writeBytes(Bytes.intToVarInt(encoded.length));
            byteArrayOutputStream.writeBytes(encoded);
            return Node.of("plaintext", byteArrayOutputStream.toByteArray());
        }

        var messageAttributes = Attributes.of()
                .put("mediatype", getMediaType(message), Objects::nonNull)
                .toMap();
        return Node.of("plaintext", messageAttributes, message.isEmpty() ? null : MessageContainerSpec.encode(message));
    }

    private CompletableFuture<Node> encodeGroup(MessageSendRequest.Chat request) {
        var encodedMessage = Bytes.messageToBytes(request.info().message());
        var sender = socketHandler.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var senderName = new SenderKeyName(request.info().chatJid().toString(), sender.toSignalAddress());
        var groupBuilder = new GroupBuilder(socketHandler.keys());
        var signalMessage = groupBuilder.createOutgoing(senderName);
        var groupCipher = new GroupCipher(senderName, socketHandler.keys());
        var groupMessage = groupCipher.encrypt(encodedMessage);
        var messageNode = createMessageNode(request, groupMessage);
        if (request.hasRecipientOverride()) {
            return queryDevices(request.recipients(), false)
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, request.force()))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::sendNode);
        }

        if(request.info().chatJid().type() == JidType.STATUS) {
            var recipients = socketHandler.store()
                    .contacts()
                    .stream()
                    .map(Contact::jid)
                    .toList();
            return queryDevices(recipients, false)
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, true))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::sendNode);
        }

        if (request.force()) {
            return socketHandler.queryGroupMetadata(request.info().chatJid())
                    .thenComposeAsync(this::getGroupDevices)
                    .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, true))
                    .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                    .thenComposeAsync(socketHandler::sendNode);
        }

        return socketHandler.queryGroupMetadata(request.info().chatJid())
                .thenComposeAsync(this::getGroupDevices)
                .thenComposeAsync(allDevices -> createGroupNodes(request, signalMessage, allDevices, false))
                .thenApplyAsync(preKeys -> createEncodedMessageNode(request, preKeys, messageNode))
                .thenComposeAsync(socketHandler::sendNode);
    }

    private CompletableFuture<Node> encodeConversation(MessageSendRequest.Chat request) {
        var sender = socketHandler.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create message: user is not signed in"));
        }

        var encodedMessage = Bytes.messageToBytes(request.info().message());
        if (request.peer()) {
            var chatJid = request.info().chatJid();
            var peerNode = createMessageNode(request, chatJid, encodedMessage, true);
            var encodedMessageNode = createEncodedMessageNode(request, List.of(peerNode), null);
            return socketHandler.sendNode(encodedMessageNode);
        }

        var deviceMessage = new DeviceSentMessage(request.info().chatJid(), request.info().message(), Optional.empty());
        var encodedDeviceMessage = Bytes.messageToBytes(deviceMessage);
        var recipients = getRecipients(request);
        return queryDevices(recipients, !isMe(request.info().chatJid()))
                .thenComposeAsync(allDevices -> createConversationNodes(request, allDevices, encodedMessage, encodedDeviceMessage))
                .thenApplyAsync(sessions -> createEncodedMessageNode(request, sessions, null))
                .thenComposeAsync(socketHandler::sendNode);
    }

    private Set<Jid> getRecipients(MessageSendRequest.Chat request) {
        if (request.hasRecipientOverride()) {
            return request.recipients();
        }

        if(request.peer()) {
            return Set.of(request.info().chatJid());
        }

        return new HashSet<>(List.of(socketHandler.store().jid().orElseThrow().toSimpleJid(), request.info().chatJid()));
    }

    private boolean isConversation(ChatMessageInfo info) {
        return info.chatJid().hasServer(JidServer.whatsapp())
                || info.chatJid().hasServer(JidServer.user());
    }

    private Node createEncodedMessageNode(MessageSendRequest.Chat request, List<Node> preKeys, Node descriptor) {
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
                .put("type", request.info().message().content() instanceof MediaMessage<?> ? "media" : "text")
                .put("verified_name", socketHandler.store().verifiedName().orElse(""), socketHandler.store().verifiedName().isPresent() && !request.peer())
                .put("category", "peer", request.peer())
                .put("duration", "900", request.info().message().type() == MessageType.LIVE_LOCATION)
                .put("device_fanout", false, request.info().message().type() == MessageType.BUTTONS)
                .put("push_priority", "high", isAppStateKeyShare(request))
                .toMap();
        return Node.of("message", attributes, body);
    }

    private boolean isAppStateKeyShare(MessageSendRequest.Chat request) {
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

    private CompletableFuture<List<Node>> createConversationNodes(MessageSendRequest.Chat request, List<Jid> contacts, byte[] message, byte[] deviceMessage) {
        var jid = socketHandler.store()
                .jid()
                .orElse(null);
        if (jid == null) {
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

    private CompletableFuture<List<Node>> createGroupNodes(MessageSendRequest.Chat request, byte[] distributionMessage, List<Jid> participants, boolean force) {
        var missingParticipants = participants.stream()
                .filter(participant -> force || !socketHandler.keys().hasGroupKeys(request.info().chatJid(), participant))
                .toList();
        if (missingParticipants.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        var whatsappMessage = new SenderKeyDistributionMessage(request.info().chatJid().toString(), distributionMessage);
        var paddedMessage = Bytes.messageToBytes(whatsappMessage);
        return querySessions(missingParticipants, force)
                .thenApplyAsync(ignored -> createMessageNodes(request, missingParticipants, paddedMessage))
                .thenApplyAsync(results -> {
                    socketHandler.keys().addRecipientsWithPreKeys(request.info().chatJid(), missingParticipants);
                    return results;
                });
    }

    protected CompletableFuture<Void> querySessions(Collection<Jid> contacts, boolean force) {
        var missingSessions = contacts.stream()
                .filter(contact -> force || !socketHandler.keys().hasSession(contact.toSignalAddress()))
                .map(contact -> Node.of("user", Map.of("jid", contact)))
                .toList();
        return missingSessions.isEmpty() ? CompletableFuture.completedFuture(null) : querySession(missingSessions);
    }

    private CompletableFuture<Void> querySession(List<Node> children) {
        return socketHandler.sendQuery("get", "encrypt", Node.of("key", children))
                .thenAcceptAsync(this::parseSessions);
    }

    private List<Node> createMessageNodes(MessageSendRequest.Chat request, List<Jid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(request, contact, message, false))
                .toList();
    }

    private Node createMessageNode(MessageSendRequest.Chat request, Jid contact, byte[] message, boolean peer) {
        var cipher = new SessionCipher(contact.toSignalAddress(), socketHandler.keys());
        var encrypted = cipher.encrypt(message);
        var messageNode = createMessageNode(request, encrypted);
        return peer ? messageNode : Node.of("to", Map.of("jid", contact), messageNode);
    }

    private CompletableFuture<List<Jid>> getGroupDevices(ChatMetadata metadata) {
        var jids = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .toList();
        return queryDevices(jids, false);
    }

    protected CompletableFuture<List<Jid>> queryDevices(Collection<Jid> contacts, boolean excludeSelf) {
        var cachedDevices = contacts.stream()
                .map(devicesCache::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(entry -> !excludeSelf || !isMe(entry))
                .toList();
        var contactNodes = contacts.stream()
                .filter(entry -> !devicesCache.containsKey(entry) && (!excludeSelf || !isMe(entry)))
                .map(contact -> Node.of("user", Map.of("jid", contact)))
                .toList();
        if(contactNodes.isEmpty()) {
            return CompletableFuture.completedFuture(cachedDevices);
        }

        var body = Node.of("usync",
                Map.of("context", "message", "index", "0", "last", "true", "mode", "query", "sid", SocketHandler.randomSid()),
                Node.of("query", Node.of("devices", Map.of("version", "2"))),
                Node.of("list", contactNodes));
        return socketHandler.sendQuery("get", "usync", body)
                .thenApplyAsync(result -> toSingleList(cachedDevices, parseDevices(result, excludeSelf)));
    }

    private List<Jid> parseDevices(Node node, boolean excludeSelf) {
        return node.children()
                .stream()
                .map(child -> child.findChild("list"))
                .flatMap(Optional::stream)
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(entry -> parseDevice(entry, excludeSelf))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Jid> parseDevice(Node wrapper, boolean excludeSelf) {
        var jid = wrapper.attributes().getRequiredJid("jid");
        var devices = wrapper.findChild("devices")
                .orElseThrow(() -> new NoSuchElementException("Missing devices"))
                .findChild("device-list")
                .orElseThrow(() -> new NoSuchElementException("Missing device list"))
                .children();
        if(devices.isEmpty()) {
            return excludeSelf && isMe(jid) ? List.of() : List.of(jid);
        }

        return devices.stream()
                .map(child -> parseDeviceId(child, jid, excludeSelf))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Jid> parseDeviceId(Node child, Jid jid, boolean excludeSelf) {
        var deviceId = child.attributes().getInt("id");
        if(!child.description().equals("device")) {
            return Optional.empty();
        }

        if(deviceId != 0 && !child.attributes().hasKey("key-index")) {
            return Optional.empty();
        }

        var result = jid.withDevice(deviceId);
        cacheDevice(result);
        if(excludeSelf && isMe(result)) {
            return Optional.empty();
        }

        return Optional.of(jid.withDevice(deviceId));
    }

    private boolean isMe(Jid jid) {
        var self = socketHandler.store().jid().orElse(null);
        if (self == null) {
            return false;
        }

        return jid.user().equals(self.user()) && Objects.equals(self.device(), jid.device());
    }

    private void cacheDevice(Jid jid) {
        var cachedDevices = devicesCache.get(jid.toSimpleJid());
        if(cachedDevices != null) {
            cachedDevices.add(jid);
            return;
        }

        var devices = new CopyOnWriteArrayList<Jid>();
        devices.add(jid);
        devicesCache.put(jid.toSimpleJid(), devices);
    }

    protected void parseSessions(Node node) {
        if(node == null) {
            return;
        }

        node.findChild("list")
                .orElseThrow(() -> new IllegalArgumentException("Cannot parse sessions: " + node))
                .listChildren("user")
                .forEach(this::parseSession);
    }

    private void parseSession(Node node) {
        if (node.hasNode("error")) {
            throw new IllegalArgumentException("Erroneous session node");
        }
        var jid = node.attributes()
                .getRequiredJid("jid");
        var registrationId = node.findChild("registration")
                .map(id -> Bytes.bytesToInt(id.contentAsBytes().orElseThrow(), 4))
                .orElseThrow(() -> new NoSuchElementException("Missing id"));
        var identity = node.findChild("identity")
                .flatMap(Node::contentAsBytes)
                .map(ISignalKeyPair::toSignalKey)
                .orElseThrow(() -> new NoSuchElementException("Missing identity"));
        var signedKey = node.findChild("skey")
                .flatMap(SignalSignedKeyPair::of)
                .orElseThrow(() -> new NoSuchElementException("Missing signed key"));
        var key = node.findChild("key")
                .flatMap(SignalSignedKeyPair::of)
                .orElse(null);
        var builder = new SessionBuilder(jid.toSignalAddress(), socketHandler.keys());
        builder.createOutgoing(registrationId, identity, signedKey, key);
    }

    public void decode(Node node, JidProvider chatOverride, boolean notify) {
        try {
            var businessName = getBusinessName(node);
            if (node.hasNode("unavailable")) {
                decodeChatMessage(node, null, businessName, notify);
                return;
            }

            var encrypted = node.listChildren("enc");
            if (!encrypted.isEmpty()) {
                encrypted.forEach(message -> decodeChatMessage(node, message, businessName, notify));
                return;
            }


            var plainText = node.findChild("plaintext");
            if (plainText.isPresent()) {
                decodeNewsletterMessage(node, plainText.get(), chatOverride, notify);
                return;
            }

            var reaction = node.findChild("reaction");
            if (reaction.isPresent()) {
                decodeNewsletterReaction(node, reaction.get(), chatOverride, notify);
                return;
            }

            decodeChatMessage(node, null, businessName, notify);
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
        return node.findChild("verified_name")
                .flatMap(Node::contentAsBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode)
                .map(certificate -> certificate.details().name());
    }

    private Node createMessageNode(MessageSendRequest.Chat request, CipheredMessageResult groupMessage) {
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
            case ImageMessage ignored -> "image";
            case VideoOrGifMessage videoMessage -> videoMessage.gifPlayback() ? "gif" : "video";
            case AudioMessage audioMessage -> audioMessage.voiceMessage() ? "ptt" : "audio";
            case ContactMessage ignored -> "vcard";
            case DocumentMessage ignored -> "document";
            case ContactsMessage ignored -> "contact_array";
            case LiveLocationMessage ignored -> "livelocation";
            case StickerMessage ignored -> "sticker";
            case ListMessage ignored -> "list";
            case ListResponseMessage ignored -> "list_response";
            case ButtonsResponseMessage ignored -> "buttons_response";
            case PaymentOrderMessage ignored -> "order";
            case ProductMessage ignored -> "product";
            case NativeFlowResponseMessage ignored -> "native_flow_response";
            case ButtonsMessage buttonsMessage ->
                    buttonsMessage.headerType().hasMedia() ? buttonsMessage.headerType().name().toLowerCase() : null;
            case null, default -> null;
        };
    }

    private void decodeNewsletterMessage(Node messageNode, Node plainTextNode, JidProvider chatOverride, boolean notify) {
        try {
            var newsletterJid = messageNode.attributes()
                    .getOptionalJid("from")
                    .or(() -> Optional.ofNullable(chatOverride).map(JidProvider::toJid))
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var messageId = messageNode.attributes()
                    .getRequiredString("id");
            if (notify) {
                socketHandler.sendMessageAck(newsletterJid, messageNode);
                if(socketHandler.store().automaticMessageReceipts()) {
                    var receiptType = getReceiptType("newsletter", false);
                    socketHandler.sendReceipt(newsletterJid, null, List.of(messageId), receiptType);
                }
            }

            var newsletter = socketHandler.store()
                    .findNewsletterByJid(newsletterJid);
            if(newsletter.isEmpty()) {
                return;
            }

            var serverId = messageNode.attributes()
                    .getRequiredInt("server_id");
            var timestamp = messageNode.attributes()
                    .getNullableLong("t");
            var views = messageNode.findChild("views_count")
                    .map(value -> value.attributes().getNullableLong("count"))
                    .orElse(null);
            var reactions = messageNode.findChild("reactions")
                    .stream()
                    .map(node -> node.listChildren("reaction"))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toConcurrentMap(
                            entry -> entry.attributes().getRequiredString("code"),
                            entry -> new NewsletterReaction(
                                    entry.attributes().getRequiredString("code"),
                                    entry.attributes().getLong("count"),
                                    entry.attributes().getBoolean("is_sender")
                            )
                    ));
            var result = plainTextNode.contentAsBytes()
                    .map(MessageContainerSpec::decode)
                    .map(messageContainer -> {
                        var readStatus = notify ? MessageStatus.DELIVERED : MessageStatus.READ;
                        var message = new NewsletterMessageInfo(
                                messageId,
                                serverId,
                                timestamp,
                                views,
                                reactions,
                                messageContainer,
                                readStatus
                        );
                        message.setNewsletter(newsletter.get());
                        return message;
                    });
            if (result.isEmpty()) {
                return;
            }

            newsletter.get().addMessage(result.get());
            if(notify) {
                socketHandler.onNewMessage(result.get());
            }
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
        }
    }

    private void decodeNewsletterReaction(Node messageNode, Node reactionNode, JidProvider chatOverride, boolean notify) {
        try {
            var messageId = messageNode.attributes()
                    .getRequiredString("id");
            var newsletterJid = messageNode.attributes()
                    .getOptionalJid("from")
                    .or(() -> Optional.ofNullable(chatOverride).map(JidProvider::toJid))
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var isSender = messageNode.attributes()
                    .getBoolean("is_sender");
            if(notify) {
                socketHandler.sendMessageAck(newsletterJid, messageNode);
                if(socketHandler.store().automaticMessageReceipts()) {
                    var receiptType = getReceiptType("newsletter", false);
                    socketHandler.sendReceipt(newsletterJid, null, List.of(messageId), receiptType);
                }
            }

            var newsletter = socketHandler.store()
                    .findNewsletterByJid(newsletterJid);
            if(newsletter.isEmpty()) {
                return;
            }

            var message = socketHandler.store()
                    .findMessageById(newsletter.get(), messageId);
            if(message.isEmpty()) {
                return;
            }

            var myReaction = isSender ? message.get()
                    .reactions()
                    .stream()
                    .filter(NewsletterReaction::fromMe)
                    .findFirst()
                    .orElse(null) : null;
            if(myReaction != null) {
                message.get().decrementReaction(myReaction.content());
            }

            var code = reactionNode.attributes()
                    .getOptionalString("code");
            if(code.isEmpty()) {
                return;
            }

            message.get().incrementReaction(code.get(), isSender);
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
        }
    }

    private void decodeChatMessage(Node infoNode, Node messageNode, String businessName, boolean notify) {
        try {
            var pushName = infoNode.attributes().getNullableString("notify");
            var timestamp = infoNode.attributes().getLong("t");
            var id = infoNode.attributes().getRequiredString("id");
            var from = infoNode.attributes()
                    .getRequiredJid("from");
            var recipient = infoNode.attributes()
                    .getOptionalJid("recipient")
                    .orElse(from);
            var participant = infoNode.attributes()
                    .getOptionalJid("participant")
                    .orElse(null);
            var messageBuilder = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING);
            var keyBuilder = new ChatMessageKeyBuilder()
                    .id(SocketHandler.randomSid());
            var receiver = socketHandler.store()
                    .jid()
                    .map(Jid::toSimpleJid)
                    .orElse(null);
            if (receiver == null) {
                return;
            }

            if (from.hasServer(JidServer.whatsapp()) || from.hasServer(JidServer.user())) {
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from.toSimpleJid(), receiver));
                messageBuilder.senderJid(from);
            } else {
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.toSimpleJid(), receiver));
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }
            var key = keyBuilder.id(id).build();

            var senderJid = key.senderJid()
                    .orElse(null);
            if (Objects.equals(senderJid, socketHandler.store().jid().orElse(null))) {
                sendEncMessageSuccessReceipt(infoNode, id, key.chatJid(), senderJid, key.fromMe());
                return;
            }

            var messageContainer = decodeChatMessageContainer(messageNode, from, participant);
            var info = messageBuilder.key(key)
                    .broadcast(key.chatJid().hasServer(JidServer.broadcast()))
                    .pushName(pushName)
                    .status(MessageStatus.DELIVERED)
                    .businessVerifiedName(businessName)
                    .timestampSeconds(timestamp)
                    .message(messageContainer)
                    .build();
            attributeMessageReceipt(info);
            attributeChatMessage(info);
            saveMessage(info, notify);
            socketHandler.onReply(info);
            sendEncMessageSuccessReceipt(infoNode, id, key.chatJid(), senderJid, key.fromMe());
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
        }
    }

    private MessageContainer decodeChatMessageContainer(Node messageNode, Jid from, Jid participant) {
        if (messageNode == null) {
            return MessageContainer.empty();
        }

        var type = messageNode.attributes().getRequiredString("type");
        var encodedMessage = messageNode.contentAsBytes();
        if (encodedMessage.isEmpty()) {
            return MessageContainer.empty();
        }

        return decodeMessageBytes(type, encodedMessage.get(), from, participant);
    }

    private void sendEncMessageSuccessReceipt(Node infoNode, String id, Jid chatJid, Jid senderJid, boolean fromMe) {
        socketHandler.sendMessageAck(chatJid, infoNode).thenComposeAsync(ignored -> {
            if(!socketHandler.store().automaticMessageReceipts()) {
                return CompletableFuture.completedFuture(null);
            }

            var participant = fromMe && senderJid == null ? chatJid : senderJid;
            var category = infoNode.attributes().getString("category");
            var receiptType = getReceiptType(category, fromMe);
            return socketHandler.sendReceipt(chatJid, participant, List.of(id), receiptType);
        });
    }

    private String getReceiptType(String category, boolean fromMe) {
        if (Objects.equals(category, "peer")) {
            return "peer_msg";
        }

        if (fromMe) {
            return "sender";
        }

        if (!socketHandler.store().online()) {
            return "inactive";
        }

        return null;
    }

    private MessageContainer decodeMessageBytes(String type, byte[] encodedMessage, Jid from, Jid participant) {
        try {
            lock.lock();
            var result = switch (type) {
                case SKMSG -> {
                    Objects.requireNonNull(participant, "Cannot decipher skmsg without participant");
                    var senderName = new SenderKeyName(from.toString(), participant.toSignalAddress());
                    var signalGroup = new GroupCipher(senderName, socketHandler.keys());
                    yield signalGroup.decrypt(encodedMessage);
                }
                case PKMSG -> {
                    var user = from.hasServer(JidServer.whatsapp()) ? from : participant;
                    Objects.requireNonNull(user, "Cannot decipher pkmsg without user");
                    var session = new SessionCipher(user.toSignalAddress(), socketHandler.keys());
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(preKey);
                }
                case MSG -> {
                    var user = from.hasServer(JidServer.whatsapp()) ? from : participant;
                    Objects.requireNonNull(user, "Cannot decipher msg without user");
                    var session = new SessionCipher(user.toSignalAddress(), socketHandler.keys());
                    var signalMessage = SignalMessage.ofSerialized(encodedMessage);
                    yield session.decrypt(signalMessage);
                }
                default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
            };
            return Bytes.bytesToMessage(result)
                    .unbox();
        } catch (Throwable throwable) {
            socketHandler.handleFailure(MESSAGE, throwable);
            return MessageContainer.empty();
        }finally {
            lock.unlock();
        }
    }

    private void attributeMessageReceipt(ChatMessageInfo info) {
        var self = socketHandler.store()
                .jid()
                .map(Jid::toSimpleJid)
                .orElse(null);
        if (!info.fromMe() || (self != null && !info.chatJid().equals(self))) {
            return;
        }
        info.receipt().readTimestampSeconds(info.timestampSeconds().orElse(0L));
        info.receipt().deliveredJids().add(self);
        info.receipt().readJids().add(self);
        info.setStatus(MessageStatus.READ);
    }

    private void saveMessage(ChatMessageInfo info, boolean notify) {
        info.message()
                .senderKeyDistributionMessage()
                .ifPresent(keyDistributionMessage -> handleDistributionMessage(keyDistributionMessage, info.senderJid()));

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

        var chat = info.chat()
                .orElseGet(() -> socketHandler.store().addNewChat(info.chatJid()));
        var result = chat.addNewMessage(info);
        if (!result || info.timestampSeconds().orElse(0L) <= socketHandler.store().initializationTimeStamp()) {
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

        if (notify) {
            socketHandler.onNewMessage(info);
        }
    }

    private void handleDistributionMessage(SenderKeyDistributionMessage distributionMessage, Jid from) {
        var groupName = new SenderKeyName(distributionMessage.groupId(), from.toSignalAddress());
        var builder = new GroupBuilder(socketHandler.keys());
        var message = SignalDistributionMessage.ofSerialized(distributionMessage.data());
        builder.createIncoming(groupName, message);
    }

    private void handleProtocolMessage(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        switch (protocolMessage.protocolType()) {
            case HISTORY_SYNC_NOTIFICATION -> onHistorySyncNotification(info, protocolMessage);
            case APP_STATE_SYNC_KEY_SHARE -> onAppStateSyncKeyShare(protocolMessage);
            case REVOKE -> onMessageRevoked(info, protocolMessage);
            case EPHEMERAL_SETTING -> onEphemeralSettings(info, protocolMessage);
            case null, default -> {}
        }
    }

    private void onEphemeralSettings(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        var chat = info.chat().orElse(null);
        var timestampSeconds = info.timestampSeconds().orElse(0L);
        if (chat != null) {
            chat.setEphemeralMessagesToggleTimeSeconds(timestampSeconds);
            chat.setEphemeralMessageDuration(ChatEphemeralTimer.of((int) protocolMessage.ephemeralExpiration()));
        }
        var setting = new EphemeralSettings((int) protocolMessage.ephemeralExpiration(), timestampSeconds);
        socketHandler.onSetting(setting);
    }

    private void onMessageRevoked(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        var id = protocolMessage.key().orElseThrow().id();
        info.chat()
                .flatMap(chat -> socketHandler.store().findMessageById(chat, id))
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
        socketHandler.pullInitialPatches();
    }

    private void onHistorySyncNotification(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        scheduleHistorySyncTimeout();

        if(isHistorySyncIgnorable(protocolMessage)) {
            return;
        }

        downloadHistorySync(protocolMessage)
                .thenAcceptAsync(this::onHistoryNotification)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(HISTORY_SYNC, throwable))
                .thenRunAsync(() -> socketHandler.sendReceipt(info.chatJid(), null, List.of(info.id()), "hist_sync"));
    }

    private boolean isHistorySyncIgnorable(ProtocolMessage protocolMessage) {
        return socketHandler.store().webHistorySetting().isZero()
                && protocolMessage.historySyncNotification()
                    .filter(entry -> entry.syncType() == Type.RECENT)
                    .isPresent();
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
                .map(result -> CompletableFuture.completedFuture(HistorySyncSpec.decode(Bytes.decompress(result))))
                .orElseGet(() -> {
                    var proxy = switch (socketHandler.store().mediaProxySetting()) {
                        case NONE, UPLOADS -> null;
                        case DOWNLOADS, ALL -> socketHandler.store().proxy().orElse(null);
                    };
                    return Medias.downloadAsync(notification, proxy)
                            .thenApplyAsync(entry -> entry.orElseThrow(() -> new NoSuchElementException("Cannot download history sync")))
                            .thenApplyAsync(result -> HistorySyncSpec.decode(Bytes.decompress(result)));
                });
    }

    private void onHistoryNotification(HistorySync history) {
        handleHistorySync(history);
        if (history.progress() == null) {
            return;
        }

        var recent = history.syncType() == Type.RECENT;
        if(recent) {
            recentHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if(recentHistorySyncTracker.isDone()) {
                socketHandler.onHistorySyncProgress(history.progress(), true);
            }
        }else {
            fullHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if(fullHistorySyncTracker.isDone()) {
                socketHandler.onHistorySyncProgress(history.progress(), false);
            }
        }
    }

    private void onMessageDeleted(ChatMessageInfo info, ChatMessageInfo message) {
        info.chat().ifPresent(chat -> chat.removeMessage(message));
        message.setRevokeTimestampSeconds(Clock.nowSeconds());
        socketHandler.onMessageDeleted(message, true);
    }

    private void handleHistorySync(HistorySync history) {
        switch (history.syncType()) {
            case INITIAL_STATUS_V3 -> handleInitialStatus(history);
            case PUSH_NAME -> handlePushNames(history);
            case INITIAL_BOOTSTRAP -> handleInitialBootstrap(history);
            case FULL -> handleChatsSync(history, false);
            case RECENT -> handleChatsSync(history, true);
            case NON_BLOCKING_DATA -> handleNonBlockingData(history);
        }
    }

    private void handleInitialStatus(HistorySync history) {
        for (var messageInfo : history.statusV3Messages()) {
            socketHandler.store().addStatus(messageInfo);
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
        if (!socketHandler.store().webHistorySetting().isZero()) {
            var jids = history.conversations()
                    .stream()
                    .map(Chat::jid)
                    .toList();
            historyCache.addAll(jids);
        }

        handleConversations(history);
        socketHandler.onChats();
    }

    private void handleChatsSync(HistorySync history, boolean recent) {
        if (socketHandler.store().webHistorySetting().isZero()) {
            return;
        }

        handleConversations(history);
        handleConversationsNotifications(history, recent);
        scheduleHistorySyncTimeout();
    }

    private void handleConversationsNotifications(HistorySync history, boolean recent) {
        var toRemove = new HashSet<Jid>();
        for (var cachedJid : historyCache) {
            var chat = socketHandler.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if (chat == null) {
                continue;
            }

            var done = !recent && !history.conversations().contains(chat);
            if (done) {
                chat.setEndOfHistoryTransfer(true);
                chat.setEndOfHistoryTransferType(Chat.EndOfHistoryTransferType.COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY);
                toRemove.add(cachedJid);
            }

            socketHandler.onChatRecentMessages(chat, done);
        }

        historyCache.removeAll(toRemove);
    }

    private void scheduleHistorySyncTimeout() {
        if (historySyncTask != null && !historySyncTask.isDone()) {
            historySyncTask.cancel(true);
        }

        this.historySyncTask = socketHandler.scheduleDelayed(this::onForcedHistorySyncCompletion, HISTORY_SYNC_MAX_TIMEOUT);
    }

    private void onForcedHistorySyncCompletion() {
        for (var cachedJid : historyCache) {
            var chat = socketHandler.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if (chat == null) {
                continue;
            }

            socketHandler.onChatRecentMessages(chat, true);
        }

        historyCache.clear();
    }


    private void handleConversations(HistorySync history) {
        for (var chat : history.conversations()) {
            for (var message : chat.messages()) {
                attributeChatMessage(message.messageInfo());
            }

            socketHandler.store().addChat(chat);
        }
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            socketHandler.pastParticipants().put(pastParticipants.groupJid(), pastParticipants.pastParticipants());
        }
    }

    @SafeVarargs
    private <T> List<T> toSingleList(List<T>... all) {
        return switch (all.length) {
            case 0 -> List.of();
            case 1 -> all[0];
            default -> Stream.of(all)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();
        };
    }

    private void attributeSender(ChatMessageInfo info, Jid senderJid) {
        if(senderJid.server() != JidServer.whatsapp() && senderJid.server() != JidServer.user()) {
            return;
        }

        var contact = socketHandler.store().findContactByJid(senderJid)
                .orElseGet(() -> socketHandler.store().addContact(new Contact(senderJid)));
        info.setSender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageChatJid().ifPresent(chatJid -> attributeContextChat(contextInfo, chatJid));
    }

    private void attributeContextChat(ContextInfo contextInfo, Jid chatJid) {
        var chat = socketHandler.store().findChatByJid(chatJid)
                .orElseGet(() -> socketHandler.store().addNewChat(chatJid));
        contextInfo.setQuotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        var contact = socketHandler.store().findContactByJid(senderJid)
                .orElseGet(() -> socketHandler.store().addContact(new Contact(senderJid)));
        contextInfo.setQuotedMessageSender(contact);
    }

    protected void attributeChatMessage(ChatMessageInfo info) {
        var chat = socketHandler.store().findChatByJid(info.chatJid())
                .orElseGet(() -> socketHandler.store().addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = socketHandler.store().jid().orElse(null);
        if (info.fromMe() && me != null) {
            info.key().setSenderJid(me.toSimpleJid());
        }

        attributeSender(info, info.senderJid());
        info.message()
                .contentWithContext()
                .flatMap(ContextualMessage::contextInfo)
                .ifPresent(this::attributeContext);
        processMessageWithSecret(info);
    }

    private void processMessageWithSecret(ChatMessageInfo info) {
        switch (info.message().content()) {
            case PollCreationMessage pollCreationMessage -> handlePollCreation(info, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> handlePollUpdate(info, pollUpdateMessage);
            case ReactionMessage reactionMessage -> handleReactionMessage(info, reactionMessage);
            default -> {}
        }
    }

    private void handlePollCreation(ChatMessageInfo info, PollCreationMessage pollCreationMessage) {
        if (pollCreationMessage.encryptionKey().isPresent()) {
            return;
        }

        info.message()
                .deviceInfo()
                .flatMap(DeviceContextInfo::messageSecret)
                .or(info::messageSecret)
                .ifPresent(pollCreationMessage::setEncryptionKey);
    }

    private void handlePollUpdate(ChatMessageInfo info, PollUpdateMessage pollUpdateMessage) {
        var originalPollInfo = socketHandler.store().findMessageByKey(pollUpdateMessage.pollCreationMessageKey());
        if(originalPollInfo.isEmpty()) {
            return;
        }

        if(!(originalPollInfo.get().message().content() instanceof  PollCreationMessage originalPollMessage)) {
            return;
        }

        pollUpdateMessage.setPollCreationMessage(originalPollMessage);
        var originalPollSender = originalPollInfo.get()
                .senderJid()
                .toSimpleJid()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        var modificationSenderJid = info.senderJid().toSimpleJid();
        pollUpdateMessage.setVoter(modificationSenderJid);
        var modificationSender = modificationSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var secretName = pollUpdateMessage.secretName().getBytes(StandardCharsets.UTF_8);
        var useSecretPayload = Bytes.concat(
                originalPollInfo.get().id().getBytes(StandardCharsets.UTF_8),
                originalPollSender,
                modificationSender,
                secretName
        );
        var encryptionKey = originalPollMessage.encryptionKey()
                .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
        var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload, 32);
        var additionalData = "%s\0%s".formatted(
                originalPollInfo.get().id(),
                modificationSenderJid
        );
        var metadata = pollUpdateMessage.encryptedMetadata()
                .orElseThrow(() -> new NoSuchElementException("Missing encrypted metadata"));
        var decrypted = AesGcm.decrypt(metadata.iv(), metadata.payload(), useCaseSecret, additionalData.getBytes(StandardCharsets.UTF_8));
        var pollVoteMessage = PollUpdateEncryptedOptionsSpec.decode(decrypted);
        var selectedOptions = pollVoteMessage.selectedOptions()
                .stream()
                .map(sha256 -> originalPollMessage.getSelectableOption(HexFormat.of().formatHex(sha256)))
                .flatMap(Optional::stream)
                .toList();
        originalPollMessage.addSelectedOptions(modificationSenderJid, selectedOptions);
        pollUpdateMessage.setVotes(selectedOptions);
        var update = new PollUpdate(info.key(), pollVoteMessage, Clock.nowMilliseconds());
        info.pollUpdates().add(update);
    }

    private void handleReactionMessage(ChatMessageInfo info, ReactionMessage reactionMessage) {
        info.setIgnore(true);
        socketHandler.store().findMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    protected void dispose() {
        historyCache.clear();
        if(historySyncTask != null) {
            historySyncTask.cancel(true);
            historySyncTask = null;
        }
        recentHistorySyncTracker.clear();
        fullHistorySyncTracker.clear();
    }

    private static class HistorySyncProgressTracker {
        private final BitSet chunksMarker;
        private final AtomicInteger chunkEnd;
        private HistorySyncProgressTracker() {
            this.chunksMarker = new BitSet();
            this.chunkEnd = new AtomicInteger(0);
        }

        private boolean isDone() {
            var chunkEnd = this.chunkEnd.get();
            return chunkEnd > 0 && IntStream.range(0, chunkEnd)
                    .allMatch(chunksMarker::get);
        }

        private void commit(int chunk, boolean finished) {
            if(finished) {
                chunkEnd.set(chunk);
            }

            chunksMarker.set(chunk);
        }

        private void clear() {
            chunksMarker.clear();
            chunkEnd.set(0);
        }
    }
}
