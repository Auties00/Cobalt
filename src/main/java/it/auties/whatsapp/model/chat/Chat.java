package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactJidType;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A model class that represents a Chat. A chat can be of two types: a conversation with a contact
 * or a group. This class is only a model, this means that changing its values will have no real
 * effect on WhatsappWeb's servers
 */
public final class Chat implements ProtobufMessage, ContactJidProvider {
    @NonNull
    private final UUID uuid;

    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final @NonNull ContactJid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
    private final @NonNull ConcurrentLinkedDeque<HistorySyncMessage> historySyncMessages;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final ContactJid newJid;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private final ContactJid oldJid;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private int unreadMessagesCount;

    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    private boolean readOnly;

    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    private boolean endOfHistoryTransfer;

    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    private @NonNull ChatEphemeralTimer ephemeralMessageDuration;

    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    private long ephemeralMessagesToggleTimeSeconds;

    @ProtobufProperty(index = 11, type = ProtobufType.OBJECT)
    private EndOfHistoryTransferType endOfHistoryTransferType;

    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    private long timestampSeconds;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    private @Nullable String name;

    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    private boolean notSpam;

    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    private boolean archived;
    @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
    private ChatDisappear disappearInitiator;

    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    private boolean markedAsUnread;

    @ProtobufProperty(index = 20, type = ProtobufType.OBJECT, repeated = true)
    private final @NonNull ArrayList<GroupParticipant> participants;

    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    private byte @NonNull [] token;

    @ProtobufProperty(index = 22, type = ProtobufType.UINT64)
    private long tokenTimestampSeconds;

    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    private byte @NonNull [] identityKey;

    @ProtobufProperty(index = 24, type = ProtobufType.UINT32)
    private int pinnedTimestampSeconds;

    @ProtobufProperty(index = 25, type = ProtobufType.UINT64)
    private @NonNull ChatMute mute;

    @ProtobufProperty(index = 26, type = ProtobufType.OBJECT)
    private ChatWallpaper wallpaper;

    @ProtobufProperty(index = 27, type = ProtobufType.OBJECT)
    private @NonNull ChatMediaVisibility mediaVisibility;

    @ProtobufProperty(index = 28, type = ProtobufType.UINT64)
    private long tokenSenderTimestampSeconds;

    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    private boolean suspended;

    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    private boolean terminated;

    @ProtobufProperty(index = 31, type = ProtobufType.UINT64)
    private long foundationTimestampSeconds;

    @ProtobufProperty(index = 32, type = ProtobufType.STRING)
    private ContactJid founder;
    @ProtobufProperty(index = 33, type = ProtobufType.STRING)
    private String description;

    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    private boolean support;

    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    private boolean parentGroup;

    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    private boolean defaultSubGroup;

    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    private final ContactJid parentGroupJid;

    @ProtobufProperty(index = 38, type = ProtobufType.STRING)
    private String displayName;

    @ProtobufProperty(index = 39, type = ProtobufType.STRING)
    private ContactJid phoneJid;
    @ProtobufProperty(index = 40, type = ProtobufType.BOOL)
    private boolean shareOwnPhoneNumber;
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    private boolean pnhDuplicateLidThread;
    @ProtobufProperty(index = 42, type = ProtobufType.STRING)
    private ContactJid lidJid;

    private final @NonNull ConcurrentHashMap<ContactJid, ContactStatus> presences;

    private final @NonNull Set<ContactJid> participantsPreKeys;

    private final @NonNull Set<PastParticipant> pastParticipants;

