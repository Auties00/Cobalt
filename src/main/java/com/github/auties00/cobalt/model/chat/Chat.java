package com.github.auties00.cobalt.model.chat;

import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfoParent;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidProvider;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.media.MediaVisibility;
import com.github.auties00.cobalt.model.sync.HistorySyncMessage;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.collections.ConcurrentLinkedHashMap;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A model class that represents a Chat. A chat can be of two types: a conversation with a contact
 * or a group. This class is only a model, this means that changing its values will have no real
 * effect on WhatsappWeb's servers
 */
@ProtobufMessage(name = "Conversation")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class Chat implements MessageInfoParent {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final Messages messages;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid newJid;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    Jid oldJid;

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
    boolean phoneDuplicateLidThread;

    @ProtobufProperty(index = 42, type = ProtobufType.STRING)
    Jid lid;

    @ProtobufProperty(index = 999, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.ENUM)
    final ConcurrentHashMap<Jid, ContactStatus> presences;

    Chat(Jid jid, Messages messages, Jid newJid, Jid oldJid, int unreadMessagesCount, boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralMessageDuration, long ephemeralMessagesToggleTimeSeconds, EndOfHistoryTransferType endOfHistoryTransferType, long timestampSeconds, String name, boolean notSpam, boolean archived, ChatDisappear disappearInitiator, boolean markedAsUnread, int pinnedTimestampSeconds, ChatMute mute, ChatWallpaper wallpaper, MediaVisibility mediaVisibility, boolean suspended, boolean terminated, boolean support, String displayName, Jid phoneJid, boolean shareOwnPhoneNumber, boolean phoneDuplicateLidThread, Jid lid, ConcurrentHashMap<Jid, ContactStatus> presences) {
        this.jid = jid;
        this.messages = messages;
        this.newJid = newJid;
        this.oldJid = oldJid;
        this.unreadMessagesCount = unreadMessagesCount;
        this.endOfHistoryTransfer = endOfHistoryTransfer;
        this.ephemeralMessageDuration = Objects.requireNonNullElse(ephemeralMessageDuration, ChatEphemeralTimer.OFF);
        this.ephemeralMessagesToggleTimeSeconds = ephemeralMessagesToggleTimeSeconds;
        this.endOfHistoryTransferType = endOfHistoryTransferType;
        this.timestampSeconds = timestampSeconds;
        this.name = name;
        this.notSpam = notSpam;
        this.archived = archived;
        this.disappearInitiator = disappearInitiator;
        this.markedAsUnread = markedAsUnread;
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
        this.mute = Objects.requireNonNullElse(mute, ChatMute.notMuted());
        this.wallpaper = wallpaper;
        this.mediaVisibility = mediaVisibility;
        this.suspended = suspended;
        this.terminated = terminated;
        this.support = support;
        this.displayName = displayName;
        this.phoneJid = phoneJid;
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        this.phoneDuplicateLidThread = phoneDuplicateLidThread;
        this.lid = lid;
        this.presences = presences;
    }

    /**
     * Returns the JID associated with this chat.
     *
     * @return a non-null Jid
     */
    public Jid jid() {
        return jid;
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

    public boolean phoneDuplicateLidThread() {
        return phoneDuplicateLidThread;
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

    public void setUnreadMessagesCount(int unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
    }

    public void setEndOfHistoryTransfer(boolean endOfHistoryTransfer) {
        this.endOfHistoryTransfer = endOfHistoryTransfer;
    }

    public void setEphemeralMessageDuration(ChatEphemeralTimer ephemeralMessageDuration) {
        this.ephemeralMessageDuration = ephemeralMessageDuration;
    }

    public void setEphemeralMessagesToggleTimeSeconds(long ephemeralMessagesToggleTimeSeconds) {
        this.ephemeralMessagesToggleTimeSeconds = ephemeralMessagesToggleTimeSeconds;
    }

    public void setEndOfHistoryTransferType(EndOfHistoryTransferType endOfHistoryTransferType) {
        this.endOfHistoryTransferType = endOfHistoryTransferType;
    }

    public void setTimestampSeconds(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotSpam(boolean notSpam) {
        this.notSpam = notSpam;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setDisappearInitiator(ChatDisappear disappearInitiator) {
        this.disappearInitiator = disappearInitiator;
    }

    public void setMarkedAsUnread(boolean markedAsUnread) {
        this.markedAsUnread = markedAsUnread;
    }

    public void setPinnedTimestampSeconds(int pinnedTimestampSeconds) {
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
    }

    public void setMute(ChatMute mute) {
        this.mute = mute;
    }

    public void setWallpaper(ChatWallpaper wallpaper) {
        this.wallpaper = wallpaper;
    }

    public void setMediaVisibility(MediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public void setPhoneJid(Jid phoneJid) {
        this.phoneJid = phoneJid;
    }

    public void setShareOwnPhoneNumber(boolean shareOwnPhoneNumber) {
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
    }

    public void setPhoneDuplicateLidThread(boolean phoneDuplicateLidThread) {
        this.phoneDuplicateLidThread = phoneDuplicateLidThread;
    }

    public void setLid(Jid lid) {
        this.lid = lid;
    }

    /**
     * Returns the name of this chat
     *
     * @return a non-null value
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
    public boolean isGroupOrCommunity() {
        return jid.server().type() == JidServer.Type.GROUP_OR_COMMUNITY;
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

        return messages.getMessageInfosAsStream()
                .limit(unreadMessagesCount())
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
    @Override
    public Optional<ChatMessageInfo> newestMessage() {
        return messages.getNewestMessageInfo();
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     *
     * @return an optional
     */
    @Override
    public Optional<ChatMessageInfo> oldestMessage() {
        return messages.getOldestMessageInfo();
    }

    /**
     * Returns all the starred messages in this chat
     *
     * @return a non-null list of messages
     */
    public SequencedCollection<ChatMessageInfo> starredMessages() {
        return messages.getMessageInfosAsStream()
                .filter(ChatMessageInfo::starred)
                .toList();
    }

    /**
     * Adds a message to the chat in the most recent slot available
     *
     * @param info the message to add to the chat
     */
    public void addMessage(ChatMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        messages.addMessageInfo(info);
        updateChatTimestamp(info);
    }

    /**
     * Remove a message from the chat
     *
     * @param info the message to remove
     * @return whether the message was removed
     */
    @Override
    public boolean removeMessage(String info) {
        if(!messages.removeMessageInfoById(info)) {
            return false;
        }

        refreshChatTimestamp();
        return true;
    }

    private void refreshChatTimestamp() {
        var message = newestMessage();
        if (message.isEmpty()) {
            this.timestampSeconds = 0L;
        }else {
            updateChatTimestamp(message.get());
        }
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
    @Override
    public void removeMessages() {
        messages.clear();
    }

    /**
     * Returns an immutable list of messages wrapped in history syncs
     * This is useful for the proto
     *
     * @return a non-null collection
     */
    public SequencedCollection<ChatMessageInfo> messages() {
        return messages.getMessageInfosAsSequencedCollection();
    }

    /**
     * Returns this object as a value
     *
     * @return a non-null value
     */
    @Override
    public Jid toJid() {
        var lid = this.lid;
        if(lid != null) {
            return lid;
        } else {
            return jid;
        }
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
                phoneDuplicateLidThread == chat.phoneDuplicateLidThread &&
               Objects.equals(jid, chat.jid) &&
               Objects.equals(messages, chat.messages) &&
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
        return Objects.hash(jid, messages, newJid, oldJid, unreadMessagesCount, endOfHistoryTransfer, ephemeralMessageDuration, ephemeralMessagesToggleTimeSeconds, endOfHistoryTransferType, timestampSeconds, name, notSpam, archived, disappearInitiator, markedAsUnread, pinnedTimestampSeconds, mute, wallpaper, mediaVisibility, suspended, terminated, support, displayName, phoneJid, shareOwnPhoneNumber, phoneDuplicateLidThread, lid, presences);
    }

    @Override
    public Optional<ChatMessageInfo> getMessageById(String id) {
        return messages.getMessageInfoById(id);
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

    }
    
    static final class Messages extends AbstractCollection<HistorySyncMessage> {
        private final ConcurrentLinkedHashMap<String, HistorySyncMessage> backing;

        Messages() {
            this.backing = new ConcurrentLinkedHashMap<>();
        }

        @Override
        public boolean add(HistorySyncMessage historySyncMessage) {
            if(historySyncMessage == null || historySyncMessage.messageInfo() == null) {
                return false;
            }else {
                backing.put(historySyncMessage.messageInfo().id(), historySyncMessage);
                return true;
            }
        }

        public boolean addMessageInfo(ChatMessageInfo messageInfo) {
            if(messageInfo == null) {
                return false;
            }else {
                backing.put(messageInfo.id(), new HistorySyncMessage(messageInfo, -1));
                return true;
            }
        }

        public Optional<ChatMessageInfo> getMessageInfoById(String id) {
            return Optional.ofNullable(backing.get(id))
                    .map(HistorySyncMessage::messageInfo);
        }
        
        public Optional<ChatMessageInfo> getOldestMessageInfo() {
            return Optional.ofNullable(backing.firstEntry())
                    .map(entry -> entry.getValue().messageInfo());
        }
        
        public Optional<ChatMessageInfo> getNewestMessageInfo() {
            return Optional.ofNullable(backing.lastEntry())
                    .map(entry -> entry.getValue().messageInfo());
        }

        public boolean removeMessageInfoById(String id) {
            return backing.remove(id) != null;
        }
        
        public Stream<ChatMessageInfo> getMessageInfosAsStream() {
            return backing.sequencedValues()
                    .stream()
                    .map(HistorySyncMessage::messageInfo);
        }

        @Override
        public Iterator<HistorySyncMessage> iterator() {
            return backing.sequencedValues().iterator();
        }
        
        @Override
        public int size() {
            return backing.size();
        }

        public SequencedCollection<ChatMessageInfo> getMessageInfosAsSequencedCollection() {
            return getMessageInfosAsSequencedCollection(backing.sequencedValues());
        }

        private SequencedCollection<ChatMessageInfo> getMessageInfosAsSequencedCollection(SequencedCollection<HistorySyncMessage> data) {
            return new SequencedCollection<>() {
                @Override
                public SequencedCollection<ChatMessageInfo> reversed() {
                    return getMessageInfosAsSequencedCollection(data.reversed());
                }

                @Override
                public int size() {
                    return data.size();
                }

                @Override
                public boolean isEmpty() {
                    return data.isEmpty();
                }

                @Override
                public boolean contains(Object o) {
                    return o instanceof ChatMessageInfo chatMessageInfo
                            && backing.containsKey(chatMessageInfo.id());
                }

                @Override
                public Iterator<ChatMessageInfo> iterator() {
                    var iterator = data.iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public ChatMessageInfo next() {
                            var result = iterator.next();
                            return result != null ? result.messageInfo() : null;
                        }
                    };
                }

                @Override
                public Object[] toArray() {
                    return data.toArray();
                }

                @Override
                public <T> T[] toArray(T[] a) {
                    return data.toArray(a);
                }

                @Override
                public boolean add(ChatMessageInfo chatMessageInfo) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean remove(Object o) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean containsAll(Collection<?> c) {
                    for(var entry : c) {
                        if(!contains(entry)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public boolean addAll(Collection<? extends ChatMessageInfo> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}