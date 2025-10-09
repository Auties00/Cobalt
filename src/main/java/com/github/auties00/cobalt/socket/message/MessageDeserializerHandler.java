package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.model.action.ContactActionBuilder;
import com.github.auties00.cobalt.model.business.BusinessVerifiedNameCertificateSpec;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.info.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.*;
import com.github.auties00.cobalt.model.message.server.ProtocolMessage;
import com.github.auties00.cobalt.model.message.server.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.model.setting.EphemeralSettingsBuilder;
import com.github.auties00.cobalt.model.sync.*;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalMessage;
import com.github.auties00.libsignal.protocol.SignalPreKeyMessage;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;
import it.auties.protobuf.stream.ProtobufInputStream;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.HISTORY_SYNC;
import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.MESSAGE;

public final class MessageDeserializerHandler extends MessageHandler {
    private static final int HISTORY_SYNC_MAX_TIMEOUT = 25;
    private static final Set<HistorySync.Type> REQUIRED_HISTORY_SYNC_TYPES = Set.of(HistorySync.Type.INITIAL_BOOTSTRAP, HistorySync.Type.PUSH_NAME, HistorySync.Type.NON_BLOCKING_DATA);

    private final Set<Jid> historyCache;
    private final HistorySyncProgressTracker recentHistorySyncTracker;
    private final HistorySyncProgressTracker fullHistorySyncTracker;
    private final Set<HistorySync.Type> historySyncTypes;
    private final SignalSessionCipher sessionCipher;
    private final SignalGroupCipher groupCipher;
    private ScheduledFuture<?> historySyncTask;

    public MessageDeserializerHandler(Whatsapp whatsapp) {
        super(whatsapp);
        this.historyCache = ConcurrentHashMap.newKeySet();
        this.historySyncTypes = ConcurrentHashMap.newKeySet();
        this.recentHistorySyncTracker = new HistorySyncProgressTracker();
        this.fullHistorySyncTracker = new HistorySyncProgressTracker();
        this.sessionCipher = new SignalSessionCipher(whatsapp.keys());
        this.groupCipher = new SignalGroupCipher(whatsapp.keys());
    }

    public void decode(Node node, boolean notify) {
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
                decodeNewsletterMessage(node, plainText.get(), notify);
                return;
            }

            var reaction = node.findChild("reaction");
            if (reaction.isPresent()) {
                decodeNewsletterReaction(node, reaction.get(), notify);
                return;
            }

