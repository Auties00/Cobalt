package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.chat.*;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.media.MediaVisibility;
import com.github.auties00.cobalt.wire.linked.message.PrivacySystemMessage;
import com.github.auties00.cobalt.wire.linked.setting.WallpaperSettings;
import it.auties.protobuf.annotation.ProtobufMessage;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * The metadata-only {@link Chat} subtype used as the value type of {@link PersistentLinkedWhatsAppChatStore#chats}.
 *
 * @apiNote
 * Cobalt embedders never construct this directly; {@link PersistentLinkedWhatsAppChatStore#addNewChat(Jid)} returns
 * one as a {@link Chat} and the protobuf builder produces one on deserialisation. Every message
 * accessor delegates to the owning store's {@link PersistentMessageStore} so that chat bodies stay
 * out of the protobuf snapshot and live in the MVStore instead.
 *
 * @implNote
 * This implementation carries no in-memory message field. After construction or deserialisation
 * the owning {@link PersistentStore} calls {@link #attach(PersistentMessageStore)} to wire the MVStore
 * facade and seed {@link #messageCount} from a one-time range walk so subsequent
 * {@link #messageCount()} calls answer in {@code O(1)}.
 */
@ProtobufMessage
final class PersistentChat extends Chat {
    /**
     * The logger for {@link PersistentChat}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentChat.class);

    /**
     * The MVStore facade backing every message accessor.
     *
     * @implNote
     * This implementation is wired by {@link #attach(PersistentMessageStore)} after construction
     * and is never serialised; protobuf encoding ignores the field entirely.
     */
    private volatile PersistentMessageStore messageStore;

    /**
     * The cached message count for this chat.
     *
     * @implNote
     * This implementation is seeded once by {@link #attach(PersistentMessageStore)} and maintained
     * incrementally by {@link #addMessage(ChatMessageInfo)} and {@link #removeMessage(String)} so
     * {@link #messageCount()} never re-walks the MVStore range.
     */
    private final AtomicInteger messageCount;

    /**
     * Constructs a metadata-only chat with the given protobuf-decoded fields.
     *
     * @apiNote
     * This constructor is package-private and intended for the generated {@code PersistentChatBuilder}
     * and the protobuf deserialiser. Callers obtain instances via
     * {@link PersistentLinkedWhatsAppChatStore#addNewChat(Jid)}.
     *
     * @implNote
     * This implementation initialises {@link #messageCount} to zero. The owning
     * {@link PersistentStore} subsequently calls {@link #attach(PersistentMessageStore)} to wire the
     * MVStore facade and reseed the count.
     *
     * @param jid                            the chat JID
     * @param newJid                         the migrated JID if this chat has been relocated, else {@code null}
     * @param oldJid                         the previous JID before migration, else {@code null}
     * @param lastMsgTimestamp               the timestamp of the most recent message
     * @param unreadCount                    the number of unread messages
     * @param readOnly                       whether the chat is read-only
     * @param endOfHistoryTransfer           whether the history transfer has finished
     * @param ephemeralExpiration            the ephemeral message timer
     * @param ephemeralSettingTimestamp      the timestamp at which the ephemeral timer was last changed
     * @param endOfHistoryTransferType       the variant of end-of-history transfer
     * @param conversationTimestamp          the conversation sort timestamp
     * @param name                           the display name
     * @param pHash                          the chat participant hash
     * @param notSpam                        whether the chat has been marked as not spam
     * @param archived                       whether the chat is archived
     * @param disappearingMode               the disappearing-message configuration
     * @param unreadMentionCount             the number of unread mentions
     * @param markedAsUnread                 whether the chat has been manually marked unread
     * @param participant                    the participants (for groups)
     * @param tcToken                        the trusted-contact token
     * @param tcTokenTimestamp               the timestamp of {@code tcToken}
     * @param contactPrimaryIdentityKey      the contact's primary identity key, if exposed
     * @param pinned                         the timestamp at which the chat was pinned, else {@code null}
     * @param mute                           the mute configuration
     * @param wallpaper                      the wallpaper settings
     * @param mediaVisibility                the media-visibility policy
     * @param tcTokenSenderTimestamp         the sender-side timestamp of {@code tcToken}
     * @param suspended                      whether the group has been suspended
     * @param terminated                     whether the group has been terminated
     * @param createdAt                      the chat creation timestamp
     * @param createdBy                      the creator JID, as a string
     * @param description                    the group description
     * @param support                        whether this is a WhatsApp support chat
     * @param isParentGroup                  whether this is a community parent group
     * @param parentGroupId                  the parent community id, if this is a subgroup
     * @param isDefaultSubgroup              whether this is the default announcement subgroup
     * @param displayName                    the alternative display name
     * @param phoneNumberJid                 the phone-number JID, when distinct from {@code jid}
     * @param shareOwnPhoneNumber            whether to share the own phone number
     * @param phoneNumberhDuplicateLidThread whether a duplicate LID thread exists for the phone number
     * @param lid                            the linked LID, when distinct from {@code jid}
     * @param username                       the username
     * @param lidOriginType                  the provenance of the {@code lid}
     * @param commentsCount                  the number of comments
     * @param locked                         whether the chat is locked
     * @param systemMessageToInsert          a pending privacy system-message to surface
     * @param capiCreatedGroup               whether the group was created via CAPI
     * @param accountLid                     the account LID
     * @param limitSharing                   whether sharing is limited
     * @param limitSharingSettingTimestamp   the timestamp of {@code limitSharing}
     * @param limitSharingTrigger            the trigger that enabled sharing limits
     * @param limitSharingInitiatedByMe      whether the local user initiated sharing limits
     * @param maibaAiThreadEnabled           whether the Meta AI thread is enabled
     */
    PersistentChat(Jid jid, Jid newJid, Jid oldJid, Instant lastMsgTimestamp, Integer unreadCount, Boolean readOnly, Boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralExpiration, Instant ephemeralSettingTimestamp, EndOfHistoryTransferType endOfHistoryTransferType, Instant conversationTimestamp, String name, String pHash, Boolean notSpam, Boolean archived, ChatDisappearingMode disappearingMode, Integer unreadMentionCount, Boolean markedAsUnread, List<GroupParticipant> participant, byte[] tcToken, Instant tcTokenTimestamp, byte[] contactPrimaryIdentityKey, Instant pinned, ChatMute mute, WallpaperSettings wallpaper, MediaVisibility mediaVisibility, Instant tcTokenSenderTimestamp, Boolean suspended, Boolean terminated, Instant createdAt, String createdBy, String description, Boolean support, Boolean isParentGroup, String parentGroupId, Boolean isDefaultSubgroup, String displayName, Jid phoneNumberJid, Boolean shareOwnPhoneNumber, Boolean phoneNumberhDuplicateLidThread, Jid lid, String username, String lidOriginType, Integer commentsCount, Boolean locked, PrivacySystemMessage systemMessageToInsert, Boolean capiCreatedGroup, Jid accountLid, Boolean limitSharing, Instant limitSharingSettingTimestamp, ChatLimitSharing.TriggerType limitSharingTrigger, Boolean limitSharingInitiatedByMe, Boolean maibaAiThreadEnabled) {
        super(jid, newJid, oldJid, lastMsgTimestamp, unreadCount, readOnly, endOfHistoryTransfer, ephemeralExpiration, ephemeralSettingTimestamp, endOfHistoryTransferType, conversationTimestamp, name, pHash, notSpam, archived, disappearingMode, unreadMentionCount, markedAsUnread, participant, tcToken, tcTokenTimestamp, contactPrimaryIdentityKey, pinned, mute, wallpaper, mediaVisibility, tcTokenSenderTimestamp, suspended, terminated, createdAt, createdBy, description, support, isParentGroup, parentGroupId, isDefaultSubgroup, displayName, phoneNumberJid, shareOwnPhoneNumber, phoneNumberhDuplicateLidThread, lid, username, lidOriginType, commentsCount, locked, systemMessageToInsert, capiCreatedGroup, accountLid, limitSharing, limitSharingSettingTimestamp, limitSharingTrigger, limitSharingInitiatedByMe, maibaAiThreadEnabled);
        this.messageCount = new AtomicInteger();
    }

    /**
     * Sets the given MVStore facade on this chat and reseeds the cached message count.
     *
     * @apiNote
     * Invoked by {@link PersistentStore#setMessageStore(PersistentMessageStore)} immediately
     * after construction or after the protobuf deserialiser produces a freshly decoded chat. Until
     * this call returns, every message accessor on this instance will throw because
     * {@link #messageStore} is still {@code null}.
     *
     * @implNote
     * This implementation walks {@link PersistentMessageStore#countChatMessages(Jid)} once so that
     * subsequent {@link #messageCount()} calls answer in {@code O(1)}.
     *
     * @param messageStore the MVStore facade owned by the parent store
     */
    void setMessageStore(PersistentMessageStore messageStore) {
        this.messageStore = messageStore;
        this.messageCount.set(messageStore.countChatMessages(jid()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat {0} message store set, cached count={1}", jid(), messageCount.get());
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote
     * The returned stream walks a consistent MVStore snapshot taken when it is created and holds no
     * native resources; callers continue to consume it inside a try-with-resources block for parity
     * with the rest of the message API.
     *
     * @implNote
     * This implementation delegates to {@link PersistentMessageStore#streamChatMessages(Jid)}.
     */
    @Override
    public Stream<ChatMessageInfo> messages() {
        return messageStore.streamChatMessages(jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the cached counter rather than re-walking MVStore.
     */
    @Override
    public int messageCount() {
        return messageCount.get();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation writes the encoded message into the {@code chat_messages} dbi under the
     * composite key {@code chatJid + 0x00 + msgId} and increments {@link #messageCount}.
     */
    @Override
    public void addMessage(ChatMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        messageStore.putChatMessage(jid(), info);
        messageCount.incrementAndGet();
        if (Log.TRACE) LOGGER.log(Level.TRACE, "message added to chat {0}", jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation deletes the MVStore key {@code chatJid + 0x00 + id} and decrements
     * {@link #messageCount} only when MVStore reports an entry was present.
     */
    @Override
    public boolean removeMessage(String id) {
        if (id == null) {
            return false;
        }
        var removed = messageStore.removeChatMessage(jid(), id);
        if (removed) {
            messageCount.decrementAndGet();
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "message {0} removed from chat {1}: {2}", id, jid(), removed);
        return removed;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation calls {@link PersistentMessageStore#removeChatMessages(Jid)} which deletes
     * every entry in the MVStore range {@code [chatJid + 0x00, chatJid + 0x01)} and resets the cached
     * counter to zero.
     */
    @Override
    public void removeMessages() {
        messageStore.removeChatMessages(jid());
        messageCount.set(0);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "all messages removed from chat {0}", jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} immediately on a {@code null} id rather
     * than letting the lookup hash a {@code null} message-id.
     */
    @Override
    public Optional<ChatMessageInfo> getMessageById(String id) {
        return id == null ? Optional.empty() : messageStore.getChatMessage(jid(), id);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the last entry in the MVStore chat range; with the store's
     * memcmp-ordered keys this is the message id that sorts highest, which for Cobalt's base64
     * message ids correlates with insertion order.
     */
    @Override
    public Optional<ChatMessageInfo> newestMessage() {
        return messageStore.newestChatMessage(jid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the first entry in the MVStore chat range.
     */
    @Override
    public Optional<ChatMessageInfo> oldestMessage() {
        return messageStore.oldestChatMessage(jid());
    }
}