    public Chat(@NonNull UUID uuid, @NonNull ContactJid jid, @NonNull ConcurrentLinkedDeque<HistorySyncMessage> historySyncMessages, ContactJid newJid, ContactJid oldJid, int unreadMessagesCount, boolean readOnly, boolean endOfHistoryTransfer, @NonNull ChatEphemeralTimer ephemeralMessageDuration, long ephemeralMessagesToggleTimeSeconds, EndOfHistoryTransferType endOfHistoryTransferType, long timestampSeconds, @Nullable String name, boolean notSpam, boolean archived, ChatDisappear disappearInitiator, boolean markedAsUnread, @NonNull ArrayList<GroupParticipant> participants, byte @NonNull [] token, long tokenTimestampSeconds, byte @NonNull [] identityKey, int pinnedTimestampSeconds, @NonNull ChatMute mute, ChatWallpaper wallpaper, @NonNull ChatMediaVisibility mediaVisibility, long tokenSenderTimestampSeconds, boolean suspended, boolean terminated, long foundationTimestampSeconds, ContactJid founder, String description, boolean support, boolean parentGroup, boolean defaultSubGroup, ContactJid parentGroupJid, String displayName, ContactJid phoneJid, boolean shareOwnPhoneNumber, boolean pnhDuplicateLidThread, ContactJid lidJid, @NonNull ConcurrentHashMap<ContactJid, ContactStatus> presences, @NonNull Set<ContactJid> participantsPreKeys, @NonNull Set<PastParticipant> pastParticipants) {
        this.uuid = uuid;
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

    public Chat(@NonNull ContactJid jid, @NonNull ConcurrentLinkedDeque<HistorySyncMessage> historySyncMessages, ContactJid newJid, ContactJid oldJid, int unreadMessagesCount, boolean readOnly, boolean endOfHistoryTransfer, @NonNull ChatEphemeralTimer ephemeralMessageDuration, long ephemeralMessagesToggleTimeSeconds, EndOfHistoryTransferType endOfHistoryTransferType, long timestampSeconds, @Nullable String name, boolean notSpam, boolean archived, ChatDisappear disappearInitiator, boolean markedAsUnread, @NonNull ArrayList<GroupParticipant> participants, byte @NonNull [] token, long tokenTimestampSeconds, byte @NonNull [] identityKey, int pinnedTimestampSeconds, @NonNull ChatMute mute, ChatWallpaper wallpaper, @NonNull ChatMediaVisibility mediaVisibility, long tokenSenderTimestampSeconds, boolean suspended, boolean terminated, long foundationTimestampSeconds, ContactJid founder, String description, boolean support, boolean parentGroup, boolean defaultSubGroup, ContactJid parentGroupJid, String displayName, ContactJid phoneJid, boolean shareOwnPhoneNumber, boolean pnhDuplicateLidThread, ContactJid lidJid) {
        this.uuid = UUID.randomUUID();
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
        this.presences = new ConcurrentHashMap<>();
        this.participantsPreKeys = ConcurrentHashMap.newKeySet();
        this.pastParticipants = ConcurrentHashMap.newKeySet();
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

        if(displayName != null) {
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
        return jid.type() == ContactJidType.GROUP;
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
    public Collection<MessageInfo> unreadMessages() {
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
    public Optional<MessageInfo> newestMessage() {
        return Optional.ofNullable(historySyncMessages.peekLast())
                .map(HistorySyncMessage::messageInfo);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestMessage() {
        return Optional.ofNullable(historySyncMessages.peekFirst())
                .map(HistorySyncMessage::messageInfo);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestStandardMessage() {
        return findMessageBy(this::isStandardMessage, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestStandardMessage() {
        return findMessageBy(this::isStandardMessage, false);
    }

    private boolean isStandardMessage(MessageInfo info) {
        return !info.message().hasCategory(MessageCategory.SERVER) && info.stubType().isEmpty();
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestMessageFromMe() {
        return findMessageBy(this::isMessageFromMe, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestMessageFromMe() {
        return findMessageBy(this::isMessageFromMe, false);
    }

    private boolean isMessageFromMe(MessageInfo info) {
        return !info.message().hasCategory(MessageCategory.SERVER) && info.stubType().isEmpty() && info.fromMe();
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestServerMessage() {
        return findMessageBy(this::isServerMessage, true);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestServerMessage() {
        return findMessageBy(this::isServerMessage, false);
    }

    private boolean isServerMessage(MessageInfo info) {
        return info.message().hasCategory(MessageCategory.SERVER) || info.stubType().isPresent();
    }

    private Optional<MessageInfo> findMessageBy(Function<MessageInfo, Boolean> filter, boolean newest) {
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
    public Collection<MessageInfo> starredMessages() {
        return historySyncMessages.stream()
                .map(HistorySyncMessage::messageInfo)
                .filter(MessageInfo::starred)
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
    public void addMessages(@NonNull Collection<HistorySyncMessage> newMessages) {
        historySyncMessages.addAll(newMessages);
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param oldMessages the non-null messages to add
     */
    public void addOldMessages(@NonNull Collection<HistorySyncMessage> oldMessages) {
        oldMessages.forEach(historySyncMessages::addFirst);
    }

    /**
     * Adds a message to the chat in the most recent slot available
     *
     * @param info the message to add to the chat
     * @return whether the message was added
     */
    public boolean addNewMessage(@NonNull MessageInfo info) {
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
    public boolean addOldMessage(@NonNull HistorySyncMessage info) {
        historySyncMessages.addFirst(info);
        return true;
    }

    /**
     * Remove a message from the chat
     *
     * @param info the message to remove
     * @return whether the message was removed
     */
    public boolean removeMessage(@NonNull MessageInfo info) {
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
    public boolean removeMessage(@NonNull Predicate<? super MessageInfo> predicate) {
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

    private void updateChatTimestamp(MessageInfo info) {
        var oldTimeStamp = newestMessage().map(MessageInfo::timestampSeconds).orElse(0L);
        if (oldTimeStamp > info.timestampSeconds()) {
            return;
        }

        this.timestampSeconds = info.timestampSeconds();
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
     * Adds a collection of participants to this chat
     *
     * @param participants the participants to add
     */
    public void addParticipants(Collection<GroupParticipant> participants) {
        participants.forEach(this::addParticipant);
    }

    /**
     * Adds a participant to this chat
     *
     * @param jid  the non-null jid of the participant
     * @param role the role of the participant
     * @return whether the participant was added
     */
    public boolean addParticipant(@NonNull ContactJid jid, GroupRole role) {
        return addParticipant(new GroupParticipant(jid, role));
    }

    /**
     * Adds a participant to this chat
     *
     * @param participant the non-null participant
     * @return whether the participant was added
     */
    public boolean addParticipant(@NonNull GroupParticipant participant) {
        return participants.add(participant);
    }

    /**
     * Removes a participant from this chat
     *
     * @param jid the non-null jid of the participant
     * @return whether the participant was removed
     */
    public boolean removeParticipant(@NonNull ContactJid jid) {
        return participants.removeIf(entry -> Objects.equals(entry.jid(), jid));
    }

    /**
     * Finds a participant by jid
     * This method only works if {@link Whatsapp#queryGroupMetadata(ContactJidProvider)} has been called before on this chat.
     * By default, all groups that have been used in the last two weeks wil be synced automatically
     *
     * @param jid the non-null jid of the participant
     * @return the participant, if present
     */
    public Optional<GroupParticipant> findParticipant(@NonNull ContactJid jid) {
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
    public boolean addPastParticipant(@NonNull PastParticipant participant) {
        return pastParticipants.add(participant);
    }

    /**
     * Adds a collection of past participants
     *
     * @param pastParticipants the non-null list of past participants
     * @return whether the participant were added
     */
    public boolean addPastParticipants(List<PastParticipant> pastParticipants) {
        var result = true;
        for (var pastParticipant : pastParticipants) {
            result &= this.pastParticipants.add(pastParticipant);
        }

        return result;
    }

    /**
     * Removes a past participant
     *
     * @param jid the non-null jid of the past participant
     * @return whether the participant was removed
     */
    public boolean removePastParticipant(@NonNull ContactJid jid) {
        return pastParticipants.removeIf(entry -> Objects.equals(entry.jid(), jid));
    }

    /**
     * Finds a past participant by jid
     *
     * @param jid the non-null jid of the past participant
     * @return the past participant, if present
     */
    public Optional<PastParticipant> findPastParticipant(@NonNull ContactJid jid) {
        return pastParticipants.stream()
                .filter(entry -> Objects.equals(entry.jid(), jid))
                .findFirst();
    }

    public Set<ContactJid> participantsPreKeys() {
        return Collections.unmodifiableSet(participantsPreKeys);
    }

    public void addParticipantsPreKeys(Collection<ContactJid> contactJids) {
        participantsPreKeys.addAll(contactJids);
    }

    public void clearParticipantsPreKeys() {
        participantsPreKeys.clear();
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
    @NonNull
    public ContactJid toJid() {
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

    public @NonNull UUID uuid() {
        return uuid;
    }

    public @NonNull ContactJid jid() {
        return jid;
    }

    public @NonNull ConcurrentLinkedDeque<HistorySyncMessage> historySyncMessages() {
        return historySyncMessages;
    }

    public Optional<ContactJid> newJid() {
        return Optional.ofNullable(newJid);
    }

    public Optional<ContactJid> oldJid() {
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

    public @NonNull ChatEphemeralTimer ephemeralMessageDuration() {
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

    public @NonNull ArrayList<GroupParticipant> participants() {
        return participants;
    }

    public byte @NonNull [] token() {
        return token;
    }

    public long tokenTimestampSeconds() {
        return tokenTimestampSeconds;
    }

    public byte @NonNull [] identityKey() {
        return identityKey;
    }

    public int pinnedTimestampSeconds() {
        return pinnedTimestampSeconds;
    }

    public @NonNull ChatMute mute() {
        return mute;
    }

    public Optional<ChatWallpaper> wallpaper() {
        return Optional.ofNullable(wallpaper);
    }

    public @NonNull ChatMediaVisibility mediaVisibility() {
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

    public Optional<ContactJid> founder() {
        return Optional.ofNullable(founder);
    }

    public Optional<String> description() {
        return description.describeConstable();
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

    public Optional<ContactJid> parentGroupJid() {
        return Optional.ofNullable(parentGroupJid);
    }

    public Optional<String> displayName() {
        return displayName.describeConstable();
    }

    public Optional<ContactJid> phoneJid() {
        return Optional.ofNullable(phoneJid);
    }

    public boolean shareOwnPn() {
        return shareOwnPhoneNumber;
    }

    public boolean pnhDuplicateLidThread() {
        return pnhDuplicateLidThread;
    }

    public Optional<ContactJid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    public @NonNull ConcurrentHashMap<ContactJid, ContactStatus> presences() {
        return presences;
    }

    public @NonNull Set<PastParticipant> pastParticipants() {
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
        return this;
    }

    public Chat setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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

    public Chat setToken(byte[] token) {
        this.token = token;
        return this;
    }

    public Chat setTokenTimestampSeconds(long tokenTimestampSeconds) {
        this.tokenTimestampSeconds = tokenTimestampSeconds;
        return this;
    }

    public Chat setIdentityKey(byte[] identityKey) {
        this.identityKey = identityKey;
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

    public Chat setMediaVisibility(ChatMediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
        return this;
    }

    public Chat setTokenSenderTimestampSeconds(long tokenSenderTimestampSeconds) {
        this.tokenSenderTimestampSeconds = tokenSenderTimestampSeconds;
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

    public Chat setFoundationTimestampSeconds(long foundationTimestampSeconds) {
        this.foundationTimestampSeconds = foundationTimestampSeconds;
        return this;
    }

    public Chat setFounder(ContactJid founder) {
        this.founder = founder;
        return this;
    }

    public Chat setDescription(String description) {
        this.description = description;
        return this;
    }

    public Chat setSupport(boolean support) {
        this.support = support;
        return this;
    }

    public Chat setParentGroup(boolean parentGroup) {
        this.parentGroup = parentGroup;
        return this;
    }

    public Chat setDefaultSubGroup(boolean defaultSubGroup) {
        this.defaultSubGroup = defaultSubGroup;
        return this;
    }

    public Chat setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Chat setPhoneJid(ContactJid phoneJid) {
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

    public Chat setLidJid(ContactJid lidJid) {
        this.lidJid = lidJid;
        return this;
    }
}