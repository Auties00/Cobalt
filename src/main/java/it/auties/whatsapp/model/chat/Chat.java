package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.jid.JidType;
import it.auties.whatsapp.model.media.MediaVisibility;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.ConcurrentLinkedHashedDequeue;

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
@ProtobufMessageName("Conversation")
public final class Chat implements ProtobufMessage, JidProvider {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
    final ConcurrentLinkedHashedDequeue<HistorySyncMessage> historySyncMessages;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid newJid;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid oldJid;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    int unreadMessagesCount;

    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean readOnly;

    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean endOfHistoryTransfer;

    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    ChatEphemeralTimer ephemeralMessageDuration;

    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    long ephemeralMessagesToggleTimeSeconds;

    @ProtobufProperty(index = 11, type = ProtobufType.OBJECT)
    EndOfHistoryTransferType endOfHistoryTransferType;

    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    long timestampSeconds;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String name;

    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    boolean notSpam;

    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    boolean archived;
    @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
    ChatDisappear disappearInitiator;

    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    boolean markedAsUnread;

    @ProtobufProperty(index = 20, type = ProtobufType.OBJECT)
    final List<GroupParticipant> participants;

    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    byte[] token;

    @ProtobufProperty(index = 22, type = ProtobufType.UINT64)
    long tokenTimestampSeconds;

    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    byte[] identityKey;
    
    @ProtobufProperty(index = 24, type = ProtobufType.UINT32)
    int pinnedTimestampSeconds;

    @ProtobufProperty(index = 25, type = ProtobufType.UINT64)
    ChatMute mute;

    @ProtobufProperty(index = 26, type = ProtobufType.OBJECT)
    ChatWallpaper wallpaper;

    @ProtobufProperty(index = 27, type = ProtobufType.OBJECT)
    MediaVisibility mediaVisibility;

    @ProtobufProperty(index = 28, type = ProtobufType.UINT64)
    long tokenSenderTimestampSeconds;

    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean suspended;

    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    boolean terminated;

    @ProtobufProperty(index = 31, type = ProtobufType.UINT64)
    long foundationTimestampSeconds;

    @ProtobufProperty(index = 32, type = ProtobufType.STRING)
    Jid founder;
    @ProtobufProperty(index = 33, type = ProtobufType.STRING)
    String description;

    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    boolean support;

    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    boolean parentGroup;

    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    boolean defaultSubGroup;

    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    final Jid parentGroupJid;

    @ProtobufProperty(index = 38, type = ProtobufType.STRING)
    String displayName;

    @ProtobufProperty(index = 39, type = ProtobufType.STRING)
    Jid phoneJid;
    
    @ProtobufProperty(index = 40, type = ProtobufType.BOOL)
    boolean shareOwnPhoneNumber;
    
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean pnhDuplicateLidThread;
    
    @ProtobufProperty(index = 42, type = ProtobufType.STRING)
    Jid lidJid;
    
    @ProtobufProperty(index = 999, type = ProtobufType.MAP, keyType = ProtobufType.STRING, valueType = ProtobufType.OBJECT)
    final ConcurrentHashMap<Jid, ContactStatus> presences;
    
    @ProtobufProperty(index = 1000, type = ProtobufType.STRING)
    final Set<Jid> participantsPreKeys;
    
    @ProtobufProperty(index = 1001, type = ProtobufType.OBJECT)
    final Set<GroupPastParticipant> pastParticipants;

