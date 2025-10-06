package com.github.auties00.cobalt.socket.message;

import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import it.auties.protobuf.stream.ProtobufOutputStream;
import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.auth.SignedDeviceIdentitySpec;
import com.github.auties00.cobalt.model.button.template.highlyStructured.HighlyStructuredFourRowTemplate;
import com.github.auties00.cobalt.model.button.template.hydrated.HydratedFourRowTemplate;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.media.AttachmentType;
import com.github.auties00.cobalt.model.media.MediaFile;
import com.github.auties00.cobalt.model.media.MutableAttachmentProvider;
import com.github.auties00.cobalt.model.message.button.*;
import com.github.auties00.cobalt.model.message.model.*;
import com.github.auties00.cobalt.model.message.server.DeviceSentMessageBuilder;
import com.github.auties00.cobalt.model.message.server.ProtocolMessage;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessageBuilder;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.model.poll.*;
import com.github.auties00.cobalt.util.*;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.MESSAGE;

public final class MessageSerializerHandler extends MessageHandler {
    private final SignalSessionCipher sessionCipher;
    private final SignalGroupCipher groupCipher;

    public MessageSerializerHandler(Whatsapp whatsapp) {
        super(whatsapp);
        this.sessionCipher = new SignalSessionCipher(whatsapp.keys());
        this.groupCipher = new SignalGroupCipher(whatsapp.keys());
    }

    public void encode(MessageRequest request) {
        switch (request) {
            case MessageRequest.Chat chatRequest -> encodeChatMessage(chatRequest);
            case MessageRequest.Newsletter newsletterRequest -> encodeNewsletterMessage(newsletterRequest);
        }
    }

    private void encodeChatMessage(MessageRequest.Chat request) {
        prepareOutgoingChatMessage(request.info());
        Node node;
        synchronized (this) {
            node = request.peer() || isConversation(request.info())
                    ? encodeConversation(request)
                    : encodeGroup(request);
        }
        whatsapp.sendNode(node);
        attributeMessageReceipt(request.info());
        saveMessage(request.info(), false);
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
        whatsapp.messagePreviewHandler()
                .attribute(textMessage);
    }

