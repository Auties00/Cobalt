package it.auties.whatsapp.socket.message;

import it.auties.linkpreview.LinkPreview;
import it.auties.linkpreview.LinkPreviewMatch;
import it.auties.linkpreview.LinkPreviewMedia;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.api.WhatsappTextPreviewPolicy;
import it.auties.whatsapp.crypto.*;
import it.auties.whatsapp.model.action.ContactActionBuilder;
import it.auties.whatsapp.model.business.BusinessVerifiedNameCertificateSpec;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplate;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplate;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.GroupOrCommunityMetadata;
import it.auties.whatsapp.model.chat.ChatParticipant;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MediaFile;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.server.DeviceSentMessageBuilder;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.server.SenderKeyDistributionMessageBuilder;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.poll.*;
import it.auties.whatsapp.model.request.MessageRequest;
import it.auties.whatsapp.model.setting.EphemeralSettingsBuilder;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentitySpec;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.SignalDistributionMessage;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.sync.HistorySync;
import it.auties.whatsapp.model.sync.HistorySyncNotification;
import it.auties.whatsapp.model.sync.HistorySyncSpec;
import it.auties.whatsapp.model.sync.PushName;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.HISTORY_SYNC;
import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.MESSAGE;
import static it.auties.whatsapp.util.SignalConstants.*;

public final class MessageComponent {
    private static final int HISTORY_SYNC_MAX_TIMEOUT = 25;
    private static final Set<HistorySync.Type> REQUIRED_HISTORY_SYNC_TYPES = Set.of(HistorySync.Type.INITIAL_BOOTSTRAP, HistorySync.Type.PUSH_NAME, HistorySync.Type.NON_BLOCKING_DATA);

    private final SocketConnection socketConnection;
    private final Map<Jid, CopyOnWriteArrayList<Jid>> devicesCache;
    private final Set<Jid> historyCache;
    private final HistorySyncProgressTracker recentHistorySyncTracker;
    private final HistorySyncProgressTracker fullHistorySyncTracker;
    private final Set<HistorySync.Type> historySyncTypes;
    private final SignalSession sessionCipher;
    private final ReentrantLock sessionCipherLock;
    private ScheduledFuture<?> historySyncTask;

    public MessageComponent(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
        this.devicesCache = new ConcurrentHashMap<>();
        this.historyCache = ConcurrentHashMap.newKeySet();
        this.historySyncTypes = ConcurrentHashMap.newKeySet();
        this.recentHistorySyncTracker = new HistorySyncProgressTracker();
        this.fullHistorySyncTracker = new HistorySyncProgressTracker();
        this.sessionCipher = new SignalSession(socketConnection.keys());
        this.sessionCipherLock = new ReentrantLock(true);
    }

    public void encode(MessageRequest request) {
        switch (request) {
            case MessageRequest.Chat chatRequest -> encodeChatMessage(chatRequest);
            case MessageRequest.Newsletter newsletterRequest -> encodeNewsletterMessage(newsletterRequest);
        }
    }

    private void encodeChatMessage(MessageRequest.Chat request) {
        try {
            prepareOutgoingChatMessage(request.info());
            var node = encodeRequest(request);
            socketConnection.sendNode(node);
            attributeMessageReceipt(request.info());
        } catch (Throwable throwable) {
            request.info().setStatus(MessageStatus.ERROR);
            socketConnection.handleFailure(MESSAGE, throwable);
        }finally {
            saveMessage(request.info(), false);
        }
    }