            decodeChatMessage(node, null, businessName, notify);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(MESSAGE, throwable);
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
                .flatMap(Node::toContentBytes)
                .map(BusinessVerifiedNameCertificateSpec::decode)
                .flatMap(certificate -> certificate.details().name());
    }

    private void decodeNewsletterMessage(Node messageInfoNode, Node messageNode, boolean notify) {
        try {
            var newsletterJid = messageInfoNode.attributes()
                    .getOptionalJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var messageId = messageInfoNode.attributes()
                    .getRequiredString("id");
            if(notify) {
                whatsapp.sendAck(messageInfoNode);
                whatsapp.sendReceipt(messageId, newsletterJid, null, false);
            }

            var newsletter = whatsapp.store()
                    .findNewsletterByJid(newsletterJid);
            if (newsletter.isEmpty()) {
                return;
            }

            var serverId = messageInfoNode.attributes()
                    .getRequiredInt("server_id");
            var timestamp = messageInfoNode.attributes()
                    .getNullableLong("t");
            var views = messageInfoNode.findChild("views_count")
                    .map(value -> value.attributes().getNullableLong("count"))
                    .orElse(null);
            var reactions = messageInfoNode.findChild("reactions")
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
                    Thread.startVirtualThread(() -> listener.onNewMessage(result.get()));
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
            Thread.startVirtualThread(() -> listener.onMessageReply(messageInfo, quotedMessageInfo));
            Thread.startVirtualThread(() -> listener.onMessageReply(whatsapp, messageInfo, quotedMessageInfo));
        }
    }

    private void decodeNewsletterReaction(Node reactionInfoNode, Node reactionNode, boolean notify) {
        try {
            var messageId = reactionInfoNode.attributes()
                    .getRequiredString("id");
            var newsletterJid = reactionInfoNode.attributes()
                    .getOptionalJid("from")
                    .orElseThrow(() -> new NoSuchElementException("Missing from"));
            var isSender = reactionInfoNode.attributes()
                    .getBoolean("is_sender");
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

            var code = reactionNode.attributes()
                    .getOptionalString("code");
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
                        .getOptionalJid("recipient_pn")
                        .or(() -> infoNode.attributes().getOptionalJid("recipient"))
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
                        .getOptionalJid("participant_pn")
                        .or(() -> infoNode.attributes().getOptionalJid("participant"))
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
        groupCipher.process(groupName, signalDistributionMessage);
    }

    private MessageContainer decodeChatMessageContainer(ChatMessageKey messageKey, Node messageNode) {
        if (messageNode == null) {
            return MessageContainer.empty();
        }

        var type = messageNode.attributes().getRequiredString("type");
        var encodedMessage = messageNode.toContentBytes();
        if (encodedMessage.isEmpty()) {
            return MessageContainer.empty();
        }

        return decodeMessageBytes(messageKey, type, encodedMessage.get());
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
                            .orElseThrow(() -> new IllegalArgumentException("Missing sender value"))
                            .toSignalAddress();
                    var senderName = new SignalSenderKeyName(groupJid.toString(), signalAddress);
                    yield groupCipher.decrypt(senderName, encodedMessage);
                }
                default -> throw new IllegalArgumentException("Unsupported encodedPoint message type: %s".formatted(type));
            };
            var messageLength = result.length - result[result.length - 1];
            return MessageContainerSpec.decode(ProtobufInputStream.fromBytes(result, 0, messageLength))
                    .unbox();
        } catch (Throwable throwable) {
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
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppStateSetting(setting));
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
        var self = whatsapp.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"));
        whatsapp.keys()
                .addWebAppStateKeys(self, data.keys());
        whatsapp.pullWebAppStatePatches(true, PatchType.values());
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
        if (whatsapp.store().webHistorySetting().isZero() && historySyncTypes.containsAll(REQUIRED_HISTORY_SYNC_TYPES)) {
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
                for (var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(history.progress(), true));
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(whatsapp, history.progress(), true));
                }
            }
        } else {
            fullHistorySyncTracker.commit(history.chunkOrder(), history.progress() == 100);
            if (fullHistorySyncTracker.isDone()) {
                for (var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(history.progress(), false));
                    Thread.startVirtualThread(() -> listener.onWebHistorySyncProgress(whatsapp, history.progress(), false));
                }
            }
        }
    }

    private void onMessageDeleted(ChatMessageInfo info, ChatMessageInfo message) {
        info.chat().ifPresent(chat -> chat.removeMessage(message));
        message.setRevokeTimestampSeconds(Clock.nowSeconds());
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageDeleted(message, true));
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
            Thread.startVirtualThread(() -> listener.onStatus(status));
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
            Thread.startVirtualThread(() -> listener.onContacts(contacts));
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
            Thread.startVirtualThread(() -> listener.onWebAppStateAction(action, index));
            Thread.startVirtualThread(() -> listener.onWebAppStateAction(whatsapp, action, index));
        }
    }

    private Contact createNewContact(Jid jid) {
        var contact = whatsapp.store().addContact(jid);
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewContact(contact));
            Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, contact));
        }
        return contact;
    }

    private void handleInitialBootstrap(HistorySync history) {
        if (!whatsapp.store().webHistorySetting().isZero()) {
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
            Thread.startVirtualThread(() -> listener.onChats(chats));
            Thread.startVirtualThread(() -> listener.onChats(whatsapp, chats));
        }
    }

    private void handleChatsSync(HistorySync history, boolean recent) {
        if (whatsapp.store().webHistorySetting().isZero()) {
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
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(chat, done));
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(whatsapp, chat, done));
            }
        }

        historyCache.removeAll(toRemove);
    }

    private void scheduleHistorySyncTimeout() {
        if (historySyncTask != null && !historySyncTask.isDone()) {
            historySyncTask.cancel(true);
        }

        this.historySyncTask = whatsapp.scheduleDelayed(this::onForcedHistorySyncCompletion, HISTORY_SYNC_MAX_TIMEOUT);
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
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(chat, true));
                Thread.startVirtualThread(() -> listener.onWebHistorySyncMessages(whatsapp, chat, true));
            }
        }

        historyCache.clear();
    }

    private void handleConversations(HistorySync history) {
        for (var chat : history.conversations()) {
            for (var message : chat.messages()) {
                attributeChatMessage(message.messageInfo());
            }

            whatsapp.store().addChat(chat);
        }
    }

    private void handleNonBlockingData(HistorySync history) {
        for (var pastParticipants : history.pastParticipants()) {
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onWebHistorySyncPastParticipants(pastParticipants.groupJid(), pastParticipants.pastParticipants()));
                Thread.startVirtualThread(() -> listener.onWebHistorySyncPastParticipants(whatsapp, pastParticipants.groupJid(), pastParticipants.pastParticipants()));
            }
        }
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