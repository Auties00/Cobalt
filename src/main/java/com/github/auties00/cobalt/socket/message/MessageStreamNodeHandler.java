package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.info.DeviceContextInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.message.model.Message;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.model.message.standard.PollCreationMessage;
import com.github.auties00.cobalt.model.message.standard.PollUpdateMessage;
import com.github.auties00.cobalt.model.message.standard.ReactionMessage;
import com.github.auties00.cobalt.model.sync.HistorySync;
import com.github.auties00.cobalt.model.sync.HistorySyncNotification;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.model.sync.PushName;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.model.proto.action.ContactActionBuilder;
import com.github.auties00.cobalt.model.proto.business.BusinessVerifiedNameCertificateSpec;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.model.proto.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.proto.message.model.*;
import com.github.auties00.cobalt.model.message.server.ProtocolMessage;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.model.proto.poll.PollUpdateBuilder;
import com.github.auties00.cobalt.model.proto.poll.PollUpdateEncryptedOptionsSpec;
import com.github.auties00.cobalt.model.proto.setting.EphemeralSettingsBuilder;
import com.github.auties00.cobalt.model.proto.sync.*;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;
import it.auties.protobuf.stream.ProtobufInputStream;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.HISTORY_SYNC;
import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MESSAGE;

public final class MessageStreamNodeHandler extends SocketStream.Handler {
    private static final int HISTORY_SYNC_MAX_TIMEOUT = 25;
    private static final Set<HistorySync.Type> REQUIRED_HISTORY_SYNC_TYPES = Set.of(HistorySync.Type.INITIAL_BOOTSTRAP, HistorySync.Type.PUSH_NAME, HistorySync.Type.NON_BLOCKING_DATA);

    private final MessageDecoder messageDecoder;
    private final Set<Jid> historyCache;
    private final HistorySyncProgressTracker recentHistorySyncTracker;
    private final HistorySyncProgressTracker fullHistorySyncTracker;
    private final Set<HistorySync.Type> historySyncTypes;
    private CompletableFuture<Void> historySyncTask;