    private void prepareOutgoingChatMessage(MessageInfo messageInfo) {
        switch (messageInfo.message().content()) {
            case MediaMessage mediaMessage -> attributeMediaMessage(messageInfo.parentJid(), mediaMessage);
            case ButtonMessage buttonMessage -> attributeButtonMessage(messageInfo.parentJid(), buttonMessage);
            case TextMessage textMessage -> attributeTextMessage(textMessage);
            case PollCreationMessage pollCreationMessage when messageInfo instanceof ChatMessageInfo pollCreationInfo ->
                    attributePollCreationMessage(pollCreationInfo, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage when messageInfo instanceof ChatMessageInfo pollUpdateInfo ->
                    attributePollUpdateMessage(pollUpdateInfo, pollUpdateMessage);
            default -> {
            }
        }

        if (messageInfo instanceof ChatMessageInfo chatMessageInfo) {
            attributeChatMessage(chatMessageInfo);
            fixEphemeralMessage(chatMessageInfo);
        }
    }

    private void fixEphemeralMessage(ChatMessageInfo info) {
        if (info.message().hasCategory(Message.Category.SERVER)) {
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

        if (info.message().type() != Message.Type.EPHEMERAL) {
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

    private void attributeTextMessage(TextMessage textMessage) {
        if (socketConnection.store().textPreviewSetting() == WhatsappTextPreviewPolicy.DISABLED) {
            return;
        }

        try {
            var result = LinkPreview.createPreview(textMessage.text());
            attributeTextMessage(textMessage, result.orElse(null));
        } catch (Exception ignored) {

        }
    }

    private void attributeTextMessage(TextMessage textMessage, LinkPreviewMatch match) {
        if (match == null) {
            return;
        }

        var uri = match.result().uri().toString();
        if (socketConnection.store().textPreviewSetting() == WhatsappTextPreviewPolicy.ENABLED_WITH_INFERENCE
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
        imageThumbnail.ifPresent(data -> {
            try(var stream = data.uri().toURL().openStream()) {
                textMessage.setThumbnail(stream.readAllBytes());
            } catch (Throwable ignored) {

            }
        });
    }

    private LinkPreviewMedia compareDimensions(LinkPreviewMedia first, LinkPreviewMedia second) {
        return first.width() * first.height() > second.width() * second.height()
                ? first
                : second;
    }

    private void attributeMediaMessage(Jid chatJid, MediaMessage mediaMessage) {
        var media = mediaMessage.decodedMedia()
                .orElseThrow(() -> new IllegalArgumentException("Missing media to upload"));
        var attachmentType = getAttachmentType(chatJid, mediaMessage);
        var mediaConnection = socketConnection.store().mediaConnection();
        var userAgent = socketConnection.store()
                .device()
                .toUserAgent(socketConnection.store().version())
                .orElse(null);
        var upload = Medias.upload(media, attachmentType, mediaConnection, userAgent);
        attributeMediaMessage(mediaMessage, upload);
    }

    private AttachmentType getAttachmentType(Jid chatJid, MediaMessage mediaMessage) {
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

    private void attributeMediaMessage(MutableAttachmentProvider attachmentProvider, MediaFile upload) {
        if (attachmentProvider instanceof MediaMessage mediaMessage) {
            mediaMessage.setHandle(upload.handle());
        }

        attachmentProvider.setMediaSha256(upload.fileSha256());
        attachmentProvider.setMediaEncryptedSha256(upload.fileEncSha256());
        attachmentProvider.setMediaKey(upload.mediaKey());
        attachmentProvider.setMediaUrl(upload.url());
        attachmentProvider.setMediaKeyTimestamp(upload.timestamp());
        attachmentProvider.setMediaDirectPath(upload.directPath());
        attachmentProvider.setMediaSize(upload.fileLength());
    }

    private void attributePollCreationMessage(ChatMessageInfo info, PollCreationMessage pollCreationMessage) {
        var pollEncryptionKey = pollCreationMessage.encryptionKey()
                .orElseGet(() -> Bytes.random(32));
        pollCreationMessage.setEncryptionKey(pollEncryptionKey);
        info.setMessageSecret(pollEncryptionKey);
        var metadata = new PollAdditionalMetadataBuilder()
                .pollInvalidated(false)
                .build();
        info.setPollAdditionalMetadata(metadata);
        info.message().deviceInfo().ifPresentOrElse(deviceInfo -> deviceInfo.setMessageSecret(pollEncryptionKey), () -> {
            var deviceInfo = new DeviceContextInfoBuilder()
                    .messageSecret(pollEncryptionKey)
                    .build();
            var message = info.message().withDeviceInfo(deviceInfo);
            info.setMessage(message);
        });
    }

    private void attributePollUpdateMessage(ChatMessageInfo info, PollUpdateMessage pollUpdateMessage) {
        try {
            if (pollUpdateMessage.encryptedMetadata().isPresent()) {
                return;
            }

            var me = socketConnection.store().jid();
            if (me.isEmpty()) {
                return;
            }

            var pollCreationId = pollUpdateMessage.pollCreationMessageKey().id();
            var additionalData = "%s\0%s".formatted(pollCreationId, me.get().withoutData());
            var encryptedOptions = pollUpdateMessage.votes()
                    .stream()
                    .map(this::getPollUpdateOptionHash)
                    .toList();
            var pollUpdateEncryptedOptions = new PollUpdateEncryptedOptionsBuilder()
                    .selectedOptions(encryptedOptions)
                    .build();
            var encryptedPollUpdateEncryptedOptions = PollUpdateEncryptedOptionsSpec.encode(pollUpdateEncryptedOptions);
            var originalPollInfo = socketConnection.store()
                    .findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                    .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
            var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
            var modificationSenderJid = info.senderJid().withoutData();
            pollUpdateMessage.setVoter(modificationSenderJid);
            var originalPollSenderJid = originalPollInfo.senderJid().withoutData();
            var useSecretPayload = pollCreationId + originalPollSenderJid + modificationSenderJid + pollUpdateMessage.secretName();
            var encryptionKey = originalPollMessage.encryptionKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
            var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload.getBytes(), 32);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(useCaseSecret, "AES"),
                    new GCMParameterSpec(128, Bytes.random(12))
            );
            cipher.updateAAD(additionalData.getBytes(StandardCharsets.UTF_8));
            var pollUpdateEncryptedPayload = cipher.doFinal(encryptedPollUpdateEncryptedOptions);
            var pollUpdateEncryptedMetadata = new PollUpdateEncryptedMetadataBuilder()
                    .payload(pollUpdateEncryptedPayload)
                    .iv(Bytes.random(12))
                    .build();
            pollUpdateMessage.setEncryptedMetadata(pollUpdateEncryptedMetadata);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt poll update", exception);
        }
    }

    private byte[] getPollUpdateOptionHash(PollOption entry) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(entry.name().getBytes());
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing sha256 implementation");
        }
    }

    private void attributeButtonMessage(Jid chatJid, ButtonMessage buttonMessage) {
        switch (buttonMessage) {
            case ButtonsMessage buttonsMessage when buttonsMessage.header().isPresent()
                    && buttonsMessage.header().get() instanceof MediaMessage mediaMessage ->
                    attributeMediaMessage(chatJid, mediaMessage);
            case TemplateMessage templateMessage when templateMessage.format().isPresent() -> {
                var templateFormatter = templateMessage.format().get();
                switch (templateFormatter) {
                    case HighlyStructuredFourRowTemplate highlyStructuredFourRowTemplate
                            when highlyStructuredFourRowTemplate.title().isPresent() && highlyStructuredFourRowTemplate.title().get() instanceof MediaMessage fourRowMedia ->
                            attributeMediaMessage(chatJid, fourRowMedia);
                    case HydratedFourRowTemplate hydratedFourRowTemplate when hydratedFourRowTemplate.title().isPresent() && hydratedFourRowTemplate.title().get() instanceof MediaMessage hydratedFourRowMedia ->
                            attributeMediaMessage(chatJid, hydratedFourRowMedia);
                    default -> {
                    }
                }
            }
            case InteractiveMessage interactiveMessage
                    when interactiveMessage.header().isPresent()
                    && interactiveMessage.header().get().attachment().isPresent()
                    && interactiveMessage.header().get().attachment().get() instanceof MediaMessage interactiveMedia ->
                    attributeMediaMessage(chatJid, interactiveMedia);
            default -> {
            }
        }
    }

    private void encodeNewsletterMessage(MessageRequest.Newsletter request) {
        try {
            prepareOutgoingChatMessage(request.info());
            var message = request.info().message();
            var messageNode = getPlainMessageNode(message);
            var type = request.info().message().content() instanceof MediaMessage ? "media" : "text";
            var attributes = Attributes.ofNullable(request.additionalAttributes())
                    .put("id", request.info().id())
                    .put("to", request.info().parentJid())
                    .put("type", type)
                    .put("media_id", getPlainMessageHandle(request), Objects::nonNull)
                    .toMap();
            socketConnection.sendNode(Node.of("message", attributes, messageNode));
            var newsletter = request.info().newsletter();
            newsletter.addMessage(request.info());
        } catch (Throwable throwable) {
            request.info().setStatus(MessageStatus.ERROR);
            socketConnection.handleFailure(MESSAGE, throwable);
        }
    }

    private String getPlainMessageHandle(MessageRequest.Newsletter request) {
        var message = request.info().message().content();
        if (!(message instanceof MediaMessage extendedMediaMessage)) {
            return null;
        }

        return extendedMediaMessage.handle()
                .orElse(null);
    }

    private Node getPlainMessageNode(MessageContainer message) {
        return switch (message.content()) {
            case ReactionMessage reactionMessage -> Node.of("reaction", Map.of("code", reactionMessage.content()));
            case TextMessage textMessage when textMessage.thumbnail().isEmpty() -> {
                var textLength = Scalar.sizeOf(textMessage.text());
                var encodedTextLength = ProtobufOutputStream.getVarIntSize(textLength);
                var encodedText = new byte[1 + textLength + encodedTextLength];
                encodedText[0] = 10;
                var textEncodeResult = StandardCharsets.UTF_8.newEncoder()
                        .encode(CharBuffer.wrap(textMessage.text()), ByteBuffer.wrap(encodedText, 1, 1 + textLength), true);
                if (textEncodeResult.isError()) {
                    throw new RuntimeException("Cannot encode UTF-8 text message: " + textEncodeResult);
                }
                encodeVarInt(textLength, encodedText, textLength);
                yield Node.of("plaintext", encodedText);
            }
            case null, default -> {
                var messageAttributes = Attributes.of()
                        .put("mediatype", getMediaType(message), Objects::nonNull)
                        .toMap();
                var encodedMessage = message.isEmpty() ? null : MessageContainerSpec.encode(message);
                yield Node.of("plaintext", messageAttributes, encodedMessage);
            }
        };
    }

    private void encodeVarInt(int value, byte[] output, int offset) {
        var position = 0;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                output[1 + offset + position] = (byte) value;
                return;
            } else {
                output[1 + offset + position++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    private Node encodeGroup(MessageRequest.Chat request) {
        var encodedMessage = messageToBytes(request.info().message());
        var sender = socketConnection.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var senderName = new SenderKeyName(request.info().chatJid(), sender.toSignalAddress());
        var signalMessage = sessionCipher.createOutgoing(senderName);
        var groupMessage = sessionCipher.encrypt(senderName, encodedMessage);
        var messageNode = createMessageNode(request, groupMessage);
        if (request.hasRecipientOverride()) {
            var allDevices = queryDevices(request.recipients(), false);
            var preKeys = createGroupNodes(request, signalMessage, allDevices, request.force());
            var encodedMessageNode = createEncodedMessageNode(request, preKeys, messageNode);
            return socketConnection.sendNode(encodedMessageNode);
        }

        if (Jid.statusBroadcastAccount().equals(request.info().chatJid())) {
            var recipients = socketConnection.store()
                    .contacts()
                    .stream()
                    .map(Contact::jid)
                    .toList();
            var allDevices = queryDevices(recipients, false);
            var preKeys = createGroupNodes(request, signalMessage, allDevices, true);
            var encodedMessageNode = createEncodedMessageNode(request, preKeys, messageNode);
            return socketConnection.sendNode(encodedMessageNode);
        }

        var groupMetadata = socketConnection.queryGroupOrCommunityMetadata(request.info().chatJid());
        var allDevices = getGroupDevices(groupMetadata);
        var preKeys = createGroupNodes(request, signalMessage, allDevices, request.force());
        return createEncodedMessageNode(request, preKeys, messageNode);
    }

    private Node encodeRequest(MessageRequest.Chat request) {
        try {
            sessionCipherLock.lock();
            return request.peer() || isConversation(request.info())
                    ? encodeConversation(request)
                    : encodeGroup(request);
        }finally {
            sessionCipherLock.unlock();
        }
    }

    private Node encodeConversation(MessageRequest.Chat request) {
        var sender = socketConnection.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var encodedMessage = messageToBytes(request.info().message());
        if (request.peer()) {
            var chatJid = request.info().chatJid();
            var peerNode = createMessageNode(request, chatJid, encodedMessage, true);
            return createEncodedMessageNode(request, List.of(peerNode), null);
        }

        var deviceMessage = new DeviceSentMessageBuilder()
                .destinationJid(request.info().chatJid())
                .message(request.info().message())
                .build();
        var encodedDeviceMessage = messageToBytes(MessageContainer.of(deviceMessage));
        var recipients = getRecipients(request);
        var allDevices = queryDevices(recipients, !isMe(request.info().chatJid()));
        var sessions = createConversationNodes(request, allDevices, encodedMessage, encodedDeviceMessage);
        return createEncodedMessageNode(request, sessions, null);
    }

    private Set<Jid> getRecipients(MessageRequest.Chat request) {
        if (request.hasRecipientOverride()) {
            return request.recipients();
        }

        if (request.peer()) {
            return Set.of(request.info().chatJid());
        }

        return new HashSet<>(List.of(socketConnection.store().jid().orElseThrow().withoutData(), request.info().chatJid()));
    }

    private boolean isConversation(ChatMessageInfo info) {
        return info.chatJid().hasServer(JidServer.user())
                || info.chatJid().hasServer(JidServer.legacyUser());
    }

    private Node createEncodedMessageNode(MessageRequest.Chat request, List<Node> preKeys, Node descriptor) {
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
            socketConnection.keys().setCompanionIdentity()
                    .ifPresent(companionIdentity -> body.add(Node.of("device-identity", SignedDeviceIdentitySpec.encode(companionIdentity))));
        }

        var attributes = Attributes.ofNullable(request.additionalAttributes())
                .put("id", request.info().id())
                .put("to", request.info().chatJid())
                .put("type", request.info().message().content() instanceof MediaMessage ? "media" : "text")
                .put("verified_name", socketConnection.store().verifiedName().orElse(""), socketConnection.store().verifiedName().isPresent() && !request.peer())
                .put("category", "peer", request.peer())
                .put("duration", "900", request.info().message().type() == Message.Type.LIVE_LOCATION)
                .put("device_fanout", false, request.info().message().type() == Message.Type.BUTTONS)
                .put("push_priority", "high", isAppStateKeyShare(request))
                .toMap();
        return Node.of("message", attributes, body);
    }

    private boolean isAppStateKeyShare(MessageRequest.Chat request) {
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

    private List<Node> createConversationNodes(MessageRequest.Chat request, List<Jid> contacts, byte[] message, byte[] deviceMessage) {
        var jid = socketConnection.store()
                .jid()
                .orElse(null);
        if (jid == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var partitioned = contacts.stream()
                .collect(Collectors.partitioningBy(contact -> Objects.equals(contact.user(), jid.user())));
        querySessions(partitioned.get(true), request.force());
        var companions = createMessageNodes(request, partitioned.get(true), deviceMessage);
        querySessions(partitioned.get(false), request.force());
        var others = createMessageNodes(request, partitioned.get(false), message);
        return toSingleList(companions, others);
    }

    private List<Node> createGroupNodes(MessageRequest.Chat request, byte[] distributionMessage, List<Jid> participants, boolean force) {
        var missingParticipants = participants.stream()
                .filter(participant -> force || !socketConnection.keys().hasGroupKeys(request.info().chatJid(), participant))
                .toList();
        if (missingParticipants.isEmpty()) {
            return List.of();
        }
        var whatsappMessage = new SenderKeyDistributionMessageBuilder()
                .groupJid(request.info().chatJid())
                .data(distributionMessage)
                .build();
        var paddedMessage = messageToBytes(MessageContainer.of(whatsappMessage));
        querySessions(missingParticipants, force);
        var results = createMessageNodes(request, missingParticipants, paddedMessage);
        socketConnection.keys().addRecipientsWithPreKeys(request.info().chatJid(), missingParticipants);
        return results;
    }

    public void querySessions(Collection<Jid> contacts, boolean force) {
        var missingSessions = contacts.stream()
                .filter(contact -> force || !socketConnection.keys().hasSession(contact.toSignalAddress()))
                .map(contact -> Node.of("user", Map.of("jid", contact)))
                .toList();
        if (missingSessions.isEmpty()) {
            return;
        }
        querySession(missingSessions);
    }

    private void querySession(List<Node> children) {
        var result = socketConnection.sendQuery("get", "encrypt", Node.of("key", children));
        parseSessions(result);
    }

    private List<Node> createMessageNodes(MessageRequest.Chat request, List<Jid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(request, contact, message, false))
                .toList();
    }

    private Node createMessageNode(MessageRequest.Chat request, Jid contact, byte[] message, boolean peer) {
        var encrypted = sessionCipher.encrypt(contact.toSignalAddress(), message);
        var messageNode = createMessageNode(request, encrypted);
        return peer ? messageNode : Node.of("to", Map.of("jid", contact), messageNode);
    }

    private List<Jid> getGroupDevices(GroupOrCommunityMetadata metadata) {
        var jids = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .toList();
        return queryDevices(jids, false);
    }

    public List<Jid> queryDevices(Collection<Jid> contacts, boolean excludeSelf) {
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
        if (contactNodes.isEmpty()) {
            return cachedDevices;
        }

        var body = Node.of("usync",
                Map.of("context", "message", "index", "0", "last", "true", "mode", "query", "sid", SocketConnection.randomSid()),
                Node.of("query", Node.of("devices", Map.of("version", "2"))),
                Node.of("list", contactNodes));
        var result = socketConnection.sendQuery("get", "usync", body);
        return toSingleList(cachedDevices, parseDevices(result, excludeSelf));
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
        if (devices.isEmpty()) {
            return excludeSelf && isMe(jid) ? List.of() : List.of(jid);
        }

        return devices.stream()
                .map(child -> parseDeviceId(child, jid, excludeSelf))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Jid> parseDeviceId(Node child, Jid jid, boolean excludeSelf) {
        var deviceId = child.attributes().getInt("id");
        if (!child.description().equals("device")) {
            return Optional.empty();
        }

        if (deviceId != 0 && !child.attributes().hasKey("key-index")) {
            return Optional.empty();
        }

        var result = jid.withDevice(deviceId);
        cacheDevice(result);
        if (excludeSelf && isMe(result)) {
            return Optional.empty();
        }

        return Optional.of(jid.withDevice(deviceId));
    }

    private boolean isMe(Jid jid) {
        var self = socketConnection.store().jid().orElse(null);
        if (self == null) {
            return false;
        }

        return jid.user().equals(self.user()) && Objects.equals(self.device(), jid.device());
    }

    private void cacheDevice(Jid jid) {
        var cachedDevices = devicesCache.get(jid.withoutData());
        if (cachedDevices != null) {
            cachedDevices.add(jid);
            return;
        }

        var devices = new CopyOnWriteArrayList<Jid>();
        devices.add(jid);
        devicesCache.put(jid.withoutData(), devices);
    }

    void parseSessions(Node node) {
        if (node == null) {
            return;
        }

        try {
            sessionCipherLock.lock();
            node.findChild("list")
                    .orElseThrow(() -> new IllegalArgumentException("Cannot parse sessions: " + node))
                    .listChildren("user")
                    .forEach(this::parseSession);
        }finally {
            sessionCipherLock.unlock();
        }
    }

    private void parseSession(Node node) {
        if (node.hasNode("error")) {
            throw new IllegalArgumentException("Erroneous session node");
        }
        var jid = node.attributes()
                .getRequiredJid("jid");
        var registrationId = node.findChild("registration")
                .map(id -> Scalar.bytesToInt(id.contentAsBytes().orElseThrow(), 4))
                .orElseThrow(() -> new NoSuchElementException("Missing id"));
        var identity = node.findChild("identity")
                .flatMap(Node::contentAsBytes)
                .map(SignalConstants::createSignalKey)
                .orElseThrow(() -> new NoSuchElementException("Missing identity"));
        var signedKey = node.findChild("skey")
                .flatMap(SignalSignedKeyPair::of)
                .orElseThrow(() -> new NoSuchElementException("Missing signed key"));
        var key = node.findChild("key")
                .flatMap(SignalSignedKeyPair::of)
                .orElse(null);
        sessionCipher.createOutgoing(jid.toSignalAddress(), registrationId, identity, signedKey, key);
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
            socketConnection.handleFailure(MESSAGE, throwable);
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
                .flatMap(certificate -> certificate.details().name());
    }

    private Node createMessageNode(MessageRequest.Chat request, SignalSession.Result groupMessage) {
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
            sendPlainMessageSuccessReceipt(messageNode, notify, newsletterJid, messageId);

            var newsletter = socketConnection.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
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
                        var message = new NewsletterMessageInfoBuilder()
                                .id(messageId)
                                .serverId(serverId)
                                .timestampSeconds(timestamp)
                                .views(views)
                                .reactions(reactions)
                                .message(messageContainer)
                                .status(readStatus)
                                .build();
                        message.setNewsletter(newsletter.get());
                        return message;
                    });
            if (result.isEmpty()) {
                return;
            }

            newsletter.get()
                    .addMessage(result.get());
            if (notify) {
                socketConnection.onNewMessage(result.get());
            }
            socketConnection.onReply(result.get());
        } catch (Throwable throwable) {
            socketConnection.handleFailure(MESSAGE, throwable);
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
            sendPlainMessageSuccessReceipt(messageNode, notify, newsletterJid, messageId);

            var newsletter = socketConnection.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return;
            }

            var message = socketConnection.store()
                    .findMessageById(newsletter.get(), messageId);
            if (message.isEmpty()) {
                return;
            }

            var myReaction = isSender ? message.get()
                    .reactions()
                    .stream()
                    .filter(NewsletterReaction::fromMe)
                    .findFirst()
                    .orElse(null) : null;
            if (myReaction != null) {
                message.get().decrementReaction(myReaction.content());
            }

            var code = reactionNode.attributes()
                    .getOptionalString("code");
            if (code.isEmpty()) {
                return;
            }

            message.get().incrementReaction(code.get(), isSender);
        } catch (Throwable throwable) {
            socketConnection.handleFailure(MESSAGE, throwable);
        }
    }

    private void decodeChatMessage(Node infoNode, Node messageNode, String businessName, boolean notify) {
        ChatMessageKey chatMessageKey = null;
        try {
            var selfJid = socketConnection.store()
                    .jid()
                    .orElse(null);
            if (selfJid == null) {
                return;
            }

            var pushName = infoNode.attributes().getNullableString("notify");
            var timestamp = infoNode.attributes().getLong("t");
            var id = infoNode.attributes().getRequiredString("id");
            var from = infoNode.attributes()
                    .getRequiredJid("from");
            var messageBuilder = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING);
            var keyBuilder = new ChatMessageKeyBuilder()
                    .id(id);
            if (from.hasServer(JidServer.user()) || from.hasServer(JidServer.legacyUser())) {
                var recipient = infoNode.attributes()
                        .getOptionalJid("recipient")
                        .orElse(from);
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from.withoutData(), selfJid.withoutData()));
                messageBuilder.senderJid(from);
            }else if(from.hasServer(JidServer.bot())) {
                var meta = infoNode.findChild("meta")
                        .orElseThrow();
                var chatJid = meta.attributes()
                        .getRequiredJid("target_chat_jid");
                var senderJid = meta.attributes()
                        .getOptionalJid("target_sender_jid")
                        .orElse(chatJid);
                keyBuilder.chatJid(chatJid);
                keyBuilder.senderJid(senderJid);
                keyBuilder.fromMe(Objects.equals(senderJid.withoutData(), selfJid.withoutData()));
            } else if(from.hasServer(JidServer.groupOrCommunity()) || from.hasServer(JidServer.broadcast()) || from.hasServer(JidServer.newsletter())) {
                var participant = infoNode.attributes()
                        .getOptionalJid("participant")
                        .or(() -> infoNode.attributes().getOptionalJid("sender_pn"))
                        .orElseThrow(() -> new NoSuchElementException("Missing sender"));
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.withoutData(), selfJid.withoutData()));
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }else {
                throw new RuntimeException("Unknown jid server: " + from.server());
            }
            chatMessageKey = keyBuilder.build();
            if (selfJid.equals(chatMessageKey.senderJid().orElse(null))) {
                return;
            }

            ChatMessageInfo info;
            try {
                sessionCipherLock.lock();
                var message = decodeChatMessageContainer(chatMessageKey, messageNode);
                info = messageBuilder.key(chatMessageKey)
                        .broadcast(chatMessageKey.chatJid().hasServer(JidServer.broadcast()))
                        .pushName(pushName)
                        .status(MessageStatus.DELIVERED)
                        .businessVerifiedName(businessName)
                        .timestampSeconds(timestamp)
                        .message(message)
                        .build();
                message.senderKeyDistributionMessage().ifPresent(keyDistributionMessage -> {
                    var groupName = new SenderKeyName(keyDistributionMessage.groupJid(), info.senderJid().toSignalAddress());
                    var signalDistributionMessage = SignalDistributionMessage.ofSerialized(keyDistributionMessage.data());
                    sessionCipher.createIncoming(groupName, signalDistributionMessage);
                });
            }finally {
                sessionCipherLock.unlock();
            }

            attributeMessageReceipt(info);
            attributeChatMessage(info);
            saveMessage(info, notify);
            socketConnection.onReply(info);
        } catch (Throwable throwable) {
            socketConnection.handleFailure(MESSAGE, throwable);
        }finally {
            if(chatMessageKey != null) {
                sendEncMessageSuccessReceipt(
                        infoNode,
                        chatMessageKey
                );
            }
        }
    }

    private MessageContainer decodeChatMessageContainer(ChatMessageKey messageKey, Node messageNode) {
        if (messageNode == null) {
            return MessageContainer.empty();
        }

        var type = messageNode.attributes().getRequiredString("type");
        var encodedMessage = messageNode.contentAsBytes();
        if (encodedMessage.isEmpty()) {
            return MessageContainer.empty();
        }

        return decodeMessageBytes(messageKey, type, encodedMessage.get());
    }

    private void sendEncMessageSuccessReceipt(Node infoNode, ChatMessageKey key) {
        socketConnection.sendMessageAck(key.chatJid(), infoNode);
        if (!socketConnection.store().automaticMessageReceipts()) {
            return;
        }

        var participant = key.fromMe() && key.senderJid().isEmpty() ? key.chatJid() : key.senderJid().get();
        var category = infoNode.attributes().getString("category");
        var receiptType = getReceiptType(category, key.fromMe());
        socketConnection.sendReceipt(key.chatJid(), participant, List.of(key.id()), receiptType);
    }

    private void sendPlainMessageSuccessReceipt(Node messageNode, boolean notify, Jid newsletterJid, String messageId) {
        if (!notify) {
            return;
        }

        socketConnection.sendMessageAck(newsletterJid, messageNode);
        if (!socketConnection.store().automaticMessageReceipts()) {
            return;
        }

        var receiptType = getReceiptType("newsletter", false);
        socketConnection.sendReceipt(newsletterJid, null, List.of(messageId), receiptType);
    }

    private String getReceiptType(String category, boolean fromMe) {
        if (Objects.equals(category, "peer")) {
            return "peer_msg";
        }

        if (fromMe) {
            return "sender";
        }

        if (!socketConnection.store().online()) {
            return "inactive";
        }

        return null;
    }

    private MessageContainer decodeMessageBytes(ChatMessageKey messageKey, String type, byte[] encodedMessage) {
        try {
            if(MSMG.equals(type)) {
                return MessageContainer.empty();
            }

            var result = switch (type) {
                case MSG -> {
                    var signalAddress = messageKey.senderJid()
                            .orElse(messageKey.chatJid())
                            .toSignalAddress();
                    var signalMessage = SignalMessage.ofSerialized(encodedMessage);
                    yield sessionCipher.decrypt(signalAddress, signalMessage);
                }
                case PKMSG -> {
                    var signalAddress = messageKey.senderJid()
                            .orElse(messageKey.chatJid())
                            .toSignalAddress();
                    var preKey = SignalPreKeyMessage.ofSerialized(encodedMessage);
                    yield sessionCipher.decrypt(signalAddress, preKey);
                }
                case SKMSG -> {
                    var groupJid = messageKey.chatJid();
                    var signalAddress = messageKey.senderJid()
                            .orElseThrow(() -> new IllegalArgumentException("Missing sender jid"))
                            .toSignalAddress();
                    var senderName = new SenderKeyName(groupJid, signalAddress);
                    yield sessionCipher.decrypt(senderName, encodedMessage);
                }
                default -> throw new IllegalArgumentException("Unsupported encoded message type: %s".formatted(type));
            };
            var messageLength = result.length - result[result.length - 1];
            return MessageContainerSpec.decode(ProtobufInputStream.fromBytes(result, 0, messageLength))
                    .unbox();
        } catch (Throwable throwable) {
            socketConnection.handleFailure(MESSAGE, throwable);
            return MessageContainer.empty();
        }
    }

    private void attributeMessageReceipt(ChatMessageInfo info) {
        var self = socketConnection.store()
                .jid()
                .map(Jid::withoutData)
                .orElse(null);
        if (!info.fromMe() || (self != null && !info.chatJid().equals(self))) {
            return;
        }
        info.receipt().setReadTimestampSeconds(info.timestampSeconds().orElse(0L));
        info.receipt().addDeliveredJid(self);
        info.receipt().addReadJid(self);
        info.setStatus(MessageStatus.READ);
    }

    private void saveMessage(ChatMessageInfo info, boolean notify) {
        if (Jid.statusBroadcastAccount().equals(info.chatJid())) {
            socketConnection.store().addStatus(info);
            socketConnection.onNewStatus(info);
            return;
        }
        if (info.message().hasCategory(Message.Category.SERVER)) {
            if (info.message().content() instanceof ProtocolMessage protocolMessage) {
                handleProtocolMessage(info, protocolMessage);
            }

            return;
        }

        var chat = info.chat()
                .orElseGet(() -> socketConnection.store().addNewChat(info.chatJid()));
        var result = chat.addNewMessage(info);
        if (!result || info.timestampSeconds().orElse(0L) <= socketConnection.store().initializationTimeStamp()) {
            return;
        }
        if (chat.archived() && socketConnection.store().unarchiveChats()) {
            chat.setArchived(false);
        }
        info.sender()
                .filter(this::isTyping)
                .ifPresent(sender -> socketConnection.onUpdateChatPresence(ContactStatus.AVAILABLE, sender.jid(), chat));
        if (!info.ignore() && !info.fromMe()) {
            chat.setUnreadMessagesCount(chat.unreadMessagesCount() + 1);
        }

        if (notify) {
            socketConnection.onNewMessage(info);
        }
    }

    private void handleProtocolMessage(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        switch (protocolMessage.protocolType()) {
            case HISTORY_SYNC_NOTIFICATION -> onHistorySyncNotification(info, protocolMessage);
            case APP_STATE_SYNC_KEY_SHARE -> onAppStateSyncKeyShare(protocolMessage);
            case REVOKE -> onMessageRevoked(info, protocolMessage);
            case EPHEMERAL_SETTING -> onEphemeralSettings(info, protocolMessage);
            case null, default -> {
            }
        }
    }

    private void onEphemeralSettings(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        var chat = info.chat().orElse(null);
        var timestampSeconds = info.timestampSeconds().orElse(0L);
        if (chat != null) {
            chat.setEphemeralMessagesToggleTimeSeconds(timestampSeconds);
            chat.setEphemeralMessageDuration(ChatEphemeralTimer.of((int) protocolMessage.ephemeralExpirationSeconds()));
        }
        var setting = new EphemeralSettingsBuilder()
                .timestampSeconds((int) protocolMessage.ephemeralExpirationSeconds())
                .timestampSeconds(timestampSeconds)
                .build();
        socketConnection.onSetting(setting);
    }

    private void onMessageRevoked(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        var id = protocolMessage.key().orElseThrow().id();
        info.chat()
                .flatMap(chat -> socketConnection.store().findMessageById(chat, id))
                .ifPresent(message -> onMessageDeleted(info, message));
    }

    private void onAppStateSyncKeyShare(ProtocolMessage protocolMessage) {
        var data = protocolMessage.appStateSyncKeyShare()
                .orElseThrow(() -> new NoSuchElementException("Missing app state keys"));
        var self = socketConnection.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        socketConnection.keys()
                .addAppKeys(self, data.keys());
        socketConnection.pullInitialPatches();
    }

    private void onHistorySyncNotification(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        scheduleHistorySyncTimeout();
        try {
            var historySync = downloadHistorySync(protocolMessage);
            onHistoryNotification(historySync);
        } catch (Throwable throwable) {
            socketConnection.handleFailure(HISTORY_SYNC, throwable);
        } finally {
            socketConnection.sendReceipt(info.chatJid(), null, List.of(info.id()), "hist_sync");
        }
    }

    private boolean isTyping(Contact sender) {
        return sender.lastKnownPresence() == ContactStatus.COMPOSING
                || sender.lastKnownPresence() == ContactStatus.RECORDING;
    }

    private HistorySync downloadHistorySync(ProtocolMessage protocolMessage) {
        if (socketConnection.store().webHistorySetting().isZero() && historySyncTypes.containsAll(REQUIRED_HISTORY_SYNC_TYPES)) {
            return null;
        }

        protocolMessage.historySyncNotification()
                .ifPresent(historySyncNotification -> historySyncTypes.add(historySyncNotification.syncType()));
        return protocolMessage.historySyncNotification()
                .map(this::downloadHistorySyncNotification)
                .orElse(null);
    }

    private HistorySync downloadHistorySyncNotification(HistorySyncNotification notification) {
        var initialPayload = notification.initialHistBootstrapInlinePayload();
        if (initialPayload.isPresent()) {
            var inflater = new Inflater();
            try (var stream = new InflaterInputStream(Streams.newInputStream(initialPayload.get()), inflater, 8192)) {
                return HistorySyncSpec.decode(ProtobufInputStream.fromStream(stream));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        return Medias.download(notification, mediaStream -> {
            var inflater = new Inflater();
            try (var stream = new InflaterInputStream(mediaStream, inflater, 8192)) {
                return HistorySyncSpec.decode(ProtobufInputStream.fromStream(stream));
            } catch (Exception exception) {
                throw new RuntimeException("Cannot decode history sync", exception);
            }
        });
    }

    private void onHistoryNotification(HistorySync history) {
        if (history == null) {
            return;
        }

        handleHistorySync(history);
        if (history.progress() == null) {
            return;
        }

        var recent = history.syncType() == HistorySync.Type.RECENT;
        if (recent) {
            recentHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if (recentHistorySyncTracker.isDone()) {
                socketConnection.onHistorySyncProgress(history.progress(), true);
            }
        } else {
            fullHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if (fullHistorySyncTracker.isDone()) {
                socketConnection.onHistorySyncProgress(history.progress(), false);
            }
        }
    }

    private void onMessageDeleted(ChatMessageInfo info, ChatMessageInfo message) {
        info.chat().ifPresent(chat -> chat.removeMessage(message));
        message.setRevokeTimestampSeconds(Clock.nowSeconds());
        socketConnection.onMessageDeleted(message, true);
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
            socketConnection.store().addStatus(messageInfo);
        }
        socketConnection.onStatus();
    }

    private void handlePushNames(HistorySync history) {
        for (var pushName : history.pushNames()) {
            handNewPushName(pushName);
        }
        socketConnection.onContacts();
    }

    private void handNewPushName(PushName pushName) {
        var jid = Jid.of(pushName.id());
        var contact = socketConnection.store()
                .findContactByJid(jid)
                .orElseGet(() -> createNewContact(jid));
        pushName.name()
                .ifPresent(contact::setChosenName);
        var action = new ContactActionBuilder()
                .firstName(pushName.name().orElse(null))
                .build();
        var index = new MessageIndexInfoBuilder()
                .type("contact")
                .targetId(pushName.id())
                .fromMe(true)
                .build();
        socketConnection.onAction(action, index);
    }

    private Contact createNewContact(Jid jid) {
        var contact = socketConnection.store().addContact(jid);
        socketConnection.onNewContact(contact);
        return contact;
    }

    private void handleInitialBootstrap(HistorySync history) {
        if (!socketConnection.store().webHistorySetting().isZero()) {
            var jids = history.conversations()
                    .stream()
                    .map(Chat::jid)
                    .toList();
            historyCache.addAll(jids);
        }

        handleConversations(history);
        socketConnection.onChats();
    }

    private void handleChatsSync(HistorySync history, boolean recent) {
        if (socketConnection.store().webHistorySetting().isZero()) {
            return;
        }

        handleConversations(history);
        handleConversationsNotifications(history, recent);
        scheduleHistorySyncTimeout();
    }

    private void handleConversationsNotifications(HistorySync history, boolean recent) {
        var toRemove = new HashSet<Jid>();
        for (var cachedJid : historyCache) {
            var chat = socketConnection.store()
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

            socketConnection.onChatRecentMessages(chat, done);
        }

        historyCache.removeAll(toRemove);
    }

    private void scheduleHistorySyncTimeout() {
        if (historySyncTask != null && !historySyncTask.isDone()) {
            historySyncTask.cancel(true);
        }

        this.historySyncTask = socketConnection.scheduleDelayed(this::onForcedHistorySyncCompletion, HISTORY_SYNC_MAX_TIMEOUT);
    }

    private void onForcedHistorySyncCompletion() {
        for (var cachedJid : historyCache) {
            var chat = socketConnection.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if (chat == null) {
                continue;
            }

            socketConnection.onChatRecentMessages(chat, true);
        }

        historyCache.clear();
    }

    private void handleConversations(HistorySync history) {
        for (var chat : history.conversations()) {
            for (var message : chat.messages()) {
                attributeChatMessage(message.messageInfo());
            }

            socketConnection.store().addChat(chat);
        }
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            socketConnection.onPastParticipants(pastParticipants.groupJid(), pastParticipants.pastParticipants());
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
        if (senderJid.server() != JidServer.user() && senderJid.server() != JidServer.legacyUser()) {
            return;
        }

        var contact = socketConnection.store().findContactByJid(senderJid)
                .orElseGet(() -> socketConnection.store().addContact(senderJid));
        info.setSender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageChatJid().ifPresent(chatJid -> attributeContextChat(contextInfo, chatJid));
    }

    private void attributeContextChat(ContextInfo contextInfo, Jid chatJid) {
        var chat = socketConnection.store().findChatByJid(chatJid)
                .orElseGet(() -> socketConnection.store().addNewChat(chatJid));
        contextInfo.setQuotedMessageChat(chat);
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        var contact = socketConnection.store().findContactByJid(senderJid)
                .orElseGet(() -> socketConnection.store().addContact(senderJid));
        contextInfo.setQuotedMessageSender(contact);
    }

    private void attributeChatMessage(ChatMessageInfo info) {
        var chat = socketConnection.store().findChatByJid(info.chatJid())
                .orElseGet(() -> socketConnection.store().addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = socketConnection.store().jid().orElse(null);
        if (info.fromMe() && me != null) {
            info.key().setSenderJid(me.withoutData());
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
            default -> {
            }
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
        try {
            var originalPollInfo = socketConnection.store().findMessageByKey(pollUpdateMessage.pollCreationMessageKey());
            if (originalPollInfo.isEmpty()) {
                return;
            }

            if (!(originalPollInfo.get().message().content() instanceof PollCreationMessage originalPollMessage)) {
                return;
            }

            pollUpdateMessage.setPollCreationMessage(originalPollMessage);
            var originalPollSenderJid = originalPollInfo.get()
                    .senderJid()
                    .withoutData();
            var modificationSenderJid = info.senderJid().withoutData();
            pollUpdateMessage.setVoter(modificationSenderJid);
            var originalPollId = originalPollInfo.get().id();
            var useSecretPayload = originalPollId + originalPollSenderJid + modificationSenderJid + pollUpdateMessage.secretName();
            var encryptionKey = originalPollMessage.encryptionKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
            var useCaseSecret = Hkdf.extractAndExpand(encryptionKey, useSecretPayload.getBytes(), 32);
            var additionalData = "%s\0%s".formatted(
                    originalPollId,
                    modificationSenderJid
            );
            var metadata = pollUpdateMessage.encryptedMetadata()
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted metadata"));
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(useCaseSecret, "AES"),
                    new GCMParameterSpec(128, metadata.iv())
            );
            cipher.updateAAD(additionalData.getBytes(StandardCharsets.UTF_8));
            var decrypted = cipher.doFinal(metadata.payload());
            var pollVoteMessage = PollUpdateEncryptedOptionsSpec.decode(decrypted);
            var selectedOptions = pollVoteMessage.selectedOptions()
                    .stream()
                    .map(sha256 -> originalPollMessage.getSelectableOption(HexFormat.of().formatHex(sha256)))
                    .flatMap(Optional::stream)
                    .toList();
            originalPollMessage.addSelectedOptions(modificationSenderJid, selectedOptions);
            pollUpdateMessage.setVotes(selectedOptions);
            var update = new PollUpdateBuilder()
                    .pollUpdateMessageKey(info.key())
                    .vote(pollVoteMessage)
                    .senderTimestampMilliseconds(Clock.nowMilliseconds())
                    .build();
            info.pollUpdates()
                    .add(update);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt poll update", exception);
        }
    }

    private void handleReactionMessage(ChatMessageInfo info, ReactionMessage reactionMessage) {
        info.setIgnore(true);
        socketConnection.store().findMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    public void dispose() {
        historyCache.clear();
        if (historySyncTask != null) {
            historySyncTask.cancel(true);
            historySyncTask = null;
        }
        recentHistorySyncTracker.clear();
        fullHistorySyncTracker.clear();
        historySyncTypes.clear();
    }

    public Node createCall(JidProvider jid) {
        var call = new CallMessageBuilder()
                .key(SignalKeyPair.random().publicKey())
                .build();
        var message = MessageContainer.of(call);
        var encodedMessage = messageToBytes(message);
        var cipheredMessage = sessionCipher.encrypt(jid.toJid().toSignalAddress(), encodedMessage);
        return Node.of("enc", Map.of("v", 2, "type", cipheredMessage.type()), cipheredMessage.message());
    }

    private byte[] messageToBytes(MessageContainer container) {
        try {
            if (container.isEmpty()) {
                return null;
            }

            var messageLength = MessageContainerSpec.sizeOf(container);
            var padByte = (byte) SecureRandom.getInstanceStrong().nextInt();
            var padLength = 1 + (15 & padByte);
            var result = new byte[messageLength + padLength];
            MessageContainerSpec.encode(container, ProtobufOutputStream.toBytes(result, 0));
            Arrays.fill(result, messageLength, messageLength + padLength, (byte) padLength);
            return result;
        }catch (Throwable exception) {
            throw new RuntimeException("Cannot encode message", exception);
        }
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
            if (finished) {
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