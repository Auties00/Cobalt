package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataEdit;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterPin;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed base of the {@link LinkedWhatsAppChatStore} sub-store.
 *
 * <p>This abstract nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore} owns the
 * strategy-independent conversation state: the favourite-chats list and mention-everyone mute
 * expirations (persisted), plus the transient in-flight call table, per-chat metadata, call history,
 * peer-message buffer, UTM read-tracking set and newsletter pins. The chat, newsletter, status and
 * message accessors are left abstract because each persistence strategy backs them differently:
 * {@code PersistentChatStore} routes message bodies to an MVStore facade while {@code TemporaryChatStore}
 * keeps everything in memory.
 *
 * @implNote
 * Phone-number/LID resolution used by {@link #findChatByJid(JidProvider)} (in the concrete subtypes)
 * reads the mapping table through a {@link LinkedWhatsAppContactStore} reference wired by the
 * {@link ProtobufWhatsAppStore} aggregate constructor via
 * {@link #setContacts(LinkedWhatsAppContactStore)}; the protobuf deserialiser builds each sub-store
 * from its own wire fields alone, so the sibling reference cannot be a constructor argument here.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class ProtobufLinkedWhatsAppChatStore implements LinkedWhatsAppChatStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppChatStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppChatStore.class);

    /**
     * The ordered list of pinned (favourite) chat JIDs.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private List<Jid> favoriteChats;

    /**
     * The per-group mute expirations for mention-everyone announcements, keyed by group JID.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.UINT64)
    private final ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap;

    /**
     * The currently tracked incoming calls keyed by call id; not persisted.
     */
    private final ConcurrentMap<String, IncomingCall> calls;

    /**
     * The per-chat metadata keyed by chat JID; not persisted.
     */
    private final ConcurrentMap<Jid, ChatMetadata> chatMetadata;

    /**
     * The call-history entries keyed by call id; not persisted.
     */
    private final ConcurrentMap<String, CallLog> callLogStates;

    /**
     * The internal peer-message buffer keyed by message id; not persisted.
     */
    private final ConcurrentMap<String, ChatMessageInfo> peerMessages;

    /**
     * The pending receipt-record recipients per sent message id; not persisted.
     */
    private final ConcurrentMap<String, Set<Jid>> pendingMessageRecipients;

    /**
     * The chat JIDs whose UTM tracking message has been read; not persisted.
     */
    private final Set<Jid> utmReadChatIds;

    /**
     * The newsletter pins keyed by newsletter JID; not persisted.
     */
    private final ConcurrentMap<Jid, NewsletterPin> newsletterPinStates;

    /**
     * The contact sub-store consulted for phone-number/LID resolution; wired by the
     * {@link ProtobufWhatsAppStore} aggregate constructor, so it is bound before the store is
     * reachable by any consumer.
     */
    protected LinkedWhatsAppContactStore contacts;

    /**
     * Constructs the shared chat sub-store state, defaulting the favourite-chats list and mute map.
     *
     * @param favoriteChats                     the favourite chat JIDs, or {@code null} for an empty list
     * @param mentionEveryoneMuteExpirationsMap the mention-everyone mute map, or {@code null} for an empty map
     */
    protected ProtobufLinkedWhatsAppChatStore(List<Jid> favoriteChats, ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap) {
        this.favoriteChats = requireNonNullElseGet(favoriteChats, ArrayList::new);
        this.mentionEveryoneMuteExpirationsMap = requireNonNullElseGet(mentionEveryoneMuteExpirationsMap, ConcurrentHashMap::new);
        this.calls = new ConcurrentHashMap<>();
        this.chatMetadata = new ConcurrentHashMap<>();
        this.callLogStates = new ConcurrentHashMap<>();
        this.peerMessages = new ConcurrentHashMap<>();
        this.pendingMessageRecipients = new ConcurrentHashMap<>();
        this.utmReadChatIds = ConcurrentHashMap.newKeySet();
        this.newsletterPinStates = new ConcurrentHashMap<>();
    }

    /**
     * Sets the contact sub-store used for phone-number/LID resolution.
     *
     * <p>The contact sub-store cannot be a constructor argument because the protobuf deserializer builds
     * this sub-store from its own wire fields alone; it is set once by the {@link ProtobufWhatsAppStore}
     * aggregate constructor, which composes both sub-stores.
     *
     * @param contacts the contact sub-store, never {@code null}
     */
    void setContacts(LinkedWhatsAppContactStore contacts) {
        this.contacts = Objects.requireNonNull(contacts, "contacts cannot be null");
    }

    /**
     * Returns the live mention-everyone mute map backing this store.
     *
     * @return the live mute map
     */
    protected ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap() {
        return mentionEveryoneMuteExpirationsMap;
    }

    @Override
    public List<Jid> favoriteChats() {
        return List.copyOf(favoriteChats);
    }

    @Override
    public LinkedWhatsAppChatStore setFavoriteChats(List<Jid> favoriteChats) {
        this.favoriteChats = new ArrayList<>(Objects.requireNonNull(favoriteChats, "favoriteChats cannot be null"));
        return this;
    }

    @Override
    public Optional<ChatMute> mentionEveryoneMuteExpiration(Jid chatJid) {
        Objects.requireNonNull(chatJid, "chatJid cannot be null");
        return Optional.ofNullable(mentionEveryoneMuteExpirationsMap.get(chatJid));
    }

    @Override
    public void setMentionEveryoneMuteExpiration(Jid chatJid, ChatMute mute) {
        Objects.requireNonNull(chatJid, "chatJid cannot be null");
        Objects.requireNonNull(mute, "mute cannot be null");
        mentionEveryoneMuteExpirationsMap.put(chatJid, mute);
    }

    @Override
    public Collection<IncomingCall> calls() {
        return List.copyOf(calls.values());
    }

    @Override
    public Optional<IncomingCall> findCallById(String callId) {
        return callId == null ? Optional.empty() : Optional.ofNullable(calls.get(callId));
    }

    @Override
    public IncomingCall addCall(IncomingCall call) {
        Objects.requireNonNull(call, "call cannot be null");
        calls.put(call.callId(), call);
        return call;
    }

    @Override
    public Optional<IncomingCall> removeCall(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(calls.remove(id));
    }

    @Override
    public Optional<ChatMetadata> findChatMetadata(Jid groupJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        return Optional.ofNullable(chatMetadata.get(groupJid));
    }

    @Override
    public void addChatMetadata(ChatMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata cannot be null");
        chatMetadata.put(metadata.jid(), metadata);
    }

    @Override
    public void removeChatMetadata(Jid groupJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        chatMetadata.remove(groupJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation only applies the local-only sync fields exposed by {@link GroupMetadataEdit};
     * the server-controlled fields round-trip through dedicated notifications. Returns
     * {@link Optional#empty()} when no group is cached for {@code groupJid}.
     */
    @Override
    public Optional<GroupMetadata> applyGroupMetadataEdit(Jid groupJid, GroupMetadataEdit edit) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(edit, "edit cannot be null");
        var existing = chatMetadata.get(groupJid);
        if (!(existing instanceof GroupMetadata group)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "group metadata edit dropped, no cached group for {0}", groupJid);
            return Optional.empty();
        }
        edit.statusMuted().ifPresent(group::setStatusMuted);
        return Optional.of(group);
    }

    @Override
    public Collection<NewsletterPin> newsletterPinStates() {
        return List.copyOf(newsletterPinStates.values());
    }

    @Override
    public Optional<NewsletterPin> findNewsletterPin(Jid newsletterJid) {
        return newsletterJid == null ? Optional.empty() : Optional.ofNullable(newsletterPinStates.get(newsletterJid));
    }

    @Override
    public LinkedWhatsAppChatStore putNewsletterPin(NewsletterPin pin) {
        Objects.requireNonNull(pin, "pin cannot be null");
        newsletterPinStates.put(pin.newsletterJid(), pin);
        return this;
    }

    @Override
    public Optional<NewsletterPin> removeNewsletterPin(Jid newsletterJid) {
        return newsletterJid == null ? Optional.empty() : Optional.ofNullable(newsletterPinStates.remove(newsletterJid));
    }

    @Override
    public LinkedWhatsAppChatStore clearNewsletterPins() {
        newsletterPinStates.clear();
        return this;
    }

    @Override
    public Collection<CallLog> callLogStates() {
        return List.copyOf(callLogStates.values());
    }

    @Override
    public Optional<CallLog> findCallLog(String callId) {
        return callId == null ? Optional.empty() : Optional.ofNullable(callLogStates.get(callId));
    }

    @Override
    public LinkedWhatsAppChatStore addCallLog(CallLog callLog) {
        Objects.requireNonNull(callLog, "callLog cannot be null");
        var callId = callLog.callId().orElseThrow(() -> new NullPointerException("callLog must have a callId"));
        callLogStates.put(callId, callLog);
        return this;
    }

    @Override
    public Optional<CallLog> removeCallLog(String callId) {
        return callId == null ? Optional.empty() : Optional.ofNullable(callLogStates.remove(callId));
    }

    @Override
    public LinkedWhatsAppChatStore clearCallLogs() {
        callLogStates.clear();
        return this;
    }

    @Override
    public void addPeerMessage(String id, ChatMessageInfo message) {
        peerMessages.put(id, message);
    }

    @Override
    public void removePeerMessage(String id) {
        peerMessages.remove(id);
    }

    @Override
    public Set<Jid> findReceiptRecords(String messageId) {
        if (messageId == null) {
            return Set.of();
        }
        var recipients = pendingMessageRecipients.get(messageId);
        return recipients != null ? Set.copyOf(recipients) : Set.of();
    }

    @Override
    public void createOrMergeReceiptRecords(String messageId, Collection<Jid> recipientJids) {
        if (messageId == null || recipientJids == null || recipientJids.isEmpty()) {
            return;
        }
        pendingMessageRecipients.compute(messageId, (k, existing) -> {
            var set = existing != null ? existing : ConcurrentHashMap.<Jid>newKeySet();
            set.addAll(recipientJids);
            return set;
        });
    }

    @Override
    public void removeReceiptRecords(String messageId) {
        pendingMessageRecipients.remove(messageId);
    }

    @Override
    public void markUtmReadForChat(Jid chatJid) {
        if (chatJid != null) {
            utmReadChatIds.add(chatJid);
        }
    }

    @Override
    public boolean hasReadUtmForChat(Jid chatJid) {
        return chatJid != null && utmReadChatIds.contains(chatJid);
    }

    @Override
    public void deleteUtmReadChatId(Jid chatJid) {
        if (chatJid != null) {
            utmReadChatIds.remove(chatJid);
        }
    }

    @Override
    public void clearUtmReadChatIds() {
        utmReadChatIds.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads the quoted-message id and parent JID from the enclosing message's
     * context info and resolves them through {@link #findMessageById(JidProvider, String)}; the
     * parent JID falls back to the enclosing message key's parent so quotes that omit the explicit
     * parent still resolve.
     */
    @Override
    public Optional<? extends LinkedMessageInfo> findQuotedMessage(LinkedMessageInfo info) {
        return info.message()
                .contextualContent()
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(context -> {
                    var quotedId = context.quotedMessageId().orElse(null);
                    var provider = context.quotedMessageParentJid()
                            .or(() -> info.key().parentJid())
                            .orElse(null);
                    if (quotedId == null || provider == null) {
                        return Optional.empty();
                    }
                    return findMessageById(provider, quotedId);
                });
    }
}