    private void attributeMediaMessage(Jid chatJid, MediaMessage mediaMessage) {
        var media = mediaMessage.decodedMedia()
                .orElseThrow(() -> new IllegalArgumentException("Missing media to upload"));
        var attachmentType = getAttachmentType(chatJid, mediaMessage);
        var mediaConnection = whatsapp.store().mediaConnection();
        var userAgent = whatsapp.store()
                .device()
                .toUserAgent(whatsapp.store().version())
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

            var me = whatsapp.store().jid();
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
            var originalPollInfo = whatsapp.store()
                    .findMessageByKey(pollUpdateMessage.pollCreationMessageKey())
                    .orElseThrow(() -> new NoSuchElementException("Missing original poll message"));
            var originalPollMessage = (PollCreationMessage) originalPollInfo.message().content();
            var modificationSenderJid = info.senderJid().withoutData();
            pollUpdateMessage.setVoter(modificationSenderJid);
            var originalPollSenderJid = originalPollInfo.senderJid().withoutData();
            var useSecretPayload = pollCreationId + originalPollSenderJid + modificationSenderJid + pollUpdateMessage.secretName();
            var encryptionKey = originalPollMessage.encryptionKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(encryptionKey, "AES"))
                    .thenExpand(useSecretPayload.getBytes(), 32);
            var useCaseSecret = hkdf.deriveData(params);
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
            default -> {}
        }
    }

    private void encodeNewsletterMessage(MessageRequest.Newsletter request) {
        try {
            prepareOutgoingChatMessage(request.info());
            var message = request.info().message();
            var messageNode = getPlainMessageNode(message);
            var type = request.info().message().content() instanceof MediaMessage ? "media" : "text";
            var attributes = NodeAttributes.ofNullable(request.additionalAttributes())
                    .put("id", request.info().id())
                    .put("to", request.info().parentJid())
                    .put("type", type)
                    .put("media_id", getPlainMessageHandle(request), Objects::nonNull)
                    .toMap();
            whatsapp.sendNode(Node.of("message", attributes, messageNode));
            var newsletter = request.info().newsletter();
            newsletter.addMessage(request.info());
        } catch (Throwable throwable) {
            request.info().setStatus(MessageStatus.ERROR);
            whatsapp.handleFailure(MESSAGE, throwable);
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
                var messageAttributes = NodeAttributes.of()
                        .put("mediatype", getMediaType(message.content()), Objects::nonNull)
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
        var sender = whatsapp.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var senderName = new SignalSenderKeyName(request.info().chatJid().toString(), sender.toSignalAddress());
        var signalMessage = groupCipher.create(senderName);
        var groupMessage = groupCipher.encrypt(senderName, encodedMessage);
        var messageNode = createMessageNode(groupMessage);
        if (request.hasRecipientOverride()) {
            var allDevices = queryDevices(request.recipients(), false);
            var preKeys = createGroupNodes(request, signalMessage, allDevices, request.force());
            var encodedMessageNode = createEncodedMessageNode(request, preKeys, messageNode);
            return whatsapp.sendNode(encodedMessageNode);
        }

        if (Jid.statusBroadcastAccount().equals(request.info().chatJid())) {
            var recipients = whatsapp.store()
                    .contacts()
                    .stream()
                    .map(Contact::jid)
                    .toList();
            var allDevices = queryDevices(recipients, false);
            var preKeys = createGroupNodes(request, signalMessage, allDevices, true);
            var encodedMessageNode = createEncodedMessageNode(request, preKeys, messageNode);
            return whatsapp.sendNode(encodedMessageNode);
        }

        var groupMetadata = whatsapp.queryGroupOrCommunityMetadata(request.info().chatJid());
        var allDevices = getGroupDevices(groupMetadata);
        var preKeys = createGroupNodes(request, signalMessage, allDevices, request.force());
        return createEncodedMessageNode(request, preKeys, messageNode);
    }

    private byte[] messageToBytes(MessageContainer container) {
        try {
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

    private Node encodeConversation(MessageRequest.Chat request) {
        var sender = whatsapp.store()
                .jid()
                .orElse(null);
        if (sender == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var encodedMessage = messageToBytes(request.info().message());
        if (request.peer()) {
            var chatJid = request.info().chatJid();
            var peerNode = createMessageNode(chatJid, encodedMessage, true);
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

        return new HashSet<>(List.of(whatsapp.store().jid().orElseThrow().withoutData(), request.info().chatJid()));
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
            whatsapp.keys().setCompanionIdentity()
                    .ifPresent(companionIdentity -> body.add(Node.of("device-identity", SignedDeviceIdentitySpec.encode(companionIdentity))));
        }

        var attributes = NodeAttributes.ofNullable(request.additionalAttributes())
                .put("id", request.info().id())
                .put("to", request.info().chatJid())
                .put("type", request.info().message().content() instanceof MediaMessage ? "media" : "text")
                .put("verified_name", whatsapp.store().verifiedName().orElse(""), whatsapp.store().verifiedName().isPresent() && !request.peer())
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
        var jid = whatsapp.store()
                .jid()
                .orElse(null);
        if (jid == null) {
            throw new IllegalStateException("Cannot create message: user is not signed in");
        }

        var partitioned = contacts.stream()
                .collect(Collectors.partitioningBy(contact -> Objects.equals(contact.user(), jid.user())));
        querySessions(partitioned.get(true), request.force());
        var companions = createMessageNodes(partitioned.get(true), deviceMessage);
        querySessions(partitioned.get(false), request.force());
        var others = createMessageNodes(partitioned.get(false), message);
        return toSingleList(companions, others);
    }

    private List<Node> createGroupNodes(MessageRequest.Chat request, byte[] distributionMessage, List<Jid> participants, boolean force) {
        var missingParticipants = participants.stream()
                .filter(participant -> force || !whatsapp.keys().hasGroupKeys(request.info().chatJid(), participant))
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
        var results = createMessageNodes(missingParticipants, paddedMessage);
        whatsapp.keys().addRecipientsWithPreKeys(request.info().chatJid(), missingParticipants);
        return results;
    }

    private List<Node> createMessageNodes(List<Jid> contacts, byte[] message) {
        return contacts.stream()
                .map(contact -> createMessageNode(contact, message, false))
                .toList();
    }

    private Node createMessageNode(Jid contact, byte[] message, boolean peer) {
        var encrypted = sessionCipher.encrypt(contact.toSignalAddress(), message);
        var messageNode = createMessageNode(encrypted);
        return peer ? messageNode : Node.of("to", Map.of("value", contact), messageNode);
    }

    @Override
    public void dispose() {

    }
}