    private boolean update;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Chat(Jid jid, ConcurrentLinkedHashedDequeue<HistorySyncMessage> historySyncMessages, Jid newJid, Jid oldJid, int unreadMessagesCount, boolean readOnly, boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralMessageDuration, long ephemeralMessagesToggleTimeSeconds, EndOfHistoryTransferType endOfHistoryTransferType, long timestampSeconds, String name, boolean notSpam, boolean archived, ChatDisappear disappearInitiator, boolean markedAsUnread, List<GroupParticipant> participants, byte[] token, long tokenTimestampSeconds, byte[] identityKey, int pinnedTimestampSeconds, ChatMute mute, ChatWallpaper wallpaper, MediaVisibility mediaVisibility, long tokenSenderTimestampSeconds, boolean suspended, boolean terminated, long foundationTimestampSeconds, Jid founder, String description, boolean support, boolean parentGroup, boolean defaultSubGroup, Jid parentGroupJid, String displayName, Jid phoneJid, boolean shareOwnPhoneNumber, boolean pnhDuplicateLidThread, Jid lidJid, ConcurrentHashMap<Jid, ContactStatus> presences, Set<Jid> participantsPreKeys, Set<GroupPastParticipant> pastParticipants) {
        this.jid = jid;
        this.historySyncMessages = historySyncMessages;
        this.newJid = newJid;
        this.oldJid = oldJid;
        this.unreadMessagesCount = unreadMessagesCount;
        this.readOnly = readOnly;
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
        this.participants = participants;
        this.token = token;
        this.tokenTimestampSeconds = tokenTimestampSeconds;
        this.identityKey = identityKey;
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
        this.mute = mute;
        this.wallpaper = wallpaper;
        this.mediaVisibility = mediaVisibility;
        this.tokenSenderTimestampSeconds = tokenSenderTimestampSeconds;
        this.suspended = suspended;
        this.terminated = terminated;
        this.foundationTimestampSeconds = foundationTimestampSeconds;
        this.founder = founder;
        this.description = description;
        this.support = support;
        this.parentGroup = parentGroup;
        this.defaultSubGroup = defaultSubGroup;
        this.parentGroupJid = parentGroupJid;
        this.displayName = displayName;
        this.phoneJid = phoneJid;
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        this.pnhDuplicateLidThread = pnhDuplicateLidThread;
        this.lidJid = lidJid;
        this.presences = presences;
        this.participantsPreKeys = participantsPreKeys;
        this.pastParticipants = pastParticipants;
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
     * Returns the timestampSeconds for the creation of this chat's token
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> tokenTimestamp() {
        return Clock.parseSeconds(tokenTimestampSeconds);
    }

    /**
     * Returns the timestampSeconds for the token sender creation of this chat
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> tokenSenderTimestamp() {
        return Clock.parseSeconds(tokenSenderTimestampSeconds);
    }

    /**
     * Returns the timestampSeconds for the creation of this chat if it's a group
     *
     * @return an optional
     */
    public Optional<ZonedDateTime> foundationTimestamp() {
        return Clock.parseSeconds(foundationTimestampSeconds);
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param newMessages the non-null messages to add
     */
    public void addMessages(Collection<HistorySyncMessage> newMessages) {
        historySyncMessages.addAll(newMessages);
        this.update = true;
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param oldMessages the non-null messages to add
     */
    public void addOldMessages(Collection<HistorySyncMessage> oldMessages) {
        oldMessages.forEach(historySyncMessages::addFirst);
        this.update = true;
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
        this.update = true;
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
        this.update = true;
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
        if (result) {
            this.update = true;
        }

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
        if(info.timestampSeconds().isEmpty()) {
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
        this.update = true;
    }

    /**
     * Removes all messages from the chat
     */
    public void removeMessages() {
        historySyncMessages.clear();
        this.update = true;
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
     * Adds a collection of participants to this chat
     *
     * @param participants the participants to add
     */
    public void addParticipants(Collection<GroupParticipant> participants) {
        participants.forEach(this::addParticipant);
        this.update = true;
    }

    /**
     * Adds a participant to this chat
     *
     * @param jid  the non-null jid of the participant
     * @param role the role of the participant
     * @return whether the participant was added
     */
    public boolean addParticipant(Jid jid, GroupRole role) {
        var result = addParticipant(new GroupParticipant(jid, role));
        if (result) {
            this.update = true;
        }

        return result;
    }

    /**
     * Adds a participant to this chat
     *
     * @param participant the non-null participant
     * @return whether the participant was added
     */
    public boolean addParticipant(GroupParticipant participant) {
        var result = participants.add(participant);
        this.update = true;
        return true;
    }

    /**
     * Removes a participant from this chat
     *
     * @param jid the non-null jid of the participant
     * @return whether the participant was removed
     */
    public boolean removeParticipant(Jid jid) {
        var result = participants.removeIf(entry -> Objects.equals(entry.jid(), jid));
        if (result) {
            this.update = true;
        }

        return result;
    }

    /**
     * Finds a participant by jid
     * This method only works if {@link Whatsapp#queryGroupMetadata(JidProvider)} has been called before on this chat.
     * By default, all groups that have been used in the last two weeks wil be synced automatically
     *
     * @param jid the non-null jid of the participant
     * @return the participant, if present
     */
    public Optional<GroupParticipant> findParticipant(Jid jid) {
        return participants.stream()
                .filter(entry -> Objects.equals(entry.jid(), jid))
                .findFirst();
    }

    /**
     * Adds a past participant
     *
     * @param participant the non-null jid of the past participant
     * @return whether the participant was added
     */
    public boolean addPastParticipant(GroupPastParticipant participant) {
        var result = pastParticipants.add(participant);
        if (result) {
            this.update = true;
        }

        return result;
    }

    /**
     * Adds a collection of past participants
     *
     * @param pastParticipants the non-null list of past participants
     * @return whether the participant were added
     */
    public boolean addPastParticipants(List<GroupPastParticipant> pastParticipants) {
        var result = true;
        for (var pastParticipant : pastParticipants) {
            result &= this.pastParticipants.add(pastParticipant);
        }

        if (result) {
            this.update = true;
        }

        return result;
    }

    /**
     * Removes a past participant
     *
     * @param jid the non-null jid of the past participant
     * @return whether the participant was removed
     */
    public boolean removePastParticipant(Jid jid) {
        var result = pastParticipants.removeIf(entry -> Objects.equals(entry.jid(), jid));
        if (result) {
            this.update = true;
        }

        return result;
    }

    /**
     * Finds a past participant by jid
     *
     * @param jid the non-null jid of the past participant
     * @return the past participant, if present
     */
    public Optional<GroupPastParticipant> findPastParticipant(Jid jid) {
        return pastParticipants.stream()
                .filter(entry -> Objects.equals(entry.jid(), jid))
                .findFirst();
    }

    public Set<Jid> participantsPreKeys() {
        return Collections.unmodifiableSet(participantsPreKeys);
    }

    public void addParticipantsPreKeys(Collection<Jid> jids) {
        participantsPreKeys.addAll(jids);
        this.update = true;
    }

    public void clearParticipantsPreKeys() {
        participantsPreKeys.clear();
        this.update = true;
    }

    /**
     * Checks if this chat is equal to another chat
     *
     * @param other the chat
     * @return a boolean
     */
    public boolean equals(Object other) {
        return other instanceof Chat that
                && Objects.equals(this.jid(), that.jid());
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

    /**
     * Returns the hash code for this chat
     *
     * @return an int
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid());
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

    public boolean readOnly() {
        return readOnly;
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

    public List<GroupParticipant> participants() {
        return Collections.unmodifiableList(participants);
    }

    public Optional<byte[]> token() {
        return Optional.ofNullable(token);
    }

    public long tokenTimestampSeconds() {
        return tokenTimestampSeconds;
    }

    public Optional<byte[]> identityKey() {
        return Optional.ofNullable(identityKey);
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

    public long tokenSenderTimestampSeconds() {
        return tokenSenderTimestampSeconds;
    }

    public boolean suspended() {
        return suspended;
    }

    public boolean terminated() {
        return terminated;
    }

    public long foundationTimestampSeconds() {
        return foundationTimestampSeconds;
    }

    public Optional<Jid> founder() {
        return Optional.ofNullable(founder);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public boolean support() {
        return support;
    }

    public boolean parentGroup() {
        return parentGroup;
    }

    public boolean defaultSubGroup() {
        return defaultSubGroup;
    }

    public Optional<Jid> parentGroupJid() {
        return Optional.ofNullable(parentGroupJid);
    }

    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    public Optional<Jid> phoneJid() {
        return Optional.ofNullable(phoneJid);
    }

    public boolean pnhDuplicateLidThread() {
        return pnhDuplicateLidThread;
    }

    public Optional<Jid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    public ConcurrentHashMap<Jid, ContactStatus> presences() {
        return presences;
    }

    public Set<GroupPastParticipant> pastParticipants() {
        return pastParticipants;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean shareOwnPhoneNumber() {
        return shareOwnPhoneNumber;
    }

    public Chat setUnreadMessagesCount(int unreadMessagesCount) {
        this.unreadMessagesCount = unreadMessagesCount;
        this.update = true;
        return this;
    }

    public Chat setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        this.update = true;
        return this;
    }

    public Chat setEndOfHistoryTransfer(boolean endOfHistoryTransfer) {
        this.endOfHistoryTransfer = endOfHistoryTransfer;
        this.update = true;
        return this;
    }

    public Chat setEphemeralMessageDuration(ChatEphemeralTimer ephemeralMessageDuration) {
        this.ephemeralMessageDuration = ephemeralMessageDuration;
        this.update = true;
        return this;
    }

    public Chat setEphemeralMessagesToggleTimeSeconds(long ephemeralMessagesToggleTimeSeconds) {
        this.ephemeralMessagesToggleTimeSeconds = ephemeralMessagesToggleTimeSeconds;
        this.update = true;
        return this;
    }

    public Chat setEndOfHistoryTransferType(EndOfHistoryTransferType endOfHistoryTransferType) {
        this.endOfHistoryTransferType = endOfHistoryTransferType;
        this.update = true;
        return this;
    }

    public Chat setTimestampSeconds(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
        this.update = true;
        return this;
    }

    public Chat setName(String name) {
        this.name = name;
        this.update = true;
        return this;
    }

    public Chat setNotSpam(boolean notSpam) {
        this.notSpam = notSpam;
        this.update = true;
        return this;
    }

    public Chat setArchived(boolean archived) {
        this.archived = archived;
        this.update = true;
        return this;
    }

    public Chat setDisappearInitiator(ChatDisappear disappearInitiator) {
        this.disappearInitiator = disappearInitiator;
        this.update = true;
        return this;
    }

    public Chat setMarkedAsUnread(boolean markedAsUnread) {
        this.markedAsUnread = markedAsUnread;
        this.update = true;
        return this;
    }

    public Chat setToken(byte[] token) {
        this.token = token;
        this.update = true;
        return this;
    }

    public Chat setTokenTimestampSeconds(long tokenTimestampSeconds) {
        this.tokenTimestampSeconds = tokenTimestampSeconds;
        this.update = true;
        return this;
    }

    public Chat setIdentityKey(byte[] identityKey) {
        this.identityKey = identityKey;
        this.update = true;
        return this;
    }

    public Chat setPinnedTimestampSeconds(int pinnedTimestampSeconds) {
        this.pinnedTimestampSeconds = pinnedTimestampSeconds;
        this.update = true;
        return this;
    }

    public Chat setMute(ChatMute mute) {
        this.mute = mute;
        this.update = true;
        return this;
    }

    public Chat setWallpaper(ChatWallpaper wallpaper) {
        this.wallpaper = wallpaper;
        this.update = true;
        return this;
    }

    public Chat setMediaVisibility(MediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
        this.update = true;
        return this;
    }

    public Chat setTokenSenderTimestampSeconds(long tokenSenderTimestampSeconds) {
        this.tokenSenderTimestampSeconds = tokenSenderTimestampSeconds;
        this.update = true;
        return this;
    }

    public Chat setSuspended(boolean suspended) {
        this.suspended = suspended;
        this.update = true;
        return this;
    }

    public Chat setTerminated(boolean terminated) {
        this.terminated = terminated;
        this.update = true;
        return this;
    }

    public Chat setFoundationTimestampSeconds(long foundationTimestampSeconds) {
        this.foundationTimestampSeconds = foundationTimestampSeconds;
        this.update = true;
        return this;
    }

    public Chat setFounder(Jid founder) {
        this.founder = founder;
        this.update = true;
        return this;
    }

    public Chat setDescription(String description) {
        this.description = description;
        this.update = true;
        return this;
    }

    public Chat setSupport(boolean support) {
        this.support = support;
        this.update = true;
        return this;
    }

    public Chat setParentGroup(boolean parentGroup) {
        this.parentGroup = parentGroup;
        this.update = true;
        return this;
    }

    public Chat setDefaultSubGroup(boolean defaultSubGroup) {
        this.defaultSubGroup = defaultSubGroup;
        this.update = true;
        return this;
    }

    public Chat setDisplayName(String displayName) {
        this.displayName = displayName;
        this.update = true;
        return this;
    }

    public Chat setPhoneJid(Jid phoneJid) {
        this.phoneJid = phoneJid;
        this.update = true;
        return this;
    }

    public Chat setShareOwnPhoneNumber(boolean shareOwnPhoneNumber) {
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        this.update = true;
        return this;
    }

    public Chat setPnhDuplicateLidThread(boolean pnhDuplicateLidThread) {
        this.pnhDuplicateLidThread = pnhDuplicateLidThread;
        this.update = true;
        return this;
    }

    public Chat setLidJid(Jid lidJid) {
        this.lidJid = lidJid;
        this.update = true;
        return this;
    }

    public boolean hasUpdate() {
        return update;
    }

    /**
     * The constants of this enumerated type describe the various types of transfers that can regard a
     * chat history sync
     */
    @ProtobufMessageName("Conversation.EndOfHistoryTransferType")
    public enum EndOfHistoryTransferType implements ProtobufEnum {
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