    public MessageStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "message");
        this.messageDecoder = new MessageDecoder(whatsapp.store());
        this.historyCache = new HashSet<>();
        this.historySyncTypes = new HashSet<>();
        this.recentHistorySyncTracker = new HistorySyncProgressTracker();
        this.fullHistorySyncTracker = new HistorySyncProgressTracker();
    }

    @Override
    public void handle(Node node) {
        try {
            var businessName = getBusinessNameFromAttribute(node);
            if (node.hasChild("unavailable")) {
                decodeChatMessage(node, null, businessName, true);
                return;
            }

            var encrypted = node.getChildren("enc");
            if (!encrypted.isEmpty()) {
                encrypted.forEach(message -> decodeChatMessage(node, message, businessName, true));
                return;
            }


            var plainText = node.getChild("plaintext");
            if (plainText.isPresent()) {
                decodeNewsletterMessage(node, plainText.get(), true);
                return;
            }

            var reaction = node.getChild("reaction");
            if (reaction.isPresent()) {
                decodeNewsletterReaction(node, reaction.get(), true);
                return;
            }

            decodeChatMessage(node, null, businessName, true);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
        }
    }

    private String getBusinessNameFromAttribute(Node node) {
        return node.getAttributeAsString("verified_name")
                .or(() -> getBusinessNameFromContent(node))
                .orElse(null);
    }

    private static Optional<String> getBusinessNameFromContent(Node node) {
        return node.getChild("verified_name")
                .flatMap(Node::toContentBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode)
                .flatMap(certificate -> certificate.details().name());
    }

    private void decodeNewsletterMessage(Node messageInfoNode, Node messageNode, boolean notify) {
        try {
            var newsletterJid = messageInfoNode
                    .getAttributeAsJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var messageId = messageInfoNode
                    .getRequiredAttributeAsString("id");
            if(notify) {
                whatsapp.sendAck(messageInfoNode);
                whatsapp.sendReceipt(messageId, newsletterJid, null, false);
            }

            var newsletter = whatsapp.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return;
            }

            var serverId = Math.toIntExact(messageInfoNode.getRequiredAttributeAsLong("server_id"));
            var timestamp = messageInfoNode.getAttributeAsLong("t", 0);
            var views = messageInfoNode.getChild("views_count")
                    .map(value -> value.getRequiredAttributeAsLong("count"))
                    .orElse(null);
            var reactions = messageInfoNode.streamChild("reactions")
                    .flatMap(node -> node.streamChildren("reaction"))
                    .collect(Collectors.toConcurrentMap(
                            entry -> entry.getRequiredAttributeAsString("code"),
                            entry -> new NewsletterReaction(
                                    entry.getRequiredAttributeAsString("code"),
                                    entry.getRequiredAttributeAsLong("count"),
                                    entry.getRequiredAttributeAsBool("is_sender")
                            )
                    ));
            var result = messageNode.toContentBytes()
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
                for (var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onNewMessage(whatsapp, result.get()));
                }
            }

            notifyReplies(result.get());
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
        }
    }

    private void notifyReplies(MessageInfo messageInfo) {
        if(messageInfo == null) {
            return;
        }

        var quotedMessageInfo = messageInfo.quotedMessage()
                .orElse(null);
        if(quotedMessageInfo == null) {
            return;
        }

        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageReply(whatsapp, messageInfo, quotedMessageInfo));
        }
    }

    private void decodeNewsletterReaction(Node reactionInfoNode, Node reactionNode, boolean notify) {
        try {
            var messageId = reactionInfoNode
                    .getRequiredAttributeAsString("id");
            var newsletterJid = reactionInfoNode
                    .getAttributeAsJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var isSender = reactionInfoNode
                    .getAttributeAsBool("is_sender", false);
            if(notify) {
                whatsapp.sendAck(reactionInfoNode);
                whatsapp.sendReceipt(messageId, newsletterJid, null, false);
            }

            var newsletter = whatsapp.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return;
            }

            var message = whatsapp.store()
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

            var code = reactionNode
                    .getAttributeAsString("code");
            if (code.isEmpty()) {
                return;
            }

            message.get().incrementReaction(code.get(), isSender);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
        }
    }

    private void decodeChatMessage(Node infoNode, Node messageNode, String businessName, boolean notify) {
        ChatMessageKey chatMessageKey = null;
        try {
            var selfJid = whatsapp.store()
                    .jid()
                    .orElse(null);
            if (selfJid == null) {
                return;
            }

            var pushName = infoNode.getAttributeAsString("notify", null);
            var timestamp = infoNode.getAttributeAsLong("t", 0);
            var id = infoNode.getRequiredAttributeAsString("id");
            var from = infoNode.getRequiredAttributeAsJid("from");
            var messageBuilder = new ChatMessageInfoBuilder()
                    .status(MessageStatus.PENDING);
            var keyBuilder = new ChatMessageKeyBuilder()
                    .id(id);
            if (from.hasServer(JidServer.user()) || from.hasServer(JidServer.legacyUser())) {
                var recipient = infoNode
                        .getAttributeAsJid("recipient_pn")
                        .or(() -> infoNode.getAttributeAsJid("recipient"))
                        .orElse(from);
                keyBuilder.chatJid(recipient);
                keyBuilder.senderJid(from);
                keyBuilder.fromMe(Objects.equals(from.withoutData(), selfJid.withoutData()));
                messageBuilder.senderJid(from);
            }else if(from.hasServer(JidServer.bot())) {
                var meta = infoNode.getChild("meta")
                        .orElseThrow();
                var chatJid = meta
                        .getRequiredAttributeAsJid("target_chat_jid");
                var senderJid = meta
                        .getAttributeAsJid("target_sender_jid")
                        .orElse(chatJid);
                keyBuilder.chatJid(chatJid);
                keyBuilder.senderJid(senderJid);
                keyBuilder.fromMe(Objects.equals(senderJid.withoutData(), selfJid.withoutData()));
            } else if(from.hasServer(JidServer.groupOrCommunity()) || from.hasServer(JidServer.broadcast()) || from.hasServer(JidServer.newsletter())) {
                var participant = infoNode
                        .getAttributeAsJid("participant_pn")
                        .or(() -> infoNode.getAttributeAsJid("participant"))
                        .orElseThrow(() -> new NoSuchElementException("Missing sender"));
                keyBuilder.chatJid(from);
                keyBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
                keyBuilder.fromMe(Objects.equals(participant.withoutData(), selfJid.withoutData()));
                messageBuilder.senderJid(Objects.requireNonNull(participant, "Missing participant in group message"));
            }else {
                throw new RuntimeException("Unknown value server: " + from.server());
            }
            chatMessageKey = keyBuilder.build();

            var senderJid = chatMessageKey.senderJid()
                    .orElseThrow(() -> new InternalError("Missing sender"));
            if (selfJid.equals(senderJid)) {
                return;
            }

            ChatMessageInfo info;
            synchronized (this) {
                var container = decodeChatMessageContainer(chatMessageKey, messageNode);
                info = messageBuilder.key(chatMessageKey)
                        .broadcast(chatMessageKey.chatJid().hasServer(JidServer.broadcast()))
                        .pushName(pushName)
                        .status(MessageStatus.DELIVERED)
                        .businessVerifiedName(businessName)
                        .timestampSeconds(timestamp)
                        .message(container)
                        .build();
                switch (container.content()) {
                    case SenderKeyDistributionMessage keyDistributionMessage ->
                            handleDistributionMessage(keyDistributionMessage, info);
                    case ProtocolMessage protocolMessage -> handleProtocolMessage(info, protocolMessage);
                    default -> {}
                }
            }

            attributeMessageReceipt(info);
            attributeChatMessage(info);
            saveMessage(info, notify);
            notifyReplies(info);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
        }finally {
            whatsapp.sendAck(infoNode);
            if(chatMessageKey != null) {
                whatsapp.sendReceipt(
                        chatMessageKey.id(),
                        chatMessageKey.chatJid(),
                        chatMessageKey.senderJid().orElse(null),
                        chatMessageKey.fromMe()
                );
            }
        }
    }

    private void handleDistributionMessage(SenderKeyDistributionMessage keyDistributionMessage, ChatMessageInfo info) {
        var groupName = new SignalSenderKeyName(keyDistributionMessage.groupJid().toString(), info.senderJid().toSignalAddress());
        var signalDistributionMessage = SignalSenderKeyDistributionMessage.ofSerialized(keyDistributionMessage.data());
        messageDecoder.process(groupName, signalDistributionMessage);
    }

    private MessageContainer decodeChatMessageContainer(ChatMessageKey messageKey, Node messageNode) {
        if (messageNode == null) {
            return MessageContainer.empty();
        }

        var type = messageNode.getRequiredAttributeAsString("type");
        var encodedMessage = messageNode.toContentBytes();
        if (encodedMessage.isEmpty()) {
            return MessageContainer.empty();
        }

        try {
            return messageDecoder.decode(messageKey, type, encodedMessage.get());
        }catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
            return MessageContainer.empty();
        }
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
            chat.setEphemeralMessageDuration(ChatEphemeralTimer.of((int) protocolMessage.ephemeralExpirationSeconds()));
        }
        var setting = new EphemeralSettingsBuilder()
                .timestampSeconds((int) protocolMessage.ephemeralExpirationSeconds())
                .timestampSeconds(timestampSeconds)
                .build();
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppStateSetting(whatsapp, setting));
        }
    }

    private void onMessageRevoked(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        var id = protocolMessage.key().orElseThrow().id();
        info.chat()
                .flatMap(chat -> whatsapp.store().findMessageById(chat, id))
                .ifPresent(message -> onMessageDeleted(info, message));
    }

    private void onAppStateSyncKeyShare(ProtocolMessage protocolMessage) {
        var data = protocolMessage.appStateSyncKeyShare()
                .orElseThrow(() -> new NoSuchElementException("Missing app state keys"));
        whatsapp.store()
                .addWebAppStateKeys(data.keys());
        whatsapp.pullWebAppState(PatchType.values());
    }

    private void onHistorySyncNotification(ChatMessageInfo info, ProtocolMessage protocolMessage) {
        scheduleHistorySyncTimeout();
        try {
            var historySync = downloadHistorySync(protocolMessage);
            onHistoryNotification(historySync);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(HISTORY_SYNC, throwable);
        } finally {
            whatsapp.sendReceipt(info.id(), info.chatJid(), "hist_sync");
        }
    }

    private HistorySync downloadHistorySync(ProtocolMessage protocolMessage) {
        if (historySyncTypes.containsAll(REQUIRED_HISTORY_SYNC_TYPES) &&
             (whatsapp.store().webHistoryPolicy().isEmpty() || whatsapp.store().webHistoryPolicy().get().isZero())) {
            return null;
        }

        protocolMessage.historySyncNotification()
                .ifPresent(historySyncNotification -> historySyncTypes.add(historySyncNotification.syncType()));
        return protocolMessage.historySyncNotification()
                .map(this::downloadHistorySyncNotification)
                .orElse(null);
    }

    private HistorySync downloadHistorySyncNotification(HistorySyncNotification notification) {
        try {
            var initialPayload = notification.initialHistBootstrapInlinePayload();
            if (initialPayload.isPresent()) {
                try(var mediaStream = ProtobufInputStream.fromStream(new InflaterByteBufferInputStream(initialPayload.get()))) {
                    return HistorySyncSpec.decode(mediaStream);
                }
            }else {
                var mediaConnection = whatsapp.store()
                        .mediaConnection()
                        .orElseThrow(() -> new InternalError("Missing media connection"));
                try(var mediaStream = ProtobufInputStream.fromStream(mediaConnection.download(notification))) {
                    return HistorySyncSpec.decode(mediaStream);
                }
            }
        } catch (Exception exception) {
            throw new MediaDownloadException("Cannot download history sync", exception);
        }
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
                for (var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(whatsapp, history.progress(), true));
                }
            }
        } else {
            fullHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if (fullHistorySyncTracker.isDone()) {
                for (var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(whatsapp, history.progress(), false));
                }
            }
        }
    }

    private void onMessageDeleted(ChatMessageInfo info, ChatMessageInfo message) {
        info.chat().ifPresent(chat -> chat.removeMessage(message.id()));
        message.setRevokeTimestampSeconds(Clock.nowSeconds());
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageDeleted(whatsapp, message, true));
        }
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
            whatsapp.store().addStatus(messageInfo);
        }
        whatsapp.store()
                .setSyncedStatus(true);
        var status = whatsapp.store()
                .status();
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onStatus(whatsapp, status));
        }
    }

    private void handlePushNames(HistorySync history) {
        for (var pushName : history.pushNames()) {
            handNewPushName(pushName);
        }
        whatsapp.store()
                .setSyncedContacts(true);
        var contacts = whatsapp.store()
                .contacts();
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onContacts(whatsapp, contacts));
        }
    }

    private void handNewPushName(PushName pushName) {
        var jid = Jid.of(pushName.id());
        var contact = whatsapp.store()
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
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppStateAction(whatsapp, action, index));
        }
    }

    private Contact createNewContact(Jid jid) {
        var contact = whatsapp.store().addNewContact(jid);
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, contact));
        }
        return contact;
    }

    private void handleInitialBootstrap(HistorySync history) {
        var historyPolicy = whatsapp.store().webHistoryPolicy();
        if (historyPolicy.isEmpty() || !historyPolicy.get().isZero()) {
            var jids = history.conversations()
                    .stream()
                    .map(Chat::jid)
                    .toList();
            historyCache.addAll(jids);
        }

        handleConversations(history);
        whatsapp.store()
                .setSyncedChats(true);
        var chats = whatsapp.store().chats();
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onChats(whatsapp, chats));
        }
    }

    private void handleChatsSync(HistorySync history, boolean recent) {
        var historyPolicy = whatsapp.store().webHistoryPolicy();
        if (historyPolicy.isPresent() && historyPolicy.get().isZero()) {
            return;
        }

        handleConversations(history);
        handleConversationsNotifications(history, recent);
        scheduleHistorySyncTimeout();
    }

    private void handleConversationsNotifications(HistorySync history, boolean recent) {
        var toRemove = new HashSet<Jid>();
        for (var cachedJid : historyCache) {
            var chat = whatsapp.store()
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

            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(whatsapp, chat, done));
            }
        }

        historyCache.removeAll(toRemove);
    }

    private void scheduleHistorySyncTimeout() {
        if (historySyncTask != null && !historySyncTask.isDone()) {
            historySyncTask.cancel(true);
        }

        this.historySyncTask =  CompletableFuture.runAsync(this::onForcedHistorySyncCompletion,
                CompletableFuture.delayedExecutor(HISTORY_SYNC_MAX_TIMEOUT, TimeUnit.SECONDS));
    }

    private void onForcedHistorySyncCompletion() {
        for (var cachedJid : historyCache) {
            var chat = whatsapp.store()
                    .findChatByJid(cachedJid)
                    .orElse(null);
            if (chat == null) {
                continue;
            }

            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(whatsapp, chat, true));
            }
        }

        historyCache.clear();
    }

    private void handleConversations(HistorySync history) {
        for (var chat : history.conversations()) {
            for (var message : chat.messages()) {
                attributeChatMessage(message);
            }

            whatsapp.store().addChat(chat);
        }
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onWebHistorySyncPastParticipants(whatsapp, pastParticipants.groupJid(), pastParticipants.pastParticipants()));
            }
        }
    }

    void attributeMessageReceipt(ChatMessageInfo info) {
        var self = whatsapp.store()
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

    void saveMessage(ChatMessageInfo info, boolean notify) {
        if (Jid.statusBroadcastAccount().equals(info.chatJid())) {
            whatsapp.store().addStatus(info);
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewStatus(whatsapp, info));
            }
            return;
        }

        if (info.message().hasCategory(Message.Category.SERVER)) {
            return;
        }

        var chat = info.chat()
                .orElseGet(() -> whatsapp.store().addNewChat(info.chatJid()));
        chat.addMessage(info);
        if (info.timestampSeconds().orElse(0L) <= whatsapp.store().initializationTimeStamp()) {
            return;
        }
        if (chat.archived() && whatsapp.store().unarchiveChats()) {
            chat.setArchived(false);
        }
        info.sender()
                .filter(this::isTyping)
                .ifPresent(sender -> {
                    var contact = whatsapp.store()
                            .findContactByJid(sender);
                    if (contact.isPresent()) {
                        contact.get().setLastKnownPresence(ContactStatus.AVAILABLE);
                        contact.get().setLastSeen(ZonedDateTime.now());
                    }

                    var provider = contact.orElse(sender);
                    chat.addPresence(sender, ContactStatus.AVAILABLE);
                    for (var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, info.chatJid(), provider.jid()));
                    }
                });
        if (!info.ignore() && !info.fromMe()) {
            chat.setUnreadMessagesCount(chat.unreadMessagesCount() + 1);
        }

        if (notify) {
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewMessage(whatsapp, info));
            }
        }
    }

    private boolean isTyping(Contact sender) {
        return sender.lastKnownPresence() == ContactStatus.COMPOSING
               || sender.lastKnownPresence() == ContactStatus.RECORDING;
    }

    void attributeChatMessage(ChatMessageInfo info) {
        var chat = whatsapp.store().findChatByJid(info.chatJid())
                .orElseGet(() -> whatsapp.store().addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = whatsapp.store().jid().orElse(null);
        if (info.fromMe() && me != null) {
            info.key().setSenderJid(me.withoutData());
        }

        attributeSender(info, info.senderJid());
        info.message()
                .contentWithContext()
                .ifPresent(message -> {
                    message.contextInfo()
                            .ifPresent(this::attributeContext);
                    processMessageWithSecret(info, message);
                });
    }

    private void processMessageWithSecret(ChatMessageInfo info, Message message) {
        switch (message) {
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
        try {
            var originalPollInfo = whatsapp.store()
                    .findChatMessageByKey(pollUpdateMessage.pollCreationMessageKey());
            if (originalPollInfo.isEmpty()) {
                return;
            }

            if(!(originalPollInfo.get().message().content() instanceof PollCreationMessage originalPollMessage)) {
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
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(encryptionKey, "AES"))
                    .thenExpand(useSecretPayload.getBytes(), 32);
            var useCaseSecret = hkdf.deriveData(params);
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
        whatsapp.store()
                .findChatMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    private void attributeSender(ChatMessageInfo info, Jid senderJid) {
        if (senderJid.server() != JidServer.user() && senderJid.server() != JidServer.legacyUser()) {
            return;
        }

        var contact = whatsapp.store().findContactByJid(senderJid)
                .orElseGet(() -> createNewContact(senderJid));
        info.setSender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageParentJid().ifPresent(parentJid -> attributeContextParent(contextInfo, parentJid));
    }

    private void attributeContextParent(ContextInfo contextInfo, Jid parentJid) {
        if(parentJid.hasServer(JidServer.newsletter())) {
            var newsletter = whatsapp.store()
                    .findNewsletterByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewNewsletter(parentJid));
            contextInfo.setQuotedMessageParent(newsletter);
        }else {
            var chat = whatsapp.store()
                    .findChatByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewChat(parentJid));
            contextInfo.setQuotedMessageParent(chat);
        }
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        var contact = whatsapp.store().findContactByJid(senderJid)
                .orElseGet(() -> createNewContact(senderJid));
        contextInfo.setQuotedMessageSender(contact);
    }

    @Override
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

    private static final class InflaterByteBufferInputStream extends InputStream {
        private static final int BUFFER_SIZE = 8192;

        private final ByteBuffer source;
        private final Inflater inflater;
        private final byte[] buffer;
        private boolean closed;
        private boolean reachedEOF;

        InflaterByteBufferInputStream(ByteBuffer source) {
            this(source, new Inflater());
        }

        InflaterByteBufferInputStream(ByteBuffer source, Inflater inflater) {
            this.source = source;
            this.inflater = inflater;
            this.buffer = new byte[BUFFER_SIZE];
            this.closed = false;
            this.reachedEOF = false;
        }

        @Override
        public int read() throws IOException {
            ensureOpen();

            if (reachedEOF) {
                return -1;
            }

            try {
                byte[] singleByte = new byte[1];

                while (true) {
                    if (inflater.finished()) {
                        reachedEOF = true;
                        return -1;
                    }

                    if (inflater.needsInput()) {
                        // Feed more compressed data to the inflater
                        int available = source.remaining();
                        if (available == 0) {
                            if (inflater.finished()) {
                                reachedEOF = true;
                                return -1;
                            }
                            // No more input and not finished - may be truncated data
                            throw new IOException("Unexpected end of compressed data");
                        }

                        // Read up to buffer size from the ByteBuffer
                        int toRead = Math.min(available, buffer.length);
                        source.get(buffer, 0, toRead);
                        inflater.setInput(buffer, 0, toRead);
                    }

                    // Decompress data
                    int bytesRead = inflater.inflate(singleByte, 0, 1);

                    if (bytesRead == 1) {
                        return singleByte[0] & 0xFF;
                    }

                    if (inflater.finished()) {
                        reachedEOF = true;
                        return -1;
                    }

                    if (inflater.needsDictionary()) {
                        throw new IOException("Inflater needs dictionary");
                    }
                }

            } catch (DataFormatException e) {
                throw new IOException("Invalid compressed data format", e);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureOpen();

            if (b == null) {
                throw new NullPointerException("Byte array is null");
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }

            if (reachedEOF) {
                return -1;
            }

            try {
                int bytesRead = 0;

                while (bytesRead == 0) {
                    if (inflater.finished()) {
                        reachedEOF = true;
                        return -1;
                    }

                    if (inflater.needsInput()) {
                        // Feed more compressed data to the inflater
                        int available = source.remaining();
                        if (available == 0) {
                            if (inflater.finished()) {
                                reachedEOF = true;
                                return -1;
                            }
                            // No more input and not finished - may be truncated data
                            throw new IOException("Unexpected end of compressed data");
                        }

                        // Read up to buffer size from the ByteBuffer
                        int toRead = Math.min(available, buffer.length);
                        source.get(buffer, 0, toRead);
                        inflater.setInput(buffer, 0, toRead);
                    }

                    // Decompress data
                    bytesRead = inflater.inflate(b, off, len);

                    if (bytesRead == 0) {
                        if (inflater.finished()) {
                            reachedEOF = true;
                            return -1;
                        }
                        if (inflater.needsDictionary()) {
                            throw new IOException("Inflater needs dictionary");
                        }
                    }
                }

                return bytesRead;

            } catch (DataFormatException e) {
                throw new IOException("Invalid compressed data format", e);
            }
        }

        @Override
        public long skip(long n) throws IOException {
            if (n < 0) {
                throw new IllegalArgumentException("Skip value is negative");
            }
            ensureOpen();

            long remaining = n;
            byte[] skipBuffer = new byte[(int) Math.min(BUFFER_SIZE, n)];

            while (remaining > 0) {
                int toRead = (int) Math.min(skipBuffer.length, remaining);
                int bytesRead = read(skipBuffer, 0, toRead);
                if (bytesRead == -1) {
                    break;
                }
                remaining -= bytesRead;
            }

            return n - remaining;
        }

        @Override
        public int available() throws IOException {
            ensureOpen();
            if (reachedEOF) {
                return 0;
            }
            return inflater.finished() ? 0 : 1;
        }

        @Override
        public void close() {
            if (!closed) {
                inflater.end();
                closed = true;
            }
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }
    }

    private static final class HistorySyncProgressTracker {
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
