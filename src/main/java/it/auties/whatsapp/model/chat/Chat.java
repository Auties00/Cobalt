package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidType;
import it.auties.whatsapp.model.media.MediaVisibility;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.ConcurrentLinkedSet;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A model class that represents a Chat. A chat can be of two types: a conversation with a contact
 * or a group. This class is only a model, this means that changing its values will have no real
 * effect on WhatsappWeb's servers
 */
@ProtobufMessage(name = "Conversation")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Chat implements JidProvider {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final ConcurrentLinkedSet<HistorySyncMessage> historySyncMessages;
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid newJid;
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid oldJid;
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    int unreadMessagesCount;
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean endOfHistoryTransfer;
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    ChatEphemeralTimer ephemeralMessageDuration;
    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    long ephemeralMessagesToggleTimeSeconds;
    @ProtobufProperty(index = 11, type = ProtobufType.ENUM)
    EndOfHistoryTransferType endOfHistoryTransferType;
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    long timestampSeconds;
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String name;
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    boolean notSpam;
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    boolean archived;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ChatDisappear disappearInitiator;
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    boolean markedAsUnread;
    @ProtobufProperty(index = 24, type = ProtobufType.UINT32)
    int pinnedTimestampSeconds;
    @ProtobufProperty(index = 25, type = ProtobufType.UINT64)
    ChatMute mute;
    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    ChatWallpaper wallpaper;
    @ProtobufProperty(index = 27, type = ProtobufType.ENUM)
    MediaVisibility mediaVisibility;
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean suspended;
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    boolean terminated;
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    boolean support;
    @ProtobufProperty(index = 38, type = ProtobufType.STRING)
    String displayName;
    @ProtobufProperty(index = 39, type = ProtobufType.STRING)
    Jid phoneJid;
    @ProtobufProperty(index = 40, type = ProtobufType.BOOL)
    boolean shareOwnPhoneNumber;
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean pnhDuplicateLidThread;
    @ProtobufProperty(index = 42, type = ProtobufType.STRING)
    Jid lid;
    @ProtobufProperty(index = 999, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.ENUM)
    final ConcurrentHashMap<Jid, ContactStatus> presences;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    Chat(Jid jid, ConcurrentLinkedSet<HistorySyncMessage> historySyncMessages, Jid newJid, Jid oldJid, int unreadMessagesCount, boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralMessageDuration, long ephemeralMessagesToggleTimeSeconds, EndOfHistoryTransferType endOfHistoryTransferType, long timestampSeconds, String name, boolean notSpam, boolean archived, ChatDisappear disappearInitiator, boolean markedAsUnread, int pinnedTimestampSeconds, ChatMute mute, ChatWallpaper wallpaper, MediaVisibility mediaVisibility, boolean suspended, boolean terminated, boolean support, String displayName, Jid phoneJid, boolean shareOwnPhoneNumber, boolean pnhDuplicateLidThread, Jid lid, ConcurrentHashMap<Jid, ContactStatus> presences) {
        this.jid = jid;
        this.historySyncMessages = historySyncMessages;
        this.newJid = newJid;
        this.oldJid = oldJid;
        this.unreadMessagesCount = unreadMessagesCount;
        this.endOfHistoryTransfer = endOfHistoryTransfer;
        this.ephemeralMessageDuration = ephemeralMessageDuration;
        this.ephemeralMessagesToggleTimeSeconds = ephemeralMessagesToggleTimeSeconds;
        this.endOfHistoryTransferType = endOfHistoryTransferType;
        this.timestampSeconds = timestampSeconds;
        this.name = name;
        this.notSpam = notSpam;
        this.archived = archived;
        this.disappearInitiator = disappearInitiator;
        this.markedAsUnread = markedAsUnread;
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
        this.mute = mute;
        this.wallpaper = wallpaper;
        this.mediaVisibility = mediaVisibility;
        this.suspended = suspended;
        this.terminated = terminated;
        this.support = support;
        this.displayName = displayName;
        this.phoneJid = phoneJid;
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        this.pnhDuplicateLidThread = pnhDuplicateLidThread;
        this.lid = lid;
        this.presences = presences;
    }

    /**
     * Returns the name of this chat
     *
     * @return a non-null string
     */
    public String name() {
        if (name != null) {
            return name;
        }

        if (displayName != null) {
            return displayName;
        }

        return jid.user();
    }

    /**
     * Returns a boolean to represent whether this chat is a group or not
     *
     * @return true if this chat is a group
     */
    public boolean isGroup() {
        return jid.type() == JidType.GROUP;
    }

    /**
     * Returns a boolean to represent whether this chat is pinned or not
     *
     * @return true if this chat is pinned
     */
    public boolean isPinned() {
        return pinnedTimestampSeconds != 0;
    }

    /**
     * Returns a boolean to represent whether ephemeral messages are enabled for this chat
     *
     * @return true if ephemeral messages are enabled for this chat
     */
    public boolean isEphemeral() {
        return ephemeralMessageDuration != ChatEphemeralTimer.OFF && ephemeralMessagesToggleTimeSeconds != 0;
    }

    /**
     * Returns all the unread messages in this chat
     *
     * @return a non-null collection
     */
    public Collection<ChatMessageInfo> unreadMessages() {
        if (!hasUnreadMessages()) {
            return List.of();
        }

        return historySyncMessages.stream()
                .limit(unreadMessagesCount())
                .map(HistorySyncMessage::messageInfo)
                .toList();
    }

    /**
     * Returns a boolean to represent whether this chat has unread messages
     *
     * @return true if this chat has unread messages
     */
    public boolean hasUnreadMessages() {
        return unreadMessagesCount > 0;
    }

    /**
     * Returns an optional value containing the seconds this chat was pinned
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> pinnedTimestamp() {
        return Clock.parseSeconds(pinnedTimestampSeconds);
    }

    /**
     * Returns the timestampSeconds for the creation of this chat in seconds since
     * {@link Instant#EPOCH}
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    /**
     * Returns an optional value containing the seconds in seconds since
     * {@link Instant#EPOCH} when ephemeral messages were turned on
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> ephemeralMessagesToggleTime() {
        return Clock.parseSeconds(ephemeralMessagesToggleTimeSeconds);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> newestMessage() {
        return Optional.ofNullable(historySyncMessages.peekLast())
                .map(HistorySyncMessage::messageInfo);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> oldestMessage() {
        return Optional.ofNullable(historySyncMessages.peekFirst())
                .map(HistorySyncMessage::messageInfo);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> newestStandardMessage() {
        return findMessageBy(this::isStandardMessage, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> oldestStandardMessage() {
        return findMessageBy(this::isStandardMessage, false);
    }

    private boolean isStandardMessage(ChatMessageInfo info) {
        return !info.message().hasCategory(MessageCategory.SERVER) && info.stubType().isEmpty();
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> newestMessageFromMe() {
        return findMessageBy(this::isMessageFromMe, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> oldestMessageFromMe() {
        return findMessageBy(this::isMessageFromMe, false);
    }

    private boolean isMessageFromMe(ChatMessageInfo info) {
        return !info.message().hasCategory(MessageCategory.SERVER) && info.stubType().isEmpty() && info.fromMe();
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> newestServerMessage() {
        return findMessageBy(this::isServerMessage, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<ChatMessageInfo> oldestServerMessage() {
        return findMessageBy(this::isServerMessage, false);
    }

    private boolean isServerMessage(ChatMessageInfo info) {
        return info.message().hasCategory(MessageCategory.SERVER) || info.stubType().isPresent();
    }

    private Optional<ChatMessageInfo> findMessageBy(Function<ChatMessageInfo, Boolean> filter, boolean newest) {
        var descendingIterator = newest ? historySyncMessages.descendingIterator() : historySyncMessages.iterator();
        while (descendingIterator.hasNext()) {
            var info = descendingIterator.next().messageInfo();
            if (filter.apply(info)) {
                return Optional.of(info);
            }
        }

        return Optional.empty();
    }


    /**
     * Returns all the starred messages in this chat
     *
     * @return a non-null list of messages
     */
    public Collection<ChatMessageInfo> starredMessages() {
        return historySyncMessages.stream()
                .map(HistorySyncMessage::messageInfo)
                .filter(ChatMessageInfo::starred)
                .toList();
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param newMessages the non-null messages to add
     */
    public void addMessages(Collection<HistorySyncMessage> newMessages) {
        historySyncMessages.addAll(newMessages);
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param oldMessages the non-null messages to add
     */
    public void addOldMessages(Collection<HistorySyncMessage> oldMessages) {
        oldMessages.forEach(historySyncMessages::addFirst);
    }

    /**
     * Adds a message to the chat in the most recent slot available
     *
     * @param info the message to add to the chat
     * @return whether the message was added
     */
    public boolean addNewMessage(ChatMessageInfo info) {
        var sync = new HistorySyncMessage(info, historySyncMessages.size());
        if (historySyncMessages.contains(sync)) {
            return false;
        }
        historySyncMessages.add(sync);
        updateChatTimestamp(info);
        return true;
    }

    /**
     * Adds a message to the chat in the oldest slot available
     *
     * @param info the message to add to the chat
     * @return whether the message was added
     */
    public boolean addOldMessage(HistorySyncMessage info) {
        historySyncMessages.addFirst(info);
        return true;
    }

    /**
     * Remove a message from the chat
     *
     * @param info the message to remove
     * @return whether the message was removed
     */
    public boolean removeMessage(ChatMessageInfo info) {
        var result = historySyncMessages.removeIf(entry -> Objects.equals(entry.messageInfo().id(), info.id()));
        refreshChatTimestamp();
        return result;
    }

    /**
     * Remove a message from the chat
     *
     * @param predicate the predicate that determines if a message should be removed
     * @return whether the message was removed
     */
    public boolean removeMessage(Predicate<? super ChatMessageInfo> predicate) {
        var result = historySyncMessages.removeIf(entry -> predicate.test(entry.messageInfo()));
        refreshChatTimestamp();
        return result;
    }

    private void refreshChatTimestamp() {
        var message = newestMessage();
        if (message.isEmpty()) {
            return;
        }

        updateChatTimestamp(message.get());
    }

    private void updateChatTimestamp(ChatMessageInfo info) {
        if (info.timestampSeconds().isEmpty()) {
            return;
        }

        var newTimestamp = info.timestampSeconds()
                .getAsLong();
        var oldTimeStamp = newestMessage()
                .map(value -> value.timestampSeconds().orElse(0L))
                .orElse(0L);
        if (oldTimeStamp > newTimestamp) {
            return;
        }

        this.timestampSeconds = newTimestamp;
    }

    /**
     * Removes all messages from the chat
     */
    public void removeMessages() {
        historySyncMessages.clear();
    }

    /**
     * Returns an immutable list of messages wrapped in history syncs
     * This is useful for the proto
     *
     * @return a non-null collection
     */
    public Collection<HistorySyncMessage> messages() {
        return Collections.unmodifiableCollection(historySyncMessages);
    }

    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    @Override
    public Jid toJid() {
        return jid();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Chat chat &&
                unreadMessagesCount == chat.unreadMessagesCount &&
                endOfHistoryTransfer == chat.endOfHistoryTransfer &&
                ephemeralMessagesToggleTimeSeconds == chat.ephemeralMessagesToggleTimeSeconds &&
                timestampSeconds == chat.timestampSeconds &&
                notSpam == chat.notSpam &&
                archived == chat.archived &&
                markedAsUnread == chat.markedAsUnread &&
                pinnedTimestampSeconds == chat.pinnedTimestampSeconds &&
                suspended == chat.suspended &&
                terminated == chat.terminated &&
                support == chat.support &&
                shareOwnPhoneNumber == chat.shareOwnPhoneNumber &&
                pnhDuplicateLidThread == chat.pnhDuplicateLidThread &&
                Objects.equals(jid, chat.jid) &&
                Objects.equals(historySyncMessages, chat.historySyncMessages) &&
                Objects.equals(newJid, chat.newJid) &&
                Objects.equals(oldJid, chat.oldJid) &&
                ephemeralMessageDuration == chat.ephemeralMessageDuration &&
                endOfHistoryTransferType == chat.endOfHistoryTransferType &&
                Objects.equals(name, chat.name) &&
                Objects.equals(disappearInitiator, chat.disappearInitiator) &&
                Objects.equals(mute, chat.mute) &&
                Objects.equals(wallpaper, chat.wallpaper) &&
                mediaVisibility == chat.mediaVisibility &&
                Objects.equals(displayName, chat.displayName) &&
                Objects.equals(phoneJid, chat.phoneJid) &&
                Objects.equals(lid, chat.lid) &&
                Objects.equals(presences, chat.presences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, historySyncMessages, newJid, oldJid, unreadMessagesCount, endOfHistoryTransfer, ephemeralMessageDuration, ephemeralMessagesToggleTimeSeconds, endOfHistoryTransferType, timestampSeconds, name, notSpam, archived, disappearInitiator, markedAsUnread, pinnedTimestampSeconds, mute, wallpaper, mediaVisibility, suspended, terminated, support, displayName, phoneJid, shareOwnPhoneNumber, pnhDuplicateLidThread, lid, presences);
    }

    public Jid jid() {
        return jid;
    }

    public Collection<HistorySyncMessage> historySyncMessages() {
        return historySyncMessages;
    }

    public Optional<Jid> newJid() {
        return Optional.ofNullable(newJid);
    }

    public Optional<Jid> oldJid() {
        return Optional.ofNullable(oldJid);
    }

    public int unreadMessagesCount() {
        return unreadMessagesCount;
    }

    public boolean endOfHistoryTransfer() {
        return endOfHistoryTransfer;
    }

    public ChatEphemeralTimer ephemeralMessageDuration() {
        return ephemeralMessageDuration;
    }

    public long ephemeralMessagesToggleTimeSeconds() {
        return ephemeralMessagesToggleTimeSeconds;
    }

    public Optional<EndOfHistoryTransferType> endOfHistoryTransferType() {
        return Optional.ofNullable(endOfHistoryTransferType);
    }

    public long timestampSeconds() {
        return timestampSeconds;
    }

    public boolean notSpam() {
        return notSpam;
    }

    public boolean archived() {
        return archived;
    }

    public Optional<ChatDisappear> disappearInitiator() {
        return Optional.ofNullable(disappearInitiator);
    }

    public boolean markedAsUnread() {
        return markedAsUnread;
    }

    public int pinnedTimestampSeconds() {
        return pinnedTimestampSeconds;
    }

    public ChatMute mute() {
        return mute;
    }

    public Optional<ChatWallpaper> wallpaper() {
        return Optional.ofNullable(wallpaper);
    }

    public MediaVisibility mediaVisibility() {
        return mediaVisibility;
    }

    public boolean suspended() {
        return suspended;
    }

    public boolean terminated() {
        return terminated;
    }

    public boolean support() {
        return support;
    }

    public Optional<Jid> phoneJid() {
        return Optional.ofNullable(phoneJid);
    }

    public boolean pnhDuplicateLidThread() {
        return pnhDuplicateLidThread;
    }

    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lid);
    }

    public Optional<ContactStatus> getPresence(JidProvider jid) {
        return Optional.ofNullable(presences.get(jid.toJid()));
    }

    public void addPresence(JidProvider jid, ContactStatus status) {
        presences.put(jid.toJid(), status);
    }

    public boolean removePresence(JidProvider jid) {
        return presences.remove(jid.toJid()) != null;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean shareOwnPhoneNumber() {
        return shareOwnPhoneNumber;
    }

    public Chat setUnreadMessagesCount(int unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
        return this;
    }

    public Chat setEndOfHistoryTransfer(boolean endOfHistoryTransfer) {
        this.endOfHistoryTransfer = endOfHistoryTransfer;
        return this;
    }

    public Chat setEphemeralMessageDuration(ChatEphemeralTimer ephemeralMessageDuration) {
        this.ephemeralMessageDuration = ephemeralMessageDuration;
        return this;
    }

    public Chat setEphemeralMessagesToggleTimeSeconds(long ephemeralMessagesToggleTimeSeconds) {
        this.ephemeralMessagesToggleTimeSeconds = ephemeralMessagesToggleTimeSeconds;
        return this;
    }

    public Chat setEndOfHistoryTransferType(EndOfHistoryTransferType endOfHistoryTransferType) {
        this.endOfHistoryTransferType = endOfHistoryTransferType;
        return this;
    }

    public Chat setTimestampSeconds(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
        return this;
    }

    public Chat setName(String name) {
        this.name = name;
        return this;
    }

    public Chat setNotSpam(boolean notSpam) {
        this.notSpam = notSpam;
        return this;
    }

    public Chat setArchived(boolean archived) {
        this.archived = archived;
        return this;
    }

    public Chat setDisappearInitiator(ChatDisappear disappearInitiator) {
        this.disappearInitiator = disappearInitiator;
        return this;
    }

    public Chat setMarkedAsUnread(boolean markedAsUnread) {
        this.markedAsUnread = markedAsUnread;
        return this;
    }

    public Chat setPinnedTimestampSeconds(int pinnedTimestampSeconds) {
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
        return this;
    }

    public Chat setMute(ChatMute mute) {
        this.mute = mute;
        return this;
    }

    public Chat setWallpaper(ChatWallpaper wallpaper) {
        this.wallpaper = wallpaper;
        return this;
    }

    public Chat setMediaVisibility(MediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
        return this;
    }

    public Chat setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public Chat setTerminated(boolean terminated) {
        this.terminated = terminated;
        return this;
    }

    public Chat setSupport(boolean support) {
        this.support = support;
        return this;
    }

    public Chat setPhoneJid(Jid phoneJid) {
        this.phoneJid = phoneJid;
        return this;
    }

    public Chat setShareOwnPhoneNumber(boolean shareOwnPhoneNumber) {
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        return this;
    }

    public Chat setPnhDuplicateLidThread(boolean pnhDuplicateLidThread) {
        this.pnhDuplicateLidThread = pnhDuplicateLidThread;
        return this;
    }

    public Chat setLid(Jid lid) {
        this.lid = lid;
        return this;
    }

    /**
     * The constants of this enumerated type describe the various types of transfers that can regard a
     * chat history sync
     */
    @ProtobufEnum(name = "Conversation.EndOfHistoryTransferType")
    public enum EndOfHistoryTransferType {
        /**
         * Complete, but more messages remain on the phone
         */
        COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY(0),

        /**
         * Complete and no more messages remain on the phone
         */
        COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY(1);

        final int index;

        EndOfHistoryTransferType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}